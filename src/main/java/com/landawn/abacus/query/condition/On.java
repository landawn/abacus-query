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

import java.util.Map;

import com.landawn.abacus.query.Filters;

/**
 * Represents an ON clause used in SQL JOIN operations.
 * The ON clause specifies the join condition between tables, providing maximum flexibility
 * for defining how tables should be related in a join operation.
 * 
 * <p>Key features of ON clause:
 * <ul>
 *   <li>Supports joins on columns with different names</li>
 *   <li>Allows complex join conditions with AND/OR logic</li>
 *   <li>Can include additional filtering conditions beyond equality</li>
 *   <li>Supports expressions and functions in join conditions</li>
 *   <li>More flexible than USING clause but more verbose</li>
 * </ul>
 * 
 * <p>Common usage patterns:
 * <ul>
 *   <li>Simple foreign key joins: {@code t1.id = t2.foreign_id}</li>
 *   <li>Composite key joins: multiple equality conditions with AND</li>
 *   <li>Range joins: using BETWEEN or inequality operators</li>
 *   <li>Conditional joins: including business logic in the join</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple column equality join
 * On on1 = new On("orders.customer_id", "customers.id");
 * // Generates: ON orders.customer_id = customers.id
 *
 * // Used in a JOIN
 * InnerJoin join = new InnerJoin("customers", on1);
 * // Generates: INNER JOIN customers ON orders.customer_id = customers.id
 *
 * // Complex condition with custom logic using Expression
 * Condition complexJoin = Filters.and(
 *     new On("o.customer_id", "c.id"),
 *     Filters.expr("o.order_date > c.registration_date")
 * );
 * LeftJoin leftJoin = new LeftJoin("customers c", complexJoin);
 * // Generates: LEFT JOIN customers c (ON o.customer_id = c.id) AND (o.order_date > c.registration_date)
 *
 * // Multiple join conditions using Map (composite key)
 * Map<String, String> joinMap = new LinkedHashMap<>();
 * joinMap.put("emp.department_id", "dept.id");
 * joinMap.put("emp.location_id", "dept.location_id");
 * On on3 = new On(joinMap);
 * // Generates: ON emp.department_id = dept.id AND emp.location_id = dept.location_id
 *
 * // Join with ON condition and additional filter
 * Condition filteredJoin = Filters.and(
 *     new On("products.category_id", "categories.id"),
 *     Filters.eq("categories.active", true)
 * );
 * RightJoin rightJoin = new RightJoin("categories", filteredJoin);
 * // Generates: RIGHT JOIN categories (ON products.category_id = categories.id) AND (categories.active = true)
 * }</pre>
 * 
 * @see Using
 * @see Join
 * @see InnerJoin
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 * @see CrossJoin
 * @see NaturalJoin
 * @see Cell
 */
public class On extends Cell {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized On instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    On() {
    }

    /**
     * Creates an ON clause with a custom condition.
     * This is the most flexible constructor, accepting any type of condition
     * for maximum control over the join logic. Typically used for complex joins
     * that go beyond simple column equality.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple equality using Expression
     * On on1 = new On(Filters.expr("a.id = b.a_id"));
     * InnerJoin join1 = new InnerJoin("table_b b", on1);
     * // Generates: INNER JOIN table_b b a.id = b.a_id
     *
     * // Complex multi-condition join
     * Condition complexCondition = Filters.and(
     *     Filters.expr("orders.customer_id = customers.id"),
     *     Filters.between("orders.order_date", "2024-01-01", "2024-12-31"),
     *     Filters.ne("customers.status", "DELETED")
     * );
     * On on2 = new On(complexCondition);
     * LeftJoin join2 = new LeftJoin("customers", on2);
     * // Generates: LEFT JOIN customers (orders.customer_id = customers.id) AND (orders.order_date BETWEEN '2024-01-01' AND '2024-12-31') AND (customers.status != 'DELETED')
     *
     * // Range join for salary bands
     * Condition rangeJoin = Filters.and(
     *     Filters.expr("emp.salary >= salary_grades.min_salary"),
     *     Filters.expr("emp.salary <= salary_grades.max_salary")
     * );
     * On on3 = new On(rangeJoin);
     * InnerJoin join3 = new InnerJoin("salary_grades", on3);
     * // Generates: INNER JOIN salary_grades (emp.salary >= salary_grades.min_salary) AND (emp.salary <= salary_grades.max_salary)
     * }</pre>
     *
     * @param condition the join condition, can be any type of condition including
     *                  Expression, Equal, And, Or, Between, or more complex conditions
     */
    public On(final Condition condition) {
        super(Operator.ON, condition);
    }

    /**
     * Creates an ON clause for simple column equality between tables.
     * This is a convenience constructor for the most common join scenario
     * where you're joining on equal values between two columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic foreign key join
     * On on1 = new On("orders.customer_id", "customers.id");
     * InnerJoin join1 = new InnerJoin("customers", on1);
     * // Generates: INNER JOIN customers ON orders.customer_id = customers.id
     *
     * // Join with table aliases
     * On on2 = new On("o.product_id", "p.id");
     * LeftJoin join2 = new LeftJoin("products p", on2);
     * // Generates: LEFT JOIN products p ON o.product_id = p.id
     *
     * // Self-join scenario
     * On on3 = new On("emp1.manager_id", "emp2.employee_id");
     * LeftJoin join3 = new LeftJoin("employees emp2", on3);
     * // Generates: LEFT JOIN employees emp2 ON emp1.manager_id = emp2.employee_id
     * }</pre>
     *
     * @param propName the column name from the first table (can include table name/alias)
     * @param anoPropName the column name from the second table (can include table name/alias)
     */
    public On(final String propName, final String anoPropName) {
        this(createOnCondition(propName, anoPropName));
    }

    /**
     * Creates an ON clause with multiple column equality conditions.
     * All conditions in the map are combined with AND. This is useful for
     * composite key joins or when multiple columns must match between tables.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Composite primary key join
     * Map<String, String> compositeKey = new LinkedHashMap<>();
     * compositeKey.put("order_items.order_id", "orders.id");
     * compositeKey.put("order_items.customer_id", "orders.customer_id");
     * On on1 = new On(compositeKey);
     * InnerJoin join1 = new InnerJoin("orders", on1);
     * // Generates: INNER JOIN orders ON order_items.order_id = orders.id
     * //                             AND order_items.customer_id = orders.customer_id
     *
     * // Multi-column natural key join
     * Map<String, String> naturalKey = new LinkedHashMap<>();
     * naturalKey.put("addresses.country_code", "countries.code");
     * naturalKey.put("addresses.region_code", "countries.region_code");
     * On on2 = new On(naturalKey);
     * LeftJoin join2 = new LeftJoin("countries", on2);
     * // Generates: LEFT JOIN countries ON addresses.country_code = countries.code
     * //                                AND addresses.region_code = countries.region_code
     *
     * // Three-column composite join
     * Map<String, String> tripleKey = new LinkedHashMap<>();
     * tripleKey.put("t1.col1", "t2.col1");
     * tripleKey.put("t1.col2", "t2.col2");
     * tripleKey.put("t1.col3", "t2.col3");
     * On on3 = new On(tripleKey);
     * // Generates: ON t1.col1 = t2.col1 AND t1.col2 = t2.col2 AND t1.col3 = t2.col3
     * }</pre>
     *
     * @param propNamePair map of column pairs where key is from first table,
     *                     value is from second table. Order is preserved if LinkedHashMap is used.
     */
    public On(final Map<String, String> propNamePair) {
        this(createOnCondition(propNamePair));
    }

    /**
     * Creates an ON condition for simple column equality.
     * This static factory method is used internally to create Equal conditions
     * for the convenience constructors.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition joinCondition = On.createOnCondition("users.id", "posts.user_id");
     * // Creates: Equal("users.id", Expression("posts.user_id"))
     * }</pre>
     *
     * @param propName the first column name
     * @param anoPropName the second column name
     * @return an Equal condition comparing the two columns
     */
    static Condition createOnCondition(final String propName, final String anoPropName) {
        return new Equal(propName, Filters.expr(anoPropName));
    }

    /**
     * Creates an ON condition from multiple column pairs.
     * If only one pair is provided, returns a simple Equal condition.
     * If multiple pairs are provided, combines them with AND.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> joinColumns = new LinkedHashMap<>();
     * joinColumns.put("t1.col1", "t2.col1");
     * joinColumns.put("t1.col2", "t2.col2");
     * Condition condition = On.createOnCondition(joinColumns);
     * // Creates: And(Equal("t1.col1", Expression("t2.col1")),
     * //              Equal("t1.col2", Expression("t2.col2")))
     * }</pre>
     *
     * @param propNamePair map of column name pairs
     * @return a single Equal condition or an And condition combining multiple equalities
     */
    static Condition createOnCondition(final Map<String, String> propNamePair) {
        if (propNamePair.size() == 1) {
            final Map.Entry<String, String> entry = propNamePair.entrySet().iterator().next();

            return createOnCondition(entry.getKey(), entry.getValue());
        } else {
            final And and = Filters.and();

            for (final Map.Entry<String, String> entry : propNamePair.entrySet()) {
                and.add(createOnCondition(entry.getKey(), entry.getValue()));
            }

            return and;
        }
    }
}