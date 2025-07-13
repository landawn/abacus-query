package com.landawn.abacus.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.condition.Condition;
import com.landawn.abacus.condition.ConditionFactory;
import com.landawn.abacus.util.SQLBuilder.PAC;
import com.landawn.abacus.util.SQLBuilder.PLC;
import com.landawn.abacus.util.SQLBuilder.PSB;
import com.landawn.abacus.util.SQLBuilder.PSC;
import com.landawn.abacus.util.SQLBuilder.SP;

/**
 * Unit tests for SQLBuilder class
 */
public class SQLBuilder12Test extends TestBase {

    @Nested
    public class PSBTest {

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

        @Test
        public void testInsert_SingleColumn() {
            SQLBuilder builder = PSB.insert("name");
            assertNotNull(builder);

            // Test with empty string - should throw exception
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(""));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SQLBuilder builder = PSB.insert("firstName", "lastName", "email");
            assertNotNull(builder);

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> PSB.insert((String[]) null));

            // Test with empty array
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(new String[0]));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SQLBuilder builder = PSB.insert(columns);
            assertNotNull(builder);

            // Test with null collection
            assertThrows(IllegalArgumentException.class, () -> PSB.insert((Collection<String>) null));

            // Test with empty collection
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(new ArrayList<String>()));
        }

        @Test
        public void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SQLBuilder builder = PSB.insert(props);
            assertNotNull(builder);

            // Test with null map
            assertThrows(IllegalArgumentException.class, () -> PSB.insert((Map<String, Object>) null));

            // Test with empty map
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(new HashMap<String, Object>()));
        }

        @Test
        public void testInsert_Entity() {
            User user = new User("John", "Doe", "john@example.com");
            SQLBuilder builder = PSB.insert(user);
            assertNotNull(builder);

            // Test with null entity
            assertThrows(IllegalArgumentException.class, () -> PSB.insert((Object) null));
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            User user = new User("John", "Doe", "john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));

            SQLBuilder builder = PSB.insert(user, excludedProps);
            assertNotNull(builder);

            // Test with null entity
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(null, excludedProps));
        }

        @Test
        public void testInsert_Class() {
            SQLBuilder builder = PSB.insert(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.insert((Class<?>) null));
        }

        @Test
        public void testInsert_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("createdDate"));
            SQLBuilder builder = PSB.insert(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(null, excludedProps));
        }

        @Test
        public void testInsertInto_Class() {
            SQLBuilder builder = PSB.insertInto(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.insertInto((Class<?>) null));
        }

        @Test
        public void testInsertInto_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("id", "createdDate"));
            SQLBuilder builder = PSB.insertInto(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.insertInto(null, excludedProps));
        }

        @Test
        public void testBatchInsert() {
            List<User> users = Arrays.asList(new User("John", "Doe", "john@example.com"), new User("Jane", "Smith", "jane@example.com"));

            SQLBuilder builder = PSB.batchInsert(users);
            assertNotNull(builder);

            // Test with empty collection
            assertThrows(IllegalArgumentException.class, () -> PSB.batchInsert(new ArrayList<>()));

            // Test with null collection
            assertThrows(IllegalArgumentException.class, () -> PSB.batchInsert(null));

            // Test with maps
            List<Map<String, Object>> maps = new ArrayList<>();
            Map<String, Object> map1 = new HashMap<>();
            map1.put("firstName", "John");
            maps.add(map1);

            SQLBuilder mapBuilder = PSB.batchInsert(maps);
            assertNotNull(mapBuilder);
        }

        @Test
        public void testUpdate_TableName() {
            SQLBuilder builder = PSB.update("users");
            assertNotNull(builder);

            // Test with null table name
            assertThrows(IllegalArgumentException.class, () -> PSB.update((String) null));

            // Test with empty table name
            assertThrows(IllegalArgumentException.class, () -> PSB.update(""));
        }

        @Test
        public void testUpdate_TableNameWithEntityClass() {
            SQLBuilder builder = PSB.update("users", User.class);
            assertNotNull(builder);

            // Test with null parameters
            assertThrows(IllegalArgumentException.class, () -> PSB.update(null, User.class));
            assertThrows(IllegalArgumentException.class, () -> PSB.update("users", null));
        }

        @Test
        public void testUpdate_EntityClass() {
            SQLBuilder builder = PSB.update(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.update((Class<?>) null));
        }

        @Test
        public void testUpdate_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("createdDate"));
            SQLBuilder builder = PSB.update(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.update(null, excludedProps));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SQLBuilder builder = PSB.deleteFrom("users");
            assertNotNull(builder);

            // Test with null table name
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom((String) null));

            // Test with empty table name
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom(""));
        }

        @Test
        public void testDeleteFrom_TableNameWithEntityClass() {
            SQLBuilder builder = PSB.deleteFrom("users", User.class);
            assertNotNull(builder);

            // Test with null parameters
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom(null, User.class));
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom("users", null));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SQLBuilder builder = PSB.deleteFrom(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom((Class<?>) null));
        }

        @Test
        public void testSelect_SingleColumn() {
            SQLBuilder builder = PSB.select("firstName");
            assertNotNull(builder);

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> PSB.select((String) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.select(""));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SQLBuilder builder = PSB.select("firstName", "lastName", "email");
            assertNotNull(builder);

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> PSB.select((String[]) null));

            // Test with empty array
            assertThrows(IllegalArgumentException.class, () -> PSB.select(new String[0]));
        }

        @Test
        public void testSelect_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            SQLBuilder builder = PSB.select(columns);
            assertNotNull(builder);

            // Test with null/empty collection
            assertThrows(IllegalArgumentException.class, () -> PSB.select((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.select(new ArrayList<String>()));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SQLBuilder builder = PSB.select(aliases);
            assertNotNull(builder);

            // Test with null/empty map
            assertThrows(IllegalArgumentException.class, () -> PSB.select((Map<String, String>) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.select(new HashMap<String, String>()));
        }

        @Test
        public void testSelect_EntityClass() {
            SQLBuilder builder = PSB.select(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.select((Class<?>) null));
        }

        @Test
        public void testSelect_EntityClassWithSubEntities() {
            SQLBuilder builder = PSB.select(User.class, true);
            assertNotNull(builder);

            // Test without sub-entities
            SQLBuilder builder2 = PSB.select(User.class, false);
            assertNotNull(builder2);
        }

        @Test
        public void testSelect_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SQLBuilder builder = PSB.select(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null excluded props
            SQLBuilder builder2 = PSB.select(User.class, null);
            assertNotNull(builder2);
        }

        @Test
        public void testSelect_EntityClassFullOptions() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SQLBuilder builder = PSB.select(User.class, true, excludedProps);
            assertNotNull(builder);

            // Test all combinations
            assertNotNull(PSB.select(User.class, false, null));
            assertNotNull(PSB.select(User.class, true, null));
            assertNotNull(PSB.select(User.class, false, excludedProps));
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SQLBuilder builder = PSB.selectFrom(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.selectFrom((Class<?>) null));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SQLBuilder builder = PSB.selectFrom(User.class, "u");
            assertNotNull(builder);

            // Test with null alias
            SQLBuilder builder2 = PSB.selectFrom(User.class, (String) null);
            assertNotNull(builder2);
        }

        @Test
        public void testSelectFrom_EntityClassWithSubEntities() {
            SQLBuilder builder = PSB.selectFrom(User.class, true);
            assertNotNull(builder);

            SQLBuilder builder2 = PSB.selectFrom(User.class, false);
            assertNotNull(builder2);
        }

        @Test
        public void testSelectFrom_EntityClassWithAliasAndSubEntities() {
            SQLBuilder builder = PSB.selectFrom(User.class, "u", true);
            assertNotNull(builder);

            SQLBuilder builder2 = PSB.selectFrom(User.class, "u", false);
            assertNotNull(builder2);
        }

        @Test
        public void testSelectFrom_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SQLBuilder builder = PSB.selectFrom(User.class, excludedProps);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_EntityClassWithAliasAndExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SQLBuilder builder = PSB.selectFrom(User.class, "u", excludedProps);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_EntityClassWithSubEntitiesAndExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SQLBuilder builder = PSB.selectFrom(User.class, true, excludedProps);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_EntityClassFullOptions() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SQLBuilder builder = PSB.selectFrom(User.class, "u", true, excludedProps);
            assertNotNull(builder);

            // Test various combinations
            assertNotNull(PSB.selectFrom(User.class, "u", false, null));
            assertNotNull(PSB.selectFrom(User.class, null, true, excludedProps));
        }

        @Test
        public void testSelect_TwoEntities() {
            SQLBuilder builder = PSB.select(User.class, "u", "user", User.class, "u2", "user2");
            assertNotNull(builder);
        }

        @Test
        public void testSelect_TwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("createdDate"));

            SQLBuilder builder = PSB.select(User.class, "u", "user", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(builder);

            // Test with null exclusions
            SQLBuilder builder2 = PSB.select(User.class, "u", "user", null, User.class, "u2", "user2", null);
            assertNotNull(builder2);
        }

        @Test
        public void testSelect_MultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SQLBuilder builder = PSB.select(selections);
            assertNotNull(builder);

            // Test with null/empty list
            assertThrows(IllegalArgumentException.class, () -> PSB.select((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.select(new ArrayList<Selection>()));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SQLBuilder builder = PSB.selectFrom(User.class, "u", "user", User.class, "u2", "user2");
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_TwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("createdDate"));

            SQLBuilder builder = PSB.selectFrom(User.class, "u", "user", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_MultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SQLBuilder builder = PSB.selectFrom(selections);
            assertNotNull(builder);

            // Test with invalid selections
            assertThrows(IllegalArgumentException.class, () -> PSB.selectFrom((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.selectFrom(new ArrayList<Selection>()));
        }

        @Test
        public void testCount_TableName() {
            SQLBuilder builder = PSB.count("users");
            assertNotNull(builder);

            // Test with null/empty table name
            assertThrows(IllegalArgumentException.class, () -> PSB.count((String) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.count(""));
        }

        @Test
        public void testCount_EntityClass() {
            SQLBuilder builder = PSB.count(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.count((Class<?>) null));
        }

        @Test
        public void testParse() {
            Condition cond = ConditionFactory.eq("firstName", "John");
            SQLBuilder builder = PSB.parse(cond, User.class);
            assertNotNull(builder);

            // Test with null condition
            assertThrows(IllegalArgumentException.class, () -> PSB.parse(null, User.class));

            // Test with complex condition
            Condition complexCond = ConditionFactory.and(ConditionFactory.eq("firstName", "John"), ConditionFactory.gt("id", 1));
            SQLBuilder complexBuilder = PSB.parse(complexCond, User.class);
            assertNotNull(complexBuilder);
        }

        @Test
        public void testCreateInstance() {
            // Test that createInstance returns a new instance
            PSB instance1 = PSB.createInstance();
            PSB instance2 = PSB.createInstance();

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }
    }

    @Nested
    public class PSCTest {

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

        @Test
        public void testInsert_SingleColumn() {
            SQLBuilder builder = PSC.insert("firstName");
            assertNotNull(builder);

            // Test SQL generation
            String sql = builder.into("account").sql();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("first_name"));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SQLBuilder builder = PSC.insert("firstName", "lastName", "email");
            assertNotNull(builder);

            String sql = builder.into("account").sql();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SQLBuilder builder = PSC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("account").sql();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        public void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SQLBuilder builder = PSC.insert(props);
            assertNotNull(builder);

            SP pair = builder.into("account").build();
            assertTrue(pair.sql.contains("first_name"));
            assertTrue(pair.sql.contains("last_name"));
            assertEquals(2, pair.parameters.size());
        }

        @Test
        public void testInsert_Entity() {
            Account account = new Account("John", "Doe");
            account.setEmail("john@example.com");

            SQLBuilder builder = PSC.insert(account);
            assertNotNull(builder);

            SP pair = builder.into("account").build();
            assertTrue(pair.sql.contains("first_name"));
            assertTrue(pair.sql.contains("last_name"));
            assertTrue(pair.sql.contains("email"));
            assertFalse(pair.sql.contains("created_date")); // ReadOnly field should be excluded
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            Account account = new Account("John", "Doe");
            account.setEmail("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));

            SQLBuilder builder = PSC.insert(account, excludedProps);
            assertNotNull(builder);

            SP pair = builder.into("account").build();
            assertTrue(pair.sql.contains("first_name"));
            assertTrue(pair.sql.contains("last_name"));
            assertFalse(pair.sql.contains("email"));
        }

        @Test
        public void testInsert_Class() {
            SQLBuilder builder = PSC.insert(Account.class);
            assertNotNull(builder);

            String sql = builder.into("account").sql();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("created_date")); // ReadOnly
            assertFalse(sql.contains("transient_field")); // Transient
        }

        @Test
        public void testInsert_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email", "status"));
            SQLBuilder builder = PSC.insert(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.into("account").sql();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertFalse(sql.contains("email"));
            assertFalse(sql.contains("status"));
        }

        @Test
        public void testInsertInto_Class() {
            SQLBuilder builder = PSC.insertInto(Account.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertInto_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("id", "createdDate"));
            SQLBuilder builder = PSC.insertInto(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("INSERT INTO account"));
            assertFalse(sql.contains(" id "));
            assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account("John", "Doe"), new Account("Jane", "Smith"));

            SQLBuilder builder = PSC.batchInsert(accounts);
            assertNotNull(builder);

            SP pair = builder.into("account").build();
            assertTrue(pair.sql.contains("VALUES"));
            assertTrue(pair.sql.contains("(?, ?)"));
            assertTrue(pair.sql.contains(", (?, ?)"));
            assertEquals(4, pair.parameters.size()); // 2 accounts * 2 fields each
        }

        @Test
        public void testUpdate_TableName() {
            SQLBuilder builder = PSC.update("account");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testUpdate_TableNameWithEntityClass() {
            SQLBuilder builder = PSC.update("account", Account.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testUpdate_EntityClass() {
            SQLBuilder builder = PSC.update(Account.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(ConditionFactory.eq("id", 1)).sql();
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
        public void testUpdate_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));
            SQLBuilder builder = PSC.update(Account.class, excludedProps);
            assertNotNull(builder);

            // Get the generated column names
            String sql = builder.set("firstName", "John").where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("first_name"));
            assertFalse(sql.contains("email"));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SQLBuilder builder = PSC.deleteFrom("account");
            assertNotNull(builder);

            String sql = builder.where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testDeleteFrom_TableNameWithEntityClass() {
            SQLBuilder builder = PSC.deleteFrom("account", Account.class);
            assertNotNull(builder);

            String sql = builder.where(ConditionFactory.eq("firstName", "John")).sql();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SQLBuilder builder = PSC.deleteFrom(Account.class);
            assertNotNull(builder);

            String sql = builder.where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("DELETE FROM account"));
        }

        @Test
        public void testSelect_SingleColumn() {
            SQLBuilder builder = PSC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("account").sql();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SQLBuilder builder = PSC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("account").sql();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelect_Collection() {
            List<String> columns = Arrays.asList("id", "firstName", "lastName");
            SQLBuilder builder = PSC.select(columns);
            assertNotNull(builder);

            String sql = builder.from("account").sql();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SQLBuilder builder = PSC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("account").sql();
            assertTrue(sql.contains("first_name AS \"fname\""));
            assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        public void testSelect_EntityClass() {
            SQLBuilder builder = PSC.select(Account.class);
            assertNotNull(builder);

            String sql = builder.from("account").sql();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
            assertTrue(sql.contains("email"));
            assertFalse(sql.contains("transient_field"));
        }

        @Test
        public void testSelect_EntityClassWithSubEntities() {
            SQLBuilder builder = PSC.select(Account.class, true);
            assertNotNull(builder);

            String sql = builder.from("account").sql();
            assertNotNull(sql);
        }

        @Test
        public void testSelect_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email", "status"));
            SQLBuilder builder = PSC.select(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.from("account").sql();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name"));
            assertFalse(sql.contains("email"));
            assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SQLBuilder builder = PSC.selectFrom(Account.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SQLBuilder builder = PSC.selectFrom(Account.class, "a");
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("FROM account a"));
            assertTrue(sql.contains("a.first_name AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SQLBuilder builder = PSC.selectFrom(Account.class, "a", "account", Account.class, "a2", "account2");
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("a.first_name AS \"account.firstName\""));
            assertTrue(sql.contains("a2.first_name AS \"account2.firstName\""));
        }

        @Test
        public void testSelect_MultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", Arrays.asList("id", "firstName"), false, null),
                    new Selection(Account.class, "a2", "account2", null, false, new HashSet<>(Arrays.asList("email"))));

            SQLBuilder builder = PSC.select(selections);
            assertNotNull(builder);

            String sql = builder.from("account a, account a2").sql();
            assertTrue(sql.contains("a.id AS \"account.id\""));
            assertTrue(sql.contains("a.first_name AS \"account.firstName\""));
            assertFalse(sql.contains("account2.email"));
        }

        @Test
        public void testCount_TableName() {
            SQLBuilder builder = PSC.count("account");
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testCount_EntityClass() {
            SQLBuilder builder = PSC.count(Account.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testParse() {
            Condition cond = ConditionFactory.and(ConditionFactory.eq("firstName", "John"), ConditionFactory.like("email", "%@example.com"));

            SQLBuilder builder = PSC.parse(cond, Account.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("first_name = ?"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("email LIKE ?"));
        }

        @Test
        public void testCreateInstance() {
            PSC instance1 = PSC.createInstance();
            PSC instance2 = PSC.createInstance();

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }

        @Test
        public void testNamingPolicy() {
            // Test that PSC uses snake_case naming
            SQLBuilder builder = PSC.select("firstName", "lastName", "emailAddress");
            String sql = builder.from("account").sql();

            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
            assertTrue(sql.contains("email_address AS \"emailAddress\""));
        }
    }

    @Nested
    public class PACTest {

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

        @Test
        public void testInsert_SingleColumn() {
            SQLBuilder builder = PAC.insert("firstName");
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").sql();
            assertTrue(sql.contains("INSERT INTO USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SQLBuilder builder = PAC.insert("firstName", "lastName", "emailAddress");
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").sql();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "emailAddress");
            SQLBuilder builder = PAC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").sql();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
        }

        @Test
        public void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SQLBuilder builder = PAC.insert(props);
            assertNotNull(builder);

            SP pair = builder.into("USER_ACCOUNT").build();
            assertTrue(pair.sql.contains("FIRST_NAME"));
            assertTrue(pair.sql.contains("LAST_NAME"));
            assertEquals(2, pair.parameters.size());
        }

        @Test
        public void testInsert_Entity() {
            UserAccount account = new UserAccount("John", "Doe");
            account.setEmailAddress("john@example.com");

            SQLBuilder builder = PAC.insert(account);
            assertNotNull(builder);

            SP pair = builder.into("USER_ACCOUNT").build();
            assertTrue(pair.sql.contains("FIRST_NAME"));
            assertTrue(pair.sql.contains("LAST_NAME"));
            assertTrue(pair.sql.contains("EMAIL_ADDRESS"));
            assertFalse(pair.sql.contains("CREATED_DATE")); // ReadOnly
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            UserAccount account = new UserAccount("John", "Doe");
            account.setEmailAddress("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("emailAddress"));

            SQLBuilder builder = PAC.insert(account, excludedProps);
            assertNotNull(builder);

            SP pair = builder.into("USER_ACCOUNT").build();
            assertTrue(pair.sql.contains("FIRST_NAME"));
            assertTrue(pair.sql.contains("LAST_NAME"));
            assertFalse(pair.sql.contains("EMAIL_ADDRESS"));
        }

        @Test
        public void testInsert_Class() {
            SQLBuilder builder = PAC.insert(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").sql();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
            assertFalse(sql.contains("CREATED_DATE")); // ReadOnly
            assertFalse(sql.contains("TEMP_DATA")); // Transient
        }

        @Test
        public void testInsertInto_Class() {
            SQLBuilder builder = PAC.insertInto(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("INSERT INTO USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testBatchInsert() {
            List<UserAccount> accounts = Arrays.asList(new UserAccount("John", "Doe"), new UserAccount("Jane", "Smith"));

            SQLBuilder builder = PAC.batchInsert(accounts);
            assertNotNull(builder);

            SP pair = builder.into("USER_ACCOUNT").build();
            assertTrue(pair.sql.contains("VALUES"));
            assertTrue(pair.sql.contains("(?, ?)"));
            assertTrue(pair.sql.contains(", (?, ?)"));
        }

        @Test
        public void testUpdate_TableName() {
            SQLBuilder builder = PAC.update("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("UPDATE USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = ?"));
        }

        @Test
        public void testUpdate_EntityClass() {
            SQLBuilder builder = PAC.update(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("UPDATE USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = ?"));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SQLBuilder builder = PAC.deleteFrom("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("DELETE FROM USER_ACCOUNT"));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SQLBuilder builder = PAC.deleteFrom(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("DELETE FROM USER_ACCOUNT"));
        }

        @Test
        public void testSelect_SingleColumn() {
            SQLBuilder builder = PAC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").sql();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SQLBuilder builder = PAC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").sql();
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SQLBuilder builder = PAC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").sql();
            assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            assertTrue(sql.contains("LAST_NAME AS \"lname\""));
        }

        @Test
        public void testSelect_EntityClass() {
            SQLBuilder builder = PAC.select(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").sql();
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
            assertFalse(sql.contains("TEMP_DATA"));
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SQLBuilder builder = PAC.selectFrom(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SQLBuilder builder = PAC.selectFrom(UserAccount.class, "u");
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("FROM USER_ACCOUNT u"));
            assertTrue(sql.contains("u.FIRST_NAME AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SQLBuilder builder = PAC.selectFrom(UserAccount.class, "u1", "user1", UserAccount.class, "u2", "user2");
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("u1.FIRST_NAME AS \"user1.firstName\""));
            assertTrue(sql.contains("u2.FIRST_NAME AS \"user2.firstName\""));
        }

        @Test
        public void testCount_TableName() {
            SQLBuilder builder = PAC.count("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        public void testCount_EntityClass() {
            SQLBuilder builder = PAC.count(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        public void testParse() {
            Condition cond = ConditionFactory.and(ConditionFactory.eq("firstName", "John"), ConditionFactory.gt("id", 1));

            SQLBuilder builder = PAC.parse(cond, UserAccount.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("FIRST_NAME = ?"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("ID > ?"));
        }

        @Test
        public void testCreateInstance() {
            PAC instance1 = PAC.createInstance();
            PAC instance2 = PAC.createInstance();

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }

        @Test
        public void testNamingPolicy() {
            // Test that PAC uses UPPER_CASE naming
            SQLBuilder builder = PAC.select("firstName", "lastName", "emailAddress");
            String sql = builder.from("USER_ACCOUNT").sql();

            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
        }

        @Test
        public void testComplexQueries() {
            // Test complex query with joins
            Set<String> excludeUser1 = new HashSet<>(Arrays.asList("tempData"));
            Set<String> excludeUser2 = new HashSet<>(Arrays.asList("createdDate"));

            SQLBuilder builder = PAC.select(UserAccount.class, "u1", "user1", excludeUser1, UserAccount.class, "u2", "user2", excludeUser2);

            String sql = builder.from("USER_ACCOUNT u1, USER_ACCOUNT u2").where(ConditionFactory.eq("u1.id", "u2.id")).sql();

            assertTrue(sql.contains("u1.ID AS \"user1.id\""));
            assertTrue(sql.contains("u2.ID AS \"user2.id\""));
            assertFalse(sql.contains("user1.tempData"));
            assertFalse(sql.contains("user2.createdDate"));
        }

        @Test
        public void testAllMethodsWithNullParameters() {
            // Test all methods handle null appropriately
            // assertThrows(IllegalArgumentException.class, () -> PAC.insert((String) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.insert((String[]) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.insert((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.insert((Map<String, Object>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.insert((Object) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.insert((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.insertInto((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.batchInsert(null));
            assertThrows(IllegalArgumentException.class, () -> PAC.update((String) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.update((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.deleteFrom((String) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.deleteFrom((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.select((String) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.select((String[]) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.select((Collection<String>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.select((Map<String, String>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.select((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.selectFrom((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.select((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.selectFrom((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.count((String) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.count((Class<?>) null));
            assertThrows(IllegalArgumentException.class, () -> PAC.parse(null, UserAccount.class));
        }
    }

    @Nested
    public class PLCTest {

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

        @Test
        public void testInsert_SingleColumn() {
            SQLBuilder builder = PLC.insert("firstName");
            assertNotNull(builder);

            String sql = builder.into("userProfile").sql();
            assertTrue(sql.contains("INSERT INTO userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SQLBuilder builder = PLC.insert("firstName", "lastName", "emailAddress");
            assertNotNull(builder);

            String sql = builder.into("userProfile").sql();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "isActive");
            SQLBuilder builder = PLC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("userProfile").sql();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("isActive"));
        }

        @Test
        public void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            props.put("isActive", true);

            SQLBuilder builder = PLC.insert(props);
            assertNotNull(builder);

            SP pair = builder.into("userProfile").build();
            assertTrue(pair.sql.contains("firstName"));
            assertTrue(pair.sql.contains("lastName"));
            assertTrue(pair.sql.contains("isActive"));
            assertEquals(3, pair.parameters.size());
        }

        @Test
        public void testInsert_Entity() {
            UserProfile profile = new UserProfile("John", "Doe");
            profile.setEmailAddress("john@example.com");
            profile.setIsActive(true);

            SQLBuilder builder = PLC.insert(profile);
            assertNotNull(builder);

            SP pair = builder.into("userProfile").build();
            assertTrue(pair.sql.contains("firstName"));
            assertTrue(pair.sql.contains("lastName"));
            assertTrue(pair.sql.contains("emailAddress"));
            assertTrue(pair.sql.contains("isActive"));
            assertFalse(pair.sql.contains("lastLoginDate")); // ReadOnly
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            UserProfile profile = new UserProfile("John", "Doe");
            profile.setEmailAddress("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("emailAddress"));

            SQLBuilder builder = PLC.insert(profile, excludedProps);
            assertNotNull(builder);

            SP pair = builder.into("userProfile").build();
            assertTrue(pair.sql.contains("firstName"));
            assertTrue(pair.sql.contains("lastName"));
            assertFalse(pair.sql.contains("emailAddress"));
        }

        @Test
        public void testInsert_Class() {
            SQLBuilder builder = PLC.insert(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.into("userProfile").sql();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertTrue(sql.contains("isActive"));
            assertFalse(sql.contains("lastLoginDate")); // ReadOnly
            assertFalse(sql.contains("sessionData")); // Transient
        }

        @Test
        public void testInsertInto_Class() {
            SQLBuilder builder = PLC.insertInto(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("INSERT INTO userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testBatchInsert() {
            List<UserProfile> profiles = Arrays.asList(new UserProfile("John", "Doe"), new UserProfile("Jane", "Smith"));

            SQLBuilder builder = PLC.batchInsert(profiles);
            assertNotNull(builder);

            SP pair = builder.into("userProfile").build();
            assertTrue(pair.sql.contains("VALUES"));
            assertTrue(pair.sql.contains("(?, ?)"));
            assertTrue(pair.sql.contains(", (?, ?)"));
        }

        @Test
        public void testUpdate_TableName() {
            SQLBuilder builder = PLC.update("userProfile");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("UPDATE userProfile"));
            assertTrue(sql.contains("firstName = ?"));
        }

        @Test
        public void testUpdate_EntityClass() {
            SQLBuilder builder = PLC.update(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("UPDATE userProfile"));
            assertTrue(sql.contains("firstName = ?"));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SQLBuilder builder = PLC.deleteFrom("userProfile");
            assertNotNull(builder);

            String sql = builder.where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("DELETE FROM userProfile"));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SQLBuilder builder = PLC.deleteFrom(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.where(ConditionFactory.eq("id", 1)).sql();
            assertTrue(sql.contains("DELETE FROM userProfile"));
        }

        @Test
        public void testSelect_SingleColumn() {
            SQLBuilder builder = PLC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("userProfile").sql();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SQLBuilder builder = PLC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("userProfile").sql();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SQLBuilder builder = PLC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("userProfile").sql();
            assertTrue(sql.contains("firstName AS \"fname\""));
            assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        public void testSelect_EntityClass() {
            SQLBuilder builder = PLC.select(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.from("userProfile").sql();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
            assertTrue(sql.contains("isActive"));
            assertFalse(sql.contains("sessionData")); // Transient
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SQLBuilder builder = PLC.selectFrom(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SQLBuilder builder = PLC.selectFrom(UserProfile.class, "p");
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("FROM userProfile p"));
            assertTrue(sql.contains("p.firstName"));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SQLBuilder builder = PLC.selectFrom(UserProfile.class, "p1", "profile1", UserProfile.class, "p2", "profile2");
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("p1.firstName AS \"profile1.firstName\""));
            assertTrue(sql.contains("p2.firstName AS \"profile2.firstName\""));
        }

        @Test
        public void testCount_TableName() {
            SQLBuilder builder = PLC.count("userProfile");
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        public void testCount_EntityClass() {
            SQLBuilder builder = PLC.count(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        public void testParse() {
            Condition cond = ConditionFactory.and(ConditionFactory.eq("firstName", "John"), ConditionFactory.eq("isActive", true));

            SQLBuilder builder = PLC.parse(cond, UserProfile.class);
            assertNotNull(builder);

            String sql = builder.sql();
            assertTrue(sql.contains("firstName = ?"));
            assertTrue(sql.contains("AND"));
            assertTrue(sql.contains("isActive = ?"));
        }

        @Test
        public void testCreateInstance() {
            PLC instance1 = PLC.createInstance();
            PLC instance2 = PLC.createInstance();

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertNotSame(instance1, instance2);
        }

        @Test
        public void testNamingPolicy() {
            // Test that PLC uses lowerCamelCase naming (no transformation)
            SQLBuilder builder = PLC.select("firstName", "lastName", "emailAddress", "isActive");
            String sql = builder.from("userProfile").sql();

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
        public void testComplexQueries() {
            // Test complex query with multiple selections
            List<Selection> selections = Arrays.asList(new Selection(UserProfile.class, "p1", "profile1", Arrays.asList("id", "firstName"), false, null),
                    new Selection(UserProfile.class, "p2", "profile2", null, false, new HashSet<>(Arrays.asList("sessionData", "lastLoginDate"))));

            SQLBuilder builder = PLC.select(selections);
            String sql = builder.from("userProfile p1, userProfile p2").sql();

            assertTrue(sql.contains("p1.id AS \"profile1.id\""));
            assertTrue(sql.contains("p1.firstName AS \"profile1.firstName\""));
            assertFalse(sql.contains("profile2.sessionData"));
            assertFalse(sql.contains("profile2.lastLoginDate"));
        }

        @Test
        public void testAllMethodOverloads() {
            // Test all overloaded methods
            assertNotNull(PLC.insert(UserProfile.class, new HashSet<>()));
            assertNotNull(PLC.insertInto(UserProfile.class, new HashSet<>()));
            assertNotNull(PLC.update("userProfile", UserProfile.class));
            assertNotNull(PLC.update(UserProfile.class, new HashSet<>()));
            assertNotNull(PLC.deleteFrom("userProfile", UserProfile.class));
            assertNotNull(PLC.select(UserProfile.class, true));
            assertNotNull(PLC.select(UserProfile.class, new HashSet<>()));
            assertNotNull(PLC.select(UserProfile.class, true, new HashSet<>()));
            assertNotNull(PLC.selectFrom(UserProfile.class, true));
            assertNotNull(PLC.selectFrom(UserProfile.class, "p", true));
            assertNotNull(PLC.selectFrom(UserProfile.class, new HashSet<>()));
            assertNotNull(PLC.selectFrom(UserProfile.class, "p", new HashSet<>()));
            assertNotNull(PLC.selectFrom(UserProfile.class, true, new HashSet<>()));
            assertNotNull(PLC.selectFrom(UserProfile.class, "p", true, new HashSet<>()));
        }

        @Test
        public void testErrorCases() {
            // Test error cases
            assertThrows(IllegalArgumentException.class, () -> PLC.insert(""));
            assertThrows(IllegalArgumentException.class, () -> PLC.insert(new String[0]));
            assertThrows(IllegalArgumentException.class, () -> PLC.insert(new ArrayList<String>()));
            assertThrows(IllegalArgumentException.class, () -> PLC.insert(new HashMap<String, Object>()));
            assertThrows(IllegalArgumentException.class, () -> PLC.batchInsert(new ArrayList<>()));
            assertThrows(IllegalArgumentException.class, () -> PLC.update(""));
            assertThrows(IllegalArgumentException.class, () -> PLC.deleteFrom(""));
            assertThrows(IllegalArgumentException.class, () -> PLC.select(""));
            assertThrows(IllegalArgumentException.class, () -> PLC.select(new String[0]));
            assertThrows(IllegalArgumentException.class, () -> PLC.select(new ArrayList<String>()));
            assertThrows(IllegalArgumentException.class, () -> PLC.select(new HashMap<String, String>()));
            assertThrows(IllegalArgumentException.class, () -> PLC.select(new ArrayList<Selection>()));
            assertThrows(IllegalArgumentException.class, () -> PLC.selectFrom(new ArrayList<Selection>()));
            assertThrows(IllegalArgumentException.class, () -> PLC.count(""));
        }
    }
}