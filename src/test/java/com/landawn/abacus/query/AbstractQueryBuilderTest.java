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
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.Union;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class AbstractQueryBuilderTest extends TestBase {
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
        String sql = Dsl.PSC.select("id", "firstName", "lastName").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testToSql() {
        String sql = Dsl.PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testBuild() {
        AbstractQueryBuilder.SP sqlPair = Dsl.PSC.select("id").from(Account.class).where(Filters.eq("id", 1)).build();
        assertNotNull(sqlPair);
        assertTrue(sqlPair.query().contains("WHERE"));
        assertEquals(1, sqlPair.parameters().size());
    }

    @Test
    public void testBuildSnapshotsProtectedParameterBuffer() {
        final SqlBuilder builder = Dsl.PSC.select("id").from(Account.class).where(Filters.eq("id", 1));
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
        final SqlBuilder builder = Dsl.PSC.select("id").distinct();

        builder.selectModifier(null).selectModifier("");

        assertEquals("SELECT DISTINCT id FROM users", builder.from("users").build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").distinct().selectModifier("   "));
    }

    @Test
    public void testSelectModifierRejectsNonSelectBuildersWithoutChangingThem() {
        final SqlBuilder update = Dsl.PSC.update("users");
        assertThrows(IllegalStateException.class, () -> update.selectModifier("DISTINCT"));
        assertEquals("UPDATE users SET name = ?", update.set("name").build().query());

        final SqlBuilder delete = Dsl.PSC.deleteFrom("users");
        assertThrows(IllegalStateException.class, () -> delete.distinct());
        assertEquals("DELETE FROM users", delete.build().query());
    }

    @Test
    public void testClosedBuilderMutationApisConsistentlyThrowIllegalStateException() {
        final SqlBuilder rawAppend = Dsl.PSC.select("id").from("users");
        rawAppend.build();
        assertThrows(IllegalStateException.class, () -> rawAppend.append("FOR UPDATE"));

        final SqlBuilder modifier = Dsl.PSC.select("id").from("users");
        modifier.build();
        assertThrows(IllegalStateException.class, () -> modifier.selectModifier("DISTINCT"));

        final SqlBuilder condition = Dsl.PSC.select("id").from("users");
        condition.build();
        assertThrows(IllegalStateException.class, () -> condition.append(Filters.eq("id", 1)));

        final SqlBuilder setOperation = Dsl.PSC.select("id").from("users");
        setOperation.build();
        assertThrows(IllegalStateException.class, () -> setOperation.union("SELECT id FROM archived_users"));
    }

    @Test
    public void testRejectedEntityOverloadsPreserveExistingMappingState() {
        final SqlBuilder query = Dsl.PSC.select("id").from(Account.class);
        assertSame(Account.class, query._entityClass);
        assertThrows(IllegalStateException.class, () -> query.from(String.class, "s"));
        assertSame(Account.class, query._entityClass);
        assertEquals("SELECT acc.id AS \"id\" FROM account acc", query.build().query());

        final SqlBuilder update = Dsl.PSC.update("users", Account.class);
        assertSame(Account.class, update._entityClass);
        assertThrows(IllegalStateException.class, () -> update.into(String.class));
        assertSame(Account.class, update._entityClass);
        assertEquals("UPDATE users SET first_name = ?", update.set("firstName").build().query());

        final SqlBuilder joined = Dsl.PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> joined.join((Class<?>) null, "n"));
        assertEquals("SELECT id FROM users", joined.build().query());
    }

    @Test
    public void testSetApisRejectNonUpdateBuildersWithoutCorruptingThem() {
        final SqlBuilder query = Dsl.PSC.select("id").from("users");
        assertThrows(IllegalStateException.class, () -> query.set("name"));
        assertEquals("SELECT id FROM users", query.build().query());

        final SqlBuilder delete = Dsl.PSC.deleteFrom("users");
        assertThrows(IllegalStateException.class, () -> delete.set(Collections.singletonMap("name", "x")));
        assertEquals("DELETE FROM users", delete.build().query());

        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").setEntity(Account.class));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("users").setEntity((Class<?>) null));
    }

    @Test
    public void testLazyDmlInitializationPrecedesClauseReservation() {
        final SqlBuilder incompleteUpdate = Dsl.PSC.update("users");
        assertThrows(IllegalStateException.class, () -> incompleteUpdate.where("id = 1"));
        assertEquals("UPDATE users SET name = ? WHERE id = 1", incompleteUpdate.set("name").where("id = 1").build().query());

        assertEquals("DELETE FROM users ORDER BY id LIMIT 1", Dsl.PSC.deleteFrom("users").orderBy("id").limit(1).build().query());

        final SqlBuilder insert = Dsl.PSC.insert("name").into("users");
        assertThrows(IllegalStateException.class, () -> insert.where("id = 1"));
        assertEquals("INSERT INTO users (name) VALUES (?)", insert.build().query());
    }

    @Test
    public void testOperationSpecificClausesRejectInvalidDmlWithoutChangingTheBuilder() {
        final SqlBuilder update = Dsl.PSC.update("users");
        assertThrows(IllegalStateException.class, () -> update.groupBy("id"));
        assertEquals("UPDATE users SET name = ?", update.set("name").build().query());

        final SqlBuilder deleteWithHaving = Dsl.PSC.deleteFrom("users");
        assertThrows(IllegalStateException.class, () -> deleteWithHaving.having("COUNT(*) > 0"));
        assertEquals("DELETE FROM users", deleteWithHaving.build().query());

        final SqlBuilder deleteWithOffset = Dsl.PSC.deleteFrom("users");
        assertThrows(IllegalStateException.class, () -> deleteWithOffset.offset(1));
        assertEquals("DELETE FROM users", deleteWithOffset.build().query());

        final Dsl sqlServerDsl = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());
        final SqlBuilder sqlServerUpdate = sqlServerDsl.update("users");
        assertThrows(IllegalStateException.class, () -> sqlServerUpdate.limit(1));
        assertEquals("UPDATE users SET name = ?", sqlServerUpdate.set("name").build().query());
    }

    @Test
    public void testSqlServerPaginationRequiresOrderByAndUsesValidOffsetFetchGrammar() {
        final Dsl sqlServerDsl = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());

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

        final SqlBuilder unresolvedFetch = sqlServerDsl.select("id").from("users");
        assertThrows(IllegalStateException.class, () -> unresolvedFetch.append(new Limit("FETCH FIRST ? ROWS ONLY")));
        assertEquals("SELECT id FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY",
                unresolvedFetch.orderBy("id").append(new Limit("FETCH FIRST ? ROWS ONLY")).build().query());

        final Dsl oracleDsl = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Oracle")).build());
        assertEquals("SELECT id FROM users FETCH FIRST 2 ROWS ONLY", oracleDsl.select("id").from("users").limit(2).build().query());

        final Dsl db2Dsl = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("DB2")).build());
        assertEquals("SELECT id FROM users FETCH FIRST 2 ROWS ONLY", db2Dsl.select("id").from("users").limit(2).build().query());
    }

    @Test
    public void testStructuredClauseRenderingFailuresAreAtomic() {
        final SqlBuilder grouped = Dsl.PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> grouped.groupBy("id -- unsafe"));
        assertEquals("SELECT id FROM users GROUP BY id", grouped.groupBy("id").build().query());

        final SqlBuilder ordered = Dsl.PSC.select("id").from("users");
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

        final SqlBuilder filtered = Dsl.PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> filtered.where(unsupported));
        assertEquals("SELECT id FROM users WHERE id = 1", filtered.where("id = 1").build().query());

        final SqlBuilder joined = Dsl.PSC.select("u.id").from("users u").join("accounts a");
        assertThrows(IllegalArgumentException.class, () -> joined.using(Arrays.asList("id", "tenant_id -- unsafe")));
        assertEquals("SELECT u.id FROM users u JOIN accounts a USING (id)", joined.using("id").build().query());

        final SqlBuilder joinedWithCondition = Dsl.PSC.select("u.id").from("users u").join("accounts a");
        assertThrows(IllegalArgumentException.class, () -> joinedWithCondition.on(unsupported));
        assertEquals("SELECT u.id FROM users u JOIN accounts a ON u.id = a.id", joinedWithCondition.on("u.id = a.id").build().query());
    }

    @Test
    public void testSetRenderingFailuresAndLateSetCallsAreAtomic() {
        final SqlBuilder partialSet = Dsl.PSC.update("users");
        assertThrows(IllegalArgumentException.class, () -> partialSet.set(Arrays.asList("name", "age -- unsafe")));
        assertEquals("UPDATE users SET name = ?", partialSet.set("name").build().query());

        final SqlBuilder lateSet = Dsl.PSC.update("users").set("name").where("id = 1");
        assertThrows(IllegalStateException.class, () -> lateSet.set("age"));
        assertEquals("UPDATE users SET name = ? WHERE id = 1", lateSet.build().query());

        final Dsl sqlServerDsl = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());
        final SqlBuilder stagedUpdate = sqlServerDsl.update(Account.class);
        final Collection<String> stagedPropNames = stagedUpdate._propOrColumnNames;
        assertThrows(IllegalArgumentException.class, () -> stagedUpdate.where("#stage.id /* unsafe */ = 1"));
        assertSame(stagedPropNames, stagedUpdate._propOrColumnNames);
        assertTrue(stagedUpdate.where("id = 1").build().query().endsWith(" WHERE id = 1"));
    }

    @Test
    public void testSetEntityGetterAndRendererFailuresRestoreAllBuilderMetadata() {
        final SqlBuilder getterFailure = Dsl.PSC.update("users");
        final Class<?> initialGetterEntityClass = getterFailure._entityClass;
        final Object initialGetterEntityInfo = getterFailure._entityInfo;
        final Object initialGetterColumnMap = getterFailure._propColumnNameMap;
        assertThrows(RuntimeException.class, () -> getterFailure.setEntity(new ThrowingUpdateEntity()));
        assertSame(initialGetterEntityClass, getterFailure._entityClass);
        assertSame(initialGetterEntityInfo, getterFailure._entityInfo);
        assertSame(initialGetterColumnMap, getterFailure._propColumnNameMap);
        assertEquals("UPDATE users SET value = ?", getterFailure.set(Collections.singletonMap("value", "ok")).build().query());

        final boolean[] failFirstRender = { true };
        final Dsl throwingNamedDsl = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
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
        assertThrows(IllegalStateException.class, () -> rendererFailure.setEntity(Account.class));
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
        final SqlBuilder unsafeInsert = Dsl.PSC.insert("firstName", "bad--name");
        assertThrows(IllegalArgumentException.class, () -> unsafeInsert.into("account"));
        assertNull(unsafeInsert._tableName);
        assertEquals(0, unsafeInsert._sb.length());
        assertThrows(IllegalStateException.class, unsafeInsert::build);

        // The NAMED_SQL VALUES path also mutates the named-parameter registries; a failed render must
        // restore them so a retried into() behaves as if the first call never happened.
        final boolean[] failFirstRender = { true };
        final Dsl throwingNamedDsl = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
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
    }

    @Test
    public void testFromRenderingFailuresAreAtomic() {
        // Rendering the staged select list is part of from(); a rejected column must roll back the
        // emitted SELECT prefix so a retried from() cannot emit a second "SELECT ..." fragment.
        final SqlBuilder unsafeSelect = Dsl.PSC.select("name", "a--b");
        assertThrows(IllegalArgumentException.class, () -> unsafeSelect.from("t"));
        assertEquals(0, unsafeSelect._sb.length());
        assertFalse(unsafeSelect._hasFromBeenSet);
        assertNull(unsafeSelect._tableName);

        // A retry fails the same way and still leaves no partial SQL behind (no doubled SELECT).
        assertThrows(IllegalArgumentException.class, () -> unsafeSelect.from("t"));
        assertEquals(0, unsafeSelect._sb.length());
        assertThrows(IllegalStateException.class, unsafeSelect::build);
    }

    @Test
    public void testJoinApisRequireFromAndPrecedeLaterClauses() {
        final SqlBuilder stagedSelect = Dsl.PSC.select("id");
        assertThrows(IllegalStateException.class, () -> stagedSelect.join("orders"));
        assertEquals("SELECT id FROM users", stagedSelect.from("users").build().query());

        final SqlBuilder filteredSelect = Dsl.PSC.select("id").from("users").where(Filters.eq("active", true));
        assertThrows(IllegalStateException.class, () -> filteredSelect.leftJoin("orders"));
        assertEquals("SELECT id FROM users WHERE active = ?", filteredSelect.build().query());

        assertThrows(IllegalStateException.class, () -> Dsl.PSC.update("users").join("orders"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").orderBy("id").innerJoin(Account.class));
    }

    @Test
    public void testStandaloneClausesRequireFromAndPreserveSqlClauseOrder() {
        final SqlBuilder stagedSelect = Dsl.PSC.select("id");
        assertThrows(IllegalStateException.class, () -> stagedSelect.where("active = 1"));
        assertEquals("SELECT id FROM users WHERE active = 1", stagedSelect.from("users").where("active = 1").build().query());

        final SqlBuilder ordered = Dsl.PSC.select("id").from("users").orderBy("id");
        assertThrows(IllegalStateException.class, () -> ordered.where("active = 1"));
        assertEquals("SELECT id FROM users ORDER BY id", ordered.build().query());

        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").groupBy("id"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").having("COUNT(*) > 0").groupBy("id"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").orderBy("id").having("COUNT(*) > 0"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").limit(5).orderBy("id"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").forUpdate().limit(5));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").forUpdate().fetchFirstRows(5));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.update("users").set("active = true").forUpdate());

        // Pagination before the terminal locking clause is valid for the supported LIMIT-style syntax.
        assertEquals("SELECT id FROM users LIMIT 5 FOR UPDATE", Dsl.PSC.select("id").from("users").limit(5).forUpdate().build().query());
    }

    @Test
    public void testTopLevelTableAliasDetectionIgnoresNonAliasSyntaxAndTrailingComments() {
        final SqlBuilder tableFunction = Dsl.PSC.select("firstName").from("unnest (items)", Account.class);
        assertNull(tableFunction._tableAlias);
        assertFalse(tableFunction.build().query().contains("(items).first_name"));

        final SqlBuilder spacedQualification = Dsl.PSC.select("firstName").from("catalog . account", Account.class);
        assertNull(spacedQualification._tableAlias);
        assertFalse(spacedQualification.build().query().contains("account.first_name"));

        final SqlBuilder tableHint = Dsl.PSC.select("firstName").from("account WITH (NOLOCK)", Account.class);
        assertNull(tableHint._tableAlias);
        assertFalse(tableHint.build().query().contains("(NOLOCK).first_name"));

        final SqlBuilder implicitAlias = Dsl.PSC.select("firstName").from("account a /* shard hint */", Account.class);
        assertEquals("a", implicitAlias._tableAlias);
        final String implicitSql = implicitAlias.build().query();
        assertTrue(implicitSql.contains("a.first_name"));
        assertTrue(implicitSql.endsWith("FROM account a /* shard hint */"));

        final SqlBuilder explicitAlias = Dsl.PSC.select("firstName").from("account AS a /* shard hint */", Account.class);
        assertEquals("a", explicitAlias._tableAlias);
        final String explicitSql = explicitAlias.build().query();
        assertTrue(explicitSql.contains("a.first_name"));
        assertTrue(explicitSql.endsWith("FROM account AS a /* shard hint */"));

        final SqlBuilder temporalAlias = Dsl.PSC.select("firstName").from("account FOR SYSTEM_TIME AS OF '2026-01-01' AS a", Account.class);
        assertEquals("a", temporalAlias._tableAlias);
        assertTrue(temporalAlias.build().query().contains("a.first_name"));
    }

    @Test
    public void testTopLevelAliasScannerBackslashEscapesOnlyInSingleQuotedLiterals() {
        // A backslash before the closing quote of a double-quoted or backtick-quoted identifier does NOT
        // escape that quote (backslash escaping applies only inside single-quoted string literals), so the
        // identifier ends at that quote and the trailing token is a real top-level table alias.
        final SqlBuilder doubleQuoted = Dsl.PSC.select("firstName").from("\"a\\\" t", Account.class);
        assertEquals("t", doubleQuoted._tableAlias);
        final String doubleQuotedSql = doubleQuoted.build().query();
        assertTrue(doubleQuotedSql.contains("t.first_name"), doubleQuotedSql);
        assertTrue(doubleQuotedSql.endsWith("FROM \"a\\\" t"), doubleQuotedSql);

        final SqlBuilder backtickQuoted = Dsl.PSC.select("firstName").from("`a\\` t", Account.class);
        assertEquals("t", backtickQuoted._tableAlias);
        assertTrue(backtickQuoted.build().query().contains("t.first_name"));
    }

    @Test
    public void testOnAndUsingRequireCompatibleUnconnectedJoin() {
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").on("users.id = orders.user_id"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").using("id"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").crossJoin("orders").on("users.id = orders.user_id"));
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("id").from("users").naturalJoin("orders").using("id"));

        final SqlBuilder joined = Dsl.PSC.select("id").from("users").join("orders").on("users.id = orders.user_id");
        assertThrows(IllegalStateException.class, () -> joined.using("id"));
        assertEquals("SELECT id FROM users JOIN orders ON users.id = orders.user_id", joined.build().query());
    }

    @Test
    public void testRawJoinWithInlineConnectorClosesConnectorSlot() {
        final SqlBuilder inlineOn = Dsl.PSC.select("*").from("users u").join("orders o ON u.id = o.user_id");
        assertThrows(IllegalStateException.class, () -> inlineOn.on("o.active = 1"));
        assertThrows(IllegalStateException.class, () -> inlineOn.using("id"));
        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", inlineOn.build().query());

        final SqlBuilder inlineUsing = Dsl.PSC.select("*").from("users u").leftJoin("orders o USING (user_id)");
        assertThrows(IllegalStateException.class, () -> inlineUsing.on("u.id = o.user_id"));
        assertEquals("SELECT * FROM users u LEFT JOIN orders o USING (user_id)", inlineUsing.build().query());

        final String nestedJoin = "(SELECT o.id FROM orders o JOIN order_items i ON o.id = i.order_id WHERE o.note = 'USING') nested /* ON USING */";
        assertEquals("SELECT * FROM users u JOIN " + nestedJoin + " ON u.id = nested.id",
                Dsl.PSC.select("*").from("users u").join(nestedJoin).on("u.id = nested.id").build().query());

        assertEquals("SELECT * FROM users u INNER JOIN orders \"ON\" ON u.id = \"ON\".user_id",
                Dsl.PSC.select("*").from("users u").innerJoin("orders \"ON\"").on("u.id = \"ON\".user_id").build().query());
    }

    @Test
    public void testPSCWithWhere() {
        String sql = Dsl.PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testPSCWithMultipleConditions() {
        String sql = Dsl.PSC.select("*").from(Account.class).where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testPSCWithOrderBy() {
        String sql = Dsl.PSC.select("*").from(Account.class).orderBy("firstName").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testPSCWithLimit() {
        String sql = Dsl.PSC.select("*").from(Account.class).limit(10).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testPSCWithJoin() {
        String sql = Dsl.PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("JOIN"));
    }

    @Test
    public void testPSCWithLeftJoin() {
        String sql = Dsl.PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testPSCWithInnerJoin() {
        String sql = Dsl.PSC.select("*").from("users").innerJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
    }

    @Test
    public void testPSCWithRightJoin() {
        String sql = Dsl.PSC.select("*").from("users").rightJoin("orders").on("users.id = orders.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testPSCWithFullJoin() {
        String sql = Dsl.PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testPSCWithCrossJoin() {
        String sql = Dsl.PSC.select("*").from("users").crossJoin("roles").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testPSCWithGroupBy() {
        String sql = Dsl.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("GROUP BY"));
    }

    @Test
    public void testPSCWithHaving() {
        String sql = Dsl.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").having(Filters.expr("COUNT(*) > 5")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("HAVING"));
    }

    @Test
    public void testPSCWithDistinct() {
        String sql = Dsl.PSC.select("status").from(Account.class).distinct().build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testPSCComplexQuery() {
        String sql = Dsl.PSC.select("u.id", "u.firstName", "COUNT(o.id) as order_count")
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
        String sql = Dsl.PSC.insertInto(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testUpdate() {
        String sql = Dsl.PSC.update(Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UPDATE"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFrom() {
        String sql = Dsl.PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectWithAlias() {
        String sql = Dsl.PSC.select("firstName AS fname", "lastName AS lname").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("AS"));
    }

    @Test
    public void testSelectWithMultipleTables() {
        String sql = Dsl.PSC.select("*").from("users", "orders").build().query();
        assertNotNull(sql);
    }

    @Test
    public void testWhereWithOr() {
        String sql = Dsl.PSC.select("*").from(Account.class).where(Filters.eq("status", "active").or(Filters.eq("status", "pending"))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testMultipleOrderBy() {
        String sql = Dsl.PSC.select("*").from(Account.class).orderBy("lastName", "firstName").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testOrderByRejectsCommentToken() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id--").build().query());
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
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("orders").using("\"a\\\" -- x"));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("orders").using("`a\\` -- x"));

        // A comment-like token that is genuinely inside a quoted identifier is still allowed.
        final String sql = Dsl.PSC.select("*").from("users").join("orders").using("\"a--b\"").build().query();
        assertTrue(sql.contains("a--b"));
    }

    @Test
    public void testCommentTokenAfterBackslashEscapedQuoteInsideSingleQuotedLiteralIsRejected() {
        // Inside a single-quoted string literal a backslash DOES escape the following quote (\'), so the next
        // quote closes the string and a trailing comment token (-- or /* */) must be rejected.
        // Regression: the scanner previously misread "\''" as a doubled-quote ('') escape and stayed "inside"
        // the string, hiding the trailing comment token from the guard.
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("note = '\\'' -- x").from("docs").build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("note = '\\'' /* x */").from("docs").build().query());

        // The backslash-escaped-quote literal on its own (no trailing comment) is still accepted.
        final String okSql = Dsl.PSC.select("note = '\\''").from("docs").build().query();
        assertNotNull(okSql);
    }

    @Test
    public void testOrderByRejectsEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy().build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy(Collections.emptyList()).build().query());
    }

    @Test
    public void testOrderByAsc() {
        String sql = Dsl.PSC.select("*").from(Account.class).orderBy("firstName", SortDirection.ASC).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testOrderByDesc() {
        String sql = Dsl.PSC.select("*").from(Account.class).orderBy("createdTime", SortDirection.DESC).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testLimitWithOffset() {
        String sql = Dsl.PSC.select("*").from(Account.class).limit(20, 10).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testFromWithEntityClass() {
        String sql = Dsl.PSC.select("*").from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FROM"));
    }

    @Test
    public void testJoinWithEntityClass() {
        String sql = Dsl.PSC.select("*").from(Account.class).join(Account.class).on("a.id = b.parent_id").build().query();
        assertNotNull(sql);
    }

    @Test
    public void testIntoWithTableName() {
        String sql = Dsl.PSC.insert("id", "name").into("accounts").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO"));
    }

    @Test
    public void testIntoRejectsEmptyTableName() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert("id").into("").build().query());
    }

    @Test
    public void testUpdateWithSet() {
        String sql = Dsl.PSC.update("accounts").set("status", "inactive").set("updated_at", "NOW()").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testDeleteFromWithTable() {
        String sql = Dsl.PSC.deleteFrom("accounts").where(Filters.eq("status", "deleted")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM"));
    }

    @Test
    public void testSelectCount() {
        String sql = Dsl.PSC.select(AbstractQueryBuilder.COUNT_ALL).from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("count(*)"));
    }

    @Test
    public void testSelectAll() {
        String sql = Dsl.PSC.select(AbstractQueryBuilder.ALL).from(Account.class).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testChainedAndOr() {
        String sql = Dsl.PSC.select("*")
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
        String sql = Dsl.PSC.select("*")
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
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").limit(10, 5).offset(2));
    }

    @Test
    public void testAppendLimitConditionWithExpression() {
        String sql = Dsl.PSC.select("*").from("users").append(new Limit("10 OFFSET 20")).build().query();
        assertTrue(sql.endsWith("LIMIT 10 OFFSET 20"));
    }

    @Test
    public void testAppendConditionAfterWhereThrowsDuplicateWhere() {
        assertThrows(IllegalStateException.class,
                () -> Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.eq("name", "Alice")).build().query());
    }

    @Test
    public void testAppendWhereClauseAfterWhereThrows() {
        assertThrows(IllegalStateException.class,
                () -> Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.where(Filters.eq("name", "Alice"))).build().query());
    }

    @Test
    public void testAppendCriteriaAfterWhereThrowsWhenCriteriaHasWhere() {
        Criteria criteria = Criteria.builder().where(Filters.eq("name", "Alice")).build();

        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).append(criteria).build().query());
    }

    @Test
    public void testAppendCriteriaPreflightsJoinAndSetOperationPlacement() {
        final Criteria joinCriteria = Criteria.builder().join("orders", Filters.expr("users.id = orders.user_id")).build();
        final SqlBuilder filtered = Dsl.PSC.select("id").from("users").where(Filters.eq("active", true));

        assertThrows(IllegalStateException.class, () -> filtered.append(joinCriteria));
        assertEquals("SELECT id FROM users WHERE active = ?", filtered.build().query());

        final Criteria unionCriteria = Criteria.builder().union(Filters.subQuery("SELECT id FROM archived_users")).build();
        final SqlBuilder ordered = Dsl.PSC.select("id").from("users").orderBy("id");

        assertThrows(IllegalStateException.class, () -> ordered.append(unionCriteria));
        assertEquals("SELECT id FROM users ORDER BY id", ordered.build().query());
    }

    @Test
    public void testAppendCriteriaPreflightsRelativeClauseOrderWithoutMutation() {
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class, Dsl.PSC.select("department").from("users").groupBy("department"),
                Criteria.builder().where(Filters.eq("active", true)).build(), "SELECT department FROM users GROUP BY department");
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class,
                Dsl.PSC.select("department").from("users").groupBy("department").having("COUNT(*) > 1"), Criteria.builder().groupBy("region").build(),
                "SELECT department FROM users GROUP BY department HAVING COUNT(*) > 1");
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class, Dsl.PSC.select("id").from("users").orderBy("id"),
                Criteria.builder().having(Filters.gt("COUNT(*)", 1)).build(), "SELECT id FROM users ORDER BY id");
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class, Dsl.PSC.select("id").from("users").limit(10),
                Criteria.builder().orderBy("id").build(), "SELECT id FROM users LIMIT 10");
        assertCriteriaRejectedWithoutMutation(IllegalStateException.class, Dsl.PSC.select("id").from("users").forUpdate(), Criteria.builder().limit(10).build(),
                "SELECT id FROM users FOR UPDATE");
    }

    @Test
    public void testAppendCriteriaValidatesEverySetOperationOperandWithoutMutation() {
        final Criteria unsafe = Criteria.builder().union(Filters.subQuery("UPDATE archived_users SET active = false")).build();
        assertCriteriaRejectedWithoutMutation(IllegalArgumentException.class, Dsl.PSC.select("id").from("users"), unsafe, "SELECT id FROM users");

        final Criteria incomplete = Criteria.builder().union(Filters.subQuery("archived_users")).build();
        assertCriteriaRejectedWithoutMutation(IllegalArgumentException.class, Dsl.PSC.select("id").from("users"), incomplete, "SELECT id FROM users");

        final Criteria unsafeSecondOperand = Criteria.builder()
                .union(Filters.subQuery("SELECT id FROM archived_users"))
                .unionAll(Filters.subQuery("SELECT id FROM deleted_users; DELETE FROM deleted_users"))
                .build();
        assertCriteriaRejectedWithoutMutation(IllegalArgumentException.class, Dsl.PSC.select("id").from("users"), unsafeSecondOperand, "SELECT id FROM users");
    }

    @Test
    public void testCompletedCriteriaSetOperationAllowsOnlyCompoundResultClauses() {
        final Criteria union = Criteria.builder().union(Filters.subQuery("SELECT id FROM archived_users")).build();
        final SqlBuilder compound = Dsl.PSC.select("id").from("users").append(union);

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
        final SqlBuilder unsafeOperand = Dsl.PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> unsafeOperand.append(new Union(Filters.subQuery("UPDATE archived_users SET active = false"))));
        assertEquals("SELECT id FROM users", unsafeOperand.build().query());

        // Position rules: a set operator cannot follow ORDER BY (or pagination/FOR UPDATE) ...
        final SqlBuilder ordered = Dsl.PSC.select("id").from("users").orderBy("id");
        assertThrows(IllegalStateException.class, () -> ordered.append(new Union(Filters.subQuery("SELECT id FROM archived_users"))));
        assertEquals("SELECT id FROM users ORDER BY id", ordered.build().query());

        // ... and requires a SELECT segment completed by from(...) on its left-hand side.
        final SqlBuilder staged = Dsl.PSC.select("id");
        assertThrows(IllegalStateException.class, () -> staged.append(new Union(Filters.subQuery("SELECT id FROM archived_users"))));
        assertEquals("SELECT id FROM users", staged.from("users").build().query());

        // A valid standalone Union still renders and completes the set operation: WHERE may no longer
        // follow, exactly as on the other set-operation routes.
        final SqlBuilder compound = Dsl.PSC.select("id").from("users").append(new Union(Filters.subQuery("SELECT id FROM archived_users")));
        assertThrows(IllegalStateException.class, () -> compound.where(Filters.eq("active", true)));
        assertEquals("SELECT id FROM users UNION SELECT id FROM archived_users", compound.build().query());
    }

    @Test
    public void testAppendCriteriaJoinTracksFollowUpOnUsingEligibility() {
        // A criteria join that carries its own ON condition closes the connector slot, even though the
        // preceding standalone join had left it open: a follow-up on() must not emit a second ON.
        final SqlBuilder conditionedJoin = Dsl.PSC.select("u.id")
                .from("users u")
                .join("orders o")
                .append(Criteria.builder().join("payments p", Filters.expr("p.order_id = o.id")).build());
        assertThrows(IllegalStateException.class, () -> conditionedJoin.on("u.id = o.user_id"));
        assertEquals("SELECT u.id FROM users u JOIN orders o JOIN payments p ON p.order_id = o.id", conditionedJoin.build().query());

        // A condition-less criteria join re-opens the slot: a follow-up on() is accepted and renders.
        final String openJoinSql = Dsl.PSC.select("u.id")
                .from("users u")
                .join("orders o")
                .on("u.id = o.user_id")
                .append(Criteria.builder().join("payments p").build())
                .on("p.order_id = o.id")
                .build()
                .query();
        assertEquals("SELECT u.id FROM users u JOIN orders o ON u.id = o.user_id JOIN payments p ON p.order_id = o.id", openJoinSql);

        // A single raw criteria join entity that already supplies its ON inline closes the slot too.
        final SqlBuilder inlineOnJoin = Dsl.PSC.select("u.id").from("users u").append(Criteria.builder().join("payments p ON p.user_id = u.id").build());
        assertThrows(IllegalStateException.class, () -> inlineOnJoin.on("p.status = 'OPEN'"));
        assertEquals("SELECT u.id FROM users u JOIN payments p ON p.user_id = u.id", inlineOnJoin.build().query());

        // A CROSS JOIN never accepts a connector, no matter how it is appended.
        final SqlBuilder crossJoined = Dsl.PSC.select("u.id").from("users u").append(Criteria.builder().join(Filters.crossJoin("payments")).build());
        assertThrows(IllegalStateException.class, () -> crossJoined.on("payments.user_id = u.id"));
        assertEquals("SELECT u.id FROM users u CROSS JOIN payments", crossJoined.build().query());
    }

    @Test
    public void testCollectionAndMapArgumentsRenderTheirValidatedSnapshots() {
        assertEquals("SELECT * FROM users",
                Dsl.PSC.select("*").from(new ChangingCollection<>(Collections.singletonList("users"), Collections.singletonList(" "))).build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from(new NominallyNonEmptyCollection<>()));

        assertEquals("SELECT * FROM users JOIN orders USING (user_id)",
                Dsl.PSC.select("*")
                        .from("users")
                        .join("orders")
                        .using(new ChangingCollection<>(Collections.singletonList("user_id"), Collections.singletonList(" ")))
                        .build()
                        .query());
        assertEquals("SELECT category FROM products GROUP BY category",
                Dsl.PSC.select("category")
                        .from("products")
                        .groupBy(new ChangingCollection<>(Collections.singletonList("category"), Collections.singletonList(" ")))
                        .build()
                        .query());
        assertEquals("SELECT id FROM users ORDER BY id",
                Dsl.PSC.select("id")
                        .from("users")
                        .orderBy(new ChangingCollection<>(Collections.singletonList("id"), Collections.singletonList(" ")))
                        .build()
                        .query());
        assertEquals("SELECT id FROM users UNION SELECT id FROM admins",
                Dsl.PSC.select("id")
                        .from("users")
                        .union(new ChangingCollection<>(Collections.singletonList("id"), Collections.singletonList(" ")))
                        .from("admins")
                        .build()
                        .query());

        assertEquals("SELECT category FROM products GROUP BY category ASC",
                Dsl.PSC.select("category").from("products").groupBy(new ChangingMap<>("category", SortDirection.ASC, " ", SortDirection.DESC)).build().query());
        assertEquals("SELECT id FROM users ORDER BY id DESC",
                Dsl.PSC.select("id").from("users").orderBy(new ChangingMap<>("id", SortDirection.DESC, " ", SortDirection.ASC)).build().query());

        final AbstractQueryBuilder.SP update = Dsl.PSC.update("users").set(new ChangingMap<>("name", "Alice", " ", "corrupted")).build();
        assertEquals("UPDATE users SET name = ?", update.query());
        assertEquals(Collections.singletonList("Alice"), update.parameters());
    }

    @Test
    public void testAppendLimitExpressionAfterLimitThrows() {
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").limit(10).append(new Limit("5")).build().query());
    }

    @Test
    public void testSelectAllowsHashJsonOperators() {
        String sql = Dsl.PSC.select("payload #>> '{meta,status}'").from("docs").build().query();
        assertTrue(sql.contains("#>>"));
    }

    @Test
    public void testSelectAllowsCommentLikeTokenInsideQuotedLiteral() {
        String sql = Dsl.PSC.select("CASE WHEN note = '--literal' THEN 1 ELSE 0 END").from("docs").build().query();
        assertTrue(sql.contains("'--literal'"));
    }

    @Test
    public void testUpdateAllowsIbatisPlaceholderExpression() {
        String sql = Dsl.PSC.update("users").set("name = #{name}").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("#{name}"));
    }

    @Test
    public void testGroupByRejectsEmptyInputs() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy().build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy(Collections.emptyList()).build().query());
    }

    @Test
    public void testWhereRejectsNullStringExpression() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").where((String) null));
    }

    @Test
    public void testClauseBuildersRejectBlankStringFragments() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("orders").on("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").join("orders").using("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").where("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy(Collections.singletonMap("   ", SortDirection.ASC)));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy("id").having("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy(Collections.singletonMap("   ", SortDirection.ASC)));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").append("   "));
    }

    @Test
    public void testInsertAndUpdateRejectBlankTableAndSetFragments() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert("id").into("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("users").set("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("users").set(Arrays.asList("name", "   ")));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("users").set(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("users").set(Collections.emptyMap()));
    }

    @Test
    public void testDslSelectAndInsertRejectBlankColumnFragments() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id", "   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select(Collections.singletonMap("   ", "alias")));

        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert("id", "   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert(Arrays.asList("id", "   ")));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert(Collections.singletonMap("   ", 1)));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert((Object) "   "));

        Map<String, Object> props = Collections.singletonMap("id", 1);
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert((Object) props, Collections.singleton("id")));

        Map<String, Object> blankProps = Collections.singletonMap("   ", 1);
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert((Object) blankProps, Collections.singleton("   ")));

        Map<Object, Object> nonStringProps = Collections.singletonMap(1, "invalid column");
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.insert((Object) nonStringProps));
    }

    @Test
    public void testSetOperationsAndDirectionsRejectInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").union("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").unionAll("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").intersect("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").except("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").minus("   "));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy("id", null));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id", null));
    }

    @Test
    public void testFetchNextRowsAndFetchFirstRowsAreMutuallyExclusive() {
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id").fetchNextRows(10).fetchFirstRows(5).build().query());
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).fetchNextRows(5).build().query());
    }

    @Test
    public void testRowLimitApisRejectNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").limit(-1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").limit(10, -1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").offset(-1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").offsetRows(-1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").fetchNextRows(-1));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").fetchFirstRows(-1));
    }

    @Test
    public void testChainedSetCollectionCallsIncludeComma() {
        String sql = Dsl.PSC.update("users").set("firstName").set("lastName").where(Filters.eq("id", 1)).build().query();
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
        String sql = Dsl.PSC.update("users").set(m1).set(m2).where(Filters.eq("id", 1)).build().query();
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
        String sql = Dsl.PSC.select("*").from("users").where(Filters.expr("status = '##ACTIVE##'")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("##ACTIVE##"), "## inside value must not be rejected as SQL comment");
    }

    @Test
    public void testSetEntityClass() {
        String sql = Dsl.PSC.update("account").setEntity(Account.class).where(Filters.eq("id", 1)).build().query();
        String deprecatedSql = Dsl.PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE account SET"), "setEntity(Class) must render an UPDATE ... SET: " + sql);
        assertTrue(sql.contains("first_name = ?"), "setEntity(Class) must include updatable properties: " + sql);
        assertEquals(deprecatedSql, sql, "deprecated set(Class) must delegate to setEntity(Class)");
    }

    @Test
    public void testSetEntityClassWithExcludedPropNames() {
        Set<String> excluded = Collections.singleton("firstName");
        String sql = Dsl.PSC.update("account").setEntity(Account.class, excluded).where(Filters.eq("id", 1)).build().query();
        String deprecatedSql = Dsl.PSC.update("account").set(Account.class, excluded).where(Filters.eq("id", 1)).build().query();

        assertFalse(sql.contains("first_name = ?"), "excluded property must not be updated: " + sql);
        assertTrue(sql.contains("last_name = ?"), "non-excluded property must be updated: " + sql);
        assertEquals(deprecatedSql, sql, "deprecated set(Class, Set) must delegate to setEntity(Class, Set)");
    }

    @Test
    public void testNamedParameterHandlerOnDialect() {
        final Dsl dsl = Dsl
                .forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, propName) -> sb.append("#{").append(propName).append("}")).build());
        final String sql = dsl.select("name").from("users").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("#{id}"));
        assertTrue(Dsl.NSC.select("name").from("users").where(Filters.eq("id", 1)).build().query().contains(":id"));
    }

    @Test
    public void testNamedParameterHandlerIsScopedAcrossInterleavedBuilders() {
        final Dsl myBatisStyle = Dsl
                .forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, propName) -> sb.append("#{").append(propName).append("}")).build());

        final SqlBuilder customBuilder = myBatisStyle.select("name").from("users");
        final SqlBuilder defaultBuilder = Dsl.NSC.select("name").from("users");

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
        final SqlDialect dialect = Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler(handler).tokenizerConfig(tokenizerConfig).build();
        final SqlDialect copy = dialect.toBuilder().build();

        assertSame(handler, copy.namedParameterHandler());
        assertSame(tokenizerConfig, copy.tokenizerConfig());
        assertEquals(dialect, copy);
        assertEquals(dialect.hashCode(), copy.hashCode());
    }

    @Test
    public void testNullTokenizerConfigUsesImmutableDefault() {
        final SqlBuilder builder = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().tokenizerConfig(null).build()).select("id").from("users");

        assertSame(SqlParser.defaultTokenizerConfig(), builder._tokenizer.tokenizerConfig());
    }

    @Test
    public void testNamedParameterHandlersRemainIsolatedDuringParallelUse() {
        final Dsl customDsl = Dsl
                .forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
        final List<String> statements = IntStream.range(0, 128)
                .parallel()
                .mapToObj(i -> (i & 1) == 0 ? customDsl.select("id").from("users").where(Filters.eq("id", i)).build().query()
                        : Dsl.NSC.select("id").from("users").where(Filters.eq("id", i)).build().query())
                .toList();

        for (int i = 0; i < statements.size(); i++) {
            assertTrue(statements.get(i).endsWith((i & 1) == 0 ? "${id}" : ":id"));
        }
    }

    @Test
    public void testSelectModifier() {
        final String sql = Dsl.PSC.select("*").selectModifier("TOP 5").from("users").build().query();

        assertTrue(sql.contains("SELECT TOP 5"));
    }

    @Test
    public void testNaturalJoin_String() {
        final String sql = Dsl.PSC.select("*").from("users").naturalJoin("orders").build().query();

        assertTrue(sql.contains("NATURAL JOIN orders"));
    }

    @Test
    public void testNaturalJoin_EntityClass() {
        final String sql = Dsl.PSC.select("*").from(Account.class).naturalJoin(Account.class).build().query();

        assertTrue(sql.contains("NATURAL JOIN"));
        assertTrue(sql.toLowerCase().contains("account"));
    }

    @Test
    public void testNaturalJoin_EntityClassAlias() {
        final String sql = Dsl.PSC.select("*").from(Account.class, "a").naturalJoin(Account.class, "b").build().query();

        assertTrue(sql.contains("NATURAL JOIN"));
        assertTrue(sql.contains(" b"));
    }

    @Test
    public void testUsing() {
        final String sql = Dsl.PSC.select("*").from("users").join("orders").using("user_id").build().query();

        assertTrue(sql.contains("USING (user_id)"));
    }

    @Test
    public void testOffsetRows() {
        final String sql = Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(20).build().query();

        assertTrue(sql.contains("OFFSET 20 ROWS"));
    }

    @Test
    public void testFetchNextRows() {
        final String sql = Dsl.PSC.select("*").from("users").orderBy("id").offsetRows(0).fetchNextRows(10).build().query();

        assertTrue(sql.contains("FETCH NEXT 10 ROWS ONLY"));
    }

    @Test
    public void testFetchFirstRows() {
        final String sql = Dsl.PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build().query();

        assertTrue(sql.contains("FETCH FIRST 10 ROWS ONLY"));
    }

    @Test
    public void testAppendIf_Condition() {
        final String withCondition = Dsl.PSC.select("*").from("users").appendIf(true, Filters.eq("status", "ACTIVE")).build().query();
        final String withoutCondition = Dsl.PSC.select("*").from("users").appendIf(false, Filters.eq("status", "ACTIVE")).build().query();

        assertTrue(withCondition.contains("status"));
        assertTrue(!withoutCondition.contains("status"));
    }

    @Test
    public void testAppendIf_String() {
        final String withExpression = Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(true, " FOR UPDATE").build().query();
        final String withoutExpression = Dsl.PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(false, " FOR UPDATE").build().query();

        assertTrue(withExpression.contains("FOR UPDATE"));
        assertTrue(!withoutExpression.contains("FOR UPDATE"));
    }

    @Test
    public void testAppendIfOrElse_Condition() {
        final AbstractQueryBuilder.SP trueBranch = Dsl.PSC.select("*")
                .from("users")
                .appendIfOrElse(true, Filters.eq("status", "ACTIVE"), Filters.eq("status", "INACTIVE"))
                .build();
        final AbstractQueryBuilder.SP falseBranch = Dsl.PSC.select("*")
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
        final String asc = Dsl.PSC.select("*").from("users").appendIfOrElse(true, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
        final String desc = Dsl.PSC.select("*").from("users").appendIfOrElse(false, " ORDER BY name ASC", " ORDER BY name DESC").build().query();

        assertTrue(asc.contains("ORDER BY name ASC"));
        assertTrue(desc.contains("ORDER BY name DESC"));
    }

    @Test
    public void testUnion_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .union(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("UNION"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testUnion_Query() {
        final String sql = Dsl.PSC.select("id").from("users").union("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("UNION SELECT id FROM admins"));
    }

    @Test
    public void testUnion_SingleNonSubQueryStringRejected() {
        // union(String) is reserved for a complete SELECT sub-query; a bare column name is rejected up front
        // instead of silently becoming a column list that fails later with an unrelated "from() must be called" error.
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").union("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testUnion_ParenthesizedFromLessQueryAccepted() {
        // "UNION (SELECT 1)" is valid SQL: a FROM-less query wrapped in balanced parentheses is as
        // complete an operand as the already-accepted unparenthesized union("SELECT 1").
        assertEquals("SELECT id FROM users UNION (SELECT 1)", Dsl.PSC.select("id").from("users").union("(SELECT 1)").build().query());
        assertEquals("SELECT id FROM users UNION ((SELECT 1))", Dsl.PSC.select("id").from("users").union("((SELECT 1))").build().query());

        // All five set operations (union/unionAll/intersect/except/minus) share the same operand check
        // (checkSetOperationSubQuery -> isSubQuery), so a second operation pins the shared path.
        assertEquals("SELECT id FROM users INTERSECT (SELECT id FROM t)",
                Dsl.PSC.select("id").from("users").intersect("(SELECT id FROM t)").build().query());
    }

    @Test
    public void testUnion_ParenthesizedNonSelectStillRejected() {
        // A parenthesized column list, an unbalanced non-SELECT fragment, and an unbalanced
        // parenthesized SELECT all remain rejected with the pointed message.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").union("(id, name)"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));

        ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").union("(name"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));

        ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").union("(SELECT 1"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testUnion_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").union(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("UNION SELECT id FROM admins"));
    }

    @Test
    public void testUnionAll_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .unionAll(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("UNION ALL"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testUnionAll_Query() {
        final String sql = Dsl.PSC.select("id").from("users").unionAll("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
    }

    @Test
    public void testUnionAll_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").unionAll("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testUnionAll_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").unionAll(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
    }

    @Test
    public void testIntersect_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .intersect(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("INTERSECT"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testIntersect_Query() {
        final String sql = Dsl.PSC.select("id").from("users").intersect("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
    }

    @Test
    public void testIntersect_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").intersect("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testIntersect_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").intersect(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
    }

    @Test
    public void testExcept_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .except(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("EXCEPT"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testExcept_Query() {
        final String sql = Dsl.PSC.select("id").from("users").except("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
    }

    @Test
    public void testExcept_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").except("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testExcept_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").except(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
    }

    @Test
    public void testMinus_SqlBuilder() {
        final AbstractQueryBuilder.SP sp = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("type", "USER"))
                .minus(Dsl.PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                .build();

        assertTrue(sp.query().contains("MINUS"));
        assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
    }

    @Test
    public void testMinus_Query() {
        final String sql = Dsl.PSC.select("id").from("users").minus("SELECT id FROM admins").build().query();

        assertTrue(sql.contains("MINUS SELECT id FROM admins"));
    }

    @Test
    public void testMinus_SingleNonSubQueryStringRejected() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("id").from("users").minus("id"));
        assertTrue(ex.getMessage().contains("SELECT sub-query"));
    }

    @Test
    public void testMinus_Collection() {
        final String sql = Dsl.PSC.select("id").from("users").minus(Collections.singletonList("id")).from("admins").build().query();

        assertTrue(sql.contains("MINUS SELECT id FROM admins"));
    }

    @Test
    public void testForUpdate() {
        final String sql = Dsl.PSC.select("*").from("users").forUpdate().build().query();

        assertTrue(sql.contains("FOR UPDATE"));
    }

    @Test
    public void testForUpdate_idempotencyGuard() {
        // Calling forUpdate() twice must not produce "FOR UPDATE FOR UPDATE".
        assertThrows(IllegalStateException.class, () -> Dsl.PSC.select("*").from("users").forUpdate().forUpdate());
    }

    @Test
    public void testSetEntity_equivalentToDeprecatedSet() {
        final Account a = new Account();
        a.setFirstName("F");
        a.setLastName("L");

        final String viaSetEntity = Dsl.PSC.update("account").setEntity(a).where(Filters.eq("id", 1)).build().query();
        final String viaSet = Dsl.PSC.update("account").set(a).where(Filters.eq("id", 1)).build().query();

        assertEquals(viaSet, viaSetEntity);
        assertTrue(viaSetEntity.contains("first_name = ?"));
    }

    @Test
    public void testSetEntity_excludedPropNames() {
        final Account a = new Account();
        a.setFirstName("F");
        a.setLastName("L");

        final Set<String> excluded = java.util.Set.of("lastName");
        final String sql = Dsl.PSC.update("account").setEntity(a, excluded).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("first_name = ?"));
        assertFalse(sql.contains("last_name = ?"));
    }

    @Test
    public void testSetEntity_rejectsCollection() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("account").setEntity(Arrays.asList("firstName", "lastName")));
    }

    @Test
    public void testSetEntity_rejectsArray() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("account").setEntity(new String[] { "firstName", "lastName" }));
    }

    @Test
    public void testIntoClass_alwaysSetsEntityClass() {
        // into(Class) should map property names to columns (entity class always set), matching from(...).
        final String sql = Dsl.PSC.insert("firstName", "lastName").into(Account.class).build().query();

        assertTrue(sql.contains("first_name"));
        assertTrue(sql.contains("last_name"));
    }

    @Test
    public void testUsing_varargs() {
        final String sql = Dsl.PSC.select("*").from("orders").join("order_items").using("order_id", "tenant_id").build().query();

        assertTrue(sql.contains("USING (order_id, tenant_id)"), sql);
    }

    @Test
    public void testUsing_collection() {
        final String sql = Dsl.PSC.select("*").from("orders").join("order_items").using(Arrays.asList("order_id", "tenant_id")).build().query();

        assertTrue(sql.contains("USING (order_id, tenant_id)"), sql);
    }

    @Test
    public void testOn_varargsComposite() {
        final String sql = Dsl.PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id", "u.tenant_id = o.tenant_id").build().query();

        assertTrue(sql.contains("ON u.id = o.user_id AND u.tenant_id = o.tenant_id"), sql);
    }

    @Test
    public void testGroupBy_mapIterationOrder() {
        final Map<String, SortDirection> groupings = new LinkedHashMap<>();
        groupings.put("category", SortDirection.ASC);
        groupings.put("brand", SortDirection.DESC);

        final String sql = Dsl.PSC.select("category", "brand").from("products").groupBy(groupings).build().query();

        assertTrue(sql.indexOf("category ASC") < sql.indexOf("brand DESC"), sql);
    }

    @Test
    public void testOrderBy_mapIterationOrder() {
        final Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("lastName", SortDirection.ASC);
        orders.put("firstName", SortDirection.DESC);

        final String sql = Dsl.PSC.select("*").from("users").orderBy(orders).build().query();

        assertTrue(sql.indexOf("ASC") < sql.indexOf("DESC"), sql);
    }

    @Test
    public void testApply_SPFunction() throws Exception {
        final List<Object> result = Dsl.PSC.select("id")
                .from("users")
                .where(Filters.eq("id", 1))
                .apply(sp -> Arrays.asList(sp.query(), sp.parameters().size()));

        assertTrue(result.get(0).toString().contains("WHERE"));
        assertEquals(1, result.get(1));
    }

    @Test
    public void testApply_SqlAndParams() throws Exception {
        final String result = Dsl.PSC.select("id").from("users").where(Filters.eq("id", 1)).apply((sql, params) -> sql + " / " + params.size());

        assertTrue(result.contains("WHERE"));
        assertTrue(result.endsWith("/ 1"));
    }

    @Test
    public void testAccept_SPConsumer() throws Exception {
        final String[] sqlHolder = new String[1];
        final int[] paramCount = new int[1];

        Dsl.PSC.select("id").from("users").where(Filters.eq("id", 1)).accept(sp -> {
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

        Dsl.PSC.select("id").from("users").where(Filters.eq("id", 1)).accept((sql, params) -> {
            sqlHolder[0] = sql;
            paramCount[0] = params.size();
        });

        assertTrue(sqlHolder[0].contains("WHERE"));
        assertEquals(1, paramCount[0]);
    }

    // Cover select-into and entity-class join overloads that inject aliases directly.
    @Test
    public void testSelectIntoFromEntityClass() {
        final String sql = Dsl.PSC.select("id", "firstName").into("account_archive").from(Account.class).build().query();

        assertTrue(sql.startsWith("INSERT INTO account_archive"));
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM account acc"));
    }

    @Test
    public void testEntityJoinOverloadsWithAlias() {
        final String innerJoinSql = Dsl.PSC.select("*").from(Account.class, "a").innerJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String leftJoinSql = Dsl.PSC.select("*").from(Account.class, "a").leftJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String rightJoinSql = Dsl.PSC.select("*").from(Account.class, "a").rightJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String fullJoinSql = Dsl.PSC.select("*").from(Account.class, "a").fullJoin(Account.class, "a2").on("a.id = a2.id").build().query();
        final String crossJoinSql = Dsl.PSC.select("*").from(Account.class, "a").crossJoin(Account.class, "a2").build().query();

        assertTrue(innerJoinSql.contains("INNER JOIN account a2"));
        assertTrue(leftJoinSql.contains("LEFT JOIN account a2"));
        assertTrue(rightJoinSql.contains("RIGHT JOIN account a2"));
        assertTrue(fullJoinSql.contains("FULL JOIN account a2"));
        assertTrue(crossJoinSql.contains("CROSS JOIN account a2"));
    }

    @Test
    public void testOrderByRejectsBlockAndHashCommentTokens() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id/*comment*/").build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").orderBy("id#comment").build().query());
    }

    @Test
    public void testPrintln() {
        final PrintStream originalOut = System.out;
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output));
            Dsl.PSC.select("id").from("users").debugPrint();
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
        String sql = Dsl.PSC.update("account").set((Object) "firstName").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("SET"));
        assertTrue(sql.contains("first_name = ?"));
    }

    @Test
    public void testSet_ObjectMapHonorsExcludedProperties() {
        java.util.Map<String, Object> props = new java.util.LinkedHashMap<>();
        props.put("firstName", "John");
        props.put("lastName", "Doe");

        String sql = Dsl.PSC.update("account").set(props, Collections.singleton("lastName")).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("first_name = ?"));
        assertTrue(!sql.contains("last_name = ?"));
    }

    @Test
    public void testInsertEntity_SkipsZeroIdAndNullProperties() {
        Account account = new Account();
        account.setId(0);
        account.setFirstName("John");
        account.setLastName(null);

        String sql = Dsl.PSC.insert(account).into("account").build().query();

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

        String sql = Dsl.PSC.insert(entity).into("frac_tbl").build().query();

        assertTrue(sql.contains("my_name"), "my_name should be included: " + sql);
        assertTrue(sql.contains("my_key"), "Fractional BigDecimal id 0.5 must not be skipped as default: " + sql);
    }

    @Test
    public void testFix_insertEntity_keepsDefaultCompositeIdPartWhenAnotherIdAssigned() {
        CompositeIdEntity entity = new CompositeIdEntity();
        entity.setTenantId(7);
        entity.setLocalId(0);
        entity.setName("Alice");

        AbstractQueryBuilder.SP sp = Dsl.PSC.insert(entity).into("composite_id_tbl").build();
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

        AbstractQueryBuilder.SP sp = Dsl.PSC.batchInsert(Arrays.asList(first, second)).into("composite_id_tbl").build();

        assertEquals("INSERT INTO composite_id_tbl (tenant_id, local_id, name) VALUES (?, ?, ?), (?, ?, ?)", sp.query());
        assertEquals(Arrays.asList(7L, 0L, "Alice", 8L, 0L, "Bob"), sp.parameters());
    }

    @Test
    public void testFix_setColumnNamesNamedSqlDeduplicatesPlaceholders() {
        AbstractQueryBuilder.SP sp = Dsl.NSC.update("users").set("status").where(Filters.eq("status", "OLD")).build();

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

        String sql = Dsl.PSC.batchInsert(java.util.Arrays.asList(e1, e2)).into("frac_tbl").build().query();

        assertTrue(sql.contains("my_name"), "my_name should be included: " + sql);
        assertTrue(sql.contains("my_key"), "Fractional BigDecimal ids must not be removed as all-zero: " + sql);
    }

    // Bug fix: set(Object entity), insert(entity), and batchInsert(entities) used HashMap internally,
    // which made the resulting SET / INSERT column order depend on hash codes rather than the
    // declared property order. The fix swaps to LinkedHashMap so column order is deterministic.

    @Test
    public void testFix_setEntity_preservesPropertyOrder() {
        Account a = new Account();
        a.setGUI("g");
        a.setEmailAddress("e@e.com");
        a.setFirstName("F");
        a.setMiddleName("M");
        a.setLastName("L");
        a.setStatus(1);

        String sql = Dsl.PSC.update("account").set(a).where(Filters.eq("id", 1)).build().query();

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

        String sql = Dsl.PSC.insert(a).into("account").build().query();

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

        String sql = Dsl.PSC.batchInsert(java.util.Arrays.asList(a1, a2)).into("account").build().query();

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
    public void testSetEntityNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("account").set((Object) null));
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.update("account").set((Object) null, null));
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
        String sql = Dsl.PSC.selectFrom(Account.class, true).build().query();
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
            String sql = Dsl.PSC.select("id").from(Account.class).where(Filters.eq("id", i)).build().query();
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
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").where((Condition) null).build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").groupBy("id").having((Condition) null).build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users u").join("orders o").on((Condition) null).build().query());
        assertThrows(IllegalArgumentException.class, () -> Dsl.PSC.select("*").from("users").append((Condition) null).build().query());
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

        final String sql = Dsl.PSC.select(selections).from("profile a, account o").where(Filters.eq("a.fullName", "X")).build().query();

        assertTrue(sql.contains("WHERE a.f_name = ?"), "aliased @Column name must resolve via the main-table registration: " + sql);
        assertFalse(sql.contains("a.full_name"), "naming-policy fallback indicates the main-table alias registration was dropped: " + sql);
    }

    @Test
    public void testFromVarargsMultiTableKeepsInlineAliasesFromFirstElement() {
        // Mirrors the single-string comma form: from("users u, orders o"). A bare two-String call from
        // this package binds to the protected from(tableName, fromClause) overload instead of the public
        // varargs, so an explicit array is required to exercise the path production callers get.
        assertEquals("SELECT * FROM users u, orders o", Dsl.PSC.select("*").from(new String[] { "users u", "orders o" }).build().query());

        // The first element carries the primary table alias used for entity-property resolution.
        final SqlBuilder aliased = Dsl.PSC.select(Account.class).from(new String[] { "account u", "orders o" });
        assertEquals("u", aliased._tableAlias);
        final String sql = aliased.build().query();
        assertTrue(sql.contains("u.first_name"), "properties must resolve against the first element's inline alias: " + sql);
        assertTrue(sql.endsWith("FROM account u, orders o"), sql);
    }
}
