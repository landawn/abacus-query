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
        assertEquals(Operator.EQUAL, Operator.getOperator("="));
        assertEquals(Operator.NOT_EQUAL, Operator.getOperator("!="));
        assertEquals(Operator.NOT_EQUAL2, Operator.getOperator("<>"));
        assertEquals(Operator.GREATER_THAN, Operator.getOperator(">"));
        assertEquals(Operator.GREATER_EQUAL, Operator.getOperator(">="));
        assertEquals(Operator.LESS_THAN, Operator.getOperator("<"));
        assertEquals(Operator.LESS_EQUAL, Operator.getOperator("<="));
    }

    @Test
    public void testGetOperator_Logical() {
        assertEquals(Operator.AND, Operator.getOperator("AND"));
        assertEquals(Operator.AND_OP, Operator.getOperator("&&"));
        assertEquals(Operator.OR, Operator.getOperator("OR"));
        assertEquals(Operator.OR_OP, Operator.getOperator("||"));
        assertEquals(Operator.NOT, Operator.getOperator("NOT"));
        assertEquals(Operator.NOT_OP, Operator.getOperator("!"));
        assertEquals(Operator.XOR, Operator.getOperator("XOR"));
    }

    @Test
    public void testGetOperator_Range() {
        assertEquals(Operator.BETWEEN, Operator.getOperator("BETWEEN"));
        assertEquals(Operator.NOT_BETWEEN, Operator.getOperator("NOT BETWEEN"));
        assertEquals(Operator.IN, Operator.getOperator("IN"));
        assertEquals(Operator.NOT_IN, Operator.getOperator("NOT IN"));
    }

    @Test
    public void testGetOperator_Pattern() {
        assertEquals(Operator.LIKE, Operator.getOperator("LIKE"));
        assertEquals(Operator.NOT_LIKE, Operator.getOperator("NOT LIKE"));
    }

    @Test
    public void testGetOperator_Null() {
        assertEquals(Operator.IS, Operator.getOperator("IS"));
        assertEquals(Operator.IS_NOT, Operator.getOperator("IS NOT"));
    }

    @Test
    public void testGetOperator_Subquery() {
        assertEquals(Operator.EXISTS, Operator.getOperator("EXISTS"));
        assertEquals(Operator.NOT_EXISTS, Operator.getOperator("NOT EXISTS"));
        assertEquals(Operator.ANY, Operator.getOperator("ANY"));
        assertEquals(Operator.SOME, Operator.getOperator("SOME"));
        assertEquals(Operator.ALL, Operator.getOperator("ALL"));
    }

    @Test
    public void testGetOperator_Join() {
        assertEquals(Operator.JOIN, Operator.getOperator("JOIN"));
        assertEquals(Operator.LEFT_JOIN, Operator.getOperator("LEFT JOIN"));
        assertEquals(Operator.RIGHT_JOIN, Operator.getOperator("RIGHT JOIN"));
        assertEquals(Operator.FULL_JOIN, Operator.getOperator("FULL JOIN"));
        assertEquals(Operator.CROSS_JOIN, Operator.getOperator("CROSS JOIN"));
        assertEquals(Operator.INNER_JOIN, Operator.getOperator("INNER JOIN"));
        assertEquals(Operator.NATURAL_JOIN, Operator.getOperator("NATURAL JOIN"));
        assertEquals(Operator.ON, Operator.getOperator("ON"));
        assertEquals(Operator.USING, Operator.getOperator("USING"));
    }

    @Test
    public void testGetOperator_Clause() {
        assertEquals(Operator.WHERE, Operator.getOperator("WHERE"));
        assertEquals(Operator.HAVING, Operator.getOperator("HAVING"));
        assertEquals(Operator.GROUP_BY, Operator.getOperator("GROUP BY"));
        assertEquals(Operator.ORDER_BY, Operator.getOperator("ORDER BY"));
        assertEquals(Operator.LIMIT, Operator.getOperator("LIMIT"));
        assertEquals(Operator.OFFSET, Operator.getOperator("OFFSET"));
    }

    @Test
    public void testGetOperator_SetOperations() {
        assertEquals(Operator.UNION, Operator.getOperator("UNION"));
        assertEquals(Operator.UNION_ALL, Operator.getOperator("UNION ALL"));
        assertEquals(Operator.INTERSECT, Operator.getOperator("INTERSECT"));
        assertEquals(Operator.EXCEPT, Operator.getOperator("EXCEPT"));
        assertEquals(Operator.MINUS, Operator.getOperator("MINUS"));
    }

    @Test
    public void testGetOperator_CaseInsensitive() {
        assertEquals(Operator.AND, Operator.getOperator("and"));
        assertEquals(Operator.OR, Operator.getOperator("or"));
        assertEquals(Operator.LIKE, Operator.getOperator("like"));
        assertEquals(Operator.BETWEEN, Operator.getOperator("between"));
        assertEquals(Operator.WHERE, Operator.getOperator("where"));
    }

    @Test
    public void testGetOperator_Invalid() {
        assertNull(Operator.getOperator("INVALID"));
        assertNull(Operator.getOperator("UNKNOWN"));
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
        assertEquals(Operator.EMPTY, Operator.getOperator(""));
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
}
