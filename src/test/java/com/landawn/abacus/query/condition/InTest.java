package com.landawn.abacus.query.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

public class InTest extends TestBase {

    @Test
    public void testConstructorWithList() {
        List<String> values = Arrays.asList("active", "pending", "approved");
        In condition = new In("status", values);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("status", condition.getPropName());
        Assertions.assertEquals(Operator.IN, condition.getOperator());
        Assertions.assertEquals(3, condition.getValues().size());
        Assertions.assertTrue(condition.getValues().containsAll(values));
    }

    @Test
    public void testConstructorWithSet() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2, 3, 5, 8));
        In condition = new In("user_id", values);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals("user_id", condition.getPropName());
        Assertions.assertEquals(5, condition.getValues().size());
        Assertions.assertTrue(condition.getValues().containsAll(values));
    }

    @Test
    public void testConstructorWithMixedTypes() {
        List<Object> values = Arrays.asList(1, "two", 3.0, true);
        In condition = new In("mixed_field", values);

        Assertions.assertNotNull(condition);
        Assertions.assertEquals(4, condition.getValues().size());
    }

    @Test
    public void testGetPropName() {
        In condition = new In("category", Arrays.asList("A", "B", "C"));
        Assertions.assertEquals("category", condition.getPropName());
    }

    @Test
    public void testGetValues() {
        List<String> values = Arrays.asList("red", "green", "blue");
        In condition = new In("color", values);

        List<?> retrievedValues = condition.getValues();
        Assertions.assertNotNull(retrievedValues);
        Assertions.assertEquals(3, retrievedValues.size());
        Assertions.assertTrue(retrievedValues.containsAll(values));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetValues() {
        In condition = new In("type", Arrays.asList("old1", "old2"));
        List<String> newValues = Arrays.asList("new1", "new2", "new3");

        condition.setValues(newValues);

        Assertions.assertEquals(3, condition.getValues().size());
        Assertions.assertTrue(condition.getValues().containsAll(newValues));
    }

    @Test
    public void testGetParameters() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        In condition = new In("value", values);

        List<Object> params = condition.getParameters();

        Assertions.assertNotNull(params);
        Assertions.assertEquals(3, params.size());
        Assertions.assertEquals(values, params);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testClearParameters() {
        In condition = new In("id", Arrays.asList(1, 2, 3));
        condition.clearParameters();

        List values = condition.getValues();
        Assertions.assertNotNull(values);
        for (Object value : values) {
            Assertions.assertNull(value);
        }
    }

    @Test
    public void testCopy() {
        In original = new In("status", Arrays.asList("A", "B", "C"));
        In copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        Assertions.assertNotSame(original.getValues(), copy.getValues());
        Assertions.assertEquals(original.getValues(), copy.getValues());
    }

    @Test
    public void testToString() {
        In condition = new In("category", Arrays.asList("electronics", "computers", "phones"));
        String result = condition.toString();

        Assertions.assertTrue(result.contains("category"));
        Assertions.assertTrue(result.contains("IN"));
        Assertions.assertTrue(result.contains("("));
        Assertions.assertTrue(result.contains(")"));
        Assertions.assertTrue(result.contains("electronics"));
        Assertions.assertTrue(result.contains("computers"));
        Assertions.assertTrue(result.contains("phones"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        In condition = new In("productId", Arrays.asList(101, 102, 103));
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);

        Assertions.assertTrue(result.contains("PRODUCT_ID"));
        Assertions.assertTrue(result.contains("IN"));
        Assertions.assertTrue(result.contains("101"));
    }

    @Test
    public void testHashCode() {
        In condition1 = new In("field", Arrays.asList(1, 2, 3));
        In condition2 = new In("field", Arrays.asList(1, 2, 3));
        In condition3 = new In("other", Arrays.asList(1, 2, 3));
        In condition4 = new In("field", Arrays.asList(4, 5, 6));

        Assertions.assertEquals(condition1.hashCode(), condition2.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition3.hashCode());
        Assertions.assertNotEquals(condition1.hashCode(), condition4.hashCode());
    }

    @Test
    public void testEquals() {
        In condition1 = new In("field", Arrays.asList(1, 2, 3));
        In condition2 = new In("field", Arrays.asList(1, 2, 3));
        In condition3 = new In("other", Arrays.asList(1, 2, 3));
        In condition4 = new In("field", Arrays.asList(4, 5, 6));

        Assertions.assertEquals(condition1, condition1);
        Assertions.assertEquals(condition1, condition2);
        Assertions.assertNotEquals(condition1, condition3);
        Assertions.assertNotEquals(condition1, condition4);
        Assertions.assertNotEquals(condition1, null);
        Assertions.assertNotEquals(condition1, "string");
    }

    @Test
    public void testConstructorWithEmptyCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new In("field", new ArrayList<>());
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSetValuesWithEmpty() {
        In condition = new In("field", Arrays.asList("value"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            condition.setValues(new ArrayList<>());
        });
    }

    @Test
    public void testLargeValuesList() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add(i);
        }

        In condition = new In("id", values);

        Assertions.assertEquals(1000, condition.getValues().size());
        Assertions.assertEquals(1000, condition.getParameters().size());
    }

    @Test
    public void testSingleValue() {
        In condition = new In("type", Collections.singletonList("SINGLE"));

        Assertions.assertEquals(1, condition.getValues().size());
        Assertions.assertEquals("SINGLE", condition.getValues().get(0));

        String result = condition.toString();
        Assertions.assertTrue(result.contains("type IN ('SINGLE')"));
    }

    @Test
    public void testNullValues() {
        List<String> values = Arrays.asList("A", null, "B");
        In condition = new In("field", values);

        Assertions.assertEquals(3, condition.getValues().size());
        Assertions.assertTrue(condition.getValues().contains(null));
    }
}