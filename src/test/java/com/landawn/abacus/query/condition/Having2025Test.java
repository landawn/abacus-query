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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.ConditionFactory.CF;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for {@link Having}.
 * Tests all public methods including construction, toString, equals, hashCode, and inherited methods.
 */
@Tag("2025")
public class Having2025Test extends TestBase {

    @Test
    public void testConstructorWithCondition() {
        Equal condition = CF.eq("COUNT(*)", 5);
        Having having = new Having(condition);

        assertNotNull(having);
        assertEquals(Operator.HAVING, having.getOperator());
    }

    @Test
    public void testToString() {
        Having having = new Having(CF.gt("COUNT(*)", 10));
        String result = having.toString();

        assertTrue(result.contains("HAVING"));
        assertTrue(result.contains("COUNT(*)"));
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Having having = new Having(CF.gt("COUNT(*)", 5));
        String result = having.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        assertNotNull(result);
        assertTrue(result.contains("HAVING"));
    }

    @Test
    public void testGetParameters() {
        Having having = new Having(CF.gt("SUM(amount)", 1000));

        assertNotNull(having.getParameters());
        assertEquals(1, having.getParameters().size());
        assertEquals(1000, having.getParameters().get(0));
    }

    @Test
    public void testGetParametersWithMultipleConditions() {
        Condition condition = CF.and(
            CF.gt("COUNT(*)", 5),
            CF.lt("AVG(price)", 100)
        );
        Having having = new Having(condition);

        assertEquals(2, having.getParameters().size());
    }

    @Test
    public void testClearParameters() {
        Having having = new Having(CF.gt("COUNT(*)", 10));
        assertEquals(1, having.getParameters().size());

        having.clearParameters();
        // Parameters should still exist but values should be cleared
        assertNotNull(having.getParameters());
    }

    @Test
    public void testEquals() {
        Having having1 = new Having(CF.gt("COUNT(*)", 5));
        Having having2 = new Having(CF.gt("COUNT(*)", 5));
        Having having3 = new Having(CF.gt("COUNT(*)", 10));

        assertEquals(having1, having2);
        assertNotEquals(having1, having3);
        assertEquals(having1, having1);
    }

    @Test
    public void testEqualsWithNull() {
        Having having = new Having(CF.gt("COUNT(*)", 5));

        assertNotEquals(having, null);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Having having = new Having(CF.gt("COUNT(*)", 5));
        Where where = new Where(CF.gt("COUNT(*)", 5));

        assertNotEquals(having, (Object) where);
    }

    @Test
    public void testHashCode() {
        Having having1 = new Having(CF.gt("COUNT(*)", 5));
        Having having2 = new Having(CF.gt("COUNT(*)", 5));

        assertEquals(having1.hashCode(), having2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        Having having = new Having(CF.gt("SUM(total)", 1000));
        int hash1 = having.hashCode();
        int hash2 = having.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testCopy() {
        Having original = new Having(CF.gt("AVG(score)", 75));
        Having copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testWithAggregateFunction() {
        Having having = new Having(CF.gt("MAX(salary)", 100000));
        String result = having.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        assertTrue(result.contains("HAVING"));
        assertTrue(result.contains("max") || result.contains("MAX"));
    }

    @Test
    public void testWithMinFunction() {
        Having having = new Having(CF.lt("MIN(age)", 18));

        assertNotNull(having);
        assertEquals(1, having.getParameters().size());
        assertEquals(18, having.getParameters().get(0));
    }

    @Test
    public void testWithComplexCondition() {
        Condition complex = CF.and(
            CF.gt("COUNT(*)", 10),
            CF.between("AVG(salary)", 50000, 100000)
        );
        Having having = new Having(complex);

        assertNotNull(having);
        assertEquals(3, having.getParameters().size());
    }

    @Test
    public void testWithOrCondition() {
        Condition orCondition = CF.or(
            CF.gt("SUM(revenue)", 1000000),
            CF.eq("COUNT(*)", 0)
        );
        Having having = new Having(orCondition);

        String result = having.toString();
        assertTrue(result.contains("OR"));
    }

    @Test
    public void testWithExpression() {
        Having having = new Having(CF.expr("COUNT(DISTINCT customer_id) > 100"));

        assertNotNull(having);
        String result = having.toString();
        assertTrue(result.contains("HAVING"));
    }

    @Test
    public void testInheritedGetConditionMethod() {
        Equal condition = CF.eq("COUNT(*)", 5);
        Having having = new Having(condition);

        Condition retrievedCondition = having.getCondition();
        assertEquals(condition, retrievedCondition);
    }

    @Test
    public void testOperatorType() {
        Having having = new Having(CF.gt("COUNT(*)", 0));

        assertEquals(Operator.HAVING, having.getOperator());
        assertNotEquals(Operator.WHERE, having.getOperator());
        assertNotEquals(Operator.GROUP_BY, having.getOperator());
    }

    @Test
    public void testWithBetweenCondition() {
        Having having = new Having(CF.between("AVG(age)", 25, 40));

        assertEquals(2, having.getParameters().size());
        assertEquals(25, having.getParameters().get(0));
        assertEquals(40, having.getParameters().get(1));
    }

    @Test
    public void testMultipleAggregatesWithAnd() {
        Condition condition = CF.and(
            CF.gt("COUNT(*)", 5),
            CF.gt("SUM(amount)", 10000),
            CF.lt("AVG(price)", 500)
        );
        Having having = new Having(condition);

        assertEquals(3, having.getParameters().size());
        String result = having.toString();
        assertTrue(result.contains("COUNT"));
        assertTrue(result.contains("SUM"));
        assertTrue(result.contains("AVG"));
    }

    @Test
    public void testWithNullSafeCondition() {
        Having having = new Having(CF.isNotNull("COUNT(*)"));

        assertNotNull(having);
        assertEquals(0, having.getParameters().size());
    }

    @Test
    public void testStringRepresentationFormat() {
        Having having = new Having(CF.ge("COUNT(*)", 1));
        String result = having.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains(">="));
    }
}
