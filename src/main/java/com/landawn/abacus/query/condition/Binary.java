/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.query.condition;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.landawn.abacus.query.QueryUtil;
import com.landawn.abacus.query.cs;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Base class for binary conditions that compare a property with a value.
 * Binary conditions represent operations with two operands: a property name and a value.
 *
 * <p>This class is concrete and can be instantiated directly, but it also serves as the
 * foundation for all comparison operations in queries,
 * providing common functionality for storing the property name, operator, and value.
 * The value can be a literal, an explicit {@link SqlExpression}, or a scalar {@link SubQuery}.
 * A Boolean right-hand value of {@code IS}/{@code IS NOT} is stored as the {@link SqlExpression}
 * {@code TRUE}/{@code FALSE}, so {@link #propValue()} returns that expression rather than the Boolean.</p>
 *
 * <p>Arrays, {@link java.util.Date} values, and {@link java.util.Calendar} values are snapshotted
 * on construction and defensively copied when exposed. Other application-defined mutable values
 * are retained by reference; callers must not mutate them while the condition is in use.</p>
 *
 * <p>Common subclasses include:</p>
 * <ul>
 *   <li>{@link Equal} - property = value</li>
 *   <li>{@link NotEqual} - property != or &lt;&gt; value</li>
 *   <li>{@link GreaterThan} - property &gt; value</li>
 *   <li>{@link GreaterThanOrEqual} - property &gt;= value</li>
 *   <li>{@link LessThan} - property &lt; value</li>
 *   <li>{@link LessThanOrEqual} - property &lt;= value</li>
 *   <li>{@link Like} - property LIKE value</li>
 *   <li>{@link NotLike} - property NOT LIKE value</li>
 *   <li>{@link Is} - property IS value</li>
 *   <li>{@link IsNot} - property IS NOT value</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple binary conditions
 * Binary eq = new Equal("name", "John");
 * Binary gt = new GreaterThan("age", 18);
 * 
 * // Binary condition with subquery
 * SubQuery avgSalary = Filters.subQuery("SELECT AVG(salary) FROM employees");
 * Binary aboveAvg = new GreaterThan("salary", avgSalary);
 * }</pre>
 *
 * <p><b>Note on {@code IN}/{@code NOT IN}:</b> although {@code Binary} accepts the membership
 * operators, prefer the dedicated {@link In}/{@link NotIn} condition classes (or the
 * {@code Filters.in}/{@code Filters.notIn} factories) for IN conditions — they expose the values
 * through {@code values()}, support multi-column row-value form, and are what
 * {@code instanceof}-based consumers expect. A {@code Binary} with {@code Operator.IN} renders
 * the same SQL but is a different type with different accessors.</p>
 *
 * @see ComposableCondition
 * @see Equal
 * @see GreaterThan
 * @see LessThan
 * @see Condition
 */
public class Binary extends ComposableCondition {

    /**
     * The operators valid for a binary {@code propName OP value} condition. This covers the operators
     * used by the concrete {@link Binary} subclasses ({@link Equal}, {@link NotEqual}, {@link GreaterThan},
     * {@link GreaterThanOrEqual}, {@link LessThan}, {@link LessThanOrEqual}, {@link Like}, {@link NotLike},
     * {@link Is}, {@link IsNot}) plus the ANSI not-equal token ({@link Operator#NOT_EQUAL_ANSI}) and the
     * membership operators ({@link Operator#IN}, {@link Operator#NOT_IN}) that {@code Binary} renders
     * directly when constructed via {@link com.landawn.abacus.query.Filters#binary}. Structural operators
     * (clauses, junctions, joins, set operations, quantifiers, {@code BETWEEN}/{@code EXISTS}) are rejected.
     */
    private static final Set<Operator> COMPARISON_OPERATORS = EnumSet.of(Operator.EQUAL, Operator.NOT_EQUAL, Operator.NOT_EQUAL_ANSI, Operator.GREATER_THAN,
            Operator.GREATER_THAN_OR_EQUAL, Operator.LESS_THAN, Operator.LESS_THAN_OR_EQUAL, Operator.LIKE, Operator.NOT_LIKE, Operator.IS, Operator.IS_NOT,
            Operator.IN, Operator.NOT_IN);

    /** SQL operators that may take a direct ALL/ANY/SOME right-hand operand. */
    private static final Set<Operator> QUANTIFIED_COMPARISON_OPERATORS = EnumSet.of(Operator.EQUAL, Operator.NOT_EQUAL, Operator.NOT_EQUAL_ANSI,
            Operator.GREATER_THAN, Operator.GREATER_THAN_OR_EQUAL, Operator.LESS_THAN, Operator.LESS_THAN_OR_EQUAL);

    /**
     * SQL truth-value keywords a Boolean {@code IS}/{@code IS NOT} operand is normalized to (the same
     * literals {@code Filters.isTrue(String)}/{@code Filters.isFalse(String)} use), so the truth value is
     * always rendered inline and never bound as a parameter.
     */
    private static final String SQL_TRUE_LITERAL = "TRUE";
    private static final String SQL_FALSE_LITERAL = "FALSE";

    /**
     * The property (column) name on the left-hand side of this binary condition;
     * {@code null} only on an uninitialized instance created by a serialization framework like Kryo.
     */
    final String propName;

    /**
     * The value on the right-hand side of this binary condition; may be {@code null} only for
     * equality and {@code IS}/{@code IS NOT} operators, a literal, an explicit scalar expression,
     * or an unmodifiable membership list for {@code IN}/{@code NOT IN}.
     */
    private final Object propValue;

    /**
     * Whether {@link #parameters()} must rebuild its result on every call instead of memoizing it: {@code true}
     * when {@link #propValue} is (or, for {@code IN}/{@code NOT IN}, contains) an array, {@code Date},
     * {@code Calendar} or a nested {@link Condition} such as a {@link SubQuery}, a quantified
     * {@link All}/{@link Any}/{@link Some} operand or a {@link SqlExpression}. Computed once at construction
     * because the operand is immutable afterwards. A nested condition counts as mutable because its own
     * {@code parameters()} may hand out fresh defensive copies that a memoized outer list would otherwise
     * share across calls.
     */
    private final boolean rebuildParametersPerCall;

    /** Lazily memoized parameters (performance only; unused when {@link #rebuildParametersPerCall} is {@code true}). */
    private transient ImmutableList<Object> cachedParameters;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Binary instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Binary() {
        propName = null;
        propValue = null;
        rebuildParametersPerCall = false;
    }

    /**
     * Creates a new Binary condition.
     * This constructor initializes a binary condition with a property name, operator, and value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a custom binary condition
     * Binary condition = new Binary("price", Operator.GREATER_THAN, 100.0);
     * // SQL: price > 100.0
     *
     * // With a subquery as value
     * SubQuery subQuery = Filters.subQuery("SELECT MIN(price) FROM products");
     * Binary minPrice = new Binary("price", Operator.GREATER_THAN_OR_EQUAL, subQuery);
     * // SQL: price >= (SELECT MIN(price) FROM products)
     * }</pre>
     * 
     * @param propName the property name to compare (must not be {@code null}, empty, or blank)
     * @param operator the comparison operator (must not be {@code null}); must be an operator valid for a
     *                 binary {@code propName OP value} condition, i.e. one of {@link Operator#EQUAL},
     *                 {@link Operator#NOT_EQUAL}, {@link Operator#NOT_EQUAL_ANSI}, {@link Operator#GREATER_THAN},
     *                 {@link Operator#GREATER_THAN_OR_EQUAL}, {@link Operator#LESS_THAN},
     *                 {@link Operator#LESS_THAN_OR_EQUAL}, {@link Operator#LIKE}, {@link Operator#NOT_LIKE},
     *                 {@link Operator#IS}, {@link Operator#IS_NOT}, {@link Operator#IN}, or {@link Operator#NOT_IN}
     * @param propValue the value to compare against; may be a literal value, {@code null} only for
     *                  equality and {@code IS}/{@code IS NOT} operators (rendering as
     *                  {@code IS NULL} / {@code IS NOT NULL}), an explicit {@link SqlExpression}, or
     *                  a scalar {@link SubQuery}. An {@link All}, {@link Any}, or {@link Some} operand is accepted
     *                  only as the direct right-hand side of {@code =}, {@code !=}, {@code <>}, {@code <},
     *                  {@code <=}, {@code >}, or {@code >=}. For an {@code IN}/{@code NOT_IN} operator, a
     *                  {@link Collection} or array value is copied defensively and must be non-empty;
     *                  elements must be non-null scalar values, explicit scalar expressions, or scalar sub-queries. A
     *                  {@link SqlExpression} or a {@link SubQuery} is also accepted as the whole right-hand
     *                  side of {@code IN}/{@code NOT IN}.
     *                  Every structured subquery with a known, non-wildcard projection must select exactly
     *                  one column; raw SQL and wildcard projection arity are left to the database.
     *                  For {@code IS}/{@code IS NOT}, the value must be {@code null}, a Boolean, or
     *                  an explicit {@link SqlExpression} such as {@code NULL}, {@code TRUE}, or {@code UNKNOWN}.
     *                  A Boolean is normalized at construction to the SQL keyword expression {@code TRUE} /
     *                  {@code FALSE} (the same literals {@code Filters.isTrue}/{@code Filters.isFalse} use), so it
     *                  is always rendered inline ({@code x IS TRUE}) and never bound as a parameter; consequently
     *                  {@code new Is("x", true)} equals {@code Filters.isTrue("x")}.
     * @throws IllegalArgumentException if {@code operator} is {@code null}; if {@code propName} is {@code null}, empty, or blank; if {@code operator}
     *                                  is not one of the operators listed above; or if, for an {@code IN}/{@code NOT_IN}
     *                                  operator, {@code propValue} is not a non-empty {@link Collection}, a non-empty
     *                                  array, a {@link SqlExpression}, or a {@link SubQuery}; if {@code propValue} is
     *                                  {@code null} for an operator other than {@code =}, {@code !=}, {@code <>}, {@code IS},
     *                                  or {@code IS NOT}, or an {@code IN}/{@code NOT IN} element is {@code null}; if
     *                                  {@code IS}/{@code IS NOT} receives a value other than {@code null}, a Boolean, or a
     *                                  {@link SqlExpression}; if a condition-valued operand is an ordinary predicate or query clause or a blank
     *                                  {@link SqlExpression}; or if an {@link All}/{@link Any}/{@link Some} operand is used
     *                                  anywhere other than the direct RHS of a compatible scalar comparison; if a
     *                                  scalar {@link SubQuery} has a known, non-wildcard projection with multiple columns;
     *                                  or if {@code propValue}, or an element of an {@code IN}/{@code NOT IN} membership
     *                                  list, is a cyclic object array
     */
    public Binary(final String propName, final Operator operator, final Object propValue) {
        super(operator);

        checkPropName(propName);

        if (!COMPARISON_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException(
                    "Binary condition operator must be a comparison operator (one of " + COMPARISON_OPERATORS + "), but was: " + operator);
        }

        this.propName = propName;
        this.propValue = normalizePropValue(operator, propValue);
        this.rebuildParametersPerCall = requiresPerCallParameters(this.propValue);
    }

    /**
     * Returns the property name being compared.
     * This is the left-hand side of the binary operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary eq = new Equal("age", 25);
     * String name = eq.propName();   // "age"
     *
     * Binary like = new Like("email", "%@example.com");
     * String likeName = like.propName();   // "email"
     * }</pre>
     *
     * @return the property name, or {@code null} for an uninitialized serialization-framework instance
     */
    public String propName() {
        return propName;
    }

    /**
     * Returns the property value without an unchecked generic cast. Arrays, dates, and calendars are
     * defensively copied, including those nested in an {@code IN}/{@code NOT IN} membership list.
     * Membership lists are returned as unmodifiable copies. A collection used as a scalar value
     * by another operator retains its concrete type and identity; callers must not mutate that
     * collection while the condition is in use.
     *
     * <p>A Boolean right-hand value of {@code IS}/{@code IS NOT} is normalized at construction to the
     * {@link SqlExpression} {@code TRUE}/{@code FALSE}, so this method returns that expression (not the
     * original {@code Boolean}) for such conditions.</p>
     *
     * @return the property value, which may be {@code null}
     */
    public Object propValue() {
        return copyPropValueForExposure(propValue);
    }

    /**
     * Returns the property value cast to the supplied runtime type.
     * This is the type-safe companion to {@link #propValue()} when the expected value type is known.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal cond = new Equal("status", "active");
     * String status = cond.propValueAs(String.class);   // "active"
     * }</pre>
     *
     * <p>A Boolean right-hand value of {@code IS}/{@code IS NOT} is normalized at construction to the
     * {@link SqlExpression} {@code TRUE}/{@code FALSE}, so {@code propValueAs(Boolean.class)} throws
     * {@link ClassCastException} for such conditions; use {@code propValueAs(SqlExpression.class)} or
     * {@link #propValue()} instead.</p>
     *
     * @param <T> the requested value type
     * @param valueType the requested value type; must not be {@code null}
     * @return the property value cast to {@code valueType}, or {@code null} when the stored value is {@code null};
     *         known mutable JDK values are returned as defensive copies
     * @throws IllegalArgumentException if {@code valueType} is {@code null}
     * @throws ClassCastException if the stored value is not assignable to {@code valueType}
     */
    public <T> T propValueAs(final Class<T> valueType) {
        N.checkArgNotNull(valueType, cs.valueType);

        return valueType.cast(copyPropValueForExposure(propValue));
    }

    /**
     * Returns the parameters for this condition.
     *
     * <ul>
     *   <li>If the value is {@code null} and the operator is {@code =}, {@code !=}, {@code <>}, {@code IS}, or
     *       {@code IS NOT}, an empty list is returned because the SQL is rendered as {@code IS NULL} /
     *       {@code IS NOT NULL} with no bind parameter.</li>
     *   <li>If the operator is {@code null} (only possible for an uninitialized instance), an empty list
     *       is returned.</li>
     *   <li>If the operator is {@code IN} or {@code NOT IN} and the value is a {@link Collection}, each element is
     *       added as a parameter; any element that is itself a {@link Condition} has its own parameters spliced in.</li>
     *   <li>If the value is a {@link Condition} (e.g., a subquery), the subquery's own parameters are returned.
     *       This includes a Boolean {@code IS}/{@code IS NOT} operand, which is normalized to the keyword
     *       expression {@code TRUE}/{@code FALSE} at construction and therefore contributes no bind parameter.</li>
     *   <li>Otherwise, a single-element list containing the value is returned.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Literal value -> single-element list
     * Binary eq = new Equal("age", 25);
     * List<Object> p1 = eq.parameters();   // [25]
     *
     * // Null value with = or != -> empty list (rendered as IS NULL / IS NOT NULL)
     * Binary nullEq = new Equal("name", (Object) null);
     * List<Object> p2 = nullEq.parameters();   // [] (empty)
     *
     * // Subquery value -> the subquery's own parameters
     * SubQuery sub = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
     * Binary eqSub = new Equal("userId", sub);
     * List<Object> p3 = eqSub.parameters();   // [true] (the subquery's params)
     * }</pre>
     *
     * <p>The result is memoized only when every operand is a plain scalar (neither an array, {@code Date},
     * {@code Calendar} nor a nested {@link Condition}); otherwise a fresh list, holding fresh defensive copies of
     * any array/{@code Date}/{@code Calendar} values (including those spliced in from a nested condition), is
     * built on every call. Mutating one of these defensive array/date/calendar copies does not affect
     * this condition or a later call. Other mutable scalar values, including collections used outside
     * {@code IN}/{@code NOT IN}, are retained by reference and must not be mutated while in use.</p>
     *
     * @return an immutable list of parameter values; known mutable JDK values in the list are defensive
     *         copies, and the result is never {@code null}
     */
    @Override
    public ImmutableList<Object> parameters() {
        if (rebuildParametersPerCall) {
            return computeParameters();
        }

        ImmutableList<Object> result = cachedParameters;

        if (result == null) {
            result = computeParameters();
            cachedParameters = result;
        }

        return result;
    }

    /**
     * Builds the parameter list returned by {@link #parameters()}, applying the rules described there.
     * The result is memoized unless the value is or contains a snapshot-mutable value (array, {@code Date},
     * {@code Calendar}) or a nested {@link Condition}, in which case it is rebuilt on every call so each caller
     * receives fresh defensive copies.
     *
     * @return an immutable list of parameter values; never {@code null}
     */
    private ImmutableList<Object> computeParameters() {
        final Operator op = operator();

        if (propValue == null
                && (op == Operator.EQUAL || op == Operator.NOT_EQUAL || op == Operator.NOT_EQUAL_ANSI || op == Operator.IS || op == Operator.IS_NOT)) {
            return ImmutableList.empty();
        }

        if (op == null) {
            return ImmutableList.empty();
        }

        if (isCollectionOperator(op) && propValue instanceof final Collection<?> values) {
            final List<Object> parameters = new ArrayList<>(values.size());

            for (final Object value : values) {
                if (value instanceof Condition) {
                    parameters.addAll(((Condition) value).parameters());
                } else {
                    parameters.add(snapshotMutableValue(value));
                }
            }

            return ImmutableList.wrap(parameters);
        }

        if (propValue instanceof Condition) {
            return ((Condition) propValue).parameters();
        } else {
            return ImmutableList.of(snapshotMutableValue(propValue));
        }
    }

    /**
     * Converts this Binary condition to its SQL representation using the specified naming policy.
     *
     * <p>Normally the format is: {@code propertyName OPERATOR value}.
     * When the value is {@code null} and the operator is {@code =} or {@code IS}, the output is
     * {@code propertyName IS NULL}; when the operator is {@code !=}, {@code <>}, or {@code IS NOT},
     * the output is {@code propertyName IS NOT NULL}. Other operators reject a {@code null} value
     * during construction because comparisons such as {@code x > NULL} and {@code x LIKE NULL}
     * cannot evaluate to true under SQL three-valued logic.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // String values are single-quoted; numbers are unquoted
     * Binary eq = new Equal("name", "John");
     * String s1 = eq.toSql(NamingPolicy.NO_CHANGE);   // "name = 'John'"
     *
     * Binary gt = new GreaterThan("age", 18);
     * String s2 = gt.toSql(NamingPolicy.NO_CHANGE);   // "age > 18"
     *
     * // Null value with = renders as IS NULL; with != renders as IS NOT NULL
     * Binary nullEq = new Equal("deletedAt", (Object) null);
     * String s3 = nullEq.toSql(NamingPolicy.NO_CHANGE);   // "deletedAt IS NULL"
     *
     * Binary nullNe = new NotEqual("deletedAt", (Object) null);
     * String s4 = nullNe.toSql(NamingPolicy.NO_CHANGE);   // "deletedAt IS NOT NULL"
     *
     * // Subquery values are parenthesized; a null naming policy uses NO_CHANGE
     * Binary sub = new Equal("userId", Filters.subQuery("SELECT id FROM users"));
     * String s5 = sub.toSql(null);   // "userId = (SELECT id FROM users)"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to the property name;
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return a SQL representation of this condition
     * @throws IllegalArgumentException if the value (or a value in an {@code IN}/{@code NOT IN} collection)
     *                                  is a {@code NaN} or infinite {@link Float}/{@link Double}, or a
     *                                  {@link Number} whose text is not a valid numeric literal, or if a
     *                                  {@link SubQuery} value (directly or inside an {@code ALL}/{@code ANY}/{@code SOME}
     *                                  operand) cannot be rendered, as documented for {@link SubQuery#toSql(NamingPolicy)}
     */
    @Override
    public String toSql(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Operator op = operator();

        if (propValue == null) {
            if (op == Operator.EQUAL || op == Operator.IS) {
                return QueryUtil.convertIdentifier(propName, effectiveNamingPolicy) + SK._SPACE + SK.IS_NULL;
            } else if (op == Operator.NOT_EQUAL || op == Operator.NOT_EQUAL_ANSI || op == Operator.IS_NOT) {
                return QueryUtil.convertIdentifier(propName, effectiveNamingPolicy) + SK._SPACE + SK.IS_NOT_NULL;
            }
        }

        final String opStr = op == null ? Strings.NULL : op.toString();

        if (isCollectionOperator(op) && propValue instanceof final Collection<?> values) {
            return QueryUtil.convertIdentifier(propName, effectiveNamingPolicy) + SK._SPACE + opStr + SK._SPACE
                    + formatCollection(values, effectiveNamingPolicy);
        }

        return QueryUtil.convertIdentifier(propName, effectiveNamingPolicy) + SK._SPACE + opStr + SK._SPACE + formatParameter(propValue, effectiveNamingPolicy);
    }

    /**
     * Tests whether {@code op} takes a collection-valued right-hand side, i.e. is {@link Operator#IN} or
     * {@link Operator#NOT_IN}.
     *
     * @param op the operator to test; may be {@code null}
     * @return {@code true} only for {@code IN} and {@code NOT_IN}
     */
    private static boolean isCollectionOperator(final Operator op) {
        return op == Operator.IN || op == Operator.NOT_IN;
    }

    /**
     * Validates {@code propValue} against {@code op} and normalizes it for storage. For {@code IN}/
     * {@code NOT IN}, a {@link Collection} or array value is defensively copied into an unmodifiable list
     * (which must be non-empty and contain no nulls), and an explicit scalar SQL expression is accepted
     * as-is; for every other operator the value is validated as a scalar operand. Ordinary predicates
     * and query clauses are never scalar operands.
     *
     * @param op the comparison operator; not {@code null}
     * @param propValue the raw right-hand-side value; may be {@code null}
     * @return the normalized value to store
     * @throws IllegalArgumentException if the value is not valid for {@code op}, as documented for
     *         {@link #Binary(String, Operator, Object)}
     */
    private static Object normalizePropValue(final Operator op, final Object propValue) {
        if (!isCollectionOperator(op)) {
            return validateScalarValueOperand(op, propValue);
        }

        if (propValue instanceof Condition) {
            return validateNonQuantifiedValueOperand(propValue, cs.propValue);
        }

        // The copies are wrapped unmodifiable so propValue() cannot leak a mutable view of the
        // internal membership list (mutation would silently desync the memoized parameters/hashCode).
        if (propValue instanceof final Collection<?> values) {
            final List<Object> valuesCopy = new ArrayList<>(values.size());

            int index = 0;
            for (final Object value : values) {
                if (value == null) {
                    throw new IllegalArgumentException("propValue[" + index + "] must not be null for " + op);
                }

                valuesCopy.add(snapshotMutableValue(validateNonQuantifiedValueOperand(value, "propValue[" + index++ + "]")));
            }

            N.checkArgNotEmpty(valuesCopy, cs.propValue);
            return Collections.unmodifiableList(valuesCopy);
        }

        if (propValue != null && propValue.getClass().isArray()) {
            final int len = Array.getLength(propValue);

            if (len == 0) {
                throw new IllegalArgumentException("propValue must not be empty");
            }

            final List<Object> values = new ArrayList<>(len);

            for (int i = 0; i < len; i++) {
                final Object value = Array.get(propValue, i);

                if (value == null) {
                    throw new IllegalArgumentException("propValue[" + i + "] must not be null for " + op);
                }

                values.add(snapshotMutableValue(validateNonQuantifiedValueOperand(value, "propValue[" + i + "]")));
            }

            return Collections.unmodifiableList(values);
        }

        throw new IllegalArgumentException("IN/NOT IN operator requires a non-empty collection, array, SqlExpression, or SubQuery value");
    }

    /**
     * Validates a scalar (non-{@code IN}/{@code NOT IN}) right-hand operand. An {@link All}/{@link Any}/
     * {@link Some} quantified-subquery operand is accepted only when {@code op} is in
     * {@link #QUANTIFIED_COMPARISON_OPERATORS}; every other operand must be non-quantified and
     * an explicitly supported scalar expression. Null is accepted only by equality and
     * {@code IS}/{@code IS NOT} operators.
     *
     * @param op the comparison operator; not {@code null}
     * @param propValue the raw right-hand-side value; may be {@code null}
     * @return the validated value
     * @throws IllegalArgumentException if {@code propValue} is {@code null} for an operator other than {@code =},
     *         {@code !=}, {@code <>}, {@code IS}, or {@code IS NOT}; if {@code IS}/{@code IS NOT} receives a value other
     *         than {@code null}, a Boolean, or a non-blank {@link SqlExpression}; if a quantified operand is used with an
     *         incompatible operator; if the operand is any other unsupported condition (an ordinary predicate or query
     *         clause, a blank {@link SqlExpression}, or a structured {@link SubQuery} with a known multi-column
     *         projection); or if the value is a cyclic object array
     */
    private static Object validateScalarValueOperand(final Operator op, final Object propValue) {
        if (propValue == null) {
            if (op == Operator.EQUAL || op == Operator.NOT_EQUAL || op == Operator.NOT_EQUAL_ANSI || op == Operator.IS || op == Operator.IS_NOT) {
                return null;
            }

            throw new IllegalArgumentException("propValue must not be null for " + op + "; use an IS NULL/IS NOT NULL condition instead");
        }

        if (op == Operator.IS || op == Operator.IS_NOT) {
            if (propValue instanceof Boolean) {
                // Normalize to the SQL truth-value keyword so every rendering path (toSql and the
                // parameterized/named builders) emits `x IS TRUE` and never binds the Boolean as a
                // parameter (`x IS ?` is not valid SQL). Same literals as Filters.isTrue/isFalse.
                return SqlExpression.of(((Boolean) propValue) ? SQL_TRUE_LITERAL : SQL_FALSE_LITERAL);
            }

            if (propValue instanceof SqlExpression) {
                return validateValueOperand(propValue, cs.propValue);
            }

            throw new IllegalArgumentException(op + " requires null, a Boolean, or an explicit SqlExpression right-hand value");
        }

        if (propValue instanceof Condition && isQuantifiedSubQueryOperand((Condition) propValue)) {
            if (!QUANTIFIED_COMPARISON_OPERATORS.contains(op)) {
                throw new IllegalArgumentException(op + " does not support an ALL/ANY/SOME right-hand operand");
            }

            return validateValueOperand(propValue, cs.propValue);
        }

        return snapshotMutableValue(validateNonQuantifiedValueOperand(propValue, cs.propValue));
    }

    /** Returns a safe public view while preserving identity for immutable and application-defined values. */
    private Object copyPropValueForExposure(final Object value) {
        if (isCollectionOperator(operator()) && value instanceof final Collection<?> values) {
            final List<Object> copy = new ArrayList<>(values.size());

            for (final Object element : values) {
                copy.add(snapshotMutableValue(element));
            }

            return Collections.unmodifiableList(copy);
        }

        return snapshotMutableValue(value);
    }

    /**
     * Detects whether returning a memoized parameter list would expose a known mutable value: an array,
     * {@code Date} or {@code Calendar} operand, or a nested {@link Condition} whose spliced-in parameters may
     * themselves be per-call defensive copies. Evaluated once at construction into
     * {@link #rebuildParametersPerCall}.
     */
    private static boolean requiresPerCallParameters(final Object value) {
        if (isSnapshotMutableValue(value) || value instanceof Condition) {
            return true;
        }

        if (value instanceof Collection<?>) {
            for (final Object element : (Collection<?>) value) {
                if (isSnapshotMutableValue(element) || element instanceof Condition) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Renders the values of an {@code IN}/{@code NOT IN} operand as a parenthesized, comma-separated list
     * of formatted parameter literals.
     *
     * @param values the non-empty membership values
     * @param namingPolicy the naming policy applied to condition-valued elements
     * @return the parenthesized value list (e.g. {@code "(1, 'a', 3)"})
     */
    private static String formatCollection(final Collection<?> values, final NamingPolicy namingPolicy) {
        final StringBuilder sb = new StringBuilder();
        sb.append(SK._PARENTHESIS_L);

        int i = 0;
        for (final Object value : values) {
            if (i++ > 0) {
                sb.append(SK.COMMA_SPACE);
            }

            sb.append(formatParameter(value, namingPolicy));
        }

        sb.append(SK._PARENTHESIS_R);
        return sb.toString();
    }

    /**
     * Returns the hash code of this Binary condition.
     * The hash code is computed based on the property name, operator, and value.
     * It is recomputed on every call because application-defined mutable values are retained by
     * reference. Arrays, dates, and calendars cannot be mutated through this condition because they
     * are snapshotted and defensively copied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Equal property/operator/value -> equal hash codes
     * Binary a = new Equal("age", 25);
     * Binary b = new Equal("age", 25);
     * boolean same = a.hashCode() == b.hashCode();   // true
     * }</pre>
     *
     * @return hash code based on property name, operator, and value
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((propName == null) ? 0 : propName.hashCode());
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        // Membership lists deep-walk array elements so IN content-equality matches scalar arrays.
        h = (h * 31) + deepPropValueHashCode(propValue);

        return h == 0 ? 1 : h;
    }

    /**
     * Checks if this Binary condition is equal to another object.
     * Two conditions are equal only if they are of the exact same runtime class and have the same
     * property name, operator, and value. The runtime class is part of the equality contract, so an
     * instance of one concrete subclass is never equal to an instance of a different subclass (or to
     * a raw {@code Binary}), even when their property name, operator, and value all match.
     *
     * <p>For {@code IN}/{@code NOT IN} membership lists, array elements are compared by content
     * (the same contract as a scalar array RHS) rather than by {@link List#equals(Object)} identity.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary a = new Equal("age", 25);
     * Binary b = new Equal("age", 25);
     * boolean eq = a.equals(b);   // true (same prop, operator, value)
     *
     * // Different value or property -> not equal
     * boolean neValue = a.equals(new Equal("age", 30));   // false
     * boolean neProp = a.equals(new Equal("name", 25));   // false
     *
     * // Non-Binary object -> not equal
     * boolean neType = a.equals("age");   // false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Binary other = (Binary) obj;
        return N.equals(propName, other.propName) && N.equals(operator, other.operator) && deepPropValueEquals(propValue, other.propValue);
    }

    /**
     * Deep equality for the RHS value. Collections (IN membership lists) are compared element-wise
     * so array members use content equality via {@link N#deepEquals(Object, Object)}, which pairs
     * with the {@link N#deepHashCode(Object)} used by {@link #hashCode()}.
     */
    private static boolean deepPropValueEquals(final Object left, final Object right) {
        if (left == right) {
            return true;
        }

        if (left == null || right == null) {
            return false;
        }

        if (left instanceof final Collection<?> leftValues && right instanceof final Collection<?> rightValues) {
            if (leftValues.size() != rightValues.size()) {
                return false;
            }

            final Iterator<?> leftIter = leftValues.iterator();
            final Iterator<?> rightIter = rightValues.iterator();

            while (leftIter.hasNext()) {
                if (!deepPropValueEquals(leftIter.next(), rightIter.next())) {
                    return false;
                }
            }

            return true;
        }

        return N.deepEquals(left, right);
    }

    /**
     * Deep hash for the RHS value, aligned with {@link #deepPropValueEquals}.
     */
    private static int deepPropValueHashCode(final Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof final Collection<?> values) {
            int h = 1;

            for (final Object element : values) {
                h = (31 * h) + deepPropValueHashCode(element);
            }

            return h;
        }

        return N.deepHashCode(value);
    }
}
