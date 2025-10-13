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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Using2025Test extends TestBase {

    @Test
    public void testConstructor_SingleColumn() {
        Using using = new Using("department_id");
        assertNotNull(using);
        assertEquals(Operator.USING, using.getOperator());
    }

    @Test
    public void testConstructor_MultipleColumns() {
        Using using = new Using("company_id", "branch_id");
        assertNotNull(using);
        assertEquals(Operator.USING, using.getOperator());
    }

    @Test
    public void testConstructor_Collection() {
        Set<String> columns = new HashSet<>(Arrays.asList("tenant_id", "workspace_id"));
        Using using = new Using(columns);
        assertNotNull(using);
        assertEquals(Operator.USING, using.getOperator());
    }

    @Test
    public void testConstructor_ThrowsOnNullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using((String[]) null);
        });
    }

    @Test
    public void testConstructor_ThrowsOnEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using(new String[0]);
        });
    }

    @Test
    public void testConstructor_ThrowsOnNullCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using((List<String>) null);
        });
    }

    @Test
    public void testConstructor_ThrowsOnEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Using(Arrays.asList());
        });
    }

    @Test
    public void testGetCondition() {
        Using using = new Using("employee_id");
        Condition condition = using.getCondition();
        assertNotNull(condition);
    }

    @Test
    public void testGetParameters() {
        Using using = new Using("customer_id");
        List<Object> params = using.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testClearParameters() {
        Using using = new Using("order_id");
        using.clearParameters();
        assertTrue(using.getParameters().isEmpty());
    }

    @Test
    public void testCopy() {
        Using original = new Using("department_id");
        Using copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testToString() {
        Using using = new Using("employee_id");
        String result = using.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("USING"));
    }

    @Test
    public void testHashCode() {
        Using using1 = new Using("id");
        Using using2 = new Using("id");
        assertEquals(using1.hashCode(), using2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Using using = new Using("id");
        assertEquals(using, using);
    }

    @Test
    public void testEquals_EqualObjects() {
        Using using1 = new Using("id");
        Using using2 = new Using("id");
        assertEquals(using1, using2);
    }

    @Test
    public void testEquals_Null() {
        Using using = new Using("id");
        assertNotEquals(null, using);
    }

    @Test
    public void testCreateUsingCondition_Array() {
        Condition condition = Using.createUsingCondition("customer_id", "order_date");
        assertNotNull(condition);
    }

    @Test
    public void testCreateUsingCondition_Collection() {
        Set<String> columns = new HashSet<>(Arrays.asList("company_id", "branch_id"));
        Condition condition = Using.createUsingCondition(columns);
        assertNotNull(condition);
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnNullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition((String[]) null);
        });
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition(new String[0]);
        });
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnNullCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition((List<String>) null);
        });
    }

    @Test
    public void testCreateUsingCondition_ThrowsOnEmptyCollection() {
        assertThrows(IllegalArgumentException.class, () -> {
            Using.createUsingCondition(Arrays.asList());
        });
    }

    @Test
    public void testCompositeKeyJoin() {
        Using using = new Using("company_id", "department_id", "team_id");
        assertNotNull(using);
        assertEquals(Operator.USING, using.getOperator());
    }

    @Test
    public void testDynamicColumnList() {
        List<String> sharedColumns = Arrays.asList("tenant_id", "organization_id");
        Using using = new Using(sharedColumns);
        assertNotNull(using);
    }
}
