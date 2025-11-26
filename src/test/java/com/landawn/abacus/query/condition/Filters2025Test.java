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
        GreaterEqual ge = Filters.greaterEqual("age", 21);
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
        LessEqual le = Filters.lessEqual("age", 60);
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
        assertNotNull(Filters.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME);
        assertTrue(Filters.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user_name").matches());
        assertTrue(Filters.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user123").matches());
        assertTrue(Filters.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user-name").matches());
        assertFalse(Filters.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user.name").matches());
        assertFalse(Filters.PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("user name").matches());
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
        GreaterEqual ge = Filters.greaterEqual("count");
        assertNotNull(ge);
    }

    @Test
    public void testLessThanWithoutValue() {
        LessThan lt = Filters.lessThan("limit");
        assertNotNull(lt);
    }

    @Test
    public void testLessEqualWithoutValue() {
        LessEqual le = Filters.lessEqual("maximum");
        assertNotNull(le);
    }

    @Test
    public void testNotEqualWithoutValue() {
        NotEqual ne = Filters.notEqual("status");
        assertNotNull(ne);
    }
}
