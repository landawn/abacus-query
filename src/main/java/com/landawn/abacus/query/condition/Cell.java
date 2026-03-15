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

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
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
 * <p>Concrete subclasses include {@link On}, {@link Using}, and {@link Clause} (and its subclasses).</p>
 *
 * @see AbstractCondition
 * @see ComposableCell
 * @see Clause
 * @see Operator
 */
public abstract class Cell extends AbstractCondition {

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
     * // Typically used via subclass constructors:
     * SubQuery subQuery = new SubQuery("SELECT MAX(salary) FROM employees");
     * On onCond = new On(Filters.equal("a.id", "b.id"));   // On extends Cell
     * }</pre>
     *
     * @param operator the operator to apply to the condition
     * @param cond the condition to wrap (must not be null)
     */
    public Cell(final Operator operator, final Condition cond) {
        super(operator);
        this.condition = N.checkArgNotNull(cond, "cond");
    }

    /**
     * Gets the wrapped condition.
     * The returned condition can be cast to its specific type if needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition eq = Filters.equal("a.id", "b.id");
     * On onCond = new On(eq);
     * Condition inner = onCond.getCondition();   // Returns the Equal condition
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
     * Gets the parameters from the wrapped condition.
     * This method delegates to the wrapped condition's getParameters method.
     * 
     * @return an immutable list of parameters from the wrapped condition, or an empty immutable list if no condition is set
     */
    @Override
    public ImmutableList<Object> getParameters() {
        return (condition == null) ? ImmutableList.empty() : condition.getParameters();
    }

    /**
     * Converts this Cell condition to its string representation using the specified naming policy.
     * The output format is: OPERATOR condition_string
     * 
     * @param namingPolicy the naming policy to apply to property names
     * @return a string representation of this Cell
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        return operator().toString() + ((condition == null) ? Strings.EMPTY : SK._SPACE + condition.toString(namingPolicy));
    }

    /**
     * Returns the hash code of this Cell.
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
     * Checks if this Cell is equal to another object.
     * Two Cells are equal if they have the same operator and wrapped condition.
     * 
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Cell other = (Cell) obj;
        return N.equals(operator, other.operator) && N.equals(condition, other.condition);
    }
}
