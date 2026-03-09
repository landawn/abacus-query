package com.landawn.abacus.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SqlBuilder.ACSB;
import com.landawn.abacus.query.SqlBuilder.LCSB;
import com.landawn.abacus.query.SqlBuilder.SCSB;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.util.N;

/**
 * Unit tests for SqlBuilder class
 */
public class SqlBuilder11Test extends TestBase {

    @BeforeEach
    public void setUp() {
        // Reset handler to default before each test
        AbstractQueryBuilder.resetHandlerForNamedParameter();
    }

    @Test
    public void testSetHandlerForNamedParameter() {
        // Test custom handler
        BiConsumer<StringBuilder, String> customHandler = (sb, propName) -> sb.append("#{").append(propName).append("}");

        AbstractQueryBuilder.setHandlerForNamedParameter(customHandler);
        // The handler is stored in ThreadLocal, so we can't directly verify it
        // But we can verify it doesn't throw exception
        Assertions.assertDoesNotThrow(() -> AbstractQueryBuilder.setHandlerForNamedParameter(customHandler));

        // Test null handler throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.setHandlerForNamedParameter(null));
    }

    @Test
    public void testResetHandlerForNamedParameter() {
        // Set custom handler first
        BiConsumer<StringBuilder, String> customHandler = (sb, propName) -> sb.append("#{").append(propName).append("}");
        AbstractQueryBuilder.setHandlerForNamedParameter(customHandler);

        // Reset to default
        Assertions.assertDoesNotThrow(() -> AbstractQueryBuilder.resetHandlerForNamedParameter());
    }

    @Nested
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

    @Nested
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

    @Nested
    public class SCSBTest {

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

        @Test
        public void testInsertSingleColumn() {
            SqlBuilder sb = SCSB.insert("name");
            Assertions.assertNotNull(sb);
            // Verify the SQL builder is created properly
            Assertions.assertDoesNotThrow(() -> sb.into("account").build().sql());
        }

        @Test
        public void testInsertMultipleColumns() {
            SqlBuilder sb = SCSB.insert("name", "email", "status");
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            List<String> columns = Arrays.asList("name", "email", "status");
            SqlBuilder sb = SCSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("name", "John");
            props.put("age", 25);

            SqlBuilder sb = SCSB.insert(props);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertEntity() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            SqlBuilder sb = SCSB.insert(account);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertEntityWithExclusions() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder sb = SCSB.insert(account, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SqlBuilder sb = SCSB.insert(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate", "id"));
            SqlBuilder sb = SCSB.insert(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SqlBuilder sb = SCSB.insertInto(Account.class);
            Assertions.assertNotNull(sb);

            // This should already have the table name set
            Assertions.assertDoesNotThrow(() -> sb.build().sql());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SqlBuilder sb = SCSB.insertInto(Account.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().sql());
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account("John", "john@email.com", "ACTIVE"), new Account("Jane", "jane@email.com", "ACTIVE"));

            SqlBuilder sb = SCSB.batchInsert(accounts);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SqlBuilder sb = SCSB.update("account");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(Filters.eq("id", 1)).build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = SCSB.update("account", Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(Filters.eq("id", 1)).build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SqlBuilder sb = SCSB.update(Account.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().sql());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder sb = SCSB.update(Account.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().sql());
        }

        @Test
        public void testDeleteFromTableName() {
            SqlBuilder sb = SCSB.deleteFrom("account");
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = SCSB.deleteFrom("account", Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SqlBuilder sb = SCSB.deleteFrom(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SqlBuilder sb = SCSB.select("COUNT(*)");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SqlBuilder sb = SCSB.select("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectCollectionOfColumns() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = SCSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder sb = SCSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SqlBuilder sb = SCSB.select(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = SCSB.select(Account.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "salt"));
            SqlBuilder sb = SCSB.select(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.select(Account.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClass() {
            SqlBuilder sb = SCSB.selectFrom(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a");
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = SCSB.selectFrom(Account.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.selectFrom(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.selectFrom(Account.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SqlBuilder sb = SCSB.select(Account.class, "a", "account", Account.class, "b", "account2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = SCSB.select(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "b", "account2", null, false, null));

            SqlBuilder sb = SCSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", "account", Account.class, "b", "account2");
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "b", "account2", null, false, null));

            SqlBuilder sb = SCSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SqlBuilder sb = SCSB.count("account");
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SqlBuilder sb = SCSB.count(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.gt("balance", 1000));

            SqlBuilder sb = SCSB.parse(cond, Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            // Should contain the condition SQL
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testParseNullCondition() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> SCSB.parse(null, Account.class));
        }

        @Test
        public void testSelectEmptyColumns() {
            // Empty column names should throw exception
            Assertions.assertThrows(IllegalArgumentException.class, () -> SCSB.select(new String[0]));

            Assertions.assertThrows(IllegalArgumentException.class, () -> SCSB.select(Collections.emptyMap()));
        }

        @Test
        public void testSelectEmptyString() {
            // Empty select part should throw exception
            Assertions.assertThrows(IllegalArgumentException.class, () -> SCSB.select(""));
        }

        @Test
        public void testComplexQueryWithConditions() {
            SqlBuilder sb = SCSB.select("firstName", "lastName")
                    .from("account")
                    .where(Filters.eq("status", "'ACTIVE'").and(Filters.gt("age", 18)))
                    .orderBy("lastName");

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
        }
    }

    @Nested
    public class ACSBTest {

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

        @Test
        public void testInsertSingleColumn() {
            SqlBuilder sb = ACSB.insert("name");
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertMultipleColumns() {
            SqlBuilder sb = ACSB.insert("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = ACSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("age", 30);

            SqlBuilder sb = ACSB.insert(props);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntity() {
            User user = new User("John", "Doe");
            user.setEmail("john@example.com");

            SqlBuilder sb = ACSB.insert(user);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityWithExclusions() {
            User user = new User("John", "Doe");
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));

            SqlBuilder sb = ACSB.insert(user, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SqlBuilder sb = ACSB.insert(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder sb = ACSB.insert(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SqlBuilder sb = ACSB.insertInto(User.class);
            Assertions.assertNotNull(sb);

            // Should be able to get SQL directly as table name is already set
            Assertions.assertDoesNotThrow(() -> sb.build().sql());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SqlBuilder sb = ACSB.insertInto(User.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().sql());
        }

        @Test
        public void testBatchInsert() {
            List<User> users = Arrays.asList(new User("John", "Doe"), new User("Jane", "Smith"));

            SqlBuilder sb = ACSB.batchInsert(users);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> propsList = new ArrayList<>();
            Map<String, Object> user1 = new HashMap<>();
            user1.put("firstName", "John");
            user1.put("lastName", "Doe");
            propsList.add(user1);

            Map<String, Object> user2 = new HashMap<>();
            user2.put("firstName", "Jane");
            user2.put("lastName", "Smith");
            propsList.add(user2);

            SqlBuilder sb = ACSB.batchInsert(propsList);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SqlBuilder sb = ACSB.update("users");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(Filters.eq("id", 1)).build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = ACSB.update("users", User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status").where(Filters.eq("id", 1)).build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SqlBuilder sb = ACSB.update(User.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().sql());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder sb = ACSB.update(User.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().sql());
        }

        @Test
        public void testDeleteFromTableName() {
            SqlBuilder sb = ACSB.deleteFrom("users");
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = ACSB.deleteFrom("users", User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SqlBuilder sb = ACSB.deleteFrom(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SqlBuilder sb = ACSB.select("COUNT(DISTINCT userId)");
            Assertions.assertNotNull(sb);

            String sql = sb.from("orders").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("COUNT(DISTINCT"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SqlBuilder sb = ACSB.select("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = ACSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("email", "emailAddress");

            SqlBuilder sb = ACSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SqlBuilder sb = ACSB.select(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = ACSB.select(User.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "secretKey"));
            SqlBuilder sb = ACSB.select(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.select(User.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClass() {
            SqlBuilder sb = ACSB.selectFrom(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = ACSB.selectFrom(User.class, "u");
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = ACSB.selectFrom(User.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = ACSB.selectFrom(User.class, "u", true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.selectFrom(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.selectFrom(User.class, "u", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.selectFrom(User.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.selectFrom(User.class, "u", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SqlBuilder sb = ACSB.select(User.class, "u1", "user1", User.class, "u2", "user2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SqlBuilder sb = ACSB.select(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u1", "user1", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder sb = ACSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SqlBuilder sb = ACSB.selectFrom(User.class, "u1", "user1", User.class, "u2", "user2");
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SqlBuilder sb = ACSB.selectFrom(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u1", "user1", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder sb = ACSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SqlBuilder sb = ACSB.count("users");
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SqlBuilder sb = ACSB.count(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = Filters.or(Filters.eq("status", "'ACTIVE'"), Filters.eq("status", "'PENDING'"));

            SqlBuilder sb = ACSB.parse(cond, User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
        }

        @Test
        public void testParseNullCondition() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> ACSB.parse(null, User.class));
        }

        @Test
        public void testEmptySelectParts() {
            // Test empty string
            Assertions.assertThrows(IllegalArgumentException.class, () -> ACSB.select(""));

            // Test empty array
            Assertions.assertThrows(IllegalArgumentException.class, () -> ACSB.select(new String[0]));

            // Test empty map
            Assertions.assertThrows(IllegalArgumentException.class, () -> ACSB.select(Collections.emptyMap()));
        }

        @Test
        public void testComplexQuery() {
            SqlBuilder sb = ACSB.select("u.firstName", "u.lastName", "COUNT(o.id)")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.userId")
                    .where(Filters.eq("u.status", "'ACTIVE'"))
                    .groupBy("u.id", "u.firstName", "u.lastName")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("u.lastName");

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
        }
    }

    @Nested
    public class LCSBTest extends TestBase {

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

        @Test
        public void testInsertSingleColumn() {
            SqlBuilder sb = LCSB.insert("customerName");
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertMultipleColumns() {
            SqlBuilder sb = LCSB.insert("firstName", "lastName", "emailAddress");
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "emailAddress", "phoneNumber");
            SqlBuilder sb = LCSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "Alice");
            props.put("lastName", "Johnson");
            props.put("emailAddress", "alice@example.com");

            SqlBuilder sb = LCSB.insert(props);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntity() {
            Customer customer = new Customer("Bob", "Smith", "bob@example.com");
            customer.setPhoneNumber("123-456-7890");

            SqlBuilder sb = LCSB.insert(customer);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityWithExclusions() {
            Customer customer = new Customer("Carol", "Davis");
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));

            SqlBuilder sb = LCSB.insert(customer, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SqlBuilder sb = LCSB.insert(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SqlBuilder sb = LCSB.insert(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SqlBuilder sb = LCSB.insertInto(Customer.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().sql());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId"));
            SqlBuilder sb = LCSB.insertInto(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().sql());
        }

        @Test
        public void testBatchInsert() {
            List<Customer> customers = Arrays.asList(new Customer("Dave", "Wilson", "dave@example.com"), new Customer("Eve", "Brown", "eve@example.com"));

            SqlBuilder sb = LCSB.batchInsert(customers);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> propsList = new ArrayList<>();

            Map<String, Object> customer1 = new HashMap<>();
            customer1.put("firstName", "Frank");
            customer1.put("lastName", "Miller");
            propsList.add(customer1);

            Map<String, Object> customer2 = new HashMap<>();
            customer2.put("firstName", "Grace");
            customer2.put("lastName", "Lee");
            propsList.add(customer2);

            SqlBuilder sb = LCSB.batchInsert(propsList);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SqlBuilder sb = LCSB.update("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'PREMIUM'").where(Filters.gt("totalPurchases", 1000)).build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = LCSB.update("customers", Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status").where(Filters.eq("customerId", 100)).build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SqlBuilder sb = LCSB.update(Customer.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("customerId", 100)).build().sql());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SqlBuilder sb = LCSB.update(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("customerId", 100)).build().sql());
        }

        @Test
        public void testDeleteFromTableName() {
            SqlBuilder sb = LCSB.deleteFrom("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = LCSB.deleteFrom("customers", Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.lt("lastLoginDate", "2020-01-01")).build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SqlBuilder sb = LCSB.deleteFrom(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.isNull("emailAddress")).build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SqlBuilder sb = LCSB.select("COUNT(*) as totalCount");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("totalCount"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SqlBuilder sb = LCSB.select("firstName", "lastName", "emailAddress");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("customerId", "firstName", "lastName");
            SqlBuilder sb = LCSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");

            SqlBuilder sb = LCSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SqlBuilder sb = LCSB.select(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = LCSB.select(Customer.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber", "registrationDate"));
            SqlBuilder sb = LCSB.select(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.select(Customer.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.selectFrom(Customer.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SqlBuilder sb = LCSB.select(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder sb = LCSB.select(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Customer.class, "c1", "customer1", null, false, null),
                    new Selection(Customer.class, "c2", "customer2", null, false, null), new Selection(Customer.class, "c3", "customer3", null, false, null));

            SqlBuilder sb = LCSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2, customers c3").build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Customer.class, "c1", "customer1", null, false, null),
                    new Selection(Customer.class, "c2", "customer2", null, false, null));

            SqlBuilder sb = LCSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SqlBuilder sb = LCSB.count("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SqlBuilder sb = LCSB.count(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.between("registrationDate", "2020-01-01", "2023-12-31"));

            SqlBuilder sb = LCSB.parse(cond, Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("BETWEEN"));
        }

        @Test
        public void testParseComplexCondition() {
            Condition cond = Filters.or(Filters.and(Filters.eq("status", "'PREMIUM'"), Filters.gt("totalPurchases", 5000)),
                    Filters.and(Filters.eq("status", "'GOLD'"), Filters.gt("totalPurchases", 3000)));

            SqlBuilder sb = LCSB.parse(cond, Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testParseNullCondition() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> LCSB.parse(null, Customer.class));
        }

        @Test
        public void testEmptySelectParts() {
            // Test empty string
            Assertions.assertThrows(IllegalArgumentException.class, () -> LCSB.select(""));

            // Test null string
            Assertions.assertThrows(IllegalArgumentException.class, () -> LCSB.select((String) null));

            // Test empty array
            Assertions.assertThrows(IllegalArgumentException.class, () -> LCSB.select(new String[0]));

            // Test empty collection
            Assertions.assertThrows(IllegalArgumentException.class, () -> LCSB.select(Collections.<String> emptyList()));

            // Test empty map
            Assertions.assertThrows(IllegalArgumentException.class, () -> LCSB.select(Collections.emptyMap()));
        }

        @Test
        public void testEmptyMultiSelects() {
            // Test null selections list
            Assertions.assertThrows(RuntimeException.class, () -> LCSB.select((List<Selection>) null));

            // Test empty selections list
            Assertions.assertThrows(RuntimeException.class, () -> LCSB.select(Collections.<String> emptyList()));
        }

        @Test
        public void testComplexQueryWithJoins() {
            SqlBuilder sb = LCSB.select("c.firstName", "c.lastName", "SUM(o.amount)")
                    .from("customers c")
                    .innerJoin("orders o")
                    .on("c.customerId = o.customerId")
                    .where(Filters.eq("c.status", "'ACTIVE'"))
                    .groupBy("c.customerId", "c.firstName", "c.lastName")
                    .having(Filters.gt("SUM(o.amount)", 10000))
                    .orderBy("SUM(o.amount) DESC");

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INNER JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        public void testQueryWithSubquery() {
            SubQuery subquery = Filters.subQuery("orders", N.asList("customerId"), Filters.gt("amount", 1000));

            SqlBuilder sb = LCSB.select("*").from("customers").where(Filters.in("customerId", subquery));

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("IN"));
        }

        @Test
        public void testUpdateWithMultipleSet() {
            SqlBuilder sb = LCSB.update("customers")
                    .set("status", "'INACTIVE'")
                    .set("lastModified", "CURRENT_TIMESTAMP")
                    .where(Filters.lt("lastLoginDate", "2020-01-01"));

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
            Assertions.assertTrue(sql.contains("SET"));
        }

        @Test
        public void testSelectDistinct() {
            SqlBuilder sb = LCSB.select("DISTINCT status").from("customers").orderBy("status");

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testQueryWithLimit() {
            SqlBuilder sb = LCSB.select("*").from("customers").where(Filters.eq("status", "'ACTIVE'")).orderBy("registrationDate DESC").limit(10);

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testQueryWithLimitAndOffset() {
            SqlBuilder sb = LCSB.select("*").from("customers").where(Filters.eq("status", "'ACTIVE'")).orderBy("customerId").limit(20, 10);

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUnionQuery() {
            SqlBuilder query1 = LCSB.select("firstName", "lastName").from("customers").where(Filters.eq("status", "'ACTIVE'"));

            SqlBuilder query2 = LCSB.select("firstName", "lastName").from("employees").where(Filters.eq("department", "'SALES'"));

            SqlBuilder sb = query1.union(query2);

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UNION"));
        }

        @Test
        public void testCaseWhenExpression() {
            String caseExpr = "CASE WHEN status = 'PREMIUM' THEN 'VIP' " + "WHEN status = 'GOLD' THEN 'Important' " + "ELSE 'Regular' END AS customerType";

            SqlBuilder sb = LCSB.select("firstName", "lastName", caseExpr).from("customers");

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("case when"));
        }

        @Test
        public void testAggregateFunctions() {
            SqlBuilder sb = LCSB.select("status", "COUNT(*) as count", "AVG(totalPurchases) as avgPurchases", "MAX(lastLoginDate) as lastActive")
                    .from("customers")
                    .groupBy("status")
                    .having(Filters.gt("COUNT(*)", 10));

            Assertions.assertNotNull(sb);
            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT"));
            Assertions.assertTrue(sql.contains("AVG"));
            Assertions.assertTrue(sql.contains("MAX"));
        }

        @Test
        public void testCreateInstance() {
            // Test that createInstance returns a new LCSB instance
            LCSB instance = LCSB.createInstance();
            Assertions.assertNotNull(instance);
            Assertions.assertTrue(instance instanceof LCSB);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c");
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = LCSB.selectFrom(Customer.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c", true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.selectFrom(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().sql();
            Assertions.assertNotNull(sql);
        }
    }

}