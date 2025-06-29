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

import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Represents a condition cell that wraps another condition with an operator.
 * This class serves as a container for a condition paired with a specific operator,
 * typically used for unary operations or clauses that modify other conditions.
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create a NOT cell
 * Cell notCell = new Cell(Operator.NOT, CF.eq("status", "inactive"));
 * 
 * // Create an EXISTS cell with a subquery
 * Cell existsCell = new Cell(Operator.EXISTS, subQuery);
 * }</pre>
 * 
 * @see AbstractCondition
 * @see Condition
 * @see Operator
 */
public class Cell extends AbstractCondition {

    private Condition condition;

    // For Kryo
    Cell() {
    }

    /**
     * Creates a new Cell with the specified operator and condition.
     * 
     * @param operator the operator to apply to the condition
     * @param condition the condition to wrap
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Cell cell = new Cell(Operator.NOT, CF.isNull("email"));
     * }</pre>
     */
    public Cell(final Operator operator, final Condition condition) {
        super(operator);
        this.condition = condition;
    }

    /**
     * Gets the wrapped condition.
     * 
     * @param <T> the type of condition to return
     * @return the wrapped condition, cast to the specified type
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Cell cell = new Cell(Operator.NOT, CF.eq("status", "active"));
     * Equal eq = cell.getCondition(); // Returns the Equal condition
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public <T extends Condition> T getCondition() {
        return (T) condition;
    }

    /**
     * Sets the wrapped condition.
     * 
     * @param condition the new condition to wrap
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    /**
     * Gets the parameters from the wrapped condition.
     * 
     * @return a list of parameters from the wrapped condition, or an empty list if no condition is set
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Cell cell = new Cell(Operator.NOT, CF.eq("name", "John"));
     * List<Object> params = cell.getParameters(); // Returns ["John"]
     * }</pre>
     */
    @Override
    public List<Object> getParameters() {
        return (condition == null) ? N.emptyList() : condition.getParameters();
    }

    /**
     * Clears the parameters of the wrapped condition.
     * This method delegates to the wrapped condition's clearParameters method.
     */
    @Override
    public void clearParameters() {
        if (condition != null) {
            condition.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this Cell.
     * The wrapped condition is also copied if present.
     * 
     * @param <T> the type of condition to return
     * @return a new Cell instance with copied values
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Cell original = new Cell(Operator.NOT, CF.eq("status", "active"));
     * Cell copy = original.copy();
     * // copy is a deep copy of original
     * }</pre>
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
     * 
     * @param namingPolicy the naming policy to apply to property names
     * @return a string representation of this Cell
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Cell cell = new Cell(Operator.NOT, CF.eq("userName", "John"));
     * String str = cell.toString(NamingPolicy.LOWER_CAMEL_CASE);
     * // Returns: "NOT userName = 'John'"
     * }</pre>
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return getOperator().toString() + ((condition == null) ? Strings.EMPTY : WD._SPACE + condition.toString(namingPolicy));
    }

    /**
     * Returns the hash code of this Cell.
     * The hash code is computed based on the operator and wrapped condition.
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
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Cell cell1 = new Cell(Operator.NOT, CF.eq("status", "active"));
     * Cell cell2 = new Cell(Operator.NOT, CF.eq("status", "active"));
     * boolean isEqual = cell1.equals(cell2); // Returns true
     * }</pre>
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