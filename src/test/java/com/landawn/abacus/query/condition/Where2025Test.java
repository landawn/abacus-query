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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Where2025Test extends TestBase {

    @Test
    public void testConstructor_SimpleCondition() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertNotNull(where);
        assertEquals(Operator.WHERE, where.getOperator());
    }

    @Test
    public void testConstructor_ComplexCondition() {
        And and = new And(new Equal("age", (Object) 25), new GreaterThan("salary", (Object) 50000));
        Where where = new Where(and);
        assertNotNull(where);
        assertEquals(Operator.WHERE, where.getOperator());
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("name", "John");
        Where where = new Where(condition);
        Condition retrieved = where.getCondition();
        assertNotNull(retrieved);
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetParameters() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        List<Object> params = where.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_MultipleValues() {
        And complexCondition = new And(Arrays.asList(new Equal("status", "active"), new GreaterThan("balance", (Object) 1000)));
        Where where = new Where(complexCondition);
        List<Object> params = where.getParameters();
        assertEquals(2, (int) params.size());
    }

    @Test
    public void testClearParameters() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertFalse(where.getParameters().isEmpty());
        where.clearParameters();
        List<Object> params = where.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        Equal condition = new Equal("name", "Alice");
        Where original = new Where(condition);
        Where copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString_Simple() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        String result = where.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testToString_Complex() {
        Or complexCondition = new Or(new And(new Equal("status", "active"), new GreaterThan("balance", (Object) 1000)), new Equal("vip", true));
        Where where = new Where(complexCondition);
        String result = where.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testHashCode() {
        Equal condition1 = new Equal("status", "active");
        Equal condition2 = new Equal("status", "active");
        Where where1 = new Where(condition1);
        Where where2 = new Where(condition2);
        assertEquals(where1.hashCode(), where2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertEquals(where, where);
    }

    @Test
    public void testEquals_EqualObjects() {
        Equal condition1 = new Equal("status", "active");
        Equal condition2 = new Equal("status", "active");
        Where where1 = new Where(condition1);
        Where where2 = new Where(condition2);
        assertEquals(where1, where2);
    }

    @Test
    public void testEquals_DifferentConditions() {
        Equal condition1 = new Equal("status", "active");
        Equal condition2 = new Equal("status", "inactive");
        Where where1 = new Where(condition1);
        Where where2 = new Where(condition2);
        assertNotEquals(where1, where2);
    }

    @Test
    public void testEquals_Null() {
        Equal condition = new Equal("status", "active");
        Where where = new Where(condition);
        assertNotEquals(null, where);
    }

    @Test
    public void testWithLikeOperator() {
        Like condition = new Like("name", "%John%");
        Where where = new Where(condition);
        assertNotNull(where);
        assertEquals(1, (int) where.getParameters().size());
    }

    @Test
    public void testWithBetween() {
        Between condition = new Between("age", (Object) 18, (Object) 65);
        Where where = new Where(condition);
        assertEquals(2, (int) where.getParameters().size());
    }

    @Test
    public void testWithInOperator() {
        In condition = new In("status", Arrays.asList("active", "pending", "approved"));
        Where where = new Where(condition);
        assertEquals(3, (int) where.getParameters().size());
    }

    @Test
    public void testWithOrCondition() {
        Or orCondition = new Or(new Equal("type", "A"), new Equal("type", "B"));
        Where where = new Where(orCondition);
        assertEquals(2, (int) where.getParameters().size());
    }
}
