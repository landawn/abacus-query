package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;

/**
 * Comprehensive test class for NamedProperty.
 * Tests all public methods including factory method, condition creation methods, and utilities.
 */
@Tag("2025")
public class NamedPropertyTest extends TestBase {
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
    public void testOfMethodCachingIsAtomicDuringConcurrentFirstAccess() throws Exception {
        final int threadCount = 24;
        final String propName = "concurrentProperty" + System.nanoTime();
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch ready = new CountDownLatch(threadCount);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<NamedProperty>> futures = new java.util.ArrayList<>(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return NamedProperty.of(propName);
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            final NamedProperty expected = futures.get(0).get(10, TimeUnit.SECONDS);

            for (final Future<NamedProperty> future : futures) {
                assertSame(expected, future.get(10, TimeUnit.SECONDS));
            }

            assertSame(expected, NamedProperty.of(propName), "Racing callers must receive the instance retained in the cache");
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testOfMethodWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            NamedProperty.of(null);
        });
    }

    @Test
    public void testConstructorWithNullThrowsIAE() {
        // Constructor rejects null with IllegalArgumentException, consistent with of(null) and the
        // rest of the name-validation surface (previously threw NullPointerException).
        assertThrows(IllegalArgumentException.class, () -> {
            new NamedProperty(null);
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
    public void testEqual() {
        NamedProperty np = NamedProperty.of("status");
        Equal equal = np.equal("active");

        assertNotNull(equal);
        assertEquals("status", equal.propName());
        assertEquals("active", equal.propValue());
    }

    @Test
    public void testEq() {
        NamedProperty np = NamedProperty.of("status");
        Equal equal = np.eq("active");

        assertNotNull(equal);
        assertEquals("status", equal.propName());
        assertEquals("active", equal.propValue());
    }

    @Test
    public void testEqWithNumber() {
        NamedProperty np = NamedProperty.of("count");
        Equal equal = np.eq(5);

        assertEquals("count", equal.propName());
        assertEquals(5, (Integer) equal.propValue());
    }

    @Test
    public void testAnyEqualWithVarargs() {
        NamedProperty np = NamedProperty.of("color");
        Or or = np.equalsAny("red", "green", "blue");

        assertNotNull(or);
        assertEquals(3, or.conditions().size());
    }

    @Test
    public void testAnyEqualWithCollection() {
        NamedProperty np = NamedProperty.of("city");
        List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago");

        Or or = np.equalsAny(cities);

        assertNotNull(or);
        assertEquals(3, or.conditions().size());
    }

    @Test
    public void testNe() {
        NamedProperty np = NamedProperty.of("status");
        NotEqual notEqual = np.ne("deleted");

        assertNotNull(notEqual);
        assertEquals("status", notEqual.propName());
        assertEquals("deleted", notEqual.propValue());
    }

    @Test
    public void testGt() {
        NamedProperty np = NamedProperty.of("age");
        GreaterThan gt = np.gt(18);

        assertNotNull(gt);
        assertEquals("age", gt.propName());
        assertEquals(18, (Integer) gt.propValue());
    }

    @Test
    public void testGe() {
        NamedProperty np = NamedProperty.of("score");
        GreaterThanOrEqual ge = np.ge(60);

        assertNotNull(ge);
        assertEquals("score", ge.propName());
        assertEquals(60, (Integer) ge.propValue());
    }

    @Test
    public void testLt() {
        NamedProperty np = NamedProperty.of("price");
        LessThan lt = np.lt(100);

        assertNotNull(lt);
        assertEquals("price", lt.propName());
        assertEquals(100, (Integer) lt.propValue());
    }

    @Test
    public void testLe() {
        NamedProperty np = NamedProperty.of("quantity");
        LessThanOrEqual le = np.le(10);

        assertNotNull(le);
        assertEquals("quantity", le.propName());
        assertEquals(10, (Integer) le.propValue());
    }

    @Test
    public void testIsNull() {
        NamedProperty np = NamedProperty.of("deletedDate");
        IsNull isNull = np.isNull();

        assertNotNull(isNull);
        assertEquals("deletedDate", isNull.propName());
    }

    @Test
    public void testIsNotNull() {
        NamedProperty np = NamedProperty.of("email");
        IsNotNull isNotNull = np.isNotNull();

        assertNotNull(isNotNull);
        assertEquals("email", isNotNull.propName());
    }

    @Test
    public void testBetween() {
        NamedProperty np = NamedProperty.of("age");
        Between between = np.between(18, 65);

        assertNotNull(between);
        assertEquals("age", between.propName());
        assertEquals(18, (Integer) between.minValue());
        assertEquals(65, (Integer) between.maxValue());
    }

    @Test
    public void testLike() {
        NamedProperty np = NamedProperty.of("name");
        Like like = np.like("John%");

        assertNotNull(like);
        assertEquals("name", like.propName());
        // Like extends Binary which has getValue()
        assertEquals("John%", like.propValue());
    }

    @Test
    public void testNotLike() {
        NamedProperty np = NamedProperty.of("email");
        NotLike notLike = np.notLike("%@temp.com");

        assertNotNull(notLike);
        assertEquals("email", notLike.propName());
        // NotLike extends Binary which has getValue()
        assertEquals("%@temp.com", notLike.propValue());
    }

    @Test
    public void testStartsWith() {
        NamedProperty np = NamedProperty.of("code");
        Like startsWith = np.startsWith("PRD");

        assertNotNull(startsWith);
        assertEquals("code", startsWith.propName());
        assertTrue(startsWith.propValue().toString().endsWith("%"));
    }

    @Test
    public void testEndsWith() {
        NamedProperty np = NamedProperty.of("filename");
        Like endsWith = np.endsWith(".pdf");

        assertNotNull(endsWith);
        assertEquals("filename", endsWith.propName());
        assertTrue(endsWith.propValue().toString().startsWith("%"));
    }

    @Test
    public void testContains() {
        NamedProperty np = NamedProperty.of("description");
        Like contains = np.contains("important");

        assertNotNull(contains);
        assertEquals("description", contains.propName());
        String value = contains.propValue().toString();
        assertTrue(value.startsWith("%"));
        assertTrue(value.endsWith("%"));
    }

    @Test
    public void testInWithVarargs() {
        NamedProperty np = NamedProperty.of("status");
        In in = np.in("active", "pending", "approved");

        assertNotNull(in);
        assertEquals("status", in.propName());
        assertEquals(3, in.values().size());
    }

    @Test
    public void testInWithCollection() {
        NamedProperty np = NamedProperty.of("id");
        Set<Integer> ids = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));

        In in = np.in(ids);

        assertNotNull(in);
        assertEquals("id", in.propName());
        assertEquals(5, in.values().size());
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

        Or or = priority.equalsAny(1, 2, 3, 4, 5);

        assertEquals(5, or.conditions().size());
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

    // Removed: testBtDeprecatedMethod() - bt() method has been removed. Use between() instead.

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
    public void testComplexAnyEqualWithMixedTypes() {
        NamedProperty type = NamedProperty.of("type");

        Or or = type.equalsAny("A", "B", "C", "D");

        assertEquals(4, or.conditions().size());
    }

    @Test
    public void testAnyEqualWithSingleValue() {
        NamedProperty status = NamedProperty.of("status");

        Or or = status.equalsAny("active");

        assertNotNull(or);
        assertEquals(1, or.conditions().size());
    }

    @Test
    public void testAnyEqualWithEmptyCollection() {
        NamedProperty status = NamedProperty.of("status");

        assertThrows(IllegalArgumentException.class, () -> {
            status.equalsAny(Arrays.asList());
        });
    }

    @Test
    public void testInWithSingleValue() {
        NamedProperty id = NamedProperty.of("id");

        In in = id.in(1);

        assertNotNull(in);
        assertEquals("id", in.propName());
        assertEquals(1, in.values().size());
    }

    @Test
    public void testBetweenWithStrings() {
        NamedProperty code = NamedProperty.of("code");

        Between between = code.between("A", "Z");

        assertNotNull(between);
        assertEquals("code", between.propName());
        assertEquals("A", between.minValue());
        assertEquals("Z", between.maxValue());
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

    @Test
    public void testNotEqual() {
        final NamedProperty property = NamedProperty.of("status");
        final NotEqual condition = property.notEqual("deleted");

        assertEquals("status", condition.propName());
        assertEquals("deleted", condition.propValue());
    }

    @Test
    public void testGreaterThan() {
        final NamedProperty property = NamedProperty.of("age");
        final GreaterThan condition = property.greaterThan(18);

        assertEquals("age", condition.propName());
        assertEquals(Integer.valueOf(18), condition.propValue());
    }

    @Test
    public void testGreaterThanOrEqual() {
        final NamedProperty property = NamedProperty.of("score");
        final GreaterThanOrEqual condition = property.greaterThanOrEqual(60);

        assertEquals("score", condition.propName());
        assertEquals(Integer.valueOf(60), condition.propValue());
    }

    @Test
    public void testLessThan() {
        final NamedProperty property = NamedProperty.of("price");
        final LessThan condition = property.lessThan(100);

        assertEquals("price", condition.propName());
        assertEquals(Integer.valueOf(100), condition.propValue());
    }

    @Test
    public void testLessThanOrEqual() {
        final NamedProperty property = NamedProperty.of("quantity");
        final LessThanOrEqual condition = property.lessThanOrEqual(10);

        assertEquals("quantity", condition.propName());
        assertEquals(Integer.valueOf(10), condition.propValue());
    }

    @Test
    public void testNotBetween() {
        final NamedProperty property = NamedProperty.of("age");
        final NotBetween condition = property.notBetween(18, 65);

        assertEquals("age", condition.propName());
        assertEquals(Integer.valueOf(18), condition.minValue());
        assertEquals(Integer.valueOf(65), condition.maxValue());
    }

    @Test
    public void testNotStartsWith() {
        final NamedProperty property = NamedProperty.of("code");
        final NotLike condition = property.notStartsWith("TMP");

        assertEquals("code", condition.propName());
        assertEquals("TMP%", condition.propValue());
    }

    @Test
    public void testNotEndsWith() {
        final NamedProperty property = NamedProperty.of("email");
        final NotLike condition = property.notEndsWith("@temp.com");

        assertEquals("email", condition.propName());
        assertEquals("%@temp.com", condition.propValue());
    }

    @Test
    public void testNotContains() {
        final NamedProperty property = NamedProperty.of("description");
        final NotLike condition = property.notContains("draft");

        assertEquals("description", condition.propName());
        assertEquals("%draft%", condition.propValue());
    }

    @Test
    public void testAnyEqual_LongArray() {
        final NamedProperty property = NamedProperty.of("user_id");
        final Or condition = property.equalsAny(new long[] { 1001L, 1002L, 1003L });

        assertNotNull(condition);
        assertEquals(3, condition.conditions().size());
        assertEquals(Long.valueOf(1001L), ((Equal) condition.conditions().get(0)).propValue());
        assertEquals(Long.valueOf(1002L), ((Equal) condition.conditions().get(1)).propValue());
        assertEquals(Long.valueOf(1003L), ((Equal) condition.conditions().get(2)).propValue());
    }

    @Test
    public void testAnyEqual_LongArray_SingleElement() {
        final NamedProperty property = NamedProperty.of("id");
        final Or condition = property.equalsAny(new long[] { 42L });

        assertEquals(1, condition.conditions().size());
    }

    @Test
    public void testAnyEqual_LongArray_Empty() {
        final NamedProperty property = NamedProperty.of("id");

        assertThrows(IllegalArgumentException.class, () -> property.equalsAny(new long[0]));
    }

    @Test
    public void testAnyEqual_DoubleArray() {
        final NamedProperty property = NamedProperty.of("rate");
        final Or condition = property.equalsAny(new double[] { 1.5, 2.0, 2.5 });

        assertNotNull(condition);
        assertEquals(3, condition.conditions().size());
        assertEquals(Double.valueOf(1.5), ((Equal) condition.conditions().get(0)).propValue());
        assertEquals(Double.valueOf(2.0), ((Equal) condition.conditions().get(1)).propValue());
        assertEquals(Double.valueOf(2.5), ((Equal) condition.conditions().get(2)).propValue());
    }

    @Test
    public void testAnyEqual_DoubleArray_SingleElement() {
        final NamedProperty property = NamedProperty.of("score");
        final Or condition = property.equalsAny(new double[] { 99.9 });

        assertEquals(1, condition.conditions().size());
    }

    @Test
    public void testAnyEqual_DoubleArray_Empty() {
        final NamedProperty property = NamedProperty.of("score");

        assertThrows(IllegalArgumentException.class, () -> property.equalsAny(new double[0]));
    }

    @Test
    public void testIn_LongArray() {
        final NamedProperty property = NamedProperty.of("user_id");
        final In condition = property.in(new long[] { 1001L, 1002L, 1003L });

        assertNotNull(condition);
        assertEquals("user_id", condition.propName());
        assertEquals(3, condition.values().size());
    }

    @Test
    public void testIn_LongArray_SingleElement() {
        final NamedProperty property = NamedProperty.of("id");
        final In condition = property.in(new long[] { 42L });

        assertEquals(1, condition.values().size());
    }

    @Test
    public void testIn_DoubleArray() {
        final NamedProperty property = NamedProperty.of("rate");
        final In condition = property.in(new double[] { 1.5, 2.0, 2.5 });

        assertNotNull(condition);
        assertEquals("rate", condition.propName());
        assertEquals(3, condition.values().size());
    }

    @Test
    public void testIn_DoubleArray_SingleElement() {
        final NamedProperty property = NamedProperty.of("score");
        final In condition = property.in(new double[] { 99.9 });

        assertEquals(1, condition.values().size());
    }

    @Test
    public void testNotIn_ObjectVarargs() {
        final NamedProperty property = NamedProperty.of("status");
        final NotIn condition = property.notIn("deleted", "archived");

        assertNotNull(condition);
        assertEquals("status", condition.propName());
        assertEquals(2, condition.values().size());
    }

    @Test
    public void testNotIn_ObjectVarargs_SingleValue() {
        final NamedProperty property = NamedProperty.of("type");
        final NotIn condition = property.notIn("invalid");

        assertEquals(1, condition.values().size());
    }

    @Test
    public void testNotIn_LongArray() {
        final NamedProperty property = NamedProperty.of("user_id");
        final NotIn condition = property.notIn(new long[] { 999L, 1000L });

        assertNotNull(condition);
        assertEquals("user_id", condition.propName());
        assertEquals(2, condition.values().size());
    }

    @Test
    public void testNotIn_LongArray_SingleElement() {
        final NamedProperty property = NamedProperty.of("id");
        final NotIn condition = property.notIn(new long[] { 42L });

        assertEquals(1, condition.values().size());
    }

    @Test
    public void testNotIn_DoubleArray() {
        final NamedProperty property = NamedProperty.of("rate");
        final NotIn condition = property.notIn(new double[] { 0.0, -1.0 });

        assertNotNull(condition);
        assertEquals("rate", condition.propName());
        assertEquals(2, condition.values().size());
    }

    @Test
    public void testNotIn_DoubleArray_SingleElement() {
        final NamedProperty property = NamedProperty.of("score");
        final NotIn condition = property.notIn(new double[] { 0.0 });

        assertEquals(1, condition.values().size());
    }

    @Test
    public void testNotIn_Collection() {
        final NamedProperty property = NamedProperty.of("department");
        final NotIn condition = property.notIn(java.util.Arrays.asList("Temp", "Archived"));

        assertNotNull(condition);
        assertEquals("department", condition.propName());
        assertEquals(2, condition.values().size());
    }

    @Test
    public void testNotIn_Collection_SingleElement() {
        final NamedProperty property = NamedProperty.of("type");
        final NotIn condition = property.notIn(java.util.Collections.singletonList("invalid"));

        assertEquals(1, condition.values().size());
    }

    @Test
    public void testConstructor() {
        NamedProperty prop = new NamedProperty("testProperty");
        Assertions.assertEquals("testProperty", prop.toString());
    }

    @Test
    public void testConstructorWithNull() {
        // Constructor now rejects null with IllegalArgumentException, consistent with NamedProperty.of(null)
        // and the rest of the name-validation surface (previously threw NullPointerException).
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new NamedProperty(null);
        });
    }

    @Test
    public void testConstructorWithEmptyString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new NamedProperty("");
        });
    }

    @Test
    public void testOfMethodWithEmptyString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            NamedProperty.of("");
        });
    }

    @Test
    public void testOfMethodWithNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            NamedProperty.of(null);
        });
    }

    @Test
    public void testConstructorAndFactoryRejectBlankPropertyName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NamedProperty("   "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> NamedProperty.of("   "));
    }

    @Test
    public void testAnyEqualWithArray() {
        NamedProperty prop = NamedProperty.of("color");
        Or condition = prop.equalsAny("red", "green", "blue");

        Assertions.assertEquals(3, condition.conditions().size());
    }

    @Test
    public void testAnyEqualWithPrimitiveArray() {
        NamedProperty prop = NamedProperty.of("priority");
        Or condition = prop.equalsAny(new int[] { 1, 2, 3 });

        Assertions.assertEquals(3, condition.conditions().size());
        Assertions.assertEquals(Integer.valueOf(1), ((Equal) condition.conditions().get(0)).propValue());
    }

    @Test
    public void testAnyEqualWithEmptyArray() {
        NamedProperty prop = NamedProperty.of("city");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            prop.equalsAny();
        });
    }

    @Test
    public void testInWithArray() {
        NamedProperty prop = NamedProperty.of("status");
        In condition = prop.in("active", "pending", "approved");

        Assertions.assertEquals("status", condition.propName());
        Assertions.assertEquals(3, condition.values().size());
    }

    @Test
    public void testInWithPrimitiveArray() {
        NamedProperty prop = NamedProperty.of("id");
        In condition = prop.in(new int[] { 1, 2, 3 });

        Assertions.assertEquals(Arrays.asList(1, 2, 3), condition.parameters());
    }

    @Test
    public void testNotInWithPrimitiveArray() {
        NamedProperty prop = NamedProperty.of("id");
        NotIn condition = prop.notIn(new int[] { 4, 5, 6 });

        Assertions.assertEquals(Arrays.asList(4, 5, 6), condition.parameters());
    }

    // --- 2nd-pass review verification tests ---

    @Test
    public void test2ndPass_eachComparatorMapsToCorrectOperator() {
        // Verify NO copy/paste bug in operator mapping for any comparator method.
        NamedProperty p = NamedProperty.of("col");
        assertEquals(Operator.EQUAL, p.equal(1).operator());
        assertEquals(Operator.EQUAL, p.eq(1).operator());
        assertEquals(Operator.NOT_EQUAL, p.notEqual(1).operator());
        assertEquals(Operator.NOT_EQUAL, p.ne(1).operator());
        assertEquals(Operator.GREATER_THAN, p.greaterThan(1).operator());
        assertEquals(Operator.GREATER_THAN, p.gt(1).operator());
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, p.greaterThanOrEqual(1).operator());
        assertEquals(Operator.GREATER_THAN_OR_EQUAL, p.ge(1).operator());
        assertEquals(Operator.LESS_THAN, p.lessThan(1).operator());
        assertEquals(Operator.LESS_THAN, p.lt(1).operator());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, p.lessThanOrEqual(1).operator());
        assertEquals(Operator.LESS_THAN_OR_EQUAL, p.le(1).operator());
    }

    @Test
    public void test2ndPass_likeWildcardsAreCorrectlyPlaced() {
        NamedProperty p = NamedProperty.of("col");
        assertEquals("foo%", p.startsWith("foo").propValue());
        assertEquals("%foo", p.endsWith("foo").propValue());
        assertEquals("%foo%", p.contains("foo").propValue());
        assertEquals("foo%", p.notStartsWith("foo").propValue());
        assertEquals("%foo", p.notEndsWith("foo").propValue());
        assertEquals("%foo%", p.notContains("foo").propValue());
    }

    @Test
    public void test2ndPass_equalWithNull_rendersIsNull() {
        // CRITICAL: equal(null) MUST render as "col IS NULL" not "col = NULL"
        NamedProperty p = NamedProperty.of("col");
        Equal eq = p.equal(null);
        Assertions.assertTrue(eq.toString().contains("IS NULL"));
        Assertions.assertFalse(eq.toString().contains("= NULL"));
    }

    @Test
    public void test2ndPass_returnedConditionsAreFresh_notCached() {
        // After np.equal("x"), the returned Equal must be a fresh instance,
        // not a shared/cached one (otherwise a caller mutating it would break others).
        NamedProperty p = NamedProperty.of("col");
        Equal e1 = p.equal("x");
        Equal e2 = p.equal("x");
        assertNotSame(e1, e2, "Each equal() call must return a fresh Equal instance");
        // But they should be value-equal:
        assertEquals(e1, e2);
    }

    @Test
    public void test2ndPass_equalsAnyArrayWithNullValues_throwsIAE_orHandlesGracefully() {
        // equalsAny(null) - explicit null array
        NamedProperty p = NamedProperty.of("col");
        assertThrows(IllegalArgumentException.class, () -> p.equalsAny((Object[]) null));
    }

    @Test
    public void test2ndPass_inEmptyArrayThrowsViaAbstractInCheck() {
        // Filters.in -> new In which checks notEmpty
        NamedProperty p = NamedProperty.of("col");
        assertThrows(IllegalArgumentException.class, () -> p.in(new int[] {}));
        assertThrows(IllegalArgumentException.class, () -> p.in(new long[] {}));
        assertThrows(IllegalArgumentException.class, () -> p.in(new double[] {}));
        assertThrows(IllegalArgumentException.class, () -> p.in(java.util.Collections.emptyList()));
    }

    @Test
    public void test2ndPass_betweenRangeOrderPreserved() {
        // NamedProperty.between(min, max) must forward min and max in that order.
        NamedProperty p = NamedProperty.of("age");
        Between b = p.between(18, 65);
        assertEquals(Integer.valueOf(18), b.minValue());
        assertEquals(Integer.valueOf(65), b.maxValue());
    }

    @Test
    public void test2ndPass_constructorBypassesCache_butEqualityHolds() {
        // public constructor bypasses cache; pool only used through of().
        NamedProperty viaCtor = new NamedProperty("uncached_prop_xyz");
        NamedProperty viaOf = NamedProperty.of("uncached_prop_xyz");
        // Not necessarily same instance (one constructed directly)
        // But should still be equal (value-equality).
        assertEquals(viaCtor, viaOf);
        assertEquals(viaCtor.hashCode(), viaOf.hashCode());
    }

    @Test
    public void test2ndPass_cacheKeysCaseSensitive() {
        // Cache MUST treat property names case-sensitively (avoid wrong matching).
        NamedProperty lower = NamedProperty.of("CaseTest_zzz");
        NamedProperty upper = NamedProperty.of("casetest_zzz");
        // They should be distinct instances and not equal.
        assertNotSame(lower, upper);
        Assertions.assertNotEquals(lower, upper);
    }

    @Test
    public void test2ndPass_cacheKeysWhitespaceSensitive() {
        // " foo" vs "foo" must NOT match in the cache.
        NamedProperty noSpace = NamedProperty.of("zzz_uniq");
        NamedProperty leadSpace = NamedProperty.of(" zzz_uniq");
        assertNotSame(noSpace, leadSpace);
        Assertions.assertNotEquals(noSpace, leadSpace);
    }

    @Test
    public void testNullEmptySymmetry() {
        assertEquals(Filters.isNullOrEmpty("name"), NamedProperty.of("name").isNullOrEmpty());
        assertEquals(Filters.isNotNullAndNotEmpty("name"), NamedProperty.of("name").isNotNullAndNotEmpty());
    }

    @Test
    public void testEqualsAnyRejectsCollectionThatBecomesEmptyDuringIteration() {
        Collection<Object> unstableValues = new AbstractCollection<>() {
            @Override
            public Iterator<Object> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> NamedProperty.of("status").equalsAny(unstableValues));
    }
}
