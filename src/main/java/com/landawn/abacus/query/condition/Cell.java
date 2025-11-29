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
 * Represents a condition cell that wraps another condition with an operator.
 * This class serves as a container for a condition paired with a specific operator,
 * typically used for unary operations or clauses that modify other conditions.
 * 
 * <p>A Cell is commonly used to wrap conditions with operators like NOT, EXISTS,
 * or to create clause conditions like WHERE, HAVING, etc. It acts as a decorator
 * that adds an operator context to an existing condition.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a NOT cell
 * Cell notCell = new Cell(Operator.NOT, Filters.eq("status", "active"));
 * 
 * // Create an EXISTS cell with a subquery
 * SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.user_id = users.id");
 * Cell existsCell = new Cell(Operator.EXISTS, subQuery);
 * }</pre>
 * 
 * @see AbstractCondition
 * @see Condition
 * @see Operator
 */
public class Cell extends AbstractCondition {

    private Condition condition;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Cell instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Cell() {
    }

    /**
     * Creates a new Cell with the specified operator and condition.
     * The Cell wraps the given condition and applies the specified operator to it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a NOT cell that negates a condition
     * Cell notCell = new Cell(Operator.NOT, Filters.isNull("email"));
     *
     * // Create an EXISTS cell for a subquery
     * SubQuery subQuery = Filters.subQuery("SELECT 1 FROM products WHERE price > 100");
     * Cell existsCell = new Cell(Operator.EXISTS, subQuery);
     * }</pre>
     *
     * @param operator the operator to apply to the condition
     * @param condition the condition to wrap (must not be null)
     */
    public Cell(final Operator operator, final Condition condition) {
        super(operator);
        this.condition = N.requireNonNull(condition, "condition");
    }

    /**
     * Gets the wrapped condition.
     * The returned condition can be cast to its specific type if needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Cell cell = new Cell(Operator.NOT, Filters.eq("status", "active"));
     * Condition condition = cell.getCondition();  // Returns the Equal condition
     *
     * // For subqueries
     * SubQuery subQuery = Filters.subQuery("SELECT 1 FROM products WHERE price > 100");
     * Cell existsCell = new Cell(Operator.EXISTS, subQuery);
     * SubQuery sq = existsCell.getCondition();
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
     * Cell cell = new Cell(Operator.NOT, Filters.eq("status", "active"));
     * // Not recommended: cell.setCondition(Filters.eq("status", "inactive"));
     * }</pre>
     * 
     * @param condition the new condition to wrap
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     */
    @Deprecated
    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    /**
     * Gets the parameters from the wrapped condition.
     * This method delegates to the wrapped condition's getParameters method.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Cell cell = new Cell(Operator.NOT, Filters.eq("name", "John"));
     * List<Object> params = cell.getParameters();  // Returns ["John"]
     * 
     * // For complex conditions
     * Cell notBetween = new Cell(Operator.NOT, Filters.between("age", 18, 65));
     * List<Object> params2 = notBetween.getParameters();  // Returns [18, 65]
     * }</pre>
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
     * <p>The parameter list size remains unchanged, but all elements become null.
     * Use this method to release large objects when the condition is no longer needed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Cell cell = new Cell(Operator.NOT, Filters.between("age", 18, 65));
     * List<Object> parameters = cell.getParameters();          // Returns [18, 65]
     * cell.clearParameters();                                  // All parameters become null
     * List<Object> updatedParameters = cell.getParameters();   // Returns [null, null]
     * }</pre>
     */
    @Override
    public void clearParameters() {
        if (condition != null) {
            condition.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this Cell.
     * The wrapped condition is also copied if present, ensuring complete independence
     * between the original and the copy.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Cell original = new Cell(Operator.NOT, Filters.eq("status", "active"));
     * Cell copy = original.copy();
     * // copy is a deep copy of original, including the wrapped condition
     * 
     * // Modifying the copy doesn't affect the original
     * copy.clearParameters();
     * List<Object> originalParams = original.getParameters();  // Still contains ["active"]
     * }</pre>
     * 
     * @param <T> the type of condition to return
     * @return a new Cell instance with copied values
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        final Cell copy = super.copy();

        if (condition != null) {
            copy.condition = condition.copy();
        }

        return (T) copy;
    }

    /**
     * Returns a string representation of this Cell using the specified naming policy.
     * The output format is: OPERATOR condition_string
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Cell cell = new Cell(Operator.NOT, Filters.eq("userName", "John"));
     * String str = cell.toString(NamingPolicy.LOWER_CAMEL_CASE);
     * // Returns: "NOT userName = 'John'"
     * 
     * Cell exists = new Cell(Operator.EXISTS, subQuery);
     * String str2 = exists.toString(NamingPolicy.NO_CHANGE);
     * // Returns: "EXISTS (SELECT ...)"
     * }</pre>
     * 
     * @param namingPolicy the naming policy to apply to property names
     * @return a string representation of this Cell
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return getOperator().toString() + ((condition == null) ? Strings.EMPTY : SK._SPACE + condition.toString(namingPolicy));
    }

    /**
     * Returns the hash code of this Cell.
     * The hash code is computed based on the operator and wrapped condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Cell cell1 = new Cell(Operator.NOT, Filters.eq("status", "active"));
     * Cell cell2 = new Cell(Operator.NOT, Filters.eq("status", "active"));
     * boolean sameHash = cell1.hashCode() == cell2.hashCode();  // true
     * }</pre>
     * 
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + ((condition == null) ? 0 : condition.hashCode());
    }

    /**
     * Checks if this Cell is equal to another object.
     * Two Cells are equal if they have the same operator and wrapped condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Cell cell1 = new Cell(Operator.NOT, Filters.eq("status", "active"));
     * Cell cell2 = new Cell(Operator.NOT, Filters.eq("status", "active"));
     * boolean isEqual = cell1.equals(cell2);  // Returns true
     * 
     * Cell cell3 = new Cell(Operator.EXISTS, Filters.eq("status", "active"));
     * boolean isNotEqual = cell1.equals(cell3);  // Returns false (different operator)
     * }</pre>
     * 
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Cell other) {
            return N.equals(operator, other.operator) && N.equals(condition, other.condition);
        }

        return false;
    }
}
