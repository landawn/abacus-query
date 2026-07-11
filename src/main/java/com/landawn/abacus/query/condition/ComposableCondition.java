/*
 * Copyright (C) 2020 HaiYang Li
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

import com.landawn.abacus.util.N;

/**
 * A {@link Condition} that supports logical composition via {@code and()}, {@code or()},
 * {@code not()}, and {@code xor()}.
 *
 * <p>Not every {@code Condition} can be logically combined — for example, SQL clauses
 * ({@link Clause}: WHERE, ORDER BY, …) and {@link Join} operations are structural
 * query components that do not participate in boolean logic. This abstract class marks the
 * conditions that generally support logical composition and provides the four operations.</p>
 *
 * <p><b>&#9888;&#65039;</b> Quantified operands ({@link All}, {@link Any}, and {@link Some}) inherit these methods
 * for type compatibility but are structural right-hand operands and are rejected when composed directly.</p>
 *
 * <p>Typical implementors include comparison conditions ({@link Binary} and subclasses),
 * range conditions ({@link AbstractBetween}), collection conditions ({@link AbstractIn}),
 * junction conditions ({@link Junction}, {@link And}, {@link Or}), and composable-cell
 * conditions ({@link ComposableCell} and its subclasses such as {@link Not}, {@link Exists}).</p>
 *
 * @see Condition
 * @see And
 * @see Or
 * @see Not
 */
public abstract class ComposableCondition extends AbstractCondition {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * Creates an uninitialized ComposableCondition instance; not for direct application use.
     */
    ComposableCondition() {
        super();
    }

    /**
     * Creates a new ComposableCondition with the specified operator.
     *
     * @param operator the logical operator for this condition (must not be {@code null})
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    protected ComposableCondition(final Operator operator) {
        super(operator);
    }

    /**
     * Creates a new NOT condition that negates this condition.
     * The result is true when this condition is false, and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition active = Filters.equal("status", "active");
     * Not notActive = ((ComposableCondition) active).not();
     * // SQL: NOT (status = 'active')
     * }</pre>
     *
     * @return a new {@link Not} condition wrapping this condition
     * @throws IllegalArgumentException if this condition is non-composable — a {@link Criteria}, a SQL clause,
     *                                  an {@code ON}/{@code USING} connector, an {@code ANY}/{@code ALL}/{@code SOME}
     *                                  quantified-subquery operand, or an empty predicate (a blank {@link Expression}
     *                                  or empty {@link Junction})
     */
    public Not not() {
        validateComposableOperand(this, "not");
        return new Not(this);
    }

    /**
     * Creates a new AND condition combining this condition with another.
     * Both conditions must be true for the result to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition age = Filters.greaterThan("age", 18);
     * Condition status = Filters.equal("status", "active");
     * And combined = ((ComposableCondition) age).and(status);
     * // SQL: ((age > 18) AND (status = 'active'))
     * }</pre>
     *
     * @param cond the condition to AND with this condition (must not be {@code null})
     * @return a new {@link And} condition containing both conditions
     * @throws IllegalArgumentException if {@code cond} is {@code null}, or if either {@code this} or {@code cond}
     *                                  is non-composable — a {@link Criteria}, a SQL clause, an
     *                                  {@code ON}/{@code USING} connector, an {@code ANY}/{@code ALL}/{@code SOME}
     *                                  quantified-subquery operand, or an empty predicate (a blank
     *                                  {@link Expression} or empty {@link Junction})
     */
    public And and(final Condition cond) {
        N.checkArgNotNull(cond, "cond");
        validateComposableOperand(this, "and");
        validateComposableOperand(cond, "and");

        return new And(this, cond);
    }

    /**
     * Creates a new OR condition combining this condition with another.
     * At least one condition must be true for the result to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition admin = Filters.equal("role", "admin");
     * Condition manager = Filters.equal("role", "manager");
     * Or either = ((ComposableCondition) admin).or(manager);
     * // SQL: ((role = 'admin') OR (role = 'manager'))
     * }</pre>
     *
     * @param cond the condition to OR with this condition (must not be {@code null})
     * @return a new {@link Or} condition containing both conditions
     * @throws IllegalArgumentException if {@code cond} is {@code null}, or if either {@code this} or {@code cond}
     *                                  is non-composable — a {@link Criteria}, a SQL clause, an
     *                                  {@code ON}/{@code USING} connector, an {@code ANY}/{@code ALL}/{@code SOME}
     *                                  quantified-subquery operand, or an empty predicate (a blank
     *                                  {@link Expression} or empty {@link Junction})
     */
    public Or or(final Condition cond) {
        N.checkArgNotNull(cond, "cond");
        validateComposableOperand(this, "or");
        validateComposableOperand(cond, "or");

        return new Or(this, cond);
    }

    /**
     * Creates a new XOR (exclusive OR) condition combining this condition with another.
     * Exactly one of the two conditions must be true for the result to be true.
     *
     * <p>XOR has no direct SQL equivalent, so it is expanded to its composable definition:
     * {@code (A AND NOT B) OR (NOT A AND B)}. The object returned is the outer {@link Or} of that
     * expanded expression.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition a = Filters.equal("type", "A");
     * Condition b = Filters.equal("type", "B");
     * Or exclusive = ((ComposableCondition) a).xor(b);
     * // Logically: (a AND NOT b) OR (NOT a AND b)
     * // SQL: ((((type = 'A') AND (NOT (type = 'B')))) OR (((NOT (type = 'A')) AND (type = 'B'))))
     * // (each junction wraps every child in parentheses and the whole expression in an outer pair,
     * //  so the nested And inside the outer Or picks up an extra layer of parens)
     * }</pre>
     *
     * @param cond the condition to XOR with this condition (must not be {@code null})
     * @return a composable condition representing the exclusive-or {@code (this AND NOT cond) OR (NOT this AND cond)}
     * @throws IllegalArgumentException if {@code cond} is {@code null}, or if either {@code this} or {@code cond}
     *                                  is non-composable — a {@link Criteria}, a SQL clause, an
     *                                  {@code ON}/{@code USING} connector, an {@code ANY}/{@code ALL}/{@code SOME}
     *                                  quantified-subquery operand, or an empty predicate (a blank
     *                                  {@link Expression} or empty {@link Junction})
     */
    public Or xor(final Condition cond) {
        N.checkArgNotNull(cond, "cond");
        validateComposableOperand(this, "xor");
        validateComposableOperand(cond, "xor");

        return new Or(new And(this, new Not(cond)), new And(new Not(this), cond));
    }
}
