package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for Junction.
 * Tests all public methods including constructors, condition management, parameters, copying, and string representation.
 */
@Tag("2025")
public class Junction2025Test extends TestBase {

    @Test
    public void testConstructorWithOperatorAndConditions() {
        Equal cond1 = Filters.eq("status", "active");
        GreaterThan cond2 = Filters.gt("age", 18);

        Junction junction = new Junction(Operator.AND, cond1, cond2);

        assertNotNull(junction);
        assertEquals(Operator.AND, junction.operator());
        assertEquals(2, junction.getConditions().size());
    }

    @Test
    public void testConstructorWithOperatorAndCollection() {
        List<Condition> conditions = Arrays.asList(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.isNotNull("email"));

        Junction junction = new Junction(Operator.OR, conditions);

        assertNotNull(junction);
        assertEquals(Operator.OR, junction.operator());
        assertEquals(3, junction.getConditions().size());
    }

    @Test
    public void testConstructorWithEmptyConditions() {
        Junction junction = new Junction(Operator.AND);

        assertNotNull(junction);
        assertEquals(Operator.AND, junction.operator());
        assertEquals(0, junction.getConditions().size());
    }

    @Test
    public void testGetConditions() {
        Equal cond1 = Filters.eq("name", "John");
        LessThan cond2 = Filters.lt("price", 100);

        Junction junction = new Junction(Operator.AND, cond1, cond2);
        List<Condition> conditions = junction.getConditions();

        assertNotNull(conditions);
        assertEquals(2, conditions.size());
        assertTrue(conditions.contains(cond1));
        assertTrue(conditions.contains(cond2));
    }

    @Test
    public void testAddConditionsArray() {
        // Commented out: Junction.add(...) APIs are currently commented out.
    }

    @Test
    public void testAddConditionsMultipleCalls() {
        // Commented out: Junction.add(...) APIs are currently commented out.
    }

    @Test
    public void testAddConditionsCollection() {
        // Commented out: Junction.add(...) APIs are currently commented out.
    }

    @Test
    public void testRemoveConditionsArray() {
        // Commented out: Junction.remove(...) APIs are currently commented out.
    }

    @Test
    public void testRemoveConditionsCollection() {
        // Commented out: Junction.remove(...) APIs are currently commented out.
    }

    @Test
    public void testClear() {
        // Commented out: Junction.clear() API is currently commented out.
    }

    @Test
    public void testGetParameters() {
        Junction junction = new Junction(Operator.AND, Filters.eq("status", "active"), Filters.between("age", 18, 65),
                Filters.in("city", new String[] { "NYC", "LA" }));

        List<Object> params = junction.getParameters();

        assertNotNull(params);
        assertEquals(5, params.size());
        assertEquals("active", params.get(0));
        assertEquals(18, params.get(1));
        assertEquals(65, params.get(2));
        assertEquals("NYC", params.get(3));
        assertEquals("LA", params.get(4));
    }

    @Test
    public void testGetParametersEmpty() {
        Junction junction = new Junction(Operator.OR);

        List<Object> params = junction.getParameters();

        assertNotNull(params);
        assertEquals(0, params.size());
    }

    @Test
    public void testGetParametersNested() {
        Junction innerJunction = new Junction(Operator.OR, Filters.eq("city", "NYC"), Filters.eq("city", "LA"));

        Junction outerJunction = new Junction(Operator.AND, Filters.eq("status", "active"), innerJunction);

        List<Object> params = outerJunction.getParameters();

        assertEquals(3, params.size());
        assertEquals("active", params.get(0));
        assertEquals("NYC", params.get(1));
        assertEquals("LA", params.get(2));
    }

    @Test
    public void testClearParameters() {
        Junction junction = new Junction(Operator.AND, Filters.eq("status", "active"), Filters.in("type", new String[] { "A", "B", "C" }));

        assertFalse(junction.getParameters().isEmpty());

        junction.clearParameters();

        List<Object> params = junction.getParameters();
        assertTrue(params.size() == 4 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopyPreservesConditions() {
        // Commented out: Junction.add(...) API is currently commented out.
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Junction junction = new Junction(Operator.AND, Filters.eq("status", "active"), Filters.gt("age", 18));

        String sql = junction.toString(NamingPolicy.SNAKE_CASE);

        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("age"));
    }

    @Test
    public void testToStringEmpty() {
        Junction junction = new Junction(Operator.AND);

        String sql = junction.toString(NamingPolicy.NO_CHANGE);

        assertEquals("", sql);
    }

    @Test
    public void testToStringWithParentheses() {
        Junction junction = new Junction(Operator.OR, Filters.eq("status", "active"), Filters.eq("status", "pending"));

        String sql = junction.toString(NamingPolicy.NO_CHANGE);

        assertTrue(sql.startsWith("("));
        assertTrue(sql.endsWith(")"));
        assertTrue(sql.contains(" OR "));
    }

    @Test
    public void testToStringNested() {
        Junction inner = new Junction(Operator.OR, Filters.eq("priority", 1), Filters.eq("priority", 2));

        Junction outer = new Junction(Operator.AND, Filters.eq("status", "active"), inner);

        String sql = outer.toString(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testHashCode() {
        Junction j1 = new Junction(Operator.AND, Filters.eq("a", 1));
        Junction j2 = new Junction(Operator.AND, Filters.eq("a", 1));

        assertEquals(j1.hashCode(), j2.hashCode());
    }

    @Test
    public void testHashCodeDifferent() {
        Junction j1 = new Junction(Operator.AND, Filters.eq("a", 1));
        Junction j2 = new Junction(Operator.OR, Filters.eq("a", 1));

        assertNotEquals(j1.hashCode(), j2.hashCode());
    }

    @Test
    public void testEquals() {
        Junction j1 = new Junction(Operator.AND, Filters.eq("status", "active"), Filters.gt("age", 18));

        Junction j2 = new Junction(Operator.AND, Filters.eq("status", "active"), Filters.gt("age", 18));

        assertEquals(j1, j2);
    }

    @Test
    public void testEqualsSameInstance() {
        Junction j1 = new Junction(Operator.AND, Filters.eq("a", 1));

        assertEquals(j1, j1);
    }

    @Test
    public void testEqualsNull() {
        Junction j1 = new Junction(Operator.AND, Filters.eq("a", 1));

        assertNotEquals(j1, null);
    }

    @Test
    public void testEqualsDifferentType() {
        Junction j1 = new Junction(Operator.AND, Filters.eq("a", 1));
        String other = "not a junction";

        assertNotEquals(j1, other);
    }

    @Test
    public void testEqualsDifferentOperator() {
        Junction j1 = new Junction(Operator.AND, Filters.eq("a", 1));
        Junction j2 = new Junction(Operator.OR, Filters.eq("a", 1));

        assertNotEquals(j1, j2);
    }

    @Test
    public void testEqualsDifferentConditions() {
        Junction j1 = new Junction(Operator.AND, Filters.eq("a", 1));
        Junction j2 = new Junction(Operator.AND, Filters.eq("b", 2));

        assertNotEquals(j1, j2);
    }

    @Test
    public void testComplexJunctionAndOperation() {
        Junction junction = new Junction(Operator.AND, Filters.eq("status", "active"), Filters.between("age", 18, 65), Filters.isNotNull("email"),
                Filters.like("name", "John%"));

        assertEquals(4, junction.getConditions().size());
        String sql = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testComplexJunctionOrOperation() {
        Junction junction = new Junction(Operator.OR, Filters.eq("priority", 1), Filters.eq("urgent", true), Filters.lt("deadline", "2025-01-01"));

        assertEquals(3, junction.getConditions().size());
        String sql = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testJunctionWithSingleCondition() {
        Junction junction = new Junction(Operator.AND, Filters.eq("status", "active"));

        assertEquals(1, junction.getConditions().size());
        assertFalse(junction.toString(NamingPolicy.NO_CHANGE).isEmpty());
    }

    @Test
    public void testJunctionConditionsListIsUnmodifiable() {
        Junction junction = new Junction(Operator.AND);
        List<Condition> conditions = junction.getConditions();

        assertThrows(UnsupportedOperationException.class, () -> conditions.add(Filters.eq("test", "value")));
    }

    @Test
    public void testAddConditions_WithNullArray() {
        // Commented out: Junction.add(...) APIs are currently commented out.
    }

    @Test
    public void testAddConditions_WithNullElement() {
        // Commented out: Junction.add(...) APIs are currently commented out.
    }

    @Test
    public void testAddConditions_CollectionWithNullElement() {
        // Commented out: Junction.add(...) APIs are currently commented out.
    }

    @Test
    public void testAddConditions_NullCollection() {
        // Commented out: Junction.add(...) APIs are currently commented out.
    }

    @Test
    public void testConstructor_WithNullElementInVarargs() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Junction(Operator.AND, Filters.eq("a", 1), null, Filters.eq("b", 2));
        });
    }

    @Test
    public void testConstructor_WithNullElementInCollection() {
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(Filters.eq("a", 1));
        conditions.add(null);

        assertThrows(IllegalArgumentException.class, () -> {
            new Junction(Operator.OR, conditions);
        });
    }

    @Test
    public void testToString_WithNullConditionInList() {
        Junction junction = new Junction(Operator.AND);
        // Access package-private field directly since getConditions() now returns an unmodifiable view
        junction.conditions.add(Filters.eq("a", 1));
        junction.conditions.add(null);
        junction.conditions.add(Filters.eq("b", 2));

        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertNotNull(result);
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    public void testGetParameters_WithNullConditionInList() {
        Junction junction = new Junction(Operator.AND);
        junction.conditions.add(Filters.eq("a", 1));
        junction.conditions.add(null);
        junction.conditions.add(Filters.eq("b", 2));

        List<Object> params = junction.getParameters();
        assertEquals(2, params.size());
        assertEquals(1, params.get(0));
        assertEquals(2, params.get(1));
    }

    @Test
    public void testClearParameters_WithNullConditionInList() {
        Junction junction = new Junction(Operator.AND);
        junction.conditions.add(Filters.eq("a", 1));
        junction.conditions.add(null);
        junction.conditions.add(Filters.eq("b", 2));

        assertDoesNotThrow(() -> junction.clearParameters());
    }

    @Test
    public void testRemoveConditions_VarargsWithMultiple() {
        // Commented out: Junction.remove(...) APIs are currently commented out.
    }

    @Test
    public void testToString_DefaultNamingPolicy() {
        Junction junction = new Junction(Operator.AND, Filters.eq("userName", "John"), Filters.gt("userAge", 18));

        String sql = junction.toString();
        assertNotNull(sql);
        assertTrue(sql.contains("userName") || sql.contains("user_name"));
    }
}
