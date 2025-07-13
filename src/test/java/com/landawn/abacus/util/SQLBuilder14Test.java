package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.SQLBuilder.MAC;
import com.landawn.abacus.util.SQLBuilder.MLC;
import com.landawn.abacus.util.SQLBuilder.MSB;
import com.landawn.abacus.util.SQLBuilder.MSC;

/**
 * Unit tests for SQLBuilder class
 */
public class SQLBuilder14Test extends TestBase {

    @Nested
    public class MSBTest extends TestBase {

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

        @Test
        public void testInsertSingleColumn() {
            String sql = MSB.insert("name").into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(name)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{name}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = MSB.insert("firstName", "lastName", "email").into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(firstName, lastName, email)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{email}"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "status");
            String sql = MSB.insert(columns).into("products").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO products"));
            Assertions.assertTrue(sql.contains("(id, name, status)"));
            Assertions.assertTrue(sql.contains("#{id}"));
            Assertions.assertTrue(sql.contains("#{name}"));
            Assertions.assertTrue(sql.contains("#{status}"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("name", "John");
            props.put("age", 30);
            String sql = MSB.insert(props).into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("name"));
            Assertions.assertTrue(sql.contains("age"));
            Assertions.assertTrue(sql.contains("#{name}"));
            Assertions.assertTrue(sql.contains("#{age}"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setEmail("john@example.com");

            String sql = MSB.insert(account).into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient field should be excluded
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly field should be excluded
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setEmail("john@example.com");

            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = MSB.insert(account, excludes).into("users").sql();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = MSB.insert(Account.class).into("test_account").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO test_account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate"));
            String sql = MSB.insert(Account.class, excludes).into("test_account").sql();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = MSB.insertInto(Account.class).sql();
            Assertions.assertTrue(sql.contains("INSERT INTO test_account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = MSB.insertInto(Account.class, excludes).sql();
            Assertions.assertTrue(sql.contains("INSERT INTO test_account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testBatchInsert() {
            List<Map<String, Object>> propsList = new ArrayList<>();
            Map<String, Object> props1 = new HashMap<>();
            props1.put("name", "John");
            props1.put("age", 30);
            Map<String, Object> props2 = new HashMap<>();
            props2.put("name", "Jane");
            props2.put("age", 25);
            propsList.add(props1);
            propsList.add(props2);

            String sql = MSB.batchInsert(propsList).into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{name}"));
            Assertions.assertTrue(sql.contains("#{name}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = MSB.update("users").set("status", "lastModified").where(CF.eq("id", 123)).sql();
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status = #{status}"));
            Assertions.assertTrue(sql.contains("lastModified = #{lastModified}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = MSB.update("user_archive", Account.class).set("firstName").where(CF.eq("id", 123)).sql();
            Assertions.assertTrue(sql.contains("UPDATE user_archive"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = MSB.update(Account.class).where(CF.eq("id", 123)).sql();
            Assertions.assertTrue(sql.contains("UPDATE test_account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("lastName = #{lastName}"));
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly
            Assertions.assertFalse(sql.contains("nonUpdatableField")); // @NonUpdatable
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate"));
            String sql = MSB.update(Account.class, excludes).where(CF.eq("id", 123)).sql();
            Assertions.assertTrue(sql.contains("UPDATE test_account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = MSB.deleteFrom("users").where(CF.eq("status", "INACTIVE")).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = #{status}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = MSB.deleteFrom("users", Account.class).where(CF.eq("firstName", "John")).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = MSB.deleteFrom(Account.class).where(CF.lt("createdDate", new Date())).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM test_account"));
            Assertions.assertTrue(sql.contains("createdDate < #{createdDate}"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = MSB.select("COUNT(*)").from("users").sql();
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = MSB.select("firstName", "lastName", "email").from("users").where(CF.eq("active", true)).sql();
            Assertions.assertTrue(sql.contains("SELECT firstName, lastName, email"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "price");
            String sql = MSB.select(columns).from("products").sql();
            Assertions.assertTrue(sql.contains("SELECT id, name, price"));
            Assertions.assertTrue(sql.contains("FROM products"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = MSB.select(aliases).from("users").sql();
            Assertions.assertTrue(sql.contains("firstName AS \"fname\""));
            Assertions.assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = MSB.select(Account.class).from("users").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = MSB.select(Account.class, true).from("users").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = MSB.select(Account.class, excludes).from("users").sql();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = MSB.selectFrom(Account.class).where(CF.eq("active", true)).sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM test_account"));
            Assertions.assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = MSB.selectFrom(Account.class, "a").where(CF.eq("a.active", true)).sql();
            Assertions.assertTrue(sql.contains("FROM test_account a"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = MSB.selectFrom(Account.class, excludes).where(CF.eq("active", true)).sql();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = MSB.select(Account.class, "a", "account", Account.class, "b", "account2").from("test_account a, test_account b").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testCountTable() {
            String sql = MSB.count("users").where(CF.eq("active", true)).sql();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = MSB.count(Account.class).where(CF.between("createdDate", new Date(), new Date())).sql();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM test_account"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.condition.Condition cond = CF.and(CF.eq("active", true), CF.gt("age", 18));
            String sql = MSB.parse(cond, Account.class).sql();
            Assertions.assertTrue(sql.contains("active = #{active}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("age > #{age}"));
        }
    }

    @Nested
    public class MSCTest extends TestBase {

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

        @Test
        public void testInsertSingleColumn() {
            String sql = MSC.insert("userName").into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(user_name)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{userName}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = MSC.insert("firstName", "lastName", "emailAddress").into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(first_name, last_name, email_address)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{emailAddress}"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> props = Arrays.asList("firstName", "lastName");
            String sql = MSC.insert(props).into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(first_name, last_name)"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("firstName", "John");
            data.put("lastName", "Doe");
            String sql = MSC.insert(data).into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        public void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            String sql = MSC.insert(user).into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("temp_data")); // @Transient
            Assertions.assertFalse(sql.contains("read_only_field")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmailAddress("john@example.com");

            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.insert(user, exclude).into("users").sql();
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("email_address"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = MSC.insert(User.class).into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email_address"));
            Assertions.assertFalse(sql.contains("temp_data")); // @Transient
            Assertions.assertFalse(sql.contains("read_only_field")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("id", "createdDate"));
            String sql = MSC.insert(User.class, exclude).into("users").sql();
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = MSC.insertInto(User.class).sql();
            Assertions.assertTrue(sql.contains("INSERT INTO test_users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("id"));
            String sql = MSC.insertInto(User.class, exclude).sql();
            Assertions.assertTrue(sql.contains("INSERT INTO test_users"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testBatchInsert() {
            List<User> users = new ArrayList<>();
            User user1 = new User();
            user1.setFirstName("John");
            user1.setLastName("Doe");
            User user2 = new User();
            user2.setFirstName("Jane");
            user2.setLastName("Smith");
            users.add(user1);
            users.add(user2);

            String sql = MSC.batchInsert(users).into("users").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(first_name, last_name)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = MSC.update("users").set("firstName", "lastName").where(CF.eq("userId", 123)).sql();
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("last_name = #{lastName}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("user_id = #{userId}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = MSC.update("users", User.class).set("firstName", "lastName").where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("last_name = #{lastName}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = MSC.update(User.class).where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("UPDATE test_users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("last_name = #{lastName}"));
            Assertions.assertFalse(sql.contains("read_only_field")); // @ReadOnly
            Assertions.assertFalse(sql.contains("non_updatable_field")); // @NonUpdatable
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("createdDate"));
            String sql = MSC.update(User.class, exclude).where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("UPDATE test_users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = MSC.deleteFrom("users").where(CF.eq("userId", 123)).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("user_id = #{userId}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = MSC.deleteFrom("users", User.class).where(CF.eq("firstName", "John")).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = MSC.deleteFrom(User.class).where(CF.eq("id", 123)).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM test_users"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = MSC.select("COUNT(*)").from("users").sql();
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = MSC.select("firstName", "lastName", "emailAddress").from("users").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email_address AS \"emailAddress\""));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = MSC.select(columns).from("users").sql();
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = MSC.select(aliases).from("users").sql();
            Assertions.assertTrue(sql.contains("first_name AS \"fname\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = MSC.select(User.class).from("users").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertTrue(sql.contains("AS \"lastName\""));
            Assertions.assertFalse(sql.contains("temp_data")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = MSC.select(User.class, true).from("users").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.select(User.class, exclude).from("users").sql();
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = MSC.selectFrom(User.class).sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM test_users"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = MSC.selectFrom(User.class, "u").where(CF.eq("u.active", true)).sql();
            Assertions.assertTrue(sql.contains("FROM test_users u"));
            Assertions.assertTrue(sql.contains("u.active = #{u.active}"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("password"));
            String sql = MSC.selectFrom(User.class, exclude).sql();
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.selectFrom(User.class, "u", exclude).sql();
            Assertions.assertTrue(sql.contains("FROM test_users u"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.selectFrom(User.class, true, exclude).sql();
            Assertions.assertTrue(sql.contains("FROM test_users"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromEntityClassWithFullOptions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.selectFrom(User.class, "u", true, exclude).sql();
            Assertions.assertTrue(sql.contains("FROM test_users u"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = MSC.select(User.class, "u", "user", User.class, "u2", "user2").from("test_users u, test_users u2").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("password", "emailAddress"));
            String sql = MSC.select(User.class, "u", "user", exclude, User.class, "u2", "user2", exclude).from("test_users u, test_users u2").sql();
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectWithMultiSelects() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));
            String sql = MSC.select(selections).from("test_users u, test_users u2").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = MSC.selectFrom(User.class, "u", "user", User.class, "u2", "user2").sql();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> exclude1 = new HashSet<>(Arrays.asList("password"));
            Set<String> exclude2 = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.selectFrom(User.class, "u", "user", exclude1, User.class, "u2", "user2", exclude2).sql();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertTrue(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromWithMultiSelects() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));
            String sql = MSC.selectFrom(selections).sql();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testCountTable() {
            String sql = MSC.count("users").sql();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = MSC.count(User.class).where(CF.gt("age", 18)).sql();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM test_users"));
            Assertions.assertTrue(sql.contains("age > #{age}"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.condition.Condition cond = CF.and(CF.eq("firstName", "John"), CF.gt("age", 18));
            String sql = MSC.parse(cond, User.class).sql();
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("age > #{age}"));
        }
    }

    @Nested
    public class MACTest extends TestBase {

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

        @Test
        public void testInsertSingleColumn() {
            String sql = MAC.insert("firstName").into("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = MAC.insert("firstName", "lastName", "email").into("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME, LAST_NAME, EMAIL)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{email}"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = MAC.insert(columns).into("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME, LAST_NAME, EMAIL)"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{email}"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            String sql = MAC.insert(props).into("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setEmail("john@example.com");

            String sql = MAC.insert(account).into("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertFalse(sql.contains("TEMP_DATA")); // @Transient
            Assertions.assertFalse(sql.contains("READ_ONLY_FIELD")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setId(1L);
            account.setFirstName("John");
            account.setLastName("Doe");

            Set<String> excludes = new HashSet<>(Arrays.asList("id"));
            String sql = MAC.insert(account, excludes).into("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertFalse(sql.contains("ID"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = MAC.insert(Account.class).into("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertFalse(sql.contains("TEMP_DATA")); // @Transient
            Assertions.assertFalse(sql.contains("READ_ONLY_FIELD")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "createdDate"));
            String sql = MAC.insert(Account.class, excludes).into("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertFalse(sql.contains("ID"));
            Assertions.assertFalse(sql.contains("CREATED_DATE"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = MAC.insertInto(Account.class).sql();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = MAC.insertInto(Account.class, excludes).sql();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertFalse(sql.contains("ID"));
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = new ArrayList<>();
            Account acc1 = new Account();
            acc1.setFirstName("John");
            acc1.setLastName("Doe");
            Account acc2 = new Account();
            acc2.setFirstName("Jane");
            acc2.setLastName("Smith");
            accounts.add(acc1);
            accounts.add(acc2);

            String sql = MAC.batchInsert(accounts).into("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME, LAST_NAME)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = MAC.update("ACCOUNT")
                    .set("firstName", "John")
                    .set("lastName", "Doe")
                    .set(Map.of("modifiedDate", new Date()))
                    .where(CF.eq("id", 1))
                    .sql();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = #{firstName}"));
            Assertions.assertTrue(sql.contains("LAST_NAME = #{lastName}"));
            Assertions.assertTrue(sql.contains("MODIFIED_DATE = #{modifiedDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = MAC.update("ACCOUNT", Account.class)
                    .set(Map.of("isActive", false))
                    .set(Map.of("deactivatedDate", new Date()))
                    .where(CF.eq("id", 1))
                    .sql();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            Assertions.assertTrue(sql.contains("DEACTIVATED_DATE = #{deactivatedDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = MAC.update(Account.class).set("status", "ACTIVE").set(Map.of("lastLoginDate", new Date())).where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("STATUS = #{status}"));
            Assertions.assertTrue(sql.contains("LAST_LOGIN_DATE = #{lastLoginDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "createdDate", "createdBy"));
            String sql = MAC.update(Account.class, excludes).set("firstName", "John").set(Map.of("modifiedDate", new Date())).where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = #{firstName}"));
            Assertions.assertTrue(sql.contains("MODIFIED_DATE = #{modifiedDate}"));
            Assertions.assertFalse(sql.contains("CREATED_DATE"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = MAC.deleteFrom("ACCOUNT").where(CF.eq("status", "INACTIVE")).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("STATUS = #{status}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = MAC.deleteFrom("ACCOUNT", Account.class).where(CF.and(CF.eq("isActive", false), CF.lt("lastLoginDate", new Date()))).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("LAST_LOGIN_DATE < #{lastLoginDate}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = MAC.deleteFrom(Account.class).where(CF.in("id", Arrays.asList(1, 2, 3))).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID IN"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = MAC.select("COUNT(*) AS total, MAX(createdDate) AS latest").from("ACCOUNT").sql();
            Assertions.assertEquals("SELECT COUNT(*) AS total, MAX(createdDate) AS latest FROM ACCOUNT", sql);
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = MAC.select("firstName", "lastName", "emailAddress").from("ACCOUNT").where(CF.gt("createdDate", new Date())).sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            Assertions.assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("CREATED_DATE > #{createdDate}"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
            String sql = MAC.select(columns).from("ACCOUNT").orderBy("createdDate DESC").sql();
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            Assertions.assertTrue(sql.contains("PHONE_NUMBER AS \"phoneNumber\""));
            Assertions.assertTrue(sql.contains("ORDER BY CREATED_DATE DESC"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");
            String sql = MAC.select(aliases).from("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lname\""));
            Assertions.assertTrue(sql.contains("EMAIL_ADDRESS AS \"email\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = MAC.select(Account.class).from("ACCOUNT").where(CF.eq("isActive", true)).sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("AS \"id\""));
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertTrue(sql.contains("AS \"lastName\""));
            Assertions.assertFalse(sql.contains("TEMP_DATA")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = MAC.select(Account.class, true).from("ACCOUNT").innerJoin("ADDRESS").on("ACCOUNT.ADDRESS_ID = ADDRESS.ID").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("INNER JOIN ADDRESS"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("passwordHash", "securityToken"));
            String sql = MAC.select(Account.class, excludes).from("ACCOUNT").sql();
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertTrue(sql.contains("AS \"lastName\""));
            Assertions.assertFalse(sql.contains("passwordHash"));
            Assertions.assertFalse(sql.contains("securityToken"));
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = MAC.select(Account.class, true, excludes).from("ACCOUNT").innerJoin("PROFILE").on("ACCOUNT.PROFILE_ID = PROFILE.ID").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("internalNotes"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = MAC.selectFrom(Account.class).where(CF.eq("isActive", true)).orderBy("createdDate DESC").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            Assertions.assertTrue(sql.contains("ORDER BY CREATED_DATE DESC"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = MAC.selectFrom(Account.class, "a").innerJoin("PROFILE p").on("a.PROFILE_ID = p.ID").where(CF.eq("a.isActive", true)).sql();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT a"));
            Assertions.assertTrue(sql.contains("INNER JOIN PROFILE p"));
            Assertions.assertTrue(sql.contains("a.IS_ACTIVE = #{a.isActive}"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            String sql = MAC.selectFrom(Account.class, true).innerJoin("ADDRESS").on("ACCOUNT.ADDRESS_ID = ADDRESS.ID").sql();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("INNER JOIN ADDRESS"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            String sql = MAC.selectFrom(Account.class, "acc", true).innerJoin("PROFILE p").on("acc.PROFILE_ID = p.ID").sql();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT acc"));
            Assertions.assertTrue(sql.contains("INNER JOIN PROFILE p"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
            String sql = MAC.selectFrom(Account.class, excludes).where(CF.eq("status", "ACTIVE")).sql();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertFalse(sql.contains("largeBlob"));
            Assertions.assertFalse(sql.contains("internalData"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = MAC.selectFrom(Account.class, "a", excludes).where(CF.like("a.emailAddress", "%@example.com")).sql();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT a"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertTrue(sql.contains("a.EMAIL_ADDRESS LIKE #{a.emailAddress}"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
            String sql = MAC.selectFrom(Account.class, true, excludes).innerJoin("ORDERS").on("ACCOUNT.ID = ORDERS.ACCOUNT_ID").sql();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertFalse(sql.contains("temporaryData"));
        }

        @Test
        public void testSelectFromEntityClassWithFullOptions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("debugInfo"));
            String sql = MAC.selectFrom(Account.class, "acc", true, excludes)
                    .innerJoin("ORDERS o")
                    .on("acc.ID = o.ACCOUNT_ID")
                    .innerJoin("ITEMS i")
                    .on("o.ID = i.ORDER_ID")
                    .where(CF.gt("o.total", 100))
                    .sql();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT acc"));
            Assertions.assertFalse(sql.contains("debugInfo"));
            Assertions.assertTrue(sql.contains("O.TOTAL > #{o.total}"));
        }

        @Test
        public void testCountTable() {
            String sql = MAC.count("ACCOUNT").where(CF.eq("isActive", true)).sql();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = MAC.count(Account.class).where(CF.between("createdDate", new Date(), new Date())).sql();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("CREATED_DATE BETWEEN"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.condition.Condition cond = CF.and(CF.eq("status", "ACTIVE"), CF.gt("balance", 1000));
            String sql = MAC.parse(cond, Account.class).sql();
            Assertions.assertTrue(sql.contains("STATUS = #{status}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("BALANCE > #{balance}"));
        }
    }

    @Nested
    public class MLCTest extends TestBase {

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

        @Test
        public void testInsertSingleColumn() {
            String sql = MLC.insert("firstName").into("account").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = MLC.insert("firstName", "lastName", "emailAddress").into("account").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName, lastName, emailAddress)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{emailAddress}"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
            String sql = MLC.insert(columns).into("account").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName, lastName, phoneNumber)"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{phoneNumber}"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("isActive", true);
            String sql = MLC.insert(props).into("account").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("isActive"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
            Assertions.assertTrue(sql.contains("#{isActive}"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedDate(new Date());

            String sql = MLC.insert(account).into("account").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("createdDate"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setId(1L);
            account.setFirstName("John");
            account.setLastName("Doe");

            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = MLC.insert(account, excludes).into("account").sql();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = MLC.insert(Account.class).into("account").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "auditFields"));
            String sql = MLC.insert(Account.class, excludes).into("account").sql();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = MLC.insertInto(Account.class).sql();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = MLC.insertInto(Account.class, excludes).sql();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = new ArrayList<>();
            Account acc1 = new Account();
            acc1.setFirstName("John");
            acc1.setLastName("Doe");
            Account acc2 = new Account();
            acc2.setFirstName("Jane");
            acc2.setLastName("Smith");
            accounts.add(acc1);
            accounts.add(acc2);

            String sql = MLC.batchInsert(accounts).into("account").sql();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName, lastName)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
            Assertions.assertTrue(sql.contains("#{lastName}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = MLC.update("account").set("firstName", "updatedName").set(Map.of("modifiedDate", new Date())).where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("modifiedDate = #{modifiedDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = MLC.update("account", Account.class).set("firstName", "John").set("lastName", "Doe").where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("lastName = #{lastName}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = MLC.update(Account.class).set("firstName", "John").where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate", "createdBy"));
            String sql = MLC.update(Account.class, excludes).set("firstName", "John").where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = MLC.deleteFrom("account").where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = MLC.deleteFrom("account", Account.class).where(CF.eq("emailAddress", "john@example.com")).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("emailAddress = #{emailAddress}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = MLC.deleteFrom(Account.class).where(CF.eq("id", 1)).sql();
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = MLC.select("COUNT(*) AS total, MAX(createdDate) AS latest").from("account").sql();
            Assertions.assertTrue(sql.contains("SELECT COUNT(*) AS total, MAX(createdDate) AS latest"));
            Assertions.assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = MLC.select("firstName", "lastName", "emailAddress").from("account").where(CF.gt("createdDate", new Date())).sql();
            Assertions.assertTrue(sql.contains("SELECT firstName, lastName, emailAddress"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("createdDate > #{createdDate}"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = getColumnsToSelect();
            String sql = MLC.select(columns).from("account").orderBy("createdDate DESC").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("ORDER BY createdDate DESC"));
        }

        private List<String> getColumnsToSelect() {
            return Arrays.asList("firstName", "lastName", "emailAddress");
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            aliases.put("emailAddress", "email");
            String sql = MLC.select(aliases).from("account").sql();
            Assertions.assertTrue(sql.contains("firstName AS \"fname\""));
            Assertions.assertTrue(sql.contains("lastName AS \"lname\""));
            Assertions.assertTrue(sql.contains("emailAddress AS \"email\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = MLC.select(Account.class).from("account").where(CF.eq("isActive", true)).sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertTrue(sql.contains("isActive"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = MLC.select(Account.class, true).from("account").innerJoin("address").on("account.addressId = address.id").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("INNER JOIN address"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password", "secretKey"));
            String sql = MLC.select(Account.class, excludes).from("account").sql();
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("secretKey"));
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = MLC.select(Account.class, true, excludes).from("account").innerJoin("profile").on("account.profileId = profile.id").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("internalNotes"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = MLC.selectFrom(Account.class).where(CF.eq("isActive", true)).orderBy("createdDate DESC").sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("isActive = #{isActive}"));
            Assertions.assertTrue(sql.contains("ORDER BY createdDate DESC"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = MLC.selectFrom(Account.class, "a")
                    .innerJoin("profile p")
                    .on("a.profileId = p.id")
                    .where(CF.like("a.emailAddress", "%@example.com"))
                    .sql();
            Assertions.assertTrue(sql.contains("FROM account a"));
            Assertions.assertTrue(sql.contains("INNER JOIN profile p"));
            Assertions.assertTrue(sql.contains("a.emailAddress LIKE #{a.emailAddress}"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            String sql = MLC.selectFrom(Account.class, true).innerJoin("address").on("account.addressId = address.id").sql();
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("INNER JOIN address"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            String sql = MLC.selectFrom(Account.class, "acc", true).innerJoin("profile p").on("acc.profileId = p.id").sql();
            Assertions.assertTrue(sql.contains("FROM account acc"));
            Assertions.assertTrue(sql.contains("INNER JOIN profile p"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
            String sql = MLC.selectFrom(Account.class, excludes).where(CF.eq("status", "ACTIVE")).sql();
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertFalse(sql.contains("largeBlob"));
            Assertions.assertFalse(sql.contains("internalData"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = MLC.selectFrom(Account.class, "a", excludes).where(CF.like("a.emailAddress", "%@example.com")).sql();
            Assertions.assertTrue(sql.contains("FROM account a"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
            String sql = MLC.selectFrom(Account.class, true, excludes).innerJoin("orders").on("account.id = orders.accountId").sql();
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertFalse(sql.contains("temporaryData"));
        }

        @Test
        public void testSelectFromEntityClassWithFullOptions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("debugInfo"));
            String sql = MLC.selectFrom(Account.class, "acc", true, excludes)
                    .innerJoin("orders o")
                    .on("acc.id = o.accountId")
                    .innerJoin("items i")
                    .on("o.id = i.orderId")
                    .where(CF.gt("o.total", 100))
                    .sql();
            Assertions.assertTrue(sql.contains("FROM account acc"));
            Assertions.assertFalse(sql.contains("debugInfo"));
            Assertions.assertTrue(sql.contains("o.total > #{o.total}"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = MLC.select(Account.class, "a", "account", Account.class, "a2", "account2")
                    .from("account a")
                    .innerJoin("account a2")
                    .on("a.id = a2.parentId")
                    .sql();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> accountExcludes = new HashSet<>(Arrays.asList("password"));
            Set<String> orderExcludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = MLC.select(Account.class, "a", "account", accountExcludes, Account.class, "a2", "account2", orderExcludes)
                    .from("account a")
                    .innerJoin("account a2")
                    .on("a.id = a2.parentId")
                    .where(CF.gt("a.createdDate", new Date()))
                    .sql();
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("internalNotes"));
        }

        @Test
        public void testSelectWithMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, true, null),
                    new Selection(Account.class, "a2", "account2", Arrays.asList("id", "name"), false, null));
            String sql = MLC.select(selections).from("account a").innerJoin("account a2").on("a.id = a2.parentId").sql();
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = MLC.selectFrom(Account.class, "a", "account", Account.class, "a2", "account2")
                    .innerJoin("a.id = a2.parentId")
                    .where(CF.gt("a.createdDate", new Date()))
                    .sql();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> accountExcludes = new HashSet<>(Arrays.asList("sensitiveData"));
            String sql = MLC.selectFrom(Account.class, "a", "account", accountExcludes, Account.class, "a2", "account2", null)
                    .innerJoin("a.id = a2.parentId")
                    .sql();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("sensitiveData"));
        }

        @Test
        public void testSelectFromWithMultipleSelections() {
            List<Selection> selections = createComplexSelections();
            String sql = MLC.selectFrom(selections)
                    .where(CF.and(CF.eq("status", "ACTIVE"), CF.gt("balance", 0)))
                    .groupBy("account.type")
                    .having(CF.gt("COUNT(*)", 5))
                    .sql();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("GROUP BY account.type"));
            Assertions.assertTrue(sql.contains("HAVING COUNT(*) > #{COUNT(*)}"));
        }

        private List<Selection> createComplexSelections() {
            return Arrays.asList(new Selection(Account.class, "a", "account", null, false, null));
        }

        @Test
        public void testCountTable() {
            String sql = MLC.count("account").where(CF.eq("isActive", true)).sql();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("isActive = #{isActive}"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = MLC.count(Account.class).where(CF.between("createdDate", new Date(), new Date())).sql();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("createdDate BETWEEN"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.condition.Condition cond = CF.and(CF.eq("status", "ACTIVE"), CF.gt("balance", 1000));
            String sql = MLC.parse(cond, Account.class).sql();
            Assertions.assertTrue(sql.contains("status = #{status}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("balance > #{balance}"));
        }
    }
}