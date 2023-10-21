/*
 * Copyright (C) 2020 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.condition;

import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 1.10.12
 */
public class InSubQuery extends AbstractCondition {
    // For Kryo
    final String propName;

    // For Kryo
    final Collection<String> propNames;

    private SubQuery subQuery;

    // For Kryo
    InSubQuery() {
        propName = null;
        propNames = null;
    }

    /**
     *
     *
     * @param propName
     * @param subQuery
     */
    public InSubQuery(String propName, SubQuery subQuery) {
        super(Operator.IN);

        N.checkArgNotNull(subQuery, "'subQuery' can't be null or empty");

        this.propName = propName;
        this.subQuery = subQuery;
        this.propNames = null;
    }

    /**
     *
     *
     * @param propNames
     * @param subQuery
     */
    public InSubQuery(Collection<String> propNames, SubQuery subQuery) {
        super(Operator.IN);

        N.checkArgNotNullOrEmpty(propNames, "propNames");
        N.checkArgNotNull(subQuery, "'subQuery' can't be null or empty");

        this.propNames = propNames;
        this.subQuery = subQuery;
        this.propName = null;
    }

    /**
     *
     *
     * @return
     */
    public String getPropName() {
        return propName;
    }

    /**
     *
     *
     * @return
     */
    public Collection<String> getPropNames() {
        return propNames;
    }

    /**
     *
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public SubQuery getSubQuery() {
        return subQuery;
    }

    /**
     *
     *
     * @param subQuery
     */
    public void setSubQuery(SubQuery subQuery) {
        this.subQuery = subQuery;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        return subQuery.getParameters();
    }

    /**
     *
     */
    @Override
    public void clearParameters() {
        subQuery.clearParameters();
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        InSubQuery copy = (InSubQuery) super.copy();

        copy.subQuery = subQuery.copy();

        return (T) copy;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + (Strings.isNotEmpty(propName) ? N.hashCode(propName) : N.hashCode(propNames));
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + ((subQuery == null) ? 0 : subQuery.hashCode());

        return h;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof InSubQuery) {
            InSubQuery other = (InSubQuery) obj;

            return N.equals(propName, other.propName) && N.equals(propNames, other.propNames) && N.equals(operator, other.operator)
                    && N.equals(subQuery, other.subQuery);
        }

        return false;
    }

    /**
     *
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (Strings.isNotEmpty(propName)) {
            if (namingPolicy == NamingPolicy.NO_CHANGE) {
                return propName + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy) + WD.PARENTHESES_R;
            } else {
                return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                        + WD.PARENTHESES_R;
            }
        } else {
            if (namingPolicy == NamingPolicy.NO_CHANGE) {
                return "(" + Strings.join(propNames, ", ") + ") " + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                        + WD.PARENTHESES_R;
            } else {
                final Throwables.Function<String, String, RuntimeException> func = new Throwables.Function<>() {
                    @Override
                    public String apply(String t) throws RuntimeException {
                        return namingPolicy.convert(t);
                    }
                };

                return "(" + Strings.join(N.map(propNames, func), ", ") + ") " + getOperator().toString() + WD.SPACE_PARENTHESES_L
                        + subQuery.toString(namingPolicy) + WD.PARENTHESES_R;
            }
        }
    }
}
