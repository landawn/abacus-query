package com.landawn.abacus.query;

import static com.landawn.abacus.query.Dsl.MSC;
import static com.landawn.abacus.query.Dsl.NSB;
import static com.landawn.abacus.query.Dsl.NSC;
import static com.landawn.abacus.query.Dsl.PAC;
import static com.landawn.abacus.query.Dsl.PLC;
import static com.landawn.abacus.query.Dsl.PSB;
import static com.landawn.abacus.query.Dsl.PSC;
import static com.landawn.abacus.query.Dsl.SCSB;
import static com.landawn.abacus.query.QueryTestSupport.assertSql;
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
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.SqlDialect.IdentifierQuote;
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

public class AbstractQueryBuilderTest extends TestBase {

    @BeforeEach
    void setUp() {
        // No setup needed for constant testing
    }

    @Nested
    class Deletes {
        @Test
        void testDeleteFrom() {
            String sql = PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        void testDeleteFromWithTable() {
            String sql = PSC.deleteFrom("accounts").where(Filters.eq("status", "deleted")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM"));
        }
    }

    @Nested
    class Inserts {
        @Test
        void testBatchInsertKeepsDefaultCompositeIdPartWhenAnotherIdIsAssigned() {
            CompositeIdEntity first = new CompositeIdEntity().setTenantId(7).setLocalId(0).setName("Alice");
            CompositeIdEntity second = new CompositeIdEntity().setTenantId(8).setLocalId(0).setName("Bob");

            AbstractQueryBuilder.SP sp = PSC.batchInsert(Arrays.asList(first, second)).into("composite_id_tbl").build();

            assertEquals("INSERT INTO composite_id_tbl (tenant_id, local_id, name) VALUES (?, ?, ?), (?, ?, ?)", sp.query());
            assertEquals(Arrays.asList(7L, 0L, "Alice", 8L, 0L, "Bob"), sp.parameters());
        }

        @Test
        void testBatchInsertOmitsCompositeIdsWhenEveryComponentIsDefault() {
            final CompositeIdEntity first = new CompositeIdEntity().setName("Alice");
            final CompositeIdEntity second = new CompositeIdEntity().setName("Bob");
            assertSql("INSERT INTO people (name) VALUES (?), (?)", List.of("Alice", "Bob"), PSC.batchInsert(List.of(first, second)).into("people").build());
            assertSql("INSERT INTO people (name) VALUES (?)", List.of("Alice"), PSC.insert(first).into("people").build());
        }

        @Test
        void testBatchInsertRejectsNonStringMapKeysBeforeAllocatingBuilder() {
            final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> PSC.batchInsert(List.of(Map.of(7, "value"))));
            assertEquals("All keys in batch INSERT maps must be String: 7", error.getMessage());
        }

        @Test
        void testDslSelectAndInsertRejectBlankColumnFragments() {
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
            assertThrows(IllegalArgumentException.class, () -> PSC.insert(props, Collections.singleton("id")));

            Map<String, Object> blankProps = Collections.singletonMap("   ", 1);
            assertThrows(IllegalArgumentException.class, () -> PSC.insert(blankProps, Collections.singleton("   ")));

            Map<Object, Object> nonStringProps = Collections.singletonMap(1, "invalid column");
            assertThrows(IllegalArgumentException.class, () -> PSC.insert(nonStringProps));
        }

        @Test
        void testFix_batchInsertEntities_doesNotSkipFractionalBigDecimalIdAsZero() {
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

        @Test
        void testFix_batchInsertEntities_preservesPropertyOrder() {
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

        @Test
        void testInsertAndUpdateRejectBlankTableAndSetFragments() {
            assertThrows(IllegalArgumentException.class, () -> PSC.insert("id").into("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set(Arrays.asList("name", "   ")));
            assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set(Collections.emptyList()));
            assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set(Collections.emptyMap()));
        }

        @Test
        void testInsertEntity_SkipsZeroIdAndNullProperties() {
            Account account = new Account();
            account.setId(0);
            account.setFirstName("John");
            account.setLastName(null);

            String sql = PSC.insert(account).into("account").build().query();

            assertTrue(sql.contains("first_name"));
            assertTrue(!sql.contains("last_name"));
            assertTrue(!sql.contains("id"));
        }

        @Test
        void testInsertInto() {
            String sql = PSC.insertInto(Account.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
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
        void testInsertNamedPlaceholdersAreSanitizedAndDeduplicated() {
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

        // Regression: a sole-wildcard INSERT ... SELECT omits the target column list, so into() no longer validated the
        // wildcard item and its comment token was only rejected later, by from().
        @Test
        void testInsertSelectSoleWildcardRejectsCommentTokenAtInto() {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.select("/* c */ *").into("bk"));
            assertEquals("SQL comment token is not allowed in column expression: /* c */ *", e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("t.* -- all").into("bk"));

            // Other sole-wildcard shapes, and a quoted "*" (a column named *, not a wildcard).
            assertEquals("INSERT INTO bk SELECT DISTINCT * FROM t", PSC.select("DISTINCT *").into("bk").from("t").build().query());
            assertEquals("INSERT INTO bk (\"*\") SELECT \"*\" FROM t", PSC.select("\"*\"").into("bk").from("t").build().query());
            assertEquals("INSERT INTO bk SELECT `t`.* FROM t", PSC.select("`t`.*").into("bk").from("t").build().query());
            assertEquals("INSERT INTO bk SELECT [t].* FROM t", PSC.select("[t].*").into("bk").from("t").build().query());
        }

        // Regression: select("*").into(t).from(s) rendered the invalid target column list "INSERT INTO t (*) SELECT * FROM s".
        @Test
        void testInsertSelectWildcardOmitsTargetColumnList() {
            assertEquals("INSERT INTO account_backup SELECT * FROM account", PSC.select("*").into("account_backup").from("account").build().query());
            assertEquals("INSERT INTO account_backup SELECT a.* FROM account a", PSC.select("a.*").into("account_backup").from("account a").build().query());
            assertEquals("INSERT INTO account_backup SELECT * FROM account",
                    PSC.select("*").into("account_backup", Account.class).from("account").build().query());

            // A wildcard mixed with other select items cannot be mapped to target columns.
            assertThrows(IllegalArgumentException.class, () -> PSC.select("a.*", "b").into("account_backup"));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("b, a.*").into("account_backup"));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*", "*").into("account_backup"));

            // Non-wildcard projections keep their target column list; count(*) and quoted/parenthesized commas are not wildcards.
            assertEquals("INSERT INTO account_backup (id, name) SELECT id, name FROM account",
                    PSC.select("id", "name").into("account_backup").from("account").build().query());
            assertEquals("INSERT INTO stats (count(*)) SELECT count(*) FROM account", PSC.select("count(*)").into("stats").from("account").build().query());
        }
    }

    @Nested
    class InsertExtensionHooks {
        @Test
        void testAppendInsertPropertiesPreservesMapOrderInsideAtomicMutation() {
            final Map<String, Object> first = new LinkedHashMap<>();
            first.put("firstName", "Alice");
            first.put("lastName", "Smith");
            final Map<String, Object> second = new LinkedHashMap<>();
            second.put("firstName", "Bob");
            second.put("lastName", "Jones");
            final SqlBuilder builder = PSC.insert(first).into("people");
            assertSame(builder, builder.mutateAtomically(() -> {
                builder._sb.append(", (");
                builder.appendInsertProps(second);
                builder._sb.append(')');
            }));
            assertSql("INSERT INTO people (first_name, last_name) VALUES (?, ?), (?, ?)", List.of("Alice", "Smith", "Bob", "Jones"), builder.build());
        }

        @Test
        void testParseInsertBeanPreservesAssignedCompositeIdsAndSnapshotsValues() {
            final CompositeIdEntity entity = new CompositeIdEntity().setTenantId(7).setLocalId(0).setName("Alice");
            final SqlBuilder builder = PSC.insert(Map.of("placeholder", 0));
            AbstractQueryBuilder.parseInsertEntity(builder, entity, null);
            entity.setTenantId(9).setName("changed");
            assertSql("INSERT INTO people (tenant_id, local_id, name) VALUES (?, ?, ?)", List.of(7L, 0L, "Alice"), builder.into("people").build());
        }

        @Test
        void testParseInsertBeanSkipsDefaultIdsAndExcludedProperties() {
            final CompositeIdEntity entity = new CompositeIdEntity().setName("Alice");
            final SqlBuilder builder = PSC.insert(Map.of("placeholder", 0));
            AbstractQueryBuilder.parseInsertEntity(builder, entity, Set.of("localId"));
            assertSql("INSERT INTO people (name) VALUES (?)", List.of("Alice"), builder.into("people").build());

            final CompositeIdEntity assigned = new CompositeIdEntity().setLocalId(8).setName(null);
            final SqlBuilder assignedBuilder = PSC.insert(Map.of("placeholder", 0));
            AbstractQueryBuilder.parseInsertEntity(assignedBuilder, assigned, Set.of());
            assertSql("INSERT INTO people (tenant_id, local_id) VALUES (?, ?)", List.of(0L, 8L), assignedBuilder.into("people").build());
        }

        @Test
        void testParseInsertMapCopiesBeforeApplyingExclusions() {
            final Map<String, Object> props = new LinkedHashMap<>();
            props.put("firstName", "Alice");
            props.put("lastName", "Smith");
            final SqlBuilder builder = PSC.insert(Map.of("placeholder", 0));
            AbstractQueryBuilder.parseInsertEntity(builder, props, Set.of("lastName"));
            assertEquals(Map.of("firstName", "Alice", "lastName", "Smith"), props);
            props.put("firstName", "changed");
            assertSql("INSERT INTO people (first_name) VALUES (?)", List.of("Alice"), builder.into("people").build());
        }

        @Test
        void testParseInsertRejectsInvalidMapsWithoutReplacingValues() {
            final SqlBuilder builder = PSC.insert(Map.of("name", "Alice"));
            try {
                assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.parseInsertEntity(builder, Map.of("name", "Bob"), Set.of("name")));
                assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.parseInsertEntity(builder, Map.of(" ", "Bob"), null));
                assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.parseInsertEntity(builder, Map.of(), null));
                assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.parseInsertEntity(builder, " ", null));
            } finally {
                assertSql("INSERT INTO people (name) VALUES (?)", List.of("Alice"), builder.into("people").build());
            }
        }

        @Test
        void testParseInsertStringSelectsOneColumn() {
            final SqlBuilder builder = PSC.insert("placeholder");
            AbstractQueryBuilder.parseInsertEntity(builder, "firstName", null);
            assertSql("INSERT INTO people (first_name) VALUES (?)", List.of(), builder.into("people").build());
            assertSql("INSERT INTO people (first_name) VALUES (?)", List.of(), PSC.insert((Object) "firstName").into("people").build());
        }
    }

    @Nested
    class Joins {
        @Test
        void testAppendCriteriaJoinTracksFollowUpOnUsingEligibility() {
            // An open qualified JOIN must be completed before a Criteria join may follow.
            final SqlBuilder openJoin = PSC.select("u.id").from("users u").join("orders o");
            assertThrows(IllegalStateException.class, () -> openJoin.append(Criteria.builder().join("payments p", Filters.expr("p.order_id = o.id")).build()));

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
        void testConnectorAfterCompletedSetOperandDoesNotMutateSql() {
            final SqlBuilder builder = PSC.select("id").from("people").union("SELECT id FROM archived_people");
            assertTrue(assertThrows(IllegalStateException.class, () -> builder.on("a.id = b.id")).getMessage().contains("completed set-operation operand"));
            assertSql("SELECT id FROM people UNION SELECT id FROM archived_people", List.of(), builder.build());
        }

        @Test
        void testCriteriaJoinEntityWithInlineConnectorIsRejected() {
            final SqlBuilder builder = PSC.select("*").from("users u");
            assertThrows(IllegalArgumentException.class,
                    () -> builder.append(Criteria.builder().join(new com.landawn.abacus.query.condition.CrossJoin("orders o ON 1=1")).build()));
            assertThrows(IllegalArgumentException.class,
                    () -> builder.append(Criteria.builder().join("orders o ON a = b", Filters.expr("u.id = o.uid")).build()));

            assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.uid",
                    builder.append(Criteria.builder().join("orders o", Filters.expr("u.id = o.uid")).build()).build().query());
        }

        @Test
        void testCrossAndNaturalJoinRejectOnlyTopLevelConnectorsWithoutMutation() {
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

        @Test
        void testEntityJoinAliasRejectsLineBreakAndCommentTokens() {
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
        void testEntityJoinOverloadsUseDeclaredTableAliases() {
            final Map<String, UnaryOperator<SqlBuilder>> joins = new LinkedHashMap<>();
            joins.put("INNER JOIN", builder -> builder.innerJoin(QueryTestSupport.Order.class));
            joins.put("LEFT JOIN", builder -> builder.leftJoin(QueryTestSupport.Order.class));
            joins.put("RIGHT JOIN", builder -> builder.rightJoin(QueryTestSupport.Order.class));
            joins.put("FULL JOIN", builder -> builder.fullJoin(QueryTestSupport.Order.class));
            for (final var join : joins.entrySet()) {
                assertSql("SELECT * FROM users u " + join.getKey() + " user_order o ON u.id = o.user_id", List.of(),
                        join.getValue().apply(PSC.select("*").from("users u")).on("u.id = o.userId").build());
            }
            assertSql("SELECT * FROM users u CROSS JOIN user_order o", List.of(),
                    PSC.select("*").from("users u").crossJoin(QueryTestSupport.Order.class).build());
        }

        @Test
        void testEntityJoinOverloadsWithAlias() {
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
        void testEveryStringJoinRejectsUnterminatedBlockCommentAfterConnectorCheck() {
            final String message = "joinExpr must not contain an unterminated block comment: t /* x";
            assertEquals(message, assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").innerJoin("t /* x")).getMessage());
            assertEquals(message, assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").leftJoin("t /* x")).getMessage());
            assertEquals(message, assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").rightJoin("t /* x")).getMessage());
            assertEquals(message, assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").fullJoin("t /* x")).getMessage());
            assertEquals(message, assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").crossJoin("t /* x")).getMessage());
            assertEquals(message, assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").naturalJoin("t /* x")).getMessage());

            // An expression with both defects keeps the earlier top-level-connector message; a connector hidden inside the
            // open comment is not top-level, so that input reports the comment.
            final String connectorMessage = "CROSS JOIN and NATURAL JOIN expressions must not contain a top-level ON or USING connector";
            assertEquals(connectorMessage,
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").crossJoin("t ON a = b /* x")).getMessage());
            assertEquals(connectorMessage,
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").naturalJoin("t USING (id) /* x")).getMessage());
            assertEquals("joinExpr must not contain an unterminated block comment: t /* x ON a = b",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").crossJoin("t /* x ON a = b")).getMessage());

            // A closed nested comment is accepted.
            assertEquals("SELECT * FROM a JOIN t /* /* */ */ ON a.id = t.id",
                    PSC.select("*").from("a").join("t /* /* */ */").on("a.id = t.id").build().query());
        }

        @Test
        void testFromNonReservedJoinModifierWordAsTableAlias() {
            assertEquals("SELECT semi.first_name AS \"firstName\" FROM account semi, device d",
                    PSC.select("firstName").from("account semi, device d", Account.class).build().query());
            assertEquals("SELECT anti.first_name AS \"firstName\" FROM account anti",
                    PSC.select("firstName").from("account anti", Account.class).build().query());
            assertEquals("SELECT a.first_name AS \"firstName\" FROM account a SEMI JOIN device d ON a.id = d.account_id",
                    PSC.select("firstName").from("account a SEMI JOIN device d ON a.id = d.account_id", Account.class).build().query());
            assertEquals("SELECT a.first_name AS \"firstName\" FROM account a ASOF LEFT JOIN device d ON a.id = d.account_id",
                    PSC.select("firstName").from("account a ASOF LEFT JOIN device d ON a.id = d.account_id", Account.class).build().query());
        }

        // Regression: ClickHouse's "GLOBAL CROSS JOIN" / "LOCAL ... JOIN" were not recognized as a join start, so the
        // modifier word became the primary table's alias ("GLOBAL.first_name").
        @Test
        void testGlobalAndLocalJoinModifiersBeforeCrossJoinAreNotPrimaryTableAlias() {
            for (final String join : new String[] { "GLOBAL CROSS JOIN q", "LOCAL CROSS JOIN q", "LOCAL ANY LEFT JOIN q USING (id)", "LOCAL JOIN q USING (id)",
                    "GLOBAL /* modifier */ CROSS JOIN q", "LOCAL -- modifier\n CROSS JOIN q", "LOCAL # modifier\n CROSS JOIN q" }) {
                assertEquals("SELECT a.first_name AS \"firstName\" FROM account a " + join,
                        PSC.select("firstName").from("account a " + join, Account.class).build().query(), join);
            }

            // Not followed by a JOIN keyword: still a plain table alias.
            assertEquals("SELECT local.first_name AS \"firstName\" FROM account local",
                    PSC.select("firstName").from("account local", Account.class).build().query());
        }

        // Regression: the second '#' of PostgreSQL's "##" operator was read as a hash comment by the alias and JOIN-connector scanners.
        @Test
        void testHashPairOperatorIsNotReadAsHashCommentByAliasAndJoinScanners() {
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

        // Regression: a qualified JOIN whose ON follows a comment ended only by a lone '\r' is rejected as incomplete (MySQL and
        // SQLite read the ON as part of the comment), but the error claimed no ON/USING had been written at all.
        @Test
        void testIncompleteJoinErrorExplainsConnectorHiddenByLoneCarriageReturnComment() {
            final Dsl mysql = lexDialect(PSC, "MySQL");

            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> PSC.select("id").from("account a").join("device d -- x\r ON a.id = d.account_id").where(Filters.eq("id", 1)));
            assertTrue(
                    e.getMessage()
                            .startsWith("The preceding qualified JOIN must be completed with on(...) or using(...) before 'WHERE'. Its ON/USING connector"),
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

        // Regression: the JOIN-connector, FROM-separator and FROM-alias scanners ignored the dialect's string, comment and bracket rules.
        @Test
        void testJoinAndFromScannersFollowDialectLexicalRules() {
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
                    sqlServer.select("u.id")
                            .from("users u")
                            .join("(SELECT * FROM files WHERE dir = 'C:\\') f ON f.uid = u.id")
                            .where(Filters.eq("u.id", 1))
                            .build()
                            .query());
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

        @Test
        void testJoinApisRequireFromAndPrecedeLaterClauses() {
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
        void testJoinConditionGroupsMultipleTablesWithCrossJoin() {
            final Criteria join = Criteria.builder().join(List.of("orders o", "payments p"), Filters.on("u.id", "o.userId")).build();
            assertSql("SELECT * FROM users u JOIN (orders o CROSS JOIN payments p) ON (u.id = o.user_id)", List.of(),
                    PSC.select("*").from("users u").append(join).build());
        }

        // Covers the dialect-aware JOIN-connector and FROM-separator scans on further dialects: a standard 'C:\' literal cannot
        // hide a CROSS JOIN connector or the separator after the primary table, while dialect comment/bracket rules still apply.
        @Test
        void testJoinConnectorAndFromSeparatorScansOnMoreDialects() {
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
            final String collectionFrom = pg.select(Account.class)
                    .from(Arrays.asList("(SELECT * FROM account WHERE p = 'C:\\') x, other o", "z"))
                    .build()
                    .query();
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

        @Test
        void testJoinedEntityMappingDoesNotLeakIntoUnqualifiedPrimaryColumns() {
            // Joining an entity class onto a plain-string FROM must not install the joined entity's property
            // mapping as the builder's PRIMARY mapping: unqualified names in later clauses used to resolve through
            // the joined entity and were prefixed with the PRIMARY table's alias (o.first_name).
            assertEquals("SELECT o.id FROM orders o JOIN account acc ON o.aid = acc.id WHERE first_name = ?",
                    PSC.select("o.id").from("orders o").join(Account.class, "acc").on("o.aid = acc.id").where(Filters.eq("firstName", "J")).build().query());
            assertEquals("SELECT o.id FROM orders o JOIN account acc ON o.aid = acc.id WHERE acc.first_name = ?",
                    PSC.select("o.id")
                            .from("orders o")
                            .join(Account.class, "acc")
                            .on("o.aid = acc.id")
                            .where(Filters.eq("acc.firstName", "J"))
                            .build()
                            .query());
            assertEquals("SELECT o.id FROM orders o JOIN account acc ON o.aid = acc.id ORDER BY last_name",
                    PSC.select("o.id").from("orders o").join(Account.class, "acc").on("o.aid = acc.id").orderBy("lastName").build().query());
            assertEquals("SELECT o.id FROM orders o JOIN account acc ON o.aid = acc.id GROUP BY o.id HAVING first_name = ?",
                    PSC.select("o.id")
                            .from("orders o")
                            .join(Account.class, "acc")
                            .on("o.aid = acc.id")
                            .groupBy("o.id")
                            .having(Filters.eq("firstName", "J"))
                            .build()
                            .query());

            // every entity-class join flavour routes through addPropColumnMapForAlias and behaves the same way
            assertEquals("SELECT o.id FROM orders o LEFT JOIN account acc ON o.aid = acc.id WHERE first_name = ?",
                    PSC.select("o.id")
                            .from("orders o")
                            .leftJoin(Account.class, "acc")
                            .on("o.aid = acc.id")
                            .where(Filters.eq("firstName", "J"))
                            .build()
                            .query());
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
        void testJoinedEntityMappingsUseTheRenderedAliasToken() {
            for (final String alias : List.of(" c ", "AS c", "AS\tc")) {
                final String query = PSC.select("*").from("review_parent p").join(ReviewChild.class, alias).on(Filters.eq("c.value", 7)).build().query();
                assertTrue(query.endsWith(" ON c.stored_value = ?"), query);
            }
        }

        @Test
        void testJoinExprWithUnterminatedBlockCommentIsRejected() {
            // Regression: join("t /* x") rendered "JOIN t /* x ON ...", commenting out the join predicate and every later
            // clause; the builder now agrees with Join's entity validation.
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").join("t /* x"));
            assertEquals("joinExpr must not contain an unterminated block comment: t /* x", e.getMessage());

            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").leftJoin("t /* x ON a.id = t.id"));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a").crossJoin("t /* x"));

            // A terminated block comment and a quoted "/*" are kept verbatim.
            assertEquals("SELECT * FROM a JOIN t /* x */ u ON a.id = u.id", PSC.select("*").from("a").join("t /* x */ u").on("a.id = u.id").build().query());
            assertEquals("SELECT * FROM a JOIN t ON t.c = '/*'", PSC.select("*").from("a").join("t ON t.c = '/*'").build().query());
        }

        // Pins a deliberate trade-off of the leading join modifiers: an alias literally named like one (GLOBAL, PASTE,
        // POSITIONAL) directly before a JOIN is read as a join modifier, so the entity columns stay unqualified.
        @Test
        void testJoinModifierWordUsedAsAliasBeforeJoinIsNotTheTableAlias() {
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

        @Test
        void testJoinOnDetectionIgnoresHashOrAtPrefixedIdentifier() {
            assertEquals("SELECT * FROM users u JOIN #on o ON u.id = o.uid", PSC.select("*").from("users u").join("#on o").on("u.id = o.uid").build().query());
            assertEquals("SELECT * FROM users u JOIN @on o ON u.id = o.uid", PSC.select("*").from("users u").join("@on o").on("u.id = o.uid").build().query());
            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users u").join("orders o ON u.id = o.uid").on("x = y"));
        }

        @Test
        void testJoinTrailingLineCommentsDoNotSwallowConnectorsOrClauses() {
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
        void testJoinWithEntityClass() {
            String sql = PSC.select("*").from(Account.class).join(Account.class).on("a.id = b.parent_id").build().query();
            assertNotNull(sql);
        }

        // Regression: ClickHouse/DuckDB leading join modifiers (ANY, ALL, GLOBAL, ARRAY, POSITIONAL, PASTE) were not
        // recognized as a JOIN start, so the modifier word became the primary table's alias ("ANY.first_name").
        @Test
        void testLeadingJoinModifiersDoNotBecomePrimaryTableAlias() {
            for (final String join : new String[] { "ANY LEFT JOIN quotes q USING (id)", "GLOBAL ANY JOIN quotes q USING (id)",
                    "ALL INNER JOIN quotes q USING (id)", "ARRAY JOIN arr AS x", "LEFT ARRAY JOIN arr AS x", "POSITIONAL JOIN quotes q", "PASTE JOIN quotes q",
                    "ASOF JOIN quotes q USING (id)", "SEMI JOIN quotes q USING (id)" }) {
                assertEquals("SELECT a.first_name AS \"firstName\" FROM account a " + join,
                        PSC.select("firstName").from("account a " + join, Account.class).build().query(), join);
            }

            // Not followed by a JOIN keyword: the word is still a plain table alias.
            assertEquals("SELECT any.first_name AS \"firstName\" FROM account any", PSC.select("firstName").from("account any", Account.class).build().query());
            assertEquals("SELECT paste.first_name AS \"firstName\" FROM account paste, quotes q",
                    PSC.select("firstName").from("account paste, quotes q", Account.class).build().query());
        }

        @Test
        void testMultipleJoins() {
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
        void testNaturalJoin_EntityClass() {
            final String sql = PSC.select("*").from(Account.class).naturalJoin(Account.class).build().query();

            assertTrue(sql.contains("NATURAL JOIN"));
            assertTrue(sql.toLowerCase().contains("account"));
        }

        @Test
        void testNaturalJoin_EntityClassAlias() {
            final String sql = PSC.select("*").from(Account.class, "a").naturalJoin(Account.class, "b").build().query();

            assertTrue(sql.contains("NATURAL JOIN"));
            assertTrue(sql.contains(" b"));
        }

        @Test
        void testNaturalJoin_String() {
            final String sql = PSC.select("*").from("users").naturalJoin("orders").build().query();

            assertTrue(sql.contains("NATURAL JOIN orders"));
        }

        @Test
        void testOn_varargsComposite() {
            final String sql = PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id", "u.tenant_id = o.tenant_id").build().query();

            assertTrue(sql.contains("ON (u.id = o.user_id) AND (u.tenant_id = o.tenant_id)"), sql);
        }

        @Test
        void testOnAndUsingRequireCompatibleUnconnectedJoin() {
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").on("users.id = orders.user_id"));
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").using("id"));
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").crossJoin("orders").on("users.id = orders.user_id"));
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").naturalJoin("orders").using("id"));

            final SqlBuilder joined = PSC.select("id").from("users").join("orders").on("users.id = orders.user_id");
            assertThrows(IllegalStateException.class, () -> joined.using("id"));
            assertEquals("SELECT id FROM users JOIN orders ON users.id = orders.user_id", joined.build().query());
        }

        // on(Condition) appended " ON " unconditionally, so an On/Using operand -- which renders its own
        // keyword -- produced "ON ON (...)" or the impossible "ON USING (...)".
        @Test
        void testOnConditionDoesNotDuplicateTheConnectorKeyword() {
            assertEquals("SELECT id FROM t INNER JOIN dept ON (t.dept_id = dept.id)",
                    PSC.select("id").from("t").innerJoin("dept").on(Filters.on("t.deptId", "dept.id")).build().query());
            assertEquals("SELECT id FROM t INNER JOIN dept USING (dept_id)",
                    PSC.select("id").from("t").innerJoin("dept").on(Filters.using("deptId")).build().query());

            // A plain predicate still gets the ON keyword supplied by the builder.
            assertEquals("SELECT id FROM t INNER JOIN dept ON t.dept_id = dept.id",
                    PSC.select("id").from("t").innerJoin("dept").on(Filters.expr("t.deptId = dept.id")).build().query());
        }

        @Test
        void testOnSingleElementArrayMatchesSingleExpressionOverload() {
            final String expression = "u.id = o.user_id OR u.id = o.owner_id";

            assertEquals(PSC.select("*").from("users u").join("orders o").on(expression).build().query(),
                    PSC.select("*").from("users u").join("orders o").on(new String[] { expression }).build().query());
        }

        @Test
        void testOnVarargsPreservesDisjunctionPrecedence() {
            final String sql = PSC.select("*")
                    .from("users u")
                    .join("orders o")
                    .on("u.id = o.user_id OR u.id = o.owner_id", "u.tenant_id = o.tenant_id OR o.public_flag = 1")
                    .build()
                    .query();

            assertEquals(
                    "SELECT * FROM users u JOIN orders o ON (u.id = o.user_id OR u.id = o.owner_id)" + " AND (u.tenant_id = o.tenant_id OR o.public_flag = 1)",
                    sql);
        }

        @Test
        void testPSCWithCrossJoin() {
            String sql = PSC.select("*").from("users").crossJoin("roles").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("CROSS JOIN"));
        }

        @Test
        void testPSCWithFullJoin() {
            String sql = PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FULL JOIN"));
        }

        @Test
        void testPSCWithInnerJoin() {
            String sql = PSC.select("*").from("users").innerJoin("orders").on("users.id = orders.user_id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INNER JOIN"));
        }

        @Test
        void testPSCWithJoin() {
            String sql = PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("JOIN"));
        }

        @Test
        void testPSCWithLeftJoin() {
            String sql = PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("LEFT JOIN"));
        }

        @Test
        void testPSCWithRightJoin() {
            String sql = PSC.select("*").from("users").rightJoin("orders").on("users.id = orders.user_id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("RIGHT JOIN"));
        }

        @Test
        void testQualifiedJoinMustBeCompletedBeforeStatementCanAdvance() {
            final SqlBuilder builder = PSC.select("u.id").from("users u").join("orders o");

            assertThrows(IllegalStateException.class, builder::build);
            assertThrows(IllegalStateException.class, () -> builder.where(Filters.eq("u.active", true)));
            assertThrows(IllegalStateException.class, () -> builder.join("payments p"));
            assertThrows(IllegalStateException.class, () -> builder.union("SELECT id FROM archived_users"));

            // Every rejection is side-effect free; the original join can still be completed and built.
            assertEquals("SELECT u.id FROM users u JOIN orders o ON u.id = o.user_id", builder.on("u.id = o.user_id").build().query());
        }

        // Regression: MySQL/MariaDB and SQLite end a line comment only at '\n', but the JOIN-connector scan ended it at a lone
        // '\r', so "u -- x\r ON ..." counted as a complete join and a WHERE could follow: those servers silently ran a cross join.
        @Test
        void testQualifiedJoinWithOnHiddenByLoneCarriageReturnCommentMustBeCompleted() {
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

        // A raw ON/USING fragment completes the pending qualified JOIN, exactly as on()/using() would.
        @Test
        void testRawAppendedConnectorCompletesQualifiedJoin() {
            assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id WHERE u.active = 1",
                    PSC.select("*").from("users u").join("orders o").append("ON u.id = o.user_id").where("u.active = 1").build().query());
            assertEquals("SELECT * FROM users u JOIN orders o USING (user_id)",
                    PSC.select("*").from("users u").join("orders o").append("USING (user_id)").build().query());
            assertEquals("SELECT * FROM users u JOIN orders o on(u.id = o.user_id)",
                    PSC.select("*").from("users u").join("orders o").append("on(u.id = o.user_id)").build().query());

            // The slot is closed afterwards, so a structured connector is rejected instead of double-rendering ON.
            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users u").join("orders o").append("ON u.id = o.user_id").on("x = y"));

            // Any other raw fragment leaves the JOIN open; the next clause and build() still reject it.
            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users u").join("orders o").append("only").build());
            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users u").join("orders o").append("online = 1").where("x = 1"));
        }

        @Test
        void testRawConnectorCanStartAfterWhitespace() {
            final SqlBuilder builder = PSC.select("*").from("people p").join("orders o").append(" \tON p.id = o.person_id");
            assertSql("SELECT * FROM people p JOIN orders o \tON p.id = o.person_id WHERE p.id = ?", List.of(7), builder.where(Filters.eq("p.id", 7)).build());
        }

        @Test
        void testRawJoinWithInlineConnectorClosesConnectorSlot() {
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
        void testUsing() {
            final String sql = PSC.select("*").from("users").join("orders").using("user_id").build().query();

            assertTrue(sql.contains("USING (user_id)"));
        }

        @Test
        void testUsing_collection() {
            final String sql = PSC.select("*").from("orders").join("order_items").using(Arrays.asList("order_id", "tenant_id")).build().query();

            assertTrue(sql.contains("USING (order_id, tenant_id)"), sql);
        }

        @Test
        void testUsing_varargs() {
            final String sql = PSC.select("*").from("orders").join("order_items").using("order_id", "tenant_id").build().query();

            assertTrue(sql.contains("USING (order_id, tenant_id)"), sql);
        }
    }

    @Nested
    class Lifecycle {
        @Test
        void testAccept_SPConsumer() throws Exception {
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
        void testAccept_SqlAndParams() throws Exception {
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
        void testAppendRejectsGenericClausesThatRequireDedicatedBuilderState() {
            for (final Operator operator : Arrays.asList(Operator.LIMIT, Operator.OFFSET, Operator.FOR_UPDATE)) {
                final SqlBuilder builder = PSC.select("id").from("users");

                assertThrows(IllegalArgumentException.class, () -> builder.append(new TestClause(operator, Filters.expr("10"))));
                assertEquals("SELECT id FROM users WHERE active = ?", builder.where(Filters.eq("active", true)).build().query());
            }
        }

        @Test
        void testApply_SPFunction() throws Exception {
            final List<Object> result = PSC.select("id")
                    .from("users")
                    .where(Filters.eq("id", 1))
                    .apply(sp -> Arrays.asList(sp.query(), sp.parameters().size()));

            assertTrue(result.get(0).toString().contains("WHERE"));
            assertEquals(1, result.get(1));
        }

        @Test
        void testApply_SqlAndParams() throws Exception {
            final String result = PSC.select("id").from("users").where(Filters.eq("id", 1)).apply((sql, params) -> sql + " / " + params.size());

            assertTrue(result.contains("WHERE"));
            assertTrue(result.endsWith("/ 1"));
        }

        @Test
        void testBuild() {
            assertSql("SELECT acc.id AS \"id\" FROM account acc WHERE acc.id = ?", List.of(1),
                    PSC.select("id").from(Account.class).where(Filters.eq("id", 1)).build());
        }

        @Test
        void testBuilderBackedSubqueriesRejectStatementTerminatorsBeforeMergingBindings() {
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
        void testBuildSnapshotsProtectedParameterBuffer() {
            final SqlBuilder builder = PSC.select("id").from(Account.class).where(Filters.eq("id", 1));
            final AbstractQueryBuilder.SP sqlPair = builder.build();

            builder._parameters.add(2);

            assertEquals(Collections.singletonList(1), sqlPair.parameters());
        }

        @Test
        void testClauseBuildersRejectBlankStringFragments() {
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
        void testClosedBuilderMutationApisConsistentlyThrowIllegalStateException() {
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
        void testClosedBuilderTakesPrecedenceOverArgumentValidation() {
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
        void testConditionOnlyBuilderRejectsSecondPredicate() {
            final SqlBuilder builder = PSC.renderCondition(Filters.equal("a", 1));
            assertThrows(IllegalStateException.class, () -> builder.append(Filters.equal("b", 2)));
            assertEquals("a = ?", builder.build().query());
        }

        // The bare predicate of a condition-only builder is meant to follow a WHERE keyword, so a later WHERE
        // ("a = ? WHERE b = 1") is malformed and rejected on every route.
        @Test
        void testConditionOnlyBuilderRejectsWhereAfterBarePredicate() {
            assertThrows(IllegalStateException.class, () -> PSC.renderCondition(Filters.eq("a", 1)).where("b = 1"));
            assertThrows(IllegalStateException.class, () -> PSC.renderCondition(Filters.eq("a", 1)).where(Filters.eq("b", 1)));
            assertThrows(IllegalStateException.class, () -> PSC.renderCondition(Filters.eq("a", 1)).append(Filters.where(Filters.eq("b", 1))));
            assertThrows(IllegalStateException.class,
                    () -> PSC.renderCondition(Filters.eq("a", 1)).append(Criteria.builder().where(Filters.eq("b", 1)).build()));

            // Clauses that may legally follow a WHERE predicate are still accepted.
            assertEquals("a = ? ORDER BY b", PSC.renderCondition(Filters.eq("a", 1)).orderBy("b").build().query());
            assertEquals("WHERE a = ? ORDER BY b", PSC.renderCondition(Criteria.builder().where(Filters.eq("a", 1)).orderBy("b").build()).build().query());
        }

        @Test
        void testDebugLoggingDescribesBuilderCreationAndCompletedSql() {
            final var records = QueryTestSupport.captureLogs(AbstractQueryBuilder.class,
                    () -> assertSql("SELECT id FROM people WHERE id = ?", List.of(7), PSC.select("id").from("people").where(Filters.eq("id", 7)).build()));
            assertTrue(records.stream().anyMatch(record -> record.getMessage().contains("SqlBuilder created")));
            assertTrue(records.stream().anyMatch(record -> record.getMessage().contains("Built SQL metadata")));
        }

        @Test
        void testDialectScopedConfigurationSurvivesToBuilder() {
            final BiConsumer<StringBuilder, String> handler = (sb, name) -> sb.append("${").append(name).append('}');
            final SqlParser.TokenizerConfig tokenizerConfig = SqlParser.tokenizerConfigBuilder().withSeparator("::").build();
            final SqlDialect dialect = NSC.sqlDialect().toBuilder().namedParameterHandler(handler).tokenizerConfig(tokenizerConfig).build();
            final SqlDialect copy = dialect.toBuilder().build();

            assertSame(handler, copy.namedParameterHandler());
            assertSame(tokenizerConfig, copy.tokenizerConfig());
            assertEquals(dialect, copy);
            assertEquals(dialect.hashCode(), copy.hashCode());
        }

        // SqlExpression.toSql and the builder's own tokenizer path both pass a digit-leading token through;
        // the builder's short-literal fast path used to be the only path that naming-converted it.
        @Test
        void testDigitLeadingShortTokenIsNotConvertedByTheBuilderFastPath() {
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

        // An empty junction is a complete predicate: the builder renders its Boolean identity exactly as Junction.toSql does.
        @Test
        void testEmptyJunctionRendersAsBooleanIdentityThroughTheBuilder() {
            assertEquals("1 = 1", PSC.renderCondition(Filters.and()).build().query());
            assertEquals("1 = 0", PSC.renderCondition(Filters.or()).build().query());
            assertEquals("SELECT * FROM account WHERE 1 = 0", PSC.select("*").from("account").where(Filters.or()).build().query());
            assertEquals("SELECT * FROM account WHERE 1 = 1", PSC.select("*").from("account").where(Filters.and()).build().query());
            assertEquals("SELECT * FROM account HAVING 1 = 1", PSC.select("*").from("account").having(Filters.and()).build().query());
            assertEquals("SELECT * FROM account WHERE (1 = 0) AND (id = ?)",
                    PSC.select("*").from("account").where(Filters.and(Filters.or(), Filters.eq("id", 1))).build().query());
            assertEquals("SELECT * FROM account WHERE 1 = 1",
                    PSC.select("*").from("account").append(Criteria.builder().where(Filters.and()).build()).build().query());
        }

        @Test
        void testEntitySetGetterAndRendererFailuresRestoreAllBuilderMetadata() {
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
        void testExcept_SqlBuilder() {
            final AbstractQueryBuilder.SP sp = PSC.select("id")
                    .from("users")
                    .where(Filters.eq("type", "USER"))
                    .except(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                    .build();

            assertTrue(sp.query().contains("EXCEPT"));
            assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
        }

        @Test
        void testExcessiveActiveBuildersAreReportedAndReleased() throws Exception {
            final var counterField = AbstractQueryBuilder.class.getDeclaredField("activeStringBuilderCounter");
            counterField.setAccessible(true);
            final var counter = (java.util.concurrent.atomic.AtomicInteger) counterField.get(null);
            final int before = counter.get();
            final List<SqlBuilder> builders = new ArrayList<>();
            final var records = QueryTestSupport.captureLogs(AbstractQueryBuilder.class, () -> {
                try {
                    for (int i = 0; i < 1025; i++) {
                        builders.add(PSC.select("id").from("people"));
                    }
                } finally {
                    for (final SqlBuilder builder : builders) {
                        assertSql("SELECT id FROM people", List.of(), builder.build());
                    }
                }
            });
            assertEquals(before, counter.get());
            assertTrue(records.stream()
                    .anyMatch(record -> record.getLevel() == java.util.logging.Level.SEVERE && record.getMessage().contains("Too many active query builders")));
        }

        @Test
        void testExtensionHooksRejectUninitializedInsertAndSelectColumns() {
            for (final var operation : List.of(com.landawn.abacus.util.OperationType.ADD, com.landawn.abacus.util.OperationType.QUERY)) {
                final SqlBuilder builder = new SqlBuilder(null);
                builder._op = operation;
                assertTrue(assertThrows(IllegalStateException.class, () -> builder.into("people")).getMessage().contains("Column names must be set"));
                assertThrows(IllegalStateException.class, builder::build);
            }
        }

        @Test
        void testFromDerivedTableParentSelectListFailureLeavesChildReusable() {
            final SqlBuilder child = PSC.select("id").from("users");
            final SqlBuilder parent = PSC.select("id -- x");
            assertThrows(IllegalArgumentException.class, () -> parent.from(child, "u"));
            assertEquals("SELECT id FROM users", child.build().query());
        }

        @Test
        void testIntersect_SqlBuilder() {
            final AbstractQueryBuilder.SP sp = PSC.select("id")
                    .from("users")
                    .where(Filters.eq("type", "USER"))
                    .intersect(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                    .build();

            assertTrue(sp.query().contains("INTERSECT"));
            assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
        }

        // limit(int, int) reports the lifecycle error before argument validation, exactly like limit(int).
        @Test
        void testLimitWithOffsetReportsClosedBuilderBeforeArgumentErrors() {
            final SqlBuilder closed = PSC.select("*").from("t");
            closed.build();
            assertThrows(IllegalStateException.class, () -> closed.limit(-1, 0));
            assertThrows(IllegalStateException.class, () -> closed.limit(-1));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("t").limit(-1, 0));
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
        void testManyBuildersDoNotLeakOrThrow() {
            for (int i = 0; i < 32; i++) {
                String sql = PSC.select("id").from(Account.class).where(Filters.eq("id", i)).build().query();
                assertNotNull(sql);
            }
        }

        @Test
        void testMinus_SqlBuilder() {
            final AbstractQueryBuilder.SP sp = PSC.select("id")
                    .from("users")
                    .where(Filters.eq("type", "USER"))
                    .minus(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                    .build();

            assertTrue(sp.query().contains("MINUS"));
            assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
        }

        @Test
        void testNamedParameterHandlerIsScopedAcrossInterleavedBuilders() {
            final Dsl myBatisStyle = Dsl
                    .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, propName) -> sb.append("#{").append(propName).append("}")).build());

            final SqlBuilder customBuilder = myBatisStyle.select("name").from("users");
            final SqlBuilder defaultBuilder = NSC.select("name").from("users");

            assertTrue(customBuilder.where(Filters.eq("id", 1)).build().query().contains("#{id}"));
            assertTrue(defaultBuilder.where(Filters.eq("id", 1)).build().query().contains(":id"));
        }

        @Test
        void testNullDialectUsesRawSqlAndSnakeCaseDefaults() {
            final SqlBuilder builder = new SqlBuilder(null);
            builder._op = com.landawn.abacus.util.OperationType.QUERY;
            builder._propOrColumnNames = List.of("firstName");
            assertSql("SELECT first_name AS \"firstName\" FROM people WHERE id = 7", List.of(), builder.from("people").where(Filters.eq("id", 7)).build());
        }

        @Test
        void testNullTerminalCallbacksDoNotConsumeBuilder() {
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

        @Test
        void testOperationSpecificClausesRejectInvalidDmlWithoutChangingTheBuilder() {
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
        void testParentTableExclusionsApplyToExpandedChildrenAndFromTables() {
            for (final Class<?> type : List.of(ExcludedChildParent.class, WhitelistedParent.class)) {
                assertEquals(List.of("id"), QueryUtil.selectPropNames(type, true, null));
                assertFalse(QueryUtil.propToColumnNameMap(type, NamingPolicy.SNAKE_CASE).containsKey("child.value"));
                final String query = PSC.selectFrom(type, true).build().query();
                assertFalse(query.contains("review_child"), query);
                assertTrue(query.startsWith("SELECT id AS \"id\" FROM "), query);
            }
        }

        // Regression: the builders converted the unquoted part of a schema-qualified type or collation after a delimited
        // schema like a column (CAMEL_CASE: status::"types".order_status -> status::"types".orderStatus).
        @Test
        void testQualifiedTypeOrCollationAfterDelimitedSchemaIsKeptByBuilders() {
            assertEquals("SELECT id FROM t WHERE status::\"types\".order_status = 'NEW'",
                    PLC.select("id").from("t").where("status::\"types\".order_status = 'NEW'").build().query());
            assertEquals("SELECT id FROM t WHERE CAST(status AS \"types\".order_status) = 'NEW'",
                    PLC.select("id").from("t").where(Filters.expr("CAST(status AS \"types\".order_status) = 'NEW'")).build().query());
            assertEquals("SELECT id FROM t WHERE name collate \"public\".my_collation = 'x'",
                    PLC.select("id").from("t").where("name collate \"public\".my_collation = 'x'").build().query());
            assertEquals("SELECT acc.id AS \"id\" FROM account acc WHERE acc.status::\"types\".orderStatus = 'NEW'",
                    PSC.select("id").from(Account.class).where(Filters.expr("status::\"types\".orderStatus = 'NEW'")).build().query());
        }

        // Regression: the builder's raw-expression rendering went verbatim at any backslash before any quote, so a JSON literal
        // with \" left every later column unconverted; it also renamed a function with a glued marker, copied the column after
        // log_${month}. and the bounds of slices and chained subscripts unconverted.
        @Test
        void testRawExpressionGluedTokensAndBackslashesRenderThroughBuilders() {
            final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());

            for (final Dsl dsl : new Dsl[] { PSC, pg }) {
                assertEquals("SELECT id FROM t WHERE payload @> '{\"a\":\"b\\\"c\"}' AND created_at > 1",
                        dsl.select("id").from("t").where("payload @> '{\"a\":\"b\\\"c\"}' AND createdAt > 1").build().query());
                assertEquals("SELECT id FROM t WHERE \"it\\'s\" = 1 AND created_at > 1",
                        dsl.select("id").from("t").where(Filters.expr("\"it\\'s\" = 1 AND createdAt > 1")).build().query());
                // A backslash before the region's own closing quote still emits the rest as written.
                assertEquals("SELECT id FROM t WHERE \"col\\\"x\" = 1 AND aB = 1",
                        dsl.select("id").from("t").where("\"col\\\"x\" = 1 AND aB = 1").build().query());
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

        @Test
        void testRejectedRewrittenSetOperationOperandLeavesParentMetadataReusable() {
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

        // Regression: when a literal name ("id_2") pushed a composed parameter to a higher suffix ("id_3"), the parent's
        // occurrence count stayed below that suffix, so a later composition could reuse the name for a different value or
        // skip its handler conversion. Covered for a reusable union snapshot here and for the other composition paths below.
        @Test
        @SuppressWarnings("deprecation")
        void testReusableUnionSnapshotRetainsSkippedParameterSuffixes() {
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

        @Test
        void testSelectModifierRejectsNonSelectBuildersWithoutChangingThem() {
            final SqlBuilder update = PSC.update("users");
            assertThrows(IllegalStateException.class, () -> update.selectModifier("DISTINCT"));
            assertEquals("UPDATE users SET name = ?", update.set("name").build().query());

            final SqlBuilder delete = PSC.deleteFrom("users");
            assertThrows(IllegalStateException.class, () -> delete.distinct());
            assertEquals("DELETE FROM users", delete.build().query());
        }

        @Test
        void testSetApisRejectNonUpdateBuildersWithoutCorruptingThem() {
            final SqlBuilder query = PSC.select("id").from("users");
            assertThrows(IllegalStateException.class, () -> query.set("name"));
            assertEquals("SELECT id FROM users", query.build().query());

            final SqlBuilder delete = PSC.deleteFrom("users");
            assertThrows(IllegalStateException.class, () -> delete.set(Collections.singletonMap("name", "x")));
            assertEquals("DELETE FROM users", delete.build().query());

            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("users").set(Account.class));
            assertThrows(IllegalArgumentException.class, () -> PSC.update("users").set((Class<?>) null));
        }

        // Regression: the builders converted a type that is not glued to its cast (CAST(x AS type), a spaced "::", whitespace
        // after a qualifying dot) like a column.
        @Test
        void testTypeInCastOrAfterSpacedCastIsKeptByBuilders() {
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
                    PLC.select("id")
                            .from("t")
                            .where("CAST(id AS format . order_status) = '1' AND CAST(id AS at /* c */ . order_status) = '1'")
                            .build()
                            .query());
        }

        @Test
        void testUnion_ParenthesizedFromLessQueryAccepted() {
            // "UNION (SELECT 1)" is valid SQL: a FROM-less query wrapped in balanced parentheses is as
            // complete an operand as the already-accepted unparenthesized union("SELECT 1").
            assertEquals("SELECT id FROM users UNION (SELECT 1)", PSC.select("id").from("users").union("(SELECT 1)").build().query());
            assertEquals("SELECT id FROM users UNION ((SELECT 1))", PSC.select("id").from("users").union("((SELECT 1))").build().query());
            assertEquals("SELECT id FROM users UNION (SELECT 1) /* operand */",
                    PSC.select("id").from("users").union("(SELECT 1) /* operand */").build().query());

            // All five set operations (union/unionAll/intersect/except/minus) share the same operand check
            // (checkSetOperationSubQuery -> isSubQuery), so a second operation pins the shared path.
            assertEquals("SELECT id FROM users INTERSECT (SELECT id FROM t)", PSC.select("id").from("users").intersect("(SELECT id FROM t)").build().query());
        }

        @Test
        void testUnion_SqlBuilder() {
            final AbstractQueryBuilder.SP sp = PSC.select("id")
                    .from("users")
                    .where(Filters.eq("type", "USER"))
                    .union(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                    .build();

            assertTrue(sp.query().contains("UNION"));
            assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
        }

        @Test
        void testUnionAll_SqlBuilder() {
            final AbstractQueryBuilder.SP sp = PSC.select("id")
                    .from("users")
                    .where(Filters.eq("type", "USER"))
                    .unionAll(PSC.select("id").from("admins").where(Filters.eq("type", "ADMIN")))
                    .build();

            assertTrue(sp.query().contains("UNION ALL"));
            assertEquals(Arrays.asList("USER", "ADMIN"), sp.parameters());
        }

        @Test
        void testUnionChildPlaceholderRenameFailsClosedOnAmbiguousDefaultDialectBackslash() {
            // Without productInfo, 'C:\' is complete (standard SQL) or unterminated (MySQL): the two readings
            // rename different placeholders, so the rewrite is rejected instead of guessing.
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> NSC.select("id")
                            .from("a")
                            .where(Filters.eq("id", 1))
                            .union(NSC.select("id").from("t").where(Filters.and(Filters.expr("path = 'C:\\'"), Filters.eq("id", 2)))));
            assertTrue(ex.getMessage().contains("productInfo"), ex.getMessage());

            assertThrows(IllegalArgumentException.class,
                    () -> MSC.select("id")
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

        @Test
        void testUpdatePrefixWithoutAssignmentsCannotBeFinalized() {
            final SqlBuilder builder = PSC.update("people");
            builder.init(false);
            assertTrue(assertThrows(IllegalStateException.class, builder::build).getMessage().contains("set() must be called"));
        }
    }

    @Nested
    class NamingAndMetadata {
        // Covers the "##" token and the backslash readings of the alias scanner: an explicit dialect finds the derived-table
        // alias past "##", the default infers none where its readings disagree, and standard dialects keep 'it\'s' unterminated.
        @Test
        void testAliasScannerHashPairAndBackslashReadingsPerDialect() {
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

        // Covers the dialect-aware line-comment termination of append(String): MySQL '#' and "--x", standard vs MySQL
        // string escapes, SQL Server temporary tables, already-terminated fragments, a lone '\r', appendIf/appendIfOrElse,
        // and an UPDATE.
        @Test
        void testAppendTerminatesTrailingLineCommentPerDialect() {
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

        // Under CAMEL_CASE the builder used Beans.normalizePropName, whose Java-keyword map rewrote the
        // identifier "class" to "clazz" while SqlExpression.toSql (plain NamingPolicy.convert) did not.
        @Test
        void testCamelCasePolicyRendersClassIdentifierIdenticallyThroughBothPaths() {
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
        void testCamelIdentityPathPreservesAcronymAndDigitBoundaries() {
            for (final String name : Arrays.asList(null, "", "firstName", "columnName42", "aB", "aB2", "a2B", "abc1Xy", "iPhone", "fooBAR", "URLValue",
                    "_firstName_", "t.firstName", "éclair", "猫名", "class")) {
                final String expected = AbstractQueryBuilder.sqlKeyWords.contains(name) ? name : QueryUtil.convertIdentifier(name, NamingPolicy.CAMEL_CASE);
                assertEquals(expected, AbstractQueryBuilder.normalizeColumnName(name, NamingPolicy.CAMEL_CASE), name);
            }
        }

        // Covers the class-aliased path of the implicit-alias quote doubling: the "classAlias." prefix sits inside the
        // quoted label, and every embedded identifier quote is still doubled.
        @Test
        void testClassAliasedImplicitSelectAliasEscapesEmbeddedIdentifierQuote() {
            assertEquals("SELECT COALESCE(\"nickName\", a.first_name) AS \"acc.COALESCE(\"\"nickName\"\", firstName)\" FROM account a", PSC.selectFrom(List.of(
                    Selection.builder(Account.class).tableAlias("a").classAlias("acc").includedPropNames(List.of("COALESCE(\"nickName\", firstName)")).build()))
                    .build()
                    .query());
        }

        // Regression: under a class alias the whole suffix after the inline AS was quoted as ONE label, so
        // "firstName AS fn, lastName AS ln" silently became one column labeled "acc.fn, lastName AS ln" (lastName vanished).
        @Test
        void testClassAliasedSelectItemWithMultiTokenAliasIsRejected() {
            for (final String item : new String[] { "firstName AS fn, lastName AS ln", "firstName AS f n" }) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC
                        .selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").classAlias("acc").includedPropNames(List.of(item)).build())));
                assertTrue(e.getMessage().contains(item), e.getMessage());
            }

            // A one-word alias under a class alias, and a comma-separated suffix without one, render as before.
            assertEquals("SELECT a.first_name AS \"acc.fn\" FROM account a",
                    PSC.selectFrom(
                            List.of(Selection.builder(Account.class).tableAlias("a").classAlias("acc").includedPropNames(List.of("firstName AS fn")).build()))
                            .build()
                            .query());
            assertEquals("SELECT a.first_name AS fn, lastName AS ln FROM account a", PSC
                    .selectFrom(List.of(Selection.builder(Account.class).tableAlias("a").includedPropNames(List.of("firstName AS fn, lastName AS ln")).build()))
                    .build()
                    .query());
        }

        @Test
        void testDerivedTableAliasLineCommentIsTerminated() {
            final String sql = PSC.select("*").from(PSC.select("id").from("users"), "u -- note").where(Filters.eq("x", 1)).build().query();
            assertEquals("SELECT * FROM (SELECT id FROM users) u -- note\n WHERE x = ?", sql);
        }

        @Test
        void testExplicitAliasEqualToExpressionSuffixIsKept() {
            // Only a whole-expression match ("name AS name") makes the alias redundant; a suffix match must keep it.
            assertEquals("SELECT unit_price * quantity AS quantity FROM t", PSC.select("unitPrice * quantity AS quantity").from("t").build().query());
            assertEquals("SELECT first_name || ' ' || last_name AS last_name FROM t",
                    PSB.select("first_name || ' ' || last_name AS last_name").from("t").build().query());
            assertEquals("SELECT a + b AS \"b\" FROM t", PSC.select(Map.of("a + b", "b")).from("t").build().query());

            assertEquals("SELECT name FROM t", PSB.select("name AS name").from("t").build().query());
            assertEquals("SELECT t.name AS name FROM t", PSB.select("t.name AS name").from("t").build().query());
        }

        @Test
        void testExplicitlySelectedExcludedSubEntityKeepsItsFromTable() {
            // The explicitly selected root is still expanded, so its table must be listed in FROM.
            assertEquals("SELECT review_child.stored_value AS \"child.value\" FROM review_parent, review_child",
                    PSC.selectFrom(Selection.builder(ExcludedChildParent.class).includedPropNames(List.of("child")).build()).build().query());
            assertEquals("SELECT review_child.stored_value AS \"child.value\" FROM review_whitelisted_parent, review_child",
                    PSC.selectFrom(Selection.builder(WhitelistedParent.class).includedPropNames(List.of("child")).build()).build().query());

            // Default projections still exclude the child and its table.
            assertEquals("SELECT id AS \"id\" FROM review_parent",
                    PSC.selectFrom(Selection.builder(ExcludedChildParent.class).includeSubEntityProperties(true).build()).build().query());
        }

        // Regression: an explicitly selected self-referencing sub-entity ("parent" of a Node) was expanded with the entity's own
        // @Table alias: under another alias it referenced an undeclared table ("n.id ... FROM node x"), and under the default
        // alias every node silently became its own parent.
        @Test
        void testExplicitlySelectedSelfReferencingSubEntityIsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> PSC.selectFrom(List.of(Selection.builder(SubEntityNode.class).tableAlias("x").includedPropNames(List.of("id", "parent")).build()))
                            .build());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id", "parent").from(SubEntityNode.class, "x"));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from(SubEntityNode.class, "x").orderByDesc("parent"));
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.select("id", "parent").from(SubEntityNode.class));
            assertTrue(e.getMessage().contains("Self-referencing sub-entity property 'parent'"), e.getMessage());

            // The generated projection leaves the cyclic property out, and an ordinary sub-entity is still expanded.
            assertEquals("SELECT x.id AS \"id\", x.name AS \"name\" FROM node x", PSC.selectFrom(SubEntityNode.class, "x", true).build().query());
            assertTrue(PSC.select("id", "devices").from(Account.class).build().query().startsWith("SELECT acc.id AS \"id\", device.id AS \"devices.id\", "));
        }

        @Test
        void testFix_insertEntity_doesNotSkipFractionalBigDecimalIdAsZero() {
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
        void testFix_insertEntity_keepsDefaultCompositeIdPartWhenAnotherIdAssigned() {
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
        void testFix_insertEntity_preservesPropertyOrder() {
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

        // Bug fix: set(Object entity), insert(entity), and batchInsert(entities) used HashMap internally,
        // which made the resulting SET / INSERT column order depend on hash codes rather than the
        // declared property order. The fix swaps to LinkedHashMap so column order is deterministic.

        @Test
        void testFix_setWithEntityObjectPreservesPropertyOrder() {
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

        // from(Collection) / from(String...) derive the primary table alias exactly like from(String): an element
        // carrying an inline JOIN must not hand its final predicate token to the alias scanner.
        @Test
        void testFromCollectionDerivesPrimaryAliasLikeFromString() {
            final String tableRefs = "users u JOIN orders o ON u.id = o.uid AND o.active = flag";
            final String viaString = PSC.select("flag.id", "u.id").from(tableRefs).build().query();
            assertEquals("SELECT flag.id AS \"flag.id\", u.id FROM users u JOIN orders o ON u.id = o.uid AND o.active = flag", viaString);

            assertEquals(viaString + ", extra x", PSC.select("flag.id", "u.id").from(Arrays.asList(tableRefs, "extra x")).build().query());
            assertEquals(viaString + ", extra x", PSC.select("flag.id", "u.id").from(tableRefs, "extra x").build().query());
            assertEquals("SELECT o.id AS \"o.id\" FROM users u, orders o", PSC.select("o.id").from(Arrays.asList("users u", "orders o")).build().query());
        }

        @Test
        void testFromVarargsMultiTableKeepsInlineAliasesFromFirstElement() {
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

        @Test
        void testFromWithEntityClass() {
            String sql = PSC.select("*").from(Account.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testFromWithNonBeanEntityClassDoesNotDependOnAlias() {
            assertEquals("SELECT a FROM t x", PSC.select("a").from("t x", Map.class).build().query());
            assertEquals("SELECT a FROM t", PSC.select("a").from("t", Map.class).build().query());
            assertEquals("INSERT INTO bk (a) SELECT a FROM t", PSC.select("a").into("bk", Map.class).from("t").build().query());
        }

        // Regression: the implicit select alias wrapped raw expression text in identifier quotes without escaping a quote inside it (invalid SQL).
        @Test
        void testImplicitSelectAliasEscapesEmbeddedIdentifierQuote() {
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

        // Regression: the inline select alias was trim()med, which keeps U+3000/U+2003, so the emitted (unquoted) alias -- and the
        // result-column label -- became "fn\u3000" instead of "fn".
        @Test
        void testInlineSelectAliasDropsTrailingUnicodeWhitespace() {
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
                    PSC.selectFrom(List
                            .of(Selection.builder(Account.class).tableAlias("a").classAlias("acc").includedPropNames(List.of("firstName AS fn\u00a0")).build()))
                            .build()
                            .query());
        }

        @Test
        void testIntoClass_alwaysSetsEntityClass() {
            // into(Class) should map property names to columns (entity class always set), matching from(...).
            final String sql = PSC.insert("firstName", "lastName").into(Account.class).build().query();

            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
        }

        @Test
        void testIntoRejectsEmptyTableName() {
            assertThrows(IllegalArgumentException.class, () -> PSC.insert("id").into("").build().query());
        }

        @Test
        void testIntoWithTableName() {
            String sql = PSC.insert("id", "name").into("accounts").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
        }

        // NamingPolicy.convert strips leading and trailing underscore runs; both rendering paths must restore
        // them through QueryUtil.convertIdentifier so a column literally named "_id" or "__v" keeps its identity.
        @Test
        void testLeadingAndTrailingUnderscoreIdentifiersKeepTheirRunsUnderNamingPolicy() {
            assertEquals("SELECT id FROM t WHERE _id = ?", PSC.select("id").from("t").where(Filters.eq("_id", 1)).build().query());
            assertEquals("SELECT _id, __v, _first_name AS \"_firstName\", first_name_ AS \"firstName_\" FROM t",
                    PSC.select("_id", "__v", "_firstName", "firstName_").from("t").build().query());
            assertEquals("INSERT INTO t (_id, _first_name) VALUES (?, ?)", PSC.insert("_id", "_firstName").into("t").build().query());
            assertEquals("UPDATE t SET _id = ? WHERE ___ = ?", PSC.update("t").set("_id", 1).where(Filters.eq("___", 2)).build().query());
            assertEquals("SELECT id FROM t WHERE _first_name = other_value",
                    PSC.select("id").from("t").where(Filters.expr("_firstName = otherValue")).build().query());

            assertEquals("SELECT _ID AS \"_id\", __V AS \"__v\", _FIRST_NAME AS \"_firstName\" FROM t",
                    PAC.select("_id", "__v", "_firstName").from("t").build().query());
            assertEquals("SELECT ID AS \"id\" FROM t WHERE _ID = ?", PAC.select("id").from("t").where(Filters.eq("_id", 1)).build().query());
            assertEquals("INSERT INTO t (_ID) VALUES (?)", PAC.insert("_id").into("t").build().query());

            // Parity with the condition path.
            assertEquals("_first_name = other_value", Filters.expr("_firstName = otherValue").toSql(NamingPolicy.SNAKE_CASE));
            assertEquals("_FIRST_NAME = OTHER_VALUE", Filters.expr("_firstName = otherValue").toSql(NamingPolicy.SCREAMING_SNAKE_CASE));
        }

        @Test
        @SuppressWarnings("deprecation")
        void testLegacyFromBridgeRecognizesCompleteBodiesAndRejectsBlankReferences() {
            assertSql("SELECT id FROM people", List.of(), PSC.select("id").from("people", "people").build());
            assertSql("SELECT id FROM people, orders", List.of(), PSC.select("id").from("people", "people, orders").build());
            assertSql("SELECT id FROM people, people_archive", List.of(), PSC.select("id").from("people", "people_archive").build());
            final SqlBuilder builder = PSC.select("id");
            assertThrows(IllegalArgumentException.class, () -> builder.from("people", " "));
            assertSql("SELECT id FROM people", List.of(), builder.from("people").build());
        }

        // Both registries must agree on the lower-case forms too: SqlExpression deliberately registers only
        // the as-is and UPPER-case keyword forms so a column genuinely named "order"/"count"/"rownum" is
        // still converted, whereas sqlKeyWords also registered every SK keyword in lower case, so the same
        // fragment rendered "X = ROWNUM" via toSql() but "X = rownum" via the builder under SCREAMING_SNAKE_CASE.
        @Test
        void testLowerCaseKeywordLikeColumnRendersIdenticallyThroughBothPathsUnderScreamingSnakeCase() {
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

        // Same for a backtick-quoted dialect and a class-aliased label.
        @Test
        void testMappedPhysicalColumnAliasesEscapeBackticksWithClassAlias() {
            final Dsl backtick = Dsl.forDialect(Dsl.PSC.sqlDialect().toBuilder().identifierQuote(IdentifierQuote.BACKTICK).build());
            assertEquals("SELECT t.`tick.name` AS ```tick.name``` FROM quoted_columns t",
                    backtick.select("`tick.name`").from(QuotedColumns.class, "t").build().query());
            assertEquals("SELECT t.`tick.name` AS `row.``tick.name``` FROM quoted_columns t", backtick
                    .selectFrom(Selection.builder(QuotedColumns.class).tableAlias("t").classAlias("row").includedPropNames(List.of("`tick.name`")).build())
                    .build()
                    .query());
        }

        // Regression: selecting a quoted physical @Column name through the entity mapping emitted its quotes undoubled
        // inside the quoted implicit label (AS ""quoted.name""), which is invalid SQL.
        @Test
        void testMappedPhysicalColumnAliasesEscapeDoubleQuotes() {
            assertEquals("SELECT t.\"quoted.name\" AS \"\"\"quoted.name\"\"\" FROM quoted_columns t",
                    Dsl.PSC.select("\"quoted.name\"").from(QuotedColumns.class, "t").build().query());
            assertEquals("SELECT t.\"quoted.name\" AS \"t.\"\"quoted.name\"\"\" FROM quoted_columns t",
                    Dsl.PSC.select("t.\"quoted.name\"").from(QuotedColumns.class, "t").build().query());
            assertEquals("SELECT t.\"escaped\"\"name\" AS \"\"\"escaped\"\"\"\"name\"\"\" FROM quoted_columns t",
                    Dsl.PSC.select("\"escaped\"\"name\"").from(QuotedColumns.class, "t").build().query());
        }

        // Covers per-selection expression resolution: a later selection without a table alias renders its expression
        // unqualified (no implicit alias, since the rendering equals the source text), and a qualifier naming the first
        // selection's alias still resolves to that selection.
        @Test
        void testMultiSelectExpressionItemsWithoutAliasOrNamingAnotherSelection() {
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

        /**
         * Regression test: the multi-select branch of {@code appendSelectListAndFromClause} used to
         * replace {@code _aliasPropColumnNameMap} wholesale, discarding the main-table alias mapping that
         * {@code appendOperationBeforeFrom} had registered moments earlier for the first selection's entity
         * class. An aliased reference to a property whose {@code @Column} name diverges from the naming
         * policy then fell back to naming-policy conversion instead of the annotated column name.
         */
        @Test
        void testMultiSelectFromKeepsMainTableAliasRegistration() {
            final List<Selection> selections = Arrays.asList(Selection.builder(DivergentColumnProfile.class).build(),
                    Selection.builder(Account.class).tableAlias("o").build());

            final String sql = PSC.select(selections).from("profile a, account o").where(Filters.eq("a.fullName", "X")).build().query();

            assertTrue(sql.contains("WHERE a.f_name = ?"), "aliased @Column name must resolve via the main-table registration: " + sql);
            assertFalse(sql.contains("a.full_name"), "naming-policy fallback indicates the main-table alias registration was dropped: " + sql);
        }

        // Regression: an inline "expr AS alias" in a class-aliased Selection rendered the dotted alias unquoted ("AS acc.fn", invalid SQL).
        @Test
        void testMultiSelectInlineAliasWithClassAliasIsQuoted() {
            assertEquals(
                    "SELECT a.first_name AS \"acc.fn\", UPPER(a.last_name) AS \"acc.ln\", d.name AS \"dev.dn\" FROM account a, device d", PSC
                            .selectFrom(List.of(
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
            assertEquals("SELECT a.first_name AS fn, d.name AS dn FROM account a, device d", PSC.selectFrom(List.of(
                    Selection.builder(Account.class).tableAlias("a").includedPropNames(List.of("firstName AS fn")).build(),
                    Selection.builder(com.landawn.abacus.query.entity.AccountDevice.class).tableAlias("d").includedPropNames(List.of("name AS dn")).build()))
                    .build()
                    .query());
        }

        @Test
        void testNamingPolicyEnum() {
            // Test that NamingPolicy enum values exist and are accessible
            assertNotNull(NamingPolicy.NO_CHANGE);
            assertNotNull(NamingPolicy.SNAKE_CASE);
            assertNotNull(NamingPolicy.SCREAMING_SNAKE_CASE);
            assertNotNull(NamingPolicy.CAMEL_CASE);
        }

        // sqlKeyWords is built only from SK, which does not carry the underscore-bearing niladic keyword
        // functions. Unregistered, a naming policy rewrote them into identifiers -- CURRENT_USER became
        // currentUser (CAMEL_CASE) and current-user (KEBAB_CASE, which parses as subtraction). This registry
        // and SqlExpression's are two independent rendering paths that must agree.
        @Test
        void testNiladicKeywordFunctionsAreNotRewrittenByTheNamingPolicy() {
            final String[] keywords = { "CURRENT_CATALOG", "CURRENT_DATE", "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_SCHEMA", "CURRENT_TIME",
                    "CURRENT_TIMESTAMP", "CURRENT_USER", "LOCALTIME", "LOCALTIMESTAMP", "SESSION_USER", "SYSTEM_USER", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP",
                    "NAN", "INFINITE", "UNKNOWN", "NULL" };

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

        @Test
        void testNonAsciiAliasCharactersRemainPartOfTheTableAlias() {
            for (final String alias : List.of("á", "a𐐀")) {
                final String query = PSC.select("id").from(ExcludedChildParent.class, alias).where(Filters.eq("id", 7)).build().query();
                assertEquals("SELECT " + alias + ".id AS \"id\" FROM review_parent " + alias + " WHERE " + alias + ".id = ?", query);
            }
        }

        // Controls: property names, explicit labels, NO_CHANGE and unmapped expressions keep their labels.
        @Test
        void testOrdinaryMappedAndExplicitAliasesRetainTheirLabels() {
            assertEquals("SELECT t.\"quoted.name\" AS \"value\" FROM quoted_columns t", Dsl.PSC.select("value").from(QuotedColumns.class, "t").build().query());
            assertEquals("SELECT t.\"quoted.name\" AS \"label\" FROM quoted_columns t",
                    Dsl.PSC.select(Map.of("\"quoted.name\"", "label")).from(QuotedColumns.class, "t").build().query());
            assertEquals("SELECT t.\"quoted.name\" FROM quoted_columns t", Dsl.PSB.select("\"quoted.name\"").from(QuotedColumns.class, "t").build().query());
            assertEquals("SELECT \"quoted.name\" AS \"\"\"quoted.name\"\"\" FROM t", Dsl.PSC.select("\"quoted.name\"").from("t").build().query());
        }

        @Test
        void testParenthesizedSelectScannerRequiresACompleteOperand() throws Exception {
            final var scan = AbstractQueryBuilder.class.getDeclaredMethod("isParenthesizedSelect", String.class, int.class);
            scan.setAccessible(true);
            for (final String sql : List.of("((SELECT (1)))", "(SELECT 1) -- trailing\n", "(SELECT 1) # trailing\n", "(SELECT 1) /* trailing */")) {
                assertEquals(true, scan.invoke(null, sql, sql.indexOf("SELECT")), sql);
            }
            for (final String sql : List.of(" SELECT 1", "(SELECT 1))", "(SELECT 1) + 2", "(SELECT 1) /* unfinished")) {
                assertEquals(false, scan.invoke(null, sql, sql.indexOf("SELECT")), sql);
            }
        }

        @Test
        void testQualifiedColumnAliasIsKeptUnlessItMatchesTheTableAlias() {
            for (final String table : List.of("people", "people p", "people person")) {
                assertSql("SELECT u.id AS \"u.id\" FROM " + table, List.of(), PSC.select(Map.of("u.id", "u.id")).from(table).build());
            }
            assertSql("SELECT u.id FROM people u", List.of(), PSC.select(Map.of("u.id", "u.id")).from("people u").build());
        }

        @Test
        void testRejectedEntityOverloadsPreserveExistingMappingState() {
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

        // Regression: the alias scanner always applied MySQL backslash escapes, so a standard 'C:\' literal hid the real AS alias.
        @Test
        void testSelectAliasScannerHonorsStandardLiteralEndingInBackslash() {
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

        // Regression: a whitespace-only alias in selectFrom(cls, " ", true) skipped the @Table alias fallback that "" gets,
        // leaving the parent's columns unqualified (ambiguous) next to the sub-entity tables.
        @Test
        void testSelectFromBlankAliasFallsBackToTableAliasWithSubEntities() {
            final String blankAlias = PSC.selectFrom(Account.class, " ", true).build().query();

            assertEquals(PSC.selectFrom(Account.class, "", true).build().query(), blankAlias);
            assertTrue(blankAlias.startsWith("SELECT acc.id AS \"id\""), blankAlias);
            assertTrue(blankAlias.endsWith(" FROM account acc, device"), blankAlias);
            assertEquals("acc", AbstractQueryBuilder.tableAlias(" ", Account.class));
            assertEquals("a", AbstractQueryBuilder.tableAlias("a", Account.class));
        }

        // A sub-entity table contributing no column must not be listed in FROM (Cartesian product), with or
        // without exclusions and through the Selection path alike.
        @Test
        void testSelectFromOmitsColumnlessSubEntityTable() {
            final String expected = "SELECT o.id AS \"id\", o.name AS \"name\" FROM columnless_owner o";

            assertEquals(expected, PSC.selectFrom(ColumnlessSubEntityOwner.class, "o", true, null).build().query());
            assertEquals("SELECT o.id AS \"id\" FROM columnless_owner o",
                    PSC.selectFrom(ColumnlessSubEntityOwner.class, "o", true, Set.of("name")).build().query());
            assertEquals(expected,
                    PSC.selectFrom(Selection.builder(ColumnlessSubEntityOwner.class).tableAlias("o").includeSubEntityProperties(true).build()).build().query());

            // A sub-entity that contributes columns is still listed.
            assertTrue(PSC.selectFrom(Account.class, "a", true, null).build().query().endsWith(" FROM account a, device"));
            assertTrue(PSC.selectFrom(Account.class, "a", true, Set.of("firstName")).build().query().endsWith(" FROM account a, device"));
        }

        // An alias-less Selection that lists a sub-entity table falls back to the entity's @Table alias, exactly
        // like selectFrom(Class, null, true), so the parent's "id" is not ambiguous next to the sub-entity's.
        @Test
        void testSelectFromSelectionFallsBackToTableAliasWithSubEntityTables() {
            final String expected = "SELECT o.id AS \"id\", o.status AS \"status\", c.id AS \"customer.id\", c.name AS \"customer.name\" FROM aliased_orders o, aliased_customer c";

            assertEquals(expected, PSC.selectFrom(AliasedOrder.class, (String) null, true).build().query());
            assertEquals(expected, PSC.selectFrom(Selection.builder(AliasedOrder.class).includeSubEntityProperties(true).build()).build().query());
            assertEquals(expected, PSC.selectFrom(List.of(Selection.builder(AliasedOrder.class).includeSubEntityProperties(true).build())).build().query());

            // An included sub-entity root also lists the sub-entity table, so the fallback applies too.
            assertEquals("SELECT o.id AS \"id\", c.id AS \"customer.id\", c.name AS \"customer.name\" FROM aliased_orders o, aliased_customer c",
                    PSC.selectFrom(Selection.builder(AliasedOrder.class).includedPropNames(List.of("id", "customer")).build()).build().query());

            // No sub-entity table: no fallback. An explicit alias is kept.
            assertEquals("SELECT id AS \"id\", status AS \"status\" FROM aliased_orders",
                    PSC.selectFrom(Selection.builder(AliasedOrder.class).build()).build().query());
            assertEquals(
                    "SELECT x.id AS \"id\", x.status AS \"status\", c.id AS \"customer.id\", c.name AS \"customer.name\" FROM aliased_orders x, aliased_customer c",
                    PSC.selectFrom(Selection.builder(AliasedOrder.class).tableAlias("x").includeSubEntityProperties(true).build()).build().query());

            // select(List) leaves FROM to the caller, so it does not fall back.
            assertEquals("SELECT id AS \"id\", status AS \"status\", c.id AS \"customer.id\", c.name AS \"customer.name\" FROM aliased_orders",
                    PSC.select(Selection.builder(AliasedOrder.class).includeSubEntityProperties(true).build()).from("aliased_orders").build().query());
        }

        @Test
        void testSelectingSubEntityRootPreservesResultClassAlias() {
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

        // Cover select-into and entity-class join overloads that inject aliases directly.
        @Test
        void testSelectIntoFromEntityClass() {
            final String sql = PSC.select("id", "firstName").into("account_archive").from(Account.class).build().query();

            assertTrue(sql.startsWith("INSERT INTO account_archive"));
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account acc"));
        }

        // Regression: a select item padded with Unicode whitespace and no alias ("lastName　") rendered the padding glued
        // to the column ("acc.last_name　", another identifier in PostgreSQL/MySQL/SQLite) and kept it in the implicit alias.
        @Test
        void testSelectItemWithoutAliasIsStrippedOfWhitespacePadding() {
            assertEquals("SELECT acc.last_name AS \"lastName\" FROM account acc", PSC.select("lastName　").from(Account.class).build().query());
            assertEquals("SELECT acc.last_name AS \"lastName\" FROM account acc", PSC.select(" lastName ").from(Account.class).build().query());
            assertEquals("SELECT acc.last_name AS \"lastName\" FROM account acc", PSC.select("lastName ").from(Account.class).build().query());
            assertEquals("SELECT acc.lastName AS \"lastName\" FROM account acc", PLC.select("lastName　").from(Account.class).build().query());
            assertEquals("SELECT UPPER(acc.last_name) AS \"UPPER(lastName)\" FROM account acc",
                    PSC.select("UPPER(lastName)　").from(Account.class).build().query());
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

        @Test
        void testSelectWithAlias() {
            String sql = PSC.select("firstName AS fname", "lastName AS lname").from(Account.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("AS"));
        }

        // Regression: a sort direction after a sub-entity property (which expands to the list of its columns) was applied to the LAST expanded column only.
        @Test
        void testSortDirectionAppliesToEveryExpandedSubEntityColumn() {
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

        // Covers the sort-direction rewrite beyond the plain entity context: a table alias, mixed varargs lists, the direct
        // groupBy(String, SortDirection), and that a rejected single item leaves the ORDER BY / GROUP BY slot unconsumed.
        @Test
        void testSortDirectionSubEntityExpansionWithTableAliasMixedListsAndRetry() {
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
            assertTrue(order.orderBy("devices", SortDirection.DESC)
                    .build()
                    .query()
                    .startsWith("SELECT acc.id AS \"id\" FROM account acc ORDER BY device.id DESC, "));

            final SqlBuilder group = PSC.select("id").from(Account.class);
            e = assertThrows(IllegalArgumentException.class, () -> group.groupBy("id -- x", SortDirection.DESC));
            assertEquals("SQL comment token is not allowed in column expression: id -- x", e.getMessage());
            assertEquals("SELECT acc.id AS \"id\" FROM account acc GROUP BY acc.first_name DESC",
                    group.groupBy("firstName", SortDirection.DESC).build().query());
        }

        @Test
        void testSubEntityTableIsOmittedWhenAllItsPropertiesAreExcluded() {
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

        // Regression: two sub-entity properties of one class listed the same table reference twice in FROM ("FROM person p,
        // address ad, address ad"); merely listing it once made both properties silently read the SAME row, so it is now
        // rejected. A self-referencing sub-entity rendered raw cyclic paths ("parent.id" with no "parent" table) and an
        // extra Cartesian "node n" reference; it is no longer expanded, consistent with propToColumnInfoMap.
        @Test
        void testSubEntityTableSharedByTwoPropertiesIsRejectedAndSelfReferenceIsNotExpanded() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(SubEntityPerson.class, true));
            assertTrue(e.getMessage().contains("'homeAddress' and 'workAddress'") && e.getMessage().contains("'address ad'"), e.getMessage());
            assertThrows(IllegalArgumentException.class,
                    () -> AbstractQueryBuilder.buildFromTableRefs(SubEntityPerson.class, null, null, NamingPolicy.SNAKE_CASE));

            // Multi-selection path.
            assertThrows(IllegalArgumentException.class,
                    () -> PSC.selectFrom(List.of(Selection.builder(SubEntityPerson.class).tableAlias("p").includeSubEntityProperties(true).build())));
            assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.getFromClause(List.of(
                    Selection.builder(SubEntityPerson.class).tableAlias("p").includedPropNames(List.of("id", "homeAddress.city", "workAddress.city")).build()),
                    NamingPolicy.SNAKE_CASE));

            // One of the two properties alone is fine, and a sub-entity table that is also selected directly is listed once.
            assertEquals("SELECT p.id AS \"id\", ad.city AS \"homeAddress.city\" FROM person p, address ad", PSC
                    .selectFrom(List.of(Selection.builder(SubEntityPerson.class).tableAlias("p").includedPropNames(List.of("id", "homeAddress.city")).build()))
                    .build()
                    .query());
            assertEquals("person p, address ad",
                    AbstractQueryBuilder
                            .getFromClause(List.of(Selection.builder(SubEntityPerson.class).tableAlias("p").includedPropNames(List.of("homeAddress")).build(),
                                    Selection.builder(SubEntityAddress.class).tableAlias("ad").build()), NamingPolicy.SNAKE_CASE));

            // Self-referencing sub-entity: its cyclic paths are not selected and add no table.
            assertEquals("SELECT n.id AS \"id\", n.name AS \"name\" FROM node n", PSC.selectFrom(SubEntityNode.class, true).build().query());
            assertEquals("SELECT x.id AS \"id\", x.name AS \"name\" FROM node x", PSC.selectFrom(SubEntityNode.class, "x", true).build().query());
            assertEquals(Arrays.asList("node n"), AbstractQueryBuilder.buildFromTableRefs(SubEntityNode.class, null, null, NamingPolicy.SNAKE_CASE));
            assertEquals("node x", AbstractQueryBuilder.getFromClause(
                    List.of(Selection.builder(SubEntityNode.class).tableAlias("x").includeSubEntityProperties(true).build()), NamingPolicy.SNAKE_CASE));
            assertEquals(Arrays.asList("id", "name"), QueryUtil.selectPropNames(SubEntityNode.class, true, null));
        }

        @Test
        void testTableAliasNormalizationPreservesEmptyAliasesAndStripsAsPrefix() throws Exception {
            final var normalize = AbstractQueryBuilder.class.getDeclaredMethod("normalizeTableAlias", String.class);
            normalize.setAccessible(true);
            assertNull(normalize.invoke(null, (Object) null));
            assertEquals("", normalize.invoke(null, ""));
            assertEquals("p", normalize.invoke(null, "aS  p "));
            assertEquals("person", normalize.invoke(null, "person"));
        }

        @Test
        void testTableAliasRecognizedAcrossCommentsAfterAs() {
            for (final String trivia : List.of("/* alias */ ", "-- alias\n", "# alias\n")) {
                final String table = "people AS " + trivia + "p";
                final SqlBuilder builder = PSC.select("firstName").from(table);
                assertEquals("p", builder._tableAlias);
                assertSql("SELECT first_name AS \"firstName\" FROM " + table, List.of(), builder.build());
            }
        }

        @Test
        void testTokenizerConfigIsCarriedBySqlDialect() {
            final SqlParser.TokenizerConfig tokenizerConfig = SqlParser.tokenizerConfigBuilder().withSeparator("::").build();
            final Dsl dsl = Dsl.forDialect(Dsl.SCSB.sqlDialect().toBuilder().tokenizerConfig(tokenizerConfig).build());
            final SqlBuilder builder = dsl.select("payload::jsonb").from("events");

            assertEquals(tokenizerConfig, dsl.sqlDialect().tokenizerConfig());
            assertEquals(tokenizerConfig, builder._tokenizer.tokenizerConfig());
            assertTrue(builder.build().query().contains("payload::jsonb"));
        }

        @Test
        void testTopLevelAliasScannerBackslashEscapesOnlyInSingleQuotedLiterals() {
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
        void testTopLevelTableAliasDetectionIgnoresNonAliasSyntaxAndTrailingComments() {
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

        // MySQL compound INTERVAL units and Oracle binary floating-point types contain underscores but are keywords,
        // not snake_case column names: a naming policy must not camelCase them.
        @Test
        void testUnderscoreKeywordsNotConvertedByNamingPolicy() {
            assertTrue(PLC.select("*").from("t").where(Filters.expr("x > INTERVAL '1-2' YEAR_MONTH")).build().query().contains("YEAR_MONTH"));

            final String sql = PLC.select("CAST(x AS BINARY_DOUBLE)").from("t").orderBy("x + INTERVAL '1' DAY_HOUR").build().query();
            assertTrue(sql.contains("BINARY_DOUBLE"), sql);
            assertTrue(sql.contains("DAY_HOUR"), sql);
        }

        // Regression: Unicode whitespace before an explicit AS stayed in the expression (String.trim() keeps U+3000), so the property mapping was missed.
        @Test
        void testUnicodeWhitespaceBeforeExplicitSelectAliasKeepsPropertyMapping() {
            assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName\u3000AS\u3000fn").from(Account.class).build().query());
            assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName\u2003AS fn").from(Account.class).build().query());
            assertEquals("SELECT acc.first_name AS fn FROM account acc", PSC.select("firstName AS fn").from(Account.class).build().query());
        }

        @Test
        void testUnicodeWhitespaceIsNotPartOfTheTableAlias() {
            for (final String space : List.of("　", " ")) {
                final String query = PSC.select("id").from("review_parent u" + space, ExcludedChildParent.class).where(Filters.eq("u.id", 1)).build().query();
                assertTrue(query.startsWith("SELECT u.id AS \"id\" FROM review_parent u"), query);
                assertTrue(query.endsWith(" WHERE u.id = ?"), query);
            }
        }
    }

    @Nested
    class ParameterBindings {
        // Same suffix high-water mark through a derived table composed as a set-operation sibling.
        @Test
        @SuppressWarnings("deprecation")
        void testDerivedTableRetainsSkippedParameterSuffixesWhenComposedAsSibling() {
            for (final Dsl dsl : new Dsl[] { Dsl.NSC, Dsl.MSC }) {
                final SqlBuilder derived = dsl.select("id").from(compoundWithSkippedSuffix(dsl), "inner_query");
                final SP result = dsl.select("id").from("outer_table").where(Filters.eq("id_3", 4)).union(derived).build();

                assertDistinctBindings(result, List.of(4, 1, 2, 3));
            }
        }

        @Test
        void testEmptyCheckpointRestoresPartiallyRenderedParametersAcrossPolicies() {
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
        void testEmptyCustomTokenDuringChildRenameRestoresParentQuery() {
            final Dsl custom = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sql, name) -> {
                if (!name.endsWith("_2")) {
                    sql.append('@').append(name);
                }
            }).build());
            final SqlBuilder parent = custom.select("id").from("people").where(Filters.eq("id", 1));
            final SqlBuilder child = custom.select("id").from("archive").where(Filters.eq("id", 2));
            final IllegalStateException error = assertThrows(IllegalStateException.class, () -> parent.union(child));
            assertEquals("The custom named-parameter handler emitted an empty token for: id_2", error.getMessage());
            assertSql("SELECT id FROM people WHERE id = @id", List.of(1), parent.build());
        }

        @Test
        void testFix_setColumnNamesIbatisSqlSanitizesAliasAndDeduplicatesPlaceholders() {
            AbstractQueryBuilder.SP sp = Dsl.MSC.update("users").set("u.firstName").where(Filters.eq("u.firstName", "John")).build();
            String sql = sp.query();

            assertTrue(sql.contains("#{firstName}"), "SET placeholder should be sanitized: " + sql);
            assertTrue(sql.contains("#{firstName_2}"), "WHERE placeholder should be de-duplicated: " + sql);
            assertFalse(sql.contains("#{u.firstName}"), "Raw dotted placeholder is invalid: " + sql);
            assertEquals(Arrays.asList("John"), sp.parameters());
        }

        @Test
        void testFix_setColumnNamesNamedSqlDeduplicatesPlaceholders() {
            AbstractQueryBuilder.SP sp = NSC.update("users").set("status").where(Filters.eq("status", "OLD")).build();

            assertEquals("UPDATE users SET status = :status WHERE status = :status_2", sp.query());
            assertEquals(Arrays.asList("OLD"), sp.parameters());
        }

        // IS / IS NOT with a Boolean is normalized to the SQL keyword expression: no bind parameter on any policy,
        // and the rendering is identical to Filters.isTrue/isFalse (the keyword's letter case follows the naming
        // policy exactly as it does for isTrue/isFalse; it is never bound as a value).
        @Test
        void testIsWithBooleanRendersSqlLiteralWithoutParameter() {
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

        @Test
        void testMybatisBindingMetadataIsPreservedDuringExpressionConversion() {
            final String expression = "firstName = #{ firstName, jdbcType=VARCHAR } AND lastName = ${ lastName }";
            final String expected = "first_name = #{ firstName, jdbcType=VARCHAR } AND last_name = ${ lastName }";
            assertEquals(expected, PSC.renderCondition(Filters.expr(expression)).build().query());
            assertEquals("SELECT * FROM people WHERE " + expected, PSC.select("*").from("people").where(expression).build().query());

            // A quoted '}' inside marker attributes does not end the marker.
            assertEquals("SELECT * FROM people WHERE foo_bar = #{a, typeHandler='}'} AND bar_baz = 1",
                    PSC.select("*").from("people").where("fooBar = #{a, typeHandler='}'} AND barBaz = 1").build().query());
        }

        // Covers the MySQL dash-pair rewrite ("a--1" is a - (-1) there) at its documented boundaries: MyBatis markers, block
        // comments and backslash-escaped "..." strings are not rewritten, "--" followed by a tab is still a comment, and the scan
        // stops at the first '[' (a known partial fix: the later "d--1 > 0" is still read as a comment).
        @Test
        void testMySqlDashPairRewriteSkipsMarkersCommentsAndStrings() {
            final Dsl mysql = lexDialect(PSC, "MySQL");

            // The marker is not rewritten; its nonsensical "--" name then still reads as a comment (pre-existing).
            assertEquals("SELECT id FROM t WHERE a = #{x", mysql.select("id").from("t").where(Filters.expr("a = #{x--y} AND b--1 > 0")).build().query());
            assertEquals("SELECT id FROM t WHERE a = 1 AND b- -1 > 0",
                    mysql.select("id").from("t").where(Filters.expr("a /* a--1 */ = 1 AND b--1 > 0")).build().query());
            assertEquals("SELECT id FROM t WHERE n = \"a\\\"--1\" AND b- -1 > 0",
                    mysql.select("id").from("t").where(Filters.expr("n = \"a\\\"--1\" AND b--1 > 0")).build().query());
            assertEquals("SELECT id FROM t WHERE a = 1  ORDER BY id",
                    mysql.select("id").from("t").where(Filters.expr("a = 1 --\tc")).orderBy("id").build().query());
            assertEquals("SELECT id FROM t WHERE c- -1 > arr[1] AND d",
                    mysql.select("id").from("t").where(Filters.expr("c--1 > arr[1] AND d--1 > 0")).build().query());
        }

        @Test
        void testNamedParameterHandlerOnDialect() {
            final Dsl dsl = Dsl
                    .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, propName) -> sb.append("#{").append(propName).append("}")).build());
            final String sql = dsl.select("name").from("users").where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("#{id}"));
            assertTrue(NSC.select("name").from("users").where(Filters.eq("id", 1)).build().query().contains(":id"));
        }

        @Test
        void testNamedParameterHandlersRemainIsolatedDuringParallelUse() {
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
        void testParameterNameSanitizationPreservesCornerCases() {
            // Returning valid names directly must retain the old normalization of every other shape.
            final String[][] cases = { { "firstName", "firstName" }, { " u.id ", "id" }, { "COUNT(*)", "COUNT" }, { "___", "param" },
                    { "_leading", "_leading" }, { "trailing__", "trailing" }, { "1abc", "p1abc" }, { "  ", "param" }, { "a@#$b", "a_b" },
                    { "éclair", "éclair" }, { "猫名", "猫名" }, { "u.col_", "col" }, { "..", "param" }, { "", "" } };
            for (final String[] item : cases) {
                assertEquals(item[1], AbstractQueryBuilder.sanitizeNamedParameterName(item[0]), item[0]);
            }
            assertNull(AbstractQueryBuilder.sanitizeNamedParameterName(null));
        }

        @Test
        void testParameterRenderingHooksPreserveUnboundMarkersAndExpressions() {
            final SqlBuilder raw = SCSB.select("*").from("people");
            raw._sb.append(" WHERE id = ");
            raw.setParameterForRawSql(Filters.QME);
            assertSql("SELECT * FROM people WHERE id = ?", List.of(), raw.build());

            final SqlBuilder named = NSC.select("*").from("people");
            named._sb.append(" WHERE id IN (");
            named.setParameterForNamedSql("id", Filters.QME);
            named._sb.append(", ");
            named.setParameterForNamedSql("id", 7);
            named._sb.append(')');
            assertSql("SELECT * FROM people WHERE id IN (:id, :id_2)", List.of(7), named.build());

            final SqlBuilder ibatis = MSC.select("*").from("people");
            ibatis._sb.append(" WHERE id = ");
            ibatis.setParameterForIbatisNamedSql("id", Filters.QME);
            ibatis._sb.append(" AND created_at < ");
            ibatis.setParameterForIbatisNamedSql("createdAt", Filters.expr("CURRENT_TIMESTAMP"));
            assertSql("SELECT * FROM people WHERE id = #{id} AND created_at < CURRENT_TIMESTAMP", List.of(), ibatis.build());
        }

        @Test
        void testRawPlaceholderMismatchReportsInlineLiteralPolicy() {
            final SqlBuilder builder = SCSB.select("id").from("people");
            final var error = assertThrows(IllegalArgumentException.class, () -> builder.renameRawSubQueryPlaceholders("SELECT ?", List.of(1, 2)));
            assertTrue(error.getMessage().contains("inline RAW_SQL literals"));
            assertSql("SELECT id FROM people", List.of(), builder.build());
        }

        @Test
        @SuppressWarnings("deprecation")
        void testRawSqlInlinedExpressionBindingLineCommentIsTerminated() {
            final SubQuery subQuery = Filters.subQuery("SELECT id FROM u WHERE a = ? AND b = 2", List.of(SqlExpression.of("x -- c")));
            assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE a = x -- c\n AND b = 2)",
                    SCSB.select("id").from("t").where(Filters.in("id", subQuery)).build().query());
        }

        @Test
        void testRawSubqueryRewriteRejectsMismatchedBindingsBeforeAllocatingNames() {
            final SqlBuilder builder = NSC.select("id").from("people");
            assertThrows(IllegalArgumentException.class, () -> builder.renameRawSubQueryPlaceholders("SELECT ?", List.of(1, 2)));
            assertThrows(IllegalArgumentException.class, () -> builder.renameRawSubQueryPlaceholders("SELECT 1", List.of(1)));
            assertSql("SELECT id FROM people WHERE id = :id", List.of(7), builder.where(Filters.eq("id", 7)).build());
        }

        /**
         * Regression test: {@link AbstractQueryBuilder#sanitizeNamedParameterName(String)}
         * strips a table-alias prefix so the returned identifier can be used as a named
         * parameter in JDBC / MyBatis / Spring named SQL.
         */
        @Test
        void testSanitizeNamedParameterName_stripsTableAliasPrefix() {
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

        @Test
        void testSPDefensivelyCopiesWrappedParameterList() {
            final List<Object> source = new ArrayList<>();
            source.add(1);

            final AbstractQueryBuilder.SP sqlPair = new AbstractQueryBuilder.SP("SELECT ?", ImmutableList.wrap(source));
            source.add(2);

            assertEquals(Collections.singletonList(1), sqlPair.parameters());
            assertThrows(IllegalArgumentException.class, () -> new AbstractQueryBuilder.SP(null, ImmutableList.empty()));
            assertThrows(IllegalArgumentException.class, () -> new AbstractQueryBuilder.SP("SELECT 1", null));
        }

        @Test
        void testUnterminatedMybatisMarkerDoesNotSuppressConversion() {
            assertEquals("SELECT * FROM people WHERE foo_bar = #{ foo_bar AND bar_baz = 1",
                    PSC.select("*").from("people").where("fooBar = #{ fooBar AND barBaz = 1").build().query());
        }
    }

    @Nested
    class Predicates {
        @Test
        void testAppendConditionAfterWhereThrowsDuplicateWhere() {
            assertThrows(IllegalStateException.class,
                    () -> PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.eq("name", "Alice")).build().query());
        }

        @Test
        void testAppendCriteriaAfterWhereThrowsWhenCriteriaHasWhere() {
            Criteria criteria = Criteria.builder().where(Filters.eq("name", "Alice")).build();

            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").where(Filters.eq("id", 1)).append(criteria).build().query());
        }

        @Test
        void testAppendIf_Condition() {
            final String withCondition = PSC.select("*").from("users").appendIf(true, Filters.eq("status", "ACTIVE")).build().query();
            final String withoutCondition = PSC.select("*").from("users").appendIf(false, Filters.eq("status", "ACTIVE")).build().query();

            assertTrue(withCondition.contains("status"));
            assertTrue(!withoutCondition.contains("status"));
        }

        @Test
        void testAppendIfOrElse_Condition() {
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
        void testAppendLimitConditionWithExpression() {
            String sql = PSC.select("*").from("users").append(new Limit("10 OFFSET 20")).build().query();
            assertTrue(sql.endsWith("LIMIT 10 OFFSET 20"));
        }

        @Test
        void testAppendRejectsStructuralImplicitWhereOperandsWithoutMutation() {
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
        void testAppendWhereClauseAfterWhereThrows() {
            assertThrows(IllegalStateException.class,
                    () -> PSC.select("*").from("users").where(Filters.eq("id", 1)).append(Filters.where(Filters.eq("name", "Alice"))).build().query());
        }

        @Test
        void testCommentOnlyRawPredicatesAreRejectedWithoutClaimingTheClause() {
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
        void testConditionClauseMethodsRejectNonPredicatesWithoutMutation() {
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
            assertEquals("SELECT id FROM users INNER JOIN accounts ON 1 = 1",
                    PSC.select("id").from("users").innerJoin("accounts").on(Filters.and()).build().query());
        }

        @Test
        void testIntoRenderingFailuresAreAtomic() {
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
        void testPSCWithHaving() {
            String sql = PSC.select("department", "COUNT(*)").from("employees").groupBy("department").having(Filters.expr("COUNT(*) > 5")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("HAVING"));
        }

        @Test
        void testPSCWithMultipleConditions() {
            String sql = PSC.select("*").from(Account.class).where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testPSCWithWhere() {
            String sql = PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("WHERE"));
        }

        // Regression: the builder collapsed the line break between two adjacent string literals ('hello '\n'world') into a
        // space, which PostgreSQL rejects; ParsedSql already kept it.
        @Test
        void testRawExpressionKeepsLineBreakBetweenAdjacentStringLiterals() {
            assertEquals("SELECT id FROM t WHERE msg = 'hello '\n'world'",
                    PSC.select("id").from("t").where(Filters.expr("msg = 'hello '\n'world'")).build().query());
            assertEquals("SELECT 'hello '\n'world' AS m FROM t", PSC.select("'hello '\n'world' AS m").from("t").build().query());
            assertEquals("SELECT id FROM t WHERE msg = 'a'\n'b'",
                    PSC.select("id").from("t").where(Filters.eq("msg", SqlExpression.of("'a' -- c\n'b'"))).build().query());
            assertEquals("SELECT id FROM t WHERE msg = 'a' 'b' AND first_name = 'x'",
                    PSC.select("id").from("t").where("msg = 'a' 'b'\nAND firstName = 'x'").build().query());
        }

        // The predicate rules are shared with ON, but a rejection must name the clause actually being built.
        @Test
        void testRejectedPredicateMessageNamesClause() {
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

        /**
         * Regression test: a {@code null} {@link Condition} passed to {@code where(Condition)},
         * {@code having(Condition)}, {@code on(Condition)}, or {@code append(Condition)} must
         * fail fast with {@link IllegalArgumentException}. Previously these methods silently
         * fell through to {@code appendCondition(null)}, producing malformed SQL containing
         * a bare {@code WHERE}/{@code HAVING}/{@code ON} keyword followed by no expression.
         */
        @Test
        void testWhereHavingOnAppendRejectNullCondition() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").where((Condition) null).build().query());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy("id").having((Condition) null).build().query());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users u").join("orders o").on((Condition) null).build().query());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").append((Condition) null).build().query());
        }

        @Test
        void testWhereRejectsNullStringExpression() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").where((String) null));
        }

        @Test
        void testWhereWithOr() {
            String sql = PSC.select("*").from(Account.class).where(Filters.eq("status", "active").or(Filters.eq("status", "pending"))).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("OR"));
        }
    }

    @Nested
    class ProjectionAndOrdering {
        @Test
        void testAppendLimitAfterFetchReportsLimitAlreadySet() {
            final IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> PSC.select("*").from("t").fetchFirstRows(3).append(new Limit("FETCH FIRST 99999999999 ROWS ONLY")));
            assertEquals("'LIMIT' has already been set and cannot be set again", e.getMessage());

            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("t").limit(3).append(new Limit("LIMIT 99999999999")));
        }

        @Test
        void testAppendLimitExpressionAfterLimitThrows() {
            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").limit(10).append(new Limit("5")).build().query());
        }

        @Test
        void testCollectionGroupingAndOrderingKeepColumnOrder() {
            assertSql("SELECT first_name AS \"firstName\", last_name AS \"lastName\" FROM people GROUP BY last_name, first_name ORDER BY first_name, last_name",
                    List.of(),
                    PSC.select("firstName", "lastName")
                            .from("people")
                            .groupBy(List.of("lastName", "firstName"))
                            .orderBy(List.of("firstName", "lastName"))
                            .build());
            assertSql("SELECT * FROM people ORDER BY first_name ASC", List.of(), PSC.select("*").from("people").orderByAsc("firstName").build());
            assertSql("SELECT * FROM people ORDER BY first_name ASC, last_name ASC", List.of(),
                    PSC.select("*").from("people").orderByAsc(List.of("firstName", "lastName")).build());
            assertSql("SELECT * FROM people ORDER BY first_name DESC, last_name DESC", List.of(),
                    PSC.select("*").from("people").orderByDesc(List.of("firstName", "lastName")).build());
        }

        @Test
        void testComposedFragmentsDistinguishTemporaryTablesFromHashComments() {
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

        // Regression: distinctOn checked its argument (an unterminated block comment) before the statement, so a non-SELECT
        // builder reported an IllegalArgumentException instead of the IllegalStateException every other path gives.
        @Test
        void testDistinctOnChecksStatementBeforeArgument() {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> PSC.update("t").distinctOn("/* x"));
            assertEquals("selectModifier() is only valid for SELECT queries", e.getMessage());

            e = assertThrows(IllegalStateException.class, () -> PSC.select("a").distinct().distinctOn("/* x"));
            assertEquals("selectModifier has already been set and cannot be set again", e.getMessage());

            assertThrows(IllegalArgumentException.class, () -> PSC.select("a").distinctOn("/* x"));
            assertEquals("SELECT DISTINCT ON (a) a FROM t", PSC.select("a").distinctOn("a").from("t").build().query());
        }

        // Regression: distinctOn("/* x") -- an unclosed block comment, most likely a typo -- silently became a plain DISTINCT.
        @Test
        void testDistinctOnRejectsUnterminatedBlockComment() {
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
        void testDistinctOnRejectsUnterminatedNestedBlockComment() {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.select("a").distinctOn("id /* outer /* inner */"));
            assertTrue(e.getMessage().contains("unterminated block comment"), e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> Criteria.builder().distinctOn("id /* outer /* inner */"));

            // Balanced nesting is closed under every reading.
            assertEquals("SELECT DISTINCT ON (id /* a /* b */ */) a FROM t WHERE b = 1",
                    PSC.select("a").distinctOn("id /* a /* b */ */").from("t").where("b = 1").build().query());
        }

        // Regression: distinctOn() wrapped a trailing line comment inside the parentheses (commenting out ')'), and a
        // comment-only expression list rendered the invalid "DISTINCT ON ()".
        @Test
        void testDistinctOnTerminatesTrailingLineCommentAndTreatsCommentOnlyAsBlank() {
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

        @Test
        void testEmptySelectModifierRemainsNoOpAfterModifierWasSet() {
            final SqlBuilder builder = PSC.select("id").distinct();

            builder.selectModifier(null).selectModifier("");

            assertEquals("SELECT DISTINCT id FROM users", builder.from("users").build().query());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").distinct().selectModifier("   "));
        }

        @Test
        void testFetchFirstRows() {
            final String sql = PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build().query();

            assertTrue(sql.contains("FETCH FIRST 10 ROWS ONLY"));
        }

        @Test
        void testFetchNextRows() {
            final String sql = PSC.select("*").from("users").orderBy("id").offsetRows(0).fetchNextRows(10).build().query();

            assertTrue(sql.contains("FETCH NEXT 10 ROWS ONLY"));
        }

        @Test
        void testFetchNextRowsAndFetchFirstRowsAreMutuallyExclusive() {
            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").orderBy("id").fetchNextRows(10).fetchFirstRows(5).build().query());
            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).fetchNextRows(5).build().query());
        }

        @Test
        void testFromRenderingFailuresAreAtomic() {
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
        void testFromTrailingLineCommentDoesNotSwallowLaterClauses() {
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
            assertEquals("SELECT * FROM account a /* hint */ WHERE id = ?",
                    PSC.select("*").from("account a /* hint */").where(Filters.eq("id", 1)).build().query());
            assertEquals("SELECT * FROM \"a -- b\" x WHERE id = ?", PSC.select("*").from("\"a -- b\" x").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        void testGroupBy_mapIterationOrder() {
            final Map<String, SortDirection> groupings = new LinkedHashMap<>();
            groupings.put("category", SortDirection.ASC);
            groupings.put("brand", SortDirection.DESC);

            final String sql = PSC.select("category", "brand").from("products").groupBy(groupings).build().query();

            assertTrue(sql.indexOf("category ASC") < sql.indexOf("brand DESC"), sql);
        }

        @Test
        void testGroupByOrderByWithDirectionReportBlankExpr() {
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
        void testGroupByRejectsEmptyInputs() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy().build().query());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy(Collections.emptyList()).build().query());
        }

        @Test
        void testLimitWithOffset() {
            String sql = PSC.select("*").from(Account.class).limit(20, 10).build().query();
            assertNotNull(sql);
        }

        @Test
        void testLimitWithOffsetRejectsSecondOffset() {
            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").limit(10, 5).offset(2));
        }

        @Test
        void testMultipleOrderBy() {
            String sql = PSC.select("*").from(Account.class).orderBy("lastName", "firstName").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("ORDER BY"));
        }

        // Regression: expression items of a non-first Selection were resolved against the FIRST selection's entity and table alias ("a.status + 1" for selection "d").
        @Test
        void testMultiSelectExpressionItemsResolveAgainstTheirOwnSelection() {
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

        // Covers the multi-selection FROM dedupe: identical selections are listed once, the same class under different
        // aliases is kept twice (a self join), and a sub-entity table that a later selection also lists is not repeated.
        @Test
        void testMultiSelectFromClauseListsEachTableReferenceOnce() {
            assertEquals("account a",
                    AbstractQueryBuilder.getFromClause(
                            List.of(Selection.builder(Account.class).tableAlias("a").build(), Selection.builder(Account.class).tableAlias("a").build()),
                            NamingPolicy.SNAKE_CASE));
            assertEquals("account a, account b",
                    AbstractQueryBuilder.getFromClause(
                            List.of(Selection.builder(Account.class).tableAlias("a").build(), Selection.builder(Account.class).tableAlias("b").build()),
                            NamingPolicy.SNAKE_CASE));
            assertEquals("account acc, device",
                    AbstractQueryBuilder.getFromClause(List.of(Selection.builder(Account.class).tableAlias("acc").includeSubEntityProperties(true).build(),
                            Selection.builder(com.landawn.abacus.query.entity.AccountDevice.class).build()), NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testOffsetRows() {
            final String sql = PSC.select("*").from("users").orderBy("id").offsetRows(20).build().query();

            assertTrue(sql.contains("OFFSET 20 ROWS"));
        }

        @Test
        void testOrderBy_mapIterationOrder() {
            final Map<String, SortDirection> orders = new LinkedHashMap<>();
            orders.put("lastName", SortDirection.ASC);
            orders.put("firstName", SortDirection.DESC);

            final String sql = PSC.select("*").from("users").orderBy(orders).build().query();

            assertTrue(sql.indexOf("ASC") < sql.indexOf("DESC"), sql);
        }

        @Test
        void testOrderByAsc() {
            String sql = PSC.select("*").from(Account.class).orderBy("firstName", SortDirection.ASC).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        void testOrderByDesc() {
            String sql = PSC.select("*").from(Account.class).orderBy("createdTime", SortDirection.DESC).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        void testOrderByRejectsBlockAndHashCommentTokens() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy("id/*comment*/").build().query());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy("id#comment").build().query());
        }

        @Test
        void testOrderByRejectsCommentToken() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy("id--").build().query());
        }

        @Test
        void testOrderByRejectsEmptyInputs() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy().build().query());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy(Collections.emptyList()).build().query());
        }

        @Test
        void testPSCSelectFrom() {
            assertSql("SELECT acc.id AS \"id\", acc.first_name AS \"firstName\", acc.last_name AS \"lastName\" FROM account acc", List.of(),
                    PSC.select("id", "firstName", "lastName").from(Account.class).build());
        }

        @Test
        void testPSCWithDistinct() {
            String sql = PSC.select("status").from(Account.class).distinct().build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        void testPSCWithGroupBy() {
            String sql = PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("GROUP BY"));
        }

        @Test
        void testPSCWithLimit() {
            String sql = PSC.select("*").from(Account.class).limit(10).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testPSCWithOrderBy() {
            String sql = PSC.select("*").from(Account.class).orderBy("firstName").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        void testRowLimitApisRejectNegativeValues() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").limit(-1));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").limit(10, -1));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").offset(-1));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").offsetRows(-1));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").fetchNextRows(-1));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").fetchFirstRows(-1));
        }

        @Test
        void testSelectAll() {
            String sql = PSC.select(AbstractQueryBuilder.ALL).from(Account.class).build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectAllowsCommentLikeTokenInsideQuotedLiteral() {
            String sql = PSC.select("CASE WHEN note = '--literal' THEN 1 ELSE 0 END").from("docs").build().query();
            assertTrue(sql.contains("'--literal'"));
        }

        @Test
        void testSelectAllowsHashJsonOperators() {
            String sql = PSC.select("payload #>> '{meta,status}'").from("docs").build().query();
            assertTrue(sql.contains("#>>"));
        }

        @Test
        void testSelectCount() {
            String sql = PSC.select(AbstractQueryBuilder.COUNT_ALL).from(Account.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("count(*)"));
        }

        @Test
        void testSelectModifier() {
            final String sql = PSC.select("*").selectModifier("TOP 5").from("users").build().query();

            assertTrue(sql.contains("SELECT TOP 5"));
        }

        // A select modifier ending in a line comment must not swallow the select list and later clauses,
        // whether it is emitted by from(...), spliced in after from(...), or carried by an appended Criteria.
        @Test
        void testSelectModifierEndingInLineCommentIsTerminated() {
            final String expected = "SELECT DISTINCT -- x\n id, name FROM t WHERE a = ?";

            assertEquals(expected, PSC.select("id", "name").selectModifier("DISTINCT -- x").from("t").where(Filters.eq("a", 1)).build().query());
            assertEquals(expected, PSC.select("id", "name").from("t").selectModifier("DISTINCT -- x").where(Filters.eq("a", 1)).build().query());
            assertEquals(expected,
                    PSC.select("id", "name")
                            .from("t")
                            .append(Criteria.builder().selectModifier("DISTINCT -- x").where(Filters.eq("a", 1)).build())
                            .build()
                            .query());

            // A modifier without a trailing comment is unchanged.
            assertEquals("SELECT DISTINCT id FROM t", PSC.select("id").from("t").selectModifier("DISTINCT").build().query());
        }

        @Test
        void testSelectWithMultipleTables() {
            String sql = PSC.select("*").from("users", "orders").build().query();
            assertNotNull(sql);
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
        void testSelectWithSubEntities_DoesNotThrow() {
            // Force the include-sub-entities select path. Using PSC.selectFrom(Class, boolean)
            // exercises both helpers that previously dereferenced propInfo without a null check.
            String sql = PSC.selectFrom(Account.class, true).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"), "Should produce a SELECT");
            assertTrue(sql.contains("FROM"), "Should produce a FROM");
        }

        @Test
        void testSqlServerPaginationRequiresOrderByAndUsesValidOffsetFetchGrammar() {
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
            assertEquals("SELECT id FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 4 ROWS ONLY",
                    explicitFirst.orderBy("id").fetchFirstRows(4).build().query());

            assertThrows(IllegalArgumentException.class, () -> new Limit("FETCH FIRST ? ROWS ONLY"));

            final Dsl oracleDsl = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Oracle")).build());
            assertEquals("SELECT id FROM users FETCH FIRST 2 ROWS ONLY", oracleDsl.select("id").from("users").limit(2).build().query());

            final Dsl db2Dsl = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("DB2")).build());
            assertEquals("SELECT id FROM users FETCH FIRST 2 ROWS ONLY", db2Dsl.select("id").from("users").limit(2).build().query());
        }

        @Test
        void testStandaloneClausesRequireFromAndPreserveSqlClauseOrder() {
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
        void testUnresolvedCommaLimitIsRenderedPortably() {
            final Dsl postgres = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
            assertEquals("SELECT * FROM users LIMIT 99999999999 OFFSET 5",
                    postgres.select("*").from("users").append(new Limit("LIMIT 5, 99999999999")).build().query());
            assertEquals("SELECT * FROM users LIMIT 99999999999 OFFSET 5",
                    PSC.select("*").from("users").append(new Limit("LIMIT 5, 99999999999")).build().query());
            assertEquals("SELECT * FROM users LIMIT 99999999999", postgres.select("*").from("users").append(new Limit("LIMIT 99999999999")).build().query());
        }
    }

    @Nested
    class RenderingContracts {
        @Test
        void testAppendCriteriaPreflightsRelativeClauseOrderWithoutMutation() {
            assertCriteriaRejectedWithoutMutation(IllegalStateException.class, PSC.select("department").from("users").groupBy("department"),
                    Criteria.builder().where(Filters.eq("active", true)).build(), "SELECT department FROM users GROUP BY department");
            assertCriteriaRejectedWithoutMutation(IllegalStateException.class,
                    PSC.select("department").from("users").groupBy("department").having("COUNT(*) > 1"), Criteria.builder().groupBy("region").build(),
                    "SELECT department FROM users GROUP BY department HAVING COUNT(*) > 1");
            assertCriteriaRejectedWithoutMutation(IllegalStateException.class, PSC.select("id").from("users").orderBy("id"),
                    Criteria.builder().having(Filters.gt("COUNT(*)", 1)).build(), "SELECT id FROM users ORDER BY id");
            assertCriteriaRejectedWithoutMutation(IllegalStateException.class, PSC.select("id").from("users").limit(10),
                    Criteria.builder().orderBy("id").build(), "SELECT id FROM users LIMIT 10");
            assertCriteriaRejectedWithoutMutation(IllegalStateException.class, PSC.select("id").from("users").forUpdate(), Criteria.builder().limit(10).build(),
                    "SELECT id FROM users FOR UPDATE");
        }

        @Test
        void testAppendIf_String() {
            final String withExpression = PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(true, " FOR UPDATE").build().query();
            final String withoutExpression = PSC.select("*").from("users").where(Filters.eq("id", 1)).appendIf(false, " FOR UPDATE").build().query();

            assertTrue(withExpression.contains("FOR UPDATE"));
            assertTrue(!withoutExpression.contains("FOR UPDATE"));
        }

        @Test
        void testAppendIfOrElse_String() {
            final String asc = PSC.select("*").from("users").appendIfOrElse(true, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
            final String desc = PSC.select("*").from("users").appendIfOrElse(false, " ORDER BY name ASC", " ORDER BY name DESC").build().query();

            assertTrue(asc.contains("ORDER BY name ASC"));
            assertTrue(desc.contains("ORDER BY name DESC"));
        }

        // append(String) used to write straight into the buffer without priming it, so an append that was the
        // first write permanently suppressed the lazily-emitted "DELETE FROM t" / "UPDATE t SET " prefix and
        // build() returned the bare fragment as the whole statement.
        @Test
        void testAppendStringAsFirstWriteStillEmitsTheStatementPrefix() {
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

        // Regression: append(String) emitted a trailing line comment raw, so the next structured clause (a DELETE's WHERE,
        // a placeholder-bearing WHERE) was silently commented out.
        @Test
        void testAppendTerminatesTrailingLineComment() {
            assertEquals("DELETE FROM account /* job */ -- audit\n WHERE id = 1",
                    SCSB.deleteFrom("account").append("/* job */ -- audit").where(Filters.eq("id", 1)).build().query());
            assertEquals("SELECT * FROM t -- note\n WHERE a = ?", PSC.select("*").from("t").append("-- note").where(Filters.eq("a", 1)).build().query());

            // No trailing line comment: unchanged.
            assertEquals("SELECT * FROM t FOR UPDATE", PSC.select("*").from("t").append("FOR UPDATE").build().query());
            assertEquals("SELECT * FROM t /* hint */ WHERE a = ?", PSC.select("*").from("t").append("/* hint */").where(Filters.eq("a", 1)).build().query());
        }

        @Test
        void testChainedAndOr() {
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
        void testChainedSetCollectionCallsIncludeComma() {
            String sql = PSC.update("users").set("firstName").set("lastName").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("first_name = ?"), "first_name assignment missing: " + sql);
            assertTrue(sql.contains("last_name = ?"), "last_name assignment missing: " + sql);
            int firstIdx = sql.indexOf("first_name");
            int commaIdx = sql.indexOf(',', firstIdx);
            int secondIdx = sql.indexOf("last_name");
            assertTrue(commaIdx > 0 && commaIdx < secondIdx, "Comma must separate the two SET assignments: " + sql);
        }

        @Test
        void testChainedSetMapCallsIncludeComma() {
            java.util.Map<String, Object> m1 = java.util.Collections.singletonMap("firstName", "John");
            java.util.Map<String, Object> m2 = java.util.Collections.singletonMap("lastName", "Doe");
            String sql = PSC.update("users").set(m1).set(m2).where(Filters.eq("id", 1)).build().query();
            int firstIdx = sql.indexOf("first_name");
            int commaIdx = sql.indexOf(',', firstIdx);
            int secondIdx = sql.indexOf("last_name");
            assertTrue(commaIdx > 0 && commaIdx < secondIdx, "Comma must separate map-based SET assignments: " + sql);
        }

        @Test
        void testCheckpointRestoresPrefixModifiedByCustomNamedRenderer() {
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

            // Under every other policy the dialect's handler is never invoked (which is why the checkpoint does not copy
            // the SQL for it there), and a failed clause is still restored exactly.
            final Dsl parameterized = Dsl.forDialect(PSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
                throw new AssertionError("named-parameter handler invoked for " + name);
            }).build());
            final SqlBuilder psc = parameterized.update("users").set("id", 1);
            final String pscBefore = psc._sb.toString();
            assertThrows(IllegalArgumentException.class, () -> psc.set("id -- c", 2));
            assertEquals(pscBefore, psc._sb.toString());
            final AbstractQueryBuilder.SP pscSp = psc.set("id", 2).where(Filters.eq("id", 3)).build();
            assertEquals("UPDATE users SET id = ?, id = ? WHERE id = ?", pscSp.query());
            assertEquals(Arrays.asList(1, 2, 3), pscSp.parameters());
        }

        // Regression: the comment guard always read \' as an escaped quote, so "'C:\' -- note" passed it on standard-string dialects and swallowed the WHERE.
        @Test
        void testCommentGuardChecksStandardStringReadingOutsideMySql() {
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

        // Pins the fail-closed choice of the comment guard without product info: DEFAULT may be any server, and under the
        // standard string reading (no backslash escapes) "'it\'s -- fine'" ends at "\'", leaving a real "--" comment.
        @Test
        void testCommentGuardRejectsMySqlStyleEscapedLiteralWithoutProductInfo() {
            assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set("note = 'it\\'s -- fine'"));

            final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
            assertEquals("UPDATE t SET note = 'it\\'s -- fine' WHERE a = ?",
                    mysql.update("t").set("note = 'it\\'s -- fine'").where(Filters.eq("a", 1)).build().query());
        }

        // Covers the comment guard's string readings (near misses stay accepted; H2/SQLite and non-E prefixes are standard)
        // and MySQL hash handling inside quotes and backticks.
        @Test
        void testCommentGuardStringReadingNearMissesAndQuotedHashes() {
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

        // Regression: with an explicit MySQL dialect the comment guard still exempted ##, #>, #- and ?#, which MySQL reads as comments.
        @Test
        void testCommentGuardTreatsEveryHashAsCommentUnderMySql() {
            final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());

            assertThrows(IllegalArgumentException.class, () -> mysql.update("account").set("status = 1 ## reset").where("status = 0"));
            assertThrows(IllegalArgumentException.class, () -> mysql.select("a #> b").from("t"));
            assertThrows(IllegalArgumentException.class, () -> mysql.select("a #-b").from("t"));
            assertThrows(IllegalArgumentException.class, () -> mysql.select("a ?# b").from("t"));
            assertThrows(IllegalArgumentException.class, () -> mysql.select("a#>>b").from("t"));

            // A MyBatis marker is still a data token, and PostgreSQL keeps its hash operators.
            assertEquals("UPDATE account SET status = #{status} WHERE status = 0",
                    mysql.update("account").set("status = #{status}").where("status = 0").build().query());
            final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
            assertEquals("SELECT a ## b FROM t", pg.select("a ## b").from("t").build().query());
        }

        @Test
        void testCommentTokenAfterBackslashEscapedQuoteInsideSingleQuotedLiteralIsRejected() {
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
        void testCommentTokenAfterBackslashTerminatedQuotedIdentifierIsRejected() {
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
        void testCommentTokensInsideSqlServerBracketIdentifierAreAllowed() {
            assertEquals("SELECT [a--b] FROM records", Dsl.PSB.select("[a--b]").from("records").build().query());
            assertEquals("SELECT [a]]--b] FROM records", Dsl.PSB.select("[a]]--b]").from("records").build().query());
        }

        @Test
        void testCompactCheckpointsCopyValuesAndRestoreNestedMutations() {
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
                // The protected entry point restores text rewritten before the start length, not only appended text.
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
            // The default ":id" token is not stored (absence means the default form).
            assertNull(builder._renderedNamedParameterTokens.get("id"));
            assertEquals(Set.of("id"), builder._generatedNamedParameterNames);
            assertTrue(builder.calledOpSet.isEmpty());
            builder._namedParameterNameOccurrences.remove(null);
            assertEquals("UPDATE users SET id = :id, id = :id_2", builder.set("id", 2).build().query());
        }

        @Test
        void testComposedFragmentsProtectStandardBackslashLiteralBoundaries() {
            assertEquals("SELECT * FROM users CROSS JOIN (SELECT 'a\\') a -- tail\n WHERE id = ?",
                    PSC.select("*").from("users").crossJoin("(SELECT 'a\\') a -- tail").where(Filters.eq("id", 1)).build().query());
            assertEquals("SELECT * FROM users CROSS JOIN (SELECT E'a\\' -- quoted') a WHERE id = ?",
                    PSC.select("*").from("users").crossJoin("(SELECT E'a\\' -- quoted') a").where(Filters.eq("id", 1)).build().query());

            final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
            assertEquals("SELECT * FROM users CROSS JOIN (SELECT 'a\\' -- quoted') a WHERE id = ?",
                    mysql.select("*").from("users").crossJoin("(SELECT 'a\\' -- quoted') a").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        void testComposedQueriesRejectTerminatorsAfterStandardBackslashLiterals() {
            final SqlBuilder parent = PSC.select("id").from("users");
            assertThrows(IllegalArgumentException.class, () -> parent.union("SELECT 'a\\';"));
            assertEquals("SELECT id FROM users", parent.build().query());

            final SqlBuilder snapshot = PSC.select("id").from("users").append("WHERE value = 'a\\';");
            assertThrows(IllegalArgumentException.class, snapshot::toSubQuery);
        }

        // Regression: the builder's scanners did not know dollar-quoted strings, so the quote in $$'$$ (text on PostgreSQL and
        // H2) read as an unterminated string that hid the comment after it: on H2, "UPDATE t SET a = $$'$$ // c WHERE id = ?"
        // updated every row, and "--" did the same on PostgreSQL, H2 and the default dialect. Raw expressions also collapsed
        // the whitespace of, and converted the words in, a dollar-quoted string ($$Hello World$$ became $$Hello world$$).
        @Test
        void testDollarQuotedStringsAreQuotedTextForCommentChecksAndRendering() {
            final Dsl h2 = lexDialect(PSC, "H2");
            final Dsl pg = lexDialect(PSC, "PostgreSQL");
            final Dsl mysql = lexDialect(PSC, "MySQL");

            // A trailing line comment after a dollar-quoted string is ended before the next clause.
            assertEquals("UPDATE t SET a = $$'$$ // c\n WHERE id = ?",
                    h2.update("t").set("a", SqlExpression.of("$$'$$ // c")).where(Filters.eq("id", 1)).build().query());
            assertEquals("UPDATE t SET a = $$'$$ -- c\n WHERE id = ?",
                    h2.update("t").set("a", SqlExpression.of("$$'$$ -- c")).where(Filters.eq("id", 1)).build().query());
            // H2 has no tagged dollar quotes: there "$t$" is no quote and the comment is inside the unterminated string.
            assertEquals("UPDATE t SET a = $t$'$t$ -- c WHERE id = ?",
                    h2.update("t").set("a", SqlExpression.of("$t$'$t$ -- c")).where(Filters.eq("id", 1)).build().query());

            for (final Dsl dsl : List.of(PSC, pg)) {
                assertEquals("UPDATE t SET a = $$'$$ -- c\n WHERE id = ?",
                        dsl.update("t").set("a", SqlExpression.of("$$'$$ -- c")).where(Filters.eq("id", 1)).build().query());
                assertEquals("UPDATE t SET a = $t$'$t$ -- c\n WHERE id = ?",
                        dsl.update("t").set("a", SqlExpression.of("$t$'$t$ -- c")).where(Filters.eq("id", 1)).build().query());
                assertEquals("SELECT a FROM t WHERE a = $$\"$$ -- c\n ORDER BY id",
                        dsl.select("a").from("t").where("a = $$\"$$ -- c").orderBy("id").build().query());
                assertEquals("UPDATE t SET a = ? WHERE (b = $$[$$ -- c\n) AND (id = ?)",
                        dsl.update("t").set("a", 1).where(Filters.and(Filters.expr("b = $$[$$ -- c"), Filters.eq("id", 1))).build().query());
                assertEquals("SELECT a FROM (SELECT $$'$$ AS v) s -- c\n WHERE x = ?",
                        dsl.select("a").from("(SELECT $$'$$ AS v) s -- c").where(Filters.eq("x", 1)).build().query());
                assertEquals("SELECT a FROM t JOIN u ON t.a = $$'$$ -- c\n WHERE id = ?",
                        dsl.select("a").from("t").join("u").on("t.a = $$'$$ -- c").where(Filters.eq("id", 1)).build().query());
                assertEquals("SELECT a FROM t GROUP BY a HAVING count(*) > $$'$$ -- c\n ORDER BY a",
                        dsl.select("a").from("t").groupBy("a").having("count(*) > $$'$$ -- c").orderBy("a").build().query());
                assertEquals("SELECT a FROM t WHERE a = $$'$$ -- c\n ORDER BY id",
                        dsl.select("a").from("t").append(" WHERE a = $$'$$ -- c").orderBy("id").build().query());
                assertEquals("SELECT a FROM t WHERE x IN (SELECT x FROM u WHERE y = $$'$$ -- c\n)",
                        dsl.select("a").from("t").where(Filters.in("x", Filters.subQuery("SELECT x FROM u WHERE y = $$'$$ -- c"))).build().query());

                // The comment guard finds the comment, as it does after an ordinary string.
                assertEquals("SQL comment token is not allowed in SQL expression: $$'$$ -- c",
                        assertThrows(IllegalArgumentException.class, () -> dsl.select("$$'$$ -- c").from("t")).getMessage());
                assertEquals("SQL comment token is not allowed in column expression: a = $$'$$ -- c",
                        assertThrows(IllegalArgumentException.class, () -> dsl.update("t").set("a = $$'$$ -- c")).getMessage());
                assertThrows(IllegalArgumentException.class, () -> dsl.select("a").from("t").orderBy("$$'$$ -- c"));

                // A dollar-quoted string renders as written; the names around it are still converted, and an AS inside it
                // splits off no alias.
                assertEquals("SELECT $$Hello World$$ AS greeting FROM t", dsl.select("$$Hello World$$ AS greeting").from("t").build().query());
                assertEquals("SELECT $$foo  as  bar$$ FROM users", dsl.select("$$foo  as  bar$$").from("users").build().query());
                assertEquals("SELECT acc.first_name AS \"firstName\" FROM account acc WHERE note = $$Hello World$$ AND acc.last_name = 1",
                        dsl.select("firstName").from(Account.class).where("note = $$Hello World$$ AND lastName = 1").build().query());
            }

            // The tokenizer can keep a dollar quote and the comment after it in one token: in a subscript, or after a non-ASCII
            // space, which the rendering emits as a plain space (H2 reads it as one anyway). The emitted text is checked too.
            for (final Dsl dsl : List.of(PSC, pg, h2)) {
                assertEquals("UPDATE t SET b = ARRAY[$$']$$] -- c\n WHERE id = ?",
                        dsl.update("t").set("b", SqlExpression.of("ARRAY[$$']$$] -- c")).where(Filters.eq("id", 1)).build().query());
                assertEquals("SELECT a FROM t WHERE a = x[1:$$']$$] -- c\n ORDER BY id",
                        dsl.select("a").from("t").where("a = x[1:$$']$$] -- c").orderBy("id").build().query());
                assertEquals("UPDATE t SET a = $$'$$ -- c\n WHERE id = ?",
                        dsl.update("t").set("a", SqlExpression.of("　$$'$$ -- c")).where(Filters.eq("id", 1)).build().query());
                assertEquals("UPDATE t SET a = 'x' || $$'$$ -- c\n WHERE id = ?",
                        dsl.update("t").set("a", SqlExpression.of("'x' || $$'$$ -- c")).where(Filters.eq("id", 1)).build().query());
                assertThrows(IllegalArgumentException.class, () -> dsl.update("t").set("a = 　$$'$$ -- c"));
                // A dollar-quoted string in an array constructor is kept as written too; the column after it is converted.
                assertEquals("SELECT a FROM t WHERE tags = ARRAY[$$a  b$$, first_name]",
                        dsl.select("a").from("t").where("tags = ARRAY[$$a  b$$, firstName]").build().query());
                // The "as" inside the string is no alias keyword: the item keeps its own alias and gets no automatic one
                // ("AS label AS \"a || ...\"" was a syntax error).
                assertEquals("SELECT a || $$ as of $$ AS label FROM t", dsl.select("a || $$ as of $$  AS label").from("t").build().query());
                assertEquals("SELECT a || $$ as of $$ AS label FROM t", dsl.select("a || $$ as of $$\n AS label").from("t").build().query());
                assertEquals("SELECT first_name || $$ as of $$ AS label FROM t", dsl.select("firstName || $$ as of $$ AS label").from("t").build().query());
            }

            // SQL Server, Oracle, DB2 and SQLite have no dollar quotes either: their scanners read none.
            for (final Dsl dsl : List.of(lexDialect(PSC, "Microsoft SQL Server"), lexDialect(PSC, "Oracle"), lexDialect(PSC, "SQLite"))) {
                assertEquals("UPDATE t SET a = $$'$$ -- c WHERE id = ?",
                        dsl.update("t").set("a", SqlExpression.of("$$'$$ -- c")).where(Filters.eq("id", 1)).build().query());
                assertEquals("SELECT $$'$$ -- c FROM t", dsl.select("$$'$$ -- c").from("t").build().query());
                assertEquals("SELECT $$foo AS bar$$ FROM t", dsl.select("$$foo  as  bar$$").from("t").build().query());
            }

            // The MySQL family has no dollar quotes: "$$" is a name there, and the quote opens a string.
            assertEquals("UPDATE t SET a = $$'$$ -- c WHERE id = ?",
                    mysql.update("t").set("a", SqlExpression.of("$$'$$ -- c")).where(Filters.eq("id", 1)).build().query());
            assertEquals("SELECT $$'$$ -- c FROM t", mysql.select("$$'$$ -- c").from("t").build().query());
            assertEquals("SELECT a FROM t WHERE a = $$a b$$", mysql.select("a").from("t").where("a = $$a  b$$").build().query());
            // The unterminated-block-comment check is dialect-agnostic and reads dollar quotes for MySQL too (fail closed).
            assertThrows(IllegalArgumentException.class, () -> mysql.select("a").from("t WHERE c = $$ OR d = '$$ /* x '"));
        }

        @Test
        void testDoubleHashNotTreatedAsSqlCommentInExpressions() {
            // ## is a whitelisted two-char token; the second # must not be re-examined as lone #
            String sql = PSC.select("*").from("users").where(Filters.expr("status = '##ACTIVE##'")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("##ACTIVE##"), "## inside value must not be rejected as SQL comment");
        }

        // Regression: the tokenizer reads \' as an escaped quote, so after a standard literal such as 'C:\' every quote boundary
        // was off by one: a later string was converted as an identifier, and "-- n/a" inside a later string was stripped
        // as a comment, truncating the WHERE clause.
        @Test
        void testEscapeDependentQuoteInRawExpressionIsEmittedVerbatim() {
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

        // The checkpoint of an append-only clause records only the buffer length (no copy of the SQL built so far per
        // clause call): a clause rejected after a wide select list must still leave the SQL byte-identical.
        @Test
        void testFailedClauseAfterWideSelectListRestoresSqlExactly() {
            final String[] columns = IntStream.range(0, 10_000).mapToObj(i -> "c" + i).toArray(String[]::new);
            final SqlBuilder builder = PSC.select(columns).from("t");
            final String before = builder._sb.toString();

            assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.eq("id -- c", 1)));
            assertEquals(before, builder._sb.toString());
            assertTrue(builder._parameters.isEmpty());
            assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.and(Filters.eq("id", 1), Filters.eq("id -- c", 2))));
            assertEquals(before, builder._sb.toString());
            assertTrue(builder._parameters.isEmpty());

            assertEquals(before + " WHERE id = ?", builder.where(Filters.eq("id", 1)).build().query());
        }

        // The Criteria select-modifier splice rewrites the SELECT prefix, so that mutation keeps the full SQL copy.
        @Test
        void testFailedCriteriaWithSelectModifierRestoresPrefixExactly() {
            final SqlBuilder builder = PSC.select("id").from("t");
            final String before = builder._sb.toString();

            // The column is rejected while the WHERE is rendered, i.e. after DISTINCT was spliced into the SELECT prefix:
            // restoring by truncation alone would leave "SELECT DISTINCT ".
            assertThrows(IllegalArgumentException.class, () -> builder.append(Criteria.builder().distinct().where(Filters.eq("id -- c", 1)).build()));
            assertEquals(before, builder._sb.toString());
            assertNull(builder._selectModifier);
            // appendIf(true, ...) goes through the same checkpoint.
            assertThrows(IllegalArgumentException.class, () -> builder.appendIf(true, Criteria.builder().distinct().where(Filters.eq("id -- c", 1)).build()));
            assertEquals(before, builder._sb.toString());
            assertNull(builder._selectModifier);
            assertEquals("SELECT DISTINCT id FROM t WHERE id = ?",
                    builder.append(Criteria.builder().distinct().where(Filters.eq("id", 1)).build()).build().query());

            // Named parameters rendered before the rejection are rolled back as well.
            final SqlBuilder named = NSC.select("a", "b", "c").from("t");
            final String namedBefore = named._sb.toString();
            assertThrows(IllegalArgumentException.class,
                    () -> named.append(Criteria.builder().distinct().where(Filters.and(Filters.eq("a", 1), Filters.eq("b -- c", 2))).build()));
            assertEquals(namedBefore, named._sb.toString());
            assertNull(named._selectModifier);
            assertTrue(named._parameters.isEmpty());
            final SP namedSp = named.append(Criteria.builder().distinct().where(Filters.eq("a", 5)).build()).build();
            assertEquals("SELECT DISTINCT a, b, c FROM t WHERE a = :a", namedSp.query());
            assertEquals(List.of(5), namedSp.parameters());

            // WHERE after ORDER BY is rejected by the pre-validation, before anything is spliced into the prefix.
            final SqlBuilder ordered = PSC.select("id").from("t").orderBy("id");
            final String orderedBefore = ordered._sb.toString();
            assertThrows(IllegalStateException.class, () -> ordered.append(Criteria.builder().distinct().where(Filters.eq("id", 1)).build()));
            assertEquals(orderedBefore, ordered._sb.toString());
            assertNull(ordered._selectModifier);
            assertEquals("SELECT DISTINCT id FROM t ORDER BY id", ordered.append(Criteria.builder().distinct().build()).build().query());
        }

        // Regression: the sort spec of a condition-layer ORDER BY / GROUP BY (Filters.orderBy/groupBy, Criteria.orderBy/groupBy) is
        // composed without knowing the dialect, so on H2 a "//" comment in a key swallowed that key's direction and every later key
        // ("ORDER BY a // c DESC, b ASC\n" ran as "ORDER BY a"). It is now rejected on H2, as orderBy(String)/groupBy(String) reject it.
        @Test
        void testH2ConditionLayerSortKeyWithDoubleSlashIsRejectedLikeBuilderOrderBy() {
            final Dsl h2 = lexDialect(PSC, "H2");
            final String orderBy = "SQL comment token is not allowed in ORDER BY expression: ";
            final String groupBy = "SQL comment token is not allowed in GROUP BY expression: ";

            assertEquals(orderBy + "a // c DESC, b ASC", assertThrows(IllegalArgumentException.class,
                    () -> h2.select("id").from("t").append(Filters.orderBy("a // c", SortDirection.DESC, "b", SortDirection.ASC))).getMessage());
            assertEquals(orderBy + "a // c DESC", assertThrows(IllegalArgumentException.class,
                    () -> h2.select("id").from("t").append(Criteria.builder().orderBy("a // c", SortDirection.DESC).limit(3).build())).getMessage());
            // The key in the middle and last position, with and without directions.
            assertEquals(orderBy + "x, a // c, b",
                    assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").append(Filters.orderBy("x", "a // c", "b"))).getMessage());
            assertEquals(orderBy + "x DESC, y DESC, a // c DESC",
                    assertThrows(IllegalArgumentException.class,
                            () -> h2.select("id").from("t").append(Criteria.builder().orderBy(List.of("x", "y", "a // c"), SortDirection.DESC).build()))
                                    .getMessage());
            final Map<String, SortDirection> orders = new LinkedHashMap<>();
            orders.put("x", SortDirection.ASC);
            orders.put("a // c", SortDirection.DESC);
            orders.put("b", SortDirection.ASC);
            assertEquals(orderBy + "x ASC, a // c DESC, b ASC",
                    assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").append(Criteria.builder().orderBy(orders).build()))
                            .getMessage());
            assertEquals(groupBy + "a // c, b", assertThrows(IllegalArgumentException.class,
                    () -> h2.select("id").from("t").append(Criteria.builder().groupBy("a // c", "b").having("count(*) > 1").build())).getMessage());
            assertEquals(groupBy + "x DESC, a // c DESC", assertThrows(IllegalArgumentException.class,
                    () -> h2.select("id").from("t").append(Filters.groupBy(List.of("x", "a // c"), SortDirection.DESC))).getMessage());
            assertEquals(groupBy + "a // c DESC",
                    assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").append(Filters.groupBy("a // c", SortDirection.DESC)))
                            .getMessage());
            // Like orderBy(String), every "//" comment is rejected: one ended inside the key, one in the last key, a raw clause.
            assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").orderBy("a // c\n", SortDirection.DESC));
            assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").append(Filters.orderBy("a // c\n", SortDirection.DESC)));
            assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").append(Filters.orderBy("x", "a // c")));
            assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").append(Filters.orderBy(Filters.expr("a DESC // newest"))));
            // A Criteria in a structured sub-query, and in a DELETE statement.
            assertThrows(IllegalArgumentException.class, () -> h2.select("id")
                    .from("t")
                    .where(Filters.in("id", Filters.subQuery("u", List.of("id"), Criteria.builder().where(Filters.eq("k", 1)).orderBy("a // c").build())))
                    .build());

            // The rejection is rolled back: no clause slot, select modifier or statement prefix is left behind.
            final SqlBuilder select = h2.select("id").from("t").where(Filters.eq("x", 1));
            assertThrows(IllegalArgumentException.class,
                    () -> select.append(Criteria.builder().distinctOn("a // c").groupBy("g").orderBy("a // c", SortDirection.DESC).build()));
            assertThrows(IllegalArgumentException.class, () -> select.append(Filters.orderBy("a // c", SortDirection.DESC, "b", SortDirection.ASC)));
            assertEquals("SELECT DISTINCT ON (a // c\n) id FROM t WHERE x = ? GROUP BY g ORDER BY a DESC",
                    select.append(Criteria.builder().distinctOn("a // c").groupBy("g").orderBy("a", SortDirection.DESC).build()).build().query());
            final SqlBuilder delete = h2.deleteFrom("t");
            assertThrows(IllegalArgumentException.class, () -> delete.append(Criteria.builder().where(Filters.eq("id", 1)).orderBy("a // c").build()));
            assertEquals("DELETE FROM t WHERE id = ?", delete.where(Filters.eq("id", 1)).build().query());

            // Not rejected on H2: "--" and block comments, which the tokenizer drops, and "//" in quoted text, a $$ string or
            // another comment.
            assertEquals("SELECT id FROM t ORDER BY a DESC, b ASC",
                    h2.select("id").from("t").append(Filters.orderBy("a -- c", SortDirection.DESC, "b", SortDirection.ASC)).build().query());
            assertEquals("SELECT id FROM t ORDER BY a DESC, b ASC",
                    h2.select("id").from("t").append(Filters.orderBy("a /* // */", SortDirection.DESC, "b", SortDirection.ASC)).build().query());
            assertEquals("SELECT id FROM t ORDER BY a DESC, b ASC",
                    h2.select("id").from("t").append(Filters.orderBy("a -- c // d", SortDirection.DESC, "b", SortDirection.ASC)).build().query());
            assertEquals("SELECT id FROM t GROUP BY 'a//b', b HAVING count(*) > 1",
                    h2.select("id").from("t").append(Criteria.builder().groupBy("'a//b'", "b").having("count(*) > 1").build()).build().query());
            assertTrue(h2.select("id")
                    .from("t")
                    .append(Filters.orderBy("$$//$$", SortDirection.DESC))
                    .build()
                    .query()
                    .startsWith("SELECT id FROM t ORDER BY $$//$$ DESC"));

            // Other dialects read "//" as an operator: byte-identical output.
            for (final Dsl other : List.of(PSC, lexDialect(PSC, "PostgreSQL"))) {
                assertEquals("SELECT id FROM t ORDER BY a // c DESC, b ASC",
                        other.select("id").from("t").append(Filters.orderBy("a // c", SortDirection.DESC, "b", SortDirection.ASC)).build().query());
                assertEquals("SELECT id FROM t ORDER BY a // c DESC LIMIT 3",
                        other.select("id").from("t").append(Criteria.builder().orderBy("a // c", SortDirection.DESC).limit(3).build()).build().query());
                assertEquals("SELECT id FROM t GROUP BY a // c, b HAVING count(*) > 1",
                        other.select("id").from("t").append(Criteria.builder().groupBy("a // c", "b").having("count(*) > 1").build()).build().query());
                assertEquals("SELECT id FROM t GROUP BY a // c DESC",
                        other.select("id").from("t").append(Filters.groupBy("a // c", SortDirection.DESC)).build().query());
                assertEquals("SELECT id FROM t ORDER BY a DESC, b ASC",
                        other.select("id").from("t").append(Filters.orderBy("a -- c", SortDirection.DESC, "b", SortDirection.ASC)).build().query());
            }
        }

        // Regression: Criteria.Builder.distinctOn composes "DISTINCT ON (" + list + ")" without knowing the dialect and ends only
        // "--"/"#" comments, so on H2 an appended Criteria rendered "SELECT DISTINCT ON (a // c)\n id FROM t" (the ')' inside the
        // comment) and "DISTINCT ON (// c)\n" for a comment-only list. The list is now rendered as distinctOn(String) renders it.
        @Test
        void testH2CriteriaDistinctOnWithDoubleSlashRendersLikeBuilderDistinctOn() {
            final Dsl h2 = lexDialect(PSC, "H2");

            assertEquals("SELECT DISTINCT ON (a // c\n) id FROM t",
                    h2.select("id").from("t").append(Criteria.builder().distinctOn("a // c").build()).build().query());
            assertEquals("SELECT DISTINCT id FROM t", h2.select("id").from("t").append(Criteria.builder().distinctOn("// c").build()).build().query());
            assertEquals("SELECT DISTINCT ON (a, b // c\n) id FROM t WHERE x = ?",
                    h2.select("id").from("t").append(Criteria.builder().distinctOn("a, b // c").where(Filters.eq("x", 1)).build()).build().query());
            assertEquals("SELECT DISTINCT id FROM t WHERE x = ?",
                    h2.select("id").from("t").append(Criteria.builder().distinctOn("// a\n// b").where(Filters.eq("x", 1)).build()).build().query());
            // A raw Criteria modifier of the same form is read the same way.
            assertEquals("SELECT DISTINCT ON (a // c\n) id FROM t",
                    h2.select("id").from("t").append(Criteria.builder().selectModifier("DISTINCT ON (a // c)").build()).build().query());
            // But a raw modifier whose list is closed before its "//" note ends in a ')' of the note: it is left to selectModifier,
            // which ends the note after it, exactly as the builder's own selectModifier renders the same text (rewriting it as a
            // DISTINCT ON list gave "... // one row per (dept\n) id": a stray ')' and an H2 syntax error).
            assertEquals("SELECT DISTINCT ON (dept) // one row per (dept)\n id FROM t",
                    h2.select("id").from("t").append(Criteria.builder().selectModifier("DISTINCT ON (dept) // one row per (dept)").build()).build().query());
            for (final String modifier : List.of("DISTINCT ON (dept) // (latest)", "DISTINCT ON (dept, a) // see f(x)", "DISTINCT ON (a) // c)",
                    "DISTINCT ON (f(a)) // g(b)", "DISTINCT ON (a // c\n) x // d)")) {
                assertEquals(h2.select("id").from("t").selectModifier(modifier).build().query(),
                        h2.select("id").from("t").append(Criteria.builder().selectModifier(modifier).build()).build().query(), modifier);
            }

            // Identical to the builder's own distinctOn, also for "--" and block comments, comments ended inside the list, and a
            // "//" in quoted text or an H2 $$ string.
            for (final String list : List.of("a // c", "// c", "// a\n// b", "a, b // c", "a // c\nb", "a\n// c", "a // c\n", "a // b -- c", "a -- c // d",
                    "a -- c", "-- c", "a /* // */", "'//' // c", "'a//b'", "$$//$$", "a", "f(a) // c", "f(a, (b)) // see (x)", "a // see b)", "a /* ) */ // c",
                    "')' // c", "$$)$$ // c")) {
                assertEquals(h2.select("id").distinctOn(list).from("t").build().query(),
                        h2.select("id").from("t").append(Criteria.builder().distinctOn(list).build()).build().query(), list);
            }

            // An unterminated block comment is still rejected by selectModifier (distinctOn rejects it too), leaving no modifier.
            final SqlBuilder builder = h2.select("id").from("t");
            assertEquals("selectModifier must not contain an unterminated block comment: DISTINCT ON (// c /* x)",
                    assertThrows(IllegalArgumentException.class, () -> builder.append(Criteria.builder().selectModifier("DISTINCT ON (// c /* x)").build()))
                            .getMessage());
            assertEquals("SELECT DISTINCT id FROM t", builder.append(Criteria.builder().distinctOn("// c").build()).build().query());

            // Other dialects read "//" as an operator: byte-identical output.
            for (final Dsl other : List.of(PSC, lexDialect(PSC, "PostgreSQL"))) {
                assertEquals("SELECT DISTINCT ON (a // c) id FROM t",
                        other.select("id").from("t").append(Criteria.builder().distinctOn("a // c").build()).build().query());
                assertEquals("SELECT DISTINCT ON (// c) id FROM t",
                        other.select("id").from("t").append(Criteria.builder().distinctOn("// c").build()).build().query());
                assertEquals("SELECT DISTINCT ON (a -- c\n) id FROM t",
                        other.select("id").from("t").append(Criteria.builder().distinctOn("a -- c").build()).build().query());
            }
        }

        // Regression: on H2 a fragment holding only "//" comments was not comment-only (the tokenizer renders "//" as two '/'
        // tokens), so where("// c") emitted the empty predicate "WHERE // c\n" while where("-- c") was rejected. It is now
        // rejected (or, for distinctOn, treated as blank) like its "--" form; "//" stays an operator on other dialects.
        @Test
        void testH2DoubleSlashCommentOnlyFragmentIsRejectedLikeDashComment() {
            final Dsl h2 = lexDialect(PSC, "H2");
            final Dsl h2raw = lexDialect(SCSB, "H2");
            final String message = "SQL expression must contain more than comments: ";

            for (final String commentOnly : List.of("// c", "// a\n// b", "/* x */ // c", "// c\n-- d", "  // c \r\n ")) {
                assertEquals(message + commentOnly, assertThrows(IllegalArgumentException.class, () -> h2.deleteFrom("t").where(commentOnly)).getMessage());
                assertEquals(message + commentOnly,
                        assertThrows(IllegalArgumentException.class, () -> h2.select("a").from("t").groupBy("a").having(commentOnly)).getMessage());
                assertEquals(message + commentOnly,
                        assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").join("u").on(commentOnly)).getMessage());
                assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").join("u").on(commentOnly, "u.x = 1"));
                assertThrows(IllegalArgumentException.class, () -> h2.deleteFrom("t").where(Filters.expr(commentOnly)));
                assertThrows(IllegalArgumentException.class, () -> h2.deleteFrom("t").where(Filters.and(Filters.expr(commentOnly), Filters.eq("a", 1))));
                assertThrows(IllegalArgumentException.class, () -> h2.deleteFrom("t").append(Criteria.builder().where(commentOnly).build()));
                assertThrows(IllegalArgumentException.class, () -> h2raw.update("t").set("a", SqlExpression.of(commentOnly)));
                assertEquals("SELECT DISTINCT a FROM t", h2.select("a").distinctOn(commentOnly).from("t").build().query());

                // A rejection is rolled back: the builder stays usable.
                final SqlBuilder builder = h2.deleteFrom("t");
                assertThrows(IllegalArgumentException.class, () -> builder.where(commentOnly));
                assertEquals("DELETE FROM t WHERE id = ?", builder.where(Filters.eq("id", 1)).build().query());
            }

            // The "--" forms, for comparison.
            assertEquals(message + "-- c", assertThrows(IllegalArgumentException.class, () -> h2.deleteFrom("t").where("-- c")).getMessage());
            assertEquals("SELECT DISTINCT a FROM t", h2.select("a").distinctOn("-- c").from("t").build().query());

            // Not comment-only: an operand before the comment, or "//" inside quoted text or an H2 $$ string.
            assertEquals("DELETE FROM t WHERE a // b\n", h2.deleteFrom("t").where("a // b").build().query());
            assertEquals("DELETE FROM t WHERE '//'", h2.deleteFrom("t").where("'//'").build().query());
            assertEquals("DELETE FROM t WHERE $$//$$\n", h2.deleteFrom("t").where("$$//$$").build().query());

            // Other dialects read "//" as an operator: unchanged.
            final Dsl pg = lexDialect(PSC, "PostgreSQL");
            assertEquals("DELETE FROM t WHERE a // b", pg.deleteFrom("t").where("a // b").build().query());
            assertEquals("DELETE FROM t WHERE // c", pg.deleteFrom("t").where("// c").build().query());
            assertEquals("SELECT DISTINCT ON (// c) a FROM t", pg.select("a").distinctOn("// c").from("t").build().query());
        }

        // Regression: "//" is a line comment only in H2, and the builder neither newline-terminated nor rejected it the way
        // it treats "--", so on H2 the clause appended after "t // c" was silently commented out
        // ("UPDATE t SET a = 1 // c WHERE x = 1" updated every row).
        @Test
        void testH2DoubleSlashLineCommentsAreTerminatedOrRejected() {
            final Dsl h2 = lexDialect(PSC, "H2");
            final Dsl pg = lexDialect(PSC, "PostgreSQL");

            assertEquals("SELECT a FROM t // c\n WHERE x = ?", h2.select("a").from("t // c").where(Filters.eq("x", 1)).build().query());
            assertEquals("SELECT a FROM t -- c\n WHERE x = ?", h2.select("a").from("t -- c").where(Filters.eq("x", 1)).build().query());
            assertEquals(h2.select("a").from(List.of("t -- c", "u")).where(Filters.eq("x", 1)).build().query().replace("--", "//"),
                    h2.select("a").from(List.of("t // c", "u")).where(Filters.eq("x", 1)).build().query());
            assertEquals("SELECT a FROM t JOIN u // c\n ON t.id = u.id", h2.select("a").from("t").join("u // c").on("t.id = u.id").build().query());
            assertEquals("SELECT a FROM t JOIN u -- c\n ON t.id = u.id", h2.select("a").from("t").join("u -- c").on("t.id = u.id").build().query());

            assertThrows(IllegalArgumentException.class, () -> h2.update("t").set("a = 1 // c"));
            assertThrows(IllegalArgumentException.class, () -> h2.select("a // c").from("t"));
            assertThrows(IllegalArgumentException.class, () -> h2.select("a").from("t").groupBy("a // c"));
            assertThrows(IllegalArgumentException.class, () -> h2.select("a").from("t").orderBy("a // c"));
            // The shared tokenizer strips a trailing "--" comment but renders "//" as ordinary tokens: terminated on H2.
            assertEquals("SELECT a FROM t WHERE x = 1  ORDER BY a", h2.select("a").from("t").where("x = 1 -- c").orderBy("a").build().query());
            assertEquals("SELECT a FROM t WHERE x = 1 // c\n ORDER BY a", h2.select("a").from("t").where("x = 1 // c").orderBy("a").build().query());
            assertEquals("SELECT a FROM t WHERE x = 1 // c ORDER BY a", pg.select("a").from("t").where("x = 1 // c").orderBy("a").build().query());
            assertEquals(h2.select("a").from("t").where("x = 1").append("-- c").orderBy("a").build().query().replace("--", "//"),
                    h2.select("a").from("t").where("x = 1").append("// c").orderBy("a").build().query());

            // A "/*" block comment is not a "//" line comment; "//" inside a string or quoted identifier is data.
            assertEquals("SELECT a FROM t /* c */ WHERE x = ?", h2.select("a").from("t /* c */").where(Filters.eq("x", 1)).build().query());
            assertEquals("UPDATE t SET a = '//' WHERE x = ?", h2.update("t").set("a = '//'").where(Filters.eq("x", 1)).build().query());
            assertEquals("SELECT a FROM \"t//u\" WHERE x = ?", h2.select("a").from("\"t//u\"").where(Filters.eq("x", 1)).build().query());

            // Negative control: PostgreSQL allows "//" as a user-defined operator, so nothing is terminated or rejected there.
            assertEquals("SELECT a FROM t // c WHERE x = ?", pg.select("a").from("t // c").where(Filters.eq("x", 1)).build().query());
            assertEquals("SELECT a FROM t // c WHERE x = ?", PSC.select("a").from("t // c").where(Filters.eq("x", 1)).build().query());
            assertEquals("UPDATE t SET a = b // 2 WHERE x = ?", pg.update("t").set("a = b // 2").where(Filters.eq("x", 1)).build().query());
            assertEquals("SELECT a FROM t JOIN u // c ON t.id = u.id", pg.select("a").from("t").join("u // c").on("t.id = u.id").build().query());

            // A "//" comment in the MIDDLE of a raw expression: the tokenizer turned the line break ending it into a space,
            // so on H2 the comment swallowed the rest ("tenant_id = 7 // scope AND id = 42" deleted every row of tenant 7).
            // From that comment on, the text is now emitted as written, keeping the line break (LF, CRLF, lone CR).
            final Dsl h2raw = lexDialect(SCSB, "H2");
            assertEquals("DELETE FROM t WHERE tenant_id = 7 // scope\nAND id = 42",
                    h2.deleteFrom("t").where("tenant_id = 7 // scope\nAND id = 42").build().query());
            assertEquals("DELETE FROM t WHERE tenant_id = 7 // scope\r\nAND id = 42",
                    h2.deleteFrom("t").where("tenant_id = 7 // scope\r\nAND id = 42").build().query());
            // H2 ends the comment at the lone CR; the extra line feed covers a reading that ends it only at '\n'.
            assertEquals("DELETE FROM t WHERE tenant_id = 7 // scope\rAND id = 42\n",
                    h2.deleteFrom("t").where("tenant_id = 7 // scope\rAND id = 42").build().query());
            assertEquals("UPDATE t SET a = 0 // reset\n+ a WHERE id = 1",
                    h2raw.update("t").set("a", SqlExpression.of("0 // reset\n+ a")).where(Filters.eq("id", 1)).build().query());
            assertEquals("INSERT INTO t (a) VALUES (1 // c\n+ 2)", h2raw.insert(Map.of("a", SqlExpression.of("1 // c\n+ 2"))).into("t").build().query());
            assertEquals("SELECT a FROM t GROUP BY a HAVING count(*) > 1 // c\nAND max(b) < 3",
                    h2.select("a").from("t").groupBy("a").having("count(*) > 1 // c\nAND max(b) < 3").build().query());
            assertEquals("SELECT a FROM t JOIN u ON t.id = u.id // c\nAND u.k = 1",
                    h2.select("a").from("t").join("u").on("t.id = u.id // c\nAND u.k = 1").build().query());
            assertEquals("SELECT a FROM t JOIN u ON (t.id = u.id // c\nAND u.k = 1) AND (u.j = 2) WHERE b = ?",
                    h2.select("a").from("t").join("u").on("t.id = u.id // c\nAND u.k = 1", "u.j = 2").where(Filters.eq("b", 1)).build().query());
            assertEquals("SELECT a FROM t WHERE x = 1 // c\nAND y = 2 ORDER BY a",
                    h2.select("a").from("t").where(Filters.expr("x = 1 // c\nAND y = 2")).orderBy("a").build().query());
            assertEquals("SELECT a FROM t WHERE (x = 1 // c\nOR y = 2) AND (z = ?)",
                    h2.select("a").from("t").where(Filters.and(Filters.expr("x = 1 // c\nOR y = 2"), Filters.eq("z", 3))).build().query());
            assertEquals("SELECT a FROM t WHERE x = 1 // c\nAND y = 2 ORDER BY a",
                    h2.select("a").from("t").append(Criteria.builder().where(Filters.expr("x = 1 // c\nAND y = 2")).orderBy("a").build()).build().query());
            assertEquals("SELECT a FROM t WHERE x = 1 // c\nAND y = 2 ORDER BY a",
                    h2.select("a").from("t").append(Filters.where("x = 1 // c\nAND y = 2")).orderBy("a").build().query());
            // "//*" opens a line comment on H2, not a block comment the tokenizer would drop ("x = 1 / AND y = 2").
            assertEquals("SELECT a FROM t WHERE x = 1 //* c */\nAND y = 2 ORDER BY a",
                    h2.select("a").from("t").where("x = 1 //* c */\nAND y = 2").orderBy("a").build().query());
            // The text before the comment is still converted; the text from the comment on is emitted as written.
            assertEquals("SELECT acc.first_name AS \"firstName\" FROM account acc WHERE acc.first_name = 1 // c\nAND lastName = 2",
                    h2.select("firstName").from(Account.class).where("firstName = 1 // c\nAND lastName = 2").build().query());
            // A comment followed only by whitespace is terminated by the trailing rule (the tokenizer collapsed its line break).
            assertEquals("SELECT a FROM t WHERE x = 1 // c \n ORDER BY a", h2.select("a").from("t").where("x = 1 // c\n  ").orderBy("a").build().query());
            // A "//" inside an H2 $$...$$ string is data, so the expression keeps the tokenizer path and later names are still
            // converted (the trailing-comment rule, which also reads the text without dollar quotes, only adds a harmless line feed).
            assertEquals("SELECT a FROM t WHERE x = $$a//b$$\n ORDER BY a", h2.select("a").from("t").where("x = $$a//b$$").orderBy("a").build().query());
            assertEquals("SELECT acc.first_name AS \"firstName\" FROM account acc WHERE note = $$see http://x.org$$ AND acc.last_name = 'y'\n",
                    h2.select("firstName").from(Account.class).where("note = $$see http://x.org$$\nAND lastName = 'y'").build().query());
            assertEquals("SELECT acc.first_name AS \"firstName\" FROM account acc WHERE x = $$a//b$$ AND acc.last_name = 2\n",
                    h2.select("firstName").from(Account.class).where("x = $$a//b$$\nAND lastName = 2").build().query());
            // A "//" comment after a closed $$ string is still a comment; from an unterminated $$ (an H2 syntax error) the
            // text is emitted as written, as is every dollar-quoted string a server without dollar quotes reads differently;
            // a "$$" glued to a name ("a$$b") opens no string, so the "//" after it is a comment.
            assertEquals("SELECT acc.first_name AS \"firstName\" FROM account acc WHERE x = $$a$$ // c\nAND lastName = 2",
                    h2.select("firstName").from(Account.class).where("x = $$a$$ // c\nAND lastName = 2").build().query());
            assertEquals("SELECT acc.first_name AS \"firstName\" FROM account acc WHERE x = $$a//b\nAND lastName = 2",
                    h2.select("firstName").from(Account.class).where("x = $$a//b\nAND lastName = 2").build().query());
            assertEquals("SELECT acc.first_name AS \"firstName\" FROM account acc WHERE a$$b // c\nAND lastName = 2",
                    h2.select("firstName").from(Account.class).where("a$$b // c\nAND lastName = 2").build().query());
            assertEquals("SELECT acc.first_name AS \"firstName\" FROM account acc WHERE x = a$$b$$ // c\nAND lastName = 2",
                    h2.select("firstName").from(Account.class).where("x = a$$b$$ // c\nAND lastName = 2").build().query());
            // H2 strings have no backslash escapes, so 'C:\' is complete and the "//" after it is a comment.
            final String backslash = String.valueOf((char) 92);
            assertEquals("SELECT a FROM t WHERE x = 'C:" + backslash + "' // c\n ORDER BY a",
                    h2.select("a").from("t").where("x = 'C:" + backslash + "' // c").orderBy("a").build().query());
            assertEquals("SELECT a FROM t WHERE x = 'C:" + backslash + "' // c\nAND y = 1 ORDER BY a",
                    h2.select("a").from("t").where("x = 'C:" + backslash + "' // c\nAND y = 1").orderBy("a").build().query());

            // The trailing-comment rule tests the EMITTED text: a "--" comment the tokenizer dropped needs no line feed,
            // even when a quoted "//" is present.
            assertEquals("SELECT a FROM t WHERE x = 'a//b'  ORDER BY a", h2.select("a").from("t").where("x = 'a//b' -- c").orderBy("a").build().query());

            // A trailing "//" comment is terminated before a closing parenthesis, a set-operation clause, the select list and ON.
            assertEquals("SELECT a FROM t WHERE x IN (SELECT x FROM u WHERE y // c\n)",
                    h2.select("a").from("t").where(Filters.in("x", Filters.subQuery("SELECT x FROM u WHERE y // c"))).build().query());
            assertEquals("SELECT a FROM (SELECT id FROM u WHERE x = 1 // c\n) s WHERE b = ?",
                    h2.select("a").from(h2.select("id").from("u").where("x = 1 // c"), "s").where(Filters.eq("b", 1)).build().query());
            assertEquals("SELECT a FROM t UNION SELECT a FROM u // c\n ORDER BY a",
                    h2.select("a").from("t").union("SELECT a FROM u // c").orderBy("a").build().query());
            assertEquals("SELECT a FROM t UNION SELECT a FROM u // c\n ORDER BY a",
                    h2.select("a").from("t").append(Criteria.builder().union(Filters.subQuery("SELECT a FROM u // c")).build()).orderBy("a").build().query());
            assertEquals("SELECT DISTINCT // c\n a FROM t", h2.select("a").selectModifier("DISTINCT // c").from("t").build().query());
            assertEquals("SELECT DISTINCT ON (a // c\n) a FROM t", h2.select("a").distinctOn("a // c").from("t").build().query());
            assertEquals("SELECT a FROM t JOIN u // c\n ON t.id = u.id",
                    h2.select("a").from("t").append(Criteria.builder().join("u // c", Filters.expr("t.id = u.id")).build()).build().query());

            // Line ends: from(String) trims a trailing CR/CRLF; an inner lone CR ends the comment on H2 but not on every server.
            assertEquals("SELECT a FROM t // c\n WHERE x = ?", h2.select("a").from("t // c\r").where(Filters.eq("x", 1)).build().query());
            assertEquals("SELECT a FROM t // c\n WHERE x = ?", h2.select("a").from("t // c\r\n").where(Filters.eq("x", 1)).build().query());
            assertEquals("SELECT a FROM t // c\r x\n WHERE x = ?", h2.select("a").from("t // c\r x").where(Filters.eq("x", 1)).build().query());
            // "//*" is a line comment, "/*/" a block comment.
            assertEquals("SELECT a FROM t //* c */\n WHERE x = ?", h2.select("a").from("t //* c */").where(Filters.eq("x", 1)).build().query());
            assertEquals("SELECT a FROM t /*/ c */ WHERE x = ?", h2.select("a").from("t /*/ c */").where(Filters.eq("x", 1)).build().query());

            // Aliases and column names are rejected like their "--" forms (an H2 alias is emitted unquoted).
            assertEquals("Column alias for 'count(*)' must not be null, blank, quoted, or contain SQL comment tokens: cnt // c",
                    assertThrows(IllegalArgumentException.class, () -> h2.select("count(*) AS cnt // c").from("t")).getMessage());
            assertEquals("Table alias for 'Account' must not contain a line break or SQL comment token: a//x",
                    assertThrows(IllegalArgumentException.class, () -> h2raw.selectFrom(List.of(Selection.builder(Account.class).tableAlias("a//x").build())))
                            .getMessage());
            // The same alias staged through the multi-entity select(...) overload is rejected when from(...) renders it, and the
            // rejection leaves the builder reusable.
            final SqlBuilder multiSelect = h2raw.select(Account.class, "a//x", "acc", Account.class, "b", "acc2");
            assertEquals("Table alias for 'Account' must not contain a line break or SQL comment token: a//x",
                    assertThrows(IllegalArgumentException.class, () -> multiSelect.from("account a, account b")).getMessage());
            assertThrows(IllegalArgumentException.class, () -> multiSelect.from(Account.class));
            assertEquals("", multiSelect._sb.toString());
            assertThrows(IllegalArgumentException.class, () -> h2.select("firstName").from(Account.class, "a//x"));
            assertThrows(IllegalArgumentException.class, () -> h2.select("a").from("t").join(Account.class, "a//x"));
            assertThrows(IllegalArgumentException.class, () -> h2.update("t").set("a // c", 1));
            assertThrows(IllegalArgumentException.class, () -> h2.select("a").from("t").where(Filters.eq("a // c", 1)).build());
            assertThrows(IllegalArgumentException.class, () -> h2.insert("a // c").into("t"));
            assertThrows(IllegalArgumentException.class, () -> h2.select("a").from("t").join("u").using("a // c"));
            assertThrows(IllegalArgumentException.class, () -> h2.select("a").from("t").orderBy("a // c", SortDirection.DESC));
            assertEquals("SELECT count(*) AS cnt // c FROM t", pg.select("count(*) AS cnt // c").from("t").build().query());
            assertTrue(lexDialect(SCSB, "PostgreSQL").selectFrom(List.of(Selection.builder(Account.class).tableAlias("a//x").build()))
                    .build()
                    .query()
                    .contains(" FROM account a//x"));

            // Every other dialect keeps reading "//" as an operator: byte-identical output, nothing terminated or rejected.
            for (final Dsl other : List.of(PSC, pg, lexDialect(PSC, "MySQL"), lexDialect(PSC, "Microsoft SQL Server"), lexDialect(PSC, "Oracle"),
                    lexDialect(PSC, "SQLite"), lexDialect(PSC, "DB2/LINUXX8664"))) {
                assertEquals("SELECT a FROM t // c WHERE x = ?", other.select("a").from("t // c").where(Filters.eq("x", 1)).build().query());
                assertEquals("DELETE FROM t WHERE x = 1 // c AND y = 2", other.deleteFrom("t").where("x = 1 // c\nAND y = 2").build().query());
                assertEquals("SELECT a FROM t WHERE x = 1 // c ORDER BY a", other.select("a").from("t").where("x = 1 // c").orderBy("a").build().query());
                assertEquals("SELECT count(*) AS cnt // c FROM t", other.select("count(*) AS cnt // c").from("t").build().query());
            }
        }

        @Test
        void testHashPrefixedExpressionIsRejectedOutsideSqlServer() {
            // Outside SQL Server, '#' opens a MySQL hash comment, so the expression would render no predicate.
            final SqlBuilder builder = PSC.select("*").from("people");
            assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.expr("#tmp.id = 1")));
            assertThrows(IllegalArgumentException.class, () -> builder.where("#tmp.id = 1"));
            assertEquals("SELECT * FROM people WHERE id = ?", builder.where(Filters.eq("id", 1)).build().query());

            final Dsl sqlServer = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());
            assertEquals("SELECT * FROM #tmp WHERE #tmp.id = 1", sqlServer.select("*").from("#tmp").where(Filters.expr("#tmp.id = 1")).build().query());
        }

        @Test
        void testIsDefaultIdPropValueFractionalNumberNotTreatedAsZero() {
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(null));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0L));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(java.math.BigDecimal.ZERO));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(java.math.BigInteger.ZERO));
            assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(java.math.BigInteger.ONE));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0.0));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0.0f));
            assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(new java.math.BigDecimal("0.9")),
                    "BigDecimal 0.9 has longValue()=0 but must not be treated as default ID");
            assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(new java.math.BigDecimal("0.1")));
            assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(0.5), "double 0.5 must not be treated as default ID");
            assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(0.1f), "float 0.1 must not be treated as default ID");
        }

        @Test
        void testLazyDmlInitializationPrecedesClauseReservation() {
            final SqlBuilder incompleteUpdate = PSC.update("users");
            assertThrows(IllegalStateException.class, () -> incompleteUpdate.where("id = 1"));
            assertEquals("UPDATE users SET name = ? WHERE id = 1", incompleteUpdate.set("name").where("id = 1").build().query());

            assertEquals("DELETE FROM users ORDER BY id LIMIT 1", PSC.deleteFrom("users").orderBy("id").limit(1).build().query());

            final SqlBuilder insert = PSC.insert("name").into("users");
            assertThrows(IllegalStateException.class, () -> insert.where("id = 1"));
            assertEquals("INSERT INTO users (name) VALUES (?)", insert.build().query());
        }

        @Test
        void testLazyValidationLabelsPreserveIndexedDiagnostics() {
            final String suffix = " must not be null, empty, or blank";
            assertEquals("columns[1]" + suffix,
                    assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.checkSqlFragmentsNotBlank(new String[] { "id", "\t " }, "columns"))
                            .getMessage());
            assertEquals("columns[2]" + suffix, assertThrows(IllegalArgumentException.class,
                    () -> AbstractQueryBuilder.checkSqlFragmentsNotBlank(Arrays.asList("id", "name", null), "columns")).getMessage());
            assertEquals("Key in props" + suffix, assertThrows(IllegalArgumentException.class,
                    () -> AbstractQueryBuilder.checkSqlFragmentKeysNotBlank(Collections.singletonMap(" ", 1), "props")).getMessage());
        }

        // Regression: MySQL "..." string literals honor backslash escapes, but the alias, FROM-separator and JOIN-connector
        // scanners (and the comment guard) read them with quote doubling only, so "\"" looked like an open quote.
        @Test
        void testLexicalScannersHonorMySqlDoubleQuotedStringEscapes() {
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

        @Test
        void testMySqlArithmeticDashPairDoesNotHideStatementTerminators() {
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

        // Covers the MySQL RAW_SQL backslash doubling at the call sites beyond WHERE '=': UPDATE SET, LIKE, BETWEEN,
        // MariaDB, a nested structured sub-query (rendered by a same-dialect sub-builder), and a nested raw SubQuery binding.
        @Test
        @SuppressWarnings("deprecation")
        void testMySqlRawSqlBackslashDoublingAtEveryValueCallSite() {
            final Dsl mysqlRaw = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());
            final Dsl mariaRaw = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MariaDB")).build());

            assertEquals("UPDATE t SET name = 'x\\\\' WHERE id = 1",
                    mysqlRaw.update("t").set(Map.of("name", "x\\")).where(Filters.eq("id", 1)).build().query());
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
                            Filters.subQuery("SELECT id FROM u WHERE a IN ?",
                                    Arrays.asList(Filters.subQuery("SELECT b FROM v WHERE c = ?", Arrays.asList("z\\"))))))
                    .build()
                    .query();
            assertTrue(nestedRaw.contains("c = 'z\\\\'"), nestedRaw);
        }

        // Regression: RAW_SQL under the MySQL dialect only doubled quotes, so a value ending in '\' escaped the closing quote (injection) and "a\b" was stored as a backspace.
        @Test
        @SuppressWarnings("deprecation")
        void testMySqlRawSqlLiteralsEscapeBackslashes() {
            final Dsl mysqlRaw = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());

            assertEquals("DELETE FROM account WHERE (name = 'x\\\\') AND (gui = ' OR 1=1 -- ')",
                    mysqlRaw.deleteFrom("account").where(Filters.and(Filters.eq("name", "x\\"), Filters.eq("gui", " OR 1=1 -- "))).build().query());

            final Map<String, Object> props = new LinkedHashMap<>();
            props.put("path", "a\\b");
            props.put("note", "it's");
            assertEquals("INSERT INTO account (path, note) VALUES ('a\\\\b', 'it''s')", mysqlRaw.insert(props).into("account").build().query());

            assertEquals("SELECT id FROM t WHERE c = '\\\\'", mysqlRaw.select("id").from("t").where(Filters.eq("c", '\\')).build().query());
            assertEquals("SELECT id FROM t WHERE name IN ('a\\\\', 'b')",
                    mysqlRaw.select("id").from("t").where(Filters.in("name", List.of("a\\", "b"))).build().query());

            // Raw sub-query bindings inlined under RAW_SQL get the same escaping.
            assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE a = 'x\\\\' AND b = ' OR 1=1 -- ')",
                    mysqlRaw.select("id")
                            .from("t")
                            .where(Filters.in("id", Filters.subQuery("SELECT id FROM u WHERE a = ? AND b = ?", Arrays.asList("x\\", " OR 1=1 -- "))))
                            .build()
                            .query());

            // A SqlExpression is SQL, not a value: emitted verbatim.
            assertEquals("SELECT id FROM t WHERE name = 'x\\'",
                    mysqlRaw.select("id").from("t").where(Filters.eq("name", SqlExpression.of("'x\\'"))).build().query());

            // Other dialects keep the SQL-standard rendering (a backslash is an ordinary character).
            assertEquals("DELETE FROM account WHERE name = 'x\\'", SCSB.deleteFrom("account").where(Filters.eq("name", "x\\")).build().query());
            final Dsl pgRaw = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
            assertEquals("DELETE FROM account WHERE name = 'x\\'", pgRaw.deleteFrom("account").where(Filters.eq("name", "x\\")).build().query());
        }

        // Regression: with a shared "#name" handler, a child token that keeps its name ("#id") was read as a hash comment by the
        // rename scan, hiding the colliding token after it on the same line, so two "#x" placeholders were bound to different values.
        @Test
        void testNonRenamedCustomHashTokenDoesNotHideLaterRenamedToken() {
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

        @Test
        void testNullMapDirectionMessageNamesKey() {
            final Map<String, SortDirection> map = new LinkedHashMap<>();
            map.put("x", null);

            assertEquals("Direction for key 'x' in groupings must not be null",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("t").groupBy(map)).getMessage());
            assertEquals("Direction for key 'x' in orders must not be null",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("t").orderBy(map)).getMessage());
        }

        @Test
        void testNullTokenizerConfigUsesImmutableDefault() {
            final SqlBuilder builder = Dsl.forDialect(NSC.sqlDialect().toBuilder().tokenizerConfig(null).build()).select("id").from("users");

            assertSame(SqlParser.defaultTokenizerConfig(), builder._tokenizer.tokenizerConfig());
        }

        // The positional set(...) overloads back onto a map, which would silently collapse a repeated name.
        @Test
        void testPositionalSetOverloadsRejectDuplicateNames() {
            assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set("a", 1, "a", 2));
            assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set("a", 1, "b", 2, "a", 3));
            assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set("a", 1, "b", 2, "b", 3));
            assertEquals("UPDATE t SET a = ?, b = ?, c = ?", PSC.update("t").set("a", 1, "b", 2, "c", 3).build().query());
        }

        // The positional set(name, value, name, value[, name, value]) duplicate check skips blank names so a blank
        // name is reported by the map overload's blank-name error, not as a "Duplicate" of another blank name.
        @Test
        void testPositionalSetReportsBlankNamesAsBlankNotDuplicate() {
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
        void testPostgreSqlRemovePathOperatorIsNotAHashComment() {
            final String sql = Dsl.PSB.select("payload #- '{address,city}'").from("events").build().query();

            assertEquals("SELECT payload #- '{address,city}' FROM events", sql);
        }

        // On PostgreSQL '[' opens an array subscript, not a bracket-quoted identifier: a "]" inside a subscript
        // string must not hide a trailing line comment or expose a quoted semicolon.
        @Test
        void testPostgreSqlSubscriptIsNotBracketQuotedIdentifier() {
            final Dsl pg = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());

            assertEquals("SELECT * FROM t, LATERAL (SELECT t.j['a]'] AS v) x -- c\n WHERE id = ?",
                    pg.select("*").from("t, LATERAL (SELECT t.j['a]'] AS v) x -- c").where(Filters.eq("id", 1)).build().query());

            assertEquals("SELECT id FROM t UNION SELECT id FROM u WHERE j['a]'] = '1' -- c\n ORDER BY id",
                    pg.select("id").from("t").union("SELECT id FROM u WHERE j['a]'] = '1' -- c").orderBy("id").build().query());

            assertEquals("SELECT id FROM t UNION SELECT id FROM u WHERE j['a]'] = ';'",
                    pg.select("id").from("t").union("SELECT id FROM u WHERE j['a]'] = ';'").build().query());
        }

        @Test
        void testPrintln() {
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

        @Test
        void testPSCComplexQuery() {
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
        void testQuestionHashOperatorIsNotAHashComment() {
            assertEquals("SELECT payload ?# path FROM events", Dsl.PSB.select("payload ?# path").from("events").build().query());
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select("payload ?#/* comment */ path").from("events"));
        }

        // Regression: the builder converted a glued subscript / array constructor / cast token as one identifier
        // (ARRAY[1, 2, 3] -> array[1,_2,_3], myTags[:tagIndex] -> my_tags[:tag_index]), skipped the column in
        // unitPrice::numeric(10,2), rewrote ::"OrderStatus", and left the column in "T".firstName unconverted.
        @Test
        void testRawExpressionGluedTokensRenderIdenticallyThroughBothPaths() {
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

            assertEquals("SELECT id FROM t WHERE id = ANY(ARRAY[1, 2, 3])",
                    PSC.select("id").from("t").where(Filters.expr("id = ANY(ARRAY[1, 2, 3])")).build().query());
            assertEquals("SELECT id FROM t WHERE my_tags[:tagIndex] = 'x'",
                    PSC.select("id").from("t").where(Filters.expr("myTags[:tagIndex] = 'x'")).build().query());
            assertEquals("SELECT id FROM t WHERE tags && ARRAY[:tagA, :tagB]",
                    NSC.select("id").from("t").where(Filters.expr("tags && ARRAY[:tagA, :tagB]")).build().query());
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

        // SqlExpression.toSql and AbstractQueryBuilder.appendStringExpr are two independent rendering paths for
        // the same raw expression text: a condition reaches the SQL through the builder, but toString()/toSql()
        // renders it directly. Guards added to one must be added to the other. Before this was pinned, the
        // builder left "_firstName" unconverted, converted "@firstName" (a SQL variable, not a column), and
        // snake-cased the contents of the string literal in "N'camelCase'".
        @Test
        void testRawExpressionRendersIdenticallyThroughBothPaths() {
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

        @Test
        void testRemovedSetOverloadsAreAbsent() throws NoSuchMethodException {
            assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("set", String[].class));
            assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setValue", String.class, Object.class));
            assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setEntity", Object.class));
            assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setEntity", Object.class, Set.class));
            assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setEntity", Class.class));
            assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod("setEntity", Class.class, Set.class));

            assertNotNull(AbstractQueryBuilder.class.getDeclaredMethod("set", String.class, Object.class));
            assertNotNull(AbstractQueryBuilder.class.getDeclaredMethod("set", Object.class, Set.class));
            assertNotNull(AbstractQueryBuilder.class.getDeclaredMethod("set", String.class, Object.class, String.class, Object.class));
            assertNotNull(
                    AbstractQueryBuilder.class.getDeclaredMethod("set", String.class, Object.class, String.class, Object.class, String.class, Object.class));
        }

        // Only the exact default token ":" + name is left out of _renderedNamedParameterTokens. Every other form is
        // recorded, including a same-length one that differs after the colon, which a set-operation rename relies on.
        @Test
        void testRenderedNamedParameterTokensRecordEveryNonDefaultForm() {
            final java.util.function.Function<BiConsumer<StringBuilder, String>, Dsl> withHandler = handler -> Dsl
                    .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler(handler).build());
            final Map<String, BiConsumer<StringBuilder, String>> handlers = new LinkedHashMap<>(); // the token rendered for "id"
            handlers.put(":id", (sb, name) -> sb.append(':').append(name));
            handlers.put(":id ", (sb, name) -> sb.append(':').append(name).append(' '));
            handlers.put("::id", (sb, name) -> sb.append("::").append(name));
            handlers.put(":i", (sb, name) -> sb.append(':').append(name, 0, name.length() - 1));
            handlers.put(":iX", (sb, name) -> sb.append(':').append(name, 0, name.length() - 1).append('X'));
            handlers.put(":ID", (sb, name) -> sb.append(':').append(name.toUpperCase()));
            handlers.put("${id}", (sb, name) -> sb.append("${").append(name).append('}'));

            handlers.forEach((token, handler) -> {
                final SqlBuilder builder = withHandler.apply(handler).select("id").from("a").where(Filters.eq("id", 1));
                assertEquals(token.equals(":id") ? Map.of() : Map.of("id", token), builder._renderedNamedParameterTokens, token);
                assertEquals("SELECT id FROM a WHERE id = " + token, builder.build().query());
            });

            // Were ":ID" taken for the default form, the child's renamed placeholder would stay ":ID" next to the parent's.
            final Dsl upper = withHandler.apply(handlers.get(":ID"));
            final SqlBuilder compound = upper.select("id").from("a").where(Filters.eq("id", 1)).union(upper.select("id").from("b").where(Filters.eq("id", 2)));
            assertEquals(Map.of("id", ":ID", "id_2", ":ID_2"), compound._renderedNamedParameterTokens);
            final SP sp = compound.build();
            assertEquals("SELECT id FROM a WHERE id = :ID UNION SELECT id FROM b WHERE id = :ID_2", sp.query());
            assertEquals(List.of(1, 2), sp.parameters());
        }

        // Only tokens that differ from the default ":name" are recorded, so a batch INSERT no longer retains one
        // redundant String and map node per placeholder; the set-operation rewrite reads absent entries as ":name".
        @Test
        void testRenderedNamedParameterTokensStoreOnlyCustomForms() {
            final List<Map<String, Object>> rows = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                final Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", i);
                row.put("name", "n" + i);
                rows.add(row);
            }

            final SqlBuilder named = NSC.batchInsert(rows).into("t");
            assertEquals("INSERT INTO t (id, name) VALUES (:id_0, :name_0), (:id_1, :name_1), (:id_2, :name_2)", named.build().query());
            assertEquals(Set.of("id_0", "name_0", "id_1", "name_1", "id_2", "name_2"), named._generatedNamedParameterNames);
            assertTrue(named._renderedNamedParameterTokens.isEmpty());

            final Dsl custom = Dsl
                    .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
            final SqlBuilder customNamed = custom.batchInsert(rows).into("t");
            assertEquals("INSERT INTO t (id, name) VALUES (${id_0}, ${name_0}), (${id_1}, ${name_1}), (${id_2}, ${name_2})", customNamed.build().query());
            assertEquals(Map.of("id_0", "${id_0}", "name_0", "${name_0}", "id_1", "${id_1}", "name_1", "${name_1}", "id_2", "${id_2}", "name_2", "${name_2}"),
                    customNamed._renderedNamedParameterTokens);

            // A handler that renders the default form for some names only records the others.
            final Dsl mixed = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
                if (name.startsWith("id")) {
                    sb.append(':').append(name);
                } else {
                    sb.append("${").append(name).append('}');
                }
            }).build());
            final SqlBuilder mixedNamed = mixed.batchInsert(rows).into("t");
            assertEquals("INSERT INTO t (id, name) VALUES (:id_0, ${name_0}), (:id_1, ${name_1}), (:id_2, ${name_2})", mixedNamed.build().query());
            assertEquals(Map.of("name_0", "${name_0}", "name_1", "${name_1}", "name_2", "${name_2}"), mixedNamed._renderedNamedParameterTokens);

            final SP compound = NSC.select("id")
                    .from("users")
                    .where(Filters.eq("id", 1))
                    .union(NSC.select("id").from("archive").where(Filters.eq("id", 3)))
                    .build();
            assertEquals("SELECT id FROM users WHERE id = :id UNION SELECT id FROM archive WHERE id = :id_2", compound.query());
            assertEquals(List.of(1, 3), compound.parameters());
        }

        // Same suffix high-water mark across repeated nesting, each level promoting another suffix.
        @Test
        @SuppressWarnings("deprecation")
        void testRepeatedCompositionRetainsEachNewlyPromotedSuffix() {
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

        // The set-operation rename and the raw-sub-query rewrite follow the same storage rule as appendNamedParameter: a
        // default ":name" token is never stored, and a custom token is stored under the final name. A renamed custom token
        // that is not recorded makes a later composition rename the wrong text and bind two values to one placeholder.
        @Test
        void testSetOperationAndRawSubQueryRenamesStoreOnlyNonDefaultTokens() {
            final SqlBuilder defaultUnion = NSC.select("id").from("a").where(Filters.eq("id", 1)).union(NSC.select("id").from("b").where(Filters.eq("id", 2)));
            assertTrue(defaultUnion._renderedNamedParameterTokens.isEmpty());
            assertEquals(Set.of("id", "id_2"), defaultUnion._generatedNamedParameterNames);
            assertEquals("SELECT id FROM a WHERE id = :id UNION SELECT id FROM b WHERE id = :id_2", defaultUnion.build().query());

            final SqlBuilder defaultRaw = NSC.select("id").from("a").where(Filters.in("id", Filters.subQuery("SELECT x FROM y WHERE p = ?", List.of(9))));
            assertTrue(defaultRaw._renderedNamedParameterTokens.isEmpty());
            final SP defaultRawSp = defaultRaw.build();
            assertEquals("SELECT id FROM a WHERE id IN (SELECT x FROM y WHERE p = :param)", defaultRawSp.query());
            assertEquals(List.of(9), defaultRawSp.parameters());

            final Dsl custom = Dsl
                    .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
            final SqlBuilder customRaw = custom.select("id").from("a").where(Filters.in("id", Filters.subQuery("SELECT x FROM y WHERE p = ?", List.of(9))));
            assertEquals(Map.of("param", "${param}"), customRaw._renderedNamedParameterTokens);
            assertEquals("SELECT id FROM a WHERE id IN (SELECT x FROM y WHERE p = ${param})", customRaw.build().query());

            final SqlBuilder parent = custom.select("id").from("a").where(Filters.eq("id", 1)).union(custom.select("id").from("b").where(Filters.eq("id", 2)));
            assertEquals(Map.of("id", "${id}", "id_2", "${id_2}"), parent._renderedNamedParameterTokens);
            final SqlBuilder grand = custom.select("id")
                    .from("g")
                    .where(Filters.and(Filters.eq("id", 0), Filters.eq("id", 0)))
                    .union(custom.select("id").from(parent, "p"));
            assertEquals(Map.of("id", "${id}", "id_2", "${id_2}", "id_3", "${id_3}", "id_4", "${id_4}"), grand._renderedNamedParameterTokens);
            final SP grandSp = grand.build();
            assertEquals("SELECT id FROM g WHERE (id = ${id}) AND (id = ${id_2}) UNION SELECT id FROM "
                    + "(SELECT id FROM a WHERE id = ${id_3} UNION SELECT id FROM b WHERE id = ${id_4}) p", grandSp.query());
            assertEquals(List.of(0, 0, 1, 2), grandSp.parameters());

            // A handler whose form depends on the name: a rename turns a default token into a custom one, or the reverse.
            final Dsl suffixedCustom = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
                if (name.matches(".*_[0-9]+")) {
                    sb.append("${").append(name).append('}');
                } else {
                    sb.append(':').append(name);
                }
            }).build());
            final SqlBuilder suffixedParent = suffixedCustom.select("id")
                    .from("a")
                    .where(Filters.eq("id", 1))
                    .union(suffixedCustom.select("id").from("b").where(Filters.eq("id", 2)));
            assertEquals(Map.of("id_2", "${id_2}"), suffixedParent._renderedNamedParameterTokens);
            final SqlBuilder suffixedGrand = suffixedCustom.select("id")
                    .from("g")
                    .where(Filters.eq("id", 0))
                    .union(suffixedCustom.select("id").from(suffixedParent, "p"));
            assertEquals(Map.of("id_2", "${id_2}", "id_3", "${id_3}"), suffixedGrand._renderedNamedParameterTokens);
            final SP suffixedSp = suffixedGrand.build();
            assertEquals(
                    "SELECT id FROM g WHERE id = :id UNION SELECT id FROM (SELECT id FROM a WHERE id = ${id_2} UNION SELECT id FROM b WHERE id = ${id_3}) p",
                    suffixedSp.query());
            assertEquals(List.of(0, 1, 2), suffixedSp.parameters());

            final Dsl unsuffixedCustom = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
                if (name.matches(".*_[0-9]+")) {
                    sb.append(':').append(name);
                } else {
                    sb.append("${").append(name).append('}');
                }
            }).build());
            final SqlBuilder unsuffixedParent = unsuffixedCustom.select("id")
                    .from("a")
                    .where(Filters.eq("id", 1))
                    .union(unsuffixedCustom.select("id").from("b").where(Filters.eq("id", 2)));
            assertEquals(Map.of("id", "${id}"), unsuffixedParent._renderedNamedParameterTokens);
            final SqlBuilder unsuffixedGrand = unsuffixedCustom.select("id")
                    .from("g")
                    .where(Filters.eq("id", 0))
                    .union(unsuffixedCustom.select("id").from(unsuffixedParent, "p"));
            assertEquals(Map.of("id", "${id}"), unsuffixedGrand._renderedNamedParameterTokens);
            final SP unsuffixedSp = unsuffixedGrand.build();
            assertEquals("SELECT id FROM g WHERE id = ${id} UNION SELECT id FROM (SELECT id FROM a WHERE id = :id_2 UNION SELECT id FROM b WHERE id = :id_3) p",
                    unsuffixedSp.query());
            assertEquals(List.of(0, 1, 2), unsuffixedSp.parameters());
        }

        // The library's own clause mutations undo a failure by truncating the buffer (the append-only invariant of _sb). An
        // overridden hook that shortens the buffer breaks that invariant: the lost text cannot be restored, which is reported
        // as a suppressed IllegalStateException instead of padding the SQL with NUL characters.
        @Test
        void testShortenedBufferInAppendOnlyMutationIsReportedNotPadded() {
            final SqlBuilder builder = new SqlBuilder(PSC.sqlDialect()) {
                @Override
                protected void appendColumnName(final String propName) {
                    if ("shrink".equals(propName)) {
                        _sb.setLength(3);
                        throw new IllegalArgumentException("hook failed");
                    }

                    super.appendColumnName(propName);
                }
            };
            builder._op = com.landawn.abacus.util.OperationType.QUERY;
            builder._propOrColumnNames = List.of("id");
            builder.from("t");
            assertEquals("SELECT id FROM t", builder._sb.toString());

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.eq("shrink", 1)));
            assertEquals("hook failed", e.getMessage());
            assertEquals(1, e.getSuppressed().length);
            assertTrue(e.getSuppressed()[0] instanceof IllegalStateException, String.valueOf(e.getSuppressed()[0]));
            assertTrue(e.getSuppressed()[0].getMessage().contains("shortened from 16 to 3"), e.getSuppressed()[0].getMessage());
            assertEquals("SEL", builder._sb.toString());
            assertTrue(builder._sb.chars().noneMatch(ch -> ch == 0));

            // The protected mutateAtomically(Runnable) restores the whole buffer, so a shrink there is undone exactly.
            final SqlBuilder protectedBuilder = PSC.select("id").from("t");
            final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> protectedBuilder.mutateAtomically(() -> {
                protectedBuilder._sb.setLength(3);
                throw new IllegalStateException("mutation failed");
            }));
            assertEquals(0, failure.getSuppressed().length);
            assertEquals("SELECT id FROM t WHERE id = ?", protectedBuilder.where(Filters.eq("id", 1)).build().query());
        }

        // Regression: every checkpoint level attached its own "Cannot restore" IllegalStateException, so nested library
        // checkpoints reported the same failure twice, and a truncating checkpoint nested in a full-copy one claimed the text
        // was lost although the enclosing checkpoint restored it exactly. Only the outermost checkpoint reports, and only if
        // the text is really lost.
        @Test
        void testShortenedBufferIsReportedOnceAndOnlyWhenNotRestored() {
            // from(Class, Collection) runs from(Collection) inside its own checkpoint: two nested truncating checkpoints.
            final SqlBuilder nested = new SqlBuilder(PSC.sqlDialect()) {
                @Override
                protected void appendColumnName(final Class<?> entityClass, final com.landawn.abacus.parser.ParserUtil.BeanInfo entityInfo,
                        final com.landawn.abacus.util.ImmutableMap<String, QueryUtil.ColumnInfo> propColumnNameMap, final String tableAlias,
                        final String propName, final String propAlias, final boolean withClassAlias, final String classAlias, final boolean isForSelect,
                        final boolean quotePropAlias) {
                    if ("shrink".equals(propName)) {
                        _sb.setLength(3);
                        throw new IllegalArgumentException("hook failed");
                    }

                    super.appendColumnName(entityClass, entityInfo, propColumnNameMap, tableAlias, propName, propAlias, withClassAlias, classAlias, isForSelect,
                            quotePropAlias);
                }
            };
            nested._op = com.landawn.abacus.util.OperationType.QUERY;
            nested._propOrColumnNames = List.of("id");
            nested.from("t");
            nested.unionSelect(List.of("id", "shrink"));
            assertEquals("SELECT id FROM t UNION ", nested._sb.toString());

            final IllegalArgumentException twice = assertThrows(IllegalArgumentException.class, () -> nested.from(Account.class, List.of("account")));
            assertEquals("hook failed", twice.getMessage());
            assertEquals(1, twice.getSuppressed().length);
            assertTrue(twice.getSuppressed()[0].getMessage().contains("shortened from 23 to 3"), twice.getSuppressed()[0].getMessage());
            assertEquals("SEL", nested._sb.toString());

            // A library clause wrapped in the protected full-copy mutateAtomically(Runnable): the enclosing checkpoint restores
            // the text exactly, so nothing is reported.
            final SqlBuilder outer = new SqlBuilder(PSC.sqlDialect()) {
                @Override
                protected void appendColumnName(final String propName) {
                    if ("shrink".equals(propName)) {
                        _sb.setLength(3);
                        throw new IllegalArgumentException("hook failed");
                    }

                    super.appendColumnName(propName);
                }
            };
            outer._op = com.landawn.abacus.util.OperationType.QUERY;
            outer._propOrColumnNames = List.of("id");
            outer.from("t");

            final IllegalArgumentException restored = assertThrows(IllegalArgumentException.class,
                    () -> outer.mutateAtomically(() -> outer.where(Filters.eq("shrink", 1))));
            assertEquals("hook failed", restored.getMessage());
            assertEquals(0, restored.getSuppressed().length);
            assertEquals("SELECT id FROM t", outer._sb.toString());
            assertEquals("SELECT id FROM t WHERE id = ?", outer.where(Filters.eq("id", 1)).build().query());

            // The nesting count is balanced after a failure: the same clause run afterwards at top level reports, exactly once.
            final SqlBuilder again = new SqlBuilder(PSC.sqlDialect()) {
                @Override
                protected void appendColumnName(final String propName) {
                    if ("shrink".equals(propName)) {
                        _sb.setLength(3);
                        throw new IllegalArgumentException("hook failed");
                    }

                    super.appendColumnName(propName);
                }
            };
            again._op = com.landawn.abacus.util.OperationType.QUERY;
            again._propOrColumnNames = List.of("id");
            again.from("t");
            assertThrows(IllegalArgumentException.class, () -> again.mutateAtomically(() -> again.where(Filters.eq("shrink", 1))));
            assertEquals(1, assertThrows(IllegalArgumentException.class, () -> again.where(Filters.eq("shrink", 1))).getSuppressed().length);
        }

        @Test
        void testSqliteBracketQuotedIdentifiersHideCommentTokens() {
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
        void testSqlServerBracketQuotedIdentifierStillOpaque() {
            final Dsl sqlServer = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());

            // [a'b] is one identifier: the quote inside it opens no string, so the trailing comment is terminated.
            assertEquals("SELECT id FROM t UNION SELECT [a'b] FROM u -- c\n ORDER BY id",
                    sqlServer.select("id").from("t").union("SELECT [a'b] FROM u -- c").orderBy("id").build().query());

            // A semicolon inside a bracket-quoted identifier is not a statement terminator.
            assertEquals("SELECT id FROM t UNION SELECT [a;b] FROM u", sqlServer.select("id").from("t").union("SELECT [a;b] FROM u").build().query());

            assertEquals("SELECT * FROM t [x -- y] -- c\n WHERE id = ?",
                    sqlServer.select("*").from("t [x -- y] -- c").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        void testSqlServerTemporaryTableIdentifiersRemainIntactInExpressions() {
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

        // Regression: outside MySQL the statement-terminator gate read the second '#' of PostgreSQL's "##" operator as a hash
        // comment, so a ';' after it was hidden and a second statement passed the set-operation / subquery gates.
        @Test
        void testStatementTerminatorGateReadsHashPairOperatorAsOneToken() {
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

        @Test
        void testStructuredClauseRenderingFailuresAreAtomic() {
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

        // Regression: the UPDATE/DELETE/INSERT target table name was appended raw, so a trailing line comment commented out the
        // SET list, the WHERE clause ("DELETE FROM t -- x WHERE id = 2" deleted every row) or the column list. It is now ended
        // with a line feed under the dialect's own comment rules, like from(String).
        @Test
        void testTargetTableNameEndingInLineCommentIsTerminated() {
            assertEquals("DELETE FROM t -- x\n WHERE id = 2", SCSB.deleteFrom("t -- x").where(Filters.eq("id", 2)).build().query());
            assertEquals("UPDATE t -- x\n SET a = 1 WHERE id = 2", SCSB.update("t -- x").set("a", 1).where(Filters.eq("id", 2)).build().query());
            assertEquals("INSERT INTO t -- x\n (a) VALUES (?)", PSC.insert("a").into("t -- x").build().query());
            assertEquals("INSERT INTO t -- x\n (a) VALUES (?)", PSC.insert("a").into("t -- x", Account.class).build().query());
            // INSERT ... SELECT, with a column list and with "*", and the (String, Class) forms of UPDATE/DELETE.
            assertEquals("INSERT INTO t2 -- x\n (a) SELECT a FROM t WHERE id = ?",
                    PSC.select("a").into("t2 -- x").from("t").where(Filters.eq("id", 1)).build().query());
            assertEquals("INSERT INTO t2 -- x\n SELECT * FROM t", PSC.select("*").into("t2 -- x").from("t").build().query());
            assertEquals("DELETE FROM account -- x\n WHERE first_name = 'a'",
                    SCSB.deleteFrom("account -- x", Account.class).where(Filters.eq("firstName", "a")).build().query());
            assertEquals("UPDATE account -- x\n SET first_name = 'a'", SCSB.update("account -- x", Account.class).set("firstName", "a").build().query());

            final Dsl mysql = lexDialect(SCSB, "MySQL");
            assertEquals("DELETE FROM t # x\n WHERE id = 2", mysql.deleteFrom("t # x").where(Filters.eq("id", 2)).build().query());
            assertEquals("UPDATE t # x\n SET a = 1 WHERE id = 2", mysql.update("t # x").set("a", 1).where(Filters.eq("id", 2)).build().query());
            assertEquals("INSERT INTO t # x\n (a) VALUES (?)", mysql.insert("a").into("t # x").build().query());

            final Dsl h2 = lexDialect(SCSB, "H2");
            assertEquals("DELETE FROM t // x\n WHERE id = 2", h2.deleteFrom("t // x").where(Filters.eq("id", 2)).build().query());
            assertEquals("UPDATE t // x\n SET a = 1 WHERE id = 2", h2.update("t // x").set("a", 1).where(Filters.eq("id", 2)).build().query());
            assertEquals("INSERT INTO t // x\n (a) VALUES (?)", h2.insert("a").into("t // x").build().query());

            // Negative controls: "//" is an operator outside H2, and plain or terminated names render as before.
            final Dsl pg = lexDialect(SCSB, "PostgreSQL");
            assertEquals("DELETE FROM t // x WHERE id = 2", pg.deleteFrom("t // x").where(Filters.eq("id", 2)).build().query());
            assertEquals("UPDATE t // x SET a = 1 WHERE id = 2", pg.update("t // x").set("a", 1).where(Filters.eq("id", 2)).build().query());
            assertEquals("INSERT INTO t // x (a) VALUES (?)", pg.insert("a").into("t // x").build().query());
            assertEquals("DELETE FROM t WHERE id = 2", SCSB.deleteFrom("t").where(Filters.eq("id", 2)).build().query());
            assertEquals("DELETE FROM t /* x */ WHERE id = 2", SCSB.deleteFrom("t /* x */").where(Filters.eq("id", 2)).build().query());
            assertEquals("DELETE FROM #tmp WHERE id = 2",
                    lexDialect(SCSB, "Microsoft SQL Server").deleteFrom("#tmp").where(Filters.eq("id", 2)).build().query());
            // "#tmp": a temporary table without product info (unchanged), a comment on MySQL (the line feed is harmless there).
            assertEquals("DELETE FROM #tmp WHERE id = 2", SCSB.deleteFrom("#tmp").where(Filters.eq("id", 2)).build().query());
            assertEquals("DELETE FROM #tmp\n WHERE id = 2", mysql.deleteFrom("#tmp").where(Filters.eq("id", 2)).build().query());
        }

        @Test
        void testToSql() {
            assertSql("SELECT acc.id AS \"id\", acc.first_name AS \"firstName\" FROM account acc WHERE acc.id = ?", List.of(1),
                    PSC.select("id", "firstName").from(Account.class).where(Filters.eq("id", 1)).build());
        }

        // Regression: the trailing-comment check had no "##"-as-operator reading, so on PostgreSQL the second '#' hid a quote
        // ("u.p ## '\n' -- c") and the real trailing "--" comment swallowed the WHERE appended after it.
        @Test
        void testTrailingCommentAfterHashPairOperatorIsTerminated() {
            final Dsl pg = lexDialect(PSC, "PostgreSQL");

            assertEquals("SELECT * FROM t JOIN u ON u.p ## '\n' -- c\n WHERE t.id = 1",
                    pg.select("*").from("t").join("u ON u.p ## '\n' -- c").where("t.id = 1").build().query());
            assertTrue(AbstractQueryBuilder.endsInsideLineComment("u.p ## '\n' -- c", false));
            assertFalse(AbstractQueryBuilder.endsInsideLineComment("u.p ## '\n' = c", false));
        }

        // Covers lone-'\r' handling of the trailing-comment check: a comment ended by '\r' is terminated with '\n' only once,
        // and a later '\n' still ends it.
        @Test
        void testTrailingCommentLoneCarriageReturnReadingEndsAtLaterLineFeed() {
            assertFalse(AbstractQueryBuilder.endsInsideLineComment("a -- x\r b\n c", false));
            assertTrue(AbstractQueryBuilder.endsInsideLineComment("a -- x\r b", false));
            assertFalse(AbstractQueryBuilder.endsInsideLineComment("a -- x\r\n b -- y\r\n", false));
            assertEquals("a -- x\r\n", QueryUtil.terminateLineComment("a -- x\r"));
            assertEquals("a -- x\r\n", QueryUtil.terminateLineComment("a -- x\r\n"));
        }

        // Regression: MySQL/MariaDB/SQLite end a line comment only at '\n', so a trailing comment closed by a lone '\r' swallowed the next clause.
        @Test
        void testTrailingLineCommentEndedByLoneCarriageReturnIsTerminatedWithLineFeed() {
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

        @Test
        void testTrueFalseKeywordsAreNotCaseConvertedByEitherRenderingPath() {
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
        void testUnterminatedBracketDoesNotHideCommentToken() {
            assertThrows(IllegalArgumentException.class, () -> PSB.update("t").set("a[1 -- x", 1));
            assertThrows(IllegalArgumentException.class, () -> PSB.update("t").set("a[/* x", 1));

            // A terminated bracket-quoted identifier still hides its content outside the MySQL/PostgreSQL families.
            assertEquals("UPDATE t SET [a--b] = ? WHERE id = ?", PSB.update("t").set("[a--b]", 1).where(Filters.eq("id", 1)).build().query());

            // PostgreSQL brackets are array subscripts, so a comment token inside them is a real comment.
            final Dsl postgres = Dsl.forDialect(PSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
            assertThrows(IllegalArgumentException.class, () -> postgres.update("t").set("a[--]", 1));
        }
    }

    @Nested
    class SubqueriesAndSetOperations {
        @Test
        void testAppendCriteriaPreflightsJoinAndSetOperationPlacement() {
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
        void testAppendCriteriaValidatesEverySetOperationOperandWithoutMutation() {
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

        // append(Condition) emitted set operations without the per-segment reset that union(...)/intersect(...)
        // perform, so the finished left operand's table alias leaked into a trailing ORDER BY (invalid on a
        // set-operation result) and a later distinct() was spliced retro-actively into that operand.
        @Test
        void testAppendedSetOperationClosesTheSegmentLikeTheUnionMethods() {
            final String viaAppend = PSC.select("firstName")
                    .from(Account.class, "acc")
                    .append(new Union(Filters.subQuery("SELECT first_name FROM archive")))
                    .orderBy("firstName")
                    .build()
                    .query();
            final String viaUnion = PSC.select("firstName")
                    .from(Account.class, "acc")
                    .union("SELECT first_name FROM archive")
                    .orderBy("firstName")
                    .build()
                    .query();

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
            assertThrows(IllegalStateException.class,
                    () -> PSC.select("id").from("t").append(new Union(Filters.subQuery("SELECT id FROM u"))).distinct().build());
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("t").union("SELECT id FROM u").distinct().build());
        }

        @Test
        void testAppendStandaloneSetOperationClauseValidatesLikeSetOperationMethods() {
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

        // Covers the single-pass child-placeholder rename (planned renames applied at once): many colliding names next to
        // many '#' temporary tables, a later operand, a custom handler, and a property named like a generated suffix.
        @Test
        void testChildPlaceholderRenameAppliesAllPlannedRenamesInOnePass() {
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
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> NSC.select("id")
                            .from("t")
                            .where(Filters.eq("id", 1))
                            .union(NSC.select("id").from("t2").where(Filters.and(Filters.expr("\"a\\\" = 1"), Filters.eq("id", 2)))));
            assertTrue(e.getMessage().contains("a backslash in quoted text"), e.getMessage());
        }

        // Covers the dialect-aware '#' and string readings of the child-placeholder rename on the remaining paths: a custom
        // handler, an IN snapshot, a derived table, "##g", MySQL "..." escapes, and the default's non-ambiguous backslashes.
        @Test
        void testChildPlaceholderRenameHashAndQuoteReadingsOnAllPaths() {
            final Dsl at = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append('@').append(name)).build());

            assertEquals("SELECT id FROM t WHERE id = @id UNION SELECT id FROM t2 x #it's\n WHERE id = @id_2",
                    at.select("id")
                            .from("t")
                            .where(Filters.eq("id", 1))
                            .union(at.select("id").from("t2 x #it's\n").where(Filters.eq("id", 2)))
                            .build()
                            .query());
            assertEquals("SELECT id FROM t WHERE (id = :id) AND (id IN (SELECT id FROM t2 x #it's\n WHERE id = :id_2))", NSC.select("id")
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

        // Regression: MySQL "..." string literals honor backslash escapes, but the rename scanner read them with quote doubling only.
        @Test
        void testChildPlaceholderRenameHonorsMySqlDoubleQuotedStringEscapes() {
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
            assertThrows(IllegalArgumentException.class,
                    () -> NSC.select("id")
                            .from("t")
                            .where(Filters.eq("id", 1))
                            .union(NSC.select("id").from("t2").where(Filters.and(Filters.expr("n <> \"a\\\"\""), Filters.eq("id", 2)))));
        }

        // Regression: the placeholder-rename scanner read every non-MySQL '#word' as a SQL Server temp table, so a quote in a hash comment hid a colliding child placeholder.
        @Test
        void testChildPlaceholderRenameSkipsHashCommentsOutsideSqlServer() {
            AbstractQueryBuilder.SP sp = NSC.select("id")
                    .from("t")
                    .where(Filters.eq("id", 1))
                    .union(NSC.select("id").from("t2 #TODO: don't scan\n").where(Filters.eq("id", 2)))
                    .build();
            assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM t2 #TODO: don't scan\n WHERE id = :id_2", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
            assertEquals(Arrays.asList("id", "id_2"), new ArrayList<>(ParsedSql.parse(sp.query()).namedParameters()));

            sp = MSC.select("id")
                    .from("t")
                    .where(Filters.eq("id", 1))
                    .union(MSC.select("id").from("t2 #TODO: don't scan\n").where(Filters.eq("id", 2)))
                    .build();
            assertEquals("SELECT id FROM t WHERE id = #{id} UNION SELECT id FROM t2 #TODO: don't scan\n WHERE id = #{id_2}", sp.query());

            final Dsl pg = Dsl.forDialect(NSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
            sp = pg.select("id").from("t").where(Filters.eq("id", 1)).union(pg.select("id").from("t2 #it's\n").where(Filters.eq("id", 2))).build();
            assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM t2 #it's\n WHERE id = :id_2", sp.query());

            // A temporary-table identifier in FROM context is still data.
            sp = NSC.select("id").from("t").where(Filters.eq("id", 1)).union(NSC.select("id").from("#tmp").where(Filters.eq("id", 2))).build();
            assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM #tmp WHERE id = :id_2", sp.query());
        }

        @Test
        void testCollectionAndMapArgumentsRenderTheirValidatedSnapshots() {
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
        void testCompletedCriteriaSetOperationAllowsOnlyCompoundResultClauses() {
            final Criteria union = Criteria.builder().union(Filters.subQuery("SELECT id FROM archived_users")).build();
            final SqlBuilder compound = PSC.select("id").from("users").append(union);

            assertThrows(IllegalStateException.class, () -> compound.where(Filters.eq("active", true)));
            assertThrows(IllegalStateException.class, () -> compound.groupBy("id"));
            assertThrows(IllegalStateException.class, () -> compound.having("COUNT(*) > 1"));
            assertThrows(IllegalStateException.class, () -> compound.join("orders"));

            assertEquals("SELECT id FROM users UNION SELECT id FROM archived_users ORDER BY id LIMIT 10", compound.orderBy("id").limit(10).build().query());
        }

        // A Criteria carrying both a select modifier and a set operation lost the modifier: it was applied
        // only after the set operation closed the segment (resetting the SELECT-keyword insertion point),
        // so build() rejected it as an unattached staged modifier. It must be spliced into the left segment
        // it was validated against, exactly like selectModifier("DISTINCT") called before union(...).
        @Test
        void testCriteriaSelectModifierWithCriteriaSetOperation() {
            final String viaCriteria = PSC.select("id")
                    .from("t")
                    .append(Criteria.builder().selectModifier("DISTINCT").union(Filters.subQuery("SELECT id FROM u")).build())
                    .build()
                    .query();
            final String viaMethods = PSC.select("id").from("t").selectModifier("DISTINCT").union("SELECT id FROM u").build().query();

            assertEquals("SELECT DISTINCT id FROM t UNION SELECT id FROM u", viaCriteria);
            assertEquals(viaMethods, viaCriteria);
        }

        // Same suffix high-water mark under a custom named-parameter handler: every parameter is converted.
        @Test
        void testCustomHandlerRewritesEveryParameterOfComposedSnapshot() {
            final Dsl at = Dsl.forDialect(Dsl.NSC.sqlDialect().toBuilder().namedParameterHandler((sql, name) -> sql.append('@').append(name)).build());
            final SP result = at.select("id").from(compoundWithSkippedSuffix(Dsl.NSC), "inner_query").build();

            assertEquals(List.of(1, 2, 3), result.parameters());
            assertTrue(result.query().contains("id = @id_3"), result.query());
            assertFalse(result.query().contains(":id"), result.query());
        }

        // Regression: a custom named-parameter handler rendering "#name" tokens was read as a hash comment by the rename
        // scanner outside SQL Server, so a colliding child placeholder kept its name (two "#id" bound to different values).
        @Test
        void testCustomHashNamedParameterTokensAreRenamedAcrossSetOperation() {
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

        @Test
        void testDialectTokenizerConfigAffectsRawSubQueryInspection() {
            final SqlParser.TokenizerConfig tokenizerConfig = SqlParser.tokenizerConfigBuilder().withSeparator("::").build();
            final Dsl dsl = Dsl.forDialect(Dsl.SCSB.sqlDialect().toBuilder().tokenizerConfig(tokenizerConfig).build());

            assertEquals("SELECT id FROM users UNION SELECT::1", dsl.select("id").from("users").union("SELECT::1").build().query());
            assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.select("id").from("users").union("SELECT::1"));
        }

        @Test
        void testDialectTokenizerConfigAlsoGovernsSetOperationReadOnlyValidation() {
            final SqlParser.TokenizerConfig tokenizerConfig = SqlParser.tokenizerConfigBuilder().withSeparator("#foo").build();
            final Dsl dsl = Dsl.forDialect(Dsl.SCSB.sqlDialect().toBuilder().tokenizerConfig(tokenizerConfig).build());

            // Under the default tokenizer, "#foo" starts a hash comment and hides the later UPDATE.
            // For this dialect it is a separator, so the full multi-statement input must be inspected.
            assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from("users").union("SELECT 1 #foo ; UPDATE users SET active = false"));
        }

        @Test
        void testExcept_Collection() {
            final String sql = PSC.select("id").from("users").exceptSelect(Collections.singletonList("id")).from("admins").build().query();

            assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
        }

        @Test
        void testExcept_Query() {
            final String sql = PSC.select("id").from("users").except("SELECT id FROM admins").build().query();

            assertTrue(sql.contains("EXCEPT SELECT id FROM admins"));
        }

        @Test
        void testExcept_SingleNonSubQueryStringRejected() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").except("id"));
            assertTrue(ex.getMessage().contains("SELECT sub-query"));
        }

        @Test
        void testHashMinusOperatorFollowedByLineCommentIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").where(Filters.eq("id #--x\n OR 1", 1)).build());
            assertThrows(IllegalArgumentException.class, () -> PSB.select("id #-- x\n, secret").from("t").build());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").groupBy("id #--x").build());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").orderBy("id #--x").build());

            // The PostgreSQL #- operator itself is still accepted.
            assertEquals("SELECT id FROM t WHERE doc #- '{a}' = ?", PSB.select("id").from("t").where(Filters.eq("doc #- '{a}'", 1)).build().query());
        }

        @Test
        void testIntersect_Collection() {
            final String sql = PSC.select("id").from("users").intersectSelect(Collections.singletonList("id")).from("admins").build().query();

            assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
        }

        @Test
        void testIntersect_Query() {
            final String sql = PSC.select("id").from("users").intersect("SELECT id FROM admins").build().query();

            assertTrue(sql.contains("INTERSECT SELECT id FROM admins"));
        }

        @Test
        void testIntersect_SingleNonSubQueryStringRejected() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").intersect("id"));
            assertTrue(ex.getMessage().contains("SELECT sub-query"));
        }

        @Test
        void testMinus_Collection() {
            final String sql = PSC.select("id").from("users").minusSelect(Collections.singletonList("id")).from("admins").build().query();

            assertTrue(sql.contains("MINUS SELECT id FROM admins"));
        }

        @Test
        void testMinus_Query() {
            final String sql = PSC.select("id").from("users").minus("SELECT id FROM admins").build().query();

            assertTrue(sql.contains("MINUS SELECT id FROM admins"));
        }

        @Test
        void testMinus_SingleNonSubQueryStringRejected() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").minus("id"));
            assertTrue(ex.getMessage().contains("SELECT sub-query"));
        }

        // Regression: MySQL starts a "--" comment only when whitespace or a control character follows, so "a--1" is
        // a - (-1) there; the shared tokenizer dropped "--1 > 0" as a comment and rendered "WHERE a" under a MySQL builder.
        @Test
        void testMySqlRawExpressionKeepsDashPairWithoutWhitespaceAsMinusOperators() {
            final Dsl mysql = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("MySQL")).build());

            assertEquals("SELECT id FROM t WHERE a- -1 > 0 ORDER BY id",
                    mysql.select("id").from("t").where(Filters.expr("a--1 > 0")).orderBy("id").build().query());
            assertEquals("SELECT id FROM t WHERE a- - -1 > 0", mysql.select("id").from("t").where(Filters.expr("a---1 > 0")).build().query());
            // Quoted text is untouched, including after a backslash-escaped quote, and real MySQL comments are still dropped.
            assertEquals("SELECT id FROM t WHERE note = 'x--y' AND b = 'it\\'s--x' AND a- -1 > 0",
                    mysql.select("id").from("t").where(Filters.expr("note = 'x--y' AND b = 'it\\'s--x' AND a--1 > 0")).build().query());
            assertEquals("SELECT id FROM t WHERE a = 1  ORDER BY id",
                    mysql.select("id").from("t").where(Filters.expr("a = 1 -- a--1")).orderBy("id").build().query());
            assertEquals("SELECT id FROM t WHERE a = 1  ORDER BY id",
                    mysql.select("id").from("t").where(Filters.expr("a = 1 # a--1")).orderBy("id").build().query());

            // In standard SQL (and without product info) "--" always starts a comment.
            assertEquals("SELECT id FROM t WHERE a", PSC.select("id").from("t").where(Filters.expr("a--1 > 0")).build().query());
        }

        // Same suffix high-water mark through a snapshot nested in a predicate subquery.
        @Test
        @SuppressWarnings("deprecation")
        void testNestedPredicateSnapshotRetainsSkippedParameterSuffixes() {
            for (final Dsl dsl : new Dsl[] { Dsl.NSC, Dsl.MSC }) {
                final SubQuery inner = dsl.select("id").from("archive").where(Filters.eq("id", 3)).toSubQuery();
                final SubQuery middle = dsl.select("id")
                        .from("users")
                        .where(Filters.and(Filters.eq("id", 1), Filters.eq("id_2", 2), Filters.in("id", inner)))
                        .toSubQuery();
                final SP result = dsl.select("id").from("outer_table").where(Filters.and(Filters.eq("id_3", 4), Filters.in("id", middle))).build();

                assertDistinctBindings(result, List.of(4, 1, 2, 3));
            }
        }

        @Test
        void testOrderByAfterUnionSelectFromEntityAliasIsUnqualified() {
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
            assertEquals("SELECT acc.firstName FROM account acc UNION SELECT acc2.firstName FROM account acc2 WHERE acc2.firstName = ? ORDER BY firstName DESC",
                    sql);

            // Same result as the builder-operand overload, which already dropped the alias.
            sql = PSB.select("firstName")
                    .from(Account.class, "acc")
                    .union(PSB.select("firstName").from(Account.class, "acc2"))
                    .orderBy("firstName")
                    .build()
                    .query();
            assertEquals("SELECT acc.firstName FROM account acc UNION SELECT acc2.firstName FROM account acc2 ORDER BY firstName", sql);

            // A plain (non-compound) query keeps alias-qualified ORDER BY columns.
            assertEquals("SELECT acc.firstName FROM account acc ORDER BY acc.firstName",
                    PSB.select("firstName").from(Account.class, "acc").orderBy("firstName").build().query());
        }

        @Test
        void testProtectedHelpersRejectNullArgumentsWithIllegalArgumentException() {
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
        void testRawAndSnapshotSubqueryTrailingCommentsDoNotSwallowClosingParenthesis() {
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

        // Regression: a raw operand with bindings had its "?"s inlined as literals under RAW_SQL, but only the raw text was
        // validated. Outside MySQL the gate reads the emitted literals under both string-escape conventions, as it does for
        // union(String), so "'\''; DELETE FROM t; -- '" (a terminator under the backslash reading) is now rejected the same way.
        @Test
        void testRawSetOperationOperandWithInlinedBindingsIsValidatedAsEmitted() {
            final String backslash = String.valueOf((char) 92);
            final String value = backslash + "'; DELETE FROM t; -- ";
            final String emittedOperand = "SELECT x FROM y WHERE p = '" + backslash + "''; DELETE FROM t; -- '";

            for (final String product : List.of("PostgreSQL", "Oracle", "H2", "Microsoft SQL Server", "SQLite")) {
                final Dsl raw = lexDialect(SCSB, product);
                final SubQuery operand = Filters.subQuery("SELECT x FROM y WHERE p = ?", List.of(value));

                for (final Condition form : List.of(Filters.union(operand), Criteria.builder().union(operand).build(),
                        Criteria.builder().distinct().unionAll(operand).build())) {
                    final SqlBuilder builder = raw.select("id").from("t");
                    assertThrows(IllegalArgumentException.class, () -> builder.append(form), product + " / " + form);
                    assertEquals("SELECT id FROM t", builder._sb.toString());
                    assertNull(builder._selectModifier);
                }

                // Same verdict as the string spelling of the emitted operand.
                assertThrows(IllegalArgumentException.class, () -> raw.select("id").from("t").union(emittedOperand));
            }

            // With placeholders kept, nothing is inlined, so the bound value cannot change the statement.
            final SP bound = PSC.select("id").from("t").append(Filters.union(Filters.subQuery("SELECT x FROM y WHERE p = ?", List.of(value)))).build();
            assertEquals("SELECT id FROM t UNION SELECT x FROM y WHERE p = ?", bound.query());
            assertEquals(List.of(value), bound.parameters());

            // A harmless raw operand with bindings is accepted under every SQL policy, its literals included.
            final SubQuery harmless = Filters.subQuery("SELECT x FROM y WHERE p = ? AND q = ?", List.of("a;b -- c /* d", 7));
            assertEquals("SELECT id FROM t UNION SELECT x FROM y WHERE p = ? AND q = ?",
                    PSC.select("id").from("t").append(Filters.union(harmless)).build().query());
            assertEquals("SELECT id FROM t UNION SELECT x FROM y WHERE p = :param AND q = :param_2",
                    NSC.select("id").from("t").append(Filters.union(harmless)).build().query());
            assertEquals("SELECT id FROM t UNION SELECT x FROM y WHERE p = #{param} AND q = #{param_2}",
                    MSC.select("id").from("t").append(Criteria.builder().union(harmless).build()).build().query());

            for (final Dsl raw : List.of(SCSB, lexDialect(SCSB, "PostgreSQL"), lexDialect(SCSB, "MySQL"), lexDialect(SCSB, "Microsoft SQL Server"))) {
                assertEquals("SELECT id FROM t UNION SELECT x FROM y WHERE p = 'a;b -- c /* d' AND q = 7",
                        raw.select("id").from("t").append(Filters.union(harmless)).build().query());
                assertEquals("SELECT id FROM t UNION SELECT x FROM y WHERE p = 'a;b -- c /* d' AND q = 7",
                        raw.select("id").from("t").append(Criteria.builder().union(harmless).build()).build().query());
            }
        }

        @Test
        void testRawSqlInlinedNegativeBindingAfterMinusDoesNotFormLineComment() {
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

        // Regression: a raw sub-query '?' right after ':' (PostgreSQL array slice) was renamed to "::param" under NAMED_SQL -- a type cast, not a parameter.
        @Test
        void testRawSubQueryPlaceholderAfterColonIsNotRenamedIntoTypeCast() {
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

        // Covers the NAMED_SQL raw sub-query rename next to ':' beyond "arr[1:?]": whitespace before the colon and an
        // identifier slice bound; a custom handler whose token does not start with ':' needs no separating space.
        @Test
        void testRawSubQueryPlaceholderAfterColonVariants() {
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
                    atNamed.select("id")
                            .from("t")
                            .where(Filters.in("id", Filters.subQuery("SELECT id FROM u WHERE arr[1:?] = x", Arrays.asList(3))))
                            .build()
                            .query());
        }

        @Test
        @SuppressWarnings("deprecation")
        void testRawSubQuerySqlExpressionBindingRendersIdenticallyUnderEveryPolicy() {
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
            assertEquals(
                    "SELECT id FROM t WHERE id IN (SELECT id FROM o WHERE a = :param AND total = (SELECT MAX(total) FROM o WHERE s = :param_2) AND b = :param_3)",
                    sp.query());
            assertEquals(Arrays.asList(1, "OPEN", 2), sp.parameters());
        }

        // The toSql pre-check reads these items as one converted identifier ("##g.a_into_evil"), while the child builder emits
        // "##g.a into evil AS ..." (on SQL Server "#t.a into evil"), a real SELECT ... INTO: only the check of the emitted
        // operand rejects them, and the rejection is rolled back completely (select modifier and named-parameter state included).
        @Test
        void testSetOperationOperandOnlyTheEmittedTextRevealsIsRejected() {
            final Dsl mssql = lexDialect(PSC, "Microsoft SQL Server");
            final Object[][] postCheckOnly = { { PSC, "##g.a into evil" }, { NSC, "##g.a into evil" }, { PLC, "##g.a into evil" }, { mssql, "#t.a into evil" },
                    { mssql, "#a into outfile '/x'" } };

            for (final Object[] c : postCheckOnly) {
                final Dsl dsl = (Dsl) c[0];
                final String item = (String) c[1];
                final SubQuery operand = new SubQuery("user_account", List.of(item), null);
                assertFalse(operand.toSql(NamingPolicy.SNAKE_CASE).contains(" into "), operand.toSql(NamingPolicy.SNAKE_CASE));

                for (final Condition form : List.of(Filters.union(operand), Criteria.builder().union(operand).build(),
                        Criteria.builder().distinct().where(Filters.eq("id", 1)).unionAll(operand).build())) {
                    final SqlBuilder builder = dsl.select("id").from("t");
                    final String before = builder._sb.toString();
                    final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> builder.append(form), item + " / " + form);
                    // Judged on the emitted text, not on toSql's "..._into_evil".
                    assertTrue(e.getMessage().contains(" into "), e.getMessage());
                    assertEquals(before, builder._sb.toString());
                    assertNull(builder._selectModifier);
                    assertTrue(builder._parameters.isEmpty());
                    assertEquals(dsl.select("id").from("t").where(Filters.eq("id", 2)).build().query(), builder.where(Filters.eq("id", 2)).build().query());
                }
            }
        }

        @Test
        void testSetOperationRejectsStatementTerminatorsWithoutChangingParent() {
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
        void testSetOperationsAndDirectionsRejectInvalidInputs() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").union("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").unionAll("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").intersect("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").except("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").minus("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").groupBy("id", null));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").orderBy("id", null));
        }

        @Test
        void testSetOperationTrailingLineCommentsDoNotSwallowFollowingOperandsOrOrderBy() {
            for (final String comment : new String[] { "-- branch hint", "# branch hint" }) {
                assertEquals("SELECT id FROM users UNION SELECT id FROM archived_users " + comment + "\n ORDER BY id",
                        PSC.select("id").from("users").union("SELECT id FROM archived_users " + comment).orderBy("id").build().query());
                assertEquals("SELECT id FROM users UNION SELECT id FROM archived_users " + comment + "\n INTERSECT SELECT id FROM active_users",
                        PSC.select("id")
                                .from("users")
                                .union("SELECT id FROM archived_users " + comment)
                                .intersect("SELECT id FROM active_users")
                                .build()
                                .query());
            }
        }

        /**
         * Regression test: calling {@code set(Object, Set)} with a {@code null} entity must
         * fail fast with a descriptive {@link IllegalArgumentException} rather than throwing
         * a raw {@link NullPointerException} from {@code entity.getClass()}.
         */
        @Test
        void testSetWithEntityObjectNullThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((Object) null));
            assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((Object) null, null));
        }

        // Regression: an INTO item of a structured set-operation operand was accepted. These items are now caught by the toSql
        // pre-check (SubQuery.toSql renders them verbatim); testSetOperationOperandOnlyTheEmittedTextRevealsIsRejected covers the
        // items only the emitted-text check catches.
        @Test
        void testStructuredSetOperationOperandIsValidatedAsEmitted() {
            final List<String> unsafeItems = List.of("first_name into outfile '/tmp/#f'", "first_name into dumpfile '/tmp/--x'", "'#' into @v",
                    "first_name into outfile '/tmp/f'");

            for (final Dsl dsl : new Dsl[] { PSC, PLC }) {
                for (final String item : unsafeItems) {
                    final SubQuery operand = new SubQuery("user_account", List.of(item), null);
                    final List<Condition> forms = List.of(Filters.union(operand), Filters.unionAll(operand), Filters.intersect(operand),
                            Filters.except(operand), Filters.minus(operand), Criteria.builder().union(operand).build(),
                            Criteria.builder().unionAll(operand).build(), Criteria.builder().where(Filters.eq("id", 1)).except(operand).build(),
                            Criteria.builder().distinct().union(operand).build());

                    for (final Condition form : forms) {
                        final SqlBuilder builder = dsl.select("id").from("t");
                        assertThrows(IllegalArgumentException.class, () -> builder.append(form), item + " / " + form);
                        assertThrows(IllegalArgumentException.class, () -> builder.appendIf(true, form), item + " / " + form);
                        // Rolled back completely: the builder stays usable.
                        assertEquals("SELECT id FROM t", builder._sb.toString());
                        assertTrue(builder._parameters.isEmpty());
                        assertEquals("SELECT id FROM t WHERE id = ?", builder.where(Filters.eq("id", 2)).build().query());
                    }
                }
            }

            // Harmless structured operands, including comment characters inside literals, render as before.
            final SubQuery harmless = new SubQuery("user_account", List.of("firstName", "'Item #1' AS s", "CONCAT(firstName, ' -- ', lastName) AS x"),
                    Filters.eq("id", 1));
            final String pscExpected = "SELECT id FROM t UNION SELECT first_name AS \"firstName\", 'Item #1' AS s, CONCAT(first_name, ' -- ', last_name) AS x"
                    + " FROM user_account WHERE id = ?";
            final String plcExpected = "SELECT id FROM t UNION SELECT firstName, 'Item #1' AS s, CONCAT(firstName, ' -- ', lastName) AS x FROM user_account WHERE id = ?";

            for (final Condition form : List.of(Filters.union(harmless), Criteria.builder().union(harmless).build())) {
                final SP psc = PSC.select("id").from("t").append(form).build();
                assertEquals(pscExpected, psc.query());
                assertEquals(List.of(1), psc.parameters());
                assertEquals(plcExpected, PLC.select("id").from("t").append(form).build().query());
            }
        }

        @Test
        void testUnion_Collection() {
            final String sql = PSC.select("id").from("users").unionSelect(Collections.singletonList("id")).from("admins").build().query();

            assertTrue(sql.contains("UNION SELECT id FROM admins"));
        }

        @Test
        void testUnion_ParenthesizedNonSelectStillRejected() {
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
        void testUnion_Query() {
            final String sql = PSC.select("id").from("users").union("SELECT id FROM admins").build().query();

            assertTrue(sql.contains("UNION SELECT id FROM admins"));
        }

        @Test
        void testUnion_SingleNonSubQueryStringRejected() {
            // union(String) is reserved for a complete SELECT sub-query; a bare column name is rejected up front
            // instead of silently becoming a column list that fails later with an unrelated "from() must be called" error.
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").union("id"));
            assertTrue(ex.getMessage().contains("SELECT sub-query"));
        }

        @Test
        void testUnionAll_Collection() {
            final String sql = PSC.select("id").from("users").unionAllSelect(Collections.singletonList("id")).from("admins").build().query();

            assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
        }

        @Test
        void testUnionAll_Query() {
            final String sql = PSC.select("id").from("users").unionAll("SELECT id FROM admins").build().query();

            assertTrue(sql.contains("UNION ALL SELECT id FROM admins"));
        }

        @Test
        void testUnionAll_SingleNonSubQueryStringRejected() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("users").unionAll("id"));
            assertTrue(ex.getMessage().contains("SELECT sub-query"));
        }

        @Test
        void testUnionChildPlaceholderRenameUsesBackslashEscapesOnMySql() {
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

        // Child-placeholder renaming must use the target dialect's string-literal convention: 'C:\' is a
        // complete literal in standard SQL, so the later child :id must still be renamed.
        @Test
        void testUnionChildPlaceholderRenameUsesStandardStringsOnPostgreSql() {
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
        void testUnionRenameWithCustomWhitespaceLedSeparatorAndSubscripts() {
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
    }

    @Nested
    class Updates {
        @Test
        void testForUpdate() {
            final String sql = PSC.select("*").from("users").forUpdate().build().query();

            assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        void testForUpdate_idempotencyGuard() {
            // Calling forUpdate() twice must not produce "FOR UPDATE FOR UPDATE".
            assertThrows(IllegalStateException.class, () -> PSC.select("*").from("users").forUpdate().forUpdate());
        }

        @Test
        void testSet_ObjectMapHonorsExcludedProperties() {
            java.util.Map<String, Object> props = new java.util.LinkedHashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            String sql = PSC.update("account").set(props, Collections.singleton("lastName")).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("first_name = ?"));
            assertTrue(!sql.contains("last_name = ?"));
        }

        @Test
        void testSet_ObjectStringDelegatesToColumnSet() {
            String sql = PSC.update("account").set((Object) "firstName").where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        void testSetAssignmentIgnoresEqualsInsideDollarQuotedSubscripts() {
            final Dsl postgres = lexDialect(PSC, "PostgreSQL");
            assertSql("UPDATE people SET items[$tag$=$tag$] = 1 WHERE id = ?", List.of(7),
                    postgres.update("people").set("items[$tag$=$tag$] = 1").where(Filters.eq("id", 7)).build());
            assertSql("UPDATE people SET items[$$=$$] = ? WHERE id = ?", List.of(7),
                    postgres.update("people").set("items[$$=$$]").where(Filters.eq("id", 7)).build());
        }

        // Regression: set((Object) map, excluded) whose exclusions removed every key reported "'entity' cannot be null or empty".
        @Test
        void testSetMapEntityValidationMessages() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set((Object) Map.of("a", 1), Set.of("a")));
            assertEquals("No properties remain after exclusions are applied", e.getMessage());

            // An entity map that is empty to begin with still names the parameter.
            e = assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set(new LinkedHashMap<>(), Set.of("a")));
            assertTrue(e.getMessage().contains("entity"), e.getMessage());

            final Map<Object, Object> nonStringKey = new LinkedHashMap<>();
            nonStringKey.put(1, "x");
            e = assertThrows(IllegalArgumentException.class, () -> PSC.update("t").set(nonStringKey));
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

        @Test
        void testSetObjectMapWithoutExclusionsKeepsBindings() {
            final Object properties = Map.of("firstName", "Alice");
            assertSql("UPDATE people SET first_name = ? WHERE id = ?", List.of("Alice", 7),
                    PSC.update("people").set(properties, Set.of()).where(Filters.eq("id", 7)).build());
        }

        @Test
        void testSetObjectOverloadRejectsArray() {
            assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set(new String[] { "firstName", "lastName" }));
        }

        @Test
        void testSetObjectOverloadRejectsCollection() {
            assertThrows(IllegalArgumentException.class, () -> PSC.update("account").set((Object) Arrays.asList("firstName", "lastName")));
        }

        @Test
        void testSetRenderingFailuresAndLateSetCallsAreAtomic() {
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

        // Regression: set(...) validation messages named internal parameters ("propOrColumnNames[0]", "props") instead of the caller's.
        @Test
        void testSetValidationMessagesNameCallerParameter() {
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

        @Test
        void testSetWithEntityClass() {
            String sql = PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("UPDATE account SET"), "set(Class) must render an UPDATE ... SET: " + sql);
            assertTrue(sql.contains("first_name = ?"), "set(Class) must include updatable properties: " + sql);
        }

        @Test
        void testSetWithEntityClassAndExcludedPropNames() {
            Set<String> excluded = Collections.singleton("firstName");
            String sql = PSC.update("account").set(Account.class, excluded).where(Filters.eq("id", 1)).build().query();

            assertFalse(sql.contains("first_name = ?"), "excluded property must not be updated: " + sql);
            assertTrue(sql.contains("last_name = ?"), "non-excluded property must be updated: " + sql);
        }

        @Test
        void testSetWithEntityObject() {
            final Account a = new Account();
            a.setFirstName("F");
            a.setLastName("L");

            final String sql = PSC.update("account").set(a).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        void testSetWithEntityObjectAndExcludedPropNames() {
            final Account a = new Account();
            a.setFirstName("F");
            a.setLastName("L");

            final Set<String> excluded = java.util.Set.of("lastName");
            final String sql = PSC.update("account").set(a, excluded).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("first_name = ?"));
            assertFalse(sql.contains("last_name = ?"));
        }

        @Test
        void testUpdate() {
            String sql = PSC.update(Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE"));
            assertTrue(sql.contains("SET"));
        }

        @Test
        void testUpdateAllowsIbatisPlaceholderExpression() {
            String sql = PSC.update("users").set("name = #{name}").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("#{name}"));
        }

        @Test
        void testUpdateWithSet() {
            String sql = PSC.update("accounts").set("status", "inactive", "updated_at", SqlExpression.of("NOW()")).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SET"));
        }
    }

    @Nested
    class Validation {
        @Test
        void testClosedBuilderStatePrecedesInvalidClauseArguments() {
            assertClosedFirst(builder -> builder.from((String) null));
            assertClosedFirst(builder -> builder.from((String[]) null));
            assertClosedFirst(builder -> builder.from((Collection<String>) null));
            assertClosedFirst(builder -> builder.from((Class<?>) null));
            assertClosedFirst(builder -> builder.join((Class<?>) null));
            assertClosedFirst(builder -> builder.on((String) null));
            assertClosedFirst(builder -> builder.on((Condition) null));
            assertClosedFirst(builder -> builder.using((String[]) null));
            assertClosedFirst(builder -> builder.where((Condition) null));
            assertClosedFirst(builder -> builder.having((String) null));
            assertClosedFirst(builder -> builder.groupBy((Map<String, SortDirection>) null));
            assertClosedFirst(builder -> builder.groupByAsc((String[]) null));
            assertClosedFirst(builder -> builder.orderBy((Collection<String>) null));
            assertClosedFirst(builder -> builder.orderByDesc((String[]) null));
            assertClosedFirst(builder -> builder.append((Condition) null));
        }

        @Test
        void testClosedBuilderStatePrecedesNullTerminalCallbacks() {
            assertClosedFirst(builder -> builder.apply((Throwables.Function<SP, Object, RuntimeException>) null));
            assertClosedFirst(builder -> builder.apply((Throwables.BiFunction<String, java.util.List<Object>, Object, RuntimeException>) null));
            assertClosedFirst(builder -> builder.accept((Throwables.Consumer<SP, RuntimeException>) null));
            assertClosedFirst(builder -> builder.accept((Throwables.BiConsumer<String, java.util.List<Object>, RuntimeException>) null));
        }

        // Regression: an entity's @Table name ending in a line comment let the comment swallow the alias ("FROM line_comment_table -- x o"),
        // and ending that comment with a line feed before the alias instead could leave the alias as the table (MySQL read "FROM #tmp",
        // line feed, "h" as "FROM h"). Such a name is now rejected by every entity FROM/JOIN path, leaving the builder unchanged; ordinary
        // names and SQL Server temporary-table names render as before.
        @Test
        void testEntityTableNameEndingInLineCommentIsRejected() {
            final String lineTable = "tableName must not end inside a line comment: line_comment_table -- x";
            final String dashOnly = "tableName must not end inside a line comment: -- x";
            final Condition idEq = Filters.eq("id", 1);

            for (final Dsl dsl : List.of(PSC, SCSB, lexDialect(PSC, "MySQL"), lexDialect(PSC, "PostgreSQL"), lexDialect(PSC, "Microsoft SQL Server"),
                    lexDialect(PSC, "H2"), lexDialect(PSC, "SQLite"), lexDialect(PSC, "Oracle"))) {
                // Builder paths: from(Class[, alias]), selectFrom, selectCountFrom and the entity joins.
                assertEquals(lineTable, assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from(LineCommentTableEntity.class)).getMessage());
                assertEquals(lineTable,
                        assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from(LineCommentTableEntity.class, "o")).getMessage());
                assertEquals(lineTable, assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from(LineCommentAliasedEntity.class)).getMessage());
                assertEquals(lineTable, assertThrows(IllegalArgumentException.class, () -> dsl.selectFrom(LineCommentAliasedEntity.class, "o")).getMessage());
                assertEquals(lineTable, assertThrows(IllegalArgumentException.class, () -> dsl.selectCountFrom(LineCommentTableEntity.class)).getMessage());
                assertEquals(lineTable,
                        assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from("t").join(LineCommentAliasedEntity.class, "o")).getMessage());
                assertEquals(lineTable,
                        assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from("t").leftJoin(LineCommentTableEntity.class)).getMessage());
                assertEquals(dashOnly,
                        assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from(CommentOnlyTableEntity.class, "o")).getMessage());
                assertEquals(dashOnly,
                        assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from("t").crossJoin(CommentOnlyTableEntity.class)).getMessage());
                // The FROM lists composed without a builder: sub-entity and multi-entity selectFrom.
                assertEquals(lineTable,
                        assertThrows(IllegalArgumentException.class, () -> dsl.selectFrom(LineCommentAliasedEntity.class, "o", true)).getMessage());
                assertEquals(lineTable, assertThrows(IllegalArgumentException.class, () -> dsl.selectFrom(
                        List.of(Selection.builder(LineCommentTableEntity.class).build(), Selection.builder(AliasedCustomer.class).tableAlias("c").build())))
                                .getMessage());
                assertEquals(dashOnly, assertThrows(IllegalArgumentException.class,
                        () -> dsl.selectFrom(List.of(Selection.builder(CommentOnlyTableEntity.class).tableAlias("o").build()))).getMessage());
                // A structured sub-query on such an entity, through the condition entry points.
                assertEquals(lineTable,
                        assertThrows(IllegalArgumentException.class,
                                () -> dsl.select("id").from("t").where(Filters.in("id", Filters.subQuery(LineCommentTableEntity.class, List.of("id"), idEq))))
                                        .getMessage());
                assertEquals(lineTable, assertThrows(IllegalArgumentException.class,
                        () -> dsl.renderCondition(Filters.exists(Filters.subQuery(LineCommentTableEntity.class, List.of("id"), idEq)))).getMessage());
            }

            assertEquals(lineTable,
                    assertThrows(IllegalArgumentException.class,
                            () -> AbstractQueryBuilder.getFromClause(List.of(Selection.builder(LineCommentTableEntity.class).build()), NamingPolicy.SNAKE_CASE))
                                    .getMessage());
            assertEquals(lineTable, assertThrows(IllegalArgumentException.class,
                    () -> AbstractQueryBuilder.buildFromTableRefs(LineCommentAliasedEntity.class, "o", null, NamingPolicy.SNAKE_CASE)).getMessage());

            // A rejected from/join leaves the builder, and its entity mapping, as it was.
            final SqlBuilder from = SCSB.select("id", "firstName");
            assertThrows(IllegalArgumentException.class, () -> from.from(LineCommentAliasedEntity.class, "o"));
            assertEquals(SCSB.select("id", "firstName").from("t").where(Filters.eq("firstName", "v")).build().query(),
                    from.from("t").where(Filters.eq("firstName", "v")).build().query());
            final SqlBuilder join = SCSB.select("id").from("t");
            assertThrows(IllegalArgumentException.class, () -> join.join(LineCommentAliasedEntity.class, "o"));
            assertEquals("SELECT id FROM t WHERE id = 1", join.where(idEq).build().query());

            // MySQL and MariaDB read '#' as a comment, so "#tmp" and "dbo.#tmp" end inside one there.
            final String hashTmp = "tableName must not end inside a line comment: #tmp";
            for (final String product : List.of("MySQL", "MariaDB")) {
                final Dsl mysql = lexDialect(PSC, product);
                assertEquals(hashTmp, assertThrows(IllegalArgumentException.class, () -> mysql.select("id").from(HashTempTableEntity.class)).getMessage(),
                        product);
                assertEquals(hashTmp, assertThrows(IllegalArgumentException.class, () -> mysql.select("id").from(HashTempTableEntity.class, "o")).getMessage(),
                        product);
                assertEquals(hashTmp, assertThrows(IllegalArgumentException.class, () -> mysql.selectFrom(HashTempTableEntity.class, "o")).getMessage(),
                        product);
                assertEquals(hashTmp, assertThrows(IllegalArgumentException.class, () -> mysql.selectCountFrom(HashTempTableEntity.class)).getMessage(),
                        product);
                assertEquals(hashTmp,
                        assertThrows(IllegalArgumentException.class, () -> mysql.select("id").from("t").join(HashTempTableEntity.class, "o")).getMessage(),
                        product);
                assertEquals(hashTmp,
                        assertThrows(IllegalArgumentException.class, () -> mysql.select("id").from("t").leftJoin(HashTempTableEntity.class)).getMessage(),
                        product);
                assertEquals("tableName must not end inside a line comment: dbo.#tmp",
                        assertThrows(IllegalArgumentException.class, () -> mysql.select("id").from(QualifiedHashTableEntity.class, "o")).getMessage(), product);

                final SqlBuilder mysqlFrom = mysql.select("id");
                assertThrows(IllegalArgumentException.class, () -> mysqlFrom.from(HashTempTableEntity.class, "o"));
                assertEquals("SELECT id FROM t WHERE id = ?", mysqlFrom.from("t").where(idEq).build().query(), product);
                final SqlBuilder mysqlJoin = mysql.select("id").from("t");
                assertThrows(IllegalArgumentException.class, () -> mysqlJoin.join(HashTempTableEntity.class, "o"));
                assertEquals("SELECT id FROM t WHERE id = ?", mysqlJoin.where(idEq).build().query(), product);
            }

            // SQL Server and the dialect-agnostic default read "#tmp" as a temporary table: unchanged. The FROM lists composed
            // without a builder read it that way on every dialect, as before.
            for (final Dsl dsl : List.of(PSC, lexDialect(PSC, "Microsoft SQL Server"))) {
                assertEquals("SELECT h.id AS \"id\" FROM #tmp h WHERE h.id = ?", dsl.select("id").from(HashTempTableEntity.class).where(idEq).build().query());
                assertEquals("SELECT id FROM t JOIN #tmp o ON t.id = o.id",
                        dsl.select("id").from("t").join(HashTempTableEntity.class, "o").on("t.id = o.id").build().query());
                assertEquals("SELECT o.id AS \"id\", c.id AS \"id\", c.name AS \"name\" FROM #tmp o, aliased_customer c",
                        dsl.selectFrom(List.of(Selection.builder(HashTempTableEntity.class).tableAlias("o").build(),
                                Selection.builder(AliasedCustomer.class).tableAlias("c").build())).build().query());
            }

            // A real trailing comment after a temporary-table name is rejected on every path and dialect (MySQL read the FROM list
            // "#tmp -- x", line feed, "o, aliased_customer c" as "FROM o, aliased_customer c").
            final String hashTmpNote = "tableName must not end inside a line comment: #tmp -- x";
            for (final Dsl dsl : List.of(PSC, lexDialect(PSC, "MySQL"), lexDialect(PSC, "Microsoft SQL Server"))) {
                assertEquals(hashTmpNote,
                        assertThrows(IllegalArgumentException.class,
                                () -> dsl.selectFrom(List.of(Selection.builder(HashTmpNoteTableEntity.class).tableAlias("o").build(),
                                        Selection.builder(AliasedCustomer.class).tableAlias("c").build()))).getMessage());
                assertEquals(hashTmpNote,
                        assertThrows(IllegalArgumentException.class, () -> dsl.select("id").from(HashTmpNoteTableEntity.class, "o")).getMessage());
            }

            // MySQL and SQLite end a line comment only at a line feed, so "-- x", CR, " t" is a comment to its end there.
            final String loneCr = "tableName must not end inside a line comment: -- x\r t";
            assertEquals(loneCr,
                    assertThrows(IllegalArgumentException.class, () -> lexDialect(PSC, "SQLite").select("id").from(LoneCarriageReturnTableEntity.class, "o"))
                            .getMessage());
            assertEquals(loneCr, assertThrows(IllegalArgumentException.class,
                    () -> lexDialect(PSC, "MySQL").select("id").from("t").join(LoneCarriageReturnTableEntity.class, "o")).getMessage());
            assertEquals(loneCr,
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from(LoneCarriageReturnTableEntity.class, "o")).getMessage());
            assertEquals(loneCr,
                    assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder
                            .getFromClause(List.of(Selection.builder(LoneCarriageReturnTableEntity.class).tableAlias("o").build()), NamingPolicy.SNAKE_CASE))
                                    .getMessage());

            // A qualifier followed only by a comment would take the alias as the table name ("main.o").
            final String qualifiedDash = "tableName must not end inside a line comment: main.-- x";
            assertEquals(qualifiedDash,
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from(QualifiedDashTableEntity.class, "o")).getMessage());
            assertEquals(qualifiedDash,
                    assertThrows(IllegalArgumentException.class, () -> lexDialect(PSC, "SQLite").select("id").from(QualifiedDashTableEntity.class, "o"))
                            .getMessage());
            assertEquals(qualifiedDash, assertThrows(IllegalArgumentException.class,
                    () -> PSC.selectFrom(List.of(Selection.builder(QualifiedDashTableEntity.class).tableAlias("o").build()))).getMessage());

            // H2 also reads "//" as a line comment: its builder paths reject such a name, other dialects' builder paths keep it as
            // written, and the FROM lists composed without a builder reject it on every dialect.
            final Dsl h2 = lexDialect(PSC, "H2");
            assertEquals("tableName must not end inside a line comment: slash_comment_table // x",
                    assertThrows(IllegalArgumentException.class, () -> h2.select("id").from(SlashCommentTableEntity.class, "o")).getMessage());
            assertEquals("tableName must not end inside a line comment: // x",
                    assertThrows(IllegalArgumentException.class, () -> h2.select("id").from("t").join(SlashCommentOnlyTableEntity.class, "o")).getMessage());
            assertEquals("SELECT o.id AS \"id\" FROM slash_comment_table // x o WHERE o.id = ?",
                    lexDialect(PSC, "PostgreSQL").select("id").from(SlashCommentTableEntity.class, "o").where(idEq).build().query());
            assertEquals("tableName must not end inside a line comment: slash_comment_table // x",
                    assertThrows(IllegalArgumentException.class,
                            () -> lexDialect(PSC, "PostgreSQL").selectFrom(List.of(Selection.builder(SlashCommentTableEntity.class).tableAlias("o").build())))
                                    .getMessage());

            // Ordinary names are unchanged.
            assertEquals("aliased_orders o, aliased_customer c", AbstractQueryBuilder.getFromClause(
                    List.of(Selection.builder(AliasedOrder.class).tableAlias("o").build(), Selection.builder(AliasedCustomer.class).tableAlias("c").build()),
                    NamingPolicy.SNAKE_CASE));
            assertEquals("SELECT o.id AS \"id\" FROM aliased_orders o WHERE o.id = 1",
                    SCSB.select("id").from(AliasedOrder.class, "o").where(Filters.eq("o.id", 1)).build().query());
            assertEquals("SELECT id FROM t JOIN aliased_orders o ON t.id = o.id",
                    SCSB.select("id").from("t").join(AliasedOrder.class, "o").on("t.id = o.id").build().query());
        }

        @Test
        void testFromClausePreservesEmptyListsAndDefaultNaming() {
            assertEquals("", AbstractQueryBuilder.getFromClause(Collections.emptyList(), null));
            final List<Selection> selections = List.of(Selection.builder(Account.class).build());
            assertEquals(AbstractQueryBuilder.getFromClause(selections, NamingPolicy.SNAKE_CASE), AbstractQueryBuilder.getFromClause(selections, null));
        }

        @Test
        void testFromClauseRejectsNullSelectionsBeforeLookingUpEntityMetadata() {
            final Selection invalidEntity = Selection.builder(String.class).build();
            final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> AbstractQueryBuilder.getFromClause(Arrays.asList(invalidEntity, null), NamingPolicy.SNAKE_CASE));
            assertTrue(exception.getMessage().contains("selection"));
            assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.getFromClause(null, NamingPolicy.SNAKE_CASE));
            assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.getFromClause(Collections.singletonList(null), NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testMissingJoinStatePrecedesConnectorArgumentsAndLeavesQueryReusable() {
            final SqlBuilder builder = Dsl.PSC.select("id").from("account");
            assertThrows(IllegalStateException.class, () -> builder.on((String) null));
            assertThrows(IllegalStateException.class, () -> builder.using((String[]) null));
            assertEquals("SELECT id FROM account", builder.build().query());
        }

        @Test
        void testNullCallbackDoesNotConsumeAnOpenBuilder() {
            final SqlBuilder builder = Dsl.PSC.select("id").from("account");
            assertThrows(IllegalArgumentException.class, () -> builder.apply((Throwables.Function<SP, Object, RuntimeException>) null));
            assertEquals("SELECT id FROM account", builder.build().query());
        }

        @Test
        void testNullSnapshotIsRejectedWithoutChangingTheParent() {
            final SqlBuilder parent = Dsl.PSC.select("id").from("account");
            assertThrows(IllegalArgumentException.class, () -> parent.appendSubQuerySnapshot(null));
            assertEquals("SELECT id FROM account", parent.build().query());
            assertThrows(IllegalStateException.class, () -> parent.appendSubQuerySnapshot(null));
        }

        @Test
        void testOptionalNullsRetainTheirMeaning() {
            final SqlBuilder builder = Dsl.PSC.select("id").selectModifier(null).from("account", (Class<?>) null);
            builder.appendIf(false, (Condition) null);
            assertEquals("SELECT id FROM account WHERE id IS NULL", builder.where(Filters.eq("id", null)).build().query());
            assertNull(AbstractQueryBuilder.normalizeColumnName(null, NamingPolicy.SNAKE_CASE));
            assertNull(AbstractQueryBuilder.sanitizeNamedParameterName(null));
            assertEquals("", AbstractQueryBuilder.tableAlias((Class<?>) null));
        }

        @Test
        void testUnselectedConditionalArgumentsRemainLazy() {
            final SqlBuilder builder = Dsl.PSC.select("id").from("account");
            builder.appendIf(false, (Condition) null).appendIf(false, (String) null);
            builder.appendIfOrElse(true, "WHERE id = 1", (String) null);
            assertEquals("SELECT id FROM account WHERE id = 1", builder.build().query());
        }

        // Regression: an unterminated "/*" in table text, a select modifier, a derived-table alias or an appended fragment
        // commented out every later clause (SQLite reads it to the end of input, silently dropping the WHERE). Each entry
        // point now rejects it, as join(String) does.
        @Test
        void testUnterminatedBlockCommentInRawTextIsRejected() {
            final Condition idEq = Filters.eq("id", 1);

            assertEquals("expr must not contain an unterminated block comment: t /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("t /* x")).getMessage());
            assertEquals("expr must not contain an unterminated block comment: a JOIN t ON a.id = t.id /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a JOIN t ON a.id = t.id /* x")).getMessage());
            assertEquals("tableNames[1] must not contain an unterminated block comment: t /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("a", "t /* x")).getMessage());
            // Each table reference is checked on its own, so a comment cannot span two of them.
            assertEquals("tableNames[0] must not contain an unterminated block comment: t /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(List.of("t /* x", "y */ u"))).getMessage());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("t /* x", Account.class));
            assertThrows(IllegalArgumentException.class, () -> PSC.selectCountFrom("t /* x"));
            assertEquals("tableName must not contain an unterminated block comment: t /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.update("t /* x").set("a", 1)).getMessage());
            assertEquals("tableName must not contain an unterminated block comment: t /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.deleteFrom("t /* x").where(idEq)).getMessage());
            assertThrows(IllegalArgumentException.class, () -> PSC.deleteFrom("t /* x").build());
            assertEquals("tableName must not contain an unterminated block comment: t /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.insert("a").into("t /* x")).getMessage());
            assertThrows(IllegalArgumentException.class, () -> PSC.insert("a").into("t /* x", Account.class));
            assertEquals("tableName must not contain an unterminated block comment: t2 /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("a").into("t2 /* x")).getMessage());
            // An entity's @Table name is not seen by Dsl's eager table-name check: the backstop at UPDATE/DELETE statement
            // initialization rejects it.
            final String openEntityTable = "tableName must not contain an unterminated block comment: open_comment_table /* x";
            assertEquals(openEntityTable,
                    assertThrows(IllegalArgumentException.class, () -> PSC.deleteFrom(OpenCommentTableEntity.class).where(idEq)).getMessage());
            assertEquals(openEntityTable,
                    assertThrows(IllegalArgumentException.class, () -> PSC.deleteFrom(OpenCommentTableEntity.class).build()).getMessage());
            assertEquals(openEntityTable,
                    assertThrows(IllegalArgumentException.class, () -> PSC.update(OpenCommentTableEntity.class).where(idEq)).getMessage());
            // The entity join overloads emit the same @Table names: an open block comment is rejected before anything is
            // written, and so is a trailing line comment, which would swallow the alias or the clauses after the join.
            assertEquals(openEntityTable,
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").join(OpenCommentTableEntity.class)).getMessage());
            assertEquals(openEntityTable,
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").leftJoin(OpenCommentTableEntity.class, "o")).getMessage());
            assertEquals(openEntityTable,
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").crossJoin(OpenCommentTableEntity.class)).getMessage());
            assertEquals(openEntityTable,
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").naturalJoin(OpenCommentTableEntity.class, "o")).getMessage());
            final SqlBuilder rejectedJoin = PSC.select("id").from("t");
            assertThrows(IllegalArgumentException.class, () -> rejectedJoin.innerJoin(OpenCommentTableEntity.class, "o"));
            assertEquals("SELECT id FROM t", rejectedJoin.build().query());
            final String lineEntityTable = "tableName must not end inside a line comment: line_comment_table -- x";
            assertEquals(lineEntityTable,
                    assertThrows(IllegalArgumentException.class, () -> SCSB.select("id").from("t").crossJoin(LineCommentTableEntity.class, "o")).getMessage());
            assertEquals(lineEntityTable,
                    assertThrows(IllegalArgumentException.class, () -> SCSB.select("id").from("t").join(LineCommentTableEntity.class, "o")).getMessage());
            assertEquals("selectModifier must not contain an unterminated block comment: TOP 1 /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("id").selectModifier("TOP 1 /* x")).getMessage());
            assertEquals("alias must not contain an unterminated block comment: s /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from(PSC.select("id").from("t"), "s /* x")).getMessage());
            assertEquals("expr must not contain an unterminated block comment: /* x",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").append("/* x")).getMessage());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").appendIf(true, "/* x"));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").appendIfOrElse(false, "/* x */", "/* x"));

            // An input that is also invalid elsewhere keeps the earlier message (the staged columns were supplied first).
            assertEquals("SQL comment token is not allowed in SQL expression: a -- b",
                    assertThrows(IllegalArgumentException.class, () -> PSC.select("a -- b").from("t /* x")).getMessage());
            assertEquals("SQL comment token is not allowed in column expression: a -- b",
                    assertThrows(IllegalArgumentException.class, () -> PSC.insert("a -- b").into("t /* x")).getMessage());

            // A rejection leaves the builder (and a derived-table child) reusable.
            final SqlBuilder select = PSC.select("id");
            assertThrows(IllegalArgumentException.class, () -> select.from("t /* x"));
            assertEquals("SELECT id FROM t WHERE id = ?", select.from("t").where(idEq).build().query());
            final SqlBuilder insert = PSC.insert("a");
            assertThrows(IllegalArgumentException.class, () -> insert.into("t /* x"));
            assertEquals("INSERT INTO t (a) VALUES (?)", insert.into("t").build().query());
            final SqlBuilder child = PSC.select("id").from("t");
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from(child, "s /* x"));
            assertEquals("SELECT id FROM t", child.build().query());
            // A rejected append(String) also undoes the statement prefix it rendered first: after update(Class) that prefix
            // includes the staged SET list, which a following set(...) would not render.
            final SqlBuilder update = PSC.update(Account.class);
            assertThrows(IllegalArgumentException.class, () -> update.append("/* x"));
            assertEquals("", update._sb.toString());
            assertEquals(PSC.update(Account.class).set("firstName", "x").where(idEq).build().query(), update.set("firstName", "x").where(idEq).build().query());
            final SqlBuilder delete = PSC.deleteFrom("t");
            assertThrows(IllegalArgumentException.class, () -> delete.append("/* x"));
            assertEquals("", delete._sb.toString());
            assertEquals("DELETE FROM t WHERE id = ?", delete.where(idEq).build().query());
            // A statement init rejects keeps init's message.
            assertEquals("set() must be called to specify the columns to update before building an UPDATE statement",
                    assertThrows(IllegalStateException.class, () -> PSC.update("t").append("/* x")).getMessage());
            assertThrows(IllegalStateException.class, () -> PSC.select("id").append("/* x"));

            // Terminated comments, a quoted "/*" and a "/*" inside a line comment are accepted unchanged.
            assertEquals("SELECT * FROM t /* x */ u WHERE id = ?", PSC.select("*").from("t /* x */ u").where(idEq).build().query());
            assertEquals("SELECT * FROM t /* x */, u WHERE id = ?", PSC.select("*").from(List.of("t /* x */", "u")).where(idEq).build().query());
            assertEquals("SELECT * FROM \"t/*\" WHERE id = ?", PSC.select("*").from("\"t/*\"").where(idEq).build().query());
            assertEquals("SELECT * FROM t -- /* x\n WHERE id = ?", PSC.select("*").from("t -- /* x").where(idEq).build().query());
            assertEquals("SELECT count(*) FROM t /* x */ WHERE id = ?", PSC.selectCountFrom("t /* x */").where(idEq).build().query());
            assertEquals("UPDATE t /* x */ SET a = ? WHERE id = ?", PSC.update("t /* x */").set("a", 1).where(idEq).build().query());
            assertEquals("DELETE FROM t /* x */ WHERE id = ?", PSC.deleteFrom("t /* x */").where(idEq).build().query());
            assertEquals("INSERT INTO t /* x */ (a) VALUES (?)", PSC.insert("a").into("t /* x */").build().query());
            assertEquals("SELECT TOP 1 /* x */ id FROM t", PSC.select("id").selectModifier("TOP 1 /* x */").from("t").build().query());
            assertEquals("SELECT id FROM (SELECT id FROM t) s /* x */ WHERE id = ?",
                    PSC.select("id").from(PSC.select("id").from("t"), "s /* x */").where(idEq).build().query());
            assertEquals("SELECT id FROM t /* x */ WHERE id = ?", PSC.select("id").from("t").append("/* x */").where(idEq).build().query());
        }

        private static void assertClosedFirst(final Consumer<SqlBuilder> operation) {
            final SqlBuilder builder = Dsl.PSC.select("id").from("account");
            builder.build();
            final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> operation.accept(builder));
            assertTrue(exception.getMessage().contains("closed"));
        }
    }

    @Nested
    class CoreSqlScanning {

        @Nested
        class AliasAndFromScanning {
            @Test
            void testAliasDetectionRetainsLastTemporalAsAndTrailingTrivia() {
                final String from = "t FOR SYSTEM_TIME AS OF '2026-01-01' AS finalAlias /* note */";
                final SqlBuilder builder = PSB.select("*").from(from);

                assertEquals("finalAlias", builder._tableAlias);
                assertSql("SELECT * FROM " + from, List.of(), builder.build());
            }

            @Test
            void testAliasScanningRetainsLongExpressionsAndExplicitAlias() {
                final String expression = "a" + " + a".repeat(1024);

                assertSql("SELECT " + expression + " FROM t", List.of(), PSB.select(expression).from("t").build());
                assertSql("SELECT " + expression + " AS total FROM t", List.of(), PSB.select(expression + " AS total").from("t").build());
            }

            @Test
            void testFromJoinLookaheadCachesOnlyActuallyVisitedOffsets() {
                // MySQL exposes --x as text, while the modifier lookahead's historical trivia rule skips it.
                // An offset interval cache would incorrectly hide the inner GLOBAL before its visible JOIN.
                final String from = "t GLOBAL --x GLOBAL JOIN other o ON t.id = o.id\n GLOBAL alias";
                final SqlBuilder builder = dialect("MySQL").select("*").from(from);

                assertNull(builder._tableAlias);
                assertSql("SELECT * FROM " + from, List.of(), builder.build());
            }

            @Test
            void testFromJoinLookaheadHandlesConvergingCommentPaths() {
                final String from = "t " + "GLOBAL --x GLOBAL GLOBAL\n ".repeat(128) + "GLOBAL alias";
                final SqlBuilder builder = dialect("MySQL").select("*").from(from);

                assertEquals("alias", builder._tableAlias);
                assertSql("SELECT * FROM " + from, List.of(), builder.build());
            }

            @Test
            void testFromJoinLookaheadHandlesLongModifierSequenceWithoutJoin() {
                // This raw text is deliberately not executable SQL; its accepted rendering must stay bounded.
                final String from = "t " + "GLOBAL ".repeat(4096) + "alias";
                final SqlBuilder builder = PSB.select("*").from(from);

                assertEquals("alias", builder._tableAlias);
                assertSql("SELECT * FROM " + from, List.of(), builder.build());
            }

            @Test
            void testFromJoinLookaheadKeepsModifierAliasesAndActualJoinBoundary() {
                for (final String alias : List.of("local", "semi", "anti")) {
                    final SqlBuilder builder = PSB.select("*").from("t " + alias);
                    assertEquals(alias, builder._tableAlias);
                    assertSql("SELECT * FROM t " + alias, List.of(), builder.build());
                }

                final String from = "t a GLOBAL ANY LEFT JOIN u ON a.id = u.id";
                final SqlBuilder builder = PSB.select("*").from(from);
                assertEquals("a", builder._tableAlias);
                assertSql("SELECT * FROM " + from, List.of(), builder.build());
            }
        }

        @Nested
        class FromCollections {
            @Test
            void testFromCollectionKeepsCallerValuesAndSingleSnapshot() {
                final List<String> caller = new ArrayList<>(List.of("  t a  ", " u b\n"));
                final SingleIterationCollection tables = new SingleIterationCollection(caller);

                assertSql("SELECT * FROM t a, u b WHERE a.id = ?", List.of(7), PSB.select("*").from(tables).where(Filters.eq("a.id", 7)).build());
                assertEquals(List.of("  t a  ", " u b\n"), caller);
                assertEquals(1, tables.iterations);
            }

            @Test
            void testFromCollectionKeepsIndexedTrimFailureAndRetry() {
                final SqlBuilder builder = PSB.select("*");
                final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> builder.from(List.of("t", "\u0000")));

                assertTrue(error.getMessage().contains("tableNames[1]"), error.getMessage());
                assertSql("SELECT * FROM t, u", List.of(), builder.from(List.of("t", "u")).build());
            }

            @Test
            void testFromCollectionKeepsSingleElementOverrideDispatch() {
                final TrackingFromBuilder builder = new TrackingFromBuilder();

                assertSql("SELECT * FROM t", List.of(), builder.from(List.of("  t  ")).build());
                assertEquals(1, builder.fromStringCalls);
                assertEquals("t", builder.fromStringArgument);
            }

            @Test
            void testFromCollectionKeepsUnterminatedCommentErrorAndRetry() {
                final SqlBuilder builder = PSB.select("*");
                final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> builder.from(List.of(" t ", " u /* open ")));

                assertEquals("tableNames[1] must not contain an unterminated block comment: u /* open", error.getMessage());
                assertSql("SELECT * FROM t, u WHERE id = ?", List.of(8), builder.from(List.of("t", "u")).where(Filters.eq("id", 8)).build());
            }

            @Test
            void testFromCollectionRestoresCommentBoundaryForDialectMarkers() {
                for (final String[] fixture : List.of(new String[] { "MySQL", "# note" }, new String[] { "H2", "// note" },
                        new String[] { "PostgreSQL", "-- note" })) {
                    assertSql("SELECT * FROM t a " + fixture[1] + "\n, u b WHERE a.id = ?", List.of(9),
                            dialect(fixture[0]).select("*").from(List.of("t a " + fixture[1] + "\n", "u b")).where(Filters.eq("a.id", 9)).build());
                }
            }
        }

        @Nested
        class QueryBoundaries {
            @Test
            void testJoinConnectorKeepsLoneCarriageReturnHintAndRetry() {
                final SqlBuilder builder = dialect("MySQL").select("*").from("t").join("u -- x\r ON u.id = t.id");
                final IllegalStateException error = assertThrows(IllegalStateException.class, () -> builder.where(Filters.eq("id", 1)));

                assertTrue(error.getMessage().contains("lone carriage return"), error.getMessage());
                assertSql("SELECT * FROM t JOIN u -- x\r ON u.id = t.id\n ON t.id = u.id WHERE t.id = ?", List.of(2),
                        builder.on("t.id = u.id").where(Filters.eq("t.id", 2)).build());
            }

            @Test
            void testJoinConnectorSeparateAndInlineAgreeForDenseTempTables() {
                final String tables = IntStream.range(0, 128).mapToObj(index -> "#t" + index).collect(Collectors.joining(", "));
                final String joined = "(SELECT * FROM " + tables + ") j";
                final SP inline = PSB.select("*").from("t").join(joined + " ON t.id = j.id").where(Filters.eq("t.id", 7)).build();
                final SP separate = PSB.select("*").from("t").join(joined).on("t.id = j.id").where(Filters.eq("t.id", 7)).build();

                assertSql(inline.query(), inline.parameters(), separate);
            }

            @Test
            void testSetOperandKeepsQuotedSemicolonsAndRejectsRealTerminator() {
                final SqlBuilder builder = PSB.select("id").from("t");
                assertThrows(IllegalArgumentException.class, () -> builder.union("SELECT id FROM u;"));
                assertSql("SELECT id FROM t UNION SELECT ';' FROM u /* ; */", List.of(), builder.union("SELECT ';' FROM u /* ; */").build());
            }

            @Test
            void testSetOperandWithoutMarkersKeepsLongQueryAndBindings() {
                final String columns = IntStream.range(0, 256).mapToObj(index -> "id" + index).collect(Collectors.joining(", "));
                final String operand = "SELECT " + columns + " FROM u";

                assertSql("SELECT id FROM t WHERE id = ? UNION " + operand, List.of(3),
                        PSB.select("id").from("t").where(Filters.eq("id", 3)).union(operand).build());
            }
        }

        private static Dsl dialect(final String product) {
            return Dsl.forDialect(PSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of(product)).build());
        }

        private static final class SingleIterationCollection extends AbstractCollection<String> {
            private final List<String> values;
            private int iterations;

            private SingleIterationCollection(final List<String> values) {
                this.values = values;
            }

            @Override
            public Iterator<String> iterator() {
                if (++iterations != 1) {
                    throw new AssertionError("FROM must use its validated snapshot");
                }
                return values.iterator();
            }

            @Override
            public int size() {
                return values.size();
            }
        }

        private static final class TrackingFromBuilder extends SqlBuilder {
            private int fromStringCalls;
            private String fromStringArgument;

            private TrackingFromBuilder() {
                super(PSB.sqlDialect());
                _op = com.landawn.abacus.util.OperationType.QUERY;
                _propOrColumnNames = List.of("*");
            }

            @Override
            public SqlBuilder from(final String expression) {
                fromStringCalls++;
                fromStringArgument = expression;
                return super.from(expression);
            }
        }
    }

    @Nested
    class IncrementalRendering {
        @Test
        void testCustomDuplicateTokensRetainExistingRewriteOrder() {
            final Dsl custom = Dsl.forDialect(
                    NSB.sqlDialect().toBuilder().namingPolicy(NamingPolicy.NO_CHANGE).namedParameterHandler((sql, name) -> sql.append("@constant")).build());
            final SubQuery snapshot = custom.select("id")
                    .from("s")
                    .where(Filters.and(Filters.eq("p", 1), Filters.eq("Aa", 2), Filters.eq("BB", 3)))
                    .toSubQuery();

            // The handler deliberately aliases distinct names. Preserve the established winner in
            // this accepted edge case, including String-hash collisions and a parent suffix shift.
            assertSql("SELECT id FROM t WHERE (p = :p) AND (EXISTS (SELECT id FROM s WHERE (p = :p_2) AND (Aa = :p_2) AND (BB = :p_2)))", List.of(0, 1, 2, 3),
                    NSB.select("id").from("t").where(Filters.and(Filters.eq("p", 0), Filters.exists(snapshot))).build());
        }

        @Test
        void testCustomHashTokensKeepUnchangedDataBeforeRenamedTokens() {
            final Dsl hash = Dsl.forDialect(NSB.sqlDialect()
                    .toBuilder()
                    .productInfo(SqlDialect.ProductInfo.of("PostgreSQL"))
                    .namedParameterHandler((sb, name) -> sb.append('#').append(name))
                    .build());
            final SqlBuilder child = hash.select("id")
                    .from("archive")
                    .where(Filters.and(Filters.eq("p", 1), Filters.eq("q", 2), Filters.eq("r", 3), Filters.eq("s", 4), Filters.eq("id", 5)));
            assertSql(
                    "SELECT id FROM records WHERE id = #id UNION SELECT id FROM archive WHERE (p = #p) AND (q = #q) AND (r = #r)"
                            + " AND (s = #s) AND (id = #id_2)\n",
                    List.of(0, 1, 2, 3, 4, 5), hash.select("id").from("records").where(Filters.eq("id", 0)).union(child).build());
        }

        @Test
        void testCustomTokensWithLongOverlappingPrefixesPreserveQuotedTextAndSnapshot() {
            final String prefix = "@" + "shared".repeat(342);
            final Dsl custom = Dsl.forDialect(NSB.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append(prefix).append(name)).build());
            final List<Condition> conditions = new ArrayList<>();
            final List<Object> parameters = new ArrayList<>();
            final List<String> expectedPredicates = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                final String name = "p".repeat(i);
                conditions.add(Filters.eq(name, i));
                parameters.add(i);
                expectedPredicates.add("(" + name + " = :" + name + ")");
            }
            final String quoted = "note = '" + prefix + "p'";
            conditions.add(Filters.expr(quoted));
            expectedPredicates.add("(" + quoted + ")");
            final SubQuery snapshot = custom.select("id").from("archive").where(Filters.and(conditions)).toSubQuery();
            final String originalSql = snapshot.rawSql();
            final String expected = "SELECT id FROM records WHERE EXISTS (SELECT id FROM archive WHERE " + String.join(" AND ", expectedPredicates) + ")";
            for (int i = 0; i < 2; i++) {
                assertSql(expected, parameters, NSB.select("id").from("records").where(Filters.exists(snapshot)).build());
                assertEquals(originalSql, snapshot.rawSql());
                assertEquals(parameters, snapshot.parameters());
            }
        }

        @Test
        void testDeepStructuredNamedChildrenPreserveAncestorSuffixAndHashCollisions() {
            // Aa and BB have the same String hash; literal id_2 also occupies a generated suffix.
            assertEquals("Aa".hashCode(), "BB".hashCode());
            final SubQuery leaf = new SubQuery("leaf", List.of("id"),
                    Filters.where(Filters.and(Filters.eq("id", 2), Filters.eq("id_2", 21), Filters.eq("BB", 13))));
            final SubQuery middle = new SubQuery("middle", List.of("id"),
                    Filters.where(Filters.and(Filters.eq("id", 1), Filters.eq("Aa", 12), Filters.in("id", leaf))));
            final Condition condition = Filters.and(Filters.eq("id_2", 20), Filters.eq("Aa", 10), Filters.eq("BB", 11), Filters.in("id", middle),
                    Filters.eq("id", 3), Filters.eq("Aa", 14), Filters.eq("BB", 15));
            final String leafSql = "SELECT id FROM leaf WHERE (id = :id_3) AND (id_2 = :id_2_2) AND (BB = :BB_2)";
            final String middleSql = "SELECT id FROM middle WHERE (id = :id) AND (Aa = :Aa_2) AND (id IN (" + leafSql + "))";
            final String expected = "SELECT id FROM records WHERE (id_2 = :id_2) AND (Aa = :Aa) AND (BB = :BB) AND (id IN (" + middleSql
                    + ")) AND (id = :id_4) AND (Aa = :Aa_3) AND (BB = :BB_3)";
            for (int i = 0; i < 2; i++) {
                assertSql(expected, List.of(20, 10, 11, 1, 12, 2, 21, 13, 3, 14, 15), NSB.select("id").from("records").where(condition).build());
            }
        }

        @Test
        void testNamedChildSuccessThenFailureRestoresParentHistory() {
            final SubQuery leaf = new SubQuery("archive", List.of("id"), Filters.where(Filters.eq("id", 1)));
            final SubQuery middle = new SubQuery("history", List.of("id"), Filters.where(Filters.and(Filters.eq("id_2", 2), Filters.in("id", leaf))));
            final SqlBuilder builder = NSB.update("records").set("id", 0);
            assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.and(Filters.in("id", middle), Filters.eq("bad -- column", 3))));
            assertEquals(List.of(0), builder.parameters());
            assertSql("UPDATE records SET id = :id WHERE (id = :id_2) AND (id_2 = :id_2_2)", List.of(0, 9, 10),
                    builder.where(Filters.and(Filters.eq("id", 9), Filters.eq("id_2", 10))).build());
        }

        @Test
        void testParameterSnapshotsCopyWrappedSubListsAndReversedViews() {
            final Object value = new byte[] { 1, 2 };
            final List<Object> source = new ArrayList<>(Arrays.asList("before", null, value, "after"));
            final ImmutableList<Object> view = ImmutableList.wrap(source).subList(1, 3).reversed();
            final SP result = new SP("SELECT ?, ?", view);
            source.clear();
            assertSql("SELECT ?, ?", Arrays.asList(value, null), result);
            assertSame(value, result.parameters().get(0));
        }

        @Test
        void testSetExcludedMapFailureRestoresEarlierAssignments() {
            final Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("id", 1);
            properties.put("skip", 2);
            properties.put("bad -- column", 3);
            final SqlBuilder builder = NSB.update("records").set("id", 0);
            assertThrows(IllegalArgumentException.class, () -> builder.set((Object) properties, Set.of("skip")));
            assertSql("UPDATE records SET id = :id, id = :id_2", List.of(0, 4), builder.set("id", 4).build());
        }

        @Test
        void testSetExcludedMapSkipsExcludedInvalidKeyAndKeepsOrder() {
            final Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("first", 1);
            properties.put(" ", 2);
            properties.put("second", null);
            final SqlBuilder builder = NSB.update("records").set((Object) properties, Set.of(" "));
            properties.clear();
            assertSql("UPDATE records SET first = :first, second = :second, third = :third", Arrays.asList(1, null, 3), builder.set("third", 3).build());
        }

        @Test
        void testUnterminatedBracketRunsStillExposeLateCommentsAndAllowRetry() {
            final String openers = "[".repeat(1024) + "x";
            for (final Dsl dialect : new Dsl[] { PSB, lexDialect(PSB, "SQL Server"), lexDialect(PSB, "SQLite") }) {
                final SqlBuilder builder = dialect.select("id").from("records");
                assertThrows(IllegalArgumentException.class, () -> builder.orderBy(openers + " -- late"));
                assertThrows(IllegalArgumentException.class, () -> builder.orderBy(openers + " /* late */"));
                assertSql("SELECT id FROM records ORDER BY id", List.of(), builder.orderBy("id").build());
                final String closed = openers + " -- quoted]";
                assertSql("SELECT id FROM records ORDER BY " + closed, List.of(), dialect.select("id").from("records").orderBy(closed).build());
            }
        }
    }

    @Nested
    class AccumulatedClauseRendering {
        @Test
        @SuppressWarnings("deprecation")
        void testAliasOnlyStateRestoresMappingAfterLargeClauseFailure() {
            final List<String> values = IntStream.range(0, 128).mapToObj(i -> "v" + i).toList();
            final Condition membership = Filters.in("c.value", values);
            for (final Dsl dsl : new Dsl[] { NSB, MSC }) {
                final SqlBuilder builder = dsl.select("*").from(ReviewChild.class, "c");
                assertEquals(List.of(), builder.parameters());
                assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.and(membership, Filters.eq("bad -- column", 1))));
                assertEquals(List.of(), builder.parameters());

                final List<String> markers = IntStream.rangeClosed(1, values.size()).mapToObj(i -> parameterToken(dsl, "value" + i)).toList();
                final List<Object> bindings = new ArrayList<>(values);
                bindings.add("done");
                assertSql("SELECT * FROM review_child c WHERE (c.stored_value IN (" + String.join(", ", markers) + ")) AND (c.stored_value = "
                        + parameterToken(dsl, "value") + ")", bindings, builder.where(Filters.and(membership, Filters.eq("c.value", "done"))).build());
            }
        }

        @Test
        void testCustomTokenManyRootsPreserveCommentsUnicodeAndSnapshot() {
            final String[] prefixes = { "/*", "@", "\u00e9", "\u03a9", "\u6c49", "\u0416", "\u0100", "\u05d0", "\u0e01", "\uD801\uDC00" };
            for (final int count : new int[] { 8, 9, 10 }) {
                final Map<String, String> tokens = new LinkedHashMap<>();
                final List<Condition> conditions = new ArrayList<>();
                final List<Object> values = new ArrayList<>();
                final List<String> rendered = new ArrayList<>();
                final List<String> rewritten = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    final String name = "p" + i;
                    final String token = prefixes[i] + name + (i == 0 ? "*/" : "");
                    tokens.put(name, token);
                    conditions.add(Filters.eq(name, i + 1));
                    values.add(i + 1);
                    rendered.add("(" + name + " = " + token + ")");
                    rewritten.add("(" + name + " = :" + name + (i == 0 ? "_2" : "") + ")");
                }
                final Dsl custom = Dsl.forDialect(NSB.sqlDialect().toBuilder().namedParameterHandler((sql, name) -> sql.append(tokens.get(name))).build());
                // The first custom token looks like a comment. It remains data, while its quoted copy stays literal text.
                // U+0429 and the U+00E9/U+03A9 token roots also share their low six bits.
                final String tail = " AND (note = '/*p0*/') AND (" + "\u6f22\u5b57".repeat(128) + " = 1) AND (\u0429p2 = 3)";
                final SubQuery snapshot = custom.select("id").from("archive").where(Filters.and(conditions)).append(tail).toSubQuery();
                final String originalSql = "SELECT id FROM archive WHERE " + String.join(" AND ", rendered) + tail;
                final String rewrittenSql = "SELECT id FROM archive WHERE " + String.join(" AND ", rewritten) + tail;
                final List<Object> bindings = new ArrayList<>();
                bindings.add(0);
                bindings.addAll(values);
                assertEquals(originalSql, snapshot.rawSql());
                for (int reuse = 0; reuse < 2; reuse++) {
                    assertSql("SELECT id FROM records WHERE (p0 = :p0) AND (EXISTS (" + rewrittenSql + "))", bindings,
                            NSB.select("id").from("records").where(Filters.and(Filters.eq("p0", 0), Filters.exists(snapshot))).build());
                    assertEquals(originalSql, snapshot.rawSql());
                    assertEquals(values, snapshot.parameters());
                }
            }
        }

        @Test
        void testCustomTokenOverlappingRootsPreserveBoundariesAndQuotedText() {
            for (final String prefix : new String[] { "@", "\u00e9@", "\uD801\uDC00@" }) {
                final Dsl custom = Dsl.forDialect(NSB.sqlDialect().toBuilder().namedParameterHandler((sql, name) -> sql.append(prefix).append(name)).build());
                final List<Condition> conditions = new ArrayList<>();
                final List<String> rendered = new ArrayList<>();
                final List<String> rewritten = new ArrayList<>();
                for (int i = 1; i <= 6; i++) {
                    final String name = "p".repeat(i);
                    conditions.add(Filters.eq(name, i));
                    rendered.add("(" + name + " = " + prefix + name + ")");
                    rewritten.add("(" + name + " = :" + name + (i == 1 ? "_2" : "") + ")");
                }
                final String tail = " AND (other = " + prefix + "pppppp_more) AND (note = '" + prefix + "pp') AND (" + "\u6f22\u5b57".repeat(128)
                        + " = 1) AND (\u0429pp = 3) AND (\u0100pp = 4) AND (\u0141pp = 5)" + (prefix.equals("@") ? "" : " AND (leading" + prefix + "p = 2)");
                final SubQuery snapshot = custom.select("id").from("archive").where(Filters.and(conditions)).append(tail).toSubQuery();
                final String originalSql = "SELECT id FROM archive WHERE " + String.join(" AND ", rendered) + tail;
                final String rewrittenSql = "SELECT id FROM archive WHERE " + String.join(" AND ", rewritten) + tail;
                assertEquals(originalSql, snapshot.rawSql());
                for (int reuse = 0; reuse < 2; reuse++) {
                    assertSql("SELECT id FROM records WHERE (p = :p) AND (EXISTS (" + rewrittenSql + "))", List.of(0, 1, 2, 3, 4, 5, 6),
                            NSB.select("id").from("records").where(Filters.and(Filters.eq("p", 0), Filters.exists(snapshot))).build());
                    assertEquals(originalSql, snapshot.rawSql());
                    assertEquals(List.of(1, 2, 3, 4, 5, 6), snapshot.parameters());
                }
            }
        }

        @Test
        @SuppressWarnings("deprecation")
        void testLargeNamedInRestoresSmallAndAccumulatedStateBeforeRetry() {
            final List<Integer> values = IntStream.range(0, 5000).boxed().toList();
            final Condition membership = Filters.in("id", values);
            // Exercise nearby history sizes without tying the behavioral contract to a private optimization cutoff.
            for (final int priorCount : new int[] { 1, 15, 16, 17 }) {
                final Map<String, Object> prior = priorProperties(priorCount, "id1", false);
                for (final Dsl dsl : new Dsl[] { NSB, MSC }) {
                    final SqlBuilder builder = dsl.update("records").set(prior);
                    assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.and(membership, Filters.eq("bad -- column", 1))));
                    assertEquals(new ArrayList<>(prior.values()), builder.parameters());

                    final List<String> markers = IntStream.rangeClosed(1, values.size())
                            .mapToObj(i -> parameterToken(dsl, i == 1 ? "id1_2" : "id" + i))
                            .toList();
                    final List<Object> bindings = new ArrayList<>(prior.values());
                    bindings.addAll(values);
                    bindings.add(9000);
                    assertSql(updatePrefix(dsl, prior) + " WHERE (id IN (" + String.join(", ", markers) + ")) AND (id1 = " + parameterToken(dsl, "id1_3") + ")",
                            bindings, builder.where(Filters.and(membership, Filters.eq("id1", 9000))).build());
                }
            }
        }

        @Test
        @SuppressWarnings("deprecation")
        void testOuterSnapshotRestoresSuccessfulAndFailedNestedClauses() {
            final List<Condition> comparisons = IntStream.range(0, 128).mapToObj(i -> (Condition) Filters.eq("id", i)).toList();
            final Condition disjunction = Filters.or(comparisons);
            for (final int priorCount : new int[] { 16, 17 }) {
                final Map<String, Object> prior = priorProperties(priorCount, "id", false);
                for (final Dsl dsl : new Dsl[] { NSB, MSC }) {
                    final SqlBuilder builder = dsl.update("records").set(prior);
                    assertThrows(IllegalStateException.class, () -> builder.mutateAtomically(() -> {
                        assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.and(disjunction, Filters.eq("bad -- column", 1))));
                        assertEquals(new ArrayList<>(prior.values()), builder.parameters());
                        builder.where(disjunction);
                        throw new IllegalStateException("outer failure after successful child");
                    }));
                    assertEquals(new ArrayList<>(prior.values()), builder.parameters());
                    final List<Object> bindings = new ArrayList<>(prior.values());
                    bindings.add(9000);
                    assertSql(updatePrefix(dsl, prior) + " WHERE id = " + parameterToken(dsl, "id_2"), bindings, builder.where(Filters.eq("id", 9000)).build());
                }
            }
        }

        @Test
        @SuppressWarnings("deprecation")
        void testRepeatedNamedOrRestoresSuffixesBeforeRetry() {
            final List<Condition> comparisons = IntStream.range(0, 2000).mapToObj(i -> (Condition) Filters.eq("id", i)).toList();
            final Condition disjunction = Filters.or(comparisons);
            for (final int priorCount : new int[] { 1, 15, 16, 17 }) {
                final Map<String, Object> prior = priorProperties(priorCount, "id", true);
                final int firstSuffix = priorCount == 1 ? 2 : 3;
                for (final Dsl dsl : new Dsl[] { NSB, MSC }) {
                    final SqlBuilder builder = dsl.update("records").set(prior);
                    assertThrows(IllegalArgumentException.class, () -> builder.where(Filters.and(disjunction, Filters.eq("bad -- column", 1))));
                    assertEquals(new ArrayList<>(prior.values()), builder.parameters());

                    final List<String> predicates = IntStream.range(0, comparisons.size())
                            .mapToObj(i -> "(id = " + parameterToken(dsl, "id_" + (firstSuffix + i)) + ")")
                            .toList();
                    final List<Object> bindings = new ArrayList<>(prior.values());
                    bindings.addAll(IntStream.range(0, comparisons.size()).boxed().toList());
                    bindings.add(9000);
                    assertSql(
                            updatePrefix(dsl, prior) + " WHERE (" + String.join(" OR ", predicates) + ") AND (id = "
                                    + parameterToken(dsl, "id_" + (firstSuffix + comparisons.size())) + ")",
                            bindings, builder.where(Filters.and(disjunction, Filters.eq("id", 9000))).build());
                }
            }
        }

        @SuppressWarnings("deprecation")
        private String parameterToken(final Dsl dsl, final String name) {
            return dsl == MSC ? "#{" + name + "}" : ":" + name;
        }

        private Map<String, Object> priorProperties(final int size, final String firstName, final boolean reserveSuffix) {
            final Map<String, Object> result = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                result.put(i == 0 ? firstName : reserveSuffix && i == 1 ? "id_2" : "seed" + i, -i - 1);
            }
            return result;
        }

        private String updatePrefix(final Dsl dsl, final Map<String, Object> properties) {
            return "UPDATE records SET " + properties.keySet().stream().map(name -> name + " = " + parameterToken(dsl, name)).collect(Collectors.joining(", "));
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

    private static <E extends RuntimeException> void assertCriteriaRejectedWithoutMutation(final Class<E> expectedType, final SqlBuilder builder,
            final Criteria criteria, final String expectedSql) {
        assertThrows(expectedType, () -> builder.append(criteria));
        assertEquals(expectedSql, builder.build().query());
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

    @Table(name = "line_comment_table -- x")
    public static class LineCommentTableEntity {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }

    @Table(name = "line_comment_table -- x", alias = "lc")
    public static class LineCommentAliasedEntity {
        private long id;
        @Column("FN_COL")
        private String firstName;
        private SubEntityAddress homeAddress;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }

        public SubEntityAddress getHomeAddress() {
            return homeAddress;
        }

        public void setHomeAddress(final SubEntityAddress homeAddress) {
            this.homeAddress = homeAddress;
        }
    }

    @Table(name = "#tmp", alias = "h")
    public static class HashTempTableEntity {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }

    @Table(name = "-- x")
    public static class CommentOnlyTableEntity {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }

    @Table(name = "// x")
    public static class SlashCommentOnlyTableEntity {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }

    @Table(name = "-- x\r t")
    public static class LoneCarriageReturnTableEntity {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }

    @Table(name = "#tmp -- x")
    public static class HashTmpNoteTableEntity {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }

    @Table(name = "dbo.#tmp")
    public static class QualifiedHashTableEntity {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }

    @Table(name = "main.-- x")
    public static class QualifiedDashTableEntity {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }

    @Table(name = "slash_comment_table // x")
    public static class SlashCommentTableEntity {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }
    }

    @Table(name = "open_comment_table /* x")
    public static class OpenCommentTableEntity {
        private long id;
        private String name;

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

    private static Dsl lexDialect(final Dsl base, final String product) {
        return Dsl.forDialect(base.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of(product)).build());
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
