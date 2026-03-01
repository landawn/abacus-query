package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class NamedPropertyTest extends TestBase {

    @Test
    public void testConstructor() {
        NamedProperty prop = new NamedProperty("testProperty");
        Assertions.assertEquals("testProperty", prop.toString());
    }

    @Test
    public void testConstructorWithNull() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new NamedProperty(null);
        });
    }

    @Test
    public void testOfMethod() {
        NamedProperty prop = NamedProperty.of("username");
        Assertions.assertNotNull(prop);
        Assertions.assertEquals("username", prop.toString());
    }

    @Test
    public void testOfMethodCaching() {
        NamedProperty prop1 = NamedProperty.of("cachedProperty");
        NamedProperty prop2 = NamedProperty.of("cachedProperty");

        // Should return the same instance due to caching
        Assertions.assertSame(prop1, prop2);
    }

    @Test
    public void testOfMethodWithEmptyString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            NamedProperty.of("");
        });
    }

    @Test
    public void testOfMethodWithNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            NamedProperty.of(null);
        });
    }

    @Test
    public void testEq() {
        NamedProperty prop = NamedProperty.of("status");
        Equal condition = prop.eq("active");

        Assertions.assertEquals("status", condition.getPropName());
        Assertions.assertEquals("active", condition.getPropValue());
    }

    @Test
    public void testEqAnyOfWithArray() {
        NamedProperty prop = NamedProperty.of("color");
        Or condition = prop.eqAnyOf("red", "green", "blue");

        Assertions.assertEquals(3, condition.getConditions().size());
    }

    @Test
    public void testEqAnyOfWithCollection() {
        NamedProperty prop = NamedProperty.of("city");
        List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago");
        Or condition = prop.eqAnyOf(cities);

        Assertions.assertEquals(3, condition.getConditions().size());
    }

    @Test
    public void testEqAnyOfWithEmptyArray() {
        NamedProperty prop = NamedProperty.of("city");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            prop.eqAnyOf();
        });
    }

    @Test
    public void testEqAnyOfWithEmptyCollection() {
        NamedProperty prop = NamedProperty.of("city");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            prop.eqAnyOf(Arrays.asList());
        });
    }

    @Test
    public void testNe() {
        NamedProperty prop = NamedProperty.of("status");
        NotEqual condition = prop.ne("deleted");

        Assertions.assertEquals("status", condition.getPropName());
        Assertions.assertEquals("deleted", condition.getPropValue());
    }

    @Test
    public void testGt() {
        NamedProperty prop = NamedProperty.of("age");
        GreaterThan condition = prop.gt(18);

        Assertions.assertEquals("age", condition.getPropName());
        Assertions.assertEquals(18, (Integer) (Integer) condition.getPropValue());
    }

    @Test
    public void testGe() {
        NamedProperty prop = NamedProperty.of("score");
        GreaterThanOrEqual condition = prop.ge(60);

        Assertions.assertEquals("score", condition.getPropName());
        Assertions.assertEquals(60, (Integer) condition.getPropValue());
    }

    @Test
    public void testLt() {
        NamedProperty prop = NamedProperty.of("price");
        LessThan condition = prop.lt(100);

        Assertions.assertEquals("price", condition.getPropName());
        Assertions.assertEquals(100, (Integer) condition.getPropValue());
    }

    @Test
    public void testLe() {
        NamedProperty prop = NamedProperty.of("quantity");
        LessThanOrEqual condition = prop.le(10);

        Assertions.assertEquals("quantity", condition.getPropName());
        Assertions.assertEquals(10, (Integer) condition.getPropValue());
    }

    @Test
    public void testIsNull() {
        NamedProperty prop = NamedProperty.of("deletedDate");
        IsNull condition = prop.isNull();

        Assertions.assertEquals("deletedDate", condition.getPropName());
    }

    @Test
    public void testIsNotNull() {
        NamedProperty prop = NamedProperty.of("email");
        IsNotNull condition = prop.isNotNull();

        Assertions.assertEquals("email", condition.getPropName());
    }

    @Test
    public void testBetween() {
        NamedProperty prop = NamedProperty.of("age");
        Between condition = prop.between(18, 65);

        Assertions.assertEquals("age", condition.getPropName());
        Assertions.assertEquals(18, (Integer) (Integer) condition.getMinValue());
        Assertions.assertEquals(65, (Integer) condition.getMaxValue());
    }

    @Test
    public void testBt() {
        NamedProperty prop = NamedProperty.of("salary");
        Between condition = prop.bt(30000, 80000);

        Assertions.assertEquals("salary", condition.getPropName());
        Assertions.assertEquals(30000, (Integer) condition.getMinValue());
        Assertions.assertEquals(80000, (Integer) condition.getMaxValue());
    }

    @Test
    public void testLike() {
        NamedProperty prop = NamedProperty.of("name");
        Like condition = prop.like("John%");

        Assertions.assertEquals("name", condition.getPropName());
        Assertions.assertEquals("John%", condition.getPropValue());
    }

    @Test
    public void testNotLike() {
        NamedProperty prop = NamedProperty.of("email");
        NotLike condition = prop.notLike("%@temp.com");

        Assertions.assertEquals("email", condition.getPropName());
        Assertions.assertEquals("%@temp.com", condition.getPropValue());
    }

    @Test
    public void testStartsWith() {
        NamedProperty prop = NamedProperty.of("name");
        Like condition = prop.startsWith("John");

        Assertions.assertEquals("name", condition.getPropName());
        Assertions.assertEquals("John%", condition.getPropValue());
    }

    @Test
    public void testEndsWith() {
        NamedProperty prop = NamedProperty.of("email");
        Like condition = prop.endsWith("@example.com");

        Assertions.assertEquals("email", condition.getPropName());
        Assertions.assertEquals("%@example.com", condition.getPropValue());
    }

    @Test
    public void testContains() {
        NamedProperty prop = NamedProperty.of("description");
        Like condition = prop.contains("important");

        Assertions.assertEquals("description", condition.getPropName());
        Assertions.assertEquals("%important%", condition.getPropValue());
    }

    @Test
    public void testInWithArray() {
        NamedProperty prop = NamedProperty.of("status");
        In condition = prop.in("active", "pending", "approved");

        Assertions.assertEquals("status", condition.getPropName());
        Assertions.assertEquals(3, condition.getValues().size());
    }

    @Test
    public void testInWithCollection() {
        NamedProperty prop = NamedProperty.of("id");
        Set<Integer> ids = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        In condition = prop.in(ids);

        Assertions.assertEquals("id", condition.getPropName());
        Assertions.assertEquals(5, condition.getValues().size());
    }

    @Test
    public void testHashCode() {
        NamedProperty prop1 = new NamedProperty("property");
        NamedProperty prop2 = new NamedProperty("property");
        NamedProperty prop3 = new NamedProperty("different");

        Assertions.assertEquals(prop1.hashCode(), prop2.hashCode());
        Assertions.assertNotEquals(prop1.hashCode(), prop3.hashCode());
    }

    @Test
    public void testEquals() {
        NamedProperty prop1 = new NamedProperty("property");
        NamedProperty prop2 = new NamedProperty("property");
        NamedProperty prop3 = new NamedProperty("different");
        NamedProperty prop4 = NamedProperty.of("property");

        Assertions.assertTrue(prop1.equals(prop1));
        Assertions.assertTrue(prop1.equals(prop2));
        Assertions.assertFalse(prop1.equals(prop3));
        Assertions.assertTrue(prop1.equals(prop4));
        Assertions.assertFalse(prop1.equals(null));
        Assertions.assertFalse(prop1.equals("not a NamedProperty"));
    }

    @Test
    public void testToString() {
        NamedProperty prop = NamedProperty.of("testProperty");
        Assertions.assertEquals("testProperty", prop.toString());
    }

    @Test
    public void testChainedConditions() {
        NamedProperty age = NamedProperty.of("age");
        NamedProperty status = NamedProperty.of("status");

        // Create complex conditions using named properties
        Or complexCondition = age.eqAnyOf(25, 30, 35);
        In statusCondition = status.in(Arrays.asList("active", "pending"));

        Assertions.assertEquals(3, complexCondition.getConditions().size());
        Assertions.assertEquals(2, statusCondition.getValues().size());
    }
}
