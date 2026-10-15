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

import static com.landawn.abacus.util.SK.COMMA_SPACE;
import static com.landawn.abacus.util.SK._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.ParsedSql;
import com.landawn.abacus.query.QueryUtil;
import com.landawn.abacus.query.QueryUtil.ColumnInfo;
import com.landawn.abacus.query.SqlBuilder;
import com.landawn.abacus.query.SqlParser;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Represents a complete or structured SELECT used within SQL conditions.
 *
 * <p>This class supports three types of subqueries:</p>
 * <ul>
 *   <li><b>Raw subqueries</b> - complete text accepted without SQL syntax validation, optionally
 *       carrying an immutable positional-binding list</li>
 *   <li><b>Structured subqueries</b> - generated from entity names/classes, property names, and conditions</li>
 *   <li><b>Builder-backed snapshots</b> - complete, validated SELECT statements captured through
 *       {@link SqlBuilder#toSubQuery()}, retaining parameters and placeholder metadata for safe
 *       composition into another builder</li>
 * </ul>
 *
 * <p>Subqueries can be used in various contexts:</p>
 * <ul>
 *   <li>Scalar values in comparisons, BETWEEN bounds, and individual IN/NOT IN list values or tuple elements</li>
 *   <li>IN/NOT IN conditions for set membership tests (see {@link InSubQuery}, {@link NotInSubQuery})</li>
 *   <li>EXISTS/NOT EXISTS for existence checks (see {@link Exists}, {@link NotExists})</li>
 *   <li>ANY/ALL/SOME for quantified comparisons (see {@link Any}, {@link All}, {@link Some})</li>
 * </ul>
 *
 * <p>A scalar subquery must select one column. Scalar operand constructors reject a structured
 * subquery whose known, non-wildcard projection has a different width. Raw SQL, builder-backed
 * snapshots, and wildcard projections do not expose a known column count for this validation, so
 * their projection width is left to the database. This check does not determine how many rows a
 * subquery returns.</p>
 *
 * <p>A structured subquery treats a blank expression filter as absent. A nonblank expression
 * containing only SQL comments is rejected, because rendering it would leave a WHERE clause
 * without a predicate.</p>
 *
 * <p>Use {@link Filters#in(Collection, SubQuery)} or {@link Filters#notIn(Collection, SubQuery)} to
 * compare multiple properties with a matching multi-column projection. These tuple membership APIs
 * require one selected column per compared property. When only row existence matters, use
 * {@link Exists} or {@link NotExists}; their subqueries may select multiple columns.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Raw SQL subquery
 * SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users WHERE status = 'active'");
 *
 * // Structured subquery with entity name
 * Condition activeCondition = Filters.equal("status", "active");
 * SubQuery subQuery2 = Filters.subQuery("users", Arrays.asList("id"), activeCondition);
 * // SQL: SELECT id FROM users WHERE status = 'active'
 *
 * // Structured subquery with entity class
 * SubQuery subQuery3 = Filters.subQuery(User.class, Arrays.asList("id", "name"),
 *                                   Filters.greaterThan("age", 18));
 * // SQL: SELECT id, name FROM <User-table-name> WHERE age > 18
 * // (the FROM target is resolved from the entity class via QueryUtil — for a class without
 * //  table-mapping annotations it falls back to the class's simple name; column-name mapping
 * //  for the selected properties is also applied when available)
 *
 * // Builder-backed snapshot with retained parameters
 * SubQuery subQuery4 = PSC.select("userId")
 *    .from("orders")
 *    .where(Filters.greaterThan("total", 100))
 *    .toSubQuery();
 * // SQL: SELECT user_id AS "userId" FROM orders WHERE total > ?
 * // parameters: [100]
 *
 * // Use in IN condition
 * InSubQuery inCondition = Filters.in("userId", subQuery1);
 * // SQL: userId IN (SELECT id FROM users WHERE status = 'active')
 * }</pre>
 *
 * @see InSubQuery
 * @see NotInSubQuery
 * @see Exists
 * @see NotExists
 * @see Any
 * @see All
 * @see Some
 */
public class SubQuery extends AbstractCondition {

    // For Kryo
    /** The entity/table name of a structured subquery, or the optional name associated with raw SQL;
     *  empty for unnamed raw SQL and builder-backed snapshots. */
    final String entityName;

    // For Kryo
    /** The entity class of a class-based structured subquery; {@code null} otherwise. */
    final Class<?> entityClass;

    private List<String> propNames;

    // For Kryo
    /** The complete query text of a raw SQL or builder-backed subquery; {@code null} for structured subqueries. */
    final String sql;

    /** Bindings captured for raw SQL (array/{@code Date}/{@code Calendar} bindings are snapshotted at
     *  construction); empty for structured subqueries and for builder-backed snapshots, whose subclass
     *  retains the source builder's parameters itself. */
    private final ImmutableList<Object> rawParameters;

    /** {@code true} when {@link #rawParameters} holds an array/{@code Date}/{@code Calendar} binding, so
     *  {@link #parameters()} must hand out fresh defensive copies on every call. Evaluated once at construction. */
    private final boolean rebuildParametersPerCall;

    /** The trailing condition or clause for a structured subquery; a predicate is normalized to a
     *  {@link Where}. {@code null} for raw SQL, builder-backed snapshots, or structured subqueries without clauses. */
    private Condition condition;

    /** Lazily memoized unmodifiable view of {@link #propNames} (performance only). */
    private transient ImmutableList<String> cachedPropNamesView;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized SubQuery instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    SubQuery() {
        entityName = null;
        entityClass = null;
        sql = null;
        rawParameters = ImmutableList.empty();
        rebuildParametersPerCall = false;
    }

    /**
     * Creates a subquery with raw SQL.
     * This provides maximum flexibility for complex subqueries that cannot be easily
     * expressed using the structured approach.
     *
     * <p>Use this constructor when:</p>
     * <ul>
     *   <li>The subquery uses database-specific features</li>
     *   <li>Complex joins or aggregations are needed</li>
     *   <li>Performance-tuned SQL is required</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple subquery
     * SubQuery subQuery = Filters.subQuery("SELECT MAX(salary) FROM employees");
     *
     * // Complex subquery with joins
     * SubQuery complexQuery = Filters.subQuery(
     *     "SELECT u.id FROM users u " +
     *     "INNER JOIN orders o ON u.id = o.user_id " +
     *     "WHERE o.total > 1000 " +
     *     "GROUP BY u.id HAVING COUNT(o.id) > 5"
     * );
     * }</pre>
     *
     * <p><b>&#9888;&#65039;</b> The entire argument is treated as complete raw query-expression text and is not
     * syntax-validated as a SELECT statement; it is scanned to validate placeholders. It is not an
     * entity/table name (contrast with
     * {@link #SubQuery(String, java.util.Collection, Condition)},
     * whose first argument is an entity name). {@code new SubQuery("orders")} would use the text
     * {@code orders} as the whole subquery. Prefer the
     * {@link com.landawn.abacus.query.Filters#subQuery(String) Filters.subQuery} factories,
     * whose overloads make the distinction explicit.</p>
     *
     * <p>This overload is only for SQL without parameter placeholders. Use
     * {@link #SubQuery(String, Collection)} when the text contains JDBC {@code ?} placeholders.</p>
     *
     * @param sql complete raw query-expression text (must not be {@code null}, empty, blank, or contain a parameter placeholder)
     * @throws IllegalArgumentException if {@code sql} is {@code null}, empty, or blank; contains malformed placeholder text
     *         such as an unclosed {@code #{...}} marker; contains a named ({@code :name}) or MyBatis {@code #{...}} placeholder;
     *         or contains a positional placeholder without a corresponding binding (including a {@code ?} outside quoted text and
     *         comments in text that does not start with a recognized query keyword)
     */
    public SubQuery(final String sql) {
        this(Strings.EMPTY, sql, Collections.emptyList(), true);
    }

    /**
     * Creates a raw subquery with positional JDBC bindings. The SQL is retained verbatim and the binding
     * list is copied in placeholder encounter order, so later changes to the supplied collection have no
     * effect. The individual binding values are snapshotted the same way condition values are: arrays are
     * deep-copied and {@code Date}/{@code Calendar} values cloned at construction, while every other binding
     * is kept by reference. A later mutation of the caller's array or date therefore never changes this
     * subquery: {@link #parameters()} exposes defensive copies, and {@link #equals(Object)}/{@link #hashCode()}
     * compare array bindings by content via {@link N#deepEquals(Object, Object)}/{@link N#deepHashCode(Object)}
     * and every other binding through its own {@code equals}/{@code hashCode}. Bindings retained by reference
     * must not be mutated while the subquery is in use, particularly when it is stored in a {@code HashSet}. Markers inside
     * quoted text and comments, and PostgreSQL JSON {@code ?} operators, are not counted as placeholders.
     *
     * <p>Named ({@code :name}) and MyBatis ({@code #{name}}) markers are deliberately rejected because
     * a raw fragment has no generated-name metadata for collision-safe composition. Use a builder-backed
     * subquery when named-placeholder policies are required.</p>
     *
     * <p>A binding that is itself SQL is written into the text at construction instead of being bound, so the
     * subquery renders the same SQL under every builder policy (exactly as a {@link SqlExpression} value of a
     * structured condition is rendered): a {@link SqlExpression} replaces its {@code ?} verbatim, and a raw
     * {@code SubQuery} replaces it as {@code (sql)} with its own bindings taking its place in the binding list.
     * {@link #rawSql()} and {@link #parameters()} report the result.</p>
     * <pre>{@code
     * SubQuery sq = new SubQuery("SELECT id FROM orders WHERE created < ? AND total > ?",
     *     Arrays.asList(SqlExpression.of("CURRENT_DATE"), 100));
     * // rawSql():     SELECT id FROM orders WHERE created < CURRENT_DATE AND total > ?
     * // parameters(): [100]
     * }</pre>
     *
     * @param sql complete raw query-expression text (must not be {@code null}, empty, or blank)
     * @param parameters positional binding values in placeholder encounter order (must not be {@code null});
     *                   individual binding values may be {@code null}; array/{@code Date}/{@code Calendar}
     *                   bindings are snapshotted, all others kept by reference; {@link SqlExpression} and raw
     *                   {@code SubQuery} bindings are inlined as described above
     * @throws IllegalArgumentException if {@code sql} is {@code null}, empty, or blank; {@code parameters}
     *         is {@code null}; the SQL contains malformed placeholder text such as an unclosed {@code #{...}} marker;
     *         the SQL contains a named ({@code :name}) or MyBatis {@code #{...}} placeholder; the number of positional
     *         placeholders differs from the number of bindings; the SQL does not start with a recognized query keyword
     *         ({@code SELECT}, {@code WITH}, {@code VALUES}, {@code TABLE}, ...) but contains a {@code ?} outside quoted text and comments; an object-array binding contains a cycle; a binding is a
     *         {@link SqlExpression} that is blank, comment-only, or contains parameter markers of its own (such as
     *         {@link Filters#QME}); or a binding is any other {@link Condition} (a predicate, or a structured or
     *         builder-backed subquery), whose SQL would depend on the enclosing builder's policy
     */
    public SubQuery(final String sql, final Collection<?> parameters) {
        this(Strings.EMPTY, sql, parameters, true);
    }

    /**
     * Creates a subquery with an entity name and raw SQL.
     * The entity name is for reference only when using raw SQL and doesn't affect the rendered SQL.
     *
     * <p>This constructor allows associating a logical entity name with a raw SQL subquery,
     * which can be useful for documentation or framework integration purposes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("orders",
     *     "SELECT order_id FROM orders WHERE total > 1000");
     * // SQL: SELECT order_id FROM orders WHERE total > 1000
     * }</pre>
     *
     * @param entityName the entity/table name; may be {@code null} or empty, in which case it is
     *            stored as the empty string
     * @param sql complete raw query-expression text (must not be {@code null}, empty, or blank)
     * @throws IllegalArgumentException if {@code sql} is {@code null}, empty, or blank; contains malformed placeholder text
     *         such as an unclosed {@code #{...}} marker; contains a named ({@code :name}) or MyBatis {@code #{...}} placeholder;
     *         or contains a positional placeholder without a corresponding binding
     * @deprecated the entity name is unused when rendering raw query-expression text; it is only exposed by
     *             {@link #entityName()} and participates in {@link #equals(Object)}/{@link #hashCode()}. Use
     *             {@link #SubQuery(String)} for raw text, or
     *             {@link #SubQuery(String, java.util.Collection, Condition)} when an
     *             entity-name-based structured subquery is actually wanted.
     */
    @Deprecated
    public SubQuery(final String entityName, final String sql) {
        this(entityName, sql, Collections.emptyList(), true);
    }

    /**
     * Constructor for builder-backed subclasses whose builders have already validated SQL and own
     * placeholder-policy metadata that cannot be reconstructed from raw text alone.
     *
     * @param sql complete builder-rendered query text
     * @param builderBackedSnapshot must be {@code true}; distinguishes this trusted path from public raw SQL construction
     * @throws IllegalArgumentException if {@code sql} is {@code null}, empty, or blank, or if
     *         {@code builderBackedSnapshot} is {@code false}
     */
    protected SubQuery(final String sql, final boolean builderBackedSnapshot) {
        this(Strings.EMPTY, sql, Collections.emptyList(), false);

        if (!builderBackedSnapshot) {
            throw new IllegalArgumentException("builderBackedSnapshot must be true");
        }
    }

    /**
     * Creates a validated raw-SQL snapshot.
     *
     * @param entityName the optional entity name retained for compatibility
     * @param sql the raw SQL text
     * @param parameters the positional bindings
     * @param validateBindings whether placeholders must match the bindings
     * @throws IllegalArgumentException if {@code sql} is null, empty, or blank; {@code parameters} is null;
     *         binding validation is enabled and the SQL has malformed, named, or mismatched placeholders;
     *         or a binding is a cyclic object array
     */
    private SubQuery(final String entityName, final String sql, final Collection<?> parameters, final boolean validateBindings) {
        super(Operator.EMPTY);
        if (Strings.isBlank(sql)) {
            throw new IllegalArgumentException("SQL statement must not be null, empty, or blank");
        }

        List<Object> bindings = copyRawParameters(parameters);
        String rawText = sql;

        if (validateBindings) {
            validateRawBindings(sql, bindings);

            if (containsConditionBinding(bindings)) {
                final List<Object> valueBindings = new ArrayList<>(bindings.size());
                rawText = inlineConditionBindings(sql, bindings, valueBindings);
                bindings = valueBindings;

                // Defensive: the inlined text is re-scanned so the remaining bindings provably line up with the
                // placeholders that are left (a spliced expression cannot add or hide a '?' unnoticed).
                try {
                    validateRawBindings(rawText, bindings);
                } catch (final IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Inlining the SqlExpression/SubQuery bindings of a raw subquery left mismatched placeholders (" + e.getMessage() + "): " + rawText,
                            e);
                }
            }
        }

        final ImmutableList<Object> parameterSnapshot = snapshotRawParameters(bindings);

        this.entityName = entityName == null ? Strings.EMPTY : entityName;
        entityClass = null;
        propNames = null;
        condition = null;
        this.sql = rawText;
        rawParameters = parameterSnapshot;
        rebuildParametersPerCall = containsSnapshotMutableValue(rawParameters);
    }

    /**
     * Creates a structured single-property subquery for an entity name.
     *
     * @param entityName the entity/table name (must not be {@code null}, empty, or blank); a bare, optionally
     *            schema-qualified name. An inline alias such as {@code users u} is not recognized by
     *            {@link #toSql(NamingPolicy)}, which converts the whole text as one identifier; build aliased
     *            sub-queries with a query builder instead
     * @param propName the property to select (must not be {@code null}, empty, or blank)
     * @param condition an optional trailing condition, clause, or {@link Criteria}. A predicate is wrapped in
     *             {@link Where}; {@code null}, a blank expression, or an empty {@code Criteria} adds no
     *             clause. An empty {@link Junction} is preserved as its true/false Boolean identity.
     * @throws IllegalArgumentException if {@code entityName} or {@code propName} is {@code null}, empty, or blank,
     *         if {@code condition} uses an {@link Operator#ON ON}/{@link Operator#USING USING} operator, is not a
     *         complete predicate (for example a standalone {@link SubQuery} or quantified operand), or is a
     *         {@link Criteria} that carries a SELECT modifier (e.g. {@code DISTINCT}) — none of which are valid here
     */
    public SubQuery(final String entityName, final String propName, final Condition condition) {
        this(entityName, java.util.Collections.singletonList(propName), condition);
    }

    /**
     * Creates a structured subquery with entity name, selected properties, and condition.
     * This approach provides automatic SQL generation and name mapping.
     *
     * <p>The generated SQL follows the pattern {@code SELECT [properties] FROM [entity] [condition-or-clauses]}.
     * An ordinary predicate is automatically wrapped in {@link Where}; a {@link Criteria} or SQL clause is
     * retained so it can contribute its own ordered clauses.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Select specific columns with conditions
     * List<String> props = Arrays.asList("id", "email");
     * Condition condition = Filters.and(
     *     Filters.equal("active", true),
     *     Filters.greaterThan("created", "2024-01-01")
     * );
     * SubQuery subQuery = Filters.subQuery("users", props, condition);
     * // SQL: SELECT id, email FROM users WHERE ((active = true) AND (created > '2024-01-01'))
     * }</pre>
     *
     * @param entityName the entity/table name (must not be {@code null}, empty, or blank); a bare, optionally
     *            schema-qualified name. An inline alias such as {@code users u} is not recognized by
     *            {@link #toSql(NamingPolicy)}, which converts the whole text as one identifier; build aliased
     *            sub-queries with a query builder instead
     * @param propNames collection of property names to select (must not be {@code null} or empty and must not contain {@code null}, empty, or blank names);
     *            an item may carry an inline alias ({@code firstName AS fn}) or be an expression ({@code COUNT(*) AS cnt}),
     *            in which case {@link #toSql(NamingPolicy)} converts only its identifiers
     * @param condition an optional trailing condition, clause, or {@link Criteria}. A predicate is wrapped in
     *             {@link Where}; {@code null}, a blank expression, or an empty {@code Criteria} adds no
     *             clause. An empty {@link Junction} is preserved as its true/false Boolean identity.
     * @throws IllegalArgumentException if {@code entityName} is {@code null}, empty, or blank, if {@code propNames} is
     *             {@code null} or empty, if {@code propNames} contains {@code null}, empty, or blank names, if {@code condition}
     *             uses an {@link Operator#ON ON}/{@link Operator#USING USING} operator, is not a complete predicate
     *             (for example a standalone {@link SubQuery} or quantified operand), or is a {@link Criteria} that
     *             carries a SELECT modifier (e.g. {@code DISTINCT}) — none of which are valid here
     */
    public SubQuery(final String entityName, final Collection<String> propNames, final Condition condition) {
        super(Operator.EMPTY);

        if (Strings.isBlank(entityName)) {
            throw new IllegalArgumentException("Entity name must not be null, empty, or blank");
        }

        final List<String> validatedPropNames = copyAndValidatePropNames(propNames);
        final Condition validatedCondition = normalizeCondition(condition);

        this.entityName = entityName;
        this.entityClass = null;
        this.propNames = validatedPropNames;
        this.condition = validatedCondition;

        sql = null;
        rawParameters = ImmutableList.empty();
        rebuildParametersPerCall = false;
    }

    /**
     * Creates a structured single-property subquery for an entity class.
     *
     * @param entityClass the entity class (must not be {@code null})
     * @param propName the property to select (must not be {@code null}, empty, or blank)
     * @param condition an optional trailing condition, clause, or {@link Criteria}. A predicate is wrapped in
     *             {@link Where}; {@code null}, a blank expression, or an empty {@code Criteria} adds no
     *             clause. An empty {@link Junction} is preserved as its true/false Boolean identity.
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, if {@code propName} is
     *         {@code null}, empty, or blank, if {@code condition} uses an
     *         {@link Operator#ON ON}/{@link Operator#USING USING} operator, is not a complete predicate
     *         (for example a standalone {@link SubQuery} or quantified operand), or is a {@link Criteria} that
     *         carries a SELECT modifier (e.g. {@code DISTINCT}) — none of which are valid here
     */
    public SubQuery(final Class<?> entityClass, final String propName, final Condition condition) {
        this(entityClass, java.util.Collections.singletonList(propName), condition);
    }

    /**
     * Creates a structured subquery with entity class, selected properties, and condition.
     * The entity name is derived from the class's simple name.
     *
     * <p>Using an entity class enables automatic table and column-name mapping. Property names remain
     * strings and are not verified against the class by this constructor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Class-based subquery construction
     * SubQuery subQuery = Filters.subQuery(Product.class,
     *     Arrays.asList("id", "categoryId"),
     *     Filters.like("name", "%electronics%")
     * );
     * // SQL: SELECT id, categoryId FROM <Product-table-name> WHERE name LIKE '%electronics%'
     * // (the FROM target is resolved from the entity class via QueryUtil — for a class without
     * //  table-mapping annotations it falls back to the class's simple name "Product"; column-name
     * //  mapping for the selected properties is also applied when available)
     *
     * // With complex conditions
     * SubQuery activeProducts = Filters.subQuery(Product.class,
     *     Arrays.asList("id", "name", "price"),
     *     Filters.and(
     *         Filters.equal("active", true),
     *         Filters.between("price", 10, 100)
     *     )
     * );
     * }</pre>
     *
     * @param entityClass the entity class (must not be {@code null})
     * @param propNames collection of property names to select (must not be {@code null} or empty and must not contain {@code null}, empty, or blank names);
     *            an item may carry an inline alias ({@code firstName AS fn}) or be an expression ({@code COUNT(*) AS cnt}),
     *            in which case {@link #toSql(NamingPolicy)} converts only its identifiers
     * @param condition an optional trailing condition, clause, or {@link Criteria}. A predicate is wrapped in
     *             {@link Where}; {@code null}, a blank expression, or an empty {@code Criteria} adds no
     *             clause. An empty {@link Junction} is preserved as its true/false Boolean identity.
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, if {@code propNames} is {@code null}
     *             or empty, if {@code propNames} contains {@code null}, empty, or blank names, if {@code condition} uses an
     *             {@link Operator#ON ON}/{@link Operator#USING USING} operator, is not a complete predicate
     *             (for example a standalone {@link SubQuery} or quantified operand), or is a {@link Criteria} that
     *             carries a SELECT modifier (e.g. {@code DISTINCT}) — none of which are valid here
     */
    public SubQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition) {
        super(Operator.EMPTY);

        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class must not be null");
        }

        final List<String> validatedPropNames = copyAndValidatePropNames(propNames);
        final Condition validatedCondition = normalizeCondition(condition);

        this.entityName = ClassUtil.getSimpleClassName(entityClass);
        this.entityClass = entityClass;
        this.propNames = validatedPropNames;
        this.condition = validatedCondition;

        sql = null;
        rawParameters = ImmutableList.empty();
        rebuildParametersPerCall = false;
    }

    /**
     * Returns the stored complete query text if this is a raw SQL subquery or builder-backed snapshot.
     * For structured subqueries created with entity name/class and conditions, this returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Raw SQL subquery
     * SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE status = 'active'");
     * String sql = rawQuery.rawSql();
     * // Returns: "SELECT id FROM users WHERE status = 'active'"
     *
     * // Structured subquery returns null for rawSql()
     * SubQuery structured = new SubQuery("users", Arrays.asList("id"), Filters.equal("status", "active"));
     * String structuredSql = structured.rawSql();
     * // Returns: null
     *
     * // Builder-backed snapshot returns the SQL rendered by its source builder
     * SubQuery built = PSC.select("id").from("users").toSubQuery();
     * String builtSql = built.rawSql();
     * // Returns: "SELECT id FROM users"
     * }</pre>
     *
     * @return the stored complete query text (for {@link #SubQuery(String, Collection)}, with any
     *         {@link SqlExpression}/raw {@code SubQuery} bindings already written in), or {@code null} if this is a
     *         structured subquery
     */
    public String rawSql() {
        return sql;
    }

    /**
     * Returns the entity/table name for this subquery.
     * This is available for both structured subqueries and raw SQL subqueries that were
     * created with an entity name parameter. For raw SQL subqueries created without
     * an entity name and for builder-backed snapshots, this is the empty string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Structured subquery with entity name
     * SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), Filters.equal("active", true));
     * String entityName = subQuery.entityName();
     * // Returns: "users"
     *
     * // Raw SQL subquery with entity name
     * SubQuery rawQuery = new SubQuery("orders", "SELECT order_id FROM orders WHERE total > 1000");
     * String name = rawQuery.entityName();
     * // Returns: "orders"
     *
     * // Raw SQL subquery without entity name
     * SubQuery simpleRaw = new SubQuery("SELECT id FROM users");
     * String emptyName = simpleRaw.entityName();
     * // Returns: "" (empty string)
     * }</pre>
     *
     * @return the entity/table name; an empty string for an initialized unnamed raw or builder-backed
     *         subquery, or {@code null} only for an uninitialized serialization-framework instance
     */
    public String entityName() {
        return entityName;
    }

    /**
     * Returns the entity class if this subquery was created with a class reference.
     * This provides type information for subqueries constructed using the class-based constructor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Subquery created with entity class
     * SubQuery subQuery = new SubQuery(Product.class, Arrays.asList("id", "name"), Filters.equal("active", true));
     * Class<?> entityClass = subQuery.entityClass();
     * // Returns: Product.class
     *
     * // Subquery created with entity name string returns null
     * SubQuery namedQuery = new SubQuery("products", Arrays.asList("id"), Filters.equal("active", true));
     * Class<?> clazz = namedQuery.entityClass();
     * // Returns: null
     * }</pre>
     *
     * @return the entity class, or {@code null} if created with an entity name string, raw SQL, or a builder snapshot
     */
    public Class<?> entityClass() {
        return entityClass;
    }

    /**
     * Returns the collection of property names to select in this subquery.
     * These are the columns that will appear in the SELECT clause of the generated SQL.
     * For raw SQL subqueries and builder-backed snapshots, this returns {@code null} because their
     * complete SELECT lists are already represented by the stored SQL text.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Structured subquery with selected properties
     * SubQuery subQuery = new SubQuery("users", Arrays.asList("id", "email", "name"), Filters.equal("active", true));
     * Collection<String> propNames = subQuery.selectPropNames();
     * // Returns: ["id", "email", "name"]
     *
     * // Raw SQL subquery returns null
     * SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE active = true");
     * Collection<String> rawProps = rawQuery.selectPropNames();
     * // Returns: null
     * }</pre>
     *
     * @return immutable list of property names to select, or {@code null} for raw SQL and builder-backed subqueries
     */
    public ImmutableList<String> selectPropNames() {
        if (propNames == null) {
            return null;
        }

        ImmutableList<String> view = cachedPropNamesView;

        if (view == null) {
            view = ImmutableList.wrap(propNames);
            cachedPropNamesView = view;
        }

        return view;
    }

    /**
     * Snapshots a structured projection.
     *
     * @param propNames the projection names to copy
     * @return the validated projection snapshot
     * @throws IllegalArgumentException if {@code propNames} is null, empty, yields no elements, or
     *         contains a null, empty, or blank name
     */
    private static List<String> copyAndValidatePropNames(final Collection<String> propNames) {
        if (propNames == null) {
            throw new IllegalArgumentException("Property names must not be null");
        }

        if (propNames.isEmpty()) {
            throw new IllegalArgumentException("Property names must not be empty");
        }

        final List<String> result = new ArrayList<>(propNames.size());

        for (final String propName : propNames) {
            if (Strings.isBlank(propName)) {
                throw new IllegalArgumentException("Property name in propNames must not be null, empty, or blank");
            }

            result.add(propName);
        }

        // The source may be a live/custom collection whose contents change between isEmpty() and
        // iteration. Do not silently turn an explicitly projected subquery into SELECT *.
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Property names must not be empty (the supplied collection yielded no elements while being copied)");
        }

        return result;
    }

    /**
     * Renders one structured select item for {@link #toSql(NamingPolicy)} the way a query builder renders the
     * same item, minus the builder's table-alias prefix. The item is first split into the projected expression
     * and an optional alias, which is re-appended as written: {@code expr AS alias}, or {@code name alias} when
     * the item is exactly a name (bare, delimited or dot-qualified, see {@link #leadingNameEnd(String)}),
     * whitespace and a bare alias, and the name is not a SQL keyword in any case (a builder converts such an
     * implicit alias like an identifier). The expression then takes one of three paths:
     * <ul>
     * <li>a bare name ({@link #isBareName(String)}) is resolved by {@link #renderName};</li>
     * <li>an expression with a comment token outside quoted regions ({@link #containsCommentToken(String)})
     * goes through {@link QueryUtil#convertIdentifier(String, NamingPolicy)} as one identifier, which keeps the
     * comment intact only under {@code NO_CHANGE} (a builder rejects such an item); a trailing line comment is
     * ended with a newline before the alias;</li>
     * <li>anything else (a function call, an operator expression, a string literal, a parameter marker, a
     * delimited identifier) is rendered by {@link #renderExpression}.</li>
     * </ul>
     * A PostgreSQL dollar-quoted string ({@code $$...$$}, {@code $tag$...$tag$}) is quoted text throughout: an
     * {@code AS}, whitespace or comment opener inside it neither splits off an alias nor selects the comment path,
     * and {@link #renderExpression} copies it as written.
     *
     * @param item the select item as supplied to the constructor (non-blank)
     * @param entityClass the entity class of an entity-class subquery, or {@code null}
     * @param columnInfoMap the entity column mapping keyed by property and mapped column names
     *        ({@link QueryUtil#propToColumnInfoMap(Class, NamingPolicy)}), or {@code null} for an entity-name subquery
     * @param namingPolicy the effective naming policy (never {@code null})
     * @return the rendered select item
     */
    private static String renderSelectItem(final String item, final Class<?> entityClass, final Map<String, ColumnInfo> columnInfoMap,
            final NamingPolicy namingPolicy) {
        // Fast path for the common plain property name: one character scan before the lookup or conversion.
        if (isBareName(item)) {
            return renderName(item, entityClass, columnInfoMap, namingPolicy);
        }

        String expression;
        String alias = null;
        String separator = null;

        final int aliasKeywordIndex = indexOfTopLevelAliasKeyword(item);

        if (aliasKeywordIndex >= 0) {
            expression = item.substring(0, aliasKeywordIndex).strip();
            alias = item.substring(aliasKeywordIndex + 2).strip();
            separator = SK.SPACE + SK.AS + SK.SPACE;
        } else {
            expression = item.strip();
            final int headEnd = leadingNameEnd(expression);

            if (headEnd > 0) {
                final String head = expression.substring(0, headEnd);
                final String tail = expression.substring(headEnd).strip();

                // A keyword in any case ("DISTINCT userId", "distinct userId", "all userId") modifies the projected
                // expression; it is never the expression of an implicit alias.
                if (isBareName(tail) && !SqlExpression.isSqlKeyword(head) && !SqlExpression.isSqlKeyword(head.toUpperCase(Locale.ROOT))) {
                    expression = head;
                    alias = tail;
                    separator = SK.SPACE;
                }
            }
        }

        final String rendered;

        if (isBareName(expression)) {
            rendered = renderName(expression, entityClass, columnInfoMap, namingPolicy);
        } else if (containsCommentToken(expression)) {
            // The tokenizer path would strip the comment. Stripping the expression also dropped the line break that
            // ended a line comment before the alias ("firstName -- c\nAS x"), so restore one before " AS x".
            rendered = QueryUtil.terminateLineComment(QueryUtil.convertIdentifier(expression, namingPolicy));
        } else {
            rendered = renderExpression(expression, entityClass, columnInfoMap, namingPolicy);
        }

        return alias == null ? rendered : rendered + separator + alias;
    }

    /**
     * Renders a bare name: an entity property or mapped column name resolves through the column mapping (see
     * {@link #mappedColumn}); otherwise a registered SQL keyword ({@code CURRENT_TIMESTAMP}) or a token that does
     * not start with an ASCII letter or {@code _} (the number {@code 2faCode} or {@code .5E3}, the dollar-quoted
     * string {@code $$text$$}, the name {@code $col} or a non-ASCII-leading name) is kept as written, as
     * {@link SqlExpression#toSql(NamingPolicy)} and the query builders keep it, and any other name goes through
     * {@link QueryUtil#convertIdentifier(String, NamingPolicy)}.
     */
    private static String renderName(final String name, final Class<?> entityClass, final Map<String, ColumnInfo> columnInfoMap,
            final NamingPolicy namingPolicy) {
        if (columnInfoMap != null) {
            final String columnName = mappedColumn(name, entityClass, columnInfoMap);

            if (columnName != null) {
                return columnName;
            }
        }

        final char first = name.charAt(0);

        // The builders and SqlExpression convert only a token that starts like an ASCII identifier (isRawIdentifierStart).
        return !(Strings.isAsciiAlpha(first) || first == '_') || SqlExpression.isSqlKeyword(name) ? name : QueryUtil.convertIdentifier(name, namingPolicy);
    }

    /**
     * Renders an expression with the token loop of {@link SqlExpression#toSql(NamingPolicy)}
     * ({@link QueryUtil#appendRawExpression}): string literals, parameter markers, delimited identifiers, function
     * names and recognised (upper-case) SQL keywords are kept, and every other identifier is converted on its own.
     * For an entity-class subquery an identifier that names a property or a mapped column resolves through the column
     * mapping first, as in a query builder ({@code max(emailAddress)} renders as {@code max(EMAIL)} for
     * {@code @Column("EMAIL")}).
     */
    private static String renderExpression(final String expression, final Class<?> entityClass, final Map<String, ColumnInfo> columnInfoMap,
            final NamingPolicy namingPolicy) {
        final UnaryOperator<String> nameConverter = word -> SqlExpression.isSqlKeyword(word) ? word : QueryUtil.convertIdentifier(word, namingPolicy);
        final UnaryOperator<String> columnConverter = columnInfoMap == null ? nameConverter : word -> {
            final String columnName = mappedColumn(word, entityClass, columnInfoMap);

            return columnName == null ? nameConverter.apply(word) : columnName;
        };

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            // A name after a delimited qualifier ("T".firstName) takes the naming policy only, as in the builders.
            QueryUtil.appendRawExpression(sb, expression, SqlParser.tokenize(expression), SqlParser.tokenizer(), false, columnConverter, nameConverter);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Returns the mapped column of an entity property, or {@code null}. Like the query builders, the lookup accepts
     * a property name ({@code emailAddress}) and a mapped column name ({@code EMAIL} for {@code @Column("EMAIL")}).
     * A key qualified with the entity's own table alias ({@code p.emailAddress} for {@code @Table(alias = "p")})
     * resolves too and keeps the qualifier, unless the mapped column is not a single unqualified identifier
     * ({@link ColumnInfo#isUnqualified()}), as in the query builders.
     */
    private static String mappedColumn(final String word, final Class<?> entityClass, final Map<String, ColumnInfo> columnInfoMap) {
        final ColumnInfo columnInfo = columnInfoMap.get(word);

        if (columnInfo != null) {
            return columnInfo.columnName();
        }

        if (entityClass == null || word.indexOf('.') < 0) {
            return null;
        }

        final String tableAlias = QueryUtil.tableAlias(entityClass);

        if (Strings.isEmpty(tableAlias) || word.length() <= tableAlias.length() + 1 || word.charAt(tableAlias.length()) != '.'
                || !word.startsWith(tableAlias)) {
            return null;
        }

        final ColumnInfo aliasedColumnInfo = columnInfoMap.get(word.substring(tableAlias.length() + 1));

        if (aliasedColumnInfo == null) {
            return null;
        }

        return aliasedColumnInfo.isUnqualified() ? tableAlias + '.' + aliasedColumnInfo.columnName() : aliasedColumnInfo.columnName();
    }

    /**
     * Finds the start of a top-level {@code AS} alias keyword in a select item: an {@code AS} (any case)
     * surrounded by whitespace, outside parentheses, quoted strings/identifiers (dollar-quoted strings included) and
     * comments, and followed by a non-blank alias. The first such keyword is taken, as the query builders do.
     *
     * @param item the select item to scan
     * @return the index of the {@code AS} keyword, or {@code -1} if the item carries no explicit alias
     */
    private static int indexOfTopLevelAliasKeyword(final String item) {
        final int len = item.length();
        int depth = 0;

        for (int i = 0; i < len; i++) {
            final char ch = item.charAt(i);

            if (ch == '\'' || ch == '"' || ch == '`') {
                i = closingQuoteIndex(item, i, ch);
            } else if (ch == '$') {
                // The "as" of $$foo as bar$$ is text. A dollar quote that is never closed holds the rest of the item.
                final int end = QueryUtil.dollarQuoteEnd(item, i);

                if (end != 0) {
                    i = end < 0 ? len : end - 1;
                }
            } else if (ch == '[') {
                final int close = item.indexOf(']', i + 1);
                i = close < 0 ? len : close;
            } else if (ch == '-' && i + 1 < len && item.charAt(i + 1) == '-') {
                final int eol = item.indexOf('\n', i + 2);
                i = eol < 0 ? len : eol;
            } else if (ch == '/' && i + 1 < len && item.charAt(i + 1) == '*') {
                final int close = item.indexOf("*/", i + 2);
                i = close < 0 ? len : close + 1;
            } else if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth = Math.max(0, depth - 1);
            } else if (depth == 0 && (ch == 'A' || ch == 'a') && i > 0 && i + 3 < len && Character.isWhitespace(item.charAt(i - 1))
                    && (item.charAt(i + 1) == 'S' || item.charAt(i + 1) == 's') && Character.isWhitespace(item.charAt(i + 2))
                    && !item.substring(i + 2).isBlank()) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the index of the quote closing the quoted region opened at {@code open} (a doubled quote
     * inside the region is an escape), or the end of {@code item} when the region is unterminated. A PostgreSQL
     * {@code E'...'} string (a standalone {@code E} or {@code e} before the quote) also honors backslash escapes
     * ({@code E'it\'s'}), as the query builders and the tokenizer read it.
     */
    private static int closingQuoteIndex(final String item, final int open, final char quote) {
        final int len = item.length();
        final boolean backslashEscapes = quote == '\'' && isEscapeStringPrefix(item, open);

        for (int i = open + 1; i < len; i++) {
            final char ch = item.charAt(i);

            if (ch == quote) {
                if (i + 1 < len && item.charAt(i + 1) == quote) {
                    i++;
                } else {
                    return i;
                }
            } else if (backslashEscapes && ch == '\\') {
                i++;
            }
        }

        return len;
    }

    /**
     * Whether the quote at {@code quoteIndex} opens a PostgreSQL {@code E'...'} string: an {@code E} or {@code e}
     * directly before it that does not end a longer name, the rule of the query builders' alias scan.
     */
    private static boolean isEscapeStringPrefix(final String text, final int quoteIndex) {
        if (quoteIndex == 0 || (text.charAt(quoteIndex - 1) != 'E' && text.charAt(quoteIndex - 1) != 'e')) {
            return false;
        }

        if (quoteIndex == 1) {
            return true;
        }

        final char beforePrefix = text.charAt(quoteIndex - 2);

        return beforePrefix < 128 && beforePrefix != '_' && beforePrefix != '$' && !Character.isLetterOrDigit(beforePrefix);
    }

    /**
     * Returns whether {@code text} is a bare, optionally dot-qualified name: letters, digits, {@code _},
     * {@code $} and {@code .} only. Such a name is a property name candidate for the entity column mapping.
     */
    private static boolean isBareName(final String text) {
        if (text.isEmpty()) {
            return false;
        }

        for (int i = 0, len = text.length(); i < len; i++) {
            if (!isBareNameChar(text.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isBareNameChar(final char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$' || ch == '.';
    }

    /**
     * Returns the end of the name that starts {@code text} if whitespace follows it, or {@code -1}. The name is a
     * sequence of dot-separated parts, each a run of bare-name characters or one delimited part ({@code "..."} and
     * {@code `...`} with doubled-quote escapes, {@code [...]}), such as {@code acc.firstName}, {@code [firstName]} or
     * {@code t."First Name"}. A delimited part directly after other name text is no name part: {@code ids[1]} and
     * {@code ARRAY[1, 2]} are a subscript and an array constructor, and {@code a"b"} is not one name. A dollar-quoted
     * string ({@code $$a b$$}) is no name either.
     */
    private static int leadingNameEnd(final String text) {
        final int len = text.length();
        boolean partStart = true; // at the start of the text or directly after a '.'
        boolean afterDelimitedPart = false;

        for (int i = 0; i < len; i++) {
            final char ch = text.charAt(i);

            if (Character.isWhitespace(ch)) {
                return i;
            } else if (partStart && ch == '$' && QueryUtil.dollarQuoteEnd(text, i) != 0) {
                return -1;
            } else if (ch == '.') {
                partStart = true;
                afterDelimitedPart = false;
            } else if (ch == '"' || ch == '`' || ch == '[') {
                if (!partStart) {
                    return -1;
                }

                i = ch == '[' ? text.indexOf(']', i + 1) : closingQuoteIndex(text, i, ch);

                if (i < 0 || i >= len) {
                    return -1; // unterminated
                }

                partStart = false;
                afterDelimitedPart = true;
            } else if (afterDelimitedPart || !isBareNameChar(ch)) {
                return -1;
            } else {
                partStart = false;
            }
        }

        return -1;
    }

    /**
     * Returns whether {@code text} contains a line ({@code --}, {@code #}) or block (<code>/*</code>) comment token
     * outside the quoted regions {@code '...'}, {@code "..."}, {@code `...`} (doubled-quote escapes) and
     * {@code [...]}; it only decides which rendering path an item takes. As for the tokenizer, the {@code #} that
     * opens a {@code #{...}} marker or a {@code #>} / {@code #-} operator, or ends a {@code ?#} operator, is no
     * comment; any other {@code #} counts, so a SQL Server {@code #temp} name stays on the identifier path. An
     * unterminated quoted region is scanned like unquoted text. An {@code E'...'} string honors backslash escapes
     * (see {@link #closingQuoteIndex}). A dollar-quoted string is skipped too, and one that is never closed holds the
     * rest of the text: {@link #renderExpression} copies such a string, comment openers included, as written.
     */
    private static boolean containsCommentToken(final String text) {
        final int len = text.length();
        // Once a '[' has no closing ']', no later '[' has one either. Remembering that keeps the scan linear: searching
        // to the end again for every unclosed '[' made it quadratic in their number.
        boolean bracketUnclosed = false;

        for (int i = 0; i < len; i++) {
            final char ch = text.charAt(i);

            if (ch == '\'' || ch == '"' || ch == '`') {
                final int close = closingQuoteIndex(text, i, ch);

                if (close < len) {
                    i = close;
                }
            } else if (ch == '$') {
                final int end = QueryUtil.dollarQuoteEnd(text, i);

                if (end < 0) {
                    return false;
                }

                if (end > 0) {
                    i = end - 1;
                }
            } else if (ch == '[') {
                if (!bracketUnclosed) {
                    final int close = text.indexOf(']', i + 1);

                    if (close >= 0) {
                        i = close;
                    } else {
                        bracketUnclosed = true;
                    }
                }
            } else if ((ch == '-' || ch == '/') && i + 1 < len && text.charAt(i + 1) == (ch == '-' ? '-' : '*')) {
                return true;
            } else if (ch == '#') {
                // Only the '#' is passed over: the tokenizer still reads a comment inside a #{...} marker.
                final char next = i + 1 < len ? text.charAt(i + 1) : 0;

                if (next != '{' && next != '>' && next != '-' && (i == 0 || text.charAt(i - 1) != '?')) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Copies the supplied raw bindings.
     *
     * @param parameters the raw bindings to copy
     * @return a mutable binding snapshot
     * @throws IllegalArgumentException if {@code parameters} is null
     */
    private static List<Object> copyRawParameters(final Collection<?> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Raw subquery parameters must not be null");
        }

        final List<Object> copy = new ArrayList<>(parameters.size());
        copy.addAll(parameters);
        return copy;
    }

    /**
     * Returns {@code true} if any raw binding is a {@link Condition} (which is inlined rather than bound).
     *
     * @param bindings the copied raw bindings
     * @return whether {@link #inlineConditionBindings(String, List, List)} must run
     */
    private static boolean containsConditionBinding(final List<Object> bindings) {
        for (final Object binding : bindings) {
            if (binding instanceof Condition) {
                return true;
            }
        }

        return false;
    }

    /**
     * Writes the SQL-valued bindings of a raw subquery into its text, so the subquery renders the same SQL under
     * every SQL policy. A JDBC driver cannot bind a {@link SqlExpression} or {@link SubQuery} object, and every
     * structured condition already renders such a value as SQL text rather than a parameter; before this, only
     * {@code RAW_SQL} builders inlined them while parameterized/named builders passed the object to JDBC.
     * <ul>
     *   <li>a {@link SqlExpression} is written verbatim (the raw text around it is verbatim too, so no naming policy
     *       applies); it must not be blank or comment-only, and must not contain parameter markers of its own
     *       (such as {@link Filters#QME}), since those would have no binding;</li>
     *   <li>a raw {@link SubQuery} ({@code SubQuery(String[, Collection])}) is written as {@code (sql)}, and its own
     *       bindings take its place in the binding list;</li>
     *   <li>any other condition (a predicate, a structured or builder-backed subquery) is rejected: its rendering
     *       depends on the enclosing builder's naming or parameter policy, which a raw fragment cannot follow.</li>
     * </ul>
     * Inlined text ending in a line comment is terminated, and it is separated by a space from an adjacent
     * identifier/number character or from a {@code -} it would otherwise fuse with into a {@code --} comment,
     * mirroring how {@code RAW_SQL} builders inline values.
     *
     * @param sql the validated raw SQL text
     * @param bindings the validated bindings, one per positional placeholder, in order
     * @param valueBindings receives the bindings that remain after inlining, in placeholder order
     * @return the SQL text with the SQL-valued bindings written in place of their placeholders
     * @throws IllegalArgumentException if a binding is a condition that cannot be inlined, as described above
     */
    private static String inlineConditionBindings(final String sql, final List<Object> bindings, final List<Object> valueBindings) {
        final int[] offsets = QueryUtil.positionalParameterOffsets(sql);
        final StringBuilder sb = new StringBuilder(sql.length() + 32);
        int last = 0;

        for (int k = 0; k < offsets.length; k++) {
            final Object binding = bindings.get(k);

            if (!(binding instanceof Condition)) {
                valueBindings.add(binding);
                continue;
            }

            final String text = QueryUtil.terminateLineComment(inlineText((Condition) binding, k, valueBindings));
            final int i = offsets[k];

            sb.append(sql, last, i);

            if (i > 0 && (isGlueChar(sql.charAt(i - 1)) || (sql.charAt(i - 1) == '-' && text.startsWith("-")))) {
                sb.append(_SPACE);
            }

            sb.append(text);
            last = i + 1;

            if (last < sql.length() && isGlueChar(sql.charAt(last))) {
                sb.append(_SPACE);
            }
        }

        return sb.append(sql, last, sql.length()).toString();
    }

    /**
     * Returns the SQL text that replaces the placeholder of a condition-valued raw binding, appending a nested raw
     * subquery's own bindings to {@code valueBindings}.
     *
     * @param binding the condition-valued binding
     * @param index the binding's position, for error messages
     * @param valueBindings the remaining value bindings collected so far
     * @return the text to write in place of the placeholder
     * @throws IllegalArgumentException if {@code binding} cannot be inlined
     */
    private static String inlineText(final Condition binding, final int index, final List<Object> valueBindings) {
        if (binding instanceof final SqlExpression expr) {
            if (isEmptyPredicate(expr)) {
                throw new IllegalArgumentException("Raw subquery binding parameters[" + index + "] is a blank or comment-only SqlExpression");
            }

            if (ParsedSql.parse(expr.literal()).parameterCount() > 0) {
                throw new IllegalArgumentException("Raw subquery binding parameters[" + index + "] is a SqlExpression containing parameter markers ("
                        + expr.literal() + "), which would have no binding; write the placeholder into the SQL text instead");
            }

            return expr.literal();
        }

        if (binding.getClass() == SubQuery.class && ((SubQuery) binding).sql != null) {
            final SubQuery subQuery = (SubQuery) binding;
            valueBindings.addAll(subQuery.parameters());

            return SK.PARENTHESIS_L + QueryUtil.terminateLineComment(subQuery.sql) + SK.PARENTHESIS_R;
        }

        throw new IllegalArgumentException("Raw subquery binding parameters[" + index + "] must be a bind value, a SqlExpression or a raw SubQuery, not "
                + describeForMessage(binding) + ": its SQL depends on the enclosing builder's naming or parameter policy");
    }

    /** Returns {@code true} for a character that would fuse with adjacent inlined text into one identifier or number. */
    private static boolean isGlueChar(final char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$' || ch == '.';
    }

    /**
     * Snapshots the already-validated raw bindings the way condition values are snapshotted: arrays are
     * deep-copied and {@code Date}/{@code Calendar} values cloned via {@link #snapshotMutableValue(Object)};
     * every other binding is kept by reference. The caller's later mutation of a bound array or date therefore
     * never changes this subquery's {@link #parameters()}, {@link #equals(Object)}, or {@link #hashCode()}.
     *
     * @param bindings the copied, validated bindings in placeholder order (never {@code null})
     * @return an immutable list of snapshotted bindings
     * @throws IllegalArgumentException if an object-array binding contains a direct or indirect cycle
     */
    private static ImmutableList<Object> snapshotRawParameters(final List<Object> bindings) {
        final List<Object> snapshot = new ArrayList<>(bindings.size());

        for (final Object binding : bindings) {
            snapshot.add(snapshotMutableValue(binding));
        }

        return ImmutableList.wrap(snapshot);
    }

    /**
     * Detects whether any captured raw binding is an array, {@code Date}, or {@code Calendar}, in which case
     * {@link #parameters()} must return fresh defensive copies on every call (mirrors {@code Binary}).
     *
     * @param bindings the captured raw bindings (never {@code null})
     * @return {@code true} if at least one binding is a known mutable JDK value
     */
    private static boolean containsSnapshotMutableValue(final List<Object> bindings) {
        for (final Object binding : bindings) {
            if (isSnapshotMutableValue(binding)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks the raw SQL against its positional bindings.
     *
     * @param sql the non-blank raw SQL to inspect
     * @param parameters the non-null binding snapshot
     * @throws IllegalArgumentException if SQL placeholder syntax is malformed, named or MyBatis placeholders
     *         are present, the text is not a statement {@link ParsedSql} recognizes as a query yet contains a
     *         {@code ?} outside quoted text and comments, or the positional placeholder count differs from the
     *         binding count
     */
    /** A tokenizer token that is a string literal with an introducer or prefix, such as N'..', E'..', _utf8'..' or U&amp;'..'. */
    private static final Pattern PREFIXED_STRING_LITERAL_TOKEN = Pattern.compile("(?s)[\\p{Alnum}_&]+'.*");

    private static void validateRawBindings(final String sql, final Collection<?> parameters) {
        // ParsedSql supplies the project's quote/comment-aware placeholder scan and also distinguishes
        // PostgreSQL JSON question-mark operators from JDBC parameters.
        final ParsedSql parsedSql = ParsedSql.parse(sql);

        if (!parsedSql.namedParameters().isEmpty()) {
            throw new IllegalArgumentException("Raw subqueries support positional '?' bindings only; named and MyBatis placeholders are not supported");
        }

        // ParsedSql inspects placeholders only in a statement it recognizes (SELECT, WITH, VALUES, TABLE, ...); for
        // other text its count is 0 without inspection, so an unquoted '?' there is unverifiable: it would reach the
        // driver as an unbound placeholder, or leave a binding without one. Fail closed instead of trusting that 0.
        if (!parsedSql.isDataOperation() && sql.indexOf('?') >= 0) {
            // SqlParser.tokenize drops comments. A token starting with a quote is a quoted identifier or string literal, and one
            // such as N'..', E'..' or U&'..' is a prefixed string literal: a '?' inside either is not a placeholder.
            for (final String token : SqlParser.tokenize(sql)) {
                if (token.indexOf('?') >= 0 && "'\"`".indexOf(token.charAt(0)) < 0 && !PREFIXED_STRING_LITERAL_TOKEN.matcher(token).matches()) {
                    throw new IllegalArgumentException("Raw subquery text is not a recognized query (such as SELECT, WITH, VALUES or TABLE),"
                            + " so its '?' placeholders cannot be validated against the bindings: " + sql);
                }
            }
        }

        if (parsedSql.parameterCount() != parameters.size()) {
            throw new IllegalArgumentException(
                    "Raw subquery placeholder count (" + parsedSql.parameterCount() + ") does not match binding count (" + parameters.size() + ")");
        }
    }

    /**
     * Normalizes an optional structured-subquery filter.
     *
     * @param cond the optional trailing condition
     * @return the clause or normalized predicate, or null for an absent filter
     * @throws IllegalArgumentException if a Criteria carries a SELECT modifier, or a non-clause filter
     *         contains a null operator, ON/USING connector, quantified operand, standalone SubQuery,
     *         nested Criteria, or another non-predicate component
     */
    private static Condition normalizeCondition(final Condition cond) {
        if (cond == null || isClause(cond)) {
            return cond;
        }

        if (cond instanceof Criteria) {
            final Criteria criteria = (Criteria) cond;

            if (!Strings.isBlank(criteria.selectModifier())) {
                throw new IllegalArgumentException("Subquery criteria cannot include a SELECT modifier");
            }

            return criteria.conditions().isEmpty() ? null : criteria;
        }

        // Blank raw text carries no predicate. Empty junctions are different: they have explicit
        // Boolean identities and must survive as WHERE 1 = 1 / WHERE 1 = 0.
        if (cond instanceof SqlExpression && Strings.isBlank(((SqlExpression) cond).literal())) {
            return null;
        }

        if (isOnOrUsing(cond)) {
            throw new IllegalArgumentException("ON/USING conditions are not valid subquery filters");
        }

        return Filters.where(validateComposableOperand(cond, "subquery filter"));
    }

    /**
     * Returns the trailing condition or clause for this structured subquery. A predicate supplied to a
     * constructor is normalized to a {@link Where}; an explicitly supplied clause or non-empty
     * {@link Criteria} is retained. Raw SQL subqueries, builder-backed snapshots, and structured
     * subqueries without clauses return {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Structured subquery with condition
     * Condition activeCondition = Filters.equal("active", true);
     * SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), activeCondition);
     * Condition condition = subQuery.condition();
     * // Returns the wrapped WHERE condition: WHERE active = true
     *
     * // Raw SQL subquery returns null for condition()
     * SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE active = true");
     * Condition rawCondition = rawQuery.condition();
     * // Returns: null
     * }</pre>
     *
     * @return the normalized condition/clause, or {@code null} if none or for a raw SQL or builder-backed subquery
     */
    public Condition condition() {
        return condition;
    }

    /**
     * Returns this subquery's parameter values. Structured subqueries collect them from their condition;
     * raw subqueries return their captured positional bindings, and builder-backed snapshots retain
     * the values captured from their source builder.
     * For a structured subquery the list is built afresh on every call (it is not memoized here), so
     * mutable parameter values such as arrays or {@code Date}s come from the condition's own per-call
     * defensive copies and are never shared between callers. Raw bindings were snapshotted at construction
     * (see {@link #SubQuery(String, java.util.Collection)}); when any of them is an array, {@code Date}, or
     * {@code Calendar}, a fresh list holding fresh defensive copies of those bindings is built on every call,
     * so mutating a returned element never affects this subquery or a later call. A raw subquery whose bindings
     * are all scalars returns the same immutable list each time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Structured subquery: parameters are collected from the condition (in order)
     * Condition cond = Filters.and(Filters.equal("status", "active"), Filters.between("age", 18, 65));
     * SubQuery structured = Filters.subQuery("users", Arrays.asList("id"), cond);
     * List<Object> params = structured.parameters();
     * // returns ["active", 18, 65]
     *
     * // Raw SQL subquery with positional bindings
     * SubQuery raw = Filters.subQuery("SELECT id FROM users WHERE status = ?", Arrays.asList("active"));
     * List<Object> rawParams = raw.parameters();
     * // returns ["active"] (immutable)
     *
     * // Structured subquery without a condition: empty
     * SubQuery noCond = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);
     * List<Object> noCondParams = noCond.parameters();
     * // returns [] (empty immutable list)
     *
     * // Builder-backed snapshot: parameters from the source builder are retained
     * SubQuery built = PSC.select("id")
     *     .from("users")
     *     .where(Filters.equal("status", "active"))
     *     .toSubQuery();
     * List<Object> builtParams = built.parameters();
     * // returns ["active"]
     * }</pre>
     *
     * @return an immutable list of parameter values (array/{@code Date}/{@code Calendar} raw bindings are
     *         defensive copies), or an empty immutable list when none were captured
     */
    @Override
    public ImmutableList<Object> parameters() {
        if (sql == null) {
            return condition == null ? ImmutableList.empty() : condition.parameters();
        }

        return rebuildParametersPerCall ? snapshotRawParameters(rawParameters) : rawParameters;
    }

    /**
     * Converts this subquery to its SQL representation.
     *
     * <p>For raw SQL subqueries and builder-backed snapshots, returns the stored SQL as-is (the
     * {@code namingPolicy} is ignored because builder-backed SQL was already rendered by its source builder).
     * For structured subqueries, generates a {@code SELECT [props] FROM [entity] [condition-or-clauses]}
     * statement, applying the naming policy to property names, the entity/table name, and to
     * the trailing condition or clauses. A select item has only its identifiers converted, the way a query
     * builder renders the same item minus the builder's table-alias prefix: string literals, parameter markers,
     * delimited identifiers, function names, recognised (upper-case) SQL keywords and tokens that do not start
     * with an ASCII letter or {@code _} (numbers such as {@code 2faCode}, dollar-quoted strings such as
     * {@code $$text$$}) are kept as written ({@code COUNT(*) AS cnt}, {@code 'HelloWorld' AS s},
     * {@code CURRENT_TIMESTAMP AS ts}).
     * An alias is kept as written as well: one after {@code AS} ({@code firstName AS fn}), and an implicit one
     * when the item is exactly a name (bare, delimited or dot-qualified), whitespace and a bare alias
     * ({@code firstName fn}, {@code [firstName] fn}, {@code acc.firstName fn}), unless that name is a SQL keyword
     * in any case ({@code distinct userId} is a modifier and a column). After any other expression, a subscript
     * ({@code ids[1]}) or an array constructor ({@code ARRAY[1, 2]}) included, a trailing word is converted like an
     * identifier, as the builders do ({@code COUNT(*) myCount} renders as {@code COUNT(*) my_count} under
     * {@code SNAKE_CASE}). A projected expression that contains a comment token
     * outside quoted regions ({@code --}, <code>/*</code>, or a {@code #} other than that of a {@code #{...}}
     * marker or a {@code #>}, {@code #-} or {@code ?#} operator) is converted as one identifier, which keeps
     * the comment intact only under {@code NO_CHANGE}. Trailing line comments in a projection or table fragment
     * are terminated with a newline before generated separators or clauses are appended.</p>
     *
     * <p>This direct rendering is intended for diagnostics. For an entity-class subquery, table
     * metadata and explicit column mappings are applied to the SELECT list, also to a property inside an
     * expression ({@code max(emailAddress)}) or qualified with the entity's table alias ({@code p.emailAddress});
     * as in the query builders, a mapped column name written in place of its property ({@code EMAIL} for
     * {@code @Column("EMAIL")}) is kept as that column. Trailing conditions receive only the naming policy.
     * Render the subquery through a {@link SqlBuilder}
     * when condition identifiers must also resolve against entity column mappings.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Raw SQL subquery: SQL returned as-is, naming policy is ignored
     * SubQuery raw = Filters.subQuery("SELECT user_id FROM users");
     * String rawStr = raw.toSql(NamingPolicy.SNAKE_CASE);
     * // returns "SELECT user_id FROM users"
     *
     * // Structured subquery with NO_CHANGE: names rendered verbatim
     * SubQuery sq = Filters.subQuery("users", Arrays.asList("id", "name"), Filters.equal("status", "active"));
     * String s1 = sq.toSql(NamingPolicy.NO_CHANGE);
     * // returns "SELECT id, name FROM users WHERE status = 'active'"
     *
     * // Structured subquery with SNAKE_CASE: props, entity, and condition are all converted
     * SubQuery sq2 = Filters.subQuery("userAccount", Arrays.asList("firstName", "lastName"),
     *                                 Filters.equal("isActive", true));
     * String s2 = sq2.toSql(NamingPolicy.SNAKE_CASE);
     * // returns "SELECT first_name, last_name FROM user_account WHERE is_active = true"
     *
     * // null naming policy behaves like NO_CHANGE
     * SubQuery sq3 = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);
     * String s3 = sq3.toSql((NamingPolicy) null);
     * // returns "SELECT id FROM users"
     * }</pre>
     *
     * @param namingPolicy the naming policy to apply; if {@code null},
     *            {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
     * @return SQL representation of the subquery
     * @throws IllegalArgumentException if this is a structured subquery whose entity class is not a valid entity
     *         bean class, or if rendering its trailing condition rejects one of its values (for example a
     *         {@code NaN} or infinite {@link Float}/{@link Double})
     * @throws UnsupportedOperationException if a structured subquery inspects bean metadata that uses
     *         the {@code long} date format for a {@code LocalDate} or {@code LocalTime} property
     * @throws RuntimeException if a custom condition renderer or a value's string conversion throws an unchecked exception
     */
    @Override
    public String toSql(final NamingPolicy namingPolicy) {
        if (sql == null) {
            final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
            final Map<String, ColumnInfo> columnInfoMap = entityClass == null ? null : QueryUtil.propToColumnInfoMap(entityClass, effectiveNamingPolicy);
            final StringBuilder sb = Objectory.createStringBuilder();

            try {
                sb.append(SK.SELECT);
                sb.append(_SPACE);

                int i = 0;

                if (propNames != null && !propNames.isEmpty()) {
                    for (final String propName : propNames) {
                        if (i++ > 0) {
                            sb.append(COMMA_SPACE);
                        }

                        sb.append(QueryUtil.terminateLineComment(renderSelectItem(propName, entityClass, columnInfoMap, effectiveNamingPolicy)));
                    }
                } else {
                    sb.append("*");
                }

                // Guard against default (Kryo) state where entityName/entityClass are null:
                // skip the FROM clause entirely instead of rendering "FROM null".
                if (entityClass != null) {
                    sb.append(_SPACE);
                    sb.append(SK.FROM);
                    sb.append(_SPACE);
                    sb.append(QueryUtil.terminateLineComment(QueryUtil.tableNameAndAlias(entityClass, effectiveNamingPolicy)));
                } else if (!Strings.isEmpty(entityName)) {
                    sb.append(_SPACE);
                    sb.append(SK.FROM);
                    sb.append(_SPACE);
                    sb.append(QueryUtil.terminateLineComment(QueryUtil.convertIdentifier(entityName, effectiveNamingPolicy)));
                }

                if (condition != null) {
                    // A Criteria already emits a leading space before each of its clauses, so only add
                    // the separator when the rendered condition does not start with one.
                    final String conditionSql = condition.toSql(effectiveNamingPolicy);

                    if (!conditionSql.isEmpty() && conditionSql.charAt(0) != _SPACE) {
                        sb.append(_SPACE);
                    }

                    sb.append(conditionSql);
                }

                return sb.toString();
            } finally {
                Objectory.recycle(sb);
            }

        } else {
            return sql;
        }
    }

    /**
     * Generates the hash code for this subquery.
     * The base hash incorporates the stored SQL string, entity name, entity class, property names,
     * and condition. Specialized builder-backed snapshots additionally incorporate their retained
     * parameters, SQL policy, and placeholder metadata.
     * It is recomputed on every call because a structured subquery condition may contain retained
     * mutable parameter values; caching would preserve a stale transitive hash. Captured raw bindings
     * are hashed by content (array bindings element-wise via {@link N#deepHashCode(Object)}), consistent
     * with {@link #equals(Object)}; because array/{@code Date}/{@code Calendar} bindings were snapshotted at
     * construction, the hash is stable even if the caller later mutates the originals.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Equal subqueries produce equal hash codes
     * SubQuery a = Filters.subQuery("SELECT id FROM users");
     * SubQuery b = Filters.subQuery("SELECT id FROM users");
     * boolean sameHash = a.hashCode() == b.hashCode();
     * // returns true
     * }</pre>
     *
     * @return hash code based on SQL, entity name/class, properties, condition, and captured raw bindings
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((sql == null) ? 0 : sql.hashCode());
        h = (h * 31) + ((entityName == null) ? 0 : entityName.hashCode());
        h = (h * 31) + ((entityClass == null) ? 0 : entityClass.hashCode());
        h = (h * 31) + ((propNames == null) ? 0 : propNames.hashCode());
        h = (h * 31) + ((condition == null) ? 0 : condition.hashCode());
        h = (h * 31) + deepRawParametersHashCode(rawParameters);

        return h == 0 ? 1 : h;
    }

    /**
     * Hashes captured raw bindings by content: array bindings contribute their element-wise
     * {@link N#deepHashCode(Object)} so that the result pairs with {@link #deepRawParametersEquals(List, List)}.
     *
     * @param bindings the captured raw bindings (never {@code null})
     * @return the order-sensitive content hash of the bindings
     */
    private static int deepRawParametersHashCode(final List<Object> bindings) {
        int h = 1;

        for (final Object binding : bindings) {
            h = (h * 31) + N.deepHashCode(binding);
        }

        return h;
    }

    /**
     * Compares two captured raw-binding lists by content: same size, and each pair of bindings equal
     * via {@link N#deepEquals(Object, Object)} (element-wise for arrays, {@code equals} otherwise).
     *
     * @param left the first binding list (never {@code null})
     * @param right the second binding list (never {@code null})
     * @return {@code true} if both lists hold content-equal bindings in the same order
     */
    private static boolean deepRawParametersEquals(final List<Object> left, final List<Object> right) {
        if (left.size() != right.size()) {
            return false;
        }

        for (int i = 0, size = left.size(); i < size; i++) {
            if (!N.deepEquals(left.get(i), right.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if this subquery is equal to another object.
     * Two subqueries are equal only when they have the exact same runtime class and all of their identity
     * fields are equal: the entity name, the entity class, the selected properties, the stored SQL string,
     * the condition, and captured raw bindings. Raw bindings are compared by content (array bindings
     * element-wise via {@link N#deepEquals(Object, Object)}, other bindings through their own {@code equals});
     * since array/{@code Date}/{@code Calendar} bindings are snapshotted at construction, the result does not
     * change when the caller later mutates the originals.
     * Builder-backed snapshots additionally compare their retained parameters, SQL policy, and placeholder metadata. The entity name
     * participates even for raw-SQL subqueries, so two raw subqueries are equal only when both their
     * SQL and their entity name match.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Same raw SQL: equal
     * SubQuery a = Filters.subQuery("SELECT id FROM users");
     * SubQuery b = Filters.subQuery("SELECT id FROM users");
     * boolean eq = a.equals(b);
     * // returns true
     *
     * // Different raw SQL: not equal
     * SubQuery c = Filters.subQuery("SELECT id FROM customers");
     * boolean ne = a.equals(c);
     * // returns false
     *
     * // A class-based subquery is NOT equal to a name-based one, even with the same simple name,
     * // because the entity class is part of identity.
     * SubQuery classBased = Filters.subQuery(Product.class, Arrays.asList("id"), Filters.equal("active", true));
     * SubQuery nameBased = Filters.subQuery("Product", Arrays.asList("id"), Filters.equal("active", true));
     * boolean differ = classBased.equals(nameBased);
     * // returns false
     *
     * // Comparison with null or a different type
     * boolean vsNull = a.equals(null);   // returns false
     * boolean vsString = a.equals("x");  // returns false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects have the same runtime class and identity fields, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final SubQuery other = (SubQuery) obj;
        return N.equals(sql, other.sql) && N.equals(entityName, other.entityName) && N.equals(entityClass, other.entityClass)
                && N.equals(propNames, other.propNames) && N.equals(condition, other.condition) && deepRawParametersEquals(rawParameters, other.rawParameters);
    }
}
