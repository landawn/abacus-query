package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.SQLBuilder;
import com.landawn.abacus.query.Selection;

public class SelectionTest extends TestBase {

    static class User {
        private Long id;
        private String name;
        private String email;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    static class Order {
        private Long id;
        private String orderDate;
        private Double total;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
        public Double getTotal() { return total; }
        public void setTotal(Double total) { this.total = total; }
    }

    static class Product {
        private Long id;
        private String name;
        private Double price;
        private Double cost;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Double getCost() { return cost; }
        public void setCost(Double cost) { this.cost = cost; }
    }

    @Test
    public void testSelectionConstructorsAndAccessors() {
        // Test no-args constructor
        Selection selection = new Selection();
        assertNull(selection.entityClass());
        assertNull(selection.tableAlias());
        assertNull(selection.classAlias());
        assertNull(selection.selectPropNames());
        assertFalse(selection.includeSubEntityProperties());
        assertNull(selection.excludedPropNames());
        
        // Test fluent setters
        Set<String> excludedProps = new HashSet<>(Arrays.asList("internalField"));
        selection.entityClass(User.class)
            .tableAlias("u")
            .classAlias("user")
            .selectPropNames(Arrays.asList("id", "name"))
            .includeSubEntityProperties(true)
            .excludedPropNames(excludedProps);
        
        assertEquals(User.class, selection.entityClass());
        assertEquals("u", selection.tableAlias());
        assertEquals("user", selection.classAlias());
        assertEquals(Arrays.asList("id", "name"), selection.selectPropNames());
        assertTrue(selection.includeSubEntityProperties());
        assertEquals(excludedProps, selection.excludedPropNames());
        
        // Test all-args constructor
        Selection selection2 = new Selection(
            Order.class,
            "o",
            "order",
            Arrays.asList("id", "orderDate"),
            false,
            new HashSet<>(Arrays.asList("internalNote"))
        );
        
        assertEquals(Order.class, selection2.entityClass());
        assertEquals("o", selection2.tableAlias());
        assertEquals("order", selection2.classAlias());
        assertEquals(Arrays.asList("id", "orderDate"), selection2.selectPropNames());
        assertFalse(selection2.includeSubEntityProperties());
        assertEquals(new HashSet<>(Arrays.asList("internalNote")), selection2.excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilder() {
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder();
        assertNotNull(builder);
        
        // Test add(Class)
        builder.add(User.class);
        
        // Test add(Class, Collection)
        builder.add(Order.class, Arrays.asList("id", "orderDate", "total"));
        
        // Test add(Class, String, String)
        builder.add(Product.class, "p", "product");
        
        // Test add(Class, String, String, Collection)
        builder.add(User.class, "u2", "user2", Arrays.asList("name", "email"));
        
        // Test add(Class, boolean, Set)
        builder.add(Order.class, true, new HashSet<>(Arrays.asList("internalField")));
        
        // Test add(Class, String, String, boolean, Set)
        builder.add(Product.class, "p2", "product2", false, new HashSet<>(Arrays.asList("cost")));
        
        // Build and verify
        List<Selection> selections = builder.build();
        assertEquals(6, selections.size());
        
        // Verify first selection (simple class)
        Selection sel1 = selections.get(0);
        assertEquals(User.class, sel1.entityClass());
        assertNull(sel1.tableAlias());
        assertNull(sel1.classAlias());
        assertNull(sel1.selectPropNames());
        assertFalse(sel1.includeSubEntityProperties());
        assertNull(sel1.excludedPropNames());
        
        // Verify second selection (with properties)
        Selection sel2 = selections.get(1);
        assertEquals(Order.class, sel2.entityClass());
        assertEquals(Arrays.asList("id", "orderDate", "total"), sel2.selectPropNames());
        
        // Verify third selection (with aliases)
        Selection sel3 = selections.get(2);
        assertEquals(Product.class, sel3.entityClass());
        assertEquals("p", sel3.tableAlias());
        assertEquals("product", sel3.classAlias());
        
        // Verify fourth selection (full config)
        Selection sel4 = selections.get(3);
        assertEquals(User.class, sel4.entityClass());
        assertEquals("u2", sel4.tableAlias());
        assertEquals("user2", sel4.classAlias());
        assertEquals(Arrays.asList("name", "email"), sel4.selectPropNames());
        
        // Verify fifth selection (with sub-entities and exclusions)
        Selection sel5 = selections.get(4);
        assertEquals(Order.class, sel5.entityClass());
        assertTrue(sel5.includeSubEntityProperties());
        assertEquals(new HashSet<>(Arrays.asList("internalField")), sel5.excludedPropNames());
        
        // Verify sixth selection (complete config)
        Selection sel6 = selections.get(5);
        assertEquals(Product.class, sel6.entityClass());
        assertEquals("p2", sel6.tableAlias());
        assertEquals("product2", sel6.classAlias());
        assertFalse(sel6.includeSubEntityProperties());
        assertEquals(new HashSet<>(Arrays.asList("cost")), sel6.excludedPropNames());
    }

    @Test
    public void testMultiSelectionBuilderApply() {
        // Create a mock function to test apply
        List<Selection> capturedSelections = null;
        
        SQLBuilder result = Selection.multiSelectionBuilder()
            .add(User.class, "u", "user")
            .add(Order.class, "o", "order")
            .apply(selections -> {
                // Capture selections for verification
                assertNotNull(selections);
                assertEquals(2, selections.size());
                return new SQLBuilder.PSC();
            });
        
        assertNotNull(result);
        assertTrue(result instanceof SQLBuilder);
    }

    @Test
    public void testMultiSelectionBuilderChaining() {
        // Test that all add methods return the builder for chaining
        Selection.MultiSelectionBuilder builder = Selection.multiSelectionBuilder();
        
        Selection.MultiSelectionBuilder result = builder
            .add(User.class)
            .add(Order.class, Arrays.asList("id", "total"))
            .add(Product.class, "p", "product")
            .add(User.class, "u", "user", Arrays.asList("name"))
            .add(Order.class, true, new HashSet<>())
            .add(Product.class, "p2", "product2", false, new HashSet<>());
        
        assertSame(builder, result);
        
        List<Selection> selections = result.build();
        assertEquals(6, selections.size());
    }
}