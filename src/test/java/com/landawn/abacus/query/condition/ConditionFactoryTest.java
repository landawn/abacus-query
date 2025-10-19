package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.condition.All;
import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.Any;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Binary;
import com.landawn.abacus.query.condition.Cell;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.CrossJoin;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.Except;
import com.landawn.abacus.query.condition.Exists;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.FullJoin;
import com.landawn.abacus.query.condition.GreaterEqual;
import com.landawn.abacus.query.condition.GreaterThan;
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
import com.landawn.abacus.query.condition.LessEqual;
import com.landawn.abacus.query.condition.LessThan;
import com.landawn.abacus.query.condition.Like;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.Minus;
import com.landawn.abacus.query.condition.NamedProperty;
import com.landawn.abacus.query.condition.NaturalJoin;
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
import com.landawn.abacus.query.condition.XOR;
import com.landawn.abacus.query.condition.ConditionFactory.CB;
import com.landawn.abacus.query.condition.ConditionFactory.CF;

public class ConditionFactoryTest extends TestBase {

    @Test
    public void testAlwaysTrue() {
        Expression expr = CF.alwaysTrue();
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("1 < 2", expr.getLiteral());
    }

    @Test
    public void testAlwaysFalse() {
        Expression expr = CF.alwaysFalse();
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("1 > 2", expr.getLiteral());
    }

    @Test
    public void testNamedProperty() {
        NamedProperty prop = CF.namedProperty("testProp");
        Assertions.assertNotNull(prop);
        Assertions.assertEquals("testProp", prop.propName());
    }

    @Test
    public void testExpr() {
        Expression expr = CF.expr("age > 18");
        Assertions.assertNotNull(expr);
        Assertions.assertEquals("age > 18", expr.getLiteral());
    }

    @Test
    public void testBinary() {
        Binary binary = CF.binary("name", Operator.EQUAL, "John");
        Assertions.assertNotNull(binary);
        Assertions.assertEquals("name", binary.getPropName());
        Assertions.assertEquals(Operator.EQUAL, binary.getOperator());
        Assertions.assertEquals("John", binary.getPropValue());
    }

    @Test
    public void testEqual() {
        // Test with value
        Equal eq1 = CF.equal("status", "active");
        Assertions.assertNotNull(eq1);
        Assertions.assertEquals("status", eq1.getPropName());
        Assertions.assertEquals("active", eq1.getPropValue());

        // Test without value (for parameterized SQL)
        Equal eq2 = CF.equal("status");
        Assertions.assertNotNull(eq2);
        Assertions.assertEquals("status", eq2.getPropName());
        Assertions.assertEquals(CF.QME, eq2.getPropValue());
    }

    @Test
    public void testEq() {
        // Test with value
        Equal eq1 = CF.eq("age", 25);
        Assertions.assertNotNull(eq1);
        Assertions.assertEquals("age", eq1.getPropName());
        Assertions.assertEquals(25, (Integer) eq1.getPropValue());

        // Test without value
        Equal eq2 = CF.eq("age");
        Assertions.assertNotNull(eq2);
        Assertions.assertEquals("age", eq2.getPropName());
        Assertions.assertEquals(CF.QME, eq2.getPropValue());
    }

    @Test
    public void testEqOrWithArray() {
        Or or = CF.eqOr("status", "active", "pending", "approved");
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testInCollection() {
        List<String> values = Arrays.asList("red", "green", "blue");
        In in = CF.in("color", values);
        Assertions.assertNotNull(in);
    }

    @Test
    public void testEqOrWithMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 25);
        props.put("status", "active");

        Or or = CF.eqOr(props);
        Assertions.assertNotNull(or);
        Assertions.assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testEqOrWithTwoProperties() {
        Or or = CF.eqOr("name", "John", "age", 25);
        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testEqOrWithThreeProperties() {
        Or or = CF.eqOr("name", "John", "age", 25, "status", "active");
        Assertions.assertNotNull(or);
        Assertions.assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testEqAndWithMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "John");
        props.put("age", 25);

        And and = CF.eqAnd(props);
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testEqAndWithTwoProperties() {
        And and = CF.eqAnd("name", "John", "age", 25);
        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
    }

    @Test
    public void testEqAndWithThreeProperties() {
        And and = CF.eqAnd("name", "John", "age", 25, "status", "active");
        Assertions.assertNotNull(and);
        Assertions.assertEquals(3, and.getConditions().size());
    }

    @Test
    public void testGtAndLt() {
        // With values
        And and1 = CF.gtAndLt("age", 18, 65);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Without values
        And and2 = CF.gtAndLt("age");
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testGeAndLt() {
        // With values
        And and1 = CF.geAndLt("score", 0, 100);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Without values
        And and2 = CF.geAndLt("score");
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testGeAndLe() {
        // With values
        And and1 = CF.geAndLe("price", 10.0, 100.0);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Without values
        And and2 = CF.geAndLe("price");
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testGtAndLe() {
        // With values
        And and1 = CF.gtAndLe("temperature", -10, 40);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Without values
        And and2 = CF.gtAndLe("temperature");
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testNotEqual() {
        // With value
        NotEqual ne1 = CF.notEqual("status", "deleted");
        Assertions.assertNotNull(ne1);
        Assertions.assertEquals("status", ne1.getPropName());
        Assertions.assertEquals("deleted", ne1.getPropValue());

        // Without value
        NotEqual ne2 = CF.notEqual("status");
        Assertions.assertNotNull(ne2);
        Assertions.assertEquals(CF.QME, ne2.getPropValue());
    }

    @Test
    public void testNe() {
        // With value
        NotEqual ne1 = CF.ne("type", "admin");
        Assertions.assertNotNull(ne1);
        Assertions.assertEquals("type", ne1.getPropName());
        Assertions.assertEquals("admin", ne1.getPropValue());

        // Without value
        NotEqual ne2 = CF.ne("type");
        Assertions.assertNotNull(ne2);
        Assertions.assertEquals(CF.QME, ne2.getPropValue());
    }

    @Test
    public void testGreaterThan() {
        // With value
        GreaterThan gt1 = CF.greaterThan("age", 18);
        Assertions.assertNotNull(gt1);
        Assertions.assertEquals("age", gt1.getPropName());
        Assertions.assertEquals(18, (Integer) (Integer) gt1.getPropValue());

        // Without value
        GreaterThan gt2 = CF.greaterThan("age");
        Assertions.assertNotNull(gt2);
        Assertions.assertEquals(CF.QME, gt2.getPropValue());
    }

    @Test
    public void testGt() {
        // With value
        GreaterThan gt1 = CF.gt("score", 60);
        Assertions.assertNotNull(gt1);
        Assertions.assertEquals("score", gt1.getPropName());
        Assertions.assertEquals(60, (Integer) gt1.getPropValue());

        // Without value
        GreaterThan gt2 = CF.gt("score");
        Assertions.assertNotNull(gt2);
        Assertions.assertEquals(CF.QME, gt2.getPropValue());
    }

    @Test
    public void testGreaterEqual() {
        // With value
        GreaterEqual ge1 = CF.greaterEqual("level", 5);
        Assertions.assertNotNull(ge1);
        Assertions.assertEquals("level", ge1.getPropName());
        Assertions.assertEquals(5, (Integer) ge1.getPropValue());

        // Without value
        GreaterEqual ge2 = CF.greaterEqual("level");
        Assertions.assertNotNull(ge2);
        Assertions.assertEquals(CF.QME, ge2.getPropValue());
    }

    @Test
    public void testGe() {
        // With value
        GreaterEqual ge1 = CF.ge("rating", 4.0);
        Assertions.assertNotNull(ge1);
        Assertions.assertEquals("rating", ge1.getPropName());
        Assertions.assertEquals(4.0, ge1.getPropValue());

        // Without value
        GreaterEqual ge2 = CF.ge("rating");
        Assertions.assertNotNull(ge2);
        Assertions.assertEquals(CF.QME, ge2.getPropValue());
    }

    @Test
    public void testLessThan() {
        // With value
        LessThan lt1 = CF.lessThan("price", 100);
        Assertions.assertNotNull(lt1);
        Assertions.assertEquals("price", lt1.getPropName());
        Assertions.assertEquals(100, (Integer) lt1.getPropValue());

        // Without value
        LessThan lt2 = CF.lessThan("price");
        Assertions.assertNotNull(lt2);
        Assertions.assertEquals(CF.QME, lt2.getPropValue());
    }

    @Test
    public void testLt() {
        // With value
        LessThan lt1 = CF.lt("quantity", 10);
        Assertions.assertNotNull(lt1);
        Assertions.assertEquals("quantity", lt1.getPropName());
        Assertions.assertEquals(10, (Integer) lt1.getPropValue());

        // Without value
        LessThan lt2 = CF.lt("quantity");
        Assertions.assertNotNull(lt2);
        Assertions.assertEquals(CF.QME, lt2.getPropValue());
    }

    @Test
    public void testLessEqual() {
        // With value
        LessEqual le1 = CF.lessEqual("discount", 50);
        Assertions.assertNotNull(le1);
        Assertions.assertEquals("discount", le1.getPropName());
        Assertions.assertEquals(50, (Integer) le1.getPropValue());

        // Without value
        LessEqual le2 = CF.lessEqual("discount");
        Assertions.assertNotNull(le2);
        Assertions.assertEquals(CF.QME, le2.getPropValue());
    }

    @Test
    public void testLe() {
        // With value
        LessEqual le1 = CF.le("temperature", 30);
        Assertions.assertNotNull(le1);
        Assertions.assertEquals("temperature", le1.getPropName());
        Assertions.assertEquals(30, (Integer) le1.getPropValue());

        // Without value
        LessEqual le2 = CF.le("temperature");
        Assertions.assertNotNull(le2);
        Assertions.assertEquals(CF.QME, le2.getPropValue());
    }

    @Test
    public void testBetween() {
        // With values
        Between bt1 = CF.between("age", 18, 65);
        Assertions.assertNotNull(bt1);
        Assertions.assertEquals("age", bt1.getPropName());
        Assertions.assertEquals(18, (Integer) (Integer) bt1.getMinValue());
        Assertions.assertEquals(65, (Integer) bt1.getMaxValue());

        // Without values
        Between bt2 = CF.between("age");
        Assertions.assertNotNull(bt2);
        Assertions.assertEquals(CF.QME, bt2.getMinValue());
        Assertions.assertEquals(CF.QME, bt2.getMaxValue());
    }

    @Test
    public void testBt() {
        // With values
        Between bt1 = CF.bt("salary", 30000, 100000);
        Assertions.assertNotNull(bt1);
        Assertions.assertEquals("salary", bt1.getPropName());
        Assertions.assertEquals(30000, (Integer) bt1.getMinValue());
        Assertions.assertEquals(100000, (Integer) bt1.getMaxValue());

        // Without values
        Between bt2 = CF.bt("salary");
        Assertions.assertNotNull(bt2);
        Assertions.assertEquals(CF.QME, bt2.getMinValue());
        Assertions.assertEquals(CF.QME, bt2.getMaxValue());
    }

    @Test
    public void testNotBetween() {
        // With values
        NotBetween nbt1 = CF.notBetween("score", 0, 50);
        Assertions.assertNotNull(nbt1);
        Assertions.assertEquals("score", nbt1.getPropName());
        Assertions.assertEquals(0, (Integer) nbt1.getMinValue());
        Assertions.assertEquals(50, (Integer) nbt1.getMaxValue());

        // Without values
        NotBetween nbt2 = CF.notBetween("score");
        Assertions.assertNotNull(nbt2);
        Assertions.assertEquals(CF.QME, nbt2.getMinValue());
        Assertions.assertEquals(CF.QME, nbt2.getMaxValue());
    }

    @Test
    public void testLike() {
        // With value
        Like like1 = CF.like("name", "%John%");
        Assertions.assertNotNull(like1);
        Assertions.assertEquals("name", like1.getPropName());
        Assertions.assertEquals("%John%", like1.getPropValue());

        // Without value
        Like like2 = CF.like("name");
        Assertions.assertNotNull(like2);
        Assertions.assertEquals(CF.QME, like2.getPropValue());
    }

    @Test
    public void testNotLike() {
        // With value
        NotLike notLike1 = CF.notLike("description", "%spam%");
        Assertions.assertNotNull(notLike1);
        Assertions.assertEquals("description", notLike1.getPropName());
        Assertions.assertEquals("%spam%", notLike1.getPropValue());

        // Without value
        NotLike notLike2 = CF.notLike("description");
        Assertions.assertNotNull(notLike2);
        Assertions.assertEquals(CF.QME, notLike2.getPropValue());
    }

    @Test
    public void testContains() {
        Like like = CF.contains("name", "John");
        Assertions.assertNotNull(like);
        Assertions.assertEquals("name", like.getPropName());
        Assertions.assertEquals("%John%", like.getPropValue());
    }

    @Test
    public void testNotContains() {
        NotLike notLike = CF.notContains("description", "spam");
        Assertions.assertNotNull(notLike);
        Assertions.assertEquals("description", notLike.getPropName());
        Assertions.assertEquals("%spam%", notLike.getPropValue());
    }

    @Test
    public void testStartsWith() {
        Like like = CF.startsWith("email", "admin@");
        Assertions.assertNotNull(like);
        Assertions.assertEquals("email", like.getPropName());
        Assertions.assertEquals("admin@%", like.getPropValue());
    }

    @Test
    public void testNotStartsWith() {
        NotLike notLike = CF.notStartsWith("username", "test_");
        Assertions.assertNotNull(notLike);
        Assertions.assertEquals("username", notLike.getPropName());
        Assertions.assertEquals("test_%", notLike.getPropValue());
    }

    @Test
    public void testEndsWith() {
        Like like = CF.endsWith("email", "@example.com");
        Assertions.assertNotNull(like);
        Assertions.assertEquals("email", like.getPropName());
        Assertions.assertEquals("%@example.com", like.getPropValue());
    }

    @Test
    public void testNotEndsWith() {
        NotLike notLike = CF.notEndsWith("filename", ".tmp");
        Assertions.assertNotNull(notLike);
        Assertions.assertEquals("filename", notLike.getPropName());
        Assertions.assertEquals("%.tmp", notLike.getPropValue());
    }

    @Test
    public void testIsNull() {
        IsNull isNull = CF.isNull("deletedAt");
        Assertions.assertNotNull(isNull);
        Assertions.assertEquals("deletedAt", isNull.getPropName());
    }

    @Test
    public void testIsEmpty() {
        Or isEmpty = CF.isEmpty("description");
        Assertions.assertNotNull(isEmpty);
        Assertions.assertEquals(2, isEmpty.getConditions().size());
    }

    @Test
    public void testIsNullOrZero() {
        Or isNullOrZero = CF.isNullOrZero("count");
        Assertions.assertNotNull(isNullOrZero);
        Assertions.assertEquals(2, isNullOrZero.getConditions().size());
    }

    @Test
    public void testIsNotNull() {
        IsNotNull isNotNull = CF.isNotNull("createdAt");
        Assertions.assertNotNull(isNotNull);
        Assertions.assertEquals("createdAt", isNotNull.getPropName());
    }

    @Test
    public void testIsNaN() {
        IsNaN isNaN = CF.isNaN("value");
        Assertions.assertNotNull(isNaN);
        Assertions.assertEquals("value", isNaN.getPropName());
    }

    @Test
    public void testIsNotNaN() {
        IsNotNaN isNotNaN = CF.isNotNaN("price");
        Assertions.assertNotNull(isNotNaN);
        Assertions.assertEquals("price", isNotNaN.getPropName());
    }

    @Test
    public void testIsInfinite() {
        IsInfinite isInfinite = CF.isInfinite("result");
        Assertions.assertNotNull(isInfinite);
        Assertions.assertEquals("result", isInfinite.getPropName());
    }

    @Test
    public void testIsNotInfinite() {
        IsNotInfinite isNotInfinite = CF.isNotInfinite("calculation");
        Assertions.assertNotNull(isNotInfinite);
        Assertions.assertEquals("calculation", isNotInfinite.getPropName());
    }

    @Test
    public void testIs() {
        Is is = CF.is("status", "active");
        Assertions.assertNotNull(is);
        Assertions.assertEquals("status", is.getPropName());
        Assertions.assertEquals("active", is.getPropValue());
    }

    @Test
    public void testIsNot() {
        IsNot isNot = CF.isNot("type", "guest");
        Assertions.assertNotNull(isNot);
        Assertions.assertEquals("type", isNot.getPropName());
        Assertions.assertEquals("guest", isNot.getPropValue());
    }

    @Test
    public void testXor() {
        XOR xor = CF.xor("hasDiscount", true);
        Assertions.assertNotNull(xor);
        Assertions.assertEquals("hasDiscount", xor.getPropName());
        Assertions.assertEquals(true, xor.getPropValue());
    }

    @Test
    public void testOr() {
        // Test with array of conditions
        Condition cond1 = CF.eq("status", "active");
        Condition cond2 = CF.eq("status", "pending");
        Or or1 = CF.or(cond1, cond2);
        Assertions.assertNotNull(or1);
        Assertions.assertEquals(2, or1.getConditions().size());

        // Test with collection of conditions
        List<Condition> conditions = Arrays.asList(cond1, cond2);
        Or or2 = CF.or(conditions);
        Assertions.assertNotNull(or2);
        Assertions.assertEquals(2, or2.getConditions().size());
    }

    @Test
    public void testAnd() {
        // Test with array of conditions
        Condition cond1 = CF.gt("age", 18);
        Condition cond2 = CF.lt("age", 65);
        And and1 = CF.and(cond1, cond2);
        Assertions.assertNotNull(and1);
        Assertions.assertEquals(2, and1.getConditions().size());

        // Test with collection of conditions
        List<Condition> conditions = Arrays.asList(cond1, cond2);
        And and2 = CF.and(conditions);
        Assertions.assertNotNull(and2);
        Assertions.assertEquals(2, and2.getConditions().size());
    }

    @Test
    public void testJunction() {
        // Test with array of conditions
        Condition cond1 = CF.eq("a", 1);
        Condition cond2 = CF.eq("b", 2);
        Junction junction1 = CF.junction(Operator.OR, cond1, cond2);
        Assertions.assertNotNull(junction1);
        Assertions.assertEquals(Operator.OR, junction1.getOperator());
        Assertions.assertEquals(2, junction1.getConditions().size());

        // Test with collection of conditions
        List<Condition> conditions = Arrays.asList(cond1, cond2);
        Junction junction2 = CF.junction(Operator.AND, conditions);
        Assertions.assertNotNull(junction2);
        Assertions.assertEquals(Operator.AND, junction2.getOperator());
        Assertions.assertEquals(2, junction2.getConditions().size());
    }

    @Test
    public void testWhere() {
        // Test with condition
        Condition condition = CF.eq("status", "active");
        Where where1 = CF.where(condition);
        Assertions.assertNotNull(where1);
        Assertions.assertEquals(condition, where1.getCondition());

        // Test with string
        Where where2 = CF.where("age > 18");
        Assertions.assertNotNull(where2);
        Assertions.assertNotNull(where2.getCondition());
    }

    @Test
    public void testGroupBy() {
        // Test with varargs
        GroupBy gb1 = CF.groupBy("department", "team");
        Assertions.assertNotNull(gb1);

        // Test with collection
        List<String> props = Arrays.asList("category", "subcategory");
        GroupBy gb2 = CF.groupBy(props);
        Assertions.assertNotNull(gb2);

        // Test with collection and direction
        GroupBy gb3 = CF.groupBy(props, SortDirection.DESC);
        Assertions.assertNotNull(gb3);

        // Test with single property and direction
        GroupBy gb4 = CF.groupBy("name", SortDirection.ASC);
        Assertions.assertNotNull(gb4);

        // Test with map
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("year", SortDirection.DESC);
        orders.put("month", SortDirection.ASC);
        GroupBy gb5 = CF.groupBy(orders);
        Assertions.assertNotNull(gb5);

        // Test with condition
        Condition condition = CF.expr("department");
        GroupBy gb6 = CF.groupBy(condition);
        Assertions.assertNotNull(gb6);
    }

    @Test
    public void testGroupByMultiplePropertiesWithDirections() {
        // Test with 2 properties
        GroupBy gb1 = CF.groupBy("prop1", SortDirection.ASC, "prop2", SortDirection.DESC);
        Assertions.assertNotNull(gb1);

        // Test with 3 properties
        GroupBy gb2 = CF.groupBy("prop1", SortDirection.ASC, "prop2", SortDirection.DESC, "prop3", SortDirection.ASC);
        Assertions.assertNotNull(gb2);
    }

    @Test
    public void testHaving() {
        // Test with condition
        Condition condition = CF.gt("COUNT(*)", 5);
        Having having1 = CF.having(condition);
        Assertions.assertNotNull(having1);
        Assertions.assertEquals(condition, having1.getCondition());

        // Test with string
        Having having2 = CF.having("COUNT(*) > 10");
        Assertions.assertNotNull(having2);
        Assertions.assertNotNull(having2.getCondition());
    }

    @Test
    public void testOrderBy() {
        // Test with varargs
        OrderBy ob1 = CF.orderBy("name", "age");
        Assertions.assertNotNull(ob1);

        // Test orderByAsc with varargs
        OrderBy ob2 = CF.orderByAsc("firstName", "lastName");
        Assertions.assertNotNull(ob2);

        // Test orderByAsc with collection
        List<String> props = Arrays.asList("category", "price");
        OrderBy ob3 = CF.orderByAsc(props);
        Assertions.assertNotNull(ob3);

        // Test orderByDesc with varargs
        OrderBy ob4 = CF.orderByDesc("createdAt", "id");
        Assertions.assertNotNull(ob4);

        // Test orderByDesc with collection
        OrderBy ob5 = CF.orderByDesc(props);
        Assertions.assertNotNull(ob5);

        // Test with collection
        OrderBy ob6 = CF.orderBy(props);
        Assertions.assertNotNull(ob6);

        // Test with collection and direction
        OrderBy ob7 = CF.orderBy(props, SortDirection.DESC);
        Assertions.assertNotNull(ob7);

        // Test with single property and direction
        OrderBy ob8 = CF.orderBy("salary", SortDirection.DESC);
        Assertions.assertNotNull(ob8);

        // Test with map
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("date", SortDirection.ASC);
        OrderBy ob9 = CF.orderBy(orders);
        Assertions.assertNotNull(ob9);

        // Test with condition
        Condition condition = CF.expr("name ASC");
        OrderBy ob10 = CF.orderBy(condition);
        Assertions.assertNotNull(ob10);
    }

    @Test
    public void testOrderByMultiplePropertiesWithDirections() {
        // Test with 2 properties
        OrderBy ob1 = CF.orderBy("prop1", SortDirection.ASC, "prop2", SortDirection.DESC);
        Assertions.assertNotNull(ob1);

        // Test with 3 properties
        OrderBy ob2 = CF.orderBy("prop1", SortDirection.ASC, "prop2", SortDirection.DESC, "prop3", SortDirection.ASC);
        Assertions.assertNotNull(ob2);
    }

    @Test
    public void testOn() {
        // Test with condition
        Condition condition = CF.eq("a.id", "b.a_id");
        On on1 = CF.on(condition);
        Assertions.assertNotNull(on1);
        Assertions.assertEquals(condition, on1.getCondition());

        // Test with string
        On on2 = CF.on("a.id = b.a_id");
        Assertions.assertNotNull(on2);
        Assertions.assertNotNull(on2.getCondition());

        // Test with two property names
        On on3 = CF.on("userId", "user.id");
        Assertions.assertNotNull(on3);

        // Test with map
        Map<String, String> propMap = new HashMap<>();
        propMap.put("orderId", "order.id");
        propMap.put("productId", "product.id");
        On on4 = CF.on(propMap);
        Assertions.assertNotNull(on4);
    }

    @Test
    public void testUsing() {
        // Test with varargs
        Using using1 = CF.using("id", "name");
        Assertions.assertNotNull(using1);

        // Test with collection
        List<String> columns = Arrays.asList("userId", "departmentId");
        Using using2 = CF.using(columns);
        Assertions.assertNotNull(using2);
    }

    @Test
    public void testJoin() {
        // Test with entity name only
        Join join1 = CF.join("users");
        Assertions.assertNotNull(join1);

        // Test with entity name and condition
        Condition condition = CF.eq("users.id", "orders.user_id");
        Join join2 = CF.join("users", condition);
        Assertions.assertNotNull(join2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("users", "roles");
        Join join3 = CF.join(entities, condition);
        Assertions.assertNotNull(join3);
    }

    @Test
    public void testLeftJoin() {
        // Test with entity name only
        LeftJoin leftJoin1 = CF.leftJoin("orders");
        Assertions.assertNotNull(leftJoin1);

        // Test with entity name and condition
        Condition condition = CF.eq("users.id", "orders.user_id");
        LeftJoin leftJoin2 = CF.leftJoin("orders", condition);
        Assertions.assertNotNull(leftJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("orders", "order_items");
        LeftJoin leftJoin3 = CF.leftJoin(entities, condition);
        Assertions.assertNotNull(leftJoin3);
    }

    @Test
    public void testRightJoin() {
        // Test with entity name only
        RightJoin rightJoin1 = CF.rightJoin("departments");
        Assertions.assertNotNull(rightJoin1);

        // Test with entity name and condition
        Condition condition = CF.eq("employees.dept_id", "departments.id");
        RightJoin rightJoin2 = CF.rightJoin("departments", condition);
        Assertions.assertNotNull(rightJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("departments", "locations");
        RightJoin rightJoin3 = CF.rightJoin(entities, condition);
        Assertions.assertNotNull(rightJoin3);
    }

    @Test
    public void testCrossJoin() {
        // Test with entity name only
        CrossJoin crossJoin1 = CF.crossJoin("products");
        Assertions.assertNotNull(crossJoin1);

        // Test with entity name and condition
        Condition condition = CF.expr("1=1");
        CrossJoin crossJoin2 = CF.crossJoin("products", condition);
        Assertions.assertNotNull(crossJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("products", "categories");
        CrossJoin crossJoin3 = CF.crossJoin(entities, condition);
        Assertions.assertNotNull(crossJoin3);
    }

    @Test
    public void testFullJoin() {
        // Test with entity name only
        FullJoin fullJoin1 = CF.fullJoin("employees");
        Assertions.assertNotNull(fullJoin1);

        // Test with entity name and condition
        Condition condition = CF.eq("employees.id", "managers.employee_id");
        FullJoin fullJoin2 = CF.fullJoin("managers", condition);
        Assertions.assertNotNull(fullJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("employees", "departments");
        FullJoin fullJoin3 = CF.fullJoin(entities, condition);
        Assertions.assertNotNull(fullJoin3);
    }

    @Test
    public void testInnerJoin() {
        // Test with entity name only
        InnerJoin innerJoin1 = CF.innerJoin("roles");
        Assertions.assertNotNull(innerJoin1);

        // Test with entity name and condition
        Condition condition = CF.eq("users.role_id", "roles.id");
        InnerJoin innerJoin2 = CF.innerJoin("roles", condition);
        Assertions.assertNotNull(innerJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("users", "user_roles");
        InnerJoin innerJoin3 = CF.innerJoin(entities, condition);
        Assertions.assertNotNull(innerJoin3);
    }

    @Test
    public void testNaturalJoin() {
        // Test with entity name only
        NaturalJoin naturalJoin1 = CF.naturalJoin("departments");
        Assertions.assertNotNull(naturalJoin1);

        // Test with entity name and condition
        Condition condition = CF.expr("TRUE");
        NaturalJoin naturalJoin2 = CF.naturalJoin("departments", condition);
        Assertions.assertNotNull(naturalJoin2);

        // Test with collection of entities and condition
        List<String> entities = Arrays.asList("employees", "departments");
        NaturalJoin naturalJoin3 = CF.naturalJoin(entities, condition);
        Assertions.assertNotNull(naturalJoin3);
    }

    @Test
    public void testIn() {
        // Test with int array
        In in1 = CF.in("age", new int[] { 18, 21, 25 });
        Assertions.assertNotNull(in1);
        Assertions.assertEquals("age", in1.getPropName());

        // Test with long array
        In in2 = CF.in("id", new long[] { 1L, 2L, 3L });
        Assertions.assertNotNull(in2);

        // Test with double array
        In in3 = CF.in("price", new double[] { 9.99, 19.99, 29.99 });
        Assertions.assertNotNull(in3);

        // Test with object array
        In in4 = CF.in("status", new Object[] { "active", "pending", "approved" });
        Assertions.assertNotNull(in4);

        // Test with collection
        List<String> values = Arrays.asList("red", "green", "blue");
        In in5 = CF.in("color", values);
        Assertions.assertNotNull(in5);
    }

    @Test
    public void testInSubQuery() {
        // Test with single property
        SubQuery subQuery = CF.subQuery("SELECT id FROM users WHERE active = true");
        InSubQuery in1 = CF.in("userId", subQuery);
        Assertions.assertNotNull(in1);

        // Test with multiple properties
        List<String> props = Arrays.asList("userId", "departmentId");
        InSubQuery in2 = CF.in(props, subQuery);
        Assertions.assertNotNull(in2);
    }

    @Test
    public void testNotIn() {
        // Test with int array
        NotIn notIn1 = CF.notIn("status", new int[] { 0, -1 });
        Assertions.assertNotNull(notIn1);

        // Test with long array
        NotIn notIn2 = CF.notIn("excludeId", new long[] { 110L, 120L });
        Assertions.assertNotNull(notIn2);

        // Test with double array
        NotIn notIn3 = CF.notIn("discount", new double[] { 0.0, 100.0 });
        Assertions.assertNotNull(notIn3);

        // Test with object array
        NotIn notIn4 = CF.notIn("type", new Object[] { "guest", "bot" });
        Assertions.assertNotNull(notIn4);

        // Test with collection
        List<String> values = Arrays.asList("spam", "deleted", "hidden");
        NotIn notIn5 = CF.notIn("status", values);
        Assertions.assertNotNull(notIn5);
    }

    @Test
    public void testNotInSubQuery() {
        // Test with single property
        SubQuery subQuery = CF.subQuery("SELECT id FROM blacklist");
        NotInSubQuery notIn1 = CF.notIn("userId", subQuery);
        Assertions.assertNotNull(notIn1);

        // Test with multiple properties
        List<String> props = Arrays.asList("email", "phone");
        NotInSubQuery notIn2 = CF.notIn(props, subQuery);
        Assertions.assertNotNull(notIn2);
    }

    @Test
    public void testAll() {
        SubQuery subQuery = CF.subQuery("SELECT price FROM products");
        All all = CF.all(subQuery);
        Assertions.assertNotNull(all);
    }

    @Test
    public void testAny() {
        SubQuery subQuery = CF.subQuery("SELECT score FROM tests");
        Any any = CF.any(subQuery);
        Assertions.assertNotNull(any);
    }

    @Test
    public void testSome() {
        SubQuery subQuery = CF.subQuery("SELECT level FROM users");
        Some some = CF.some(subQuery);
        Assertions.assertNotNull(some);
    }

    @Test
    public void testExists() {
        SubQuery subQuery = CF.subQuery("SELECT 1 FROM orders WHERE user_id = users.id");
        Exists exists = CF.exists(subQuery);
        Assertions.assertNotNull(exists);
    }

    @Test
    public void testNotExists() {
        SubQuery subQuery = CF.subQuery("SELECT 1 FROM blacklist WHERE email = users.email");
        NotExists notExists = CF.notExists(subQuery);
        Assertions.assertNotNull(notExists);
    }

    @Test
    public void testUnion() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM archived_users");
        Union union = CF.union(subQuery);
        Assertions.assertNotNull(union);
    }

    @Test
    public void testUnionAll() {
        SubQuery subQuery = CF.subQuery("SELECT name FROM products");
        UnionAll unionAll = CF.unionAll(subQuery);
        Assertions.assertNotNull(unionAll);
    }

    @Test
    public void testExcept() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM deleted_items");
        Except except = CF.except(subQuery);
        Assertions.assertNotNull(except);
    }

    @Test
    public void testIntersect() {
        SubQuery subQuery = CF.subQuery("SELECT user_id FROM premium_users");
        Intersect intersect = CF.intersect(subQuery);
        Assertions.assertNotNull(intersect);
    }

    @Test
    public void testMinus() {
        SubQuery subQuery = CF.subQuery("SELECT id FROM inactive_accounts");
        Minus minus = CF.minus(subQuery);
        Assertions.assertNotNull(minus);
    }

    @Test
    public void testCell() {
        Condition condition = CF.eq("status", "active");
        Cell cell = CF.cell(Operator.AND, condition);
        Assertions.assertNotNull(cell);
        Assertions.assertEquals(Operator.AND, cell.getOperator());
        Assertions.assertEquals(condition, cell.getCondition());
    }

    @Test
    public void testSubQuery() {
        // Test with class, properties and condition
        List<String> props = Arrays.asList("id", "name");
        Condition condition = CF.eq("active", true);
        SubQuery sq1 = CF.subQuery(String.class, props, condition);
        Assertions.assertNotNull(sq1);

        // Test with entity name, properties and condition
        SubQuery sq2 = CF.subQuery("users", props, condition);
        Assertions.assertNotNull(sq2);

        // Test with entity name, properties and string condition
        SubQuery sq3 = CF.subQuery("users", props, "active = 1");
        Assertions.assertNotNull(sq3);

        // Test with entity name and sql (deprecated)
        SubQuery sq4 = CF.subQuery("users", "SELECT * FROM users");
        Assertions.assertNotNull(sq4);

        // Test with sql only
        SubQuery sq5 = CF.subQuery("SELECT COUNT(*) FROM orders");
        Assertions.assertNotNull(sq5);
    }

    @Test
    public void testLimit() {
        // Test with count only
        Limit limit1 = CF.limit(10);
        Assertions.assertNotNull(limit1);
        Assertions.assertEquals(10, limit1.getCount());

        // Test with offset and count
        Limit limit2 = CF.limit(20, 50);
        Assertions.assertNotNull(limit2);
        Assertions.assertEquals(20, limit2.getOffset());
        Assertions.assertEquals(50, limit2.getCount());

        // Test with expression
        Limit limit3 = CF.limit("10 OFFSET 5");
        Assertions.assertNotNull(limit3);
    }

    @Test
    public void testCriteria() {
        Criteria criteria = CF.criteria();
        Assertions.assertNotNull(criteria);
    }

    @Test
    public void testCF() {

        // Test a method via CF
        Equal eq = CF.CF.eq("test", "value");
        Assertions.assertNotNull(eq);
        Assertions.assertEquals("test", eq.getPropName());
        Assertions.assertEquals("value", eq.getPropValue());
    }

    @Test
    public void testCB() {
        // Test where with condition
        Condition condition = CF.eq("status", "active");
        Criteria criteria1 = CB.where(condition);
        Assertions.assertNotNull(criteria1);

        // Test where with string
        Criteria criteria2 = CB.where("age > 18");
        Assertions.assertNotNull(criteria2);

        // Test groupBy with condition
        Criteria criteria3 = CB.groupBy(condition);
        Assertions.assertNotNull(criteria3);

        // Test groupBy with varargs
        Criteria criteria4 = CB.groupBy("dept", "team");
        Assertions.assertNotNull(criteria4);

        // Test groupBy with property and direction
        Criteria criteria5 = CB.groupBy("salary", SortDirection.DESC);
        Assertions.assertNotNull(criteria5);

        // Test groupBy with collection
        List<String> props = Arrays.asList("year", "month");
        Criteria criteria6 = CB.groupBy(props);
        Assertions.assertNotNull(criteria6);

        // Test groupBy with collection and direction
        Criteria criteria7 = CB.groupBy(props, SortDirection.DESC);
        Assertions.assertNotNull(criteria7);

        // Test groupBy with map
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("date", SortDirection.ASC);
        Criteria criteria8 = CB.groupBy(orders);
        Assertions.assertNotNull(criteria8);

        // Test having with condition
        Criteria criteria9 = CB.having(condition);
        Assertions.assertNotNull(criteria9);

        // Test having with string
        Criteria criteria10 = CB.having("COUNT(*) > 5");
        Assertions.assertNotNull(criteria10);

        // Test orderByAsc with varargs
        Criteria criteria11 = CB.orderByAsc("firstName", "lastName");
        Assertions.assertNotNull(criteria11);

        // Test orderByAsc with collection
        Criteria criteria12 = CB.orderByAsc(props);
        Assertions.assertNotNull(criteria12);

        // Test orderByDesc with varargs
        Criteria criteria13 = CB.orderByDesc("createdAt", "id");
        Assertions.assertNotNull(criteria13);

        // Test orderByDesc with collection
        Criteria criteria14 = CB.orderByDesc(props);
        Assertions.assertNotNull(criteria14);

        // Test orderBy with condition
        Criteria criteria15 = CB.orderBy(condition);
        Assertions.assertNotNull(criteria15);

        // Test orderBy with varargs
        Criteria criteria16 = CB.orderBy("name", "age");
        Assertions.assertNotNull(criteria16);

        // Test orderBy with property and direction
        Criteria criteria17 = CB.orderBy("salary", SortDirection.DESC);
        Assertions.assertNotNull(criteria17);

        // Test orderBy with collection
        Criteria criteria18 = CB.orderBy(props);
        Assertions.assertNotNull(criteria18);

        // Test orderBy with collection and direction
        Criteria criteria19 = CB.orderBy(props, SortDirection.DESC);
        Assertions.assertNotNull(criteria19);

        // Test orderBy with map
        Criteria criteria20 = CB.orderBy(orders);
        Assertions.assertNotNull(criteria20);

        // Test limit with Limit condition
        Limit limitCond = CF.limit(10);
        Criteria criteria21 = CB.limit(limitCond);
        Assertions.assertNotNull(criteria21);

        // Test limit with count
        Criteria criteria22 = CB.limit(20);
        Assertions.assertNotNull(criteria22);

        // Test limit with offset and count
        Criteria criteria23 = CB.limit(10, 30);
        Assertions.assertNotNull(criteria23);

        // Test limit with expression
        Criteria criteria24 = CB.limit("50 OFFSET 100");
        Assertions.assertNotNull(criteria24);
    }
}