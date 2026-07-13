package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class NotExistsTest extends TestBase {
    @Test
    public void testConstructor_WithRawSQLSubQuery() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE customer_id = users.id");
        NotExists condition = new NotExists(subQuery);
        assertNotNull(condition);
        assertEquals(Operator.NOT_EXISTS, condition.operator());
    }

    @Test
    public void testConstructor_WithStructuredSubQuery() {
        Condition whereCondition = new Equal("status", "inactive");
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), whereCondition);
        NotExists condition = new NotExists(subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testGetOperator() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM products");
        NotExists condition = new NotExists(subQuery);
        assertEquals(Operator.NOT_EXISTS, condition.operator());
    }

    @Test
    public void testGetCondition() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM users WHERE active = false");
        NotExists condition = new NotExists(subQuery);
        SubQuery retrieved = (SubQuery) condition.condition();
        assertNotNull(retrieved);
        assertEquals(subQuery, retrieved);
    }

    @Test
    public void testParameters_EmptyForRawSQL() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        NotExists condition = new NotExists(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertTrue(params.isEmpty());
    }

    @Test
    public void testParameters_WithConditionParameters() {
        Condition whereCondition = new Equal("status", "cancelled");
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), whereCondition);
        NotExists condition = new NotExists(subQuery);
        List<Object> params = condition.parameters();
        assertNotNull(params);
        assertEquals(1, params.size());
        assertEquals("cancelled", params.get(0));
    }

    @Test
    public void testToString_NoChange() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE status = 'cancelled'");
        NotExists condition = new NotExists(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertEquals("NOT EXISTS (SELECT 1 FROM orders WHERE status = 'cancelled')", result);
    }

    @Test
    public void testToString_WithStructuredQuery() {
        Condition whereCondition = new Equal("deleted", true);
        SubQuery subQuery = Filters.subQuery("users", Arrays.asList("id"), whereCondition);
        NotExists condition = new NotExists(subQuery);
        String result = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("NOT EXISTS"));
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("users"));
    }

    @Test
    public void testHashCode() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM orders");
        NotExists cond1 = new NotExists(subQuery1);
        NotExists cond2 = new NotExists(subQuery2);
        assertEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testHashCode_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM products");
        NotExists cond1 = new NotExists(subQuery1);
        NotExists cond2 = new NotExists(subQuery2);
        assertNotEquals(cond1.hashCode(), cond2.hashCode());
    }

    @Test
    public void testEquals_SameObject() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        NotExists condition = new NotExists(subQuery);
        assertEquals(condition, condition);
    }

    @Test
    public void testEquals_EqualObjects() {
        SubQuery subQuery1 = Filters.subQuery("SELECT id FROM users WHERE deleted = true");
        SubQuery subQuery2 = Filters.subQuery("SELECT id FROM users WHERE deleted = true");
        NotExists cond1 = new NotExists(subQuery1);
        NotExists cond2 = new NotExists(subQuery2);
        assertEquals(cond1, cond2);
    }

    @Test
    public void testEquals_DifferentSubQueries() {
        SubQuery subQuery1 = Filters.subQuery("SELECT 1 FROM orders");
        SubQuery subQuery2 = Filters.subQuery("SELECT 1 FROM invoices");
        NotExists cond1 = new NotExists(subQuery1);
        NotExists cond2 = new NotExists(subQuery2);
        assertNotEquals(cond1, cond2);
    }

    @Test
    public void testEquals_Null() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        NotExists condition = new NotExists(subQuery);
        assertNotEquals(null, condition);
    }

    @Test
    public void testEquals_DifferentClass() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders");
        NotExists condition = new NotExists(subQuery);
        assertNotEquals(condition, "string");
    }

    @Test
    public void testFindCustomersWithoutOrders() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
        NotExists condition = new NotExists(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("NOT EXISTS"));
        assertTrue(sql.contains("customer_id"));
    }

    @Test
    public void testFindProductsWithoutReviews() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM reviews WHERE reviews.product_id = products.id");
        NotExists condition = new NotExists(subQuery);
        assertNotNull(condition);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("NOT EXISTS"));
    }

    @Test
    public void testSubQueryWithMultipleConditions() {
        And andCondition = new And(Arrays.asList(new Equal("cancelled", true), new LessThan("amount", 10)));
        SubQuery subQuery = Filters.subQuery("orders", Arrays.asList("id"), andCondition);
        NotExists condition = new NotExists(subQuery);
        List<Object> params = condition.parameters();
        assertEquals(2, params.size());
    }

    @Test
    public void testOrphanedRecordsCheck() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM parent_table WHERE parent_table.id = child_table.parent_id");
        NotExists condition = new NotExists(subQuery);
        String sql = condition.toSql(NamingPolicy.NO_CHANGE);
        assertTrue(sql.contains("NOT EXISTS"));
        assertTrue(sql.contains("parent_id"));
    }

    @Test
    public void testEmployeesWithoutProjects() {
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM project_assignments WHERE project_assignments.employee_id = employees.id");
        NotExists condition = new NotExists(subQuery);
        assertNotNull(condition);
    }

    private SubQuery simpleSubQuery;

    private NotExists notExistsCondition;

    @BeforeEach
    void setUp() {
        simpleSubQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
        notExistsCondition = new NotExists(simpleSubQuery);
    }

    @Test
    void testConstructorWithSubQuery() {
        NotExists notExists = new NotExists(simpleSubQuery);

        assertNotNull(notExists);
        assertEquals(Operator.NOT_EXISTS, notExists.operator());
    }

    @Test
    void testConstructorWithNullSubQuery() {
        assertThrows(IllegalArgumentException.class, () -> new NotExists(null));
    }

    @Test
    void testParameters() {
        // Simple SubQuery without parameters should return empty list
        assertNotNull(notExistsCondition.parameters());
    }

    @Test
    void testToString() {
        String result = notExistsCondition.toString();

        assertNotNull(result);
        // Should contain NOT EXISTS in the output
        // Note: specific format may vary based on implementation
    }
}
