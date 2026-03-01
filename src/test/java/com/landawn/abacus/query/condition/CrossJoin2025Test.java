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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class CrossJoin2025Test extends TestBase {

    @Test
    public void testConstructor_Simple() {
        CrossJoin join = new CrossJoin("colors");
        assertNotNull(join);
        assertEquals(Operator.CROSS_JOIN, join.operator());
    }

    @Test
    public void testConstructor_WithCondition() {
        CrossJoin join = new CrossJoin("products", new Equal("available", true));
        assertNotNull(join);
        assertNotNull(join.getCondition());
        assertEquals(Operator.CROSS_JOIN, join.operator());
    }

    @Test
    public void testConstructor_MultipleEntities() {
        List<String> entities = Arrays.asList("sizes", "colors", "styles");
        CrossJoin join = new CrossJoin(entities, new Equal("active", true));
        assertNotNull(join);
        assertEquals(3, (int) join.getJoinEntities().size());
        assertEquals(Operator.CROSS_JOIN, join.operator());
    }

    @Test
    public void testGetJoinEntities() {
        List<String> entities = Arrays.asList("table1", "table2");
        CrossJoin join = new CrossJoin(entities, null);
        List<String> result = join.getJoinEntities();
        assertEquals(2, (int) result.size());
        assertTrue(result.contains("table1"));
        assertTrue(result.contains("table2"));
    }

    @Test
    public void testGetCondition() {
        Equal condition = new Equal("active", true);
        CrossJoin join = new CrossJoin("products", condition);
        Condition retrieved = join.getCondition();
        assertEquals(condition, retrieved);
    }

    @Test
    public void testGetCondition_Null() {
        CrossJoin join = new CrossJoin("colors");
        assertNull(join.getCondition());
    }

    @Test
    public void testGetParameters_Empty() {
        CrossJoin join = new CrossJoin("sizes");
        assertTrue(join.getParameters().isEmpty());
    }

    @Test
    public void testGetParameters_WithCondition() {
        CrossJoin join = new CrossJoin("products", new Equal("active", true));
        List<Object> params = join.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals(true, params.get(0));
    }

    @Test
    public void testClearParameters() {
        CrossJoin join = new CrossJoin("products", new Equal("status", "available"));
        assertFalse(join.getParameters().isEmpty());
        join.clearParameters();
        List<Object> params = join.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        CrossJoin original = new CrossJoin("products", new Equal("category_id", (Object) 222));
        CrossJoin copy = (CrossJoin) original.copy();
        assertNotSame(original, copy);
        assertEquals(original.getJoinEntities(), copy.getJoinEntities());
        assertNotSame(original.getJoinEntities(), copy.getJoinEntities());
    }

    @Test
    public void testToString_Simple() {
        CrossJoin join = new CrossJoin("colors");
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("CROSS JOIN"));
        assertTrue(result.contains("colors"));
    }

    @Test
    public void testToString_WithCondition() {
        CrossJoin join = new CrossJoin("products", new Equal("active", true));
        String result = join.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("CROSS JOIN"));
        assertTrue(result.contains("products"));
    }

    @Test
    public void testHashCode() {
        CrossJoin join1 = new CrossJoin("colors", new Equal("a", "b"));
        CrossJoin join2 = new CrossJoin("colors", new Equal("a", "b"));
        assertEquals(join1.hashCode(), join2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        CrossJoin join = new CrossJoin("colors");
        assertEquals(join, join);
    }

    @Test
    public void testEquals_EqualObjects() {
        CrossJoin join1 = new CrossJoin("colors", new Equal("a", "b"));
        CrossJoin join2 = new CrossJoin("colors", new Equal("a", "b"));
        assertEquals(join1, join2);
    }

    @Test
    public void testEquals_DifferentEntities() {
        CrossJoin join1 = new CrossJoin("colors");
        CrossJoin join2 = new CrossJoin("sizes");
        assertNotEquals(join1, join2);
    }

    @Test
    public void testEquals_Null() {
        CrossJoin join = new CrossJoin("colors");
        assertNotEquals(null, join);
    }

    @Test
    public void testAnd() {
        CrossJoin join1 = new CrossJoin("colors");
        CrossJoin join2 = new CrossJoin("sizes");
        And result = join1.and(join2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        CrossJoin join1 = new CrossJoin("colors");
        CrossJoin join2 = new CrossJoin("sizes");
        Or result = join1.or(join2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        CrossJoin join = new CrossJoin("colors");
        Not result = join.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testCartesianProduct() {
        CrossJoin join = new CrossJoin("colors");
        assertNotNull(join);
        assertEquals(Operator.CROSS_JOIN, join.operator());
        assertNull(join.getCondition());
    }

    @Test
    public void testAllCombinations() {
        List<String> tables = Arrays.asList("test_users", "test_permissions");
        CrossJoin join = new CrossJoin(tables, null);
        assertEquals(2, (int) join.getJoinEntities().size());
    }

    @Test
    public void testGenerateTestData() {
        CrossJoin join = new CrossJoin("available_times");
        assertNotNull(join);
        assertNull(join.getCondition());
    }
}
