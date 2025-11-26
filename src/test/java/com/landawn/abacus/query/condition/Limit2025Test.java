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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Comprehensive test class for {@link Limit}.
 */
@Tag("2025")
public class Limit2025Test extends TestBase {

    @Test
    public void testConstructorWithCount() {
        Limit limit = new Limit(10);

        assertNotNull(limit);
        assertEquals(10, limit.getCount());
        assertEquals(0, limit.getOffset());
        assertNull(limit.getExpr());
    }

    @Test
    public void testConstructorWithOffsetAndCount() {
        Limit limit = new Limit(20, 10);

        assertNotNull(limit);
        assertEquals(10, limit.getCount());
        assertEquals(20, limit.getOffset());
        assertNull(limit.getExpr());
    }

    @Test
    public void testConstructorWithExpression() {
        Limit limit = new Limit("10 OFFSET 20");

        assertNotNull(limit);
        assertEquals("10 OFFSET 20", limit.getExpr());
        assertEquals(Integer.MAX_VALUE, limit.getCount());
        assertEquals(0, limit.getOffset());
    }

    @Test
    public void testGetCount() {
        Limit limit = new Limit(50);

        assertEquals(50, limit.getCount());
    }

    @Test
    public void testGetOffset() {
        Limit limit = new Limit(100, 25);

        assertEquals(100, limit.getOffset());
        assertEquals(25, limit.getCount());
    }

    @Test
    public void testGetExpr() {
        Limit limit = new Limit("FIRST 10 ROWS");

        assertEquals("FIRST 10 ROWS", limit.getExpr());
    }

    @Test
    public void testGetExprWithoutExpression() {
        Limit limit = new Limit(10);

        assertNull(limit.getExpr());
    }

    @Test
    public void testGetParameters() {
        Limit limit = new Limit(10);

        assertNotNull(limit.getParameters());
        assertTrue(limit.getParameters().isEmpty());
    }

    @Test
    public void testClearParameters() {
        Limit limit = new Limit(15);
        limit.clearParameters();

        // Should do nothing, but not throw exception
        assertNotNull(limit.getParameters());
        assertTrue(limit.getParameters().isEmpty());
    }

    @Test
    public void testAndThrowsException() {
        Limit limit = new Limit(10);

        assertThrows(UnsupportedOperationException.class, () -> {
            limit.and(new Equal("id", 1));
        });
    }

    @Test
    public void testOrThrowsException() {
        Limit limit = new Limit(10);

        assertThrows(UnsupportedOperationException.class, () -> {
            limit.or(new Equal("id", 1));
        });
    }

    @Test
    public void testNotThrowsException() {
        Limit limit = new Limit(10);

        assertThrows(UnsupportedOperationException.class, () -> {
            limit.not();
        });
    }

    @Test
    public void testToStringWithCountOnly() {
        Limit limit = new Limit(10);
        String result = limit.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("10"));
        assertFalse(result.contains("OFFSET"));
    }

    @Test
    public void testToStringWithOffsetAndCount() {
        Limit limit = new Limit(20, 10);
        String result = limit.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("LIMIT"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("OFFSET"));
        assertTrue(result.contains("20"));
    }

    @Test
    public void testToStringWithExpression() {
        Limit limit = new Limit("FIRST 5 ROWS");
        String result = limit.toString(NamingPolicy.NO_CHANGE);

        assertEquals("FIRST 5 ROWS", result);
    }

    @Test
    public void testEquals() {
        Limit limit1 = new Limit(10);
        Limit limit2 = new Limit(10);
        Limit limit3 = new Limit(20);

        assertEquals(limit1, limit2);
        assertNotEquals(limit1, limit3);
        assertEquals(limit1, limit1);
    }

    @Test
    public void testEqualsWithOffset() {
        Limit limit1 = new Limit(10, 5);
        Limit limit2 = new Limit(10, 5);
        Limit limit3 = new Limit(20, 5);

        assertEquals(limit1, limit2);
        assertNotEquals(limit1, limit3);
    }

    @Test
    public void testEqualsWithExpression() {
        Limit limit1 = new Limit("LIMIT 10");
        Limit limit2 = new Limit("LIMIT 10");
        Limit limit3 = new Limit("LIMIT 20");

        assertEquals(limit1, limit2);
        assertNotEquals(limit1, limit3);
    }

    @Test
    public void testEqualsWithNull() {
        Limit limit = new Limit(10);

        assertNotEquals(limit, null);
    }

    @Test
    public void testEqualsWithDifferentClass() {
        Limit limit = new Limit(10);
        OrderBy orderBy = new OrderBy("id");

        assertNotEquals(limit, (Object) orderBy);
    }

    @Test
    public void testHashCode() {
        Limit limit1 = new Limit(10);
        Limit limit2 = new Limit(10);

        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    public void testHashCodeWithOffset() {
        Limit limit1 = new Limit(20, 10);
        Limit limit2 = new Limit(20, 10);

        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    public void testHashCodeWithExpression() {
        Limit limit1 = new Limit("LIMIT 10");
        Limit limit2 = new Limit("LIMIT 10");

        assertEquals(limit1.hashCode(), limit2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        Limit limit = new Limit(50, 10);
        int hash1 = limit.hashCode();
        int hash2 = limit.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testOperatorType() {
        Limit limit = new Limit(10);

        assertEquals(Operator.LIMIT, limit.getOperator());
        assertNotEquals(Operator.WHERE, limit.getOperator());
    }

    @Test
    public void testZeroOffset() {
        Limit limit = new Limit(0, 10);

        assertEquals(0, limit.getOffset());
        assertEquals(10, limit.getCount());
    }

    @Test
    public void testLargeOffset() {
        Limit limit = new Limit(1000000, 50);

        assertEquals(1000000, limit.getOffset());
        assertEquals(50, limit.getCount());
    }

    @Test
    public void testLargeCount() {
        Limit limit = new Limit(Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, limit.getCount());
    }

    @Test
    public void testToStringFormat() {
        Limit limit = new Limit(100);
        String result = limit.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

        assertNotNull(result);
        assertTrue(result.contains("100"));
    }

    @Test
    public void testCopy() {
        Limit original = new Limit(30, 10);
        Limit copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getCount(), copy.getCount());
        assertEquals(original.getOffset(), copy.getOffset());
    }

    @Test
    public void testCopyWithExpression() {
        Limit original = new Limit("LIMIT 20");
        Limit copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getExpr(), copy.getExpr());
    }

    @Test
    public void testPagination() {
        // Page 1: First 10 records
        Limit page1 = new Limit(0, 10);
        assertEquals(0, page1.getOffset());
        assertEquals(10, page1.getCount());

        // Page 2: Next 10 records
        Limit page2 = new Limit(10, 10);
        assertEquals(10, page2.getOffset());
        assertEquals(10, page2.getCount());

        // Page 3: Next 10 records
        Limit page3 = new Limit(20, 10);
        assertEquals(20, page3.getOffset());
        assertEquals(10, page3.getCount());
    }

    @Test
    public void testCustomExpressionFormats() {
        Limit mysqlStyle = new Limit("10, 20");
        assertEquals("10, 20", mysqlStyle.getExpr());

        Limit standardStyle = new Limit("20 OFFSET 10");
        assertEquals("20 OFFSET 10", standardStyle.getExpr());
    }
}
