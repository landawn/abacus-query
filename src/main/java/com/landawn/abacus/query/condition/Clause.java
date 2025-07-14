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
 * <p>This class extends {@link Cell} and disables the logical combination methods
 * by throwing {@link UnsupportedOperationException}.</p>
 * 
 * <p>Common subclasses include:</p>
 * <ul>
 *   <li>{@link Where} - WHERE clause for filtering rows</li>
 *   <li>{@link Having} - HAVING clause for filtering grouped results</li>
 *   <li>{@link GroupBy} - GROUP BY clause for grouping rows</li>
 *   <li>{@link OrderBy} - ORDER BY clause for sorting results</li>
 *   <li>{@link Join} - Various JOIN clauses</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Clauses are typically used through their specific implementations
 * Where where = new Where(CF.eq("status", "active"));
 * Having having = new Having(CF.gt("COUNT(*)", 5));
 * 
 * // Cannot combine clauses with AND/OR/NOT
 * // where.and(having); // This will throw UnsupportedOperationException
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
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public Not not() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}