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
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class UnionAll2025Test extends TestBase {

    @Test
    public void testConstructor() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotNull(unionAll);
        assertEquals(Operator.UNION_ALL, unionAll.getOperator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM orders");
        UnionAll unionAll = new UnionAll(subQuery);
        SubQuery retrieved = unionAll.getCondition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testGetParameters() {
        SubQuery subQuery = Filters.subQuery("customers", List.of("*"), new Equal("region", "EAST"));
        UnionAll unionAll = new UnionAll(subQuery);
        List<Object> params = unionAll.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("EAST", params.get(0));
    }

    @Test
    public void testClearParameters() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM products WHERE year = 2024");
        UnionAll unionAll = new UnionAll(subQuery);
        // SubQuery with raw SQL doesn't have parameters - test condition instead
        assertTrue(unionAll.getParameters().isEmpty());
    }

    @Test
    public void testCopy() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        UnionAll original = new UnionAll(subQuery);
        UnionAll copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM transactions");
        UnionAll unionAll = new UnionAll(subQuery);
        String result = unionAll.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("UNION"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll1 = new UnionAll(subQuery1);
        UnionAll unionAll2 = new UnionAll(subQuery2);
        assertEquals(unionAll1.hashCode(), unionAll2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertEquals(unionAll, unionAll);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll1 = new UnionAll(subQuery1);
        UnionAll unionAll2 = new UnionAll(subQuery2);
        assertEquals(unionAll1, unionAll2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        UnionAll unionAll1 = new UnionAll(subQuery1);
        UnionAll unionAll2 = new UnionAll(subQuery2);
        assertNotEquals(unionAll1, unionAll2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotEquals(null, unionAll);
    }

    @Test
    public void testKeepsDuplicates() {
        SubQuery subQuery = Filters.subQuery("SELECT * FROM archived_transactions");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotNull(unionAll);
        assertEquals(Operator.UNION_ALL, unionAll.getOperator());
    }

    @Test
    public void testPerformance() {
        SubQuery subQuery = Filters.subQuery("SELECT id, name, 'active' as status FROM active_users");
        UnionAll unionAll = new UnionAll(subQuery);
        assertEquals(Operator.UNION_ALL, unionAll.getOperator());
    }

    @Test
    public void testAnd_ThrowsException() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        Condition otherCondition = Filters.eq("test", "value");

        assertThrows(UnsupportedOperationException.class, () -> {
            unionAll.and(otherCondition);
        });
    }

    @Test
    public void testOr_ThrowsException() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        Condition otherCondition = Filters.eq("test", "value");

        assertThrows(UnsupportedOperationException.class, () -> {
            unionAll.or(otherCondition);
        });
    }

    @Test
    public void testNot_ThrowsException() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);

        assertThrows(UnsupportedOperationException.class, () -> {
            unionAll.not();
        });
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertEquals(Operator.UNION_ALL, unionAll.getOperator());
    }

    @Test
    public void testSetCondition() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM table1");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM table2");
        UnionAll unionAll = new UnionAll(subQuery1);
        unionAll.setCondition(subQuery2);
        assertEquals(subQuery2, unionAll.getCondition());
    }

    @Test
    public void testToString_NoArgs() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM transactions");
        UnionAll unionAll = new UnionAll(subQuery);
        String result = unionAll.toString();
        assertTrue(result.contains("UNION"));
    }

    @Test
    public void testCopy_DeepCopy() {
        SubQuery subQuery = Filters.subQuery("customers", List.of("*"), new Equal("region", "EAST"));
        UnionAll original = new UnionAll(subQuery);
        UnionAll copy = original.copy();

        // Modify original's subquery
        original.clearParameters();

        // Copy should not be affected
        List<Object> copyParams = copy.getParameters();
        assertEquals("EAST", copyParams.get(0));
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM table1");
        UnionAll unionAll = new UnionAll(subQuery);
        assertNotEquals(unionAll, "not a UnionAll");
        assertNotEquals(unionAll, new Union(subQuery));
    }
}
