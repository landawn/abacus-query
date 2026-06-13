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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;

/**
 * Represents a composable OR condition that combines multiple conditions.
 * The OR condition evaluates to true if at least one of its child conditions evaluates to true.
 * 
 * <p>This class extends {@link Junction} and provides a fluent API for building complex OR conditions.
 * The OR operator follows standard SQL evaluation rules where the entire expression is true if
 * any single condition is true.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Returns true if ANY child condition is true</li>
 *   <li>Returns false only if ALL child conditions are false</li>
 *   <li>Supports unlimited number of child conditions</li>
 *   <li>Can be nested with other composable operators (AND, NOT)</li>
 *   <li>Whether the database short-circuits evaluation is engine-dependent</li>
 * </ul>
 *
 * <p>Relationship to other composable operators:</p>
 * <ul>
 *   <li>OR requires at least one condition to be true</li>
 *   <li>AND requires all conditions to be true</li>
 *   <li>NOT negates a condition</li>
 *   <li>XOR is available via the {@link ComposableCondition#xor(Condition)} method, requiring exactly one of two conditions to be true</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create OR with multiple conditions
 * Or or = new Or(
 *     Filters.equal("status", "active"),
 *     Filters.equal("status", "pending"),
 *     Filters.equal("status", "review")
 * );
 * // SQL: ((status = 'active') OR (status = 'pending') OR (status = 'review'))
 *
 * // Build OR condition fluently
 * Or or2 = new Or(Filters.greaterThan("age", 65))
 *     .or(Filters.lessThan("age", 18));
 * // SQL: ((age > 65) OR (age < 18))
 * }</pre>
 *
 * @see Junction
 * @see And
 * @see Not
 * @see Condition
 */
public class Or extends Junction {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor creates an uninitialized Or instance and should not be used
     * directly in application code. It exists solely for serialization/deserialization purposes.
     */
    Or() {
    }

    /**
     * Creates a new OR condition with the specified conditions.
     * All provided conditions will be combined using the OR operator.
     *
     * <p>The OR expression is true if any single child condition is true. Evaluation
     * order at the database level depends on the SQL engine. This constructor accepts
     * a variable number of conditions for convenience.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find users in specific cities
     * Or or = new Or(
     *     Filters.equal("city", "New York"),
     *     Filters.equal("city", "Los Angeles"),
     *     Filters.equal("city", "Chicago")
     * );
     * // SQL: ((city = 'New York') OR (city = 'Los Angeles') OR (city = 'Chicago'))
     *
     * // Complex OR with different condition types
     * Or complexOr = new Or(
     *     Filters.like("email", "%@gmail.com"),
     *     Filters.like("email", "%@yahoo.com"),
     *     Filters.isNull("email")
     * );
     * // SQL: ((email LIKE '%@gmail.com') OR (email LIKE '%@yahoo.com') OR (email IS NULL))
     * }</pre>
     *
     * @param conditions the conditions to combine with OR logic; may be {@code null} or empty
     * @throws IllegalArgumentException if any element in {@code conditions} is {@code null}, or if any
     *             element is a {@link Criteria}, has a clause operator (WHERE, JOIN variants, ORDER_BY, etc.),
     *             is an {@code ON}/{@code USING} condition that is not an {@link On} instance, is an
     *             {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand, or is an empty predicate
     *             (a blank {@link Expression} or empty {@link Junction})
     */
    public Or(final Condition... conditions) {
        super(Operator.OR, conditions);
    }

    /**
     * Creates a new OR condition with a collection of conditions.
     * This constructor is useful when conditions are built dynamically.
     *
     * <p>All conditions in the collection will be combined using the OR operator.
     * The collection is copied internally so that subsequent changes to the original
     * collection do not affect this condition.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Dynamic condition building
     * List<Condition> conditions = new ArrayList<>();
     * for (String name : searchNames) {
     *     conditions.add(Filters.like("name", "%" + name + "%"));
     * }
     * Or or = new Or(conditions);
     * // SQL: ((name LIKE '%name1%') OR (name LIKE '%name2%') OR ...)
     *
     * // Combining existing conditions (use an ordered collection to preserve iteration order)
     * List<Condition> statusConditions = new ArrayList<>();
     * statusConditions.add(Filters.equal("status", "active"));
     * statusConditions.add(Filters.equal("status", "pending"));
     * Or statusOr = new Or(statusConditions);
     * // SQL: ((status = 'active') OR (status = 'pending'))
     * }</pre>
     *
     * @param conditions the collection of conditions to combine with OR logic; may be {@code null} or empty
     * @throws IllegalArgumentException if any element in {@code conditions} is {@code null}, or if any
     *             element is a {@link Criteria}, has a clause operator (WHERE, JOIN variants, ORDER_BY, etc.),
     *             is an {@code ON}/{@code USING} condition that is not an {@link On} instance, is an
     *             {@code ANY}/{@code ALL}/{@code SOME} quantified-subquery operand, or is an empty predicate
     *             (a blank {@link Expression} or empty {@link Junction})
     */
    public Or(final Collection<? extends Condition> conditions) {
        super(Operator.OR, conditions);
    }

    /**
     * Trusted private constructor used by fluent chaining ({@link #or(Condition)}) to avoid
     * re-validating already-validated conditions. Behavior is identical to the public
     * collection constructor; only the redundant per-element null check is skipped.
     * The supplied list is taken over directly (not copied), so callers must pass a private,
     * freshly created list that they will not retain or mutate afterwards.
     *
     * @param validatedConditions an already-validated, freshly created list of conditions
     * @param marker disambiguation marker (ignored)
     */
    private Or(final List<? extends Condition> validatedConditions, final boolean marker) {
        super(Operator.OR, validatedConditions, marker);
    }

    /**
     * Creates a new Or condition by adding another condition to this OR.
     * This method returns a new Or instance containing all existing conditions plus the new one.
     *
     * <p>This method provides a fluent interface for building OR conditions incrementally.
     * Each call returns a new OR instance, preserving immutability. The new condition
     * is added to the end of the existing conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Build condition step by step
     * Or or = new Or(Filters.equal("type", "A"))
     *     .or(Filters.equal("type", "B"))
     *     .or(Filters.equal("type", "C"));
     * // SQL: ((type = 'A') OR (type = 'B') OR (type = 'C'))
     *
     * // Add conditions conditionally
     * Or baseOr = new Or(Filters.equal("status", "active"));
     * if (includeInactive) {
     *     baseOr = baseOr.or(Filters.equal("status", "inactive"));
     * }
     * if (includePending) {
     *     baseOr = baseOr.or(Filters.equal("status", "pending"));
     * }
     * // Results vary based on flags
     * }</pre>
     *
     * @param cond the condition to add to this OR. Must not be {@code null} and must be
     *             composable (i.e. not a {@link Clause}, a {@link Join}, or an {@code ON}/{@code USING} condition).
     * @return a new Or condition containing all existing conditions plus the new one
     * @throws IllegalArgumentException if {@code cond} is {@code null}, or if {@code cond}
     *             is a non-composable condition (a {@link Clause}, a {@link Join}, or an {@code ON}/{@code USING} condition)
     */
    @Override
    public Or or(final Condition cond) {
        N.checkArgNotNull(cond, "cond");
        validateComposableOperand(cond, "or");

        final List<Condition> conditionList = new ArrayList<>(this.conditions.size() + 1);

        conditionList.addAll(this.conditions);

        conditionList.add(cond);

        return new Or(conditionList, true);
    }
}
