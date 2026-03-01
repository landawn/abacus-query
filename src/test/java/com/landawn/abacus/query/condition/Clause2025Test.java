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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

@Tag("2025")
public class Clause2025Test extends TestBase {

    private static class TestClause extends Clause {
        public TestClause(Operator operator, Condition condition) {
            super(operator, condition);
        }
    }

    @Test
    public void testConstructor() {
        Condition condition = Filters.eq("test", "value");
        TestClause clause = new TestClause(Operator.WHERE, condition);
        assertNotNull(clause);
        assertNotNull(clause.operator());
        assertNotNull(clause.getCondition());
    }

    @Test
    public void testGetCondition() {
        Condition condition = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.WHERE, condition);

        Condition retrieved = clause.getCondition();

        assertEquals(condition, retrieved);
    }

    @Test
    public void testSetCondition() {
        Condition initial = Filters.eq("old", "value");
        TestClause clause = new TestClause(Operator.HAVING, initial);

        Condition newCondition = Filters.gt("count", 5);
        clause.setCondition(newCondition);

        assertEquals(newCondition, clause.getCondition());
    }

    @Test
    public void testGetParameters() {
        Condition condition = Filters.between("age", 18, 65);
        TestClause clause = new TestClause(Operator.WHERE, condition);

        java.util.List<Object> params = clause.getParameters();

        assertNotNull(params);
        assertEquals(2, params.size());
        assertEquals(18, params.get(0));
        assertEquals(65, params.get(1));
    }

    @Test
    public void testClearParameters() {
        Condition condition = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.WHERE, condition);

        assertFalse(clause.getParameters().isEmpty());

        clause.clearParameters();

        java.util.List<Object> params = clause.getParameters();
        assertTrue(params.size() == 1 && params.stream().allMatch(param -> param == null));
    }

    @Test
    public void testCopy() {
        Condition condition = Filters.eq("field", "value");
        TestClause original = new TestClause(Operator.WHERE, condition);

        TestClause copy = original.copy();

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertEquals(original.operator(), copy.operator());
        assertNotSame(original.getCondition(), copy.getCondition());
    }

    @Test
    public void testToString_WithNamingPolicy() {
        Condition condition = Filters.eq("userName", "John");
        TestClause clause = new TestClause(Operator.WHERE, condition);

        String result = clause.toString(com.landawn.abacus.util.NamingPolicy.SNAKE_CASE);

        assertNotNull(result);
        assertTrue(result.contains("WHERE"));
    }

    @Test
    public void testToString_DefaultNamingPolicy() {
        Condition condition = Filters.eq("status", "active");
        TestClause clause = new TestClause(Operator.HAVING, condition);

        String result = clause.toString();

        assertNotNull(result);
        assertTrue(result.contains("HAVING"));
    }

    @Test
    public void testHashCode() {
        Condition condition = Filters.eq("test", "value");
        TestClause clause1 = new TestClause(Operator.WHERE, condition);
        TestClause clause2 = new TestClause(Operator.WHERE, Filters.eq("test", "value"));

        assertEquals(clause1.hashCode(), clause2.hashCode());
    }

    @Test
    public void testEquals_SameInstance() {
        TestClause clause = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        assertEquals(clause, clause);
    }

    @Test
    public void testEquals_EqualClauses() {
        TestClause clause1 = new TestClause(Operator.WHERE, Filters.eq("status", "active"));
        TestClause clause2 = new TestClause(Operator.WHERE, Filters.eq("status", "active"));

        assertEquals(clause1, clause2);
    }

    @Test
    public void testEquals_DifferentOperator() {
        TestClause clause1 = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        TestClause clause2 = new TestClause(Operator.HAVING, Filters.eq("a", 1));

        assertNotEquals(clause1, clause2);
    }

    @Test
    public void testEquals_DifferentCondition() {
        TestClause clause1 = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        TestClause clause2 = new TestClause(Operator.WHERE, Filters.eq("b", 2));

        assertNotEquals(clause1, clause2);
    }

    @Test
    public void testEquals_Null() {
        TestClause clause = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        assertNotEquals(clause, null);
    }

    @Test
    public void testEquals_DifferentType() {
        TestClause clause = new TestClause(Operator.WHERE, Filters.eq("a", 1));
        assertNotEquals(clause, "not a clause");
    }

    @Test
    public void testGetOperator() {
        TestClause clause = new TestClause(Operator.GROUP_BY, Filters.eq("test", "value"));
        assertEquals(Operator.GROUP_BY, clause.operator());
    }

    @Test
    public void testWithComplexCondition() {
        Condition complex = Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.isNotNull("email"));
        TestClause clause = new TestClause(Operator.WHERE, complex);

        java.util.List<Object> params = clause.getParameters();
        assertEquals(2, params.size());
    }

    @Test
    public void testCopyPreservesValues() {
        TestClause original = new TestClause(Operator.WHERE, Filters.eq("test", "value"));

        TestClause copy = original.copy();
        copy.clearParameters();

        // Original should still have parameters
        assertFalse(original.getParameters().isEmpty());
    }
}
