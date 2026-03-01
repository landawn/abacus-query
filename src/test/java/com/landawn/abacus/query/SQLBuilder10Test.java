package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.SQLBuilder.NAC;
import com.landawn.abacus.query.SQLBuilder.NLC;
import com.landawn.abacus.query.SQLBuilder.NSC;
import com.landawn.abacus.query.SQLBuilder.PAC;
import com.landawn.abacus.query.SQLBuilder.PLC;
import com.landawn.abacus.query.SQLBuilder.PSC;
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.Having;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Tuple.Tuple2;

public class SQLBuilder10Test extends TestBase {

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

        String sql = NSC.select("name").from("users").where(Filters.eq("id", 1)).sql();
        assertEquals("SELECT name FROM users WHERE id = ${id}", sql);

        // Create a named SQL to test the handler
        sql = NSC.update("account").set("firstName").where(Filters.eq("id", 1)).sql();

        assertTrue(sql.contains("${firstName}"));
        assertTrue(sql.contains("${id}"));

        // Reset to default
        AbstractQueryBuilder.resetHandlerForNamedParameter();

        sql = NSC.update("account").set("firstName").where(Filters.eq("id", 1)).sql();

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
        String sql = PSC.insert("firstName", "lastName").into("account").sql();

        assertEquals("INSERT INTO account (first_name, last_name) VALUES (?, ?)", sql);

        // Test with entity class
        sql = PSC.insert("firstName", "lastName").into(Account.class).sql();

        assertEquals("INSERT INTO test_account (first_name, last_name) VALUES (?, ?)", sql);
    }

    @Test
    public void testIntoWithEntityAndTableName() {
        String sql = PSC.insert("firstName", "lastName").into("custom_table", Account.class).sql();

        assertEquals("INSERT INTO custom_table (first_name, last_name) VALUES (?, ?)", sql);
    }

    //    @Test
    //    public void testIntoWithInvalidOperation() {
    //        SQLBuilder builder = PSC.select("*").from("account");
    //        assertThrows(RuntimeException.class, () -> builder.into("account"));
    //    }

    @Test
    public void testIntoWithoutColumns() {
        assertThrows(RuntimeException.class, () -> PSC.update("account").into("account"));
    }

    @Test
    public void testDistinct() {
        String sql = PSC.select("name").distinct().from("account").sql();

        assertEquals("SELECT DISTINCT name FROM account", sql);
    }

    @Test
    public void testPreselect() {
        String sql = PSC.select("*").selectModifier("TOP 10").from("account").sql();

        assertEquals("SELECT TOP 10 * FROM account", sql);

        // Test duplicate selectModifier
        SQLBuilder builder = PSC.select("*").selectModifier("TOP 10");
        assertThrows(IllegalStateException.class, () -> builder.selectModifier("DISTINCT"));
    }

    @Test
    public void testFrom() {
        // Single table
        String sql = PSC.select("*").from("users").sql();
        assertEquals("SELECT * FROM users", sql);

        // Multiple tables
        sql = PSC.select("*").from(Array.of("users", "orders")).sql();
        assertEquals("SELECT * FROM users, orders", sql);

        // Collection of tables
        sql = PSC.select("*").from(Arrays.asList("users", "orders", "products")).sql();
        assertEquals("SELECT * FROM users, orders, products", sql);

        // With alias
        sql = PSC.select("*").from("users u").sql();
        assertEquals("SELECT * FROM users u", sql);

        // With entity class
        sql = PSC.select("*").from(Account.class).sql();
        assertEquals("SELECT * FROM test_account", sql);

        // With entity class and alias
        sql = PSC.select("*").from(Account.class, "a").sql();
        assertEquals("SELECT * FROM test_account a", sql);
    }

    @Test
    public void testFromWithExpression() {
        String sql = PSC.select("*").from("(SELECT * FROM users) t").sql();
        assertEquals("SELECT * FROM (SELECT * FROM users) t", sql);

        sql = PSC.select("*").from("users u, orders o").sql();
        assertEquals("SELECT * FROM users u, orders o", sql);
    }

    @Test
    public void testFromWithoutSelect() {
        assertThrows(RuntimeException.class, () -> PSC.update("account").from("account"));
    }

    @Test
    public void testJoin() {
        String sql = PSC.select("*").from("users u").join("orders o ON u.id = o.user_id").sql();

        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);

        // With entity class
        sql = PSC.select("*").from(Account.class, "a").join(Order.class, "o").on("a.id = o.user_id").sql();

        assertEquals("SELECT * FROM test_account a JOIN user_order o ON a.id = o.user_id", sql);
    }

    @Test
    public void testInnerJoin() {
        String sql = PSC.select("*").from("users u").innerJoin("orders o ON u.id = o.user_id").sql();

        assertEquals("SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testLeftJoin() {
        String sql = PSC.select("*").from("users u").leftJoin("orders o ON u.id = o.user_id").sql();

        assertEquals("SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testRightJoin() {
        String sql = PSC.select("*").from("users u").rightJoin("orders o ON u.id = o.user_id").sql();

        assertEquals("SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testFullJoin() {
        String sql = PSC.select("*").from("users u").fullJoin("orders o ON u.id = o.user_id").sql();

        assertEquals("SELECT * FROM users u FULL JOIN orders o ON u.id = o.user_id", sql);
    }

    @Test
    public void testCrossJoin() {
        String sql = PSC.select("*").from("users").crossJoin("orders").sql();

        assertEquals("SELECT * FROM users CROSS JOIN orders", sql);
    }

    @Test
    public void testNaturalJoin() {
        String sql = PSC.select("*").from("users").naturalJoin("orders").sql();

        assertEquals("SELECT * FROM users NATURAL JOIN orders", sql);
    }

    @Test
    public void testOn() {
        String sql = PSC.select("*").from("users u").join("orders o").on("u.id = o.user_id").sql();

        assertEquals("SELECT * FROM users u JOIN orders o ON u.id = o.user_id", sql);

        // With condition
        sql = PSC.select("*").from("users u").join("orders o").on(Filters.eq("u.id", "o.user_id")).sql();

        assertTrue(sql.contains("ON"));
    }

    @Test
    public void testUsing() {
        String sql = PSC.select("*").from("users").join("orders").using("user_id").sql();

        assertEquals("SELECT * FROM users JOIN orders USING user_id", sql);
    }

    @Test
    public void testWhere() {
        String sql = PSC.select("*").from("users").where("age > 18").sql();

        assertEquals("SELECT * FROM users WHERE age > 18", sql);

        // With condition
        sql = PSC.select("*").from("users").where(Filters.gt("age", 18)).sql();

        assertEquals("SELECT * FROM users WHERE age > ?", sql);
    }

    @Test
    public void testGroupBy() {
        // Single column
        String sql = PSC.select("category", "COUNT(*)").from("products").groupBy("category").sql();

        assertEquals("SELECT category, COUNT(*) FROM products GROUP BY category", sql);

        // Multiple columns
        sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupBy("category", "brand").sql();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand", sql);

        // With direction
        sql = PSC.select("category", "COUNT(*)").from("products").groupBy("category", SortDirection.DESC).sql();

        assertEquals("SELECT category, COUNT(*) FROM products GROUP BY category DESC", sql);

        // Collection
        sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupBy(Arrays.asList("category", "brand")).sql();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand", sql);

        // Map with directions
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("category", SortDirection.ASC);
        orders.put("brand", SortDirection.DESC);

        sql = PSC.select("category", "brand", "COUNT(*)").from("products").groupBy(orders).sql();

        assertEquals("SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand DESC", sql);
    }

    @Test
    public void testHaving() {
        String sql = PSC.select("category", "COUNT(*) as count").from("products").groupBy("category").having("COUNT(*) > 10").sql();

        assertEquals("SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > 10", sql);

        // With condition
        sql = PSC.select("category", "COUNT(*) as count").from("products").groupBy("category").having(Filters.gt("COUNT(*)", 10)).sql();

        assertEquals("SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > ?", sql);
    }

    @Test
    public void testOrderBy() {
        // Single column
        String sql = PSC.select("*").from("users").orderBy("name").sql();

        assertEquals("SELECT * FROM users ORDER BY name", sql);

        // Multiple columns
        sql = PSC.select("*").from("users").orderBy("lastName", "firstName").sql();

        assertEquals("SELECT * FROM users ORDER BY last_name, first_name", sql);

        // With direction
        sql = PSC.select("*").from("users").orderBy("name", SortDirection.DESC).sql();

        assertEquals("SELECT * FROM users ORDER BY name DESC", sql);

        // Collection
        sql = PSC.select("*").from("users").orderBy(Arrays.asList("lastName", "firstName")).sql();

        assertEquals("SELECT * FROM users ORDER BY last_name, first_name", sql);

        // Map with directions
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("lastName", SortDirection.ASC);
        orders.put("firstName", SortDirection.DESC);

        sql = PSC.select("*").from("users").orderBy(orders).sql();

        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name DESC", sql);
    }

    @Test
    public void testOrderByAsc() {
        String sql = PSC.select("*").from("users").orderByAsc("name").sql();

        assertEquals("SELECT * FROM users ORDER BY name ASC", sql);

        sql = PSC.select("*").from("users").orderByAsc("lastName", "firstName").sql();

        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC", sql);

        sql = PSC.select("*").from("users").orderByAsc(Arrays.asList("lastName", "firstName")).sql();

        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC", sql);
    }

    @Test
    public void testOrderByDesc() {
        String sql = PSC.select("*").from("users").orderByDesc("createdDate").sql();

        assertEquals("SELECT * FROM users ORDER BY created_date DESC", sql);

        sql = PSC.select("*").from("users").orderByDesc("lastName", "firstName").sql();

        assertEquals("SELECT * FROM users ORDER BY last_name DESC, first_name DESC", sql);

        sql = PSC.select("*").from("users").orderByDesc(Arrays.asList("lastName", "firstName")).sql();

        assertEquals("SELECT * FROM users ORDER BY last_name DESC, first_name DESC", sql);
    }

    @Test
    public void testLimit() {
        String sql = PSC.select("*").from("users").limit(10).sql();

        assertEquals("SELECT * FROM users LIMIT 10", sql);

        // With offset
        sql = PSC.select("*").from("users").limit(10, 20).sql();

        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testOffset() {
        String sql = PSC.select("*").from("users").limit(10).offset(20).sql();

        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    public void testOffsetRows() {
        String sql = PSC.select("*").from("users").orderBy("id").offsetRows(20).fetchNextRows(10).sql();

        assertEquals("SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testFetchNextNRowsOnly() {
        String sql = PSC.select("*").from("users").orderBy("id").offsetRows(0).fetchNextRows(10).sql();

        assertEquals("SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void testFetchFirstNRowsOnly() {
        String sql = PSC.select("*").from("users").orderBy("id").fetchFirstRows(10).sql();

        assertEquals("SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY", sql);
    }

    @Test
    public void testAppend() {
        // With condition
        String sql = PSC.select("*").from("users").append(Filters.and(Filters.gt("age", 18), Filters.lt("age", 65))).sql();

        assertEquals("SELECT * FROM users WHERE (age > ?) AND (age < ?)", sql);

        // With string
        sql = PSC.select("*").from("users").append(" FOR UPDATE").sql();

        assertEquals("SELECT * FROM users FOR UPDATE", sql);
    }

    //    @Test
    //    public void testAppendIf() {
    //        // With condition - true
    //        String sql = PSC.select("*").from("users").appendIf(true, Filters.gt("age", 18)).sql();
    //
    //        assertEquals("SELECT * FROM users WHERE age > ?", sql);
    //
    //        // With condition - false
    //        sql = PSC.select("*").from("users").appendIf(false, Filters.gt("age", 18)).sql();
    //
    //        assertEquals("SELECT * FROM users", sql);
    //
    //        // With string
    //        sql = PSC.select("*").from("users").appendIf(true, " FOR UPDATE").sql();
    //
    //        assertEquals("SELECT * FROM users FOR UPDATE", sql);
    //
    //        // With consumer
    //        sql = PSC.select("*").from("users").appendIf(true, builder -> builder.where(Filters.gt("age", 18)).orderBy("name")).sql();
    //
    //        assertTrue(sql.contains("WHERE age > ?"));
    //        assertTrue(sql.contains("ORDER BY name"));
    //    }
    //
    //    @Test
    //    public void testAppendIfOrElse() {
    //        // With condition
    //        String sql = PSC.select("*").from("users").appendIfOrElse(true, Filters.eq("status", "active"), Filters.eq("status", "inactive")).sql();
    //
    //        assertEquals("SELECT * FROM users WHERE status = ?", sql);
    //
    //        sql = PSC.select("*").from("users").appendIfOrElse(false, Filters.eq("status", "active"), Filters.eq("status", "inactive")).sql();
    //
    //        assertEquals("SELECT * FROM users WHERE status = ?", sql);
    //
    //        // With string
    //        sql = PSC.select("*").from("users").appendIfOrElse(true, " ORDER BY name ASC", " ORDER BY name DESC").sql();
    //
    //        assertEquals("SELECT * FROM users ORDER BY name ASC", sql);
    //    }

    @Test
    public void testUnion() {
        SQLBuilder query1 = PSC.select("id", "name").from("users");
        SQLBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.union(query2).sql();
        assertEquals("SELECT id, name FROM users UNION SELECT id, name FROM customers", sql);

        // With string
        sql = PSC.select("id", "name").from("users").union("SELECT id, name FROM customers").sql();

        assertEquals("SELECT id, name FROM users UNION SELECT id, name FROM customers", sql);

        // Start new select
        sql = PSC.select("id", "name").from("users").union("id", "name").from("customers").sql();

        assertEquals("SELECT id, name FROM users UNION SELECT id, name FROM customers", sql);

        // With collection
        sql = PSC.select("id", "name").from("users").union(Arrays.asList("id", "name")).from("customers").sql();

        assertEquals("SELECT id, name FROM users UNION SELECT id, name FROM customers", sql);
    }

    @Test
    public void testUnionAll() {
        SQLBuilder query1 = PSC.select("id", "name").from("users");
        SQLBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.unionAll(query2).sql();
        assertEquals("SELECT id, name FROM users UNION ALL SELECT id, name FROM customers", sql);
    }

    @Test
    public void testIntersect() {
        SQLBuilder query1 = PSC.select("id", "name").from("users");
        SQLBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.intersect(query2).sql();
        assertEquals("SELECT id, name FROM users INTERSECT SELECT id, name FROM customers", sql);
    }

    @Test
    public void testExcept() {
        SQLBuilder query1 = PSC.select("id", "name").from("users");
        SQLBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.except(query2).sql();
        assertEquals("SELECT id, name FROM users EXCEPT SELECT id, name FROM customers", sql);
    }

    @Test
    public void testMinus() {
        SQLBuilder query1 = PSC.select("id", "name").from("users");
        SQLBuilder query2 = PSC.select("id", "name").from("customers");

        String sql = query1.minus(query2).sql();
        assertEquals("SELECT id, name FROM users MINUS SELECT id, name FROM customers", sql);
    }

    @Test
    public void testForUpdate() {
        String sql = PSC.select("*").from("users").where(Filters.eq("id", 1)).forUpdate().sql();

        assertEquals("SELECT * FROM users WHERE id = ? FOR UPDATE", sql);
    }

    @Test
    public void testSet() {
        // With expression
        String sql = PSC.update("users").set("name = 'John'").where(Filters.eq("id", 1)).sql();

        assertEquals("UPDATE users SET name = 'John' WHERE id = ?", sql);

        // With columns
        sql = PSC.update("users").set("firstName", "lastName", "email").where(Filters.eq("id", 1)).sql();

        assertEquals("UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?", sql);

        // With collection
        sql = PSC.update("users").set(Arrays.asList("firstName", "lastName")).where(Filters.eq("id", 1)).sql();

        assertEquals("UPDATE users SET first_name = ?, last_name = ? WHERE id = ?", sql);

        // With map
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("firstName", "John");
        values.put("lastName", "Doe");

        sql = PSC.update("users").set(values).where(Filters.eq("id", 1)).sql();

        assertEquals("UPDATE users SET first_name = ?, last_name = ? WHERE id = ?", sql);

        // With entity
        Account account = new Account();
        account.setFirstName("John");
        account.setLastName("Doe");

        sql = PSC.update("account").set(account).where(Filters.eq("id", 1)).sql();

        assertTrue(sql.contains("UPDATE account SET"));
        assertTrue(sql.contains("WHERE id = ?"));

        // With entity class
        sql = PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).sql();

        assertTrue(sql.contains("UPDATE account SET"));
        assertTrue(sql.contains("WHERE id = ?"));

        // With excluded properties
        Set<String> excluded = N.asSet("lastModifiedDate");
        sql = PSC.update("account").set(account, excluded).where(Filters.eq("id", 1)).sql();

        assertTrue(sql.contains("UPDATE account SET"));
        assertFalse(sql.contains("last_modified_date"));

        sql = PSC.update("account").set(Account.class, excluded).where(Filters.eq("id", 1)).sql();

        assertTrue(sql.contains("UPDATE account SET"));
        assertFalse(sql.contains("last_modified_date"));
    }

    @Test
    public void testSql() {
        String sql = PSC.select("id", "name").from("account").where(Filters.gt("age", 18)).sql();

        assertEquals("SELECT id, name FROM account WHERE age > ?", sql);

        // Test double call throws exception
        SQLBuilder builder = PSC.select("*").from("users");
        builder.sql();
        assertThrows(RuntimeException.class, () -> builder.sql());
    }

    @Test
    public void testParameters() {
        SQLBuilder builder = PSC.select("*").from("account").where(Filters.eq("name", "John").and(Filters.gt("age", 25)));

        SP sP = builder.build();
        List<Object> params = sP.parameters;

        assertEquals(2, params.size());
        assertEquals("John", params.get(0));
        assertEquals(25, params.get(1));
    }

    @Test
    public void testPair() {
        SQLBuilder.SP sP = PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).build();

        assertEquals("SELECT * FROM account WHERE status = ?", sP.query);
        assertEquals(1, sP.parameters.size());
        assertEquals("ACTIVE", sP.parameters.get(0));
    }

    @Test
    public void testApplyFunction() throws Exception {
        List<String> result = PSC.select("*")
                .from("account")
                .where(Filters.eq("status", "ACTIVE"))
                .apply(sp -> Arrays.asList(sp.query, sp.parameters.toString()));

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
        // This test just ensures println() doesn't throw exception
        PSC.select("*").from("account").where(Filters.between("age", 18, 65)).println();
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
                .sql();

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
    public void testSPEquals() {
        SQLBuilder.SP sp1 = new SQLBuilder.SP("SELECT * FROM users", Arrays.asList(1, 2));
        SQLBuilder.SP sp2 = new SQLBuilder.SP("SELECT * FROM users", Arrays.asList(1, 2));
        SQLBuilder.SP sp3 = new SQLBuilder.SP("SELECT * FROM accounts", Arrays.asList(1, 2));
        SQLBuilder.SP sp4 = new SQLBuilder.SP("SELECT * FROM users", Arrays.asList(1, 3));

        assertEquals(sp1, sp1);
        assertEquals(sp1, sp2);
        assertNotEquals(sp1, sp3);
        assertNotEquals(sp1, sp4);
        assertNotEquals(sp1, null);
        assertNotEquals(sp1, "not an SP");
    }

    @Test
    public void testSPHashCode() {
        SQLBuilder.SP sp1 = new SQLBuilder.SP("SELECT * FROM users", Arrays.asList(1, 2));
        SQLBuilder.SP sp2 = new SQLBuilder.SP("SELECT * FROM users", Arrays.asList(1, 2));

        assertEquals(sp1.hashCode(), sp2.hashCode());
    }

    @Test
    public void testSPToString() {
        SQLBuilder.SP sP = new SQLBuilder.SP("SELECT * FROM users", Arrays.asList(1, "test"));
        String str = sP.toString();

        assertTrue(str.contains("sql=SELECT * FROM users"));
        assertTrue(str.contains("parameters=[1, test]"));
    }

    // Test different naming policies
    @Test
    public void testNamingPolicies() {
        // Snake case (PSC)
        String sql = PSC.select("firstName", "lastName").from("userAccount").sql();
        assertTrue(sql.contains("first_name"));
        assertTrue(sql.contains("last_name"));
        assertTrue(sql.contains("userAccount"));

        // Upper case (PAC)
        sql = PAC.select("firstName", "lastName").from("userAccount").sql();
        assertTrue(sql.contains("FIRST_NAME"));
        assertTrue(sql.contains("LAST_NAME"));
        assertTrue(sql.contains("userAccount"));

        // Lower camel case (PLC)
        sql = PLC.select("first_name", "last_name").from("user_account").sql();
        assertTrue(sql.contains("firstName"));
        assertTrue(sql.contains("lastName"));
        assertTrue(sql.contains("user_account"));
    }

    // Test SQL policies
    @Test
    public void testSQLPolicies() {
        // Parameterized SQL
        String sql = PSC.update("account").set("name").where(Filters.eq("id", 1)).sql();
        assertTrue(sql.contains("name = ?"));
        assertTrue(sql.contains("id = ?"));

        // Named SQL
        sql = NSC.update("account").set("name").where(Filters.eq("id", 1)).sql();
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
                .sql();

        assertTrue(sql.contains("FROM users u"));
        assertTrue(sql.contains("LEFT JOIN orders o ON u.id = o.user_id"));
        assertTrue(sql.contains("LEFT JOIN products p ON o.product_id = p.id"));
        assertTrue(sql.contains("WHERE u.status = ?"));
    }

    @Test
    public void testJoinWithEntityClasses() {
        String sql = PSC.select("*").from(Account.class, "a").join(Order.class, "o").on(Filters.eq("a.id", "o.userId")).sql();

        assertTrue(sql.contains("FROM test_account a"));
        assertTrue(sql.contains("JOIN user_order o"));
    }

    @Test
    public void testConditionsWithAnd() {
        String sql = PSC.select("*").from("users").where(Filters.and(Filters.gt("age", 18), Filters.lt("age", 65), Filters.eq("status", "ACTIVE"))).sql();

        assertTrue(sql.contains("WHERE (age > ?) AND (age < ?) AND (status = ?)"));
    }

    @Test
    public void testConditionsWithOr() {
        String sql = PSC.select("*").from("users").where(Filters.or(Filters.eq("status", "ACTIVE"), Filters.eq("status", "PENDING"))).sql();

        assertTrue(sql.contains("WHERE (status = ?) OR (status = ?)"));
    }

    @Test
    public void testBetweenCondition() {
        String sql = PSC.select("*").from("users").where(Filters.between("age", 18, 65)).sql();

        assertEquals("SELECT * FROM users WHERE age BETWEEN ? AND ?", sql);
    }

    @Test
    public void testNotBetweenCondition() {
        String sql = PSC.select("*").from("users").where(Filters.notBetween("age", 18, 65)).sql();

        assertEquals("SELECT * FROM users WHERE age NOT BETWEEN ? AND ?", sql);
    }

    @Test
    public void testInCondition() {
        String sql = PSC.select("*").from("users").where(Filters.in("status", Arrays.asList("ACTIVE", "PENDING", "APPROVED"))).sql();

        assertEquals("SELECT * FROM users WHERE status IN (?, ?, ?)", sql);
    }

    @Test
    public void testNotInCondition() {
        String sql = PSC.select("*").from("users").where(Filters.notIn("status", Arrays.asList("DELETED", "BANNED"))).sql();

        assertEquals("SELECT * FROM users WHERE status NOT IN (?, ?)", sql);
    }

    @Test
    public void testIsNullCondition() {
        String sql = PSC.select("*").from("users").where(Filters.isNull("deletedDate")).sql();

        assertEquals("SELECT * FROM users WHERE deleted_date IS NULL", sql);
    }

    @Test
    public void testIsNotNullCondition() {
        String sql = PSC.select("*").from("users").where(Filters.isNotNull("email")).sql();

        assertEquals("SELECT * FROM users WHERE email IS NOT NULL", sql);
    }

    @Test
    public void testLikeCondition() {
        String sql = PSC.select("*").from("users").where(Filters.like("name", "%John%")).sql();

        assertEquals("SELECT * FROM users WHERE name LIKE ?", sql);
    }

    @Test
    public void testNotLikeCondition() {
        String sql = PSC.select("*").from("users").where(Filters.notLike("email", "%@temp.com")).sql();

        assertEquals("SELECT * FROM users WHERE email NOT LIKE ?", sql);
    }

    @Test
    public void testCriteriaCondition() {
        Criteria criteria = Filters.criteria().where(Filters.gt("age", 18)).groupBy("status").having(Filters.gt("COUNT(*)", 5)).orderBy("status").limit(10);

        String sql = PSC.select("status", "COUNT(*)").from("users").append(criteria).sql();

        assertTrue(sql.contains("WHERE age > ?"));
        assertTrue(sql.contains("GROUP BY status"));
        assertTrue(sql.contains("HAVING COUNT(*) > ?"));
        assertTrue(sql.contains("ORDER BY status"));
        assertTrue(sql.contains("LIMIT 10"));
    }

    @Test
    public void testExpressionCondition() {
        String sql = PSC.select("*").from("users").where(Filters.expr("age > 18 AND status = 'ACTIVE'")).sql();

        assertEquals("SELECT * FROM users WHERE age > 18 AND status = 'ACTIVE'", sql);
    }

    @Test
    public void testSelectWithAlias() {
        String sql = PSC.select("firstName AS name", "age").from("users").sql();

        assertTrue(sql.contains("first_name AS name"));
        assertTrue(sql.contains("age"));
    }

    @Test
    public void testSelectWithAliasMap() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("firstName", "name");
        aliases.put("lastName", "surname");

        String sql = PSC.select(aliases).from("users").sql();

        assertTrue(sql.contains("first_name AS \"name\""));
        assertTrue(sql.contains("last_name AS \"surname\""));
    }

    @Test
    public void testInsertWithMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("firstName", "John");
        values.put("lastName", "Doe");
        values.put("email", "john@example.com");

        String sql = PSC.insert(values).into("users").sql();

        assertEquals("INSERT INTO users (first_name, last_name, email) VALUES (?, ?, ?)", sql);
    }

    @Test
    public void testInsertWithEntity() {
        Account account = new Account();
        account.setId(1);
        account.setFirstName("John");
        account.setLastName("Doe");

        String sql = PSC.insert(account).into("account").sql();

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

        String sql = PSC.batchInsert(propsList).into("users").sql();

        assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?), (?, ?)", sql);
    }

    @Test
    public void testUpdateWithoutSet() {
        String sql = PSC.update("users").where(Filters.eq("id", 1)).sql();

        assertTrue(sql.contains("UPDATE users SET"));
        assertTrue(sql.contains("WHERE id = ?"));
    }

    @Test
    public void testDeleteFrom() {
        String sql = PSC.deleteFrom("users").where(Filters.eq("status", "DELETED")).sql();

        assertEquals("DELETE FROM users WHERE status = ?", sql);

        // With entity class
        sql = PSC.deleteFrom(Account.class).where(Filters.eq("id", 1)).sql();

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
        String sql = NSC.select("*").from("users").where(Filters.eq("firstName", "John").and(Filters.gt("age", 25))).sql();

        assertTrue(sql.contains("first_name = :firstName"));
        assertTrue(sql.contains("age > :age"));

        List<Object> params = NSC.select("*").from("users").where(Filters.eq("firstName", "John").and(Filters.gt("age", 25))).parameters();

        assertEquals(2, params.size());
        assertEquals("John", params.get(0));
        assertEquals(25, params.get(1));
    }

    @Test
    public void testNamedSQLWithIn() {
        String sql = NSC.select("*").from("users").where(Filters.in("status", Arrays.asList("ACTIVE", "PENDING"))).sql();

        assertTrue(sql.contains("status IN (:status1, :status2)"));
    }

    @Test
    public void testNamedSQLWithBetween() {
        String sql = NSC.select("*").from("users").where(Filters.between("age", 18, 65)).sql();

        assertTrue(sql.contains("age BETWEEN :minAge AND :maxAge"));
    }

    @Test
    public void testComplexJoinConditions() {
        String sql = PSC.select("*")
                .from("users u")
                .leftJoin("orders o")
                .on(Filters.and(Filters.eq("u.id", "o.user_id"), Filters.eq("o.status", "COMPLETED")))
                .sql();

        assertTrue(sql.contains("LEFT JOIN orders o ON"));
        assertTrue(sql.contains("u.id = ?"));
        assertTrue(sql.contains("o.status = ?"));
    }

    @Test
    public void testIbatisSQLPolicy() {
        // Need to create a new builder class that uses IBATIS_SQL policy
        // Since the existing builders use different policies
        // This is just to show the expected behavior

        // The IBATIS SQL policy would generate #{paramName} style parameters
        // Example expected output: "UPDATE users SET name = #{name} WHERE id = #{id}"
    }

    @Test
    public void testMultipleWhereConditions() {

        assertThrows(IllegalStateException.class, () -> {
            PSC.select("*")
                    .from("users")
                    .where(Filters.gt("age", 18))
                    .where(Filters.eq("status", "ACTIVE"))
                    .where(Filters.like("email", "%@company.com"))
                    .sql();
        });

    }

    @Test
    public void testUnionWithSubQuery() {
        String subQuery = "(SELECT id, name FROM archived_users WHERE status = 'INACTIVE')";

        String sql = PSC.select("id", "name").from("users").where(Filters.eq("status", "ACTIVE")).union(subQuery).sql();

        assertTrue(sql.contains("SELECT id, name FROM users WHERE status = ?"));
        assertTrue(sql.contains("UNION"));
        assertTrue(sql.contains(subQuery));
    }

    @Test
    public void testColumnNameWithSpecialCharacters() {
        String sql = PSC.select("user.name", "COUNT(*) as total").from("users").groupBy("user.name").sql();

        assertTrue(sql.contains("user.name"));
        assertTrue(sql.contains(" COUNT(*) AS total"));
        assertTrue(sql.contains("GROUP BY user.name"));
    }

    @Test
    public void testMultipleGroupByWithDifferentSortDirections() {
        String sql = PSC.select("category", "brand", "COUNT(*)")
                .from("products")
                .groupBy(N.asLinkedHashMap("brand", SortDirection.DESC, "category", SortDirection.ASC))
                .sql();

        // Note: This test shows that each groupBy call replaces the previous one
        // The SQL will only contain the last GROUP BY clause
        assertTrue(sql.contains("GROUP BY brand DESC"));
    }

    @Test
    public void testLimitWithLargeNumbers() {
        String sql = PSC.select("*").from("users").limit(50, 1000000).sql();

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
                .sql();

        assertTrue(sql.contains("UPDATE users SET"));
        assertTrue(sql.contains("status = ?"));
        assertTrue(sql.contains("last_modified_date = ?"));
        assertTrue(sql.contains("WHERE"));
    }

    @Test
    public void testExpressionWithComplexSQL() {
        String sql = PSC.select("*").from("users").where(Filters.expr("DATEDIFF(day, created_date, GETDATE()) > 30")).sql();

        assertEquals("SELECT * FROM users WHERE DATEDIFF(day, created_date, GETDATE()) > 30", sql);
    }

    @Test
    public void testJoinUsingMultipleColumns() {
        String sql = PSC.select("*").from("table1").join("table2").using("(col1, col2)").sql();

        assertEquals("SELECT * FROM table1 JOIN table2 USING (col1, col2)", sql);
    }

    @Test
    public void testSelectWithFunctions() {
        String sql = PSC.select("MAX(age)", "MIN(age)", "AVG(age)", "COUNT(*)").from("users").groupBy("status").sql();

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
                .sql();

        assertTrue(sql.contains("GROUP BY status"));
        assertTrue(sql.contains("HAVING (COUNT(*) > ?) AND (COUNT(*) < ?)"));
    }

    @Test
    public void testAppendWithCriteria() {
        Criteria criteria = Filters.criteria()
                .where(Filters.eq("status", "ACTIVE"))
                .groupBy("department")
                .having(Filters.gt("COUNT(*)", 5))
                .orderBy("department")
                .limit(20, 10);

        String sql = PSC.select("department", "COUNT(*)").from("employees").append(criteria).sql();

        assertTrue(sql.contains("WHERE status = ?"));
        assertTrue(sql.contains("GROUP BY department"));
        assertTrue(sql.contains("HAVING COUNT(*) > ?"));
        assertTrue(sql.contains("ORDER BY department"));
        assertTrue(sql.contains("LIMIT 20 OFFSET 10"));
    }

    @Test
    public void testWhereClause() {
        Where where = Filters.where("age > 18 AND status = 'ACTIVE'");

        String sql = PSC.select("*").from("users").append(where).sql();

        assertTrue(sql.contains("WHERE age > 18 AND status = 'ACTIVE'"));
    }

    @Test
    public void testHavingClause() {
        Having having = Filters.having("COUNT(*) > 10");

        String sql = PSC.select("status", "COUNT(*)").from("users").groupBy("status").append(having).sql();

        assertTrue(sql.contains("HAVING COUNT(*) > 10"));
    }

    @Test
    public void testOrderByWithExpression() {
        String sql = PSC.select("*").from("users").orderBy("CASE WHEN status = 'VIP' THEN 0 ELSE 1 END, name").sql();

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

        String sql = PSC.insert(account, excluded).into("account").sql();

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

        String sql = PSC.batchInsert(accounts).into("account").sql();

        assertTrue(sql.contains("INSERT INTO account"));
        assertTrue(sql.contains("VALUES"));
        assertTrue(sql.contains("), ("));
    }

    @Test
    public void testDeleteFromWithMultipleConditions() {
        String sql = PSC.deleteFrom("users").where(Filters.and(Filters.eq("status", "DELETED"), Filters.lt("deletedDate", new Date()))).sql();

        assertTrue(sql.contains("DELETE FROM users"));
        assertTrue(sql.contains("WHERE (status = ?) AND (deleted_date < ?)"));
    }

    @Test
    public void testSelectFromWithExcludedProperties() {
        String sql = PSC.selectFrom(Account.class, N.asSet("createdDate", "lastModifiedDate")).sql();

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
                .sql();

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
    public void testSQLBuilderWithDifferentNamingPolicies() {
        // Test that different SQLBuilder implementations use correct naming policies

        // Snake case
        assertTrue(PSC.select("firstName").from("userAccount").sql().contains("first_name"));
        assertTrue(NSC.select("firstName").from("userAccount").sql().contains("first_name"));

        // Upper case
        assertTrue(PAC.select("firstName").from("userAccount").sql().contains("FIRST_NAME"));
        assertTrue(NAC.select("firstName").from("userAccount").sql().contains("FIRST_NAME"));

        // Lower camel case
        assertTrue(PLC.select("first_name").from("user_account").sql().contains("firstName"));
        assertTrue(NLC.select("first_name").from("user_account").sql().contains("firstName"));
    }

    @Test
    public void testAppendWithClause() {
        // Test appending a Clause condition
        String sql = PSC.select("*").from("users").append(Filters.eq("status", "ACTIVE")).sql();

        assertTrue(sql.contains("status = ?"));
    }

    @Test
    public void testMultipleAppendsWithConditions() {
        String sql = PSC.select("*").from("users").append(Filters.eq("status", "ACTIVE")).append(" AND age > 18").sql();

        assertTrue(sql.contains("WHERE status = ?"));
        assertTrue(sql.contains("AND age > 18"));
    }

    @Test
    public void testUpdateAllProperties() {
        String sql = PSC.update("account").set(Account.class).where(Filters.eq("id", 1)).sql();

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
                .sql());
    }

    @Test
    public void testNullParameters() {
        String sql = PSC.select("*").from("users").where(Filters.eq("deletedBy", null)).sql();

        assertEquals("SELECT * FROM users WHERE deleted_by = ?", sql);

        SQLBuilder builder = PSC.select("*").from("users").where(Filters.eq("deletedBy", null));
        builder.sql();

        List<Object> params = builder.parameters();
        assertEquals(1, params.size());
        assertNull(params.get(0));
    }

    @Test
    public void testCaseInsensitiveKeywords() {
        // Test that SQL keywords are preserved regardless of case
        String sql = PSC.select("*").from("users").where(Filters.expr("select = 1 AND from = 2")).sql();

        assertTrue(sql.contains("select = 1 AND from = 2"));
    }

    @Test
    public void testVeryLongColumnList() {
        // Test with many columns
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            columns.add("col" + i);
        }

        String sql = PSC.select(columns).from("big_table").sql();

        assertTrue(sql.startsWith("SELECT col0, col1, col2"));
        assertTrue(sql.contains("col49"));
        assertTrue(sql.contains("FROM big_table"));
    }

    @Test
    public void testSpecialCharactersInValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "O'Brien");
        values.put("comment", "Test \"quote\" handling");

        String sql = PSC.update("users").set(values).where(Filters.eq("id", 1)).sql();

        assertTrue(sql.contains("UPDATE users SET"));
        assertTrue(sql.contains("name = ?"));
        assertTrue(sql.contains("comment = ?"));

        SQLBuilder builder = PSC.update("users").set(values).where(Filters.eq("id", 1));
        builder.sql();

        List<Object> params = builder.parameters();
        assertEquals("O'Brien", params.get(0));
        assertEquals("Test \"quote\" handling", params.get(1));
    }

    @Test
    public void testAliasPropColumnNameMap() {
        // Test query with table aliases and property column name mapping
        String sql = PSC.select("a.firstName", "o.orderNumber").from(Account.class, "a").join(Order.class, "o").on("a.id = o.userId").sql();

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

        String sql = PSC.select(AbstractQueryBuilder.COUNT_ALL).from("users").sql();

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
    public void testQMEAsParameter() {
        // Test using QME (Question Mark Expression) as parameter
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", Filters.QME);
        values.put("age", 25);

        String sql = PSC.update("users").set(values).where(Filters.eq("id", 1)).sql();

        assertTrue(sql.contains("name = ?"));
        assertTrue(sql.contains("age = ?"));
    }

    @Test
    public void testClosedBuilderException() {
        // Test that using a closed builder throws exception
        SQLBuilder builder = PSC.select("*").from("users");
        builder.sql(); // This closes the builder

        // Any operation after sql() should throw exception
        assertThrows(RuntimeException.class, () -> builder.where(Filters.eq("id", 1)));
        assertThrows(RuntimeException.class, () -> builder.sql());
    }

    @Test
    public void testSPImmutability() {
        // Test that SP parameters list is immutable
        List<Object> mutableList = new ArrayList<>();
        mutableList.add("param1");

        SQLBuilder.SP sP = new SQLBuilder.SP("SELECT * FROM users", mutableList);

        // Try to modify the list
        assertThrows(UnsupportedOperationException.class, () -> sP.parameters.add("param2"));
    }

    @Test
    public void testActiveStringBuilderLimit() {
        // This test verifies that the active StringBuilder counter works
        // In practice, creating too many builders without calling sql() would log warnings

        List<SQLBuilder> builders = new ArrayList<>();

        // Create multiple builders without closing them
        for (int i = 0; i < 10; i++) {
            builders.add(PSC.select("*").from("users"));
        }

        // Clean up
        for (SQLBuilder builder : builders) {
            builder.sql();
        }
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
}
