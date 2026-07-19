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
 * Models a SQL row-limiting clause.
 *
 * <p>The numeric constructors render the portable internal form {@code LIMIT count [OFFSET offset]}.
 * The expression constructor accepts and normalizes the following forms:</p>
 * <ul>
 *   <li>{@code LIMIT count}</li>
 *   <li>{@code LIMIT count OFFSET offset}</li>
 *   <li>MySQL {@code LIMIT offset, count}</li>
 *   <li>SQL:2008 {@code [OFFSET offset ROWS] FETCH FIRST|NEXT count ROWS ONLY}</li>
 * </ul>
 *
 * <p>A count or offset in an expression may be a non-negative integer, {@code ?}, {@code :name}, or
 * <code>#{name}</code>. Fully numeric expressions expose their parsed values through {@link #count()} and
 * {@link #offset()}. Expressions containing a placeholder, or an integer outside the {@code int} range,
 * remain <em>unresolved</em>; use {@link #isResolved()}, {@link #resolvedCount()}, and
 * {@link #resolvedOffset()} when the distinction matters.</p>
 *
 * <p>{@link #toSql(NamingPolicy)} returns this object's normalized representation. A query builder may
 * translate a resolved limit to the pagination syntax required by its SQL dialect.</p>
 *
 * <p>All two-argument numeric APIs use {@code (count, offset)} order, including
 * {@link com.landawn.abacus.query.AbstractQueryBuilder#limit(int, int)},
 * {@link Criteria.Builder#limit(int, int)}, and
 * {@link com.landawn.abacus.query.DynamicQuery.Builder#limit(int, int)}.</p>
 *
 * <pre>{@code
 * new Limit(10);                                      // LIMIT 10
 * new Limit(20, 50);                                  // LIMIT 20 OFFSET 50
 * new Limit("OFFSET 50 ROWS FETCH NEXT 20 ROWS ONLY");
 * }</pre>
 *
 * <p><b>API note:</b> Placeholders embedded in an expression are not reported by
 * {@link #parameters()}.</p>
 *
 * @see Clause
 */
public class Limit extends Clause {

    private int count;

    private int offset;

    private String expr;

    private boolean isResolved;

    /**
     * Creates an uninitialized instance for serialization frameworks.
     *
     * <p>Application code should use one of the public constructors.</p>
     */
    Limit() {
    }

    /**
     * Creates {@code LIMIT count} with no offset.
     *
     * <p>A count of zero is valid and renders as {@code LIMIT 0}.</p>
     *
     * @param count maximum number of rows to return; must be non-negative
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public Limit(final int count) {
        this(count, 0);
    }

    /**
     * Creates {@code LIMIT count OFFSET offset}.
     *
     * <p>When {@code offset} is zero, the {@code OFFSET} portion is omitted.</p>
     *
     * @param count maximum number of rows to return; must be non-negative
     * @param offset number of rows to skip; must be non-negative
     * @throws IllegalArgumentException if {@code count} or {@code offset} is negative
     */
    public Limit(final int count, final int offset) {
        // Deliberately NOT SqlExpression.of(...): that factory's cache is unbounded, so interning every
        // distinct (count, offset) pair would grow without limit under dynamic pagination values.
        super(Operator.LIMIT, new SqlExpression(offset == 0 ? String.valueOf(N.checkArgNotNegative(count, "count"))
                : N.checkArgNotNegative(count, "count") + " OFFSET " + N.checkArgNotNegative(offset, "offset")));

        this.count = count;
        this.offset = offset;
        this.isResolved = true;
    }

    /**
     * Creates a row-limiting clause from a validated SQL expression.
     *
     * <p>The accepted forms are listed in the class description. Leading and trailing whitespace is removed,
     * internal whitespace is collapsed, and pagination keywords are converted to upper case. Placeholder names
     * retain their original case. An expression beginning with a number or placeholder is interpreted as a
     * {@code LIMIT} expression, so {@code "10 OFFSET 20"} becomes {@code "LIMIT 10 OFFSET 20"}.</p>
     *
     * <p>If every slot is an {@code int}-range integer, the count and offset are resolved. Otherwise the
     * normalized expression is retained but its numeric values remain unresolved. Floating-point and negative
     * numbers, misspelled keywords, and unrelated SQL are rejected. Named placeholders are restricted to
     * word characters ({@code [A-Za-z0-9_]}); dotted or otherwise exotic parameter names accepted elsewhere
     * (for example, {@code ParsedSql}-style {@code :page.size}) are not valid here.</p>
     *
     * <pre>{@code
     * new Limit("10 OFFSET 20");                         // LIMIT 10 OFFSET 20
     * new Limit("20, 10");                              // LIMIT 20, 10
     * new Limit("OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY");
     * new Limit("? OFFSET ?");                          // unresolved
     * }</pre>
     *
     * <p><b>API note:</b> {@link #toSql(NamingPolicy)} returns the normalized expression. SQL builders may
     * render a resolved expression using the target dialect's pagination syntax. Opaque expressions are
     * generally emitted verbatim; generic {@code LIMIT} expressions may be adapted to an
     * {@code OFFSET}/{@code FETCH} dialect. Note that adapting a placeholder-bearing
     * {@code LIMIT count OFFSET offset} form to an {@code OFFSET}/{@code FETCH} dialect reverses the
     * positional order of its {@code ?} placeholders (the offset placeholder is emitted before the count
     * placeholder); prefer named placeholders when the target dialect may vary.</p>
     *
     * @param expr row-limiting expression; must be non-null, non-blank, and match a supported form
     * @throws IllegalArgumentException if {@code expr} is null, blank, or syntactically unsupported
     */
    public Limit(final String expr) {
        this(prepare(expr));
    }

    private Limit(final Prepared prepared) {
        // Deliberately NOT SqlExpression.of(...): expressions carry dynamic count/offset values, and the
        // factory's unbounded cache would retain every distinct pair for the classloader lifetime.
        super(Operator.LIMIT, new SqlExpression(prepared.conditionExpr));

        this.expr = prepared.literal;
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
     * Returns the normalized expression supplied to {@link #Limit(String)}.
     *
     * <p>The expression is retained even when its count and offset were successfully resolved. Numeric
     * constructors do not create an expression.</p>
     *
     * @return the normalized expression, or {@code null} if this instance was created numerically
     * @see #hasExpression()
     */
    public String expression() {
        return expr;
    }

    /**
     * Tests whether this instance was created from an expression.
     *
     * @return {@code true} if {@link #expression()} is non-null
     * @see #isResolved()
     */
    public boolean hasExpression() {
        return expr != null;
    }

    /**
     * Returns the maximum number of rows to return.
     *
     * <p><b>API note:</b> Prefer {@link #resolvedCount()} when {@link Integer#MAX_VALUE} could be a
     * legitimate count.</p>
     *
     * @return the resolved count, {@link Integer#MAX_VALUE} when an expression is unresolved,
     *         or zero for an uninitialized serialization instance
     */
    public int count() {
        return count;
    }

    /**
     * Returns the number of rows to skip.
     *
     * <p><b>API note:</b> Use {@link #resolvedOffset()} to distinguish a resolved zero from an unresolved
     * expression.</p>
     *
     * @return the resolved offset, or zero when no offset is specified, the expression is unresolved,
     *         or this is an uninitialized serialization instance
     */
    public int offset() {
        return offset;
    }

    /**
     * Tests whether both the count and offset are available as {@code int} values.
     *
     * <p>Numeric constructors and fully numeric expressions are resolved. An expression containing a
     * placeholder or an out-of-range integer is unresolved.</p>
     *
     * @return {@code true} if {@link #resolvedCount()} and {@link #resolvedOffset()} are both present
     */
    public boolean isResolved() {
        return isResolved;
    }

    /**
     * Returns the row count when it can be represented as an {@code int}.
     *
     * @return the resolved count, or an empty optional for an unresolved expression
     */
    public OptionalInt resolvedCount() {
        return isResolved() ? OptionalInt.of(count) : OptionalInt.empty();
    }

    /**
     * Returns the row offset when it can be represented as an {@code int}.
     *
     * @return the resolved offset, or an empty optional for an unresolved expression
     */
    public OptionalInt resolvedOffset() {
        return isResolved() ? OptionalInt.of(offset) : OptionalInt.empty();
    }

    /**
     * Returns an empty parameter list.
     *
     * <p>Placeholders embedded in an expression are raw SQL text and are not tracked by this condition.
     * Code that uses such placeholders is responsible for supplying the corresponding bindings.</p>
     *
     * @return an empty immutable list
     */
    @Override
    public ImmutableList<Object> parameters() {
        return ImmutableList.empty();
    }

    /**
     * Returns the normalized, dialect-independent SQL representation.
     *
     * <ul>
     *   <li>An expression-based instance returns its normalized {@link #expression()}.</li>
     *   <li>A numeric instance returns {@code LIMIT count}, followed by {@code OFFSET offset} when the
     *       offset is greater than zero.</li>
     *   <li>An uninitialized serialization instance returns {@code "null"}.</li>
     * </ul>
     *
     * <p>The naming policy has no effect because a limit contains no identifiers. Dialect-specific pagination
     * conversion is performed by SQL builders, not by this method.</p>
     *
     * @param namingPolicy naming policy; ignored and may be {@code null}
     * @return normalized SQL for this limit, or {@code "null"} for an uninitialized instance
     */
    @Override
    public String toSql(final NamingPolicy namingPolicy) {
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
     * Returns a hash code based on the operator and either the normalized expression or the numeric count and offset.
     *
     * @return this limit's hash code
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((operator() == null) ? 0 : operator().hashCode());

        if (Strings.isNotEmpty(expr)) {
            return (h * 31) + expr.hashCode();
        } else {
            h = (h * 31) + count;
            return (h * 31) + offset;
        }
    }

    /**
     * Compares limits by representation and value.
     *
     * <p>Expression-based limits compare their normalized expressions; numeric limits compare count and offset.
     * The two representations are intentionally not equal even when they produce identical SQL text.</p>
     *
     * @param obj object to compare
     * @return {@code true} if {@code obj} is a {@code Limit} with the same operator and representation
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

        if (Strings.isNotEmpty(expr)) {
            return Strings.isNotEmpty(other.expr) && expr.equals(other.expr);
        } else {
            return Strings.isEmpty(other.expr) && (count == other.count) && (offset == other.offset);
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

    /** A run of one or more whitespace characters, collapsed to a single space during normalization. */
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

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

        final String collapsed = WHITESPACE_RUN.matcher(segment).replaceAll(" ");
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
