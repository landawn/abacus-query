package com.landawn.abacus.query.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

public class NotInTest extends TestBase {

    @Test
    public void testConstructorWithList() {
        List<String> values = Arrays.asList("deleted", "archived", "inactive");
        NotIn notIn = CF.notIn("status", values);
        
        Assertions.assertNotNull(notIn);
        Assertions.assertEquals("status", notIn.getPropName());
        Assertions.assertEquals(values, notIn.getValues());
        Assertions.assertEquals(Operator.NOT_IN, notIn.getOperator());
    }

    @Test
    public void testConstructorWithSet() {
        Set<Integer> values = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        NotIn notIn = CF.notIn("userId", values);
        
        Assertions.assertEquals("userId", notIn.getPropName());
        Assertions.assertEquals(5, notIn.getValues().size());
        Assertions.assertTrue(notIn.getValues().containsAll(values));
    }

    @Test
    public void testConstructorWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.notIn("status", emptyList);
        });
    }

    @Test
    public void testConstructorWithNullCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CF.notIn("status", (List<String>) null);
        });
    }

    @Test
    public void testSetValues() {
        List<String> originalValues = Arrays.asList("a", "b", "c");
        List<String> newValues = Arrays.asList("x", "y", "z");
        
        NotIn notIn = CF.notIn("type", originalValues);
        notIn.setValues(newValues);
        
        Assertions.assertEquals(newValues, notIn.getValues());
    }

    @Test
    public void testGetParameters() {
        List<Integer> values = Arrays.asList(10, 20, 30);
        NotIn notIn = CF.notIn("amount", values);
        
        List<Object> params = notIn.getParameters();
        Assertions.assertEquals(values, params);
    }

    @Test
    public void testGetParametersWithNull() {
        List<String> values = Arrays.asList("a", "b");
        NotIn notIn = CF.notIn("code", values);
        notIn.setValues(null);
        
        List<Object> params = notIn.getParameters();
        Assertions.assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        List<String> values = new ArrayList<>(Arrays.asList("a", "b", "c"));
        NotIn notIn = CF.notIn("type", values);
        
        notIn.clearParameters();
        
        // Values should be filled with null
        Assertions.assertTrue(notIn.getValues().stream().allMatch(v -> v == null));
    }

    @Test
    public void testCopy() {
        List<String> values = Arrays.asList("temp", "draft", "test");
        NotIn original = CF.notIn("documentType", values);
        
        NotIn copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertNotSame(original.getValues(), copy.getValues());
        Assertions.assertEquals(original.getValues(), copy.getValues());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString() {
        List<String> values = Arrays.asList("A", "B", "C");
        NotIn notIn = CF.notIn("category", values);
        
        String result = notIn.toString();
        Assertions.assertTrue(result.contains("category"));
        Assertions.assertTrue(result.contains("NOT IN"));
        Assertions.assertTrue(result.contains("category NOT IN ('A', 'B', 'C')"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        List<String> values = Arrays.asList("active", "pending");
        NotIn notIn = CF.notIn("user_status", values);
        
        String result = notIn.toString(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        Assertions.assertTrue(result.contains("USER_STATUS"));
        Assertions.assertTrue(result.contains("NOT IN"));
        Assertions.assertTrue(result.contains("USER_STATUS NOT IN ('active', 'pending')"));
    }

    @Test
    public void testHashCode() {
        List<String> values1 = Arrays.asList("a", "b", "c");
        List<String> values2 = Arrays.asList("a", "b", "c");
        List<String> values3 = Arrays.asList("x", "y", "z");
        
        NotIn notIn1 = CF.notIn("type", values1);
        NotIn notIn2 = CF.notIn("type", values2);
        NotIn notIn3 = CF.notIn("type", values3);
        NotIn notIn4 = CF.notIn("category", values1);
        
        Assertions.assertEquals(notIn1.hashCode(), notIn2.hashCode());
        Assertions.assertNotEquals(notIn1.hashCode(), notIn3.hashCode());
        Assertions.assertNotEquals(notIn1.hashCode(), notIn4.hashCode());
    }

    @Test
    public void testEquals() {
        List<String> values1 = Arrays.asList("a", "b", "c");
        List<String> values2 = Arrays.asList("a", "b", "c");
        List<String> values3 = Arrays.asList("x", "y", "z");
        
        NotIn notIn1 = CF.notIn("type", values1);
        NotIn notIn2 = CF.notIn("type", values2);
        NotIn notIn3 = CF.notIn("type", values3);
        NotIn notIn4 = CF.notIn("category", values1);
        
        Assertions.assertTrue(notIn1.equals(notIn1));
        Assertions.assertTrue(notIn1.equals(notIn2));
        Assertions.assertFalse(notIn1.equals(notIn3));
        Assertions.assertFalse(notIn1.equals(notIn4));
        Assertions.assertFalse(notIn1.equals(null));
        Assertions.assertFalse(notIn1.equals("not a NotIn"));
    }

    @Test
    public void testWithMixedTypes() {
        List<Object> values = Arrays.asList(1, "two", 3.0, true);
        NotIn notIn = CF.notIn("mixedField", values);
        
        Assertions.assertEquals(4, notIn.getValues().size());
        Assertions.assertTrue(notIn.getValues().contains(1));
        Assertions.assertTrue(notIn.getValues().contains("two"));
        Assertions.assertTrue(notIn.getValues().contains(3.0));
        Assertions.assertTrue(notIn.getValues().contains(true));
    }

    @Test
    public void testLargeValueSet() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add(i);
        }
        
        NotIn notIn = CF.notIn("id", values);
        
        Assertions.assertEquals(1000, notIn.getValues().size());
        Assertions.assertEquals(1000, notIn.getParameters().size());
    }
}