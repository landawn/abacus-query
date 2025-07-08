package com.landawn.abacus.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

public class AbstractConditionTest extends TestBase {

    // Create a concrete implementation for testing
    private static class TestCondition extends AbstractCondition {
        private String value;
        
        public TestCondition(Operator operator, String value) {
            super(operator);
            this.value = value;
        }
        
        @Override
        public List<Object> getParameters() {
            return value == null ? N.emptyList() : Arrays.asList(value);
        }
        
        @Override
        public void clearParameters() {
            value = null;
        }
        
        @Override
        public String toString(NamingPolicy namingPolicy) {
            return getOperator().toString() + " " + value;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TestCondition)) return false;
            TestCondition other = (TestCondition) obj;
            return Objects.equals(operator, other.operator) && Objects.equals(value, other.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(operator, value);
        }
    }

    @Test
    public void testConstructor() {
        TestCondition condition = new TestCondition(Operator.EQUAL, "test");
        
        Assertions.assertNotNull(condition);
        Assertions.assertEquals(Operator.EQUAL, condition.getOperator());
        Assertions.assertEquals("test", condition.value);
    }

    @Test
    public void testGetOperator() {
        TestCondition condition = new TestCondition(Operator.NOT_EQUAL, "value");
        Assertions.assertEquals(Operator.NOT_EQUAL, condition.getOperator());
    }

    @Test
    public void testAnd() {
        TestCondition cond1 = new TestCondition(Operator.EQUAL, "test1");
        TestCondition cond2 = new TestCondition(Operator.NOT_EQUAL, "test2");
        
        And and = cond1.and(cond2);
        
        Assertions.assertNotNull(and);
        Assertions.assertEquals(Operator.AND, and.getOperator());
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(cond1));
        Assertions.assertTrue(and.getConditions().contains(cond2));
    }

    @Test
    public void testOr() {
        TestCondition cond1 = new TestCondition(Operator.GREATER_THAN, "10");
        TestCondition cond2 = new TestCondition(Operator.LESS_THAN, "5");
        
        Or or = cond1.or(cond2);
        
        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.getOperator());
        Assertions.assertEquals(2, or.getConditions().size());
        Assertions.assertTrue(or.getConditions().contains(cond1));
        Assertions.assertTrue(or.getConditions().contains(cond2));
    }

    @Test
    public void testNot() {
        TestCondition condition = new TestCondition(Operator.LIKE, "%test%");
        
        Not not = condition.not();
        
        Assertions.assertNotNull(not);
        Assertions.assertEquals(Operator.NOT, not.getOperator());
        Assertions.assertEquals(condition, not.getCondition());
    }

    @Test
    public void testCopy() {
        TestCondition original = new TestCondition(Operator.BETWEEN, "range");
        
        TestCondition copy = original.copy();
        
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
        // Note: The value field is not copied by the base class copy method
    }

    @Test
    public void testToString() {
        TestCondition condition = new TestCondition(Operator.IN, "list");
        
        String result = condition.toString();
        Assertions.assertEquals("IN list", result);
    }

    @Test
    public void testParameter2StringWithString() {
        String result = AbstractCondition.parameter2String("test", NamingPolicy.NO_CHANGE);
        Assertions.assertEquals("'test'", result);
    }

    @Test
    public void testParameter2StringWithNumber() {
        String result = AbstractCondition.parameter2String(123, NamingPolicy.NO_CHANGE);
        Assertions.assertEquals("123", result);
    }

    @Test
    public void testParameter2StringWithNull() {
        String result = AbstractCondition.parameter2String(null, NamingPolicy.NO_CHANGE);
        Assertions.assertNull(result);
    }

    @Test
    public void testParameter2StringWithCondition() {
        Equal eq = ConditionFactory.eq("name", "John");
        String result = AbstractCondition.parameter2String(eq, NamingPolicy.NO_CHANGE);
        Assertions.assertEquals("name = 'John'", result);
    }

    @Test
    public void testParameter2StringWithConditionAndNamingPolicy() {
        Equal eq = ConditionFactory.eq("firstName", "John");
        String result = AbstractCondition.parameter2String(eq, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertEquals("first_name = 'John'", result);
    }

    @Test
    public void testConcatPropNamesArray() {
        // Test empty array
        String result = AbstractCondition.concatPropNames();
        Assertions.assertEquals("", result);
        
        // Test single element
        result = AbstractCondition.concatPropNames("name");
        Assertions.assertEquals("name", result);
        
        // Test two elements
        result = AbstractCondition.concatPropNames("city", "state");
        Assertions.assertEquals("(city, state)", result);
        
        // Test three elements
        result = AbstractCondition.concatPropNames("a", "b", "c");
        Assertions.assertEquals("(a, b, c)", result);
        
        // Test more than three elements
        result = AbstractCondition.concatPropNames("col1", "col2", "col3", "col4", "col5");
        Assertions.assertEquals("(col1, col2, col3, col4, col5)", result);
    }

    @Test
    public void testConcatPropNamesCollection() {
        // Test empty collection
        List<String> empty = new ArrayList<>();
        String result = AbstractCondition.concatPropNames(empty);
        Assertions.assertEquals("", result);
        
        // Test single element
        List<String> single = Arrays.asList("name");
        result = AbstractCondition.concatPropNames(single);
        Assertions.assertEquals("name", result);
        
        // Test two elements
        List<String> two = Arrays.asList("city", "state");
        result = AbstractCondition.concatPropNames(two);
        Assertions.assertEquals("(city, state)", result);
        
        // Test three elements
        List<String> three = Arrays.asList("a", "b", "c");
        result = AbstractCondition.concatPropNames(three);
        Assertions.assertEquals("(a, b, c)", result);
        
        // Test more than three elements
        List<String> many = Arrays.asList("col1", "col2", "col3", "col4", "col5");
        result = AbstractCondition.concatPropNames(many);
        Assertions.assertEquals("(col1, col2, col3, col4, col5)", result);
    }

    @Test
    public void testConcatPropNamesWithSet() {
        // Test with LinkedHashSet to maintain order
        Set<String> props = new LinkedHashSet<>();
        props.add("first");
        props.add("second");
        props.add("third");
        
        String result = AbstractCondition.concatPropNames(props);
        Assertions.assertEquals("(first, second, third)", result);
    }

    @Test
    public void testComplexConditionChaining() {
        TestCondition cond1 = new TestCondition(Operator.EQUAL, "val1");
        TestCondition cond2 = new TestCondition(Operator.NOT_EQUAL, "val2");
        TestCondition cond3 = new TestCondition(Operator.GREATER_THAN, "val3");
        TestCondition cond4 = new TestCondition(Operator.LESS_THAN, "val4");
        
        // Test complex chaining: (cond1 AND cond2) OR (cond3 AND cond4)
        And and1 = cond1.and(cond2);
        And and2 = cond3.and(cond4);
        Or complex = and1.or(and2);
        
        Assertions.assertNotNull(complex);
        Assertions.assertEquals(2, complex.getConditions().size());
        
        // Test NOT of complex condition
        Not notComplex = complex.not();
        Assertions.assertNotNull(notComplex);
        Assertions.assertEquals(complex, notComplex.getCondition());
    }

    @Test
    public void testCloneableSupport() {
        // Verify that copy() works through Cloneable
        TestCondition original = new TestCondition(Operator.LIKE, "pattern");
        TestCondition copy = original.copy();
        
        // The base implementation uses clone()
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testNullOperatorHandling() {
        // Test condition with null operator (through default constructor)
        AbstractCondition condition = new AbstractCondition() {
            @Override
            public List<Object> getParameters() {
                return N.emptyList();
            }
            
            @Override
            public void clearParameters() {
            }
            
            @Override
            public String toString(NamingPolicy namingPolicy) {
                return "NULL_OP";
            }
        };
        
        Assertions.assertNull(condition.getOperator());
    }
}