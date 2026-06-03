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

import static com.landawn.abacus.util.SK._SPACE;

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Represents a LIMIT clause in SQL queries to restrict the number of rows returned.
 * This class supports a simple LIMIT (count only), LIMIT with OFFSET for pagination,
 * and a custom expression form for database-specific syntax. The LIMIT clause is essential
 * for controlling result set size and implementing efficient data retrieval strategies,
 * especially for large datasets.
 *
 * <p>This class provides three ways to create LIMIT clauses:
 * <ul>
 *   <li>Simple limit with count only — produces {@code LIMIT n}</li>
 *   <li>Limit with count and offset for pagination — produces {@code LIMIT n OFFSET m}</li>
 *   <li>Custom expression for database-specific syntax (e.g., MySQL's {@code LIMIT offset, count}) via {@link #Limit(String)}</li>
 * </ul>
 *
 * <p>All numeric APIs consistently use {@code (count, offset)} parameter order:
 * {@link com.landawn.abacus.query.AbstractQueryBuilder#limit(int, int)},
 * {@link Criteria.Builder#limit(int, int)}, and
 * {@link com.landawn.abacus.query.DynamicQuery.Builder#limit(int, int)}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Limit to first 10 rows
 * Limit limit1 = new Limit(10);
 * // SQL: LIMIT 10
 *
 * // Pagination: skip 50 rows then return up to 20 (count=20, offset=50)
 * Limit limit2 = new Limit(20, 50);
 * // SQL: LIMIT 20 OFFSET 50
 *
 * // Custom expression for specific databases
 * Limit limit3 = new Limit("10 OFFSET 20");
 * // SQL: LIMIT 10 OFFSET 20
 * }</pre>
 *
 * @see Clause
 * @see AbstractCondition
 */
public class Limit extends Clause {

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
     * // topFive.toString() returns "LIMIT 5"
     *
     * // Limit search results to 100
     * Limit searchLimit = new Limit(100);
     * // searchLimit.toString() returns "LIMIT 100"
     *
     * // Boundary: zero is allowed
     * Limit none = new Limit(0);
     * // none.toString() returns "LIMIT 0"
     *
     * // Edge: a negative count is rejected
     * Limit bad = new Limit(-1);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param count the maximum number of rows to return. Must be non-negative.
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public Limit(final int count) {
        this(count, 0);
    }

    /**
     * Creates a LIMIT clause with both count and offset.
     * When {@code offset} is {@code 0}, the rendered SQL omits the {@code OFFSET} clause and produces
     * {@code LIMIT count} only.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Page 3: Results 21-30 (count=10, offset=20)
     * Limit page3 = new Limit(10, 20);
     * // page3.toString() returns "LIMIT 10 OFFSET 20"
     *
     * // offset == 0 omits the OFFSET clause
     * Limit firstPage = new Limit(10, 0);
     * // firstPage.toString() returns "LIMIT 10"
     *
     * // Edge: a negative offset (or count) is rejected
     * Limit bad = new Limit(10, -1);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param count the maximum number of rows to return. Must be non-negative.
     * @param offset the number of rows to skip before returning results. Must be non-negative.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative
     */
    public Limit(final int count, final int offset) {
        super(Operator.LIMIT, Expression.of(offset == 0 ? String.valueOf(N.checkArgNotNegative(count, "count"))
                : N.checkArgNotNegative(count, "count") + " OFFSET " + N.checkArgNotNegative(offset, "offset")));

        this.count = count;
        this.offset = offset;
    }

    /**
     * Creates a LIMIT clause from a string expression.
     * This constructor allows for custom LIMIT expressions to accommodate database-specific
     * syntax or complex limit scenarios that can't be expressed with simple count/offset.
     *
     * <p>If the expression starts with a digit, {@code '?'}, {@code ':'}, or <code>"#{"</code>, the literal
     * {@code "LIMIT "} prefix is added automatically; otherwise the expression is used as-is.
     * After this normalization, {@link #getExpression()} returns the prefixed form and
     * {@link #toString(NamingPolicy)} renders it directly without inserting an additional {@code LIMIT}.</p>
     *
     * <p>Note: {@link #getCount()} returns {@link Integer#MAX_VALUE} and {@link #getOffset()} returns
     * {@code 0} when the instance is constructed via this constructor, regardless of the expression contents.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Standard LIMIT with OFFSET (numeric prefix triggers automatic "LIMIT " prepend)
     * Limit standard = new Limit("10 OFFSET 20");
     * // toString() -> "LIMIT 10 OFFSET 20"
     *
     * // MySQL-style limit (offset, count)
     * Limit mysql = new Limit("20, 10");
     * // toString() -> "LIMIT 20, 10"
     *
     * // Non-numeric prefix is kept verbatim (no automatic "LIMIT " prepend)
     * Limit custom = new Limit("FETCH FIRST 10 ROWS ONLY");
     * // toString() -> "FETCH FIRST 10 ROWS ONLY"
     *
     * // Edge: a null, empty, or blank expression is rejected
     * Limit bad1 = new Limit((String) null);   // throws IllegalArgumentException
     * Limit bad2 = new Limit("");              // throws IllegalArgumentException
     * Limit bad3 = new Limit("   ");           // throws IllegalArgumentException
     * }</pre>
     *
     * @param expr the custom LIMIT expression as a string. Must not be {@code null}, empty, or blank.
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     */
    public Limit(final String expr) {
        this(normalizeExpression(expr), true);
    }

    private Limit(final String normalizedExpr, @SuppressWarnings("unused") final boolean normalized) {
        super(Operator.LIMIT, Expression.of(conditionExprFromNormalized(normalizedExpr)));

        this.expr = normalizedExpr;
        this.count = Integer.MAX_VALUE;
        this.offset = 0;
    }

    /**
     * Returns the custom expression string if one was provided.
     * This method returns the normalized expression string from the string constructor
     * (which may have {@code "LIMIT "} prepended if the input starts with a digit, {@code '?'},
     * {@code ':'}, or <code>"#{"</code>), or {@code null} if the Limit was created with
     * count/offset parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Limit created with a custom expression
     * Limit customLimit = new Limit("10 OFFSET 20");
     * String expr = customLimit.getExpression();
     * // Returns: "LIMIT 10 OFFSET 20"
     *
     * // Limit created with count/offset returns null
     * Limit numericLimit = new Limit(20, 10);
     * String noExpr = numericLimit.getExpression();
     * // Returns: null
     * }</pre>
     *
     * @return the custom expression string, or {@code null} if constructed with count/offset parameters
     */
    public String getExpression() {
        return expr;
    }

    /**
     * Gets the maximum number of rows to return.
     * For Limit instances created with a custom expression, this returns {@link Integer#MAX_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple count limit
     * Limit limit = new Limit(25);
     * int count = limit.getCount();
     * // Returns: 25
     *
     * // Limit with offset (count=50, offset=100)
     * Limit paged = new Limit(50, 100);
     * int pageCount = paged.getCount();
     * // Returns: 50
     *
     * // Custom expression limit
     * Limit custom = new Limit("10 OFFSET 20");
     * int customCount = custom.getCount();
     * // Returns: Integer.MAX_VALUE
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
     * // Limit with offset for pagination (count=10, offset=20)
     * Limit page3 = new Limit(10, 20);
     * int offset = page3.getOffset();
     * // Returns: 20
     *
     * // Simple count-only limit
     * Limit simple = new Limit(10);
     * int noOffset = simple.getOffset();
     * // Returns: 0
     *
     * // Custom expression limit
     * Limit custom = new Limit("10 OFFSET 20");
     * int customOffset = custom.getOffset();
     * // Returns: 0
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
     * new Limit(10).getParameters();              // returns [] (empty, immutable)
     * new Limit(10, 20).getParameters();          // returns []
     *
     * // Edge: even an expression containing a placeholder yields no bound parameters
     * new Limit("? OFFSET ?").getParameters();    // returns []
     * }</pre>
     *
     * @return an empty immutable list as LIMIT has no parameters
     */
    @Override
    public ImmutableList<Object> getParameters() {
        return ImmutableList.empty();
    }

    /**
     * Converts this LIMIT clause to its string representation according to the specified naming policy.
     * The output format depends on how the Limit was constructed:
     * <ul>
     *   <li>Custom expression (non-empty {@code expr}): returns the normalized expression as-is (which may
     *       have {@code "LIMIT "} prepended, per {@link #Limit(String)})</li>
     *   <li>Uninitialized instance (no expression and {@code null} operator, e.g. produced by the
     *       package-private default constructor during Kryo deserialization): returns the literal
     *       {@code "null"} (consistent with {@link Cell#toString(NamingPolicy)}), not {@code "LIMIT 0"}</li>
     *   <li>Count only (offset {@code == 0}): returns {@code "LIMIT count"}</li>
     *   <li>Count with offset ({@code offset > 0}): returns {@code "LIMIT count OFFSET offset"}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new Limit(10).toString(NamingPolicy.NO_CHANGE);            // returns "LIMIT 10"
     * new Limit(10, 20).toString(NamingPolicy.NO_CHANGE);        // returns "LIMIT 10 OFFSET 20"
     *
     * // Edge: a numeric-prefixed expression keeps its auto-prepended "LIMIT "
     * new Limit("10 OFFSET 20").toString(NamingPolicy.NO_CHANGE); // returns "LIMIT 10 OFFSET 20"
     *
     * // Edge: a non-numeric expression is rendered verbatim (no "LIMIT " prepend)
     * new Limit("FETCH FIRST 10 ROWS ONLY").toString(NamingPolicy.NO_CHANGE);
     * // returns "FETCH FIRST 10 ROWS ONLY"
     * }</pre>
     *
     * @param namingPolicy the naming policy parameter is currently ignored — LIMIT operates on numeric
     *                      values or a raw expression, not property names
     * @return the string representation of this LIMIT clause; {@code "null"} for an uninitialized instance
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (Strings.isNotEmpty(expr)) {
            return expr;
        }

        // Uninitialized instance (e.g., from Kryo default constructor): render the operator
        // consistently with Cell.toString instead of pretending to be "LIMIT 0".
        if (operator() == null) {
            return Strings.NULL;
        }

        return offset > 0 ? SK.LIMIT + _SPACE + count + _SPACE + SK.OFFSET + _SPACE + offset : SK.LIMIT + _SPACE + count;
    }

    /**
     * Computes the hash code for this LIMIT clause.
     * The hash code is calculated based on either the custom expression (if present)
     * or the combination of count and offset values. This ensures that Limit instances
     * with the same logical content have the same hash code.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new Limit(10, 20).hashCode() == new Limit(10, 20).hashCode();                   // true
     * new Limit("10 OFFSET 20").hashCode() == new Limit("10 OFFSET 20").hashCode();   // true
     *
     * // Edge: different count/offset -> different hash code
     * new Limit(10).hashCode() == new Limit(20).hashCode();   // (typically) false
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
     * Two Limit instances are considered equal if either:
     * <ul>
     *   <li>both have a non-empty custom expression and the expressions are equal, or</li>
     *   <li>neither has a custom expression and both have the same count and offset values.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new Limit(10, 20).equals(new Limit(10, 20));                   // returns true
     * new Limit("10 OFFSET 20").equals(new Limit("10 OFFSET 20"));   // returns true
     *
     * // Edge: different count -> not equal
     * new Limit(10).equals(new Limit(20));   // returns false
     *
     * // Edge: the numeric form and the expression form are never equal,
     * // even when they render to the same SQL
     * new Limit(10, 20).equals(new Limit("10 OFFSET 20"));   // returns false
     *
     * new Limit(10).equals(null);   // returns false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the object is a Limit with the same {@code expr} or matching count/offset values
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Limit other) {
            if (Strings.isNotEmpty(expr)) {
                return Strings.isNotEmpty(other.expr) && expr.equals(other.expr);
            } else {
                return Strings.isEmpty(other.expr) && (count == other.count) && (offset == other.offset);
            }
        }

        return false;
    }

    private static String normalizeExpression(final String expr) {
        if (Strings.isEmpty(expr)) {
            throw new IllegalArgumentException("Limit expression cannot be null or empty");
        }

        final String trimmed = expr.trim();

        if (Strings.isEmpty(trimmed)) {
            throw new IllegalArgumentException("Limit expression cannot be null or empty");
        }

        return shouldPrefixLimit(trimmed) ? SK.LIMIT + _SPACE + trimmed : trimmed;
    }

    private static String conditionExprFromNormalized(final String normalizedExpr) {
        if (Strings.startsWithIgnoreCase(normalizedExpr, SK.LIMIT + _SPACE)) {
            return normalizedExpr.substring(SK.LIMIT.length() + 1).trim();
        }

        return normalizedExpr;
    }

    private static boolean shouldPrefixLimit(final String expr) {
        final char firstChar = expr.charAt(0);

        return Character.isDigit(firstChar) || firstChar == '?' || firstChar == ':' || (firstChar == '#' && expr.length() > 1 && expr.charAt(1) == '{');
    }
}
