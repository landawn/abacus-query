package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.SqlDialect.IdentifierQuote;
import static com.landawn.abacus.query.Dsl.*;
import com.landawn.abacus.query.condition.Clause;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SqlExpression;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Union;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Throwables;

@Tag("2025")
public class AbstractQueryBuilderTest extends TestBase {

    @Test
    public void testComposedFragmentsProtectStandardBackslashLiteralBoundaries() {
        assertEquals("SELECT * FROM users CROSS JOIN (SELECT 'a\\') a -- tail\n WHERE id = ?",
                PSC.select("*").from("users").crossJoin("(SELECT 'a\\') a -- tail").where(Filters.eq("id", 1)).build().query());
        assertEquals("SELECT * FROM users CROSS JOIN (SELECT E'a\\' -- quoted') a WHERE id = ?",
                PSC.select("*").from("users").crossJoin("(SELECT E'a\\' -- quoted') a").where(Filters.eq("id", 1)).build().query());

        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        assertEquals("SELECT * FROM users CROSS JOIN (SELECT 'a\\' -- quoted') a WHERE id = ?",
                mysql.select("*").from("users").crossJoin("(SELECT 'a\\' -- quoted') a").where(Filters.eq("id", 1)).build().query());
    }

    @Test
    public void testComposedQueriesRejectTerminatorsAfterStandardBackslashLiterals() {
        final SqlBuilder parent = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> parent.union("SELECT 'a\\';"));
        assertEquals("SELECT id FROM users", parent.build().query());

        final SqlBuilder snapshot = PSC.select("id").from("users").append("WHERE value = 'a\\';");
        assertThrows(IllegalArgumentException.class, snapshot::toSubQuery);
    }

    @Test
    public void testMySqlArithmeticDashPairDoesNotHideStatementTerminators() {
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        final SqlBuilder parent = mysql.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> parent.union("SELECT 2--1;"));
        assertEquals("SELECT id FROM users UNION SELECT 2--1 ORDER BY id", parent.union("SELECT 2--1").orderBy("id").build().query());
        assertEquals("SELECT id FROM users UNION SELECT 2-- comment;\n ORDER BY id",
                mysql.select("id").from("users").union("SELECT 2-- comment;").orderBy("id").build().query());
        for (final String comment : List.of("#>;", "#-;", "##;")) {
            assertEquals("SELECT id FROM users UNION SELECT 2 " + comment + "\n ORDER BY id",
                    mysql.select("id").from("users").union("SELECT 2 " + comment).orderBy("id").build().query());
        }
    }

    @Test
    public void testSetOperationRejectsStatementTerminatorsWithoutChangingParent() {
        for (final String query : new String[] { "SELECT id FROM archived_users;", "SELECT id FROM archived_users; SELECT id FROM another_table",
                "SELECT id FROM #tmp;", "SELECT id FROM #tmp, ##other; SELECT id FROM another_table" }) {
            final SqlBuilder parent = PSC.select("id").from("users");
            assertThrows(IllegalArgumentException.class, () -> parent.union(query));
            assertEquals("SELECT id FROM users UNION SELECT id FROM active_users ORDER BY id",
                    parent.union("SELECT id FROM active_users").orderBy("id").build().query());
        }
        assertEquals("SELECT id FROM users UNION SELECT ';' FROM archived_users /* ; */ ORDER BY id",
                PSC.select("id").from("users").union("SELECT ';' FROM archived_users /* ; */").orderBy("id").build().query());
        assertEquals("SELECT id FROM users UNION SELECT [;] FROM archived_users -- ;\n ORDER BY id",
                PSC.select("id").from("users").union("SELECT [;] FROM archived_users -- ;").orderBy("id").build().query());

        final SqlBuilder criteriaParent = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> criteriaParent.append(new Union(Filters.subQuery("SELECT id FROM archived_users;"))));
        assertEquals("SELECT id FROM users", criteriaParent.build().query());
    }

    @Test
    public void testComposedFragmentsDistinguishTemporaryTablesFromHashComments() {
        final Dsl sqlServer = Dsl.forDialect(NSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());
        for (final Dsl dsl : new Dsl[] { NSB, sqlServer }) {
            assertEquals("SELECT id FROM (SELECT id FROM #tmp) t", dsl.select("id").from(dsl.select("id").from("#tmp"), "t").build().query());
            assertEquals("SELECT id FROM users WHERE id IN (SELECT id FROM #tmp)",
                    dsl.select("id").from("users").where(Filters.in("id", Filters.subQuery("SELECT id FROM #tmp"))).build().query());
            final SqlBuilder parent = dsl.select("id").from("users");
            assertThrows(IllegalArgumentException.class, () -> parent.union("SELECT id FROM #tmp;"));
            assertEquals("SELECT id FROM users", parent.build().query());
        }

        final Dsl mysql = Dsl.forDialect(NSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        for (final Dsl dsl : new Dsl[] { NSB, mysql }) {
            assertEquals("SELECT id FROM users UNION SELECT id FROM archive WHERE id = 1 #note\n ORDER BY id",
                    dsl.select("id").from("users").union("SELECT id FROM archive WHERE id = 1 #note").orderBy("id").build().query());

            // The SELECT classifier conservatively rejects any mid-statement '#' comment whose line contains ';'
            // (SQL Server and H2's MSSQLServer/Oracle modes read '#' as a name, so the ';' would split statements),
            // even when the comment-boundary scanner recognizes a hash comment.
            final SqlBuilder parent = dsl.select("id").from("users");
            assertThrows(IllegalArgumentException.class, () -> parent.union("SELECT id FROM archive WHERE id = 1 #note;"));
            assertThrows(IllegalArgumentException.class, () -> parent.union("SELECT id FROM archive WHERE id = 1 # note;"));
            assertEquals("SELECT id FROM users", parent.build().query());
        }
    }

    @Test
    public void testBuilderBackedSubqueriesRejectStatementTerminatorsBeforeMergingBindings() {
        final SqlBuilder parent = NSC.select("id");
        final SqlBuilder rejected = NSC.select("id").from("archived_users").where(Filters.eq("status", 7)).append("; SELECT id FROM another_table");
        assertThrows(IllegalArgumentException.class, () -> parent.from(rejected, "a"));
        assertTrue(parent._parameters.isEmpty());
        assertTrue(parent._generatedNamedParameterNames.isEmpty());
        assertEquals("SELECT id FROM users WHERE status = :status", parent.from("users").where(Filters.eq("status", 8)).build().query());

        final SqlBuilder snapshot = PSC.select("id").from("users").append(";");
        assertThrows(IllegalArgumentException.class, snapshot::toSubQuery);

        final SqlBuilder unionParent = NSC.select("id").from("users").where(Filters.eq("status", 1));
        assertThrows(IllegalArgumentException.class,
                () -> unionParent.union(NSC.select("id").from("archived_users").where(Filters.eq("status", 2)).append(";")));
        final AbstractQueryBuilder.SP result = unionParent.union(NSC.select("id").from("active_users").where(Filters.eq("status", 3))).build();
        assertEquals("SELECT id FROM users WHERE status = :status UNION SELECT id FROM active_users WHERE status = :status_2", result.query());
        assertEquals(List.of(1, 3), result.parameters());
    }

    @Test
    public void testJoinTrailingLineCommentsDoNotSwallowConnectorsOrClauses() {
        for (final String comment : new String[] { "-- join hint", "# join hint" }) {
            assertEquals("SELECT * FROM users u JOIN orders o " + comment + "\n ON u.id = o.user_id WHERE u.id = ?",
                    PSC.select("*").from("users u").join("orders o " + comment).on("u.id = o.user_id").where(Filters.eq("u.id", 1)).build().query());
            assertEquals("SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id " + comment + "\n WHERE u.id = ?",
                    PSC.select("*").from("users u").leftJoin("orders o ON u.id = o.user_id " + comment).where(Filters.eq("u.id", 1)).build().query());
            assertEquals("SELECT * FROM users u CROSS JOIN orders o " + comment + "\n WHERE u.id = ?",
                    PSC.select("*").from("users u").crossJoin("orders o " + comment).where(Filters.eq("u.id", 1)).build().query());
            final Criteria criteria = Criteria.builder().join("orders o " + comment, Filters.expr("u.id = o.user_id")).build();
            assertEquals("SELECT * FROM users u JOIN orders o " + comment + "\n ON u.id = o.user_id",
                    PSC.select("*").from("users u").append(criteria).build().query());
        }
        assertEquals("SELECT * FROM users u JOIN orders o /* hint */ ON u.id = o.user_id",
                PSC.select("*").from("users u").join("orders o /* hint */").on("u.id = o.user_id").build().query());
    }

    @Test
    public void testSetOperationTrailingLineCommentsDoNotSwallowFollowingOperandsOrOrderBy() {
        for (final String comment : new String[] { "-- branch hint", "# branch hint" }) {
            assertEquals("SELECT id FROM users UNION SELECT id FROM archived_users " + comment + "\n ORDER BY id",
                    PSC.select("id").from("users").union("SELECT id FROM archived_users " + comment).orderBy("id").build().query());
            assertEquals("SELECT id FROM users UNION SELECT id FROM archived_users " + comment + "\n INTERSECT SELECT id FROM active_users",
                    PSC.select("id").from("users").union("SELECT id FROM archived_users " + comment).intersect("SELECT id FROM active_users").build().query());
        }
    }

    @Test
    public void testRawAndSnapshotSubqueryTrailingCommentsDoNotSwallowClosingParenthesis() {
        for (final String comment : new String[] { "-- subquery hint", "# subquery hint" }) {
            final SubQuery raw = Filters.subQuery("SELECT id FROM archived_users WHERE status = ? " + comment, List.of(7));
            final AbstractQueryBuilder.SP rawResult = PSC.select("id").from("users").where(Filters.in("id", raw)).build();
            assertEquals("SELECT id FROM users WHERE id IN (SELECT id FROM archived_users WHERE status = ? " + comment + "\n)", rawResult.query());
            assertEquals(List.of(7), rawResult.parameters());

            final SqlBuilder child = NSC.select("id").from("archived_users").where(Filters.eq("status", 7)).append(comment);
            final AbstractQueryBuilder.SP derivedResult = NSC.select("id").from(child, "a").where(Filters.eq("status", 8)).build();
            assertEquals("SELECT id FROM (SELECT id FROM archived_users WHERE status = :status " + comment + "\n) a WHERE status = :status_2",
                    derivedResult.query());
            assertEquals(List.of(7, 8), derivedResult.parameters());

            final SubQuery snapshot = NSC.select("id").from("archived_users").where(Filters.eq("status", 7)).append(comment).toSubQuery();
            final AbstractQueryBuilder.SP snapshotResult = NSC.select("id").from("users").where(Filters.in("id", snapshot)).build();
            assertEquals("SELECT id FROM users WHERE id IN (SELECT id FROM archived_users WHERE status = :status " + comment + "\n)",
                    snapshotResult.query());
            assertEquals(List.of(7), snapshotResult.parameters());
        }
    }

    @Test
    public void testParameterNameSanitizationPreservesCornerCases() {
        // Returning valid names directly must retain the old normalization of every other shape.
        final String[][] cases = { { "firstName", "firstName" }, { " u.id ", "id" }, { "COUNT(*)", "COUNT" },
                { "___", "param" }, { "_leading", "_leading" }, { "trailing__", "trailing" }, { "1abc", "p1abc" },
                { "  ", "param" }, { "a@#$b", "a_b" }, { "éclair", "éclair" }, { "猫名", "猫名" },
                { "u.col_", "col" }, { "..", "param" }, { "", "" } };
        for (final String[] item : cases) {
            assertEquals(item[1], AbstractQueryBuilder.sanitizeNamedParameterName(item[0]), item[0]);
        }
        assertNull(AbstractQueryBuilder.sanitizeNamedParameterName(null));
    }

    @Test
    public void testEmptyCheckpointRestoresPartiallyRenderedParametersAcrossPolicies() {
        for (final Dsl dsl : new Dsl[] { PSC, NSC, MSC, SCSB }) {
            final SqlBuilder builder = dsl.update("users");
            final Map<String, Object> rejected = new LinkedHashMap<>();
            rejected.put("id", 1);
            rejected.put("bad--column", 2);
            // Rendering fails after the first binding. An empty snapshot must still remove
            // all SQL, names, parameters, and the lazy UPDATE initialization before retry.
            assertThrows(IllegalArgumentException.class, () -> builder.set(rejected));
            assertTrue(builder._parameters.isEmpty());
            assertTrue(builder._namedParameterNameOccurrences.isEmpty());
            assertTrue(builder._generatedNamedParameterNames.isEmpty());
            assertTrue(builder._renderedNamedParameterTokens.isEmpty());
            final AbstractQueryBuilder.SP actual = builder.set("id", 3).where(Filters.eq("id", 4)).build();
            final AbstractQueryBuilder.SP expected = dsl.update("users").set("id", 3).where(Filters.eq("id", 4)).build();
            assertEquals(expected.query(), actual.query());
            assertEquals(expected.parameters(), actual.parameters());
        }
    }

    @Test
    public void testCheckpointRestoresPrefixModifiedByCustomNamedRenderer() {
        final Dsl dsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
            if (name.equals("fail")) {
                sb.insert(0, "damaged prefix ");
                throw new IllegalStateException("renderer failed");
            }
            sb.append(':').append(name);
        }).build());
        final SqlBuilder builder = dsl.update("users").set("id", 1);
        // Named renderers receive the live buffer; rollback must restore overwritten text,
        // not merely truncate it, and retain already emitted name occurrences.
        assertThrows(IllegalStateException.class, () -> builder.set("fail", 9));
        final AbstractQueryBuilder.SP actual = builder.set("id", 2).build();
        assertEquals("UPDATE users SET id = :id, id = :id_2", actual.query());
        assertEquals(Arrays.asList(1, 2), actual.parameters());
    }


    @Test
    public void testCompactCheckpointsCopyValuesAndRestoreNestedMutations() {
        final SqlBuilder builder = NSC.update("users").set("id", 1);
        final String originalSql = builder._sb.toString();
        builder._aliasPropColumnNameMap = new java.util.HashMap<>();
        builder._aliasPropColumnNameMap.put("u", Collections.emptyMap());
        builder._namedParameterNameOccurrences.put(null, null);
        // A snapshot of live Map.Entry objects would follow these in-place value changes.
        // Nested failures must restore their own starting state before the outer rollback.
        assertThrows(IllegalStateException.class, () -> builder.mutateAtomically(() -> {
            builder._parameters.set(0, 9);
            builder._namedParameterNameOccurrences.put("id", 9);
            builder._namedParameterNameOccurrences.put(null, 7);
            builder._renderedNamedParameterTokens.put("id", "changed");
            builder._generatedNamedParameterNames.add("id_9");
            builder.calledOpSet.add("temporary");
            builder._sb.replace(0, 6, "BROKEN");
            builder._aliasPropColumnNameMap.clear();
            assertThrows(IllegalArgumentException.class, () -> builder.mutateAtomically(() -> {
                builder._parameters.clear();
                builder._namedParameterNameOccurrences.put("id", 10);
                builder._renderedNamedParameterTokens.clear();
                builder._generatedNamedParameterNames.clear();
                builder.calledOpSet.clear();
                throw new IllegalArgumentException("inner");
            }));
            assertEquals(Integer.valueOf(9), builder._namedParameterNameOccurrences.get("id"));
            assertEquals(List.of(9), builder._parameters);
            assertEquals("changed", builder._renderedNamedParameterTokens.get("id"));
            throw new IllegalStateException("outer");
        }));
        assertEquals(originalSql, builder._sb.toString());
        assertEquals(Collections.singletonMap("u", Collections.emptyMap()), builder._aliasPropColumnNameMap);
        assertEquals(List.of(1), builder._parameters);
        assertEquals(Integer.valueOf(1), builder._namedParameterNameOccurrences.get("id"));
        assertTrue(builder._namedParameterNameOccurrences.containsKey(null));
        assertNull(builder._namedParameterNameOccurrences.get(null));
        assertEquals(":id", builder._renderedNamedParameterTokens.get("id"));
        assertEquals(Set.of("id"), builder._generatedNamedParameterNames);
        assertTrue(builder.calledOpSet.isEmpty());
        builder._namedParameterNameOccurrences.remove(null);
        assertEquals("UPDATE users SET id = :id, id = :id_2", builder.set("id", 2).build().query());
    }

    @Test
    public void testLazyValidationLabelsPreserveIndexedDiagnostics() {
        final String suffix = " must not be null, empty, or blank";
        assertEquals("columns[1]" + suffix, assertThrows(IllegalArgumentException.class,
                () -> AbstractQueryBuilder.checkSqlFragmentsNotBlank(new String[] { "id", "\t " }, "columns")).getMessage());
        assertEquals("columns[2]" + suffix, assertThrows(IllegalArgumentException.class,
                () -> AbstractQueryBuilder.checkSqlFragmentsNotBlank(Arrays.asList("id", "name", null), "columns")).getMessage());
        assertEquals("Key in props" + suffix, assertThrows(IllegalArgumentException.class,
                () -> AbstractQueryBuilder.checkSqlFragmentKeysNotBlank(Collections.singletonMap(" ", 1), "props")).getMessage());
    }

    @Test
    public void testCamelIdentityPathPreservesAcronymAndDigitBoundaries() {
        for (final String name : Arrays.asList(null, "", "firstName", "columnName42", "aB", "aB2", "a2B", "abc1Xy", "iPhone",
                "fooBAR", "URLValue", "_firstName_", "t.firstName", "éclair", "猫名", "class")) {
            final String expected = AbstractQueryBuilder.sqlKeyWords.contains(name) ? name : QueryUtil.convertIdentifier(name, NamingPolicy.CAMEL_CASE);
            assertEquals(expected, AbstractQueryBuilder.normalizeColumnName(name, NamingPolicy.CAMEL_CASE), name);
        }
    }

    private static final class TestClause extends Clause {
        TestClause(final Operator operator, final Condition condition) {
            super(operator, condition);
        }
    }

    private static final class ChangingCollection<E> extends AbstractCollection<E> {
        private final Collection<E> firstIteration;
        private final Collection<E> laterIterations;
        private int iterationCount;

        ChangingCollection(final Collection<E> firstIteration, final Collection<E> laterIterations) {
            this.firstIteration = firstIteration;
            this.laterIterations = laterIterations;
        }

        @Override
        public Iterator<E> iterator() {
            return (iterationCount++ == 0 ? firstIteration : laterIterations).iterator();
        }

        @Override
        public int size() {
            return firstIteration.size();
        }
    }

    private static final class NominallyNonEmptyCollection<E> extends AbstractCollection<E> {
        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 1;
        }
    }

    private static final class ChangingMap<V> extends AbstractMap<String, V> {
        private final Entry<String, V> firstEntry;
        private final Entry<String, V> laterEntry;
        private int iterationCount;

        ChangingMap(final String firstKey, final V firstValue, final String laterKey, final V laterValue) {
            firstEntry = new SimpleImmutableEntry<>(firstKey, firstValue);
            laterEntry = new SimpleImmutableEntry<>(laterKey, laterValue);
        }

        @Override
        public Set<Entry<String, V>> entrySet() {
            return Collections.singleton(iterationCount++ == 0 ? firstEntry : laterEntry);
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static final class ThrowingUpdateEntity {
        public String getValue() {
            throw new IllegalStateException("getter failed");
        }

        public void setValue(final String value) {
            // Bean setter supplied so the property is considered updatable.
        }
    }

    @Test
    public void testConstants() {
        assertNotNull(AbstractQueryBuilder.ALL);
        assertNotNull(AbstractQueryBuilder.TOP);
        assertNotNull(AbstractQueryBuilder.UNIQUE);
        assertNotNull(AbstractQueryBuilder.DISTINCT);
        assertNotNull(AbstractQueryBuilder.DISTINCTROW);
        assertNotNull(AbstractQueryBuilder.ASTERISK);
        assertNotNull(AbstractQueryBuilder.COUNT_ALL);
    }

    @Test
    public void testPSCSelectFrom() {
        String sql = PSC.select("id", "firstName", "lastName").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testToSql() {
        String sql = PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testBuild() {
        AbstractQueryBuilder.SP sqlPair = PSC.select("id").from(Account.class).where(Filters.eq("id", 1)).build();
        assertNotNull(sqlPair);
        assertTrue(sqlPair.query().contains("WHERE"));
        assertEquals(1, sqlPair.parameters().size());
    }

    @Test
    public void testBuildSnapshotsProtectedParameterBuffer() {
        final SqlBuilder builder = PSC.select("id").from(Account.class).where(Filters.eq("id", 1));
        final AbstractQueryBuilder.SP sqlPair = builder.build();

        builder._parameters.add(2);

        assertEquals(Collections.singletonList(1), sqlPair.parameters());
    }

    @Test
    public void testSPDefensivelyCopiesWrappedParameterList() {
        final List<Object> source = new ArrayList<>();
        source.add(1);

        final AbstractQueryBuilder.SP sqlPair = new AbstractQueryBuilder.SP("SELECT ?", ImmutableList.wrap(source));
        source.add(2);

        assertEquals(Collections.singletonList(1), sqlPair.parameters());
        assertThrows(IllegalArgumentException.class, () -> new AbstractQueryBuilder.SP(null, ImmutableList.empty()));
        assertThrows(IllegalArgumentException.class, () -> new AbstractQueryBuilder.SP("SELECT 1", null));
    }

    @Test
    public void testEmptySelectModifierRemainsNoOpAfterModifierWasSet() {
        final SqlBuilder builder = PSC.select("id").distinct();

        builder.selectModifier(null).selectModifier("");

        assertEquals("SELECT DISTINCT id FROM users", builder.from("users").build().query());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").distinct().selectModifier("   "));
    }

    @Test
    public void testSelectModifierRejectsNonSelectBuildersWithoutChangingThem() {
        final SqlBuilder update = PSC.update("users");
        assertThrows(IllegalStateException.class, () -> update.selectModifier("DISTINCT"));
        assertEquals("UPDATE users SET name = ?", update.set("name").build().query());

        final SqlBuilder delete = PSC.deleteFrom("users");
        assertThrows(IllegalStateException.class, () -> delete.distinct());
        assertEquals("DELETE FROM users", delete.build().query());
    }

    @Test
    public void testClosedBuilderMutationApisConsistentlyThrowIllegalStateException() {
        final SqlBuilder rawAppend = PSC.select("id").from("users");
        rawAppend.build();
        assertThrows(IllegalStateException.class, () -> rawAppend.append("FOR UPDATE"));
        // Lifecycle errors take precedence over argument validation after the builder is closed.
        assertThrows(IllegalStateException.class, () -> rawAppend.append((String) null));
        assertThrows(IllegalStateException.class, () -> rawAppend.append("   "));

        final SqlBuilder modifier = PSC.select("id").from("users");
        modifier.build();
        assertThrows(IllegalStateException.class, () -> modifier.selectModifier("DISTINCT"));
        assertThrows(IllegalStateException.class, () -> modifier.selectModifier(null));
        assertThrows(IllegalStateException.class, () -> modifier.selectModifier(""));

        final SqlBuilder condition = PSC.select("id").from("users");
        condition.build();
        assertThrows(IllegalStateException.class, () -> condition.append(Filters.eq("id", 1)));
        assertThrows(IllegalStateException.class, () -> condition.appendIf(false, Filters.eq("id", 1)));
        assertThrows(IllegalStateException.class, () -> condition.appendIf(false, (Condition) null));
        assertThrows(IllegalStateException.class, () -> condition.appendIf(true, (Condition) null));

        final SqlBuilder pagination = PSC.select("id").from("users");
        pagination.build();
        assertThrows(IllegalStateException.class, () -> pagination.limit(-1));
        assertThrows(IllegalStateException.class, () -> pagination.limit(10));

        final SqlBuilder setOperation = PSC.select("id").from("users");
        setOperation.build();
        assertThrows(IllegalStateException.class, () -> setOperation.union("SELECT id FROM archived_users"));
    }

    @Test
    public void testRejectedEntityOverloadsPreserveExistingMappingState() {
        final SqlBuilder query = PSC.select("id").from(Account.class);
        assertSame(Account.class, query._entityClass);
        assertThrows(IllegalStateException.class, () -> query.from(String.class, "s"));
        assertSame(Account.class, query._entityClass);
        assertEquals("SELECT acc.id AS \"id\" FROM account acc", query.build().query());

        final SqlBuilder update = PSC.update("users", Account.class);
        assertSame(Account.class, update._entityClass);
        assertThrows(IllegalStateException.class, () -> update.into(String.class));
        assertSame(Account.class, update._entityClass);
        assertEquals("UPDATE users SET first_name = ?", update.set("firstName").build().query());

        final SqlBuilder joined = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> joined.join((Class<?>) null, "n"));
        assertEquals("SELECT id FROM users", joined.build().query());

        // A non-bean join class must be rejected before any JOIN text or alias mapping is written.
        final SqlBuilder nonBeanJoin = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> nonBeanJoin.leftJoin(String.class, "s"));
        assertThrows(IllegalArgumentException.class, () -> nonBeanJoin.crossJoin(String.class));
        assertEquals("SELECT id FROM users", nonBeanJoin.build().query());
    }

    @Test
    public void testSetApisRejectNonUpdateBuildersWithoutCorruptingThem() {
        final SqlBuilder query = PSC.select("id").from("users");
        assertThrows(IllegalStateException.class, () -> query.set("name"));
        assertEquals("SELECT id FROM users", query.build().query());

        final SqlBuilder delete = PSC.deleteFrom("users");
        assertThrows(IllegalStateException.class, () -> delete.set(Collections.singletonMap("name", "x")));
        assertEquals("DELETE FROM users", delete.build().query());

        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").set(Account.class));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set((Class<?>) null));
    }

    @Test
    public void testRemovedSetOverloadsAreAbsent() throws NoSuchMethodException {
        assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("set", String[].class));
        assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setValue", String.class, Object.class));
        assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setEntity", Object.class));
        assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setEntity", Object.class, Set.class));
        assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setEntity", Class.class));
        assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setEntity", Class.class, Set.class));

        assertNotNull(AbstractQueryBuilder.class.getDeclaredMethod("set", String.class, Object.class));
        assertNotNull(AbstractQueryBuilder.class.getDeclaredMethod("set", Object.class, Set.class));
        assertNotNull(AbstractQueryBuilder.class.getDeclaredMethod("set", String.class, Object.class, String.class, Object.class));
        assertNotNull(AbstractQueryBuilder.class.getDeclaredMethod("set", String.class, Object.class, String.class, Object.class, String.class, Object.class));
    }

    @Test
    public void testLazyDmlInitializationPrecedesClauseReservation() {
        final SqlBuilder incompleteUpdate = PSC.update("users");
        assertThrows(IllegalStateException.class, () -> incompleteUpdate.where("id = 1"));
        assertEquals("UPDATE users SET name = ? WHERE id = 1", incompleteUpdate.set("name").where("id = 1").build().query());

        assertEquals("DELETE FROM users ORDER BY id LIMIT 1", PSC.deleteFrom("users").orderBy("id").limit(1).build().query());

        final SqlBuilder insert = PSC.insert("name").into("users");
        assertThrows(IllegalStateException.class, () -> insert.where("id = 1"));
        assertEquals("INSERT INTO users (name) VALUES (?)", insert.build().query());
    }

    @Test
    public void testOperationSpecificClausesRejectInvalidDmlWithoutChangingTheBuilder() {
        final SqlBuilder update = PSC.update("users");
        assertThrows(IllegalStateException.class, () -> update.groupBy("id"));
        assertEquals("UPDATE users SET name = ?", update.set("name").build().query());

        final SqlBuilder deleteWithHaving = PSC.deleteFrom("users");
        assertThrows(IllegalStateException.class, () -> deleteWithHaving.having("COUNT(*) > 0"));
        assertEquals("DELETE FROM users", deleteWithHaving.build().query());

        final SqlBuilder deleteWithOffset = PSC.deleteFrom("users");
        assertThrows(IllegalStateException.class, () -> deleteWithOffset.offset(1));
        assertEquals("DELETE FROM users", deleteWithOffset.build().query());

        final Dsl sqlServerDsl = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());
        final SqlBuilder sqlServerUpdate = sqlServerDsl.update("users");
        assertThrows(IllegalStateException.class, () -> sqlServerUpdate.limit(1));
        assertEquals("UPDATE users SET name = ?", sqlServerUpdate.set("name").build().query());
    }

    @Test
    public void testSqlServerPaginationRequiresOrderByAndUsesValidOffsetFetchGrammar() {
        final Dsl sqlServerDsl = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());

        final SqlBuilder countOnly = sqlServerDsl.select("id").from("users");
        assertThrows(IllegalStateException.class, () -> countOnly.limit(10));
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", countOnly.orderBy("id").limit(10).build().query());

        final SqlBuilder countAndOffset = sqlServerDsl.select("id").from("users");
        assertThrows(IllegalStateException.class, () -> countAndOffset.limit(10, 5));
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", countAndOffset.orderBy("id").limit(10, 5).build().query());

        final SqlBuilder offset = sqlServerDsl.select("id").from("users");
        assertThrows(IllegalStateException.class, () -> offset.offset(5));
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 5 ROWS", offset.orderBy("id").offset(5).build().query());

        final SqlBuilder offsetRows = sqlServerDsl.select("id").from("users");
        assertThrows(IllegalStateException.class, () -> offsetRows.offsetRows(6));
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 6 ROWS", offsetRows.orderBy("id").offsetRows(6).build().query());

        final SqlBuilder explicitNext = sqlServerDsl.select("id").from("users");
        assertThrows(IllegalStateException.class, () -> explicitNext.fetchNextRows(3));
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 3 ROWS ONLY", explicitNext.orderBy("id").fetchNextRows(3).build().query());

        final SqlBuilder explicitFirst = sqlServerDsl.select("id").from("users");
        assertThrows(IllegalStateException.class, () -> explicitFirst.fetchFirstRows(4));
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 4 ROWS ONLY", explicitFirst.orderBy("id").fetchFirstRows(4).build().query());

        assertThrows(IllegalArgumentException.class, () -> new Limit("FETCH FIRST ? ROWS ONLY"));

        final Dsl oracleDsl = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Oracle")).build());
        assertEquals("SELECT id FROM users FETCH FIRST 2 ROWS ONLY", oracleDsl.select("id").from("users").limit(2).build().query());

        final Dsl db2Dsl = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("DB2")).build());
        assertEquals("SELECT id FROM users FETCH FIRST 2 ROWS ONLY", db2Dsl.select("id").from("users").limit(2).build().query());
    }

    @Test
    public void testStructuredClauseRenderingFailuresAreAtomic() {
        final SqlBuilder grouped = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> grouped.groupBy("id -- unsafe"));
        assertEquals("SELECT id FROM users GROUP BY id", grouped.groupBy("id").build().query());

        final SqlBuilder ordered = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> ordered.orderBy("id", "name /* unsafe */"));
        assertEquals("SELECT id FROM users ORDER BY id", ordered.orderBy("id").build().query());

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
                return "unsupported";
            }
        };

        final SqlBuilder filtered = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> filtered.where(unsupported));
        assertEquals("SELECT id FROM users WHERE id = 1", filtered.where("id = 1").build().query());

        final SqlBuilder joined = PSC.select("u.id").from("users u").join("accounts a");
        assertThrows(IllegalArgumentException.class, () -> joined.using(Arrays.asList("id", "tenant_id -- unsafe")));
        assertEquals("SELECT u.id FROM users u JOIN accounts a USING (id)", joined.using("id").build().query());

        final SqlBuilder joinedWithCondition = PSC.select("u.id").from("users u").join("accounts a");
        assertThrows(IllegalArgumentException.class, () -> joinedWithCondition.on(unsupported));
        assertEquals("SELECT u.id FROM users u JOIN accounts a ON u.id = a.id", joinedWithCondition.on("u.id = a.id").build().query());
    }

    @Test
    public void testSetRenderingFailuresAndLateSetCallsAreAtomic() {
        final SqlBuilder partialSet = PSC.update("users");
        assertThrows(IllegalArgumentException.class, () -> partialSet.set(Arrays.asList("name", "age -- unsafe")));
        assertEquals("UPDATE users SET name = ?", partialSet.set("name").build().query());

        final SqlBuilder lateSet = PSC.update("users").set("name").where("id = 1");
        assertThrows(IllegalStateException.class, () -> lateSet.set("age"));
        assertEquals("UPDATE users SET name = ? WHERE id = 1", lateSet.build().query());

        final Dsl sqlServerDsl = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());
        final SqlBuilder stagedUpdate = sqlServerDsl.update(Account.class);
        final Collection<String> stagedPropNames = stagedUpdate._propOrColumnNames;
        assertThrows(IllegalArgumentException.class, () -> stagedUpdate.where("#stage.id /* unsafe */ = 1"));
        assertSame(stagedPropNames, stagedUpdate._propOrColumnNames);
        assertTrue(stagedUpdate.where("id = 1").build().query().endsWith(" WHERE id = 1"));
    }

    @Test
    public void testEntitySetGetterAndRendererFailuresRestoreAllBuilderMetadata() {
        final SqlBuilder getterFailure = PSC.update("users");
        final Class<?> initialGetterEntityClass = getterFailure._entityClass;
        final Object initialGetterEntityInfo = getterFailure._entityInfo;
        final Object initialGetterColumnMap = getterFailure._propColumnNameMap;
        assertThrows(RuntimeException.class, () -> getterFailure.set(new ThrowingUpdateEntity()));
        assertSame(initialGetterEntityClass, getterFailure._entityClass);
        assertSame(initialGetterEntityInfo, getterFailure._entityInfo);
        assertSame(initialGetterColumnMap, getterFailure._propColumnNameMap);
        assertEquals("UPDATE users SET value = ?", getterFailure.set(Collections.singletonMap("value", "ok")).build().query());

        final boolean[] failFirstRender = { true };
        final Dsl throwingNamedDsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
            if (failFirstRender[0]) {
                failFirstRender[0] = false;
                throw new IllegalStateException("named renderer failed");
            }

            sb.append(':').append(name);
        }).build());
        final SqlBuilder rendererFailure = throwingNamedDsl.update("users");
        final Class<?> initialRendererEntityClass = rendererFailure._entityClass;
        final Object initialRendererEntityInfo = rendererFailure._entityInfo;
        final Object initialRendererColumnMap = rendererFailure._propColumnNameMap;
        assertThrows(IllegalStateException.class, () -> rendererFailure.set(Account.class));
        assertSame(initialRendererEntityClass, rendererFailure._entityClass);
        assertSame(initialRendererEntityInfo, rendererFailure._entityInfo);
        assertSame(initialRendererColumnMap, rendererFailure._propColumnNameMap);
        assertTrue(rendererFailure._namedParameterNameOccurrences.isEmpty());
        assertTrue(rendererFailure._generatedNamedParameterNames.isEmpty());
        assertTrue(rendererFailure._renderedNamedParameterTokens.isEmpty());
        assertEquals("UPDATE users SET status = :status", rendererFailure.set(Collections.singletonMap("status", "ACTIVE")).build().query());
    }

    @Test
    public void testIntoRenderingFailuresAreAtomic() {
        // A comment-token column is rejected while the INSERT text is being emitted; the failed into()
        // must not leave a partial "INSERT INTO account (first_name, " behind nor keep _tableName set,
        // otherwise a later build() would silently return the truncated statement.
        final SqlBuilder unsafeInsert = PSC.insert("firstName", "bad--name");
        assertThrows(IllegalArgumentException.class, () -> unsafeInsert.into("account"));
        assertNull(unsafeInsert._tableName);
        assertEquals(0, unsafeInsert._sb.length());
        assertThrows(IllegalStateException.class, unsafeInsert::build);

        // The NAMED_SQL VALUES path also mutates the named-parameter registries; a failed render must
        // restore them so a retried into() behaves as if the first call never happened.
        final boolean[] failFirstRender = { true };
        final Dsl throwingNamedDsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
            if (failFirstRender[0]) {
                failFirstRender[0] = false;
                throw new IllegalStateException("named renderer failed");
            }

            sb.append(':').append(name);
        }).build());
        final SqlBuilder namedInsert = throwingNamedDsl.insert("firstName", "lastName");
        assertThrows(IllegalStateException.class, () -> namedInsert.into("account"));
        assertNull(namedInsert._tableName);
        assertEquals(0, namedInsert._sb.length());
        assertTrue(namedInsert._namedParameterNameOccurrences.isEmpty());
        assertTrue(namedInsert._generatedNamedParameterNames.isEmpty());
        assertTrue(namedInsert._renderedNamedParameterTokens.isEmpty());
        assertEquals("INSERT INTO account (first_name, last_name) VALUES (:firstName, :lastName)", namedInsert.into("account").build().query());

        // Entity-aware overloads used to install their mapping before opening the rendering
        // transaction, so a rejected column left metadata from a failed into(...) attempt behind.
        final SqlBuilder entityInsert = PSC.insert("bad--name");
        assertThrows(IllegalArgumentException.class, () -> entityInsert.into("account", Account.class));
        assertNull(entityInsert._entityClass);
        assertNull(entityInsert._entityInfo);
        assertNull(entityInsert._propColumnNameMap);
        assertNull(entityInsert._tableName);
        assertEquals(0, entityInsert._sb.length());

        final SqlBuilder classEntityInsert = PSC.insert("bad--name");
        assertThrows(IllegalArgumentException.class, () -> classEntityInsert.into(Account.class));
        assertNull(classEntityInsert._entityClass);
        assertNull(classEntityInsert._entityInfo);
        assertNull(classEntityInsert._propColumnNameMap);
        assertNull(classEntityInsert._tableName);
        assertEquals(0, classEntityInsert._sb.length());
    }

    @Test
    public void testFromRenderingFailuresAreAtomic() {
        // Rendering the staged select list is part of from(); a rejected column must roll back the
        // emitted SELECT prefix so a retried from() cannot emit a second "SELECT ..." fragment.
        final SqlBuilder unsafeSelect = PSC.select("name", "a--b");
        assertThrows(IllegalArgumentException.class, () -> unsafeSelect.from("t"));
        assertEquals(0, unsafeSelect._sb.length());
        assertFalse(unsafeSelect._hasFromBeenSet);
        assertNull(unsafeSelect._tableName);

        // A retry fails the same way and still leaves no partial SQL behind (no doubled SELECT).
        assertThrows(IllegalArgumentException.class, () -> unsafeSelect.from("t"));
        assertEquals(0, unsafeSelect._sb.length());
        assertThrows(IllegalStateException.class, unsafeSelect::build);

        // The entity association is part of from(...), so it must be rolled back along with the SQL
        // when rendering a staged select expression fails.
        final SqlBuilder entitySelect = PSC.select("bad--name");
        assertThrows(IllegalArgumentException.class, () -> entitySelect.from("account", Account.class));
        assertNull(entitySelect._entityClass);
        assertNull(entitySelect._entityInfo);
        assertNull(entitySelect._propColumnNameMap);
        assertNull(entitySelect._tableName);
        assertFalse(entitySelect._hasFromBeenSet);
        assertEquals(0, entitySelect._sb.length());

        final SqlBuilder classEntitySelect = PSC.select("bad--name");
        assertThrows(IllegalArgumentException.class, () -> classEntitySelect.from(Account.class, "acc"));
        assertNull(classEntitySelect._entityClass);
        assertNull(classEntitySelect._entityInfo);
        assertNull(classEntitySelect._propColumnNameMap);
        assertNull(classEntitySelect._tableName);
        assertFalse(classEntitySelect._hasFromBeenSet);
        assertEquals(0, classEntitySelect._sb.length());

        final SqlBuilder collectionEntitySelect = PSC.select("bad--name");
        assertThrows(IllegalArgumentException.class, () -> collectionEntitySelect.from(Account.class, Collections.singletonList("account")));
        assertNull(collectionEntitySelect._entityClass);
        assertNull(collectionEntitySelect._entityInfo);
        assertNull(collectionEntitySelect._propColumnNameMap);
        assertNull(collectionEntitySelect._tableName);
        assertFalse(collectionEntitySelect._hasFromBeenSet);
        assertEquals(0, collectionEntitySelect._sb.length());
    }

    @Test
    public void testJoinApisRequireFromAndPrecedeLaterClauses() {
        final SqlBuilder stagedSelect = PSC.select("id");
        assertThrows(IllegalStateException.class, () -> stagedSelect.join("orders"));
        assertEquals("SELECT id FROM users", stagedSelect.from("users").build().query());

        final SqlBuilder filteredSelect = PSC.select("id").from("users").where(Filters.eq("active", true));
        assertThrows(IllegalStateException.class, () -> filteredSelect.leftJoin("orders"));
        assertEquals("SELECT id FROM users WHERE active = ?", filteredSelect.build().query());

        assertThrows(IllegalStateException.class, () -> PSC.update("users").join("orders"));
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").orderBy("id").innerJoin(Account.class));
    }

    @Test
    public void testStandaloneClausesRequireFromAndPreserveSqlClauseOrder() {
        final SqlBuilder stagedSelect = PSC.select("id");
        assertThrows(IllegalStateException.class, () -> stagedSelect.where("active = 1"));
        assertEquals("SELECT id FROM users WHERE active = 1", stagedSelect.from("users").where("active = 1").build().query());

        final SqlBuilder ordered = PSC.select("id").from("users").orderBy("id");
        assertThrows(IllegalStateException.class, () -> ordered.where("active = 1"));
        assertEquals("SELECT id FROM users ORDER BY id", ordered.build().query());

        assertThrows(IllegalStateException.class, () -> PSC.select("id").groupBy("id"));
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").having("COUNT(*) > 0").groupBy("id"));
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").orderBy("id").having("COUNT(*) > 0"));
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").limit(5).orderBy("id"));
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").forUpdate().limit(5));
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").forUpdate().fetchFirstRows(5));
        assertThrows(IllegalStateException.class, () -> PSC.update("users").set("active = true").forUpdate());

        // Pagination before the terminal locking clause is valid for the supported LIMIT-style syntax.
        assertEquals("SELECT id FROM users LIMIT 5 FOR UPDATE", PSC.select("id").from("users").limit(5).forUpdate().build().query());
    }

    @Test
    public void testTopLevelTableAliasDetectionIgnoresNonAliasSyntaxAndTrailingComments() {
        final SqlBuilder tableFunction = PSC.select("firstName").from("unnest (items)", Account.class);
        assertNull(tableFunction._tableAlias);
        assertFalse(tableFunction.build().query().contains("(items).first_name"));

        final SqlBuilder spacedQualification = PSC.select("firstName").from("catalog . account", Account.class);
        assertNull(spacedQualification._tableAlias);
        assertFalse(spacedQualification.build().query().contains("account.first_name"));

        final SqlBuilder tableHint = PSC.select("firstName").from("account WITH (NOLOCK)", Account.class);
        assertNull(tableHint._tableAlias);
        assertFalse(tableHint.build().query().contains("(NOLOCK).first_name"));

        final SqlBuilder implicitAlias = PSC.select("firstName").from("account a /* shard hint */", Account.class);
        assertEquals("a", implicitAlias._tableAlias);
        final String implicitSql = implicitAlias.build().query();
        assertTrue(implicitSql.contains("a.first_name"));
        assertTrue(implicitSql.endsWith("FROM account a /* shard hint */"));

        final SqlBuilder explicitAlias = PSC.select("firstName").from("account AS a /* shard hint */", Account.class);
        assertEquals("a", explicitAlias._tableAlias);
        final String explicitSql = explicitAlias.build().query();
        assertTrue(explicitSql.contains("a.first_name"));
        assertTrue(explicitSql.endsWith("FROM account AS a /* shard hint */"));

        final SqlBuilder temporalAlias = PSC.select("firstName").from("account FOR SYSTEM_TIME AS OF '2026-01-01' AS a", Account.class);
        assertEquals("a", temporalAlias._tableAlias);
        assertTrue(temporalAlias.build().query().contains("a.first_name"));
    }

    @Test
    public void testTopLevelAliasScannerBackslashEscapesOnlyInSingleQuotedLiterals() {
        // A backslash before the closing quote of a double-quoted or backtick-quoted identifier does NOT
        // escape that quote (backslash escaping applies only inside single-quoted string literals), so the
        // identifier ends at that quote and the trailing token is a real top-level table alias.
        final SqlBuilder doubleQuoted = PSC.select("firstName").from("\"a\\\" t", Account.class);
        assertEquals("t", doubleQuoted._tableAlias);
        final String doubleQuotedSql = doubleQuoted.build().query();
        assertTrue(doubleQuotedSql.contains("t.first_name"), doubleQuotedSql);
        assertTrue(doubleQuotedSql.endsWith("FROM \"a\\\" t"), doubleQuotedSql);

        final SqlBuilder backtickQuoted = PSC.select("firstName").from("`a\\` t", Account.class);
        assertEquals("t", backtickQuoted._tableAlias);
        assertTrue(backtickQuoted.build().query().contains("t.first_name"));
    }

    @Test
    public void testOnAndUsingRequireCompatibleUnconnectedJoin() {
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").on("users.id = orders.user_id"));
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").using("id"));
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").crossJoin("orders").on("users.id = orders.user_id"));
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").naturalJoin("orders").using("id"));

        final SqlBuilder joined = PSC.select("id").from("users").join("orders").on("users.id = orders.user_id");
        assertThrows(IllegalStateException.class, () -> joined.using("id"));
        assertEquals("SELECT id FROM users JOIN orders ON users.id = orders.user_id", joined.build().query());
    }

    @Test
    public void testQualifiedJoinMustBeCompletedBeforeStatementCanAdvance() {
        final SqlBuilder builder = PSC.select("u.id").from("users u").join("orders o");

        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(IllegalStateException.class, () -> builder.where(Filters.eq("u.active", true)));
        assertThrows(IllegalStateException.class, () -> builder.join("payments p"));
        assertThrows(IllegalStateException.class, () -> builder.union("SELECT id FROM archived_users"));

        // Every rejection is side-effect free; the original join can still be completed and built.
        assertEquals("SELECT u.id FROM users u JOIN orders o ON u.id = o.user_id", builder.on("u.id = o.user_id").build().query());
    }

    @Test
    public void testRawJoinWithInlineConnectorClosesConnectorSlot() {
        final SqlBuilder inlineOn = PSC.select("*").from("users u").join("orders o ON u.id = o.user_id");
        assertThrows(IllegalStateException.class, () -> inlineOn.on("o.active = 1"));
        assertThrows(IllegalStateException.class, () -> inlineOn.using("id"));
        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", inlineOn.build().query());

        final SqlBuilder inlineUsing = PSC.select("*").from("users u").leftJoin("orders o USING (user_id)");
        assertThrows(IllegalStateException.class, () -> inlineUsing.on("u.id = o.user_id"));
        assertEquals("SELECT * FROM users u LEFT JOIN orders o USING (user_id)", inlineUsing.build().query());

        final String nestedJoin = "(SELECT o.id FROM orders o JOIN order_items i ON o.id = i.order_id WHERE o.note = 'USING') nested /* ON USING */";
        assertEquals("SELECT * FROM users u JOIN " + nestedJoin + " ON u.id = nested.id",
                PSC.select("*").from("users u").join(nestedJoin).on("u.id = nested.id").build().query());

        assertEquals("SELECT * FROM users u INNER JOIN orders \"ON\" ON u.id = \"ON\".user_id",
                PSC.select("*").from("users u").innerJoin("orders \"ON\"").on("u.id = \"ON\".user_id").build().query());
    }

    @Test
    public void testPSCWithWhere() {
        String sql = PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testPSCWithMultipleConditions() {
        String sql = PSC.select("*").from(Account.class).where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testPSCWithOrderBy() {
        String sql = PSC.select("*").from(Account.class).orderBy("firstName").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testPSCWithLimit() {
        String sql = PSC.select("*").from(Account.class).limit(10).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testPSCWithJoin() {
        String sql = PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("JOIN"));
    }

    @Test
    public void testPSCWithLeftJoin() {
        String sql = PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testPSCWithInnerJoin() {
        String sql = PSC.select("*").from("users").innerJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
    }

    @Test
    public void testPSCWithRightJoin() {
        String sql = PSC.select("*").from("users").rightJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testPSCWithFullJoin() {
        String sql = PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testPSCWithCrossJoin() {
        String sql = PSC.select("*").from("users").crossJoin("roles").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testPSCWithGroupBy() {
        String sql = PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("GROUP BY"));
    }

    @Test
    public void testPSCWithHaving() {
        String sql = PSC.select("department", "COUNT(*)").from("employees").groupBy("department").having(Filters.expr("COUNT(*) > 5")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("HAVING"));
    }

    @Test
    public void testPSCWithDistinct() {
        String sql = PSC.select("status").from(Account.class).distinct().build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testPSCComplexQuery() {
        String sql = PSC.select("u.id", "u.firstName", "COUNT(o.id) as order_count")
                .from("users u")
                .leftJoin("orders o")
                .on("u.id = o.user_id")
                .where(Filters.eq("u.status", "active"))
                .groupBy("u.id", "u.firstName")
                .having(Filters.expr("COUNT(o.id) > 0"))
                .orderBy("order_count", SortDirection.DESC)
                .limit(10)
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("LEFT JOIN"));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("GROUP BY"));
        assertTrue(sql.contains("HAVING"));
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testInsertInto() {
        String sql = PSC.insertInto(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testUpdate() {
        String sql = PSC.update(Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UPDATE"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFrom() {
        String sql = PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectWithAlias() {
        String sql = PSC.select("firstName AS fname", "lastName AS lname").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("AS"));
    }

    @Test
    public void testSelectWithMultipleTables() {
        String sql = PSC.select("*").from("users", "orders").build().query();
        assertNotNull(sql);
    }

    @Test
    public void testWhereWithOr() {
        String sql = PSC.select("*").from(Account.class).where(Filters.eq("status", "active").or(Filters.eq("status", "pending"))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testMultipleOrderBy() {
        String sql = PSC.select("*").from(Account.class).orderBy("lastName", "firstName").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testOrderByRejectsCommentToken() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy("id--").build().query());
    }

    @Test
    public void testPostgreSqlRemovePathOperatorIsNotAHashComment() {
        final String sql = Dsl.PSB.select("payload #- '{address,city}'").from("events").build().query();

        assertEquals("SELECT payload #- '{address,city}' FROM events", sql);
    }

    @Test
    public void testQuestionHashOperatorIsNotAHashComment() {
        assertEquals("SELECT payload ?# path FROM events", Dsl.PSB.select("payload ?# path").from("events").build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select("payload ?#/* comment */ path").from("events"));
    }

    @Test
    public void testSqlServerTemporaryTableIdentifiersRemainIntactInExpressions() {
        final Dsl sqlServerDsl = Dsl.forDialect(Dsl.PSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());

        final String localTempSql = sqlServerDsl.select("#stage.id").from("#stage").where("#stage.id = 1").build().query();
        assertTrue(localTempSql.startsWith("SELECT #stage.id"));
        assertTrue(localTempSql.contains("FROM #stage WHERE #stage.id = 1"));

        final String globalTempSql = sqlServerDsl.select("##stage.id").from("##stage").where("##stage.id = 1").build().query();
        assertTrue(globalTempSql.startsWith("SELECT ##stage.id"));
        assertTrue(globalTempSql.contains("FROM ##stage WHERE ##stage.id = 1"));

        final SqlBuilder aliasedGlobalTemp = sqlServerDsl.select("firstName").from("##stage s", Account.class);
        assertEquals("s", aliasedGlobalTemp._tableAlias);
        final String aliasedGlobalTempSql = aliasedGlobalTemp.build().query();
        assertTrue(aliasedGlobalTempSql.contains("s.firstName"), aliasedGlobalTempSql);
        assertTrue(aliasedGlobalTempSql.endsWith("FROM ##stage s"));
        assertThrows(IllegalArgumentException.class, () -> sqlServerDsl.select("#stage.id /* comment */").from("#stage"));
    }

    @Test
    public void testCommentTokensInsideSqlServerBracketIdentifierAreAllowed() {
        assertEquals("SELECT [a--b] FROM records", Dsl.PSB.select("[a--b]").from("records").build().query());
        assertEquals("SELECT [a]]--b] FROM records", Dsl.PSB.select("[a]]--b]").from("records").build().query());
    }

    @Test
    public void testCommentTokenAfterBackslashTerminatedQuotedIdentifierIsRejected() {
        // A backslash before the closing quote of a double-quoted or backtick-quoted identifier does NOT escape
        // that quote (backslash escaping applies only inside single-quoted string literals), so the closing quote
        // terminates the identifier and the trailing "--" is a real SQL comment token that must be rejected.
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").join("orders").using("\"a\\\" -- x"));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").join("orders").using("`a\\` -- x"));

        // A comment-like token that is genuinely inside a quoted identifier is still allowed.
        final String sql = PSC.select("*").from("users").join("orders").using("\"a--b\"").build().query();
        assertTrue(sql.contains("a--b"));
    }

    @Test
    public void testCommentTokenAfterBackslashEscapedQuoteInsideSingleQuotedLiteralIsRejected() {
        // Inside a single-quoted string literal a backslash DOES escape the following quote (\'), so the next
        // quote closes the string and a trailing comment token (-- or /* */) must be rejected.
        // Regression: the scanner previously misread "\''" as a doubled-quote ('') escape and stayed "inside"
        // the string, hiding the trailing comment token from the guard.
        assertThrows(IllegalArgumentException.class, () -> PSC.select("note = '\\'' -- x").from("docs").build().query());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("note = '\\'' /* x */").from("docs").build().query());

        // The backslash-escaped-quote literal on its own (no trailing comment) is still accepted.
        final String okSql = PSC.select("note = '\\''").from("docs").build().query();
        assertNotNull(okSql);
    }

    @Test
    public void testOrderByRejectsEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy().build().query());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy(Collections.emptyList()).build().query());
    }

    @Test
    public void testOrderByAsc() {
        String sql = PSC.select("*").from(Account.class).orderBy("firstName", SortDirection.ASC).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testOrderByDesc() {
        String sql = PSC.select("*").from(Account.class).orderBy("createdTime", SortDirection.DESC).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testLimitWithOffset() {
        String sql = PSC.select("*").from(Account.class).limit(20, 10).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testFromWithEntityClass() {
        String sql = PSC.select("*").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testJoinWithEntityClass() {
        String sql = PSC.select("*").from(Account.class).join(Account.class).on("a.id = b.parent_id").build().query();
        assertNotNull(sql);
    }

    @Test
    public void testIntoWithTableName() {
        String sql = PSC.insert("id", "name").into("accounts").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testIntoRejectsEmptyTableName() {
        assertThrows(IllegalArgumentException.class, () -> PSC.insert("id").into("").build().query());
    }

    @Test
    public void testUpdateWithSet() {
        String sql = PSC.update("accounts").set("status", "inactive", "updated_at", SqlExpression.of("NOW()")).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFromWithTable() {
        String sql = PSC.deleteFrom("accounts").where(Filters.eq("status", "deleted")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectCount() {
        String sql = PSC.select(AbstractQueryBuilder.COUNT_ALL).from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("count(*)"));
    }

    @Test
    public void testSelectAll() {
        String sql = PSC.select(AbstractQueryBuilder.ALL).from(Account.class).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testChainedAndOr() {
        String sql = PSC.select("*")
                .from(Account.class)
                .where(Filters.eq("status", "active").and(Filters.gt("age", 18)).or(Filters.eq("role", "admin")))
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testMultipleJoins() {
        String sql = PSC.select("*")
                .from("users u")
                .innerJoin("orders o")
                .on("u.id = o.user_id")
                .leftJoin("products p")
                .on("o.product_id = p.id")
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testLimitWithOffsetRejectsSecondOffset() {
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").limit(10, 5).offset(2));
    }

    @Test
    public void testAppendLimitConditionWithExpression() {
        String sql = PSC.select("*").from("users").append(new Limit("10 OFFSET 20")).build().query();
        assertTrue(sql.endsWith("LIMIT 10 OFFSET 20"));
    }

    @Test
    public void testAppendConditionAfterWhereThrowsDuplicateWhere() {
        assertThrows(IllegalStateException.class,
                () -> PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.eq("name", "Alice")).build().query());
    }

    @Test
    public void testAppendWhereClauseAfterWhereThrows() {
        assertThrows(IllegalStateException.class,
                () -> PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.where(Filters.eq("name", "Alice"))).build().query());
    }

    @Test
    public void testAppendCriteriaAfterWhereThrowsWhenCriteriaHasWhere() {
        Criteria criteria = Criteria.builder().where(Filters.eq("name", "Alice")).build();

        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").where(Filters.eq("id", 1)).append(criteria).build().query());
    }

    @Test
    public void testAppendCriteriaPreflightsJoinAndSetOperationPlacement() {
        final Criteria joinCriteria = Criteria.builder().join("orders", Filters.expr("users.id = orders.user_id")).build();
        final SqlBuilder filtered = PSC.select("id").from("users").where(Filters.eq("active", true));

        assertThrows(IllegalStateException.class, () -> filtered.append(joinCriteria));
        assertEquals("SELECT id FROM users WHERE active = ?", filtered.build().query());

        final Criteria unionCriteria = Criteria.builder().union(Filters.subQuery("SELECT id FROM archived_users")).build();
        final SqlBuilder ordered = PSC.select("id").from("users").orderBy("id");

        assertThrows(IllegalStateException.class, () -> ordered.append(unionCriteria));
        assertEquals("SELECT id FROM users ORDER BY id", ordered.build().query());
    }

    @Test
    public void testAppendCriteriaPreflightsRelativeClauseOrderWithoutMutation() {
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class, PSC.select("department").from("users").groupBy("department"),
                Criteria.builder().where(Filters.eq("active", true)).build(), "SELECT department FROM users GROUP BY department");
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class, PSC.select("department").from("users").groupBy("department").having("COUNT(*) > 1"),
                Criteria.builder().groupBy("region").build(), "SELECT department FROM users GROUP BY department HAVING COUNT(*) > 1");
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class, PSC.select("id").from("users").orderBy("id"),
                Criteria.builder().having(Filters.gt("COUNT(*)", 1)).build(), "SELECT id FROM users ORDER BY id");
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class, PSC.select("id").from("users").limit(10), Criteria.builder().orderBy("id").build(),
                "SELECT id FROM users LIMIT 10");
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class, PSC.select("id").from("users").forUpdate(), Criteria.builder().limit(10).build(),
                "SELECT id FROM users FOR UPDATE");
    }

    @Test
    public void testAppendCriteriaValidatesEverySetOperationOperandWithoutMutation() {
        final Criteria unsafe = Criteria.builder().union(Filters.subQuery("UPDATE archived_users SET active = false")).build();
        assertCriteriaRejectedWithoutMutation(IllegalArgumentException.class, PSC.select("id").from("users"), unsafe, "SELECT id FROM users");

        final Criteria incomplete = Criteria.builder().union(Filters.subQuery("archived_users")).build();
        assertCriteriaRejectedWithoutMutation(IllegalArgumentException.class, PSC.select("id").from("users"), incomplete, "SELECT id FROM users");

        final Criteria unsafeSecondOperand = Criteria.builder()
                .union(Filters.subQuery("SELECT id FROM archived_users"))
                .unionAll(Filters.subQuery("SELECT id FROM deleted_users; DELETE FROM deleted_users"))
                .build();
        assertCriteriaRejectedWithoutMutation(IllegalArgumentException.class, PSC.select("id").from("users"), unsafeSecondOperand, "SELECT id FROM users");
    }

    @Test
    public void testCompletedCriteriaSetOperationAllowsOnlyCompoundResultClauses() {
        final Criteria union = Criteria.builder().union(Filters.subQuery("SELECT id FROM archived_users")).build();
        final SqlBuilder compound = PSC.select("id").from("users").append(union);

        assertThrows(IllegalStateException.class, () -> compound.where(Filters.eq("active", true)));
        assertThrows(IllegalStateException.class, () -> compound.groupBy("id"));
        assertThrows(IllegalStateException.class, () -> compound.having("COUNT(*) > 1"));
        assertThrows(IllegalStateException.class, () -> compound.join("orders"));

        assertEquals("SELECT id FROM users UNION SELECT id FROM archived_users ORDER BY id LIMIT 10", compound.orderBy("id").limit(10).build().query());
    }

    private static <E extends RuntimeException> void assertCriteriaRejectedWithoutMutation(final Class<E> expectedType, final SqlBuilder builder,
            final Criteria criteria, final String expectedSql) {
        assertThrows(expectedType, () -> builder.append(criteria));
        assertEquals(expectedSql, builder.build().query());
    }

    @Test
    public void testAppendStandaloneSetOperationClauseValidatesLikeSetOperationMethods() {
        // The operand must be a complete read-only SELECT sub-query, exactly as on the
        // union(String)/union(builder)/Criteria set-operation routes.
        final SqlBuilder unsafeOperand = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> unsafeOperand.append(new Union(Filters.subQuery("UPDATE archived_users SET active = false"))));
        assertEquals("SELECT id FROM users", unsafeOperand.build().query());

        // Position rules: a set operator cannot follow ORDER BY (or pagination/FOR UPDATE) ...
        final SqlBuilder ordered = PSC.select("id").from("users").orderBy("id");
        assertThrows(IllegalStateException.class, () -> ordered.append(new Union(Filters.subQuery("SELECT id FROM archived_users"))));
        assertEquals("SELECT id FROM users ORDER BY id", ordered.build().query());

        // ... and requires a SELECT segment completed by from(...) on its left-hand side.
        final SqlBuilder staged = PSC.select("id");
        assertThrows(IllegalStateException.class, () -> staged.append(new Union(Filters.subQuery("SELECT id FROM archived_users"))));
        assertEquals("SELECT id FROM users", staged.from("users").build().query());

        // A valid standalone Union still renders and completes the set operation: WHERE may no longer
        // follow, exactly as on the other set-operation routes.
        final SqlBuilder compound = PSC.select("id").from("users").append(new Union(Filters.subQuery("SELECT id FROM archived_users")));
        assertThrows(IllegalStateException.class, () -> compound.where(Filters.eq("active", true)));
        assertEquals("SELECT id FROM users UNION SELECT id FROM archived_users", compound.build().query());
    }

    @Test
    public void testAppendCriteriaJoinTracksFollowUpOnUsingEligibility() {
        // An open qualified JOIN must be completed before a Criteria join may follow.
        final SqlBuilder openJoin = PSC.select("u.id").from("users u").join("orders o");
        assertThrows(IllegalStateException.class,
                () -> openJoin.append(Criteria.builder().join("payments p", Filters.expr("p.order_id = o.id")).build()));

        // A criteria join always carries its own ON/USING (Join constructor invariant) and therefore closes
        // the connector slot: a follow-up on() must not emit a second ON.
        final SqlBuilder conditionedJoin = PSC.select("u.id")
                .from("users u")
                .join("orders o")
                .on("u.id = o.user_id")
                .append(Criteria.builder().join("payments p", Filters.expr("p.order_id = o.id")).build());
        assertThrows(IllegalStateException.class, () -> conditionedJoin.on("x = y"));
        assertEquals("SELECT u.id FROM users u JOIN orders o ON u.id = o.user_id JOIN payments p ON p.order_id = o.id", conditionedJoin.build().query());

        // A condition-less qualified criteria join is no longer constructible, so the "re-opened slot"
        // scenario cannot arise; the same holds for a raw join entity carrying its ON inline.
        assertThrows(IllegalArgumentException.class, () -> Criteria.builder().join("payments p"));
        assertThrows(IllegalArgumentException.class, () -> Criteria.builder().join("payments p ON p.user_id = u.id"));

        // A CROSS JOIN never accepts a connector, no matter how it is appended.
        final SqlBuilder crossJoined = PSC.select("u.id").from("users u").append(Criteria.builder().join(Filters.crossJoin("payments")).build());
        assertThrows(IllegalStateException.class, () -> crossJoined.on("payments.user_id = u.id"));
        assertEquals("SELECT u.id FROM users u CROSS JOIN payments", crossJoined.build().query());
    }

    @Test
    public void testCollectionAndMapArgumentsRenderTheirValidatedSnapshots() {
        assertEquals("SELECT * FROM users",
                PSC.select("*").from(new ChangingCollection<>(Collections.singletonList("users"), Collections.singletonList(" "))).build().query());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(new NominallyNonEmptyCollection<>()));

        assertEquals("SELECT * FROM users JOIN orders USING (user_id)",
                PSC.select("*")
                        .from("users")
                        .join("orders")
                        .using(new ChangingCollection<>(Collections.singletonList("user_id"), Collections.singletonList(" ")))
                        .build()
                        .query());
        assertEquals("SELECT category FROM products GROUP BY category",
                PSC.select("category")
                        .from("products")
                        .groupBy(new ChangingCollection<>(Collections.singletonList("category"), Collections.singletonList(" ")))
                        .build()
                        .query());
        assertEquals("SELECT id FROM users ORDER BY id",
                PSC.select("id")
                        .from("users")
                        .orderBy(new ChangingCollection<>(Collections.singletonList("id"), Collections.singletonList(" ")))
                        .build()
                        .query());
        assertEquals("SELECT id FROM users UNION SELECT id FROM admins",
                PSC.select("id")
                        .from("users")
                        .unionSelect(new ChangingCollection<>(Collections.singletonList("id"), Collections.singletonList(" ")))
                        .from("admins")
                        .build()
                        .query());

        assertEquals("SELECT category FROM products GROUP BY category ASC",
                PSC.select("category").from("products").groupBy(new ChangingMap<>("category", SortDirection.ASC, " ", SortDirection.DESC)).build().query());
        assertEquals("SELECT id FROM users ORDER BY id DESC",
                PSC.select("id").from("users").orderBy(new ChangingMap<>("id", SortDirection.DESC, " ", SortDirection.ASC)).build().query());

        final AbstractQueryBuilder.SP update = PSC.update("users").set(new ChangingMap<>("name", "Alice", " ", "corrupted")).build();
        assertEquals("UPDATE users SET name = ?", update.query());
        assertEquals(Collections.singletonList("Alice"), update.parameters());
    }

    @Test
    public void testAppendLimitExpressionAfterLimitThrows() {
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").limit(10).append(new Limit("5")).build().query());
    }

    @Test
    public void testSelectAllowsHashJsonOperators() {
        String sql = PSC.select("payload #>> '{meta,status}'").from("docs").build().query();
        assertTrue(sql.contains("#>>"));
    }

    @Test
    public void testSelectAllowsCommentLikeTokenInsideQuotedLiteral() {
        String sql = PSC.select("CASE WHEN note = '--literal' THEN 1 ELSE 0 END").from("docs").build().query();
        assertTrue(sql.contains("'--literal'"));
    }

    @Test
    public void testUpdateAllowsIbatisPlaceholderExpression() {
        String sql = PSC.update("users").set("name = #{name}").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("#{name}"));
    }

    @Test
    public void testGroupByRejectsEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy().build().query());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy(Collections.emptyList()).build().query());
    }

    @Test
    public void testWhereRejectsNullStringExpression() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").where((String) null));
    }

    @Test
    public void testClauseBuildersRejectBlankStringFragments() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").join("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").join("orders").on("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").join("orders").using("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").where("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy(Collections.singletonMap("   ", SortDirection.ASC)));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy("id").having("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy(Collections.singletonMap("   ", SortDirection.ASC)));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").append("   "));
    }

    @Test
    public void testInsertAndUpdateRejectBlankTableAndSetFragments() {
        assertThrows(IllegalArgumentException.class, () -> PSC.insert("id").into("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set(Arrays.asList("name", "   ")));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set(Collections.emptyMap()));
    }

    @Test
    public void testDslSelectAndInsertRejectBlankColumnFragments() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id", "   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> PSC.select(Collections.singletonMap("   ", "alias")));

        assertThrows(IllegalArgumentException.class, () -> PSC.insert("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.insert("id", "   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.insert(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> PSC.insert(Collections.singletonMap("   ", 1)));
        assertThrows(IllegalArgumentException.class, () -> PSC.insert((Object) "   "));

        Map<String, Object> props = Collections.singletonMap("id", 1);
        assertThrows(IllegalArgumentException.class, () -> PSC.insert((Object) props, Collections.singleton("id")));

        Map<String, Object> blankProps = Collections.singletonMap("   ", 1);
        assertThrows(IllegalArgumentException.class, () -> PSC.insert((Object) blankProps, Collections.singleton("   ")));

        Map<Object, Object> nonStringProps = Collections.singletonMap(1, "invalid column");
        assertThrows(IllegalArgumentException.class, () -> PSC.insert((Object) nonStringProps));
    }

    @Test
    public void testSetOperationsAndDirectionsRejectInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").union("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").unionAll("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").intersect("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").except("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").minus("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy("id", null));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy("id", null));
    }

    @Test
    public void testFetchNextRowsAndFetchFirstRowsAreMutuallyExclusive() {
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").orderBy("id").fetchNextRows(10).fetchFirstRows(5).build().query());
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).fetchNextRows(5).build().query());
    }

    @Test
    public void testRowLimitApisRejectNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").limit(-1));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").limit(10, -1));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").offset(-1));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").offsetRows(-1));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").fetchNextRows(-1));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").fetchFirstRows(-1));
    }

    @Test
    public void testChainedSetCollectionCallsIncludeComma() {
        String sql = PSC.update("users").set("firstName").set("lastName").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("first_name = ?"), "first_name assignment missing: " + sql);
        assertTrue(sql.contains("last_name = ?"), "last_name assignment missing: " + sql);
        int firstIdx = sql.indexOf("first_name");
        int commaIdx = sql.indexOf(',', firstIdx);
        int secondIdx = sql.indexOf("last_name");
        assertTrue(commaIdx > 0 && commaIdx < secondIdx, "Comma must separate the two SET assignments: " + sql);
    }

    @Test
    public void testChainedSetMapCallsIncludeComma() {
        java.util.Map<String, Object> m1 = java.util.Collections.singletonMap("firstName", "John");
        java.util.Map<String, Object> m2 = java.util.Collections.singletonMap("lastName", "Doe");
        String sql = PSC.update("users").set(m1).set(m2).where(Filters.eq("id", 1)).build().query();
        int firstIdx = sql.indexOf("first_name");
        int commaIdx = sql.indexOf(',', firstIdx);
        int secondIdx = sql.indexOf("last_name");
        assertTrue(commaIdx > 0 && commaIdx < secondIdx, "Comma must separate map-based SET assignments: " + sql);
    }

    @Test
    public void testIsDefaultIdPropValueFractionalNumberNotTreatedAsZero() {
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(null));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0L));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(java.math.BigDecimal.ZERO));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0.0));
        assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0.0f));
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(new java.math.BigDecimal("0.9")),
                "BigDecimal 0.9 has longValue()=0 but must not be treated as default ID");
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(new java.math.BigDecimal("0.1")));
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(0.5), "double 0.5 must not be treated as default ID");
        assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(0.1f), "float 0.1 must not be treated as default ID");
    }

    @Test
    public void testDoubleHashNotTreatedAsSqlCommentInExpressions() {
        // ## is a whitelisted two-char token; the second # must not be re-examined as lone #
        String sql = PSC.select("*").from("users").where(Filters.expr("status = '##ACTIVE##'")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("##ACTIVE##"), "## inside value must not be rejected as SQL comment");
    }

    @Test
    public void testSetWithEntityClass() {
        String sql = PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE account SET"), "set(Class) must render an UPDATE ... SET: " + sql);
        assertTrue(sql.contains("first_name = ?"), "set(Class) must include updatable properties: " + sql);
    }

    @Test
    public void testSetWithEntityClassAndExcludedPropNames() {
        Set<String> excluded = Collections.singleton("firstName");
        String sql = PSC.update("account").set(Account.class, excluded).where(Filters.eq("id", 1)).build().query();

        assertFalse(sql.contains("first_name = ?"), "excluded property must not be updated: " + sql);
        assertTrue(sql.contains("last_name = ?"), "non-excluded property must be updated: " + sql);
    }

    @Test
    public void testNamedParameterHandlerOnDialect() {
        final Dsl dsl = Dsl
                .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, propName) -> sb.append("#{").append(propName).append("}")).build());
        final String sql = dsl.select("name").from("users").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("#{id}"));
        assertTrue(NSC.select("name").from("users").where(Filters.eq("id", 1)).build().query().contains(":id"));
    }

    @Test
    public void testNamedParameterHandlerIsScopedAcrossInterleavedBuilders() {
        final Dsl myBatisStyle = Dsl
                .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, propName) -> sb.append("#{").append(propName).append("}")).build());

        final SqlBuilder customBuilder = myBatisStyle.select("name").from("users");
        final SqlBuilder defaultBuilder = NSC.select("name").from("users");

        assertTrue(customBuilder.where(Filters.eq("id", 1)).build().query().contains("#{id}"));
        assertTrue(defaultBuilder.where(Filters.eq("id", 1)).build().query().contains(":id"));
    }

    @Test
    public void testTokenizerConfigIsCarriedBySqlDialect() {
        final SqlParser.TokenizerConfig tokenizerConfig = SqlParser.tokenizerConfigBuilder().withSeparator("::").build();
        final Dsl dsl = Dsl.forDialect(Dsl.SCSB.sqlDialect().toBuilder().tokenizerConfig(tokenizerConfig).build());
        final SqlBuilder builder = dsl.select("payload::jsonb").from("events");

        assertEquals(tokenizerConfig, dsl.sqlDialect().tokenizerConfig());
        assertEquals(tokenizerConfig, builder._tokenizer.tokenizerConfig());
        assertTrue(builder.build().query().contains("payload::jsonb"));
    }

    @Test
    public void testDialectTokenizerConfigAffectsRawSubQueryInspection() {
        final SqlParser.TokenizerConfig tokenizerConfig = SqlParser.tokenizerConfigBuilder().withSeparator("::").build();
        final Dsl dsl = Dsl.forDialect(Dsl.SCSB.sqlDialect().toBuilder().tokenizerConfig(tokenizerConfig).build());

        assertEquals("SELECT id FROM users UNION SELECT::1", dsl.select("id").from("users").union("SELECT::1").build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.select("id").from("users").union("SELECT::1"));
    }

    @Test
    public void testDialectTokenizerConfigAlsoGovernsSetOperationReadOnlyValidation() {
        final SqlParser.TokenizerConfig tokenizerConfig = SqlParser.tokenizerConfigBuilder().withSeparator("#foo").build();
        final Dsl dsl = Dsl.forDialect(Dsl.SCSB.sqlDialect().toBuilder().tokenizerConfig(tokenizerConfig).build());

        // Under the default tokenizer, "#foo" starts a hash comment and hides the later UPDATE.
        // For this dialect it is a separator, so the full multi-statement input must be inspected.
        assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from("users").union("SELECT 1 #foo ; UPDATE users SET active = false"));
    }

    @Test
    public void testDialectScopedConfigurationSurvivesToBuilder() {
        final BiConsumer<StringBuilder, String> handler = (sb, name) -> sb.append("${").append(name).append('}');
        final SqlParser.TokenizerConfig tokenizerConfig = SqlParser.tokenizerConfigBuilder().withSeparator("::").build();
        final SqlDialect dialect = NSC.sqlDialect().toBuilder().namedParameterHandler(handler).tokenizerConfig(tokenizerConfig).build();
        final SqlDialect copy = dialect.toBuilder().build();

        assertSame(handler, copy.namedParameterHandler());
        assertSame(tokenizerConfig, copy.tokenizerConfig());
        assertEquals(dialect, copy);
        assertEquals(dialect.hashCode(), copy.hashCode());
    }

    @Test
    public void testNullTokenizerConfigUsesImmutableDefault() {
        final SqlBuilder builder = Dsl.forDialect(NSC.sqlDialect().toBuilder().tokenizerConfig(null).build()).select("id").from("users");

        assertSame(SqlParser.defaultTokenizerConfig(), builder._tokenizer.tokenizerConfig());
    }

    @Test
    public void testNamedParameterHandlersRemainIsolatedDuringParallelUse() {
        final Dsl customDsl = Dsl
                .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
        final List<String> statements = IntStream.range(0, 128)
                .parallel()
                .mapToObj(i -> (i & 1) == 0 ? customDsl.select("id").from("users").where(Filters.eq("id", i)).build().query()
                        : NSC.select("id").from("users").where(Filters.eq("id", i)).build().query())
                .toList();

        for (int i = 0; i < statements.size(); i++) {
            assertTrue(statements.get(i).endsWith((i & 1) == 0 ? "${id}" : ":id"));
        }
    }

    @Test
    public void testSelectModifier() {
        final String sql = PSC.select("*").selectModifier("TOP 5").from("users").build().query();

        assertTrue(sql.contains("SELECT TOP 5"));
    }

    @Test
    public void testNaturalJoin_String() {
        final String sql = PSC.select("*").from("users").naturalJoin("orders").build().query();

        assertTrue(sql.contains("NATURAL JOIN orders"));
    }

    @Test
    public void testNaturalJoin_EntityClass() {
        final String sql = PSC.select("*").from(Account.class).naturalJoin(Account.class).build().query();

        assertTrue(sql.contains("NATURAL JOIN"));
        assertTrue(sql.toLowerCase().contains("account"));
    }

    @Test
    public void testNaturalJoin_EntityClassAlias() {
        final String sql = PSC.select("*").from(Account.class, "a").naturalJoin(Account.class, "b").build().query();

        assertTrue(sql.contains("NATURAL JOIN"));
        assertTrue(sql.contains(" b"));
    }

    @Test
    public void testUsing() {
        final String sql = PSC.select("*").from("users").join("orders").using("user_id").build().query();

        assertTrue(sql.contains("USING (user_id)"));
    }

    @Test
    public void testOffsetRows() {
        final String sql = PSC.select("*").from("users").orderBy("id").offsetRows(20).build().query();

        assertTrue(sql.contains("OFFSET 20 ROWS"));
    }

    @Test
    public void testFetchNextRows() {
        final String sql = PSC.select("*").from("users").orderBy("id").offsetRows(0).fetchNextRows(10).build().query();

        assertTrue(sql.contains("FETCH NEXT 10 ROWS ONLY"));
    }

    @Test
    public void testFetchFirstRows() {
        final String sql = PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build().query();

        assertTrue(sql.contains("FETCH FIRST 10 ROWS ONLY"));
    }

    @Test
    public void testAppendIf_Condition() {
        final String withCondition = PSC.select("*").from("users").appendIf(true, Filters.eq("status", "ACTIVE")).build().query();
        final String withoutCondition = PSC.select("*").from("users").appendIf(false, Filters.eq("status", "ACTIVE")).build().query();

        assertTrue(withCondition.contains("status"));
        assertTrue(!withoutCondition.contains("status"));
    }

    @Test
    public void testAppendIf_String() {
        final String withExpression = PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(true, " FOR UPDATE").build().query();
        final String withoutExpression = PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(false, " FOR UPDATE").build().query();

        assertTrue(withExpression.contains("FOR UPDATE"));
        assertTrue(!withoutExpression.contains("FOR UPDATE"));
    }

    @Test
    public void testAppendIfOrElse_Condition() {
        final AbstractQueryBuilder.SP trueBranch = PSC.select("*")
                .from("users")
                .appendIfOrElse(true, Filters.eq("status", "ACTIVE"), Filters.eq("status", "INACTIVE"))
                .build();
        final AbstractQueryBuilder.SP falseBranch = PSC.select("*")
                .from("users")
                .appendIfOrElse(false, Filters.eq("status", "ACTIVE"), Filters.eq("status", "INACTIVE"))
                .build();

        assertTrue(trueBranch.query().contains("WHERE"));
        assertTrue(falseBranch.query().contains("WHERE"));
        assertEquals(Arrays.asList("ACTIVE"), trueBranch.parameters());
        assertEquals(Arrays.asList("INACTIVE"), falseBranch.parameters());
    }

    @Test
    public void testAppendIfOrElse_String() {
        final String asc = PSC.select("*").from("users").appendIfOrElse(true, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
        final String desc = PSC.select("*").from("users").appendIfOrElse(false, " ORDER BY name ASC", " ORDER BY name DESC").build().query();

        assertTrue(asc.contains("ORDER BY name ASC"));
        assertTrue(desc.contains("ORDER BY name DESC"));
    }

    @Test
    public void testUnion_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .union(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("UNION"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testRejectedRewrittenSetOperationOperandLeavesParentMetadataReusable() {
        final boolean[] poisonRenamedToken = { true };
        final Dsl dsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
            if (poisonRenamedToken[0] && "id_2".equals(name)) {
                sb.append(":id_2; DELETE FROM audit_log");
            } else {
                sb.append(':').append(name);
            }
        }).build());
        final SqlBuilder parent = dsl.select("id").from("users").where(Filters.eq("id", 1));
        final SqlBuilder rejectedChild = dsl.select("id").from("archived_users").where(Filters.eq("id", 2));

        assertThrows(IllegalArgumentException.class, () -> parent.union(rejectedChild));

        poisonRenamedToken[0] = false;
        final AbstractQueryBuilder.SP sp = parent.union(dsl.select("id").from("active_users").where(Filters.eq("id", 3))).build();

        assertEquals("SELECT id FROM users WHERE id = :id UNION SELECT id FROM active_users WHERE id = :id_2", sp.query());
        assertEquals(Arrays.asList(1, 3), sp.parameters());
    }

    @Test
    public void testUnion_Query() {
        final String sql = PSC.select("id").from("users").union("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("UNION SELECT id FROM admins"));
    }

    @Test
    public void testUnion_SingleNonSubQueryStringRejected() {
        // union(String) is reserved for a complete SELECT sub-query; a bare column name is rejected up front
        // instead of silently becoming a column list that fails later with an unrelated "from() must be called" error.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").union("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testUnion_ParenthesizedFromLessQueryAccepted() {
        // "UNION (SELECT 1)" is valid SQL: a FROM-less query wrapped in balanced parentheses is as
        // complete an operand as the already-accepted unparenthesized union("SELECT 1").
        assertEquals("SELECT id FROM users UNION (SELECT 1)", PSC.select("id").from("users").union("(SELECT 1)").build().query());
        assertEquals("SELECT id FROM users UNION ((SELECT 1))", PSC.select("id").from("users").union("((SELECT 1))").build().query());
        assertEquals("SELECT id FROM users UNION (SELECT 1) /* operand */", PSC.select("id").from("users").union("(SELECT 1) /* operand */").build().query());

        // All five set operations (union/unionAll/intersect/except/minus) share the same operand check
        // (checkSetOperationSubQuery -> isSubQuery), so a second operation pins the shared path.
        assertEquals("SELECT id FROM users INTERSECT (SELECT id FROM t)", PSC.select("id").from("users").intersect("(SELECT id FROM t)").build().query());
    }

    @Test
    public void testUnion_ParenthesizedNonSelectStillRejected() {
        // A parenthesized column list, an unbalanced non-SELECT fragment, and an unbalanced
        // parenthesized SELECT all remain rejected with the pointed message.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").union("(id, name)"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));

        ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").union("(name"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));

        ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").union("(SELECT 1"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));

        ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").union("(SELECT 1) + 2"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
        assertFalse(AbstractQueryBuilder.isInlineQuery("(SELECT 1) + 2"));
        assertFalse(AbstractQueryBuilder.isInlineQuery("(SELECT 1) 'not trivia'"));
        assertFalse(AbstractQueryBuilder.isInlineQuery("(SELECT 1) /* unterminated"));
    }

    @Test
    public void testProtectedHelpersRejectNullArgumentsWithIllegalArgumentException() {
        // previously threw NullPointerException
        assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.isInlineQuery((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.isInlineQuery((String) null));
        assertFalse(AbstractQueryBuilder.isInlineQuery(new String[] { null, null }));

        assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.parseInsertEntity(null, "id", null));
        assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.getFromClause(null, NamingPolicy.SNAKE_CASE));
        assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.hasSubEntityToInclude(null, true));
        assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.hasSubEntityToInclude(null, false));

        final SqlBuilder builder = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> builder.mutateAtomically(null));
        assertThrows(IllegalArgumentException.class, () -> builder.appendInsertProps(null));
        assertThrows(IllegalArgumentException.class, () -> builder.appendInsertProps(null, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> builder.appendInsertProps(Collections.emptyMap(), null, -1));
        assertThrows(IllegalArgumentException.class, () -> builder.seedNamedParameterOccurrences(null));
        assertThrows(IllegalArgumentException.class, () -> builder.adoptNamedParameterOccurrences(null));
        assertThrows(IllegalArgumentException.class, () -> builder.appendColumnName(null, null, null, null, null, null, false, null, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> builder.normalizeColumnName((com.landawn.abacus.util.ImmutableMap<String, QueryUtil.ColumnInfo>) null, null));

        // The rejected calls left the builder usable.
        assertEquals("SELECT id FROM users", builder.build().query());
    }

    @Test
    public void testUnion_Collection() {
        final String sql = PSC.select("id").from("users").unionSelect(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("UNION SELECT id FROM admins"));
    }

    @Test
    public void testUnionAll_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .unionAll(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("UNION ALL"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testUnionAll_Query() {
        final String sql = PSC.select("id").from("users").unionAll("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
    }

    @Test
    public void testUnionAll_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").unionAll("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testUnionAll_Collection() {
        final String sql = PSC.select("id").from("users").unionAllSelect(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
    }

    @Test
    public void testIntersect_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .intersect(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("INTERSECT"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testIntersect_Query() {
        final String sql = PSC.select("id").from("users").intersect("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
    }

    @Test
    public void testIntersect_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").intersect("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testIntersect_Collection() {
        final String sql = PSC.select("id").from("users").intersectSelect(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
    }

    @Test
    public void testExcept_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .except(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("EXCEPT"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testExcept_Query() {
        final String sql = PSC.select("id").from("users").except("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
    }

    @Test
    public void testExcept_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").except("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testExcept_Collection() {
        final String sql = PSC.select("id").from("users").exceptSelect(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
    }

    @Test
    public void testMinus_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .minus(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("MINUS"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testMinus_Query() {
        final String sql = PSC.select("id").from("users").minus("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("MINUS SELECT id FROM admins"));
    }

    @Test
    public void testMinus_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").minus("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testMinus_Collection() {
        final String sql = PSC.select("id").from("users").minusSelect(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("MINUS SELECT id FROM admins"));
    }

    @Test
    public void testForUpdate() {
        final String sql = PSC.select("*").from("users").forUpdate().build().query();

        assertTrue(sql.contains("FOR UPDATE"));
    }

    @Test
    public void testForUpdate_idempotencyGuard() {
        // Calling forUpdate() twice must not produce "FOR UPDATE FOR UPDATE".
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").forUpdate().forUpdate());
    }

    @Test
    public void testSetWithEntityObject() {
        final Account a = new Account();
        a.setFirstName("F");
        a.setLastName("L");

        final String sql = PSC.update("account").set(a).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("first_name = ?"));
    }

    @Test
    public void testSetWithEntityObjectAndExcludedPropNames() {
        final Account a = new Account();
        a.setFirstName("F");
        a.setLastName("L");

        final Set<String> excluded = java.util.Set.of("lastName");
        final String sql = PSC.update("account").set(a, excluded).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("first_name = ?"));
        assertFalse(sql.contains("last_name = ?"));
    }

    @Test
    public void testSetObjectOverloadRejectsCollection() {
        assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((Object) Arrays.asList("firstName", "lastName")));
    }

    @Test
    public void testSetObjectOverloadRejectsArray() {
        assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((Object) new String[] { "firstName", "lastName" }));
    }

    @Test
    public void testIntoClass_alwaysSetsEntityClass() {
        // into(Class) should map property names to columns (entity class always set), matching from(...).
        final String sql = PSC.insert("firstName", "lastName").into(Account.class).build().query();

        assertTrue(sql.contains("first_name"));
        assertTrue(sql.contains("last_name"));
    }

    @Test
    public void testUsing_varargs() {
        final String sql = PSC.select("*").from("orders").join("order_items").using("order_id", "tenant_id").build().query();

        assertTrue(sql.contains("USING (order_id, tenant_id)"), sql);
    }

    @Test
    public void testUsing_collection() {
        final String sql = PSC.select("*").from("orders").join("order_items").using(Arrays.asList("order_id", "tenant_id")).build().query();

        assertTrue(sql.contains("USING (order_id, tenant_id)"), sql);
    }

    @Test
    public void testOn_varargsComposite() {
        final String sql = PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id", "u.tenant_id = o.tenant_id").build().query();

        assertTrue(sql.contains("ON (u.id = o.user_id) AND (u.tenant_id = o.tenant_id)"), sql);
    }

    @Test
    public void testOnVarargsPreservesDisjunctionPrecedence() {
        final String sql = PSC.select("*")
                .from("users u")
                .join("orders o")
                .on("u.id = o.user_id OR u.id = o.owner_id", "u.tenant_id = o.tenant_id OR o.public_flag = 1")
                .build()
                .query();

        assertEquals("SELECT * FROM users u JOIN orders o ON (u.id = o.user_id OR u.id = o.owner_id)"
                + " AND (u.tenant_id = o.tenant_id OR o.public_flag = 1)", sql);
    }

    @Test
    public void testOnSingleElementArrayMatchesSingleExpressionOverload() {
        final String expression = "u.id = o.user_id OR u.id = o.owner_id";

        assertEquals(PSC.select("*").from("users u").join("orders o").on(expression).build().query(),
                PSC.select("*").from("users u").join("orders o").on(new String[] { expression }).build().query());
    }

    @Test
    public void testGroupBy_mapIterationOrder() {
        final Map<String, SortDirection> groupings = new LinkedHashMap<>();
        groupings.put("category", SortDirection.ASC);
        groupings.put("brand", SortDirection.DESC);

        final String sql = PSC.select("category", "brand").from("products").groupBy(groupings).build().query();

        assertTrue(sql.indexOf("category ASC") < sql.indexOf("brand DESC"), sql);
    }

    @Test
    public void testOrderBy_mapIterationOrder() {
        final Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("lastName", SortDirection.ASC);
        orders.put("firstName", SortDirection.DESC);

        final String sql = PSC.select("*").from("users").orderBy(orders).build().query();

        assertTrue(sql.indexOf("ASC") < sql.indexOf("DESC"), sql);
    }

    @Test
    public void testApply_SPFunction() throws Exception {
        final List<Object> result = PSC.select("id").from("users").where(Filters.eq("id", 1)).apply(sp -> Arrays.asList(sp.query(), sp.parameters().size()));

        assertTrue(result.get(0).toString().contains("WHERE"));
        assertEquals(1, result.get(1));
    }

    @Test
    public void testApply_SqlAndParams() throws Exception {
        final String result = PSC.select("id").from("users").where(Filters.eq("id", 1)).apply((sql, params) -> sql + " / " + params.size());

        assertTrue(result.contains("WHERE"));
        assertTrue(result.endsWith("/ 1"));
    }

    @Test
    public void testAccept_SPConsumer() throws Exception {
        final String[] sqlHolder = new String[1];
        final int[] paramCount = new int[1];

        PSC.select("id").from("users").where(Filters.eq("id", 1)).accept(sp -> {
            sqlHolder[0] = sp.query();
            paramCount[0] = sp.parameters().size();
        });

        assertTrue(sqlHolder[0].contains("WHERE"));
        assertEquals(1, paramCount[0]);
    }

    @Test
    public void testAccept_SqlAndParams() throws Exception {
        final String[] sqlHolder = new String[1];
        final int[] paramCount = new int[1];

        PSC.select("id").from("users").where(Filters.eq("id", 1)).accept((sql, params) -> {
            sqlHolder[0] = sql;
            paramCount[0] = params.size();
        });

        assertTrue(sqlHolder[0].contains("WHERE"));
        assertEquals(1, paramCount[0]);
    }

    @Test
    public void testNullTerminalCallbacksDoNotConsumeBuilder() {
        final SqlBuilder applySp = PSC.select("id").from("users");
        final IllegalArgumentException applySpError = assertThrows(IllegalArgumentException.class,
                () -> applySp.apply((Throwables.Function<AbstractQueryBuilder.SP, Object, RuntimeException>) null));
        assertTrue(applySpError.getMessage().contains("function"));
        assertEquals("SELECT id FROM users", applySp.build().query());

        final SqlBuilder applySqlAndParams = PSC.select("id").from("users");
        final IllegalArgumentException applySqlAndParamsError = assertThrows(IllegalArgumentException.class,
                () -> applySqlAndParams.apply((Throwables.BiFunction<String, List<Object>, Object, RuntimeException>) null));
        assertTrue(applySqlAndParamsError.getMessage().contains("function"));
        assertEquals("SELECT id FROM users", applySqlAndParams.build().query());

        final SqlBuilder acceptSp = PSC.select("id").from("users");
        final IllegalArgumentException acceptSpError = assertThrows(IllegalArgumentException.class,
                () -> acceptSp.accept((Throwables.Consumer<AbstractQueryBuilder.SP, RuntimeException>) null));
        assertTrue(acceptSpError.getMessage().contains("consumer"));
        assertEquals("SELECT id FROM users", acceptSp.build().query());

        final SqlBuilder acceptSqlAndParams = PSC.select("id").from("users");
        final IllegalArgumentException acceptSqlAndParamsError = assertThrows(IllegalArgumentException.class,
                () -> acceptSqlAndParams.accept((Throwables.BiConsumer<String, List<Object>, RuntimeException>) null));
        assertTrue(acceptSqlAndParamsError.getMessage().contains("consumer"));
        assertEquals("SELECT id FROM users", acceptSqlAndParams.build().query());
    }

    // Cover select-into and entity-class join overloads that inject aliases directly.
    @Test
    public void testSelectIntoFromEntityClass() {
        final String sql = PSC.select("id", "firstName").into("account_archive").from(Account.class).build().query();

        assertTrue(sql.startsWith("INSERT INTO account_archive"));
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM account acc"));
    }

    @Test
    public void testEntityJoinOverloadsWithAlias() {
        final String innerJoinSql = PSC.select("*").from(Account.class, "a").innerJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String leftJoinSql = PSC.select("*").from(Account.class, "a").leftJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String rightJoinSql = PSC.select("*").from(Account.class, "a").rightJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String fullJoinSql = PSC.select("*").from(Account.class, "a").fullJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String crossJoinSql = PSC.select("*").from(Account.class, "a").crossJoin(Account.class, "a2").build().query();

        assertTrue(innerJoinSql.contains("INNER JOIN account a2"));
        assertTrue(leftJoinSql.contains("LEFT JOIN account a2"));
        assertTrue(rightJoinSql.contains("RIGHT JOIN account a2"));
        assertTrue(fullJoinSql.contains("FULL JOIN account a2"));
        assertTrue(crossJoinSql.contains("CROSS JOIN account a2"));
    }

    @Test
    public void testEntityJoinAliasRejectsLineBreakAndCommentTokens() {
        // Like from(Class, String), the entity JOIN overloads emit the alias verbatim after the table name, so a
        // comment token in it would swallow the ON connector and every later clause.
        final SqlBuilder builder = PSC.select("firstName").from(Account.class, "a");

        final IllegalArgumentException comment = assertThrows(IllegalArgumentException.class, () -> builder.join(Account.class, "b -- x"));
        assertTrue(comment.getMessage().startsWith("Table alias for 'Account' must not contain a line break or SQL comment token"), comment.getMessage());
        assertThrows(IllegalArgumentException.class, () -> builder.innerJoin(Account.class, "b /* x */"));
        assertThrows(IllegalArgumentException.class, () -> builder.leftJoin(Account.class, "b #x"));
        assertThrows(IllegalArgumentException.class, () -> builder.rightJoin(Account.class, "b\n"));
        assertThrows(IllegalArgumentException.class, () -> builder.fullJoin(Account.class, "b\r"));
        assertThrows(IllegalArgumentException.class, () -> builder.crossJoin(Account.class, "b -- x"));
        assertThrows(IllegalArgumentException.class, () -> builder.naturalJoin(Account.class, "b -- x"));

        // The rejection happens before any state changes, so the builder is still usable.
        assertEquals("SELECT a.first_name AS \"firstName\" FROM account a JOIN account b ON a.id = b.id WHERE a.first_name = ?",
                builder.join(Account.class, "b").on("a.id = b.id").where(Filters.eq("firstName", "x")).build().query());

        // A null or empty alias still means "no alias" and is accepted.
        assertEquals("SELECT a.first_name AS \"firstName\" FROM account a LEFT JOIN account ON a.id = account.id",
                PSC.select("firstName").from(Account.class, "a").leftJoin(Account.class, "").on("a.id = account.id").build().query());
    }

    @Test
    public void testOrderByRejectsBlockAndHashCommentTokens() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy("id/*comment*/").build().query());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy("id#comment").build().query());
    }

    @Test
    public void testPrintln() {
        final PrintStream originalOut = System.out;
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output));
            PSC.select("id").from("users").debugPrint();
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString().contains("SELECT id FROM users"));
    }

    @BeforeEach
    void setUp() {
        // No setup needed for constant testing
    }

    @Test
    void testPublicConstants() {
        assertEquals("ALL", AbstractQueryBuilder.ALL);
        assertEquals("TOP", AbstractQueryBuilder.TOP);
        assertEquals("UNIQUE", AbstractQueryBuilder.UNIQUE);
        assertEquals("DISTINCT", AbstractQueryBuilder.DISTINCT);
        assertEquals("DISTINCTROW", AbstractQueryBuilder.DISTINCTROW);
        assertEquals("*", AbstractQueryBuilder.ASTERISK);
        assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
    }

    @Test
    void testConstantsAreNotNull() {
        assertNotNull(AbstractQueryBuilder.ALL);
        assertNotNull(AbstractQueryBuilder.TOP);
        assertNotNull(AbstractQueryBuilder.UNIQUE);
        assertNotNull(AbstractQueryBuilder.DISTINCT);
        assertNotNull(AbstractQueryBuilder.DISTINCTROW);
        assertNotNull(AbstractQueryBuilder.ASTERISK);
        assertNotNull(AbstractQueryBuilder.COUNT_ALL);
    }

    @Test
    void testNamingPolicyEnum() {
        // Test that NamingPolicy enum values exist and are accessible
        assertNotNull(NamingPolicy.NO_CHANGE);
        assertNotNull(NamingPolicy.SNAKE_CASE);
        assertNotNull(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertNotNull(NamingPolicy.CAMEL_CASE);
    }

    @Test
    public void testSet_ObjectStringDelegatesToColumnSet() {
        String sql = PSC.update("account").set((Object) "firstName").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("SET"));
        assertTrue(sql.contains("first_name = ?"));
    }

    @Test
    public void testSet_ObjectMapHonorsExcludedProperties() {
        java.util.Map<String, Object> props = new java.util.LinkedHashMap<>();
        props.put("firstName", "John");
        props.put("lastName", "Doe");

        String sql = PSC.update("account").set(props, Collections.singleton("lastName")).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("first_name = ?"));
        assertTrue(!sql.contains("last_name = ?"));
    }

    @Test
    public void testInsertEntity_SkipsZeroIdAndNullProperties() {
        Account account = new Account();
        account.setId(0);
        account.setFirstName("John");
        account.setLastName(null);

        String sql = PSC.insert(account).into("account").build().query();

        assertTrue(sql.contains("first_name"));
        assertTrue(!sql.contains("last_name"));
        assertTrue(!sql.contains("id"));
    }

    @com.landawn.abacus.annotation.Table(name = "frac_tbl")
    public static class FractionalIdEntity {
        @com.landawn.abacus.annotation.Id
        private java.math.BigDecimal myKey;
        private String myName;

        public java.math.BigDecimal getMyKey() {
            return myKey;
        }

        public FractionalIdEntity setMyKey(java.math.BigDecimal myKey) {
            this.myKey = myKey;
            return this;
        }

        public String getMyName() {
            return myName;
        }

        public FractionalIdEntity setMyName(String myName) {
            this.myName = myName;
            return this;
        }
    }

    @com.landawn.abacus.annotation.Table(name = "composite_id_tbl")
    public static class CompositeIdEntity {
        @com.landawn.abacus.annotation.Id
        private long tenantId;

        @com.landawn.abacus.annotation.Id
        private long localId;

        private String name;

        public long getTenantId() {
            return tenantId;
        }

        public CompositeIdEntity setTenantId(long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public long getLocalId() {
            return localId;
        }

        public CompositeIdEntity setLocalId(long localId) {
            this.localId = localId;
            return this;
        }

        public String getName() {
            return name;
        }

        public CompositeIdEntity setName(String name) {
            this.name = name;
            return this;
        }
    }

    @Test
    public void testFix_insertEntity_doesNotSkipFractionalBigDecimalIdAsZero() {
        // BigDecimal("0.5").longValue() == 0 (truncation), so the buggy check would
        // wrongly treat 0.5 as a default/unset ID and omit it from the INSERT.
        FractionalIdEntity entity = new FractionalIdEntity();
        entity.setMyKey(new java.math.BigDecimal("0.5"));
        entity.setMyName("Alice");

        String sql = PSC.insert(entity).into("frac_tbl").build().query();

        assertTrue(sql.contains("my_name"), "my_name should be included: " + sql);
        assertTrue(sql.contains("my_key"), "Fractional BigDecimal id 0.5 must not be skipped as default: " + sql);
    }

    @Test
    public void testFix_insertEntity_keepsDefaultCompositeIdPartWhenAnotherIdAssigned() {
        CompositeIdEntity entity = new CompositeIdEntity();
        entity.setTenantId(7);
        entity.setLocalId(0);
        entity.setName("Alice");

        AbstractQueryBuilder.SP sp = PSC.insert(entity).into("composite_id_tbl").build();
        String sql = sp.query();

        assertTrue(sql.contains("tenant_id"), "assigned id should be included: " + sql);
        assertTrue(sql.contains("local_id"), "default-valued composite id part should be included: " + sql);
        assertTrue(sql.contains("name"), "regular non-null property should be included: " + sql);
        assertEquals(Arrays.asList(7L, 0L, "Alice"), sp.parameters());
    }

    @Test
    public void testBatchInsertKeepsDefaultCompositeIdPartWhenAnotherIdIsAssigned() {
        CompositeIdEntity first = new CompositeIdEntity().setTenantId(7).setLocalId(0).setName("Alice");
        CompositeIdEntity second = new CompositeIdEntity().setTenantId(8).setLocalId(0).setName("Bob");

        AbstractQueryBuilder.SP sp = PSC.batchInsert(Arrays.asList(first, second)).into("composite_id_tbl").build();

        assertEquals("INSERT INTO composite_id_tbl (tenant_id, local_id, name) VALUES (?, ?, ?), (?, ?, ?)", sp.query());
        assertEquals(Arrays.asList(7L, 0L, "Alice", 8L, 0L, "Bob"), sp.parameters());
    }

    @Test
    public void testFix_setColumnNamesNamedSqlDeduplicatesPlaceholders() {
        AbstractQueryBuilder.SP sp = NSC.update("users").set("status").where(Filters.eq("status", "OLD")).build();

        assertEquals("UPDATE users SET status = :status WHERE status = :status_2", sp.query());
        assertEquals(Arrays.asList("OLD"), sp.parameters());
    }

    @Test
    public void testFix_setColumnNamesIbatisSqlSanitizesAliasAndDeduplicatesPlaceholders() {
        AbstractQueryBuilder.SP sp = Dsl.MSC.update("users").set("u.firstName").where(Filters.eq("u.firstName", "John")).build();
        String sql = sp.query();

        assertTrue(sql.contains("#{firstName}"), "SET placeholder should be sanitized: " + sql);
        assertTrue(sql.contains("#{firstName_2}"), "WHERE placeholder should be de-duplicated: " + sql);
        assertFalse(sql.contains("#{u.firstName}"), "Raw dotted placeholder is invalid: " + sql);
        assertEquals(Arrays.asList("John"), sp.parameters());
    }

    @Test
    public void testFix_batchInsertEntities_doesNotSkipFractionalBigDecimalIdAsZero() {
        // Same defect as above, but for the batch-insert code path that builds props from a collection.
        FractionalIdEntity e1 = new FractionalIdEntity();
        e1.setMyKey(new java.math.BigDecimal("0.5"));
        e1.setMyName("Alice");

        FractionalIdEntity e2 = new FractionalIdEntity();
        e2.setMyKey(new java.math.BigDecimal("0.7"));
        e2.setMyName("Bob");

        String sql = PSC.batchInsert(java.util.Arrays.asList(e1, e2)).into("frac_tbl").build().query();

        assertTrue(sql.contains("my_name"), "my_name should be included: " + sql);
        assertTrue(sql.contains("my_key"), "Fractional BigDecimal ids must not be removed as all-zero: " + sql);
    }

    // Bug fix: set(Object entity), insert(entity), and batchInsert(entities) used HashMap internally,
    // which made the resulting SET / INSERT column order depend on hash codes rather than the
    // declared property order. The fix swaps to LinkedHashMap so column order is deterministic.

    @Test
    public void testFix_setWithEntityObjectPreservesPropertyOrder() {
        Account a = new Account();
        a.setGUI("g");
        a.setEmailAddress("e@e.com");
        a.setFirstName("F");
        a.setMiddleName("M");
        a.setLastName("L");
        a.setStatus(1);

        String sql = PSC.update("account").set(a).where(Filters.eq("id", 1)).build().query();

        // Property order in Account.java is: gui, emailAddress, firstName, middleName, lastName, status, ...
        // The SET clause must therefore list those columns in that order.
        int gui = sql.indexOf("gui = ?");
        int email = sql.indexOf("email_address = ?");
        int first = sql.indexOf("first_name = ?");
        int middle = sql.indexOf("middle_name = ?");
        int last = sql.indexOf("last_name = ?");
        int status = sql.indexOf("status = ?");

        assertTrue(gui > 0 && email > 0 && first > 0 && middle > 0 && last > 0 && status > 0, "all columns must be present: " + sql);
        assertTrue(gui < email, "gui before email_address in: " + sql);
        assertTrue(email < first, "email_address before first_name in: " + sql);
        assertTrue(first < middle, "first_name before middle_name in: " + sql);
        assertTrue(middle < last, "middle_name before last_name in: " + sql);
        assertTrue(last < status, "last_name before status in: " + sql);
    }

    @Test
    public void testFix_insertEntity_preservesPropertyOrder() {
        Account a = new Account();
        a.setGUI("g");
        a.setEmailAddress("e@e.com");
        a.setFirstName("F");
        a.setMiddleName("M");
        a.setLastName("L");
        a.setStatus(1);

        String sql = PSC.insert(a).into("account").build().query();

        int gui = sql.indexOf("gui");
        int email = sql.indexOf("email_address");
        int first = sql.indexOf("first_name");
        int middle = sql.indexOf("middle_name");
        int last = sql.indexOf("last_name");
        int status = sql.indexOf("status");

        assertTrue(gui > 0 && email > 0 && first > 0 && middle > 0 && last > 0 && status > 0, "all columns must be present: " + sql);
        assertTrue(gui < email, "gui before email_address in: " + sql);
        assertTrue(email < first, "email_address before first_name in: " + sql);
        assertTrue(first < middle, "first_name before middle_name in: " + sql);
        assertTrue(middle < last, "middle_name before last_name in: " + sql);
        assertTrue(last < status, "last_name before status in: " + sql);
    }

    @Test
    public void testFix_batchInsertEntities_preservesPropertyOrder() {
        Account a1 = new Account();
        a1.setGUI("g1");
        a1.setEmailAddress("e1@e.com");
        a1.setFirstName("F1");
        a1.setMiddleName("M1");
        a1.setLastName("L1");
        a1.setStatus(1);

        Account a2 = new Account();
        a2.setGUI("g2");
        a2.setEmailAddress("e2@e.com");
        a2.setFirstName("F2");
        a2.setMiddleName("M2");
        a2.setLastName("L2");
        a2.setStatus(2);

        String sql = PSC.batchInsert(java.util.Arrays.asList(a1, a2)).into("account").build().query();

        int gui = sql.indexOf("gui");
        int email = sql.indexOf("email_address");
        int first = sql.indexOf("first_name");
        int middle = sql.indexOf("middle_name");
        int last = sql.indexOf("last_name");
        int status = sql.indexOf("status");

        assertTrue(gui > 0 && email > 0 && first > 0 && middle > 0 && last > 0 && status > 0, "all columns must be present: " + sql);
        assertTrue(gui < email, "gui before email_address in: " + sql);
        assertTrue(email < first, "email_address before first_name in: " + sql);
        assertTrue(first < middle, "first_name before middle_name in: " + sql);
        assertTrue(middle < last, "middle_name before last_name in: " + sql);
        assertTrue(last < status, "last_name before status in: " + sql);
    }

    /**
     * Regression test: calling {@code set(Object, Set)} with a {@code null} entity must
     * fail fast with a descriptive {@link IllegalArgumentException} rather than throwing
     * a raw {@link NullPointerException} from {@code entity.getClass()}.
     */
    @Test
    public void testSetWithEntityObjectNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((Object) null));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((Object) null, null));
    }

    /**
     * Regression test: {@link AbstractQueryBuilder#sanitizeNamedParameterName(String)}
     * strips a table-alias prefix so the returned identifier can be used as a named
     * parameter in JDBC / MyBatis / Spring named SQL.
     */
    @Test
    public void testSanitizeNamedParameterName_stripsTableAliasPrefix() {
        // Simple names are unchanged.
        assertEquals("id", AbstractQueryBuilder.sanitizeNamedParameterName("id"));
        assertEquals("firstName", AbstractQueryBuilder.sanitizeNamedParameterName("firstName"));
        // Aliased names are stripped to the suffix.
        assertEquals("id", AbstractQueryBuilder.sanitizeNamedParameterName("u.id"));
        assertEquals("orderDate", AbstractQueryBuilder.sanitizeNamedParameterName("ord.orderDate"));
        // Multi-level prefixes collapse to the last segment.
        assertEquals("c", AbstractQueryBuilder.sanitizeNamedParameterName("a.b.c"));
        // Function/expression names are reduced to legal placeholder identifiers.
        assertEquals("COUNT", AbstractQueryBuilder.sanitizeNamedParameterName("COUNT(*)"));
        assertEquals("COUNT", AbstractQueryBuilder.sanitizeNamedParameterName("COUNT(o.id)"));
        // Edge cases.
        assertEquals("", AbstractQueryBuilder.sanitizeNamedParameterName(""));
        assertEquals(null, AbstractQueryBuilder.sanitizeNamedParameterName(null));
        assertEquals("ord", AbstractQueryBuilder.sanitizeNamedParameterName("ord."));
        // All-punctuation input collapses to the fixed fallback placeholder name.
        assertEquals("param", AbstractQueryBuilder.sanitizeNamedParameterName("??"));
        // A leading digit is prefixed so the placeholder remains a legal identifier.
        assertEquals("p123col", AbstractQueryBuilder.sanitizeNamedParameterName("123col"));
    }

    /**
     * Regression test: the {@code insert(String...).into(...)} VALUES placeholders for named
     * and iBATIS SQL must be routed through {@code nextNamedParameterName(...)} — exactly like
     * every other named-parameter site ({@code set(...)}, {@code appendInsertProps(...)}). Before
     * the fix this INSERT column path emitted the raw column name verbatim, so duplicate column
     * names produced colliding placeholders (e.g. {@code :id, :id}) instead of the de-duplicated
     * {@code :id, :id_2}. The common case of clean, distinct property names is unchanged.
     */
    @Test
    public void testInsertNamedPlaceholdersAreSanitizedAndDeduplicated() {
        // Clean, distinct names: unchanged (no regression).
        String named = Dsl.NLC.insert("firstName", "lastName").into("account").build().query();
        assertTrue(named.contains("VALUES (:firstName, :lastName)"), "Unexpected named INSERT SQL: " + named);

        // Duplicate column names: placeholders must be de-duplicated via the occurrence counter.
        String dup = Dsl.NLC.insert("id", "id").into("account").build().query();
        assertTrue(dup.contains("VALUES (:id, :id_2)"), "Duplicate named placeholders not de-duplicated: " + dup);

        // iBATIS (#{...}) path: same routing through nextNamedParameterName.
        String ibatisClean = Dsl.MLC.insert("firstName").into("account").build().query();
        assertTrue(ibatisClean.contains("VALUES (#{firstName})"), "Unexpected iBATIS INSERT SQL: " + ibatisClean);

        String ibatisDup = Dsl.MLC.insert("id", "id").into("account").build().query();
        assertTrue(ibatisDup.contains("VALUES (#{id}, #{id_2})"), "Duplicate iBATIS placeholders not de-duplicated: " + ibatisDup);
    }

    /**
     * Regression test (Pass 2): {@code Account} declares sub-entity properties
     * ({@code contact} and {@code devices}). When a SELECT-with-sub-entities is
     * built, every code path that iterates {@link com.landawn.abacus.parser.ParserUtil.BeanInfo#subEntityPropNameList}
     * and calls {@code getPropInfo(name)} must tolerate the rare null return
     * (defensive guard added in {@code getSelectTableNames} and {@code getFromClause}).
     * The normal happy path should still produce SQL with the sub-entity tables.
     */
    @Test
    public void testSelectWithSubEntities_DoesNotThrow_Pass2() {
        // Force the include-sub-entities select path. Using PSC.selectFrom(Class, boolean)
        // exercises both helpers that previously dereferenced propInfo without a null check.
        String sql = PSC.selectFrom(Account.class, true).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"), "Should produce a SELECT");
        assertTrue(sql.contains("FROM"), "Should produce a FROM");
    }

    /**
     * Regression test (Pass 2): the constructor-time builder-leak warning logic must log
     * at the highest applicable severity. Previously the {@code else if (> 1024)} branch
     * was unreachable when warn was enabled, because the prior {@code if (> 512 && warn)}
     * branch consumed all matching cases. The fix swaps the order so that an over-1024
     * count produces the ERROR-level log instead of the warning.
     *
     * <p>Functional verification is indirect: we exercise the constructor + build() lifecycle
     * heavily enough that the warning branch would be reachable, and confirm that the
     * builder still produces valid SQL without throwing. (Asserting on the log output itself
     * would require a logger mock and is out of scope.)
     */
    @Test
    public void testManyBuildersDoNotLeakOrThrow_Pass2() {
        for (int i = 0; i < 32; i++) {
            String sql = PSC.select("id").from(Account.class).where(Filters.eq("id", i)).build().query();
            assertNotNull(sql);
        }
    }

    /**
     * Regression test: a {@code null} {@link Condition} passed to {@code where(Condition)},
     * {@code having(Condition)}, {@code on(Condition)}, or {@code append(Condition)} must
     * fail fast with {@link IllegalArgumentException}. Previously these methods silently
     * fell through to {@code appendCondition(null)}, producing malformed SQL containing
     * a bare {@code WHERE}/{@code HAVING}/{@code ON} keyword followed by no expression.
     */
    @Test
    public void testWhereHavingOnAppendRejectNullCondition() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").where((Condition) null).build().query());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy("id").having((Condition) null).build().query());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users u").join("orders o").on((Condition) null).build().query());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").append((Condition) null).build().query());
    }

    @com.landawn.abacus.annotation.Table(name = "profile")
    public static class DivergentColumnProfile {
        private long id;

        @com.landawn.abacus.annotation.Column("f_name")
        private String fullName;

        public long getId() {
            return id;
        }

        public DivergentColumnProfile setId(long id) {
            this.id = id;
            return this;
        }

        public String getFullName() {
            return fullName;
        }

        public DivergentColumnProfile setFullName(String fullName) {
            this.fullName = fullName;
            return this;
        }
    }

    /**
     * Regression test: the multi-select branch of {@code appendSelectListAndFromClause} used to
     * replace {@code _aliasPropColumnNameMap} wholesale, discarding the main-table alias mapping that
     * {@code appendOperationBeforeFrom} had registered moments earlier for the first selection's entity
     * class. An aliased reference to a property whose {@code @Column} name diverges from the naming
     * policy then fell back to naming-policy conversion instead of the annotated column name.
     */
    @Test
    public void testMultiSelectFromKeepsMainTableAliasRegistration() {
        final List<Selection> selections = Arrays.asList(Selection.builder(DivergentColumnProfile.class).build(),
                Selection.builder(Account.class).tableAlias("o").build());

        final String sql = PSC.select(selections).from("profile a, account o").where(Filters.eq("a.fullName", "X")).build().query();

        assertTrue(sql.contains("WHERE a.f_name = ?"), "aliased @Column name must resolve via the main-table registration: " + sql);
        assertFalse(sql.contains("a.full_name"), "naming-policy fallback indicates the main-table alias registration was dropped: " + sql);
    }

    // SqlExpression.toSql and AbstractQueryBuilder.appendStringExpr are two independent rendering paths for
    // the same raw expression text: a condition reaches the SQL through the builder, but toString()/toSql()
    // renders it directly. Guards added to one must be added to the other. Before this was pinned, the
    // builder left "_firstName" unconverted, converted "@firstName" (a SQL variable, not a column), and
    // snake-cased the contents of the string literal in "N'camelCase'".
    @Test
    public void testRawExpressionRendersIdenticallyThroughBothPaths() {
        for (final String expr : new String[] { "_firstName = otherValue", "@firstName + columnName", "N'camelCase' = firstName",
                "_utf8mb4'camelCase' = firstName", "firstName = lastName", "@@version = firstName", "price  *  2", "aB-cD" }) {
            final String viaCondition = Filters.expr(expr).toSql(NamingPolicy.SNAKE_CASE);
            final String builtSql = PSC.select("id").from("t").where(Filters.expr(expr)).build().query();
            final String viaBuilder = builtSql.substring(builtSql.indexOf("WHERE ") + 6);

            assertEquals(viaCondition, viaBuilder, "rendering paths diverged for: " + expr);
        }

        // The specific corruptions the guards prevent.
        assertEquals("N'camelCase' = first_name", Filters.expr("N'camelCase' = firstName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("@firstName + column_name", Filters.expr("@firstName + columnName").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_first_name = other_value", Filters.expr("_firstName = otherValue").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("a_b-c_d", Filters.expr("aB-cD").toSql(NamingPolicy.SNAKE_CASE));
    }

    // SqlExpression.toSql and the builder's own tokenizer path both pass a digit-leading token through;
    // the builder's short-literal fast path used to be the only path that naming-converted it.
    @Test
    public void testDigitLeadingShortTokenIsNotConvertedByTheBuilderFastPath() {
        for (final String expr : new String[] { "2faCode", "3dModel", "0x1F" }) {
            final String viaCondition = Filters.expr(expr).toSql(NamingPolicy.SNAKE_CASE);
            final String builtSql = PSC.select("id").from("t").where(Filters.expr(expr)).build().query();

            assertEquals(viaCondition, builtSql.substring(builtSql.indexOf("WHERE ") + 6), "rendering paths diverged for: " + expr);
        }

        assertEquals("SELECT id FROM t WHERE 2faCode", PSC.select("id").from("t").where(Filters.expr("2faCode")).build().query());
        assertEquals("SELECT id FROM t WHERE 2faCode = 1", PSC.select("id").from("t").where(Filters.expr("2faCode = 1")).build().query());
        // Column-name callers share the fast path.
        assertEquals("SELECT 2faCode FROM t", PSC.select("2faCode").from("t").build().query());
        assertEquals("SELECT id FROM t ORDER BY 2faCode", PSC.select("id").from("t").orderBy("2faCode").build().query());
    }

    // Under CAMEL_CASE the builder used Beans.normalizePropName, whose Java-keyword map rewrote the
    // identifier "class" to "clazz" while SqlExpression.toSql (plain NamingPolicy.convert) did not.
    @Test
    public void testCamelCasePolicyRendersClassIdentifierIdenticallyThroughBothPaths() {
        for (final String expr : new String[] { "class = 1", "CLASS = 1", "class = 1 AND other_col = 2" }) {
            final String viaCondition = Filters.expr(expr).toSql(NamingPolicy.CAMEL_CASE);
            final String builtSql = PLC.select("id").from("t").where(Filters.expr(expr)).build().query();

            assertEquals(viaCondition, builtSql.substring(builtSql.indexOf("WHERE ") + 6), "rendering paths diverged for: " + expr);
        }

        assertEquals("SELECT class FROM t", PLC.select("class").from("t").build().query());
        assertEquals("SELECT id FROM t WHERE class = ?", PLC.select("id").from("t").where(Filters.eq("class", 1)).build().query());
        assertEquals("class", AbstractQueryBuilder.normalizeColumnName("class", NamingPolicy.CAMEL_CASE));
    }

    @Test
    public void testFromVarargsMultiTableKeepsInlineAliasesFromFirstElement() {
        // Mirrors the single-string comma form: from("users u, orders o"). A bare two-String call from
        // this package binds to the protected from(tableName, fromClause) overload instead of the public
        // varargs, so an explicit array is required to exercise the path production callers get.
        assertEquals("SELECT * FROM users u, orders o", PSC.select("*").from(new String[] { "users u", "orders o" }).build().query());

        // The first element carries the primary table alias used for entity-property resolution.
        final SqlBuilder aliased = PSC.select(Account.class).from(new String[] { "account u", "orders o" });
        assertEquals("u", aliased._tableAlias);
        final String sql = aliased.build().query();
        assertTrue(sql.contains("u.first_name"), "properties must resolve against the first element's inline alias: " + sql);
        assertTrue(sql.endsWith("FROM account u, orders o"), sql);
    }

    // append(String) used to write straight into the buffer without priming it, so an append that was the
    // first write permanently suppressed the lazily-emitted "DELETE FROM t" / "UPDATE t SET " prefix and
    // build() returned the bare fragment as the whole statement.
    @Test
    public void testAppendStringAsFirstWriteStillEmitsTheStatementPrefix() {
        assertEquals("DELETE FROM account WHERE id = 1", PSC.deleteFrom("account").append("WHERE id = 1").build().query());
        assertEquals("DELETE FROM account WHERE id = 1", PSC.deleteFrom("account").appendIf(true, "WHERE id = 1").build().query());
        assertEquals("DELETE FROM account WHERE id = 1", PSC.deleteFrom("account").appendIfOrElse(true, "WHERE id = 1", "WHERE id = 2").build().query());
        assertEquals("DELETE FROM account WHERE id = 2", PSC.deleteFrom("account").appendIfOrElse(false, "WHERE id = 1", "WHERE id = 2").build().query());

        // An UPDATE whose SET list is already staged by update(Class, ...) renders it before the fragment.
        final String updateSql = PSC.update(Account.class).append("WHERE id = 1").build().query();
        assertTrue(updateSql.startsWith("UPDATE account SET id = ?, gui = ?, "), updateSql);
        assertTrue(updateSql.endsWith(" WHERE id = 1"), updateSql);

        // An UPDATE with nothing staged fails fast rather than silently dropping "UPDATE t SET ".
        assertThrows(IllegalStateException.class, () -> PSC.update("account").append("WHERE id = 1").build());
    }

    // append(Condition) emitted set operations without the per-segment reset that union(...)/intersect(...)
    // perform, so the finished left operand's table alias leaked into a trailing ORDER BY (invalid on a
    // set-operation result) and a later distinct() was spliced retro-actively into that operand.
    @Test
    public void testAppendedSetOperationClosesTheSegmentLikeTheUnionMethods() {
        final String viaAppend = PSC.select("firstName")
                .from(Account.class, "acc")
                .append(new Union(Filters.subQuery("SELECT first_name FROM archive")))
                .orderBy("firstName")
                .build()
                .query();
        final String viaUnion = PSC.select("firstName").from(Account.class, "acc").union("SELECT first_name FROM archive").orderBy("firstName").build().query();

        assertEquals(viaUnion, viaAppend);
        assertTrue(viaAppend.endsWith("ORDER BY first_name"), "ORDER BY after a set operation must not be alias-qualified: " + viaAppend);

        // Same reset for the Criteria-carried set-operation path.
        final String viaCriteria = PSC.select("firstName")
                .from(Account.class, "acc")
                .append(Criteria.builder().union(Filters.subQuery("SELECT first_name FROM archive")).orderBy("firstName").build())
                .build()
                .query();
        assertTrue(viaCriteria.endsWith("ORDER BY first_name"), viaCriteria);

        // A select modifier can no longer be spliced into the already-finished left operand; the staged
        // modifier is now rejected at build() exactly as it is after union(String).
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("t").append(new Union(Filters.subQuery("SELECT id FROM u"))).distinct().build());
        assertThrows(IllegalStateException.class, () -> PSC.select("id").from("t").union("SELECT id FROM u").distinct().build());
    }

    // A Criteria carrying both a select modifier and a set operation lost the modifier: it was applied
    // only after the set operation closed the segment (resetting the SELECT-keyword insertion point),
    // so build() rejected it as an unattached staged modifier. It must be spliced into the left segment
    // it was validated against, exactly like selectModifier("DISTINCT") called before union(...).
    @Test
    public void testCriteriaSelectModifierWithCriteriaSetOperation() {
        final String viaCriteria = PSC.select("id")
                .from("t")
                .append(Criteria.builder().selectModifier("DISTINCT").union(Filters.subQuery("SELECT id FROM u")).build())
                .build()
                .query();
        final String viaMethods = PSC.select("id").from("t").selectModifier("DISTINCT").union("SELECT id FROM u").build().query();

        assertEquals("SELECT DISTINCT id FROM t UNION SELECT id FROM u", viaCriteria);
        assertEquals(viaMethods, viaCriteria);
    }

    // on(Condition) appended " ON " unconditionally, so an On/Using operand -- which renders its own
    // keyword -- produced "ON ON (...)" or the impossible "ON USING (...)".
    @Test
    public void testOnConditionDoesNotDuplicateTheConnectorKeyword() {
        assertEquals("SELECT id FROM t INNER JOIN dept ON (t.dept_id = dept.id)",
                PSC.select("id").from("t").innerJoin("dept").on(Filters.on("t.deptId", "dept.id")).build().query());
        assertEquals("SELECT id FROM t INNER JOIN dept USING (dept_id)",
                PSC.select("id").from("t").innerJoin("dept").on(Filters.using("deptId")).build().query());

        // A plain predicate still gets the ON keyword supplied by the builder.
        assertEquals("SELECT id FROM t INNER JOIN dept ON t.dept_id = dept.id",
                PSC.select("id").from("t").innerJoin("dept").on(Filters.expr("t.deptId = dept.id")).build().query());
    }

    @Test
    public void testConditionClauseMethodsRejectNonPredicatesWithoutMutation() {
        final Condition nullOperator = new Condition() {
            @Override
            public Operator operator() {
                return null;
            }

            @Override
            public ImmutableList<Object> parameters() {
                return ImmutableList.empty();
            }

            @Override
            public String toSql(final NamingPolicy namingPolicy) {
                return "invalid";
            }
        };

        final SqlBuilder where = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> where.where(Criteria.builder().where(Filters.eq("active", true)).build()));
        assertThrows(IllegalArgumentException.class, () -> where.where(Filters.subQuery("SELECT id FROM archived_users")));
        assertThrows(IllegalArgumentException.class, () -> where.where(Filters.where(Filters.eq("active", true))));
        assertThrows(IllegalArgumentException.class, () -> where.where(Filters.on("users.id", "accounts.user_id")));
        assertEquals("SELECT id FROM users WHERE active = ?", where.where(Filters.eq("active", true)).build().query());

        final SqlBuilder having = PSC.select("department").from("users").groupBy("department");
        assertThrows(IllegalArgumentException.class, () -> having.having(Filters.innerJoin("accounts")));
        assertThrows(IllegalArgumentException.class, () -> having.having(Filters.any(Filters.subQuery("SELECT user_id FROM archived_users"))));
        assertThrows(IllegalArgumentException.class, () -> having.having(Filters.using("department")));
        assertEquals("SELECT department FROM users GROUP BY department HAVING COUNT(*) > 1", having.having(Filters.expr("COUNT(*) > 1")).build().query());

        final SqlBuilder on = PSC.select("id").from("users").innerJoin("accounts");
        assertThrows(IllegalArgumentException.class, () -> on.on(Filters.expr(" ")));
        assertThrows(IllegalArgumentException.class, () -> on.on(nullOperator));
        assertEquals("SELECT id FROM users INNER JOIN accounts ON users.id = accounts.user_id",
                on.on(Filters.expr("users.id = accounts.user_id")).build().query());

        // An empty junction is a complete predicate (its Boolean identity), so it is accepted as an ON predicate.
        assertEquals("SELECT id FROM users INNER JOIN accounts ON 1 = 1", PSC.select("id").from("users").innerJoin("accounts").on(Filters.and()).build().query());
    }

    @Test
    public void testAppendRejectsStructuralImplicitWhereOperandsWithoutMutation() {
        final SqlBuilder builder = PSC.select("id").from("users");
        final SubQuery subQuery = Filters.subQuery("SELECT user_id FROM archived_users");

        assertThrows(IllegalArgumentException.class, () -> builder.append(subQuery));
        assertThrows(IllegalArgumentException.class, () -> builder.append(Filters.any(subQuery)));
        assertThrows(IllegalArgumentException.class, () -> builder.append(Filters.on("users.id", "accounts.user_id")));
        assertThrows(IllegalArgumentException.class, () -> builder.append(Filters.innerJoin("accounts")));
        assertEquals("SELECT id FROM users WHERE active = ?", builder.append(Filters.eq("active", true)).build().query());

        assertEquals("ANY (SELECT user_id FROM archived_users)", PSC.renderCondition(Filters.any(subQuery)).build().query());
    }

    @Test
    public void testAppendRejectsGenericClausesThatRequireDedicatedBuilderState() {
        for (final Operator operator : Arrays.asList(Operator.LIMIT, Operator.OFFSET, Operator.FOR_UPDATE)) {
            final SqlBuilder builder = PSC.select("id").from("users");

            assertThrows(IllegalArgumentException.class, () -> builder.append(new TestClause(operator, Filters.expr("10"))));
            assertEquals("SELECT id FROM users WHERE active = ?", builder.where(Filters.eq("active", true)).build().query());
        }
    }

    @Test
    public void testCrossAndNaturalJoinRejectOnlyTopLevelConnectorsWithoutMutation() {
        final SqlBuilder cross = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> cross.crossJoin("orders o ON o.user_id = users.id"));
        assertEquals("SELECT id FROM users CROSS JOIN orders o", cross.crossJoin("orders o").build().query());

        final SqlBuilder natural = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> natural.naturalJoin("orders o USING (user_id)"));
        assertEquals("SELECT id FROM users NATURAL JOIN orders o", natural.naturalJoin("orders o").build().query());

        assertEquals("SELECT id FROM users CROSS JOIN (SELECT a.id FROM a JOIN b ON a.id = b.id) nested_orders",
                PSC.select("id").from("users").crossJoin("(SELECT a.id FROM a JOIN b ON a.id = b.id) nested_orders").build().query());
        assertEquals("SELECT id FROM users NATURAL JOIN (SELECT id FROM a JOIN b USING (id)) nested_orders",
                PSC.select("id").from("users").naturalJoin("(SELECT id FROM a JOIN b USING (id)) nested_orders").build().query());
    }

    // sqlKeyWords is built only from SK, which does not carry the underscore-bearing niladic keyword
    // functions. Unregistered, a naming policy rewrote them into identifiers -- CURRENT_USER became
    // currentUser (CAMEL_CASE) and current-user (KEBAB_CASE, which parses as subtraction). This registry
    // and SqlExpression's are two independent rendering paths that must agree.
    @Test
    public void testNiladicKeywordFunctionsAreNotRewrittenByTheNamingPolicy() {
        final String[] keywords = { "CURRENT_CATALOG", "CURRENT_DATE", "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_SCHEMA", "CURRENT_TIME", "CURRENT_TIMESTAMP",
                "CURRENT_USER", "LOCALTIME", "LOCALTIMESTAMP", "SESSION_USER", "SYSTEM_USER", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "NAN", "INFINITE",
                "UNKNOWN", "NULL" };

        for (final String keyword : keywords) {
            for (final Dsl dsl : new Dsl[] { Dsl.PSC, Dsl.PLC, Dsl.PAC }) {
                // Only the keyword itself is asserted: the naming policy legitimately rewrites the "x" operand.
                final String sql = dsl.select("id").from("t").where(Filters.expr("x = " + keyword)).build().query();
                assertTrue(sql.endsWith(" = " + keyword), keyword + " must survive the naming policy, but got: " + sql);
            }
        }

        // A column genuinely named like a keyword, in lower case, must still be converted: the registry is
        // deliberately upper-case only.
        assertEquals("SELECT id FROM t WHERE x = currentUser", Dsl.PLC.select("id").from("t").where(Filters.expr("x = current_user")).build().query());
    }

    // Both registries must agree on the lower-case forms too: SqlExpression deliberately registers only
    // the as-is and UPPER-case keyword forms so a column genuinely named "order"/"count"/"rownum" is
    // still converted, whereas sqlKeyWords also registered every SK keyword in lower case, so the same
    // fragment rendered "X = ROWNUM" via toSql() but "X = rownum" via the builder under SCREAMING_SNAKE_CASE.
    @Test
    public void testLowerCaseKeywordLikeColumnRendersIdenticallyThroughBothPathsUnderScreamingSnakeCase() {
        for (final String expr : new String[] { "x = rownum", "x = order", "x = count", "id desc", "x = current_date" }) {
            final String viaCondition = Filters.expr(expr).toSql(NamingPolicy.SCREAMING_SNAKE_CASE);
            final String builtSql = PAC.select("id").from("t").where(Filters.expr(expr)).build().query();

            assertEquals(viaCondition, builtSql.substring(builtSql.indexOf("WHERE ") + 6), "rendering paths diverged for: " + expr);
        }

        assertEquals("SELECT ID AS \"id\" FROM t WHERE X = ROWNUM", PAC.select("id").from("t").where(Filters.expr("x = rownum")).build().query());
        // Non-expression consumers share the same registry: plain column names, ORDER BY and SET columns.
        assertEquals("SELECT COUNT AS \"count\" FROM t ORDER BY ORDER", PAC.select("count").from("t").orderBy("order").build().query());
        assertEquals("UPDATE t SET COUNT = ? WHERE ORDER = ?", PAC.update("t").set("count").where(Filters.eq("order", 1)).build().query());
        // The canonical upper-case keyword form is still left untouched.
        assertEquals("SELECT id FROM t WHERE x = CURRENT_DATE", PLC.select("id").from("t").where(Filters.expr("x = CURRENT_DATE")).build().query());
    }

    // NamingPolicy.convert strips leading and trailing underscore runs; both rendering paths must restore
    // them through QueryUtil.convertIdentifier so a column literally named "_id" or "__v" keeps its identity.
    @Test
    public void testLeadingAndTrailingUnderscoreIdentifiersKeepTheirRunsUnderNamingPolicy() {
        assertEquals("SELECT id FROM t WHERE _id = ?", PSC.select("id").from("t").where(Filters.eq("_id", 1)).build().query());
        assertEquals("SELECT _id, __v, _first_name AS \"_firstName\", first_name_ AS \"firstName_\" FROM t",
                PSC.select("_id", "__v", "_firstName", "firstName_").from("t").build().query());
        assertEquals("INSERT INTO t (_id, _first_name) VALUES (?, ?)", PSC.insert("_id", "_firstName").into("t").build().query());
        assertEquals("UPDATE t SET _id = ? WHERE ___ = ?", PSC.update("t").set("_id", 1).where(Filters.eq("___", 2)).build().query());
        assertEquals("SELECT id FROM t WHERE _first_name = other_value", PSC.select("id").from("t").where(Filters.expr("_firstName = otherValue")).build().query());

        assertEquals("SELECT _ID AS \"_id\", __V AS \"__v\", _FIRST_NAME AS \"_firstName\" FROM t",
                PAC.select("_id", "__v", "_firstName").from("t").build().query());
        assertEquals("SELECT ID AS \"id\" FROM t WHERE _ID = ?", PAC.select("id").from("t").where(Filters.eq("_id", 1)).build().query());
        assertEquals("INSERT INTO t (_ID) VALUES (?)", PAC.insert("_id").into("t").build().query());

        // Parity with the condition path.
        assertEquals("_first_name = other_value", Filters.expr("_firstName = otherValue").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("_FIRST_NAME = OTHER_VALUE", Filters.expr("_firstName = otherValue").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
    }

    // An empty junction is a complete predicate: the builder renders its Boolean identity exactly as Junction.toSql does.
    @Test
    public void testEmptyJunctionRendersAsBooleanIdentityThroughTheBuilder() {
        assertEquals("1 = 1", PSC.renderCondition(Filters.and()).build().query());
        assertEquals("1 = 0", PSC.renderCondition(Filters.or()).build().query());
        assertEquals("SELECT * FROM account WHERE 1 = 0", PSC.select("*").from("account").where(Filters.or()).build().query());
        assertEquals("SELECT * FROM account WHERE 1 = 1", PSC.select("*").from("account").where(Filters.and()).build().query());
        assertEquals("SELECT * FROM account HAVING 1 = 1", PSC.select("*").from("account").having(Filters.and()).build().query());
        assertEquals("SELECT * FROM account WHERE (1 = 0) AND (id = ?)",
                PSC.select("*").from("account").where(Filters.and(Filters.or(), Filters.eq("id", 1))).build().query());
        assertEquals("SELECT * FROM account WHERE 1 = 1", PSC.select("*").from("account").append(Criteria.builder().where(Filters.and()).build()).build().query());
    }

    // A raw ON/USING fragment completes the pending qualified JOIN, exactly as on()/using() would.
    @Test
    public void testRawAppendedConnectorCompletesQualifiedJoin() {
        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id WHERE u.active = 1",
                PSC.select("*").from("users u").join("orders o").append("ON u.id = o.user_id").where("u.active = 1").build().query());
        assertEquals("SELECT * FROM users u JOIN orders o USING (user_id)", PSC.select("*").from("users u").join("orders o").append("USING (user_id)").build().query());
        assertEquals("SELECT * FROM users u JOIN orders o on(u.id = o.user_id)",
                PSC.select("*").from("users u").join("orders o").append("on(u.id = o.user_id)").build().query());

        // The slot is closed afterwards, so a structured connector is rejected instead of double-rendering ON.
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users u").join("orders o").append("ON u.id = o.user_id").on("x = y"));

        // Any other raw fragment leaves the JOIN open; the next clause and build() still reject it.
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users u").join("orders o").append("only").build());
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users u").join("orders o").append("online = 1").where("x = 1"));
    }

    // from(Collection) / from(String...) derive the primary table alias exactly like from(String): an element
    // carrying an inline JOIN must not hand its final predicate token to the alias scanner.
    @Test
    public void testFromCollectionDerivesPrimaryAliasLikeFromString() {
        final String tableRefs = "users u JOIN orders o ON u.id = o.uid AND o.active = flag";
        final String viaString = PSC.select("flag.id", "u.id").from(tableRefs).build().query();
        assertEquals("SELECT flag.id AS \"flag.id\", u.id FROM users u JOIN orders o ON u.id = o.uid AND o.active = flag", viaString);

        assertEquals(viaString + ", extra x", PSC.select("flag.id", "u.id").from(Arrays.asList(tableRefs, "extra x")).build().query());
        assertEquals(viaString + ", extra x", PSC.select("flag.id", "u.id").from(tableRefs, "extra x").build().query());
        assertEquals("SELECT o.id AS \"o.id\" FROM users u, orders o", PSC.select("o.id").from(Arrays.asList("users u", "orders o")).build().query());
    }

    // limit(int, int) reports the lifecycle error before argument validation, exactly like limit(int).
    @Test
    public void testLimitWithOffsetReportsClosedBuilderBeforeArgumentErrors() {
        final SqlBuilder closed = PSC.select("*").from("t");
        closed.build();
        assertThrows(IllegalStateException.class, () -> closed.limit(-1, 0));
        assertThrows(IllegalStateException.class, () -> closed.limit(-1));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("t").limit(-1, 0));
    }

    // The positional set(...) overloads back onto a map, which would silently collapse a repeated name.
    @Test
    public void testPositionalSetOverloadsRejectDuplicateNames() {
        assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set("a", 1, "a", 2));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set("a", 1, "b", 2, "a", 3));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set("a", 1, "b", 2, "b", 3));
        assertEquals("UPDATE t SET a = ?, b = ?, c = ?", PSC.update("t").set("a", 1, "b", 2, "c", 3).build().query());
    }

    // IS / IS NOT with a Boolean is normalized to the SQL keyword expression: no bind parameter on any policy,
    // and the rendering is identical to Filters.isTrue/isFalse (the keyword's letter case follows the naming
    // policy exactly as it does for isTrue/isFalse; it is never bound as a value).
    @Test
    public void testIsWithBooleanRendersSqlLiteralWithoutParameter() {
        final AbstractQueryBuilder.SP isTrue = PSC.select("*").from("t").where(Filters.is("x", true)).build();
        assertTrue("SELECT * FROM t WHERE x IS TRUE".equalsIgnoreCase(isTrue.query()), isTrue.query());
        assertTrue(isTrue.parameters().isEmpty());
        assertEquals(PSC.select("*").from("t").where(Filters.isTrue("x")).build().query(), isTrue.query());
        assertEquals("SELECT * FROM t WHERE X IS TRUE", PAC.select("*").from("t").where(Filters.is("x", true)).build().query());

        final AbstractQueryBuilder.SP isNotFalse = NSC.select("*").from("t").where(Filters.isNot("x", false)).build();
        assertTrue("SELECT * FROM t WHERE x IS NOT FALSE".equalsIgnoreCase(isNotFalse.query()), isNotFalse.query());
        assertTrue(isNotFalse.parameters().isEmpty());
        assertEquals(NSC.select("*").from("t").where(Filters.isNot("x", SqlExpression.of("FALSE"))).build().query(), isNotFalse.query());

        // Builder and condition paths agree.
        assertEquals(Filters.isTrue("x").toSql(NamingPolicy.SNAKE_CASE), Filters.is("x", true).toSql(NamingPolicy.SNAKE_CASE));
    }

    // The positional set(name, value, name, value[, name, value]) duplicate check skips blank names so a blank
    // name is reported by the map overload's blank-name error, not as a "Duplicate" of another blank name.
    @Test
    public void testPositionalSetReportsBlankNamesAsBlankNotDuplicate() {
        final IllegalArgumentException twoBlank = assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set("", 1, "", 2));
        assertTrue(twoBlank.getMessage().contains("blank"), twoBlank.getMessage());
        assertFalse(twoBlank.getMessage().contains("Duplicate"), twoBlank.getMessage());

        final IllegalArgumentException threeBlank = assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set(" ", 1, " ", 2, "name", 3));
        assertTrue(threeBlank.getMessage().contains("blank"), threeBlank.getMessage());
        assertFalse(threeBlank.getMessage().contains("Duplicate"), threeBlank.getMessage());

        // Non-blank duplicates are still rejected as duplicates.
        final IllegalArgumentException dup = assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set("name", 1, "name", 2));
        assertTrue(dup.getMessage().contains("Duplicate"), dup.getMessage());
    }

    @Test
    public void testTrueFalseKeywordsAreNotCaseConvertedByEitherRenderingPath() {
        // TRUE / FALSE are registered SQL keywords in BOTH registries (AbstractQueryBuilder.sqlKeyWords and
        // SqlExpression.SQL_KEY_WORDS), so a naming policy never rewrites them into "true" / "false".
        assertEquals("SELECT * FROM t WHERE active IS TRUE", PSC.select("*").from("t").where(Filters.isTrue("active")).build().query());
        assertEquals("SELECT * FROM t WHERE active IS NOT FALSE", PSC.select("*").from("t").where(Filters.isNot("active", false)).build().query());
        assertEquals("SELECT * FROM t WHERE active_flag = TRUE", PSC.select("*").from("t").where(Filters.expr("activeFlag = TRUE")).build().query());
        assertEquals("active_flag = TRUE", Filters.expr("activeFlag = TRUE").toSql(NamingPolicy.SNAKE_CASE));
        assertEquals("active_flag = FALSE", Filters.expr("activeFlag = FALSE").toSql(NamingPolicy.SNAKE_CASE));
        // Registration is UPPER-case only: a column genuinely named "true" is still converted.
        assertEquals("is_true = 1", Filters.expr("isTrue = 1").toSql(NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testJoinedEntityMappingDoesNotLeakIntoUnqualifiedPrimaryColumns() {
        // Joining an entity class onto a plain-string FROM must not install the joined entity's property
        // mapping as the builder's PRIMARY mapping: unqualified names in later clauses used to resolve through
        // the joined entity and were prefixed with the PRIMARY table's alias (o.first_name).
        assertEquals("SELECT o.id FROM orders o JOIN account acc ON o.aid = acc.id WHERE first_name = ?",
                PSC.select("o.id").from("orders o").join(Account.class, "acc").on("o.aid = acc.id").where(Filters.eq("firstName", "J")).build().query());
        assertEquals("SELECT o.id FROM orders o JOIN account acc ON o.aid = acc.id WHERE acc.first_name = ?",
                PSC.select("o.id").from("orders o").join(Account.class, "acc").on("o.aid = acc.id").where(Filters.eq("acc.firstName", "J")).build().query());
        assertEquals("SELECT o.id FROM orders o JOIN account acc ON o.aid = acc.id ORDER BY last_name",
                PSC.select("o.id").from("orders o").join(Account.class, "acc").on("o.aid = acc.id").orderBy("lastName").build().query());
        assertEquals("SELECT o.id FROM orders o JOIN account acc ON o.aid = acc.id GROUP BY o.id HAVING first_name = ?",
                PSC.select("o.id").from("orders o").join(Account.class, "acc").on("o.aid = acc.id").groupBy("o.id").having(Filters.eq("firstName", "J")).build().query());

        // every entity-class join flavour routes through addPropColumnMapForAlias and behaves the same way
        assertEquals("SELECT o.id FROM orders o LEFT JOIN account acc ON o.aid = acc.id WHERE first_name = ?",
                PSC.select("o.id").from("orders o").leftJoin(Account.class, "acc").on("o.aid = acc.id").where(Filters.eq("firstName", "J")).build().query());
        assertEquals("SELECT o.id FROM orders o NATURAL JOIN account acc WHERE first_name = ?",
                PSC.select("o.id").from("orders o").naturalJoin(Account.class, "acc").where(Filters.eq("firstName", "J")).build().query());
        assertEquals("SELECT o.id FROM orders o CROSS JOIN account acc WHERE first_name = ?",
                PSC.select("o.id").from("orders o").crossJoin(Account.class, "acc").where(Filters.eq("firstName", "J")).build().query());
        // the alias-less overload derives "acc" from @Table(alias = "acc") and registers it the same way
        assertEquals("SELECT o.id FROM orders o JOIN account acc ON o.aid = acc.id WHERE first_name = ?",
                PSC.select("o.id").from("orders o").join(Account.class).on("o.aid = acc.id").where(Filters.eq("firstName", "J")).build().query());

        final SqlBuilder builder = PSC.select("o.id").from("orders o").join(Account.class, "acc");
        assertNull(builder._propColumnNameMap);
        builder.on("o.aid = acc.id").build();
    }

    @Test
    public void testClosedBuilderTakesPrecedenceOverArgumentValidation() {
        // every clause method reports the closed state first, like limit(int) and append(String) already did
        final SqlBuilder closed = PSC.select("*").from("t");
        closed.build();

        assertThrows(IllegalStateException.class, () -> closed.offset(-1));
        assertThrows(IllegalStateException.class, () -> closed.offsetRows(-1));
        assertThrows(IllegalStateException.class, () -> closed.fetchNextRows(-1));
        assertThrows(IllegalStateException.class, () -> closed.fetchFirstRows(-1));
        assertThrows(IllegalStateException.class, () -> closed.appendIfOrElse(true, (Condition) null, (Condition) null));
        assertThrows(IllegalStateException.class, () -> closed.appendIfOrElse(true, (String) null, (String) null));
        assertThrows(IllegalStateException.class, () -> closed.groupBy(" ", SortDirection.ASC));
        assertThrows(IllegalStateException.class, () -> closed.orderBy(" ", SortDirection.DESC));
        assertThrows(IllegalStateException.class, () -> closed.groupBy("a", (SortDirection) null));
        assertThrows(IllegalStateException.class, () -> closed.orderBy("a", (SortDirection) null));
        assertThrows(IllegalStateException.class, () -> closed.groupBy(Arrays.asList("a"), (SortDirection) null));
        assertThrows(IllegalStateException.class, () -> closed.orderBy(Arrays.asList("a"), (SortDirection) null));

        // positive control: an OPEN builder still rejects exactly these arguments, so the checks above are
        // about precedence, not about the argument validation having been dropped
        final SqlBuilder open1 = PSC.select("*").from("t");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> open1.offset(-1)).getMessage().contains("offset"));
        assertThrows(IllegalArgumentException.class, () -> open1.offsetRows(-1));
        assertThrows(IllegalArgumentException.class, () -> open1.fetchNextRows(-1));
        assertThrows(IllegalArgumentException.class, () -> open1.fetchFirstRows(-1));
        assertThrows(IllegalArgumentException.class, () -> open1.appendIfOrElse(true, (Condition) null, (Condition) null));
        assertThrows(IllegalArgumentException.class, () -> open1.appendIfOrElse(false, "x", (String) null));
        assertThrows(IllegalArgumentException.class, () -> open1.groupBy(" ", SortDirection.ASC));
        assertThrows(IllegalArgumentException.class, () -> open1.orderBy("a", (SortDirection) null));
        open1.build(); // recycle the pooled StringBuilder
    }

    @Test
    public void testGroupByOrderByWithDirectionReportBlankExpr() {
        // pre-built locals so the rejected builders release their pooled StringBuilder
        final SqlBuilder groupByBuilder = PSC.select("*").from("t");
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> groupByBuilder.groupBy(" ", SortDirection.ASC));
        assertTrue(e1.getMessage().contains("expr"), e1.getMessage());
        groupByBuilder.build();

        final SqlBuilder orderByBuilder = PSC.select("*").from("t");
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> orderByBuilder.orderBy(" ", SortDirection.DESC));
        assertTrue(e2.getMessage().contains("expr"), e2.getMessage());
        orderByBuilder.build();

        assertEquals("SELECT * FROM t GROUP BY a ASC ORDER BY b DESC",
                PSC.select("*").from("t").groupBy("a", SortDirection.ASC).orderBy("b", SortDirection.DESC).build().query());
    }

    @Test
    public void testRawSqlInlinedNegativeBindingAfterMinusDoesNotFormLineComment() {
        final String sql = SCSB.select("id")
                .from("t")
                .where(Filters.in("id", Filters.subQuery("SELECT x FROM u WHERE a = -? AND b = 1", List.of(-5))))
                .build()
                .query();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT x FROM u WHERE a = - -5 AND b = 1)", sql);
        assertFalse(sql.contains("--"));

        final String sql2 = SCSB.select("id")
                .from("t")
                .where(Filters.in("id", Filters.subQuery("SELECT x FROM u WHERE a = 10-? AND b = ?", List.of(-5.5, 3))))
                .build()
                .query();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT x FROM u WHERE a = 10- -5.5 AND b = 3)", sql2);

        // Positive values are unchanged.
        assertEquals("SELECT id FROM t WHERE id IN (SELECT x FROM u WHERE a = 10-5)",
                SCSB.select("id").from("t").where(Filters.in("id", Filters.subQuery("SELECT x FROM u WHERE a = 10-?", List.of(5)))).build().query());
    }

    @Test
    public void testHashMinusOperatorFollowedByLineCommentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").where(Filters.eq("id #--x\n OR 1", 1)).build());
        assertThrows(IllegalArgumentException.class, () -> PSB.select("id #-- x\n, secret").from("t").build());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").groupBy("id #--x").build());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").orderBy("id #--x").build());

        // The PostgreSQL #- operator itself is still accepted.
        assertEquals("SELECT id FROM t WHERE doc #- '{a}' = ?", PSB.select("id").from("t").where(Filters.eq("doc #- '{a}'", 1)).build().query());
    }

    @Test
    public void testFromWithNonBeanEntityClassDoesNotDependOnAlias() {
        assertEquals("SELECT a FROM t x", PSC.select("a").from("t x", Map.class).build().query());
        assertEquals("SELECT a FROM t", PSC.select("a").from("t", Map.class).build().query());
        assertEquals("INSERT INTO bk (a) SELECT a FROM t", PSC.select("a").into("bk", Map.class).from("t").build().query());
    }

    @Test
    public void testJoinOnDetectionIgnoresHashOrAtPrefixedIdentifier() {
        assertEquals("SELECT * FROM users u JOIN #on o ON u.id = o.uid", PSC.select("*").from("users u").join("#on o").on("u.id = o.uid").build().query());
        assertEquals("SELECT * FROM users u JOIN @on o ON u.id = o.uid", PSC.select("*").from("users u").join("@on o").on("u.id = o.uid").build().query());
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users u").join("orders o ON u.id = o.uid").on("x = y"));
    }

    @Test
    public void testFromNonReservedJoinModifierWordAsTableAlias() {
        assertEquals("SELECT semi.first_name AS \"firstName\" FROM account semi, device d",
                PSC.select("firstName").from("account semi, device d", Account.class).build().query());
        assertEquals("SELECT anti.first_name AS \"firstName\" FROM account anti", PSC.select("firstName").from("account anti", Account.class).build().query());
        assertEquals("SELECT a.first_name AS \"firstName\" FROM account a SEMI JOIN device d ON a.id = d.account_id",
                PSC.select("firstName").from("account a SEMI JOIN device d ON a.id = d.account_id", Account.class).build().query());
        assertEquals("SELECT a.first_name AS \"firstName\" FROM account a ASOF LEFT JOIN device d ON a.id = d.account_id",
                PSC.select("firstName").from("account a ASOF LEFT JOIN device d ON a.id = d.account_id", Account.class).build().query());
    }

    @Test
    public void testFromDerivedTableParentSelectListFailureLeavesChildReusable() {
        final SqlBuilder child = PSC.select("id").from("users");
        final SqlBuilder parent = PSC.select("id -- x");
        assertThrows(IllegalArgumentException.class, () -> parent.from(child, "u"));
        assertEquals("SELECT id FROM users", child.build().query());
    }

    @Test
    public void testFromTrailingLineCommentDoesNotSwallowLaterClauses() {
        // from(String) trims the expression; the newline that ended the trailing comment must not be lost
        assertEquals("SELECT * FROM account a -- shard hint\n WHERE id = ?",
                PSC.select("*").from("account a -- shard hint\n").where(Filters.eq("id", 1)).build().query());
        assertEquals("SELECT * FROM account a # shard hint\n WHERE id = ?",
                PSC.select("*").from("account a # shard hint\n").where(Filters.eq("id", 1)).build().query());
        assertEquals("SELECT * FROM account a -- hint\n ORDER BY id", PSC.select("*").from("account a -- hint").orderBy("id").build().query());
        // from(Collection) trims each element and joins with ", "; the comment must not swallow the next table
        assertEquals("SELECT * FROM account a -- hint\n, orders o WHERE a.id = ?",
                PSC.select("*").from(List.of("account a -- hint\n", "orders o")).where(Filters.eq("a.id", 1)).build().query());
        // block comments and comment-like text inside quoted identifiers are unaffected
        assertEquals("SELECT * FROM account a /* hint */ WHERE id = ?", PSC.select("*").from("account a /* hint */").where(Filters.eq("id", 1)).build().query());
        assertEquals("SELECT * FROM \"a -- b\" x WHERE id = ?", PSC.select("*").from("\"a -- b\" x").where(Filters.eq("id", 1)).build().query());
    }

    @Test
    public void testOrderByAfterUnionSelectFromEntityAliasIsUnqualified() {
        String sql = PSB.select("firstName")
                .from(Account.class, "acc")
                .unionSelect(Arrays.asList("firstName"))
                .from(Account.class, "acc2")
                .orderBy("firstName")
                .build()
                .query();
        assertEquals("SELECT acc.firstName FROM account acc UNION SELECT acc2.firstName FROM account acc2 ORDER BY firstName", sql);

        // The right-hand operand's own WHERE still resolves through its alias; only the combined-result ORDER BY drops it.
        sql = PSB.select("firstName")
                .from(Account.class, "acc")
                .unionSelect(Arrays.asList("firstName"))
                .from(Account.class, "acc2")
                .where(Filters.eq("firstName", 1))
                .orderByDesc("firstName")
                .build()
                .query();
        assertEquals("SELECT acc.firstName FROM account acc UNION SELECT acc2.firstName FROM account acc2 WHERE acc2.firstName = ? ORDER BY firstName DESC", sql);

        // Same result as the builder-operand overload, which already dropped the alias.
        sql = PSB.select("firstName").from(Account.class, "acc").union(PSB.select("firstName").from(Account.class, "acc2")).orderBy("firstName").build().query();
        assertEquals("SELECT acc.firstName FROM account acc UNION SELECT acc2.firstName FROM account acc2 ORDER BY firstName", sql);

        // A plain (non-compound) query keeps alias-qualified ORDER BY columns.
        assertEquals("SELECT acc.firstName FROM account acc ORDER BY acc.firstName", PSB.select("firstName").from(Account.class, "acc").orderBy("firstName").build().query());
    }

    @Test
    public void testParentTableExclusionsApplyToExpandedChildrenAndFromTables() {
        for (final Class<?> type : List.of(ExcludedChildParent.class, WhitelistedParent.class)) {
            assertEquals(List.of("id"), QueryUtil.selectPropNames(type, true, null));
            assertFalse(QueryUtil.propToColumnNameMap(type, NamingPolicy.SNAKE_CASE).containsKey("child.value"));
            final String query = PSC.selectFrom(type, true).build().query();
            assertFalse(query.contains("review_child"), query);
            assertTrue(query.startsWith("SELECT id AS \"id\" FROM "), query);
        }
    }

    @Test
    public void testExplicitlySelectedExcludedSubEntityKeepsItsFromTable() {
        // The explicitly selected root is still expanded, so its table must be listed in FROM.
        assertEquals("SELECT review_child.stored_value AS \"child.value\" FROM review_parent, review_child",
                PSC.selectFrom(Selection.builder(ExcludedChildParent.class).includedPropNames(List.of("child")).build()).build().query());
        assertEquals("SELECT review_child.stored_value AS \"child.value\" FROM review_whitelisted_parent, review_child",
                PSC.selectFrom(Selection.builder(WhitelistedParent.class).includedPropNames(List.of("child")).build()).build().query());

        // Default projections still exclude the child and its table.
        assertEquals("SELECT id AS \"id\" FROM review_parent",
                PSC.selectFrom(Selection.builder(ExcludedChildParent.class).includeSubEntityProperties(true).build()).build().query());
    }

    @Test
    public void testNonAsciiAliasCharactersRemainPartOfTheTableAlias() {
        for (final String alias : List.of("á", "a𐐀")) {
            final String query = PSC.select("id").from(ExcludedChildParent.class, alias).where(Filters.eq("id", 7)).build().query();
            assertEquals("SELECT " + alias + ".id AS \"id\" FROM review_parent " + alias + " WHERE " + alias + ".id = ?", query);
        }
    }

    @Test
    public void testUnicodeWhitespaceIsNotPartOfTheTableAlias() {
        for (final String space : List.of("　", " ")) {
            final String query = PSC.select("id").from("review_parent u" + space, ExcludedChildParent.class).where(Filters.eq("u.id", 1)).build().query();
            assertTrue(query.startsWith("SELECT u.id AS \"id\" FROM review_parent u"), query);
            assertTrue(query.endsWith(" WHERE u.id = ?"), query);
        }
    }

    @Test
    public void testSelectingSubEntityRootPreservesResultClassAlias() {
        final String root = PSC.selectFrom(Selection.builder(IncludedChildParent.class).classAlias("parent").includedPropNames(List.of("child")).build())
                .build()
                .query();
        final String expanded = PSC
                .selectFrom(Selection.builder(IncludedChildParent.class).classAlias("parent").includedPropNames(List.of("child.value")).build())
                .build()
                .query();
        assertEquals(expanded, root);
        assertTrue(root.contains("AS \"parent.child.value\""), root);
    }

    @Test
    public void testJoinedEntityMappingsUseTheRenderedAliasToken() {
        for (final String alias : List.of(" c ", "AS c", "AS\tc")) {
            final String query = PSC.select("*").from("review_parent p").join(ReviewChild.class, alias).on(Filters.eq("c.value", 7)).build().query();
            assertTrue(query.endsWith(" ON c.stored_value = ?"), query);
        }
    }

    @Test
    public void testMybatisBindingMetadataIsPreservedDuringExpressionConversion() {
        final String expression = "firstName = #{ firstName, jdbcType=VARCHAR } AND lastName = ${ lastName }";
        final String expected = "first_name = #{ firstName, jdbcType=VARCHAR } AND last_name = ${ lastName }";
        assertEquals(expected, PSC.renderCondition(Filters.expr(expression)).build().query());
        assertEquals("SELECT * FROM people WHERE " + expected, PSC.select("*").from("people").where(expression).build().query());

        // A quoted '}' inside marker attributes does not end the marker.
        assertEquals("SELECT * FROM people WHERE foo_bar = #{a, typeHandler='}'} AND bar_baz = 1",
                PSC.select("*").from("people").where("fooBar = #{a, typeHandler='}'} AND barBaz = 1").build().query());
    }

    @Test
    public void testUnterminatedMybatisMarkerDoesNotSuppressConversion() {
        assertEquals("SELECT * FROM people WHERE foo_bar = #{ foo_bar AND bar_baz = 1",
                PSC.select("*").from("people").where("fooBar = #{ fooBar AND barBaz = 1").build().query());
    }

    @Test
    public void testCommentOnlyRawPredicatesAreRejectedWithoutClaimingTheClause() {
        for (final String expression : List.of("-- comment", "/* comment */", "# comment", "-- Keep comments\n/* comment */")) {
            final SqlBuilder builder = PSC.select("*").from("people");
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> builder.where(expression));
            assertTrue(e.getMessage().contains(expression), e.getMessage());
            assertEquals("SELECT * FROM people WHERE id = ?", builder.where(Filters.eq("id", 7)).build().query());

            final SqlBuilder grouped = PSC.select("a").from("people").groupBy("a");
            assertThrows(IllegalArgumentException.class, () -> grouped.having(expression));
            assertEquals("SELECT a FROM people GROUP BY a HAVING COUNT(*) > 1", grouped.having("COUNT(*) > 1").build().query());

            final SqlBuilder joined = PSC.select("*").from("people p").join("orders o");
            assertThrows(IllegalArgumentException.class, () -> joined.on("p.id = o.person_id", expression));
            assertEquals("SELECT * FROM people p JOIN orders o ON p.id = o.person_id", joined.on("p.id = o.person_id").build().query());
        }
    }

    @Test
    public void testHashPrefixedExpressionIsRejectedOutsideSqlServer() {
        // Outside SQL Server, '#' opens a MySQL hash comment, so the expression would render no predicate.
        final SqlBuilder builder = PSC.select("*").from("people");
        assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.expr("#tmp.id = 1")));
        assertThrows(IllegalArgumentException.class, () -> builder.where("#tmp.id = 1"));
        assertEquals("SELECT * FROM people WHERE id = ?", builder.where(Filters.eq("id", 1)).build().query());

        final Dsl sqlServer = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());
        assertEquals("SELECT * FROM #tmp WHERE #tmp.id = 1", sqlServer.select("*").from("#tmp").where(Filters.expr("#tmp.id = 1")).build().query());
    }

    @Test
    public void testExplicitAliasEqualToExpressionSuffixIsKept() {
        // Only a whole-expression match ("name AS name") makes the alias redundant; a suffix match must keep it.
        assertEquals("SELECT unit_price * quantity AS quantity FROM t", PSC.select("unitPrice * quantity AS quantity").from("t").build().query());
        assertEquals("SELECT first_name || ' ' || last_name AS last_name FROM t",
                PSB.select("first_name || ' ' || last_name AS last_name").from("t").build().query());
        assertEquals("SELECT a + b AS \"b\" FROM t", PSC.select(Map.of("a + b", "b")).from("t").build().query());

        assertEquals("SELECT name FROM t", PSB.select("name AS name").from("t").build().query());
        assertEquals("SELECT t.name AS name FROM t", PSB.select("t.name AS name").from("t").build().query());
    }

    @Test
    public void testDerivedTableAliasLineCommentIsTerminated() {
        final String sql = PSC.select("*").from(PSC.select("id").from("users"), "u -- note").where(Filters.eq("x", 1)).build().query();
        assertEquals("SELECT * FROM (SELECT id FROM users) u -- note\n WHERE x = ?", sql);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRawSqlInlinedExpressionBindingLineCommentIsTerminated() {
        final SubQuery subQuery = Filters.subQuery("SELECT id FROM u WHERE a = ? AND b = 2", List.of(SqlExpression.of("x -- c")));
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE a = x -- c\n AND b = 2)",
                SCSB.select("id").from("t").where(Filters.in("id", subQuery)).build().query());
    }

    @Test
    public void testUnterminatedBracketDoesNotHideCommentToken() {
        assertThrows(IllegalArgumentException.class, () -> PSB.update("t").set("a[1 -- x", 1));
        assertThrows(IllegalArgumentException.class, () -> PSB.update("t").set("a[/* x", 1));

        // A terminated bracket-quoted identifier still hides its content outside the MySQL/PostgreSQL families.
        assertEquals("UPDATE t SET [a--b] = ? WHERE id = ?", PSB.update("t").set("[a--b]", 1).where(Filters.eq("id", 1)).build().query());

        // PostgreSQL brackets are array subscripts, so a comment token inside them is a real comment.
        final Dsl postgres = Dsl.forDialect(PSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        assertThrows(IllegalArgumentException.class, () -> postgres.update("t").set("a[--]", 1));
    }

    @Test
    public void testSqliteBracketQuotedIdentifiersHideCommentTokens() {
        // SQLite accepts [..] identifier quoting (and has no array subscripts), so [a--b] is one identifier there.
        final Dsl sqlite = Dsl.forDialect(PSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("SQLite")).build());
        assertEquals("UPDATE t SET [a--b] = ? WHERE id = ?", sqlite.update("t").set("[a--b]", 1).where(Filters.eq("id", 1)).build().query());
        assertThrows(IllegalArgumentException.class, () -> sqlite.update("t").set("a[1 -- x", 1));

        // SQLite ends a bracket identifier at the first ']' ("]]" is no escape), so the "--" after it is a real comment.
        assertThrows(IllegalArgumentException.class, () -> sqlite.update("t").set("[a]]--x]", 1));
        assertEquals("UPDATE t SET [a]]--x] = ?", PSB.update("t").set("[a]]--x]", 1).build().query());

        // A trailing line comment after a bracket identifier is still terminated before the next clause.
        assertEquals("SELECT [a--b] FROM t -- c\n WHERE id = ?", sqlite.select("[a--b]").from("t -- c").where(Filters.eq("id", 1)).build().query());
    }

    @Test
    public void testUnresolvedCommaLimitIsRenderedPortably() {
        final Dsl postgres = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        assertEquals("SELECT * FROM users LIMIT 99999999999 OFFSET 5",
                postgres.select("*").from("users").append(new Limit("LIMIT 5, 99999999999")).build().query());
        assertEquals("SELECT * FROM users LIMIT 99999999999 OFFSET 5", PSC.select("*").from("users").append(new Limit("LIMIT 5, 99999999999")).build().query());
        assertEquals("SELECT * FROM users LIMIT 99999999999", postgres.select("*").from("users").append(new Limit("LIMIT 99999999999")).build().query());
    }

    @Test
    public void testSubEntityTableIsOmittedWhenAllItsPropertiesAreExcluded() {
        final Set<String> excluded = new java.util.HashSet<>();

        for (final String propName : QueryUtil.selectPropNames(Account.class, true, null)) {
            if (propName.startsWith("devices.")) {
                excluded.add(propName);
            }
        }

        final String sql = PSC.selectFrom(Account.class, "a", true, excluded).build().query();
        assertTrue(sql.endsWith(" FROM account a"), sql);
        assertFalse(sql.contains("device"), sql);

        // Keeping one nested property keeps its table.
        excluded.remove("devices.name");
        assertTrue(PSC.selectFrom(Account.class, "a", true, excluded).build().query().endsWith(" FROM account a, device"));
    }

    @Test
    public void testConditionOnlyBuilderRejectsSecondPredicate() {
        final SqlBuilder builder = PSC.renderCondition(Filters.equal("a", 1));
        assertThrows(IllegalStateException.class, () -> builder.append(Filters.equal("b", 2)));
        assertEquals("a = ?", builder.build().query());
    }

    @Test
    public void testCriteriaJoinEntityWithInlineConnectorIsRejected() {
        final SqlBuilder builder = PSC.select("*").from("users u");
        assertThrows(IllegalArgumentException.class,
                () -> builder.append(Criteria.builder().join(new com.landawn.abacus.query.condition.CrossJoin("orders o ON 1=1")).build()));
        assertThrows(IllegalArgumentException.class, () -> builder.append(Criteria.builder().join("orders o ON a = b", Filters.expr("u.id = o.uid")).build()));

        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.uid",
                builder.append(Criteria.builder().join("orders o", Filters.expr("u.id = o.uid")).build()).build().query());
    }

    @Table(name = "review_parent", nonColumnFields = { "child" })
    public static class ExcludedChildParent {
        private int id;
        private ReviewChild child;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public ReviewChild getChild() {
            return child;
        }

        public void setChild(final ReviewChild child) {
            this.child = child;
        }
    }

    @Table(name = "review_whitelisted_parent", columnFields = { "id" })
    public static class WhitelistedParent {
        private int id;
        private ReviewChild child;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public ReviewChild getChild() {
            return child;
        }

        public void setChild(final ReviewChild child) {
            this.child = child;
        }
    }

    @Table(name = "review_child")
    public static class ReviewChild {
        @Column("stored_value")
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @Table(name = "review_included_parent")
    public static class IncludedChildParent extends ExcludedChildParent {
    }

    // Child-placeholder renaming must use the target dialect's string-literal convention: 'C:\' is a
    // complete literal in standard SQL, so the later child :id must still be renamed.
    @Test
    public void testUnionChildPlaceholderRenameUsesStandardStringsOnPostgreSql() {
        final Dsl pg = Dsl.forDialect(NSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        final AbstractQueryBuilder.SP sp = pg.select("id")
                .from("a")
                .where(Filters.eq("id", 1))
                .union(pg.select("id").from("t").where(Filters.and(Filters.expr("path = 'C:\\'"), Filters.eq("id", 2))))
                .build();

        assertEquals("SELECT id FROM a WHERE id = :id UNION SELECT id FROM t WHERE (path = 'C:\\') AND (id = :id_2)", sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());

        final Dsl pgIbatis = Dsl.forDialect(MSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        final String ibatis = pgIbatis.select("id")
                .from("a")
                .where(Filters.eq("id", 1))
                .union(pgIbatis.select("id").from("t").where(Filters.and(Filters.expr("path = 'C:\\'"), Filters.eq("id", 2))))
                .build()
                .query();

        assertTrue(ibatis.endsWith("(path = 'C:\\') AND (id = #{id_2})"), ibatis);
    }

    @Test
    public void testUnionChildPlaceholderRenameUsesBackslashEscapesOnMySql() {
        final Dsl mysql = Dsl.forDialect(NSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        final String sql = mysql.select("id")
                .from("a")
                .where(Filters.eq("id", 1))
                .union(mysql.select("id").from("t").where(Filters.and(Filters.expr("path = 'x\\' AND :id = 1'"), Filters.eq("id", 2))))
                .build()
                .query();

        // Under MySQL, \' does not end the literal: the ":id" text inside it is data and stays untouched.
        assertEquals("SELECT id FROM a WHERE id = :id UNION SELECT id FROM t WHERE (path = 'x\\' AND :id = 1') AND (id = :id_2)", sql);
    }

    @Test
    public void testUnionChildPlaceholderRenameFailsClosedOnAmbiguousDefaultDialectBackslash() {
        // Without productInfo, 'C:\' is complete (standard SQL) or unterminated (MySQL): the two readings
        // rename different placeholders, so the rewrite is rejected instead of guessing.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NSC.select("id")
                .from("a")
                .where(Filters.eq("id", 1))
                .union(NSC.select("id").from("t").where(Filters.and(Filters.expr("path = 'C:\\'"), Filters.eq("id", 2)))));
        assertTrue(ex.getMessage().contains("productInfo"), ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> MSC.select("id")
                .from("a")
                .where(Filters.eq("id", 1))
                .union(MSC.select("id").from("t").where(Filters.and(Filters.expr("path = 'C:\\'"), Filters.eq("id", 2)))));

        // A backslash both readings agree on (an escaped backslash) is still rewritten normally.
        final String sql = NSC.select("id")
                .from("a")
                .where(Filters.eq("id", 1))
                .union(NSC.select("id").from("t").where(Filters.and(Filters.expr("path = 'a\\\\b'"), Filters.eq("id", 2))))
                .build()
                .query();
        assertTrue(sql.endsWith("(path = 'a\\\\b') AND (id = :id_2)"), sql);
    }

    // On PostgreSQL '[' opens an array subscript, not a bracket-quoted identifier: a "]" inside a subscript
    // string must not hide a trailing line comment or expose a quoted semicolon.
    @Test
    public void testPostgreSqlSubscriptIsNotBracketQuotedIdentifier() {
        final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());

        assertEquals("SELECT * FROM t, LATERAL (SELECT t.j['a]'] AS v) x -- c\n WHERE id = ?",
                pg.select("*").from("t, LATERAL (SELECT t.j['a]'] AS v) x -- c").where(Filters.eq("id", 1)).build().query());

        assertEquals("SELECT id FROM t UNION SELECT id FROM u WHERE j['a]'] = '1' -- c\n ORDER BY id",
                pg.select("id").from("t").union("SELECT id FROM u WHERE j['a]'] = '1' -- c").orderBy("id").build().query());

        assertEquals("SELECT id FROM t UNION SELECT id FROM u WHERE j['a]'] = ';'",
                pg.select("id").from("t").union("SELECT id FROM u WHERE j['a]'] = ';'").build().query());
    }

    @Test
    public void testSqlServerBracketQuotedIdentifierStillOpaque() {
        final Dsl sqlServer = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());

        // [a'b] is one identifier: the quote inside it opens no string, so the trailing comment is terminated.
        assertEquals("SELECT id FROM t UNION SELECT [a'b] FROM u -- c\n ORDER BY id",
                sqlServer.select("id").from("t").union("SELECT [a'b] FROM u -- c").orderBy("id").build().query());

        // A semicolon inside a bracket-quoted identifier is not a statement terminator.
        assertEquals("SELECT id FROM t UNION SELECT [a;b] FROM u", sqlServer.select("id").from("t").union("SELECT [a;b] FROM u").build().query());

        assertEquals("SELECT * FROM t [x -- y] -- c\n WHERE id = ?",
                sqlServer.select("*").from("t [x -- y] -- c").where(Filters.eq("id", 1)).build().query());
    }

    // A select modifier ending in a line comment must not swallow the select list and later clauses,
    // whether it is emitted by from(...), spliced in after from(...), or carried by an appended Criteria.
    @Test
    public void testSelectModifierEndingInLineCommentIsTerminated() {
        final String expected = "SELECT DISTINCT -- x\n id, name FROM t WHERE a = ?";

        assertEquals(expected, PSC.select("id", "name").selectModifier("DISTINCT -- x").from("t").where(Filters.eq("a", 1)).build().query());
        assertEquals(expected, PSC.select("id", "name").from("t").selectModifier("DISTINCT -- x").where(Filters.eq("a", 1)).build().query());
        assertEquals(expected, PSC.select("id", "name")
                .from("t")
                .append(Criteria.builder().selectModifier("DISTINCT -- x").where(Filters.eq("a", 1)).build())
                .build()
                .query());

        // A modifier without a trailing comment is unchanged.
        assertEquals("SELECT DISTINCT id FROM t", PSC.select("id").from("t").selectModifier("DISTINCT").build().query());
    }

    // A sub-entity table contributing no column must not be listed in FROM (Cartesian product), with or
    // without exclusions and through the Selection path alike.
    @Test
    public void testSelectFromOmitsColumnlessSubEntityTable() {
        final String expected = "SELECT o.id AS \"id\", o.name AS \"name\" FROM columnless_owner o";

        assertEquals(expected, PSC.selectFrom(ColumnlessSubEntityOwner.class, "o", true, null).build().query());
        assertEquals("SELECT o.id AS \"id\" FROM columnless_owner o", PSC.selectFrom(ColumnlessSubEntityOwner.class, "o", true, Set.of("name")).build().query());
        assertEquals(expected,
                PSC.selectFrom(Selection.builder(ColumnlessSubEntityOwner.class).tableAlias("o").includeSubEntityProperties(true).build()).build().query());

        // A sub-entity that contributes columns is still listed.
        assertTrue(PSC.selectFrom(Account.class, "a", true, null).build().query().endsWith(" FROM account a, device"));
        assertTrue(PSC.selectFrom(Account.class, "a", true, Set.of("firstName")).build().query().endsWith(" FROM account a, device"));
    }

    // An alias-less Selection that lists a sub-entity table falls back to the entity's @Table alias, exactly
    // like selectFrom(Class, null, true), so the parent's "id" is not ambiguous next to the sub-entity's.
    @Test
    public void testSelectFromSelectionFallsBackToTableAliasWithSubEntityTables() {
        final String expected = "SELECT o.id AS \"id\", o.status AS \"status\", c.id AS \"customer.id\", c.name AS \"customer.name\" FROM aliased_orders o, aliased_customer c";

        assertEquals(expected, PSC.selectFrom(AliasedOrder.class, (String) null, true).build().query());
        assertEquals(expected, PSC.selectFrom(Selection.builder(AliasedOrder.class).includeSubEntityProperties(true).build()).build().query());
        assertEquals(expected, PSC.selectFrom(List.of(Selection.builder(AliasedOrder.class).includeSubEntityProperties(true).build())).build().query());

        // An included sub-entity root also lists the sub-entity table, so the fallback applies too.
        assertEquals("SELECT o.id AS \"id\", c.id AS \"customer.id\", c.name AS \"customer.name\" FROM aliased_orders o, aliased_customer c",
                PSC.selectFrom(Selection.builder(AliasedOrder.class).includedPropNames(List.of("id", "customer")).build()).build().query());

        // No sub-entity table: no fallback. An explicit alias is kept.
        assertEquals("SELECT id AS \"id\", status AS \"status\" FROM aliased_orders", PSC.selectFrom(Selection.builder(AliasedOrder.class).build()).build().query());
        assertEquals("SELECT x.id AS \"id\", x.status AS \"status\", c.id AS \"customer.id\", c.name AS \"customer.name\" FROM aliased_orders x, aliased_customer c",
                PSC.selectFrom(Selection.builder(AliasedOrder.class).tableAlias("x").includeSubEntityProperties(true).build()).build().query());

        // select(List) leaves FROM to the caller, so it does not fall back.
        assertEquals("SELECT id AS \"id\", status AS \"status\", c.id AS \"customer.id\", c.name AS \"customer.name\" FROM aliased_orders",
                PSC.select(Selection.builder(AliasedOrder.class).includeSubEntityProperties(true).build()).from("aliased_orders").build().query());
    }

    // The predicate rules are shared with ON, but a rejection must name the clause actually being built.
    @Test
    public void testRejectedPredicateMessageNamesClause() {
        final IllegalArgumentException where = assertThrows(IllegalArgumentException.class,
                () -> PSC.select("*").from("t").where(SqlExpression.of("-- x")).build().query());
        assertTrue(where.getMessage().startsWith("WHERE condition type "), where.getMessage());

        final IllegalArgumentException implicitWhere = assertThrows(IllegalArgumentException.class,
                () -> PSC.select("*").from("t").append(Filters.expr(" ")).build().query());
        assertTrue(implicitWhere.getMessage().startsWith("WHERE condition type "), implicitWhere.getMessage());

        final IllegalArgumentException having = assertThrows(IllegalArgumentException.class,
                () -> PSC.select("*").from("t").groupBy("a").having(Filters.expr(" ")).build().query());
        assertTrue(having.getMessage().startsWith("HAVING condition type "), having.getMessage());

        final IllegalArgumentException on = assertThrows(IllegalArgumentException.class,
                () -> PSC.select("*").from("t").innerJoin("u").on(Filters.expr(" ")).build().query());
        assertTrue(on.getMessage().startsWith("ON condition type "), on.getMessage());
    }

    @Test
    public void testAppendLimitAfterFetchReportsLimitAlreadySet() {
        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> PSC.select("*").from("t").fetchFirstRows(3).append(new Limit("FETCH FIRST 99999999999 ROWS ONLY")));
        assertEquals("'LIMIT' has already been set and cannot be set again", e.getMessage());

        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("t").limit(3).append(new Limit("LIMIT 99999999999")));
    }

    // The bare predicate of a condition-only builder is meant to follow a WHERE keyword, so a later WHERE
    // ("a = ? WHERE b = 1") is malformed and rejected on every route.
    @Test
    public void testConditionOnlyBuilderRejectsWhereAfterBarePredicate() {
        assertThrows(IllegalStateException.class, () -> PSC.renderCondition(Filters.eq("a", 1)).where("b = 1"));
        assertThrows(IllegalStateException.class, () -> PSC.renderCondition(Filters.eq("a", 1)).where(Filters.eq("b", 1)));
        assertThrows(IllegalStateException.class, () -> PSC.renderCondition(Filters.eq("a", 1)).append(Filters.where(Filters.eq("b", 1))));
        assertThrows(IllegalStateException.class, () -> PSC.renderCondition(Filters.eq("a", 1)).append(Criteria.builder().where(Filters.eq("b", 1)).build()));

        // Clauses that may legally follow a WHERE predicate are still accepted.
        assertEquals("a = ? ORDER BY b", PSC.renderCondition(Filters.eq("a", 1)).orderBy("b").build().query());
        assertEquals("WHERE a = ? ORDER BY b", PSC.renderCondition(Criteria.builder().where(Filters.eq("a", 1)).orderBy("b").build()).build().query());
    }

    @Test
    public void testNullMapDirectionMessageNamesKey() {
        final Map<String, SortDirection> map = new LinkedHashMap<>();
        map.put("x", null);

        assertEquals("Direction for key 'x' in groupings must not be null",
                assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("t").groupBy(map)).getMessage());
        assertEquals("Direction for key 'x' in orders must not be null",
                assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("t").orderBy(map)).getMessage());
    }

    // MySQL compound INTERVAL units and Oracle binary floating-point types contain underscores but are keywords,
    // not snake_case column names: a naming policy must not camelCase them.
    @Test
    public void testUnderscoreKeywordsNotConvertedByNamingPolicy() {
        assertTrue(PLC.select("*").from("t").where(Filters.expr("x > INTERVAL '1-2' YEAR_MONTH")).build().query().contains("YEAR_MONTH"));

        final String sql = PLC.select("CAST(x AS BINARY_DOUBLE)").from("t").orderBy("x + INTERVAL '1' DAY_HOUR").build().query();
        assertTrue(sql.contains("BINARY_DOUBLE"), sql);
        assertTrue(sql.contains("DAY_HOUR"), sql);
    }

    @Table(name = "columnless_owner")
    public static class ColumnlessSubEntityOwner {
        private int id;
        private String name;
        private ColumnlessSubEntity wrapper;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public ColumnlessSubEntity getWrapper() {
            return wrapper;
        }

        public void setWrapper(final ColumnlessSubEntity wrapper) {
            this.wrapper = wrapper;
        }
    }

    /** A sub-entity whose only property is itself a nested bean, so it maps no column of its own. */
    @Table(name = "columnless_wrapper")
    public static class ColumnlessSubEntity {
        private NestedAddress addr;

        public NestedAddress getAddr() {
            return addr;
        }

        public void setAddr(final NestedAddress addr) {
            this.addr = addr;
        }
    }

    public static class NestedAddress {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    @Table(name = "aliased_orders", alias = "o")
    public static class AliasedOrder {
        private int id;
        private String status;
        private AliasedCustomer customer;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(final String status) {
            this.status = status;
        }

        public AliasedCustomer getCustomer() {
            return customer;
        }

        public void setCustomer(final AliasedCustomer customer) {
            this.customer = customer;
        }
    }

    @Table(name = "aliased_customer", alias = "c")
    public static class AliasedCustomer {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testRawSubQuerySqlExpressionBindingRendersIdenticallyUnderEveryPolicy() {
        // Before: PARAMETERIZED/NAMED/IBATIS builders passed the SqlExpression object to JDBC as a value.
        final SubQuery sq = Filters.subQuery("SELECT id FROM x WHERE created < ? AND k = ?", Arrays.asList(SqlExpression.of("NOW()"), 5));

        AbstractQueryBuilder.SP sp = PSC.select("id").from("t").where(Filters.in("id", sq)).build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM x WHERE created < NOW() AND k = ?)", sp.query());
        assertEquals(Arrays.asList(5), sp.parameters());

        sp = NSC.select("id").from("t").where(Filters.in("id", sq)).build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM x WHERE created < NOW() AND k = :param)", sp.query());
        assertEquals(Arrays.asList(5), sp.parameters());

        sp = MSC.select("id").from("t").where(Filters.in("id", sq)).build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM x WHERE created < NOW() AND k = #{param})", sp.query());
        assertEquals(Arrays.asList(5), sp.parameters());

        sp = SCSB.select("id").from("t").where(Filters.in("id", sq)).build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM x WHERE created < NOW() AND k = 5)", sp.query());
        assertTrue(sp.parameters().isEmpty());

        // A raw sub-query binding becomes a scalar sub-query, its bindings merged in placeholder order.
        final SubQuery scalar = Filters.subQuery("SELECT id FROM o WHERE a = ? AND total = ? AND b = ?",
                Arrays.asList(1, Filters.subQuery("SELECT MAX(total) FROM o WHERE s = ?", Arrays.asList("OPEN")), 2));
        sp = NSC.select("id").from("t").where(Filters.in("id", scalar)).build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM o WHERE a = :param AND total = (SELECT MAX(total) FROM o WHERE s = :param_2) AND b = :param_3)",
                sp.query());
        assertEquals(Arrays.asList(1, "OPEN", 2), sp.parameters());
    }

    // Regression: a sort direction after a sub-entity property (which expands to the list of its columns) was applied to the LAST expanded column only.
    @Test
    public void testSortDirectionAppliesToEveryExpandedSubEntityColumn() {
        final String undirected = PSC.select("id").from(Account.class).orderBy("devices").build().query();
        final String prefix = "SELECT acc.id AS \"id\" FROM account acc ORDER BY ";
        assertTrue(undirected.startsWith(prefix + "device.id, device.account_id, "), undirected);

        final String columns = undirected.substring(prefix.length());
        final String desc = columns.replace(", ", " DESC, ") + " DESC";
        final String asc = columns.replace(", ", " ASC, ") + " ASC";

        assertEquals(prefix + desc, PSC.select("id").from(Account.class).orderByDesc("devices").build().query());
        assertEquals(prefix + desc, PSC.select("id").from(Account.class).orderBy("devices", SortDirection.DESC).build().query());
        assertEquals(prefix + "acc.first_name DESC, " + desc,
                PSC.select("id").from(Account.class).orderBy(List.of("firstName", "devices"), SortDirection.DESC).build().query());

        final Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("devices", SortDirection.ASC);
        orders.put("id", SortDirection.DESC);
        assertEquals(prefix + asc + ", acc.id DESC", PSC.select("id").from(Account.class).orderBy(orders).build().query());

        final String groupPrefix = "SELECT acc.id AS \"id\" FROM account acc GROUP BY ";
        assertEquals(groupPrefix + desc, PSC.select("id").from(Account.class).groupByDesc("devices").build().query());
        assertEquals(groupPrefix + asc, PSC.select("id").from(Account.class).groupByAsc(List.of("devices")).build().query());
        assertEquals(groupPrefix + asc + ", acc.id DESC", PSC.select("id").from(Account.class).groupBy(orders).build().query());

        // Plain columns are unaffected.
        assertEquals("SELECT acc.id AS \"id\" FROM account acc ORDER BY acc.first_name DESC",
                PSC.select("id").from(Account.class).orderBy("firstName", SortDirection.DESC).build().query());
    }

    // Regression: a raw sub-query '?' right after ':' (PostgreSQL array slice) was renamed to "::param" under NAMED_SQL -- a type cast, not a parameter.
    @Test
    public void testRawSubQueryPlaceholderAfterColonIsNotRenamedIntoTypeCast() {
        final SubQuery slice = Filters.subQuery("SELECT id FROM u WHERE x = ANY(arr[?:?])", Arrays.asList(1, 3));

        AbstractQueryBuilder.SP sp = NSC.select("id").from("t").where(Filters.in("id", slice)).build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE x = ANY(arr[:param: :param_2]))", sp.query());
        assertEquals(Arrays.asList(1, 3), sp.parameters());
        assertEquals(2, ParsedSql.parse(sp.query()).parameterCount());

        sp = NSC.select("id").from("t").where(Filters.in("id", Filters.subQuery("SELECT id FROM u WHERE arr[1:?] = x", Arrays.asList(3)))).build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE arr[1: :param] = x)", sp.query());
        assertEquals(1, ParsedSql.parse(sp.query()).parameterCount());

        // A trailing "::" cast after the placeholder stays a cast; MyBatis and raw renderings are unchanged.
        sp = NSC.select("id").from("t").where(Filters.in("id", Filters.subQuery("SELECT id FROM u WHERE x = ?::int", Arrays.asList(1)))).build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE x = :param::int)", sp.query());
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE x = ANY(arr[#{param}:#{param_2}]))",
                MSC.select("id").from("t").where(Filters.in("id", slice)).build().query());
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE x = ANY(arr[1:3]))",
                SCSB.select("id").from("t").where(Filters.in("id", slice)).build().query());
    }

    // Regression: RAW_SQL under the MySQL dialect only doubled quotes, so a value ending in '\' escaped the closing quote (injection) and "a\b" was stored as a backspace.
    @Test
    @SuppressWarnings("deprecation")
    public void testMySqlRawSqlLiteralsEscapeBackslashes() {
        final Dsl mysqlRaw = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());

        assertEquals("DELETE FROM account WHERE (name = 'x\\\\') AND (gui = ' OR 1=1 -- ')",
                mysqlRaw.deleteFrom("account").where(Filters.and(Filters.eq("name", "x\\"), Filters.eq("gui", " OR 1=1 -- "))).build().query());

        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("path", "a\\b");
        props.put("note", "it's");
        assertEquals("INSERT INTO account (path, note) VALUES ('a\\\\b', 'it''s')", mysqlRaw.insert(props).into("account").build().query());

        assertEquals("SELECT id FROM t WHERE c = '\\\\'", mysqlRaw.select("id").from("t").where(Filters.eq("c", '\\')).build().query());
        assertEquals("SELECT id FROM t WHERE name IN ('a\\\\', 'b')", mysqlRaw.select("id").from("t").where(Filters.in("name", List.of("a\\", "b"))).build().query());

        // Raw sub-query bindings inlined under RAW_SQL get the same escaping.
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE a = 'x\\\\' AND b = ' OR 1=1 -- ')",
                mysqlRaw.select("id")
                        .from("t")
                        .where(Filters.in("id", Filters.subQuery("SELECT id FROM u WHERE a = ? AND b = ?", Arrays.asList("x\\", " OR 1=1 -- "))))
                        .build()
                        .query());

        // A SqlExpression is SQL, not a value: emitted verbatim.
        assertEquals("SELECT id FROM t WHERE name = 'x\\'", mysqlRaw.select("id").from("t").where(Filters.eq("name", SqlExpression.of("'x\\'"))).build().query());

        // Other dialects keep the SQL-standard rendering (a backslash is an ordinary character).
        assertEquals("DELETE FROM account WHERE name = 'x\\'", SCSB.deleteFrom("account").where(Filters.eq("name", "x\\")).build().query());
        final Dsl pgRaw = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        assertEquals("DELETE FROM account WHERE name = 'x\\'", pgRaw.deleteFrom("account").where(Filters.eq("name", "x\\")).build().query());
    }

    // Regression: an inline "expr AS alias" in a class-aliased Selection rendered the dotted alias unquoted ("AS acc.fn", invalid SQL).
    @Test
    public void testMultiSelectInlineAliasWithClassAliasIsQuoted() {
        assertEquals("SELECT a.first_name AS \"acc.fn\", UPPER(a.last_name) AS \"acc.ln\", d.name AS \"dev.dn\" FROM account a, device d",
                PSC.selectFrom(List.of(
                        Selection.builder(Account.class)
                                .tableAlias("a")
                                .classAlias("acc")
                                .includedPropNames(List.of("firstName AS fn", "UPPER(lastName) AS ln"))
                                .build(),
                        Selection.builder(com.landawn.abacus.query.entity.AccountDevice.class)
                                .tableAlias("d")
                                .classAlias("dev")
                                .includedPropNames(List.of("name AS dn"))
                                .build()))
                        .build()
                        .query());

        // Without a class alias the user's inline alias stays verbatim.
        assertEquals("SELECT a.first_name AS fn, d.name AS dn FROM account a, device d",
                PSC.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").includedPropNames(List.of("firstName AS fn")).build(),
                        Selection.builder(com.landawn.abacus.query.entity.AccountDevice.class).tableAlias("d").includedPropNames(List.of("name AS dn")).build()))
                        .build()
                        .query());
    }

    // Regression: expression items of a non-first Selection were resolved against the FIRST selection's entity and table alias ("a.status + 1" for selection "d").
    @Test
    public void testMultiSelectExpressionItemsResolveAgainstTheirOwnSelection() {
        assertEquals(
                "SELECT a.id AS \"id\", d.status + 1 AS \"status + 1\", COALESCE(d.status, 0) AS \"COALESCE(status, 0)\", d.status AS st, (d.status) AS \"(status)\""
                        + " FROM account a, device d WHERE a.status = ?",
                PSC.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").includedPropNames(List.of("id")).build(),
                        Selection.builder(com.landawn.abacus.query.entity.AccountDevice.class)
                                .tableAlias("d")
                                .includedPropNames(List.of("status + 1", "COALESCE(status, 0)", "status AS st", "(status)"))
                                .build()))
                        // the builder-level entity/alias (first selection) is restored for the clauses that follow
                        .where(Filters.eq("status", 1))
                        .build()
                        .query());
    }

    // Regression: the implicit select alias wrapped raw expression text in identifier quotes without escaping a quote inside it (invalid SQL).
    @Test
    public void testImplicitSelectAliasEscapesEmbeddedIdentifierQuote() {
        assertEquals("SELECT COALESCE(\"nickName\", first_name) AS \"COALESCE(\"\"nickName\"\", firstName)\" FROM account",
                PSC.select("COALESCE(\"nickName\", firstName)").from("account").build().query());
        assertEquals("SELECT CONCAT(first_name, '\"') AS \"CONCAT(firstName, '\"\"')\" FROM account",
                PSC.select("CONCAT(firstName, '\"')").from("account").build().query());

        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        assertEquals("SELECT COALESCE(`nickName`, first_name) AS `COALESCE(``nickName``, firstName)` FROM account",
                mysql.select("COALESCE(`nickName`, firstName)").from("account").build().query());

        // No embedded quote: unchanged.
        assertEquals("SELECT COALESCE(nick_name, first_name) AS \"COALESCE(nickName, firstName)\" FROM account",
                PSC.select("COALESCE(nickName, firstName)").from("account").build().query());
    }

    // Regression: distinctOn() wrapped a trailing line comment inside the parentheses (commenting out ')'), and a
    // comment-only expression list rendered the invalid "DISTINCT ON ()".
    @Test
    public void testDistinctOnTerminatesTrailingLineCommentAndTreatsCommentOnlyAsBlank() {
        assertEquals("SELECT DISTINCT ON (a -- x\n) a FROM t", PSC.select("a").distinctOn("a -- x").from("t").build().query());
        assertEquals("SELECT DISTINCT ON (a -- x\n) a FROM t WHERE a = ?",
                PSC.select("a").from("t").distinctOn("a -- x").where(Filters.eq("a", 1)).build().query());

        assertEquals("SELECT DISTINCT a FROM t", PSC.select("a").distinctOn("-- x").from("t").build().query());
        assertEquals("SELECT DISTINCT a FROM t", PSC.select("a").distinctOn("/* x */").from("t").build().query());
        assertEquals("SELECT DISTINCT a FROM t", PSC.select("a").distinctOn(" ").from("t").build().query());

        // A comment that is followed by an expression is kept verbatim.
        assertEquals("SELECT DISTINCT ON (/* k */ a) a FROM t", PSC.select("a").distinctOn("/* k */ a").from("t").build().query());
        assertEquals("SELECT DISTINCT ON (a, b) a FROM t", PSC.select("a").distinctOn("a, b").from("t").build().query());
    }

    // Regression: append(String) emitted a trailing line comment raw, so the next structured clause (a DELETE's WHERE,
    // a placeholder-bearing WHERE) was silently commented out.
    @Test
    public void testAppendTerminatesTrailingLineComment() {
        assertEquals("DELETE FROM account /* job */ -- audit\n WHERE id = 1",
                SCSB.deleteFrom("account").append("/* job */ -- audit").where(Filters.eq("id", 1)).build().query());
        assertEquals("SELECT * FROM t -- note\n WHERE a = ?", PSC.select("*").from("t").append("-- note").where(Filters.eq("a", 1)).build().query());

        // No trailing line comment: unchanged.
        assertEquals("SELECT * FROM t FOR UPDATE", PSC.select("*").from("t").append("FOR UPDATE").build().query());
        assertEquals("SELECT * FROM t /* hint */ WHERE a = ?", PSC.select("*").from("t").append("/* hint */").where(Filters.eq("a", 1)).build().query());
    }

    // Regression: select("*").into(t).from(s) rendered the invalid target column list "INSERT INTO t (*) SELECT * FROM s".
    @Test
    public void testInsertSelectWildcardOmitsTargetColumnList() {
        assertEquals("INSERT INTO account_backup SELECT * FROM account", PSC.select("*").into("account_backup").from("account").build().query());
        assertEquals("INSERT INTO account_backup SELECT a.* FROM account a", PSC.select("a.*").into("account_backup").from("account a").build().query());
        assertEquals("INSERT INTO account_backup SELECT * FROM account", PSC.select("*").into("account_backup", Account.class).from("account").build().query());

        // A wildcard mixed with other select items cannot be mapped to target columns.
        assertThrows(IllegalArgumentException.class, () -> PSC.select("a.*", "b").into("account_backup"));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("b, a.*").into("account_backup"));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*", "*").into("account_backup"));

        // Non-wildcard projections keep their target column list; count(*) and quoted/parenthesized commas are not wildcards.
        assertEquals("INSERT INTO account_backup (id, name) SELECT id, name FROM account",
                PSC.select("id", "name").into("account_backup").from("account").build().query());
        assertEquals("INSERT INTO stats (count(*)) SELECT count(*) FROM account", PSC.select("count(*)").into("stats").from("account").build().query());
    }

    // Regression: a whitespace-only alias in selectFrom(cls, " ", true) skipped the @Table alias fallback that "" gets,
    // leaving the parent's columns unqualified (ambiguous) next to the sub-entity tables.
    @Test
    public void testSelectFromBlankAliasFallsBackToTableAliasWithSubEntities() {
        final String blankAlias = PSC.selectFrom(Account.class, " ", true).build().query();

        assertEquals(PSC.selectFrom(Account.class, "", true).build().query(), blankAlias);
        assertTrue(blankAlias.startsWith("SELECT acc.id AS \"id\""), blankAlias);
        assertTrue(blankAlias.endsWith(" FROM account acc, device"), blankAlias);
        assertEquals("acc", SqlBuilder.tableAlias(" ", Account.class));
        assertEquals("a", SqlBuilder.tableAlias("a", Account.class));
    }

    // Regression: ClickHouse/DuckDB leading join modifiers (ANY, ALL, GLOBAL, ARRAY, POSITIONAL, PASTE) were not
    // recognized as a JOIN start, so the modifier word became the primary table's alias ("ANY.first_name").
    @Test
    public void testLeadingJoinModifiersDoNotBecomePrimaryTableAlias() {
        for (final String join : new String[] { "ANY LEFT JOIN quotes q USING (id)", "GLOBAL ANY JOIN quotes q USING (id)", "ALL INNER JOIN quotes q USING (id)",
                "ARRAY JOIN arr AS x", "LEFT ARRAY JOIN arr AS x", "POSITIONAL JOIN quotes q", "PASTE JOIN quotes q", "ASOF JOIN quotes q USING (id)",
                "SEMI JOIN quotes q USING (id)" }) {
            assertEquals("SELECT a.first_name AS \"firstName\" FROM account a " + join,
                    PSC.select("firstName").from("account a " + join, Account.class).build().query(), join);
        }

        // Not followed by a JOIN keyword: the word is still a plain table alias.
        assertEquals("SELECT any.first_name AS \"firstName\" FROM account any", PSC.select("firstName").from("account any", Account.class).build().query());
        assertEquals("SELECT paste.first_name AS \"firstName\" FROM account paste, quotes q",
                PSC.select("firstName").from("account paste, quotes q", Account.class).build().query());
    }

    // Regression: two sub-entity properties of one class listed the same table reference twice in FROM ("FROM person p,
    // address ad, address ad"); merely listing it once made both properties silently read the SAME row, so it is now
    // rejected. A self-referencing sub-entity rendered raw cyclic paths ("parent.id" with no "parent" table) and an
    // extra Cartesian "node n" reference; it is no longer expanded, consistent with propToColumnInfoMap.
    @Test
    public void testSubEntityTableSharedByTwoPropertiesIsRejectedAndSelfReferenceIsNotExpanded() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(SubEntityPerson.class, true));
        assertTrue(e.getMessage().contains("'homeAddress' and 'workAddress'") && e.getMessage().contains("'address ad'"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> SqlBuilder.buildFromTableRefs(SubEntityPerson.class, null, null, NamingPolicy.SNAKE_CASE));

        // Multi-selection path.
        assertThrows(IllegalArgumentException.class,
                () -> PSC.selectFrom(List.of(Selection.builder(SubEntityPerson.class).tableAlias("p").includeSubEntityProperties(true).build())));
        assertThrows(IllegalArgumentException.class,
                () -> SqlBuilder.getFromClause(List.of(Selection.builder(SubEntityPerson.class).tableAlias("p").includedPropNames(List.of("id", "homeAddress.city",
                        "workAddress.city")).build()), NamingPolicy.SNAKE_CASE));

        // One of the two properties alone is fine, and a sub-entity table that is also selected directly is listed once.
        assertEquals("SELECT p.id AS \"id\", ad.city AS \"homeAddress.city\" FROM person p, address ad",
                PSC.selectFrom(List.of(Selection.builder(SubEntityPerson.class).tableAlias("p").includedPropNames(List.of("id", "homeAddress.city")).build()))
                        .build()
                        .query());
        assertEquals("person p, address ad",
                SqlBuilder.getFromClause(List.of(Selection.builder(SubEntityPerson.class).tableAlias("p").includedPropNames(List.of("homeAddress")).build(),
                        Selection.builder(SubEntityAddress.class).tableAlias("ad").build()), NamingPolicy.SNAKE_CASE));

        // Self-referencing sub-entity: its cyclic paths are not selected and add no table.
        assertEquals("SELECT n.id AS \"id\", n.name AS \"name\" FROM node n", PSC.selectFrom(SubEntityNode.class, true).build().query());
        assertEquals("SELECT x.id AS \"id\", x.name AS \"name\" FROM node x", PSC.selectFrom(SubEntityNode.class, "x", true).build().query());
        assertEquals(Arrays.asList("node n"), SqlBuilder.buildFromTableRefs(SubEntityNode.class, null, null, NamingPolicy.SNAKE_CASE));
        assertEquals("node x", SqlBuilder.getFromClause(List.of(Selection.builder(SubEntityNode.class).tableAlias("x").includeSubEntityProperties(true).build()),
                NamingPolicy.SNAKE_CASE));
        assertEquals(Arrays.asList("id", "name"), QueryUtil.selectPropNames(SubEntityNode.class, true, null));
    }

    // Regression: set(...) validation messages named internal parameters ("propOrColumnNames[0]", "props") instead of the caller's.
    @Test
    public void testSetValidationMessagesNameCallerParameter() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((String) null));
        assertTrue(e.getMessage().startsWith("expr "), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((String) null, 1));
        assertTrue(e.getMessage().startsWith("propOrColumnName "), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((Object) new LinkedHashMap<String, Object>()));
        assertTrue(e.getMessage().contains("entity"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((Object) " "));
        assertTrue(e.getMessage().startsWith("entity "), e.getMessage());

        // A non-UPDATE builder still fails with the operation check first.
        assertThrows(IllegalStateException.class, () -> PSC.select("a").set((String) null));
    }

    @Table(name = "address", alias = "ad")
    public static class SubEntityAddress {
        private long id;
        private String city;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    @Table(name = "person", alias = "p")
    public static class SubEntityPerson {
        private long id;
        private String name;
        private SubEntityAddress homeAddress;
        private SubEntityAddress workAddress;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public SubEntityAddress getHomeAddress() {
            return homeAddress;
        }

        public void setHomeAddress(final SubEntityAddress homeAddress) {
            this.homeAddress = homeAddress;
        }

        public SubEntityAddress getWorkAddress() {
            return workAddress;
        }

        public void setWorkAddress(final SubEntityAddress workAddress) {
            this.workAddress = workAddress;
        }
    }

    @Table(name = "node", alias = "n")
    public static class SubEntityNode {
        private long id;
        private String name;
        private SubEntityNode parent;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public SubEntityNode getParent() {
            return parent;
        }

        public void setParent(final SubEntityNode parent) {
            this.parent = parent;
        }
    }

    // Regression: MySQL/MariaDB/SQLite end a line comment only at '\n', so a trailing comment closed by a lone '\r' swallowed the next clause.
    @Test
    public void testTrailingLineCommentEndedByLoneCarriageReturnIsTerminatedWithLineFeed() {
        final Dsl mysql = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        final Dsl sqlite = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("SQLite")).build());

        assertEquals("SELECT * FROM users u JOIN orders o ON o.uid = u.id -- note\r\n WHERE u.id = 1",
                mysql.select("*").from("users u").join("orders o ON o.uid = u.id -- note\r").where("u.id = 1").build().query());
        assertEquals("SELECT * FROM users u JOIN orders o ON o.uid = u.id # note\r\n WHERE u.id = 1",
                mysql.select("*").from("users u").join("orders o ON o.uid = u.id # note\r").where("u.id = 1").build().query());
        assertEquals("SELECT * FROM users u CROSS JOIN orders o -- note\r\n WHERE u.id = 1",
                sqlite.select("*").from("users u").crossJoin("orders o -- note\r").where("u.id = 1").build().query());
        assertEquals("SELECT * FROM users u CROSS JOIN orders o --\r\n WHERE u.id = 1",
                SCSB.select("*").from("users u").crossJoin("orders o --\r").where("u.id = 1").build().query());
        assertEquals("SELECT id FROM users UNION SELECT id FROM admins -- note\r\n ORDER BY id",
                SCSB.select("id").from("users").union("SELECT id FROM admins -- note\r").orderBy("id").build().query());
        assertEquals("SELECT DISTINCT -- x\r\n id FROM users u", mysql.select("id").selectModifier("DISTINCT -- x\r").from("users u").build().query());
        assertTrue(AbstractQueryBuilder.endsInsideLineComment("a -- x\r", false));
        assertTrue(AbstractQueryBuilder.endsInsideLineComment("a -- x\r b", false));

        // A line feed already ends the comment in every dialect: nothing is added.
        assertEquals("SELECT * FROM users u JOIN orders o ON o.uid = u.id -- note\r\n WHERE u.id = 1",
                mysql.select("*").from("users u").join("orders o ON o.uid = u.id -- note\r\n").where("u.id = 1").build().query());
        assertFalse(AbstractQueryBuilder.endsInsideLineComment("a -- x\r\n b", false));
    }

    // Regression: the second '#' of PostgreSQL's "##" operator was read as a hash comment by the alias and JOIN-connector scanners.
    @Test
    public void testHashPairOperatorIsNotReadAsHashCommentByAliasAndJoinScanners() {
        final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());

        assertEquals("SELECT point_a ## line_b AS closest FROM t", pg.select("pointA ## lineB AS closest").from("t").build().query());
        assertEquals("SELECT point_a ## line_b AS closest FROM t", PSC.select("pointA ## lineB AS closest").from("t").build().query());
        assertEquals("SELECT a.first_name ## a.last_name AS x, a.id AS \"id\" FROM account a",
                pg.select("firstName ## lastName AS x", "id").from(Account.class, "a").build().query());
        // The ON after the operator is visible, so WHERE may follow. (The conservative trailing-comment check still adds a harmless line feed.)
        assertEquals("SELECT * FROM t JOIN LATERAL (SELECT t.p ## t.l AS c) x ON true\n WHERE t.id = 1",
                pg.select("*").from("t").join("LATERAL (SELECT t.p ## t.l AS c) x ON true").where("t.id = 1").build().query());

        // The dialect-agnostic default still terminates a trailing MySQL-style "## note" comment, and an ambiguous FROM tail infers no alias.
        assertEquals("SELECT * FROM t JOIN u ON u.id = t.id ## note\n WHERE t.id = 1",
                SCSB.select("*").from("t").join("u ON u.id = t.id ## note").where("t.id = 1").build().query());
        assertEquals("SELECT first_name AS \"firstName\" FROM account a ## main table\n",
                PSC.select("firstName").from("account a ## main table", Account.class).build().query());
        // Under MySQL the "##" opens a comment, so the alias before it is found.
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        assertEquals("SELECT a.first_name AS `firstName` FROM account a ## main table\n",
                mysql.select("firstName").from("account a ## main table", Account.class).build().query());
    }

    // Regression: the alias scanner always applied MySQL backslash escapes, so a standard 'C:\' literal hid the real AS alias.
    @Test
    public void testSelectAliasScannerHonorsStandardLiteralEndingInBackslash() {
        final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());

        assertEquals("SELECT 'C:\\' || ' AS x' AS y FROM t", pg.select("'C:\\' || ' AS x' AS y").from("t").build().query());
        // The default dialect's backslash reading leaves a literal unterminated, so the standard reading decides ...
        assertEquals("SELECT 'C:\\' || ' AS x' AS y FROM t", PSC.select("'C:\\' || ' AS x' AS y").from("t").build().query());
        // ... and, conversely, a MySQL-style escaped quote keeps its alias.
        assertEquals("SELECT CONCAT(first_name, 'it\\'s') AS fn FROM t", PSC.select("CONCAT(firstName, 'it\\'s') AS fn").from("t").build().query());
        assertEquals("SELECT CONCAT(first_name, 'it\\'s') AS fn FROM t", mysql.select("CONCAT(firstName, 'it\\'s') AS fn").from("t").build().query());
        // An E'...' string honors backslash escapes in every dialect.
        assertEquals("SELECT E'C:\\' AS x' AS y FROM t", pg.select("E'C:\\' AS x' AS y").from("t").build().query());
    }

    // Regression: Unicode whitespace before an explicit AS stayed in the expression (String.trim() keeps U+3000), so the property mapping was missed.
    @Test
    public void testUnicodeWhitespaceBeforeExplicitSelectAliasKeepsPropertyMapping() {
        assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName\u3000AS\u3000fn").from(Account.class).build().query());
        assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName\u2003AS fn").from(Account.class).build().query());
        assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName AS fn").from(Account.class).build().query());
    }

    // Regression: the JOIN-connector, FROM-separator and FROM-alias scanners ignored the dialect's string, comment and bracket rules.
    @Test
    public void testJoinAndFromScannersFollowDialectLexicalRules() {
        final Dsl pg = Dsl.forDialect(SqlDialect.builder()
                .namingPolicy(NamingPolicy.SNAKE_CASE)
                .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
                .productInfo(SqlDialect.ProductInfo.of("PostgreSQL"))
                .build());
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        final Dsl sqlServer = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());

        assertEquals("SELECT u.id FROM users u LEFT JOIN (SELECT id FROM t WHERE name LIKE 'a\\_%' ESCAPE '\\') s ON s.id = u.id WHERE u.id = ?",
                pg.select("u.id")
                        .from("users u")
                        .leftJoin("(SELECT id FROM t WHERE name LIKE 'a\\_%' ESCAPE '\\') s ON s.id = u.id")
                        .where(Filters.eq("u.id", 1))
                        .build()
                        .query());
        assertEquals("SELECT u.id FROM users u LEFT JOIN (SELECT id FROM t WHERE name LIKE 'a\\_%' ESCAPE '\\') s ON s.id = u.id WHERE u.id = ?",
                PSC.select("u.id")
                        .from("users u")
                        .leftJoin("(SELECT id FROM t WHERE name LIKE 'a\\_%' ESCAPE '\\') s ON s.id = u.id")
                        .where(Filters.eq("u.id", 1))
                        .build()
                        .query());
        assertEquals("SELECT u.id FROM users u JOIN (SELECT * FROM files WHERE dir = 'C:\\') f ON f.uid = u.id WHERE u.id = ?",
                sqlServer.select("u.id").from("users u").join("(SELECT * FROM files WHERE dir = 'C:\\') f ON f.uid = u.id").where(Filters.eq("u.id", 1)).build().query());
        // MySQL "a--1" is arithmetic, not a comment.
        assertEquals("SELECT u.id FROM users u JOIN (SELECT id, a--1 AS x FROM t) s ON s.id = u.id WHERE u.id = ?",
                mysql.select("u.id").from("users u").join("(SELECT id, a--1 AS x FROM t) s ON s.id = u.id").where(Filters.eq("u.id", 1)).build().query());
        // PostgreSQL '[' opens an array subscript, not a bracket-quoted identifier.
        assertEquals("SELECT u.id FROM users u JOIN LATERAL unnest(ARRAY['a]', 'b']) AS x(v) ON true WHERE u.id = ?",
                pg.select("u.id").from("users u").join("LATERAL unnest(ARRAY['a]', 'b']) AS x(v) ON true").where(Filters.eq("u.id", 1)).build().query());
        // A CROSS JOIN must not carry an ON that is visible under the standard reading.
        assertThrows(IllegalArgumentException.class, () -> pg.select("u.id").from("users u").crossJoin("(SELECT 'C:\\') s ON true"));

        // The primary FROM alias (and the separator before the next table) is found past a standard backslash-terminated literal.
        assertEquals("SELECT x.first_name AS \"firstName\" FROM (SELECT * FROM account WHERE p = 'C:\\') x",
                pg.select("firstName").from("(SELECT * FROM account WHERE p = 'C:\\') x", Account.class).build().query());
        assertEquals("SELECT x.first_name AS \"firstName\" FROM (SELECT * FROM account WHERE p = 'C:\\') x, other o",
                pg.select("firstName").from("(SELECT * FROM account WHERE p = 'C:\\') x, other o", Account.class).build().query());
        assertEquals("SELECT x.first_name AS \"firstName\" FROM (SELECT * FROM account WHERE p = 'C:\\') x",
                PSC.select("firstName").from("(SELECT * FROM account WHERE p = 'C:\\') x", Account.class).build().query());
    }

    // Regression: the placeholder-rename scanner read every non-MySQL '#word' as a SQL Server temp table, so a quote in a hash comment hid a colliding child placeholder.
    @Test
    public void testChildPlaceholderRenameSkipsHashCommentsOutsideSqlServer() {
        AbstractQueryBuilder.SP sp = NSC.select("id")
                .from("t")
                .where(Filters.eq("id", 1))
                .union(NSC.select("id").from("t2 #TODO: don't scan\n").where(Filters.eq("id", 2)))
                .build();
        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM t2 #TODO: don't scan\n WHERE id = :id_2", sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());
        assertEquals(Arrays.asList("id", "id_2"), new ArrayList<>(ParsedSql.parse(sp.query()).namedParameters()));

        sp = MSC.select("id").from("t").where(Filters.eq("id", 1)).union(MSC.select("id").from("t2 #TODO: don't scan\n").where(Filters.eq("id", 2))).build();
        assertEquals("SELECT id FROM t WHERE id = #{id} UNION SELECT id FROM t2 #TODO: don't scan\n WHERE id = #{id_2}", sp.query());

        final Dsl pg = Dsl.forDialect(NSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        sp = pg.select("id").from("t").where(Filters.eq("id", 1)).union(pg.select("id").from("t2 #it's\n").where(Filters.eq("id", 2))).build();
        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM t2 #it's\n WHERE id = :id_2", sp.query());

        // A temporary-table identifier in FROM context is still data.
        sp = NSC.select("id").from("t").where(Filters.eq("id", 1)).union(NSC.select("id").from("#tmp").where(Filters.eq("id", 2))).build();
        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM #tmp WHERE id = :id_2", sp.query());
    }

    // Regression: MySQL "..." string literals honor backslash escapes, but the rename scanner read them with quote doubling only.
    @Test
    public void testChildPlaceholderRenameHonorsMySqlDoubleQuotedStringEscapes() {
        final Dsl mysql = Dsl.forDialect(NSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());

        AbstractQueryBuilder.SP sp = mysql.select("id")
                .from("t")
                .where(Filters.eq("id", 1))
                .union(mysql.select("id").from("t2").where(Filters.and(Filters.expr("n <> \"it\\\"s :id\""), Filters.eq("id", 2))))
                .build();
        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM t2 WHERE (n <> \"it\\\"s :id\") AND (id = :id_2)", sp.query());
        assertEquals(Arrays.asList(1, 2), sp.parameters());

        sp = mysql.select("id")
                .from("t")
                .where(Filters.eq("id", 1))
                .union(mysql.select("id").from("t2").where(Filters.and(Filters.expr("n <> \"a\\\"\""), Filters.eq("id", 2))))
                .build();
        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM t2 WHERE (n <> \"a\\\"\") AND (id = :id_2)", sp.query());

        // The dialect-agnostic default cannot tell which reading applies, so it fails closed.
        assertThrows(IllegalArgumentException.class, () -> NSC.select("id")
                .from("t")
                .where(Filters.eq("id", 1))
                .union(NSC.select("id").from("t2").where(Filters.and(Filters.expr("n <> \"a\\\"\""), Filters.eq("id", 2)))));
    }

    // Regression: the comment guard always read \' as an escaped quote, so "'C:\' -- note" passed it on standard-string dialects and swallowed the WHERE.
    @Test
    public void testCommentGuardChecksStandardStringReadingOutsideMySql() {
        final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        final Dsl oracle = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Oracle")).build());
        final Dsl sqlServer = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());

        assertThrows(IllegalArgumentException.class, () -> pg.update("account").set("dir = 'C:\\' -- normalize dir").where("status = 0"));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set("dir = 'C:\\' -- normalize dir").where("status = 0"));
        assertThrows(IllegalArgumentException.class, () -> oracle.select("'a\\' -- x'").from("account"));
        assertThrows(IllegalArgumentException.class, () -> pg.select("id").from("account").orderBy("'a\\' -- x'"));
        assertThrows(IllegalArgumentException.class, () -> sqlServer.select("'C:\\' + #t.firstName -- c").from("#t"));
        // The SQL Server temporary-table scan no longer hides "#t" behind the backslash reading, so the guard applies to raw WHERE text.
        assertThrows(IllegalArgumentException.class, () -> sqlServer.select("id").from("#t").where("'C:\\' + #t.name = 'x' -- c"));

        // E'...' strings and MySQL strings keep their backslash escapes.
        assertEquals("UPDATE account SET dir = E'C:\\' -- quoted' WHERE status = 0",
                pg.update("account").set("dir = E'C:\\' -- quoted'").where("status = 0").build().query());
        assertEquals("UPDATE account SET dir = 'it\\'s -- fine' WHERE status = 0",
                mysql.update("account").set("dir = 'it\\'s -- fine'").where("status = 0").build().query());
    }

    // Regression: with an explicit MySQL dialect the comment guard still exempted ##, #>, #- and ?#, which MySQL reads as comments.
    @Test
    public void testCommentGuardTreatsEveryHashAsCommentUnderMySql() {
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());

        assertThrows(IllegalArgumentException.class, () -> mysql.update("account").set("status = 1 ## reset").where("status = 0"));
        assertThrows(IllegalArgumentException.class, () -> mysql.select("a #> b").from("t"));
        assertThrows(IllegalArgumentException.class, () -> mysql.select("a #-b").from("t"));
        assertThrows(IllegalArgumentException.class, () -> mysql.select("a ?# b").from("t"));
        assertThrows(IllegalArgumentException.class, () -> mysql.select("a#>>b").from("t"));

        // A MyBatis marker is still a data token, and PostgreSQL keeps its hash operators.
        assertEquals("UPDATE account SET status = #{status} WHERE status = 0", mysql.update("account").set("status = #{status}").where("status = 0").build().query());
        final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        assertEquals("SELECT a ## b FROM t", pg.select("a ## b").from("t").build().query());
    }

    // Regression: the builder converted a glued subscript / array constructor / cast token as one identifier
    // (ARRAY[1, 2, 3] -> array[1,_2,_3], myTags[:tagIndex] -> my_tags[:tag_index]), skipped the column in
    // unitPrice::numeric(10,2), rewrote ::"OrderStatus", and left the column in "T".firstName unconverted.
    @Test
    public void testRawExpressionGluedTokensRenderIdenticallyThroughBothPaths() {
        for (final String expr : new String[] { "id = ANY(ARRAY[1, 2, 3])", "tags && ARRAY[:tagA, :tagB]", "myTags[:tagIndex] = 'x'",
                "myTags[#{tagIndex}] = 'x'", "log_${yearMonth}.createdAt > 0", "scores[idx + 1] > 5", "scores[CURRENT_DATE - startDay] > 5",
                "payloadData['camelKey'] = 1", "ARRAY[firstName, lastName] && tags", "unitPrice::numeric(10,2) > 5", "orderStatus::\"OrderStatus\" = 'NEW'",
                "\"T\".firstName = 1", "`t`.firstName = 1", "[t].firstName = 1", "\"s\".myFunc(firstName) > 0", "arr[idx # 2] = 1", "fn_${v}(aB) > 0",
                "myTags[firstName:lastName] = 1", "matrix[rowIdx][colIdx] > 0", "arr[firstName || '#'] = 1", "arr[1].fieldName = 1",
                "firstName COLLATE utf8mb4_bin = 'x'", "payload @> '{\"a\":\"b\\\"c\"}' AND createdAt > 1", "msg = 'hello '\n'world'" }) {
            final String viaCondition = Filters.expr(expr).toSql(NamingPolicy.SNAKE_CASE);
            final String builtSql = PSC.select("id").from("t").where(Filters.expr(expr)).build().query();

            assertEquals(viaCondition, builtSql.substring(builtSql.indexOf("WHERE ") + 6), "rendering paths diverged for: " + expr);
        }

        assertEquals("SELECT id FROM t WHERE id = ANY(ARRAY[1, 2, 3])", PSC.select("id").from("t").where(Filters.expr("id = ANY(ARRAY[1, 2, 3])")).build().query());
        assertEquals("SELECT id FROM t WHERE my_tags[:tagIndex] = 'x'", PSC.select("id").from("t").where(Filters.expr("myTags[:tagIndex] = 'x'")).build().query());
        assertEquals("SELECT id FROM t WHERE tags && ARRAY[:tagA, :tagB]", NSC.select("id").from("t").where(Filters.expr("tags && ARRAY[:tagA, :tagB]")).build().query());
        assertEquals("SELECT id FROM t WHERE unit_price::numeric(10,2) > 5",
                PSC.select("id").from("t").where(Filters.expr("unitPrice::numeric(10,2) > 5")).build().query());
        assertEquals("SELECT id FROM t WHERE order_status::\"OrderStatus\" = 'NEW'",
                PSC.select("id").from("t").where(Filters.expr("orderStatus::\"OrderStatus\" = 'NEW'")).build().query());
        assertEquals("SELECT id FROM t WHERE \"T\".first_name = 1", PSC.select("id").from("t").where(Filters.expr("\"T\".firstName = 1")).build().query());

        // The leading name still resolves through the entity mapping and table alias; the part after a delimited
        // qualifier takes the naming policy only, since the builder's own alias would not apply to it.
        assertEquals("SELECT acc.id AS \"id\" FROM account acc WHERE \"T\".first_name = 1 AND acc.first_name[1] = 2",
                PSC.select("id").from(Account.class).where(Filters.expr("\"T\".firstName = 1 AND firstName[1] = 2")).build().query());
    }

    // Regression: the builders converted the unquoted part of a schema-qualified type or collation after a delimited
    // schema like a column (CAMEL_CASE: status::"types".order_status -> status::"types".orderStatus).
    @Test
    public void testQualifiedTypeOrCollationAfterDelimitedSchemaIsKeptByBuilders() {
        assertEquals("SELECT id FROM t WHERE status::\"types\".order_status = 'NEW'",
                PLC.select("id").from("t").where("status::\"types\".order_status = 'NEW'").build().query());
        assertEquals("SELECT id FROM t WHERE CAST(status AS \"types\".order_status) = 'NEW'",
                PLC.select("id").from("t").where(Filters.expr("CAST(status AS \"types\".order_status) = 'NEW'")).build().query());
        assertEquals("SELECT id FROM t WHERE name collate \"public\".my_collation = 'x'",
                PLC.select("id").from("t").where("name collate \"public\".my_collation = 'x'").build().query());
        assertEquals("SELECT acc.id AS \"id\" FROM account acc WHERE acc.status::\"types\".orderStatus = 'NEW'",
                PSC.select("id").from(Account.class).where(Filters.expr("status::\"types\".orderStatus = 'NEW'")).build().query());
    }

    // Regression: the builders converted a type that is not glued to its cast (CAST(x AS type), a spaced "::", whitespace
    // after a qualifying dot) like a column.
    @Test
    public void testTypeInCastOrAfterSpacedCastIsKeptByBuilders() {
        assertEquals("SELECT id FROM t WHERE CAST(status AS order_status) = 'NEW' AND status :: order_status = 'NEW'",
                PLC.select("id").from("t").where("CAST(status AS order_status) = 'NEW' AND status :: order_status = 'NEW'").build().query());
        assertEquals("SELECT id FROM t WHERE status::\"types\". order_status = 'NEW'",
                PLC.select("id").from("t").where("status::\"types\". order_status = 'NEW'").build().query());
        assertEquals("SELECT id FROM t WHERE name collate \"public\" . my_collation = 'x'",
                PLC.select("id").from("t").where("name collate \"public\" . my_collation = 'x'").build().query());
        assertEquals("SELECT acc.id AS \"id\" FROM account acc WHERE CAST(acc.first_name AS myType) = acc.last_name",
                PSC.select("id").from(Account.class).where(Filters.expr("CAST(firstName AS myType) = lastName")).build().query());
        assertEquals("SELECT CAST(first_name AS myType) AS fn FROM t", PSC.select("CAST(firstName AS myType) AS fn").from("t").build().query());

        // A chained cast keeps both types; a clause after a CAST type is rendered as usual.
        assertEquals("SELECT id FROM t WHERE status :: int:: order_status = 1",
                PLC.select("id").from("t").where("status :: int:: order_status = 1").build().query());
        assertEquals("SELECT id FROM t WHERE CAST(event_time AS STRING format 'YYYY' at time zone time_zone) = 'x'",
                PSC.select("id").from("t").where("CAST(eventTime AS STRING format 'YYYY' at time zone timeZone) = 'x'").build().query());
        // A schema named like a clause keyword is part of the type.
        assertEquals("SELECT id FROM t WHERE CAST(id AS format . order_status) = '1' AND CAST(id AS at . order_status) = '1'",
                PLC.select("id").from("t").where("CAST(id AS format . order_status) = '1' AND CAST(id AS at /* c */ . order_status) = '1'").build().query());
    }

    // Regression: the tokenizer reads \' as an escaped quote, so after a standard literal such as 'C:\' every quote boundary
    // was off by one: a later string was converted as an identifier, and "-- n/a" inside a later string was stripped
    // as a comment, truncating the WHERE clause.
    @Test
    public void testEscapeDependentQuoteInRawExpressionIsEmittedVerbatim() {
        final String noteExpr = SqlExpression.and(SqlExpression.eq("path", "C:\\"), SqlExpression.eq("note", "-- n/a"));

        // The verbatim text ends inside a line comment under the backslash reading, so it is terminated before ORDER BY.
        assertEquals("SELECT id FROM t WHERE (path = 'C:\\') AND (note = '-- n/a')\n ORDER BY id",
                PSC.select("id").from("t").where(Filters.expr(noteExpr)).orderBy("id").build().query());

        final Dsl postgres = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        assertEquals("SELECT id FROM t WHERE (path = 'C:\\') AND (note = '-- n/a')\n ORDER BY id",
                postgres.select("id").from("t").where(Filters.expr(noteExpr)).orderBy("id").build().query());

        final String likeExpr = SqlExpression.and(SqlExpression.like("fileName", "%\\"), SqlExpression.eq("ownerName", "John Smith"));
        final String built = PSC.select("id").from("t").where(Filters.expr(likeExpr)).build().query();
        assertEquals("SELECT id FROM t WHERE (file_name LIKE '%\\') AND (ownerName = 'John Smith')", built);
        assertEquals(Filters.expr(likeExpr).toSql(NamingPolicy.SNAKE_CASE), built.substring(built.indexOf("WHERE ") + 6));

        // MySQL always reads \' as an escape, which is how the tokenizer reads it, so conversion continues there.
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        assertEquals("SELECT id FROM t WHERE a = 'It\\'s' AND first_name = 'x'",
                mysql.select("id").from("t").where(Filters.expr("a = 'It\\'s' AND firstName = 'x'")).build().query());
        assertEquals("SELECT id FROM t WHERE a = 'It\\'s' AND firstName = 'x'",
                PSC.select("id").from("t").where(Filters.expr("a = 'It\\'s' AND firstName = 'x'")).build().query());
    }

    // Regression: MySQL starts a "--" comment only when whitespace or a control character follows, so "a--1" is
    // a - (-1) there; the shared tokenizer dropped "--1 > 0" as a comment and rendered "WHERE a" under a MySQL builder.
    @Test
    public void testMySqlRawExpressionKeepsDashPairWithoutWhitespaceAsMinusOperators() {
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());

        assertEquals("SELECT id FROM t WHERE a- -1 > 0 ORDER BY id", mysql.select("id").from("t").where(Filters.expr("a--1 > 0")).orderBy("id").build().query());
        assertEquals("SELECT id FROM t WHERE a- - -1 > 0", mysql.select("id").from("t").where(Filters.expr("a---1 > 0")).build().query());
        // Quoted text is untouched, including after a backslash-escaped quote, and real MySQL comments are still dropped.
        assertEquals("SELECT id FROM t WHERE note = 'x--y' AND b = 'it\\'s--x' AND a- -1 > 0",
                mysql.select("id").from("t").where(Filters.expr("note = 'x--y' AND b = 'it\\'s--x' AND a--1 > 0")).build().query());
        assertEquals("SELECT id FROM t WHERE a = 1  ORDER BY id", mysql.select("id").from("t").where(Filters.expr("a = 1 -- a--1")).orderBy("id").build().query());
        assertEquals("SELECT id FROM t WHERE a = 1  ORDER BY id", mysql.select("id").from("t").where(Filters.expr("a = 1 # a--1")).orderBy("id").build().query());

        // In standard SQL (and without product info) "--" always starts a comment.
        assertEquals("SELECT id FROM t WHERE a", PSC.select("id").from("t").where(Filters.expr("a--1 > 0")).build().query());
    }

    // Regression: the builder's raw-expression rendering went verbatim at any backslash before any quote, so a JSON literal
    // with \" left every later column unconverted; it also renamed a function with a glued marker, copied the column after
    // log_${month}. and the bounds of slices and chained subscripts unconverted.
    @Test
    public void testRawExpressionGluedTokensAndBackslashesRenderThroughBuilders() {
        final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());

        for (final Dsl dsl : new Dsl[] { PSC, pg }) {
            assertEquals("SELECT id FROM t WHERE payload @> '{\"a\":\"b\\\"c\"}' AND created_at > 1",
                    dsl.select("id").from("t").where("payload @> '{\"a\":\"b\\\"c\"}' AND createdAt > 1").build().query());
            assertEquals("SELECT id FROM t WHERE \"it\\'s\" = 1 AND created_at > 1",
                    dsl.select("id").from("t").where(Filters.expr("\"it\\'s\" = 1 AND createdAt > 1")).build().query());
            // A backslash before the region's own closing quote still emits the rest as written.
            assertEquals("SELECT id FROM t WHERE \"col\\\"x\" = 1 AND aB = 1", dsl.select("id").from("t").where("\"col\\\"x\" = 1 AND aB = 1").build().query());
        }

        final java.util.function.BiFunction<Dsl, String, String> where = (dsl, expr) -> {
            final String sql = dsl.select("id").from("t").where(expr).build().query();
            return sql.substring(sql.indexOf(" WHERE ") + 7);
        };

        assertEquals("x = E'It\\'s' AND first_name = 'x'", where.apply(pg, "x = E'It\\'s' AND firstName = 'x'"));
        // SCREAMING_SNAKE_CASE and CAMEL_CASE builders also emit the text from 'C:\' on as written.
        assertEquals("A_B = 'C:\\' AND firstName = 1", where.apply(PAC, "aB = 'C:\\' AND firstName = 1"));
        assertEquals("aB = 'C:\\' AND first_name = 1", where.apply(PLC, "a_b = 'C:\\' AND first_name = 1"));

        assertEquals("my_func_${ver}(x) > 0", where.apply(PLC, "my_func_${ver}(x) > 0"));
        assertEquals("getValue_${v}(A_B) > 0", where.apply(PAC, "getValue_${v}(aB) > 0"));
        assertEquals("LOG_${yearMonth}.CREATED_AT = 1", where.apply(PAC, "log_${yearMonth}.createdAt = 1"));
        assertEquals("log_${yearMonth}.created_at = 1", where.apply(PSC, "log_${yearMonth}.createdAt = 1"));
        assertEquals("logTable_${month}.createTime > 0", where.apply(PLC, "log_table_${month}.create_time > 0"));
        assertEquals("my_tags[first_name:last_name] = 1", where.apply(PSC, "myTags[firstName:lastName] = 1"));
        assertEquals("MATRIX[ROW_IDX][COL_IDX] > 0", where.apply(PAC, "matrix[rowIdx][colIdx] > 0"));
        assertEquals("arr[1].field_name = 1", where.apply(PSC, "arr[1].fieldName = 1"));
        // The collation name is kept exactly under every case-changing policy; the COLLATE keyword follows the policy.
        assertEquals("firstName collate utf8mb4_bin = 'x'", where.apply(PLC, "first_name COLLATE utf8mb4_bin = 'x'"));
        assertEquals("LAST_NAME COLLATE Latin1_General_CS_AS = 'x'", where.apply(PAC, "lastName COLLATE Latin1_General_CS_AS = 'x'"));

        // The leading name of a glued subscript inside a function still resolves through the entity mapping and alias.
        assertEquals("SELECT acc.id AS \"id\" FROM account acc WHERE coalesce(acc.first_name[1], 0) = 1",
                PSC.select("id").from(Account.class).where("coalesce(firstName[1], 0) = 1").build().query());
    }

    // Regression: the builder collapsed the line break between two adjacent string literals ('hello '\n'world') into a
    // space, which PostgreSQL rejects; ParsedSql already kept it.
    @Test
    public void testRawExpressionKeepsLineBreakBetweenAdjacentStringLiterals() {
        assertEquals("SELECT id FROM t WHERE msg = 'hello '\n'world'", PSC.select("id").from("t").where(Filters.expr("msg = 'hello '\n'world'")).build().query());
        assertEquals("SELECT 'hello '\n'world' AS m FROM t", PSC.select("'hello '\n'world' AS m").from("t").build().query());
        assertEquals("SELECT id FROM t WHERE msg = 'a'\n'b'", PSC.select("id").from("t").where(Filters.eq("msg", SqlExpression.of("'a' -- c\n'b'"))).build().query());
        assertEquals("SELECT id FROM t WHERE msg = 'a' 'b' AND first_name = 'x'",
                PSC.select("id").from("t").where("msg = 'a' 'b'\nAND firstName = 'x'").build().query());
    }

    // Regression: under a class alias the whole suffix after the inline AS was quoted as ONE label, so
    // "firstName AS fn, lastName AS ln" silently became one column labeled "acc.fn, lastName AS ln" (lastName vanished).
    @Test
    public void testClassAliasedSelectItemWithMultiTokenAliasIsRejected() {
        for (final String item : new String[] { "firstName AS fn, lastName AS ln", "firstName AS f n" }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> PSC.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").classAlias("acc").includedPropNames(List.of(item)).build())));
            assertTrue(e.getMessage().contains(item), e.getMessage());
        }

        // A one-word alias under a class alias, and a comma-separated suffix without one, render as before.
        assertEquals("SELECT a.first_name AS \"acc.fn\" FROM account a",
                PSC.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").classAlias("acc").includedPropNames(List.of("firstName AS fn")).build()))
                        .build()
                        .query());
        assertEquals("SELECT a.first_name AS fn, lastName AS ln FROM account a",
                PSC.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").includedPropNames(List.of("firstName AS fn, lastName AS ln")).build()))
                        .build()
                        .query());
    }

    // Regression: the inline select alias was trim()med, which keeps U+3000/U+2003, so the emitted (unquoted) alias -- and the
    // result-column label -- became "fn\u3000" instead of "fn".
    @Test
    public void testInlineSelectAliasDropsTrailingUnicodeWhitespace() {
        assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName AS fn\u3000").from(Account.class).build().query());
        assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName\u2003AS\u2003fn\u2003").from(Account.class).build().query());

        // The multi-column / table-alias path and INSERT ... SELECT (whose target column takes the expression only).
        assertEquals("SELECT a.first_name AS fn, a.last_name AS \"lastName\" FROM account a",
                PSC.select("firstName\u3000AS\u3000fn", "lastName").from(Account.class, "a").build().query());
        assertEquals("INSERT INTO account_backup (first_name) SELECT acc.first_name AS fn FROM account acc",
                PSC.select("firstName\u3000AS\u3000fn\u3000").into("account_backup").from(Account.class).build().query());

        // No-break spaces (U+00A0, U+202F), which String.strip() keeps, are padding too.
        assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName AS fn\u00a0").from(Account.class).build().query());
        assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName AS fn\u202f").from(Account.class).build().query());
        assertEquals("SELECT a.first_name AS \"acc.fn\" FROM account a",
                PSC.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").classAlias("acc").includedPropNames(List.of("firstName AS fn\u00a0")).build()))
                        .build()
                        .query());
    }

    // Covers the sort-direction rewrite beyond the plain entity context: a table alias, mixed varargs lists, the direct
    // groupBy(String, SortDirection), and that a rejected single item leaves the ORDER BY / GROUP BY slot unconsumed.
    @Test
    public void testSortDirectionSubEntityExpansionWithTableAliasMixedListsAndRetry() {
        final String aliased = PSC.select("id").from(Account.class, "a").orderBy("devices", SortDirection.DESC).build().query();
        assertTrue(aliased.startsWith("SELECT a.id AS \"id\" FROM account a ORDER BY device.id DESC, device.account_id DESC, "), aliased);
        assertTrue(aliased.endsWith(", device.create_time DESC"), aliased);

        final String mixedOrder = PSC.select("id").from(Account.class).orderByDesc("devices", "id").build().query();
        assertTrue(mixedOrder.startsWith("SELECT acc.id AS \"id\" FROM account acc ORDER BY device.id DESC, "), mixedOrder);
        assertTrue(mixedOrder.endsWith(", device.create_time DESC, acc.id DESC"), mixedOrder);

        final String mixedGroup = PSC.select("id").from(Account.class).groupByDesc("devices", "firstName").build().query();
        assertTrue(mixedGroup.endsWith(", device.create_time DESC, acc.first_name DESC"), mixedGroup);

        final String groupAsc = PSC.select("id").from(Account.class).groupBy("devices", SortDirection.ASC).build().query();
        assertTrue(groupAsc.startsWith("SELECT acc.id AS \"id\" FROM account acc GROUP BY device.id ASC, device.account_id ASC, "), groupAsc);
        assertTrue(groupAsc.endsWith(", device.create_time ASC"), groupAsc);

        final SqlBuilder order = PSC.select("id").from(Account.class);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> order.orderBy("id -- x", SortDirection.DESC));
        assertEquals("SQL comment token is not allowed in column expression: id -- x", e.getMessage());
        assertTrue(order.orderBy("devices", SortDirection.DESC).build().query().startsWith("SELECT acc.id AS \"id\" FROM account acc ORDER BY device.id DESC, "));

        final SqlBuilder group = PSC.select("id").from(Account.class);
        e = assertThrows(IllegalArgumentException.class, () -> group.groupBy("id -- x", SortDirection.DESC));
        assertEquals("SQL comment token is not allowed in column expression: id -- x", e.getMessage());
        assertEquals("SELECT acc.id AS \"id\" FROM account acc GROUP BY acc.first_name DESC", group.groupBy("firstName", SortDirection.DESC).build().query());
    }

    // Regression: distinctOn("/* x") -- an unclosed block comment, most likely a typo -- silently became a plain DISTINCT.
    @Test
    public void testDistinctOnRejectsUnterminatedBlockComment() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.select("a").distinctOn("/* x"));
        assertTrue(e.getMessage().contains("unterminated block comment"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("a").distinctOn("a /* x"));
        assertThrows(IllegalArgumentException.class, () -> Criteria.builder().distinctOn("/* x"));

        // Several terminated comments with nothing else are still a plain DISTINCT; a quoted "/*" is data.
        assertEquals("SELECT DISTINCT a FROM t", PSC.select("a").distinctOn("-- a\n-- b").from("t").build().query());
        assertEquals("SELECT DISTINCT a FROM t", PSC.select("a").distinctOn("/* a */ /* b */").from("t").build().query());
        assertEquals("SELECT DISTINCT ON ('/*') a FROM t", PSC.select("a").distinctOn("'/*'").from("t").build().query());

        // A trailing comment is terminated before ')' under each dialect's reading; quoted dashes are not a comment.
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        assertEquals("SELECT DISTINCT ON (a # x\n) a FROM t", PSC.select("a").distinctOn("a # x").from("t").build().query());
        assertEquals("SELECT DISTINCT ON (a -- x\r\n) a FROM t", mysql.select("a").distinctOn("a -- x\r").from("t").build().query());
        assertEquals("SELECT DISTINCT ON ('--') a FROM t", PSC.select("a").distinctOn("'--'").from("t").build().query());
    }

    // Regression: PostgreSQL, SQL Server and H2 nest block comments, so the comment in "id /* outer /* inner */" stayed open
    // there and swallowed the closing ')' and every clause appended after it.
    @Test
    public void testDistinctOnRejectsUnterminatedNestedBlockComment() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.select("a").distinctOn("id /* outer /* inner */"));
        assertTrue(e.getMessage().contains("unterminated block comment"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Criteria.builder().distinctOn("id /* outer /* inner */"));

        // Balanced nesting is closed under every reading.
        assertEquals("SELECT DISTINCT ON (id /* a /* b */ */) a FROM t WHERE b = 1",
                PSC.select("a").distinctOn("id /* a /* b */ */").from("t").where("b = 1").build().query());
    }

    // Regression: a sole-wildcard INSERT ... SELECT omits the target column list, so into() no longer validated the
    // wildcard item and its comment token was only rejected later, by from().
    @Test
    public void testInsertSelectSoleWildcardRejectsCommentTokenAtInto() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.select("/* c */ *").into("bk"));
        assertEquals("SQL comment token is not allowed in column expression: /* c */ *", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("t.* -- all").into("bk"));

        // Other sole-wildcard shapes, and a quoted "*" (a column named *, not a wildcard).
        assertEquals("INSERT INTO bk SELECT DISTINCT * FROM t", PSC.select("DISTINCT *").into("bk").from("t").build().query());
        assertEquals("INSERT INTO bk (\"*\") SELECT \"*\" FROM t", PSC.select("\"*\"").into("bk").from("t").build().query());
        assertEquals("INSERT INTO bk SELECT `t`.* FROM t", PSC.select("`t`.*").into("bk").from("t").build().query());
        assertEquals("INSERT INTO bk SELECT [t].* FROM t", PSC.select("[t].*").into("bk").from("t").build().query());
    }

    // Pins a deliberate trade-off of the leading join modifiers: an alias literally named like one (GLOBAL, PASTE,
    // POSITIONAL) directly before a JOIN is read as a join modifier, so the entity columns stay unqualified.
    @Test
    public void testJoinModifierWordUsedAsAliasBeforeJoinIsNotTheTableAlias() {
        assertEquals("SELECT first_name AS \"firstName\" FROM account global JOIN b ON b.id = global.id",
                PSC.select("firstName").from("account global JOIN b ON b.id = global.id", Account.class).build().query());
        assertEquals("SELECT first_name AS \"firstName\" FROM account paste JOIN b ON b.id = paste.id",
                PSC.select("firstName").from("account paste JOIN b ON b.id = paste.id", Account.class).build().query());
        assertEquals("SELECT first_name AS \"firstName\" FROM account positional LEFT JOIN b ON true",
                PSC.select("firstName").from("account positional LEFT JOIN b ON true", Account.class).build().query());

        // Without a following JOIN keyword the word is an ordinary alias.
        assertEquals("SELECT global.first_name AS \"firstName\" FROM account global, b",
                PSC.select("firstName").from("account global, b", Account.class).build().query());
    }

    // Regression: set((Object) map, excluded) whose exclusions removed every key reported "'entity' cannot be null or empty".
    @Test
    public void testSetMapEntityValidationMessages() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set((Object) Map.of("a", 1), Set.of("a")));
        assertEquals("No properties remain after exclusions are applied", e.getMessage());

        // An entity map that is empty to begin with still names the parameter.
        e = assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set((Object) new LinkedHashMap<String, Object>(), Set.of("a")));
        assertTrue(e.getMessage().contains("entity"), e.getMessage());

        final Map<Object, Object> nonStringKey = new LinkedHashMap<>();
        nonStringKey.put(1, "x");
        e = assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set((Object) nonStringKey));
        assertTrue(e.getMessage().startsWith("entity keys must be non-blank strings"), e.getMessage());

        final Map<String, Object> blankKey = new LinkedHashMap<>();
        blankKey.put(" ", "x");
        e = assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set((Object) blankKey));
        assertTrue(e.getMessage().startsWith("Key in entity must not"), e.getMessage());

        // set(String, Object) checks the statement before its arguments.
        assertThrows(IllegalStateException.class, () -> PSC.select("a").set((String) null, 1));
        final SqlBuilder closed = PSC.update("t").set("a");
        closed.build();
        assertThrows(IllegalStateException.class, () -> closed.set((String) null, 1));
    }

    // Pins the fail-closed choice of the comment guard without product info: DEFAULT may be any server, and under the
    // standard string reading (no backslash escapes) "'it\'s -- fine'" ends at "\'", leaving a real "--" comment.
    @Test
    public void testCommentGuardRejectsMySqlStyleEscapedLiteralWithoutProductInfo() {
        assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set("note = 'it\\'s -- fine'"));

        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        assertEquals("UPDATE t SET note = 'it\\'s -- fine' WHERE a = ?",
                mysql.update("t").set("note = 'it\\'s -- fine'").where(Filters.eq("a", 1)).build().query());
    }

    // Covers the MySQL RAW_SQL backslash doubling at the call sites beyond WHERE '=': UPDATE SET, LIKE, BETWEEN,
    // MariaDB, a nested structured sub-query (rendered by a same-dialect sub-builder), and a nested raw SubQuery binding.
    @Test
    @SuppressWarnings("deprecation")
    public void testMySqlRawSqlBackslashDoublingAtEveryValueCallSite() {
        final Dsl mysqlRaw = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        final Dsl mariaRaw = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MariaDB")).build());

        assertEquals("UPDATE t SET name = 'x\\\\' WHERE id = 1", mysqlRaw.update("t").set(Map.of("name", "x\\")).where(Filters.eq("id", 1)).build().query());
        assertEquals("SELECT id FROM t WHERE name LIKE 'a\\\\%'", mysqlRaw.select("id").from("t").where(Filters.like("name", "a\\%")).build().query());
        assertEquals("SELECT id FROM t WHERE name BETWEEN 'a\\\\' AND 'b\\\\'",
                mysqlRaw.select("id").from("t").where(Filters.between("name", "a\\", "b\\")).build().query());
        assertEquals("SELECT id FROM t WHERE name = 'x\\\\'", mariaRaw.select("id").from("t").where(Filters.eq("name", "x\\")).build().query());

        final String nested = mysqlRaw.select("id")
                .from("t")
                .where(Filters.in("id", Filters.subQuery(Account.class, List.of("id"), Filters.eq("firstName", "x\\"))))
                .build()
                .query();
        assertTrue(nested.contains("first_name = 'x\\\\'"), nested);

        final String nestedRaw = mysqlRaw.select("id")
                .from("t")
                .where(Filters.in("id",
                        Filters.subQuery("SELECT id FROM u WHERE a IN ?", Arrays.asList(Filters.subQuery("SELECT b FROM v WHERE c = ?", Arrays.asList("z\\"))))))
                .build()
                .query();
        assertTrue(nestedRaw.contains("c = 'z\\\\'"), nestedRaw);
    }

    // Covers the class-aliased path of the implicit-alias quote doubling: the "classAlias." prefix sits inside the
    // quoted label, and every embedded identifier quote is still doubled.
    @Test
    public void testClassAliasedImplicitSelectAliasEscapesEmbeddedIdentifierQuote() {
        assertEquals("SELECT COALESCE(\"nickName\", a.first_name) AS \"acc.COALESCE(\"\"nickName\"\", firstName)\" FROM account a",
                PSC.selectFrom(List.of(
                        Selection.builder(Account.class).tableAlias("a").classAlias("acc").includedPropNames(List.of("COALESCE(\"nickName\", firstName)")).build()))
                        .build()
                        .query());
    }

    // Covers the multi-selection FROM dedupe: identical selections are listed once, the same class under different
    // aliases is kept twice (a self join), and a sub-entity table that a later selection also lists is not repeated.
    @Test
    public void testMultiSelectFromClauseListsEachTableReferenceOnce() {
        assertEquals("account a", SqlBuilder.getFromClause(
                List.of(Selection.builder(Account.class).tableAlias("a").build(), Selection.builder(Account.class).tableAlias("a").build()), NamingPolicy.SNAKE_CASE));
        assertEquals("account a, account b", SqlBuilder.getFromClause(
                List.of(Selection.builder(Account.class).tableAlias("a").build(), Selection.builder(Account.class).tableAlias("b").build()), NamingPolicy.SNAKE_CASE));
        assertEquals("account acc, device",
                SqlBuilder.getFromClause(List.of(Selection.builder(Account.class).tableAlias("acc").includeSubEntityProperties(true).build(),
                        Selection.builder(com.landawn.abacus.query.entity.AccountDevice.class).build()), NamingPolicy.SNAKE_CASE));
    }

    // Covers the NAMED_SQL raw sub-query rename next to ':' beyond "arr[1:?]": whitespace before the colon and an
    // identifier slice bound; a custom handler whose token does not start with ':' needs no separating space.
    @Test
    public void testRawSubQueryPlaceholderAfterColonVariants() {
        AbstractQueryBuilder.SP sp = NSC.select("id")
                .from("t")
                .where(Filters.in("id", Filters.subQuery("SELECT id FROM u WHERE arr[1 :?] = x", Arrays.asList(3))))
                .build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE arr[1 : :param] = x)", sp.query());
        assertEquals(1, ParsedSql.parse(sp.query()).parameterCount());

        sp = NSC.select("id").from("t").where(Filters.in("id", Filters.subQuery("SELECT id FROM u WHERE arr[n:?] = x", Arrays.asList(3)))).build();
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE arr[n: :param] = x)", sp.query());
        assertEquals(1, ParsedSql.parse(sp.query()).parameterCount());

        final Dsl atNamed = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append('@').append(name)).build());
        assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE arr[1:@param] = x)",
                atNamed.select("id").from("t").where(Filters.in("id", Filters.subQuery("SELECT id FROM u WHERE arr[1:?] = x", Arrays.asList(3)))).build().query());
    }

    // Covers per-selection expression resolution: a later selection without a table alias renders its expression
    // unqualified (no implicit alias, since the rendering equals the source text), and a qualifier naming the first
    // selection's alias still resolves to that selection.
    @Test
    public void testMultiSelectExpressionItemsWithoutAliasOrNamingAnotherSelection() {
        assertEquals("SELECT a.id AS \"id\", status + 1 FROM account a, device",
                PSC.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").includedPropNames(List.of("id")).build(),
                        Selection.builder(com.landawn.abacus.query.entity.AccountDevice.class).includedPropNames(List.of("status + 1")).build()))
                        .build()
                        .query());

        final String sql = PSC.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").includedPropNames(List.of("id")).build(),
                Selection.builder(com.landawn.abacus.query.entity.AccountDevice.class).tableAlias("d").includedPropNames(List.of("a.status + 1")).build()))
                .build()
                .query();
        assertTrue(sql.startsWith("SELECT a.id AS \"id\", a.status + 1"), sql);
    }

    // Covers the dialect-aware line-comment termination of append(String): MySQL '#' and "--x", standard vs MySQL
    // string escapes, SQL Server temporary tables, already-terminated fragments, a lone '\r', appendIf/appendIfOrElse,
    // and an UPDATE.
    @Test
    public void testAppendTerminatesTrailingLineCommentPerDialect() {
        final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
        final Dsl sqlServer = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());

        assertEquals("SELECT * FROM t # note\n WHERE a = ?", mysql.select("*").from("t").append("# note").where(Filters.eq("a", 1)).build().query());

        // "--x" is not a comment in MySQL, but is one in standard SQL.
        assertEquals("SELECT * FROM t WHERE a = 1 --x LIMIT 3", mysql.select("*").from("t").append("WHERE a = 1 --x").limit(3).build().query());
        assertEquals("SELECT * FROM t WHERE a = 1 --x\n LIMIT 3", PSC.select("*").from("t").append("WHERE a = 1 --x").limit(3).build().query());
        assertEquals("SELECT * FROM t WHERE a = 1 --x\n LIMIT 3", pg.select("*").from("t").append("WHERE a = 1 --x").limit(3).build().query());

        // Under MySQL 'C:\' does not end, so "-- c" is inside the literal; in standard SQL it is a comment.
        assertEquals("SELECT * FROM t WHERE a = 'C:\\' -- c LIMIT 3", mysql.select("*").from("t").append("WHERE a = 'C:\\' -- c").limit(3).build().query());
        assertEquals("SELECT * FROM t WHERE a = 'C:\\' -- c\n LIMIT 3", PSC.select("*").from("t").append("WHERE a = 'C:\\' -- c").limit(3).build().query());

        // A temporary table is data in SQL Server (and the dialect-agnostic default), a comment in MySQL.
        assertEquals("SELECT * FROM t JOIN #tmp x ON x.id = t.id WHERE a = ?",
                sqlServer.select("*").from("t").append("JOIN #tmp x ON x.id = t.id").where(Filters.eq("a", 1)).build().query());
        assertEquals("SELECT * FROM t JOIN #tmp x ON x.id = t.id WHERE a = ?",
                PSC.select("*").from("t").append("JOIN #tmp x ON x.id = t.id").where(Filters.eq("a", 1)).build().query());
        assertEquals("SELECT * FROM t JOIN #tmp x ON x.id = t.id\n WHERE a = ?",
                mysql.select("*").from("t").append("JOIN #tmp x ON x.id = t.id").where(Filters.eq("a", 1)).build().query());

        // Already terminated: no second line feed; a lone '\r' gets its '\n'.
        assertEquals("SELECT * FROM t -- note\n WHERE a = ?", PSC.select("*").from("t").append("-- note\n").where(Filters.eq("a", 1)).build().query());
        assertEquals("SELECT * FROM t -- note\r\n WHERE a = ?", PSC.select("*").from("t").append("-- note\r").where(Filters.eq("a", 1)).build().query());

        assertEquals("SELECT * FROM t -- c\n WHERE a = ?", PSC.select("*").from("t").appendIf(true, "-- c").where(Filters.eq("a", 1)).build().query());
        assertEquals("SELECT * FROM t -- c\n WHERE a = ?",
                PSC.select("*").from("t").appendIfOrElse(false, "FOR UPDATE", "-- c").where(Filters.eq("a", 1)).build().query());
        assertEquals("UPDATE account SET name = ? -- audit\n WHERE id = ?",
                PSC.update("account").set("name").append("-- audit").where(Filters.eq("id", 1)).build().query());
    }

    // Regression: a select item padded with Unicode whitespace and no alias ("lastName　") rendered the padding glued
    // to the column ("acc.last_name　", another identifier in PostgreSQL/MySQL/SQLite) and kept it in the implicit alias.
    @Test
    public void testSelectItemWithoutAliasIsStrippedOfWhitespacePadding() {
        assertEquals("SELECT acc.last_name AS \"lastName\" FROM account acc", PSC.select("lastName　").from(Account.class).build().query());
        assertEquals("SELECT acc.last_name AS \"lastName\" FROM account acc", PSC.select(" lastName ").from(Account.class).build().query());
        assertEquals("SELECT acc.last_name AS \"lastName\" FROM account acc", PSC.select("lastName ").from(Account.class).build().query());
        assertEquals("SELECT acc.lastName AS \"lastName\" FROM account acc", PLC.select("lastName　").from(Account.class).build().query());
        assertEquals("SELECT UPPER(acc.last_name) AS \"UPPER(lastName)\" FROM account acc", PSC.select("UPPER(lastName)　").from(Account.class).build().query());
        assertEquals("SELECT a.last_name AS \"lastName\", a.first_name AS \"firstName\" FROM account a",
                PSC.select("lastName　", "firstName").from(Account.class, "a").build().query());
        // No-break spaces (U+00A0, U+2007, U+202F), which String.strip() keeps, are padding too.
        assertEquals("SELECT acc.last_name AS \"lastName\" FROM account acc", PSC.select("lastName ").from(Account.class).build().query());
        assertEquals("SELECT acc.last_name AS \"lastName\" FROM account acc", PSC.select(" lastName ").from(Account.class).build().query());
        assertEquals("INSERT INTO bk (last_name) SELECT acc.last_name AS \"lastName\" FROM account acc",
                PSC.select("lastName ").into("bk").from(Account.class).build().query());

        // The INSERT ... SELECT target column, a class-aliased selection, and an explicit select(Map) alias.
        assertEquals("INSERT INTO bk (last_name) SELECT acc.last_name AS \"lastName\" FROM account acc",
                PSC.select("lastName　").into("bk").from(Account.class).build().query());
        assertEquals("SELECT a.last_name AS \"acc.lastName\" FROM account a",
                PSC.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").classAlias("acc").includedPropNames(List.of("lastName　")).build()))
                        .build()
                        .query());
        assertEquals("SELECT acc.last_name AS \"ln\" FROM account acc", PSC.select(Map.of("lastName　", "ln")).from(Account.class).build().query());
    }

    private static Dsl lexDialect(final Dsl base, final String product) {
        return Dsl.forDialect(base.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of(product)).build());
    }

    // Regression: outside MySQL the statement-terminator gate read the second '#' of PostgreSQL's "##" operator as a hash
    // comment, so a ';' after it was hidden and a second statement passed the set-operation / subquery gates.
    @Test
    public void testStatementTerminatorGateReadsHashPairOperatorAsOneToken() {
        final Dsl pg = lexDialect(PSC, "PostgreSQL");
        final Dsl oracle = lexDialect(PSC, "Oracle");

        assertThrows(IllegalArgumentException.class, () -> pg.select("id").from("t").union("SELECT p ## l FROM t; SELECT 2"));
        assertThrows(IllegalArgumentException.class, () -> oracle.select("id").from("t").union("SELECT p ## l FROM t; SELECT 2 FROM dual"));
        assertThrows(IllegalArgumentException.class, () -> pg.select("id").from(pg.select("x").from("t").where("p ## l; SELECT 2"), "s"));
        // The dialect-agnostic default checks both "##" readings.
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").union("SELECT p ## l FROM t; SELECT 2"));

        // The operator alone is accepted (the conservative trailing-comment check still adds a harmless line feed).
        assertEquals("SELECT id FROM t UNION SELECT p ## l FROM t\n", pg.select("id").from("t").union("SELECT p ## l FROM t").build().query());
    }

    // Regression: the trailing-comment check had no "##"-as-operator reading, so on PostgreSQL the second '#' hid a quote
    // ("u.p ## '\n' -- c") and the real trailing "--" comment swallowed the WHERE appended after it.
    @Test
    public void testTrailingCommentAfterHashPairOperatorIsTerminated() {
        final Dsl pg = lexDialect(PSC, "PostgreSQL");

        assertEquals("SELECT * FROM t JOIN u ON u.p ## '\n' -- c\n WHERE t.id = 1",
                pg.select("*").from("t").join("u ON u.p ## '\n' -- c").where("t.id = 1").build().query());
        assertTrue(AbstractQueryBuilder.endsInsideLineComment("u.p ## '\n' -- c", false));
        assertFalse(AbstractQueryBuilder.endsInsideLineComment("u.p ## '\n' = c", false));
    }

    // Regression: ClickHouse's "GLOBAL CROSS JOIN" / "LOCAL ... JOIN" were not recognized as a join start, so the
    // modifier word became the primary table's alias ("GLOBAL.first_name").
    @Test
    public void testGlobalAndLocalJoinModifiersBeforeCrossJoinAreNotPrimaryTableAlias() {
        for (final String join : new String[] { "GLOBAL CROSS JOIN q", "LOCAL CROSS JOIN q", "LOCAL ANY LEFT JOIN q USING (id)", "LOCAL JOIN q USING (id)" }) {
            assertEquals("SELECT a.first_name AS \"firstName\" FROM account a " + join,
                    PSC.select("firstName").from("account a " + join, Account.class).build().query(), join);
        }

        // Not followed by a JOIN keyword: still a plain table alias.
        assertEquals("SELECT local.first_name AS \"firstName\" FROM account local", PSC.select("firstName").from("account local", Account.class).build().query());
    }

    // Regression: MySQL/MariaDB and SQLite end a line comment only at '\n', but the JOIN-connector scan ended it at a lone
    // '\r', so "u -- x\r ON ..." counted as a complete join and a WHERE could follow: those servers silently ran a cross join.
    @Test
    public void testQualifiedJoinWithOnHiddenByLoneCarriageReturnCommentMustBeCompleted() {
        final Dsl mysql = lexDialect(PSC, "MySQL");
        final Dsl sqlite = lexDialect(PSC, "SQLite");
        final Dsl pg = lexDialect(PSC, "PostgreSQL");

        assertThrows(IllegalStateException.class, () -> mysql.select("*").from("t").join("u -- x\r ON u.id = t.id").where("t.id = 1"));
        assertThrows(IllegalStateException.class, () -> sqlite.select("*").from("t").join("u -- x\r ON u.id = t.id").where("t.id = 1"));
        // The dialect-agnostic default may target either kind of server.
        assertThrows(IllegalStateException.class, () -> PSC.select("*").from("t").join("u -- x\r ON u.id = t.id").where("t.id = 1"));
        assertEquals("SELECT * FROM t JOIN u -- x\r ON u.id = t.id\n ON u.id = t.id",
                mysql.select("*").from("t").join("u -- x\r ON u.id = t.id").on("u.id = t.id").build().query());

        // PostgreSQL ends the comment at '\r': the ON is real there, so the join is complete.
        assertEquals("SELECT * FROM t JOIN u -- x\r ON u.id = t.id\n WHERE t.id = 1",
                pg.select("*").from("t").join("u -- x\r ON u.id = t.id").where("t.id = 1").build().query());
        assertThrows(IllegalStateException.class, () -> pg.select("*").from("t").join("u -- x\r ON u.id = t.id").on("u.id = t.id"));
        // A CRLF line break ends the comment everywhere.
        assertEquals("SELECT * FROM t JOIN u -- x\r\n ON u.id = t.id WHERE t.id = 1",
                mysql.select("*").from("t").join("u -- x\r\n ON u.id = t.id").where("t.id = 1").build().query());

        // Rejecting a CROSS JOIN connector counts one visible under either line-end convention.
        assertThrows(IllegalArgumentException.class, () -> mysql.select("*").from("t").crossJoin("u -- x\r ON u.id = t.id"));
        assertThrows(IllegalArgumentException.class, () -> pg.select("*").from("t").crossJoin("u -- it's\r 'a\n ON true"));
    }

    // Regression: MySQL "..." string literals honor backslash escapes, but the alias, FROM-separator and JOIN-connector
    // scanners (and the comment guard) read them with quote doubling only, so "\"" looked like an open quote.
    @Test
    public void testLexicalScannersHonorMySqlDoubleQuotedStringEscapes() {
        final Dsl mysql = lexDialect(PSC, "MySQL");

        assertEquals("SELECT CONCAT(first_name, \"\\\"\") AS q FROM t", mysql.select("CONCAT(firstName, \"\\\"\") AS q").from("t").build().query());
        assertEquals("SELECT CONCAT(first_name, \"\\\"\") AS q FROM t", PSC.select("CONCAT(firstName, \"\\\"\") AS q").from("t").build().query());
        assertEquals("SELECT d.first_name AS `firstName` FROM (SELECT \"\\\"\" AS q) d",
                mysql.select("firstName").from("(SELECT \"\\\"\" AS q) d", Account.class).build().query());
        assertEquals("SELECT d.first_name AS `firstName` FROM (SELECT \"\\\"\" AS q) d, other o",
                mysql.select("firstName").from("(SELECT \"\\\"\" AS q) d, other o", Account.class).build().query());
        assertEquals("SELECT * FROM t JOIN (SELECT \"\\\"\" AS q) u ON u.q = t.q WHERE t.id = 1",
                mysql.select("*").from("t").join("(SELECT \"\\\"\" AS q) u ON u.q = t.q").where("t.id = 1").build().query());

        // "a\"" is one MySQL string, so the "--" after it is a real comment.
        assertThrows(IllegalArgumentException.class, () -> mysql.update("t").set("note = \"a\\\"\" -- x"));
        assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set("note = \"a\\\"\" -- x"));
    }

    // Regression: a custom named-parameter handler rendering "#name" tokens was read as a hash comment by the rename
    // scanner outside SQL Server, so a colliding child placeholder kept its name (two "#id" bound to different values).
    @Test
    public void testCustomHashNamedParameterTokensAreRenamedAcrossSetOperation() {
        for (final String product : new String[] { null, "PostgreSQL", "Oracle", "MySQL" }) {
            SqlDialect.SqlDialectBuilder dialect = NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append('#').append(name));

            if (product != null) {
                dialect = dialect.productInfo(SqlDialect.ProductInfo.of(product));
            }

            final Dsl hash = Dsl.forDialect(dialect.build());
            final AbstractQueryBuilder.SP sp = hash.select("id")
                    .from("t")
                    .where(Filters.eq("id", 1))
                    .union(hash.select("id").from("t2").where(Filters.eq("id", 2)))
                    .build();

            assertEquals("SELECT id FROM t WHERE id = #id UNION SELECT id FROM t2 WHERE id = #id_2\n", sp.query(), product);
            assertEquals(Arrays.asList(1, 2), sp.parameters(), product);
        }
    }

    // Covers the single-pass child-placeholder rename (planned renames applied at once): many colliding names next to
    // many '#' temporary tables, a later operand, a custom handler, and a property named like a generated suffix.
    @Test
    public void testChildPlaceholderRenameAppliesAllPlannedRenamesInOnePass() {
        final StringBuilder from = new StringBuilder();
        final List<Condition> conditions = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            from.append(i == 0 ? "" : ", ").append("#t").append(i);
            conditions.add(Filters.eq("id", i));
        }

        for (final Dsl dsl : new Dsl[] { NSC, MSC }) {
            final AbstractQueryBuilder.SP sp = dsl.select("id")
                    .from("t")
                    .where(Filters.and(Filters.eq("id", -1), Filters.eq("id", -2)))
                    .union(dsl.select("id").from(from.toString()).where(Filters.and(conditions)))
                    .build();
            final List<String> names = new ArrayList<>(ParsedSql.parse(sp.query()).namedParameters());

            assertEquals(32, names.size());
            assertEquals(32, new java.util.HashSet<>(names).size(), sp.query());
            assertEquals("id_32", names.get(31));
        }

        AbstractQueryBuilder.SP sp = NSC.select("id")
                .from("t")
                .where(Filters.and(Filters.eq("id", 1), Filters.eq("id_3", 13)))
                .union(NSC.select("id")
                        .from("t2")
                        .where(Filters.and(Filters.eq("id", 2), Filters.eq("id", 3), Filters.eq("id", 4), Filters.eq("id_3", 5), Filters.eq("id_2", 6))))
                .build();
        assertEquals("SELECT id FROM t WHERE (id = :id) AND (id_3 = :id_3) UNION SELECT id FROM t2 WHERE (id = :id_2) AND (id = :id_5) AND (id = :id_4)"
                + " AND (id_3 = :id_3_3) AND (id_2 = :id_2_2)", sp.query());
        assertEquals(Arrays.asList(1, 13, 2, 3, 4, 5, 6), sp.parameters());

        sp = NSC.select("id")
                .from("t")
                .where(Filters.and(Filters.eq("id", 1), Filters.eq("id", 11)))
                .union(NSC.select("id").from("t2").where(Filters.and(Filters.eq("id", 2), Filters.eq("id", 3))))
                .union(NSC.select("id").from("t3").where(Filters.and(Filters.eq("id", 4), Filters.eq("id_2", 5))))
                .build();
        assertEquals("SELECT id FROM t WHERE (id = :id) AND (id = :id_2) UNION SELECT id FROM t2 WHERE (id = :id_3) AND (id = :id_4)"
                + " UNION SELECT id FROM t3 WHERE (id = :id_5) AND (id_2 = :id_2_2)", sp.query());

        final Dsl at = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append('@').append(name)).build());
        sp = at.select("id")
                .from("t")
                .where(Filters.and(Filters.eq("id", 1), Filters.eq("id", 3)))
                .union(at.select("id").from("t2").where(Filters.and(Filters.eq("id", 2), Filters.eq("id", 4), Filters.eq("idx", 5))))
                .build();
        assertEquals("SELECT id FROM t WHERE (id = @id) AND (id = @id_2) UNION SELECT id FROM t2 WHERE (id = @id_3) AND (id = @id_4) AND (idx = @idx)",
                sp.query());

        // A child rendered with the default handler under a parent with a custom one is re-rendered with the parent's tokens.
        sp = at.select("id").from("t").where(Filters.eq("id", 1)).union(NSC.select("id").from("t2").where(Filters.eq("id", 2))).build();
        assertEquals("SELECT id FROM t WHERE id = @id UNION SELECT id FROM t2 WHERE id = @id_2", sp.query());

        // The dialect-agnostic default still fails closed when a backslash makes quoted-text boundaries ambiguous.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> NSC.select("id")
                .from("t")
                .where(Filters.eq("id", 1))
                .union(NSC.select("id").from("t2").where(Filters.and(Filters.expr("\"a\\\" = 1"), Filters.eq("id", 2)))));
        assertTrue(e.getMessage().contains("a backslash in quoted text"), e.getMessage());
    }

    // Covers lone-'\r' handling of the trailing-comment check: a comment ended by '\r' is terminated with '\n' only once,
    // and a later '\n' still ends it.
    @Test
    public void testTrailingCommentLoneCarriageReturnReadingEndsAtLaterLineFeed() {
        assertFalse(AbstractQueryBuilder.endsInsideLineComment("a -- x\r b\n c", false));
        assertTrue(AbstractQueryBuilder.endsInsideLineComment("a -- x\r b", false));
        assertFalse(AbstractQueryBuilder.endsInsideLineComment("a -- x\r\n b -- y\r\n", false));
        assertEquals("a -- x\r\n", QueryUtil.terminateLineComment("a -- x\r"));
        assertEquals("a -- x\r\n", QueryUtil.terminateLineComment("a -- x\r\n"));
    }

    // Covers the dialect-aware JOIN-connector and FROM-separator scans on further dialects: a standard 'C:\' literal cannot
    // hide a CROSS JOIN connector or the separator after the primary table, while dialect comment/bracket rules still apply.
    @Test
    public void testJoinConnectorAndFromSeparatorScansOnMoreDialects() {
        final Dsl oracle = lexDialect(PSC, "Oracle");
        final Dsl db2 = lexDialect(PSC, "DB2");
        final Dsl sqlServer = lexDialect(PSC, "Microsoft SQL Server");
        final Dsl mysql = lexDialect(PSC, "MySQL");
        final Dsl sqlite = lexDialect(PSC, "SQLite");
        final Dsl pg = lexDialect(PSC, "PostgreSQL");

        assertThrows(IllegalArgumentException.class, () -> oracle.select("u.id").from("users u").crossJoin("(SELECT 'C:\\' FROM dual) s ON 1=1"));
        assertThrows(IllegalArgumentException.class, () -> db2.select("u.id").from("users u").crossJoin("(SELECT 'C:\\' FROM sysibm.sysdummy1) s ON 1=1"));

        for (final Dsl dsl : new Dsl[] { oracle, db2, sqlServer }) {
            assertEquals("SELECT x.first_name AS \"firstName\" FROM (SELECT * FROM account WHERE p = 'C:\\') x, other o",
                    dsl.select("firstName").from("(SELECT * FROM account WHERE p = 'C:\\') x, other o", Account.class).build().query());
        }

        // from(Collection): only the first element's primary reference supplies the table alias.
        final String collectionFrom = pg.select(Account.class).from(Arrays.asList("(SELECT * FROM account WHERE p = 'C:\\') x, other o", "z")).build().query();
        assertTrue(collectionFrom.startsWith("SELECT x.id AS \"id\", "), collectionFrom);
        assertTrue(collectionFrom.endsWith(" FROM (SELECT * FROM account WHERE p = 'C:\\') x, other o, z"), collectionFrom);

        // Dialect comment and bracket rules keep connector-looking text hidden ...
        assertEquals("SELECT u.id FROM users u CROSS JOIN s # ON x\n", mysql.select("u.id").from("users u").crossJoin("s # ON x\n").build().query());
        assertEquals("SELECT u.id FROM users u CROSS JOIN [x ON] s", sqlServer.select("u.id").from("users u").crossJoin("[x ON] s").build().query());
        assertEquals("SELECT u.id FROM users u JOIN [s ON x] s2 ON s2.id = u.id WHERE u.id = 1",
                sqlite.select("u.id").from("users u").join("[s ON x] s2 ON s2.id = u.id").where("u.id = 1").build().query());
        // ... and the dialect-agnostic default also reads "##" as a possible MySQL comment start (fail closed).
        assertThrows(IllegalArgumentException.class, () -> PSC.select("u.id").from("users u").crossJoin("u ## don't\nON true"));
    }

    // Covers the "##" token and the backslash readings of the alias scanner: an explicit dialect finds the derived-table
    // alias past "##", the default infers none where its readings disagree, and standard dialects keep 'it\'s' unterminated.
    @Test
    public void testAliasScannerHashPairAndBackslashReadingsPerDialect() {
        final Dsl pg = lexDialect(PSC, "PostgreSQL");
        final Dsl sqlServer = lexDialect(PSC, "Microsoft SQL Server");

        assertEquals("SELECT a.first_name AS \"firstName\" FROM (SELECT x ## y AS c FROM t) a\n",
                pg.select("firstName").from("(SELECT x ## y AS c FROM t) a", Account.class).build().query());
        assertEquals("SELECT first_name AS \"firstName\" FROM (SELECT x ## y AS c FROM t) a\n",
                PSC.select("firstName").from("(SELECT x ## y AS c FROM t) a", Account.class).build().query());

        // Both default readings complete but disagree, or none completes: no alias is inferred and the item is verbatim.
        assertEquals("SELECT 'a\\' AS x, \\'c' AS y FROM t", PSC.select("'a\\' AS x, \\'c' AS y").from("t").build().query());
        assertEquals("SELECT 'a\\' AS x, 'b AS y FROM t", PSC.select("'a\\' AS x, 'b AS y").from("t").build().query());
        // A standard dialect does not honor \' (the literal is unterminated there), so no derived-table alias is inferred.
        assertEquals("SELECT first_name AS \"firstName\" FROM (SELECT 'it\\'s' AS p) a",
                pg.select("firstName").from("(SELECT 'it\\'s' AS p) a", Account.class).build().query());
        assertEquals("SELECT 'C:\\' || ' AS x' AS y FROM t", sqlServer.select("'C:\\' || ' AS x' AS y").from("t").build().query());
    }

    // Covers the dialect-aware '#' and string readings of the child-placeholder rename on the remaining paths: a custom
    // handler, an IN snapshot, a derived table, "##g", MySQL "..." escapes, and the default's non-ambiguous backslashes.
    @Test
    public void testChildPlaceholderRenameHashAndQuoteReadingsOnAllPaths() {
        final Dsl at = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append('@').append(name)).build());

        assertEquals("SELECT id FROM t WHERE id = @id UNION SELECT id FROM t2 x #it's\n WHERE id = @id_2",
                at.select("id").from("t").where(Filters.eq("id", 1)).union(at.select("id").from("t2 x #it's\n").where(Filters.eq("id", 2))).build().query());
        assertEquals("SELECT id FROM t WHERE (id = :id) AND (id IN (SELECT id FROM t2 x #it's\n WHERE id = :id_2))",
                NSC.select("id")
                        .from("t")
                        .where(Filters.and(Filters.eq("id", 1), Filters.in("id", NSC.select("id").from("t2 x #it's\n").where(Filters.eq("id", 2)).toSubQuery())))
                        .build()
                        .query());
        assertEquals("SELECT id FROM (SELECT id FROM t2 x #it's\n WHERE id = :id) d WHERE id = :id_2",
                NSC.select("id").from(NSC.select("id").from("t2 x #it's\n").where(Filters.eq("id", 2)), "d").where(Filters.eq("id", 1)).build().query());
        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM ##g x WHERE id = :id_2",
                NSC.select("id").from("t").where(Filters.eq("id", 1)).union(NSC.select("id").from("##g x").where(Filters.eq("id", 2))).build().query());

        // MySQL "a\"" is one string literal, for iBATIS markers and custom tokens too.
        final Dsl mysqlIbatis = lexDialect(MSC, "MySQL");
        assertEquals("SELECT id FROM t WHERE id = #{id} UNION SELECT id FROM t2 WHERE (n <> \"a\\\"\") AND (id = #{id_2})",
                mysqlIbatis.select("id")
                        .from("t")
                        .where(Filters.eq("id", 1))
                        .union(mysqlIbatis.select("id").from("t2").where(Filters.and(Filters.expr("n <> \"a\\\"\""), Filters.eq("id", 2))))
                        .build()
                        .query());
        final Dsl atMysql = Dsl.forDialect(at.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
        assertEquals("SELECT id FROM t WHERE id = @id UNION SELECT id FROM t2 WHERE (n <> \"a\\\"\") AND (id = @id_2)",
                atMysql.select("id")
                        .from("t")
                        .where(Filters.eq("id", 1))
                        .union(atMysql.select("id").from("t2").where(Filters.and(Filters.expr("n <> \"a\\\"\""), Filters.eq("id", 2))))
                        .build()
                        .query());

        // The default's two readings agree on even backslashes and on a backslash inside a single-quoted string.
        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM t2 WHERE (\"a\\\\b\" = 1) AND (id = :id_2)",
                NSC.select("id")
                        .from("t")
                        .where(Filters.eq("id", 1))
                        .union(NSC.select("id").from("t2").where(Filters.and(Filters.expr("\"a\\\\b\" = 1"), Filters.eq("id", 2))))
                        .build()
                        .query());
        assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM t2 WHERE (j = '{\"a\":\"b\\\"c\"}') AND (id = :id_2)",
                NSC.select("id")
                        .from("t")
                        .where(Filters.eq("id", 1))
                        .union(NSC.select("id").from("t2").where(Filters.and(Filters.expr("j = '{\"a\":\"b\\\"c\"}'"), Filters.eq("id", 2))))
                        .build()
                        .query());

        // Explicit standard dialects read "a\" as a complete quoted identifier: no ambiguity, no exception.
        for (final String product : new String[] { "PostgreSQL", "Oracle", "Microsoft SQL Server" }) {
            final Dsl named = lexDialect(NSC, product);
            assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM t2 WHERE (\"a\\\" = 1) AND (id = :id_2)",
                    named.select("id")
                            .from("t")
                            .where(Filters.eq("id", 1))
                            .union(named.select("id").from("t2").where(Filters.and(Filters.expr("\"a\\\" = 1"), Filters.eq("id", 2))))
                            .build()
                            .query(),
                    product);
        }
    }

    // Covers the comment guard's string readings (near misses stay accepted; H2/SQLite and non-E prefixes are standard)
    // and MySQL hash handling inside quotes and backticks.
    @Test
    public void testCommentGuardStringReadingNearMissesAndQuotedHashes() {
        final Dsl pg = lexDialect(PSC, "PostgreSQL");
        final Dsl h2 = lexDialect(PSC, "H2");
        final Dsl sqlite = lexDialect(PSC, "SQLite");
        final Dsl mysql = lexDialect(PSC, "MySQL");

        assertEquals("UPDATE t SET dir = 'C:\\', note = 'x' WHERE a = 1", pg.update("t").set("dir = 'C:\\', note = 'x'").where("a = 1").build().query());
        assertEquals("SELECT 'a\\\\b' AS x FROM t", pg.select("'a\\\\b' AS x").from("t").build().query());
        assertThrows(IllegalArgumentException.class, () -> h2.select("'x\\' -- c").from("t"));
        assertThrows(IllegalArgumentException.class, () -> sqlite.select("'x\\' -- c").from("t"));
        // "fee'...'" is an identifier followed by a standard string, not an E'...' escape string.
        assertThrows(IllegalArgumentException.class, () -> pg.select("fee'a\\' -- x'").from("t"));

        assertEquals("SELECT 'a##b' AS x FROM t", mysql.select("'a##b' AS x").from("t").build().query());
        assertEquals("SELECT `a#b` FROM t", mysql.select("`a#b`").from("t").build().query());
        assertEquals("SELECT a ## b FROM t", PSC.select("a ## b").from("t").build().query());
    }

    @Test
    public void testUnionRenameWithCustomWhitespaceLedSeparatorAndSubscripts() {
        // Regression: with a configured separator starting with whitespace (" AND"), ParsedSql could not align the
        // tokens back onto the SQL, so renaming the colliding child placeholder of a union whose child contains a
        // subscript after an AND threw IllegalStateException ("Cannot locate subscript brackets").
        for (final String separator : new String[] { "::", " AND" }) {
            final Dsl dsl = Dsl.forDialect(SqlDialect.builder()
                    .sqlPolicy(SqlDialect.SqlPolicy.NAMED_SQL)
                    .tokenizerConfig(SqlParser.tokenizerConfigBuilder().withSeparator(separator).build())
                    .build());

            final String sql = dsl.select("a")
                    .from("t")
                    .where(Filters.eq("id", 1))
                    .union(dsl.select("a[1]").from("t").where(Filters.eq("id", 2).and(Filters.eq("b", 3)).and(Filters.expr("x[1] = 0"))))
                    .build()
                    .query();

            assertEquals("SELECT a FROM t WHERE id = :id UNION SELECT a[1] FROM t WHERE (id = :id_2) AND (b = :b) AND (x[1] = 0)", sql, separator);
        }
    }

    // Regression: with a shared "#name" handler, a child token that keeps its name ("#id") was read as a hash comment by the
    // rename scan, hiding the colliding token after it on the same line, so two "#x" placeholders were bound to different values.
    @Test
    public void testNonRenamedCustomHashTokenDoesNotHideLaterRenamedToken() {
        for (final String product : new String[] { null, "PostgreSQL", "MySQL" }) {
            SqlDialect.SqlDialectBuilder dialect = NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append('#').append(name));

            if (product != null) {
                dialect = dialect.productInfo(SqlDialect.ProductInfo.of(product));
            }

            final Dsl hash = Dsl.forDialect(dialect.build());

            AbstractQueryBuilder.SP sp = hash.select("id")
                    .from("t")
                    .where(Filters.eq("x", 1))
                    .union(hash.select("id").from("t2").where(Filters.and(Filters.eq("id", 2), Filters.eq("x", 3))))
                    .build();
            assertEquals("SELECT id FROM t WHERE x = #x UNION SELECT id FROM t2 WHERE (id = #id) AND (x = #x_2)\n", sp.query(), product);
            assertEquals(Arrays.asList(1, 2, 3), sp.parameters(), product);

            // The IN-subquery snapshot path.
            sp = hash.select("id")
                    .from("t")
                    .where(Filters.and(Filters.eq("x", 1),
                            Filters.in("id", hash.select("id").from("t2").where(Filters.and(Filters.eq("id", 2), Filters.eq("x", 3))).toSubQuery())))
                    .build();
            assertEquals("SELECT id FROM t WHERE (x = #x) AND (id IN (SELECT id FROM t2 WHERE (id = #id) AND (x = #x_2)\n))", sp.query(), product);
        }
    }

    // Regression: a qualified JOIN whose ON follows a comment ended only by a lone '\r' is rejected as incomplete (MySQL and
    // SQLite read the ON as part of the comment), but the error claimed no ON/USING had been written at all.
    @Test
    public void testIncompleteJoinErrorExplainsConnectorHiddenByLoneCarriageReturnComment() {
        final Dsl mysql = lexDialect(PSC, "MySQL");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> PSC.select("id").from("account a").join("device d -- x\r ON a.id = d.account_id").where(Filters.eq("id", 1)));
        assertTrue(e.getMessage().startsWith("The preceding qualified JOIN must be completed with on(...) or using(...) before 'WHERE'. Its ON/USING connector"),
                e.getMessage());
        assertTrue(e.getMessage().contains("lone carriage return") && e.getMessage().contains("SqlDialect.productInfo"), e.getMessage());

        e = assertThrows(IllegalStateException.class, () -> mysql.select("id").from("account a").join("device d -- x\r ON a.id = d.account_id").build());
        assertTrue(e.getMessage().contains("lone carriage return") && !e.getMessage().contains("SqlDialect.productInfo"), e.getMessage());
        e = assertThrows(IllegalStateException.class,
                () -> mysql.select("id").from("account a").join("device d -- x\r ON a.id = d.account_id").union("SELECT 1"));
        assertTrue(e.getMessage().contains("lone carriage return"), e.getMessage());

        // A join without any ON keeps the plain message.
        e = assertThrows(IllegalStateException.class, () -> PSC.select("id").from("account a").join("device d").where(Filters.eq("id", 1)));
        assertEquals("The preceding qualified JOIN must be completed with on(...) or using(...) before 'WHERE'", e.getMessage());
    }

    // Regression: an explicitly selected self-referencing sub-entity ("parent" of a Node) was expanded with the entity's own
    // @Table alias: under another alias it referenced an undeclared table ("n.id ... FROM node x"), and under the default
    // alias every node silently became its own parent.
    @Test
    public void testExplicitlySelectedSelfReferencingSubEntityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PSC
                .selectFrom(List.of(Selection.builder(SubEntityNode.class).tableAlias("x").includedPropNames(List.of("id", "parent")).build()))
                .build());
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id", "parent").from(SubEntityNode.class, "x"));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from(SubEntityNode.class, "x").orderByDesc("parent"));
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.select("id", "parent").from(SubEntityNode.class));
        assertTrue(e.getMessage().contains("Self-referencing sub-entity property 'parent'"), e.getMessage());

        // The generated projection leaves the cyclic property out, and an ordinary sub-entity is still expanded.
        assertEquals("SELECT x.id AS \"id\", x.name AS \"name\" FROM node x", PSC.selectFrom(SubEntityNode.class, "x", true).build().query());
        assertTrue(PSC.select("id", "devices").from(Account.class).build().query().startsWith("SELECT acc.id AS \"id\", device.id AS \"devices.id\", "));
    }

    // Regression: distinctOn checked its argument (an unterminated block comment) before the statement, so a non-SELECT
    // builder reported an IllegalArgumentException instead of the IllegalStateException every other path gives.
    @Test
    public void testDistinctOnChecksStatementBeforeArgument() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> PSC.update("t").distinctOn("/* x"));
        assertEquals("selectModifier() is only valid for SELECT queries", e.getMessage());

        e = assertThrows(IllegalStateException.class, () -> PSC.select("a").distinct().distinctOn("/* x"));
        assertEquals("selectModifier has already been set and cannot be set again", e.getMessage());

        assertThrows(IllegalArgumentException.class, () -> PSC.select("a").distinctOn("/* x"));
        assertEquals("SELECT DISTINCT ON (a) a FROM t", PSC.select("a").distinctOn("a").from("t").build().query());
    }

    // Covers the MySQL dash-pair rewrite ("a--1" is a - (-1) there) at its documented boundaries: MyBatis markers, block
    // comments and backslash-escaped "..." strings are not rewritten, "--" followed by a tab is still a comment, and the scan
    // stops at the first '[' (a known partial fix: the later "d--1 > 0" is still read as a comment).
    @Test
    public void testMySqlDashPairRewriteSkipsMarkersCommentsAndStrings() {
        final Dsl mysql = lexDialect(PSC, "MySQL");

        // The marker is not rewritten; its nonsensical "--" name then still reads as a comment (pre-existing).
        assertEquals("SELECT id FROM t WHERE a = #{x", mysql.select("id").from("t").where(Filters.expr("a = #{x--y} AND b--1 > 0")).build().query());
        assertEquals("SELECT id FROM t WHERE a = 1 AND b- -1 > 0", mysql.select("id").from("t").where(Filters.expr("a /* a--1 */ = 1 AND b--1 > 0")).build().query());
        assertEquals("SELECT id FROM t WHERE n = \"a\\\"--1\" AND b- -1 > 0",
                mysql.select("id").from("t").where(Filters.expr("n = \"a\\\"--1\" AND b--1 > 0")).build().query());
        assertEquals("SELECT id FROM t WHERE a = 1  ORDER BY id", mysql.select("id").from("t").where(Filters.expr("a = 1 --\tc")).orderBy("id").build().query());
        assertEquals("SELECT id FROM t WHERE c- -1 > arr[1] AND d", mysql.select("id").from("t").where(Filters.expr("c--1 > arr[1] AND d--1 > 0")).build().query());
    }

    // Regression: when a literal name ("id_2") pushed a composed parameter to a higher suffix ("id_3"), the parent's
    // occurrence count stayed below that suffix, so a later composition could reuse the name for a different value or
    // skip its handler conversion. Covered for a reusable union snapshot here and for the other composition paths below.
    @Test
    @SuppressWarnings("deprecation")
    public void testReusableUnionSnapshotRetainsSkippedParameterSuffixes() {
        for (final Dsl dsl : new Dsl[] { Dsl.NSC, Dsl.MSC }) {
            final SubQuery snapshot = compoundWithSkippedSuffix(dsl).toSubQuery();
            final String originalSql = snapshot.rawSql();

            for (final int outerValue : new int[] { 4, 5 }) {
                final SP result = dsl.select("id")
                        .from("outer_table")
                        .where(Filters.and(Filters.eq("id_3", outerValue), Filters.in("id", snapshot)))
                        .build();

                assertDistinctBindings(result, List.of(outerValue, 1, 2, 3));
                assertEquals(originalSql, snapshot.rawSql());
                assertEquals(List.of(1, 2, 3), snapshot.parameters());
            }
        }
    }

    // Same suffix high-water mark through a snapshot nested in a predicate subquery.
    @Test
    @SuppressWarnings("deprecation")
    public void testNestedPredicateSnapshotRetainsSkippedParameterSuffixes() {
        for (final Dsl dsl : new Dsl[] { Dsl.NSC, Dsl.MSC }) {
            final SubQuery inner = dsl.select("id").from("archive").where(Filters.eq("id", 3)).toSubQuery();
            final SubQuery middle = dsl.select("id")
                    .from("users")
                    .where(Filters.and(Filters.eq("id", 1), Filters.eq("id_2", 2), Filters.in("id", inner)))
                    .toSubQuery();
            final SP result = dsl.select("id")
                    .from("outer_table")
                    .where(Filters.and(Filters.eq("id_3", 4), Filters.in("id", middle)))
                    .build();

            assertDistinctBindings(result, List.of(4, 1, 2, 3));
        }
    }

    // Same suffix high-water mark through a derived table composed as a set-operation sibling.
    @Test
    @SuppressWarnings("deprecation")
    public void testDerivedTableRetainsSkippedParameterSuffixesWhenComposedAsSibling() {
        for (final Dsl dsl : new Dsl[] { Dsl.NSC, Dsl.MSC }) {
            final SqlBuilder derived = dsl.select("id").from(compoundWithSkippedSuffix(dsl), "inner_query");
            final SP result = dsl.select("id").from("outer_table").where(Filters.eq("id_3", 4)).union(derived).build();

            assertDistinctBindings(result, List.of(4, 1, 2, 3));
        }
    }

    // Same suffix high-water mark across repeated nesting, each level promoting another suffix.
    @Test
    @SuppressWarnings("deprecation")
    public void testRepeatedCompositionRetainsEachNewlyPromotedSuffix() {
        for (final Dsl dsl : new Dsl[] { Dsl.NSC, Dsl.MSC }) {
            SubQuery snapshot = compoundWithSkippedSuffix(dsl).toSubQuery();
            final ArrayList<Integer> values = new ArrayList<>(List.of(1, 2, 3));

            for (int suffix = 3; suffix <= 6; suffix++) {
                values.add(0, suffix + 1);
                snapshot = dsl.select("id")
                        .from("outer_table")
                        .where(Filters.and(Filters.eq("id_" + suffix, suffix + 1), Filters.in("id", snapshot)))
                        .toSubQuery();

                assertDistinctBindings(new SP(snapshot.rawSql(), ImmutableList.copyOf(snapshot.parameters())), values);
            }
        }
    }

    // Same suffix high-water mark under a custom named-parameter handler: every parameter is converted.
    @Test
    public void testCustomHandlerRewritesEveryParameterOfComposedSnapshot() {
        final Dsl at = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sql, name) -> sql.append('@').append(name)).build());
        final SP result = at.select("id").from(compoundWithSkippedSuffix(Dsl.NSC), "inner_query").build();

        assertEquals(List.of(1, 2, 3), result.parameters());
        assertTrue(result.query().contains("id = @id_3"), result.query());
        assertFalse(result.query().contains(":id"), result.query());
    }

    private static SqlBuilder compoundWithSkippedSuffix(final Dsl dsl) {
        return dsl.select("id")
                .from("users")
                .where(Filters.and(Filters.eq("id", 1), Filters.eq("id_2", 2)))
                .union(dsl.select("id").from("archive").where(Filters.eq("id", 3)));
    }

    private static void assertDistinctBindings(final SP result, final List<Integer> values) {
        final List<String> names = ParsedSql.parse(result.query()).namedParameters();
        assertEquals(values, result.parameters());
        assertEquals(values.size(), names.size(), result.query());
        assertEquals(values.size(), new HashSet<>(names).size(), result.query());
    }

    // Regression: selecting a quoted physical @Column name through the entity mapping emitted its quotes undoubled
    // inside the quoted implicit label (AS ""quoted.name""), which is invalid SQL.
    @Test
    public void testMappedPhysicalColumnAliasesEscapeDoubleQuotes() {
        assertEquals("SELECT t.\"quoted.name\" AS \"\"\"quoted.name\"\"\" FROM quoted_columns t",
                Dsl.PSC.select("\"quoted.name\"").from(QuotedColumns.class, "t").build().query());
        assertEquals("SELECT t.\"quoted.name\" AS \"t.\"\"quoted.name\"\"\" FROM quoted_columns t",
                Dsl.PSC.select("t.\"quoted.name\"").from(QuotedColumns.class, "t").build().query());
        assertEquals("SELECT t.\"escaped\"\"name\" AS \"\"\"escaped\"\"\"\"name\"\"\" FROM quoted_columns t",
                Dsl.PSC.select("\"escaped\"\"name\"").from(QuotedColumns.class, "t").build().query());
    }

    // Same for a backtick-quoted dialect and a class-aliased label.
    @Test
    public void testMappedPhysicalColumnAliasesEscapeBackticksWithClassAlias() {
        final Dsl backtick = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().identifierQuote(IdentifierQuote.BACKTICK).build());
        assertEquals("SELECT t.`tick.name` AS ```tick.name``` FROM quoted_columns t",
                backtick.select("`tick.name`").from(QuotedColumns.class, "t").build().query());
        assertEquals("SELECT t.`tick.name` AS `row.``tick.name``` FROM quoted_columns t",
                backtick.selectFrom(Selection.builder(QuotedColumns.class)
                        .tableAlias("t")
                        .classAlias("row")
                        .includedPropNames(List.of("`tick.name`"))
                        .build()).build().query());
    }

    // Controls: property names, explicit labels, NO_CHANGE and unmapped expressions keep their labels.
    @Test
    public void testOrdinaryMappedAndExplicitAliasesRetainTheirLabels() {
        assertEquals("SELECT t.\"quoted.name\" AS \"value\" FROM quoted_columns t",
                Dsl.PSC.select("value").from(QuotedColumns.class, "t").build().query());
        assertEquals("SELECT t.\"quoted.name\" AS \"label\" FROM quoted_columns t",
                Dsl.PSC.select(Map.of("\"quoted.name\"", "label")).from(QuotedColumns.class, "t").build().query());
        assertEquals("SELECT t.\"quoted.name\" FROM quoted_columns t",
                Dsl.PSB.select("\"quoted.name\"").from(QuotedColumns.class, "t").build().query());
        assertEquals("SELECT \"quoted.name\" AS \"\"\"quoted.name\"\"\" FROM t",
                Dsl.PSC.select("\"quoted.name\"").from("t").build().query());
    }

    @Table("quoted_columns")
    public static final class QuotedColumns {
        @Column("\"quoted.name\"")
        private String value;
        @Column("`tick.name`")
        private String tick;
        @Column("\"escaped\"\"name\"")
        private String escaped;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getTick() {
            return tick;
        }

        public void setTick(final String tick) {
            this.tick = tick;
        }

        public String getEscaped() {
            return escaped;
        }

        public void setEscaped(final String escaped) {
            this.escaped = escaped;
        }
    }
}
