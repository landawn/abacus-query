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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class IsNull2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsNull condition = new IsNull("email");
        assertEquals("email", condition.getPropName());
        assertEquals(Operator.IS, condition.getOperator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNull(null));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new IsNull(""));
    }

    @Test
    public void testGetPropName() {
        IsNull condition = new IsNull("phone_number");
        assertEquals("phone_number", condition.getPropName());
    }

    @Test
    public void testToString_NoChange() {
        IsNull condition = new IsNull("email");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    public void testToString_SnakeCase() {
        IsNull condition = new IsNull("assignedTo");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("assigned_to"));
    }

    @Test
    public void testEquals() {
        IsNull c1 = new IsNull("email");
        IsNull c2 = new IsNull("email");
        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        IsNull c1 = new IsNull("email");
        IsNull c2 = new IsNull("phone");
        assertNotEquals(c1, c2);
    }

    @Test
    public void testHashCode() {
        IsNull c1 = new IsNull("email");
        IsNull c2 = new IsNull("email");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testCopy() {
        IsNull original = new IsNull("email");
        IsNull copy = original.copy();
        assertNotNull(copy);
        assertEquals(original.getPropName(), copy.getPropName());
    }

    @Test
    public void testUseCaseScenario_MissingEmail() {
        IsNull emailCheck = new IsNull("email");
        assertTrue(emailCheck.toString(NamingPolicy.NO_CHANGE).contains("IS NULL"));
    }

    @Test
    public void testUseCaseScenario_UnassignedTasks() {
        IsNull assigneeCheck = new IsNull("assigned_to");
        assertEquals("assigned_to", assigneeCheck.getPropName());
    }

    @Test
    public void testUseCaseScenario_OptionalFields() {
        IsNull middleNameCheck = new IsNull("middle_name");
        assertNotNull(middleNameCheck.toString(NamingPolicy.NO_CHANGE));
    }

    @Test
    public void testGetOperator() {
        IsNull condition = new IsNull("field");
        assertEquals(Operator.IS, condition.getOperator());
    }

    @Test
    public void testGetPropValue() {
        IsNull condition = new IsNull("field");
        Expression value = condition.getPropValue();
        assertNotNull(value);
        assertEquals(IsNull.NULL, value);
    }

    @Test
    public void testGetParameters() {
        IsNull condition = new IsNull("field");
        assertTrue(condition.getParameters().isEmpty() || condition.getParameters().size() == 1);
    }

    @Test
    public void testClearParameters() {
        IsNull condition = new IsNull("field");
        condition.clearParameters();
        assertNotNull(condition);
    }

    @Test
    public void testToString_NoArgs() {
        IsNull condition = new IsNull("email");
        String result = condition.toString();
        assertNotNull(result);
        assertTrue(result.contains("email"));
    }

    @Test
    public void testAnd() {
        IsNull cond1 = new IsNull("email");
        IsNull cond2 = new IsNull("phone");
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        IsNull cond1 = new IsNull("email");
        IsNull cond2 = new IsNull("phone");
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        IsNull condition = new IsNull("email");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
    }

    @Test
    public void testEquals_DifferentClass() {
        IsNull condition = new IsNull("field");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testEquals_Null() {
        IsNull condition = new IsNull("field");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_SameObject() {
        IsNull condition = new IsNull("field");
        assertEquals(condition, condition);
    }

    @Test
    public void testHashCode_DifferentPropName() {
        IsNull c1 = new IsNull("field1");
        IsNull c2 = new IsNull("field2");
        assertNotEquals(c1.hashCode(), c2.hashCode());
    }
}
