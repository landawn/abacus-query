package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

public class OnTest extends TestBase {

    @Test
    void testCommentOnlyRightPropNameMessageNamesRightPropName() {
        // Regression: the comment-only check was left to Equal, whose message named its internal 'propValue' parameter.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new On("a.id", "/* x */"));
        assertTrue(ex.getMessage().contains("rightPropName"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new On(Map.of("a.id", "-- x")));
        assertTrue(ex.getMessage().contains("rightPropName"), ex.getMessage());

        assertEquals("ON a.id = b.id", new On("a.id", "b.id").toString());
    }

    @Test
    void testCommentOnlyOrUnterminatedBlockCommentPropNamesAreRejectedOnBothSides() {
        // Regression: only rightPropName was checked for comment-only text, and neither side for an unterminated block
        // comment, so On("-- x", "b") rendered a one-sided "ON -- x <newline> = b" and On("a /* c", "b") commented out the predicate.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new On("-- x", "b.id"));
        assertEquals("leftPropName must not consist only of SQL comments: -- x", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new On("/* c */", "b.id"));
        assertTrue(ex.getMessage().contains("leftPropName"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new On("a.id /* c", "b.id"));
        assertEquals("leftPropName must not contain an unterminated block comment: a.id /* c", ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> new On("a.id", "b.id /* c"));
        assertEquals("rightPropName must not contain an unterminated block comment: b.id /* c", ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> new On(Map.of("-- x", "b.id")));
        assertThrows(IllegalArgumentException.class, () -> new On(Map.of("a.id", "b.id /* c")));
        assertThrows(IllegalArgumentException.class, () -> Filters.on("a.id /* c", "b.id"));
        assertThrows(IllegalArgumentException.class, () -> Filters.on(Map.of("a.id /* c", "b.id")));

        // Existing right-side rejections are unchanged; terminated comments and a SQL Server #temp reference are kept.
        ex = assertThrows(IllegalArgumentException.class, () -> new On("a.id", "-- x"));
        assertEquals("rightPropName must not consist only of SQL comments: -- x", ex.getMessage());
        assertEquals("ON a.id /* c */ = b.id", new On("a.id /* c */", "b.id").toString());
        assertEquals("ON #tmp.id = b.id", new On("#tmp.id", "b.id").toString());
    }

    @Test
    void testCreateOnConditionRejectsNullMapAndNullEntries() {
        assertThrows(IllegalArgumentException.class, () -> On.createOnCondition((Map<String, String>) null));
        final Map<String, String> invalid = new AbstractMap<>() {
            @Override
            public Set<Entry<String, String>> entrySet() {
                return java.util.Collections.singleton(null);
            }
        };
        final var error = assertThrows(IllegalArgumentException.class, () -> new On(invalid));
        assertEquals("propNamePairs must not contain a null entry", error.getMessage());
    }

    @Test
    void testMySqlDashReadingOfUnterminatedBlockCommentPropNames() {
        // Regression: MySQL reads "a --/*" as "a - -" plus an open block comment (it needs whitespace after "--"); both
        // sides were accepted and rendered "ON a --/* <newline> = b".
        assertEquals("leftPropName must not contain an unterminated block comment: a --/*",
                assertThrows(IllegalArgumentException.class, () -> new On("a --/*", "b")).getMessage());
        assertEquals("rightPropName must not contain an unterminated block comment: b --/*",
                assertThrows(IllegalArgumentException.class, () -> new On("a", "b --/*")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> new On(Map.of("a --x /* y", "b")));

        // With whitespace after "--" every dialect reads a line comment, which the renderer terminates.
        assertEquals("ON a -- /* x\n = b", new On("a -- /* x", "b").toString());
    }

    @Test
    void testCommentCounterExamplesOnBothSides() {
        // A terminated comment on the right side is stripped by the SqlExpression rendering.
        assertEquals("ON a.id = b.id ", new On("a.id", "b.id /* c */").toString());

        // Quoted identifiers, a #temp reference and optimizer hints are not unterminated comments.
        assertEquals("ON \"a/*\" = b.id", new On("\"a/*\"", "b.id").toString());
        assertEquals("ON `a/*` = b.id", new On("`a/*`", "b.id").toString());
        assertEquals("ON a = \"b/*\"", new On("a", "\"b/*\"").toString());
        assertEquals("ON #temp.id /* x */ = b", new On("#temp.id /* x */", "b").toString());
        assertEquals("ON /*+ h */ a = b", new On("/*+ h */ a", "b").toString());

        // Nested comments (PostgreSQL, SQL Server): a balanced nest is closed, an inner opener alone is not.
        assertEquals("ON /* /* */ */ a = b", new On("/* /* */ */ a", "b").toString());
        assertThrows(IllegalArgumentException.class, () -> new On("/* /* */ a", "b"));
        assertThrows(IllegalArgumentException.class, () -> new On("a", "b /* /* */"));

        // With both sides invalid, leftPropName is reported first.
        assertEquals("leftPropName must not consist only of SQL comments: -- x",
                assertThrows(IllegalArgumentException.class, () -> new On("-- x", "/* c */")).getMessage());
    }

    @Test
    void testComplexCondition() {
        And complexCondition = new And(new Equal("orders.customer_id", "customers.id"), new GreaterThan("orders.order_date", "customers.registration_date"));
        On on = new On(complexCondition);
        assertNotNull(on.condition());
    }

    @Test
    void testCompositeKey() {
        Map<String, String> compositeKey = new LinkedHashMap<>();
        compositeKey.put("order_items.order_id", "orders.id");
        compositeKey.put("order_items.customer_id", "orders.customer_id");
        On on = new On(compositeKey);
        assertNotNull(on.condition());
    }

    @Test
    void testConstructor_SimpleEquality() {
        On on = new On("orders.customer_id", "customers.id");
        assertNotNull(on);
        assertEquals(Operator.ON, on.operator());
    }

    @Test
    void testConstructor_WithCondition() {
        Equal condition = new Equal("a.id", "b.a_id");
        On on = new On(condition);
        assertNotNull(on);
        assertNotNull(on.condition());
        assertEquals(Operator.ON, on.operator());
    }

    @Test
    void testConstructor_WithMap() {
        Map<String, String> joinMap = new LinkedHashMap<>();
        joinMap.put("emp.department_id", "dept.id");
        joinMap.put("emp.location_id", "dept.location_id");
        On on = new On(joinMap);
        assertNotNull(on);
        assertEquals(Operator.ON, on.operator());
    }

    @Test
    void testConstructorRejectsBlankPropertyNames() {
        assertThrows(IllegalArgumentException.class, () -> Filters.on("   ", "orders.user_id"));
        assertThrows(IllegalArgumentException.class, () -> Filters.on("users.id", "   "));
    }

    @Test
    void testConstructorRejectsClauseAndConnectorConditions() {
        assertThrows(IllegalArgumentException.class, () -> Filters.on(Filters.where(Filters.eq("active", true))));
        assertThrows(IllegalArgumentException.class, () -> Filters.on(Filters.using("id")));
        assertThrows(IllegalArgumentException.class, () -> Filters.on(Filters.expr("ON users.id = orders.user_id")));
        assertThrows(IllegalArgumentException.class, () -> Filters.on(Filters.expr("USING (id)")));
        assertThrows(IllegalArgumentException.class, () -> Filters.on(Filters.expr("   ")));
    }

    @Test
    void testConstructorRejectsNestedConnectorCondition() {
        Condition nestedOn = new Junction(Operator.AND, java.util.Arrays.asList(Filters.on("a.id", "b.id"), Filters.eq("b.active", true)), true);

        assertThrows(IllegalArgumentException.class, () -> new On(nestedOn));
    }

    @Test
    void testConstructorRejectsQuantifiedSubQueryOperand() {
        // ANY/ALL/SOME are quantified-subquery operands (valid only on the RHS of a comparison);
        // "ON ANY (SELECT ...)" is invalid SQL, so On must reject them like Clause does.
        SubQuery sub = Filters.subQuery("SELECT id FROM users");

        assertThrows(IllegalArgumentException.class, () -> new On(Filters.any(sub)));
        assertThrows(IllegalArgumentException.class, () -> new On(Filters.all(sub)));
        assertThrows(IllegalArgumentException.class, () -> new On(Filters.some(sub)));
        assertThrows(IllegalArgumentException.class, () -> Filters.on(Filters.any(sub)));
    }

    @Test
    void testConstructorRejectsWrappedNonPredicateComponent() {
        assertThrows(IllegalArgumentException.class, () -> new On(new TestComposableWrapper(new Where(Filters.eq("active", true)))));
        assertThrows(IllegalArgumentException.class, () -> new On(new TestComposableWrapper(Filters.any(new SubQuery("SELECT id FROM users")))));
    }

    @Test
    void testConstructorWithComplexCondition() {
        And complexCondition = Filters.and(Filters.eq("a.id", Filters.expr("b.a_id")), Filters.gt("b.created", "2024-01-01"));
        On on = Filters.on(complexCondition);

        assertEquals(complexCondition, on.condition());
    }

    @Test
    void testConstructorWithCondition() {
        Condition joinCondition = Filters.eq("t1.id", Filters.expr("t2.user_id"));
        On on = Filters.on(joinCondition);

        assertNotNull(on);
        assertEquals(Operator.ON, on.operator());
        assertEquals(joinCondition, on.condition());
    }

    @Test
    void testConstructorWithEmptyMap() {
        Map<String, String> emptyMap = new LinkedHashMap<>();

        // Empty map should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> Filters.on(emptyMap));
    }

    @Test
    void testConstructorWithMap() {
        Map<String, String> joinConditions = new LinkedHashMap<>();
        joinConditions.put("orders.customer_id", "customers.id");
        joinConditions.put("orders.store_id", "customers.preferred_store_id");

        On on = Filters.on(joinConditions);

        assertEquals(Operator.ON, on.operator());
        assertNotNull(on.condition());
    }

    @Test
    void testConstructorWithSingleEntryMap() {
        Map<String, String> joinCondition = new LinkedHashMap<>();
        joinCondition.put("products.category_id", "categories.id");

        On on = Filters.on(joinCondition);

        // Should create a single Equal condition
        assertTrue(on.condition() instanceof Equal);
    }

    @Test
    void testConstructorWithTableAliases() {
        On on = Filters.on("u.department_id", "d.id");

        Equal condition = (Equal) on.condition();
        assertEquals("u.department_id", condition.propName());
        // The second property becomes an expression
        assertTrue(condition.propValue() instanceof SqlExpression);
    }

    @Test
    void testConstructorWithTwoProperties() {
        On on = Filters.on("users.id", "posts.user_id");

        assertEquals(Operator.ON, on.operator());
        assertNotNull(on.condition());
        assertTrue(on.condition() instanceof Equal);
    }

    @Test
    void testCreateOnCondition_Map_MultipleEntries() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "b.id");
        map.put("a.code", "b.code");
        Condition condition = On.createOnCondition(map);
        assertNotNull(condition);
        assertTrue(condition instanceof And);
    }

    @Test
    void testCreateOnCondition_Map_SingleEntry() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "b.id");
        Condition condition = On.createOnCondition(map);
        assertNotNull(condition);
        assertTrue(condition instanceof Equal);
    }

    @Test
    void testCreateOnCondition_Map_ThrowsOnEmptyRightValue() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "");
        assertThrows(IllegalArgumentException.class, () -> On.createOnCondition(map));
    }

    @Test
    void testCreateOnCondition_Simple() {
        Condition condition = On.createOnCondition("users.id", "posts.user_id");
        assertNotNull(condition);
        assertTrue(condition instanceof Equal);
    }

    @Test
    void testCreateOnCondition_ThrowsOnEmptyRightPropName() {
        assertThrows(IllegalArgumentException.class, () -> On.createOnCondition("users.id", ""));
    }

    @Test
    void testCreateOnConditionSnapshotsEveryEntryWhenReportedSizeIsStale() {
        final Map<String, String> liveMap = new AbstractMap<>() {
            @Override
            public Set<Entry<String, String>> entrySet() {
                return new LinkedHashSet<>(
                        Arrays.asList(new AbstractMap.SimpleImmutableEntry<>("a.id", "b.id"), new AbstractMap.SimpleImmutableEntry<>("a.tenant", "b.tenant")));
            }

            @Override
            public int size() {
                // Models a concurrent/live map that gained an entry after size() was observed.
                return 1;
            }
        };

        final On on = new On(liveMap);

        assertEquals("ON ((a.id = b.id) AND (a.tenant = b.tenant))", on.toString());
    }

    @Test
    void testCreateOnConditionWithMap() {
        Map<String, String> joinPairs = new LinkedHashMap<>();
        joinPairs.put("a.id", "b.a_id");
        joinPairs.put("a.type", "b.type");

        Condition condition = On.createOnCondition(joinPairs);

        assertTrue(condition instanceof And);
        And and = (And) condition;
        assertEquals(2, and.conditions().size());
    }

    @Test
    void testCreateOnConditionWithSingleEntryMap() {
        Map<String, String> singlePair = new LinkedHashMap<>();
        singlePair.put("users.id", "profiles.user_id");

        Condition condition = On.createOnCondition(singlePair);

        assertTrue(condition instanceof Equal);
    }

    @Test
    void testCreateOnConditionWithTwoStrings() {
        Condition condition = On.createOnCondition("table1.col1", "table2.col2");

        assertTrue(condition instanceof Equal);
        Equal equal = (Equal) condition;
        assertEquals("table1.col1", equal.propName());
        assertTrue(equal.propValue() instanceof SqlExpression);
    }

    @Test
    void testEmptyJunctionRendersBooleanIdentity() {
        // An initialized empty junction is a complete predicate, so ON accepts it and renders its identity.
        assertEquals("ON 1 = 1", new On(Filters.and()).toString());
        assertEquals("ON 1 = 0", Filters.on(Filters.or()).toString());
        assertTrue(new On(Filters.and()).parameters().isEmpty());

        assertThrows(IllegalArgumentException.class, () -> new On(Filters.expr("  ")));
        assertThrows(IllegalArgumentException.class, () -> new On(new And()));
    }

    @Test
    void testEquals() {
        On on1 = Filters.on("a.id", "b.a_id");
        On on2 = Filters.on("a.id", "b.a_id");
        On on3 = Filters.on("x.id", "y.x_id");

        Map<String, String> map = new LinkedHashMap<>();
        map.put("a.id", "b.a_id");
        On on4 = Filters.on(map);

        assertTrue(on1.equals(on1));
        assertTrue(on1.equals(on2));
        assertFalse(on1.equals(on3));
        assertTrue(on1.equals(on4)); // Same condition, different construction
        assertFalse(on1.equals(null));
        assertFalse(on1.equals("not an On"));
    }

    @Test
    void testEquals_DifferentColumns() {
        On on1 = new On("a.id", "b.id");
        On on2 = new On("c.id", "d.id");
        assertNotEquals(on1, on2);
    }

    @Test
    void testEquals_EqualObjects() {
        On on1 = new On("a.id", "b.id");
        On on2 = new On("a.id", "b.id");
        assertEquals(on1, on2);
    }

    @Test
    void testEquals_Null() {
        On on = new On("a.id", "b.id");
        assertNotEquals(null, on);
    }

    @Test
    void testEquals_SameObject() {
        On on = new On("a.id", "b.id");
        assertEquals(on, on);
    }

    @Test
    void testGetCondition() {
        Equal condition = new Equal("users.id", "posts.user_id");
        On on = new On(condition);
        Condition retrieved = on.condition();
        assertNotNull(retrieved);
    }

    @Test
    void testGetOperator() {
        On on = Filters.on("a.id", "b.a_id");

        assertEquals(Operator.ON, on.operator());
    }

    @Test
    void testHashCode() {
        On on1 = new On("a.id", "b.id");
        On on2 = new On("a.id", "b.id");
        assertEquals(on1.hashCode(), on2.hashCode());
    }

    @Test
    void testHashTempTableRightPropNameIsAcceptedAndCommentOnlyFiltersOnIsRejected() {
        // Covers the SQL Server #temp carve-out of the comment-only rightPropName check and the Filters.on entry points.
        assertNotNull(new On("a.id", "#tmp.id"));
        assertNotNull(new On(Map.of("a.id", "#tmp.id")));

        final com.landawn.abacus.query.Dsl sqlServer = com.landawn.abacus.query.Dsl.forDialect(com.landawn.abacus.query.SqlDialect.builder()
                .productInfo(com.landawn.abacus.query.SqlDialect.ProductInfo.of("Microsoft SQL Server"))
                .namingPolicy(NamingPolicy.SNAKE_CASE)
                .sqlPolicy(com.landawn.abacus.query.SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
                .build());
        assertEquals("SELECT * FROM a JOIN #tmp ON (a.id = #tmp.id)",
                sqlServer.select("*").from("a").join("#tmp").on(new On("a.id", "#tmp.id")).build().query());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Filters.on("a.id", "/* x */"));
        assertTrue(ex.getMessage().contains("rightPropName"), ex.getMessage());

        ex = assertThrows(IllegalArgumentException.class, () -> Filters.on("a.id", "# x"));
        assertTrue(ex.getMessage().contains("rightPropName"), ex.getMessage());
    }

    @Test
    void testNullConditionMessageNamesCondition() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new On((Condition) null));
        assertTrue(ex.getMessage().contains("condition"), ex.getMessage());
    }

    @Test
    void testParameters_Empty() {
        On on = new On("a.id", "b.id");
        List<Object> params = on.parameters();
        assertNotNull(params);
    }

    @Test
    void testParameters_WithValues() {
        And complexCondition = new And(new Equal("orders.customer_id", "customers.id"), new Equal("orders.status", "active"));
        On on = new On(complexCondition);
        List<Object> params = on.parameters();
        assertEquals(2, params.size());
        assertEquals("customers.id", params.get(0));
        assertEquals("active", params.get(1));
    }

    @Test
    void testPracticalExample1() {
        // Simple column equality
        On on = Filters.on("employees.department_id", "departments.id");

        // Would be used like: JOIN departments ON employees.department_id = departments.id
        String result = on.toString();
        assertTrue(result.contains("employees.department_id"));
    }

    @Test
    void testPracticalExample2() {
        // Multiple join conditions
        Map<String, String> joinMap = new LinkedHashMap<>();
        joinMap.put("orders.customer_id", "customers.id");
        joinMap.put("orders.region", "customers.region");

        On on = Filters.on(joinMap);

        // Would create: ON orders.customer_id = customers.id AND orders.region = customers.region
        assertTrue(on.condition() instanceof And);
    }

    @Test
    void testPracticalExample3() {
        // Complex join with additional conditions
        And complexJoin = Filters.and(Filters.eq("products.category_id", Filters.expr("categories.id")), Filters.eq("products.active", true),
                Filters.gt("products.stock", 0));

        On on = Filters.on(complexJoin);

        assertEquals(complexJoin, on.condition());
        assertEquals(2, on.condition().parameters().size());
    }

    @Test
    void testToString() {
        On on = new On("orders.customer_id", "customers.id");
        String result = on.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("ON"));
    }

    @Test
    void testWithAdditionalFilters() {
        And filteredJoin = new And(new Equal("products.category_id", "categories.id"), new Equal("categories.active", true));
        On on = new On(filteredJoin);
        assertEquals(2, on.parameters().size());
    }

    private static final class TestComposableWrapper extends ComposableCell {
        TestComposableWrapper(final Condition condition) {
            super(Operator.NOT, condition);
        }
    }
}
