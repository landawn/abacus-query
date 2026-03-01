/*
 * Copyright (C) 2025 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Operator2025Test extends TestBase {

    @Test
    public void testGetOperator_Comparison() {
        assertEquals(Operator.EQUAL, Operator.of("="));
        assertEquals(Operator.NOT_EQUAL, Operator.of("!="));
        assertEquals(Operator.NOT_EQUAL_ANSI, Operator.of("<>"));
        assertEquals(Operator.GREATER_THAN, Operator.of(">"));
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, Operator.of(">="));
        assertEquals(Operator.LESS_THAN, Operator.of("<"));
        assertEquals(Operator.LESS_THAN_OR_EQUAL, Operator.of("<="));
    }

    @Test
    public void testGetOperator_Logical() {
        assertEquals(Operator.AND, Operator.of("AND"));
        assertEquals(Operator.AND_OP, Operator.of("&&"));
        assertEquals(Operator.OR, Operator.of("OR"));
        assertEquals(Operator.OR_OP, Operator.of("||"));
        assertEquals(Operator.NOT, Operator.of("NOT"));
        assertEquals(Operator.NOT_OP, Operator.of("!"));
        assertEquals(Operator.XOR, Operator.of("XOR"));
    }

    @Test
    public void testGetOperator_Range() {
        assertEquals(Operator.BETWEEN, Operator.of("BETWEEN"));
        assertEquals(Operator.NOT_BETWEEN, Operator.of("NOT BETWEEN"));
        assertEquals(Operator.IN, Operator.of("IN"));
        assertEquals(Operator.NOT_IN, Operator.of("NOT IN"));
    }

    @Test
    public void testGetOperator_Pattern() {
        assertEquals(Operator.LIKE, Operator.of("LIKE"));
        assertEquals(Operator.NOT_LIKE, Operator.of("NOT LIKE"));
    }

    @Test
    public void testGetOperator_Null() {
        assertEquals(Operator.IS, Operator.of("IS"));
        assertEquals(Operator.IS_NOT, Operator.of("IS NOT"));
    }

    @Test
    public void testGetOperator_Subquery() {
        assertEquals(Operator.EXISTS, Operator.of("EXISTS"));
        assertEquals(Operator.NOT_EXISTS, Operator.of("NOT EXISTS"));
        assertEquals(Operator.ANY, Operator.of("ANY"));
        assertEquals(Operator.SOME, Operator.of("SOME"));
        assertEquals(Operator.ALL, Operator.of("ALL"));
    }

    @Test
    public void testGetOperator_Join() {
        assertEquals(Operator.JOIN, Operator.of("JOIN"));
        assertEquals(Operator.LEFT_JOIN, Operator.of("LEFT JOIN"));
        assertEquals(Operator.RIGHT_JOIN, Operator.of("RIGHT JOIN"));
        assertEquals(Operator.FULL_JOIN, Operator.of("FULL JOIN"));
        assertEquals(Operator.CROSS_JOIN, Operator.of("CROSS JOIN"));
        assertEquals(Operator.INNER_JOIN, Operator.of("INNER JOIN"));
        assertEquals(Operator.NATURAL_JOIN, Operator.of("NATURAL JOIN"));
        assertEquals(Operator.ON, Operator.of("ON"));
        assertEquals(Operator.USING, Operator.of("USING"));
    }

    @Test
    public void testGetOperator_Clause() {
        assertEquals(Operator.WHERE, Operator.of("WHERE"));
        assertEquals(Operator.HAVING, Operator.of("HAVING"));
        assertEquals(Operator.GROUP_BY, Operator.of("GROUP BY"));
        assertEquals(Operator.ORDER_BY, Operator.of("ORDER BY"));
        assertEquals(Operator.LIMIT, Operator.of("LIMIT"));
        assertEquals(Operator.OFFSET, Operator.of("OFFSET"));
    }

    @Test
    public void testGetOperator_SetOperations() {
        assertEquals(Operator.UNION, Operator.of("UNION"));
        assertEquals(Operator.UNION_ALL, Operator.of("UNION ALL"));
        assertEquals(Operator.INTERSECT, Operator.of("INTERSECT"));
        assertEquals(Operator.EXCEPT, Operator.of("EXCEPT"));
        assertEquals(Operator.MINUS, Operator.of("MINUS"));
    }

    @Test
    public void testGetOperator_CaseInsensitive() {
        assertEquals(Operator.AND, Operator.of("and"));
        assertEquals(Operator.OR, Operator.of("or"));
        assertEquals(Operator.LIKE, Operator.of("like"));
        assertEquals(Operator.BETWEEN, Operator.of("between"));
        assertEquals(Operator.WHERE, Operator.of("where"));
    }

    @Test
    public void testGetOperator_Invalid() {
        assertNull(Operator.of("INVALID"));
        assertNull(Operator.of("UNKNOWN"));
    }

    @Test
    public void testGetName() {
        assertEquals("=", Operator.EQUAL.getName());
        assertEquals("!=", Operator.NOT_EQUAL.getName());
        assertEquals("AND", Operator.AND.getName());
        assertEquals("OR", Operator.OR.getName());
        assertEquals("LIKE", Operator.LIKE.getName());
        assertEquals("BETWEEN", Operator.BETWEEN.getName());
        assertEquals("WHERE", Operator.WHERE.getName());
        assertEquals("LEFT JOIN", Operator.LEFT_JOIN.getName());
    }

    @Test
    public void testToString() {
        assertEquals("=", Operator.EQUAL.toString());
        assertEquals("!=", Operator.NOT_EQUAL.toString());
        assertEquals("AND", Operator.AND.toString());
        assertEquals("OR", Operator.OR.toString());
        assertEquals("LIKE", Operator.LIKE.toString());
        assertEquals("BETWEEN", Operator.BETWEEN.toString());
        assertEquals("WHERE", Operator.WHERE.toString());
        assertEquals("LEFT JOIN", Operator.LEFT_JOIN.toString());
    }

    @Test
    public void testValues() {
        Operator[] values = Operator.values();
        assertNotNull(values);
        assertTrue(values.length > 40);
    }

    @Test
    public void testValueOf() {
        assertEquals(Operator.EQUAL, Operator.valueOf("EQUAL"));
        assertEquals(Operator.NOT_EQUAL, Operator.valueOf("NOT_EQUAL"));
        assertEquals(Operator.AND, Operator.valueOf("AND"));
        assertEquals(Operator.OR, Operator.valueOf("OR"));
        assertEquals(Operator.WHERE, Operator.valueOf("WHERE"));
    }

    @Test
    public void testGetOperator_Empty() {
        assertEquals(Operator.EMPTY, Operator.of(""));
    }

    @Test
    public void testOrdinal() {
        assertNotNull(Operator.EQUAL.ordinal());
        assertNotNull(Operator.AND.ordinal());
    }

    @Test
    public void testName() {
        assertEquals("EQUAL", Operator.EQUAL.name());
        assertEquals("NOT_EQUAL", Operator.NOT_EQUAL.name());
        assertEquals("AND", Operator.AND.name());
        assertEquals("OR", Operator.OR.name());
    }

    @Test
    public void testGetOperator_NullInput() {
        assertNull(Operator.of(null));
    }

    @Test
    public void testGetOperator_Caching() {
        // First call initializes the cache
        Operator first = Operator.of("AND");
        // Second call should use cache
        Operator second = Operator.of("AND");
        assertEquals(first, second);
        assertEquals(Operator.AND, first);
    }

    @Test
    public void testGetOperator_ForUpdate() {
        assertEquals(Operator.FOR_UPDATE, Operator.of("FOR UPDATE"));
    }

    @Test
    public void testGetOperator_Offset() {
        assertEquals(Operator.OFFSET, Operator.of("OFFSET"));
    }

    @Test
    public void testAllOperators() {
        // Test all comparison operators
        assertNotNull(Operator.EQUAL);
        assertNotNull(Operator.NOT_EQUAL);
        assertNotNull(Operator.NOT_EQUAL_ANSI);
        assertNotNull(Operator.GREATER_THAN);
        assertNotNull(Operator.GREATER_THAN_OR_EQUAL);
        assertNotNull(Operator.LESS_THAN);
        assertNotNull(Operator.LESS_THAN_OR_EQUAL);

        // Test all logical operators
        assertNotNull(Operator.AND);
        assertNotNull(Operator.AND_OP);
        assertNotNull(Operator.OR);
        assertNotNull(Operator.OR_OP);
        assertNotNull(Operator.NOT);
        assertNotNull(Operator.NOT_OP);
        assertNotNull(Operator.XOR);

        // Test all join types
        assertNotNull(Operator.JOIN);
        assertNotNull(Operator.LEFT_JOIN);
        assertNotNull(Operator.RIGHT_JOIN);
        assertNotNull(Operator.FULL_JOIN);
        assertNotNull(Operator.CROSS_JOIN);
        assertNotNull(Operator.INNER_JOIN);
        assertNotNull(Operator.NATURAL_JOIN);

        // Test clauses
        assertNotNull(Operator.WHERE);
        assertNotNull(Operator.HAVING);
        assertNotNull(Operator.GROUP_BY);
        assertNotNull(Operator.ORDER_BY);
        assertNotNull(Operator.LIMIT);
        assertNotNull(Operator.OFFSET);

        // Test set operations
        assertNotNull(Operator.UNION);
        assertNotNull(Operator.UNION_ALL);
        assertNotNull(Operator.INTERSECT);
        assertNotNull(Operator.EXCEPT);
        assertNotNull(Operator.MINUS);

        // Test others
        assertNotNull(Operator.BETWEEN);
        assertNotNull(Operator.NOT_BETWEEN);
        assertNotNull(Operator.IN);
        assertNotNull(Operator.NOT_IN);
        assertNotNull(Operator.LIKE);
        assertNotNull(Operator.NOT_LIKE);
        assertNotNull(Operator.IS);
        assertNotNull(Operator.IS_NOT);
        assertNotNull(Operator.EXISTS);
        assertNotNull(Operator.NOT_EXISTS);
        assertNotNull(Operator.ANY);
        assertNotNull(Operator.SOME);
        assertNotNull(Operator.ALL);
        assertNotNull(Operator.ON);
        assertNotNull(Operator.USING);
        assertNotNull(Operator.FOR_UPDATE);
        assertNotNull(Operator.EMPTY);
    }
}
