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

import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.Strings;

/**
 * Enumeration of SQL operators supported by the condition framework.
 * 
 * <p>This enum defines all the operators that can be used in SQL conditions,
 * including comparison operators, logical operators, join types, and SQL clauses.
 * Each operator has a string representation that corresponds to its SQL syntax.</p>
 * 
 * <p>Categories of operators:</p>
 * <ul>
 *   <li><b>Comparison:</b> EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN, etc.</li>
 *   <li><b>Logical:</b> AND, OR, NOT, XOR</li>
 *   <li><b>Range/Set:</b> BETWEEN, IN, NOT_IN, LIKE</li>
 *   <li><b>Null checks:</b> IS, IS_NOT</li>
 *   <li><b>Subquery:</b> EXISTS, ANY, SOME, ALL</li>
 *   <li><b>Join types:</b> JOIN, LEFT_JOIN, RIGHT_JOIN, FULL_JOIN, etc.</li>
 *   <li><b>Clauses:</b> WHERE, HAVING, GROUP_BY, ORDER_BY, etc.</li>
 *   <li><b>Set operations:</b> UNION, UNION_ALL, INTERSECT, EXCEPT</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Direct operator usage
 * Operator op = Operator.EQUAL;
 * String sql = propertyName + " " + op.toString() + " ?";
 * 
 * // Get operator by name
 * Operator gtOp = Operator.getOperator(">");
 * Operator andOp = Operator.getOperator("AND");
 * 
 * // Check operator type
 * if (operator == Operator.BETWEEN) {
 *     // Handle between logic
 * }
 * }</pre>
 */
public enum Operator {
    /**
     * Equal operator (=).
     * Used for equality comparisons.
     */
    EQUAL(SK.EQUAL),

    /**
     * Not equal operator (!=).
     * Used for inequality comparisons.
     */
    NOT_EQUAL(SK.NOT_EQUAL),

    /**
     * Alternative not equal operator (<>).
     * Some databases prefer this syntax.
     */
    NOT_EQUAL2(SK.NOT_EQUAL2),

    /**
     * NOT logical operator.
     * Used to negate conditions.
     */
    NOT(SK.NOT),

    /**
     * NOT operator symbol (!).
     * Alternative representation of NOT.
     */
    NOT_OP(SK.EXCLAMATION),

    /**
     * XOR (exclusive OR) operator.
     * True when exactly one condition is true.
     */
    XOR(SK.XOR),

    /**
     * LIKE operator.
     * Used for pattern matching with wildcards.
     */
    LIKE(SK.LIKE),

    /**
     * NOT LIKE operator.
     * Negation of LIKE for pattern exclusion.
     */
    NOT_LIKE(NOT + " " + SK.LIKE),

    /**
     * AND logical operator.
     * All conditions must be true.
     */
    AND(SK.AND),

    /**
     * AND operator symbol (&&).
     * Alternative representation of AND.
     */
    AND_OP(SK.AND_OP),

    /**
     * OR logical operator.
     * At least one condition must be true.
     */
    OR(SK.OR),

    /**
     * OR operator symbol (||).
     * Alternative representation of OR.
     */
    OR_OP(SK.OR_OP),

    /**
     * Greater than operator (>).
     * Used for "greater than" comparisons.
     */
    GREATER_THAN(SK.GREATER_THAN),

    /**
     * Greater than or equal operator (>=).
     * Used for "greater than or equal" comparisons.
     */
    GREATER_EQUAL(SK.GREATER_EQUAL),

    /**
     * Less than operator (<).
     * Used for "less than" comparisons.
     */
    LESS_THAN(SK.LESS_THAN),

    /**
     * Less than or equal operator (<=).
     * Used for "less than or equal" comparisons.
     */
    LESS_EQUAL(SK.LESS_EQUAL),

    /**
     * BETWEEN operator.
     * Checks if value is within a range (inclusive).
     */
    BETWEEN(SK.BETWEEN),

    /**
     * NOT BETWEEN operator.
     * Checks if value is outside a range.
     */
    NOT_BETWEEN(NOT + " " + SK.BETWEEN),

    /**
     * IS operator.
     * Used for NULL comparisons (IS NULL).
     */
    IS(SK.IS),

    /**
     * IS NOT operator.
     * Used for NOT NULL comparisons (IS NOT NULL).
     */
    IS_NOT(SK.IS_NOT),

    /**
     * EXISTS operator.
     * Checks if subquery returns any rows.
     */
    EXISTS(SK.EXISTS),

    /**
     * NOT EXISTS operator.
     * Checks if subquery not returns any rows.
     */
    NOT_EXISTS(SK.NOT_EXISTS),

    /**
     * IN operator.
     * Checks if value is in a list or subquery result.
     */
    IN(SK.IN),

    /**
     * NOT IN operator.
     * Checks if value is not in a list or subquery result.
     */
    NOT_IN(SK.NOT_IN),

    /**
     * ANY operator.
     * Compares value with any value from subquery.
     */
    ANY(SK.ANY),

    /**
     * SOME operator.
     * Synonym for ANY operator.
     */
    SOME(SK.SOME),

    /**
     * ALL operator.
     * Compares value with all values from subquery.
     */
    ALL(SK.ALL),

    /**
     * ON operator.
     * Specifies join condition.
     */
    ON(SK.ON),

    /**
     * USING operator.
     * Specifies join columns with same names.
     */
    USING(SK.USING),

    /**
     * Basic JOIN operator.
     * Performs inner join by default.
     */
    JOIN(SK.JOIN),

    /**
     * LEFT JOIN operator.
     * Returns all rows from left table.
     */
    LEFT_JOIN(SK.LEFT_JOIN),

    /**
     * RIGHT JOIN operator.
     * Returns all rows from right table.
     */
    RIGHT_JOIN(SK.RIGHT_JOIN),

    /**
     * FULL JOIN operator.
     * Returns all rows from both tables.
     */
    FULL_JOIN(SK.FULL_JOIN),

    /**
     * CROSS JOIN operator.
     * Cartesian product of two tables.
     */
    CROSS_JOIN(SK.CROSS_JOIN),

    /**
     * INNER JOIN operator.
     * Returns only matching rows.
     */
    INNER_JOIN(SK.INNER_JOIN),

    /**
     * NATURAL JOIN operator.
     * Joins on all columns with same names.
     */
    NATURAL_JOIN(SK.NATURAL_JOIN),

    /**
     * WHERE clause operator.
     * Filters rows based on conditions.
     */
    WHERE(SK.WHERE),

    /**
     * HAVING clause operator.
     * Filters groups after GROUP BY.
     */
    HAVING(SK.HAVING),

    /**
     * GROUP BY clause operator.
     * Groups rows by specified columns.
     */
    GROUP_BY(SK.GROUP_BY),

    /**
     * ORDER BY clause operator.
     * Sorts result set.
     */
    ORDER_BY(SK.ORDER_BY),

    /**
     * LIMIT clause operator.
     * Limits number of returned rows.
     */
    LIMIT(SK.LIMIT),

    /**
     * OFFSET clause operator.
     * Skips specified number of rows.
     */
    OFFSET(SK.OFFSET),

    /**
     * FOR UPDATE clause operator.
     * Locks selected rows for update.
     * @deprecated
     */
    FOR_UPDATE(SK.FOR_UPDATE),

    /**
     * UNION operator.
     * Combines results, removes duplicates.
     */
    UNION(SK.UNION),

    /**
     * UNION ALL operator.
     * Combines results, keeps duplicates.
     */
    UNION_ALL(SK.UNION_ALL),

    /**
     * INTERSECT operator.
     * Returns common rows from queries.
     */
    INTERSECT(SK.INTERSECT),

    /**
     * EXCEPT operator.
     * Returns rows from first query not in second.
     */
    EXCEPT(SK.EXCEPT),

    /**
     * MINUS operator.
     * Synonym for EXCEPT (Oracle syntax).
     */
    MINUS(SK.EXCEPT2),

    /**
     * Empty operator.
     * Special operator representing no operation.
     */
    EMPTY(Strings.EMPTY);

    /**
     * The string representation of this operator.
     */
    private final String name;

    /**
     * Cache for operator lookup by name.
     */
    private static final Map<String, Operator> operatorMap = new HashMap<>();

    /**
     * Constructs an Operator with the specified string representation.
     *
     * @param name the SQL string representation of this operator
     */
    Operator(final String name) {
        this.name = name;
    }

    /**
     * Gets an Operator by its string representation.
     * 
     * <p>This method performs case-insensitive lookup and caches results for performance.
     * It can handle both symbolic operators (like "=", ">") and word operators (like "AND", "OR").</p>
     *
     * @param name the string representation of the operator
     * @return the corresponding Operator enum value, or null if not found
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Operator eq = Operator.getOperator("=");        // Returns EQUAL
     * Operator and = Operator.getOperator("AND");     // Returns AND
     * Operator gt = Operator.getOperator(">");        // Returns GREATER_THAN
     * Operator like = Operator.getOperator("like");   // Returns LIKE (case-insensitive)
     * }</pre>
     */
    public static synchronized Operator getOperator(final String name) {
        if (operatorMap.isEmpty()) {
            final Operator[] values = Operator.values();

            for (final Operator value : values) {
                operatorMap.put(value.name, value);
            }
        }

        Operator operator = operatorMap.get(name);

        if (operator == null) {
            operator = operatorMap.get(name.toUpperCase());

            if (operator != null) {
                operatorMap.put(name, operator);
            }
        }

        return operator;
    }

    /**
     * Gets the string representation of this operator.
     *
     * @return the SQL string representation
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the string representation of this operator.
     *
     * @return the SQL string representation
     */
    @Override
    public String toString() {
        return name;
    }
}