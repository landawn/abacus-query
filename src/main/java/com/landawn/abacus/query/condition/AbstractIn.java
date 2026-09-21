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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.query.QueryUtil;
import com.landawn.abacus.query.cs;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for IN and NOT IN conditions in SQL queries.
 * This class provides all shared implementation for collection membership conditions,
 * similar to how {@link Binary} serves as the base for {@link Equal}, {@link NotEqual}, etc.
 *
 * <p>Two forms are supported:</p>
 * <ul>
 *   <li><b>Single-column</b> ({@link #AbstractIn(String, Operator, Collection)}): each value is a
 *       scalar, rendered as {@code propName IN (v1, v2, ...)}.</li>
 *   <li><b>Row value constructor</b>
 *       ({@link #AbstractIn(Collection, Operator, Collection)}): each value is itself a row whose
 *       width matches the number of property names, rendered as
 *       {@code (p1, p2) IN ((v1a, v1b), (v2a, v2b), ...)}. One-column row values are also allowed,
 *       for example {@code (p1) IN ((v1), (v2))}. This mirrors the subquery form provided by
 *       {@link AbstractInSubQuery}. A row may be supplied as a {@link Collection} or other
 *       {@link Iterable}, an object array, a {@link Map} (looked up by property name) or a bean (read
 *       by property name); see {@link #AbstractIn(Collection, Operator, Collection)} for details.</li>
 * </ul>
 *
 * <p><b>&#9888;&#65039;</b> The row value-list form is supported by MySQL, PostgreSQL,
 * Oracle and DB2, but <i>not</i> by SQL Server (rewrite the composite comparison with
 * {@code EXISTS}/{@code NOT EXISTS} or a join there).</p>
 *
 * <p>The only difference between {@link In} and {@link NotIn} is the operator
 * ({@code IN} vs {@code NOT IN}). All fields, getters, and methods
 * for parameters, string rendering, hashing, and equality are identical.</p>
 *
 * <p>Each subquery used as a scalar membership value or tuple element must select exactly one column
 * when its structured, non-wildcard projection is known. Raw SQL and wildcard projections have unknown
 * arity and are left to the database.</p>
 *
 * @see In
 * @see NotIn
 * @see InSubQuery
 * @see NotInSubQuery
 * @see AbstractInSubQuery
 * @see ComposableCondition
 */
public abstract class AbstractIn extends ComposableCondition {

    // For Kryo
    final ImmutableList<String> propNames;

    private final boolean rowValueConstructor;

    private List<?> values;

    /**
     * Whether {@link #values} holds at least one array, {@code Date}, {@code Calendar} or nested
     * {@link Condition} element (a scalar {@link SubQuery} or {@link SqlExpression}, directly or inside a row
     * tuple). Computed once at construction because the membership list is immutable afterwards; when
     * {@code true}, {@link #values()} and {@link #parameters()} rebuild a fresh list with defensive copies on
     * every call instead of memoizing. A nested condition counts as mutable because its own
     * {@code parameters()} may hand out fresh defensive copies that a memoized outer list would otherwise
     * share across calls.
     */
    private final boolean rebuildPerCall;

    /** Lazily memoized parameters (performance only; unused when {@link #rebuildPerCall} is {@code true}). */
    private transient ImmutableList<Object> cachedParameters;

    /** Lazily memoized immutable view of {@link #values} (performance only). */
    private transient ImmutableList<?> cachedValuesView;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    AbstractIn() {
        propNames = ImmutableList.empty();
        rowValueConstructor = false;
        rebuildPerCall = false;
    }

    /**
     * Creates a new single-column IN or NOT IN condition. The given values are copied into an internal
     * {@link ArrayList}, so later mutations to the supplied collection do not affect this
     * condition. Array, {@link java.util.Date} and {@link java.util.Calendar} elements are snapshotted
     * (deep-copied) at construction, exactly as {@link Binary} does for an {@code IN} membership list, so
     * later mutation of the caller's objects does not change this condition's SQL, parameters, hash code or
     * equality; {@link #values()} and {@link #parameters()} hand out defensive copies of such elements.
     * Other application-defined values are retained by reference. {@code null} elements are rejected because
     * SQL membership predicates do not treat {@code NULL} as an ordinary value. Individual elements may be
     * literal values, {@link SqlExpression}s or scalar {@link SubQuery}s; the latter two have their parameters
     * spliced into {@link #parameters()}. Any other {@link Condition} element is rejected.
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param values the collection of values to check membership against (must not be {@code null}, empty,
     *               or contain {@code null})
     * @throws NullPointerException if {@code operator} is {@code null}
     * @throws IllegalArgumentException if {@code propName} is {@code null}/empty/blank, {@code values} is {@code null}/empty
     *                                  or contains {@code null},
     *                                  or {@code operator} is neither {@link Operator#IN} nor {@link Operator#NOT_IN},
     *                                  or if any element is a {@link Condition} other than a non-blank {@link SqlExpression}
     *                                  or a scalar {@link SubQuery} (predicates, clauses, {@link Criteria}, JOIN/ON/USING
     *                                  connectors and {@link All}/{@link Any}/{@link Some} quantified operands are all rejected),
     *                                  if a scalar {@link SubQuery} has a known, non-wildcard projection containing
     *                                  multiple columns, or if an element is a cyclic object array
     */
    protected AbstractIn(final String propName, final Operator operator, final Collection<?> values) {
        super(validateOperator(operator));

        checkPropName(propName);
        N.checkArgNotNull(values, cs.values);

        final List<Object> valuesCopy = new ArrayList<>(values.size());

        // Snapshot array/Date/Calendar elements like Binary.IN so the caller's later mutations cannot
        // desync this condition's SQL/parameters/hashCode from its equality.
        for (final Object value : values) {
            valuesCopy.add(snapshotMutableValue(value));
        }

        N.checkArgNotEmpty(valuesCopy, cs.values);
        rejectNullElements(valuesCopy, "values");
        validateNonQuantifiedValueOperands(valuesCopy, "values");

        this.propNames = ImmutableList.wrap(Collections.singletonList(propName));
        this.rowValueConstructor = false;
        this.rebuildPerCall = containsSnapshotMutableValue(valuesCopy, false);
        // Freeze the list like Binary.IN and the row-value path so a future internal mutator
        // cannot desync memoized parameters() / values() from the stored membership list.
        this.values = Collections.unmodifiableList(valuesCopy);
    }

    /**
     * Creates a new row value constructor IN or NOT IN condition, rendered as
     * {@code (p1, p2) IN ((v1a, v1b), (v2a, v2b), ...)}. Each element of {@code valueRows} is a row whose
     * width must equal {@code propNames.size()}. A singleton {@code propNames} collection is valid and
     * renders as {@code (p1) IN ((v1), (v2), ...)}. A row may be supplied in any of the following forms:
     * <ul>
     *   <li>a {@link Collection} or other {@link Iterable} of exactly {@code propNames.size()} elements,
     *       taken positionally;</li>
     *   <li>an object array ({@code Object[]}) of exactly {@code propNames.size()} elements, taken
     *       positionally;</li>
     *   <li>a {@link Map} containing every property name as a key, whose values are looked up by property name;
     *       or</li>
     *   <li>a bean whose property values are read by property name.</li>
     * </ul>
     * Both the property names and each row are copied internally, so later mutations to the supplied
     * collections do not affect this condition. Array, {@link java.util.Date} and {@link java.util.Calendar}
     * tuple elements are snapshotted (deep-copied) at construction and exposed as defensive copies by
     * {@link #values()} and {@link #parameters()}, as in the single-column form; other application-defined
     * values are retained by reference. Individual row values may be literal values, {@link SqlExpression}s
     * or scalar {@link SubQuery}s; the latter two have their parameters spliced into {@link #parameters()}.
     * Any other {@link Condition} element is rejected.
     *
     * <p>Every resolved tuple element must be non-{@code null}. Missing map keys are reported separately
     * from explicitly mapped {@code null} values so a property-name typo cannot silently change SQL
     * three-valued-logic behavior. A bean row that does not expose a requested property is rejected.</p>
     *
     * @param propNames the property/column names (must not be {@code null} or empty and must not contain
     *                  {@code null}, empty, or blank names)
     * @param operator the operator ({@link Operator#IN} or {@link Operator#NOT_IN})
     * @param valueRows the collection of value rows (must not be {@code null} or empty); each row must be
     *               non-{@code null} and resolve to exactly {@code propNames.size()} non-{@code null} values.
     *               A row may be a {@link Collection}, {@link Iterable}, object array,
     *               {@link Map} or bean
     * @throws NullPointerException if {@code operator} is {@code null}
     * @throws IllegalArgumentException if {@code operator} is neither {@link Operator#IN} nor {@link Operator#NOT_IN},
     *                                  if {@code propNames} is {@code null}/empty or contains any {@code null}, empty, or blank name,
     *                                  if {@code valueRows} is {@code null}/empty, if any row is {@code null} or of an
     *                                  unsupported type, if a positional row's width does not match {@code propNames.size()},
     *                                  if a map row is missing a requested key, if a tuple element is {@code null},
     *                                  if a bean row does not expose a requested property, if any tuple element is a
     *                                  {@link Condition} other than a non-blank {@link SqlExpression} or a scalar
     *                                  {@link SubQuery} (predicates, clauses, {@link Criteria}, JOIN/ON/USING connectors
     *                                  and {@link All}/{@link Any}/{@link Some} quantified operands are all rejected),
     *                                  if a scalar {@link SubQuery} has a known, non-wildcard projection containing
     *                                  multiple columns, or if a tuple element is a cyclic object array
     */
    protected AbstractIn(final Collection<String> propNames, final Operator operator, final Collection<?> valueRows) {
        super(validateOperator(operator));

        this.propNames = copyAndValidatePropNames(propNames);
        N.checkArgNotNull(valueRows, cs.valueRows);

        this.rowValueConstructor = true;

        final List<?> valueRowsCopy = new ArrayList<>(valueRows);
        N.checkArgNotEmpty(valueRowsCopy, cs.valueRows);

        final int arity = this.propNames.size();
        final List<List<Object>> copy = new ArrayList<>(valueRowsCopy.size());

        int rowIndex = 0;

        for (final Object row : valueRowsCopy) {
            N.checkArgNotNull(row, "value row");

            // Each tuple is wrapped unmodifiable so values() is immutable in depth, not just at the
            // outer ImmutableList level (a mutated tuple would silently desync the memoized parameters).
            final List<Object> tuple = toRowTuple(row, this.propNames, arity);
            final String rowPath = "valueRows[" + rowIndex++ + "]";

            // Snapshot array/Date/Calendar tuple elements like the single-column form and Binary.IN.
            for (int i = 0, n = tuple.size(); i < n; i++) {
                tuple.set(i, snapshotMutableValue(tuple.get(i)));
            }

            rejectNullElements(tuple, rowPath);
            validateNonQuantifiedValueOperands(tuple, rowPath);
            copy.add(Collections.unmodifiableList(tuple));
        }

        this.rebuildPerCall = containsSnapshotMutableValue(copy, true);
        this.values = copy;
    }

    private static Operator validateOperator(final Operator operator) {
        if (operator == null) {
            throw new NullPointerException("operator");
        }

        if (operator != Operator.IN && operator != Operator.NOT_IN) {
            throw new IllegalArgumentException("Only IN and NOT_IN are supported: " + operator);
        }

        return operator;
    }

    /**
     * Normalizes a single row-value row into a list of exactly {@code arity} values, ordered to
     * match {@code propNames}. See {@link #AbstractIn(Collection, Operator, Collection)} for the accepted
     * row forms.
     */
    private static List<Object> toRowTuple(final Object row, final Collection<String> propNames, final int arity) {
        if (row instanceof Map) {
            final Map<?, ?> map = (Map<?, ?>) row;
            final List<Object> tuple = new ArrayList<>(arity);

            for (final String propName : propNames) {
                if (!map.containsKey(propName)) {
                    throw new IllegalArgumentException("Map value row is missing required property key: " + propName);
                }

                tuple.add(map.get(propName));
            }

            return tuple;
        } else if (row instanceof Object[]) {
            final Object[] array = (Object[]) row;
            checkRowWidth(array.length, arity, false);

            final List<Object> tuple = new ArrayList<>(arity);
            Collections.addAll(tuple, array);

            return tuple;
        } else if (row instanceof Iterable) {
            final List<Object> tuple = new ArrayList<>(arity);
            final Iterator<?> iter = ((Iterable<?>) row).iterator();

            // Read at most one element beyond the expected width. This both detects an oversized
            // tuple and guarantees that an infinite Iterable fails promptly instead of hanging.
            while (tuple.size() <= arity && iter.hasNext()) {
                tuple.add(iter.next());
            }

            if (row instanceof Collection) {
                // A Collection knows its exact size, so report it instead of the truncated lower bound.
                checkRowWidth(((Collection<?>) row).size(), arity, false);
            }

            // The tuple was filled from the iterator, which a live or misbehaving Collection need not keep in
            // step with size(); the copied width is what gets rendered, so it must match the arity as well.
            // The loop above stops one element past the expected width, so an oversized count is a lower bound.
            checkRowWidth(tuple.size(), arity, tuple.size() > arity);

            return tuple;
        } else if (Beans.isBeanClass(row.getClass())) {
            final List<Object> tuple = new ArrayList<>(arity);

            for (final String propName : propNames) {
                tuple.add(Beans.getPropValue(row, propName));
            }

            return tuple;
        } else {
            throw new IllegalArgumentException(
                    "Each row-value row must be a Collection, Iterable, object array, Map or bean, but found: " + row.getClass().getName());
        }
    }

    /**
     * @param truncated {@code true} when {@code actual} is only a lower bound because the row was read from an
     *                  {@link Iterable} that was not consumed past {@code arity + 1} elements
     */
    private static void checkRowWidth(final int actual, final int arity, final boolean truncated) {
        if (actual != arity) {
            final String actualDescription = truncated ? "at least " + actual : String.valueOf(actual);
            throw new IllegalArgumentException(
                    "Each value row must have exactly " + arity + " element(s) to match the number of property names, but found " + actualDescription);
        }
    }

    private static void rejectNullElements(final Collection<?> values, final String path) {
        int index = 0;

        for (final Object value : values) {
            if (value == null) {
                throw new IllegalArgumentException(path + " must not contain null elements; null found at index " + index);
            }

            index++;
        }
    }

    private static ImmutableList<String> copyAndValidatePropNames(final Collection<String> propNames) {
        N.checkArgNotNull(propNames, cs.propNames);

        final List<String> copy = new ArrayList<>(propNames);
        N.checkArgNotEmpty(copy, cs.propNames);

        for (final String propName : copy) {
            checkPropName(propName);
        }

        return ImmutableList.wrap(copy);
    }

    /**
     * Returns the property name being checked in this IN or NOT IN condition. For a row-value
     * condition this returns the first property name; prefer {@link #propNames()} in that case.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In inCond = new In("status", Arrays.asList("active", "pending"));
     * String prop = inCond.propName();   // "status"
     * }</pre>
     *
     * @return the (first) property name, or {@code null} for an uninitialized instance
     */
    public String propName() {
        return N.firstOrNullIfEmpty(propNames);
    }

    /**
     * Returns the property names checked in this IN or NOT IN condition. For a single-column condition
     * the returned collection holds exactly one name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In single = new In("status", Arrays.asList("active", "pending"));
     * Collection<String> p1 = single.propNames();   // ["status"]
     *
     * In multi = new In(Arrays.asList("first_name", "last_name"),
     *                   Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));
     * Collection<String> p2 = multi.propNames();   // ["first_name", "last_name"]
     * }</pre>
     *
     * @return non-null immutable collection of property names
     */
    public ImmutableList<String> propNames() {
        return propNames;
    }

    /**
     * Returns the values used by this IN or NOT IN condition. For a row-value condition each element
     * is itself a tuple (a list of values, one per property name).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In inCond = new In("status", Arrays.asList("active", "pending"));
     * List<?> values = inCond.values();   // ["active", "pending"]
     * }</pre>
     *
     * @return an immutable list of the values (or value tuples), or an empty immutable list for an uninitialized instance;
     *         array, {@code Date} and {@code Calendar} elements in the list are defensive copies (a fresh list is
     *         built on every call when any element is an array, {@code Date}, {@code Calendar} or nested
     *         {@link Condition}; the memoized view is reused only when every element is a plain scalar), so
     *         mutating one of these defensive copies never affects this condition. Other mutable values remain
     *         shared by reference
     */
    public ImmutableList<?> values() { //NOSONAR
        if (values == null) {
            return ImmutableList.empty();
        }

        if (rebuildPerCall) {
            return ImmutableList.wrap(copyValuesForExposure());
        }

        ImmutableList<?> view = cachedValuesView;

        if (view == null) {
            view = ImmutableList.wrap(values);
            cachedValuesView = view;
        }

        return view;
    }

    /** Mirrors {@code Binary.copyPropValueForExposure}: fresh copies of snapshot-mutable elements, identity for the rest. */
    private List<Object> copyValuesForExposure() {
        final List<Object> copy = new ArrayList<>(values.size());

        if (usesRowValueConstructor()) {
            for (final Object tuple : values) {
                final Collection<?> row = (Collection<?>) tuple;
                final List<Object> tupleCopy = new ArrayList<>(row.size());

                for (final Object value : row) {
                    tupleCopy.add(snapshotMutableValue(value));
                }

                copy.add(Collections.unmodifiableList(tupleCopy));
            }
        } else {
            for (final Object value : values) {
                copy.add(snapshotMutableValue(value));
            }
        }

        return copy;
    }

    /**
     * Detects whether a memoized {@link #values()} / {@link #parameters()} list would expose a known mutable
     * element: an array, {@code Date} or {@code Calendar}, or a nested {@link Condition} whose spliced-in
     * parameters may themselves be per-call defensive copies. Evaluated once at construction into
     * {@link #rebuildPerCall}, since the membership list is immutable afterwards.
     */
    private static boolean containsSnapshotMutableValue(final List<?> values, final boolean rowValueConstructor) {
        if (rowValueConstructor) {
            for (final Object tuple : values) {
                for (final Object value : (Collection<?>) tuple) {
                    if (isSnapshotMutableValue(value) || value instanceof Condition) {
                        return true;
                    }
                }
            }
        } else {
            for (final Object value : values) {
                if (isSnapshotMutableValue(value) || value instanceof Condition) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks whether this condition was created in row value constructor form, i.e. via
     * {@link #AbstractIn(Collection, Operator, Collection)}, where each value is a tuple matched
     * positionally against the property names and rendered as {@code (p1, p2) IN ((v1a, v1b), ...)}.
     * A scalar condition created via {@link #AbstractIn(String, Operator, Collection)} returns {@code false}.
     *
     * <p>Note that the mode is independent of the number of property names: a single-property row value
     * condition renders as {@code (p1) IN ((v1), (v2))} and carries one-element tuples in
     * {@link #values()}, while a scalar condition renders as {@code p1 IN (v1, v2)} and carries
     * plain values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In scalar = new In("status", Arrays.asList("active", "pending"));
     * boolean b1 = scalar.usesRowValueConstructor();   // false
     *
     * In rowValue = new In(Arrays.asList("firstName", "lastName"),
     *                      Arrays.asList(Arrays.asList("John", "Doe")));
     * boolean b2 = rowValue.usesRowValueConstructor(); // true
     * }</pre>
     *
     * @return {@code true} if this condition renders in row value constructor form, {@code false} for the scalar form
     */
    public boolean usesRowValueConstructor() {
        return rowValueConstructor;
    }

    /**
     * Returns the parameter values for this condition, flattened in declaration order. For a single-column
     * scalar condition the parameters are the values from {@link #values()}; for a row-value
     * condition they are the row tuples flattened row by row, column within row. Any individual value
     * that is itself a {@link Condition} (a nested sub-condition) has its parameters spliced into the
     * result in place of that value; non-{@code Condition} values are included as-is.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // String values listed in order
     * In in = new In("status", Arrays.asList("active", "pending"));
     * List<Object> p1 = in.parameters();   // ["active", "pending"]
     *
     * // Numeric values
     * In nums = new In("id", Arrays.asList(1, 2, 3));
     * List<Object> p2 = nums.parameters();   // [1, 2, 3]
     * }</pre>
     *
     * <p>The result is memoized only when every membership value is a plain scalar (neither an array,
     * {@code Date}, {@code Calendar} nor a nested {@link Condition}); otherwise a fresh list, holding fresh
     * defensive copies of any array/{@code Date}/{@code Calendar} values (including those spliced in from a
     * nested condition), is built on every call.</p>
     *
     * @return an immutable list of parameter values, or an empty immutable list for an uninitialized instance
     *         (e.g. created via the no-arg constructor for deserialization); array, {@code Date} and
     *         {@code Calendar} values in the list are defensive copies (the list is rebuilt on every call in
     *         that case rather than memoized), so mutating one of these defensive copies never affects this condition
     *         or a later call. Other mutable values remain shared by reference
     */
    @Override
    public ImmutableList<Object> parameters() {
        if (rebuildPerCall) {
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
     * Builds the parameter list returned by {@link #parameters()}. The result is memoized unless a membership
     * value is an array, {@code Date}, {@code Calendar} or a nested {@link Condition}, in which case it is
     * rebuilt on every call so each caller receives fresh defensive copies.
     */
    private ImmutableList<Object> computeParameters() {
        if (values == null) {
            return ImmutableList.empty();
        }

        final List<Object> parameters = new ArrayList<>(values.size());

        if (usesRowValueConstructor()) {
            for (final Object tuple : values) {
                for (final Object value : (Collection<?>) tuple) {
                    addParameter(parameters, value);
                }
            }
        } else {
            for (final Object value : values) {
                addParameter(parameters, value);
            }
        }

        return ImmutableList.wrap(parameters);
    }

    private static void addParameter(final List<Object> parameters, final Object value) {
        if (value instanceof Condition) {
            parameters.addAll(((Condition) value).parameters());
        } else {
            parameters.add(snapshotMutableValue(value));
        }
    }

    /**
     * Converts this condition to its SQL representation.
     * The format is {@code propName IN (v1, v2, ...)} for {@link In}, or
     * {@code propName NOT IN (v1, v2, ...)} for {@link NotIn}. If the operator is {@code null}
     * (only possible for an uninitialized instance), the literal {@code "null"} is rendered
     * in place of the operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // String values are single-quoted
     * In in = new In("status", Arrays.asList("active", "pending"));
     * String s1 = in.toSql(NamingPolicy.NO_CHANGE);   // "status IN ('active', 'pending')"
     *
     * // NotIn uses the NOT IN operator
     * NotIn notIn = new NotIn("status", Arrays.asList("active", "pending"));
     * String s2 = notIn.toSql(NamingPolicy.NO_CHANGE);   // "status NOT IN ('active', 'pending')"
     *
     * // Numeric values are unquoted; a null naming policy uses NO_CHANGE
     * In nums = new In("id", Arrays.asList(1, 2, 3));
     * String s3 = nums.toSql(null);   // "id IN (1, 2, 3)"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply to the property name(s);
     *                     if {@code null}, {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return the SQL representation, e.g., {@code "status IN ('active', 'pending')"} or, for a
     *         multi-column condition, {@code "(first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))"}
     * @throws IllegalArgumentException if a scalar or row value is a {@code NaN} or infinite {@link Float}/{@link Double},
     *                                  or a {@link Number} whose text is not a valid numeric literal
     */
    @Override
    public String toSql(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Operator op = operator();
        final String opStr = op == null ? Strings.NULL : op.toString();

        final int size = values == null ? 0 : values.size();
        final StringBuilder sb = new StringBuilder(16 + (size << 3));

        if (usesRowValueConstructor()) {
            sb.append(SK._PARENTHESIS_L);
            int p = 0;
            for (final String propName : propNames) {
                if (p++ > 0) {
                    sb.append(SK.COMMA_SPACE);
                }
                sb.append(QueryUtil.convertIdentifier(propName, effectiveNamingPolicy));
            }
            sb.append(SK._PARENTHESIS_R).append(SK._SPACE).append(opStr).append(SK.SPACE_PARENTHESIS_L);

            if (values != null) {
                for (int i = 0; i < size; i++) {
                    if (i > 0) {
                        sb.append(SK.COMMA_SPACE);
                    }
                    sb.append(SK._PARENTHESIS_L);
                    int c = 0;
                    for (final Object value : (Collection<?>) values.get(i)) {
                        if (c++ > 0) {
                            sb.append(SK.COMMA_SPACE);
                        }
                        sb.append(formatParameter(value, effectiveNamingPolicy));
                    }
                    sb.append(SK._PARENTHESIS_R);
                }
            }

            sb.append(SK._PARENTHESIS_R);
            return sb.toString();
        }

        sb.append(QueryUtil.convertIdentifier(propName(), effectiveNamingPolicy)).append(SK._SPACE).append(opStr).append(SK.SPACE_PARENTHESIS_L);

        if (values != null) {
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    sb.append(SK.COMMA_SPACE);
                }
                sb.append(formatParameter(values.get(i), effectiveNamingPolicy));
            }
        }

        sb.append(SK._PARENTHESIS_R);
        return sb.toString();
    }

    /**
     * Generates the hash code for this condition.
     * Array, {@code Date} and {@code Calendar} elements are snapshotted at construction, so their contribution
     * is stable; other application-defined elements are retained by reference, so the hash is recomputed on
     * every call rather than memoized, and a mutable element of that kind cannot leave this condition with a
     * stale hash that disagrees with an equal, newly constructed condition. Array elements hash by content.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Same property/operator/values -> equal hash codes
     * In a = new In("status", Arrays.asList("active", "pending"));
     * In b = new In("status", Arrays.asList("active", "pending"));
     * boolean same = a.hashCode() == b.hashCode();   // true
     * }</pre>
     *
     * @return the hash code based on property name(s), operator, row-value mode, and values
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + N.hashCode(propNames);
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        h = (h * 31) + (rowValueConstructor ? 1231 : 1237);
        // Deep-walk membership values so array (and nested collection) elements match Binary's
        // scalar array contract rather than List.equals identity semantics for array elements.
        h = (h * 31) + deepMembershipHashCode(values);

        return h == 0 ? 1 : h;
    }

    /**
     * Checks if this condition is equal to another object.
     * Two conditions are equal if they have the exact same runtime class, property name(s),
     * operator, row-value mode, and values list.
     *
     * <p>Membership values are compared deeply: array elements use content equality (the same
     * contract as a scalar array RHS on {@link Binary}), and nested collections (row-value tuples)
     * are walked element-wise.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In a = new In("status", Arrays.asList("active", "pending"));
     * In b = new In("status", Arrays.asList("active", "pending"));
     * boolean eq = a.equals(b);   // true
     *
     * // Different values -> not equal
     * boolean neValues = a.equals(new In("status", Arrays.asList("active")));   // false
     *
     * // Different operator (IN vs NOT IN) -> not equal
     * boolean neOp = a.equals(new NotIn("status", Arrays.asList("active", "pending")));   // false
     *
     * // Non-AbstractIn object -> not equal
     * boolean neType = a.equals("status");   // false
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

        final AbstractIn other = (AbstractIn) obj;
        return rowValueConstructor == other.rowValueConstructor && N.equals(propNames, other.propNames) && N.equals(operator, other.operator)
                && deepMembershipEquals(values, other.values);
    }

    /**
     * Deep equality for membership values / row tuples. Collections are compared element-wise so
     * array members use content equality ({@link N#deepEquals(Object, Object)}, aligned with the
     * {@link N#deepHashCode(Object)} leaf in {@link #deepMembershipHashCode}) rather than reference
     * identity from {@link List#equals(Object)}.
     */
    private static boolean deepMembershipEquals(final Object left, final Object right) {
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
                if (!deepMembershipEquals(leftIter.next(), rightIter.next())) {
                    return false;
                }
            }

            return true;
        }

        return N.deepEquals(left, right);
    }

    /**
     * Deep hash for membership values / row tuples, aligned with {@link #deepMembershipEquals}.
     */
    private static int deepMembershipHashCode(final Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof final Collection<?> values) {
            int h = 1;

            for (final Object element : values) {
                h = (31 * h) + deepMembershipHashCode(element);
            }

            return h;
        }

        return N.deepHashCode(value);
    }
}
