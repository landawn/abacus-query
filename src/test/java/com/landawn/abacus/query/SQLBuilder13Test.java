package com.landawn.abacus.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.query.SQLBuilder.NAC;
import com.landawn.abacus.query.SQLBuilder.NLC;
import com.landawn.abacus.query.SQLBuilder.NSB;
import com.landawn.abacus.query.SQLBuilder.NSC;
import com.landawn.abacus.query.SQLBuilder.PSC;
import com.landawn.abacus.query.SQLBuilder10Test.Order;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.util.N;

/**
 * Unit tests for SQLBuilder class
 */
public class SQLBuilder13Test extends TestBase {

    @Nested
    public class NSBTest {

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

        @Test
        public void testInsertWithSingleExpression() {
            String sql = NSB.insert("user_name").into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("user_name"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains(":user_name"));
        }

        @Test
        public void testInsertWithMultipleColumns() {
            String sql = NSB.insert("first_name", "last_name", "email").into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains(":first_name"));
            Assertions.assertTrue(sql.contains(":last_name"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "created_date");
            String sql = NSB.insert(columns).into("products").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO products"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("name"));
            Assertions.assertTrue(sql.contains("created_date"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("username", "john_doe");
            data.put("age", 25);
            String sql = NSB.insert(data).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("username"));
            Assertions.assertTrue(sql.contains("age"));
            Assertions.assertTrue(sql.contains(":username"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setEmail("john@example.com");
            user.setPassword("secret");
            user.setCreatedDate(new Date());
            user.setTempData("temp");

            String sql = NSB.insert(user).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            // Should include regular fields
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("password"));
            // Should NOT include @ReadOnly or @Transient fields
            Assertions.assertFalse(sql.contains("createdDate"));
            Assertions.assertFalse(sql.contains("tempData"));
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setEmail("john@example.com");
            user.setPassword("secret");

            Set<String> exclude = N.asSet("password");
            String sql = NSB.insert(user, exclude).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = NSB.insert(User.class).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            // Should include insertable fields
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("password"));
            // Should NOT include @ReadOnly or @Transient fields
            Assertions.assertFalse(sql.contains("createdDate"));
            Assertions.assertFalse(sql.contains("tempData"));
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> exclude = N.asSet("id", "password");
            String sql = NSB.insert(User.class, exclude).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testInsertInto() {
            String sql = NSB.insertInto(User.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO test_user"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> exclude = N.asSet("id");
            String sql = NSB.insertInto(User.class, exclude).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO test_user"));
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
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

            String sql = NSB.batchInsert(users).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            // Should have multiple value sets
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testUpdate() {
            String sql = NSB.update("users").set("last_login", "status").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("last_login"));
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains(":id"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = NSB.update("user_accounts", User.class).set("firstName", "John").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE user_accounts"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = NSB.update(User.class).set("firstName", "email").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE test_user"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> exclude = N.asSet("password", "createdDate");
            String sql = NSB.update(User.class, exclude).set("firstName", "email").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE test_user"));
            // Should be able to update non-excluded fields
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = NSB.deleteFrom("users").where(Filters.eq("status", "inactive")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertTrue(sql.contains(":status"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = NSB.deleteFrom("user_accounts", User.class).where(Filters.lt("lastLogin", new Date())).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM user_accounts"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("lastLogin"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = NSB.deleteFrom(User.class).where(Filters.eq("id", 123)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM test_user"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = NSB.select("COUNT(*) AS total").from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*) AS total"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = NSB.select("id", "name", "email", "created_date").from("users").where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("created_date"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("active"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = NSB.select(columns).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("u.first_name", "firstName");
            aliases.put("u.last_name", "lastName");
            aliases.put("COUNT(o.id)", "orderCount");

            String sql = NSB.select(aliases).from("users u").leftJoin("orders o").on("u.id = o.user_id").groupBy("u.id").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("u.first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("u.last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("COUNT(o.id) AS \"orderCount\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = NSB.select(User.class).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            // Should include all non-transient fields
            Assertions.assertTrue(sql.contains("createdDate"));
            Assertions.assertTrue(sql.contains("password"));
            // Should NOT include @Transient fields
            Assertions.assertFalse(sql.contains("tempData"));
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = NSB.select(User.class, true).from("users u").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> exclude = N.asSet("password", "createdDate");
            String sql = NSB.select(User.class, exclude).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> exclude = N.asSet("password");
            String sql = NSB.select(User.class, true, exclude).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFrom() {
            String sql = NSB.selectFrom(User.class).where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM test_user"));
            Assertions.assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = NSB.selectFrom(User.class, "u").where(Filters.eq("u.active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM test_user u"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = NSB.selectFrom(User.class, true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = NSB.selectFrom(User.class, "u", true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = NSB.selectFrom(User.class, exclude).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = NSB.selectFrom(User.class, "u", exclude).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("test_user u"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = NSB.selectFrom(User.class, true, exclude).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> exclude = N.asSet("password");
            String sql = NSB.selectFrom(User.class, "u", true, exclude).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = NSB.select(User.class, "u", "user_", User.class, "u2", "user2_").from("users u").join("users u2").on("u.id = u2.parent_id").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("u.id AS \"user_.id\""));
            Assertions.assertTrue(sql.contains("u2.id AS \"user2_.id\""));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeUser = N.asSet("password");
            Set<String> excludeUser2 = N.asSet("email");

            String sql = NSB.select(User.class, "u", "user_", excludeUser, User.class, "u2", "user2_", excludeUser2)
                    .from("users u")
                    .join("users u2")
                    .on("u.id = u2.parent_id")
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, N.asSet("password")));

            String sql = NSB.select(selections).from("users u").join("users m").on("u.manager_id = m.id").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = NSB.selectFrom(User.class, "u", "user_", User.class, "m", "manager_").where(Filters.eq("u.active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = N.asSet("password");
            Set<String> excludeM = N.asSet("email", "password");

            String sql = NSB.selectFrom(User.class, "u", "user_", excludeU, User.class, "m", "manager_", excludeM).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, null));

            String sql = NSB.selectFrom(selections).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = NSB.count("users").where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = NSB.count(User.class).where(Filters.eq("status", "active")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM test_user"));
            Assertions.assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18));
            String sql = NSB.parse(cond, User.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains("age"));
            Assertions.assertTrue(sql.contains(":age"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testEmptyCollectionThrows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSB.insert(new ArrayList<String>()).into("users").query();
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSB.select(new ArrayList<String>()).from("users").query();
            });
        }

        @Test
        public void testNullArgumentsThrow() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSB.parse(null, User.class);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSB.select((String) null).from("users");
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSB.select((Collection<String>) null).from("users");
            });
        }

        @Test
        public void testJoinOperations() {
            // Test INNER JOIN
            String sql = NSB.select("*").from("users u").join("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("JOIN"));

            // Test LEFT JOIN
            sql = NSB.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            // Test RIGHT JOIN
            sql = NSB.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            // Test FULL JOIN
            sql = NSB.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            // Test CROSS JOIN
            sql = NSB.select("*").from("users").crossJoin("departments").query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            // Test NATURAL JOIN
            sql = NSB.select("*").from("users").naturalJoin("user_profiles").query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testMultipleJoins() {
            String sql = NSB.select("u.name", "o.id", "p.name")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .leftJoin("products p")
                    .on("o.product_id = p.id")
                    .where(Filters.eq("u.active", true))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN orders"));
            Assertions.assertTrue(sql.contains("LEFT JOIN products"));
        }

        @Test
        public void testJoinWithCondition() {
            String sql = NSB.select("*")
                    .from("users u")
                    .leftJoin("orders o")
                    .on(Filters.and(Filters.eq("u.id", "o.user_id"), Filters.gt("o.amount", 100)))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testUpdateOperations() {
            // Test simple update
            String sql = NSB.update("users").set("status", "active").where(Filters.eq("id", 1)).query();

            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status"));

            // Test update with multiple sets
            sql = NSB.update("users").set("firstName", "John").set("lastName", "Doe").set(Map.of("age", 30)).where(Filters.eq("id", 1)).query();

            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("age"));

            // Test update with Map
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("age", 25);

            sql = NSB.update("users").set(updates).where(Filters.eq("id", 2)).query();

            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("age"));
        }

        @Test
        public void testWhereOperations() {
            // Test single where
            String sql = NSB.select("*").from("users").where(Filters.eq("status", "active")).query();

            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status"));

            // Test multiple where (should be AND'ed)
            sql = NSB.select("*").from("users").where(Filters.eq("status", "active").and(Filters.gt("age", 18))).query();

            Assertions.assertTrue(sql.contains("AND"));

            // Test where with OR
            sql = NSB.select("*").from("users").where(Filters.or(Filters.eq("status", "active"), Filters.eq("status", "premium"))).query();

            Assertions.assertTrue(sql.contains("OR"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = NSB.select("department", "COUNT(*) as count").from("users").groupBy("department").having(Filters.gt("COUNT(*)", 5)).query();

            Assertions.assertTrue(sql.contains("GROUP BY department"));
            Assertions.assertTrue(sql.contains("HAVING"));

            // Test multiple group by
            sql = NSB.select("department", "location", "COUNT(*)").from("users").groupBy("department", "location").query();

            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("department"));
            Assertions.assertTrue(sql.contains("location"));
        }

        @Test
        public void testOrderBy() {
            // Test simple order by
            String sql = NSB.select("*").from("users").orderBy("name").query();

            Assertions.assertTrue(sql.contains("ORDER BY name"));

            // Test order by with direction
            sql = NSB.select("*").from("users").orderBy("name ASC", "age DESC").query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("name ASC"));
            Assertions.assertTrue(sql.contains("age DESC"));
        }

        @Test
        public void testLimitOffset() {
            // Test limit only
            String sql = NSB.select("*").from("users").limit(10).query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));

            // Test limit with offset
            sql = NSB.select("*").from("users").limit(10).offset(20).query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with count and offset
            sql = NSB.select("*").from("users").limit(10, 5).query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testForUpdate() {
            String sql = NSB.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testUnion() {
            NSB builder1 = (NSB) NSB.select("name").from("users");
            NSB builder2 = (NSB) NSB.select("name").from("customers");

            String sql1 = builder1.query();
            String sql2 = builder2.query();

            // Union would be manual construction
            String unionSql = sql1 + " UNION " + sql2;
            Assertions.assertTrue(unionSql.contains("UNION"));
        }

        @Test
        public void testAppend() {
            String sql = NSB.select("*").from("users").append(" WHERE custom_condition = true").query();

            Assertions.assertTrue(sql.contains("custom_condition"));

            // Test multiple appends
            sql = NSB.select("*").from("users").append(" WHERE 1=1").append(" AND status = 'active'").append(" ORDER BY name").query();

            Assertions.assertTrue(sql.contains("1=1"));
            Assertions.assertTrue(sql.contains("status = 'active'"));
            Assertions.assertTrue(sql.contains("ORDER BY name"));
        }

        @Test
        public void testComplexConditions() {
            // Test nested AND/OR
            String sql = NSB.select("*")
                    .from("users")
                    .where(Filters.or(Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18), Filters.lt("age", 65)),
                            Filters.and(Filters.eq("status", "premium"), Filters.isNotNull("subscription_id"))))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = NSB.select("*").from("users").where(Filters.isNull("email")).query();
            Assertions.assertTrue(sql.contains("IS NULL"));

            // Test IS NOT NULL
            sql = NSB.select("*").from("users").where(Filters.isNotNull("email")).query();
            Assertions.assertTrue(sql.contains("IS NOT NULL"));

            // Test IN
            sql = NSB.select("*").from("users").where(Filters.in("status", Arrays.asList("active", "premium", "trial"))).query();
            Assertions.assertTrue(sql.contains("IN"));

            // Test NOT IN
            sql = NSB.select("*").from("users").where(Filters.notIn("status", Arrays.asList("inactive", "banned"))).query();
            Assertions.assertTrue(sql.contains("NOT IN"));

            // Test BETWEEN
            sql = NSB.select("*").from("users").where(Filters.between("age", 18, 65)).query();
            Assertions.assertTrue(sql.contains("BETWEEN"));

            // Test LIKE
            sql = NSB.select("*").from("users").where(Filters.like("name", "%John%")).query();
            Assertions.assertTrue(sql.contains("LIKE"));

            // Test NOT LIKE
            sql = NSB.select("*").from("users").where(Filters.notLike("email", "%spam%")).query();
            Assertions.assertTrue(sql.contains("NOT LIKE"));
        }

        @Test
        public void testInsertReturning() {
            // Some databases support RETURNING clause
            String sql = NSB.insert("firstName", "lastName").into("users").append(" RETURNING id").query();

            Assertions.assertTrue(sql.contains("RETURNING id"));
        }

        @Test
        public void testCaseExpression() {
            String sql = NSB.select("name", "CASE WHEN age < 18 THEN 'Minor' WHEN age < 65 THEN 'Adult' ELSE 'Senior' END as category").from("users").query();

            Assertions.assertTrue(sql.contains("CASE"));
            Assertions.assertTrue(sql.contains("WHEN"));
            Assertions.assertTrue(sql.contains("THEN"));
            Assertions.assertTrue(sql.contains("ELSE"));
        }

        @Test
        public void testWindowFunctions() {
            String sql = NSB.select("name", "salary", "ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) as rank").from("employees").query();

            Assertions.assertTrue(sql.contains("ROW_NUMBER()"));
            Assertions.assertTrue(sql.contains("OVER"));
            Assertions.assertTrue(sql.contains("PARTITION BY"));
        }

        @Test
        public void testDistinct() {
            String sql = NSB.select("DISTINCT department").from("users").query();

            Assertions.assertTrue(sql.contains("DISTINCT"));

            // Test with multiple columns
            sql = NSB.select("DISTINCT department", "location").from("users").query();

            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testAggregateFunctions() {
            String sql = NSB
                    .select("COUNT(*) as total", "AVG(salary) as avg_salary", "MAX(salary) as max_salary", "MIN(salary) as min_salary",
                            "SUM(salary) as total_salary")
                    .from("employees")
                    .query();

            Assertions.assertTrue(sql.contains("COUNT(*)"));
            Assertions.assertTrue(sql.contains("AVG(salary)"));
            Assertions.assertTrue(sql.contains("MAX(salary)"));
            Assertions.assertTrue(sql.contains("MIN(salary)"));
            Assertions.assertTrue(sql.contains("SUM(salary)"));
        }

        @Test
        public void testNamedParametersConsistency() {
            // Ensure named parameters use consistent format
            String sql = NSB.select("*")
                    .from("users")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.eq("lastName", "Doe"), Filters.gt("age", 18)))
                    .query();

            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testEntityClassPropertyMapping() {
            // Ensure properties are correctly mapped
            User user = new User();
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmail("test@example.com");

            String sql = NSB.insert(user).into("test_user").query();

            // Should use property names as parameter names
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testComplexMultiTableQuery() {
            String sql = NSB.select("u.name as userName", "d.name as departmentName", "COUNT(o.id) as orderCount", "SUM(o.total) as totalRevenue")
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
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("JOIN"));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }
    }

    @Nested
    public class NSCTest {

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

        @Test
        public void testInsertSingleExpression() {
            String sql = NSC.insert("name").into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(name)"));
            Assertions.assertTrue(sql.contains("VALUES (:name)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = NSC.insert("firstName", "lastName", "email").into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = NSC.insert(columns).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains(":firstName"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            String sql = NSC.insert(props).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail("john@example.com");
            user.setCreatedDate(new Date()); // Should be excluded (ReadOnly)
            user.setTempField("temp"); // Should be excluded (Transient)

            String sql = NSC.insert(user).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("created_date"));
            Assertions.assertFalse(sql.contains("temp_field"));
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail("john@example.com");

            Set<String> excluded = Set.of("email");
            String sql = NSC.insert(user, excluded).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = NSC.insert(User.class).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertFalse(sql.contains("created_date"));
            Assertions.assertFalse(sql.contains("temp_field"));
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "status");
            String sql = NSC.insert(User.class, excluded).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertFalse(sql.contains("status"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertInto() {
            String sql = NSC.insertInto(User.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = NSC.insertInto(User.class, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testBatchInsert() {
            List<User> users = Arrays.asList(new User() {
                {
                    setFirstName("John");
                    setLastName("Doe");
                }
            }, new User() {
                {
                    setFirstName("Jane");
                    setLastName("Smith");
                }
            });

            String sql = NSC.batchInsert(users).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            // Should have multiple parameter sets for batch insert
        }

        @Test
        public void testUpdate() {
            String sql = NSC.update("users").set("firstName", "John").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = NSC.update("users", User.class).set("status").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = NSC.update(User.class).set("firstName", "email").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdDate", "status");
            String sql = NSC.update(User.class, excluded).set("firstName").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = NSC.deleteFrom("users").where(Filters.eq("status", "INACTIVE")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = NSC.deleteFrom("users", User.class).where(Filters.eq("status", "INACTIVE")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = NSC.deleteFrom(User.class).where(Filters.eq("status", "INACTIVE")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testSelectSinglePart() {
            String sql = NSC.select("COUNT(*)").from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = NSC.select("firstName", "lastName", "email").from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = NSC.select(columns).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = NSC.select(aliases).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name AS \"fname\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = NSC.select(User.class).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("created_date AS \"createdDate\""));
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertFalse(sql.contains("temp_field"));
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = NSC.select(User.class, true).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdDate", "status");
            String sql = NSC.select(User.class, excluded).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("created_date"));
            Assertions.assertFalse(sql.contains("status"));
            Assertions.assertTrue(sql.contains("first_name"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.select(User.class, true, excluded).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFrom() {
            String sql = NSC.selectFrom(User.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = NSC.selectFrom(User.class, "u").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM users u"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = NSC.selectFrom(User.class, true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = NSC.selectFrom(User.class, "u", true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, "u", excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("users u"));
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, true, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, "u", true, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = NSC.select(User.class, "u", "user", User.class, "m", "manager").from("users u").join("users m").on("u.manager_id = m.id").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("u.id AS \"user.id\""));
            Assertions.assertTrue(sql.contains("m.id AS \"manager.id\""));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeU = Set.of("status");
            Set<String> excludeM = Set.of("email");

            String sql = NSC.select(User.class, "u", "user", excludeU, User.class, "m", "manager", excludeM)
                    .from("users u")
                    .join("users m")
                    .on("u.manager_id = m.id")
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, Set.of("status")));

            String sql = NSC.select(selections).from("users u").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = NSC.selectFrom(User.class, "u", "user", User.class, "m", "manager").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = Set.of("status");
            Set<String> excludeM = Set.of("email");

            String sql = NSC.selectFrom(User.class, "u", "user", excludeU, User.class, "m", "manager", excludeM).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, null));

            String sql = NSC.selectFrom(selections).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = NSC.count("users").where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains(":active"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = NSC.count(User.class).where(Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18))).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("age > :age"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18), Filters.like("email", "%@example.com"));
            String sql = NSC.parse(cond, User.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("age > :age"));
            Assertions.assertTrue(sql.contains("email LIKE :email"));
        }

        @Test
        public void testNamingPolicySnakeCase() {
            // NSC uses SNAKE_CASE naming policy
            String sql = NSC.select("firstName", "lastName").from("userAccounts").query();

            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("userAccounts"));
        }

        @Test
        public void testComplexQueryWithJoins() {
            String sql = NSC.select("u.firstName", "u.lastName", "COUNT(o.id)")
                    .from("users u")
                    .leftJoin("orders o")
                    .on("u.id = o.user_id")
                    .where(Filters.eq("u.active", true))
                    .groupBy("u.id", "u.firstName", "u.lastName")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("COUNT(o.id) DESC")
                    .limit(10)
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("u.first_name AS \"u.firstName\""));
            Assertions.assertTrue(sql.contains("u.last_name AS \"u.lastName\""));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = NSC.update("users").set(updates).where(Filters.eq("id", 1)).query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("last_name = :lastName"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = NSC.batchInsert(data).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testNamedParametersAreCorrect() {
            // Ensure named parameters follow the correct pattern
            String sql = NSC.select("*")
                    .from("users")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.in("status", Arrays.asList("ACTIVE", "PREMIUM")),
                            Filters.between("age", 18, 65)))
                    .query();

            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains("status IN (:status1, :status2)"));
            Assertions.assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
        }

        @Test
        public void testColumnNameTransformation() {
            // Test that camelCase property names are converted to snake_case
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            String sql = NSC.insert(user).into("users").query();

            // Column names should be snake_case
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));

            // Parameter names should remain camelCase
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testEmptyArgumentsThrow() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSC.select("");
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSC.select(new String[0]);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSC.select(new ArrayList<String>());
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSC.select(new HashMap<String, String>());
            });
        }

        @Test
        public void testNullConditionThrows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSC.parse(null, User.class);
            });
        }

        @Test
        public void testAllJoinTypes() {
            // Test all join variations
            String sql;

            sql = NSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = NSC.select("*").from("users u").innerJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = NSC.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = NSC.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = NSC.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = NSC.select("*").from("users").crossJoin("departments").query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = NSC.select("*").from("users").naturalJoin("user_profiles").query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = NSC.select("*").from("users").append(" WHERE MATCH(name) AGAINST (:search IN BOOLEAN MODE)").query();

            Assertions.assertTrue(sql.contains("MATCH"));
            Assertions.assertTrue(sql.contains("AGAINST"));
        }

        @Test
        public void testComplexNestedConditions() {
            String sql = NSC.select("*")
                    .from("users")
                    .where(Filters.or(Filters.and(Filters.eq("status", "ACTIVE"), Filters.or(Filters.lt("age", 18), Filters.gt("age", 65))),
                            Filters.and(Filters.eq("status", "PREMIUM"), Filters.between("age", 18, 65))))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testForUpdate() {
            String sql = NSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testLimitOffset() {
            String sql = NSC.select("*").from("users").limit(10).offset(20).query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = NSC.select("*").from("users").limit(10, 20).query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }
    }

    @Nested
    public class NACTest {

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

        @Test
        public void testInsertSingleExpression() {
            String sql = NAC.insert("FIRST_NAME").into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME)"));
            Assertions.assertTrue(sql.contains("VALUES (:FIRST_NAME)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = NAC.insert("firstName", "lastName", "email").into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = NAC.insert(columns).into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = Map.of("firstName", "John", "lastName", "Doe");
            String sql = NAC.insert(props).into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedTime(new Date()); // Should be excluded
            account.setSessionId("123"); // Should be excluded

            String sql = NAC.insert(account).into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertFalse(sql.contains("CREATED_TIME"));
            Assertions.assertFalse(sql.contains("SESSION_ID"));
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setFirstName("John");
            account.setPassword("secret");

            Set<String> exclude = Set.of("password");
            String sql = NAC.insert(account, exclude).into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = NAC.insert(Account.class).into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("ID"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertTrue(sql.contains("PASSWORD"));
            Assertions.assertFalse(sql.contains("CREATED_TIME"));
            Assertions.assertFalse(sql.contains("SESSION_ID"));
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "createdTime");
            String sql = NAC.insert(Account.class, excluded).into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("ID"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
        }

        @Test
        public void testInsertInto() {
            String sql = NAC.insertInto(Account.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = NAC.insertInto(Account.class, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertFalse(sql.contains("ID"));
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account() {
                {
                    setFirstName("John");
                    setLastName("Doe");
                }
            }, new Account() {
                {
                    setFirstName("Jane");
                    setLastName("Smith");
                }
            });

            String sql = NAC.batchInsert(accounts).into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testUpdate() {
            String sql = NAC.update("ACCOUNT").set("STATUS", "ACTIVE").where(Filters.eq("ID", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("STATUS = :STATUS"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = :ID"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = NAC.update("ACCOUNT", Account.class).set("status").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("STATUS"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = NAC.update(Account.class).set("firstName", "lastName").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdTime", "password");
            String sql = NAC.update(Account.class, excluded).set("firstName").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = NAC.deleteFrom("ACCOUNT").where(Filters.eq("STATUS", "INACTIVE")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("STATUS = :STATUS"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = NAC.deleteFrom("ACCOUNT", Account.class).where(Filters.eq("firstName", "John")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = :firstName"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = NAC.deleteFrom(Account.class).where(Filters.eq("firstName", "John")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = :firstName"));
        }

        @Test
        public void testSelectSinglePart() {
            String sql = NAC.select("COUNT(*)").from("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = NAC.select("firstName", "lastName", "email").from("ACCOUNT").where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertTrue(sql.contains("ACTIVE = :active"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = NAC.select(columns).from("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = Map.of("firstName", "fname", "lastName", "lname");
            String sql = NAC.select(aliases).from("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = NAC.select(Account.class).from("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("ID"));
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertTrue(sql.contains("CREATED_TIME AS \"createdTime\""));
            Assertions.assertTrue(sql.contains("PASSWORD"));
            Assertions.assertFalse(sql.contains("SESSION_ID"));
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = NAC.select(Account.class, true).from("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.select(Account.class, excluded).from("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.select(Account.class, true, excluded).from("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFrom() {
            String sql = NAC.selectFrom(Account.class).where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("ACTIVE = :active"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = NAC.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM ACCOUNT a"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = NAC.selectFrom(Account.class, true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = NAC.selectFrom(Account.class, "a", true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.selectFrom(Account.class, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.selectFrom(Account.class, "a", excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("ACCOUNT a"));
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.selectFrom(Account.class, true, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.selectFrom(Account.class, "a", true, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = NAC.select(Account.class, "a", "account", Account.class, "o", "order")
                    .from("ACCOUNT a")
                    .join("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("a.ID AS \"account.id\""));
            Assertions.assertTrue(sql.contains("o.ID AS \"order.id\""));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = NAC.select(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .from("ACCOUNT a")
                    .join("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "o", "order", null, true, null));
            String sql = NAC.select(selections).from("ACCOUNT a").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = NAC.selectFrom(Account.class, "a", "account", Account.class, "o", "order").where(Filters.eq("a.ID", "o.ACCOUNT_ID")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = NAC.selectFrom(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .where(Filters.eq("a.ID", "o.ACCOUNT_ID"))
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "o", "order", null, true, null));
            String sql = NAC.selectFrom(selections).where(Filters.eq("a.ID", "o.ACCOUNT_ID")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = NAC.count("ACCOUNT").where(Filters.eq("STATUS", "ACTIVE")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("STATUS = :STATUS"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = NAC.count(Account.class).where(Filters.eq("status", "ACTIVE")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("STATUS = :status"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = NAC.parse(cond, Account.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("STATUS = :status"));
            Assertions.assertTrue(sql.contains("BALANCE > :balance"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testNamingPolicyUpperCase() {
            // NAC uses SCREAMING_SNAKE_CASE naming policy
            String sql = NAC.select("firstName", "lastName").from("userAccounts").query();

            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("userAccounts"));
        }

        @Test
        public void testComplexQueryWithJoins() {
            String sql = NAC.select("a.firstName", "a.lastName", "COUNT(o.id)")
                    .from("ACCOUNT a")
                    .leftJoin("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .where(Filters.eq("a.active", true))
                    .groupBy("a.ID", "a.firstName", "a.lastName")
                    .having(Filters.gt("COUNT(o.ID)", 5))
                    .orderBy("COUNT(o.ID) DESC")
                    .limit(10)
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("a.FIRST_NAME AS \"a.firstName\""));
            Assertions.assertTrue(sql.contains("a.LAST_NAME AS \"a.lastName\""));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = NAC.update("ACCOUNT").set(updates).where(Filters.eq("ID", 1)).query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME = :firstName"));
            Assertions.assertTrue(sql.contains("LAST_NAME = :lastName"));
            Assertions.assertTrue(sql.contains("ID = :ID"));
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = NAC.batchInsert(data).into("ACCOUNT").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testNamedParametersAreCorrect() {
            String sql = NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.in("status", Arrays.asList("ACTIVE", "PREMIUM")),
                            Filters.between("age", 18, 65)))
                    .query();

            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains("AGE BETWEEN :minAge AND :maxAge"));
        }

        @Test
        public void testColumnNameTransformation() {
            // Test that camelCase property names are converted to SCREAMING_SNAKE_CASE
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");

            String sql = NAC.insert(account).into("ACCOUNT").query();

            // Column names should be UPPER_CASE
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));

            // Parameter names should remain camelCase
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testEmptyArgumentsThrow() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NAC.select("");
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NAC.select(new String[0]);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NAC.select(new ArrayList<String>());
            });
        }

        @Test
        public void testNullConditionThrows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NAC.parse(null, Account.class);
            });
        }

        @Test
        public void testAllJoinTypes() {
            String sql;

            sql = NAC.select("*").from("ACCOUNT a").join("ORDER o").on("a.ID = o.ACCOUNT_ID").query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = NAC.select("*").from("ACCOUNT a").innerJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = NAC.select("*").from("ACCOUNT a").leftJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = NAC.select("*").from("ACCOUNT a").rightJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = NAC.select("*").from("ACCOUNT a").fullJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = NAC.select("*").from("ACCOUNT").crossJoin("DEPARTMENT").query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = NAC.select("*").from("ACCOUNT").naturalJoin("ACCOUNT_PROFILE").query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = NAC.select("*").from("ACCOUNT").append(" WHERE CUSTOM_FUNCTION(NAME) = TRUE").query();

            Assertions.assertTrue(sql.contains("CUSTOM_FUNCTION"));
        }

        @Test
        public void testComplexNestedConditions() {
            String sql = NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.or(Filters.and(Filters.eq("STATUS", "ACTIVE"), Filters.or(Filters.lt("AGE", 18), Filters.gt("AGE", 65))),
                            Filters.and(Filters.eq("STATUS", "PREMIUM"), Filters.between("AGE", 18, 65))))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains(":STATUS"));
            Assertions.assertTrue(sql.contains(":AGE"));
        }

        @Test
        public void testForUpdate() {
            String sql = NAC.select("*").from("ACCOUNT").where(Filters.eq("ID", 1)).forUpdate().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testLimitOffset() {
            String sql = NAC.select("*").from("ACCOUNT").limit(10).offset(20).query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = NAC.select("*").from("ACCOUNT").limit(10, 20).query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testInsertWithUpperCaseColumns() {
            // Test that already uppercase columns remain uppercase
            String sql = NAC.insert("FIRST_NAME", "LAST_NAME").into("ACCOUNT").query();

            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains(":FIRST_NAME"));
            Assertions.assertTrue(sql.contains(":LAST_NAME"));
        }

        @Test
        public void testTableNameTransformation() {
            // Test table name is converted to uppercase
            String sql = NAC.select("*").from("userAccounts").query();
            Assertions.assertTrue(sql.contains("FROM userAccounts"));
        }

        @Test
        public void testJoinWithConditions() {
            String sql = NAC.select("*")
                    .from("ACCOUNT a")
                    .leftJoin("ORDER o")
                    .on(Filters.and(Filters.eq("a.ID", "o.ACCOUNT_ID"), Filters.gt("o.AMOUNT", 100)))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = NAC.select("DEPARTMENT", "COUNT(*) AS CNT").from("EMPLOYEE").groupBy("DEPARTMENT").having(Filters.gt("COUNT(*)", 5)).query();

            Assertions.assertTrue(sql.contains("GROUP BY DEPARTMENT"));
            Assertions.assertTrue(sql.contains("HAVING"));
        }

        @Test
        public void testOrderBy() {
            String sql = NAC.select("*").from("ACCOUNT").orderBy("LAST_NAME ASC", "FIRST_NAME ASC").query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LAST_NAME ASC"));
            Assertions.assertTrue(sql.contains("FIRST_NAME ASC"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = NAC.select("*").from("ACCOUNT").where(Filters.isNull("EMAIL")).query();
            Assertions.assertTrue(sql.contains("EMAIL IS NULL"));

            // Test IS NOT NULL
            sql = NAC.select("*").from("ACCOUNT").where(Filters.isNotNull("EMAIL")).query();
            Assertions.assertTrue(sql.contains("EMAIL IS NOT NULL"));

            // Test IN
            sql = NAC.select("*").from("ACCOUNT").where(Filters.in("STATUS", Arrays.asList("ACTIVE", "PREMIUM"))).query();
            Assertions.assertTrue(sql.contains("STATUS IN"));

            // Test BETWEEN
            sql = NAC.select("*").from("ACCOUNT").where(Filters.between("AGE", 18, 65)).query();
            Assertions.assertTrue(sql.contains("AGE BETWEEN"));

            // Test LIKE
            sql = NAC.select("*").from("ACCOUNT").where(Filters.like("NAME", "%JOHN%")).query();
            Assertions.assertTrue(sql.contains("NAME LIKE"));
        }

        @Test
        public void testMultipleWhereConditions() {
            String sql = NAC.select("*").from("ACCOUNT").where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%"))).query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("AND"));
        }

    }

    @Nested
    public class NLCTest extends TestBase {

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

        @Test
        public void testInsertSingleExpression() {
            String sql = NLC.insert("firstName").into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName)"));
            Assertions.assertTrue(sql.contains("VALUES (:firstName)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = NLC.insert("firstName", "lastName", "email").into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = NLC.insert(columns).into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = Map.of("firstName", "John", "lastName", "Doe");
            String sql = NLC.insert(props).into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithEntity() {
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");
            account.setCreatedTime(new Date()); // Should be excluded
            account.setSessionToken("123"); // Should be excluded

            String sql = NLC.insert(account).into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("createdTime"));
            Assertions.assertFalse(sql.contains("sessionToken"));
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            Account account = new Account();
            account.setFirstName("John");
            account.setPassword("secret");

            Set<String> exclude = Set.of("password");
            String sql = NLC.insert(account, exclude).into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = NLC.insert(Account.class).into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("password"));
            Assertions.assertFalse(sql.contains("createdTime"));
            Assertions.assertFalse(sql.contains("sessionToken"));
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "createdTime");
            String sql = NLC.insert(Account.class, excluded).into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testInsertInto() {
            String sql = NLC.insertInto(Account.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = NLC.insertInto(Account.class, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account() {
                {
                    setFirstName("John");
                    setLastName("Doe");
                }
            }, new Account() {
                {
                    setFirstName("Jane");
                    setLastName("Smith");
                }
            });

            String sql = NLC.batchInsert(accounts).into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testUpdate() {
            String sql = NLC.update("account").set("status", "active").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status = :status"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = NLC.update("account", Account.class).set("status").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = NLC.update(Account.class).set("firstName", "lastName").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdTime", "password");
            String sql = NLC.update(Account.class, excluded).set("firstName").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = NLC.deleteFrom("account").where(Filters.eq("status", "inactive")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = NLC.deleteFrom("account", Account.class).where(Filters.eq("status", "inactive")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = NLC.deleteFrom(Account.class).where(Filters.eq("status", "inactive")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testSelectSinglePart() {
            String sql = NLC.select("COUNT(*)").from("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = NLC.select("firstName", "lastName", "email").from("account").where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("active = :active"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = NLC.select(columns).from("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = Map.of("firstName", "fname", "lastName", "lname");
            String sql = NLC.select(aliases).from("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName AS \"fname\""));
            Assertions.assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = NLC.select(Account.class).from("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("createdTime"));
            Assertions.assertTrue(sql.contains("password"));
            Assertions.assertFalse(sql.contains("sessionToken"));
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = NLC.select(Account.class, true).from("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password", "createdTime");
            String sql = NLC.select(Account.class, excluded).from("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("createdTime"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.select(Account.class, true, excluded).from("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFrom() {
            String sql = NLC.selectFrom(Account.class).where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("active = :active"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = NLC.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM account a"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = NLC.selectFrom(Account.class, true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = NLC.selectFrom(Account.class, "a", true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.selectFrom(Account.class, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.selectFrom(Account.class, "a", excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("account a"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.selectFrom(Account.class, true, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.selectFrom(Account.class, "a", true, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = NLC.select(Account.class, "a", "account", Account.class, "o", "order")
                    .from("account a")
                    .join("order o")
                    .on("a.id = o.accountId")
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("a.id AS \"account.id\""));
            Assertions.assertTrue(sql.contains("o.id AS \"order.id\""));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = NLC.select(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .from("account a")
                    .join("order o")
                    .on("a.id = o.accountId")
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "o", "order", null, true, null));
            String sql = NLC.select(selections).from("account a").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = NLC.selectFrom(Account.class, "a", "account", Account.class, "o", "order").where(Filters.eq("a.id", "o.accountId")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = NLC.selectFrom(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .where(Filters.eq("a.id", "o.accountId"))
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "o", "order", null, true, null));
            String sql = NLC.selectFrom(selections).where(Filters.eq("a.id", "o.accountId")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = NLC.count("account").where(Filters.eq("status", "active")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = NLC.count(Account.class).where(Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000))).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
            Assertions.assertTrue(sql.contains("balance > :balance"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000));
            String sql = NLC.parse(cond, Account.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("status = :status"));
            Assertions.assertTrue(sql.contains("balance > :balance"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testNamingPolicyCamelCase() {
            // NLC uses CAMEL_CASE naming policy
            String sql = NLC.select("firstName", "lastName").from("userAccounts").query();

            // Should preserve camelCase
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("userAccounts"));
        }

        @Test
        public void testComplexQueryWithJoins() {
            String sql = NLC.select("a.firstName", "a.lastName", "COUNT(o.id)")
                    .from("account a")
                    .leftJoin("order o")
                    .on("a.id = o.accountId")
                    .where(Filters.eq("a.active", true))
                    .groupBy("a.id", "a.firstName", "a.lastName")
                    .having(Filters.gt("COUNT(o.id)", 5))
                    .orderBy("COUNT(o.id) DESC")
                    .limit(10)
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("a.firstName"));
            Assertions.assertTrue(sql.contains("a.lastName"));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = NLC.update("account").set(updates).where(Filters.eq("id", 1)).query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName = :firstName"));
            Assertions.assertTrue(sql.contains("lastName = :lastName"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = NLC.batchInsert(data).into("account").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testNamedParametersAreCorrect() {
            String sql = NLC.select("*")
                    .from("account")
                    .where(Filters.and(Filters.eq("firstName", "John"), Filters.in("status", Arrays.asList("active", "premium")),
                            Filters.between("age", 18, 65)))
                    .query();

            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains("status IN (:status1, :status2)"));
            Assertions.assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
        }

        @Test
        public void testColumnNamePreservation() {
            // Test that camelCase property names are preserved
            Account account = new Account();
            account.setFirstName("John");
            account.setLastName("Doe");

            String sql = NLC.insert(account).into("account").query();

            // Column names should remain camelCase
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));

            // Parameter names should also be camelCase
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testEmptyArgumentsThrow() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NLC.select("");
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NLC.select(new String[0]);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NLC.select(new ArrayList<String>());
            });
        }

        @Test
        public void testNullConditionThrows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NLC.parse(null, Account.class);
            });
        }

        @Test
        public void testAllJoinTypes() {
            String sql;

            sql = NLC.select("*").from("account a").join("order o").on("a.id = o.accountId").query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = NLC.select("*").from("account a").innerJoin("order o").on("a.id = o.accountId").query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = NLC.select("*").from("account a").leftJoin("order o").on("a.id = o.accountId").query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = NLC.select("*").from("account a").rightJoin("order o").on("a.id = o.accountId").query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = NLC.select("*").from("account a").fullJoin("order o").on("a.id = o.accountId").query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = NLC.select("*").from("account").crossJoin("department").query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = NLC.select("*").from("account").naturalJoin("accountProfile").query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = NLC.select("*").from("account").append(" WHERE customFunction(name) = true").query();

            Assertions.assertTrue(sql.contains("customFunction"));
        }

        @Test
        public void testComplexNestedConditions() {
            String sql = NLC.select("*")
                    .from("account")
                    .where(Filters.or(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.lt("age", 18), Filters.gt("age", 65))),
                            Filters.and(Filters.eq("status", "premium"), Filters.between("age", 18, 65))))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testForUpdate() {
            String sql = NLC.select("*").from("account").where(Filters.eq("id", 1)).forUpdate().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testLimitOffset() {
            String sql = NLC.select("*").from("account").limit(10).offset(20).query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = NLC.select("*").from("account").limit(10, 20).query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testTableNamePreservation() {
            // Test table name preserves camelCase
            String sql = NLC.select("*").from("userAccounts").query();
            Assertions.assertTrue(sql.contains("FROM userAccounts"));
        }

        @Test
        public void testJoinWithConditions() {
            String sql = NLC.select("*")
                    .from("account a")
                    .leftJoin("order o")
                    .on(Filters.and(Filters.eq("a.id", "o.accountId"), Filters.gt("o.amount", 100)))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = NLC.select("department", "COUNT(*) as cnt").from("employee").groupBy("department").having(Filters.gt("COUNT(*)", 5)).query();

            Assertions.assertTrue(sql.contains("GROUP BY department"));
            Assertions.assertTrue(sql.contains("HAVING"));
        }

        @Test
        public void testOrderBy() {
            String sql = NLC.select("*").from("account").orderBy("lastName ASC", "firstName ASC").query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("lastName ASC"));
            Assertions.assertTrue(sql.contains("firstName ASC"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = NLC.select("*").from("account").where(Filters.isNull("email")).query();
            Assertions.assertTrue(sql.contains("email IS NULL"));

            // Test IS NOT NULL
            sql = NLC.select("*").from("account").where(Filters.isNotNull("email")).query();
            Assertions.assertTrue(sql.contains("email IS NOT NULL"));

            // Test IN
            sql = NLC.select("*").from("account").where(Filters.in("status", Arrays.asList("active", "premium"))).query();
            Assertions.assertTrue(sql.contains("status IN"));

            // Test BETWEEN
            sql = NLC.select("*").from("account").where(Filters.between("age", 18, 65)).query();
            Assertions.assertTrue(sql.contains("age BETWEEN"));

            // Test LIKE
            sql = NLC.select("*").from("account").where(Filters.like("name", "%John%")).query();
            Assertions.assertTrue(sql.contains("name LIKE"));
        }

        @Test
        public void testMultipleWhereConditions() {
            String sql = NLC.select("*").from("account").where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%"))).query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testDistinct() {
            String sql = NLC.select("DISTINCT department").from("employee").query();

            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testAggregateFunctions() {
            String sql = NLC
                    .select("COUNT(*) as total", "AVG(salary) as avgSalary", "MAX(salary) as maxSalary", "MIN(salary) as minSalary",
                            "SUM(salary) as totalSalary")
                    .from("employee")
                    .query();

            Assertions.assertTrue(sql.contains("COUNT(*)"));
            Assertions.assertTrue(sql.contains("AVG(salary)"));
            Assertions.assertTrue(sql.contains("MAX(salary)"));
            Assertions.assertTrue(sql.contains("MIN(salary)"));
            Assertions.assertTrue(sql.contains("SUM(salary)"));
        }

        @Test
        public void testWindowFunctions() {
            String sql = NLC.select("name", "salary", "ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) as rank").from("employee").query();

            Assertions.assertTrue(sql.contains("OVER (PARTITION BY department ORDER BY salary DESC) AS rank"));
        }

        @Test
        public void testCaseExpression() {
            String sql = NLC.select("name", "CASE WHEN age < 18 THEN 'Minor' WHEN age < 65 THEN 'Adult' ELSE 'Senior' END as category").from("users").query();

            Assertions.assertTrue(sql.contains("case when age < 18 then 'Minor' when age < 65 then 'Adult' else 'Senior' end AS category"));
        }

        @Test
        public void testInsertReturning() {
            // Some databases support RETURNING clause
            String sql = NLC.insert("firstName", "lastName").into("users").append(" RETURNING id").query();

            Assertions.assertTrue(sql.contains("RETURNING id"));
        }

        @Test
        public void testUpdateMultipleColumns() {
            String sql = NLC.update("users")
                    .set("firstName", "John")
                    .set("lastName", "Doe")
                    .set(Map.of("age", 30))
                    .set("email", "john@example.com")
                    .where(Filters.eq("id", 1))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("age"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testComplexMultiTableQuery() {
            String sql = NLC.select("u.name as userName", "d.name as departmentName", "COUNT(o.id) as orderCount", "SUM(o.total) as totalRevenue")
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
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("JOIN"));
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUnionAll() {
            // Test building queries that could be used in UNION ALL
            String sql1 = NLC.select("name", "email").from("users").query();
            String sql2 = NLC.select("name", "email").from("customers").query();

            // Manual UNION ALL construction
            String unionAllSql = sql1 + " UNION ALL " + sql2;
            Assertions.assertTrue(unionAllSql.contains("UNION ALL"));
        }

        @Test
        public void testEntityClassPropertyMapping() {
            // Ensure properties are correctly mapped
            Account account = new Account();
            account.setFirstName("Test");
            account.setLastName("User");
            account.setEmail("test@example.com");

            String sql = NLC.insert(account).into("account").query();

            // Should use property names as both column and parameter names
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));

            Date startDate = new Date();
            Date endDate = new Date(startDate.getTime() + 86400000); // 1 day later

            sql = NLC.selectFrom(Order.class, "ord", true).where(Filters.between("ord.orderDate", startDate, endDate)).query();
            // Output: SELECT ord.id, ord.orderNumber, ord.amount, ord.status,
            //                acc.id AS "account.id", acc.firstName AS "account.firstName"
            //         FROM orders ord
            //         LEFT JOIN account acc ON ord.accountId = acc.id
            //         WHERE ord.orderDate BETWEEN :startDate AND :endDate
            N.println(sql);
        }
    }

    @Nested
    public class PSCTest extends TestBase {

        @Table("users")
        public static class User {
            private long id;
            @Column("first_name")
            private String firstName;
            @Column("last_name")
            private String lastName;
            private String email;
            @ReadOnly
            private Date created_date;
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

            public Date getCreated_date() {
                return created_date;
            }

            public void setCreated_date(Date created_date) {
                this.created_date = created_date;
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

        @Test
        public void testInsertSingleExpression() {
            String sql = PSC.insert("name").into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(name)"));
            Assertions.assertTrue(sql.contains("VALUES (?)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = PSC.insert("firstName", "lastName", "email").into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("VALUES (?, ?, ?)"));
        }

        @Test
        public void testInsertWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = PSC.insert(columns).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("VALUES (?, ?, ?)"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");
            String sql = PSC.insert(props).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("VALUES (?, ?)"));
        }

        @Test
        public void testInsertWithEntity() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail("john@example.com");
            user.setCreated_date(new Date()); // Should be excluded (ReadOnly)
            user.setTempData("temp"); // Should be excluded (Transient)

            String sql = PSC.insert(user).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("created_date"));
            Assertions.assertFalse(sql.contains("temp_data"));
        }

        @Test
        public void testInsertWithEntityAndExclusions() {
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail("john@example.com");

            Set<String> excluded = Set.of("email");
            String sql = PSC.insert(user, excluded).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = PSC.insert(User.class).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("password"));
            Assertions.assertFalse(sql.contains("created_date"));
            Assertions.assertFalse(sql.contains("temp_data"));
        }

        @Test
        public void testInsertWithEntityClassAndExclusions() {
            Set<String> excluded = Set.of("id", "password");
            String sql = PSC.insert(User.class, excluded).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertInto() {
            String sql = PSC.insertInto(User.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = PSC.insertInto(User.class, excluded).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
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

            String sql = PSC.batchInsert(users).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            // Should have multiple value sets with ?
            Assertions.assertTrue(sql.contains("(?, ?)"));
        }

        @Test
        public void testUpdate() {
            String sql = PSC.update("users").set("last_login", "status").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("last_login = ?"));
            Assertions.assertTrue(sql.contains("status = ?"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = ?"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = PSC.update("users", User.class).set("firstName", "John").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = PSC.update(User.class).set("firstName", "email").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name = ?"));
            Assertions.assertTrue(sql.contains("email = ?"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password", "created_date");
            String sql = PSC.update(User.class, excluded).set("firstName", "email").where(Filters.eq("id", 1)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name = ?"));
            Assertions.assertTrue(sql.contains("email = ?"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = PSC.deleteFrom("users").where(Filters.eq("status", "inactive")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = ?"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = PSC.deleteFrom("users", User.class).where(Filters.lt("lastLogin", new Date())).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("last_login < ?"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = PSC.deleteFrom(User.class).where(Filters.eq("id", 123)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = ?"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = PSC.select("COUNT(*) AS total").from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*) AS total"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = PSC.select("id", "name", "email", "created_date").from("users").where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("name"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("created_date"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("active = ?"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            String sql = PSC.select(columns).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("u.first_name", "firstName");
            aliases.put("u.last_name", "lastName");
            aliases.put("COUNT(o.id)", "orderCount");

            String sql = PSC.select(aliases).from("users u").leftJoin("orders o").on("u.id = o.user_id").groupBy("u.id").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("u.first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("u.last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("COUNT(o.id) AS \"orderCount\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = PSC.select(User.class).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains("created_date"));
            Assertions.assertTrue(sql.contains("password"));
            Assertions.assertFalse(sql.contains("temp_data"));
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = PSC.select(User.class, true).from("users u").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> exclude = Set.of("password", "created_date");
            String sql = PSC.select(User.class, exclude).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("id"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> exclude = Set.of("password");
            String sql = PSC.select(User.class, true, exclude).from("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFrom() {
            String sql = PSC.selectFrom(User.class).where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("active = ?"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = PSC.selectFrom(User.class, "u").where(Filters.eq("u.active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM users u"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = PSC.selectFrom(User.class, true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = PSC.selectFrom(User.class, "u", true).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> exclude = Set.of("password");
            String sql = PSC.selectFrom(User.class, exclude).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> exclude = Set.of("password");
            String sql = PSC.selectFrom(User.class, "u", exclude).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("users u"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> exclude = Set.of("password");
            String sql = PSC.selectFrom(User.class, true, exclude).query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> exclude = Set.of("password");
            String sql = PSC.selectFrom(User.class, "u", true, exclude).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = PSC.select(User.class, "u", "user_", User.class, "u2", "user2_").from("users u").join("users u2").on("u.id = u2.parent_id").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("u.id AS \"user_.id\""));
            Assertions.assertTrue(sql.contains("u2.id AS \"user2_.id\""));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> excludeUser = Set.of("password");
            Set<String> excludeUser2 = Set.of("email");

            String sql = PSC.select(User.class, "u", "user_", excludeUser, User.class, "u2", "user2_", excludeUser2)
                    .from("users u")
                    .join("users u2")
                    .on("u.id = u2.parent_id")
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, Set.of("password")));

            String sql = PSC.select(selections).from("users u").join("users m").on("u.manager_id = m.id").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = PSC.selectFrom(User.class, "u", "user_", User.class, "m", "manager_").where(Filters.eq("u.active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = Set.of("password");
            Set<String> excludeM = Set.of("email", "password");

            String sql = PSC.selectFrom(User.class, "u", "user_", excludeU, User.class, "m", "manager_", excludeM).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, null));

            String sql = PSC.selectFrom(selections).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = PSC.count("users").where(Filters.eq("active", true)).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("active = ?"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = PSC.count(User.class).where(Filters.eq("status", "active")).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = ?"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18));
            String sql = PSC.parse(cond, User.class).query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("status = ?"));
            Assertions.assertTrue(sql.contains("age > ?"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testUpdateWithMap() {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("lastName", "Smith");

            String sql = PSC.update("users").set(updates).where(Filters.eq("id", 1)).query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name = ?"));
            Assertions.assertTrue(sql.contains("last_name = ?"));
            Assertions.assertTrue(sql.contains("id = ?"));
        }

        @Test
        public void testBatchInsertWithMaps() {
            List<Map<String, Object>> data = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("firstName", "John");
            row1.put("lastName", "Doe");
            data.add(row1);

            Map<String, Object> row2 = new HashMap<>();
            row2.put("firstName", "Jane");
            row2.put("lastName", "Smith");
            data.add(row2);

            String sql = PSC.batchInsert(data).into("users").query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("(?, ?)"));
        }

        @Test
        public void testNamingPolicySnakeCase() {
            // PSC uses SNAKE_CASE naming policy
            String sql = PSC.select("firstName", "lastName").from("userAccounts").query();

            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertTrue(sql.contains("userAccounts"));
        }

        @Test
        public void testColumnNameTransformation() {
            // Test that camelCase property names are converted to snake_case
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            String sql = PSC.insert(user).into("users").query();

            // Column names should be snake_case
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));

            // Should use ? for parameters
            Assertions.assertTrue(sql.contains("VALUES (?, ?)"));
        }

        @Test
        public void testAllJoinTypes() {
            // Test all join variations
            String sql;

            sql = PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = PSC.select("*").from("users u").innerJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = PSC.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = PSC.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = PSC.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = PSC.select("*").from("users").crossJoin("departments").query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = PSC.select("*").from("users").naturalJoin("user_profiles").query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = PSC.select("*").from("users").where(Filters.isNull("email")).query();
            Assertions.assertTrue(sql.contains("email IS NULL"));

            // Test IS NOT NULL
            sql = PSC.select("*").from("users").where(Filters.isNotNull("email")).query();
            Assertions.assertTrue(sql.contains("email IS NOT NULL"));

            // Test IN
            sql = PSC.select("*").from("users").where(Filters.in("status", Arrays.asList("active", "premium", "trial"))).query();
            Assertions.assertTrue(sql.contains("status IN (?, ?, ?)"));

            // Test BETWEEN
            sql = PSC.select("*").from("users").where(Filters.between("age", 18, 65)).query();
            Assertions.assertTrue(sql.contains("age BETWEEN ? AND ?"));

            // Test LIKE
            sql = PSC.select("*").from("users").where(Filters.like("name", "%John%")).query();
            Assertions.assertTrue(sql.contains("name LIKE ?"));
        }

        @Test
        public void testEmptyArgumentsThrow() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                PSC.select("");
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                PSC.select(new String[0]);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                PSC.select(new ArrayList<String>());
            });
        }

        @Test
        public void testNullConditionThrows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                PSC.parse(null, User.class);
            });
        }

        @Test
        public void testForUpdate() {
            String sql = PSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
            Assertions.assertTrue(sql.contains("id = ?"));
        }

        @Test
        public void testLimitOffset() {
            String sql = PSC.select("*").from("users").limit(10).offset(20).query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = PSC.select("*").from("users").limit(10, 20).query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = PSC.select("*").from("users").append(" WHERE custom_function(name) = ?").query();

            Assertions.assertTrue(sql.contains("custom_function"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = PSC.select("department", "COUNT(*) as count").from("users").groupBy("department").having(Filters.gt("COUNT(*)", 5)).query();

            Assertions.assertTrue(sql.contains("GROUP BY department"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("COUNT(*) > ?"));
        }

        @Test
        public void testOrderBy() {
            String sql = PSC.select("*").from("users").orderBy("last_name ASC", "first_name ASC").query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("last_name ASC"));
            Assertions.assertTrue(sql.contains("first_name ASC"));
        }

        @Test
        public void testMultipleWhereConditions() {
            String sql = PSC.select("*").from("users").where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%"))).query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("active = ?"));
            Assertions.assertTrue(sql.contains("age > ?"));
            Assertions.assertTrue(sql.contains("name LIKE ?"));
        }

        @Test
        public void testComplexConditionCombinations() {
            String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.or(Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18)),
                            Filters.and(Filters.eq("status", "premium"), Filters.isNotNull("subscription_id"))))
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("status = ?"));
            Assertions.assertTrue(sql.contains("age > ?"));
            Assertions.assertTrue(sql.contains("subscription_id IS NOT NULL"));
        }
    }
}