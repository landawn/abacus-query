package com.landawn.abacus.query.condition;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.ConditionFactory;
import com.landawn.abacus.query.condition.Equal;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.GreaterThan;
import com.landawn.abacus.query.condition.LessEqual;
import com.landawn.abacus.query.condition.Not;
import com.landawn.abacus.query.condition.NotEqual;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Or;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.util.NamingPolicy;

public class EqualTest extends TestBase {

    @Test
    public void testConstructor() {
        Equal eq = ConditionFactory.eq("status", "active");

        Assertions.assertNotNull(eq);
        Assertions.assertEquals("status", eq.getPropName());
        Assertions.assertEquals("active", eq.getPropValue());
        Assertions.assertEquals(Operator.EQUAL, eq.getOperator());
    }

    @Test
    public void testConstructorWithDifferentTypes() {
        // Test with Integer
        Equal eqInt = ConditionFactory.eq("count", 100);
        Assertions.assertEquals(100, (Integer) eqInt.getPropValue());

        // Test with Boolean
        Equal eqBool = ConditionFactory.eq("active", true);
        Assertions.assertEquals(true, eqBool.getPropValue());

        // Test with Double
        Equal eqDouble = ConditionFactory.eq("price", 99.99);
        Assertions.assertEquals(99.99, eqDouble.getPropValue());

        // Test with Date
        Date now = new Date();
        Equal eqDate = ConditionFactory.eq("createdDate", now);
        Assertions.assertEquals(now, eqDate.getPropValue());

        // Test with null
        Equal eqNull = ConditionFactory.eq("deletedDate", null);
        Assertions.assertNull(eqNull.getPropValue());
    }

    @Test
    public void testConstructorWithEmptyPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Equal("", "value");
        });
    }

    @Test
    public void testConstructorWithNullPropName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Equal(null, "value");
        });
    }

    @Test
    public void testToString() {
        Equal eq = ConditionFactory.eq("name", "John");
        String result = eq.toString();
        Assertions.assertEquals("name = 'John'", result);
    }

    @Test
    public void testToStringWithNumber() {
        Equal eq = ConditionFactory.eq("age", 25);
        String result = eq.toString();
        Assertions.assertEquals("age = 25", result);
    }

    @Test
    public void testToStringWithBoolean() {
        Equal eq = ConditionFactory.eq("isActive", true);
        String result = eq.toString();
        Assertions.assertEquals("isActive = true", result);
    }

    @Test
    public void testToStringWithNull() {
        Equal eq = ConditionFactory.eq("middleName", null);
        String result = eq.toString();
        Assertions.assertEquals("middleName = null", result);
    }

    @Test
    public void testToStringWithNamingPolicy() {
        Equal eq = ConditionFactory.eq("firstName", "Jane");
        String result = eq.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        Assertions.assertEquals("first_name = 'Jane'", result);
    }

    @Test
    public void testGetParameters() {
        Equal eq = ConditionFactory.eq("email", "test@example.com");
        var params = eq.getParameters();

        Assertions.assertEquals(1, params.size());
        Assertions.assertEquals("test@example.com", params.get(0));
    }

    @Test
    public void testClearParameters() {
        Equal eq = ConditionFactory.eq("id", 12345);
        Assertions.assertEquals(12345, (Integer) eq.getPropValue());

        eq.clearParameters();
        Assertions.assertNull(eq.getPropValue());
    }

    @Test
    public void testCopy() {
        Equal original = ConditionFactory.eq("department", "Sales");
        Equal copy = original.copy();

        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals(original.getPropName(), copy.getPropName());
        Assertions.assertEquals((Object) original.getPropValue(), copy.getPropValue());
        Assertions.assertEquals(original.getOperator(), copy.getOperator());
    }

    @Test
    public void testEquals() {
        Equal eq1 = ConditionFactory.eq("status", "active");
        Equal eq2 = ConditionFactory.eq("status", "active");
        Equal eq3 = ConditionFactory.eq("status", "inactive");
        Equal eq4 = ConditionFactory.eq("type", "active");

        Assertions.assertEquals(eq1, eq1);
        Assertions.assertEquals(eq1, eq2);
        Assertions.assertNotEquals(eq1, eq3); // Different value
        Assertions.assertNotEquals(eq1, eq4); // Different property
        Assertions.assertNotEquals(eq1, null);
        Assertions.assertNotEquals(eq1, "string");
    }

    @Test
    public void testHashCode() {
        Equal eq1 = ConditionFactory.eq("code", "ABC123");
        Equal eq2 = ConditionFactory.eq("code", "ABC123");

        Assertions.assertEquals(eq1.hashCode(), eq2.hashCode());
    }

    @Test
    public void testAnd() {
        Equal eq1 = ConditionFactory.eq("status", "active");
        Equal eq2 = ConditionFactory.eq("type", "premium");

        And and = eq1.and(eq2);

        Assertions.assertNotNull(and);
        Assertions.assertEquals(Operator.AND, and.getOperator());
        Assertions.assertEquals(2, and.getConditions().size());
        Assertions.assertTrue(and.getConditions().contains(eq1));
        Assertions.assertTrue(and.getConditions().contains(eq2));
    }

    @Test
    public void testOr() {
        Equal eq1 = ConditionFactory.eq("department", "Sales");
        Equal eq2 = ConditionFactory.eq("department", "Marketing");

        Or or = eq1.or(eq2);

        Assertions.assertNotNull(or);
        Assertions.assertEquals(Operator.OR, or.getOperator());
        Assertions.assertEquals(2, or.getConditions().size());
    }

    @Test
    public void testNot() {
        Equal eq = ConditionFactory.eq("deleted", false);

        Not not = eq.not();

        Assertions.assertNotNull(not);
        Assertions.assertEquals(Operator.NOT, not.getOperator());
        Assertions.assertEquals(eq, not.getCondition());
    }

    @Test
    public void testWithSubQuery() {
        SubQuery subQuery = ConditionFactory.subQuery("SELECT MAX(salary) FROM employees");
        Equal eq = new Equal("salary", subQuery);

        Assertions.assertEquals(subQuery, eq.getPropValue());

        String result = eq.toString();
        Assertions.assertTrue(result.contains("salary ="));
        Assertions.assertTrue(result.contains("SELECT MAX(salary) FROM employees"));
    }

    @Test
    public void testSetPropValue() {
        Equal eq = ConditionFactory.eq("status", "pending");
        Assertions.assertEquals("pending", eq.getPropValue());

        eq.setPropValue("approved");
        Assertions.assertEquals("approved", eq.getPropValue());
    }

    @Test
    public void testWithExpression() {
        Expression expr = Expression.of("CURRENT_TIMESTAMP");
        Equal eq = new Equal("lastModified", expr);

        Assertions.assertEquals(expr, eq.getPropValue());

        String result = eq.toString();
        Assertions.assertEquals("lastModified = CURRENT_TIMESTAMP", result);
    }

    @Test
    public void testEqualWithQuestionMark() {
        Equal eq = ConditionFactory.equal("name");

        Assertions.assertEquals(ConditionFactory.QME, eq.getPropValue());
        String result = eq.toString();
        Assertions.assertEquals("name = ?", result);
    }

    @Test
    public void testEqWithQuestionMark() {
        Equal eq = ConditionFactory.eq("id");

        Assertions.assertEquals(ConditionFactory.QME, eq.getPropValue());
        String result = eq.toString();
        Assertions.assertEquals("id = ?", result);
    }

    @Test
    public void testComplexEqualityChain() {
        Equal status = ConditionFactory.eq("status", "active");
        GreaterThan age = ConditionFactory.gt("age", 18);
        LessEqual salary = ConditionFactory.le("salary", 100000);
        NotEqual type = ConditionFactory.ne("type", "temporary");

        And complex = status.and(age).and(salary).and(type);

        Assertions.assertNotNull(complex);
        Assertions.assertEquals(4, complex.getConditions().size());
    }
}