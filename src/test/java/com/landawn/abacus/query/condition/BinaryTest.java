package com.landawn.abacus.query.condition;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("2025")
class Binary2025Test extends TestBase {

    @Test
    public void testConstructor() {
        Binary condition = new Binary("age", Operator.EQUAL, 25);
        assertEquals("age", condition.getPropName());
        assertEquals(25, (int) condition.getPropValue());
        assertEquals(Operator.EQUAL, condition.operator());
    }

    @Test
    public void testConstructor_NullPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Binary(null, Operator.EQUAL, 25));
    }

    @Test
    public void testConstructor_EmptyPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> new Binary("", Operator.EQUAL, 25));
    }

    @Test
    public void testGetPropName() {
        Binary condition = new Binary("userName", Operator.LIKE, "%test%");
        assertEquals("userName", condition.getPropName());
    }

    @Test
    public void testGetPropValue() {
        Binary condition = new Binary("age", Operator.GREATER_THAN, 30);
        Integer value = condition.getPropValue();
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testGetPropValue_String() {
        Binary condition = new Binary("name", Operator.EQUAL, "Alice");
        String value = condition.getPropValue();
        assertEquals("Alice", value);
    }

    @Test
    public void testGetPropValue_Null() {
        Binary condition = new Binary("field", Operator.IS, null);
        assertNull(condition.getPropValue());
    }

    @Test
    public void testGetOperator() {
        Binary condition = new Binary("field", Operator.NOT_EQUAL, "value");
        assertEquals(Operator.NOT_EQUAL, condition.operator());
    }

    @Test
    public void testGetParameters() {
        Binary condition = new Binary("status", Operator.EQUAL, "active");
        List<Object> params = condition.getParameters();
        assertEquals(1, (int) params.size());
        assertEquals("active", params.get(0));
    }

    @Test
    public void testGetParameters_WithCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users");
        Binary condition = new Binary("userId", Operator.IN, subQuery);
        List<Object> params = condition.getParameters();
        assertNotNull(params);
    }

    @Test
    public void testClearParameters() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        condition.clearParameters();
        assertNull(condition.getPropValue());
    }

    @Test
    public void testClearParameters_WithCondition() {
        Equal innerCond = new Equal("id", 100);
        Binary condition = new Binary("userId", Operator.EQUAL, innerCond);
        condition.clearParameters();
        assertNull(innerCond.getPropValue());
    }

    @Test
    public void testToString_NoChange() {
        Binary condition = new Binary("userName", Operator.EQUAL, "Alice");
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("userName"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("="));
    }

    @Test
    public void testToString_SnakeCase() {
        Binary condition = new Binary("userName", Operator.GREATER_THAN, "Bob");
        String result = condition.toString(NamingPolicy.SNAKE_CASE);
        assertTrue(result.contains("user_name"));
    }

    @Test
    public void testToStringWithNullAndEqualOperator() {
        Binary condition = new Binary("deletedAt", Operator.EQUAL, null);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertEquals("deletedAt IS NULL", result);
    }

    @Test
    public void testToStringWithNullAndNotEqualOperator() {
        Binary condition = new Binary("deletedAt", Operator.NOT_EQUAL, null);
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertEquals("deletedAt IS NOT NULL", result);
    }

    @Test
    public void testToString_WithSubQueryAddsParentheses() {
        Binary condition = new Binary("userId", Operator.EQUAL, Filters.subQuery("SELECT id FROM users"));
        String result = condition.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("= (SELECT id FROM users)"));
    }

    @Test
    public void testHashCode() {
        Binary cond1 = new Binary("age", Operator.EQUAL, 25);
        Binary cond2 = new Binary("age", Operator.EQUAL, 25);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        Binary cond1 = new Binary("age", Operator.EQUAL, 25);
        Binary cond2 = new Binary("age", Operator.EQUAL, 30);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        Binary cond1 = new Binary("status", Operator.EQUAL, "active");
        Binary cond2 = new Binary("status", Operator.EQUAL, "active");
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropName() {
        Binary cond1 = new Binary("field1", Operator.EQUAL, "value");
        Binary cond2 = new Binary("field2", Operator.EQUAL, "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentOperator() {
        Binary cond1 = new Binary("field", Operator.EQUAL, "value");
        Binary cond2 = new Binary("field", Operator.NOT_EQUAL, "value");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentPropValue() {
        Binary cond1 = new Binary("field", Operator.EQUAL, "value1");
        Binary cond2 = new Binary("field", Operator.EQUAL, "value2");
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        assertNotEquals(condition, "string");
    }

    @Test
    public void testAnd() {
        Binary cond1 = new Binary("a", Operator.EQUAL, 1);
        Binary cond2 = new Binary("b", Operator.EQUAL, 2);
        And result = cond1.and(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testOr() {
        Binary cond1 = new Binary("a", Operator.EQUAL, 1);
        Binary cond2 = new Binary("b", Operator.EQUAL, 2);
        Or result = cond1.or(cond2);
        assertEquals(Integer.valueOf(2), result.getConditions().size());
    }

    @Test
    public void testAndRejectsClauseOperand() {
        Binary cond = new Binary("a", Operator.EQUAL, 1);
        assertThrows(IllegalArgumentException.class, () -> cond.and(Filters.where(Filters.equal("b", 2))));
    }

    @Test
    public void testOrRejectsClauseOperand() {
        Binary cond = new Binary("a", Operator.EQUAL, 1);
        assertThrows(IllegalArgumentException.class, () -> cond.or(Filters.orderBy("b")));
    }

    @Test
    public void testXorRejectsClauseOperand() {
        Binary cond = new Binary("a", Operator.EQUAL, 1);
        assertThrows(IllegalArgumentException.class, () -> cond.xor(Filters.limit(1)));
    }

    @Test
    public void testNot() {
        Binary condition = new Binary("field", Operator.EQUAL, "value");
        Not result = condition.not();
        assertNotNull(result);
        assertEquals(Operator.NOT, result.operator());
    }

    @Test
    public void testDifferentOperators() {
        Binary eq = new Binary("a", Operator.EQUAL, 1);
        Binary gt = new Binary("b", Operator.GREATER_THAN, 2);
        Binary lt = new Binary("c", Operator.LESS_THAN, 3);
        Binary like = new Binary("d", Operator.LIKE, "%test%");

        assertEquals(Operator.EQUAL, eq.operator());
        assertEquals(Operator.GREATER_THAN, gt.operator());
        assertEquals(Operator.LESS_THAN, lt.operator());
        assertEquals(Operator.LIKE, like.operator());
    }

}

public class BinaryTest extends TestBase {

    @Test
    public void testConstructor() {
        Binary binary = Filters.binary("price", Operator.GREATER_THAN, 100.0);

        Assertions.assertNotNull(binary);
        Assertions.assertEquals("price", binary.getPropName());
        Assertions.assertEquals(Operator.GREATER_THAN, binary.operator());
        Assertions.assertEquals(100.0, binary.getPropValue());
    }

    @Test
    public void testConstructorWithNullValue() {
        Binary binary = Filters.binary("optional", Operator.EQUAL, null);

        Assertions.assertNotNull(binary);
        Assertions.assertNull(binary.getPropValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Binary("", Operator.EQUAL, "value");
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Binary(null, Operator.EQUAL, "value");
        });
    }

    @Test
    public void testGetPropName() {
        Binary binary = Filters.binary("userName", Operator.EQUAL, "John");
        Assertions.assertEquals("userName", binary.getPropName());
    }

    @Test
    public void testGetPropValue() {
        Binary binary = Filters.binary("age", Operator.GREATER_THAN_OR_EQUAL, 25);
        Integer value = binary.getPropValue();
        Assertions.assertEquals(25, value);
    }

    @Test
    public void testGetParametersWithLiteralValue() {
        Binary binary = Filters.binary("score", Operator.LESS_THAN, 80.5);
        List<Object> params = binary.getParameters();

        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals(80.5, params.get(0));
    }

    @Test
    public void testGetParametersWithConditionValue() {
        SubQuery subQuery = Filters.subQuery("SELECT MAX(price) FROM products");
        Binary binary = Filters.binary("price", Operator.EQUAL, subQuery);

        List<Object> params = binary.getParameters();
        Assertions.assertEquals(subQuery.getParameters(), params);
    }

    @Test
    public void testClearParametersWithLiteralValue() {
        Binary binary = Filters.binary("count", Operator.GREATER_THAN, 100);
        binary.clearParameters();

        Assertions.assertNull(binary.getPropValue());
    }

    @Test
    public void testClearParametersWithConditionValue() {
        Between between = Filters.between("value", 10, 20);
        Binary binary = Filters.binary("range", Operator.EQUAL, between);

        binary.clearParameters();

        List<Object> params = binary.getParameters();
        Assertions.assertTrue(params.size() == 2 && params.get(0) == null && params.get(1) == null);
    }

    @Test
    public void testToString() {
        Binary binary = Filters.binary("name", Operator.NOT_EQUAL, "Test");
        String result = binary.toString();

        Assertions.assertEquals("name != 'Test'", result);
    }

    @Test
    public void testToStringWithNumber() {
        Binary binary = Filters.binary("amount", Operator.GREATER_THAN, 1000);
        String result = binary.toString();

        Assertions.assertEquals("amount > 1000", result);
    }

    @Test
    public void testToStringWithNull() {
        Binary binary = Filters.binary("deleted", Operator.EQUAL, null);
        String result = binary.toString();

        Assertions.assertEquals("deleted IS NULL", result);
    }

    @Test
    public void testToStringWithNullAndNotEqual() {
        Binary binary = Filters.binary("deleted", Operator.NOT_EQUAL, null);
        String result = binary.toString();

        Assertions.assertEquals("deleted IS NOT NULL", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Binary binary = Filters.binary("firstName", Operator.LIKE, "John%");
        String result = binary.toString(NamingPolicy.SNAKE_CASE);

        Assertions.assertEquals("first_name LIKE 'John%'", result);
    }

    @Test
    public void testToStringWithConditionValue() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        Binary binary = Filters.binary("user_id", Operator.IN, subQuery);
        String result = binary.toString();

        Assertions.assertTrue(result.contains("user_id IN"));
        Assertions.assertTrue(result.contains("SELECT id FROM users WHERE active = true"));
    }

    @Test
    public void testEquals() {
        Binary binary1 = Filters.binary("age", Operator.GREATER_THAN_OR_EQUAL, 18);
        Binary binary2 = Filters.binary("age", Operator.GREATER_THAN_OR_EQUAL, 18);
        Binary binary3 = Filters.binary("age", Operator.GREATER_THAN_OR_EQUAL, 21);
        Binary binary4 = Filters.binary("age", Operator.GREATER_THAN, 18);
        Binary binary5 = Filters.binary("height", Operator.GREATER_THAN_OR_EQUAL, 18);

        Assertions.assertEquals(binary1, binary1);
        Assertions.assertEquals(binary1, binary2);
        Assertions.assertNotEquals(binary1, binary3); // Different value
        Assertions.assertNotEquals(binary1, binary4); // Different operator
        Assertions.assertNotEquals(binary1, binary5); // Different property
        Assertions.assertNotEquals(binary1, null);
        Assertions.assertNotEquals(binary1, "string");
    }

    @Test
    public void testHashCode() {
        Binary binary1 = Filters.binary("status", Operator.EQUAL, "active");
        Binary binary2 = Filters.binary("status", Operator.EQUAL, "active");

        Assertions.assertEquals(binary1.hashCode(), binary2.hashCode());
    }

    @Test
    public void testHashCodeWithNull() {
        Binary binary1 = Filters.binary("optional", Operator.EQUAL, null);
        Binary binary2 = Filters.binary("optional", Operator.EQUAL, null);

        Assertions.assertEquals(binary1.hashCode(), binary2.hashCode());
    }

    @Test
    public void testAnd() {
        Binary binary = Filters.binary("age", Operator.GREATER_THAN_OR_EQUAL, 18);
        LessThan lt = Filters.lt("age", 65);

        And and = binary.and(lt);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(binary));
        Assertions.assertTrue(and.getConditions().contains(lt));
    }

    @Test
    public void testOr() {
        Binary binary = Filters.binary("status", Operator.EQUAL, "premium");
        Equal eq = Filters.eq("status", "vip");

        Or or = binary.or(eq);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        Binary binary = Filters.binary("active", Operator.EQUAL, false);

        Not not = binary.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(binary, not.getCondition());
    }

    @Test
    public void testAllOperators() {
        // Test with various operators
        Binary eq = Filters.binary("prop", Operator.EQUAL, 1);
        Binary ne = Filters.binary("prop", Operator.NOT_EQUAL, 1);
        Binary gt = Filters.binary("prop", Operator.GREATER_THAN, 1);
        Binary ge = Filters.binary("prop", Operator.GREATER_THAN_OR_EQUAL, 1);
        Binary lt = Filters.binary("prop", Operator.LESS_THAN, 1);
        Binary le = Filters.binary("prop", Operator.LESS_THAN_OR_EQUAL, 1);
        Binary like = Filters.binary("prop", Operator.LIKE, "%test%");
        Binary in = Filters.binary("prop", Operator.IN, Arrays.asList(1, 2, 3));

        Assertions.assertEquals("prop = 1", eq.toString());
        Assertions.assertEquals("prop != 1", ne.toString());
        Assertions.assertEquals("prop > 1", gt.toString());
        Assertions.assertEquals("prop >= 1", ge.toString());
        Assertions.assertEquals("prop < 1", lt.toString());
        Assertions.assertEquals("prop <= 1", le.toString());
        Assertions.assertEquals("prop LIKE '%test%'", like.toString());
        Assertions.assertTrue(in.toString().contains("prop IN"));
    }
}
