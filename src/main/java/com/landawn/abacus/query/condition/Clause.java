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

import java.util.List;

import com.landawn.abacus.query.SK;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;

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
public abstract class Clause extends AbstractCondition {

    private Condition condition;

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
        super(operator);
        this.condition = N.checkArgNotNull(cond, "cond");
    }

    /**
     * Gets the wrapped condition.
     * The returned condition can be cast to its specific type if needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a NOT clause wrapping an equality condition
     * Clause notClause = new Clause(Operator.NOT, Filters.equal("status", "active"));
     * Condition inner = notClause.getCondition();   // the Equal condition for status = 'active'
     *
     * // Create an EXISTS clause with a subquery
     * SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.user_id = users.id");
     * Clause existsClause = new Clause(Operator.EXISTS, subQuery);
     * SubQuery sq = existsClause.getCondition();   // the SubQuery instance
     * }</pre>
     *
     * @param <T> the type of condition to return
     * @return the wrapped condition, cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T extends Condition> T getCondition() {
        return (T) condition;
    }

    /**
     * Sets the wrapped condition.
     * This method should generally not be used as conditions should be immutable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Deprecated: prefer creating a new Clause instead
     * Clause notClause = new Clause(Operator.NOT, Filters.equal("status", "active"));
     * notClause.setCondition(Filters.equal("status", "inactive"));   // Not recommended
     *
     * // Preferred approach: create a new Clause
     * Clause newNotClause = new Clause(Operator.NOT, Filters.equal("status", "inactive"));
     * }</pre>
     *
     * @param cond the new condition to wrap
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     */
    @Deprecated
    public void setCondition(final Condition cond) {
        this.condition = cond;
    }

    /**
     * Gets the parameters from the wrapped condition.
     * This method delegates to the wrapped condition's getParameters method.
     * 
     * @return a list of parameters from the wrapped condition, or an empty list if no condition is set
     */
    @Override
    public List<Object> getParameters() {
        return (condition == null) ? N.emptyList() : condition.getParameters();
    }

    /**
     * Clears all parameter values by setting them to null to free memory.
     * This method delegates to the wrapped condition's clearParameters method.
     *
     */
    @Override
    public void clearParameters() {
        if (condition != null) {
            condition.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this Clause.
     * The wrapped condition is also copied if present, ensuring complete independence
     * between the original and the copy.
     * 
     * @param <T> the type of condition to return
     * @return a new Clause instance with copied values
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final Clause copy = super.copy();

        if (condition != null) {
            copy.condition = condition.copy();
        }

        return (T) copy;
    }

    /**
     * Converts this Clause condition to its string representation using the specified naming policy.
     * The output format is: OPERATOR condition_string
     * 
     * @param namingPolicy the naming policy to apply to property names
     * @return a string representation of this Clause
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return operator().toString() + ((condition == null) ? Strings.EMPTY : SK._SPACE + condition.toString(namingPolicy));
    }

    /**
     * Returns the hash code of this Clause.
     * The hash code is computed based on the operator and wrapped condition.
     * 
     * @return hash code based on operator and wrapped condition
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + ((condition == null) ? 0 : condition.hashCode());
    }

    /**
     * Checks if this Clause is equal to another object.
     * Two Clauses are equal if they have the same operator and wrapped condition.
     * 
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Clause other) {
            return N.equals(operator, other.operator) && N.equals(condition, other.condition);
        }

        return false;
    }
}