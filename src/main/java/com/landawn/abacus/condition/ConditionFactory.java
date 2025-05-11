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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.condition.Expression.Expr;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.EntityId;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.QueryUtil;
import com.landawn.abacus.util.SortDirection;
import com.landawn.abacus.util.WD;

/**
 * A factory for creating Condition objects.
 *
 */
public class ConditionFactory {
    /**
     * Expression with question mark literal: "?".
     */
    public static final Expression QME = Expr.of(WD.QUESTION_MARK);

    public static final SortDirection ASC = SortDirection.ASC;

    public static final SortDirection DESC = SortDirection.DESC;

    private static final Expression ALWAYS_TRUE = Expression.of("1 < 2");

    private static final Expression ALWAYS_FALSE = Expression.of("1 > 2");

    private ConditionFactory() {
        // No instance;
    }

    /**
     *
     *
     * @return
     */
    public static Expression alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     *
     * @return
     * @deprecated
     */
    @Deprecated
    public static Expression alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     *
     * @param propName
     * @return
     */
    public static NamedProperty namedProperty(final String propName) {
        return NamedProperty.of(propName);
    }

    /**
     *
     * @param literal
     * @return
     */
    public static Expression expr(final String literal) {
        return Expression.of(literal);
    }

    /**
     *
     * @param propName
     * @param operator
     * @param propValue
     * @return
     */
    public static Binary binary(final String propName, final Operator operator, final Object propValue) {
        return new Binary(propName, operator, propValue);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Equal equal(final String propName, final Object propValue) { //NOSONAR
        return new Equal(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static Equal equal(final String propName) {//NOSONAR
        return equal(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Equal eq(final String propName, final Object propValue) {
        return new Equal(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static Equal eq(final String propName) {
        return eq(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValues
     * @return
     */
    public static Or eqOr(final String propName, final Object... propValues) {
        N.checkArgNotEmpty(propValues, "propValues");

        final Or or = CF.or();

        for (final Object propValue : propValues) {
            or.add(eq(propName, propValue));
        }

        return or;
    }

    /**
     *
     * @param propName
     * @param propValues
     * @return
     */
    public static Or eqOr(final String propName, final Collection<?> propValues) {
        N.checkArgNotEmpty(propValues, "propValues");

        final Or or = CF.or();

        for (final Object propValue : propValues) {
            or.add(eq(propName, propValue));
        }

        return or;
    }

    /**
     *
     * @param props
     * @return
     */
    public static Or eqOr(final Map<String, ?> props) {
        N.checkArgNotEmpty(props, "props");

        final Iterator<? extends Map.Entry<String, ?>> propIter = props.entrySet().iterator();

        if (props.size() == 1) {
            final Map.Entry<String, ?> prop = propIter.next();
            return or(eq(prop.getKey(), prop.getValue()));
        } else if (props.size() == 2) {
            final Map.Entry<String, ?> prop1 = propIter.next();
            final Map.Entry<String, ?> prop2 = propIter.next();
            return eq(prop1.getKey(), prop1.getValue()).or(eq(prop2.getKey(), prop2.getValue()));
        } else {
            final Condition[] conds = new Condition[props.size()];
            Map.Entry<String, ?> prop = null;

            for (int i = 0, size = props.size(); i < size; i++) {
                prop = propIter.next();
                conds[i] = CF.eq(prop.getKey(), prop.getValue());
            }

            return or(conds);
        }
    }

    /**
     *
     * @param entity
     * @return
     */
    public static Or eqOr(final Object entity) {
        return eqOr(entity, QueryUtil.getSelectPropNames(entity.getClass(), false, null));
    }

    /**
     *
     * @param entity
     * @param selectPropNames
     * @return
     */
    public static Or eqOr(final Object entity, final Collection<String> selectPropNames) {
        N.checkArgNotEmpty(selectPropNames, "selectPropNames"); //NOSONAR

        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entity.getClass());
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return or(eq(propName, entityInfo.getPropValue(entity, propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, entityInfo.getPropValue(entity, propName1)).or(eq(propName2, entityInfo.getPropValue(entity, propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, entityInfo.getPropValue(entity, propName));
            }

            return or(conds);
        }
    }

    /**
     *
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @return
     */
    public static Or eqOr(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        return eq(propName1, propValue1).or(eq(propName2, propValue2));
    }

    /**
     *
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @param propName3
     * @param propValue3
     * @return
     */
    public static Or eqOr(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3,
            final Object propValue3) {
        return or(eq(propName1, propValue1), eq(propName2, propValue2), eq(propName3, propValue3));
    }

    /**
     *
     * @param props
     * @return
     */
    public static And eqAnd(final Map<String, ?> props) {
        N.checkArgNotEmpty(props, "props");

        final Iterator<? extends Map.Entry<String, ?>> propIter = props.entrySet().iterator();

        if (props.size() == 1) {
            final Map.Entry<String, ?> prop = propIter.next();
            return and(eq(prop.getKey(), prop.getValue()));
        } else if (props.size() == 2) {
            final Map.Entry<String, ?> prop1 = propIter.next();
            final Map.Entry<String, ?> prop2 = propIter.next();
            return eq(prop1.getKey(), prop1.getValue()).and(eq(prop2.getKey(), prop2.getValue()));
        } else {
            final Condition[] conds = new Condition[props.size()];
            Map.Entry<String, ?> prop = null;

            for (int i = 0, size = props.size(); i < size; i++) {
                prop = propIter.next();
                conds[i] = CF.eq(prop.getKey(), prop.getValue());
            }

            return and(conds);
        }

    }

    /**
     *
     * @param entity
     * @return
     */
    public static And eqAnd(final Object entity) {
        return eqAnd(entity, QueryUtil.getSelectPropNames(entity.getClass(), false, null));
    }

    /**
     *
     * @param entity
     * @param selectPropNames
     * @return
     */
    public static And eqAnd(final Object entity, final Collection<String> selectPropNames) {
        N.checkArgNotEmpty(selectPropNames, "selectPropNames");

        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entity.getClass());
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return and(eq(propName, entityInfo.getPropValue(entity, propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, entityInfo.getPropValue(entity, propName1)).and(eq(propName2, entityInfo.getPropValue(entity, propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, entityInfo.getPropValue(entity, propName));
            }

            return and(conds);
        }
    }

    /**
     *
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @return
     */
    public static And eqAnd(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        return eq(propName1, propValue1).and(eq(propName2, propValue2));
    }

    /**
     *
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @param propName3
     * @param propValue3
     * @return
     */
    public static And eqAnd(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3,
            final Object propValue3) {
        return and(eq(propName1, propValue1), eq(propName2, propValue2), eq(propName3, propValue3));
    }

    /**
     * Eq and or.
     *
     * @param propsList
     * @return
     */
    @Beta
    public static Or eqAndOr(final List<? extends Map<String, ?>> propsList) {
        N.checkArgNotEmpty(propsList, "propsList");

        final Condition[] conds = new Condition[propsList.size()];

        for (int i = 0, size = propsList.size(); i < size; i++) {
            conds[i] = eqAnd(propsList.get(i));
        }

        return or(conds);
    }

    /**
     * Eq and or.
     *
     * @param entities
     * @return
     */
    @Beta
    public static Or eqAndOr(final Collection<?> entities) {
        N.checkArgNotEmpty(entities, "entities");

        return eqAndOr(entities, QueryUtil.getSelectPropNames(N.firstNonNull(entities).orElseNull().getClass(), false, null));
    }

    /**
     * Eq and or.
     *
     * @param entities
     * @param selectPropNames
     * @return
     */
    @Beta
    public static Or eqAndOr(final Collection<?> entities, final Collection<String> selectPropNames) {
        N.checkArgNotEmpty(entities, "entities");
        N.checkArgNotEmpty(selectPropNames, "selectPropNames");

        final Iterator<?> iter = entities.iterator();
        final Condition[] conds = new Condition[entities.size()];

        for (int i = 0, size = entities.size(); i < size; i++) {
            conds[i] = eqAnd(iter.next(), selectPropNames);
        }

        return or(conds);
    }

    /**
     *
     *
     * @param propName
     * @param minValue
     * @param maxValue
     * @return
     */
    public static And gtAndLt(final String propName, final Object minValue, final Object maxValue) {
        return gt(propName, minValue).and(lt(propName, maxValue));
    }

    /**
     *
     *
     * @param propName
     * @return
     */
    public static And gtAndLt(final String propName) {
        return gt(propName).and(lt(propName));
    }

    /**
     *
     *
     * @param propName
     * @param minValue
     * @param maxValue
     * @return
     */
    public static And geAndLt(final String propName, final Object minValue, final Object maxValue) {
        return ge(propName, minValue).and(lt(propName, maxValue));
    }

    /**
     *
     *
     * @param propName
     * @return
     */
    public static And geAndLt(final String propName) {
        return ge(propName).and(lt(propName));
    }

    /**
     *
     *
     * @param propName
     * @param minValue
     * @param maxValue
     * @return
     */
    public static And geAndLe(final String propName, final Object minValue, final Object maxValue) {
        return ge(propName, minValue).and(le(propName, maxValue));
    }

    /**
     *
     *
     * @param propName
     * @return
     */
    public static And geAndLe(final String propName) {
        return ge(propName).and(le(propName));
    }

    /**
     *
     *
     * @param propName
     * @param minValue
     * @param maxValue
     * @return
     */
    public static And gtAndLe(final String propName, final Object minValue, final Object maxValue) {
        return gt(propName, minValue).and(le(propName, maxValue));
    }

    /**
     *
     *
     * @param propName
     * @return
     */
    public static And gtAndLe(final String propName) {
        return gt(propName).and(le(propName));
    }

    /**
     *
     *
     * @param entityId
     * @return
     */
    public static And id2Cond(final EntityId entityId) {
        N.checkArgNotNull(entityId, "entityId");

        final Collection<String> selectPropNames = entityId.keySet();
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return and(eq(propName, entityId.get(propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, entityId.get(propName1)).and(eq(propName2, entityId.get(propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, entityId.get(propName));
            }

            return and(conds);
        }
    }

    /**
     *
     *
     * @param entityIds
     * @return
     */
    public static Or id2Cond(final Collection<? extends EntityId> entityIds) {
        N.checkArgNotEmpty(entityIds, "entityIds");

        final Iterator<? extends EntityId> iter = entityIds.iterator();
        final Condition[] conds = new Condition[entityIds.size()];

        for (int i = 0, size = entityIds.size(); i < size; i++) {
            conds[i] = CF.id2Cond(iter.next());
        }

        return CF.or(conds);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static NotEqual notEqual(final String propName, final Object propValue) {
        return new NotEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static NotEqual notEqual(final String propName) {
        return notEqual(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static NotEqual ne(final String propName, final Object propValue) {
        return new NotEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static NotEqual ne(final String propName) {
        return ne(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static GreaterThan greaterThan(final String propName, final Object propValue) {
        return new GreaterThan(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static GreaterThan greaterThan(final String propName) {
        return greaterThan(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static GreaterThan gt(final String propName, final Object propValue) {
        return new GreaterThan(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static GreaterThan gt(final String propName) {
        return gt(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static GreaterEqual greaterEqual(final String propName, final Object propValue) {
        return new GreaterEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static GreaterEqual greaterEqual(final String propName) {
        return greaterEqual(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static GreaterEqual ge(final String propName, final Object propValue) {
        return new GreaterEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static GreaterEqual ge(final String propName) {
        return ge(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static LessThan lessThan(final String propName, final Object propValue) {
        return new LessThan(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static LessThan lessThan(final String propName) {
        return lessThan(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static LessThan lt(final String propName, final Object propValue) {
        return new LessThan(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static LessThan lt(final String propName) {
        return lt(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static LessEqual lessEqual(final String propName, final Object propValue) {
        return new LessEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static LessEqual lessEqual(final String propName) {
        return lessEqual(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static LessEqual le(final String propName, final Object propValue) {
        return new LessEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static LessEqual le(final String propName) {
        return le(propName, QME);
    }

    /**
     *
     * @param propName
     * @param minValue
     * @param maxValue
     * @return
     */
    public static Between between(final String propName, final Object minValue, final Object maxValue) {
        return new Between(propName, minValue, maxValue);
    }

    /**
     *
     * @param propName
     * @return
     */
    public static Between between(final String propName) {
        return new Between(propName, CF.QME, CF.QME);
    }

    /**
     *
     * @param propName
     * @param minValue
     * @param maxValue
     * @return
     * @deprecated please use {@link #between(String, Object, Object)}
     */
    @Deprecated
    public static Between bt(final String propName, final Object minValue, final Object maxValue) {
        return new Between(propName, minValue, maxValue);
    }

    /**
     *
     * @param propName
     * @return
     * @deprecated please use {@link #between(String)}
     */
    @Deprecated
    public static Between bt(final String propName) {
        return new Between(propName, CF.QME, CF.QME);
    }

    /**
     *
     * @param propName
     * @param minValue
     * @param maxValue
     * @return
     */
    public static NotBetween notBetween(final String propName, final Object minValue, final Object maxValue) {
        return new NotBetween(propName, minValue, maxValue);
    }

    /**
     *
     * @param propName
     * @return
     */
    public static NotBetween notBetween(final String propName) {
        return new NotBetween(propName, CF.QME, CF.QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Like like(final String propName, final Object propValue) {
        return new Like(propName, propValue);
    }

    /**
     *
     * @param propName
     * @return
     */
    public static Like like(final String propName) {
        return like(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static NotLike notLike(final String propName, final Object propValue) {
        return new NotLike(propName, propValue);
    }

    /**
     *
     * @param propName
     * @return
     */
    public static NotLike notLike(final String propName) {
        return new NotLike(propName, CF.QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Like contains(final String propName, final Object propValue) {
        return new Like(propName, WD._PERCENT + N.stringOf(propValue) + WD._PERCENT);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static NotLike notContains(final String propName, final Object propValue) {
        return new NotLike(propName, WD._PERCENT + N.stringOf(propValue) + WD._PERCENT);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Like startsWith(final String propName, final Object propValue) {
        return new Like(propName, N.stringOf(propValue) + WD._PERCENT);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static NotLike notStartsWith(final String propName, final Object propValue) {
        return new NotLike(propName, N.stringOf(propValue) + WD._PERCENT);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Like endsWith(final String propName, final Object propValue) {
        return new Like(propName, WD._PERCENT + N.stringOf(propValue));
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static NotLike notEndsWith(final String propName, final Object propValue) {
        return new NotLike(propName, WD._PERCENT + N.stringOf(propValue));
    }

    /**
     * Checks if is null.
     *
     * @param propName
     * @return
     */
    public static IsNull isNull(final String propName) {
        return new IsNull(propName);
    }

    /**
     * Checks if is null or empty.
     *
     * @param propName
     * @return
     */
    @Beta
    public static Or isEmpty(final String propName) {
        return isNull(propName).or(equal(propName, ""));
    }

    /**
     * Checks if is null or zero.
     *
     * @param propName
     * @return
     */
    @Beta
    public static Or isNullOrZero(final String propName) {
        return isNull(propName).or(equal(propName, 0));
    }

    /**
     * Checks if is not null.
     *
     * @param propName
     * @return
     */
    public static IsNotNull isNotNull(final String propName) {
        return new IsNotNull(propName);
    }

    /**
     * Checks if is na N.
     *
     * @param propName
     * @return
     */
    public static IsNaN isNaN(final String propName) {
        return new IsNaN(propName);
    }

    /**
     * Checks if is not na N.
     *
     * @param propName
     * @return
     */
    public static IsNotNaN isNotNaN(final String propName) {
        return new IsNotNaN(propName);
    }

    /**
     * Checks if is infinite.
     *
     * @param propName
     * @return
     */
    public static IsInfinite isInfinite(final String propName) {
        return new IsInfinite(propName);
    }

    /**
     * Checks if is not infinite.
     *
     * @param propName
     * @return
     */
    public static IsNotInfinite isNotInfinite(final String propName) {
        return new IsNotInfinite(propName);
    }

    /**
     * Checks if is.
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Is is(final String propName, final Object propValue) {
        return new Is(propName, propValue);
    }

    /**
     * Checks if is not.
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static IsNot isNot(final String propName, final Object propValue) {
        return new IsNot(propName, propValue);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static XOR xor(final String propName, final Object propValue) {
        return new XOR(propName, propValue);
    }

    /**
     *
     * @param conditions
     * @return
     */
    public static Or or(final Condition... conditions) {
        return new Or(conditions);
    }

    /**
     *
     * @param conditions
     * @return
     */
    public static Or or(final Collection<? extends Condition> conditions) {
        return new Or(conditions);
    }

    /**
     *
     * @param conditions
     * @return
     */
    public static And and(final Condition... conditions) {
        return new And(conditions);
    }

    /**
     *
     * @param conditions
     * @return
     */
    public static And and(final Collection<? extends Condition> conditions) {
        return new And(conditions);
    }

    /**
     *
     * @param operator
     * @param conditions
     * @return
     */
    @Beta
    public static Junction junction(final Operator operator, final Condition... conditions) {
        return new Junction(operator, conditions);
    }

    /**
     *
     * @param operator
     * @param conditions
     * @return
     */
    @Beta
    public static Junction junction(final Operator operator, final Collection<? extends Condition> conditions) {
        return new Junction(operator, conditions);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Where where(final Condition condition) {
        return new Where(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Where where(final String condition) {
        return new Where(expr(condition));
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static GroupBy groupBy(final String... propNames) {
        return new GroupBy(propNames);
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static GroupBy groupBy(final Collection<String> propNames) {
        return groupBy(propNames, SortDirection.ASC);
    }

    /**
     *
     * @param propNames
     * @param direction
     * @return
     */
    public static GroupBy groupBy(final Collection<String> propNames, final SortDirection direction) {
        return new GroupBy(propNames, direction);
    }

    /**
     *
     * @param propName
     * @param direction
     * @return
     */
    public static GroupBy groupBy(final String propName, final SortDirection direction) {
        return new GroupBy(propName, direction);
    }

    /**
     *
     * @param propNameA
     * @param directionA
     * @param propNameB
     * @param directionB
     * @return
     */
    public static GroupBy groupBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB) {
        return groupBy(N.asLinkedHashMap(propNameA, directionA, propNameB, directionB));
    }

    /**
     *
     * @param propNameA
     * @param directionA
     * @param propNameB
     * @param directionB
     * @param propNameC
     * @param directionC
     * @return
     */
    public static GroupBy groupBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB,
            final String propNameC, final SortDirection directionC) {
        return groupBy(N.asLinkedHashMap(propNameA, directionA, propNameB, directionB, propNameC, directionC));
    }

    /**
     *
     * @param orders should be a {@code LinkedHashMap}
     * @return
     */
    public static GroupBy groupBy(final Map<String, SortDirection> orders) {
        return new GroupBy(orders);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static GroupBy groupBy(final Condition condition) {
        return new GroupBy(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Having having(final Condition condition) {
        return new Having(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Having having(final String condition) {
        return new Having(expr(condition));
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static OrderBy orderBy(final String... propNames) {
        return new OrderBy(propNames);
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static OrderBy orderByAsc(final String... propNames) {
        return new OrderBy(Array.asList(propNames), SortDirection.ASC);
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static OrderBy orderByAsc(final Collection<String> propNames) {
        return new OrderBy(propNames, SortDirection.ASC);
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static OrderBy orderByDesc(final String... propNames) {
        return new OrderBy(Array.asList(propNames), SortDirection.DESC);
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static OrderBy orderByDesc(final Collection<String> propNames) {
        return new OrderBy(propNames, SortDirection.DESC);
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static OrderBy orderBy(final Collection<String> propNames) {
        return orderBy(propNames, SortDirection.ASC);
    }

    /**
     *
     * @param propNames
     * @param direction
     * @return
     */
    public static OrderBy orderBy(final Collection<String> propNames, final SortDirection direction) {
        return new OrderBy(propNames, direction);
    }

    /**
     *
     * @param propName
     * @param direction
     * @return
     */
    public static OrderBy orderBy(final String propName, final SortDirection direction) {
        return new OrderBy(propName, direction);
    }

    /**
     *
     * @param propNameA
     * @param directionA
     * @param propNameB
     * @param directionB
     * @return
     */
    public static OrderBy orderBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB) {
        return orderBy(N.asLinkedHashMap(propNameA, directionA, propNameB, directionB));
    }

    /**
     *
     * @param propNameA
     * @param directionA
     * @param propNameB
     * @param directionB
     * @param propNameC
     * @param directionC
     * @return
     */
    public static OrderBy orderBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB,
            final String propNameC, final SortDirection directionC) {
        return orderBy(N.asLinkedHashMap(propNameA, directionA, propNameB, directionB, propNameC, directionC));
    }

    /**
     *
     * @param orders should be a {@code LinkedHashMap}
     * @return
     */
    public static OrderBy orderBy(final Map<String, SortDirection> orders) {
        return new OrderBy(orders);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static OrderBy orderBy(final Condition condition) {
        return new OrderBy(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static On on(final Condition condition) {
        return new On(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static On on(final String condition) {
        return new On(expr(condition));
    }

    /**
     *
     * @param propName
     * @param anoPropName
     * @return
     */
    public static On on(final String propName, final String anoPropName) {
        return new On(propName, anoPropName);
    }

    /**
     *
     * @param propNamePair
     * @return
     */
    public static On on(final Map<String, String> propNamePair) {
        return new On(propNamePair);
    }

    /**
     * It's recommended to use {@code On}, instead of {@code Using}.
     *
     * @param columnNames
     * @return
     * @deprecated
     */
    @Deprecated
    public static Using using(final String... columnNames) {
        return new Using(columnNames);
    }

    /**
     * It's recommended to use {@code On}, instead of {@code Using}.
     *
     * @param columnNames
     * @return
     * @deprecated
     */
    @Deprecated
    public static Using using(final Collection<String> columnNames) {
        return new Using(columnNames);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static Join join(final String joinEntity) {
        return new Join(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static Join join(final String joinEntity, final Condition condition) {
        return new Join(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static Join join(final Collection<String> joinEntities, final Condition condition) {
        return new Join(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static LeftJoin leftJoin(final String joinEntity) {
        return new LeftJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static LeftJoin leftJoin(final String joinEntity, final Condition condition) {
        return new LeftJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static LeftJoin leftJoin(final Collection<String> joinEntities, final Condition condition) {
        return new LeftJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static RightJoin rightJoin(final String joinEntity) {
        return new RightJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static RightJoin rightJoin(final String joinEntity, final Condition condition) {
        return new RightJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static RightJoin rightJoin(final Collection<String> joinEntities, final Condition condition) {
        return new RightJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static CrossJoin crossJoin(final String joinEntity) {
        return new CrossJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static CrossJoin crossJoin(final String joinEntity, final Condition condition) {
        return new CrossJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static CrossJoin crossJoin(final Collection<String> joinEntities, final Condition condition) {
        return new CrossJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static FullJoin fullJoin(final String joinEntity) {
        return new FullJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static FullJoin fullJoin(final String joinEntity, final Condition condition) {
        return new FullJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static FullJoin fullJoin(final Collection<String> joinEntities, final Condition condition) {
        return new FullJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static InnerJoin innerJoin(final String joinEntity) {
        return new InnerJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static InnerJoin innerJoin(final String joinEntity, final Condition condition) {
        return new InnerJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static InnerJoin innerJoin(final Collection<String> joinEntities, final Condition condition) {
        return new InnerJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static NaturalJoin naturalJoin(final String joinEntity) {
        return new NaturalJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static NaturalJoin naturalJoin(final String joinEntity, final Condition condition) {
        return new NaturalJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static NaturalJoin naturalJoin(final Collection<String> joinEntities, final Condition condition) {
        return new NaturalJoin(joinEntities, condition);
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static In in(final String propName, final int[] values) {
        return in(propName, Array.box(values));
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static In in(final String propName, final long[] values) {
        return in(propName, Array.box(values));
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static In in(final String propName, final double[] values) {
        return in(propName, Array.box(values));
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static In in(final String propName, final Object[] values) {
        return in(propName, Arrays.asList(values));
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static In in(final String propName, final Collection<?> values) {
        return new In(propName, values);
    }

    /**
     *
     *
     * @param propName
     * @param subQuery
     * @return
     */
    public static InSubQuery in(final String propName, final SubQuery subQuery) {
        return new InSubQuery(propName, subQuery);
    }

    /**
     *
     *
     * @param propNames
     * @param subQuery
     * @return
     */
    public static InSubQuery in(final Collection<String> propNames, final SubQuery subQuery) {
        return new InSubQuery(propNames, subQuery);
    }

    //    public static Condition in(final String propName, final SubQuery condition) {
    //        return new Binary(propName, Operator.IN, condition);
    //    }
    //
    //    public static Condition in(final Collection<String> propNames, final SubQuery condition) {
    //        return new Binary(AbstractCondition.concatPropNames(propNames), Operator.IN, condition);
    //    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static NotIn notIn(final String propName, final int[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static NotIn notIn(final String propName, final long[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static NotIn notIn(final String propName, final double[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static NotIn notIn(final String propName, final Object[] values) {
        return notIn(propName, Arrays.asList(values));
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static NotIn notIn(final String propName, final Collection<?> values) {
        return new NotIn(propName, values);
    }

    /**
     *
     * @param propName
     * @param subQuery
     * @return
     */
    public static NotInSubQuery notIn(final String propName, final SubQuery subQuery) {
        return new NotInSubQuery(propName, subQuery);
    }

    /**
     *
     *
     * @param propNames
     * @param subQuery
     * @return
     */
    public static NotInSubQuery notIn(final Collection<String> propNames, final SubQuery subQuery) {
        return new NotInSubQuery(propNames, subQuery);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static All all(final SubQuery condition) {
        return new All(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Any any(final SubQuery condition) {
        return new Any(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Some some(final SubQuery condition) {
        return new Some(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Exists exists(final SubQuery condition) {
        return new Exists(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Not notExists(final SubQuery condition) {
        return exists(condition).not();
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Union union(final SubQuery condition) {
        return new Union(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static UnionAll unionAll(final SubQuery condition) {
        return new UnionAll(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Except except(final SubQuery condition) {
        return new Except(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Intersect intersect(final SubQuery condition) {
        return new Intersect(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Minus minus(final SubQuery condition) {
        return new Minus(condition);
    }

    /**
     *
     * @param operator
     * @param condition
     * @return
     */
    @Beta
    public static Cell cell(final Operator operator, final Condition condition) {
        return new Cell(operator, condition);
    }

    /**
     *
     * @param entityClass
     * @param propNames
     * @param condition
     * @return
     */
    public static SubQuery subQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition) {
        return new SubQuery(entityClass, propNames, condition);
    }

    /**
     *
     * @param entityName
     * @param propNames
     * @param condition
     * @return
     */
    public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final Condition condition) {
        return new SubQuery(entityName, propNames, condition);
    }

    /**
     *
     * @param entityName
     * @param propNames
     * @param condition
     * @return
     */
    public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final String condition) {
        return new SubQuery(entityName, propNames, expr(condition));
    }

    /**
     *
     *
     * @param entityName
     * @param sql
     * @return
     * @see #subQuery(String)
     * @deprecated replaced by {@link #subQuery(String)}
     */
    @Deprecated
    public static SubQuery subQuery(final String entityName, final String sql) {
        return new SubQuery(entityName, sql);
    }

    /**
     *
     * @param sql
     * @return
     */
    public static SubQuery subQuery(final String sql) {
        return new SubQuery(sql);
    }

    /**
     *
     * @param count
     * @return
     */
    public static Limit limit(final int count) {
        return new Limit(count);
    }

    /**
     *
     * @param offset
     * @param count
     * @return
     */
    public static Limit limit(final int offset, final int count) {
        return new Limit(offset, count);
    }

    /**
     *
     * @param expr
     * @return
     */
    public static Limit limit(final String expr) {
        return new Limit(expr);
    }

    /**
     *
     *
     * @return
     */
    public static Criteria criteria() {
        return new Criteria();
    }

    /**
     * The Class CF.
     */
    public static final class CF extends ConditionFactory {

        private CF() {
            // singleton for utility class.
        }
    }

    public static final class CB {

        private CB() {
            // singleton for utility class.
        }

        /**
         *
         *
         * @param condition
         * @return
         */
        public static Criteria where(final Condition condition) {
            return CF.criteria().where(condition);
        }

        /**
         *
         *
         * @param condition
         * @return
         */
        public static Criteria where(final String condition) {
            return CF.criteria().where(condition);
        }

        /**
         *
         * @param condition
         * @return
         */
        public static Criteria groupBy(final Condition condition) {
            return CF.criteria().groupBy(condition);
        }

        /**
         *
         * @param propNames
         * @return
         */
        public static Criteria groupBy(final String... propNames) {
            return CF.criteria().groupBy(propNames);
        }

        /**
         *
         * @param propName
         * @param direction
         * @return
         */
        public static Criteria groupBy(final String propName, final SortDirection direction) {
            return CF.criteria().groupBy(propName, direction);
        }

        /**
         *
         * @param propNames
         * @return
         */
        public static Criteria groupBy(final Collection<String> propNames) {
            return CF.criteria().groupBy(propNames);
        }

        /**
         *
         * @param propNames
         * @param direction
         * @return
         */
        public static Criteria groupBy(final Collection<String> propNames, final SortDirection direction) {
            return CF.criteria().groupBy(propNames, direction);
        }

        /**
         *
         * @param orders
         * @return
         */
        public static Criteria groupBy(final Map<String, SortDirection> orders) {
            return CF.criteria().groupBy(orders);
        }

        /**
         *
         * @param condition
         * @return
         */
        public static Criteria having(final Condition condition) {
            return CF.criteria().having(condition);
        }

        /**
         *
         * @param condition
         * @return
         */
        public static Criteria having(final String condition) {
            return CF.criteria().having(condition);
        }

        /**
         *
         * @param propNames
         * @return
         */
        public static Criteria orderByAsc(final String... propNames) {
            return CF.criteria().orderByAsc(propNames);
        }

        /**
         *
         * @param propNames
         * @return
         */
        public static Criteria orderByAsc(final Collection<String> propNames) {
            return CF.criteria().orderByAsc(propNames);
        }

        /**
         *
         * @param propNames
         * @return
         */
        public static Criteria orderByDesc(final String... propNames) {
            return CF.criteria().orderByDesc(propNames);
        }

        /**
         *
         * @param propNames
         * @return
         */
        public static Criteria orderByDesc(final Collection<String> propNames) {
            return CF.criteria().orderByDesc(propNames);
        }

        /**
         *
         * @param condition
         * @return
         */
        public static Criteria orderBy(final Condition condition) {
            return CF.criteria().orderBy(condition);
        }

        /**
         *
         * @param propNames
         * @return
         */
        public static Criteria orderBy(final String... propNames) {
            return CF.criteria().orderBy(propNames);
        }

        /**
         *
         * @param propName
         * @param direction
         * @return
         */
        public static Criteria orderBy(final String propName, final SortDirection direction) {
            return CF.criteria().orderBy(propName, direction);
        }

        /**
         *
         * @param propNames
         * @return
         */
        public static Criteria orderBy(final Collection<String> propNames) {
            return CF.criteria().orderBy(propNames);
        }

        /**
         *
         * @param propNames
         * @param direction
         * @return
         */
        public static Criteria orderBy(final Collection<String> propNames, final SortDirection direction) {
            return CF.criteria().orderBy(propNames, direction);
        }

        /**
         *
         * @param orders
         * @return
         */
        public static Criteria orderBy(final Map<String, SortDirection> orders) {
            return CF.criteria().orderBy(orders);
        }

        /**
         *
         * @param condition
         * @return
         */
        public static Criteria limit(final Limit condition) {
            return CF.criteria().limit(condition);
        }

        /**
         *
         * @param count
         * @return
         */
        public static Criteria limit(final int count) {
            return CF.criteria().limit(count);
        }

        /**
         *
         * @param offset
         * @param count
         * @return
         */
        public static Criteria limit(final int offset, final int count) {
            return CF.criteria().limit(offset, count);
        }

        /**
         *
         *
         * @param expr
         * @return
         */
        public static Criteria limit(final String expr) {
            return CF.criteria().limit(expr);
        }
    }
}
