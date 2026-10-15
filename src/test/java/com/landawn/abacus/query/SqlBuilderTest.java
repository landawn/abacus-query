package com.landawn.abacus.query;

import static com.landawn.abacus.query.Dsl.MSB;
import static com.landawn.abacus.query.Dsl.MSC;
import static com.landawn.abacus.query.Dsl.NSB;
import static com.landawn.abacus.query.Dsl.NSC;
import static com.landawn.abacus.query.Dsl.PAC;
import static com.landawn.abacus.query.Dsl.PLC;
import static com.landawn.abacus.query.Dsl.PSB;
import static com.landawn.abacus.query.Dsl.PSC;
import static com.landawn.abacus.query.Dsl.SCSB;
import static com.landawn.abacus.query.QueryTestSupport.assertSql;
import static com.landawn.abacus.query.QueryTestSupport.selection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.QueryTestSupport.Order;
import com.landawn.abacus.query.SqlDialect.ProductInfo;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.query.condition.And;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Having;
import com.landawn.abacus.query.condition.Junction;
import com.landawn.abacus.query.condition.Limit;
import com.landawn.abacus.query.condition.Operator;
import com.landawn.abacus.query.condition.SqlExpression;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Using;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

public class SqlBuilderTest extends TestBase {

    @Nested
    class Deletes {
        // DELETE tests
        @Test
        void testDeleteFrom() {
            assertSql("DELETE FROM users WHERE id = ?", List.of(1), PSC.deleteFrom("users").where(Filters.eq("id", 1)).build());
        }

        @Test
        void testDeleteFromFactory() {
            SqlBuilder builder = PSC.deleteFrom("users");
            assertSql("DELETE FROM users", List.of(), builder.build());
        }

        @Test
        void testDeleteFromWithEntityClass() {
            assertSql("DELETE FROM test_account WHERE id = ?", List.of(1), PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build());
        }

        @Test
        void testDeleteFromWithMultipleConditions() {
            Date cutoff = new Date(1_700_000_000_000L);
            SP result = PSC.deleteFrom("users").where(Filters.and(Filters.eq("status", "DELETED"), Filters.lt("deletedDate", cutoff))).build();

            assertSql("DELETE FROM users WHERE (status = ?) AND (deleted_date < ?)", List.of("DELETED", cutoff), result);
        }
    }

    @Nested
    class Inserts {
        @Test
        void testBatchInsert() {
            List<Map<String, Object>> propsList = new ArrayList<>();
            Map<String, Object> map1 = new LinkedHashMap<>();
            map1.put("firstName", "John");
            map1.put("lastName", "Doe");

            Map<String, Object> map2 = new LinkedHashMap<>();
            map2.put("firstName", "Jane");
            map2.put("lastName", "Smith");

            propsList.add(map1);
            propsList.add(map2);

            String sql = PSC.batchInsert(propsList).into("users").build().query();

            assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?), (?, ?)", sql);
        }

        /**
         * Bug: batchInsert with all-empty Maps would produce malformed SQL: {@code INSERT INTO t () VALUES ()}.
         */
        @Test
        void testBatchInsert_EmptyMapThrows_Pass4() {
            List<Map<String, Object>> rows = Arrays.asList(new LinkedHashMap<>(), new LinkedHashMap<>());

            assertThrows(IllegalArgumentException.class, () -> PSC.batchInsert(rows));
        }

        /**
         * Bug: batchInsert with extra columns in a later row silently dropped them.
         */
        @Test
        void testBatchInsert_MapsWithExtraKeyInLaterRowThrows_Pass4() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            row2.put("email", "jane@example.com"); // extra key

            List<Map<String, Object>> rows = Arrays.asList(row1, row2);

            assertThrows(IllegalArgumentException.class, () -> PSC.batchInsert(rows));
        }

        /**
         * Regression: batchInsert with identical key sets still works correctly across multiple rows.
         */
        @Test
        void testBatchInsert_MapsWithMatchingKeysProducesCorrectSql_Pass4() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");

            Map<String, Object> row3 = new LinkedHashMap<>();
            row3.put("firstName", "Bob");
            row3.put("lastName", "Johnson");

            SP sp = PSC.batchInsert(Arrays.asList(row1, row2, row3)).into("account").build();

            assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?), (?, ?)", sp.query());
            assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith", "Bob", "Johnson"), sp.parameters());
        }

        // ===== Pass-4: DML schema validation =====

        /**
         * Bug: batchInsert(List&lt;Map&gt;) with rows that have different key sets silently produced
         * wrong SQL. The column list was taken from the first Map; for subsequent rows, missing
         * columns were filled with null and extra columns were silently dropped, causing data loss
         * and stray NULL parameters. Now an IllegalArgumentException is thrown.
         */
        @Test
        void testBatchInsert_MapsWithMismatchedKeysThrows_Pass4() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("firstName", "Jane");
            row2.put("email", "jane@example.com"); // wrong key — would silently lose lastName + email

            List<Map<String, Object>> rows = Arrays.asList(row1, row2);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.batchInsert(rows));
            assertTrue(ex.getMessage().contains("same key set"), "Exception should mention key set mismatch but was: " + ex.getMessage());
        }

        /**
         * Bug: batchInsert with a missing column in a later row silently inserted NULL for that column.
         */
        @Test
        void testBatchInsert_MapsWithMissingKeyInLaterRowThrows_Pass4() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("firstName", "Jane");
            // missing lastName

            List<Map<String, Object>> rows = Arrays.asList(row1, row2);

            assertThrows(IllegalArgumentException.class, () -> PSC.batchInsert(rows));
        }

        /**
         * Regression: batchInsert with one null element followed by valid maps continues to work
         * (null elements are skipped).
         */
        @Test
        void testBatchInsert_NullElementsSkippedThenValidated_Pass4() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");

            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(null);
            rows.add(row1);
            rows.add(null);
            rows.add(row2);

            SP sp = PSC.batchInsert(rows).into("account").build();
            assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
            assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
        }

        @Test
        void testBatchInsertAllowsSameBeanClassWithNullRows() {
            final BatchEntityWithId first = new BatchEntityWithId();
            first.setOtherId(10);
            first.setName("first");
            final BatchEntityWithId second = new BatchEntityWithId();
            second.setOtherId(20);
            second.setName("second");

            final SP sp = PSC.batchInsert(Arrays.asList(first, null, second)).into("batch_entity").build();

            assertTrue(sp.query().startsWith("INSERT INTO batch_entity"), sp.query());
            assertTrue(sp.query().contains("), ("), sp.query());
            assertEquals(4, sp.parameters().size());
        }

        @Test
        void testBatchInsertRejectsDifferentBeanClassesWithSameProperties() {
            final BatchEntityWithId first = new BatchEntityWithId();
            first.setName("first");
            final BatchEntityWithOtherId second = new BatchEntityWithOtherId();
            second.setName("second");

            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.batchInsert(Arrays.asList(first, null, second)));

            assertTrue(ex.getMessage().contains("same runtime class"), ex.getMessage());
            assertTrue(ex.getMessage().contains(BatchEntityWithId.class.getName()), ex.getMessage());
            assertTrue(ex.getMessage().contains(BatchEntityWithOtherId.class.getName()), ex.getMessage());
        }

        @Test
        void testBatchInsertRejectsRowsWithNoRemainingColumns() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();

            assertThrows(IllegalArgumentException.class, () -> PSC.batchInsert(Arrays.asList(new AllNullBatchEntity(), new AllNullBatchEntity())));
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "Rejected input must not allocate a builder whose pooled StringBuilder cannot be released");
        }

        @Test
        void testBatchInsertVariants_Batch2() {
            List<Map<String, Object>> rows = batchRows();

            List<SP> results = Arrays.asList(Dsl.SCSB.batchInsert(rows).into("users").build(), Dsl.ACSB.batchInsert(rows).into("users").build(),
                    Dsl.PSB.batchInsert(rows).into("users").build(), PSC.batchInsert(rows).into("users").build(),
                    Dsl.NSB.batchInsert(rows).into("users").build(), NSC.batchInsert(rows).into("users").build(),
                    Dsl.NAC.batchInsert(rows).into("users").build(), Dsl.NLC.batchInsert(rows).into("users").build(),
                    Dsl.MSC.batchInsert(rows).into("users").build(), Dsl.MLC.batchInsert(rows).into("users").build());

            for (SP sp : results) {
                assertInsertBuilds(sp);
            }
        }

        @Test
        void testBatchInsertWithDifferentMapKeyOrder() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("lastName", "Smith");
            row2.put("firstName", "Jane");

            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(row1);
            rows.add(row2);

            AbstractQueryBuilder.SP sp = PSC.batchInsert(rows).into("users").build();

            assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
            assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
        }

        @Test
        void testBatchInsertWithDifferentMapKeyOrder_NamedLowerCamelCase() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("lastName", "Smith");
            row2.put("firstName", "Jane");

            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(row1);
            rows.add(row2);

            AbstractQueryBuilder.SP sp = Dsl.NLC.batchInsert(rows).into("users").build();

            assertEquals("INSERT INTO users (firstName, lastName) VALUES (:firstName_0, :lastName_0), (:firstName_1, :lastName_1)", sp.query());
            assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
        }

        @Test
        void testBatchInsertWithDifferentMapKeyOrder_NamedSnakeCase() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("lastName", "Smith");
            row2.put("firstName", "Jane");

            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(row1);
            rows.add(row2);

            AbstractQueryBuilder.SP sp = NSC.batchInsert(rows).into("users").build();

            assertEquals("INSERT INTO users (first_name, last_name) VALUES (:firstName_0, :lastName_0), (:firstName_1, :lastName_1)", sp.query());
            assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
        }

        @Test
        void testBatchInsertWithEntities() {
            List<Account> accounts = new ArrayList<>();

            Account account1 = new Account();
            account1.setFirstName("John");
            account1.setLastName("Doe");

            Account account2 = new Account();
            account2.setFirstName("Jane");
            account2.setLastName("Smith");

            accounts.add(account1);
            accounts.add(account2);

            String sql = PSC.batchInsert(accounts).into("account").build().query();

            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("), ("));
        }

        @Test
        void testBatchInsertWithMixedEntityTypesThrows() {
            List<Object> entities = new ArrayList<>();
            entities.add(new Account());
            entities.add(new Order());

            assertThrows(IllegalArgumentException.class, () -> PSC.batchInsert(entities));
        }

        @Test
        void testBatchInsertWithNullMapRows() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");

            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(row1);
            rows.add(null);
            rows.add(row2);

            AbstractQueryBuilder.SP sp = PSC.batchInsert(rows).into("users").build();

            assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
            assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
        }

        @Test
        void testDeleteFromTableWithEntityVariants_Batch2() {
            List<SP> results = Arrays.asList(Dsl.SCSB.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                    Dsl.ACSB.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                    Dsl.PSB.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                    PSC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                    Dsl.NSB.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                    NSC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                    Dsl.NAC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                    Dsl.NLC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                    Dsl.MSC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build(),
                    Dsl.MLC.deleteFrom("account", SqlBuilderTest.Account.class).where(Filters.eq("id", 1)).build());

            for (SP sp : results) {
                assertDeleteBuilds(sp, "account");
            }
        }

        @Test
        void testEntityClassInsertExamplesUseEveryInsertableProperty() {
            final String fullInsert = "INSERT INTO account (id, first_name, last_name, email) VALUES (?, ?, ?, ?)";
            final String insertWithoutId = "INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)";

            assertEquals(fullInsert, PSC.insert(JavadocAccount.class).into("account").build().query());
            assertEquals(fullInsert, PSC.insertInto(JavadocAccount.class).build().query());
            assertEquals(insertWithoutId, PSC.insert(JavadocAccount.class, Collections.singleton("id")).into("account").build().query());
            assertEquals(insertWithoutId, PSC.insertInto(JavadocAccount.class, Collections.singleton("id")).build().query());
        }

        @Test
        void testIncompleteInsertAndPreSetUpdateFailFast() {
            assertThrows(IllegalStateException.class, () -> PSC.insert("id").build());
            assertThrows(IllegalStateException.class, () -> PSC.insert("id").append("RETURNING id").build());
            assertThrows(IllegalStateException.class, () -> PSC.update("users").append("RETURNING id").build());
        }

        @Test
        void testInsertBeanKeepsDefaultValuedNonIdPrimitive() {
            // only null values and default-valued ID properties are skipped; a non-ID primitive at its default is inserted
            final SP sp = PSC.insert(new com.landawn.abacus.query.entity.Account()).into("t").build();
            assertEquals("INSERT INTO t (status) VALUES (?)", sp.query());
            assertEquals(Arrays.asList(0), sp.parameters());

            // a non-default ID is inserted (only a DEFAULT-valued ID is skipped)
            final SP withId = PSC.insert(new com.landawn.abacus.query.entity.Account(5)).into("t").build();
            assertEquals("INSERT INTO t (id, status) VALUES (?, ?)", withId.query());
            assertEquals(Arrays.asList(5L, 0), withId.parameters());

            // the reworded @throws: excluding the only insertable value leaves nothing to insert
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> PSC.insert(new com.landawn.abacus.query.entity.Account(), N.asSet("status")).into("t").build());
            assertTrue(e.getMessage().contains("No insertable values remain"), e.getMessage());
        }

        // INSERT tests
        @Test
        void testInsertInto() {
            String sql = PSC.insert("id", "name").into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
        }

        @Test
        void testInsertIntoFactory() {
            SqlBuilder builder = PSC.insert("id", "name");
            assertNotNull(builder);
        }

        @Test
        void testInsertIntoWithEntityClass() {
            String sql = PSC.insert("firstName", "lastName").into(Account.class).build().query();
            assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        void testInsertSelectColumnListOmitsSelectAlias() {
            assertEquals("INSERT INTO bk (first_name) SELECT first_name AS fn FROM account",
                    PSC.select("firstName AS fn").into("bk").from("account").build().query());
            assertEquals("INSERT INTO bk (first_name) SELECT first_name AS \"fn\" FROM account",
                    PSC.select(Map.of("firstName", "fn")).into("bk").from("account").build().query());
        }

        @Test
        void testInsertWithEntity() {
            Account account = new Account();
            account.setId(1);
            account.setFirstName("John");
            account.setLastName("Doe");

            String sql = PSC.insert(account).into("account").build().query();

            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testInsertWithExcludedProperties() {
            Account account = new Account();
            account.setId(1);
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedDate(new Date(1_700_000_000_000L));
            account.setLastModifiedDate(new Date(1_700_086_400_000L));

            Set<String> excluded = N.asSet("createdDate", "lastModifiedDate");

            SP result = PSC.insert(account, excluded).into("account").build();

            assertSql("INSERT INTO account (id, first_name, last_name, age) VALUES (?, ?, ?, ?)", List.of(1L, "John", "Doe", 0), result);
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("firstName", "John");
            values.put("lastName", "Doe");
            values.put("email", "john@example.com");

            String sql = PSC.insert(values).into("users").build().query();

            assertEquals("INSERT INTO users (first_name, last_name, email) VALUES (?, ?, ?)", sql);
        }

        @Test
        void testMSB_SelectFromMultiSelectionAndBatchInsert() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a1", "account1", null, false, null),
                    selection(Account.class, "a2", "account2", null, false, null));

            SqlBuilder selectBuilder = Dsl.MSB.selectFrom(selections);
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(new LinkedHashMap<>(Map.of("firstName", "John")));
            rows.add(new LinkedHashMap<>(Map.of("firstName", "Jane")));
            SP batchInsert = Dsl.MSB.batchInsert(rows).into("users").build();

            assertTrue(selectBuilder.build().query().contains("FROM"));
            assertTrue(batchInsert.query().contains("INSERT INTO users"));
        }

        @Test
        void testMultiEntitySelectFromVariants_Batch2() {
            Set<String> excludedA = Collections.singleton("lastModifiedDate");
            Set<String> excludedB = Collections.singleton("amount");

            List<String> results = Arrays.asList(
                    Dsl.SCSB.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                    Dsl.ACSB.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                    Dsl.PSB.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                    PSC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                    Dsl.NSB.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                    NSC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                    Dsl.NAC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                    Dsl.NLC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                    Dsl.MSC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query(),
                    Dsl.MLC.selectFrom(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB).build().query());

            for (String sql : results) {
                assertTrue(sql.contains("FROM"));
                assertTrue(sql.contains("a1"));
                assertTrue(sql.contains("o1"));
            }
        }

        @Test
        void testMultiEntitySelectVariants_Batch2() {
            Set<String> excludedA = Collections.singleton("lastModifiedDate");
            Set<String> excludedB = Collections.singleton("amount");

            List<String> results = Arrays.asList(
                    Dsl.SCSB.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query(),
                    Dsl.ACSB.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query(),
                    Dsl.PSB.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query(),
                    PSC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query(),
                    Dsl.NSB.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query(),
                    NSC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query(),
                    Dsl.NAC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query(),
                    Dsl.NLC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query(),
                    Dsl.MSC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query(),
                    Dsl.MLC.select(SqlBuilderTest.Account.class, "a1", "account1", excludedA, Order.class, "o1", "order1", excludedB)
                            .from("test_account a1, user_order o1")
                            .build()
                            .query());

            for (String sql : results) {
                assertTrue(sql.contains("SELECT"));
                assertTrue(sql.contains("FROM test_account a1, user_order o1"));
            }
        }

        // Named SQL tests
        @Test
        void testNamedInsert() {
            String sql = PSC.insert("firstName", "lastName").into(Account.class).build().query();
            assertNotNull(sql);
        }

        /**
         * Bug: in the INSERT ... SELECT flow, the modifier insertion searched for the "SELECT" substring
         * and matched inside a rendered identifier (e.g. "SELECTED_FLAG"), splicing the modifier into the
         * column list of the INSERT clause.
         */
        @Test
        void testSelectModifierWithInsertSelectAndSelectLikeColumnName() {
            String sql = Dsl.PAC.select("selectedFlag").into("t2").distinct().from("t1").build().query();
            assertEquals("INSERT INTO t2 (SELECTED_FLAG) SELECT DISTINCT SELECTED_FLAG AS \"selectedFlag\" FROM t1", sql);
        }

        @Test
        void testSharedInsertDispatchPreservesPolicySetterHooks() {
            class RecordingBuilder extends SqlBuilder {
                int genericCalls;
                int policyCalls;

                RecordingBuilder(final SqlDialect dialect) {
                    super(dialect);
                    _isForConditionOnly = true;
                }

                @Override
                protected void setParameter(final String name, final Object value) {
                    genericCalls++;
                    super.setParameter(name, value);
                }

                @Override
                protected void setParameterForParameterizedSql(final Object value) {
                    policyCalls++;
                    super.setParameterForParameterizedSql(value);
                }

                @Override
                protected void setParameterForRawSql(final Object value) {
                    policyCalls++;
                    super.setParameterForRawSql(value);
                }

                @Override
                protected void setParameterForNamedSql(final String name, final Object value) {
                    policyCalls++;
                    super.setParameterForNamedSql(name, value);
                }

                @Override
                protected void setParameterForIbatisNamedSql(final String name, final Object value) {
                    policyCalls++;
                    super.setParameterForIbatisNamedSql(name, value);
                }
            }
            final Dsl[] dsls = { PSC, NSC, MSC, SCSB };
            final String[] expected = { "?, ?", ":id_3, :firstName_3", "#{id_3}, #{firstName_3}", "1, 'A'" };
            final Map<String, Object> values = new LinkedHashMap<>();
            values.put("id", 1);
            values.put("firstName", "A");
            for (int i = 0; i < dsls.length; i++) {
                final RecordingBuilder builder = new RecordingBuilder(dsls[i].sqlDialect());
                builder.appendInsertProps(values, values.keySet(), 3);
                assertEquals(0, builder.genericCalls);
                assertEquals(2, builder.policyCalls);
                assertEquals(expected[i], builder.build().query());
            }
        }

        @Test
        void testUpdateEntityWithExcludedPropsVariants_Batch2() {
            Set<String> excluded = Collections.singleton("lastModifiedDate");

            List<SP> results = Arrays.asList(Dsl.SCSB.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                    Dsl.ACSB.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                    Dsl.PSB.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                    PSC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                    Dsl.NSB.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                    NSC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                    Dsl.NAC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                    Dsl.NLC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                    Dsl.MSC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build(),
                    Dsl.MLC.update(SqlBuilderTest.Account.class, excluded).set("status").where(Filters.eq("id", 1)).build());

            for (SP sp : results) {
                assertTrue(sp.query().contains("UPDATE"));
                assertTrue(sp.query().contains("WHERE"));
            }
        }

        @Test
        void testUpdateTableWithEntityVariants_Batch2() {
            List<SP> results = Arrays.asList(
                    Dsl.SCSB.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                    Dsl.ACSB.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                    Dsl.PSB.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                    PSC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                    Dsl.NSB.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                    NSC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                    Dsl.NAC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                    Dsl.NLC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                    Dsl.MSC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build(),
                    Dsl.MLC.update("account", SqlBuilderTest.Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build());

            for (SP sp : results) {
                assertUpdateBuilds(sp, "account");
            }
        }
    }

    @Nested
    class Joins {
        @Test
        void testComplexJoinConditions() {
            String sql = PSC.select("*")
                    .from("users u")
                    .leftJoin("orders o")
                    .on(Filters.and(Filters.eq("u.id", "o.user_id"), Filters.eq("o.status", "COMPLETED")))
                    .build()
                    .query();

            assertTrue(sql.contains("LEFT JOIN orders o ON"));
            assertTrue(sql.contains("u.id = ?"));
            assertTrue(sql.contains("o.status = ?"));
        }

        @Test
        void testCriteriaJoinWithRawConditionRendersOnKeyword() {
            // A raw (non-On/Using) join condition previously rendered with no ON keyword and no separator,
            // fusing the table name and the condition: "JOIN orderst1.id = ...".
            Criteria exprJoin = Criteria.builder().join("orders", Filters.expr("users.id = orders.user_id")).build();
            assertEquals("SELECT * FROM users JOIN orders ON users.id = orders.user_id", PSC.select("*").from("users").append(exprJoin).build().query());

            Criteria binaryJoin = Criteria.builder().join("orders", Filters.equal("orders.user_id", 5)).build();
            SP sp = PSC.select("*").from("users").append(binaryJoin).build();
            assertEquals("SELECT * FROM users JOIN orders ON orders.user_id = ?", sp.query());
            assertEquals(Arrays.asList(5), sp.parameters());

            // An On condition renders its own ON keyword -- no double "ON ON".
            Criteria onJoin = Criteria.builder().join("orders", Filters.on("users.id", "orders.user_id")).build();
            String onSql = PSC.select("*").from("users").append(onJoin).build().query();
            assertTrue(onSql.contains("JOIN orders ON ("));
            assertFalse(onSql.contains("ON ON"));
        }

        /**
         * Bug: a {@code Using} join condition reached the generic Cell branch of appendCondition, which
         * wrapped its already-parenthesized column list in a second pair of parentheses:
         * {@code USING ((employee_id))} — invalid SQL.
         */
        @Test
        void testCriteriaJoinWithUsingRendersSingleParentheses() {
            Criteria criteria = Criteria.builder().innerJoin("employees", new Using("employee_id")).build();
            String sql = PSC.select("*").from("orders").append(criteria).build().query();
            assertEquals("SELECT * FROM orders INNER JOIN employees USING (employee_id)", sql);

            Criteria multiColumn = Criteria.builder().leftJoin("assignments", new Using("company_id", "department_id")).build();
            String sql2 = PSC.select("*").from("projects").append(multiColumn).build().query();
            assertEquals("SELECT * FROM projects LEFT JOIN assignments USING (company_id, department_id)", sql2);
        }

        @Test
        void testCriteriaUsingColumnsDoNotInheritEntityTableAlias() {
            final Criteria criteria = Criteria.builder().join("test_account b", Filters.using("status")).where(Filters.eq("id", 1)).build();
            final SP result = PSC.select("*").from(Account.class, "a").append(criteria).build();

            assertEquals("SELECT * FROM test_account a JOIN test_account b USING (account_status) WHERE a.id = ?", result.query());
            assertEquals(Arrays.asList(1), result.parameters());
        }

        @Test
        void testCrossJoin() {
            String sql = PSC.select("*").from("users").crossJoin("roles").build().query();
            assertTrue(sql.contains("CROSS JOIN"));
        }

        @Test
        void testFailedUsingRenderingRestoresEntityTableAlias() {
            final SqlBuilder builder = PSC.select("*").from(Account.class, "a").join(Account.class, "b");

            assertThrows(IllegalArgumentException.class, () -> builder.using(Arrays.asList("status", "id -- unsafe")));
            final SP result = builder.on(Filters.eq("id", 1)).build();
            assertEquals("SELECT * FROM test_account a JOIN test_account b ON a.id = ?", result.query());
            assertEquals(Arrays.asList(1), result.parameters());
        }

        // Multiple table sources
        @Test
        void testFromMultipleTablesWithJoin() {
            String sql = PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").join("products p").on("o.product_id = p.id").build().query();
            assertTrue(sql.contains("users u"));
            assertTrue(sql.contains("orders o"));
            assertTrue(sql.contains("products p"));
        }

        @Test
        void testFullJoin() {
            String sql = PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").build().query();
            assertTrue(sql.contains("FULL JOIN"));
        }

        // JOIN tests
        @Test
        void testInnerJoin() {
            String sql = PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").build().query();
            assertTrue(sql.contains("JOIN"));
            assertTrue(sql.contains("ON"));
        }

        @Test
        void testJoin() {
            String sql = PSC.select("*").from("users u").join("orders o ON u.id = o.user_id").build().query();

            assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);

            // With entity class
            sql = PSC.select("*").from(Account.class, "a").join(Order.class, "o").on("a.id = o.user_id").build().query();

            assertEquals("SELECT * FROM test_account a JOIN user_order o ON a.id = o.user_id", sql);
        }

        // USING clause tests
        @Test
        void testJoinUsing() {
            String sql = PSC.select("*").from("users").join("orders").using("user_id").build().query();
            assertTrue(sql.contains("USING (user_id)"));
        }

        @Test
        void testJoinUsingMultipleColumns() {
            String sql = PSC.select("*").from("table1").join("table2").using("(col1, col2)").build().query();

            assertEquals("SELECT * FROM table1 JOIN table2 USING (col1, col2)", sql);
        }

        @Test
        void testJoinWithEntityClasses() {
            String sql = PSC.select("*").from(Account.class, "a").join(Order.class, "o").on(Filters.eq("a.id", "o.userId")).build().query();

            assertTrue(sql.contains("FROM test_account a"));
            assertTrue(sql.contains("JOIN user_order o"));
        }

        @Test
        void testLeftJoin() {
            String sql = PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").build().query();
            assertTrue(sql.contains("LEFT JOIN"));
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
            assertTrue(sql.contains("INNER JOIN"));
            assertTrue(sql.contains("LEFT JOIN"));
        }

        @Test
        void testNaturalJoin() {
            String sql = PSC.select("*").from("users").naturalJoin("orders").build().query();

            assertEquals("SELECT * FROM users NATURAL JOIN orders", sql);
        }

        @Test
        void testOn() {
            String sql = PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();

            assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);

            // With condition
            sql = PSC.select("*").from("users u").join("orders o").on(Filters.eq("u.id", "o.user_id")).build().query();

            assertTrue(sql.contains("ON"));
        }

        /**
         * Scenario 1 — JOIN with parameter + WHERE with parameter via Criteria.
         * Verifies join-ON parameters appear before WHERE parameters in the final list,
         * matching the left-to-right order of {@code ?} placeholders in the SQL.
         */
        @Test
        void testParamOrder_JoinOnWithParamThenWhere_Pass4() {
            com.landawn.abacus.query.condition.InnerJoin innerJoin = Filters.innerJoin("orders o",
                    Filters.expr("u.id = o.user_id").and(Filters.eq("o.status", "active")));
            Criteria crit = Criteria.builder().join(innerJoin).where(Filters.gt("u.age", 18)).build();
            SP sp = PSC.select("u.id").from("users u").append(crit).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList("active", 18), sp.parameters(), "Expected [active, 18] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        /**
         * Scenario 4 — Multiple JOINs with parameters, then WHERE with parameter.
         */
        @Test
        void testParamOrder_MultipleJoinsWithParams_Pass4() {
            com.landawn.abacus.query.condition.InnerJoin innerJoin = Filters.innerJoin("orders o",
                    Filters.expr("u.id = o.user_id").and(Filters.gt("o.amount", 100)));
            com.landawn.abacus.query.condition.LeftJoin leftJoin = Filters.leftJoin("payments p",
                    Filters.expr("p.order_id = o.id").and(Filters.eq("p.method", "CC")));
            Criteria crit = Criteria.builder().join(innerJoin, leftJoin).where(Filters.eq("u.country", "US")).build();
            SP sp = PSC.select("u.id").from("users u").append(crit).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList(100, "CC", "US"), sp.parameters(), "Expected [100, CC, US] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        @Test
        void testRightJoin() {
            String sql = PSC.select("*").from("users").rightJoin("departments").on("users.dept_id = departments.id").build().query();
            assertTrue(sql.contains("RIGHT JOIN"));
        }

        @Test
        void testUsing() {
            String sql = PSC.select("*").from("users").join("orders").using("user_id").build().query();

            assertEquals("SELECT * FROM users JOIN orders USING (user_id)", sql);
        }

        @Test
        void testUsingColumnsDoNotInheritEntityTableAlias() {
            for (final Dsl dsl : new Dsl[] { PSB, PSC, NSC }) {
                final String expected = "SELECT * FROM test_account a JOIN test_account b USING (account_status) WHERE a.id = ";
                final String placeholder = dsl == NSC ? ":id" : "?";

                assertEquals(expected + placeholder,
                        dsl.select("*").from(Account.class, "a").join(Account.class, "b").using("status").where(Filters.eq("id", 1)).build().query());
                assertEquals(expected + placeholder,
                        dsl.select("*").from(Account.class, "a").join(Account.class, "b").using("(status)").where(Filters.eq("id", 1)).build().query());
                assertEquals(expected + placeholder,
                        dsl.select("*")
                                .from(Account.class, "a")
                                .join(Account.class, "b")
                                .using(Collections.singletonList("status"))
                                .where(Filters.eq("id", 1))
                                .build()
                                .query());
                assertEquals(expected + placeholder,
                        dsl.select("*")
                                .from(Account.class, "a")
                                .join(Account.class, "b")
                                .on(Filters.using("status"))
                                .where(Filters.eq("id", 1))
                                .build()
                                .query());
                assertEquals("SELECT * FROM test_account a JOIN test_account b USING (id, account_status)",
                        dsl.select("*").from(Account.class, "a").join(Account.class, "b").using("id", "status").build().query());
            }
        }

        @Test
        void testUsingRejectsQualifiedColumnMappings() {
            // Clearing the table alias only stops the builder from adding a qualifier; a property mapped to an
            // already-qualified column still renders one, which USING (...) does not accept. Reject it instead of
            // emitting invalid SQL.
            final List<Function<SqlBuilder, SqlBuilder>> usingForms = Arrays.asList(builder -> builder.using("name"),
                    builder -> builder.using(new String[] { "name" }), builder -> builder.using(Arrays.asList("name")),
                    builder -> builder.on(Filters.using("name")), builder -> builder.using("(p.display_name)"));

            for (final Function<SqlBuilder, SqlBuilder> usingForm : usingForms) {
                final SqlBuilder builder = PSC.select("*").from(QualifiedProfile.class, "p").join("other");
                final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> usingForm.apply(builder));

                assertTrue(failure.getMessage().contains("USING column names must be unqualified"), failure.getMessage());
                assertTrue(failure.getMessage().contains("p.display_name"), failure.getMessage());

                // The rejected clause is rolled back, so the pending JOIN can still be completed with ON.
                assertEquals("SELECT * FROM profiles p JOIN other ON p.display_name = ?", builder.on(Filters.eq("name", "x")).build().query());
            }
        }
    }

    @Nested
    class Lifecycle {
        @Test
        void testAccept_BiConsumer() {
            final List<String> captured = new ArrayList<>();
            PSC.select("*").from("account").where(Filters.eq("id", 1)).accept((sql, params) -> {
                captured.add(sql);
                captured.add(String.valueOf(params.size()));
            });
            assertEquals(2, captured.size());
            assertEquals("SELECT * FROM account WHERE id = ?", captured.get(0));
            assertEquals("1", captured.get(1));
        }

        @Test
        void testAccept_Consumer() {
            final List<String> captured = new ArrayList<>();
            PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).accept(sp -> {
                captured.add(sp.query());
                captured.add(sp.parameters().toString());
            });
            assertEquals(2, captured.size());
            assertEquals("SELECT * FROM account WHERE status = ?", captured.get(0));
            assertEquals("[ACTIVE]", captured.get(1));
        }

        @Test
        void testActiveStringBuilderLimit() {
            List<SqlBuilder> builders = new ArrayList<>();
            int activeBefore = AbstractQueryBuilder.activeStringBuilderCounter.get();

            // Building each query should increment the active StringBuilder counter until build() releases it.
            for (int i = 0; i < 10; i++) {
                builders.add(PSC.select("*").from("users"));
            }

            assertEquals(activeBefore + builders.size(), AbstractQueryBuilder.activeStringBuilderCounter.get());

            for (SqlBuilder builder : builders) {
                builder.build();
            }

            assertEquals(activeBefore, AbstractQueryBuilder.activeStringBuilderCounter.get());
        }

        @Test
        void testAppendIfRejectsClosedBuilderForBothBranchesAndOverloads() {
            SqlBuilder conditionBuilder = PSC.select("*").from("users");
            conditionBuilder.build();

            assertThrows(IllegalStateException.class, () -> conditionBuilder.appendIf(false, Filters.eq("id", 1)));
            assertThrows(IllegalStateException.class, () -> conditionBuilder.appendIf(true, Filters.eq("id", 1)));

            SqlBuilder expressionBuilder = PSC.select("*").from("users");
            expressionBuilder.build();

            assertThrows(IllegalStateException.class, () -> expressionBuilder.appendIf(false, "FOR UPDATE"));
            assertThrows(IllegalStateException.class, () -> expressionBuilder.appendIf(true, "FOR UPDATE"));
        }

        @Test
        void testApplyBiFunction() throws Exception {
            String result = PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).apply((sql, params) -> sql + " - " + params.size());

            assertEquals("SELECT * FROM account WHERE status = ? - 1", result);
        }

        @Test
        void testApplyFunction() throws Exception {
            List<String> result = PSC.select("*")
                    .from("account")
                    .where(Filters.eq("status", "ACTIVE"))
                    .apply(sp -> Arrays.asList(sp.query(), sp.parameters().toString()));

            assertEquals(2, result.size());
            assertEquals("SELECT * FROM account WHERE status = ?", result.get(0));
            assertEquals("[ACTIVE]", result.get(1));
        }

        @Test
        void testBatchInsertMapRowsAreDefensivelyCopiedBeforeBuild() {
            Map<String, Object> row1 = new LinkedHashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");

            Map<String, Object> row2 = new LinkedHashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");

            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(row1);
            rows.add(row2);

            SqlBuilder builder = PSC.batchInsert(rows);
            row1.clear();
            row2.put("email", "jane@example.com");
            rows.clear();

            SP sp = builder.into("account").build();
            assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
            assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
        }

        @Test
        void testBuilderInputsAreDefensivelyCopiedBeforeBuild() {
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("firstName", "John");
            SqlBuilder insert = PSC.insert(props);
            props.clear();

            SP insertSp = insert.into("account").build();
            assertEquals("INSERT INTO account (first_name) VALUES (?)", insertSp.query());
            assertEquals(Arrays.asList("John"), insertSp.parameters());

            Map<String, Object> entityProps = new LinkedHashMap<>();
            entityProps.put("lastName", "Doe");
            Object entity = entityProps;
            SqlBuilder insertObject = PSC.insert(entity);
            entityProps.clear();

            SP insertObjectSp = insertObject.into("account").build();
            assertEquals("INSERT INTO account (last_name) VALUES (?)", insertObjectSp.query());
            assertEquals(Arrays.asList("Doe"), insertObjectSp.parameters());

            List<String> columns = new ArrayList<>(Arrays.asList("firstName"));
            SqlBuilder selectColumns = PSC.select(columns);
            columns.add("lastName");
            columns.clear();
            assertEquals("SELECT first_name AS \"firstName\" FROM account", selectColumns.from("account").build().query());

            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            SqlBuilder selectAliases = PSC.select(aliases);
            aliases.put("lastName", "lname");
            aliases.clear();
            assertEquals("SELECT first_name AS \"fname\" FROM account", selectAliases.from("account").build().query());
        }

        @Test
        void testBuilderSubclassStillObservesBetweenNamesAndEveryInValue() {
            class RecordingBuilder extends SqlBuilder {
                final List<String> names = new ArrayList<>();

                RecordingBuilder(final SqlDialect dialect) {
                    super(dialect);
                    _isForConditionOnly = true;
                }

                @Override
                protected void setParameter(final String name, final Object value) {
                    names.add(name);
                    super.setParameter(name, value);
                }
            }
            for (final Dsl dsl : new Dsl[] { PSC, NSC, MSC, SCSB }) {
                final RecordingBuilder builder = new RecordingBuilder(dsl.sqlDialect());
                builder.appendCondition(Filters.between("u.createdAt", 1, 9));
                assertEquals(Arrays.asList("minCreatedAt", "maxCreatedAt"), builder.names);
                builder.build();
            }
            final RecordingBuilder builder = new RecordingBuilder(PSC.sqlDialect());
            builder.appendCondition(Filters.in("id", java.util.stream.IntStream.range(0, 32).boxed().toList()));
            assertEquals(Collections.nCopies(32, "id"), builder.names);
            assertEquals(32, builder.build().parameters().size());
        }

        // Build method tests
        @Test
        void testBuildMethod() {
            AbstractQueryBuilder.SP sp = PSC.select("*").from("users").build();
            assertNotNull(sp);
            assertNotNull(sp.query());
        }

        @Test
        void testBuiltInsertsReleaseStagedMapsAndKeepTheirParameterValues() {
            final Object value = new byte[] { 1, 2, 3 };
            final SqlBuilder insert = Dsl.PSC.insert(Map.of("value", value)).into("measurements");
            assertNotNull(insert._props);

            final SP result = insert.build();
            assertStagedInputsReleased(insert);
            assertEquals("INSERT INTO measurements (value) VALUES (?)", result.query());
            assertSame(value, result.parameters().get(0));
            assertSame(value, insert.parameters().get(0));

            final SqlBuilder batch = Dsl.SCSB.batchInsert(List.of(Map.of("value", 1), Map.of("value", 2))).into("measurements");
            assertNotNull(batch._propsList);
            assertEquals("INSERT INTO measurements (value) VALUES (1), (2)", batch.build().query());
            assertStagedInputsReleased(batch);
        }

        @Test
        void testBuiltQueriesReleaseEveryProjectionForm() {
            final SqlBuilder columns = Dsl.PSC.select("id").from("measurements");
            final SqlBuilder aliases = Dsl.PSC.select(Map.of("id", "key")).from("measurements");
            final SqlBuilder selections = Dsl.PSC
                    .select(Selection.builder(com.landawn.abacus.query.entity.Account.class).includedPropNames(List.of("id")).build())
                    .from("measurements");
            assertNotNull(columns._propOrColumnNames);
            assertNotNull(aliases._propOrColumnNameAliases);
            assertNotNull(selections._multiSelects);

            for (final SqlBuilder query : List.of(columns, aliases, selections)) {
                assertTrue(query.build().query().startsWith("SELECT "));
                assertStagedInputsReleased(query);
            }
        }

        @Test
        void testClassBasedSelectFromRenderingFailureReleasesAllocatedBuilder() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();

            assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(BlankTableNameEntity.class, null, false, null));
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "A failed class-based SELECT FROM convenience factory must release its pooled buffer");
        }

        @Test
        void testClosedBuilderException() {
            // Test that using a closed builder throws exception
            SqlBuilder builder = PSC.select("*").from("users");
            builder.build().query(); // This closes the builder

            // Any operation after sql() should throw exception
            assertThrows(RuntimeException.class, () -> builder.where(Filters.eq("id", 1)));
            assertThrows(RuntimeException.class, () -> builder.build().query());
        }

        @Test
        void testConsumedChildrenKeepMetadataNeededForNamedParameterComposition() {
            final SqlBuilder child = Dsl.NSC.select(Map.of("id", "key")).from("other").where(Filters.eq("id", 2));
            final SP union = Dsl.NSC.select("id").from("measurements").where(Filters.eq("id", 1)).union(child).build();
            assertStagedInputsReleased(child);
            assertEquals("SELECT id FROM measurements WHERE id = :id UNION SELECT id AS \"key\" FROM other WHERE id = :id_2", union.query());
            assertEquals(List.of(1, 2), union.parameters());

            final SqlBuilder source = Dsl.NSC.select("id").from("other").where(Filters.eq("id", 2));
            final SubQuery snapshot = source.toSubQuery();
            assertStagedInputsReleased(source);

            for (int i = 0; i < 2; i++) {
                final SP parent = Dsl.NSC.select("id").from("measurements").where(Filters.and(Filters.eq("id", 1), Filters.in("id", snapshot))).build();
                assertEquals("SELECT id FROM measurements WHERE (id = :id) AND (id IN (SELECT id FROM other WHERE id = :id_2))", parent.query());
                assertEquals(List.of(1, 2), parent.parameters());
            }
        }

        @Test
        void testCountByNonBeanClassReleasesAllocatedBuilder() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();

            assertThrows(IllegalArgumentException.class, () -> PSC.selectCountFrom(String.class));
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "A count factory whose FROM rejects the entity class must release its pooled buffer");

            assertThrows(IllegalArgumentException.class, () -> PSC.selectCountFrom(BlankTableNameEntity.class));
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "A count factory whose FROM rejects mapped table metadata must release its pooled buffer");

            // The pool must remain usable: a subsequent normal count build still succeeds.
            assertEquals("SELECT count(*) FROM test_account", PSC.selectCountFrom(Account.class).build().query());
        }

        @Test
        void testCountRejectsBlankTableBeforeAllocatingBuilder() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();

            assertThrows(IllegalArgumentException.class, () -> PSC.selectCountFrom("   "));
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "Rejected input must not allocate a builder whose pooled StringBuilder cannot be released");
        }

        @Test
        void testCriteriaSelectModifierConflictsWithExistingBuilderModifier() {
            Criteria criteria = Criteria.builder().distinct().build();

            assertThrows(IllegalStateException.class, () -> PSC.select("department").distinct().from("employees").append(criteria));
        }

        @Test
        void testFailedFinalizationReleasesInputsWhilePreliminaryFailureKeepsThemReusable() {
            final SqlBuilder incompleteInsert = Dsl.PSC.insert(Map.of("value", 1));
            assertThrows(IllegalStateException.class, incompleteInsert::build);
            assertStagedInputsReleased(incompleteInsert);

            final SqlBuilder incompleteSelect = Dsl.PSC.select("id");
            assertThrows(IllegalStateException.class, incompleteSelect::build);
            assertStagedInputsReleased(incompleteSelect);

            final SqlBuilder incompleteJoin = Dsl.PSC.select("id").from("measurements m").join("other o");
            assertThrows(IllegalStateException.class, incompleteJoin::build);
            assertNotNull(incompleteJoin._sb);
            assertNotNull(incompleteJoin._propOrColumnNames);
            assertEquals("SELECT id FROM measurements m JOIN other o ON m.id = o.id", incompleteJoin.on("m.id = o.id").build().query());
            assertStagedInputsReleased(incompleteJoin);
        }

        @Test
        void testFailedStructuredSubQueryRenderingReleasesNestedAndOuterBuilders() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();
            final SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), unsupportedCondition());

            assertThrows(IllegalArgumentException.class, () -> PSC.renderCondition(Filters.in("id", subQuery)));
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "A failed condition factory must release both its outer and structured-subquery buffers");
        }

        @Test
        void testInsertIntoRenderingFailureReleasesAllocatedBuilder() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();
            final IllegalStateException rendererFailure = new IllegalStateException("named renderer failed");
            final Dsl throwingNamedDsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
                throw rendererFailure;
            }).build());

            assertSame(rendererFailure, assertThrows(IllegalStateException.class, () -> throwingNamedDsl.insertInto(JavadocAccount.class)));
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "A failed INSERT INTO convenience factory must release its pooled buffer");
        }

        @Test
        void testInsertMapRejectsAllKeysExcludedBeforeAllocatingBuilder() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();

            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.insert((Object) N.asMap("a", 1), N.asSet("a")));

            assertTrue(ex.getMessage().contains("entity map must contain at least one non-excluded property"), ex.getMessage());
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "Rejected input must not allocate a builder whose pooled StringBuilder cannot be released");
        }

        // Regression: KEBAB_CASE was rejected only by the Dsl constructor, so a SqlBuilder subclass calling
        // super(kebabDialect) still rendered hyphenated (invalid) identifiers.
        @Test
        void testKebabCaseNamingPolicyIsRejectedByBuilderConstructor() {
            final SqlDialect kebab = SqlDialect.builder().namingPolicy(NamingPolicy.KEBAB_CASE).sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL).build();
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new SqlBuilder(kebab) {
            });
            assertTrue(e.getMessage().contains("KEBAB_CASE"), e.getMessage());

            final IllegalArgumentException viaDsl = assertThrows(IllegalArgumentException.class, () -> Dsl.forDialect(kebab));
            assertEquals(viaDsl.getMessage(), e.getMessage());
        }

        @Test
        void testMetadataFactoriesRejectEmptyColumnSetsBeforeAllocatingBuilder() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();
            final Set<String> allProperties = Collections.singleton("value");
            final Selection emptySelection = Selection.builder(AllNullBatchEntity.class).excludedPropNames(allProperties).build();

            assertThrows(IllegalArgumentException.class, () -> PSC.insert(AllNullBatchEntity.class, allProperties));
            assertThrows(IllegalArgumentException.class, () -> PSC.select(AllNullBatchEntity.class, allProperties));
            assertThrows(IllegalArgumentException.class, () -> PSC.update(AllNullBatchEntity.class, allProperties));
            assertThrows(IllegalArgumentException.class, () -> PSC.select(emptySelection));
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "Factories that cannot produce a column must reject before allocating pooled buffers");
        }

        // Performance and resource cleanup
        @Test
        void testMultipleBuildCalls() {
            SqlBuilder builder = PSC.select("*").from("users");
            String sql1 = builder.build().query();
            assertNotNull(sql1);
            // Builder should be reusable or properly cleaned up
        }

        @Test
        void testProtectedBuilderHelpersRejectNullWithIllegalArgumentException() {
            final SqlBuilder builder = PSC.select("id");

            assertThrows(IllegalArgumentException.class, () -> builder.appendOperationBeforeFrom(null));
            assertThrows(IllegalArgumentException.class, () -> builder.appendUsingClause(null));

            // the rejected calls left the builder untouched
            assertEquals("SELECT id FROM users", builder.from("users").build().query());
        }

        @Test
        void testRawSubQueryBindingsRetainPolicyInReusableSnapshots() {
            final SubQuery activeOwners = new SubQuery("SELECT id FROM owners WHERE active = ?", Arrays.asList(true));
            final SubQuery archived = PSC.select("id").from("archived_records").where(Filters.in("owner_id", activeOwners)).toSubQuery();
            final SqlBuilder incompatible = NSC.select("id").from("current_records");

            assertThrows(IllegalArgumentException.class, () -> incompatible.where(Filters.in("id", archived)));
            assertEquals("SELECT id FROM current_records WHERE id = :id", incompatible.where(Filters.eq("id", 1)).build().query());

            final SP compatible = PSC.select("id").from("current_records").where(Filters.in("id", archived)).build();
            assertEquals(
                    "SELECT id FROM current_records WHERE id IN (SELECT id FROM archived_records WHERE owner_id IN (SELECT id FROM owners WHERE active = ?))",
                    compatible.query());
            assertEquals(Arrays.asList(true), compatible.parameters());
        }

        @Test
        void testRejectedSiblingSetOperationDoesNotConsumeRightBuilder() {
            SqlBuilder right = PSC.select("id").from("archived_records");

            assertThrows(IllegalStateException.class, () -> PSC.select("id").unionAll(right));
            assertEquals("SELECT id FROM archived_records", right.build().query());
        }

        @Test
        void testSelectFromRenderingFailureReleasesAllocatedBuilder() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();
            final Selection unsafeSelection = Selection.builder(AllNullBatchEntity.class)
                    .includedPropNames(Collections.singletonList("value -- unsafe"))
                    .build();

            assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(unsafeSelection));
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "A SELECT FROM factory that fails during projection rendering must release its pooled buffer");
        }

        @Test
        void testSetOperationBuildersKeepNamedParameterNamesUnique() {
            assertEquals("SELECT id FROM users WHERE status = :status UNION SELECT id FROM archived_users WHERE status = :status_2",
                    NSC.select("id")
                            .from("users")
                            .where(Filters.eq("status", "ACTIVE"))
                            .union(NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                            .build()
                            .query());

            assertEquals("SELECT id FROM users WHERE status = :status UNION ALL SELECT id FROM archived_users WHERE status = :status_2",
                    NSC.select("id")
                            .from("users")
                            .where(Filters.eq("status", "ACTIVE"))
                            .unionAll(NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                            .build()
                            .query());

            assertEquals("SELECT id FROM users WHERE status = :status INTERSECT SELECT id FROM archived_users WHERE status = :status_2",
                    NSC.select("id")
                            .from("users")
                            .where(Filters.eq("status", "ACTIVE"))
                            .intersect(NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                            .build()
                            .query());

            assertEquals("SELECT id FROM users WHERE status = :status EXCEPT SELECT id FROM archived_users WHERE status = :status_2",
                    NSC.select("id")
                            .from("users")
                            .where(Filters.eq("status", "ACTIVE"))
                            .except(NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                            .build()
                            .query());

            assertEquals("SELECT id FROM users WHERE status = :status MINUS SELECT id FROM archived_users WHERE status = :status_2",
                    NSC.select("id")
                            .from("users")
                            .where(Filters.eq("status", "ACTIVE"))
                            .minus(NSC.select("id").from("archived_users").where(Filters.eq("status", "INACTIVE")))
                            .build()
                            .query());
        }

        @Test
        void testSetOperationsApplyOrderingAndPaginationToCombinedResult() {
            final List<BiConsumer<SqlBuilder, SqlBuilder>> setOperations = Arrays.asList(SqlBuilder::union, SqlBuilder::unionAll, SqlBuilder::intersect,
                    SqlBuilder::except, SqlBuilder::minus);
            final List<String> operators = Arrays.asList("UNION", "UNION ALL", "INTERSECT", "EXCEPT", "MINUS");

            for (int i = 0; i < setOperations.size(); i++) {
                final SqlBuilder parent = NSC.select("id").from("current_records").where(Filters.eq("status", "current"));
                final SqlBuilder child = NSC.select("id").from("archived_records").where(Filters.eq("status", "archived"));
                setOperations.get(i).accept(parent, child);
                final SP result = parent.orderBy("id").limit(5).build();

                assertEquals("SELECT id FROM current_records WHERE status = :status " + operators.get(i)
                        + " SELECT id FROM archived_records WHERE status = :status_2 ORDER BY id LIMIT 5", result.query());
                assertEquals(Arrays.asList("current", "archived"), result.parameters());
            }
        }

        @Test
        void testSetOperationsRejectClosedChildrenWithoutConsumingParent() {
            final List<BiConsumer<SqlBuilder, SqlBuilder>> operations = Arrays.asList(SqlBuilder::union, SqlBuilder::unionAll, SqlBuilder::intersect,
                    SqlBuilder::except, SqlBuilder::minus);
            for (final BiConsumer<SqlBuilder, SqlBuilder> operation : operations) {
                for (int variant = 0; variant < 3; variant++) {
                    final SqlBuilder parent = PSC.select("id").from("current_records");
                    final SqlBuilder child = PSC.select("id").from("archived_records");
                    if (variant == 1) {
                        child.limit(1);
                    } else if (variant == 2) {
                        child.union("SELECT id FROM older_records");
                    }
                    child.build();

                    // Terminal/compound metadata survives build(). It must not change the lifecycle
                    // exception into an isolation error, and the rejected call must leave the parent usable.
                    assertThrows(IllegalStateException.class, () -> operation.accept(parent, child), "Closed child variant: " + variant);
                    assertEquals("SELECT id FROM current_records", parent.build().query());
                }
            }
        }

        @Test
        void testSiblingSetOperationsRejectUnisolatedBranchClausesWithoutConsumingBuilders() {
            final List<BiConsumer<SqlBuilder, SqlBuilder>> setOperations = Arrays.asList(SqlBuilder::union, SqlBuilder::unionAll, SqlBuilder::intersect,
                    SqlBuilder::except, SqlBuilder::minus);

            for (final BiConsumer<SqlBuilder, SqlBuilder> setOperation : setOperations) {
                for (int variant = 0; variant < 10; variant++) {
                    final SqlBuilder parent = PSC.select("id").from("current_records");
                    final SqlBuilder child = PSC.select("id").from("archived_records");

                    switch (variant) {
                        case 0 -> child.limit(1);
                        case 1 -> child.orderBy("id");
                        case 2 -> child.union("SELECT id FROM older_records");
                        case 3 -> child.unionSelect(Arrays.asList("id")).from("older_records");
                        case 4 -> child.forUpdate();
                        case 5 -> child.offset(1);
                        case 6 -> child.offsetRows(1);
                        case 7 -> child.fetchFirstRows(1);
                        case 8 -> child.fetchNextRows(1);
                        default -> child.except("SELECT id FROM older_records");
                    }

                    final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> setOperation.accept(parent, child));
                    assertTrue(failure.getMessage().contains("isolated right-hand query"), failure.getMessage());
                    assertTrue(failure.getMessage().contains("combined result"), failure.getMessage());
                    assertTrue(failure.getMessage().contains("apply those clauses to the parent"), failure.getMessage());
                    assertTrue(failure.getMessage().contains("preserve child-local scope"), failure.getMessage());
                    assertEquals("SELECT id FROM current_records", parent.build().query());
                    assertTrue(child.build().query().startsWith("SELECT id FROM archived_records"));
                }
            }
        }

        @Test
        void testSingleBeanInsertRejectsNoRemainingValuesBeforeAllocatingBuilder() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();

            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.insert(new AllNullBatchEntity()));

            assertTrue(ex.getMessage().contains("No insertable values remain"), ex.getMessage());
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "Rejected input must not allocate a builder whose pooled StringBuilder cannot be released");
        }

        @Test
        void testSqlBuilder_NSC_simpleSelect() {
            String sql = NSC.select("id", "name").from("account").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        void testSqlBuilder_PSC_deleteFrom() {
            String sql = PSC.deleteFrom("account").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM account"));
        }

        @Test
        void testSqlBuilder_PSC_insertIntoValues() {
            String sql = PSC.insert("id", "name", "email").into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testSqlBuilder_PSC_rejectsLimitAfterOffset() {
            final SqlBuilder builder = PSC.select("id", "name").from("users").offset(10);

            assertThrows(IllegalStateException.class, () -> builder.limit(5));
            assertEquals("SELECT id, name FROM users OFFSET 10", builder.build().query());
        }

        @Test
        void testSqlBuilder_PSC_selectDistinct() {
            String sql = PSC.select("DISTINCT department").from("employees").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithAndOr() {
            String sql = PSC.select("id", "name")
                    .from("users")
                    .where(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.gt("age", 18), Filters.eq("verified", true))))
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithBetween() {
            String sql = PSC.select("id", "name", "age").from("users").where(Filters.between("age", 18, 65)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("BETWEEN"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithCrossJoin() {
            String sql = PSC.select("u.id", "p.name").from("users u").crossJoin("products p").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("CROSS JOIN"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithFullJoin() {
            String sql = PSC.select("u.id", "o.total").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FULL JOIN"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithGroupBy() {
            String sql = PSC.select("department", "COUNT(*) AS cnt").from("employees").groupBy("department").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("GROUP BY"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithHaving() {
            String sql = PSC.select("department", "COUNT(*) AS cnt")
                    .from("employees")
                    .groupBy("department")
                    .having(Filters.expr("COUNT(*) > 5"))
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("HAVING"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithIn() {
            String sql = PSC.select("id", "name").from("users").where(Filters.in("status", Arrays.asList("active", "pending"))).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("IN"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithInnerJoin() {
            String sql = PSC.select("u.id", "o.total").from("users u").innerJoin("orders o").on("u.id = o.user_id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INNER JOIN"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithIsNull() {
            String sql = PSC.select("id", "name").from("users").where(Filters.isNull("deleted_at")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("IS NULL"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithJoin() {
            String sql = PSC.select("u.id", "u.name", "o.total").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("LEFT JOIN"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithLike() {
            String sql = PSC.select("id", "name", "email").from("users").where(Filters.like("email", "%@company.com")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("LIKE"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithLimit() {
            String sql = PSC.select("id", "name").from("users").limit(10).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithOrderBy() {
            String sql = PSC.select("id", "name").from("users").orderBy("name").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithRightJoin() {
            String sql = PSC.select("u.id", "o.total").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("RIGHT JOIN"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithUnion() {
            String sql = PSC.select("id", "name").from("active_users").union(PSC.select("id", "name").from("archived_users")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UNION"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithUnionAll() {
            String sql = PSC.select("id", "name").from("active_users").unionAll(PSC.select("id", "name").from("temp_users")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UNION ALL"));
        }

        @Test
        void testSqlBuilder_PSC_selectWithWhere() {
            String sql = PSC.select("id", "name").from("users").where(Filters.gt("age", 18)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM users"));
        }

        @Test
        void testSqlBuilder_PSC_simpleSelect() {
            String sql = PSC.select("id", "name").from("account").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        void testSqlBuilder_PSC_updateWithConditions() {
            String sql = PSC.update("account").set(Arrays.asList("name", "status")).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("SET"));
        }

        @Test
        void testSqlBuilderWithDifferentNamingPolicies() {
            // Test that different SqlBuilder implementations use correct naming policies

            // Snake case
            assertTrue(PSC.select("firstName").from("userAccount").build().query().contains("first_name"));
            assertTrue(NSC.select("firstName").from("userAccount").build().query().contains("first_name"));

            // Upper case
            assertTrue(Dsl.PAC.select("firstName").from("userAccount").build().query().contains("FIRST_NAME"));
            assertTrue(Dsl.NAC.select("firstName").from("userAccount").build().query().contains("FIRST_NAME"));

            // Lower camel case
            assertTrue(Dsl.PLC.select("first_name").from("user_account").build().query().contains("firstName"));
            assertTrue(Dsl.NLC.select("first_name").from("user_account").build().query().contains("firstName"));
        }

        @Test
        void testUpdateEntityClassExclusionsApplyToDeferredSetTemplate() {
            assertEquals("UPDATE account SET id = ?, first_name = ?, last_name = ?, email = ? WHERE id = ?",
                    PSC.update(JavadocAccount.class).where(Filters.equal("id", 1)).build().query());
            assertEquals("UPDATE account SET id = ?, first_name = ?, last_name = ? WHERE id = ?",
                    PSC.update(JavadocAccount.class, Collections.singleton("email")).where(Filters.equal("id", 1)).build().query());
        }

        @Test
        void testUsingAcceptsQuotedDotsInsideColumnNames() {
            // A dot inside a quoted identifier belongs to the name, so it is not a qualifier.
            assertEquals("SELECT * FROM test_account a JOIN test_account b USING (\"weird.col\")",
                    PSC.select("*").from(Account.class, "a").join(Account.class, "b").using("(\"weird.col\")").build().query());
        }
    }

    @Nested
    class NamingAndMetadata {
        @Test
        void testAliasedNestedPropertyPreservesExistingColumnQualifier() {
            final SP sp = PSC.select("h.profile.name")
                    .from(QualifiedProfileHolder.class, "h")
                    .join(QualifiedProfile.class, "p")
                    .on("h.profile_id = p.id")
                    .where(Filters.eq("h.profile.name", "Ada"))
                    .build();

            assertEquals("SELECT p.display_name AS \"h.profile.name\" FROM profile_holders h JOIN profiles p ON h.profile_id = p.id WHERE p.display_name = ?",
                    sp.query());
            assertEquals(Collections.singletonList("Ada"), sp.parameters());
        }

        @Test
        void testAliaspropColumnNameMap() {
            // Test query with table aliases and property column name mapping
            String sql = PSC.select("a.firstName", "o.orderNumber").from(Account.class, "a").join(Order.class, "o").on("a.id = o.userId").build().query();

            assertTrue(sql.contains("a.first_name AS \"a.firstName\""));
            assertTrue(sql.contains("o.order_number AS \"o.orderNumber\""));
        }

        @Test
        void testCamelCaseKeepsCollationNameAfterCollate() {
            // Regression: CAMEL_CASE converted collation names like columns (utf8mb4_unicode_ci -> utf8mb4UnicodeCi).
            final String sql = PLC.select("id")
                    .from("t")
                    .where("first_name COLLATE utf8mb4_bin = 'x'")
                    .orderBy("last_name COLLATE utf8mb4_unicode_ci")
                    .build()
                    .query();

            assertTrue(sql.contains(" utf8mb4_bin = 'x'"), sql);
            assertTrue(sql.endsWith(" utf8mb4_unicode_ci"), sql);
        }

        @Test
        void testColumnNameWithSpecialCharacters() {
            String sql = PSC.select("user.name", "COUNT(*) as total").from("users").groupBy("user.name").build().query();

            assertTrue(sql.contains("user.name"));
            assertTrue(sql.contains(" COUNT(*) AS total"));
            assertTrue(sql.contains("GROUP BY user.name"));
        }

        // Regression: the sort-direction rewrite sent every WHERE / GROUP BY / ORDER BY column straight to a private
        // overload, so a subclass override of the protected appendColumnName hooks was silently skipped.
        @Test
        void testColumnRenderingDispatchesThroughProtectedAppendColumnNameHooks() {
            class RecordingBuilder extends SqlBuilder {
                int singleArgCalls;
                int fullCalls;

                RecordingBuilder() {
                    super(PSC.sqlDialect());
                    _isForConditionOnly = true;
                    setEntityClass(com.landawn.abacus.query.entity.Account.class);
                }

                @Override
                protected void appendColumnName(final String propName) {
                    singleArgCalls++;
                    super.appendColumnName(propName);
                }

                @Override
                protected void appendColumnName(final Class<?> entityClass, final com.landawn.abacus.parser.ParserUtil.BeanInfo entityInfo,
                        final ImmutableMap<String, QueryUtil.ColumnInfo> propColumnNameMap, final String tableAlias, final String propName,
                        final String propAlias, final boolean withClassAlias, final String classAlias, final boolean isForSelect,
                        final boolean quotePropAlias) {
                    fullCalls++;
                    super.appendColumnName(entityClass, entityInfo, propColumnNameMap, tableAlias, propName, propAlias, withClassAlias, classAlias, isForSelect,
                            quotePropAlias);
                }
            }

            final Map<String, SortDirection> directions = new LinkedHashMap<>();
            directions.put("firstName", SortDirection.ASC);
            directions.put("lastName", SortDirection.DESC);

            final List<Function<RecordingBuilder, SqlBuilder>> calls = Arrays.asList(b -> b.append(Filters.eq("firstName", 1)), b -> b.orderBy("firstName"),
                    b -> b.orderByDesc("firstName"), b -> b.orderBy("firstName", SortDirection.ASC), b -> b.groupByDesc("firstName"),
                    b -> b.orderByAsc("firstName", "lastName"), b -> b.groupBy(Arrays.asList("firstName", "lastName"), SortDirection.DESC),
                    b -> b.orderBy(directions), b -> b.groupBy(directions));
            final int[] expectedColumns = { 1, 1, 1, 1, 1, 2, 2, 2, 2 };
            final String[] expectedSql = { "first_name = ?", "ORDER BY first_name", "ORDER BY first_name DESC", "ORDER BY first_name ASC",
                    "GROUP BY first_name DESC", "ORDER BY first_name ASC, last_name ASC", "GROUP BY first_name DESC, last_name DESC",
                    "ORDER BY first_name ASC, last_name DESC", "GROUP BY first_name ASC, last_name DESC" };

            for (int i = 0; i < calls.size(); i++) {
                final RecordingBuilder builder = new RecordingBuilder();
                final String sql = calls.get(i).apply(builder).build().query();
                assertEquals(expectedSql[i], sql);
                assertEquals(expectedColumns[i], builder.singleArgCalls, sql);
                assertEquals(expectedColumns[i], builder.fullCalls, sql);
            }
        }

        @Test
        void testDanglingIntoSegmentThrowsForAliasesAndMultiSelects() {
            // select(Map).into() without the follow-up from() used to emit a truncated
            // "INSERT INTO account (first_name)" with no SELECT or VALUES part.
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fn");
            assertThrows(IllegalStateException.class, () -> PSC.select(aliases).into("account").build());

            // Same for select(List<Selection>).into().
            List<Selection> selections = Arrays.asList(Selection.builder(Account.class).build());
            assertThrows(IllegalStateException.class, () -> PSC.select(selections).into("account").build());

            // Completing the INSERT ... SELECT still works.
            assertEquals("INSERT INTO backup (first_name) SELECT first_name AS \"fn\" FROM account",
                    PSC.select(aliases).into("backup").from("account").build().query());
        }

        @Test
        void testDefaultDialectRecognizesAssignmentsUnderEitherBackslashReading() {
            final Dsl defaultDialect = setExpressionDsl(null);
            final Dsl standardDialect = setExpressionDsl("PostgreSQL");
            final Dsl mysql = setExpressionDsl("MySQL");

            // A standard quoted column can end in a literal backslash; the MySQL reading leaves its quote open.
            final String standardAssignment = "\"metric\\\" = 1";
            assertSetExpression(defaultDialect, standardAssignment, standardAssignment);
            assertSetExpression(standardDialect, standardAssignment, standardAssignment);
            assertSetExpression(mysql, standardAssignment, standardAssignment + " = ?");

            // These lexical fixtures require the second default reading. They are not validated SQL assignment targets.
            // The first has balanced standard quotes; the second leaves a standard quote open before reaching '='.
            for (final String expression : List.of("'a\\'b' = 'c\\'d'", "'a\\'b' = 1", "\"a\\\"b\" = 1")) {
                assertSetExpression(defaultDialect, expression, expression);
                assertSetExpression(standardDialect, expression, expression + " = ?");
                assertSetExpression(mysql, expression, expression);
            }

            final String quotedColumn = "\"metric=\\\"";
            assertSetExpression(defaultDialect, quotedColumn, quotedColumn + " = ?");
        }

        @Test
        void testEntityClassSelectAliasesEveryColumnBackToItsPropertyName() {
            // Pins the Dsl Javadoc examples for select(Class), selectFrom(Class) and selectFrom(Class, alias):
            // with a naming policy other than NO_CHANGE, EVERY selected column of an entity-class SELECT is
            // aliased back to its property name - including columns like id/email whose rendered column name
            // already equals the property name. (The string-based select(String...) overload, by contrast,
            // only aliases names the naming policy actually changed.)
            assertEquals("SELECT id AS \"id\", first_name AS \"firstName\", last_name AS \"lastName\", email AS \"email\" FROM account",
                    PSC.select(JavadocAccount.class).from("account").build().query());

            assertEquals("SELECT id AS \"id\", first_name AS \"firstName\", last_name AS \"lastName\", email AS \"email\" FROM account WHERE email = ?",
                    PSC.selectFrom(JavadocAccount.class).where(Filters.eq("email", "john.doe@example.com")).build().query());

            assertEquals(
                    "SELECT a.id AS \"id\", a.first_name AS \"firstName\", a.last_name AS \"lastName\", a.email AS \"email\" FROM account a WHERE a.email = ?",
                    PSC.selectFrom(JavadocAccount.class, "a").where(Filters.eq("a.email", "john.doe@example.com")).build().query());
        }

        @Test
        void testEntityWithNoTableAnnotation() {
            // Test with a simple class without @Table annotation
            class SimpleEntity {
                private Long id;
                private String name;

                public Long getId() {
                    return id;
                }

                public void setId(Long id) {
                    this.id = id;
                }

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }
            }

            String tableName = AbstractQueryBuilder.getTableName(SimpleEntity.class, NamingPolicy.SNAKE_CASE);
            assertEquals("simple_entity", tableName);
        }

        @Test
        void testExplicitMappedSelectAliasesAreHonoredWithNoChangeNamingPolicy() {
            for (final Dsl dsl : new Dsl[] { PSB, NSB, MSB }) {
                assertEquals("SELECT account_status AS \"currentStatus\" FROM test_account",
                        dsl.select(Collections.singletonMap("status", "currentStatus")).from(Account.class).build().query());
                assertEquals("SELECT a.account_status AS \"currentStatus\" FROM test_account a",
                        dsl.select(Collections.singletonMap("a.status", "currentStatus")).from(Account.class, "a").build().query());
                assertEquals("SELECT account_status AS currentStatus FROM test_account",
                        dsl.select("status AS currentStatus").from(Account.class).build().query());
                assertEquals("SELECT a.account_status AS currentStatus FROM test_account a",
                        dsl.select("a.status AS currentStatus").from(Account.class, "a").build().query());
            }
        }

        @Test
        void testFormatColumnNameEdgeCases() {
            // Test edge cases for column name formatting
            assertEquals("id", AbstractQueryBuilder.normalizeColumnName("id", NamingPolicy.SNAKE_CASE));
            assertEquals("ID", AbstractQueryBuilder.normalizeColumnName("id", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("user_name123", AbstractQueryBuilder.normalizeColumnName("userName123", NamingPolicy.SNAKE_CASE));
            assertEquals("USER_NAME123", AbstractQueryBuilder.normalizeColumnName("userName123", NamingPolicy.SCREAMING_SNAKE_CASE));
        }

        @Test
        void testFromAliasScannerSkipsQuotesCommentsAndNestedParentheses() {
            final String subquerySql = PSC.select(Account.class).from("(SELECT ')' AS marker FROM account /* ) */) e").build().query();
            final String doubleQuotedTableSql = PSC.select(Account.class).from("\"Order Details\" od").build().query();
            final String bracketQuotedTableSql = PSC.select(Account.class).from("[Order Details] AS od").build().query();
            final String commentSeparatedAliasSql = PSC.select(Account.class).from("account -- AS ignored\n e").build().query();
            final String trailingCommentSql = PSC.select(Account.class).from("account /* no alias */").build().query();

            assertTrue(subquerySql.startsWith("SELECT e."), subquerySql);
            assertTrue(subquerySql.endsWith(" FROM (SELECT ')' AS marker FROM account /* ) */) e"), subquerySql);
            assertTrue(doubleQuotedTableSql.startsWith("SELECT od."), doubleQuotedTableSql);
            assertTrue(doubleQuotedTableSql.endsWith(" FROM \"Order Details\" od"), doubleQuotedTableSql);
            assertTrue(bracketQuotedTableSql.startsWith("SELECT od."), bracketQuotedTableSql);
            assertTrue(bracketQuotedTableSql.endsWith(" FROM [Order Details] AS od"), bracketQuotedTableSql);
            assertTrue(commentSeparatedAliasSql.startsWith("SELECT e."), commentSeparatedAliasSql);
            assertTrue(commentSeparatedAliasSql.endsWith(" FROM account -- AS ignored\n e"), commentSeparatedAliasSql);
            assertFalse(trailingCommentSql.startsWith("SELECT /*"), trailingCommentSql);
            assertTrue(trailingCommentSql.endsWith(" FROM account /* no alias */"), trailingCommentSql);
        }

        @Test
        void testFromEntityClass() {
            String sql = PSC.select("*").from(Account.class).build().query();
            assertNotNull(sql);
        }

        @Test
        void testFromRejectsNullOrBlankTableNames() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from((String) null));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(new String[] { "users", null }));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(new String[] { "users", "   " }));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(Arrays.asList("users", null)));
            assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(Arrays.asList("users", "  ")));
        }

        @Test
        void testFromWithAlias() {
            String sql = PSC.select("u.*").from("users u").build().query();
            assertTrue(sql.contains("FROM users u"));
        }

        @Test
        void testFromWithAsAliasAndEntityClass() {
            String sql = PSC.select("firstName").from("test_account AS a", Account.class).build().query();
            assertTrue(sql.contains("a.first_name"));
            assertTrue(sql.contains("FROM test_account AS a"));
        }

        /**
         * Bug: the static full-select-parts cache was keyed only by naming policy + entity class, so two
         * dialects sharing a naming policy but using different identifier quotes poisoned each other's
         * cached SELECT list (e.g. PSC emitting backtick-quoted aliases after a backtick dialect ran first).
         */
        @Test
        void testFullSelectPartsCacheIsDialectQuoteAware() {
            Dsl backtickDsl = Dsl.forDialect(SqlDialect.builder()
                    .namingPolicy(NamingPolicy.SNAKE_CASE)
                    .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
                    .identifierQuote(SqlDialect.IdentifierQuote.BACKTICK)
                    .build());

            String backtickSql = backtickDsl.selectFrom(BfxQuoted.class).build().query();
            assertEquals("SELECT id AS `id`, first_name AS `firstName` FROM bfx_quoted", backtickSql);

            String pscSql = PSC.selectFrom(BfxQuoted.class).build().query();
            assertEquals("SELECT id AS \"id\", first_name AS \"firstName\" FROM bfx_quoted", pscSql);

            // Render both again to verify neither direction poisoned the other's cache.
            assertEquals(backtickSql, backtickDsl.selectFrom(BfxQuoted.class).build().query());
            assertEquals(pscSql, PSC.selectFrom(BfxQuoted.class).build().query());
        }

        @Test
        void testGetSubEntityPropNames() {
            ImmutableSet<String> subProps = AbstractQueryBuilder.getSubEntityPropNames(Account.class);
            assertNotNull(subProps);
            assertTrue(subProps.isEmpty()); // Account has no sub-entities
        }

        @Test
        void testGetTableAlias() {
            assertEquals("", AbstractQueryBuilder.tableAlias(Account.class));
            assertEquals("o", AbstractQueryBuilder.tableAlias(Order.class));
        }

        @Test
        void testGetTableAliasOrName() {
            assertEquals("test_account", AbstractQueryBuilder.tableAliasOrName(Account.class, NamingPolicy.SNAKE_CASE));
            assertEquals("o", AbstractQueryBuilder.tableAliasOrName(Order.class, NamingPolicy.SNAKE_CASE));

            assertEquals("custom", AbstractQueryBuilder.tableAliasOrName("custom", Account.class, NamingPolicy.SNAKE_CASE));
            assertEquals("o", AbstractQueryBuilder.tableAliasOrName("", Order.class, NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testGetTableAliasWithSpecifiedAlias() {
            assertEquals("a", AbstractQueryBuilder.tableAlias("a", Account.class));
            assertEquals("", AbstractQueryBuilder.tableAlias("", Account.class));
            assertEquals("", AbstractQueryBuilder.tableAlias(null, Account.class));
        }

        // Static method tests

        @Test
        void testGetTableName() {
            assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.SNAKE_CASE));
            assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.CAMEL_CASE));
            assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.NO_CHANGE));

            assertEquals("user_order", AbstractQueryBuilder.getTableName(Order.class, NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testGroupByMultipleColumns() {
            String sql = PSC.select("year", "month", "COUNT(*)").from("sales").groupBy("year", "month").build().query();
            assertTrue(sql.contains("GROUP BY"));
        }

        @Test
        void testIntoWithEntityAndTableName() {
            String sql = PSC.insert("firstName", "lastName").into("custom_table", Account.class).build().query();

            assertEquals("INSERT INTO custom_table (first_name, last_name) VALUES (?, ?)", sql);
        }

        //    @Test
        //    void testIntoWithInvalidOperation() {
        //        SqlBuilder builder = PSC.select("*").from("account");
        //        assertThrows(RuntimeException.class, () -> builder.into("account"));
        //    }

        @Test
        void testIntoWithoutColumns() {
            assertThrows(RuntimeException.class, () -> PSC.update("account").into("account"));
        }

        @Test
        void testKebabCaseNamingPolicyIsRejected() {
            // Regression: KEBAB_CASE rendered unquoted hyphenated identifiers ("first-name" = first - name), so every
            // statement such a Dsl produced was invalid SQL.
            assertThrows(IllegalArgumentException.class,
                    () -> Dsl.forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.KEBAB_CASE).sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL).build()));

            assertEquals("SELECT FirstName AS \"firstName\" FROM account",
                    Dsl.forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.UPPER_CAMEL_CASE).sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL).build())
                            .select("firstName")
                            .from("account")
                            .build()
                            .query());
        }

        @Test
        void testKebabCaseRejectionMessageAndNullNamingPolicyDefault() {
            // Covers the KEBAB_CASE rejection message and the null naming policy default (snake case).
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Dsl.forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.KEBAB_CASE).sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL).build()));
            assertTrue(ex.getMessage().contains("KEBAB_CASE"), ex.getMessage());

            assertEquals("SELECT first_name AS \"firstName\" FROM account",
                    Dsl.forDialect(SqlDialect.builder().build()).select("firstName").from("account").build().query());
        }

        @Test
        void testLCSB_SelectFromDefaultEntityClass() {
            String sql = Dsl.LCSB.selectFrom(Account.class).build().query();

            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testMAC_SelectOverloadsWithTableAliases() {
            Set<String> excluded = Collections.singleton("lastName");
            String selectSql = Dsl.MAC.select(Account.class, "a1", "account1", excluded, Account.class, "a2", "account2", excluded)
                    .from("ACCOUNT1 a1, ACCOUNT2 a2")
                    .build()
                    .query();
            String selectFromSql = Dsl.MAC.selectFrom(Account.class, "a1", "account1", Account.class, "a2", "account2").build().query();

            assertTrue(selectSql.contains("FROM ACCOUNT1 a1, ACCOUNT2 a2"));
            assertTrue(selectFromSql.contains("FROM"));
        }

        @Test
        void testMultiColumnInCondition_BeanRows() {
            Account row = new Account();
            row.setFirstName("John");
            row.setLastName("Doe");

            AbstractQueryBuilder.SP sp = PSC.select("*").from("users").where(Filters.in(Arrays.asList("firstName", "lastName"), Arrays.asList(row))).build();

            assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN ((?, ?))", sp.query());
            assertEquals(Arrays.asList("John", "Doe"), sp.parameters());
        }

        @Test
        void testMultiColumnInCondition_MapRows() {
            AbstractQueryBuilder.SP sp = PSC.select("*")
                    .from("users")
                    .where(Filters.in(Arrays.asList("firstName", "lastName"),
                            Arrays.asList(N.asMap("firstName", "John", "lastName", "Doe"), N.asMap("firstName", "Jane", "lastName", "Roe"))))
                    .build();

            assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN ((?, ?), (?, ?))", sp.query());
            assertEquals(Arrays.asList("John", "Doe", "Jane", "Roe"), sp.parameters());
        }

        @Test
        void testMultiColumnInCondition_Named() {
            String sql = NSC.select("*")
                    .from("users")
                    .where(Filters.in(Arrays.asList("firstName", "lastName"), Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe"))))
                    .build()
                    .query();

            assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN ((:firstName1, :lastName1), (:firstName2, :lastName2))", sql);
        }

        @Test
        void testMultiColumnInCondition_Raw() {
            String sql = Dsl.SCSB.select("*")
                    .from("users")
                    .where(Filters.in(Arrays.asList("firstName", "lastName"), Arrays.asList(Arrays.asList("John", "Doe"))))
                    .build()
                    .query();

            assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN (('John', 'Doe'))", sql);
        }

        @Test
        void testMultiSelectDoesNotAddSubEntityTableWhenAllItsPropertiesAreExcluded() {
            final Selection holder = Selection.builder(QualifiedProfileHolder.class)
                    .tableAlias("h")
                    .includeSubEntityProperties(true)
                    .excludedPropNames(Collections.singleton("profile.name"))
                    .build();
            final Selection profile = Selection.builder(QualifiedProfile.class).tableAlias("p").build();

            assertEquals("SELECT p.display_name AS \"name\" FROM profile_holders h, profiles p",
                    PSC.selectFrom(Arrays.asList(holder, profile)).build().query());
        }

        /**
         * Bug: {@code Dsl.selectFrom(List<Selection>)} hardcoded SNAKE_CASE for the generated FROM clause,
         * so non-snake-case DSLs (PAC/PLC/...) emitted a FROM clause inconsistent with the SELECT list.
         */
        @Test
        void testMultiSelectFromUsesDialectNamingPolicyForFromClause() {
            String pacSql = Dsl.PAC.selectFrom(BfxUser.class, "u", "user", BfxAddress.class, "a", "address").build().query();
            assertTrue(pacSql.endsWith(" FROM BFX_USER u, BFX_ADDRESS a"), pacSql);

            String plcSql = Dsl.PLC.selectFrom(BfxUser.class, "u", "user", BfxAddress.class, "a", "address").build().query();
            assertTrue(plcSql.endsWith(" FROM bfxUser u, bfxAddress a"), plcSql);

            // snake_case DSL behavior is unchanged.
            String pscSql = PSC.selectFrom(BfxUser.class, "u", "user", BfxAddress.class, "a", "address").build().query();
            assertTrue(pscSql.endsWith(" FROM bfx_user u, bfx_address a"), pscSql);
        }

        // Test different naming policies
        @Test
        void testNamingPolicies() {
            // Snake case (PSC)
            String sql = PSC.select("firstName", "lastName").from("userAccount").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("userAccount"));

            // Upper case (PAC)
            sql = Dsl.PAC.select("firstName", "lastName").from("userAccount").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("userAccount"));

            // Lower camel case (PLC)
            sql = Dsl.PLC.select("first_name", "last_name").from("user_account").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("user_account"));
        }

        @Test
        void testNamingPolicyConversion() {
            // Test normalize column name
            assertEquals("first_name", AbstractQueryBuilder.normalizeColumnName("firstName", NamingPolicy.SNAKE_CASE));
            assertEquals("FIRST_NAME", AbstractQueryBuilder.normalizeColumnName("firstName", NamingPolicy.SCREAMING_SNAKE_CASE));
            assertEquals("firstName", AbstractQueryBuilder.normalizeColumnName("first_name", NamingPolicy.CAMEL_CASE));
            assertEquals("firstName", AbstractQueryBuilder.normalizeColumnName("firstName", NamingPolicy.NO_CHANGE));

            // SQL keywords should not be converted
            assertEquals("SELECT", AbstractQueryBuilder.normalizeColumnName("SELECT", NamingPolicy.SNAKE_CASE));
            assertEquals("FROM", AbstractQueryBuilder.normalizeColumnName("FROM", NamingPolicy.SNAKE_CASE));
        }

        @Test
        void testOrderByMultipleColumns() {
            String sql = PSC.select("*").from("users").orderBy("last_name", "first_name").build().query();
            assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        void testPAC_SelectOverloadsWithExcludedProperties() {
            Set<String> excluded = Collections.singleton("lastName");
            String selectSql = Dsl.PAC.select(Account.class, excluded).from("USER_ACCOUNT").build().query();
            String selectFromSql = Dsl.PAC.selectFrom(Account.class, "ua", true, excluded).build().query();

            assertTrue(selectSql.contains("SELECT"));
            assertTrue(selectSql.contains("FROM USER_ACCOUNT"));
            assertTrue(selectFromSql.contains("FROM"));
            assertTrue(selectFromSql.contains("ua"));
        }

        @Test
        void testPLC_SelectOverloadsWithTwoEntityClasses() {
            String sql = Dsl.PLC.select(Account.class, "a1", "account1", Account.class, "a2", "account2").from("account1 a1, account2 a2").build().query();

            assertTrue(sql.contains("FROM account1 a1, account2 a2"));
        }

        /**
         * Bug: the {@code dslCache} field was declared <i>after</i> the predefined {@code Dsl} constants
         * whose initializers call {@link Dsl#forDialect(SqlDialect)}. Static field initializers run in
         * textual order, so {@code dslCache} was still {@code null} when the first constant ({@code PSB})
         * ran {@code forDialect(...)}, throwing {@code NullPointerException} -> {@code ExceptionInInitializerError}
         * during class init and leaving every later access failing with
         * {@code NoClassDefFoundError: Could not initialize class ...Dsl}. Fix: declare {@code dslCache}
         * before the constants. This test pins both the initialization and the caching contract.
         */
        @Test
        void testPredefinedDslConstantsInitializeAndForDialectCaches() {
            // Touch every predefined constant: each must initialize (non-null) and be usable.
            for (final Dsl dsl : new Dsl[] { Dsl.PSB, Dsl.PSC, Dsl.PAC, Dsl.PLC, Dsl.NSB, Dsl.NSC, Dsl.NAC, Dsl.NLC, Dsl.SCSB, Dsl.ACSB, Dsl.LCSB, Dsl.MSB,
                    Dsl.MSC, Dsl.MAC, Dsl.MLC }) {
                assertNotNull(dsl);
                assertNotNull(dsl.sqlDialect());
            }

            // forDialect returns the canonical cached instance for a dialect equal to a predefined one...
            final SqlDialect pscDialect = SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL).build();
            assertSame(Dsl.PSC, Dsl.forDialect(pscDialect));

            // ...and a fresh, non-null instance for a dialect that is not predefined.
            final SqlDialect customDialect = SqlDialect.builder()
                    .namingPolicy(NamingPolicy.SNAKE_CASE)
                    .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
                    .identifierQuote(SqlDialect.IdentifierQuote.BACKTICK)
                    .build();
            final Dsl custom = Dsl.forDialect(customDialect);
            assertNotNull(custom);
            assertNotSame(Dsl.PSC, custom);
        }

        @Test
        void testPropToColumnInfoMap() {
            ImmutableMap<String, QueryUtil.ColumnInfo> map = AbstractQueryBuilder.propToColumnInfoMap(Account.class, NamingPolicy.SNAKE_CASE);

            assertNotNull(map);
            assertTrue(map.containsKey("firstName"));
            assertTrue(map.containsKey("status"));

            assertEquals("first_name", map.get("firstName").columnName());
            assertEquals("account_status", map.get("status").columnName()); // Should use @Column annotation
        }

        @Test
        void testQualifiedColumnMappingDoesNotRepeatAliasInRawExpression() {
            final SP sp = PSC.select("LOWER(p.name)").from(QualifiedProfile.class, "p").where("p.name = ?").build();

            assertEquals("SELECT LOWER(p.display_name) AS \"LOWER(p.name)\" FROM profiles p WHERE p.display_name = ?", sp.query());
            assertTrue(sp.parameters().isEmpty());
        }

        @Test
        void testQualifiedColumnMappingDoesNotRepeatAliasInSelectAndPredicate() {
            final SP sp = PSC.select("p.name").from(QualifiedProfile.class, "p").where(Filters.eq("p.name", "Ada")).orderBy("p.name").build();

            assertEquals("SELECT p.display_name AS \"p.name\" FROM profiles p WHERE p.display_name = ? ORDER BY p.display_name", sp.query());
            assertEquals(Collections.singletonList("Ada"), sp.parameters());
        }

        @Test
        void testQualifiedWildcardIsNeverAutoAliased() {
            // "d.* AS "d.*"" is invalid SQL: a wildcard select item never gets the auto alias, whichever table it qualifies
            assertEquals("SELECT a.*, d.* FROM account a JOIN device d ON a.id = d.account_id",
                    PSC.select("a.*", "d.*").from("account a").join("device d").on("a.id = d.account_id").build().query());
            assertEquals("SELECT t.* FROM t", PSC.select("t.*").from("t").build().query());
            // trailing whitespace does not hide the wildcard from the guard
            assertFalse(PSC.select("a.*", "d.* ").from("account a").join("device d").on("a.id = d.account_id").build().query().contains(" AS "));
            assertEquals("SELECT d.* FROM account acc JOIN device d ON account.id = d.account_id",
                    PSC.select("d.*")
                            .from(com.landawn.abacus.query.entity.Account.class)
                            .join(com.landawn.abacus.query.entity.AccountDevice.class, "d")
                            .on("account.id = d.account_id")
                            .build()
                            .query());
            assertFalse(PAC.select("acc.*").from("account acc").build().query().contains(" AS "));
        }

        @Test
        void testQuotedEqualsColumnsWorkInsideMixedAssignmentLists() {
            final SP result = Dsl.PSC.update("measurements")
                    .set(List.of("\"metric=value\"", "otherValue = COALESCE(previousValue, 0)", "plainValue"))
                    .where(Filters.eq("id", 7))
                    .build();

            assertEquals("UPDATE measurements SET \"metric=value\" = ?, other_value = COALESCE(previous_value, 0), plain_value = ? WHERE id = ?",
                    result.query());
            assertEquals(List.of(7), result.parameters());
        }

        @Test
        void testQuotedEqualsSignsDoNotTurnColumnNamesIntoAssignments() {
            for (final SqlPolicy policy : SqlPolicy.values()) {
                final Dsl dsl = Dsl.forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(policy).build());
                for (final String column : List.of("\"metric=value\"", "`metric=value`", "[metric=value]", "\"metric\"\"=value\"", "`metric``=value`",
                        "[metric]]=value]")) {
                    final String placeholder = switch (policy) {
                        case RAW_SQL, PARAMETERIZED_SQL -> "?";
                        case NAMED_SQL -> ":metric_value";
                        case IBATIS_SQL -> "#{metric_value}";
                    };
                    final SP result = dsl.update("measurements").set(column).build();
                    assertEquals("UPDATE measurements SET " + column + " = " + placeholder, result.query(), policy + ": " + column);
                    assertTrue(result.parameters().isEmpty());
                }
            }
        }

        @Test
        void testRawFromBodyUsesOnlyPrimaryTableAliasForEntityColumns() {
            final String inlineJoinSql = PSC.select(Account.class).from("test_account a JOIN user_order o ON a.id = o.account_id").build().query();
            final String quotedCommaSql = PSC.select(Account.class).from("\"accounts,archive\" a").build().query();
            final String qualifiedKeywordTableSql = PSC.select(Account.class).from("schema.left l").build().query();
            final String straightJoinSql = PSC.select(Account.class).from("test_account a STRAIGHT_JOIN user_order o ON a.id = o.account_id").build().query();
            final String outerApplySql = PSC.select(Account.class).from("test_account a OUTER APPLY order_summary(a.id) o").build().query();

            assertTrue(inlineJoinSql.startsWith("SELECT a.id"), inlineJoinSql);
            assertFalse(inlineJoinSql.contains("o.account_id.id"), inlineJoinSql);
            assertTrue(inlineJoinSql.endsWith(" FROM test_account a JOIN user_order o ON a.id = o.account_id"), inlineJoinSql);
            assertTrue(quotedCommaSql.startsWith("SELECT a.id"), quotedCommaSql);
            assertTrue(quotedCommaSql.endsWith(" FROM \"accounts,archive\" a"), quotedCommaSql);
            assertTrue(qualifiedKeywordTableSql.startsWith("SELECT l.id"), qualifiedKeywordTableSql);
            assertTrue(qualifiedKeywordTableSql.endsWith(" FROM schema.left l"), qualifiedKeywordTableSql);
            assertTrue(straightJoinSql.startsWith("SELECT a.id"), straightJoinSql);
            assertTrue(straightJoinSql.endsWith(" FROM test_account a STRAIGHT_JOIN user_order o ON a.id = o.account_id"), straightJoinSql);
            assertTrue(outerApplySql.startsWith("SELECT a.id"), outerApplySql);
            assertTrue(outerApplySql.endsWith(" FROM test_account a OUTER APPLY order_summary(a.id) o"), outerApplySql);
        }

        @Test
        void testRealAssignmentsAfterQuotedColumnsKeepTheirExistingValues() {
            for (final SqlPolicy policy : SqlPolicy.values()) {
                final Dsl dsl = Dsl.forDialect(SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(policy).build());
                final SP result = dsl.update("measurements").set("\"metric=value\" = 'left=right'").build();
                assertEquals("UPDATE measurements SET \"metric=value\" = 'left=right'", result.query(), policy.toString());
                assertTrue(result.parameters().isEmpty());
            }
        }

        @Test
        void testSelectExpressionFindsOnlyTopLevelAsAlias() {
            // The CAST type is copied as written.
            assertEquals("SELECT CAST(created_at AS DATE) AS createdDay FROM events",
                    NSC.select("CAST(created_at AS DATE) AS createdDay").from("events").build().query());
            assertEquals("SELECT CONCAT(' AS ', name) AS label FROM events", NSC.select("CONCAT(' AS ', name) AS label").from("events").build().query());
            assertEquals("SELECT \"created AS date\" AS label FROM events", NSC.select("\"created AS date\" AS label").from("events").build().query());
        }

        @Test
        void testSelectExpressionRejectsUnsafeInlineAlias() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select("firstName AS bad--alias").from("account").build());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("firstName AS bad/*alias").from("account").build());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("firstName AS bad*/alias").from("account").build());
            // '#' starts a comment in MySQL and a quote opens a string literal; both would truncate or
            // corrupt the statement if emitted unquoted after AS.
            assertThrows(IllegalArgumentException.class, () -> PSC.select("firstName AS x#y").from("account").build());
            assertThrows(IllegalArgumentException.class, () -> PSC.select("firstName AS x'y").from("account").build());
        }

        @Test
        void testSelectFromRejectsUnsafeTableAlias() {
            for (final String alias : new String[] { "a--", "a#", "a /* x */", "a\nb" }) {
                assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(com.landawn.abacus.query.entity.Account.class, alias, false),
                        "alias: " + alias);
                assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(com.landawn.abacus.query.entity.Account.class, alias, true),
                        "alias: " + alias);
                assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(com.landawn.abacus.query.entity.Account.class, alias),
                        "alias: " + alias);
            }

            // Plain aliases, quoted aliases and the class-level @Table alias are unaffected.
            assertTrue(PSC.selectFrom(com.landawn.abacus.query.entity.Account.class, "a")
                    .where(Filters.eq("a.status", 1))
                    .build()
                    .query()
                    .contains("FROM account a WHERE a.status = ?"));
            assertTrue(PSC.select("*").from(com.landawn.abacus.query.entity.Account.class, "\"A\"").build().query().endsWith("FROM account \"A\""));
            assertTrue(PSC.selectFrom(com.landawn.abacus.query.entity.Account.class).build().query().startsWith("SELECT acc.id AS \"id\""));
        }

        @Test
        void testSelectFromWithExcludedProperties() {
            String sql = PSC.selectFrom(Account.class, N.asSet("createdDate", "lastModifiedDate")).build().query();

            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM test_account"));
            assertFalse(sql.contains("created_date"));
            assertFalse(sql.contains("last_modified_date"));
        }

        /**
         * Bug: {@code Dsl.selectFrom(Class, alias, includeSubEntityProperties, excludedPropNames)} hardcoded
         * SNAKE_CASE for the sub-entity FROM table names, producing FROM clauses that did not match the
         * sub-entity column prefixes in the SELECT list for non-snake-case DSLs.
         */
        @Test
        void testSelectFromWithSubEntitiesUsesDialectNamingPolicyForFromClause() {
            String pacSql = Dsl.PAC.selectFrom(BfxUser.class, true).build().query();
            assertTrue(pacSql.endsWith(" FROM BFX_USER, BFX_ADDRESS"), pacSql);

            String plcSql = Dsl.PLC.selectFrom(BfxUser.class, true).build().query();
            assertTrue(plcSql.endsWith(" FROM bfxUser, bfxAddress"), plcSql);

            String pscSql = PSC.selectFrom(BfxUser.class, true).build().query();
            assertTrue(pscSql.endsWith(" FROM bfx_user, bfx_address"), pscSql);
        }

        @Test
        void testSelectionsResolvingToNoPropertiesInTotalAreRejected() {
            final int activeBuilderCount = AbstractQueryBuilder.activeStringBuilderCounter.get();
            final Selection emptySelection = Selection.builder(AllNullBatchEntity.class).excludedPropNames(Collections.singleton("value")).build();

            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select(Arrays.asList(emptySelection)));

            assertTrue(ex.getMessage().contains("Selections must resolve to at least one property in total"), ex.getMessage());
            assertEquals(activeBuilderCount, AbstractQueryBuilder.activeStringBuilderCounter.get(),
                    "Rejected input must not allocate a builder whose pooled StringBuilder cannot be released");
        }

        @Test
        void testSelectMapRejectsUnsafeAliases() {
            Map<String, String> aliases = new LinkedHashMap<>();

            aliases.put("firstName", "bad\"alias");
            assertThrows(IllegalArgumentException.class, () -> PSC.select(aliases));

            aliases.clear();
            aliases.put("firstName", "bad`alias");
            assertThrows(IllegalArgumentException.class, () -> PSC.select(aliases));

            aliases.clear();
            aliases.put("firstName", "bad--alias");
            assertThrows(IllegalArgumentException.class, () -> PSC.select(aliases));

            aliases.clear();
            aliases.put("firstName", "bad#alias");
            assertThrows(IllegalArgumentException.class, () -> PSC.select(aliases));

            aliases.clear();
            aliases.put("firstName", "bad'alias");
            assertThrows(IllegalArgumentException.class, () -> PSC.select(aliases));
        }

        @Test
        void testSelectMultipleColumns() {
            String sql = PSC.select("id", "name", "email").from("users").build().query();
            assertTrue(sql.contains("SELECT id, name, email"));
        }

        @Test
        void testSelectSingleColumn() {
            String sql = PSC.select("name").from("users").build().query();
            assertEquals("SELECT name FROM users", sql);
        }

        @Test
        void testSelectSingleExpressionAppliesNamingPolicy() {
            // Documented behavior of Dsl.select(String): identifiers inside the expression ARE converted
            // by the naming policy; function names, keywords and the AS alias are preserved.
            assertEquals("SELECT first_name || ' ' || last_name AS fullName FROM account",
                    PSC.select("firstName || ' ' || lastName AS fullName").from("account").build().query());
        }

        @Test
        void testSelectSingleExpressionDoesNotSplitCommaListOrImplicitAlias() {
            // select(String) treats its argument as ONE select item: a comma list is not split and an alias
            // must be introduced with AS, otherwise the auto-alias heuristic aliases the whole text
            assertEquals("SELECT first_name, last_name AS \"firstName, lastName\" FROM account",
                    PSC.select("firstName, lastName").from("account").build().query());
            assertEquals("SELECT first_name fn AS \"firstName fn\" FROM account", PSC.select("firstName fn").from("account").build().query());
            assertEquals("SELECT first_name AS fn FROM account", PSC.select("firstName AS fn").from("account").build().query());
            assertEquals("SELECT first_name AS \"firstName\", last_name AS \"lastName\" FROM account",
                    PSC.select("firstName", "lastName").from("account").build().query());
            // identifiers after the first top-level AS are left unconverted, and the comma list is still not split
            assertEquals("SELECT first_name AS fn, lastName FROM account", PSC.select("firstName AS fn, lastName").from("account").build().query());
            // the paragraph is scoped to a policy that CHANGES the text: under NO_CHANGE the rendered text equals
            // the argument, so the auto-alias heuristic never fires and no alias is added at all
            assertEquals("SELECT firstName fn FROM account", PSB.select("firstName fn").from("account").build().query());
            assertEquals("SELECT firstName, lastName FROM account", PSB.select("firstName, lastName").from("account").build().query());
        }

        @Test
        void testSelectWithAlias() {
            String sql = PSC.select("first_name AS fname").from("users").build().query();
            assertTrue(sql.contains("AS fname"));
        }

        @Test
        void testSelectWithAliasMap() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "name");
            aliases.put("lastName", "surname");

            String sql = PSC.select(aliases).from("users").build().query();

            assertTrue(sql.contains("first_name AS \"name\""));
            assertTrue(sql.contains("last_name AS \"surname\""));
        }

        @Test
        void testSelectWithEntityClass() {
            String sql = PSC.select(Account.class).from(Account.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM"));
        }

        /**
         * The dialect accessor follows the fluent lower-camel naming convention (was {@code SqlDialect()}).
         */
        @Test
        void testSqlDialectAccessor() {
            SqlBuilder builder = PSC.select("id").from("users");

            assertNotNull(builder.sqlDialect());
            assertEquals(NamingPolicy.SNAKE_CASE, builder.sqlDialect().namingPolicy());
            assertEquals(SqlDialect.SqlPolicy.PARAMETERIZED_SQL, builder.sqlDialect().sqlPolicy());

            builder.build();
        }

        @Test
        void testSubEntityExpansionPreservesQualifiedColumnMapping() {
            final String sql = PSC.select("profile").from(QualifiedProfileHolder.class).build().query();

            assertEquals("SELECT p.display_name AS \"profile.name\" FROM profile_holders", sql);
        }

        @Test
        void testUnicodeEscapeEqualsSignsRemainPartOfTheQuotedColumn() {
            final Dsl postgres = Dsl.forDialect(SqlDialect.builder()
                    .productInfo(ProductInfo.of("PostgreSQL"))
                    .namingPolicy(NamingPolicy.SNAKE_CASE)
                    .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
                    .build());
            final String column = "U&\"metric=003Dvalue\" UESCAPE '='";
            final String renderedColumn = "u&\"metric=003Dvalue\" uescape '='";

            assertEquals("UPDATE measurements SET " + renderedColumn + " = ?", postgres.update("measurements").set(column).build().query());
            assertEquals("UPDATE measurements SET " + renderedColumn + " = 0", postgres.update("measurements").set(column + " = 0").build().query());
        }

        // An unsafe Selection table alias is rejected with a message that names it as a TABLE alias (it was
        // previously reported as "Column alias for 'Account table alias'").
        @Test
        void testUnsafeSelectionTableAliasIsReportedAsTableAlias() {
            final List<Selection> blankAlias = Arrays.asList(Selection.builder(Account.class).tableAlias(" ").build());
            final IllegalArgumentException blank = assertThrows(IllegalArgumentException.class, () -> PSC.select(blankAlias));
            assertTrue(blank.getMessage().startsWith("Table alias for 'Account' must not be"), blank.getMessage());
            assertFalse(blank.getMessage().contains("Column alias"), blank.getMessage());

            final List<Selection> commentAlias = Arrays.asList(Selection.builder(Account.class).tableAlias("a--").build());
            final IllegalArgumentException comment = assertThrows(IllegalArgumentException.class, () -> PSC.select(commentAlias));
            assertTrue(comment.getMessage().startsWith("Table alias for 'Account' must not be"), comment.getMessage());

            // The class alias keeps its column-alias wording and a safe table alias still renders.
            final List<Selection> badClassAlias = Arrays.asList(Selection.builder(Account.class).tableAlias("a").classAlias("x'y").build());
            assertTrue(assertThrows(IllegalArgumentException.class, () -> PSC.select(badClassAlias)).getMessage().startsWith("Column alias for 'Account'"));
            assertNotNull(PSC.select(Arrays.asList(Selection.builder(Account.class).tableAlias("a").classAlias("acc").build()))
                    .from("test_account a")
                    .build()
                    .query());
        }

        @Test
        void testVeryLongColumnList() {
            // Test with many columns
            List<String> columns = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                columns.add("col" + i);
            }

            String sql = PSC.select(columns).from("big_table").build().query();

            assertTrue(sql.startsWith("SELECT col0, col1, col2"));
            assertTrue(sql.contains("col49"));
            assertTrue(sql.contains("FROM big_table"));
        }
    }

    @Nested
    class ParameterBindings {
        @Test
        void testExplicitDerivedTableIsolationPreservesBranchLimitAndBindings() {
            final SqlBuilder child = NSC.select("id").from("archived_records").where(Filters.eq("status", "archived")).orderBy("id").limit(1);
            final SqlBuilder isolated = NSC.select("*").from(child, "branch");
            final SP result = NSC.select("id").from("current_records").where(Filters.eq("status", "current")).union(isolated).orderBy("id").limit(5).build();

            assertEquals("SELECT id FROM current_records WHERE status = :status UNION SELECT * FROM "
                    + "(SELECT id FROM archived_records WHERE status = :status_2 ORDER BY id LIMIT 1) branch ORDER BY id LIMIT 5", result.query());
            assertEquals(Arrays.asList("current", "archived"), result.parameters());
        }

        @Test
        void testGlobalNamedParameterHandlerMethodsAreRemoved() {
            assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getMethod("setHandlerForNamedParameter", BiConsumer.class));
            assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getMethod("resetHandlerForNamedParameter"));
        }

        @Test
        void testLargePositionalInKeepsExpressionBindingsAndRejectsNulls() {
            for (final int size : new int[] { 16, 17, 256 }) {
                final List<Object> values = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    values.add(i);
                }
                final SP simple = PSC.select("id").from("users").where(Filters.in("id", values)).build();
                assertEquals(values, simple.parameters());
                assertEquals("SELECT id FROM users WHERE id IN (" + String.join(", ", Collections.nCopies(size, "?")) + ")", simple.query());
                values.set(3, null);
                assertThrows(IllegalArgumentException.class, () -> Filters.in("id", values));
                values.set(3, 3);
                // Expressions and QME must bypass bulk binding, including when found at the end.
                values.set(size - 1, Filters.QME);
                values.set(size - 2, SqlExpression.of("42"));
                final SP mixed = PSC.select("id").from("users").where(Filters.in("id", values)).build();
                assertEquals(values.subList(0, size - 2), mixed.parameters());
                assertTrue(mixed.query().endsWith(", 42, ?)"));
            }
        }

        @Test
        void testLimitExpressionRejectsUntrackedPlaceholders() {
            assertThrows(IllegalArgumentException.class, () -> Filters.limit("LIMIT ?"));
            assertThrows(IllegalArgumentException.class, () -> Filters.limit("LIMIT ? OFFSET ?"));
            assertThrows(IllegalArgumentException.class, () -> Filters.limit("LIMIT :count"));
            assertThrows(IllegalArgumentException.class, () -> Filters.limit("LIMIT #{count}"));
            assertThrows(IllegalArgumentException.class, () -> Criteria.builder().limit("FETCH NEXT ? ROWS ONLY"));
        }

        @Test
        void testMultiColumnInCondition_Parameterized() {
            AbstractQueryBuilder.SP sp = PSC.select("*")
                    .from("users")
                    .where(Filters.in(Arrays.asList("firstName", "lastName"), Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe"))))
                    .build();

            assertEquals("SELECT * FROM users WHERE (first_name, last_name) IN ((?, ?), (?, ?))", sp.query());
            assertEquals(Arrays.asList("John", "Doe", "Jane", "Roe"), sp.parameters());
        }

        @Test
        void testMultiColumnNotInCondition_Parameterized() {
            String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.notIn(Arrays.asList("firstName", "lastName"), Arrays.asList(Arrays.asList("John", "Doe"))))
                    .build()
                    .query();

            assertEquals("SELECT * FROM users WHERE (first_name, last_name) NOT IN ((?, ?))", sql);
        }

        /**
         * Regression test: when a table-aliased property like "ord.orderDate" is used with
         * BETWEEN under a named-SQL builder (NSC/NSB/NAC/NLC), the synthesized "min"/"max"
         * parameter names previously contained an embedded dot (":minOrd.orderDate"), which
         * most named-parameter parsers (Spring's NamedParameterJdbcTemplate, MyBatis, etc.)
         * reject as invalid identifiers. The fix strips the alias prefix so the placeholder
         * becomes ":minOrderDate" / ":maxOrderDate".
         */
        @Test
        void testNamedBetweenWithTableAliasedColumn_noDotInParameterName() {
            // NSC: snake_case + ":name" placeholders.
            String sql = NSC.select("*").from("orders ord").where(Filters.between("ord.orderDate", "2023-01-01", "2023-12-31")).build().query();
            assertTrue(sql.contains(":minOrderDate"), sql);
            assertTrue(sql.contains(":maxOrderDate"), sql);
            // The buggy form with a dot inside the parameter name must not be present.
            assertFalse(sql.contains(":minOrd.orderDate"), sql);
            assertFalse(sql.contains(":maxOrd.orderDate"), sql);

            // NotBetween must follow the same rule.
            String sql2 = NSC.select("*").from("orders ord").where(Filters.notBetween("ord.orderDate", "2023-01-01", "2023-12-31")).build().query();
            assertTrue(sql2.contains(":minOrderDate"), sql2);
            assertTrue(sql2.contains(":maxOrderDate"), sql2);
            assertFalse(sql2.contains(":minOrd.orderDate"), sql2);

            // Same rule for iBATIS-style placeholders generated by MSC.
            String sql3 = Dsl.MSC.select("*").from("orders ord").where(Filters.between("ord.orderDate", "a", "b")).build().query();
            assertTrue(sql3.contains("#{minOrderDate}"), sql3);
            assertTrue(sql3.contains("#{maxOrderDate}"), sql3);
            assertFalse(sql3.contains("#{minOrd.orderDate}"), sql3);
        }

        /**
         * Regression test: a table-aliased property used with a simple equality / LIKE under a
         * named- or iBATIS-style SQL builder must produce a valid placeholder identifier (i.e.
         * the alias prefix must be stripped). Previously, {@code Filters.eq("u.id", ...)}
         * generated {@code :u.id} which is not a valid named parameter.
         */
        @Test
        void testNamedEqualWithTableAliasedColumn_noDotInParameterName() {
            // NSC (named SQL, snake_case): the placeholder must be ":id", not ":u.id".
            String sql = NSC.select("*").from("users u").where(Filters.eq("u.id", 1L)).build().query();
            assertTrue(sql.contains(":id"), sql);
            assertFalse(sql.contains(":u.id"), sql);

            // MSC (iBATIS SQL, snake_case): the placeholder must be "#{id}", not "#{u.id}".
            String sql2 = Dsl.MSC.select("*").from("users u").where(Filters.eq("u.id", 1L)).build().query();
            assertTrue(sql2.contains("#{id}"), sql2);
            assertFalse(sql2.contains("#{u.id}"), sql2);
        }

        /**
         * Regression test: a table-aliased property used with IN/NOT IN under a named-style
         * SQL builder must produce valid placeholder identifiers (no embedded dots).
         */
        @Test
        void testNamedInWithTableAliasedColumn_noDotInParameterName() {
            String sql = NSC.select("*").from("users u").where(Filters.in("u.status", Arrays.asList("A", "B"))).build().query();
            // Each IN element must produce ":status1", ":status2" — never ":u.status1".
            assertFalse(sql.contains(":u.status"), sql);
            assertTrue(sql.contains(":status1"), sql);
            assertTrue(sql.contains(":status2"), sql);

            String sql2 = NSC.select("*").from("users u").where(Filters.notIn("u.status", Arrays.asList("X", "Y"))).build().query();
            assertFalse(sql2.contains(":u.status"), sql2);
            assertTrue(sql2.contains(":status1"), sql2);
            assertTrue(sql2.contains(":status2"), sql2);
        }

        @Test
        void testNamedParameterHandlerIsCarriedBySqlDialect() {
            final BiConsumer<StringBuilder, String> customHandler = (sb, propName) -> sb.append("#{").append(propName).append("}");
            final SqlDialect dialect = NSC.sqlDialect().toBuilder().namedParameterHandler(customHandler).build();

            assertEquals(customHandler, dialect.namedParameterHandler());
            assertEquals("SELECT name FROM users WHERE id = #{id}",
                    Dsl.forDialect(dialect).select("name").from("users").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        void testNamedParameterHandlerIsScopedToDialect() {
            final BiConsumer<StringBuilder, String> customHandler = (sb, propName) -> sb.append("${").append(propName).append("}");
            final Dsl customDsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler(customHandler).build());

            String sql = customDsl.select("name").from("users").where(Filters.eq("id", 1)).build().query();
            assertEquals("SELECT name FROM users WHERE id = ${id}", sql);

            sql = customDsl.update("account").set("firstName").where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("${firstName}"));
            assertTrue(sql.contains("${id}"));

            // The predefined DSL remains unchanged.
            sql = NSC.update("account").set("firstName").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":id"));
        }

        @Test
        void testNamedParameterHandlerMustEmitToken() {
            final Dsl dsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
                // Deliberately emits nothing.
            }).build());

            assertThrows(IllegalStateException.class, () -> dsl.select("name").from("users").where(Filters.eq("id", 1)).build());
        }

        /**
         * Bug: the generated "name_N" suffix for repeated named parameters could collide with a property
         * literally named "name_N", producing two different values bound to one placeholder.
         */
        @Test
        void testNamedParameterSuffixDoesNotCollideWithLiteralPropertyName() {
            SP sp = NSC.select("*").from("users").where(Filters.and(Filters.eq("id", 1), Filters.eq("id", 2), Filters.eq("id_2", 3))).build();

            assertEquals("SELECT * FROM users WHERE (id = :id) AND (id = :id_2) AND (id_2 = :id_2_2)", sp.query());
            assertEquals(Arrays.asList(1, 2, 3), sp.parameters());
        }

        /**
         * Same collision with the literal "name_N" property occurring first: the second occurrence of the
         * base name must skip the taken "name_2" suffix.
         */
        @Test
        void testNamedParameterSuffixSkipsNameTakenByLiteralProperty() {
            SP sp = NSC.select("*").from("users").where(Filters.and(Filters.eq("id_2", 3), Filters.eq("id", 1), Filters.eq("id", 2))).build();

            assertEquals("SELECT * FROM users WHERE (id_2 = :id_2) AND (id = :id) AND (id = :id_3)", sp.query());
            assertEquals(Arrays.asList(3, 1, 2), sp.parameters());
        }

        @Test
        void testNamedSQLWithParameters() {
            String sql = NSC.select("*").from("users").where(Filters.eq("firstName", "John").and(Filters.gt("age", 25))).build().query();

            assertTrue(sql.contains("first_name = :firstName"));
            assertTrue(sql.contains("age > :age"));

            List<Object> params = NSC.select("*").from("users").where(Filters.eq("firstName", "John").and(Filters.gt("age", 25))).parameters();

            assertEquals(2, params.size());
            assertEquals("John", params.get(0));
            assertEquals(25, params.get(1));
        }

        @Test
        void testNamedSQLWithRepeatedPropertyUsesUniqueParameterNames() {
            String sql = NSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").and(Filters.ne("status", "DELETED"))).build().query();

            assertTrue(sql.contains("status = :status"));
            assertTrue(sql.contains("status != :status_2"));

            List<Object> params = NSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").and(Filters.ne("status", "DELETED"))).parameters();
            assertEquals(Arrays.asList("ACTIVE", "DELETED"), params);
        }

        @Test
        void testNullNamedParameterHandlerUsesDefault() {
            final Dsl dsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler(null).build());
            assertEquals("SELECT name FROM users WHERE id = :id", dsl.select("name").from("users").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        void testNullParameters() {
            String sql = PSC.select("*").from("users").where(Filters.eq("deletedBy", null)).build().query();

            assertEquals("SELECT * FROM users WHERE deleted_by IS NULL", sql);

            SqlBuilder builder = PSC.select("*").from("users").where(Filters.eq("deletedBy", null));
            builder.build().query();

            List<Object> params = builder.parameters();
            assertTrue(params.isEmpty());

            String sql2 = PSC.select("*").from("users").where(Filters.ne("deletedBy", null)).build().query();
            assertEquals("SELECT * FROM users WHERE deleted_by IS NOT NULL", sql2);

            SqlBuilder builder2 = PSC.select("*").from("users").where(Filters.ne("deletedBy", null));
            builder2.build().query();
            assertTrue(builder2.parameters().isEmpty());
        }

        // Parameterized query tests
        @Test
        void testPairWithParameters() {
            SqlBuilder builder = PSC.select("*").from("users").where(Filters.eq("id", 1));
            AbstractQueryBuilder.SP sp = builder.build();
            assertNotNull(sp);
            assertNotNull(sp.query());
            assertNotNull(sp.parameters());
        }

        @Test
        void testParameters() {
            SqlBuilder builder = PSC.select("*").from("account").where(Filters.eq("name", "John").and(Filters.gt("age", 25)));

            SP sp = builder.build();
            List<Object> params = sp.parameters();

            assertEquals(2, params.size());
            assertEquals("John", params.get(0));
            assertEquals(25, params.get(1));
        }

        /**
         * Scenario 6 — Nested subqueries (subquery inside subquery).
         * Verifies parameters flatten in left-to-right SQL order.
         */
        @Test
        void testParamOrder_NestedSubQueries_Pass4() {
            SubQuery innerInnerSQ = Filters.subQuery("regions", Arrays.asList("id"), Filters.eq("continent", "EU"));
            SubQuery middleSQ = Filters.subQuery("countries", Arrays.asList("id"), Filters.in("region_id", innerInnerSQ).and(Filters.eq("active", true)));
            SP sp = PSC.select("id").from("users").where(Filters.in("country_id", middleSQ).and(Filters.gt("age", 30))).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList("EU", true, 30), sp.parameters(), "Expected [EU, true, 30] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        /**
         * Extra: Three-branch AND junction interleaving Binary/SubQuery/Binary — verifies that
         * Junction iteration preserves parameter ordering when subqueries sit between binary
         * branches.
         */
        @Test
        void testParamOrder_ThreeWayAndInterleaved_Pass4() {
            SubQuery sq = Filters.subQuery("flagged", Arrays.asList("user_id"), Filters.eq("severity", "HIGH").and(Filters.gt("count", 2)));
            SP sp = PSC.select("id").from("users").where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", sq), Filters.gt("age", 21))).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList("ACTIVE", "HIGH", 2, 21), sp.parameters(),
                    "Expected [ACTIVE, HIGH, 2, 21] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        /**
         * Scenario 5 — HAVING with parameter after GROUP BY and WHERE with parameter.
         */
        @Test
        void testParamOrder_WhereGroupByHaving_Pass4() {
            Criteria crit = Criteria.builder().where(Filters.gt("salary", 50000)).groupBy("department").having(Filters.gt("COUNT(*)", 5)).build();
            SP sp = PSC.select("department", "COUNT(*)").from("employees").append(crit).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList(50000, 5), sp.parameters(), "Expected [50000, 5] but got " + sp.parameters() + " SQL=" + sp.query());
        }
    }

    @Nested
    class Predicates {
        @Test
        void testAppendIf_ConditionFalse() {
            String sql = PSC.select("*").from("users").appendIf(false, Filters.gt("age", 18)).build().query();
            assertEquals("SELECT * FROM users", sql);
        }

        @Test
        void testAppendIf_ConditionTrue() {
            String sql = PSC.select("*").from("users").appendIf(true, Filters.gt("age", 18)).build().query();
            assertEquals("SELECT * FROM users WHERE age > ?", sql);
        }

        @Test
        void testAppendIfOrElse_ConditionFalse() {
            SP sp = PSC.select("*").from("users").appendIfOrElse(false, Filters.eq("status", "active"), Filters.eq("status", "inactive")).build();
            assertEquals("SELECT * FROM users WHERE status = ?", sp.query());
            assertEquals("inactive", sp.parameters().get(0));
        }

        @Test
        void testAppendIfOrElse_ConditionTrue() {
            String sql = PSC.select("*").from("users").appendIfOrElse(true, Filters.eq("status", "active"), Filters.eq("status", "inactive")).build().query();
            assertEquals("SELECT * FROM users WHERE status = ?", sql);
            assertEquals("active",
                    PSC.select("*")
                            .from("users")
                            .appendIfOrElse(true, Filters.eq("status", "active"), Filters.eq("status", "inactive"))
                            .build()
                            .parameters()
                            .get(0));
        }

        @Test
        void testBetweenCondition() {
            String sql = PSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();

            assertEquals("SELECT * FROM users WHERE age BETWEEN ? AND ?", sql);
        }

        // Chaining tests
        @Test
        void testChainedAndConditions() {
            String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.eq("status", "active").and(Filters.gt("age", 18)).and(Filters.lt("age", 65)))
                    .build()
                    .query();
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testChainedOrConditions() {
            String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.eq("role", "admin").or(Filters.eq("role", "moderator")).or(Filters.eq("role", "owner")))
                    .build()
                    .query();
            assertTrue(sql.contains("OR"));
        }

        @Test
        void testConditionsWithAnd() {
            String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.and(Filters.gt("age", 18), Filters.lt("age", 65), Filters.eq("status", "ACTIVE")))
                    .build()
                    .query();

            assertTrue(sql.contains("WHERE (age > ?) AND (age < ?) AND (status = ?)"));
        }

        @Test
        void testConditionsWithOr() {
            String sql = PSC.select("*").from("users").where(Filters.or(Filters.eq("status", "ACTIVE"), Filters.eq("status", "PENDING"))).build().query();

            assertTrue(sql.contains("WHERE (status = ?) OR (status = ?)"));
        }

        @Test
        void testCriteriaCondition() {
            Criteria criteria = Criteria.builder()
                    .where(Filters.gt("age", 18))
                    .groupBy("status")
                    .having(Filters.gt("COUNT(*)", 5))
                    .orderBy("status")
                    .limit(10)
                    .build();

            String sql = PSC.select("status", "COUNT(*)").from("users").append(criteria).build().query();

            assertTrue(sql.contains("WHERE age > ?"));
            assertTrue(sql.contains("GROUP BY status"));
            assertTrue(sql.contains("HAVING COUNT(*) > ?"));
            assertTrue(sql.contains("ORDER BY status"));
            assertTrue(sql.contains("LIMIT 10"));
        }

        @Test
        void testCustomAbstractBetweenSubclassesRenderUnderEveryPolicyAndWhenNested() {
            // Covers custom AbstractBetween subclasses (BETWEEN and NOT BETWEEN) under the named/raw/MyBatis policies and nested
            // in a junction, a Criteria and a structured SubQuery (all of which used to throw "Unsupported condition type").
            SP sp = NSC.select("id").from("t o").where(new CustomNotBetween("o.age", 18, 65)).build();
            assertEquals("SELECT id FROM t o WHERE o.age NOT BETWEEN :minAge AND :maxAge", sp.query());
            assertEquals(Arrays.asList(18, 65), sp.parameters());

            assertEquals("SELECT id FROM t WHERE age BETWEEN 1 AND 2", SCSB.select("id").from("t").where(new CustomBetween("age", 1, 2)).build().query());
            assertEquals("SELECT id FROM t WHERE age NOT BETWEEN 1 AND 2",
                    SCSB.select("id").from("t").where(new CustomNotBetween("age", 1, 2)).build().query());

            sp = MSC.select("id").from("t").where(new CustomNotBetween("age", 1, 2)).build();
            assertEquals("SELECT id FROM t WHERE age NOT BETWEEN #{minAge} AND #{maxAge}", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());

            sp = PSC.select("id").from("t").where(Filters.and(Filters.eq("a", 1), new CustomNotBetween("age", 18, 65))).build();
            assertEquals("SELECT id FROM t WHERE (a = ?) AND (age NOT BETWEEN ? AND ?)", sp.query());
            assertEquals(Arrays.asList(1, 18, 65), sp.parameters());

            sp = PSC.select("id").from("t").append(Criteria.builder().where(new CustomNotBetween("age", 18, 65)).build()).build();
            assertEquals("SELECT id FROM t WHERE age NOT BETWEEN ? AND ?", sp.query());
            assertEquals(Arrays.asList(18, 65), sp.parameters());

            sp = PSC.select("id").from("t").where(Filters.in("id", Filters.subQuery("u", Arrays.asList("id"), new CustomNotBetween("age", 18, 65)))).build();
            assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE age NOT BETWEEN ? AND ?)", sp.query());
            assertEquals(Arrays.asList(18, 65), sp.parameters());

            sp = NSC.select("id").from("t").where(Filters.in("id", Filters.subQuery("u", Arrays.asList("id"), new CustomBetween("age", 18, 65)))).build();
            assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE age BETWEEN :minAge AND :maxAge)", sp.query());
            assertEquals(Arrays.asList(18, 65), sp.parameters());
        }

        @Test
        void testCustomAbstractBetweenSubclassIsRendered() {
            // Regression: appendCondition dispatched on the concrete Between/NotBetween classes, so a custom AbstractBetween
            // subclass rendered fine via toString() but builders rejected it with "Unsupported condition type".
            final AbstractQueryBuilder.SP sp = PSC.select("id").from("account").where(new CustomBetween("age", 18, 65)).build();
            assertEquals("SELECT id FROM account WHERE age BETWEEN ? AND ?", sp.query());
            assertEquals(Arrays.asList(18, 65), sp.parameters());
        }

        @Test
        void testEmptyConditions() {
            // An empty junction renders as its Boolean identity (neutral element) instead of being rejected.
            assertEquals("SELECT * FROM users WHERE 1 = 1", PSC.select("*").from("users").where(Filters.and()).build().query());
            assertEquals("SELECT * FROM users WHERE 1 = 0", PSC.select("*").from("users").where(Filters.or()).build().query());
            assertEquals("1 = 1", PSC.renderCondition(Filters.and()).build().query());
        }

        @Test
        void testExpressionCondition() {
            String sql = PSC.select("*").from("users").where(Filters.expr("age > 18 AND status = 'ACTIVE'")).build().query();

            assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'ACTIVE'", sql);
        }

        @Test
        void testExpressionInWhere() {
            SqlExpression expr = Filters.expr("age > 18 AND status = 'active'");
            String sql = PSC.select("*").from("users").where(expr).build().query();
            assertNotNull(sql);
        }

        // HAVING tests
        @Test
        void testHaving() {
            String sql = PSC.select("department", "COUNT(*)").from("employees").groupBy("department").having(Filters.expr("COUNT(*) > 5")).build().query();
            assertTrue(sql.contains("HAVING"));
        }

        @Test
        void testHavingClause() {
            Having having = Filters.having("COUNT(*) > 10");

            String sql = PSC.select("status", "COUNT(*)").from("users").groupBy("status").append(having).build().query();

            assertTrue(sql.contains("HAVING COUNT(*) > 10"));
        }

        @Test
        void testHavingWithMultipleConditions() {
            String sql = PSC.select("status", "COUNT(*) as count")
                    .from("users")
                    .groupBy("status")
                    .having(Filters.and(Filters.gt("COUNT(*)", 10), Filters.lt("COUNT(*)", 100)))
                    .build()
                    .query();

            assertTrue(sql.contains("GROUP BY status"));
            assertTrue(sql.contains("HAVING (COUNT(*) > ?) AND (COUNT(*) < ?)"));
        }

        @Test
        void testInCondition() {
            String sql = PSC.select("*").from("users").where(Filters.in("status", Arrays.asList("ACTIVE", "PENDING", "APPROVED"))).build().query();

            assertEquals("SELECT * FROM users WHERE status IN (?, ?, ?)", sql);
        }

        @Test
        void testIntegralRawValuesMatchValidatedLiteralRendering() {
            final Object[] numbers = { Byte.MIN_VALUE, Byte.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE,
                    Long.MAX_VALUE, 0, 0L, new java.math.BigInteger("123456789012345678901234567890"), new java.math.BigDecimal("1.2300"), 1.25d };
            for (final Object value : numbers) {
                assertEquals("UPDATE users SET value = " + SqlExpression.renderValue(value), SCSB.update("users").set("value", value).build().query());
            }
            // Non-finite floating point and arbitrary Number text still go through validation.
            for (final Object value : new Object[] { Double.NaN, Double.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY }) {
                final SqlBuilder builder = SCSB.update("users");
                assertThrows(IllegalArgumentException.class, () -> builder.set("value", value));
                assertEquals("UPDATE users SET value = 1", builder.set("value", 1).build().query());
            }
        }

        // Instance method tests using PSC (Parameterized SQL with snake_case)

        @Test
        void testInto() {
            String sql = PSC.insert("firstName", "lastName").into("account").build().query();

            assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?)", sql);

            // Test with entity class
            sql = PSC.insert("firstName", "lastName").into(Account.class).build().query();

            assertEquals("INSERT INTO test_account (first_name, last_name) VALUES (?, ?)", sql);
        }

        @Test
        void testInWithAllSimpleValues_BackwardCompatPass3() {
            SP sp = PSC.select("id").from("test_account").where(Filters.in("status", Arrays.asList("A", "B", "C"))).build();
            assertTrue(sp.query().endsWith("IN (?, ?, ?)"), "Simple IN should produce three placeholders: " + sp.query());
            assertEquals(3, sp.parameters().size());
            assertEquals(Arrays.asList("A", "B", "C"), sp.parameters());
        }

        @Test
        void testInWithExpressionValueIsEmbeddedNotDropped_Pass3() {
            SP sp = PSC.select("id").from("test_account").where(Filters.in("status", Arrays.asList(SqlExpression.of("'ACTIVE'"), "PENDING"))).build();
            assertTrue(sp.query().contains("'ACTIVE'"), "SqlExpression literal must be embedded in IN list: " + sp.query());
            assertTrue(sp.query().contains("?"), "Non-SqlExpression value must still be parameterized: " + sp.query());
            assertEquals(1, sp.parameters().size(), "Only the non-SqlExpression value should be bound; got: " + sp.parameters());
            assertEquals("PENDING", sp.parameters().get(0));
        }

        @Test
        void testIsNotNullCondition() {
            String sql = PSC.select("*").from("users").where(Filters.isNotNull("email")).build().query();

            assertEquals("SELECT * FROM users WHERE email IS NOT NULL", sql);
        }

        @Test
        void testIsNullCondition() {
            String sql = PSC.select("*").from("users").where(Filters.isNull("deletedDate")).build().query();

            assertEquals("SELECT * FROM users WHERE deleted_date IS NULL", sql);
        }

        @Test
        void testLikeCondition() {
            String sql = PSC.select("*").from("users").where(Filters.like("name", "%John%")).build().query();

            assertEquals("SELECT * FROM users WHERE name LIKE ?", sql);
        }

        @Test
        void testMultipleAppendsWithConditions() {
            String sql = PSC.select("*").from("users").append(Filters.eq("status", "ACTIVE")).append(" AND age > 18").build().query();

            assertTrue(sql.contains("WHERE status = ?"));
            assertTrue(sql.contains("AND age > 18"));
        }

        @Test
        void testMultipleWhereConditions() {

            assertThrows(IllegalStateException.class, () -> {
                PSC.select("*")
                        .from("users")
                        .where(Filters.gt("age", 18))
                        .where(Filters.eq("status", "ACTIVE"))
                        .where(Filters.like("email", "%@company.com"))
                        .build()
                        .query();
            });

        }

        @Test
        void testNamedSQLWithBetween() {
            String sql = NSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();

            assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
        }

        @Test
        void testNotBetweenCondition() {
            String sql = PSC.select("*").from("users").where(Filters.notBetween("age", 18, 65)).build().query();

            assertEquals("SELECT * FROM users WHERE age NOT BETWEEN ? AND ?", sql);
        }

        @Test
        void testNotInCondition() {
            String sql = PSC.select("*").from("users").where(Filters.notIn("status", Arrays.asList("DELETED", "BANNED"))).build().query();

            assertEquals("SELECT * FROM users WHERE status NOT IN (?, ?)", sql);
        }

        @Test
        void testNotInWithExpressionValueIsEmbeddedNotDropped_Pass3() {
            SP sp = PSC.select("id").from("test_account").where(Filters.notIn("status", Arrays.asList(SqlExpression.of("'DELETED'"), "ARCHIVED"))).build();
            assertTrue(sp.query().contains("'DELETED'"), "SqlExpression literal must be embedded in NOT IN list: " + sp.query());
            assertTrue(sp.query().contains("NOT IN"));
            assertEquals(1, sp.parameters().size());
            assertEquals("ARCHIVED", sp.parameters().get(0));
        }

        @Test
        void testNotLikeCondition() {
            String sql = PSC.select("*").from("users").where(Filters.notLike("email", "%@temp.com")).build().query();

            assertEquals("SELECT * FROM users WHERE email NOT LIKE ?", sql);
        }

        @Test
        void testParenthesizedUnaryConditionDoesNotAddDoubleSpace() {
            final AbstractQueryBuilder.SP sp = PSC.select("*").from("users").where(Filters.not(Filters.equal("status", "ACTIVE"))).build();

            assertEquals("SELECT * FROM users WHERE NOT (status = ?)", sp.query());
            assertEquals(Arrays.asList("ACTIVE"), sp.parameters());
        }

        @Test
        void testUnsupportedConditionMessageUsesConditionClass() {
            final Condition unsupported = unsupportedCondition();

            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").where(unsupported).build());

            assertTrue(ex.getMessage().contains(unsupported.getClass().getName()));
        }

        @Test
        void testWhere() {
            String sql = PSC.select("*").from("users").where("age > 18").build().query();

            assertEquals("SELECT * FROM users WHERE age > 18", sql);

            // With condition
            sql = PSC.select("*").from("users").where(Filters.gt("age", 18)).build().query();

            assertEquals("SELECT * FROM users WHERE age > ?", sql);
        }

        /**
         * Regression test for ClassCastException bug in SqlBuilder.appendCondition().
         * Where and Having extend Clause (not Cell), so casting them to Cell would throw ClassCastException.
         * This test verifies that Where/Having conditions are correctly handled when nested inside a Junction.
         */
        @Test
        void testWhereAndHavingNestedInJunction() {
            // Where nested inside an And junction - this triggers SqlBuilder.appendCondition() with a Where instance
            Where where = Filters.where("age > 18");
            Having having = Filters.having("COUNT(*) > 5");

            // Using Where as a standalone Clause via append()
            String sql = PSC.select("*").from("users").append(where).build().query();
            assertTrue(sql.contains("WHERE age > 18"));

            // Using Having as a standalone Clause via append()
            sql = PSC.select("status", "COUNT(*)").from("users").groupBy("status").append(having).build().query();
            assertTrue(sql.contains("HAVING COUNT(*) > 5"));

            // Where inside a Criteria (exercises AbstractQueryBuilder.append -> appendCondition path)
            Criteria criteria = Criteria.builder().where(Filters.expr("status = 'ACTIVE'")).groupBy("status").having(Filters.expr("COUNT(*) > 10")).build();
            sql = PSC.select("status", "COUNT(*)").from("users").append(criteria).build().query();
            assertTrue(sql.contains("WHERE status = 'ACTIVE'"));
            assertTrue(sql.contains("HAVING COUNT(*) > 10"));
        }

        @Test
        void testWhereBetween() {
            String sql = PSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();
            assertTrue(sql.contains("BETWEEN"));
        }

        @Test
        void testWhereClause() {
            Where where = Filters.where("age > 18 AND status = 'ACTIVE'");

            String sql = PSC.select("*").from("users").append(where).build().query();

            assertTrue(sql.contains("WHERE age > 18 AND status = 'ACTIVE'"));
        }

        // WHERE clause tests
        @Test
        void testWhereEqual() {
            String sql = PSC.select("*").from("users").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("WHERE"));
        }

        // Multiple where conditions with different operators
        @Test
        void testWhereGreaterThan() {
            String sql = PSC.select("*").from("users").where(Filters.gt("age", 18)).build().query();
            assertTrue(sql.contains(">"));
        }

        @Test
        void testWhereGreaterThanOrEqual() {
            String sql = PSC.select("*").from("users").where(Filters.ge("age", 21)).build().query();
            assertTrue(sql.contains(">="));
        }

        // IN clause tests
        @Test
        void testWhereIn() {
            String sql = PSC.select("*").from("users").where("id IN (1, 2, 3)").build().query();
            assertTrue(sql.contains("IN"));
        }

        @Test
        void testWhereIsNotNull() {
            String sql = PSC.select("*").from("users").where(Filters.isNotNull("email")).build().query();
            assertTrue(sql.contains("IS NOT NULL"));
        }

        @Test
        void testWhereIsNotWithNullValue() {
            String sql = PSC.select("*").from("users").where(Filters.isNot("deleted_at", null)).build().query();
            assertTrue(sql.contains("IS NOT NULL"));
            assertFalse(sql.contains("IS NOT ?"));
        }

        @Test
        void testWhereIsNull() {
            String sql = PSC.select("*").from("users").where(Filters.isNull("deleted_at")).build().query();
            assertTrue(sql.contains("IS NULL"));
        }

        @Test
        void testWhereIsWithNullValue() {
            String sql = PSC.select("*").from("users").where(Filters.is("deleted_at", null)).build().query();
            assertTrue(sql.contains("IS NULL"));
            assertFalse(sql.contains("IS ?"));
        }

        @Test
        void testWhereLessThan() {
            String sql = PSC.select("*").from("users").where(Filters.lt("age", 65)).build().query();
            assertTrue(sql.contains("<"));
        }

        @Test
        void testWhereLessThanOrEqual() {
            String sql = PSC.select("*").from("users").where(Filters.le("age", 60)).build().query();
            assertTrue(sql.contains("<="));
        }

        @Test
        void testWhereLike() {
            String sql = PSC.select("*").from("users").where(Filters.like("name", "%John%")).build().query();
            assertTrue(sql.contains("LIKE"));
        }

        @Test
        void testWhereMultipleConditions() {
            String sql = PSC.select("*").from("users").where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testWhereNotEqual() {
            String sql = PSC.select("*").from("users").where(Filters.ne("status", "deleted")).build().query();
            assertTrue(sql.contains("!=") || sql.contains("<>"));
        }

        // NULL handling
        @Test
        void testWhereNullCheck() {
            String sql = PSC.select("*").from("users").where(Filters.isNull("deleted_at").and(Filters.isNotNull("email"))).build().query();
            assertTrue(sql.contains("IS NULL"));
            assertTrue(sql.contains("IS NOT NULL"));
        }

        @Test
        void testWhereOr() {
            String sql = PSC.select("*").from("users").where(Filters.eq("role", "admin").or(Filters.eq("role", "moderator"))).build().query();
            assertTrue(sql.contains("OR"));
        }

        // Complex conditions
        @Test
        void testWhereWithNestedAndOr() {
            String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.eq("role", "admin"), Filters.eq("role", "moderator"))))
                    .build()
                    .query();
            assertNotNull(sql);
        }
    }

    @Nested
    class ProjectionAndOrdering {
        // Complex query tests
        @Test
        void testComplexSelectQuery() {
            String sql = PSC.select("u.id", "u.name", "COUNT(o.id) as order_count")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .where(Filters.eq("u.status", "active"))
                    .groupBy("u.id", "u.name")
                    .having(Filters.expr("COUNT(o.id) > 5"))
                    .orderBy("order_count", SortDirection.DESC)
                    .limit(10)
                    .build()
                    .query();

            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("GROUP BY"));
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("LIMIT"));
        }

        // Aggregate functions tests
        @Test
        void testCount() {
            String sql = PSC.select("COUNT(*)").from("users").build().query();
            assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        void testCriteriaSelectModifierRequiresCurrentSelectSegment() {
            Criteria criteria = Criteria.builder().distinct().build();

            assertThrows(IllegalStateException.class, () -> PSC.update("employees").set("department = 'sales'").append(criteria));
            assertThrows(IllegalStateException.class, () -> PSC.deleteFrom("employees").append(criteria));
            assertThrows(IllegalStateException.class, () -> PSC.renderCondition(Filters.equal("active", true)).append(criteria));
            assertThrows(IllegalStateException.class,
                    () -> PSC.select("department").from("employees").union("SELECT department FROM archived_employees").append(criteria));
        }

        @Test
        void testCriteriaSelectModifiersAreAppliedToCurrentSelect() {
            assertEquals("SELECT DISTINCT department FROM employees",
                    PSC.select("department").from("employees").append(Criteria.builder().distinct().build()).build().query());
            assertEquals("SELECT DISTINCT ON (department) department FROM employees",
                    PSC.select("department").from("employees").append(Criteria.builder().distinctOn("department").build()).build().query());
            assertEquals("SELECT DISTINCTROW department FROM employees",
                    PSC.select("department").from("employees").append(Criteria.builder().distinctRow().build()).build().query());
            assertEquals("SELECT SQL_CALC_FOUND_ROWS department FROM employees",
                    PSC.select("department").from("employees").append(Criteria.builder().selectModifier("SQL_CALC_FOUND_ROWS").build()).build().query());
        }

        // DISTINCT tests
        @Test
        void testDistinct() {
            String sql = PSC.select("status").from("users").distinct().build().query();
            assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        void testEmptySelect() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select().from("users").build().query());
        }

        @Test
        void testExplicitFetchCannotBeCombinedWithAnotherRowLimitOrTrailingOffset() {
            SqlBuilder fetchFirst = PSC.select("*").from("users").orderBy("id").fetchFirstRows(10);
            assertThrows(IllegalStateException.class, () -> fetchFirst.limit(5));
            assertThrows(IllegalStateException.class, () -> fetchFirst.offset(20));
            assertEquals("SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY", fetchFirst.build().query());

            SqlBuilder limit = PSC.select("*").from("users").orderBy("id").limit(10);
            assertThrows(IllegalStateException.class, () -> limit.fetchNextRows(5));
            assertEquals("SELECT * FROM users ORDER BY id LIMIT 10", limit.build().query());

            SqlBuilder limitStyleOffset = PSC.select("*").from("users").orderBy("id").offset(20);
            assertThrows(IllegalStateException.class, () -> limitStyleOffset.fetchNextRows(10));
            assertEquals("SELECT * FROM users ORDER BY id OFFSET 20", limitStyleOffset.build().query());

            assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY",
                    PSC.select("*").from("users").orderBy("id").offsetRows(20).fetchNextRows(10).build().query());
        }

        // SqlExpression tests
        @Test
        void testExpressionInSelect() {
            String sql = PSC.select("COUNT(*) as total").from("users").build().query();
            assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        void testFetchFirstNRowsOnly() {
            String sql = PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build().query();

            assertEquals("SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY", sql);
        }

        @Test
        void testFetchNextNRowsOnly() {
            String sql = PSC.select("*").from("users").orderBy("id").offsetRows(0).fetchNextRows(10).build().query();

            assertEquals("SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", sql);
        }

        @Test
        void testFrom() {
            // Single table
            String sql = PSC.select("*").from("users").build().query();
            assertEquals("SELECT * FROM users", sql);

            // Multiple tables
            sql = PSC.select("*").from(Array.of("users", "orders")).build().query();
            assertEquals("SELECT * FROM users, orders", sql);

            // Collection of tables
            sql = PSC.select("*").from(Arrays.asList("users", "orders", "products")).build().query();
            assertEquals("SELECT * FROM users, orders, products", sql);

            // With alias
            sql = PSC.select("*").from("users u").build().query();
            assertEquals("SELECT * FROM users u", sql);

            // With entity class
            sql = PSC.select("*").from(Account.class).build().query();
            assertEquals("SELECT * FROM test_account", sql);

            // With entity class and alias
            sql = PSC.select("*").from(Account.class, "a").build().query();
            assertEquals("SELECT * FROM test_account a", sql);
        }

        @Test
        void testFromAndIntoReentryGuards() {
            // Double from() used to emit two fused SELECT fragments.
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("t1").from("t2"));

            // into() after from() used to concatenate a second INSERT statement with no separator.
            assertThrows(IllegalStateException.class, () -> PSC.select("firstName").from("account").into("backup"));

            // A second into() is rejected as well.
            assertThrows(IllegalStateException.class, () -> PSC.insert("firstName").into("t1").into("t2"));

            // An INSERT ... SELECT left without its from() now fails at build instead of emitting a fragment.
            assertThrows(IllegalStateException.class, () -> PSC.select("firstName").into("backup").build());

            // The documented INSERT ... SELECT flow still works.
            assertEquals("INSERT INTO account_backup (first_name) SELECT first_name AS \"firstName\" FROM account",
                    PSC.select("firstName").into("account_backup").from("account").build().query());
        }

        @Test
        void testFromMultipleTables() {
            String sql = PSC.select("*").from("users", "orders").build().query();
            assertEquals("SELECT * FROM users, orders", sql);
        }

        // FROM clause tests
        @Test
        void testFromSingleTable() {
            String sql = PSC.select("*").from("users").build().query();
            assertTrue(sql.contains("FROM users"));
        }

        @Test
        void testFromWithExpression() {
            String sql = PSC.select("*").from("(SELECT * FROM users) t").build().query();
            assertEquals("SELECT * FROM (SELECT * FROM users) t", sql);

            sql = PSC.select("*").from("users u, orders o").build().query();
            assertEquals("SELECT * FROM users u, orders o", sql);
        }

        @Test
        void testFromWithoutSelect() {
            assertThrows(RuntimeException.class, () -> PSC.update("account").from("account"));
        }

        // GROUP BY tests
        @Test
        void testGroupBy() {
            String sql = PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build().query();
            assertTrue(sql.contains("GROUP BY department"));
        }

        @Test
        void testGroupBy_CollectionWithAscDirection() {
            String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupBy(Arrays.asList("category", "brand"), SortDirection.ASC)
                    .build()
                    .query();
            assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC", sql);
        }

        @Test
        void testGroupBy_CollectionWithDirection() {
            String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupBy(Arrays.asList("category", "brand"), SortDirection.DESC)
                    .build()
                    .query();
            assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC", sql);
        }

        @Test
        void testGroupByAsc() {
            String sql = PSC.select("category", "COUNT(*)").from("products").groupByAsc("category").build().query();

            assertEquals("SELECT category, COUNT(*) FROM products GROUP BY category ASC", sql);

            sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupByAsc("category", "brand").build().query();

            assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC", sql);

            sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupByAsc(Arrays.asList("category", "brand")).build().query();

            assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC", sql);
        }

        @Test
        void testGroupByDesc() {
            String sql = PSC.select("category", "COUNT(*)").from("products").groupByDesc("category").build().query();

            assertEquals("SELECT category, COUNT(*) FROM products GROUP BY category DESC", sql);

            sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupByDesc("category", "brand").build().query();

            assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC", sql);

            sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupByDesc(Arrays.asList("category", "brand")).build().query();

            assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC", sql);
        }

        // LIMIT tests
        @Test
        void testLimit() {
            String sql = PSC.select("*").from("users").limit(10).build().query();
            assertTrue(sql.contains("LIMIT 10"));
        }

        @Test
        void testLimitWithLargeNumbers() {
            String sql = PSC.select("*").from("users").limit(50, 1000000).build().query();

            assertEquals("SELECT * FROM users LIMIT 50 OFFSET 1000000", sql);
        }

        @Test
        void testLimitWithOffset() {
            String sql = PSC.select("*").from("users").limit(20, 10).build().query();
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testMultipleGroupByWithDifferentSortDirections() {
            String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupBy(N.asMap("brand", SortDirection.DESC, "category", SortDirection.ASC))
                    .build()
                    .query();

            // Note: This test shows that each groupBy call replaces the previous one
            // The SQL will only contain the last GROUP BY clause
            assertTrue(sql.contains("GROUP BY brand DESC"));
        }

        @Test
        void testMultiSelectionsValidateInputs() {
            assertThrows(IllegalArgumentException.class, () -> PSC.select(selection(Account.class, "a", "bad\"alias", Arrays.asList("id"), false, null)));
            assertThrows(IllegalArgumentException.class, () -> PSC.select(selection(Account.class, "a", "account", Arrays.asList("   "), false, null)));
            assertThrows(IllegalArgumentException.class, () -> PSC.select(Account.class, "a", "account", (Class<?>) null, "b", "other"));
            assertThrows(IllegalArgumentException.class, () -> PSC.selectFrom(Account.class, "a", "account", (Class<?>) null, "b", "other"));
        }

        // Offset tests
        @Test
        void testOffset() {
            String sql = PSC.select("*").from("users").offset(20).build().query();
            assertTrue(sql.contains("OFFSET") || sql.contains("20"));
        }

        @Test
        void testOffsetRows() {
            String sql = PSC.select("*").from("users").orderBy("id").offsetRows(20).fetchNextRows(10).build().query();

            assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
        }

        // ORDER BY tests
        @Test
        void testOrderBy() {
            String sql = PSC.select("*").from("users").orderBy("name").build().query();
            assertTrue(sql.contains("ORDER BY name"));
        }

        @Test
        void testOrderBy_CollectionWithAscDirection() {
            String sql = PSC.select("*").from("users").orderBy(Arrays.asList("lastName", "firstName"), SortDirection.ASC).build().query();
            assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC", sql);
        }

        @Test
        void testOrderBy_CollectionWithDirection() {
            String sql = PSC.select("*").from("users").orderBy(Arrays.asList("lastName", "firstName"), SortDirection.DESC).build().query();
            assertEquals("SELECT * FROM users ORDER BY last_name DESC, first_name DESC", sql);
        }

        @Test
        void testOrderByAsc() {
            String sql = PSC.select("*").from("users").orderBy("name", SortDirection.ASC).build().query();
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("ASC"));
        }

        @Test
        void testOrderByDesc() {
            String sql = PSC.select("*").from("users").orderBy("created_date", SortDirection.DESC).build().query();
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("DESC"));
        }

        @Test
        void testOrderByWithExpression() {
            String sql = PSC.select("*").from("users").orderBy("CASE WHEN status = 'VIP' THEN 0 ELSE 1 END, name").build().query();

            assertTrue(sql.contains(" ORDER BY case when status = 'VIP' then 0 else 1 end, name"));
        }

        // Preselect tests
        @Test
        void testPreselectDistinct() {
            String sql = PSC.select("status").from("users").selectModifier("DISTINCT").build().query();
            assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        void testRejectedCombinedLimitDoesNotReserveLimitSlot() {
            final SqlBuilder builder = PSC.select("*").from("users").offset(20);

            assertThrows(IllegalStateException.class, () -> builder.limit(10, 30));
            assertFalse(builder.calledOpSet.contains("LIMIT"));
            assertEquals("SELECT * FROM users OFFSET 20", builder.build().query());
        }

        @Test
        void testRejectedCriteriaDoesNotLeaveSelectModifierBehind() {
            SqlBuilder builder = PSC.select("department").from("employees").where("department IS NULL");
            Criteria criteria = Criteria.builder().distinct().where("department IS NOT NULL").build();

            assertThrows(IllegalStateException.class, () -> builder.append(criteria));
            assertEquals("SELECT department FROM employees WHERE department IS NULL", builder.build().query());
        }

        // Basic SELECT tests
        @Test
        void testSelectAll() {
            String sql = PSC.select("*").from("users").build().query();
            assertEquals("SELECT * FROM users", sql);
        }

        @Test
        void testSelectConstants() {
            // Test selecting constants
            assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
            assertEquals("*", AbstractQueryBuilder.ASTERISK);
            assertEquals("DISTINCT", AbstractQueryBuilder.DISTINCT);
            assertEquals("TOP", AbstractQueryBuilder.TOP);

            String sql = PSC.select(AbstractQueryBuilder.COUNT_ALL).from("users").build().query();

            assertEquals("SELECT count(*) FROM users", sql);
        }

        // Static factory tests
        @Test
        void testSelectFactory() {
            SqlBuilder builder = PSC.select("*");
            assertNotNull(builder);
        }

        /**
         * Regression: a modifier set after from() is still inserted right after the current SELECT keyword.
         */
        @Test
        void testSelectModifierAfterFromStillApplies() {
            String sql = PSC.select("name").from("account").distinct().build().query();
            assertEquals("SELECT DISTINCT name FROM account", sql);
        }

        @Test
        void testSelectModifierEmittedVerbatim() {
            // The modifier used to be routed through column-name normalization, camelCasing any
            // non-keyword modifier under PLC (SQL_CALC_FOUND_ROWS -> sqlCalcFoundRows).
            assertEquals("SELECT SQL_CALC_FOUND_ROWS * FROM account",
                    Dsl.PLC.select("*").selectModifier("SQL_CALC_FOUND_ROWS").from("account").build().query());

            // The late-insert path (selectModifier after from) must be verbatim too.
            assertEquals("SELECT SQL_CALC_FOUND_ROWS * FROM account",
                    Dsl.PLC.select("*").from("account").selectModifier("SQL_CALC_FOUND_ROWS").build().query());
        }

        // Collection-based select
        @Test
        void testSelectWithCollection() {
            String sql = PSC.select(Arrays.asList("id", "name", "email")).from("users").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        void testSelectWithFunctions() {
            String sql = PSC.select("MAX(age)", "MIN(age)", "AVG(age)", "COUNT(*)").from("users").groupBy("status").build().query();

            assertTrue(sql.contains("MAX(age)"));
            assertTrue(sql.contains("MIN(age)"));
            assertTrue(sql.contains("AVG(age)"));
            assertTrue(sql.contains("COUNT(*)"));
        }

        // Naming policy tests
        @Test
        void testSelectWithLowerCaseUnderscore() {
            String sql = PSC.select(Account.class).from(Account.class).build().query();
            assertNotNull(sql);
        }

        // Edge case tests
        @Test
        void testSelectWithNoFrom() {
            assertThrows(Exception.class, () -> {
                PSC.select("*").build().query();
            });
        }

        @Test
        void testSelectWithUpperCaseUnderscore() {
            String sql = Dsl.PAC.select(Account.class).from(Account.class).build().query();
            assertNotNull(sql);
        }
    }

    @Nested
    class RenderingContracts {
        @Test
        void testAppend() {
            // With condition
            String sql = PSC.select("*").from("users").append(Filters.and(Filters.gt("age", 18), Filters.lt("age", 65))).build().query();

            assertEquals("SELECT * FROM users WHERE (age > ?) AND (age < ?)", sql);

            // With string (leading space in the argument)
            sql = PSC.select("*").from("users").append(" FOR UPDATE").build().query();

            assertEquals("SELECT * FROM users FOR UPDATE", sql);

            // Without a leading space: a separating space is inserted automatically.
            sql = PSC.select("*").from("users").append("FOR UPDATE").build().query();

            assertEquals("SELECT * FROM users FOR UPDATE", sql);

            // Chained appends never produce a doubled space regardless of leading spaces.
            sql = PSC.select("*").from("users").append("WHERE 1=1").append(" AND status = 'x'").append("ORDER BY name").build().query();

            assertEquals("SELECT * FROM users WHERE 1=1 AND status = 'x' ORDER BY name", sql);
        }

        @Test
        void testAppendConditionHookRendersWhereAndHavingWithoutExtraParentheses() {
            final SqlBuilder where = PSC.select("id").from("people");
            where.appendCondition(new Where(new And(Filters.eq("id", 7))));
            assertSql("SELECT id FROM people WHERE id = ?", List.of(7), where.build());

            final SqlBuilder having = PSC.select("department").from("people").groupBy("department");
            having.appendCondition(new Having(Filters.gt("COUNT(*)", 2)));
            assertSql("SELECT department FROM people GROUP BY department HAVING COUNT(*) > ?", List.of(2), having.build());
        }

        @Test
        void testAppendIf_StringFalse() {
            String sql = PSC.select("*").from("users").appendIf(false, " FOR UPDATE").build().query();
            assertEquals("SELECT * FROM users", sql);
        }

        @Test
        void testAppendIf_StringTrue() {
            String sql = PSC.select("*").from("users").appendIf(true, " FOR UPDATE").build().query();
            assertEquals("SELECT * FROM users FOR UPDATE", sql);
        }

        @Test
        void testAppendIfOrElse_StringFalse() {
            String sql = PSC.select("*").from("users").appendIfOrElse(false, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
            assertEquals("SELECT * FROM users ORDER BY name DESC", sql);
        }

        @Test
        void testAppendIfOrElse_StringTrue() {
            String sql = PSC.select("*").from("users").appendIfOrElse(true, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
            assertEquals("SELECT * FROM users ORDER BY name ASC", sql);
        }

        @Test
        void testAppendRendersEmptyJunctionAsBooleanIdentity() {
            // A junction without sub-conditions is a complete predicate through its Boolean identity
            // (1 = 1 for AND, 1 = 0 for OR), exactly as Junction.toSql renders it; append(Condition) adds
            // the implicit WHERE keyword as for any other predicate.
            assertEquals("SELECT * FROM users WHERE 1 = 0", PSC.select("*").from("users").append(new Junction(Operator.OR)).build().query());
            assertEquals("SELECT * FROM users WHERE 1 = 1", PSC.select("*").from("users").append(new Junction(Operator.AND)).build().query());

            // A blank raw expression is still not a predicate.
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("users").append(Filters.expr(" ")));
            assertTrue(ex.getMessage().contains("a blank expression is not a predicate"), ex.getMessage());
        }

        @Test
        void testAppendWithClause() {
            // Test appending a Clause condition
            String sql = PSC.select("*").from("users").append(Filters.eq("status", "ACTIVE")).build().query();

            assertTrue(sql.contains("status = ?"));
        }

        @Test
        void testAppendWithCriteria() {
            Criteria criteria = Criteria.builder()
                    .where(Filters.eq("status", "ACTIVE"))
                    .groupBy("department")
                    .having(Filters.gt("COUNT(*)", 5))
                    .orderBy("department")
                    .limit(20, 10)
                    .build();

            String sql = PSC.select("department", "COUNT(*)").from("employees").append(criteria).build().query();

            assertTrue(sql.contains("WHERE status = ?"));
            assertTrue(sql.contains("GROUP BY department"));
            assertTrue(sql.contains("HAVING COUNT(*) > ?"));
            assertTrue(sql.contains("ORDER BY department"));
            assertTrue(sql.contains("LIMIT 20 OFFSET 10"));
        }

        @Test
        void testAvg() {
            String sql = PSC.select("AVG(price)").from("products").build().query();
            assertTrue(sql.contains("AVG(price)"));
        }

        @Test
        void testBinaryWithInOperatorRendersAsInClause() {
            // A Binary built via Filters.binary(prop, Operator.IN, collection) must render as a real IN clause
            // (col IN (?, ?, ...)) -- identical to In/NotIn and Binary.toString() -- rather than binding the whole
            // collection as a single parameter (regression: previously produced "status IN ?" bound to the list).
            final SP binaryIn = PSC.select("*").from("users").where(Filters.binary("status", Operator.IN, N.asList("A", "B", "C"))).build();
            final SP realIn = PSC.select("*").from("users").where(Filters.in("status", N.asList("A", "B", "C"))).build();

            assertEquals("SELECT * FROM users WHERE status IN (?, ?, ?)", binaryIn.query());
            assertEquals(realIn.query(), binaryIn.query());
            assertEquals(Arrays.asList("A", "B", "C"), binaryIn.parameters());

            // NOT IN behaves the same way.
            final SP binaryNotIn = PSC.select("*").from("users").where(Filters.binary("status", Operator.NOT_IN, N.asList("A", "B"))).build();

            assertEquals("SELECT * FROM users WHERE status NOT IN (?, ?)", binaryNotIn.query());
            assertEquals(Arrays.asList("A", "B"), binaryNotIn.parameters());
        }

        @Test
        void testCaseInsensitiveKeywords() {
            // Test that SQL keywords are preserved regardless of case
            String sql = PSC.select("*").from("users").where(Filters.expr("select = 1 AND from = 2")).build().query();

            assertTrue(sql.contains("select = 1 AND from = 2"));
        }

        // CASE WHEN tests
        @Test
        void testCaseWhen() {
            String sql = PSC.select("CASE WHEN age < 18 THEN 'minor' ELSE 'adult' END as age_group").from("users").build().query();
            assertTrue(sql.contains(" case when age < 18 then 'minor' else 'adult' end AS age_group "));
        }

        /**
         * Regression: normal chained set() behavior is unchanged.
         */
        @Test
        void testChainedSetWithoutTrailingWhitespaceUnchanged() {
            String sql = PSC.update("users").set("a = 1").set("b").where(Filters.eq("id", 1)).build().query();
            assertEquals("UPDATE users SET a = 1, b = ? WHERE id = ?", sql);
        }

        /**
         * Bug: chained set() calls decided the leading comma by sniffing the buffer's last character,
         * so a raw fragment with trailing whitespace swallowed the comma and produced invalid SQL.
         */
        @Test
        void testChainedSetWithTrailingWhitespaceFragmentKeepsComma() {
            String sql = PSC.update("users").set("a = 1 ").set("b").where(Filters.eq("id", 1)).build().query();
            // The raw fragment's trailing space is preserved verbatim; the comma is what matters here.
            assertEquals("UPDATE users SET a = 1 , b = ? WHERE id = ?", sql);
        }

        @Test
        void testCompleteLiteralSetOperandsCanBeChained() {
            String sql = PSC.select("id")
                    .from("current_records")
                    .union("SELECT id FROM archived_records")
                    .except("SELECT id FROM deleted_records")
                    .build()
                    .query();

            assertEquals("SELECT id FROM current_records UNION SELECT id FROM archived_records EXCEPT SELECT id FROM deleted_records", sql);
        }

        @Test
        void testComplexQuery() {
            String sql = PSC.select("u.id", "u.name", "o.order_number")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .where(Filters.gt("u.age", 18).and(Filters.eq("u.status", "ACTIVE")))
                    .groupBy("u.id", "u.name", "o.order_number")
                    .having(Filters.gt("COUNT(*)", 1))
                    .orderBy("u.name", SortDirection.ASC)
                    .limit(20, 10)
                    .build()
                    .query();

            assertTrue(sql.contains("SELECT u.id, u.name, o.order_number"));
            assertTrue(sql.contains("FROM users u"));
            assertTrue(sql.contains("LEFT JOIN orders o"));
            assertTrue(sql.contains("ON u.id = o.user_id"));
            assertTrue(sql.contains("WHERE (u.age > ?) AND (u.status = ?)"));
            assertTrue(sql.contains("GROUP BY u.id, u.name, o.order_number"));
            assertTrue(sql.contains("HAVING COUNT(*) > ?"));
            assertTrue(sql.contains("ORDER BY u.name ASC"));
            assertTrue(sql.contains("LIMIT 20 OFFSET 10"));
        }

        @Test
        void testComplexQueryWithAllFeatures() {
            final Date createdAfter = new Date(1_700_000_000_000L);
            final SP result = PSC.select("u.id", "u.name", "COUNT(o.id) as order_count", "SUM(o.amount) as total_amount")
                    .from("users u")
                    .leftJoin("orders o")
                    .on(Filters.and(Filters.eq("u.id", "o.user_id"), Filters.eq("o.status", "COMPLETED")))
                    .where(Filters.and(Filters.gt("u.created_date", createdAfter), Filters.in("u.status", Arrays.asList("ACTIVE", "VIP"))))
                    .groupBy("u.id", "u.name")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("total_amount", SortDirection.DESC)
                    .limit(20, 10)
                    .build();

            assertSql(
                    "SELECT u.id, u.name, COUNT(o.id) AS order_count, SUM(o.amount) AS total_amount "
                            + "FROM users u LEFT JOIN orders o ON (u.id = ?) AND (o.status = ?) "
                            + "WHERE (u.created_date > ?) AND (u.status IN (?, ?)) GROUP BY u.id, u.name "
                            + "HAVING COUNT(o.id) > ? ORDER BY total_amount DESC LIMIT 20 OFFSET 10",
                    List.of("o.user_id", "COMPLETED", createdAfter, "ACTIVE", "VIP", 5), result);
        }

        // Constants verification
        @Test
        void testConstants() {
            assertNotNull(AbstractQueryBuilder.ALL);
            assertNotNull(AbstractQueryBuilder.DISTINCT);
            assertNotNull(AbstractQueryBuilder.COUNT_ALL);
            assertEquals("ALL", AbstractQueryBuilder.ALL);
            assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
        }

        @Test
        void testDebugPrint() {
            SqlBuilder builder = PSC.select("*").from("account").where(Filters.between("age", 18, 65));
            builder.debugPrint();
            assertThrows(RuntimeException.class, () -> builder.where(Filters.eq("id", 1)));
        }

        @Test
        void testEqualityInsideArraySubscriptsIsNotTheSetAssignment() {
            final Dsl postgres = Dsl.forDialect(SqlDialect.builder()
                    .productInfo(ProductInfo.of("PostgreSQL"))
                    .namingPolicy(NamingPolicy.SNAKE_CASE)
                    .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
                    .build());

            final SP result = postgres.update("measurements").set("readings[(statusCode = 1)::int]").build();
            assertEquals("UPDATE measurements SET readings[(status_code = 1)::int] = ?", result.query());
            assertTrue(result.parameters().isEmpty());

            final SP assignment = postgres.update("measurements").set("readings[(statusCode = 1)::int] = 0").build();
            assertEquals("UPDATE measurements SET readings[(status_code = 1)::int] = 0", assignment.query());
        }

        @Test
        void testExpressionWithComplexSQL() {
            String sql = PSC.select("*").from("users").where(Filters.expr("DATEDIFF(day, created_date, GETDATE()) > 30")).build().query();

            assertEquals("SELECT * FROM users WHERE DATEDIFF(day, created_date, GETDATE()) > 30", sql);
        }

        @Test
        void testIbatisSqlPolicy() {
            SP sp = Dsl.MSC.update("users").set(Map.of("name", "Alice")).where(Filters.eq("id", 1)).build();

            assertEquals("UPDATE users SET name = #{name} WHERE id = #{id}", sp.query());
            assertEquals(Arrays.asList("Alice", 1), sp.parameters());
        }

        @Test
        void testIsDefaultIdPropValue() {
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(null));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(0L));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(BigInteger.ZERO));
            assertTrue(AbstractQueryBuilder.isDefaultIdPropValue(new BigDecimal(0)));

            assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(1));
            assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(-1));
            assertFalse(AbstractQueryBuilder.isDefaultIdPropValue("0"));
            assertFalse(AbstractQueryBuilder.isDefaultIdPropValue(""));
        }

        @Test
        void testIsNamedSql() {
            assertFalse(PSC.select("*").from("account").isNamedSql());
            assertTrue(NSC.select("*").from("account").isNamedSql());
        }

        @Test
        void testLoadPropNamesByClass() {
            Set<String>[] propNames = AbstractQueryBuilder.loadPropNamesByClass(Account.class);

            assertNotNull(propNames);
            assertEquals(5, propNames.length);

            // propNames[0] - for select, including sub entity properties
            assertTrue(propNames[0].contains("firstName"));
            assertTrue(propNames[0].contains("lastName"));
            assertTrue(propNames[0].contains("email"));
            assertTrue(propNames[0].contains("status"));

            // propNames[1] - for select, no sub entity properties
            assertTrue(propNames[1].contains("firstName"));

            // propNames[2] - for insert with id
            assertTrue(propNames[2].contains("id"));
            assertTrue(propNames[2].contains("createdDate")); // @NonUpdatable
            assertFalse(propNames[2].contains("lastModifiedDate")); // @ReadOnly

            // propNames[3] - for insert without id
            assertFalse(propNames[3].contains("id")); // ID should be excluded

            // propNames[4] - for update
            assertFalse(propNames[4].contains("createdDate")); // @NonUpdatable
            assertFalse(propNames[4].contains("lastModifiedDate")); // @ReadOnly
        }

        @Test
        void testMax() {
            String sql = PSC.select("MAX(price)").from("products").build().query();
            assertTrue(sql.contains("MAX(price)"));
        }

        @Test
        void testMin() {
            String sql = PSC.select("MIN(price)").from("products").build().query();
            assertTrue(sql.contains("MIN(price)"));
        }

        @Test
        void testMysqlSetDetectionEscapesStringsButUsesDoublingForBacktickIdentifiers() {
            final Dsl defaultDialect = setExpressionDsl(null);
            final Dsl standardDialect = setExpressionDsl("PostgreSQL");

            for (final String product : List.of("MySQL", "MariaDB")) {
                final Dsl mysql = setExpressionDsl(product);

                // Synthetic fragments isolate the quote conventions before an assignment operator is established.
                for (final String quoted : List.of("'a\\'=b'", "\"a\\\"=b\"")) {
                    assertSetExpression(mysql, quoted, quoted + " = ?");
                    assertSetExpression(defaultDialect, quoted, quoted);
                    assertSetExpression(standardDialect, quoted, quoted);
                    assertSetExpression(mysql, quoted + " = 1", quoted + " = 1");
                }

                for (final String quotedColumn : List.of("`metric\\=value`", "`metric\\``=value`")) {
                    assertSetExpression(mysql, quotedColumn, quotedColumn + " = ?");
                    assertSetExpression(mysql, quotedColumn + " = 1", quotedColumn + " = 1");
                }

                final String assignment = "`metric\\` = 1";
                assertSetExpression(mysql, assignment, assignment);
            }
        }

        @Test
        void testNamed() {
            Map<String, SqlExpression> result = AbstractQueryBuilder.namedPlaceholders("firstName", "lastName");
            assertEquals(2, result.size());
            assertEquals(Filters.QME, result.get("firstName"));
            assertEquals(Filters.QME, result.get("lastName"));

            List<String> propList = Arrays.asList("email", "status");
            result = AbstractQueryBuilder.namedPlaceholders(propList);
            assertEquals(2, result.size());
            assertEquals(Filters.QME, result.get("email"));
            assertEquals(Filters.QME, result.get("status"));
        }

        @Test
        void testNamedInWithExpressionValue_Pass3() {
            SP sp = NSC.select("id").from("test_account").where(Filters.in("status", Arrays.asList(SqlExpression.of("'ACTIVE'"), "PENDING"))).build();
            assertTrue(sp.query().contains("'ACTIVE'"), "SqlExpression must be embedded in named IN list: " + sp.query());
            assertTrue(sp.query().contains(":"), "Non-SqlExpression value must use named placeholder: " + sp.query());
            assertEquals(1, sp.parameters().size());
            assertEquals("PENDING", sp.parameters().get(0));
        }

        @Test
        void testNamedSQLWithIn() {
            String sql = NSC.select("*").from("users").where(Filters.in("status", Arrays.asList("ACTIVE", "PENDING"))).build().query();

            assertTrue(sql.contains("status IN (:status1, :status2)"));
        }

        @Test
        void testNestedAndQuotedSubscriptContentsDoNotHideTheSetAssignment() {
            final Dsl postgres = Dsl.forDialect(SqlDialect.builder()
                    .productInfo(ProductInfo.of("PostgreSQL"))
                    .namingPolicy(NamingPolicy.SNAKE_CASE)
                    .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
                    .build());
            final Map<String, String> targets = Map.of("readings[CASE WHEN statusCode = ']=(' THEN 1 ELSE 2 END]",
                    "readings[case when status_code = ']=(' then 1 else 2 end]", "readings[(indexes[(statusCode = 1)::int])]",
                    "readings[(indexes[(status_code = 1)::int])]");

            for (final Map.Entry<String, String> target : targets.entrySet()) {
                assertEquals("UPDATE measurements SET " + target.getValue() + " = ?", postgres.update("measurements").set(target.getKey()).build().query());
                assertEquals("UPDATE measurements SET " + target.getValue() + " = 0",
                        postgres.update("measurements").set(target.getKey() + " = 0").build().query());
            }
        }

        @Test
        void testPair() {
            SqlBuilder.SP sp = PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).build();

            assertEquals("SELECT * FROM account WHERE status = ?", sp.query());
            assertEquals(1, sp.parameters().size());
            assertEquals("ACTIVE", sp.parameters().get(0));
        }

        @Test
        void testPreselect() {
            String sql = PSC.select("*").selectModifier("TOP 10").from("account").build().query();

            assertEquals("SELECT TOP 10 * FROM account", sql);

            // Test duplicate selectModifier
            SqlBuilder builder = PSC.select("*").selectModifier("TOP 10");
            assertThrows(IllegalStateException.class, () -> builder.selectModifier("DISTINCT"));
        }

        @Test
        void testQMEAsparameter() {
            // Test using QME (Question Mark SqlExpression) as parameter
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("name", Filters.QME);
            values.put("age", 25);

            String sql = PSC.update("users").set(values).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("name = ?"));
            assertTrue(sql.contains("age = ?"));
        }

        @Test
        void testRawSqlRendersDateFamilyValuesAsLocalWallClockText() {
            // Regression: RAW_SQL inlined java.sql.Date/Time/Timestamp as UTC instants ("...T08:00:00.000Z"), so the
            // database saw a different date/time than the one the parameterized builders bind.
            final String sql = SCSB.select("id")
                    .from("orders")
                    .where(Filters.and(Filters.eq("orderDate", java.sql.Date.valueOf("2020-01-02")),
                            Filters.eq("createdAt", java.sql.Timestamp.valueOf("2020-01-02 03:04:05")),
                            Filters.eq("shipTime", java.sql.Time.valueOf("03:04:05"))))
                    .build()
                    .query();

            assertEquals("SELECT id FROM orders WHERE (order_date = '2020-01-02') AND (created_at = '2020-01-02 03:04:05.0') AND (ship_time = '03:04:05')",
                    sql);
        }

        @Test
        void testRejectedCriteriaWithLaterClauseConflictDoesNotAppendEarlierClauses() {
            final SqlBuilder builder = PSC.select("department").from("employees").orderBy("department");
            final Criteria criteria = Criteria.builder().where(Filters.equal("active", true)).orderBy("id").build();

            assertThrows(IllegalStateException.class, () -> builder.append(criteria));

            final SP result = builder.build();
            assertEquals("SELECT department FROM employees ORDER BY department", result.query());
            assertTrue(result.parameters().isEmpty());
        }

        // ==== Regression tests for the 2026-07-03 review fixes ====

        @Test
        void testSinglePropRowValueInRendersTupleRows() {
            // A single-prop row-value In previously fell into the scalar IN path, binding each tuple List
            // whole as one parameter ("id IN (?, ?)" with params [[1],[2]]; raw mode "IN ('[1]', '[2]')").
            SP sp = PSC.select("*").from("users").where(Filters.in(N.asList("id"), N.asList(N.asList(1), N.asList(2)))).build();
            assertEquals("SELECT * FROM users WHERE (id) IN ((?), (?))", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());

            SP named = NSC.select("*").from("users").where(Filters.in(N.asList("id"), N.asList(N.asList(1), N.asList(2)))).build();
            assertEquals("SELECT * FROM users WHERE (id) IN ((:id1), (:id2))", named.query());
            assertEquals(Arrays.asList(1, 2), named.parameters());

            SP notIn = PSC.select("*").from("users").where(Filters.notIn(N.asList("id"), N.asList(N.asList(1), N.asList(2)))).build();
            assertEquals("SELECT * FROM users WHERE (id) NOT IN ((?), (?))", notIn.query());
            assertEquals(Arrays.asList(1, 2), notIn.parameters());
        }

        @Test
        void testSpecialCharactersInValues() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("name", "O'Brien");
            values.put("comment", "Test \"quote\" handling");

            String sql = PSC.update("users").set(values).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("UPDATE users SET"));
            assertTrue(sql.contains("name = ?"));
            assertTrue(sql.contains("comment = ?"));

            SqlBuilder builder = PSC.update("users").set(values).where(Filters.eq("id", 1));
            builder.build().query();

            List<Object> params = builder.parameters();
            assertEquals("O'Brien", params.get(0));
            assertEquals("Test \"quote\" handling", params.get(1));
        }

        @Test
        void testSql() {
            String sql = PSC.select("id", "name").from("account").where(Filters.gt("age", 18)).build().query();

            assertEquals("SELECT id, name FROM account WHERE age > ?", sql);

            // Test double call throws exception
            SqlBuilder builder = PSC.select("*").from("users");
            builder.build().query();
            assertThrows(RuntimeException.class, () -> builder.build().query());
        }

        // Test SQL policies
        @Test
        void testSQLPolicies() {
            // Parameterized SQL
            String sql = PSC.update("account").set("name").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("name = ?"));
            assertTrue(sql.contains("id = ?"));

            // Named SQL
            sql = NSC.update("account").set("name").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("name = :name"));
            assertTrue(sql.contains("id = :id"));
        }

        @Test
        void testSqlServerRawSqlRendersBooleanAsBit() {
            // Regression: RAW_SQL rendered Boolean as true/false, which T-SQL reads as column names.
            final Dsl sqlServer = Dsl.forDialect(SqlDialect.builder()
                    .productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server"))
                    .namingPolicy(NamingPolicy.SNAKE_CASE)
                    .sqlPolicy(SqlDialect.SqlPolicy.RAW_SQL)
                    .build());

            assertEquals("SELECT id FROM account WHERE (active = 1) AND (deleted = 0)",
                    sqlServer.select("id").from("account").where(Filters.and(Filters.eq("active", true), Filters.eq("deleted", false))).build().query());

            // Other dialects keep the TRUE/FALSE literals.
            assertEquals("SELECT id FROM account WHERE active = true", SCSB.select("id").from("account").where(Filters.eq("active", true)).build().query());
        }

        // Covers the SQL Server Boolean -> 1/0 RAW_SQL rendering at the value call sites beyond WHERE '=': INSERT, batch
        // INSERT, UPDATE SET (its own call site), IN, a raw sub-query binding, and a nested structured sub-query.
        @Test
        @SuppressWarnings("deprecation")
        void testSqlServerRawSqlRendersBooleanAsBitInEveryValuePath() {
            final Dsl sqlServer = Dsl.forDialect(SCSB.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server")).build());

            assertEquals("INSERT INTO t (a) VALUES (1)", sqlServer.insert(Map.of("a", true)).into("t").build().query());
            assertEquals("INSERT INTO t (a) VALUES (1), (0)", sqlServer.batchInsert(List.of(Map.of("a", true), Map.of("a", false))).into("t").build().query());
            assertEquals("UPDATE t SET a = 0 WHERE id = 1", sqlServer.update("t").set(Map.of("a", false)).where(Filters.eq("id", 1)).build().query());
            assertEquals("SELECT id FROM t WHERE a IN (1, 0)", sqlServer.select("id").from("t").where(Filters.in("a", List.of(true, false))).build().query());
            assertEquals("SELECT id FROM t WHERE id IN (SELECT id FROM u WHERE a = 1)",
                    sqlServer.select("id")
                            .from("t")
                            .where(Filters.in("id", Filters.subQuery("SELECT id FROM u WHERE a = ?", Arrays.asList(true))))
                            .build()
                            .query());

            final String nested = sqlServer.select("id")
                    .from("t")
                    .where(Filters.in("id", Filters.subQuery(com.landawn.abacus.query.entity.Account.class, List.of("id"), Filters.eq("status", true))))
                    .build()
                    .query();
            assertTrue(nested.contains("status = 1"), nested);
        }

        @Test
        void testSum() {
            String sql = PSC.select("SUM(amount)").from("orders").build().query();
            assertTrue(sql.contains("SUM(amount)"));
        }

        @Test
        void testToString() {
            String sql = PSC.select("*").from("account").where(Filters.eq("id", 1)).toString();

            assertNotEquals("SELECT * FROM account WHERE id = ?", sql);
        }

        @Test
        void testUnterminatedSetQuotesDoNotEstablishAnAssignment() {
            // An added placeholder does not repair malformed SQL; these assertions specify only assignment detection.
            for (final String product : new String[] { null, "PostgreSQL", "MySQL", "MariaDB", "SQL Server" }) {
                final Dsl dsl = setExpressionDsl(product);

                for (final String expression : List.of("'metric=value", "\"metric=value", "`metric=value", "[metric=value")) {
                    assertSetExpression(dsl, expression, expression + " = ?");
                }

                for (final String expression : List.of("metric = 'unterminated", "metric = \"unterminated", "metric = `unterminated",
                        "metric = [unterminated")) {
                    assertSetExpression(dsl, expression, expression);
                }
            }
        }
    }

    @Nested
    class SubqueriesAndSetOperations {
        @Test
        void testAppendCondition_nullConditionThrowsIllegalArgumentException() {
            final SqlBuilder builder = PSC.renderCondition(Filters.eq("id", 1));

            try {
                // previously threw NullPointerException from the unsupported-type fallback
                assertThrows(IllegalArgumentException.class, () -> builder.appendCondition(null));
            } finally {
                builder.build();
            }
        }

        @Test
        void testBatchInsertUsesOneOuterCollectionSnapshot() {
            final BatchEntityWithId beanRow = new BatchEntityWithId();
            beanRow.setName("correct");

            final Map<String, Object> laterMapRow = new LinkedHashMap<>();
            laterMapRow.put("otherId", 999L);
            laterMapRow.put("name", "wrong");

            final Collection<Object> changingRows = new java.util.AbstractCollection<>() {
                private int iteratorCalls;

                @Override
                public java.util.Iterator<Object> iterator() {
                    return Collections.<Object> singleton(iteratorCalls++ == 0 ? beanRow : laterMapRow).iterator();
                }

                @Override
                public int size() {
                    return 1;
                }
            };

            final SP result = PSC.batchInsert(changingRows).into("batch_entity").build();

            assertEquals("INSERT INTO batch_entity (other_id, name) VALUES (?, ?)", result.query());
            assertEquals(Arrays.asList(0L, "correct"), result.parameters());
        }

        @Test
        void testBinaryWithInOperatorAndSubQueryRendersAsInSubQuery() {
            // A Binary built via Filters.binary(prop, IN, subQuery) must render the sub-query inline in
            // parentheses -- "col IN (SELECT ...)" -- rather than binding the sub-query as a parameter.
            final SP binaryIn = PSC.select("*").from("users").where(Filters.binary("userId", Operator.IN, Filters.subQuery("SELECT id FROM x"))).build();

            assertEquals("SELECT * FROM users WHERE user_id IN (SELECT id FROM x)", binaryIn.query());
            assertTrue(binaryIn.parameters().isEmpty());

            // NOT IN behaves the same way.
            final SP binaryNotIn = PSC.select("*").from("users").where(Filters.binary("userId", Operator.NOT_IN, Filters.subQuery("SELECT id FROM x"))).build();

            assertEquals("SELECT * FROM users WHERE user_id NOT IN (SELECT id FROM x)", binaryNotIn.query());
            assertTrue(binaryNotIn.parameters().isEmpty());
        }

        @Test
        void testBinaryWithSubQueryRhsIsParenthesized() {
            String sql = PSC.select("*").from("users").where(Filters.equal("id", Filters.subQuery("SELECT MAX(user_id) FROM orders"))).build().query();
            assertTrue(sql.contains("id = (SELECT MAX(user_id) FROM orders)"));
        }

        @Test
        void testDslVarargsAreSnapshottedBeforeDeferredRendering() {
            String[] selectColumns = { "firstName" };
            SqlBuilder selectBuilder = PSC.select(selectColumns);
            selectColumns[0] = "lastName";

            String selectSql = selectBuilder.from("account").build().query();
            assertTrue(selectSql.contains("first_name"));
            assertFalse(selectSql.contains("last_name"));

            String[] insertColumns = { "firstName" };
            SqlBuilder insertBuilder = PSC.insert(insertColumns);
            insertColumns[0] = "lastName";

            assertEquals("INSERT INTO account (first_name) VALUES (?)", insertBuilder.into("account").build().query());
        }

        @Test
        void testExcept() {
            SqlBuilder query1 = PSC.select("id", "name").from("users");
            SqlBuilder query2 = PSC.select("id", "name").from("customers");

            String sql = query1.except(query2).build().query();
            assertEquals("SELECT id, name FROM users EXCEPT SELECT id, name FROM customers", sql);

            SqlBuilder self = PSC.select("id").from("users");
            assertThrows(IllegalArgumentException.class, () -> self.except(self));
        }

        @Test
        void testExcept_CollectionOverload() {
            String sql = PSC.select("id", "name").from("users").exceptSelect(Arrays.asList("id", "name")).from("customers").build().query();
            assertEquals("SELECT id, name FROM users EXCEPT SELECT id, name FROM customers", sql);
        }

        @Test
        void testExcept_StringOverload() {
            String sql = PSC.select("id", "name").from("users").except("SELECT id, name FROM customers").build().query();
            assertEquals("SELECT id, name FROM users EXCEPT SELECT id, name FROM customers", sql);
        }

        @Test
        void testFailedCriteriaSetOperationDoesNotRetainIsolationState() {
            final Dsl rejecting = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sql, name) -> {
                if (name.equals("rejected")) {
                    throw new IllegalArgumentException("Rejected test parameter");
                }
                sql.append(':').append(name);
            }).build());
            final SqlBuilder child = rejecting.select("id").from("archived_records");
            final Criteria invalid = Criteria.builder().union(Filters.subQuery("SELECT id FROM older_records")).orderBy(Filters.eq("rejected", 1)).build();

            assertThrows(IllegalArgumentException.class, () -> child.append(invalid));
            assertEquals("SELECT id FROM current_records UNION SELECT id FROM archived_records",
                    NSC.select("id").from("current_records").union(child).build().query());
        }

        @Test
        void testFailedRawSubQueryRenderingRestoresParameterPolicyMetadata() {
            final SubQuery raw = new SubQuery("SELECT id FROM owners WHERE active = ?", List.of(true));
            final SqlBuilder child = PSC.select("id").from("archived_records");

            // The first predicate renders and records positional bindings before the unsupported second
            // predicate fails. Rollback must clear that policy requirement along with the SQL and bindings.
            final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> child.where(Filters.and(Filters.exists(raw), unsupportedCondition())));
            assertTrue(failure.getMessage().contains("Unsupported condition type"), failure.getMessage());

            final SP result = NSC.select("id").from("current_records").where(Filters.eq("status", "current")).union(child).build();
            assertEquals("SELECT id FROM current_records WHERE status = :status UNION SELECT id FROM archived_records", result.query());
            assertEquals(List.of("current"), result.parameters());
        }

        @Test
        void testIncompleteSetOperationSegmentThrowsInsteadOfTruncating() {
            // Previously built "SELECT id FROM t UNION " with the staged columns silently dropped.
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("t").unionSelect(Arrays.asList("id", "name")).build());

            // A non-SELECT sibling builder is rejected up front instead of being staged as a "column list".
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("t").union(PSC.update("t2").set("a")));

            // Completing the segment still works.
            String sql = PSC.select("id").from("t").unionSelect(Arrays.asList("id", "name")).from("t2").build().query();
            assertTrue(sql.startsWith("SELECT id FROM t UNION SELECT "));
            assertTrue(sql.endsWith("FROM t2"));
        }

        @Test
        void testInlinedRawSubQueryBindingsDoNotRestrictParentParameterPolicy() {
            final SubQuery raw = new SubQuery("SELECT id FROM owners WHERE name = ?", List.of("O'Brien"));
            final SqlBuilder child = SCSB.select("id").from("archived_records").where(Filters.exists(raw));

            // RAW_SQL consumes the raw bindings as literals, leaving no placeholders that require the
            // parent to use its policy. Only the parent's named value belongs in the final binding list.
            final SP result = NSC.select("id").from("current_records").where(Filters.eq("status", "current")).union(child).build();
            assertEquals("SELECT id FROM current_records WHERE status = :status UNION SELECT id FROM archived_records "
                    + "WHERE EXISTS (SELECT id FROM owners WHERE name = 'O''Brien')", result.query());
            assertEquals(List.of("current"), result.parameters());
        }

        @Test
        void testIntersect() {
            SqlBuilder query1 = PSC.select("id", "name").from("users");
            SqlBuilder query2 = PSC.select("id", "name").from("customers");

            String sql = query1.intersect(query2).build().query();
            assertEquals("SELECT id, name FROM users INTERSECT SELECT id, name FROM customers", sql);

            SqlBuilder self = PSC.select("id").from("users");
            assertThrows(IllegalArgumentException.class, () -> self.intersect(self));
        }

        @Test
        void testIntersect_CollectionOverload() {
            String sql = PSC.select("id", "name").from("users").intersectSelect(Arrays.asList("id", "name")).from("customers").build().query();
            assertEquals("SELECT id, name FROM users INTERSECT SELECT id, name FROM customers", sql);
        }

        @Test
        void testIntersect_StringOverload() {
            String sql = PSC.select("id", "name").from("users").intersect("SELECT id, name FROM customers").build().query();
            assertEquals("SELECT id, name FROM users INTERSECT SELECT id, name FROM customers", sql);
        }

        @Test
        void testMinus() {
            SqlBuilder query1 = PSC.select("id", "name").from("users");
            SqlBuilder query2 = PSC.select("id", "name").from("customers");

            String sql = query1.minus(query2).build().query();
            assertEquals("SELECT id, name FROM users MINUS SELECT id, name FROM customers", sql);

            SqlBuilder self = PSC.select("id").from("users");
            assertThrows(IllegalArgumentException.class, () -> self.minus(self));
        }

        @Test
        void testMinus_CollectionOverload() {
            String sql = PSC.select("id", "name").from("users").minusSelect(Arrays.asList("id", "name")).from("customers").build().query();
            assertEquals("SELECT id, name FROM users MINUS SELECT id, name FROM customers", sql);
        }

        @Test
        void testMinus_StringOverload() {
            String sql = PSC.select("id", "name").from("users").minus("SELECT id, name FROM customers").build().query();
            assertEquals("SELECT id, name FROM users MINUS SELECT id, name FROM customers", sql);
        }

        @Test
        void testMixedLargeInPreservesSubQueryAndMarkerPositionsBetweenRuns() {
            final List<Object> values = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                values.add(i);
            }
            values.set(0, Filters.QME);
            values.set(5, new SubQuery("SELECT value FROM archive WHERE id = ?", Collections.singletonList(100)));
            values.set(16, SqlExpression.of("42"));
            values.set(31, Filters.QME);
            final SP result = PSC.select("id").from("users").where(Filters.in("id", values)).build();
            final List<Object> expectedBindings = new ArrayList<>();
            final List<String> rendered = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                if (i == 5) {
                    rendered.add("(SELECT value FROM archive WHERE id = ?)");
                    expectedBindings.add(100);
                } else if (i == 16) {
                    rendered.add("42");
                } else {
                    rendered.add("?");
                    if (i != 0 && i != 31) {
                        expectedBindings.add(i);
                    }
                }
            }
            assertEquals("SELECT id FROM users WHERE id IN (" + String.join(", ", rendered) + ")", result.query());
            assertEquals(expectedBindings, result.parameters());

            final SqlBuilder literals = PSC.select("id").from("users").where(Filters.in("id", Collections.nCopies(32, SqlExpression.of("42"))));
            assertFalse(literals._hasGeneratedParameterPlaceholder);
            assertTrue(literals.build().parameters().isEmpty());
            final SqlBuilder markers = PSC.select("id").from("users").where(Filters.in("id", Collections.nCopies(32, Filters.QME)));
            assertTrue(markers._hasGeneratedParameterPlaceholder);
            assertTrue(markers.build().parameters().isEmpty());
        }

        @Test
        void testNamedParameterHandlerPropagatesIntoStructuredSubQuery() {
            final Dsl dsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
            final SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("userId"), Filters.eq("status", "OPEN"));
            final SP sp = dsl.select("id").from("users").where(Filters.in("id", subQuery)).build();

            assertEquals("SELECT id FROM users WHERE id IN (SELECT user_id AS \"userId\" FROM orders WHERE status = ${status})", sp.query());
            assertEquals(Arrays.asList("OPEN"), sp.parameters());
        }

        @Test
        void testNamedParameters_InSubQueryAndOuterConditionsUseUniqueNames_Pass4() {
            SubQuery innerSubQuery = Filters.subQuery("orders", Arrays.asList("user_id"), Filters.eq("status", "SHIPPED"));
            SP sp = NSC.select("id")
                    .from("users")
                    .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.in("id", innerSubQuery), Filters.eq("status", "PENDING")))
                    .build();

            assertEquals(
                    "SELECT id FROM users WHERE (status = :status) AND (id IN (SELECT user_id FROM orders WHERE status = :status_2)) AND (status = :status_3)",
                    sp.query());
            assertEquals(Arrays.asList("ACTIVE", "SHIPPED", "PENDING"), sp.parameters());
        }

        @Test
        void testNamedPlaceholdersNullThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.namedPlaceholders((String[]) null));
            assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.namedPlaceholders((Collection<String>) null));
        }

        @Test
        void testNestedCompoundSnapshotDoesNotRequireOuterBranchIsolation() {
            final SubQuery owners = PSC.select("id")
                    .from("owners")
                    .where(Filters.eq("active", true))
                    .union(PSC.select("id").from("former_owners").where(Filters.eq("active", false)))
                    .orderBy("id")
                    .limit(1)
                    .toSubQuery();
            final SqlBuilder child = PSC.select("id").from("archived_records").where(Filters.in("owner_id", owners));

            // The IN subquery's parentheses already contain its UNION/ORDER BY/LIMIT. Its bindings must
            // propagate to the outer branch, but its top-level isolation requirement must stay inside IN.
            final SP result = PSC.select("id").from("current_records").union(child).build();
            assertEquals(
                    "SELECT id FROM current_records UNION SELECT id FROM archived_records WHERE owner_id IN "
                            + "(SELECT id FROM owners WHERE active = ? UNION SELECT id FROM former_owners WHERE active = ? ORDER BY id LIMIT 1)",
                    result.query());
            assertEquals(List.of(true, false), result.parameters());
        }

        @Test
        void testNullEntityClassTableNameHelpersThrowIllegalArgumentException() {
            // previously threw NullPointerException from the ConcurrentHashMap cache lookup
            assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.getTableName(null, NamingPolicy.SNAKE_CASE));
            assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.tableAliasOrName(null, NamingPolicy.SNAKE_CASE));
            assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.tableAliasOrName(null, null, NamingPolicy.SNAKE_CASE));
            // an explicit alias short-circuits before the entity class is consulted
            assertEquals("custom", AbstractQueryBuilder.tableAliasOrName("custom", null, NamingPolicy.SNAKE_CASE));

            // the cached property-name helpers report the caller's parameter name, not abacus-common's internal 'cls'
            final IllegalArgumentException propNamesEx = assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.loadPropNamesByClass(null));
            assertTrue(propNamesEx.getMessage().contains("entityClass"));
            final IllegalArgumentException subEntityEx = assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.getSubEntityPropNames(null));
            assertTrue(subEntityEx.getMessage().contains("entityClass"));
        }

        @Test
        void testOwnedInsertSnapshotStillIsolatesCallerMapsBeansAndExclusions() {
            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("firstName", "before");
            input.put("optional", null);
            input.put("lastName", "excluded");
            final Set<String> excluded = new java.util.HashSet<>(Collections.singleton("lastName"));
            // Force the Object overload whose factory creates the owned snapshot.
            final SqlBuilder builder = NSC.insert(input, excluded);
            input.clear();
            input.put("bad--column", "after");
            excluded.add("firstName");
            final SP result = builder.into("users").build();
            assertEquals("INSERT INTO users (first_name, optional) VALUES (:firstName, :optional)", result.query());
            assertEquals(Arrays.asList("before", null), result.parameters());

            final BatchEntityWithId bean = new BatchEntityWithId();
            bean.setName("before");
            bean.setOtherId(7);
            final SqlBuilder beanBuilder = NSC.insert(bean);
            bean.setName("after");
            bean.setOtherId(9);
            final SP beanResult = beanBuilder.into("users").build();
            assertEquals("INSERT INTO users (other_id, name) VALUES (:otherId, :name)", beanResult.query());
            assertEquals(Arrays.asList(7L, "before"), beanResult.parameters());
        }

        /**
         * Extra: BETWEEN combined with subquery, exercising Between's two parameters mixed
         * with subquery parameters.
         */
        @Test
        void testParamOrder_BetweenMixedWithSubQuery_Pass4() {
            SubQuery sq = Filters.subQuery("blacklist", Arrays.asList("user_id"), Filters.eq("region", "EU"));
            SP sp = PSC.select("id").from("users").where(Filters.notIn("id", sq).and(Filters.between("age", 18, 65))).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList("EU", 18, 65), sp.parameters(), "Expected [EU, 18, 65] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        /**
         * Scenario 2 — WHERE with subquery containing parameter + outer WHERE parameter.
         */
        @Test
        void testParamOrder_InSubQueryThenAndBinary_Pass4() {
            SubQuery innerSubQuery = Filters.subQuery("departments", Arrays.asList("id"), Filters.eq("region", "EU"));
            SP sp = PSC.select("id").from("users").where(Filters.in("department_id", innerSubQuery).and(Filters.gt("age", 21))).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList("EU", 21), sp.parameters(), "Expected [EU, 21] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        /**
         * Scenario 7 — InSubQuery + AND + Binary.
         */
        @Test
        void testParamOrder_InSubQueryThenAndStatus_Pass4() {
            SubQuery adminSQ = Filters.subQuery("admins", Arrays.asList("id"), Filters.gt("level", 3));
            SP sp = PSC.select("id").from("test_account").where(Filters.in("user_id", adminSQ).and(Filters.eq("status", "active"))).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList(3, "active"), sp.parameters(), "Expected [3, active] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        /**
         * Extra: OR junction with subquery on left, binary on right — verifies the OR path
         * (different from AND) keeps parameter order matching SQL left-to-right.
         */
        @Test
        void testParamOrder_OrJunctionWithSubQueryAndBinary_Pass4() {
            SubQuery sq = Filters.subQuery("admins", Arrays.asList("id"), Filters.eq("rank", "S"));
            SP sp = PSC.select("id").from("users").where(Filters.in("id", sq).or(Filters.eq("vip", true))).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList("S", true), sp.parameters(), "Expected [S, true] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        /**
         * Scenario 3 — Subquery in WHERE plus outer WHERE parameter, then ensure the structured
         * subquery's WHERE condition's parameters precede the outer condition's parameter.
         * (The library does not support correlated SELECT subqueries directly; we exercise the
         *  equivalent ordering risk through WHERE-with-subquery + AND-with-binary.)
         */
        @Test
        void testParamOrder_StructuredSubQueryConditionBeforeOuter_Pass4() {
            SubQuery sq = Filters.subQuery("orders", Arrays.asList("user_id"), Filters.eq("status", "active").and(Filters.gt("amount", 50)));
            SP sp = PSC.select("id").from("users").where(Filters.in("id", sq).and(Filters.gt("age", 18))).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList("active", 50, 18), sp.parameters(), "Expected [active, 50, 18] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        /**
         * Scenario 8 — Criteria with WHERE containing subquery + ORDER BY + LIMIT.
         */
        @Test
        void testParamOrder_WhereSubQueryOrderByLimit_Pass4() {
            SubQuery vipSQ = Filters.subQuery("vip_users", Arrays.asList("id"), Filters.eq("tier", "GOLD"));
            Criteria crit = Criteria.builder().where(Filters.in("user_id", vipSQ).and(Filters.gt("score", 90))).orderBy("score DESC").limit(10).build();
            SP sp = PSC.select("id").from("test_account").append(crit).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList("GOLD", 90), sp.parameters(), "Expected [GOLD, 90] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        /**
         * Extra: Subquery used inside HAVING after WHERE with parameter. Verifies parameters
         * from the outer WHERE precede parameters originating from HAVING's nested subquery.
         */
        @Test
        void testParamOrder_WhereThenHavingWithSubQuery_Pass4() {
            SubQuery sq = Filters.subQuery("eligible_depts", Arrays.asList("dept_id"), Filters.eq("active", true));
            Criteria crit = Criteria.builder()
                    .where(Filters.eq("status", "ACTIVE"))
                    .groupBy("dept_id")
                    .having(Filters.in("dept_id", sq).and(Filters.gt("COUNT(*)", 3)))
                    .build();
            SP sp = PSC.select("dept_id", "COUNT(*)").from("employees").append(crit).build();

            assertEquals(countQmarks(sp.query()), sp.parameters().size(),
                    "Number of '?' placeholders must equal parameter list size; SQL: " + sp.query() + " params: " + sp.parameters());
            assertEquals(Arrays.asList("ACTIVE", true, 3), sp.parameters(), "Expected [ACTIVE, true, 3] but got " + sp.parameters() + " SQL=" + sp.query());
        }

        @Test
        void testRawSqlInlinesDateFamilyBindingsOfRawSubQueryAndCalendarAtJvmZone() {
            // Covers the RAW_SQL raw-subquery inlining path for date-family bindings and a Calendar rendered at its instant in the
            // JVM zone (as abacus binds it: new Timestamp(calendar.getTimeInMillis())), not in the calendar's own zone.
            assertEquals("SELECT id FROM t WHERE id IN (SELECT x FROM y WHERE z = '2020-01-02')",
                    SCSB.select("id")
                            .from("t")
                            .where(Filters.in("id", Filters.subQuery("SELECT x FROM y WHERE z = ?", Arrays.asList(java.sql.Date.valueOf("2020-01-02")))))
                            .build()
                            .query());
            assertEquals("SELECT id FROM t WHERE id IN (SELECT x FROM y WHERE z = '2020-01-02 03:04:05.5')",
                    SCSB.select("id")
                            .from("t")
                            .where(Filters.in("id",
                                    Filters.subQuery("SELECT x FROM y WHERE z = ?", Arrays.asList(java.sql.Timestamp.valueOf("2020-01-02 03:04:05.5")))))
                            .build()
                            .query());

            final java.util.TimeZone defaultZone = java.util.TimeZone.getDefault();

            try {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

                final java.util.Calendar tokyo = new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
                tokyo.clear();
                tokyo.set(2020, java.util.Calendar.JANUARY, 2, 3, 4, 5);

                // 2020-01-02 03:04:05 in Tokyo is 2020-01-01 18:04:05 in UTC.
                assertEquals("SELECT id FROM t WHERE c = '2020-01-01 18:04:05.0'", SCSB.select("id").from("t").where(Filters.eq("c", tokyo)).build().query());
                assertEquals("SELECT id FROM t WHERE id IN (SELECT x FROM y WHERE z = '2020-01-01 18:04:05.0')",
                        SCSB.select("id")
                                .from("t")
                                .where(Filters.in("id", Filters.subQuery("SELECT x FROM y WHERE z = ?", Arrays.asList(tokyo))))
                                .build()
                                .query());
            } finally {
                java.util.TimeZone.setDefault(defaultZone);
            }
        }

        @Test
        void testRawSubQueryBindingsPreventMixedPoliciesInDerivedTables() {
            final SubQuery activeOwners = new SubQuery("SELECT id FROM owners WHERE active = ?", Arrays.asList(true));
            final SqlBuilder parent = NSC.select("id");
            final SqlBuilder child = PSC.select("id").from("archived_records").where(Filters.in("owner_id", activeOwners));

            assertThrows(IllegalArgumentException.class, () -> parent.from(child, "archived"));
            assertEquals("SELECT id FROM current_records", parent.from("current_records").build().query());
            assertEquals(Arrays.asList(true), child.build().parameters());
        }

        @Test
        void testRawSubQueryBindingsPreventMixedPoliciesInSiblingSetOperations() {
            final SubQuery activeOwners = new SubQuery("SELECT id FROM owners WHERE active = ?", Arrays.asList(true));
            final SqlBuilder left = NSC.select("id").from("current_records");
            final SqlBuilder right = PSC.select("id").from("archived_records").where(Filters.in("owner_id", activeOwners));

            assertThrows(IllegalArgumentException.class, () -> left.union(right));
            assertEquals("SELECT id FROM current_records", left.build().query());
            final SP rightSql = right.build();
            assertEquals("SELECT id FROM archived_records WHERE owner_id IN (SELECT id FROM owners WHERE active = ?)", rightSql.query());
            assertEquals(Arrays.asList(true), rightSql.parameters());
        }

        @Test
        void testRawSubQueryCompactExpressionsValidateAndRewriteEveryBinding() {
            final String[][] cases = { { "SELECT ?||?||?", "SELECT :param||:param_2||:param_3", "SELECT #{param}||#{param_2}||#{param_3}" },
                    { "SELECT ?-?-?", "SELECT :param-:param_2-:param_3", "SELECT #{param}-#{param_2}-#{param_3}" },
                    { "SELECT ARRAY[?||?||?]", "SELECT ARRAY[:param||:param_2||:param_3]", "SELECT ARRAY[#{param}||#{param_2}||#{param_3}]" },
                    { "SELECT ARRAY[?-?-?]", "SELECT ARRAY[:param-:param_2-:param_3]", "SELECT ARRAY[#{param}-#{param_2}-#{param_3}]" } };

            for (final String[] sql : cases) {
                final List<Object> bindings = sql[0].contains("||") ? List.of("a", "b", "c") : List.of(3, 2, 1);
                final SubQuery subQuery = new SubQuery(sql[0], bindings);

                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql[0]), sql[0]);
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql[0], bindings.subList(0, 1)), sql[0]);
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql[0], bindings.subList(0, 2)), sql[0]);

                final Dsl[] policies = { PSC, NSC, MSC };

                for (int policy = 0; policy < policies.length; policy++) {
                    final SP result = policies[policy].select("id").from("records").where(Filters.exists(subQuery)).build();
                    assertEquals("SELECT id FROM records WHERE EXISTS (" + sql[policy] + ")", result.query(), sql[0]);
                    assertEquals(bindings, result.parameters(), sql[0]);
                }
            }
        }

        @Test
        void testRawSubQueryJdbcEscapedOperatorsArePreserved() {
            for (final String sql : List.of("SELECT 1 FROM documents WHERE doc ?? 'k'", "SELECT ARRAY[doc ?? 'k'] FROM documents",
                    "SELECT doc ??| keys FROM documents", "SELECT doc ??& keys FROM documents", "SELECT doc @?? path FROM documents")) {
                final SubQuery noBindings = new SubQuery(sql);
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql, List.of("extra")), sql);
                for (final Dsl dsl : List.of(PSC, NSC, MSC)) {
                    final SP result = dsl.select("id").from("records").where(Filters.exists(noBindings)).build();
                    assertEquals("SELECT id FROM records WHERE EXISTS (" + sql + ")", result.query(), sql);
                    assertTrue(result.parameters().isEmpty(), sql);
                }
            }

            final String[][] cases = {
                    { "SELECT 1 FROM documents WHERE doc ?? 'k' AND id = ?", "SELECT 1 FROM documents WHERE doc ?? 'k' AND id = :param",
                            "SELECT 1 FROM documents WHERE doc ?? 'k' AND id = #{param}" },
                    { "SELECT doc ??? FROM documents", "SELECT doc ??:param FROM documents", "SELECT doc ??#{param} FROM documents" },
                    { "SELECT ARRAY[doc ?? ?] FROM documents", "SELECT ARRAY[doc ?? :param] FROM documents", "SELECT ARRAY[doc ?? #{param}] FROM documents" },
                    { "SELECT doc ??| ARRAY[?] FROM documents", "SELECT doc ??| ARRAY[:param] FROM documents",
                            "SELECT doc ??| ARRAY[#{param}] FROM documents" },
                    { "SELECT doc @??? FROM documents", "SELECT doc @??:param FROM documents", "SELECT doc @??#{param} FROM documents" },
                    { "SELECT ARRAY[doc ??& ?] FROM documents", "SELECT ARRAY[doc ??& :param] FROM documents",
                            "SELECT ARRAY[doc ??& #{param}] FROM documents" } };
            for (final String[] sql : cases) {
                final SubQuery subQuery = new SubQuery(sql[0], List.of("key"));
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql[0]), sql[0]);
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql[0], List.of("key", "extra")), sql[0]);
                final Dsl[] policies = { PSC, NSC, MSC };
                for (int policy = 0; policy < policies.length; policy++) {
                    final SP result = policies[policy].select("id").from("records").where(Filters.exists(subQuery)).build();
                    assertEquals("SELECT id FROM records WHERE EXISTS (" + sql[policy] + ")", result.query(), sql[0]);
                    assertEquals(List.of("key"), result.parameters(), sql[0]);

                    // Generated MyBatis SQL must remain parseable even when its opener touches the escaped operator.
                    final ParsedSql reparsed = ParsedSql.parse(result.query());
                    assertEquals(1, reparsed.parameterCount(), result.query());
                    assertEquals(policy == 0 ? List.of() : List.of("param"), reparsed.namedParameters(), result.query());
                    assertEquals("SELECT id FROM records WHERE EXISTS (" + sql[0] + ")", reparsed.parameterizedSql(), result.query());

                    if (policy != 0) {
                        // A raw SubQuery cannot accept unresolved named/MyBatis bindings, with or without supplied values.
                        assertThrows(IllegalArgumentException.class, () -> new SubQuery(result.query()), result.query());
                        assertThrows(IllegalArgumentException.class, () -> new SubQuery(result.query(), List.of("key")), result.query());
                    }
                }
            }
        }

        @Test
        void testRawSubQueryJsonOperatorsAreNotRewrittenAsBindings() {
            final String[][] cases = {
                    { "SELECT doc ? format('%s', ?) FROM documents", "SELECT doc ? format('%s', :param) FROM documents",
                            "SELECT doc ? format('%s', #{param}) FROM documents" },
                    { "SELECT ARRAY[doc ? format('%s', ?)] FROM documents", "SELECT ARRAY[doc ? format('%s', :param)] FROM documents",
                            "SELECT ARRAY[doc ? format('%s', #{param})] FROM documents" },
                    { "SELECT doc ?| ARRAY[?] FROM documents", "SELECT doc ?| ARRAY[:param] FROM documents", "SELECT doc ?| ARRAY[#{param}] FROM documents" } };

            for (final String[] sql : cases) {
                final SubQuery subQuery = new SubQuery(sql[0], List.of("key"));
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql[0]), sql[0]);
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql[0], List.of("key", "extra")), sql[0]);

                final Dsl[] policies = { PSC, NSC, MSC };

                for (int policy = 0; policy < policies.length; policy++) {
                    final SP result = policies[policy].select("id").from("records").where(Filters.exists(subQuery)).build();
                    assertEquals("SELECT id FROM records WHERE EXISTS (" + sql[policy] + ")", result.query(), sql[0]);
                    assertEquals(List.of("key"), result.parameters(), sql[0]);
                }
            }

            for (final String sql : List.of("SELECT doc ? NULL FROM documents", "SELECT ARRAY[doc ? NULL] FROM documents",
                    "SELECT doc ? format('%s', 'key') FROM documents")) {
                final SubQuery subQuery = new SubQuery(sql);
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql, List.of("extra")), sql);

                for (final Dsl dsl : List.of(PSC, NSC, MSC)) {
                    final SP result = dsl.select("id").from("records").where(Filters.exists(subQuery)).build();
                    assertEquals("SELECT id FROM records WHERE EXISTS (" + sql + ")", result.query(), sql);
                    assertTrue(result.parameters().isEmpty(), sql);
                }
            }
        }

        @Test
        void testRawSubQueryPlaceholderGluedToDotIsSeparated() {
            // "?.5" must not be rewritten into the literal "1.5" (a WRONG VALUE in valid SQL) nor into ":param.5";
            // "?.x" would even re-parse as the parameter NAME "param.x", losing the name the builder registered
            final SubQuery dotDigit = new SubQuery("SELECT id FROM t WHERE a=?.5 AND b=?", Arrays.asList(1, 2));
            assertTrue(SCSB.select("*").from("x").where(Filters.in("id", dotDigit)).build().query().contains("a=1 .5 AND b=2"),
                    SCSB.select("*").from("x").where(Filters.in("id", dotDigit)).build().query());
            assertTrue(NSC.select("*").from("x").where(Filters.in("id", dotDigit)).build().query().contains("a=:param .5 AND b=:param_2"),
                    NSC.select("*").from("x").where(Filters.in("id", dotDigit)).build().query());
            assertTrue(MSC.select("*").from("x").where(Filters.in("id", dotDigit)).build().query().contains("a=#{param} .5 AND b=#{param_2}"),
                    MSC.select("*").from("x").where(Filters.in("id", dotDigit)).build().query());

            final SubQuery dotName = new SubQuery("SELECT id FROM t WHERE a=?.x AND b=?", Arrays.asList(1, 2));
            final SP named = NSC.select("*").from("x").where(Filters.in("id", dotName)).build();
            assertTrue(named.query().contains("a=:param .x AND b=:param_2"), named.query());
            assertEquals(Arrays.asList("param", "param_2"), ParsedSql.parse(named.query()).namedParameters());

            // controls: the digit and keyword glue cases the guard already covered
            final SubQuery digit = new SubQuery("SELECT id FROM t WHERE a=?5 AND b=?", Arrays.asList(1, 2));
            assertTrue(SCSB.select("*").from("x").where(Filters.in("id", digit)).build().query().contains("a=1 5 AND b=2"),
                    SCSB.select("*").from("x").where(Filters.in("id", digit)).build().query());
            final SubQuery keyword = new SubQuery("SELECT id FROM t WHERE a=?AND b=?", Arrays.asList(1, 2));
            assertTrue(NSC.select("*").from("x").where(Filters.in("id", keyword)).build().query().contains("a=:param AND b=:param_2"),
                    NSC.select("*").from("x").where(Filters.in("id", keyword)).build().query());
        }

        @Test
        void testRawSubQueryPlaceholderGluedToKeywordIsSeparatedUnderNamedSql() {
            // "?AND" must not be rewritten into ":paramAND" (named) or "1AND" (inlined)
            final SubQuery raw = new SubQuery("SELECT id FROM t WHERE a=?AND b=?", Arrays.asList(1, 2));

            final SP named = NSC.select("*").from("x").where(Filters.in("id", raw)).build();
            assertTrue(named.query().contains("a=:param AND b=:param_2"), named.query());
            assertEquals(Arrays.asList(1, 2), named.parameters());

            final SP inlined = SCSB.select("*").from("x").where(Filters.in("id", raw)).build();
            assertTrue(inlined.query().contains("a=1 AND b=2"), inlined.query());

            // an ordinary placeholder followed by whitespace is rendered exactly as before
            final SP spaced = NSC.select("*")
                    .from("x")
                    .where(Filters.in("id", new SubQuery("SELECT id FROM t WHERE a = ? AND b = ?", Arrays.asList(1, 2))))
                    .build();
            assertTrue(spaced.query().contains("a = :param AND b = :param_2"), spaced.query());

            // a keyword glued to the LEFT of a placeholder ("AND?=b") is separated as well
            final SubQuery leftGlued = new SubQuery("SELECT id FROM t WHERE a=?AND?=b", Arrays.asList(1, 2));
            final SP leftNamed = NSC.select("*").from("x").where(Filters.in("id", leftGlued)).build();
            assertTrue(leftNamed.query().contains("a=:param AND :param_2=b"), leftNamed.query());
            final SP leftInlined = SCSB.select("*").from("x").where(Filters.in("id", leftGlued)).build();
            assertTrue(leftInlined.query().contains("a=1 AND 2=b"), leftInlined.query());

            // IBATIS rewrites the same way
            final SP ibatis = MSC.select("*").from("x").where(Filters.in("id", raw)).build();
            assertTrue(ibatis.query().contains("a=#{param} AND b=#{param_2}"), ibatis.query());

            // control: PARAMETERIZED_SQL keeps the '?' placeholders, so the text is passed through untouched
            final SP passThrough = PSC.select("*").from("x").where(Filters.in("id", raw)).build();
            assertTrue(passThrough.query().contains("a=?AND b=?"), passThrough.query());
            assertEquals(Arrays.asList(1, 2), passThrough.parameters());
        }

        @Test
        void testRawSubQueryUnaryGeometricOperatorsPreserveTypedOperands() {
            final String[][] cases = { { "SELECT ?- ?::line", "SELECT ?- :param::line", "SELECT ?- #{param}::line" },
                    { "SELECT ?| ?::lseg", "SELECT ?| :param::lseg", "SELECT ?| #{param}::lseg" },
                    { "SELECT ?- (?::line)", "SELECT ?- (:param::line)", "SELECT ?- (#{param}::line)" },
                    { "SELECT ARRAY[?- ?::line]", "SELECT ARRAY[?- :param::line]", "SELECT ARRAY[?- #{param}::line]" },
                    { "SELECT ARRAY[?| ?::lseg]", "SELECT ARRAY[?| :param::lseg]", "SELECT ARRAY[?| #{param}::lseg]" } };
            final List<String> bindings = List.of("(0,0),(1,0)");

            for (final String[] sql : cases) {
                final SubQuery subQuery = new SubQuery(sql[0], bindings);
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql[0]), sql[0]);
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql[0], List.of("first", "second")), sql[0]);

                final Dsl[] policies = { PSC, NSC, MSC };

                for (int policy = 0; policy < policies.length; policy++) {
                    final SP result = policies[policy].select("id").from("records").where(Filters.exists(subQuery)).build();
                    assertEquals("SELECT id FROM records WHERE EXISTS (" + sql[policy] + ")", result.query(), sql[0]);
                    assertEquals(bindings, result.parameters(), sql[0]);
                }
            }

            for (final String sql : List.of("SELECT ?- line(point(0, 0), point(1, 0))", "SELECT ?| lseg(point(0, 0), point(0, 1))",
                    "SELECT ARRAY[?- line(point(0, 0), point(1, 0))]", "SELECT ARRAY[?| lseg(point(0, 0), point(0, 1))]")) {
                final SubQuery subQuery = new SubQuery(sql);
                assertThrows(IllegalArgumentException.class, () -> new SubQuery(sql, List.of("extra")), sql);

                for (final Dsl dsl : List.of(PSC, NSC, MSC)) {
                    final SP result = dsl.select("id").from("records").where(Filters.exists(subQuery)).build();
                    assertEquals("SELECT id FROM records WHERE EXISTS (" + sql + ")", result.query(), sql);
                    assertTrue(result.parameters().isEmpty(), sql);
                }
            }
        }

        @Test
        void testRejectedExceptChildCanBeIsolatedWithoutFlatteningItsOperators() {
            final SqlBuilder parent = PSC.select("id").from("current_records");
            final SqlBuilder child = PSC.select("id").from("archived_records").except(PSC.select("id").from("older_records"));

            assertThrows(IllegalArgumentException.class, () -> parent.except(child));

            // Both builders remain usable; the derived table preserves A EXCEPT (B EXCEPT C).
            final SP result = parent.except(PSC.select("id").from(child, "branch")).build();
            assertEquals(
                    "SELECT id FROM current_records EXCEPT SELECT id FROM " + "(SELECT id FROM archived_records EXCEPT SELECT id FROM older_records) branch",
                    result.query());
            assertTrue(result.parameters().isEmpty());
        }

        /**
         * Bug: a select modifier set after a set operation was inserted into the FIRST segment's SELECT
         * (raw indexOf-based insertion) and then also emitted for the new segment.
         */
        @Test
        void testSelectModifierAfterSetOperationAppliesToNewSegmentOnly() {
            String sql = PSC.select("id").from("users").unionSelect(Collections.singletonList("id")).distinct().from("customers").build().query();
            assertEquals("SELECT id FROM users UNION SELECT DISTINCT id FROM customers", sql);
        }

        @Test
        void testSelectModifierAfterVerbatimSetOperationThrows() {
            // DISTINCT staged after union("SELECT ...") used to be dropped silently: there is no SELECT
            // keyword to attach it to and no from() can legally follow the verbatim sub-query.
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("t1").union("SELECT id FROM t2").distinct().build());

            // A modifier staged before a column-list set operation is still rendered by the follow-up from().
            assertEquals("SELECT id FROM t1 UNION SELECT DISTINCT id, name FROM t2",
                    PSC.select("id").from("t1").unionSelect(Arrays.asList("id", "name")).distinct().from("t2").build().query());
        }

        /**
         * Bug: a select modifier set on the first segment leaked into every following set-operation
         * segment (DISTINCT silently applied to the UNION ALL branch, changing query semantics).
         */
        @Test
        void testSelectModifierDoesNotLeakIntoSetOperationSegments() {
            String sql = PSC.select("id").distinct().from("a_tbl").unionAllSelect(Collections.singletonList("id")).from("b_tbl").build().query();
            assertEquals("SELECT DISTINCT id FROM a_tbl UNION ALL SELECT id FROM b_tbl", sql);
        }

        @Test
        void testSetOperationColumnsAreSnapshottedBeforeDeferredRendering() {
            List<String> unionColumns = new ArrayList<>(Arrays.asList("legacyId"));
            SqlBuilder builder = PSC.select("id").from("current_records").unionSelect(unionColumns);
            unionColumns.set(0, "mutatedId");

            String sql = builder.from("legacy_records").build().query();
            assertTrue(sql.contains("legacy_id"));
            assertFalse(sql.contains("mutated_id"));
        }

        @Test
        void testSetOperationCrossBaseNamedParameterCollision() {
            // Parent's literal property "id_2" used to collide with the child's generated "id" + "_2" suffix,
            // binding the same :id_2 placeholder to two different values.
            SqlBuilder child = NSC.select("id").from("t2").where(Filters.and(Filters.equal("id", 1), Filters.equal("id", 2)));
            SP sp = NSC.select("id").from("t1").where(Filters.equal("id_2", 100)).union(child).build();

            List<String> names = new ArrayList<>();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(":([A-Za-z0-9_]+)").matcher(sp.query());
            while (m.find()) {
                names.add(m.group(1));
            }

            assertEquals(3, names.size());
            assertEquals(3, new HashSet<>(names).size()); // every placeholder unique across the compound query
            assertEquals(Arrays.asList(100, 1, 2), sp.parameters());
        }

        @Test
        void testSetOperationNamedParameterTokenSafety() {
            SP combined = NSC.select("id")
                    .from("current_records")
                    .where(Filters.equal("type", "current"))
                    .union(NSC.select("payload::type").from("archived_records").where(Filters.equal("type", "archived")))
                    .build();

            assertTrue(combined.query().contains("payload::type"), combined.query());
            assertFalse(combined.query().contains("payload::type_2"), combined.query());
            assertTrue(combined.query().contains(":type_2"), combined.query());
            assertEquals(Arrays.asList("current", "archived"), combined.parameters());
        }

        @Test
        void testSetOperationNormalizesChildNamedParametersToParentHandler() {
            final Dsl parentDsl = Dsl
                    .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("${").append(name).append('}')).build());
            final SP sp = parentDsl.select("id")
                    .from("users")
                    .where(Filters.equal("id", 1))
                    .union(NSC.select("id").from("archived_users").where(Filters.equal("id", 2)))
                    .build();

            assertEquals("SELECT id FROM users WHERE id = ${id} UNION SELECT id FROM archived_users WHERE id = ${id_2}", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        @Test
        void testSetOperationPropertyVarargsOverloadsAreRemoved() {
            for (final String methodName : Arrays.asList("union", "unionAll", "intersect", "except", "minus")) {
                assertThrows(NoSuchMethodException.class, () -> AbstractQueryBuilder.class.getDeclaredMethod(methodName, String[].class));
            }
        }

        @Test
        void testSetOperationRejectsDifferentGeneratedNamedParameterPolicies() {
            final SqlBuilder parent = NSC.select("id").from("users").where(Filters.equal("id", 1));
            final SqlBuilder child = Dsl.MSC.select("id").from("archived_users").where(Filters.equal("id", 2));

            assertThrows(IllegalArgumentException.class, () -> parent.union(child));
        }

        @Test
        void testSetOperationRejectsMutationContainingNestedSelect() {
            SqlBuilder builder = PSC.select("id").from("current_records");

            assertThrows(IllegalArgumentException.class, () -> builder.union("UPDATE target SET value = (SELECT value FROM source) FROM target"));
            assertEquals("SELECT id FROM current_records", builder.build().query());
        }

        @Test
        void testSetOperationRenamesCustomNamedParameterTokensWithoutCorruptingWrapperText() {
            final Dsl customDsl = Dsl
                    .forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("CAST(:").append(name).append(" AS uuid)")).build());
            final SP sp = customDsl.select("id")
                    .from("users")
                    .where(Filters.equal("id", 1))
                    .union(customDsl.select("id").from("archived_users").where(Filters.equal("id", 2)))
                    .build();

            assertEquals("SELECT id FROM users WHERE id = CAST(:id AS uuid) UNION SELECT id FROM archived_users WHERE id = CAST(:id_2 AS uuid)", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        @Test
        void testSetOperationSnapshotsAndStructuredSubQueriesRequireBranchIsolation() {
            final SubQuery limitedSnapshot = PSC.select("id").from("archived_records").limit(1).toSubQuery();
            final SubQuery compoundSnapshot = PSC.select("id").from("archived_records").unionSelect(Arrays.asList("id")).from("older_records").toSubQuery();
            final SubQuery limitedStructured = Filters.subQuery("archived_records", Arrays.asList("id"), new Limit(1));
            final SubQuery orderedStructured = Filters.subQuery("archived_records", Arrays.asList("id"), Criteria.builder().orderBy("id").build());
            final SubQuery compoundStructured = Filters.subQuery("archived_records", Arrays.asList("id"),
                    Criteria.builder().union(Filters.subQuery("SELECT id FROM older_records")).build());

            for (final SubQuery child : Arrays.asList(limitedSnapshot, compoundSnapshot, limitedStructured, orderedStructured, compoundStructured)) {
                final SqlBuilder standalone = PSC.select("id").from("current_records");
                assertThrows(IllegalArgumentException.class, () -> standalone.append(Filters.union(child)));
                assertEquals("SELECT id FROM current_records", standalone.build().query());

                final SqlBuilder criteria = PSC.select("id").from("current_records");
                assertThrows(IllegalArgumentException.class, () -> criteria.append(Criteria.builder().union(child).limit(5).build()));
                assertEquals("SELECT id FROM current_records WHERE id = ?", criteria.where(Filters.eq("id", 1)).build().query());
            }

            assertEquals("SELECT id FROM current_records WHERE id IN (SELECT id FROM archived_records LIMIT 1)",
                    PSC.select("id").from("current_records").where(Filters.in("id", limitedSnapshot)).build().query());
        }

        @Test
        void testSetOperationsRejectTerminalClausesOnLeftOperand() {
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("current_records").orderBy("id").union("SELECT id FROM archived_records"));
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("current_records").limit(10).intersect("SELECT id FROM archived_records"));
            assertThrows(IllegalStateException.class, () -> PSC.select("id").from("current_records").forUpdate().minus("SELECT id FROM archived_records"));
        }

        @Test
        void testSetOperationsRequireACompleteSelectLeftOperand() {
            assertThrows(IllegalStateException.class, () -> PSC.select("id").union("SELECT id FROM archived_records"));
            assertThrows(IllegalStateException.class, () -> PSC.select("id").intersectSelect(Arrays.asList("id")));
            assertThrows(IllegalStateException.class,
                    () -> PSC.update("current_records").set(Collections.singletonMap("active", false)).except("SELECT id FROM archived_records"));
            assertThrows(IllegalStateException.class, () -> PSC.renderCondition(Filters.equal("active", true)).minus("SELECT id FROM archived_records"));
            assertThrows(IllegalStateException.class,
                    () -> PSC.select("id")
                            .from("current_records")
                            .union("SELECT id FROM archived_records")
                            .distinct()
                            .except("SELECT id FROM deleted_records"));
        }

        @Test
        void testSiblingSetOperationDetectsParametersGeneratedInsideStructuredSubQuery() {
            final SqlBuilder left = NSC.select("id").from("current_records");
            final SubQuery activeOwners = Filters.subQuery("owners", Arrays.asList("id"), Filters.equal("active", true));
            final SqlBuilder right = PSC.select("id").from("archived_records").where(Filters.in("owner_id", activeOwners));

            assertThrows(IllegalArgumentException.class, () -> left.union(right));
            assertEquals("SELECT id FROM current_records", left.build().query());

            final SP rightSql = right.build();
            assertEquals("SELECT id FROM archived_records WHERE owner_id IN (SELECT id FROM owners WHERE active = ?)", rightSql.query());
            assertEquals(Arrays.asList(true), rightSql.parameters());
        }

        @Test
        void testSiblingSetOperationRejectsMixedSqlPoliciesWhenNamedParametersAreGenerated() {
            SqlBuilder left = NSC.select("id").from("current_records");
            SqlBuilder right = PSC.select("id").from("archived_records").where(Filters.equal("active", true));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> left.union(right));
            assertTrue(ex.getMessage().contains("parent's SQL policy"), ex.getMessage());
            assertEquals("SELECT id FROM current_records", left.build().query());
            assertEquals("SELECT id FROM archived_records WHERE active = ?", right.build().query());

            assertEquals("SELECT id FROM current_records WHERE active = :active UNION SELECT id FROM archived_records",
                    NSC.select("id")
                            .from("current_records")
                            .where(Filters.equal("active", true))
                            .union(PSC.select("id").from("archived_records"))
                            .build()
                            .query());

            SqlBuilder questionMarkLeft = NSC.select("id").from("current_records");
            SqlBuilder questionMark = PSC.select("id").from("archived_records").where(Filters.equal("active", Filters.QME));
            assertThrows(IllegalArgumentException.class, () -> questionMarkLeft.union(questionMark));
            assertEquals("SELECT id FROM current_records", questionMarkLeft.build().query());
            assertEquals("SELECT id FROM archived_records WHERE active = ?", questionMark.build().query());
        }

        @Test
        void testStaleMultiSelectsDoNotLeakAcrossSetOperation() {
            // The multi-select column list used to survive a verbatim union and be re-emitted by the
            // next from(), producing two juxtaposed SELECTs ("... UNION SELECT id FROM t2 SELECT ... FROM t3").
            List<Selection> selections = Arrays.asList(Selection.builder(Account.class).build());
            assertThrows(IllegalStateException.class, () -> PSC.select(selections).from("account").union("SELECT id FROM t2").from("t3"));

            // The verbatim-union compound itself still builds.
            String sql = PSC.select(selections).from("account").union("SELECT id FROM t2").build().query();
            assertTrue(sql.endsWith(" FROM account UNION SELECT id FROM t2"));
        }

        // Subquery tests
        @Test
        void testSubquery() {
            String subquery = PSC.select("id").from("active_users").build().query();
            String sql = PSC.select("*").from("orders").where("user_id IN (" + subquery + ")").build().query();
            assertTrue(sql.contains("IN"));
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSubQuerySnapshotEqualityDistinguishesIsolationMetadata() {
            // The isolation flag is part of a snapshot's state, so two snapshots carrying the same SQL and the
            // same bindings are only equal when they also agree on whether the source needs branch isolation.
            final SubQuery paginated = PSC.select("id").from("t").limit(1).toSubQuery();
            final SubQuery appended = PSC.select("id").from("t").append("LIMIT 1").toSubQuery();

            assertEquals(paginated.toString(), appended.toString());
            assertEquals(paginated.parameters(), appended.parameters());
            assertNotEquals(paginated, appended);
            assertNotEquals(appended, paginated);

            // Matching metadata is equal, with a consistent hash code.
            final SubQuery samePagination = PSC.select("id").from("t").limit(1).toSubQuery();
            assertEquals(paginated, samePagination);
            assertEquals(paginated.hashCode(), samePagination.hashCode());

            final SubQuery sameAppended = PSC.select("id").from("t").append("LIMIT 1").toSubQuery();
            assertEquals(appended, sameAppended);
            assertEquals(appended.hashCode(), sameAppended.hashCode());

            // Only the isolated one is refused as a set-operation operand.
            assertThrows(IllegalArgumentException.class, () -> PSC.select("id").from("u").append(Filters.union(paginated)));
            assertEquals("SELECT id FROM u UNION SELECT id FROM t LIMIT 1", PSC.select("id").from("u").append(Filters.union(appended)).build().query());
        }

        @Test
        void testToSubQuerySnapshotComparesArrayParametersByContent() {
            // the snapshot's captured parameters compare by content like every other condition class does
            final SubQuery first = PSC.select("id").from("blob_store").where(Filters.eq("payload", new byte[] { 1, 2, 3 })).toSubQuery();
            final SubQuery second = PSC.select("id").from("blob_store").where(Filters.eq("payload", new byte[] { 1, 2, 3 })).toSubQuery();
            final SubQuery other = PSC.select("id").from("blob_store").where(Filters.eq("payload", new byte[] { 9 })).toSubQuery();

            assertEquals(first, second);
            assertEquals(first.hashCode(), second.hashCode());
            assertNotEquals(first, other);
            assertEquals(Filters.in("id", first), Filters.in("id", second));
            assertEquals(Filters.in("id", first).hashCode(), Filters.in("id", second).hashCode());
        }

        @Test
        void testUnfinishedCompoundChildCanBeCompletedAfterIsolationRejection() {
            final SqlBuilder parent = PSC.select("id").from("current_records");
            final SqlBuilder child = PSC.select("id").from("archived_records").unionSelect(List.of("id"));

            // An open SELECT segment already makes the child compound. Prevalidation must reject it
            // before final rendering consumes its buffer, leaving it available to complete and isolate.
            assertThrows(IllegalArgumentException.class, () -> parent.union(child));
            child.from("older_records");
            final SP result = parent.union(PSC.select("id").from(child, "branch")).build();

            assertEquals("SELECT id FROM current_records UNION SELECT id FROM " + "(SELECT id FROM archived_records UNION SELECT id FROM older_records) branch",
                    result.query());
            assertTrue(result.parameters().isEmpty());
        }

        // UNION tests
        @Test
        void testUnion() {
            String sql1 = PSC.select("id", "name").from("users").build().query();
            String sql2 = PSC.select("id", "name").from("archived_users").build().query();
            String unionSql = sql1 + " UNION " + sql2;
            assertTrue(unionSql.contains("UNION"));
        }

        @Test
        void testUnionAll() {
            SqlBuilder query1 = PSC.select("id", "name").from("users");
            SqlBuilder query2 = PSC.select("id", "name").from("customers");

            String sql = query1.unionAll(query2).build().query();
            assertEquals("SELECT id, name FROM users UNION ALL SELECT id, name FROM customers", sql);

            SqlBuilder self = PSC.select("id").from("users");
            assertThrows(IllegalArgumentException.class, () -> self.unionAll(self));
        }

        @Test
        void testUnionAll_CollectionOverload() {
            String sql = PSC.select("id", "name").from("users").unionAllSelect(Arrays.asList("id", "name")).from("customers").build().query();
            assertEquals("SELECT id, name FROM users UNION ALL SELECT id, name FROM customers", sql);
        }

        @Test
        void testUnionAll_StringOverload() {
            String sql = PSC.select("id", "name").from("users").unionAll("SELECT id, name FROM customers").build().query();
            assertEquals("SELECT id, name FROM users UNION ALL SELECT id, name FROM customers", sql);
        }

        /**
         * Bug: the union-rename logic picked "name_(parentCount+i)" blindly, colliding with a property
         * literally named "name_2" on the child side.
         */
        @Test
        void testUnionNamedParameterRenameAvoidsChildLiteralName() {
            SP sp = NSC.select("id")
                    .from("users")
                    .where(Filters.eq("id", 1))
                    .union(NSC.select("id").from("archive").where(Filters.and(Filters.eq("id", 2), Filters.eq("id_2", 3))))
                    .build();

            assertEquals("SELECT id FROM users WHERE id = :id UNION SELECT id FROM archive WHERE (id = :id_3) AND (id_2 = :id_2)", sp.query());
            assertEquals(Arrays.asList(1, 2, 3), sp.parameters());
        }

        /**
         * Same union-rename collision on the parent side: the renamed child parameter must not collide
         * with the parent's literally-named "id_2" placeholder.
         */
        @Test
        void testUnionNamedParameterRenameAvoidsParentLiteralName() {
            SP sp = NSC.select("id")
                    .from("users")
                    .where(Filters.and(Filters.eq("id", 1), Filters.eq("id_2", 9)))
                    .union(NSC.select("id").from("archive").where(Filters.eq("id", 2)))
                    .build();

            assertEquals("SELECT id FROM users WHERE (id = :id) AND (id_2 = :id_2) UNION SELECT id FROM archive WHERE id = :id_3", sp.query());
            assertEquals(Arrays.asList(1, 9, 2), sp.parameters());
        }

        /**
         * Bug: during set-operation named-parameter uniquify, the token-replacement scanner treated a
         * SQL Server temporary-table identifier ({@code #tmp}) in the child operand as a MySQL hash
         * comment and skipped the rest of the line, so the child's colliding {@code :id} was silently
         * left un-renamed (both legs bound the same placeholder). On a SQL Server dialect,
         * {@code #name}/{@code ##name} are data tokens, never comments.
         */
        @Test
        void testUnionNamedParameterRenameHandlesSqlServerTempTableInChild() {
            final Dsl mssqlNamed = Dsl.forDialect(SqlDialect.builder()
                    .namingPolicy(NamingPolicy.NO_CHANGE)
                    .sqlPolicy(SqlDialect.SqlPolicy.NAMED_SQL)
                    .productInfo(SqlDialect.ProductInfo.of("Microsoft SQL Server"))
                    .build());

            SP sp = mssqlNamed.select("id").from("t").where(Filters.eq("id", 1)).union(mssqlNamed.select("id").from("#tmp").where(Filters.eq("id", 2))).build();

            assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM #tmp WHERE id = :id_2", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());

            // Global temporary table (##name): already safe before the fix via the ##-specific carve-out,
            // pinned here so both temp-identifier forms stay covered.
            sp = mssqlNamed.select("id").from("t").where(Filters.eq("id", 1)).union(mssqlNamed.select("id").from("##tmp").where(Filters.eq("id", 2))).build();

            assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM ##tmp WHERE id = :id_2", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        @Test
        void testUnionNamedParameterRenameHandlesTempTableInChildOnDefaultDialect() {
            SP sp = NSB.select("id").from("t").where(Filters.eq("id", 1)).union(NSB.select("id").from("#tmp").where(Filters.eq("id", 2))).build();
            assertEquals("SELECT id FROM t WHERE id = :id UNION SELECT id FROM #tmp WHERE id = :id_2", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
            assertEquals(Arrays.asList("id", "id_2"), ParsedSql.parse(sp.query()).namedParameters());

            sp = MSB.select("id").from("t").where(Filters.eq("id", 1)).union(MSB.select("id").from("#tmp").where(Filters.eq("id", 2))).build();
            assertEquals("SELECT id FROM t WHERE id = #{id} UNION SELECT id FROM #tmp WHERE id = #{id_2}", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());

            // Builder-backed condition subquery: same rename path.
            sp = NSB.select("id")
                    .from("t")
                    .where(Filters.and(Filters.eq("id", 1), Filters.in("id", NSB.select("id").from("#tmp").where(Filters.eq("id", 2)).toSubQuery())))
                    .build();
            assertEquals("SELECT id FROM t WHERE (id = :id) AND (id IN (SELECT id FROM #tmp WHERE id = :id_2))", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        /**
         * Regression: the plain duplicated-name union rename (no literal-name collisions) is unchanged.
         */
        @Test
        void testUnionNamedParameterRenamePlainCaseUnchanged() {
            SP sp = NSC.select("id").from("users").where(Filters.eq("id", 1)).union(NSC.select("id").from("archive").where(Filters.eq("id", 2))).build();

            assertEquals("SELECT id FROM users WHERE id = :id UNION SELECT id FROM archive WHERE id = :id_2", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        @Test
        void testUnionNamedParameterRenameSkipsDoubledQuotesAndBackticks() {
            SP sp = NSC.select("id")
                    .from("users")
                    .where(Filters.eq("id", 1))
                    .union(NSC.select("id").from("archive").where(Filters.eq("id", 2)).append(" AND a = \"x\"\":id\" AND b = `y``:id` AND c = ':id'"))
                    .build();

            // The genuine parameter of the second query is renamed...
            assertTrue(sp.query().contains("archive WHERE id = :id_2"), sp.query());
            // ...while colon-tokens embedded inside double-quoted, backtick-quoted, and single-quoted
            // literals (with doubled "" / `` escapes) are left intact.
            assertTrue(sp.query().contains("a = \"x\"\":id\""), sp.query());
            assertTrue(sp.query().contains("b = `y``:id`"), sp.query());
            assertTrue(sp.query().contains("c = ':id'"), sp.query());
            assertFalse(sp.query().contains(":id_2\""), sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        @Test
        void testUnionNamedParameterRenameSkipsQuotedLiterals() {
            SP sp = NSC.select("id")
                    .from("users")
                    .where(Filters.eq("id", 1))
                    .union(NSC.select("id").from("archive").where(Filters.eq("id", 2)).append(" AND note = ':id' AND quoted_name = \":id\""))
                    .build();

            assertTrue(sp.query().contains("archive WHERE id = :id_2"), sp.query());
            assertTrue(sp.query().contains("note = ':id'"), sp.query());
            assertTrue(sp.query().contains("quoted_name = \":id\""), sp.query());
            assertFalse(sp.query().contains("note = ':id_2'"), sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        /**
         * Guard for the fix above: on a MySQL dialect a genuine hash comment in the child operand still
         * stops the uniquify scanner, so a {@code :id} inside the comment text is left untouched while
         * the genuine parameter before it is renamed.
         */
        @Test
        void testUnionNamedParameterRenameStillSkipsMySqlHashComment() {
            final Dsl mysqlNamed = Dsl.forDialect(SqlDialect.builder()
                    .namingPolicy(NamingPolicy.NO_CHANGE)
                    .sqlPolicy(SqlDialect.SqlPolicy.NAMED_SQL)
                    .productInfo(SqlDialect.ProductInfo.of("MySQL"))
                    .build());

            final SP sp = mysqlNamed.select("id")
                    .from("users")
                    .where(Filters.eq("id", 1))
                    .union(mysqlNamed.select("id").from("archive").where(Filters.eq("id", 2)).append(" # note :id"))
                    .build();

            assertTrue(sp.query().contains("archive WHERE id = :id_2"), sp.query());
            assertTrue(sp.query().endsWith("# note :id\n"), sp.query());
            assertFalse(sp.query().contains("# note :id_2"), sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        @Test
        void testUnionRenamesChainedSubscriptParametersWithoutChangingQuotedText() {
            final String suffix = " FROM score_archive WHERE note = ':param #{param}' AND [literal:param] = 1 /* :param #{param} */";

            for (final Dsl dsl : Arrays.asList(NSC, MSC)) {
                final SubQuery subQuery = Filters.subQuery("SELECT scores[?][?]" + suffix, Arrays.asList(2, 3));
                final SP sp = dsl.select("id")
                        .from("users")
                        .where(Filters.eq("param", 1))
                        .union(dsl.select("id").from("archive").where(Filters.in("id", subQuery)))
                        .build();
                final String expectedExpression = dsl == NSC ? "scores[:param_2][:param_3]" : "scores[#{param_2}][#{param_3}]";

                assertTrue(sp.query().contains("SELECT " + expectedExpression + suffix), sp.query());
                assertEquals(Arrays.asList(1, 2, 3), sp.parameters());
            }
        }

        @Test
        void testUnionRenamesCustomNamedParametersInsideArraySubscripts() {
            final Dsl customDsl = Dsl.forDialect(
                    NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> sb.append("CAST(:").append(name).append(" AS integer)")).build());
            final SubQuery subQuery = Filters.subQuery("SELECT scores[?] FROM score_archive", Collections.singletonList(2));
            final SP sp = customDsl.select("id")
                    .from("users")
                    .where(Filters.eq("param", 1))
                    .union(customDsl.select("id").from("archive").where(Filters.in("id", subQuery)))
                    .build();

            assertEquals("SELECT id FROM users WHERE param = CAST(:param AS integer) UNION SELECT id FROM archive WHERE id IN "
                    + "(SELECT scores[CAST(:param_2 AS integer)] FROM score_archive)", sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        @Test
        void testUnionRenamesGeneratedIbatisParametersInsideArraySubscripts() {
            final SubQuery subQuery = Filters.subQuery("SELECT scores[?] FROM score_archive", Collections.singletonList(2));
            final SP sp = MSC.select("id")
                    .from("users")
                    .where(Filters.eq("param", 1))
                    .union(MSC.select("id").from("archive").where(Filters.in("id", subQuery)))
                    .build();

            assertEquals("SELECT id FROM users WHERE param = #{param} UNION SELECT id FROM archive WHERE id IN (SELECT scores[#{param_2}] FROM score_archive)",
                    sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        @Test
        void testUnionRenamesGeneratedNamedParametersInsideArraySubscripts() {
            final SubQuery subQuery = Filters.subQuery("SELECT scores[?] FROM score_archive", Collections.singletonList(2));
            final SP sp = NSC.select("id")
                    .from("users")
                    .where(Filters.eq("param", 1))
                    .union(NSC.select("id").from("archive").where(Filters.in("id", subQuery)))
                    .build();

            assertEquals("SELECT id FROM users WHERE param = :param UNION SELECT id FROM archive WHERE id IN (SELECT scores[:param_2] FROM score_archive)",
                    sp.query());
            assertEquals(Arrays.asList(1, 2), sp.parameters());
        }

        /**
         * Bug: {@code union("SELECT 1")} (a valid FROM-less query) was treated as a column list and
         * silently dropped, emitting "... UNION " with nothing after the keyword.
         */
        @Test
        void testUnionWithFromLessQueryIsAppendedInline() {
            String sql = PSC.select("id").from("users").union("SELECT 1").build().query();
            assertEquals("SELECT id FROM users UNION SELECT 1", sql);
        }

        /**
         * Regression: a full inline sub-query and a plain column list after union are unchanged.
         */
        @Test
        void testUnionWithFullQueryAndColumnListUnchanged() {
            String sql = PSC.select("id").from("users").union("SELECT id FROM customers").build().query();
            assertEquals("SELECT id FROM users UNION SELECT id FROM customers", sql);

            String sql2 = PSC.select("id").from("users").unionSelect(Collections.singletonList("id")).from("customers").build().query();
            assertEquals("SELECT id FROM users UNION SELECT id FROM customers", sql2);
        }

        @Test
        void testUnionWithSubQuery() {
            String subQuery = "(SELECT id, name FROM archived_users WHERE status = 'INACTIVE')";

            String sql = PSC.select("id", "name").from("users").where(Filters.eq("status", "ACTIVE")).union(subQuery).build().query();

            assertTrue(sql.contains("SELECT id, name FROM users WHERE status = ?"));
            assertTrue(sql.contains("UNION"));
            assertTrue(sql.contains(subQuery));
        }
    }

    @Nested
    class Updates {
        @Test
        void testComplexUpdateWithMultipleConditions() {
            Date modifiedAt = new Date(1_700_086_400_000L);
            Date lastLoginCutoff = new Date(1_700_000_000_000L);
            Map<String, Object> updateValues = new LinkedHashMap<>();
            updateValues.put("status", "INACTIVE");
            updateValues.put("lastModifiedDate", modifiedAt);

            SP result = PSC.update("users")
                    .set(updateValues)
                    .where(Filters.and(Filters.lt("lastLoginDate", lastLoginCutoff),
                            Filters.or(Filters.eq("status", "PENDING"), Filters.eq("status", "ACTIVE"))))
                    .build();

            assertSql("UPDATE users SET status = ?, last_modified_date = ? WHERE (last_login_date < ?) AND ((status = ?) OR (status = ?))",
                    List.of("INACTIVE", modifiedAt, lastLoginCutoff, "PENDING", "ACTIVE"), result);
        }

        @Test
        void testForUpdate() {
            String sql = PSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

            assertEquals("SELECT * FROM users WHERE id = ? FOR UPDATE", sql);
        }

        @Test
        void testPAC_UpdateAndDeleteWithEntityClass() {
            String updateSql = Dsl.PAC.update("USER_ACCOUNT", Account.class).set("firstName").where(Filters.eq("id", 1)).build().query();
            String deleteSql = Dsl.PAC.deleteFrom("USER_ACCOUNT", Account.class).where(Filters.eq("id", 1)).build().query();

            assertTrue(updateSql.contains("UPDATE USER_ACCOUNT"));
            assertTrue(deleteSql.contains("DELETE FROM USER_ACCOUNT"));
        }

        @Test
        void testSet() {
            // With expression
            String sql = PSC.update("users").set("name = 'John'").where(Filters.eq("id", 1)).build().query();

            assertEquals("UPDATE users SET name = 'John' WHERE id = ?", sql);

            // With columns
            sql = PSC.update("users").set(Arrays.asList("firstName", "lastName", "email")).where(Filters.eq("id", 1)).build().query();

            assertEquals("UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?", sql);

            // With collection
            sql = PSC.update("users").set(Arrays.asList("firstName", "lastName")).where(Filters.eq("id", 1)).build().query();

            assertEquals("UPDATE users SET first_name = ?, last_name = ? WHERE id = ?", sql);

            // With map
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("firstName", "John");
            values.put("lastName", "Doe");

            sql = PSC.update("users").set(values).where(Filters.eq("id", 1)).build().query();

            assertEquals("UPDATE users SET first_name = ?, last_name = ? WHERE id = ?", sql);

            // With entity
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");

            sql = PSC.update("account").set(account).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("UPDATE account SET"));
            assertTrue(sql.contains("WHERE id = ?"));

            // With entity class
            sql = PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("UPDATE account SET"));
            assertTrue(sql.contains("WHERE id = ?"));

            // With excluded properties
            Set<String> excluded = N.asSet("lastModifiedDate");
            sql = PSC.update("account").set(account, excluded).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("UPDATE account SET"));
            assertFalse(sql.contains("last_modified_date"));

            sql = PSC.update("account").set(Account.class, excluded).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("UPDATE account SET"));
            assertFalse(sql.contains("last_modified_date"));
        }

        @Test
        void testSetEntityRejectsAnEmptyUpdatableProjectionWithoutChangingMapping() {
            final SqlBuilder builder = PSC.update("users");

            assertThrows(IllegalArgumentException.class, () -> builder.set(AllNullBatchEntity.class, Collections.singleton("value")));
            assertNull(builder._entityClass);
            assertEquals("UPDATE users SET name = ?", builder.set("name").build().query());

            final SqlBuilder objectBuilder = PSC.update("users");
            assertThrows(IllegalArgumentException.class, () -> objectBuilder.set(new AllNullBatchEntity(), Collections.singleton("value")));
            assertNull(objectBuilder._entityClass);
            assertEquals("UPDATE users SET name = ?", objectBuilder.set("name").build().query());
        }

        // UPDATE tests
        @Test
        void testUpdate() {
            String sql = PSC.update("users").set("status", "inactive").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("SET"));
        }

        @Test
        void testUpdateAllProperties() {
            String sql = PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("UPDATE account SET"));
            assertTrue(sql.contains("first_name = ?"));
            assertTrue(sql.contains("last_name = ?"));
            assertFalse(sql.contains("created_date")); // @NonUpdatable
            assertFalse(sql.contains("last_modified_date")); // @ReadOnly
        }

        @Test
        void testUpdateAndDeleteRejectBlankTableNames() {
            assertThrows(IllegalArgumentException.class, () -> PSC.update("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.update("   ", Account.class));
            assertThrows(IllegalArgumentException.class, () -> PSC.deleteFrom("   "));
            assertThrows(IllegalArgumentException.class, () -> PSC.deleteFrom("   ", Account.class));
        }

        @Test
        void testUpdateFactory() {
            SqlBuilder builder = PSC.update("users");
            assertNotNull(builder);
        }

        @Test
        void testUpdateMultipleColumns() {
            String sql = PSC.update("users").set("first_name", "John", "last_name", "Doe").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("SET"));
        }

        @Test
        void testUpdateWithEntityClass() {
            String sql = PSC.update(Account.class).set("status", "active").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
        }

        // Map-based operations
        @Test
        void testUpdateWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("first_name", "John");
            props.put("last_name", "Doe");

            String sql = PSC.update("users").set(props).where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("SET"));
        }

        @Test
        void testUpdateWithoutSet() {
            // Used to emit "UPDATE users SET  WHERE id = ?" with an empty SET list; now fails fast.
            assertThrows(IllegalStateException.class, () -> PSC.update("users").where(Filters.eq("id", 1)));
            assertThrows(IllegalStateException.class, () -> PSC.update("users").build());

            // update(Class) pre-stages the updatable columns, so no explicit set() is required.
            String sql = PSC.update(Account.class).where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.startsWith("UPDATE test_account SET "));
            assertTrue(sql.contains("first_name = ?"));
            assertTrue(sql.endsWith(" WHERE id = ?"));
        }
    }

    /** Minimal entity mirroring the Account shape used by the Dsl entity-class SELECT Javadoc examples. */
    @Table(name = "account")
    public static class JavadocAccount {
        private long id;
        private String firstName;
        private String lastName;
        private String email;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /** Deliberately malformed metadata used to force a post-allocation FROM-rendering failure. */
    @Table(name = " ")
    public static class BlankTableNameEntity {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    // Test entity classes
    @Table(name = "test_account")
    public static class Account {
        @Id
        private long id;
        private String firstName;
        private String lastName;
        private String email;
        @NonUpdatable
        private Date createdDate;
        @ReadOnly
        private Date lastModifiedDate;
        @Column("account_status")
        private String status;
        private int age;
        private BigDecimal balance;

        // Getters and setters
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Date getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }

        public Date getLastModifiedDate() {
            return lastModifiedDate;
        }

        public void setLastModifiedDate(Date lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }

    public static class TestEntity {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private Integer age;
        private String status;
        private Date createdDate;
        private Date modifiedDate;

        public TestEntity() {
        }

        public TestEntity(String firstName, String lastName, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }

        public Date getModifiedDate() {
            return modifiedDate;
        }

        public void setModifiedDate(Date modifiedDate) {
            this.modifiedDate = modifiedDate;
        }
    }

    public static class TestOrder {
        private Long orderId;
        private Long userId;
        private String orderNumber;
        private Date orderDate;
        private Double totalAmount;

        // Getters and setters
        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public Date getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(Date orderDate) {
            this.orderDate = orderDate;
        }

        public Double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }
    }

    private static List<Map<String, Object>> batchRows() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("firstName", "Jane");
        row2.put("lastName", "Smith");

        return Arrays.asList(row1, row2);
    }

    private static void assertInsertBuilds(SP sp) {
        assertTrue(sp.query().contains("INSERT INTO users"));
        assertNotNull(sp.parameters());
    }

    private static void assertUpdateBuilds(SP sp, String tableName) {
        assertTrue(sp.query().contains("UPDATE " + tableName));
        assertTrue(sp.query().contains("WHERE"));
    }

    private static void assertDeleteBuilds(SP sp, String tableName) {
        assertTrue(sp.query().contains("DELETE FROM " + tableName));
        assertTrue(sp.query().contains("WHERE"));
    }

    // ===== Pass-4: parameter ordering across JOINs, subqueries, nested conditions =====

    private static int countQmarks(String s) {
        return s.length() - s.replace("?", "").length();
    }

    public static class BfxQuoted {
        private long id;
        private String firstName;

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
    }

    public static class BfxAddress {
        private long id;
        private String streetName;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getStreetName() {
            return streetName;
        }

        public void setStreetName(final String streetName) {
            this.streetName = streetName;
        }
    }

    public static class BfxUser {
        private long id;
        private String firstName;
        private BfxAddress address;

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

        public BfxAddress getAddress() {
            return address;
        }

        public void setAddress(final BfxAddress address) {
            this.address = address;
        }
    }

    private static Condition unsupportedCondition() {
        return new Condition() {
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
                throw new AssertionError("toSql(NamingPolicy) should not be used for unsupported condition messages");
            }

            @Override
            public String toString() {
                throw new AssertionError("toString() should not be used for unsupported condition messages");
            }
        };
    }

    @Table(name = "profiles", alias = "p")
    public static class QualifiedProfile {
        @Column("p.display_name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Table(name = "profile_holders")
    public static class QualifiedProfileHolder {
        private QualifiedProfile profile;

        public QualifiedProfile getProfile() {
            return profile;
        }

        public void setProfile(final QualifiedProfile profile) {
            this.profile = profile;
        }
    }

    static final class AllNullBatchEntity {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    static final class BatchEntityWithId {
        @Id
        private long id;
        private long otherId;
        private String name;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public long getOtherId() {
            return otherId;
        }

        public void setOtherId(final long otherId) {
            this.otherId = otherId;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    static final class BatchEntityWithOtherId {
        private long id;
        @Id
        private long otherId;
        private String name;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public long getOtherId() {
            return otherId;
        }

        public void setOtherId(final long otherId) {
            this.otherId = otherId;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    /** A user-defined AbstractBetween subclass (the constructor is protected for exactly this). */
    static final class CustomBetween extends com.landawn.abacus.query.condition.AbstractBetween {
        CustomBetween(final String propName, final Object minValue, final Object maxValue) {
            super(propName, com.landawn.abacus.query.condition.Operator.BETWEEN, minValue, maxValue);
        }
    }

    /** A user-defined NOT BETWEEN subclass of AbstractBetween. */
    static final class CustomNotBetween extends com.landawn.abacus.query.condition.AbstractBetween {
        CustomNotBetween(final String propName, final Object minValue, final Object maxValue) {
            super(propName, com.landawn.abacus.query.condition.Operator.NOT_BETWEEN, minValue, maxValue);
        }
    }

    private static Dsl setExpressionDsl(final String productName) {
        return Dsl.forDialect(SqlDialect.builder()
                .productInfo(productName == null ? null : ProductInfo.of(productName))
                .namingPolicy(NamingPolicy.NO_CHANGE)
                .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
                .build());
    }

    private static void assertSetExpression(final Dsl dsl, final String expression, final String expectedSetClause) {
        final SP result = dsl.update("measurements").set(expression).build();
        assertEquals("UPDATE measurements SET " + expectedSetClause, result.query(), expression);
        assertTrue(result.parameters().isEmpty(), expression);
    }

    private static void assertStagedInputsReleased(final SqlBuilder builder) {
        assertNull(builder._sb);
        assertNull(builder._props);
        assertNull(builder._propsList);
        assertNull(builder._propOrColumnNames);
        assertNull(builder._propOrColumnNameAliases);
        assertNull(builder._multiSelects);
    }
}
