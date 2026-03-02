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

/**
 * Abstract base class for SQL clause conditions.
 * Clauses represent major SQL query components like WHERE, HAVING, GROUP BY, ORDER BY, etc.
 * Unlike regular conditions, clauses typically cannot be combined using AND/OR/NOT operations.
 * 
 * <p>A Clause is a special type of {@link Clause} that represents a complete SQL clause.
 * While regular conditions can be combined with logical operators (AND, OR, NOT),
 * clauses are standalone components that structure the query. Attempting to use
 * logical operations on clauses will result in {@link UnsupportedOperationException}.</p>
 * 
 * <p>This design enforces proper SQL structure - you cannot, for example, AND two
 * WHERE clauses together. Instead, you combine the conditions within a single WHERE clause.</p>
 * 
 * <p>Common subclasses include:</p>
 * <ul>
 *   <li>{@link Where} - WHERE clause for filtering rows</li>
 *   <li>{@link Having} - HAVING clause for filtering grouped results</li>
 *   <li>{@link GroupBy} - GROUP BY clause for grouping rows</li>
 *   <li>{@link OrderBy} - ORDER BY clause for sorting results</li>
 *   <li>{@link Join} - Various JOIN clauses (INNER, LEFT, RIGHT, FULL, etc.)</li>
 *   <li>{@link Limit} - LIMIT clause for restricting result count</li>
 *   <li>{@link Union}, {@link Intersect}, {@link Except} - Set operations</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Clauses are typically used through their specific implementations
 * Where where = new Where(Filters.equal("status", "active"));
 * Having having = new Having(Filters.greaterThan("COUNT(*)", 5));
 * OrderBy orderBy = new OrderBy("created_date", SortDirection.DESC);
 *
 * // Add them to criteria
 * Criteria criteria = new Criteria()
 *     .where(Filters.equal("status", "active"))    // Correct usage
 *     .having(Filters.greaterThan("COUNT(*)", 5));
 *
 * // Cannot combine clauses with AND/OR/NOT
 * // where.and(having);   // This will throw UnsupportedOperationException
 *
 * // Instead, combine conditions within a clause
 * Where complexWhere = new Where(
 *     Filters.and(
 *         Filters.equal("status", "active"),
 *         Filters.greaterThan("age", 18)
 *     )
 * );
 * }</pre>
 * 
 * @see Clause
 * @see Condition
 * @see Criteria
 */
public abstract class Clause extends Cell {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Clause instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Clause() {
    }

    /**
     * Creates a new Clause with the specified operator and condition.
     * The Clause wraps the given condition and applies the specified operator to it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a NOT clause that negates a condition
     * Clause notClause = new Clause(Operator.NOT, Filters.isNull("email"));
     *
     * // Create an EXISTS clause for a subquery
     * SubQuery subQuery = Filters.subQuery("SELECT 1 FROM products WHERE price > 100");
     * Clause existsClause = new Clause(Operator.EXISTS, subQuery);
     * }</pre>
     *
     * @param operator the operator to apply to the condition
     * @param cond the condition to wrap (must not be null)
     */
    public Clause(final Operator operator, final Condition cond) {
        super(operator, cond);
    }

    /**
     * Not supported for structural clauses.
     * @throws UnsupportedOperationException always
     */
    @Override
    public Not not() {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for structural clauses.
     * @throws UnsupportedOperationException always
     */
    @Override
    public And and(final Condition cond) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for structural clauses.
     * @throws UnsupportedOperationException always
     */
    @Override
    public Or or(final Condition cond) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported for structural clauses.
     * @throws UnsupportedOperationException always
     */
    @Override
    public Or xor(final Condition cond) {
        throw new UnsupportedOperationException();
    }
}