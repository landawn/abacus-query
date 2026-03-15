package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class CrossJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        CrossJoin join = new CrossJoin("colors");
        assertNotNull(join);
        assertEquals(Operator.CROSS_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        CrossJoin join = new CrossJoin("products", new Equal("available", true));
        assertNotNull(join);
        assertNotNull(join.getCondition());
        assertEquals(Operator.CROSS_JOIN, join.operator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("sizes", "colors", "styles");
        CrossJoin join = new CrossJoin(entities, new Equal("active", true));
        assertNotNull(join);
        assertEquals(3, (int) join.getJoinEntities().size());
        assertEquals(Operator.CROSS_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        CrossJoin join = new CrossJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("active", true);
        CrossJoin join = new CrossJoin("products", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        CrossJoin join = new CrossJoin("colors");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        CrossJoin join = new CrossJoin("sizes");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        CrossJoin join = new CrossJoin("products", new Equal("active", true));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    public void testToString_Simple() {
        CrossJoin join = new CrossJoin("colors");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("CROSS JOIN"));
        assertTrue(result.contains("colors"));
    }

    @Test
    public void testToString_WithCondition() {
        CrossJoin join = new CrossJoin("products", new Equal("active", true));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("CROSS JOIN"));
        assertTrue(result.contains("products"));
    }

    @Test
    public void testHashCode() {
        CrossJoin join1 = new CrossJoin("colors", new Equal("a", "b"));
        CrossJoin join2 = new CrossJoin("colors", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        CrossJoin join = new CrossJoin("colors");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        CrossJoin join1 = new CrossJoin("colors", new Equal("a", "b"));
        CrossJoin join2 = new CrossJoin("colors", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        CrossJoin join1 = new CrossJoin("colors");
        CrossJoin join2 = new CrossJoin("sizes");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        CrossJoin join = new CrossJoin("colors");
        assertNotEquals(null, join);
    }

    @Test
    public void testCartesianProduct() {
        CrossJoin join = new CrossJoin("colors");
        assertNotNull(join);
        assertEquals(Operator.CROSS_JOIN, join.operator());
        assertNull(join.getCondition());
    }

    @Test
    public void testAllCombinations() {
        List<String> tables = Arrays.asList("test_users", "test_permissions");
        CrossJoin join = new CrossJoin(tables, null);
        assertEquals(2, (int) join.getJoinEntities().size());
    }

    @Test
    public void testGenerateTestData() {
        CrossJoin join = new CrossJoin("available_times");
        assertNotNull(join);
        assertNull(join.getCondition());
    }
}

public class CrossJoinTest extends TestBase {

    @Test
    public void testConstructorWithJoinEntity() {
        CrossJoin join = Filters.crossJoin("products");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.CROSS_JOIN, join.operator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("products"));
    }

    @Test
    public void testConstructorWithJoinEntityAndCondition() {
        Equal eq = Filters.eq("available", true);
        CrossJoin join = Filters.crossJoin("products", eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.CROSS_JOIN, join.operator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("products"));
        Assertions.assertEquals(eq, join.getCondition());
    }

    @Test
    public void testConstructorWithMultipleEntities() {
        List<String> entities = Arrays.asList("sizes", "colors", "styles");
        Equal eq = Filters.eq("active", true);
        CrossJoin join = Filters.crossJoin(entities, eq);

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.CROSS_JOIN, join.operator());
        Assertions.assertEquals(3, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().containsAll(entities));
        Assertions.assertEquals(eq, join.getCondition());
    }

    @Test
    public void testToString() {
        CrossJoin join = Filters.crossJoin("categories");
        String result = join.toString();

        Assertions.assertTrue(result.contains("CROSS JOIN"));
        Assertions.assertTrue(result.contains("categories"));
    }

    @Test
    public void testToStringWithCondition() {
        GreaterThan gt = Filters.gt("price", 0);
        CrossJoin join = Filters.crossJoin("products", gt);
        String result = join.toString();

        Assertions.assertTrue(result.contains("CROSS JOIN"));
        Assertions.assertTrue(result.contains("products"));
        Assertions.assertTrue(result.contains("price > 0"));
    }

    @Test
    public void testToStringWithMultipleEntities() {
        List<String> entities = Arrays.asList("table1", "table2", "table3");
        Equal eq = Filters.eq("status", "active");
        CrossJoin join = Filters.crossJoin(entities, eq);
        String result = join.toString();

        Assertions.assertTrue(result.contains("CROSS JOIN"));
        Assertions.assertTrue(result.contains("table1"));
        Assertions.assertTrue(result.contains("table2"));
        Assertions.assertTrue(result.contains("table3"));
        Assertions.assertTrue(result.contains("status = 'active'"));
    }

    @Test
    public void testGetParameters() {
        Between between = Filters.between("quantity", 10, 100);
        CrossJoin join = Filters.crossJoin("inventory", between);

        List<Object> params = join.getParameters();
        Assertions.assertEquals(2, params.size());
        Assertions.assertEquals(10, params.get(0));
        Assertions.assertEquals(100, params.get(1));
    }

    @Test
    public void testGetParametersWithoutCondition() {
        CrossJoin join = Filters.crossJoin("products");

        List<Object> params = join.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testEquals() {
        Equal eq1 = Filters.eq("active", true);
        Equal eq2 = Filters.eq("active", true);

        CrossJoin join1 = Filters.crossJoin("products", eq1);
        CrossJoin join2 = Filters.crossJoin("products", eq2);
        CrossJoin join3 = Filters.crossJoin("categories", eq1);
        CrossJoin join4 = Filters.crossJoin("products");

        Assertions.assertEquals(join1, join2);
        Assertions.assertNotEquals(join1, join3);
        Assertions.assertNotEquals(join1, join4);
        Assertions.assertNotEquals(join1, null);
        Assertions.assertNotEquals(join1, "string");
    }

    @Test
    public void testHashCode() {
        NotEqual ne = Filters.ne("deleted", true);
        CrossJoin join1 = Filters.crossJoin("items", ne);
        CrossJoin join2 = Filters.crossJoin("items", ne);

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testInheritedJoinMethods() {
        CrossJoin join = Filters.crossJoin("products");

        // Test getJoinEntities
        Collection<String> entities = join.getJoinEntities();
        Assertions.assertEquals(1, entities.size());
        Assertions.assertTrue(entities.contains("products"));

        // Test getOperator
        Assertions.assertEquals(Operator.CROSS_JOIN, join.operator());
    }

}
