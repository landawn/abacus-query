package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.condition.Condition;
import com.landawn.abacus.condition.OrderBy;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;

public class QueryBeanTest extends TestBase {

    static class TestEntity {
        private String name;
        private Integer age;
        private String status;
        private String role;
        private Double salary;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Double getSalary() { return salary; }
        public void setSalary(Double salary) { this.salary = salary; }
    }

    @Test
    public void testQueryBeanFluentApi() {
        QueryBean query = new QueryBean()
            .select(Arrays.asList("id", "name", "email"))
            .from("users")
            .distinct(true)
            .offset(10)
            .limit(20);
        
        assertEquals(Arrays.asList("id", "name", "email"), query.select());
        assertEquals("users", query.from());
        assertTrue(query.distinct());
        assertEquals(10, query.offset());
        assertEquals(20, query.limit());
    }

    @Test
    public void testFilterBeanToCondition() {
        // Test simple equals condition
        QueryBean.FilterBean filter = new QueryBean.FilterBean()
            .fieldName("status")
            .operator(QueryBean.Operator.equals)
            .parameter("active");
        
        Condition condition = filter.toCondition();
        assertNotNull(condition);
        
        // Test with entity class for type conversion
        filter = new QueryBean.FilterBean()
            .fieldName("age")
            .operator(QueryBean.Operator.greater_than)
            .parameter("25");
        
        condition = filter.toCondition(TestEntity.class);
        assertNotNull(condition);
        
        // Test with explicit field type
        filter = new QueryBean.FilterBean()
            .fieldName("salary")
            .fieldType("Double")
            .operator(QueryBean.Operator.less_than)
            .parameter("50000.0");
        
        condition = filter.toCondition();
        assertNotNull(condition);
    }

    @Test
    public void testFilterBeanComplexConditions() {
        // Test AND conditions
        QueryBean.FilterBean mainFilter = new QueryBean.FilterBean()
            .fieldName("status")
            .operator(QueryBean.Operator.equals)
            .parameter("active");
        
        QueryBean.FilterBean andFilter = new QueryBean.FilterBean()
            .fieldName("role")
            .operator(QueryBean.Operator.in)
            .parameter("[\"admin\", \"moderator\"]");
        
        mainFilter.and(Arrays.asList(andFilter));
        
        Condition condition = mainFilter.toCondition();
        assertNotNull(condition);
        
        // Test OR conditions
        QueryBean.FilterBean orMainFilter = new QueryBean.FilterBean()
            .fieldName("age")
            .operator(QueryBean.Operator.greater_than)
            .parameter("30");
        
        QueryBean.FilterBean orFilter = new QueryBean.FilterBean()
            .fieldName("role")
            .operator(QueryBean.Operator.equals)
            .parameter("senior");
        
        orMainFilter.or(Arrays.asList(orFilter));
        
        condition = orMainFilter.toCondition();
        assertNotNull(condition);
        
        // Test that both AND and OR cannot be set
        QueryBean.FilterBean invalidFilter = new QueryBean.FilterBean()
            .fieldName("test")
            .operator(QueryBean.Operator.equals)
            .parameter("value")
            .and(Arrays.asList(andFilter))
            .or(Arrays.asList(orFilter));
        
        assertThrows(IllegalArgumentException.class, () -> invalidFilter.toCondition());
    }

    @Test
    public void testAllOperators() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestEntity.class);
        
        // Test each operator
        testOperator(QueryBean.Operator.equals, "name", "John", beanInfo);
        testOperator(QueryBean.Operator.not_equals, "name", "Jane", beanInfo);
        testOperator(QueryBean.Operator.greater_than, "age", "25", beanInfo);
        testOperator(QueryBean.Operator.greater_than_or_equals, "age", "25", beanInfo);
        testOperator(QueryBean.Operator.less_than, "age", "30", beanInfo);
        testOperator(QueryBean.Operator.less_than_or_equals, "age", "30", beanInfo);
        testOperator(QueryBean.Operator.starts_with, "name", "Jo", beanInfo);
        testOperator(QueryBean.Operator.not_starts_with, "name", "Ja", beanInfo);
        testOperator(QueryBean.Operator.ends_with, "name", "hn", beanInfo);
        testOperator(QueryBean.Operator.not_ends_with, "name", "ne", beanInfo);
        testOperator(QueryBean.Operator.contains, "name", "oh", beanInfo);
        testOperator(QueryBean.Operator.not_contains, "name", "xy", beanInfo);
        testOperator(QueryBean.Operator.in, "role", "[\"admin\", \"user\"]", beanInfo);
        testOperator(QueryBean.Operator.not_in, "role", "[\"guest\", \"banned\"]", beanInfo);
        testOperator(QueryBean.Operator.between, "age", "[20, 30]", beanInfo);
        testOperator(QueryBean.Operator.not_between, "age", "[40, 50]", beanInfo);
        
        // Test alias operators
        testOperator(QueryBean.Operator.not_less_than, "age", "25", beanInfo);
        testOperator(QueryBean.Operator.not_greater_than, "age", "30", beanInfo);
        testOperator(QueryBean.Operator.before, "age", "30", beanInfo);
        testOperator(QueryBean.Operator.after, "age", "25", beanInfo);
        testOperator(QueryBean.Operator.not_before, "age", "25", beanInfo);
        testOperator(QueryBean.Operator.not_after, "age", "30", beanInfo);
    }

    private void testOperator(QueryBean.Operator operator, String fieldName, String parameter, BeanInfo beanInfo) {
        QueryBean.FilterBean filter = new QueryBean.FilterBean()
            .fieldName(fieldName)
            .operator(operator)
            .parameter(parameter);
        
        Condition condition = filter.toCondition(beanInfo);
        assertNotNull(condition);
    }

    @Test
    public void testOrderByBean() {
        // Test single field ordering
        QueryBean.OrderByBean orderBy = new QueryBean.OrderByBean()
            .fieldName("created_date")
            .sortDirection(SortDirection.DESC);
        
        OrderBy order = orderBy.toOrderBy();
        assertNotNull(order);
        
        // Test multiple fields ordering
        orderBy = new QueryBean.OrderByBean()
            .fieldNames(Arrays.asList("last_name", "first_name"))
            .sortDirection(SortDirection.ASC);
        
        order = orderBy.toOrderBy();
        assertNotNull(order);
        
        // Test that fieldNames takes precedence over fieldName
        orderBy = new QueryBean.OrderByBean()
            .fieldName("ignored")
            .fieldNames(Arrays.asList("priority1", "priority2"))
            .sortDirection(SortDirection.DESC);
        
        order = orderBy.toOrderBy();
        assertNotNull(order);
    }

    @Test
    public void testOperatorSqlMethods() {
        // Test toSql without column
        assertEquals("= 'value'", QueryBean.Operator.equals.toSql("value"));
        assertEquals("= 123", QueryBean.Operator.equals.toSql(123));
        
        // Test toSql with column
        assertEquals("name = 'John'", QueryBean.Operator.equals.toSql("name", "John"));
        assertEquals("age > 25", QueryBean.Operator.greater_than.toSql("age", 25));
        
        // Test toParameterizedSql without column
        assertEquals("= ?", QueryBean.Operator.equals.toParameterizedSql("value"));
        
        // Test toParameterizedSql with column
        assertEquals("name = ?", QueryBean.Operator.equals.toParameterizedSql("name", "value"));
        
        // Test appendSql
        StringBuilder sb = new StringBuilder();
        QueryBean.Operator.equals.appendSql(sb, "value");
        assertEquals("= 'value'", sb.toString());
        
        sb = new StringBuilder();
        QueryBean.Operator.equals.appendSql(sb, "name", "John");
        assertEquals("name = 'John'", sb.toString());
        
        // Test appendParameterizedSql
        sb = new StringBuilder();
        QueryBean.Operator.equals.appendParameterizedSql(sb, "value");
        assertEquals("= ?", sb.toString());
        
        sb = new StringBuilder();
        QueryBean.Operator.equals.appendParameterizedSql(sb, "name", "value");
        assertEquals("name = ?", sb.toString());
    }

    @Test
    public void testOperatorSpecialCases() {
        // Test LIKE operators
        assertEquals("LIKE '%test%'", QueryBean.Operator.contains.toSql("test"));
        assertEquals("name LIKE '%test%'", QueryBean.Operator.contains.toSql("name", "test"));
        assertEquals("LIKE 'test%'", QueryBean.Operator.starts_with.toSql("test"));
        assertEquals("LIKE '%test'", QueryBean.Operator.ends_with.toSql("test"));
        
        // Test IN operator
        assertEquals("IN ('a', 'b', 'c')", QueryBean.Operator.in.toSql(Arrays.asList("a", "b", "c")));
        assertEquals("IN (1, 2, 3)", QueryBean.Operator.in.toSql(Arrays.asList(1, 2, 3)));
        assertEquals("status IN ('active', 'pending')", QueryBean.Operator.in.toSql("status", Arrays.asList("active", "pending")));
        assertEquals("IN (?, ?, ?)", QueryBean.Operator.in.toParameterizedSql(Arrays.asList("a", "b", "c")));
        assertEquals("status IN (?, ?)", QueryBean.Operator.in.toParameterizedSql("status", Arrays.asList("a", "b")));
        
        // Test BETWEEN operator
        assertEquals("BETWEEN (10, 20)", QueryBean.Operator.between.toSql(Arrays.asList(10, 20)));
        assertEquals("BETWEEN ('2023-01-01', '2023-12-31')", QueryBean.Operator.between.toSql(Arrays.asList("2023-01-01", "2023-12-31")));
        assertEquals("age BETWEEN (18, 65)", QueryBean.Operator.between.toSql("age", Arrays.asList(18, 65)));
        assertEquals("BETWEEN (?, ?)", QueryBean.Operator.between.toParameterizedSql(Arrays.asList(10, 20)));
        assertEquals("age BETWEEN (?, ?)", QueryBean.Operator.between.toParameterizedSql("age", Arrays.asList(10, 20)));
    }

    @Test
    public void testOperatorSqlOperator() {
        // Test sqlOperator() method
        assertEquals("=", QueryBean.Operator.equals.sqlOperator());
        assertEquals("!=", QueryBean.Operator.not_equals.sqlOperator());
        assertEquals(">", QueryBean.Operator.greater_than.sqlOperator());
        assertEquals(">=", QueryBean.Operator.greater_than_or_equals.sqlOperator());
        assertEquals("<", QueryBean.Operator.less_than.sqlOperator());
        assertEquals("<=", QueryBean.Operator.less_than_or_equals.sqlOperator());
        assertEquals("LIKE", QueryBean.Operator.starts_with.sqlOperator());
        assertEquals("NOT LIKE", QueryBean.Operator.not_starts_with.sqlOperator());
        assertEquals("IN", QueryBean.Operator.in.sqlOperator());
        assertEquals("NOT IN", QueryBean.Operator.not_in.sqlOperator());
        assertEquals("BETWEEN", QueryBean.Operator.between.sqlOperator());
        assertEquals("NOT BETWEEN", QueryBean.Operator.not_between.sqlOperator());
    }

    @Test
    public void testQueryBeanAllArgsConstructor() {
        List<String> select = Arrays.asList("id", "name");
        String from = "users";
        List<QueryBean.FilterBean> where = Arrays.asList(
            new QueryBean.FilterBean().fieldName("status").operator(QueryBean.Operator.equals).parameter("active")
        );
        List<QueryBean.FilterBean> having = Arrays.asList(
            new QueryBean.FilterBean().fieldName("count").operator(QueryBean.Operator.greater_than).parameter("5")
        );
        List<QueryBean.OrderByBean> orderBy = Arrays.asList(
            new QueryBean.OrderByBean().fieldName("created_date").sortDirection(SortDirection.DESC)
        );
        
        QueryBean query = new QueryBean(select, from, where, having, orderBy, true, 10, 20);
        
        assertEquals(select, query.select());
        assertEquals(from, query.from());
        assertEquals(where, query.where());
        assertEquals(having, query.having());
        assertEquals(orderBy, query.orderBy());
        assertTrue(query.distinct());
        assertEquals(10, query.offset());
        assertEquals(20, query.limit());
    }
}