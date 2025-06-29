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

package com.landawn.abacus.condition;

import static com.landawn.abacus.util.WD.COMMA_SPACE;
import static com.landawn.abacus.util.WD._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Represents a subquery that can be used within SQL conditions.
 * A subquery is a SELECT statement nested inside another SQL statement.
 * 
 * <p>This class supports two types of subqueries:</p>
 * <ul>
 *   <li>Raw SQL subqueries - directly specified SQL strings</li>
 *   <li>Structured subqueries - built from entity names, property names, and conditions</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Raw SQL subquery
 * SubQuery subQuery1 = new SubQuery("SELECT id FROM users WHERE status = 'active'");
 * 
 * // Structured subquery with entity name
 * Condition activeCondition = new Equal("status", "active");
 * SubQuery subQuery2 = new SubQuery("users", Arrays.asList("id"), activeCondition);
 * // Generates: SELECT id FROM users WHERE status = 'active'
 * 
 * // Structured subquery with entity class
 * SubQuery subQuery3 = new SubQuery(User.class, Arrays.asList("id", "name"), 
 *                                   new GreaterThan("age", 18));
 * // Generates: SELECT id, name FROM User WHERE age > 18
 * 
 * // Use in IN condition
 * In inCondition = new In("userId", subQuery1);
 * // Results in: userId IN (SELECT id FROM users WHERE status = 'active')
 * }</pre>
 */
public class SubQuery extends AbstractCondition {

    // For Kryo
    final String entityName;

    // For Kryo
    final Class<?> entityClass;

    private Collection<String> propNames;

    // For Kryo
    final String sql;

    /**
     * Field condition.
     */
    private Condition condition;

    // For Kryo
    SubQuery() {
        entityName = null;
        entityClass = null;
        sql = null;
    }

    /**
     * Constructs a subquery with raw SQL.
     *
     * @param sql the SQL SELECT statement
     * @throws IllegalArgumentException if sql is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("SELECT MAX(salary) FROM employees");
     * }</pre>
     */
    public SubQuery(final String sql) {
        this(Strings.EMPTY, sql);
    }

    /**
     * Constructs a subquery with an entity name and raw SQL.
     * The entity name is for reference only when using raw SQL.
     *
     * @param entityName the entity/table name (can be empty)
     * @param sql the SQL SELECT statement
     * @throws IllegalArgumentException if sql is null or empty
     * 
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery("orders", 
     *     "SELECT order_id FROM orders WHERE total > 1000");
     * }</pre>
     */
    public SubQuery(final String entityName, final String sql) {
        super(Operator.EMPTY);
        this.entityName = entityName;
        entityClass = null;

        if (Strings.isEmpty(sql)) {
            throw new IllegalArgumentException("The sql script can't be null or empty.");
        }

        propNames = null;
        condition = null;
        this.sql = sql;
    }

    /**
     * Constructs a structured subquery with entity name, selected properties, and condition.
     *
     * @param entityName the entity/table name
     * @param propNames collection of property names to select
     * @param condition the WHERE condition (if it's not already a clause, it will be wrapped in WHERE)
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> props = Arrays.asList("id", "email");
     * Condition condition = new And(
     *     new Equal("active", true),
     *     new GreaterThan("created", "2023-01-01")
     * );
     * SubQuery subQuery = new SubQuery("users", props, condition);
     * // Generates: SELECT id, email FROM users WHERE active = true AND created > '2023-01-01'
     * }</pre>
     */
    public SubQuery(final String entityName, final Collection<String> propNames, final Condition condition) {
        super(Operator.EMPTY);
        this.entityName = entityName;
        entityClass = null;
        this.propNames = propNames;
        if (condition == null || CriteriaUtil.isClause(condition) || condition instanceof Expression) {
            this.condition = condition;
        } else {
            this.condition = CF.where(condition);
        }

        sql = null;
    }

    /**
     * Constructs a structured subquery with entity class, selected properties, and condition.
     * The entity name is derived from the class's simple name.
     *
     * @param entityClass the entity class
     * @param propNames collection of property names to select
     * @param condition the WHERE condition (if it's not already a clause, it will be wrapped in WHERE)
     * 
     * <p>Example:</p>
     * <pre>{@code
     * SubQuery subQuery = new SubQuery(Product.class, 
     *     Arrays.asList("id", "categoryId"),
     *     new Like("name", "%electronics%")
     * );
     * // Generates: SELECT id, categoryId FROM Product WHERE name LIKE '%electronics%'
     * }</pre>
     */
    public SubQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition) {
        super(Operator.EMPTY);
        entityName = ClassUtil.getSimpleClassName(entityClass);
        this.entityClass = entityClass;
        this.propNames = propNames;
        if (condition == null || CriteriaUtil.isClause(condition) || condition instanceof Expression) {
            this.condition = condition;
        } else {
            this.condition = CF.where(condition);
        }

        sql = null;
    }

    /**
     * Returns the raw SQL script if this is a raw SQL subquery.
     *
     * @return the SQL script, or null if this is a structured subquery
     */
    public String getSql() {
        return sql;
    }

    /**
     * Gets the entity/table name for this subquery.
     *
     * @return the entity name
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Gets the entity class if this subquery was created with a class.
     *
     * @return the entity class, or null if created with entity name
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * Gets the collection of property names to select in this subquery.
     *
     * @return collection of property names, or null for raw SQL subqueries
     */
    public Collection<String> getSelectPropNames() {
        return propNames;
    }

    /**
     * Gets the WHERE condition for this subquery.
     *
     * @return the condition, or null if no condition or raw SQL subquery
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Gets the list of parameter values from the condition.
     *
     * @return list of parameters, or empty list if no condition
     */
    @Override
    public List<Object> getParameters() {
        return condition == null ? N.emptyList() : condition.getParameters();
    }

    /**
     * Clears all parameters from the condition.
     */
    @Override
    public void clearParameters() {
        if (condition != null) {
            condition.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this subquery.
     *
     * @param <T> the type of condition to return
     * @return a new SubQuery instance with copied values
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
     * @param namingPolicy the naming policy to apply
     * @return string representation of the subquery
     * 
     * <p>For raw SQL subqueries, returns the SQL as-is.
     * For structured subqueries, generates the SELECT statement.</p>
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (sql == null) {
            final StringBuilder sb = Objectory.createStringBuilder();

            try {
                sb.append(WD.SELECT);
                sb.append(_SPACE);

                int i = 0;

                for (final String propName : propNames) {
                    if (i++ > 0) {
                        sb.append(COMMA_SPACE);
                    }

                    sb.append(propName);
                }

                sb.append(_SPACE);
                sb.append(WD.FROM);

                sb.append(_SPACE);
                sb.append(entityName);

                if (condition != null) {
                    sb.append(_SPACE);

                    sb.append(condition.toString(namingPolicy));
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
     *
     * @return hash code based on sql, entity name, properties, and condition
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((sql == null) ? 0 : sql.hashCode());
        h = (h * 31) + ((entityName == null) ? 0 : entityName.hashCode());
        h = (h * 31) + ((propNames == null) ? 0 : propNames.hashCode());
        return (h * 31) + ((condition == null) ? 0 : condition.hashCode());
    }

    /**
     * Checks if this subquery is equal to another object.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final SubQuery other) {
            return N.equals(sql, other.sql) && N.equals(entityName, other.entityName) && N.equals(propNames, other.propNames)
                    && N.equals(condition, other.condition);
        }

        return false;
    }
}