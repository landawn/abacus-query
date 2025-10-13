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
public class IsNotNull2025Test extends TestBase {

    @Test
    public void testConstructor() {
        IsNotNull condition = new IsNotNull("email");
        assertEquals("email", condition.getPropName());
        assertEquals(Operator.IS_NOT, condition.getOperator());
    }

    @Test
    public void testGetPropName() {
        IsNotNull condition = new IsNotNull("customer_name");
        assertEquals("customer_name", condition.getPropName());
    }

    @Test
    public void testToString_NoChange() {
        IsNotNull condition = new IsNotNull("email");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("email"));
        assertTrue(result.contains("IS NOT"));
        assertTrue(result.contains("NULL"));
    }

    @Test
    public void testToString_LowerCaseWithUnderscore() {
        IsNotNull condition = new IsNotNull("phoneNumber");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("phone_number"));
    }

    @Test
    public void testEquals() {
        IsNotNull c1 = new IsNotNull("email");
        IsNotNull c2 = new IsNotNull("email");
        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        IsNotNull c1 = new IsNotNull("email");
        IsNotNull c2 = new IsNotNull("phone");
        assertNotEquals(c1, c2);
    }

    @Test
    public void testHashCode() {
        IsNotNull c1 = new IsNotNull("email");
        IsNotNull c2 = new IsNotNull("email");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testCopy() {
        IsNotNull original = new IsNotNull("email");
        IsNotNull copy = original.copy();
        assertNotNull(copy);
        assertEquals(original.getPropName(), copy.getPropName());
    }

    @Test
    public void testUseCaseScenario_RequiredEmail() {
        IsNotNull emailCheck = new IsNotNull("email");
        assertTrue(emailCheck.toString(NamingPolicy.NO_CHANGE).contains("IS NOT NULL"));
    }

    @Test
    public void testUseCaseScenario_ValidatedFields() {
        IsNotNull nameCheck = new IsNotNull("customer_name");
        assertEquals("customer_name", nameCheck.getPropName());
    }
}
