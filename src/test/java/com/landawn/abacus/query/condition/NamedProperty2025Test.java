package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Comprehensive test class for NamedProperty.
 * Tests all public methods including factory method, condition creation methods, and utilities.
 */
@Tag("2025")
public class NamedProperty2025Test extends TestBase {

    @Test
    public void testConstructorWithPropertyName() {
        NamedProperty np = new NamedProperty("age");

        assertNotNull(np);
        assertEquals("age", np.propName());
    }

    @Test
    public void testOfMethod() {
        NamedProperty np = NamedProperty.of("username");

        assertNotNull(np);
        assertEquals("username", np.propName());
    }

    @Test
    public void testOfMethodCaching() {
        NamedProperty np1 = NamedProperty.of("age");
        NamedProperty np2 = NamedProperty.of("age");

        assertSame(np1, np2, "Should return cached instance");
    }

    @Test
    public void testOfMethodWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            NamedProperty.of(null);
        });
    }

    @Test
    public void testOfMethodWithEmptyStringThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            NamedProperty.of("");
        });
    }

    @Test
    public void testPropName() {
        NamedProperty np = NamedProperty.of("email");

        assertEquals("email", np.propName());
    }

    @Test
    public void testEq() {
        NamedProperty np = NamedProperty.of("status");
        Equal equal = np.eq("active");

        assertNotNull(equal);
        assertEquals("status", equal.getPropName());
        assertEquals("active", equal.getPropValue());
    }

    @Test
    public void testEqWithNumber() {
        NamedProperty np = NamedProperty.of("count");
        Equal equal = np.eq(5);

        assertEquals("count", equal.getPropName());
        assertEquals(5, (Integer) equal.getPropValue());
    }

    @Test
    public void testEqOrWithVarargs() {
        NamedProperty np = NamedProperty.of("color");
        Or or = np.eqOr("red", "green", "blue");

        assertNotNull(or);
        assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testEqOrWithCollection() {
        NamedProperty np = NamedProperty.of("city");
        List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago");

        Or or = np.eqOr(cities);

        assertNotNull(or);
        assertEquals(3, or.getConditions().size());
    }

    @Test
    public void testNe() {
        NamedProperty np = NamedProperty.of("status");
        NotEqual notEqual = np.ne("deleted");

        assertNotNull(notEqual);
        assertEquals("status", notEqual.getPropName());
        assertEquals("deleted", notEqual.getPropValue());
    }

    @Test
    public void testGt() {
        NamedProperty np = NamedProperty.of("age");
        GreaterThan gt = np.gt(18);

        assertNotNull(gt);
        assertEquals("age", gt.getPropName());
        assertEquals(18, (Integer) gt.getPropValue());
    }

    @Test
    public void testGe() {
        NamedProperty np = NamedProperty.of("score");
        GreaterThanOrEqual ge = np.ge(60);

        assertNotNull(ge);
        assertEquals("score", ge.getPropName());
        assertEquals(60, (Integer) ge.getPropValue());
    }

    @Test
    public void testLt() {
        NamedProperty np = NamedProperty.of("price");
        LessThan lt = np.lt(100);

        assertNotNull(lt);
        assertEquals("price", lt.getPropName());
        assertEquals(100, (Integer) lt.getPropValue());
    }

    @Test
    public void testLe() {
        NamedProperty np = NamedProperty.of("quantity");
        LessThanOrEqual le = np.le(10);

        assertNotNull(le);
        assertEquals("quantity", le.getPropName());
        assertEquals(10, (Integer) le.getPropValue());
    }

    @Test
    public void testIsNull() {
        NamedProperty np = NamedProperty.of("deletedDate");
        IsNull isNull = np.isNull();

        assertNotNull(isNull);
        assertEquals("deletedDate", isNull.getPropName());
    }

    @Test
    public void testIsNotNull() {
        NamedProperty np = NamedProperty.of("email");
        IsNotNull isNotNull = np.isNotNull();

        assertNotNull(isNotNull);
        assertEquals("email", isNotNull.getPropName());
    }

    @Test
    public void testBetween() {
        NamedProperty np = NamedProperty.of("age");
        Between between = np.between(18, 65);

        assertNotNull(between);
        assertEquals("age", between.getPropName());
        assertEquals(18, (Integer) between.getMinValue());
        assertEquals(65, (Integer) between.getMaxValue());
    }

    @Test
    public void testLike() {
        NamedProperty np = NamedProperty.of("name");
        Like like = np.like("John%");

        assertNotNull(like);
        assertEquals("name", like.getPropName());
        // Like extends Binary which has getValue()
        assertEquals("John%", ((Binary) like).getPropValue());
    }

    @Test
    public void testNotLike() {
        NamedProperty np = NamedProperty.of("email");
        NotLike notLike = np.notLike("%@temp.com");

        assertNotNull(notLike);
        assertEquals("email", notLike.getPropName());
        // NotLike extends Binary which has getValue()
        assertEquals("%@temp.com", ((Binary) notLike).getPropValue());
    }

    @Test
    public void testStartsWith() {
        NamedProperty np = NamedProperty.of("code");
        Like startsWith = np.startsWith("PRD");

        assertNotNull(startsWith);
        assertEquals("code", startsWith.getPropName());
        assertTrue(((Binary) startsWith).getPropValue().toString().endsWith("%"));
    }

    @Test
    public void testEndsWith() {
        NamedProperty np = NamedProperty.of("filename");
        Like endsWith = np.endsWith(".pdf");

        assertNotNull(endsWith);
        assertEquals("filename", endsWith.getPropName());
        assertTrue(((Binary) endsWith).getPropValue().toString().startsWith("%"));
    }

    @Test
    public void testContains() {
        NamedProperty np = NamedProperty.of("description");
        Like contains = np.contains("important");

        assertNotNull(contains);
        assertEquals("description", contains.getPropName());
        String value = ((Binary) contains).getPropValue().toString();
        assertTrue(value.startsWith("%"));
        assertTrue(value.endsWith("%"));
    }

    @Test
    public void testInWithVarargs() {
        NamedProperty np = NamedProperty.of("status");
        In in = np.in("active", "pending", "approved");

        assertNotNull(in);
        assertEquals("status", in.getPropName());
        assertEquals(3, in.getValues().size());
    }

    @Test
    public void testInWithCollection() {
        NamedProperty np = NamedProperty.of("id");
        Set<Integer> ids = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));

        In in = np.in(ids);

        assertNotNull(in);
        assertEquals("id", in.getPropName());
        assertEquals(5, in.getValues().size());
    }

    @Test
    public void testHashCode() {
        NamedProperty np1 = NamedProperty.of("age");
        NamedProperty np2 = NamedProperty.of("age");

        assertEquals(np1.hashCode(), np2.hashCode());
    }

    @Test
    public void testHashCodeDifferent() {
        NamedProperty np1 = NamedProperty.of("age");
        NamedProperty np2 = NamedProperty.of("name");

        assertNotEquals(np1.hashCode(), np2.hashCode());
    }

    @Test
    public void testEquals() {
        NamedProperty np1 = NamedProperty.of("status");
        NamedProperty np2 = NamedProperty.of("status");

        assertEquals(np1, np2);
    }

    @Test
    public void testEqualsSameInstance() {
        NamedProperty np = NamedProperty.of("test");

        assertEquals(np, np);
    }

    @Test
    public void testEqualsNull() {
        NamedProperty np = NamedProperty.of("test");

        assertNotEquals(np, null);
    }

    @Test
    public void testEqualsDifferentType() {
        NamedProperty np = NamedProperty.of("test");

        assertNotEquals(np, "not a named property");
    }

    @Test
    public void testEqualsDifferentProperty() {
        NamedProperty np1 = NamedProperty.of("age");
        NamedProperty np2 = NamedProperty.of("name");

        assertNotEquals(np1, np2);
    }

    @Test
    public void testToString() {
        NamedProperty np = NamedProperty.of("username");

        assertEquals("username", np.toString());
    }

    @Test
    public void testChainedConditions() {
        NamedProperty age = NamedProperty.of("age");

        Equal eq = age.eq(25);
        GreaterThan gt = age.gt(18);
        Between between = age.between(20, 30);

        assertNotNull(eq);
        assertNotNull(gt);
        assertNotNull(between);
    }

    @Test
    public void testComplexUsageWithOr() {
        NamedProperty priority = NamedProperty.of("priority");

        Or or = priority.eqOr(1, 2, 3, 4, 5);

        assertEquals(5, or.getConditions().size());
    }

    @Test
    public void testMultipleNamedProperties() {
        NamedProperty username = NamedProperty.of("username");
        NamedProperty email = NamedProperty.of("email");
        NamedProperty age = NamedProperty.of("age");

        assertNotNull(username);
        assertNotNull(email);
        assertNotNull(age);

        assertNotSame(username, email);
        assertNotSame(email, age);
    }

    @Test
    public void testBtDeprecatedMethod() {
        NamedProperty np = NamedProperty.of("score");

        @SuppressWarnings("deprecation")
        Between between = np.bt(0, 100);

        assertNotNull(between);
        assertEquals("score", between.getPropName());
    }

    @Test
    public void testLikePatterns() {
        NamedProperty name = NamedProperty.of("name");

        Like starts = name.startsWith("John");
        Like ends = name.endsWith("Smith");
        Like contains = name.contains("middle");
        Like custom = name.like("J%n");

        assertNotNull(starts);
        assertNotNull(ends);
        assertNotNull(contains);
        assertNotNull(custom);
    }

    @Test
    public void testComparisonOperators() {
        NamedProperty value = NamedProperty.of("value");

        Equal eq = value.eq(100);
        NotEqual ne = value.ne(0);
        GreaterThan gt = value.gt(50);
        GreaterThanOrEqual ge = value.ge(50);
        LessThan lt = value.lt(150);
        LessThanOrEqual le = value.le(150);

        assertNotNull(eq);
        assertNotNull(ne);
        assertNotNull(gt);
        assertNotNull(ge);
        assertNotNull(lt);
        assertNotNull(le);
    }

    @Test
    public void testNullChecks() {
        NamedProperty field = NamedProperty.of("optionalField");

        IsNull isNull = field.isNull();
        IsNotNull isNotNull = field.isNotNull();

        assertNotNull(isNull);
        assertNotNull(isNotNull);
    }

    @Test
    public void testComplexEqOrWithMixedTypes() {
        NamedProperty type = NamedProperty.of("type");

        Or or = type.eqOr("A", "B", "C", "D");

        assertEquals(4, or.getConditions().size());
    }

    @Test
    public void testEqOrWithSingleValue() {
        NamedProperty status = NamedProperty.of("status");

        Or or = status.eqOr("active");

        assertNotNull(or);
        assertEquals(1, or.getConditions().size());
    }

    @Test
    public void testEqOrWithEmptyCollection() {
        NamedProperty status = NamedProperty.of("status");

        assertThrows(IllegalArgumentException.class, () -> {
            status.eqOr(Arrays.asList());
        });
    }

    @Test
    public void testInWithSingleValue() {
        NamedProperty id = NamedProperty.of("id");

        In in = id.in(1);

        assertNotNull(in);
        assertEquals("id", in.getPropName());
        assertEquals(1, in.getValues().size());
    }

    @Test
    public void testBetweenWithStrings() {
        NamedProperty code = NamedProperty.of("code");

        Between between = code.between("A", "Z");

        assertNotNull(between);
        assertEquals("code", between.getPropName());
        assertEquals("A", between.getMinValue());
        assertEquals("Z", between.getMaxValue());
    }

    @Test
    public void testConstructorWithCamelCase() {
        NamedProperty np = new NamedProperty("firstName");

        assertEquals("firstName", np.propName());
    }

    @Test
    public void testConstructorWithUnderscore() {
        NamedProperty np = new NamedProperty("first_name");

        assertEquals("first_name", np.propName());
    }

    @Test
    public void testOfMethodWithComplexName() {
        NamedProperty np = NamedProperty.of("user.address.city");

        assertEquals("user.address.city", np.propName());
    }
}
