package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.ConditionFactory;
import com.landawn.abacus.query.condition.CrossJoin;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.GreaterThan;
import com.landawn.abacus.query.condition.In;
import com.landawn.abacus.query.condition.Like;
import com.landawn.abacus.query.condition.NotEqual;
import com.landawn.abacus.query.condition.Operator;

public class CrossJoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        CrossJoin join = ConditionFactory.crossJoin("products");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.CROSS_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("products"));
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Equal eq = ConditionFactory.eq("available", true);
        CrossJoin join = ConditionFactory.crossJoin("products", eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.CROSS_JOIN, join.getOperator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("products"));
        Assertions.assertEquals(eq, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("sizes", "colors", "styles");
        Equal eq = ConditionFactory.eq("active", true);
        CrossJoin join = ConditionFactory.crossJoin(entities, eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.CROSS_JOIN, join.getOperator());
        Assertions.assertEquals(3, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(entities));
        Assertions.assertEquals(eq, join.getCondition());
    }

    @Test
    public void testToString() {
        CrossJoin join = ConditionFactory.crossJoin("categories");
        String result = join.toString();

        Assertions.assertTrue(result.contains("CROSS JOIN"));
        Assertions.assertTrue(result.contains("categories"));
    }

    @Test
    public void testToStringWithCondition() {
        GreaterThan gt = ConditionFactory.gt("price", 0);
        CrossJoin join = ConditionFactory.crossJoin("products", gt);
        String result = join.toString();

        Assertions.assertTrue(result.contains("CROSS JOIN"));
        Assertions.assertTrue(result.contains("products"));
        Assertions.assertTrue(result.contains("price > 0"));
    }

    @Test
    public void testToStringWithMultipleEntities() {
        List<String> entities = Arrays.asList("table1", "table2", "table3");
        Equal eq = ConditionFactory.eq("status", "active");
        CrossJoin join = ConditionFactory.crossJoin(entities, eq);
        String result = join.toString();

        Assertions.assertTrue(result.contains("CROSS JOIN"));
        Assertions.assertTrue(result.contains("table1"));
        Assertions.assertTrue(result.contains("table2"));
        Assertions.assertTrue(result.contains("table3"));
        Assertions.assertTrue(result.contains("status = 'active'"));
    }

    @Test
    public void testGetParameters() {
        Between between = ConditionFactory.between("quantity", 10, 100);
        CrossJoin join = ConditionFactory.crossJoin("inventory", between);

        List<Object> params = join.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(10, params.get(0));
        Assertions.assertEquals(100, params.get(1));
    }

    @Test
    public void testGetParametersWithoutCondition() {
        CrossJoin join = ConditionFactory.crossJoin("products");

        List<Object> params = join.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        In in = ConditionFactory.in("category_id", Arrays.asList(1, 2, 3));
        CrossJoin join = ConditionFactory.crossJoin("categories", in);

        join.clearParameters();

        List<Object> params = join.getParameters();
        Assertions.assertTrue(params.size() == 3 || params.stream().allMatch(p -> p == null));
    }

    @Test
    public void testCopy() {
        Like like = ConditionFactory.like("name", "%test%");
        CrossJoin original = ConditionFactory.crossJoin("products", like);

        CrossJoin copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
        Assertions.assertEquals((Condition) original.getCondition(), copy.getCondition());
        Assertions.assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testEquals() {
        Equal eq1 = ConditionFactory.eq("active", true);
        Equal eq2 = ConditionFactory.eq("active", true);

        CrossJoin join1 = ConditionFactory.crossJoin("products", eq1);
        CrossJoin join2 = ConditionFactory.crossJoin("products", eq2);
        CrossJoin join3 = ConditionFactory.crossJoin("categories", eq1);
        CrossJoin join4 = ConditionFactory.crossJoin("products");

        Assertions.assertEquals(join1, join2);
        Assertions.assertNotEquals(join1, join3);
        Assertions.assertNotEquals(join1, join4);
        Assertions.assertNotEquals(join1, null);
        Assertions.assertNotEquals(join1, "string");
    }

    @Test
    public void testHashCode() {
        NotEqual ne = ConditionFactory.ne("deleted", true);
        CrossJoin join1 = ConditionFactory.crossJoin("items", ne);
        CrossJoin join2 = ConditionFactory.crossJoin("items", ne);

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testInheritedJoinMethods() {
        CrossJoin join = ConditionFactory.crossJoin("products");

        // Test getJoinEntities
        Collection<String> entities = join.getJoinEntities();
        Assertions.assertEquals(1, entities.size());
        Assertions.assertTrue(entities.contains("products"));

        // Test getOperator
        Assertions.assertEquals(Operator.CROSS_JOIN, join.getOperator());
    }

}