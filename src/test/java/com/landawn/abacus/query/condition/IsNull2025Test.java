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
    public void testToString_LowerCaseWithUnderscore() {
        IsNull condition = new IsNull("assignedTo");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
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
}
