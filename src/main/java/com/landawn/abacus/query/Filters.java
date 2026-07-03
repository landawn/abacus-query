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

package com.landawn.abacus.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.query.condition.All;
import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.Any;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Binary;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.CrossJoin;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.Except;
import com.landawn.abacus.query.condition.Exists;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.FullJoin;
import com.landawn.abacus.query.condition.GreaterThan;
import com.landawn.abacus.query.condition.GreaterThanOrEqual;
import com.landawn.abacus.query.condition.GroupBy;
import com.landawn.abacus.query.condition.Having;
import com.landawn.abacus.query.condition.In;
import com.landawn.abacus.query.condition.InSubQuery;
import com.landawn.abacus.query.condition.InnerJoin;
import com.landawn.abacus.query.condition.Intersect;
import com.landawn.abacus.query.condition.Is;
import com.landawn.abacus.query.condition.IsInfinite;
import com.landawn.abacus.query.condition.IsNaN;
import com.landawn.abacus.query.condition.IsNot;
import com.landawn.abacus.query.condition.IsNotInfinite;
import com.landawn.abacus.query.condition.IsNotNaN;
import com.landawn.abacus.query.condition.IsNotNull;
import com.landawn.abacus.query.condition.IsNull;
import com.landawn.abacus.query.condition.Join;
import com.landawn.abacus.query.condition.Junction;
import com.landawn.abacus.query.condition.LeftJoin;
import com.landawn.abacus.query.condition.LessThan;
import com.landawn.abacus.query.condition.LessThanOrEqual;
import com.landawn.abacus.query.condition.Like;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.Minus;
import com.landawn.abacus.query.condition.NamedProperty;
import com.landawn.abacus.query.condition.NaturalJoin;
import com.landawn.abacus.query.condition.Not;
import com.landawn.abacus.query.condition.NotBetween;
import com.landawn.abacus.query.condition.NotEqual;
import com.landawn.abacus.query.condition.NotExists;
import com.landawn.abacus.query.condition.NotIn;
import com.landawn.abacus.query.condition.NotInSubQuery;
import com.landawn.abacus.query.condition.NotLike;
import com.landawn.abacus.query.condition.On;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Or;
import com.landawn.abacus.query.condition.OrderBy;
import com.landawn.abacus.query.condition.RightJoin;
import com.landawn.abacus.query.condition.Some;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Union;
import com.landawn.abacus.query.condition.UnionAll;
import com.landawn.abacus.query.condition.Using;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.EntityId;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;

/**
 * Factory class for creating SQL {@link Condition} objects used in query construction.
 * Provides methods for all standard SQL comparison, logical, pattern matching, and subquery operations.
 *
 * <p><b>Warning:</b> Value-based methods (e.g., {@code equal}, {@code in}, {@code between}) capture structured
 * values that are later rendered according to the {@link SqlBuilder} policy. APIs that accept raw SQL fragments
 * (e.g., {@code expr(...)}) append those fragments verbatim and must not be built from untrusted data.</p>
 *
 * <p><b>API Categories:</b></p>
 * <ul>
 *   <li><b>Comparison:</b> {@code equal/eq}, {@code notEqual/ne}, {@code greaterThan/gt}, {@code lessThan/lt},
 *       {@code greaterThanOrEqual/ge}, {@code lessThanOrEqual/le}</li>
 *   <li><b>Range/Collection:</b> {@code between}, {@code notBetween}, {@code in}, {@code notIn}
 *       (range/keyword factories intentionally have <b>no</b> short alias; the former {@code bt}
 *       alias for {@code between} was removed)</li>
 *   <li><b>Pattern Matching:</b> {@code like}, {@code notLike}, {@code contains}, {@code startsWith}, {@code endsWith}</li>
 *   <li><b>Null Checks:</b> {@code isNull}, {@code isNotNull}</li>
 *   <li><b>Logical:</b> {@code and}, {@code or}, {@code not}</li>
 *   <li><b>Subquery:</b> {@code exists}, {@code notExists}, {@code in(String, SubQuery)}, {@code notIn(String, SubQuery)}</li>
 *   <li><b>Joins:</b> {@code join}, {@code leftJoin}, {@code rightJoin}, {@code innerJoin},
 *       {@code fullJoin}, {@code crossJoin}, {@code naturalJoin}</li>
 *   <li><b>Clauses:</b> {@code where}, {@code having}, {@code groupBy}, {@code orderBy},
 *       {@code limit}, {@code on}, {@code using}</li>
 *   <li><b>Set operations:</b> {@code union}, {@code unionAll}, {@code intersect}, {@code except}, {@code minus}</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic conditions
 * Condition active = Filters.equal("status", "ACTIVE");
 * Condition adult = Filters.greaterThanOrEqual("age", 18);
 *
 * // Composite conditions
 * Condition filter = Filters.and(
 *     Filters.equal("department", "Engineering"),
 *     Filters.or(
 *         Filters.greaterThan("salary", 75000),
 *         Filters.equal("level", "Senior")
 *     ),
 *     Filters.isNotNull("manager_id")
 * );
 *
 * // Collection and range
 * Condition statusFilter = Filters.in("status", Arrays.asList("PENDING", "APPROVED"));
 * Condition priceRange = Filters.between("price", 100.0, 500.0);
 * }</pre>
 *
 * @see Condition
 * @see Expression
 * @see SqlBuilder
 */
public class Filters {
    /**
     * Expression representing a question mark literal ("?") for use in parameterized SQL queries.
     * This constant is used when creating conditions with placeholders for prepared statements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a parameterized condition; the parameterless equal(String) overload uses QME internally
     * Equal condition = Filters.equal("age");
     * }</pre>
     */
    public static final Expression QME = Expression.of(SK.QUESTION_MARK);

    /** Expression representing {@code "1 < 2"} which always evaluates to {@code true}. */
    private static final Expression ALWAYS_TRUE = Expression.of("1 < 2");

    /** Expression representing {@code "1 > 2"} which always evaluates to {@code false}. */
    private static final Expression ALWAYS_FALSE = Expression.of("1 > 2");

    private Filters() {
        // utility class
    }

    /**
     * Returns a condition that always evaluates to true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition condition = includeFilter ? Filters.equal("status", "active")
     *                                    : Filters.alwaysTrue();
     * // Filters.alwaysTrue() renders the literal: 1 < 2
     * }</pre>
     *
     * @return an {@link Expression} that always evaluates to true (1 &lt; 2)
     * @deprecated dangerous; could silently bypass all filtering, returning all rows.
     *             Avoid using this method; restructure the conditional logic instead.
     */
    @Deprecated
    public static Expression alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     * Returns a condition that always evaluates to false.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition condition = excludeAll ? Filters.alwaysFalse()
     *                                  : Filters.equal("status", "active");
     * // Filters.alwaysFalse() renders the literal: 1 > 2
     * }</pre>
     *
     * @return an {@link Expression} that always evaluates to false (1 &gt; 2)
     * @deprecated dangerous; could silently return zero rows.
     *             Avoid using this method; restructure the conditional logic instead.
     */
    @Deprecated
    public static Expression alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Creates (or returns a cached) {@link NamedProperty} instance representing a property/column name.
     * This is used to reference database columns through a dedicated value object that exposes a
     * fluent condition-building API. Instances are pooled by {@link NamedProperty#of(String)} so
     * repeated calls with the same name return the same instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamedProperty prop = Filters.namedProperty("user_name");
     * // Renders as: user_name
     * }</pre>
     *
     * @param propName the name of the property/column (must not be {@code null}, empty, or blank)
     * @return a {@link NamedProperty} instance
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    @Beta
    public static NamedProperty namedProperty(final String propName) {
        return NamedProperty.of(propName);
    }

    /**
     * Creates an {@link Expression} from a string literal.
     * This allows for custom SQL expressions to be included in queries.
     *
     * <p><b>Warning:</b> The literal is appended verbatim into the generated SQL. Do not pass
     * unsanitized user input — use parameterized condition factories (e.g. {@link #equal(String, Object)})
     * instead to avoid SQL injection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression expr = Filters.expr("UPPER(name) = 'JOHN'");
     * // SQL fragment: UPPER(name) = 'JOHN'
     * }</pre>
     *
     * @param literal the SQL expression as a string (must not be {@code null})
     * @return an {@link Expression} instance
     * @throws IllegalArgumentException if {@code literal} is {@code null}
     */
    public static Expression expr(final String literal) {
        return Expression.of(literal);
    }

    /**
     * Creates a negation condition that represents the logical {@code NOT} of the provided condition.
     *
     * <p>This method creates a {@link Not} condition that inverts the result of the wrapped condition.
     * It can be used to negate any other condition type, such as {@link Equal}, {@link Like}, {@link In}, {@link Between}, etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a NOT LIKE condition
     * Like likeCondition = Filters.like("name", "%test%");
     * Not notLike = Filters.not(likeCondition);
     *
     * // Create a NOT IN condition
     * Not notIn = Filters.not(Filters.in("status", Arrays.asList("inactive", "deleted")));
     *
     * // Create a NOT BETWEEN condition
     * Not notBetween = Filters.not(Filters.between("age", 18, 65));
     *
     * // Create a complex negated condition
     * Not complexNot = Filters.not(Filters.and(
     *     Filters.equal("status", "active"),
     *     Filters.greaterThan("age", 18),
     *     Filters.like("email", "%@company.com")
     * ));
     * }</pre>
     *
     * @param cond the condition to negate (must not be {@code null} and must be a composable condition)
     * @return a {@link Not} condition that wraps and negates the provided condition
     * @throws IllegalArgumentException if {@code cond} is {@code null} or is a clause/join condition (e.g., {@code ON}/{@code USING}) that cannot be composed
     * @see Not
     * @see Condition
     */
    public static Not not(final Condition cond) {
        return new Not(cond);
    }

    /**
     * Creates a {@link Binary} condition with the specified property name, operator, and value.
     * This is a general factory for creating conditions with a binary comparison/membership
     * {@link Operator}, useful when one of the convenience factories (e.g. {@link #equal(String, Object)},
     * {@link #greaterThan(String, Object)}) does not cover the desired operator.
     *
     * <p>The {@code operator} must be a valid binary comparison or membership operator: one of
     * {@link Operator#EQUAL}, {@link Operator#NOT_EQUAL}, {@link Operator#NOT_EQUAL_ANSI},
     * {@link Operator#GREATER_THAN}, {@link Operator#GREATER_THAN_OR_EQUAL}, {@link Operator#LESS_THAN},
     * {@link Operator#LESS_THAN_OR_EQUAL}, {@link Operator#LIKE}, {@link Operator#NOT_LIKE},
     * {@link Operator#IS}, {@link Operator#IS_NOT}, {@link Operator#IN}, or {@link Operator#NOT_IN}.
     * Structural operators (such as {@code WHERE}, {@code JOIN}, {@code AND}, {@code OR},
     * {@code UNION}, {@code BETWEEN}, {@code EXISTS}) are <b>not</b> binary {@code propName OP value}
     * predicates and are rejected with an {@link IllegalArgumentException}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary condition = Filters.binary("price", Operator.GREATER_THAN, 100);
     * // SQL fragment: price > 100
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param operator the binary comparison/membership operator to use (must not be {@code null} and must be
     *                 a valid binary operator; structural operators are rejected)
     * @param propValue the value to compare against; may be a literal, {@code null}, or another
     *                  {@link Condition} such as a {@link SubQuery}. For an {@code IN}/{@code NOT_IN}
     *                  operator, a {@link Collection} or array value is copied defensively and must be non-empty.
     * @return a {@link Binary} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank; if {@code operator}
     *                                  is not a valid binary comparison/membership operator (e.g. a structural
     *                                  operator); or if, for an {@code IN}/{@code NOT_IN} operator, {@code propValue}
     *                                  is not a non-empty {@link Collection}, a non-empty array, or a {@link Condition}
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    public static Binary binary(final String propName, final Operator operator, final Object propValue) {
        return new Binary(propName, operator, propValue);
    }

    /**
     * Creates a parameterized {@link Binary} condition for use with prepared statements.
     * The condition uses a question mark ({@code ?}) placeholder in place of the value, which is
     * provided later when the statement is executed. This is the parameterized counterpart of
     * {@link #binary(String, Operator, Object)}, mirroring pairs such as
     * {@link #equal(String, Object)} / {@link #equal(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary condition = Filters.binary("price", Operator.GREATER_THAN);
     * // SQL fragment: price > ?
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param operator the binary comparison/membership operator to use (must not be {@code null} and must be
     *                 a valid binary operator; structural operators are rejected)
     * @return a {@link Binary} condition with a {@code ?} placeholder value
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code operator}
     *                                  is not a valid binary comparison/membership operator (e.g. a structural operator)
     * @throws NullPointerException if {@code operator} is {@code null}
     * @see #binary(String, Operator, Object)
     */
    public static Binary binary(final String propName, final Operator operator) {
        return new Binary(propName, operator, QME);
    }

    /**
     * Creates an equality condition ({@code =}) for the specified property and value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal condition = Filters.equal("username", "john_doe");
     * // SQL fragment: username = 'john_doe'
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param propValue the value to compare for equality; may be a literal, {@code null}
     *                  (renders as {@code IS NULL}), or another {@link Condition} such as a {@link SubQuery}
     * @return an {@link Equal} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static Equal equal(final String propName, final Object propValue) { //NOSONAR
        return new Equal(propName, propValue);
    }

    /**
     * Creates a parameterized equality condition for use with prepared statements.
     * The value will be represented by a question mark ({@code ?}) placeholder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal condition = Filters.equal("user_id");
     * // SQL fragment: user_id = ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link Equal} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static Equal equal(final String propName) {//NOSONAR
        return equal(propName, QME);
    }

    /**
     * Creates an equality condition ({@code =}) for the specified property and value.
     * This is a shorthand alias for {@link #equal(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal condition = Filters.eq("status", "active");
     * // SQL fragment: status = 'active'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare for equality
     * @return an {@link Equal} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static Equal eq(final String propName, final Object propValue) {
        return equal(propName, propValue);
    }

    /**
     * Creates a parameterized equality condition for use with prepared statements.
     * This is a shorthand alias for {@link #equal(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal condition = Filters.eq("email");
     * // SQL fragment: email = ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link Equal} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static Equal eq(final String propName) {
        return equal(propName);
    }

    /**
     * Creates an {@code OR} condition from a map where each entry represents a property-value equality check
     * across <b>different</b> columns/properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> props = new LinkedHashMap<>();
     * props.put("name", "John");
     * props.put("email", "john@example.com");
     * Or condition = Filters.anyEqual(props);
     * // SQL fragment: ((name = 'John') OR (email = 'john@example.com'))
     * }</pre>
     *
     * <p>Note: despite the similar name, this is a different operation from
     * {@link NamedProperty#equalsAny(Object...)}: {@code anyEqual} tests one value for each of
     * several properties, while {@code equalsAny} tests several candidate values for one property.</p>
     *
     * @param props map of property names to values (must not be empty)
     * @return an {@link Or} condition
     * @throws IllegalArgumentException if {@code props} is {@code null} or empty
     * @see NamedProperty#equalsAny(Object...)
     */
    public static Or anyEqual(final Map<String, ?> props) {
        N.checkArgNotEmpty(props, "props");

        final Iterator<? extends Map.Entry<String, ?>> propIter = props.entrySet().iterator();

        if (props.size() == 1) {
            final Map.Entry<String, ?> prop = propIter.next();
            return or(equal(prop.getKey(), prop.getValue()));
        } else if (props.size() == 2) {
            final Map.Entry<String, ?> prop1 = propIter.next();
            final Map.Entry<String, ?> prop2 = propIter.next();
            return equal(prop1.getKey(), prop1.getValue()).or(equal(prop2.getKey(), prop2.getValue()));
        } else {
            final Condition[] conds = new Condition[props.size()];
            Map.Entry<String, ?> prop = null;

            for (int i = 0, size = props.size(); i < size; i++) {
                prop = propIter.next();
                conds[i] = Filters.equal(prop.getKey(), prop.getValue());
            }

            return or(conds);
        }
    }

    /**
     * Creates an {@code OR} condition from an entity object using all its properties.
     * Each property of the entity will be included as an equality check in the {@code OR} condition
     * across <b>different</b> columns/properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "john@example.com");
     * Or condition = Filters.anyEqual(user);
     * // SQL fragment: ((name = 'John') OR (email = 'john@example.com'))
     * }</pre>
     *
     * @param entity the entity object whose properties will be used
     * @return an {@link Or} condition
     * @throws IllegalArgumentException if {@code entity} is {@code null}
     */
    public static Or anyEqual(final Object entity) {
        N.checkArgNotNull(entity, "entity");

        return anyEqual(entity, QueryUtil.getSelectPropNames(entity.getClass(), false, null));
    }

    /**
     * Creates an {@code OR} condition from an entity object using only the specified properties.
     * Each property forms an equality check in the {@code OR} condition
     * across <b>different</b> columns/properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "john@example.com", 25);
     * Or condition = Filters.anyEqual(user, Arrays.asList("name", "email"));
     * // Only uses name and email, ignores age
     * }</pre>
     *
     * @param entity the entity object
     * @param selectPropNames the property names to include (must not be empty)
     * @return an {@link Or} condition
     * @throws IllegalArgumentException if {@code entity} is {@code null} or {@code selectPropNames} is {@code null} or empty
     */
    public static Or anyEqual(final Object entity, final Collection<String> selectPropNames) {
        N.checkArgNotNull(entity, "entity");
        N.checkArgNotEmpty(selectPropNames, "selectPropNames"); //NOSONAR

        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entity.getClass());
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return or(equal(propName, entityInfo.getPropValue(entity, propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return equal(propName1, entityInfo.getPropValue(entity, propName1)).or(equal(propName2, entityInfo.getPropValue(entity, propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = Filters.equal(propName, entityInfo.getPropValue(entity, propName));
            }

            return or(conds);
        }
    }

    /**
     * Creates an {@code OR} condition with two property-value pairs
     * across <b>different</b> columns/properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = Filters.anyEqual("name", "John", "email", "john@example.com");
     * // SQL fragment: ((name = 'John') OR (email = 'john@example.com'))
     * }</pre>
     *
     * @param propName1 first property name
     * @param propValue1 first property value
     * @param propName2 second property name
     * @param propValue2 second property value
     * @return an {@link Or} condition
     * @throws IllegalArgumentException if any property name is {@code null}, empty, or blank
     */
    public static Or anyEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        return equal(propName1, propValue1).or(equal(propName2, propValue2));
    }

    /**
     * Creates an {@code OR} condition with three property-value pairs
     * across <b>different</b> columns/properties.
     *
     * <p>This is the largest positional-pair overload. For more than three pairs, prefer the map-based
     * {@link #anyEqual(Map)} form (using a {@link java.util.LinkedHashMap} to preserve order): pairing
     * names and values positionally becomes increasingly error-prone as the argument count grows, since a
     * transposed name/value is not caught by the compiler.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = Filters.anyEqual("status", "active", "type", "premium", "verified", true);
     * // SQL fragment: ((status = 'active') OR (type = 'premium') OR (verified = true))
     * }</pre>
     *
     * @param propName1 first property name
     * @param propValue1 first property value
     * @param propName2 second property name
     * @param propValue2 second property value
     * @param propName3 third property name
     * @param propValue3 third property value
     * @return an {@link Or} condition
     * @throws IllegalArgumentException if any property name is {@code null}, empty, or blank
     */
    public static Or anyEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3,
            final Object propValue3) {
        return or(equal(propName1, propValue1), equal(propName2, propValue2), equal(propName3, propValue3));
    }

    /**
     * Creates an {@code AND} condition from a map where each entry represents a property-value equality check
     * across <b>different</b> columns/properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> props = new LinkedHashMap<>();
     * props.put("status", "active");
     * props.put("type", "premium");
     * And condition = Filters.allEqual(props);
     * // SQL fragment: ((status = 'active') AND (type = 'premium'))
     * }</pre>
     *
     * @param props map of property names to values (must not be empty)
     * @return an {@link And} condition
     * @throws IllegalArgumentException if {@code props} is {@code null} or empty
     */
    public static And allEqual(final Map<String, ?> props) {
        N.checkArgNotEmpty(props, "props");

        final Iterator<? extends Map.Entry<String, ?>> propIter = props.entrySet().iterator();

        if (props.size() == 1) {
            final Map.Entry<String, ?> prop = propIter.next();
            return and(equal(prop.getKey(), prop.getValue()));
        } else if (props.size() == 2) {
            final Map.Entry<String, ?> prop1 = propIter.next();
            final Map.Entry<String, ?> prop2 = propIter.next();
            return equal(prop1.getKey(), prop1.getValue()).and(equal(prop2.getKey(), prop2.getValue()));
        } else {
            final Condition[] conds = new Condition[props.size()];
            Map.Entry<String, ?> prop = null;

            for (int i = 0, size = props.size(); i < size; i++) {
                prop = propIter.next();
                conds[i] = Filters.equal(prop.getKey(), prop.getValue());
            }

            return and(conds);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> stringKeyMap(final Map<?, ?> props) {
        for (final Object propName : props.keySet()) {
            N.checkArgument(propName instanceof String, "Map keys must be String: " + propName);
        }

        return (Map<String, ?>) props;
    }

    /**
     * Creates an {@code AND} condition from an entity object using all its properties.
     * Each property of the entity will be included as an equality check in the {@code AND} condition
     * across <b>different</b> columns/properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "john@example.com", 25);
     * And condition = Filters.allEqual(user);
     * // SQL fragment: ((name = 'John') AND (email = 'john@example.com') AND (age = 25))
     * }</pre>
     *
     * @param entity the entity object whose properties will be used
     * @return an {@link And} condition
     * @throws IllegalArgumentException if {@code entity} is {@code null}
     */
    public static And allEqual(final Object entity) {
        N.checkArgNotNull(entity, "entity");

        return allEqual(entity, QueryUtil.getSelectPropNames(entity.getClass(), false, null));
    }

    /**
     * Creates an {@code AND} condition from an entity object using only the specified properties.
     * Each property forms an equality check in the {@code AND} condition
     * across <b>different</b> columns/properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "john@example.com", 25);
     * And condition = Filters.allEqual(user, Arrays.asList("email", "age"));
     * // Only uses email and age, ignores name
     * }</pre>
     *
     * @param entity the entity object
     * @param selectPropNames the property names to include (must not be empty)
     * @return an {@link And} condition
     * @throws IllegalArgumentException if {@code entity} is {@code null} or {@code selectPropNames} is {@code null} or empty
     */
    public static And allEqual(final Object entity, final Collection<String> selectPropNames) {
        N.checkArgNotNull(entity, "entity");
        N.checkArgNotEmpty(selectPropNames, "selectPropNames");

        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entity.getClass());
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return and(equal(propName, entityInfo.getPropValue(entity, propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return equal(propName1, entityInfo.getPropValue(entity, propName1)).and(equal(propName2, entityInfo.getPropValue(entity, propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = Filters.equal(propName, entityInfo.getPropValue(entity, propName));
            }

            return and(conds);
        }
    }

    /**
     * Creates an {@code AND} condition with two property-value pairs
     * across <b>different</b> columns/properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.allEqual("status", "active", "type", "premium");
     * // SQL fragment: ((status = 'active') AND (type = 'premium'))
     * }</pre>
     *
     * @param propName1 first property name
     * @param propValue1 first property value
     * @param propName2 second property name
     * @param propValue2 second property value
     * @return an {@link And} condition
     * @throws IllegalArgumentException if any property name is {@code null}, empty, or blank
     */
    public static And allEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        return equal(propName1, propValue1).and(equal(propName2, propValue2));
    }

    /**
     * Creates an {@code AND} condition with three property-value pairs
     * across <b>different</b> columns/properties.
     *
     * <p>This is the largest positional-pair overload. For more than three pairs, prefer the map-based
     * {@link #allEqual(Map)} form (using a {@link java.util.LinkedHashMap} to preserve order): pairing
     * names and values positionally becomes increasingly error-prone as the argument count grows, since a
     * transposed name/value is not caught by the compiler.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.allEqual("status", "active", "type", "premium", "verified", true);
     * // SQL fragment: ((status = 'active') AND (type = 'premium') AND (verified = true))
     * }</pre>
     *
     * @param propName1 first property name
     * @param propValue1 first property value
     * @param propName2 second property name
     * @param propValue2 second property value
     * @param propName3 third property name
     * @param propValue3 third property value
     * @return an {@link And} condition
     * @throws IllegalArgumentException if any property name is {@code null}, empty, or blank
     */
    public static And allEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3,
            final Object propValue3) {
        return and(equal(propName1, propValue1), equal(propName2, propValue2), equal(propName3, propValue3));
    }

    /**
     * Creates an {@code OR} condition from a collection of property maps or entities, where each non-null element forms an {@code AND} condition.
     * If the first non-null element is a {@link Map}, all non-null elements must be maps with {@link String} keys. Otherwise, all properties of
     * each entity will be used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<Map<String, Object>> propsSet = new LinkedHashSet<>();
     * propsSet.add(Map.of("status", "active", "type", "premium"));
     * propsSet.add(Map.of("status", "trial", "verified", true));
     * Or mapCondition = Filters.anyOfAllEqual(propsSet);
     * // Results in: (status='active' AND type='premium') OR (status='trial' AND verified=true)
     *
     * List<User> users = Arrays.asList(
     *     new User("John", "john@example.com"),
     *     new User("Jane", "jane@example.com")
     * );
     * Or entityCondition = Filters.anyOfAllEqual(users);
     * // Results in: (name='John' AND email='john@example.com') OR (name='Jane' AND email='jane@example.com')
     * }</pre>
     *
     * @param entitiesOrMaps collection of property maps or entity objects (must not be empty)
     * @return an {@link Or} condition
     * @throws IllegalArgumentException if {@code entitiesOrMaps} is {@code null} or empty, if all elements are {@code null}, if the first non-null
     *                                  element is a map and any other non-null element is not a map, or if a map key is not a {@link String}
     */
    @Beta
    public static Or anyOfAllEqual(final Collection<?> entitiesOrMaps) {
        N.checkArgNotEmpty(entitiesOrMaps, "entitiesOrMaps");

        final Object firstNonNull = N.firstNonNull(entitiesOrMaps).orElseThrow(() -> new IllegalArgumentException("All specified entities/maps are null."));

        if (firstNonNull instanceof Map) {
            final List<Condition> condList = new ArrayList<>(entitiesOrMaps.size());

            for (final Object entityOrMap : entitiesOrMaps) {
                if (entityOrMap != null) {
                    N.checkArgument(entityOrMap instanceof Map, "All non-null elements must be Map when the first non-null element is Map");
                    condList.add(allEqual(stringKeyMap((Map<?, ?>) entityOrMap)));
                }
            }

            return or(condList);
        }

        return anyOfAllEqual(entitiesOrMaps, QueryUtil.getSelectPropNames(firstNonNull.getClass(), false, null));
    }

    /**
     * Creates an {@code OR} condition from a collection of entities using only specified properties.
     * Each entity forms an {@code AND} condition with the selected properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<User> users = Arrays.asList(new User("John", "active"), new User("Jane", "trial"));
     * Or condition = Filters.anyOfAllEqual(users, Arrays.asList("name", "status"));
     * // Only uses name and status properties from each user
     * // Results in: (name = 'John' AND status = 'active') OR (name = 'Jane' AND status = 'trial')
     * }</pre>
     *
     * @param entities collection of entity objects (must not be empty)
     * @param selectPropNames the property names to include (must not be empty)
     * @return an {@link Or} condition
     * @throws IllegalArgumentException if {@code entities} or {@code selectPropNames} is empty, or all entities are null
     */
    @Beta
    public static Or anyOfAllEqual(final Collection<?> entities, final Collection<String> selectPropNames) {
        N.checkArgNotEmpty(entities, "entities");
        N.checkArgNotEmpty(selectPropNames, "selectPropNames");
        N.checkArgument(!N.allNull(entities), "All specified entities are null.");

        final List<Condition> condList = new ArrayList<>(entities.size());

        for (final Object entity : entities) {
            if (entity != null) {
                condList.add(allEqual(entity, selectPropNames));
            }
        }

        return or(condList);
    }

    /**
     * Creates a {@code BETWEEN}-like condition using greater-than ({@code gt}) and less-than ({@code lt}) comparisons.
     * The result is: {@code propName > minValue AND propName < maxValue}.
     *
     * <p>Abbreviations: {@code gt} = {@link #greaterThan(String, Object) greaterThan},
     * {@code lt} = {@link #lessThan(String, Object) lessThan}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.gtAndLt("age", 18, 65);
     * // SQL fragment: ((age > 18) AND (age < 65))
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (exclusive)
     * @param maxValue the maximum value (exclusive)
     * @return an {@link And} condition
     */
    public static And gtAndLt(final String propName, final Object minValue, final Object maxValue) {
        return gt(propName, minValue).and(lt(propName, maxValue));
    }

    /**
     * Creates a parameterized {@code BETWEEN}-like condition for prepared statements.
     * The result is: {@code propName > ? AND propName < ?}.
     *
     * <p>Abbreviations: {@code gt} = {@link #greaterThan(String) greaterThan},
     * {@code lt} = {@link #lessThan(String) lessThan}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.gtAndLt("price");
     * // SQL fragment: ((price > ?) AND (price < ?))
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link And} condition with parameter placeholders
     */
    public static And gtAndLt(final String propName) {
        return gt(propName).and(lt(propName));
    }

    /**
     * Creates a {@code BETWEEN}-like condition using greater-than-or-equal ({@code ge}) and less-than ({@code lt}) comparisons.
     * The result is: {@code propName >= minValue AND propName < maxValue}.
     *
     * <p>Abbreviations: {@code ge} = {@link #greaterThanOrEqual(String, Object) greaterThanOrEqual},
     * {@code lt} = {@link #lessThan(String, Object) lessThan}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.geAndLt("price", 100, 500);
     * // SQL fragment: ((price >= 100) AND (price < 500))
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (exclusive)
     * @return an {@link And} condition
     */
    public static And geAndLt(final String propName, final Object minValue, final Object maxValue) {
        return ge(propName, minValue).and(lt(propName, maxValue));
    }

    /**
     * Creates a parameterized {@code BETWEEN}-like condition for prepared statements.
     * The result is: {@code propName >= ? AND propName < ?}.
     *
     * <p>Abbreviations: {@code ge} = {@link #greaterThanOrEqual(String) greaterThanOrEqual},
     * {@code lt} = {@link #lessThan(String) lessThan}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.geAndLt("score");
     * // SQL fragment: ((score >= ?) AND (score < ?))
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link And} condition with parameter placeholders
     */
    public static And geAndLt(final String propName) {
        return ge(propName).and(lt(propName));
    }

    /**
     * Creates a {@code BETWEEN}-like condition using greater-than-or-equal ({@code ge}) and less-than-or-equal ({@code le}) comparisons.
     * The result is: {@code propName >= minValue AND propName <= maxValue}.
     *
     * <p>Abbreviations: {@code ge} = {@link #greaterThanOrEqual(String, Object) greaterThanOrEqual},
     * {@code le} = {@link #lessThanOrEqual(String, Object) lessThanOrEqual}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.geAndLe("date", "2023-01-01", "2023-12-31");
     * // SQL fragment: ((date >= '2023-01-01') AND (date <= '2023-12-31'))
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (inclusive)
     * @return an {@link And} condition
     */
    public static And geAndLe(final String propName, final Object minValue, final Object maxValue) {
        return ge(propName, minValue).and(le(propName, maxValue));
    }

    /**
     * Creates a parameterized {@code BETWEEN}-like condition for prepared statements.
     * The result is: {@code propName >= ? AND propName <= ?}.
     *
     * <p>Abbreviations: {@code ge} = {@link #greaterThanOrEqual(String) greaterThanOrEqual},
     * {@code le} = {@link #lessThanOrEqual(String) lessThanOrEqual}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.geAndLe("amount");
     * // SQL fragment: ((amount >= ?) AND (amount <= ?))
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link And} condition with parameter placeholders
     */
    public static And geAndLe(final String propName) {
        return ge(propName).and(le(propName));
    }

    /**
     * Creates a {@code BETWEEN}-like condition using greater-than ({@code gt}) and less-than-or-equal ({@code le}) comparisons.
     * The result is: {@code propName > minValue AND propName <= maxValue}.
     *
     * <p>Abbreviations: {@code gt} = {@link #greaterThan(String, Object) greaterThan},
     * {@code le} = {@link #lessThanOrEqual(String, Object) lessThanOrEqual}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.gtAndLe("score", 0, 100);
     * // SQL fragment: ((score > 0) AND (score <= 100))
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (exclusive)
     * @param maxValue the maximum value (inclusive)
     * @return an {@link And} condition
     */
    public static And gtAndLe(final String propName, final Object minValue, final Object maxValue) {
        return gt(propName, minValue).and(le(propName, maxValue));
    }

    /**
     * Creates a parameterized {@code BETWEEN}-like condition for prepared statements.
     * The result is: {@code propName > ? AND propName <= ?}.
     *
     * <p>Abbreviations: {@code gt} = {@link #greaterThan(String) greaterThan},
     * {@code le} = {@link #lessThanOrEqual(String) lessThanOrEqual}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.gtAndLe("temperature");
     * // SQL fragment: ((temperature > ?) AND (temperature <= ?))
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link And} condition with parameter placeholders
     */
    public static And gtAndLe(final String propName) {
        return gt(propName).and(le(propName));
    }

    /**
     * Converts an {@link EntityId} to an {@link And} condition where each key-value pair becomes an equality check.
     * {@link EntityId} typically represents a composite primary key.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntityId id = EntityId.of("companyId", 1, "userId", 100);
     * And condition = Filters.idToCond(id);
     * // SQL fragment: ((companyId = 1) AND (userId = 100))
     * // (EntityId orders its keys alphabetically, regardless of insertion order)
     * }</pre>
     *
     * @param entityId the {@link EntityId} containing key-value pairs (must not be null)
     * @return an {@link And} condition
     * @throws IllegalArgumentException if {@code entityId} is null or contains no keys
     */
    public static And idToCond(final EntityId entityId) {
        N.checkArgNotNull(entityId, "entityId");

        final Collection<String> selectPropNames = entityId.keySet();
        N.checkArgNotEmpty(selectPropNames, "entityId");
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return and(equal(propName, entityId.get(propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return equal(propName1, entityId.get(propName1)).and(equal(propName2, entityId.get(propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = Filters.equal(propName, entityId.get(propName));
            }

            return and(conds);
        }
    }

    /**
     * Converts a collection of {@link EntityId}s to an {@link Or} condition where each {@link EntityId} becomes an {@link And} condition.
     * Useful for querying multiple entities by their composite keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<EntityId> ids = Arrays.asList(
     *     EntityId.of("companyId", 1, "userId", 100),
     *     EntityId.of("companyId", 2, "userId", 200)
     * );
     * Or condition = Filters.idToCond(ids);
     * // Results in: ((((companyId = 1) AND (userId = 100))) OR (((companyId = 2) AND (userId = 200))))
     * }</pre>
     *
     * @param entityIds collection of {@link EntityId}s (must not be {@code null} or empty)
     * @return an {@link Or} condition
     * @throws IllegalArgumentException if {@code entityIds} is {@code null}, empty, or contains an
     *         {@link EntityId} with no keys
     */
    public static Or idToCond(final Collection<? extends EntityId> entityIds) {
        N.checkArgNotEmpty(entityIds, "entityIds");

        final Iterator<? extends EntityId> iter = entityIds.iterator();
        final Condition[] conds = new Condition[entityIds.size()];

        for (int i = 0, size = entityIds.size(); i < size; i++) {
            conds[i] = Filters.idToCond(iter.next());
        }

        return Filters.or(conds);
    }

    /**
     * Converts an {@link EntityId} to an {@link And} condition where each key-value pair becomes an equality check.
     * {@link EntityId} typically represents a composite primary key.
     *
     * @param entityId the {@link EntityId} containing key-value pairs (must not be null)
     * @return an {@link And} condition
     * @throws IllegalArgumentException if {@code entityId} is null or contains no keys
     * @deprecated the digit-abbreviation name is inconsistent with this class's spelled-out naming
     *             convention; use {@link #idToCond(EntityId)} instead
     */
    @Deprecated
    public static And id2Cond(final EntityId entityId) {
        return idToCond(entityId);
    }

    /**
     * Converts a collection of {@link EntityId}s to an {@link Or} condition where each {@link EntityId} becomes an {@link And} condition.
     * Useful for querying multiple entities by their composite keys.
     *
     * @param entityIds collection of {@link EntityId}s (must not be {@code null} or empty)
     * @return an {@link Or} condition
     * @throws IllegalArgumentException if {@code entityIds} is {@code null}, empty, or contains an
     *         {@link EntityId} with no keys
     * @deprecated the digit-abbreviation name is inconsistent with this class's spelled-out naming
     *             convention; use {@link #idToCond(Collection)} instead
     */
    @Deprecated
    public static Or id2Cond(final Collection<? extends EntityId> entityIds) {
        return idToCond(entityIds);
    }

    /**
     * Creates a not-equal condition ({@code !=}) for the specified property and value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotEqual condition = Filters.notEqual("status", "deleted");
     * // SQL fragment: status != 'deleted'
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param propValue the value to compare for inequality; may be a literal, {@code null}
     *                  (renders as {@code IS NOT NULL}), or another {@link Condition} such as a {@link SubQuery}
     * @return a {@link NotEqual} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static NotEqual notEqual(final String propName, final Object propValue) {
        return new NotEqual(propName, propValue);
    }

    /**
     * Creates a parameterized not-equal condition for use with prepared statements.
     * The value will be represented by a question mark ({@code ?}) placeholder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotEqual condition = Filters.notEqual("user_type");
     * // SQL fragment: user_type != ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link NotEqual} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static NotEqual notEqual(final String propName) {
        return notEqual(propName, QME);
    }

    /**
     * Creates a not-equal condition ({@code !=}) for the specified property and value.
     * This is a shorthand alias for {@link #notEqual(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotEqual condition = Filters.ne("status", "inactive");
     * // SQL fragment: status != 'inactive'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare for inequality
     * @return a {@link NotEqual} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static NotEqual ne(final String propName, final Object propValue) {
        return notEqual(propName, propValue);
    }

    /**
     * Creates a parameterized not-equal condition for use with prepared statements.
     * This is a shorthand alias for {@link #notEqual(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotEqual condition = Filters.ne("category");
     * // SQL fragment: category != ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link NotEqual} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static NotEqual ne(final String propName) {
        return notEqual(propName);
    }

    /**
     * Creates a greater-than condition ({@code >}) for the specified property and value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThan condition = Filters.greaterThan("age", 18);
     * // SQL fragment: age > 18
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param propValue the value to compare against
     * @return a {@link GreaterThan} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static GreaterThan greaterThan(final String propName, final Object propValue) {
        return new GreaterThan(propName, propValue);
    }

    /**
     * Creates a parameterized greater-than condition for use with prepared statements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThan condition = Filters.greaterThan("salary");
     * // SQL fragment: salary > ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link GreaterThan} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static GreaterThan greaterThan(final String propName) {
        return greaterThan(propName, QME);
    }

    /**
     * Creates a greater-than condition ({@code >}) for the specified property and value.
     * This is a shorthand alias for {@link #greaterThan(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThan condition = Filters.gt("price", 100);
     * // SQL fragment: price > 100
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a {@link GreaterThan} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static GreaterThan gt(final String propName, final Object propValue) {
        return greaterThan(propName, propValue);
    }

    /**
     * Creates a parameterized greater-than condition for use with prepared statements.
     * This is a shorthand alias for {@link #greaterThan(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThan condition = Filters.gt("quantity");
     * // SQL fragment: quantity > ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link GreaterThan} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static GreaterThan gt(final String propName) {
        return greaterThan(propName);
    }

    /**
     * Creates a greater-than-or-equal condition ({@code >=}) for the specified property and value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThanOrEqual condition = Filters.greaterThanOrEqual("score", 60);
     * // SQL fragment: score >= 60
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param propValue the value to compare against
     * @return a {@link GreaterThanOrEqual} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static GreaterThanOrEqual greaterThanOrEqual(final String propName, final Object propValue) {
        return new GreaterThanOrEqual(propName, propValue);
    }

    /**
     * Creates a parameterized greater-than-or-equal condition for use with prepared statements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThanOrEqual condition = Filters.greaterThanOrEqual("min_age");
     * // SQL fragment: min_age >= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link GreaterThanOrEqual} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static GreaterThanOrEqual greaterThanOrEqual(final String propName) {
        return greaterThanOrEqual(propName, QME);
    }

    /**
     * Creates a greater-than-or-equal condition ({@code >=}) for the specified property and value.
     * This is a shorthand alias for {@link #greaterThanOrEqual(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThanOrEqual condition = Filters.ge("level", 5);
     * // SQL fragment: level >= 5
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a {@link GreaterThanOrEqual} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static GreaterThanOrEqual ge(final String propName, final Object propValue) {
        return greaterThanOrEqual(propName, propValue);
    }

    /**
     * Creates a parameterized greater-than-or-equal condition for use with prepared statements.
     * This is a shorthand alias for {@link #greaterThanOrEqual(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThanOrEqual condition = Filters.ge("rating");
     * // SQL fragment: rating >= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link GreaterThanOrEqual} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static GreaterThanOrEqual ge(final String propName) {
        return greaterThanOrEqual(propName);
    }

    /**
     * Creates a less-than condition ({@code <}) for the specified property and value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThan condition = Filters.lessThan("age", 65);
     * // SQL fragment: age < 65
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param propValue the value to compare against
     * @return a {@link LessThan} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static LessThan lessThan(final String propName, final Object propValue) {
        return new LessThan(propName, propValue);
    }

    /**
     * Creates a parameterized less-than condition for use with prepared statements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThan condition = Filters.lessThan("max_price");
     * // SQL fragment: max_price < ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link LessThan} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static LessThan lessThan(final String propName) {
        return lessThan(propName, QME);
    }

    /**
     * Creates a less-than condition ({@code <}) for the specified property and value.
     * This is a shorthand alias for {@link #lessThan(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThan condition = Filters.lt("stock", 10);
     * // SQL fragment: stock < 10
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a {@link LessThan} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static LessThan lt(final String propName, final Object propValue) {
        return lessThan(propName, propValue);
    }

    /**
     * Creates a parameterized less-than condition for use with prepared statements.
     * This is a shorthand alias for {@link #lessThan(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThan condition = Filters.lt("expiry_date");
     * // SQL fragment: expiry_date < ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link LessThan} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static LessThan lt(final String propName) {
        return lessThan(propName);
    }

    /**
     * Creates a less-than-or-equal condition ({@code <=}) for the specified property and value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThanOrEqual condition = Filters.lessThanOrEqual("discount", 50);
     * // SQL fragment: discount <= 50
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param propValue the value to compare against
     * @return a {@link LessThanOrEqual} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static LessThanOrEqual lessThanOrEqual(final String propName, final Object propValue) {
        return new LessThanOrEqual(propName, propValue);
    }

    /**
     * Creates a parameterized less-than-or-equal condition for use with prepared statements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThanOrEqual condition = Filters.lessThanOrEqual("max_attempts");
     * // SQL fragment: max_attempts <= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link LessThanOrEqual} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static LessThanOrEqual lessThanOrEqual(final String propName) {
        return lessThanOrEqual(propName, QME);
    }

    /**
     * Creates a less-than-or-equal condition ({@code <=}) for the specified property and value.
     * This is a shorthand alias for {@link #lessThanOrEqual(String, Object)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThanOrEqual condition = Filters.le("priority", 3);
     * // SQL fragment: priority <= 3
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a {@link LessThanOrEqual} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static LessThanOrEqual le(final String propName, final Object propValue) {
        return lessThanOrEqual(propName, propValue);
    }

    /**
     * Creates a parameterized less-than-or-equal condition for use with prepared statements.
     * This is a shorthand alias for {@link #lessThanOrEqual(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThanOrEqual condition = Filters.le("weight");
     * // SQL fragment: weight <= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link LessThanOrEqual} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static LessThanOrEqual le(final String propName) {
        return lessThanOrEqual(propName);
    }

    /**
     * Creates a {@link Between} condition for the specified property and range values.
     * The condition is inclusive on both ends.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = Filters.between("age", 18, 65);
     * // SQL fragment: age BETWEEN 18 AND 65
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (inclusive)
     * @return a {@link Between} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static Between between(final String propName, final Object minValue, final Object maxValue) {
        return new Between(propName, minValue, maxValue);
    }

    /**
     * Creates a parameterized {@link Between} condition for use with prepared statements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = Filters.between("price");
     * // SQL fragment: price BETWEEN ? AND ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link Between} condition with parameter placeholders
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static Between between(final String propName) {
        return new Between(propName, QME, QME);
    }

    // Removed: bt(String, Object, Object) and bt(String) - non-standard abbreviations.
    // Use between(String, Object, Object) or between(String) instead.

    /**
     * Creates a {@link NotBetween} condition for the specified property and range values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = Filters.notBetween("temperature", -10, 40);
     * // SQL fragment: temperature NOT BETWEEN -10 AND 40
     * // True when temperature < -10 OR temperature > 40
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value of the excluded range (inclusive)
     * @param maxValue the maximum value of the excluded range (inclusive)
     * @return a {@link NotBetween} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static NotBetween notBetween(final String propName, final Object minValue, final Object maxValue) {
        return new NotBetween(propName, minValue, maxValue);
    }

    /**
     * Creates a parameterized {@link NotBetween} condition for use with prepared statements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = Filters.notBetween("score");
     * // SQL fragment: score NOT BETWEEN ? AND ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link NotBetween} condition with parameter placeholders
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static NotBetween notBetween(final String propName) {
        return new NotBetween(propName, QME, QME);
    }

    /**
     * Creates a {@link Like} condition for pattern matching.
     * Use SQL wildcards ({@code %} for any characters, {@code _} for single character) in the pattern.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = Filters.like("email", "%@gmail.com");
     * // SQL fragment: email LIKE '%@gmail.com'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the pattern to match (can include SQL wildcards)
     * @return a {@link Like} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static Like like(final String propName, final String propValue) {
        return new Like(propName, propValue);
    }

    /**
     * Creates a parameterized {@link Like} condition for use with prepared statements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = Filters.like("name");
     * // SQL fragment: name LIKE ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link Like} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static Like like(final String propName) {
        return new Like(propName, QME);
    }

    /**
     * Creates a {@link NotLike} condition for pattern matching exclusion.
     * Use SQL wildcards ({@code %} for any characters, {@code _} for single character) in the pattern.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = Filters.notLike("filename", "%.tmp");
     * // SQL fragment: filename NOT LIKE '%.tmp'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the pattern to exclude (can include SQL wildcards)
     * @return a {@link NotLike} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static NotLike notLike(final String propName, final String propValue) {
        return new NotLike(propName, propValue);
    }

    /**
     * Creates a parameterized {@link NotLike} condition for use with prepared statements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = Filters.notLike("description");
     * // SQL fragment: description NOT LIKE ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a {@link NotLike} condition with a parameter placeholder
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     * @see com.landawn.abacus.query.SqlBuilder
     */
    public static NotLike notLike(final String propName) {
        return new NotLike(propName, QME);
    }

    /**
     * Creates a {@link Like} condition that checks if the property contains the specified value.
     * Automatically wraps the value with {@code %} wildcards.
     * Wildcard characters ({@code %}, {@code _}) already present in {@code propValue} are not
     * escaped and remain active in the generated {@code LIKE} pattern.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = Filters.contains("description", "java");
     * // SQL fragment: description LIKE '%java%'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to search for (must not be {@code null})
     * @return a {@link Like} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code propValue} is {@code null}
     */
    public static Like contains(final String propName, final String propValue) {
        N.checkArgNotNull(propValue, "propValue");
        return new Like(propName, SK._PERCENT + propValue + SK._PERCENT);
    }

    /**
     * Creates a {@link NotLike} condition that checks if the property does not contain the specified value.
     * Automatically wraps the value with {@code %} wildcards.
     * Wildcard characters ({@code %}, {@code _}) already present in {@code propValue} are not
     * escaped and remain active in the generated {@code LIKE} pattern.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = Filters.notContains("tags", "deprecated");
     * // SQL fragment: tags NOT LIKE '%deprecated%'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to exclude (must not be {@code null})
     * @return a {@link NotLike} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code propValue} is {@code null}
     */
    public static NotLike notContains(final String propName, final String propValue) {
        N.checkArgNotNull(propValue, "propValue");
        return new NotLike(propName, SK._PERCENT + propValue + SK._PERCENT);
    }

    /**
     * Creates a {@link Like} condition that checks if the property starts with the specified value.
     * Automatically appends a {@code %} wildcard.
     * Wildcard characters ({@code %}, {@code _}) already present in {@code propValue} are not
     * escaped and remain active in the generated {@code LIKE} pattern.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = Filters.startsWith("name", "John");
     * // SQL fragment: name LIKE 'John%'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the prefix to search for (must not be {@code null})
     * @return a {@link Like} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code propValue} is {@code null}
     */
    public static Like startsWith(final String propName, final String propValue) {
        N.checkArgNotNull(propValue, "propValue");
        return new Like(propName, propValue + SK._PERCENT);
    }

    /**
     * Creates a {@link NotLike} condition that checks if the property does not start with the specified value.
     * Automatically appends a {@code %} wildcard.
     * Wildcard characters ({@code %}, {@code _}) already present in {@code propValue} are not
     * escaped and remain active in the generated {@code LIKE} pattern.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = Filters.notStartsWith("code", "TEST");
     * // SQL fragment: code NOT LIKE 'TEST%'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the prefix to exclude (must not be {@code null})
     * @return a {@link NotLike} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code propValue} is {@code null}
     */
    public static NotLike notStartsWith(final String propName, final String propValue) {
        N.checkArgNotNull(propValue, "propValue");
        return new NotLike(propName, propValue + SK._PERCENT);
    }

    /**
     * Creates a {@link Like} condition that checks if the property ends with the specified value.
     * Automatically prepends a {@code %} wildcard.
     * Wildcard characters ({@code %}, {@code _}) already present in {@code propValue} are not
     * escaped and remain active in the generated {@code LIKE} pattern.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = Filters.endsWith("email", "@company.com");
     * // SQL fragment: email LIKE '%@company.com'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the suffix to search for (must not be {@code null})
     * @return a {@link Like} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code propValue} is {@code null}
     */
    public static Like endsWith(final String propName, final String propValue) {
        N.checkArgNotNull(propValue, "propValue");
        return new Like(propName, SK._PERCENT + propValue);
    }

    /**
     * Creates a {@link NotLike} condition that checks if the property does not end with the specified value.
     * Automatically prepends a {@code %} wildcard.
     * Wildcard characters ({@code %}, {@code _}) already present in {@code propValue} are not
     * escaped and remain active in the generated {@code LIKE} pattern.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = Filters.notEndsWith("filename", ".tmp");
     * // SQL fragment: filename NOT LIKE '%.tmp'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the suffix to exclude (must not be {@code null})
     * @return a {@link NotLike} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code propValue} is {@code null}
     */
    public static NotLike notEndsWith(final String propName, final String propValue) {
        N.checkArgNotNull(propValue, "propValue");
        return new NotLike(propName, SK._PERCENT + propValue);
    }

    /**
     * Creates an {@link IsNull} condition to check if a property value is null.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNull condition = Filters.isNull("deleted_at");
     * // SQL fragment: deleted_at IS NULL
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link IsNull} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static IsNull isNull(final String propName) {
        return new IsNull(propName);
    }

    /**
     * Creates a condition to check if a property is null or empty string.
     * This combines {@code IS NULL} and {@code = ''} checks with {@code OR}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = Filters.isNullOrEmpty("description");
     * // SQL fragment: ((description IS NULL) OR (description = ''))
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link Or} condition combining null and empty checks
     */
    @Beta
    public static Or isNullOrEmpty(final String propName) {
        return isNull(propName).or(equal(propName, ""));
    }

    /**
     * Creates a condition to check if a property is null or zero.
     * This combines {@code IS NULL} and {@code = 0} checks with {@code OR}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = Filters.isNullOrZero("quantity");
     * // SQL fragment: ((quantity IS NULL) OR (quantity = 0))
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link Or} condition combining null and zero checks
     */
    @Beta
    public static Or isNullOrZero(final String propName) {
        return isNull(propName).or(equal(propName, 0));
    }

    /**
     * Creates an {@link IsNotNull} condition to check if a property value is not null.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNotNull condition = Filters.isNotNull("created_at");
     * // SQL fragment: created_at IS NOT NULL
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link IsNotNull} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static IsNotNull isNotNull(final String propName) {
        return new IsNotNull(propName);
    }

    /**
     * Creates a compound condition to check that a property is neither null nor an empty string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.isNotNullAndNotEmpty("email");
     * // SQL fragment: ((email IS NOT NULL) AND (email != ''))
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link And} condition combining not-null and not-empty checks
     */
    @Beta
    public static And isNotNullAndNotEmpty(final String propName) {
        return isNotNull(propName).and(notEqual(propName, ""));
    }

    /**
     * Creates a compound condition to check that a property is neither null nor zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.isNotNullAndNotZero("quantity");
     * // SQL fragment: ((quantity IS NOT NULL) AND (quantity != 0))
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link And} condition combining not-null and non-zero checks
     */
    @Beta
    public static And isNotNullAndNotZero(final String propName) {
        return isNotNull(propName).and(notEqual(propName, 0));
    }

    /**
     * Creates a condition to check if a numeric property value is {@code NaN} (Not a Number).
     * This is specific to floating-point columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNaN condition = Filters.isNaN("calculation_result");
     * // SQL fragment: calculation_result IS NAN
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link IsNaN} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static IsNaN isNaN(final String propName) {
        return new IsNaN(propName);
    }

    /**
     * Creates a condition to check if a numeric property value is not {@code NaN}.
     * This is specific to floating-point columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNotNaN condition = Filters.isNotNaN("temperature");
     * // SQL fragment: temperature IS NOT NAN
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link IsNotNaN} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static IsNotNaN isNotNaN(final String propName) {
        return new IsNotNaN(propName);
    }

    /**
     * Creates a condition to check if a numeric property value is infinite.
     * This is specific to floating-point columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsInfinite condition = Filters.isInfinite("ratio");
     * // SQL fragment: ratio IS INFINITE
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link IsInfinite} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static IsInfinite isInfinite(final String propName) {
        return new IsInfinite(propName);
    }

    /**
     * Creates a condition to check if a numeric property value is not infinite.
     * This is specific to floating-point columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNotInfinite condition = Filters.isNotInfinite("percentage");
     * // SQL fragment: percentage IS NOT INFINITE
     * }</pre>
     *
     * @param propName the property/column name
     * @return an {@link IsNotInfinite} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static IsNotInfinite isNotInfinite(final String propName) {
        return new IsNotInfinite(propName);
    }

    /**
     * Creates an {@link Is} condition (SQL {@code IS} predicate) for the specified property and value.
     * Unlike equality ({@code =}), {@code IS} is used for special SQL keywords like {@code NULL},
     * {@code NAN}, {@code INFINITE}, or {@code UNKNOWN}. Prefer the dedicated factories
     * ({@link #isNull(String)}, {@link #isNaN(String)}, {@link #isInfinite(String)}) for those well-known cases.
     *
     * <p>If {@code propValue} is Java {@code null}, the rendered SQL collapses to {@code propName IS NULL}.
     * Otherwise {@code propValue} is typically an {@link Expression} representing the desired SQL keyword.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Is condition = Filters.is("status", Filters.expr("UNKNOWN"));
     * // SQL fragment: status IS UNKNOWN
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param propValue the right-hand value (typically an {@link Expression}); may be {@code null}
     *                  (renders as {@code IS NULL})
     * @return an {@link Is} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static Is is(final String propName, final Object propValue) {
        return new Is(propName, propValue);
    }

    /**
     * Creates an {@link IsNot} condition (SQL {@code IS NOT} predicate) for the specified property and value.
     * Unlike inequality ({@code !=}), {@code IS NOT} is used for special SQL keywords like {@code NULL},
     * {@code NAN}, {@code INFINITE}, or {@code UNKNOWN}. Prefer the dedicated factories
     * ({@link #isNotNull(String)}, {@link #isNotNaN(String)}, {@link #isNotInfinite(String)}) for those
     * well-known cases.
     *
     * <p>If {@code propValue} is Java {@code null}, the rendered SQL collapses to
     * {@code propName IS NOT NULL}. Otherwise {@code propValue} is typically an {@link Expression}
     * representing the desired SQL keyword.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNot condition = Filters.isNot("status", Filters.expr("UNKNOWN"));
     * // SQL fragment: status IS NOT UNKNOWN
     * }</pre>
     *
     * @param propName the property/column name (must not be {@code null}, empty, or blank)
     * @param propValue the right-hand value (typically an {@link Expression}); may be {@code null}
     *                  (renders as {@code IS NOT NULL})
     * @return an {@link IsNot} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static IsNot isNot(final String propName, final Object propValue) {
        return new IsNot(propName, propValue);
    }

    /**
     * Creates an {@link Or} junction combining multiple conditions.
     * At least one condition must be true for the {@code OR} to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = Filters.or(
     *     Filters.equal("status", "active"),
     *     Filters.greaterThan("priority", 5),
     *     Filters.isNull("deleted_at")
     * );
     * // Results in: ((status = 'active') OR (priority > 5) OR (deleted_at IS NULL))
     * }</pre>
     *
     * @param conditions the array of conditions to combine with {@code OR}; {@code null} or empty
     *                   is permitted and yields an empty junction (which renders as an empty string)
     * @return an {@link Or} junction
     * @throws IllegalArgumentException if any element of {@code conditions} is {@code null}
     */
    public static Or or(final Condition... conditions) {
        return new Or(conditions);
    }

    /**
     * Creates an {@link Or} junction combining multiple conditions from a collection.
     * At least one condition must be true for the {@code OR} to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Condition> conditions = Arrays.asList(
     *     Filters.equal("type", "admin"),
     *     Filters.equal("type", "moderator")
     * );
     * Or condition = Filters.or(conditions);
     * // Results in: ((type = 'admin') OR (type = 'moderator'))
     * }</pre>
     *
     * @param conditions the collection of conditions to combine with {@code OR}; {@code null} or
     *                   empty is permitted and yields an empty junction
     * @return an {@link Or} junction
     * @throws IllegalArgumentException if any element of {@code conditions} is {@code null}
     */
    public static Or or(final Collection<? extends Condition> conditions) {
        return new Or(conditions);
    }

    /**
     * Creates an {@link And} junction combining multiple conditions.
     * All conditions must be true for the {@code AND} to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = Filters.and(
     *     Filters.equal("status", "active"),
     *     Filters.greaterThanOrEqual("age", 18),
     *     Filters.isNotNull("email")
     * );
     * // Results in: ((status = 'active') AND (age >= 18) AND (email IS NOT NULL))
     * }</pre>
     *
     * @param conditions the array of conditions to combine with {@code AND}; {@code null} or
     *                   empty is permitted and yields an empty junction (which renders as an empty string)
     * @return an {@link And} junction
     * @throws IllegalArgumentException if any element of {@code conditions} is {@code null}
     */
    public static And and(final Condition... conditions) {
        return new And(conditions);
    }

    /**
     * Creates an {@link And} junction combining multiple conditions from a collection.
     * All conditions must be true for the {@code AND} to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Condition> conditions = Arrays.asList(
     *     Filters.between("price", 10, 100),
     *     Filters.equal("in_stock", true)
     * );
     * And condition = Filters.and(conditions);
     * // Results in: ((price BETWEEN 10 AND 100) AND (in_stock = true))
     * }</pre>
     *
     * @param conditions the collection of conditions to combine with {@code AND}; {@code null} or
     *                   empty is permitted and yields an empty junction
     * @return an {@link And} junction
     * @throws IllegalArgumentException if any element of {@code conditions} is {@code null}
     */
    public static And and(final Collection<? extends Condition> conditions) {
        return new And(conditions);
    }

    /**
     * Creates a {@link Junction} combining multiple conditions with the given operator, which must be
     * {@link Operator#AND} or {@link Operator#OR}. This is useful when the operator is chosen at runtime;
     * for a fixed operator prefer {@link #and(Condition...)} or {@link #or(Condition...)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction condition = Filters.junction(Operator.OR,
     *     Filters.equal("flag1", true),
     *     Filters.equal("flag2", true)
     * );
     * // Results in: ((flag1 = true) OR (flag2 = true))
     * }</pre>
     *
     * @param operator the junction operator; must be {@link Operator#AND} or {@link Operator#OR}
     * @param conditions the array of conditions to combine
     * @return a {@link Junction} with the specified operator
     * @throws NullPointerException if {@code operator} is {@code null}
     * @throws IllegalArgumentException if {@code operator} is not {@link Operator#AND} or {@link Operator#OR},
     *             or if any element of {@code conditions} is {@code null}
     */
    @Beta
    public static Junction junction(final Operator operator, final Condition... conditions) {
        return new Junction(operator, conditions);
    }

    /**
     * Creates a {@link Junction} combining conditions from a collection with the given operator, which must be
     * {@link Operator#AND} or {@link Operator#OR}. This is useful when the operator is chosen at runtime;
     * for a fixed operator prefer {@link #and(Collection)} or {@link #or(Collection)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Condition> conditionsList = Arrays.asList(Filters.equal("flag1", true), Filters.equal("flag2", true));
     * Junction condition = Filters.junction(Operator.OR, conditionsList);
     * // Results in: ((flag1 = true) OR (flag2 = true))
     * }</pre>
     *
     * @param operator the junction operator; must be {@link Operator#AND} or {@link Operator#OR}
     * @param conditions the collection of conditions to combine
     * @return a {@link Junction} with the specified operator
     * @throws NullPointerException if {@code operator} is {@code null}
     * @throws IllegalArgumentException if {@code operator} is not {@link Operator#AND} or {@link Operator#OR},
     *             or if any element of {@code conditions} is {@code null}
     */
    @Beta
    public static Junction junction(final Operator operator, final Collection<? extends Condition> conditions) {
        return new Junction(operator, conditions);
    }

    /**
     * Creates a {@link Where} clause with the specified condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where where = Filters.where(Filters.equal("active", true));
     * // Results in SQL like: WHERE active = true
     * }</pre>
     *
     * @param cond the condition for the {@code WHERE} clause (must not be {@code null})
     * @return a {@link Where} clause
     * @throws IllegalArgumentException if {@code cond} is {@code null}, or is a Criteria, another clause, an {@code ON}/{@code USING} condition, an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand, or an empty predicate (a blank {@link Expression} or empty {@link Junction}) — none of which can be nested inside a clause
     */
    public static Where where(final Condition cond) {
        return new Where(cond);
    }

    /**
     * Creates a {@link Where} clause from a raw SQL expression string.
     * Useful for custom SQL expressions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where where = Filters.where("YEAR(created_date) = 2023");
     * // Results in SQL like: WHERE YEAR(created_date) = 2023
     * }</pre>
     *
     * @param expr the SQL expression as a string (must not be {@code null} or empty)
     * @return a {@link Where} clause
     * @throws IllegalArgumentException if {@code expr} is {@code null} or empty
     */
    public static Where where(final String expr) {
        N.checkArgNotEmpty(expr, "expr");

        return new Where(expr(expr));
    }

    /**
     * Creates a {@link GroupBy} clause with ascending order for a single property.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupByAsc("department");
     * // Results in SQL like: GROUP BY department ASC
     * }</pre>
     *
     * @param propName the property/column name to group by ascending
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static GroupBy groupByAsc(final String propName) {
        return new GroupBy(propName, SortDirection.ASC);
    }

    /**
     * Creates a {@link GroupBy} clause with ascending order for the specified properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupByAsc("department", "role");
     * // Results in SQL like: GROUP BY department ASC, role ASC
     * }</pre>
     *
     * @param propNames the property/column names to group by ascending
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static GroupBy groupByAsc(final String... propNames) {
        return new GroupBy(Array.asList(propNames), SortDirection.ASC);
    }

    /**
     * Creates a {@link GroupBy} clause with ascending order for properties from a collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("department", "role");
     * GroupBy groupBy = Filters.groupByAsc(columns);
     * // Results in SQL like: GROUP BY department ASC, role ASC
     * }</pre>
     *
     * @param propNames collection of property/column names to group by ascending
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static GroupBy groupByAsc(final Collection<String> propNames) {
        return new GroupBy(propNames, SortDirection.ASC);
    }

    /**
     * Creates a {@link GroupBy} clause with descending order for a single property.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupByDesc("sales");
     * // Results in SQL like: GROUP BY sales DESC
     * }</pre>
     *
     * @param propName the property/column name to group by descending
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static GroupBy groupByDesc(final String propName) {
        return new GroupBy(propName, SortDirection.DESC);
    }

    /**
     * Creates a {@link GroupBy} clause with descending order for the specified properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupByDesc("sales", "region");
     * // Results in SQL like: GROUP BY sales DESC, region DESC
     * }</pre>
     *
     * @param propNames the property/column names to group by descending
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static GroupBy groupByDesc(final String... propNames) {
        return new GroupBy(Array.asList(propNames), SortDirection.DESC);
    }

    /**
     * Creates a {@link GroupBy} clause with descending order for properties from a collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("sales", "region");
     * GroupBy groupBy = Filters.groupByDesc(columns);
     * // Results in SQL like: GROUP BY sales DESC, region DESC
     * }</pre>
     *
     * @param propNames collection of property/column names to group by descending
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static GroupBy groupByDesc(final Collection<String> propNames) {
        return new GroupBy(propNames, SortDirection.DESC);
    }

    /**
     * Creates a {@link GroupBy} clause with the specified property names.
     * Groups results by the given columns. No explicit sort direction is appended
     * to the columns; the database default is used.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupBy("department", "role");
     * // Results in SQL like: GROUP BY department, role
     * }</pre>
     *
     * @param propNames the property/column names to group by
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static GroupBy groupBy(final String... propNames) {
        return new GroupBy(propNames);
    }

    /**
     * Creates a {@link GroupBy} clause with properties from a collection.
     * Groups results by the given columns. No explicit sort direction is appended
     * to the columns; the database default is used. To append a sort direction to
     * every column, use {@link #groupBy(Collection, SortDirection)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("country", "city");
     * GroupBy groupBy = Filters.groupBy(columns);
     * // Results in SQL like: GROUP BY country, city
     * }</pre>
     *
     * @param propNames collection of property/column names to group by
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static GroupBy groupBy(final Collection<String> propNames) {
        N.checkArgNotEmpty(propNames, "propNames");

        return new GroupBy(propNames);
    }

    /**
     * Creates a {@link GroupBy} clause with properties and specified sort direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupBy(Arrays.asList("sales", "region"), SortDirection.DESC);
     * // Results in SQL like: GROUP BY sales DESC, region DESC
     * }</pre>
     *
     * @param propNames collection of property/column names to group by
     * @param direction the sort direction ({@code ASC} or {@code DESC})
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, if any property name is {@code null}, empty, or blank, or if {@code direction} is {@code null}
     */
    public static GroupBy groupBy(final Collection<String> propNames, final SortDirection direction) {
        return new GroupBy(propNames, direction);
    }

    /**
     * Creates a {@link GroupBy} clause with a single property and sort direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupBy("category", SortDirection.DESC);
     * // Results in SQL like: GROUP BY category DESC
     * }</pre>
     *
     * @param propName the property/column name to group by
     * @param direction the sort direction ({@code ASC} or {@code DESC})
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code direction} is {@code null}
     */
    public static GroupBy groupBy(final String propName, final SortDirection direction) {
        return new GroupBy(propName, direction);
    }

    /**
     * Creates a {@link GroupBy} clause with two properties and their respective sort directions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
     * // Results in SQL like: GROUP BY year DESC, month ASC
     * }</pre>
     *
     * @param propName1 first property name
     * @param direction1 first property sort direction
     * @param propName2 second property name
     * @param direction2 second property sort direction
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if any property name is {@code null}, empty, or blank, or if any sort direction is {@code null}
     */
    public static GroupBy groupBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2) {
        return groupBy(N.asMap(propName1, direction1, propName2, direction2));
    }

    /**
     * Creates a {@link GroupBy} clause with three properties and their respective sort directions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupBy("country", SortDirection.ASC, "state", SortDirection.ASC, "city", SortDirection.DESC);
     * // Results in SQL like: GROUP BY country ASC, state ASC, city DESC
     * }</pre>
     *
     * @param propName1 first property name
     * @param direction1 first property sort direction
     * @param propName2 second property name
     * @param direction2 second property sort direction
     * @param propName3 third property name
     * @param direction3 third property sort direction
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if any property name is {@code null}, empty, or blank, or if any sort direction is {@code null}
     */
    public static GroupBy groupBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2,
            final String propName3, final SortDirection direction3) {
        return groupBy(N.asMap(propName1, direction1, propName2, direction2, propName3, direction3));
    }

    /**
     * Creates a {@link GroupBy} clause from a map of property names to sort directions.
     * The map should be a {@link java.util.LinkedHashMap} to preserve insertion order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("department", SortDirection.ASC);
     * orders.put("salary", SortDirection.DESC);
     * GroupBy groupBy = Filters.groupBy(orders);
     * // Results in SQL like: GROUP BY department ASC, salary DESC
     * }</pre>
     *
     * @param groupings map of property names to sort directions (should be a {@link java.util.LinkedHashMap} to preserve order)
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code groupings} is {@code null} or empty, if any property name is {@code null}, empty, or blank, or if any sort direction is {@code null}
     */
    public static GroupBy groupBy(final Map<String, SortDirection> groupings) {
        return new GroupBy(groupings);
    }

    /**
     * Creates a {@link GroupBy} clause with a custom condition.
     * Allows for complex {@code GROUP BY} expressions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = Filters.groupBy(
     *     Filters.expr("YEAR(order_date), MONTH(order_date)")
     * );
     * }</pre>
     *
     * @param cond the grouping condition (must not be {@code null})
     * @return a {@link GroupBy} clause
     * @throws IllegalArgumentException if {@code cond} is {@code null}, or is a Criteria, another clause, an {@code ON}/{@code USING} condition, an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand, or an empty predicate (a blank {@link Expression} or empty {@link Junction}) — none of which can be nested inside a clause
     */
    public static GroupBy groupBy(final Condition cond) {
        return new GroupBy(cond);
    }

    /**
     * Creates a {@link Having} clause with the specified condition.
     * {@code HAVING} is used to filter grouped results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Having having = Filters.having(Filters.greaterThan("COUNT(*)", 5));
     * // Results in SQL like: HAVING COUNT(*) > 5
     * }</pre>
     *
     * @param cond the condition for the {@code HAVING} clause (must not be {@code null})
     * @return a {@link Having} clause
     * @throws IllegalArgumentException if {@code cond} is {@code null}, or is a Criteria, another clause, an {@code ON}/{@code USING} condition, an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand, or an empty predicate (a blank {@link Expression} or empty {@link Junction}) — none of which can be nested inside a clause
     */
    public static Having having(final Condition cond) {
        return new Having(cond);
    }

    /**
     * Creates a {@link Having} clause from a raw SQL expression string.
     * Useful for aggregate function conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Having having = Filters.having("SUM(amount) > 1000");
     * // Results in SQL like: HAVING SUM(amount) > 1000
     * }</pre>
     *
     * @param expr the SQL expression as a string (must not be {@code null} or empty)
     * @return a {@link Having} clause
     * @throws IllegalArgumentException if {@code expr} is {@code null} or empty
     */
    public static Having having(final String expr) {
        N.checkArgNotEmpty(expr, "expr");

        return new Having(expr(expr));
    }

    /**
     * Creates an {@link OrderBy} clause with ascending order for a single property.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderByAsc("created_date");
     * // Results in SQL like: ORDER BY created_date ASC
     * }</pre>
     *
     * @param propName the property/column name to order by ascending
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static OrderBy orderByAsc(final String propName) {
        return new OrderBy(propName, SortDirection.ASC);
    }

    /**
     * Creates an {@link OrderBy} clause with ascending order for the specified properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderByAsc("created_date", "id");
     * // Results in SQL like: ORDER BY created_date ASC, id ASC
     * }</pre>
     *
     * @param propNames the property/column names to order by ascending
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static OrderBy orderByAsc(final String... propNames) {
        return new OrderBy(Array.asList(propNames), SortDirection.ASC);
    }

    /**
     * Creates an {@link OrderBy} clause with ascending order for properties from a collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("priority", "created_date");
     * OrderBy orderBy = Filters.orderByAsc(columns);
     * // Results in SQL like: ORDER BY priority ASC, created_date ASC
     * }</pre>
     *
     * @param propNames collection of property/column names to order by ascending
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static OrderBy orderByAsc(final Collection<String> propNames) {
        return new OrderBy(propNames, SortDirection.ASC);
    }

    /**
     * Creates an {@link OrderBy} clause with descending order for a single property.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderByDesc("score");
     * // Results in SQL like: ORDER BY score DESC
     * }</pre>
     *
     * @param propName the property/column name to order by descending
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank
     */
    public static OrderBy orderByDesc(final String propName) {
        return new OrderBy(propName, SortDirection.DESC);
    }

    /**
     * Creates an {@link OrderBy} clause with descending order for the specified properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderByDesc("score", "timestamp");
     * // Results in SQL like: ORDER BY score DESC, timestamp DESC
     * }</pre>
     *
     * @param propNames the property/column names to order by descending
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static OrderBy orderByDesc(final String... propNames) {
        return new OrderBy(Array.asList(propNames), SortDirection.DESC);
    }

    /**
     * Creates an {@link OrderBy} clause with descending order for properties from a collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("amount", "date");
     * OrderBy orderBy = Filters.orderByDesc(columns);
     * // Results in SQL like: ORDER BY amount DESC, date DESC
     * }</pre>
     *
     * @param propNames collection of property/column names to order by descending
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static OrderBy orderByDesc(final Collection<String> propNames) {
        return new OrderBy(propNames, SortDirection.DESC);
    }

    /**
     * Creates an {@link OrderBy} clause with the specified property names.
     * Orders results by the given columns in the database default order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderBy("last_name", "first_name");
     * // Results in SQL like: ORDER BY last_name, first_name
     * }</pre>
     *
     * @param propNames the property/column names to order by
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static OrderBy orderBy(final String... propNames) {
        return new OrderBy(propNames);
    }

    /**
     * Creates an {@link OrderBy} clause with properties from a collection. No explicit sort direction keyword is emitted; SQL's default
     * ordering direction applies.
     *
     * <p>The iteration order of the collection determines the sort priority. Use an order-preserving collection (such as {@link List} or
     * {@link java.util.LinkedHashSet}) to guarantee predictable ordering.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("name", "age");
     * OrderBy orderBy = Filters.orderBy(columns);
     * // Results in SQL like: ORDER BY name, age
     * }</pre>
     *
     * @param propNames collection of property/column names to order by
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, or if any property name is {@code null}, empty, or blank
     */
    public static OrderBy orderBy(final Collection<String> propNames) {
        return new OrderBy(propNames);
    }

    /**
     * Creates an {@link OrderBy} clause with properties and specified sort direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderBy(Arrays.asList("price", "rating"), SortDirection.DESC);
     * // Results in SQL like: ORDER BY price DESC, rating DESC
     * }</pre>
     *
     * @param propNames collection of property/column names to order by
     * @param direction the sort direction ({@code ASC} or {@code DESC})
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty, if any property name is {@code null}, empty, or blank, or if {@code direction} is {@code null}
     */
    public static OrderBy orderBy(final Collection<String> propNames, final SortDirection direction) {
        return new OrderBy(propNames, direction);
    }

    /**
     * Creates an {@link OrderBy} clause with a single property and sort direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderBy("modified_date", SortDirection.DESC);
     * // Results in SQL like: ORDER BY modified_date DESC
     * }</pre>
     *
     * @param propName the property/column name to order by
     * @param direction the sort direction ({@code ASC} or {@code DESC})
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code direction} is {@code null}
     */
    public static OrderBy orderBy(final String propName, final SortDirection direction) {
        return new OrderBy(propName, direction);
    }

    /**
     * Creates an {@link OrderBy} clause with two properties and their respective sort directions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderBy("status", SortDirection.ASC, "priority", SortDirection.DESC);
     * // Results in SQL like: ORDER BY status ASC, priority DESC
     * }</pre>
     *
     * @param propName1 first property name
     * @param direction1 first property sort direction
     * @param propName2 second property name
     * @param direction2 second property sort direction
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if any property name is {@code null}, empty, or blank, or if any sort direction is {@code null}
     */
    public static OrderBy orderBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2) {
        return orderBy(N.asMap(propName1, direction1, propName2, direction2));
    }

    /**
     * Creates an {@link OrderBy} clause with three properties and their respective sort directions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderBy("year", SortDirection.DESC, "month", SortDirection.DESC, "day", SortDirection.ASC);
     * // Results in SQL like: ORDER BY year DESC, month DESC, day ASC
     * }</pre>
     *
     * @param propName1 first property name
     * @param direction1 first property sort direction
     * @param propName2 second property name
     * @param direction2 second property sort direction
     * @param propName3 third property name
     * @param direction3 third property sort direction
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if any property name is {@code null}, empty, or blank, or if any sort direction is {@code null}
     */
    public static OrderBy orderBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2,
            final String propName3, final SortDirection direction3) {
        return orderBy(N.asMap(propName1, direction1, propName2, direction2, propName3, direction3));
    }

    /**
     * Creates an {@link OrderBy} clause from a map of property names to sort directions.
     * The map should be a {@link java.util.LinkedHashMap} to preserve insertion order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("category", SortDirection.ASC);
     * orders.put("price", SortDirection.DESC);
     * orders.put("name", SortDirection.ASC);
     * OrderBy orderBy = Filters.orderBy(orders);
     * // Results in SQL like: ORDER BY category ASC, price DESC, name ASC
     * }</pre>
     *
     * @param orders map of property names to sort directions (should be a {@link java.util.LinkedHashMap} to preserve order)
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code orders} is {@code null} or empty, if any property name is {@code null}, empty, or blank, or if any sort direction is {@code null}
     */
    public static OrderBy orderBy(final Map<String, SortDirection> orders) {
        return new OrderBy(orders);
    }

    /**
     * Creates an {@link OrderBy} clause with a custom condition.
     * Allows for complex {@code ORDER BY} expressions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = Filters.orderBy(
     *     Filters.expr("CASE WHEN status = 'urgent' THEN 1 ELSE 2 END, created_date DESC")
     * );
     * // Results in SQL like: ORDER BY CASE WHEN status = 'urgent' THEN 1 ELSE 2 END, created_date DESC
     * }</pre>
     *
     * @param cond the ordering condition (must not be {@code null})
     * @return an {@link OrderBy} clause
     * @throws IllegalArgumentException if {@code cond} is {@code null}, or is a Criteria, another clause, an {@code ON}/{@code USING} condition, an {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand, or an empty predicate (a blank {@link Expression} or empty {@link Junction}) — none of which can be nested inside a clause
     */
    public static OrderBy orderBy(final Condition cond) {
        return new OrderBy(cond);
    }

    /**
     * Creates an {@link On} clause for JOIN operations with the specified condition.
     *
     * <p>Note: the example below passes literal column references to {@link #equal(String, Object)},
     * which renders the right-hand side as a string literal ({@code 'orders.user_id'}), not as a
     * column reference. To compare two columns, prefer {@link #on(String, String)} or
     * {@link #expr(String)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * On on = Filters.on(Filters.expr("users.id = orders.user_id"));
     * // Results in SQL like: ON users.id = orders.user_id
     * }</pre>
     *
     * @param cond the join condition (must not be {@code null})
     * @return an {@link On} clause
     * @throws IllegalArgumentException if {@code cond} is {@code null}, or is a Criteria, another clause, an {@code ON}/{@code USING} condition, or an empty predicate (a blank {@link Expression} or empty junction)
     */
    public static On on(final Condition cond) {
        return new On(cond);
    }

    /**
     * Creates an {@link On} clause from a raw SQL expression string for JOIN operations.
     *
     * <p><b>Warning:</b> the expression is appended verbatim to the generated SQL. Do not build
     * it from untrusted input — use {@link #on(Condition)} with parameterized conditions instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * On on = Filters.on("users.department_id = departments.id AND users.active = true");
     * // Results in SQL like: ON users.department_id = departments.id AND users.active = true
     * }</pre>
     *
     * @param expr the join condition as a string (must not be {@code null}, empty, or blank)
     * @return an {@link On} clause
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     */
    public static On on(final String expr) {
        return new On(expr(expr));
    }

    /**
     * Creates an {@link On} clause for simple equality join between two columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * On on = Filters.on("user_id", "id");
     * // Results in SQL like: ON user_id = id
     * }</pre>
     *
     * @param leftPropName the first column name
     * @param rightPropName the second column name to join with
     * @return an {@link On} clause
     * @throws IllegalArgumentException if {@code leftPropName} or {@code rightPropName} is {@code null}, empty, or blank
     */
    public static On on(final String leftPropName, final String rightPropName) {
        return new On(leftPropName, rightPropName);
    }

    /**
     * Creates an {@link On} clause from a map of column pairs for JOIN operations.
     * Each entry represents an equality join condition between two columns; multiple entries
     * are combined with {@code AND}. Use a {@link java.util.LinkedHashMap} to preserve the
     * order of the equality conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> joinPairs = new LinkedHashMap<>();
     * joinPairs.put("orders.user_id", "users.id");
     * joinPairs.put("orders.product_id", "products.id");
     * On on = Filters.on(joinPairs);
     * // Generates: ON ((orders.user_id = users.id) AND (orders.product_id = products.id))
     * }</pre>
     *
     * @param propNamePairs map of column name pairs for joining (should be a
     *                     {@link java.util.LinkedHashMap} to preserve order; must not be {@code null} or empty)
     * @return an {@link On} clause
     * @throws IllegalArgumentException if {@code propNamePairs} is {@code null}, empty, or contains a {@code null}, empty, or blank column name
     */
    public static On on(final Map<String, String> propNamePairs) {
        return new On(propNamePairs);
    }

    /**
     * Creates a USING clause for JOIN operations with the specified columns.
     * USING is an alternative to ON when joining tables on columns with the same name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Using usingClause = Filters.using("user_id", "department_id");
     * // Results in SQL like: USING (user_id, department_id)
     * }</pre>
     *
     * @param columnNames the column names used for joining
     * @return a {@link Using} clause
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, empty, contains a {@code null}, empty, or blank element, or contains a qualified (dotted) column name
     * @deprecated It's recommended to use {@link #on(Map)} or multiple {@link #on(String, String)} clauses instead of
     *             {@code Using} for better portability and clarity. Replace {@code using("col1", "col2")} with explicit
     *             {@code on(N.asMap("table1.col1", "table2.col1", "table1.col2", "table2.col2"))}.
     */
    @Deprecated
    public static Using using(final String... columnNames) {
        return new Using(columnNames);
    }

    /**
     * Creates a USING clause from a collection of column names for JOIN operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> cols = Arrays.asList("user_id", "department_id");
     * Using usingClause = Filters.using(cols);
     * // Results in SQL like: USING (user_id, department_id)
     * }</pre>
     *
     * @param columnNames collection of column names used for joining
     * @return a {@link Using} clause
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, empty, contains a {@code null}, empty, or blank element, or contains a qualified (dotted) column name
     * @deprecated It's recommended to use {@link #on(Map)} or multiple {@link #on(String, String)} clauses
     *             instead of {@code Using} for better portability and clarity. Replace {@code using(columnList)}
     *             with explicit {@code on()} conditions that specify the full column names with table prefixes.
     */
    @Deprecated
    public static Using using(final Collection<String> columnNames) {
        return new Using(columnNames);
    }

    /**
     * Creates a {@link Join} clause for the specified entity/table.
     * This creates an implicit inner join.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = Filters.join("orders");
     * // Results in SQL like: JOIN orders
     * }</pre>
     *
     * @param joinEntity the entity/table name to join
     * @return a {@link Join} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static Join join(final String joinEntity) {
        return new Join(joinEntity);
    }

    /**
     * Creates a {@link Join} clause with the specified entity and join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = Filters.join("orders",
     *     Filters.on("users.id", "orders.user_id"));
     * // Results in SQL like: JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param joinEntity the entity/table name to join
     * @param cond the join condition
     * @return a {@link Join} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static Join join(final String joinEntity, final Condition cond) {
        return new Join(joinEntity, cond);
    }

    /**
     * Creates a {@link Join} clause with multiple entities and a join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = Filters.join(Arrays.asList("orders", "products"),
     *     Filters.on("orders.product_id", "products.id"));
     * // Results in SQL like: JOIN (orders, products) ON orders.product_id = products.id
     * }</pre>
     *
     * @param joinEntities collection of entity/table names to join
     * @param cond the join condition
     * @return a {@link Join} clause
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null}, empty, or contains a {@code null}/empty/blank element
     */
    public static Join join(final Collection<String> joinEntities, final Condition cond) {
        return new Join(joinEntities, cond);
    }

    /**
     * Creates a LEFT JOIN clause for the specified entity/table.
     * Returns all records from the left table and matched records from the right table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LeftJoin join = Filters.leftJoin("orders");
     * // Results in SQL like: LEFT JOIN orders
     * }</pre>
     *
     * @param joinEntity the entity/table name to left join
     * @return a {@link LeftJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static LeftJoin leftJoin(final String joinEntity) {
        return new LeftJoin(joinEntity);
    }

    /**
     * Creates a LEFT JOIN clause with the specified entity and join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LeftJoin join = Filters.leftJoin("orders",
     *     Filters.on("users.id", "orders.user_id"));
     * // Results in SQL like: LEFT JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param joinEntity the entity/table name to left join
     * @param cond the join condition
     * @return a {@link LeftJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static LeftJoin leftJoin(final String joinEntity, final Condition cond) {
        return new LeftJoin(joinEntity, cond);
    }

    /**
     * Creates a LEFT JOIN clause with multiple entities and a join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LeftJoin join = Filters.leftJoin(Arrays.asList("orders", "order_items"),
     *     Filters.on("orders.id", "order_items.order_id"));
     * // Results in SQL like: LEFT JOIN (orders, order_items) ON orders.id = order_items.order_id
     * }</pre>
     *
     * @param joinEntities collection of entity/table names to left join
     * @param cond the join condition
     * @return a {@link LeftJoin} clause
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null}, empty, or contains a {@code null}/empty/blank element
     */
    public static LeftJoin leftJoin(final Collection<String> joinEntities, final Condition cond) {
        return new LeftJoin(joinEntities, cond);
    }

    /**
     * Creates a RIGHT JOIN clause for the specified entity/table.
     * Returns all records from the right table and matched records from the left table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RightJoin join = Filters.rightJoin("users");
     * // Results in SQL like: RIGHT JOIN users
     * }</pre>
     *
     * @param joinEntity the entity/table name to right join
     * @return a {@link RightJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static RightJoin rightJoin(final String joinEntity) {
        return new RightJoin(joinEntity);
    }

    /**
     * Creates a RIGHT JOIN clause with the specified entity and join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RightJoin join = Filters.rightJoin("users",
     *     Filters.on("orders.user_id", "users.id"));
     * // Results in SQL like: RIGHT JOIN users ON orders.user_id = users.id
     * }</pre>
     *
     * @param joinEntity the entity/table name to right join
     * @param cond the join condition
     * @return a {@link RightJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static RightJoin rightJoin(final String joinEntity, final Condition cond) {
        return new RightJoin(joinEntity, cond);
    }

    /**
     * Creates a RIGHT JOIN clause with multiple entities and a join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RightJoin join = Filters.rightJoin(Arrays.asList("departments", "locations"),
     *     Filters.on("departments.location_id", "locations.id"));
     * // Results in SQL like: RIGHT JOIN (departments, locations) ON departments.location_id = locations.id
     * }</pre>
     *
     * @param joinEntities collection of entity/table names to right join
     * @param cond the join condition
     * @return a {@link RightJoin} clause
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null}, empty, or contains a {@code null}/empty/blank element
     */
    public static RightJoin rightJoin(final Collection<String> joinEntities, final Condition cond) {
        return new RightJoin(joinEntities, cond);
    }

    /**
     * Creates a CROSS JOIN clause for the specified entity/table.
     * Returns the Cartesian product of both tables.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CrossJoin join = Filters.crossJoin("colors");
     * // Results in SQL like: CROSS JOIN colors
     * }</pre>
     *
     * @param joinEntity the entity/table name to cross join
     * @return a {@link CrossJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static CrossJoin crossJoin(final String joinEntity) {
        return new CrossJoin(joinEntity);
    }

    /**
     * Creates a CROSS JOIN clause with the specified entity and optional condition.
     * Note: Traditional CROSS JOIN doesn't use conditions, but some databases support it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CrossJoin join = Filters.crossJoin("sizes", Filters.equal("active", true));
     * // Results in SQL like: CROSS JOIN sizes ON active = true
     * // (a non-ON condition is prefixed with the ON keyword when rendered)
     * }</pre>
     *
     * @param joinEntity the entity/table name to cross join
     * @param cond the optional join condition
     * @return a {@link CrossJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static CrossJoin crossJoin(final String joinEntity, final Condition cond) {
        return new CrossJoin(joinEntity, cond);
    }

    /**
     * Creates a CROSS JOIN clause with multiple entities and optional condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CrossJoin join = Filters.crossJoin(Arrays.asList("colors", "sizes"), null);
     * // Results in SQL like: CROSS JOIN (colors, sizes)
     * }</pre>
     *
     * @param joinEntities collection of entity/table names to cross join
     * @param cond the optional join condition
     * @return a {@link CrossJoin} clause
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null}, empty, or contains a {@code null}/empty/blank element
     */
    public static CrossJoin crossJoin(final Collection<String> joinEntities, final Condition cond) {
        return new CrossJoin(joinEntities, cond);
    }

    /**
     * Creates a FULL JOIN clause for the specified entity/table.
     * Returns all records when there is a match in either table.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FullJoin join = Filters.fullJoin("departments");
     * // Results in SQL like: FULL JOIN departments
     * }</pre>
     *
     * @param joinEntity the entity/table name to full join
     * @return a {@link FullJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static FullJoin fullJoin(final String joinEntity) {
        return new FullJoin(joinEntity);
    }

    /**
     * Creates a FULL JOIN clause with the specified entity and join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FullJoin join = Filters.fullJoin("employees",
     *     Filters.on("departments.id", "employees.dept_id"));
     * // Results in SQL like: FULL JOIN employees ON departments.id = employees.dept_id
     * }</pre>
     *
     * @param joinEntity the entity/table name to full join
     * @param cond the join condition
     * @return a {@link FullJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static FullJoin fullJoin(final String joinEntity, final Condition cond) {
        return new FullJoin(joinEntity, cond);
    }

    /**
     * Creates a FULL JOIN clause with multiple entities and a join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FullJoin join = Filters.fullJoin(Arrays.asList("employees", "contractors"),
     *     Filters.on("employees.project_id", "contractors.project_id"));
     * // Results in SQL like: FULL JOIN (employees, contractors) ON employees.project_id = contractors.project_id
     * }</pre>
     *
     * @param joinEntities collection of entity/table names to full join
     * @param cond the join condition
     * @return a {@link FullJoin} clause
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null}, empty, or contains a {@code null}/empty/blank element
     */
    public static FullJoin fullJoin(final Collection<String> joinEntities, final Condition cond) {
        return new FullJoin(joinEntities, cond);
    }

    /**
     * Creates an INNER JOIN clause for the specified entity/table.
     * Returns records that have matching values in both tables.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InnerJoin join = Filters.innerJoin("orders");
     * // Results in SQL like: INNER JOIN orders
     * }</pre>
     *
     * @param joinEntity the entity/table name to inner join
     * @return an {@link InnerJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static InnerJoin innerJoin(final String joinEntity) {
        return new InnerJoin(joinEntity);
    }

    /**
     * Creates an INNER JOIN clause with the specified entity and join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InnerJoin join = Filters.innerJoin("products",
     *     Filters.on("order_items.product_id", "products.id"));
     * // Results in SQL like: INNER JOIN products ON order_items.product_id = products.id
     * }</pre>
     *
     * @param joinEntity the entity/table name to inner join
     * @param cond the join condition
     * @return an {@link InnerJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static InnerJoin innerJoin(final String joinEntity, final Condition cond) {
        return new InnerJoin(joinEntity, cond);
    }

    /**
     * Creates an INNER JOIN clause with multiple entities and a join condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InnerJoin join = Filters.innerJoin(Arrays.asList("orders", "order_details"),
     *     Filters.on("orders.id", "order_details.order_id"));
     * // Results in SQL like: INNER JOIN (orders, order_details) ON orders.id = order_details.order_id
     * }</pre>
     *
     * @param joinEntities collection of entity/table names to inner join
     * @param cond the join condition
     * @return an {@link InnerJoin} clause
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null}, empty, or contains a {@code null}/empty/blank element
     */
    public static InnerJoin innerJoin(final Collection<String> joinEntities, final Condition cond) {
        return new InnerJoin(joinEntities, cond);
    }

    /**
     * Creates a NATURAL JOIN clause for the specified entity/table.
     * Automatically joins tables based on columns with the same name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NaturalJoin join = Filters.naturalJoin("departments");
     * // Results in SQL like: NATURAL JOIN departments
     * }</pre>
     *
     * @param joinEntity the entity/table name to natural join
     * @return a {@link NaturalJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public static NaturalJoin naturalJoin(final String joinEntity) {
        return new NaturalJoin(joinEntity);
    }

    /**
     * Creates a NATURAL JOIN clause with the specified entity.
     * A NATURAL JOIN derives its join predicate implicitly from columns with matching names and
     * therefore does not accept an explicit condition: {@code cond} must be {@code null}. This
     * overload exists only for API symmetry with the other join factories. To apply an additional
     * filter, place it in the enclosing query's {@code WHERE} clause instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NaturalJoin join = Filters.naturalJoin("departments", null);
     * // Results in SQL like: NATURAL JOIN departments
     * }</pre>
     *
     * @param joinEntity the entity/table name to natural join
     * @param cond must be {@code null}; a NATURAL JOIN cannot carry an explicit condition
     * @return a {@link NaturalJoin} clause
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank, or if {@code cond} is non-{@code null}
     * @deprecated a NATURAL JOIN cannot carry an explicit condition, so this overload accepts only {@code null} and
     *             throws for any other value. Use {@link #naturalJoin(String)} instead, and place any additional
     *             filter in the enclosing query's {@code WHERE} clause.
     */
    @Deprecated
    public static NaturalJoin naturalJoin(final String joinEntity, final Condition cond) {
        return new NaturalJoin(joinEntity, cond);
    }

    /**
     * Creates a NATURAL JOIN clause with multiple entities.
     * As with the single-entity overload, a NATURAL JOIN cannot carry an explicit condition, so
     * {@code cond} must be {@code null}; this form exists only for API symmetry with the other
     * join factories.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NaturalJoin join = Filters.naturalJoin(Arrays.asList("tables", "categories"), null);
     * // Results in SQL like: NATURAL JOIN (tables, categories)
     * }</pre>
     *
     * @param joinEntities collection of entity/table names to natural join
     * @param cond must be {@code null}; a NATURAL JOIN cannot carry an explicit condition
     * @return a {@link NaturalJoin} clause
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null}, empty, or contains a {@code null}/empty/blank element, or if {@code cond} is non-{@code null}
     * @deprecated a NATURAL JOIN cannot carry an explicit condition, so this overload accepts only {@code null} and
     *             throws for any other value. Use the condition-less {@link NaturalJoin#NaturalJoin(Collection)}
     *             constructor instead, and place any additional filter in the enclosing query's {@code WHERE} clause.
     */
    @Deprecated
    public static NaturalJoin naturalJoin(final Collection<String> joinEntities, final Condition cond) {
        return new NaturalJoin(joinEntities, cond);
    }

    /**
     * Creates an IN condition with an array of boolean values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in("active", new boolean[] {true, false});
     * // SQL fragment: active IN (true, false)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of boolean values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final boolean[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of char values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in("grade", new char[] {'A', 'B', 'C'});
     * // SQL fragment: grade IN ('A', 'B', 'C')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of char values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final char[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of byte values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in("flag", new byte[] {0, 1, 2});
     * // SQL fragment: flag IN (0, 1, 2)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of byte values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final byte[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of short values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in("level", new short[] {1, 2, 3});
     * // SQL fragment: level IN (1, 2, 3)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of short values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final short[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of integer values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in("user_id", new int[] {1, 2, 3, 4});
     * // SQL fragment: user_id IN (1, 2, 3, 4)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of integer values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final int[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of long values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in("order_id", new long[] {1001L, 1002L, 1003L});
     * // SQL fragment: order_id IN (1001, 1002, 1003)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of long values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final long[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of float values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in("ratio", new float[] {0.25f, 0.5f, 0.75f});
     * // SQL fragment: ratio IN (0.25, 0.5, 0.75)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of float values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final float[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of double values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in("price", new double[] {9.99, 19.99, 29.99});
     * // SQL fragment: price IN (9.99, 19.99, 29.99)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of double values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final double[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of object values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in("status", new String[] {"active", "pending", "approved"});
     * // SQL fragment: status IN ('active', 'pending', 'approved')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final Object[] values) {
        return in(propName, values == null ? (Collection<?>) null : Arrays.asList(values));
    }

    /**
     * Creates an IN condition with a collection of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> categories = Arrays.asList("electronics", "books", "toys");
     * In condition = Filters.in("category", categories);
     * // SQL fragment: category IN ('electronics', 'books', 'toys')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values collection of values
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static In in(final String propName, final Collection<?> values) {
        return new In(propName, values);
    }

    /**
     * Creates a row value constructor IN condition.
     * The tuple of property values must match one of the supplied value rows.
     *
     * <p>Each element of {@code values} is one row and may be supplied as a {@link Collection} or other
     * {@link Iterable}, an object array, a {@link Map} (looked up by property name) or a bean (read by
     * property name).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = Filters.in(Arrays.asList("first_name", "last_name"),
     *         Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));
     * // SQL fragment: (first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))
     * }</pre>
     *
     * <p><b>Portability note:</b> the row value-list form is supported by MySQL, PostgreSQL,
     * Oracle and DB2, but <i>not</i> by SQL Server (use {@link #in(Collection, SubQuery)} there).</p>
     *
     * @param propNames the property/column names (must not be {@code null} or empty and must not contain {@code null}/blank names)
     * @param values collection of value rows; each row must resolve to exactly {@code propNames.size()} values.
     *               A row may be a {@link Collection}, {@link Iterable}, object array, {@link Map} or bean
     * @return an {@link In} condition
     * @throws IllegalArgumentException if {@code propNames} is {@code null}/empty or contains any {@code null}/blank name,
     *                                  if {@code values} is {@code null} or empty, if any row is {@code null} or of an
     *                                  unsupported type, or if a positional row's width does not match {@code propNames.size()}
     */
    public static In in(final Collection<String> propNames, final Collection<?> values) {
        return new In(propNames, values);
    }

    /**
     * Creates an IN condition with a subquery.
     * The property value must be in the result set of the subquery.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT id FROM active_users");
     * InSubQuery condition = Filters.in("user_id", subQuery);
     * // SQL fragment: user_id IN (SELECT id FROM active_users)
     * }</pre>
     *
     * @param propName the property/column name
     * @param subQuery the subquery to check against
     * @return an {@link InSubQuery} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code subQuery} is {@code null}
     */
    public static InSubQuery in(final String propName, final SubQuery subQuery) {
        return new InSubQuery(propName, subQuery);
    }

    /**
     * Creates an IN condition with multiple properties and a subquery.
     * Used for composite key comparisons.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT user_id, order_id FROM recent_orders");
     * InSubQuery condition = Filters.in(Arrays.asList("user_id", "order_id"), subQuery);
     * // SQL fragment: (user_id, order_id) IN (SELECT user_id, order_id FROM recent_orders)
     * }</pre>
     *
     * @param propNames collection of property/column names
     * @param subQuery the subquery to check against
     * @return an {@link InSubQuery} condition
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty or contains a {@code null}, empty, or blank name, or if {@code subQuery} is {@code null}
     */
    public static InSubQuery in(final Collection<String> propNames, final SubQuery subQuery) {
        return new InSubQuery(propNames, subQuery);
    }

    /**
     * Creates a NOT IN condition with an array of boolean values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn("active", new boolean[] {false});
     * // SQL fragment: active NOT IN (false)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of boolean values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final boolean[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of char values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn("grade", new char[] {'D', 'F'});
     * // SQL fragment: grade NOT IN ('D', 'F')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of char values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final char[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of byte values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn("flag", new byte[] {0, 1});
     * // SQL fragment: flag NOT IN (0, 1)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of byte values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final byte[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of short values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn("level", new short[] {0, 9});
     * // SQL fragment: level NOT IN (0, 9)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of short values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final short[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of integer values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn("status_code", new int[] {404, 500, 503});
     * // SQL fragment: status_code NOT IN (404, 500, 503)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of integer values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final int[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of long values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn("excluded_ids", new long[] {110L, 120L, 130L});
     * // SQL fragment: excluded_ids NOT IN (110, 120, 130)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of long values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final long[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of float values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn("ratio", new float[] {0.0f, 1.0f});
     * // SQL fragment: ratio NOT IN (0.0, 1.0)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of float values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final float[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of double values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn("discount", new double[] {0.0, 100.0});
     * // SQL fragment: discount NOT IN (0.0, 100.0)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of double values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final double[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of object values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn("role", new String[] {"guest", "banned"});
     * // SQL fragment: role NOT IN ('guest', 'banned')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final Object[] values) {
        return notIn(propName, values == null ? (Collection<?>) null : Arrays.asList(values));
    }

    /**
     * Creates a NOT IN condition with a collection of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excludedCountries = new HashSet<>(Arrays.asList("XX", "YY"));
     * NotIn condition = Filters.notIn("country_code", excludedCountries);
     * // SQL fragment: country_code NOT IN ('XX', 'YY')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values collection of values to exclude
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code values} is {@code null} or empty
     */
    public static NotIn notIn(final String propName, final Collection<?> values) {
        return new NotIn(propName, values);
    }

    /**
     * Creates a row value constructor NOT IN condition.
     * The tuple of property values must not match any of the supplied value rows.
     *
     * <p>Each element of {@code values} is one row and may be supplied as a {@link Collection} or other
     * {@link Iterable}, an object array, a {@link Map} (looked up by property name) or a bean (read by
     * property name).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = Filters.notIn(Arrays.asList("first_name", "last_name"),
     *         Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));
     * // SQL fragment: (first_name, last_name) NOT IN (('John', 'Doe'), ('Jane', 'Roe'))
     * }</pre>
     *
     * <p><b>Portability note:</b> the row value-list form is supported by MySQL, PostgreSQL,
     * Oracle and DB2, but <i>not</i> by SQL Server (use {@link #notIn(Collection, SubQuery)} there).</p>
     *
     * @param propNames the property/column names (must not be {@code null} or empty and must not contain {@code null}/blank names)
     * @param values collection of value rows to exclude; each row must resolve to exactly {@code propNames.size()}
     *               values. A row may be a {@link Collection}, {@link Iterable}, object array, {@link Map} or bean
     * @return a {@link NotIn} condition
     * @throws IllegalArgumentException if {@code propNames} is {@code null}/empty or contains any {@code null}/blank name,
     *                                  if {@code values} is {@code null} or empty, if any row is {@code null} or of an
     *                                  unsupported type, or if a positional row's width does not match {@code propNames.size()}
     */
    public static NotIn notIn(final Collection<String> propNames, final Collection<?> values) {
        return new NotIn(propNames, values);
    }

    /**
     * Creates a NOT IN condition with a subquery.
     * The property value must not be in the result set of the subquery.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT id FROM blacklisted_users");
     * NotInSubQuery condition = Filters.notIn("user_id", subQuery);
     * // SQL fragment: user_id NOT IN (SELECT id FROM blacklisted_users)
     * }</pre>
     *
     * @param propName the property/column name
     * @param subQuery the subquery to check against
     * @return a {@link NotInSubQuery} condition
     * @throws IllegalArgumentException if {@code propName} is {@code null}, empty, or blank, or if {@code subQuery} is {@code null}
     */
    public static NotInSubQuery notIn(final String propName, final SubQuery subQuery) {
        return new NotInSubQuery(propName, subQuery);
    }

    /**
     * Creates a NOT IN condition with multiple properties and a subquery.
     * Used for composite key exclusions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT user_id, product_id FROM returns");
     * NotInSubQuery condition = Filters.notIn(Arrays.asList("user_id", "product_id"), subQuery);
     * // SQL fragment: (user_id, product_id) NOT IN (SELECT user_id, product_id FROM returns)
     * }</pre>
     *
     * @param propNames collection of property/column names
     * @param subQuery the subquery to check against
     * @return a {@link NotInSubQuery} condition
     * @throws IllegalArgumentException if {@code propNames} is {@code null} or empty or contains a {@code null}, empty, or blank name, or if {@code subQuery} is {@code null}
     */
    public static NotInSubQuery notIn(final Collection<String> propNames, final SubQuery subQuery) {
        return new NotInSubQuery(propNames, subQuery);
    }

    /**
     * Creates an ALL condition for comparison with all values from a subquery.
     * The condition is true if the comparison is true for all values returned by the subquery.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE dept = 'IT'");
     * All condition = Filters.all(subQuery);
     * // This factory only wraps the subquery as: ALL (SELECT salary FROM employees WHERE dept = 'IT').
     * // When used as the RHS of a comparison such as gt, the full fragment renders:
     * // salary > ALL (SELECT salary FROM employees WHERE dept = 'IT')
     * }</pre>
     *
     * @param subQuery the subquery
     * @return an {@link All} condition
     */
    public static All all(final SubQuery subQuery) {
        return new All(subQuery);
    }

    /**
     * Creates an ANY condition for comparison with any value from a subquery.
     * The condition is true if the comparison is true for at least one value returned by the subquery.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'electronics'");
     * Any condition = Filters.any(subQuery);
     * // This factory only wraps the subquery as: ANY (SELECT price FROM products WHERE category = 'electronics').
     * // When used as the RHS of a comparison such as lt, the full fragment renders:
     * // price < ANY (SELECT price FROM products WHERE category = 'electronics')
     * }</pre>
     *
     * @param subQuery the subquery
     * @return an {@link Any} condition
     */
    public static Any any(final SubQuery subQuery) {
        return new Any(subQuery);
    }

    /**
     * Creates a SOME condition for comparison with some values from a subquery.
     * SOME is functionally equivalent to ANY.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT score FROM exams WHERE student_id = 123");
     * Some condition = Filters.some(subQuery);
     * // This factory only wraps the subquery as: SOME (SELECT score FROM exams WHERE student_id = 123).
     * // When used as the RHS of a comparison such as le, the full fragment renders:
     * // passing_score <= SOME (SELECT score FROM exams WHERE student_id = 123)
     * }</pre>
     *
     * @param subQuery the subquery
     * @return a {@link Some} condition
     */
    public static Some some(final SubQuery subQuery) {
        return new Some(subQuery);
    }

    /**
     * Creates an EXISTS condition to check if a subquery returns any rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.user_id = users.id");
     * Exists condition = Filters.exists(subQuery);
     * // SQL fragment: EXISTS (SELECT 1 FROM orders WHERE orders.user_id = users.id)
     * }</pre>
     *
     * @param subQuery the subquery to check
     * @return an {@link Exists} condition
     */
    public static Exists exists(final SubQuery subQuery) {
        return new Exists(subQuery);
    }

    /**
     * Creates a NOT EXISTS condition to check if a subquery returns no rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT 1 FROM archived_users WHERE archived_users.id = users.id");
     * NotExists condition = Filters.notExists(subQuery);
     * // SQL fragment: NOT EXISTS (SELECT 1 FROM archived_users WHERE archived_users.id = users.id)
     * }</pre>
     *
     * @param subQuery the subquery to check
     * @return a {@link NotExists} condition
     */
    public static NotExists notExists(final SubQuery subQuery) {
        return new NotExists(subQuery);
    }

    /**
     * Creates a UNION clause to combine results from a subquery.
     * UNION removes duplicate rows from the combined result set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT id FROM archived_users");
     * Union union = Filters.union(subQuery);
     * // Results in SQL like: UNION SELECT id FROM archived_users
     * }</pre>
     *
     * @param subQuery the subquery to union with
     * @return a {@link Union} clause
     */
    public static Union union(final SubQuery subQuery) {
        return new Union(subQuery);
    }

    /**
     * Creates a UNION ALL clause to combine results from a subquery.
     * UNION ALL keeps all rows including duplicates.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT name FROM inactive_products");
     * UnionAll unionAll = Filters.unionAll(subQuery);
     * // Results in SQL like: UNION ALL SELECT name FROM inactive_products
     * }</pre>
     *
     * @param subQuery the subquery to union with
     * @return a {@link UnionAll} clause
     */
    public static UnionAll unionAll(final SubQuery subQuery) {
        return new UnionAll(subQuery);
    }

    /**
     * Creates an EXCEPT clause to subtract results from a subquery.
     * Returns rows from the first query that are not in the second query.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT id FROM blacklisted_customers");
     * Except except = Filters.except(subQuery);
     * // Results in SQL like: EXCEPT SELECT id FROM blacklisted_customers
     * }</pre>
     *
     * @param subQuery the subquery to subtract
     * @return an {@link Except} clause
     */
    public static Except except(final SubQuery subQuery) {
        return new Except(subQuery);
    }

    /**
     * Creates an INTERSECT clause to find common results with a subquery.
     * Returns only rows that appear in both queries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT product_id FROM discounted_items");
     * Intersect intersect = Filters.intersect(subQuery);
     * // Results in SQL like: INTERSECT SELECT product_id FROM discounted_items
     * }</pre>
     *
     * @param subQuery the subquery to intersect with
     * @return an {@link Intersect} clause
     */
    public static Intersect intersect(final SubQuery subQuery) {
        return new Intersect(subQuery);
    }

    /**
     * Creates a MINUS clause to subtract results from a subquery.
     * MINUS is similar to EXCEPT but is used in some databases like Oracle.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("SELECT id FROM deleted_records");
     * Minus minus = Filters.minus(subQuery);
     * // Results in SQL like: MINUS SELECT id FROM deleted_records
     * }</pre>
     *
     * @param subQuery the subquery to subtract
     * @return a {@link Minus} clause
     */
    public static Minus minus(final SubQuery subQuery) {
        return new Minus(subQuery);
    }

    /**
     * Creates a SubQuery from an entity class with selected properties and condition.
     * If {@code cond} is not already a {@link com.landawn.abacus.query.condition.Criteria Criteria}
     * or a clause (such as {@link Where}), it will be automatically wrapped in a {@code WHERE} clause
     * when the subquery is rendered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery(User.class,
     *     Arrays.asList("id", "name"),
     *     Filters.equal("active", true));
     * // Generates: SELECT id, name FROM User WHERE active = true
     * }</pre>
     *
     * @param entityClass the entity class representing the table (must not be {@code null})
     * @param propNames collection of property names to select (must not be {@code null} or empty, and must not contain
     *                  {@code null}, empty, or blank elements)
     * @param cond the WHERE condition for the subquery; may be {@code null} for no WHERE clause
     * @return a {@link SubQuery}
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, if {@code propNames} is
     *         {@code null} or empty, contains a {@code null}, empty, or blank element, or if {@code cond}
     *         uses an {@code ON}/{@code USING} operator
     */
    public static SubQuery subQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition cond) {
        return new SubQuery(entityClass, propNames, cond);
    }

    /**
     * Creates a SubQuery from an entity class with selected properties and a raw SQL condition string.
     *
     * <p><b>Warning:</b> {@code expr} is appended verbatim to the generated SQL. Do not build it
     * from untrusted input — use {@link #subQuery(Class, Collection, Condition)} with parameterized
     * conditions instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery(Product.class,
     *     Arrays.asList("id", "price"),
     *     "category = 'electronics' AND in_stock = true");
     * // Generates: SELECT id, price FROM Product WHERE category = 'electronics' AND in_stock = true
     * }</pre>
     *
     * @param entityClass the entity class representing the table (must not be {@code null})
     * @param propNames collection of property names to select (must not be {@code null} or empty, and must not contain
     *                  {@code null}, empty, or blank elements)
     * @param expr the WHERE condition as a raw SQL string (must not be {@code null}; may be empty for no filter condition)
     * @return a {@link SubQuery}
     * @throws IllegalArgumentException if {@code entityClass} is {@code null},
     *         if {@code propNames} is {@code null} or empty, contains a {@code null}, empty, or blank element,
     *         or if {@code expr} is {@code null}
     * @see #subQuery(String, Collection, String)
     */
    public static SubQuery subQuery(final Class<?> entityClass, final Collection<String> propNames, final String expr) {
        return new SubQuery(entityClass, propNames, expr(expr));
    }

    /**
     * Creates a SubQuery from an entity name with selected properties and condition.
     * If {@code cond} is not already a {@link com.landawn.abacus.query.condition.Criteria Criteria}
     * or a clause (such as {@link Where}), it will be automatically wrapped in a {@code WHERE} clause
     * when the subquery is rendered.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("users",
     *     Arrays.asList("id", "email"),
     *     Filters.like("email", "%@company.com"));
     * // Generates: SELECT id, email FROM users WHERE email LIKE '%@company.com'
     * }</pre>
     *
     * @param entityName the entity/table name (must not be {@code null}, empty, or blank)
     * @param propNames collection of property names to select (must not be {@code null} or empty, and must not contain
     *                  {@code null}, empty, or blank elements)
     * @param cond the WHERE condition for the subquery; may be {@code null} for no WHERE clause
     * @return a {@link SubQuery}
     * @throws IllegalArgumentException if {@code entityName} is {@code null}, empty, or blank, if
     *         {@code propNames} is {@code null} or empty, contains a {@code null}, empty, or blank element,
     *         or if {@code cond} uses an {@code ON}/{@code USING} operator
     */
    public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final Condition cond) {
        return new SubQuery(entityName, propNames, cond);
    }

    /**
     * Creates a SubQuery from an entity name with selected properties and a raw SQL condition string.
     *
     * <p><b>Warning:</b> {@code expr} is appended verbatim to the generated SQL. Do not build it
     * from untrusted input — use {@link #subQuery(String, Collection, Condition)} with parameterized
     * conditions instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("products",
     *     Arrays.asList("id", "price"),
     *     "category = 'electronics' AND in_stock = true");
     * // Generates: SELECT id, price FROM products WHERE category = 'electronics' AND in_stock = true
     * }</pre>
     *
     * @param entityName the entity/table name (must not be {@code null}, empty, or blank)
     * @param propNames collection of property names to select (must not be {@code null} or empty, and must not contain
     *                  {@code null}, empty, or blank elements)
     * @param expr the WHERE condition as a raw SQL string (must not be {@code null}; may be empty for no filter condition)
     * @return a {@link SubQuery}
     * @throws IllegalArgumentException if {@code entityName} is {@code null}, empty, or blank,
     *         if {@code propNames} is {@code null} or empty, contains a {@code null}, empty, or blank element,
     *         or if {@code expr} is {@code null}
     */
    public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final String expr) {
        return new SubQuery(entityName, propNames, expr(expr));
    }

    /**
     * Creates a SubQuery from an entity name and raw SQL.
     * This method allows for complete control over the subquery SQL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("orders",
     *     "SELECT COUNT(*) FROM orders WHERE user_id = ?");
     * // Generates: SELECT COUNT(*) FROM orders WHERE user_id = ?   (entityName is ignored when full SQL is supplied)
     * }</pre>
     *
     * @param entityName the entity/table name
     * @param sql the complete SQL for the subquery
     * @return a {@link SubQuery}
     * @throws IllegalArgumentException if {@code sql} is {@code null} or empty
     * @see #subQuery(String)
     * @deprecated when the full SQL is supplied, {@code entityName} is not used to build the
     *             subquery; use {@link #subQuery(String)} instead.
     */
    @Deprecated
    public static SubQuery subQuery(final String entityName, final String sql) {
        return new SubQuery(entityName, sql);
    }

    /**
     * Creates a SubQuery from raw SQL.
     * This provides complete control over the subquery content.
     *
     * <p><b>Warning:</b> {@code sql} is included verbatim in the generated query. Do not build it
     * from untrusted input.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery(
     *     "SELECT user_id FROM orders WHERE total > 1000 GROUP BY user_id"
     * );
     * // Generates: SELECT user_id FROM orders WHERE total > 1000 GROUP BY user_id
     * }</pre>
     *
     * @param sql the complete SQL for the subquery (must not be {@code null} or empty)
     * @return a {@link SubQuery}
     * @throws IllegalArgumentException if {@code sql} is {@code null} or empty
     */
    public static SubQuery subQuery(final String sql) {
        return new SubQuery(sql);
    }

    /**
     * Creates a {@link Limit} clause to restrict the number of rows returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = Filters.limit(10);
     * // Results in SQL like: LIMIT 10
     * }</pre>
     *
     * @param count the maximum number of rows to return (must be non-negative)
     * @return a {@link Limit} clause
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public static Limit limit(final int count) {
        return new Limit(count);
    }

    /**
     * Creates a {@link Limit} clause with a count and offset.
     * Used for pagination of results. Note the parameter order is {@code (count, offset)} —
     * the first argument is the maximum number of rows to return, and the second is the number
     * of rows to skip. When {@code offset == 0}, the rendered SQL omits the {@code OFFSET} clause.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = Filters.limit(20, 10);
     * // Results in SQL like: LIMIT 20 OFFSET 10 (skip 10, take 20)
     * }</pre>
     *
     * @param count the maximum number of rows to return (must be non-negative)
     * @param offset the number of rows to skip (must be non-negative)
     * @return a {@link Limit} clause
     * @throws IllegalArgumentException if {@code count} or {@code offset} is negative
     */
    public static Limit limit(final int count, final int offset) {
        return new Limit(count, offset);
    }

    /**
     * Creates a LIMIT clause from a string expression, formatting and validating it against a fixed grammar.
     * The expression is trimmed, its internal whitespace collapsed, and its SQL keywords upper-cased (parameter
     * names left intact); a {@code "LIMIT "} prefix is prepended when it begins with a digit, {@code '?'},
     * {@code ':'}, or <code>"#{"</code>. It must be one of {@code LIMIT count}, {@code LIMIT count OFFSET offset},
     * MySQL's {@code LIMIT offset, count}, or the SQL:2008
     * {@code [OFFSET offset ROWS] FETCH NEXT/FIRST count ROWS ONLY} forms — where each number is an integer or a
     * {@code ?} / {@code :name} / <code>#{name}</code> placeholder — otherwise an {@link IllegalArgumentException}
     * is thrown. Integer forms are parsed into concrete count/offset; placeholder forms stay opaque. See
     * {@link Limit#Limit(String)} for full details.
     *
     * <p>When the condition is rendered by a SQL builder, a parsed expression is emitted in the target
     * dialect's pagination syntax (so MySQL's comma form and the {@code FETCH} forms are re-rendered per
     * dialect). An opaque (placeholder) expression is re-rendered in the dialect's {@code FETCH} syntax only
     * when the dialect paginates with {@code OFFSET}/{@code FETCH} (Oracle, DB2 or SQL Server, per
     * {@link SqlDialect.ProductInfo}) and it is a generic {@code LIMIT count [OFFSET offset]} form; otherwise
     * it is emitted verbatim.</p>
     *
     * <p><b>Warning:</b> {@code expr} is included verbatim in the generated SQL. Do not build it
     * from untrusted input.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = Filters.limit("10 OFFSET 20");
     * // Renders as: LIMIT 10 OFFSET 20
     * }</pre>
     *
     * @param expr the limit expression as a string (must not be {@code null}, empty, or blank, and must match
     *             one of the accepted forms)
     * @return a {@link Limit} clause
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, blank, or not an accepted limit form
     */
    public static Limit limit(final String expr) {
        return new Limit(expr);
    }
}
