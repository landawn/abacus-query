package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
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
class NaturalJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        NaturalJoin join = new NaturalJoin("employees");
        assertNotNull(join);
        assertEquals(Operator.NATURAL_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NaturalJoin("departments", new Equal("status", "active")));
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("customers", "orders", "products");
        NaturalJoin join = new NaturalJoin(entities, null);
        assertNotNull(join);
        assertEquals(3, (int) join.getJoinEntities().size());
        assertEquals(Operator.NATURAL_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        NaturalJoin join = new NaturalJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        NaturalJoin join = new NaturalJoin("departments");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetCondition_Null() {
        NaturalJoin join = new NaturalJoin("employees");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        NaturalJoin join = new NaturalJoin("employees");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NaturalJoin("orders", new Equal("status", "completed")));
    }

    @Test
    public void testToString_Simple() {
        NaturalJoin join = new NaturalJoin("employees");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("NATURAL JOIN"));
        assertTrue(result.contains("employees"));
    }

    @Test
    public void testToString_WithCondition() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NaturalJoin("departments", new Equal("active", true)));
    }

    @Test
    public void testHashCode() {
        NaturalJoin join1 = new NaturalJoin("employees");
        NaturalJoin join2 = new NaturalJoin("employees");
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NaturalJoin join = new NaturalJoin("employees");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        NaturalJoin join1 = new NaturalJoin("employees");
        NaturalJoin join2 = new NaturalJoin("employees");
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        NaturalJoin join1 = new NaturalJoin("employees");
        NaturalJoin join2 = new NaturalJoin("departments");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        NaturalJoin join = new NaturalJoin("employees");
        assertNotEquals(null, join);
    }

    @Test
    public void testAutomaticColumcountMatchBetweening() {
        NaturalJoin join = new NaturalJoin("customers");
        assertNotNull(join);
        assertNull(join.getCondition());
    }

    @Test
    public void testWithAdditionalFilter() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NaturalJoin("orders", new GreaterThan("orderDate", "2024-01-01")));
    }
}

public class NaturalJoinTest extends TestBase {

    @Test
    public void testConstructorWithEntityOnly() {
        NaturalJoin join = Filters.naturalJoin("employees");

        Assertions.assertNotNull(join);
        Assertions.assertEquals(Operator.NATURAL_JOIN, join.operator());
        Assertions.assertEquals(1, join.getJoinEntities().size());
        Assertions.assertTrue(join.getJoinEntities().contains("employees"));
        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testConstructorWithEntityAndCondition() {
        Condition activeOnly = Filters.eq("status", "active");

        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.naturalJoin("departments", activeOnly));
    }

    @Test
    public void testConstructorWithMultipleEntitiesAndCondition() {
        List<String> tables = Arrays.asList("employees", "departments");
        Condition condition = Filters.gt("salary", 50000);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.naturalJoin(tables, condition));
    }

    @Test
    public void testGetJoinEntities() {
        NaturalJoin join = Filters.naturalJoin("orders");

        List<String> entities = join.getJoinEntities();
        Assertions.assertNotNull(entities);
        Assertions.assertEquals(1, entities.size());
        Assertions.assertEquals("orders", entities.get(0));
    }

    @Test
    public void testGetCondition() {
        NaturalJoin join = Filters.naturalJoin("orders");

        Assertions.assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters() {
        Equal condition = Filters.eq("active", true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.naturalJoin("users", condition));
    }

    @Test
    public void testGetParametersNoCondition() {
        NaturalJoin join = Filters.naturalJoin("employees");

        List<Object> params = join.getParameters();
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testToString() {
        NaturalJoin join = Filters.naturalJoin("departments");

        String result = join.toString();
        Assertions.assertTrue(result.contains("NATURAL JOIN"));
        Assertions.assertTrue(result.contains("departments"));
    }

    @Test
    public void testToStringWithCondition() {
        Equal condition = Filters.eq("active", true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.naturalJoin("users", condition));
    }

    @Test
    public void testHashCode() {
        NaturalJoin join1 = Filters.naturalJoin("employees");
        NaturalJoin join2 = Filters.naturalJoin("employees");
        NaturalJoin join3 = Filters.naturalJoin("departments");

        Assertions.assertEquals(join1.hashCode(), join2.hashCode());
        Assertions.assertNotEquals(join1.hashCode(), join3.hashCode());
    }

    @Test
    public void testEquals() {
        Equal condition = Filters.eq("active", true);

        NaturalJoin join1 = Filters.naturalJoin("employees");
        NaturalJoin join2 = Filters.naturalJoin("employees");
        NaturalJoin join3 = Filters.naturalJoin("departments");

        Assertions.assertTrue(join1.equals(join1));
        Assertions.assertTrue(join1.equals(join2));
        Assertions.assertFalse(join1.equals(join3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.naturalJoin("employees", condition));
        Assertions.assertFalse(join1.equals(null));
        Assertions.assertFalse(join1.equals("not a NaturalJoin"));
    }

    @Test
    public void testComplexCondition() {
        Condition complexCondition = Filters.and(Filters.eq("department", "Sales"), Filters.gt("experience", 5), Filters.like("skills", "%leadership%"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.naturalJoin("employees", complexCondition));
    }

    @Test
    public void testMultipleTablesComplexJoin() {
        List<String> tables = Arrays.asList("customers", "orders", "products");
        Condition highValue = Filters.gt("totalAmount", 1000);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Filters.naturalJoin(tables, highValue));
    }
}
