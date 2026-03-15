package com.landawn.abacus.query;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.All;
import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.Any;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Binary;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.CrossJoin;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.Except;
import com.landawn.abacus.query.condition.Exists;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.FullJoin;
import com.landawn.abacus.query.condition.GreaterThan;
import com.landawn.abacus.query.condition.GreaterThanOrEqual;
import com.landawn.abacus.query.condition.GroupBy;
import com.landawn.abacus.query.condition.Having;
import com.landawn.abacus.query.condition.In;
import com.landawn.abacus.query.condition.InSubQuery;
import com.landawn.abacus.query.condition.InnerJoin;
import com.landawn.abacus.query.condition.Intersect;
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
import com.landawn.abacus.query.condition.Minus;
import com.landawn.abacus.query.condition.NamedProperty;
import com.landawn.abacus.query.condition.NaturalJoin;
import com.landawn.abacus.query.condition.Not;
import com.landawn.abacus.query.condition.NotBetween;
import com.landawn.abacus.query.condition.NotEqual;
import com.landawn.abacus.query.condition.NotExists;
import com.landawn.abacus.query.condition.NotIn;
import com.landawn.abacus.query.condition.NotInSubQuery;
import com.landawn.abacus.query.condition.NotLike;
import com.landawn.abacus.query.condition.On;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Or;
import com.landawn.abacus.query.condition.OrderBy;
import com.landawn.abacus.query.condition.RightJoin;
import com.landawn.abacus.query.condition.Some;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Union;
import com.landawn.abacus.query.condition.UnionAll;
import com.landawn.abacus.query.condition.Using;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.EntityId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("2025")
class Filters2025Test extends TestBase {

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
        Expression expr = Filters.expr("age > 18");
        assertNotNull(expr);
    }

    @Test
    public void testBinary() {
        Binary binary = Filters.binary("age", Operator.GREATER_THAN, 18);
        assertNotNull(binary);
        assertEquals("age", binary.getPropName());
        assertEquals(Operator.GREATER_THAN, binary.operator());
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
    public void testInCollectionForAge() {
        List<Integer> ages = Arrays.asList(20, 30, 40);
        In in = Filters.in("age", ages);
        assertNotNull(in);
        assertFalse(in.getValues().isEmpty());
    }

    @Test
    public void testAnyEqualMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 30);

        Or or = Filters.anyEqual(props);
        assertNotNull(or);
        assertEquals(2, or.getConditions().size());
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

        assertEquals(2, or.getConditions().size());
        assertEquals("id", ((Equal) or.getConditions().get(0)).getPropName());
        assertEquals(Long.valueOf(7L), ((Equal) or.getConditions().get(0)).getPropValue());
        assertEquals("firstName", ((Equal) or.getConditions().get(1)).getPropName());
        assertEquals("Jane", ((Equal) or.getConditions().get(1)).getPropValue());
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
        assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testAnyEqualThreeProps() {
        Or or = Filters.anyEqual("name", "John", "age", 30, "status", "active");
        assertNotNull(or);
        assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testAllEqualMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("status", "active");
        props.put("verified", true);

        And and = Filters.allEqual(props);
        assertNotNull(and);
        assertEquals(2, and.getConditions().size());
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

        assertEquals(2, and.getConditions().size());
        assertEquals("id", ((Equal) and.getConditions().get(0)).getPropName());
        assertEquals(Long.valueOf(9L), ((Equal) and.getConditions().get(0)).getPropValue());
        assertEquals("lastName", ((Equal) and.getConditions().get(1)).getPropName());
        assertEquals("Doe", ((Equal) and.getConditions().get(1)).getPropValue());
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
        assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testAllEqualThreeProps() {
        And and = Filters.allEqual("name", "John", "age", 30, "status", "active");
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
    public void testGreaterThanOrEqual() {
        GreaterThanOrEqual ge = Filters.greaterThanOrEqual("age", 21);
        assertNotNull(ge);
        assertEquals("age", ge.getPropName());
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
        assertEquals("age", lt.getPropName());
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
        assertEquals("age", le.getPropName());
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
        assertEquals("age", between.getPropName());
        assertEquals(Integer.valueOf(18), between.getMinValue());
        assertEquals(Integer.valueOf(65), between.getMaxValue());
    }

    // Removed: testBt() - bt() methods have been removed. Use between() instead.

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
    public void testAnyOfAllEqualList() {
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
}

class Filters2026Test extends TestBase {

    @Test
    public void testId2Cond() {
        final EntityId entityId = EntityId.of("userId", 1, "orderId", 100);

        final And condition = Filters.id2Cond(entityId);

        assertEquals(Operator.AND, condition.operator());
        assertEquals(2, condition.getConditions().size());
        assertEquals(2, condition.getParameters().size());
        Assertions.assertTrue(condition.getParameters().containsAll(Arrays.asList(1, 100)));
    }

    @Test
    public void testId2Cond_SingleProperty() {
        final EntityId entityId = EntityId.of("userId", 1);

        final And condition = Filters.id2Cond(entityId);

        assertEquals(1, condition.getConditions().size());
        assertEquals(Arrays.asList(1), condition.getParameters());
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
        assertEquals(2, condition.getConditions().size());
        assertEquals(4, condition.getParameters().size());
        Assertions.assertTrue(condition.getParameters().containsAll(Arrays.asList(1, 100, 2, 200)));
    }

    @Test
    public void testId2Cond_CollectionEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Filters.id2Cond(Arrays.<EntityId> asList()));
    }

    @Test
    public void testIsNotNullAndNotEmpty() {
        final And condition = Filters.isNotNullAndNotEmpty("email");

        assertEquals(Operator.AND, condition.operator());
        assertEquals(2, condition.getConditions().size());
        Assertions.assertTrue(condition.toString().contains("IS NOT NULL"));
        Assertions.assertTrue(condition.toString().contains("!= ''"));
    }

    @Test
    public void testIsNotNullAndNotZero() {
        final And condition = Filters.isNotNullAndNotZero("quantity");

        assertEquals(Operator.AND, condition.operator());
        assertEquals(2, condition.getConditions().size());
        Assertions.assertTrue(condition.toString().contains("IS NOT NULL"));
        Assertions.assertTrue(condition.toString().contains("!= 0"));
    }
}

public class FiltersTest extends TestBase {

    @Test
    public void testAlwaysTrue() {
        Expression expr = Filters.alwaysTrue();
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("1 < 2", expr.getLiteral());
    }

    @Test
    public void testAlwaysFalse() {
        Expression expr = Filters.alwaysFalse();
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("1 > 2", expr.getLiteral());
    }

    @Test
    public void testNamedProperty() {
        NamedProperty prop = Filters.namedProperty("testProp");
        Assertions.assertNotNull(prop);
        Assertions.assertEquals("testProp", prop.propName());
    }

    @Test
    public void testExpr() {
        Expression expr = Filters.expr("age > 18");
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("age > 18", expr.getLiteral());
    }

    @Test
    public void testBinary() {
        Binary binary = Filters.binary("name", Operator.EQUAL, "John");
        Assertions.assertNotNull(binary);
        Assertions.assertEquals("name", binary.getPropName());
        Assertions.assertEquals(Operator.EQUAL, binary.operator());
        Assertions.assertEquals("John", binary.getPropValue());
    }

    @Test
    public void testEqual() {
        // Test with value
        Equal eq1 = Filters.equal("status", "active");
        Assertions.assertNotNull(eq1);
        Assertions.assertEquals("status", eq1.getPropName());
        Assertions.assertEquals("active", eq1.getPropValue());

        // Test without value (for parameterized SQL)
        Equal eq2 = Filters.equal("status");
        Assertions.assertNotNull(eq2);
        Assertions.assertEquals("status", eq2.getPropName());
        Assertions.assertEquals(Filters.QME, eq2.getPropValue());
    }

    @Test
    public void testEq() {
        // Test with value
        Equal eq1 = Filters.eq("age", 25);
        Assertions.assertNotNull(eq1);
        Assertions.assertEquals("age", eq1.getPropName());
        Assertions.assertEquals(25, (Integer) eq1.getPropValue());

        // Test without value
        Equal eq2 = Filters.eq("age");
        Assertions.assertNotNull(eq2);
        Assertions.assertEquals("age", eq2.getPropName());
        Assertions.assertEquals(Filters.QME, eq2.getPropValue());
    }

    @Test
    public void testAnyEqualWithArray() {
        Or or = Filters.anyEqual("status", "active", "pending", "approved");
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testInCollection() {
        List<String> values = Arrays.asList("red", "green", "blue");
        In in = Filters.in("color", values);
        Assertions.assertNotNull(in);
    }

    @Test
    public void testAnyEqualWithMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 25);
        props.put("status", "active");

        Or or = Filters.anyEqual(props);
        Assertions.assertNotNull(or);
        Assertions.assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testAnyEqualWithTwoProperties() {
        Or or = Filters.anyEqual("name", "John", "age", 25);
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testAnyEqualWithThreeProperties() {
        Or or = Filters.anyEqual("name", "John", "age", 25, "status", "active");
        Assertions.assertNotNull(or);
        Assertions.assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testAllEqualWithMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 25);

        And and = Filters.allEqual(props);
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testAllEqualWithTwoProperties() {
        And and = Filters.allEqual("name", "John", "age", 25);
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testAllEqualWithThreeProperties() {
        And and = Filters.allEqual("name", "John", "age", 25, "status", "active");
        Assertions.assertNotNull(and);
        Assertions.assertEquals(3, and.getConditions().size());
    }

    @Test
    public void testGtAndLt() {
        // With values
        And and1 = Filters.gtAndLt("age", 18, 65);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Without values
        And and2 = Filters.gtAndLt("age");
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testGeAndLt() {
        // With values
        And and1 = Filters.geAndLt("score", 0, 100);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Without values
        And and2 = Filters.geAndLt("score");
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testGeAndLe() {
        // With values
        And and1 = Filters.geAndLe("price", 10.0, 100.0);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Without values
        And and2 = Filters.geAndLe("price");
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testGtAndLe() {
        // With values
        And and1 = Filters.gtAndLe("temperature", -10, 40);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Without values
        And and2 = Filters.gtAndLe("temperature");
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testNotEqual() {
        // With value
        NotEqual ne1 = Filters.notEqual("status", "deleted");
        Assertions.assertNotNull(ne1);
        Assertions.assertEquals("status", ne1.getPropName());
        Assertions.assertEquals("deleted", ne1.getPropValue());

        // Without value
        NotEqual ne2 = Filters.notEqual("status");
        Assertions.assertNotNull(ne2);
        Assertions.assertEquals(Filters.QME, ne2.getPropValue());
    }

    @Test
    public void testNe() {
        // With value
        NotEqual ne1 = Filters.ne("type", "admin");
        Assertions.assertNotNull(ne1);
        Assertions.assertEquals("type", ne1.getPropName());
        Assertions.assertEquals("admin", ne1.getPropValue());

        // Without value
        NotEqual ne2 = Filters.ne("type");
        Assertions.assertNotNull(ne2);
        Assertions.assertEquals(Filters.QME, ne2.getPropValue());
    }

    @Test
    public void testGreaterThan() {
        // With value
        GreaterThan gt1 = Filters.greaterThan("age", 18);
        Assertions.assertNotNull(gt1);
        Assertions.assertEquals("age", gt1.getPropName());
        Assertions.assertEquals(18, (Integer) (Integer) gt1.getPropValue());

        // Without value
        GreaterThan gt2 = Filters.greaterThan("age");
        Assertions.assertNotNull(gt2);
        Assertions.assertEquals(Filters.QME, gt2.getPropValue());
    }

    @Test
    public void testGt() {
        // With value
        GreaterThan gt1 = Filters.gt("score", 60);
        Assertions.assertNotNull(gt1);
        Assertions.assertEquals("score", gt1.getPropName());
        Assertions.assertEquals(60, (Integer) gt1.getPropValue());

        // Without value
        GreaterThan gt2 = Filters.gt("score");
        Assertions.assertNotNull(gt2);
        Assertions.assertEquals(Filters.QME, gt2.getPropValue());
    }

    @Test
    public void testGreaterThanOrEqual() {
        // With value
        GreaterThanOrEqual ge1 = Filters.greaterThanOrEqual("level", 5);
        Assertions.assertNotNull(ge1);
        Assertions.assertEquals("level", ge1.getPropName());
        Assertions.assertEquals(5, (Integer) ge1.getPropValue());

        // Without value
        GreaterThanOrEqual ge2 = Filters.greaterThanOrEqual("level");
        Assertions.assertNotNull(ge2);
        Assertions.assertEquals(Filters.QME, ge2.getPropValue());
    }

    @Test
    public void testGe() {
        // With value
        GreaterThanOrEqual ge1 = Filters.ge("rating", 4.0);
        Assertions.assertNotNull(ge1);
        Assertions.assertEquals("rating", ge1.getPropName());
        Assertions.assertEquals(4.0, ge1.getPropValue());

        // Without value
        GreaterThanOrEqual ge2 = Filters.ge("rating");
        Assertions.assertNotNull(ge2);
        Assertions.assertEquals(Filters.QME, ge2.getPropValue());
    }

    @Test
    public void testLessThan() {
        // With value
        LessThan lt1 = Filters.lessThan("price", 100);
        Assertions.assertNotNull(lt1);
        Assertions.assertEquals("price", lt1.getPropName());
        Assertions.assertEquals(100, (Integer) lt1.getPropValue());

        // Without value
        LessThan lt2 = Filters.lessThan("price");
        Assertions.assertNotNull(lt2);
        Assertions.assertEquals(Filters.QME, lt2.getPropValue());
    }

    @Test
    public void testLt() {
        // With value
        LessThan lt1 = Filters.lt("quantity", 10);
        Assertions.assertNotNull(lt1);
        Assertions.assertEquals("quantity", lt1.getPropName());
        Assertions.assertEquals(10, (Integer) lt1.getPropValue());

        // Without value
        LessThan lt2 = Filters.lt("quantity");
        Assertions.assertNotNull(lt2);
        Assertions.assertEquals(Filters.QME, lt2.getPropValue());
    }

    @Test
    public void testLessThanOrEqual() {
        // With value
        LessThanOrEqual le1 = Filters.lessThanOrEqual("discount", 50);
        Assertions.assertNotNull(le1);
        Assertions.assertEquals("discount", le1.getPropName());
        Assertions.assertEquals(50, (Integer) le1.getPropValue());

        // Without value
        LessThanOrEqual le2 = Filters.lessThanOrEqual("discount");
        Assertions.assertNotNull(le2);
        Assertions.assertEquals(Filters.QME, le2.getPropValue());
    }

    @Test
    public void testLe() {
        // With value
        LessThanOrEqual le1 = Filters.le("temperature", 30);
        Assertions.assertNotNull(le1);
        Assertions.assertEquals("temperature", le1.getPropName());
        Assertions.assertEquals(30, (Integer) le1.getPropValue());

        // Without value
        LessThanOrEqual le2 = Filters.le("temperature");
        Assertions.assertNotNull(le2);
        Assertions.assertEquals(Filters.QME, le2.getPropValue());
    }

    @Test
    public void testBetween() {
        // With values
        Between bt1 = Filters.between("age", 18, 65);
        Assertions.assertNotNull(bt1);
        Assertions.assertEquals("age", bt1.getPropName());
        Assertions.assertEquals(18, (Integer) (Integer) bt1.getMinValue());
        Assertions.assertEquals(65, (Integer) bt1.getMaxValue());

        // Without values
        Between bt2 = Filters.between("age");
        Assertions.assertNotNull(bt2);
        Assertions.assertEquals(Filters.QME, bt2.getMinValue());
        Assertions.assertEquals(Filters.QME, bt2.getMaxValue());
    }

    // Removed: testBt() - bt() methods have been removed. Use between() instead.

    @Test
    public void testNotBetween() {
        // With values
        NotBetween nbt1 = Filters.notBetween("score", 0, 50);
        Assertions.assertNotNull(nbt1);
        Assertions.assertEquals("score", nbt1.getPropName());
        Assertions.assertEquals(0, (Integer) nbt1.getMinValue());
        Assertions.assertEquals(50, (Integer) nbt1.getMaxValue());

        // Without values
        NotBetween nbt2 = Filters.notBetween("score");
        Assertions.assertNotNull(nbt2);
        Assertions.assertEquals(Filters.QME, nbt2.getMinValue());
        Assertions.assertEquals(Filters.QME, nbt2.getMaxValue());
    }

    @Test
    public void testLike() {
        // With value
        Like like1 = Filters.like("name", "%John%");
        Assertions.assertNotNull(like1);
        Assertions.assertEquals("name", like1.getPropName());
        Assertions.assertEquals("%John%", like1.getPropValue());

        // Without value
        Like like2 = Filters.like("name");
        Assertions.assertNotNull(like2);
        Assertions.assertEquals(Filters.QME, like2.getPropValue());
    }

    @Test
    public void testNotLike() {
        // With value
        NotLike notLike1 = Filters.notLike("description", "%spam%");
        Assertions.assertNotNull(notLike1);
        Assertions.assertEquals("description", notLike1.getPropName());
        Assertions.assertEquals("%spam%", notLike1.getPropValue());

        // Without value
        NotLike notLike2 = Filters.notLike("description");
        Assertions.assertNotNull(notLike2);
        Assertions.assertEquals(Filters.QME, notLike2.getPropValue());
    }

    @Test
    public void testContains() {
        Like like = Filters.contains("name", "John");
        Assertions.assertNotNull(like);
        Assertions.assertEquals("name", like.getPropName());
        Assertions.assertEquals("%John%", like.getPropValue());
    }

    @Test
    public void testNotContains() {
        NotLike notLike = Filters.notContains("description", "spam");
        Assertions.assertNotNull(notLike);
        Assertions.assertEquals("description", notLike.getPropName());
        Assertions.assertEquals("%spam%", notLike.getPropValue());
    }

    @Test
    public void testStartsWith() {
        Like like = Filters.startsWith("email", "admin@");
        Assertions.assertNotNull(like);
        Assertions.assertEquals("email", like.getPropName());
        Assertions.assertEquals("admin@%", like.getPropValue());
    }

    @Test
    public void testNotStartsWith() {
        NotLike notLike = Filters.notStartsWith("username", "test_");
        Assertions.assertNotNull(notLike);
        Assertions.assertEquals("username", notLike.getPropName());
        Assertions.assertEquals("test_%", notLike.getPropValue());
    }

    @Test
    public void testEndsWith() {
        Like like = Filters.endsWith("email", "@example.com");
        Assertions.assertNotNull(like);
        Assertions.assertEquals("email", like.getPropName());
        Assertions.assertEquals("%@example.com", like.getPropValue());
    }

    @Test
    public void testNotEndsWith() {
        NotLike notLike = Filters.notEndsWith("filename", ".tmp");
        Assertions.assertNotNull(notLike);
        Assertions.assertEquals("filename", notLike.getPropName());
        Assertions.assertEquals("%.tmp", notLike.getPropValue());
    }

    @Test
    public void testIsNull() {
        IsNull isNull = Filters.isNull("deletedAt");
        Assertions.assertNotNull(isNull);
        Assertions.assertEquals("deletedAt", isNull.getPropName());
    }

    @Test
    public void testIsEmpty() {
        Or isEmpty = Filters.isNullOrEmpty("description");
        Assertions.assertNotNull(isEmpty);
        Assertions.assertEquals(2, isEmpty.getConditions().size());
    }

    @Test
    public void testIsNullOrZero() {
        Or isNullOrZero = Filters.isNullOrZero("count");
        Assertions.assertNotNull(isNullOrZero);
        Assertions.assertEquals(2, isNullOrZero.getConditions().size());
    }

    @Test
    public void testIsNotNull() {
        IsNotNull isNotNull = Filters.isNotNull("createdAt");
        Assertions.assertNotNull(isNotNull);
        Assertions.assertEquals("createdAt", isNotNull.getPropName());
    }

    @Test
    public void testIsNaN() {
        IsNaN isNaN = Filters.isNaN("value");
        Assertions.assertNotNull(isNaN);
        Assertions.assertEquals("value", isNaN.getPropName());
    }

    @Test
    public void testIsNotNaN() {
        IsNotNaN isNotNaN = Filters.isNotNaN("price");
        Assertions.assertNotNull(isNotNaN);
        Assertions.assertEquals("price", isNotNaN.getPropName());
    }

    @Test
    public void testIsInfinite() {
        IsInfinite isInfinite = Filters.isInfinite("result");
        Assertions.assertNotNull(isInfinite);
        Assertions.assertEquals("result", isInfinite.getPropName());
    }

    @Test
    public void testIsNotInfinite() {
        IsNotInfinite isNotInfinite = Filters.isNotInfinite("calculation");
        Assertions.assertNotNull(isNotInfinite);
        Assertions.assertEquals("calculation", isNotInfinite.getPropName());
    }

    @Test
    public void testIs() {
        Is is = Filters.is("status", "active");
        Assertions.assertNotNull(is);
        Assertions.assertEquals("status", is.getPropName());
        Assertions.assertEquals("active", is.getPropValue());
    }

    @Test
    public void testIsNot() {
        IsNot isNot = Filters.isNot("type", "guest");
        Assertions.assertNotNull(isNot);
        Assertions.assertEquals("type", isNot.getPropName());
        Assertions.assertEquals("guest", isNot.getPropValue());
    }

    @Test
    public void testOr() {
        // Test with array of conditions
        Condition cond1 = Filters.eq("status", "active");
        Condition cond2 = Filters.eq("status", "pending");
        Or or1 = Filters.or(cond1, cond2);
        Assertions.assertNotNull(or1);
        Assertions.assertEquals(2, or1.getConditions().size());

        // Test with collection of conditions
        List<Condition> conditions = Arrays.asList(cond1, cond2);
        Or or2 = Filters.or(conditions);
        Assertions.assertNotNull(or2);
        Assertions.assertEquals(2, or2.getConditions().size());
    }

    @Test
    public void testAnd() {
        // Test with array of conditions
        Condition cond1 = Filters.gt("age", 18);
        Condition cond2 = Filters.lt("age", 65);
        And and1 = Filters.and(cond1, cond2);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Test with collection of conditions
        List<Condition> conditions = Arrays.asList(cond1, cond2);
        And and2 = Filters.and(conditions);
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testJunction() {
        // Test with array of conditions
        Condition cond1 = Filters.eq("a", 1);
        Condition cond2 = Filters.eq("b", 2);
        Junction junction1 = Filters.junction(Operator.OR, cond1, cond2);
        Assertions.assertNotNull(junction1);
        Assertions.assertEquals(Operator.OR, junction1.operator());
        Assertions.assertEquals(2, junction1.getConditions().size());

        // Test with collection of conditions
        List<Condition> conditions = Arrays.asList(cond1, cond2);
        Junction junction2 = Filters.junction(Operator.AND, conditions);
        Assertions.assertNotNull(junction2);
        Assertions.assertEquals(Operator.AND, junction2.operator());
        Assertions.assertEquals(2, junction2.getConditions().size());
    }

    @Test
    public void testWhere() {
        // Test with condition
        Condition condition = Filters.eq("status", "active");
        Where where1 = Filters.where(condition);
        Assertions.assertNotNull(where1);
        Assertions.assertEquals(condition, where1.getCondition());

        // Test with string
        Where where2 = Filters.where("age > 18");
        Assertions.assertNotNull(where2);
        Assertions.assertNotNull(where2.getCondition());
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
        Assertions.assertEquals(condition, having1.getCondition());

        // Test with string
        Having having2 = Filters.having("COUNT(*) > 10");
        Assertions.assertNotNull(having2);
        Assertions.assertNotNull(having2.getCondition());
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
        Assertions.assertEquals(condition, on1.getCondition());

        // Test with string
        On on2 = Filters.on("a.id = b.a_id");
        Assertions.assertNotNull(on2);
        Assertions.assertNotNull(on2.getCondition());

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

        // Test with entity name and condition
        Condition condition = Filters.expr("1=1");
        CrossJoin crossJoin2 = Filters.crossJoin("products", condition);
        Assertions.assertNotNull(crossJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("products", "categories");
        CrossJoin crossJoin3 = Filters.crossJoin(entities, condition);
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

        // Test with entity name and condition
        Condition condition = Filters.expr("TRUE");
        NaturalJoin naturalJoin2 = Filters.naturalJoin("departments", condition);
        Assertions.assertNotNull(naturalJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("employees", "departments");
        NaturalJoin naturalJoin3 = Filters.naturalJoin(entities, condition);
        Assertions.assertNotNull(naturalJoin3);
    }

    @Test
    public void testIn() {
        // Test with int array
        In in1 = Filters.in("age", new int[] { 18, 21, 25 });
        Assertions.assertNotNull(in1);
        Assertions.assertEquals("age", in1.getPropName());

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
    public void testInSubQuery() {
        // Test with single property
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        InSubQuery in1 = Filters.in("userId", subQuery);
        Assertions.assertNotNull(in1);

        // Test with multiple properties
        List<String> props = Arrays.asList("userId", "departmentId");
        InSubQuery in2 = Filters.in(props, subQuery);
        Assertions.assertNotNull(in2);
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

    @Test
    public void testNotInSubQuery() {
        // Test with single property
        SubQuery subQuery = Filters.subQuery("SELECT id FROM blacklist");
        NotInSubQuery notIn1 = Filters.notIn("userId", subQuery);
        Assertions.assertNotNull(notIn1);

        // Test with multiple properties
        List<String> props = Arrays.asList("email", "phone");
        NotInSubQuery notIn2 = Filters.notIn(props, subQuery);
        Assertions.assertNotNull(notIn2);
    }

    @Test
    public void testAll() {
        SubQuery subQuery = Filters.subQuery("SELECT price FROM products");
        All all = Filters.all(subQuery);
        Assertions.assertNotNull(all);
    }

    @Test
    public void testAny() {
        SubQuery subQuery = Filters.subQuery("SELECT score FROM tests");
        Any any = Filters.any(subQuery);
        Assertions.assertNotNull(any);
    }

    @Test
    public void testSome() {
        SubQuery subQuery = Filters.subQuery("SELECT level FROM users");
        Some some = Filters.some(subQuery);
        Assertions.assertNotNull(some);
    }

    @Test
    public void testExists() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE user_id = users.id");
        Exists exists = Filters.exists(subQuery);
        Assertions.assertNotNull(exists);
    }

    @Test
    public void testNotExists() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM blacklist WHERE email = users.email");
        NotExists notExists = Filters.notExists(subQuery);
        Assertions.assertNotNull(notExists);
    }

    @Test
    public void testUnion() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM archived_users");
        Union union = Filters.union(subQuery);
        Assertions.assertNotNull(union);
    }

    @Test
    public void testUnionAll() {
        SubQuery subQuery = Filters.subQuery("SELECT name FROM products");
        UnionAll unionAll = Filters.unionAll(subQuery);
        Assertions.assertNotNull(unionAll);
    }

    @Test
    public void testExcept() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM deleted_items");
        Except except = Filters.except(subQuery);
        Assertions.assertNotNull(except);
    }

    @Test
    public void testIntersect() {
        SubQuery subQuery = Filters.subQuery("SELECT user_id FROM premium_users");
        Intersect intersect = Filters.intersect(subQuery);
        Assertions.assertNotNull(intersect);
    }

    @Test
    public void testMinus() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_accounts");
        Minus minus = Filters.minus(subQuery);
        Assertions.assertNotNull(minus);
    }

    //    @Test
    //    public void testCell() {
    //        Condition condition = Filters.eq("status", "active");
    //        Cell cell = Filters.cell(Operator.AND, condition);
    //        Assertions.assertNotNull(cell);
    //        Assertions.assertEquals(Operator.AND, cell.operator());
    //        Assertions.assertEquals(condition, cell.getCondition());
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
        Assertions.assertEquals(10, limit1.getCount());

        // Test with offset and count
        Limit limit2 = Filters.limit(50, 20);
        Assertions.assertNotNull(limit2);
        Assertions.assertEquals(20, limit2.getOffset());
        Assertions.assertEquals(50, limit2.getCount());

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
}

class FiltersJavadocExamples extends TestBase {

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
        Expression expr = Filters.expr("UPPER(name) = 'JOHN'");
        assertNotNull(expr);
        assertEquals("UPPER(name) = 'JOHN'", expr.getLiteral());
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
}
