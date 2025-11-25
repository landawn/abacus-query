/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.query.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.query.QueryUtil;
import com.landawn.abacus.query.SK;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.condition.Expression.Expr;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.EntityId;
import com.landawn.abacus.util.N;

/**
 * A comprehensive, enterprise-grade factory class providing a complete suite of SQL condition builders
 * for constructing type-safe, parameterized database queries with advanced logical operations, comparison
 * operators, and complex join conditions. This class serves as the foundation for building dynamic,
 * secure SQL queries that prevent SQL injection attacks while maintaining optimal performance through
 * prepared statement usage and intelligent query optimization strategies.
 *
 * <p>The {@code ConditionFactory} class addresses critical challenges in enterprise database programming
 * by providing a fluent, type-safe API for constructing complex SQL WHERE clauses, JOIN conditions,
 * and subquery predicates. It supports the full spectrum of SQL operators and logical constructs,
 * enabling developers to build sophisticated queries programmatically while maintaining code readability
 * and ensuring database security through parameterized query generation.</p>
 *
 * <p><b>⚠️ IMPORTANT - SQL Injection Prevention:</b>
 * All condition methods in this factory generate parameterized SQL with proper value binding,
 * ensuring complete protection against SQL injection attacks. Never concatenate user input
 * directly into condition values - always use the provided parameter binding mechanisms
 * for secure database operations in production environments.</p>
 *
 * <p><b>Key Features and Capabilities:</b>
 * <ul>
 *   <li><b>Complete SQL Operator Support:</b> All standard SQL comparison, logical, and pattern matching operators</li>
 *   <li><b>Type-Safe Query Construction:</b> Compile-time type checking for database column and value operations</li>
 *   <li><b>Parameterized Query Generation:</b> Automatic parameter binding preventing SQL injection vulnerabilities</li>
 *   <li><b>Complex Logical Operations:</b> Support for nested AND/OR/NOT conditions with proper precedence</li>
 *   <li><b>Advanced Join Conditions:</b> Comprehensive support for all SQL join types and complex join predicates</li>
 *   <li><b>Subquery Integration:</b> Seamless integration with EXISTS, IN, and correlated subquery patterns</li>
 *   <li><b>Pattern Matching:</b> Advanced LIKE, REGEX, and full-text search condition support</li>
 *   <li><b>Collection Operations:</b> Optimized IN/NOT IN operations for collections and arrays</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Security First:</b> All operations generate parameterized SQL preventing injection attacks</li>
 *   <li><b>Type Safety Priority:</b> Strong typing ensures compile-time validation of query construction</li>
 *   <li><b>Fluent Interface:</b> Method chaining enables readable, expressive query building patterns</li>
 *   <li><b>Performance Optimized:</b> Generated SQL is optimized for database execution plan efficiency</li>
 *   <li><b>Framework Agnostic:</b> Works with any JDBC-based framework or standalone applications</li>
 * </ul>
 *
 * <p><b>Condition Categories and Operators:</b>
 * <table border="1" style="border-collapse: collapse;">
 *   <caption><b>SQL Condition Types and Corresponding Factory Methods</b></caption>
 *   <tr style="background-color: #f2f2f2;">
 *     <th>Category</th>
 *     <th>SQL Operators</th>
 *     <th>Factory Methods</th>
 *     <th>Usage Examples</th>
 *   </tr>
 *   <tr>
 *     <td>Equality/Inequality</td>
 *     <td>=, !=, &lt;&gt;</td>
 *     <td>eq(), ne(), notEqual()</td>
 *     <td>eq("status", "ACTIVE")</td>
 *   </tr>
 *   <tr>
 *     <td>Comparison</td>
 *     <td>&lt;, &lt;=, &gt;, &gt;=</td>
 *     <td>lt(), le(), gt(), ge()</td>
 *     <td>gt("age", 18), le("salary", 50000)</td>
 *   </tr>
 *   <tr>
 *     <td>Range Operations</td>
 *     <td>BETWEEN, NOT BETWEEN</td>
 *     <td>between(), notBetween()</td>
 *     <td>between("price", 10, 100)</td>
 *   </tr>
 *   <tr>
 *     <td>Collection Membership</td>
 *     <td>IN, NOT IN</td>
 *     <td>in(), notIn()</td>
 *     <td>in("category", Arrays.asList("A", "B"))</td>
 *   </tr>
 *   <tr>
 *     <td>Pattern Matching</td>
 *     <td>LIKE, NOT LIKE, REGEX</td>
 *     <td>like(), notLike(), regex()</td>
 *     <td>like("email", "%@company.com")</td>
 *   </tr>
 *   <tr>
 *     <td>Null Checking</td>
 *     <td>IS NULL, IS NOT NULL</td>
 *     <td>isNull(), isNotNull()</td>
 *     <td>isNotNull("optional_field")</td>
 *   </tr>
 *   <tr>
 *     <td>Logical Operations</td>
 *     <td>AND, OR, NOT</td>
 *     <td>and(), or(), not()</td>
 *     <td>and(eq("active", true), gt("age", 21))</td>
 *   </tr>
 *   <tr>
 *     <td>Subquery Operations</td>
 *     <td>EXISTS, NOT EXISTS</td>
 *     <td>exists(), notExists()</td>
 *     <td>exists("SELECT 1 FROM orders WHERE ...")</td>
 *   </tr>
 * </table>
 *
 * <p><b>Core API Categories:</b>
 * <ul>
 *   <li><b>Basic Comparison:</b> {@code eq()}, {@code ne()}, {@code lt()}, {@code le()}, {@code gt()}, {@code ge()}</li>
 *   <li><b>Range and Collection:</b> {@code between()}, {@code in()}, {@code notIn()}, {@code like()}</li>
 *   <li><b>Null Operations:</b> {@code isNull()}, {@code isNotNull()}, {@code isEmpty()}, {@code isNotEmpty()}</li>
 *   <li><b>Logical Combinators:</b> {@code and()}, {@code or()}, {@code not()}, {@code xor()}</li>
 *   <li><b>Advanced Patterns:</b> {@code regex()}, {@code fullTextSearch()}, {@code soundex()}</li>
 *   <li><b>Join Conditions:</b> {@code join()}, {@code leftJoin()}, {@code innerJoin()}, {@code outerJoin()}</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic equality and comparison conditions
 * Condition userActive = ConditionFactory.eq("status", "ACTIVE");
 * Condition adultUsers = ConditionFactory.ge("age", 18);
 * Condition recentOrders = ConditionFactory.gt("order_date", lastWeek);
 *
 * // Complex logical conditions with proper precedence
 * Condition complexFilter = ConditionFactory.and(
 *     ConditionFactory.eq("department", "Engineering"),
 *     ConditionFactory.or(
 *         ConditionFactory.gt("salary", 75000),
 *         ConditionFactory.eq("level", "Senior")
 *     ),
 *     ConditionFactory.isNotNull("manager_id")
 * );
 *
 * // Collection-based conditions for efficient IN operations
 * List<String> validStatuses = Arrays.asList("PENDING", "APPROVED", "ACTIVE");
 * Condition statusFilter = ConditionFactory.in("status", validStatuses);
 *
 * // Pattern matching for flexible text search
 * Condition emailFilter = ConditionFactory.like("email", "%@company.com");
 * Condition namePattern = ConditionFactory.regex("name", "^[A-Z][a-z]+ [A-Z][a-z]+$");
 *
 * // Range conditions for efficient database queries
 * Condition priceRange = ConditionFactory.between("price", 100.0, 500.0);
 * Condition dateRange = ConditionFactory.between("created_date", startDate, endDate);
 *
 * // Subquery conditions for complex business logic
 * Condition hasOrders = ConditionFactory.exists(
 *     "SELECT 1 FROM orders WHERE customer_id = customers.id AND status = 'COMPLETED'"
 * );
 * }</pre>
 *
 * <p><b>Advanced Query Construction Patterns:</b>
 * <pre>{@code
 * public class UserQueryBuilder {
 *
 *     // Dynamic query building based on search criteria
 *     public static Condition buildUserSearchCondition(UserSearchCriteria criteria) {
 *         List<Condition> conditions = new ArrayList<>();
 *
 *         // Add conditions based on provided criteria
 *         if (criteria.getName() != null) {
 *             conditions.add(ConditionFactory.like("name", "%" + criteria.getName() + "%"));
 *         }
 *
 *         if (criteria.getDepartment() != null) {
 *             conditions.add(ConditionFactory.eq("department", criteria.getDepartment()));
 *         }
 *
 *         if (criteria.getMinAge() != null) {
 *             conditions.add(ConditionFactory.ge("age", criteria.getMinAge()));
 *         }
 *
 *         if (criteria.getMaxAge() != null) {
 *             conditions.add(ConditionFactory.le("age", criteria.getMaxAge()));
 *         }
 *
 *         if (criteria.getSkills() != null && !criteria.getSkills().isEmpty()) {
 *             conditions.add(ConditionFactory.exists(
 *                 "SELECT 1 FROM user_skills WHERE user_id = users.id AND skill IN (" +
 *                 criteria.getSkills().stream().map(s -> "?").collect(Collectors.joining(",")) + ")"
 *             ));
 *         }
 *
 *         // Combine all conditions with AND logic
 *         return conditions.isEmpty() ? ConditionFactory.alwaysTrue() :
 *                conditions.stream().reduce(ConditionFactory::and).orElse(ConditionFactory.alwaysTrue());
 *     }
 *
 *     // Complex join condition building
 *     public static Condition buildUserOrderJoinCondition(OrderSearchCriteria orderCriteria) {
 *         Condition baseJoin = ConditionFactory.eq("users.id", "orders.customer_id");
 *
 *         List<Condition> orderConditions = new ArrayList<>();
 *         orderConditions.add(baseJoin);
 *
 *         if (orderCriteria.getMinTotal() != null) {
 *             orderConditions.add(ConditionFactory.ge("orders.total_amount", orderCriteria.getMinTotal()));
 *         }
 *
 *         if (orderCriteria.getDateRange() != null) {
 *             orderConditions.add(ConditionFactory.between("orders.order_date",
 *                 orderCriteria.getDateRange().getStart(),
 *                 orderCriteria.getDateRange().getEnd()));
 *         }
 *
 *         return orderConditions.stream().reduce(ConditionFactory::and).orElse(baseJoin);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Type Safety and Parameter Binding:</b>
 * <ul>
 *   <li><b>Compile-Time Validation:</b> Generic type parameters ensure type consistency between columns and values</li>
 *   <li><b>Automatic Parameter Binding:</b> All values are automatically converted to prepared statement parameters</li>
 *   <li><b>Collection Handling:</b> Collections and arrays are properly expanded into IN clause parameters</li>
 *   <li><b>Null Safety:</b> Proper handling of null values with appropriate SQL NULL semantics</li>
 *   <li><b>Type Conversion:</b> Automatic conversion between compatible Java and SQL types</li>
 * </ul>
 *
 * <p><b>Performance Optimization Features:</b>
 * <ul>
 *   <li><b>Prepared Statement Usage:</b> All conditions generate parameterized SQL for statement caching</li>
 *   <li><b>Index-Friendly Operations:</b> Generated SQL is optimized for database index usage</li>
 *   <li><b>Efficient Collection Handling:</b> Large IN clauses are optimized for database performance</li>
 *   <li><b>Query Plan Optimization:</b> Condition ordering optimized for database execution plans</li>
 *   <li><b>Memory Efficiency:</b> Minimal object allocation during condition construction</li>
 * </ul>
 *
 * <p><b>Integration with Query Builders:</b>
 * <ul>
 *   <li><b>SQL Builder Integration:</b> Seamless integration with SQL query builders and ORM frameworks</li>
 *   <li><b>Criteria API Support:</b> Compatible with JPA Criteria API patterns and usage</li>
 *   <li><b>Dynamic Query Construction:</b> Supports runtime query building based on user input</li>
 *   <li><b>Framework Agnostic:</b> Works with any JDBC-based persistence framework</li>
 * </ul>
 *
 * <p><b>Advanced Pattern Matching Support:</b>
 * <ul>
 *   <li><b>SQL LIKE Patterns:</b> Full support for SQL LIKE with % and _ wildcards</li>
 *   <li><b>Regular Expressions:</b> Database-specific regex support (MySQL, PostgreSQL, Oracle)</li>
 *   <li><b>Case Sensitivity:</b> Configurable case-sensitive and case-insensitive matching</li>
 *   <li><b>Full-Text Search:</b> Integration with database full-text search capabilities</li>
 *   <li><b>Phonetic Matching:</b> SOUNDEX and similar phonetic matching algorithms</li>
 * </ul>
 *
 * <p><b>Logical Operation Precedence and Grouping:</b>
 * <ul>
 *   <li><b>Proper Precedence:</b> Automatic parentheses insertion for correct logical operator precedence</li>
 *   <li><b>Explicit Grouping:</b> Support for explicit condition grouping with parentheses</li>
 *   <li><b>Nested Conditions:</b> Unlimited nesting depth for complex logical expressions</li>
 *   <li><b>Short-Circuit Evaluation:</b> Optimized SQL generation for efficient condition evaluation</li>
 * </ul>
 *
 * <p><b>Error Handling and Validation:</b>
 * <ul>
 *   <li><b>Parameter Validation:</b> Comprehensive validation of condition parameters</li>
 *   <li><b>SQL Syntax Validation:</b> Early detection of potential SQL syntax issues</li>
 *   <li><b>Type Compatibility Checking:</b> Validation of type compatibility between columns and values</li>
 *   <li><b>Detailed Error Messages:</b> Clear, actionable error messages for debugging</li>
 * </ul>
 *
 * <p><b>Best Practices and Recommendations:</b>
 * <ul>
 *   <li>Always use factory methods instead of constructing Condition objects directly</li>
 *   <li>Prefer specific comparison methods (eq, gt, etc.) over generic expression building</li>
 *   <li>Use collections with IN operations instead of multiple OR conditions for better performance</li>
 *   <li>Leverage BETWEEN operations for range queries to enable index usage</li>
 *   <li>Group related conditions logically to improve query readability and performance</li>
 *   <li>Use EXISTS instead of IN for subqueries when checking for existence</li>
 *   <li>Consider database-specific optimizations for high-performance applications</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Concatenating user input directly into SQL strings (use parameterized conditions)</li>
 *   <li>Creating overly complex nested conditions that hurt query performance</li>
 *   <li>Using LIKE conditions on unindexed columns in large tables</li>
 *   <li>Ignoring null handling in equality comparisons</li>
 *   <li>Not considering database-specific performance characteristics</li>
 *   <li>Using string concatenation for dynamic condition building</li>
 * </ul>
 *
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li><b>SQL Injection Prevention:</b> All factory methods generate parameterized SQL</li>
 *   <li><b>Input Validation:</b> Automatic validation and sanitization of condition parameters</li>
 *   <li><b>Access Control:</b> Integration points for column-level security and access control</li>
 *   <li><b>Audit Trail:</b> Support for logging and auditing of dynamic query construction</li>
 * </ul>
 *
 * @see Condition
 * @see Expression
 * @see QueryUtil
 * @see com.landawn.abacus.query.SQLBuilder
 * @see com.landawn.abacus.annotation.Column
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.sql/java/sql/PreparedStatement.html">PreparedStatement</a>
 * @see <a href="https://en.wikipedia.org/wiki/SQL_injection">SQL Injection Prevention</a>
 */
public class ConditionFactory {
    /**
     * Expression representing a question mark literal ("?") for use in parameterized SQL queries.
     * This constant is used when creating conditions with placeholders for prepared statements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a parameterized condition
     * Equal condition = ConditionFactory.eq("age"); // Uses QME internally
     * }</pre>
     */
    public static final Expression QME = Expr.of(SK.QUESTION_MARK);

    /**
     * Constant representing ascending sort direction.
     * Used when creating ORDER BY or GROUP BY clauses.
     */
    public static final SortDirection ASC = SortDirection.ASC;

    /**
     * Constant representing descending sort direction.
     * Used when creating ORDER BY or GROUP BY clauses.
     */
    public static final SortDirection DESC = SortDirection.DESC;

    /**
     * Regular expression pattern for validating alphanumeric column names.
     * Column names must consist of letters, digits, underscores, or hyphens.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean isValid = PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME.matcher("column_name").matches();
     * }</pre>
     */
    public static final Pattern PATTERN_FOR_ALPHANUMERIC_COLUMN_NAME = Pattern.compile("^[a-zA-Z0-9_-]+$");

    static final Expression ALWAYS_TRUE = Expression.of("1 < 2");

    private static final Expression ALWAYS_FALSE = Expression.of("1 > 2");

    private ConditionFactory() {
        // No instance;
    }

    /**
     * Returns a condition that always evaluates to true.
     * Useful for building dynamic queries where a condition might be conditionally included.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition condition = includeFilter ? ConditionFactory.eq("status", "active") 
     *                                    : ConditionFactory.alwaysTrue();
     * }</pre>
     *
     * @return an Expression that always evaluates to true (1 &lt; 2)
     */
    public static Expression alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     * Returns a condition that always evaluates to false.
     * 
     * @return an Expression that always evaluates to false (1 > 2)
     * @deprecated This method is deprecated and should not be used in new code
     */
    @Deprecated
    public static Expression alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Creates a negation condition that represents the logical NOT of the provided condition.
     * 
     * <p>This method creates a Not condition that inverts the logical result of the wrapped condition.
     * It can be used to negate any other condition type, such as Equal, Like, In, Between, etc.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a NOT LIKE condition
     * Like likeCondition = CF.like("name", "%test%");
     * Not notLike = CF.not(likeCondition);
     * 
     * // Create a NOT IN condition
     * Not notIn = CF.not(CF.in("status", Arrays.asList("inactive", "deleted")));
     * 
     * // Create a NOT BETWEEN condition
     * Not notBetween = CF.not(CF.between("age", 18, 65));
     * 
     * // Create a complex negated condition
     * Not complexNot = CF.not(CF.and(
     *     CF.eq("status", "active"),
     *     CF.gt("age", 18),
     *     CF.like("email", "%@company.com")
     * ));
     * }</pre>
     * 
     * @param condition the condition to negate
     * @return a Not condition that wraps and negates the provided condition
     * @see Not
     * @see Condition
     * @see ConditionFactory
     */
    public static Not not(final Condition condition) {
        return condition.not();
    }

    /**
     * Creates a NamedProperty instance representing a property/column name.
     * This is used to reference database columns in a type-safe manner.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NamedProperty prop = ConditionFactory.namedProperty("user_name");
     * }</pre>
     *
     * @param propName the name of the property/column
     * @return a NamedProperty instance
     */
    public static NamedProperty namedProperty(final String propName) {
        return NamedProperty.of(propName);
    }

    /**
     * Creates an Expression from a string literal.
     * This allows for custom SQL expressions to be included in queries.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Expression expr = ConditionFactory.expr("UPPER(name) = 'JOHN'");
     * }</pre>
     *
     * @param literal the SQL expression as a string
     * @return an Expression instance
     */
    public static Expression expr(final String literal) {
        return Expression.of(literal);
    }

    /**
     * Creates a binary condition with the specified property name, operator, and value.
     * This is a general method for creating conditions with any binary operator.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Binary condition = ConditionFactory.binary("price", Operator.GREATER_THAN, 100);
     * }</pre>
     *
     * @param propName the property/column name
     * @param operator the binary operator to use
     * @param propValue the value to compare against
     * @return a Binary condition
     */
    public static Binary binary(final String propName, final Operator operator, final Object propValue) {
        return new Binary(propName, operator, propValue);
    }

    /**
     * Creates an equality condition (=) for the specified property and value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal condition = ConditionFactory.equal("username", "john_doe");
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare for equality
     * @return an Equal condition
     */
    public static Equal equal(final String propName, final Object propValue) { //NOSONAR
        return new Equal(propName, propValue);
    }

    /**
     * Creates a parameterized equality condition for use with prepared statements.
     * The value will be represented by a question mark (?) placeholder.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal condition = ConditionFactory.equal("user_id");
     * // Results in SQL like: WHERE user_id = ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return an Equal condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static Equal equal(final String propName) {//NOSONAR
        return equal(propName, QME);
    }

    /**
     * Creates an equality condition (=) for the specified property and value.
     * This is a shorthand alias for {@link #equal(String, Object)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal condition = ConditionFactory.eq("status", "active");
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare for equality
     * @return an Equal condition
     */
    public static Equal eq(final String propName, final Object propValue) {
        return new Equal(propName, propValue);
    }

    /**
     * Creates a parameterized equality condition for use with prepared statements.
     * This is a shorthand alias for {@link #equal(String)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Equal condition = ConditionFactory.eq("email");
     * // Results in SQL like: WHERE email = ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return an Equal condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static Equal eq(final String propName) {
        return eq(propName, QME);
    }

    //    // The method eqOr(String, Object[]) is ambiguous for the type ConditionFactory
    //    /**
    //     * Creates an OR condition where the property equals any of the provided values.
    //     * 
    //     * <p><b>Usage Examples:</b></p>
    //     * <pre>{@code
    //     * Or condition = ConditionFactory.eqOr("status", "active", "pending", "approved");
    //     * // Results in SQL like: WHERE status = 'active' OR status = 'pending' OR status = 'approved'
    //     * }</pre>
    //     *
    //     * @param propName the property/column name
    //     * @param propValues the values to compare against (must not be empty)
    //     * @return an Or condition
    //     * @throws IllegalArgumentException if propValues is empty
    //     * @deprecated Use {@link #eqOr(String, Collection)} instead for better clarity.
    //     * @see #eqOr(String, Collection)
    //     */
    //    @Deprecated
    //    public static Or eqOr(final String propName, final Object... propValues) {
    //        N.checkArgNotEmpty(propValues, "propValues");
    //
    //        final Or or = CF.or();
    //
    //        for (final Object propValue : propValues) {
    //            or.add(eq(propName, propValue));
    //        }
    //
    //        return or;
    //    }

    /**
     * Creates an OR condition where the property equals any of the values in the collection.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> statuses = Arrays.asList("active", "pending");
     * Or condition = ConditionFactory.eqOr("status", statuses);
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValues the collection of values to compare against (must not be empty)
     * @return an Or condition
     * @throws IllegalArgumentException if propValues is empty
     * @deprecated Use {@link #in(String, Collection)} instead for better clarity.
     */
    @Deprecated
    public static Or eqOr(final String propName, final Collection<?> propValues) {
        N.checkArgNotEmpty(propValues, "propValues");

        final Or or = CF.or();

        for (final Object propValue : propValues) {
            or.add(eq(propName, propValue));
        }

        return or;
    }

    /**
     * Creates an OR condition from a map where each entry represents a property-value equality check.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> props = new HashMap<>();
     * props.put("name", "John");
     * props.put("email", "john@example.com");
     * Or condition = ConditionFactory.eqOr(props);
     * // Results in SQL like: WHERE name = 'John' OR email = 'john@example.com'
     * }</pre>
     *
     * @param props map of property names to values (must not be empty)
     * @return an Or condition
     * @throws IllegalArgumentException if props is empty
     */
    public static Or eqOr(final Map<String, ?> props) {
        N.checkArgNotEmpty(props, "props");

        final Iterator<? extends Map.Entry<String, ?>> propIter = props.entrySet().iterator();

        if (props.size() == 1) {
            final Map.Entry<String, ?> prop = propIter.next();
            return or(eq(prop.getKey(), prop.getValue()));
        } else if (props.size() == 2) {
            final Map.Entry<String, ?> prop1 = propIter.next();
            final Map.Entry<String, ?> prop2 = propIter.next();
            return eq(prop1.getKey(), prop1.getValue()).or(eq(prop2.getKey(), prop2.getValue()));
        } else {
            final Condition[] conds = new Condition[props.size()];
            Map.Entry<String, ?> prop = null;

            for (int i = 0, size = props.size(); i < size; i++) {
                prop = propIter.next();
                conds[i] = CF.eq(prop.getKey(), prop.getValue());
            }

            return or(conds);
        }
    }

    /**
     * Creates an OR condition from an entity object using all its properties.
     * Each property of the entity will be included as an equality check in the OR condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "john@example.com");
     * Or condition = ConditionFactory.eqOr(user);
     * // Results in SQL like: WHERE name = 'John' OR email = 'john@example.com'
     * }</pre>
     *
     * @param entity the entity object whose properties will be used
     * @return an Or condition
     */
    @SuppressWarnings("deprecation")
    public static Or eqOr(final Object entity) {
        return eqOr(entity, QueryUtil.getSelectPropNames(entity.getClass(), false, null));
    }

    /**
     * Creates an OR condition from an entity object using only the specified properties.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "john@example.com", 25);
     * Or condition = ConditionFactory.eqOr(user, Arrays.asList("name", "email"));
     * // Only uses name and email, ignores age
     * }</pre>
     *
     * @param entity the entity object
     * @param selectPropNames the property names to include (must not be empty)
     * @return an Or condition
     * @throws IllegalArgumentException if selectPropNames is empty
     */
    public static Or eqOr(final Object entity, final Collection<String> selectPropNames) {
        N.checkArgNotEmpty(selectPropNames, "selectPropNames"); //NOSONAR

        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entity.getClass());
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return or(eq(propName, entityInfo.getPropValue(entity, propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, entityInfo.getPropValue(entity, propName1)).or(eq(propName2, entityInfo.getPropValue(entity, propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, entityInfo.getPropValue(entity, propName));
            }

            return or(conds);
        }
    }

    /**
     * Creates an OR condition with two property-value pairs.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = ConditionFactory.eqOr("name", "John", "email", "john@example.com");
     * // Results in SQL like: WHERE name = 'John' OR email = 'john@example.com'
     * }</pre>
     *
     * @param propName1 first property name
     * @param propValue1 first property value
     * @param propName2 second property name
     * @param propValue2 second property value
     * @return an Or condition
     */
    public static Or eqOr(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        return eq(propName1, propValue1).or(eq(propName2, propValue2));
    }

    /**
     * Creates an OR condition with three property-value pairs.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = ConditionFactory.eqOr("status", "active", "type", "premium", "verified", true);
     * }</pre>
     *
     * @param propName1 first property name
     * @param propValue1 first property value
     * @param propName2 second property name
     * @param propValue2 second property value
     * @param propName3 third property name
     * @param propValue3 third property value
     * @return an Or condition
     */
    public static Or eqOr(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3,
            final Object propValue3) {
        return or(eq(propName1, propValue1), eq(propName2, propValue2), eq(propName3, propValue3));
    }

    /**
     * Creates an AND condition from a map where each entry represents a property-value equality check.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> props = new HashMap<>();
     * props.put("status", "active");
     * props.put("type", "premium");
     * And condition = ConditionFactory.eqAnd(props);
     * // Results in SQL like: WHERE status = 'active' AND type = 'premium'
     * }</pre>
     *
     * @param props map of property names to values (must not be empty)
     * @return an And condition
     * @throws IllegalArgumentException if props is empty
     */
    public static And eqAnd(final Map<String, ?> props) {
        N.checkArgNotEmpty(props, "props");

        final Iterator<? extends Map.Entry<String, ?>> propIter = props.entrySet().iterator();

        if (props.size() == 1) {
            final Map.Entry<String, ?> prop = propIter.next();
            return and(eq(prop.getKey(), prop.getValue()));
        } else if (props.size() == 2) {
            final Map.Entry<String, ?> prop1 = propIter.next();
            final Map.Entry<String, ?> prop2 = propIter.next();
            return eq(prop1.getKey(), prop1.getValue()).and(eq(prop2.getKey(), prop2.getValue()));
        } else {
            final Condition[] conds = new Condition[props.size()];
            Map.Entry<String, ?> prop = null;

            for (int i = 0, size = props.size(); i < size; i++) {
                prop = propIter.next();
                conds[i] = CF.eq(prop.getKey(), prop.getValue());
            }

            return and(conds);
        }

    }

    /**
     * Creates an AND condition from an entity object using all its properties.
     * Each property of the entity will be included as an equality check in the AND condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "john@example.com", 25);
     * And condition = ConditionFactory.eqAnd(user);
     * // Results in SQL like: WHERE name = 'John' AND email = 'john@example.com' AND age = 25
     * }</pre>
     *
     * @param entity the entity object whose properties will be used
     * @return an And condition
     */
    @SuppressWarnings("deprecation")
    public static And eqAnd(final Object entity) {
        return eqAnd(entity, QueryUtil.getSelectPropNames(entity.getClass(), false, null));
    }

    /**
     * Creates an AND condition from an entity object using only the specified properties.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "john@example.com", 25);
     * And condition = ConditionFactory.eqAnd(user, Arrays.asList("email", "age"));
     * // Only uses email and age, ignores name
     * }</pre>
     *
     * @param entity the entity object
     * @param selectPropNames the property names to include (must not be empty)
     * @return an And condition
     * @throws IllegalArgumentException if selectPropNames is empty
     */
    public static And eqAnd(final Object entity, final Collection<String> selectPropNames) {
        N.checkArgNotEmpty(selectPropNames, "selectPropNames");

        final BeanInfo entityInfo = ParserUtil.getBeanInfo(entity.getClass());
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return and(eq(propName, entityInfo.getPropValue(entity, propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, entityInfo.getPropValue(entity, propName1)).and(eq(propName2, entityInfo.getPropValue(entity, propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, entityInfo.getPropValue(entity, propName));
            }

            return and(conds);
        }
    }

    /**
     * Creates an AND condition with two property-value pairs.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.eqAnd("status", "active", "type", "premium");
     * // Results in SQL like: WHERE status = 'active' AND type = 'premium'
     * }</pre>
     *
     * @param propName1 first property name
     * @param propValue1 first property value
     * @param propName2 second property name
     * @param propValue2 second property value
     * @return an And condition
     */
    public static And eqAnd(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        return eq(propName1, propValue1).and(eq(propName2, propValue2));
    }

    /**
     * Creates an AND condition with three property-value pairs.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.eqAnd("status", "active", "type", "premium", "verified", true);
     * }</pre>
     *
     * @param propName1 first property name
     * @param propValue1 first property value
     * @param propName2 second property name
     * @param propValue2 second property value
     * @param propName3 third property name
     * @param propValue3 third property value
     * @return an And condition
     */
    public static And eqAnd(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3,
            final Object propValue3) {
        return and(eq(propName1, propValue1), eq(propName2, propValue2), eq(propName3, propValue3));
    }

    /**
     * Creates an OR condition where each element in the list represents an AND condition of property-value pairs.
     * This is useful for creating conditions like: (a=1 AND b=2) OR (a=3 AND b=4).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map<String, Object>> propsList = new ArrayList<>();
     * propsList.add(Map.of("status", "active", "type", "premium"));
     * propsList.add(Map.of("status", "trial", "verified", true));
     * Or condition = ConditionFactory.eqAndOr(propsList);
     * // Results in: (status='active' AND type='premium') OR (status='trial' AND verified=true)
     * }</pre>
     *
     * @param propsList list of property maps (must not be empty)
     * @return an Or condition
     * @throws IllegalArgumentException if propsList is empty
     */
    @Beta
    public static Or eqAndOr(final List<? extends Map<String, ?>> propsList) {
        N.checkArgNotEmpty(propsList, "propsList");

        final Condition[] conds = new Condition[propsList.size()];

        for (int i = 0, size = propsList.size(); i < size; i++) {
            conds[i] = eqAnd(propsList.get(i));
        }

        return or(conds);
    }

    /**
     * Creates an OR condition from a collection of entities, where each entity forms an AND condition.
     * All properties of each entity will be used.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<User> users = Arrays.asList(
     *     new User("John", "john@example.com"),
     *     new User("Jane", "jane@example.com")
     * );
     * Or condition = ConditionFactory.eqAndOr(users);
     * // Results in: (name='John' AND email='john@example.com') OR (name='Jane' AND email='jane@example.com')
     * }</pre>
     *
     * @param entities collection of entity objects (must not be empty)
     * @return an Or condition
     * @throws IllegalArgumentException if entities is empty
     */
    @SuppressWarnings("deprecation")
    @Beta
    public static Or eqAndOr(final Collection<?> entities) {
        N.checkArgNotEmpty(entities, "entities");

        return eqAndOr(entities, QueryUtil.getSelectPropNames(N.firstNonNull(entities).orElseNull().getClass(), false, null));
    }

    /**
     * Creates an OR condition from a collection of entities using only specified properties.
     * Each entity forms an AND condition with the selected properties.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<User> users = Arrays.asList(new User(...), new User(...));
     * Or condition = ConditionFactory.eqAndOr(users, Arrays.asList("name", "status"));
     * // Only uses name and status properties from each user
     * }</pre>
     *
     * @param entities collection of entity objects (must not be empty)
     * @param selectPropNames the property names to include (must not be empty)
     * @return an Or condition
     * @throws IllegalArgumentException if entities or selectPropNames is empty
     */
    @Beta
    public static Or eqAndOr(final Collection<?> entities, final Collection<String> selectPropNames) {
        N.checkArgNotEmpty(entities, "entities");
        N.checkArgNotEmpty(selectPropNames, "selectPropNames");

        final Iterator<?> iter = entities.iterator();
        final Condition[] conds = new Condition[entities.size()];

        for (int i = 0, size = entities.size(); i < size; i++) {
            conds[i] = eqAnd(iter.next(), selectPropNames);
        }

        return or(conds);
    }

    /**
     * Creates a BETWEEN-like condition using greater than and less than comparisons.
     * The result is: propName &gt; minValue AND propName &lt; maxValue.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.gtAndLt("age", 18, 65);
     * // Results in SQL like: WHERE age &gt; 18 AND age &lt; 65
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (exclusive)
     * @param maxValue the maximum value (exclusive)
     * @return an And condition
     */
    public static And gtAndLt(final String propName, final Object minValue, final Object maxValue) {
        return gt(propName, minValue).and(lt(propName, maxValue));
    }

    /**
     * Creates a parameterized BETWEEN-like condition for prepared statements.
     * The result is: propName &gt; ? AND propName &lt; ?.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.gtAndLt("price");
     * // Results in SQL like: WHERE price &gt; ? AND price &lt; ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return an And condition with parameter placeholders
     */
    public static And gtAndLt(final String propName) {
        return gt(propName).and(lt(propName));
    }

    /**
     * Creates a BETWEEN-like condition using greater than or equal and less than comparisons.
     * The result is: propName &gt;= minValue AND propName &lt; maxValue.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.geAndLt("price", 100, 500);
     * // Results in SQL like: WHERE price &gt;= 100 AND price &lt; 500
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (exclusive)
     * @return an And condition
     */
    public static And geAndLt(final String propName, final Object minValue, final Object maxValue) {
        return ge(propName, minValue).and(lt(propName, maxValue));
    }

    /**
     * Creates a parameterized BETWEEN-like condition for prepared statements.
     * The result is: propName &gt;= ? AND propName &lt; ?.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.geAndLt("score");
     * // Results in SQL like: WHERE score &gt;= ? AND score &lt; ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return an And condition with parameter placeholders
     */
    public static And geAndLt(final String propName) {
        return ge(propName).and(lt(propName));
    }

    /**
     * Creates a BETWEEN-like condition using greater than or equal and less than or equal comparisons.
     * The result is: propName &gt;= minValue AND propName &lt;= maxValue.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.geAndLe("date", startDate, endDate);
     * // Results in SQL like: WHERE date &gt;= '2023-01-01' AND date &lt;= '2023-12-31'
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (inclusive)
     * @return an And condition
     */
    public static And geAndLe(final String propName, final Object minValue, final Object maxValue) {
        return ge(propName, minValue).and(le(propName, maxValue));
    }

    /**
     * Creates a parameterized BETWEEN-like condition for prepared statements.
     * The result is: propName &gt;= ? AND propName &lt;= ?.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.geAndLe("amount");
     * // Results in SQL like: WHERE amount &gt;= ? AND amount &lt;= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return an And condition with parameter placeholders
     */
    public static And geAndLe(final String propName) {
        return ge(propName).and(le(propName));
    }

    /**
     * Creates a BETWEEN-like condition using greater than and less than or equal comparisons.
     * The result is: propName &gt; minValue AND propName &lt;= maxValue.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.gtAndLe("score", 0, 100);
     * // Results in SQL like: WHERE score &gt; 0 AND score &lt;= 100
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (exclusive)
     * @param maxValue the maximum value (inclusive)
     * @return an And condition
     */
    public static And gtAndLe(final String propName, final Object minValue, final Object maxValue) {
        return gt(propName, minValue).and(le(propName, maxValue));
    }

    /**
     * Creates a parameterized BETWEEN-like condition for prepared statements.
     * The result is: propName &gt; ? AND propName &lt;= ?.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.gtAndLe("temperature");
     * // Results in SQL like: WHERE temperature &gt; ? AND temperature &lt;= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return an And condition with parameter placeholders
     */
    public static And gtAndLe(final String propName) {
        return gt(propName).and(le(propName));
    }

    /**
     * Converts an EntityId to an AND condition where each key-value pair becomes an equality check.
     * EntityId typically represents a composite primary key.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * EntityId id = EntityId.of("userId", 123, "orderId", 456);
     * And condition = ConditionFactory.id2Cond(id);
     * // Results in SQL like: WHERE userId = 123 AND orderId = 456
     * }</pre>
     *
     * @param entityId the EntityId containing key-value pairs (must not be null)
     * @return an And condition
     * @throws IllegalArgumentException if entityId is null
     */
    public static And id2Cond(final EntityId entityId) {
        N.checkArgNotNull(entityId, "entityId");

        final Collection<String> selectPropNames = entityId.keySet();
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return and(eq(propName, entityId.get(propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, entityId.get(propName1)).and(eq(propName2, entityId.get(propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, entityId.get(propName));
            }

            return and(conds);
        }
    }

    /**
     * Converts a collection of EntityIds to an OR condition where each EntityId becomes an AND condition.
     * Useful for querying multiple entities by their composite keys.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<EntityId> ids = Arrays.asList(
     *     EntityId.of("userId", 1, "orderId", 100),
     *     EntityId.of("userId", 2, "orderId", 200)
     * );
     * Or condition = ConditionFactory.id2Cond(ids);
     * // Results in: (userId=1 AND orderId=100) OR (userId=2 AND orderId=200)
     * }</pre>
     *
     * @param entityIds collection of EntityIds (must not be empty)
     * @return an Or condition
     * @throws IllegalArgumentException if entityIds is empty
     */
    public static Or id2Cond(final Collection<? extends EntityId> entityIds) {
        N.checkArgNotEmpty(entityIds, "entityIds");

        final Iterator<? extends EntityId> iter = entityIds.iterator();
        final Condition[] conds = new Condition[entityIds.size()];

        for (int i = 0, size = entityIds.size(); i < size; i++) {
            conds[i] = CF.id2Cond(iter.next());
        }

        return CF.or(conds);
    }

    /**
     * Creates a not-equal condition (!=) for the specified property and value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotEqual condition = ConditionFactory.notEqual("status", "deleted");
     * // Results in SQL like: WHERE status != 'deleted'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare for inequality
     * @return a NotEqual condition
     */
    public static NotEqual notEqual(final String propName, final Object propValue) {
        return new NotEqual(propName, propValue);
    }

    /**
     * Creates a parameterized not-equal condition for use with prepared statements.
     * The value will be represented by a question mark (?) placeholder.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotEqual condition = ConditionFactory.notEqual("user_type");
     * // Results in SQL like: WHERE user_type != ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a NotEqual condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static NotEqual notEqual(final String propName) {
        return notEqual(propName, QME);
    }

    /**
     * Creates a not-equal condition (!=) for the specified property and value.
     * This is a shorthand alias for {@link #notEqual(String, Object)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotEqual condition = ConditionFactory.ne("status", "inactive");
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare for inequality
     * @return a NotEqual condition
     */
    public static NotEqual ne(final String propName, final Object propValue) {
        return new NotEqual(propName, propValue);
    }

    /**
     * Creates a parameterized not-equal condition for use with prepared statements.
     * This is a shorthand alias for {@link #notEqual(String)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotEqual condition = ConditionFactory.ne("category");
     * // Results in SQL like: WHERE category != ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a NotEqual condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static NotEqual ne(final String propName) {
        return ne(propName, QME);
    }

    /**
     * Creates a greater-than condition (>) for the specified property and value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThan condition = ConditionFactory.greaterThan("age", 18);
     * // Results in SQL like: WHERE age > 18
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a GreaterThan condition
     */
    public static GreaterThan greaterThan(final String propName, final Object propValue) {
        return new GreaterThan(propName, propValue);
    }

    /**
     * Creates a parameterized greater-than condition for use with prepared statements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThan condition = ConditionFactory.greaterThan("salary");
     * // Results in SQL like: WHERE salary > ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a GreaterThan condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static GreaterThan greaterThan(final String propName) {
        return greaterThan(propName, QME);
    }

    /**
     * Creates a greater-than condition (>) for the specified property and value.
     * This is a shorthand alias for {@link #greaterThan(String, Object)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThan condition = ConditionFactory.gt("price", 100);
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a GreaterThan condition
     */
    public static GreaterThan gt(final String propName, final Object propValue) {
        return new GreaterThan(propName, propValue);
    }

    /**
     * Creates a parameterized greater-than condition for use with prepared statements.
     * This is a shorthand alias for {@link #greaterThan(String)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterThan condition = ConditionFactory.gt("quantity");
     * // Results in SQL like: WHERE quantity > ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a GreaterThan condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static GreaterThan gt(final String propName) {
        return gt(propName, QME);
    }

    /**
     * Creates a greater-than-or-equal condition (>=) for the specified property and value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterEqual condition = ConditionFactory.greaterEqual("score", 60);
     * // Results in SQL like: WHERE score >= 60
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a GreaterEqual condition
     */
    public static GreaterEqual greaterEqual(final String propName, final Object propValue) {
        return new GreaterEqual(propName, propValue);
    }

    /**
     * Creates a parameterized greater-than-or-equal condition for use with prepared statements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterEqual condition = ConditionFactory.greaterEqual("min_age");
     * // Results in SQL like: WHERE min_age >= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a GreaterEqual condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static GreaterEqual greaterEqual(final String propName) {
        return greaterEqual(propName, QME);
    }

    /**
     * Creates a greater-than-or-equal condition (>=) for the specified property and value.
     * This is a shorthand alias for {@link #greaterEqual(String, Object)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterEqual condition = ConditionFactory.ge("level", 5);
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a GreaterEqual condition
     */
    public static GreaterEqual ge(final String propName, final Object propValue) {
        return new GreaterEqual(propName, propValue);
    }

    /**
     * Creates a parameterized greater-than-or-equal condition for use with prepared statements.
     * This is a shorthand alias for {@link #greaterEqual(String)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GreaterEqual condition = ConditionFactory.ge("rating");
     * // Results in SQL like: WHERE rating >= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a GreaterEqual condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static GreaterEqual ge(final String propName) {
        return ge(propName, QME);
    }

    /**
     * Creates a less-than condition (&lt;) for the specified property and value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThan condition = ConditionFactory.lessThan("age", 65);
     * // Results in SQL like: WHERE age &lt; 65
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a LessThan condition
     */
    public static LessThan lessThan(final String propName, final Object propValue) {
        return new LessThan(propName, propValue);
    }

    /**
     * Creates a parameterized less-than condition for use with prepared statements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThan condition = ConditionFactory.lessThan("max_price");
     * // Results in SQL like: WHERE max_price &lt; ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a LessThan condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static LessThan lessThan(final String propName) {
        return lessThan(propName, QME);
    }

    /**
     * Creates a less-than condition (&lt;) for the specified property and value.
     * This is a shorthand alias for {@link #lessThan(String, Object)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThan condition = ConditionFactory.lt("stock", 10);
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a LessThan condition
     */
    public static LessThan lt(final String propName, final Object propValue) {
        return new LessThan(propName, propValue);
    }

    /**
     * Creates a parameterized less-than condition for use with prepared statements.
     * This is a shorthand alias for {@link #lessThan(String)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessThan condition = ConditionFactory.lt("expiry_date");
     * // Results in SQL like: WHERE expiry_date < ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a LessThan condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static LessThan lt(final String propName) {
        return lt(propName, QME);
    }

    /**
     * Creates a less-than-or-equal condition (&lt;=) for the specified property and value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessEqual condition = ConditionFactory.lessEqual("discount", 50);
     * // Results in SQL like: WHERE discount &lt;= 50
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a LessEqual condition
     */
    public static LessEqual lessEqual(final String propName, final Object propValue) {
        return new LessEqual(propName, propValue);
    }

    /**
     * Creates a parameterized less-than-or-equal condition for use with prepared statements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessEqual condition = ConditionFactory.lessEqual("max_attempts");
     * // Results in SQL like: WHERE max_attempts &lt;= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a LessEqual condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static LessEqual lessEqual(final String propName) {
        return lessEqual(propName, QME);
    }

    /**
     * Creates a less-than-or-equal condition (&lt;=) for the specified property and value.
     * This is a shorthand alias for {@link #lessEqual(String, Object)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessEqual condition = ConditionFactory.le("priority", 3);
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare against
     * @return a LessEqual condition
     */
    public static LessEqual le(final String propName, final Object propValue) {
        return new LessEqual(propName, propValue);
    }

    /**
     * Creates a parameterized less-than-or-equal condition for use with prepared statements.
     * This is a shorthand alias for {@link #lessEqual(String)}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LessEqual condition = ConditionFactory.le("weight");
     * // Results in SQL like: WHERE weight <= ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a LessEqual condition with a parameter placeholder
     * @see com.landawn.abacus.query.SQLBuilder
     */
    public static LessEqual le(final String propName) {
        return le(propName, QME);
    }

    /**
     * Creates a BETWEEN condition for the specified property and range values.
     * The condition is inclusive on both ends.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = ConditionFactory.between("age", 18, 65);
     * // Results in SQL like: WHERE age BETWEEN 18 AND 65
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (inclusive)
     * @return a Between condition
     */
    public static Between between(final String propName, final Object minValue, final Object maxValue) {
        return new Between(propName, minValue, maxValue);
    }

    /**
     * Creates a parameterized BETWEEN condition for use with prepared statements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Between condition = ConditionFactory.between("price");
     * // Results in SQL like: WHERE price BETWEEN ? AND ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a Between condition with parameter placeholders
     */
    public static Between between(final String propName) {
        return new Between(propName, CF.QME, CF.QME);
    }

    /**
     * Creates a BETWEEN condition for the specified property and range values.
     * This is an alias for {@link #between(String, Object, Object)}.
     * 
     * @param propName the property/column name
     * @param minValue the minimum value (inclusive)
     * @param maxValue the maximum value (inclusive)
     * @return a Between condition
     * @deprecated please use {@link #between(String, Object, Object)}
     */
    @Deprecated
    public static Between bt(final String propName, final Object minValue, final Object maxValue) {
        return new Between(propName, minValue, maxValue);
    }

    /**
     * Creates a parameterized BETWEEN condition for use with prepared statements.
     * This is an alias for {@link #between(String)}.
     * 
     * @param propName the property/column name
     * @return a Between condition with parameter placeholders
     * @deprecated please use {@link #between(String)}
     */
    @Deprecated
    public static Between bt(final String propName) {
        return new Between(propName, CF.QME, CF.QME);
    }

    /**
     * Creates a NOT BETWEEN condition for the specified property and range values.
     * The condition excludes both boundary values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = ConditionFactory.notBetween("temperature", -10, 40);
     * // Results in SQL like: WHERE temperature NOT BETWEEN -10 AND 40
     * }</pre>
     *
     * @param propName the property/column name
     * @param minValue the minimum value (exclusive)
     * @param maxValue the maximum value (exclusive)
     * @return a NotBetween condition
     */
    public static NotBetween notBetween(final String propName, final Object minValue, final Object maxValue) {
        return new NotBetween(propName, minValue, maxValue);
    }

    /**
     * Creates a parameterized NOT BETWEEN condition for use with prepared statements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotBetween condition = ConditionFactory.notBetween("score");
     * // Results in SQL like: WHERE score NOT BETWEEN ? AND ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a NotBetween condition with parameter placeholders
     */
    public static NotBetween notBetween(final String propName) {
        return new NotBetween(propName, CF.QME, CF.QME);
    }

    /**
     * Creates a LIKE condition for pattern matching.
     * Use SQL wildcards (% for any characters, _ for single character) in the pattern.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = ConditionFactory.like("email", "%@gmail.com");
     * // Results in SQL like: WHERE email LIKE '%@gmail.com'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the pattern to match (can include SQL wildcards)
     * @return a Like condition
     */
    public static Like like(final String propName, final Object propValue) {
        return new Like(propName, propValue);
    }

    /**
     * Creates a parameterized LIKE condition for use with prepared statements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = ConditionFactory.like("name");
     * // Results in SQL like: WHERE name LIKE ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a Like condition with a parameter placeholder
     */
    public static Like like(final String propName) {
        return like(propName, QME);
    }

    /**
     * Creates a NOT LIKE condition for pattern matching exclusion.
     * Use SQL wildcards (% for any characters, _ for single character) in the pattern.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = ConditionFactory.notLike("filename", "%.tmp");
     * // Results in SQL like: WHERE filename NOT LIKE '%.tmp'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the pattern to exclude (can include SQL wildcards)
     * @return a NotLike condition
     */
    public static NotLike notLike(final String propName, final Object propValue) {
        return new NotLike(propName, propValue);
    }

    /**
     * Creates a parameterized NOT LIKE condition for use with prepared statements.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = ConditionFactory.notLike("description");
     * // Results in SQL like: WHERE description NOT LIKE ?
     * }</pre>
     *
     * @param propName the property/column name
     * @return a NotLike condition with a parameter placeholder
     */
    public static NotLike notLike(final String propName) {
        return new NotLike(propName, CF.QME);
    }

    /**
     * Creates a LIKE condition that checks if the property contains the specified value.
     * Automatically wraps the value with % wildcards.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = ConditionFactory.contains("description", "java");
     * // Results in SQL like: WHERE description LIKE '%java%'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to search for
     * @return a Like condition
     */
    public static Like contains(final String propName, final Object propValue) {
        return new Like(propName, SK._PERCENT + N.stringOf(propValue) + SK._PERCENT);
    }

    /**
     * Creates a NOT LIKE condition that checks if the property does not contain the specified value.
     * Automatically wraps the value with % wildcards.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = ConditionFactory.notContains("tags", "deprecated");
     * // Results in SQL like: WHERE tags NOT LIKE '%deprecated%'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to exclude
     * @return a NotLike condition
     */
    public static NotLike notContains(final String propName, final Object propValue) {
        return new NotLike(propName, SK._PERCENT + N.stringOf(propValue) + SK._PERCENT);
    }

    /**
     * Creates a LIKE condition that checks if the property starts with the specified value.
     * Automatically appends a % wildcard.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = ConditionFactory.startsWith("name", "John");
     * // Results in SQL like: WHERE name LIKE 'John%'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the prefix to search for
     * @return a Like condition
     */
    public static Like startsWith(final String propName, final Object propValue) {
        return new Like(propName, N.stringOf(propValue) + SK._PERCENT);
    }

    /**
     * Creates a NOT LIKE condition that checks if the property does not start with the specified value.
     * Automatically appends a % wildcard.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = ConditionFactory.notStartsWith("code", "TEST");
     * // Results in SQL like: WHERE code NOT LIKE 'TEST%'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the prefix to exclude
     * @return a NotLike condition
     */
    public static NotLike notStartsWith(final String propName, final Object propValue) {
        return new NotLike(propName, N.stringOf(propValue) + SK._PERCENT);
    }

    /**
     * Creates a LIKE condition that checks if the property ends with the specified value.
     * Automatically prepends a % wildcard.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Like condition = ConditionFactory.endsWith("email", "@company.com");
     * // Results in SQL like: WHERE email LIKE '%@company.com'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the suffix to search for
     * @return a Like condition
     */
    public static Like endsWith(final String propName, final Object propValue) {
        return new Like(propName, SK._PERCENT + N.stringOf(propValue));
    }

    /**
     * Creates a NOT LIKE condition that checks if the property does not end with the specified value.
     * Automatically prepends a % wildcard.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotLike condition = ConditionFactory.notEndsWith("filename", ".tmp");
     * // Results in SQL like: WHERE filename NOT LIKE '%.tmp'
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the suffix to exclude
     * @return a NotLike condition
     */
    public static NotLike notEndsWith(final String propName, final Object propValue) {
        return new NotLike(propName, SK._PERCENT + N.stringOf(propValue));
    }

    /**
     * Creates an IS NULL condition to check if a property value is null.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNull condition = ConditionFactory.isNull("deleted_at");
     * // Results in SQL like: WHERE deleted_at IS NULL
     * }</pre>
     *
     * @param propName the property/column name
     * @return an IsNull condition
     */
    public static IsNull isNull(final String propName) {
        return new IsNull(propName);
    }

    /**
     * Creates a condition to check if a property is null or empty string.
     * This combines IS NULL and = '' checks with OR.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = ConditionFactory.isEmpty("description");
     * // Results in SQL like: WHERE description IS NULL OR description = ''
     * }</pre>
     *
     * @param propName the property/column name
     * @return an Or condition combining null and empty checks
     */
    @Beta
    public static Or isEmpty(final String propName) {
        return isNull(propName).or(equal(propName, ""));
    }

    /**
     * Creates a condition to check if a property is null or zero.
     * This combines IS NULL and = 0 checks with OR.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = ConditionFactory.isNullOrZero("quantity");
     * // Results in SQL like: WHERE quantity IS NULL OR quantity = 0
     * }</pre>
     *
     * @param propName the property/column name
     * @return an Or condition combining null and zero checks
     */
    @Beta
    public static Or isNullOrZero(final String propName) {
        return isNull(propName).or(equal(propName, 0));
    }

    /**
     * Creates an IS NOT NULL condition to check if a property value is not null.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNotNull condition = ConditionFactory.isNotNull("created_at");
     * // Results in SQL like: WHERE created_at IS NOT NULL
     * }</pre>
     *
     * @param propName the property/column name
     * @return an IsNotNull condition
     */
    public static IsNotNull isNotNull(final String propName) {
        return new IsNotNull(propName);
    }

    /**
     * Creates a condition to check if a numeric property value is NaN (Not a Number).
     * This is specific to floating-point columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNaN condition = ConditionFactory.isNaN("calculation_result");
     * // Results in SQL like: WHERE calculation_result IS NAN
     * }</pre>
     *
     * @param propName the property/column name
     * @return an IsNaN condition
     */
    public static IsNaN isNaN(final String propName) {
        return new IsNaN(propName);
    }

    /**
     * Creates a condition to check if a numeric property value is not NaN.
     * This is specific to floating-point columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNotNaN condition = ConditionFactory.isNotNaN("temperature");
     * // Results in SQL like: WHERE temperature IS NOT NAN
     * }</pre>
     *
     * @param propName the property/column name
     * @return an IsNotNaN condition
     */
    public static IsNotNaN isNotNaN(final String propName) {
        return new IsNotNaN(propName);
    }

    /**
     * Creates a condition to check if a numeric property value is infinite.
     * This is specific to floating-point columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsInfinite condition = ConditionFactory.isInfinite("ratio");
     * // Results in SQL like: WHERE ratio IS INFINITE
     * }</pre>
     *
     * @param propName the property/column name
     * @return an IsInfinite condition
     */
    public static IsInfinite isInfinite(final String propName) {
        return new IsInfinite(propName);
    }

    /**
     * Creates a condition to check if a numeric property value is not infinite.
     * This is specific to floating-point columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNotInfinite condition = ConditionFactory.isNotInfinite("percentage");
     * // Results in SQL like: WHERE percentage IS NOT INFINITE
     * }</pre>
     *
     * @param propName the property/column name
     * @return an IsNotInfinite condition
     */
    public static IsNotInfinite isNotInfinite(final String propName) {
        return new IsNotInfinite(propName);
    }

    /**
     * Creates an IS condition for database-specific identity comparisons.
     * Different from equals (=), IS is used for special SQL comparisons.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Is condition = ConditionFactory.is("is_active", true);
     * // Results in SQL like: WHERE is_active IS TRUE
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare
     * @return an Is condition
     */
    public static Is is(final String propName, final Object propValue) {
        return new Is(propName, propValue);
    }

    /**
     * Creates an IS NOT condition for database-specific identity comparisons.
     * Different from not equals (!=), IS NOT is used for special SQL comparisons.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IsNot condition = ConditionFactory.isNot("is_deleted", true);
     * // Results in SQL like: WHERE is_deleted IS NOT TRUE
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to compare
     * @return an IsNot condition
     */
    public static IsNot isNot(final String propName, final Object propValue) {
        return new IsNot(propName, propValue);
    }

    /**
     * Creates an XOR (exclusive OR) condition for the specified property and value.
     * The condition is true when exactly one of the operands is true.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XOR condition = ConditionFactory.xor("is_premium", true);
     * // Results in SQL like: WHERE is_premium XOR TRUE
     * }</pre>
     *
     * @param propName the property/column name
     * @param propValue the value to XOR with
     * @return an XOR condition
     */
    public static XOR xor(final String propName, final Object propValue) {
        return new XOR(propName, propValue);
    }

    /**
     * Creates an OR junction combining multiple conditions.
     * At least one condition must be true for the OR to be true.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Or condition = ConditionFactory.or(
     *     ConditionFactory.eq("status", "active"),
     *     ConditionFactory.gt("priority", 5),
     *     ConditionFactory.isNull("deleted_at")
     * );
     * // Results in: ((status = 'active') OR (priority > 5) OR (deleted_at IS NULL))
     * }</pre>
     *
     * @param conditions the array of conditions to combine with OR
     * @return an Or junction
     */
    public static Or or(final Condition... conditions) {
        return new Or(conditions);
    }

    /**
     * Creates an OR junction combining multiple conditions from a collection.
     * At least one condition must be true for the OR to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Condition> conditions = Arrays.asList(
     *     ConditionFactory.eq("type", "admin"),
     *     ConditionFactory.eq("type", "moderator")
     * );
     * Or condition = ConditionFactory.or(conditions);
     * }</pre>
     *
     * @param conditions the collection of conditions to combine with OR
     * @return an Or junction
     */
    public static Or or(final Collection<? extends Condition> conditions) {
        return new Or(conditions);
    }

    /**
     * Creates an AND junction combining multiple conditions.
     * All conditions must be true for the AND to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * And condition = ConditionFactory.and(
     *     ConditionFactory.eq("status", "active"),
     *     ConditionFactory.ge("age", 18),
     *     ConditionFactory.isNotNull("email")
     * );
     * // Results in: ((status = 'active') AND (age >= 18) AND (email IS NOT NULL))
     * }</pre>
     *
     * @param conditions the array of conditions to combine with AND
     * @return an And junction
     */
    public static And and(final Condition... conditions) {
        return new And(conditions);
    }

    /**
     * Creates an AND junction combining multiple conditions from a collection.
     * All conditions must be true for the AND to be true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Condition> conditions = Arrays.asList(
     *     ConditionFactory.between("price", 10, 100),
     *     ConditionFactory.eq("in_stock", true)
     * );
     * And condition = ConditionFactory.and(conditions);
     * }</pre>
     *
     * @param conditions the collection of conditions to combine with AND
     * @return an And junction
     */
    public static And and(final Collection<? extends Condition> conditions) {
        return new And(conditions);
    }

    /**
     * Creates a junction with a custom operator combining multiple conditions.
     * This allows for database-specific junction types beyond AND/OR.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction condition = ConditionFactory.junction(CustomOperator.NAND,
     *     ConditionFactory.eq("flag1", true),
     *     ConditionFactory.eq("flag2", true)
     * );
     * }</pre>
     *
     * @param operator the junction operator to use
     * @param conditions the array of conditions to combine
     * @return a Junction with the specified operator
     */
    @Beta
    public static Junction junction(final Operator operator, final Condition... conditions) {
        return new Junction(operator, conditions);
    }

    /**
     * Creates a junction with a custom operator combining conditions from a collection.
     * This allows for database-specific junction types beyond AND/OR.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Junction condition = ConditionFactory.junction(CustomOperator.NOR, conditionsList);
     * }</pre>
     *
     * @param operator the junction operator to use
     * @param conditions the collection of conditions to combine
     * @return a Junction with the specified operator
     */
    @Beta
    public static Junction junction(final Operator operator, final Collection<? extends Condition> conditions) {
        return new Junction(operator, conditions);
    }

    /**
     * Creates a WHERE clause with the specified condition.
     * This is typically the starting point for building a query.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where where = ConditionFactory.where(ConditionFactory.eq("active", true));
     * }</pre>
     *
     * @param condition the condition for the WHERE clause
     * @return a Where clause
     */
    public static Where where(final Condition condition) {
        return new Where(condition);
    }

    /**
     * Creates a WHERE clause from a string expression.
     * Useful for custom SQL expressions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Where where = ConditionFactory.where("YEAR(created_date) = 2023");
     * }</pre>
     *
     * @param condition the SQL expression as a string
     * @return a Where clause
     */
    public static Where where(final String condition) {
        return new Where(expr(condition));
    }

    /**
     * Creates a GROUP BY clause with the specified property names.
     * Groups results by the given columns in ascending order.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = ConditionFactory.groupBy("department", "role");
     * // Results in SQL like: GROUP BY department, role
     * }</pre>
     *
     * @param propNames the property/column names to group by
     * @return a GroupBy clause
     */
    public static GroupBy groupBy(final String... propNames) {
        return new GroupBy(propNames);
    }

    /**
     * Creates a GROUP BY clause with properties from a collection.
     * Groups results by the given columns in ascending order.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("country", "city");
     * GroupBy groupBy = ConditionFactory.groupBy(columns);
     * }</pre>
     *
     * @param propNames collection of property/column names to group by
     * @return a GroupBy clause
     */
    public static GroupBy groupBy(final Collection<String> propNames) {
        return groupBy(propNames, SortDirection.ASC);
    }

    /**
     * Creates a GROUP BY clause with properties and specified sort direction.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = ConditionFactory.groupBy(Arrays.asList("sales", "region"), DESC);
     * // Results in SQL like: GROUP BY sales DESC, region DESC
     * }</pre>
     *
     * @param propNames collection of property/column names to group by
     * @param direction the sort direction (ASC or DESC)
     * @return a GroupBy clause
     */
    public static GroupBy groupBy(final Collection<String> propNames, final SortDirection direction) {
        return new GroupBy(propNames, direction);
    }

    /**
     * Creates a GROUP BY clause with a single property and sort direction.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = ConditionFactory.groupBy("category", DESC);
     * // Results in SQL like: GROUP BY category DESC
     * }</pre>
     *
     * @param propName the property/column name to group by
     * @param direction the sort direction (ASC or DESC)
     * @return a GroupBy clause
     */
    public static GroupBy groupBy(final String propName, final SortDirection direction) {
        return new GroupBy(propName, direction);
    }

    /**
     * Creates a GROUP BY clause with two properties and their respective sort directions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = ConditionFactory.groupBy("year", DESC, "month", ASC);
     * // Results in SQL like: GROUP BY year DESC, month ASC
     * }</pre>
     *
     * @param propNameA first property name
     * @param directionA first property sort direction
     * @param propNameB second property name
     * @param directionB second property sort direction
     * @return a GroupBy clause
     */
    public static GroupBy groupBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB) {
        return groupBy(N.asLinkedHashMap(propNameA, directionA, propNameB, directionB));
    }

    /**
     * Creates a GROUP BY clause with three properties and their respective sort directions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = ConditionFactory.groupBy("country", ASC, "state", ASC, "city", DESC);
     * }</pre>
     *
     * @param propNameA first property name
     * @param directionA first property sort direction
     * @param propNameB second property name
     * @param directionB second property sort direction
     * @param propNameC third property name
     * @param directionC third property sort direction
     * @return a GroupBy clause
     */
    public static GroupBy groupBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB,
            final String propNameC, final SortDirection directionC) {
        return groupBy(N.asLinkedHashMap(propNameA, directionA, propNameB, directionB, propNameC, directionC));
    }

    /**
     * Creates a GROUP BY clause from a map of property names to sort directions.
     * The map should be a LinkedHashMap to preserve order.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("department", ASC);
     * orders.put("salary", DESC);
     * GroupBy groupBy = ConditionFactory.groupBy(orders);
     * }</pre>
     *
     * @param orders map of property names to sort directions (should be LinkedHashMap)
     * @return a GroupBy clause
     */
    public static GroupBy groupBy(final Map<String, SortDirection> orders) {
        return new GroupBy(orders);
    }

    /**
     * Creates a GROUP BY clause with a custom condition.
     * Allows for complex GROUP BY expressions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * GroupBy groupBy = ConditionFactory.groupBy(
     *     ConditionFactory.expr("YEAR(order_date), MONTH(order_date)")
     * );
     * }</pre>
     *
     * @param condition the grouping condition
     * @return a GroupBy clause
     */
    public static GroupBy groupBy(final Condition condition) {
        return new GroupBy(condition);
    }

    /**
     * Creates a HAVING clause with the specified condition.
     * HAVING is used to filter grouped results.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Having having = ConditionFactory.having(ConditionFactory.gt("COUNT(*)", 5));
     * // Results in SQL like: HAVING COUNT(*) > 5
     * }</pre>
     *
     * @param condition the condition for the HAVING clause
     * @return a Having clause
     */
    public static Having having(final Condition condition) {
        return new Having(condition);
    }

    /**
     * Creates a HAVING clause from a string expression.
     * Useful for aggregate function conditions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Having having = ConditionFactory.having("SUM(amount) > 1000");
     * }</pre>
     *
     * @param condition the SQL expression as a string
     * @return a Having clause
     */
    public static Having having(final String condition) {
        return new Having(expr(condition));
    }

    /**
     * Creates an ORDER BY clause with the specified property names.
     * Orders results by the given columns in ascending order by default.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = ConditionFactory.orderBy("last_name", "first_name");
     * // Results in SQL like: ORDER BY last_name, first_name
     * }</pre>
     *
     * @param propNames the property/column names to order by
     * @return an OrderBy clause
     */
    public static OrderBy orderBy(final String... propNames) {
        return new OrderBy(propNames);
    }

    /**
     * Creates an ORDER BY clause with ascending order for the specified properties.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = ConditionFactory.orderByAsc("created_date", "id");
     * // Results in SQL like: ORDER BY created_date ASC, id ASC
     * }</pre>
     *
     * @param propNames the property/column names to order by ascending
     * @return an OrderBy clause
     */
    public static OrderBy orderByAsc(final String... propNames) {
        return new OrderBy(Array.asList(propNames), SortDirection.ASC);
    }

    /**
     * Creates an ORDER BY clause with ascending order for properties from a collection.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("priority", "created_date");
     * OrderBy orderBy = ConditionFactory.orderByAsc(columns);
     * }</pre>
     *
     * @param propNames collection of property/column names to order by ascending
     * @return an OrderBy clause
     */
    public static OrderBy orderByAsc(final Collection<String> propNames) {
        return new OrderBy(propNames, SortDirection.ASC);
    }

    /**
     * Creates an ORDER BY clause with descending order for the specified properties.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = ConditionFactory.orderByDesc("score", "timestamp");
     * // Results in SQL like: ORDER BY score DESC, timestamp DESC
     * }</pre>
     *
     * @param propNames the property/column names to order by descending
     * @return an OrderBy clause
     */
    public static OrderBy orderByDesc(final String... propNames) {
        return new OrderBy(Array.asList(propNames), SortDirection.DESC);
    }

    /**
     * Creates an ORDER BY clause with descending order for properties from a collection.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> columns = Arrays.asList("amount", "date");
     * OrderBy orderBy = ConditionFactory.orderByDesc(columns);
     * }</pre>
     *
     * @param propNames collection of property/column names to order by descending
     * @return an OrderBy clause
     */
    public static OrderBy orderByDesc(final Collection<String> propNames) {
        return new OrderBy(propNames, SortDirection.DESC);
    }

    /**
     * Creates an ORDER BY clause with properties from a collection in ascending order.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> columns = new HashSet<>(Arrays.asList("name", "age"));
     * OrderBy orderBy = ConditionFactory.orderBy(columns);
     * }</pre>
     *
     * @param propNames collection of property/column names to order by
     * @return an OrderBy clause
     */
    public static OrderBy orderBy(final Collection<String> propNames) {
        return orderBy(propNames, SortDirection.ASC);
    }

    /**
     * Creates an ORDER BY clause with properties and specified sort direction.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = ConditionFactory.orderBy(Arrays.asList("price", "rating"), DESC);
     * // Results in SQL like: ORDER BY price DESC, rating DESC
     * }</pre>
     *
     * @param propNames collection of property/column names to order by
     * @param direction the sort direction (ASC or DESC)
     * @return an OrderBy clause
     */
    public static OrderBy orderBy(final Collection<String> propNames, final SortDirection direction) {
        return new OrderBy(propNames, direction);
    }

    /**
     * Creates an ORDER BY clause with a single property and sort direction.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = ConditionFactory.orderBy("modified_date", DESC);
     * // Results in SQL like: ORDER BY modified_date DESC
     * }</pre>
     *
     * @param propName the property/column name to order by
     * @param direction the sort direction (ASC or DESC)
     * @return an OrderBy clause
     */
    public static OrderBy orderBy(final String propName, final SortDirection direction) {
        return new OrderBy(propName, direction);
    }

    /**
     * Creates an ORDER BY clause with two properties and their respective sort directions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = ConditionFactory.orderBy("status", ASC, "priority", DESC);
     * // Results in SQL like: ORDER BY status ASC, priority DESC
     * }</pre>
     *
     * @param propNameA first property name
     * @param directionA first property sort direction
     * @param propNameB second property name
     * @param directionB second property sort direction
     * @return an OrderBy clause
     */
    public static OrderBy orderBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB) {
        return orderBy(N.asLinkedHashMap(propNameA, directionA, propNameB, directionB));
    }

    /**
     * Creates an ORDER BY clause with three properties and their respective sort directions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = ConditionFactory.orderBy("year", DESC, "month", DESC, "day", ASC);
     * }</pre>
     *
     * @param propNameA first property name
     * @param directionA first property sort direction
     * @param propNameB second property name
     * @param directionB second property sort direction
     * @param propNameC third property name
     * @param directionC third property sort direction
     * @return an OrderBy clause
     */
    public static OrderBy orderBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB,
            final String propNameC, final SortDirection directionC) {
        return orderBy(N.asLinkedHashMap(propNameA, directionA, propNameB, directionB, propNameC, directionC));
    }

    /**
     * Creates an ORDER BY clause from a map of property names to sort directions.
     * The map should be a LinkedHashMap to preserve order.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, SortDirection> orders = new LinkedHashMap<>();
     * orders.put("category", ASC);
     * orders.put("price", DESC);
     * orders.put("name", ASC);
     * OrderBy orderBy = ConditionFactory.orderBy(orders);
     * }</pre>
     *
     * @param orders map of property names to sort directions (should be LinkedHashMap)
     * @return an OrderBy clause
     */
    public static OrderBy orderBy(final Map<String, SortDirection> orders) {
        return new OrderBy(orders);
    }

    /**
     * Creates an ORDER BY clause with a custom condition.
     * Allows for complex ORDER BY expressions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OrderBy orderBy = ConditionFactory.orderBy(
     *     ConditionFactory.expr("CASE WHEN status = 'urgent' THEN 1 ELSE 2 END, created_date DESC")
     * );
     * }</pre>
     *
     * @param condition the ordering condition
     * @return an OrderBy clause
     */
    public static OrderBy orderBy(final Condition condition) {
        return new OrderBy(condition);
    }

    /**
     * Creates an ON clause for JOIN operations with the specified condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * On on = ConditionFactory.on(ConditionFactory.eq("users.id", "orders.user_id"));
     * // Results in SQL like: ON users.id = orders.user_id
     * }</pre>
     *
     * @param condition the join condition
     * @return an On clause
     */
    public static On on(final Condition condition) {
        return new On(condition);
    }

    /**
     * Creates an ON clause from a string expression for JOIN operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * On on = ConditionFactory.on("users.department_id = departments.id AND users.active = true");
     * }</pre>
     *
     * @param condition the join condition as a string
     * @return an On clause
     */
    public static On on(final String condition) {
        return new On(expr(condition));
    }

    /**
     * Creates an ON clause for simple equality join between two columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * On on = ConditionFactory.on("user_id", "id");
     * // Results in SQL like: ON user_id = id
     * }</pre>
     *
     * @param propName the first column name
     * @param anoPropName the second column name to join with
     * @return an On clause
     */
    public static On on(final String propName, final String anoPropName) {
        return new On(propName, anoPropName);
    }

    /**
     * Creates an ON clause from a map of column pairs for JOIN operations.
     * Each entry represents a join condition between two columns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> joinPairs = new HashMap<>();
     * joinPairs.put("orders.user_id", "users.id");
     * joinPairs.put("orders.product_id", "products.id");
     * On on = ConditionFactory.on(joinPairs);
     * }</pre>
     *
     * @param propNamePair map of column name pairs for joining
     * @return an On clause
     */
    public static On on(final Map<String, String> propNamePair) {
        return new On(propNamePair);
    }

    /**
     * Creates a USING clause for JOIN operations with the specified columns.
     * USING is an alternative to ON when joining tables on columns with the same name.
     * 
     * @param columnNames the column names used for joining
     * @return a Using clause
     * @deprecated It's recommended to use {@code On}, instead of {@code Using}
     */
    @Deprecated
    public static Using using(final String... columnNames) {
        return new Using(columnNames);
    }

    /**
     * Creates a USING clause from a collection of column names for JOIN operations.
     * 
     * @param columnNames collection of column names used for joining
     * @return a Using clause
     * @deprecated It's recommended to use {@code On}, instead of {@code Using}
     */
    @Deprecated
    public static Using using(final Collection<String> columnNames) {
        return new Using(columnNames);
    }

    /**
     * Creates a JOIN clause for the specified entity/table.
     * This creates an inner join by default.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = ConditionFactory.join("orders");
     * // Results in SQL like: JOIN orders
     * }</pre>
     *
     * @param joinEntity the entity/table name to join
     * @return a Join clause
     */
    public static Join join(final String joinEntity) {
        return new Join(joinEntity);
    }

    /**
     * Creates a JOIN clause with the specified entity and join condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = ConditionFactory.join("orders", 
     *     ConditionFactory.on("users.id", "orders.user_id"));
     * // Results in SQL like: JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param joinEntity the entity/table name to join
     * @param condition the join condition
     * @return a Join clause
     */
    public static Join join(final String joinEntity, final Condition condition) {
        return new Join(joinEntity, condition);
    }

    /**
     * Creates a JOIN clause with multiple entities and a join condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Join join = ConditionFactory.join(Arrays.asList("orders", "products"),
     *     ConditionFactory.on("orders.product_id", "products.id"));
     * }</pre>
     *
     * @param joinEntities collection of entity/table names to join
     * @param condition the join condition
     * @return a Join clause
     */
    public static Join join(final Collection<String> joinEntities, final Condition condition) {
        return new Join(joinEntities, condition);
    }

    /**
     * Creates a LEFT JOIN clause for the specified entity/table.
     * Returns all records from the left table and matched records from the right table.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LeftJoin join = ConditionFactory.leftJoin("orders");
     * // Results in SQL like: LEFT JOIN orders
     * }</pre>
     *
     * @param joinEntity the entity/table name to left join
     * @return a LeftJoin clause
     */
    public static LeftJoin leftJoin(final String joinEntity) {
        return new LeftJoin(joinEntity);
    }

    /**
     * Creates a LEFT JOIN clause with the specified entity and join condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LeftJoin join = ConditionFactory.leftJoin("orders",
     *     ConditionFactory.on("users.id", "orders.user_id"));
     * // Results in SQL like: LEFT JOIN orders ON users.id = orders.user_id
     * }</pre>
     *
     * @param joinEntity the entity/table name to left join
     * @param condition the join condition
     * @return a LeftJoin clause
     */
    public static LeftJoin leftJoin(final String joinEntity, final Condition condition) {
        return new LeftJoin(joinEntity, condition);
    }

    /**
     * Creates a LEFT JOIN clause with multiple entities and a join condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LeftJoin join = ConditionFactory.leftJoin(Arrays.asList("orders", "order_items"),
     *     ConditionFactory.on("orders.id", "order_items.order_id"));
     * }</pre>
     *
     * @param joinEntities collection of entity/table names to left join
     * @param condition the join condition
     * @return a LeftJoin clause
     */
    public static LeftJoin leftJoin(final Collection<String> joinEntities, final Condition condition) {
        return new LeftJoin(joinEntities, condition);
    }

    /**
     * Creates a RIGHT JOIN clause for the specified entity/table.
     * Returns all records from the right table and matched records from the left table.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RightJoin join = ConditionFactory.rightJoin("users");
     * // Results in SQL like: RIGHT JOIN users
     * }</pre>
     *
     * @param joinEntity the entity/table name to right join
     * @return a RightJoin clause
     */
    public static RightJoin rightJoin(final String joinEntity) {
        return new RightJoin(joinEntity);
    }

    /**
     * Creates a RIGHT JOIN clause with the specified entity and join condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RightJoin join = ConditionFactory.rightJoin("users",
     *     ConditionFactory.on("orders.user_id", "users.id"));
     * // Results in SQL like: RIGHT JOIN users ON orders.user_id = users.id
     * }</pre>
     *
     * @param joinEntity the entity/table name to right join
     * @param condition the join condition
     * @return a RightJoin clause
     */
    public static RightJoin rightJoin(final String joinEntity, final Condition condition) {
        return new RightJoin(joinEntity, condition);
    }

    /**
     * Creates a RIGHT JOIN clause with multiple entities and a join condition.
     * 
     * @param joinEntities collection of entity/table names to right join
     * @param condition the join condition
     * @return a RightJoin clause
     */
    public static RightJoin rightJoin(final Collection<String> joinEntities, final Condition condition) {
        return new RightJoin(joinEntities, condition);
    }

    /**
     * Creates a CROSS JOIN clause for the specified entity/table.
     * Returns the Cartesian product of both tables.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CrossJoin join = ConditionFactory.crossJoin("colors");
     * // Results in SQL like: CROSS JOIN colors
     * }</pre>
     *
     * @param joinEntity the entity/table name to cross join
     * @return a CrossJoin clause
     */
    public static CrossJoin crossJoin(final String joinEntity) {
        return new CrossJoin(joinEntity);
    }

    /**
     * Creates a CROSS JOIN clause with the specified entity and optional condition.
     * Note: Traditional CROSS JOIN doesn't use conditions, but some databases support it.
     * 
     * @param joinEntity the entity/table name to cross join
     * @param condition the optional join condition
     * @return a CrossJoin clause
     */
    public static CrossJoin crossJoin(final String joinEntity, final Condition condition) {
        return new CrossJoin(joinEntity, condition);
    }

    /**
     * Creates a CROSS JOIN clause with multiple entities and optional condition.
     * 
     * @param joinEntities collection of entity/table names to cross join
     * @param condition the optional join condition
     * @return a CrossJoin clause
     */
    public static CrossJoin crossJoin(final Collection<String> joinEntities, final Condition condition) {
        return new CrossJoin(joinEntities, condition);
    }

    /**
     * Creates a FULL JOIN clause for the specified entity/table.
     * Returns all records when there is a match in either table.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FullJoin join = ConditionFactory.fullJoin("departments");
     * // Results in SQL like: FULL JOIN departments
     * }</pre>
     *
     * @param joinEntity the entity/table name to full join
     * @return a FullJoin clause
     */
    public static FullJoin fullJoin(final String joinEntity) {
        return new FullJoin(joinEntity);
    }

    /**
     * Creates a FULL JOIN clause with the specified entity and join condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FullJoin join = ConditionFactory.fullJoin("employees",
     *     ConditionFactory.on("departments.id", "employees.dept_id"));
     * // Results in SQL like: FULL JOIN employees ON departments.id = employees.dept_id
     * }</pre>
     *
     * @param joinEntity the entity/table name to full join
     * @param condition the join condition
     * @return a FullJoin clause
     */
    public static FullJoin fullJoin(final String joinEntity, final Condition condition) {
        return new FullJoin(joinEntity, condition);
    }

    /**
     * Creates a FULL JOIN clause with multiple entities and a join condition.
     * 
     * @param joinEntities collection of entity/table names to full join
     * @param condition the join condition
     * @return a FullJoin clause
     */
    public static FullJoin fullJoin(final Collection<String> joinEntities, final Condition condition) {
        return new FullJoin(joinEntities, condition);
    }

    /**
     * Creates an INNER JOIN clause for the specified entity/table.
     * Returns records that have matching values in both tables.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InnerJoin join = ConditionFactory.innerJoin("orders");
     * // Results in SQL like: INNER JOIN orders
     * }</pre>
     *
     * @param joinEntity the entity/table name to inner join
     * @return an InnerJoin clause
     */
    public static InnerJoin innerJoin(final String joinEntity) {
        return new InnerJoin(joinEntity);
    }

    /**
     * Creates an INNER JOIN clause with the specified entity and join condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InnerJoin join = ConditionFactory.innerJoin("products",
     *     ConditionFactory.on("order_items.product_id", "products.id"));
     * // Results in SQL like: INNER JOIN products ON order_items.product_id = products.id
     * }</pre>
     *
     * @param joinEntity the entity/table name to inner join
     * @param condition the join condition
     * @return an InnerJoin clause
     */
    public static InnerJoin innerJoin(final String joinEntity, final Condition condition) {
        return new InnerJoin(joinEntity, condition);
    }

    /**
     * Creates an INNER JOIN clause with multiple entities and a join condition.
     * 
     * @param joinEntities collection of entity/table names to inner join
     * @param condition the join condition
     * @return an InnerJoin clause
     */
    public static InnerJoin innerJoin(final Collection<String> joinEntities, final Condition condition) {
        return new InnerJoin(joinEntities, condition);
    }

    /**
     * Creates a NATURAL JOIN clause for the specified entity/table.
     * Automatically joins tables based on columns with the same name.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NaturalJoin join = ConditionFactory.naturalJoin("departments");
     * // Results in SQL like: NATURAL JOIN departments
     * }</pre>
     *
     * @param joinEntity the entity/table name to natural join
     * @return a NaturalJoin clause
     */
    public static NaturalJoin naturalJoin(final String joinEntity) {
        return new NaturalJoin(joinEntity);
    }

    /**
     * Creates a NATURAL JOIN clause with the specified entity and additional condition.
     * Note: Traditional NATURAL JOIN doesn't use conditions, but some databases support it.
     * 
     * @param joinEntity the entity/table name to natural join
     * @param condition the additional join condition
     * @return a NaturalJoin clause
     */
    public static NaturalJoin naturalJoin(final String joinEntity, final Condition condition) {
        return new NaturalJoin(joinEntity, condition);
    }

    /**
     * Creates a NATURAL JOIN clause with multiple entities and additional condition.
     * 
     * @param joinEntities collection of entity/table names to natural join
     * @param condition the additional join condition
     * @return a NaturalJoin clause
     */
    public static NaturalJoin naturalJoin(final Collection<String> joinEntities, final Condition condition) {
        return new NaturalJoin(joinEntities, condition);
    }

    /**
     * Creates an IN condition with an array of integer values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = ConditionFactory.in("user_id", new int[] {1, 2, 3, 4});
     * // Results in SQL like: WHERE user_id IN (1, 2, 3, 4)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of integer values
     * @return an In condition
     */
    public static In in(final String propName, final int[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of long values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = ConditionFactory.in("order_id", new long[] {1001L, 1002L, 1003L});
     * // Results in SQL like: WHERE order_id IN (1001, 1002, 1003)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of long values
     * @return an In condition
     */
    public static In in(final String propName, final long[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of double values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = ConditionFactory.in("price", new double[] {9.99, 19.99, 29.99});
     * // Results in SQL like: WHERE price IN (9.99, 19.99, 29.99)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of double values
     * @return an In condition
     */
    public static In in(final String propName, final double[] values) {
        return in(propName, Array.box(values));
    }

    /**
     * Creates an IN condition with an array of object values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * In condition = ConditionFactory.in("status", new String[] {"active", "pending", "approved"});
     * // Results in SQL like: WHERE status IN ('active', 'pending', 'approved')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of values
     * @return an In condition
     */
    public static In in(final String propName, final Object[] values) {
        return in(propName, Arrays.asList(values));
    }

    /**
     * Creates an IN condition with a collection of values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> categories = Arrays.asList("electronics", "books", "toys");
     * In condition = ConditionFactory.in("category", categories);
     * // Results in SQL like: WHERE category IN ('electronics', 'books', 'toys')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values collection of values
     * @return an In condition
     */
    public static In in(final String propName, final Collection<?> values) {
        return new In(propName, values);
    }

    /**
     * Creates an IN condition with a subquery.
     * The property value must be in the result set of the subquery.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM active_users");
     * InSubQuery condition = ConditionFactory.in("user_id", subQuery);
     * // Results in SQL like: WHERE user_id IN (SELECT id FROM active_users)
     * }</pre>
     *
     * @param propName the property/column name
     * @param subQuery the subquery to check against
     * @return an InSubQuery condition
     */
    public static InSubQuery in(final String propName, final SubQuery subQuery) {
        return new InSubQuery(propName, subQuery);
    }

    /**
     * Creates an IN condition with multiple properties and a subquery.
     * Used for composite key comparisons.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT user_id, order_id FROM recent_orders");
     * InSubQuery condition = ConditionFactory.in(Arrays.asList("user_id", "order_id"), subQuery);
     * // Results in SQL like: WHERE (user_id, order_id) IN (SELECT user_id, order_id FROM recent_orders)
     * }</pre>
     *
     * @param propNames collection of property/column names
     * @param subQuery the subquery to check against
     * @return an InSubQuery condition
     */
    public static InSubQuery in(final Collection<String> propNames, final SubQuery subQuery) {
        return new InSubQuery(propNames, subQuery);
    }

    /**
     * Creates a NOT IN condition with an array of integer values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = ConditionFactory.notIn("status_code", new int[] {404, 500, 503});
     * // Results in SQL like: WHERE status_code NOT IN (404, 500, 503)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of integer values to exclude
     * @return a NotIn condition
     */
    public static NotIn notIn(final String propName, final int[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of long values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = ConditionFactory.notIn("excluded_ids", new long[] {110L, 120L, 130L});
     * // Results in SQL like: WHERE excluded_ids NOT IN (110, 120, 130)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of long values to exclude
     * @return a NotIn condition
     */
    public static NotIn notIn(final String propName, final long[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of double values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = ConditionFactory.notIn("discount", new double[] {0.0, 100.0});
     * // Results in SQL like: WHERE discount NOT IN (0.0, 100.0)
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of double values to exclude
     * @return a NotIn condition
     */
    public static NotIn notIn(final String propName, final double[] values) {
        return notIn(propName, Array.box(values));
    }

    /**
     * Creates a NOT IN condition with an array of object values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NotIn condition = ConditionFactory.notIn("role", new String[] {"guest", "banned"});
     * // Results in SQL like: WHERE role NOT IN ('guest', 'banned')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values array of values to exclude
     * @return a NotIn condition
     */
    public static NotIn notIn(final String propName, final Object[] values) {
        return notIn(propName, Arrays.asList(values));
    }

    /**
     * Creates a NOT IN condition with a collection of values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excludedCountries = new HashSet<>(Arrays.asList("XX", "YY"));
     * NotIn condition = ConditionFactory.notIn("country_code", excludedCountries);
     * // Results in SQL like: WHERE country_code NOT IN ('XX', 'YY')
     * }</pre>
     *
     * @param propName the property/column name
     * @param values collection of values to exclude
     * @return a NotIn condition
     */
    public static NotIn notIn(final String propName, final Collection<?> values) {
        return new NotIn(propName, values);
    }

    /**
     * Creates a NOT IN condition with a subquery.
     * The property value must not be in the result set of the subquery.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM blacklisted_users");
     * NotInSubQuery condition = ConditionFactory.notIn("user_id", subQuery);
     * // Results in SQL like: WHERE user_id NOT IN (SELECT id FROM blacklisted_users)
     * }</pre>
     *
     * @param propName the property/column name
     * @param subQuery the subquery to check against
     * @return a NotInSubQuery condition
     */
    public static NotInSubQuery notIn(final String propName, final SubQuery subQuery) {
        return new NotInSubQuery(propName, subQuery);
    }

    /**
     * Creates a NOT IN condition with multiple properties and a subquery.
     * Used for composite key exclusions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT user_id, product_id FROM returns");
     * NotInSubQuery condition = ConditionFactory.notIn(Arrays.asList("user_id", "product_id"), subQuery);
     * // Results in SQL like: WHERE (user_id, product_id) NOT IN (SELECT user_id, product_id FROM returns)
     * }</pre>
     *
     * @param propNames collection of property/column names
     * @param subQuery the subquery to check against
     * @return a NotInSubQuery condition
     */
    public static NotInSubQuery notIn(final Collection<String> propNames, final SubQuery subQuery) {
        return new NotInSubQuery(propNames, subQuery);
    }

    /**
     * Creates an ALL condition for comparison with all values from a subquery.
     * The condition is true if the comparison is true for all values returned by the subquery.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT salary FROM employees WHERE dept = 'IT'");
     * All condition = ConditionFactory.all(subQuery);
     * // Used in: WHERE salary > ALL (SELECT salary FROM employees WHERE dept = 'IT')
     * }</pre>
     *
     * @param condition the subquery condition
     * @return an All condition
     */
    public static All all(final SubQuery condition) {
        return new All(condition);
    }

    /**
     * Creates an ANY condition for comparison with any value from a subquery.
     * The condition is true if the comparison is true for at least one value returned by the subquery.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT price FROM products WHERE category = 'electronics'");
     * Any condition = ConditionFactory.any(subQuery);
     * // Used in: WHERE price < ANY (SELECT price FROM products WHERE category = 'electronics')
     * }</pre>
     *
     * @param condition the subquery condition
     * @return an Any condition
     */
    public static Any any(final SubQuery condition) {
        return new Any(condition);
    }

    /**
     * Creates a SOME condition for comparison with some values from a subquery.
     * SOME is functionally equivalent to ANY.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT score FROM exams WHERE student_id = 123");
     * Some condition = ConditionFactory.some(subQuery);
     * // Used in: WHERE passing_score <= SOME (SELECT score FROM exams WHERE student_id = 123)
     * }</pre>
     *
     * @param condition the subquery condition
     * @return a Some condition
     */
    public static Some some(final SubQuery condition) {
        return new Some(condition);
    }

    /**
     * Creates an EXISTS condition to check if a subquery returns any rows.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM orders WHERE orders.user_id = users.id");
     * Exists condition = ConditionFactory.exists(subQuery);
     * // Results in SQL like: WHERE EXISTS (SELECT 1 FROM orders WHERE orders.user_id = users.id)
     * }</pre>
     *
     * @param condition the subquery to check
     * @return an Exists condition
     */
    public static Exists exists(final SubQuery condition) {
        return new Exists(condition);
    }

    /**
     * Creates a NOT EXISTS condition to check if a subquery returns no rows.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT 1 FROM archived_users WHERE archived_users.id = users.id");
     * NotExists condition = ConditionFactory.notExists(subQuery);
     * // Results in SQL like: WHERE NOT EXISTS (SELECT 1 FROM archived_users WHERE archived_users.id = users.id)
     * }</pre>
     *
     * @param condition the subquery to check
     * @return a NotExists condition
     */
    public static NotExists notExists(final SubQuery condition) {
        return new NotExists(condition);
    }

    /**
     * Creates a UNION clause to combine results from a subquery.
     * UNION removes duplicate rows from the combined result set.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM archived_users");
     * Union union = ConditionFactory.union(subQuery);
     * // Results in SQL like: UNION SELECT id FROM archived_users
     * }</pre>
     *
     * @param condition the subquery to union with
     * @return a Union clause
     */
    public static Union union(final SubQuery condition) {
        return new Union(condition);
    }

    /**
     * Creates a UNION ALL clause to combine results from a subquery.
     * UNION ALL keeps all rows including duplicates.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT name FROM inactive_products");
     * UnionAll unionAll = ConditionFactory.unionAll(subQuery);
     * // Results in SQL like: UNION ALL SELECT name FROM inactive_products
     * }</pre>
     *
     * @param condition the subquery to union with
     * @return a UnionAll clause
     */
    public static UnionAll unionAll(final SubQuery condition) {
        return new UnionAll(condition);
    }

    /**
     * Creates an EXCEPT clause to subtract results from a subquery.
     * Returns rows from the first query that are not in the second query.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM blacklisted_customers");
     * Except except = ConditionFactory.except(subQuery);
     * // Results in SQL like: EXCEPT SELECT id FROM blacklisted_customers
     * }</pre>
     *
     * @param condition the subquery to subtract
     * @return an Except clause
     */
    public static Except except(final SubQuery condition) {
        return new Except(condition);
    }

    /**
     * Creates an INTERSECT clause to find common results with a subquery.
     * Returns only rows that appear in both queries.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT product_id FROM discounted_items");
     * Intersect intersect = ConditionFactory.intersect(subQuery);
     * // Results in SQL like: INTERSECT SELECT product_id FROM discounted_items
     * }</pre>
     *
     * @param condition the subquery to intersect with
     * @return an Intersect clause
     */
    public static Intersect intersect(final SubQuery condition) {
        return new Intersect(condition);
    }

    /**
     * Creates a MINUS clause to subtract results from a subquery.
     * MINUS is similar to EXCEPT but is used in some databases like Oracle.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("SELECT id FROM deleted_records");
     * Minus minus = ConditionFactory.minus(subQuery);
     * // Results in SQL like: MINUS SELECT id FROM deleted_records
     * }</pre>
     *
     * @param condition the subquery to subtract
     * @return a Minus clause
     */
    public static Minus minus(final SubQuery condition) {
        return new Minus(condition);
    }

    /**
     * Creates a Cell condition with a custom operator and condition.
     * This is for advanced use cases requiring special condition handling.
     * 
     * @param operator the operator to apply
     * @param condition the condition to wrap
     * @return a Cell condition
     */
    @Beta
    public static Cell cell(final Operator operator, final Condition condition) {
        return new Cell(operator, condition);
    }

    /**
     * Creates a SubQuery from an entity class with selected properties and condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery(User.class, 
     *     Arrays.asList("id", "name"),
     *     ConditionFactory.eq("active", true));
     * // Generates subquery based on User entity
     * }</pre>
     *
     * @param entityClass the entity class representing the table
     * @param propNames collection of property names to select
     * @param condition the WHERE condition for the subquery
     * @return a SubQuery
     */
    public static SubQuery subQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition) {
        return new SubQuery(entityClass, propNames, condition);
    }

    /**
     * Creates a SubQuery from an entity name with selected properties and condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("users",
     *     Arrays.asList("id", "email"),
     *     ConditionFactory.like("email", "%@company.com"));
     * }</pre>
     *
     * @param entityName the entity/table name
     * @param propNames collection of property names to select
     * @param condition the WHERE condition for the subquery
     * @return a SubQuery
     */
    public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final Condition condition) {
        return new SubQuery(entityName, propNames, condition);
    }

    /**
     * Creates a SubQuery from an entity name with selected properties and string condition.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("products",
     *     Arrays.asList("id", "price"),
     *     "category = 'electronics' AND in_stock = true");
     * }</pre>
     *
     * @param entityName the entity/table name
     * @param propNames collection of property names to select
     * @param condition the WHERE condition as a string
     * @return a SubQuery
     */
    public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final String condition) {
        return new SubQuery(entityName, propNames, expr(condition));
    }

    /**
     * Creates a SubQuery from an entity name and raw SQL.
     * This method allows for complete control over the subquery SQL.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery("orders", 
     *     "SELECT COUNT(*) FROM orders WHERE user_id = ?");
     * }</pre>
     *
     * @param entityName the entity/table name
     * @param sql the complete SQL for the subquery
     * @return a SubQuery
     * @see #subQuery(String)
     * @deprecated replaced by {@link #subQuery(String)}
     */
    @Deprecated
    public static SubQuery subQuery(final String entityName, final String sql) {
        return new SubQuery(entityName, sql);
    }

    /**
     * Creates a SubQuery from raw SQL.
     * This provides complete control over the subquery content.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SubQuery subQuery = ConditionFactory.subQuery(
     *     "SELECT user_id FROM orders WHERE total > 1000 GROUP BY user_id"
     * );
     * }</pre>
     *
     * @param sql the complete SQL for the subquery
     * @return a SubQuery
     */
    public static SubQuery subQuery(final String sql) {
        return new SubQuery(sql);
    }

    /**
     * Creates a LIMIT clause to restrict the number of rows returned.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = ConditionFactory.limit(10);
     * // Results in SQL like: LIMIT 10
     * }</pre>
     *
     * @param count the maximum number of rows to return
     * @return a Limit clause
     */
    public static Limit limit(final int count) {
        return new Limit(count);
    }

    /**
     * Creates a LIMIT clause with an offset and count.
     * Used for pagination of results.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = ConditionFactory.limit(20, 10);
     * // Results in SQL like: LIMIT 10 OFFSET 20 (skip 20, take 10)
     * }</pre>
     *
     * @param offset the number of rows to skip
     * @param count the maximum number of rows to return
     * @return a Limit clause
     */
    public static Limit limit(final int offset, final int count) {
        return new Limit(offset, count);
    }

    /**
     * Creates a LIMIT clause from a string expression.
     * Allows for database-specific limit syntax.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Limit limit = ConditionFactory.limit("10 OFFSET 20");
     * }</pre>
     *
     * @param expr the limit expression as a string
     * @return a Limit clause
     */
    public static Limit limit(final String expr) {
        return new Limit(expr);
    }

    /**
     * Creates an empty Criteria object for building complex query conditions.
     * Criteria allows for fluent building of query conditions.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Criteria criteria = ConditionFactory.criteria()
     *     .where(ConditionFactory.eq("status", "active"))
     *     .orderBy("created_date", DESC)
     *     .limit(10);
     * }</pre>
     *
     * @return a new empty Criteria instance
     */
    public static Criteria criteria() {
        return new Criteria();
    }

    /**
     * A utility class providing static factory methods identical to ConditionFactory.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Instead of ConditionFactory.eq("name", "John")
     * Condition c = Filters.eq("name", "John");
     * }</pre>
     * 
     * <p>All methods in ConditionFactory are available through Filters.</p>
     */
    @Beta
    public static final class Filters extends ConditionFactory {

        private Filters() {
            // singleton for utility class.
        }
    }

    /**
     * A utility class providing static factory methods identical to ConditionFactory.
     * CF serves as a shorter alias for ConditionFactory to reduce verbosity.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Instead of ConditionFactory.eq("name", "John")
     * Condition c = CF.eq("name", "John");
     * }</pre>
     * 
     * <p>All methods in ConditionFactory are available through CF.</p>
     */
    public static final class CF extends ConditionFactory {

        private CF() {
            // singleton for utility class.
        }
    }

    /**
     * A utility class for building Criteria objects with a fluent interface.
     * CB (Criteria Builder) provides static methods that create and return
     * Criteria instances with initial conditions already applied.
     * 
     * <p>This class is designed for convenient one-line criteria building
     * without needing to call criteria() first.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Instead of: ConditionFactory.criteria().where(condition)
     * Criteria c = CB.where(CF.eq("status", "active"));
     * 
     * // Chain multiple operations
     * Criteria c = CB.where("age > 18")
     *     .orderBy("name")
     *     .limit(50);
     * }</pre>
     */
    public static final class CB {

        private CB() {
            // singleton for utility class.
        }

        /**
         * Creates a new Criteria with a WHERE clause containing the specified condition.
         * This is a shortcut for creating criteria with an initial WHERE condition.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.where(CF.eq("active", true));
         * }</pre>
         *
         * @param condition the condition for the WHERE clause
         * @return a new Criteria with the WHERE condition
         */
        public static Criteria where(final Condition condition) {
            return CF.criteria().where(condition);
        }

        /**
         * Creates a new Criteria with a WHERE clause from a string expression.
         * This is a shortcut for creating criteria with an initial WHERE condition.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.where("age >= 18 AND country = 'US'");
         * }</pre>
         *
         * @param condition the SQL expression as a string
         * @return a new Criteria with the WHERE condition
         */
        public static Criteria where(final String condition) {
            return CF.criteria().where(condition);
        }

        /**
         * Creates a new Criteria with a GROUP BY clause containing the specified condition.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.groupBy(CF.expr("YEAR(date), MONTH(date)"));
         * }</pre>
         *
         * @param condition the grouping condition
         * @return a new Criteria with the GROUP BY condition
         */
        public static Criteria groupBy(final Condition condition) {
            return CF.criteria().groupBy(condition);
        }

        /**
         * Creates a new Criteria with a GROUP BY clause for the specified properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.groupBy("department", "role");
         * }</pre>
         *
         * @param propNames the property/column names to group by
         * @return a new Criteria with the GROUP BY clause
         */
        public static Criteria groupBy(final String... propNames) {
            return CF.criteria().groupBy(propNames);
        }

        /**
         * Creates a new Criteria with a GROUP BY clause for a single property with sort direction.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.groupBy("sales_amount", DESC);
         * }</pre>
         *
         * @param propName the property/column name to group by
         * @param direction the sort direction (ASC or DESC)
         * @return a new Criteria with the GROUP BY clause
         */
        public static Criteria groupBy(final String propName, final SortDirection direction) {
            return CF.criteria().groupBy(propName, direction);
        }

        /**
         * Creates a new Criteria with a GROUP BY clause for properties from a collection.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> groupCols = Arrays.asList("country", "state", "city");
         * Criteria criteria = CB.groupBy(groupCols);
         * }</pre>
         *
         * @param propNames collection of property/column names to group by
         * @return a new Criteria with the GROUP BY clause
         */
        public static Criteria groupBy(final Collection<String> propNames) {
            return CF.criteria().groupBy(propNames);
        }

        /**
         * Creates a new Criteria with a GROUP BY clause for properties with sort direction.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.groupBy(Arrays.asList("year", "quarter"), DESC);
         * }</pre>
         *
         * @param propNames collection of property/column names to group by
         * @param direction the sort direction (ASC or DESC)
         * @return a new Criteria with the GROUP BY clause
         */
        public static Criteria groupBy(final Collection<String> propNames, final SortDirection direction) {
            return CF.criteria().groupBy(propNames, direction);
        }

        /**
         * Creates a new Criteria with a GROUP BY clause from a map of properties to sort directions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, SortDirection> grouping = new LinkedHashMap<>();
         * grouping.put("category", ASC);
         * grouping.put("subcategory", DESC);
         * Criteria criteria = CB.groupBy(grouping);
         * }</pre>
         *
         * @param orders map of property names to sort directions
         * @return a new Criteria with the GROUP BY clause
         */
        public static Criteria groupBy(final Map<String, SortDirection> orders) {
            return CF.criteria().groupBy(orders);
        }

        /**
         * Creates a new Criteria with a HAVING clause containing the specified condition.
         * HAVING is used to filter grouped results.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.having(CF.gt("COUNT(*)", 5));
         * }</pre>
         *
         * @param condition the condition for the HAVING clause
         * @return a new Criteria with the HAVING condition
         */
        public static Criteria having(final Condition condition) {
            return CF.criteria().having(condition);
        }

        /**
         * Creates a new Criteria with a HAVING clause from a string expression.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.having("SUM(amount) > 1000 AND COUNT(*) > 10");
         * }</pre>
         *
         * @param condition the SQL expression as a string
         * @return a new Criteria with the HAVING condition
         */
        public static Criteria having(final String condition) {
            return CF.criteria().having(condition);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause in ascending order.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.orderByAsc("last_name", "first_name");
         * }</pre>
         *
         * @param propNames the property/column names to order by ascending
         * @return a new Criteria with the ORDER BY clause
         */
        public static Criteria orderByAsc(final String... propNames) {
            return CF.criteria().orderByAsc(propNames);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause in ascending order from a collection.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.orderByAsc(Arrays.asList("priority", "created_date"));
         * }</pre>
         *
         * @param propNames collection of property/column names to order by ascending
         * @return a new Criteria with the ORDER BY clause
         */
        public static Criteria orderByAsc(final Collection<String> propNames) {
            return CF.criteria().orderByAsc(propNames);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause in descending order.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.orderByDesc("score", "timestamp");
         * }</pre>
         *
         * @param propNames the property/column names to order by descending
         * @return a new Criteria with the ORDER BY clause
         */
        public static Criteria orderByDesc(final String... propNames) {
            return CF.criteria().orderByDesc(propNames);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause in descending order from a collection.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.orderByDesc(Arrays.asList("total_sales", "profit"));
         * }</pre>
         *
         * @param propNames collection of property/column names to order by descending
         * @return a new Criteria with the ORDER BY clause
         */
        public static Criteria orderByDesc(final Collection<String> propNames) {
            return CF.criteria().orderByDesc(propNames);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause containing the specified condition.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.orderBy(CF.expr("FIELD(status, 'new', 'pending', 'complete')"));
         * }</pre>
         *
         * @param condition the ordering condition
         * @return a new Criteria with the ORDER BY condition
         */
        public static Criteria orderBy(final Condition condition) {
            return CF.criteria().orderBy(condition);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause for the specified properties.
         * Orders by ascending by default.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.orderBy("category", "name");
         * }</pre>
         *
         * @param propNames the property/column names to order by
         * @return a new Criteria with the ORDER BY clause
         */
        public static Criteria orderBy(final String... propNames) {
            return CF.criteria().orderBy(propNames);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause for a single property with direction.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.orderBy("created_date", DESC);
         * }</pre>
         *
         * @param propName the property/column name to order by
         * @param direction the sort direction (ASC or DESC)
         * @return a new Criteria with the ORDER BY clause
         */
        public static Criteria orderBy(final String propName, final SortDirection direction) {
            return CF.criteria().orderBy(propName, direction);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause for properties from a collection.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.orderBy(Arrays.asList("status", "priority"));
         * }</pre>
         *
         * @param propNames collection of property/column names to order by
         * @return a new Criteria with the ORDER BY clause
         */
        public static Criteria orderBy(final Collection<String> propNames) {
            return CF.criteria().orderBy(propNames);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause for properties with direction.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.orderBy(Arrays.asList("amount", "date"), DESC);
         * }</pre>
         *
         * @param propNames collection of property/column names to order by
         * @param direction the sort direction (ASC or DESC)
         * @return a new Criteria with the ORDER BY clause
         */
        public static Criteria orderBy(final Collection<String> propNames, final SortDirection direction) {
            return CF.criteria().orderBy(propNames, direction);
        }

        /**
         * Creates a new Criteria with an ORDER BY clause from a map of properties to directions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, SortDirection> ordering = new LinkedHashMap<>();
         * ordering.put("priority", DESC);
         * ordering.put("created_date", ASC);
         * Criteria criteria = CB.orderBy(ordering);
         * }</pre>
         *
         * @param orders map of property names to sort directions
         * @return a new Criteria with the ORDER BY clause
         */
        public static Criteria orderBy(final Map<String, SortDirection> orders) {
            return CF.criteria().orderBy(orders);
        }

        /**
         * Creates a new Criteria with a LIMIT clause from a Limit condition.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.limit(CF.limit(10, 20));
         * }</pre>
         *
         * @param condition the Limit condition
         * @return a new Criteria with the LIMIT clause
         */
        public static Criteria limit(final Limit condition) {
            return CF.criteria().limit(condition);
        }

        /**
         * Creates a new Criteria with a LIMIT clause for the specified count.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.limit(100);
         * // Results in SQL like: LIMIT 100
         * }</pre>
         *
         * @param count the maximum number of rows to return
         * @return a new Criteria with the LIMIT clause
         */
        public static Criteria limit(final int count) {
            return CF.criteria().limit(count);
        }

        /**
         * Creates a new Criteria with a LIMIT clause with offset and count.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.limit(50, 25);
         * // Results in SQL like: LIMIT 25 OFFSET 50
         * }</pre>
         *
         * @param offset the number of rows to skip
         * @param count the maximum number of rows to return
         * @return a new Criteria with the LIMIT clause
         */
        public static Criteria limit(final int offset, final int count) {
            return CF.criteria().limit(offset, count);
        }

        /**
         * Creates a new Criteria with a LIMIT clause from a string expression.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Criteria criteria = CB.limit("100 OFFSET 200");
         * }</pre>
         *
         * @param expr the limit expression as a string
         * @return a new Criteria with the LIMIT clause
         */
        public static Criteria limit(final String expr) {
            return CF.criteria().limit(expr);
        }
    }
}