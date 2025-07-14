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

import static com.landawn.abacus.query.SK._PARENTHESES_L;
import static com.landawn.abacus.query.SK._PARENTHESES_R;
import static com.landawn.abacus.query.SK._SPACE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * Base class for logical junction conditions that combine multiple conditions.
 * This class is used to join multiple conditions using logical operators like AND or OR.
 * It must not contain clause conditions (WHERE, ORDER BY, etc.) - those are handled by {@link Criteria}.
 * 
 * <p>This class serves as the parent for specific junction types:
 * <ul>
 *   <li>{@link And} - combines conditions with logical AND</li>
 *   <li>{@link Or} - combines conditions with logical OR</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Create an AND junction
 * Junction and = new Junction(Operator.AND, 
 *     new Equal("status", "active"),
 *     new GreaterThan("age", 18)
 * );
 * // Generates: ((status = 'active') AND (age > 18))
 * 
 * // Create an OR junction
 * Junction or = new Junction(Operator.OR,
 *     new Equal("city", "NYC"),
 *     new Equal("city", "LA")
 * );
 * // Generates: ((city = 'NYC') OR (city = 'LA'))
 * }</pre>
 * 
 * @see And
 * @see Or
 * @see Criteria
 */
public class Junction extends AbstractCondition {

    List<Condition> conditionList;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    Junction() {
    }

    /**
     * Creates a new Junction with the specified operator and conditions.
     *
     * @param operator the logical operator to use (AND, OR, etc.). Must not be null.
     * @param conditions the conditions to combine. Can be empty but not null.
     * 
     * <p>Example:
     * <pre>{@code
     * Junction junction = new Junction(Operator.AND,
     *     new Equal("active", true),
     *     new NotNull("email")
     * );
     * }</pre>
     */
    public Junction(final Operator operator, final Condition... conditions) {
        super(operator);
        conditionList = new ArrayList<>();
        add(conditions);
    }

    /**
     * Creates a new Junction with the specified operator and collection of conditions.
     *
     * @param operator the logical operator to use (AND, OR, etc.). Must not be null.
     * @param conditions the collection of conditions to combine. Can be empty but not null.
     * 
     * <p>Example:
     * <pre>{@code
     * List<Condition> conditions = Arrays.asList(
     *     new Equal("status", "active"),
     *     new GreaterThan("score", 80)
     * );
     * Junction junction = new Junction(Operator.AND, conditions);
     * }</pre>
     */
    public Junction(final Operator operator, final Collection<? extends Condition> conditions) {
        super(operator);
        conditionList = new ArrayList<>();
        add(conditions); // NOSONAR
    }

    /**
     * Gets the list of conditions contained in this junction.
     *
     * @return the list of conditions. Modifications to this list will affect the junction.
     */
    public List<Condition> getConditions() {
        return conditionList;
    }

    /**
     * Replaces all conditions in this junction with the specified conditions.
     *
     * @param conditions the new conditions to set. Existing conditions will be cleared.
     * 
     * <p>Example:
     * <pre>{@code
     * junction.set(
     *     new Equal("status", "active"),
     *     new NotNull("email")
     * );
     * }</pre>
     */
    public final void set(final Condition... conditions) {
        conditionList.clear();
        add(conditions);
    }

    /**
     * Replaces all conditions in this junction with the specified collection of conditions.
     *
     * @param conditions the new collection of conditions to set. Existing conditions will be cleared.
     */
    public void set(final Collection<? extends Condition> conditions) {
        conditionList.clear();
        add(conditions);
    }

    /**
     * Adds the specified conditions to this junction.
     *
     * @param conditions the conditions to add
     * 
     * <p>Example:
     * <pre>{@code
     * junction.add(
     *     new LessThan("price", 100),
     *     new Equal("inStock", true)
     * );
     * }</pre>
     */
    public final void add(final Condition... conditions) {
        conditionList.addAll(Arrays.asList(conditions));
    }

    /**
     * Adds the specified collection of conditions to this junction.
     *
     * @param conditions the collection of conditions to add
     */
    public void add(final Collection<? extends Condition> conditions) {
        conditionList.addAll(conditions);
    }

    /**
     * Removes the specified conditions from this junction.
     *
     * @param conditions the conditions to remove
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public final void remove(final Condition... conditions) {
        for (final Condition cond : conditions) {
            conditionList.remove(cond);
        }
    }

    /**
     * Removes the specified collection of conditions from this junction.
     *
     * @param conditions the collection of conditions to remove
     * @deprecated Condition should be immutable except using {@code clearParameter()} to release resources.
     */
    @Deprecated
    public void remove(final Collection<? extends Condition> conditions) {
        conditionList.removeAll(conditions);
    }

    /**
     * Removes all conditions from this junction.
     */
    public void clear() {
        conditionList.clear();
    }

    /**
     * Gets all parameters from all conditions in this junction.
     * This method recursively collects parameters from all nested conditions.
     *
     * @return a list containing all parameters from all conditions
     */
    @Override
    public List<Object> getParameters() {
        final List<Object> parameters = new ArrayList<>();

        for (final Condition condition : conditionList) {
            parameters.addAll(condition.getParameters());
        }

        return parameters;
    }

    /**
     * Clears parameters from all conditions in this junction.
     * This method recursively clears parameters from all nested conditions.
     */
    @Override
    public void clearParameters() {
        for (final Condition condition : conditionList) {
            condition.clearParameters();
        }
    }

    /**
     * Creates a deep copy of this junction including all its conditions.
     *
     * @param <T> the type of the condition
     * @return a new Junction instance with copies of all conditions
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Condition> T copy() {
        final Junction result = super.copy();

        result.conditionList = new ArrayList<>();

        for (final Condition cond : conditionList) {
            result.conditionList.add(cond.copy());
        }

        return (T) result;
    }

    /**
     * Converts this junction to its string representation according to the specified naming policy.
     * The output format wraps each condition in parentheses and joins them with the operator.
     *
     * @param namingPolicy the naming policy to apply to property names
     * @return the string representation, e.g., "((age > 18) AND (status = 'active'))"
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (N.isEmpty(conditionList)) {
            return Strings.EMPTY;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(_PARENTHESES_L);

            for (int i = 0; i < conditionList.size(); i++) {
                if (i > 0) {
                    sb.append(_SPACE);
                    sb.append(getOperator().toString());
                    sb.append(_SPACE);
                }

                sb.append(_PARENTHESES_L);
                sb.append(conditionList.get(i).toString(namingPolicy));
                sb.append(_PARENTHESES_R);
            }

            sb.append(_PARENTHESES_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Computes the hash code for this junction based on its operator and conditions.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + operator.hashCode();
        return (h * 31) + conditionList.hashCode();
    }

    /**
     * Checks if this junction is equal to another object.
     *
     * @param obj the object to compare with
     * @return true if the object is a Junction with the same operator and conditions
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Junction other) {
            return N.equals(operator, other.operator) && N.equals(conditionList, other.conditionList);

        }

        return false;
    }
}