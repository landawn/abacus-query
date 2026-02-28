package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.query.Filters;

/**
 * Verifies ALL Javadoc {@code <pre>{@code ...}</pre>} examples from the 26 condition
 * comparison / logical / range files compile and run correctly, and asserts
 * expected toString() output wherever the Javadoc documents it.
 */
public class JavadocExamplesConditionComparisonTest {

    // =====================================================================
    // 1. Equal.java
    // =====================================================================

    @Test
    public void testEqual_classLevel_simpleEquality() {
        Equal statusCheck = new Equal("status", "active");
        assertNotNull(statusCheck);
        // SQL comment says: status = 'active'
        assertEquals("status = 'active'", statusCheck.toString());
    }

    @Test
    public void testEqual_classLevel_numericComparison() {
        Equal idCheck = new Equal("userId", 12345);
        assertNotNull(idCheck);
        assertEquals("userId = 12345", idCheck.toString());
    }

    @Test
    public void testEqual_classLevel_dateComparison() {
        Equal dateCheck = new Equal("createdDate", LocalDate.of(2024, 1, 1));
        assertNotNull(dateCheck);
        // toString will use LocalDate.toString() => '2024-01-01'
    }

    @Test
    public void testEqual_classLevel_nullCheck() {
        Equal nullCheck = new Equal("deletedDate", null);
        assertNotNull(nullCheck);
        assertEquals("deletedDate = null", nullCheck.toString());
    }

    @Test
    public void testEqual_classLevel_subqueryComparison() {
        SubQuery maxSalary = Filters.subQuery("SELECT MAX(salary) FROM employees");
        Equal maxSalaryCheck = new Equal("salary", maxSalary);
        assertNotNull(maxSalaryCheck);
    }

    @Test
    public void testEqual_constructor_stringEquality() {
        Equal nameCheck = new Equal("name", "John");
        assertNotNull(nameCheck);
        assertEquals("name = 'John'", nameCheck.toString());
    }

    @Test
    public void testEqual_constructor_numericEquality() {
        Equal countCheck = new Equal("count", 0);
        assertNotNull(countCheck);
        assertEquals("count = 0", countCheck.toString());
    }

    @Test
    public void testEqual_constructor_booleanEquality() {
        Equal activeCheck = new Equal("isActive", true);
        assertNotNull(activeCheck);
        assertEquals("isActive = true", activeCheck.toString());
    }

    @Test
    public void testEqual_constructor_dateEquality() {
        Equal dateCheck = new Equal("birthDate", LocalDate.of(1990, 1, 1));
        assertNotNull(dateCheck);
    }

    @Test
    public void testEqual_constructor_subqueryEquality() {
        SubQuery avgSalary = Filters.subQuery("SELECT AVG(salary) FROM employees");
        Equal avgCheck = new Equal("salary", avgSalary);
        assertNotNull(avgCheck);
    }

    // =====================================================================
    // 2. NotEqual.java
    // =====================================================================

    @Test
    public void testNotEqual_classLevel_simpleNotEqual() {
        NotEqual condition1 = new NotEqual("status", "deleted");
        assertNotNull(condition1);
        assertEquals("status != 'deleted'", condition1.toString());
    }

    @Test
    public void testNotEqual_classLevel_numericComparison() {
        NotEqual condition2 = new NotEqual("quantity", 0);
        assertNotNull(condition2);
        assertEquals("quantity != 0", condition2.toString());
    }

    @Test
    public void testNotEqual_classLevel_stringComparison() {
        NotEqual condition3 = new NotEqual("assignee", "admin");
        assertNotNull(condition3);
        assertEquals("assignee != 'admin'", condition3.toString());
    }

    @Test
    public void testNotEqual_classLevel_dateStringComparison() {
        NotEqual condition4 = new NotEqual("created", "2024-01-01");
        assertNotNull(condition4);
        assertEquals("created != '2024-01-01'", condition4.toString());
    }

    @Test
    public void testNotEqual_constructor_excludeUser() {
        NotEqual notAdmin = new NotEqual("username", "admin");
        assertNotNull(notAdmin);
        assertEquals("username != 'admin'", notAdmin.toString());
    }

    @Test
    public void testNotEqual_constructor_excludeDefault() {
        NotEqual notDefault = new NotEqual("configuration", "default");
        assertNotNull(notDefault);
        assertEquals("configuration != 'default'", notDefault.toString());
    }

    @Test
    public void testNotEqual_constructor_filterOutZero() {
        NotEqual notZero = new NotEqual("balance", 0);
        assertNotNull(notZero);
        assertEquals("balance != 0", notZero.toString());
    }

    // =====================================================================
    // 3. GreaterThan.java
    // =====================================================================

    @Test
    public void testGreaterThan_classLevel_ageCheck() {
        GreaterThan adults = new GreaterThan("age", 18);
        assertNotNull(adults);
        assertEquals("age > 18", adults.toString());
    }

    @Test
    public void testGreaterThan_classLevel_priceCheck() {
        GreaterThan premium = new GreaterThan("price", 99.99);
        assertNotNull(premium);
        assertEquals("price > 99.99", premium.toString());
    }

    @Test
    public void testGreaterThan_classLevel_dateCheck() {
        GreaterThan afterStart = new GreaterThan("start_date", "2023-01-01");
        assertNotNull(afterStart);
        assertEquals("start_date > '2023-01-01'", afterStart.toString());
    }

    @Test
    public void testGreaterThan_classLevel_combineWithLessThan() {
        And priceRange = new And(
            new GreaterThan("price", 10.00),
            new LessThan("price", 100.00)
        );
        assertNotNull(priceRange);
        // Junction format: ((cond1) AND (cond2))
        assertEquals("((price > 10.0) AND (price < 100.0))", priceRange.toString());
    }

    @Test
    public void testGreaterThan_constructor_salaryCondition() {
        GreaterThan salaryCondition = new GreaterThan("salary", 50000);
        assertNotNull(salaryCondition);
        assertEquals("salary > 50000", salaryCondition.toString());
    }

    @Test
    public void testGreaterThan_constructor_temperatureCondition() {
        GreaterThan tempCondition = new GreaterThan("temperature", 100);
        assertNotNull(tempCondition);
        assertEquals("temperature > 100", tempCondition.toString());
    }

    @Test
    public void testGreaterThan_constructor_dateCondition() {
        GreaterThan dateCondition = new GreaterThan("expiryDate", LocalDate.of(2024, 12, 31));
        assertNotNull(dateCondition);
    }

    @Test
    public void testGreaterThan_constructor_subquery() {
        SubQuery avgPrice = Filters.subQuery("SELECT AVG(price) FROM products");
        GreaterThan aboveAverage = new GreaterThan("price", avgPrice);
        assertNotNull(aboveAverage);
    }

    // =====================================================================
    // 4. GreaterEqual.java
    // =====================================================================

    @Test
    public void testGreaterEqual_classLevel_ageLimit() {
        GreaterEqual ageLimit = new GreaterEqual("age", 18);
        assertNotNull(ageLimit);
        assertEquals("age >= 18", ageLimit.toString());
    }

    @Test
    public void testGreaterEqual_classLevel_priceMin() {
        GreaterEqual priceMin = new GreaterEqual("price", 99.99);
        assertNotNull(priceMin);
        assertEquals("price >= 99.99", priceMin.toString());
    }

    @Test
    public void testGreaterEqual_classLevel_startDate() {
        GreaterEqual startDate = new GreaterEqual("start_date", "2023-01-01");
        assertNotNull(startDate);
        assertEquals("start_date >= '2023-01-01'", startDate.toString());
    }

    @Test
    public void testGreaterEqual_classLevel_inclusiveRange() {
        And priceRange = new And(
            new GreaterEqual("price", 10.00),
            new LessEqual("price", 100.00)
        );
        assertNotNull(priceRange);
        assertEquals("((price >= 10.0) AND (price <= 100.0))", priceRange.toString());
    }

    @Test
    public void testGreaterEqual_constructor_salaryCondition() {
        GreaterEqual salaryCondition = new GreaterEqual("salary", 50000);
        assertNotNull(salaryCondition);
        assertEquals("salary >= 50000", salaryCondition.toString());
    }

    @Test
    public void testGreaterEqual_constructor_scoreCondition() {
        GreaterEqual scoreCondition = new GreaterEqual("score", 60);
        assertNotNull(scoreCondition);
        assertEquals("score >= 60", scoreCondition.toString());
    }

    @Test
    public void testGreaterEqual_constructor_dateCondition() {
        GreaterEqual dateCondition = new GreaterEqual("expiryDate", LocalDate.now());
        assertNotNull(dateCondition);
    }

    @Test
    public void testGreaterEqual_constructor_subquery() {
        SubQuery avgPrice = Filters.subQuery("SELECT AVG(price) FROM products");
        GreaterEqual atOrAboveAverage = new GreaterEqual("price", avgPrice);
        assertNotNull(atOrAboveAverage);
    }

    // =====================================================================
    // 5. LessThan.java
    // =====================================================================

    @Test
    public void testLessThan_classLevel_underAge() {
        LessThan underAge = new LessThan("age", 18);
        assertNotNull(underAge);
        assertEquals("age < 18", underAge.toString());
    }

    @Test
    public void testLessThan_classLevel_priceLimit() {
        LessThan priceLimit = new LessThan("price", 99.99);
        assertNotNull(priceLimit);
        assertEquals("price < 99.99", priceLimit.toString());
    }

    @Test
    public void testLessThan_classLevel_beforeDeadline() {
        LessThan beforeDeadline = new LessThan("submit_date", "2023-12-31");
        assertNotNull(beforeDeadline);
        assertEquals("submit_date < '2023-12-31'", beforeDeadline.toString());
    }

    @Test
    public void testLessThan_classLevel_combineWithGreaterThan() {
        And priceRange = new And(
            new GreaterThan("price", 10.00),
            new LessThan("price", 100.00)
        );
        assertNotNull(priceRange);
        assertEquals("((price > 10.0) AND (price < 100.0))", priceRange.toString());
    }

    @Test
    public void testLessThan_constructor_minorCheck() {
        LessThan minorCheck = new LessThan("age", 18);
        assertNotNull(minorCheck);
        assertEquals("age < 18", minorCheck.toString());
    }

    @Test
    public void testLessThan_constructor_salaryLimit() {
        LessThan salaryLimit = new LessThan("salary", 50000);
        assertNotNull(salaryLimit);
        assertEquals("salary < 50000", salaryLimit.toString());
    }

    @Test
    public void testLessThan_constructor_expiringItems() {
        LessThan expiringItems = new LessThan("expiry_date", LocalDate.now().plusDays(1));
        assertNotNull(expiringItems);
    }

    @Test
    public void testLessThan_constructor_freezing() {
        LessThan freezing = new LessThan("temperature", 0);
        assertNotNull(freezing);
        assertEquals("temperature < 0", freezing.toString());
    }

    @Test
    public void testLessThan_constructor_subquery() {
        SubQuery avgPrice = Filters.subQuery("SELECT AVG(price) FROM products");
        LessThan belowAverage = new LessThan("price", avgPrice);
        assertNotNull(belowAverage);
    }

    // =====================================================================
    // 6. LessEqual.java
    // =====================================================================

    @Test
    public void testLessEqual_classLevel_ageLimit() {
        LessEqual ageLimit = new LessEqual("age", 18);
        assertNotNull(ageLimit);
        assertEquals("age <= 18", ageLimit.toString());
    }

    @Test
    public void testLessEqual_classLevel_priceLimit() {
        LessEqual priceLimit = new LessEqual("price", 99.99);
        assertNotNull(priceLimit);
        assertEquals("price <= 99.99", priceLimit.toString());
    }

    @Test
    public void testLessEqual_classLevel_deadline() {
        LessEqual deadline = new LessEqual("submit_date", "2023-12-31");
        assertNotNull(deadline);
        assertEquals("submit_date <= '2023-12-31'", deadline.toString());
    }

    @Test
    public void testLessEqual_classLevel_inclusiveRange() {
        And priceRange = new And(
            new GreaterEqual("price", 10.00),
            new LessEqual("price", 100.00)
        );
        assertNotNull(priceRange);
        assertEquals("((price >= 10.0) AND (price <= 100.0))", priceRange.toString());
    }

    @Test
    public void testLessEqual_constructor_stockLimit() {
        LessEqual stockLimit = new LessEqual("quantity", 100);
        assertNotNull(stockLimit);
        assertEquals("quantity <= 100", stockLimit.toString());
    }

    @Test
    public void testLessEqual_constructor_todayOrEarlier() {
        LessEqual todayOrEarlier = new LessEqual("order_date", LocalDate.now());
        assertNotNull(todayOrEarlier);
    }

    @Test
    public void testLessEqual_constructor_maxDiscount() {
        LessEqual maxDiscount = new LessEqual("discount_percent", 50);
        assertNotNull(maxDiscount);
        assertEquals("discount_percent <= 50", maxDiscount.toString());
    }

    @Test
    public void testLessEqual_constructor_tempThreshold() {
        LessEqual tempThreshold = new LessEqual("temperature", 25.5);
        assertNotNull(tempThreshold);
        assertEquals("temperature <= 25.5", tempThreshold.toString());
    }

    @Test
    public void testLessEqual_constructor_subquery() {
        SubQuery avgPrice = Filters.subQuery("SELECT AVG(price) FROM products");
        LessEqual atOrBelowAverage = new LessEqual("price", avgPrice);
        assertNotNull(atOrBelowAverage);
    }

    // =====================================================================
    // 7. Like.java
    // =====================================================================

    @Test
    public void testLike_classLevel_startsWithJohn() {
        Like startsWithJohn = new Like("name", "John%");
        assertNotNull(startsWithJohn);
        assertEquals("name LIKE 'John%'", startsWithJohn.toString());
    }

    @Test
    public void testLike_classLevel_exampleEmails() {
        Like exampleEmails = new Like("email", "%@example.com");
        assertNotNull(exampleEmails);
        assertEquals("email LIKE '%@example.com'", exampleEmails.toString());
    }

    @Test
    public void testLike_classLevel_phoneProducts() {
        Like phoneProducts = new Like("product_name", "%phone%");
        assertNotNull(phoneProducts);
        assertEquals("product_name LIKE '%phone%'", phoneProducts.toString());
    }

    @Test
    public void testLike_classLevel_pattern() {
        Like pattern = new Like("word", "A___E");
        assertNotNull(pattern);
        assertEquals("word LIKE 'A___E'", pattern.toString());
    }

    @Test
    public void testLike_classLevel_complexPattern() {
        Like complexPattern = new Like("code", "PRD-20__-___");
        assertNotNull(complexPattern);
        assertEquals("code LIKE 'PRD-20__-___'", complexPattern.toString());
    }

    @Test
    public void testLike_constructor_startsWith() {
        Like startsWith = new Like("title", "The%");
        assertNotNull(startsWith);
        assertEquals("title LIKE 'The%'", startsWith.toString());
    }

    @Test
    public void testLike_constructor_endsWith() {
        Like endsWith = new Like("filename", "%.pdf");
        assertNotNull(endsWith);
        assertEquals("filename LIKE '%.pdf'", endsWith.toString());
    }

    @Test
    public void testLike_constructor_contains() {
        Like contains = new Like("description", "%important%");
        assertNotNull(contains);
        assertEquals("description LIKE '%important%'", contains.toString());
    }

    @Test
    public void testLike_constructor_specificCharPositions() {
        Like pattern = new Like("code", "A_B_C");
        assertNotNull(pattern);
        assertEquals("code LIKE 'A_B_C'", pattern.toString());
    }

    @Test
    public void testLike_constructor_mixedWildcards() {
        Like mixed = new Like("serial", "SN-%_____");
        assertNotNull(mixed);
        assertEquals("serial LIKE 'SN-%_____'", mixed.toString());
    }

    @Test
    public void testLike_constructor_emailDomain() {
        Like emailDomain = new Like("email", "%@%.com");
        assertNotNull(emailDomain);
        assertEquals("email LIKE '%@%.com'", emailDomain.toString());
    }

    @Test
    public void testLike_constructor_phonePattern() {
        Like phonePattern = new Like("phone", "(___) ___-____");
        assertNotNull(phonePattern);
        assertEquals("phone LIKE '(___) ___-____'", phonePattern.toString());
    }

    @Test
    public void testLike_constructor_escaped() {
        Like escaped = new Like("path", "%\\_%");
        assertNotNull(escaped);
        assertEquals("path LIKE '%\\_%'", escaped.toString());
    }

    // =====================================================================
    // 8. NotLike.java
    // =====================================================================

    @Test
    public void testNotLike_classLevel_excludeNames() {
        NotLike condition1 = new NotLike("name", "John%");
        assertNotNull(condition1);
        assertEquals("name NOT LIKE 'John%'", condition1.toString());
    }

    @Test
    public void testNotLike_classLevel_excludeEmails() {
        NotLike condition2 = new NotLike("email", "%@gmail.com");
        assertNotNull(condition2);
        assertEquals("email NOT LIKE '%@gmail.com'", condition2.toString());
    }

    @Test
    public void testNotLike_classLevel_excludeThreeLetterCodes() {
        NotLike condition3 = new NotLike("code", "___");
        assertNotNull(condition3);
        assertEquals("code NOT LIKE '___'", condition3.toString());
    }

    @Test
    public void testNotLike_constructor_excludeTemp() {
        NotLike notLike = new NotLike("productName", "%temp%");
        assertNotNull(notLike);
        assertEquals("productName NOT LIKE '%temp%'", notLike.toString());
    }

    @Test
    public void testNotLike_constructor_excludeTmp() {
        NotLike notLike2 = new NotLike("filename", "%.tmp");
        assertNotNull(notLike2);
        assertEquals("filename NOT LIKE '%.tmp'", notLike2.toString());
    }

    @Test
    public void testNotLike_constructor_excludeTest() {
        NotLike testExclude = new NotLike("code", "TEST%");
        assertNotNull(testExclude);
        assertEquals("code NOT LIKE 'TEST%'", testExclude.toString());
    }

    // =====================================================================
    // 9. And.java
    // =====================================================================

    @Test
    public void testAnd_classLevel_multipleConditions() {
        And and = new And(
            Filters.eq("status", "active"),
            Filters.gt("age", 18),
            Filters.lt("age", 65)
        );
        assertNotNull(and);
        assertEquals("((status = 'active') AND (age > 18) AND (age < 65))", and.toString());
    }

    @Test
    public void testAnd_classLevel_chainAdditional() {
        And and = new And(
            Filters.eq("status", "active"),
            Filters.gt("age", 18),
            Filters.lt("age", 65)
        );
        And extended = and.and(Filters.eq("country", "USA"));
        assertNotNull(extended);
        assertEquals("((status = 'active') AND (age > 18) AND (age < 65) AND (country = 'USA'))", extended.toString());
    }

    @Test
    public void testAnd_classLevel_fromCollection() {
        List<Condition> conditions = Arrays.asList(
            Filters.isNotNull("email"),
            Filters.eq("verified", true)
        );
        And fromList = new And(conditions);
        assertNotNull(fromList);
    }

    @Test
    public void testAnd_constructor_simpleAnd() {
        And and = new And(
            Filters.eq("department", "Sales"),
            Filters.ge("salary", 50000)
        );
        assertNotNull(and);
        assertEquals("((department = 'Sales') AND (salary >= 50000))", and.toString());
    }

    @Test
    public void testAnd_constructor_complexAnd() {
        And complex = new And(
            Filters.eq("status", "active"),
            Filters.between("age", 25, 65),
            Filters.in("role", Arrays.asList("Manager", "Director")),
            Filters.isNotNull("email")
        );
        assertNotNull(complex);
    }

    @Test
    public void testAnd_collectionConstructor_dynamicConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(Filters.eq("status", "active"));
        conditions.add(Filters.isNotNull("email"));
        boolean includeAgeFilter = true;
        if (includeAgeFilter) {
            conditions.add(Filters.gt("age", 21));
        }
        And and = new And(conditions);
        assertNotNull(and);
    }

    @Test
    public void testAnd_andMethod_chaining() {
        And and = new And(Filters.eq("status", "active"));
        And extended = and
            .and(Filters.gt("score", 80))
            .and(Filters.lt("attempts", 3))
            .and(Filters.eq("verified", true));
        assertNotNull(extended);
        assertEquals("((status = 'active') AND (score > 80) AND (attempts < 3) AND (verified = true))", extended.toString());
    }

    // =====================================================================
    // 10. Or.java
    // =====================================================================

    @Test
    public void testOr_classLevel_multipleConditions() {
        Or or = new Or(
            Filters.eq("status", "active"),
            Filters.eq("status", "pending"),
            Filters.eq("status", "review")
        );
        assertNotNull(or);
        assertEquals("((status = 'active') OR (status = 'pending') OR (status = 'review'))", or.toString());
    }

    @Test
    public void testOr_classLevel_fluent() {
        Or or2 = new Or(Filters.gt("age", 65))
            .or(Filters.lt("age", 18));
        assertNotNull(or2);
        assertEquals("((age > 65) OR (age < 18))", or2.toString());
    }

    @Test
    public void testOr_constructor_specificCities() {
        Or or = new Or(
            Filters.eq("city", "New York"),
            Filters.eq("city", "Los Angeles"),
            Filters.eq("city", "Chicago")
        );
        assertNotNull(or);
        assertEquals("((city = 'New York') OR (city = 'Los Angeles') OR (city = 'Chicago'))", or.toString());
    }

    @Test
    public void testOr_constructor_complexOrWithDifferentTypes() {
        Or complexOr = new Or(
            Filters.like("email", "%@gmail.com"),
            Filters.like("email", "%@yahoo.com"),
            Filters.isNull("email")
        );
        assertNotNull(complexOr);
        assertEquals("((email LIKE '%@gmail.com') OR (email LIKE '%@yahoo.com') OR (email IS NULL))", complexOr.toString());
    }

    @Test
    public void testOr_collectionConstructor_dynamicConditions() {
        List<Condition> conditions = new ArrayList<>();
        List<String> searchNames = Arrays.asList("name1", "name2");
        for (String name : searchNames) {
            conditions.add(Filters.like("name", "%" + name + "%"));
        }
        Or or = new Or(conditions);
        assertNotNull(or);
    }

    @Test
    public void testOr_orMethod_stepByStep() {
        Or or = new Or(Filters.eq("type", "A"))
            .or(Filters.eq("type", "B"))
            .or(Filters.eq("type", "C"));
        assertNotNull(or);
        assertEquals("((type = 'A') OR (type = 'B') OR (type = 'C'))", or.toString());
    }

    @Test
    public void testOr_orMethod_conditionalAdding() {
        boolean includeInactive = true;
        boolean includePending = true;

        Or baseOr = new Or(Filters.eq("status", "active"));
        if (includeInactive) {
            baseOr = baseOr.or(Filters.eq("status", "inactive"));
        }
        if (includePending) {
            baseOr = baseOr.or(Filters.eq("status", "pending"));
        }
        assertNotNull(baseOr);
    }

    // =====================================================================
    // 11. Not.java
    // =====================================================================

    @Test
    public void testNot_classLevel_notLike() {
        Like likeCondition = new Like("name", "%test%");
        Not notLike = new Not(likeCondition);
        assertNotNull(notLike);
        assertEquals("NOT name LIKE '%test%'", notLike.toString());
    }

    @Test
    public void testNot_classLevel_notIn() {
        In deptCondition = new In("department_id", Arrays.asList(10, 20, 30));
        Not notInDepts = new Not(deptCondition);
        assertNotNull(notInDepts);
        assertEquals("NOT department_id IN (10, 20, 30)", notInDepts.toString());
    }

    @Test
    public void testNot_classLevel_notComplexCondition() {
        And complexCondition = new And(
            new Equal("priority", "HIGH"),
            new Equal("status", "URGENT")
        );
        Not notUrgentHigh = new Not(complexCondition);
        assertNotNull(notUrgentHigh);
        assertEquals("NOT ((priority = 'HIGH') AND (status = 'URGENT'))", notUrgentHigh.toString());
    }

    @Test
    public void testNot_classLevel_notExists() {
        SubQuery hasOrders = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
        Exists existsCondition = new Exists(hasOrders);
        Not noOrders = new Not(existsCondition);
        assertNotNull(noOrders);
        assertEquals("NOT EXISTS (SELECT 1 FROM orders WHERE orders.customer_id = customers.id)", noOrders.toString());
    }

    @Test
    public void testNot_constructor_simpleNegation() {
        Equal isActive = new Equal("active", true);
        Not isInactive = new Not(isActive);
        assertNotNull(isInactive);
        assertEquals("NOT active = true", isInactive.toString());
    }

    @Test
    public void testNot_constructor_negatingBetween() {
        Between ageRange = new Between("age", 18, 65);
        Not outsideRange = new Not(ageRange);
        assertNotNull(outsideRange);
        assertEquals("NOT age BETWEEN 18 AND 65", outsideRange.toString());
    }

    @Test
    public void testNot_constructor_negatingOr() {
        Or multiStatus = new Or(
            new Equal("status", "PENDING"),
            new Equal("status", "PROCESSING")
        );
        Not notPendingOrProcessing = new Not(multiStatus);
        assertNotNull(notPendingOrProcessing);
        assertEquals("NOT ((status = 'PENDING') OR (status = 'PROCESSING'))", notPendingOrProcessing.toString());
    }

    // =====================================================================
    // 12. XOR.java
    // =====================================================================

    @Test
    public void testXOR_classLevel_basicUsage() {
        XOR xor = new XOR("usePasswordAuth", true);
        assertNotNull(xor);
    }

    @Test
    public void testXOR_classLevel_flagCheck() {
        XOR flagCheck = new XOR("isActive", 1);
        assertNotNull(flagCheck);
    }

    @Test
    public void testXOR_classLevel_portableMutuallyExclusive() {
        Or exclusiveOr = new Or(
            new And(Filters.eq("hasPasswordAuth", true), Filters.eq("hasBiometricAuth", false)),
            new And(Filters.eq("hasPasswordAuth", false), Filters.eq("hasBiometricAuth", true))
        );
        assertNotNull(exclusiveOr);
    }

    @Test
    public void testXOR_constructor_exclusiveAuth() {
        XOR exclusiveAuth = new XOR("usePasswordAuth", true);
        assertNotNull(exclusiveAuth);
    }

    @Test
    public void testXOR_constructor_numericXor() {
        XOR xorCheck = new XOR("flagA", 1);
        assertNotNull(xorCheck);
    }

    // =====================================================================
    // 13. In.java
    // =====================================================================

    @Test
    public void testIn_classLevel_statusCheck() {
        In statusCheck = new In("status", Arrays.asList("active", "pending", "approved"));
        assertNotNull(statusCheck);
        assertEquals("status IN ('active', 'pending', 'approved')", statusCheck.toString());
    }

    @Test
    public void testIn_classLevel_userFilter() {
        In userFilter = new In("user_id", Arrays.asList(1, 2, 3, 5, 8));
        assertNotNull(userFilter);
        assertEquals("user_id IN (1, 2, 3, 5, 8)", userFilter.toString());
    }

    @Test
    public void testIn_classLevel_categoryFilter() {
        Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers"));
        In categoryFilter = new In("category", categories);
        assertNotNull(categoryFilter);
        // Set ordering is non-deterministic, just verify it runs
    }

    @Test
    public void testIn_constructor_categoryFilter() {
        Set<String> categories = new HashSet<>(Arrays.asList("electronics", "computers", "phones"));
        In categoryFilter = new In("category", categories);
        assertNotNull(categoryFilter);
    }

    @Test
    public void testIn_constructor_idFilter() {
        List<Long> ids = Arrays.asList(101L, 102L, 103L);
        In idFilter = new In("product_id", ids);
        assertNotNull(idFilter);
        assertEquals("product_id IN (101, 102, 103)", idFilter.toString());
    }

    @Test
    public void testIn_constructor_priorityFilter() {
        List<String> priorities = Arrays.asList("HIGH", "CRITICAL");
        In priorityFilter = new In("priority", priorities);
        assertNotNull(priorityFilter);
        assertEquals("priority IN ('HIGH', 'CRITICAL')", priorityFilter.toString());
    }

    @Test
    public void testIn_getPropName() {
        In statusFilter = new In("status", Arrays.asList("active", "pending"));
        String propName = statusFilter.getPropName();
        assertEquals("status", propName);
    }

    @Test
    public void testIn_getValues() {
        In idFilter = new In("user_id", Arrays.asList(1, 2, 3, 5, 8));
        List<?> values = idFilter.getValues();
        assertEquals(Arrays.asList(1, 2, 3, 5, 8), values);

        In statusFilter = new In("status", Arrays.asList("active", "pending"));
        List<?> statuses = statusFilter.getValues();
        assertEquals(Arrays.asList("active", "pending"), statuses);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIn_setValues() {
        In statusFilter = new In("status", Arrays.asList("active", "pending"));
        statusFilter.setValues(Arrays.asList("approved", "completed"));
        assertEquals(Arrays.asList("approved", "completed"), statusFilter.getValues());

        // Preferred approach: create a new In instance
        In updatedFilter = new In("status", Arrays.asList("approved", "completed"));
        assertNotNull(updatedFilter);
    }

    // =====================================================================
    // 14. NotIn.java
    // =====================================================================

    @Test
    public void testNotIn_classLevel_excludeStatuses() {
        List<String> inactiveStatuses = Arrays.asList("deleted", "archived", "suspended");
        NotIn condition = new NotIn("status", inactiveStatuses);
        assertNotNull(condition);
        assertEquals("status NOT IN ('deleted', 'archived', 'suspended')", condition.toString());
    }

    @Test
    public void testNotIn_classLevel_excludeDepts() {
        Set<Integer> excludedDepts = new HashSet<>(Arrays.asList(10, 20, 30));
        NotIn deptCondition = new NotIn("department_id", excludedDepts);
        assertNotNull(deptCondition);
        // Set ordering is non-deterministic
    }

    @Test
    public void testNotIn_classLevel_excludeTestEmails() {
        List<String> testEmails = Arrays.asList("test@example.com", "demo@example.com");
        NotIn emailCondition = new NotIn("email", testEmails);
        assertNotNull(emailCondition);
    }

    @Test
    public void testNotIn_constructor_excludeCategories() {
        List<String> excludedCategories = Arrays.asList("discontinued", "internal", "test");
        NotIn notIn = new NotIn("category", excludedCategories);
        assertNotNull(notIn);
        assertEquals("category NOT IN ('discontinued', 'internal', 'test')", notIn.toString());
    }

    @Test
    public void testNotIn_constructor_excludeTestUsers() {
        Set<Integer> testUserIds = new HashSet<>(Arrays.asList(1, 2, 999));
        NotIn excludeUsers = new NotIn("user_id", testUserIds);
        assertNotNull(excludeUsers);
    }

    @Test
    public void testNotIn_getPropName() {
        NotIn excludeFilter = new NotIn("status", Arrays.asList("deleted", "archived"));
        String propName = excludeFilter.getPropName();
        assertEquals("status", propName);
    }

    @Test
    public void testNotIn_getValues() {
        NotIn excludeFilter = new NotIn("department_id", Arrays.asList(10, 20, 30));
        List<?> values = excludeFilter.getValues();
        assertEquals(Arrays.asList(10, 20, 30), values);

        NotIn statusFilter = new NotIn("status", Arrays.asList("deleted", "archived"));
        List<?> excluded = statusFilter.getValues();
        assertEquals(Arrays.asList("deleted", "archived"), excluded);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testNotIn_setValues() {
        NotIn statusFilter = new NotIn("status", Arrays.asList("deleted", "archived"));
        statusFilter.setValues(Arrays.asList("suspended", "banned"));
        assertEquals(Arrays.asList("suspended", "banned"), statusFilter.getValues());

        // Preferred approach: create a new NotIn instance
        NotIn updatedFilter = new NotIn("status", Arrays.asList("suspended", "banned"));
        assertNotNull(updatedFilter);
    }

    // =====================================================================
    // 15. InSubQuery.java
    // =====================================================================

    @Test
    public void testInSubQuery_classLevel_singleColumn() {
        SubQuery premiumCustomers = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
        InSubQuery condition = new InSubQuery("customer_id", premiumCustomers);
        assertNotNull(condition);
    }

    @Test
    public void testInSubQuery_classLevel_multipleColumns() {
        SubQuery validAssignments = Filters.subQuery("SELECT dept_id, location_id FROM allowed_assignments");
        InSubQuery multiColumn = new InSubQuery(Arrays.asList("department_id", "location_id"), validAssignments);
        assertNotNull(multiColumn);
    }

    @Test
    public void testInSubQuery_singleColumnConstructor_activeCategories() {
        SubQuery activeCategories = Filters.subQuery("SELECT category_id FROM categories WHERE active = true");
        InSubQuery condition = new InSubQuery("category_id", activeCategories);
        assertNotNull(condition);
    }

    @Test
    public void testInSubQuery_singleColumnConstructor_richDepts() {
        SubQuery richDepts = Filters.subQuery("SELECT dept_id FROM departments WHERE budget > 1000000");
        InSubQuery condition2 = new InSubQuery("department_id", richDepts);
        assertNotNull(condition2);
    }

    @Test
    public void testInSubQuery_multiColumnConstructor() {
        List<String> columns = Arrays.asList("dept_id", "loc_id");
        SubQuery validCombinations = Filters.subQuery(
            "SELECT department_id, location_id FROM dept_locations WHERE active = 'Y'"
        );
        InSubQuery condition = new InSubQuery(columns, validCombinations);
        assertNotNull(condition);
    }

    @Test
    public void testInSubQuery_getPropName() {
        SubQuery subQuery = Filters.subQuery("SELECT customer_id FROM customers WHERE status = 'premium'");
        InSubQuery condition = new InSubQuery("customer_id", subQuery);
        String propName = condition.getPropName();
        assertEquals("customer_id", propName);

        // For multi-column conditions, getPropName() returns null
        InSubQuery multiCol = new InSubQuery(Arrays.asList("dept_id", "loc_id"), subQuery);
        String name = multiCol.getPropName();
        assertNull(name);
    }

    @Test
    public void testInSubQuery_getPropNames() {
        SubQuery subQuery = Filters.subQuery("SELECT dept_id, loc_id FROM dept_locations");
        InSubQuery multiCol = new InSubQuery(Arrays.asList("department_id", "location_id"), subQuery);
        Collection<String> propNames = multiCol.getPropNames();
        assertEquals(Arrays.asList("department_id", "location_id"), new ArrayList<>(propNames));

        // For single-column conditions, getPropNames() returns null
        InSubQuery singleCol = new InSubQuery("customer_id", subQuery);
        Collection<String> names = singleCol.getPropNames();
        assertNull(names);
    }

    @Test
    public void testInSubQuery_getSubQuery() {
        SubQuery activeCategories = Filters.subQuery("SELECT category_id FROM categories WHERE active = true");
        InSubQuery condition = new InSubQuery("category_id", activeCategories);
        SubQuery retrieved = condition.getSubQuery();
        assertNotNull(retrieved);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testInSubQuery_setSubQuery() {
        SubQuery originalQuery = Filters.subQuery("SELECT id FROM users WHERE active = true");
        InSubQuery condition = new InSubQuery("user_id", originalQuery);

        SubQuery newQuery = Filters.subQuery("SELECT id FROM users WHERE role = 'admin'");
        condition.setSubQuery(newQuery);

        // Preferred approach: create a new InSubQuery instance
        InSubQuery updatedCondition = new InSubQuery("user_id", newQuery);
        assertNotNull(updatedCondition);
    }

    // =====================================================================
    // 16. NotInSubQuery.java
    // =====================================================================

    @Test
    public void testNotInSubQuery_classLevel_singleProperty() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);
        assertNotNull(condition);
    }

    @Test
    public void testNotInSubQuery_classLevel_multipleProperties() {
        List<String> props = Arrays.asList("firstName", "lastName");
        SubQuery subQuery2 = Filters.subQuery("SELECT fname, lname FROM blacklist");
        NotInSubQuery condition2 = new NotInSubQuery(props, subQuery2);
        assertNotNull(condition2);
    }

    @Test
    public void testNotInSubQuery_singleColumnConstructor_deletedItems() {
        SubQuery deletedItems = Filters.subQuery("SELECT id FROM deleted_items");
        NotInSubQuery condition = new NotInSubQuery("itemId", deletedItems);
        assertNotNull(condition);
    }

    @Test
    public void testNotInSubQuery_singleColumnConstructor_notHR() {
        SubQuery deptQuery = Filters.subQuery("SELECT user_id FROM dept_users WHERE dept = 'HR'");
        NotInSubQuery notHR = new NotInSubQuery("id", deptQuery);
        assertNotNull(notHR);
    }

    @Test
    public void testNotInSubQuery_multiColumnConstructor_restrictedLocations() {
        List<String> props = Arrays.asList("country", "city");
        SubQuery restricted = Filters.subQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(props, restricted);
        assertNotNull(condition);
    }

    @Test
    public void testNotInSubQuery_multiColumnConstructor_noDupes() {
        List<String> uniqueProps = Arrays.asList("firstName", "lastName", "email");
        SubQuery existing = Filters.subQuery("SELECT fname, lname, email FROM existing_users");
        NotInSubQuery noDupes = new NotInSubQuery(uniqueProps, existing);
        assertNotNull(noDupes);
    }

    @Test
    public void testNotInSubQuery_getPropName() {
        SubQuery subQuery = Filters.subQuery("SELECT id FROM inactive_users");
        NotInSubQuery condition = new NotInSubQuery("userId", subQuery);
        String propName = condition.getPropName();
        assertEquals("userId", propName);

        // For multi-property conditions, getPropName() returns null
        NotInSubQuery multiProp = new NotInSubQuery(Arrays.asList("country", "city"), subQuery);
        String name = multiProp.getPropName();
        assertNull(name);
    }

    @Test
    public void testNotInSubQuery_getPropNames() {
        SubQuery restricted = Filters.subQuery("SELECT country, city FROM restricted_locations");
        NotInSubQuery condition = new NotInSubQuery(Arrays.asList("country", "city"), restricted);
        Collection<String> propNames = condition.getPropNames();
        assertEquals(Arrays.asList("country", "city"), new ArrayList<>(propNames));

        // For single-property conditions, getPropNames() returns null
        NotInSubQuery singleProp = new NotInSubQuery("userId", restricted);
        Collection<String> names = singleProp.getPropNames();
        assertNull(names);
    }

    @Test
    public void testNotInSubQuery_getSubQuery() {
        SubQuery deletedItems = Filters.subQuery("SELECT id FROM deleted_items");
        NotInSubQuery condition = new NotInSubQuery("itemId", deletedItems);
        SubQuery retrieved = condition.getSubQuery();
        assertNotNull(retrieved);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testNotInSubQuery_setSubQuery() {
        SubQuery originalQuery = Filters.subQuery("SELECT id FROM blocked_users");
        NotInSubQuery condition = new NotInSubQuery("userId", originalQuery);

        SubQuery newQuery = Filters.subQuery("SELECT id FROM suspended_users");
        condition.setSubQuery(newQuery);

        // Preferred approach: create a new NotInSubQuery instance
        NotInSubQuery updatedCondition = new NotInSubQuery("userId", newQuery);
        assertNotNull(updatedCondition);
    }

    // =====================================================================
    // 17. Exists.java
    // =====================================================================

    @Test
    public void testExists_classLevel_hasOrders() {
        SubQuery orderExists = Filters.subQuery(
            "SELECT 1 FROM orders WHERE orders.customer_id = customers.id"
        );
        Exists hasOrders = new Exists(orderExists);
        assertNotNull(hasOrders);
    }

    @Test
    public void testExists_classLevel_hasReviews() {
        SubQuery reviewExists = Filters.subQuery(
            "SELECT 1 FROM reviews WHERE reviews.product_id = products.id"
        );
        Exists hasReviews = new Exists(reviewExists);
        assertNotNull(hasReviews);
    }

    @Test
    public void testExists_classLevel_hasEmployees() {
        SubQuery employeeExists = Filters.subQuery(
            "SELECT 1 FROM employees WHERE employees.dept_id = departments.id"
        );
        Exists hasEmployees = new Exists(employeeExists);
        assertNotNull(hasEmployees);
    }

    @Test
    public void testExists_constructor_hasSubordinates() {
        SubQuery subordinatesQuery = Filters.subQuery(
            "SELECT 1 FROM employees e2 WHERE e2.manager_id = e1.id"
        );
        Exists hasSubordinates = new Exists(subordinatesQuery);
        assertNotNull(hasSubordinates);
    }

    @Test
    public void testExists_constructor_inActiveOrder() {
        SubQuery activeOrderQuery = Filters.subQuery(
            "SELECT 1 FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE oi.product_id = products.id " +
            "AND o.status = 'active'"
        );
        Exists inActiveOrder = new Exists(activeOrderQuery);
        assertNotNull(inActiveOrder);
    }

    @Test
    public void testExists_constructor_isAdmin() {
        SubQuery permissionQuery = Filters.subQuery(
            "SELECT 1 FROM user_permissions up " +
            "WHERE up.user_id = users.id " +
            "AND up.permission = 'admin'"
        );
        Exists isAdmin = new Exists(permissionQuery);
        assertNotNull(isAdmin);
    }

    @Test
    public void testExists_constructor_deptHasEmployees() {
        SubQuery hasEmployees = Filters.subQuery("SELECT 1 FROM employees WHERE dept_id = departments.id");
        Exists deptHasEmployees = new Exists(hasEmployees);
        assertNotNull(deptHasEmployees);
    }

    // =====================================================================
    // 18. NotExists.java
    // =====================================================================

    @Test
    public void testNotExists_classLevel_noOrders() {
        SubQuery orderNotExists = Filters.subQuery(
            "SELECT 1 FROM orders WHERE orders.customer_id = customers.id"
        );
        NotExists noOrders = new NotExists(orderNotExists);
        assertNotNull(noOrders);
    }

    @Test
    public void testNotExists_classLevel_noReviews() {
        SubQuery reviewNotExists = Filters.subQuery(
            "SELECT 1 FROM reviews WHERE reviews.product_id = products.id"
        );
        NotExists noReviews = new NotExists(reviewNotExists);
        assertNotNull(noReviews);
    }

    @Test
    public void testNotExists_constructor_noProjects() {
        SubQuery projectCheck = Filters.subQuery(
            "SELECT 1 FROM project_assignments " +
            "WHERE project_assignments.employee_id = employees.id"
        );
        NotExists noProjects = new NotExists(projectCheck);
        assertNotNull(noProjects);
    }

    @Test
    public void testNotExists_constructor_noOrders() {
        SubQuery orderCheck = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
        NotExists noOrders = new NotExists(orderCheck);
        assertNotNull(noOrders);
    }

    @Test
    public void testNotExists_constructor_noReviews() {
        SubQuery reviewCheck = Filters.subQuery("SELECT 1 FROM reviews WHERE reviews.product_id = products.id");
        NotExists noReviews = new NotExists(reviewCheck);
        assertNotNull(noReviews);
    }

    @Test
    public void testNotExists_constructor_emptyDept() {
        SubQuery empCheck = Filters.subQuery("SELECT 1 FROM employees WHERE employees.dept_id = departments.id");
        NotExists emptyDept = new NotExists(empCheck);
        assertNotNull(emptyDept);
    }

    // =====================================================================
    // 19. Is.java
    // =====================================================================

    @Test
    public void testIs_classLevel_nullCheck() {
        Is nullCheck = new Is("email", null);
        assertNotNull(nullCheck);
        assertEquals("email IS null", nullCheck.toString());
    }

    @Test
    public void testIs_classLevel_customExpression() {
        Expression customExpr = Filters.expr("UNKNOWN");
        Is unknownCheck = new Is("status", customExpr);
        assertNotNull(unknownCheck);
        assertEquals("status IS UNKNOWN", unknownCheck.toString());
    }

    @Test
    public void testIs_constructor_nullPhoneNumber() {
        Is nullCheck = new Is("phone_number", null);
        assertNotNull(nullCheck);
        assertEquals("phone_number IS null", nullCheck.toString());
    }

    @Test
    public void testIs_constructor_nanExpression() {
        Expression nanExpr = Filters.expr("NAN");
        Is nanCheck = new Is("temperature", nanExpr);
        assertNotNull(nanCheck);
        assertEquals("temperature IS NAN", nanCheck.toString());
    }

    @Test
    public void testIs_constructor_unknownExpression() {
        Expression unknownExpr = Filters.expr("UNKNOWN");
        Is triStateCheck = new Is("verification_status", unknownExpr);
        assertNotNull(triStateCheck);
        assertEquals("verification_status IS UNKNOWN", triStateCheck.toString());
    }

    // =====================================================================
    // 20. IsNot.java
    // =====================================================================

    @Test
    public void testIsNot_classLevel_notNull() {
        IsNot notNull = new IsNot("email", null);
        assertNotNull(notNull);
        assertEquals("email IS NOT null", notNull.toString());
    }

    @Test
    public void testIsNot_classLevel_notUnknown() {
        Expression unknownExpr = Filters.expr("UNKNOWN");
        IsNot notUnknown = new IsNot("status", unknownExpr);
        assertNotNull(notUnknown);
        assertEquals("status IS NOT UNKNOWN", notUnknown.toString());
    }

    @Test
    public void testIsNot_constructor_notNullPhoneNumber() {
        IsNot notNull = new IsNot("phone_number", null);
        assertNotNull(notNull);
        assertEquals("phone_number IS NOT null", notNull.toString());
    }

    @Test
    public void testIsNot_constructor_notNaN() {
        Expression nanExpr = Filters.expr("NAN");
        IsNot notNaN = new IsNot("temperature", nanExpr);
        assertNotNull(notNaN);
        assertEquals("temperature IS NOT NAN", notNaN.toString());
    }

    @Test
    public void testIsNot_constructor_notPending() {
        Expression pendingExpr = Filters.expr("PENDING");
        IsNot notPending = new IsNot("order_status", pendingExpr);
        assertNotNull(notPending);
        assertEquals("order_status IS NOT PENDING", notPending.toString());
    }

    // =====================================================================
    // 21. IsNull.java
    // =====================================================================

    @Test
    public void testIsNull_classLevel_emailCheck() {
        IsNull emailCheck = new IsNull("email");
        assertNotNull(emailCheck);
        assertEquals("email IS NULL", emailCheck.toString());
    }

    @Test
    public void testIsNull_classLevel_phoneCheck() {
        IsNull phoneCheck = new IsNull("phone_number");
        assertNotNull(phoneCheck);
        assertEquals("phone_number IS NULL", phoneCheck.toString());
    }

    @Test
    public void testIsNull_classLevel_assigneeCheck() {
        IsNull assigneeCheck = new IsNull("assigned_to");
        assertNotNull(assigneeCheck);
        assertEquals("assigned_to IS NULL", assigneeCheck.toString());
    }

    @Test
    public void testIsNull_classLevel_combineWithOther() {
        And incompleteProfile = new And(
            new IsNull("profile_picture"),
            new IsNull("bio"),
            new IsNotNull("user_id")
        );
        assertNotNull(incompleteProfile);
    }

    @Test
    public void testIsNull_constructor_birthdateCheck() {
        IsNull birthdateCheck = new IsNull("birth_date");
        assertNotNull(birthdateCheck);
        assertEquals("birth_date IS NULL", birthdateCheck.toString());
    }

    @Test
    public void testIsNull_constructor_processedCheck() {
        IsNull processedCheck = new IsNull("processed_date");
        assertNotNull(processedCheck);
        assertEquals("processed_date IS NULL", processedCheck.toString());
    }

    @Test
    public void testIsNull_constructor_descCheck() {
        IsNull descCheck = new IsNull("description");
        assertNotNull(descCheck);
        assertEquals("description IS NULL", descCheck.toString());
    }

    @Test
    public void testIsNull_constructor_managerCheck() {
        IsNull managerCheck = new IsNull("manager_id");
        assertNotNull(managerCheck);
        assertEquals("manager_id IS NULL", managerCheck.toString());
    }

    @Test
    public void testIsNull_constructor_middleNameCheck() {
        IsNull middleNameCheck = new IsNull("middle_name");
        assertNotNull(middleNameCheck);
        assertEquals("middle_name IS NULL", middleNameCheck.toString());
    }

    // =====================================================================
    // 22. IsNotNull.java
    // =====================================================================

    @Test
    public void testIsNotNull_classLevel_emailCheck() {
        IsNotNull emailCheck = new IsNotNull("email");
        assertNotNull(emailCheck);
        assertEquals("email IS NOT NULL", emailCheck.toString());
    }

    @Test
    public void testIsNotNull_classLevel_nameCheck() {
        IsNotNull nameCheck = new IsNotNull("customer_name");
        assertNotNull(nameCheck);
        assertEquals("customer_name IS NOT NULL", nameCheck.toString());
    }

    @Test
    public void testIsNotNull_classLevel_requiredFields() {
        And requiredFields = new And(
            new IsNotNull("first_name"),
            new IsNotNull("last_name"),
            new IsNotNull("email")
        );
        assertNotNull(requiredFields);
    }

    @Test
    public void testIsNotNull_constructor_emailCheck() {
        IsNotNull emailCheck = new IsNotNull("email");
        assertNotNull(emailCheck);
        assertEquals("email IS NOT NULL", emailCheck.toString());
    }

    @Test
    public void testIsNotNull_constructor_phoneCheck() {
        IsNotNull phoneCheck = new IsNotNull("phone_number");
        assertNotNull(phoneCheck);
        assertEquals("phone_number IS NOT NULL", phoneCheck.toString());
    }

    @Test
    public void testIsNotNull_constructor_addressCheck() {
        IsNotNull addressCheck = new IsNotNull("shipping_address");
        assertNotNull(addressCheck);
        assertEquals("shipping_address IS NOT NULL", addressCheck.toString());
    }

    @Test
    public void testIsNotNull_constructor_dateCheck() {
        IsNotNull dateCheck = new IsNotNull("registration_date");
        assertNotNull(dateCheck);
        assertEquals("registration_date IS NOT NULL", dateCheck.toString());
    }

    // =====================================================================
    // 23. IsNaN.java
    // =====================================================================

    @Test
    public void testIsNaN_classLevel_calcCheck() {
        IsNaN calcCheck = new IsNaN("calculation_result");
        assertNotNull(calcCheck);
        assertEquals("calculation_result IS NAN", calcCheck.toString());
    }

    @Test
    public void testIsNaN_classLevel_invalidRatio() {
        IsNaN invalidRatio = new IsNaN("profit_ratio");
        assertNotNull(invalidRatio);
        assertEquals("profit_ratio IS NAN", invalidRatio.toString());
    }

    @Test
    public void testIsNaN_classLevel_mathError() {
        IsNaN mathError = new IsNaN("sqrt_result");
        assertNotNull(mathError);
        assertEquals("sqrt_result IS NAN", mathError.toString());
    }

    @Test
    public void testIsNaN_classLevel_combineWithOthers() {
        Or invalidNumeric = new Or(
            new IsNaN("score"),
            new IsInfinite("score"),
            new IsNull("score")
        );
        assertNotNull(invalidNumeric);
    }

    @Test
    public void testIsNaN_constructor_tempCheck() {
        IsNaN tempCheck = new IsNaN("temperature");
        assertNotNull(tempCheck);
        assertEquals("temperature IS NAN", tempCheck.toString());
    }

    @Test
    public void testIsNaN_constructor_calcError() {
        IsNaN calcError = new IsNaN("computed_value");
        assertNotNull(calcError);
        assertEquals("computed_value IS NAN", calcError.toString());
    }

    @Test
    public void testIsNaN_constructor_divError() {
        IsNaN divError = new IsNaN("average_score");
        assertNotNull(divError);
        assertEquals("average_score IS NAN", divError.toString());
    }

    @Test
    public void testIsNaN_constructor_statsCheck() {
        IsNaN statsCheck = new IsNaN("standard_deviation");
        assertNotNull(statsCheck);
        assertEquals("standard_deviation IS NAN", statsCheck.toString());
    }

    @Test
    public void testIsNaN_constructor_sensorError() {
        IsNaN sensorError = new IsNaN("pressure_reading");
        assertNotNull(sensorError);
        assertEquals("pressure_reading IS NAN", sensorError.toString());
    }

    // =====================================================================
    // 24. IsNotNaN.java
    // =====================================================================

    @Test
    public void testIsNotNaN_classLevel_validResult() {
        IsNotNaN validResult = new IsNotNaN("calculation_result");
        assertNotNull(validResult);
        assertEquals("calculation_result IS NOT NAN", validResult.toString());
    }

    @Test
    public void testIsNotNaN_classLevel_validRatio() {
        IsNotNaN validRatio = new IsNotNaN("profit_ratio");
        assertNotNull(validRatio);
        assertEquals("profit_ratio IS NOT NAN", validRatio.toString());
    }

    @Test
    public void testIsNotNaN_classLevel_validReading() {
        IsNotNaN validReading = new IsNotNaN("temperature");
        assertNotNull(validReading);
        assertEquals("temperature IS NOT NAN", validReading.toString());
    }

    @Test
    public void testIsNotNaN_classLevel_combineWithOthers() {
        And validNumber = new And(
            new IsNotNaN("score"),
            new IsNotInfinite("score"),
            new Between("score", 0, 100)
        );
        assertNotNull(validNumber);
    }

    @Test
    public void testIsNotNaN_constructor_tempCheck() {
        IsNotNaN tempCheck = new IsNotNaN("temperature");
        assertNotNull(tempCheck);
        assertEquals("temperature IS NOT NAN", tempCheck.toString());
    }

    @Test
    public void testIsNotNaN_constructor_calcCheck() {
        IsNotNaN calcCheck = new IsNotNaN("computed_value");
        assertNotNull(calcCheck);
        assertEquals("computed_value IS NOT NAN", calcCheck.toString());
    }

    @Test
    public void testIsNotNaN_constructor_statsCheck() {
        IsNotNaN statsCheck = new IsNotNaN("standard_deviation");
        assertNotNull(statsCheck);
        assertEquals("standard_deviation IS NOT NAN", statsCheck.toString());
    }

    @Test
    public void testIsNotNaN_constructor_financeCheck() {
        IsNotNaN financeCheck = new IsNotNaN("return_on_investment");
        assertNotNull(financeCheck);
        assertEquals("return_on_investment IS NOT NAN", financeCheck.toString());
    }

    @Test
    public void testIsNotNaN_constructor_measurementCheck() {
        IsNotNaN measurementCheck = new IsNotNaN("ph_level");
        assertNotNull(measurementCheck);
        assertEquals("ph_level IS NOT NAN", measurementCheck.toString());
    }

    // =====================================================================
    // 25. IsInfinite.java
    // =====================================================================

    @Test
    public void testIsInfinite_classLevel_overflowCheck() {
        IsInfinite overflowCheck = new IsInfinite("growth_rate");
        assertNotNull(overflowCheck);
        assertEquals("growth_rate IS INFINITE", overflowCheck.toString());
    }

    @Test
    public void testIsInfinite_classLevel_divisionCheck() {
        IsInfinite divisionCheck = new IsInfinite("calculated_ratio");
        assertNotNull(divisionCheck);
        assertEquals("calculated_ratio IS INFINITE", divisionCheck.toString());
    }

    @Test
    public void testIsInfinite_classLevel_calcError() {
        IsInfinite calcError = new IsInfinite("risk_score");
        assertNotNull(calcError);
        assertEquals("risk_score IS INFINITE", calcError.toString());
    }

    @Test
    public void testIsInfinite_classLevel_combineWithNaN() {
        Or invalidValue = new Or(
            new IsInfinite("metric_value"),
            new IsNaN("metric_value")
        );
        assertNotNull(invalidValue);
    }

    @Test
    public void testIsInfinite_constructor_rateCheck() {
        IsInfinite rateCheck = new IsInfinite("interest_rate");
        assertNotNull(rateCheck);
        assertEquals("interest_rate IS INFINITE", rateCheck.toString());
    }

    @Test
    public void testIsInfinite_constructor_overflowCheck() {
        IsInfinite overflowCheck = new IsInfinite("computed_value");
        assertNotNull(overflowCheck);
        assertEquals("computed_value IS INFINITE", overflowCheck.toString());
    }

    @Test
    public void testIsInfinite_constructor_divByZero() {
        IsInfinite divByZero = new IsInfinite("average_per_unit");
        assertNotNull(divByZero);
        assertEquals("average_per_unit IS INFINITE", divByZero.toString());
    }

    @Test
    public void testIsInfinite_constructor_expCheck() {
        IsInfinite expCheck = new IsInfinite("exponential_growth");
        assertNotNull(expCheck);
        assertEquals("exponential_growth IS INFINITE", expCheck.toString());
    }

    // =====================================================================
    // 26. IsNotInfinite.java
    // =====================================================================

    @Test
    public void testIsNotInfinite_classLevel_finiteRatio() {
        IsNotInfinite finiteRatio = new IsNotInfinite("price_ratio");
        assertNotNull(finiteRatio);
        assertEquals("price_ratio IS NOT INFINITE", finiteRatio.toString());
    }

    @Test
    public void testIsNotInfinite_classLevel_validMeasurement() {
        IsNotInfinite validMeasurement = new IsNotInfinite("sensor_reading");
        assertNotNull(validMeasurement);
        assertEquals("sensor_reading IS NOT INFINITE", validMeasurement.toString());
    }

    @Test
    public void testIsNotInfinite_classLevel_validGrowth() {
        IsNotInfinite validGrowth = new IsNotInfinite("growth_rate");
        assertNotNull(validGrowth);
        assertEquals("growth_rate IS NOT INFINITE", validGrowth.toString());
    }

    @Test
    public void testIsNotInfinite_classLevel_combineWithOthers() {
        And validNumeric = new And(
            new IsNotInfinite("calculated_value"),
            new IsNotNaN("calculated_value"),
            new GreaterThan("calculated_value", 0)
        );
        assertNotNull(validNumeric);
    }

    @Test
    public void testIsNotInfinite_constructor_calcCheck() {
        IsNotInfinite calcCheck = new IsNotInfinite("calculation_result");
        assertNotNull(calcCheck);
        assertEquals("calculation_result IS NOT INFINITE", calcCheck.toString());
    }

    @Test
    public void testIsNotInfinite_constructor_growthCheck() {
        IsNotInfinite growthCheck = new IsNotInfinite("year_over_year_growth");
        assertNotNull(growthCheck);
        assertEquals("year_over_year_growth IS NOT INFINITE", growthCheck.toString());
    }

    @Test
    public void testIsNotInfinite_constructor_divisionCheck() {
        IsNotInfinite divisionCheck = new IsNotInfinite("average_score");
        assertNotNull(divisionCheck);
        assertEquals("average_score IS NOT INFINITE", divisionCheck.toString());
    }

    @Test
    public void testIsNotInfinite_constructor_scientificCheck() {
        IsNotInfinite scientificCheck = new IsNotInfinite("exponential_result");
        assertNotNull(scientificCheck);
        assertEquals("exponential_result IS NOT INFINITE", scientificCheck.toString());
    }
}
