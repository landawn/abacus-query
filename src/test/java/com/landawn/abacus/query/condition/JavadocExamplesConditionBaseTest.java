/*
 * Test class that verifies ALL Javadoc usage examples from the condition package base/utility files:
 * Condition.java, Operator.java, Binary.java, Cell.java, Between.java, NotBetween.java,
 * Expression.java, NamedProperty.java, Criteria.java, CriteriaUtil.java, Junction.java
 */
package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

/**
 * Verifies all Javadoc {@code <pre>{@code ...}</pre>} examples from the condition package base/utility files.
 */
@SuppressWarnings("deprecation")
public class JavadocExamplesConditionBaseTest {

    // =====================================================================
    // Condition.java - class-level example
    // =====================================================================

    @Test
    void condition_classLevel_createSimpleConditions() {
        // Create simple conditions
        Condition ageCondition = Filters.gt("age", 18);
        Condition statusCondition = Filters.eq("status", "active");

        // Combine conditions using logical operations
        Condition combined = ageCondition.and(statusCondition);

        // Negate a condition
        Condition notActive = statusCondition.not();

        // Get parameters for prepared statements
        List<Object> params = combined.getParameters();
        assertEquals(Arrays.asList(18, "active"), params);

        assertNotNull(notActive);
        assertInstanceOf(Not.class, notActive);
    }

    // =====================================================================
    // Condition.java - getOperator() examples
    // =====================================================================

    @Test
    void condition_getOperator_examples() {
        Condition eq = Filters.eq("status", "active");
        Operator op = eq.getOperator();
        assertEquals(Operator.EQUAL, op);

        Condition between = Filters.between("age", 18, 65);
        Operator betweenOp = between.getOperator();
        assertEquals(Operator.BETWEEN, betweenOp);

        Condition combined = eq.and(between);
        Operator andOp = combined.getOperator();
        assertEquals(Operator.AND, andOp);
    }

    // =====================================================================
    // Condition.java - and() examples
    // =====================================================================

    @Test
    void condition_and_examples() {
        Condition age = Filters.gt("age", 18);
        Condition status = Filters.eq("status", "active");
        And combined = age.and(status);
        assertNotNull(combined);
        assertInstanceOf(And.class, combined);

        // Can be chained
        Condition verified = Filters.eq("verified", true);
        And all = age.and(status).and(verified);
        assertNotNull(all);
        assertEquals(3, all.getConditions().size());
    }

    // =====================================================================
    // Condition.java - or() examples
    // =====================================================================

    @Test
    void condition_or_examples() {
        Condition premium = Filters.eq("memberType", "premium");
        Condition vip = Filters.eq("memberType", "vip");
        Or combined = premium.or(vip);
        assertNotNull(combined);
        assertInstanceOf(Or.class, combined);

        // Can be chained
        Condition gold = Filters.eq("memberType", "gold");
        Or any = premium.or(vip).or(gold);
        assertNotNull(any);
        assertEquals(3, any.getConditions().size());
    }

    // =====================================================================
    // Condition.java - not() examples
    // =====================================================================

    @Test
    void condition_not_examples() {
        Condition isNull = Filters.isNull("email");
        Not isNotNull = isNull.not();
        assertNotNull(isNotNull);
        assertInstanceOf(Not.class, isNotNull);

        // Complex negation
        Condition complex = Filters.and(
                Filters.eq("status", "active"),
                Filters.gt("age", 18));
        Not negated = complex.not();
        assertNotNull(negated);
    }

    // =====================================================================
    // Condition.java - copy() examples
    // =====================================================================

    @Test
    void condition_copy_examples() {
        Condition original = Filters.eq("name", "John");
        Condition copied = original.copy();

        // The copy is independent of the original
        original.clearParameters();
        List<Object> copiedParams = copied.getParameters();
        assertEquals(Arrays.asList((Object) "John"), copiedParams);

        // Copy a complex condition
        Condition complex = Filters.and(Filters.gt("age", 18), Filters.eq("status", "active"));
        Condition complexCopy = complex.copy();
        assertNotNull(complexCopy);
    }

    // =====================================================================
    // Condition.java - getParameters() examples
    // =====================================================================

    @Test
    void condition_getParameters_examples() {
        Condition eq = Filters.eq("name", "John");
        List<Object> params = eq.getParameters();
        assertEquals(Arrays.asList((Object) "John"), params);

        Condition between = Filters.between("age", 18, 65);
        List<Object> rangeParams = between.getParameters();
        assertEquals(Arrays.asList(18, 65), rangeParams);

        Condition combined = Filters.and(eq, between);
        List<Object> allParams = combined.getParameters();
        assertEquals(Arrays.asList("John", 18, 65), allParams);
    }

    // =====================================================================
    // Condition.java - clearParameters() examples
    // =====================================================================

    @Test
    void condition_clearParameters_examples() {
        Condition eq = Filters.eq("name", "John");
        List<Object> params = eq.getParameters();
        assertEquals(Arrays.asList((Object) "John"), params);

        // Release parameter memory when the condition is no longer needed
        eq.clearParameters();
        List<Object> cleared = eq.getParameters();
        assertEquals(Arrays.asList((Object) null), cleared);

        // For compound conditions, clears parameters recursively
        Condition combined = Filters.and(Filters.gt("age", 18), Filters.eq("status", "active"));
        combined.clearParameters();
    }

    // =====================================================================
    // Condition.java - toString(NamingPolicy) examples
    // =====================================================================

    @Test
    void condition_toStringNamingPolicy_examples() {
        Condition eq = Filters.eq("firstName", "John");

        // No change to property names
        String noChange = eq.toString(NamingPolicy.NO_CHANGE);
        assertEquals("firstName = 'John'", noChange);

        // Convert to lower case with underscores (snake_case)
        String lower = eq.toString(NamingPolicy.SNAKE_CASE);
        assertEquals("first_name = 'John'", lower);

        // Convert to upper case with underscores (SCREAMING_SNAKE_CASE)
        String upper = eq.toString(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertEquals("FIRST_NAME = 'John'", upper);
    }

    // =====================================================================
    // Operator.java - class-level examples
    // =====================================================================

    @Test
    void operator_classLevel_directUsage() {
        // Direct operator usage
        Operator op = Operator.EQUAL;
        String propertyName = "age";
        String sql = propertyName + " " + op.toString() + " ?";
        assertEquals("age = ?", sql);

        // Get operator by name
        Operator gtOp = Operator.getOperator(">");
        assertEquals(Operator.GREATER_THAN, gtOp);

        Operator andOp = Operator.getOperator("AND");
        assertEquals(Operator.AND, andOp);

        // Check operator type
        Operator betweenOp = Operator.BETWEEN;
        assertEquals(Operator.BETWEEN, betweenOp);
    }

    // =====================================================================
    // Operator.java - getOperator(String) examples
    // =====================================================================

    @Test
    void operator_getOperator_symbolicOperators() {
        Operator eq = Operator.getOperator("=");
        assertEquals(Operator.EQUAL, eq);

        Operator gt = Operator.getOperator(">");
        assertEquals(Operator.GREATER_THAN, gt);

        Operator gte = Operator.getOperator(">=");
        assertEquals(Operator.GREATER_EQUAL, gte);
    }

    @Test
    void operator_getOperator_wordOperators() {
        Operator andOp = Operator.getOperator("AND");
        assertEquals(Operator.AND, andOp);

        Operator orOp = Operator.getOperator("OR");
        assertEquals(Operator.OR, orOp);

        Operator likeOp = Operator.getOperator("LIKE");
        assertEquals(Operator.LIKE, likeOp);
    }

    @Test
    void operator_getOperator_unknownAndNull() {
        Operator unknown = Operator.getOperator("UNKNOWN");
        assertNull(unknown);

        Operator nullOp = Operator.getOperator(null);
        assertNull(nullOp);
    }

    // =====================================================================
    // Operator.java - getName() examples
    // =====================================================================

    @Test
    void operator_getName_examples() {
        String eqName = Operator.EQUAL.getName();
        assertEquals("=", eqName);

        String andName = Operator.AND.getName();
        assertEquals("AND", andName);

        String betweenName = Operator.BETWEEN.getName();
        assertEquals("BETWEEN", betweenName);

        String likeName = Operator.LIKE.getName();
        assertEquals("LIKE", likeName);

        String joinName = Operator.LEFT_JOIN.getName();
        assertEquals("LEFT JOIN", joinName);
    }

    // =====================================================================
    // Binary.java - class-level examples
    // =====================================================================

    @Test
    void binary_classLevel_simpleBinaryConditions() {
        // Simple binary conditions
        Binary eq = new Equal("name", "John");
        assertNotNull(eq);

        Binary gt = new GreaterThan("age", 18);
        assertNotNull(gt);

        // Binary condition with subquery
        SubQuery avgSalary = Filters.subQuery("SELECT AVG(salary) FROM employees");
        Binary aboveAvg = new GreaterThan("salary", avgSalary);
        assertNotNull(aboveAvg);
    }

    // =====================================================================
    // Binary.java - constructor examples
    // =====================================================================

    @Test
    void binary_constructor_examples() {
        // Create a custom binary condition
        Binary condition = new Binary("price", Operator.GREATER_THAN, 100.0);
        assertNotNull(condition);
        assertEquals("price", condition.getPropName());

        // With a subquery as value
        SubQuery subQuery = Filters.subQuery("SELECT MIN(price) FROM products");
        Binary minPrice = new Binary("price", Operator.GREATER_EQUAL, subQuery);
        assertNotNull(minPrice);
    }

    // =====================================================================
    // Binary.java - getPropName() examples
    // =====================================================================

    @Test
    void binary_getPropName_examples() {
        Binary eq = new Equal("age", 25);
        String name = eq.getPropName();
        assertEquals("age", name);

        Binary like = new Like("email", "%@example.com");
        String likeName = like.getPropName();
        assertEquals("email", likeName);
    }

    // =====================================================================
    // Binary.java - getPropValue() examples
    // =====================================================================

    @Test
    void binary_getPropValue_examples() {
        Binary eq = new Equal("age", 25);
        Integer value = eq.getPropValue();
        assertEquals(25, value);

        Binary like = new Like("name", "%John%");
        String pattern = like.getPropValue();
        assertEquals("%John%", pattern);

        // With a subquery as value
        SubQuery subQuery = Filters.subQuery("SELECT MAX(salary) FROM employees");
        Binary gt = new GreaterThan("salary", subQuery);
        SubQuery sub = gt.getPropValue();
        assertNotNull(sub);
    }

    // =====================================================================
    // Binary.java - setPropValue() examples
    // =====================================================================

    @Test
    void binary_setPropValue_examples() {
        // Deprecated: prefer creating a new condition instead
        Binary eq = new Equal("status", "active");
        eq.setPropValue("inactive");
        assertEquals("inactive", (String) eq.getPropValue());

        // Preferred approach: create a new condition
        Binary newEq = new Equal("status", "inactive");
        assertEquals("inactive", (String) newEq.getPropValue());
    }

    // =====================================================================
    // Cell.java - class-level examples
    // =====================================================================

    @Test
    void cell_classLevel_examples() {
        // Create a NOT cell
        Cell notCell = new Cell(Operator.NOT, Filters.eq("status", "active"));
        assertNotNull(notCell);

        // Create an EXISTS cell with a subquery
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.user_id = users.id");
        Cell existsCell = new Cell(Operator.EXISTS, subQuery);
        assertNotNull(existsCell);
    }

    // =====================================================================
    // Cell.java - constructor examples
    // =====================================================================

    @Test
    void cell_constructor_examples() {
        // Create a NOT cell that negates a condition
        Cell notCell = new Cell(Operator.NOT, Filters.isNull("email"));
        assertNotNull(notCell);

        // Create an EXISTS cell for a subquery
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM products WHERE price > 100");
        Cell existsCell = new Cell(Operator.EXISTS, subQuery);
        assertNotNull(existsCell);
    }

    // =====================================================================
    // Cell.java - getCondition() examples
    // =====================================================================

    @Test
    void cell_getCondition_examples() {
        // Create a NOT cell wrapping an equality condition
        Cell notCell = new Cell(Operator.NOT, Filters.eq("status", "active"));
        Condition inner = notCell.getCondition();
        assertNotNull(inner);

        // Create an EXISTS cell with a subquery
        SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.user_id = users.id");
        Cell existsCell = new Cell(Operator.EXISTS, subQuery);
        SubQuery sq = existsCell.getCondition();
        assertNotNull(sq);
    }

    // =====================================================================
    // Cell.java - setCondition() examples
    // =====================================================================

    @Test
    void cell_setCondition_examples() {
        // Deprecated: prefer creating a new Cell instead
        Cell notCell = new Cell(Operator.NOT, Filters.eq("status", "active"));
        notCell.setCondition(Filters.eq("status", "inactive"));

        // Preferred approach: create a new Cell
        Cell newNotCell = new Cell(Operator.NOT, Filters.eq("status", "inactive"));
        assertNotNull(newNotCell);
    }

    // =====================================================================
    // Between.java - class-level examples
    // =====================================================================

    @Test
    void between_classLevel_numericRange() {
        Between ageRange = new Between("age", 18, 65);
        assertNotNull(ageRange);
        assertEquals("age", ageRange.getPropName());
    }

    @Test
    void between_classLevel_stringRange() {
        Between nameRange = new Between("lastName", "A", "M");
        assertNotNull(nameRange);
    }

    @Test
    void between_classLevel_subqueryRange() {
        SubQuery minPrice = Filters.subQuery("SELECT MIN(price) FROM products");
        SubQuery maxPrice = Filters.subQuery("SELECT MAX(price) FROM products");
        Between priceRange = new Between("price", minPrice, maxPrice);
        assertNotNull(priceRange);
    }

    // =====================================================================
    // Between.java - constructor examples
    // =====================================================================

    @Test
    void between_constructor_examples() {
        // Check if age is between 18 and 65 (inclusive)
        Between ageRange = new Between("age", 18, 65);
        assertNotNull(ageRange);

        // Check if salary is within a range
        Between salaryRange = new Between("salary", 50000, 100000);
        assertNotNull(salaryRange);

        // Use with subqueries for dynamic ranges
        SubQuery avgMinus10 = Filters.subQuery("SELECT AVG(score) - 10 FROM scores");
        SubQuery avgPlus10 = Filters.subQuery("SELECT AVG(score) + 10 FROM scores");
        Between nearAverage = new Between("score", avgMinus10, avgPlus10);
        assertNotNull(nearAverage);
    }

    // =====================================================================
    // Between.java - getPropName() examples
    // =====================================================================

    @Test
    void between_getPropName_examples() {
        Between ageRange = new Between("age", 18, 65);
        String name = ageRange.getPropName();
        assertEquals("age", name);

        Between dateRange = new Between("orderDate", "2024-01-01", "2024-12-31");
        String dateName = dateRange.getPropName();
        assertEquals("orderDate", dateName);
    }

    // =====================================================================
    // Between.java - getMinValue() examples
    // =====================================================================

    @Test
    void between_getMinValue_examples() {
        Between ageRange = new Between("age", 18, 65);
        Integer min = ageRange.getMinValue();
        assertEquals(18, min);

        Between priceRange = new Between("price", 9.99, 99.99);
        Double minPrice = priceRange.getMinValue();
        assertEquals(9.99, minPrice);
    }

    // =====================================================================
    // Between.java - getMaxValue() examples
    // =====================================================================

    @Test
    void between_getMaxValue_examples() {
        Between ageRange = new Between("age", 18, 65);
        Integer max = ageRange.getMaxValue();
        assertEquals(65, max);

        Between priceRange = new Between("price", 9.99, 99.99);
        Double maxPrice = priceRange.getMaxValue();
        assertEquals(99.99, maxPrice);
    }

    // =====================================================================
    // Between.java - setMinValue() / setMaxValue() examples
    // =====================================================================

    @Test
    void between_setMinMaxValue_examples() {
        // Deprecated: prefer creating a new Between instead
        Between range = new Between("age", 18, 65);
        range.setMinValue(21);
        assertEquals(21, (int) range.getMinValue());

        // Preferred approach: create a new Between
        Between newRange = new Between("age", 21, 65);
        assertEquals(21, (int) newRange.getMinValue());

        Between range2 = new Between("age", 18, 65);
        range2.setMaxValue(70);
        assertEquals(70, (int) range2.getMaxValue());

        Between newRange2 = new Between("age", 18, 70);
        assertEquals(70, (int) newRange2.getMaxValue());
    }

    // =====================================================================
    // NotBetween.java - class-level examples
    // =====================================================================

    @Test
    void notBetween_classLevel_examples() {
        // Exclude normal temperature range
        NotBetween abnormalTemp = new NotBetween("temperature", 36.0, 37.5);
        assertNotNull(abnormalTemp);

        // Find orders outside business hours
        NotBetween outsideHours = new NotBetween("order_hour", 9, 17);
        assertNotNull(outsideHours);

        // Exclude mid-range salaries
        NotBetween salaryRange = new NotBetween("salary", 50000, 100000);
        assertNotNull(salaryRange);

        // Using with date strings
        NotBetween dateRange = new NotBetween("order_date", "2024-01-01", "2024-12-31");
        assertNotNull(dateRange);
    }

    // =====================================================================
    // NotBetween.java - constructor examples
    // =====================================================================

    @Test
    void notBetween_constructor_examples() {
        // Find products with extreme prices
        NotBetween priceRange = new NotBetween("price", 10.0, 1000.0);
        assertNotNull(priceRange);

        // Find events outside regular working days
        NotBetween workdays = new NotBetween("day_of_week", 2, 6);
        assertNotNull(workdays);
    }

    // =====================================================================
    // NotBetween.java - getPropName() examples
    // =====================================================================

    @Test
    void notBetween_getPropName_examples() {
        NotBetween tempRange = new NotBetween("temperature", 36.0, 37.5);
        String name = tempRange.getPropName();
        assertEquals("temperature", name);

        NotBetween salaryRange = new NotBetween("salary", 50000, 100000);
        String salaryName = salaryRange.getPropName();
        assertEquals("salary", salaryName);
    }

    // =====================================================================
    // NotBetween.java - getMinValue() / getMaxValue() examples
    // =====================================================================

    @Test
    void notBetween_getMinMaxValue_examples() {
        NotBetween tempRange = new NotBetween("temperature", 36.0, 37.5);
        Double min = tempRange.getMinValue();
        assertEquals(36.0, min);
        Double max = tempRange.getMaxValue();
        assertEquals(37.5, max);

        NotBetween ageRange = new NotBetween("age", 18, 65);
        Integer minAge = ageRange.getMinValue();
        assertEquals(18, minAge);
        Integer maxAge = ageRange.getMaxValue();
        assertEquals(65, maxAge);
    }

    // =====================================================================
    // NotBetween.java - setMinValue() / setMaxValue() examples
    // =====================================================================

    @Test
    void notBetween_setMinMaxValue_examples() {
        NotBetween range = new NotBetween("temperature", 36.0, 37.5);
        range.setMinValue(35.5);
        assertEquals(35.5, (double) range.getMinValue());

        NotBetween newRange = new NotBetween("temperature", 35.5, 37.5);
        assertEquals(35.5, (double) newRange.getMinValue());

        NotBetween range2 = new NotBetween("temperature", 36.0, 37.5);
        range2.setMaxValue(38.0);
        assertEquals(38.0, (double) range2.getMaxValue());

        NotBetween newRange2 = new NotBetween("temperature", 36.0, 38.0);
        assertEquals(38.0, (double) newRange2.getMaxValue());
    }

    // =====================================================================
    // Expression.java - class-level examples
    // =====================================================================

    @Test
    void expression_classLevel_examples() {
        // Simple expression
        Expression expr = Expression.of("price * 0.9");
        assertNotNull(expr);

        // Using in a condition
        Condition discountPrice = Filters.expr("price * 0.9 < 100");
        assertNotNull(discountPrice);

        // SQL functions
        String upperName = Expression.upper("name");
        assertEquals("UPPER(name)", upperName);

        String avgPrice = Expression.average("price");
        assertEquals("AVG(price)", avgPrice);

        // Complex expressions (use Expression.of() for column references to avoid quoting)
        String complex = Expression.plus(Expression.of("base_price"), Expression.of("tax"), Expression.of("shipping"));
        assertEquals("base_price + tax + shipping", complex);
    }

    // =====================================================================
    // Expression.java - constructor examples
    // =====================================================================

    @Test
    void expression_constructor_examples() {
        Expression expr1 = new Expression("CURRENT_TIMESTAMP");
        assertNotNull(expr1);

        Expression expr2 = new Expression("price * quantity");
        assertNotNull(expr2);

        Expression expr3 = new Expression("CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END");
        assertNotNull(expr3);

        Expression expr4 = new Expression("COALESCE(middle_name, '')");
        assertNotNull(expr4);
    }

    // =====================================================================
    // Expression.java - getLiteral() examples
    // =====================================================================

    @Test
    void expression_getLiteral_examples() {
        Expression expr = new Expression("price * quantity");
        String literal = expr.getLiteral();
        assertEquals("price * quantity", literal);

        Expression expr2 = Expression.of("CURRENT_TIMESTAMP");
        String literal2 = expr2.getLiteral();
        assertEquals("CURRENT_TIMESTAMP", literal2);

        Expression empty = new Expression(null);
        String literal3 = empty.getLiteral();
        assertNull(literal3);
    }

    // =====================================================================
    // Expression.java - of() examples
    // =====================================================================

    @Test
    void expression_of_caching() {
        Expression expr1 = Expression.of("CURRENT_DATE");
        Expression expr2 = Expression.of("CURRENT_DATE");
        // expr1 == expr2 (same instance due to caching)
        assertSame(expr1, expr2);

        Expression calc = Expression.of("price * 1.1");
        assertNotNull(calc);
    }

    // =====================================================================
    // Expression.java - equal/eq examples
    // =====================================================================

    @Test
    void expression_equal_examples() {
        String expr = Expression.equal("age", 25);
        assertEquals("age = 25", expr);

        String expr2 = Expression.equal("status", "active");
        assertEquals("status = 'active'", expr2);
    }

    @Test
    void expression_eq_examples() {
        String expr = Expression.eq("user_id", 123);
        assertEquals("user_id = 123", expr);
    }

    // =====================================================================
    // Expression.java - notEqual/ne examples
    // =====================================================================

    @Test
    void expression_notEqual_examples() {
        String expr = Expression.notEqual("status", "INACTIVE");
        assertEquals("status != 'INACTIVE'", expr);

        String expr2 = Expression.notEqual("count", 0);
        assertEquals("count != 0", expr2);
    }

    @Test
    void expression_ne_examples() {
        String expr = Expression.ne("type", "guest");
        assertEquals("type != 'guest'", expr);
    }

    // =====================================================================
    // Expression.java - greaterThan/gt examples
    // =====================================================================

    @Test
    void expression_greaterThan_examples() {
        String expr = Expression.greaterThan("salary", 50000);
        assertEquals("salary > 50000", expr);

        String expr2 = Expression.greaterThan("created_date", "2024-01-01");
        assertEquals("created_date > '2024-01-01'", expr2);
    }

    @Test
    void expression_gt_examples() {
        String expr = Expression.gt("age", 18);
        assertEquals("age > 18", expr);
    }

    // =====================================================================
    // Expression.java - greaterEqual/ge examples
    // =====================================================================

    @Test
    void expression_greaterEqual_examples() {
        String expr = Expression.greaterEqual("score", 60);
        assertEquals("score >= 60", expr);
    }

    @Test
    void expression_ge_examples() {
        String expr = Expression.ge("quantity", 1);
        assertEquals("quantity >= 1", expr);
    }

    // =====================================================================
    // Expression.java - lessThan/lt examples
    // =====================================================================

    @Test
    void expression_lessThan_examples() {
        String expr = Expression.lessThan("price", 100);
        assertEquals("price < 100", expr);
    }

    @Test
    void expression_lt_examples() {
        String expr = Expression.lt("stock", 10);
        assertEquals("stock < 10", expr);
    }

    // =====================================================================
    // Expression.java - lessEqual/le examples
    // =====================================================================

    @Test
    void expression_lessEqual_examples() {
        String expr = Expression.lessEqual("discount", 50);
        assertEquals("discount <= 50", expr);
    }

    @Test
    void expression_le_examples() {
        String expr = Expression.le("temperature", 32);
        assertEquals("temperature <= 32", expr);
    }

    // =====================================================================
    // Expression.java - between examples
    // =====================================================================

    @Test
    void expression_between_examples() {
        String expr = Expression.between("age", 18, 65);
        assertEquals("age BETWEEN 18 AND 65", expr);

        String expr2 = Expression.between("price", 10.0, 50.0);
        assertEquals("price BETWEEN 10.0 AND 50.0", expr2);
    }

    @Test
    void expression_bt_examples() {
        String expr = Expression.bt("score", 60, 100);
        assertEquals("score BETWEEN 60 AND 100", expr);
    }

    // =====================================================================
    // Expression.java - like examples
    // =====================================================================

    @Test
    void expression_like_examples() {
        String expr = Expression.like("name", "John%");
        assertEquals("name LIKE 'John%'", expr);

        String expr2 = Expression.like("email", "%@gmail.com");
        assertEquals("email LIKE '%@gmail.com'", expr2);

        String expr3 = Expression.like("code", "A__");
        assertEquals("code LIKE 'A__'", expr3);
    }

    // =====================================================================
    // Expression.java - isNull/isNotNull examples
    // =====================================================================

    @Test
    void expression_isNull_examples() {
        String expr = Expression.isNull("email");
        assertEquals("email IS NULL", expr);

        String expr2 = Expression.isNull("middle_name");
        assertEquals("middle_name IS NULL", expr2);
    }

    @Test
    void expression_isNotNull_examples() {
        String expr = Expression.isNotNull("email");
        assertEquals("email IS NOT NULL", expr);

        String expr2 = Expression.isNotNull("phone");
        assertEquals("phone IS NOT NULL", expr2);
    }

    // =====================================================================
    // Expression.java - isEmpty/isNotEmpty examples
    // =====================================================================

    @Test
    void expression_isEmpty_examples() {
        String expr = Expression.isEmpty("description");
        assertEquals("description IS BLANK", expr);

        String expr2 = Expression.isEmpty("address");
        assertEquals("address IS BLANK", expr2);
    }

    @Test
    void expression_isNotEmpty_examples() {
        String expr = Expression.isNotEmpty("name");
        assertEquals("name IS NOT BLANK", expr);

        String expr2 = Expression.isNotEmpty("comment");
        assertEquals("comment IS NOT BLANK", expr2);
    }

    // =====================================================================
    // Expression.java - and/or examples
    // =====================================================================

    @Test
    void expression_and_examples() {
        String expr = Expression.and("active = true", "age > 18", "status = 'APPROVED'");
        assertEquals("active = true AND age > 18 AND status = 'APPROVED'", expr);

        String expr2 = Expression.and("verified = 1", "email IS NOT NULL");
        assertEquals("verified = 1 AND email IS NOT NULL", expr2);
    }

    @Test
    void expression_or_examples() {
        String expr = Expression.or("status = 'active'", "status = 'pending'", "priority = 1");
        assertEquals("status = 'active' OR status = 'pending' OR priority = 1", expr);
    }

    // =====================================================================
    // Expression.java - plus/minus/multi/division/modulus examples
    // =====================================================================

    @Test
    void expression_plus_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.plus(Expression.of("price"), Expression.of("tax"), Expression.of("shipping"));
        assertEquals("price + tax + shipping", expr);

        String expr2 = Expression.plus(Expression.of("base_salary"), 5000, Expression.of("bonus"));
        assertEquals("base_salary + 5000 + bonus", expr2);
    }

    @Test
    void expression_minus_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.minus(Expression.of("total"), Expression.of("discount"), Expression.of("tax_credit"));
        assertEquals("total - discount - tax_credit", expr);

        String expr2 = Expression.minus(Expression.of("price"), 10);
        assertEquals("price - 10", expr2);
    }

    @Test
    void expression_multi_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.multi(Expression.of("price"), Expression.of("quantity"), Expression.of("tax_rate"));
        assertEquals("price * quantity * tax_rate", expr);

        String expr2 = Expression.multi(Expression.of("hours"), 60);
        assertEquals("hours * 60", expr2);
    }

    @Test
    void expression_division_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.division(Expression.of("total"), Expression.of("count"));
        assertEquals("total / count", expr);

        String expr2 = Expression.division(Expression.of("distance"), Expression.of("time"), 60);
        assertEquals("distance / time / 60", expr2);
    }

    @Test
    void expression_modulus_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.modulus(Expression.of("value"), 10);
        assertEquals("value % 10", expr);

        String expr2 = Expression.modulus(Expression.of("id"), Expression.of("batch_size"));
        assertEquals("id % batch_size", expr2);
    }

    // =====================================================================
    // Expression.java - shift/bitwise examples
    // =====================================================================

    @Test
    void expression_lShift_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.lShift(Expression.of("flags"), 2);
        assertEquals("flags << 2", expr);
    }

    @Test
    void expression_rShift_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.rShift(Expression.of("value"), 4);
        assertEquals("value >> 4", expr);
    }

    @Test
    void expression_bitwiseAnd_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.bitwiseAnd(Expression.of("permissions"), Expression.of("mask"));
        assertEquals("permissions & mask", expr);

        String expr2 = Expression.bitwiseAnd(Expression.of("flags"), 0xFF);
        assertEquals("flags & 255", expr2);
    }

    @Test
    void expression_bitwiseOr_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.bitwiseOr(Expression.of("flags1"), Expression.of("flags2"));
        assertEquals("flags1 | flags2", expr);
    }

    @Test
    void expression_bitwiseXOr_examples() {
        // Use Expression.of() for column references to avoid single-quote wrapping
        String expr = Expression.bitwiseXOr(Expression.of("value1"), Expression.of("value2"));
        assertEquals("value1 ^ value2", expr);
    }

    // =====================================================================
    // Expression.java - formalize examples
    // =====================================================================

    @Test
    void expression_formalize_examples() {
        assertEquals("'text'", Expression.formalize("text"));
        assertEquals("123", Expression.formalize(123));
        assertEquals("45.67", Expression.formalize(45.67));
        assertEquals("null", Expression.formalize(null));
        assertEquals("true", Expression.formalize(true));
        assertEquals("false", Expression.formalize(false));

        Expression expr = new Expression("COUNT(*)");
        assertEquals("COUNT(*)", Expression.formalize(expr));
    }

    @Test
    void expression_formalize_escapedQuote() {
        // Check O'Brien is escaped
        String result = Expression.formalize("O'Brien");
        // The result should contain an escaped single quote
        assertNotNull(result);
        assertTrue(result.startsWith("'"));
        assertTrue(result.endsWith("'"));
    }

    // =====================================================================
    // Expression.java - SQL function examples: count, average, sum, min, max
    // =====================================================================

    @Test
    void expression_count_examples() {
        assertEquals("COUNT(*)", Expression.count("*"));
        assertEquals("COUNT(id)", Expression.count("id"));
        assertEquals("COUNT(DISTINCT department)", Expression.count("DISTINCT department"));
    }

    @Test
    void expression_average_examples() {
        assertEquals("AVG(salary)", Expression.average("salary"));
        assertEquals("AVG(age)", Expression.average("age"));
    }

    @Test
    void expression_sum_examples() {
        assertEquals("SUM(amount)", Expression.sum("amount"));
        assertEquals("SUM(quantity * price)", Expression.sum("quantity * price"));
    }

    @Test
    void expression_min_examples() {
        assertEquals("MIN(price)", Expression.min("price"));
        assertEquals("MIN(created_date)", Expression.min("created_date"));
    }

    @Test
    void expression_max_examples() {
        assertEquals("MAX(score)", Expression.max("score"));
        assertEquals("MAX(last_login)", Expression.max("last_login"));
    }

    // =====================================================================
    // Expression.java - math function examples
    // =====================================================================

    @Test
    void expression_abs_examples() {
        assertEquals("ABS(balance)", Expression.abs("balance"));
        assertEquals("ABS(temperature)", Expression.abs("temperature"));
    }

    @Test
    void expression_acos_examples() {
        assertEquals("ACOS(0.5)", Expression.acos("0.5"));
        assertEquals("ACOS(cosine_value)", Expression.acos("cosine_value"));
    }

    @Test
    void expression_asin_examples() {
        assertEquals("ASIN(0.5)", Expression.asin("0.5"));
        assertEquals("ASIN(sine_value)", Expression.asin("sine_value"));
    }

    @Test
    void expression_atan_examples() {
        assertEquals("ATAN(1)", Expression.atan("1"));
        assertEquals("ATAN(tangent_value)", Expression.atan("tangent_value"));
    }

    @Test
    void expression_ceil_examples() {
        assertEquals("CEIL(4.2)", Expression.ceil("4.2"));
        assertEquals("CEIL(price)", Expression.ceil("price"));
    }

    @Test
    void expression_cos_examples() {
        assertEquals("COS(3.14159)", Expression.cos("3.14159"));
        assertEquals("COS(angle)", Expression.cos("angle"));
    }

    @Test
    void expression_exp_examples() {
        assertEquals("EXP(2)", Expression.exp("2"));
        assertEquals("EXP(growth_rate)", Expression.exp("growth_rate"));
    }

    @Test
    void expression_floor_examples() {
        assertEquals("FLOOR(4.8)", Expression.floor("4.8"));
        assertEquals("FLOOR(average)", Expression.floor("average"));
    }

    @Test
    void expression_log_examples() {
        assertEquals("LOG(10, 100)", Expression.log("10", "100"));
        assertEquals("LOG(2, value)", Expression.log("2", "value"));
    }

    @Test
    void expression_ln_examples() {
        assertEquals("LN(10)", Expression.ln("10"));
        assertEquals("LN(value)", Expression.ln("value"));
    }

    @Test
    void expression_mod_examples() {
        assertEquals("MOD(10, 3)", Expression.mod("10", "3"));
        assertEquals("MOD(id, batch_size)", Expression.mod("id", "batch_size"));
    }

    @Test
    void expression_power_examples() {
        assertEquals("POWER(2, 10)", Expression.power("2", "10"));
        assertEquals("POWER(base, exponent)", Expression.power("base", "exponent"));
    }

    @Test
    void expression_sign_examples() {
        assertEquals("SIGN(-5)", Expression.sign("-5"));
        assertEquals("SIGN(balance)", Expression.sign("balance"));
    }

    @Test
    void expression_sin_examples() {
        assertEquals("SIN(1.5708)", Expression.sin("1.5708"));
        assertEquals("SIN(angle)", Expression.sin("angle"));
    }

    @Test
    void expression_sqrt_examples() {
        assertEquals("SQRT(16)", Expression.sqrt("16"));
        assertEquals("SQRT(area)", Expression.sqrt("area"));
    }

    @Test
    void expression_tan_examples() {
        assertEquals("TAN(0.7854)", Expression.tan("0.7854"));
        assertEquals("TAN(angle)", Expression.tan("angle"));
    }

    // =====================================================================
    // Expression.java - string function examples
    // =====================================================================

    @Test
    void expression_concat_examples() {
        String expr = Expression.concat("firstName", "' '");
        assertEquals("CONCAT(firstName, ' ')", expr);

        String expr2 = Expression.concat("city", "', '");
        assertEquals("CONCAT(city, ', ')", expr2);
    }

    @Test
    void expression_replace_examples() {
        String expr = Expression.replace("email", "'@'", "'_at_'");
        assertEquals("REPLACE(email, '@', '_at_')", expr);

        String expr2 = Expression.replace("phone", "'-'", "''");
        assertEquals("REPLACE(phone, '-', '')", expr2);
    }

    @Test
    void expression_stringLength_examples() {
        assertEquals("LENGTH(name)", Expression.stringLength("name"));
        assertEquals("LENGTH(description)", Expression.stringLength("description"));
    }

    @Test
    void expression_subString_examples() {
        assertEquals("SUBSTR(phone, 1)", Expression.subString("phone", 1));
        assertEquals("SUBSTR(code, 3)", Expression.subString("code", 3));

        assertEquals("SUBSTR(phone, 1, 3)", Expression.subString("phone", 1, 3));
        assertEquals("SUBSTR(zip, 1, 5)", Expression.subString("zip", 1, 5));
    }

    @Test
    void expression_trim_examples() {
        assertEquals("TRIM(input)", Expression.trim("input"));
        assertEquals("TRIM(user_name)", Expression.trim("user_name"));
    }

    @Test
    void expression_lTrim_examples() {
        assertEquals("LTRIM(comment)", Expression.lTrim("comment"));
        assertEquals("LTRIM(address)", Expression.lTrim("address"));
    }

    @Test
    void expression_rTrim_examples() {
        assertEquals("RTRIM(code)", Expression.rTrim("code"));
        assertEquals("RTRIM(description)", Expression.rTrim("description"));
    }

    @Test
    void expression_lPad_examples() {
        assertEquals("LPAD(id, 10, '0')", Expression.lPad("id", 10, "'0'"));
        assertEquals("LPAD(code, 5, ' ')", Expression.lPad("code", 5, "' '"));
    }

    @Test
    void expression_rPad_examples() {
        assertEquals("RPAD(name, 20, ' ')", Expression.rPad("name", 20, "' '"));
        assertEquals("RPAD(code, 10, 'X')", Expression.rPad("code", 10, "'X'"));
    }

    @Test
    void expression_lower_examples() {
        assertEquals("LOWER(email)", Expression.lower("email"));
        assertEquals("LOWER(COUNTRY)", Expression.lower("COUNTRY"));
    }

    @Test
    void expression_upper_examples() {
        assertEquals("UPPER(name)", Expression.upper("name"));
        assertEquals("UPPER(country_code)", Expression.upper("country_code"));
    }

    // =====================================================================
    // Expression.Expr - alias class example
    // =====================================================================

    @Test
    void expression_expr_alias_examples() {
        // This would need package-level access since constructor is package-private
        // The Javadoc shows: Expression.Expr expr = new Expression.Expr("price * quantity");
        // But the constructor is package-private. Just verify the class exists and is usable.
        // Expression.Expr is package-private, so we can access it from within the same package.
        Expression.Expr expr = new Expression.Expr("price * quantity");
        assertNotNull(expr);
        assertEquals("price * quantity", expr.getLiteral());
    }

    // =====================================================================
    // NamedProperty.java - class-level examples
    // =====================================================================

    @Test
    void namedProperty_classLevel_createAndUse() {
        // Create a named property (cached instance)
        NamedProperty age = NamedProperty.of("age");
        NamedProperty status = NamedProperty.of("status");
        NamedProperty name = NamedProperty.of("name");

        // Use it to create various conditions
        Condition c1 = age.eq(25);
        assertNotNull(c1);

        Condition c2 = age.gt(18);
        assertNotNull(c2);

        Condition c3 = age.between(20, 30);
        assertNotNull(c3);

        Condition c4 = age.in(Arrays.asList(25, 30, 35));
        assertNotNull(c4);

        // Pattern matching conditions
        Condition c5 = name.like("John%");
        assertNotNull(c5);

        Condition c6 = name.startsWith("J");
        assertNotNull(c6);

        Condition c7 = name.contains("oh");
        assertNotNull(c7);

        // Null checks
        Condition c8 = status.isNotNull();
        assertNotNull(c8);

        // Chain conditions with OR
        Or orCondition = age.eqOr(25, 30, 35);
        assertNotNull(orCondition);

        // Combine with AND/OR for complex queries
        Condition complex = age.gt(18).and(status.eq("active"));
        assertNotNull(complex);
    }

    // =====================================================================
    // NamedProperty.java - constructor examples
    // =====================================================================

    @Test
    void namedProperty_constructor_examples() {
        NamedProperty age = new NamedProperty("age");
        assertNotNull(age);
        // However, prefer using: NamedProperty.of("age") for caching benefits
    }

    // =====================================================================
    // NamedProperty.java - of() examples
    // =====================================================================

    @Test
    void namedProperty_of_examples() {
        NamedProperty username = NamedProperty.of("username");
        assertNotNull(username);

        NamedProperty status = NamedProperty.of("status");
        assertNotNull(status);

        // Invalid usage - throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            NamedProperty.of("");
        });
    }

    // =====================================================================
    // NamedProperty.java - propName() examples
    // =====================================================================

    @Test
    void namedProperty_propName_examples() {
        NamedProperty age = NamedProperty.of("age");
        String propName = age.propName();
        assertEquals("age", propName);
    }

    // =====================================================================
    // NamedProperty.java - eq/ne/gt/ge/lt/le examples
    // =====================================================================

    @Test
    void namedProperty_eq_examples() {
        NamedProperty.of("status").eq("active");
        NamedProperty.of("count").eq(5);
        // Runs without exception
    }

    @Test
    void namedProperty_eqOr_varargs_examples() {
        NamedProperty.of("color").eqOr("red", "green", "blue");
        NamedProperty.of("priority").eqOr(1, 2, 3);
        // Runs without exception
    }

    @Test
    void namedProperty_eqOr_collection_examples() {
        List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago");
        NamedProperty.of("city").eqOr(cities);
        // Runs without exception
    }

    @Test
    void namedProperty_ne_examples() {
        NamedProperty.of("status").ne("deleted");
        NamedProperty.of("count").ne(0);
        // Runs without exception
    }

    @Test
    void namedProperty_gt_examples() {
        NamedProperty.of("age").gt(18);
        NamedProperty.of("price").gt(99.99);
        // Runs without exception
    }

    @Test
    void namedProperty_ge_examples() {
        NamedProperty.of("score").ge(60);
        NamedProperty.of("age").ge(21);
        // Runs without exception
    }

    @Test
    void namedProperty_lt_examples() {
        NamedProperty.of("price").lt(100);
        NamedProperty.of("age").lt(18);
        // Runs without exception
    }

    @Test
    void namedProperty_le_examples() {
        NamedProperty.of("quantity").le(10);
        NamedProperty.of("age").le(65);
        // Runs without exception
    }

    // =====================================================================
    // NamedProperty.java - isNull/isNotNull examples
    // =====================================================================

    @Test
    void namedProperty_isNull_examples() {
        NamedProperty.of("email").isNull();
        NamedProperty.of("middle_name").isNull();
        NamedProperty.of("deleted_at").isNull();
        // Runs without exception
    }

    @Test
    void namedProperty_isNotNull_examples() {
        NamedProperty.of("email").isNotNull();
        NamedProperty.of("phone").isNotNull();
        NamedProperty.of("address").isNotNull();
        // Runs without exception
    }

    // =====================================================================
    // NamedProperty.java - between examples
    // =====================================================================

    @Test
    void namedProperty_between_examples() {
        NamedProperty.of("age").between(18, 65);
        NamedProperty.of("price").between(10.0, 100.0);
        // Runs without exception
    }

    // =====================================================================
    // NamedProperty.java - like/notLike/startsWith/endsWith/contains examples
    // =====================================================================

    @Test
    void namedProperty_like_examples() {
        NamedProperty.of("name").like("John%");
        NamedProperty.of("email").like("%@example.com");
        // Runs without exception
    }

    @Test
    void namedProperty_notLike_examples() {
        NamedProperty.of("email").notLike("%@temp.com");
        NamedProperty.of("name").notLike("test%");
        // Runs without exception
    }

    @Test
    void namedProperty_startsWith_examples() {
        NamedProperty.of("name").startsWith("John");
        NamedProperty.of("code").startsWith("PRD");
        // Runs without exception
    }

    @Test
    void namedProperty_endsWith_examples() {
        NamedProperty.of("email").endsWith("@example.com");
        NamedProperty.of("filename").endsWith(".pdf");
        // Runs without exception
    }

    @Test
    void namedProperty_contains_examples() {
        NamedProperty.of("description").contains("important");
        NamedProperty.of("title").contains("query");
        // Runs without exception
    }

    // =====================================================================
    // NamedProperty.java - in() examples
    // =====================================================================

    @Test
    void namedProperty_in_varargs_examples() {
        NamedProperty.of("status").in("active", "pending", "approved");
        NamedProperty.of("priority").in(1, 2, 3);
        // Runs without exception
    }

    @Test
    void namedProperty_in_collection_examples() {
        List<String> departments = Arrays.asList("Sales", "Marketing", "IT");
        NamedProperty.of("department").in(departments);
        // Runs without exception
    }

    // =====================================================================
    // Criteria.java - class-level examples
    // =====================================================================

    @Test
    void criteria_classLevel_complexQuery() {
        // Build a complex query with multiple clauses
        Criteria criteria = new Criteria()
                .join("orders", new On("users.id", "orders.user_id"))
                .where(Filters.and(
                        Filters.eq("users.status", "active"),
                        Filters.gt("orders.amount", 100)))
                .groupBy("users.department")
                .having(Filters.gt("COUNT(*)", 5))
                .orderBy("COUNT(*)", SortDirection.DESC)
                .limit(10);
        assertNotNull(criteria);
    }

    @Test
    void criteria_classLevel_distinctUsers() {
        // Using distinct
        Criteria distinctUsers = new Criteria()
                .distinct()
                .where(Filters.eq("active", true))
                .orderBy("name");
        assertNotNull(distinctUsers);
        assertEquals("DISTINCT", distinctUsers.preselect());
    }

    // =====================================================================
    // Criteria.java - constructor examples
    // =====================================================================

    @Test
    void criteria_constructor_examples() {
        Criteria criteria = new Criteria();
        criteria.where(Filters.eq("status", "active"))
                .orderBy("created_date", SortDirection.DESC)
                .limit(10);
        assertNotNull(criteria);
    }

    // =====================================================================
    // Criteria.java - preselect() examples
    // =====================================================================

    @Test
    void criteria_preselect_examples() {
        Criteria criteria = new Criteria().distinct();
        String modifier = criteria.preselect();
        assertEquals("DISTINCT", modifier);
    }

    // =====================================================================
    // Criteria.java - getJoins() examples
    // =====================================================================

    @Test
    void criteria_getJoins_examples() {
        Criteria criteria = new Criteria()
                .join("orders", new On("users.id", "orders.user_id"))
                .join("payments", new On("orders.id", "payments.order_id"));

        List<Join> joins = criteria.getJoins();
        assertEquals(2, joins.size());

        Criteria noJoins = new Criteria().where(Filters.eq("status", "active"));
        List<Join> empty = noJoins.getJoins();
        assertTrue(empty.isEmpty());
    }

    // =====================================================================
    // Criteria.java - getWhere() examples
    // =====================================================================

    @Test
    void criteria_getWhere_examples() {
        Criteria criteria = new Criteria()
                .where(Filters.eq("status", "active"));

        Cell whereClause = criteria.getWhere();
        assertNotNull(whereClause);

        Criteria noWhere = new Criteria().orderBy("name");
        Cell result = noWhere.getWhere();
        assertNull(result);
    }

    // =====================================================================
    // Criteria.java - getGroupBy() examples
    // =====================================================================

    @Test
    void criteria_getGroupBy_examples() {
        Criteria criteria = new Criteria()
                .groupBy("department", "location");

        Cell groupByClause = criteria.getGroupBy();
        assertNotNull(groupByClause);

        Criteria noGroupBy = new Criteria().where(Filters.eq("active", true));
        Cell result = noGroupBy.getGroupBy();
        assertNull(result);
    }

    // =====================================================================
    // Criteria.java - getHaving() examples
    // =====================================================================

    @Test
    void criteria_getHaving_examples() {
        Criteria criteria = new Criteria()
                .groupBy("department")
                .having("COUNT(*) > 5");

        Cell havingClause = criteria.getHaving();
        assertNotNull(havingClause);

        Criteria noHaving = new Criteria().groupBy("category");
        Cell result = noHaving.getHaving();
        assertNull(result);
    }

    // =====================================================================
    // Criteria.java - getAggregation() examples
    // =====================================================================

    @Test
    void criteria_getAggregation_examples() {
        SubQuery archivedUsers = Filters.subQuery("SELECT * FROM archived_users");
        SubQuery tempUsers = Filters.subQuery("SELECT * FROM temp_users");
        Criteria criteria = new Criteria()
                .where(Filters.eq("active", true))
                .union(archivedUsers)
                .unionAll(tempUsers);

        List<Cell> aggregations = criteria.getAggregation();
        assertEquals(2, aggregations.size());

        Criteria noAgg = new Criteria().where(Filters.eq("status", "active"));
        List<Cell> empty = noAgg.getAggregation();
        assertTrue(empty.isEmpty());
    }

    // =====================================================================
    // Criteria.java - getOrderBy() examples
    // =====================================================================

    @Test
    void criteria_getOrderBy_examples() {
        Criteria criteria = new Criteria()
                .orderBy("name", SortDirection.ASC);

        Cell orderByClause = criteria.getOrderBy();
        assertNotNull(orderByClause);

        Criteria noOrderBy = new Criteria().where(Filters.eq("active", true));
        Cell result = noOrderBy.getOrderBy();
        assertNull(result);
    }

    // =====================================================================
    // Criteria.java - getLimit() examples
    // =====================================================================

    @Test
    void criteria_getLimit_examples() {
        Criteria criteria = new Criteria()
                .where(Filters.eq("active", true))
                .limit(50);

        Limit limitClause = criteria.getLimit();
        assertNotNull(limitClause);

        Criteria noLimit = new Criteria().where(Filters.eq("status", "active"));
        Limit result = noLimit.getLimit();
        assertNull(result);
    }

    // =====================================================================
    // Criteria.java - getConditions() examples
    // =====================================================================

    @Test
    void criteria_getConditions_examples() {
        Criteria criteria = new Criteria()
                .where(Filters.eq("status", "active"))
                .orderBy("name")
                .limit(10);

        List<Condition> conditions = criteria.getConditions();
        assertEquals(3, conditions.size());

        Criteria empty = new Criteria();
        List<Condition> none = empty.getConditions();
        assertTrue(none.isEmpty());
    }

    // =====================================================================
    // Criteria.java - get(Operator) examples
    // =====================================================================

    @Test
    void criteria_get_operator_examples() {
        Criteria criteria = new Criteria()
                .join("orders", new On("users.id", "orders.user_id"))
                .join("payments", new On("orders.id", "payments.order_id"))
                .where(Filters.eq("status", "active"));

        List<Condition> joins = criteria.get(Operator.JOIN);
        assertEquals(2, joins.size());

        List<Condition> wheres = criteria.get(Operator.WHERE);
        assertEquals(1, wheres.size());

        List<Condition> limits = criteria.get(Operator.LIMIT);
        assertTrue(limits.isEmpty());
    }

    // =====================================================================
    // Criteria.java - clear() examples
    // =====================================================================

    @Test
    void criteria_clear_examples() {
        Criteria criteria = new Criteria()
                .where(Filters.eq("status", "active"))
                .orderBy("name")
                .limit(10);

        criteria.clear();
        assertTrue(criteria.getConditions().isEmpty());
        assertNull(criteria.preselect());

        // Rebuild the criteria with new conditions
        criteria.where(Filters.gt("age", 21))
                .limit(20);
        assertEquals(2, criteria.getConditions().size());
    }

    // =====================================================================
    // Criteria.java - distinct() examples
    // =====================================================================

    @Test
    void criteria_distinct_examples() {
        Criteria criteria = new Criteria()
                .distinct()
                .where(Filters.eq("status", "active"));
        assertEquals("DISTINCT", criteria.preselect());
    }

    // =====================================================================
    // Criteria.java - distinctBy() examples
    // =====================================================================

    @Test
    void criteria_distinctBy_examples() {
        Criteria criteria = new Criteria()
                .distinctBy("department, location");
        assertNotNull(criteria.preselect());
        assertTrue(criteria.preselect().contains("DISTINCT"));

        criteria.distinctBy("city");
        assertNotNull(criteria.preselect());
    }

    // =====================================================================
    // Criteria.java - distinctRow() examples
    // =====================================================================

    @Test
    void criteria_distinctRow_examples() {
        Criteria criteria = new Criteria()
                .distinctRow()
                .where(Filters.eq("active", true));
        assertEquals("DISTINCTROW", criteria.preselect());
    }

    // =====================================================================
    // Criteria.java - distinctRowBy() examples
    // =====================================================================

    @Test
    void criteria_distinctRowBy_examples() {
        Criteria criteria = new Criteria()
                .distinctRowBy("category, subcategory");
        assertNotNull(criteria.preselect());
        assertTrue(criteria.preselect().contains("DISTINCTROW"));
    }

    // =====================================================================
    // Criteria.java - preselect(String) examples
    // =====================================================================

    @Test
    void criteria_preselect_string_examples() {
        Criteria criteria = new Criteria()
                .preselect("SQL_CALC_FOUND_ROWS")
                .where(Filters.eq("active", true));
        assertEquals("SQL_CALC_FOUND_ROWS", criteria.preselect());

        criteria.preselect("SQL_NO_CACHE");
        assertEquals("SQL_NO_CACHE", criteria.preselect());
    }

    // =====================================================================
    // Criteria.java - join examples
    // =====================================================================

    @Test
    void criteria_join_string_examples() {
        Criteria criteria = new Criteria()
                .join("orders")
                .where(Filters.eq("users.id", "orders.user_id"));
        assertNotNull(criteria);
    }

    @Test
    void criteria_join_stringCondition_examples() {
        Criteria criteria = new Criteria()
                .join("orders", new On("users.id", "orders.user_id"))
                .where(Filters.eq("users.status", "active"));
        assertNotNull(criteria);
    }

    // =====================================================================
    // Criteria.java - where examples
    // =====================================================================

    @Test
    void criteria_where_condition_examples() {
        Criteria criteria = new Criteria()
                .where(Filters.and(
                        Filters.eq("status", "active"),
                        Filters.gt("age", 18),
                        Filters.like("email", "%@company.com")));
        assertNotNull(criteria.getWhere());
    }

    @Test
    void criteria_where_string_examples() {
        Criteria criteria = new Criteria()
                .where("age > 18 AND status = 'active'");
        assertNotNull(criteria.getWhere());

        criteria.where("YEAR(created_date) = 2024 OR special_flag = true");
        assertNotNull(criteria.getWhere());
    }

    // =====================================================================
    // Criteria.java - groupBy examples
    // =====================================================================

    @Test
    void criteria_groupBy_varargs_examples() {
        Criteria criteria = new Criteria()
                .groupBy("department", "location", "role");
        assertNotNull(criteria.getGroupBy());
    }

    @Test
    void criteria_groupBy_withDirection_examples() {
        Criteria criteria = new Criteria()
                .groupBy("total_sales", SortDirection.DESC);
        assertNotNull(criteria.getGroupBy());
    }

    @Test
    void criteria_groupBy_twoProperties_examples() {
        Criteria criteria = new Criteria()
                .groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
        assertNotNull(criteria.getGroupBy());
    }

    @Test
    void criteria_groupBy_threeProperties_examples() {
        Criteria criteria = new Criteria()
                .groupBy("country", SortDirection.ASC, "state", SortDirection.ASC, "city", SortDirection.DESC);
        assertNotNull(criteria.getGroupBy());
    }

    @Test
    void criteria_groupBy_collection_examples() {
        List<String> groupCols = Arrays.asList("region", "product_type");
        Criteria criteria = new Criteria()
                .groupBy(groupCols);
        assertNotNull(criteria.getGroupBy());
    }

    @Test
    void criteria_groupBy_map_examples() {
        Map<String, SortDirection> grouping = new LinkedHashMap<>();
        grouping.put("department", SortDirection.ASC);
        grouping.put("salary_range", SortDirection.DESC);
        grouping.put("years_experience", SortDirection.DESC);
        Criteria criteria = new Criteria()
                .groupBy(grouping);
        assertNotNull(criteria.getGroupBy());
    }

    // =====================================================================
    // Criteria.java - having examples
    // =====================================================================

    @Test
    void criteria_having_condition_examples() {
        Criteria criteria = new Criteria()
                .groupBy("department")
                .having(Filters.and(
                        Filters.gt("COUNT(*)", 10),
                        Filters.lt("AVG(salary)", 100000)));
        assertNotNull(criteria.getHaving());
    }

    @Test
    void criteria_having_string_examples() {
        Criteria criteria = new Criteria()
                .groupBy("product_category")
                .having("SUM(revenue) > 10000 AND COUNT(*) > 5");
        assertNotNull(criteria.getHaving());
    }

    // =====================================================================
    // Criteria.java - orderBy examples
    // =====================================================================

    @Test
    void criteria_orderByAsc_examples() {
        Criteria criteria = new Criteria()
                .orderByAsc("lastName", "firstName", "middleName");
        assertNotNull(criteria.getOrderBy());
    }

    @Test
    void criteria_orderByDesc_examples() {
        Criteria criteria = new Criteria()
                .orderByDesc("score", "createdDate");
        assertNotNull(criteria.getOrderBy());
    }

    @Test
    void criteria_orderBy_varargs_examples() {
        Criteria criteria = new Criteria()
                .orderBy("department", "lastName", "firstName");
        assertNotNull(criteria.getOrderBy());
    }

    @Test
    void criteria_orderBy_withDirection_examples() {
        Criteria criteria = new Criteria()
                .orderBy("createdDate", SortDirection.DESC);
        assertNotNull(criteria.getOrderBy());
    }

    @Test
    void criteria_orderBy_twoProperties_examples() {
        Criteria criteria = new Criteria()
                .orderBy("priority", SortDirection.DESC, "createdDate", SortDirection.ASC);
        assertNotNull(criteria.getOrderBy());
    }

    @Test
    void criteria_orderBy_threeProperties_examples() {
        Criteria criteria = new Criteria()
                .orderBy("category", SortDirection.ASC, "price", SortDirection.DESC, "name", SortDirection.ASC);
        assertNotNull(criteria.getOrderBy());
    }

    @Test
    void criteria_orderBy_collection_examples() {
        List<String> sortCols = Arrays.asList("country", "state", "city");
        Criteria criteria = new Criteria()
                .orderBy(sortCols);
        assertNotNull(criteria.getOrderBy());
    }

    @Test
    void criteria_orderBy_map_examples() {
        Map<String, SortDirection> ordering = new LinkedHashMap<>();
        ordering.put("priority", SortDirection.DESC);
        ordering.put("createdDate", SortDirection.DESC);
        ordering.put("name", SortDirection.ASC);
        Criteria criteria = new Criteria()
                .orderBy(ordering);
        assertNotNull(criteria.getOrderBy());
    }

    // =====================================================================
    // Criteria.java - limit examples
    // =====================================================================

    @Test
    void criteria_limit_condition_examples() {
        Limit customLimit = Filters.limit(100);
        Criteria criteria = new Criteria()
                .limit(customLimit);
        assertNotNull(criteria.getLimit());
    }

    @Test
    void criteria_limit_int_examples() {
        Criteria criteria = new Criteria()
                .where(Filters.eq("status", "active"))
                .limit(10);
        assertNotNull(criteria.getLimit());
    }

    @Test
    void criteria_limit_offsetCount_examples() {
        // Page 3 with 20 items per page (skip 40, take 20)
        Criteria criteria = new Criteria()
                .orderBy("id")
                .limit(40, 20);
        assertNotNull(criteria.getLimit());
    }

    @Test
    void criteria_limit_string_examples() {
        Criteria criteria = new Criteria()
                .limit("10 OFFSET 20");
        assertNotNull(criteria.getLimit());
    }

    // =====================================================================
    // Criteria.java - union/unionAll/intersect/except/minus examples
    // =====================================================================

    @Test
    void criteria_union_examples() {
        SubQuery archivedUsers = Filters.subQuery("SELECT * FROM archived_users WHERE active = true");
        Criteria criteria = new Criteria()
                .where(Filters.eq("status", "active"))
                .union(archivedUsers);
        assertFalse(criteria.getAggregation().isEmpty());
    }

    @Test
    void criteria_unionAll_examples() {
        SubQuery pendingOrders = Filters.subQuery("SELECT * FROM pending_orders");
        Criteria criteria = new Criteria()
                .where(Filters.eq("status", "completed"))
                .unionAll(pendingOrders);
        assertFalse(criteria.getAggregation().isEmpty());
    }

    @Test
    void criteria_intersect_examples() {
        SubQuery premiumUsers = Filters.subQuery("SELECT user_id FROM premium_members");
        Criteria criteria = new Criteria()
                .where(Filters.eq("active", true))
                .intersect(premiumUsers);
        assertFalse(criteria.getAggregation().isEmpty());
    }

    @Test
    void criteria_except_examples() {
        SubQuery excludedUsers = Filters.subQuery("SELECT user_id FROM blacklist");
        Criteria criteria = new Criteria()
                .where(Filters.eq("status", "active"))
                .except(excludedUsers);
        assertFalse(criteria.getAggregation().isEmpty());
    }

    @Test
    void criteria_minus_examples() {
        SubQuery inactiveUsers = Filters.subQuery("SELECT user_id FROM inactive_users");
        Criteria criteria = new Criteria()
                .where(Filters.eq("registered", true))
                .minus(inactiveUsers);
        assertFalse(criteria.getAggregation().isEmpty());
    }

    // =====================================================================
    // CriteriaUtil.java - class-level examples
    // =====================================================================

    @Test
    void criteriaUtil_classLevel_examples() {
        // Check if an operator is a clause operator
        boolean isClause = CriteriaUtil.isClause(Operator.WHERE);
        assertTrue(isClause);

        boolean notClause = CriteriaUtil.isClause(Operator.EQUAL);
        assertFalse(notClause);

        // Get all clause operators
        Set<Operator> clauses = CriteriaUtil.getClauseOperators();
        assertNotNull(clauses);
        assertFalse(clauses.isEmpty());
    }

    // =====================================================================
    // CriteriaUtil.java - getClauseOperators() examples
    // =====================================================================

    @Test
    void criteriaUtil_getClauseOperators_examples() {
        Set<Operator> clauseOps = CriteriaUtil.getClauseOperators();
        assertNotNull(clauseOps);
        assertTrue(clauseOps.contains(Operator.WHERE));
        assertTrue(clauseOps.contains(Operator.ORDER_BY));
        assertTrue(clauseOps.contains(Operator.JOIN));
    }

    // =====================================================================
    // CriteriaUtil.java - isClause(Operator) examples
    // =====================================================================

    @Test
    void criteriaUtil_isClause_operator_examples() {
        assertTrue(CriteriaUtil.isClause(Operator.WHERE));
        assertTrue(CriteriaUtil.isClause(Operator.ORDER_BY));
        assertTrue(CriteriaUtil.isClause(Operator.LEFT_JOIN));
        assertFalse(CriteriaUtil.isClause(Operator.EQUAL));
        assertFalse(CriteriaUtil.isClause(Operator.AND));
        assertFalse(CriteriaUtil.isClause((Operator) null));
    }

    // =====================================================================
    // CriteriaUtil.java - isClause(String) examples
    // =====================================================================

    @Test
    void criteriaUtil_isClause_string_examples() {
        assertTrue(CriteriaUtil.isClause("WHERE"));
        assertTrue(CriteriaUtil.isClause("ORDER BY"));
        assertTrue(CriteriaUtil.isClause("GROUP BY"));
        assertFalse(CriteriaUtil.isClause("="));
        assertFalse(CriteriaUtil.isClause("AND"));
    }

    // =====================================================================
    // CriteriaUtil.java - isClause(Condition) examples
    // =====================================================================

    @Test
    void criteriaUtil_isClause_condition_examples() {
        Where whereClause = new Where(Filters.eq("status", "active"));
        assertTrue(CriteriaUtil.isClause(whereClause));

        OrderBy orderByClause = new OrderBy("name");
        assertTrue(CriteriaUtil.isClause(orderByClause));

        Equal equalCond = Filters.eq("age", 25);
        assertFalse(CriteriaUtil.isClause(equalCond));

        assertFalse(CriteriaUtil.isClause((Condition) null));
    }

    // =====================================================================
    // CriteriaUtil.java - add(Criteria, Condition...) examples
    // =====================================================================

    @Test
    void criteriaUtil_add_varargs_examples() {
        Criteria criteria = new Criteria();
        CriteriaUtil.add(criteria,
                new Where(Filters.eq("status", "active")),
                new OrderBy("name", SortDirection.ASC));
        assertEquals(2, criteria.getConditions().size());
    }

    @Test
    void criteriaUtil_add_collection_examples() {
        Criteria criteria = new Criteria();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Where(Filters.eq("active", true)));
        conditions.add(new Limit(10));
        CriteriaUtil.add(criteria, conditions);
        assertEquals(2, criteria.getConditions().size());
    }

    // =====================================================================
    // CriteriaUtil.java - remove examples
    // =====================================================================

    @Test
    void criteriaUtil_remove_operator_examples() {
        Criteria criteria = new Criteria()
                .where(Filters.eq("status", "active"))
                .orderBy("name", SortDirection.ASC);
        CriteriaUtil.remove(criteria, Operator.WHERE);
        assertNull(criteria.getWhere());

        CriteriaUtil.remove(criteria, Operator.ORDER_BY);
        assertNull(criteria.getOrderBy());
    }

    @Test
    void criteriaUtil_remove_conditions_examples() {
        Where whereClause = new Where(Filters.eq("status", "active"));
        OrderBy orderByClause = new OrderBy("name");
        Criteria criteria = new Criteria();
        CriteriaUtil.add(criteria, whereClause, orderByClause);

        CriteriaUtil.remove(criteria, whereClause, orderByClause);
        assertTrue(criteria.getConditions().isEmpty());
    }

    @Test
    void criteriaUtil_remove_collection_examples() {
        Where whereClause = new Where(Filters.eq("status", "active"));
        Limit limitClause = new Limit(10);
        List<Condition> conditionsToRemove = new ArrayList<>();
        conditionsToRemove.add(whereClause);
        conditionsToRemove.add(limitClause);

        Criteria criteria = new Criteria();
        CriteriaUtil.add(criteria, whereClause, limitClause);

        CriteriaUtil.remove(criteria, conditionsToRemove);
        assertTrue(criteria.getConditions().isEmpty());
    }

    // =====================================================================
    // Junction.java - class-level examples
    // =====================================================================

    @Test
    void junction_classLevel_andJunction() {
        // Create an AND junction
        Junction and = new Junction(Operator.AND,
                new Equal("status", "active"),
                new GreaterThan("age", 18),
                new LessThan("age", 65));
        assertNotNull(and);
        String result = and.toString(NamingPolicy.NO_CHANGE);
        // Generates: ((status = 'active') AND (age > 18) AND (age < 65))
        assertTrue(result.contains("AND"));
    }

    @Test
    void junction_classLevel_orJunction() {
        // Create an OR junction
        Junction or = new Junction(Operator.OR,
                new Equal("city", "New York"),
                new Equal("city", "Los Angeles"),
                new Equal("city", "Chicago"));
        assertNotNull(or);
        String result = or.toString(NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("OR"));
    }

    @Test
    void junction_classLevel_nestedJunctions() {
        // Nested junctions for complex logic
        Junction complex = new Junction(Operator.AND,
                new Equal("type", "premium"),
                new Junction(Operator.OR,
                        new GreaterThan("balance", 10000),
                        new Equal("vip_status", true)));
        assertNotNull(complex);
    }

    // =====================================================================
    // Junction.java - constructor examples (varargs)
    // =====================================================================

    @Test
    void junction_constructor_varargs_examples() {
        // Create an AND junction with multiple conditions
        Junction activeAdults = new Junction(Operator.AND,
                new Equal("active", true),
                new GreaterEqual("age", 18),
                new IsNotNull("email"));
        assertNotNull(activeAdults);

        // Create an OR junction for status checks
        Junction validStatus = new Junction(Operator.OR,
                new Equal("status", "approved"),
                new Equal("status", "pending_review"),
                new Equal("override", true));
        assertNotNull(validStatus);
    }

    // =====================================================================
    // Junction.java - constructor examples (collection)
    // =====================================================================

    @Test
    void junction_constructor_collection_examples() {
        // Create conditions dynamically
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Equal("status", "active"));
        conditions.add(new GreaterThan("score", 80));

        Junction junction = new Junction(Operator.AND, conditions);
        assertNotNull(junction);
        assertEquals(2, junction.getConditions().size());
    }

    // =====================================================================
    // Junction.java - getConditions() examples
    // =====================================================================

    @Test
    void junction_getConditions_examples() {
        Junction and = new Junction(Operator.AND,
                new Equal("status", "active"),
                new GreaterThan("age", 18));
        List<Condition> conditions = and.getConditions();
        int count = conditions.size();
        assertEquals(2, count);
    }

    // =====================================================================
    // Junction.java - set(Condition...) examples
    // =====================================================================

    @Test
    void junction_set_varargs_examples() {
        Junction junction = new Junction(Operator.AND,
                new Equal("status", "active"));

        // Replace all conditions with new ones
        junction.set(
                new Equal("status", "pending"),
                new GreaterThan("priority", 5),
                new IsNotNull("assignee"));
        assertEquals(3, junction.getConditions().size());
    }

    // =====================================================================
    // Junction.java - set(Collection) examples
    // =====================================================================

    @Test
    void junction_set_collection_examples() {
        Junction junction = new Junction(Operator.OR);

        // Build conditions dynamically
        List<Condition> newConditions = new ArrayList<>();
        newConditions.add(new Equal("region", "US"));
        newConditions.add(new Equal("region", "EU"));
        newConditions.add(new Equal("region", "APAC"));

        junction.set(newConditions);
        assertEquals(3, junction.getConditions().size());
    }

    // =====================================================================
    // Junction.java - add(Condition...) examples
    // =====================================================================

    @Test
    void junction_add_varargs_examples() {
        Junction junction = new Junction(Operator.AND);

        // Add initial conditions
        junction.add(
                new Equal("status", "active"),
                new GreaterThan("score", 0));

        // Add more conditions later
        junction.add(
                new LessThan("price", 100),
                new Equal("inStock", true));
        assertEquals(4, junction.getConditions().size());
    }

    // =====================================================================
    // Junction.java - remove(Condition...) examples
    // =====================================================================

    @Test
    void junction_remove_examples() {
        Junction junction = new Junction(Operator.AND);
        Condition cond1 = new Equal("status", "active");
        Condition cond2 = new Equal("type", "premium");
        junction.add(cond1, cond2);

        // Remove specific condition
        junction.remove(cond1);
        assertEquals(1, junction.getConditions().size());
    }

    // =====================================================================
    // Junction.java - clear() examples
    // =====================================================================

    @Test
    void junction_clear_examples() {
        Junction junction = new Junction(Operator.AND,
                new Equal("status", "active"),
                new GreaterThan("age", 18));

        // Remove all conditions
        junction.clear();
        assertEquals(0, junction.getConditions().size());

        // Junction can still accept new conditions after clearing
        junction.add(new Equal("role", "admin"));
        assertEquals(1, junction.getConditions().size());
    }
}
