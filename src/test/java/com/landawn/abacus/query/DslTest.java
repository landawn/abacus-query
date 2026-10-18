package com.landawn.abacus.query;

import static com.landawn.abacus.query.Dsl.NSC;
import static com.landawn.abacus.query.Dsl.PSC;
import static com.landawn.abacus.query.QueryTestSupport.assertSql;
import static com.landawn.abacus.query.QueryTestSupport.selection;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.QueryTestSupport.Order;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.SqlExpression;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.util.N;

public class DslTest extends TestBase {

    @Nested
    class ACSBTest {
        @Test
        void testBatchInsert() {
            List<User> users = Arrays.asList(new User("John", "Doe"), new User("Jane", "Smith"));

            SqlBuilder sb = Dsl.ACSB.batchInsert(users);
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testBatchInsertWithMaps() {
            List<Map<String, Object>> propsList = new ArrayList<>();
            Map<String, Object> user1 = new HashMap<>();
            user1.put("firstName", "John");
            user1.put("lastName", "Doe");
            propsList.add(user1);

            Map<String, Object> user2 = new HashMap<>();
            user2.put("firstName", "Jane");
            user2.put("lastName", "Smith");
            propsList.add(user2);

            SqlBuilder sb = Dsl.ACSB.batchInsert(propsList);
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testComplexQuery() {
            SqlBuilder sb = Dsl.ACSB.select("u.firstName", "u.lastName", "COUNT(o.id)")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.userId")
                    .where(Filters.eq("u.status", "'ACTIVE'"))
                    .groupBy("u.id", "u.firstName", "u.lastName")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("u.lastName");

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("GROUP BY"));
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        void testCountEntityClass() {
            SqlBuilder sb = Dsl.ACSB.selectCountFrom(User.class);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("count(*)"));
        }

        @Test
        void testCountTableName() {
            SqlBuilder sb = Dsl.ACSB.selectCountFrom("users");
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("count(*)"));
        }

        @Test
        void testDeleteFromEntityClass() {
            SqlBuilder sb = Dsl.ACSB.deleteFrom(User.class);
            assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            assertNotNull(sql);
        }

        @Test
        void testDeleteFromTableName() {
            SqlBuilder sb = Dsl.ACSB.deleteFrom("users");
            assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.ACSB.deleteFrom("users", User.class);
            assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            assertNotNull(sql);
        }

        @Test
        void testEmptySelectParts() {
            // Test empty string
            assertThrows(IllegalArgumentException.class, () -> Dsl.ACSB.select(""));

            // Test empty array
            assertThrows(IllegalArgumentException.class, () -> Dsl.ACSB.select(new String[0]));

            // Test empty map
            assertThrows(IllegalArgumentException.class, () -> Dsl.ACSB.select(Collections.emptyMap()));
        }

        @Test
        void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = Dsl.ACSB.insert(columns);
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntity() {
            User user = new User("John", "Doe");
            user.setEmail("john@example.com");

            SqlBuilder sb = Dsl.ACSB.insert(user);
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntityClass() {
            SqlBuilder sb = Dsl.ACSB.insert(User.class);
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder sb = Dsl.ACSB.insert(User.class, excluded);
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntityWithExclusions() {
            User user = new User("John", "Doe");
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));

            SqlBuilder sb = Dsl.ACSB.insert(user, excluded);
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertInto() {
            SqlBuilder sb = Dsl.ACSB.insertInto(User.class);
            assertNotNull(sb);

            // Should be able to get SQL directly as table name is already set
            assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SqlBuilder sb = Dsl.ACSB.insertInto(User.class, excluded);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        void testInsertMultipleColumns() {
            SqlBuilder sb = Dsl.ACSB.insert("firstName", "lastName", "email");
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        void testInsertSingleColumn() {
            SqlBuilder sb = Dsl.ACSB.insert("name");
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("age", 30);

            SqlBuilder sb = Dsl.ACSB.insert(props);
            assertNotNull(sb);

            String sql = sb.into("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testParseCondition() {
            Condition cond = Filters.or(Filters.eq("status", "'ACTIVE'"), Filters.eq("status", "'PENDING'"));

            SqlBuilder sb = Dsl.ACSB.renderCondition(cond, User.class);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("OR"));
        }

        @Test
        void testParseNullCondition() {
            assertThrows(IllegalArgumentException.class, () -> Dsl.ACSB.renderCondition(null, User.class));
        }

        @Test
        void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = Dsl.ACSB.select(columns);
            assertNotNull(sb);

            String sql = sb.from("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClass() {
            SqlBuilder sb = Dsl.ACSB.select(User.class);
            assertNotNull(sb);

            String sql = sb.from("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "secretKey"));
            SqlBuilder sb = Dsl.ACSB.select(User.class, excluded);
            assertNotNull(sb);

            String sql = sb.from("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.ACSB.select(User.class, true);
            assertNotNull(sb);

            String sql = sb.from("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.select(User.class, true, excluded);
            assertNotNull(sb);

            String sql = sb.from("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClass() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u", true, excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u");
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u", excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u", true);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, true);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, true, excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u1", "user1", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder sb = Dsl.ACSB.selectFrom(selections);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromTwoEntities() {
            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u1", "user1", User.class, "u2", "user2");
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SqlBuilder sb = Dsl.ACSB.selectFrom(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectMultipleColumns() {
            SqlBuilder sb = Dsl.ACSB.select("firstName", "lastName", "email");
            assertNotNull(sb);

            String sql = sb.from("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u1", "user1", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder sb = Dsl.ACSB.select(selections);
            assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectSingleExpression() {
            SqlBuilder sb = Dsl.ACSB.select("COUNT(DISTINCT userId)");
            assertNotNull(sb);

            String sql = sb.from("orders").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("COUNT(DISTINCT"));
        }

        @Test
        void testSelectTwoEntities() {
            SqlBuilder sb = Dsl.ACSB.select(User.class, "u1", "user1", User.class, "u2", "user2");
            assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SqlBuilder sb = Dsl.ACSB.select(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("email", "emailAddress");

            SqlBuilder sb = Dsl.ACSB.select(aliases);
            assertNotNull(sb);

            String sql = sb.from("users").build().query();
            assertNotNull(sql);
        }

        @Test
        void testUpdateEntityClass() {
            SqlBuilder sb = Dsl.ACSB.update(User.class);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder sb = Dsl.ACSB.update(User.class, excluded);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        void testUpdateTableName() {
            SqlBuilder sb = Dsl.ACSB.update("users");
            assertNotNull(sb);

            String sql = sb.set("status", "ACTIVE").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE"));
        }

        @Test
        void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.ACSB.update("users", User.class);
            assertNotNull(sb);

            String sql = sb.set("status").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
        }

        public static class User {
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            private String status;
            private Date createdDate;

            public User() {
            }

            public User(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
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
        }
    }

    @Nested
    class FactoryContracts {
        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Factory.class, names = { "PSB", "PAC", "PLC" })
        void testCreateInstance(final Factory factory) {
            // Each factory call returns a new SqlBuilder instance
            SqlBuilder instance1 = factory.dsl.select("*");
            SqlBuilder instance2 = factory.dsl.select("*");

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
            assertSql("SELECT * FROM first_table", List.of(), instance1.from("first_table").build());
            assertSql("SELECT * FROM second_table", List.of(), instance2.from("second_table").build());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Factory.class, names = { "NAC", "NLC" })
        void testEmptyArgumentsThrow(final Factory factory) {
            assertThrows(IllegalArgumentException.class, () -> {
                factory.dsl.select("");
            });

            assertThrows(IllegalArgumentException.class, () -> {
                factory.dsl.select(new String[0]);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                factory.dsl.select(new ArrayList<Selection>());
            });
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Factory.class, names = { "NSB", "NLC" })
        void testInsertReturning(final Factory factory) {
            assertSql("INSERT INTO users (firstName, lastName) VALUES (:firstName, :lastName) RETURNING id", List.of(),
                    factory.dsl.insert("firstName", "lastName").into("users").append(" RETURNING id").build());
        }

        @Test
        void testMetadataFailuresReleaseAllocatedBuilders() throws Exception {
            final var counterField = AbstractQueryBuilder.class.getDeclaredField("activeStringBuilderCounter");
            counterField.setAccessible(true);
            final var counter = (java.util.concurrent.atomic.AtomicInteger) counterField.get(null);
            final List<Runnable> factories = List.of(() -> PSC.update("broken", UnsupportedDateEntity.class),
                    () -> PSC.deleteFrom("broken", UnsupportedDateEntity.class), () -> PSC.deleteFrom(UnsupportedDateEntity.class),
                    () -> PSC.select(List.of(Selection.builder(UnsupportedDateEntity.class).includedPropNames(List.of("id")).build())));
            for (final Runnable factory : factories) {
                final int before = counter.get();
                final var failure = assertThrows(UnsupportedOperationException.class, factory::run);
                assertTrue(failure.getMessage().contains("Date format cannot be 'long'"));
                assertEquals(before, counter.get(), "A rejected factory must release its pooled builder");
            }
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Factory.class, names = { "MSB", "MSC" })
        void testSelectSingleExpression(final Factory factory) {
            assertSql("SELECT COUNT(*) FROM users", List.of(), factory.dsl.select("COUNT(*)").from("users").build());
        }

        // Regression: the factories released a builder they could not return only on RuntimeException | Error, so a checked
        // exception thrown sneakily by a named-parameter handler (Kotlin, @SneakyThrows) leaked its pooled buffer and the
        // active-builder count.
        @Test
        void testSneakyCheckedHandlerFailureReleasesAllocatedBuilder() {
            final java.io.IOException handlerFailure = new java.io.IOException("handler I/O failure");
            final Dsl sneakyDsl = Dsl.forDialect(NSC.sqlDialect().toBuilder().namedParameterHandler((sb, name) -> {
                if ("firstName".equals(name)) {
                    throw DslTest.<RuntimeException> sneakyThrow(handlerFailure);
                }

                sb.append(':').append(name);
            }).build());
            final int before = AbstractQueryBuilder.activeStringBuilderCounter.get();

            final Throwable failure = assertThrows(Throwable.class, () -> sneakyDsl.insertInto(com.landawn.abacus.query.entity.Account.class));
            assertTrue(failure == handlerFailure, String.valueOf(failure));
            assertEquals(before, AbstractQueryBuilder.activeStringBuilderCounter.get(), "A rejected factory must release its pooled builder");

            // Control: a handler that does not throw renders the statement.
            assertTrue(NSC.insertInto(com.landawn.abacus.query.entity.Account.class).build().query().startsWith("INSERT INTO account ("));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Factory.class, names = { "NAC", "NLC" })
        void testTableNamePreservation(final Factory factory) {
            assertSql("SELECT * FROM userAccounts", List.of(), factory.dsl.select("*").from("userAccounts").build());
        }
    }

    @Nested
    class LCSBTest {
        @Test
        void testAggregateFunctions() {
            SqlBuilder sb = Dsl.LCSB.select("status", "COUNT(*) as count", "AVG(totalPurchases) as avgPurchases", "MAX(lastLoginDate) as lastActive")
                    .from("customers")
                    .groupBy("status")
                    .having(Filters.gt("COUNT(*)", 10));

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("COUNT"));
            assertTrue(sql.contains("AVG"));
            assertTrue(sql.contains("MAX"));
        }

        @Test
        void testBatchInsert() {
            List<Customer> customers = Arrays.asList(new Customer("Dave", "Wilson", "dave@example.com"), new Customer("Eve", "Brown", "eve@example.com"));

            SqlBuilder sb = Dsl.LCSB.batchInsert(customers);
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testBatchInsertWithMaps() {
            List<Map<String, Object>> propsList = new ArrayList<>();

            Map<String, Object> customer1 = new HashMap<>();
            customer1.put("firstName", "Frank");
            customer1.put("lastName", "Miller");
            propsList.add(customer1);

            Map<String, Object> customer2 = new HashMap<>();
            customer2.put("firstName", "Grace");
            customer2.put("lastName", "Lee");
            propsList.add(customer2);

            SqlBuilder sb = Dsl.LCSB.batchInsert(propsList);
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testCaseWhenExpression() {
            String caseExpr = "CASE WHEN status = 'PREMIUM' THEN 'VIP' " + "WHEN status = 'GOLD' THEN 'Important' " + "ELSE 'Regular' END AS customerType";

            SqlBuilder sb = Dsl.LCSB.select("firstName", "lastName", caseExpr).from("customers");

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("case when"));
        }

        @Test
        void testComplexQueryWithJoins() {
            SqlBuilder sb = Dsl.LCSB.select("c.firstName", "c.lastName", "SUM(o.amount)")
                    .from("customers c")
                    .innerJoin("orders o")
                    .on("c.customerId = o.customerId")
                    .where(Filters.eq("c.status", "'ACTIVE'"))
                    .groupBy("c.customerId", "c.firstName", "c.lastName")
                    .having(Filters.gt("SUM(o.amount)", 10000))
                    .orderBy("SUM(o.amount) DESC");

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INNER JOIN"));
            assertTrue(sql.contains("GROUP BY"));
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        void testCountEntityClass() {
            SqlBuilder sb = Dsl.LCSB.selectCountFrom(Customer.class);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("count(*)"));
        }

        @Test
        void testCountTableName() {
            SqlBuilder sb = Dsl.LCSB.selectCountFrom("customers");
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("count(*)"));
        }

        @Test
        void testCreateInstance() {
            // Each factory call returns a new SqlBuilder instance
            SqlBuilder instance1 = Dsl.LCSB.select("*");
            SqlBuilder instance2 = Dsl.LCSB.select("*");
            assertNotNull(instance1);
            assertNotSame(instance1, instance2);
        }

        @Test
        void testDeleteFromEntityClass() {
            SqlBuilder sb = Dsl.LCSB.deleteFrom(Customer.class);
            assertNotNull(sb);

            String sql = sb.where(Filters.isNull("emailAddress")).build().query();
            assertNotNull(sql);
        }

        @Test
        void testDeleteFromTableName() {
            SqlBuilder sb = Dsl.LCSB.deleteFrom("customers");
            assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.LCSB.deleteFrom("customers", Customer.class);
            assertNotNull(sb);

            String sql = sb.where(Filters.lt("lastLoginDate", "2020-01-01")).build().query();
            assertNotNull(sql);
        }

        @Test
        void testEmptyMultiSelects() {
            // Test null selections list
            assertThrows(RuntimeException.class, () -> Dsl.LCSB.select((List<Selection>) null));

            // Test empty selections list
            assertThrows(RuntimeException.class, () -> Dsl.LCSB.select(Collections.<String> emptyList()));
        }

        @Test
        void testEmptySelectParts() {
            // Test empty string
            assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select(""));

            // Test null string
            assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select((String) null));

            // Test empty array
            assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select(new String[0]));

            // Test empty collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select(Collections.<String> emptyList()));

            // Test empty map
            assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.select(Collections.emptyMap()));
        }

        @Test
        void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "emailAddress", "phoneNumber");
            SqlBuilder sb = Dsl.LCSB.insert(columns);
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntity() {
            Customer customer = new Customer("Bob", "Smith", "bob@example.com");
            customer.setPhoneNumber("123-456-7890");

            SqlBuilder sb = Dsl.LCSB.insert(customer);
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntityClass() {
            SqlBuilder sb = Dsl.LCSB.insert(Customer.class);
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SqlBuilder sb = Dsl.LCSB.insert(Customer.class, excluded);
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntityWithExclusions() {
            Customer customer = new Customer("Carol", "Davis");
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));

            SqlBuilder sb = Dsl.LCSB.insert(customer, excluded);
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertInto() {
            SqlBuilder sb = Dsl.LCSB.insertInto(Customer.class);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId"));
            SqlBuilder sb = Dsl.LCSB.insertInto(Customer.class, excluded);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        void testInsertMultipleColumns() {
            SqlBuilder sb = Dsl.LCSB.insert("firstName", "lastName", "emailAddress");
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        void testInsertSingleColumn() {
            SqlBuilder sb = Dsl.LCSB.insert("customerName");
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "Alice");
            props.put("lastName", "Johnson");
            props.put("emailAddress", "alice@example.com");

            SqlBuilder sb = Dsl.LCSB.insert(props);
            assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testParseComplexCondition() {
            Condition cond = Filters.or(Filters.and(Filters.eq("status", "'PREMIUM'"), Filters.gt("totalPurchases", 5000)),
                    Filters.and(Filters.eq("status", "'GOLD'"), Filters.gt("totalPurchases", 3000)));

            SqlBuilder sb = Dsl.LCSB.renderCondition(cond, Customer.class);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("OR"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testParseCondition() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.between("registrationDate", "2020-01-01", "2023-12-31"));

            SqlBuilder sb = Dsl.LCSB.renderCondition(cond, Customer.class);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("BETWEEN"));
        }

        @Test
        void testParseNullCondition() {
            assertThrows(IllegalArgumentException.class, () -> Dsl.LCSB.renderCondition(null, Customer.class));
        }

        @Test
        void testQueryWithLimit() {
            SqlBuilder sb = Dsl.LCSB.select("*").from("customers").where(Filters.eq("status", "'ACTIVE'")).orderBy("registrationDate DESC").limit(10);

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testQueryWithLimitAndOffset() {
            SqlBuilder sb = Dsl.LCSB.select("*").from("customers").where(Filters.eq("status", "'ACTIVE'")).orderBy("customerId").limit(20, 10);

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testQueryWithSubquery() {
            SubQuery subquery = Filters.subQuery("orders", N.asList("customerId"), Filters.gt("amount", 1000));

            SqlBuilder sb = Dsl.LCSB.select("*").from("customers").where(Filters.in("customerId", subquery));

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("IN"));
        }

        @Test
        void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("customerId", "firstName", "lastName");
            SqlBuilder sb = Dsl.LCSB.select(columns);
            assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectDistinct() {
            SqlBuilder sb = Dsl.LCSB.select("DISTINCT status").from("customers").orderBy("status");

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        void testSelectEntityClass() {
            SqlBuilder sb = Dsl.LCSB.select(Customer.class);
            assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber", "registrationDate"));
            SqlBuilder sb = Dsl.LCSB.select(Customer.class, excluded);
            assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.LCSB.select(Customer.class, true);
            assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.select(Customer.class, true, excluded);
            assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c", true, excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c");
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c", excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c", true);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, true);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, true, excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Customer.class, "c1", "customer1", null, false, null),
                    selection(Customer.class, "c2", "customer2", null, false, null));

            SqlBuilder sb = Dsl.LCSB.selectFrom(selections);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromTwoEntities() {
            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = Dsl.LCSB.selectFrom(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectMultipleColumns() {
            SqlBuilder sb = Dsl.LCSB.select("firstName", "lastName", "emailAddress");
            assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Customer.class, "c1", "customer1", null, false, null),
                    selection(Customer.class, "c2", "customer2", null, false, null), selection(Customer.class, "c3", "customer3", null, false, null));

            SqlBuilder sb = Dsl.LCSB.select(selections);
            assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2, customers c3").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectSingleExpression() {
            SqlBuilder sb = Dsl.LCSB.select("COUNT(*) as totalCount");
            assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("totalCount"));
        }

        @Test
        void testSelectTwoEntities() {
            SqlBuilder sb = Dsl.LCSB.select(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder sb = Dsl.LCSB.select(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");

            SqlBuilder sb = Dsl.LCSB.select(aliases);
            assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            assertNotNull(sql);
        }

        @Test
        void testUnionQuery() {
            SqlBuilder query1 = Dsl.LCSB.select("firstName", "lastName").from("customers").where(Filters.eq("status", "'ACTIVE'"));

            SqlBuilder query2 = Dsl.LCSB.select("firstName", "lastName").from("employees").where(Filters.eq("department", "'SALES'"));

            SqlBuilder sb = query1.union(query2);

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UNION"));
        }

        @Test
        void testUpdateEntityClass() {
            SqlBuilder sb = Dsl.LCSB.update(Customer.class);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("customerId", 100)).build().query());
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SqlBuilder sb = Dsl.LCSB.update(Customer.class, excluded);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("customerId", 100)).build().query());
        }

        @Test
        void testUpdateTableName() {
            SqlBuilder sb = Dsl.LCSB.update("customers");
            assertNotNull(sb);

            String sql = sb.set("status", "PREMIUM").where(Filters.gt("totalPurchases", 1000)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE"));
        }

        @Test
        void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.LCSB.update("customers", Customer.class);
            assertNotNull(sb);

            String sql = sb.set("status").where(Filters.eq("customerId", 100)).build().query();
            assertNotNull(sql);
        }

        @Test
        void testUpdateWithMultipleSet() {
            SqlBuilder sb = Dsl.LCSB.update("customers")
                    .set("status", "INACTIVE")
                    .set("lastModified", SqlExpression.of("CURRENT_TIMESTAMP"))
                    .where(Filters.lt("lastLoginDate", "2020-01-01"));

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE"));
            assertTrue(sql.contains("SET"));
        }

        public static class Customer {
            private Long customerId;
            private String firstName;
            private String lastName;
            private String emailAddress;
            private String phoneNumber;
            private String status;
            private Date registrationDate;

            public Customer() {
            }

            public Customer(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            public Customer(String firstName, String lastName, String email) {
                this.firstName = firstName;
                this.lastName = lastName;
                this.emailAddress = email;
            }

            // Getters and setters
            public Long getCustomerId() {
                return customerId;
            }

            public void setCustomerId(Long customerId) {
                this.customerId = customerId;
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

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public String getPhoneNumber() {
                return phoneNumber;
            }

            public void setPhoneNumber(String phoneNumber) {
                this.phoneNumber = phoneNumber;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public Date getRegistrationDate() {
                return registrationDate;
            }

            public void setRegistrationDate(Date registrationDate) {
                this.registrationDate = registrationDate;
            }
        }
    }

    @Nested
    class MACTest {
        @Test
        void testBatchInsert() {
            List<Account> accounts = new ArrayList<>();
            Account acc1 = new Account();
            acc1.setFirstName("John");
            acc1.setLastName("Doe");
            Account acc2 = new Account();
            acc2.setFirstName("Jane");
            acc2.setLastName("Smith");
            accounts.add(acc1);
            accounts.add(acc2);

            String sql = Dsl.MAC.batchInsert(accounts).into("ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("(FIRST_NAME, LAST_NAME)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{firstName_0}"));
            assertTrue(sql.contains("#{lastName_1}"));
        }

        @Test
        void testCountEntityClass() {
            String sql = Dsl.MAC.selectCountFrom(Account.class)
                    .where(Filters.between("createdDate", new Date(1_700_000_000_000L), new Date(1_700_000_000_000L)))
                    .build()
                    .query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertTrue(sql.contains("CREATED_DATE BETWEEN"));
        }

        @Test
        void testCountTable() {
            String sql = Dsl.MAC.selectCountFrom("ACCOUNT").where(Filters.eq("isActive", true)).build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
        }

        @Test
        void testDeleteFromEntityClass() {
            String sql = Dsl.MAC.deleteFrom(Account.class).where(Filters.in("id", Arrays.asList(1, 2, 3))).build().query();
            assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("ID IN"));
        }

        @Test
        void testDeleteFromTable() {
            String sql = Dsl.MAC.deleteFrom("ACCOUNT").where(Filters.eq("status", "INACTIVE")).build().query();
            assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("STATUS = #{status}"));
        }

        @Test
        void testDeleteFromTableWithEntityClass() {
            String sql = Dsl.MAC.deleteFrom("ACCOUNT", Account.class)
                    .where(Filters.and(Filters.eq("isActive", false), Filters.lt("lastLoginDate", new Date(1_700_000_000_000L))))
                    .build()
                    .query();
            assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("LAST_LOGIN_DATE < #{lastLoginDate}"));
        }

        @Test
        void testInsertIntoEntityClass() {
            String sql = Dsl.MAC.insertInto(Account.class).build().query();
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL"));
        }

        @Test
        void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = Dsl.MAC.insertInto(Account.class, excludes).build().query();
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL"));
            assertFalse(sql.contains("ID"));
        }

        @Test
        void testInsertMultipleColumns() {
            String sql = Dsl.MAC.insert("firstName", "lastName", "email").into("ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("(FIRST_NAME, LAST_NAME, EMAIL)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
            assertTrue(sql.contains("#{email}"));
        }

        @Test
        void testInsertSingleColumn() {
            String sql = Dsl.MAC.insert("firstName").into("ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("(FIRST_NAME)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{firstName}"));
        }

        @Test
        void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = Dsl.MAC.insert(columns).into("ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("(FIRST_NAME, LAST_NAME, EMAIL)"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
            assertTrue(sql.contains("#{email}"));
        }

        @Test
        void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setEmail("john@example.com");

            String sql = Dsl.MAC.insert(account).into("ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL"));
            assertFalse(sql.contains("TEMP_DATA")); // @Transient
            assertFalse(sql.contains("READ_ONLY_FIELD")); // @ReadOnly
        }

        @Test
        void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setId(1L);
            account.setFirstName("John");
            account.setLastName("Doe");

            Set<String> excludes = new HashSet<>(Arrays.asList("id"));
            String sql = Dsl.MAC.insert(account, excludes).into("ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertFalse(sql.contains("ID"));
        }

        @Test
        void testInsertWithEntityClass() {
            String sql = Dsl.MAC.insert(Account.class).into("ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL"));
            assertFalse(sql.contains("TEMP_DATA")); // @Transient
            assertFalse(sql.contains("READ_ONLY_FIELD")); // @ReadOnly
        }

        @Test
        void testInsertWithEntityClassAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "createdDate"));
            String sql = Dsl.MAC.insert(Account.class, excludes).into("ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL"));
            assertFalse(sql.contains("ID"));
            assertFalse(sql.contains("CREATED_DATE"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            String sql = Dsl.MAC.insert(props).into("ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = Dsl.MAC.renderCondition(cond, Account.class).build().query();
            assertTrue(sql.contains("STATUS = #{status}"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("BALANCE > #{balance}"));
        }

        @Test
        void testSelectEntityClass() {
            String sql = Dsl.MAC.select(Account.class).from("ACCOUNT").where(Filters.eq("isActive", true)).build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("AS \"id\""));
            assertTrue(sql.contains("AS \"firstName\""));
            assertTrue(sql.contains("AS \"lastName\""));
            assertFalse(sql.contains("TEMP_DATA")); // @Transient
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("passwordHash", "securityToken"));
            String sql = Dsl.MAC.select(Account.class, excludes).from("ACCOUNT").build().query();
            assertTrue(sql.contains("AS \"firstName\""));
            assertTrue(sql.contains("AS \"lastName\""));
            assertFalse(sql.contains("passwordHash"));
            assertFalse(sql.contains("securityToken"));
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.MAC.select(Account.class, true).from("ACCOUNT").innerJoin("ADDRESS").on("ACCOUNT.ADDRESS_ID = ADDRESS.ID").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertTrue(sql.contains("INNER JOIN ADDRESS"));
        }

        @Test
        void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = Dsl.MAC.select(Account.class, true, excludes)
                    .from("ACCOUNT")
                    .innerJoin("PROFILE")
                    .on("ACCOUNT.PROFILE_ID = PROFILE.ID")
                    .build()
                    .query();
            assertTrue(sql.contains("SELECT"));
            assertFalse(sql.contains("internalNotes"));
        }

        @Test
        void testSelectFromEntityClass() {
            String sql = Dsl.MAC.selectFrom(Account.class).where(Filters.eq("isActive", true)).orderBy("createdDate DESC").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            assertTrue(sql.contains("ORDER BY CREATED_DATE DESC"));
        }

        @Test
        void testSelectFromEntityClassWithAlias() {
            String sql = Dsl.MAC.selectFrom(Account.class, "a")
                    .innerJoin("PROFILE p")
                    .on("a.PROFILE_ID = p.ID")
                    .where(Filters.eq("a.isActive", true))
                    .build()
                    .query();
            assertTrue(sql.contains("FROM ACCOUNT a"));
            assertTrue(sql.contains("INNER JOIN PROFILE p"));
            // Table-alias prefix is stripped from the named-parameter identifier so
            // the placeholder remains a valid iBATIS expression.
            assertTrue(sql.contains("a.IS_ACTIVE = #{isActive}"));
        }

        @Test
        void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = Dsl.MAC.selectFrom(Account.class, "a", excludes).where(Filters.like("a.emailAddress", "%@example.com")).build().query();
            assertTrue(sql.contains("FROM ACCOUNT a"));
            assertFalse(sql.contains("password"));
            // Table-alias prefix is stripped from the iBATIS placeholder identifier.
            assertTrue(sql.contains("a.EMAIL_ADDRESS LIKE #{emailAddress}"));
        }

        @Test
        void testSelectFromEntityClassWithAliasAndSubEntities() {
            String sql = Dsl.MAC.selectFrom(Account.class, "acc", true).innerJoin("PROFILE p").on("acc.PROFILE_ID = p.ID").build().query();
            assertTrue(sql.contains("FROM ACCOUNT acc"));
            assertTrue(sql.contains("INNER JOIN PROFILE p"));
        }

        @Test
        void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
            String sql = Dsl.MAC.selectFrom(Account.class, excludes).where(Filters.eq("status", "ACTIVE")).build().query();
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertFalse(sql.contains("largeBlob"));
            assertFalse(sql.contains("internalData"));
        }

        @Test
        void testSelectFromEntityClassWithFullOptions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("debugInfo"));
            String sql = Dsl.MAC.selectFrom(Account.class, "acc", true, excludes)
                    .innerJoin("ORDERS o")
                    .on("acc.ID = o.ACCOUNT_ID")
                    .innerJoin("ITEMS i")
                    .on("o.ID = i.ORDER_ID")
                    .where(Filters.gt("o.total", 100))
                    .build()
                    .query();
            assertTrue(sql.contains("FROM ACCOUNT acc"));
            assertFalse(sql.contains("debugInfo"));
            // Table-alias prefix is stripped from the iBATIS placeholder identifier.
            assertTrue(sql.contains("O.TOTAL > #{total}"));
        }

        @Test
        void testSelectFromEntityClassWithSubEntities() {
            String sql = Dsl.MAC.selectFrom(Account.class, true).innerJoin("ADDRESS").on("ACCOUNT.ADDRESS_ID = ADDRESS.ID").build().query();
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertTrue(sql.contains("INNER JOIN ADDRESS"));
        }

        @Test
        void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
            String sql = Dsl.MAC.selectFrom(Account.class, true, excludes).innerJoin("ORDERS").on("ACCOUNT.ID = ORDERS.ACCOUNT_ID").build().query();
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertFalse(sql.contains("temporaryData"));
        }

        @Test
        void testSelectMultipleColumns() {
            String sql = Dsl.MAC.select("firstName", "lastName", "emailAddress")
                    .from("ACCOUNT")
                    .where(Filters.gt("createdDate", new Date(1_700_000_000_000L)))
                    .build()
                    .query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertTrue(sql.contains("CREATED_DATE > #{createdDate}"));
        }

        @Test
        void testSelectSingleExpression() {
            String sql = Dsl.MAC.select("COUNT(*) AS total, MAX(createdDate) AS latest").from("ACCOUNT").build().query();
            assertEquals("SELECT COUNT(*) AS total, MAX(createdDate) AS latest FROM ACCOUNT", sql);
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");
            String sql = Dsl.MAC.select(aliases).from("ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            assertTrue(sql.contains("LAST_NAME AS \"lname\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"email\""));
        }

        @Test
        void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
            String sql = Dsl.MAC.select(columns).from("ACCOUNT").orderBy("createdDate DESC").build().query();
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("PHONE_NUMBER AS \"phoneNumber\""));
            assertTrue(sql.contains("ORDER BY CREATED_DATE DESC"));
        }

        @Test
        void testUpdateEntityClass() {
            String sql = Dsl.MAC.update(Account.class)
                    .set("status", "ACTIVE")
                    .set(Map.of("lastLoginDate", new Date(1_700_000_000_000L)))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            assertTrue(sql.contains("UPDATE ACCOUNT"));
            assertTrue(sql.contains("STATUS = #{status}"));
            assertTrue(sql.contains("LAST_LOGIN_DATE = #{lastLoginDate}"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "createdDate", "createdBy"));
            String sql = Dsl.MAC.update(Account.class, excludes)
                    .set("firstName", "John")
                    .set(Map.of("modifiedDate", new Date(1_700_000_000_000L)))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            assertTrue(sql.contains("UPDATE ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = #{firstName}"));
            assertTrue(sql.contains("MODIFIED_DATE = #{modifiedDate}"));
            assertFalse(sql.contains("CREATED_DATE"));
        }

        @Test
        void testUpdateTable() {
            String sql = Dsl.MAC.update("ACCOUNT")
                    .set("firstName", "John")
                    .set("lastName", "Doe")
                    .set(Map.of("modifiedDate", new Date(1_700_000_000_000L)))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            assertTrue(sql.contains("UPDATE ACCOUNT"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("FIRST_NAME = #{firstName}"));
            assertTrue(sql.contains("LAST_NAME = #{lastName}"));
            assertTrue(sql.contains("MODIFIED_DATE = #{modifiedDate}"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        void testUpdateTableWithEntityClass() {
            String sql = Dsl.MAC.update("ACCOUNT", Account.class)
                    .set(Map.of("isActive", false))
                    .set(Map.of("deactivatedDate", new Date(1_700_000_000_000L)))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            assertTrue(sql.contains("UPDATE ACCOUNT"));
            assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            assertTrue(sql.contains("DEACTIVATED_DATE = #{deactivatedDate}"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("ID = #{id}"));
        }

        @Table("ACCOUNT")
        public static class Account {
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            private Date createdDate;
            @Transient
            private String tempData;
            @ReadOnly
            private String readOnlyField;
            @NonUpdatable
            private String nonUpdatableField;

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

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }
        }
    }

    @Nested
    class MLCTest {
        @Test
        void testBatchInsert() {
            List<Account> accounts = new ArrayList<>();
            Account acc1 = new Account();
            acc1.setFirstName("John");
            acc1.setLastName("Doe");
            Account acc2 = new Account();
            acc2.setFirstName("Jane");
            acc2.setLastName("Smith");
            accounts.add(acc1);
            accounts.add(acc2);

            String sql = Dsl.MLC.batchInsert(accounts).into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("(firstName, lastName)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{firstName_0}"));
            assertTrue(sql.contains("#{lastName_1}"));
        }

        @Test
        void testCountEntityClass() {
            String sql = Dsl.MLC.selectCountFrom(Account.class)
                    .where(Filters.between("createdDate", new Date(1_700_000_000_000L), new Date(1_700_000_000_000L)))
                    .build()
                    .query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("createdDate BETWEEN"));
        }

        @Test
        void testCountTable() {
            String sql = Dsl.MLC.selectCountFrom("account").where(Filters.eq("isActive", true)).build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("isActive = #{isActive}"));
        }

        @Test
        void testDeleteFromEntityClass() {
            String sql = Dsl.MLC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        void testDeleteFromTable() {
            String sql = Dsl.MLC.deleteFrom("account").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        void testDeleteFromTableWithEntityClass() {
            String sql = Dsl.MLC.deleteFrom("account", Account.class).where(Filters.eq("emailAddress", "john@example.com")).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("emailAddress = #{emailAddress}"));
        }

        @Test
        void testInsertIntoEntityClass() {
            String sql = Dsl.MLC.insertInto(Account.class).build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
        }

        @Test
        void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = Dsl.MLC.insertInto(Account.class, excludes).build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertFalse(sql.contains("id"));
        }

        @Test
        void testInsertMultipleColumns() {
            String sql = Dsl.MLC.insert("firstName", "lastName", "emailAddress").into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("(firstName, lastName, emailAddress)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
            assertTrue(sql.contains("#{emailAddress}"));
        }

        @Test
        void testInsertSingleColumn() {
            String sql = Dsl.MLC.insert("firstName").into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("(firstName)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{firstName}"));
        }

        @Test
        void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
            String sql = Dsl.MLC.insert(columns).into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("(firstName, lastName, phoneNumber)"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
            assertTrue(sql.contains("#{phoneNumber}"));
        }

        @Test
        void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedDate(new Date(1_700_000_000_000L));

            String sql = Dsl.MLC.insert(account).into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("createdDate"));
            assertFalse(sql.contains("tempData")); // @Transient
            assertFalse(sql.contains("readOnlyField")); // @ReadOnly
        }

        @Test
        void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setId(1L);
            account.setFirstName("John");
            account.setLastName("Doe");

            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = Dsl.MLC.insert(account, excludes).into("account").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertFalse(sql.contains("id"));
        }

        @Test
        void testInsertWithEntityClass() {
            String sql = Dsl.MLC.insert(Account.class).into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertFalse(sql.contains("tempData")); // @Transient
            assertFalse(sql.contains("readOnlyField")); // @ReadOnly
        }

        @Test
        void testInsertWithEntityClassAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "auditFields"));
            String sql = Dsl.MLC.insert(Account.class, excludes).into("account").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertFalse(sql.contains("id"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("isActive", true);
            String sql = Dsl.MLC.insert(props).into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("isActive"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
            assertTrue(sql.contains("#{isActive}"));
        }

        @Test
        void testNamedHavingFunctionExpressionUsesValidPlaceholder() {
            AbstractQueryBuilder.SP sp = Dsl.NLC.select("department", "COUNT(*)")
                    .from("employees")
                    .groupBy("department")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build();

            assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > :COUNT", sp.query());
            assertEquals(Arrays.asList(5), sp.parameters());
        }

        @Test
        void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = Dsl.MLC.renderCondition(cond, Account.class).build().query();
            assertTrue(sql.contains("status = #{status}"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("balance > #{balance}"));
        }

        @Test
        void testSelectEntityClass() {
            String sql = Dsl.MLC.select(Account.class).from("account").where(Filters.eq("isActive", true)).build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertTrue(sql.contains("isActive"));
            assertFalse(sql.contains("tempData")); // @Transient
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password", "secretKey"));
            String sql = Dsl.MLC.select(Account.class, excludes).from("account").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertFalse(sql.contains("password"));
            assertFalse(sql.contains("secretKey"));
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.MLC.select(Account.class, true).from("account").innerJoin("address").on("account.addressId = address.id").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("INNER JOIN address"));
        }

        @Test
        void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = Dsl.MLC.select(Account.class, true, excludes)
                    .from("account")
                    .innerJoin("profile")
                    .on("account.profileId = profile.id")
                    .build()
                    .query();
            assertTrue(sql.contains("SELECT"));
            assertFalse(sql.contains("internalNotes"));
        }

        @Test
        void testSelectFromEntityClass() {
            String sql = Dsl.MLC.selectFrom(Account.class).where(Filters.eq("isActive", true)).orderBy("createdDate DESC").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("isActive = #{isActive}"));
            assertTrue(sql.contains("ORDER BY createdDate DESC"));
        }

        @Test
        void testSelectFromEntityClassWithAlias() {
            String sql = Dsl.MLC.selectFrom(Account.class, "a")
                    .innerJoin("profile p")
                    .on("a.profileId = p.id")
                    .where(Filters.like("a.emailAddress", "%@example.com"))
                    .build()
                    .query();
            assertTrue(sql.contains("FROM account a"));
            assertTrue(sql.contains("INNER JOIN profile p"));
            // Table-alias prefix is stripped from the iBATIS placeholder identifier.
            assertTrue(sql.contains("a.emailAddress LIKE #{emailAddress}"));
        }

        @Test
        void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = Dsl.MLC.selectFrom(Account.class, "a", excludes).where(Filters.like("a.emailAddress", "%@example.com")).build().query();
            assertTrue(sql.contains("FROM account a"));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectFromEntityClassWithAliasAndSubEntities() {
            String sql = Dsl.MLC.selectFrom(Account.class, "acc", true).innerJoin("profile p").on("acc.profileId = p.id").build().query();
            assertTrue(sql.contains("FROM account acc"));
            assertTrue(sql.contains("INNER JOIN profile p"));
        }

        @Test
        void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
            String sql = Dsl.MLC.selectFrom(Account.class, excludes).where(Filters.eq("status", "ACTIVE")).build().query();
            assertTrue(sql.contains("FROM account"));
            assertFalse(sql.contains("largeBlob"));
            assertFalse(sql.contains("internalData"));
        }

        @Test
        void testSelectFromEntityClassWithFullOptions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("debugInfo"));
            String sql = Dsl.MLC.selectFrom(Account.class, "acc", true, excludes)
                    .innerJoin("orders o")
                    .on("acc.id = o.accountId")
                    .innerJoin("items i")
                    .on("o.id = i.orderId")
                    .where(Filters.gt("o.total", 100))
                    .build()
                    .query();
            assertTrue(sql.contains("FROM account acc"));
            assertFalse(sql.contains("debugInfo"));
            // Table-alias prefix is stripped from the iBATIS placeholder identifier.
            assertTrue(sql.contains("o.total > #{total}"));
        }

        @Test
        void testSelectFromEntityClassWithSubEntities() {
            String sql = Dsl.MLC.selectFrom(Account.class, true).innerJoin("address").on("account.addressId = address.id").build().query();
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("INNER JOIN address"));
        }

        @Test
        void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
            String sql = Dsl.MLC.selectFrom(Account.class, true, excludes).innerJoin("orders").on("account.id = orders.accountId").build().query();
            assertTrue(sql.contains("FROM account"));
            assertFalse(sql.contains("temporaryData"));
        }

        @Test
        void testSelectFromMultipleEntities() {
            // The join predicate between the two comma-listed entity references belongs in WHERE: a qualified
            // JOIN whose "table" is a predicate can never be completed with on()/using().
            String sql = Dsl.MLC.selectFrom(Account.class, "a", "account", Account.class, "a2", "account2")
                    .where(Filters.and(Filters.expr("a.id = a2.parentId"), Filters.gt("a.createdDate", new Date(1_700_000_000_000L))))
                    .build()
                    .query();
            assertTrue(sql.contains("FROM account a, account a2 WHERE (a.id = a2.parentId) AND (a.createdDate > #{createdDate})"), sql);
            assertTrue(sql.contains("account."));
            assertTrue(sql.contains("account2."));
        }

        @Test
        void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> accountExcludes = new HashSet<>(Arrays.asList("sensitiveData"));
            String sql = Dsl.MLC.selectFrom(Account.class, "a", "account", accountExcludes, Account.class, "a2", "account2", null)
                    .where(Filters.expr("a.id = a2.parentId"))
                    .build()
                    .query();
            assertTrue(sql.endsWith("FROM account a, account a2 WHERE a.id = a2.parentId"), sql);
            assertFalse(sql.contains("sensitiveData"));
        }

        @Test
        void testSelectFromWithMultipleSelections() {
            List<Selection> selections = createComplexSelections();
            String sql = Dsl.MLC.selectFrom(selections)
                    .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 0)))
                    .groupBy("account.type")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build()
                    .query();
            assertTrue(sql.contains("FROM"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("GROUP BY account.type"));
            assertTrue(sql.contains("HAVING COUNT(*) > #{COUNT}"));
        }

        @Test
        void testSelectMultipleColumns() {
            String sql = Dsl.MLC.select("firstName", "lastName", "emailAddress")
                    .from("account")
                    .where(Filters.gt("createdDate", new Date(1_700_000_000_000L)))
                    .build()
                    .query();
            assertTrue(sql.contains("SELECT firstName, lastName, emailAddress"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("createdDate > #{createdDate}"));
        }

        @Test
        void testSelectMultipleEntities() {
            String sql = Dsl.MLC.select(Account.class, "a", "account", Account.class, "a2", "account2")
                    .from("account a")
                    .innerJoin("account a2")
                    .on("a.id = a2.parentId")
                    .build()
                    .query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("account."));
            assertTrue(sql.contains("account2."));
        }

        @Test
        void testSelectMultipleEntitiesWithExclusions() {
            Set<String> accountExcludes = new HashSet<>(Arrays.asList("password"));
            Set<String> orderExcludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = Dsl.MLC.select(Account.class, "a", "account", accountExcludes, Account.class, "a2", "account2", orderExcludes)
                    .from("account a")
                    .innerJoin("account a2")
                    .on("a.id = a2.parentId")
                    .where(Filters.gt("a.createdDate", new Date(1_700_000_000_000L)))
                    .build()
                    .query();
            assertTrue(sql.contains("account."));
            assertTrue(sql.contains("account2."));
            assertFalse(sql.contains("password"));
            assertFalse(sql.contains("internalNotes"));
        }

        @Test
        void testSelectSingleExpression() {
            String sql = Dsl.MLC.select("COUNT(*) AS total, MAX(createdDate) AS latest").from("account").build().query();
            assertTrue(sql.contains("SELECT COUNT(*) AS total, MAX(createdDate) AS latest"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");
            String sql = Dsl.MLC.select(aliases).from("account").build().query();
            assertTrue(sql.contains("firstName AS \"fname\""));
            assertTrue(sql.contains("lastName AS \"lname\""));
            assertTrue(sql.contains("emailAddress AS \"email\""));
        }

        @Test
        void testSelectWithCollection() {
            List<String> columns = getColumnsToSelect();
            String sql = Dsl.MLC.select(columns).from("account").orderBy("createdDate DESC").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("ORDER BY createdDate DESC"));
        }

        @Test
        void testSelectWithMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, true, null),
                    selection(Account.class, "a2", "account2", Arrays.asList("id", "name"), false, null));
            String sql = Dsl.MLC.select(selections).from("account a").innerJoin("account a2").on("a.id = a2.parentId").build().query();
            assertTrue(sql.contains("account."));
            assertTrue(sql.contains("account2."));
        }

        @Test
        void testUpdateEntityClass() {
            String sql = Dsl.MLC.update(Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("firstName = #{firstName}"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate", "createdBy"));
            String sql = Dsl.MLC.update(Account.class, excludes).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("firstName = #{firstName}"));
            assertFalse(sql.contains("createdDate"));
        }

        @Test
        void testUpdateTable() {
            String sql = Dsl.MLC.update("account")
                    .set("firstName", "updatedName")
                    .set(Map.of("modifiedDate", new Date(1_700_000_000_000L)))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("firstName = #{firstName}"));
            assertTrue(sql.contains("modifiedDate = #{modifiedDate}"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        void testUpdateTableWithEntityClass() {
            String sql = Dsl.MLC.update("account", Account.class).set("firstName", "John", "lastName", "Doe").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("firstName = #{firstName}"));
            assertTrue(sql.contains("lastName = #{lastName}"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("id = #{id}"));
        }

        @Table("account")
        public static class Account {
            private Long id;
            private String firstName;
            private String lastName;
            private String emailAddress;
            private Boolean isActive;
            private Date createdDate;
            private Date modifiedDate;
            @Transient
            private String tempData;
            @ReadOnly
            private String readOnlyField;
            @NonUpdatable
            private String nonUpdatableField;

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

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public Boolean getIsActive() {
                return isActive;
            }

            public void setIsActive(Boolean isActive) {
                this.isActive = isActive;
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

        private List<String> getColumnsToSelect() {
            return Arrays.asList("firstName", "lastName", "emailAddress");
        }

        private List<Selection> createComplexSelections() {
            return Arrays.asList(selection(Account.class, "a", "account", null, false, null));
        }
    }

    @Nested
    class MSBTest {
        @Test
        void testBatchInsert() {
            List<Map<String, Object>> propsList = new ArrayList<>();
            Map<String, Object> props1 = new HashMap<>();
            props1.put("name", "John");
            props1.put("age", 30);
            Map<String, Object> props2 = new HashMap<>();
            props2.put("name", "Jane");
            props2.put("age", 25);
            propsList.add(props1);
            propsList.add(props2);

            String sql = Dsl.MSB.batchInsert(propsList).into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{name_0}"));
            assertTrue(sql.contains("#{name_1}"));
        }

        @Test
        void testCountEntityClass() {
            String sql = Dsl.MSB.selectCountFrom(Account.class)
                    .where(Filters.between("createdDate", new Date(1_700_000_000_000L), new Date(1_700_000_000_000L)))
                    .build()
                    .query();
            assertEquals("SELECT count(*) FROM test_account WHERE createdDate BETWEEN #{minCreatedDate} AND #{maxCreatedDate}", sql);
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM test_account"));
        }

        @Test
        void testCountTable() {
            String sql = Dsl.MSB.selectCountFrom("users").where(Filters.eq("active", true)).build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM users"));
            assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        void testDeleteFromEntityClass() {
            String sql = Dsl.MSB.deleteFrom(Account.class).where(Filters.lt("createdDate", new Date(1_700_000_000_000L))).build().query();
            assertTrue(sql.contains("DELETE FROM test_account"));
            assertTrue(sql.contains("createdDate < #{createdDate}"));
        }

        @Test
        void testDeleteFromTable() {
            String sql = Dsl.MSB.deleteFrom("users").where(Filters.eq("status", "INACTIVE")).build().query();
            assertTrue(sql.contains("DELETE FROM users"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("status = #{status}"));
        }

        @Test
        void testDeleteFromTableWithEntityClass() {
            String sql = Dsl.MSB.deleteFrom("users", Account.class).where(Filters.eq("firstName", "John")).build().query();
            assertTrue(sql.contains("DELETE FROM users"));
            assertTrue(sql.contains("firstName = #{firstName}"));
        }

        @Test
        void testInsertIntoEntityClass() {
            String sql = Dsl.MSB.insertInto(Account.class).build().query();
            assertTrue(sql.contains("INSERT INTO test_account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
        }

        @Test
        void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = Dsl.MSB.insertInto(Account.class, excludes).build().query();
            assertTrue(sql.contains("INSERT INTO test_account"));
            assertTrue(sql.contains("firstName"));
            assertFalse(sql.contains("email"));
        }

        @Test
        void testInsertMultipleColumns() {
            String sql = Dsl.MSB.insert("firstName", "lastName", "email").into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("(firstName, lastName, email)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
            assertTrue(sql.contains("#{email}"));
        }

        @Test
        void testInsertSingleColumn() {
            String sql = Dsl.MSB.insert("name").into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("(name)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{name}"));
        }

        @Test
        void testInsertWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "status");
            String sql = Dsl.MSB.insert(columns).into("products").build().query();
            assertTrue(sql.contains("INSERT INTO products"));
            assertTrue(sql.contains("(id, name, status)"));
            assertTrue(sql.contains("#{id}"));
            assertTrue(sql.contains("#{name}"));
            assertTrue(sql.contains("#{status}"));
        }

        @Test
        void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setEmail("john@example.com");

            String sql = Dsl.MSB.insert(account).into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("tempData")); // @Transient field should be excluded
            assertFalse(sql.contains("readOnlyField")); // @ReadOnly field should be excluded
        }

        @Test
        void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setEmail("john@example.com");

            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = Dsl.MSB.insert(account, excludes).into("users").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertFalse(sql.contains("email"));
        }

        @Test
        void testInsertWithEntityClass() {
            String sql = Dsl.MSB.insert(Account.class).into("test_account").build().query();
            assertTrue(sql.contains("INSERT INTO test_account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("tempData")); // @Transient
            assertFalse(sql.contains("readOnlyField")); // @ReadOnly
        }

        @Test
        void testInsertWithEntityClassAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate"));
            String sql = Dsl.MSB.insert(Account.class, excludes).into("test_account").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertFalse(sql.contains("createdDate"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("name", "John");
            props.put("age", 30);
            String sql = Dsl.MSB.insert(props).into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("name"));
            assertTrue(sql.contains("age"));
            assertTrue(sql.contains("#{name}"));
            assertTrue(sql.contains("#{age}"));
        }

        @Test
        void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("active", true), Filters.gt("age", 18));
            String sql = Dsl.MSB.renderCondition(cond, Account.class).build().query();
            assertTrue(sql.contains("active = #{active}"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("age > #{age}"));
        }

        @Test
        void testSelectEntityClass() {
            String sql = Dsl.MSB.select(Account.class).from("users").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertFalse(sql.contains("tempData")); // @Transient
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = Dsl.MSB.select(Account.class, excludes).from("users").build().query();
            assertTrue(sql.contains("firstName"));
            assertFalse(sql.contains("email"));
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.MSB.select(Account.class, true).from("users").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        void testSelectFromEntityClass() {
            String sql = Dsl.MSB.selectFrom(Account.class).where(Filters.eq("active", true)).build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM test_account"));
            assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        void testSelectFromEntityClassWithAlias() {
            String sql = Dsl.MSB.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).build().query();
            assertTrue(sql.contains("FROM test_account a"));
        }

        @Test
        void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = Dsl.MSB.selectFrom(Account.class, excludes).where(Filters.eq("active", true)).build().query();
            assertTrue(sql.contains("firstName"));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectMultipleColumns() {
            String sql = Dsl.MSB.select("firstName", "lastName", "email").from("users").where(Filters.eq("active", true)).build().query();
            assertTrue(sql.contains("SELECT firstName, lastName, email"));
            assertTrue(sql.contains("FROM users"));
            assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        void testSelectMultipleEntities() {
            String sql = Dsl.MSB.select(Account.class, "a", "account", Account.class, "b", "account2").from("test_account a, test_account b").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("account."));
            assertTrue(sql.contains("account2."));
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = Dsl.MSB.select(aliases).from("users").build().query();
            assertTrue(sql.contains("firstName AS \"fname\""));
            assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        void testSelectWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "price");
            String sql = Dsl.MSB.select(columns).from("products").build().query();
            assertTrue(sql.contains("SELECT id, name, price"));
            assertTrue(sql.contains("FROM products"));
        }

        @Test
        void testUpdateEntityClass() {
            String sql = Dsl.MSB.update(Account.class).where(Filters.eq("id", 123)).build().query();
            assertTrue(sql.contains("UPDATE test_account"));
            assertTrue(sql.contains("firstName = #{firstName}"));
            assertTrue(sql.contains("lastName = #{lastName}"));
            assertFalse(sql.contains("readOnlyField")); // @ReadOnly
            assertFalse(sql.contains("nonUpdatableField")); // @NonUpdatable
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate"));
            String sql = Dsl.MSB.update(Account.class, excludes).where(Filters.eq("id", 123)).build().query();
            assertTrue(sql.contains("UPDATE test_account"));
            assertTrue(sql.contains("firstName = #{firstName}"));
            assertFalse(sql.contains("createdDate"));
        }

        @Test
        void testUpdateTable() {
            String sql = Dsl.MSB.update("users").set(Arrays.asList("status", "lastModified")).where(Filters.eq("id", 123)).build().query();
            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("status = #{status}"));
            assertTrue(sql.contains("lastModified = #{lastModified}"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        void testUpdateTableWithEntityClass() {
            String sql = Dsl.MSB.update("user_archive", Account.class).set("firstName").where(Filters.eq("id", 123)).build().query();
            assertTrue(sql.contains("UPDATE user_archive"));
            assertTrue(sql.contains("firstName = #{firstName}"));
        }

        @Table("test_account")
        public static class Account {
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            private Date createdDate;
            @Transient
            private String tempData;
            @ReadOnly
            private String readOnlyField;
            @NonUpdatable
            private String nonUpdatableField;

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

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }
        }
    }

    @Nested
    class MSCTest {
        @Test
        void testBatchInsert() {
            List<User> users = new ArrayList<>();
            User user1 = new User();
            user1.setFirstName("John");
            user1.setLastName("Doe");
            User user2 = new User();
            user2.setFirstName("Jane");
            user2.setLastName("Smith");
            users.add(user1);
            users.add(user2);

            String sql = Dsl.MSC.batchInsert(users).into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("(first_name, last_name)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{firstName_0}"));
        }

        @Test
        void testCountEntityClass() {
            String sql = Dsl.MSC.selectCountFrom(User.class).where(Filters.gt("age", 18)).build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM test_users"));
            assertTrue(sql.contains("age > #{age}"));
        }

        @Test
        void testCountTable() {
            String sql = Dsl.MSC.selectCountFrom("users").build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM users"));
        }

        @Test
        void testDeleteFromEntityClass() {
            String sql = Dsl.MSC.deleteFrom(User.class).where(Filters.eq("id", 123)).build().query();
            assertTrue(sql.contains("DELETE FROM test_users"));
            assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        void testDeleteFromTable() {
            String sql = Dsl.MSC.deleteFrom("users").where(Filters.eq("userId", 123)).build().query();
            assertTrue(sql.contains("DELETE FROM users"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("user_id = #{userId}"));
        }

        @Test
        void testDeleteFromTableWithEntityClass() {
            String sql = Dsl.MSC.deleteFrom("users", User.class).where(Filters.eq("firstName", "John")).build().query();
            assertTrue(sql.contains("DELETE FROM users"));
            assertTrue(sql.contains("first_name = #{firstName}"));
        }

        @Test
        void testInsertIntoEntityClass() {
            String sql = Dsl.MSC.insertInto(User.class).build().query();
            assertTrue(sql.contains("INSERT INTO test_users"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
        }

        @Test
        void testInsertIntoEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("id"));
            String sql = Dsl.MSC.insertInto(User.class, exclude).build().query();
            assertTrue(sql.contains("INSERT INTO test_users"));
            assertFalse(sql.contains("id"));
        }

        @Test
        void testInsertMultipleColumns() {
            String sql = Dsl.MSC.insert("firstName", "lastName", "emailAddress").into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("(first_name, last_name, email_address)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
            assertTrue(sql.contains("#{emailAddress}"));
        }

        @Test
        void testInsertSingleColumn() {
            String sql = Dsl.MSC.insert("userName").into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("(user_name)"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains("#{userName}"));
        }

        @Test
        void testInsertWithCollection() {
            List<String> props = Arrays.asList("firstName", "lastName");
            String sql = Dsl.MSC.insert(props).into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("(first_name, last_name)"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            String sql = Dsl.MSC.insert(user).into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertFalse(sql.contains("temp_data")); // @Transient
            assertFalse(sql.contains("read_only_field")); // @ReadOnly
        }

        @Test
        void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmailAddress("john@example.com");

            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.insert(user, exclude).into("users").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertFalse(sql.contains("email_address"));
        }

        @Test
        void testInsertWithEntityClass() {
            String sql = Dsl.MSC.insert(User.class).into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email_address"));
            assertFalse(sql.contains("temp_data")); // @Transient
            assertFalse(sql.contains("read_only_field")); // @ReadOnly
        }

        @Test
        void testInsertWithEntityClassAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("id", "createdDate"));
            String sql = Dsl.MSC.insert(User.class, exclude).into("users").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertFalse(sql.contains("created_date"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("firstName", "John");
            data.put("lastName", "Doe");
            String sql = Dsl.MSC.insert(data).into("users").build().query();
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("#{firstName}"));
            assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18));
            String sql = Dsl.MSC.renderCondition(cond, User.class).build().query();
            assertTrue(sql.contains("first_name = #{firstName}"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("age > #{age}"));
        }

        @Test
        void testSelectEntityClass() {
            String sql = Dsl.MSC.select(User.class).from("users").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("AS \"firstName\""));
            assertTrue(sql.contains("AS \"lastName\""));
            assertFalse(sql.contains("temp_data")); // @Transient
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.select(User.class, exclude).from("users").build().query();
            assertTrue(sql.contains("AS \"firstName\""));
            assertFalse(sql.contains("emailAddress"));
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.MSC.select(User.class, true).from("users").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("AS \"firstName\""));
        }

        @Test
        void testSelectFromEntityClass() {
            String sql = Dsl.MSC.selectFrom(User.class).build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM test_users"));
        }

        @Test
        void testSelectFromEntityClassWithAlias() {
            String sql = Dsl.MSC.selectFrom(User.class, "u").where(Filters.eq("u.active", true)).build().query();
            assertTrue(sql.contains("FROM test_users u"));
            // Table-alias prefix is stripped from the named-parameter identifier so the
            // placeholder remains a valid iBATIS expression (#{active} not #{u.active}).
            assertTrue(sql.contains("u.active = #{active}"));
        }

        @Test
        void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.selectFrom(User.class, "u", exclude).build().query();
            assertTrue(sql.contains("FROM test_users u"));
            assertFalse(sql.contains("emailAddress"));
        }

        @Test
        void testSelectFromEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("password"));
            String sql = Dsl.MSC.selectFrom(User.class, exclude).build().query();
            assertTrue(sql.contains("AS \"firstName\""));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectFromEntityClassWithFullOptions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.selectFrom(User.class, "u", true, exclude).build().query();
            assertTrue(sql.contains("FROM test_users u"));
            assertFalse(sql.contains("emailAddress"));
        }

        @Test
        void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.selectFrom(User.class, true, exclude).build().query();
            assertTrue(sql.contains("FROM test_users"));
            assertFalse(sql.contains("emailAddress"));
        }

        @Test
        void testSelectFromMultipleEntities() {
            String sql = Dsl.MSC.selectFrom(User.class, "u", "user", User.class, "u2", "user2").build().query();
            assertTrue(sql.contains("FROM"));
            assertTrue(sql.contains("user."));
            assertTrue(sql.contains("user2."));
        }

        @Test
        void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> exclude1 = new HashSet<>(Arrays.asList("password"));
            Set<String> exclude2 = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = Dsl.MSC.selectFrom(User.class, "u", "user", exclude1, User.class, "u2", "user2", exclude2).build().query();
            assertTrue(sql.contains("FROM"));
            assertFalse(sql.contains("password"));
            assertTrue(sql.contains("emailAddress"));
        }

        @Test
        void testSelectFromWithMultiSelects() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));
            String sql = Dsl.MSC.selectFrom(selections).build().query();
            assertTrue(sql.contains("FROM"));
            assertTrue(sql.contains("user."));
            assertTrue(sql.contains("user2."));
        }

        @Test
        void testSelectMultipleColumns() {
            String sql = Dsl.MSC.select("firstName", "lastName", "emailAddress").from("users").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
            assertTrue(sql.contains("email_address AS \"emailAddress\""));
        }

        @Test
        void testSelectMultipleEntities() {
            String sql = Dsl.MSC.select(User.class, "u", "user", User.class, "u2", "user2").from("test_users u, test_users u2").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("user."));
            assertTrue(sql.contains("user2."));
        }

        @Test
        void testSelectMultipleEntitiesWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("password", "emailAddress"));
            String sql = Dsl.MSC.select(User.class, "u", "user", exclude, User.class, "u2", "user2", exclude)
                    .from("test_users u, test_users u2")
                    .build()
                    .query();
            assertTrue(sql.contains("user."));
            assertTrue(sql.contains("user2."));
            assertFalse(sql.contains("password"));
            assertFalse(sql.contains("emailAddress"));
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = Dsl.MSC.select(aliases).from("users").build().query();
            assertTrue(sql.contains("first_name AS \"fname\""));
            assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.MSC.select(columns).from("users").build().query();
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        void testSelectWithMultiSelects() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));
            String sql = Dsl.MSC.select(selections).from("test_users u, test_users u2").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("user."));
            assertTrue(sql.contains("user2."));
        }

        @Test
        void testUpdateEntityClass() {
            String sql = Dsl.MSC.update(User.class).where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE test_users"));
            assertTrue(sql.contains("first_name = #{firstName}"));
            assertTrue(sql.contains("last_name = #{lastName}"));
            assertFalse(sql.contains("read_only_field")); // @ReadOnly
            assertFalse(sql.contains("non_updatable_field")); // @NonUpdatable
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("createdDate"));
            String sql = Dsl.MSC.update(User.class, exclude).where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE test_users"));
            assertTrue(sql.contains("first_name = #{firstName}"));
            assertFalse(sql.contains("created_date"));
        }

        @Test
        void testUpdateTable() {
            String sql = Dsl.MSC.update("users").set(Arrays.asList("firstName", "lastName")).where(Filters.eq("userId", 123)).build().query();
            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("first_name = #{firstName}"));
            assertTrue(sql.contains("last_name = #{lastName}"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("user_id = #{userId}"));
        }

        @Test
        void testUpdateTableWithEntityClass() {
            String sql = Dsl.MSC.update("users", User.class).set(Arrays.asList("firstName", "lastName")).where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("first_name = #{firstName}"));
            assertTrue(sql.contains("last_name = #{lastName}"));
        }

        @Table("test_users")
        public static class User {
            private Long id;
            private String firstName;
            private String lastName;
            private String emailAddress;
            private Date createdDate;
            @Transient
            private String tempData;
            @ReadOnly
            private String readOnlyField;
            @NonUpdatable
            private String nonUpdatableField;

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

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }
        }
    }

    @Nested
    class NACTest {
        @Test
        void testAllJoinTypes() {
            String sql;

            sql = Dsl.NAC.select("*").from("ACCOUNT a").join("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            assertTrue(sql.contains("JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT a").innerJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            assertTrue(sql.contains("INNER JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT a").leftJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            assertTrue(sql.contains("LEFT JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT a").rightJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            assertTrue(sql.contains("RIGHT JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT a").fullJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            assertTrue(sql.contains("FULL JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT").crossJoin("DEPARTMENT").build().query();
            assertTrue(sql.contains("CROSS JOIN"));

            sql = Dsl.NAC.select("*").from("ACCOUNT").naturalJoin("ACCOUNT_PROFILE").build().query();
            assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        void testAppendCustomSQL() {
            String sql = Dsl.NAC.select("*").from("ACCOUNT").append(" WHERE CUSTOM_FUNCTION(NAME) = TRUE").build().query();

            assertTrue(sql.contains("CUSTOM_FUNCTION"));
        }

        @Test
        void testBatchInsert() {
            final Account john = new Account();
            john.setFirstName("John");
            john.setLastName("Doe");
            final Account jane = new Account();
            jane.setFirstName("Jane");
            jane.setLastName("Smith");
            List<Account> accounts = Arrays.asList(john, jane);

            String sql = Dsl.NAC.batchInsert(accounts).into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = Dsl.NAC.batchInsert(data).into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testColumnNameTransformation() {
            // Test that camelCase property names are converted to SCREAMING_SNAKE_CASE
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");

            String sql = Dsl.NAC.insert(account).into("ACCOUNT").build().query();

            // Column names should be UPPER_CASE
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));

            // Parameter names should remain camelCase
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
        }

        @Test
        void testComplexNestedConditions() {
            String sql = Dsl.NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.or(Filters.and(Filters.eq("STATUS", "ACTIVE"), Filters.or(Filters.lt("AGE", 18), Filters.gt("AGE", 65))),
                            Filters.and(Filters.eq("STATUS", "PREMIUM"), Filters.between("AGE", 18, 65))))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("OR"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains(":STATUS"));
            assertTrue(sql.contains(":AGE"));
        }

        @Test
        void testComplexQueryWithJoins() {
            String sql = Dsl.NAC.select("a.firstName", "a.lastName", "COUNT(o.id)")
                    .from("ACCOUNT a")
                    .leftJoin("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .where(Filters.eq("a.active", true))
                    .groupBy("a.ID", "a.firstName", "a.lastName")
                    .having(Filters.gt("COUNT(o.ID)", 5))
                    .orderBy("COUNT(o.ID) DESC")
                    .limit(10)
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("a.FIRST_NAME AS \"a.firstName\""));
            assertTrue(sql.contains("a.LAST_NAME AS \"a.lastName\""));
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("GROUP BY"));
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testCount() {
            String sql = Dsl.NAC.selectCountFrom("ACCOUNT").where(Filters.eq("STATUS", "ACTIVE")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertTrue(sql.contains("STATUS = :STATUS"));
        }

        @Test
        void testCountEntityClass() {
            String sql = Dsl.NAC.selectCountFrom(Account.class).where(Filters.eq("status", "ACTIVE")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertTrue(sql.contains("STATUS = :status"));
        }

        @Test
        void testDeleteFrom() {
            String sql = Dsl.NAC.deleteFrom("ACCOUNT").where(Filters.eq("STATUS", "INACTIVE")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("STATUS = :STATUS"));
        }

        @Test
        void testDeleteFromEntityClass() {
            String sql = Dsl.NAC.deleteFrom(Account.class).where(Filters.eq("firstName", "John")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = :firstName"));
        }

        @Test
        void testDeleteFromWithEntityClass() {
            String sql = Dsl.NAC.deleteFrom("ACCOUNT", Account.class).where(Filters.eq("firstName", "John")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = :firstName"));
        }

        @Test
        void testForUpdate() {
            String sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.eq("ID", 1)).forUpdate().build().query();

            assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        void testGroupByHaving() {
            String sql = Dsl.NAC.select("DEPARTMENT", "COUNT(*) AS CNT")
                    .from("EMPLOYEE")
                    .groupBy("DEPARTMENT")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build()
                    .query();

            assertTrue(sql.contains("GROUP BY DEPARTMENT"));
            assertTrue(sql.contains("HAVING"));
        }

        @Test
        void testInsertInto() {
            String sql = Dsl.NAC.insertInto(Account.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = Dsl.NAC.insertInto(Account.class, excluded).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertFalse(sql.contains("ID"));
        }

        @Test
        void testInsertMultipleColumns() {
            String sql = Dsl.NAC.insert("firstName", "lastName", "email").into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL"));
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
            assertTrue(sql.contains(":email"));
        }

        @Test
        void testInsertSingleExpression() {
            String sql = Dsl.NAC.insert("FIRST_NAME").into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("(FIRST_NAME)"));
            assertTrue(sql.contains("VALUES (:FIRST_NAME)"));
        }

        @Test
        void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.NAC.insert(columns).into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
        }

        @Test
        void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedTime(new Date(1_700_000_000_000L)); // Should be excluded
            account.setSessionId("123"); // Should be excluded

            String sql = Dsl.NAC.insert(account).into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertFalse(sql.contains("CREATED_TIME"));
            assertFalse(sql.contains("SESSION_ID"));
        }

        @Test
        void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setFirstName("John");
            account.setPassword("secret");

            Set<String> exclude = Set.of("password");
            String sql = Dsl.NAC.insert(account, exclude).into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FIRST_NAME"));
            assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        void testInsertWithEntityClass() {
            String sql = Dsl.NAC.insert(Account.class).into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL"));
            assertTrue(sql.contains("PASSWORD"));
            assertFalse(sql.contains("CREATED_TIME"));
            assertFalse(sql.contains("SESSION_ID"));
        }

        @Test
        void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "createdTime");
            String sql = Dsl.NAC.insert(Account.class, excluded).into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> props = Map.of("firstName", "John", "lastName", "Doe");
            String sql = Dsl.NAC.insert(props).into("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
        }

        @Test
        void testInsertWithUpperCaseColumns() {
            // Test that already uppercase columns remain uppercase
            String sql = Dsl.NAC.insert("FIRST_NAME", "LAST_NAME").into("ACCOUNT").build().query();

            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains(":FIRST_NAME"));
            assertTrue(sql.contains(":LAST_NAME"));
        }

        @Test
        void testJoinWithConditions() {
            String sql = Dsl.NAC.select("*")
                    .from("ACCOUNT a")
                    .leftJoin("ORDER o")
                    .on(Filters.and(Filters.eq("a.ID", "o.ACCOUNT_ID"), Filters.gt("o.AMOUNT", 100)))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testLimitOffset() {
            String sql = Dsl.NAC.select("*").from("ACCOUNT").limit(10).offset(20).build().query();

            assertTrue(sql.contains("LIMIT 10"));
            assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = Dsl.NAC.select("*").from("ACCOUNT").limit(10, 20).build().query();

            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testMultipleWhereConditions() {
            String sql = Dsl.NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%")))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testNamedParametersAreCorrect() {
            String sql = Dsl.NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.in("status", Arrays.asList("ACTIVE", "PREMIUM")),
                            Filters.between("age", 18, 65)))
                    .build()
                    .query();

            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":status"));
            assertTrue(sql.contains("AGE BETWEEN :minAge AND :maxAge"));
        }

        @Test
        void testNamingPolicyUpperCase() {
            // NAC uses SCREAMING_SNAKE_CASE naming policy
            String sql = Dsl.NAC.select("firstName", "lastName").from("userAccounts").build().query();

            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("userAccounts"));
        }

        @Test
        void testNullConditionThrows() {
            assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NAC.renderCondition(null, Account.class);
            });
        }

        @Test
        void testOrderBy() {
            String sql = Dsl.NAC.select("*").from("ACCOUNT").orderBy("LAST_NAME ASC", "FIRST_NAME ASC").build().query();

            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("LAST_NAME ASC"));
            assertTrue(sql.contains("FIRST_NAME ASC"));
        }

        @Test
        void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = Dsl.NAC.renderCondition(cond, Account.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("STATUS = :status"));
            assertTrue(sql.contains("BALANCE > :balance"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testSelectEntityClass() {
            String sql = Dsl.NAC.select(Account.class).from("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL"));
            assertTrue(sql.contains("CREATED_TIME AS \"createdTime\""));
            assertTrue(sql.contains("PASSWORD"));
            assertFalse(sql.contains("SESSION_ID"));
        }

        @Test
        void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.select(Account.class, true, excluded).from("ACCOUNT").build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.select(Account.class, excluded).from("ACCOUNT").build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("PASSWORD"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.NAC.select(Account.class, true).from("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSelectFrom() {
            String sql = Dsl.NAC.selectFrom(Account.class).where(Filters.eq("active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM ACCOUNT"));
            assertTrue(sql.contains("ACTIVE = :active"));
        }

        @Test
        void testSelectFromMultipleEntities() {
            String sql = Dsl.NAC.selectFrom(Account.class, "a", "account", Account.class, "o", "order")
                    .where(Filters.eq("a.ID", "o.ACCOUNT_ID"))
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = Dsl.NAC.selectFrom(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .where(Filters.eq("a.ID", "o.ACCOUNT_ID"))
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithAlias() {
            String sql = Dsl.NAC.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM ACCOUNT a"));
        }

        @Test
        void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.selectFrom(Account.class, "a", excluded).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("ACCOUNT a"));
            assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        void testSelectFromWithAliasAndSubEntities() {
            String sql = Dsl.NAC.selectFrom(Account.class, "a", true).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.selectFrom(Account.class, "a", true, excluded).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.selectFrom(Account.class, excluded).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "o", "order", null, true, null));
            String sql = Dsl.NAC.selectFrom(selections).where(Filters.eq("a.ID", "o.ACCOUNT_ID")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithSubEntities() {
            String sql = Dsl.NAC.selectFrom(Account.class, true).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NAC.selectFrom(Account.class, true, excluded).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        void testSelectMultipleColumns() {
            String sql = Dsl.NAC.select("firstName", "lastName", "email").from("ACCOUNT").where(Filters.eq("active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL"));
            assertTrue(sql.contains("ACTIVE = :active"));
        }

        @Test
        void testSelectMultipleEntities() {
            String sql = Dsl.NAC.select(Account.class, "a", "account", Account.class, "o", "order")
                    .from("ACCOUNT a")
                    .join("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("a.ID AS \"account.id\""));
            assertTrue(sql.contains("o.ID AS \"order.id\""));
        }

        @Test
        void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = Dsl.NAC.select(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .from("ACCOUNT a")
                    .join("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSelectSinglePart() {
            String sql = Dsl.NAC.select("COUNT(*)").from("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM ACCOUNT"));
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = Map.of("firstName", "fname", "lastName", "lname");
            String sql = Dsl.NAC.select(aliases).from("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            assertTrue(sql.contains("LAST_NAME AS \"lname\""));
        }

        @Test
        void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.NAC.select(columns).from("ACCOUNT").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
        }

        @Test
        void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "o", "order", null, true, null));
            String sql = Dsl.NAC.select(selections).from("ACCOUNT a").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSpecialConditions() {
            // Test IS NULL
            String sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.isNull("EMAIL")).build().query();
            assertTrue(sql.contains("EMAIL IS NULL"));

            // Test IS NOT NULL
            sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.isNotNull("EMAIL")).build().query();
            assertTrue(sql.contains("EMAIL IS NOT NULL"));

            // Test IN
            sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.in("STATUS", Arrays.asList("ACTIVE", "PREMIUM"))).build().query();
            assertTrue(sql.contains("STATUS IN"));

            // Test BETWEEN
            sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.between("AGE", 18, 65)).build().query();
            assertTrue(sql.contains("AGE BETWEEN"));

            // Test LIKE
            sql = Dsl.NAC.select("*").from("ACCOUNT").where(Filters.like("NAME", "%JOHN%")).build().query();
            assertTrue(sql.contains("NAME LIKE"));
        }

        @Test
        void testUpdate() {
            String sql = Dsl.NAC.update("ACCOUNT").set("STATUS", "ACTIVE").where(Filters.eq("ID", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE ACCOUNT"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("STATUS = :STATUS"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("ID = :ID"));
        }

        @Test
        void testUpdateEntityClass() {
            String sql = Dsl.NAC.update(Account.class).set(Arrays.asList("firstName", "lastName")).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdTime", "password");
            String sql = Dsl.NAC.update(Account.class, excluded).set("firstName").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        void testUpdateWithEntityClass() {
            String sql = Dsl.NAC.update("ACCOUNT", Account.class).set("status").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE ACCOUNT"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("STATUS"));
        }

        @Test
        void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = Dsl.NAC.update("ACCOUNT").set(updates).where(Filters.eq("ID", 1)).build().query();

            assertNotNull(sql);
            assertTrue(sql.contains("FIRST_NAME = :firstName"));
            assertTrue(sql.contains("LAST_NAME = :lastName"));
            assertTrue(sql.contains("ID = :ID"));
        }

        @Table("ACCOUNT")
        public static class Account {
            private long id;
            @Column("FIRST_NAME")
            private String firstName;
            @Column("LAST_NAME")
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdTime;
            @NonUpdatable
            private String password;
            @Transient
            private String sessionId;

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

            public Date getCreatedTime() {
                return createdTime;
            }

            public void setCreatedTime(Date createdTime) {
                this.createdTime = createdTime;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getSessionId() {
                return sessionId;
            }

            public void setSessionId(String sessionId) {
                this.sessionId = sessionId;
            }
        }
    }

    @Nested
    class NLCTest {
        @Test
        void testAggregateFunctions() {
            String sql = Dsl.NLC
                    .select("COUNT(*) as total", "AVG(salary) as avgSalary", "MAX(salary) as maxSalary", "MIN(salary) as minSalary",
                            "SUM(salary) as totalSalary")
                    .from("employee")
                    .build()
                    .query();

            assertTrue(sql.contains("COUNT(*)"));
            assertTrue(sql.contains("AVG(salary)"));
            assertTrue(sql.contains("MAX(salary)"));
            assertTrue(sql.contains("MIN(salary)"));
            assertTrue(sql.contains("SUM(salary)"));
        }

        @Test
        void testAllJoinTypes() {
            String sql;

            sql = Dsl.NLC.select("*").from("account a").join("order o").on("a.id = o.accountId").build().query();
            assertTrue(sql.contains("JOIN"));

            sql = Dsl.NLC.select("*").from("account a").innerJoin("order o").on("a.id = o.accountId").build().query();
            assertTrue(sql.contains("INNER JOIN"));

            sql = Dsl.NLC.select("*").from("account a").leftJoin("order o").on("a.id = o.accountId").build().query();
            assertTrue(sql.contains("LEFT JOIN"));

            sql = Dsl.NLC.select("*").from("account a").rightJoin("order o").on("a.id = o.accountId").build().query();
            assertTrue(sql.contains("RIGHT JOIN"));

            sql = Dsl.NLC.select("*").from("account a").fullJoin("order o").on("a.id = o.accountId").build().query();
            assertTrue(sql.contains("FULL JOIN"));

            sql = Dsl.NLC.select("*").from("account").crossJoin("department").build().query();
            assertTrue(sql.contains("CROSS JOIN"));

            sql = Dsl.NLC.select("*").from("account").naturalJoin("accountProfile").build().query();
            assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        void testAppendCustomSQL() {
            String sql = Dsl.NLC.select("*").from("account").append(" WHERE customFunction(name) = true").build().query();

            assertTrue(sql.contains("customFunction"));
        }

        @Test
        void testBatchInsert() {
            final Account john = new Account();
            john.setFirstName("John");
            john.setLastName("Doe");
            final Account jane = new Account();
            jane.setFirstName("Jane");
            jane.setLastName("Smith");
            List<Account> accounts = Arrays.asList(john, jane);

            String sql = Dsl.NLC.batchInsert(accounts).into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = Dsl.NLC.batchInsert(data).into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testCaseExpression() {
            String sql = Dsl.NLC.select("name", "CASE WHEN age < 18 THEN 'Minor' WHEN age < 65 THEN 'Adult' ELSE 'Senior' END as category")
                    .from("users")
                    .build()
                    .query();

            assertTrue(sql.contains("case when age < 18 then 'Minor' when age < 65 then 'Adult' else 'Senior' end AS category"));
        }

        @Test
        void testColumnNamePreservation() {
            // Test that camelCase property names are preserved
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");

            String sql = Dsl.NLC.insert(account).into("account").build().query();

            // Column names should remain camelCase
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));

            // Parameter names should also be camelCase
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
        }

        @Test
        void testComplexMultiTableQuery() {
            String sql = Dsl.NLC.select("u.name as userName", "d.name as departmentName", "COUNT(o.id) as orderCount", "SUM(o.total) as totalRevenue")
                    .from("users u")
                    .join("departments d")
                    .on("u.departmentId = d.id")
                    .leftJoin("orders o")
                    .on("u.id = o.userId")
                    .where(Filters.and(Filters.eq("u.active", true), Filters.between("o.createdDate", "2023-01-01", "2023-12-31")))
                    .groupBy("u.id", "u.name", "d.id", "d.name")
                    .having(Filters.gt("COUNT(o.id)", 0))
                    .orderBy("totalRevenue DESC")
                    .limit(20)
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("JOIN"));
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("GROUP BY"));
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testComplexNestedConditions() {
            String sql = Dsl.NLC.select("*")
                    .from("account")
                    .where(Filters.or(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.lt("age", 18), Filters.gt("age", 65))),
                            Filters.and(Filters.eq("status", "premium"), Filters.between("age", 18, 65))))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("OR"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains(":status"));
            assertTrue(sql.contains(":age"));
        }

        @Test
        void testComplexQueryWithJoins() {
            String sql = Dsl.NLC.select("a.firstName", "a.lastName", "COUNT(o.id)")
                    .from("account a")
                    .leftJoin("order o")
                    .on("a.id = o.accountId")
                    .where(Filters.eq("a.active", true))
                    .groupBy("a.id", "a.firstName", "a.lastName")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("COUNT(o.id) DESC")
                    .limit(10)
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("a.firstName"));
            assertTrue(sql.contains("a.lastName"));
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("GROUP BY"));
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testCount() {
            String sql = Dsl.NLC.selectCountFrom("account").where(Filters.eq("status", "active")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("status = :status"));
        }

        @Test
        void testCountEntityClass() {
            String sql = Dsl.NLC.selectCountFrom(Account.class).where(Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000))).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("status = :status"));
            assertTrue(sql.contains("balance > :balance"));
        }

        @Test
        void testDeleteFrom() {
            String sql = Dsl.NLC.deleteFrom("account").where(Filters.eq("status", "inactive")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("status = :status"));
        }

        @Test
        void testDeleteFromEntityClass() {
            String sql = Dsl.NLC.deleteFrom(Account.class).where(Filters.eq("status", "inactive")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("status = :status"));
        }

        @Test
        void testDeleteFromWithEntityClass() {
            String sql = Dsl.NLC.deleteFrom("account", Account.class).where(Filters.eq("status", "inactive")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("status = :status"));
        }

        @Test
        void testDistinct() {
            String sql = Dsl.NLC.select("DISTINCT department").from("employee").build().query();

            assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        void testEntityClassPropertyMapping() {
            // Ensure properties are correctly mapped
            Account account = new Account();
            account.setFirstName("Test");
            account.setLastName("User");
            account.setEmail("test@example.com");

            String sql = Dsl.NLC.insert(account).into("account").build().query();

            // Should use property names as both column and parameter names
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
            assertTrue(sql.contains(":email"));

            Date startDate = new Date(1_700_000_000_000L);
            Date endDate = new Date(startDate.getTime() + 86400000); // 1 day later

            sql = Dsl.NLC.selectFrom(Order.class, "ord", true).where(Filters.between("ord.orderDate", startDate, endDate)).build().query();
            assertEquals("SELECT ord.id AS \"id\", ord.userId AS \"userId\", ord.orderNumber AS \"orderNumber\", "
                    + "ord.amount AS \"amount\", ord.orderDate AS \"orderDate\" FROM user_order ord "
                    + "WHERE ord.orderDate BETWEEN :minOrderDate AND :maxOrderDate", sql);
        }

        @Test
        void testForUpdate() {
            String sql = Dsl.NLC.select("*").from("account").where(Filters.eq("id", 1)).forUpdate().build().query();

            assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        void testGroupByHaving() {
            String sql = Dsl.NLC.select("department", "COUNT(*) as cnt")
                    .from("employee")
                    .groupBy("department")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build()
                    .query();

            assertTrue(sql.contains("GROUP BY department"));
            assertTrue(sql.contains("HAVING"));
        }

        @Test
        void testInsertInto() {
            String sql = Dsl.NLC.insertInto(Account.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = Dsl.NLC.insertInto(Account.class, excluded).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO account"));
            assertFalse(sql.contains("id"));
        }

        @Test
        void testInsertMultipleColumns() {
            String sql = Dsl.NLC.insert("firstName", "lastName", "email").into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
            assertTrue(sql.contains(":email"));
        }

        @Test
        void testInsertSingleExpression() {
            String sql = Dsl.NLC.insert("firstName").into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("(firstName)"));
            assertTrue(sql.contains("VALUES (:firstName)"));
        }

        @Test
        void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.NLC.insert(columns).into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
        }

        @Test
        void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedTime(new Date(1_700_000_000_000L)); // Should be excluded
            account.setSessionToken("123"); // Should be excluded

            String sql = Dsl.NLC.insert(account).into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertFalse(sql.contains("createdTime"));
            assertFalse(sql.contains("sessionToken"));
        }

        @Test
        void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setFirstName("John");
            account.setPassword("secret");

            Set<String> exclude = Set.of("password");
            String sql = Dsl.NLC.insert(account, exclude).into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("firstName"));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testInsertWithEntityClass() {
            String sql = Dsl.NLC.insert(Account.class).into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains("password"));
            assertFalse(sql.contains("createdTime"));
            assertFalse(sql.contains("sessionToken"));
        }

        @Test
        void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "createdTime");
            String sql = Dsl.NLC.insert(Account.class, excluded).into("account").build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> props = Map.of("firstName", "John", "lastName", "Doe");
            String sql = Dsl.NLC.insert(props).into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
        }

        @Test
        void testJoinWithConditions() {
            String sql = Dsl.NLC.select("*")
                    .from("account a")
                    .leftJoin("order o")
                    .on(Filters.and(Filters.eq("a.id", "o.accountId"), Filters.gt("o.amount", 100)))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testLimitOffset() {
            String sql = Dsl.NLC.select("*").from("account").limit(10).offset(20).build().query();

            assertTrue(sql.contains("LIMIT 10"));
            assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = Dsl.NLC.select("*").from("account").limit(10, 20).build().query();

            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testMultipleWhereConditions() {
            String sql = Dsl.NLC.select("*")
                    .from("account")
                    .where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%")))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testNamedParametersAreCorrect() {
            String sql = Dsl.NLC.select("*")
                    .from("account")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.in("status", Arrays.asList("active", "premium")),
                            Filters.between("age", 18, 65)))
                    .build()
                    .query();

            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains("status IN (:status1, :status2)"));
            assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
        }

        @Test
        void testNamingPolicyCamelCase() {
            // NLC uses CAMEL_CASE naming policy
            String sql = Dsl.NLC.select("firstName", "lastName").from("userAccounts").build().query();

            // Should preserve camelCase
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("userAccounts"));
        }

        @Test
        void testNullConditionThrows() {
            assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NLC.renderCondition(null, Account.class);
            });
        }

        @Test
        void testOrderBy() {
            String sql = Dsl.NLC.select("*").from("account").orderBy("lastName ASC", "firstName ASC").build().query();

            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("lastName ASC"));
            assertTrue(sql.contains("firstName ASC"));
        }

        @Test
        void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000));
            String sql = Dsl.NLC.renderCondition(cond, Account.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("status = :status"));
            assertTrue(sql.contains("balance > :balance"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testSelectEntityClass() {
            String sql = Dsl.NLC.select(Account.class).from("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains("createdTime"));
            assertTrue(sql.contains("password"));
            assertFalse(sql.contains("sessionToken"));
        }

        @Test
        void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.select(Account.class, true, excluded).from("account").build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password", "createdTime");
            String sql = Dsl.NLC.select(Account.class, excluded).from("account").build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("password"));
            assertFalse(sql.contains("createdTime"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.NLC.select(Account.class, true).from("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSelectFrom() {
            String sql = Dsl.NLC.selectFrom(Account.class).where(Filters.eq("active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("active = :active"));
        }

        @Test
        void testSelectFromMultipleEntities() {
            String sql = Dsl.NLC.selectFrom(Account.class, "a", "account", Account.class, "o", "order")
                    .where(Filters.eq("a.id", "o.accountId"))
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = Dsl.NLC.selectFrom(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .where(Filters.eq("a.id", "o.accountId"))
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithAlias() {
            String sql = Dsl.NLC.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM account a"));
        }

        @Test
        void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.selectFrom(Account.class, "a", excluded).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("account a"));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectFromWithAliasAndSubEntities() {
            String sql = Dsl.NLC.selectFrom(Account.class, "a", true).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.selectFrom(Account.class, "a", true, excluded).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.selectFrom(Account.class, excluded).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "o", "order", null, true, null));
            String sql = Dsl.NLC.selectFrom(selections).where(Filters.eq("a.id", "o.accountId")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithSubEntities() {
            String sql = Dsl.NLC.selectFrom(Account.class, true).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = Dsl.NLC.selectFrom(Account.class, true, excluded).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectMultipleColumns() {
            String sql = Dsl.NLC.select("firstName", "lastName", "email").from("account").where(Filters.eq("active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains("active = :active"));
        }

        @Test
        void testSelectMultipleEntities() {
            String sql = Dsl.NLC.select(Account.class, "a", "account", Account.class, "o", "order")
                    .from("account a")
                    .join("order o")
                    .on("a.id = o.accountId")
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("a.id AS \"account.id\""));
            assertTrue(sql.contains("o.id AS \"order.id\""));
        }

        @Test
        void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = Dsl.NLC.select(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .from("account a")
                    .join("order o")
                    .on("a.id = o.accountId")
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSelectSinglePart() {
            String sql = Dsl.NLC.select("COUNT(*)").from("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = Map.of("firstName", "fname", "lastName", "lname");
            String sql = Dsl.NLC.select(aliases).from("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("firstName AS \"fname\""));
            assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = Dsl.NLC.select(columns).from("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
        }

        @Test
        void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "o", "order", null, true, null));
            String sql = Dsl.NLC.select(selections).from("account a").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSpecialConditions() {
            // Test IS NULL
            String sql = Dsl.NLC.select("*").from("account").where(Filters.isNull("email")).build().query();
            assertTrue(sql.contains("email IS NULL"));

            // Test IS NOT NULL
            sql = Dsl.NLC.select("*").from("account").where(Filters.isNotNull("email")).build().query();
            assertTrue(sql.contains("email IS NOT NULL"));

            // Test IN
            sql = Dsl.NLC.select("*").from("account").where(Filters.in("status", Arrays.asList("active", "premium"))).build().query();
            assertTrue(sql.contains("status IN"));

            // Test BETWEEN
            sql = Dsl.NLC.select("*").from("account").where(Filters.between("age", 18, 65)).build().query();
            assertTrue(sql.contains("age BETWEEN"));

            // Test LIKE
            sql = Dsl.NLC.select("*").from("account").where(Filters.like("name", "%John%")).build().query();
            assertTrue(sql.contains("name LIKE"));
        }

        @Test
        void testUnionAll() {
            // Test building queries that could be used in UNION ALL
            String sql1 = Dsl.NLC.select("name", "email").from("users").build().query();
            String sql2 = Dsl.NLC.select("name", "email").from("customers").build().query();

            // Manual UNION ALL construction
            String unionAllSql = sql1 + " UNION ALL " + sql2;
            assertTrue(unionAllSql.contains("UNION ALL"));
        }

        @Test
        void testUpdate() {
            String sql = Dsl.NLC.update("account").set("status", "active").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("status = :status"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("id = :id"));
        }

        @Test
        void testUpdateEntityClass() {
            String sql = Dsl.NLC.update(Account.class).set(Arrays.asList("firstName", "lastName")).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdTime", "password");
            String sql = Dsl.NLC.update(Account.class, excluded).set("firstName").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        void testUpdateMultipleColumns() {
            String sql = Dsl.NLC.update("users")
                    .set("firstName", "John")
                    .set("lastName", "Doe")
                    .set(Map.of("age", 30))
                    .set("email", "john@example.com")
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("age"));
            assertTrue(sql.contains("email"));
        }

        @Test
        void testUpdateWithEntityClass() {
            String sql = Dsl.NLC.update("account", Account.class).set("status").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("status"));
        }

        @Test
        void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = Dsl.NLC.update("account").set(updates).where(Filters.eq("id", 1)).build().query();

            assertNotNull(sql);
            assertTrue(sql.contains("firstName = :firstName"));
            assertTrue(sql.contains("lastName = :lastName"));
            assertTrue(sql.contains("id = :id"));
        }

        @Test
        void testWindowFunctions() {
            String sql = Dsl.NLC.select("name", "salary", "ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) as rank")
                    .from("employee")
                    .build()
                    .query();

            assertTrue(sql.contains("OVER (PARTITION BY department ORDER BY salary DESC) AS rank"));
        }

        @Table("account")
        public static class Account {
            private long id;
            private String firstName;
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdTime;
            @NonUpdatable
            private String password;
            @Transient
            private String sessionToken;

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

            public Date getCreatedTime() {
                return createdTime;
            }

            public void setCreatedTime(Date createdTime) {
                this.createdTime = createdTime;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getSessionToken() {
                return sessionToken;
            }

            public void setSessionToken(String sessionToken) {
                this.sessionToken = sessionToken;
            }
        }
    }

    @Nested
    class NSBTest {
        @Test
        void testAggregateFunctions() {
            String sql = Dsl.NSB
                    .select("COUNT(*) as total", "AVG(salary) as avg_salary", "MAX(salary) as max_salary", "MIN(salary) as min_salary",
                            "SUM(salary) as total_salary")
                    .from("employees")
                    .build()
                    .query();

            assertTrue(sql.contains("COUNT(*)"));
            assertTrue(sql.contains("AVG(salary)"));
            assertTrue(sql.contains("MAX(salary)"));
            assertTrue(sql.contains("MIN(salary)"));
            assertTrue(sql.contains("SUM(salary)"));
        }

        @Test
        void testAppend() {
            String sql = Dsl.NSB.select("*").from("users").append(" WHERE custom_condition = true").build().query();

            assertTrue(sql.contains("custom_condition"));

            // Test multiple appends
            sql = Dsl.NSB.select("*").from("users").append(" WHERE 1=1").append(" AND status = 'active'").append(" ORDER BY name").build().query();

            assertTrue(sql.contains("1=1"));
            assertTrue(sql.contains("status = 'active'"));
            assertTrue(sql.contains("ORDER BY name"));
        }

        @Test
        void testBatchInsert() {
            List<User> users = new ArrayList<>();
            User user1 = new User();
            user1.setFirstName("John");
            user1.setLastName("Doe");

            User user2 = new User();
            user2.setFirstName("Jane");
            user2.setLastName("Smith");

            users.add(user1);
            users.add(user2);

            String sql = Dsl.NSB.batchInsert(users).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("VALUES"));
            // Should have multiple value sets
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
        }

        @Test
        void testCaseExpression() {
            String sql = Dsl.NSB.select("name", "CASE WHEN age < 18 THEN 'Minor' WHEN age < 65 THEN 'Adult' ELSE 'Senior' END as category")
                    .from("users")
                    .build()
                    .query();

            assertTrue(sql.contains("CASE"));
            assertTrue(sql.contains("WHEN"));
            assertTrue(sql.contains("THEN"));
            assertTrue(sql.contains("ELSE"));
        }

        @Test
        void testComplexConditions() {
            // Test nested AND/OR
            String sql = Dsl.NSB.select("*")
                    .from("users")
                    .where(Filters.or(Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.lt("age", 65)),
                            Filters.and(Filters.eq("status", "premium"), Filters.isNotNull("subscription_id"))))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("OR"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testComplexMultiTableQuery() {
            String sql = Dsl.NSB.select("u.name as userName", "d.name as departmentName", "COUNT(o.id) as orderCount", "SUM(o.total) as totalRevenue")
                    .from("users u")
                    .join("departments d")
                    .on("u.department_id = d.id")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .where(Filters.and(Filters.eq("u.active", true), Filters.between("o.created_date", "2023-01-01", "2023-12-31")))
                    .groupBy("u.id", "u.name", "d.id", "d.name")
                    .having(Filters.gt("COUNT(o.id)", 0))
                    .orderBy("totalRevenue DESC")
                    .limit(20)
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("JOIN"));
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("GROUP BY"));
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testCount() {
            String sql = Dsl.NSB.selectCountFrom("users").where(Filters.eq("active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM users"));
            assertTrue(sql.contains("WHERE"));
        }

        @Test
        void testCountEntityClass() {
            String sql = Dsl.NSB.selectCountFrom(User.class).where(Filters.eq("status", "active")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM test_user"));
            assertTrue(sql.contains("WHERE"));
        }

        @Test
        void testDeleteFrom() {
            String sql = Dsl.NSB.deleteFrom("users").where(Filters.eq("status", "inactive")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM users"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("status"));
            assertTrue(sql.contains(":status"));
        }

        @Test
        void testDeleteFromEntityClass() {
            String sql = Dsl.NSB.deleteFrom(User.class).where(Filters.eq("id", 123)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM test_user"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("id"));
        }

        @Test
        void testDeleteFromWithEntityClass() {
            String sql = Dsl.NSB.deleteFrom("user_accounts", User.class).where(Filters.lt("lastLogin", new Date(1_700_000_000_000L))).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM user_accounts"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("lastLogin"));
        }

        @Test
        void testDistinct() {
            String sql = Dsl.NSB.select("DISTINCT department").from("users").build().query();

            assertTrue(sql.contains("DISTINCT"));

            // Test with multiple columns
            sql = Dsl.NSB.select("DISTINCT department", "location").from("users").build().query();

            assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        void testEmptyCollectionThrows() {
            assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.insert(new ArrayList<>()).into("users").build().query();
            });

            assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.select(new ArrayList<Selection>()).from("users").build().query();
            });
        }

        @Test
        void testEntityClassPropertyMapping() {
            // Ensure properties are correctly mapped
            User user = new User();
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmail("test@example.com");

            String sql = Dsl.NSB.insert(user).into("test_user").build().query();

            // Should use property names as parameter names
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
            assertTrue(sql.contains(":email"));
        }

        @Test
        void testForUpdate() {
            String sql = Dsl.NSB.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

            assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        void testGroupByHaving() {
            String sql = Dsl.NSB.select("department", "COUNT(*) as count")
                    .from("users")
                    .groupBy("department")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build()
                    .query();

            assertTrue(sql.contains("GROUP BY department"));
            assertTrue(sql.contains("HAVING"));

            // Test multiple group by
            sql = Dsl.NSB.select("department", "location", "COUNT(*)").from("users").groupBy("department", "location").build().query();

            assertTrue(sql.contains("GROUP BY"));
            assertTrue(sql.contains("department"));
            assertTrue(sql.contains("location"));
        }

        @Test
        void testInsertInto() {
            String sql = Dsl.NSB.insertInto(User.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO test_user"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testInsertIntoWithExclusions() {
            Set<String> exclude = N.asSet("id");
            String sql = Dsl.NSB.insertInto(User.class, exclude).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO test_user"));
            assertFalse(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        void testInsertWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "created_date");
            String sql = Dsl.NSB.insert(columns).into("products").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO products"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("name"));
            assertTrue(sql.contains("created_date"));
        }

        @Test
        void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setEmail("john@example.com");
            user.setPassword("secret");
            user.setCreatedDate(new Date(1_700_000_000_000L));
            user.setTempData("temp");

            String sql = Dsl.NSB.insert(user).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            // Should include regular fields
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains("password"));
            // Should NOT include @ReadOnly or @Transient fields
            assertFalse(sql.contains("createdDate"));
            assertFalse(sql.contains("tempData"));
        }

        @Test
        void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setEmail("john@example.com");
            user.setPassword("secret");

            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.insert(user, exclude).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testInsertWithEntityClass() {
            String sql = Dsl.NSB.insert(User.class).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            // Should include insertable fields
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains("password"));
            // Should NOT include @ReadOnly or @Transient fields
            assertFalse(sql.contains("createdDate"));
            assertFalse(sql.contains("tempData"));
        }

        @Test
        void testInsertWithEntityClassAndExclusions() {
            Set<String> exclude = N.asSet("id", "password");
            String sql = Dsl.NSB.insert(User.class, exclude).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertFalse(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("username", "john_doe");
            data.put("age", 25);
            String sql = Dsl.NSB.insert(data).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("username"));
            assertTrue(sql.contains("age"));
            assertTrue(sql.contains(":username"));
            assertTrue(sql.contains(":age"));
        }

        @Test
        void testInsertWithMultipleColumns() {
            String sql = Dsl.NSB.insert("first_name", "last_name", "email").into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains(":first_name"));
            assertTrue(sql.contains(":last_name"));
            assertTrue(sql.contains(":email"));
        }

        @Test
        void testInsertWithSingleExpression() {
            String sql = Dsl.NSB.insert("user_name").into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("user_name"));
            assertTrue(sql.contains("VALUES"));
            assertTrue(sql.contains(":user_name"));
        }

        @Test
        void testJoinOperations() {
            // Test INNER JOIN
            String sql = Dsl.NSB.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();
            assertTrue(sql.contains("JOIN"));

            // Test LEFT JOIN
            sql = Dsl.NSB.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
            assertTrue(sql.contains("LEFT JOIN"));

            // Test RIGHT JOIN
            sql = Dsl.NSB.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
            assertTrue(sql.contains("RIGHT JOIN"));

            // Test FULL JOIN
            sql = Dsl.NSB.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
            assertTrue(sql.contains("FULL JOIN"));

            // Test CROSS JOIN
            sql = Dsl.NSB.select("*").from("users").crossJoin("departments").build().query();
            assertTrue(sql.contains("CROSS JOIN"));

            // Test NATURAL JOIN
            sql = Dsl.NSB.select("*").from("users").naturalJoin("user_profiles").build().query();
            assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        void testJoinWithCondition() {
            String sql = Dsl.NSB.select("*")
                    .from("users u")
                    .leftJoin("orders o")
                    .on(Filters.and(Filters.eq("u.id", "o.user_id"), Filters.gt("o.amount", 100)))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testLimitOffset() {
            // Test limit only
            String sql = Dsl.NSB.select("*").from("users").limit(10).build().query();

            assertTrue(sql.contains("LIMIT 10"));

            // Test limit with offset
            sql = Dsl.NSB.select("*").from("users").limit(10).offset(20).build().query();

            assertTrue(sql.contains("LIMIT 10"));
            assertTrue(sql.contains("OFFSET 20"));

            // Test limit with count and offset
            sql = Dsl.NSB.select("*").from("users").limit(10, 5).build().query();

            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testMultipleJoins() {
            String sql = Dsl.NSB.select("u.name", "o.id", "p.name")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .leftJoin("products p")
                    .on("o.product_id = p.id")
                    .where(Filters.eq("u.active", true))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("LEFT JOIN orders"));
            assertTrue(sql.contains("LEFT JOIN products"));
        }

        @Test
        void testNamedParametersConsistency() {
            // Ensure named parameters use consistent format
            String sql = Dsl.NSB.select("*")
                    .from("users")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.eq("lastName", "Doe"), Filters.gt("age", 18)))
                    .build()
                    .query();

            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
            assertTrue(sql.contains(":age"));
        }

        @Test
        void testNullArgumentsThrow() {
            assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.renderCondition(null, User.class);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.select((String) null).from("users");
            });

            assertThrows(IllegalArgumentException.class, () -> {
                Dsl.NSB.select((Collection<String>) null).from("users");
            });
        }

        @Test
        void testOrderBy() {
            // Test simple order by
            String sql = Dsl.NSB.select("*").from("users").orderBy("name").build().query();

            assertTrue(sql.contains("ORDER BY name"));

            // Test order by with direction
            sql = Dsl.NSB.select("*").from("users").orderBy("name ASC", "age DESC").build().query();

            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("name ASC"));
            assertTrue(sql.contains("age DESC"));
        }

        @Test
        void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18));
            String sql = Dsl.NSB.renderCondition(cond, User.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("status"));
            assertTrue(sql.contains(":status"));
            assertTrue(sql.contains("age"));
            assertTrue(sql.contains(":age"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testSelectEntityClass() {
            String sql = Dsl.NSB.select(User.class).from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
            // Should include all non-transient fields
            assertTrue(sql.contains("createdDate"));
            assertTrue(sql.contains("password"));
            // Should NOT include @Transient fields
            assertFalse(sql.contains("tempData"));
        }

        @Test
        void testSelectEntityClassWithAllOptions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.select(User.class, true, exclude).from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> exclude = N.asSet("password", "createdDate");
            String sql = Dsl.NSB.select(User.class, exclude).from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertFalse(sql.contains("password"));
            assertFalse(sql.contains("createdDate"));
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            String sql = Dsl.NSB.select(User.class, true).from("users u").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSelectFrom() {
            String sql = Dsl.NSB.selectFrom(User.class).where(Filters.eq("active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM test_user"));
            assertTrue(sql.contains("WHERE"));
        }

        @Test
        void testSelectFromMultipleEntities() {
            String sql = Dsl.NSB.selectFrom(User.class, "u", "user_", User.class, "m", "manager_").where(Filters.eq("u.active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = N.asSet("password");
            Set<String> excludeM = N.asSet("email", "password");

            String sql = Dsl.NSB.selectFrom(User.class, "u", "user_", excludeU, User.class, "m", "manager_", excludeM).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithAlias() {
            String sql = Dsl.NSB.selectFrom(User.class, "u").where(Filters.eq("u.active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM test_user u"));
        }

        @Test
        void testSelectFromWithAliasAndExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.selectFrom(User.class, "u", exclude).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("test_user u"));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectFromWithAliasAndSubEntities() {
            String sql = Dsl.NSB.selectFrom(User.class, "u", true).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithAllOptions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.selectFrom(User.class, "u", true, exclude).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectFromWithExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.selectFrom(User.class, exclude).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "m", "manager", null, false, null));

            String sql = Dsl.NSB.selectFrom(selections).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithSubEntities() {
            String sql = Dsl.NSB.selectFrom(User.class, true).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = Dsl.NSB.selectFrom(User.class, true, exclude).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("password"));
        }

        @Test
        void testSelectMultipleColumns() {
            String sql = Dsl.NSB.select("id", "name", "email", "created_date").from("users").where(Filters.eq("active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("name"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains("created_date"));
            assertTrue(sql.contains("FROM users"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("active"));
        }

        @Test
        void testSelectMultipleEntities() {
            String sql = Dsl.NSB.select(User.class, "u", "user_", User.class, "u2", "user2_")
                    .from("users u")
                    .join("users u2")
                    .on("u.id = u2.parent_id")
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("u.id AS \"user_.id\""));
            assertTrue(sql.contains("u2.id AS \"user2_.id\""));
        }

        @Test
        void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeUser = N.asSet("password");
            Set<String> excludeUser2 = N.asSet("email");

            String sql = Dsl.NSB.select(User.class, "u", "user_", excludeUser, User.class, "u2", "user2_", excludeUser2)
                    .from("users u")
                    .join("users u2")
                    .on("u.id = u2.parent_id")
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSelectSingleExpression() {
            String sql = Dsl.NSB.select("COUNT(*) AS total").from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT COUNT(*) AS total"));
            assertTrue(sql.contains("FROM users"));
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("u.first_name", "firstName");
            aliases.put("u.last_name", "lastName");
            aliases.put("COUNT(o.id)", "orderCount");

            String sql = Dsl.NSB.select(aliases).from("users u").leftJoin("orders o").on("u.id = o.user_id").groupBy("u.id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("u.first_name AS \"firstName\""));
            assertTrue(sql.contains("u.last_name AS \"lastName\""));
            assertTrue(sql.contains("COUNT(o.id) AS \"orderCount\""));
        }

        @Test
        void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = Dsl.NSB.select(columns).from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("email"));
        }

        @Test
        void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "m", "manager", null, false, N.asSet("password")));

            String sql = Dsl.NSB.select(selections).from("users u").join("users m").on("u.manager_id = m.id").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSpecialConditions() {
            // Test IS NULL
            String sql = Dsl.NSB.select("*").from("users").where(Filters.isNull("email")).build().query();
            assertTrue(sql.contains("IS NULL"));

            // Test IS NOT NULL
            sql = Dsl.NSB.select("*").from("users").where(Filters.isNotNull("email")).build().query();
            assertTrue(sql.contains("IS NOT NULL"));

            // Test IN
            sql = Dsl.NSB.select("*").from("users").where(Filters.in("status", Arrays.asList("active", "premium", "trial"))).build().query();
            assertTrue(sql.contains("IN"));

            // Test NOT IN
            sql = Dsl.NSB.select("*").from("users").where(Filters.notIn("status", Arrays.asList("inactive", "banned"))).build().query();
            assertTrue(sql.contains("NOT IN"));

            // Test BETWEEN
            sql = Dsl.NSB.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();
            assertTrue(sql.contains("BETWEEN"));

            // Test LIKE
            sql = Dsl.NSB.select("*").from("users").where(Filters.like("name", "%John%")).build().query();
            assertTrue(sql.contains("LIKE"));

            // Test NOT LIKE
            sql = Dsl.NSB.select("*").from("users").where(Filters.notLike("email", "%spam%")).build().query();
            assertTrue(sql.contains("NOT LIKE"));
        }

        @Test
        void testUnion() {
            SqlBuilder builder1 = Dsl.NSB.select("name").from("users");
            SqlBuilder builder2 = Dsl.NSB.select("name").from("customers");

            String sql1 = builder1.build().query();
            String sql2 = builder2.build().query();

            // Union would be manual construction
            String unionSql = sql1 + " UNION " + sql2;
            assertTrue(unionSql.contains("UNION"));
        }

        @Test
        void testUpdate() {
            String sql = Dsl.NSB.update("users").set(Arrays.asList("last_login", "status")).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("last_login"));
            assertTrue(sql.contains("status"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains(":id"));
        }

        @Test
        void testUpdateEntityClass() {
            String sql = Dsl.NSB.update(User.class).set(Arrays.asList("firstName", "email")).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE test_user"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("email"));
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> exclude = N.asSet("password", "createdDate");
            String sql = Dsl.NSB.update(User.class, exclude).set(Arrays.asList("firstName", "email")).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE test_user"));
            // Should be able to update non-excluded fields
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("email"));
        }

        @Test
        void testUpdateOperations() {
            // Test simple update
            String sql = Dsl.NSB.update("users").set("status", "active").where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("status"));

            // Test update with multiple sets
            sql = Dsl.NSB.update("users").set("firstName", "John", "lastName", "Doe").set(Map.of("age", 30)).where(Filters.eq("id", 1)).build().query();

            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("age"));

            // Test update with Map
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("age", 25);

            sql = Dsl.NSB.update("users").set(updates).where(Filters.eq("id", 2)).build().query();

            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("age"));
        }

        @Test
        void testUpdateWithEntityClass() {
            String sql = Dsl.NSB.update("user_accounts", User.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE user_accounts"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        void testWhereOperations() {
            // Test single where
            String sql = Dsl.NSB.select("*").from("users").where(Filters.eq("status", "active")).build().query();

            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("status"));

            // Test multiple where (should be AND'ed)
            sql = Dsl.NSB.select("*").from("users").where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();

            assertTrue(sql.contains("AND"));

            // Test where with OR
            sql = Dsl.NSB.select("*").from("users").where(Filters.or(Filters.eq("status", "active"), Filters.eq("status", "premium"))).build().query();

            assertTrue(sql.contains("OR"));
        }

        @Test
        void testWindowFunctions() {
            String sql = Dsl.NSB.select("name", "salary", "ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) as rank")
                    .from("employees")
                    .build()
                    .query();

            assertTrue(sql.contains("ROW_NUMBER()"));
            assertTrue(sql.contains("OVER"));
            assertTrue(sql.contains("PARTITION BY"));
        }

        @Table("test_user")
        public static class User {
            private long id;
            private String firstName;
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String password;
            @Transient
            private String tempData;

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

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getTempData() {
                return tempData;
            }

            public void setTempData(String tempData) {
                this.tempData = tempData;
            }
        }
    }

    @Nested
    class NSCTest {
        @Test
        void testAllJoinTypes() {
            // Test all join variations
            String sql;

            sql = NSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();
            assertTrue(sql.contains("JOIN"));

            sql = NSC.select("*").from("users u").innerJoin("orders o").on("u.id = o.user_id").build().query();
            assertTrue(sql.contains("INNER JOIN"));

            sql = NSC.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
            assertTrue(sql.contains("LEFT JOIN"));

            sql = NSC.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
            assertTrue(sql.contains("RIGHT JOIN"));

            sql = NSC.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
            assertTrue(sql.contains("FULL JOIN"));

            sql = NSC.select("*").from("users").crossJoin("departments").build().query();
            assertTrue(sql.contains("CROSS JOIN"));

            sql = NSC.select("*").from("users").naturalJoin("user_profiles").build().query();
            assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        void testAppendCustomSQL() {
            String sql = NSC.select("*").from("users").append(" WHERE MATCH(name) AGAINST (:search IN BOOLEAN MODE)").build().query();

            assertTrue(sql.contains("MATCH"));
            assertTrue(sql.contains("AGAINST"));
        }

        @Test
        void testBatchInsert() {
            final User john = new User();
            john.setFirstName("John");
            john.setLastName("Doe");
            final User jane = new User();
            jane.setFirstName("Jane");
            jane.setLastName("Smith");
            List<User> users = Arrays.asList(john, jane);

            String sql = NSC.batchInsert(users).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("VALUES"));
            // Should have multiple parameter sets for batch insert
        }

        @Test
        void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = NSC.batchInsert(data).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testColumnNameTransformation() {
            // Test that camelCase property names are converted to snake_case
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            String sql = NSC.insert(user).into("users").build().query();

            // Column names should be snake_case
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));

            // Parameter names should remain camelCase
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
        }

        @Test
        void testComplexNestedConditions() {
            String sql = NSC.select("*")
                    .from("users")
                    .where(Filters.or(Filters.and(Filters.eq("status", "ACTIVE"), Filters.or(Filters.lt("age", 18), Filters.gt("age", 65))),
                            Filters.and(Filters.eq("status", "PREMIUM"), Filters.between("age", 18, 65))))
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("OR"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains(":status"));
            assertTrue(sql.contains(":age"));
        }

        @Test
        void testComplexQueryWithJoins() {
            String sql = NSC.select("u.firstName", "u.lastName", "COUNT(o.id)")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .where(Filters.eq("u.active", true))
                    .groupBy("u.id", "u.firstName", "u.lastName")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("COUNT(o.id) DESC")
                    .limit(10)
                    .build()
                    .query();

            assertNotNull(sql);
            assertTrue(sql.contains("u.first_name AS \"u.firstName\""));
            assertTrue(sql.contains("u.last_name AS \"u.lastName\""));
            assertTrue(sql.contains("LEFT JOIN"));
            assertTrue(sql.contains("GROUP BY"));
            assertTrue(sql.contains("HAVING"));
            assertTrue(sql.contains("ORDER BY"));
            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testCount() {
            String sql = NSC.selectCountFrom("users").where(Filters.eq("active", true)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM users"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains(":active"));
        }

        @Test
        void testCountEntityClass() {
            String sql = NSC.selectCountFrom(User.class).where(Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18))).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM users"));
            assertTrue(sql.contains("first_name = :firstName"));
            assertTrue(sql.contains("age > :age"));
        }

        @Test
        void testDeleteFrom() {
            String sql = NSC.deleteFrom("users").where(Filters.eq("status", "INACTIVE")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM users"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("status = :status"));
        }

        @Test
        void testDeleteFromEntityClass() {
            String sql = NSC.deleteFrom(User.class).where(Filters.eq("status", "INACTIVE")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM users"));
            assertTrue(sql.contains("status = :status"));
        }

        @Test
        void testDeleteFromWithEntityClass() {
            String sql = NSC.deleteFrom("users", User.class).where(Filters.eq("status", "INACTIVE")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM users"));
            assertTrue(sql.contains("status = :status"));
        }

        @Test
        void testEmptyArgumentsThrow() {
            assertThrows(IllegalArgumentException.class, () -> {
                NSC.select("");
            });

            assertThrows(IllegalArgumentException.class, () -> {
                NSC.select(new String[0]);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                NSC.select(new ArrayList<Selection>());
            });

            assertThrows(IllegalArgumentException.class, () -> {
                NSC.select(new HashMap<>());
            });
        }

        @Test
        void testForUpdate() {
            String sql = NSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

            assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        void testInsertInto() {
            String sql = NSC.insertInto(User.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("VALUES"));
        }

        @Test
        void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = NSC.insertInto(User.class, excluded).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertFalse(sql.contains("id"));
        }

        @Test
        void testInsertMultipleColumns() {
            String sql = NSC.insert("firstName", "lastName", "email").into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
            assertTrue(sql.contains(":email"));
        }

        @Test
        void testInsertSingleExpression() {
            String sql = NSC.insert("name").into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("(name)"));
            assertTrue(sql.contains("VALUES (:name)"));
        }

        @Test
        void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = NSC.insert(columns).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains(":firstName"));
        }

        @Test
        void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail("john@example.com");
            user.setCreatedDate(new Date(1_700_000_000_000L)); // Should be excluded (ReadOnly)
            user.setTempField("temp"); // Should be excluded (Transient)

            String sql = NSC.insert(user).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("created_date"));
            assertFalse(sql.contains("temp_field"));
        }

        @Test
        void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail("john@example.com");

            Set<String> excluded = Set.of("email");
            String sql = NSC.insert(user, excluded).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertFalse(sql.contains("email"));
        }

        @Test
        void testInsertWithEntityClass() {
            String sql = NSC.insert(User.class).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains("status"));
            assertFalse(sql.contains("created_date"));
            assertFalse(sql.contains("temp_field"));
        }

        @Test
        void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "status");
            String sql = NSC.insert(User.class, excluded).into("users").build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("id"));
            assertFalse(sql.contains("status"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            String sql = NSC.insert(props).into("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains(":lastName"));
        }

        @Test
        void testLimitOffset() {
            String sql = NSC.select("*").from("users").limit(10).offset(20).build().query();

            assertTrue(sql.contains("LIMIT 10"));
            assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = NSC.select("*").from("users").limit(10, 20).build().query();

            assertTrue(sql.contains("LIMIT"));
        }

        @Test
        void testNamedParametersAreCorrect() {
            // Ensure named parameters follow the correct pattern
            String sql = NSC.select("*")
                    .from("users")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.in("status", Arrays.asList("ACTIVE", "PREMIUM")),
                            Filters.between("age", 18, 65)))
                    .build()
                    .query();

            assertTrue(sql.contains(":firstName"));
            assertTrue(sql.contains("status IN (:status1, :status2)"));
            assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
        }

        @Test
        void testNamingPolicySnakeCase() {
            // NSC uses SNAKE_CASE naming policy
            String sql = NSC.select("firstName", "lastName").from("userAccounts").build().query();

            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("userAccounts"));
        }

        @Test
        void testNullConditionThrows() {
            assertThrows(IllegalArgumentException.class, () -> {
                NSC.renderCondition(null, User.class);
            });
        }

        @Test
        void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18), Filters.like("email", "%@example.com"));
            String sql = NSC.renderCondition(cond, User.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("first_name = :firstName"));
            assertTrue(sql.contains("age > :age"));
            assertTrue(sql.contains("email LIKE :email"));
        }

        @Test
        void testSelectEntityClass() {
            String sql = NSC.select(User.class).from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
            assertTrue(sql.contains("email"));
            assertTrue(sql.contains("created_date AS \"createdDate\""));
            assertTrue(sql.contains("status"));
            assertFalse(sql.contains("temp_field"));
        }

        @Test
        void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.select(User.class, true, excluded).from("users").build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("status"));
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdDate", "status");
            String sql = NSC.select(User.class, excluded).from("users").build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("created_date"));
            assertFalse(sql.contains("status"));
            assertTrue(sql.contains("first_name"));
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            String sql = NSC.select(User.class, true).from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSelectFrom() {
            String sql = NSC.selectFrom(User.class).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM users"));
        }

        @Test
        void testSelectFromMultipleEntities() {
            String sql = NSC.selectFrom(User.class, "u", "user", User.class, "m", "manager").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = Set.of("status");
            Set<String> excludeM = Set.of("email");

            String sql = NSC.selectFrom(User.class, "u", "user", excludeU, User.class, "m", "manager", excludeM).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithAlias() {
            String sql = NSC.selectFrom(User.class, "u").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM users u"));
        }

        @Test
        void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, "u", excluded).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("users u"));
            assertFalse(sql.contains("status"));
        }

        @Test
        void testSelectFromWithAliasAndSubEntities() {
            String sql = NSC.selectFrom(User.class, "u", true).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, "u", true, excluded).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("status"));
        }

        @Test
        void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, excluded).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("status"));
        }

        @Test
        void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "m", "manager", null, false, null));

            String sql = NSC.selectFrom(selections).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithSubEntities() {
            String sql = NSC.selectFrom(User.class, true).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM"));
        }

        @Test
        void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, true, excluded).build().query();
            assertNotNull(sql);
            assertFalse(sql.contains("status"));
        }

        @Test
        void testSelectMultipleColumns() {
            String sql = NSC.select("firstName", "lastName", "email").from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
            assertTrue(sql.contains("email"));
        }

        @Test
        void testSelectMultipleEntities() {
            String sql = NSC.select(User.class, "u", "user", User.class, "m", "manager")
                    .from("users u")
                    .join("users m")
                    .on("u.manager_id = m.id")
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("u.id AS \"user.id\""));
            assertTrue(sql.contains("m.id AS \"manager.id\""));
        }

        @Test
        void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeU = Set.of("status");
            Set<String> excludeM = Set.of("email");

            String sql = NSC.select(User.class, "u", "user", excludeU, User.class, "m", "manager", excludeM)
                    .from("users u")
                    .join("users m")
                    .on("u.manager_id = m.id")
                    .build()
                    .query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSelectSinglePart() {
            String sql = NSC.select("COUNT(*)").from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM users"));
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = NSC.select(aliases).from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("first_name AS \"fname\""));
            assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = NSC.select(columns).from("users").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "m", "manager", null, false, Set.of("status")));

            String sql = NSC.select(selections).from("users u").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testUpdate() {
            String sql = NSC.update("users").set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("first_name = :firstName"));
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("id = :id"));
        }

        @Test
        void testUpdateEntityClass() {
            String sql = NSC.update(User.class).set(Arrays.asList("firstName", "email")).where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdDate", "status");
            String sql = NSC.update(User.class, excluded).set("firstName").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("first_name"));
        }

        @Test
        void testUpdateWithEntityClass() {
            String sql = NSC.update("users", User.class).set("status").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE users"));
            assertTrue(sql.contains("SET"));
            assertTrue(sql.contains("status"));
        }

        @Test
        void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = NSC.update("users").set(updates).where(Filters.eq("id", 1)).build().query();

            assertNotNull(sql);
            assertTrue(sql.contains("first_name = :firstName"));
            assertTrue(sql.contains("last_name = :lastName"));
            assertTrue(sql.contains("id = :id"));
        }

        @Table("users")
        public static class User {
            private long id;
            @Column("first_name")
            private String firstName;
            @Column("last_name")
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String status;
            @Transient
            private String tempField;

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

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public String getTempField() {
                return tempField;
            }

            public void setTempField(String tempField) {
                this.tempField = tempField;
            }
        }
    }

    @Nested
    class PACTest {
        @Test
        void testAllMethodsWithNullParameters() {
            // Test all methods handle null appropriately
            // assertThrows(IllegalArgumentException.class, () -> PAC.insert((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((String[]) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((Map<String, Object>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((Object) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insert((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.insertInto((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.batchInsert(null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.update((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.update((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.deleteFrom((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.deleteFrom((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((String[]) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((Map<String, String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.selectFrom((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.select((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.selectFrom((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.selectCountFrom((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.selectCountFrom((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PAC.renderCondition(null, UserAccount.class));
        }

        @Test
        void testBatchInsert() {
            List<UserAccount> accounts = Arrays.asList(new UserAccount("John", "Doe"), new UserAccount("Jane", "Smith"));

            SqlBuilder builder = Dsl.PAC.batchInsert(accounts);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("VALUES"));
            assertTrue(sp.query().contains("(?, ?)"));
            assertTrue(sp.query().contains(", (?, ?)"));
        }

        @Test
        void testComplexQueries() {
            // Test complex query with joins
            Set<String> excludeUser1 = new HashSet<>(Arrays.asList("tempData"));
            Set<String> excludeUser2 = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder builder = Dsl.PAC.select(UserAccount.class, "u1", "user1", excludeUser1, UserAccount.class, "u2", "user2", excludeUser2);

            String sql = builder.from("USER_ACCOUNT u1, USER_ACCOUNT u2").where(Filters.eq("u1.id", "u2.id")).build().query();

            assertTrue(sql.contains("u1.ID AS \"user1.id\""));
            assertTrue(sql.contains("u2.ID AS \"user2.id\""));
            assertFalse(sql.contains("user1.tempData"));
            assertFalse(sql.contains("user2.createdDate"));
        }

        @Test
        void testCount_EntityClass() {
            SqlBuilder builder = Dsl.PAC.selectCountFrom(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        void testCount_TableName() {
            SqlBuilder builder = Dsl.PAC.selectCountFrom("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        void testDeleteFrom_EntityClass() {
            SqlBuilder builder = Dsl.PAC.deleteFrom(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM USER_ACCOUNT"));
        }

        @Test
        void testDeleteFrom_TableName() {
            SqlBuilder builder = Dsl.PAC.deleteFrom("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM USER_ACCOUNT"));
        }

        @Test
        void testInsert_Class() {
            SqlBuilder builder = Dsl.PAC.insert(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
            assertFalse(sql.contains("CREATED_DATE")); // ReadOnly
            assertFalse(sql.contains("TEMP_DATA")); // Transient
        }

        @Test
        void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "emailAddress");
            SqlBuilder builder = Dsl.PAC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
        }

        @Test
        void testInsert_Entity() {
            UserAccount account = new UserAccount("John", "Doe");
            account.setEmailAddress("john@example.com");

            SqlBuilder builder = Dsl.PAC.insert(account);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("FIRST_NAME"));
            assertTrue(sp.query().contains("LAST_NAME"));
            assertTrue(sp.query().contains("EMAIL_ADDRESS"));
            assertFalse(sp.query().contains("CREATED_DATE")); // ReadOnly
        }

        @Test
        void testInsert_EntityWithExcludedProps() {
            UserAccount account = new UserAccount("John", "Doe");
            account.setEmailAddress("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder builder = Dsl.PAC.insert(account, excludedProps);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("FIRST_NAME"));
            assertTrue(sp.query().contains("LAST_NAME"));
            assertFalse(sp.query().contains("EMAIL_ADDRESS"));
        }

        @Test
        void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SqlBuilder builder = Dsl.PAC.insert(props);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("FIRST_NAME"));
            assertTrue(sp.query().contains("LAST_NAME"));
            assertEquals(2, sp.parameters().size());
        }

        @Test
        void testInsert_MultipleColumns() {
            SqlBuilder builder = Dsl.PAC.insert("firstName", "lastName", "emailAddress");
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
        }

        @Test
        void testInsert_SingleColumn() {
            SqlBuilder builder = Dsl.PAC.insert("firstName");
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        void testInsertInto_Class() {
            SqlBuilder builder = Dsl.PAC.insertInto(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        void testNamingPolicy() {
            // Test that PAC uses UPPER_CASE naming
            SqlBuilder builder = Dsl.PAC.select("firstName", "lastName", "emailAddress");
            String sql = builder.from("USER_ACCOUNT").build().query();

            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
        }

        @Test
        void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("id", 1));

            SqlBuilder builder = Dsl.PAC.renderCondition(cond, UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FIRST_NAME = ?"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("ID > ?"));
        }

        @Test
        void testSelect_EntityClass() {
            SqlBuilder builder = Dsl.PAC.select(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
            assertFalse(sql.contains("TEMP_DATA"));
        }

        @Test
        void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = Dsl.PAC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            assertTrue(sql.contains("LAST_NAME AS \"lname\""));
        }

        @Test
        void testSelect_MultipleColumns() {
            SqlBuilder builder = Dsl.PAC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
        }

        @Test
        void testSelect_SingleColumn() {
            SqlBuilder builder = Dsl.PAC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        void testSelectFrom_EntityClass() {
            SqlBuilder builder = Dsl.PAC.selectFrom(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
        }

        @Test
        void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = Dsl.PAC.selectFrom(UserAccount.class, "u");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FROM USER_ACCOUNT u"));
            assertTrue(sql.contains("u.FIRST_NAME AS \"firstName\""));
        }

        @Test
        void testSelectFrom_TwoEntities() {
            SqlBuilder builder = Dsl.PAC.selectFrom(UserAccount.class, "u1", "user1", UserAccount.class, "u2", "user2");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("u1.FIRST_NAME AS \"user1.firstName\""));
            assertTrue(sql.contains("u2.FIRST_NAME AS \"user2.firstName\""));
        }

        @Test
        void testUpdate_EntityClass() {
            SqlBuilder builder = Dsl.PAC.update(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = ?"));
        }

        @Test
        void testUpdate_TableName() {
            SqlBuilder builder = Dsl.PAC.update("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = ?"));
        }

        @Table("USER_ACCOUNT")
        public static class UserAccount {
            @Id
            private Long id;
            private String firstName;
            private String lastName;
            private String emailAddress;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String accountStatus;
            @Transient
            private String tempData;

            public UserAccount() {
            }

            public UserAccount(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
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

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }

            public String getAccountStatus() {
                return accountStatus;
            }

            public void setAccountStatus(String accountStatus) {
                this.accountStatus = accountStatus;
            }

            public String getTempData() {
                return tempData;
            }

            public void setTempData(String tempData) {
                this.tempData = tempData;
            }
        }
    }

    @Nested
    class PLCTest {
        @Test
        void testAllMethodOverloads() {
            // Test all overloaded methods
            assertNotNull(Dsl.PLC.insert(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.insertInto(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.update("userProfile", UserProfile.class));
            assertNotNull(Dsl.PLC.update(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.deleteFrom("userProfile", UserProfile.class));
            assertNotNull(Dsl.PLC.select(UserProfile.class, true));
            assertNotNull(Dsl.PLC.select(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.select(UserProfile.class, true, new HashSet<>()));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, true));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, "p", true));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, new HashSet<>()));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, "p", new HashSet<>()));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, true, new HashSet<>()));
            assertNotNull(Dsl.PLC.selectFrom(UserProfile.class, "p", true, new HashSet<>()));
        }

        @Test
        void testBatchInsert() {
            List<UserProfile> profiles = Arrays.asList(new UserProfile("John", "Doe"), new UserProfile("Jane", "Smith"));

            SqlBuilder builder = Dsl.PLC.batchInsert(profiles);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("VALUES"));
            assertTrue(sp.query().contains("(?, ?)"));
            assertTrue(sp.query().contains(", (?, ?)"));
        }

        @Test
        void testComplexQueries() {
            // Test complex query with multiple selections
            List<Selection> selections = Arrays.asList(selection(UserProfile.class, "p1", "profile1", Arrays.asList("id", "firstName"), false, null),
                    selection(UserProfile.class, "p2", "profile2", null, false, new HashSet<>(Arrays.asList("sessionData", "lastLoginDate"))));

            SqlBuilder builder = Dsl.PLC.select(selections);
            String sql = builder.from("userProfile p1, userProfile p2").build().query();

            assertTrue(sql.contains("p1.id AS \"profile1.id\""));
            assertTrue(sql.contains("p1.firstName AS \"profile1.firstName\""));
            assertFalse(sql.contains("profile2.sessionData"));
            assertFalse(sql.contains("profile2.lastLoginDate"));
        }

        @Test
        void testCount_EntityClass() {
            SqlBuilder builder = Dsl.PLC.selectCountFrom(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        void testCount_TableName() {
            SqlBuilder builder = Dsl.PLC.selectCountFrom("userProfile");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        void testDeleteFrom_EntityClass() {
            SqlBuilder builder = Dsl.PLC.deleteFrom(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM userProfile"));
        }

        @Test
        void testDeleteFrom_TableName() {
            SqlBuilder builder = Dsl.PLC.deleteFrom("userProfile");
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM userProfile"));
        }

        @Test
        void testErrorCases() {
            // Test error cases
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.insert(""));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.insert(new String[0]));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.insert(new ArrayList<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.insert(new HashMap<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.batchInsert(new ArrayList<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.update(""));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.deleteFrom(""));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(""));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(new String[0]));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(new ArrayList<Selection>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(new HashMap<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.select(new ArrayList<Selection>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.selectFrom(new ArrayList<>()));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PLC.selectCountFrom(""));
        }

        @Test
        void testInsert_Class() {
            SqlBuilder builder = Dsl.PLC.insert(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertTrue(sql.contains("isActive"));
            assertFalse(sql.contains("lastLoginDate")); // ReadOnly
            assertFalse(sql.contains("sessionData")); // Transient
        }

        @Test
        void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "isActive");
            SqlBuilder builder = Dsl.PLC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("isActive"));
        }

        @Test
        void testInsert_Entity() {
            UserProfile profile = new UserProfile("John", "Doe");
            profile.setEmailAddress("john@example.com");
            profile.setIsActive(true);

            SqlBuilder builder = Dsl.PLC.insert(profile);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("firstName"));
            assertTrue(sp.query().contains("lastName"));
            assertTrue(sp.query().contains("emailAddress"));
            assertTrue(sp.query().contains("isActive"));
            assertFalse(sp.query().contains("lastLoginDate")); // ReadOnly
        }

        @Test
        void testInsert_EntityWithExcludedProps() {
            UserProfile profile = new UserProfile("John", "Doe");
            profile.setEmailAddress("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder builder = Dsl.PLC.insert(profile, excludedProps);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("firstName"));
            assertTrue(sp.query().contains("lastName"));
            assertFalse(sp.query().contains("emailAddress"));
        }

        @Test
        void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("isActive", true);

            SqlBuilder builder = Dsl.PLC.insert(props);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("firstName"));
            assertTrue(sp.query().contains("lastName"));
            assertTrue(sp.query().contains("isActive"));
            assertEquals(3, sp.parameters().size());
        }

        @Test
        void testInsert_MultipleColumns() {
            SqlBuilder builder = Dsl.PLC.insert("firstName", "lastName", "emailAddress");
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
        }

        @Test
        void testInsert_SingleColumn() {
            SqlBuilder builder = Dsl.PLC.insert("firstName");
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("INSERT INTO userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        void testInsertInto_Class() {
            SqlBuilder builder = Dsl.PLC.insertInto(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        void testNamingPolicy() {
            // Test that PLC uses camelCase naming (no transformation)
            SqlBuilder builder = Dsl.PLC.select("firstName", "lastName", "emailAddress", "isActive");
            String sql = builder.from("userProfile").build().query();

            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertTrue(sql.contains("isActive"));

            // Verify no snake_case or UPPER_CASE transformation
            assertFalse(sql.contains("first_name"));
            assertFalse(sql.contains("FIRST_NAME"));
            assertFalse(sql.contains("email_address"));
            assertFalse(sql.contains("EMAIL_ADDRESS"));
            assertFalse(sql.contains("is_active"));
            assertFalse(sql.contains("IS_ACTIVE"));
        }

        @Test
        void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.eq("isActive", true));

            SqlBuilder builder = Dsl.PLC.renderCondition(cond, UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("firstName = ?"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("isActive = ?"));
        }

        @Test
        void testSelect_EntityClass() {
            SqlBuilder builder = Dsl.PLC.select(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertTrue(sql.contains("isActive"));
            assertFalse(sql.contains("sessionData")); // Transient
        }

        @Test
        void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = Dsl.PLC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("firstName AS \"fname\""));
            assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        void testSelect_MultipleColumns() {
            SqlBuilder builder = Dsl.PLC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
        }

        @Test
        void testSelect_SingleColumn() {
            SqlBuilder builder = Dsl.PLC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        void testSelectFrom_EntityClass() {
            SqlBuilder builder = Dsl.PLC.selectFrom(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = Dsl.PLC.selectFrom(UserProfile.class, "p");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FROM userProfile p"));
            assertTrue(sql.contains("p.firstName"));
        }

        @Test
        void testSelectFrom_TwoEntities() {
            SqlBuilder builder = Dsl.PLC.selectFrom(UserProfile.class, "p1", "profile1", UserProfile.class, "p2", "profile2");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("p1.firstName AS \"profile1.firstName\""));
            assertTrue(sql.contains("p2.firstName AS \"profile2.firstName\""));
        }

        @Test
        void testUpdate_EntityClass() {
            SqlBuilder builder = Dsl.PLC.update(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE userProfile"));
            assertTrue(sql.contains("firstName = ?"));
        }

        @Test
        void testUpdate_TableName() {
            SqlBuilder builder = Dsl.PLC.update("userProfile");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE userProfile"));
            assertTrue(sql.contains("firstName = ?"));
        }

        @Table("userProfile")
        public static class UserProfile {
            @Id
            private Long id;
            private String firstName;
            private String lastName;
            private String emailAddress;
            private Boolean isActive;
            @ReadOnly
            private Date lastLoginDate;
            @NonUpdatable
            private String accountType;
            @Transient
            private String sessionData;

            public UserProfile() {
            }

            public UserProfile(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
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

            public String getEmailAddress() {
                return emailAddress;
            }

            public void setEmailAddress(String emailAddress) {
                this.emailAddress = emailAddress;
            }

            public Boolean getIsActive() {
                return isActive;
            }

            public void setIsActive(Boolean isActive) {
                this.isActive = isActive;
            }

            public Date getLastLoginDate() {
                return lastLoginDate;
            }

            public void setLastLoginDate(Date lastLoginDate) {
                this.lastLoginDate = lastLoginDate;
            }

            public String getAccountType() {
                return accountType;
            }

            public void setAccountType(String accountType) {
                this.accountType = accountType;
            }

            public String getSessionData() {
                return sessionData;
            }

            public void setSessionData(String sessionData) {
                this.sessionData = sessionData;
            }
        }
    }

    @Nested
    class PSBTest {
        @Test
        void testBatchInsert() {
            List<User> users = Arrays.asList(new User("John", "Doe", "john@example.com"), new User("Jane", "Smith", "jane@example.com"));

            SqlBuilder builder = Dsl.PSB.batchInsert(users);
            assertNotNull(builder);

            // Test with empty collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.batchInsert(new ArrayList<>()));

            // Test with null collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.batchInsert(null));

            // Test with maps
            List<Map<String, Object>> maps = new ArrayList<>();
            Map<String, Object> map1 = new HashMap<>();
            map1.put("firstName", "John");
            maps.add(map1);

            SqlBuilder mapBuilder = Dsl.PSB.batchInsert(maps);
            assertNotNull(mapBuilder);
        }

        @Test
        void testCount_EntityClass() {
            SqlBuilder builder = Dsl.PSB.selectCountFrom(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.selectCountFrom((Class<?>) null));
        }

        @Test
        void testCount_TableName() {
            SqlBuilder builder = Dsl.PSB.selectCountFrom("users");
            assertNotNull(builder);

            // Test with null/empty table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.selectCountFrom((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.selectCountFrom(""));
        }

        @Test
        void testDeleteFrom_EntityClass() {
            SqlBuilder builder = Dsl.PSB.deleteFrom(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom((Class<?>) null));
        }

        @Test
        void testDeleteFrom_TableName() {
            SqlBuilder builder = Dsl.PSB.deleteFrom("users");
            assertNotNull(builder);

            // Test with null table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom((String) null));

            // Test with empty table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom(""));
        }

        @Test
        void testDeleteFrom_TableNameWithEntityClass() {
            SqlBuilder builder = Dsl.PSB.deleteFrom("users", User.class);
            assertNotNull(builder);

            // Test with null parameters
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom(null, User.class));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.deleteFrom("users", null));
        }

        @Test
        void testInsert_Class() {
            SqlBuilder builder = Dsl.PSB.insert(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((Class<?>) null));
        }

        @Test
        void testInsert_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder builder = Dsl.PSB.insert(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(null, excludedProps));
        }

        @Test
        void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder builder = Dsl.PSB.insert(columns);
            assertNotNull(builder);

            // Test with null collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((Collection<String>) null));

            // Test with empty collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(new ArrayList<>()));
        }

        @Test
        void testInsert_Entity() {
            User user = new User("John", "Doe", "john@example.com");
            SqlBuilder builder = Dsl.PSB.insert(user);
            assertNotNull(builder);

            // Test with null entity
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((Object) null));
        }

        @Test
        void testInsert_EntityWithExcludedProps() {
            User user = new User("John", "Doe", "john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));

            SqlBuilder builder = Dsl.PSB.insert(user, excludedProps);
            assertNotNull(builder);

            // Test with null entity
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(null, excludedProps));
        }

        @Test
        void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SqlBuilder builder = Dsl.PSB.insert(props);
            assertNotNull(builder);

            // Test with null map
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((Map<String, Object>) null));

            // Test with empty map
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(new HashMap<>()));
        }

        @Test
        void testInsert_MultipleColumns() {
            SqlBuilder builder = Dsl.PSB.insert("firstName", "lastName", "email");
            assertNotNull(builder);

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert((String[]) null));

            // Test with empty array
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(new String[0]));
        }

        @Test
        void testInsert_SingleColumn() {
            SqlBuilder builder = Dsl.PSB.insert("name");
            assertNotNull(builder);

            // Test with empty string - should throw exception
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insert(""));
        }

        @Test
        void testInsertInto_Class() {
            SqlBuilder builder = Dsl.PSB.insertInto(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insertInto((Class<?>) null));
        }

        @Test
        void testInsertInto_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder builder = Dsl.PSB.insertInto(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.insertInto(null, excludedProps));
        }

        @Test
        void testParse() {
            Condition cond = Filters.eq("firstName", "John");
            SqlBuilder builder = Dsl.PSB.renderCondition(cond, User.class);
            assertNotNull(builder);

            // Test with null condition
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.renderCondition(null, User.class));

            // Test with complex condition
            Condition complexCond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("id", 1));
            SqlBuilder complexBuilder = Dsl.PSB.renderCondition(complexCond, User.class);
            assertNotNull(complexBuilder);
        }

        @Test
        void testSelect_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            SqlBuilder builder = Dsl.PSB.select(columns);
            assertNotNull(builder);

            // Test with null/empty collection
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(new ArrayList<Selection>()));
        }

        @Test
        void testSelect_EntityClass() {
            SqlBuilder builder = Dsl.PSB.select(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((Class<?>) null));
        }

        @Test
        void testSelect_EntityClassFullOptions() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.select(User.class, true, excludedProps);
            assertNotNull(builder);

            // Test all combinations
            assertNotNull(Dsl.PSB.select(User.class, false, null));
            assertNotNull(Dsl.PSB.select(User.class, true, null));
            assertNotNull(Dsl.PSB.select(User.class, false, excludedProps));
        }

        @Test
        void testSelect_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.select(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null excluded props
            SqlBuilder builder2 = Dsl.PSB.select(User.class, null);
            assertNotNull(builder2);
        }

        @Test
        void testSelect_EntityClassWithSubEntities() {
            SqlBuilder builder = Dsl.PSB.select(User.class, true);
            assertNotNull(builder);

            // Test without sub-entities
            SqlBuilder builder2 = Dsl.PSB.select(User.class, false);
            assertNotNull(builder2);
        }

        @Test
        void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = Dsl.PSB.select(aliases);
            assertNotNull(builder);

            // Test with null/empty map
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((Map<String, String>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(new HashMap<>()));
        }

        @Test
        void testSelect_MultipleColumns() {
            SqlBuilder builder = Dsl.PSB.select("firstName", "lastName", "email");
            assertNotNull(builder);

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((String[]) null));

            // Test with empty array
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(new String[0]));
        }

        @Test
        void testSelect_MultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder builder = Dsl.PSB.select(selections);
            assertNotNull(builder);

            // Test with null/empty list
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(new ArrayList<Selection>()));
        }

        @Test
        void testSelect_SingleColumn() {
            SqlBuilder builder = Dsl.PSB.select("firstName");
            assertNotNull(builder);

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select((String) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.select(""));
        }

        @Test
        void testSelect_TwoEntities() {
            SqlBuilder builder = Dsl.PSB.select(User.class, "u", "user", User.class, "u2", "user2");
            assertNotNull(builder);
        }

        @Test
        void testSelect_TwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder builder = Dsl.PSB.select(User.class, "u", "user", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(builder);

            // Test with null exclusions
            SqlBuilder builder2 = Dsl.PSB.select(User.class, "u", "user", null, User.class, "u2", "user2", null);
            assertNotNull(builder2);
        }

        @Test
        void testSelectFrom_EntityClass() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.selectFrom((Class<?>) null));
        }

        @Test
        void testSelectFrom_EntityClassFullOptions() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", true, excludedProps);
            assertNotNull(builder);

            // Test various combinations
            assertNotNull(Dsl.PSB.selectFrom(User.class, "u", false, null));
            assertNotNull(Dsl.PSB.selectFrom(User.class, null, true, excludedProps));
        }

        @Test
        void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u");
            assertNotNull(builder);

            // Test with null alias
            SqlBuilder builder2 = Dsl.PSB.selectFrom(User.class, (String) null);
            assertNotNull(builder2);
        }

        @Test
        void testSelectFrom_EntityClassWithAliasAndExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", excludedProps);
            assertNotNull(builder);
        }

        @Test
        void testSelectFrom_EntityClassWithAliasAndSubEntities() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", true);
            assertNotNull(builder);

            SqlBuilder builder2 = Dsl.PSB.selectFrom(User.class, "u", false);
            assertNotNull(builder2);
        }

        @Test
        void testSelectFrom_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, excludedProps);
            assertNotNull(builder);
        }

        @Test
        void testSelectFrom_EntityClassWithSubEntities() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, true);
            assertNotNull(builder);

            SqlBuilder builder2 = Dsl.PSB.selectFrom(User.class, false);
            assertNotNull(builder2);
        }

        @Test
        void testSelectFrom_EntityClassWithSubEntitiesAndExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, true, excludedProps);
            assertNotNull(builder);
        }

        @Test
        void testSelectFrom_MultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(User.class, "u", "user", null, false, null),
                    selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder builder = Dsl.PSB.selectFrom(selections);
            assertNotNull(builder);

            // Test with invalid selections
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.selectFrom((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.selectFrom(new ArrayList<>()));
        }

        @Test
        void testSelectFrom_TwoEntities() {
            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", "user", User.class, "u2", "user2");
            assertNotNull(builder);
        }

        @Test
        void testSelectFrom_TwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder builder = Dsl.PSB.selectFrom(User.class, "u", "user", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(builder);
        }

        @Test
        void testUpdate_EntityClass() {
            SqlBuilder builder = Dsl.PSB.update(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update((Class<?>) null));
        }

        @Test
        void testUpdate_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder builder = Dsl.PSB.update(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update(null, excludedProps));
        }

        @Test
        void testUpdate_TableName() {
            SqlBuilder builder = Dsl.PSB.update("users");
            assertNotNull(builder);

            // Test with null table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update((String) null));

            // Test with empty table name
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update(""));
        }

        @Test
        void testUpdate_TableNameWithEntityClass() {
            SqlBuilder builder = Dsl.PSB.update("users", User.class);
            assertNotNull(builder);

            // Test with null parameters
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update(null, User.class));
            assertThrows(IllegalArgumentException.class, () -> Dsl.PSB.update("users", null));
        }

        @Table("test_users")
        public static class User {
            @Id
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String nonUpdatableField;
            @Transient
            private String transientField;

            public User() {
            }

            public User(String firstName, String lastName, String email) {
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

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }

            public String getNonUpdatableField() {
                return nonUpdatableField;
            }

            public void setNonUpdatableField(String nonUpdatableField) {
                this.nonUpdatableField = nonUpdatableField;
            }

            public String getTransientField() {
                return transientField;
            }

            public void setTransientField(String transientField) {
                this.transientField = transientField;
            }
        }
    }

    @Nested
    class PSCTest {
        @Test
        void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account("John", "Doe"), new Account("Jane", "Smith"));

            SqlBuilder builder = PSC.batchInsert(accounts);
            assertNotNull(builder);

            SP sp = builder.into("account").build();
            assertTrue(sp.query().contains("VALUES"));
            assertTrue(sp.query().contains("(?, ?)"));
            assertTrue(sp.query().contains(", (?, ?)"));
            assertEquals(4, sp.parameters().size()); // 2 accounts * 2 fields each
        }

        @Test
        void testCount_EntityClass() {
            SqlBuilder builder = PSC.selectCountFrom(Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        void testCount_TableName() {
            SqlBuilder builder = PSC.selectCountFrom("account");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        void testCreateInstance() {
            SqlBuilder instance1 = PSC.select("*");
            SqlBuilder instance2 = PSC.select("*");

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }

        @Test
        void testDeleteFrom_EntityClass() {
            SqlBuilder builder = PSC.deleteFrom(Account.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
        }

        @Test
        void testDeleteFrom_TableName() {
            SqlBuilder builder = PSC.deleteFrom("account");
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("WHERE"));
        }

        @Test
        void testDeleteFrom_TableNameWithEntityClass() {
            SqlBuilder builder = PSC.deleteFrom("account", Account.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("firstName", "John")).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        void testInsert_Class() {
            SqlBuilder builder = PSC.insert(Account.class);
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("created_date")); // ReadOnly
            assertFalse(sql.contains("transient_field")); // Transient
        }

        @Test
        void testInsert_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email", "status"));
            SqlBuilder builder = PSC.insert(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertFalse(sql.contains("email"));
            assertFalse(sql.contains("status"));
        }

        @Test
        void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder builder = PSC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        void testInsert_Entity() {
            Account account = new Account("John", "Doe");
            account.setEmail("john@example.com");

            SqlBuilder builder = PSC.insert(account);
            assertNotNull(builder);

            SP sp = builder.into("account").build();
            assertTrue(sp.query().contains("first_name"));
            assertTrue(sp.query().contains("last_name"));
            assertTrue(sp.query().contains("email"));
            assertFalse(sp.query().contains("created_date")); // ReadOnly field should be excluded
        }

        @Test
        void testInsert_EntityWithExcludedProps() {
            Account account = new Account("John", "Doe");
            account.setEmail("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));

            SqlBuilder builder = PSC.insert(account, excludedProps);
            assertNotNull(builder);

            SP sp = builder.into("account").build();
            assertTrue(sp.query().contains("first_name"));
            assertTrue(sp.query().contains("last_name"));
            assertFalse(sp.query().contains("email"));
        }

        @Test
        void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SqlBuilder builder = PSC.insert(props);
            assertNotNull(builder);

            SP sp = builder.into("account").build();
            assertTrue(sp.query().contains("first_name"));
            assertTrue(sp.query().contains("last_name"));
            assertEquals(2, sp.parameters().size());
        }

        @Test
        void testInsert_MultipleColumns() {
            SqlBuilder builder = PSC.insert("firstName", "lastName", "email");
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        void testInsert_SingleColumn() {
            SqlBuilder builder = PSC.insert("firstName");
            assertNotNull(builder);

            // Test SQL generation
            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("first_name"));
        }

        @Test
        void testInsertInto_Class() {
            SqlBuilder builder = PSC.insertInto(Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
        }

        @Test
        void testInsertInto_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder builder = PSC.insertInto(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertFalse(sql.contains(" id "));
            assertFalse(sql.contains("created_date"));
        }

        @Test
        void testNamingPolicy() {
            // Test that PSC uses snake_case naming
            SqlBuilder builder = PSC.select("firstName", "lastName", "emailAddress");
            String sql = builder.from("account").build().query();

            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
            assertTrue(sql.contains("email_address AS \"emailAddress\""));
        }

        @Test
        void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.like("email", "%@example.com"));

            SqlBuilder builder = PSC.renderCondition(cond, Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("first_name = ?"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("email LIKE ?"));
        }

        @Test
        void testSelect_Collection() {
            List<String> columns = Arrays.asList("id", "firstName", "lastName");
            SqlBuilder builder = PSC.select(columns);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        void testSelect_EntityClass() {
            SqlBuilder builder = PSC.select(Account.class);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("transient_field"));
        }

        @Test
        void testSelect_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email", "status"));
            SqlBuilder builder = PSC.select(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name"));
            assertFalse(sql.contains("email"));
            assertFalse(sql.contains("status"));
        }

        @Test
        void testSelect_EntityClassWithSubEntities() {
            SqlBuilder builder = PSC.select(Account.class, true);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = PSC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("first_name AS \"fname\""));
            assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        void testSelect_MultipleColumns() {
            SqlBuilder builder = PSC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        void testSelect_MultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", Arrays.asList("id", "firstName"), false, null),
                    selection(Account.class, "a2", "account2", null, false, new HashSet<>(Arrays.asList("email"))));

            SqlBuilder builder = PSC.select(selections);
            assertNotNull(builder);

            String sql = builder.from("account a, account a2").build().query();
            assertTrue(sql.contains("a.id AS \"account.id\""));
            assertTrue(sql.contains("a.first_name AS \"account.firstName\""));
            assertFalse(sql.contains("account2.email"));
        }

        @Test
        void testSelect_SingleColumn() {
            SqlBuilder builder = PSC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        void testSelectFrom_EntityClass() {
            SqlBuilder builder = PSC.selectFrom(Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
        }

        @Test
        void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = PSC.selectFrom(Account.class, "a");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FROM account a"));
            assertTrue(sql.contains("a.first_name AS \"firstName\""));
        }

        @Test
        void testSelectFrom_TwoEntities() {
            SqlBuilder builder = PSC.selectFrom(Account.class, "a", "account", Account.class, "a2", "account2");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("a.first_name AS \"account.firstName\""));
            assertTrue(sql.contains("a2.first_name AS \"account2.firstName\""));
        }

        @Test
        void testUpdate_EntityClass() {
            SqlBuilder builder = PSC.update(Account.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
            // Should include updatable fields
            assertTrue(sql.contains("first_name"));
            assertFalse(sql.contains("last_name"));
            assertFalse(sql.contains("email"));
            // Should not include non-updatable fields
            assertFalse(sql.contains("created_date"));
        }

        @Test
        void testUpdate_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));
            SqlBuilder builder = PSC.update(Account.class, excludedProps);
            assertNotNull(builder);

            // Get the generated column names
            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("first_name"));
            assertFalse(sql.contains("email"));
        }

        @Test
        void testUpdate_TableName() {
            SqlBuilder builder = PSC.update("account");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        void testUpdate_TableNameWithEntityClass() {
            SqlBuilder builder = PSC.update("account", Account.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Table("account")
        public static class Account {
            @Id
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            @ReadOnly
            private Date createdDate;
            @NonUpdatable
            private String status;
            @Transient
            private String transientField;

            public Account() {
            }

            public Account(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
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

            public Date getCreatedDate() {
                return createdDate;
            }

            public void setCreatedDate(Date createdDate) {
                this.createdDate = createdDate;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public String getTransientField() {
                return transientField;
            }

            public void setTransientField(String transientField) {
                this.transientField = transientField;
            }
        }
    }

    @Nested
    class SCSBTest {
        @Test
        void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account("John", "john@email.com", "ACTIVE"), new Account("Jane", "jane@email.com", "ACTIVE"));

            SqlBuilder sb = Dsl.SCSB.batchInsert(accounts);
            assertNotNull(sb);

            String sql = sb.into("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testComplexQueryWithConditions() {
            SqlBuilder sb = Dsl.SCSB.select("firstName", "lastName")
                    .from("account")
                    .where(Filters.eq("status", "'ACTIVE'").and(Filters.gt("age", 18)))
                    .orderBy("lastName");

            assertNotNull(sb);
            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("WHERE"));
            assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        void testCountEntityClass() {
            SqlBuilder sb = Dsl.SCSB.selectCountFrom(Account.class);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("count(*)"));
        }

        @Test
        void testCountTableName() {
            SqlBuilder sb = Dsl.SCSB.selectCountFrom("account");
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("count(*)"));
        }

        @Test
        void testDeleteFromEntityClass() {
            SqlBuilder sb = Dsl.SCSB.deleteFrom(Account.class);
            assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().query();
            assertNotNull(sql);
        }

        @Test
        void testDeleteFromTableName() {
            SqlBuilder sb = Dsl.SCSB.deleteFrom("account");
            assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("DELETE FROM"));
            assertTrue(sql.contains("account"));
        }

        @Test
        void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.SCSB.deleteFrom("account", Account.class);
            assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().query();
            assertNotNull(sql);
        }

        @Test
        void testFromCondition_singleArg_equivalentToNullEntityClass() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.gt("balance", 1000));

            String viaSingleArg = Dsl.SCSB.renderCondition(cond).build().query();
            String viaNullClass = Dsl.SCSB.renderCondition(Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.gt("balance", 1000)), null).build().query();

            assertEquals(viaNullClass, viaSingleArg);
            assertTrue(viaSingleArg.contains("AND"));
        }

        @Test
        void testFromCondition_singleArg_nullCondition() {
            assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.renderCondition(null));
        }

        @Test
        void testInsertCollectionOfColumns() {
            List<String> columns = Arrays.asList("name", "email", "status");
            SqlBuilder sb = Dsl.SCSB.insert(columns);
            assertNotNull(sb);

            String sql = sb.into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        void testInsertEntity() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            SqlBuilder sb = Dsl.SCSB.insert(account);
            assertNotNull(sb);

            String sql = sb.into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        void testInsertEntityClass() {
            SqlBuilder sb = Dsl.SCSB.insert(Account.class);
            assertNotNull(sb);

            String sql = sb.into("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate", "id"));
            SqlBuilder sb = Dsl.SCSB.insert(Account.class, excluded);
            assertNotNull(sb);

            String sql = sb.into("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertEntityWithExclusions() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder sb = Dsl.SCSB.insert(account, excluded);
            assertNotNull(sb);

            String sql = sb.into("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testInsertInto() {
            SqlBuilder sb = Dsl.SCSB.insertInto(Account.class);
            assertNotNull(sb);

            // This should already have the table name set
            assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SqlBuilder sb = Dsl.SCSB.insertInto(Account.class, excluded);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        void testInsertMultipleColumns() {
            SqlBuilder sb = Dsl.SCSB.insert("name", "email", "status");
            assertNotNull(sb);

            String sql = sb.into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
            assertTrue(sql.contains("account"));
        }

        @Test
        void testInsertSingleColumn() {
            SqlBuilder sb = Dsl.SCSB.insert("name");
            assertNotNull(sb);
            // Verify the SQL builder is created properly
            assertDoesNotThrow(() -> sb.into("account").build().query());
        }

        @Test
        void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("name", "John");
            props.put("age", 25);

            SqlBuilder sb = Dsl.SCSB.insert(props);
            assertNotNull(sb);

            String sql = sb.into("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        void testParseCondition() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.gt("balance", 1000));

            SqlBuilder sb = Dsl.SCSB.renderCondition(cond, Account.class);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
            // Should contain the condition SQL
            assertTrue(sql.contains("AND"));
        }

        @Test
        void testParseNullCondition() {
            assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.renderCondition(null, Account.class));
        }

        @Test
        void testSelectCollectionOfColumns() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = Dsl.SCSB.select(columns);
            assertNotNull(sb);

            String sql = sb.from("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEmptyColumns() {
            // Empty column names should throw exception
            assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.select(new String[0]));

            assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.select(Collections.emptyMap()));
        }

        @Test
        void testSelectEmptyString() {
            // Empty select part should throw exception
            assertThrows(IllegalArgumentException.class, () -> Dsl.SCSB.select(""));
        }

        @Test
        void testSelectEntityClass() {
            SqlBuilder sb = Dsl.SCSB.select(Account.class);
            assertNotNull(sb);

            String sql = sb.from("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "salt"));
            SqlBuilder sb = Dsl.SCSB.select(Account.class, excluded);
            assertNotNull(sb);

            String sql = sb.from("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.SCSB.select(Account.class, true);
            assertNotNull(sb);

            String sql = sb.from("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.select(Account.class, true, excluded);
            assertNotNull(sb);

            String sql = sb.from("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClass() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", true, excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a");
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", true);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, true);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, true, excluded);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "b", "account2", null, false, null));

            SqlBuilder sb = Dsl.SCSB.selectFrom(selections);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromTwoEntities() {
            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", "account", Account.class, "b", "account2");
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = Dsl.SCSB.selectFrom(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            assertNotNull(sb);

            String sql = sb.build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectMultipleColumns() {
            SqlBuilder sb = Dsl.SCSB.select("firstName", "lastName", "email");
            assertNotNull(sb);

            String sql = sb.from("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }

        @Test
        void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(selection(Account.class, "a", "account", null, false, null),
                    selection(Account.class, "b", "account2", null, false, null));

            SqlBuilder sb = Dsl.SCSB.select(selections);
            assertNotNull(sb);

            String sql = sb.from("account a, account b").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectSingleExpression() {
            SqlBuilder sb = Dsl.SCSB.select("COUNT(*)");
            assertNotNull(sb);

            String sql = sb.from("account").build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        void testSelectTwoEntities() {
            SqlBuilder sb = Dsl.SCSB.select(Account.class, "a", "account", Account.class, "b", "account2");
            assertNotNull(sb);

            String sql = sb.from("account a, account b").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = Dsl.SCSB.select(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            assertNotNull(sb);

            String sql = sb.from("account a, account b").build().query();
            assertNotNull(sql);
        }

        @Test
        void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder sb = Dsl.SCSB.select(aliases);
            assertNotNull(sb);

            String sql = sb.from("account").build().query();
            assertNotNull(sql);
        }

        @Test
        void testUpdateEntityClass() {
            SqlBuilder sb = Dsl.SCSB.update(Account.class);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder sb = Dsl.SCSB.update(Account.class, excluded);
            assertNotNull(sb);

            assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        void testUpdateTableName() {
            SqlBuilder sb = Dsl.SCSB.update("account");
            assertNotNull(sb);

            String sql = sb.set("status", "ACTIVE").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
            assertTrue(sql.contains("UPDATE"));
            assertTrue(sql.contains("account"));
        }

        @Test
        void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = Dsl.SCSB.update("account", Account.class);
            assertNotNull(sb);

            String sql = sb.set("status", "ACTIVE").where(Filters.eq("id", 1)).build().query();
            assertNotNull(sql);
        }

        public static class Account {
            private Long id;
            private String firstName;
            private String lastName;
            private String email;
            private String status;
            private Date createdDate;

            public Account() {
            }

            public Account(String firstName, String lastName) {
                this.firstName = firstName;
                this.lastName = lastName;
            }

            public Account(String firstName, String email, String status) {
                this.firstName = firstName;
                this.email = email;
                this.status = status;
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
        }
    }

    @Nested
    class Validation {
        @Test
        void testAliasValidationRejectsNullMapsButPreservesEmptyMaps() {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Dsl.validateColumnAliases(null)).getMessage().contains(cs.propOrColumnNameAliases));
            Dsl.validateColumnAliases(Collections.emptyMap());
            Dsl.validateColumnAliases(Collections.singletonMap("name", "display_name"));
            assertThrows(IllegalArgumentException.class, () -> Dsl.validateColumnAliases(Collections.singletonMap("name", null)));
        }

        @Test
        void testDialectConstructionUsesTheSameValidationAsTheFactory() {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> new Dsl(null)).getMessage().contains(cs.sqlDialect));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Dsl.forDialect(null)).getMessage().contains(cs.sqlDialect));
        }

        // renderCondition documents the H2 rejection of a "//" line comment in a condition-layer GROUP BY / ORDER BY expression,
        // reached directly, through a Criteria, and through a structured sub-query; other dialects render the same conditions.
        @Test
        void testRenderConditionRejectsH2DoubleSlashSortKey() {
            final Dsl h2 = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("H2")).build());
            final Dsl postgres = Dsl.forDialect(PSC.sqlDialect().toBuilder().productInfo(SqlDialect.ProductInfo.of("PostgreSQL")).build());
            final Condition orderBy = Filters.orderBy("a // c", SortDirection.DESC, "b", SortDirection.ASC);
            final Condition criteria = com.landawn.abacus.query.condition.Criteria.builder().where(Filters.eq("x", 1)).groupBy("g // c").build();
            final Condition subQuery = Filters.in("id",
                    Filters.subQuery("u", List.of("id"), com.landawn.abacus.query.condition.Criteria.builder().orderBy("a // c").build()));

            assertEquals("SQL comment token is not allowed in ORDER BY expression: a // c DESC, b ASC",
                    assertThrows(IllegalArgumentException.class, () -> h2.renderCondition(orderBy)).getMessage());
            assertEquals("SQL comment token is not allowed in GROUP BY expression: g // c",
                    assertThrows(IllegalArgumentException.class, () -> h2.renderCondition(criteria, null)).getMessage());
            assertEquals("SQL comment token is not allowed in ORDER BY expression: a // c",
                    assertThrows(IllegalArgumentException.class, () -> h2.renderCondition(subQuery)).getMessage());

            assertEquals("ORDER BY a // c DESC, b ASC", postgres.renderCondition(orderBy).build().query());
            assertEquals("WHERE x = ? GROUP BY g // c", postgres.renderCondition(criteria, null).build().query());
            assertEquals("id IN (SELECT id FROM u ORDER BY a // c)", postgres.renderCondition(subQuery).build().query());
        }

        // The UPDATE/DELETE target table name is rejected by the factory call itself, before any clause is staged, so an
        // open block comment cannot swallow the SET/WHERE clauses rendered after it.
        @Test
        void testUpdateAndDeleteFromRejectUnterminatedBlockCommentInTableNameEagerly() {
            final String expected = "tableName must not contain an unterminated block comment: t /* x";

            for (final Factory factory : Factory.values()) {
                final Dsl dsl = factory.dsl;
                assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> dsl.update("t /* x")).getMessage(), factory.name());
                assertEquals(expected,
                        assertThrows(IllegalArgumentException.class, () -> dsl.update("t /* x", com.landawn.abacus.query.entity.Account.class)).getMessage(),
                        factory.name());
                assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> dsl.deleteFrom("t /* x")).getMessage(), factory.name());
                assertEquals(expected,
                        assertThrows(IllegalArgumentException.class, () -> dsl.deleteFrom("t /* x", com.landawn.abacus.query.entity.Account.class))
                                .getMessage(),
                        factory.name());
                assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> dsl.selectCountFrom("t /* x")).getMessage(), factory.name());
            }

            // The table name is checked before the column list, so it wins over a later comment-bearing SET column.
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> PSC.update("t /* x").set("a -- b")).getMessage());
            // Blank names keep their own message; terminated and quoted comments are accepted.
            assertTrue(assertThrows(IllegalArgumentException.class, () -> PSC.update(" ")).getMessage().contains("tableName"));
            assertEquals("UPDATE t /* x */ SET a = ?", PSC.update("t /* x */").set("a").build().query());
            assertEquals("DELETE FROM \"t/*x\"", PSC.deleteFrom("\"t/*x\"").build().query());
            assertEquals("DELETE FROM t", PSC.deleteFrom("t", com.landawn.abacus.query.entity.Account.class).build().query());
        }
    }

    enum Factory {
        SCSB(Dsl.SCSB),
        ACSB(Dsl.ACSB),
        LCSB(Dsl.LCSB),
        PSB(Dsl.PSB),
        PSC(Dsl.PSC),
        PAC(Dsl.PAC),
        PLC(Dsl.PLC),
        NSB(Dsl.NSB),
        NSC(Dsl.NSC),
        NAC(Dsl.NAC),
        NLC(Dsl.NLC),
        MSB(Dsl.MSB),
        MSC(Dsl.MSC),
        MAC(Dsl.MAC),
        MLC(Dsl.MLC);

        final Dsl dsl;

        Factory(final Dsl dsl) {
            this.dsl = dsl;
        }
    }

    /** Throws {@code failure}, even a checked exception, without declaring it (as Kotlin or Lombok {@code @SneakyThrows} code can). */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(final Throwable failure) throws E {
        throw (E) failure;
    }

    @lombok.Data
    static class UnsupportedDateEntity {
        private long id;
        @com.landawn.abacus.annotation.JsonXmlField(dateFormat = "long")
        private java.time.LocalDate eventDate;
    }
}
