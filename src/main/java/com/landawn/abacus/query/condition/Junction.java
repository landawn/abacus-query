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

import static com.landawn.abacus.query.SK._PARENTHESIS_L;
import static com.landawn.abacus.query.SK._PARENTHESIS_R;
import static com.landawn.abacus.query.SK._SPACE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;

/**
 * Base class for logical junction conditions that combine multiple conditions.
 * This class is used to join multiple conditions using logical operators like AND or OR.
 * It provides the foundation for creating complex query conditions by combining simpler ones.
 * Junctions can be nested to create arbitrarily complex logical expressions.
 * 
 * <p>Important: Junction must not contain clause conditions (WHERE, ORDER BY, etc.) - 
 * those are handled by {@link Criteria}. This class is specifically for logical
 * combinations of conditional expressions.
 * 
 * <p>This class serves as the parent for specific junction types:
 * <ul>
 *   <li>{@link And} - combines conditions with logical AND (all must be true)</li>
 *   <li>{@link Or} - combines conditions with logical OR (at least one must be true)</li>
 * </ul>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Dynamic condition management (add, remove, clear)</li>
 *   <li>Deep copying support for safe condition reuse</li>
 *   <li>Automatic parentheses handling for correct precedence</li>
 *   <li>Parameter collection from all nested conditions</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create an AND junction
 * Junction and = new Junction(Operator.AND, 
 *     new Equal("status", "active"),
 *     new GreaterThan("age", 18),
 *     new LessThan("age", 65)
 * );
 * // Generates: ((status = 'active') AND (age > 18) AND (age < 65))
 * 
 * // Create an OR junction
 * Junction or = new Junction(Operator.OR,
 *     new Equal("city", "New York"),
 *     new Equal("city", "Los Angeles"),
 *     new Equal("city", "Chicago")
 * );
 * // Generates: ((city = 'New York') OR (city = 'Los Angeles') OR (city = 'Chicago'))
 * 
 * // Nested junctions for complex logic
 * Junction complex = new Junction(Operator.AND,
 *     new Equal("type", "premium"),
 *     new Junction(Operator.OR,
 *         new GreaterThan("balance", 10000),
 *         new Equal("vip_status", true)
 *     )
 * );
 * }</pre>
 * 
 * @see And
 * @see Or
 * @see Criteria
 * @see AbstractCondition
 */
public class Junction extends LogicalCondition {

    List<Condition> conditions;

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Junction instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Junction() {
        conditions = new ArrayList<>();
    }

    /**
     * Creates a new Junction with the specified operator and conditions.
     * This constructor initializes the junction with a set of conditions that will
     * be combined using the specified logical operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an AND junction with multiple conditions
     * Junction activeAdults = new Junction(Operator.AND,
     *     new Equal("active", true),
     *     new GreaterThanOrEqual("age", 18),
     *     new IsNotNull("email")
     * );
     *
     * // Create an OR junction for status checks
     * Junction validStatus = new Junction(Operator.OR,
     *     new Equal("status", "approved"),
     *     new Equal("status", "pending_review"),
     *     new Equal("override", true)
     * );
     * }</pre>
     *
     * @param operator the logical operator to use (AND, OR, etc.). Must not be null.
     * @param conditions the conditions to combine. Can be empty but not null.
     * @throws IllegalArgumentException if any condition in the array is null
     */
    public Junction(final Operator operator, final Condition... conditions) {
        super(operator);
        this.conditions = new ArrayList<>();
        add(conditions);
    }

    /**
     * Creates a new Junction with the specified operator and collection of conditions.
     * This constructor is useful when conditions are already collected in a list or set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create conditions dynamically
     * List<Condition> conditions = new ArrayList<>();
     * conditions.add(new Equal("status", "active"));
     * conditions.add(new GreaterThan("score", 80));
     * if (includeDateCheck) {
     *     conditions.add(new LessThanOrEqual("date", today));
     * }
     *
     * Junction junction = new Junction(Operator.AND, conditions);
     * }</pre>
     *
     * @param operator the logical operator to use (AND, OR, etc.). Must not be null.
     * @param conditions the collection of conditions to combine. Can be empty but not null.
     * @throws IllegalArgumentException if any condition in the collection is null
     */
    public Junction(final Operator operator, final Collection<? extends Condition> conditions) {
        super(operator);
        this.conditions = new ArrayList<>();
        add(conditions); // NOSONAR
    }

    /**
     * Gets the list of conditions contained in this junction.
     * The returned list is the internal representation and modifications to it
     * will affect the junction. Use with caution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction and = new Junction(Operator.AND,
     *     new Equal("status", "active"),
     *     new GreaterThan("age", 18));
     * List<Condition> conditions = and.getConditions();
     * // Returns: [Equal("status", "active"), GreaterThan("age", 18)]
     * int count = conditions.size();
     * // Returns: 2
     * }</pre>
     *
     * @return an unmodifiable view of the list of conditions in this junction.
     */
    public List<Condition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    /**
     * Replaces all conditions in this junction with the specified conditions.
     * This method clears existing conditions before adding the new ones.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction junction = new Junction(Operator.AND,
     *     new Equal("status", "active"));
     *
     * // Replace all conditions with new ones
     * junction.set(
     *     new Equal("status", "pending"),
     *     new GreaterThan("priority", 5),
     *     new IsNotNull("assignee")
     * );
     * // Junction now contains only the three new conditions
     * }</pre>
     *
     * @param conditions the new conditions to set. Existing conditions will be cleared.
     * @throws IllegalArgumentException if conditions array contains null elements
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     */
    @Deprecated
    public final void set(final Condition... conditions) {
        if (N.notEmpty(conditions)) {
            for (final Condition condition : conditions) {
                if (condition == null) {
                    throw new IllegalArgumentException("Condition cannot be null");
                }
            }
        }

        this.conditions.clear();
        add(conditions);
    }

    /**
     * Replaces all conditions in this junction with the specified collection of conditions.
     * This method clears existing conditions before adding the new ones.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction junction = new Junction(Operator.OR);
     *
     * // Build conditions dynamically
     * List<Condition> newConditions = new ArrayList<>();
     * newConditions.add(new Equal("region", "US"));
     * newConditions.add(new Equal("region", "EU"));
     * newConditions.add(new Equal("region", "APAC"));
     *
     * // Replace all existing conditions with the new collection
     * junction.set(newConditions);
     * // Junction now contains: region = 'US' OR region = 'EU' OR region = 'APAC'
     * }</pre>
     *
     * @param conditions the new collection of conditions to set. Existing conditions will be cleared.
     * @throws IllegalArgumentException if conditions collection contains null elements
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     */
    @Deprecated
    public void set(final Collection<? extends Condition> conditions) {
        if (N.notEmpty(conditions)) {
            for (final Condition condition : conditions) {
                if (condition == null) {
                    throw new IllegalArgumentException("Condition cannot be null");
                }
            }
        }

        this.conditions.clear();
        add(conditions);
    }

    /**
     * Adds the specified conditions to this junction.
     * The conditions are appended to the existing list of conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction junction = new Junction(Operator.AND);
     *
     * // Add initial conditions
     * junction.add(
     *     new Equal("status", "active"),
     *     new GreaterThan("score", 0)
     * );
     *
     * // Add more conditions later
     * junction.add(
     *     new LessThan("price", 100),
     *     new Equal("inStock", true)
     * );
     * }</pre>
     *
     * @param conditions the conditions to add
     * @throws IllegalArgumentException if conditions array contains null elements
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     */
    @Deprecated
    public final void add(final Condition... conditions) {
        if (N.isEmpty(conditions)) {
            return;
        }

        for (final Condition condition : conditions) {
            if (condition == null) {
                throw new IllegalArgumentException("Condition cannot be null");
            }
        }

        this.conditions.addAll(Arrays.asList(conditions));
    }

    /**
     * Adds the specified collection of conditions to this junction.
     * The conditions are appended to the existing list of conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction junction = new Junction(Operator.OR);
     *
     * // Add conditions from another source
     * List<Condition> userConditions = getUserDefinedConditions();
     * junction.add(userConditions);
     * }</pre>
     *
     * @param conditions the collection of conditions to add
     * @throws IllegalArgumentException if conditions contains null elements
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     */
    @Deprecated
    public void add(final Collection<? extends Condition> conditions) {
        if (N.isEmpty(conditions)) {
            return;
        }

        for (final Condition condition : conditions) {
            if (condition == null) {
                throw new IllegalArgumentException("Condition cannot be null");
            }
        }

        this.conditions.addAll(conditions);
    }

    /**
     * Removes the specified conditions from this junction.
     * Only exact object matches are removed, not logically equivalent conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction junction = new Junction(Operator.AND);
     * Condition cond1 = new Equal("status", "active");
     * Condition cond2 = new Equal("type", "premium");
     * junction.add(cond1, cond2);
     *
     * // Remove specific condition
     * junction.remove(cond1);
     * }</pre>
     *
     * @param conditions the conditions to remove
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Instead of modifying an existing junction, create a new junction with the desired conditions.
     *             For example, use {@code new Junction(operator, desiredConditions)} instead of
     *             {@code junction.remove(unwantedConditions)}.
     */
    @Deprecated
    public final void remove(final Condition... conditions) {
        if (N.isEmpty(conditions)) {
            return;
        }

        for (final Condition cond : conditions) {
            this.conditions.remove(cond);
        }
    }

    /**
     * Removes the specified collection of conditions from this junction.
     * Only exact object matches are removed, not logically equivalent conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction junction = new Junction(Operator.AND);
     * List<Condition> toRemove = getObsoleteConditions();
     * junction.remove(toRemove);
     * }</pre>
     *
     * @param conditions the collection of conditions to remove
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Instead of modifying an existing junction, create a new junction with the desired conditions.
     *             For example, use {@code new Junction(operator, desiredConditions)} instead of
     *             {@code junction.remove(unwantedConditions)}.
     */
    @Deprecated
    public void remove(final Collection<? extends Condition> conditions) {
        if (N.isEmpty(conditions)) {
            return;
        }

        this.conditions.removeAll(conditions);
    }

    /**
     * Removes all conditions from this junction.
     * After this operation, the junction will be empty but can still accept new conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction junction = new Junction(Operator.AND,
     *     new Equal("status", "active"),
     *     new GreaterThan("age", 18));
     *
     * // Remove all conditions
     * junction.clear();
     * // junction.getConditions().size() returns 0
     *
     * // Junction can still accept new conditions after clearing
     * junction.add(new Equal("role", "admin"));
     * }</pre>
     *
     * @deprecated Condition should be immutable except using {@code clearParameters()} to release resources.
     *             Instead of modifying an existing junction, create a new junction with the desired conditions.
     */
    @Deprecated
    public void clear() {
        conditions.clear();
    }

    /**
     * Gets all parameters from all conditions in this junction.
     * This method recursively collects parameters from all nested conditions,
     * including those in nested junctions. The order of parameters matches
     * the order they would appear in the generated SQL.
     * 
     * @return a list containing all parameters from all conditions
     */
    @Override
    public List<Object> getParameters() {
        final List<Object> parameters = new ArrayList<>();

        for (final Condition condition : conditions) {
            if (condition != null) {
                parameters.addAll(condition.getParameters());
            }
        }

        return ImmutableList.wrap(parameters);
    }

    /**
     * Clears parameters in all child conditions by recursively calling clearParameters() on each.
     * This method delegates the clearing operation to each contained condition in the junction.
     *
     * <p>Use this method to release large objects held by any condition in the junction tree
     * when the junction is no longer needed.</p>
     *
     */
    @Override
    public void clearParameters() {
        for (final Condition condition : conditions) {
            if (condition != null) {
                condition.clearParameters();
            }
        }
    }

    /**
     * Creates a deep copy of this junction including all its conditions.
     * The copy includes copies of all contained conditions, ensuring that
     * modifications to the copy don't affect the original.
     * 
     * @param <T> the type of the condition
     * @return a new Junction instance with copies of all conditions
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Condition> T copy() {
        final Junction result = super.copy();

        result.conditions = new ArrayList<>();

        for (final Condition cond : conditions) {
            if (cond != null) {
                result.conditions.add(cond.copy());
            }
        }

        return (T) result;
    }

    /**
     * Converts this junction to its string representation according to the specified naming policy.
     * The output format wraps each condition in parentheses and joins them with the operator.
     * This ensures proper precedence in complex logical expressions.
     * 
     * @param namingPolicy the naming policy to apply to property names
     * @return the string representation with proper parentheses and spacing
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (N.isEmpty(conditions)) {
            return Strings.EMPTY;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(_PARENTHESIS_L);

            boolean isFirst = true;
            for (final Condition condition : conditions) {
                if (condition == null) {
                    continue;
                }

                if (!isFirst) {
                    sb.append(_SPACE);
                    sb.append(operator().toString());
                    sb.append(_SPACE);
                }

                sb.append(_PARENTHESIS_L);
                sb.append(condition.toString(namingPolicy));
                sb.append(_PARENTHESIS_R);

                isFirst = false;
            }

            sb.append(_PARENTHESIS_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Computes the hash code for this junction based on its operator and conditions.
     * The hash code is consistent with equals() - junctions with the same operator
     * and conditions will have the same hash code.
     * 
     * @return hash code based on operator and condition list
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((operator == null) ? 0 : operator.hashCode());
        return (h * 31) + conditions.hashCode();
    }

    /**
     * Checks if this junction is equal to another object.
     * Two junctions are considered equal if they have the same operator
     * and contain the same conditions in the same order.
     * 
     * @param obj the object to compare with
     * @return {@code true} if the object is a Junction with the same operator and conditions
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Junction other) {
            return N.equals(operator, other.operator) && N.equals(conditions, other.conditions);

        }

        return false;
    }
}
