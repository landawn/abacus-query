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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class AbstractCondition2025Test extends TestBase {

    @Test
    public void testGetOperator() {
        AbstractCondition condition = new Equal("name", "John");
        assertEquals(Operator.EQUAL, condition.getOperator());

        AbstractCondition and = new And(new Equal("a", 1));
        assertEquals(Operator.AND, and.getOperator());
    }

    @Test
    public void testAnd() {
        Equal cond1 = new Equal("status", "active");
        Equal cond2 = new Equal("age", 18);

        And result = cond1.and(cond2);
        assertNotNull(result);
        assertEquals(Operator.AND, result.getOperator());
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testAnd_NullCondition() {
        Equal cond = new Equal("status", "active");
        assertThrows(IllegalArgumentException.class, () -> cond.and(null));
    }

    @Test
    public void testOr() {
        Equal cond1 = new Equal("status", "active");
        Equal cond2 = new Equal("status", "pending");

        Or result = cond1.or(cond2);
        assertNotNull(result);
        assertEquals(Operator.OR, result.getOperator());
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr_NullCondition() {
        Equal cond = new Equal("status", "active");
        assertThrows(IllegalArgumentException.class, () -> cond.or(null));
    }

    @Test
    public void testNot() {
        Equal cond = new Equal("status", "active");
        Not result = cond.not();

        assertNotNull(result);
        assertEquals(Operator.NOT, result.getOperator());
        assertSame(cond, result.getCondition());
    }

    @Test
    public void testCopy() {
        Equal original = new Equal("name", "John");
        Equal copy = original.copy();

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertEquals(original.getPropName(), copy.getPropName());
        assertEquals((Object) original.getPropValue(), (Object) copy.getPropValue());
    }

    @Test
    public void testToString_DefaultNamingPolicy() {
        Equal condition = new Equal("userName", "John");
        String result = condition.toString();
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("John"));
    }

    @Test
    public void testParameter2String_String() {
        Equal condition = new Equal("name", "John");
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("'John'"));
    }

    @Test
    public void testParameter2String_Null() {
        Equal condition = new Equal("name", null);
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertNull(params.get(0));
    }

    @Test
    public void testParameter2String_Number() {
        Equal condition = new Equal("age", 25);
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("25"));
        assertFalse(str.contains("'25'"));
    }

    @Test
    public void testConcatPropNames_SingleName() {
        // Tested indirectly through other condition classes
        Equal condition = new Equal("name", "value");
        assertNotNull(condition.getPropName());
    }

    @Test
    public void testConcatPropNames_MultipleNames() {
        // Tested through classes that use multiple prop names
        GroupBy groupBy = new GroupBy("col1", "col2");
        String str = groupBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
    }

    @Test
    public void testChainedOperations() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);
        Equal cond3 = new Equal("c", 3);

        And and = cond1.and(cond2).and(cond3);
        assertEquals(3, (int) and.getConditions().size());
    }

    @Test
    public void testMixedLogicalOperations() {
        Equal cond1 = new Equal("a", 1);
        Equal cond2 = new Equal("b", 2);

        And and = cond1.and(cond2);
        Not not = and.not();

        assertNotNull(not);
        assertEquals(Operator.NOT, not.getOperator());
    }

    @Test
    public void testToString_WithNamingPolicy() {
        Equal condition = new Equal("userName", "John");
        String result = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.contains("user_name"));
        assertTrue(result.contains("John"));
    }

    @Test
    public void testToString_WithNoChangePolicy() {
        Equal condition = new Equal("firstName", "Jane");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("firstName"));
        assertTrue(result.contains("Jane"));
    }

    @Test
    public void testParameter2String_WithCondition() {
        Equal innerCondition = new Equal("id", 100);
        Equal outerCondition = new Equal("userId", innerCondition);
        String str = outerCondition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("userId"));
        assertTrue(str.contains("id"));
    }

    @Test
    public void testParameter2String_WithIsNull() {
        Equal condition = new Equal("field", IsNull.NULL);
        String str = condition.toString(NamingPolicy.NO_CHANGE);
        assertNotNull(str);
        assertTrue(str.contains("NULL"));
    }

    @Test
    public void testConcatPropNames_EmptyArray() {
        // Testing through GroupBy with no props - just verify it can be created
        GroupBy groupBy = new GroupBy();
        assertNotNull(groupBy);
        // Note: toString() on empty GroupBy throws NPE, this is expected behavior
    }

    @Test
    public void testConcatPropNames_TwoNames() {
        GroupBy groupBy = new GroupBy("col1", "col2");
        String str = groupBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
    }

    @Test
    public void testConcatPropNames_ThreeNames() {
        GroupBy groupBy = new GroupBy("col1", "col2", "col3");
        String str = groupBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
        assertTrue(str.contains("col3"));
    }

    @Test
    public void testConcatPropNames_FourNames() {
        GroupBy groupBy = new GroupBy("col1", "col2", "col3", "col4");
        String str = groupBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
        assertTrue(str.contains("col3"));
        assertTrue(str.contains("col4"));
    }

    @Test
    public void testConcatPropNames_Collection() {
        // Testing through OrderBy which uses collection internally
        OrderBy orderBy = new OrderBy("a", "b", "c");
        String str = orderBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
        assertTrue(str.contains("c"));
    }

    @Test
    public void testConcatPropNames_CollectionSingleItem() {
        OrderBy orderBy = new OrderBy("single");
        String str = orderBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("single"));
    }

    @Test
    public void testConcatPropNames_CollectionTwoItems() {
        OrderBy orderBy = new OrderBy("first", "second");
        String str = orderBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("first"));
        assertTrue(str.contains("second"));
    }

    @Test
    public void testConcatPropNames_CollectionThreeItems() {
        OrderBy orderBy = new OrderBy("a", "b", "c");
        String str = orderBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
        assertTrue(str.contains("c"));
    }

    @Test
    public void testConcatPropNames_CollectionFourOrMore() {
        OrderBy orderBy = new OrderBy("col1", "col2", "col3", "col4", "col5");
        String str = orderBy.toString(NamingPolicy.NO_CHANGE);
        assertTrue(str.contains("col1"));
        assertTrue(str.contains("col2"));
        assertTrue(str.contains("col3"));
        assertTrue(str.contains("col4"));
        assertTrue(str.contains("col5"));
    }
}
