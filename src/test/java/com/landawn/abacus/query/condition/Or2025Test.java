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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class Or2025Test extends TestBase {

    @Test
    public void testConstructor_VarArgs() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or junction = new Or(cond1, cond2);

        assertEquals(2, (int) junction.getConditions().size());
        assertEquals(Operator.OR, junction.getOperator());
    }

    @Test
    public void testConstructor_Collection() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        List<Condition> conditions = Arrays.asList(cond1, cond2);

        Or junction = new Or(conditions);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testConstructor_SingleCondition() {
        Equal cond = new Equal("status", "active");
        Or junction = new Or(cond);
        assertEquals((Object) Integer.valueOf(1), junction.getConditions().size());
    }

    @Test
    public void testConstructor_EmptyConditions() {
        Or junction = new Or();
        assertEquals(Integer.valueOf(0), junction.getConditions().size());
    }

    @Test
    public void testGetConditions() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or junction = new Or(cond1, cond2);

        List<Condition> conditions = junction.getConditions();
        assertNotNull(conditions);
        assertEquals(2, (int) conditions.size());
    }

    @Test
    public void testSet_VarArgs() {
        Or junction = new Or();
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);

        junction.set(cond1, cond2);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testSet_Collection() {
        Or junction = new Or();
        List<Condition> conditions = Arrays.asList(new Equal("a", 1), new Equal("b", 2));

        junction.set(conditions);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testSet_ReplacesExisting() {
        Or junction = new Or(new Equal("old", 1));
        junction.set(new Equal("new", 2));

        assertEquals((Object) Integer.valueOf(1), junction.getConditions().size());
        Equal condition = (Equal) junction.getConditions().get(0);
        assertEquals("new", condition.getPropName());
    }

    @Test
    public void testAdd_VarArgs() {
        Or junction = new Or();
        junction.add(new Equal("a", 1), new Equal("b", 2));
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testAdd_Collection() {
        Or junction = new Or();
        List<Condition> conditions = Arrays.asList(new Equal("a", 1), new Equal("b", 2));
        junction.add(conditions);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testAdd_Incremental() {
        Or junction = new Or(new Equal("a", 1));
        junction.add(new Equal("b", 2));
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testClear() {
        Or junction = new Or(new Equal("a", 1), new Equal("b", 2));
        junction.clear();
        assertEquals(Integer.valueOf(0), junction.getConditions().size());
    }

    @Test
    public void testGetParameters() {
        Or junction = new Or(new Equal("a", 1), new Equal("b", "test"));
        List<Object> params = junction.getParameters();
        assertEquals(2, (int) params.size());
        assertEquals((Object) Integer.valueOf(1), params.get(0));
        assertEquals("test", params.get(1));
    }

    @Test
    public void testGetParameters_EmptyConditions() {
        Or junction = new Or();
        List<Object> params = junction.getParameters();
        assertEquals(0, (int) params.size());
    }

    @Test
    public void testClearParameters() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or junction = new Or(cond1, cond2);

        junction.clearParameters();
        assertNull(cond1.getPropValue());
        assertNull(cond2.getPropValue());
    }

    @Test
    public void testCopy() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Or original = new Or(cond1, cond2);

        Or copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getConditions().size(), copy.getConditions().size());
    }

    @Test
    public void testCopy_DeepCopy() {
        Equal cond = new Equal("a", 1);
        Or original = new Or(cond);
        Or copy = original.copy();

        // Modify original condition
        cond.clearParameters();

        // Copy should not be affected
        Equal copiedCond = (Equal) copy.getConditions().get(0);
        assertEquals(1, (int) copiedCond.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        Or junction = new Or(new Equal("a", 1), new Equal("b", 2));
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("OR"));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    public void testToString_EmptyConditions() {
        Or junction = new Or();
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertEquals("", result);
    }

    @Test
    public void testToString_SingleCondition() {
        Or junction = new Or(new Equal("status", "active"));
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("status"));
        assertFalse(result.contains("OR"));
    }

    @Test
    public void testHashCode() {
        Or j1 = new Or(new Equal("a", 1));
        Or j2 = new Or(new Equal("a", 1));
        assertEquals(j1.hashCode(), j2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Or junction = new Or(new Equal("a", 1));
        assertEquals(junction, junction);
    }

    @Test
    public void testEquals_EqualObjects() {
        Or j1 = new Or(new Equal("a", 1));
        Or j2 = new Or(new Equal("a", 1));
        assertEquals(j1, j2);
    }

    @Test
    public void testEquals_DifferentConditions() {
        Or j1 = new Or(new Equal("a", 1));
        Or j2 = new Or(new Equal("b", 2));
        assertNotEquals(j1, j2);
    }

    @Test
    public void testEquals_Null() {
        Or junction = new Or(new Equal("a", 1));
        assertNotEquals(null, junction);
    }

    @Test
    public void testOrMethod() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);

        Or original = new Or(cond1, cond2);
        Or extended = original.or(cond3);

        assertEquals(Integer.valueOf(3), extended.getConditions().size());
        assertEquals(2, (int) original.getConditions().size());
    }

    @Test
    public void testOrMethod_NullCondition() {
        Or junction = new Or(new Equal("a", 1));
        assertThrows(IllegalArgumentException.class, () -> junction.or(null));
    }

    @Test
    public void testNestedJunctions() {
        Or inner = new Or(new Equal("a", 1), new Equal("b", 2));
        Or outer = new Or(inner, new Equal("c", 3));

        assertEquals(2, (int) outer.getConditions().size());
        List<Object> params = outer.getParameters();
        assertEquals(3, (int) params.size());
    }

    @Test
    public void testAndMethod() {
        Or or = new Or(new Equal("a", 1));
        Equal cond = new Equal("b", 2);
        And result = or.and(cond);

        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
        assertEquals(Operator.AND, result.getOperator());
    }

    @Test
    public void testNotMethod() {
        Or or = new Or(new Equal("a", 1), new Equal("b", 2));
        Not result = or.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
        Or innerCondition = result.getCondition();
        assertEquals(2, (int) innerCondition.getConditions().size());
    }

    @Test
    public void testToString_NoArgs() {
        Or or = new Or(new Equal("status", "active"), new Equal("status", "pending"));
        String result = or.toString();

        assertTrue(result.contains("OR"));
        assertTrue(result.contains("status"));
    }

    @Test
    public void testConstructor_NullConditionInArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Or(new Equal("a", 1), null, new Equal("b", 2));
        });
    }

    @Test
    public void testAdd_NullConditionInArray() {
        Or or = new Or();
        assertThrows(IllegalArgumentException.class, () -> {
            or.add(new Equal("a", 1), null);
        });
    }

    @Test
    public void testSet_NullConditionInArray() {
        Or or = new Or(new Equal("old", 1));
        assertThrows(IllegalArgumentException.class, () -> {
            or.set(new Equal("a", 1), null);
        });
    }

    @Test
    public void testGetOperator() {
        Or or = new Or(new Equal("a", 1));
        assertEquals(Operator.OR, or.getOperator());
    }

    @Test
    public void testGetOperator_EmptyConstructor() {
        // Empty constructor doesn't set operator (for Kryo serialization)
        Or or = new Or();
        assertNull(or.getOperator());
    }

    @Test
    public void testConstructor_NullConditionInCollection() {
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(new Equal("a", 1));
        conditions.add(null);
        assertThrows(IllegalArgumentException.class, () -> {
            new Or(conditions);
        });
    }

    @Test
    public void testAdd_NullConditionInCollection() {
        Or or = new Or();
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(new Equal("a", 1));
        conditions.add(null);
        assertThrows(IllegalArgumentException.class, () -> {
            or.add(conditions);
        });
    }

    @Test
    public void testAdd_NullCollection() {
        Or or = new Or();
        or.add((java.util.Collection<Condition>) null);
        assertEquals(0, (int) or.getConditions().size());
    }

    @Test
    public void testSet_NullConditionInCollection() {
        Or or = new Or(new Equal("old", 1));
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(null);
        assertThrows(IllegalArgumentException.class, () -> {
            or.set(conditions);
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRemove_VarArgs() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);
        Or or = new Or(cond1, cond2, cond3);

        or.remove(cond1, cond3);
        assertEquals(1, (int) or.getConditions().size());
        assertEquals(cond2, or.getConditions().get(0));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRemove_Collection() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);
        Or or = new Or(cond1, cond2, cond3);

        List<Condition> toRemove = Arrays.asList(cond1, cond2);
        or.remove(toRemove);
        assertEquals(1, (int) or.getConditions().size());
        assertEquals(cond3, or.getConditions().get(0));
    }
}
