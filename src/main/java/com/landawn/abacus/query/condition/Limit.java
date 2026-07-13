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

import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Represents a LIMIT clause in SQL queries to restrict the number of rows returned.
 * This class supports a simple LIMIT (count only), LIMIT with OFFSET for pagination,
 * and a string expression form covering the standard pagination syntaxes. The LIMIT clause is essential
 * for controlling result set size and implementing efficient data retrieval strategies,
 * especially for large datasets.
 *
 * <p>This class provides three ways to create LIMIT clauses:
 * <ul>
 *   <li>Simple limit with count only — produces {@code LIMIT n}</li>
 *   <li>Limit with count and offset for pagination — produces {@code LIMIT n OFFSET m}</li>
 *   <li>String expression via {@link #Limit(String)}, which is formatted (whitespace collapsed, keywords
 *       upper-cased) and validated against a fixed grammar — {@code LIMIT n}, {@code LIMIT n OFFSET m},
 *       MySQL's {@code LIMIT offset, count}, and the SQL:2008
 *       {@code OFFSET m ROW[S] FETCH NEXT/FIRST n ROW[S] ONLY} / {@code FETCH FIRST/NEXT n ROW[S] ONLY} forms,
 *       where each number may be an integer or a {@code ?} / {@code :name} / <code>#{name}</code> placeholder.
 *       Integer forms are parsed into concrete {@code count}/{@code offset} (the original literal is retained);
 *       placeholder forms stay opaque; any other input is rejected with an {@link IllegalArgumentException}</li>
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
 * // String expression (parsed into count=10, offset=20)
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

    private String literal;

    private boolean isResolved;

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
        this.isResolved = true;
    }

    /**
     * Creates a LIMIT clause from a string expression, formatting and validating it against a fixed grammar.
     *
     * <p><b>Formatting.</b> The expression is trimmed, its internal whitespace runs are collapsed to a single
     * space, and its SQL keywords ({@code LIMIT}, {@code OFFSET}, {@code FETCH}, {@code FIRST}, {@code NEXT},
     * {@code ROW}/{@code ROWS}, {@code ONLY}) are upper-cased (parameter names inside {@code #{...}} or after
     * {@code :} keep their original case). If the expression starts with a digit, {@code '?'}, {@code ':'}, or
     * <code>"#{"</code>, a {@code "LIMIT "} prefix is added automatically; otherwise it is used as-is. This
     * formatted string is what {@link #literal()} and {@link #toSql(NamingPolicy)} return.</p>
     *
     * <p><b>Accepted grammar.</b> Each number slot below is either an integer literal or a {@code ?} /
     * {@code :name} / <code>#{name}</code> placeholder:</p>
     * <ul>
     *   <li>{@code LIMIT count}</li>
     *   <li>{@code LIMIT count OFFSET offset}</li>
     *   <li>MySQL's {@code LIMIT offset, count}</li>
     *   <li>SQL:2008 {@code OFFSET offset ROW[S] FETCH NEXT/FIRST count ROW[S] ONLY}</li>
     *   <li>SQL:2008 {@code FETCH FIRST/NEXT count ROW[S] ONLY} (offset {@code 0})</li>
     * </ul>
     *
     * <p><b>Parsing.</b> When every number slot is an integer literal, {@link #count()} and
     * {@link #offset()} return their concrete values. When a slot is a placeholder (or an integer literal
     * that overflows {@code int}), the value stays unresolved and opaque: {@link #count()} returns
     * {@link Integer#MAX_VALUE} and {@link #offset()} returns {@code 0}. Anything that does not match the
     * grammar — including a float or negative number, a keyword typo, or unrelated syntax — is rejected with
     * an {@link IllegalArgumentException}.</p>
     *
     * <p><b>&#9888;&#65039;</b> When this condition is rendered by a SQL builder, a parsed expression is emitted in the
     * target dialect's pagination syntax from its {@code count}/{@code offset} (so, e.g., MySQL's comma
     * form and the {@code FETCH} forms are re-rendered per dialect). An opaque (placeholder) expression is
     * re-rendered in the dialect's {@code FETCH} syntax only when the dialect paginates with
     * {@code OFFSET}/{@code FETCH} (Oracle, DB2 or SQL Server, per
     * {@link com.landawn.abacus.query.SqlDialect.ProductInfo}) and it is a generic
     * {@code LIMIT count [OFFSET offset]} form; otherwise it is emitted verbatim.
     * {@link #toSql(NamingPolicy)} itself is dialect-agnostic and always returns the formatted literal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Standard LIMIT with OFFSET (numeric prefix triggers automatic "LIMIT " prepend)
     * Limit standard = new Limit("10 OFFSET 20");
     * // toString() -> "LIMIT 10 OFFSET 20"; count() -> 10; offset() -> 20
     *
     * // Case-insensitive input with collapsed whitespace
     * Limit lower = new Limit("limit 10   offset 20");
     * // toString() -> "LIMIT 10 OFFSET 20"
     *
     * // MySQL-style limit (offset, count)
     * Limit mysql = new Limit("20, 10");
     * // toString() -> "LIMIT 20, 10"; count() -> 10; offset() -> 20
     *
     * // SQL:2008 FETCH form (offset and count parsed; normalized literal retained)
     * Limit fetch = new Limit("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY");
     * // toString() -> "OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY"; count() -> 20; offset() -> 5
     *
     * // Placeholder expression stays opaque
     * Limit ph = new Limit("? OFFSET ?");
     * // toString() -> "LIMIT ? OFFSET ?"; count() -> Integer.MAX_VALUE; offset() -> 0
     *
     * // Edge: a null, empty, or blank expression is rejected
     * Limit bad1 = new Limit((String) null);   // throws IllegalArgumentException
     * Limit bad2 = new Limit("");              // throws IllegalArgumentException
     *
     * // Edge: a float, negative, or otherwise malformed expression is rejected
     * Limit bad3 = new Limit("LIMIT 1.0");                       // throws IllegalArgumentException
     * Limit bad4 = new Limit("LIMIT -1");                        // throws IllegalArgumentException
     * Limit bad5 = new Limit("OFFSET 5 ROWS NEXT 20 ROWS ONLY"); // throws IllegalArgumentException (missing FETCH)
     * }</pre>
     *
     * @param expr the LIMIT expression as a string. Must not be {@code null}, empty, or blank, and must
     *             match one of the accepted forms.
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, blank, or not an accepted limit form
     */
    public Limit(final String expr) {
        this(prepare(expr));
    }

    private Limit(final Prepared prepared) {
        super(Operator.LIMIT, Expression.of(prepared.conditionExpr));

        this.literal = prepared.literal;
        this.count = prepared.count;
        this.offset = prepared.offset;
        this.isResolved = prepared.resolved;
    }

    /**
     * Immutable carrier for the values a {@link #Limit(String)} needs, computed once by {@link #prepare(String)}
     * so they can be handed to the private constructor before its mandatory {@code super(...)} call.
     */
    private record Prepared(String conditionExpr, String literal, int count, int offset, boolean resolved) {
    }

    /**
     * Formats {@code expr} (see {@link #normalizeAndFormat(String)}), validates it against the accepted
     * grammar, and, when every number slot is an integer literal, parses its {@code count}/{@code offset}.
     * The wrapped-condition expression is built from the count/offset slot tokens in canonical
     * {@code count [OFFSET offset]} order (so it always starts with a number slot and never collides with a
     * clause keyword such as {@code OFFSET}).
     *
     * @param expr the raw expression passed to {@link #Limit(String)}
     * @return the prepared values for the private constructor
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank, or is not one of the
     *         accepted limit forms
     */
    private static Prepared prepare(final String expr) {
        final String literal = normalizeAndFormat(expr);
        final String[] slots = matchSlots(literal);

        if (slots == null) {
            throw new IllegalArgumentException("Invalid LIMIT expression. Supported forms are"
                    + " 'LIMIT n', 'LIMIT n OFFSET m', 'LIMIT offset, count' and '[OFFSET m ROWS] FETCH FIRST|NEXT n ROWS ONLY',"
                    + " where each number may be an integer literal or a '?', ':name' or '#{name}' placeholder");
        }

        final String countToken = slots[0];
        final String offsetToken = slots[1];
        final String conditionExpr = offsetToken == null ? countToken : countToken + _SPACE + SK.OFFSET + _SPACE + offsetToken;

        final Integer count = toInt(countToken);
        final Integer offset = offsetToken == null ? Integer.valueOf(0) : toInt(offsetToken);

        // A placeholder slot (or an integer literal that overflows int) leaves the value unresolved: the
        // expression is accepted but stays opaque (count == MAX_VALUE, offset == 0), rendered from its literal.
        if (count == null || offset == null) {
            return new Prepared(conditionExpr, literal, Integer.MAX_VALUE, 0, false);
        }

        return new Prepared(conditionExpr, literal, count, offset, true);
    }

    /**
     * Returns the LIMIT literal string if one was provided.
     * This method returns the formatted literal from the string constructor — trimmed, with internal
     * whitespace collapsed and SQL keywords upper-cased, and possibly with a {@code "LIMIT "} prefix added
     * (when the input starts with a digit, {@code '?'}, {@code ':'}, or <code>"#{"</code>) — or {@code null}
     * if the Limit was created with count/offset parameters. The literal is retained even when the expression
     * was parsed into concrete {@code count}/{@code offset} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Limit created with a string expression (retained even though it is parsed)
     * Limit customLimit = new Limit("10 OFFSET 20");
     * String literal = customLimit.literal();
     * // Returns: "LIMIT 10 OFFSET 20"
     *
     * // A FETCH-style expression keeps its verbatim literal
     * Limit fetch = new Limit("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY");
     * // fetch.literal() returns: "OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY"
     *
     * // Limit created with count/offset returns null
     * Limit numericLimit = new Limit(20, 10);
     * String noLiteral = numericLimit.literal();
     * // Returns: null
     * }</pre>
     *
     * @return the LIMIT literal string, or {@code null} if constructed with count/offset parameters
     */
    public String literal() {
        return literal;
    }

    /**
     * Checks whether this Limit was created from a string expression (see {@link #Limit(String)}).
     * When this returns {@code true}, {@link #literal()} is non-null and is what gets rendered;
     * {@link #count()}/{@link #offset()} may hold sentinel values ({@link Integer#MAX_VALUE}/0)
     * if the expression stayed opaque. When it returns {@code false}, the Limit was created with
     * numeric count/offset parameters and {@link #literal()} returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new Limit("10 OFFSET 20").hasLiteral();   // true
     * new Limit(10, 20).hasLiteral();           // false
     * }</pre>
     *
     * @return {@code true} if this Limit was constructed from a string expression, {@code false} otherwise
     * @see #literal()
     */
    public boolean hasLiteral() {
        return literal != null;
    }

    /**
     * Returns the maximum number of rows to return.
     * For a string expression that was parsed (see {@link #Limit(String)}), this returns the parsed count.
     * For a string expression that stayed opaque (a placeholder slot, or an integer literal that overflows
     * {@code int}), this returns {@link Integer#MAX_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple count limit
     * Limit limit = new Limit(25);
     * int count = limit.count();
     * // Returns: 25
     *
     * // Limit with offset (count=50, offset=100)
     * Limit paged = new Limit(50, 100);
     * int pageCount = paged.count();
     * // Returns: 50
     *
     * // Parsed string expression
     * Limit custom = new Limit("10 OFFSET 20");
     * int customCount = custom.count();
     * // Returns: 10
     *
     * // Opaque (placeholder) expression
     * Limit opaque = new Limit("? OFFSET ?");
     * int opaqueCount = opaque.count();
     * // Returns: Integer.MAX_VALUE
     * }</pre>
     *
     * @return the row count limit, or {@link Integer#MAX_VALUE} for an opaque (unparsed) string expression
     */
    public int count() {
        return count;
    }

    /**
     * Returns the number of rows to skip before returning results.
     * For Limit instances created with only a count, this returns 0.
     * For a string expression that was parsed (see {@link #Limit(String)}), this returns the parsed offset.
     * For a string expression that stayed opaque (a placeholder slot, or an integer literal that overflows
     * {@code int}), this returns 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Limit with offset for pagination (count=10, offset=20)
     * Limit page3 = new Limit(10, 20);
     * int offset = page3.offset();
     * // Returns: 20
     *
     * // Simple count-only limit
     * Limit simple = new Limit(10);
     * int noOffset = simple.offset();
     * // Returns: 0
     *
     * // Parsed string expression
     * Limit custom = new Limit("10 OFFSET 20");
     * int customOffset = custom.offset();
     * // Returns: 20
     *
     * // Opaque (placeholder) expression
     * Limit opaque = new Limit("? OFFSET ?");
     * int opaqueOffset = opaque.offset();
     * // Returns: 0
     * }</pre>
     *
     * @return the offset value, or 0 if constructed with only count or with an opaque (unparsed) string expression
     */
    public int offset() {
        return offset;
    }

    /**
     * Returns whether the count and offset are resolved numeric values rather than sentinels for an opaque expression.
     *
     * @return {@code true} for numeric limits and parsed numeric expressions; {@code false} for opaque expressions
     */
    public boolean isResolved() {
        return isResolved;
    }

    /**
     * Returns the resolved row count when available.
     *
     * @return the resolved count, or an empty optional for an opaque expression
     */
    public OptionalInt resolvedCount() {
        return isResolved() ? OptionalInt.of(count) : OptionalInt.empty();
    }

    /**
     * Returns the resolved row offset when available.
     *
     * @return the resolved offset, or an empty optional for an opaque expression
     */
    public OptionalInt resolvedOffset() {
        return isResolved() ? OptionalInt.of(offset) : OptionalInt.empty();
    }

    /**
     * Returns the parameters for this LIMIT clause.
     * LIMIT clauses do not have bindable parameters as the count and offset
     * are typically part of the SQL structure itself, not parameterized values.
     * This method always returns an empty list.
     *
     * <p>If the expression form ({@link #Limit(String)}) contains placeholders ({@code ?} or
     * named parameters), this class does not track them; the caller is responsible for binding
     * those values separately when preparing the statement.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new Limit(10).parameters();              // returns [] (empty, immutable)
     * new Limit(10, 20).parameters();          // returns []
     *
     * // Edge: even an expression containing a placeholder yields no bound parameters
     * new Limit("? OFFSET ?").parameters();    // returns []
     * }</pre>
     *
     * @return an empty immutable list as LIMIT has no parameters
     */
    @Override
    public ImmutableList<Object> parameters() {
        return ImmutableList.empty();
    }

    /**
     * Converts this LIMIT clause to its SQL representation according to the specified naming policy.
     * The output format depends on how the Limit was constructed:
     * <ul>
     *   <li>String expression (non-empty {@code literal}): returns the formatted literal as-is (whitespace
     *       collapsed and keywords upper-cased, possibly with {@code "LIMIT "} prepended, per
     *       {@link #Limit(String)}), even when it was parsed into concrete count/offset</li>
     *   <li>Uninitialized instance (no expression and {@code null} operator, e.g. produced by the
     *       package-private default constructor during Kryo deserialization): returns the literal
     *       {@code "null"} (consistent with {@link Cell#toSql(NamingPolicy)}), not {@code "LIMIT 0"}</li>
     *   <li>Count only (offset {@code == 0}): returns {@code "LIMIT count"}</li>
     *   <li>Count with offset ({@code offset > 0}): returns {@code "LIMIT count OFFSET offset"}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * new Limit(10).toSql(NamingPolicy.NO_CHANGE);            // returns "LIMIT 10"
     * new Limit(10, 20).toSql(NamingPolicy.NO_CHANGE);        // returns "LIMIT 10 OFFSET 20"
     *
     * // Edge: a numeric-prefixed expression keeps its auto-prepended "LIMIT "
     * new Limit("10 OFFSET 20").toSql(NamingPolicy.NO_CHANGE); // returns "LIMIT 10 OFFSET 20"
     *
     * // Edge: a non-numeric expression is rendered verbatim (no "LIMIT " prepend)
     * new Limit("FETCH FIRST 10 ROWS ONLY").toSql(NamingPolicy.NO_CHANGE);
     * // returns "FETCH FIRST 10 ROWS ONLY"
     * }</pre>
     *
     * @param namingPolicy the naming policy parameter is currently ignored — LIMIT operates on numeric
     *                      values or a raw expression, not property names
     * @return the SQL representation of this LIMIT clause; {@code "null"} for an uninitialized instance
     */
    @Override
    public String toSql(final NamingPolicy namingPolicy) {
        if (Strings.isNotEmpty(literal)) {
            return literal;
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
     * @return the hash code based on literal if present, otherwise based on count and offset
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((operator() == null) ? 0 : operator().hashCode());

        if (Strings.isNotEmpty(literal)) {
            return (h * 31) + literal.hashCode();
        } else {
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
     * @return {@code true} if the object is of the same class with the same {@code literal} or matching count/offset values
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Limit other = (Limit) obj;

        if (!N.equals(operator(), other.operator())) {
            return false;
        }

        if (Strings.isNotEmpty(literal)) {
            return Strings.isNotEmpty(other.literal) && literal.equals(other.literal);
        } else {
            return Strings.isEmpty(other.literal) && (count == other.count) && (offset == other.offset);
        }
    }

    /** SQL keywords upper-cased by {@link #normalizeAndFormat(String)} (parameter names are left untouched). */
    private static final Set<String> KEYWORDS = Set.of("LIMIT", "OFFSET", "FETCH", "FIRST", "NEXT", "ROW", "ROWS", "ONLY");

    /**
     * Matches a MyBatis-style <code>#{...}</code> parameter placeholder. Its body (a case-sensitive parameter
     * name that may contain surrounding spaces) is copied verbatim during normalization &mdash; never
     * whitespace-collapsed and never keyword-upper-cased.
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("#\\{[^}]*\\}");

    /** A number slot: an integer literal, or a {@code ?} / {@code :name} / <code>#{name}</code> parameter placeholder. */
    private static final String SLOT = "(?:\\d+|\\?|:\\w+|#\\{[^}]+\\})";

    /**
     * Matches the {@code LIMIT}-family forms: {@code LIMIT count}, {@code LIMIT count OFFSET offset}, and
     * MySQL's {@code LIMIT offset, count}. Group 1 is the leading slot, group 2 the MySQL trailing count
     * slot, group 3 the {@code OFFSET} slot; each slot is an integer or a placeholder (see {@link #SLOT}).
     */
    private static final Pattern LIMIT_FAMILY_PATTERN = Pattern.compile("LIMIT\\s+(" + SLOT + ")(?:\\s*,\\s*(" + SLOT + ")|\\s+OFFSET\\s+(" + SLOT + "))?",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches the SQL:2008 {@code FETCH}-family forms: {@code FETCH FIRST|NEXT count ROW[S] ONLY} optionally
     * preceded by {@code OFFSET offset ROW[S]}. Group 1 is the optional {@code OFFSET} slot, group 2 the
     * {@code FETCH} count slot; each slot is an integer or a placeholder (see {@link #SLOT}).
     */
    private static final Pattern FETCH_FAMILY_PATTERN = Pattern
            .compile("(?:OFFSET\\s+(" + SLOT + ")\\s+ROWS?\\s+)?FETCH\\s+(?:FIRST|NEXT)\\s+(" + SLOT + ")\\s+ROWS?\\s+ONLY", Pattern.CASE_INSENSITIVE);

    /**
     * Validates {@code expr} against the accepted grammar and, on success, returns its count/offset slot
     * tokens as {@code [countToken, offsetToken]} (where {@code offsetToken} is {@code null} when no offset
     * is present). Returns {@code null} when {@code expr} matches none of the accepted forms.
     *
     * @param expr the normalized, formatted expression
     * @return the {@code [countToken, offsetToken]} slot tokens, or {@code null} if the form is not recognized
     */
    private static String[] matchSlots(final String expr) {
        final Matcher limitMatcher = LIMIT_FAMILY_PATTERN.matcher(expr);

        if (limitMatcher.matches()) {
            final String first = limitMatcher.group(1);

            if (limitMatcher.group(2) != null) {
                // MySQL "LIMIT offset, count".
                return new String[] { limitMatcher.group(2), first };
            } else if (limitMatcher.group(3) != null) {
                // "LIMIT count OFFSET offset".
                return new String[] { first, limitMatcher.group(3) };
            } else {
                // "LIMIT count".
                return new String[] { first, null };
            }
        }

        final Matcher fetchMatcher = FETCH_FAMILY_PATTERN.matcher(expr);

        if (fetchMatcher.matches()) {
            // "[OFFSET offset ROWS] FETCH FIRST|NEXT count ROWS ONLY".
            return new String[] { fetchMatcher.group(2), fetchMatcher.group(1) };
        }

        return null;
    }

    /**
     * Parses a slot token as a non-negative {@code int}, or returns {@code null} when it is a parameter
     * placeholder or an integer literal that overflows {@code int} (in which case the value stays unresolved).
     *
     * @param token a non-null count/offset slot token
     * @return the parsed value, or {@code null} if the token is not a resolvable integer
     */
    private static Integer toInt(final String token) {
        for (int i = 0, len = token.length(); i < len; i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return null;
            }
        }

        try {
            return Integer.parseInt(token);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Normalizes and formats a raw limit expression: trims it, collapses internal whitespace runs to a single
     * space, prepends {@code "LIMIT "} when it starts with a bare number or placeholder, and upper-cases the
     * SQL keywords ({@code LIMIT}, {@code OFFSET}, {@code FETCH}, {@code FIRST}, {@code NEXT}, {@code ROW[S]},
     * {@code ONLY}) while leaving parameter names inside {@code #{...}} / after {@code :} untouched.
     *
     * @param expr the raw expression
     * @return the normalized, formatted expression
     * @throws IllegalArgumentException if {@code expr} is {@code null}, empty, or blank
     */
    private static String normalizeAndFormat(final String expr) {
        if (Strings.isEmpty(expr)) {
            throw new IllegalArgumentException("Limit expression must not be null, empty, or blank");
        }

        final String trimmed = expr.trim();

        if (Strings.isEmpty(trimmed)) {
            throw new IllegalArgumentException("Limit expression must not be null, empty, or blank");
        }

        // Collapse whitespace and upper-case keywords only outside #{...} placeholders; a placeholder body
        // is a case-sensitive parameter name (possibly a keyword like "offset") that must survive verbatim.
        final String formatted = formatOutsidePlaceholders(trimmed);

        return shouldPrefixLimit(formatted) ? SK.LIMIT + _SPACE + formatted : formatted;
    }

    /**
     * Applies {@link #collapseAndUpperCaseKeywords(String)} to every stretch of the expression that lies
     * outside a {@link #PLACEHOLDER_PATTERN} match, copying each matched <code>#{...}</code> placeholder
     * through unchanged.
     *
     * @param expr the trimmed raw expression
     * @return the expression with keywords upper-cased and whitespace collapsed, placeholder bodies preserved
     */
    private static String formatOutsidePlaceholders(final String expr) {
        final Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(expr);
        final StringBuilder sb = new StringBuilder(expr.length());
        int lastEnd = 0;

        while (placeholderMatcher.find()) {
            sb.append(collapseAndUpperCaseKeywords(expr.substring(lastEnd, placeholderMatcher.start())));
            sb.append(placeholderMatcher.group());
            lastEnd = placeholderMatcher.end();
        }

        sb.append(collapseAndUpperCaseKeywords(expr.substring(lastEnd)));

        return sb.toString();
    }

    /**
     * Collapses internal whitespace runs to a single space and upper-cases whole-word SQL keywords in a
     * placeholder-free segment. Leading/trailing whitespace is preserved as a single space so adjacent
     * placeholders stay separated. Keyword matching is whole-token, so a {@code :name} placeholder such as
     * {@code :offset} is left untouched (the token {@code :offset} is not the keyword {@code OFFSET}).
     *
     * @param segment a stretch of the expression that contains no <code>#{...}</code> placeholder
     * @return the collapsed, keyword-upper-cased segment
     */
    private static String collapseAndUpperCaseKeywords(final String segment) {
        if (segment.isEmpty()) {
            return segment;
        }

        final String collapsed = segment.replaceAll("\\s+", " ");
        final boolean leadingSpace = collapsed.charAt(0) == ' ';
        final boolean trailingSpace = collapsed.length() > 1 && collapsed.charAt(collapsed.length() - 1) == ' ';
        final String[] tokens = collapsed.trim().split(" ");
        final StringBuilder sb = new StringBuilder(collapsed.length());

        if (leadingSpace) {
            sb.append(' ');
        }

        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }

            final String token = tokens[i];
            final String upper = token.toUpperCase(Locale.ROOT);

            sb.append(KEYWORDS.contains(upper) ? upper : token);
        }

        if (trailingSpace) {
            sb.append(' ');
        }

        return sb.toString();
    }

    private static boolean shouldPrefixLimit(final String expr) {
        final char firstChar = expr.charAt(0);

        return Character.isDigit(firstChar) || firstChar == '?' || firstChar == ':' || (firstChar == '#' && expr.length() > 1 && expr.charAt(1) == '{');
    }
}
