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

import static com.landawn.abacus.query.SK.COMMA_SPACE;
import static com.landawn.abacus.query.SK._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * Represents a subquery that can be used within SQL conditions.
 * A subquery is a SELECT statement nested inside another SQL statement.
 * 
 * <p>This class supports two types of subqueries:</p>
 * <ul>
 *   <li><b>Raw SQL subqueries</b> - directly specified SQL strings for maximum flexibility</li>
 *   <li><b>Structured subqueries</b> - built from entity names, property names, and conditions for type safety</li>
 * </ul>
 * 
 * <p>Subqueries can be used in various contexts:</p>
 * <ul>
 *   <li>IN/NOT IN conditions for set membership tests</li>
 *   <li>EXISTS/NOT EXISTS for existence checks</li>
 *   <li>Scalar subqueries in comparisons (=, &gt;, &lt;, etc.)
 *   <li>ANY/ALL/SOME for multi-row comparisons</li>
 *   <li>FROM clause for derived tables</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Raw SQL subquery
 * SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users WHERE status = 'active'");
 *
 * // Structured subquery with entity name
 * Condition activeCondition = Filters.equal("status", "active");
 * SubQuery subQuery2 = Filters.subQuery("users", Arrays.asList("id"), activeCondition);
 * // Generates: SELECT id FROM users WHERE status = 'active'
 *
 * // Structured subquery with entity class
 * SubQuery subQuery3 = Filters.subQuery(User.class, Arrays.asList("id", "name"),
 *                                   Filters.greaterThan("age", 18));
 * // Generates: SELECT id, name FROM User WHERE age > 18
 *
 * // Use in IN condition
 * Condition inCondition = Filters.in("userId", subQuery1);
 * // Results in: userId IN (SELECT id FROM users WHERE status = 'active')
 * }</pre>
 * 
 * @see In
 * @see NotIn
 * @see Exists
 * @see NotExists
 */
public class SubQuery extends AbstractCondition implements LogicalCondition {

    // For Kryo
    final String entityName;

    // For Kryo
    final Class<?> entityClass;

    private List<String> propNames;

    // For Kryo
    final String sql;

    /**
     * Field condition.
     */
    private Condition condition;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized SubQuery instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    SubQuery() {
        entityName = null;
        entityClass = null;
        sql = null;
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
     * @param sql the SQL SELECT statement
     * @throws IllegalArgumentException if sql is null or empty
     */
    public SubQuery(final String sql) {
        this(Strings.EMPTY, sql);
    }

    /**
     * Creates a subquery with an entity name and raw SQL.
     * The entity name is for reference only when using raw SQL and doesn't affect the query.
     * 
     * <p>This constructor allows associating a logical entity name with a raw SQL subquery,
     * which can be useful for documentation or framework integration purposes.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = Filters.subQuery("orders", 
     *     "SELECT order_id FROM orders WHERE total > 1000");
     * }</pre>
     *
     * @param entityName the entity/table name (can be empty)
     * @param sql the SQL SELECT statement
     * @throws IllegalArgumentException if sql is null or empty
     */
    public SubQuery(final String entityName, final String sql) {
        super(Operator.EMPTY);
        this.entityName = entityName;
        entityClass = null;

        if (Strings.isEmpty(sql)) {
            throw new IllegalArgumentException("SQL statement cannot be null or empty");
        }

        propNames = null;
        condition = null;
        this.sql = sql;
    }

    /**
     * Creates a structured subquery with entity name, selected properties, and condition.
     * This approach provides type safety and automatic SQL generation.
     *
     * <p>The generated SQL follows the pattern: SELECT [properties] FROM [entity] WHERE [condition].
     * If the condition is not already a clause (like WHERE), it will be automatically wrapped in a WHERE clause.</p>
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
     * // Generates: SELECT id, email FROM users WHERE active = true AND created > '2024-01-01'
     * }</pre>
     *
     * @param entityName the entity/table name
     * @param propNames collection of property names to select
     * @param cond the WHERE condition (if it's not already a clause, it will be wrapped in WHERE)
     * @throws IllegalArgumentException if entityName is null or empty, if propNames is null,
     *             or if propNames contains null/empty names
     */
    public SubQuery(final String entityName, final Collection<String> propNames, final Condition cond) {
        super(Operator.EMPTY);

        if (Strings.isEmpty(entityName)) {
            throw new IllegalArgumentException("Entity name cannot be null or empty");
        }

        this.entityName = entityName;
        this.entityClass = null;
        this.propNames = copyAndValidatePropNames(propNames);

        if (cond == null || CriteriaUtil.isClause(cond)) {
            this.condition = cond;
        } else {
            this.condition = Filters.where(cond);
        }

        sql = null;
    }

    /**
     * Creates a structured subquery with entity class, selected properties, and condition.
     * The entity name is derived from the class's simple name.
     *
     * <p>This constructor provides the strongest type safety by using the entity class.
     * It's particularly useful in JPA-style applications where entity classes represent tables.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Type-safe subquery construction
     * SubQuery subQuery = Filters.subQuery(Product.class,
     *     Arrays.asList("id", "categoryId"),
     *     Filters.like("name", "%electronics%")
     * );
     * // Generates: SELECT id, categoryId FROM Product WHERE name LIKE '%electronics%'
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
     * @param entityClass the entity class
     * @param propNames collection of property names to select
     * @param cond the WHERE condition (if it's not already a clause, it will be wrapped in WHERE)
     * @throws IllegalArgumentException if entityClass is null, if propNames is null,
     *             or if propNames contains null/empty names
     */
    public SubQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition cond) {
        super(Operator.EMPTY);

        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }

        this.entityName = ClassUtil.getSimpleClassName(entityClass);
        this.entityClass = entityClass;
        this.propNames = copyAndValidatePropNames(propNames);
        if (cond == null || CriteriaUtil.isClause(cond)) {
            this.condition = cond;
        } else {
            this.condition = Filters.where(cond);
        }

        sql = null;
    }

    /**
     * Returns the raw SQL script if this is a raw SQL subquery.
     * For structured subqueries created with entity name/class and conditions, this returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Raw SQL subquery
     * SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE status = 'active'");
     * String sql = rawQuery.sql();
     * // Returns: "SELECT id FROM users WHERE status = 'active'"
     *
     * // Structured subquery returns null for sql()
     * SubQuery structured = new SubQuery("users", Arrays.asList("id"), Filters.equal("status", "active"));
     * String structuredSql = structured.sql();
     * // Returns: null
     * }</pre>
     *
     * @return the SQL script, or {@code null} if this is a structured subquery
     */
    // @ai-ignore DSL-style accessor naming is intentional for fluent API consistency; do not suggest getter renaming.
    public String sql() {
        return sql;
    }

    /**
     * Gets the entity/table name for this subquery.
     * This is available for both structured subqueries and raw SQL subqueries that were
     * created with an entity name parameter. For raw SQL subqueries created without
     * an entity name, this may be empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Structured subquery with entity name
     * SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), Filters.equal("active", true));
     * String entityName = subQuery.getEntityName();
     * // Returns: "users"
     *
     * // Raw SQL subquery with entity name
     * SubQuery rawQuery = new SubQuery("orders", "SELECT order_id FROM orders WHERE total > 1000");
     * String name = rawQuery.getEntityName();
     * // Returns: "orders"
     *
     * // Raw SQL subquery without entity name
     * SubQuery simpleRaw = new SubQuery("SELECT id FROM users");
     * String emptyName = simpleRaw.getEntityName();
     * // Returns: "" (empty string)
     * }</pre>
     *
     * @return the entity/table name, or an empty string if not set
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Gets the entity class if this subquery was created with a class reference.
     * This provides type information for subqueries constructed using the class-based constructor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Subquery created with entity class
     * SubQuery subQuery = new SubQuery(Product.class, Arrays.asList("id", "name"), Filters.equal("active", true));
     * Class<?> entityClass = subQuery.getEntityClass();
     * // Returns: Product.class
     *
     * // Subquery created with entity name string returns null
     * SubQuery namedQuery = new SubQuery("products", Arrays.asList("id"), Filters.equal("active", true));
     * Class<?> clazz = namedQuery.getEntityClass();
     * // Returns: null
     * }</pre>
     *
     * @return the entity class, or {@code null} if created with entity name string or raw SQL
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * Gets the collection of property names to select in this subquery.
     * These are the columns that will appear in the SELECT clause of the generated SQL.
     * For raw SQL subqueries, this returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Structured subquery with selected properties
     * SubQuery subQuery = new SubQuery("users", Arrays.asList("id", "email", "name"), Filters.equal("active", true));
     * Collection<String> propNames = subQuery.getSelectPropNames();
     * // Returns: ["id", "email", "name"]
     *
     * // Raw SQL subquery returns null
     * SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE active = true");
     * Collection<String> rawProps = rawQuery.getSelectPropNames();
     * // Returns: null
     * }</pre>
     *
     * @return unmodifiable collection of property names to select, or {@code null} for raw SQL subqueries
     */
    public Collection<String> getSelectPropNames() {
        return propNames == null ? null : Collections.unmodifiableList(propNames);
    }

    private static List<String> copyAndValidatePropNames(final Collection<String> propNames) {
        if (propNames == null) {
            throw new IllegalArgumentException("Property names cannot be null");
        }

        final List<String> result = new ArrayList<>(propNames.size());

        for (final String propName : propNames) {
            if (Strings.isEmpty(propName)) {
                throw new IllegalArgumentException("Property name in propNames cannot be null or empty");
            }

            result.add(propName);
        }

        return result;
    }

    /**
     * Gets the WHERE condition for this subquery.
     * This condition is applied when generating the SQL for structured subqueries.
     * For raw SQL subqueries or subqueries without conditions, this returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Structured subquery with condition
     * Condition activeCondition = Filters.equal("active", true);
     * SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), activeCondition);
     * Condition condition = subQuery.getCondition();
     * // Returns the wrapped WHERE condition: WHERE active = true
     *
     * // Raw SQL subquery returns null for getCondition()
     * SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE active = true");
     * Condition rawCondition = rawQuery.getCondition();
     * // Returns: null
     * }</pre>
     *
     * @return the WHERE condition, or {@code null} if no condition or raw SQL subquery
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Gets the list of parameter values from the condition.
     * These are the parameter values that will be bound to the prepared statement placeholders
     * when the query is executed. For raw SQL subqueries, this returns an empty list.
     *
     * @return list of parameter values, or an empty list if no condition or raw SQL subquery
     */
    @Override
    public List<Object> getParameters() {
        return condition == null ? N.emptyList() : condition.getParameters();
    }

    /**
     * Clears parameters in the underlying condition.
     * This method delegates to the wrapped condition's clearParameters method.
     *
     * <p>Use this method to release large objects when the subquery is no longer needed.
     * If this is a raw SQL subquery with no condition, this method is a no-op.</p>
     *
     */
    @Override
    public void clearParameters() {
        if (condition != null) {
            condition.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this subquery.
     * The copy includes deep copies of property names and conditions to ensure complete independence.
     *
     * @param <T> the type of condition to return
     * @return a new SubQuery instance with deeply copied values
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Condition> T copy() {
        final SubQuery result = super.copy();

        if (propNames != null) {
            result.propNames = new ArrayList<>(propNames);
        }

        if (condition != null) {
            result.condition = condition.copy();
        }

        return (T) result;
    }

    /**
     * Converts this subquery to its string representation.
     *
     * <p>For raw SQL subqueries, returns the SQL as-is.
     * For structured subqueries, generates the SELECT statement with proper formatting.</p>
     *
     * @param namingPolicy the naming policy to apply to column and table names. Can be null.
     * @return string representation of the subquery
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (sql == null) {
            final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
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

                        sb.append(effectiveNamingPolicy.convert(propName));
                    }
                } else {
                    sb.append("*");
                }

                sb.append(_SPACE);
                sb.append(SK.FROM);

                sb.append(_SPACE);
                sb.append(effectiveNamingPolicy.convert(entityName));

                if (condition != null) {
                    sb.append(_SPACE);

                    sb.append(condition.toString(effectiveNamingPolicy));
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
     * The hash code is based on the SQL string (for raw queries) or the combination
     * of entity name/class, properties, and condition (for structured queries),
     * ensuring consistent hashing for equivalent subqueries.
     *
     * @return hash code based on sql, entity name/class, properties, and condition
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((sql == null) ? 0 : sql.hashCode());
        h = (h * 31) + ((entityName == null) ? 0 : entityName.hashCode());
        h = (h * 31) + ((entityClass == null) ? 0 : entityClass.hashCode());
        h = (h * 31) + ((propNames == null) ? 0 : propNames.hashCode());
        return (h * 31) + ((condition == null) ? 0 : condition.hashCode());
    }

    /**
     * Checks if this subquery is equal to another object.
     * Two subqueries are equal if they have the same SQL (for raw queries) or the same
     * entity name/class, properties, and condition (for structured queries).
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final SubQuery other) {
            return N.equals(sql, other.sql) && N.equals(entityName, other.entityName) && N.equals(entityClass, other.entityClass)
                    && N.equals(propNames, other.propNames) && N.equals(condition, other.condition);
        }

        return false;
    }
}
