package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Binary;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.CrossJoin;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.Exists;
import com.landawn.abacus.query.condition.SqlExpression;
import com.landawn.abacus.query.condition.FullJoin;
import com.landawn.abacus.query.condition.GreaterThan;
import com.landawn.abacus.query.condition.GreaterThanOrEqual;
import com.landawn.abacus.query.condition.GroupBy;
import com.landawn.abacus.query.condition.Having;
import com.landawn.abacus.query.condition.In;
import com.landawn.abacus.query.condition.InSubQuery;
import com.landawn.abacus.query.condition.InnerJoin;
import com.landawn.abacus.query.condition.Is;
import com.landawn.abacus.query.condition.IsInfinite;
import com.landawn.abacus.query.condition.IsNaN;
import com.landawn.abacus.query.condition.IsNot;
import com.landawn.abacus.query.condition.IsNotInfinite;
import com.landawn.abacus.query.condition.IsNotNaN;
import com.landawn.abacus.query.condition.IsNotNull;
import com.landawn.abacus.query.condition.IsNull;
import com.landawn.abacus.query.condition.Join;
import com.landawn.abacus.query.condition.Junction;
import com.landawn.abacus.query.condition.LeftJoin;
import com.landawn.abacus.query.condition.LessThan;
import com.landawn.abacus.query.condition.LessThanOrEqual;
import com.landawn.abacus.query.condition.Like;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.NamedProperty;
import com.landawn.abacus.query.condition.NaturalJoin;
import com.landawn.abacus.query.condition.Not;
import com.landawn.abacus.query.condition.NotBetween;
import com.landawn.abacus.query.condition.NotEqual;
import com.landawn.abacus.query.condition.NotIn;
import com.landawn.abacus.query.condition.NotInSubQuery;
import com.landawn.abacus.query.condition.NotLike;
import com.landawn.abacus.query.condition.On;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Or;
import com.landawn.abacus.query.condition.OrderBy;
import com.landawn.abacus.query.condition.RightJoin;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Using;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.EntityId;

@Tag("2025")
public class FiltersTest extends TestBase {
    @Test
    public void testAlwaysTrue() {
        SqlExpression expr = Filters.alwaysTrue();
        assertNotNull(expr);
    }

    @Test
    public void testAlwaysFalse() {
        SqlExpression expr = Filters.alwaysFalse();
        assertNotNull(expr);
    }

    @Test
    public void testNot() {
        Condition condition = Filters.equal("name", "John");
        Not notCondition = Filters.not(condition);
        assertNotNull(notCondition);
        assertEquals(Operator.NOT, notCondition.operator());
    }

    @Test
    public void testNamedProperty() {
        NamedProperty prop = Filters.namedProperty("userName");
        assertNotNull(prop);
        assertEquals("userName", prop.propName());
    }

    @Test
    public void testExpr() {
        SqlExpression expr = Filters.expr("age > 18");
        assertNotNull(expr);
    }

    @Test
    public void testBinary() {
        Binary binary = Filters.binary("age", Operator.GREATER_THAN, 18);
        assertNotNull(binary);
        assertEquals("age", binary.propName());
        assertEquals(Operator.GREATER_THAN, binary.operator());
    }

    @Test
    public void testBinaryWithMembershipOperatorAndValues() {
        Binary in = Filters.binary("status", Operator.IN, Arrays.asList("NEW", "OPEN"));
        assertEquals(Operator.IN, in.operator());
        assertEquals(Arrays.asList("NEW", "OPEN"), in.parameters());

        Binary notIn = Filters.binary("status", Operator.NOT_IN, new String[] { "CLOSED", "DELETED" });
        assertEquals(Operator.NOT_IN, notIn.operator());
        assertEquals(Arrays.asList("CLOSED", "DELETED"), notIn.parameters());
    }

    @Test
    public void testBinaryParameterized() {
        Binary binary = Filters.binary("price", Operator.GREATER_THAN);
        assertNotNull(binary);
        assertEquals("price", binary.propName());
        assertEquals(Operator.GREATER_THAN, binary.operator());
        assertSame(Filters.QME, binary.propValue());
        // Renders identically to the dedicated parameterized factory for the same operator.
        assertEquals(Filters.greaterThan("price").toString(), binary.toString());
    }

    @Test
    public void testBinaryParameterizedInvalidArgsThrow() {
        assertThrows(IllegalArgumentException.class, () -> Filters.binary("", Operator.GREATER_THAN));
        assertThrows(IllegalArgumentException.class, () -> Filters.binary("price", Operator.WHERE));
    }

    @Test
    public void testEqual() {
        Equal equal = Filters.equal("name", "John");
        assertNotNull(equal);
        assertEquals("name", equal.propName());
        assertEquals("John", equal.propValue());
    }

    @Test
    public void testEqualWithoutValue() {
        Equal equal = Filters.equal("name");
        assertNotNull(equal);
        assertEquals("name", equal.propName());
    }

    @Test
    public void testEq() {
        Equal equal = Filters.eq("age", 30);
        assertNotNull(equal);
        assertEquals("age", equal.propName());
        assertEquals(30, (Integer) equal.propValue());
    }

    @Test
    public void testEqWithoutValue() {
        Equal equal = Filters.eq("status");
        assertNotNull(equal);
    }

    @Test
    public void testInCollectionForAge() {
        List<Integer> ages = Arrays.asList(20, 30, 40);
        In in = Filters.in("age", ages);
        assertNotNull(in);
        assertFalse(in.values().isEmpty());
    }

    @Test
    public void testAnyEqualMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 30);

        Or or = Filters.anyEqual(props);
        assertNotNull(or);
        assertEquals(2, or.conditions().size());
    }

    @Test
    public void testAnyEqualEntity() {
        Account account = new Account();
        Or or = Filters.anyEqual(account);
        assertNotNull(or);
    }

    // Verify entity overloads honor the explicitly selected property subset.
    @Test
    public void testAnyEqualEntityWithSelectProps() {
        Account account = new Account().setId(7L).setFirstName("Jane").setStatus(3);

        Or or = Filters.anyEqual(account, Arrays.asList("id", "firstName"));

        assertEquals(2, or.conditions().size());
        assertEquals("id", ((Equal) or.conditions().get(0)).propName());
        assertEquals(Long.valueOf(7L), ((Equal) or.conditions().get(0)).propValue());
        assertEquals("firstName", ((Equal) or.conditions().get(1)).propName());
        assertEquals("Jane", ((Equal) or.conditions().get(1)).propValue());
    }

    @Test
    public void testAnyEqualEntityWithSelectPropsRejectsNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> Filters.anyEqual(null, Arrays.asList("id")));
    }

    @Test
    public void testAnyEqualEntityWithSelectProps_EmptySelectPropNames() {
        Account account = new Account().setId(7L);

        assertThrows(IllegalArgumentException.class, () -> Filters.anyEqual(account, Arrays.asList()));
    }

    @Test
    public void testAnyEqualTwoProps() {
        Or or = Filters.anyEqual("name", "John", "age", 30);
        assertNotNull(or);
        assertEquals(2, or.conditions().size());
    }

    @Test
    public void testAnyEqualThreeProps() {
        Or or = Filters.anyEqual("name", "John", "age", 30, "status", "active");
        assertNotNull(or);
        assertEquals(3, or.conditions().size());
    }

    @Test
    public void testAllEqualMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("status", "active");
        props.put("verified", true);

        And and = Filters.allEqual(props);
        assertNotNull(and);
        assertEquals(2, and.conditions().size());
    }

    @Test
    public void testAllEqualEntity() {
        Account account = new Account();
        And and = Filters.allEqual(account);
        assertNotNull(and);
    }

    // Verify entity overloads honor the explicitly selected property subset.
    @Test
    public void testAllEqualEntityWithSelectProps() {
        Account account = new Account().setId(9L).setLastName("Doe").setStatus(5);

        And and = Filters.allEqual(account, Arrays.asList("id", "lastName"));

        assertEquals(2, and.conditions().size());
        assertEquals("id", ((Equal) and.conditions().get(0)).propName());
        assertEquals(Long.valueOf(9L), ((Equal) and.conditions().get(0)).propValue());
        assertEquals("lastName", ((Equal) and.conditions().get(1)).propName());
        assertEquals("Doe", ((Equal) and.conditions().get(1)).propValue());
    }

    @Test
    public void testAllEqualEntityWithSelectPropsRejectsNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> Filters.allEqual(null, Arrays.asList("id")));
    }

    @Test
    public void testAllEqualEntityWithSelectProps_EmptySelectPropNames() {
        Account account = new Account().setId(9L);

        assertThrows(IllegalArgumentException.class, () -> Filters.allEqual(account, Arrays.asList()));
    }

    @Test
    public void testAllEqualTwoProps() {
        And and = Filters.allEqual("status", "active", "verified", true);
        assertNotNull(and);
        assertEquals(2, and.conditions().size());
    }

    @Test
    public void testAllEqualThreeProps() {
        And and = Filters.allEqual("name", "John", "age", 30, "status", "active");
        assertNotNull(and);
        assertEquals(3, and.conditions().size());
    }

    @Test
    public void testGtAndLt() {
        And and = Filters.gtAndLt("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.conditions().size());
    }

    @Test
    public void testGeAndLt() {
        And and = Filters.geAndLt("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.conditions().size());
    }

    @Test
    public void testGeAndLe() {
        And and = Filters.geAndLe("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.conditions().size());
    }

    @Test
    public void testGtAndLe() {
        And and = Filters.gtAndLe("age", 18, 65);
        assertNotNull(and);
        assertEquals(2, and.conditions().size());
    }

    @Test
    public void testNotEqual() {
        NotEqual notEqual = Filters.notEqual("status", "deleted");
        assertNotNull(notEqual);
        assertEquals("status", notEqual.propName());
        assertEquals("deleted", notEqual.propValue());
    }

    @Test
    public void testNe() {
        NotEqual ne = Filters.ne("status", "inactive");
        assertNotNull(ne);
        assertEquals("status", ne.propName());
    }

    @Test
    public void testGreaterThan() {
        GreaterThan gt = Filters.greaterThan("age", 18);
        assertNotNull(gt);
        assertEquals("age", gt.propName());
        assertEquals(Integer.valueOf(18), gt.propValue());
    }

    @Test
    public void testGt() {
        GreaterThan gt = Filters.gt("score", 90);
        assertNotNull(gt);
        assertEquals("score", gt.propName());
    }

    @Test
    public void testGreaterThanOrEqual() {
        GreaterThanOrEqual ge = Filters.greaterThanOrEqual("age", 21);
        assertNotNull(ge);
        assertEquals("age", ge.propName());
    }

    @Test
    public void testGe() {
        GreaterThanOrEqual ge = Filters.ge("score", 80);
        assertNotNull(ge);
    }

    @Test
    public void testLessThan() {
        LessThan lt = Filters.lessThan("age", 65);
        assertNotNull(lt);
        assertEquals("age", lt.propName());
    }

    @Test
    public void testLt() {
        LessThan lt = Filters.lt("temperature", 100);
        assertNotNull(lt);
    }

    @Test
    public void testLessThanOrEqual() {
        LessThanOrEqual le = Filters.lessThanOrEqual("age", 60);
        assertNotNull(le);
        assertEquals("age", le.propName());
    }

    @Test
    public void testLe() {
        LessThanOrEqual le = Filters.le("price", 1000);
        assertNotNull(le);
    }

    @Test
    public void testBetween() {
        Between between = Filters.between("age", 18, 65);
        assertNotNull(between);
        assertEquals("age", between.propName());
        assertEquals(Integer.valueOf(18), between.minValue());
        assertEquals(Integer.valueOf(65), between.maxValue());
    }

    // Removed: testBt() - bt() methods have been removed. Use between() instead.

    @Test
    public void testNotBetween() {
        NotBetween notBetween = Filters.notBetween("age", 0, 17);
        assertNotNull(notBetween);
        assertEquals("age", notBetween.propName());
    }

    @Test
    public void testLike() {
        Like like = Filters.like("name", "%John%");
        assertNotNull(like);
        assertEquals("name", like.propName());
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
        assertEquals("deletedAt", isNull.propName());
    }

    @Test
    public void testIsEmpty() {
        Or isEmpty = Filters.isNullOrEmpty("description");
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
        assertEquals("email", isNotNull.propName());
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
    public void testOrVarargs() {
        Condition cond1 = Filters.equal("name", "John");
        Condition cond2 = Filters.equal("name", "Jane");
        Or or = Filters.or(cond1, cond2);
        assertNotNull(or);
        assertEquals(2, or.conditions().size());
    }

    @Test
    public void testOrCollection() {
        Condition cond1 = Filters.equal("status", "active");
        Condition cond2 = Filters.equal("status", "pending");
        Or or = Filters.or(Arrays.asList(cond1, cond2));
        assertNotNull(or);
        assertEquals(2, or.conditions().size());
    }

    @Test
    public void testAndVarargs() {
        Condition cond1 = Filters.equal("status", "active");
        Condition cond2 = Filters.greaterThan("age", 18);
        And and = Filters.and(cond1, cond2);
        assertNotNull(and);
        assertEquals(2, and.conditions().size());
    }

    @Test
    public void testAndCollection() {
        Condition cond1 = Filters.equal("verified", true);
        Condition cond2 = Filters.notEqual("status", "deleted");
        And and = Filters.and(Arrays.asList(cond1, cond2));
        assertNotNull(and);
        assertEquals(2, and.conditions().size());
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
        assertEquals(Operator.WHERE, where.operator());
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
    public void testGroupByAsc() {
        GroupBy groupBy = Filters.groupByAsc("department", "year");
        assertNotNull(groupBy);
        assertEquals(Operator.GROUP_BY, groupBy.operator());
    }

    @Test
    public void testGroupByDesc() {
        GroupBy groupBy = Filters.groupByDesc("sales", "region");
        assertNotNull(groupBy);
        assertEquals(Operator.GROUP_BY, groupBy.operator());
    }

    @Test
    public void testGroupByAscSingleColumn() {
        GroupBy groupBy = Filters.groupByAsc("department");
        assertNotNull(groupBy);
        assertEquals(Operator.GROUP_BY, groupBy.operator());
        assertTrue(groupBy.toString().contains("department ASC"), groupBy.toString());
    }

    @Test
    public void testGroupByAscCollection() {
        GroupBy collForm = Filters.groupByAsc(Arrays.asList("department", "year"));
        GroupBy varargForm = Filters.groupByAsc("department", "year");
        assertNotNull(collForm);
        assertEquals(Operator.GROUP_BY, collForm.operator());
        assertEquals(varargForm.toString(), collForm.toString(), "groupByAsc(Collection) must match groupByAsc(String...)");
    }

    @Test
    public void testGroupByDescSingleColumn() {
        GroupBy groupBy = Filters.groupByDesc("sales");
        assertNotNull(groupBy);
        assertEquals(Operator.GROUP_BY, groupBy.operator());
        assertTrue(groupBy.toString().contains("sales DESC"), groupBy.toString());
    }

    @Test
    public void testGroupByDescCollection() {
        GroupBy collForm = Filters.groupByDesc(Arrays.asList("sales", "region"));
        GroupBy varargForm = Filters.groupByDesc("sales", "region");
        assertNotNull(collForm);
        assertEquals(Operator.GROUP_BY, collForm.operator());
        assertEquals(varargForm.toString(), collForm.toString(), "groupByDesc(Collection) must match groupByDesc(String...)");
    }

    @Test
    public void testOrderByAscDescSingleColumn() {
        OrderBy asc = Filters.orderByAsc("created_date");
        assertNotNull(asc);
        assertEquals(Operator.ORDER_BY, asc.operator());
        assertTrue(asc.toString().contains("created_date ASC"), asc.toString());

        OrderBy desc = Filters.orderByDesc("score");
        assertNotNull(desc);
        assertEquals(Operator.ORDER_BY, desc.operator());
        assertTrue(desc.toString().contains("score DESC"), desc.toString());
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
    public void testGreaterThanOrEqualWithoutValue() {
        GreaterThanOrEqual ge = Filters.greaterThanOrEqual("count");
        assertNotNull(ge);
    }

    @Test
    public void testLessThanWithoutValue() {
        LessThan lt = Filters.lessThan("limit");
        assertNotNull(lt);
    }

    @Test
    public void testLessThanOrEqualWithoutValue() {
        LessThanOrEqual le = Filters.lessThanOrEqual("maximum");
        assertNotNull(le);
    }

    @Test
    public void testNotEqualWithoutValue() {
        NotEqual ne = Filters.notEqual("status");
        assertNotNull(ne);
    }

    @Test
    public void testAnyOfAllEqualMapList() {
        Map<String, Object> props1 = new HashMap<>();
        props1.put("status", "active");
        props1.put("type", "A");

        Map<String, Object> props2 = new HashMap<>();
        props2.put("status", "pending");
        props2.put("type", "B");

        Or or = Filters.anyOfAllEqual(Arrays.asList(props1, props2));
        assertNotNull(or);
    }

    @Test
    public void testAnyOfAllEqualMapSet() {
        Map<String, Object> props1 = new LinkedHashMap<>();
        props1.put("status", "active");
        props1.put("type", "A");

        Map<String, Object> props2 = new LinkedHashMap<>();
        props2.put("status", "pending");
        props2.put("type", "B");

        LinkedHashSet<Map<String, Object>> propsSet = new LinkedHashSet<>(Arrays.asList(props1, props2));

        Or or = Filters.anyOfAllEqual(propsSet);

        assertEquals(2, or.conditions().size());
        assertTrue(or.parameters().containsAll(Arrays.asList("active", "A", "pending", "B")));
    }

    @Test
    public void testAnyOfAllEqualEntities() {
        Account account1 = new Account();
        Account account2 = new Account();

        Or or = Filters.anyOfAllEqual(Arrays.asList(account1, account2));
        assertNotNull(or);
    }

    @Test
    public void testAnyOfAllEqualEntitiesWithSelectPropNames() {
        Account account1 = new Account();
        Account account2 = new Account();

        Or or = Filters.anyOfAllEqual(Arrays.asList(account1, account2), Arrays.asList("id", "firstName"));
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
    public void testCrossJoinEntities() {
        com.landawn.abacus.query.condition.CrossJoin crossJoin = Filters.crossJoin(Arrays.asList("orders", "payments"));
        assertEquals(2, crossJoin.joinEntities().size());
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
    public void testNaturalJoinEntities() {
        com.landawn.abacus.query.condition.NaturalJoin naturalJoin = Filters.naturalJoin(Arrays.asList("orders", "payments"));
        assertEquals(2, naturalJoin.joinEntities().size());
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
    public void testInFloatArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("ratio", new float[] { 0.25f, 0.5f, 0.75f });
        assertNotNull(in);
        assertEquals(3, in.parameters().size());
    }

    @Test
    public void testInShortArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("level", new short[] { 1, 2, 3 });
        assertNotNull(in);
        assertEquals(3, in.parameters().size());
    }

    @Test
    public void testInByteArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("flag", new byte[] { 0, 1, 2 });
        assertNotNull(in);
        assertEquals(3, in.parameters().size());
    }

    @Test
    public void testInBooleanArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("active", new boolean[] { true, false });
        assertNotNull(in);
        assertEquals(2, in.parameters().size());
    }

    @Test
    public void testInCharArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("grade", new char[] { 'A', 'B', 'C' });
        assertNotNull(in);
        assertEquals(3, in.parameters().size());
    }

    @Test
    public void testInObjectArray() {
        com.landawn.abacus.query.condition.In in = Filters.in("status", new Object[] { "active", "pending" });
        assertNotNull(in);
    }

    @Test
    public void testInObjectArrayNullDelegatesToValidation() {
        assertThrows(IllegalArgumentException.class, () -> Filters.in("status", (Object[]) null));
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
    public void testNotInFloatArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("ratio", new float[] { 0.0f, 1.0f });
        assertNotNull(notIn);
        assertEquals(2, notIn.parameters().size());
    }

    @Test
    public void testNotInShortArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("level", new short[] { 0, 9 });
        assertNotNull(notIn);
        assertEquals(2, notIn.parameters().size());
    }

    @Test
    public void testNotInByteArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("flag", new byte[] { 0, 1 });
        assertNotNull(notIn);
        assertEquals(2, notIn.parameters().size());
    }

    @Test
    public void testNotInBooleanArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("active", new boolean[] { false });
        assertNotNull(notIn);
        assertEquals(1, notIn.parameters().size());
    }

    @Test
    public void testNotInCharArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("grade", new char[] { 'D', 'F' });
        assertNotNull(notIn);
        assertEquals(2, notIn.parameters().size());
    }

    @Test
    public void testNotInObjectArray() {
        com.landawn.abacus.query.condition.NotIn notIn = Filters.notIn("status", new Object[] { "deleted", "archived" });
        assertNotNull(notIn);
    }

    @Test
    public void testNotInObjectArrayNullDelegatesToValidation() {
        assertThrows(IllegalArgumentException.class, () -> Filters.notIn("status", (Object[]) null));
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

    //    @Test
    //    public void testCell() {
    //        Condition condition = Filters.equal("status", "active");
    //        com.landawn.abacus.query.condition.Cell cell = Filters.cell(Operator.WHERE, condition);
    //        assertNotNull(cell);
    //    }

    @Test
    public void testSubQueryWithEntityClass() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery(Account.class, Arrays.asList("id", "name"), Filters.equal("active", true));
        assertNotNull(subQuery);
    }

    @Test
    public void testSubQueryWithEntityClassAndStringCondition() {
        com.landawn.abacus.query.condition.SubQuery subQuery = Filters.subQuery(Account.class, Arrays.asList("id", "name"), "active = true");
        assertNotNull(subQuery);
        assertEquals(Account.class, subQuery.entityClass());
        // Mirrors the entityName-based overload: the raw string becomes an SqlExpression condition.
        assertEquals(Filters.subQuery("Account", Arrays.asList("id", "name"), "active = true").condition().toString(), subQuery.condition().toString());
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
        com.landawn.abacus.query.condition.Limit limit = Filters.limit(20, 10);
        assertNotNull(limit);
    }

    @Test
    public void testLimitExpr() {
        com.landawn.abacus.query.condition.Limit limit = Filters.limit("10 OFFSET 20");
        assertNotNull(limit);
    }

    @Test
    public void testCBWhere() {
        Criteria criteria = Criteria.builder().where(Filters.equal("status", "active")).build();
        assertNotNull(criteria);
    }

    @Test
    public void testCBWhereString() {
        Criteria criteria = Criteria.builder().where("status = 'active'").build();
        assertNotNull(criteria);
    }

    @Test
    public void testCBGroupBy() {
        Criteria criteria = Criteria.builder().groupBy("department").build();
        assertNotNull(criteria);
    }

    @Test
    public void testCBHaving() {
        Criteria criteria = Criteria.builder().having(Filters.expr("COUNT(*) > 5")).build();
        assertNotNull(criteria);
    }

    @Test
    public void testCBOrderBy() {
        Criteria criteria = Criteria.builder().orderBy("name").build();
        assertNotNull(criteria);
    }

    @Test
    public void testCBLimit() {
        Criteria criteria = Criteria.builder().limit(10).build();
        assertNotNull(criteria);
    }

    @Test
    public void testLimit_002() {
        String literal = "OFFSET 5 ROWS FETCH NEXT 20 ROWS ONLY";

        Limit limit = Filters.limit(literal);

        // The SQL:2008 FETCH form is parsed into concrete count/offset while its literal is retained.
        assertEquals(literal, limit.expression());
        assertEquals(20, limit.count());
        assertEquals(5, limit.offset());
    }

    @Test
    public void testId2Cond() {
        final EntityId entityId = EntityId.of("userId", 1, "orderId", 100);

        final And condition = Filters.id2Cond(entityId);

        assertEquals(Operator.AND, condition.operator());
        assertEquals(2, condition.conditions().size());
        assertEquals(2, condition.parameters().size());
        Assertions.assertTrue(condition.parameters().containsAll(Arrays.asList(1, 100)));
    }

    @Test
    public void testId2Cond_SingleProperty() {
        final EntityId entityId = EntityId.of("userId", 1);

        final And condition = Filters.id2Cond(entityId);

        assertEquals(1, condition.conditions().size());
        assertEquals(Arrays.asList(1), condition.parameters());
    }

    @Test
    public void testId2Cond_NullEntityId() {
        assertThrows(IllegalArgumentException.class, () -> Filters.id2Cond((EntityId) null));
    }

    @Test
    public void testId2Cond_Collection() {
        final List<EntityId> entityIds = Arrays.asList(EntityId.of("userId", 1, "orderId", 100), EntityId.of("userId", 2, "orderId", 200));

        final Or condition = Filters.id2Cond(entityIds);

        assertEquals(Operator.OR, condition.operator());
        assertEquals(2, condition.conditions().size());
        assertEquals(4, condition.parameters().size());
        Assertions.assertTrue(condition.parameters().containsAll(Arrays.asList(1, 100, 2, 200)));
    }

    @Test
    public void testId2Cond_CollectionEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Filters.id2Cond(Arrays.<EntityId> asList()));
    }

    @Test
    public void testIdToCond() {
        final EntityId entityId = EntityId.of("userId", 1, "orderId", 100);

        final And condition = Filters.idToCond(entityId);

        assertEquals(Operator.AND, condition.operator());
        assertEquals(2, condition.conditions().size());
        assertEquals(2, condition.parameters().size());
        Assertions.assertTrue(condition.parameters().containsAll(Arrays.asList(1, 100)));
    }

    @Test
    public void testIdToCond_SingleProperty() {
        final EntityId entityId = EntityId.of("userId", 1);

        final And condition = Filters.idToCond(entityId);

        assertEquals(1, condition.conditions().size());
        assertEquals(Arrays.asList(1), condition.parameters());
    }

    @Test
    public void testIdToCond_NullEntityId() {
        assertThrows(IllegalArgumentException.class, () -> Filters.idToCond((EntityId) null));
    }

    @Test
    public void testIdToCond_Collection() {
        final List<EntityId> entityIds = Arrays.asList(EntityId.of("userId", 1, "orderId", 100), EntityId.of("userId", 2, "orderId", 200));

        final Or condition = Filters.idToCond(entityIds);

        assertEquals(Operator.OR, condition.operator());
        assertEquals(2, condition.conditions().size());
        assertEquals(4, condition.parameters().size());
        Assertions.assertTrue(condition.parameters().containsAll(Arrays.asList(1, 100, 2, 200)));
    }

    @Test
    public void testIdToCond_CollectionEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Filters.idToCond(Arrays.<EntityId> asList()));
    }

    // Exercise the size-specific overload branches for map/entity conversion helpers.
    @Test
    public void testAnyEqualMap_SingleEntry() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("status", "ACTIVE");

        final Or condition = Filters.anyEqual(props);

        assertEquals(1, condition.conditions().size());
        assertEquals("status", ((Equal) condition.conditions().get(0)).propName());
    }

    @Test
    public void testAnyEqualMap_ThreeEntries() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("status", "ACTIVE");
        props.put("type", "PREMIUM");
        props.put("verified", true);

        final Or condition = Filters.anyEqual(props);

        assertEquals(3, condition.conditions().size());
        assertEquals("status", ((Equal) condition.conditions().get(0)).propName());
        assertEquals("verified", ((Equal) condition.conditions().get(2)).propName());
    }

    @Test
    public void testAllEqualMap_SingleEntry() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("status", "ACTIVE");

        final And condition = Filters.allEqual(props);

        assertEquals(1, condition.conditions().size());
        assertEquals("status", ((Equal) condition.conditions().get(0)).propName());
    }

    @Test
    public void testAllEqualMap_ThreeEntries() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("status", "ACTIVE");
        props.put("type", "PREMIUM");
        props.put("verified", true);

        final And condition = Filters.allEqual(props);

        assertEquals(3, condition.conditions().size());
        assertEquals("status", ((Equal) condition.conditions().get(0)).propName());
        assertEquals("verified", ((Equal) condition.conditions().get(2)).propName());
    }

    @Test
    public void testAnyEqualEntityWithSelectProps_SingleSelectProp() {
        final Account account = new Account().setId(17L).setFirstName("Jane");

        final Or condition = Filters.anyEqual(account, Arrays.asList("id"));

        assertEquals(1, condition.conditions().size());
        assertEquals(Long.valueOf(17L), ((Equal) condition.conditions().get(0)).propValue());
    }

    @Test
    public void testAllEqualEntityWithSelectProps_ThreeSelectProps() {
        final Account account = new Account().setId(23L).setFirstName("Jane").setLastName("Doe");

        final And condition = Filters.allEqual(account, Arrays.asList("id", "firstName", "lastName"));

        assertEquals(3, condition.conditions().size());
        assertEquals("id", ((Equal) condition.conditions().get(0)).propName());
        assertEquals("lastName", ((Equal) condition.conditions().get(2)).propName());
    }

    @Test
    public void testAnyOfAllEqual_AllNullEntities() {
        assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(Arrays.asList(null, null)));
    }

    @Test
    public void testAnyOfAllEqualWithSelectProps_NullEntityIgnored() {
        final Account account = new Account().setId(29L).setFirstName("Alex");

        final Or condition = Filters.anyOfAllEqual(Arrays.asList(null, account, null), Arrays.asList("id", "firstName"));

        assertEquals(1, condition.conditions().size());
        assertEquals(2, condition.parameters().size());
    }

    @Test
    public void testId2Cond_ThreeProperties() {
        final EntityId entityId = EntityId.of("tenantId", 1, "userId", 2, "orderId", 3);

        final And condition = Filters.id2Cond(entityId);

        assertEquals(3, condition.conditions().size());
        Assertions.assertTrue(condition.parameters().containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testIsNotNullAndNotEmpty() {
        final And condition = Filters.isNotNullAndNotEmpty("email");

        assertEquals(Operator.AND, condition.operator());
        assertEquals(2, condition.conditions().size());
        Assertions.assertTrue(condition.toString().contains("IS NOT NULL"));
        Assertions.assertTrue(condition.toString().contains("!= ''"));
    }

    @Test
    public void testIsNotNullAndNotZero() {
        final And condition = Filters.isNotNullAndNotZero("quantity");

        assertEquals(Operator.AND, condition.operator());
        assertEquals(2, condition.conditions().size());
        Assertions.assertTrue(condition.toString().contains("IS NOT NULL"));
        Assertions.assertTrue(condition.toString().contains("!= 0"));
    }

    @Test
    public void testAnyEqualWithArray() {
        Or or = Filters.anyEqual("status", "active", "pending", "approved");
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.conditions().size());
    }

    @Test
    public void testAnyEqualWithMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 25);
        props.put("status", "active");

        Or or = Filters.anyEqual(props);
        Assertions.assertNotNull(or);
        Assertions.assertEquals(3, or.conditions().size());
    }

    @Test
    public void testAnyEqualWithTwoProperties() {
        Or or = Filters.anyEqual("name", "John", "age", 25);
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.conditions().size());
    }

    @Test
    public void testAnyEqualWithThreeProperties() {
        Or or = Filters.anyEqual("name", "John", "age", 25, "status", "active");
        Assertions.assertNotNull(or);
        Assertions.assertEquals(3, or.conditions().size());
    }

    @Test
    public void testAllEqualWithMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 25);

        And and = Filters.allEqual(props);
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.conditions().size());
    }

    @Test
    public void testAllEqualWithTwoProperties() {
        And and = Filters.allEqual("name", "John", "age", 25);
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.conditions().size());
    }

    @Test
    public void testAllEqualWithThreeProperties() {
        And and = Filters.allEqual("name", "John", "age", 25, "status", "active");
        Assertions.assertNotNull(and);
        Assertions.assertEquals(3, and.conditions().size());
    }

    @Test
    public void testOr() {
        // Test with array of conditions
        Condition cond1 = Filters.eq("status", "active");
        Condition cond2 = Filters.eq("status", "pending");
        Or or1 = Filters.or(cond1, cond2);
        Assertions.assertNotNull(or1);
        Assertions.assertEquals(2, or1.conditions().size());

        // Test with collection of conditions
        List<Condition> conditions = Arrays.asList(cond1, cond2);
        Or or2 = Filters.or(conditions);
        Assertions.assertNotNull(or2);
        Assertions.assertEquals(2, or2.conditions().size());
    }

    @Test
    public void testAnd() {
        // Test with array of conditions
        Condition cond1 = Filters.gt("age", 18);
        Condition cond2 = Filters.lt("age", 65);
        And and1 = Filters.and(cond1, cond2);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.conditions().size());

        // Test with collection of conditions
        List<Condition> conditions = Arrays.asList(cond1, cond2);
        And and2 = Filters.and(conditions);
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.conditions().size());
    }

    @Test
    public void testWhere() {
        // Test with condition
        Condition condition = Filters.eq("status", "active");
        Where where1 = Filters.where(condition);
        Assertions.assertNotNull(where1);
        Assertions.assertEquals(condition, where1.condition());

        // Test with string
        Where where2 = Filters.where("age > 18");
        Assertions.assertNotNull(where2);
        Assertions.assertNotNull(where2.condition());
    }

    @Test
    public void testGroupBy() {
        // Test with varargs
        GroupBy gb1 = Filters.groupBy("department", "team");
        Assertions.assertNotNull(gb1);

        // Test with collection
        List<String> props = Arrays.asList("category", "subcategory");
        GroupBy gb2 = Filters.groupBy(props);
        Assertions.assertNotNull(gb2);

        // Test with collection and direction
        GroupBy gb3 = Filters.groupBy(props, SortDirection.DESC);
        Assertions.assertNotNull(gb3);

        // Test with single property and direction
        GroupBy gb4 = Filters.groupBy("name", SortDirection.ASC);
        Assertions.assertNotNull(gb4);

        // Test with map
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("year", SortDirection.DESC);
        orders.put("month", SortDirection.ASC);
        GroupBy gb5 = Filters.groupBy(orders);
        Assertions.assertNotNull(gb5);

        // Test with condition
        Condition condition = Filters.expr("department");
        GroupBy gb6 = Filters.groupBy(condition);
        Assertions.assertNotNull(gb6);
    }

    @Test
    public void testGroupByMultiplePropertiesWithDirections() {
        // Test with 2 properties
        GroupBy gb1 = Filters.groupBy("prop1", SortDirection.ASC, "prop2", SortDirection.DESC);
        Assertions.assertNotNull(gb1);

        // Test with 3 properties
        GroupBy gb2 = Filters.groupBy("prop1", SortDirection.ASC, "prop2", SortDirection.DESC, "prop3", SortDirection.ASC);
        Assertions.assertNotNull(gb2);
    }

    @Test
    public void testHaving() {
        // Test with condition
        Condition condition = Filters.gt("COUNT(*)", 5);
        Having having1 = Filters.having(condition);
        Assertions.assertNotNull(having1);
        Assertions.assertEquals(condition, having1.condition());

        // Test with string
        Having having2 = Filters.having("COUNT(*) > 10");
        Assertions.assertNotNull(having2);
        Assertions.assertNotNull(having2.condition());
    }

    @Test
    public void testOrderBy() {
        // Test with varargs
        OrderBy ob1 = Filters.orderBy("name", "age");
        Assertions.assertNotNull(ob1);

        // Test orderByAsc with varargs
        OrderBy ob2 = Filters.orderByAsc("firstName", "lastName");
        Assertions.assertNotNull(ob2);

        // Test orderByAsc with collection
        List<String> props = Arrays.asList("category", "price");
        OrderBy ob3 = Filters.orderByAsc(props);
        Assertions.assertNotNull(ob3);

        // Test orderByDesc with varargs
        OrderBy ob4 = Filters.orderByDesc("createdAt", "id");
        Assertions.assertNotNull(ob4);

        // Test orderByDesc with collection
        OrderBy ob5 = Filters.orderByDesc(props);
        Assertions.assertNotNull(ob5);

        // Test with collection
        OrderBy ob6 = Filters.orderBy(props);
        Assertions.assertNotNull(ob6);

        // Test with collection and direction
        OrderBy ob7 = Filters.orderBy(props, SortDirection.DESC);
        Assertions.assertNotNull(ob7);

        // Test with single property and direction
        OrderBy ob8 = Filters.orderBy("salary", SortDirection.DESC);
        Assertions.assertNotNull(ob8);

        // Test with map
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("date", SortDirection.ASC);
        OrderBy ob9 = Filters.orderBy(orders);
        Assertions.assertNotNull(ob9);

        // Test with condition
        Condition condition = Filters.expr("name ASC");
        OrderBy ob10 = Filters.orderBy(condition);
        Assertions.assertNotNull(ob10);
    }

    @Test
    public void testOrderByMultiplePropertiesWithDirections() {
        // Test with 2 properties
        OrderBy ob1 = Filters.orderBy("prop1", SortDirection.ASC, "prop2", SortDirection.DESC);
        Assertions.assertNotNull(ob1);

        // Test with 3 properties
        OrderBy ob2 = Filters.orderBy("prop1", SortDirection.ASC, "prop2", SortDirection.DESC, "prop3", SortDirection.ASC);
        Assertions.assertNotNull(ob2);
    }

    @Test
    public void testOn() {
        // Test with condition
        Condition condition = Filters.eq("a.id", "b.a_id");
        On on1 = Filters.on(condition);
        Assertions.assertNotNull(on1);
        Assertions.assertEquals(condition, on1.condition());

        // Test with string
        On on2 = Filters.on("a.id = b.a_id");
        Assertions.assertNotNull(on2);
        Assertions.assertNotNull(on2.condition());

        // Test with two property names
        On on3 = Filters.on("userId", "user.id");
        Assertions.assertNotNull(on3);

        // Test with map
        Map<String, String> propMap = new HashMap<>();
        propMap.put("orderId", "order.id");
        propMap.put("productId", "product.id");
        On on4 = Filters.on(propMap);
        Assertions.assertNotNull(on4);
    }

    @Test
    public void testUsing() {
        // Test with varargs
        Using using1 = Filters.using("id", "name");
        Assertions.assertNotNull(using1);

        // Test with collection
        List<String> columns = Arrays.asList("userId", "departmentId");
        Using using2 = Filters.using(columns);
        Assertions.assertNotNull(using2);
    }

    @Test
    public void testJoin() {
        // Test with entity name only
        Join join1 = Filters.join("users");
        Assertions.assertNotNull(join1);

        // Test with entity name and condition
        Condition condition = Filters.eq("users.id", "orders.user_id");
        Join join2 = Filters.join("users", condition);
        Assertions.assertNotNull(join2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("users", "roles");
        Join join3 = Filters.join(entities, condition);
        Assertions.assertNotNull(join3);
    }

    @Test
    public void testLeftJoin() {
        // Test with entity name only
        LeftJoin leftJoin1 = Filters.leftJoin("orders");
        Assertions.assertNotNull(leftJoin1);

        // Test with entity name and condition
        Condition condition = Filters.eq("users.id", "orders.user_id");
        LeftJoin leftJoin2 = Filters.leftJoin("orders", condition);
        Assertions.assertNotNull(leftJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("orders", "order_items");
        LeftJoin leftJoin3 = Filters.leftJoin(entities, condition);
        Assertions.assertNotNull(leftJoin3);
    }

    @Test
    public void testRightJoin() {
        // Test with entity name only
        RightJoin rightJoin1 = Filters.rightJoin("departments");
        Assertions.assertNotNull(rightJoin1);

        // Test with entity name and condition
        Condition condition = Filters.eq("employees.dept_id", "departments.id");
        RightJoin rightJoin2 = Filters.rightJoin("departments", condition);
        Assertions.assertNotNull(rightJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("departments", "locations");
        RightJoin rightJoin3 = Filters.rightJoin(entities, condition);
        Assertions.assertNotNull(rightJoin3);
    }

    @Test
    public void testCrossJoin() {
        // Test with entity name only
        CrossJoin crossJoin1 = Filters.crossJoin("products");
        Assertions.assertNotNull(crossJoin1);

        // Test with a collection of entities
        List<String> entities = Arrays.asList("products", "categories");
        CrossJoin crossJoin3 = Filters.crossJoin(entities);
        Assertions.assertNotNull(crossJoin3);
    }

    @Test
    public void testFullJoin() {
        // Test with entity name only
        FullJoin fullJoin1 = Filters.fullJoin("employees");
        Assertions.assertNotNull(fullJoin1);

        // Test with entity name and condition
        Condition condition = Filters.eq("employees.id", "managers.employee_id");
        FullJoin fullJoin2 = Filters.fullJoin("managers", condition);
        Assertions.assertNotNull(fullJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("employees", "departments");
        FullJoin fullJoin3 = Filters.fullJoin(entities, condition);
        Assertions.assertNotNull(fullJoin3);
    }

    @Test
    public void testInnerJoin() {
        // Test with entity name only
        InnerJoin innerJoin1 = Filters.innerJoin("roles");
        Assertions.assertNotNull(innerJoin1);

        // Test with entity name and condition
        Condition condition = Filters.eq("users.role_id", "roles.id");
        InnerJoin innerJoin2 = Filters.innerJoin("roles", condition);
        Assertions.assertNotNull(innerJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("users", "user_roles");
        InnerJoin innerJoin3 = Filters.innerJoin(entities, condition);
        Assertions.assertNotNull(innerJoin3);
    }

    @Test
    public void testNaturalJoin() {
        // Test with entity name only
        NaturalJoin naturalJoin1 = Filters.naturalJoin("departments");
        Assertions.assertNotNull(naturalJoin1);

        // Test with a collection of entities
        List<String> entities = Arrays.asList("employees", "departments");
        NaturalJoin naturalJoin3 = Filters.naturalJoin(entities);
        Assertions.assertNotNull(naturalJoin3);
    }

    @Test
    public void testIn() {
        // Test with int array
        In in1 = Filters.in("age", new int[] { 18, 21, 25 });
        Assertions.assertNotNull(in1);
        Assertions.assertEquals("age", in1.propName());

        // Test with long array
        In in2 = Filters.in("id", new long[] { 1L, 2L, 3L });
        Assertions.assertNotNull(in2);

        // Test with double array
        In in3 = Filters.in("price", new double[] { 9.99, 19.99, 29.99 });
        Assertions.assertNotNull(in3);

        // Test with object array
        In in4 = Filters.in("status", new Object[] { "active", "pending", "approved" });
        Assertions.assertNotNull(in4);

        // Test with collection
        List<String> values = Arrays.asList("red", "green", "blue");
        In in5 = Filters.in("color", values);
        Assertions.assertNotNull(in5);
    }

    @Test
    public void testNotIn() {
        // Test with int array
        NotIn notIn1 = Filters.notIn("status", new int[] { 0, -1 });
        Assertions.assertNotNull(notIn1);

        // Test with long array
        NotIn notIn2 = Filters.notIn("excludeId", new long[] { 110L, 120L });
        Assertions.assertNotNull(notIn2);

        // Test with double array
        NotIn notIn3 = Filters.notIn("discount", new double[] { 0.0, 100.0 });
        Assertions.assertNotNull(notIn3);

        // Test with object array
        NotIn notIn4 = Filters.notIn("type", new Object[] { "guest", "bot" });
        Assertions.assertNotNull(notIn4);

        // Test with collection
        List<String> values = Arrays.asList("spam", "deleted", "hidden");
        NotIn notIn5 = Filters.notIn("status", values);
        Assertions.assertNotNull(notIn5);
    }

    //    @Test
    //    public void testCell() {
    //        Condition condition = Filters.eq("status", "active");
    //        Cell cell = Filters.cell(Operator.AND, condition);
    //        Assertions.assertNotNull(cell);
    //        Assertions.assertEquals(Operator.AND, cell.operator());
    //        Assertions.assertEquals(condition, cell.condition());
    //    }

    @Test
    public void testSubQuery() {
        // Test with class, properties and condition
        List<String> props = Arrays.asList("id", "name");
        Condition condition = Filters.eq("active", true);
        SubQuery sq1 = Filters.subQuery(String.class, props, condition);
        Assertions.assertNotNull(sq1);

        // Test with entity name, properties and condition
        SubQuery sq2 = Filters.subQuery("users", props, condition);
        Assertions.assertNotNull(sq2);

        // Test with entity name, properties and string condition
        SubQuery sq3 = Filters.subQuery("users", props, "active = 1");
        Assertions.assertNotNull(sq3);

        // Test with entity name and sql (deprecated)
        SubQuery sq4 = Filters.subQuery("users", "SELECT * FROM users");
        Assertions.assertNotNull(sq4);

        // Test with sql only
        SubQuery sq5 = Filters.subQuery("SELECT COUNT(*) FROM orders");
        Assertions.assertNotNull(sq5);
    }

    @Test
    public void testLimit() {
        // Test with count only
        Limit limit1 = Filters.limit(10);
        Assertions.assertNotNull(limit1);
        Assertions.assertEquals(10, limit1.count());

        // Test with offset and count
        Limit limit2 = Filters.limit(50, 20);
        Assertions.assertNotNull(limit2);
        Assertions.assertEquals(20, limit2.offset());
        Assertions.assertEquals(50, limit2.count());

        // Test with expression
        Limit limit3 = Filters.limit("10 OFFSET 5");
        Assertions.assertNotNull(limit3);
    }

    @Test
    public void testCB() {
        // Test where with condition
        Condition condition = Filters.eq("status", "active");
        Criteria criteria1 = Criteria.builder().where(condition).build();
        Assertions.assertNotNull(criteria1);

        // Test where with string
        Criteria criteria2 = Criteria.builder().where("age > 18").build();
        Assertions.assertNotNull(criteria2);

        // Test groupBy with condition
        Criteria criteria3 = Criteria.builder().groupBy(condition).build();
        Assertions.assertNotNull(criteria3);

        // Test groupBy with varargs
        Criteria criteria4 = Criteria.builder().groupBy("dept", "team").build();
        Assertions.assertNotNull(criteria4);

        // Test groupBy with property and direction
        Criteria criteria5 = Criteria.builder().groupBy("salary", SortDirection.DESC).build();
        Assertions.assertNotNull(criteria5);

        // Test groupBy with collection
        List<String> props = Arrays.asList("year", "month");
        Criteria criteria6 = Criteria.builder().groupBy(props).build();
        Assertions.assertNotNull(criteria6);

        // Test groupBy with collection and direction
        Criteria criteria7 = Criteria.builder().groupBy(props, SortDirection.DESC).build();
        Assertions.assertNotNull(criteria7);

        // Test groupBy with map
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("date", SortDirection.ASC);
        Criteria criteria8 = Criteria.builder().groupBy(orders).build();
        Assertions.assertNotNull(criteria8);

        // Test having with condition
        Criteria criteria9 = Criteria.builder().having(condition).build();
        Assertions.assertNotNull(criteria9);

        // Test having with string
        Criteria criteria10 = Criteria.builder().having("COUNT(*) > 5").build();
        Assertions.assertNotNull(criteria10);

        // Test orderByAsc with varargs
        Criteria criteria11 = Criteria.builder().orderByAsc("firstName", "lastName").build();
        Assertions.assertNotNull(criteria11);

        // Test orderByAsc with collection
        Criteria criteria12 = Criteria.builder().orderByAsc(props).build();
        Assertions.assertNotNull(criteria12);

        // Test orderByDesc with varargs
        Criteria criteria13 = Criteria.builder().orderByDesc("createdAt", "id").build();
        Assertions.assertNotNull(criteria13);

        // Test orderByDesc with collection
        Criteria criteria14 = Criteria.builder().orderByDesc(props).build();
        Assertions.assertNotNull(criteria14);

        // Test orderBy with condition
        Criteria criteria15 = Criteria.builder().orderBy(condition).build();
        Assertions.assertNotNull(criteria15);

        // Test orderBy with varargs
        Criteria criteria16 = Criteria.builder().orderBy("name", "age").build();
        Assertions.assertNotNull(criteria16);

        // Test orderBy with property and direction
        Criteria criteria17 = Criteria.builder().orderBy("salary", SortDirection.DESC).build();
        Assertions.assertNotNull(criteria17);

        // Test orderBy with collection
        Criteria criteria18 = Criteria.builder().orderBy(props).build();
        Assertions.assertNotNull(criteria18);

        // Test orderBy with collection and direction
        Criteria criteria19 = Criteria.builder().orderBy(props, SortDirection.DESC).build();
        Assertions.assertNotNull(criteria19);

        // Test orderBy with map
        Criteria criteria20 = Criteria.builder().orderBy(orders).build();
        Assertions.assertNotNull(criteria20);

        // Test limit with Limit condition
        Limit limitCond = Filters.limit(10);
        Criteria criteria21 = Criteria.builder().limit(limitCond).build();
        Assertions.assertNotNull(criteria21);

        // Test limit with count
        Criteria criteria22 = Criteria.builder().limit(20).build();
        Assertions.assertNotNull(criteria22);

        // Test limit with offset and count
        Criteria criteria23 = Criteria.builder().limit(30, 10).build();
        Assertions.assertNotNull(criteria23);

        // Test limit with expression
        Criteria criteria24 = Criteria.builder().limit("50 OFFSET 100").build();
        Assertions.assertNotNull(criteria24);
    }

    @Test
    public void testFilters_alwaysTrue() {
        Condition condition = Filters.alwaysTrue();
        assertNotNull(condition);
    }

    @Test
    public void testFilters_not() {
        Like likeCondition = Filters.like("name", "%test%");
        Not notLike = Filters.not(likeCondition);
        assertNotNull(notLike);

        Not notIn = Filters.not(Filters.in("status", Arrays.asList("inactive", "deleted")));
        assertNotNull(notIn);

        Not notBetween = Filters.not(Filters.between("age", 18, 65));
        assertNotNull(notBetween);

        Not complexNot = Filters.not(Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.like("email", "%@company.com")));
        assertNotNull(complexNot);
    }

    @Test
    public void testFilters_namedProperty() {
        NamedProperty prop = Filters.namedProperty("user_name");
        assertNotNull(prop);
        assertEquals("user_name", prop.propName());
    }

    @Test
    public void testFilters_expr() {
        SqlExpression expr = Filters.expr("UPPER(name) = 'JOHN'");
        assertNotNull(expr);
        assertEquals("UPPER(name) = 'JOHN'", expr.literal());
    }

    @Test
    public void testFilters_binary() {
        Binary condition = Filters.binary("price", Operator.GREATER_THAN, 100);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_equal() {
        Equal condition = Filters.equal("username", "john_doe");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_equalParameterized() {
        Equal condition = Filters.equal("user_id");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eq() {
        Equal condition = Filters.eq("status", "active");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_eqParameterized() {
        Equal condition = Filters.eq("email");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_anyEqual_map() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", "John");
        props.put("email", "john@example.com");
        Or condition = Filters.anyEqual(props);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_anyEqual_twoProps() {
        Or condition = Filters.anyEqual("name", "John", "email", "john@example.com");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_anyEqual_threeProps() {
        Or condition = Filters.anyEqual("status", "active", "type", "premium", "verified", true);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_allEqual_map() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("status", "active");
        props.put("type", "premium");
        And condition = Filters.allEqual(props);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_allEqual_twoProps() {
        And condition = Filters.allEqual("status", "active", "type", "premium");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_allEqual_threeProps() {
        And condition = Filters.allEqual("status", "active", "type", "premium", "verified", true);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_gtAndLt() {
        And condition = Filters.gtAndLt("age", 18, 65);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_gtAndLt_parameterized() {
        And condition = Filters.gtAndLt("price");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_geAndLt() {
        And condition = Filters.geAndLt("price", 100, 500);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_geAndLe() {
        And condition = Filters.geAndLe("date", "2023-01-01", "2023-12-31");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_gtAndLe() {
        And condition = Filters.gtAndLe("score", 0, 100);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notEqual() {
        NotEqual condition = Filters.notEqual("status", "deleted");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_ne() {
        NotEqual condition = Filters.ne("status", "inactive");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_greaterThan() {
        GreaterThan condition = Filters.greaterThan("age", 18);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_gt() {
        GreaterThan condition = Filters.gt("price", 100);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_greaterThanOrEqual() {
        GreaterThanOrEqual condition = Filters.greaterThanOrEqual("score", 60);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_ge() {
        GreaterThanOrEqual condition = Filters.ge("age", 18);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_lessThan() {
        LessThan condition = Filters.lt("price", 1000);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_lessThanOrEqual() {
        LessThanOrEqual condition = Filters.le("age", 65);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_between() {
        Between condition = Filters.between("price", 100.0, 500.0);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notBetween() {
        NotBetween condition = Filters.notBetween("age", 0, 17);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_like() {
        Like condition = Filters.like("email", "%@company.com");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notLike() {
        NotLike condition = Filters.notLike("name", "%test%");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_isNull() {
        IsNull condition = Filters.isNull("optional_field");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_isNotNull() {
        IsNotNull condition = Filters.isNotNull("required_field");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_in() {
        In condition = Filters.in("status", Arrays.asList("PENDING", "APPROVED", "ACTIVE"));
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notIn() {
        NotIn condition = Filters.notIn("status", Arrays.asList("deleted", "banned"));
        assertNotNull(condition);
    }

    @Test
    public void testFilters_and() {
        And condition = Filters.and(Filters.eq("department", "Engineering"), Filters.or(Filters.gt("salary", 75000), Filters.eq("level", "Senior")),
                Filters.isNotNull("manager_id"));
        assertNotNull(condition);
    }

    @Test
    public void testFilters_or() {
        Or condition = Filters.or(Filters.eq("status", "active"), Filters.eq("status", "pending"));
        assertNotNull(condition);
    }

    @Test
    public void testFilters_contains() {
        Like condition = Filters.contains("name", "John");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_startsWith() {
        Like condition = Filters.startsWith("name", "John");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_endsWith() {
        Like condition = Filters.endsWith("email", "@company.com");
        assertNotNull(condition);
    }

    @Test
    public void testFilters_exists() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = customers.id AND status = 'COMPLETED'");
        Exists hasOrders = Filters.exists(subQuery);
        assertNotNull(hasOrders);
    }

    @Test
    public void testFilters_inSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM active_users");
        InSubQuery condition = Filters.in("id", subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_notInSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM blocked_users");
        NotInSubQuery condition = Filters.notIn("id", subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testFilters_subQuery() {
        SubQuery sq = Filters.subQuery("SELECT COUNT(*) FROM orders WHERE user_id = users.id");
        assertNotNull(sq);
    }

    @Test
    public void testFilters_groupBy() {
        GroupBy groupBy = Filters.groupBy("department");
        assertNotNull(groupBy);
    }

    @Test
    public void testFilters_orderBy() {
        OrderBy orderBy = Filters.orderBy("name");
        assertNotNull(orderBy);
    }

    @Test
    public void testFilters_limit() {
        Limit limit = Filters.limit(10);
        assertNotNull(limit);
    }

    @Test
    public void testFilters_limitWithOffset() {
        Limit limit = Filters.limit(10, 20);
        assertNotNull(limit);
    }

    @Test
    public void testFilters_join() {
        Join join = Filters.join("orders ON users.id = orders.user_id");
        assertNotNull(join);
    }

    @Test
    public void testFilters_leftJoin() {
        LeftJoin leftJoin = Filters.leftJoin("orders ON users.id = orders.user_id");
        assertNotNull(leftJoin);
    }

    @Test
    public void testFilters_rightJoin() {
        RightJoin rightJoin = Filters.rightJoin("orders ON users.id = orders.user_id");
        assertNotNull(rightJoin);
    }

    @Test
    public void testFilters_innerJoin() {
        InnerJoin innerJoin = Filters.innerJoin("orders ON users.id = orders.user_id");
        assertNotNull(innerJoin);
    }

    @Test
    public void testFilters_fullJoin() {
        FullJoin fullJoin = Filters.fullJoin("orders ON users.id = orders.user_id");
        assertNotNull(fullJoin);
    }

    @Test
    public void testFilters_crossJoin() {
        CrossJoin crossJoin = Filters.crossJoin("products");
        assertNotNull(crossJoin);
    }

    @Test
    public void testFilters_naturalJoin() {
        NaturalJoin naturalJoin = Filters.naturalJoin("department");
        assertNotNull(naturalJoin);
    }

    @Test
    public void testAllEqual_EntitySingleSelectedProperty() {
        com.landawn.abacus.query.entity.Account account = new com.landawn.abacus.query.entity.Account();
        account.setFirstName("John");

        And condition = Filters.allEqual(account, Arrays.asList("firstName"));

        assertEquals(1, condition.conditions().size());
    }

    @Test
    public void testAllEqual_EntityTwoSelectedProperties() {
        com.landawn.abacus.query.entity.Account account = new com.landawn.abacus.query.entity.Account();
        account.setFirstName("John");
        account.setLastName("Doe");

        And condition = Filters.allEqual(account, Arrays.asList("firstName", "lastName"));

        assertEquals(2, condition.conditions().size());
    }

    @Test
    public void testAlwaysTrueAndAlwaysFalseReturnDistinctExpressions() {
        // Both accessors must work and return non-null, distinct objects
        var t = Filters.alwaysTrue();
        var f = Filters.alwaysFalse();
        assertNotNull(t);
        assertNotNull(f);
        assertNotSame(t, f, "alwaysTrue and alwaysFalse must be distinct instances");
        // Semantics: alwaysTrue uses '<', alwaysFalse uses '>'
        assertTrue(t.toString().contains("<"), "alwaysTrue expression should contain '<'");
        assertTrue(f.toString().contains(">"), "alwaysFalse expression should contain '>'");
    }

    // --- Bug fixes: wildcard methods must reject null propValue (previously silently produced "%null%") ---

    @Test
    public void testContains_NullPropValue_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.contains("name", null));
    }

    @Test
    public void testNotContains_NullPropValue_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.notContains("name", null));
    }

    @Test
    public void testStartsWith_NullPropValue_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.startsWith("name", null));
    }

    @Test
    public void testNotStartsWith_NullPropValue_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.notStartsWith("name", null));
    }

    @Test
    public void testEndsWith_NullPropValue_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.endsWith("name", null));
    }

    @Test
    public void testNotEndsWith_NullPropValue_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.notEndsWith("name", null));
    }

    @Test
    public void testWildcardMethods_NonNullValues_StillWork() {
        // Regression guard: ensure happy paths still produce the expected wildcard patterns.
        assertEquals("%java%", Filters.contains("col", "java").propValue());
        assertEquals("%java%", Filters.notContains("col", "java").propValue());
        assertEquals("Jo%", Filters.startsWith("col", "Jo").propValue());
        assertEquals("TEST%", Filters.notStartsWith("col", "TEST").propValue());
        assertEquals("%.com", Filters.endsWith("col", ".com").propValue());
        assertEquals("%.tmp", Filters.notEndsWith("col", ".tmp").propValue());
    }

    // --- Bug fix: having(String) must reject null/empty (matching where(String) contract) ---

    @Test
    public void testHavingString_NullExpr_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.having((String) null));
    }

    @Test
    public void testHavingString_EmptyExpr_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.having(""));
    }

    @Test
    public void testHavingString_NonEmptyExpr_StillWorks() {
        Having h = Filters.having("SUM(amount) > 1000");
        assertNotNull(h);
        assertTrue(h.toString().contains("SUM(amount) > 1000"));
    }

    // --- Guard: on(String) must reject null/empty (matching where(String)/having(String) contract) ---

    @Test
    public void testOnString_NullExpr_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.on((String) null));
    }

    @Test
    public void testOnString_EmptyExpr_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> Filters.on(""));
    }

    @Test
    public void testOnString_NonEmptyExpr_StillWorks() {
        On on = Filters.on("users.id = orders.user_id");
        assertNotNull(on);
        assertTrue(on.toString().contains("users.id = orders.user_id"));
    }

    // --- 2nd-pass review verification tests ---
    // These confirm that operator-pair factory methods do NOT have copy/paste inversions,
    // that LIKE wildcards are placed on the correct side, and that null-value handling is correct.

    @Test
    public void test2ndPass_gtAndLt_usesGtAndLt() {
        And c = Filters.gtAndLt("age", 18, 65);
        assertEquals(2, c.conditions().size());
        assertEquals(Operator.GREATER_THAN, c.conditions().get(0).operator());
        assertEquals(Operator.LESS_THAN, c.conditions().get(1).operator());
    }

    @Test
    public void test2ndPass_geAndLt_usesGeAndLt() {
        And c = Filters.geAndLt("price", 100, 500);
        assertEquals(2, c.conditions().size());
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, c.conditions().get(0).operator());
        assertEquals(Operator.LESS_THAN, c.conditions().get(1).operator());
    }

    @Test
    public void test2ndPass_geAndLe_usesGeAndLe() {
        And c = Filters.geAndLe("score", 0, 100);
        assertEquals(2, c.conditions().size());
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, c.conditions().get(0).operator());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, c.conditions().get(1).operator());
    }

    @Test
    public void test2ndPass_gtAndLe_usesGtAndLe() {
        And c = Filters.gtAndLe("temp", -10, 40);
        assertEquals(2, c.conditions().size());
        assertEquals(Operator.GREATER_THAN, c.conditions().get(0).operator());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, c.conditions().get(1).operator());
    }

    @Test
    public void test2ndPass_parameterized_gtAndLt_geAndLt_geAndLe_gtAndLe_useCorrectOperators() {
        assertEquals(Operator.GREATER_THAN, Filters.gtAndLt("c").conditions().get(0).operator());
        assertEquals(Operator.LESS_THAN, Filters.gtAndLt("c").conditions().get(1).operator());

        assertEquals(Operator.GREATER_THAN_OR_EQUAL, Filters.geAndLt("c").conditions().get(0).operator());
        assertEquals(Operator.LESS_THAN, Filters.geAndLt("c").conditions().get(1).operator());

        assertEquals(Operator.GREATER_THAN_OR_EQUAL, Filters.geAndLe("c").conditions().get(0).operator());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, Filters.geAndLe("c").conditions().get(1).operator());

        assertEquals(Operator.GREATER_THAN, Filters.gtAndLe("c").conditions().get(0).operator());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, Filters.gtAndLe("c").conditions().get(1).operator());
    }

    @Test
    public void test2ndPass_likeWildcards_placement() {
        // contains: %v% on both sides
        assertEquals("%foo%", Filters.contains("c", "foo").propValue());
        // notContains: %v% on both sides
        assertEquals("%foo%", Filters.notContains("c", "foo").propValue());
        // startsWith: v%
        assertEquals("foo%", Filters.startsWith("c", "foo").propValue());
        // notStartsWith: v%
        assertEquals("foo%", Filters.notStartsWith("c", "foo").propValue());
        // endsWith: %v
        assertEquals("%foo", Filters.endsWith("c", "foo").propValue());
        // notEndsWith: %v
        assertEquals("%foo", Filters.notEndsWith("c", "foo").propValue());
    }

    @Test
    public void test2ndPass_equalWithNull_rendersIsNull() {
        // CRITICAL: equal(prop, null) MUST render as "prop IS NULL", not "prop = NULL"
        // (otherwise the condition is never true in SQL three-valued logic)
        Equal eq = Filters.equal("col", null);
        assertTrue(eq.toString().contains("IS NULL"), "equal(c, null) should render as IS NULL but was: " + eq.toString());
        assertFalse(eq.toString().contains("= NULL"));
    }

    @Test
    public void test2ndPass_notEqualWithNull_rendersIsNotNull() {
        // CRITICAL: notEqual(prop, null) MUST render as "prop IS NOT NULL"
        NotEqual ne = Filters.notEqual("col", null);
        assertTrue(ne.toString().contains("IS NOT NULL"), "notEqual(c, null) should render as IS NOT NULL but was: " + ne.toString());
    }

    @Test
    public void test2ndPass_inPrimitiveArrays_correctBoxing() {
        // int[]
        In intIn = Filters.in("col", new int[] { 1, 2, 3 });
        assertEquals(3, intIn.values().size());
        assertEquals(Integer.valueOf(1), intIn.values().iterator().next());

        // long[]
        In longIn = Filters.in("col", new long[] { 10L, 20L });
        assertEquals(2, longIn.values().size());
        assertEquals(Long.valueOf(10L), longIn.values().iterator().next());

        // double[]
        In dblIn = Filters.in("col", new double[] { 1.5, 2.5 });
        assertEquals(2, dblIn.values().size());
        assertEquals(Double.valueOf(1.5), dblIn.values().iterator().next());
    }

    @Test
    public void test2ndPass_notInPrimitiveArrays_correctBoxing() {
        NotIn intNi = Filters.notIn("col", new int[] { 1, 2 });
        assertEquals(2, intNi.values().size());

        NotIn longNi = Filters.notIn("col", new long[] { 1L, 2L });
        assertEquals(2, longNi.values().size());

        NotIn dblNi = Filters.notIn("col", new double[] { 1.0, 2.0 });
        assertEquals(2, dblNi.values().size());
    }

    @Test
    public void test2ndPass_orEmptyArray_buildsEmptyOrJunction() {
        // Verify or() with empty array doesn't NPE; the resulting toString is empty.
        Or empty = Filters.or(new Condition[0]);
        assertNotNull(empty);
        assertEquals("", empty.toString());
    }

    @Test
    public void test2ndPass_andEmptyArray_buildsEmptyAndJunction() {
        And empty = Filters.and(new Condition[0]);
        assertNotNull(empty);
        assertEquals("", empty.toString());
    }

    @Test
    public void test2ndPass_notWithNull_throwsIAE() {
        // Not(null) should throw because Junction rejects null conditions.
        assertThrows(IllegalArgumentException.class, () -> Filters.not(null));
    }

    @Test
    public void test2ndPass_betweenWithMinGreaterThanMax_rendersAsIs() {
        // No swapping; documented behavior: rendered as-is, even when min > max.
        Between b = Filters.between("col", 100, 1);
        String s = b.toString();
        assertTrue(s.contains("100"));
        assertTrue(s.contains("1"));
        // min is rendered before max regardless of value order
        assertTrue(s.indexOf("100") < s.indexOf(" AND "));
    }

    @Test
    public void test2ndPass_betweenParameterized_renderedWithTwoPlaceholders() {
        Between b = Filters.between("col");
        // Should produce "col BETWEEN ? AND ?" (both placeholders are QME Expressions)
        String s = b.toString();
        assertTrue(s.contains("BETWEEN ? AND ?"), "Expected 'BETWEEN ? AND ?' but got: " + s);
    }

    @Test
    public void testBinaryRejectsMembershipOperators() {
        assertThrows(IllegalArgumentException.class, () -> Filters.binary("status", Operator.IN));
        assertThrows(IllegalArgumentException.class, () -> Filters.binary("status", Operator.NOT_IN));
    }

    @Test
    public void testLikeObjectOperandsAndVarargsIn() {
        SubQuery subQuery = Filters.subQuery("SELECT pattern FROM patterns");
        assertEquals(subQuery, Filters.like("name", (Object) subQuery).propValue());
        SqlExpression expression = Filters.expr("LOWER(pattern)");
        assertEquals(expression, Filters.notLike("name", (Object) expression).propValue());
        assertEquals(Arrays.asList("NEW", "OPEN"), Filters.in("status", "NEW", "OPEN").parameters());
        assertEquals(Arrays.asList("CLOSED", "DELETED"), Filters.notIn("status", "CLOSED", "DELETED").parameters());
    }

    @Test
    public void testEqualityFactoriesConsumeLiveMapInOnePass() {
        final Or any = Filters.anyEqual(mapWithUnstableSize());
        final And all = Filters.allEqual(mapWithUnstableSize());

        assertEquals(3, any.conditions().size());
        assertEquals(3, all.conditions().size());
        assertEquals(Arrays.asList(1, 2, 3), any.parameters());
        assertEquals(Arrays.asList(1, 2, 3), all.parameters());
    }

    @Test
    public void testEntityEqualityFactoriesConsumePropertyNamesInOnePass() {
        final Account account = new Account().setId(7L).setFirstName("Ada").setLastName("Lovelace");
        final Or any = Filters.anyEqual(account, collectionWithUnstableSize("id", "firstName", "lastName"));
        final And all = Filters.allEqual(account, collectionWithUnstableSize("id", "firstName", "lastName"));

        assertEquals(3, any.conditions().size());
        assertEquals(3, all.conditions().size());
        assertEquals(Arrays.asList(7L, "Ada", "Lovelace"), any.parameters());
        assertEquals(Arrays.asList(7L, "Ada", "Lovelace"), all.parameters());
    }

    @Test
    public void testIdCollectionFactoryConsumesInputInOnePass() {
        final Collection<EntityId> entityIds = collectionWithUnstableSize(EntityId.of("id", 1), EntityId.of("id", 2));

        final Or condition = Filters.idToCond(entityIds);

        assertEquals(2, condition.conditions().size());
        assertEquals(Arrays.asList(1, 2), condition.parameters());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testEqualityFactoriesRejectRawMapsWithNonStringKeysConsistently() {
        final Map rawMap = new LinkedHashMap();
        rawMap.put(1, "value");

        final IllegalArgumentException anyFailure = assertThrows(IllegalArgumentException.class, () -> Filters.anyEqual(rawMap));
        final IllegalArgumentException allFailure = assertThrows(IllegalArgumentException.class, () -> Filters.allEqual(rawMap));

        assertTrue(anyFailure.getMessage().contains("String"));
        assertTrue(allFailure.getMessage().contains("String"));
    }

    @Test
    public void testAnyOfAllEqualRejectsMixedEntitiesAndMapsInEitherOrder() {
        final Account account = new Account().setId(3L);
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", 4L);

        assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(Arrays.asList(props, account)));
        assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(Arrays.asList(account, props)));
        assertThrows(IllegalArgumentException.class, () -> Filters.anyOfAllEqual(Arrays.asList(account, props), Arrays.asList("id")));
    }

    @Test
    public void testBeanEqualityOverloadsDirectMapCallersToMapOverloads() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", 4L);

        assertThrows(IllegalArgumentException.class, () -> Filters.anyEqual((Object) props));
        assertThrows(IllegalArgumentException.class, () -> Filters.anyEqual((Object) props, Arrays.asList("id")));
        assertThrows(IllegalArgumentException.class, () -> Filters.allEqual((Object) props));
        assertThrows(IllegalArgumentException.class, () -> Filters.allEqual((Object) props, Arrays.asList("id")));
    }

    private static Map<String, Object> mapWithUnstableSize() {
        final Map<String, Object> result = new LinkedHashMap<>() {
            private int sizeCalls;

            @Override
            public int size() {
                return sizeCalls++ == 0 ? super.size() : super.size() + 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };

        result.put("a", 1);
        result.put("b", 2);
        result.put("c", 3);
        return result;
    }

    @SafeVarargs
    private static <T> Collection<T> collectionWithUnstableSize(final T... values) {
        final List<T> snapshot = Arrays.asList(values);

        return new AbstractCollection<>() {
            private int sizeCalls;

            @Override
            public Iterator<T> iterator() {
                return snapshot.iterator();
            }

            @Override
            public int size() {
                return sizeCalls++ == 0 ? snapshot.size() : snapshot.size() + 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
    }

    @Test
    public void testFiltersIsFinal() {
        assertTrue(java.lang.reflect.Modifier.isFinal(Filters.class.getModifiers()));
    }
}
