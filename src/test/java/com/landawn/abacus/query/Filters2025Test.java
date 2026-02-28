/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Binary;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.GreaterEqual;
import com.landawn.abacus.query.condition.GreaterThan;
import com.landawn.abacus.query.condition.GroupBy;
import com.landawn.abacus.query.condition.Is;
import com.landawn.abacus.query.condition.IsInfinite;
import com.landawn.abacus.query.condition.IsNaN;
import com.landawn.abacus.query.condition.IsNot;
import com.landawn.abacus.query.condition.IsNotInfinite;
import com.landawn.abacus.query.condition.IsNotNaN;
import com.landawn.abacus.query.condition.IsNotNull;
import com.landawn.abacus.query.condition.IsNull;
import com.landawn.abacus.query.condition.Junction;
import com.landawn.abacus.query.condition.LessEqual;
import com.landawn.abacus.query.condition.LessThan;
import com.landawn.abacus.query.condition.Like;
import com.landawn.abacus.query.condition.NamedProperty;
import com.landawn.abacus.query.condition.Not;
import com.landawn.abacus.query.condition.NotBetween;
import com.landawn.abacus.query.condition.NotEqual;
import com.landawn.abacus.query.condition.NotLike;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Or;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.query.condition.XOR;
import com.landawn.abacus.query.entity.Account;

@Tag("2025")
public class Filters2025Test extends TestBase {

    @Test
    public void testAlwaysTrue() {
        Expression expr = Filters.alwaysTrue();
        assertNotNull(expr);
    }

    @Test
    public void testAlwaysFalse() {
        Expression expr = Filters.alwaysFalse();
        assertNotNull(expr);
    }

    @Test
    public void testNot() {
        Condition condition = Filters.equal("name", "John");
        Not notCondition = Filters.not(condition);
        assertNotNull(notCondition);
        assertEquals(Operator.NOT, notCondition.getOperator());
    }

    @Test
    public void testNamedProperty() {
        NamedProperty prop = Filters.namedProperty("userName");
        assertNotNull(prop);
        assertEquals("userName", prop.propName());
    }

    @Test
    public void testExpr() {
        Expression expr = Filters.expr("age > 18");
        assertNotNull(expr);
    }

    @Test
    public void testBinary() {
        Binary binary = Filters.binary("age", Operator.GREATER_THAN, 18);
        assertNotNull(binary);
        assertEquals("age", binary.getPropName());
        assertEquals(Operator.GREATER_THAN, binary.getOperator());
    }

    @Test
    public void testEqual() {
        Equal equal = Filters.equal("name", "John");
        assertNotNull(equal);
        assertEquals("name", equal.getPropName());
        assertEquals("John", equal.getPropValue());
    }

    @Test
    public void testEqualWithoutValue() {
        Equal equal = Filters.equal("name");
        assertNotNull(equal);
        assertEquals("name", equal.getPropName());
    }

    @Test
    public void testEq() {
        Equal equal = Filters.eq("age", 30);
        assertNotNull(equal);
        assertEquals("age", equal.getPropName());
        assertEquals(30, (Integer) equal.getPropValue());
    }

    @Test
    public void testEqWithoutValue() {
        Equal equal = Filters.eq("status");
        assertNotNull(equal);
    }

    @Test
    public void testEqOrCollection() {
        List<Integer> ages = Arrays.asList(20, 30, 40);
        Or or = Filters.eqOr("age", ages);
        assertNotNull(or);
        assertFalse(or.getConditions().isEmpty());
    }

    @Test
    public void testEqOrMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 30);

        Or or = Filters.eqOr(props);
        assertNotNull(or);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testEqOrEntity() {
        Account account = new Account();
        Or or = Filters.eqOr(account);
        assertNotNull(or);
    }

    @Test
    public void testEqOrTwoProps() {
        Or or = Filters.eqOr("name", "John", "age", 30);
        assertNotNull(or);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testEqOrThreeProps() {
        Or or = Filters.eqOr("name", "John", "age", 30, "status", "active");
        assertNotNull(or);
        assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testEqAndMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("status", "active");
        props.put("verified", true);

        And and = Filters.eqAnd(props);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testEqAndEntity() {
        Account account = new Account();
        And and = Filters.eqAnd(account);
        assertNotNull(and);
    }

    @Test
    public void testEqAndTwoProps() {
        And and = Filters.eqAnd("status", "active", "verified", true);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testEqAndThreeProps() {
        And and = Filters.eqAnd("name", "John", "age", 30, "status", "active");
        assertNotNull(and);
        assertEquals(3, and.getConditions().size());
    }

    @Test
    public void testGtAndLt() {
        And and = Filters.gtAndLt("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testGeAndLt() {
        And and = Filters.geAndLt("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testGeAndLe() {
        And and = Filters.geAndLe("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testGtAndLe() {
        And and = Filters.gtAndLe("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testNotEqual() {
        NotEqual notEqual = Filters.notEqual("status", "deleted");
        assertNotNull(notEqual);
        assertEquals("status", notEqual.getPropName());
        assertEquals("deleted", notEqual.getPropValue());
    }

    @Test
    public void testNe() {
        NotEqual ne = Filters.ne("status", "inactive");
        assertNotNull(ne);
        assertEquals("status", ne.getPropName());
    }

    @Test
    public void testGreaterThan() {
        GreaterThan gt = Filters.greaterThan("age", 18);
        assertNotNull(gt);
        assertEquals("age", gt.getPropName());
        assertEquals(Integer.valueOf(18), gt.getPropValue());
    }

    @Test
    public void testGt() {
        GreaterThan gt = Filters.gt("score", 90);
        assertNotNull(gt);
        assertEquals("score", gt.getPropName());
    }

    @Test
    public void testGreaterEqual() {
        GreaterEqual ge = Filters.greaterThanOrEqual("age", 21);
        assertNotNull(ge);
        assertEquals("age", ge.getPropName());
    }

    @Test
    public void testGe() {
        GreaterEqual ge = Filters.ge("score", 80);
        assertNotNull(ge);
    }

    @Test
    public void testLessThan() {
        LessThan lt = Filters.lessThan("age", 65);
        assertNotNull(lt);
        assertEquals("age", lt.getPropName());
    }

    @Test
    public void testLt() {
        LessThan lt = Filters.lt("temperature", 100);
        assertNotNull(lt);
    }

    @Test
    public void testLessEqual() {
        LessEqual le = Filters.lessThanOrEqual("age", 60);
        assertNotNull(le);
        assertEquals("age", le.getPropName());
    }

    @Test
    public void testLe() {
        LessEqual le = Filters.le("price", 1000);
        assertNotNull(le);
    }

    @Test
    public void testBetween() {
        Between between = Filters.between("age", 18, 65);
        assertNotNull(between);
        assertEquals("age", between.getPropName());
        assertEquals(Integer.valueOf(18), between.getMinValue());
        assertEquals(Integer.valueOf(65), between.getMaxValue());
    }

    @Test
    public void testBt() {
        Between bt = Filters.bt("price", 100, 1000);
        assertNotNull(bt);
    }

    @Test
    public void testNotBetween() {
        NotBetween notBetween = Filters.notBetween("age", 0, 17);
        assertNotNull(notBetween);
        assertEquals("age", notBetween.getPropName());
    }

    @Test
    public void testLike() {
        Like like = Filters.like("name", "%John%");
        assertNotNull(like);
        assertEquals("name", like.getPropName());
    }

    @Test
    public void testNotLike() {
        NotLike notLike = Filters.notLike("name", "%test%");
        assertNotNull(notLike);
    }

    @Test
    public void testContains() {
        Like contains = Filters.contains("description", "keyword");
        assertNotNull(contains);
    }

    @Test
    public void testNotContains() {
        NotLike notContains = Filters.notContains("description", "spam");
        assertNotNull(notContains);
    }

    @Test
    public void testStartsWith() {
        Like startsWith = Filters.startsWith("name", "John");
        assertNotNull(startsWith);
    }

    @Test
    public void testNotStartsWith() {
        NotLike notStartsWith = Filters.notStartsWith("name", "Test");
        assertNotNull(notStartsWith);
    }

    @Test
    public void testEndsWith() {
        Like endsWith = Filters.endsWith("email", "@example.com");
        assertNotNull(endsWith);
    }

    @Test
    public void testNotEndsWith() {
        NotLike notEndsWith = Filters.notEndsWith("email", "@spam.com");
        assertNotNull(notEndsWith);
    }

    @Test
    public void testIsNull() {
        IsNull isNull = Filters.isNull("deletedAt");
        assertNotNull(isNull);
        assertEquals("deletedAt", isNull.getPropName());
    }

    @Test
    public void testIsEmpty() {
        Or isEmpty = Filters.isEmpty("description");
        assertNotNull(isEmpty);
    }

    @Test
    public void testIsNullOrZero() {
        Or isNullOrZero = Filters.isNullOrZero("count");
        assertNotNull(isNullOrZero);
    }

    @Test
    public void testIsNotNull() {
        IsNotNull isNotNull = Filters.isNotNull("email");
        assertNotNull(isNotNull);
        assertEquals("email", isNotNull.getPropName());
    }

    @Test
    public void testIsNaN() {
        IsNaN isNaN = Filters.isNaN("value");
        assertNotNull(isNaN);
    }

    @Test
    public void testIsNotNaN() {
        IsNotNaN isNotNaN = Filters.isNotNaN("value");
        assertNotNull(isNotNaN);
    }

    @Test
    public void testIsInfinite() {
        IsInfinite isInfinite = Filters.isInfinite("value");
        assertNotNull(isInfinite);
    }

    @Test
    public void testIsNotInfinite() {
        IsNotInfinite isNotInfinite = Filters.isNotInfinite("value");
        assertNotNull(isNotInfinite);
    }

    @Test
    public void testIs() {
        Is is = Filters.is("status", "active");
        assertNotNull(is);
    }

    @Test
    public void testIsNot() {
        IsNot isNot = Filters.isNot("status", "deleted");
        assertNotNull(isNot);
    }

    @Test
    public void testXor() {
        XOR xor = Filters.xor("flag", true);
        assertNotNull(xor);
    }

    @Test
    public void testOrVarargs() {
        Condition cond1 = Filters.equal("name", "John");
        Condition cond2 = Filters.equal("name", "Jane");
        Or or = Filters.or(cond1, cond2);
        assertNotNull(or);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testOrCollection() {
        Condition cond1 = Filters.equal("status", "active");
        Condition cond2 = Filters.equal("status", "pending");
        Or or = Filters.or(Arrays.asList(cond1, cond2));
        assertNotNull(or);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testAndVarargs() {
        Condition cond1 = Filters.equal("status", "active");
        Condition cond2 = Filters.greaterThan("age", 18);
        And and = Filters.and(cond1, cond2);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testAndCollection() {
        Condition cond1 = Filters.equal("verified", true);
        Condition cond2 = Filters.notEqual("status", "deleted");
        And and = Filters.and(Arrays.asList(cond1, cond2));
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testJunction() {
        Condition cond1 = Filters.equal("name", "John");
        Condition cond2 = Filters.equal("age", 30);
        Junction junction = Filters.junction(Operator.OR, cond1, cond2);
        assertNotNull(junction);
    }

    @Test
    public void testWhereCondition() {
        Condition condition = Filters.equal("status", "active");
        Where where = Filters.where(condition);
        assertNotNull(where);
        assertEquals(Operator.WHERE, where.getOperator());
    }

    @Test
    public void testWhereString() {
        Where where = Filters.where("status = 'active'");
        assertNotNull(where);
    }

    @Test
    public void testGroupByVarargs() {
        GroupBy groupBy = Filters.groupBy("department", "year");
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByCollection() {
        GroupBy groupBy = Filters.groupBy(Arrays.asList("region", "category"));
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByWithDirection() {
        GroupBy groupBy = Filters.groupBy(Arrays.asList("year", "month"), SortDirection.DESC);
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByOneColumn() {
        GroupBy groupBy = Filters.groupBy("department", SortDirection.ASC);
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByTwoColumns() {
        GroupBy groupBy = Filters.groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByThreeColumns() {
        GroupBy groupBy = Filters.groupBy("year", SortDirection.DESC, "month", SortDirection.ASC, "day", SortDirection.DESC);
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByMap() {
        Map<String, SortDirection> orders = new HashMap<>();
        orders.put("category", SortDirection.ASC);
        orders.put("priority", SortDirection.DESC);

        GroupBy groupBy = Filters.groupBy(orders);
        assertNotNull(groupBy);
    }

    @Test
    public void testConstants() {
        assertNotNull(SortDirection.ASC);
        assertEquals(SortDirection.ASC, SortDirection.ASC);

        assertNotNull(SortDirection.DESC);
        assertEquals(SortDirection.DESC, SortDirection.DESC);

        assertNotNull(Filters.QME);
    }

    @Test
    public void testPatternForAlphanumericColumnName() {
        assertNotNull(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME);
        assertTrue(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user_name").matches());
        assertTrue(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user123").matches());
        assertTrue(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user-name").matches());
        assertFalse(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user.name").matches());
        assertFalse(QueryUtil.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user name").matches());
    }

    @Test
    public void testComplexConditionChaining() {
        Condition condition = Filters.and(Filters.equal("status", "active"), Filters.greaterThan("age", 18),
                Filters.or(Filters.equal("role", "admin"), Filters.equal("role", "moderator")));
        assertNotNull(condition);
    }

    @Test
    public void testBetweenWithoutValues() {
        Between between = Filters.between("age");
        assertNotNull(between);
    }

    @Test
    public void testNotBetweenWithoutValues() {
        NotBetween notBetween = Filters.notBetween("price");
        assertNotNull(notBetween);
    }

    @Test
    public void testLikeWithoutValue() {
        Like like = Filters.like("name");
        assertNotNull(like);
    }

    @Test
    public void testNotLikeWithoutValue() {
        NotLike notLike = Filters.notLike("email");
        assertNotNull(notLike);
    }

    @Test
    public void testGtAndLtWithoutValues() {
        And and = Filters.gtAndLt("age");
        assertNotNull(and);
    }

    @Test
    public void testGeAndLtWithoutValues() {
        And and = Filters.geAndLt("price");
        assertNotNull(and);
    }

    @Test
    public void testGeAndLeWithoutValues() {
        And and = Filters.geAndLe("score");
        assertNotNull(and);
    }

    @Test
    public void testGtAndLeWithoutValues() {
        And and = Filters.gtAndLe("rating");
        assertNotNull(and);
    }

    @Test
    public void testGreaterThanWithoutValue() {
        GreaterThan gt = Filters.greaterThan("value");
        assertNotNull(gt);
    }

    @Test
    public void testGreaterEqualWithoutValue() {
        GreaterEqual ge = Filters.greaterThanOrEqual("count");
        assertNotNull(ge);
    }

    @Test
    public void testLessThanWithoutValue() {
        LessThan lt = Filters.lessThan("limit");
        assertNotNull(lt);
    }

    @Test
    public void testLessEqualWithoutValue() {
        LessEqual le = Filters.lessThanOrEqual("maximum");
        assertNotNull(le);
    }

    @Test
    public void testNotEqualWithoutValue() {
        NotEqual ne = Filters.notEqual("status");
        assertNotNull(ne);
    }

    @Test
    public void testEqAndOrList() {
        Map<String, Object> props1 = new HashMap<>();
        props1.put("status", "active");
        props1.put("type", "A");

        Map<String, Object> props2 = new HashMap<>();
        props2.put("status", "pending");
        props2.put("type", "B");

        Or or = Filters.eqAndOr(Arrays.asList(props1, props2));
        assertNotNull(or);
    }

    @Test
    public void testEqAndOrEntities() {
        Account account1 = new Account();
        Account account2 = new Account();

        Or or = Filters.eqAndOr(Arrays.asList(account1, account2));
        assertNotNull(or);
    }

    @Test
    public void testEqAndOrEntitiesWithSelectPropNames() {
        Account account1 = new Account();
        Account account2 = new Account();

        Or or = Filters.eqAndOr(Arrays.asList(account1, account2), Arrays.asList("id", "firstName"));
        assertNotNull(or);
    }

    @Test
    public void testOrderByVarargs() {
        com.landawn.abacus.query.condition.OrderBy orderBy = Filters.orderBy("name", "age");
        assertNotNull(orderBy);
    }

    @Test
    public void testOrderByCollection() {
        com.landawn.abacus.query.condition.OrderBy orderBy = Filters.orderBy(Arrays.asList("name", "age"));
        assertNotNull(orderBy);
    }

    @Test
    public void testOrderByWithDirection() {
        com.landawn.abacus.query.condition.OrderBy orderBy = Filters.orderBy(Arrays.asList("name", "age"), SortDirection.DESC);
        assertNotNull(orderBy);
    }

    @Test
    public void testOrderByOneColumn() {
        com.landawn.abacus.query.condition.OrderBy orderBy = Filters.orderBy("name", SortDirection.ASC);
        assertNotNull(orderBy);
    }

    @Test
    public void testOrderByTwoColumns() {
        com.landawn.abacus.query.condition.OrderBy orderBy = Filters.orderBy("name", SortDirection.ASC, "age", SortDirection.DESC);
        assertNotNull(orderBy);
    }

    @Test
    public void testOrderByThreeColumns() {
        com.landawn.abacus.query.condition.OrderBy orderBy = Filters.orderBy("name", SortDirection.ASC, "age", SortDirection.DESC, "email", SortDirection.ASC);
        assertNotNull(orderBy);
    }

    @Test
    public void testOrderByMap() {
        Map<String, SortDirection> orders = new HashMap<>();
        orders.put("name", SortDirection.ASC);
        orders.put("age", SortDirection.DESC);

        com.landawn.abacus.query.condition.OrderBy orderBy = Filters.orderBy(orders);
        assertNotNull(orderBy);
    }

    @Test
    public void testOrderByCondition() {
        Condition condition = Filters.expr("name ASC");
        com.landawn.abacus.query.condition.OrderBy orderBy = Filters.orderBy(condition);
        assertNotNull(orderBy);
    }

    @Test
    public void testHavingCondition() {
        Condition condition = Filters.expr("COUNT(*) > 5");
        com.landawn.abacus.query.condition.Having having = Filters.having(condition);
        assertNotNull(having);
    }

    @Test
    public void testOnCondition() {
        Condition condition = Filters.expr("users.id = orders.user_id");
        com.landawn.abacus.query.condition.On on = Filters.on(condition);
        assertNotNull(on);
    }

    @Test
    public void testOnString() {
        com.landawn.abacus.query.condition.On on = Filters.on("users.id = orders.user_id");
        assertNotNull(on);
    }

    @Test
    public void testOnPropNames() {
        com.landawn.abacus.query.condition.On on = Filters.on("id", "user_id");
        assertNotNull(on);
    }

    @Test
    public void testOnMap() {
        Map<String, String> propNamePair = new HashMap<>();
        propNamePair.put("id", "user_id");

        com.landawn.abacus.query.condition.On on = Filters.on(propNamePair);
        assertNotNull(on);
    }

    @Test
    public void testUsingVarargs() {
        com.landawn.abacus.query.condition.Using using = Filters.using("id", "name");
        assertNotNull(using);
    }

    @Test
    public void testUsingCollection() {
        com.landawn.abacus.query.condition.Using using = Filters.using(Arrays.asList("id", "name"));
        assertNotNull(using);
    }

    @Test
    public void testJoinEntity() {
        com.landawn.abacus.query.condition.Join join = Filters.join("orders");
        assertNotNull(join);
    }

    @Test
    public void testJoinEntityWithCondition() {
        Condition condition = Filters.expr("users.id = orders.user_id");
        com.landawn.abacus.query.condition.Join join = Filters.join("orders", condition);
        assertNotNull(join);
    }

    @Test
    public void testJoinEntitiesWithCondition() {
        Condition condition = Filters.expr("id = user_id");
        com.landawn.abacus.query.condition.Join join = Filters.join(Arrays.asList("orders", "payments"), condition);
        assertNotNull(join);
    }

    @Test
    public void testLeftJoinEntity() {
        com.landawn.abacus.query.condition.LeftJoin leftJoin = Filters.leftJoin("orders");
        assertNotNull(leftJoin);
    }

    @Test
    public void testLeftJoinEntityWithCondition() {
        Condition condition = Filters.expr("users.id = orders.user_id");
        com.landawn.abacus.query.condition.LeftJoin leftJoin = Filters.leftJoin("orders", condition);
        assertNotNull(leftJoin);
    }

    @Test
    public void testLeftJoinEntitiesWithCondition() {
        Condition condition = Filters.expr("id = user_id");
        com.landawn.abacus.query.condition.LeftJoin leftJoin = Filters.leftJoin(Arrays.asList("orders", "payments"), condition);
        assertNotNull(leftJoin);
    }

    @Test
    public void testRightJoinEntity() {
        com.landawn.abacus.query.condition.RightJoin rightJoin = Filters.rightJoin("orders");
        assertNotNull(rightJoin);
    }

    @Test
    public void testRightJoinEntityWithCondition() {
        Condition condition = Filters.expr("users.id = orders.user_id");
        com.landawn.abacus.query.condition.RightJoin rightJoin = Filters.rightJoin("orders", condition);
        assertNotNull(rightJoin);
    }

    @Test
    public void testRightJoinEntitiesWithCondition() {
        Condition condition = Filters.expr("id = user_id");
        com.landawn.abacus.query.condition.RightJoin rightJoin = Filters.rightJoin(Arrays.asList("orders", "payments"), condition);
        assertNotNull(rightJoin);
    }

    @Test
    public void testCrossJoinEntity() {
        com.landawn.abacus.query.condition.CrossJoin crossJoin = Filters.crossJoin("orders");
        assertNotNull(crossJoin);
    }

    @Test
    public void testCrossJoinEntityWithCondition() {
        Condition condition = Filters.expr("users.id = orders.user_id");
        com.landawn.abacus.query.condition.CrossJoin crossJoin = Filters.crossJoin("orders", condition);
        assertNotNull(crossJoin);
    }

    @Test
    public void testCrossJoinEntitiesWithCondition() {
        Condition condition = Filters.expr("id = user_id");
        com.landawn.abacus.query.condition.CrossJoin crossJoin = Filters.crossJoin(Arrays.asList("orders", "payments"), condition);
        assertNotNull(crossJoin);
    }

    @Test
    public void testFullJoinEntity() {
        com.landawn.abacus.query.condition.FullJoin fullJoin = Filters.fullJoin("orders");
        assertNotNull(fullJoin);
    }

    @Test
    public void testFullJoinEntityWithCondition() {
        Condition condition = Filters.expr("users.id = orders.user_id");
        com.landawn.abacus.query.condition.FullJoin fullJoin = Filters.fullJoin("orders", condition);
        assertNotNull(fullJoin);
    }

    @Test
    public void testFullJoinEntitiesWithCondition() {
        Condition condition = Filters.expr("id = user_id");
        com.landawn.abacus.query.condition.FullJoin fullJoin = Filters.fullJoin(Arrays.asList("orders", "payments"), condition);
        assertNotNull(fullJoin);
    }

    @Test
    public void testInnerJoinEntity() {
        com.landawn.abacus.query.condition.InnerJoin innerJoin = Filters.innerJoin("orders");
        assertNotNull(innerJoin);
    }

    @Test
    public void testInnerJoinEntityWithCondition() {
        Condition condition = Filters.expr("users.id = orders.user_id");
        com.landawn.abacus.query.condition.InnerJoin innerJoin = Filters.innerJoin("orders", condition);
        assertNotNull(innerJoin);
    }

    @Test
    public void testInnerJoinEntitiesWithCondition() {
        Condition condition = Filters.expr("id = user_id");
        com.landawn.abacus.query.condition.InnerJoin innerJoin = Filters.innerJoin(Arrays.asList("orders", "payments"), condition);
        assertNotNull(innerJoin);
    }

    @Test
    public void testNaturalJoinEntity() {
        com.landawn.abacus.query.condition.NaturalJoin naturalJoin = Filters.naturalJoin("orders");
        assertNotNull(naturalJoin);
    }

    @Test
    public void testNaturalJoinEntityWithCondition() {
        Condition condition = Filters.expr("users.id = orders.user_id");
        com.landawn.abacus.query.condition.NaturalJoin naturalJoin = Filters.naturalJoin("orders", condition);
        assertNotNull(naturalJoin);
    }

    @Test
    public void testNaturalJoinEntitiesWithCondition() {
        Condition condition = Filters.expr("id = user_id");
        com.landawn.abacus.query.condition.NaturalJoin naturalJoin = Filters.naturalJoin(Arrays.asList("orders", "payments"), condition);
        assertNotNull(naturalJoin);
    }

    @Test
    public void testInIntArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("id", new int[] { 1, 2, 3 });
        assertNotNull(in);
    }

    @Test
    public void testInLongArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("id", new long[] { 1L, 2L, 3L });
        assertNotNull(in);
    }

    @Test
    public void testInDoubleArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("score", new double[] { 1.5, 2.5, 3.5 });
        assertNotNull(in);
    }

    @Test
    public void testInObjectArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("status", new Object[] { "active", "pending" });
        assertNotNull(in);
    }

    @Test
    public void testInCollection() {
        com.landawn.abacus.query.condition.In in = Filters.in("id", Arrays.asList(1, 2, 3));
        assertNotNull(in);
    }

    @Test
    public void testInSubQuery() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        com.landawn.abacus.query.condition.InSubQuery inSubQuery = Filters.in("user_id", subQuery);
        assertNotNull(inSubQuery);
    }

    @Test
    public void testInSubQueryMultipleProps() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT id, name FROM users");
        com.landawn.abacus.query.condition.InSubQuery inSubQuery = Filters.in(Arrays.asList("id", "name"), subQuery);
        assertNotNull(inSubQuery);
    }

    @Test
    public void testNotInIntArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("id", new int[] { 1, 2, 3 });
        assertNotNull(notIn);
    }

    @Test
    public void testNotInLongArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("id", new long[] { 1L, 2L, 3L });
        assertNotNull(notIn);
    }

    @Test
    public void testNotInDoubleArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("score", new double[] { 1.5, 2.5, 3.5 });
        assertNotNull(notIn);
    }

    @Test
    public void testNotInObjectArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("status", new Object[] { "deleted", "archived" });
        assertNotNull(notIn);
    }

    @Test
    public void testNotInCollection() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("id", Arrays.asList(1, 2, 3));
        assertNotNull(notIn);
    }

    @Test
    public void testNotInSubQuery() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT id FROM blocked_users");
        com.landawn.abacus.query.condition.NotInSubQuery notInSubQuery = Filters.notIn("user_id", subQuery);
        assertNotNull(notInSubQuery);
    }

    @Test
    public void testNotInSubQueryMultipleProps() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT id, name FROM blocked");
        com.landawn.abacus.query.condition.NotInSubQuery notInSubQuery = Filters.notIn(Arrays.asList("id", "name"), subQuery);
        assertNotNull(notInSubQuery);
    }

    @Test
    public void testAll() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        com.landawn.abacus.query.condition.All all = Filters.all(subQuery);
        assertNotNull(all);
    }

    @Test
    public void testAny() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        com.landawn.abacus.query.condition.Any any = Filters.any(subQuery);
        assertNotNull(any);
    }

    @Test
    public void testSome() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        com.landawn.abacus.query.condition.Some some = Filters.some(subQuery);
        assertNotNull(some);
    }

    @Test
    public void testExists() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE user_id = users.id");
        com.landawn.abacus.query.condition.Exists exists = Filters.exists(subQuery);
        assertNotNull(exists);
    }

    @Test
    public void testNotExists() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE user_id = users.id");
        com.landawn.abacus.query.condition.NotExists notExists = Filters.notExists(subQuery);
        assertNotNull(notExists);
    }

    @Test
    public void testUnion() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_users");
        com.landawn.abacus.query.condition.Union union = Filters.union(subQuery);
        assertNotNull(union);
    }

    @Test
    public void testUnionAll() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_users");
        com.landawn.abacus.query.condition.UnionAll unionAll = Filters.unionAll(subQuery);
        assertNotNull(unionAll);
    }

    @Test
    public void testExcept() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM blocked_users");
        com.landawn.abacus.query.condition.Except except = Filters.except(subQuery);
        assertNotNull(except);
    }

    @Test
    public void testIntersect() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM premium_users");
        com.landawn.abacus.query.condition.Intersect intersect = Filters.intersect(subQuery);
        assertNotNull(intersect);
    }

    @Test
    public void testMinus() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM inactive_users");
        com.landawn.abacus.query.condition.Minus minus = Filters.minus(subQuery);
        assertNotNull(minus);
    }

    @Test
    public void testCell() {
        Condition condition = Filters.equal("status", "active");
        com.landawn.abacus.query.condition.Cell cell = Filters.cell(Operator.WHERE, condition);
        assertNotNull(cell);
    }

    @Test
    public void testSubQueryWithEntityClass() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery(Account.class, Arrays.asList("id", "name"), Filters.equal("active", true));
        assertNotNull(subQuery);
    }

    @Test
    public void testSubQueryWithEntityName() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id", "name"), Filters.equal("active", true));
        assertNotNull(subQuery);
    }

    @Test
    public void testSubQueryWithEntityNameAndStringCondition() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id", "name"), "active = true");
        assertNotNull(subQuery);
    }

    @Test
    public void testSubQueryWithEntityNameAndSql() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("users", "SELECT id FROM users WHERE active = true");
        assertNotNull(subQuery);
    }

    @Test
    public void testSubQueryWithSql() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery("SELECT * FROM users WHERE active = true");
        assertNotNull(subQuery);
    }

    @Test
    public void testLimitInt() {
        com.landawn.abacus.query.condition.Limit limit = Filters.limit(10);
        assertNotNull(limit);
    }

    @Test
    public void testLimitWithOffset() {
        com.landawn.abacus.query.condition.Limit limit = Filters.limit(10, 20);
        assertNotNull(limit);
    }

    @Test
    public void testLimitExpr() {
        com.landawn.abacus.query.condition.Limit limit = Filters.limit("10 OFFSET 20");
        assertNotNull(limit);
    }

    @Test
    public void testCriteria() {
        com.landawn.abacus.query.condition.Criteria criteria = Filters.criteria();
        assertNotNull(criteria);
    }

    @Test
    public void testCBWhere() {
        com.landawn.abacus.query.condition.Criteria criteria = Filters.CB.where(Filters.equal("status", "active"));
        assertNotNull(criteria);
    }

    @Test
    public void testCBWhereString() {
        com.landawn.abacus.query.condition.Criteria criteria = Filters.CB.where("status = 'active'");
        assertNotNull(criteria);
    }

    @Test
    public void testCBGroupBy() {
        com.landawn.abacus.query.condition.Criteria criteria = Filters.CB.groupBy("department");
        assertNotNull(criteria);
    }

    @Test
    public void testCBHaving() {
        com.landawn.abacus.query.condition.Criteria criteria = Filters.CB.having(Filters.expr("COUNT(*) > 5"));
        assertNotNull(criteria);
    }

    @Test
    public void testCBOrderBy() {
        com.landawn.abacus.query.condition.Criteria criteria = Filters.CB.orderBy("name");
        assertNotNull(criteria);
    }

    @Test
    public void testCBLimit() {
        com.landawn.abacus.query.condition.Criteria criteria = Filters.CB.limit(10);
        assertNotNull(criteria);
    }
}
