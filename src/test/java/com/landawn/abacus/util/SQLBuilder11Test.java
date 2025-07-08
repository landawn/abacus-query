package com.landawn.abacus.util;

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
import com.landawn.abacus.condition.Condition;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.condition.SubQuery;
import com.landawn.abacus.util.SQLBuilder.ACSB;
import com.landawn.abacus.util.SQLBuilder.LCSB;
import com.landawn.abacus.util.SQLBuilder.SCSB;

/**
 * Unit tests for SQLBuilder class
 */
public class SQLBuilder11Test extends TestBase {

    @BeforeEach
    public void setUp() {
        // Reset handler to default before each test
        SQLBuilder.resetHandlerForNamedParameter();
    }

    @Test
    public void testSetHandlerForNamedParameter() {
        // Test custom handler
        BiConsumer<StringBuilder, String> customHandler = (sb, propName) -> sb.append("#{").append(propName).append("}");

        SQLBuilder.setHandlerForNamedParameter(customHandler);
        // The handler is stored in ThreadLocal, so we can't directly verify it
        // But we can verify it doesn't throw exception
        Assertions.assertDoesNotThrow(() -> SQLBuilder.setHandlerForNamedParameter(customHandler));

        // Test null handler throws exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> SQLBuilder.setHandlerForNamedParameter(null));
    }

    @Test
    public void testResetHandlerForNamedParameter() {
        // Set custom handler first
        BiConsumer<StringBuilder, String> customHandler = (sb, propName) -> sb.append("#{").append(propName).append("}");
        SQLBuilder.setHandlerForNamedParameter(customHandler);

        // Reset to default
        Assertions.assertDoesNotThrow(() -> SQLBuilder.resetHandlerForNamedParameter());
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
            SQLBuilder sb = SCSB.insert("name");
            Assertions.assertNotNull(sb);
            // Verify the SQL builder is created properly
            Assertions.assertDoesNotThrow(() -> sb.into("account").sql());
        }

        @Test
        public void testInsertMultipleColumns() {
            SQLBuilder sb = SCSB.insert("name", "email", "status");
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            List<String> columns = Arrays.asList("name", "email", "status");
            SQLBuilder sb = SCSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("name", "John");
            props.put("age", 25);

            SQLBuilder sb = SCSB.insert(props);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertEntity() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            SQLBuilder sb = SCSB.insert(account);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertEntityWithExclusions() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));

            SQLBuilder sb = SCSB.insert(account, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SQLBuilder sb = SCSB.insert(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate", "id"));
            SQLBuilder sb = SCSB.insert(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SQLBuilder sb = SCSB.insertInto(Account.class);
            Assertions.assertNotNull(sb);

            // This should already have the table name set
            Assertions.assertDoesNotThrow(() -> sb.sql());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SQLBuilder sb = SCSB.insertInto(Account.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.sql());
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account("John", "john@email.com", "ACTIVE"), new Account("Jane", "jane@email.com", "ACTIVE"));

            SQLBuilder sb = SCSB.batchInsert(accounts);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SQLBuilder sb = SCSB.update("account");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(CF.eq("id", 1)).sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SQLBuilder sb = SCSB.update("account", Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(CF.eq("id", 1)).sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SQLBuilder sb = SCSB.update(Account.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(CF.eq("id", 1)).sql());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));
            SQLBuilder sb = SCSB.update(Account.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(CF.eq("id", 1)).sql());
        }

        @Test
        public void testDeleteFromTableName() {
            SQLBuilder sb = SCSB.deleteFrom("account");
            Assertions.assertNotNull(sb);

            String sql = sb.where(CF.eq("status", "'DELETED'")).sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SQLBuilder sb = SCSB.deleteFrom("account", Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(CF.eq("status", "'DELETED'")).sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SQLBuilder sb = SCSB.deleteFrom(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(CF.eq("status", "'DELETED'")).sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SQLBuilder sb = SCSB.select("COUNT(*)");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SQLBuilder sb = SCSB.select("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectCollectionOfColumns() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SQLBuilder sb = SCSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SQLBuilder sb = SCSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SQLBuilder sb = SCSB.select(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SQLBuilder sb = SCSB.select(Account.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "salt"));
            SQLBuilder sb = SCSB.select(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = SCSB.select(Account.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClass() {
            SQLBuilder sb = SCSB.selectFrom(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SQLBuilder sb = SCSB.selectFrom(Account.class, "a");
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SQLBuilder sb = SCSB.selectFrom(Account.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SQLBuilder sb = SCSB.selectFrom(Account.class, "a", true);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = SCSB.selectFrom(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = SCSB.selectFrom(Account.class, "a", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = SCSB.selectFrom(Account.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = SCSB.selectFrom(Account.class, "a", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SQLBuilder sb = SCSB.select(Account.class, "a", "account", Account.class, "b", "account2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SQLBuilder sb = SCSB.select(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "b", "account2", null, false, null));

            SQLBuilder sb = SCSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SQLBuilder sb = SCSB.selectFrom(Account.class, "a", "account", Account.class, "b", "account2");
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SQLBuilder sb = SCSB.selectFrom(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "b", "account2", null, false, null));

            SQLBuilder sb = SCSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SQLBuilder sb = SCSB.count("account");
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testCountTableNameWithEntityClass() {
            SQLBuilder sb = SCSB.count("account", Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SQLBuilder sb = SCSB.count(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = CF.and(CF.eq("status", "'ACTIVE'"), CF.gt("balance", 1000));

            SQLBuilder sb = SCSB.parse(cond, Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
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
            SQLBuilder sb = SCSB.select("firstName", "lastName").from("account").where(CF.eq("status", "'ACTIVE'").and(CF.gt("age", 18))).orderBy("lastName");

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
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
            SQLBuilder sb = ACSB.insert("name");
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertMultipleColumns() {
            SQLBuilder sb = ACSB.insert("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SQLBuilder sb = ACSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("age", 30);

            SQLBuilder sb = ACSB.insert(props);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntity() {
            User user = new User("John", "Doe");
            user.setEmail("john@example.com");

            SQLBuilder sb = ACSB.insert(user);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityWithExclusions() {
            User user = new User("John", "Doe");
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));

            SQLBuilder sb = ACSB.insert(user, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SQLBuilder sb = ACSB.insert(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SQLBuilder sb = ACSB.insert(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SQLBuilder sb = ACSB.insertInto(User.class);
            Assertions.assertNotNull(sb);

            // Should be able to get SQL directly as table name is already set
            Assertions.assertDoesNotThrow(() -> sb.sql());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SQLBuilder sb = ACSB.insertInto(User.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.sql());
        }

        @Test
        public void testBatchInsert() {
            List<User> users = Arrays.asList(new User("John", "Doe"), new User("Jane", "Smith"));

            SQLBuilder sb = ACSB.batchInsert(users);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
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

            SQLBuilder sb = ACSB.batchInsert(propsList);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SQLBuilder sb = ACSB.update("users");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(CF.eq("id", 1)).sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SQLBuilder sb = ACSB.update("users", User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status").where(CF.eq("id", 1)).sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SQLBuilder sb = ACSB.update(User.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(CF.eq("id", 1)).sql());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SQLBuilder sb = ACSB.update(User.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(CF.eq("id", 1)).sql());
        }

        @Test
        public void testDeleteFromTableName() {
            SQLBuilder sb = ACSB.deleteFrom("users");
            Assertions.assertNotNull(sb);

            String sql = sb.where(CF.eq("status", "'INACTIVE'")).sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SQLBuilder sb = ACSB.deleteFrom("users", User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(CF.eq("status", "'INACTIVE'")).sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SQLBuilder sb = ACSB.deleteFrom(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(CF.eq("status", "'INACTIVE'")).sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SQLBuilder sb = ACSB.select("COUNT(DISTINCT userId)");
            Assertions.assertNotNull(sb);

            String sql = sb.from("orders").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("COUNT(DISTINCT"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SQLBuilder sb = ACSB.select("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SQLBuilder sb = ACSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("email", "emailAddress");

            SQLBuilder sb = ACSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SQLBuilder sb = ACSB.select(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SQLBuilder sb = ACSB.select(User.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "secretKey"));
            SQLBuilder sb = ACSB.select(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = ACSB.select(User.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClass() {
            SQLBuilder sb = ACSB.selectFrom(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SQLBuilder sb = ACSB.selectFrom(User.class, "u");
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SQLBuilder sb = ACSB.selectFrom(User.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SQLBuilder sb = ACSB.selectFrom(User.class, "u", true);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = ACSB.selectFrom(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = ACSB.selectFrom(User.class, "u", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = ACSB.selectFrom(User.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SQLBuilder sb = ACSB.selectFrom(User.class, "u", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SQLBuilder sb = ACSB.select(User.class, "u1", "user1", User.class, "u2", "user2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SQLBuilder sb = ACSB.select(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u1", "user1", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SQLBuilder sb = ACSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SQLBuilder sb = ACSB.selectFrom(User.class, "u1", "user1", User.class, "u2", "user2");
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SQLBuilder sb = ACSB.selectFrom(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u1", "user1", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SQLBuilder sb = ACSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SQLBuilder sb = ACSB.count("users");
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testCountTableNameWithEntityClass() {
            SQLBuilder sb = ACSB.count("users", User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SQLBuilder sb = ACSB.count(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = CF.or(CF.eq("status", "'ACTIVE'"), CF.eq("status", "'PENDING'"));

            SQLBuilder sb = ACSB.parse(cond, User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
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
            SQLBuilder sb = ACSB.select("u.firstName", "u.lastName", "COUNT(o.id)")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.userId")
                    .where(CF.eq("u.status", "'ACTIVE'"))
                    .groupBy("u.id", "u.firstName", "u.lastName")
                    .having(CF.gt("COUNT(o.id)", 5))
                    .orderBy("u.lastName");

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
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
            SQLBuilder sb = LCSB.insert("customerName");
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertMultipleColumns() {
            SQLBuilder sb = LCSB.insert("firstName", "lastName", "emailAddress");
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "emailAddress", "phoneNumber");
            SQLBuilder sb = LCSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "Alice");
            props.put("lastName", "Johnson");
            props.put("emailAddress", "alice@example.com");

            SQLBuilder sb = LCSB.insert(props);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntity() {
            Customer customer = new Customer("Bob", "Smith", "bob@example.com");
            customer.setPhoneNumber("123-456-7890");

            SQLBuilder sb = LCSB.insert(customer);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityWithExclusions() {
            Customer customer = new Customer("Carol", "Davis");
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));

            SQLBuilder sb = LCSB.insert(customer, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SQLBuilder sb = LCSB.insert(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SQLBuilder sb = LCSB.insert(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SQLBuilder sb = LCSB.insertInto(Customer.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.sql());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId"));
            SQLBuilder sb = LCSB.insertInto(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.sql());
        }

        @Test
        public void testBatchInsert() {
            List<Customer> customers = Arrays.asList(new Customer("Dave", "Wilson", "dave@example.com"), new Customer("Eve", "Brown", "eve@example.com"));

            SQLBuilder sb = LCSB.batchInsert(customers);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
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

            SQLBuilder sb = LCSB.batchInsert(propsList);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SQLBuilder sb = LCSB.update("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'PREMIUM'").where(CF.gt("totalPurchases", 1000)).sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SQLBuilder sb = LCSB.update("customers", Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status").where(CF.eq("customerId", 100)).sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SQLBuilder sb = LCSB.update(Customer.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(CF.eq("customerId", 100)).sql());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SQLBuilder sb = LCSB.update(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(CF.eq("customerId", 100)).sql());
        }

        @Test
        public void testDeleteFromTableName() {
            SQLBuilder sb = LCSB.deleteFrom("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.where(CF.eq("status", "'INACTIVE'")).sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SQLBuilder sb = LCSB.deleteFrom("customers", Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(CF.lt("lastLoginDate", "2020-01-01")).sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SQLBuilder sb = LCSB.deleteFrom(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(CF.isNull("emailAddress")).sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SQLBuilder sb = LCSB.select("COUNT(*) as totalCount");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("totalCount"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SQLBuilder sb = LCSB.select("firstName", "lastName", "emailAddress");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("customerId", "firstName", "lastName");
            SQLBuilder sb = LCSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");

            SQLBuilder sb = LCSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SQLBuilder sb = LCSB.select(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SQLBuilder sb = LCSB.select(Customer.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber", "registrationDate"));
            SQLBuilder sb = LCSB.select(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SQLBuilder sb = LCSB.select(Customer.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SQLBuilder sb = LCSB.selectFrom(Customer.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SQLBuilder sb = LCSB.selectFrom(Customer.class, "c", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SQLBuilder sb = LCSB.select(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("emailAddress"));

            SQLBuilder sb = LCSB.select(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Customer.class, "c1", "customer1", null, false, null),
                    new Selection(Customer.class, "c2", "customer2", null, false, null), new Selection(Customer.class, "c3", "customer3", null, false, null));

            SQLBuilder sb = LCSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2, customers c3").sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SQLBuilder sb = LCSB.selectFrom(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SQLBuilder sb = LCSB.selectFrom(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Customer.class, "c1", "customer1", null, false, null),
                    new Selection(Customer.class, "c2", "customer2", null, false, null));

            SQLBuilder sb = LCSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SQLBuilder sb = LCSB.count("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testCountTableNameWithEntityClass() {
            SQLBuilder sb = LCSB.count("customers", Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SQLBuilder sb = LCSB.count(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = CF.and(CF.eq("status", "'ACTIVE'"), CF.between("registrationDate", "2020-01-01", "2023-12-31"));

            SQLBuilder sb = LCSB.parse(cond, Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("BETWEEN"));
        }

        @Test
        public void testParseComplexCondition() {
            Condition cond = CF.or(CF.and(CF.eq("status", "'PREMIUM'"), CF.gt("totalPurchases", 5000)),
                    CF.and(CF.eq("status", "'GOLD'"), CF.gt("totalPurchases", 3000)));

            SQLBuilder sb = LCSB.parse(cond, Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
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
            SQLBuilder sb = LCSB.select("c.firstName", "c.lastName", "SUM(o.amount)")
                    .from("customers c")
                    .innerJoin("orders o")
                    .on("c.customerId = o.customerId")
                    .where(CF.eq("c.status", "'ACTIVE'"))
                    .groupBy("c.customerId", "c.firstName", "c.lastName")
                    .having(CF.gt("SUM(o.amount)", 10000))
                    .orderBy("SUM(o.amount) DESC");

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INNER JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
        }

        @Test
        public void testQueryWithSubquery() {
            SubQuery subquery = CF.subQuery("orders", N.asList("customerId"), CF.gt("amount", 1000));

            SQLBuilder sb = LCSB.select("*").from("customers").where(CF.in("customerId", subquery));

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("IN"));
        }

        @Test
        public void testUpdateWithMultipleSet() {
            SQLBuilder sb = LCSB.update("customers")
                    .set("status", "'INACTIVE'")
                    .set("lastModified", "CURRENT_TIMESTAMP")
                    .where(CF.lt("lastLoginDate", "2020-01-01"));

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
            Assertions.assertTrue(sql.contains("SET"));
        }


        @Test
        public void testSelectDistinct() {
            SQLBuilder sb = LCSB.select("DISTINCT status").from("customers").orderBy("status");

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testQueryWithLimit() {
            SQLBuilder sb = LCSB.select("*").from("customers").where(CF.eq("status", "'ACTIVE'")).orderBy("registrationDate DESC").limit(10);

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testQueryWithLimitAndOffset() {
            SQLBuilder sb = LCSB.select("*").from("customers").where(CF.eq("status", "'ACTIVE'")).orderBy("customerId").limit(10, 20);

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUnionQuery() {
            SQLBuilder query1 = LCSB.select("firstName", "lastName").from("customers").where(CF.eq("status", "'ACTIVE'"));

            SQLBuilder query2 = LCSB.select("firstName", "lastName").from("employees").where(CF.eq("department", "'SALES'"));

            SQLBuilder sb = query1.union(query2);

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UNION"));
        }


        @Test
        public void testCaseWhenExpression() {
            String caseExpr = "CASE WHEN status = 'PREMIUM' THEN 'VIP' " + "WHEN status = 'GOLD' THEN 'Important' " + "ELSE 'Regular' END AS customerType";

            SQLBuilder sb = LCSB.select("firstName", "lastName", caseExpr).from("customers");

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("CASE"));
        }

        @Test
        public void testAggregateFunctions() {
            SQLBuilder sb = LCSB.select("status", "COUNT(*) as count", "AVG(totalPurchases) as avgPurchases", "MAX(lastLoginDate) as lastActive")
                    .from("customers")
                    .groupBy("status")
                    .having(CF.gt("COUNT(*)", 10));

            Assertions.assertNotNull(sb);
            String sql = sb.sql();
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
            SQLBuilder sb = LCSB.selectFrom(Customer.class, "c");
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SQLBuilder sb = LCSB.selectFrom(Customer.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SQLBuilder sb = LCSB.selectFrom(Customer.class, "c", true);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SQLBuilder sb = LCSB.selectFrom(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SQLBuilder sb = LCSB.selectFrom(Customer.class, "c", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.sql();
            Assertions.assertNotNull(sql);
        }
    }

}