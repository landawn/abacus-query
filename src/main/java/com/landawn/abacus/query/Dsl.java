/*
 * Copyright (C) 2026 HaiYang Li
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.query.SqlDialect.SQLPolicy;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.OperationType;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.Optional;

/**
 * Entry point for building SQL statements with a fixed {@link SqlDialect} (naming policy + parameter style).
 *
 * <p>
 * DSL = a specialized language/API for expressing one kind of task clearly
 * </p>
 *
 * <p>Each predefined constant on this class (e.g. {@link #PSC}, {@link #NSC}, {@link #SCSB}) is a
 * {@code Dsl} bound to a specific dialect. Call one of the statement methods &mdash; {@code insert},
 * {@code select}, {@code update}, {@code deleteFrom}, {@code count}, etc. &mdash; to obtain a fresh
 * {@link SqlBuilder} configured for that operation. Dsl instances are immutable and thread-safe; the
 * {@link SqlBuilder} instances they produce are not.</p>
 */
public final class Dsl {
    // Declared before the predefined constants below so it is non-null when their initializers call
    // forDialect(...); the canonical instances are registered into it by the static block that follows them.
    private static final Map<SqlDialect, Dsl> dslCache = new ConcurrentHashMap<>();

    /**
     * Parameterized-SQL DSL ({@code ?} placeholders) that leaves property/column names unchanged.
     */
    public static final Dsl PSB = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.NO_CHANGE).sqlPolicy(SQLPolicy.PARAMETERIZED_SQL).build());
    /**
     * Parameterized-SQL DSL ({@code ?} placeholders) with {@code snake_case} naming.
     */
    public static final Dsl PSC = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(SQLPolicy.PARAMETERIZED_SQL).build());
    /**
     * Parameterized-SQL DSL ({@code ?} placeholders) with {@code UPPER_CASE_WITH_UNDERSCORE} naming.
     */
    public static final Dsl PAC = forDialect(
            SqlDialect.builder().namingPolicy(NamingPolicy.SCREAMING_SNAKE_CASE).sqlPolicy(SQLPolicy.PARAMETERIZED_SQL).build());
    /**
     * Parameterized-SQL DSL ({@code ?} placeholders) with {@code lowerCamelCase} naming.
     */
    public static final Dsl PLC = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.CAMEL_CASE).sqlPolicy(SQLPolicy.PARAMETERIZED_SQL).build());
    /**
     * Named-SQL DSL ({@code :name} placeholders) that leaves property/column names unchanged.
     */
    public static final Dsl NSB = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.NO_CHANGE).sqlPolicy(SQLPolicy.NAMED_SQL).build());
    /**
     * Named-SQL DSL ({@code :name} placeholders) with {@code snake_case} naming.
     */
    public static final Dsl NSC = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(SQLPolicy.NAMED_SQL).build());
    /**
     * Named-SQL DSL ({@code :name} placeholders) with {@code UPPER_CASE_WITH_UNDERSCORE} naming.
     */
    public static final Dsl NAC = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.SCREAMING_SNAKE_CASE).sqlPolicy(SQLPolicy.NAMED_SQL).build());
    /**
     * Named-SQL DSL ({@code :name} placeholders) with {@code lowerCamelCase} naming.
     */
    public static final Dsl NLC = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.CAMEL_CASE).sqlPolicy(SQLPolicy.NAMED_SQL).build());
    /**
     * Raw-SQL DSL with {@code snake_case} naming; values are inlined as SQL literals rather than parameterized.
     *
     * @deprecated {@link #PSC} or {@link #NSC} is preferred for better security and performance.
     *             Un-parameterized SQL is vulnerable to SQL injection attacks.
     */
    @Deprecated
    public static final Dsl SCSB = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(SQLPolicy.RAW_SQL).build());
    /**
     * Raw-SQL DSL with {@code UPPER_CASE_WITH_UNDERSCORE} naming; values are inlined as SQL literals rather than parameterized.
     *
     * @deprecated {@link #PAC} or {@link #NAC} is preferred for better security and performance.
     *             Un-parameterized SQL is vulnerable to SQL injection attacks.
     */
    @Deprecated
    public static final Dsl ACSB = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.SCREAMING_SNAKE_CASE).sqlPolicy(SQLPolicy.RAW_SQL).build());
    /**
     * Raw-SQL DSL with {@code lowerCamelCase} naming; values are inlined as SQL literals rather than parameterized.
     *
     * @deprecated {@link #PLC} or {@link #NLC} is preferred for better security and performance.
     *             Un-parameterized SQL is vulnerable to SQL injection attacks.
     */
    @Deprecated
    public static final Dsl LCSB = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.CAMEL_CASE).sqlPolicy(SQLPolicy.RAW_SQL).build());
    /**
     * iBATIS/MyBatis-SQL DSL ({@code #{name}} placeholders) that leaves property/column names unchanged.
     *
     * @deprecated Use {@link #NSB} instead.
     *             Note: Switching from MSB to NSB changes the parameter style from iBATIS ({@code #{param}}) to named ({@code :param}).
     */
    @Deprecated
    public static final Dsl MSB = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.NO_CHANGE).sqlPolicy(SQLPolicy.IBATIS_SQL).build());
    /**
     * iBATIS/MyBatis-SQL DSL ({@code #{name}} placeholders) with {@code snake_case} naming.
     *
     * @deprecated Use {@link #NSC} instead.
     *             Note: Switching from MSC to NSC changes the parameter style from iBATIS ({@code #{param}}) to named ({@code :param}).
     */
    @Deprecated
    public static final Dsl MSC = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(SQLPolicy.IBATIS_SQL).build());
    /**
     * iBATIS/MyBatis-SQL DSL ({@code #{name}} placeholders) with {@code UPPER_CASE_WITH_UNDERSCORE} naming.
     *
     * @deprecated Use {@link #NAC} instead.
     *             Note: Switching from MAC to NAC changes the parameter style from iBATIS ({@code #{param}}) to named ({@code :param}).
     */
    @Deprecated
    public static final Dsl MAC = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.SCREAMING_SNAKE_CASE).sqlPolicy(SQLPolicy.IBATIS_SQL).build());

    /**
     * iBATIS/MyBatis-SQL DSL ({@code #{name}} placeholders) with {@code lowerCamelCase} naming.
     *
     * @deprecated Use {@link #NLC} instead.
     *             Note: Switching from MLC to NLC changes the parameter style from iBATIS ({@code #{param}}) to named ({@code :param}).
     */
    @Deprecated
    public static final Dsl MLC = forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.CAMEL_CASE).sqlPolicy(SQLPolicy.IBATIS_SQL).build());

    static {
        dslCache.put(Dsl.PSB.sqlDialect, Dsl.PSB);
        dslCache.put(Dsl.PSC.sqlDialect, Dsl.PSC);
        dslCache.put(Dsl.PAC.sqlDialect, Dsl.PAC);
        dslCache.put(Dsl.PLC.sqlDialect, Dsl.PLC);
        dslCache.put(Dsl.NSB.sqlDialect, Dsl.NSB);
        dslCache.put(Dsl.NSC.sqlDialect, Dsl.NSC);
        dslCache.put(Dsl.NAC.sqlDialect, Dsl.NAC);
        dslCache.put(Dsl.NLC.sqlDialect, Dsl.NLC);
        dslCache.put(Dsl.SCSB.sqlDialect, Dsl.SCSB);
        dslCache.put(Dsl.ACSB.sqlDialect, Dsl.ACSB);
        dslCache.put(Dsl.LCSB.sqlDialect, Dsl.LCSB);
        dslCache.put(Dsl.MSB.sqlDialect, Dsl.MSB);
        dslCache.put(Dsl.MSC.sqlDialect, Dsl.MSC);
        dslCache.put(Dsl.MAC.sqlDialect, Dsl.MAC);
        dslCache.put(Dsl.MLC.sqlDialect, Dsl.MLC);
    }

    final SqlDialect sqlDialect;

    final NamingPolicy namingPolicy;

    Dsl(final SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
        namingPolicy = sqlDialect.namingPolicy() == null ? NamingPolicy.SNAKE_CASE : sqlDialect.namingPolicy();
    }

    /**
     * Creates a {@code Dsl} bound to the given {@link SqlDialect}, fixing the naming policy and
     * parameter style of every {@link SqlBuilder} it produces.
     *
     * <p>The common dialect combinations are already exposed as predefined constants on
     * {@link Dsl} (for example {@link Dsl#PSC} or {@link Dsl#NSC}); use this
     * factory to obtain a DSL for a combination that is not predefined. The returned {@code Dsl} is
     * immutable and thread-safe, so it is typically stored in a {@code static final} field and reused.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * static final Dsl MY_DSL = Dsl.forDialect(SqlDialect.builder()
     *         .namingPolicy(NamingPolicy.SNAKE_CASE)
     *         .sqlPolicy(SQLPolicy.PARAMETERIZED_SQL)
     *         .build());
     *
     * String sql = MY_DSL.insert("firstName", "lastName").into("account").build().query();
     * // Output: INSERT INTO account (first_name, last_name) VALUES (?, ?)
     * }</pre>
     *
     * @param sqlDialect the dialect (naming policy, parameter style, identifier quote and optional product info) the new DSL is bound to
     * @return a new {@code Dsl} that produces {@link SqlBuilder} instances using the given dialect
     * @throws IllegalArgumentException if {@code sqlDialect} is {@code null}
     */
    public static Dsl forDialect(final SqlDialect sqlDialect) {
        N.checkArgNotNull(sqlDialect, "sqlDialect");

        final Dsl dsl = dslCache.get(sqlDialect);

        if (dsl != null) {
            return dsl;
        }

        return new Dsl(sqlDialect);
    }

    /**
     * Returns the {@link SqlDialect} this builder renders SQL with.
     *
     * @return the dialect (naming policy, parameter style, identifier quote and optional product info) bound to this builder
     */
    public SqlDialect sqlDialect() {
        return sqlDialect;
    }

    private SqlBuilder createSqlBuilderInstance() {
        return new SqlBuilder(sqlDialect);
    }

    /**
     * Creates an INSERT statement for a single column expression.
     *
     * <p>This method creates an INSERT statement template with a single column. The actual value
     * will be provided as a parameter when executing the query.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.insert("firstName").into("account").build().query();
     * // Output: INSERT INTO account (first_name) VALUES (?)
     * }</pre>
     *
     * @param expr the column name or expression to insert
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if expr is null or empty
     */
    public SqlBuilder insert(final String expr) {
        N.checkArgNotEmpty(expr, SqlBuilder.INSERTION_PART_MSG);

        return insert(N.asArray(expr));
    }

    /**
     * Creates an INSERT statement for multiple columns.
     *
     * <p>This method creates an INSERT statement template with multiple columns. Property names
     * are automatically converted to snake_case format. Values will be provided as parameters
     * when executing the query.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.insert("firstName", "lastName", "email")
     *                 .into("account")
     *                 .build().query();
     * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
     * }</pre>
     *
     * @param propOrColumnNames the property or column names to insert
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if propOrColumnNames is null or empty
     */
    public SqlBuilder insert(final String... propOrColumnNames) {
        N.checkArgNotEmpty(propOrColumnNames, SqlBuilder.INSERTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.ADD;
        instance._propOrColumnNames = Array.asList(propOrColumnNames);

        return instance;
    }

    /**
     * Creates an INSERT statement for a collection of columns.
     *
     * <p>This method provides flexibility when column names are dynamically generated or come from
     * a collection. Property names are automatically converted to snake_case format.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("firstName", "lastName", "email");
     * String sql = PSC.insert(columns).into("account").build().query();
     * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
     * }</pre>
     *
     * @param propOrColumnNames collection of property or column names to insert
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if propOrColumnNames is null or empty
     */
    public SqlBuilder insert(final Collection<String> propOrColumnNames) {
        N.checkArgNotEmpty(propOrColumnNames, SqlBuilder.INSERTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.ADD;
        instance._propOrColumnNames = new ArrayList<>(propOrColumnNames);

        return instance;
    }

    /**
     * Creates an INSERT statement from a map of property names and values.
     *
     * <p>This method generates an INSERT statement where map keys represent property names
     * (converted to snake_case) and values are used to generate parameter placeholders.
     * The actual values can be retrieved using the {@code build()} method.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> props = new LinkedHashMap<>();
     * props.put("firstName", "John");
     * props.put("lastName", "Doe");
     * SP sqlPair = PSC.insert(props).into("account").build();
     * // sqlPair.query(): INSERT INTO account (first_name, last_name) VALUES (?, ?)
     * // sqlPair.parameters(): ["John", "Doe"]
     * }</pre>
     *
     * @param props map of property names to their values
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if props is null or empty
     */
    public SqlBuilder insert(final Map<String, Object> props) {
        N.checkArgNotEmpty(props, SqlBuilder.INSERTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.ADD;
        instance._props = new LinkedHashMap<>(props);

        return instance;
    }

    /**
     * Creates an INSERT statement from an entity object.
     *
     * <p>This method inspects the entity object and includes all insertable properties of the entity
     * (those not marked with {@code @Transient}, {@code @ReadOnly}, or {@code @ReadOnlyId}).
     * Property names are converted to snake_case format.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * account.setEmail("john.doe@example.com");
     *
     * SP sqlPair = PSC.insert(account).into("account").build();
     * // sqlPair.query(): INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
     * // sqlPair.parameters(): ["John", "Doe", "john.doe@example.com"]
     * }</pre>
     *
     * @param entity the entity object to insert
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if entity is null
     */
    public SqlBuilder insert(final Object entity) {
        return insert(entity, null);
    }

    /**
     * Creates an INSERT statement from an entity object with excluded properties.
     *
     * <p>This method allows fine-grained control over which properties to include in the INSERT
     * statement. Properties in the exclusion set will not be included even if they have values
     * and are normally insertable.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * account.setEmail("john.doe@example.com");
     * account.setCreatedDate(new Date());
     *
     * Set<String> excluded = N.asSet("createdDate");
     * SP sqlPair = PSC.insert(account, excluded).into("account").build();
     * // sqlPair.query(): INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
     * // sqlPair.parameters(): ["John", "Doe", "john.doe@example.com"]
     * }</pre>
     *
     * @param entity the entity object to insert
     * @param excludedPropNames set of property names to exclude from the insert
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if entity is null
     */
    public SqlBuilder insert(final Object entity, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entity, SqlBuilder.INSERTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.ADD;
        instance.setEntityClass(entity.getClass());

        SqlBuilder.parseInsertEntity(instance, entity, excludedPropNames);

        return instance;
    }

    /**
     * Creates an INSERT statement for an entity class.
     *
     * <p>This method generates an INSERT statement template based on the entity class structure.
     * All properties suitable for insertion (excluding those marked with @Transient, @ReadOnly,
     * or @ReadOnlyId) are included. Property names are converted to snake_case format.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.insert(Account.class).into("account").build().query();
     * // Output: INSERT INTO account (first_name, last_name, email, created_date) VALUES (?, ?, ?, ?)
     * }</pre>
     *
     * @param entityClass the entity class to generate INSERT for
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder insert(final Class<?> entityClass) {
        return insert(entityClass, null);
    }

    /**
     * Creates an INSERT statement for an entity class with excluded properties.
     *
     * <p>This method generates an INSERT statement template based on the entity class structure,
     * excluding specified properties. This is useful for creating reusable INSERT templates
     * that exclude certain fields like auto-generated IDs or timestamps.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("createdDate", "modifiedDate");
     * String sql = PSC.insert(Account.class, excluded).into("account").build().query();
     * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
     * }</pre>
     *
     * @param entityClass the entity class to generate INSERT for
     * @param excludedPropNames set of property names to exclude from the insert
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, SqlBuilder.INSERTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.ADD;
        instance.setEntityClass(entityClass);
        instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

        return instance;
    }

    /**
     * Creates an INSERT INTO statement for an entity class.
     *
     * <p>This is a convenience method that combines insert() and into() operations.
     * The table name is automatically derived from the entity class name or @Table annotation.
     * Property names are converted to snake_case format.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.insertInto(Account.class).build().query();
     * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
     * }</pre>
     *
     * @param entityClass the entity class to generate INSERT INTO for
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder insertInto(final Class<?> entityClass) {
        return insertInto(entityClass, null);
    }

    /**
     * Creates an INSERT INTO statement for an entity class with excluded properties.
     *
     * <p>This convenience method combines insert() and into() operations while allowing
     * property exclusion. The table name is derived from the entity class.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("id", "createdDate");
     * String sql = PSC.insertInto(Account.class, excluded).build().query();
     * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
     * }</pre>
     *
     * @param entityClass the entity class to generate INSERT INTO for
     * @param excludedPropNames set of property names to exclude from the insert
     * @return a new SqlBuilder instance configured for INSERT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
        return insert(entityClass, excludedPropNames).into(entityClass);
    }

    /**
     * Generates a MySQL-style batch INSERT statement.
     *
     * <p>This method creates an efficient batch insert statement with multiple value sets
     * in a single INSERT statement, which is particularly useful for MySQL databases and
     * provides better performance than multiple individual INSERT statements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Account> accounts = Arrays.asList(
     *     new Account("John", "Doe"),
     *     new Account("Jane", "Smith"),
     *     new Account("Bob", "Johnson")
     * );
     *
     * SP sqlPair = PSC.batchInsert(accounts).into("account").build();
     * // sqlPair.query(): INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?), (?, ?)
     * // sqlPair.parameters(): ["John", "Doe", "Jane", "Smith", "Bob", "Johnson"]
     * }</pre>
     *
     * @param propsList list of entities or property maps to insert
     * @return a new SqlBuilder instance configured for batch INSERT operation
     * @throws IllegalArgumentException if {@code propsList} is null or empty, if every element is
     *         {@code null}, if the first non-null element is an empty {@code Map}, if elements have
     *         mixed types (some {@code Map}, some bean), or if the non-null elements do not all share
     *         the same key set / insertable property set
     */
    @Beta
    public SqlBuilder batchInsert(final Collection<?> propsList) {
        N.checkArgNotEmpty(propsList, SqlBuilder.INSERTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.ADD;
        final Optional<?> first = N.firstNonNull(propsList);

        if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
            instance.setEntityClass(first.get().getClass());
        }

        instance._propsList = SqlBuilder.toInsertPropsList(propsList);

        return instance;
    }

    /**
     * Creates an UPDATE statement for a table.
     *
     * <p>This method starts building an UPDATE statement. Use the {@code set(String...)} method
     * to specify which columns to update (each column gets a {@code ?} placeholder), or use
     * {@code set(Map)} to specify column names together with their values. Property names are
     * converted to snake_case format.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update("account")
     *                 .set("firstName", "lastName")
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * // Output: UPDATE account SET first_name = ?, last_name = ? WHERE id = ?
     * }</pre>
     *
     * @param tableName the name of the table to update
     * @return a new SqlBuilder instance configured for UPDATE operation
     * @throws IllegalArgumentException if tableName is null or empty
     */
    public SqlBuilder update(final String tableName) {
        N.checkArgNotEmpty(tableName, SqlBuilder.UPDATE_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.UPDATE;
        instance._tableName = tableName;

        return instance;
    }

    /**
     * Creates an UPDATE statement for a table with entity class mapping.
     *
     * <p>This method creates an UPDATE statement where the entity class provides property-to-column
     * name mapping information. This ensures proper snake_case conversion for all property names
     * used in the update operation. Use {@code set(String...)} to specify the column names to
     * update (each gets a {@code ?} placeholder), or {@code set(Map)} to supply names and values
     * together.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update("account", Account.class)
     *                 .set("firstName", "lastModified")
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * // Output: UPDATE account SET first_name = ?, last_modified = ? WHERE id = ?
     * }</pre>
     *
     * @param tableName the name of the table to update
     * @param entityClass the entity class for property mapping
     * @return a new SqlBuilder instance configured for UPDATE operation
     * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
     */
    public SqlBuilder update(final String tableName, final Class<?> entityClass) {
        N.checkArgNotEmpty(tableName, SqlBuilder.UPDATE_PART_MSG);
        N.checkArgNotNull(entityClass, SqlBuilder.UPDATE_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.UPDATE;
        instance._tableName = tableName;
        instance.setEntityClass(entityClass);

        return instance;
    }

    /**
     * Creates an UPDATE statement for an entity class.
     *
     * <p>This method derives the table name from the entity class name or {@code @Table} annotation
     * and pre-populates the SET clause with all updatable properties (those not marked
     * {@code @ReadOnly} or {@code @NonUpdatable}). A WHERE clause should be added before
     * calling {@code build()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.update(Account.class)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * // Output: UPDATE account SET first_name = ?, last_name = ?, email = ?, ... WHERE id = ?
     * }</pre>
     *
     * @param entityClass the entity class to update
     * @return a new SqlBuilder instance configured for UPDATE operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder update(final Class<?> entityClass) {
        return update(entityClass, null);
    }

    /**
     * Creates an UPDATE statement for an entity class with excluded properties.
     *
     * <p>This method creates an UPDATE statement excluding specified properties in addition to
     * those automatically excluded by annotations (@ReadOnly, @NonUpdatable). This is useful
     * for partial updates or when certain fields should never be updated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("createdDate", "createdBy");
     * String sql = PSC.update(Account.class, excluded)
     *                 .set(account)
     *                 .where(Filters.equal("id", account.getId()))
     *                 .build().query();
     * }</pre>
     *
     * @param entityClass the entity class to update
     * @param excludedPropNames set of property names to exclude from the update
     * @return a new SqlBuilder instance configured for UPDATE operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, SqlBuilder.UPDATE_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.UPDATE;
        instance.setEntityClass(entityClass);
        instance._tableName = SqlBuilder.getTableName(entityClass, instance._namingPolicy);
        instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

        return instance;
    }

    /**
     * Creates a DELETE FROM statement for a table.
     *
     * <p>This method starts building a DELETE statement. Typically followed by WHERE conditions
     * to specify which rows to delete. Property names in WHERE conditions will be converted
     * to snake_case format if an entity class is associated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.deleteFrom("account")
     *                 .where(Filters.equal("status", "inactive"))
     *                 .build().query();
     * // Output: DELETE FROM account WHERE status = ?
     * }</pre>
     *
     * @param tableName the name of the table to delete from
     * @return a new SqlBuilder instance configured for DELETE operation
     * @throws IllegalArgumentException if tableName is null or empty
     */
    public SqlBuilder deleteFrom(final String tableName) {
        N.checkArgNotEmpty(tableName, SqlBuilder.DELETION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.DELETE;
        instance._tableName = tableName;

        return instance;
    }

    /**
     * Creates a DELETE FROM statement for a table with entity class mapping.
     *
     * <p>This method creates a DELETE statement where the entity class provides property-to-column
     * name mapping for WHERE conditions. This ensures proper snake_case conversion for property
     * names used in conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.deleteFrom("account", Account.class)
     *                 .where(Filters.lessThan("lastLoginDate", thirtyDaysAgo))
     *                 .build().query();
     * // Output: DELETE FROM account WHERE last_login_date < ?
     * }</pre>
     *
     * @param tableName the name of the table to delete from
     * @param entityClass the entity class for property mapping
     * @return a new SqlBuilder instance configured for DELETE operation
     * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
     */
    public SqlBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
        N.checkArgNotEmpty(tableName, SqlBuilder.DELETION_PART_MSG);
        N.checkArgNotNull(entityClass, SqlBuilder.DELETION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.DELETE;
        instance._tableName = tableName;
        instance.setEntityClass(entityClass);

        return instance;
    }

    /**
     * Creates a DELETE FROM statement for an entity class.
     *
     * <p>This method creates a DELETE statement where the table name is derived from the entity
     * class name or @Table annotation. Property names in WHERE conditions will be automatically
     * converted to snake_case format.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.deleteFrom(Account.class)
     *                 .where(Filters.equal("id", 1))
     *                 .build().query();
     * // Output: DELETE FROM account WHERE id = ?
     * }</pre>
     *
     * @param entityClass the entity class to delete from
     * @return a new SqlBuilder instance configured for DELETE operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder deleteFrom(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, SqlBuilder.DELETION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.DELETE;
        instance.setEntityClass(entityClass);
        instance._tableName = SqlBuilder.getTableName(entityClass, instance._namingPolicy);

        return instance;
    }

    /**
     * Creates a SELECT statement with a single expression.
     *
     * <p>This method is useful for complex select expressions, aggregate functions, or when
     * selecting computed values. The expression is used as-is without property name conversion.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("COUNT(*)")
     *                 .from("account")
     *                 .where(Filters.equal("status", "active"))
     *                 .build().query();
     * // Output: SELECT COUNT(*) FROM account WHERE status = ?
     *
     * String sql2 = PSC.select("firstName || ' ' || lastName AS fullName")
     *                  .from("account")
     *                  .build().query();
     * // Output: SELECT firstName || ' ' || lastName AS fullName FROM account
     * }</pre>
     *
     * @param selectPart the select expression
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if selectPart is null or empty
     */
    public SqlBuilder select(final String selectPart) {
        N.checkArgNotEmpty(selectPart, SqlBuilder.SELECTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.QUERY;
        instance._propOrColumnNames = Array.asList(selectPart);
        return instance;
    }

    /**
     * Creates a SELECT statement with multiple columns.
     *
     * <p>This method creates a SELECT statement for multiple columns. Property names are
     * converted to snake_case format and aliased back to their original camelCase names
     * to maintain proper object mapping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select("id", "firstName", "lastName", "email")
     *                 .from("account")
     *                 .where(Filters.equal("status", "active"))
     *                 .build().query();
     * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = ?
     * }</pre>
     *
     * @param propOrColumnNames the property or column names to select
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if propOrColumnNames is null or empty
     */
    public SqlBuilder select(final String... propOrColumnNames) {
        N.checkArgNotEmpty(propOrColumnNames, SqlBuilder.SELECTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.QUERY;
        instance._propOrColumnNames = Array.asList(propOrColumnNames);

        return instance;
    }

    /**
     * Creates a SELECT statement with a collection of columns.
     *
     * <p>This method provides flexibility when column names are dynamically generated. Property
     * names are converted to snake_case format with appropriate aliases.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("id", "firstName", "lastName");
     * String sql = PSC.select(columns)
     *                 .from("account")
     *                 .build().query();
     * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName" FROM account
     * }</pre>
     *
     * @param propOrColumnNames collection of property or column names to select
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if propOrColumnNames is null or empty
     */
    public SqlBuilder select(final Collection<String> propOrColumnNames) {
        N.checkArgNotEmpty(propOrColumnNames, SqlBuilder.SELECTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.QUERY;
        instance._propOrColumnNames = new ArrayList<>(propOrColumnNames);

        return instance;
    }

    /**
     * Creates a SELECT statement with column aliases.
     *
     * <p>This method allows specifying custom aliases for selected columns. The map keys are
     * property names (converted to snake_case) and values are their desired aliases in the
     * result set.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> columnAliases = new LinkedHashMap<>();
     * columnAliases.put("firstName", "fname");
     * columnAliases.put("lastName", "lname");
     * columnAliases.put("emailAddress", "email");
     *
     * String sql = PSC.select(columnAliases)
     *                 .from("account")
     *                 .build().query();
     * // Output: SELECT first_name AS "fname", last_name AS "lname", email_address AS "email" FROM account
     * }</pre>
     *
     * @param propOrColumnNameAliases map of property/column names to their aliases
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if {@code propOrColumnNameAliases} is null or empty, or if any alias is
     *                                  blank, contains a quote character, a line break, or an SQL comment token
     */
    public SqlBuilder select(final Map<String, String> propOrColumnNameAliases) {
        N.checkArgNotEmpty(propOrColumnNameAliases, SqlBuilder.SELECTION_PART_MSG);
        validateColumnAliases(propOrColumnNameAliases);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.QUERY;
        instance._propOrColumnNameAliases = new LinkedHashMap<>(propOrColumnNameAliases);

        return instance;
    }

    /**
     * Creates a SELECT statement for all properties of an entity class.
     *
     * <p>This method generates a SELECT statement including all properties from the entity class
     * that are not marked with @Transient. Property names are converted to snake_case with
     * appropriate aliases.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select(Account.class)
     *                 .from("account")
     *                 .build().query();
     * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email, created_date AS "createdDate" FROM account
     * }</pre>
     *
     * @param entityClass the entity class to select properties from
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder select(final Class<?> entityClass) {
        return select(entityClass, false);
    }

    /**
     * Creates a SELECT statement for an entity class with optional sub-entity properties.
     *
     * <p>When includeSubEntityProperties is true, properties of nested entity objects are also
     * included in the selection with appropriate prefixes. This is useful for fetching related
     * entities in a single query.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Without sub-entities
     * String sql1 = PSC.select(Order.class, false)
     *                  .from("orders")
     *                  .build().query();
     *
     * // With sub-entities (includes nested object properties)
     * String sql2 = PSC.select(Order.class, true)
     *                  .from("orders")
     *                  .build().query();
     * }</pre>
     *
     * @param entityClass the entity class to select properties from
     * @param includeSubEntityProperties whether to include properties of nested entity objects
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
        return select(entityClass, includeSubEntityProperties, null);
    }

    /**
     * Creates a SELECT statement for an entity class with excluded properties.
     *
     * <p>This method selects all properties from the entity class except those specified in
     * the exclusion set. This is useful for queries that need most but not all properties.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("password", "secretKey");
     * String sql = PSC.select(Account.class, excluded)
     *                 .from("account")
     *                 .build().query();
     * // Selects all Account properties except password and secretKey
     * }</pre>
     *
     * @param entityClass the entity class to select properties from
     * @param excludedPropNames set of property names to exclude from selection
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
        return select(entityClass, false, excludedPropNames);
    }

    /**
     * Creates a SELECT statement for an entity class with sub-entities and exclusions.
     *
     * <p>This method provides full control over entity property selection, allowing both
     * inclusion of sub-entity properties and exclusion of specific properties.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("internalNotes", "auditLog");
     * String sql = PSC.select(Order.class, true, excluded)
     *                 .from("orders")
     *                 .build().query();
     * // Selects all Order properties including sub-entities, except excluded ones
     * }</pre>
     *
     * @param entityClass the entity class to select properties from
     * @param includeSubEntityProperties whether to include properties of nested entity objects
     * @param excludedPropNames set of property names to exclude from selection
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, SqlBuilder.SELECTION_PART_MSG);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.QUERY;
        instance.setEntityClass(entityClass);
        instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

        return instance;
    }

    /**
     * Creates a complete SELECT FROM statement for an entity class.
     *
     * <p>This is a convenience method that combines select() and from() operations.
     * The table name is automatically derived from the entity class name or @Table annotation.
     * All property names are converted to snake_case with appropriate aliases.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.selectFrom(Account.class)
     *                 .where(Filters.equal("status", "active"))
     *                 .build().query();
     * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = ?
     * }</pre>
     *
     * @param entityClass the entity class to select from
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder selectFrom(final Class<?> entityClass) {
        return selectFrom(entityClass, false);
    }

    /**
     * Creates a SELECT FROM statement for an entity class with table alias.
     *
     * <p>This method creates a SELECT FROM statement where columns are prefixed with the table
     * alias. This is useful for joins and disambiguating column names in complex queries.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.selectFrom(Account.class, "a")
     *                 .where(Filters.equal("a.status", "active"))
     *                 .build().query();
     * // Output: SELECT a.id, a.first_name AS "firstName", a.last_name AS "lastName", a.email FROM account a WHERE a.status = ?
     * }</pre>
     *
     * @param entityClass the entity class to select from
     * @param alias the table alias to use
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder selectFrom(final Class<?> entityClass, final String alias) {
        return selectFrom(entityClass, alias, false);
    }

    /**
     * Creates a SELECT FROM statement with optional sub-entity properties.
     *
     * <p>This convenience method combines SELECT and FROM operations with control over
     * sub-entity inclusion. When sub-entities are included, appropriate joins may be
     * generated automatically.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.selectFrom(Order.class, true)
     *                 .where(Filters.greaterThan("total", 100))
     *                 .build().query();
     * // Includes properties from nested entities like customer, items, etc.
     * }</pre>
     *
     * @param entityClass the entity class to select from
     * @param includeSubEntityProperties whether to include properties of nested entity objects
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
        return selectFrom(entityClass, includeSubEntityProperties, null);
    }

    /**
     * Creates a SELECT FROM statement with alias and sub-entity option.
     *
     * <p>This method combines table aliasing with sub-entity property inclusion for
     * complex queries involving related entities.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.selectFrom(Order.class, "o", true)
     *                 .where(Filters.equal("o.status", "pending"))
     *                 .build().query();
     * // Selects from orders with alias 'o' including sub-entity properties
     * }</pre>
     *
     * @param entityClass the entity class to select from
     * @param alias the table alias to use
     * @param includeSubEntityProperties whether to include properties of nested entity objects
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
        return selectFrom(entityClass, alias, includeSubEntityProperties, null);
    }

    /**
     * Creates a SELECT FROM statement with excluded properties.
     *
     * <p>This convenience method creates a complete SELECT FROM statement while excluding
     * specific properties from the selection.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("password", "secretKey");
     * String sql = PSC.selectFrom(Account.class, excluded)
     *                 .where(Filters.equal("active", true))
     *                 .build().query();
     * // Selects all properties except password and secretKey
     * }</pre>
     *
     * @param entityClass the entity class to select from
     * @param excludedPropNames set of property names to exclude from selection
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
        return selectFrom(entityClass, false, excludedPropNames);
    }

    /**
     * Creates a SELECT FROM statement with alias and excluded properties.
     *
     * <p>This method combines table aliasing with property exclusion for precise control
     * over the generated SELECT statement.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("password");
     * String sql = PSC.selectFrom(Account.class, "a", excluded)
     *                 .innerJoin("orders o").on("a.id = o.account_id")
     *                 .build().query();
     * // Selects from account with alias 'a', excluding password
     * }</pre>
     *
     * @param entityClass the entity class to select from
     * @param alias the table alias to use
     * @param excludedPropNames set of property names to exclude from selection
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
        return selectFrom(entityClass, alias, false, excludedPropNames);
    }

    /**
     * Creates a SELECT FROM statement with sub-entities and exclusions.
     *
     * <p>This method provides control over both sub-entity inclusion and property exclusion
     * while automatically determining the appropriate table alias.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("internalData");
     * String sql = PSC.selectFrom(Order.class, true, excluded)
     *                 .where(Filters.between("orderDate", startDate, endDate))
     *                 .build().query();
     * // Includes sub-entities but excludes internalData
     * }</pre>
     *
     * @param entityClass the entity class to select from
     * @param includeSubEntityProperties whether to include properties of nested entity objects
     * @param excludedPropNames set of property names to exclude from selection
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
        return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
    }

    /**
     * Creates a complete SELECT FROM statement with all options.
     *
     * <p>This method provides maximum flexibility by allowing control over table alias,
     * sub-entity inclusion, and property exclusion. It handles complex scenarios including
     * automatic join generation for sub-entities.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = N.asSet("password", "internalNotes");
     * String sql = PSC.selectFrom(Account.class, "a", true, excluded)
     *                 .innerJoin("orders o").on("a.id = o.account_id")
     *                 .where(Filters.greaterThan("o.total", 1000))
     *                 .build().query();
     * // Complex query with full control over selection
     * }</pre>
     *
     * @param entityClass the entity class to select from
     * @param alias the table alias to use
     * @param includeSubEntityProperties whether to include properties of nested entity objects
     * @param excludedPropNames set of property names to exclude from selection
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
            final Set<String> excludedPropNames) {
        N.checkArgNotNull(entityClass, SqlBuilder.SELECTION_PART_MSG);

        if (SqlBuilder.hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
            final List<String> selectTableNames = SqlBuilder.getSelectTableNames(entityClass, alias, excludedPropNames, namingPolicy);
            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
        }

        return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
    }

    /**
     * Creates a SELECT statement for multiple entity classes (for joins).
     *
     * <p>This method is designed for queries that need to select from multiple tables,
     * typically used with joins. Each entity gets both a table alias and a class alias
     * for proper result mapping. Property names are converted to snake_case.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.select(Account.class, "a", "account",
     *                        Order.class, "o", "order")
     *                 .from("account a")
     *                 .innerJoin("orders o").on("a.id = o.account_id")
     *                 .build().query();
     * // Output: SELECT a.id AS "account.id", a.first_name AS "account.firstName", ...,
     * //                o.id AS "order.id", o.order_date AS "order.orderDate", ...
     * }</pre>
     *
     * @param entityClassA first entity class
     * @param tableAliasA table alias for first entity
     * @param classAliasA property prefix for first entity results
     * @param entityClassB second entity class
     * @param tableAliasB table alias for second entity
     * @param classAliasB property prefix for second entity results
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if {@code entityClassA} or {@code entityClassB} is {@code null}
     */
    public SqlBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
            final String tableAliasB, final String classAliasB) {
        return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
    }

    /**
     * Creates a SELECT statement for multiple entity classes with exclusions.
     *
     * <p>Extended version that allows excluding specific properties from each entity
     * in the multi-table select. This is useful for joins where you don't need all
     * properties from each table.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> userExclude = N.asSet("password", "salt");
     * Set<String> orderExclude = N.asSet("internalNotes");
     *
     * String sql = PSC.select(Account.class, "a", "account", userExclude,
     *                        Order.class, "o", "order", orderExclude)
     *                 .from("account a")
     *                 .innerJoin("orders o").on("a.id = o.account_id")
     *                 .build().query();
     * }</pre>
     *
     * @param entityClassA first entity class
     * @param tableAliasA table alias for first entity
     * @param classAliasA property prefix for first entity results
     * @param excludedPropNamesA excluded properties for first entity
     * @param entityClassB second entity class
     * @param tableAliasB table alias for second entity
     * @param classAliasB property prefix for second entity results
     * @param excludedPropNamesB excluded properties for second entity
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClassA or entityClassB is null
     */
    public SqlBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
            final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
        N.checkArgNotNull(entityClassA, SqlBuilder.SELECTION_PART_MSG);

        final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

        return select(multiSelects);
    }

    /**
     * Creates a SELECT statement for multiple entities using Selection descriptors.
     *
     * <p>This is the most flexible method for multi-entity queries, allowing any number
     * of entities with full control over aliases, sub-entities, and exclusions. Each
     * Selection object defines how to select from one entity.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Selection> selections = Arrays.asList(
     *     new Selection(Account.class, "a", "account", null, false, null),
     *     new Selection(Order.class, "o", "order", Arrays.asList("id", "total"), false, null),
     *     new Selection(Product.class, "p", "product", null, false, N.asSet("description"))
     * );
     *
     * String sql = PSC.select(selections)
     *                 .from("account a")
     *                 .innerJoin("orders o").on("a.id = o.account_id")
     *                 .innerJoin("order_items oi").on("o.id = oi.order_id")
     *                 .innerJoin("products p").on("oi.product_id = p.id")
     *                 .build().query();
     * }</pre>
     *
     * @param multiSelects list of Selection objects defining what to select from each entity
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if multiSelects is null, empty, or contains invalid data
     */
    public SqlBuilder select(final List<Selection> multiSelects) {
        SqlBuilder.checkMultiSelects(multiSelects);

        final SqlBuilder instance = createSqlBuilderInstance();

        instance._op = OperationType.QUERY;
        instance.setEntityClass(multiSelects.get(0).entityClass());
        instance._multiSelects = new ArrayList<>(multiSelects);

        return instance;
    }

    /**
     * Creates a SELECT FROM statement for multiple entity classes.
     *
     * <p>This convenience method combines select() and from() for multi-table queries.
     * The FROM clause is automatically generated based on the entity classes and their
     * aliases.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.selectFrom(Account.class, "a", "account",
     *                            Order.class, "o", "order")
     *                 .where(Filters.expr("a.id = o.account_id"))
     *                 .build().query();
     * // Automatically generates appropriate FROM clause
     * }</pre>
     *
     * @param entityClassA first entity class
     * @param tableAliasA table alias for first entity
     * @param classAliasA property prefix for first entity
     * @param entityClassB second entity class
     * @param tableAliasB table alias for second entity
     * @param classAliasB property prefix for second entity
     * @return a new SqlBuilder instance with SELECT and FROM configured
     * @throws IllegalArgumentException if {@code entityClassA} or {@code entityClassB} is {@code null}
     */
    public SqlBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
            final String tableAliasB, final String classAliasB) {
        return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
    }

    /**
     * Creates a SELECT FROM statement for multiple entity classes with exclusions.
     *
     * <p>Extended version allowing property exclusions for each entity in the query.
     * The FROM clause is automatically generated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> userExclude = N.asSet("password");
     * String sql = PSC.selectFrom(Account.class, "a", "account", userExclude,
     *                            Order.class, "o", "order", null)
     *                 .where(Filters.expr("a.id = o.account_id"))
     *                 .build().query();
     * }</pre>
     *
     * @param entityClassA first entity class
     * @param tableAliasA table alias for first entity
     * @param classAliasA property prefix for first entity
     * @param excludedPropNamesA excluded properties for first entity
     * @param entityClassB second entity class
     * @param tableAliasB table alias for second entity
     * @param classAliasB property prefix for second entity
     * @param excludedPropNamesB excluded properties for second entity
     * @return a new SqlBuilder instance with SELECT and FROM configured
     * @throws IllegalArgumentException if entityClassA or entityClassB is null
     */
    public SqlBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
            final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
        N.checkArgNotNull(entityClassA, SqlBuilder.SELECTION_PART_MSG);

        final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

        return selectFrom(multiSelects);
    }

    /**
     * Creates a SELECT FROM statement for multiple entity selections.
     *
     * <p>Most flexible method for multi-entity queries with automatic FROM clause generation.
     * Each Selection object can have different configurations for its entity.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Selection> selections = Arrays.asList(
     *     new Selection(Account.class, "a", "account", null, false, null),
     *     new Selection(Order.class, "o", "order", null, true, null),  // include sub-entities
     *     new Selection(Product.class, "p", "product", null, false, N.asSet("details"))
     * );
     *
     * String sql = PSC.selectFrom(selections)
     *                 .where(Filters.equal("a.status", "active"))
     *                 .build().query();
     * }</pre>
     *
     * @param multiSelects list of Selection objects defining what to select from each entity
     * @return a new SqlBuilder instance with SELECT and FROM configured
     * @throws IllegalArgumentException if multiSelects is null, empty, or contains invalid data
     */
    public SqlBuilder selectFrom(final List<Selection> multiSelects) {
        SqlBuilder.checkMultiSelects(multiSelects);

        final String fromClause = SqlBuilder.getFromClause(multiSelects, namingPolicy);

        return select(multiSelects).from(fromClause);
    }

    /**
     * Creates a COUNT(*) query for a table.
     *
     * <p>Convenience method for generating count queries. This is equivalent to
     * {@code select("COUNT(*)").from(tableName)} but more expressive.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.count("account")
     *                 .where(Filters.equal("status", "active"))
     *                 .build().query();
     * // Output: SELECT count(*) FROM account WHERE status = ?
     * }</pre>
     *
     * @param tableName the table to count rows from
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if tableName is null or empty
     */
    public SqlBuilder count(final String tableName) {
        N.checkArgNotEmpty(tableName, SqlBuilder.SELECTION_PART_MSG);

        return select(SqlBuilder.COUNT_ALL_LIST).from(tableName);
    }

    /**
     * Creates a COUNT(*) query for an entity class.
     *
     * <p>The table name is derived from the entity class name or @Table annotation.
     * This is a convenient way to count rows with entity class mapping for proper
     * property name conversion in WHERE conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String sql = PSC.count(Account.class)
     *                 .where(Filters.isNotNull("email"))
     *                 .build().query();
     * // Output: SELECT count(*) FROM account WHERE email IS NOT NULL
     * }</pre>
     *
     * @param entityClass the entity class to count
     * @return a new SqlBuilder instance configured for SELECT operation
     * @throws IllegalArgumentException if entityClass is null
     */
    public SqlBuilder count(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass, SqlBuilder.SELECTION_PART_MSG);

        return select(SqlBuilder.COUNT_ALL_LIST).from(entityClass);
    }

    /**
     * Renders a condition as a standalone SQL fragment, using the given entity class for property-to-column mapping.
     *
     * <p>This method is useful for generating just the WHERE clause portion of a query
     * with proper property-to-column name mapping. The resulting builder is condition-only:
     * no {@code SELECT}, {@code FROM}, or other clause keyword is emitted, only the rendered condition.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition cond = Filters.and(
     *     Filters.equal("firstName", "John"),
     *     Filters.like("email", "%@example.com")
     * );
     *
     * String sql = PSC.renderCondition(cond, Account.class).build().query();
     * // Output: (first_name = ?) AND (email LIKE ?)
     * }</pre>
     *
     * @param cond the condition to render (must not be {@code null})
     * @param entityClass the entity class used for property-to-column mapping (may be {@code null})
     * @return a new SqlBuilder instance containing the rendered condition SQL
     * @throws IllegalArgumentException if cond is null
     */
    public SqlBuilder renderCondition(final Condition cond, final Class<?> entityClass) {
        N.checkArgNotNull(cond, "cond");

        final SqlBuilder instance = createSqlBuilderInstance();

        instance.setEntityClass(entityClass);
        instance._op = OperationType.QUERY;
        instance._isForConditionOnly = true;
        instance.append(cond);

        return instance;
    }

    /**
     * Renders a condition as a standalone SQL fragment, using the given entity class for property-to-column mapping.
     *
     * @param cond the condition to render (must not be {@code null})
     * @param entityClass the entity class used for property-to-column mapping (may be {@code null})
     * @return a new SqlBuilder instance containing the rendered condition SQL
     * @throws IllegalArgumentException if cond is null
     * @deprecated use {@link #renderCondition(Condition, Class)} - this method does not build a {@code FROM} clause despite its name
     */
    @Deprecated
    public SqlBuilder fromCondition(final Condition cond, final Class<?> entityClass) {
        return renderCondition(cond, entityClass);
    }

    static void validateColumnAliases(final Map<String, String> propOrColumnNameAliases) {
        for (final String alias : propOrColumnNameAliases.values()) {
            if (Strings.isBlank(alias) || alias.indexOf('"') >= 0 || alias.indexOf('`') >= 0 || alias.indexOf('\r') >= 0 || alias.indexOf('\n') >= 0
                    || alias.contains("--") || alias.contains("/*") || alias.contains("*/")) {
                throw new IllegalArgumentException("Column alias must not be null, blank, quoted, or contain SQL comment tokens");
            }
        }
    }

}