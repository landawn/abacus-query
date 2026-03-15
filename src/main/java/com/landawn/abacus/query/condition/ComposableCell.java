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

/**
 * A composable variant of {@link Cell} that supports logical composition via AND/OR/NOT operations.
 * Like Cell, it wraps another condition with an operator, but extends {@link ComposableCondition}
 * instead of {@link AbstractCondition}, enabling chaining with other conditions.
 *
 * <p>Concrete subclasses include {@link Not}, {@link Exists}, {@link NotExists}, {@link All}, {@link Any}, and {@link Some}.</p>
 *
 * @see Cell
 * @see ComposableCondition
 * @see Not
 * @see Exists
 * @see NotExists
 */
public abstract class ComposableCell extends ComposableCondition {

    private Condition condition;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * Creates an uninitialized ComposableCell instance; not for direct application use.
     */
    ComposableCell() {
    }

    /**
     * Creates a new ComposableCell with the specified operator and condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Typically used via subclass constructors:
     * SubQuery subQuery = new SubQuery("SELECT 1 FROM orders WHERE user_id = users.id");
     * Exists exists = new Exists(subQuery);   // Exists extends ComposableCell
     * }</pre>
     *
     * @param operator the operator to apply to the condition
     * @param cond the condition to wrap (must not be null)
     */
    public ComposableCell(final Operator operator, final Condition cond) {
        super(operator);
        this.condition = N.checkArgNotNull(cond, "cond");
    }

    /**
     * Gets the wrapped condition, cast to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition eq = Filters.equal("status", "active");
     * Not notCond = new Not(eq);
     * Condition inner = notCond.getCondition();   // Returns the Equal condition
     * }</pre>
     *
     * @param <T> the type of condition to return
     * @return the wrapped condition
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
     * Converts this ComposableCell to its string representation using the specified naming policy.
     * The output format is: OPERATOR condition_string
     *
     * @param namingPolicy the naming policy to apply to property names
     * @return a string representation of this ComposableCell
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.NO_CHANGE : namingPolicy;
        final Condition condition = getCondition();
        final String conditionString = condition == null ? "" : condition.toString(effectiveNamingPolicy);
        return operator().toString() + SK._SPACE + SK._PARENTHESIS_L + conditionString + SK._PARENTHESIS_R;
    }

    /**
     * Returns the hash code of this ComposableCell, based on the operator and wrapped condition.
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
     * Checks if this ComposableCell is equal to another object.
     * Two ComposableCells are equal if they have the same operator and wrapped condition.
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

        final ComposableCell other = (ComposableCell) obj;
        return N.equals(operator, other.operator) && N.equals(condition, other.condition);
    }
}
