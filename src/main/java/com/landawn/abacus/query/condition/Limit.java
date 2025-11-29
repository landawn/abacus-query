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

import static com.landawn.abacus.query.SK._SPACE;

import java.util.List;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

/**
 * Represents a LIMIT clause in SQL queries to restrict the number of rows returned.
 * This class supports both simple LIMIT (count only) and LIMIT with OFFSET for pagination.
 * The LIMIT clause is essential for controlling result set size and implementing efficient
 * data retrieval strategies, especially for large datasets.
 * 
 * <p>This class provides three ways to create LIMIT clauses:
 * <ul>
 *   <li>Simple limit with count only</li>
 *   <li>Limit with offset for pagination</li>
 *   <li>Custom expression for database-specific syntax</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Limit to first 10 rows
 * Limit limit1 = new Limit(10);
 * // SQL: LIMIT 10
 * 
 * // Pagination: Get 20 rows starting from row 50
 * Limit limit2 = new Limit(50, 20);
 * // SQL: LIMIT 20 OFFSET 50
 * 
 * // Custom expression for specific databases
 * Limit limit3 = new Limit("10 OFFSET 20");
 * }</pre>
 * 
 * @see Clause
 * @see AbstractCondition
 */
public class Limit extends AbstractCondition {

    private int count;

    private int offset;

    private String expr;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Limit instance and should not be used 
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Limit() {
    }

    /**
     * Creates a LIMIT clause with the specified row count.
     * This constructor creates a simple LIMIT without OFFSET, returning rows from the beginning
     * of the result set up to the specified count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top 5 customers
     * Limit topFive = new Limit(5);
     * // SQL: SELECT * FROM customers LIMIT 5
     *
     * // Limit search results to 100
     * Limit searchLimit = new Limit(100);
     * // SQL: SELECT * FROM products WHERE name LIKE '%phone%' LIMIT 100
     * }</pre>
     *
     * @param count the maximum number of rows to return. Should be non-negative (typically positive).
     * @throws IllegalArgumentException if count is negative (implementation-dependent)
     */
    public Limit(final int count) {
        this(0, count);
    }

    /**
     * Creates a LIMIT clause with both count and offset.
     * This constructor enables pagination by specifying how many rows to skip (offset)
     * and how many rows to return (count). This is the standard way to implement
     * result pagination in SQL queries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Page 1: First 10 results (offset 0)
     * Limit page1 = new Limit(0, 10);
     * // SQL: SELECT * FROM orders LIMIT 10 OFFSET 0
     *
     * // Page 3: Results 21-30 (offset 20, count 10)
     * Limit page3 = new Limit(20, 10);
     * // SQL: SELECT * FROM orders LIMIT 10 OFFSET 20
     *
     * // Get 50 products starting from the 101st
     * Limit products = new Limit(100, 50);
     * // SQL: SELECT * FROM products LIMIT 50 OFFSET 100
     * }</pre>
     *
     * @param offset the number of rows to skip before returning results. Should be non-negative.
     * @param count the maximum number of rows to return after the offset. Should be non-negative.
     * @throws IllegalArgumentException if offset or count is negative (implementation-dependent)
     */
    public Limit(final int offset, final int count) {
        super(Operator.LIMIT);
        this.count = count;
        this.offset = offset;
    }

    /**
     * Creates a LIMIT clause from a string expression.
     * This constructor allows for custom LIMIT expressions to accommodate database-specific
     * syntax or complex limit scenarios that can't be expressed with simple count/offset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Standard LIMIT with OFFSET
     * Limit standard = new Limit("10 OFFSET 20");
     *
     * // MySQL-style limit (offset, count)
     * Limit mysql = new Limit("20, 10");
     *
     * // Database-specific syntax (e.g., Firebird)
     * Limit custom = new Limit("FIRST 10 SKIP 20");
     * }</pre>
     *
     * @param expr the custom LIMIT expression as a string. Should not be null or empty.
     * @throws IllegalArgumentException if expr is null or empty (implementation-dependent)
     */
    public Limit(final String expr) {
        this(0, Integer.MAX_VALUE);

        this.expr = expr;
    }

    /**
     * Returns the custom expression string if one was provided.
     * This method returns the raw expression string passed to the string constructor,
     * or {@code null} if the Limit was created with count/offset parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit1 = new Limit("10 OFFSET 20");
     * String expr = limit1.getExpr();  // Returns "10 OFFSET 20"
     *
     * Limit limit2 = new Limit(10, 20);
     * String expr2 = limit2.getExpr();  // Returns null
     * }</pre>
     *
     * @return the custom expression string, or {@code null} if constructed with count/offset parameters
     */
    public String getExpr() {
        return expr;
    }

    /**
     * Gets the maximum number of rows to return.
     * For Limit instances created with a custom expression, this returns {@link Integer#MAX_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = new Limit(50, 20);
     * int maxRows = limit.getCount();  // Returns 20
     *
     * Limit customLimit = new Limit("10 OFFSET 20");
     * int count = customLimit.getCount();  // Returns Integer.MAX_VALUE
     * }</pre>
     *
     * @return the row count limit, or {@link Integer#MAX_VALUE} if constructed with a custom expression
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets the number of rows to skip before returning results.
     * For Limit instances created with only a count, this returns 0.
     * For Limit instances created with a custom expression, this also returns 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = new Limit(50, 20);
     * int skip = limit.getOffset();  // Returns 50
     *
     * Limit limitNoOffset = new Limit(10);
     * int skip2 = limitNoOffset.getOffset();  // Returns 0
     *
     * Limit customLimit = new Limit("10 OFFSET 20");
     * int skip3 = customLimit.getOffset();  // Returns 0
     * }</pre>
     *
     * @return the offset value, or 0 if constructed with only count or with a custom expression
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the parameters for this LIMIT clause.
     * LIMIT clauses do not have bindable parameters as the count and offset
     * are typically part of the SQL structure itself, not parameterized values.
     * This method always returns an empty list.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = new Limit(10);
     * List<Object> params = limit.getParameters();  // Returns empty list
     * }</pre>
     *
     * @return an empty list as LIMIT has no parameters
     */
    @Override
    public List<Object> getParameters() {
        return N.emptyList();
    }

    /**
     * This method does nothing for LIMIT clauses.
     * LIMIT clauses do not have parameters that can be cleared, as the row count
     * and offset are stored as primitive values, not as parameter placeholders.
     *
     * <p>This method is a no-op to satisfy the Condition interface contract.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = new Limit(10);
     * limit.clearParameters();                           // Does nothing - no parameters to clear
     * List<Object> parameters = limit.getParameters();   // Returns empty list
     * }</pre>
     */
    @Override
    public void clearParameters() {
        // do nothing.
    }

    /**
     * Attempts to combine this LIMIT with another condition using AND.
     * This operation is not supported for LIMIT clauses as they are not logical
     * conditions that can be combined with AND/OR operators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = new Limit(10);
     * try {
     *     limit.and(someCondition);  // Throws UnsupportedOperationException
     * } catch (UnsupportedOperationException e) {
     *     // Expected behavior
     * }
     * }</pre>
     *
     * @param condition the condition to combine with (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as LIMIT cannot be combined with AND
     */
    @Override
    public And and(final Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to combine this LIMIT with another condition using OR.
     * This operation is not supported for LIMIT clauses as they are not logical
     * conditions that can be combined with AND/OR operators.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = new Limit(10);
     * try {
     *     limit.or(someCondition);  // Throws UnsupportedOperationException
     * } catch (UnsupportedOperationException e) {
     *     // Expected behavior
     * }
     * }</pre>
     *
     * @param condition the condition to combine with (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as LIMIT cannot be combined with OR
     */
    @Override
    public Or or(final Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to negate this LIMIT clause.
     * This operation is not supported for LIMIT clauses as they represent
     * a result set constraint, not a logical condition that can be negated.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = new Limit(10);
     * try {
     *     limit.not();  // Throws UnsupportedOperationException
     * } catch (UnsupportedOperationException e) {
     *     // Expected behavior
     * }
     * }</pre>
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as LIMIT cannot be negated
     */
    @Override
    public Not not() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts this LIMIT clause to its string representation according to the specified naming policy.
     * The output format depends on how the Limit was constructed:
     * - Custom expression: returns the expression as-is
     * - Count only: returns "LIMIT count"
     * - Count with offset: returns "LIMIT count OFFSET offset"
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit1 = new Limit(10);
     * String sql1 = limit1.toString(NamingPolicy.LOWER_CASE);  // "LIMIT 10"
     * 
     * Limit limit2 = new Limit(20, 10);
     * String sql2 = limit2.toString(NamingPolicy.LOWER_CASE);  // "LIMIT 10 OFFSET 20"
     * 
     * Limit limit3 = new Limit("FIRST 5 ROWS");
     * String sql3 = limit3.toString(NamingPolicy.LOWER_CASE);  // "FIRST 5 ROWS"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply (though LIMIT typically doesn't need name conversion)
     * @return the string representation of this LIMIT clause
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (Strings.isNotEmpty(expr)) {
            return expr;
        } else {
            return offset > 0 ? SK.LIMIT + _SPACE + count + _SPACE + SK.OFFSET + _SPACE + offset : SK.LIMIT + _SPACE + count;
        }
    }

    /**
     * Computes the hash code for this LIMIT clause.
     * The hash code is calculated based on either the custom expression (if present)
     * or the combination of count and offset values. This ensures that Limit instances
     * with the same logical content have the same hash code.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit1 = new Limit(10);
     * Limit limit2 = new Limit(10);
     * assert limit1.hashCode() == limit2.hashCode();
     * 
     * Limit limit3 = new Limit("LIMIT 10");
     * Limit limit4 = new Limit("LIMIT 10");
     * assert limit3.hashCode() == limit4.hashCode();
     * }</pre>
     *
     * @return the hash code based on expr if present, otherwise based on count and offset
     */
    @Override
    public int hashCode() {
        if (Strings.isNotEmpty(expr)) {
            return expr.hashCode();
        } else {
            int h = 17;
            h = (h * 31) + count;
            return (h * 31) + offset;
        }
    }

    /**
     * Checks if this LIMIT clause is equal to another object.
     * Two Limit instances are considered equal if:
     * - Both have the same custom expression, or
     * - Both have the same count and offset values
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit1 = new Limit(50, 20);
     * Limit limit2 = new Limit(50, 20);
     * assert limit1.equals(limit2);  // true
     * 
     * Limit limit3 = new Limit("LIMIT 20 OFFSET 50");
     * Limit limit4 = new Limit("LIMIT 20 OFFSET 50");
     * assert limit3.equals(limit4);  // true
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the object is a Limit with the same expr or count/offset values
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Limit other) {
            if (Strings.isNotEmpty(expr)) {
                return expr.equals(other.expr);
            } else {
                return (count == other.count) && (offset == other.offset);
            }
        }

        return false;
    }
}
