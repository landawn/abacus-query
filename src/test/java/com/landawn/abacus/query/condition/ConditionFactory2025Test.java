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

package com.landawn.abacus.query.condition;

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
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.entity.Account;

@Tag("2025")
public class ConditionFactory2025Test extends TestBase {

    @Test
    public void testAlwaysTrue() {
        Expression expr = ConditionFactory.alwaysTrue();
        assertNotNull(expr);
    }

    @Test
    public void testAlwaysFalse() {
        Expression expr = ConditionFactory.alwaysFalse();
        assertNotNull(expr);
    }

    @Test
    public void testNot() {
        Condition condition = ConditionFactory.equal("name", "John");
        Not notCondition = ConditionFactory.not(condition);
        assertNotNull(notCondition);
        assertEquals(Operator.NOT, notCondition.getOperator());
    }

    @Test
    public void testNamedProperty() {
        NamedProperty prop = ConditionFactory.namedProperty("userName");
        assertNotNull(prop);
        assertEquals("userName", prop.propName());
    }

    @Test
    public void testExpr() {
        Expression expr = ConditionFactory.expr("age > 18");
        assertNotNull(expr);
    }

    @Test
    public void testBinary() {
        Binary binary = ConditionFactory.binary("age", Operator.GREATER_THAN, 18);
        assertNotNull(binary);
        assertEquals("age", binary.getPropName());
        assertEquals(Operator.GREATER_THAN, binary.getOperator());
    }

    @Test
    public void testEqual() {
        Equal equal = ConditionFactory.equal("name", "John");
        assertNotNull(equal);
        assertEquals("name", equal.getPropName());
        assertEquals("John", equal.getPropValue());
    }

    @Test
    public void testEqualWithoutValue() {
        Equal equal = ConditionFactory.equal("name");
        assertNotNull(equal);
        assertEquals("name", equal.getPropName());
    }

    @Test
    public void testEq() {
        Equal equal = ConditionFactory.eq("age", 30);
        assertNotNull(equal);
        assertEquals("age", equal.getPropName());
        assertEquals(30, (Integer) equal.getPropValue());
    }

    @Test
    public void testEqWithoutValue() {
        Equal equal = ConditionFactory.eq("status");
        assertNotNull(equal);
    }

    @Test
    public void testEqOrCollection() {
        List<Integer> ages = Arrays.asList(20, 30, 40);
        Or or = ConditionFactory.eqOr("age", ages);
        assertNotNull(or);
        assertFalse(or.getConditions().isEmpty());
    }

    @Test
    public void testEqOrMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 30);

        Or or = ConditionFactory.eqOr(props);
        assertNotNull(or);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testEqOrEntity() {
        Account account = new Account();
        Or or = ConditionFactory.eqOr(account);
        assertNotNull(or);
    }

    @Test
    public void testEqOrTwoProps() {
        Or or = ConditionFactory.eqOr("name", "John", "age", 30);
        assertNotNull(or);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testEqOrThreeProps() {
        Or or = ConditionFactory.eqOr("name", "John", "age", 30, "status", "active");
        assertNotNull(or);
        assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testEqAndMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("status", "active");
        props.put("verified", true);

        And and = ConditionFactory.eqAnd(props);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testEqAndEntity() {
        Account account = new Account();
        And and = ConditionFactory.eqAnd(account);
        assertNotNull(and);
    }

    @Test
    public void testEqAndTwoProps() {
        And and = ConditionFactory.eqAnd("status", "active", "verified", true);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testEqAndThreeProps() {
        And and = ConditionFactory.eqAnd("name", "John", "age", 30, "status", "active");
        assertNotNull(and);
        assertEquals(3, and.getConditions().size());
    }

    @Test
    public void testGtAndLt() {
        And and = ConditionFactory.gtAndLt("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testGeAndLt() {
        And and = ConditionFactory.geAndLt("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testGeAndLe() {
        And and = ConditionFactory.geAndLe("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testGtAndLe() {
        And and = ConditionFactory.gtAndLe("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testNotEqual() {
        NotEqual notEqual = ConditionFactory.notEqual("status", "deleted");
        assertNotNull(notEqual);
        assertEquals("status", notEqual.getPropName());
        assertEquals("deleted", notEqual.getPropValue());
    }

    @Test
    public void testNe() {
        NotEqual ne = ConditionFactory.ne("status", "inactive");
        assertNotNull(ne);
        assertEquals("status", ne.getPropName());
    }

    @Test
    public void testGreaterThan() {
        GreaterThan gt = ConditionFactory.greaterThan("age", 18);
        assertNotNull(gt);
        assertEquals("age", gt.getPropName());
        assertEquals(Integer.valueOf(18), gt.getPropValue());
    }

    @Test
    public void testGt() {
        GreaterThan gt = ConditionFactory.gt("score", 90);
        assertNotNull(gt);
        assertEquals("score", gt.getPropName());
    }

    @Test
    public void testGreaterEqual() {
        GreaterEqual ge = ConditionFactory.greaterEqual("age", 21);
        assertNotNull(ge);
        assertEquals("age", ge.getPropName());
    }

    @Test
    public void testGe() {
        GreaterEqual ge = ConditionFactory.ge("score", 80);
        assertNotNull(ge);
    }

    @Test
    public void testLessThan() {
        LessThan lt = ConditionFactory.lessThan("age", 65);
        assertNotNull(lt);
        assertEquals("age", lt.getPropName());
    }

    @Test
    public void testLt() {
        LessThan lt = ConditionFactory.lt("temperature", 100);
        assertNotNull(lt);
    }

    @Test
    public void testLessEqual() {
        LessEqual le = ConditionFactory.lessEqual("age", 60);
        assertNotNull(le);
        assertEquals("age", le.getPropName());
    }

    @Test
    public void testLe() {
        LessEqual le = ConditionFactory.le("price", 1000);
        assertNotNull(le);
    }

    @Test
    public void testBetween() {
        Between between = ConditionFactory.between("age", 18, 65);
        assertNotNull(between);
        assertEquals("age", between.getPropName());
        assertEquals(Integer.valueOf(18), between.getMinValue());
        assertEquals(Integer.valueOf(65), between.getMaxValue());
    }

    @Test
    public void testBt() {
        Between bt = ConditionFactory.bt("price", 100, 1000);
        assertNotNull(bt);
    }

    @Test
    public void testNotBetween() {
        NotBetween notBetween = ConditionFactory.notBetween("age", 0, 17);
        assertNotNull(notBetween);
        assertEquals("age", notBetween.getPropName());
    }

    @Test
    public void testLike() {
        Like like = ConditionFactory.like("name", "%John%");
        assertNotNull(like);
        assertEquals("name", like.getPropName());
    }

    @Test
    public void testNotLike() {
        NotLike notLike = ConditionFactory.notLike("name", "%test%");
        assertNotNull(notLike);
    }

    @Test
    public void testContains() {
        Like contains = ConditionFactory.contains("description", "keyword");
        assertNotNull(contains);
    }

    @Test
    public void testNotContains() {
        NotLike notContains = ConditionFactory.notContains("description", "spam");
        assertNotNull(notContains);
    }

    @Test
    public void testStartsWith() {
        Like startsWith = ConditionFactory.startsWith("name", "John");
        assertNotNull(startsWith);
    }

    @Test
    public void testNotStartsWith() {
        NotLike notStartsWith = ConditionFactory.notStartsWith("name", "Test");
        assertNotNull(notStartsWith);
    }

    @Test
    public void testEndsWith() {
        Like endsWith = ConditionFactory.endsWith("email", "@example.com");
        assertNotNull(endsWith);
    }

    @Test
    public void testNotEndsWith() {
        NotLike notEndsWith = ConditionFactory.notEndsWith("email", "@spam.com");
        assertNotNull(notEndsWith);
    }

    @Test
    public void testIsNull() {
        IsNull isNull = ConditionFactory.isNull("deletedAt");
        assertNotNull(isNull);
        assertEquals("deletedAt", isNull.getPropName());
    }

    @Test
    public void testIsEmpty() {
        Or isEmpty = ConditionFactory.isEmpty("description");
        assertNotNull(isEmpty);
    }

    @Test
    public void testIsNullOrZero() {
        Or isNullOrZero = ConditionFactory.isNullOrZero("count");
        assertNotNull(isNullOrZero);
    }

    @Test
    public void testIsNotNull() {
        IsNotNull isNotNull = ConditionFactory.isNotNull("email");
        assertNotNull(isNotNull);
        assertEquals("email", isNotNull.getPropName());
    }

    @Test
    public void testIsNaN() {
        IsNaN isNaN = ConditionFactory.isNaN("value");
        assertNotNull(isNaN);
    }

    @Test
    public void testIsNotNaN() {
        IsNotNaN isNotNaN = ConditionFactory.isNotNaN("value");
        assertNotNull(isNotNaN);
    }

    @Test
    public void testIsInfinite() {
        IsInfinite isInfinite = ConditionFactory.isInfinite("value");
        assertNotNull(isInfinite);
    }

    @Test
    public void testIsNotInfinite() {
        IsNotInfinite isNotInfinite = ConditionFactory.isNotInfinite("value");
        assertNotNull(isNotInfinite);
    }

    @Test
    public void testIs() {
        Is is = ConditionFactory.is("status", "active");
        assertNotNull(is);
    }

    @Test
    public void testIsNot() {
        IsNot isNot = ConditionFactory.isNot("status", "deleted");
        assertNotNull(isNot);
    }

    @Test
    public void testXor() {
        XOR xor = ConditionFactory.xor("flag", true);
        assertNotNull(xor);
    }

    @Test
    public void testOrVarargs() {
        Condition cond1 = ConditionFactory.equal("name", "John");
        Condition cond2 = ConditionFactory.equal("name", "Jane");
        Or or = ConditionFactory.or(cond1, cond2);
        assertNotNull(or);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testOrCollection() {
        Condition cond1 = ConditionFactory.equal("status", "active");
        Condition cond2 = ConditionFactory.equal("status", "pending");
        Or or = ConditionFactory.or(Arrays.asList(cond1, cond2));
        assertNotNull(or);
        assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testAndVarargs() {
        Condition cond1 = ConditionFactory.equal("status", "active");
        Condition cond2 = ConditionFactory.greaterThan("age", 18);
        And and = ConditionFactory.and(cond1, cond2);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testAndCollection() {
        Condition cond1 = ConditionFactory.equal("verified", true);
        Condition cond2 = ConditionFactory.notEqual("status", "deleted");
        And and = ConditionFactory.and(Arrays.asList(cond1, cond2));
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testJunction() {
        Condition cond1 = ConditionFactory.equal("name", "John");
        Condition cond2 = ConditionFactory.equal("age", 30);
        Junction junction = ConditionFactory.junction(Operator.OR, cond1, cond2);
        assertNotNull(junction);
    }

    @Test
    public void testWhereCondition() {
        Condition condition = ConditionFactory.equal("status", "active");
        Where where = ConditionFactory.where(condition);
        assertNotNull(where);
        assertEquals(Operator.WHERE, where.getOperator());
    }

    @Test
    public void testWhereString() {
        Where where = ConditionFactory.where("status = 'active'");
        assertNotNull(where);
    }

    @Test
    public void testGroupByVarargs() {
        GroupBy groupBy = ConditionFactory.groupBy("department", "year");
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByCollection() {
        GroupBy groupBy = ConditionFactory.groupBy(Arrays.asList("region", "category"));
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByWithDirection() {
        GroupBy groupBy = ConditionFactory.groupBy(Arrays.asList("year", "month"), SortDirection.DESC);
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByOneColumn() {
        GroupBy groupBy = ConditionFactory.groupBy("department", SortDirection.ASC);
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByTwoColumns() {
        GroupBy groupBy = ConditionFactory.groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByThreeColumns() {
        GroupBy groupBy = ConditionFactory.groupBy("year", SortDirection.DESC, "month", SortDirection.ASC, "day", SortDirection.DESC);
        assertNotNull(groupBy);
    }

    @Test
    public void testGroupByMap() {
        Map<String, SortDirection> orders = new HashMap<>();
        orders.put("category", SortDirection.ASC);
        orders.put("priority", SortDirection.DESC);

        GroupBy groupBy = ConditionFactory.groupBy(orders);
        assertNotNull(groupBy);
    }

    @Test
    public void testConstants() {
        assertNotNull(ConditionFactory.ASC);
        assertEquals(SortDirection.ASC, ConditionFactory.ASC);

        assertNotNull(ConditionFactory.DESC);
        assertEquals(SortDirection.DESC, ConditionFactory.DESC);

        assertNotNull(ConditionFactory.QME);
    }

    @Test
    public void testPatternForAlphanumericColumnName() {
        assertNotNull(ConditionFactory.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME);
        assertTrue(ConditionFactory.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user_name").matches());
        assertTrue(ConditionFactory.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user123").matches());
        assertTrue(ConditionFactory.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user-name").matches());
        assertFalse(ConditionFactory.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user.name").matches());
        assertFalse(ConditionFactory.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user name").matches());
    }

    @Test
    public void testComplexConditionChaining() {
        Condition condition = ConditionFactory.and(ConditionFactory.equal("status", "active"), ConditionFactory.greaterThan("age", 18),
                ConditionFactory.or(ConditionFactory.equal("role", "admin"), ConditionFactory.equal("role", "moderator")));
        assertNotNull(condition);
    }

    @Test
    public void testBetweenWithoutValues() {
        Between between = ConditionFactory.between("age");
        assertNotNull(between);
    }

    @Test
    public void testNotBetweenWithoutValues() {
        NotBetween notBetween = ConditionFactory.notBetween("price");
        assertNotNull(notBetween);
    }

    @Test
    public void testLikeWithoutValue() {
        Like like = ConditionFactory.like("name");
        assertNotNull(like);
    }

    @Test
    public void testNotLikeWithoutValue() {
        NotLike notLike = ConditionFactory.notLike("email");
        assertNotNull(notLike);
    }

    @Test
    public void testGtAndLtWithoutValues() {
        And and = ConditionFactory.gtAndLt("age");
        assertNotNull(and);
    }

    @Test
    public void testGeAndLtWithoutValues() {
        And and = ConditionFactory.geAndLt("price");
        assertNotNull(and);
    }

    @Test
    public void testGeAndLeWithoutValues() {
        And and = ConditionFactory.geAndLe("score");
        assertNotNull(and);
    }

    @Test
    public void testGtAndLeWithoutValues() {
        And and = ConditionFactory.gtAndLe("rating");
        assertNotNull(and);
    }

    @Test
    public void testGreaterThanWithoutValue() {
        GreaterThan gt = ConditionFactory.greaterThan("value");
        assertNotNull(gt);
    }

    @Test
    public void testGreaterEqualWithoutValue() {
        GreaterEqual ge = ConditionFactory.greaterEqual("count");
        assertNotNull(ge);
    }

    @Test
    public void testLessThanWithoutValue() {
        LessThan lt = ConditionFactory.lessThan("limit");
        assertNotNull(lt);
    }

    @Test
    public void testLessEqualWithoutValue() {
        LessEqual le = ConditionFactory.lessEqual("maximum");
        assertNotNull(le);
    }

    @Test
    public void testNotEqualWithoutValue() {
        NotEqual ne = ConditionFactory.notEqual("status");
        assertNotNull(ne);
    }
}
