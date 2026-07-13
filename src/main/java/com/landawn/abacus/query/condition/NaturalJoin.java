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

import java.util.Collection;

/**
 * Represents a NATURAL JOIN clause in SQL queries.
 * 
 * <p>A NATURAL JOIN automatically joins tables based on all columns with the same names 
 * in both tables. It's a special type of equi-join where the join predicate arises 
 * implicitly by comparing all columns in both tables that have the same column names.
 * The result set contains only one column for each pair of equally named columns.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Automatically identifies common column names</li>
 *   <li>Performs equality comparison on all matching columns</li>
 *   <li>Eliminates duplicate columns in the result</li>
 *   <li>No explicit join condition needed for matching columns</li>
 *   <li>Does not accept an explicit {@code ON}/{@code USING} or filter condition</li>
 * </ul>
 * 
 * <p>Important considerations:</p>
 * <ul>
 *   <li>Schema changes can silently affect query behavior</li>
 *   <li>Adding columns with common names changes join semantics</li>
 *   <li>Less explicit than other join types</li>
 *   <li>May join on unintended columns if naming conventions overlap</li>
 *   <li>Best used when table relationships are well-defined and stable</li>
 * </ul>
 * 
 * <p>Because the join predicate is implicit, a NATURAL JOIN does <b>not</b> accept an explicit
 * {@code ON}/{@code USING} or filter condition. The condition-taking constructors are therefore
 * <b>deprecated</b>: they exist only for API symmetry with the other {@link Join} subclasses, require
 * {@code cond} to be {@code null}, and reject any non-{@code null} condition with an
 * {@link IllegalArgumentException}. Prefer the condition-less {@link #NaturalJoin(String)} /
 * {@link #NaturalJoin(Collection)} constructors, and to apply an additional filter place it in the
 * enclosing query's {@code WHERE} clause instead.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple natural join - automatically joins on common column names
 * NaturalJoin join1 = new NaturalJoin("employees");
 * // SQL: NATURAL JOIN employees
 * // Automatically joins on any columns with identical names
 *
 * // Multiple tables natural join
 * List<String> tables = Arrays.asList("employees", "departments");
 * NaturalJoin multiJoin = new NaturalJoin(tables);
 * // SQL: NATURAL JOIN (employees, departments)
 * }</pre>
 *
 * @see Join
 * @see InnerJoin
 * @see LeftJoin
 * @see RightJoin
 * @see FullJoin
 * @see CrossJoin
 * @see On
 * @see Using
 */
public class NaturalJoin extends Join {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized NaturalJoin instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    NaturalJoin() {
    }

    /**
     * Creates a NATURAL JOIN clause for the specified table or entity.
     * The join will automatically use all columns with matching names between the tables.
     *
     * <p>This constructor creates a pure natural join without additional conditions.
     * The database engine will identify all columns with identical names in both tables
     * and create an implicit equality condition for each pair.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // If 'orders' and 'customers' both have 'customer_id' column
     * NaturalJoin join = new NaturalJoin("customers");
     * // SQL: NATURAL JOIN customers
     * // Automatically joins on orders.customer_id = customers.customer_id
     * }</pre>
     *
     * @param joinEntity the table or entity to join with. Can include alias (e.g., "orders o").
     * @throws IllegalArgumentException if {@code joinEntity} is {@code null}, empty, or blank
     */
    public NaturalJoin(final String joinEntity) {
        super(Operator.NATURAL_JOIN, joinEntity);
    }

    /**
     * Creates a NATURAL JOIN clause with multiple tables/entities.
     * The rendered SQL is {@code NATURAL JOIN (t1, t2, ...)};
     * because most databases do not accept a comma-separated list after {@code NATURAL JOIN}, this
     * form is rarely directly executable and is provided mainly for symmetry with the other join
     * subclasses. Prefer chaining individual {@link NaturalJoin} clauses for portable SQL.
     *
     * <p>Because a NATURAL JOIN derives its join predicate implicitly, no condition is needed; this is
     * a convenience for joining the supplied entities without an explicit condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join customers, orders, and products naturally
     * List<String> tables = Arrays.asList("customers", "orders", "products");
     * NaturalJoin join = new NaturalJoin(tables);
     * // SQL: NATURAL JOIN (customers, orders, products)
     * }</pre>
     *
     * @param joinEntities the collection of tables or entities to join with.
     * @throws IllegalArgumentException if {@code joinEntities} is {@code null} or empty, or contains {@code null}, empty, or blank elements
     */
    public NaturalJoin(final Collection<String> joinEntities) {
        super(Operator.NATURAL_JOIN, joinEntities, null);
    }

}
