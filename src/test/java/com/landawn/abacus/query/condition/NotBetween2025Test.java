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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class NotBetween2025Test extends TestBase {

    @Test
    public void testConstructor_ValidRange() {
        NotBetween condition = new NotBetween("age", 18, 65);

        assertEquals("age", condition.getPropName());
        assertEquals(Integer.valueOf(18), condition.getMinValue());
        assertEquals(Integer.valueOf(65), condition.getMaxValue());
        assertEquals(Operator.NOT_BETWEEN, condition.operator());
    }

    @Test
    public void testConstructor_NullPropName() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween(null, 1, 10));
    }

    @Test
    public void testConstructor_EmptyPropName() {
        assertThrows(IllegalArgumentException.class, () -> new NotBetween("", 1, 10));
    }

    @Test
    public void testConstructor_NumericRange() {
        NotBetween condition = new NotBetween("price", 10.0, 100.0);

        assertEquals("price", condition.getPropName());
        assertEquals(10.0, (Double) condition.getMinValue());
        assertEquals(100.0, (Double) condition.getMaxValue());
    }

    @Test
    public void testConstructor_StringRange() {
        NotBetween condition = new NotBetween("grade", "A", "C");

        assertEquals("grade", condition.getPropName());
        assertEquals("A", condition.getMinValue());
        assertEquals("C", condition.getMaxValue());
    }

    @Test
    public void testConstructor_DateRange() {
        NotBetween condition = new NotBetween("order_date", "2024-01-01", "2024-12-31");

        assertEquals("order_date", condition.getPropName());
        assertEquals("2024-01-01", condition.getMinValue());
        assertEquals("2024-12-31", condition.getMaxValue());
    }

    @Test
    public void testConstructor_NullValues() {
        NotBetween condition = new NotBetween("score", null, null);

        assertEquals("score", condition.getPropName());
        assertNull(condition.getMinValue());
        assertNull(condition.getMaxValue());
    }

    @Test
    public void testGetPropName() {
        NotBetween condition = new NotBetween("temperature", 36.0, 37.5);
        assertEquals("temperature", condition.getPropName());
    }

    @Test
    public void testGetMinValue() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertEquals(Integer.valueOf(18), condition.getMinValue());
    }

    @Test
    public void testGetMaxValue() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertEquals(Integer.valueOf(65), condition.getMaxValue());
    }

    @Test
    public void testSetMinValue() {
        NotBetween condition = new NotBetween("age", 18, 65);
        condition.setMinValue(21);
        assertEquals(Integer.valueOf(21), condition.getMinValue());
    }

    @Test
    public void testSetMaxValue() {
        NotBetween condition = new NotBetween("age", 18, 65);
        condition.setMaxValue(70);
        assertEquals(Integer.valueOf(70), condition.getMaxValue());
    }

    @Test
    public void testGetParameters_Simple() {
        NotBetween condition = new NotBetween("age", 18, 65);
        List<Object> params = condition.getParameters();

        assertEquals(2, params.size());
        assertEquals(Integer.valueOf(18), params.get(0));
        assertEquals(Integer.valueOf(65), params.get(1));
    }

    @Test
    public void testGetParameters_WithConditionValues() {
        Expression minExpr = new Expression("MIN(age)");
        Expression maxExpr = new Expression("MAX(age)");
        NotBetween condition = new NotBetween("score", minExpr, maxExpr);

        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testGetParameters_NullValues() {
        NotBetween condition = new NotBetween("score", null, null);
        List<Object> params = condition.getParameters();

        assertEquals(2, params.size());
        assertNull(params.get(0));
        assertNull(params.get(1));
    }

    @Test
    public void testClearParameters() {
        NotBetween condition = new NotBetween("age", 18, 65);
        condition.clearParameters();

        assertNull(condition.getMinValue());
        assertNull(condition.getMaxValue());
    }

    @Test
    public void testClearParameters_WithConditionValues() {
        Expression minExpr = new Expression("MIN(age)");
        Expression maxExpr = new Expression("MAX(age)");
        NotBetween condition = new NotBetween("score", minExpr, maxExpr);

        condition.clearParameters();
        // Expressions should have their parameters cleared
        assertNotNull(condition.getMinValue());
        assertNotNull(condition.getMaxValue());
    }

    @Test
    public void testCopy() {
        NotBetween original = new NotBetween("age", 18, 65);
        NotBetween copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getMinValue(), (Object) copy.getMinValue());
        assertEquals((Object) original.getMaxValue(), (Object) copy.getMaxValue());
    }

    @Test
    public void testCopy_DeepCopy() {
        NotBetween original = new NotBetween("score", 60, 80);
        NotBetween copy = original.copy();

        // Modify original
        original.clearParameters();

        // Copy should not be affected
        assertEquals(Integer.valueOf(60), copy.getMinValue());
        assertEquals(Integer.valueOf(80), copy.getMaxValue());
    }

    @Test
    public void testCopy_WithConditionValues() {
        Expression minExpr = new Expression("MIN(age)");
        Expression maxExpr = new Expression("MAX(age)");
        NotBetween original = new NotBetween("score", minExpr, maxExpr);

        NotBetween copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.getMinValue(), copy.getMinValue());
        assertNotSame(original.getMaxValue(), copy.getMaxValue());
    }

    @Test
    public void testToString_NoChange() {
        NotBetween condition = new NotBetween("age", 18, 65);
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("age"));
        assertTrue(result.contains("NOT BETWEEN"));
        assertTrue(result.contains("18"));
        assertTrue(result.contains("65"));
    }

    @Test
    public void testToString_StringValues() {
        NotBetween condition = new NotBetween("grade", "A", "C");
        String result = condition.toString(NamingPolicy.NO_CHANGE);

        assertTrue(result.contains("grade"));
        assertTrue(result.contains("NOT BETWEEN"));
    }

    @Test
    public void testToString_SnakeCase() {
        NotBetween condition = new NotBetween("maxAge", 18, 65);
        String result = condition.toString(NamingPolicy.SNAKE_CASE);

        assertTrue(result.contains("max_age"));
        assertTrue(result.contains("NOT BETWEEN"));
    }

    @Test
    public void testHashCode_Equal() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 18, 65);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCode_Different() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 20, 65);

        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 18, 65);

        assertEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("score", 18, 65);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentMinValue() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 20, 65);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_DifferentMaxValue() {
        NotBetween c1 = new NotBetween("age", 18, 65);
        NotBetween c2 = new NotBetween("age", 18, 70);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testEquals_Null() {
        NotBetween condition = new NotBetween("age", 18, 65);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentType() {
        NotBetween condition = new NotBetween("age", 18, 65);
        String other = "not a NotBetween";
        assertNotEquals(condition, other);
    }

    @Test
    public void testUseCaseScenario_ExtremeValues() {
        // Find products with extreme prices (very cheap or very expensive)
        NotBetween priceRange = new NotBetween("price", 10.0, 1000.0);
        String sql = priceRange.toString(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("price"));
        assertTrue(sql.contains("NOT BETWEEN"));
        assertEquals(2, priceRange.getParameters().size());
    }

    @Test
    public void testUseCaseScenario_OutsideBusinessHours() {
        // Find orders outside business hours (before 9 AM or after 5 PM)
        NotBetween outsideHours = new NotBetween("order_hour", 9, 17);
        String sql = outsideHours.toString(NamingPolicy.NO_CHANGE);

        assertTrue(sql.contains("order_hour"));
        assertTrue(sql.contains("NOT BETWEEN"));
    }

    @Test
    public void testUseCaseScenario_AbnormalTemperature() {
        // Exclude normal temperature range
        NotBetween abnormalTemp = new NotBetween("temperature", 36.0, 37.5);
        List<Object> params = abnormalTemp.getParameters();

        assertEquals(2, params.size());
        assertEquals(36.0, (Double) params.get(0));
        assertEquals(37.5, (Double) params.get(1));
    }

    @Test
    public void testAnd() {
        NotBetween cond1 = new NotBetween("age", 18, 65);
        NotBetween cond2 = new NotBetween("salary", 30000, 100000);
        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        NotBetween cond1 = new NotBetween("age", 18, 30);
        NotBetween cond2 = new NotBetween("age", 50, 65);
        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testNot() {
        NotBetween condition = new NotBetween("age", 18, 65);
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }
}
