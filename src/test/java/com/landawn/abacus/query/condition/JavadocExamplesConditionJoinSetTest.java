/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;

/**
 * Verifies ALL Javadoc usage examples in the condition join/set/clause/subquery files.
 * Each test method corresponds to a specific {@code <pre>{@code ...}</pre>} block from source Javadocs.
 */
public class JavadocExamplesConditionJoinSetTest {

    // =====================================================
    // Join.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testJoin_classLevel_basicJoin() {
        Join join = new Join("orders");
        assertEquals("JOIN orders", join.toString());
    }

    @Test
    public void testJoin_classLevel_joinWithOnCondition() {
        Join joinWithCondition = new Join("orders o",
            new On("customers.id", "o.customer_id"));
        assertEquals("JOIN orders o ON customers.id = o.customer_id", joinWithCondition.toString());
    }

    @Test
    public void testJoin_classLevel_joinWithExpression() {
        Join exprJoin = new Join("orders o",
            Filters.expr("customers.id = o.customer_id"));
        assertEquals("JOIN orders o customers.id = o.customer_id", exprJoin.toString());
    }

    @Test
    public void testJoin_classLevel_joinMultipleTables() {
        Join multiJoin = new Join(Arrays.asList("orders o", "order_items oi"),
            new On("o.id", "oi.order_id"));
        assertEquals("JOIN (orders o, order_items oi) ON o.id = oi.order_id", multiJoin.toString());
    }

    // Join(String) constructor examples
    @Test
    public void testJoin_constructor_simple() {
        Join join = new Join("products");
        assertEquals("JOIN products", join.toString());
    }

    @Test
    public void testJoin_constructor_withAlias() {
        Join aliasJoin = new Join("product_categories pc");
        assertEquals("JOIN product_categories pc", aliasJoin.toString());
    }

    // Join(String, Condition) constructor examples
    @Test
    public void testJoin_constructorWithCondition_on() {
        Join orderJoin = new Join("orders o",
            new On("customers.id", "o.customer_id"));
        assertEquals("JOIN orders o ON customers.id = o.customer_id", orderJoin.toString());
    }

    @Test
    public void testJoin_constructorWithCondition_expr() {
        Join exprJoin = new Join("orders o",
            Filters.expr("customers.id = o.customer_id"));
        assertEquals("JOIN orders o customers.id = o.customer_id", exprJoin.toString());
    }

    @Test
    public void testJoin_constructorWithCondition_complexAnd() {
        Join complexJoin = new Join("products p",
            new And(
                new On("categories.id", "p.category_id"),
                Filters.eq("p.active", true)
            ));
        String result = complexJoin.toString();
        assertNotNull(result);
        assertTrue(result.startsWith("JOIN products p"));
        // And wraps each condition in parens with outer parens:
        // ((ON categories.id = p.category_id) AND (p.active = true))
        assertTrue(result.contains("ON categories.id = p.category_id"));
        assertTrue(result.contains("p.active = true"));
    }

    // Join(Collection, Condition) constructor examples
    @Test
    public void testJoin_multiTableConstructor_andOnConditions() {
        List<String> tables = Arrays.asList("orders o", "customers c");
        Join multiJoin = new Join(tables,
            new And(
                new On("o.customer_id", "c.id"),
                new On("c.address_id", "a.id")
            ));
        String result = multiJoin.toString();
        assertTrue(result.startsWith("JOIN (orders o, customers c)"));
        assertTrue(result.contains("ON o.customer_id = c.id"));
        assertTrue(result.contains("ON c.address_id = a.id"));
    }

    @Test
    public void testJoin_multiTableConstructor_exprConditions() {
        List<String> tables = Arrays.asList("orders o", "customers c");
        Join exprMultiJoin = new Join(tables,
            new And(
                Filters.expr("o.customer_id = c.id"),
                Filters.expr("o.status = 'active'")
            ));
        String result = exprMultiJoin.toString();
        assertTrue(result.startsWith("JOIN (orders o, customers c)"));
        assertTrue(result.contains("o.customer_id = c.id"));
        assertTrue(result.contains("o.status = 'active'"));
    }

    // getJoinEntities() examples
    @Test
    public void testJoin_getJoinEntities_single() {
        Join join = new Join("orders o", new On("customers.id", "o.customer_id"));
        List<String> entities = join.getJoinEntities();
        assertEquals(Arrays.asList("orders o"), entities);
    }

    @Test
    public void testJoin_getJoinEntities_multi() {
        Join multiJoin = new Join(Arrays.asList("orders o", "order_items oi"),
            new On("o.id", "oi.order_id"));
        List<String> multiEntities = multiJoin.getJoinEntities();
        assertEquals(Arrays.asList("orders o", "order_items oi"), multiEntities);
    }

    // getCondition() examples
    @Test
    public void testJoin_getCondition_withOn() {
        On onCondition = new On("customers.id", "o.customer_id");
        Join join = new Join("orders o", onCondition);
        Condition condition = join.getCondition();
        assertNotNull(condition);
        assertTrue(condition.toString().contains("ON customers.id = o.customer_id"));
    }

    @Test
    public void testJoin_getCondition_noCondition() {
        Join simpleJoin = new Join("products");
        Condition noCondition = simpleJoin.getCondition();
        assertNull(noCondition);
    }

    // =====================================================
    // InnerJoin.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testInnerJoin_classLevel_simple() {
        InnerJoin join1 = new InnerJoin("orders");
        assertEquals("INNER JOIN orders", join1.toString());
    }

    @Test
    public void testInnerJoin_classLevel_withOn() {
        InnerJoin join2 = new InnerJoin("orders o",
            new On("customers.id", "o.customer_id"));
        assertEquals("INNER JOIN orders o ON customers.id = o.customer_id", join2.toString());
    }

    @Test
    public void testInnerJoin_classLevel_complexAnd() {
        InnerJoin customerOrders = new InnerJoin("orders o",
            new And(
                new On("c.id", "o.customer_id"),
                Filters.eq("o.status", "completed")
            ));
        String result = customerOrders.toString();
        assertTrue(result.startsWith("INNER JOIN orders o"));
        assertTrue(result.contains("ON c.id = o.customer_id"));
        assertTrue(result.contains("o.status = 'completed'"));
    }

    @Test
    public void testInnerJoin_classLevel_complexMultiCondition() {
        InnerJoin complexJoin = new InnerJoin("inventory i",
            new And(
                new On("p.product_id", "i.product_id"),
                new On("p.warehouse_id", "i.warehouse_id"),
                Filters.gt("i.quantity", 0)
            ));
        String result = complexJoin.toString();
        assertTrue(result.startsWith("INNER JOIN inventory i"));
        assertTrue(result.contains("ON p.product_id = i.product_id"));
        assertTrue(result.contains("ON p.warehouse_id = i.warehouse_id"));
        assertTrue(result.contains("i.quantity > 0"));
    }

    @Test
    public void testInnerJoin_classLevel_withExpr() {
        InnerJoin exprJoin = new InnerJoin("customers c",
            Filters.expr("orders.customer_id = c.id"));
        assertEquals("INNER JOIN customers c orders.customer_id = c.id", exprJoin.toString());
    }

    // InnerJoin(String) constructor examples
    @Test
    public void testInnerJoin_constructorString_simple() {
        InnerJoin join = new InnerJoin("products");
        assertEquals("INNER JOIN products", join.toString());
    }

    @Test
    public void testInnerJoin_constructorString_withAlias() {
        InnerJoin aliasJoin = new InnerJoin("order_details od");
        assertEquals("INNER JOIN order_details od", aliasJoin.toString());
    }

    // InnerJoin(String, Condition) constructor examples
    @Test
    public void testInnerJoin_constructorWithCondition_customerOrders() {
        InnerJoin customerOrders = new InnerJoin("customers c",
            new On("orders.customer_id", "c.id"));
        assertEquals("INNER JOIN customers c ON orders.customer_id = c.id", customerOrders.toString());
    }

    @Test
    public void testInnerJoin_constructorWithCondition_compositeKey() {
        InnerJoin compositeJoin = new InnerJoin("order_items oi",
            new And(
                new On("orders.id", "oi.order_id"),
                new On("orders.version", "oi.order_version")
            ));
        String result = compositeJoin.toString();
        assertTrue(result.startsWith("INNER JOIN order_items oi"));
        assertTrue(result.contains("ON orders.id = oi.order_id"));
        assertTrue(result.contains("ON orders.version = oi.order_version"));
    }

    @Test
    public void testInnerJoin_constructorWithCondition_filtered() {
        InnerJoin filteredJoin = new InnerJoin("products p",
            new And(
                new On("order_items.product_id", "p.id"),
                Filters.eq("p.active", true),
                Filters.gt("p.stock", 0)
            ));
        String result = filteredJoin.toString();
        assertTrue(result.startsWith("INNER JOIN products p"));
        assertTrue(result.contains("ON order_items.product_id = p.id"));
        assertTrue(result.contains("p.active = true"));
        assertTrue(result.contains("p.stock > 0"));
    }

    @Test
    public void testInnerJoin_constructorWithCondition_expr() {
        InnerJoin exprJoin = new InnerJoin("customers c",
            Filters.expr("orders.customer_id = c.id"));
        assertEquals("INNER JOIN customers c orders.customer_id = c.id", exprJoin.toString());
    }

    // InnerJoin(Collection, Condition) constructor examples
    @Test
    public void testInnerJoin_multiTable_simpleOn() {
        List<String> tables = Arrays.asList("orders o", "customers c");
        InnerJoin multiJoin = new InnerJoin(tables,
            new On("o.customer_id", "c.id"));
        assertEquals("INNER JOIN (orders o, customers c) ON o.customer_id = c.id", multiJoin.toString());
    }

    @Test
    public void testInnerJoin_multiTable_complexAnd() {
        List<String> entities = Arrays.asList("products p", "categories cat", "suppliers s");
        InnerJoin complexMulti = new InnerJoin(entities,
            new And(
                new On("p.category_id", "cat.id"),
                new On("p.supplier_id", "s.id")
            ));
        String result = complexMulti.toString();
        assertTrue(result.startsWith("INNER JOIN (products p, categories cat, suppliers s)"));
        assertTrue(result.contains("ON p.category_id = cat.id"));
        assertTrue(result.contains("ON p.supplier_id = s.id"));
    }

    @Test
    public void testInnerJoin_multiTable_withExpr() {
        List<String> tables = Arrays.asList("orders o", "customers c");
        InnerJoin exprMulti = new InnerJoin(tables,
            Filters.expr("o.customer_id = c.id AND o.status = 'active'"));
        assertEquals("INNER JOIN (orders o, customers c) o.customer_id = c.id AND o.status = 'active'", exprMulti.toString());
    }

    // =====================================================
    // LeftJoin.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testLeftJoin_classLevel_simple() {
        LeftJoin join1 = new LeftJoin("orders");
        assertEquals("LEFT JOIN orders", join1.toString());
    }

    @Test
    public void testLeftJoin_classLevel_withOn() {
        LeftJoin customerOrders = new LeftJoin("orders o",
            new On("customers.id", "o.customer_id"));
        assertEquals("LEFT JOIN orders o ON customers.id = o.customer_id", customerOrders.toString());
    }

    @Test
    public void testLeftJoin_classLevel_noOrders() {
        LeftJoin noOrders = new LeftJoin("orders o",
            new On("c.id", "o.customer_id"));
        assertEquals("LEFT JOIN orders o ON c.id = o.customer_id", noOrders.toString());
    }

    @Test
    public void testLeftJoin_classLevel_complex() {
        LeftJoin complexJoin = new LeftJoin("order_items oi",
            new And(
                new On("o.id", "oi.order_id"),
                Filters.eq("oi.status", "active"),
                Filters.gt("oi.quantity", 0)
            ));
        String result = complexJoin.toString();
        assertTrue(result.startsWith("LEFT JOIN order_items oi"));
        assertTrue(result.contains("ON o.id = oi.order_id"));
        assertTrue(result.contains("oi.status = 'active'"));
        assertTrue(result.contains("oi.quantity > 0"));
    }

    @Test
    public void testLeftJoin_classLevel_withExpr() {
        LeftJoin exprJoin = new LeftJoin("orders o",
            Filters.expr("customers.id = o.customer_id"));
        assertEquals("LEFT JOIN orders o customers.id = o.customer_id", exprJoin.toString());
    }

    // LeftJoin(String) constructor examples
    @Test
    public void testLeftJoin_constructorString_simple() {
        LeftJoin join = new LeftJoin("departments");
        assertEquals("LEFT JOIN departments", join.toString());
    }

    @Test
    public void testLeftJoin_constructorString_alias() {
        LeftJoin aliasJoin = new LeftJoin("employee_departments ed");
        assertEquals("LEFT JOIN employee_departments ed", aliasJoin.toString());
    }

    // LeftJoin(String, Condition) constructor examples
    @Test
    public void testLeftJoin_constructorWithCondition_customerOrders() {
        LeftJoin customerOrders = new LeftJoin("orders o",
            new On("customers.id", "o.customer_id"));
        assertEquals("LEFT JOIN orders o ON customers.id = o.customer_id", customerOrders.toString());
    }

    @Test
    public void testLeftJoin_constructorWithCondition_empDept() {
        LeftJoin empDept = new LeftJoin("departments d",
            new On("employees.dept_id", "d.id"));
        assertEquals("LEFT JOIN departments d ON employees.dept_id = d.id", empDept.toString());
    }

    @Test
    public void testLeftJoin_constructorWithCondition_active() {
        LeftJoin activeItems = new LeftJoin("order_items oi",
            new And(
                new On("orders.id", "oi.order_id"),
                Filters.eq("oi.status", "active"),
                Filters.gt("oi.created_date", "2023-01-01")
            ));
        String result = activeItems.toString();
        assertTrue(result.startsWith("LEFT JOIN order_items oi"));
        assertTrue(result.contains("ON orders.id = oi.order_id"));
        assertTrue(result.contains("oi.status = 'active'"));
        assertTrue(result.contains("oi.created_date > '2023-01-01'"));
    }

    @Test
    public void testLeftJoin_constructorWithCondition_expr() {
        LeftJoin exprJoin = new LeftJoin("orders o",
            Filters.expr("customers.id = o.customer_id AND o.amount > 100"));
        assertEquals("LEFT JOIN orders o customers.id = o.customer_id AND o.amount > 100", exprJoin.toString());
    }

    // LeftJoin(Collection, Condition) constructor examples
    @Test
    public void testLeftJoin_multiTable() {
        List<String> tables = Arrays.asList("orders o", "order_items oi");
        LeftJoin join = new LeftJoin(tables,
            new And(
                new On("c.id", "o.customer_id"),
                new On("o.id", "oi.order_id")
            ));
        String result = join.toString();
        assertTrue(result.startsWith("LEFT JOIN (orders o, order_items oi)"));
        assertTrue(result.contains("ON c.id = o.customer_id"));
        assertTrue(result.contains("ON o.id = oi.order_id"));
    }

    @Test
    public void testLeftJoin_multiTable_expr() {
        List<String> tables = Arrays.asList("orders o", "order_items oi");
        LeftJoin exprJoin = new LeftJoin(tables,
            Filters.expr("c.id = o.customer_id AND o.id = oi.order_id"));
        assertEquals("LEFT JOIN (orders o, order_items oi) c.id = o.customer_id AND o.id = oi.order_id", exprJoin.toString());
    }

    // =====================================================
    // RightJoin.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testRightJoin_classLevel_simple() {
        RightJoin join1 = new RightJoin("departments");
        assertEquals("RIGHT JOIN departments", join1.toString());
    }

    @Test
    public void testRightJoin_classLevel_withOn() {
        RightJoin join2 = new RightJoin("departments",
            new On("employees.dept_id", "departments.id"));
        assertEquals("RIGHT JOIN departments ON employees.dept_id = departments.id", join2.toString());
    }

    @Test
    public void testRightJoin_classLevel_complex() {
        RightJoin complexJoin = new RightJoin("products p",
            new And(
                new On("order_items.product_id", "p.id"),
                Filters.eq("p.active", true)
            ));
        String result = complexJoin.toString();
        assertTrue(result.startsWith("RIGHT JOIN products p"));
        assertTrue(result.contains("ON order_items.product_id = p.id"));
        assertTrue(result.contains("p.active = true"));
    }

    @Test
    public void testRightJoin_classLevel_withExpr() {
        RightJoin exprJoin = new RightJoin("departments",
            Filters.expr("employees.dept_id = departments.id"));
        assertEquals("RIGHT JOIN departments employees.dept_id = departments.id", exprJoin.toString());
    }

    // RightJoin(String) constructor examples
    @Test
    public void testRightJoin_constructorString_simple() {
        RightJoin join = new RightJoin("departments");
        assertEquals("RIGHT JOIN departments", join.toString());
    }

    @Test
    public void testRightJoin_constructorString_alias() {
        RightJoin aliasJoin = new RightJoin("all_customers c");
        assertEquals("RIGHT JOIN all_customers c", aliasJoin.toString());
    }

    // RightJoin(String, Condition) constructor examples
    @Test
    public void testRightJoin_constructorWithCondition_products() {
        RightJoin allProducts = new RightJoin("products p",
            new On("order_items.product_id", "p.id"));
        assertEquals("RIGHT JOIN products p ON order_items.product_id = p.id", allProducts.toString());
    }

    @Test
    public void testRightJoin_constructorWithCondition_departments() {
        RightJoin allDepts = new RightJoin("departments d",
            new On("employees.dept_id", "d.id"));
        assertEquals("RIGHT JOIN departments d ON employees.dept_id = d.id", allDepts.toString());
    }

    @Test
    public void testRightJoin_constructorWithCondition_complex() {
        RightJoin activeCategories = new RightJoin("categories c",
            new And(
                new On("products.category_id", "c.id"),
                Filters.eq("c.active", true),
                Filters.gt("c.created_date", "2023-01-01")
            ));
        String result = activeCategories.toString();
        assertTrue(result.startsWith("RIGHT JOIN categories c"));
        assertTrue(result.contains("ON products.category_id = c.id"));
        assertTrue(result.contains("c.active = true"));
        assertTrue(result.contains("c.created_date > '2023-01-01'"));
    }

    @Test
    public void testRightJoin_constructorWithCondition_expr() {
        RightJoin exprJoin = new RightJoin("products p",
            Filters.expr("order_items.product_id = p.id AND p.stock > 0"));
        assertEquals("RIGHT JOIN products p order_items.product_id = p.id AND p.stock > 0", exprJoin.toString());
    }

    // RightJoin(Collection, Condition) constructor examples
    @Test
    public void testRightJoin_multiTable() {
        List<String> tables = Arrays.asList("categories c", "subcategories sc");
        RightJoin join = new RightJoin(tables,
            new And(
                new On("p.category_id", "c.id"),
                new On("p.subcategory_id", "sc.id")
            ));
        String result = join.toString();
        assertTrue(result.startsWith("RIGHT JOIN (categories c, subcategories sc)"));
        assertTrue(result.contains("ON p.category_id = c.id"));
        assertTrue(result.contains("ON p.subcategory_id = sc.id"));
    }

    @Test
    public void testRightJoin_multiTable_expr() {
        List<String> tables = Arrays.asList("categories c", "subcategories sc");
        RightJoin exprJoin = new RightJoin(tables,
            Filters.expr("p.category_id = c.id AND p.subcategory_id = sc.id"));
        assertEquals("RIGHT JOIN (categories c, subcategories sc) p.category_id = c.id AND p.subcategory_id = sc.id", exprJoin.toString());
    }

    // =====================================================
    // FullJoin.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testFullJoin_classLevel_withOn() {
        FullJoin join = new FullJoin("orders",
            new On("users.id", "orders.user_id"));
        assertEquals("FULL JOIN orders ON users.id = orders.user_id", join.toString());
    }

    @Test
    public void testFullJoin_classLevel_inventory() {
        FullJoin inventoryJoin = new FullJoin("warehouse_inventory",
            new On("online_inventory.product_id", "warehouse_inventory.product_id"));
        assertEquals("FULL JOIN warehouse_inventory ON online_inventory.product_id = warehouse_inventory.product_id", inventoryJoin.toString());
    }

    @Test
    public void testFullJoin_classLevel_complex() {
        FullJoin complexJoin = new FullJoin("external_data e",
            new And(
                new On("internal_data.id", "e.id"),
                Filters.gt("e.updated_date", "2024-01-01")
            ));
        String result = complexJoin.toString();
        assertTrue(result.startsWith("FULL JOIN external_data e"));
        assertTrue(result.contains("ON internal_data.id = e.id"));
        assertTrue(result.contains("e.updated_date > '2024-01-01'"));
    }

    // FullJoin(String) constructor examples
    @Test
    public void testFullJoin_constructorString_simple() {
        FullJoin join = new FullJoin("departments");
        assertEquals("FULL JOIN departments", join.toString());
    }

    @Test
    public void testFullJoin_constructorString_alias() {
        FullJoin aliasJoin = new FullJoin("employee_departments ed");
        assertEquals("FULL JOIN employee_departments ed", aliasJoin.toString());
    }

    // FullJoin(String, Condition) constructor examples
    @Test
    public void testFullJoin_constructorWithCondition_empDept() {
        FullJoin empDept = new FullJoin("departments d",
            new On("employees.dept_id", "d.id"));
        assertEquals("FULL JOIN departments d ON employees.dept_id = d.id", empDept.toString());
    }

    @Test
    public void testFullJoin_constructorWithCondition_usersOrders() {
        FullJoin allData = new FullJoin("orders o",
            new On("users.id", "o.user_id"));
        assertEquals("FULL JOIN orders o ON users.id = o.user_id", allData.toString());
    }

    @Test
    public void testFullJoin_constructorWithCondition_complex() {
        FullJoin reconcileData = new FullJoin("external_inventory ei",
            new And(
                new On("internal_inventory.product_id", "ei.product_id"),
                Filters.eq("ei.active", true),
                Filters.gt("ei.updated_date", "2023-01-01")
            ));
        String result = reconcileData.toString();
        assertTrue(result.startsWith("FULL JOIN external_inventory ei"));
        assertTrue(result.contains("ON internal_inventory.product_id = ei.product_id"));
        assertTrue(result.contains("ei.active = true"));
        assertTrue(result.contains("ei.updated_date > '2023-01-01'"));
    }

    @Test
    public void testFullJoin_constructorWithCondition_expr() {
        FullJoin exprJoin = new FullJoin("departments d",
            Filters.expr("employees.dept_id = d.id AND d.active = true"));
        assertEquals("FULL JOIN departments d employees.dept_id = d.id AND d.active = true", exprJoin.toString());
    }

    // FullJoin(Collection, Condition) constructor examples
    @Test
    public void testFullJoin_multiTable() {
        List<String> tables = Arrays.asList("employees e", "contractors c");
        FullJoin join = new FullJoin(tables,
            new And(
                new On("d.id", "e.dept_id"),
                new On("d.id", "c.dept_id")
            ));
        String result = join.toString();
        assertTrue(result.startsWith("FULL JOIN (employees e, contractors c)"));
        assertTrue(result.contains("ON d.id = e.dept_id"));
        assertTrue(result.contains("ON d.id = c.dept_id"));
    }

    @Test
    public void testFullJoin_multiTable_expr() {
        List<String> tables = Arrays.asList("employees e", "contractors c");
        FullJoin exprJoin = new FullJoin(tables,
            Filters.expr("d.id = e.dept_id AND d.id = c.dept_id"));
        assertEquals("FULL JOIN (employees e, contractors c) d.id = e.dept_id AND d.id = c.dept_id", exprJoin.toString());
    }

    // =====================================================
    // CrossJoin.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testCrossJoin_classLevel_simple() {
        CrossJoin join = new CrossJoin("colors");
        assertEquals("CROSS JOIN colors", join.toString());
    }

    @Test
    public void testCrossJoin_classLevel_multiTable() {
        List<String> tables = Arrays.asList("sizes", "colors");
        CrossJoin multiJoin = new CrossJoin(tables, null);
        assertEquals("CROSS JOIN (sizes, colors)", multiJoin.toString());
    }

    @Test
    public void testCrossJoin_classLevel_withCondition() {
        CrossJoin filtered = new CrossJoin("categories",
            Filters.eq("active", true));
        String result = filtered.toString();
        assertTrue(result.startsWith("CROSS JOIN categories"));
        assertTrue(result.contains("active = true"));
    }

    @Test
    public void testCrossJoin_classLevel_withExpr() {
        CrossJoin exprJoin = new CrossJoin("inventory",
            Filters.expr("quantity > 0"));
        assertEquals("CROSS JOIN inventory quantity > 0", exprJoin.toString());
    }

    // CrossJoin(String) constructor examples
    @Test
    public void testCrossJoin_constructorString_simple() {
        CrossJoin join = new CrossJoin("colors");
        assertEquals("CROSS JOIN colors", join.toString());
    }

    @Test
    public void testCrossJoin_constructorString_alias() {
        CrossJoin aliasJoin = new CrossJoin("available_sizes s");
        assertEquals("CROSS JOIN available_sizes s", aliasJoin.toString());
    }

    // CrossJoin(String, Condition) constructor examples
    @Test
    public void testCrossJoin_constructorWithCondition_expr() {
        CrossJoin filtered = new CrossJoin("products p",
            Filters.expr("p.category = 'electronics'"));
        assertEquals("CROSS JOIN products p p.category = 'electronics'", filtered.toString());
    }

    @Test
    public void testCrossJoin_constructorWithCondition_on() {
        CrossJoin withOn = new CrossJoin("inventory i",
            new On("w.id", "i.warehouse_id"));
        assertEquals("CROSS JOIN inventory i ON w.id = i.warehouse_id", withOn.toString());
    }

    @Test
    public void testCrossJoin_constructorWithCondition_complex() {
        CrossJoin complexCross = new CrossJoin("inventory i",
            new And(
                new On("i.warehouse_id", "w.id"),
                Filters.eq("i.active", true)
            ));
        String result = complexCross.toString();
        assertTrue(result.startsWith("CROSS JOIN inventory i"));
        assertTrue(result.contains("ON i.warehouse_id = w.id"));
        assertTrue(result.contains("i.active = true"));
    }

    // CrossJoin(Collection, Condition) constructor examples
    @Test
    public void testCrossJoin_multiTable_withFilter() {
        List<String> tables = Arrays.asList("sizes s", "colors c", "styles st");
        CrossJoin join = new CrossJoin(tables,
            Filters.eq("active", true));
        String result = join.toString();
        assertTrue(result.startsWith("CROSS JOIN (sizes s, colors c, styles st)"));
        assertTrue(result.contains("active = true"));
    }

    @Test
    public void testCrossJoin_multiTable_withOn() {
        List<String> relatedTables = Arrays.asList("table1 t1", "table2 t2");
        CrossJoin withOn = new CrossJoin(relatedTables,
            new On("t1.id", "t2.t1_id"));
        assertEquals("CROSS JOIN (table1 t1, table2 t2) ON t1.id = t2.t1_id", withOn.toString());
    }

    @Test
    public void testCrossJoin_multiTable_withExpr() {
        List<String> tables = Arrays.asList("sizes s", "colors c", "styles st");
        CrossJoin exprJoin = new CrossJoin(tables,
            Filters.expr("active = true AND archived = false"));
        assertEquals("CROSS JOIN (sizes s, colors c, styles st) active = true AND archived = false", exprJoin.toString());
    }

    // =====================================================
    // NaturalJoin.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testNaturalJoin_classLevel_simple() {
        NaturalJoin join1 = new NaturalJoin("employees");
        assertEquals("NATURAL JOIN employees", join1.toString());
    }

    @Test
    public void testNaturalJoin_classLevel_withFilter() {
        NaturalJoin join2 = new NaturalJoin("departments",
            Filters.eq("status", "active"));
        String result = join2.toString();
        assertTrue(result.startsWith("NATURAL JOIN departments"));
        assertTrue(result.contains("status = 'active'"));
    }

    @Test
    public void testNaturalJoin_classLevel_withExpr() {
        NaturalJoin join3 = new NaturalJoin("orders",
            Filters.expr("order_date > '2024-01-01'"));
        assertEquals("NATURAL JOIN orders order_date > '2024-01-01'", join3.toString());
    }

    @Test
    public void testNaturalJoin_classLevel_multiTable() {
        List<String> tables = Arrays.asList("employees", "departments");
        NaturalJoin multiJoin = new NaturalJoin(tables,
            Filters.eq("active", true));
        String result = multiJoin.toString();
        assertTrue(result.startsWith("NATURAL JOIN (employees, departments)"));
        assertTrue(result.contains("active = true"));
    }

    // NaturalJoin(String) constructor example
    @Test
    public void testNaturalJoin_constructorString() {
        NaturalJoin join = new NaturalJoin("customers");
        assertEquals("NATURAL JOIN customers", join.toString());
    }

    // NaturalJoin(String, Condition) constructor examples
    @Test
    public void testNaturalJoin_constructorWithCondition_gt() {
        NaturalJoin join1 = new NaturalJoin("orders",
            Filters.gt("orderDate", "2024-01-01"));
        String result = join1.toString();
        assertTrue(result.startsWith("NATURAL JOIN orders"));
        assertTrue(result.contains("orderDate > '2024-01-01'"));
    }

    @Test
    public void testNaturalJoin_constructorWithCondition_expr() {
        NaturalJoin join2 = new NaturalJoin("products",
            Filters.expr("price > 100 AND stock > 0"));
        assertEquals("NATURAL JOIN products price > 100 AND stock > 0", join2.toString());
    }

    @Test
    public void testNaturalJoin_constructorWithCondition_complexAnd() {
        NaturalJoin join3 = new NaturalJoin("employees",
            new And(
                Filters.eq("status", "active"),
                Filters.gt("hire_date", "2020-01-01")
            ));
        String result = join3.toString();
        assertTrue(result.startsWith("NATURAL JOIN employees"));
        assertTrue(result.contains("status = 'active'"));
        assertTrue(result.contains("hire_date > '2020-01-01'"));
    }

    // NaturalJoin(Collection, Condition) constructor examples
    @Test
    public void testNaturalJoin_multiTable_gt() {
        List<String> tables = Arrays.asList("customers", "orders", "products");
        NaturalJoin join1 = new NaturalJoin(tables,
            Filters.gt("totalAmount", 1000));
        String result = join1.toString();
        assertTrue(result.startsWith("NATURAL JOIN (customers, orders, products)"));
        assertTrue(result.contains("totalAmount > 1000"));
    }

    @Test
    public void testNaturalJoin_multiTable_expr() {
        List<String> tables = Arrays.asList("customers", "orders", "products");
        NaturalJoin join2 = new NaturalJoin(tables,
            Filters.expr("status = 'active' AND verified = true"));
        assertEquals("NATURAL JOIN (customers, orders, products) status = 'active' AND verified = true", join2.toString());
    }

    @Test
    public void testNaturalJoin_multiTable_complexAnd() {
        List<String> tables = Arrays.asList("customers", "orders", "products");
        NaturalJoin join3 = new NaturalJoin(tables,
            new And(
                Filters.eq("region", "US"),
                Filters.gt("created_date", "2024-01-01")
            ));
        String result = join3.toString();
        assertTrue(result.startsWith("NATURAL JOIN (customers, orders, products)"));
        assertTrue(result.contains("region = 'US'"));
        assertTrue(result.contains("created_date > '2024-01-01'"));
    }

    // =====================================================
    // On.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testOn_classLevel_simpleEquality() {
        On on1 = new On("orders.customer_id", "customers.id");
        assertTrue(on1.toString().contains("ON orders.customer_id = customers.id"));
    }

    @Test
    public void testOn_classLevel_usedInJoin() {
        On on1 = new On("orders.customer_id", "customers.id");
        InnerJoin join = new InnerJoin("customers", on1);
        assertEquals("INNER JOIN customers ON orders.customer_id = customers.id", join.toString());
    }

    @Test
    public void testOn_classLevel_complexCondition() {
        Condition complexJoin = Filters.and(
            new On("o.customer_id", "c.id"),
            Filters.expr("o.order_date > c.registration_date")
        );
        LeftJoin leftJoin = new LeftJoin("customers c", complexJoin);
        String result = leftJoin.toString();
        assertTrue(result.startsWith("LEFT JOIN customers c"));
        assertTrue(result.contains("ON o.customer_id = c.id"));
        assertTrue(result.contains("o.order_date > c.registration_date"));
    }

    @Test
    public void testOn_classLevel_mapBasedCompositeKey() {
        Map<String, String> joinMap = new LinkedHashMap<>();
        joinMap.put("emp.department_id", "dept.id");
        joinMap.put("emp.location_id", "dept.location_id");
        On on3 = new On(joinMap);
        String result = on3.toString();
        assertTrue(result.contains("ON"));
        assertTrue(result.contains("emp.department_id = dept.id"));
        assertTrue(result.contains("emp.location_id = dept.location_id"));
    }

    @Test
    public void testOn_classLevel_filteredJoin() {
        Condition filteredJoin = Filters.and(
            new On("products.category_id", "categories.id"),
            Filters.eq("categories.active", true)
        );
        RightJoin rightJoin = new RightJoin("categories", filteredJoin);
        String result = rightJoin.toString();
        assertTrue(result.startsWith("RIGHT JOIN categories"));
        assertTrue(result.contains("ON products.category_id = categories.id"));
        assertTrue(result.contains("categories.active = true"));
    }

    // On(Condition) constructor examples
    @Test
    public void testOn_constructorCondition_simpleExpr() {
        On on1 = new On(Filters.expr("a.id = b.a_id"));
        InnerJoin join1 = new InnerJoin("table_b b", on1);
        assertEquals("INNER JOIN table_b b ON a.id = b.a_id", join1.toString());
    }

    @Test
    public void testOn_constructorCondition_complex() {
        Condition complexCondition = Filters.and(
            Filters.expr("orders.customer_id = customers.id"),
            Filters.between("orders.order_date", "2024-01-01", "2024-12-31"),
            Filters.ne("customers.status", "DELETED")
        );
        On on2 = new On(complexCondition);
        LeftJoin join2 = new LeftJoin("customers", on2);
        String result = join2.toString();
        assertTrue(result.startsWith("LEFT JOIN customers ON"));
        assertTrue(result.contains("orders.customer_id = customers.id"));
        assertTrue(result.contains("orders.order_date BETWEEN '2024-01-01' AND '2024-12-31'"));
        assertTrue(result.contains("customers.status != 'DELETED'"));
    }

    @Test
    public void testOn_constructorCondition_rangeJoin() {
        Condition rangeJoin = Filters.and(
            Filters.expr("emp.salary >= salary_grades.min_salary"),
            Filters.expr("emp.salary <= salary_grades.max_salary")
        );
        On on3 = new On(rangeJoin);
        InnerJoin join3 = new InnerJoin("salary_grades", on3);
        String result = join3.toString();
        assertTrue(result.startsWith("INNER JOIN salary_grades ON"));
        assertTrue(result.contains("emp.salary >= salary_grades.min_salary"));
        assertTrue(result.contains("emp.salary <= salary_grades.max_salary"));
    }

    // On(String, String) constructor examples
    @Test
    public void testOn_constructorTwoStrings_basic() {
        On on1 = new On("orders.customer_id", "customers.id");
        InnerJoin join1 = new InnerJoin("customers", on1);
        assertEquals("INNER JOIN customers ON orders.customer_id = customers.id", join1.toString());
    }

    @Test
    public void testOn_constructorTwoStrings_aliases() {
        On on2 = new On("o.product_id", "p.id");
        LeftJoin join2 = new LeftJoin("products p", on2);
        assertEquals("LEFT JOIN products p ON o.product_id = p.id", join2.toString());
    }

    @Test
    public void testOn_constructorTwoStrings_selfJoin() {
        On on3 = new On("emp1.manager_id", "emp2.employee_id");
        LeftJoin join3 = new LeftJoin("employees emp2", on3);
        assertEquals("LEFT JOIN employees emp2 ON emp1.manager_id = emp2.employee_id", join3.toString());
    }

    // On(Map) constructor examples
    @Test
    public void testOn_constructorMap_compositeKey() {
        Map<String, String> compositeKey = new LinkedHashMap<>();
        compositeKey.put("order_items.order_id", "orders.id");
        compositeKey.put("order_items.customer_id", "orders.customer_id");
        On on1 = new On(compositeKey);
        InnerJoin join1 = new InnerJoin("orders", on1);
        String result = join1.toString();
        assertTrue(result.startsWith("INNER JOIN orders ON"));
        assertTrue(result.contains("order_items.order_id = orders.id"));
        assertTrue(result.contains("order_items.customer_id = orders.customer_id"));
    }

    @Test
    public void testOn_constructorMap_tripleKey() {
        Map<String, String> tripleKey = new LinkedHashMap<>();
        tripleKey.put("t1.col1", "t2.col1");
        tripleKey.put("t1.col2", "t2.col2");
        tripleKey.put("t1.col3", "t2.col3");
        On on3 = new On(tripleKey);
        String result = on3.toString();
        assertTrue(result.contains("t1.col1 = t2.col1"));
        assertTrue(result.contains("t1.col2 = t2.col2"));
        assertTrue(result.contains("t1.col3 = t2.col3"));
    }

    // =====================================================
    // Using.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testUsing_classLevel_singleColumn() {
        Using using1 = new Using("department_id");
        InnerJoin join1 = new InnerJoin("departments", using1);
        String result = join1.toString();
        assertTrue(result.startsWith("INNER JOIN departments USING"));
        assertTrue(result.contains("department_id"));
    }

    @Test
    public void testUsing_classLevel_multipleColumns() {
        Using using2 = new Using("company_id", "branch_id");
        LeftJoin join2 = new LeftJoin("branches", using2);
        String result = join2.toString();
        assertTrue(result.startsWith("LEFT JOIN branches USING"));
        assertTrue(result.contains("company_id"));
        assertTrue(result.contains("branch_id"));
    }

    @Test
    public void testUsing_classLevel_collection() {
        Set<String> commonColumns = new LinkedHashSet<>(Arrays.asList("tenant_id", "workspace_id"));
        Using using3 = new Using(commonColumns);
        RightJoin join3 = new RightJoin("workspaces", using3);
        String result = join3.toString();
        assertTrue(result.startsWith("RIGHT JOIN workspaces USING"));
        assertTrue(result.contains("tenant_id"));
        assertTrue(result.contains("workspace_id"));
    }

    @Test
    public void testUsing_classLevel_tripleColumn() {
        Using using4 = new Using("org_id", "dept_id", "team_id");
        InnerJoin join4 = new InnerJoin("assignments", using4);
        String result = join4.toString();
        assertTrue(result.startsWith("INNER JOIN assignments USING"));
        assertTrue(result.contains("org_id"));
        assertTrue(result.contains("dept_id"));
        assertTrue(result.contains("team_id"));
    }

    // Using(String...) constructor examples
    @Test
    public void testUsing_constructorVarargs_single() {
        Using using = new Using("employee_id");
        InnerJoin join = new InnerJoin("employees", using);
        String result = join.toString();
        assertTrue(result.contains("INNER JOIN employees USING"));
        assertTrue(result.contains("employee_id"));
    }

    @Test
    public void testUsing_constructorVarargs_three() {
        Using multiColumn = new Using("company_id", "department_id", "team_id");
        LeftJoin leftJoin = new LeftJoin("assignments", multiColumn);
        String result = leftJoin.toString();
        assertTrue(result.contains("LEFT JOIN assignments USING"));
        assertTrue(result.contains("company_id"));
        assertTrue(result.contains("department_id"));
        assertTrue(result.contains("team_id"));
    }

    @Test
    public void testUsing_constructorVarargs_two() {
        Using twoCol = new Using("user_id", "account_id");
        RightJoin rightJoin = new RightJoin("accounts", twoCol);
        String result = rightJoin.toString();
        assertTrue(result.contains("RIGHT JOIN accounts USING"));
        assertTrue(result.contains("user_id"));
        assertTrue(result.contains("account_id"));
    }

    // Using(Collection) constructor examples
    @Test
    public void testUsing_constructorCollection_list() {
        List<String> sharedColumns = Arrays.asList("customer_id", "order_date");
        Using using = new Using(sharedColumns);
        InnerJoin join = new InnerJoin("customers", using);
        String result = join.toString();
        assertTrue(result.contains("INNER JOIN customers USING"));
        assertTrue(result.contains("customer_id"));
        assertTrue(result.contains("order_date"));
    }

    @Test
    public void testUsing_constructorCollection_set() {
        Set<String> tenantColumns = new LinkedHashSet<>();
        tenantColumns.add("tenant_id");
        tenantColumns.add("organization_id");
        Using tenantUsing = new Using(tenantColumns);
        LeftJoin leftJoin = new LeftJoin("organizations", tenantUsing);
        String result = leftJoin.toString();
        assertTrue(result.contains("LEFT JOIN organizations USING"));
        assertTrue(result.contains("tenant_id"));
        assertTrue(result.contains("organization_id"));
    }

    @Test
    public void testUsing_constructorCollection_threeElements() {
        List<String> joinCols = Arrays.asList("region_id", "country_id", "state_id");
        Using locationUsing = new Using(joinCols);
        RightJoin rightJoin = new RightJoin("locations", locationUsing);
        String result = rightJoin.toString();
        assertTrue(result.contains("RIGHT JOIN locations USING"));
        assertTrue(result.contains("region_id"));
        assertTrue(result.contains("country_id"));
        assertTrue(result.contains("state_id"));
    }

    // =====================================================
    // Where.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testWhere_classLevel_simple() {
        Condition condition = Filters.eq("status", "active");
        Where where = new Where(condition);
        assertEquals("WHERE status = 'active'", where.toString());
    }

    @Test
    public void testWhere_classLevel_complex() {
        Condition and = Filters.and(Filters.eq("age", 25), Filters.gt("salary", 50000));
        Where where2 = new Where(and);
        String result = where2.toString();
        assertTrue(result.startsWith("WHERE"));
        assertTrue(result.contains("age = 25"));
        assertTrue(result.contains("salary > 50000"));
    }

    // Where(Condition) constructor examples
    @Test
    public void testWhere_constructor_like() {
        Condition condition = Filters.like("name", "%John%");
        Where where = new Where(condition);
        String result = where.toString();
        assertTrue(result.startsWith("WHERE"));
        assertTrue(result.contains("name LIKE '%John%'"));
    }

    @Test
    public void testWhere_constructor_complex() {
        Condition complexCondition = Filters.or(
            Filters.and(Filters.eq("status", "active"), Filters.gt("balance", 1000)),
            Filters.eq("vip", true)
        );
        Where complexWhere = new Where(complexCondition);
        String result = complexWhere.toString();
        assertTrue(result.startsWith("WHERE"));
        assertTrue(result.contains("status = 'active'"));
        assertTrue(result.contains("balance > 1000"));
        assertTrue(result.contains("vip = true"));
    }

    @Test
    public void testWhere_constructor_withSubQuery() {
        SubQuery activeUsers = Filters.subQuery("SELECT id FROM users WHERE active = true");
        Where whereIn = new Where(Filters.in("user_id", activeUsers));
        String result = whereIn.toString();
        assertTrue(result.startsWith("WHERE"));
        assertTrue(result.contains("user_id IN"));
        assertTrue(result.contains("SELECT id FROM users WHERE active = true"));
    }

    // =====================================================
    // OrderBy.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testOrderBy_classLevel_singleColumn() {
        OrderBy orderBy1 = new OrderBy("lastName");
        assertEquals("ORDER BY lastName", orderBy1.toString());
    }

    @Test
    public void testOrderBy_classLevel_multipleColumns() {
        OrderBy orderBy2 = new OrderBy("lastName", "firstName");
        assertEquals("ORDER BY lastName, firstName", orderBy2.toString());
    }

    @Test
    public void testOrderBy_classLevel_withDirection() {
        OrderBy orderBy3 = new OrderBy("salary", SortDirection.DESC);
        assertEquals("ORDER BY salary DESC", orderBy3.toString());
    }

    @Test
    public void testOrderBy_classLevel_collectionWithDirection() {
        OrderBy orderBy4 = new OrderBy(Arrays.asList("created", "modified"), SortDirection.DESC);
        String result = orderBy4.toString();
        assertTrue(result.startsWith("ORDER BY"));
        assertTrue(result.contains("created"));
        assertTrue(result.contains("modified"));
        assertTrue(result.contains("DESC"));
    }

    @Test
    public void testOrderBy_classLevel_mapDirections() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("priority", SortDirection.DESC);
        orders.put("created", SortDirection.ASC);
        OrderBy orderBy5 = new OrderBy(orders);
        String result = orderBy5.toString();
        assertTrue(result.startsWith("ORDER BY"));
        assertTrue(result.contains("priority DESC"));
        assertTrue(result.contains("created ASC"));
    }

    // OrderBy(Condition) constructor examples
    @Test
    public void testOrderBy_constructorCondition_caseExpr() {
        Condition expr = Filters.expr("CASE WHEN status='urgent' THEN 1 ELSE 2 END");
        OrderBy orderBy = new OrderBy(expr);
        assertEquals("ORDER BY CASE WHEN status='urgent' THEN 1 ELSE 2 END", orderBy.toString());
    }

    @Test
    public void testOrderBy_constructorCondition_calculated() {
        Condition calcExpr = Filters.expr("(price * quantity) DESC");
        OrderBy totalOrder = new OrderBy(calcExpr);
        assertEquals("ORDER BY (price * quantity) DESC", totalOrder.toString());
    }

    // OrderBy(String...) constructor examples
    @Test
    public void testOrderBy_constructorVarargs_multi() {
        OrderBy orderBy = new OrderBy("country", "state", "city");
        assertEquals("ORDER BY country, state, city", orderBy.toString());
    }

    @Test
    public void testOrderBy_constructorVarargs_hierarchical() {
        OrderBy hierarchical = new OrderBy("department", "team", "lastName", "firstName");
        assertEquals("ORDER BY department, team, lastName, firstName", hierarchical.toString());
    }

    // OrderBy(String, SortDirection) constructor examples
    @Test
    public void testOrderBy_constructorWithDirection_desc() {
        OrderBy orderBy = new OrderBy("price", SortDirection.DESC);
        assertEquals("ORDER BY price DESC", orderBy.toString());
    }

    @Test
    public void testOrderBy_constructorWithDirection_asc() {
        OrderBy dateOrder = new OrderBy("created_date", SortDirection.ASC);
        assertEquals("ORDER BY created_date ASC", dateOrder.toString());
    }

    // OrderBy(Collection, SortDirection) constructor examples
    @Test
    public void testOrderBy_constructorCollectionDirection_desc() {
        List<String> dateFields = Arrays.asList("created", "updated", "published");
        OrderBy orderBy = new OrderBy(dateFields, SortDirection.DESC);
        String result = orderBy.toString();
        assertTrue(result.startsWith("ORDER BY"));
        assertTrue(result.contains("created DESC"));
        assertTrue(result.contains("updated DESC"));
        assertTrue(result.contains("published DESC"));
    }

    @Test
    public void testOrderBy_constructorCollectionDirection_asc() {
        List<String> nameFields = Arrays.asList("lastName", "firstName", "middleName");
        OrderBy nameOrder = new OrderBy(nameFields, SortDirection.ASC);
        String result = nameOrder.toString();
        assertTrue(result.startsWith("ORDER BY"));
        assertTrue(result.contains("lastName ASC"));
        assertTrue(result.contains("firstName ASC"));
        assertTrue(result.contains("middleName ASC"));
    }

    // OrderBy(Map) constructor examples
    @Test
    public void testOrderBy_constructorMap() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("isActive", SortDirection.DESC);
        orders.put("priority", SortDirection.DESC);
        orders.put("created", SortDirection.ASC);
        OrderBy orderBy = new OrderBy(orders);
        String result = orderBy.toString();
        assertTrue(result.startsWith("ORDER BY"));
        assertTrue(result.contains("isActive DESC"));
        assertTrue(result.contains("priority DESC"));
        assertTrue(result.contains("created ASC"));
    }

    // =====================================================
    // Limit.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testLimit_classLevel_countOnly() {
        Limit limit1 = new Limit(10);
        assertEquals("LIMIT 10", limit1.toString());
    }

    @Test
    public void testLimit_classLevel_withOffset() {
        Limit limit2 = new Limit(50, 20);
        assertEquals("LIMIT 20 OFFSET 50", limit2.toString());
    }

    @Test
    public void testLimit_classLevel_customExpr() {
        Limit limit3 = new Limit("10 OFFSET 20");
        assertNotNull(limit3.toString());
    }

    // Limit(int) constructor examples
    @Test
    public void testLimit_constructorInt_topFive() {
        Limit topFive = new Limit(5);
        assertEquals("LIMIT 5", topFive.toString());
    }

    @Test
    public void testLimit_constructorInt_hundred() {
        Limit searchLimit = new Limit(100);
        assertEquals("LIMIT 100", searchLimit.toString());
    }

    // Limit(int, int) constructor examples
    @Test
    public void testLimit_constructorIntInt_page1() {
        Limit page1 = new Limit(0, 10);
        // offset=0 so just LIMIT 10
        assertEquals("LIMIT 10", page1.toString());
    }

    @Test
    public void testLimit_constructorIntInt_page3() {
        Limit page3 = new Limit(20, 10);
        assertEquals("LIMIT 10 OFFSET 20", page3.toString());
    }

    @Test
    public void testLimit_constructorIntInt_products() {
        Limit products = new Limit(100, 50);
        assertEquals("LIMIT 50 OFFSET 100", products.toString());
    }

    // Limit(String) constructor examples
    @Test
    public void testLimit_constructorString_standard() {
        Limit standard = new Limit("10 OFFSET 20");
        assertNotNull(standard.getExpr());
    }

    @Test
    public void testLimit_constructorString_mysql() {
        Limit mysql = new Limit("20, 10");
        assertNotNull(mysql.getExpr());
    }

    @Test
    public void testLimit_constructorString_custom() {
        Limit custom = new Limit("FIRST 10 SKIP 20");
        assertNotNull(custom.getExpr());
    }

    // getExpr() examples
    @Test
    public void testLimit_getExpr_custom() {
        Limit customLimit = new Limit("10 OFFSET 20");
        String expr = customLimit.getExpr();
        assertNotNull(expr);
        assertTrue(expr.contains("10 OFFSET 20"));
    }

    @Test
    public void testLimit_getExpr_numeric() {
        Limit numericLimit = new Limit(20, 10);
        String noExpr = numericLimit.getExpr();
        assertNull(noExpr);
    }

    // getCount() examples
    @Test
    public void testLimit_getCount_simple() {
        Limit limit = new Limit(25);
        assertEquals(25, limit.getCount());
    }

    @Test
    public void testLimit_getCount_paged() {
        Limit paged = new Limit(100, 50);
        assertEquals(50, paged.getCount());
    }

    @Test
    public void testLimit_getCount_custom() {
        Limit custom = new Limit("10 OFFSET 20");
        assertEquals(Integer.MAX_VALUE, custom.getCount());
    }

    // getOffset() examples
    @Test
    public void testLimit_getOffset_paged() {
        Limit page3 = new Limit(20, 10);
        assertEquals(20, page3.getOffset());
    }

    @Test
    public void testLimit_getOffset_simple() {
        Limit simple = new Limit(10);
        assertEquals(0, simple.getOffset());
    }

    @Test
    public void testLimit_getOffset_custom() {
        Limit custom = new Limit("10 OFFSET 20");
        assertEquals(0, custom.getOffset());
    }

    // Limit.and(), Limit.or(), Limit.not() - exception examples
    @Test
    public void testLimit_andThrows() {
        Limit limit = new Limit(10);
        assertThrows(UnsupportedOperationException.class, () -> limit.and(Filters.eq("a", 1)));
    }

    @Test
    public void testLimit_orThrows() {
        Limit limit = new Limit(10);
        assertThrows(UnsupportedOperationException.class, () -> limit.or(Filters.eq("a", 1)));
    }

    @Test
    public void testLimit_notThrows() {
        Limit limit = new Limit(10);
        assertThrows(UnsupportedOperationException.class, () -> limit.not());
    }

    // =====================================================
    // GroupBy.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testGroupBy_classLevel_simple() {
        GroupBy byDept = new GroupBy("department");
        assertEquals("GROUP BY department", byDept.toString());
    }

    @Test
    public void testGroupBy_classLevel_multipleColumns() {
        GroupBy byLocation = new GroupBy("department", "location");
        assertEquals("GROUP BY department, location", byLocation.toString());
    }

    @Test
    public void testGroupBy_classLevel_withDirection() {
        GroupBy bySales = new GroupBy("sales_amount", SortDirection.DESC);
        assertEquals("GROUP BY sales_amount DESC", bySales.toString());
    }

    @Test
    public void testGroupBy_classLevel_mapDirections() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("department", SortDirection.ASC);
        orders.put("salary", SortDirection.DESC);
        GroupBy complex = new GroupBy(orders);
        String result = complex.toString();
        assertTrue(result.startsWith("GROUP BY"));
        assertTrue(result.contains("department ASC"));
        assertTrue(result.contains("salary DESC"));
    }

    // GroupBy(Condition) constructor examples
    @Test
    public void testGroupBy_constructorCondition_year() {
        GroupBy byYear = new GroupBy(Filters.expr("YEAR(order_date)"));
        assertEquals("GROUP BY YEAR(order_date)", byYear.toString());
    }

    @Test
    public void testGroupBy_constructorCondition_caseExpr() {
        GroupBy byRange = new GroupBy(Filters.expr("CASE WHEN age < 30 THEN 'Young' ELSE 'Senior' END"));
        assertEquals("GROUP BY CASE WHEN age < 30 THEN 'Young' ELSE 'Senior' END", byRange.toString());
    }

    // GroupBy(String...) constructor examples
    @Test
    public void testGroupBy_constructorVarargs_single() {
        GroupBy byStatus = new GroupBy("status");
        assertEquals("GROUP BY status", byStatus.toString());
    }

    @Test
    public void testGroupBy_constructorVarargs_two() {
        GroupBy byDeptLoc = new GroupBy("department", "location");
        assertEquals("GROUP BY department, location", byDeptLoc.toString());
    }

    @Test
    public void testGroupBy_constructorVarargs_three() {
        GroupBy byDimensions = new GroupBy("region", "product_category", "year");
        assertEquals("GROUP BY region, product_category, year", byDimensions.toString());
    }

    // GroupBy(String, SortDirection) constructor examples
    @Test
    public void testGroupBy_constructorWithDirection_desc() {
        GroupBy topSales = new GroupBy("sales_amount", SortDirection.DESC);
        assertEquals("GROUP BY sales_amount DESC", topSales.toString());
    }

    @Test
    public void testGroupBy_constructorWithDirection_asc() {
        GroupBy byDate = new GroupBy("order_date", SortDirection.ASC);
        assertEquals("GROUP BY order_date ASC", byDate.toString());
    }

    // GroupBy(Collection, SortDirection) constructor examples
    @Test
    public void testGroupBy_constructorCollectionDirection_desc() {
        List<String> columns = Arrays.asList("department", "location", "year");
        GroupBy allDesc = new GroupBy(columns, SortDirection.DESC);
        String result = allDesc.toString();
        assertTrue(result.startsWith("GROUP BY"));
        assertTrue(result.contains("department DESC"));
        assertTrue(result.contains("location DESC"));
        assertTrue(result.contains("year DESC"));
    }

    @Test
    public void testGroupBy_constructorCollectionDirection_asc() {
        Set<String> categories = new LinkedHashSet<>(Arrays.asList("type", "subtype"));
        GroupBy byCategory = new GroupBy(categories, SortDirection.ASC);
        String result = byCategory.toString();
        assertTrue(result.startsWith("GROUP BY"));
        assertTrue(result.contains("type ASC"));
        assertTrue(result.contains("subtype ASC"));
    }

    // GroupBy(Map) constructor examples
    @Test
    public void testGroupBy_constructorMap_complex() {
        Map<String, SortDirection> orders = new LinkedHashMap<>();
        orders.put("department", SortDirection.ASC);
        orders.put("salary_range", SortDirection.DESC);
        orders.put("hire_year", SortDirection.ASC);
        GroupBy complex = new GroupBy(orders);
        String result = complex.toString();
        assertTrue(result.startsWith("GROUP BY"));
        assertTrue(result.contains("department ASC"));
        assertTrue(result.contains("salary_range DESC"));
        assertTrue(result.contains("hire_year ASC"));
    }

    @Test
    public void testGroupBy_constructorMap_sales() {
        Map<String, SortDirection> salesGroup = new LinkedHashMap<>();
        salesGroup.put("region", SortDirection.ASC);
        salesGroup.put("total_sales", SortDirection.DESC);
        GroupBy salesAnalysis = new GroupBy(salesGroup);
        String result = salesAnalysis.toString();
        assertTrue(result.startsWith("GROUP BY"));
        assertTrue(result.contains("region ASC"));
        assertTrue(result.contains("total_sales DESC"));
    }

    // =====================================================
    // Having.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testHaving_classLevel_moreThan5() {
        Having moreThan5 = new Having(Filters.gt("COUNT(*)", 5));
        String result = moreThan5.toString();
        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains("COUNT(*)"));
        assertTrue(result.contains("> 5"));
    }

    @Test
    public void testHaving_classLevel_avgPriceHigh() {
        Having avgPriceHigh = new Having(Filters.gt("AVG(price)", 100));
        String result = avgPriceHigh.toString();
        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains("AVG(price)"));
        assertTrue(result.contains("> 100"));
    }

    @Test
    public void testHaving_classLevel_bigSpenders() {
        Having bigSpenders = new Having(Filters.gt("SUM(order_total)", 10000));
        String result = bigSpenders.toString();
        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains("SUM(order_total)"));
        assertTrue(result.contains("> 10000"));
    }

    @Test
    public void testHaving_classLevel_complex() {
        Having complex = new Having(
            Filters.and(
                Filters.gt("COUNT(*)", 10),
                Filters.lt("AVG(age)", 40),
                Filters.ge("SUM(revenue)", 50000)
            )
        );
        String result = complex.toString();
        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains("COUNT(*)"));
        assertTrue(result.contains("AVG(age)"));
        assertTrue(result.contains("SUM(revenue)"));
    }

    // Having(Condition) constructor examples
    @Test
    public void testHaving_constructor_highSales() {
        Having highSales = new Having(Filters.gt("SUM(sales)", 10000));
        String result = highSales.toString();
        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains("SUM(sales) > 10000"));
    }

    @Test
    public void testHaving_constructor_minCount() {
        Having minCount = new Having(Filters.ge("COUNT(*)", 3));
        String result = minCount.toString();
        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains("COUNT(*) >= 3"));
    }

    @Test
    public void testHaving_constructor_avgRange() {
        Having avgRange = new Having(
            Filters.and(
                Filters.ge("AVG(score)", 60),
                Filters.le("AVG(score)", 90)
            )
        );
        String result = avgRange.toString();
        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains("AVG(score) >= 60"));
        assertTrue(result.contains("AVG(score) <= 90"));
    }

    @Test
    public void testHaving_constructor_maxCheck() {
        Having maxCheck = new Having(Filters.lt("MAX(temperature)", 100));
        String result = maxCheck.toString();
        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains("MAX(temperature) < 100"));
    }

    @Test
    public void testHaving_constructor_multipleAggs() {
        Having multipleAggs = new Having(
            Filters.and(
                Filters.gt("COUNT(DISTINCT customer_id)", 10),
                Filters.ge("SUM(amount)", 5000),
                Filters.between("AVG(rating)", 3.0, 5.0)
            )
        );
        String result = multipleAggs.toString();
        assertTrue(result.startsWith("HAVING"));
        assertTrue(result.contains("COUNT(DISTINCT customer_id)"));
        assertTrue(result.contains("SUM(amount)"));
        assertTrue(result.contains("AVG(rating)"));
        assertTrue(result.contains("BETWEEN"));
    }

    // =====================================================
    // SubQuery.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testSubQuery_classLevel_rawSql() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users WHERE status = 'active'");
        assertEquals("SELECT id FROM users WHERE status = 'active'", subQuery1.toString());
    }

    @Test
    public void testSubQuery_classLevel_structured() {
        Condition activeCondition = Filters.eq("status", "active");
        SubQuery subQuery2 = Filters.subQuery("users", Arrays.asList("id"), activeCondition);
        String result = subQuery2.toString();
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("id"));
        assertTrue(result.contains("FROM"));
        assertTrue(result.contains("users"));
        assertTrue(result.contains("status = 'active'"));
    }

    @Test
    public void testSubQuery_classLevel_inCondition() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users WHERE status = 'active'");
        Condition inCondition = Filters.in("userId", subQuery1);
        String result = inCondition.toString();
        assertTrue(result.contains("userId IN"));
        assertTrue(result.contains("SELECT id FROM users WHERE status = 'active'"));
    }

    // SubQuery(String sql) constructor examples
    @Test
    public void testSubQuery_constructorSql_simple() {
        SubQuery subQuery = Filters.subQuery("SELECT MAX(salary) FROM employees");
        assertEquals("SELECT MAX(salary) FROM employees", subQuery.toString());
    }

    @Test
    public void testSubQuery_constructorSql_complex() {
        SubQuery complexQuery = Filters.subQuery(
            "SELECT u.id FROM users u " +
            "INNER JOIN orders o ON u.id = o.user_id " +
            "WHERE o.total > 1000 " +
            "GROUP BY u.id HAVING COUNT(o.id) > 5"
        );
        assertNotNull(complexQuery.toString());
    }

    // SubQuery(String entityName, String sql) constructor examples
    @Test
    public void testSubQuery_constructorEntityNameSql() {
        SubQuery subQuery = Filters.subQuery("orders",
            "SELECT order_id FROM orders WHERE total > 1000");
        assertEquals("SELECT order_id FROM orders WHERE total > 1000", subQuery.toString());
        assertEquals("orders", subQuery.getEntityName());
    }

    // SubQuery(String entityName, Collection, Condition) constructor examples
    @Test
    public void testSubQuery_constructorEntityNamePropsCondition() {
        List<String> props = Arrays.asList("id", "email");
        Condition condition = Filters.and(
            Filters.eq("active", true),
            Filters.gt("created", "2024-01-01")
        );
        SubQuery subQuery = Filters.subQuery("users", props, condition);
        String result = subQuery.toString();
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("id"));
        assertTrue(result.contains("email"));
        assertTrue(result.contains("FROM"));
        assertTrue(result.contains("users"));
        assertTrue(result.contains("active = true"));
        assertTrue(result.contains("created > '2024-01-01'"));
    }

    // getSql() examples
    @Test
    public void testSubQuery_getSql_raw() {
        SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE status = 'active'");
        String sql = rawQuery.getSql();
        assertEquals("SELECT id FROM users WHERE status = 'active'", sql);
    }

    @Test
    public void testSubQuery_getSql_structured() {
        SubQuery structured = new SubQuery("users", Arrays.asList("id"), Filters.eq("status", "active"));
        String structuredSql = structured.getSql();
        assertNull(structuredSql);
    }

    // getEntityName() examples
    @Test
    public void testSubQuery_getEntityName_structured() {
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), Filters.eq("active", true));
        assertEquals("users", subQuery.getEntityName());
    }

    @Test
    public void testSubQuery_getEntityName_rawWithEntity() {
        SubQuery rawQuery = new SubQuery("orders", "SELECT order_id FROM orders WHERE total > 1000");
        assertEquals("orders", rawQuery.getEntityName());
    }

    @Test
    public void testSubQuery_getEntityName_rawSimple() {
        SubQuery simpleRaw = new SubQuery("SELECT id FROM users");
        assertEquals("", simpleRaw.getEntityName());
    }

    // getEntityClass() examples
    @Test
    public void testSubQuery_getEntityClass_withString() {
        SubQuery namedQuery = new SubQuery("products", Arrays.asList("id"), Filters.eq("active", true));
        assertNull(namedQuery.getEntityClass());
    }

    // getSelectPropNames() examples
    @Test
    public void testSubQuery_getSelectPropNames_structured() {
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id", "email", "name"), Filters.eq("active", true));
        Collection<String> propNames = subQuery.getSelectPropNames();
        assertNotNull(propNames);
        assertEquals(3, propNames.size());
        assertTrue(propNames.contains("id"));
        assertTrue(propNames.contains("email"));
        assertTrue(propNames.contains("name"));
    }

    @Test
    public void testSubQuery_getSelectPropNames_raw() {
        SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE active = true");
        assertNull(rawQuery.getSelectPropNames());
    }

    // getCondition() examples
    @Test
    public void testSubQuery_getCondition_structured() {
        Condition activeCondition = Filters.eq("active", true);
        SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), activeCondition);
        Condition condition = subQuery.getCondition();
        assertNotNull(condition);
        assertTrue(condition.toString().contains("active = true"));
    }

    @Test
    public void testSubQuery_getCondition_raw() {
        SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE active = true");
        assertNull(rawQuery.getCondition());
    }

    // =====================================================
    // All.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testAll_classLevel_electronics() {
        SubQuery electronicsQuery = Filters.subQuery(
            "SELECT price FROM products WHERE category = 'Electronics'"
        );
        All allElectronics = new All(electronicsQuery);
        String result = allElectronics.toString();
        assertTrue(result.startsWith("ALL"));
        assertTrue(result.contains("SELECT price FROM products WHERE category = 'Electronics'"));
    }

    @Test
    public void testAll_classLevel_managerSalaries() {
        SubQuery managerSalaries = Filters.subQuery(
            "SELECT salary FROM employees WHERE is_manager = true"
        );
        All allManagers = new All(managerSalaries);
        String result = allManagers.toString();
        assertTrue(result.startsWith("ALL"));
        assertTrue(result.contains("SELECT salary FROM employees WHERE is_manager = true"));
    }

    // All(SubQuery) constructor examples
    @Test
    public void testAll_constructor_competitorPrices() {
        SubQuery competitorPrices = Filters.subQuery(
            "SELECT price FROM competitor_products WHERE product_type = 'Premium'"
        );
        All allCompetitors = new All(competitorPrices);
        assertNotNull(allCompetitors.toString());
    }

    @Test
    public void testAll_constructor_classAverages() {
        SubQuery classAverages = Filters.subQuery(
            "SELECT avg_score FROM class_statistics WHERE year = 2024"
        );
        All allAverages = new All(classAverages);
        assertNotNull(allAverages.toString());
    }

    @Test
    public void testAll_constructor_premiumPrices() {
        SubQuery premiumPrices = Filters.subQuery("SELECT price FROM products WHERE category = 'premium'");
        All allPremium = new All(premiumPrices);
        assertNotNull(allPremium.toString());
    }

    // =====================================================
    // Any.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testAny_classLevel_electronics() {
        SubQuery electronicsQuery = Filters.subQuery(
            "SELECT price FROM products WHERE category = 'Electronics'"
        );
        Any anyPrice = new Any(electronicsQuery);
        String result = anyPrice.toString();
        assertTrue(result.startsWith("ANY"));
        assertTrue(result.contains("SELECT price FROM products WHERE category = 'Electronics'"));
    }

    @Test
    public void testAny_classLevel_managerSalaries() {
        SubQuery managerSalaries = Filters.subQuery(
            "SELECT salary FROM employees WHERE is_manager = true"
        );
        Any anyManagerSalary = new Any(managerSalaries);
        assertNotNull(anyManagerSalary.toString());
    }

    // Any(SubQuery) constructor examples
    @Test
    public void testAny_constructor_budgets() {
        SubQuery budgetQuery = Filters.subQuery(
            "SELECT budget FROM departments WHERE region = 'West'"
        );
        Any anyBudget = new Any(budgetQuery);
        assertNotNull(anyBudget.toString());
    }

    @Test
    public void testAny_constructor_passingScores() {
        SubQuery passingScores = Filters.subQuery(
            "SELECT passing_score FROM exams WHERE subject = 'Math'"
        );
        Any anyPassingScore = new Any(passingScores);
        assertNotNull(anyPassingScore.toString());
    }

    @Test
    public void testAny_constructor_competitorPrices() {
        SubQuery competitorPrices = Filters.subQuery("SELECT price FROM competitor_products");
        Any anyPrice = new Any(competitorPrices);
        assertNotNull(anyPrice.toString());
    }

    // =====================================================
    // Some.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testSome_classLevel_managerSalaries() {
        SubQuery managerSalaries = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
        Some someCondition = new Some(managerSalaries);
        String result = someCondition.toString();
        assertTrue(result.startsWith("SOME"));
        assertTrue(result.contains("SELECT salary FROM employees WHERE role = 'manager'"));
    }

    @Test
    public void testSome_classLevel_competitorPrices() {
        SubQuery competitorPrices = Filters.subQuery("SELECT price FROM competitor_products");
        Some somePrice = new Some(competitorPrices);
        assertNotNull(somePrice.toString());
    }

    // Some(SubQuery) constructor examples
    @Test
    public void testSome_constructor_deptBudgets() {
        SubQuery deptBudgets = Filters.subQuery("SELECT budget FROM departments");
        Some someCondition = new Some(deptBudgets);
        assertNotNull(someCondition.toString());
    }

    @Test
    public void testSome_constructor_managerSalaries() {
        SubQuery managerSalaries = Filters.subQuery("SELECT salary FROM employees WHERE is_manager = true");
        Some someManagerSalary = new Some(managerSalaries);
        assertNotNull(someManagerSalary.toString());
    }

    @Test
    public void testSome_constructor_thresholds() {
        SubQuery thresholds = Filters.subQuery("SELECT threshold FROM order_levels");
        Some someThreshold = new Some(thresholds);
        assertNotNull(someThreshold.toString());
    }

    // =====================================================
    // Union.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testUnion_classLevel_laCustomers() {
        SubQuery laCustomers = Filters.subQuery("SELECT id, name FROM customers WHERE city='LA'");
        Union union = new Union(laCustomers);
        String result = union.toString();
        assertTrue(result.startsWith("UNION"));
        assertTrue(result.contains("SELECT id, name FROM customers WHERE city='LA'"));
    }

    @Test
    public void testUnion_classLevel_inactiveUsers() {
        SubQuery inactiveUsers = Filters.subQuery("SELECT user_id, email FROM inactive_users");
        Union allUsers = new Union(inactiveUsers);
        assertNotNull(allUsers.toString());
    }

    @Test
    public void testUnion_classLevel_pastOrders() {
        SubQuery pastOrders = Filters.subQuery("SELECT order_id, customer_id FROM orders WHERE year = 2023");
        Union allOrders = new Union(pastOrders);
        assertNotNull(allOrders.toString());
    }

    // Union(SubQuery) constructor examples
    @Test
    public void testUnion_constructor_eastCustomers() {
        SubQuery eastCustomers = Filters.subQuery("SELECT customer_id, name FROM customers WHERE region = 'East'");
        Union union = new Union(eastCustomers);
        assertNotNull(union.toString());
    }

    @Test
    public void testUnion_constructor_inactive() {
        SubQuery inactiveUsers = Filters.subQuery("SELECT user_id, email FROM inactive_users");
        Union allUsers = new Union(inactiveUsers);
        assertNotNull(allUsers.toString());
    }

    @Test
    public void testUnion_constructor_historical() {
        SubQuery historicalOrders = Filters.subQuery("SELECT order_id, total FROM archived_orders");
        Union allOrders = new Union(historicalOrders);
        assertNotNull(allOrders.toString());
    }

    // =====================================================
    // UnionAll.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testUnionAll_classLevel_archived() {
        SubQuery archivedTransactions = Filters.subQuery("SELECT * FROM archived_transactions WHERE year = 2024");
        UnionAll unionAll = new UnionAll(archivedTransactions);
        String result = unionAll.toString();
        assertTrue(result.startsWith("UNION ALL"));
        assertTrue(result.contains("SELECT * FROM archived_transactions WHERE year = 2024"));
    }

    @Test
    public void testUnionAll_classLevel_inactive() {
        SubQuery inactiveUsers = Filters.subQuery("SELECT id, name, 'inactive' as status FROM inactive_users");
        UnionAll allUsers = new UnionAll(inactiveUsers);
        assertNotNull(allUsers.toString());
    }

    // UnionAll(SubQuery) constructor examples
    @Test
    public void testUnionAll_constructor_eastOrders() {
        SubQuery eastOrders = Filters.subQuery("SELECT order_id, amount FROM orders WHERE region = 'EAST'");
        UnionAll allOrders = new UnionAll(eastOrders);
        assertNotNull(allOrders.toString());
    }

    @Test
    public void testUnionAll_constructor_archivedTxns() {
        SubQuery archivedTxns = Filters.subQuery("SELECT txn_id, date, amount FROM archived_transactions");
        UnionAll allTxns = new UnionAll(archivedTxns);
        assertNotNull(allTxns.toString());
    }

    @Test
    public void testUnionAll_constructor_quarterly() {
        SubQuery q1Data = Filters.subQuery("SELECT * FROM sales_q1");
        SubQuery q2Data = Filters.subQuery("SELECT * FROM sales_q2");
        UnionAll allSales = new UnionAll(q2Data);
        assertNotNull(allSales.toString());
        // q1Data is also valid independently
        assertNotNull(q1Data.toString());
    }

    // =====================================================
    // Intersect.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testIntersect_classLevel_recentOrders() {
        SubQuery recentOrders = Filters.subQuery("SELECT customer_id FROM orders WHERE order_date > '2023-01-01'");
        Intersect intersect = new Intersect(recentOrders);
        String result = intersect.toString();
        assertTrue(result.startsWith("INTERSECT"));
        assertTrue(result.contains("SELECT customer_id FROM orders WHERE order_date > '2023-01-01'"));
    }

    @Test
    public void testIntersect_classLevel_onSale() {
        SubQuery onSale = Filters.subQuery("SELECT product_id FROM promotions WHERE discount > 0");
        Intersect commonProducts = new Intersect(onSale);
        assertNotNull(commonProducts.toString());
    }

    // Intersect(SubQuery) constructor examples
    @Test
    public void testIntersect_constructor_activeUsers() {
        SubQuery activeUsers = Filters.subQuery("SELECT user_id FROM activity WHERE last_login > CURRENT_DATE - 30");
        Intersect premiumActive = new Intersect(activeUsers);
        assertNotNull(premiumActive.toString());
    }

    @Test
    public void testIntersect_constructor_projectB() {
        SubQuery projectB = Filters.subQuery("SELECT employee_id FROM assignments WHERE project = 'B'");
        Intersect bothProjects = new Intersect(projectB);
        assertNotNull(bothProjects.toString());
    }

    @Test
    public void testIntersect_constructor_skills() {
        SubQuery position2Skills = Filters.subQuery("SELECT skill_id FROM position_skills WHERE position_id = 2");
        Intersect commonSkills = new Intersect(position2Skills);
        assertNotNull(commonSkills.toString());
    }

    @Test
    public void testIntersect_constructor_promotions() {
        SubQuery onPromotion = Filters.subQuery("SELECT product_id FROM promotions WHERE active = true");
        Intersect availablePromotions = new Intersect(onPromotion);
        assertNotNull(availablePromotions.toString());
    }

    // =====================================================
    // Except.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testExcept_classLevel_customersWithoutOrders() {
        SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Except customersWithoutOrders = new Except(customersWithOrders);
        String result = customersWithoutOrders.toString();
        assertTrue(result.startsWith("EXCEPT"));
        assertTrue(result.contains("SELECT DISTINCT customer_id FROM orders"));
    }

    @Test
    public void testExcept_classLevel_unsoldProducts() {
        SubQuery recentlySold = Filters.subQuery(
            "SELECT product_id FROM order_items WHERE order_date > DATE_SUB(NOW(), INTERVAL 1 MONTH)"
        );
        Except unsoldProducts = new Except(recentlySold);
        assertNotNull(unsoldProducts.toString());
    }

    // Except(SubQuery) constructor examples
    @Test
    public void testExcept_constructor_notManagers() {
        SubQuery managers = Filters.subQuery("SELECT employee_id FROM employees WHERE is_manager = true");
        Except notManagers = new Except(managers);
        assertNotNull(notManagers.toString());
    }

    @Test
    public void testExcept_constructor_customersWithoutOrders() {
        SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Except customersWithoutOrders = new Except(customersWithOrders);
        assertNotNull(customersWithoutOrders.toString());
    }

    @Test
    public void testExcept_constructor_otherSkills() {
        SubQuery requiredSkills = Filters.subQuery("SELECT skill_id FROM job_requirements WHERE job_id = 123");
        Except otherSkills = new Except(requiredSkills);
        assertNotNull(otherSkills.toString());
    }

    @Test
    public void testExcept_constructor_unsoldProducts() {
        SubQuery soldProducts = Filters.subQuery("SELECT product_id FROM sales");
        Except unsoldProducts = new Except(soldProducts);
        assertNotNull(unsoldProducts.toString());
    }

    // =====================================================
    // Minus.java - class-level Javadoc examples
    // =====================================================

    @Test
    public void testMinus_classLevel_customersWithoutOrders() {
        SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Minus customersWithoutOrders = new Minus(customersWithOrders);
        String result = customersWithoutOrders.toString();
        assertTrue(result.startsWith("MINUS"));
        assertTrue(result.contains("SELECT DISTINCT customer_id FROM orders"));
    }

    @Test
    public void testMinus_classLevel_soldProducts() {
        SubQuery soldProducts = Filters.subQuery(
            "SELECT DISTINCT product_id FROM order_items WHERE order_date > DATE_SUB(NOW(), INTERVAL 1 MONTH)"
        );
        Minus unsoldProducts = new Minus(soldProducts);
        assertNotNull(unsoldProducts.toString());
    }

    @Test
    public void testMinus_classLevel_assignedEmployees() {
        SubQuery assignedEmployees = Filters.subQuery("SELECT DISTINCT employee_id FROM project_assignments");
        Minus unassignedEmployees = new Minus(assignedEmployees);
        assertNotNull(unassignedEmployees.toString());
    }

    // Minus(SubQuery) constructor examples
    @Test
    public void testMinus_constructor_unsoldProducts() {
        SubQuery soldProducts = Filters.subQuery("SELECT product_id FROM sales");
        Minus unsoldProducts = new Minus(soldProducts);
        assertNotNull(unsoldProducts.toString());
    }

    @Test
    public void testMinus_constructor_unassigned() {
        SubQuery assignedEmployees = Filters.subQuery("SELECT employee_id FROM project_assignments");
        Minus unassigned = new Minus(assignedEmployees);
        assertNotNull(unassigned.toString());
    }

    @Test
    public void testMinus_constructor_customersWithoutOrders() {
        SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders");
        Minus customersWithoutOrders = new Minus(customersWithOrders);
        assertNotNull(customersWithoutOrders.toString());
    }

    @Test
    public void testMinus_constructor_optionalSkills() {
        SubQuery requiredSkills = Filters.subQuery("SELECT skill_id FROM position_requirements WHERE position_id = 5");
        Minus optionalSkills = new Minus(requiredSkills);
        assertNotNull(optionalSkills.toString());
    }
}
