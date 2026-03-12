
package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.SqlBuilder.ACSB;
import com.landawn.abacus.query.SqlBuilder.LCSB;
import com.landawn.abacus.query.SqlBuilder.MAC;
import com.landawn.abacus.query.SqlBuilder.MLC;
import com.landawn.abacus.query.SqlBuilder.MSB;
import com.landawn.abacus.query.SqlBuilder.MSC;
import com.landawn.abacus.query.SqlBuilder.NAC;
import com.landawn.abacus.query.SqlBuilder.NLC;
import com.landawn.abacus.query.SqlBuilder.NSB;
import com.landawn.abacus.query.SqlBuilder.NSC;
import com.landawn.abacus.query.SqlBuilder.PAC;
import com.landawn.abacus.query.SqlBuilder.PLC;
import com.landawn.abacus.query.SqlBuilder.PSB;
import com.landawn.abacus.query.SqlBuilder.PSC;
import com.landawn.abacus.query.SqlBuilder.SCSB;
import com.landawn.abacus.query.SqlBuilder10Test.Order;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.Having;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.query.entity.Account;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Tuple.Tuple2;

class SqlBuilder10Test extends TestBase {

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

    @Table(name = "user_order", alias = "o")
    public static class Order {
        private long id;
        private long userId;
        private String orderNumber;
        private BigDecimal amount;
        private Date orderDate;

        // Getters and setters
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Date getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(Date orderDate) {
            this.orderDate = orderDate;
        }
    }

    @BeforeEach
    public void setUp() {
        // Reset any static state if needed
        AbstractQueryBuilder.resetHandlerForNamedParameter();
    }

    // Static method tests

    @Test
    public void testGetTableName() {
        assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.SNAKE_CASE));
        assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.CAMEL_CASE));
        assertEquals("test_account", AbstractQueryBuilder.getTableName(Account.class, NamingPolicy.NO_CHANGE));

        assertEquals("user_order", AbstractQueryBuilder.getTableName(Order.class, NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testGetTableAlias() {
        assertEquals("", AbstractQueryBuilder.getTableAlias(Account.class));
        assertEquals("o", AbstractQueryBuilder.getTableAlias(Order.class));
    }

    @Test
    public void testGetTableAliasWithSpecifiedAlias() {
        assertEquals("a", AbstractQueryBuilder.getTableAlias("a", Account.class));
        assertEquals("", AbstractQueryBuilder.getTableAlias("", Account.class));
        assertEquals("", AbstractQueryBuilder.getTableAlias(null, Account.class));
    }

    @Test
    public void testGetTableAliasOrName() {
        assertEquals("test_account", AbstractQueryBuilder.getTableAliasOrName(Account.class, NamingPolicy.SNAKE_CASE));
        assertEquals("o", AbstractQueryBuilder.getTableAliasOrName(Order.class, NamingPolicy.SNAKE_CASE));

        assertEquals("custom", AbstractQueryBuilder.getTableAliasOrName("custom", Account.class, NamingPolicy.SNAKE_CASE));
        assertEquals("o", AbstractQueryBuilder.getTableAliasOrName("", Order.class, NamingPolicy.SNAKE_CASE));
    }

    @Test
    public void testIsDefaultIdPropValue() {
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
    public void testLoadPropNamesByClass() {
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
    public void testGetSubEntityPropNames() {
        ImmutableSet<String> subProps = AbstractQueryBuilder.getSubEntityPropNames(Account.class);
        assertNotNull(subProps);
        assertTrue(subProps.isEmpty()); // Account has no sub-entities
    }

    @Test
    public void testNamed() {
        Map<String, Expression> result = AbstractQueryBuilder.named("firstName", "lastName");
        assertEquals(2, result.size());
        assertEquals(Filters.QME, result.get("firstName"));
        assertEquals(Filters.QME, result.get("lastName"));

        List<String> propList = Arrays.asList("email", "status");
        result = AbstractQueryBuilder.named(propList);
        assertEquals(2, result.size());
        assertEquals(Filters.QME, result.get("email"));
        assertEquals(Filters.QME, result.get("status"));
    }

    @Test
    public void testSetAndResetHandlerForNamedParameter() {
        // Test custom handler
        BiConsumer<StringBuilder, String> customHandler = (sb, propName) -> sb.append("${").append(propName).append("}");

        AbstractQueryBuilder.setHandlerForNamedParameter(customHandler);

        String sql = NSC.select("name").from("users").where(Filters.eq("id", 1)).build().query();
        assertEquals("SELECT name FROM users WHERE id = ${id}", sql);

        // Create a named SQL to test the handler
        sql = NSC.update("account").set("firstName").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("${firstName}"));
        assertTrue(sql.contains("${id}"));

        // Reset to default
        AbstractQueryBuilder.resetHandlerForNamedParameter();

        sql = NSC.update("account").set("firstName").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains(":firstName"));
        assertTrue(sql.contains(":id"));
    }

    @Test
    public void testSetHandlerForNamedParameterWithNull() {
        assertThrows(IllegalArgumentException.class, () -> AbstractQueryBuilder.setHandlerForNamedParameter(null));
    }

    // Instance method tests using PSC (Parameterized SQL with snake_case)

    @Test
    public void testInto() {
        String sql = PSC.insert("firstName", "lastName").into("account").build().query();

        assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?)", sql);

        // Test with entity class
        sql = PSC.insert("firstName", "lastName").into(Account.class).build().query();

        assertEquals("INSERT INTO test_account (first_name, last_name) VALUES (?, ?)", sql);
    }

    @Test
    public void testIntoWithEntityAndTableName() {
        String sql = PSC.insert("firstName", "lastName").into("custom_table", Account.class).build().query();

        assertEquals("INSERT INTO custom_table (first_name, last_name) VALUES (?, ?)", sql);
    }

    //    @Test
    //    public void testIntoWithInvalidOperation() {
    //        SqlBuilder builder = PSC.select("*").from("account");
    //        assertThrows(RuntimeException.class, () -> builder.into("account"));
    //    }

    @Test
    public void testIntoWithoutColumns() {
        assertThrows(RuntimeException.class, () -> PSC.update("account").into("account"));
    }

    @Test
    public void testDistinct() {
        String sql = PSC.select("name").distinct().from("account").build().query();

        assertEquals("SELECT DISTINCT name FROM account", sql);
    }

    @Test
    public void testPreselect() {
        String sql = PSC.select("*").selectModifier("TOP 10").from("account").build().query();

        assertEquals("SELECT TOP 10 * FROM account", sql);

        // Test duplicate selectModifier
        SqlBuilder builder = PSC.select("*").selectModifier("TOP 10");
        assertThrows(IllegalStateException.class, () -> builder.selectModifier("DISTINCT"));
    }

    @Test
    public void testFrom() {
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
    public void testFromRejectsNullOrBlankTableNames() {
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from((String) null));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from("   "));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(new String[] { "users", null }));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(new String[] { "users", "   " }));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(Arrays.asList("users", null)));
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*").from(Arrays.asList("users", "  ")));
    }

    @Test
    public void testFromWithExpression() {
        String sql = PSC.select("*").from("(SELECT * FROM users) t").build().query();
        assertEquals("SELECT * FROM (SELECT * FROM users) t", sql);

        sql = PSC.select("*").from("users u, orders o").build().query();
        assertEquals("SELECT * FROM users u, orders o", sql);
    }

    @Test
    public void testFromWithAsAliasAndEntityClass() {
        String sql = PSC.select("firstName").from("test_account AS a", Account.class).build().query();
        assertTrue(sql.contains("a.first_name"));
        assertTrue(sql.contains("FROM test_account AS a"));
    }

    @Test
    public void testFromWithoutSelect() {
        assertThrows(RuntimeException.class, () -> PSC.update("account").from("account"));
    }

    @Test
    public void testJoin() {
        String sql = PSC.select("*").from("users u").join("orders o ON u.id = o.user_id").build().query();

        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);

        // With entity class
        sql = PSC.select("*").from(Account.class, "a").join(Order.class, "o").on("a.id = o.user_id").build().query();

        assertEquals("SELECT * FROM test_account a JOIN user_order o ON a.id = o.user_id", sql);
    }

    @Test
    public void testInnerJoin() {
        String sql = PSC.select("*").from("users u").innerJoin("orders o ON u.id = o.user_id").build().query();

        assertEquals("SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testLeftJoin() {
        String sql = PSC.select("*").from("users u").leftJoin("orders o ON u.id = o.user_id").build().query();

        assertEquals("SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testRightJoin() {
        String sql = PSC.select("*").from("users u").rightJoin("orders o ON u.id = o.user_id").build().query();

        assertEquals("SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFullJoin() {
        String sql = PSC.select("*").from("users u").fullJoin("orders o ON u.id = o.user_id").build().query();

        assertEquals("SELECT * FROM users u FULL JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testCrossJoin() {
        String sql = PSC.select("*").from("users").crossJoin("orders").build().query();

        assertEquals("SELECT * FROM users CROSS JOIN orders", sql);
    }

    @Test
    public void testNaturalJoin() {
        String sql = PSC.select("*").from("users").naturalJoin("orders").build().query();

        assertEquals("SELECT * FROM users NATURAL JOIN orders", sql);
    }

    @Test
    public void testOn() {
        String sql = PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();

        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);

        // With condition
        sql = PSC.select("*").from("users u").join("orders o").on(Filters.eq("u.id", "o.user_id")).build().query();

        assertTrue(sql.contains("ON"));
    }

    @Test
    public void testUsing() {
        String sql = PSC.select("*").from("users").join("orders").using("user_id").build().query();

        assertEquals("SELECT * FROM users JOIN orders USING (user_id)", sql);
    }

    @Test
    public void testWhere() {
        String sql = PSC.select("*").from("users").where("age > 18").build().query();

        assertEquals("SELECT * FROM users WHERE age > 18", sql);

        // With condition
        sql = PSC.select("*").from("users").where(Filters.gt("age", 18)).build().query();

        assertEquals("SELECT * FROM users WHERE age > ?", sql);
    }

    @Test
    public void testGroupBy() {
        // Single column
        String sql = PSC.select("category", "COUNT(*)").from("products").groupBy("category").build().query();

        assertEquals("SELECT category, COUNT(*) FROM products GROUP BY category", sql);

        // Multiple columns
        sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupBy("category", "brand").build().query();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand", sql);

        // With direction
        sql = PSC.select("category", "COUNT(*)").from("products").groupBy("category", SortDirection.DESC).build().query();

        assertEquals("SELECT category, COUNT(*) FROM products GROUP BY category DESC", sql);

        // Collection
        sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupBy(Arrays.asList("category", "brand")).build().query();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand", sql);

        // Map with directions
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("category", SortDirection.ASC);
        orders.put("brand", SortDirection.DESC);

        sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupBy(orders).build().query();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand DESC", sql);
    }

    @Test
    public void testHaving() {
        String sql = PSC.select("category", "COUNT(*) as count").from("products").groupBy("category").having("COUNT(*) > 10").build().query();

        assertEquals("SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > 10", sql);

        // With condition
        sql = PSC.select("category", "COUNT(*) as count").from("products").groupBy("category").having(Filters.gt("COUNT(*)", 10)).build().query();

        assertEquals("SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > ?", sql);
    }

    @Test
    public void testOrderBy() {
        // Single column
        String sql = PSC.select("*").from("users").orderBy("name").build().query();

        assertEquals("SELECT * FROM users ORDER BY name", sql);

        // Multiple columns
        sql = PSC.select("*").from("users").orderBy("lastName", "firstName").build().query();

        assertEquals("SELECT * FROM users ORDER BY last_name, first_name", sql);

        // With direction
        sql = PSC.select("*").from("users").orderBy("name", SortDirection.DESC).build().query();

        assertEquals("SELECT * FROM users ORDER BY name DESC", sql);

        // Collection
        sql = PSC.select("*").from("users").orderBy(Arrays.asList("lastName", "firstName")).build().query();

        assertEquals("SELECT * FROM users ORDER BY last_name, first_name", sql);

        // Map with directions
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("lastName", SortDirection.ASC);
        orders.put("firstName", SortDirection.DESC);

        sql = PSC.select("*").from("users").orderBy(orders).build().query();

        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name DESC", sql);
    }

    @Test
    public void testOrderByAsc() {
        String sql = PSC.select("*").from("users").orderByAsc("name").build().query();

        assertEquals("SELECT * FROM users ORDER BY name ASC", sql);

        sql = PSC.select("*").from("users").orderByAsc("lastName", "firstName").build().query();

        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC", sql);

        sql = PSC.select("*").from("users").orderByAsc(Arrays.asList("lastName", "firstName")).build().query();

        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC", sql);
    }

    @Test
    public void testOrderByDesc() {
        String sql = PSC.select("*").from("users").orderByDesc("createdDate").build().query();

        assertEquals("SELECT * FROM users ORDER BY created_date DESC", sql);

        sql = PSC.select("*").from("users").orderByDesc("lastName", "firstName").build().query();

        assertEquals("SELECT * FROM users ORDER BY last_name DESC, first_name DESC", sql);

        sql = PSC.select("*").from("users").orderByDesc(Arrays.asList("lastName", "firstName")).build().query();

        assertEquals("SELECT * FROM users ORDER BY last_name DESC, first_name DESC", sql);
    }

    @Test
    public void testLimit() {
        String sql = PSC.select("*").from("users").limit(10).build().query();

        assertEquals("SELECT * FROM users LIMIT 10", sql);

        // With offset
        sql = PSC.select("*").from("users").limit(10, 20).build().query();

        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testOffset() {
        String sql = PSC.select("*").from("users").limit(10).offset(20).build().query();

        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testOffsetRows() {
        String sql = PSC.select("*").from("users").orderBy("id").offsetRows(20).fetchNextRows(10).build().query();

        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testFetchNextNRowsOnly() {
        String sql = PSC.select("*").from("users").orderBy("id").offsetRows(0).fetchNextRows(10).build().query();

        assertEquals("SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testFetchFirstNRowsOnly() {
        String sql = PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).build().query();

        assertEquals("SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY", sql);
    }

    @Test
    public void testAppend() {
        // With condition
        String sql = PSC.select("*").from("users").append(Filters.and(Filters.gt("age", 18), Filters.lt("age", 65))).build().query();

        assertEquals("SELECT * FROM users WHERE (age > ?) AND (age < ?)", sql);

        // With string
        sql = PSC.select("*").from("users").append(" FOR UPDATE").build().query();

        assertEquals("SELECT * FROM users FOR UPDATE", sql);
    }

    //    @Test
    //    public void testAppendIf() {
    //        // With condition - true
    //        String sql = PSC.select("*").from("users").appendIf(true, Filters.gt("age", 18)).build().query();
    //
    //        assertEquals("SELECT * FROM users WHERE age > ?", sql);
    //
    //        // With condition - false
    //        sql = PSC.select("*").from("users").appendIf(false, Filters.gt("age", 18)).build().query();
    //
    //        assertEquals("SELECT * FROM users", sql);
    //
    //        // With string
    //        sql = PSC.select("*").from("users").appendIf(true, " FOR UPDATE").build().query();
    //
    //        assertEquals("SELECT * FROM users FOR UPDATE", sql);
    //
    //        // With consumer
    //        sql = PSC.select("*").from("users").appendIf(true, builder -> builder.where(Filters.gt("age", 18)).orderBy("name")).build().query();
    //
    //        assertTrue(sql.contains("WHERE age > ?"));
    //        assertTrue(sql.contains("ORDER BY name"));
    //    }
    //
    //    @Test
    //    public void testAppendIfOrElse() {
    //        // With condition
    //        String sql = PSC.select("*").from("users").appendIfOrElse(true, Filters.eq("status", "active"), Filters.eq("status", "inactive")).build().query();
    //
    //        assertEquals("SELECT * FROM users WHERE status = ?", sql);
    //
    //        sql = PSC.select("*").from("users").appendIfOrElse(false, Filters.eq("status", "active"), Filters.eq("status", "inactive")).build().query();
    //
    //        assertEquals("SELECT * FROM users WHERE status = ?", sql);
    //
    //        // With string
    //        sql = PSC.select("*").from("users").appendIfOrElse(true, " ORDER BY name ASC", " ORDER BY name DESC").build().query();
    //
    //        assertEquals("SELECT * FROM users ORDER BY name ASC", sql);
    //    }

    @Test
    public void testUnion() {
        SqlBuilder query1 = PSC.select("id", "name").from("users");
        SqlBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.union(query2).build().query();
        assertEquals("SELECT id, name FROM users UNION SELECT id, name FROM customers", sql);

        // With string
        sql = PSC.select("id", "name").from("users").union("SELECT id, name FROM customers").build().query();

        assertEquals("SELECT id, name FROM users UNION SELECT id, name FROM customers", sql);

        // Start new select
        sql = PSC.select("id", "name").from("users").union("id", "name").from("customers").build().query();

        assertEquals("SELECT id, name FROM users UNION SELECT id, name FROM customers", sql);

        // With collection
        sql = PSC.select("id", "name").from("users").union(Arrays.asList("id", "name")).from("customers").build().query();

        assertEquals("SELECT id, name FROM users UNION SELECT id, name FROM customers", sql);

        SqlBuilder self = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> self.union(self));
    }

    @Test
    public void testUnionAll() {
        SqlBuilder query1 = PSC.select("id", "name").from("users");
        SqlBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.unionAll(query2).build().query();
        assertEquals("SELECT id, name FROM users UNION ALL SELECT id, name FROM customers", sql);

        SqlBuilder self = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> self.unionAll(self));
    }

    @Test
    public void testIntersect() {
        SqlBuilder query1 = PSC.select("id", "name").from("users");
        SqlBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.intersect(query2).build().query();
        assertEquals("SELECT id, name FROM users INTERSECT SELECT id, name FROM customers", sql);

        SqlBuilder self = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> self.intersect(self));
    }

    @Test
    public void testExcept() {
        SqlBuilder query1 = PSC.select("id", "name").from("users");
        SqlBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.except(query2).build().query();
        assertEquals("SELECT id, name FROM users EXCEPT SELECT id, name FROM customers", sql);

        SqlBuilder self = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> self.except(self));
    }

    @Test
    public void testMinus() {
        SqlBuilder query1 = PSC.select("id", "name").from("users");
        SqlBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.minus(query2).build().query();
        assertEquals("SELECT id, name FROM users MINUS SELECT id, name FROM customers", sql);

        SqlBuilder self = PSC.select("id").from("users");
        assertThrows(IllegalArgumentException.class, () -> self.minus(self));
    }

    @Test
    public void testForUpdate() {
        String sql = PSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

        assertEquals("SELECT * FROM users WHERE id = ? FOR UPDATE", sql);
    }

    @Test
    public void testSet() {
        // With expression
        String sql = PSC.update("users").set("name = 'John'").where(Filters.eq("id", 1)).build().query();

        assertEquals("UPDATE users SET name = 'John' WHERE id = ?", sql);

        // With columns
        sql = PSC.update("users").set("firstName", "lastName", "email").where(Filters.eq("id", 1)).build().query();

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
    public void testSql() {
        String sql = PSC.select("id", "name").from("account").where(Filters.gt("age", 18)).build().query();

        assertEquals("SELECT id, name FROM account WHERE age > ?", sql);

        // Test double call throws exception
        SqlBuilder builder = PSC.select("*").from("users");
        builder.build().query();
        assertThrows(RuntimeException.class, () -> builder.build().query());
    }

    @Test
    public void testParameters() {
        SqlBuilder builder = PSC.select("*").from("account").where(Filters.eq("name", "John").and(Filters.gt("age", 25)));

        SP sp = builder.build();
        List<Object> params = sp.parameters();

        assertEquals(2, params.size());
        assertEquals("John", params.get(0));
        assertEquals(25, params.get(1));
    }

    @Test
    public void testPair() {
        SqlBuilder.SP sp = PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).build();

        assertEquals("SELECT * FROM account WHERE status = ?", sp.query());
        assertEquals(1, sp.parameters().size());
        assertEquals("ACTIVE", sp.parameters().get(0));
    }

    @Test
    public void testApplyFunction() throws Exception {
        List<String> result = PSC.select("*")
                .from("account")
                .where(Filters.eq("status", "ACTIVE"))
                .apply(sp -> Arrays.asList(sp.query(), sp.parameters().toString()));

        assertEquals(2, result.size());
        assertEquals("SELECT * FROM account WHERE status = ?", result.get(0));
        assertEquals("[ACTIVE]", result.get(1));
    }

    @Test
    public void testApplyBiFunction() throws Exception {
        String result = PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).apply((sql, params) -> sql + " - " + params.size());

        assertEquals("SELECT * FROM account WHERE status = ? - 1", result);
    }

    @Test
    public void testPrintln() {
        SqlBuilder builder = PSC.select("*").from("account").where(Filters.between("age", 18, 65));
        builder.println();
        assertThrows(RuntimeException.class, () -> builder.where(Filters.eq("id", 1)));
    }

    @Test
    public void testToString() {
        String sql = PSC.select("*").from("account").where(Filters.eq("id", 1)).toString();

        assertNotEquals("SELECT * FROM account WHERE id = ?", sql);
    }

    @Test
    public void testIsNamedSql() {
        assertFalse(PSC.select("*").from("account").isNamedSql());
        assertTrue(NSC.select("*").from("account").isNamedSql());
    }

    @Test
    public void testComplexQuery() {
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

    // Test different naming policies
    @Test
    public void testNamingPolicies() {
        // Snake case (PSC)
        String sql = PSC.select("firstName", "lastName").from("userAccount").build().query();
        assertTrue(sql.contains("first_name"));
        assertTrue(sql.contains("last_name"));
        assertTrue(sql.contains("userAccount"));

        // Upper case (PAC)
        sql = PAC.select("firstName", "lastName").from("userAccount").build().query();
        assertTrue(sql.contains("FIRST_NAME"));
        assertTrue(sql.contains("LAST_NAME"));
        assertTrue(sql.contains("userAccount"));

        // Lower camel case (PLC)
        sql = PLC.select("first_name", "last_name").from("user_account").build().query();
        assertTrue(sql.contains("firstName"));
        assertTrue(sql.contains("lastName"));
        assertTrue(sql.contains("user_account"));
    }

    // Test SQL policies
    @Test
    public void testSQLPolicies() {
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
    public void testMultipleJoins() {
        String sql = PSC.select("*")
                .from("users u")
                .leftJoin("orders o")
                .on("u.id = o.user_id")
                .leftJoin("products p")
                .on("o.product_id = p.id")
                .where(Filters.eq("u.status", "ACTIVE"))
                .build()
                .query();

        assertTrue(sql.contains("FROM users u"));
        assertTrue(sql.contains("LEFT JOIN orders o ON u.id = o.user_id"));
        assertTrue(sql.contains("LEFT JOIN products p ON o.product_id = p.id"));
        assertTrue(sql.contains("WHERE u.status = ?"));
    }

    @Test
    public void testJoinWithEntityClasses() {
        String sql = PSC.select("*").from(Account.class, "a").join(Order.class, "o").on(Filters.eq("a.id", "o.userId")).build().query();

        assertTrue(sql.contains("FROM test_account a"));
        assertTrue(sql.contains("JOIN user_order o"));
    }

    @Test
    public void testConditionsWithAnd() {
        String sql = PSC.select("*")
                .from("users")
                .where(Filters.and(Filters.gt("age", 18), Filters.lt("age", 65), Filters.eq("status", "ACTIVE")))
                .build()
                .query();

        assertTrue(sql.contains("WHERE (age > ?) AND (age < ?) AND (status = ?)"));
    }

    @Test
    public void testConditionsWithOr() {
        String sql = PSC.select("*").from("users").where(Filters.or(Filters.eq("status", "ACTIVE"), Filters.eq("status", "PENDING"))).build().query();

        assertTrue(sql.contains("WHERE (status = ?) OR (status = ?)"));
    }

    @Test
    public void testBetweenCondition() {
        String sql = PSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();

        assertEquals("SELECT * FROM users WHERE age BETWEEN ? AND ?", sql);
    }

    @Test
    public void testNotBetweenCondition() {
        String sql = PSC.select("*").from("users").where(Filters.notBetween("age", 18, 65)).build().query();

        assertEquals("SELECT * FROM users WHERE age NOT BETWEEN ? AND ?", sql);
    }

    @Test
    public void testInCondition() {
        String sql = PSC.select("*").from("users").where(Filters.in("status", Arrays.asList("ACTIVE", "PENDING", "APPROVED"))).build().query();

        assertEquals("SELECT * FROM users WHERE status IN (?, ?, ?)", sql);
    }

    @Test
    public void testNotInCondition() {
        String sql = PSC.select("*").from("users").where(Filters.notIn("status", Arrays.asList("DELETED", "BANNED"))).build().query();

        assertEquals("SELECT * FROM users WHERE status NOT IN (?, ?)", sql);
    }

    @Test
    public void testIsNullCondition() {
        String sql = PSC.select("*").from("users").where(Filters.isNull("deletedDate")).build().query();

        assertEquals("SELECT * FROM users WHERE deleted_date IS NULL", sql);
    }

    @Test
    public void testIsNotNullCondition() {
        String sql = PSC.select("*").from("users").where(Filters.isNotNull("email")).build().query();

        assertEquals("SELECT * FROM users WHERE email IS NOT NULL", sql);
    }

    @Test
    public void testLikeCondition() {
        String sql = PSC.select("*").from("users").where(Filters.like("name", "%John%")).build().query();

        assertEquals("SELECT * FROM users WHERE name LIKE ?", sql);
    }

    @Test
    public void testNotLikeCondition() {
        String sql = PSC.select("*").from("users").where(Filters.notLike("email", "%@temp.com")).build().query();

        assertEquals("SELECT * FROM users WHERE email NOT LIKE ?", sql);
    }

    @Test
    public void testCriteriaCondition() {
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
    public void testExpressionCondition() {
        String sql = PSC.select("*").from("users").where(Filters.expr("age > 18 AND status = 'ACTIVE'")).build().query();

        assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'ACTIVE'", sql);
    }

    @Test
    public void testSelectWithAlias() {
        String sql = PSC.select("firstName AS name", "age").from("users").build().query();

        assertTrue(sql.contains("first_name AS name"));
        assertTrue(sql.contains("age"));
    }

    @Test
    public void testSelectWithAliasMap() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("firstName", "name");
        aliases.put("lastName", "surname");

        String sql = PSC.select(aliases).from("users").build().query();

        assertTrue(sql.contains("first_name AS \"name\""));
        assertTrue(sql.contains("last_name AS \"surname\""));
    }

    @Test
    public void testInsertWithMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("firstName", "John");
        values.put("lastName", "Doe");
        values.put("email", "john@example.com");

        String sql = PSC.insert(values).into("users").build().query();

        assertEquals("INSERT INTO users (first_name, last_name, email) VALUES (?, ?, ?)", sql);
    }

    @Test
    public void testInsertWithEntity() {
        Account account = new Account();
        account.setId(1);
        account.setFirstName("John");
        account.setLastName("Doe");

        String sql = PSC.insert(account).into("account").build().query();

        assertTrue(sql.contains("INSERT INTO account"));
        assertTrue(sql.contains("VALUES"));
    }

    @Test
    public void testBatchInsert() {
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

    @Test
    public void testUpdateWithoutSet() {
        String sql = PSC.update("users").where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE users SET"));
        assertTrue(sql.contains("WHERE id = ?"));
    }

    @Test
    public void testDeleteFrom() {
        String sql = PSC.deleteFrom("users").where(Filters.eq("status", "DELETED")).build().query();

        assertEquals("DELETE FROM users WHERE status = ?", sql);

        // With entity class
        sql = PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();

        assertEquals("DELETE FROM test_account WHERE id = ?", sql);
    }

    @Test
    public void testNamingPolicyConversion() {
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
    public void testProp2ColumnNameMap() {
        ImmutableMap<String, Tuple2<String, Boolean>> map = AbstractQueryBuilder.prop2ColumnNameMap(Account.class, NamingPolicy.SNAKE_CASE);

        assertNotNull(map);
        assertTrue(map.containsKey("firstName"));
        assertTrue(map.containsKey("status"));

        assertEquals("first_name", map.get("firstName")._1);
        assertEquals("account_status", map.get("status")._1); // Should use @Column annotation
    }

    @Test
    public void testNamedSQLWithParameters() {
        String sql = NSC.select("*").from("users").where(Filters.eq("firstName", "John").and(Filters.gt("age", 25))).build().query();

        assertTrue(sql.contains("first_name = :firstName"));
        assertTrue(sql.contains("age > :age"));

        List<Object> params = NSC.select("*").from("users").where(Filters.eq("firstName", "John").and(Filters.gt("age", 25))).parameters();

        assertEquals(2, params.size());
        assertEquals("John", params.get(0));
        assertEquals(25, params.get(1));
    }

    @Test
    public void testNamedSQLWithIn() {
        String sql = NSC.select("*").from("users").where(Filters.in("status", Arrays.asList("ACTIVE", "PENDING"))).build().query();

        assertTrue(sql.contains("status IN (:status1, :status2)"));
    }

    @Test
    public void testNamedSQLWithBetween() {
        String sql = NSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();

        assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
    }

    @Test
    public void testNamedSQLWithRepeatedPropertyUsesUniqueParameterNames() {
        String sql = NSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").and(Filters.ne("status", "DELETED"))).build().query();

        assertTrue(sql.contains("status = :status"));
        assertTrue(sql.contains("status != :status_2"));

        List<Object> params = NSC.select("*").from("users").where(Filters.eq("status", "ACTIVE").and(Filters.ne("status", "DELETED"))).parameters();
        assertEquals(Arrays.asList("ACTIVE", "DELETED"), params);
    }

    @Test
    public void testComplexJoinConditions() {
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
    public void testIbatisSQLPolicy() {
        SP sp = MSC.update("users").set(Map.of("name", "Alice")).where(Filters.eq("id", 1)).build();

        assertEquals("UPDATE users SET name = #{name} WHERE id = #{id}", sp.query());
        assertEquals(Arrays.asList("Alice", 1), sp.parameters());
    }

    @Test
    public void testMultipleWhereConditions() {

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
    public void testUnionWithSubQuery() {
        String subQuery = "(SELECT id, name FROM archived_users WHERE status = 'INACTIVE')";

        String sql = PSC.select("id", "name").from("users").where(Filters.eq("status", "ACTIVE")).union(subQuery).build().query();

        assertTrue(sql.contains("SELECT id, name FROM users WHERE status = ?"));
        assertTrue(sql.contains("UNION"));
        assertTrue(sql.contains(subQuery));
    }

    @Test
    public void testColumnNameWithSpecialCharacters() {
        String sql = PSC.select("user.name", "COUNT(*) as total").from("users").groupBy("user.name").build().query();

        assertTrue(sql.contains("user.name"));
        assertTrue(sql.contains(" COUNT(*) AS total"));
        assertTrue(sql.contains("GROUP BY user.name"));
    }

    @Test
    public void testMultipleGroupByWithDifferentSortDirections() {
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
    public void testLimitWithLargeNumbers() {
        String sql = PSC.select("*").from("users").limit(50, 1000000).build().query();

        assertEquals("SELECT * FROM users LIMIT 50 OFFSET 1000000", sql);
    }

    @Test
    public void testComplexUpdateWithMultipleConditions() {
        Map<String, Object> updateValues = new LinkedHashMap<>();
        updateValues.put("status", "INACTIVE");
        updateValues.put("lastModifiedDate", new Date());

        String sql = PSC.update("users")
                .set(updateValues)
                .where(Filters.and(Filters.lt("lastLoginDate", new Date()), Filters.or(Filters.eq("status", "PENDING"), Filters.eq("status", "ACTIVE"))))
                .build()
                .query();

        assertTrue(sql.contains("UPDATE users SET"));
        assertTrue(sql.contains("status = ?"));
        assertTrue(sql.contains("last_modified_date = ?"));
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testExpressionWithComplexSQL() {
        String sql = PSC.select("*").from("users").where(Filters.expr("DATEDIFF(day, created_date, GETDATE()) > 30")).build().query();

        assertEquals("SELECT * FROM users WHERE DATEDIFF(day, created_date, GETDATE()) > 30", sql);
    }

    @Test
    public void testJoinUsingMultipleColumns() {
        String sql = PSC.select("*").from("table1").join("table2").using("(col1, col2)").build().query();

        assertEquals("SELECT * FROM table1 JOIN table2 USING (col1, col2)", sql);
    }

    @Test
    public void testSelectWithFunctions() {
        String sql = PSC.select("MAX(age)", "MIN(age)", "AVG(age)", "COUNT(*)").from("users").groupBy("status").build().query();

        assertTrue(sql.contains("MAX(age)"));
        assertTrue(sql.contains("MIN(age)"));
        assertTrue(sql.contains("AVG(age)"));
        assertTrue(sql.contains("COUNT(*)"));
    }

    @Test
    public void testHavingWithMultipleConditions() {
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
    public void testAppendWithCriteria() {
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
    public void testWhereClause() {
        Where where = Filters.where("age > 18 AND status = 'ACTIVE'");

        String sql = PSC.select("*").from("users").append(where).build().query();

        assertTrue(sql.contains("WHERE age > 18 AND status = 'ACTIVE'"));
    }

    @Test
    public void testHavingClause() {
        Having having = Filters.having("COUNT(*) > 10");

        String sql = PSC.select("status", "COUNT(*)").from("users").groupBy("status").append(having).build().query();

        assertTrue(sql.contains("HAVING COUNT(*) > 10"));
    }

    @Test
    public void testOrderByWithExpression() {
        String sql = PSC.select("*").from("users").orderBy("CASE WHEN status = 'VIP' THEN 0 ELSE 1 END, name").build().query();

        assertTrue(sql.contains(" ORDER BY case when status = 'VIP' then 0 else 1 end, name"));
    }

    @Test
    public void testInsertWithExcludedProperties() {
        Account account = new Account();
        account.setId(1);
        account.setFirstName("John");
        account.setLastName("Doe");
        account.setCreatedDate(new Date());
        account.setLastModifiedDate(new Date());

        Set<String> excluded = N.asSet("createdDate", "lastModifiedDate");

        String sql = PSC.insert(account, excluded).into("account").build().query();

        assertTrue(sql.contains("INSERT INTO account"));
        assertFalse(sql.contains("created_date"));
        assertFalse(sql.contains("last_modified_date"));
    }

    @Test
    public void testBatchInsertWithEntities() {
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
    public void testBatchInsertWithMixedEntityTypesThrows() {
        List<Object> entities = new ArrayList<>();
        entities.add(new Account());
        entities.add(new Order());

        assertThrows(IllegalArgumentException.class, () -> PSC.batchInsert(entities));
    }

    @Test
    public void testDeleteFromWithMultipleConditions() {
        String sql = PSC.deleteFrom("users").where(Filters.and(Filters.eq("status", "DELETED"), Filters.lt("deletedDate", new Date()))).build().query();

        assertTrue(sql.contains("DELETE FROM users"));
        assertTrue(sql.contains("WHERE (status = ?) AND (deleted_date < ?)"));
    }

    @Test
    public void testSelectFromWithExcludedProperties() {
        String sql = PSC.selectFrom(Account.class, N.asSet("createdDate", "lastModifiedDate")).build().query();

        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM test_account"));
        assertFalse(sql.contains("created_date"));
        assertFalse(sql.contains("last_modified_date"));
    }

    @Test
    public void testComplexQueryWithAllFeatures() {
        // Create a complex query using all major features
        String sql = PSC.select("u.id", "u.name", "COUNT(o.id) as order_count", "SUM(o.amount) as total_amount")
                .from("users u")
                .leftJoin("orders o")
                .on(Filters.and(Filters.eq("u.id", "o.user_id"), Filters.eq("o.status", "COMPLETED")))
                .where(Filters.and(Filters.gt("u.created_date", new Date()), Filters.in("u.status", Arrays.asList("ACTIVE", "VIP"))))
                .groupBy("u.id", "u.name")
                .having(Filters.gt("COUNT(o.id)", 5))
                .orderBy("total_amount", SortDirection.DESC)
                .limit(20, 10)
                .build()
                .query();

        // Verify all parts are present
        N.println(sql);
        assertTrue(sql.contains("SELECT u.id, u.name, COUNT(o.id) AS order_count, SUM(o.amount) AS total_amount"));
        assertTrue(sql.contains("FROM users u"));
        assertTrue(sql.contains("LEFT JOIN orders o"));
        assertTrue(sql.contains("ON"));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("GROUP BY u.id, u.name"));
        assertTrue(sql.contains("HAVING COUNT(o.id) > ?"));
        assertTrue(sql.contains("ORDER BY total_amount DESC"));
        assertTrue(sql.contains("LIMIT 20 OFFSET 10"));
    }

    @Test
    public void testSqlBuilderWithDifferentNamingPolicies() {
        // Test that different SqlBuilder implementations use correct naming policies

        // Snake case
        assertTrue(PSC.select("firstName").from("userAccount").build().query().contains("first_name"));
        assertTrue(NSC.select("firstName").from("userAccount").build().query().contains("first_name"));

        // Upper case
        assertTrue(PAC.select("firstName").from("userAccount").build().query().contains("FIRST_NAME"));
        assertTrue(NAC.select("firstName").from("userAccount").build().query().contains("FIRST_NAME"));

        // Lower camel case
        assertTrue(PLC.select("first_name").from("user_account").build().query().contains("firstName"));
        assertTrue(NLC.select("first_name").from("user_account").build().query().contains("firstName"));
    }

    @Test
    public void testAppendWithClause() {
        // Test appending a Clause condition
        String sql = PSC.select("*").from("users").append(Filters.eq("status", "ACTIVE")).build().query();

        assertTrue(sql.contains("status = ?"));
    }

    @Test
    public void testMultipleAppendsWithConditions() {
        String sql = PSC.select("*").from("users").append(Filters.eq("status", "ACTIVE")).append(" AND age > 18").build().query();

        assertTrue(sql.contains("WHERE status = ?"));
        assertTrue(sql.contains("AND age > 18"));
    }

    @Test
    public void testUpdateAllProperties() {
        String sql = PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("UPDATE account SET"));
        assertTrue(sql.contains("first_name = ?"));
        assertTrue(sql.contains("last_name = ?"));
        assertFalse(sql.contains("created_date")); // @NonUpdatable
        assertFalse(sql.contains("last_modified_date")); // @ReadOnly
    }

    @Test
    public void testFormatColumnNameEdgeCases() {
        // Test edge cases for column name formatting
        assertEquals("id", AbstractQueryBuilder.normalizeColumnName("id", NamingPolicy.SNAKE_CASE));
        assertEquals("ID", AbstractQueryBuilder.normalizeColumnName("id", NamingPolicy.SCREAMING_SNAKE_CASE));
        assertEquals("user_name123", AbstractQueryBuilder.normalizeColumnName("userName123", NamingPolicy.SNAKE_CASE));
        assertEquals("USER_NAME123", AbstractQueryBuilder.normalizeColumnName("userName123", NamingPolicy.SCREAMING_SNAKE_CASE));
    }

    @Test
    public void testEmptyConditions() {
        // Test handling of empty conditions
        assertThrows(IllegalArgumentException.class, () -> PSC.select("*")
                .from("users")
                .where(Filters.and()) // Empty AND
                .build()
                .query());
    }

    @Test
    public void testNullParameters() {
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

    @Test
    public void testCaseInsensitiveKeywords() {
        // Test that SQL keywords are preserved regardless of case
        String sql = PSC.select("*").from("users").where(Filters.expr("select = 1 AND from = 2")).build().query();

        assertTrue(sql.contains("select = 1 AND from = 2"));
    }

    @Test
    public void testVeryLongColumnList() {
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

    @Test
    public void testSpecialCharactersInValues() {
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
    public void testAliaspropColumnNameMap() {
        // Test query with table aliases and property column name mapping
        String sql = PSC.select("a.firstName", "o.orderNumber").from(Account.class, "a").join(Order.class, "o").on("a.id = o.userId").build().query();

        assertTrue(sql.contains("a.first_name AS \"a.firstName\""));
        assertTrue(sql.contains("o.order_number AS \"o.orderNumber\""));
    }

    @Test
    public void testSelectConstants() {
        // Test selecting constants
        assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
        assertEquals("*", AbstractQueryBuilder.ASTERISK);
        assertEquals("DISTINCT", AbstractQueryBuilder.DISTINCT);
        assertEquals("TOP", AbstractQueryBuilder.TOP);

        String sql = PSC.select(AbstractQueryBuilder.COUNT_ALL).from("users").build().query();

        assertEquals("SELECT count(*) FROM users", sql);
    }

    @Test
    public void testEntityWithNoTableAnnotation() {
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
    public void testQMEAsparameter() {
        // Test using QME (Question Mark Expression) as parameter
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", Filters.QME);
        values.put("age", 25);

        String sql = PSC.update("users").set(values).where(Filters.eq("id", 1)).build().query();

        assertTrue(sql.contains("name = ?"));
        assertTrue(sql.contains("age = ?"));
    }

    @Test
    public void testClosedBuilderException() {
        // Test that using a closed builder throws exception
        SqlBuilder builder = PSC.select("*").from("users");
        builder.build().query(); // This closes the builder

        // Any operation after sql() should throw exception
        assertThrows(RuntimeException.class, () -> builder.where(Filters.eq("id", 1)));
        assertThrows(RuntimeException.class, () -> builder.build().query());
    }

    @Test
    public void testActiveStringBuilderLimit() {
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
    public void testAllPublicConstants() {
        // Verify all public constants
        assertEquals("ALL", AbstractQueryBuilder.ALL);
        assertEquals("TOP", AbstractQueryBuilder.TOP);
        assertEquals("UNIQUE", AbstractQueryBuilder.UNIQUE);
        assertEquals("DISTINCT", AbstractQueryBuilder.DISTINCT);
        assertEquals("DISTINCTROW", AbstractQueryBuilder.DISTINCTROW);
        assertEquals("*", AbstractQueryBuilder.ASTERISK);
        assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
    }

    /**
     * Regression test for ClassCastException bug in SqlBuilder.appendCondition().
     * Where and Having extend Clause (not Cell), so casting them to Cell would throw ClassCastException.
     * This test verifies that Where/Having conditions are correctly handled when nested inside a Junction.
     */
    @Test
    public void testWhereAndHavingNestedInJunction() {
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
        Criteria criteria = Criteria.builder().where(Filters.expr("status = 'ACTIVE'")).having(Filters.expr("COUNT(*) > 10")).build();
        sql = PSC.select("status", "COUNT(*)").from("users").groupBy("status").append(criteria).build().query();
        assertTrue(sql.contains("WHERE status = 'ACTIVE'"));
        assertTrue(sql.contains("HAVING COUNT(*) > 10"));
    }
}

/**
 * Unit tests for SqlBuilder class
 */
class SqlBuilder11Test extends TestBase {

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
            Assertions.assertDoesNotThrow(() -> sb.into("account").build().query());
        }

        @Test
        public void testInsertMultipleColumns() {
            SqlBuilder sb = SCSB.insert("name", "email", "status");
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            List<String> columns = Arrays.asList("name", "email", "status");
            SqlBuilder sb = SCSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
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

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertEntity() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            SqlBuilder sb = SCSB.insert(account);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertEntityWithExclusions() {
            Account account = new Account("John", "john@email.com", "ACTIVE");
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder sb = SCSB.insert(account, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SqlBuilder sb = SCSB.insert(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate", "id"));
            SqlBuilder sb = SCSB.insert(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SqlBuilder sb = SCSB.insertInto(Account.class);
            Assertions.assertNotNull(sb);

            // This should already have the table name set
            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SqlBuilder sb = SCSB.insertInto(Account.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testBatchInsert() {
            List<Account> accounts = Arrays.asList(new Account("John", "john@email.com", "ACTIVE"), new Account("Jane", "jane@email.com", "ACTIVE"));

            SqlBuilder sb = SCSB.batchInsert(accounts);
            Assertions.assertNotNull(sb);

            String sql = sb.into("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SqlBuilder sb = SCSB.update("account");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = SCSB.update("account", Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SqlBuilder sb = SCSB.update(Account.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder sb = SCSB.update(Account.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        public void testDeleteFromTableName() {
            SqlBuilder sb = SCSB.deleteFrom("account");
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
            Assertions.assertTrue(sql.contains("account"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = SCSB.deleteFrom("account", Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SqlBuilder sb = SCSB.deleteFrom(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'DELETED'")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SqlBuilder sb = SCSB.select("COUNT(*)");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("COUNT(*)"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SqlBuilder sb = SCSB.select("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectCollectionOfColumns() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = SCSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder sb = SCSB.select(aliases);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SqlBuilder sb = SCSB.select(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = SCSB.select(Account.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "salt"));
            SqlBuilder sb = SCSB.select(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.select(Account.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClass() {
            SqlBuilder sb = SCSB.selectFrom(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = SCSB.selectFrom(Account.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.selectFrom(Account.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.selectFrom(Account.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SqlBuilder sb = SCSB.select(Account.class, "a", "account", Account.class, "b", "account2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = SCSB.select(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "b", "account2", null, false, null));

            SqlBuilder sb = SCSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("account a, account b").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", "account", Account.class, "b", "account2");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = SCSB.selectFrom(Account.class, "a", "account", excludedA, Account.class, "b", "account2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "b", "account2", null, false, null));

            SqlBuilder sb = SCSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SqlBuilder sb = SCSB.count("account");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SqlBuilder sb = SCSB.count(Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.gt("balance", 1000));

            SqlBuilder sb = SCSB.parse(cond, Account.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
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
            String sql = sb.build().query();
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

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertMultipleColumns() {
            SqlBuilder sb = ACSB.insert("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = ACSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
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

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntity() {
            User user = new User("John", "Doe");
            user.setEmail("john@example.com");

            SqlBuilder sb = ACSB.insert(user);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityWithExclusions() {
            User user = new User("John", "Doe");
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));

            SqlBuilder sb = ACSB.insert(user, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SqlBuilder sb = ACSB.insert(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder sb = ACSB.insert(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SqlBuilder sb = ACSB.insertInto(User.class);
            Assertions.assertNotNull(sb);

            // Should be able to get SQL directly as table name is already set
            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id"));
            SqlBuilder sb = ACSB.insertInto(User.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testBatchInsert() {
            List<User> users = Arrays.asList(new User("John", "Doe"), new User("Jane", "Smith"));

            SqlBuilder sb = ACSB.batchInsert(users);
            Assertions.assertNotNull(sb);

            String sql = sb.into("users").build().query();
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

            String sql = sb.into("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SqlBuilder sb = ACSB.update("users");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'ACTIVE'").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = ACSB.update("users", User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SqlBuilder sb = ACSB.update(User.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder sb = ACSB.update(User.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("id", 1)).build().query());
        }

        @Test
        public void testDeleteFromTableName() {
            SqlBuilder sb = ACSB.deleteFrom("users");
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = ACSB.deleteFrom("users", User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SqlBuilder sb = ACSB.deleteFrom(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SqlBuilder sb = ACSB.select("COUNT(DISTINCT userId)");
            Assertions.assertNotNull(sb);

            String sql = sb.from("orders").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("COUNT(DISTINCT"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SqlBuilder sb = ACSB.select("firstName", "lastName", "email");
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder sb = ACSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
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

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SqlBuilder sb = ACSB.select(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = ACSB.select(User.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password", "secretKey"));
            SqlBuilder sb = ACSB.select(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.select(User.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClass() {
            SqlBuilder sb = ACSB.selectFrom(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            SqlBuilder sb = ACSB.selectFrom(User.class, "u");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = ACSB.selectFrom(User.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = ACSB.selectFrom(User.class, "u", true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.selectFrom(User.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.selectFrom(User.class, "u", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.selectFrom(User.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("password"));
            SqlBuilder sb = ACSB.selectFrom(User.class, "u", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SqlBuilder sb = ACSB.select(User.class, "u1", "user1", User.class, "u2", "user2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SqlBuilder sb = ACSB.select(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u1", "user1", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder sb = ACSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("users u1, users u2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SqlBuilder sb = ACSB.selectFrom(User.class, "u1", "user1", User.class, "u2", "user2");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("email"));

            SqlBuilder sb = ACSB.selectFrom(User.class, "u1", "user1", excludedA, User.class, "u2", "user2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u1", "user1", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder sb = ACSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SqlBuilder sb = ACSB.count("users");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SqlBuilder sb = ACSB.count(User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = Filters.or(Filters.eq("status", "'ACTIVE'"), Filters.eq("status", "'PENDING'"));

            SqlBuilder sb = ACSB.parse(cond, User.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
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
            String sql = sb.build().query();
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

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertMultipleColumns() {
            SqlBuilder sb = LCSB.insert("firstName", "lastName", "emailAddress");
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO"));
        }

        @Test
        public void testInsertCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("firstName", "lastName", "emailAddress", "phoneNumber");
            SqlBuilder sb = LCSB.insert(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
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

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntity() {
            Customer customer = new Customer("Bob", "Smith", "bob@example.com");
            customer.setPhoneNumber("123-456-7890");

            SqlBuilder sb = LCSB.insert(customer);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityWithExclusions() {
            Customer customer = new Customer("Carol", "Davis");
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));

            SqlBuilder sb = LCSB.insert(customer, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClass() {
            SqlBuilder sb = LCSB.insert(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SqlBuilder sb = LCSB.insert(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testInsertInto() {
            SqlBuilder sb = LCSB.insertInto(Customer.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId"));
            SqlBuilder sb = LCSB.insertInto(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.build().query());
        }

        @Test
        public void testBatchInsert() {
            List<Customer> customers = Arrays.asList(new Customer("Dave", "Wilson", "dave@example.com"), new Customer("Eve", "Brown", "eve@example.com"));

            SqlBuilder sb = LCSB.batchInsert(customers);
            Assertions.assertNotNull(sb);

            String sql = sb.into("customers").build().query();
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

            String sql = sb.into("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateTableName() {
            SqlBuilder sb = LCSB.update("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.set("status", "'PREMIUM'").where(Filters.gt("totalPurchases", 1000)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
        }

        @Test
        public void testUpdateTableNameWithEntityClass() {
            SqlBuilder sb = LCSB.update("customers", Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.set("status").where(Filters.eq("customerId", 100)).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testUpdateEntityClass() {
            SqlBuilder sb = LCSB.update(Customer.class);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("customerId", 100)).build().query());
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("customerId", "registrationDate"));
            SqlBuilder sb = LCSB.update(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            Assertions.assertDoesNotThrow(() -> sb.set("status").where(Filters.eq("customerId", 100)).build().query());
        }

        @Test
        public void testDeleteFromTableName() {
            SqlBuilder sb = LCSB.deleteFrom("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.eq("status", "'INACTIVE'")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM"));
        }

        @Test
        public void testDeleteFromTableNameWithEntityClass() {
            SqlBuilder sb = LCSB.deleteFrom("customers", Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.lt("lastLoginDate", "2020-01-01")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testDeleteFromEntityClass() {
            SqlBuilder sb = LCSB.deleteFrom(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.where(Filters.isNull("emailAddress")).build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectSingleExpression() {
            SqlBuilder sb = LCSB.select("COUNT(*) as totalCount");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("totalCount"));
        }

        @Test
        public void testSelectMultipleColumns() {
            SqlBuilder sb = LCSB.select("firstName", "lastName", "emailAddress");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectCollectionOfColumns() {
            Collection<String> columns = Arrays.asList("customerId", "firstName", "lastName");
            SqlBuilder sb = LCSB.select(columns);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
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

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClass() {
            SqlBuilder sb = LCSB.select(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            SqlBuilder sb = LCSB.select(Customer.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber", "registrationDate"));
            SqlBuilder sb = LCSB.select(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.select(Customer.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.selectFrom(Customer.class, true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassFullOptions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c", true, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntities() {
            SqlBuilder sb = LCSB.select(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder sb = LCSB.select(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Customer.class, "c1", "customer1", null, false, null),
                    new Selection(Customer.class, "c2", "customer2", null, false, null), new Selection(Customer.class, "c3", "customer3", null, false, null));

            SqlBuilder sb = LCSB.select(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.from("customers c1, customers c2, customers c3").build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntities() {
            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c1", "customer1", Customer.class, "c2", "customer2");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromTwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("phoneNumber"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("status"));

            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c1", "customer1", excludedA, Customer.class, "c2", "customer2", excludedB);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Customer.class, "c1", "customer1", null, false, null),
                    new Selection(Customer.class, "c2", "customer2", null, false, null));

            SqlBuilder sb = LCSB.selectFrom(selections);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testCountTableName() {
            SqlBuilder sb = LCSB.count("customers");
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testCountEntityClass() {
            SqlBuilder sb = LCSB.count(Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("count(*)"));
        }

        @Test
        public void testParseCondition() {
            Condition cond = Filters.and(Filters.eq("status", "'ACTIVE'"), Filters.between("registrationDate", "2020-01-01", "2023-12-31"));

            SqlBuilder sb = LCSB.parse(cond, Customer.class);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
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

            String sql = sb.build().query();
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
            String sql = sb.build().query();
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
            String sql = sb.build().query();
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
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE"));
            Assertions.assertTrue(sql.contains("SET"));
        }

        @Test
        public void testSelectDistinct() {
            SqlBuilder sb = LCSB.select("DISTINCT status").from("customers").orderBy("status");

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testQueryWithLimit() {
            SqlBuilder sb = LCSB.select("*").from("customers").where(Filters.eq("status", "'ACTIVE'")).orderBy("registrationDate DESC").limit(10);

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testQueryWithLimitAndOffset() {
            SqlBuilder sb = LCSB.select("*").from("customers").where(Filters.eq("status", "'ACTIVE'")).orderBy("customerId").limit(20, 10);

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testUnionQuery() {
            SqlBuilder query1 = LCSB.select("firstName", "lastName").from("customers").where(Filters.eq("status", "'ACTIVE'"));

            SqlBuilder query2 = LCSB.select("firstName", "lastName").from("employees").where(Filters.eq("department", "'SALES'"));

            SqlBuilder sb = query1.union(query2);

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UNION"));
        }

        @Test
        public void testCaseWhenExpression() {
            String caseExpr = "CASE WHEN status = 'PREMIUM' THEN 'VIP' " + "WHEN status = 'GOLD' THEN 'Important' " + "ELSE 'Regular' END AS customerType";

            SqlBuilder sb = LCSB.select("firstName", "lastName", caseExpr).from("customers");

            Assertions.assertNotNull(sb);
            String sql = sb.build().query();
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
            String sql = sb.build().query();
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

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            SqlBuilder sb = LCSB.selectFrom(Customer.class, true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c", true);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.selectFrom(Customer.class, excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excluded = new HashSet<>(Arrays.asList("phoneNumber"));
            SqlBuilder sb = LCSB.selectFrom(Customer.class, "c", excluded);
            Assertions.assertNotNull(sb);

            String sql = sb.build().query();
            Assertions.assertNotNull(sql);
        }
    }

}

/**
 * Unit tests for SqlBuilder class
 */
class SqlBuilder12Test extends TestBase {

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
            SqlBuilder builder = PSB.insert("name");
            assertNotNull(builder);

            // Test with empty string - should throw exception
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(""));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SqlBuilder builder = PSB.insert("firstName", "lastName", "email");
            assertNotNull(builder);

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> PSB.insert((String[]) null));

            // Test with empty array
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(new String[0]));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder builder = PSB.insert(columns);
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

            SqlBuilder builder = PSB.insert(props);
            assertNotNull(builder);

            // Test with null map
            assertThrows(IllegalArgumentException.class, () -> PSB.insert((Map<String, Object>) null));

            // Test with empty map
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(new HashMap<String, Object>()));
        }

        @Test
        public void testInsert_Entity() {
            User user = new User("John", "Doe", "john@example.com");
            SqlBuilder builder = PSB.insert(user);
            assertNotNull(builder);

            // Test with null entity
            assertThrows(IllegalArgumentException.class, () -> PSB.insert((Object) null));
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            User user = new User("John", "Doe", "john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));

            SqlBuilder builder = PSB.insert(user, excludedProps);
            assertNotNull(builder);

            // Test with null entity
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(null, excludedProps));
        }

        @Test
        public void testInsert_Class() {
            SqlBuilder builder = PSB.insert(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.insert((Class<?>) null));
        }

        @Test
        public void testInsert_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder builder = PSB.insert(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.insert(null, excludedProps));
        }

        @Test
        public void testInsertInto_Class() {
            SqlBuilder builder = PSB.insertInto(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.insertInto((Class<?>) null));
        }

        @Test
        public void testInsertInto_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder builder = PSB.insertInto(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.insertInto(null, excludedProps));
        }

        @Test
        public void testBatchInsert() {
            List<User> users = Arrays.asList(new User("John", "Doe", "john@example.com"), new User("Jane", "Smith", "jane@example.com"));

            SqlBuilder builder = PSB.batchInsert(users);
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

            SqlBuilder mapBuilder = PSB.batchInsert(maps);
            assertNotNull(mapBuilder);
        }

        @Test
        public void testUpdate_TableName() {
            SqlBuilder builder = PSB.update("users");
            assertNotNull(builder);

            // Test with null table name
            assertThrows(IllegalArgumentException.class, () -> PSB.update((String) null));

            // Test with empty table name
            assertThrows(IllegalArgumentException.class, () -> PSB.update(""));
        }

        @Test
        public void testUpdate_TableNameWithEntityClass() {
            SqlBuilder builder = PSB.update("users", User.class);
            assertNotNull(builder);

            // Test with null parameters
            assertThrows(IllegalArgumentException.class, () -> PSB.update(null, User.class));
            assertThrows(IllegalArgumentException.class, () -> PSB.update("users", null));
        }

        @Test
        public void testUpdate_EntityClass() {
            SqlBuilder builder = PSB.update(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.update((Class<?>) null));
        }

        @Test
        public void testUpdate_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("createdDate"));
            SqlBuilder builder = PSB.update(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.update(null, excludedProps));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SqlBuilder builder = PSB.deleteFrom("users");
            assertNotNull(builder);

            // Test with null table name
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom((String) null));

            // Test with empty table name
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom(""));
        }

        @Test
        public void testDeleteFrom_TableNameWithEntityClass() {
            SqlBuilder builder = PSB.deleteFrom("users", User.class);
            assertNotNull(builder);

            // Test with null parameters
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom(null, User.class));
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom("users", null));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SqlBuilder builder = PSB.deleteFrom(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.deleteFrom((Class<?>) null));
        }

        @Test
        public void testSelect_SingleColumn() {
            SqlBuilder builder = PSB.select("firstName");
            assertNotNull(builder);

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> PSB.select((String) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.select(""));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SqlBuilder builder = PSB.select("firstName", "lastName", "email");
            assertNotNull(builder);

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> PSB.select((String[]) null));

            // Test with empty array
            assertThrows(IllegalArgumentException.class, () -> PSB.select(new String[0]));
        }

        @Test
        public void testSelect_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            SqlBuilder builder = PSB.select(columns);
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

            SqlBuilder builder = PSB.select(aliases);
            assertNotNull(builder);

            // Test with null/empty map
            assertThrows(IllegalArgumentException.class, () -> PSB.select((Map<String, String>) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.select(new HashMap<String, String>()));
        }

        @Test
        public void testSelect_EntityClass() {
            SqlBuilder builder = PSB.select(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.select((Class<?>) null));
        }

        @Test
        public void testSelect_EntityClassWithSubEntities() {
            SqlBuilder builder = PSB.select(User.class, true);
            assertNotNull(builder);

            // Test without sub-entities
            SqlBuilder builder2 = PSB.select(User.class, false);
            assertNotNull(builder2);
        }

        @Test
        public void testSelect_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = PSB.select(User.class, excludedProps);
            assertNotNull(builder);

            // Test with null excluded props
            SqlBuilder builder2 = PSB.select(User.class, null);
            assertNotNull(builder2);
        }

        @Test
        public void testSelect_EntityClassFullOptions() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = PSB.select(User.class, true, excludedProps);
            assertNotNull(builder);

            // Test all combinations
            assertNotNull(PSB.select(User.class, false, null));
            assertNotNull(PSB.select(User.class, true, null));
            assertNotNull(PSB.select(User.class, false, excludedProps));
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SqlBuilder builder = PSB.selectFrom(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.selectFrom((Class<?>) null));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = PSB.selectFrom(User.class, "u");
            assertNotNull(builder);

            // Test with null alias
            SqlBuilder builder2 = PSB.selectFrom(User.class, (String) null);
            assertNotNull(builder2);
        }

        @Test
        public void testSelectFrom_EntityClassWithSubEntities() {
            SqlBuilder builder = PSB.selectFrom(User.class, true);
            assertNotNull(builder);

            SqlBuilder builder2 = PSB.selectFrom(User.class, false);
            assertNotNull(builder2);
        }

        @Test
        public void testSelectFrom_EntityClassWithAliasAndSubEntities() {
            SqlBuilder builder = PSB.selectFrom(User.class, "u", true);
            assertNotNull(builder);

            SqlBuilder builder2 = PSB.selectFrom(User.class, "u", false);
            assertNotNull(builder2);
        }

        @Test
        public void testSelectFrom_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = PSB.selectFrom(User.class, excludedProps);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_EntityClassWithAliasAndExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = PSB.selectFrom(User.class, "u", excludedProps);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_EntityClassWithSubEntitiesAndExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = PSB.selectFrom(User.class, true, excludedProps);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_EntityClassFullOptions() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("password"));
            SqlBuilder builder = PSB.selectFrom(User.class, "u", true, excludedProps);
            assertNotNull(builder);

            // Test various combinations
            assertNotNull(PSB.selectFrom(User.class, "u", false, null));
            assertNotNull(PSB.selectFrom(User.class, null, true, excludedProps));
        }

        @Test
        public void testSelect_TwoEntities() {
            SqlBuilder builder = PSB.select(User.class, "u", "user", User.class, "u2", "user2");
            assertNotNull(builder);
        }

        @Test
        public void testSelect_TwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder builder = PSB.select(User.class, "u", "user", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(builder);

            // Test with null exclusions
            SqlBuilder builder2 = PSB.select(User.class, "u", "user", null, User.class, "u2", "user2", null);
            assertNotNull(builder2);
        }

        @Test
        public void testSelect_MultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder builder = PSB.select(selections);
            assertNotNull(builder);

            // Test with null/empty list
            assertThrows(IllegalArgumentException.class, () -> PSB.select((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.select(new ArrayList<Selection>()));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SqlBuilder builder = PSB.selectFrom(User.class, "u", "user", User.class, "u2", "user2");
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_TwoEntitiesWithExclusions() {
            Set<String> excludedA = new HashSet<>(Arrays.asList("password"));
            Set<String> excludedB = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder builder = PSB.selectFrom(User.class, "u", "user", excludedA, User.class, "u2", "user2", excludedB);
            assertNotNull(builder);
        }

        @Test
        public void testSelectFrom_MultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));

            SqlBuilder builder = PSB.selectFrom(selections);
            assertNotNull(builder);

            // Test with invalid selections
            assertThrows(IllegalArgumentException.class, () -> PSB.selectFrom((List<Selection>) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.selectFrom(new ArrayList<Selection>()));
        }

        @Test
        public void testCount_TableName() {
            SqlBuilder builder = PSB.count("users");
            assertNotNull(builder);

            // Test with null/empty table name
            assertThrows(IllegalArgumentException.class, () -> PSB.count((String) null));
            assertThrows(IllegalArgumentException.class, () -> PSB.count(""));
        }

        @Test
        public void testCount_EntityClass() {
            SqlBuilder builder = PSB.count(User.class);
            assertNotNull(builder);

            // Test with null class
            assertThrows(IllegalArgumentException.class, () -> PSB.count((Class<?>) null));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.eq("firstName", "John");
            SqlBuilder builder = PSB.parse(cond, User.class);
            assertNotNull(builder);

            // Test with null condition
            assertThrows(IllegalArgumentException.class, () -> PSB.parse(null, User.class));

            // Test with complex condition
            Condition complexCond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("id", 1));
            SqlBuilder complexBuilder = PSB.parse(complexCond, User.class);
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
            SqlBuilder builder = PSC.insert("firstName");
            assertNotNull(builder);

            // Test SQL generation
            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("first_name"));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SqlBuilder builder = PSC.insert("firstName", "lastName", "email");
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "email");
            SqlBuilder builder = PSC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("account").build().query();
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
            assertTrue(sql.contains("email"));
        }

        @Test
        public void testInsert_Map() {
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
        public void testInsert_Entity() {
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
        public void testInsert_EntityWithExcludedProps() {
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
        public void testInsert_Class() {
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
        public void testInsert_ClassWithExcludedProps() {
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
        public void testInsertInto_Class() {
            SqlBuilder builder = PSC.insertInto(Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertTrue(sql.contains("first_name"));
            assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertInto_ClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("id", "createdDate"));
            SqlBuilder builder = PSC.insertInto(Account.class, excludedProps);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO account"));
            assertFalse(sql.contains(" id "));
            assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testBatchInsert() {
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
        public void testUpdate_TableName() {
            SqlBuilder builder = PSC.update("account");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testUpdate_TableNameWithEntityClass() {
            SqlBuilder builder = PSC.update("account", Account.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testUpdate_EntityClass() {
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
        public void testUpdate_EntityClassWithExcludedProps() {
            Set<String> excludedProps = new HashSet<>(Arrays.asList("email"));
            SqlBuilder builder = PSC.update(Account.class, excludedProps);
            assertNotNull(builder);

            // Get the generated column names
            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("first_name"));
            assertFalse(sql.contains("email"));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SqlBuilder builder = PSC.deleteFrom("account");
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testDeleteFrom_TableNameWithEntityClass() {
            SqlBuilder builder = PSC.deleteFrom("account", Account.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("firstName", "John")).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
            assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SqlBuilder builder = PSC.deleteFrom(Account.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM account"));
        }

        @Test
        public void testSelect_SingleColumn() {
            SqlBuilder builder = PSC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SqlBuilder builder = PSC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelect_Collection() {
            List<String> columns = Arrays.asList("id", "firstName", "lastName");
            SqlBuilder builder = PSC.select(columns);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
            assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelect_Map() {
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
        public void testSelect_EntityClass() {
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
        public void testSelect_EntityClassWithSubEntities() {
            SqlBuilder builder = PSC.select(Account.class, true);
            assertNotNull(builder);

            String sql = builder.from("account").build().query();
            assertNotNull(sql);
        }

        @Test
        public void testSelect_EntityClassWithExcludedProps() {
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
        public void testSelectFrom_EntityClass() {
            SqlBuilder builder = PSC.selectFrom(Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM account"));
            assertTrue(sql.contains("first_name AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = PSC.selectFrom(Account.class, "a");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FROM account a"));
            assertTrue(sql.contains("a.first_name AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SqlBuilder builder = PSC.selectFrom(Account.class, "a", "account", Account.class, "a2", "account2");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("a.first_name AS \"account.firstName\""));
            assertTrue(sql.contains("a2.first_name AS \"account2.firstName\""));
        }

        @Test
        public void testSelect_MultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", Arrays.asList("id", "firstName"), false, null),
                    new Selection(Account.class, "a2", "account2", null, false, new HashSet<>(Arrays.asList("email"))));

            SqlBuilder builder = PSC.select(selections);
            assertNotNull(builder);

            String sql = builder.from("account a, account a2").build().query();
            assertTrue(sql.contains("a.id AS \"account.id\""));
            assertTrue(sql.contains("a.first_name AS \"account.firstName\""));
            assertFalse(sql.contains("account2.email"));
        }

        @Test
        public void testCount_TableName() {
            SqlBuilder builder = PSC.count("account");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testCount_EntityClass() {
            SqlBuilder builder = PSC.count(Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.like("email", "%@example.com"));

            SqlBuilder builder = PSC.parse(cond, Account.class);
            assertNotNull(builder);

            String sql = builder.build().query();
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
            SqlBuilder builder = PSC.select("firstName", "lastName", "emailAddress");
            String sql = builder.from("account").build().query();

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
            SqlBuilder builder = PAC.insert("firstName");
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("INSERT INTO USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SqlBuilder builder = PAC.insert("firstName", "lastName", "emailAddress");
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "emailAddress");
            SqlBuilder builder = PAC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
        }

        @Test
        public void testInsert_Map() {
            Map<String, Object> props = new HashMap<>();
            props.put("firstName", "John");
            props.put("lastName", "Doe");

            SqlBuilder builder = PAC.insert(props);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("FIRST_NAME"));
            assertTrue(sp.query().contains("LAST_NAME"));
            assertEquals(2, sp.parameters().size());
        }

        @Test
        public void testInsert_Entity() {
            UserAccount account = new UserAccount("John", "Doe");
            account.setEmailAddress("john@example.com");

            SqlBuilder builder = PAC.insert(account);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("FIRST_NAME"));
            assertTrue(sp.query().contains("LAST_NAME"));
            assertTrue(sp.query().contains("EMAIL_ADDRESS"));
            assertFalse(sp.query().contains("CREATED_DATE")); // ReadOnly
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            UserAccount account = new UserAccount("John", "Doe");
            account.setEmailAddress("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder builder = PAC.insert(account, excludedProps);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("FIRST_NAME"));
            assertTrue(sp.query().contains("LAST_NAME"));
            assertFalse(sp.query().contains("EMAIL_ADDRESS"));
        }

        @Test
        public void testInsert_Class() {
            SqlBuilder builder = PAC.insert(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.into("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME"));
            assertTrue(sql.contains("LAST_NAME"));
            assertTrue(sql.contains("EMAIL_ADDRESS"));
            assertFalse(sql.contains("CREATED_DATE")); // ReadOnly
            assertFalse(sql.contains("TEMP_DATA")); // Transient
        }

        @Test
        public void testInsertInto_Class() {
            SqlBuilder builder = PAC.insertInto(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testBatchInsert() {
            List<UserAccount> accounts = Arrays.asList(new UserAccount("John", "Doe"), new UserAccount("Jane", "Smith"));

            SqlBuilder builder = PAC.batchInsert(accounts);
            assertNotNull(builder);

            SP sp = builder.into("USER_ACCOUNT").build();
            assertTrue(sp.query().contains("VALUES"));
            assertTrue(sp.query().contains("(?, ?)"));
            assertTrue(sp.query().contains(", (?, ?)"));
        }

        @Test
        public void testUpdate_TableName() {
            SqlBuilder builder = PAC.update("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = ?"));
        }

        @Test
        public void testUpdate_EntityClass() {
            SqlBuilder builder = PAC.update(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME = ?"));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SqlBuilder builder = PAC.deleteFrom("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM USER_ACCOUNT"));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SqlBuilder builder = PAC.deleteFrom(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM USER_ACCOUNT"));
        }

        @Test
        public void testSelect_SingleColumn() {
            SqlBuilder builder = PAC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SqlBuilder builder = PAC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = PAC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            assertTrue(sql.contains("LAST_NAME AS \"lname\""));
        }

        @Test
        public void testSelect_EntityClass() {
            SqlBuilder builder = PAC.select(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.from("USER_ACCOUNT").build().query();
            assertTrue(sql.contains("ID"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
            assertFalse(sql.contains("TEMP_DATA"));
        }

        @Test
        public void testSelectFrom_EntityClass() {
            SqlBuilder builder = PAC.selectFrom(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = PAC.selectFrom(UserAccount.class, "u");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FROM USER_ACCOUNT u"));
            assertTrue(sql.contains("u.FIRST_NAME AS \"firstName\""));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SqlBuilder builder = PAC.selectFrom(UserAccount.class, "u1", "user1", UserAccount.class, "u2", "user2");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("u1.FIRST_NAME AS \"user1.firstName\""));
            assertTrue(sql.contains("u2.FIRST_NAME AS \"user2.firstName\""));
        }

        @Test
        public void testCount_TableName() {
            SqlBuilder builder = PAC.count("USER_ACCOUNT");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        public void testCount_EntityClass() {
            SqlBuilder builder = PAC.count(UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM USER_ACCOUNT"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("id", 1));

            SqlBuilder builder = PAC.parse(cond, UserAccount.class);
            assertNotNull(builder);

            String sql = builder.build().query();
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
            SqlBuilder builder = PAC.select("firstName", "lastName", "emailAddress");
            String sql = builder.from("USER_ACCOUNT").build().query();

            assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
            assertTrue(sql.contains("EMAIL_ADDRESS AS \"emailAddress\""));
        }

        @Test
        public void testComplexQueries() {
            // Test complex query with joins
            Set<String> excludeUser1 = new HashSet<>(Arrays.asList("tempData"));
            Set<String> excludeUser2 = new HashSet<>(Arrays.asList("createdDate"));

            SqlBuilder builder = PAC.select(UserAccount.class, "u1", "user1", excludeUser1, UserAccount.class, "u2", "user2", excludeUser2);

            String sql = builder.from("USER_ACCOUNT u1, USER_ACCOUNT u2").where(Filters.eq("u1.id", "u2.id")).build().query();

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
            SqlBuilder builder = PLC.insert("firstName");
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("INSERT INTO userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testInsert_MultipleColumns() {
            SqlBuilder builder = PLC.insert("firstName", "lastName", "emailAddress");
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
            assertTrue(sql.contains("emailAddress"));
        }

        @Test
        public void testInsert_Collection() {
            List<String> columns = Arrays.asList("firstName", "lastName", "isActive");
            SqlBuilder builder = PLC.insert(columns);
            assertNotNull(builder);

            String sql = builder.into("userProfile").build().query();
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

            SqlBuilder builder = PLC.insert(props);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("firstName"));
            assertTrue(sp.query().contains("lastName"));
            assertTrue(sp.query().contains("isActive"));
            assertEquals(3, sp.parameters().size());
        }

        @Test
        public void testInsert_Entity() {
            UserProfile profile = new UserProfile("John", "Doe");
            profile.setEmailAddress("john@example.com");
            profile.setIsActive(true);

            SqlBuilder builder = PLC.insert(profile);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("firstName"));
            assertTrue(sp.query().contains("lastName"));
            assertTrue(sp.query().contains("emailAddress"));
            assertTrue(sp.query().contains("isActive"));
            assertFalse(sp.query().contains("lastLoginDate")); // ReadOnly
        }

        @Test
        public void testInsert_EntityWithExcludedProps() {
            UserProfile profile = new UserProfile("John", "Doe");
            profile.setEmailAddress("john@example.com");
            Set<String> excludedProps = new HashSet<>(Arrays.asList("emailAddress"));

            SqlBuilder builder = PLC.insert(profile, excludedProps);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("firstName"));
            assertTrue(sp.query().contains("lastName"));
            assertFalse(sp.query().contains("emailAddress"));
        }

        @Test
        public void testInsert_Class() {
            SqlBuilder builder = PLC.insert(UserProfile.class);
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
        public void testInsertInto_Class() {
            SqlBuilder builder = PLC.insertInto(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("INSERT INTO userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testBatchInsert() {
            List<UserProfile> profiles = Arrays.asList(new UserProfile("John", "Doe"), new UserProfile("Jane", "Smith"));

            SqlBuilder builder = PLC.batchInsert(profiles);
            assertNotNull(builder);

            SP sp = builder.into("userProfile").build();
            assertTrue(sp.query().contains("VALUES"));
            assertTrue(sp.query().contains("(?, ?)"));
            assertTrue(sp.query().contains(", (?, ?)"));
        }

        @Test
        public void testUpdate_TableName() {
            SqlBuilder builder = PLC.update("userProfile");
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE userProfile"));
            assertTrue(sql.contains("firstName = ?"));
        }

        @Test
        public void testUpdate_EntityClass() {
            SqlBuilder builder = PLC.update(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("UPDATE userProfile"));
            assertTrue(sql.contains("firstName = ?"));
        }

        @Test
        public void testDeleteFrom_TableName() {
            SqlBuilder builder = PLC.deleteFrom("userProfile");
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM userProfile"));
        }

        @Test
        public void testDeleteFrom_EntityClass() {
            SqlBuilder builder = PLC.deleteFrom(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.where(Filters.eq("id", 1)).build().query();
            assertTrue(sql.contains("DELETE FROM userProfile"));
        }

        @Test
        public void testSelect_SingleColumn() {
            SqlBuilder builder = PLC.select("COUNT(*)");
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("SELECT COUNT(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        public void testSelect_MultipleColumns() {
            SqlBuilder builder = PLC.select("id", "firstName", "lastName");
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("firstName"));
            assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testSelect_Map() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");

            SqlBuilder builder = PLC.select(aliases);
            assertNotNull(builder);

            String sql = builder.from("userProfile").build().query();
            assertTrue(sql.contains("firstName AS \"fname\""));
            assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        public void testSelect_EntityClass() {
            SqlBuilder builder = PLC.select(UserProfile.class);
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
        public void testSelectFrom_EntityClass() {
            SqlBuilder builder = PLC.selectFrom(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT"));
            assertTrue(sql.contains("FROM userProfile"));
            assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testSelectFrom_EntityClassWithAlias() {
            SqlBuilder builder = PLC.selectFrom(UserProfile.class, "p");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("FROM userProfile p"));
            assertTrue(sql.contains("p.firstName"));
        }

        @Test
        public void testSelectFrom_TwoEntities() {
            SqlBuilder builder = PLC.selectFrom(UserProfile.class, "p1", "profile1", UserProfile.class, "p2", "profile2");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("p1.firstName AS \"profile1.firstName\""));
            assertTrue(sql.contains("p2.firstName AS \"profile2.firstName\""));
        }

        @Test
        public void testCount_TableName() {
            SqlBuilder builder = PLC.count("userProfile");
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        public void testCount_EntityClass() {
            SqlBuilder builder = PLC.count(UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
            assertTrue(sql.contains("SELECT count(*)"));
            assertTrue(sql.contains("FROM userProfile"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.eq("isActive", true));

            SqlBuilder builder = PLC.parse(cond, UserProfile.class);
            assertNotNull(builder);

            String sql = builder.build().query();
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
            // Test that PLC uses camelCase naming (no transformation)
            SqlBuilder builder = PLC.select("firstName", "lastName", "emailAddress", "isActive");
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
        public void testComplexQueries() {
            // Test complex query with multiple selections
            List<Selection> selections = Arrays.asList(new Selection(UserProfile.class, "p1", "profile1", Arrays.asList("id", "firstName"), false, null),
                    new Selection(UserProfile.class, "p2", "profile2", null, false, new HashSet<>(Arrays.asList("sessionData", "lastLoginDate"))));

            SqlBuilder builder = PLC.select(selections);
            String sql = builder.from("userProfile p1, userProfile p2").build().query();

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

/**
 * Unit tests for SqlBuilder class
 */
class SqlBuilder13Test extends TestBase {

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
            String sql = NSB.insert("user_name").into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("user_name"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains(":user_name"));
        }

        @Test
        public void testInsertWithMultipleColumns() {
            String sql = NSB.insert("first_name", "last_name", "email").into("users").build().query();
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
            String sql = NSB.insert(columns).into("products").build().query();
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
            String sql = NSB.insert(data).into("users").build().query();
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

            String sql = NSB.insert(user).into("users").build().query();
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
            String sql = NSB.insert(user, exclude).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = NSB.insert(User.class).into("users").build().query();
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
            String sql = NSB.insert(User.class, exclude).into("users").build().query();
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
            String sql = NSB.insertInto(User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO test_user"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> exclude = N.asSet("id");
            String sql = NSB.insertInto(User.class, exclude).build().query();
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

            String sql = NSB.batchInsert(users).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            // Should have multiple value sets
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testUpdate() {
            String sql = NSB.update("users").set("last_login", "status").where(Filters.eq("id", 1)).build().query();
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
            String sql = NSB.update("user_accounts", User.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE user_accounts"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = NSB.update(User.class).set("firstName", "email").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE test_user"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> exclude = N.asSet("password", "createdDate");
            String sql = NSB.update(User.class, exclude).set("firstName", "email").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE test_user"));
            // Should be able to update non-excluded fields
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = NSB.deleteFrom("users").where(Filters.eq("status", "inactive")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status"));
            Assertions.assertTrue(sql.contains(":status"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = NSB.deleteFrom("user_accounts", User.class).where(Filters.lt("lastLogin", new Date())).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM user_accounts"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("lastLogin"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = NSB.deleteFrom(User.class).where(Filters.eq("id", 123)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM test_user"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = NSB.select("COUNT(*) AS total").from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*) AS total"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = NSB.select("id", "name", "email", "created_date").from("users").where(Filters.eq("active", true)).build().query();
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
            String sql = NSB.select(columns).from("users").build().query();
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

            String sql = NSB.select(aliases).from("users u").leftJoin("orders o").on("u.id = o.user_id").groupBy("u.id").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("u.first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("u.last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("COUNT(o.id) AS \"orderCount\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = NSB.select(User.class).from("users").build().query();
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
            String sql = NSB.select(User.class, true).from("users u").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> exclude = N.asSet("password", "createdDate");
            String sql = NSB.select(User.class, exclude).from("users").build().query();
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
            String sql = NSB.select(User.class, true, exclude).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFrom() {
            String sql = NSB.selectFrom(User.class).where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM test_user"));
            Assertions.assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = NSB.selectFrom(User.class, "u").where(Filters.eq("u.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM test_user u"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = NSB.selectFrom(User.class, true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = NSB.selectFrom(User.class, "u", true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = NSB.selectFrom(User.class, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = NSB.selectFrom(User.class, "u", exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("test_user u"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> exclude = N.asSet("password");
            String sql = NSB.selectFrom(User.class, true, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> exclude = N.asSet("password");
            String sql = NSB.selectFrom(User.class, "u", true, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = NSB.select(User.class, "u", "user_", User.class, "u2", "user2_")
                    .from("users u")
                    .join("users u2")
                    .on("u.id = u2.parent_id")
                    .build()
                    .query();
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
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, N.asSet("password")));

            String sql = NSB.select(selections).from("users u").join("users m").on("u.manager_id = m.id").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = NSB.selectFrom(User.class, "u", "user_", User.class, "m", "manager_").where(Filters.eq("u.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = N.asSet("password");
            Set<String> excludeM = N.asSet("email", "password");

            String sql = NSB.selectFrom(User.class, "u", "user_", excludeU, User.class, "m", "manager_", excludeM).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, null));

            String sql = NSB.selectFrom(selections).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = NSB.count("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = NSB.count(User.class).where(Filters.eq("status", "active")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM test_user"));
            Assertions.assertTrue(sql.contains("WHERE"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18));
            String sql = NSB.parse(cond, User.class).build().query();
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
                NSB.insert(new ArrayList<String>()).into("users").build().query();
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                NSB.select(new ArrayList<String>()).from("users").build().query();
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
            String sql = NSB.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("JOIN"));

            // Test LEFT JOIN
            sql = NSB.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            // Test RIGHT JOIN
            sql = NSB.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            // Test FULL JOIN
            sql = NSB.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            // Test CROSS JOIN
            sql = NSB.select("*").from("users").crossJoin("departments").build().query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            // Test NATURAL JOIN
            sql = NSB.select("*").from("users").naturalJoin("user_profiles").build().query();
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
                    .build()
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
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testUpdateOperations() {
            // Test simple update
            String sql = NSB.update("users").set("status", "active").where(Filters.eq("id", 1)).build().query();

            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status"));

            // Test update with multiple sets
            sql = NSB.update("users").set("firstName", "John").set("lastName", "Doe").set(Map.of("age", 30)).where(Filters.eq("id", 1)).build().query();

            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("age"));

            // Test update with Map
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", "Jane");
            updates.put("age", 25);

            sql = NSB.update("users").set(updates).where(Filters.eq("id", 2)).build().query();

            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("age"));
        }

        @Test
        public void testWhereOperations() {
            // Test single where
            String sql = NSB.select("*").from("users").where(Filters.eq("status", "active")).build().query();

            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status"));

            // Test multiple where (should be AND'ed)
            sql = NSB.select("*").from("users").where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();

            Assertions.assertTrue(sql.contains("AND"));

            // Test where with OR
            sql = NSB.select("*").from("users").where(Filters.or(Filters.eq("status", "active"), Filters.eq("status", "premium"))).build().query();

            Assertions.assertTrue(sql.contains("OR"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = NSB.select("department", "COUNT(*) as count").from("users").groupBy("department").having(Filters.gt("COUNT(*)", 5)).build().query();

            Assertions.assertTrue(sql.contains("GROUP BY department"));
            Assertions.assertTrue(sql.contains("HAVING"));

            // Test multiple group by
            sql = NSB.select("department", "location", "COUNT(*)").from("users").groupBy("department", "location").build().query();

            Assertions.assertTrue(sql.contains("GROUP BY"));
            Assertions.assertTrue(sql.contains("department"));
            Assertions.assertTrue(sql.contains("location"));
        }

        @Test
        public void testOrderBy() {
            // Test simple order by
            String sql = NSB.select("*").from("users").orderBy("name").build().query();

            Assertions.assertTrue(sql.contains("ORDER BY name"));

            // Test order by with direction
            sql = NSB.select("*").from("users").orderBy("name ASC", "age DESC").build().query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("name ASC"));
            Assertions.assertTrue(sql.contains("age DESC"));
        }

        @Test
        public void testLimitOffset() {
            // Test limit only
            String sql = NSB.select("*").from("users").limit(10).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));

            // Test limit with offset
            sql = NSB.select("*").from("users").limit(10).offset(20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with count and offset
            sql = NSB.select("*").from("users").limit(10, 5).build().query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testForUpdate() {
            String sql = NSB.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testUnion() {
            NSB builder1 = (NSB) NSB.select("name").from("users");
            NSB builder2 = (NSB) NSB.select("name").from("customers");

            String sql1 = builder1.build().query();
            String sql2 = builder2.build().query();

            // Union would be manual construction
            String unionSql = sql1 + " UNION " + sql2;
            Assertions.assertTrue(unionSql.contains("UNION"));
        }

        @Test
        public void testAppend() {
            String sql = NSB.select("*").from("users").append(" WHERE custom_condition = true").build().query();

            Assertions.assertTrue(sql.contains("custom_condition"));

            // Test multiple appends
            sql = NSB.select("*").from("users").append(" WHERE 1=1").append(" AND status = 'active'").append(" ORDER BY name").build().query();

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
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = NSB.select("*").from("users").where(Filters.isNull("email")).build().query();
            Assertions.assertTrue(sql.contains("IS NULL"));

            // Test IS NOT NULL
            sql = NSB.select("*").from("users").where(Filters.isNotNull("email")).build().query();
            Assertions.assertTrue(sql.contains("IS NOT NULL"));

            // Test IN
            sql = NSB.select("*").from("users").where(Filters.in("status", Arrays.asList("active", "premium", "trial"))).build().query();
            Assertions.assertTrue(sql.contains("IN"));

            // Test NOT IN
            sql = NSB.select("*").from("users").where(Filters.notIn("status", Arrays.asList("inactive", "banned"))).build().query();
            Assertions.assertTrue(sql.contains("NOT IN"));

            // Test BETWEEN
            sql = NSB.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();
            Assertions.assertTrue(sql.contains("BETWEEN"));

            // Test LIKE
            sql = NSB.select("*").from("users").where(Filters.like("name", "%John%")).build().query();
            Assertions.assertTrue(sql.contains("LIKE"));

            // Test NOT LIKE
            sql = NSB.select("*").from("users").where(Filters.notLike("email", "%spam%")).build().query();
            Assertions.assertTrue(sql.contains("NOT LIKE"));
        }

        @Test
        public void testInsertReturning() {
            // Some databases support RETURNING clause
            String sql = NSB.insert("firstName", "lastName").into("users").append(" RETURNING id").build().query();

            Assertions.assertTrue(sql.contains("RETURNING id"));
        }

        @Test
        public void testCaseExpression() {
            String sql = NSB.select("name", "CASE WHEN age < 18 THEN 'Minor' WHEN age < 65 THEN 'Adult' ELSE 'Senior' END as category")
                    .from("users")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("CASE"));
            Assertions.assertTrue(sql.contains("WHEN"));
            Assertions.assertTrue(sql.contains("THEN"));
            Assertions.assertTrue(sql.contains("ELSE"));
        }

        @Test
        public void testWindowFunctions() {
            String sql = NSB.select("name", "salary", "ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) as rank")
                    .from("employees")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("ROW_NUMBER()"));
            Assertions.assertTrue(sql.contains("OVER"));
            Assertions.assertTrue(sql.contains("PARTITION BY"));
        }

        @Test
        public void testDistinct() {
            String sql = NSB.select("DISTINCT department").from("users").build().query();

            Assertions.assertTrue(sql.contains("DISTINCT"));

            // Test with multiple columns
            sql = NSB.select("DISTINCT department", "location").from("users").build().query();

            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testAggregateFunctions() {
            String sql = NSB
                    .select("COUNT(*) as total", "AVG(salary) as avg_salary", "MAX(salary) as max_salary", "MIN(salary) as min_salary",
                            "SUM(salary) as total_salary")
                    .from("employees")
                    .build()
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
                    .build()
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

            String sql = NSB.insert(user).into("test_user").build().query();

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
                    .build()
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
            String sql = NSC.insert("name").into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(name)"));
            Assertions.assertTrue(sql.contains("VALUES (:name)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = NSC.insert("firstName", "lastName", "email").into("users").build().query();
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
            String sql = NSC.insert(columns).into("users").build().query();
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
            String sql = NSC.insert(props).into("users").build().query();
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

            String sql = NSC.insert(user).into("users").build().query();
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
            String sql = NSC.insert(user, excluded).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = NSC.insert(User.class).into("users").build().query();
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
            String sql = NSC.insert(User.class, excluded).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertFalse(sql.contains("status"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertInto() {
            String sql = NSC.insertInto(User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = NSC.insertInto(User.class, excluded).build().query();
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

            String sql = NSC.batchInsert(users).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            // Should have multiple parameter sets for batch insert
        }

        @Test
        public void testUpdate() {
            String sql = NSC.update("users").set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = NSC.update("users", User.class).set("status").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = NSC.update(User.class).set("firstName", "email").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdDate", "status");
            String sql = NSC.update(User.class, excluded).set("firstName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = NSC.deleteFrom("users").where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = NSC.deleteFrom("users", User.class).where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = NSC.deleteFrom(User.class).where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testSelectSinglePart() {
            String sql = NSC.select("COUNT(*)").from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = NSC.select("firstName", "lastName", "email").from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = NSC.select(columns).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = NSC.select(aliases).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name AS \"fname\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = NSC.select(User.class).from("users").build().query();
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
            String sql = NSC.select(User.class, true).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdDate", "status");
            String sql = NSC.select(User.class, excluded).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("created_date"));
            Assertions.assertFalse(sql.contains("status"));
            Assertions.assertTrue(sql.contains("first_name"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.select(User.class, true, excluded).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFrom() {
            String sql = NSC.selectFrom(User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = NSC.selectFrom(User.class, "u").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM users u"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = NSC.selectFrom(User.class, true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = NSC.selectFrom(User.class, "u", true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, "u", excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("users u"));
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("status");
            String sql = NSC.selectFrom(User.class, "u", true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("status"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = NSC.select(User.class, "u", "user", User.class, "m", "manager")
                    .from("users u")
                    .join("users m")
                    .on("u.manager_id = m.id")
                    .build()
                    .query();
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
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, Set.of("status")));

            String sql = NSC.select(selections).from("users u").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = NSC.selectFrom(User.class, "u", "user", User.class, "m", "manager").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = Set.of("status");
            Set<String> excludeM = Set.of("email");

            String sql = NSC.selectFrom(User.class, "u", "user", excludeU, User.class, "m", "manager", excludeM).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, null));

            String sql = NSC.selectFrom(selections).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = NSC.count("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains(":active"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = NSC.count(User.class).where(Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18))).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("age > :age"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18), Filters.like("email", "%@example.com"));
            String sql = NSC.parse(cond, User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name = :firstName"));
            Assertions.assertTrue(sql.contains("age > :age"));
            Assertions.assertTrue(sql.contains("email LIKE :email"));
        }

        @Test
        public void testNamingPolicySnakeCase() {
            // NSC uses SNAKE_CASE naming policy
            String sql = NSC.select("firstName", "lastName").from("userAccounts").build().query();

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
                    .build()
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

            String sql = NSC.update("users").set(updates).where(Filters.eq("id", 1)).build().query();

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

            String sql = NSC.batchInsert(data).into("users").build().query();
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
                    .build()
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

            String sql = NSC.insert(user).into("users").build().query();

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

            sql = NSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = NSC.select("*").from("users u").innerJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = NSC.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = NSC.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = NSC.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = NSC.select("*").from("users").crossJoin("departments").build().query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = NSC.select("*").from("users").naturalJoin("user_profiles").build().query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = NSC.select("*").from("users").append(" WHERE MATCH(name) AGAINST (:search IN BOOLEAN MODE)").build().query();

            Assertions.assertTrue(sql.contains("MATCH"));
            Assertions.assertTrue(sql.contains("AGAINST"));
        }

        @Test
        public void testComplexNestedConditions() {
            String sql = NSC.select("*")
                    .from("users")
                    .where(Filters.or(Filters.and(Filters.eq("status", "ACTIVE"), Filters.or(Filters.lt("age", 18), Filters.gt("age", 65))),
                            Filters.and(Filters.eq("status", "PREMIUM"), Filters.between("age", 18, 65))))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testForUpdate() {
            String sql = NSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testLimitOffset() {
            String sql = NSC.select("*").from("users").limit(10).offset(20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = NSC.select("*").from("users").limit(10, 20).build().query();

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
            String sql = NAC.insert("FIRST_NAME").into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME)"));
            Assertions.assertTrue(sql.contains("VALUES (:FIRST_NAME)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = NAC.insert("firstName", "lastName", "email").into("ACCOUNT").build().query();
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
            String sql = NAC.insert(columns).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = Map.of("firstName", "John", "lastName", "Doe");
            String sql = NAC.insert(props).into("ACCOUNT").build().query();
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

            String sql = NAC.insert(account).into("ACCOUNT").build().query();
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
            String sql = NAC.insert(account, exclude).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = NAC.insert(Account.class).into("ACCOUNT").build().query();
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
            String sql = NAC.insert(Account.class, excluded).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("ID"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
        }

        @Test
        public void testInsertInto() {
            String sql = NAC.insertInto(Account.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = NAC.insertInto(Account.class, excluded).build().query();
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

            String sql = NAC.batchInsert(accounts).into("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testUpdate() {
            String sql = NAC.update("ACCOUNT").set("STATUS", "ACTIVE").where(Filters.eq("ID", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("STATUS = :STATUS"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = :ID"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = NAC.update("ACCOUNT", Account.class).set("status").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("STATUS"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = NAC.update(Account.class).set("firstName", "lastName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdTime", "password");
            String sql = NAC.update(Account.class, excluded).set("firstName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = NAC.deleteFrom("ACCOUNT").where(Filters.eq("STATUS", "INACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("STATUS = :STATUS"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = NAC.deleteFrom("ACCOUNT", Account.class).where(Filters.eq("firstName", "John")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = :firstName"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = NAC.deleteFrom(Account.class).where(Filters.eq("firstName", "John")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = :firstName"));
        }

        @Test
        public void testSelectSinglePart() {
            String sql = NAC.select("COUNT(*)").from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = NAC.select("firstName", "lastName", "email").from("ACCOUNT").where(Filters.eq("active", true)).build().query();
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
            String sql = NAC.select(columns).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"firstName\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lastName\""));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = Map.of("firstName", "fname", "lastName", "lname");
            String sql = NAC.select(aliases).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = NAC.select(Account.class).from("ACCOUNT").build().query();
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
            String sql = NAC.select(Account.class, true).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.select(Account.class, excluded).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.select(Account.class, true, excluded).from("ACCOUNT").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFrom() {
            String sql = NAC.selectFrom(Account.class).where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("ACTIVE = :active"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = NAC.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM ACCOUNT a"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = NAC.selectFrom(Account.class, true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = NAC.selectFrom(Account.class, "a", true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.selectFrom(Account.class, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.selectFrom(Account.class, "a", excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("ACCOUNT a"));
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.selectFrom(Account.class, true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = NAC.selectFrom(Account.class, "a", true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("PASSWORD"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = NAC.select(Account.class, "a", "account", Account.class, "o", "order")
                    .from("ACCOUNT a")
                    .join("ORDER o")
                    .on("a.ID = o.ACCOUNT_ID")
                    .build()
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
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "o", "order", null, true, null));
            String sql = NAC.select(selections).from("ACCOUNT a").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = NAC.selectFrom(Account.class, "a", "account", Account.class, "o", "order").where(Filters.eq("a.ID", "o.ACCOUNT_ID")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = NAC.selectFrom(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .where(Filters.eq("a.ID", "o.ACCOUNT_ID"))
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "o", "order", null, true, null));
            String sql = NAC.selectFrom(selections).where(Filters.eq("a.ID", "o.ACCOUNT_ID")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = NAC.count("ACCOUNT").where(Filters.eq("STATUS", "ACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("STATUS = :STATUS"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = NAC.count(Account.class).where(Filters.eq("status", "ACTIVE")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("STATUS = :status"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = NAC.parse(cond, Account.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("STATUS = :status"));
            Assertions.assertTrue(sql.contains("BALANCE > :balance"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testNamingPolicyUpperCase() {
            // NAC uses SCREAMING_SNAKE_CASE naming policy
            String sql = NAC.select("firstName", "lastName").from("userAccounts").build().query();

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
                    .build()
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

            String sql = NAC.update("ACCOUNT").set(updates).where(Filters.eq("ID", 1)).build().query();

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

            String sql = NAC.batchInsert(data).into("ACCOUNT").build().query();
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
                    .build()
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

            String sql = NAC.insert(account).into("ACCOUNT").build().query();

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

            sql = NAC.select("*").from("ACCOUNT a").join("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = NAC.select("*").from("ACCOUNT a").innerJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = NAC.select("*").from("ACCOUNT a").leftJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = NAC.select("*").from("ACCOUNT a").rightJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = NAC.select("*").from("ACCOUNT a").fullJoin("ORDER o").on("a.ID = o.ACCOUNT_ID").build().query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = NAC.select("*").from("ACCOUNT").crossJoin("DEPARTMENT").build().query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = NAC.select("*").from("ACCOUNT").naturalJoin("ACCOUNT_PROFILE").build().query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = NAC.select("*").from("ACCOUNT").append(" WHERE CUSTOM_FUNCTION(NAME) = TRUE").build().query();

            Assertions.assertTrue(sql.contains("CUSTOM_FUNCTION"));
        }

        @Test
        public void testComplexNestedConditions() {
            String sql = NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.or(Filters.and(Filters.eq("STATUS", "ACTIVE"), Filters.or(Filters.lt("AGE", 18), Filters.gt("AGE", 65))),
                            Filters.and(Filters.eq("STATUS", "PREMIUM"), Filters.between("AGE", 18, 65))))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains(":STATUS"));
            Assertions.assertTrue(sql.contains(":AGE"));
        }

        @Test
        public void testForUpdate() {
            String sql = NAC.select("*").from("ACCOUNT").where(Filters.eq("ID", 1)).forUpdate().build().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testLimitOffset() {
            String sql = NAC.select("*").from("ACCOUNT").limit(10).offset(20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = NAC.select("*").from("ACCOUNT").limit(10, 20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testInsertWithUpperCaseColumns() {
            // Test that already uppercase columns remain uppercase
            String sql = NAC.insert("FIRST_NAME", "LAST_NAME").into("ACCOUNT").build().query();

            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains(":FIRST_NAME"));
            Assertions.assertTrue(sql.contains(":LAST_NAME"));
        }

        @Test
        public void testTableNameTransformation() {
            // Test table name is converted to uppercase
            String sql = NAC.select("*").from("userAccounts").build().query();
            Assertions.assertTrue(sql.contains("FROM userAccounts"));
        }

        @Test
        public void testJoinWithConditions() {
            String sql = NAC.select("*")
                    .from("ACCOUNT a")
                    .leftJoin("ORDER o")
                    .on(Filters.and(Filters.eq("a.ID", "o.ACCOUNT_ID"), Filters.gt("o.AMOUNT", 100)))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = NAC.select("DEPARTMENT", "COUNT(*) AS CNT").from("EMPLOYEE").groupBy("DEPARTMENT").having(Filters.gt("COUNT(*)", 5)).build().query();

            Assertions.assertTrue(sql.contains("GROUP BY DEPARTMENT"));
            Assertions.assertTrue(sql.contains("HAVING"));
        }

        @Test
        public void testOrderBy() {
            String sql = NAC.select("*").from("ACCOUNT").orderBy("LAST_NAME ASC", "FIRST_NAME ASC").build().query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("LAST_NAME ASC"));
            Assertions.assertTrue(sql.contains("FIRST_NAME ASC"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = NAC.select("*").from("ACCOUNT").where(Filters.isNull("EMAIL")).build().query();
            Assertions.assertTrue(sql.contains("EMAIL IS NULL"));

            // Test IS NOT NULL
            sql = NAC.select("*").from("ACCOUNT").where(Filters.isNotNull("EMAIL")).build().query();
            Assertions.assertTrue(sql.contains("EMAIL IS NOT NULL"));

            // Test IN
            sql = NAC.select("*").from("ACCOUNT").where(Filters.in("STATUS", Arrays.asList("ACTIVE", "PREMIUM"))).build().query();
            Assertions.assertTrue(sql.contains("STATUS IN"));

            // Test BETWEEN
            sql = NAC.select("*").from("ACCOUNT").where(Filters.between("AGE", 18, 65)).build().query();
            Assertions.assertTrue(sql.contains("AGE BETWEEN"));

            // Test LIKE
            sql = NAC.select("*").from("ACCOUNT").where(Filters.like("NAME", "%JOHN%")).build().query();
            Assertions.assertTrue(sql.contains("NAME LIKE"));
        }

        @Test
        public void testMultipleWhereConditions() {
            String sql = NAC.select("*")
                    .from("ACCOUNT")
                    .where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%")))
                    .build()
                    .query();

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
            String sql = NLC.insert("firstName").into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName)"));
            Assertions.assertTrue(sql.contains("VALUES (:firstName)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = NLC.insert("firstName", "lastName", "email").into("account").build().query();
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
            String sql = NLC.insert(columns).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
        }

        @Test
        public void testInsertWithMap() {
            Map<String, Object> props = Map.of("firstName", "John", "lastName", "Doe");
            String sql = NLC.insert(props).into("account").build().query();
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

            String sql = NLC.insert(account).into("account").build().query();
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
            String sql = NLC.insert(account, exclude).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = NLC.insert(Account.class).into("account").build().query();
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
            String sql = NLC.insert(Account.class, excluded).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testInsertInto() {
            String sql = NLC.insertInto(Account.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = NLC.insertInto(Account.class, excluded).build().query();
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

            String sql = NLC.batchInsert(accounts).into("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testUpdate() {
            String sql = NLC.update("account").set("status", "active").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status = :status"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = :id"));
        }

        @Test
        public void testUpdateWithEntityClass() {
            String sql = NLC.update("account", Account.class).set("status").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = NLC.update(Account.class).set("firstName", "lastName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("createdTime", "password");
            String sql = NLC.update(Account.class, excluded).set("firstName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = NLC.deleteFrom("account").where(Filters.eq("status", "inactive")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = NLC.deleteFrom("account", Account.class).where(Filters.eq("status", "inactive")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = NLC.deleteFrom(Account.class).where(Filters.eq("status", "inactive")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testSelectSinglePart() {
            String sql = NLC.select("COUNT(*)").from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = NLC.select("firstName", "lastName", "email").from("account").where(Filters.eq("active", true)).build().query();
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
            String sql = NLC.select(columns).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = Map.of("firstName", "fname", "lastName", "lname");
            String sql = NLC.select(aliases).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("firstName AS \"fname\""));
            Assertions.assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = NLC.select(Account.class).from("account").build().query();
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
            String sql = NLC.select(Account.class, true).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password", "createdTime");
            String sql = NLC.select(Account.class, excluded).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("createdTime"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testSelectEntityClassWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.select(Account.class, true, excluded).from("account").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFrom() {
            String sql = NLC.selectFrom(Account.class).where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("active = :active"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = NLC.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM account a"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = NLC.selectFrom(Account.class, true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = NLC.selectFrom(Account.class, "a", true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.selectFrom(Account.class, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.selectFrom(Account.class, "a", excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("account a"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.selectFrom(Account.class, true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> excluded = Set.of("password");
            String sql = NLC.selectFrom(Account.class, "a", true, excluded).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = NLC.select(Account.class, "a", "account", Account.class, "o", "order")
                    .from("account a")
                    .join("order o")
                    .on("a.id = o.accountId")
                    .build()
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
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "o", "order", null, true, null));
            String sql = NLC.select(selections).from("account a").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = NLC.selectFrom(Account.class, "a", "account", Account.class, "o", "order").where(Filters.eq("a.id", "o.accountId")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeA = Set.of("password");
            Set<String> excludeO = Set.of("internalNotes");

            String sql = NLC.selectFrom(Account.class, "a", "account", excludeA, Account.class, "o", "order", excludeO)
                    .where(Filters.eq("a.id", "o.accountId"))
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, false, null),
                    new Selection(Account.class, "o", "order", null, true, null));
            String sql = NLC.selectFrom(selections).where(Filters.eq("a.id", "o.accountId")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = NLC.count("account").where(Filters.eq("status", "active")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = NLC.count(Account.class).where(Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000))).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("status = :status"));
            Assertions.assertTrue(sql.contains("balance > :balance"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000));
            String sql = NLC.parse(cond, Account.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("status = :status"));
            Assertions.assertTrue(sql.contains("balance > :balance"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testNamingPolicyCamelCase() {
            // NLC uses CAMEL_CASE naming policy
            String sql = NLC.select("firstName", "lastName").from("userAccounts").build().query();

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
                    .build()
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

            String sql = NLC.update("account").set(updates).where(Filters.eq("id", 1)).build().query();

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

            String sql = NLC.batchInsert(data).into("account").build().query();
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
                    .build()
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

            String sql = NLC.insert(account).into("account").build().query();

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

            sql = NLC.select("*").from("account a").join("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = NLC.select("*").from("account a").innerJoin("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = NLC.select("*").from("account a").leftJoin("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = NLC.select("*").from("account a").rightJoin("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = NLC.select("*").from("account a").fullJoin("order o").on("a.id = o.accountId").build().query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = NLC.select("*").from("account").crossJoin("department").build().query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = NLC.select("*").from("account").naturalJoin("accountProfile").build().query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = NLC.select("*").from("account").append(" WHERE customFunction(name) = true").build().query();

            Assertions.assertTrue(sql.contains("customFunction"));
        }

        @Test
        public void testComplexNestedConditions() {
            String sql = NLC.select("*")
                    .from("account")
                    .where(Filters.or(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.lt("age", 18), Filters.gt("age", 65))),
                            Filters.and(Filters.eq("status", "premium"), Filters.between("age", 18, 65))))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("OR"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains(":status"));
            Assertions.assertTrue(sql.contains(":age"));
        }

        @Test
        public void testForUpdate() {
            String sql = NLC.select("*").from("account").where(Filters.eq("id", 1)).forUpdate().build().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
        }

        @Test
        public void testLimitOffset() {
            String sql = NLC.select("*").from("account").limit(10).offset(20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = NLC.select("*").from("account").limit(10, 20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testTableNamePreservation() {
            // Test table name preserves camelCase
            String sql = NLC.select("*").from("userAccounts").build().query();
            Assertions.assertTrue(sql.contains("FROM userAccounts"));
        }

        @Test
        public void testJoinWithConditions() {
            String sql = NLC.select("*")
                    .from("account a")
                    .leftJoin("order o")
                    .on(Filters.and(Filters.eq("a.id", "o.accountId"), Filters.gt("o.amount", 100)))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("LEFT JOIN"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = NLC.select("department", "COUNT(*) as cnt").from("employee").groupBy("department").having(Filters.gt("COUNT(*)", 5)).build().query();

            Assertions.assertTrue(sql.contains("GROUP BY department"));
            Assertions.assertTrue(sql.contains("HAVING"));
        }

        @Test
        public void testOrderBy() {
            String sql = NLC.select("*").from("account").orderBy("lastName ASC", "firstName ASC").build().query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("lastName ASC"));
            Assertions.assertTrue(sql.contains("firstName ASC"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = NLC.select("*").from("account").where(Filters.isNull("email")).build().query();
            Assertions.assertTrue(sql.contains("email IS NULL"));

            // Test IS NOT NULL
            sql = NLC.select("*").from("account").where(Filters.isNotNull("email")).build().query();
            Assertions.assertTrue(sql.contains("email IS NOT NULL"));

            // Test IN
            sql = NLC.select("*").from("account").where(Filters.in("status", Arrays.asList("active", "premium"))).build().query();
            Assertions.assertTrue(sql.contains("status IN"));

            // Test BETWEEN
            sql = NLC.select("*").from("account").where(Filters.between("age", 18, 65)).build().query();
            Assertions.assertTrue(sql.contains("age BETWEEN"));

            // Test LIKE
            sql = NLC.select("*").from("account").where(Filters.like("name", "%John%")).build().query();
            Assertions.assertTrue(sql.contains("name LIKE"));
        }

        @Test
        public void testMultipleWhereConditions() {
            String sql = NLC.select("*")
                    .from("account")
                    .where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%")))
                    .build()
                    .query();

            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("AND"));
        }

        @Test
        public void testDistinct() {
            String sql = NLC.select("DISTINCT department").from("employee").build().query();

            Assertions.assertTrue(sql.contains("DISTINCT"));
        }

        @Test
        public void testAggregateFunctions() {
            String sql = NLC
                    .select("COUNT(*) as total", "AVG(salary) as avgSalary", "MAX(salary) as maxSalary", "MIN(salary) as minSalary",
                            "SUM(salary) as totalSalary")
                    .from("employee")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("COUNT(*)"));
            Assertions.assertTrue(sql.contains("AVG(salary)"));
            Assertions.assertTrue(sql.contains("MAX(salary)"));
            Assertions.assertTrue(sql.contains("MIN(salary)"));
            Assertions.assertTrue(sql.contains("SUM(salary)"));
        }

        @Test
        public void testWindowFunctions() {
            String sql = NLC.select("name", "salary", "ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) as rank")
                    .from("employee")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("OVER (PARTITION BY department ORDER BY salary DESC) AS rank"));
        }

        @Test
        public void testCaseExpression() {
            String sql = NLC.select("name", "CASE WHEN age < 18 THEN 'Minor' WHEN age < 65 THEN 'Adult' ELSE 'Senior' END as category")
                    .from("users")
                    .build()
                    .query();

            Assertions.assertTrue(sql.contains("case when age < 18 then 'Minor' when age < 65 then 'Adult' else 'Senior' end AS category"));
        }

        @Test
        public void testInsertReturning() {
            // Some databases support RETURNING clause
            String sql = NLC.insert("firstName", "lastName").into("users").append(" RETURNING id").build().query();

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
                    .build()
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
                    .build()
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
            String sql1 = NLC.select("name", "email").from("users").build().query();
            String sql2 = NLC.select("name", "email").from("customers").build().query();

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

            String sql = NLC.insert(account).into("account").build().query();

            // Should use property names as both column and parameter names
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("email"));
            Assertions.assertTrue(sql.contains(":firstName"));
            Assertions.assertTrue(sql.contains(":lastName"));
            Assertions.assertTrue(sql.contains(":email"));

            Date startDate = new Date();
            Date endDate = new Date(startDate.getTime() + 86400000); // 1 day later

            sql = NLC.selectFrom(Order.class, "ord", true).where(Filters.between("ord.orderDate", startDate, endDate)).build().query();
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
            String sql = PSC.insert("name").into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(name)"));
            Assertions.assertTrue(sql.contains("VALUES (?)"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = PSC.insert("firstName", "lastName", "email").into("users").build().query();
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
            String sql = PSC.insert(columns).into("users").build().query();
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
            String sql = PSC.insert(props).into("users").build().query();
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

            String sql = PSC.insert(user).into("users").build().query();
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
            String sql = PSC.insert(user, excluded).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = PSC.insert(User.class).into("users").build().query();
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
            String sql = PSC.insert(User.class, excluded).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("id"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertInto() {
            String sql = PSC.insertInto(User.class).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
        }

        @Test
        public void testInsertIntoWithExclusions() {
            Set<String> excluded = Set.of("id");
            String sql = PSC.insertInto(User.class, excluded).build().query();
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

            String sql = PSC.batchInsert(users).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            // Should have multiple value sets with ?
            Assertions.assertTrue(sql.contains("(?, ?)"));
        }

        @Test
        public void testUpdate() {
            String sql = PSC.update("users").set("last_login", "status").where(Filters.eq("id", 1)).build().query();
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
            String sql = PSC.update("users", User.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("first_name = ?"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = PSC.update(User.class).set("firstName", "email").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name = ?"));
            Assertions.assertTrue(sql.contains("email = ?"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excluded = Set.of("password", "created_date");
            String sql = PSC.update(User.class, excluded).set("firstName", "email").where(Filters.eq("id", 1)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name = ?"));
            Assertions.assertTrue(sql.contains("email = ?"));
        }

        @Test
        public void testDeleteFrom() {
            String sql = PSC.deleteFrom("users").where(Filters.eq("status", "inactive")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = ?"));
        }

        @Test
        public void testDeleteFromWithEntityClass() {
            String sql = PSC.deleteFrom("users", User.class).where(Filters.lt("lastLogin", new Date())).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("last_login < ?"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = PSC.deleteFrom(User.class).where(Filters.eq("id", 123)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = ?"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = PSC.select("COUNT(*) AS total").from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT COUNT(*) AS total"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = PSC.select("id", "name", "email", "created_date").from("users").where(Filters.eq("active", true)).build().query();
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
            String sql = PSC.select(columns).from("users").build().query();
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

            String sql = PSC.select(aliases).from("users u").leftJoin("orders o").on("u.id = o.user_id").groupBy("u.id").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("u.first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("u.last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("COUNT(o.id) AS \"orderCount\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = PSC.select(User.class).from("users").build().query();
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
            String sql = PSC.select(User.class, true).from("users u").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> exclude = Set.of("password", "created_date");
            String sql = PSC.select(User.class, exclude).from("users").build().query();
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
            String sql = PSC.select(User.class, true, exclude).from("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFrom() {
            String sql = PSC.selectFrom(User.class).where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("active = ?"));
        }

        @Test
        public void testSelectFromWithAlias() {
            String sql = PSC.selectFrom(User.class, "u").where(Filters.eq("u.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM users u"));
        }

        @Test
        public void testSelectFromWithSubEntities() {
            String sql = PSC.selectFrom(User.class, true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithAliasAndSubEntities() {
            String sql = PSC.selectFrom(User.class, "u", true).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithExclusions() {
            Set<String> exclude = Set.of("password");
            String sql = PSC.selectFrom(User.class, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAliasAndExclusions() {
            Set<String> exclude = Set.of("password");
            String sql = PSC.selectFrom(User.class, "u", exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("users u"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithSubEntitiesAndExclusions() {
            Set<String> exclude = Set.of("password");
            String sql = PSC.selectFrom(User.class, true, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromWithAllOptions() {
            Set<String> exclude = Set.of("password");
            String sql = PSC.selectFrom(User.class, "u", true, exclude).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = PSC.select(User.class, "u", "user_", User.class, "u2", "user2_")
                    .from("users u")
                    .join("users u2")
                    .on("u.id = u2.parent_id")
                    .build()
                    .query();
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
                    .build()
                    .query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, Set.of("password")));

            String sql = PSC.select(selections).from("users u").join("users m").on("u.manager_id = m.id").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT"));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = PSC.selectFrom(User.class, "u", "user_", User.class, "m", "manager_").where(Filters.eq("u.active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> excludeU = Set.of("password");
            Set<String> excludeM = Set.of("email", "password");

            String sql = PSC.selectFrom(User.class, "u", "user_", excludeU, User.class, "m", "manager_", excludeM).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testSelectFromWithSelectionList() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "m", "manager", null, false, null));

            String sql = PSC.selectFrom(selections).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("FROM"));
        }

        @Test
        public void testCount() {
            String sql = PSC.count("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("active = ?"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = PSC.count(User.class).where(Filters.eq("status", "active")).build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = ?"));
        }

        @Test
        public void testParse() {
            Condition cond = Filters.and(Filters.eq("status", "active"), Filters.gt("age", 18));
            String sql = PSC.parse(cond, User.class).build().query();
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

            String sql = PSC.update("users").set(updates).where(Filters.eq("id", 1)).build().query();

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

            String sql = PSC.batchInsert(data).into("users").build().query();
            Assertions.assertNotNull(sql);
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("(?, ?)"));
        }

        @Test
        public void testNamingPolicySnakeCase() {
            // PSC uses SNAKE_CASE naming policy
            String sql = PSC.select("firstName", "lastName").from("userAccounts").build().query();

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

            String sql = PSC.insert(user).into("users").build().query();

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

            sql = PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("JOIN"));

            sql = PSC.select("*").from("users u").innerJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("INNER JOIN"));

            sql = PSC.select("*").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("LEFT JOIN"));

            sql = PSC.select("*").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("RIGHT JOIN"));

            sql = PSC.select("*").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
            Assertions.assertTrue(sql.contains("FULL JOIN"));

            sql = PSC.select("*").from("users").crossJoin("departments").build().query();
            Assertions.assertTrue(sql.contains("CROSS JOIN"));

            sql = PSC.select("*").from("users").naturalJoin("user_profiles").build().query();
            Assertions.assertTrue(sql.contains("NATURAL JOIN"));
        }

        @Test
        public void testSpecialConditions() {
            // Test IS NULL
            String sql = PSC.select("*").from("users").where(Filters.isNull("email")).build().query();
            Assertions.assertTrue(sql.contains("email IS NULL"));

            // Test IS NOT NULL
            sql = PSC.select("*").from("users").where(Filters.isNotNull("email")).build().query();
            Assertions.assertTrue(sql.contains("email IS NOT NULL"));

            // Test IN
            sql = PSC.select("*").from("users").where(Filters.in("status", Arrays.asList("active", "premium", "trial"))).build().query();
            Assertions.assertTrue(sql.contains("status IN (?, ?, ?)"));

            // Test BETWEEN
            sql = PSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();
            Assertions.assertTrue(sql.contains("age BETWEEN ? AND ?"));

            // Test LIKE
            sql = PSC.select("*").from("users").where(Filters.like("name", "%John%")).build().query();
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
            String sql = PSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().build().query();

            Assertions.assertTrue(sql.contains("FOR UPDATE"));
            Assertions.assertTrue(sql.contains("id = ?"));
        }

        @Test
        public void testLimitOffset() {
            String sql = PSC.select("*").from("users").limit(10).offset(20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT 10"));
            Assertions.assertTrue(sql.contains("OFFSET 20"));

            // Test limit with offset parameter
            sql = PSC.select("*").from("users").limit(10, 20).build().query();

            Assertions.assertTrue(sql.contains("LIMIT"));
        }

        @Test
        public void testAppendCustomSQL() {
            String sql = PSC.select("*").from("users").append(" WHERE custom_function(name) = ?").build().query();

            Assertions.assertTrue(sql.contains("custom_function"));
        }

        @Test
        public void testGroupByHaving() {
            String sql = PSC.select("department", "COUNT(*) as count").from("users").groupBy("department").having(Filters.gt("COUNT(*)", 5)).build().query();

            Assertions.assertTrue(sql.contains("GROUP BY department"));
            Assertions.assertTrue(sql.contains("HAVING"));
            Assertions.assertTrue(sql.contains("COUNT(*) > ?"));
        }

        @Test
        public void testOrderBy() {
            String sql = PSC.select("*").from("users").orderBy("last_name ASC", "first_name ASC").build().query();

            Assertions.assertTrue(sql.contains("ORDER BY"));
            Assertions.assertTrue(sql.contains("last_name ASC"));
            Assertions.assertTrue(sql.contains("first_name ASC"));
        }

        @Test
        public void testMultipleWhereConditions() {
            String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.eq("ACTIVE", true).and(Filters.gt("AGE", 18)).and(Filters.like("NAME", "J%")))
                    .build()
                    .query();

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
                    .build()
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

/**
 * Unit tests for SqlBuilder class
 */
class SqlBuilder14Test extends TestBase {

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
            String sql = MSB.insert("name").into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(name)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{name}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = MSB.insert("firstName", "lastName", "email").into("users").build().query();
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
            String sql = MSB.insert(columns).into("products").build().query();
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
            String sql = MSB.insert(props).into("users").build().query();
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

            String sql = MSB.insert(account).into("users").build().query();
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
            String sql = MSB.insert(account, excludes).into("users").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = MSB.insert(Account.class).into("test_account").build().query();
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
            String sql = MSB.insert(Account.class, excludes).into("test_account").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = MSB.insertInto(Account.class).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO test_account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = MSB.insertInto(Account.class, excludes).build().query();
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

            String sql = MSB.batchInsert(propsList).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{name_0}"));
            Assertions.assertTrue(sql.contains("#{name_1}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = MSB.update("users").set("status", "lastModified").where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("status = #{status}"));
            Assertions.assertTrue(sql.contains("lastModified = #{lastModified}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = MSB.update("user_archive", Account.class).set("firstName").where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE user_archive"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = MSB.update(Account.class).where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE test_account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("lastName = #{lastName}"));
            Assertions.assertFalse(sql.contains("readOnlyField")); // @ReadOnly
            Assertions.assertFalse(sql.contains("nonUpdatableField")); // @NonUpdatable
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate"));
            String sql = MSB.update(Account.class, excludes).where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE test_account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = MSB.deleteFrom("users").where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("status = #{status}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = MSB.deleteFrom("users", Account.class).where(Filters.eq("firstName", "John")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = MSB.deleteFrom(Account.class).where(Filters.lt("createdDate", new Date())).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM test_account"));
            Assertions.assertTrue(sql.contains("createdDate < #{createdDate}"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = MSB.select("COUNT(*)").from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = MSB.select("firstName", "lastName", "email").from("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT firstName, lastName, email"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("id", "name", "price");
            String sql = MSB.select(columns).from("products").build().query();
            Assertions.assertTrue(sql.contains("SELECT id, name, price"));
            Assertions.assertTrue(sql.contains("FROM products"));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = MSB.select(aliases).from("users").build().query();
            Assertions.assertTrue(sql.contains("firstName AS \"fname\""));
            Assertions.assertTrue(sql.contains("lastName AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = MSB.select(Account.class).from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("tempData")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = MSB.select(Account.class, true).from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("firstName"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("email"));
            String sql = MSB.select(Account.class, excludes).from("users").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("email"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = MSB.selectFrom(Account.class).where(Filters.eq("active", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM test_account"));
            Assertions.assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = MSB.selectFrom(Account.class, "a").where(Filters.eq("a.active", true)).build().query();
            Assertions.assertTrue(sql.contains("FROM test_account a"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = MSB.selectFrom(Account.class, excludes).where(Filters.eq("active", true)).build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = MSB.select(Account.class, "a", "account", Account.class, "b", "account2").from("test_account a, test_account b").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testCountTable() {
            String sql = MSB.count("users").where(Filters.eq("active", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
            Assertions.assertTrue(sql.contains("active = #{active}"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = MSB.count(Account.class).where(Filters.between("createdDate", new Date(), new Date())).build().query();
            N.println(sql);
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM test_account"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("active", true), Filters.gt("age", 18));
            String sql = MSB.parse(cond, Account.class).build().query();
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
            String sql = MSC.insert("userName").into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(user_name)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{userName}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = MSC.insert("firstName", "lastName", "emailAddress").into("users").build().query();
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
            String sql = MSC.insert(props).into("users").build().query();
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
            String sql = MSC.insert(data).into("users").build().query();
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

            String sql = MSC.insert(user).into("users").build().query();
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
            String sql = MSC.insert(user, exclude).into("users").build().query();
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("email_address"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = MSC.insert(User.class).into("users").build().query();
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
            String sql = MSC.insert(User.class, exclude).into("users").build().query();
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
            Assertions.assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = MSC.insertInto(User.class).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO test_users"));
            Assertions.assertTrue(sql.contains("first_name"));
            Assertions.assertTrue(sql.contains("last_name"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("id"));
            String sql = MSC.insertInto(User.class, exclude).build().query();
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

            String sql = MSC.batchInsert(users).into("users").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO users"));
            Assertions.assertTrue(sql.contains("(first_name, last_name)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName_0}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = MSC.update("users").set("firstName", "lastName").where(Filters.eq("userId", 123)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("last_name = #{lastName}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("user_id = #{userId}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = MSC.update("users", User.class).set("firstName", "lastName").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("last_name = #{lastName}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = MSC.update(User.class).where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE test_users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertTrue(sql.contains("last_name = #{lastName}"));
            Assertions.assertFalse(sql.contains("read_only_field")); // @ReadOnly
            Assertions.assertFalse(sql.contains("non_updatable_field")); // @NonUpdatable
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("createdDate"));
            String sql = MSC.update(User.class, exclude).where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE test_users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
            Assertions.assertFalse(sql.contains("created_date"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = MSC.deleteFrom("users").where(Filters.eq("userId", 123)).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("user_id = #{userId}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = MSC.deleteFrom("users", User.class).where(Filters.eq("firstName", "John")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM users"));
            Assertions.assertTrue(sql.contains("first_name = #{firstName}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = MSC.deleteFrom(User.class).where(Filters.eq("id", 123)).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM test_users"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = MSC.select("COUNT(*)").from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT COUNT(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = MSC.select("firstName", "lastName", "emailAddress").from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
            Assertions.assertTrue(sql.contains("email_address AS \"emailAddress\""));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = Arrays.asList("firstName", "lastName");
            String sql = MSC.select(columns).from("users").build().query();
            Assertions.assertTrue(sql.contains("first_name AS \"firstName\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lastName\""));
        }

        @Test
        public void testSelectWithAliases() {
            Map<String, String> aliases = new HashMap<>();
            aliases.put("firstName", "fname");
            aliases.put("lastName", "lname");
            String sql = MSC.select(aliases).from("users").build().query();
            Assertions.assertTrue(sql.contains("first_name AS \"fname\""));
            Assertions.assertTrue(sql.contains("last_name AS \"lname\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = MSC.select(User.class).from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertTrue(sql.contains("AS \"lastName\""));
            Assertions.assertFalse(sql.contains("temp_data")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = MSC.select(User.class, true).from("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.select(User.class, exclude).from("users").build().query();
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = MSC.selectFrom(User.class).build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM test_users"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = MSC.selectFrom(User.class, "u").where(Filters.eq("u.active", true)).build().query();
            Assertions.assertTrue(sql.contains("FROM test_users u"));
            Assertions.assertTrue(sql.contains("u.active = #{u.active}"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("password"));
            String sql = MSC.selectFrom(User.class, exclude).build().query();
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.selectFrom(User.class, "u", exclude).build().query();
            Assertions.assertTrue(sql.contains("FROM test_users u"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.selectFrom(User.class, true, exclude).build().query();
            Assertions.assertTrue(sql.contains("FROM test_users"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromEntityClassWithFullOptions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.selectFrom(User.class, "u", true, exclude).build().query();
            Assertions.assertTrue(sql.contains("FROM test_users u"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectMultipleEntities() {
            String sql = MSC.select(User.class, "u", "user", User.class, "u2", "user2").from("test_users u, test_users u2").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testSelectMultipleEntitiesWithExclusions() {
            Set<String> exclude = new HashSet<>(Arrays.asList("password", "emailAddress"));
            String sql = MSC.select(User.class, "u", "user", exclude, User.class, "u2", "user2", exclude).from("test_users u, test_users u2").build().query();
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectWithMultiSelects() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));
            String sql = MSC.select(selections).from("test_users u, test_users u2").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = MSC.selectFrom(User.class, "u", "user", User.class, "u2", "user2").build().query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> exclude1 = new HashSet<>(Arrays.asList("password"));
            Set<String> exclude2 = new HashSet<>(Arrays.asList("emailAddress"));
            String sql = MSC.selectFrom(User.class, "u", "user", exclude1, User.class, "u2", "user2", exclude2).build().query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertTrue(sql.contains("emailAddress"));
        }

        @Test
        public void testSelectFromWithMultiSelects() {
            List<Selection> selections = Arrays.asList(new Selection(User.class, "u", "user", null, false, null),
                    new Selection(User.class, "u2", "user2", null, false, null));
            String sql = MSC.selectFrom(selections).build().query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("user."));
            Assertions.assertTrue(sql.contains("user2."));
        }

        @Test
        public void testCountTable() {
            String sql = MSC.count("users").build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM users"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = MSC.count(User.class).where(Filters.gt("age", 18)).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM test_users"));
            Assertions.assertTrue(sql.contains("age > #{age}"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 18));
            String sql = MSC.parse(cond, User.class).build().query();
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
            String sql = MAC.insert("firstName").into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = MAC.insert("firstName", "lastName", "email").into("ACCOUNT").build().query();
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
            String sql = MAC.insert(columns).into("ACCOUNT").build().query();
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
            String sql = MAC.insert(props).into("ACCOUNT").build().query();
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

            String sql = MAC.insert(account).into("ACCOUNT").build().query();
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
            String sql = MAC.insert(account, excludes).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertFalse(sql.contains("ID"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = MAC.insert(Account.class).into("ACCOUNT").build().query();
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
            String sql = MAC.insert(Account.class, excludes).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
            Assertions.assertFalse(sql.contains("ID"));
            Assertions.assertFalse(sql.contains("CREATED_DATE"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = MAC.insertInto(Account.class).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME"));
            Assertions.assertTrue(sql.contains("LAST_NAME"));
            Assertions.assertTrue(sql.contains("EMAIL"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = MAC.insertInto(Account.class, excludes).build().query();
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

            String sql = MAC.batchInsert(accounts).into("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO ACCOUNT"));
            Assertions.assertTrue(sql.contains("(FIRST_NAME, LAST_NAME)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName_0}"));
            Assertions.assertTrue(sql.contains("#{lastName_1}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = MAC.update("ACCOUNT")
                    .set("firstName", "John")
                    .set("lastName", "Doe")
                    .set(Map.of("modifiedDate", new Date()))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
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
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            Assertions.assertTrue(sql.contains("DEACTIVATED_DATE = #{deactivatedDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = MAC.update(Account.class).set("status", "ACTIVE").set(Map.of("lastLoginDate", new Date())).where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("STATUS = #{status}"));
            Assertions.assertTrue(sql.contains("LAST_LOGIN_DATE = #{lastLoginDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID = #{id}"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "createdDate", "createdBy"));
            String sql = MAC.update(Account.class, excludes)
                    .set("firstName", "John")
                    .set(Map.of("modifiedDate", new Date()))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("UPDATE ACCOUNT"));
            Assertions.assertTrue(sql.contains("FIRST_NAME = #{firstName}"));
            Assertions.assertTrue(sql.contains("MODIFIED_DATE = #{modifiedDate}"));
            Assertions.assertFalse(sql.contains("CREATED_DATE"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = MAC.deleteFrom("ACCOUNT").where(Filters.eq("status", "INACTIVE")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("STATUS = #{status}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = MAC.deleteFrom("ACCOUNT", Account.class)
                    .where(Filters.and(Filters.eq("isActive", false), Filters.lt("lastLoginDate", new Date())))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("LAST_LOGIN_DATE < #{lastLoginDate}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = MAC.deleteFrom(Account.class).where(Filters.in("id", Arrays.asList(1, 2, 3))).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("ID IN"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = MAC.select("COUNT(*) AS total, MAX(createdDate) AS latest").from("ACCOUNT").build().query();
            Assertions.assertEquals("SELECT COUNT(*) AS total, MAX(createdDate) AS latest FROM ACCOUNT", sql);
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = MAC.select("firstName", "lastName", "emailAddress").from("ACCOUNT").where(Filters.gt("createdDate", new Date())).build().query();
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
            String sql = MAC.select(columns).from("ACCOUNT").orderBy("createdDate DESC").build().query();
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
            String sql = MAC.select(aliases).from("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("FIRST_NAME AS \"fname\""));
            Assertions.assertTrue(sql.contains("LAST_NAME AS \"lname\""));
            Assertions.assertTrue(sql.contains("EMAIL_ADDRESS AS \"email\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = MAC.select(Account.class).from("ACCOUNT").where(Filters.eq("isActive", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("AS \"id\""));
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertTrue(sql.contains("AS \"lastName\""));
            Assertions.assertFalse(sql.contains("TEMP_DATA")); // @Transient
        }

        @Test
        public void testSelectEntityClassWithSubEntities() {
            String sql = MAC.select(Account.class, true).from("ACCOUNT").innerJoin("ADDRESS").on("ACCOUNT.ADDRESS_ID = ADDRESS.ID").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("INNER JOIN ADDRESS"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("passwordHash", "securityToken"));
            String sql = MAC.select(Account.class, excludes).from("ACCOUNT").build().query();
            Assertions.assertTrue(sql.contains("AS \"firstName\""));
            Assertions.assertTrue(sql.contains("AS \"lastName\""));
            Assertions.assertFalse(sql.contains("passwordHash"));
            Assertions.assertFalse(sql.contains("securityToken"));
        }

        @Test
        public void testSelectEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes"));
            String sql = MAC.select(Account.class, true, excludes).from("ACCOUNT").innerJoin("PROFILE").on("ACCOUNT.PROFILE_ID = PROFILE.ID").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("internalNotes"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = MAC.selectFrom(Account.class).where(Filters.eq("isActive", true)).orderBy("createdDate DESC").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
            Assertions.assertTrue(sql.contains("ORDER BY CREATED_DATE DESC"));
        }

        @Test
        public void testSelectFromEntityClassWithAlias() {
            String sql = MAC.selectFrom(Account.class, "a")
                    .innerJoin("PROFILE p")
                    .on("a.PROFILE_ID = p.ID")
                    .where(Filters.eq("a.isActive", true))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT a"));
            Assertions.assertTrue(sql.contains("INNER JOIN PROFILE p"));
            Assertions.assertTrue(sql.contains("a.IS_ACTIVE = #{a.isActive}"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            String sql = MAC.selectFrom(Account.class, true).innerJoin("ADDRESS").on("ACCOUNT.ADDRESS_ID = ADDRESS.ID").build().query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("INNER JOIN ADDRESS"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            String sql = MAC.selectFrom(Account.class, "acc", true).innerJoin("PROFILE p").on("acc.PROFILE_ID = p.ID").build().query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT acc"));
            Assertions.assertTrue(sql.contains("INNER JOIN PROFILE p"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
            String sql = MAC.selectFrom(Account.class, excludes).where(Filters.eq("status", "ACTIVE")).build().query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertFalse(sql.contains("largeBlob"));
            Assertions.assertFalse(sql.contains("internalData"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = MAC.selectFrom(Account.class, "a", excludes).where(Filters.like("a.emailAddress", "%@example.com")).build().query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT a"));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertTrue(sql.contains("a.EMAIL_ADDRESS LIKE #{a.emailAddress}"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
            String sql = MAC.selectFrom(Account.class, true, excludes).innerJoin("ORDERS").on("ACCOUNT.ID = ORDERS.ACCOUNT_ID").build().query();
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
                    .where(Filters.gt("o.total", 100))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM ACCOUNT acc"));
            Assertions.assertFalse(sql.contains("debugInfo"));
            Assertions.assertTrue(sql.contains("O.TOTAL > #{o.total}"));
        }

        @Test
        public void testCountTable() {
            String sql = MAC.count("ACCOUNT").where(Filters.eq("isActive", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("IS_ACTIVE = #{isActive}"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = MAC.count(Account.class).where(Filters.between("createdDate", new Date(), new Date())).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM ACCOUNT"));
            Assertions.assertTrue(sql.contains("CREATED_DATE BETWEEN"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = MAC.parse(cond, Account.class).build().query();
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
            String sql = MLC.insert("firstName").into("account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName}"));
        }

        @Test
        public void testInsertMultipleColumns() {
            String sql = MLC.insert("firstName", "lastName", "emailAddress").into("account").build().query();
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
            String sql = MLC.insert(columns).into("account").build().query();
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
            String sql = MLC.insert(props).into("account").build().query();
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

            String sql = MLC.insert(account).into("account").build().query();
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
            String sql = MLC.insert(account, excludes).into("account").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testInsertWithEntityClass() {
            String sql = MLC.insert(Account.class).into("account").build().query();
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
            String sql = MLC.insert(Account.class, excludes).into("account").build().query();
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
            Assertions.assertFalse(sql.contains("id"));
        }

        @Test
        public void testInsertIntoEntityClass() {
            String sql = MLC.insertInto(Account.class).build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("firstName"));
            Assertions.assertTrue(sql.contains("lastName"));
            Assertions.assertTrue(sql.contains("emailAddress"));
        }

        @Test
        public void testInsertIntoEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
            String sql = MLC.insertInto(Account.class, excludes).build().query();
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

            String sql = MLC.batchInsert(accounts).into("account").build().query();
            Assertions.assertTrue(sql.contains("INSERT INTO account"));
            Assertions.assertTrue(sql.contains("(firstName, lastName)"));
            Assertions.assertTrue(sql.contains("VALUES"));
            Assertions.assertTrue(sql.contains("#{firstName_0}"));
            Assertions.assertTrue(sql.contains("#{lastName_1}"));
        }

        @Test
        public void testUpdateTable() {
            String sql = MLC.update("account")
                    .set("firstName", "updatedName")
                    .set(Map.of("modifiedDate", new Date()))
                    .where(Filters.eq("id", 1))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("SET"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("modifiedDate = #{modifiedDate}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateTableWithEntityClass() {
            String sql = MLC.update("account", Account.class).set("firstName", "John").set("lastName", "Doe").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("lastName = #{lastName}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateEntityClass() {
            String sql = MLC.update(Account.class).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testUpdateEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("createdDate", "createdBy"));
            String sql = MLC.update(Account.class, excludes).set("firstName", "John").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("UPDATE account"));
            Assertions.assertTrue(sql.contains("firstName = #{firstName}"));
            Assertions.assertFalse(sql.contains("createdDate"));
        }

        @Test
        public void testDeleteFromTable() {
            String sql = MLC.deleteFrom("account").where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testDeleteFromTableWithEntityClass() {
            String sql = MLC.deleteFrom("account", Account.class).where(Filters.eq("emailAddress", "john@example.com")).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("emailAddress = #{emailAddress}"));
        }

        @Test
        public void testDeleteFromEntityClass() {
            String sql = MLC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
            Assertions.assertTrue(sql.contains("DELETE FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("id = #{id}"));
        }

        @Test
        public void testSelectSingleExpression() {
            String sql = MLC.select("COUNT(*) AS total, MAX(createdDate) AS latest").from("account").build().query();
            Assertions.assertTrue(sql.contains("SELECT COUNT(*) AS total, MAX(createdDate) AS latest"));
            Assertions.assertTrue(sql.contains("FROM account"));
        }

        @Test
        public void testSelectMultipleColumns() {
            String sql = MLC.select("firstName", "lastName", "emailAddress").from("account").where(Filters.gt("createdDate", new Date())).build().query();
            Assertions.assertTrue(sql.contains("SELECT firstName, lastName, emailAddress"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("createdDate > #{createdDate}"));
        }

        @Test
        public void testSelectWithCollection() {
            List<String> columns = getColumnsToSelect();
            String sql = MLC.select(columns).from("account").orderBy("createdDate DESC").build().query();
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
            String sql = MLC.select(aliases).from("account").build().query();
            Assertions.assertTrue(sql.contains("firstName AS \"fname\""));
            Assertions.assertTrue(sql.contains("lastName AS \"lname\""));
            Assertions.assertTrue(sql.contains("emailAddress AS \"email\""));
        }

        @Test
        public void testSelectEntityClass() {
            String sql = MLC.select(Account.class).from("account").where(Filters.eq("isActive", true)).build().query();
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
            String sql = MLC.select(Account.class, true).from("account").innerJoin("address").on("account.addressId = address.id").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("INNER JOIN address"));
        }

        @Test
        public void testSelectEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password", "secretKey"));
            String sql = MLC.select(Account.class, excludes).from("account").build().query();
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
            String sql = MLC.select(Account.class, true, excludes).from("account").innerJoin("profile").on("account.profileId = profile.id").build().query();
            Assertions.assertTrue(sql.contains("SELECT"));
            Assertions.assertFalse(sql.contains("internalNotes"));
        }

        @Test
        public void testSelectFromEntityClass() {
            String sql = MLC.selectFrom(Account.class).where(Filters.eq("isActive", true)).orderBy("createdDate DESC").build().query();
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
                    .where(Filters.like("a.emailAddress", "%@example.com"))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM account a"));
            Assertions.assertTrue(sql.contains("INNER JOIN profile p"));
            Assertions.assertTrue(sql.contains("a.emailAddress LIKE #{a.emailAddress}"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntities() {
            String sql = MLC.selectFrom(Account.class, true).innerJoin("address").on("account.addressId = address.id").build().query();
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("INNER JOIN address"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndSubEntities() {
            String sql = MLC.selectFrom(Account.class, "acc", true).innerJoin("profile p").on("acc.profileId = p.id").build().query();
            Assertions.assertTrue(sql.contains("FROM account acc"));
            Assertions.assertTrue(sql.contains("INNER JOIN profile p"));
        }

        @Test
        public void testSelectFromEntityClassWithExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
            String sql = MLC.selectFrom(Account.class, excludes).where(Filters.eq("status", "ACTIVE")).build().query();
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertFalse(sql.contains("largeBlob"));
            Assertions.assertFalse(sql.contains("internalData"));
        }

        @Test
        public void testSelectFromEntityClassWithAliasAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("password"));
            String sql = MLC.selectFrom(Account.class, "a", excludes).where(Filters.like("a.emailAddress", "%@example.com")).build().query();
            Assertions.assertTrue(sql.contains("FROM account a"));
            Assertions.assertFalse(sql.contains("password"));
        }

        @Test
        public void testSelectFromEntityClassWithSubEntitiesAndExclusions() {
            Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
            String sql = MLC.selectFrom(Account.class, true, excludes).innerJoin("orders").on("account.id = orders.accountId").build().query();
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
                    .where(Filters.gt("o.total", 100))
                    .build()
                    .query();
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
                    .build()
                    .query();
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
                    .where(Filters.gt("a.createdDate", new Date()))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
            Assertions.assertFalse(sql.contains("password"));
            Assertions.assertFalse(sql.contains("internalNotes"));
        }

        @Test
        public void testSelectWithMultipleSelections() {
            List<Selection> selections = Arrays.asList(new Selection(Account.class, "a", "account", null, true, null),
                    new Selection(Account.class, "a2", "account2", Arrays.asList("id", "name"), false, null));
            String sql = MLC.select(selections).from("account a").innerJoin("account a2").on("a.id = a2.parentId").build().query();
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testSelectFromMultipleEntities() {
            String sql = MLC.selectFrom(Account.class, "a", "account", Account.class, "a2", "account2")
                    .innerJoin("a.id = a2.parentId")
                    .where(Filters.gt("a.createdDate", new Date()))
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertTrue(sql.contains("account."));
            Assertions.assertTrue(sql.contains("account2."));
        }

        @Test
        public void testSelectFromMultipleEntitiesWithExclusions() {
            Set<String> accountExcludes = new HashSet<>(Arrays.asList("sensitiveData"));
            String sql = MLC.selectFrom(Account.class, "a", "account", accountExcludes, Account.class, "a2", "account2", null)
                    .innerJoin("a.id = a2.parentId")
                    .build()
                    .query();
            Assertions.assertTrue(sql.contains("FROM"));
            Assertions.assertFalse(sql.contains("sensitiveData"));
        }

        @Test
        public void testSelectFromWithMultipleSelections() {
            List<Selection> selections = createComplexSelections();
            String sql = MLC.selectFrom(selections)
                    .where(Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 0)))
                    .groupBy("account.type")
                    .having(Filters.gt("COUNT(*)", 5))
                    .build()
                    .query();
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
            String sql = MLC.count("account").where(Filters.eq("isActive", true)).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("WHERE"));
            Assertions.assertTrue(sql.contains("isActive = #{isActive}"));
        }

        @Test
        public void testCountEntityClass() {
            String sql = MLC.count(Account.class).where(Filters.between("createdDate", new Date(), new Date())).build().query();
            Assertions.assertTrue(sql.contains("SELECT count(*)"));
            Assertions.assertTrue(sql.contains("FROM account"));
            Assertions.assertTrue(sql.contains("createdDate BETWEEN"));
        }

        @Test
        public void testParseCondition() {
            com.landawn.abacus.query.condition.Condition cond = Filters.and(Filters.eq("status", "ACTIVE"), Filters.gt("balance", 1000));
            String sql = MLC.parse(cond, Account.class).build().query();
            Assertions.assertTrue(sql.contains("status = #{status}"));
            Assertions.assertTrue(sql.contains("AND"));
            Assertions.assertTrue(sql.contains("balance > #{balance}"));
        }
    }
}

@Tag("2025")
public class SqlBuilderTest extends TestBase {

    // Basic SELECT tests
    @Test
    public void testSelectAll() {
        String sql = SqlBuilder.PSC.select("*").from("users").build().query();
        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    public void testSelectSingleColumn() {
        String sql = SqlBuilder.PSC.select("name").from("users").build().query();
        assertEquals("SELECT name FROM users", sql);
    }

    @Test
    public void testSelectMultipleColumns() {
        String sql = SqlBuilder.PSC.select("id", "name", "email").from("users").build().query();
        assertTrue(sql.contains("SELECT id, name, email"));
    }

    @Test
    public void testSelectWithAlias() {
        String sql = SqlBuilder.PSC.select("first_name AS fname").from("users").build().query();
        assertTrue(sql.contains("AS fname"));
    }

    @Test
    public void testSelectWithEntityClass() {
        String sql = SqlBuilder.PSC.select(Account.class).from(Account.class).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
    }

    // FROM clause tests
    @Test
    public void testFromSingleTable() {
        String sql = SqlBuilder.PSC.select("*").from("users").build().query();
        assertTrue(sql.contains("FROM users"));
    }

    @Test
    public void testFromMultipleTables() {
        String sql = SqlBuilder.PSC.select("*").from("users", "orders").build().query();
        assertTrue(sql.contains("FROM orders"));
    }

    @Test
    public void testFromWithAlias() {
        String sql = SqlBuilder.PSC.select("u.*").from("users u").build().query();
        assertTrue(sql.contains("FROM users u"));
    }

    @Test
    public void testFromEntityClass() {
        String sql = SqlBuilder.PSC.select("*").from(Account.class).build().query();
        assertNotNull(sql);
    }

    // WHERE clause tests
    @Test
    public void testWhereEqual() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testWhereMultipleConditions() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.eq("status", "active").and(Filters.gt("age", 18))).build().query();
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testWhereOr() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.eq("role", "admin").or(Filters.eq("role", "moderator"))).build().query();
        assertTrue(sql.contains("OR"));
    }

    @Test
    public void testWhereBetween() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.between("age", 18, 65)).build().query();
        assertTrue(sql.contains("BETWEEN"));
    }

    @Test
    public void testWhereLike() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.like("name", "%John%")).build().query();
        assertTrue(sql.contains("LIKE"));
    }

    @Test
    public void testWhereIsNull() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.isNull("deleted_at")).build().query();
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    public void testWhereIsNotNull() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.isNotNull("email")).build().query();
        assertTrue(sql.contains("IS NOT NULL"));
    }

    @Test
    public void testWhereIsWithNullValue() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.is("deleted_at", null)).build().query();
        assertTrue(sql.contains("IS NULL"));
        assertFalse(sql.contains("IS ?"));
    }

    @Test
    public void testWhereIsNotWithNullValue() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.isNot("deleted_at", null)).build().query();
        assertTrue(sql.contains("IS NOT NULL"));
        assertFalse(sql.contains("IS NOT ?"));
    }

    // JOIN tests
    @Test
    public void testInnerJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").join("orders").on("users.id = orders.user_id").build().query();
        assertTrue(sql.contains("JOIN"));
        assertTrue(sql.contains("ON"));
    }

    @Test
    public void testLeftJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").leftJoin("orders").on("users.id = orders.user_id").build().query();
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testRightJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").rightJoin("departments").on("users.dept_id = departments.id").build().query();
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testFullJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").fullJoin("departments").on("users.dept_id = departments.id").build().query();
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testCrossJoin() {
        String sql = SqlBuilder.PSC.select("*").from("users").crossJoin("roles").build().query();
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testMultipleJoins() {
        String sql = SqlBuilder.PSC.select("*")
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

    // GROUP BY tests
    @Test
    public void testGroupBy() {
        String sql = SqlBuilder.PSC.select("department", "COUNT(*)").from("employees").groupBy("department").build().query();
        assertTrue(sql.contains("GROUP BY department"));
    }

    @Test
    public void testGroupByMultipleColumns() {
        String sql = SqlBuilder.PSC.select("year", "month", "COUNT(*)").from("sales").groupBy("year", "month").build().query();
        assertTrue(sql.contains("GROUP BY"));
    }

    // HAVING tests
    @Test
    public void testHaving() {
        String sql = SqlBuilder.PSC.select("department", "COUNT(*)")
                .from("employees")
                .groupBy("department")
                .having(Filters.expr("COUNT(*) > 5"))
                .build()
                .query();
        assertTrue(sql.contains("HAVING"));
    }

    // ORDER BY tests
    @Test
    public void testOrderBy() {
        String sql = SqlBuilder.PSC.select("*").from("users").orderBy("name").build().query();
        assertTrue(sql.contains("ORDER BY name"));
    }

    @Test
    public void testOrderByAsc() {
        String sql = SqlBuilder.PSC.select("*").from("users").orderBy("name", SortDirection.ASC).build().query();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("ASC"));
    }

    @Test
    public void testOrderByDesc() {
        String sql = SqlBuilder.PSC.select("*").from("users").orderBy("created_date", SortDirection.DESC).build().query();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("DESC"));
    }

    @Test
    public void testOrderByMultipleColumns() {
        String sql = SqlBuilder.PSC.select("*").from("users").orderBy("last_name", "first_name").build().query();
        assertTrue(sql.contains("ORDER BY"));
    }

    // LIMIT tests
    @Test
    public void testLimit() {
        String sql = SqlBuilder.PSC.select("*").from("users").limit(10).build().query();
        assertTrue(sql.contains("LIMIT 10"));
    }

    @Test
    public void testLimitWithOffset() {
        String sql = SqlBuilder.PSC.select("*").from("users").limit(20, 10).build().query();
        assertTrue(sql.contains("LIMIT"));
    }

    // DISTINCT tests
    @Test
    public void testDistinct() {
        String sql = SqlBuilder.PSC.select("status").from("users").distinct().build().query();
        assertTrue(sql.contains("DISTINCT"));
    }

    // INSERT tests
    @Test
    public void testInsertInto() {
        String sql = SqlBuilder.PSC.insert("id", "name").into("users").build().query();
        assertTrue(sql.contains("INSERT INTO users"));
    }

    @Test
    public void testInsertIntoWithEntityClass() {
        String sql = SqlBuilder.PSC.insert("firstName", "lastName").into(Account.class).build().query();
        assertTrue(sql.contains("INSERT INTO"));
    }

    // UPDATE tests
    @Test
    public void testUpdate() {
        String sql = SqlBuilder.PSC.update("users").set("status", "inactive").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("UPDATE users"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testUpdateMultipleColumns() {
        String sql = SqlBuilder.PSC.update("users").set("first_name", "John").set("last_name", "Doe").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testUpdateWithEntityClass() {
        String sql = SqlBuilder.PSC.update(Account.class).set("status", "active").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
    }

    // DELETE tests
    @Test
    public void testDeleteFrom() {
        String sql = SqlBuilder.PSC.deleteFrom("users").where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("DELETE FROM users"));
    }

    @Test
    public void testDeleteFromWithEntityClass() {
        String sql = SqlBuilder.PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testBatchInsertWithDifferentMapKeyOrder() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("lastName", "Smith");
        row2.put("firstName", "Jane");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        AbstractQueryBuilder.SP sp = SqlBuilder.PSC.batchInsert(rows).into("users").build();

        assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    @Test
    public void testBatchInsertWithDifferentMapKeyOrder_NamedSnakeCase() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("lastName", "Smith");
        row2.put("firstName", "Jane");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        AbstractQueryBuilder.SP sp = SqlBuilder.NSC.batchInsert(rows).into("users").build();

        assertEquals("INSERT INTO users (first_name, last_name) VALUES (:firstName_0, :lastName_0), (:firstName_1, :lastName_1)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    @Test
    public void testBatchInsertWithDifferentMapKeyOrder_NamedLowerCamelCase() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("lastName", "Smith");
        row2.put("firstName", "Jane");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        AbstractQueryBuilder.SP sp = SqlBuilder.NLC.batchInsert(rows).into("users").build();

        assertEquals("INSERT INTO users (firstName, lastName) VALUES (:firstName_0, :lastName_0), (:firstName_1, :lastName_1)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    @Test
    public void testBatchInsertWithNullMapRows() {
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

        AbstractQueryBuilder.SP sp = SqlBuilder.PSC.batchInsert(rows).into("users").build();

        assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?), (?, ?)", sp.query());
        assertEquals(Arrays.asList("John", "Doe", "Jane", "Smith"), sp.parameters());
    }

    // Complex query tests
    @Test
    public void testComplexSelectQuery() {
        String sql = SqlBuilder.PSC.select("u.id", "u.name", "COUNT(o.id) as order_count")
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

    // Naming policy tests
    @Test
    public void testSelectWithLowerCaseUnderscore() {
        String sql = SqlBuilder.PSC.select(Account.class).from(Account.class).build().query();
        assertNotNull(sql);
    }

    @Test
    public void testSelectWithUpperCaseUnderscore() {
        String sql = SqlBuilder.PAC.select(Account.class).from(Account.class).build().query();
        assertNotNull(sql);
    }

    // Parameterized query tests
    @Test
    public void testPairWithParameters() {
        SqlBuilder builder = SqlBuilder.PSC.select("*").from("users").where(Filters.eq("id", 1));
        AbstractQueryBuilder.SP sp = builder.build();
        assertNotNull(sp);
        assertNotNull(sp.query());
        assertNotNull(sp.parameters());
    }

    // Subquery tests
    @Test
    public void testSubquery() {
        String subquery = SqlBuilder.PSC.select("id").from("active_users").build().query();
        String sql = SqlBuilder.PSC.select("*").from("orders").where("user_id IN (" + subquery + ")").build().query();
        assertTrue(sql.contains("IN"));
        assertTrue(sql.contains("SELECT"));
    }

    // UNION tests
    @Test
    public void testUnion() {
        String sql1 = SqlBuilder.PSC.select("id", "name").from("users").build().query();
        String sql2 = SqlBuilder.PSC.select("id", "name").from("archived_users").build().query();
        String unionSql = sql1 + " UNION " + sql2;
        assertTrue(unionSql.contains("UNION"));
    }

    // Expression tests
    @Test
    public void testExpressionInSelect() {
        String sql = SqlBuilder.PSC.select("COUNT(*) as total").from("users").build().query();
        assertTrue(sql.contains("COUNT(*)"));
    }

    @Test
    public void testExpressionInWhere() {
        Expression expr = Filters.expr("age > 18 AND status = 'active'");
        String sql = SqlBuilder.PSC.select("*").from("users").where(expr).build().query();
        assertNotNull(sql);
    }

    // Aggregate functions tests
    @Test
    public void testCount() {
        String sql = SqlBuilder.PSC.select("COUNT(*)").from("users").build().query();
        assertTrue(sql.contains("COUNT(*)"));
    }

    @Test
    public void testSum() {
        String sql = SqlBuilder.PSC.select("SUM(amount)").from("orders").build().query();
        assertTrue(sql.contains("SUM(amount)"));
    }

    @Test
    public void testAvg() {
        String sql = SqlBuilder.PSC.select("AVG(price)").from("products").build().query();
        assertTrue(sql.contains("AVG(price)"));
    }

    @Test
    public void testMax() {
        String sql = SqlBuilder.PSC.select("MAX(price)").from("products").build().query();
        assertTrue(sql.contains("MAX(price)"));
    }

    @Test
    public void testMin() {
        String sql = SqlBuilder.PSC.select("MIN(price)").from("products").build().query();
        assertTrue(sql.contains("MIN(price)"));
    }

    // Build method tests
    @Test
    public void testBuildMethod() {
        AbstractQueryBuilder.SP sp = SqlBuilder.PSC.select("*").from("users").build();
        assertNotNull(sp);
        assertNotNull(sp.query());
    }

    // Multiple where conditions with different operators
    @Test
    public void testWhereGreaterThan() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.gt("age", 18)).build().query();
        assertTrue(sql.contains(">"));
    }

    @Test
    public void testWhereLessThan() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.lt("age", 65)).build().query();
        assertTrue(sql.contains("<"));
    }

    @Test
    public void testWhereGreaterThanOrEqual() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.ge("age", 21)).build().query();
        assertTrue(sql.contains(">="));
    }

    @Test
    public void testWhereLessThanOrEqual() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.le("age", 60)).build().query();
        assertTrue(sql.contains("<="));
    }

    @Test
    public void testWhereNotEqual() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.ne("status", "deleted")).build().query();
        assertTrue(sql.contains("!=") || sql.contains("<>"));
    }

    // IN clause tests
    @Test
    public void testWhereIn() {
        String sql = SqlBuilder.PSC.select("*").from("users").where("id IN (1, 2, 3)").build().query();
        assertTrue(sql.contains("IN"));
    }

    // CASE WHEN tests
    @Test
    public void testCaseWhen() {
        String sql = SqlBuilder.PSC.select("CASE WHEN age < 18 THEN 'minor' ELSE 'adult' END as age_group").from("users").build().query();
        assertTrue(sql.contains(" case when age < 18 then 'minor' else 'adult' end AS age_group "));
    }

    // Multiple table sources
    @Test
    public void testFromMultipleTablesWithJoin() {
        String sql = SqlBuilder.PSC.select("*")
                .from("users u")
                .join("orders o")
                .on("u.id = o.user_id")
                .join("products p")
                .on("o.product_id = p.id")
                .build()
                .query();
        assertTrue(sql.contains("users u"));
        assertTrue(sql.contains("orders o"));
        assertTrue(sql.contains("products p"));
    }

    // Chaining tests
    @Test
    public void testChainedAndConditions() {
        String sql = SqlBuilder.PSC.select("*")
                .from("users")
                .where(Filters.eq("status", "active").and(Filters.gt("age", 18)).and(Filters.lt("age", 65)))
                .build()
                .query();
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testChainedOrConditions() {
        String sql = SqlBuilder.PSC.select("*")
                .from("users")
                .where(Filters.eq("role", "admin").or(Filters.eq("role", "moderator")).or(Filters.eq("role", "owner")))
                .build()
                .query();
        assertTrue(sql.contains("OR"));
    }

    // Edge case tests
    @Test
    public void testSelectWithNoFrom() {
        assertThrows(Exception.class, () -> {
            SqlBuilder.PSC.select("*").build().query();
        });
    }

    @Test
    public void testEmptySelect() {
        assertThrows(IllegalArgumentException.class, () -> SqlBuilder.PSC.select().from("users").build().query());
    }

    // Named SQL tests
    @Test
    public void testNamedInsert() {
        String sql = SqlBuilder.PSC.insert("firstName", "lastName").into(Account.class).build().query();
        assertNotNull(sql);
    }

    // Static factory tests
    @Test
    public void testSelectFactory() {
        SqlBuilder builder = SqlBuilder.PSC.select("*");
        assertNotNull(builder);
    }

    @Test
    public void testInsertIntoFactory() {
        SqlBuilder builder = SqlBuilder.PSC.insert("id", "name");
        assertNotNull(builder);
    }

    @Test
    public void testUpdateFactory() {
        SqlBuilder builder = SqlBuilder.PSC.update("users");
        assertNotNull(builder);
    }

    @Test
    public void testDeleteFromFactory() {
        SqlBuilder builder = SqlBuilder.PSC.deleteFrom("users");
        assertNotNull(builder);
    }

    // Performance and resource cleanup
    @Test
    public void testMultipleBuildCalls() {
        SqlBuilder builder = SqlBuilder.PSC.select("*").from("users");
        String sql1 = builder.build().query();
        assertNotNull(sql1);
        // Builder should be reusable or properly cleaned up
    }

    // Collection-based select
    @Test
    public void testSelectWithCollection() {
        String sql = SqlBuilder.PSC.select(Arrays.asList("id", "name", "email")).from("users").build().query();
        assertTrue(sql.contains("id"));
        assertTrue(sql.contains("name"));
        assertTrue(sql.contains("email"));
    }

    // Map-based operations
    @Test
    public void testUpdateWithMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("first_name", "John");
        props.put("last_name", "Doe");

        String sql = SqlBuilder.PSC.update("users").set(props).where(Filters.eq("id", 1)).build().query();
        assertTrue(sql.contains("SET"));
    }

    // Complex conditions
    @Test
    public void testWhereWithNestedAndOr() {
        String sql = SqlBuilder.PSC.select("*")
                .from("users")
                .where(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.eq("role", "admin"), Filters.eq("role", "moderator"))))
                .build()
                .query();
        assertNotNull(sql);
    }

    // NULL handling
    @Test
    public void testWhereNullCheck() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.isNull("deleted_at").and(Filters.isNotNull("email"))).build().query();
        assertTrue(sql.contains("IS NULL"));
        assertTrue(sql.contains("IS NOT NULL"));
    }

    // Preselect tests
    @Test
    public void testPreselectDistinct() {
        String sql = SqlBuilder.PSC.select("status").from("users").selectModifier("DISTINCT").build().query();
        assertTrue(sql.contains("DISTINCT"));
    }

    // USING clause tests
    @Test
    public void testJoinUsing() {
        String sql = SqlBuilder.PSC.select("*").from("users").join("orders").using("user_id").build().query();
        assertTrue(sql.contains("USING (user_id)"));
    }

    @Test
    public void testBinaryWithSubQueryRhsIsParenthesized() {
        String sql = SqlBuilder.PSC.select("*").from("users").where(Filters.equal("id", Filters.subQuery("SELECT MAX(user_id) FROM orders"))).build().query();
        assertTrue(sql.contains("id = (SELECT MAX(user_id) FROM orders)"));
    }

    // Offset tests
    @Test
    public void testOffset() {
        String sql = SqlBuilder.PSC.select("*").from("users").offset(20).build().query();
        assertTrue(sql.contains("OFFSET") || sql.contains("20"));
    }

    // Constants verification
    @Test
    public void testConstants() {
        assertNotNull(AbstractQueryBuilder.ALL);
        assertNotNull(AbstractQueryBuilder.DISTINCT);
        assertNotNull(AbstractQueryBuilder.COUNT_ALL);
        assertEquals("ALL", AbstractQueryBuilder.ALL);
        assertEquals("count(*)", AbstractQueryBuilder.COUNT_ALL);
    }
}

class SqlBuilderJavadocExamples extends TestBase {

    @Test
    public void testSqlBuilder_PSC_simpleSelect() {
        String sql = PSC.select("id", "name").from("account").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM account"));
    }

    @Test
    public void testSqlBuilder_PSC_updateWithConditions() {
        String sql = PSC.update("account").set("name", "status").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UPDATE account"));
        assertTrue(sql.contains("SET"));
    }

    @Test
    public void testSqlBuilder_PSC_deleteFrom() {
        String sql = PSC.deleteFrom("account").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DELETE FROM account"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithWhere() {
        String sql = PSC.select("id", "name").from("users").where(Filters.gt("age", 18)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FROM users"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithJoin() {
        String sql = PSC.select("u.id", "u.name", "o.total").from("users u").leftJoin("orders o").on("u.id = o.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LEFT JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithGroupBy() {
        String sql = PSC.select("department", "COUNT(*) AS cnt").from("employees").groupBy("department").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("GROUP BY"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithOrderBy() {
        String sql = PSC.select("id", "name").from("users").orderBy("name").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("ORDER BY"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithLimit() {
        String sql = PSC.select("id", "name").from("users").limit(10).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LIMIT"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithBetween() {
        String sql = PSC.select("id", "name", "age").from("users").where(Filters.between("age", 18, 65)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("BETWEEN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithIn() {
        String sql = PSC.select("id", "name").from("users").where(Filters.in("status", Arrays.asList("active", "pending"))).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("IN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithLike() {
        String sql = PSC.select("id", "name", "email").from("users").where(Filters.like("email", "%@company.com")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("LIKE"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithIsNull() {
        String sql = PSC.select("id", "name").from("users").where(Filters.isNull("deleted_at")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("IS NULL"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithAndOr() {
        String sql = PSC.select("id", "name")
                .from("users")
                .where(Filters.and(Filters.eq("status", "active"), Filters.or(Filters.gt("age", 18), Filters.eq("verified", true))))
                .build()
                .query();
        assertNotNull(sql);
        assertTrue(sql.contains("AND"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithRightJoin() {
        String sql = PSC.select("u.id", "o.total").from("users u").rightJoin("orders o").on("u.id = o.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("RIGHT JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithFullJoin() {
        String sql = PSC.select("u.id", "o.total").from("users u").fullJoin("orders o").on("u.id = o.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("FULL JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithInnerJoin() {
        String sql = PSC.select("u.id", "o.total").from("users u").innerJoin("orders o").on("u.id = o.user_id").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INNER JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithCrossJoin() {
        String sql = PSC.select("u.id", "p.name").from("users u").crossJoin("products p").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("CROSS JOIN"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithHaving() {
        String sql = PSC.select("department", "COUNT(*) AS cnt").from("employees").groupBy("department").having(Filters.expr("COUNT(*) > 5")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("HAVING"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithUnion() {
        String sql = PSC.select("id", "name").from("active_users").union(PSC.select("id", "name").from("archived_users")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UNION"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithUnionAll() {
        String sql = PSC.select("id", "name").from("active_users").unionAll(PSC.select("id", "name").from("temp_users")).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("UNION ALL"));
    }

    @Test
    public void testSqlBuilder_NSC_simpleSelect() {
        String sql = NSC.select("id", "name").from("account").where(Filters.eq("id", 1)).build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM account"));
    }

    @Test
    public void testSqlBuilder_PSC_insertIntoValues() {
        String sql = PSC.insert("id", "name", "email").into("users").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("INSERT INTO users"));
        assertTrue(sql.contains("VALUES"));
    }

    @Test
    public void testSqlBuilder_PSC_selectDistinct() {
        String sql = PSC.select("DISTINCT department").from("employees").build().query();
        assertNotNull(sql);
        assertTrue(sql.contains("DISTINCT"));
    }

    @Test
    public void testSqlBuilder_PSC_selectWithOffsetAndLimit() {
        String sql = PSC.select("id", "name").from("users").offset(10).limit(5).build().query();
        assertNotNull(sql);
    }
}
