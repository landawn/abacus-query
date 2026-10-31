package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.NamingPolicy;

class QueryBuilderCompositionTuningTest {
    private static final class CountingSet extends HashSet<String> {
        long lookups;

        CountingSet(final Set<String> values) {
            super(values);
        }

        @Override
        public boolean contains(final Object value) {
            lookups++;
            return super.contains(value);
        }
    }

    private static Condition denseParent(final int n) {
        final List<Condition> conditions = new ArrayList<>();
        for (int i = 2; i <= 2 * n; i++) {
            conditions.add(Filters.eq("id_" + i, i));
        }
        return Filters.and(conditions);
    }

    private static int projectionEnd(final SqlBuilder builder) throws ReflectiveOperationException {
        final Method method = AbstractQueryBuilder.class.getDeclaredMethod("firstOperandProjectionEnd");
        method.setAccessible(true);
        return (int) method.invoke(builder);
    }

    private static SqlBuilder rawProjection(final Dsl dsl, final String sql) {
        final SqlBuilder builder = dsl.select("id").from("t").union("SELECT id FROM u");
        builder._sb.setLength(0);
        builder._sb.append(sql);
        return builder;
    }

    private static Condition repeated(final int n) {
        final List<Condition> conditions = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            conditions.add(Filters.eq("id", i));
        }
        return Filters.and(conditions);
    }

    private static Set<?> resultLabels(final SqlBuilder builder) throws ReflectiveOperationException {
        final Method method = AbstractQueryBuilder.class.getDeclaredMethod("firstOperandResultColumns");
        method.setAccessible(true);
        final Map<?, ?> result = (Map<?, ?>) method.invoke(builder);
        return result == null ? Set.of() : result.keySet();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCollisionIntervalsAvoidRepeatedSearchesAndKeepExactNames() throws ReflectiveOperationException {
        final int n = 128;
        for (final SqlPolicy policy : List.of(SqlPolicy.NAMED_SQL, SqlPolicy.IBATIS_SQL)) {
            final Dsl dsl = Dsl.forDialect(Dsl.NSB.sqlDialect().toBuilder().sqlPolicy(policy).build());
            final SqlBuilder parent = dsl.select("id").from("t").where(denseParent(n));
            final Field field = AbstractQueryBuilder.class.getDeclaredField("_generatedNamedParameterNames");
            field.setAccessible(true);
            final CountingSet names = new CountingSet((Set<String>) field.get(parent));
            field.set(parent, names);
            final SP result = parent.union(dsl.select("id").from("u").where(repeated(n))).build();
            final List<String> expectedNames = new ArrayList<>();
            final List<Integer> expectedValues = new ArrayList<>();
            for (int i = 2; i <= 2 * n; i++) {
                expectedNames.add("id_" + i);
                expectedValues.add(i);
            }
            for (int i = 1; i <= n; i++) {
                expectedNames.add(i == 1 ? "id" : "id_" + (3 * n + 1 - i));
                expectedValues.add(i - 1);
            }
            assertEquals(expectedNames, ParsedSql.parse(result.query(), null).namedParameters());
            assertEquals(expectedValues, result.parameters());
            assertTrue(names.lookups < 16L * n, "Repeated occupied-range scans: " + names.lookups);
        }
    }

    @Test
    void testCompoundOrderByKeepsLabelsAfterDistinctFromOperators() throws ReflectiveOperationException {
        final Dsl pg = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        for (final Dsl dsl : List.of(Dsl.PSC, pg)) {
            for (final String expression : List.of("a IS DISTINCT FROM b", "a IS NOT DISTINCT FROM b", "a is distinct from b", "a is not distinct from b",
                    "(a + 1) IS DISTINCT FROM (SELECT b FROM u)", "CASE WHEN a IS DISTINCT FROM b THEN 1 ELSE 0 END",
                    "a IS DISTINCT FROM b IS NOT DISTINCT FROM c")) {
                final Map<String, String> columns = new LinkedHashMap<>();
                columns.put(expression, "resultFlag");
                columns.put("firstName", "firstName");
                final SqlBuilder builder = dsl.select(columns).from("t").where(Filters.eq("active", 42)).union("SELECT false, first_name FROM u");
                assertEquals(builder._sb.length(), projectionEnd(builder), expression);
                assertEquals(Set.of("resultFlag", "firstName"), resultLabels(builder), expression);
                final SP result = builder.orderBy("resultFlag", "firstName").build();
                assertTrue(result.query().endsWith(" ORDER BY \"resultFlag\", \"firstName\""), result.query());
                assertEquals(List.of(42), result.parameters());
            }
        }
    }

    @Test
    void testCustomHandlerOrderAndFailureRetryRemainUnchanged() {
        final List<String> calls = new ArrayList<>();
        final AtomicBoolean fail = new AtomicBoolean(false);
        final Dsl dsl = Dsl.forDialect(Dsl.NSB.sqlDialect().toBuilder().namedParameterHandler((sql, name) -> {
            calls.add(name);
            if (fail.get() && name.equals("id_35")) {
                throw new IllegalArgumentException("planned handler failure");
            }
            sql.append('@').append(name);
        }).build());
        final SqlBuilder parent = dsl.select("id").from("t").where(denseParent(16));
        calls.clear();
        fail.set(true);
        assertThrows(IllegalArgumentException.class, () -> parent.union(Dsl.NSB.select("id").from("u").where(repeated(16))));
        assertEquals(List.of("id_33", "id_34", "id_35"), calls);
        calls.clear();
        fail.set(false);
        final SP result = parent.union(Dsl.NSB.select("id").from("u").where(repeated(16))).build();
        final List<String> expectedCalls = new ArrayList<>();
        for (int i = 33; i <= 47; i++) {
            expectedCalls.add("id_" + i);
        }
        expectedCalls.add("id");
        assertEquals(expectedCalls, calls);
        assertEquals(47, result.parameters().size());
        assertTrue(result.query().contains("id = @id"));
    }

    @Test
    void testDefaultProjectionBoundExcludesLargeTail() throws ReflectiveOperationException {
        final String tail = "x".repeat(65536);
        final SqlBuilder builder = Dsl.PSB.select(Map.of("coalesce(id, 0)", "resultId")).from("t").where("payload = '" + tail + "'").union("SELECT id FROM u");
        assertTrue(projectionEnd(builder) < 100);
        assertTrue(builder.orderBy("resultId").build().query().endsWith(" ORDER BY \"resultId\""));
    }

    @Test
    void testDistinctFromTokenBoundariesAndRetainedComments() throws ReflectiveOperationException {
        final List<Dsl> dsls = new ArrayList<>(List.of(Dsl.PSB));
        for (final String separator : List.of(".", ". ", " . ", ". \u001c", "::", "IS NOT", "IS DISTINCT", "NOT DISTINCT", "IS NOT DISTINCT", "IS \t NOT")) {
            dsls.add(Dsl.forDialect(
                    Dsl.PSB.sqlDialect().toBuilder().tokenizerConfig(SqlParser.TokenizerConfig.builder().withSeparator(separator).build()).build()));
        }

        for (final Dsl dsl : dsls) {
            for (final String operator : List.of("IS DISTINCT FROM", "IS NOT DISTINCT FROM", "Is NoT DiStInCt FrOm", "IS \t NOT DISTINCT FROM",
                    "IS /*one*/ NOT /*two*/ DISTINCT /*three*/ FROM", "IS/*one*/NOT /*two*/ DISTINCT/*three*/FROM", "IS -- comment\n DISTINCT FROM")) {
                final SqlBuilder builder = rawProjection(dsl, "SELECT-- Keep comments\na " + operator + " b AS result, y AS later FROM t");
                try {
                    assertEquals(Set.of("result", "later"), resultLabels(builder), operator);
                } finally {
                    builder.build();
                }
            }

            for (final String prefix : List.of("\"IS\" DISTINCT", "IS \"NOT\" DISTINCT", "IS \"DISTINCT\"", "q.IS DISTINCT", "q. IS DISTINCT",
                    "q. \u001c IS DISTINCT", "q . /*kept*/ IS DISTINCT", "IS (NOT) DISTINCT", "IS NOT, DISTINCT", "IS NOT + DISTINCT", "IS\u000bDISTINCT",
                    "IS\u001cDISTINCT")) {
                final SqlBuilder builder = rawProjection(dsl, "SELECT-- Keep comments\nid AS head, " + prefix + " FROM t AS late, y AS after FROM u");
                try {
                    assertEquals(Set.of("head"), resultLabels(builder), prefix);
                } finally {
                    builder.build();
                }
            }
        }

        for (final char blank : new char[] { '\u000b', '\u001c', '\u001d', '\u001e', '\u001f' }) {
            final SqlBuilder builder = rawProjection(Dsl.PSB, "SELECT a IS NOT DISTINCT " + blank + " FROM b AS result, y AS later FROM t");
            try {
                assertEquals(Set.of("result", "later"), resultLabels(builder));
            } finally {
                builder.build();
            }
        }

        final SqlBuilder commented = rawProjection(Dsl.PSB,
                "SELECT-- Keep comments\na IS /*kept*/ DISTINCT FROM b AS result, id AS /*kept*/ hidden, y AS later FROM t");
        try {
            assertEquals(Set.of("result", "later"), resultLabels(commented));
        } finally {
            commented.build();
        }
    }

    @Test
    void testFallbackProjectionPreservesCustomAndAmbiguousPrefixes() throws ReflectiveOperationException {
        final Dsl custom = Dsl
                .forDialect(Dsl.PSB.sqlDialect().toBuilder().tokenizerConfig(SqlParser.tokenizerConfigBuilder().withSeparator(" FROM ").build()).build());
        final SqlBuilder configured = custom.select(Map.of("id", "resultId")).from("t").union("SELECT id FROM u");
        assertEquals(configured._sb.length(), projectionEnd(configured));
        configured.build();

        for (final String prefix : List.of("$$ FROM $$ AS x", "'\\' AS x", "[field] AS x")) {
            final SqlBuilder builder = Dsl.PSB.select(prefix, "id").from("t").union("SELECT id, id FROM u");
            assertEquals(builder._sb.length(), projectionEnd(builder), prefix);
            builder.build();
        }
        final SqlBuilder commented = Dsl.PSB.select("id").selectModifier("/* kept */").from("t").union("SELECT id FROM u");
        assertEquals(commented._sb.length(), projectionEnd(commented));
        commented.build();
    }

    @Test
    void testFreedLowerSuffixesRemainAvailable() {
        final SP result = Dsl.NSB.select("id").from("t").where(Filters.eq("id", -1)).union(Dsl.NSB.select("id").from("u").where(repeated(32))).build();
        final List<String> expectedNames = new ArrayList<>(List.of("id"));
        for (int i = 2; i <= 33; i++) {
            expectedNames.add("id_" + i);
        }
        assertEquals(expectedNames, ParsedSql.parse(result.query(), null).namedParameters());
    }

    @Test
    void testLowercaseFromBoundsIndependentProjection() throws ReflectiveOperationException {
        for (final String from : List.of("FROM", "from", "FrOm", "fRoM")) {
            final String prefix = "SELECT coalesce(id, 0) AS \"result\" " + from;
            final String sql = prefix + " t WHERE payload = '" + "x".repeat(65536) + "'";
            final SqlBuilder builder = rawProjection(Dsl.PSB, sql);
            try {
                assertEquals(prefix.length(), projectionEnd(builder));
                assertEquals(Set.of("result"), resultLabels(builder));
            } finally {
                builder.build();
            }
        }

        for (final String sql : List.of("SELECT " + "x".repeat(65536), "SELECT xfrom AS result", "SELECT \"from\" AS result", "SELECT (id from t) AS result",
                "SELECT id AS result from_table", "SELECT id AS result FROM")) {
            final SqlBuilder builder = rawProjection(Dsl.PSB, sql);
            try {
                assertEquals(sql.length(), projectionEnd(builder));
            } finally {
                builder.build();
            }
        }
    }

    @Test
    void testOverflowFallsBackWithoutChangingNames() {
        final List<Condition> parentConditions = new ArrayList<>(List.of(Filters.eq("id", -1)));
        for (int i = 12; i >= 0; i--) {
            parentConditions.add(Filters.eq("id_" + (Integer.MAX_VALUE - i), i));
        }
        final SqlBuilder parent = Dsl.NSB.select("id").from("t").where(Filters.and(parentConditions));
        parent._namedParameterNameOccurrences.put("id", Integer.MAX_VALUE - 15);
        final SP result = parent.union(Dsl.NSB.select("id").from("u").where(repeated(3))).build();
        assertTrue(result.query().contains(":id_" + (Integer.MAX_VALUE - 14)));
        assertTrue(result.query().contains(":id_" + (Integer.MAX_VALUE - 13)));
        assertTrue(result.query().contains(":id_" + Integer.MIN_VALUE));
        assertEquals(17, result.parameters().size());
    }

    @Test
    void testRollbackAfterSnapshotRenamingLeavesNoCachedState() {
        final SubQuery snapshot = Dsl.NSB.select("id").from("u").where(repeated(32)).toSubQuery();
        final Condition valid = Filters.and(denseParent(32), Filters.exists(snapshot), Filters.exists(snapshot));
        final Condition unsupported = new Condition() {
            @Override
            public Operator operator() {
                return Operator.EQUAL;
            }

            @Override
            public ImmutableList<Object> parameters() {
                return ImmutableList.empty();
            }

            @Override
            public String toSql(final NamingPolicy namingPolicy) {
                throw new AssertionError("Unsupported dispatch");
            }
        };
        final SqlBuilder retry = Dsl.NSB.select("id").from("t");
        assertThrows(IllegalArgumentException.class, () -> retry.where(Filters.and(valid, unsupported)));
        final SP retried = retry.where(valid).build();
        final SP fresh = Dsl.NSB.select("id").from("t").where(valid).build();
        assertEquals(fresh.query(), retried.query());
        assertEquals(fresh.parameters(), retried.parameters());
    }
}
