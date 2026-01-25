/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class IsNotNaN2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsNotNaN condition = new IsNotNaN("temperature");
        assertEquals("temperature", condition.getPropName());
        assertEquals(Operator.IS_NOT, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNaN(null));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNotNaN(""));
    }

    @Test
    public void testGetPropName() {
        IsNotNaN condition = new IsNotNaN("sensorValue");
        assertEquals("sensorValue", condition.getPropName());
    }

    @Test
    public void testGetPropName_DifferentNames() {
        IsNotNaN cond1 = new IsNotNaN("value1");
        IsNotNaN cond2 = new IsNotNaN("value2");
        assertNotEquals(cond1.getPropName(), cond2.getPropName());
    }

    @Test
    public void testGetPropValue() {
        IsNotNaN condition = new IsNotNaN("calculatedValue");
        Expression value = condition.getPropValue();
        assertNotNull(value);
    }

    @Test
    public void testGetOperator() {
        IsNotNaN condition = new IsNotNaN("field");
        assertEquals(Operator.IS_NOT, condition.getOperator());
    }

    @Test
    public void testGetParameters() {
        IsNotNaN condition = new IsNotNaN("temperature");
        List<Object> params = condition.getParameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testClearParameters() {
        IsNotNaN condition = new IsNotNaN("value");
        condition.clearParameters();
        // Should not throw exception
        assertNotNull(condition);
    }

    @Test
    public void testCopy() {
        IsNotNaN original = new IsNotNaN("temperature");
        IsNotNaN copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString_NoChange() {
        IsNotNaN condition = new IsNotNaN("temperature");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("temperature"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NAN"));
    }

    @Test
    public void testToString_SnakeCase() {
        IsNotNaN condition = new IsNotNaN("sensorValue");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("sensor_value"));
    }

    @Test
    public void testToString_ScreamingSnakeCase() {
        IsNotNaN condition = new IsNotNaN("fieldName");
        String result = condition.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertTrue(result.contains("FIELD_NAME"));
    }

    @Test
    public void testHashCode() {
        IsNotNaN cond1 = new IsNotNaN("value");
        IsNotNaN cond2 = new IsNotNaN("value");
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        IsNotNaN cond1 = new IsNotNaN("value1");
        IsNotNaN cond2 = new IsNotNaN("value2");
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        IsNotNaN condition = new IsNotNaN("field");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        IsNotNaN cond1 = new IsNotNaN("temperature");
        IsNotNaN cond2 = new IsNotNaN("temperature");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        IsNotNaN cond1 = new IsNotNaN("field1");
        IsNotNaN cond2 = new IsNotNaN("field2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        IsNotNaN condition = new IsNotNaN("field");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        IsNotNaN condition = new IsNotNaN("field");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        IsNotNaN cond1 = new IsNotNaN("temp1");
        IsNotNaN cond2 = new IsNotNaN("temp2");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        IsNotNaN cond1 = new IsNotNaN("value1");
        IsNotNaN cond2 = new IsNotNaN("value2");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        IsNotNaN condition = new IsNotNaN("field");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    public void testInheritance() {
        IsNotNaN condition = new IsNotNaN("field");
        assertTrue(condition instanceof IsNot);
        assertTrue(condition instanceof Binary);
        assertTrue(condition instanceof AbstractCondition);
        assertTrue(condition instanceof Condition);
    }

    @Test
    public void testDoublePropertyName() {
        IsNotNaN condition = new IsNotNaN("calculationResult");
        assertEquals("calculationResult", condition.getPropName());
    }

    @Test
    public void testComplexPropertyName() {
        IsNotNaN condition = new IsNotNaN("user.profile.score");
        assertEquals("user.profile.score", condition.getPropName());
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("user.profile.score"));
    }

    @Test
    public void testCopyIndependence() {
        IsNotNaN original = new IsNotNaN("value");
        IsNotNaN copy = original.copy();

        copy.clearParameters();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
    }

    @Test
    public void testMultipleInstances() {
        IsNotNaN cond1 = new IsNotNaN("field1");
        IsNotNaN cond2 = new IsNotNaN("field2");
        IsNotNaN cond3 = new IsNotNaN("field3");

        assertNotEquals(cond1, cond2);
        assertNotEquals(cond2, cond3);
        assertNotEquals(cond1, cond3);
    }
}
