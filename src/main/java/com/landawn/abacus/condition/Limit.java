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

import static com.landawn.abacus.util.WD._SPACE;

import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 */
public class Limit extends AbstractCondition {

    private int count;

    private int offset;

    private String expr;

    // For Kryo
    Limit() {
    }

    /**
     * Constructor for LIMIT.
     *
     * @param count
     */
    public Limit(final int count) {
        this(0, count);
    }

    /**
     *
     *
     * @param offset
     * @param count
     */
    public Limit(final int offset, final int count) {
        super(Operator.LIMIT);
        this.count = count;
        this.offset = offset;
    }

    /**
     *
     *
     * @param expr
     */
    public Limit(final String expr) {
        this(0, Integer.MAX_VALUE);

        this.expr = expr;
    }

    /**
     *
     *
     * @return
     */
    public String getExpr() {
        return expr;
    }

    /**
     * Gets the count.
     *
     * @return
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets the offset.
     *
     * @return
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        return N.emptyList();
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        // do nothing.
    }

    /**
     *
     *
     * @param condition
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public And and(final Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param condition
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public Or or(final Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public Not not() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (Strings.isEmpty(expr)) {
            return expr;
        } else {
            return offset > 0 ? WD.LIMIT + _SPACE + count + _SPACE + WD.OFFSET + _SPACE + offset : WD.LIMIT + _SPACE + count;
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        if (Strings.isNotEmpty(expr)) {
            return expr.hashCode();
        } else {
            int h = 17;
            h = (h * 31) + count;
            return (h * 31) + offset;
        }
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Limit other) {
            if (Strings.isNotEmpty(expr)) {
                return expr.equals(other.expr);
            } else {
                return (count == other.count) && (offset == other.offset);
            }
        }

        return false;
    }
}
