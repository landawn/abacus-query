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
public class And2025Test extends TestBase {

    @Test
    public void testConstructor_VarArgs() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And junction = new And(cond1, cond2);

        assertEquals(2, (int) junction.getConditions().size());
        assertEquals(Operator.AND, junction.getOperator());
    }

    @Test
    public void testConstructor_Collection() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        List<Condition> conditions = Arrays.asList(cond1, cond2);

        And junction = new And(conditions);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testConstructor_SingleCondition() {
        Equal cond = new Equal("status", "active");
        And junction = new And(cond);
        assertEquals((Object) Integer.valueOf(1), junction.getConditions().size());
    }

    @Test
    public void testConstructor_EmptyConditions() {
        And junction = new And();
        assertEquals(Integer.valueOf(0), junction.getConditions().size());
    }

    @Test
    public void testGetConditions() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And junction = new And(cond1, cond2);

        List<Condition> conditions = junction.getConditions();
        assertNotNull(conditions);
        assertEquals(2, (int) conditions.size());
    }

    @Test
    public void testSet_VarArgs() {
        And junction = new And();
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);

        junction.set(cond1, cond2);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testSet_Collection() {
        And junction = new And();
        List<Condition> conditions = Arrays.asList(new Equal("a", 1), new Equal("b", 2));

        junction.set(conditions);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testSet_ReplacesExisting() {
        And junction = new And(new Equal("old", 1));
        junction.set(new Equal("new", 2));

        assertEquals((Object) Integer.valueOf(1), junction.getConditions().size());
        Equal condition = (Equal) junction.getConditions().get(0);
        assertEquals("new", condition.getPropName());
    }

    @Test
    public void testAdd_VarArgs() {
        And junction = new And();
        junction.add(new Equal("a", 1), new Equal("b", 2));
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testAdd_Collection() {
        And junction = new And();
        List<Condition> conditions = Arrays.asList(new Equal("a", 1), new Equal("b", 2));
        junction.add(conditions);
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testAdd_Incremental() {
        And junction = new And(new Equal("a", 1));
        junction.add(new Equal("b", 2));
        assertEquals(2, (int) junction.getConditions().size());
    }

    @Test
    public void testClear() {
        And junction = new And(new Equal("a", 1), new Equal("b", 2));
        junction.clear();
        assertEquals(Integer.valueOf(0), junction.getConditions().size());
    }

    @Test
    public void testGetParameters() {
        And junction = new And(new Equal("a", 1), new Equal("b", "test"));
        List<Object> params = junction.getParameters();
        assertEquals(2, (int) params.size());
        assertEquals((Object) Integer.valueOf(1), params.get(0));
        assertEquals("test", params.get(1));
    }

    @Test
    public void testGetParameters_EmptyConditions() {
        And junction = new And();
        List<Object> params = junction.getParameters();
        assertEquals(0, (int) params.size());
    }

    @Test
    public void testClearParameters() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And junction = new And(cond1, cond2);

        junction.clearParameters();
        assertNull(cond1.getPropValue());
        assertNull(cond2.getPropValue());
    }

    @Test
    public void testCopy() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        And original = new And(cond1, cond2);

        And copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getConditions().size(), copy.getConditions().size());
    }

    @Test
    public void testCopy_DeepCopy() {
        Equal cond = new Equal("a", 1);
        And original = new And(cond);
        And copy = original.copy();

        // Modify original condition
        cond.clearParameters();

        // Copy should not be affected
        Equal copiedCond = (Equal) copy.getConditions().get(0);
        assertEquals(1, (int) copiedCond.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        And junction = new And(new Equal("a", 1), new Equal("b", 2));
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("AND"));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    public void testToString_EmptyConditions() {
        And junction = new And();
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertEquals("", result);
    }

    @Test
    public void testToString_SingleCondition() {
        And junction = new And(new Equal("status", "active"));
        String result = junction.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("status"));
        assertFalse(result.contains("AND"));
    }

    @Test
    public void testHashCode() {
        And j1 = new And(new Equal("a", 1));
        And j2 = new And(new Equal("a", 1));
        assertEquals(j1.hashCode(), j2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        And junction = new And(new Equal("a", 1));
        assertEquals(junction, junction);
    }

    @Test
    public void testEquals_EqualObjects() {
        And j1 = new And(new Equal("a", 1));
        And j2 = new And(new Equal("a", 1));
        assertEquals(j1, j2);
    }

    @Test
    public void testEquals_DifferentConditions() {
        And j1 = new And(new Equal("a", 1));
        And j2 = new And(new Equal("b", 2));
        assertNotEquals(j1, j2);
    }

    @Test
    public void testEquals_Null() {
        And junction = new And(new Equal("a", 1));
        assertNotEquals(null, junction);
    }

    @Test
    public void testAndMethod() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);

        And original = new And(cond1, cond2);
        And extended = original.and(cond3);

        assertEquals(Integer.valueOf(3), extended.getConditions().size());
        assertEquals(2, (int) original.getConditions().size());
    }

    @Test
    public void testAndMethod_NullCondition() {
        And junction = new And(new Equal("a", 1));
        assertThrows(IllegalArgumentException.class, () -> junction.and(null));
    }

    @Test
    public void testNestedJunctions() {
        And inner = new And(new Equal("a", 1), new Equal("b", 2));
        And outer = new And(inner, new Equal("c", 3));

        assertEquals(2, (int) outer.getConditions().size());
        List<Object> params = outer.getParameters();
        assertEquals(3, (int) params.size());
    }

    @Test
    public void testOrMethod() {
        And and = new And(new Equal("a", 1));
        Equal cond = new Equal("b", 2);
        Or result = and.or(cond);

        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
        assertEquals(Operator.OR, result.getOperator());
    }

    @Test
    public void testNotMethod() {
        And and = new And(new Equal("a", 1), new Equal("b", 2));
        Not result = and.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
        And innerCondition = result.getCondition();
        assertEquals(2, (int) innerCondition.getConditions().size());
    }

    @Test
    public void testToString_NoArgs() {
        And and = new And(new Equal("status", "active"), new Equal("verified", true));
        String result = and.toString();

        assertTrue(result.contains("AND"));
        assertTrue(result.contains("status"));
        assertTrue(result.contains("verified"));
    }

    @Test
    public void testConstructor_NullConditionInArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            new And(new Equal("a", 1), null, new Equal("b", 2));
        });
    }

    @Test
    public void testAdd_NullConditionInArray() {
        And and = new And();
        assertThrows(IllegalArgumentException.class, () -> {
            and.add(new Equal("a", 1), null);
        });
    }

    @Test
    public void testSet_NullConditionInArray() {
        And and = new And(new Equal("old", 1));
        assertThrows(IllegalArgumentException.class, () -> {
            and.set(new Equal("a", 1), null);
        });
    }

    @Test
    public void testGetOperator() {
        And and = new And(new Equal("a", 1));
        assertEquals(Operator.AND, and.getOperator());
    }

    @Test
    public void testGetOperator_EmptyConstructor() {
        // Empty constructor doesn't set operator (for Kryo serialization)
        And and = new And();
        assertNull(and.getOperator());
    }

    @Test
    public void testConstructor_NullConditionInCollection() {
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(new Equal("a", 1));
        conditions.add(null);
        assertThrows(IllegalArgumentException.class, () -> {
            new And(conditions);
        });
    }

    @Test
    public void testAdd_NullConditionInCollection() {
        And and = new And();
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(new Equal("a", 1));
        conditions.add(null);
        assertThrows(IllegalArgumentException.class, () -> {
            and.add(conditions);
        });
    }

    @Test
    public void testAdd_NullCollection() {
        And and = new And();
        and.add((java.util.Collection<Condition>) null);
        assertEquals(0, (int) and.getConditions().size());
    }

    @Test
    public void testSet_NullConditionInCollection() {
        And and = new And(new Equal("old", 1));
        List<Condition> conditions = new java.util.ArrayList<>();
        conditions.add(null);
        assertThrows(IllegalArgumentException.class, () -> {
            and.set(conditions);
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRemove_VarArgs() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);
        And and = new And(cond1, cond2, cond3);

        and.remove(cond1, cond3);
        assertEquals(1, (int) and.getConditions().size());
        assertEquals(cond2, and.getConditions().get(0));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRemove_Collection() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);
        And and = new And(cond1, cond2, cond3);

        List<Condition> toRemove = Arrays.asList(cond1, cond2);
        and.remove(toRemove);
        assertEquals(1, (int) and.getConditions().size());
        assertEquals(cond3, and.getConditions().get(0));
    }
}
