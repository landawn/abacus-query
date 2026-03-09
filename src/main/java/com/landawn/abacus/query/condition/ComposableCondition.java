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
 * conditions that <em>do</em> support logical composition and provides working default
 * implementations for the four operations.</p>
 *
 * <p>Typical implementors include comparison conditions ({@link Binary} and subclasses),
 * range conditions ({@link AbstractBetween}), collection conditions ({@link AbstractIn}),
 * junction conditions ({@link Junction}, {@link And}, {@link Or}), and others.</p>
 *
 * @see Condition
 * @see And
 * @see Or
 * @see Not
 */
public abstract class ComposableCondition extends AbstractCondition {

    ComposableCondition() {
        super();
    }

    protected ComposableCondition(final Operator operator) {
        super(operator);
    }

    /**
     * Creates a new NOT condition that negates this condition.
     * The result is true when this condition is false, and vice versa.
     *
     * @return a new Not condition wrapping this condition
     */
    public Not not() {
        return new Not(this);
    }

    /**
     * Creates a new AND condition combining this condition with another.
     * Both conditions must be true for the result to be true.
     *
     * @param cond the condition to AND with this condition (must not be null)
     * @return a new And condition containing both conditions
     * @throws IllegalArgumentException if {@code cond} is null
     */
    public And and(final Condition cond) {
        N.checkArgNotNull(cond, "cond");
        validateComposableOperand(cond, "and");

        return new And(this, cond);
    }

    /**
     * Creates a new OR condition combining this condition with another.
     * At least one condition must be true for the result to be true.
     *
     * @param cond the condition to OR with this condition (must not be null)
     * @return a new Or condition containing both conditions
     * @throws IllegalArgumentException if {@code cond} is null
     */
    public Or or(final Condition cond) {
        N.checkArgNotNull(cond, "cond");
        validateComposableOperand(cond, "or");

        return new Or(this, cond);
    }

    /**
     * Creates a new XOR (exclusive OR) condition combining this condition with another.
     * Exactly one of the two conditions must be true for the result to be true.
     *
     * <p>XOR has no direct SQL equivalent, so it is expanded to its composable definition:
     * {@code (A AND NOT B) OR (NOT A AND B)}. The return type is {@link Or} because
     * that outer OR is the top-level operator once the expression is flattened.</p>
     *
     * @param cond the condition to XOR with this condition (must not be null)
     * @return a new {@link Or} condition representing {@code (this AND NOT cond) OR (NOT this AND cond)}
     * @throws IllegalArgumentException if {@code cond} is null
     */
    public Or xor(final Condition cond) {
        N.checkArgNotNull(cond, "cond");
        validateComposableOperand(cond, "xor");

        return new Or(new And(this, new Not(cond)), new And(new Not(this), cond));
    }
}
