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
 * This class supports both simple LIMIT (count only) and LIMIT with OFFSET.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Limit to 10 rows
 * Limit limit1 = new Limit(10);
 * // Generates: LIMIT 10
 * 
 * // Limit to 20 rows, starting from row 50
 * Limit limit2 = new Limit(50, 20);
 * // Generates: LIMIT 20 OFFSET 50
 * }</pre>
 */
public class Limit extends AbstractCondition {

    private int count;

    private int offset;

    private String expr;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    Limit() {
    }

    /**
     * Creates a LIMIT clause with the specified row count.
     * This constructor creates a simple LIMIT without OFFSET.
     *
     * @param count the maximum number of rows to return. Must be non-negative.
     * 
     * <p>Example:
     * <pre>{@code
     * Limit limit = new Limit(5);
     * // Generates: LIMIT 5
     * }</pre>
     */
    public Limit(final int count) {
        this(0, count);
    }

    /**
     * Creates a LIMIT clause with both count and offset.
     * This allows pagination by specifying how many rows to skip and how many to return.
     *
     * @param offset the number of rows to skip before returning results. Must be non-negative.
     * @param count the maximum number of rows to return after the offset. Must be non-negative.
     * 
     * <p>Example:
     * <pre>{@code
     * Limit limit = new Limit(100, 20);
     * // Generates: LIMIT 20 OFFSET 100
     * }</pre>
     */
    public Limit(final int offset, final int count) {
        super(Operator.LIMIT);
        this.count = count;
        this.offset = offset;
    }

    /**
     * Creates a LIMIT clause from a string expression.
     * This constructor allows for custom LIMIT expressions.
     *
     * @param expr the custom LIMIT expression as a string
     * 
     * <p>Example:
     * <pre>{@code
     * Limit limit = new Limit("10 OFFSET 20");
     * }</pre>
     */
    public Limit(final String expr) {
        this(0, Integer.MAX_VALUE);

        this.expr = expr;
    }

    /**
     * Returns the custom expression string if one was provided.
     *
     * @return the custom expression string, or null if constructed with count/offset
     */
    public String getExpr() {
        return expr;
    }

    /**
     * Gets the maximum number of rows to return.
     *
     * @return the row count limit
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets the number of rows to skip before returning results.
     *
     * @return the offset value
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the parameters for this LIMIT clause.
     * LIMIT clauses do not have parameters, so this always returns an empty list.
     *
     * @return an empty list
     */
    @Override
    public List<Object> getParameters() {
        return N.emptyList();
    }

    /**
     * Clears any parameters associated with this condition.
     * Since LIMIT has no parameters, this method does nothing.
     */
    @Override
    public void clearParameters() {
        // do nothing.
    }

    /**
     * Attempts to combine this LIMIT with another condition using AND.
     * This operation is not supported for LIMIT clauses.
     *
     * @param condition the condition to combine with
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as LIMIT cannot be combined with AND
     */
    @Override
    public And and(final Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to combine this LIMIT with another condition using OR.
     * This operation is not supported for LIMIT clauses.
     *
     * @param condition the condition to combine with
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as LIMIT cannot be combined with OR
     */
    @Override
    public Or or(final Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to negate this LIMIT clause.
     * This operation is not supported for LIMIT clauses.
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
     *
     * @param namingPolicy the naming policy to apply (though LIMIT typically doesn't need name conversion)
     * @return the string representation of this LIMIT clause, e.g., "LIMIT 10" or "LIMIT 10 OFFSET 20"
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
     *
     * @param obj the object to compare with
     * @return true if the object is a Limit with the same expr or count/offset values
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