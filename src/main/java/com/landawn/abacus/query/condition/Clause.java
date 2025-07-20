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
 * <p>A Clause is a special type of {@link Cell} that represents a complete SQL clause.
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
 * <p>Usage example:</p>
 * <pre>{@code
 * // Clauses are typically used through their specific implementations
 * Where where = new Where(CF.eq("status", "active"));
 * Having having = new Having(CF.gt("COUNT(*)", 5));
 * OrderBy orderBy = new OrderBy("created_date", DESC);
 * 
 * // Add them to criteria
 * Criteria criteria = new Criteria()
 *     .where(CF.eq("status", "active"))    // Correct usage
 *     .having(CF.gt("COUNT(*)", 5));
 * 
 * // Cannot combine clauses with AND/OR/NOT
 * // where.and(having); // This will throw UnsupportedOperationException
 * 
 * // Instead, combine conditions within a clause
 * Where complexWhere = new Where(
 *     CF.and(
 *         CF.eq("status", "active"),
 *         CF.gt("age", 18)
 *     )
 * );
 * }</pre>
 * 
 * @see Cell
 * @see Condition
 * @see Criteria
 */
public abstract class Clause extends Cell {

    // For Kryo
    Clause() {
    }

    /**
     * Creates a new Clause with the specified operator and condition.
     * The operator identifies the type of clause (WHERE, HAVING, etc.),
     * and the condition contains the actual filtering or sorting logic.
     * 
     * <p>Example implementation in a subclass:</p>
     * <pre>{@code
     * public class Where extends Clause {
     *     public Where(Condition condition) {
     *         super(Operator.WHERE, condition);
     *     }
     * }
     * }</pre>
     * 
     * @param operator the clause operator (e.g., WHERE, HAVING, GROUP_BY)
     * @param condition the condition to be wrapped by this clause
     */
    protected Clause(final Operator operator, final Condition condition) {
        super(operator, condition);
    }

    /**
     * This operation is not supported for Clause objects.
     * Clauses cannot be combined using AND logic.
     * 
     * <p>Clauses represent complete SQL components that cannot be logically combined.
     * For example, you cannot have "WHERE ... AND HAVING ..." at the same level.
     * Instead, use AND within the condition of a single clause.</p>
     * 
     * <p>Example of correct usage:</p>
     * <pre>{@code
     * // Wrong - trying to AND clauses
     * // where.and(having); // Throws UnsupportedOperationException
     * 
     * // Correct - AND conditions within a clause
     * Where where = new Where(
     *     CF.and(
     *         CF.eq("status", "active"),
     *         CF.gt("age", 18)
     *     )
     * );
     * }</pre>
     * 
     * @param condition the condition to AND with (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public And and(final Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported for Clause objects.
     * Clauses cannot be combined using OR logic.
     * 
     * <p>Clauses are structural components of SQL that must maintain their independence.
     * You cannot OR two different clause types together. Use OR within the condition
     * of a single clause instead.</p>
     * 
     * <p>Example of correct usage:</p>
     * <pre>{@code
     * // Wrong - trying to OR clauses
     * // where.or(orderBy); // Throws UnsupportedOperationException
     * 
     * // Correct - OR conditions within a clause
     * Where where = new Where(
     *     CF.or(
     *         CF.eq("status", "active"),
     *         CF.eq("status", "pending")
     *     )
     * );
     * }</pre>
     * 
     * @param condition the condition to OR with (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public Or or(final Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported for Clause objects.
     * Clauses cannot be negated using NOT logic.
     * 
     * <p>SQL clauses don't have a NOT form. You cannot have "NOT WHERE" or "NOT ORDER BY".
     * Instead, use NOT within the condition of the clause.</p>
     * 
     * <p>Example of correct usage:</p>
     * <pre>{@code
     * // Wrong - trying to negate a clause
     * // where.not(); // Throws UnsupportedOperationException
     * 
     * // Correct - NOT condition within a clause
     * Where where = new Where(
     *     CF.not(CF.eq("status", "inactive"))
     * );
     * 
     * // Or use specific NOT operations
     * Where where2 = new Where(CF.notIn("id", Arrays.asList(1, 2, 3)));
     * }</pre>
     * 
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public Not not() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}