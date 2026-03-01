/*
 * Copyright (c) 2015, Haiyang Li.
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

package com.landawn.abacus.query;

import static com.landawn.abacus.query.SK._PARENTHESES_L;
import static com.landawn.abacus.query.SK._PARENTHESES_R;
import static com.landawn.abacus.query.SK._SPACE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.query.condition.Between;
import com.landawn.abacus.query.condition.Binary;
import com.landawn.abacus.query.condition.Cell;
import com.landawn.abacus.query.condition.Condition;
import com.landawn.abacus.query.condition.Expression;
import com.landawn.abacus.query.condition.Having;
import com.landawn.abacus.query.condition.In;
import com.landawn.abacus.query.condition.InSubQuery;
import com.landawn.abacus.query.condition.Junction;
import com.landawn.abacus.query.condition.NotBetween;
import com.landawn.abacus.query.condition.NotIn;
import com.landawn.abacus.query.condition.NotInSubQuery;
import com.landawn.abacus.query.condition.SubQuery;
import com.landawn.abacus.query.condition.Where;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.OperationType;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.Optional;

/**
 * A comprehensive, enterprise-grade fluent SQL builder providing type-safe, programmatic construction
 * of complex SQL statements with advanced features including parameterized queries, multiple naming
 * conventions, entity mapping, and sophisticated query optimization. This abstract base class serves
 * as the foundation for building dynamic, secure SQL queries that prevent SQL injection attacks while
 * maintaining optimal database performance through intelligent query generation and caching strategies.
 *
 * <p>The {@code SQLBuilder} class addresses critical challenges in enterprise database programming by
 * providing a fluent, type-safe API for constructing SQL statements programmatically. It supports the
 * complete spectrum of SQL operations (SELECT, INSERT, UPDATE, DELETE) with advanced features like
 * complex joins, subqueries, window functions, and Common Table Expressions (CTEs), enabling developers
 * to build sophisticated database queries while maintaining code readability and ensuring security
 * through parameterized query generation.</p>
 *
 * <p><b>⚠️ IMPORTANT - Resource Management:</b>
 * All SQLBuilder instances must be properly finalized by calling {@code sql()}, {@code build()}, or
 * {@code pair()} to generate the final SQL string and release internal resources. Failure to finalize
 * builder instances may result in memory leaks in long-running applications. Always use try-with-resources
 * or ensure proper cleanup in production environments.</p>
 *
 * <p><b>Key Features and Capabilities:</b>
 * <ul>
 *   <li><b>Complete SQL Operation Support:</b> Full coverage of SELECT, INSERT, UPDATE, DELETE, and DDL operations</li>
 *   <li><b>Parameterized Query Generation:</b> Automatic parameter binding preventing SQL injection vulnerabilities</li>
 *   <li><b>Multiple Naming Conventions:</b> Support for snake_case, UPPER_CASE, camelCase, and custom naming policies</li>
 *   <li><b>Entity Class Integration:</b> Seamless mapping with JPA-style annotations and reflection-based field mapping</li>
 *   <li><b>Advanced Join Operations:</b> Comprehensive support for INNER, LEFT, RIGHT, FULL OUTER, and CROSS joins</li>
 *   <li><b>Subquery Integration:</b> Nested queries, correlated subqueries, and EXISTS/NOT EXISTS operations</li>
 *   <li><b>Window Function Support:</b> ROW_NUMBER, RANK, DENSE_RANK, and aggregate window functions</li>
 *   <li><b>CTE (Common Table Expressions):</b> WITH clause support for complex recursive and non-recursive queries</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Type Safety First:</b> Compile-time validation and type checking for all SQL construction operations</li>
 *   <li><b>Security by Design:</b> All operations generate parameterized SQL preventing injection attacks</li>
 *   <li><b>Fluent Interface:</b> Method chaining enables readable, expressive query building patterns</li>
 *   <li><b>Performance Optimized:</b> Generated SQL is optimized for database execution plan efficiency</li>
 *   <li><b>Framework Agnostic:</b> Works with any JDBC-based framework or standalone applications</li>
 *   <li><b>Extensible Architecture:</b> Abstract design allows for custom implementations and extensions</li>
 * </ul>
 *
 * <p><b>Concrete Implementation Classes and their Characteristics:</b>
 * <table border="1" style="border-collapse: collapse;">
 *   <caption>SQLBuilder Implementation Classes and Their Characteristics</caption>
 *   <tr style="background-color: #f2f2f2;">
 *     <th>Class</th>
 *     <th>Parameter Style</th>
 *     <th>Naming Convention</th>
 *     <th>Use Cases</th>
 *     <th>Example Output</th>
 *   </tr>
 *   <tr>
 *     <td>PSC</td>
 *     <td>Parameterized (?)</td>
 *     <td>snake_case</td>
 *     <td>JDBC PreparedStatement</td>
 *     <td>SELECT first_name FROM users WHERE id = ?</td>
 *   </tr>
 *   <tr>
 *     <td>PAC</td>
 *     <td>Parameterized (?)</td>
 *     <td>UPPER_CASE</td>
 *     <td>Legacy database systems</td>
 *     <td>SELECT FIRST_NAME FROM USERS WHERE ID = ?</td>
 *   </tr>
 *   <tr>
 *     <td>PLC</td>
 *     <td>Parameterized (?)</td>
 *     <td>camelCase</td>
 *     <td>Modern ORM frameworks</td>
 *     <td>SELECT firstName FROM users WHERE id = ?</td>
 *   </tr>
 *   <tr>
 *     <td>NSC</td>
 *     <td>Named (:name)</td>
 *     <td>snake_case</td>
 *     <td>Named parameter frameworks</td>
 *     <td>SELECT first_name FROM users WHERE id = :id</td>
 *   </tr>
 *   <tr>
 *     <td>NAC</td>
 *     <td>Named (:name)</td>
 *     <td>UPPER_CASE</td>
 *     <td>Enterprise applications</td>
 *     <td>SELECT FIRST_NAME FROM USERS WHERE ID = :ID</td>
 *   </tr>
 *   <tr>
 *     <td>NLC</td>
 *     <td>Named (:name)</td>
 *     <td>camelCase</td>
 *     <td>Modern web applications</td>
 *     <td>SELECT firstName FROM users WHERE id = :id</td>
 *   </tr>
 * </table>
 *
 * <p><b>Core SQL Operation Categories:</b>
 * <ul>
 *   <li><b>Query Operations:</b> {@code select()}, {@code selectFrom()}, {@code from()}, {@code where()}, {@code orderBy()}</li>
 *   <li><b>Data Modification:</b> {@code insert()}, {@code insertInto()}, {@code update()}, {@code set()}, {@code delete()}</li>
 *   <li><b>Join Operations:</b> {@code join()}, {@code leftJoin()}, {@code rightJoin()}, {@code fullJoin()}, {@code crossJoin()}</li>
 *   <li><b>Aggregation:</b> {@code groupBy()}, {@code having()}, {@code distinct()}, {@code union()}, {@code unionAll()}</li>
 *   <li><b>Subqueries:</b> {@code exists()}, {@code notExists()}, {@code in()}, {@code notIn()}, {@code subQuery()}</li>
 *   <li><b>Window Functions:</b> {@code over()}, {@code partitionBy()}, {@code rowNumber()}, {@code rank()}</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic SELECT query with parameterized conditions
 * String sql = PSC.select("firstName", "lastName", "email")
 *     .from("users")
 *     .where(Filters.eq("department", "Engineering"))
 *     .and(Filters.gt("salary", 50000))
 *     .orderBy("lastName", "firstName")
 *     .sql();
 * // Output: SELECT first_name AS "firstName", last_name AS "lastName", email 
 * //         FROM users WHERE department = ? AND salary > ? ORDER BY last_name, first_name
 *
 * // Complex JOIN query with multiple tables
 * String sql = PSC.select("u.firstName", "u.lastName", "d.name", "COUNT(p.id)")
 *     .from("users", "u")
 *     .join("departments", "d").on("u.department_id = d.id")
 *     .leftJoin("projects", "p").on("u.id = p.assigned_user_id")
 *     .where(Filters.eq("u.active", true))
 *     .groupBy("u.id", "u.firstName", "u.lastName", "d.name")
 *     .having(Filters.gt("COUNT(p.id)", 2))
 *     .orderBy("u.lastName")
 *     .sql();
 *
 * // INSERT with entity object mapping
 * User user = new User("John", "Doe", "john.doe@company.com");
 * String sql = PSC.insert(user).into("users").sql();
 * // Automatically maps entity fields to database columns
 *
 * // UPDATE with selective field updates
 * String sql = PSC.update("users")
 *     .set("last_login", LocalDateTime.now())
 *     .set("login_count", "login_count + 1")
 *     .where(Filters.eq("id", userId))
 *     .sql();
 *
 * // Named parameter query for Spring/Hibernate integration
 * String sql = NSC.select("*")
 *     .from("orders")
 *     .where(Filters.between("order_date", ":startDate", ":endDate"))
 *     .and(Filters.in("status", ":statusList"))
 *     .sql();
 * }</pre>
 *
 * <p><b>Entity Mapping and Annotation Support:</b>
 * <ul>
 *   <li><b>JPA Annotations:</b> {@code @Table}, {@code @Column}, {@code @Id}, {@code @Transient}</li>
 *   <li><b>Abacus Annotations:</b> {@code @ReadOnly}, {@code @ReadOnlyId}, {@code @NonUpdatable}</li>
 *   <li><b>Automatic Field Mapping:</b> Reflection-based mapping with naming convention conversion</li>
 *   <li><b>Custom Column Names:</b> Override default naming with annotation-based configuration</li>
 *   <li><b>Type Conversion:</b> Automatic Java-to-SQL type mapping with custom converter support</li>
 * </ul>
 *
 * <p><b>Naming Convention Support:</b>
 * <ul>
 *   <li><b>snake_case:</b> Standard database naming (user_name, created_date)</li>
 *   <li><b>UPPER_CASE:</b> Legacy database systems (USER_NAME, CREATED_DATE)</li>
 *   <li><b>camelCase:</b> Java-style naming (userName, createdDate)</li>
 *   <li><b>Custom Policies:</b> Implement {@code NamingPolicy} for specialized naming requirements</li>
 *   <li><b>Mixed Conventions:</b> Support for databases with multiple naming conventions</li>
 * </ul>
 *
 * <p><b>Parameter Binding and Security:</b>
 * <ul>
 *   <li><b>Parameterized Queries:</b> All values are automatically parameterized to prevent SQL injection</li>
 *   <li><b>Named Parameters:</b> Support for named parameter binding (:paramName)</li>
 *   <li><b>Collection Parameters:</b> Automatic expansion of collections for IN clauses</li>
 *   <li><b>Type Safety:</b> Compile-time type checking for parameter values</li>
 *   <li><b>Null Handling:</b> Proper SQL NULL semantics for null values</li>
 * </ul>
 *
 * <p><b>Performance Optimization Features:</b>
 * <ul>
 *   <li><b>Query Plan Optimization:</b> Generated SQL is optimized for database execution plans</li>
 *   <li><b>Index-Friendly Operations:</b> Condition ordering optimized for index usage</li>
 *   <li><b>Batch Operation Support:</b> Efficient batch INSERT/UPDATE/DELETE operations</li>
 *   <li><b>Memory Efficiency:</b> Minimal object allocation during query construction</li>
 *   <li><b>Caching Support:</b> Integration with query result caching frameworks</li>
 * </ul>
 *
 * <p><b>Advanced SQL Features:</b>
 * <ul>
 *   <li><b>Window Functions:</b> ROW_NUMBER(), RANK(), DENSE_RANK(), LAG(), LEAD()</li>
 *   <li><b>Aggregate Functions:</b> COUNT(), SUM(), AVG(), MIN(), MAX() with OVER clauses</li>
 *   <li><b>Common Table Expressions:</b> WITH clauses for recursive and non-recursive queries</li>
 *   <li><b>Set Operations:</b> UNION, UNION ALL, INTERSECT, EXCEPT operations</li>
 *   <li><b>Conditional Logic:</b> CASE WHEN expressions and COALESCE functions</li>
 * </ul>
 *
 * <p><b>Error Handling and Validation:</b>
 * <ul>
 *   <li><b>SQL Syntax Validation:</b> Early detection of potential SQL syntax issues</li>
 *   <li><b>Parameter Validation:</b> Comprehensive validation of query parameters</li>
 *   <li><b>Type Compatibility:</b> Validation of type compatibility between columns and values</li>
 *   <li><b>Resource Management:</b> Automatic cleanup and resource management</li>
 *   <li><b>Detailed Error Messages:</b> Clear, actionable error messages for debugging</li>
 * </ul>
 *
 * <p><b>Best Practices and Recommendations:</b>
 * <ul>
 *   <li>Always finalize builders with {@code sql()}, {@code build()}, or {@code pair()} method calls</li>
 *   <li>Use appropriate naming convention classes (PSC, PAC, PLC, NSC, NAC, NLC) for your environment</li>
 *   <li>Leverage entity mapping annotations for maintainable database-to-object mapping</li>
 *   <li>Use parameterized queries exclusively to prevent SQL injection attacks</li>
 *   <li>Group related conditions logically to improve query readability and performance</li>
 *   <li>Consider database-specific optimizations for high-performance applications</li>
 *   <li>Use batch operations for bulk data modifications</li>
 *   <li>Implement proper error handling and logging for production environments</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Forgetting to finalize builder instances (memory leaks)</li>
 *   <li>Concatenating user input directly into SQL strings (security vulnerabilities)</li>
 *   <li>Creating overly complex nested queries that hurt performance</li>
 *   <li>Ignoring database-specific performance characteristics</li>
 *   <li>Not using appropriate indexes for generated query patterns</li>
 *   <li>Mixing different naming conventions within the same application</li>
 *   <li>Not handling null values properly in conditional logic</li>
 * </ul>
 *
 * <p><b>Thread Safety and Concurrent Usage:</b>
 * <ul>
 *   <li><b>Instance Safety:</b> Individual builder instances are not thread-safe (use per-thread instances)</li>
 *   <li><b>Factory Methods:</b> Static factory methods are thread-safe for concurrent access</li>
 *   <li><b>Immutable Results:</b> Generated SQL strings are immutable and thread-safe</li>
 *   <li><b>Concurrent Usage:</b> Multiple threads can use separate builder instances safely</li>
 * </ul>
 *
 * @see Filters
 * @see Condition
 * @see Expression
 * @see AbstractQueryBuilder
 * @see com.landawn.abacus.annotation.Table
 * @see com.landawn.abacus.annotation.Column
 * @see com.landawn.abacus.annotation.ReadOnly
 * @see com.landawn.abacus.annotation.ReadOnlyId
 * @see com.landawn.abacus.annotation.NonUpdatable
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.sql/java/sql/PreparedStatement.html">PreparedStatement</a>
 * @see <a href="https://en.wikipedia.org/wiki/SQL_injection">SQL Injection Prevention</a>
 */
@SuppressWarnings("deprecation")
public abstract class SQLBuilder extends AbstractQueryBuilder<SQLBuilder> { // NOSONAR

    // TODO performance goal: 80% cases (or maybe SQL.length < 1024?) can be composed in 0.1 millisecond. 0.01 millisecond will be fantastic if possible.

    protected static final Logger logger = LoggerFactory.getLogger(SQLBuilder.class);

    //    public static final String _1 = "1";
    //
    //    public static final List<String> _1_list = ImmutableList.of(_1);

    // private static final Map<Class<?>, ImmutableSet<String>> nonSubEntityPropNamesPool = new ObjectPool<>(N.POOL_SIZE);

    /**
     * Constructs a new SQLBuilder with the specified naming policy and SQL policy.
     * 
     * @param namingPolicy the naming policy for column names, defaults to SNAKE_CASE if null
     * @param sqlPolicy the SQL generation policy, defaults to SQL if null
     */
    protected SQLBuilder(final NamingPolicy namingPolicy, final SQLPolicy sqlPolicy) {
        super(namingPolicy, sqlPolicy);
    }

    /**
     * Generates the final SQL string from this builder.
     * 
     * <p>This method is identical to {@link #sql()} and returns the constructed SQL statement
     * as a String. After calling this method, the builder instance should not be reused as it
     * may be in a closed state.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple SELECT query
     * String sql = PSC.select("id", "name")
     *                 .from("account")
     *                 .where(Filters.gt("age", 18))
     *                 .sql();
     * // Result: SELECT id, name FROM account WHERE age > ?
     * }</pre> 
     *
     * @return the generated SQL string
     * @throws RuntimeException if the builder has already been closed or is in an invalid state
     * @see #sql()
     * @see #build()
     */
    public String sql() {
        return super.query();
    }

    /**
     * Generates the final SQL query string and releases resources.
     * This method should be called only once. After calling this method, the SQLBuilder instance cannot be used again.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example usage:
     * String sql = PSC.select("*")
     *                 .from("users")
     *                 .where(Filters.eq("status", "ACTIVE"))
     *                 .query();
     * // sql contains: "SELECT * FROM users WHERE status = ?"
     * }</pre>
     *
     * @return the generated SQL query string
     * @see #sql()
     * @deprecated replaced by {@code sql()}
     */
    @Override
    public String query() {
        return super.query();
    }

    @Override
    protected void appendCondition(final Condition cond) {
        //    if (sb.charAt(sb.length() - 1) != _SPACE) {
        //        sb.append(_SPACE);
        //    }

        if (cond instanceof final Binary binary) {
            final String propName = binary.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(binary.getOperator().toString());
            _sb.append(_SPACE);

            final Object propValue = binary.getPropValue();
            setParameter(propName, propValue);
        } else if (cond instanceof final Between bt) {
            final String propName = bt.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(bt.getOperator().toString());
            _sb.append(_SPACE);

            final Object minValue = bt.getMinValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("min" + Strings.capitalize(propName), minValue);
            } else {
                setParameter(propName, minValue);
            }

            _sb.append(_SPACE);
            _sb.append(SK.AND);
            _sb.append(_SPACE);

            final Object maxValue = bt.getMaxValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("max" + Strings.capitalize(propName), maxValue);
            } else {
                setParameter(propName, maxValue);
            }
        } else if (cond instanceof final NotBetween nbt) {
            final String propName = nbt.getPropName();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(nbt.getOperator().toString());
            _sb.append(_SPACE);

            final Object minValue = nbt.getMinValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("min" + Strings.capitalize(propName), minValue);
            } else {
                setParameter(propName, minValue);
            }

            _sb.append(_SPACE);
            _sb.append(SK.AND);
            _sb.append(_SPACE);

            final Object maxValue = nbt.getMaxValue();
            if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("max" + Strings.capitalize(propName), maxValue);
            } else {
                setParameter(propName, maxValue);
            }
        } else if (cond instanceof final In in) {
            final String propName = in.getPropName();
            final List<Object> params = in.getParameters();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(in.getOperator().toString());
            _sb.append(SK.SPACE_PARENTHESES_L);

            for (int i = 0, len = params.size(); i < len; i++) {
                if (i > 0) {
                    _sb.append(SK.COMMA_SPACE);
                }

                if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                    setParameter(propName + (i + 1), params.get(i));
                } else {
                    setParameter(propName, params.get(i));
                }
            }

            _sb.append(SK._PARENTHESES_R);
        } else if (cond instanceof final InSubQuery inSubQuery) {
            final String propName = inSubQuery.getPropName();

            if (Strings.isNotEmpty(propName)) {
                appendColumnName(propName);
            } else {
                _sb.append(SK._PARENTHESES_L);

                int idx = 0;

                for (final String e : inSubQuery.getPropNames()) {
                    if (idx++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(e);
                }

                _sb.append(SK._PARENTHESES_R);
            }

            _sb.append(_SPACE);
            _sb.append(inSubQuery.getOperator().toString());

            _sb.append(SK.SPACE_PARENTHESES_L);

            appendCondition(inSubQuery.getSubQuery());

            _sb.append(SK._PARENTHESES_R);
        } else if (cond instanceof final NotIn notIn) {
            final String propName = notIn.getPropName();
            final List<Object> params = notIn.getParameters();

            appendColumnName(propName);

            _sb.append(_SPACE);
            _sb.append(notIn.getOperator().toString());
            _sb.append(SK.SPACE_PARENTHESES_L);

            for (int i = 0, len = params.size(); i < len; i++) {
                if (i > 0) {
                    _sb.append(SK.COMMA_SPACE);
                }

                if (_sqlPolicy == SQLPolicy.NAMED_SQL || _sqlPolicy == SQLPolicy.IBATIS_SQL) {
                    setParameter(propName + (i + 1), params.get(i));
                } else {
                    setParameter(propName, params.get(i));
                }
            }

            _sb.append(SK._PARENTHESES_R);
        } else if (cond instanceof final NotInSubQuery notInSubQuery) {
            final String propName = notInSubQuery.getPropName();

            if (Strings.isNotEmpty(propName)) {
                appendColumnName(propName);
            } else {
                _sb.append(SK._PARENTHESES_L);

                int idx = 0;

                for (final String e : notInSubQuery.getPropNames()) {
                    if (idx++ > 0) {
                        _sb.append(_COMMA_SPACE);
                    }

                    appendColumnName(e);
                }

                _sb.append(SK._PARENTHESES_R);
            }

            _sb.append(_SPACE);
            _sb.append(notInSubQuery.getOperator().toString());
            _sb.append(SK.SPACE_PARENTHESES_L);

            appendCondition(notInSubQuery.getSubQuery());

            _sb.append(SK._PARENTHESES_R);
        } else if (cond instanceof Where || cond instanceof Having) {
            final Cell cell = (Cell) cond;

            _sb.append(_SPACE);
            _sb.append(cell.getOperator().toString());
            _sb.append(_SPACE);

            appendCondition(cell.getCondition());
        } else if (cond instanceof final Cell cell) {
            _sb.append(_SPACE);
            _sb.append(cell.getOperator().toString());
            _sb.append(_SPACE);

            _sb.append(_PARENTHESES_L);
            appendCondition(cell.getCondition());
            _sb.append(_PARENTHESES_R);
        } else if (cond instanceof final Junction junction) {
            final List<Condition> conditionList = junction.getConditions();

            if (N.isEmpty(conditionList)) {
                throw new IllegalArgumentException("Junction condition (" + junction.getOperator() + ") must contain at least one element");
            }

            if (conditionList.size() == 1) {
                appendCondition(conditionList.get(0));
            } else {
                // TODO ((id = :id) AND (gui = :gui)) is not support in Cassandra.
                // only (id = :id) AND (gui = :gui) works.
                // sb.append(_PARENTHESES_L);

                for (int i = 0, size = conditionList.size(); i < size; i++) {
                    if (i > 0) {
                        _sb.append(_SPACE);
                        _sb.append(junction.getOperator().toString());
                        _sb.append(_SPACE);
                    }

                    _sb.append(_PARENTHESES_L);

                    appendCondition(conditionList.get(i));

                    _sb.append(_PARENTHESES_R);
                }

                // sb.append(_PARENTHESES_R);
            }
        } else if (cond instanceof final SubQuery subQuery) {
            final Condition subCond = subQuery.getCondition();

            if (Strings.isNotEmpty(subQuery.sql())) {
                _sb.append(subQuery.sql());
            } else {
                final SQLBuilder subBuilder;

                if (subQuery.getEntityClass() != null) {
                    if (this instanceof SCSB) {
                        subBuilder = SCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof PSC) {
                        subBuilder = PSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof MSC) {
                        subBuilder = MSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof NSC) {
                        subBuilder = NSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof ACSB) {
                        subBuilder = ACSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof PAC) {
                        subBuilder = PAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof MAC) {
                        subBuilder = MAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof NAC) {
                        subBuilder = NAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof LCSB) {
                        subBuilder = LCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof PLC) {
                        subBuilder = PLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof MLC) {
                        subBuilder = MLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof NLC) {
                        subBuilder = NLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof PSB) {
                        subBuilder = PSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof NSB) {
                        subBuilder = NSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else if (this instanceof MSB) {
                        subBuilder = MSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityClass());
                    } else {
                        throw new UnsupportedOperationException("SubQuery condition not supported for this builder type: " + cond);
                    }
                } else if (this instanceof SCSB) {
                    subBuilder = SCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof PSC) {
                    subBuilder = PSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof MSC) {
                    subBuilder = MSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof NSC) {
                    subBuilder = NSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof ACSB) {
                    subBuilder = ACSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof PAC) {
                    subBuilder = PAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof MAC) {
                    subBuilder = MAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof NAC) {
                    subBuilder = NAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof LCSB) {
                    subBuilder = LCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof PLC) {
                    subBuilder = PLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof MLC) {
                    subBuilder = MLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof NLC) {
                    subBuilder = NLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof PSB) {
                    subBuilder = PSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof NSB) {
                    subBuilder = NSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else if (this instanceof MSB) {
                    subBuilder = MSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName());
                } else {
                    throw new UnsupportedOperationException("SubQuery condition not supported for this builder type: " + cond);
                }

                if (subCond != null) {
                    subBuilder.append(subCond);
                }

                final SP subSP = subBuilder.build();

                _sb.append(subSP.query);

                if (N.notEmpty(subSP.parameters)) {
                    _parameters.addAll(subSP.parameters);
                }
            }
        } else if (cond instanceof Expression) {
            // ==== version 1
            // sb.append(cond.toString());

            // ==== version 2
            //    final List<String> words = SQLParser.parse(((Expression) cond).getLiteral());
            //    final Map<String, String> propColumnNameMap = getPropColumnNameMap(entityClass, namingPolicy);
            //
            //    String word = null;
            //
            //    for (int i = 0, size = words.size(); i < size; i++) {
            //        word = words.get(i);
            //
            //        if ((i > 2) && SK.AS.equalsIgnoreCase(words.get(i - 2))) {
            //            sb.append(word);
            //        } else if ((i > 1) && SK.SPACE.equalsIgnoreCase(words.get(i - 1))
            //                && (propColumnNameMap.containsKey(words.get(i - 2)) || propColumnNameMap.containsValue(words.get(i - 2)))) {
            //            sb.append(word);
            //        } else {
            //            sb.append(normalizeColumnName(propColumnNameMap, word));
            //        }
            //    }

            // ==== version 3
            appendStringExpr(((Expression) cond).getLiteral(), false);
        } else {
            throw new IllegalArgumentException("Unsupported condition type: " + cond);
        }
    }

    /**
     * Un-parameterized SQL builder with snake case (lower case with underscore) field/column naming strategy.
     * 
     * <p>This builder generates SQL with actual values embedded directly in the SQL string. Property names
     * are automatically converted from camelCase to snake_case for database column names.</p>
     * 
     * <p>Features:</p>
     * <ul>
     *   <li>Converts "firstName" to "first_name"</li>
     *   <li>Generates non-parameterized SQL (values embedded directly)</li>
     *   <li>Suitable for debugging or read-only queries</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SCSB.select("firstName", "lastName").from("account").where(Filters.eq("id", 1)).sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = 1
     * }</pre>
     *
     * @deprecated {@code PSC or NSC} is preferred for better security and performance. 
     *             Un-parameterized SQL is vulnerable to SQL injection attacks.
     */
    @Deprecated
    public static class SCSB extends SQLBuilder {

        /**
         * Constructs a new SCSB instance with snake_case naming policy and non-parameterized SQL policy.
         * 
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        SCSB() {
            super(NamingPolicy.SNAKE_CASE, SQLPolicy.RAW_SQL);
        }

        /**
         * Creates a new instance of SCSB.
         * 
         * <p>This factory method is used internally by the static methods to create new builder instances.
         * Each SQL building operation starts with a fresh instance to ensure thread safety.</p>
         * 
         * @return a new SCSB instance
         */
        protected static SCSB createInstance() {
            return new SCSB();
        }

        /**
         * Creates an INSERT SQL builder for a single column.
         * 
         * <p>This method initializes an INSERT statement for one column. The column name will be
         * converted according to the snake_case naming policy.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.insert("firstName")
         *                  .into("account")
         *                  .values("John")
         *                  .sql();
         * // Output: INSERT INTO account (first_name) VALUES ('John')
         * }</pre>
         *
         * @param expr the column name or expression
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for multiple columns.
         * 
         * <p>This method initializes an INSERT statement for multiple columns. All column names
         * will be converted according to the snake_case naming policy.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.insert("firstName", "lastName", "email")
         *                  .into("account")
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         *
         * @param propOrColumnNames the column names to insert
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for a collection of columns.
         * 
         * <p>This method is useful when column names are determined dynamically. The collection
         * can contain property names that will be converted to column names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = SCSB.insert(columns)
         *                  .into("account")
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         *
         * @param propOrColumnNames the collection of column names to insert
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with column-value mappings.
         * 
         * <p>This method allows specifying both column names and their values together. The map keys
         * represent column names (which will be converted to snake_case) and values are the data
         * to insert.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> props = N.asMap("firstName", "John", "age", 25);
         * String sql = SCSB.insert(props).into("account").sql();
         * // Output: INSERT INTO account (first_name, age) VALUES ('John', 25)
         * }</pre>
         *
         * @param props map of column names to values
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object.
         * 
         * <p>This method extracts values from the entity's properties and creates an INSERT statement.
         * Properties marked with @Transient, @ReadOnly, or similar annotations are automatically excluded.
         * Property names are converted to snake_case column names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account("John", "john@email.com");
         * String sql = SCSB.insert(account).into("account").sql();
         * // Output: INSERT INTO account (first_name, email) VALUES ('John', 'john@email.com')
         * }</pre>
         *
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object, excluding specified properties.
         * 
         * <p>This method allows selective insertion of entity properties. In addition to properties
         * automatically excluded by annotations, you can specify additional properties to exclude.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account("John", "john@email.com");
         * Set<String> excluded = N.asSet("createdDate");
         * String sql = SCSB.insert(account, excluded).into("account").sql();
         * // Output: INSERT INTO account (first_name, email) VALUES ('John', 'john@email.com')
         * }</pre>
         *
         * @param entity the entity object to insert
         * @param excludedPropNames properties to exclude from the insert
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity class.
         * 
         * <p>This method generates an INSERT template for all insertable properties of the class.
         * Properties marked with @ReadOnly, @ReadOnlyId, or @Transient are automatically excluded.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.insert(Account.class).into("account").sql();
         * // Output: INSERT INTO account (first_name, last_name, email, status)
         * }</pre>
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity class, excluding specified properties.
         * 
         * <p>This method generates an INSERT template excluding both annotation-based exclusions
         * and the specified properties. Useful for creating templates where certain fields
         * are populated by database defaults or triggers.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("id", "createdDate");
         * String sql = SCSB.insert(Account.class, excluded).into("account").sql();
         * // Output: INSERT INTO account (first_name, last_name, email)
         * }</pre>
         *
         * @param entityClass the entity class
         * @param excludedPropNames properties to exclude from the insert
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class.
         * 
         * <p>This convenience method combines insert() and into() operations. The table name
         * is derived from the entity class using the naming policy.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.insertInto(Account.class)
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class, excluding specified properties.
         * 
         * <p>This convenience method combines insert() and into() operations with property exclusion.
         * The table name is derived from the entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("id");
         * String sql = SCSB.insertInto(Account.class, excluded)
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         *
         * @param entityClass the entity class
         * @param excludedPropNames properties to exclude from the insert
         * @return a new SQLBuilder instance for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple entities or property maps.
         * 
         * <p>This method generates MySQL-style batch insert SQL for efficient bulk inserts.
         * All entities or maps in the collection must have the same structure (same properties).</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "john@email.com"),
         *     new Account("Jane", "jane@email.com")
         * );
         * String sql = SCSB.batchInsert(accounts).into("account").sql();
         * // Output: INSERT INTO account (first_name, email) VALUES ('John', 'john@email.com'), ('Jane', 'jane@email.com')
         * }</pre>
         *
         * @param propsList list of entities or property maps to insert
         * @return a new SQLBuilder instance for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * <p>
         * <b>Note:</b> This is a beta feature and may be subject to change
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table.
         * 
         * <p>This method starts building an UPDATE statement for the specified table.
         * The SET clause should be added using the set() method.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.update("account")
         *                  .set("status", "'ACTIVE'")
         *                  .where(Filters.eq("id", 1))
         *                  .sql();
         * // Output: UPDATE account SET status = 'ACTIVE' WHERE id = 1
         * }</pre>
         *
         * @param tableName the table name to update
         * @return a new SQLBuilder instance for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with entity class context.
         * 
         * <p>This method provides entity class information for property-to-column name mapping
         * when building the UPDATE statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.update("account", Account.class)
         *                  .set("firstName", "'Jane'")
         *                  .where(Filters.eq("id", 1))
         *                  .sql();
         * // Output: UPDATE account SET first_name = 'Jane' WHERE id = 1
         * }</pre>
         *
         * @param tableName the table name to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class and includes all
         * updatable properties. Properties marked with @NonUpdatable or @ReadOnly are excluded.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.update(Account.class)
         *                  .set("status", "'INACTIVE'")
         *                  .where(Filters.lt("lastLogin", "2023-01-01"))
         *                  .sql();
         * // Output: UPDATE account SET status = 'INACTIVE' WHERE last_login < '2023-01-01'
         * }</pre>
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class, excluding specified properties.
         * 
         * <p>This method allows additional property exclusions beyond those marked with
         * annotations. Useful for partial updates or when certain fields should not be modified.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("email", "createdDate");
         * String sql = SCSB.update(Account.class, excluded)
         *                  .set("status", "'ACTIVE'")
         *                  .where(Filters.eq("id", 1))
         *                  .sql();
         * // Output: UPDATE account SET status = 'ACTIVE' WHERE id = 1
         * }</pre>
         *
         * @param entityClass the entity class
         * @param excludedPropNames additional properties to exclude from updates
         * @return a new SQLBuilder instance for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table.
         * 
         * <p>This method starts building a DELETE statement for the specified table.
         * A WHERE clause should typically be added to avoid deleting all rows.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.deleteFrom("account")
         *                  .where(Filters.eq("status", "'DELETED'"))
         *                  .sql();
         * // Output: DELETE FROM account WHERE status = 'DELETED'
         * }</pre>
         *
         * @param tableName the table name to delete from
         * @return a new SQLBuilder instance for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table with entity class context.
         * 
         * <p>This method provides entity class information for property-to-column name mapping
         * in the WHERE clause of the DELETE statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.deleteFrom("account", Account.class)
         *                  .where(Filters.eq("status", "'INACTIVE'"))
         *                  .sql();
         * // Output: DELETE FROM account WHERE status = 'INACTIVE'
         * }</pre>
         *
         * @param tableName the table name to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class using the naming policy.
         * Always add a WHERE clause to avoid accidentally deleting all rows.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.deleteFrom(Account.class)
         *                  .where(Filters.and(
         *                      Filters.eq("status", "'INACTIVE'"),
         *                      Filters.lt("lastLogin", "2022-01-01")
         *                  ))
         *                  .sql();
         * // Output: DELETE FROM account WHERE status = 'INACTIVE' AND last_login < '2022-01-01'
         * }</pre>
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a custom select expression.
         * 
         * <p>This method allows complex SELECT expressions including aggregate functions,
         * calculated columns, or any valid SQL select expression.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.select("COUNT(DISTINCT customer_id)")
         *                  .from("orders")
         *                  .where(Filters.between("order_date", "2023-01-01", "2023-12-31"))
         *                  .sql();
         * // Output: SELECT COUNT(DISTINCT customer_id) FROM orders WHERE order_date BETWEEN '2023-01-01' AND '2023-12-31'
         * }</pre>
         *
         * @param selectPart the select expression (e.g., "COUNT(*)", "DISTINCT name")
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for multiple columns.
         * 
         * <p>This method builds a SELECT statement with the specified columns. Property names
         * are converted to column names using the snake_case naming policy.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.select("firstName", "lastName", "email")
         *                  .from("account")
         *                  .where(Filters.eq("status", "'ACTIVE'"))
         *                  .sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = 'ACTIVE'
         * }</pre>
         *
         * @param propOrColumnNames the column names to select
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for a collection of columns.
         * 
         * <p>This method is useful when column names are determined dynamically at runtime.
         * The collection can contain property names that will be converted to column names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = SCSB.select(columns)
         *                  .from("account")
         *                  .where(Filters.eq("status", "'ACTIVE'"))
         *                  .sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = 'ACTIVE'
         * }</pre>
         *
         * @param propOrColumnNames the collection of column names to select
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * 
         * <p>This method allows specifying custom aliases for each selected column. The map keys
         * are column names (converted to snake_case) and values are the aliases to use.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = N.asMap(
         *     "firstName", "fname",
         *     "lastName", "lname"
         * );
         * String sql = SCSB.select(aliases).from("account").sql();
         * // Output: SELECT first_name AS fname, last_name AS lname FROM account
         * }</pre>
         *
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class.
         * 
         * <p>This method generates a SELECT statement including all properties of the entity class,
         * excluding those marked with @Transient annotation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.select(Account.class)
         *                  .from("account")
         *                  .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account
         * }</pre>
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with optional sub-entity properties.
         * 
         * <p>When includeSubEntityProperties is true, properties from related entities (marked with
         * appropriate annotations) will also be included in the SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.select(Order.class, true)
         *                  .from("orders")
         *                  .sql();
         * // Output includes both Order properties and related Customer properties
         * }</pre>
         *
         * @param entityClass the entity class
         * @param includeSubEntityProperties whether to include properties from related entities
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class, excluding specified properties.
         * 
         * <p>This method allows selective property selection by excluding certain properties
         * from the SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "salt");
         * String sql = SCSB.select(Account.class, excluded)
         *                  .from("account")
         *                  .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account
         * }</pre>
         *
         * @param entityClass the entity class
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with full control over property inclusion.
         * 
         * <p>This method provides complete control over which properties to include in the SELECT
         * statement, with options for sub-entity properties and property exclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("internalData");
         * String sql = SCSB.select(Order.class, true, excluded)
         *                  .from("orders")
         *                  .sql();
         * // Output includes Order and sub-entity properties, excluding internalData
         * }</pre>
         *
         * @param entityClass the entity class
         * @param includeSubEntityProperties whether to include properties from related entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class.
         * 
         * <p>This convenience method combines select() and from() operations. The table name
         * is automatically derived from the entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.selectFrom(Account.class)
         *                  .where(Filters.eq("status", "'ACTIVE'"))
         *                  .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = 'ACTIVE'
         * }</pre>
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with table alias.
         * 
         * <p>This method allows specifying a table alias for use in complex queries with joins
         * or subqueries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.selectFrom(Account.class, "a")
         *                  .innerJoin("orders", "o").on("a.id = o.account_id")
         *                  .sql();
         * // Output: SELECT a.id, a.first_name AS "firstName" ... FROM account a INNER JOIN orders o ON a.id = o.account_id
         * }</pre>
         *
         * @param entityClass the entity class
         * @param alias the table alias
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity inclusion option.
         * 
         * <p>This convenience method combines select() and from() operations with the option
         * to include properties from related entities.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.selectFrom(Order.class, true)
         *                  .where(Filters.gt("totalAmount", 100))
         *                  .sql();
         * // Output includes automatic joins for sub-entities
         * }</pre>
         *
         * @param entityClass the entity class
         * @param includeSubEntityProperties whether to include properties from related entities
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and sub-entity inclusion option.
         * 
         * <p>This method combines table aliasing with sub-entity property inclusion for
         * complex query construction.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.selectFrom(Order.class, "o", true)
         *                  .where(Filters.eq("o.status", "'COMPLETED'"))
         *                  .sql();
         * // Output includes aliased columns and joins for sub-entities
         * }</pre>
         *
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include properties from related entities
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder excluding specified properties.
         * 
         * <p>This convenience method combines select() and from() operations while excluding
         * certain properties from the selection.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("largeBlob", "metadata");
         * String sql = SCSB.selectFrom(Account.class, excluded)
         *                  .where(Filters.eq("active", true))
         *                  .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account WHERE active = true
         * }</pre>
         *
         * @param entityClass the entity class
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias, excluding specified properties.
         * 
         * <p>This method provides aliasing capability while excluding specified properties
         * from the SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("internalCode");
         * String sql = SCSB.selectFrom(Account.class, "a", excluded)
         *                  .innerJoin("orders", "o").on("a.id = o.account_id")
         *                  .sql();
         * // Output uses alias "a" and excludes internalCode property
         * }</pre>
         *
         * @param entityClass the entity class
         * @param alias the table alias
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity inclusion and property exclusion.
         * 
         * <p>This method provides a convenient way to create a complete SELECT FROM statement
         * with control over sub-entities and property exclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("deletedFlag");
         * String sql = SCSB.selectFrom(Order.class, true, excluded)
         *                  .where(Filters.gt("createdDate", "2023-01-01"))
         *                  .sql();
         * // Output includes Order with Customer sub-entity, excluding deletedFlag
         * }</pre>
         *
         * @param entityClass the entity class
         * @param includeSubEntityProperties whether to include properties from related entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with full control over all options.
         * 
         * <p>This method provides complete control over the SELECT FROM statement generation,
         * including table alias, sub-entity properties, and property exclusion. When sub-entities
         * are included, appropriate joins will be generated automatically.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("debugInfo");
         * String sql = SCSB.selectFrom(Order.class, "ord", true, excluded)
         *                  .where(Filters.gt("ord.totalAmount", 1000))
         *                  .sql();
         * // Output: Complex SELECT with alias, sub-entities, and exclusions
         * }</pre>
         *
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include properties from related entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.SNAKE_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes.
         * 
         * <p>This method sets up a query to select from two tables with proper aliasing and
         * property prefixing. Each entity's properties will be prefixed with their class alias
         * in the result set.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.select(Account.class, "a", "account",
         *                         Order.class, "o", "order")
         *                  .from("account a")
         *                  .innerJoin("orders o").on("a.id = o.account_id")
         *                  .sql();
         * // Output: SELECT a.first_name AS "account.firstName", o.total AS "order.total" ...
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA property prefix for the first entity in results
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB property prefix for the second entity in results
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes with property exclusions.
         * 
         * <p>This method extends the two-entity join capability by allowing exclusion of specific
         * properties from each entity. Useful for avoiding large fields or sensitive data in joins.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> accountExcluded = N.asSet("password");
         * Set<String> orderExcluded = N.asSet("internalNotes");
         * String sql = SCSB.select(Account.class, "a", "account", accountExcluded,
         *                         Order.class, "o", "order", orderExcluded)
         *                  .from("account a")
         *                  .innerJoin("orders o").on("a.id = o.account_id")
         *                  .sql();
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA property prefix for the first entity in results
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB property prefix for the second entity in results
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * 
         * <p>This is the most flexible method for complex multi-table queries. Each Selection
         * object encapsulates the configuration for one entity including alias, property prefix,
         * and exclusions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, null)
         * );
         * String sql = SCSB.select(selections)
         *                  .from("account a")
         *                  .innerJoin("orders o").on("a.id = o.account_id")
         *                  .innerJoin("products p").on("o.product_id = p.id")
         *                  .sql();
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if multiSelects is null, empty, or contains invalid data
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for joining two entity classes.
         * 
         * <p>This convenience method combines select() and from() operations for two-entity joins.
         * The FROM clause is automatically generated based on the entity classes.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.selectFrom(Account.class, "a", "account",
         *                             Order.class, "o", "order")
         *                  .where(Filters.eq("a.status", "'ACTIVE'"))
         *                  .sql();
         * // Output: SELECT ... FROM account a, orders o WHERE a.status = 'ACTIVE'
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA property prefix for the first entity in results
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB property prefix for the second entity in results
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for joining two entity classes with property exclusions.
         * 
         * <p>This method combines multi-entity selection with automatic FROM clause generation
         * and property filtering capabilities.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> accountExcluded = N.asSet("passwordHash");
         * Set<String> orderExcluded = N.asSet("debugData");
         * String sql = SCSB.selectFrom(Account.class, "a", "account", accountExcluded,
         *                             Order.class, "o", "order", orderExcluded)
         *                  .where(Filters.eq("o.status", "'COMPLETED'"))
         *                  .sql();
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA property prefix for the first entity in results
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB property prefix for the second entity in results
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * 
         * <p>This method automatically generates the appropriate FROM clause based on the
         * provided Selection configurations. It's the most convenient way to build complex
         * multi-table queries with automatic FROM clause generation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, null),
         *     new Selection(Order.class, "o", "order", null, true, null)
         * );
         * String sql = SCSB.selectFrom(selections)
         *                  .where(Filters.eq("a.verified", true))
         *                  .sql();
         * // Output: SELECT ... FROM account a, orders o WHERE a.verified = true
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance for SELECT operation
         * @throws IllegalArgumentException if multiSelects is null, empty, or contains invalid data
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.SNAKE_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for a table.
         * 
         * <p>This is a convenience method for creating COUNT queries to get the total number
         * of rows in a table.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.count("account")
         *                  .where(Filters.eq("status", "'ACTIVE'"))
         *                  .sql();
         * // Output: SELECT count(*) FROM account WHERE status = 'ACTIVE'
         * }</pre>
         *
         * @param tableName the table name to count rows from
         * @return a new SQLBuilder instance for COUNT operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class and creates a COUNT query.
         * Useful for getting row counts with type-safe table name resolution.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = SCSB.count(Account.class)
         *                  .where(Filters.between("createdDate", "2023-01-01", "2023-12-31"))
         *                  .sql();
         * // Output: SELECT count(*) FROM account WHERE created_date BETWEEN '2023-01-01' AND '2023-12-31'
         * }</pre>
         *
         * @param entityClass the entity class
         * @return a new SQLBuilder instance for COUNT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context.
         * 
         * <p>This method is used to generate SQL fragments for conditions only, without
         * building a complete SQL statement. It's useful for debugging conditions or
         * building dynamic query parts that will be combined later.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("status", "'ACTIVE'"),
         *     Filters.gt("balance", 1000)
         * );
         * String sql = SCSB.parse(cond, Account.class).sql();
         * // Output: status = 'ACTIVE' AND balance > 1000
         * }</pre>
         *
         * @param cond the condition to parse
         * @param entityClass the entity class for property mapping (can be null)
         * @return a new SQLBuilder instance containing the condition SQL
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Un-parameterized SQL builder with all capital case (upper case with underscore) field/column naming strategy.
     * 
     * <p>This builder generates SQL with actual values embedded directly in the SQL string. Property names
     * are automatically converted from camelCase to SCREAMING_SNAKE_CASE for database column names.</p>
     * 
     * <p>Features:</p>
     * <ul>
     *   <li>Converts "firstName" to "FIRST_NAME"</li>
     *   <li>Generates non-parameterized SQL (values embedded directly)</li>
     *   <li>Suitable for databases that use uppercase column names</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ACSB.select("firstName", "lastName").from("account").where(Filters.eq("id", 1)).sql();
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = 1
     * }</pre>
     *
     * @deprecated {@code PAC or NAC} is preferred for better security and performance. 
     *             Un-parameterized SQL is vulnerable to SQL injection attacks.
     */
    @Deprecated
    public static class ACSB extends SQLBuilder {

        /**
         * Constructs a new ACSB instance with SCREAMING_SNAKE_CASE naming policy and non-parameterized SQL policy.
         * 
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        ACSB() {
            super(NamingPolicy.SCREAMING_SNAKE_CASE, SQLPolicy.RAW_SQL);
        }

        /**
         * Creates a new instance of ACSB.
         * 
         * <p>This factory method is used internally by the static methods to create new builder instances.
         * Each SQL building operation starts with a fresh instance to ensure thread safety.</p>
         * 
         * @return a new ACSB instance
         */
        protected static ACSB createInstance() {
            return new ACSB();
        }

        /**
         * Creates an INSERT SQL builder for a single column.
         * 
         * <p>This method initializes an INSERT statement for one column. The column name will be
         * converted to SCREAMING_SNAKE_CASE according to the naming policy.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.insert("firstName")
         *                  .into("users")
         *                  .values("John")
         *                  .sql();
         * // Output: INSERT INTO USERS (FIRST_NAME) VALUES ('John')
         * }</pre>
         *
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for multiple columns.
         * 
         * <p>This method initializes an INSERT statement for multiple columns. All column names
         * will be converted to SCREAMING_SNAKE_CASE according to the naming policy.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.insert("firstName", "lastName", "email")
         *                  .into("users")
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to insert, in order
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for a collection of columns.
         * 
         * <p>This method is useful when column names are dynamically determined. The collection
         * can contain property names that will be converted to uppercase column names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = ACSB.insert(columns)
         *                  .into("users")
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         *
         * @param propOrColumnNames the collection of column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with column-value mappings.
         * 
         * <p>This method allows direct specification of column names and their corresponding
         * values as a Map. Column names will be converted to uppercase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("firstName", "John");
         * data.put("age", 30);
         * String sql = ACSB.insert(data).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, AGE) VALUES ('John', 30)
         * }</pre>
         *
         * @param props map of column names to their corresponding values
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object.
         * 
         * <p>This method extracts property values from the given entity object and creates
         * an INSERT statement. Properties marked with @Transient, @ReadOnly, or similar 
         * annotations are automatically excluded. Property names are converted to uppercase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User("John", 30, "john@example.com");
         * String sql = ACSB.insert(user).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, AGE, EMAIL) VALUES ('John', 30, 'john@example.com')
         * }</pre>
         *
         * @param entity the entity object containing data to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object, excluding specified properties.
         * 
         * <p>This method allows selective insertion of entity properties. In addition to properties
         * automatically excluded by annotations, you can specify additional properties to exclude.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User("John", 30, "john@example.com");
         * Set<String> excluded = new HashSet<>(Arrays.asList("createdDate", "modifiedDate"));
         * String sql = ACSB.insert(user, excluded).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, AGE, EMAIL) VALUES ('John', 30, 'john@example.com')
         * }</pre>
         *
         * @param entity the entity object containing data to insert
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity class.
         * 
         * <p>This method generates an INSERT template for the specified entity class,
         * including all insertable properties. Properties marked with @ReadOnly, @ReadOnlyId,
         * or @Transient are automatically excluded.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.insert(User.class).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, AGE, EMAIL)
         * }</pre>
         *
         * @param entityClass the entity class to use as template
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity class, excluding specified properties.
         * 
         * <p>This method generates an INSERT template excluding both annotation-based exclusions
         * and the specified properties. Useful for creating templates where certain fields
         * are populated by database defaults or triggers.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
         * String sql = ACSB.insert(User.class, excluded).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, AGE, EMAIL)
         * }</pre>
         *
         * @param entityClass the entity class to use as template
         * @param excludedPropNames properties to exclude from the insert template
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class.
         * 
         * <p>This is a convenience method that combines insert() and into() operations.
         * The table name is derived from the entity class name and converted to uppercase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.insertInto(User.class)
         *                  .values("John", "Doe", 30, "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO USER (FIRST_NAME, LAST_NAME, AGE, EMAIL) VALUES ('John', 'Doe', 30, 'john@example.com')
         * }</pre>
         *
         * @param entityClass the entity class to insert into
         * @return a new SQLBuilder instance configured for INSERT INTO operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class, excluding specified properties.
         * 
         * <p>This convenience method combines insert() and into() operations with property exclusion.
         * The table name is derived from the entity class and converted to uppercase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("id"));
         * String sql = ACSB.insertInto(User.class, excluded)
         *                  .values("John", "Doe", 30, "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO USER (FIRST_NAME, LAST_NAME, AGE, EMAIL) VALUES ('John', 'Doe', 30, 'john@example.com')
         * }</pre>
         *
         * @param entityClass the entity class to insert into
         * @param excludedPropNames properties to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT INTO operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple entities or property maps.
         * 
         * <p>This method generates MySQL-style batch insert SQL for efficient bulk inserts.
         * All entities or maps in the collection must have the same structure (same properties).</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", 30, "john@example.com"),
         *     new User("Jane", 25, "jane@example.com")
         * );
         * String sql = ACSB.batchInsert(users).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, AGE, EMAIL) VALUES 
         * //         ('John', 30, 'john@example.com'), 
         * //         ('Jane', 25, 'jane@example.com')
         * }</pre>
         *
         * @param propsList list of entities or property maps to insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * <p>
         * <b>Note:</b> This is a beta feature and may be subject to change
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * <p>This method initializes a new SQLBuilder for UPDATE operations on the
         * specified table. The columns to update should be specified using the
         * set() method.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.update("users")
         *                  .set("LAST_NAME", "'Smith'")
         *                  .where(Filters.eq("ID", 123))
         *                  .sql();
         * // Output: UPDATE USERS SET LAST_NAME = 'Smith' WHERE ID = 123
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with entity class context.
         * 
         * <p>This method provides entity class information for property-to-column name mapping
         * when building the UPDATE statement. Property names will be converted to uppercase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.update("users", User.class)
         *                  .set("age", 31)  // "age" is mapped to "AGE" column
         *                  .where(Filters.eq("firstName", "'John'"))
         *                  .sql();
         * // Output: UPDATE USERS SET AGE = 31 WHERE FIRST_NAME = 'John'
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class and includes all
         * updatable properties. Properties marked with @NonUpdatable or @ReadOnly are excluded.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.update(User.class)
         *                  .set("age", 31)
         *                  .where(Filters.eq("firstName", "'John'"))
         *                  .sql();
         * // Output: UPDATE USER SET AGE = 31 WHERE FIRST_NAME = 'John'
         * }</pre>
         *
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class, excluding specified properties.
         * 
         * <p>This method allows additional property exclusions beyond those marked with
         * annotations. Useful for partial updates or when certain fields should not be modified.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
         * String sql = ACSB.update(User.class, excluded)
         *                  .set("age", 31)
         *                  .where(Filters.eq("id", 1))
         *                  .sql();
         * // Output: UPDATE USER SET AGE = 31 WHERE ID = 1
         * }</pre>
         *
         * @param entityClass the entity class to update
         * @param excludedPropNames additional properties to exclude from updates
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table.
         * 
         * <p>This method initializes a new SQLBuilder for DELETE operations on the
         * specified table. A WHERE clause should typically be added to avoid deleting all rows.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.deleteFrom("users")
         *                  .where(Filters.lt("AGE", 18))
         *                  .sql();
         * // Output: DELETE FROM USERS WHERE AGE < 18
         * }</pre>
         *
         * @param tableName the table name to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table with entity class context.
         * 
         * <p>This method provides entity class information for property-to-column name mapping
         * in the WHERE clause of the DELETE statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.deleteFrom("users", User.class)
         *                  .where(Filters.eq("age", 18))  // "age" is mapped to "AGE" column
         *                  .sql();
         * // Output: DELETE FROM USERS WHERE AGE = 18
         * }</pre>
         *
         * @param tableName the table name to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class name and converts it
         * to uppercase according to the naming policy.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.deleteFrom(User.class)
         *                  .where(Filters.eq("ID", 1))
         *                  .sql();
         * // Output: DELETE FROM USER WHERE ID = 1
         * }</pre>
         *
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a custom select expression.
         * 
         * <p>This method allows complex SELECT expressions including aggregate functions,
         * calculated columns, or any valid SQL select expression.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.select("COUNT(*) as total, AVG(SALARY) as avgSalary")
         *                  .from("EMPLOYEES")
         *                  .sql();
         * // Output: SELECT COUNT(*) as total, AVG(SALARY) as avgSalary FROM EMPLOYEES
         * }</pre>
         *
         * @param selectPart the SELECT expression or clause
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified columns.
         * 
         * <p>This method builds a SELECT statement with the specified columns. Property names
         * are converted to uppercase column names using the SCREAMING_SNAKE_CASE naming policy.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.select("firstName", "lastName", "age")
         *                  .from("users")
         *                  .where(Filters.gte("age", 18))
         *                  .sql();
         * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName", AGE AS "age" 
         * //         FROM USERS WHERE AGE >= 18
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for a collection of columns.
         * 
         * <p>This method is useful when column names are determined dynamically at runtime.
         * The collection can contain property names that will be converted to uppercase column names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = getRequiredColumns();   // returns ["firstName", "email"]
         * String sql = ACSB.select(columns)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT FIRST_NAME AS "firstName", EMAIL AS "email" FROM USERS
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * 
         * <p>This method allows specifying custom aliases for each selected column. The map keys
         * are column names (converted to uppercase) and values are the aliases to use.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = ACSB.select(aliases).from("users").sql();
         * // Output: SELECT FIRST_NAME AS "fname", LAST_NAME AS "lname" FROM USERS
         * }</pre>
         *
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class.
         * 
         * <p>This method generates a SELECT statement including all properties of the entity class,
         * excluding those marked with @Transient annotation. Column names are converted to uppercase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.select(User.class)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", LAST_NAME AS "lastName", AGE AS "age", EMAIL AS "email" FROM USERS
         * }</pre>
         *
         * @param entityClass the entity class whose properties to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with sub-entity option.
         * 
         * <p>When includeSubEntityProperties is true, properties of nested entities are included.
         * This is useful for fetching related data in a single query.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.select(Order.class, true)  // includes Customer sub-entity
         *                  .from("orders")
         *                  .sql();
         * // Output includes both Order and nested Customer properties with uppercase column names
         * }</pre>
         *
         * @param entityClass the entity class whose properties to select
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class, excluding specified properties.
         * 
         * <p>This method allows selective property selection by excluding certain properties
         * from the SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("password", "secretKey"));
         * String sql = ACSB.select(User.class, excluded)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", LAST_NAME AS "lastName", AGE AS "age", EMAIL AS "email" FROM USERS
         * }</pre>
         *
         * @param entityClass the entity class whose properties to select
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with full control over property selection.
         * 
         * <p>This method combines sub-entity inclusion and property exclusion options, providing
         * complete control over which properties appear in the SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = ACSB.select(Order.class, true, excluded)
         *                  .from("orders")
         *                  .sql();
         * // Output includes Order and Customer properties, excluding internalNotes
         * }</pre>
         *
         * @param entityClass the entity class whose properties to select
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM SQL builder for an entity class.
         * 
         * <p>This is a convenience method that combines select() and from() operations.
         * The table name is derived from the entity class and converted to uppercase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.selectFrom(User.class)
         *                  .where(Filters.gte("age", 18))
         *                  .sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", LAST_NAME AS "lastName", AGE AS "age", EMAIL AS "email" 
         * //         FROM USER WHERE AGE >= 18
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias for an entity class.
         * 
         * <p>The alias is used to qualify column names in complex queries with joins or subqueries.
         * The table name is derived from the entity class and converted to uppercase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.selectFrom(User.class, "u")
         *                  .where(Filters.gte("u.AGE", 18))
         *                  .sql();
         * // Output: SELECT u.ID AS "id", u.FIRST_NAME AS "firstName", u.LAST_NAME AS "lastName", u.AGE AS "age", u.EMAIL AS "email" 
         * //         FROM USER u WHERE u.AGE >= 18
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity inclusion option.
         * 
         * <p>When includeSubEntityProperties is true, joins are added for sub-entities.
         * This provides a convenient way to fetch related data in a single query.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.selectFrom(Order.class, true)
         *                  .where(Filters.eq("STATUS", "'ACTIVE'"))
         *                  .sql();
         * // Output includes JOINs for sub-entities like Customer
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include and join sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and sub-entity options.
         * 
         * <p>This method combines table aliasing with sub-entity inclusion for building
         * complex queries with related data.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.selectFrom(Order.class, "o", true)
         *                  .where(Filters.eq("o.STATUS", "'ACTIVE'"))
         *                  .sql();
         * // Output includes aliased columns and JOINs for sub-entities
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include and join sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with property exclusion.
         * 
         * <p>This method allows selective property selection with automatic FROM clause generation.
         * Properties in the excluded set will not appear in the SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("largeBlob", "metadata"));
         * String sql = ACSB.selectFrom(User.class, excluded)
         *                  .where(Filters.eq("ACTIVE", true))
         *                  .sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", LAST_NAME AS "lastName", AGE AS "age", EMAIL AS "email" 
         * //         FROM USER WHERE ACTIVE = true
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and property exclusion.
         * 
         * <p>This method combines aliasing with selective property selection for flexible
         * query construction.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("internalCode"));
         * String sql = ACSB.selectFrom(User.class, "u", excluded)
         *                  .innerJoin("ORDERS", "o").on("u.ID = o.USER_ID")
         *                  .sql();
         * // Output uses alias "u" and excludes internalCode property
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity and exclusion options.
         * 
         * <p>This method provides full control over entity selection including sub-entities
         * while allowing certain properties to be excluded.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("deletedFlag"));
         * String sql = ACSB.selectFrom(Order.class, true, excluded)
         *                  .where(Filters.gt("CREATED_DATE", "'2023-01-01'"))
         *                  .sql();
         * // Output includes Order with Customer sub-entity, excluding deletedFlag
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include and join sub-entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with full control over all options.
         * 
         * <p>This is the most comprehensive selectFrom method providing complete control over
         * aliasing, sub-entity inclusion, and property exclusion. When sub-entities are included,
         * appropriate joins are generated automatically.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("debugInfo"));
         * String sql = ACSB.selectFrom(Order.class, "ord", true, excluded)
         *                  .where(Filters.gt("ord.TOTAL_AMOUNT", 1000))
         *                  .sql();
         * // Output: Complex SELECT with alias, sub-entities, and exclusions
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include and join sub-entities
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.SCREAMING_SNAKE_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes.
         * 
         * <p>This method sets up a query to select from two tables with aliasing.
         * Each entity's properties will be prefixed with their class alias in the result set.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.select(User.class, "u", "user", Order.class, "o", "order")
         *                  .from("USERS", "u")
         *                  .innerJoin("ORDERS", "o").on("u.ID = o.USER_ID")
         *                  .sql();
         * // Output: SELECT with columns from both entities properly aliased
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA class alias prefix for first entity columns
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB class alias prefix for second entity columns
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes with exclusions.
         * 
         * <p>This method allows excluding specific properties from each entity in the join.
         * Useful for optimizing queries by excluding unnecessary or large fields.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclusions = new HashSet<>(Arrays.asList("password"));
         * Set<String> orderExclusions = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = ACSB.select(User.class, "u", "user", userExclusions,
         *                         Order.class, "o", "order", orderExclusions)
         *                  .from("USERS", "u")
         *                  .innerJoin("ORDERS", "o").on("u.ID = o.USER_ID")
         *                  .sql();
         * // Output: SELECT with filtered columns from both entities
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA class alias prefix for first entity columns
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB class alias prefix for second entity columns
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * 
         * <p>This is the most flexible method for complex multi-table queries.
         * Each Selection object encapsulates the configuration for one entity.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, null)
         * );
         * String sql = ACSB.select(selections)
         *                  .from("USERS", "u")
         *                  .innerJoin("ORDERS", "o").on("u.ID = o.USER_ID")
         *                  .innerJoin("PRODUCTS", "p").on("o.PRODUCT_ID = p.ID")
         *                  .sql();
         * // Output: Complex SELECT with columns from all three entities
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if multiSelects is invalid
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM SQL builder for joining two entities.
         * 
         * <p>This convenience method includes both SELECT and FROM clauses.
         * The FROM clause is automatically generated based on the entity classes.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.selectFrom(User.class, "u", "user", Order.class, "o", "order")
         *                  .where(Filters.eq("u.ACTIVE", true))
         *                  .sql();
         * // Output: SELECT ... FROM USER u, ORDER o WHERE u.ACTIVE = true
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA class alias prefix for first entity columns
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB class alias prefix for second entity columns
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for joining two entities with exclusions.
         * 
         * <p>This method combines multi-entity selection with property filtering and
         * automatic FROM clause generation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExcl = new HashSet<>(Arrays.asList("passwordHash"));
         * Set<String> orderExcl = new HashSet<>(Arrays.asList("debugData"));
         * String sql = ACSB.selectFrom(User.class, "u", "user", userExcl,
         *                             Order.class, "o", "order", orderExcl)
         *                  .where(Filters.eq("o.STATUS", "'COMPLETED'"))
         *                  .sql();
         * // Output: SELECT ... FROM USER u, ORDER o WHERE o.STATUS = 'COMPLETED'
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA class alias prefix for first entity columns
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB class alias prefix for second entity columns
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * 
         * <p>This method automatically generates the FROM clause based on the selections.
         * It's the most convenient way to build complex multi-table queries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, true, null)
         * );
         * String sql = ACSB.selectFrom(selections)
         *                  .where(Filters.eq("u.VERIFIED", true))
         *                  .sql();
         * // Output: SELECT ... FROM USER u, ORDER o WHERE u.VERIFIED = true
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if multiSelects is invalid
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.SCREAMING_SNAKE_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for a table.
         * 
         * <p>This is a convenience method for counting rows. The table name is
         * automatically converted to uppercase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.count("users")
         *                  .where(Filters.eq("ACTIVE", true))
         *                  .sql();
         * // Output: SELECT count(*) FROM USERS WHERE ACTIVE = true
         * }</pre>
         *
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for an entity class.
         * 
         * <p>The table name is derived from the entity class and converted to uppercase.
         * This provides type-safe row counting.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = ACSB.count(User.class)
         *                  .where(Filters.gte("AGE", 18))
         *                  .sql();
         * // Output: SELECT count(*) FROM USER WHERE AGE >= 18
         * }</pre>
         *
         * @param entityClass the entity class to count
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a Condition object into SQL with entity class mapping.
         * 
         * <p>This method is used to generate SQL fragments from Condition objects.
         * Property names in the condition are converted to uppercase column names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(Filters.eq("firstName", "'John'"), Filters.gt("age", 18));
         * String sql = ACSB.parse(cond, User.class).sql();
         * // Output: FIRST_NAME = 'John' AND AGE > 18
         * }</pre>
         *
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for property name mapping (can be null)
         * @return a new SQLBuilder instance containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * SQL builder implementation with lower camel case naming policy.
     * 
     * <p>This builder generates SQL with actual values embedded directly in the SQL string.
     * Property names are preserved in camelCase format without conversion. This is useful
     * for databases that use camelCase column naming conventions.</p>
     * 
     * <p>Features:</p>
     * <ul>
     *   <li>Preserves "firstName" as "firstName" (no conversion)</li>
     *   <li>Generates non-parameterized SQL (values embedded directly)</li>
     *   <li>Suitable for databases with camelCase column names</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LCSB.select("firstName", "lastName").from("userAccount").where(Filters.eq("userId", 1)).sql();
     * // Output: SELECT firstName, lastName FROM userAccount WHERE userId = 1
     * }</pre>
     * 
     * @deprecated Use other naming policy implementations like {@link PSC}, {@link PAC}, {@link NSC} instead.
     *             Un-parameterized SQL is vulnerable to SQL injection attacks.
     */
    @Deprecated
    public static class LCSB extends SQLBuilder {

        /**
         * Constructs a new LCSB instance with CAMEL_CASE naming policy and non-parameterized SQL policy.
         * 
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        LCSB() {
            super(NamingPolicy.CAMEL_CASE, SQLPolicy.RAW_SQL);
        }

        /**
         * Creates a new instance of LCSB.
         * 
         * <p>This factory method is used internally by the static methods to create new builder instances.
         * Each SQL building operation starts with a fresh instance to ensure thread safety.</p>
         * 
         * @return a new LCSB instance
         */
        protected static LCSB createInstance() {
            return new LCSB();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * 
         * <p>This method is a convenience wrapper that delegates to {@link #insert(String...)} 
         * with a single element array. Column names remain in camelCase format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.insert("userName")
         *                  .into("users")
         *                  .values("John")
         *                  .sql();
         * // Output: INSERT INTO users (userName) VALUES ('John')
         * }</pre>
         * 
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         * 
         * @see #insert(String...)
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified columns.
         * 
         * <p>This method initializes a new SQLBuilder for INSERT operations with the specified
         * column names. The actual values should be provided later using the VALUES clause.
         * Column names remain in camelCase format without conversion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.insert("firstName", "lastName", "email")
         *                  .into("users")
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified columns collection.
         * 
         * <p>This method is similar to {@link #insert(String...)} but accepts a Collection
         * of column names instead of varargs. Useful when column names are dynamically determined.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = LCSB.insert(columns)
         *                  .into("users")
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with property name-value pairs.
         * 
         * <p>This method allows direct specification of column names and their corresponding
         * values as a Map. Column names remain in camelCase format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("age", 30);
         * String sql = LCSB.insert(props).into("users").sql();
         * // Output: INSERT INTO users (firstName, age) VALUES ('John', 30)
         * }</pre>
         * 
         * @param props map of property names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object.
         * 
         * <p>This method extracts property values from the given entity object and creates
         * an INSERT statement. Properties marked with @Transient, @ReadOnly, or similar 
         * annotations are automatically excluded. Property names remain in camelCase.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * String sql = LCSB.insert(user).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         * 
         * @see #insert(Object, Set)
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object with excluded properties.
         * 
         * <p>This method is similar to {@link #insert(Object)} but allows exclusion of
         * specific properties from the INSERT statement beyond those excluded by annotations.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * Set<String> excluded = new HashSet<>(Arrays.asList("createdDate", "modifiedDate"));
         * String sql = LCSB.insert(user, excluded).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for an entity class.
         * 
         * <p>This method generates an INSERT template for the specified entity class,
         * including all insertable properties. Properties marked with @ReadOnly, @ReadOnlyId,
         * or @Transient are automatically excluded.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.insert(User.class).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName, email)
         * }</pre>
         * 
         * @param entityClass the entity class to create INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @see #insert(Class, Set)
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for an entity class with excluded properties.
         * 
         * <p>This method generates an INSERT template for the specified entity class,
         * excluding the specified properties. Properties marked with {@link ReadOnly},
         * {@link ReadOnlyId}, or {@link com.landawn.abacus.annotation.Transient} annotations 
         * are automatically excluded.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
         * String sql = LCSB.insert(User.class, excluded).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName, email)
         * }</pre>
         * 
         * @param entityClass the entity class to create INSERT for
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class.
         * 
         * <p>This is a convenience method that combines {@link #insert(Class)} and
         * {@link #into(Class)} operations. The table name is derived from the entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.insertInto(User.class)
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         * 
         * @param entityClass the entity class to insert into
         * @return a new SQLBuilder instance configured for INSERT INTO operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @see #insertInto(Class, Set)
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class with excluded properties.
         * 
         * <p>This is a convenience method that combines {@link #insert(Class, Set)} and
         * {@link #into(Class)} operations. The table name is derived from the entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("id"));
         * String sql = LCSB.insertInto(User.class, excluded)
         *                  .values("John", "Doe", "john@example.com")
         *                  .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) VALUES ('John', 'Doe', 'john@example.com')
         * }</pre>
         * 
         * @param entityClass the entity class to insert into
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance configured for INSERT INTO operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple entities or property maps.
         * 
         * <p>This method generates MySQL-style batch insert SQL for inserting multiple
         * rows in a single statement. The input collection can contain either entity
         * objects or Map instances. All items must have the same structure.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * String sql = LCSB.batchInsert(users).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName) VALUES ('John', 'Doe'), ('Jane', 'Smith')
         * }</pre>
         * 
         * @param propsList collection of entities or property maps to batch insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * <p>
         * <b>Note:</b> This is a beta feature and may be subject to change
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * <p>This method initializes a new SQLBuilder for UPDATE operations on the
         * specified table. The columns to update should be specified using the
         * {@code set()} method.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.update("users")
         *                  .set("lastName", "'Smith'")
         *                  .where(Filters.eq("id", 123))
         *                  .sql();
         * // Output: UPDATE users SET lastName = 'Smith' WHERE id = 123
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class context.
         * 
         * <p>This method is similar to {@link #update(String)} but also provides entity
         * class information for better type safety and property name mapping.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.update("users", User.class)
         *                  .set("age", 31)
         *                  .where(Filters.eq("firstName", "'John'"))
         *                  .sql();
         * // Output: UPDATE users SET age = 31 WHERE firstName = 'John'
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class corresponding to the table
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class and includes
         * all updatable properties. Properties marked with {@link NonUpdatable} or
         * {@link ReadOnly} are automatically excluded.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.update(User.class)
         *                  .set("age", 31)
         *                  .where(Filters.eq("firstName", "'John'"))
         *                  .sql();
         * // Output: UPDATE users SET age = 31 WHERE firstName = 'John'
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @see #update(Class, Set)
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class, excluding specified properties.
         * 
         * <p>This method generates an UPDATE template for the specified entity class,
         * excluding the specified properties. Properties marked with {@link ReadOnly},
         * {@link NonUpdatable}, or {@link com.landawn.abacus.annotation.Transient} 
         * annotations are automatically excluded.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("id", "createdDate"));
         * String sql = LCSB.update(User.class, excluded)
         *                  .set("firstName", "'John'")
         *                  .where(Filters.eq("id", 123))
         *                  .sql();
         * // Output: UPDATE users SET firstName = 'John' WHERE id = 123
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table.
         * 
         * <p>This method initializes a new SQLBuilder for DELETE operations on the
         * specified table. A WHERE clause should typically be added to avoid deleting all rows.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.deleteFrom("users")
         *                  .where(Filters.eq("status", "'inactive'"))
         *                  .sql();
         * // Output: DELETE FROM users WHERE status = 'inactive'
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table with entity class context.
         * 
         * <p>This method is similar to {@link #deleteFrom(String)} but also provides entity
         * class information for better type safety in WHERE conditions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.deleteFrom("users", User.class)
         *                  .where(Filters.eq("age", 18))
         *                  .sql();
         * // Output: DELETE FROM users WHERE age = 18
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class corresponding to the table
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class using the
         * configured naming policy.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.deleteFrom(User.class)
         *                  .where(Filters.eq("id", 1))
         *                  .sql();
         * // Output: DELETE FROM users WHERE id = 1
         * }</pre>
         * 
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a custom select expression.
         * 
         * <p>This method allows specification of complex SELECT expressions including
         * aggregate functions, calculated fields, etc.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.select("COUNT(*) as total, AVG(salary) as avgSalary")
         *                  .from("employees")
         *                  .sql();
         * // Output: SELECT COUNT(*) as total, AVG(salary) as avgSalary FROM employees
         * }</pre>
         * 
         * @param selectPart the SELECT expression or clause
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified columns.
         * 
         * <p>This method initializes a SELECT query with the specified column names.
         * Column names remain in camelCase format without conversion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.select("firstName", "lastName", "email")
         *                  .from("users")
         *                  .where(Filters.eq("active", true))
         *                  .sql();
         * // Output: SELECT firstName, lastName, email FROM users WHERE active = true
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified columns collection.
         * 
         * <p>This method is similar to {@link #select(String...)} but accepts a Collection
         * of column names instead of varargs.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = getRequiredColumns();
         * String sql = LCSB.select(columns)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT firstName, lastName, email FROM users
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * 
         * <p>This method allows specification of column names with their aliases for
         * the SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = LCSB.select(aliases).from("users").sql();
         * // Output: SELECT firstName AS fname, lastName AS lname FROM users
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class.
         * 
         * <p>This method generates a SELECT statement including all properties of the
         * specified entity class, excluding any transient fields.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.select(User.class)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT id, firstName, lastName, email FROM users
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @see #select(Class, boolean)
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with sub-entity option.
         * 
         * <p>When includeSubEntityProperties is true, properties of sub-entities
         * (nested objects) will also be included in the SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // If User has an Address sub-entity
         * String sql = LCSB.select(User.class, true)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT firstName, lastName, address.street, address.city FROM users
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class, excluding specified properties.
         * 
         * <p>This method generates a SELECT statement for the entity class, excluding
         * the specified properties.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("password", "secretKey"));
         * String sql = LCSB.select(User.class, excluded)
         *                  .from("users")
         *                  .sql();
         * // Output: SELECT id, firstName, lastName, email FROM users
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with sub-entity option and excluded properties.
         * 
         * <p>This method provides full control over which properties to include in the
         * SELECT statement, with options for sub-entities and property exclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = LCSB.select(Order.class, true, excluded)
         *                  .from("orders")
         *                  .sql();
         * // Output includes Order and Customer properties, excluding internalNotes
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class.
         * 
         * <p>This is a convenience method that combines SELECT and FROM operations.
         * The table name is derived from the entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.selectFrom(User.class)
         *                  .where(Filters.eq("active", true))
         *                  .sql();
         * // Output: SELECT id, firstName, lastName, email FROM users WHERE active = true
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         * @throws IllegalArgumentException if entityClass is null
         * 
         * @see #selectFrom(Class, boolean)
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with table alias.
         * 
         * <p>This method allows specification of a table alias for use in complex queries
         * with joins or subqueries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.selectFrom(User.class, "u")
         *                  .where(Filters.eq("u.active", true))
         *                  .sql();
         * // Output: SELECT u.id, u.firstName, u.lastName FROM users u WHERE u.active = true
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with sub-entity option.
         * 
         * <p>This is a convenience method that combines SELECT and FROM operations
         * with the option to include sub-entity properties.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.selectFrom(Order.class, true)
         *                  .where(Filters.gt("totalAmount", 100))
         *                  .sql();
         * // Output includes JOINs for sub-entities
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and sub-entity option.
         * 
         * <p>This method combines table aliasing with sub-entity property inclusion
         * for complex query construction.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.selectFrom(Order.class, "o", true)
         *                  .where(Filters.eq("o.status", "'ACTIVE'"))
         *                  .sql();
         * // Output includes aliased columns and JOINs for sub-entities
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with property exclusion.
         * 
         * <p>This method allows selective property selection with automatic FROM clause.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("largeBlob", "metadata"));
         * String sql = LCSB.selectFrom(User.class, excluded)
         *                  .where(Filters.eq("active", true))
         *                  .sql();
         * // Output: SELECT id, firstName, lastName, email FROM users WHERE active = true
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and property exclusion.
         * 
         * <p>This method provides aliasing capability while excluding specified properties
         * from the SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("internalCode"));
         * String sql = LCSB.selectFrom(User.class, "u", excluded)
         *                  .innerJoin("orders", "o").on("u.id = o.userId")
         *                  .sql();
         * // Output uses alias "u" and excludes internalCode property
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity option and property exclusion.
         * 
         * <p>This method provides a convenient way to create a complete SELECT FROM
         * statement with control over sub-entities and property exclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("deletedFlag"));
         * String sql = LCSB.selectFrom(Order.class, true, excluded)
         *                  .where(Filters.gt("createdDate", "'2023-01-01'"))
         *                  .sql();
         * // Output includes Order with Customer sub-entity, excluding deletedFlag
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with full control over all options.
         * 
         * <p>This method provides complete control over the SELECT FROM statement generation,
         * including table alias, sub-entity properties, and property exclusion. When
         * sub-entities are included, appropriate joins will be generated.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = new HashSet<>(Arrays.asList("debugInfo"));
         * String sql = LCSB.selectFrom(Order.class, "ord", true, excluded)
         *                  .where(Filters.gt("ord.totalAmount", 1000))
         *                  .sql();
         * // Output: Complex SELECT with alias, sub-entities, and exclusions
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the select
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases.
         * 
         * <p>This method sets up a query to select from two tables with aliasing.
         * Each entity's properties will be prefixed with their class alias
         * in the result set.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.select(User.class, "u", "user", 
         *                         Order.class, "o", "order")
         *                  .from("users u")
         *                  .join("orders o").on("u.id = o.userId")
         *                  .sql();
         * // Output: SELECT u.firstName AS "user.firstName", u.lastName AS "user.lastName",
         * //                o.orderId AS "order.orderId", o.orderDate AS "order.orderDate"
         * //         FROM users u
         * //         JOIN orders o ON u.id = o.userId
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for the first entity (used in result mapping)
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for the second entity (used in result mapping)
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases and excluded properties.
         * 
         * <p>This method extends {@link #select(Class, String, String, Class, String, String)}
         * by allowing exclusion of specific properties from each entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclusions = new HashSet<>(Arrays.asList("password"));
         * Set<String> orderExclusions = new HashSet<>(Arrays.asList("internalNotes"));
         * 
         * String sql = LCSB.select(User.class, "u", "user", userExclusions,
         *                         Order.class, "o", "order", orderExclusions)
         *                  .from("users u")
         *                  .join("orders o").on("u.id = o.userId")
         *                  .sql();
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * 
         * <p>This method provides the most flexible way to construct complex SELECT
         * statements involving multiple entities with different configurations. Each
         * Selection object encapsulates the configuration for one entity.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, excludedProps)
         * );
         * 
         * String sql = LCSB.select(selections)
         *                  .from("users u")
         *                  .join("orders o").on("u.id = o.userId")
         *                  .join("products p").on("o.productId = p.id")
         *                  .sql();
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining the entities to select
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with aliases.
         * 
         * <p>This is a convenience method that combines the multi-entity SELECT with
         * automatic FROM clause generation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.selectFrom(User.class, "u", "user", Order.class, "o", "order")
         *                  .where(Filters.eq("u.active", true))
         *                  .sql();
         * // Output: SELECT ... FROM users u, orders o WHERE u.active = true
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT FROM operation
         * @throws IllegalArgumentException if entityClassA is null
         * 
         * @see #select(Class, String, String, Class, String, String)
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with aliases and excluded properties.
         * 
         * <p>This is a convenience method that combines the multi-entity SELECT with
         * automatic FROM clause generation, while allowing property exclusions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExcl = new HashSet<>(Arrays.asList("passwordHash"));
         * Set<String> orderExcl = new HashSet<>(Arrays.asList("debugData"));
         * 
         * String sql = LCSB.selectFrom(User.class, "u", "user", userExcl,
         *                             Order.class, "o", "order", orderExcl)
         *                  .where(Filters.eq("o.status", "'COMPLETED'"))
         *                  .sql();
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT FROM operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * 
         * <p>This method automatically generates the appropriate FROM clause based on
         * the provided Selection configurations. It's the most convenient way to build
         * complex multi-table queries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null)
         * );
         * 
         * String sql = LCSB.selectFrom(selections)
         *                  .where(Filters.eq("u.verified", true))
         *                  .sql();
         * // Output: SELECT u.firstName AS "user.firstName", ... 
         * //         FROM users u, orders o 
         * //         WHERE u.verified = true
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining the entities to select
         * @return a new SQLBuilder instance configured for multi-table SELECT FROM operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         * 
         * <p>This is a convenience method for creating COUNT queries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.count("users")
         *                  .where(Filters.eq("active", true))
         *                  .sql();
         * // Output: SELECT count(*) FROM users WHERE active = true
         * }</pre>
         * 
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for an entity class.
         * 
         * <p>This method derives the table name from the entity class and creates
         * a COUNT query.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = LCSB.count(User.class)
         *                  .where(Filters.between("age", 18, 65))
         *                  .sql();
         * // Output: SELECT count(*) FROM users WHERE age BETWEEN 18 AND 65
         * }</pre>
         * 
         * @param entityClass the entity class to count
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context.
         * 
         * <p>This method is useful for generating just the SQL representation of a
         * condition without building a complete statement. It's typically used for
         * debugging or building dynamic query parts.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("active", true),
         *     Filters.gt("age", 18)
         * );
         * 
         * String sql = LCSB.parse(cond, User.class).sql();
         * // Output: active = true AND age > 18
         * }</pre>
         * 
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for property name resolution
         * @return a new SQLBuilder instance containing only the condition SQL
         * @throws IllegalArgumentException if cond is null
         * 
         * @see Filters
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Parameterized SQL builder with no naming policy transformation.
     * 
     * <p>This builder generates parameterized SQL statements using '?' placeholders and preserves
     * the original casing of property and column names without any transformation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Property names are preserved as-is
     * String sql = PSB.select("first_Name", "last_NaMe")
     *                 .from("account")
     *                 .where(Filters.eq("last_NaMe", 1))
     *                 .sql();
     * // Output: SELECT first_Name, last_NaMe FROM account WHERE last_NaMe = ?
     * }</pre>
     */
    public static class PSB extends SQLBuilder {

        /**
         * Constructs a new PSB instance with NO_CHANGE naming policy and parameterized SQL policy.
         *
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        PSB() {
            super(NamingPolicy.NO_CHANGE, SQLPolicy.PARAMETERIZED_SQL);
        }

        /**
         * Creates a new instance of PSB for internal use by the builder pattern.
         *
         * <p>This factory method is used internally to create new builder instances
         * when starting a new SQL construction chain.</p>
         *
         * @return a new PSB instance
         */
        protected static PSB createInstance() {
            return new PSB();
        }

        /**
         * Creates an INSERT statement builder for a single column expression.
         * 
         * <p>This method is a convenience wrapper that internally calls {@link #insert(String...)}
         * with a single-element array.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.insert("user_name").into("users");
         * }</pre>
         * 
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement builder for the specified columns.
         * 
         * <p>The column names are used as-is without any naming transformation.
         * The actual values must be provided later using the {@code values()} method.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.insert("name", "email", "age")
         *                         .into("users")
         *                         .values("John", "john@example.com", 25);
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to include in the INSERT statement
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement builder for the specified collection of columns.
         * 
         * <p>This method allows using any Collection implementation (List, Set, etc.) to specify
         * the columns for insertion.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("name", "email", "age");
         * SQLBuilder builder = PSB.insert(columns).into("users");
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to include in the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement builder using a map of property names to values.
         * 
         * <p>The map keys represent column names and the values are the corresponding values
         * to be inserted. This provides a convenient way to specify both columns and values
         * in a single call.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("name", "John");
         * data.put("age", 25);
         * SQLBuilder builder = PSB.insert(data).into("users");
         * }</pre>
         * 
         * @param props map where keys are column names and values are the values to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement builder from an entity object.
         * 
         * <p>All non-null properties of the entity will be included in the INSERT statement,
         * except those marked with {@code @Transient}, {@code @ReadOnly}, or {@code @ReadOnlyId}
         * annotations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User("John", "john@example.com", 25);
         * SQLBuilder builder = PSB.insert(user).into("users");
         * }</pre>
         * 
         * @param entity the entity object whose properties will be inserted
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement builder from an entity object with excluded properties.
         * 
         * <p>Properties can be excluded from the INSERT statement by specifying their names
         * in the excludedPropNames set. This is useful when certain properties should not
         * be inserted even if they have values.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User();
         * Set<String> excluded = N.asSet("createdTime", "updatedTime");
         * SQLBuilder builder = PSB.insert(user, excluded).into("users");
         * }</pre>
         * 
         * @param entity the entity object whose properties will be inserted
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement builder for an entity class.
         * 
         * <p>This method generates an INSERT template for all insertable properties of the
         * specified entity class. Properties marked with {@code @Transient}, {@code @ReadOnly},
         * or {@code @ReadOnlyId} annotations are automatically excluded.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.insert(User.class)
         *                         .into("users")
         *                         .values("John", "john@example.com", 25);
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT statement for
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement builder for an entity class with excluded properties.
         * 
         * <p>Generates an INSERT template excluding the specified properties in addition to
         * those automatically excluded by annotations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("version", "lastModified");
         * SQLBuilder builder = PSB.insert(User.class, excluded).into("users");
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT statement for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO statement builder for an entity class.
         * 
         * <p>This is a convenience method that combines {@link #insert(Class)} and {@link #into(Class)}
         * in a single call. The table name is derived from the entity class name or its {@code @Table}
         * annotation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.insertInto(User.class)
         *                         .values("John", "john@example.com", 25);
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT INTO statement for
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO statement builder for an entity class with excluded properties.
         * 
         * <p>Combines INSERT and INTO operations while excluding specified properties from
         * the generated statement.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("id", "version");
         * SQLBuilder builder = PSB.insertInto(User.class, excluded)
         *                         .values("John", "john@example.com");
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT INTO statement for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT statement builder for multiple records.
         * 
         * <p>Generates MySQL-style batch insert SQL that can insert multiple rows in a single
         * statement. The input collection can contain either entity objects or Map instances
         * representing the data to insert.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "john@example.com"),
         *     new User("Jane", "jane@example.com")
         * );
         * SQLBuilder builder = PSB.batchInsert(users).into("users");
         * // Generates: INSERT INTO users (name, email) VALUES (?, ?), (?, ?)
         * }</pre>
         * 
         * @param propsList collection of entities or property maps to insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement builder for the specified table.
         * 
         * <p>The table name is used as-is without any transformation. Columns to update
         * must be specified using the {@code set()} method.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.update("users")
         *                         .set("name", "email")
         *                         .where(Filters.eq("id", 1));
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement builder for a table with entity class mapping.
         * 
         * <p>Specifying the entity class enables property name transformation and validation
         * based on the entity's field definitions.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.update("users", User.class)
         *                         .set("name", "email")
         *                         .where(Filters.eq("id", 1));
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement builder for an entity class.
         * 
         * <p>The table name is derived from the entity class name or its {@code @Table} annotation.
         * All updatable properties (excluding those marked with {@code @NonUpdatable}, {@code @ReadOnly},
         * or {@code @Transient}) are included in the SET clause.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.update(User.class)
         *                         .where(Filters.eq("id", 1));
         * }</pre>
         * 
         * @param entityClass the entity class to generate UPDATE statement for
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement builder for an entity class with excluded properties.
         * 
         * <p>Generates an UPDATE statement excluding the specified properties in addition to
         * those automatically excluded by annotations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("createdTime", "createdBy");
         * SQLBuilder builder = PSB.update(User.class, excluded)
         *                         .where(Filters.eq("id", 1));
         * }</pre>
         * 
         * @param entityClass the entity class to generate UPDATE statement for
         * @param excludedPropNames set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement builder for the specified table.
         * 
         * <p>The table name is used as-is without any transformation. WHERE conditions
         * should be added to avoid deleting all records.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.deleteFrom("users")
         *                         .where(Filters.eq("status", "inactive"));
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM statement builder for a table with entity class mapping.
         * 
         * <p>Specifying the entity class enables property name validation in WHERE conditions
         * based on the entity's field definitions.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.deleteFrom("users", User.class)
         *                         .where(Filters.eq("lastLogin", null));
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement builder for an entity class.
         * 
         * <p>The table name is derived from the entity class name or its {@code @Table} annotation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.deleteFrom(User.class)
         *                         .where(Filters.eq("id", 1));
         * }</pre>
         * 
         * @param entityClass the entity class to generate DELETE FROM statement for
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement builder for a single column or expression.
         * 
         * <p>The selectPart can be a simple column name or a complex expression including
         * functions, aliases, etc.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.select("COUNT(*)").from("users");
         * SQLBuilder builder2 = PSB.select("name AS userName").from("users");
         * }</pre>
         * 
         * @param selectPart the column name or expression to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement builder for multiple columns.
         * 
         * <p>Column names are used as-is without any transformation. Each column can be
         * a simple name or include expressions and aliases.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.select("id", "name", "email")
         *                         .from("users")
         *                         .where(Filters.gt("age", 18));
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement builder for a collection of columns.
         * 
         * <p>This method allows using any Collection implementation (List, Set, etc.) to specify
         * the columns to select.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("id", "name", "email");
         * SQLBuilder builder = PSB.select(columns).from("users");
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement builder with column aliases.
         * 
         * <p>The map keys represent the column names or expressions to select, and the values
         * are their corresponding aliases in the result set.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new LinkedHashMap<>();
         * aliases.put("u.name", "userName");
         * aliases.put("u.email", "userEmail");
         * SQLBuilder builder = PSB.select(aliases).from("users u");
         * // Generates: SELECT u.name AS userName, u.email AS userEmail FROM users u
         * }</pre>
         * 
         * @param propOrColumnNameAliases map where keys are column names and values are aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement builder for all properties of an entity class.
         * 
         * <p>Selects all properties of the entity class except those marked with
         * {@code @Transient} annotation. Sub-entity properties are not included by default.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.select(User.class)
         *                         .from("users")
         *                         .where(Filters.eq("active", true));
         * }</pre>
         * 
         * @param entityClass the entity class whose properties to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement builder for an entity class with sub-entity option.
         * 
         * <p>When includeSubEntityProperties is true, properties of sub-entities (nested objects)
         * are also included in the selection with appropriate aliasing.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // If User has an Address sub-entity
         * SQLBuilder builder = PSB.select(User.class, true)
         *                         .from("users u")
         *                         .join("addresses a").on("u.address_id = a.id");
         * }</pre>
         * 
         * @param entityClass the entity class whose properties to select
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement builder for an entity class with excluded properties.
         * 
         * <p>Generates a SELECT statement excluding the specified properties in addition to
         * those automatically excluded by {@code @Transient} annotation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "secretKey");
         * SQLBuilder builder = PSB.select(User.class, excluded)
         *                         .from("users");
         * }</pre>
         * 
         * @param entityClass the entity class whose properties to select
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement builder with full control over entity property selection.
         * 
         * <p>Provides complete control over which properties to include or exclude, including
         * sub-entity properties.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("internalNotes");
         * SQLBuilder builder = PSB.select(User.class, true, excluded)
         *                         .from("users u")
         *                         .join("addresses a").on("u.address_id = a.id");
         * }</pre>
         * 
         * @param entityClass the entity class whose properties to select
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement builder for an entity class.
         * 
         * <p>This is a convenience method that combines SELECT and FROM operations.
         * The table name is derived from the entity class name or its {@code @Table} annotation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class)
         *                         .where(Filters.eq("status", "active"));
         * // Equivalent to: PSB.select(User.class).from(User.class)
         * }</pre>
         * 
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @return a new SQLBuilder instance with both SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement builder with a table alias.
         * 
         * <p>The alias is used to qualify column names in the generated SQL, which is useful
         * for joins and subqueries.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class, "u")
         *                         .join("orders o").on("u.id = o.user_id");
         * // Generates: SELECT u.id, u.name, ... FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         * 
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param alias the table alias to use in the FROM clause
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement builder with sub-entity properties option.
         * 
         * <p>When includeSubEntityProperties is true, appropriate joins are automatically
         * generated for sub-entities.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class, true)
         *                         .where(Filters.isNotNull("address.city"));
         * }</pre>
         * 
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement builder with alias and sub-entity properties option.
         * 
         * <p>Combines table aliasing with sub-entity property inclusion for complex queries.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class, "u", true)
         *                         .where(Filters.like("u.name", "John%"));
         * }</pre>
         * 
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param alias the table alias to use in the FROM clause
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement builder with excluded properties.
         * 
         * <p>Convenience method for creating a complete SELECT FROM statement while excluding
         * specific properties.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("largeBlob", "internalData");
         * SQLBuilder builder = PSB.selectFrom(User.class, excluded)
         *                         .limit(10);
         * }</pre>
         * 
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement builder with alias and excluded properties.
         * 
         * <p>Provides aliasing capability while excluding specific properties from selection.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password");
         * SQLBuilder builder = PSB.selectFrom(User.class, "u", excluded)
         *                         .join("roles r").on("u.role_id = r.id");
         * }</pre>
         * 
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param alias the table alias to use in the FROM clause
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement builder with sub-entities and excluded properties.
         * 
         * <p>Allows including sub-entity properties while excluding specific properties
         * from the selection.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("user.password", "user.salt");
         * SQLBuilder builder = PSB.selectFrom(Order.class, true, excluded);
         * }</pre>
         * 
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement builder with full control over all options.
         * 
         * <p>This method provides complete control over the SELECT FROM generation, including
         * aliasing, sub-entity properties, and property exclusion. When sub-entities are included,
         * appropriate JOIN clauses may be automatically generated.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("audit.createdBy", "audit.modifiedBy");
         * SQLBuilder builder = PSB.selectFrom(Product.class, "p", true, excluded)
         *                         .where(Filters.gt("p.price", 100))
         *                         .orderBy("p.name");
         * }</pre>
         * 
         * @param entityClass the entity class to generate SELECT FROM statement for
         * @param alias the table alias to use in the FROM clause
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance with SELECT and FROM clauses set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement builder for joining two entity classes.
         * 
         * <p>This method facilitates creating SELECT statements that retrieve data from two
         * related tables. Each entity class can have its own table alias and class alias
         * for property prefixing in the result set.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.select(User.class, "u", "user", 
         *                                Order.class, "o", "order")
         *                         .from("users u")
         *                         .join("orders o").on("u.id = o.user_id");
         * // Properties will be prefixed: user.name, user.email, order.id, order.total
         * }</pre>
         * 
         * @param entityClassA the first entity class to select from
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the prefix for properties of the first entity in results
         * @param entityClassB the second entity class to select from
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the prefix for properties of the second entity in results
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if any required parameter is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement builder for joining two entities with excluded properties.
         * 
         * <p>Provides fine-grained control over which properties to include from each entity
         * when performing joins.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclude = N.asSet("password", "salt");
         * Set<String> orderExclude = N.asSet("internalNotes");
         * 
         * SQLBuilder builder = PSB.select(User.class, "u", "user", userExclude,
         *                                Order.class, "o", "order", orderExclude)
         *                         .from("users u")
         *                         .join("orders o").on("u.id = o.user_id");
         * }</pre>
         * 
         * @param entityClassA the first entity class to select from
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the prefix for properties of the first entity in results
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class to select from
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the prefix for properties of the second entity in results
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if entity classes are null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement builder for multiple entity selections.
         * 
         * <p>This method provides the most flexibility for complex multi-table queries,
         * allowing each table to have its own configuration including aliases, column
         * selections, and sub-entity handling.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", Arrays.asList("id", "name"), false, null),
         *     new Selection(Order.class, "o", "order", null, true, N.asSet("deleted"))
         * );
         * 
         * SQLBuilder builder = PSB.select(selections)
         *                         .from("users u")
         *                         .join("orders o").on("u.id = o.user_id");
         * }</pre>
         * 
         * @param multiSelects list of Selection configurations for each table
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if multiSelects is null, empty, or contains invalid selections
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for joining two entity classes.
         * 
         * <p>This convenience method combines SELECT and FROM operations for two-table joins.
         * The FROM clause is automatically generated based on the entity configurations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.selectFrom(User.class, "u", "user",
         *                                    Order.class, "o", "order")
         *                         .where(Filters.eq("u.status", "active"));
         * }</pre>
         * 
         * @param entityClassA the first entity class to select from
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the prefix for properties of the first entity in results
         * @param entityClassB the second entity class to select from
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the prefix for properties of the second entity in results
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if any required parameter is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for two entities with excluded properties.
         * 
         * <p>Generates a complete SELECT FROM statement for joining two tables while
         * excluding specified properties from each entity.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludeUser = N.asSet("passwordHash");
         * Set<String> excludeOrder = N.asSet("internalId");
         * 
         * SQLBuilder builder = PSB.selectFrom(User.class, "u", "user", excludeUser,
         *                                    Order.class, "o", "order", excludeOrder)
         *                         .where(Filters.between("o.created", startDate, endDate));
         * }</pre>
         * 
         * @param entityClassA the first entity class to select from
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the prefix for properties of the first entity in results
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class to select from
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the prefix for properties of the second entity in results
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entity classes are null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement builder for multiple entity selections.
         * 
         * <p>This method automatically generates both SELECT and FROM clauses based on
         * the provided Selection configurations, including proper table aliasing and joins
         * for sub-entities when specified.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Customer.class, "c", "customer", null, true, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", Arrays.asList("name", "price"), false, null)
         * );
         * 
         * SQLBuilder builder = PSB.selectFrom(selections)
         *                         .where(Filters.and(
         *                             Filters.eq("c.status", "premium"),
         *                             Filters.gt("o.total", 1000)
         *                         ));
         * }</pre>
         * 
         * @param multiSelects list of Selection configurations for each table
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if multiSelects is null, empty, or invalid
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query builder for the specified table.
         * 
         * <p>This is a convenience method for creating count queries without specifying
         * the COUNT(*) expression explicitly.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * SQLBuilder builder = PSB.count("users")
         *                         .where(Filters.eq("status", "active"));
         * // Generates: SELECT count(*) FROM users WHERE status = ?
         * }</pre>
         * 
         * @param tableName the name of the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT query
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query builder for an entity class.
         * 
         * <p>The table name is derived from the entity class name or its {@code @Table} annotation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * long count = PSB.count(User.class)
         *                 .where(Filters.like("email", "%@example.com"))
         *                 .queryForSingleResult(Long.class);
         * }</pre>
         * 
         * @param entityClass the entity class to count rows for
         * @return a new SQLBuilder instance configured for COUNT query
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context.
         * 
         * <p>This method is useful for generating SQL fragments from Condition objects,
         * particularly for debugging or when building complex dynamic queries. The entity
         * class provides context for property name resolution.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("status", "active"),
         *     Filters.gt("age", 18)
         * );
         * String sql = PSB.parse(cond, User.class).sql();
         * // Result: "status = ? AND age > ?"
         * }</pre>
         * 
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for property name context
         * @return a new SQLBuilder instance containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Parameterized SQL builder with snake_case (lower case with underscore) field/column naming strategy.
     * 
     * <p>PSC (Parameterized Snake Case) generates SQL with placeholder parameters (?) and converts
     * property names from camelCase to snake_case. This is the most commonly used SQL builder
     * for applications using standard SQL databases with snake_case column naming conventions.</p>
     * 
     * <p><b>Naming Convention:</b></p>
     * <ul>
     *   <li>Property: firstName → Column: first_name</li>
     *   <li>Property: accountNumber → Column: account_number</li>
     *   <li>Property: isActive → Column: is_active</li>
     * </ul>
     * 
     * <p><b>Basic Usage Examples:</b></p>
     * <pre>{@code
     * // Simple SELECT
     * String sql = PSC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(Filters.eq("id", 1))
     *                 .sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = ?
     * 
     * // INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = PSC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (first_name, last_name) VALUES (?, ?)
     * 
     * // UPDATE with specific fields
     * String sql = PSC.update("account")
     *                 .set("firstName", "John")
     *                 .set("lastName", "Smith")
     *                 .where(Filters.eq("id", 1))
     *                 .sql();
     * // Output: UPDATE account SET first_name = ?, last_name = ? WHERE id = ?
     * }</pre>
     * 
     * <p><b>Advanced Examples:</b></p>
     * <pre>{@code
     * // SELECT with entity class
     * String sql = PSC.selectFrom(Account.class)
     *                 .where(Filters.gt("createdDate", new Date()))
     *                 .orderBy("lastName ASC")
     *                 .limit(10)
     *                 .sql();
     * 
     * // Batch INSERT
     * List<Account> accounts = Arrays.asList(account1, account2, account3);
     * SP sqlPair = PSC.batchInsert(accounts).into("account").build();
     * // sqlPair.sql: INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?), (?, ?)
     * // sqlPair.parameters: ["John", "Doe", "Jane", "Smith", "Bob", "Johnson"]
     * 
     * // Complex JOIN query
     * String sql = PSC.select("a.id", "a.firstName", "COUNT(o.id) AS orderCount")
     *                 .from("account a")
     *                 .leftJoin("orders o").on("a.id = o.account_id")
     *                 .groupBy("a.id", "a.firstName")
     *                 .having(Filters.gt("COUNT(o.id)", 5))
     *                 .sql();
     * }</pre>
     * 
     * @see SQLBuilder
     * @see NSC
     */
    public static class PSC extends SQLBuilder {

        /**
         * Constructs a new PSC instance with snake_case naming policy and parameterized SQL policy.
         *
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        PSC() {
            super(NamingPolicy.SNAKE_CASE, SQLPolicy.PARAMETERIZED_SQL);
        }

        /**
         * Creates a new instance of PSC for internal use by the builder pattern.
         *
         * <p>This factory method is used internally to create new builder instances
         * when starting a new SQL construction chain.</p>
         *
         * @return a new PSC instance
         */
        protected static PSC createInstance() {
            return new PSC();
        }

        /**
         * Creates an INSERT statement for a single column expression.
         * 
         * <p>This method creates an INSERT statement template with a single column. The actual value
         * will be provided as a parameter when executing the query.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.insert("firstName").into("account").sql();
         * // Output: INSERT INTO account (first_name) VALUES (?)
         * }</pre>
         * 
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for multiple columns.
         * 
         * <p>This method creates an INSERT statement template with multiple columns. Property names
         * are automatically converted to snake_case format. Values will be provided as parameters
         * when executing the query.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.insert("firstName", "lastName", "email")
         *                 .into("account")
         *                 .sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for a collection of columns.
         * 
         * <p>This method provides flexibility when column names are dynamically generated or come from
         * a collection. Property names are automatically converted to snake_case format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = PSC.insert(columns).into("account").sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement from a map of property names and values.
         * 
         * <p>This method generates an INSERT statement where map keys represent property names
         * (converted to snake_case) and values are used to generate parameter placeholders.
         * The actual values can be retrieved using the {@code build()} method.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * SP sqlPair = PSC.insert(props).into("account").build();
         * // sqlPair.sql: INSERT INTO account (first_name, last_name) VALUES (?, ?)
         * // sqlPair.parameters: ["John", "Doe"]
         * }</pre>
         * 
         * @param props map of property names to their values
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement from an entity object.
         * 
         * <p>This method inspects the entity object and extracts all non-null properties that are
         * suitable for insertion. Properties marked with @Transient, @ReadOnly, or @ReadOnlyId
         * annotations are automatically excluded. Property names are converted to snake_case format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setEmail("john.doe@example.com");
         * 
         * SP sqlPair = PSC.insert(account).into("account").build();
         * // sqlPair.sql: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john.doe@example.com"]
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement from an entity object with excluded properties.
         * 
         * <p>This method allows fine-grained control over which properties to include in the INSERT
         * statement. Properties in the exclusion set will not be included even if they have values
         * and are normally insertable.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setEmail("john.doe@example.com");
         * account.setCreatedDate(new Date());
         * 
         * Set<String> excluded = N.asSet("createdDate");
         * SP sqlPair = PSC.insert(account, excluded).into("account").build();
         * // sqlPair.sql: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john.doe@example.com"]
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for an entity class.
         * 
         * <p>This method generates an INSERT statement template based on the entity class structure.
         * All properties suitable for insertion (excluding those marked with @Transient, @ReadOnly,
         * or @ReadOnlyId) are included. Property names are converted to snake_case format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.insert(Account.class).into("account").sql();
         * // Output: INSERT INTO account (first_name, last_name, email, created_date) VALUES (?, ?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class with excluded properties.
         * 
         * <p>This method generates an INSERT statement template based on the entity class structure,
         * excluding specified properties. This is useful for creating reusable INSERT templates
         * that exclude certain fields like auto-generated IDs or timestamps.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("createdDate", "modifiedDate");
         * String sql = PSC.insert(Account.class, excluded).into("account").sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO statement for an entity class.
         * 
         * <p>This is a convenience method that combines insert() and into() operations.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * Property names are converted to snake_case format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.insertInto(Account.class).sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT INTO for
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO statement for an entity class with excluded properties.
         * 
         * <p>This convenience method combines insert() and into() operations while allowing
         * property exclusion. The table name is derived from the entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("id", "createdDate");
         * String sql = PSC.insertInto(Account.class, excluded).sql();
         * // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT INTO for
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generates a MySQL-style batch INSERT statement.
         * 
         * <p>This method creates an efficient batch insert statement with multiple value sets
         * in a single INSERT statement, which is particularly useful for MySQL databases and
         * provides better performance than multiple individual INSERT statements.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "Doe"),
         *     new Account("Jane", "Smith"),
         *     new Account("Bob", "Johnson")
         * );
         * 
         * SP sqlPair = PSC.batchInsert(accounts).into("account").build();
         * // sqlPair.sql: INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?), (?, ?)
         * // sqlPair.parameters: ["John", "Doe", "Jane", "Smith", "Bob", "Johnson"]
         * }</pre>
         * 
         * @param propsList list of entities or property maps to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propsList is null or empty
         * @deprecated This feature is in beta and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table.
         * 
         * <p>This method starts building an UPDATE statement. Use the {@code set()} method to specify
         * which columns to update and their values. Property names in subsequent operations will be
         * converted to snake_case format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.update("account")
         *                 .set("firstName", "John")
         *                 .set("lastName", "Smith")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET first_name = ?, last_name = ? WHERE id = ?
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class mapping.
         * 
         * <p>This method creates an UPDATE statement where the entity class provides property-to-column
         * name mapping information. This ensures proper snake_case conversion for all property names
         * used in the update operation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.update("account", Account.class)
         *                 .set("firstName", "John")
         *                 .set("lastModified", new Date())
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET first_name = ?, last_modified = ? WHERE id = ?
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class.
         *
         * <p>This method creates an UPDATE statement where the table name is derived from the entity
         * class name or {@code @Table} annotation. All updatable properties (excluding those marked with
         * {@code @ReadOnly} or {@code @NonUpdatable}) are included by default.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.update(Account.class)
         *                 .set("status", "active")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET status = ? WHERE id = ?
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class with excluded properties.
         * 
         * <p>This method creates an UPDATE statement excluding specified properties in addition to
         * those automatically excluded by annotations (@ReadOnly, @NonUpdatable). This is useful
         * for partial updates or when certain fields should never be updated.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("createdDate", "createdBy");
         * String sql = PSC.update(Account.class, excluded)
         *                 .set(account)
         *                 .where(Filters.eq("id", account.getId()))
         *                 .sql();
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the update
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for a table.
         * 
         * <p>This method starts building a DELETE statement. Typically followed by WHERE conditions
         * to specify which rows to delete. Property names in WHERE conditions will be converted
         * to snake_case format if an entity class is associated.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.deleteFrom("account")
         *                 .where(Filters.eq("status", "inactive"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE status = ?
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for a table with entity class mapping.
         * 
         * <p>This method creates a DELETE statement where the entity class provides property-to-column
         * name mapping for WHERE conditions. This ensures proper snake_case conversion for property
         * names used in conditions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.deleteFrom("account", Account.class)
         *                 .where(Filters.lt("lastLoginDate", thirtyDaysAgo))
         *                 .sql();
         * // Output: DELETE FROM account WHERE last_login_date < ?
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for an entity class.
         * 
         * <p>This method creates a DELETE statement where the table name is derived from the entity
         * class name or @Table annotation. Property names in WHERE conditions will be automatically
         * converted to snake_case format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.deleteFrom(Account.class)
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id = ?
         * }</pre>
         * 
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression.
         * 
         * <p>This method is useful for complex select expressions, aggregate functions, or when
         * selecting computed values. The expression is used as-is without property name conversion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.select("COUNT(*)")
         *                 .from("account")
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE status = ?
         * 
         * String sql2 = PSC.select("firstName || ' ' || lastName AS fullName")
         *                  .from("account")
         *                  .sql();
         * // Output: SELECT firstName || ' ' || lastName AS fullName FROM account
         * }</pre>
         * 
         * @param selectPart the select expression
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement with multiple columns.
         * 
         * <p>This method creates a SELECT statement for multiple columns. Property names are
         * converted to snake_case format and aliased back to their original camelCase names
         * to maintain proper object mapping.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.select("id", "firstName", "lastName", "email")
         *                 .from("account")
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = ?
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement with a collection of columns.
         * 
         * <p>This method provides flexibility when column names are dynamically generated. Property
         * names are converted to snake_case format with appropriate aliases.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("id", "firstName", "lastName");
         * String sql = PSC.select(columns)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName" FROM account
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with column aliases.
         * 
         * <p>This method allows specifying custom aliases for selected columns. The map keys are
         * property names (converted to snake_case) and values are their desired aliases in the
         * result set.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> columnAliases = new HashMap<>();
         * columnAliases.put("firstName", "fname");
         * columnAliases.put("lastName", "lname");
         * columnAliases.put("emailAddress", "email");
         * 
         * String sql = PSC.select(columnAliases)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT first_name AS "fname", last_name AS "lname", email_address AS "email" FROM account
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all properties of an entity class.
         * 
         * <p>This method generates a SELECT statement including all properties from the entity class
         * that are not marked with @Transient. Property names are converted to snake_case with
         * appropriate aliases.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.select(Account.class)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email, created_date AS "createdDate" FROM account
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with optional sub-entity properties.
         * 
         * <p>When includeSubEntityProperties is true, properties of nested entity objects are also
         * included in the selection with appropriate prefixes. This is useful for fetching related
         * entities in a single query.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Without sub-entities
         * String sql1 = PSC.select(Order.class, false)
         *                  .from("orders")
         *                  .sql();
         * 
         * // With sub-entities (includes nested object properties)
         * String sql2 = PSC.select(Order.class, true)
         *                  .from("orders")
         *                  .sql();
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class with excluded properties.
         * 
         * <p>This method selects all properties from the entity class except those specified in
         * the exclusion set. This is useful for queries that need most but not all properties.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "secretKey");
         * String sql = PSC.select(Account.class, excluded)
         *                 .from("account")
         *                 .sql();
         * // Selects all Account properties except password and secretKey
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement for an entity class with sub-entities and exclusions.
         * 
         * <p>This method provides full control over entity property selection, allowing both
         * inclusion of sub-entity properties and exclusion of specific properties.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("internalNotes", "auditLog");
         * String sql = PSC.select(Order.class, true, excluded)
         *                 .from("orders")
         *                 .sql();
         * // Selects all Order properties including sub-entities, except excluded ones
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for an entity class.
         * 
         * <p>This is a convenience method that combines select() and from() operations.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * All property names are converted to snake_case with appropriate aliases.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.selectFrom(Account.class)
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = ?
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement for an entity class with table alias.
         * 
         * <p>This method creates a SELECT FROM statement where columns are prefixed with the table
         * alias. This is useful for joins and disambiguating column names in complex queries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.selectFrom(Account.class, "a")
         *                 .where(Filters.eq("a.status", "active"))
         *                 .sql();
         * // Output: SELECT a.id, a.first_name AS "firstName", a.last_name AS "lastName", a.email FROM account a WHERE a.status = ?
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement with optional sub-entity properties.
         * 
         * <p>This convenience method combines SELECT and FROM operations with control over
         * sub-entity inclusion. When sub-entities are included, appropriate joins may be
         * generated automatically.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.selectFrom(Order.class, true)
         *                 .where(Filters.gt("total", 100))
         *                 .sql();
         * // Includes properties from nested entities like customer, items, etc.
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with alias and sub-entity option.
         * 
         * <p>This method combines table aliasing with sub-entity property inclusion for
         * complex queries involving related entities.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.selectFrom(Order.class, "o", true)
         *                 .where(Filters.eq("o.status", "pending"))
         *                 .sql();
         * // Selects from orders with alias 'o' including sub-entity properties
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with excluded properties.
         * 
         * <p>This convenience method creates a complete SELECT FROM statement while excluding
         * specific properties from the selection.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "secretKey");
         * String sql = PSC.selectFrom(Account.class, excluded)
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * // Selects all properties except password and secretKey
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with alias and excluded properties.
         * 
         * <p>This method combines table aliasing with property exclusion for precise control
         * over the generated SELECT statement.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password");
         * String sql = PSC.selectFrom(Account.class, "a", excluded)
         *                 .innerJoin("orders o").on("a.id = o.account_id")
         *                 .sql();
         * // Selects from account with alias 'a', excluding password
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with sub-entities and exclusions.
         * 
         * <p>This method provides control over both sub-entity inclusion and property exclusion
         * while automatically determining the appropriate table alias.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("internalData");
         * String sql = PSC.selectFrom(Order.class, true, excluded)
         *                 .where(Filters.between("orderDate", startDate, endDate))
         *                 .sql();
         * // Includes sub-entities but excludes internalData
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM statement with all options.
         * 
         * <p>This method provides maximum flexibility by allowing control over table alias,
         * sub-entity inclusion, and property exclusion. It handles complex scenarios including
         * automatic join generation for sub-entities.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "internalNotes");
         * String sql = PSC.selectFrom(Account.class, "a", true, excluded)
         *                 .innerJoin("orders o").on("a.id = o.account_id")
         *                 .where(Filters.gt("o.total", 1000))
         *                 .sql();
         * // Complex query with full control over selection
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.SNAKE_CASE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for multiple entity classes (for joins).
         * 
         * <p>This method is designed for queries that need to select from multiple tables,
         * typically used with joins. Each entity gets both a table alias and a class alias
         * for proper result mapping. Property names are converted to snake_case.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.select(Account.class, "a", "account",
         *                        Order.class, "o", "order")
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.account_id")
         *                 .sql();
         * // Output: SELECT a.id AS "account.id", a.first_name AS "account.firstName", ..., 
         * //                o.id AS "order.id", o.order_date AS "order.orderDate", ...
         * }</pre>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity results
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity results
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for multiple entity classes with exclusions.
         * 
         * <p>Extended version that allows excluding specific properties from each entity
         * in the multi-table select. This is useful for joins where you don't need all
         * properties from each table.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclude = N.asSet("password", "salt");
         * Set<String> orderExclude = N.asSet("internalNotes");
         * 
         * String sql = PSC.select(Account.class, "a", "account", userExclude,
         *                        Order.class, "o", "order", orderExclude)
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.account_id")
         *                 .sql();
         * }</pre>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity results
         * @param excludedPropNamesA excluded properties for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity results
         * @param excludedPropNamesB excluded properties for second entity
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entities using Selection descriptors.
         * 
         * <p>This is the most flexible method for multi-entity queries, allowing any number
         * of entities with full control over aliases, sub-entities, and exclusions. Each
         * Selection object defines how to select from one entity.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, null),
         *     new Selection(Order.class, "o", "order", Arrays.asList("id", "total"), false, null),
         *     new Selection(Product.class, "p", "product", null, false, N.asSet("description"))
         * );
         * 
         * String sql = PSC.select(selections)
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.account_id")
         *                 .innerJoin("order_items oi").on("o.id = oi.order_id")
         *                 .innerJoin("products p").on("oi.product_id = p.id")
         *                 .sql();
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM statement for multiple entity classes.
         * 
         * <p>This convenience method combines select() and from() for multi-table queries.
         * The FROM clause is automatically generated based on the entity classes and their
         * aliases.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.selectFrom(Account.class, "a", "account",
         *                            Order.class, "o", "order")
         *                 .innerJoin("o").on("a.id = o.account_id")
         *                 .sql();
         * // Automatically generates appropriate FROM clause
         * }</pre>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for multiple entity classes with exclusions.
         * 
         * <p>Extended version allowing property exclusions for each entity in the query.
         * The FROM clause is automatically generated.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclude = N.asSet("password");
         * String sql = PSC.selectFrom(Account.class, "a", "account", userExclude,
         *                            Order.class, "o", "order", null)
         *                 .innerJoin("o").on("a.id = o.account_id")
         *                 .sql();
         * }</pre>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param excludedPropNamesA excluded properties for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @param excludedPropNamesB excluded properties for second entity
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement for multiple entity selections.
         * 
         * <p>Most flexible method for multi-entity queries with automatic FROM clause generation.
         * Each Selection object can have different configurations for its entity.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, null),
         *     new Selection(Order.class, "o", "order", null, true, null),  // include sub-entities
         *     new Selection(Product.class, "p", "product", null, false, N.asSet("details"))
         * );
         * 
         * String sql = PSC.selectFrom(selections)
         *                 .where(Filters.eq("a.status", "active"))
         *                 .sql();
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.SNAKE_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for a table.
         * 
         * <p>Convenience method for generating count queries. This is equivalent to
         * {@code select("COUNT(*)").from(tableName)} but more expressive.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.count("account")
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE status = ?
         * }</pre>
         * 
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.
         * This is a convenient way to count rows with entity class mapping for proper
         * property name conversion in WHERE conditions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PSC.count(Account.class)
         *                 .where(Filters.isNotNull("email"))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE email IS NOT NULL
         * }</pre>
         * 
         * @param entityClass the entity class to count
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class mapping.
         * 
         * <p>This method is useful for generating just the WHERE clause portion of a query
         * with proper property-to-column name mapping.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("firstName", "John"),
         *     Filters.like("email", "%@example.com")
         * );
         * 
         * String sql = PSC.parse(cond, Account.class).sql();
         * // Output: first_name = ? AND email LIKE ?
         * }</pre>
         * 
         * @param cond the condition to parse
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance containing just the condition SQL
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Parameterized SQL builder with SCREAMING_SNAKE_CASE naming policy.
     * 
     * <p>This builder generates parameterized SQL statements (using '?' placeholders) with column names 
     * converted to uppercase with underscores. This follows the traditional database naming convention.</p>
     * 
     * <p>Key features:</p>
     * <ul>
     *   <li>Converts camelCase property names to SCREAMING_SNAKE_CASE column names</li>
     *   <li>Uses '?' placeholders for parameter binding</li>
     *   <li>Maintains property name aliases in result sets for proper object mapping</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Property 'firstName' becomes column 'FIRST_NAME'
     * String sql = PAC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(Filters.eq("id", 1))
     *                 .sql();
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = ?
     * }</pre>
     */
    public static class PAC extends SQLBuilder {

        /**
         * Constructs a new PAC instance with SCREAMING_SNAKE_CASE naming policy and parameterized SQL policy.
         * 
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        PAC() {
            super(NamingPolicy.SCREAMING_SNAKE_CASE, SQLPolicy.PARAMETERIZED_SQL);
        }

        /**
         * Creates a new instance of PAC.
         * 
         * <p>This factory method is used internally by the static methods to create new builder instances.
         * Each SQL building operation starts with a fresh instance to ensure thread safety.</p>
         * 
         * @return a new PAC instance
         */
        protected static PAC createInstance() {
            return new PAC();
        }

        /**
         * Creates an INSERT statement for a single expression or column.
         * 
         * <p>This method is a convenience wrapper that delegates to {@link #insert(String...)} 
         * with a single element array.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.insert("name").into("users").sql();
         * // Output: INSERT INTO USERS (NAME) VALUES (?)
         * }</pre>
         * 
         * @param expr the expression or column name to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for specified columns.
         * 
         * <p>The column names will be converted according to the SCREAMING_SNAKE_CASE naming policy.
         * Use {@link #into(String)} to specify the target table.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.insert("firstName", "lastName", "email")
         *                 .into("users")
         *                 .sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to include in the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for specified columns from a collection.
         * 
         * <p>This method accepts a collection of column names, providing flexibility when 
         * the column list is dynamically generated.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = PAC.insert(columns).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to include
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement from a map of property names to values.
         * 
         * <p>The map keys represent column names and will be converted according to the naming policy.
         * The values are used to determine the number of parameter placeholders needed.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("firstName", "John");
         * data.put("lastName", "Doe");
         * String sql = PAC.insert(data).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME) VALUES (?, ?)
         * }</pre>
         * 
         * @param props map of property names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement from an entity object.
         * 
         * <p>This method inspects the entity object and includes all properties that are not marked 
         * with exclusion annotations (@Transient, @ReadOnly, etc.). The table name is inferred 
         * from the entity class or @Table annotation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * String sql = PAC.insert(user).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement from an entity object with excluded properties.
         * 
         * <p>This method allows fine-grained control over which properties to include in the INSERT.
         * Properties in the exclusion set will not be included even if they are normally insertable.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * Set<String> exclude = new HashSet<>(Arrays.asList("createdDate", "modifiedDate"));
         * String sql = PAC.insert(user, exclude).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement template for an entity class.
         * 
         * <p>This method generates an INSERT statement based on the class structure without 
         * requiring an actual entity instance. All insertable properties are included.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.insert(User.class).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement template for an entity class with excluded properties.
         * 
         * <p>This method generates an INSERT statement based on the class structure, excluding 
         * specified properties. Useful for creating reusable INSERT templates.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = PAC.insert(User.class, exclude).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for an entity class with automatic table name resolution.
         * 
         * <p>This is a convenience method that combines {@link #insert(Class)} with {@link #into(Class)}.
         * The table name is determined from the @Table annotation or class name.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.insertInto(User.class).sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to insert into
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class with excluded properties and automatic table name.
         * 
         * <p>Combines the functionality of specifying excluded properties with automatic table name resolution.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("id"));
         * String sql = PAC.insertInto(User.class, exclude).sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to insert into
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT statement for multiple entities (MySQL style).
         * 
         * <p>This method generates a single INSERT statement with multiple value sets, 
         * which is more efficient than multiple individual INSERTs. This is particularly 
         * useful for MySQL and compatible databases.</p>
         * 
         * <p>Note: This is a beta feature and may change in future versions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * String sql = PAC.batchInsert(users).into("users").sql();
         * // Output: INSERT INTO USERS (FIRST_NAME, LAST_NAME) VALUES (?, ?), (?, ?)
         * }</pre>
         * 
         * @param propsList collection of entities or property maps to batch insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for a specified table.
         * 
         * <p>This method starts building an UPDATE statement. Use {@link #set(String...)} 
         * to specify which columns to update.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.update("users")
         *                 .set("firstName", "lastName")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE USERS SET FIRST_NAME = ?, LAST_NAME = ? WHERE ID = ?
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class context.
         * 
         * <p>This method allows specifying both the table name and entity class, 
         * which enables proper property-to-column name mapping.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.update("users", User.class)
         *                 .set("firstName", "lastName")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE USERS SET FIRST_NAME = ?, LAST_NAME = ? WHERE ID = ?
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class with automatic table name.
         * 
         * <p>The table name is determined from the @Table annotation or class name. 
         * All updatable properties (excluding @ReadOnly, @NonUpdatable) are included.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.update(User.class)
         *                 .set("firstName", "lastName")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE USERS SET FIRST_NAME = ?, LAST_NAME = ? WHERE ID = ?
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class with excluded properties.
         * 
         * <p>This method generates an UPDATE statement excluding specified properties 
         * in addition to those marked with @ReadOnly or @NonUpdatable annotations.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("version", "modifiedDate"));
         * String sql = PAC.update(User.class, exclude)
         *                 .set("firstName", "lastName")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE USERS SET FIRST_NAME = ?, LAST_NAME = ? WHERE ID = ?
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from updates
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE statement for a specified table.
         * 
         * <p>This method starts building a DELETE FROM statement. Typically followed 
         * by WHERE conditions to specify which rows to delete.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.deleteFrom("users")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM USERS WHERE ID = ?
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE statement for a table with entity class context.
         * 
         * <p>This method allows specifying both the table name and entity class 
         * for proper property-to-column name mapping in WHERE conditions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.deleteFrom("users", User.class)
         *                 .where(Filters.eq("email", "john@example.com"))
         *                 .sql();
         * // Output: DELETE FROM USERS WHERE EMAIL = ?
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE statement for an entity class with automatic table name.
         * 
         * <p>The table name is determined from the @Table annotation or class name.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.deleteFrom(User.class)
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM USERS WHERE ID = ?
         * }</pre>
         * 
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression or column.
         * 
         * <p>This method can accept complex expressions like aggregate functions, 
         * calculations, or simple column names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.select("COUNT(*)").from("users").sql();
         * // Output: SELECT count(*) FROM USERS
         * 
         * String sql2 = PAC.select("MAX(age)").from("users").sql();
         * // Output: SELECT MAX(AGE) FROM USERS
         * }</pre>
         * 
         * @param selectPart the SELECT expression (e.g., "COUNT(*)", "MAX(age)", "firstName")
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement with multiple columns.
         * 
         * <p>Column names will be converted according to the SCREAMING_SNAKE_CASE 
         * naming policy and aliased back to their original property names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.select("firstName", "lastName", "email")
         *                 .from("users")
         *                 .sql();
         * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName", EMAIL AS "email" FROM USERS
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement with columns from a collection.
         * 
         * <p>This method accepts a collection of column names, useful when the column 
         * list is dynamically generated.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = PAC.select(columns).from("users").sql();
         * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName", EMAIL AS "email" FROM USERS
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with column aliases.
         * 
         * <p>This method allows specifying custom aliases for selected columns. 
         * The map keys are column names and values are their aliases.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new LinkedHashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = PAC.select(aliases).from("users").sql();
         * // Output: SELECT FIRST_NAME AS "fname", LAST_NAME AS "lname" FROM USERS
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all properties of an entity class.
         * 
         * <p>This method selects all properties from the entity class that are not 
         * marked with @Transient. Sub-entity properties are not included by default.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.select(User.class).from("users").sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", LAST_NAME AS "lastName", EMAIL AS "email" FROM USERS
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with sub-entity control.
         * 
         * <p>When includeSubEntityProperties is true, properties of nested entity types 
         * are also included in the selection with appropriate prefixes.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // If User has an Address sub-entity
         * String sql = PAC.select(User.class, true).from("users").sql();
         * // Output includes address properties: ADDRESS_STREET AS "address.street", etc.
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class with excluded properties.
         * 
         * <p>This method selects all properties except those specified in the exclusion set.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password", "salt"));
         * String sql = PAC.select(User.class, exclude).from("users").sql();
         * // Output excludes password and salt columns
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement with full control over entity property selection.
         * 
         * <p>This method combines sub-entity inclusion control with property exclusion, 
         * providing maximum flexibility in column selection.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.select(User.class, true, exclude)
         *                 .from("users")
         *                 .sql();
         * // Output includes sub-entity properties but excludes password
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from sub-entities
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for an entity class.
         * 
         * <p>This convenience method combines SELECT and FROM operations. The table name 
         * is derived from the @Table annotation or entity class name.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class).where(Filters.eq("active", true)).sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", ... FROM USERS WHERE ACTIVE = ?
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance with both SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement with a table alias.
         * 
         * <p>The alias is used to qualify column names in the generated SQL, useful 
         * for self-joins or disambiguating columns in complex queries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class, "u")
         *                 .where(Filters.eq("u.active", true))
         *                 .sql();
         * // Output: SELECT u.ID AS "id", u.FIRST_NAME AS "firstName", ... FROM USERS u WHERE u.ACTIVE = ?
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement with sub-entity property control.
         * 
         * <p>When includeSubEntityProperties is true and the entity has sub-entities, 
         * appropriate joins may be generated automatically.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class, true)
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * // Output includes joins for sub-entities if present
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include sub-entity properties
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with table alias and sub-entity control.
         * 
         * <p>Combines table aliasing with sub-entity property inclusion for complex queries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class, "u", true)
         *                 .where(Filters.eq("u.active", true))
         *                 .sql();
         * // Output: SELECT u.ID AS "id", ... FROM USERS u WHERE u.ACTIVE = ?
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include sub-entity properties
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with excluded properties.
         * 
         * <p>This method provides a convenient way to select from an entity while 
         * excluding specific properties.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, exclude).sql();
         * // Output excludes the password column
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with alias and excluded properties.
         * 
         * <p>Combines table aliasing with property exclusion for precise query control.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, "u", exclude).sql();
         * // Output: SELECT u.ID AS "id", ... FROM USERS u (excluding password)
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with sub-entity control and exclusions.
         * 
         * <p>Provides control over both sub-entity inclusion and property exclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, true, exclude).sql();
         * // Output includes sub-entities but excludes password
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include sub-entity properties
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with full control over all options.
         * 
         * <p>This method provides maximum flexibility by allowing control over table alias, 
         * sub-entity inclusion, and property exclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, "u", true, exclude)
         *                 .innerJoin("addresses", "a").on("u.id = a.user_id")
         *                 .sql();
         * // Complex query with full control
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include sub-entity properties
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.SCREAMING_SNAKE_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for two entity classes with aliases.
         * 
         * <p>This method is designed for queries that need to select from two tables, 
         * typically used with joins. Each entity gets both a table alias and a class alias 
         * for result mapping.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.select(User.class, "u", "user", Order.class, "o", "order")
         *                 .from("users", "u")
         *                 .innerJoin("orders", "o").on("u.id = o.user_id")
         *                 .sql();
         * // Output: SELECT u.ID AS "user.id", ..., o.ID AS "order.id", ... 
         * }</pre>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity results
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity results
         * @return a new SQLBuilder instance configured for multi-table SELECT
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for two entity classes with aliases and exclusions.
         * 
         * <p>Extended version that allows excluding specific properties from each entity 
         * in the multi-table select.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclude = new HashSet<>(Arrays.asList("password"));
         * Set<String> orderExclude = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = PAC.select(User.class, "u", "user", userExclude,
         *                        Order.class, "o", "order", orderExclude)
         *                 .from("users", "u")
         *                 .innerJoin("orders", "o").on("u.id = o.user_id")
         *                 .sql();
         * }</pre>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entities using Selection descriptors.
         * 
         * <p>This is the most flexible method for multi-entity queries, allowing any number 
         * of entities with full control over aliases, sub-entities, and exclusions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, null)
         * );
         * String sql = PAC.select(selections)
         *                 .from("users", "u")
         *                 .innerJoin("orders", "o").on("u.id = o.user_id")
         *                 .innerJoin("products", "p").on("o.product_id = p.id")
         *                 .sql();
         * }</pre>
         * 
         * @param multiSelects list of Selection descriptors for each entity
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for two entities.
         * 
         * <p>Convenience method that combines SELECT and FROM for two-table queries. 
         * The FROM clause is automatically generated based on the entity classes.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.selectFrom(User.class, "u", "user", 
         *                            Order.class, "o", "order")
         *                 .innerJoin("o").on("u.id = o.user_id")
         *                 .sql();
         * }</pre>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @return a new SQLBuilder instance with SELECT and FROM configured
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for two entities with exclusions.
         * 
         * <p>Extended version allowing property exclusions for each entity in the query.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclude = new HashSet<>(Arrays.asList("password"));
         * String sql = PAC.selectFrom(User.class, "u", "user", userExclude,
         *                            Order.class, "o", "order", null)
         *                 .innerJoin("o").on("u.id = o.user_id")
         *                 .sql();
         * }</pre>
         * 
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement for multiple entities.
         * 
         * <p>Most flexible method for multi-entity queries with automatic FROM clause generation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null)
         * );
         * String sql = PAC.selectFrom(selections)
         *                 .innerJoin("o").on("u.id = o.user_id")
         *                 .sql();
         * }</pre>
         * 
         * @param multiSelects list of Selection descriptors
         * @return a new SQLBuilder instance with SELECT and FROM configured
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.SCREAMING_SNAKE_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for a table.
         * 
         * <p>Convenience method for generating count queries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.count("users").where(Filters.eq("active", true)).sql();
         * // Output: SELECT count(*) FROM USERS WHERE ACTIVE = ?
         * }</pre>
         * 
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT query
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>Table name is derived from the entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PAC.count(User.class)
         *                 .where(Filters.gt("age", 18))
         *                 .sql();
         * // Output: SELECT count(*) FROM USERS WHERE AGE > ?
         * }</pre>
         * 
         * @param entityClass the entity class to count
         * @return a new SQLBuilder instance configured for COUNT query
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity property mapping.
         * 
         * <p>This method is useful for generating just the SQL representation of a condition, 
         * typically for use in complex queries or debugging.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(Filters.eq("firstName", "John"), Filters.gt("age", 21));
         * String sql = PAC.parse(cond, User.class).sql();
         * // Output: FIRST_NAME = ? AND AGE > ?
         * }</pre>
         * 
         * @param cond the condition to parse
         * @param entityClass entity class for property name mapping
         * @return a new SQLBuilder instance containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Parameterized SQL builder with camelCase field/column naming strategy.
     * 
     * <p>PLC (Parameterized Lower Camel Case) generates SQL with placeholder parameters (?) 
     * while maintaining camelCase naming for both properties and columns. This is useful when 
     * your database columns follow camelCase naming convention instead of the traditional 
     * snake_case.</p>
     * 
     * <p><b>Naming Convention:</b></p>
     * <ul>
     *   <li>Property: firstName → Column: firstName</li>
     *   <li>Property: accountNumber → Column: accountNumber</li>
     *   <li>Property: isActive → Column: isActive</li>
     * </ul>
     * 
     * <p><b>Basic Usage Examples:</b></p>
     * <pre>{@code
     * // Simple SELECT
     * String sql = PLC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(Filters.eq("id", 1))
     *                 .sql();
     * // Output: SELECT firstName, lastName FROM account WHERE id = ?
     * 
     * // INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = PLC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (firstName, lastName) VALUES (?, ?)
     * 
     * // UPDATE with specific fields
     * String sql = PLC.update("account")
     *                 .set("firstName", "John")
     *                 .set("lastName", "Smith")
     *                 .where(Filters.eq("id", 1))
     *                 .sql();
     * // Output: UPDATE account SET firstName = ?, lastName = ? WHERE id = ?
     * }</pre>
     * 
     * <p><b>Advanced Examples:</b></p>
     * <pre>{@code
     * // Complex JOIN query with camelCase columns
     * String sql = PLC.select("a.id", "a.firstName", "COUNT(o.id) AS orderCount")
     *                 .from("account a")
     *                 .leftJoin("orders o").on("a.id = o.accountId")
     *                 .groupBy("a.id", "a.firstName")
     *                 .having(Filters.gt("COUNT(o.id)", 5))
     *                 .sql();
     * 
     * // Using with MongoDB-style collections
     * String sql = PLC.selectFrom(UserProfile.class)
     *                 .where(Filters.and(
     *                     Filters.eq("isActive", true),
     *                     Filters.gte("lastLoginDate", lastWeek)
     *                 ))
     *                 .orderBy("lastLoginDate DESC")
     *                 .sql();
     * }</pre>
     * 
     * @see SQLBuilder
     * @see PSC
     * @see NLC
     */
    public static class PLC extends SQLBuilder {

        /**
         * Constructs a new PLC instance with camelCase naming policy and parameterized SQL policy.
         * 
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        PLC() {
            super(NamingPolicy.CAMEL_CASE, SQLPolicy.PARAMETERIZED_SQL);
        }

        /**
         * Creates a new instance of PLC.
         * 
         * <p>This factory method is used internally by the static methods to create new builder instances.
         * Each SQL building operation starts with a fresh instance to ensure thread safety.</p>
         * 
         * @return a new PLC instance
         */
        protected static PLC createInstance() {
            return new PLC();
        }

        /**
         * Creates an INSERT statement for a single column expression.
         * 
         * <p>This method generates an INSERT statement with a single column using camelCase naming.
         * The generated SQL will use placeholder parameters (?) for the values.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PLC.insert("firstName").into("account").sql();
         * // Output: INSERT INTO account (firstName) VALUES (?)
         * }</pre>
         * 
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for multiple columns.
         * 
         * <p>This method generates an INSERT statement with multiple columns using camelCase naming.
         * The order of columns in the INSERT statement matches the order provided in the parameters.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PLC.insert("firstName", "lastName", "email")
         *                 .into("account")
         *                 .sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * 
         * // With actual values using build()
         * SP sqlPair = PLC.insert("firstName", "lastName", "email")
         *                 .into("account")
         *                 .values("John", "Doe", "john@example.com")
         *                 .build();
         * // sqlPair.sql: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john@example.com"]
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for a collection of columns.
         * 
         * <p>This method is useful when the column list is dynamically generated or comes from
         * another source. The columns maintain their camelCase naming.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = PLC.insert(columns).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * 
         * // Dynamic column selection
         * List<String> requiredFields = getRequiredFields();
         * String sql = PLC.insert(requiredFields).into("userProfile").sql();
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement from a map of property names and values.
         * 
         * <p>This method is particularly useful when you have a dynamic set of fields to insert.
         * The map keys represent column names and values represent the data to insert.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * props.put("emailAddress", "john.doe@example.com");
         * props.put("isActive", true);
         * 
         * SP sqlPair = PLC.insert(props).into("account").build();
         * // sqlPair.sql: INSERT INTO account (firstName, lastName, emailAddress, isActive) VALUES (?, ?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john.doe@example.com", true]
         * 
         * // With null values (will be included)
         * props.put("middleName", null);
         * SP sqlPair2 = PLC.insert(props).into("account").build();
         * // Includes middleName with NULL value
         * }</pre>
         * 
         * @param props map of property names to their values
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement from an entity object.
         * 
         * <p>This method extracts all non-null properties from the entity object,
         * excluding those marked with @Transient, @ReadOnly, or @ReadOnlyId annotations.
         * Property names maintain their camelCase format. This is the most convenient way
         * to insert data when working with entity objects.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setEmailAddress("john.doe@example.com");
         * account.setCreatedDate(new Date());
         * 
         * SP sqlPair = PLC.insert(account).into("account").build();
         * // sqlPair.sql: INSERT INTO account (firstName, lastName, emailAddress, createdDate) VALUES (?, ?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john.doe@example.com", Date object]
         * 
         * // Entity with @ReadOnly fields (will be excluded)
         * @Table("userProfile")
         * public class UserProfile {
         *     @ReadOnlyId
         *     private Long id;  // Excluded from INSERT
         *     private String userName;
         *     @ReadOnly
         *     private Date lastModified;  // Excluded from INSERT
         * }
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement from an entity object with excluded properties.
         * 
         * <p>This method allows fine-grained control over which properties to include in the INSERT.
         * Properties can be excluded explicitly in addition to those marked with annotations.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setEmailAddress("john@example.com");
         * account.setCreatedDate(new Date());
         * account.setInternalNotes("VIP customer");
         * 
         * Set<String> excluded = N.asSet("createdDate", "internalNotes");
         * SP sqlPair = PLC.insert(account, excluded).into("account").build();
         * // sqlPair.sql: INSERT INTO account (firstName, lastName, emailAddress) VALUES (?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john@example.com"]
         * 
         * // Exclude all audit fields
         * Set<String> auditFields = N.asSet("createdBy", "createdDate", "modifiedBy", "modifiedDate");
         * SP sqlPair2 = PLC.insert(account, auditFields).into("account").build();
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for an entity class.
         * 
         * <p>This method includes all properties of the entity class that are suitable for insertion,
         * excluding those marked with @Transient, @ReadOnly, or @ReadOnlyId annotations. This is useful
         * when you want to generate the INSERT structure without having an actual entity instance.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PLC.insert(Account.class).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, email, createdDate) VALUES (?, ?, ?, ?)
         * 
         * // Can be used to prepare statements
         * String template = PLC.insert(UserProfile.class).into("userProfile").sql();
         * // Then bind values with your JDBC framework as needed.
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class with excluded properties.
         * 
         * <p>This method provides control over which properties to include when generating
         * the INSERT statement from a class definition.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Exclude auto-generated fields
         * Set<String> excluded = N.asSet("id", "createdDate", "modifiedDate");
         * String sql = PLC.insert(Account.class, excluded).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * 
         * // Exclude computed fields
         * Set<String> computed = N.asSet("fullName", "age", "accountBalance");
         * String sql2 = PLC.insert(Customer.class, computed).into("customer").sql();
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO statement for an entity class.
         * 
         * <p>This is a convenience method that combines insert() and into() operations.
         * The table name is derived from the entity class name or @Table annotation.
         * This provides the most concise way to generate INSERT statements for entities.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Using class name as table name
         * String sql = PLC.insertInto(Account.class).sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * 
         * // Using @Table annotation
         * @Table("user_accounts")
         * public class Account { ... }
         * 
         * String sql2 = PLC.insertInto(Account.class).sql();
         * // Output: INSERT INTO user_accounts (firstName, lastName, email) VALUES (?, ?, ?)
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT INTO for
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO statement for an entity class with excluded properties.
         * 
         * <p>This convenience method combines insert() and into() operations while allowing
         * property exclusions. The table name is automatically determined from the entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("id", "createdDate", "version");
         * String sql = PLC.insertInto(Account.class, excluded).sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (?, ?, ?)
         * 
         * // For batch operations
         * String template = PLC.insertInto(Order.class, N.asSet("id", "orderNumber")).sql();
         * // Use template for bulk inserts
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT INTO for
         * @param excludedPropNames set of property names to exclude from the insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generates a MySQL-style batch INSERT statement.
         * 
         * <p>This method creates an efficient batch insert statement with multiple value sets
         * in a single INSERT statement. This is significantly more efficient than executing
         * multiple individual INSERT statements. Property names maintain their camelCase format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "Doe", "john@example.com"),
         *     new Account("Jane", "Smith", "jane@example.com"),
         *     new Account("Bob", "Johnson", "bob@example.com")
         * );
         * 
         * SP sqlPair = PLC.batchInsert(accounts).into("account").build();
         * // sqlPair.sql: INSERT INTO account (firstName, lastName, emailAddress) VALUES (?, ?, ?), (?, ?, ?), (?, ?, ?)
         * // sqlPair.parameters: ["John", "Doe", "john@example.com", "Jane", "Smith", "jane@example.com", "Bob", "Johnson", "bob@example.com"]
         * 
         * // With maps
         * List<Map<String, Object>> data = Arrays.asList(
         *     N.asMap("firstName", "John", "lastName", "Doe"),
         *     N.asMap("firstName", "Jane", "lastName", "Smith")
         * );
         * SP sqlPair2 = PLC.batchInsert(data).into("account").build();
         * }</pre>
         * 
         * @param propsList list of entities or property maps to insert
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propsList is null or empty
         * @deprecated This feature is in beta and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table.
         * 
         * <p>This method starts building an UPDATE statement. Column names maintain camelCase format.
         * The actual columns to update are specified using the set() method.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PLC.update("account")
         *                 .set("firstName", "John")
         *                 .set("lastName", "Smith")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = ?, lastName = ? WHERE id = ?
         * 
         * // Update with expression
         * String sql2 = PLC.update("account")
         *                  .set("loginCount", "loginCount + 1")
         *                  .set("lastLoginDate", new Date())
         *                  .where(Filters.eq("id", 1))
         *                  .sql();
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class mapping.
         * 
         * <p>The entity class provides property-to-column name mapping information,
         * which is useful when using the set() method with entity objects.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Smith");
         * account.setModifiedDate(new Date());
         * 
         * SP sqlPair = PLC.update("account", Account.class)
         *                 .set(account)
         *                 .where(Filters.eq("id", account.getId()))
         *                 .build();
         * // sqlPair.sql: UPDATE account SET firstName = ?, lastName = ?, modifiedDate = ? WHERE id = ?
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.
         * All updatable properties are included by default when using set() with an entity object.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Simple update
         * String sql = PLC.update(Account.class)
         *                 .set("status", "active")
         *                 .set("activatedDate", new Date())
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET status = ?, activatedDate = ? WHERE id = ?
         * 
         * // Update with entity
         * Account account = getAccount();
         * account.setStatus("inactive");
         * SP sqlPair = PLC.update(Account.class)
         *                 .set(account)
         *                 .where(Filters.eq("id", account.getId()))
         *                 .build();
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class with excluded properties.
         * 
         * <p>Properties marked with @NonUpdatable or @ReadOnly are automatically excluded.
         * Additional properties can be excluded through the excludedPropNames parameter.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Exclude audit fields from update
         * Set<String> excluded = N.asSet("createdDate", "createdBy");
         * 
         * Account account = getAccount();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setCreatedDate(new Date());   // This will be ignored
         * 
         * SP sqlPair = PLC.update(Account.class, excluded)
         *                 .set(account)
         *                 .where(Filters.eq("id", account.getId()))
         *                 .build();
         * // createdDate excluded even though set in entity
         * 
         * // Exclude version control fields
         * Set<String> versionExcluded = N.asSet("version", "previousVersion");
         * String sql = PLC.update(Document.class, versionExcluded)
         *                 .set("content", newContent)
         *                 .where(Filters.eq("id", docId))
         *                 .sql();
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the update
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for a table.
         * 
         * <p>This method creates a DELETE statement. Always use with a WHERE clause
         * to avoid deleting all rows in the table.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Delete specific records
         * String sql = PLC.deleteFrom("account")
         *                 .where(Filters.eq("status", "inactive"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE status = ?
         * 
         * // Delete with multiple conditions
         * String sql2 = PLC.deleteFrom("account")
         *                  .where(Filters.and(
         *                      Filters.eq("status", "inactive"),
         *                      Filters.lt("lastLoginDate", thirtyDaysAgo)
         *                  ))
         *                  .sql();
         * 
         * // Delete with limit (database-specific)
         * String sql3 = PLC.deleteFrom("logs")
         *                  .where(Filters.lt("createdDate", oneYearAgo))
         *                  .limit(1000)
         *                  .sql();
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for a table with entity class mapping.
         * 
         * <p>The entity class provides property-to-column name mapping for use in WHERE conditions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Delete with entity property names
         * String sql = PLC.deleteFrom("account", Account.class)
         *                 .where(Filters.and(
         *                     Filters.eq("accountType", "trial"),
         *                     Filters.lt("createdDate", expirationDate)
         *                 ))
         *                 .sql();
         * // Property names are used even though table name is specified
         * 
         * // Using with entity instance
         * Account account = getAccount();
         * String sql2 = PLC.deleteFrom("account", Account.class)
         *                  .where(Filters.eq("id", account.getId()))
         *                  .sql();
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM statement for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.
         * This provides a type-safe way to generate DELETE statements.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Delete by ID
         * String sql = PLC.deleteFrom(Account.class)
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id = ?
         * 
         * // Bulk delete with conditions
         * String sql2 = PLC.deleteFrom(Order.class)
         *                  .where(Filters.and(
         *                      Filters.eq("status", "cancelled"),
         *                      Filters.lt("orderDate", oneYearAgo)
         *                  ))
         *                  .sql();
         * 
         * // With @Table annotation
         * @Table("user_sessions")
         * public class Session { ... }
         * 
         * String sql3 = PLC.deleteFrom(Session.class)
         *                  .where(Filters.lt("expiryTime", now))
         *                  .sql();
         * // Output: DELETE FROM user_sessions WHERE expiryTime < ?
         * }</pre>
         * 
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression.
         * 
         * <p>This method is useful for complex select expressions, aggregate functions,
         * or when selecting computed values.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Count query
         * String sql = PLC.select("COUNT(*)")
         *                 .from("account")
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE status = ?
         * 
         * // Complex expression
         * String sql2 = PLC.select("firstName || ' ' || lastName AS fullName")
         *                  .from("account")
         *                  .sql();
         * // Output: SELECT firstName || ' ' || lastName AS fullName FROM account
         * 
         * // Aggregate with grouping
         * String sql3 = PLC.select("departmentId, COUNT(*) AS employeeCount")
         *                  .from("employee")
         *                  .groupBy("departmentId")
         *                  .having(Filters.gt("COUNT(*)", 10))
         *                  .sql();
         * }</pre>
         * 
         * @param selectPart the select expression
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement with multiple columns.
         * 
         * <p>Column names maintain camelCase format. This is the most common way to
         * create SELECT statements when you know the specific columns needed.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Simple select
         * String sql = PLC.select("id", "firstName", "lastName", "email")
         *                 .from("account")
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email FROM account WHERE status = ?
         * 
         * // With table aliases
         * String sql2 = PLC.select("a.id", "a.firstName", "o.orderId", "o.totalAmount")
         *                  .from("account a")
         *                  .innerJoin("orders o").on("a.id = o.accountId")
         *                  .sql();
         * 
         * // Mixed columns and expressions
         * String sql3 = PLC.select("id", "firstName", "lastName", 
         *                          "YEAR(CURRENT_DATE) - YEAR(birthDate) AS age")
         *                  .from("account")
         *                  .sql();
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement with a collection of columns.
         * 
         * <p>This method is useful when the column list is dynamically generated or
         * comes from another source.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Dynamic column selection
         * List<String> columns = getUserSelectedColumns();
         * String sql = PLC.select(columns)
         *                 .from("account")
         *                 .sql();
         * 
         * // Programmatically built column list
         * List<String> cols = new ArrayList<>();
         * cols.add("id");
         * cols.add("firstName");
         * if (includeEmail) {
         *     cols.add("emailAddress");
         * }
         * String sql2 = PLC.select(cols).from("account").sql();
         * 
         * // From entity metadata
         * List<String> entityColumns = getEntityColumns(Account.class);
         * String sql3 = PLC.select(entityColumns).from("account").sql();
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with column aliases.
         * 
         * <p>This method allows you to specify custom aliases for each selected column,
         * which is useful for renaming columns in the result set.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> columnAliases = new HashMap<>();
         * columnAliases.put("firstName", "fname");
         * columnAliases.put("lastName", "lname");
         * columnAliases.put("emailAddress", "email");
         * 
         * String sql = PLC.select(columnAliases)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT firstName AS fname, lastName AS lname, emailAddress AS email FROM account
         * 
         * // For JSON output formatting
         * Map<String, String> jsonAliases = new HashMap<>();
         * jsonAliases.put("id", "user_id");
         * jsonAliases.put("firstName", "first_name");
         * jsonAliases.put("lastName", "last_name");
         * String sql2 = PLC.select(jsonAliases).from("account").sql();
         * 
         * // Complex aliases with expressions
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName || ' ' || lastName", "full_name");
         * aliases.put("YEAR(CURRENT_DATE) - YEAR(birthDate)", "age");
         * String sql3 = PLC.select(aliases).from("account").sql();
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all properties of an entity class.
         * 
         * <p>This method selects all properties from an entity class, excluding those
         * marked with @Transient annotation. Properties maintain their camelCase naming.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Select all fields from Account entity
         * String sql = PLC.select(Account.class)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email, createdDate FROM account
         * 
         * // With WHERE clause
         * String sql2 = PLC.select(Account.class)
         *                  .from("account")
         *                  .where(Filters.eq("status", "active"))
         *                  .sql();
         * 
         * // Entity with @Transient fields
         * public class User {
         *     private Long id;
         *     private String userName;
         *     @Transient
         *     private String tempPassword;  // Excluded from SELECT
         * }
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with optional sub-entity properties.
         * 
         * <p>When includeSubEntityProperties is true, properties of nested entity objects
         * are also included in the selection, which is useful for fetching related data.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Entity with nested object
         * public class Order {
         *     private Long id;
         *     private String orderNumber;
         *     private Customer customer;  // Nested entity
         * }
         * 
         * // Without sub-entities
         * String sql = PLC.select(Order.class, false)
         *                 .from("orders")
         *                 .sql();
         * // Output: SELECT id, orderNumber FROM orders
         * 
         * // With sub-entities
         * String sql2 = PLC.select(Order.class, true)
         *                  .from("orders o")
         *                  .innerJoin("customers c").on("o.customerId = c.id")
         *                  .sql();
         * // Includes customer properties as well
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class with excluded properties.
         * 
         * <p>This method allows you to select most properties of an entity while
         * explicitly excluding certain ones.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Exclude sensitive fields
         * Set<String> excluded = N.asSet("password", "securityAnswer", "ssn");
         * String sql = PLC.select(Account.class, excluded)
         *                 .from("account")
         *                 .sql();
         * // All fields except password, securityAnswer, and ssn
         * 
         * // Exclude large fields for list views
         * Set<String> listExcluded = N.asSet("biography", "profileImage", "attachments");
         * String sql2 = PLC.select(Author.class, listExcluded)
         *                  .from("authors")
         *                  .sql();
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement for an entity class with all options.
         * 
         * <p>This method provides full control over property selection, including
         * sub-entity properties and exclusions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Complex entity with relationships
         * public class Invoice {
         *     private Long id;
         *     private String invoiceNumber;
         *     private Customer customer;
         *     private List<InvoiceItem> items;
         *     private byte[] pdfData;  // Large field
         * }
         * 
         * // Select with sub-entities but exclude large fields
         * Set<String> excluded = N.asSet("pdfData", "items");
         * String sql = PLC.select(Invoice.class, true, excluded)
         *                 .from("invoices i")
         *                 .innerJoin("customers c").on("i.customerId = c.id")
         *                 .sql();
         * // Includes invoice and customer fields, but not pdfData or items
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for an entity class.
         * 
         * <p>This is a convenience method that combines select() and from() operations.
         * The table name is derived from the entity class name or @Table annotation.
         * This is the most concise way to create entity-based queries.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Simple entity query
         * String sql = PLC.selectFrom(Account.class)
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email FROM account WHERE status = ?
         * 
         * // With @Table annotation
         * @Table("user_accounts")
         * public class Account { ... }
         * 
         * String sql2 = PLC.selectFrom(Account.class)
         *                  .orderBy("createdDate DESC")
         *                  .limit(10)
         *                  .sql();
         * // Output: SELECT ... FROM user_accounts ORDER BY createdDate DESC LIMIT 10
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement for an entity class with table alias.
         * 
         * <p>Table aliases are essential for joins and disambiguating column names
         * when multiple tables are involved in the query.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Simple alias usage
         * String sql = PLC.selectFrom(Account.class, "a")
         *                 .where(Filters.eq("a.status", "active"))
         *                 .sql();
         * // Output: SELECT a.id, a.firstName, a.lastName, a.email FROM account a WHERE a.status = ?
         * 
         * // With joins
         * String sql2 = PLC.selectFrom(Order.class, "o")
         *                  .innerJoin("customers c").on("o.customerId = c.id")
         *                  .where(Filters.eq("c.country", "USA"))
         *                  .sql();
         * 
         * // Self-join
         * String sql3 = PLC.selectFrom(Employee.class, "e1")
         *                  .leftJoin("employee e2").on("e1.managerId = e2.id")
         *                  .sql();
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement with optional sub-entity properties.
         * 
         * <p>This method combines entity selection with the option to include
         * properties from related entities.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Entity with relationships
         * public class BlogPost {
         *     private Long id;
         *     private String title;
         *     private Author author;
         *     private List<Comment> comments;
         * }
         * 
         * // Without sub-entities (flat selection)
         * String sql = PLC.selectFrom(BlogPost.class, false)
         *                 .sql();
         * // Output: SELECT id, title FROM blog_post
         * 
         * // With sub-entities (includes author fields)
         * String sql2 = PLC.selectFrom(BlogPost.class, true)
         *                  .sql();
         * // Includes author properties in selection
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with alias and sub-entity option.
         * 
         * <p>This method provides alias support along with sub-entity property inclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Query with related entities
         * String sql = PLC.selectFrom(Order.class, "o", true)
         *                 .innerJoin("customers c").on("o.customerId = c.id")
         *                 .innerJoin("addresses a").on("c.addressId = a.id")
         *                 .where(Filters.eq("a.country", "USA"))
         *                 .sql();
         * // Includes order and related entity properties with proper aliases
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with excluded properties.
         * 
         * <p>This method creates a complete query while excluding specific properties
         * from the selection.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Exclude large or sensitive fields
         * Set<String> excluded = N.asSet("password", "biography", "photo");
         * String sql = PLC.selectFrom(UserProfile.class, excluded)
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * 
         * // Exclude computed fields
         * Set<String> computed = N.asSet("age", "fullName", "totalSpent");
         * String sql2 = PLC.selectFrom(Customer.class, computed)
         *                  .sql();
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with alias and excluded properties.
         * 
         * <p>This method combines alias support with property exclusion for
         * precise control over the generated query.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Complex query with exclusions
         * Set<String> excluded = N.asSet("internalNotes", "debugInfo");
         * String sql = PLC.selectFrom(Order.class, "o", excluded)
         *                 .innerJoin("customers c").on("o.customerId = c.id")
         *                 .where(Filters.between("o.orderDate", startDate, endDate))
         *                 .sql();
         * 
         * // Multiple table query
         * Set<String> sensitiveFields = N.asSet("ssn", "creditCard");
         * String sql2 = PLC.selectFrom(Customer.class, "c", sensitiveFields)
         *                  .leftJoin("orders o").on("c.id = o.customerId")
         *                  .groupBy("c.id")
         *                  .sql();
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with sub-entities and exclusions.
         * 
         * <p>This method allows including sub-entity properties while excluding
         * specific properties from the main or sub-entities.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Include related entities but exclude sensitive fields
         * Set<String> excluded = N.asSet("password", "customer.creditCard");
         * String sql = PLC.selectFrom(Order.class, true, excluded)
         *                 .sql();
         * // Includes order and customer fields except the excluded ones
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM statement with all options.
         * 
         * <p>This method provides maximum flexibility for creating SELECT statements
         * with entity mapping, allowing control over aliases, sub-entity inclusion,
         * and property exclusions.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Comprehensive query example
         * Set<String> excluded = N.asSet("password", "internalNotes", "customer.creditScore");
         * String sql = PLC.selectFrom(Order.class, "o", true, excluded)
         *                 .innerJoin("customers c").on("o.customerId = c.id")
         *                 .leftJoin("shipping_addresses sa").on("o.shippingAddressId = sa.id")
         *                 .where(Filters.and(
         *                     Filters.eq("o.status", "pending"),
         *                     Filters.gt("o.totalAmount", 100)
         *                 ))
         *                 .orderBy("o.createdDate DESC")
         *                 .sql();
         * 
         * // Report query with specific field selection
         * Set<String> reportExcluded = N.asSet("id", "createdBy", "modifiedBy", "version");
         * String sql2 = PLC.selectFrom(SalesReport.class, "sr", false, reportExcluded)
         *                  .where(Filters.between("sr.reportDate", startDate, endDate))
         *                  .sql();
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of nested entity objects
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.CAMEL_CASE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for multiple entity classes (for joins).
         *
         * <p>This method is useful for selecting columns from multiple entities in a join query.
         * Each entity's properties are prefixed with the class alias in the result set.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PLC.select(Account.class, "a", "account", Order.class, "o", "order")
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .sql();
         * // Output: SELECT a.firstName AS "account.firstName", o.totalAmount AS "order.totalAmount" ... FROM account a INNER JOIN orders o ON a.id = o.accountId
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @return a new SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for multiple entity classes with exclusions.
         *
         * <p>This method allows selective property exclusion from each entity in a multi-entity SELECT,
         * useful for excluding sensitive or unnecessary fields like passwords or large blobs.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> accExcluded = N.asSet("password", "securityToken");
         * Set<String> ordExcluded = N.asSet("internalNotes");
         * String sql = PLC.select(Account.class, "a", "account", accExcluded,
         *                        Order.class, "o", "order", ordExcluded)
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .sql();
         * // Output: SELECT a.firstName AS "account.firstName", a.email AS "account.email", o.totalAmount AS "order.totalAmount" ... FROM account a INNER JOIN orders o ON a.id = o.accountId
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param excludedPropNamesA excluded properties for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @param excludedPropNamesB excluded properties for second entity
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entity selections.
         *
         * <p>This method provides the most flexible way to select from multiple entities,
         * allowing fine-grained control over each entity's selected properties through Selection objects.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = N.asList(
         *     new Selection(Account.class, "a", "account", N.asSet("firstName", "lastName")),
         *     new Selection(Order.class, "o", "order", N.asSet("totalAmount", "orderDate"))
         * );
         * String sql = PLC.select(selections)
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .sql();
         * // Output: SELECT a.firstName AS "account.firstName", a.lastName AS "account.lastName", o.totalAmount AS "order.totalAmount" ... FROM account a INNER JOIN orders o ON a.id = o.accountId
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM statement for multiple entity classes.
         *
         * <p>This convenience method combines SELECT and FROM operations for multi-entity queries,
         * automatically deriving table names from entity classes and setting up the FROM clause.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PLC.selectFrom(Account.class, "a", "account", Order.class, "o", "order")
         *                 .where(Filters.eq("a.id", "o.accountId"))
         *                 .sql();
         * // Output: SELECT a.firstName AS "account.firstName", o.totalAmount AS "order.totalAmount" ... FROM account a, orders o WHERE a.id = o.accountId
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @return a new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for multiple entity classes with exclusions.
         *
         * <p>This method combines SELECT and FROM operations while allowing property exclusions for each entity,
         * useful for excluding sensitive fields or optimizing query performance by selecting only needed columns.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> accExcluded = N.asSet("password");
         * Set<String> ordExcluded = N.asSet("internalComments");
         * String sql = PLC.selectFrom(Account.class, "a", "account", accExcluded,
         *                            Order.class, "o", "order", ordExcluded)
         *                 .where(Filters.eq("a.id", "o.accountId"))
         *                 .sql();
         * // Output: SELECT a.firstName AS "account.firstName", a.email AS "account.email", o.totalAmount AS "order.totalAmount" ... FROM account a, orders o WHERE a.id = o.accountId
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA property prefix for first entity
         * @param excludedPropNamesA excluded properties for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB property prefix for second entity
         * @param excludedPropNamesB excluded properties for second entity
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement for multiple entity selections.
         *
         * <p>This method provides maximum flexibility for multi-entity queries by accepting Selection objects
         * that define exactly what properties to select from each entity, along with automatic FROM clause generation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = N.asList(
         *     new Selection(Account.class, "a", "account", N.asSet("firstName", "email")),
         *     new Selection(Order.class, "o", "order", N.asSet("totalAmount"))
         * );
         * String sql = PLC.selectFrom(selections)
         *                 .where(Filters.eq("a.id", "o.accountId"))
         *                 .sql();
         * // Output: SELECT a.firstName AS "account.firstName", a.email AS "account.email", o.totalAmount AS "order.totalAmount" FROM account a, orders o WHERE a.id = o.accountId
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for a table.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PLC.count("account")
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE status = ?
         * }</pre>
         *
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>The table name is derived from the entity class name or @Table annotation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = PLC.count(Account.class)
         *                 .where(Filters.isNotNull("email"))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE email IS NOT NULL
         * }</pre>
         *
         * @param entityClass the entity class to count
         * @return a new SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class mapping.
         * 
         * <p>This method is useful for generating just the WHERE clause portion of a query
         * with proper property-to-column name mapping.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("firstName", "John"),
         *     Filters.like("emailAddress", "%@example.com")
         * );
         * 
         * String sql = PLC.parse(cond, Account.class).sql();
         * // Output: firstName = ? AND emailAddress LIKE ?
         * }</pre>
         * 
         * @param cond the condition to parse
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance containing just the condition SQL
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with {@code NamingPolicy.NO_CHANGE} field/column naming strategy.
     * 
     * <p>This builder generates SQL with named parameters (e.g., :paramName) and preserves the original
     * naming convention of fields and columns without any transformation. It's particularly useful when
     * working with databases where column names match exactly with your Java field names.</p>
     *
     * <p>For example:</p>
     * <pre>{@code
     * N.println(NSB.select("first_Name", "last_NaMe").from("account").where(Filters.eq("last_NaMe", 1)).sql());
     * // SELECT first_Name, last_NaMe FROM account WHERE last_NaMe = :last_NaMe
     * }</pre>
     */
    public static class NSB extends SQLBuilder {

        /**
         * Constructs a new NSB instance with NO_CHANGE naming policy and named SQL policy.
         * This constructor is package-private and used internally by the builder pattern.
         */
        NSB() {
            super(NamingPolicy.NO_CHANGE, SQLPolicy.NAMED_SQL);
        }

        /**
         * Indicates whether this builder generates named SQL parameters.
         * 
         * <p>This implementation always returns {@code true} as NSB generates SQL with named parameters
         * (e.g., :paramName) instead of positional parameters (?).</p>
         * 
         * @return true, indicating this builder uses named SQL parameters
         */
        @Override
        protected boolean isNamedSql() {
            return true;
        }

        /**
         * Creates a new instance of NSB for internal use by the builder pattern.
         * 
         * <p>This factory method is used internally to create new builder instances
         * when starting a new SQL construction chain.</p>
         * 
         * @return a new NSB instance
         */
        protected static NSB createInstance() {
            return new NSB();
        }

        /**
         * Creates an INSERT SQL builder with a single column expression.
         * 
         * <p>This method is a convenience wrapper that internally calls {@link #insert(String...)} 
         * with a single-element array.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.insert("user_name").into("users").sql();
         * // INSERT INTO users (user_name) VALUES (:user_name)
         * }</pre>
         *
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder with specified column names.
         * 
         * <p>The generated SQL will include placeholders for the specified columns using named parameters.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.insert("first_name", "last_name", "email")
         *                 .into("users")
         *                 .sql();
         * // INSERT INTO users (first_name, last_name, email) VALUES (:first_name, :last_name, :email)
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to include in the INSERT statement
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with a collection of column names.
         * 
         * <p>This method allows using any Collection implementation (List, Set, etc.) to specify
         * the columns for the INSERT statement.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("id", "name", "created_date");
         * String sql = NSB.insert(columns).into("products").sql();
         * // INSERT INTO products (id, name, created_date) VALUES (:id, :name, :created_date)
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to include
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from a map of column names to values.
         * 
         * <p>The map keys represent column names and the values are the corresponding values to insert.
         * This method is useful when you have dynamic column-value pairs.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("username", "john_doe");
         * data.put("age", 25);
         * String sql = NSB.insert(data).into("users").sql();
         * // INSERT INTO users (username, age) VALUES (:username, :age)
         * }</pre>
         *
         * @param props map of column names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object.
         * 
         * <p>This method extracts all non-null properties from the entity object to create the INSERT statement.
         * Properties annotated with {@code @Transient}, {@code @ReadOnly}, or {@code @ReadOnlyId} are automatically excluded.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User();
         * user.setName("John");
         * user.setEmail("john@example.com");
         * String sql = NSB.insert(user).into("users").sql();
         * // INSERT INTO users (name, email) VALUES (:name, :email)
         * }</pre>
         *
         * @param entity the entity object containing data to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object with excluded properties.
         * 
         * <p>This method allows fine-grained control over which properties to exclude from the INSERT statement,
         * in addition to the automatically excluded annotated properties.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User();
         * user.setName("John");
         * user.setEmail("john@example.com");
         * user.setPassword("secret");
         * Set<String> exclude = N.asSet("password");
         * String sql = NSB.insert(user, exclude).into("users").sql();
         * // INSERT INTO users (name, email) VALUES (:name, :email)
         * }</pre>
         *
         * @param entity the entity object containing data to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for a specific entity class.
         * 
         * <p>This method generates an INSERT template for all insertable properties of the entity class.
         * Properties are determined by the class structure and annotations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.insert(User.class).into("users").sql();
         * // INSERT INTO users (id, name, email, created_date) VALUES (:id, :name, :email, :created_date)
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for a specific entity class with excluded properties.
         * 
         * <p>This method provides control over which properties to include in the INSERT statement
         * when generating SQL from a class definition.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("id", "createdDate");
         * String sql = NSB.insert(User.class, exclude).into("users").sql();
         * // INSERT INTO users (name, email) VALUES (:name, :email)
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder with automatic table name detection.
         * 
         * <p>The table name is automatically determined from the entity class using the {@code @Table} annotation
         * or by converting the class name according to the naming policy.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * @Table("user_accounts")
         * class User { ... }
         * 
         * String sql = NSB.insertInto(User.class).sql();
         * // INSERT INTO user_accounts (id, name, email) VALUES (:id, :name, :email)
         * }</pre>
         *
         * @param entityClass the entity class for INSERT operation
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder with automatic table name detection and excluded properties.
         * 
         * <p>Combines automatic table name detection with the ability to exclude specific properties
         * from the INSERT statement.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("version", "lastModified");
         * String sql = NSB.insertInto(User.class, exclude).sql();
         * // INSERT INTO users (id, name, email) VALUES (:id, :name, :email)
         * }</pre>
         *
         * @param entityClass the entity class for INSERT operation
         * @param excludedPropNames set of property names to exclude
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple records (MySQL style).
         * 
         * <p>This method generates a single INSERT statement with multiple value sets, which is more efficient
         * than multiple individual INSERT statements. The input can be a collection of entity objects or maps.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "john@email.com"),
         *     new User("Jane", "jane@email.com")
         * );
         * String sql = NSB.batchInsert(users).into("users").sql();
         * // INSERT INTO users (name, email) VALUES (:name_0, :email_0), (:name_1, :email_1)
         * }</pre>
         *
         * @param propsList collection of entities or property maps to insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * <p>
         * <b>Note:</b> This is a beta feature and may be subject to changes
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * <p>This method starts building an UPDATE statement for the given table. You must call
         * {@code set()} methods to specify which columns to update.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.update("users")
         *                 .set("last_login", "status")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // UPDATE users SET last_login = :last_login, status = :status WHERE id = :id
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with entity class mapping.
         * 
         * <p>This method allows specifying both the table name and entity class, which enables
         * proper property-to-column name mapping based on the entity's annotations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.update("user_accounts", User.class)
         *                 .set("lastLogin", "active")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // UPDATE user_accounts SET last_login = :lastLogin, active = :active WHERE id = :id
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null/empty or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class with automatic table name detection.
         * 
         * <p>The table name is derived from the entity class, and all updatable properties
         * (excluding those marked with {@code @NonUpdatable}, {@code @ReadOnly}, etc.) are included.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.update(User.class)
         *                 .set("name", "email")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // UPDATE users SET name = :name, email = :email WHERE id = :id
         * }</pre>
         *
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class with excluded properties.
         * 
         * <p>This method automatically determines updatable properties from the entity class
         * while allowing additional properties to be excluded from the UPDATE.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("password", "createdDate");
         * String sql = NSB.update(User.class, exclude)
         *                 .set("name", "email")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // UPDATE users SET name = :name, email = :email WHERE id = :id
         * }</pre>
         *
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table.
         * 
         * <p>This method initiates a DELETE statement. Typically, you'll want to add WHERE conditions
         * to avoid deleting all records in the table.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.deleteFrom("users")
         *                 .where(Filters.eq("status", "inactive"))
         *                 .sql();
         * // DELETE FROM users WHERE status = :status
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for a table with entity class mapping.
         * 
         * <p>This method enables proper property-to-column name mapping when building WHERE conditions
         * for the DELETE statement.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.deleteFrom("user_accounts", User.class)
         *                 .where(Filters.lt("lastLogin", someDate))
         *                 .sql();
         * // DELETE FROM user_accounts WHERE last_login < :lastLogin
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null/empty or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for an entity class with automatic table name detection.
         * 
         * <p>The table name is derived from the entity class using {@code @Table} annotation
         * or naming policy conversion.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.deleteFrom(User.class)
         *                 .where(Filters.eq("id", 123))
         *                 .sql();
         * // DELETE FROM users WHERE id = :id
         * }</pre>
         *
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single column or expression.
         * 
         * <p>The selectPart parameter can be a simple column name or a complex SQL expression.
         * This method is useful for selecting computed values or using SQL functions.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.select("COUNT(*) AS total").from("users").sql();
         * // SELECT COUNT(*) AS total FROM users
         * 
         * String sql2 = NSB.select("MAX(salary) - MIN(salary) AS salary_range")
         *                  .from("employees")
         *                  .sql();
         * // SELECT MAX(salary) - MIN(salary) AS salary_range FROM employees
         * }</pre>
         *
         * @param selectPart the column name or SQL expression to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder with multiple columns.
         * 
         * <p>Each string in the array represents a column to select. The columns will be
         * included in the SELECT clause in the order specified.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.select("id", "name", "email", "created_date")
         *                 .from("users")
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * // SELECT id, name, email, created_date FROM users WHERE active = :active
         * }</pre>
         *
         * @param propOrColumnNames array of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a collection of columns.
         * 
         * <p>This method allows using any Collection implementation to specify the columns
         * to select, providing flexibility in how column lists are constructed.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = getRequiredColumns();   // Dynamic column list
         * String sql = NSB.select(columns)
         *                 .from("products")
         *                 .where(Filters.gt("price", 100))
         *                 .sql();
         * // SELECT column1, column2, ... FROM products WHERE price > :price
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * 
         * <p>The map keys represent the column names or expressions, and the values are their aliases.
         * This is useful for renaming columns in the result set.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new LinkedHashMap<>();
         * aliases.put("u.first_name", "firstName");
         * aliases.put("u.last_name", "lastName");
         * aliases.put("COUNT(o.id)", "orderCount");
         * 
         * String sql = NSB.select(aliases)
         *                 .from("users u")
         *                 .leftJoin("orders o").on("u.id = o.user_id")
         *                 .groupBy("u.id")
         *                 .sql();
         * // SELECT u.first_name AS firstName, u.last_name AS lastName, COUNT(o.id) AS orderCount
         * // FROM users u LEFT JOIN orders o ON u.id = o.user_id GROUP BY u.id
         * }</pre>
         *
         * @param propOrColumnNameAliases map of column names/expressions to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class.
         * 
         * <p>This method selects all properties from the entity class that are not marked
         * as transient. Sub-entity properties are not included by default.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // If User class has properties: id, name, email, address
         * String sql = NSB.select(User.class).from("users").sql();
         * // SELECT id, name, email, address FROM users
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with optional sub-entity properties.
         * 
         * <p>When includeSubEntityProperties is true, properties of nested entity types are also included
         * in the selection, which is useful for fetching related data in a single query.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // If User has an Address sub-entity
         * String sql = NSB.select(User.class, true)
         *                 .from("users u")
         *                 .leftJoin("addresses a").on("u.address_id = a.id")
         *                 .sql();
         * // SELECT u.id, u.name, u.email, a.street, a.city, a.zip FROM users u
         * // LEFT JOIN addresses a ON u.address_id = a.id
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties from nested entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with excluded properties.
         * 
         * <p>This method allows selecting most properties from an entity while excluding specific ones,
         * which is useful when you want to omit large fields like BLOBs or sensitive data.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("password", "profilePicture");
         * String sql = NSB.select(User.class, exclude).from("users").sql();
         * // SELECT id, name, email, created_date FROM users
         * // (assuming User has id, name, email, created_date, password, and profilePicture)
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder with full control over entity property selection.
         * 
         * <p>This method combines the ability to include sub-entity properties and exclude specific
         * properties, providing maximum flexibility in constructing SELECT statements.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("user.password", "address.coordinates");
         * String sql = NSB.select(User.class, true, exclude)
         *                 .from("users u")
         *                 .leftJoin("addresses a").on("u.address_id = a.id")
         *                 .sql();
         * // Selects all User and Address properties except password and coordinates
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties from nested entities
         * @param excludedPropNames set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for an entity class.
         * 
         * <p>This is a convenience method that combines select() and from() operations.
         * The table name is automatically derived from the entity class.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.selectFrom(User.class).where(Filters.eq("active", true)).sql();
         * // SELECT id, name, email FROM users WHERE active = :active
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with a table alias.
         * 
         * <p>This method allows specifying a table alias for use in joins and qualified column references.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.selectFrom(User.class, "u")
         *                 .leftJoin("orders o").on("u.id = o.user_id")
         *                 .where(Filters.isNotNull("o.id"))
         *                 .sql();
         * // SELECT u.id, u.name, u.email FROM users u
         * // LEFT JOIN orders o ON u.id = o.user_id WHERE o.id IS NOT NULL
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT...FROM SQL builder with sub-entity property inclusion.
         * 
         * <p>When includeSubEntityProperties is true, the method automatically handles joining
         * related tables for nested entities.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Automatically includes joins for sub-entities
         * String sql = NSB.selectFrom(Order.class, true).sql();
         * // May generate: SELECT o.*, c.*, p.* FROM orders o
         * // LEFT JOIN customers c ON o.customer_id = c.id
         * // LEFT JOIN products p ON o.product_id = p.id
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include nested entity properties
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT...FROM SQL builder with alias and sub-entity control.
         * 
         * <p>Combines table aliasing with sub-entity property inclusion for complex queries.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.selectFrom(Order.class, "o", true)
         *                 .where(Filters.gt("o.total", 1000))
         *                 .sql();
         * // Generates SELECT with proper aliases for main and sub-entities
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include nested entity properties
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT...FROM SQL builder with property exclusion.
         * 
         * <p>This convenience method combines selecting specific properties and setting the FROM clause.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("largeData", "internalNotes");
         * String sql = NSB.selectFrom(User.class, exclude).sql();
         * // SELECT id, name, email FROM users (excluding largeData and internalNotes)
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT...FROM SQL builder with alias and property exclusion.
         * 
         * <p>Provides aliasing capability while excluding specific properties from selection.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("password");
         * String sql = NSB.selectFrom(User.class, "u", exclude)
         *                 .join("profiles p").on("u.id = p.user_id")
         *                 .sql();
         * // SELECT u.id, u.name, u.email FROM users u JOIN profiles p ON u.id = p.user_id
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT...FROM SQL builder with sub-entities and exclusions.
         * 
         * <p>This method automatically handles complex FROM clauses when sub-entities are included.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("customer.creditCard");
         * String sql = NSB.selectFrom(Order.class, true, exclude).sql();
         * // Selects Order with Customer sub-entity but excludes creditCard field
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include nested entity properties
         * @param excludedPropNames properties to exclude
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a fully-configured SELECT...FROM SQL builder.
         * 
         * <p>This is the most comprehensive selectFrom method, providing full control over
         * aliasing, sub-entity inclusion, and property exclusion.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = N.asSet("audit.details", "customer.internalNotes");
         * String sql = NSB.selectFrom(Order.class, "o", true, exclude)
         *                 .where(Filters.between("o.orderDate", startDate, endDate))
         *                 .orderBy("o.orderDate DESC")
         *                 .sql();
         * // Complex SELECT with multiple tables, aliases, and exclusions
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias
         * @param includeSubEntityProperties whether to include nested entity properties
         * @param excludedPropNames properties to exclude from selection
         * @return a new SQLBuilder instance with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases.
         * 
         * <p>This method is designed for queries that need to select from multiple tables with
         * proper aliasing for both table names and result set column prefixes.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.select(User.class, "u", "user_", Order.class, "o", "order_")
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .sql();
         * // SELECT u.id AS user_id, u.name AS user_name, o.id AS order_id, o.total AS order_total
         * // FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA column prefix for first entity in results
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB column prefix for second entity in results
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases and exclusions.
         * 
         * <p>This method provides full control over selecting from two entities, including the ability
         * to exclude specific properties from each entity.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludeUser = N.asSet("password");
         * Set<String> excludeOrder = N.asSet("internalNotes");
         * 
         * String sql = NSB.select(User.class, "u", "user_", excludeUser,
         *                        Order.class, "o", "order_", excludeOrder)
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .sql();
         * // Selects all fields except excluded ones with proper prefixes
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA column prefix for first entity
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB column prefix for second entity
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * 
         * <p>This is the most flexible method for multi-table selections, accepting a list of
         * Selection objects that define how each entity should be selected.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user_", null, false, null),
         *     new Selection(Order.class, "o", "order_", null, true, N.asSet("notes")),
         *     new Selection(Product.class, "p", "product_", null, false, null)
         * );
         * 
         * String sql = NSB.select(selections)
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .join("products p").on("o.product_id = p.id")
         *                 .sql();
         * // Complex multi-table SELECT with different configurations per table
         * }</pre>
         *
         * @param multiSelects list of Selection configurations
         * @return a new SQLBuilder instance configured for multi-table SELECT
         * @throws IllegalArgumentException if multiSelects is invalid
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for two entities.
         * 
         * <p>This convenience method combines select() and from() for two-table queries.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.selectFrom(User.class, "u", "user_",
         *                            Order.class, "o", "order_")
         *                 .where(Filters.eq("u.id", 123))
         *                 .sql();
         * // Automatically generates FROM clause with proper joins
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA column prefix for first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB column prefix for second entity
         * @return a new SQLBuilder with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for two entities with exclusions.
         * 
         * <p>This method automatically generates the appropriate FROM clause based on the
         * provided entity classes and their relationships.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.selectFrom(Customer.class, "c", "cust_", N.asSet("password"),
         *                            Account.class, "a", "acct_", N.asSet("pin"))
         *                 .sql();
         * // Generates complete SELECT...FROM with exclusions
         * }</pre>
         *
         * @param entityClassA first entity class
         * @param tableAliasA table alias for first entity
         * @param classAliasA column prefix for first entity
         * @param excludedPropNamesA properties to exclude from first entity
         * @param entityClassB second entity class
         * @param tableAliasB table alias for second entity
         * @param classAliasB column prefix for second entity
         * @param excludedPropNamesB properties to exclude from second entity
         * @return a new SQLBuilder with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for multiple entities.
         * 
         * <p>This method automatically generates the FROM clause based on the Selection
         * configurations, handling complex multi-table queries.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = createSelectionList();
         * String sql = NSB.selectFrom(selections)
         *                 .where(Filters.gt("o.amount", 100))
         *                 .orderBy("o.date DESC")
         *                 .sql();
         * // Automatically generates complete multi-table query
         * }</pre>
         *
         * @param multiSelects list of Selection configurations
         * @return a new SQLBuilder with SELECT and FROM clauses configured
         * @throws IllegalArgumentException if multiSelects is invalid
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for the specified table.
         * 
         * <p>This is a convenience method for creating count queries.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.count("users").where(Filters.eq("active", true)).sql();
         * // SELECT count(*) FROM users WHERE active = :active
         * }</pre>
         *
         * @param tableName the table to count records from
         * @return a new SQLBuilder configured for COUNT query
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>The table name is automatically derived from the entity class.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSB.count(User.class)
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // SELECT count(*) FROM users WHERE status = :status
         * }</pre>
         *
         * @param entityClass the entity class to count
         * @return a new SQLBuilder configured for COUNT query
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class mapping.
         * 
         * <p>This method is useful for generating just the SQL representation of a condition,
         * without the full query structure. It's primarily used for debugging or building
         * dynamic query parts.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("status", "active"),
         *     Filters.gt("age", 18)
         * );
         * String sql = NSB.parse(cond, User.class).sql();
         * // status = :status AND age > :age
         * }</pre>
         *
         * @param cond the condition to parse
         * @param entityClass the entity class for property name mapping
         * @return a new SQLBuilder containing only the condition SQL
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with snake_case (lower case with underscore) field/column naming strategy.
     * 
     * <p>This builder generates SQL statements using named parameters (e.g., :paramName) instead of 
     * positional parameters (?), and converts property names from camelCase to snake_case for column names.</p>
     * 
     * <p>Named parameters are useful for:</p>
     * <ul>
     *   <li>Better readability of generated SQL</li>
     *   <li>Reusing the same parameter multiple times in a query</li>
     *   <li>Integration with frameworks that support named parameters</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple SELECT with named parameters
     * String sql = NSC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(Filters.eq("id", 1))
     *                 .sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = :id
     * 
     * // INSERT with entity - generates named parameters
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = NSC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (first_name, last_name) VALUES (:firstName, :lastName)
     * }</pre>
     */
    public static class NSC extends SQLBuilder {

        /**
         * Constructs a new NSC instance with snake_case naming policy and named SQL policy.
         * 
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        NSC() {
            super(NamingPolicy.SNAKE_CASE, SQLPolicy.NAMED_SQL);
        }

        /**
         * Checks if this builder generates named SQL (with :paramName syntax).
         * 
         * @return always returns {@code true} for NSC builder
         */
        @Override
        protected boolean isNamedSql() {
            return true;
        }

        /**
         * Creates a new instance of NSC.
         * 
         * <p>This factory method is used internally by the static methods to create new builder instances.
         * Each SQL building operation starts with a fresh instance to ensure thread safety.</p>
         * 
         * @return a new NSC instance
         */
        protected static NSC createInstance() {
            return new NSC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.insert("name").into("users").sql();
         * // Output: INSERT INTO users (name) VALUES (:name)
         * }</pre>
         * 
         * @param expr the column name or expression to insert
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for multiple columns with named parameters.
         * 
         * <p>Each column will have a corresponding named parameter in the VALUES clause.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.insert("firstName", "lastName", "email")
         *                 .into("users")
         *                 .sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for a collection of columns with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = NSC.insert(columns).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param propOrColumnNames the collection of property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from a map of column-value pairs with named parameters.
         * 
         * <p>The map keys become both column names and parameter names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * String sql = NSC.insert(props).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param props the map of property names to values
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder from an entity object with named parameters.
         * 
         * <p>Property names from the entity become named parameters in the SQL.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User();
         * user.setFirstName("John");
         * user.setLastName("Doe");
         * String sql = NSC.insert(user).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder from an entity object with excluded properties and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User();
         * user.setFirstName("John");
         * user.setLastName("Doe");
         * user.setCreatedDate(new Date());
         * 
         * Set<String> excluded = Set.of("createdDate");
         * String sql = NSC.insert(user, excluded).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for an entity class with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.insert(User.class).into("users").sql();
         * // Output: INSERT INTO users (id, first_name, last_name, email) VALUES (:id, :firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class to base the INSERT on
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for an entity class with excluded properties and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("id", "createdDate");
         * String sql = NSC.insert(User.class, excluded).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class to base the INSERT on
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with named parameters
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class with named parameters.
         * 
         * <p>This is a convenience method that automatically determines the table name from the entity class.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * @Table("users")
         * public class User { ... }
         * 
         * String sql = NSC.insertInto(User.class).sql();
         * // Output: INSERT INTO users (id, first_name, last_name, email) VALUES (:id, :firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class to insert into
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for an entity class with excluded properties and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("id");
         * String sql = NSC.insertInto(User.class, excluded).sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class to insert into
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder with named parameters in MySQL style.
         * 
         * <p>Note: Named parameters in batch inserts may have limited support depending on the database driver.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * String sql = NSC.batchInsert(users).into("users").sql();
         * // Output format depends on the implementation
         * }</pre>
         * 
         * @param propsList list of entities or property maps to insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * <p>
         * <b>Note:</b> This API is in beta and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.update("users")
         *                 .set("firstName", "John")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = :firstName WHERE id = :id
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation with named parameters
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for a table with entity class context and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.update("users", User.class)
         *                 .set("firstName", "John")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = :firstName WHERE id = :id
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation with named parameters
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for an entity class with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.update(User.class)
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = :firstName, last_name = :lastName WHERE id = :id
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation with named parameters
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for an entity class with excluded properties and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("createdDate", "createdBy");
         * String sql = NSC.update(User.class, excluded)
         *                 .set("firstName", "John")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = :firstName WHERE id = :id
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @param excludedPropNames the set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation with named parameters
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.deleteFrom("users")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM users WHERE id = :id
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation with named parameters
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for a table with entity class context and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.deleteFrom("users", User.class)
         *                 .where(Filters.eq("firstName", "John"))
         *                 .sql();
         * // Output: DELETE FROM users WHERE first_name = :firstName
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation with named parameters
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for an entity class with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.deleteFrom(User.class)
         *                 .where(Filters.eq("firstName", "John"))
         *                 .sql();
         * // Output: DELETE FROM users WHERE first_name = :firstName
         * }</pre>
         * 
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation with named parameters
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single column or expression using named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.select("COUNT(*)").from("users").where(Filters.eq("active", true)).sql();
         * // Output: SELECT count(*) FROM users WHERE active = :active
         * }</pre>
         * 
         * @param selectPart the column name or SQL expression to select
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder with multiple columns using named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.select("firstName", "lastName", "email")
         *                 .from("users")
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", email FROM users WHERE active = :active
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a collection of columns using named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = NSC.select(columns).from("users").sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", email FROM users
         * }</pre>
         * 
         * @param propOrColumnNames the collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases using named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = NSC.select(aliases).from("users").sql();
         * // Output: SELECT first_name AS fname, last_name AS lname FROM users
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of an entity class with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.select(User.class).from("users").where(Filters.eq("active", true)).sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM users WHERE active = :active
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with sub-entity option and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.select(User.class, true).from("users").sql();
         * // Includes properties from User and any embedded entities
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with excluded properties and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password", "secretKey");
         * String sql = NSC.select(User.class, excluded).from("users").sql();
         * // Selects all User properties except password and secretKey
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for an entity class with all options and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.select(User.class, true, excluded)
         *                 .from("users")
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * // Output uses named parameter :active
         * }</pre>
         * 
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with named parameters
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with named parameters.
         * 
         * <p>This is a convenience method that combines SELECT and FROM operations.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class).where(Filters.eq("id", 1)).sql();
         * // Output: SELECT id, first_name AS "firstName", last_name AS "lastName" FROM users WHERE id = :id
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for an entity class with table alias and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class, "u")
         *                 .where(Filters.eq("u.active", true))
         *                 .sql();
         * // Output uses named parameter :active
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity option and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class, true).sql();
         * // Includes properties from User and any embedded entities with automatic joins
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and sub-entity option using named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class, "u", true).sql();
         * // Includes properties from User and embedded entities with table alias
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with excluded properties and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.selectFrom(User.class, excluded).where(Filters.eq("active", true)).sql();
         * // Selects all properties except password, uses :active parameter
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with alias and excluded properties using named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.selectFrom(User.class, "u", excluded).sql();
         * // Selects all properties except password with table alias
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity and exclusion options using named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.selectFrom(User.class, true, excluded).sql();
         * // Selects all properties including sub-entities except password
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with all options and named parameters.
         * 
         * <p>This is the most flexible selectFrom method.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password");
         * String sql = NSC.selectFrom(User.class, "u", true, excluded)
         *                 .where(Filters.and(
         *                     Filters.eq("u.active", true),
         *                     Filters.like("u.email", "%@example.com")
         *                 ))
         *                 .sql();
         * // Complex select with alias, sub-entities, exclusions, and named parameters
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties true to include properties of embedded entities
         * @param excludedPropNames the set of property names to exclude from selection
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.SNAKE_CASE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity classes with named parameters.
         * 
         * <p>Used for multi-table queries with joins.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.select(User.class, "u", "user", Order.class, "o", "order")
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .where(Filters.eq("u.active", true))
         *                 .sql();
         * // Uses named parameter :active
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for property prefixing of the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for property prefixing of the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity classes with exclusions and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclusions = Set.of("password");
         * Set<String> orderExclusions = Set.of("internalNotes");
         * 
         * String sql = NSC.select(User.class, "u", "user", userExclusions,
         *                        Order.class, "o", "order", orderExclusions)
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .where(Filters.eq("u.active", true))
         *                 .sql();
         * // Uses named parameters for conditions
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for property prefixing of the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for property prefixing of the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, Set.of("description"))
         * );
         * String sql = NSC.select(selections)
         *                 .from("users u")
         *                 .where(Filters.eq("u.status", "ACTIVE"))
         *                 .sql();
         * // Uses named parameter :status
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining the entities to select
         * @return a new SQLBuilder instance configured for multi-table SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity classes with named parameters.
         * 
         * <p>Automatically generates the FROM clause based on entity classes.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.selectFrom(User.class, "u", "user", Order.class, "o", "order")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .where(Filters.gt("o.amount", 100))
         *                 .sql();
         * // Uses named parameter :amount
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for property prefixing of the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for property prefixing of the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT with FROM clause
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entities with exclusions and named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclusions = Set.of("password");
         * Set<String> orderExclusions = Set.of("internalNotes");
         * 
         * String sql = NSC.selectFrom(User.class, "u", "user", userExclusions,
         *                            Order.class, "o", "order", orderExclusions)
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .where(Filters.between("o.orderDate", startDate, endDate))
         *                 .sql();
         * // Uses named parameters :startDate and :endDate
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias for property prefixing of the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias for property prefixing of the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-table SELECT with FROM clause
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple selections with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null)
         * );
         * String sql = NSC.selectFrom(selections)
         *                 .where(Filters.in("u.id", Arrays.asList(1, 2, 3)))
         *                 .sql();
         * // Uses named parameters for the IN clause
         * }</pre>
         * 
         * @param multiSelects list of Selection objects defining the entities to select
         * @return a new SQLBuilder instance configured for multi-table SELECT with FROM clause
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.SNAKE_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for a table with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.count("users").where(Filters.eq("active", true)).sql();
         * // Output: SELECT count(*) FROM users WHERE active = :active
         * }</pre>
         * 
         * @param tableName the name of the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for an entity class with named parameters.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NSC.count(User.class)
         *                 .where(Filters.and(
         *                     Filters.eq("firstName", "John"),
         *                     Filters.gt("age", 18)
         *                 ))
         *                 .sql();
         * // Output: SELECT count(*) FROM users WHERE first_name = :firstName AND age = :age
         * }</pre>
         * 
         * @param entityClass the entity class to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context and named parameters.
         * 
         * <p>This method generates just the condition part of SQL with named parameters.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("firstName", "John"),
         *     Filters.gt("age", 18),
         *     Filters.like("email", "%@example.com")
         * );
         * String sql = NSC.parse(cond, User.class).sql();
         * // Output: first_name = :firstName AND age = :age AND email LIKE :email
         * }</pre>
         * 
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for property-to-column mapping
         * @return a new SQLBuilder instance containing only the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with all capital case (upper case with underscore) field/column naming strategy.
     * This builder generates SQL with named parameters (e.g., :paramName) and converts property names
     * to SCREAMING_SNAKE_CASE format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple SELECT with named parameters
     * N.println(NAC.select("firstName", "lastName").from("account").where(Filters.eq("id", 1)).sql());
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = :id
     * 
     * // INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = NAC.insert(account).into("ACCOUNT").sql();
     * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (:firstName, :lastName)
     * }</pre>
     */
    public static class NAC extends SQLBuilder {

        /**
         * Constructs a new NAC instance with SCREAMING_SNAKE_CASE naming policy and named SQL policy.
         * 
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        NAC() {
            super(NamingPolicy.SCREAMING_SNAKE_CASE, SQLPolicy.NAMED_SQL);
        }

        /**
         * Indicates whether this builder generates named SQL parameters.
         * 
         * @return always returns {@code true} for NAC
         */
        @Override
        protected boolean isNamedSql() {
            return true;
        }

        /**
         * Creates a new instance of NAC.
         * 
         * <p>This factory method is used internally by the static methods to create new builder instances.
         * Each SQL building operation starts with a fresh instance to ensure thread safety.</p>
         * 
         * @return a new NAC instance
         */
        protected static NAC createInstance() {
            return new NAC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.insert("FIRST_NAME").into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME) VALUES (:FIRST_NAME)
         * }</pre>
         * 
         * @param expr the column expression to insert
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified property or column names.
         * Property names will be converted to SCREAMING_SNAKE_CASE format.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.insert("firstName", "lastName").into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified collection of property or column names.
         * Property names will be converted to SCREAMING_SNAKE_CASE format.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = NAC.insert(columns).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param propOrColumnNames the collection of property or column names to insert
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified property-value map.
         * Property names will be converted to SCREAMING_SNAKE_CASE format.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> props = Map.of("firstName", "John", "lastName", "Doe", "age", 30);
         * String sql = NAC.insert(props).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, AGE) VALUES (:firstName, :lastName, :age)
         * }</pre>
         * 
         * @param props the map of property names to values
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity object.
         * The entity's properties will be extracted and used for the INSERT statement.
         * Properties marked with @ReadOnly, @ReadOnlyId, or @Transient will be excluded.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * String sql = NAC.insert(account).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity object with excluded properties.
         * The entity's properties will be extracted and used for the INSERT statement,
         * excluding the specified property names.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setCreatedTime(new Date());
         * String sql = NAC.insert(account, Set.of("createdTime")).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class.
         * All insertable properties of the class will be included.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.insert(Account.class).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (ID, FIRST_NAME, LAST_NAME, EMAIL) VALUES (:id, :firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with excluded properties.
         * All insertable properties of the class will be included except those specified.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.insert(Account.class, Set.of("id", "createdTime")).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * @Table("USER_ACCOUNT")
         * class Account { ... }
         * 
         * String sql = NAC.insertInto(Account.class).sql();
         * // Output: INSERT INTO USER_ACCOUNT (ID, FIRST_NAME, LAST_NAME) VALUES (:id, :firstName, :lastName)
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for the specified entity class with excluded properties.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.insertInto(Account.class, Set.of("id")).sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for MySQL-style batch inserts.
         * Generates a single INSERT statement with multiple value rows.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account acc1 = new Account("John", "Doe");
         * Account acc2 = new Account("Jane", "Smith");
         * List<Account> accounts = Arrays.asList(acc1, acc2);
         * String sql = NAC.batchInsert(accounts).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES 
         * //         (:firstName_1, :lastName_1), (:firstName_2, :lastName_2)
         * }</pre>
         * 
         * @param propsList collection of entities or property maps to insert
         * @return an SQLBuilder configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * <p>
         * <b>Note:</b> This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.update("ACCOUNT")
         *                 .set("STATUS", "ACTIVE")
         *                 .where(Filters.eq("ID", 1))
         *                 .sql();
         * // Output: UPDATE ACCOUNT SET STATUS = :STATUS WHERE ID = :id
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return an SQLBuilder configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class mapping.
         * The entity class provides property-to-column mapping information.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.update("ACCOUNT", Account.class)
         *                 .set("status", "lastModified")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE ACCOUNT SET STATUS = :status, LAST_MODIFIED = :lastModified WHERE ID = :id
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return an SQLBuilder configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null/empty or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * All updatable properties will be included.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.update(Account.class)
         *                 .set("status", "lastModified")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE ACCOUNT SET STATUS = :status, LAST_MODIFIED = :lastModified WHERE ID = :id
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class with excluded properties.
         * The table name will be derived from the entity class name or @Table annotation.
         * Properties marked with @NonUpdatable or in the excluded set will be omitted.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.update(Account.class, Set.of("createdTime"))
         *                 .set("status", "lastModified")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE ACCOUNT SET STATUS = :status, LAST_MODIFIED = :lastModified WHERE ID = :id
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the UPDATE
         * @return an SQLBuilder configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.deleteFrom("ACCOUNT")
         *                 .where(Filters.eq("STATUS", "INACTIVE"))
         *                 .sql();
         * // Output: DELETE FROM ACCOUNT WHERE STATUS = :STATUS
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return an SQLBuilder configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table with entity class mapping.
         * The entity class provides property-to-column mapping information for conditions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.deleteFrom("ACCOUNT", Account.class)
         *                 .where(Filters.eq("status", "INACTIVE"))
         *                 .sql();
         * // Output: DELETE FROM ACCOUNT WHERE STATUS = :status
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return an SQLBuilder configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null/empty or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.deleteFrom(Account.class)
         *                 .where(Filters.and(Filters.eq("status", "INACTIVE"), Filters.lt("lastLogin", yesterday)))
         *                 .sql();
         * // Output: DELETE FROM ACCOUNT WHERE STATUS = :status AND LAST_LOGIN < :lastLogin
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single select expression.
         * The expression can be a column name, function call, or any valid SQL expression.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.select("COUNT(*)").from("ACCOUNT").sql();
         * // Output: SELECT count(*) FROM ACCOUNT
         * 
         * String sql2 = NAC.select("MAX(BALANCE)").from("ACCOUNT").where(Filters.eq("STATUS", "ACTIVE")).sql();
         * // Output: SELECT MAX(BALANCE) FROM ACCOUNT WHERE STATUS = :STATUS
         * }</pre>
         * 
         * @param selectPart the select expression
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified property or column names.
         * Property names will be converted to SCREAMING_SNAKE_CASE format.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.select("firstName", "lastName", "email")
         *                 .from("ACCOUNT")
         *                 .where(Filters.eq("STATUS", "ACTIVE"))
         *                 .sql();
         * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName", EMAIL AS "email" 
         * //         FROM ACCOUNT WHERE STATUS = :STATUS
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified collection of property or column names.
         * Property names will be converted to SCREAMING_SNAKE_CASE format.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "accountBalance");
         * String sql = NAC.select(columns)
         *                 .from("ACCOUNT")
         *                 .orderBy("LAST_NAME")
         *                 .sql();
         * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName", 
         * //         ACCOUNT_BALANCE AS "accountBalance" FROM ACCOUNT ORDER BY LAST_NAME
         * }</pre>
         * 
         * @param propOrColumnNames the collection of property or column names to select
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * The map keys are property/column names and values are their aliases.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = Map.of(
         *     "firstName", "fname",
         *     "lastName", "lname",
         *     "accountBalance", "balance"
         * );
         * String sql = NAC.select(aliases).from("ACCOUNT").sql();
         * // Output: SELECT FIRST_NAME AS fname, LAST_NAME AS lname, ACCOUNT_BALANCE AS balance FROM ACCOUNT
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of the specified entity class.
         * Properties marked with @Transient will be excluded.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.select(Account.class)
         *                 .from("ACCOUNT")
         *                 .where(Filters.gt("BALANCE", 1000))
         *                 .sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", LAST_NAME AS "lastName", 
         * //         EMAIL AS "email", BALANCE AS "balance" FROM ACCOUNT WHERE BALANCE > :BALANCE
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // With sub-entities included
         * String sql = NAC.select(Order.class, true)
         *                 .from("ORDER")
         *                 .sql();
         * // Will include properties from Order and its related entities
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class with exclusions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.select(Account.class, Set.of("password", "securityQuestion"))
         *                 .from("ACCOUNT")
         *                 .sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", LAST_NAME AS "lastName", 
         * //         EMAIL AS "email" FROM ACCOUNT
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class with options.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.select(Order.class, true, Set.of("internalNotes"))
         *                 .from("ORDER o")
         *                 .join("CUSTOMER c", Filters.eq("o.CUSTOMER_ID", "c.ID"))
         *                 .sql();
         * // Selects all Order properties except internalNotes, plus Customer properties
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class.
         * Combines SELECT and FROM operations in a single call.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(Account.class)
         *                 .where(Filters.eq("status", "ACTIVE"))
         *                 .orderBy("lastName")
         *                 .sql();
         * // Output: SELECT ID AS "id", FIRST_NAME AS "firstName", LAST_NAME AS "lastName" 
         * //         FROM ACCOUNT WHERE STATUS = :status ORDER BY LAST_NAME
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class with table alias.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(Account.class, "a")
         *                 .where(Filters.eq("a.status", "ACTIVE"))
         *                 .sql();
         * // Output: SELECT a.ID AS "id", a.FIRST_NAME AS "firstName", a.LAST_NAME AS "lastName" 
         * //         FROM ACCOUNT a WHERE a.STATUS = :status
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class with sub-entity option.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(Order.class, true)
         *                 .where(Filters.gt("orderDate", yesterday))
         *                 .sql();
         * // Will select from Order and its related entity tables with automatic joins
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and sub-entity option.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(Order.class, "o", true)
         *                 .where(Filters.eq("o.status", "PENDING"))
         *                 .sql();
         * // Selects from Order with alias 'o' and includes sub-entity properties
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with excluded properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(Account.class, Set.of("password", "securityAnswer"))
         *                 .where(Filters.eq("email", "john@example.com"))
         *                 .sql();
         * // Selects all Account properties except password and securityAnswer
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and excluded properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(Account.class, "acc", Set.of("password"))
         *                 .join("ORDER o", Filters.eq("acc.ID", "o.ACCOUNT_ID"))
         *                 .sql();
         * // Selects Account properties with alias 'acc', excluding password
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity option and excluded properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(Order.class, true, Set.of("internalNotes", "auditLog"))
         *                 .where(Filters.between("orderDate", startDate, endDate))
         *                 .sql();
         * // Selects Order and sub-entity properties, excluding internalNotes and auditLog
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with all options.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(Order.class, "ord", true, Set.of("deletedFlag"))
         *                 .where(Filters.and(
         *                     Filters.eq("ord.status", "SHIPPED"),
         *                     Filters.gt("ord.amount", 100)
         *                 ))
         *                 .sql();
         * // Comprehensive SELECT with alias, sub-entities, and exclusions
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.SCREAMING_SNAKE_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with table and class aliases.
         * This is useful for JOIN queries where columns from multiple tables need distinct aliases.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.select(Account.class, "a", "account", Order.class, "o", "order")
         *                 .from("ACCOUNT a")
         *                 .join("ORDER o", Filters.eq("a.ID", "o.ACCOUNT_ID"))
         *                 .sql();
         * // Output: SELECT a.ID AS "account.id", a.FIRST_NAME AS "account.firstName",
         * //         o.ID AS "order.id", o.ORDER_DATE AS "order.orderDate" ...
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases and exclusions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.select(
         *                     Account.class, "a", "account", Set.of("password"),
         *                     Order.class, "o", "order", Set.of("internalNotes")
         *                 )
         *                 .from("ACCOUNT a")
         *                 .join("ORDER o", Filters.eq("a.ID", "o.ACCOUNT_ID"))
         *                 .where(Filters.eq("a.STATUS", "ACTIVE"))
         *                 .sql();
         * // Selects from both entities with aliases, excluding specified properties
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * This is the most flexible way to select from multiple entities with different configurations.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, Set.of("password")),
         *     new Selection(Order.class, "o", "order", null, true, null),
         *     new Selection(Product.class, "p", "product", null, false, Set.of("cost"))
         * );
         * String sql = NAC.select(selections)
         *                 .from("ACCOUNT a")
         *                 .join("ORDER o", Filters.eq("a.ID", "o.ACCOUNT_ID"))
         *                 .join("PRODUCT p", Filters.eq("o.PRODUCT_ID", "p.ID"))
         *                 .sql();
         * }</pre>
         * 
         * @param multiSelects list of Selection configurations for each entity
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with table and class aliases.
         * Automatically generates the FROM clause with proper table names.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(Account.class, "a", "account", Order.class, "o", "order")
         *                 .where(Filters.eq("a.ID", "o.ACCOUNT_ID"))
         *                 .sql();
         * // Automatically handles the FROM clause with proper table names and aliases
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with aliases and exclusions.
         * Automatically generates the FROM clause with proper table names.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.selectFrom(
         *                     Account.class, "acc", "account", Set.of("password"),
         *                     Order.class, "ord", "order", Set.of("notes")
         *                 )
         *                 .where(Filters.and(
         *                     Filters.eq("acc.STATUS", "PREMIUM"),
         *                     Filters.gt("ord.AMOUNT", 1000)
         *                 ))
         *                 .sql();
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * Automatically generates the FROM clause based on the selection configurations.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "acc", "accounts", true, null),
         *     new Selection(Order.class, "o", "ord", "orders", false, Set.of("deletedFlag")),
         *     new Selection(Product.class, "p", "prod", null, false, null)
         * );
         * String sql = NAC.selectFrom(selections)
         *                 .where(Filters.eq("a.ID", "o.ACCOUNT_ID"))
         *                 .sql();
         * // Complex multi-table SELECT with automatic FROM clause generation
         * }</pre>
         * 
         * @param multiSelects list of Selection configurations for each entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.SCREAMING_SNAKE_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.count("ACCOUNT").where(Filters.eq("STATUS", "ACTIVE")).sql();
         * // Output: SELECT count(*) FROM ACCOUNT WHERE STATUS = :STATUS
         * 
         * String sql2 = NAC.count("ORDER").where(Filters.gt("AMOUNT", 100)).sql();
         * // Output: SELECT count(*) FROM ORDER WHERE AMOUNT > :AMOUNT
         * }</pre>
         * 
         * @param tableName the name of the table to count rows from
         * @return an SQLBuilder configured for COUNT query
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NAC.count(Account.class)
         *                 .where(Filters.and(
         *                     Filters.eq("status", "ACTIVE"),
         *                     Filters.gt("balance", 0)
         *                 ))
         *                 .sql();
         * // Output: SELECT count(*) FROM ACCOUNT WHERE STATUS = :status AND BALANCE > :balance
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for COUNT query
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL using the entity class for property mapping.
         * This method is useful for generating just the SQL fragment for a condition.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("status", "ACTIVE"),
         *     Filters.gt("balance", 1000),
         *     Filters.like("lastName", "Smith%")
         * );
         * String sql = NAC.parse(cond, Account.class).sql();
         * // Output: STATUS = :status AND BALANCE > :balance AND LAST_NAME LIKE :lastName
         * }</pre>
         * 
         * @param cond the condition to parse
         * @param entityClass the entity class for property-to-column mapping
         * @return an SQLBuilder containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with lower camel case field/column naming strategy.
     * This builder generates SQL with named parameters (e.g., :paramName) and preserves
     * property names in camelCase format.
     * 
     * <p>The NLC builder is ideal for applications that use camelCase naming conventions
     * in their Java code and want to maintain this convention in their SQL queries.
     * Named parameters make the generated SQL more readable and easier to debug.</p>
     * 
     * <p>Key features:</p>
     * <ul>
     *   <li>Generates named parameters instead of positional parameters (?)</li>
     *   <li>Preserves camelCase property names without conversion</li>
     *   <li>Supports all standard SQL operations (SELECT, INSERT, UPDATE, DELETE)</li>
     *   <li>Integrates with entity classes using annotations</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple SELECT with named parameters
     * N.println(NLC.select("firstName", "lastName").from("account").where(Filters.eq("id", 1)).sql());
     * // Output: SELECT firstName, lastName FROM account WHERE id = :id
     * 
     * // INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = NLC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (firstName, lastName) VALUES (:firstName, :lastName)
     * }</pre>
     */
    public static class NLC extends SQLBuilder {

        /**
         * Constructs a new NLC instance with camelCase naming policy and named SQL policy.
         *
         * <p>This constructor is package-private and should not be called directly. Use the static
         * factory methods like {@link #select(String...)}, {@link #insert(String...)}, etc. instead.</p>
         */
        NLC() {
            super(NamingPolicy.CAMEL_CASE, SQLPolicy.NAMED_SQL);
        }

        /**
         * Indicates whether this builder generates named SQL parameters.
         * Named parameters use the format :parameterName instead of positional ? placeholders.
         * 
         * @return always returns {@code true} for NLC builders
         */
        @Override
        protected boolean isNamedSql() {
            return true;
        }

        /**
         * Creates a new instance of NLC builder.
         * This factory method is used internally to create new builder instances.
         * 
         * @return a new NLC instance with default configuration
         */
        protected static NLC createInstance() {
            return new NLC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * This method is useful when inserting data into a single column or when using SQL expressions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.insert("firstName").into("account").sql();
         * // Output: INSERT INTO account (firstName) VALUES (:firstName)
         * }</pre>
         * 
         * @param expr the column expression to insert
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified property or column names.
         * Property names will be preserved in camelCase format without any naming conversion.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.insert("firstName", "lastName", "email")
         *                 .into("account")
         *                 .sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to insert
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified collection of property or column names.
         * This method is useful when the column names are dynamically determined at runtime.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = NLC.insert(columns).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param propOrColumnNames the collection of property or column names to insert
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified property-value map.
         * The map keys represent column names and values represent the data to insert.
         * This method allows direct specification of both columns and their values.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("firstName", "John");
         * data.put("lastName", "Doe");
         * data.put("age", 30);
         * String sql = NLC.insert(data).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, age) VALUES (:firstName, :lastName, :age)
         * }</pre>
         * 
         * @param props the map of property names to values
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity object.
         * The entity's properties will be extracted using reflection and used for the INSERT statement.
         * Properties marked with @ReadOnly, @ReadOnlyId, or @Transient annotations will be automatically excluded.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setEmail("john.doe@example.com");
         * String sql = NLC.insert(account).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, email) VALUES (:firstName, :lastName, :email)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity object with excluded properties.
         * This method allows fine-grained control over which properties are included in the INSERT statement.
         * Properties in the excluded set, as well as those marked with @ReadOnly, @ReadOnlyId, or @Transient, will be omitted.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setCreatedTime(new Date());
         * String sql = NLC.insert(account, Set.of("createdTime"))
         *                 .into("account")
         *                 .sql();
         * // Output: INSERT INTO account (firstName, lastName) VALUES (:firstName, :lastName)
         * }</pre>
         * 
         * @param entity the entity object to insert
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class.
         * All insertable properties of the class will be included in the INSERT statement.
         * This method is useful when you want to prepare an INSERT template based on the entity structure.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.insert(Account.class).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, email, age) VALUES (:firstName, :lastName, :email, :age)
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with excluded properties.
         * This method generates an INSERT template based on the entity class structure,
         * excluding specified properties and those marked with restrictive annotations.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.insert(Account.class, Set.of("id", "createdTime"))
         *                 .into("account")
         *                 .sql();
         * // Output: INSERT INTO account (firstName, lastName, email, age) VALUES (:firstName, :lastName, :email, :age)
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT INTO SQL builder for the specified entity class.
         * This is a convenience method that combines insert() and into() operations.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.insertInto(Account.class).sql();
         * // Output: INSERT INTO account (firstName, lastName, email, age) VALUES (:firstName, :lastName, :email, :age)
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT INTO SQL builder for the specified entity class with excluded properties.
         * This convenience method combines insert() and into() operations while allowing property exclusion.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.insertInto(Account.class, Set.of("id", "version"))
         *                 .sql();
         * // Output: INSERT INTO account (firstName, lastName, email, age) VALUES (:firstName, :lastName, :email, :age)
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the INSERT
         * @return an SQLBuilder configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for MySQL-style batch inserts.
         * This method generates a single INSERT statement with multiple value rows,
         * which is more efficient than executing multiple individual INSERT statements.
         * Each entity in the collection will have its own set of named parameters with numeric suffixes.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account1 = new Account("John", "Doe");
         * Account account2 = new Account("Jane", "Smith");
         * Account account3 = new Account("Bob", "Johnson");
         * List<Account> accounts = Arrays.asList(account1, account2, account3);
         * 
         * String sql = NLC.batchInsert(accounts).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName) VALUES 
         * //         (:firstName_1, :lastName_1), 
         * //         (:firstName_2, :lastName_2), 
         * //         (:firstName_3, :lastName_3)
         * }</pre>
         * 
         * @param propsList collection of entities or property maps to insert
         * @return an SQLBuilder configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * <p>
         * <b>Note:</b> This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * This method starts building an UPDATE statement for the given table name.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.update("account")
         *                 .set("status", "active")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET status = :status WHERE id = :id
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return an SQLBuilder configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class mapping.
         * The entity class provides property-to-column mapping information for the UPDATE statement.
         * This is useful when you want to update a table using entity property names.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.update("account", Account.class)
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = :firstName, lastName = :lastName WHERE id = :id
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return an SQLBuilder configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null/empty or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * All updatable properties (not marked with @NonUpdatable) will be available for updating.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.update(Account.class)
         *                 .set("status", "active")
         *                 .set("lastLoginTime", new Date())
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET status = :status, lastLoginTime = :lastLoginTime WHERE id = :id
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class with excluded properties.
         * The table name will be derived from the entity class name or @Table annotation.
         * Properties marked with @NonUpdatable or in the excluded set will be omitted from updates.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.update(Account.class, Set.of("createdTime", "createdBy"))
         *                 .set("status", "active")
         *                 .set("modifiedTime", new Date())
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET status = :status, modifiedTime = :modifiedTime WHERE id = :id
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the UPDATE
         * @return an SQLBuilder configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table.
         * This method starts building a DELETE statement for the given table name.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.deleteFrom("account")
         *                 .where(Filters.eq("status", "inactive"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE status = :status
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return an SQLBuilder configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified table with entity class mapping.
         * The entity class provides property-to-column mapping information for WHERE conditions.
         * This is useful when you want to use entity property names in the WHERE clause.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.deleteFrom("account", Account.class)
         *                 .where(Filters.and(
         *                     Filters.eq("status", "inactive"),
         *                     Filters.lt("lastLoginTime", oneYearAgo)
         *                 ))
         *                 .sql();
         * // Output: DELETE FROM account WHERE status = :status AND lastLoginTime < :lastLoginTime
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return an SQLBuilder configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null/empty or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE FROM SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.deleteFrom(Account.class)
         *                 .where(Filters.eq("status", "inactive"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE status = :status
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single select expression.
         * The expression can be a column name, function call, or any valid SQL expression.
         * This method is useful for simple queries or when using SQL functions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.select("COUNT(*)").from("account").sql();
         * // Output: SELECT count(*) FROM account
         * 
         * String sql2 = NLC.select("MAX(balance)").from("account").where(Filters.eq("status", "active")).sql();
         * // Output: SELECT MAX(balance) FROM account WHERE status = :status
         * }</pre>
         * 
         * @param selectPart the select expression
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified property or column names.
         * Property names will be preserved in camelCase format without any conversion.
         * This is the most common way to start building a SELECT query.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.select("firstName", "lastName", "email")
         *                 .from("account")
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT firstName, lastName, email FROM account WHERE status = :status
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to select
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified collection of property or column names.
         * This method is useful when the columns to select are determined dynamically at runtime.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = NLC.select(columns)
         *                 .from("account")
         *                 .orderBy("lastName")
         *                 .sql();
         * // Output: SELECT firstName, lastName, email FROM account ORDER BY lastName
         * }</pre>
         * 
         * @param propOrColumnNames the collection of property or column names to select
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * The map keys are property/column names and values are their aliases.
         * This method allows you to rename columns in the result set.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> columnAliases = new HashMap<>();
         * columnAliases.put("firstName", "fname");
         * columnAliases.put("lastName", "lname");
         * columnAliases.put("emailAddress", "email");
         * 
         * String sql = NLC.select(columnAliases)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT firstName AS fname, lastName AS lname, emailAddress AS email FROM account
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of the specified entity class.
         * This method selects all properties that are not marked with @Transient annotation.
         * Sub-entity properties are not included by default.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.select(Account.class)
         *                 .from("account")
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email, status, balance FROM account WHERE status = :status
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class.
         * This method allows control over whether properties from sub-entities should be included.
         * Sub-entities are typically used for one-to-one or many-to-one relationships.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Without sub-entity properties
         * String sql1 = NLC.select(Order.class, false).from("orders").sql();
         * // Output: SELECT id, orderNumber, amount, status FROM orders
         * 
         * // With sub-entity properties (if Order has an Account sub-entity)
         * String sql2 = NLC.select(Order.class, true).from("orders").sql();
         * // Output: SELECT id, orderNumber, amount, status, account.id, account.firstName, account.lastName FROM orders
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class with exclusions.
         * This method selects all properties except those specified in the excluded set.
         * Properties marked with @Transient are always excluded.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password", "secretKey");
         * String sql = NLC.select(Account.class, excluded)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email, status, balance FROM account
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for properties of the specified entity class with full control.
         * This method provides complete control over property selection, sub-entity inclusion, and exclusions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password", "internalNotes");
         * String sql = NLC.select(Account.class, true, excluded)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email, status, balance, address.street, address.city FROM account
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class.
         * This is a convenience method that combines SELECT and FROM operations.
         * The table name is derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.selectFrom(Account.class)
         *                 .where(Filters.eq("status", "active"))
         *                 .orderBy("lastName")
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email, status, balance FROM account WHERE status = :status ORDER BY lastName
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class with table alias.
         * The alias is used to qualify column names in complex queries with joins.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.selectFrom(Account.class, "a")
         *                 .innerJoin("orders o", Filters.eq("a.id", "o.accountId"))
         *                 .where(Filters.eq("a.status", "active"))
         *                 .sql();
         * // Output: SELECT a.id, a.firstName, a.lastName, a.email, a.status, a.balance 
         * //         FROM account a 
         * //         INNER JOIN orders o ON a.id = o.accountId 
         * //         WHERE a.status = :status
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM SQL builder for the specified entity class with sub-entity option.
         * When sub-entity properties are included, the appropriate JOIN clauses are automatically generated.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Assuming Order has an Account sub-entity
         * String sql = NLC.selectFrom(Order.class, true)
         *                 .where(Filters.gt("amount", 100))
         *                 .sql();
         * // Output: SELECT o.id, o.orderNumber, o.amount, o.status, 
         * //                a.id AS "account.id", a.firstName AS "account.firstName", a.lastName AS "account.lastName"
         * //         FROM orders o 
         * //         LEFT JOIN account a ON o.accountId = a.id
         * //         WHERE o.amount > :amount
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and sub-entity option.
         * This method provides control over both table aliasing and sub-entity inclusion.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.selectFrom(Order.class, "ord", true)
         *                 .where(Filters.between("orderDate", startDate, endDate))
         *                 .sql();
         * // Output: SELECT ord.id, ord.orderNumber, ord.amount, ord.status,
         * //                acc.id AS "account.id", acc.firstName AS "account.firstName"
         * //         FROM orders ord
         * //         LEFT JOIN account acc ON ord.accountId = acc.id
         * //         WHERE orderDate BETWEEN :minOrderDate AND :maxOrderDate
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM SQL builder with excluded properties.
         * This is a convenience method for common use cases where certain properties should be excluded.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> sensitiveFields = Set.of("password", "ssn", "creditCardNumber");
         * String sql = NLC.selectFrom(Account.class, sensitiveFields)
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email, status, balance FROM account WHERE id = :id
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with table alias and excluded properties.
         * This method combines table aliasing with property exclusion for complex queries.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password", "securityAnswer");
         * String sql = NLC.selectFrom(Account.class, "acc", excluded)
         *                 .innerJoin("orders o", Filters.eq("acc.id", "o.accountId"))
         *                 .where(Filters.gt("o.amount", 1000))
         *                 .sql();
         * // Output: SELECT acc.id, acc.firstName, acc.lastName, acc.email, acc.status, acc.balance
         * //         FROM account acc
         * //         INNER JOIN orders o ON acc.id = o.accountId
         * //         WHERE o.amount > :amount
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with sub-entity option and excluded properties.
         * This method allows inclusion of sub-entity properties while excluding specific fields.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("account.password", "internalNotes");
         * String sql = NLC.selectFrom(Order.class, true, excluded)
         *                 .where(Filters.eq("status", "COMPLETED"))
         *                 .sql();
         * // Output: SELECT o.id, o.orderNumber, o.amount, o.status,
         * //                a.id AS "account.id", a.firstName AS "account.firstName", a.lastName AS "account.lastName"
         * //         FROM orders o
         * //         LEFT JOIN account a ON o.accountId = a.id
         * //         WHERE o.status = :status
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM SQL builder with full control over all options.
         * This is the most comprehensive method for creating SELECT queries with entity classes.
         * When includeSubEntityProperties is true, appropriate JOIN clauses are automatically generated.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = Set.of("password", "account.internalId");
         * String sql = NLC.selectFrom(Order.class, "o", true, excluded)
         *                 .where(Filters.and(
         *                     Filters.eq("o.status", "PENDING"),
         *                     Filters.gt("o.amount", 500)
         *                 ))
         *                 .orderBy("o.orderDate DESC")
         *                 .sql();
         * // Output: SELECT o.id, o.orderNumber, o.amount, o.status, o.orderDate,
         * //                a.id AS "account.id", a.firstName AS "account.firstName", a.lastName AS "account.lastName"
         * //         FROM orders o
         * //         LEFT JOIN account a ON o.accountId = a.id
         * //         WHERE o.status = :status AND o.amount > :amount
         * //         ORDER BY o.orderDate DESC
         * }</pre>
         * 
         * @param entityClass the entity class
         * @param alias the table alias
         * @param includeSubEntityProperties if true, properties of sub-entities will be included
         * @param excludedPropNames the set of property names to exclude from the SELECT
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.CAMEL_CASE);
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with table and class aliases.
         * This method is specifically designed for JOIN queries where columns from multiple tables
         * need distinct aliases to avoid naming conflicts in the result set.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.select(Account.class, "a", "account", Order.class, "o", "order")
         *                 .from("account a")
         *                 .innerJoin("orders o", Filters.eq("a.id", "o.accountId"))
         *                 .where(Filters.eq("a.status", "active"))
         *                 .sql();
         * // Output: SELECT a.id AS "account.id", a.firstName AS "account.firstName", a.lastName AS "account.lastName",
         * //                o.id AS "order.id", o.orderNumber AS "order.orderNumber", o.amount AS "order.amount"
         * //         FROM account a
         * //         INNER JOIN orders o ON a.id = o.accountId
         * //         WHERE a.status = :status
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for two entity classes with aliases and exclusions.
         * This method provides full control over column selection from multiple tables,
         * allowing exclusion of sensitive or unnecessary fields from each entity.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> accountExclusions = Set.of("password", "securityQuestion");
         * Set<String> orderExclusions = Set.of("internalNotes");
         * 
         * String sql = NLC.select(Account.class, "a", "account", accountExclusions,
         *                        Order.class, "o", "order", orderExclusions)
         *                 .from("account a")
         *                 .innerJoin("orders o", Filters.eq("a.id", "o.accountId"))
         *                 .where(Filters.and(
         *                     Filters.eq("a.status", "active"),
         *                     Filters.gt("o.amount", 1000)
         *                 ))
         *                 .sql();
         * // Output: SELECT a.id AS "account.id", a.firstName AS "account.firstName", a.lastName AS "account.lastName",
         * //                a.email AS "account.email", a.status AS "account.status",
         * //                o.id AS "order.id", o.orderNumber AS "order.orderNumber", o.amount AS "order.amount"
         * //         FROM account a
         * //         INNER JOIN orders o ON a.id = o.accountId
         * //         WHERE a.status = :status AND o.amount > :amount
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * This is the most flexible method for selecting from multiple entities with different configurations.
         * Each Selection object specifies how columns from a particular entity should be selected and aliased.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, Set.of("password")),
         *     new Selection(Order.class, "o", "order", null, true, null),
         *     new Selection(Product.class, "p", "product", null, false, Set.of("cost"))
         * );
         * 
         * String sql = NLC.select(selections)
         *                 .from("account a")
         *                 .innerJoin("orders o", Filters.eq("a.id", "o.accountId"))
         *                 .innerJoin("order_items oi", Filters.eq("o.id", "oi.orderId"))
         *                 .innerJoin("products p", Filters.eq("oi.productId", "p.id"))
         *                 .where(Filters.eq("a.status", "active"))
         *                 .sql();
         * // Output: Complex SELECT with columns from all three entities, properly aliased
         * }</pre>
         * 
         * @param multiSelects list of Selection configurations for each entity
         * @return an SQLBuilder configured for SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with table and class aliases.
         * This convenience method automatically generates the FROM clause with proper table names
         * derived from the entity classes or their @Table annotations.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.selectFrom(Account.class, "a", "account", Order.class, "o", "order")
         *                 .where(Filters.eq("a.id", "o.accountId"))
         *                 .sql();
         * // Output: SELECT a.id AS "account.id", a.firstName AS "account.firstName", a.lastName AS "account.lastName",
         * //                o.id AS "order.id", o.orderNumber AS "order.orderNumber", o.amount AS "order.amount"
         * //         FROM account a, orders o
         * //         WHERE a.id = o.accountId
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM SQL builder for two entity classes with aliases and exclusions.
         * This convenience method automatically generates the FROM clause and allows fine-grained
         * control over which properties are selected from each entity.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> accountExclusions = Set.of("password", "securityAnswer");
         * Set<String> orderExclusions = Set.of("internalNotes", "adminComments");
         * 
         * String sql = NLC.selectFrom(Account.class, "acc", "account", accountExclusions,
         *                            Order.class, "ord", "order", orderExclusions)
         *                 .where(Filters.and(
         *                     Filters.eq("acc.status", "PREMIUM"),
         *                     Filters.between("orderDate", startDate, endDate)
         *                 ))
         *                 .orderBy("ord.amount DESC")
         *                 .sql();
         * // Output: SELECT acc.id AS "account.id", acc.firstName AS "account.firstName", 
         * //                acc.lastName AS "account.lastName", acc.email AS "account.email",
         * //                ord.id AS "order.id", ord.orderNumber AS "order.orderNumber", 
         * //                ord.amount AS "order.amount", ord.orderDate AS "order.orderDate"
         * //         FROM account acc, orders ord
         * //         WHERE acc.status = :status AND orderDate BETWEEN :minOrderDate AND :maxOrderDate
         * //         ORDER BY ord.amount DESC
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for columns from the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for columns from the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM SQL builder for multiple entity selections.
         * This method automatically generates the FROM clause based on the selection configurations,
         * making it the most convenient way to build complex multi-table queries.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, false, Set.of("password")),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(OrderItem.class, "oi", "item", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, Set.of("cost", "supplier"))
         * );
         * 
         * String sql = NLC.selectFrom(selections)
         *                 .where(Filters.and(
         *                     Filters.eq("a.status", "active"),
         *                     Filters.eq("o.status", "completed"),
         *                     Filters.gt("p.price", 100)
         *                 ))
         *                 .groupBy("a.id", "o.id")
         *                 .having(Filters.gt("SUM(oi.quantity * p.price)", 1000))
         *                 .sql();
         * // Output: Complex SELECT with proper FROM clause, column aliasing, WHERE, GROUP BY, and HAVING
         * }</pre>
         * 
         * @param multiSelects list of Selection configurations for each entity
         * @return an SQLBuilder configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         * This is a convenience method for creating simple count queries.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.count("account")
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE status = :status
         * 
         * // Can also be used with joins
         * String sql2 = NLC.count("account a")
         *                  .innerJoin("orders o", Filters.eq("a.id", "o.accountId"))
         *                  .where(Filters.gt("o.amount", 1000))
         *                  .sql();
         * // Output: SELECT count(*) FROM account a INNER JOIN orders o ON a.id = o.accountId WHERE o.amount > :amount
         * }</pre>
         * 
         * @param tableName the name of the table to count rows from
         * @return an SQLBuilder configured for COUNT query
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified entity class.
         * The table name will be derived from the entity class name or @Table annotation.
         * This is a convenience method for counting rows in entity-mapped tables.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = NLC.count(Account.class)
         *                 .where(Filters.eq("status", "active"))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE status = :status
         * 
         * // Can use entity properties in WHERE clause
         * String sql2 = NLC.count(Order.class)
         *                  .where(Filters.and(
         *                      Filters.gt("amount", 100),
         *                      Filters.between("orderDate", startDate, endDate)
         *                  ))
         *                  .sql();
         * // Output: SELECT count(*) FROM orders WHERE amount > :amount AND orderDate BETWEEN :minOrderDate AND :maxOrderDate
         * }</pre>
         * 
         * @param entityClass the entity class
         * @return an SQLBuilder configured for COUNT query
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL using the entity class for property mapping.
         * This method is useful for generating just the SQL fragment for a condition.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("status", "ACTIVE"),
         *     Filters.gt("balance", 1000),
         *     Filters.like("lastName", "Smith%")
         * );
         * String sql = NLC.parse(cond, Account.class).sql();
         * // Output: status = :status AND balance > :balance AND lastName LIKE :lastName
         * }</pre>
         * 
         * @param cond the condition to parse
         * @param entityClass the entity class for property-to-column mapping
         * @return an SQLBuilder containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * Named SQL builder with {@code NamingPolicy.NO_CHANGE} field/column naming strategy.
     * This class generates SQL with MyBatis-style named parameters (#{paramName}).
     * 
     * <p>This builder preserves the exact case of property and column names as they are provided,
     * without any transformation. It's useful when working with databases that have specific
     * naming conventions that should not be altered.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Column names are preserved exactly as written
     * String sql = MSB.select("first_Name", "last_NaMe")
     *                 .from("account")
     *                 .where(Filters.eq("last_NaMe", 1))
     *                 .sql();
     * // Output: SELECT first_Name, last_NaMe FROM account WHERE last_NaMe = #{last_NaMe}
     * }</pre>
     * 
     * @deprecated Use {@link NSB} or other non-deprecated builders instead
     */
    @Deprecated
    public static class MSB extends SQLBuilder {

        /**
         * Constructs a new MSB instance with NO_CHANGE naming policy and IBATIS_SQL format.
         * This constructor is package-private and should not be called directly.
         * Use the static factory methods instead.
         */
        MSB() {
            super(NamingPolicy.NO_CHANGE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Creates a new instance of MSB.
         * This is an internal factory method used by the static builder methods.
         *
         * @return a new MSB instance
         */
        protected static MSB createInstance() {
            return new MSB();
        }

        /**
         * Creates an INSERT statement for a single column.
         * 
         * <p>This is a convenience method equivalent to calling {@code insert(new String[] {expr})}.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.insert("name").into("users").sql();
         * // Output: INSERT INTO users (name) VALUES (#{name})
         * }</pre>
         * 
         * @param expr the column name or expression to insert
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for the specified columns.
         * 
         * <p>The column names will be used both in the INSERT column list and as parameter names
         * in the VALUES clause.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.insert("firstName", "lastName", "email")
         *                 .into("users")
         *                 .sql();
         * // Output: INSERT INTO users (firstName, lastName, email) 
         * //         VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         * 
         * @param propOrColumnNames the property or column names to include in the INSERT
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for the specified columns provided as a collection.
         * 
         * <p>This method is useful when the column names are dynamically determined.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("id", "name", "status");
         * String sql = MSB.insert(columns).into("products").sql();
         * // Output: INSERT INTO products (id, name, status) 
         * //         VALUES (#{id}, #{name}, #{status})
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to include
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement using a map of column names to values.
         * 
         * <p>The map keys represent column names, and the values are the corresponding
         * values to be inserted. This is useful for dynamic INSERT statements.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("name", "John");
         * data.put("age", 30);
         * String sql = MSB.insert(data).into("users").sql();
         * // Output: INSERT INTO users (name, age) VALUES (#{name}, #{age})
         * }</pre>
         * 
         * @param props map of column names to their values
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement based on an entity object.
         * 
         * <p>All non-null properties of the entity will be included in the INSERT statement,
         * except those marked with {@code @Transient}, {@code @ReadOnly}, or {@code @ReadOnlyId}
         * annotations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User("John", "Doe", "john@example.com");
         * String sql = MSB.insert(user).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName, email) 
         * //         VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         * 
         * @param entity the entity object containing data to insert
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement based on an entity object, excluding specified properties.
         * 
         * <p>This method allows fine-grained control over which properties are included
         * in the INSERT statement.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User();
         * Set<String> exclude = new HashSet<>(Arrays.asList("createdDate", "modifiedDate"));
         * String sql = MSB.insert(user, exclude).into("users").sql();
         * }</pre>
         * 
         * @param entity the entity object containing data to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for all insertable properties of an entity class.
         * 
         * <p>This generates an INSERT template based on the entity class structure,
         * including all properties except those annotated with {@code @Transient},
         * {@code @ReadOnly}, or {@code @ReadOnlyId}.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.insert(User.class).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName, email, age) 
         * //         VALUES (#{firstName}, #{lastName}, #{email}, #{age})
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class, excluding specified properties.
         * 
         * <p>This method provides control over which properties are included when
         * generating an INSERT template from an entity class.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = MSB.insert(User.class, exclude).into("users").sql();
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for an entity class with automatic table name detection.
         * 
         * <p>The table name is determined from the {@code @Table} annotation on the entity class,
         * or derived from the class name if no annotation is present.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * @Table("users")
         * public class User { ... }
         * 
         * String sql = MSB.insertInto(User.class).sql();
         * // Output: INSERT INTO users (firstName, lastName, email) 
         * //         VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         * 
         * @param entityClass the entity class to insert
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT statement for an entity class with automatic table name detection,
         * excluding specified properties.
         *
         * <p>Combines automatic table name detection with property exclusion. This is useful when
         * certain properties should not be inserted (e.g., auto-generated IDs, calculated fields).</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("id", "createdDate");
         * String sql = MSB.insertInto(Account.class, excluded)
         *                 .values("John", "john@email.com", "ACTIVE")
         *                 .sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, EMAIL, STATUS) VALUES (?, ?, ?)
         * }</pre>
         *
         * @param entityClass the entity class to insert
         * @param excludedPropNames set of property names to exclude
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT statement for multiple records in MySQL style.
         * 
         * <p>This generates a single INSERT statement with multiple value sets,
         * which is more efficient than multiple individual INSERT statements.</p>
         * 
         * <p>The method accepts a collection of entities or maps. All items must have
         * the same structure (same properties/keys).</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * String sql = MSB.batchInsert(users).into("users").sql();
         * // Output: INSERT INTO users (firstName, lastName) 
         * //         VALUES (#{firstName}, #{lastName}), 
         * //                (#{firstName}, #{lastName})
         * }</pre>
         * 
         * @param propsList collection of entities or property maps to insert
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propsList is null or empty
         * 
         * <p>
         * <b>Note:</b> This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for the specified table.
         * 
         * <p>After calling this method, use {@code set()} to specify which columns to update.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.update("users")
         *                 .set("status", "lastModified")
         *                 .where(Filters.eq("id", 123))
         *                 .sql();
         * // Output: UPDATE users SET status = #{status}, lastModified = #{lastModified} 
         * //         WHERE id = #{id}
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class context.
         * 
         * <p>This method is useful when you want to specify a custom table name
         * but still use entity class metadata for column mapping.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.update("user_archive", User.class)
         *                 .set("status")
         *                 .where(Filters.eq("userId", 123))
         *                 .sql();
         * }</pre>
         * 
         * @param tableName the name of the table to update
         * @param entityClass the entity class for column mapping
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class with automatic table name detection.
         * 
         * <p>All updatable properties (excluding those marked with {@code @ReadOnly},
         * {@code @ReadOnlyId}, {@code @NonUpdatable}, or {@code @Transient}) will be
         * included in the SET clause.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.update(User.class)
         *                 .where(Filters.eq("id", 123))
         *                 .sql();
         * // Output: UPDATE users SET firstName = #{firstName}, lastName = #{lastName}, 
         * //         email = #{email} WHERE id = #{id}
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class, excluding specified properties.
         * 
         * <p>This method provides fine-grained control over which properties are included
         * in the UPDATE statement's SET clause.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("createdDate", "createdBy"));
         * String sql = MSB.update(User.class, exclude)
         *                 .where(Filters.eq("id", 123))
         *                 .sql();
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the update
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE statement for the specified table.
         * 
         * <p>Use {@code where()} to add conditions to the DELETE statement.
         * Be careful with DELETE statements without WHERE clauses.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.deleteFrom("users")
         *                 .where(Filters.eq("status", "INACTIVE"))
         *                 .sql();
         * // Output: DELETE FROM users WHERE status = #{status}
         * }</pre>
         * 
         * @param tableName the name of the table to delete from
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE statement for a table with entity class context.
         *
         * <p>This method is useful when you want to use a custom table name
         * but still benefit from entity class metadata for column mapping in WHERE conditions.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.deleteFrom("ACCOUNT_ARCHIVE", Account.class)
         *                 .where(Filters.lt("lastLogin", "2020-01-01"))
         *                 .sql();
         * // Output: DELETE FROM ACCOUNT_ARCHIVE WHERE LAST_LOGIN < ?
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for column mapping
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty, or entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE statement for an entity class with automatic table name detection.
         * 
         * <p>The table name is determined from the {@code @Table} annotation on the entity class,
         * or derived from the class name if no annotation is present.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.deleteFrom(User.class)
         *                 .where(Filters.lt("lastLoginDate", someDate))
         *                 .sql();
         * // Output: DELETE FROM users WHERE lastLoginDate < #{lastLoginDate}
         * }</pre>
         * 
         * @param entityClass the entity class representing the table
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression or column.
         * 
         * <p>The expression can be a simple column name, a function call, or any valid SQL expression.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.select("COUNT(*)").from("users").sql();
         * // Output: SELECT count(*) FROM users
         * 
         * String sql2 = MSB.select("MAX(salary)").from("employees").sql();
         * // Output: SELECT MAX(salary) FROM employees
         * }</pre>
         * 
         * @param selectPart the SELECT expression (column, function, etc.)
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement with multiple columns or expressions.
         * 
         * <p>Each string in the array represents a column name or expression to be selected.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.select("firstName", "lastName", "email")
         *                 .from("users")
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * // Output: SELECT firstName, lastName, email FROM users WHERE active = #{active}
         * }</pre>
         * 
         * @param propOrColumnNames array of property or column names to select
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement with columns specified as a collection.
         * 
         * <p>This method is useful when the columns to select are determined dynamically.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = getRequiredColumns();
         * String sql = MSB.select(columns)
         *                 .from("users")
         *                 .sql();
         * }</pre>
         * 
         * @param propOrColumnNames collection of property or column names to select
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with column aliases.
         * 
         * <p>The map keys represent the column names or expressions, and the values
         * represent their aliases in the result set.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = MSB.select(aliases).from("users").sql();
         * // Output: SELECT firstName AS fname, lastName AS lname FROM users
         * }</pre>
         * 
         * @param propOrColumnNameAliases map of column names/expressions to their aliases
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all columns of an entity class.
         * 
         * <p>Selects all properties that are not marked with {@code @Transient} annotation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.select(User.class).from("users").sql();
         * // Output: SELECT id, firstName, lastName, email FROM users
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with sub-entity control.
         * 
         * <p>When {@code includeSubEntityProperties} is true, properties that are themselves
         * entities will have their properties included in the selection with prefixed names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // If User has an Address property
         * String sql = MSB.select(User.class, true).from("users").sql();
         * // May include: id, firstName, address.street, address.city, etc.
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class, excluding specified properties.
         * 
         * <p>This method allows you to select most properties of an entity while excluding
         * a few specific ones.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password", "secretKey"));
         * String sql = MSB.select(User.class, exclude).from("users").sql();
         * // Output: SELECT id, firstName, lastName, email FROM users
         * // (password and secretKey are excluded)
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from selection
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement for an entity class with full control over selection.
         *
         * <p>This method combines sub-entity inclusion control with property exclusion,
         * providing maximum flexibility in determining what to select.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "securityToken");
         * String sql = MSC.select(Account.class, true, excluded)
         *                 .from("ACCOUNT")
         *                 .sql();
         * // Output: SELECT ID, FIRST_NAME, EMAIL ... FROM ACCOUNT (excludes password, securityToken; includes sub-entity properties)
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from selection
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for an entity class.
         * 
         * <p>This is a convenience method that combines select() and from() operations.
         * The table name is automatically determined from the entity class.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.selectFrom(User.class)
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, email FROM users WHERE active = #{active}
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a SELECT FROM statement for an entity class with a table alias.
         * 
         * <p>The alias can be used in WHERE conditions and JOIN clauses to disambiguate
         * column references.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.selectFrom(User.class, "u")
         *                 .where(Filters.eq("u.active", true))
         *                 .sql();
         * // Output: SELECT u.id, u.firstName, u.lastName FROM users u WHERE u.active = #{active}
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT FROM statement with sub-entity inclusion control.
         *
         * <p>When sub-entities are included, the query may generate JOINs to fetch
         * related entity data in a single query.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.selectFrom(Order.class, true)
         *                 .where(Filters.gt("totalAmount", 100))
         *                 .sql();
         * // Output: SELECT o.ID, o.TOTAL_AMOUNT, c.NAME ... FROM ORDERS o LEFT JOIN CUSTOMERS c ON ... WHERE o.TOTAL_AMOUNT > ?
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement with table alias and sub-entity control.
         *
         * <p>Combines table aliasing with sub-entity property inclusion for complex queries.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.selectFrom(Order.class, "ord", true)
         *                 .where(Filters.eq("ord.status", "'PENDING'"))
         *                 .sql();
         * // Output: SELECT ord.ID, ord.TOTAL_AMOUNT, c.NAME ... FROM ORDERS ord LEFT JOIN CUSTOMERS c ON ... WHERE ord.status = ?
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT FROM statement excluding specified properties.
         *
         * <p>This is a convenience method that combines property exclusion with
         * automatic FROM clause generation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("password", "internalNotes");
         * String sql = MSC.selectFrom(Account.class, excluded)
         *                 .where(Filters.eq("status", "'ACTIVE'"))
         *                 .sql();
         * // Output: SELECT ID, FIRST_NAME, EMAIL FROM ACCOUNT WHERE STATUS = ?
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with alias and property exclusion.
         *
         * <p>Provides table aliasing while excluding specific properties from selection.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("largeBlob");
         * String sql = MSC.selectFrom(Document.class, "doc", excluded)
         *                 .where(Filters.like("doc.title", "'%report%'"))
         *                 .sql();
         * // Output: SELECT doc.ID, doc.TITLE, doc.AUTHOR FROM DOCUMENTS doc WHERE doc.TITLE LIKE ?
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with sub-entity control and property exclusion.
         *
         * <p>This method provides control over both sub-entity inclusion and property exclusion
         * without specifying a table alias.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excluded = N.asSet("internalData");
         * String sql = MSC.selectFrom(Order.class, true, excluded)
         *                 .where(Filters.gt("totalAmount", 500))
         *                 .sql();
         * // Output: SELECT o.ID, o.TOTAL_AMOUNT, c.NAME ... FROM ORDERS o LEFT JOIN CUSTOMERS c ON ... WHERE o.TOTAL_AMOUNT > ?
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT FROM statement with full control over all options.
         * 
         * <p>This is the most flexible selectFrom method, allowing control over table alias,
         * sub-entity inclusion, and property exclusion.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = new HashSet<>(Arrays.asList("password"));
         * String sql = MSB.selectFrom(User.class, "u", true, exclude)
         *                 .where(Filters.eq("u.active", true))
         *                 .sql();
         * // May generate complex query with JOINs for sub-entities
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.NO_CHANGE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for joining two entity classes.
         * 
         * <p>This method sets up a query that will select columns from two different tables,
         * preparing for a JOIN operation. Each entity can have its own table alias and
         * result set column prefix.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.select(User.class, "u", "user", Order.class, "o", "order")
         *                 .from("users", "u")
         *                 .join("orders", "o").on("u.id = o.user_id")
         *                 .sql();
         * // Output: SELECT u.id AS "user.id", u.name AS "user.name", 
         * //                o.id AS "order.id", o.total AS "order.total"
         * //         FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA column prefix for the first entity's columns in the result
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB column prefix for the second entity's columns in the result
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for joining two entity classes with property exclusion.
         *
         * <p>This method extends the two-entity select by allowing you to exclude specific
         * properties from each entity class independently.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExcluded = N.asSet("PASSWORD", "SECURITY_TOKEN");
         * Set<String> orderExcluded = N.asSet("INTERNAL_NOTES");
         * String sql = MSC.select(User.class, "u", "user", userExcluded,
         *                        Order.class, "o", "order", orderExcluded)
         *                 .from("USERS u")
         *                 .join("ORDERS o").on("u.ID = o.USER_ID")
         *                 .sql();
         * // Output: SELECT u.ID AS "user.ID", u.NAME AS "user.NAME", o.ID AS "order.ID" FROM USERS u JOIN ORDERS o ON u.ID = o.USER_ID
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA column prefix for the first entity's columns
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB column prefix for the second entity's columns
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entity classes with detailed configuration.
         * 
         * <p>This is the most flexible select method, supporting any number of entities
         * with individual configuration for each through the Selection objects.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, excludeSet)
         * );
         * String sql = MSB.select(selections).from(...).sql();
         * }</pre>
         * 
         * @param multiSelects list of Selection configurations for each entity
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if multiSelects is null, empty, or contains invalid configurations
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM statement for joining two entities.
         *
         * <p>This is a convenience method that combines multi-entity selection with
         * automatic FROM clause generation including proper table aliases.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.selectFrom(User.class, "u", "user", Order.class, "o", "order")
         *                 .where(Filters.eq("u.ID", "o.USER_ID"))
         *                 .sql();
         * // Output: SELECT u.ID AS "user.ID", u.NAME AS "user.NAME", o.ID AS "order.ID" FROM USERS u, ORDERS o WHERE u.ID = o.USER_ID
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA column prefix for the first entity's columns
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB column prefix for the second entity's columns
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT FROM statement for two entities with property exclusion.
         *
         * <p>Combines multi-entity selection with property exclusion and automatic
         * FROM clause generation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExcluded = N.asSet("PASSWORD");
         * Set<String> orderExcluded = N.asSet("NOTES");
         * String sql = MSC.selectFrom(User.class, "u", "user", userExcluded,
         *                            Order.class, "o", "order", orderExcluded)
         *                 .where(Filters.eq("u.ID", "o.USER_ID"))
         *                 .sql();
         * // Output: SELECT u.ID AS "user.ID", u.NAME AS "user.NAME", o.ID AS "order.ID" FROM USERS u, ORDERS o WHERE u.ID = o.USER_ID
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA table alias for the first entity
         * @param classAliasA column prefix for the first entity's columns
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB table alias for the second entity
         * @param classAliasB column prefix for the second entity's columns
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a SELECT FROM statement for multiple entities with detailed configuration.
         *
         * <p>This method automatically generates the appropriate FROM clause with all
         * necessary table names and aliases based on the Selection configurations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = N.asList(
         *     new Selection(User.class, "u", "user"),
         *     new Selection(Order.class, "o", "order"),
         *     new Selection(Product.class, "p", "product")
         * );
         * String sql = MSC.selectFrom(selections)
         *                 .where(Filters.and(Filters.eq("u.ID", "o.USER_ID"), Filters.eq("o.PRODUCT_ID", "p.ID")))
         *                 .sql();
         * // Output: SELECT u.ID AS "user.ID", o.ID AS "order.ID", p.NAME AS "product.NAME" FROM USERS u, ORDERS o, PRODUCTS p WHERE ...
         * }</pre>
         *
         * @param multiSelects list of Selection configurations for each entity
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if multiSelects is null, empty, or contains invalid configurations
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.NO_CHANGE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) query for the specified table.
         * 
         * <p>This is a convenience method for creating count queries.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.count("users")
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * // Output: SELECT count(*) FROM users WHERE active = #{active}
         * }</pre>
         * 
         * @param tableName the table to count rows from
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) query for an entity class.
         * 
         * <p>The table name is automatically determined from the entity class.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSB.count(User.class)
         *                 .where(Filters.between("age", 18, 65))
         *                 .sql();
         * // Output: SELECT count(*) FROM users WHERE age BETWEEN #{minAge} AND #{maxAge}
         * }</pre>
         * 
         * @param entityClass the entity class representing the table
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL with entity class context.
         * 
         * <p>This method is useful for generating just the SQL representation of a condition,
         * without building a complete statement. It can be used for debugging or for
         * building complex dynamic queries.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("active", true),
         *     Filters.gt("age", 18)
         * );
         * String sql = MSB.parse(cond, User.class).sql();
         * // Output: active = #{active} AND age > #{age}
         * }</pre>
         * 
         * @param cond the condition to parse
         * @param entityClass the entity class for column name mapping
         * @return the SQLBuilder instance containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * MyBatis-style SQL builder with snake_case field/column naming strategy.
     * This class automatically converts camelCase property names to snake_case column names.
     * 
     * <p>This builder is ideal for databases that follow the snake_case naming convention
     * (e.g., user_name, first_name) while keeping Java property names in camelCase.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Property names are automatically converted to snake_case
     * String sql = MSC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(Filters.eq("userId", 1))
     *                 .sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" 
     * //         FROM account WHERE user_id = #{userId}
     * 
     * // INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = MSC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (first_name, last_name) VALUES (#{firstName}, #{lastName})
     * }</pre>
     * 
     * @deprecated Use {@link NSC} or other non-deprecated builders instead
     */
    @Deprecated
    public static class MSC extends SQLBuilder {

        /**
         * Package-private constructor for internal use only.
         * Creates a new MSC instance with snake_case naming policy and MyBatis SQL format.
         */
        MSC() {
            super(NamingPolicy.SNAKE_CASE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Factory method to create a new MSC instance.
         * 
         * @return a new MSC SQLBuilder instance
         */
        protected static MSC createInstance() {
            return new MSC();
        }

        /**
         * Creates an INSERT statement for a single column.
         * 
         * <p>The property name will be converted to snake_case for the column name.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.insert("userName").into("users").sql();
         * // Output: INSERT INTO users (user_name) VALUES (#{userName})
         * }</pre>
         *
         * @param expr the property name or expression to insert
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT statement for the specified properties.
         * 
         * <p>Property names will be converted to snake_case for column names,
         * while keeping the original names for parameter placeholders.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.insert("firstName", "lastName", "emailAddress")
         *                 .into("users")
         *                 .sql();
         * // Output: INSERT INTO users (first_name, last_name, email_address) 
         * //         VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         *
         * @param propOrColumnNames the property names to include in the INSERT
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT statement for properties provided as a collection.
         * 
         * <p>This method is useful when property names are determined at runtime.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> props = Arrays.asList("firstName", "lastName");
         * String sql = MSC.insert(props).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param propOrColumnNames collection of property names to include
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT statement using a map of properties to values.
         * 
         * <p>Map keys (property names) will be converted to snake_case for column names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("firstName", "John");
         * data.put("lastName", "Doe");
         * String sql = MSC.insert(data).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) 
         * //         VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param props map of property names to their values
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT statement based on an entity object.
         * 
         * <p>Property names from the entity will be converted to snake_case for column names.
         * Properties marked with {@code @Transient}, {@code @ReadOnly}, or {@code @ReadOnlyId}
         * annotations will be excluded.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User();
         * user.setFirstName("John");
         * user.setLastName("Doe");
         * String sql = MSC.insert(user).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         * 
         * @param entity the entity object containing data to insert
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT statement based on an entity object, excluding specified properties.
         * 
         * <p>Provides fine-grained control over which properties are included,
         * with automatic snake_case conversion for column names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * User user = new User();
         * user.setFirstName("John");
         * user.setLastName("Doe");
         * user.setCreatedDate(new Date());
         * Set<String> exclude = Set.of("createdDate");
         * String sql = MSC.insert(user, exclude).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param entity the entity object containing data to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement template for an entity class.
         * 
         * <p>Generates an INSERT template with all insertable properties,
         * automatically converting property names to snake_case column names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.insert(User.class).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name, email) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         * 
         * @param entityClass the entity class to generate INSERT for
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT statement template for an entity class, excluding specified properties.
         * 
         * <p>Allows selective property inclusion with automatic snake_case conversion.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = Set.of("id", "createdDate");
         * String sql = MSC.insert(User.class, exclude).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT statement with automatic table name detection.
         * 
         * <p>The table name is determined from the {@code @Table} annotation,
         * with the entity's property names converted to snake_case columns.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * @Table("users")
         * class User { 
         *     private String firstName;
         *     private String lastName;
         * }
         * 
         * String sql = MSC.insertInto(User.class).sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         * 
         * @param entityClass the entity class to insert
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT statement with automatic table name detection, excluding properties.
         * 
         * <p>Combines automatic table name detection with selective property inclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = Set.of("id", "version");
         * String sql = MSC.insertInto(User.class, exclude).sql();
         * // Output: INSERT INTO users (first_name, last_name) VALUES (#{firstName}, #{lastName})
         * }</pre>
         * 
         * @param entityClass the entity class to insert
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT statement for multiple records.
         * 
         * <p>Generates a single INSERT with multiple value sets, with property names
         * converted to snake_case for column names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<User> users = Arrays.asList(
         *     new User("John", "Doe"),
         *     new User("Jane", "Smith")
         * );
         * String sql = MSC.batchInsert(users).into("users").sql();
         * // Output: INSERT INTO users (first_name, last_name) 
         * //         VALUES (#{firstName}, #{lastName}), 
         * //                (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param propsList collection of entities or property maps to insert
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propsList is null or empty
         *
         * <p>
         * <b>Note:</b> This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE statement for the specified table.
         * 
         * <p>Use {@code set()} to specify columns to update. Column names in conditions
         * will be converted from camelCase to snake_case.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.update("users")
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .where(Filters.eq("userId", 123))
         *                 .sql();
         * // Output: UPDATE users SET first_name = #{firstName}, last_name = #{lastName} 
         * //         WHERE user_id = #{userId}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE statement for a table with entity class context.
         * 
         * <p>Property names will be automatically converted to snake_case column names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.update("users", User.class)
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = #{firstName}, last_name = #{lastName} WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty, or if entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE statement for an entity class.
         * 
         * <p>All updatable properties will be included in the SET clause,
         * with automatic conversion to snake_case column names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.update(User.class)
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = #{firstName}, last_name = #{lastName} WHERE id = #{id}
         * }</pre>
         * 
         * @param entityClass the entity class to update
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE statement for an entity class, excluding specified properties.
         * 
         * <p>Provides control over which properties to update, with automatic
         * snake_case conversion for column names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = Set.of("id", "createdDate");
         * String sql = MSC.update(User.class, exclude)
         *                 .set("firstName", "John")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE users SET first_name = #{firstName}, last_name = #{lastName} WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the update
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE statement for the specified table.
         * 
         * <p>Property names in WHERE conditions will be converted to snake_case column names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.deleteFrom("users")
         *                 .where(Filters.eq("userId", 123))
         *                 .sql();
         * // Output: DELETE FROM users WHERE user_id = #{userId}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE statement for a table with entity class context.
         * 
         * <p>Provides property name mapping for WHERE conditions.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.deleteFrom("users", User.class)
         *                 .where(Filters.eq("userId", 123))
         *                 .sql();
         * // Output: DELETE FROM users WHERE user_id = #{userId}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty, or if entityClass is null
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE statement for an entity class.
         * 
         * <p>The table name is determined from the {@code @Table} annotation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.deleteFrom(User.class)
         *                 .where(Filters.eq("id", 123))
         *                 .sql();
         * // Output: DELETE FROM users WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to delete from
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT statement with a single expression or column.
         * 
         * <p>This method accepts raw SQL expressions or column names.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.select("COUNT(*)").from("users").sql();
         * // Output: SELECT count(*) FROM users
         * 
         * String sql2 = MSC.select("firstName").from("users").sql();
         * // Output: SELECT first_name AS "firstName" FROM users
         * }</pre>
         *
         * @param selectPart the SQL expression or column name to select
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT statement for the specified columns or properties.
         * 
         * <p>Property names will be converted to snake_case column names with aliases.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.select("firstName", "lastName", "emailAddress")
         *                 .from("users")
         *                 .sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", 
         * //               email_address AS "emailAddress" FROM users
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to select
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT statement for columns provided as a collection.
         * 
         * <p>Useful when column names are determined at runtime.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName");
         * String sql = MSC.select(columns).from("users").sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM users
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT statement with custom column aliases.
         * 
         * <p>Map keys are property/column names, values are their aliases.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = MSC.select(aliases).from("users").sql();
         * // Output: SELECT first_name AS "fname", last_name AS "lname" FROM users
         * }</pre>
         *
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT statement for all properties of an entity class.
         * 
         * <p>Properties marked with {@code @Transient} annotation will be excluded.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.select(User.class).from("users").sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName", 
         * //               email AS "email" FROM users
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT statement for an entity class with sub-entity control.
         * 
         * <p>When includeSubEntityProperties is true, properties of embedded entities
         * will also be included in the selection.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.select(User.class, true).from("users").sql();
         * // Will include properties from any embedded entities
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement for an entity class, excluding specified properties.
         * 
         * <p>Allows fine-grained control over which properties to include in the selection.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = Set.of("password", "secretKey");
         * String sql = MSC.select(User.class, exclude).from("users").sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM users
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from selection
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement for an entity class with full control options.
         * 
         * <p>Combines sub-entity inclusion control with property exclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = Set.of("internalData");
         * String sql = MSC.select(User.class, true, exclude).from("users").sql();
         * // Includes sub-entity properties but excludes specified fields
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @param excludedPropNames set of property names to exclude from selection
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT statement with automatic table name detection.
         * 
         * <p>The table name is determined from the {@code @Table} annotation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * @Table("users")
         * class User { ... }
         * 
         * String sql = MSC.selectFrom(User.class).sql();
         * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM users
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a complete SELECT statement with table alias.
         * 
         * <p>The table alias will be used in the FROM clause and can be referenced
         * in WHERE conditions.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.selectFrom(User.class, "u")
         *                 .where(Filters.eq("u.active", true))
         *                 .sql();
         * // Output: SELECT u.first_name AS "firstName", u.last_name AS "lastName" 
         * //         FROM users u WHERE u.active = #{u.active}
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a SELECT statement with sub-entity inclusion control.
         * 
         * <p>When includeSubEntityProperties is true, performs joins for embedded entities.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.selectFrom(User.class, true)
         *                 .where(Filters.eq("active", true))
         *                 .sql();
         * // Includes sub-entity properties with automatic joins
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement with table alias and sub-entity control.
         * 
         * <p>Combines table aliasing with sub-entity property inclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.selectFrom(User.class, "u", true)
         *                 .where(Filters.like("u.email", "%@example.com"))
         *                 .sql();
         * // Uses alias 'u' and includes sub-entity properties
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT statement excluding specified properties.
         * 
         * <p>Automatically determines table name from {@code @Table} annotation.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = Set.of("largeBlob", "tempData");
         * String sql = MSC.selectFrom(User.class, exclude)
         *                 .where(Filters.gt("createdDate", someDate))
         *                 .sql();
         * // Selects all properties except excluded ones
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement with table alias and property exclusion.
         * 
         * <p>Combines table aliasing with selective property inclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = Set.of("password");
         * String sql = MSC.selectFrom(User.class, "u", exclude)
         *                 .innerJoin("orders o").on("u.id = o.user_id")
         *                 .sql();
         * // Uses alias and excludes password field
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a SELECT statement with sub-entity control and property exclusion.
         * 
         * <p>Provides full control over property selection with automatic table detection.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = Set.of("debug", "temp");
         * String sql = MSC.selectFrom(User.class, true, exclude)
         *                 .orderBy("last_name")
         *                 .sql();
         * // Includes sub-entities but excludes specified fields
         * }</pre>
         * 
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a SELECT statement with full control over all options.
         * 
         * <p>This is the most comprehensive selectFrom method, providing control over
         * table alias, sub-entity inclusion, and property exclusion.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> exclude = Set.of("internal");
         * String sql = MSC.selectFrom(User.class, "u", true, exclude)
         *                 .leftJoin("address a").on("u.address_id = a.id")
         *                 .where(Filters.isNotNull("a.city"))
         *                 .sql();
         * // Full control over alias, sub-entities, and excluded properties
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties from embedded entities
         * @param excludedPropNames set of property names to exclude
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.SNAKE_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT statement for joining two entity classes.
         * 
         * <p>This method sets up a join between two tables with specified aliases
         * for both table names and result set mapping.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.select(User.class, "u", "user", 
         *                        Order.class, "o", "order")
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .sql();
         * // Output: SELECT u.first_name AS "user.firstName", u.last_name AS "user.lastName",
         * //               o.order_id AS "order.orderId", o.total AS "order.total" 
         * //         FROM users u JOIN orders o ON u.id = o.user_id
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT statement for joining two entity classes with property exclusion.
         * 
         * <p>Provides control over which properties to include from each entity in the join.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> userExclude = Set.of("password");
         * Set<String> orderExclude = Set.of("internalNotes");
         * String sql = MSC.select(User.class, "u", "user", userExclude,
         *                        Order.class, "o", "order", orderExclude)
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .sql();
         * // Joins two tables excluding specified properties
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT statement for multiple entity selections.
         * 
         * <p>This method supports complex queries involving multiple entities with
         * individual configuration for each selection.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(User.class, "u", "user", null, false, null),
         *     new Selection(Order.class, "o", "order", null, false, null),
         *     new Selection(Product.class, "p", "product", null, false, Set.of("description"))
         * );
         * String sql = MSC.select(selections)
         *                 .from("users u")
         *                 .join("orders o").on("u.id = o.user_id")
         *                 .join("products p").on("o.product_id = p.id")
         *                 .sql();
         * // Complex multi-table query with custom configurations
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining entities and their configurations
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT statement for joining two entities with automatic FROM clause.
         * 
         * <p>Automatically generates the FROM clause based on entity annotations.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.selectFrom(User.class, "u", "user",
         *                           Order.class, "o", "order")
         *                 .on("u.id = o.user_id")
         *                 .where(Filters.gt("o.total", 100))
         *                 .sql();
         * // Automatic FROM clause with proper table names
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @return the SQLBuilder instance for method chaining
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a complete SELECT statement for joining entities with property exclusion.
         * 
         * <p>Combines automatic FROM clause generation with selective property inclusion.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludeUser = Set.of("passwordHash");
         * String sql = MSC.selectFrom(User.class, "u", "user", excludeUser,
         *                           Order.class, "o", "order", null)
         *                 .on("u.id = o.user_id")
         *                 .sql();
         * // Automatic FROM with excluded properties
         * }</pre>
         * 
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClassA is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a complete SELECT statement for multiple entities with automatic FROM clause.
         * 
         * <p>Generates both SELECT and FROM clauses based on the provided selections.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = createSelections();
         * String sql = MSC.selectFrom(selections)
         *                 .where(complexConditions)
         *                 .orderBy("user.last_name")
         *                 .sql();
         * // Fully automatic multi-table query generation
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining entities and their configurations
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.SNAKE_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a SELECT COUNT(*) statement for the specified table.
         * 
         * <p>Generates a count query to get the total number of rows in a table.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.count("users").sql();
         * // Output: SELECT count(*) FROM users
         * 
         * String sql2 = MSC.count("users")
         *                  .where(Filters.eq("active", true))
         *                  .sql();
         * // Output: SELECT count(*) FROM users WHERE active = #{active}
         * }</pre>
         *
         * @param tableName the name of the table to count
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a SELECT COUNT(*) statement for an entity class.
         * 
         * <p>The table name is determined from the {@code @Table} annotation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MSC.count(User.class)
         *                 .where(Filters.gt("age", 18))
         *                 .sql();
         * // Output: SELECT count(*) FROM users WHERE age > #{age}
         * }</pre>
         *
         * @param entityClass the entity class to count
         * @return the SQLBuilder instance for method chaining
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Generates SQL for a condition only, without a complete statement.
         * 
         * <p>This method is useful for generating WHERE clause fragments or
         * testing condition SQL generation.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("firstName", "John"),
         *     Filters.gt("age", 18)
         * );
         * String sql = MSC.parse(cond, User.class).sql();
         * // Output: first_name = #{firstName} AND age > #{age}
         * }</pre>
         * 
         * @param cond the condition to generate SQL for
         * @param entityClass the entity class for property mapping
         * @return the SQLBuilder instance containing only the condition SQL
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * MyBatis-style SQL builder with all capital case (upper case with underscore) field/column naming strategy.
     * 
     * <p>This builder generates SQL with MyBatis-style parameter placeholders (#{paramName}) and converts
     * property names to SCREAMING_SNAKE_CASE format. For example, a property named "firstName" 
     * will be converted to "FIRST_NAME" in the SQL.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate SELECT with column aliasing
     * String sql = MAC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(Filters.eq("id", 1))
     *                 .sql();
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = #{id}
     * 
     * // Generate INSERT with entity
     * Account account = new Account();
     * account.setFirstName("John");
     * account.setLastName("Doe");
     * String sql = MAC.insert(account).into("ACCOUNT").sql();
     * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (#{firstName}, #{lastName})
     * }</pre>
     * 
     * @deprecated Use {@link NAC} (Named SQL with All Caps) instead for better clarity
     */
    @Deprecated
    public static class MAC extends SQLBuilder {

        /**
         * Creates a new instance of MAC SQL builder.
         * Internal constructor - use static factory methods instead.
         */
        MAC() {
            super(NamingPolicy.SCREAMING_SNAKE_CASE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Creates a new MAC instance.
         * Internal factory method.
         * 
         * @return a new MAC SQL builder instance
         */
        protected static MAC createInstance() {
            return new MAC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * 
         * <p>The column name will be converted to SCREAMING_SNAKE_CASE format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.insert("firstName").into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME) VALUES (#{firstName})
         * }</pre>
         *
         * @param expr the column expression or property name to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified columns.
         * 
         * <p>Column names will be converted to SCREAMING_SNAKE_CASE format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.insert("firstName", "lastName", "email")
         *                 .into("ACCOUNT")
         *                 .sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified collection of columns.
         * 
         * <p>Useful when column names are determined at runtime. Column names will be 
         * converted to SCREAMING_SNAKE_CASE format.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "email");
         * String sql = MAC.insert(columns).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder using a map of property names to values.
         * 
         * <p>Map keys will be converted to SCREAMING_SNAKE_CASE format for column names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * String sql = MAC.insert(props).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param props map of property names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the given entity object.
         * All non-null properties of the entity will be included in the INSERT statement.
         * 
         * <p>Property names will be converted to SCREAMING_SNAKE_CASE format for column names.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * String sql = MAC.insert(account).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder for the given entity object, excluding specified properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setId(1L);
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * Set<String> excludes = new HashSet<>(Arrays.asList("id"));
         * String sql = MAC.insert(account, excludes).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class.
         * All properties marked as insertable will be included.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.insert(Account.class).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class, excluding specified properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "createTime"));
         * String sql = MAC.insert(Account.class, excludes).into("ACCOUNT").sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with automatic table name detection.
         * The table name is derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.insertInto(Account.class).sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with automatic table name detection,
         * excluding specified properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = MAC.insertInto(Account.class, excludes).sql();
         * // Output: INSERT INTO ACCOUNT (FIRST_NAME, LAST_NAME, EMAIL) VALUES (#{firstName}, #{lastName}, #{email})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Creates a batch INSERT SQL builder for multiple entities or property maps.
         * Generates MySQL-style batch insert syntax with camelCase column names.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "Doe"),
         *     new Account("Jane", "Smith")
         * );
         * String sql = MAC.batchInsert(accounts).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName) VALUES (#{firstName_0}, #{lastName_0}), (#{firstName_1}, #{lastName_1})
         * }</pre>
         *
         * @param propsList collection of entities or property maps to batch insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * @deprecated This is a beta feature and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.update("account")
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .set("modifiedDate", new Date())
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName}, lastName = #{lastName}, modifiedDate = #{modifiedDate} WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class mapping.
         * The entity class is used for property name validation and type checking.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.update("account", Account.class)
         *                 .set("isActive", false)
         *                 .set("deactivatedDate", new Date())
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET isActive = #{isActive}, deactivatedDate = #{deactivatedDate} WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty, or if entityClass is null
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class.
         * The table name is derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.update(Account.class)
         *                 .set("status", "ACTIVE")
         *                 .set("lastLoginDate", new Date())
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET status = #{status}, lastLoginDate = #{lastLoginDate} WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class, excluding specified properties.
         * Properties marked with @NonUpdatable or in the excluded set will not be updated.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "createdDate", "createdBy"));
         * String sql = MAC.update(Account.class, excludes)
         *                 .set("firstName", "John")
         *                 .set("modifiedDate", new Date())
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName}, modifiedDate = #{modifiedDate} WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to update
         * @param excludedPropNames set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.deleteFrom("account")
         *                 .where(Filters.eq("status", "INACTIVE"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE status = #{status}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table with entity class mapping.
         * The entity class is used for property name to column name mapping in WHERE conditions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.deleteFrom("account", Account.class)
         *                 .where(Filters.and(
         *                     Filters.eq("isActive", false),
         *                     Filters.lt("lastLoginDate", lastYear)
         *                 ))
         *                 .sql();
         * // Output: DELETE FROM account WHERE isActive = #{isActive} AND lastLoginDate < #{lastLoginDate}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for property mapping
         * @return a new SQLBuilder instance configured for DELETE operation
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified entity class.
         * The table name is derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.deleteFrom(Account.class)
         *                 .where(Filters.in("id", Arrays.asList(1, 2, 3)))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id IN (#{id_0}, #{id_1}, #{id_2})
         * }</pre>
         *
         * @param entityClass the entity class to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a single column or expression.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.select("COUNT(*)").from("account").sql();
         * // Output: SELECT count(*) FROM account
         *
         * String sql2 = MAC.select("firstName").from("account").sql();
         * // Output: SELECT firstName FROM account
         * }</pre>
         *
         * @param selectPart the column name or SQL expression to select
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder with multiple columns.
         * Column names remain in camelCase format.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.select("firstName", "lastName", "emailAddress")
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT firstName, lastName, emailAddress FROM account
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a collection of columns.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
         * String sql = MAC.select(columns).from("account").sql();
         * // Output: SELECT firstName, lastName, phoneNumber FROM account
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * aliases.put("emailAddress", "email");
         * String sql = MAC.select(aliases).from("account").sql();
         * // Output: SELECT firstName AS fname, lastName AS lname, emailAddress AS email FROM account
         * }</pre>
         *
         * @param propOrColumnNameAliases map of property/column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of the specified entity class.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.select(Account.class).from("account").sql();
         * // Output: SELECT id, firstName, lastName, emailAddress, createdDate FROM account
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class with optional sub-entity properties.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Without sub-entities
         * String sql = MAC.select(Order.class, false).from("orders").sql();
         * // Output: SELECT id, customerId, orderDate, totalAmount FROM orders
         *
         * // With sub-entities (includes properties from related entities)
         * String sql = MAC.select(Order.class, true).from("orders").sql();
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class, excluding specified properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("passwordHash", "securityToken"));
         * String sql = MAC.select(Account.class, excludes).from("account").sql();
         * // Output: SELECT id, firstName, lastName, emailAddress FROM account
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class with full control over included properties.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes", "debugInfo"));
         * String sql = MAC.select(Customer.class, true, excludes)
         *                 .from("customer")
         *                 .sql();
         * // Selects all Customer properties and sub-entity properties, except excluded ones
         * }</pre>
         *
         * @param entityClass the entity class to select properties from
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT FROM SQL builder for the specified entity class.
         * Automatically determines the table name from the entity class.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.selectFrom(Account.class).where(Filters.eq("isActive", true)).sql();
         * // Output: SELECT id, firstName, lastName, emailAddress FROM account WHERE isActive = #{isActive}
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with a table alias.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.selectFrom(Account.class, "a")
         *                 .where(Filters.like("a.emailAddress", "%@example.com"))
         *                 .sql();
         * // Output: SELECT a.id, a.firstName, a.lastName, a.emailAddress FROM account a WHERE a.emailAddress LIKE #{emailAddress}
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with optional sub-entity properties.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.selectFrom(Order.class, true)
         *                 .where(Filters.between("orderDate", startDate, endDate))
         *                 .sql();
         * // Includes Order properties and related sub-entity properties
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with table alias and sub-entity control.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.selectFrom(Product.class, "p", true)
         *                 .innerJoin("category", "c").on("p.categoryId = c.id")
         *                 .where(Filters.eq("c.isActive", true))
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with property exclusions.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("largeJsonData", "binaryContent"));
         * String sql = MAC.selectFrom(Document.class, excludes)
         *                 .where(Filters.eq("documentType", "PDF"))
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with table alias and property exclusions.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("encryptedData"));
         * String sql = MAC.selectFrom(User.class, "u", excludes)
         *                 .leftJoin("user_roles", "ur").on("u.id = ur.userId")
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with sub-entity control and property exclusions.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("auditLog"));
         * String sql = MAC.selectFrom(Invoice.class, true, excludes)
         *                 .where(Filters.eq("isPaid", false))
         *                 .orderBy("dueDate")
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a complete SELECT FROM SQL builder with full control over all options.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("tempData"));
         * String sql = MAC.selectFrom(Transaction.class, "t", true, excludes)
         *                 .innerJoin("account", "a").on("t.accountId = a.id")
         *                 .where(Filters.and(
         *                     Filters.eq("a.isActive", true),
         *                     Filters.gt("t.amount", 1000)
         *                 ))
         *                 .sql();
         * }</pre>
         *
         * @param entityClass the entity class to select from
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties from related sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT FROM operation
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.SCREAMING_SNAKE_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.select(Order.class, "o", "order",
         *                        Customer.class, "c", "customer")
         *                 .from("orders o")
         *                 .innerJoin("customers c").on("o.customerId = c.id")
         *                 .sql();
         * // Selects columns from both entities with proper aliasing
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for the first entity's columns
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for the second entity's columns
         * @return a new SQLBuilder instance configured for multi-entity SELECT
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for joining two entity classes with property exclusions.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> orderExcludes = new HashSet<>(Arrays.asList("internalNotes"));
         * Set<String> customerExcludes = new HashSet<>(Arrays.asList("creditCardInfo"));
         * String sql = MAC.select(Order.class, "o", "order", orderExcludes,
         *                        Customer.class, "c", "customer", customerExcludes)
         *                 .from("orders o")
         *                 .innerJoin("customers c").on("o.customerId = c.id")
         *                 .where(Filters.gt("o.totalAmount", 500))
         *                 .sql();
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for the first entity's columns
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for the second entity's columns
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections with custom configurations.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Order.class, "o", "order", null, true, null),
         *     new Selection(Customer.class, "c", "customer", null, false, excludedProps),
         *     new Selection(Product.class, "p", "product", null, false, null)
         * );
         * String sql = MAC.select(selections)
         *                 .from("orders o")
         *                 .innerJoin("customers c").on("o.customerId = c.id")
         *                 .innerJoin("order_items oi").on("o.id = oi.orderId")
         *                 .innerJoin("products p").on("oi.productId = p.id")
         *                 .sql();
         * }</pre>
         *
         * @param multiSelects list of Selection configurations for multiple entities
         * @return a new SQLBuilder instance configured for multi-entity SELECT
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT FROM SQL builder for joining two entity classes.
         * Automatically generates the FROM clause with proper table names.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.selectFrom(Order.class, "o", "order",
         *                            Customer.class, "c", "customer")
         *                 .on("o.customerId = c.id")
         *                 .where(Filters.eq("c.country", "USA"))
         *                 .sql();
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for the first entity's columns
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for the second entity's columns
         * @return a new SQLBuilder instance configured for multi-entity SELECT FROM
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a complete SELECT FROM SQL builder for joining two entity classes with property exclusions.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> orderExcludes = new HashSet<>(Arrays.asList("tempData"));
         * Set<String> productExcludes = new HashSet<>(Arrays.asList("warehouseNotes"));
         * String sql = MAC.selectFrom(Order.class, "o", "order", orderExcludes,
         *                            Product.class, "p", "product", productExcludes)
         *                 .innerJoin("order_items", "oi").on("o.id = oi.orderId")
         *                 .on("oi.productId = p.id")
         *                 .sql();
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the class alias prefix for the first entity's columns
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the class alias prefix for the second entity's columns
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT FROM
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a complete SELECT FROM SQL builder for multiple entity selections.
         * Automatically generates the FROM clause based on the Selection configurations.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Department.class, "d", null, null, false, null),
         *     new Selection(Employee.class, "e", null, null, true, excludedProps),
         *     new Selection(Project.class, "p", null, null, false, null)
         * );
         * String sql = MAC.selectFrom(selections)
         *                 .on("d.id = e.departmentId")
         *                 .leftJoin("employee_projects", "ep").on("e.id = ep.employeeId")
         *                 .on("ep.projectId = p.id")
         *                 .sql();
         * }</pre>
         *
         * @param multiSelects list of Selection configurations for multiple entities
         * @return a new SQLBuilder instance configured for multi-entity SELECT FROM
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.SCREAMING_SNAKE_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.count("account").where(Filters.eq("isActive", true)).sql();
         * // Output: SELECT count(*) FROM account WHERE isActive = #{isActive}
         * }</pre>
         *
         * @param tableName the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified entity class.
         * The table name is derived from the entity class.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MAC.count(Account.class)
         *                 .where(Filters.or(
         *                     Filters.isNull("emailAddress"),
         *                     Filters.eq("emailVerified", false)
         *                 ))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE emailAddress IS NULL OR emailVerified = #{emailVerified}
         * }</pre>
         *
         * @param entityClass the entity class to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL format using the specified entity class for property mapping.
         * This is useful for generating WHERE clause fragments with camelCase naming.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("isActive", true),
         *     Filters.like("emailAddress", "%@company.com"),
         *     Filters.gt("accountBalance", 0)
         * );
         * String sql = MAC.parse(cond, Account.class).sql();
         * // Output: isActive = #{isActive} AND emailAddress LIKE #{emailAddress} AND accountBalance > #{accountBalance}
         * }</pre>
         *
         * @param cond the condition to parse
         * @param entityClass the entity class for property to column mapping
         * @return a new SQLBuilder instance containing the parsed condition
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }

    /**
     * MyBatis-style SQL builder with lower camel case field/column naming strategy.
     * 
     * <p>This builder generates SQL with MyBatis-style parameter placeholders (#{paramName}) and uses
     * camelCase naming convention. Property names are used as-is without transformation to 
     * snake_case or upper case.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Property names remain in camelCase
     * String sql = MLC.select("firstName", "lastName")
     *                 .from("account")
     *                 .where(Filters.eq("id", 1))
     *                 .sql();
     * // Output: SELECT firstName, lastName FROM account WHERE id = #{id}
     * 
     * // INSERT with camelCase columns
     * Account account = new Account();
     * account.setFirstName("John");
     * String sql = MLC.insert(account).into("account").sql();
     * // Output: INSERT INTO account (firstName, lastName) VALUES (#{firstName}, #{lastName})
     * }</pre>
     * 
     * @deprecated Use {@link NLC} (Named SQL with Lower Camel) instead for better clarity
     */
    @Deprecated
    public static class MLC extends SQLBuilder {

        /**
         * Creates a new instance of MLC SQL builder.
         * Internal constructor - use static factory methods instead.
         */
        MLC() {
            super(NamingPolicy.CAMEL_CASE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Creates a new MLC instance.
         * Internal factory method.
         * 
         * @return a new MLC SQL builder instance
         */
        protected static MLC createInstance() {
            return new MLC();
        }

        /**
         * Creates an INSERT SQL builder for a single column expression.
         * The column name remains in camelCase format and uses MyBatis-style parameter placeholder.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.insert("firstName").into("account").sql();
         * // Output: INSERT INTO account (firstName) VALUES (#{firstName})
         * }</pre>
         *
         * @param expr the column expression or property name to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if expr is null or empty
         */
        public static SQLBuilder insert(final String expr) {
            N.checkArgNotEmpty(expr, INSERTION_PART_MSG);

            return insert(N.asArray(expr));
        }

        /**
         * Creates an INSERT SQL builder for the specified columns.
         * Column names remain in camelCase format and use MyBatis-style parameter placeholders.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.insert("firstName", "lastName", "emailAddress")
         *                 .into("account")
         *                 .sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress) VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified collection of columns.
         * Column names remain in camelCase format and use MyBatis-style parameter placeholders.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = Arrays.asList("firstName", "lastName", "phoneNumber");
         * String sql = MLC.insert(columns).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, phoneNumber) VALUES (#{firstName}, #{lastName}, #{phoneNumber})
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder insert(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder using a map of property names to values.
         * The map keys are used as column names in camelCase format with MyBatis-style placeholders.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> props = new HashMap<>();
         * props.put("firstName", "John");
         * props.put("lastName", "Doe");
         * props.put("isActive", true);
         * String sql = MLC.insert(props).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, isActive) VALUES (#{firstName}, #{lastName}, #{isActive})
         * }</pre>
         *
         * @param props map of property names to their values
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if props is null or empty
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            N.checkArgNotEmpty(props, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance._props = props;

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the given entity object.
         * All non-null properties of the entity will be included in the INSERT statement
         * with camelCase column names and MyBatis-style placeholders.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * account.setCreatedDate(new Date());
         * String sql = MLC.insert(account).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, createdDate) VALUES (#{firstName}, #{lastName}, #{createdDate})
         * }</pre>
         *
         * @param entity the entity object to insert
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         * Creates an INSERT SQL builder for the given entity object, excluding specified properties.
         * Properties not in the exclude set will be included with camelCase names and MyBatis-style placeholders.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Account account = new Account();
         * account.setId(1L);   // Will be excluded
         * account.setFirstName("John");
         * account.setLastName("Doe");
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = MLC.insert(account, excludes).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName) VALUES (#{firstName}, #{lastName})
         * }</pre>
         *
         * @param entity the entity object to insert
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entity is null
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entity, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entity.getClass());

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class.
         * All properties marked as insertable will be included with camelCase names and MyBatis-style placeholders.
         * Properties annotated with @ReadOnly, @ReadOnlyId, or @Transient are automatically excluded.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.insert(Account.class).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress, createdDate) VALUES (#{firstName}, #{lastName}, #{emailAddress}, #{createdDate})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class, excluding specified properties.
         * Properties not in the exclude set and not annotated as read-only will be included
         * with camelCase names and MyBatis-style placeholders.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "auditFields"));
         * String sql = MLC.insert(Account.class, excludes).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress) VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getInsertPropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with automatic table name detection.
         * The table name is derived from the entity class name or @Table annotation.
         * All insertable properties are included with camelCase names and MyBatis-style placeholders.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.insertInto(Account.class).sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress) VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         * Creates an INSERT SQL builder for the specified entity class with automatic table name detection,
         * excluding specified properties. The table name is derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("id", "version"));
         * String sql = MLC.insertInto(Account.class, excludes).sql();
         * // Output: INSERT INTO account (firstName, lastName, emailAddress) VALUES (#{firstName}, #{lastName}, #{emailAddress})
         * }</pre>
         *
         * @param entityClass the entity class to generate INSERT for
         * @param excludedPropNames set of property names to exclude from the INSERT
         * @return a new SQLBuilder instance configured for INSERT operation with table name set
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generates MySQL-style batch insert SQL for multiple entities or property maps.
         * This method creates a single INSERT statement with multiple value rows for efficient batch insertion.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Account> accounts = Arrays.asList(
         *     new Account("John", "Doe"),
         *     new Account("Jane", "Smith")
         * );
         * String sql = MLC.batchInsert(accounts).into("account").sql();
         * // Output: INSERT INTO account (firstName, lastName) VALUES (#{firstName_0}, #{lastName_0}), (#{firstName_1}, #{lastName_1})
         * }</pre>
         *
         * @param propsList list of entities or properties maps to batch insert
         * @return a new SQLBuilder instance configured for batch INSERT operation
         * @throws IllegalArgumentException if propsList is null or empty
         * <p>
         * <b>Note:</b> This API is in beta and may change in future versions
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            N.checkArgNotEmpty(propsList, INSERTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (first.isPresent() && Beans.isBeanClass(first.get().getClass())) {
                instance.setEntityClass(first.get().getClass());
            }

            instance._propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table.
         * Use the {@code set()} method to specify which columns to update.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.update("account")
         *                 .set("firstName", "updatedName")
         *                 .set("modifiedDate", new Date())
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName}, modifiedDate = #{modifiedDate} WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified table with entity class context.
         * The entity class provides metadata for property-to-column mapping.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.update("account", Account.class)
         *                 .set("firstName", "John")
         *                 .set("lastName", "Doe")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName}, lastName = #{lastName} WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to update
         * @param entityClass the entity class for column mapping metadata
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder update(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, UPDATE_PART_MSG);
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * All updatable properties (excluding @ReadOnly, @NonUpdatable) are included.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.update(Account.class)
         *                 .set("firstName", "John")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName} WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to generate UPDATE for
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         * Creates an UPDATE SQL builder for the specified entity class, excluding specified properties.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * Properties in the exclude set or annotated as non-updatable are not included.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("createdDate", "createdBy"));
         * String sql = MLC.update(Account.class, excludes)
         *                 .set("firstName", "John")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: UPDATE account SET firstName = #{firstName} WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to generate UPDATE for
         * @param excludedPropNames set of property names to exclude from the UPDATE
         * @return a new SQLBuilder instance configured for UPDATE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, UPDATE_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.UPDATE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);
            instance._propOrColumnNames = QueryUtil.getUpdatePropNames(entityClass, excludedPropNames);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.deleteFrom("account")
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id = #{id}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified table with entity class context.
         * The entity class provides metadata for property-to-column mapping in WHERE conditions.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.deleteFrom("account", Account.class)
         *                 .where(Filters.eq("emailAddress", "john@example.com"))
         *                 .sql();
         * // Output: DELETE FROM account WHERE emailAddress = #{emailAddress}
         * }</pre>
         *
         * @param tableName the name of the table to delete from
         * @param entityClass the entity class for column mapping metadata
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass) {
            N.checkArgNotEmpty(tableName, DELETION_PART_MSG);
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance._tableName = tableName;
            instance.setEntityClass(entityClass);

            return instance;
        }

        /**
         * Creates a DELETE SQL builder for the specified entity class.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.deleteFrom(Account.class)
         *                 .where(Filters.eq("id", 1))
         *                 .sql();
         * // Output: DELETE FROM account WHERE id = #{id}
         * }</pre>
         *
         * @param entityClass the entity class to generate DELETE for
         * @return a new SQLBuilder instance configured for DELETE operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, DELETION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.DELETE;
            instance.setEntityClass(entityClass);
            instance._tableName = getTableName(entityClass, instance._namingPolicy);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with a custom select expression.
         * This method allows for complex select expressions including functions and calculations.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.select("COUNT(*) AS total, MAX(createdDate) AS latest")
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT COUNT(*) AS total, MAX(createdDate) AS latest FROM account
         * }</pre>
         *
         * @param selectPart the custom select expression
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if selectPart is null or empty
         */
        public static SQLBuilder select(final String selectPart) {
            N.checkArgNotEmpty(selectPart, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(selectPart);
            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified columns.
         * Column names remain in camelCase format.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.select("firstName", "lastName", "emailAddress")
         *                 .from("account")
         *                 .where(Filters.gt("createdDate", someDate))
         *                 .sql();
         * // Output: SELECT firstName, lastName, emailAddress FROM account WHERE createdDate > #{createdDate}
         * }</pre>
         *
         * @param propOrColumnNames the property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final String... propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = Array.asList(propOrColumnNames);

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for the specified collection of columns.
         * This is useful when column names are dynamically determined.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> columns = getColumnsToSelect();
         * String sql = MLC.select(columns)
         *                 .from("account")
         *                 .orderBy("createdDate DESC")
         *                 .sql();
         * // Output: SELECT firstName, lastName, emailAddress FROM account ORDER BY createdDate DESC
         * }</pre>
         *
         * @param propOrColumnNames collection of property or column names to select
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNames is null or empty
         */
        public static SQLBuilder select(final Collection<String> propOrColumnNames) {
            N.checkArgNotEmpty(propOrColumnNames, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNames = propOrColumnNames;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder with column aliases.
         * The map keys are column names and values are their aliases.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> aliases = new HashMap<>();
         * aliases.put("firstName", "fname");
         * aliases.put("lastName", "lname");
         * String sql = MLC.select(aliases)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT firstName AS fname, lastName AS lname FROM account
         * }</pre>
         *
         * @param propOrColumnNameAliases map of column names to their aliases
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if propOrColumnNameAliases is null or empty
         */
        public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases) {
            N.checkArgNotEmpty(propOrColumnNameAliases, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance._propOrColumnNameAliases = propOrColumnNameAliases;

            return instance;
        }

        /**
         * Creates a SELECT SQL builder for all properties of the specified entity class.
         * Properties annotated with @Transient are automatically excluded.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.select(Account.class)
         *                 .from("account")
         *                 .where(Filters.eq("isActive", true))
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, emailAddress, isActive FROM account WHERE isActive = #{isActive}
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT for
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class with optional sub-entity inclusion.
         * When includeSubEntityProperties is true, properties of embedded entities are also selected.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.select(Account.class, true)
         *                 .from("account")
         *                 .innerJoin("address").on("account.addressId = address.id")
         *                 .sql();
         * // Output: SELECT account.*, address.* FROM account INNER JOIN address ON account.addressId = address.id
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class, excluding specified properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("password", "secretKey"));
         * String sql = MLC.select(Account.class, excludes)
         *                 .from("account")
         *                 .sql();
         * // Output: SELECT id, firstName, lastName, emailAddress FROM account
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT for
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a SELECT SQL builder for the specified entity class with sub-entity inclusion control
         * and property exclusion.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = MLC.select(Account.class, true, excludes)
         *                 .from("account")
         *                 .innerJoin("profile").on("account.profileId = profile.id")
         *                 .sql();
         * // Output: SELECT account columns except internalNotes, profile.* FROM account INNER JOIN profile ON account.profileId = profile.id
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(entityClass);
            instance._propOrColumnNames = QueryUtil.getSelectPropNames(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for the specified entity class.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class)
         *                 .where(Filters.eq("isActive", true))
         *                 .orderBy("createdDate DESC")
         *                 .sql();
         * // Output: SELECT * FROM account WHERE isActive = #{isActive} ORDER BY createdDate DESC
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for the specified entity class with table alias.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class, "a")
         *                 .innerJoin("profile p").on("a.profileId = p.id")
         *                 .where(Filters.eq("a.isActive", true))
         *                 .sql();
         * // Output: SELECT a.* FROM account a INNER JOIN profile p ON a.profileId = p.id WHERE a.isActive = #{isActive}
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param alias the table alias to use
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias) {
            return selectFrom(entityClass, alias, false);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with sub-entity inclusion control.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class, true)
         *                 .innerJoin("address").on("account.addressId = address.id")
         *                 .sql();
         * // Output: SELECT account.*, address.* FROM account INNER JOIN address ON account.addressId = address.id
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with table alias and sub-entity inclusion control.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class, "acc", true)
         *                 .innerJoin("profile p").on("acc.profileId = p.id")
         *                 .sql();
         * // Output: SELECT acc.*, p.* FROM account acc INNER JOIN profile p ON acc.profileId = p.id
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, alias, includeSubEntityProperties, null);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with property exclusion.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("largeBlob", "internalData"));
         * String sql = MLC.selectFrom(Account.class, excludes)
         *                 .where(Filters.eq("status", "ACTIVE"))
         *                 .sql();
         * // Output: SELECT columns except largeBlob, internalData FROM account WHERE status = #{status}
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with table alias and property exclusion.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("password"));
         * String sql = MLC.selectFrom(Account.class, "a", excludes)
         *                 .where(Filters.like("a.emailAddress", "%@example.com"))
         *                 .sql();
         * // Output: SELECT a.columns except password FROM account a WHERE a.emailAddress LIKE #{emailAddress}
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param alias the table alias to use
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, alias, false, excludedPropNames);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with sub-entity inclusion control and property exclusion.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("temporaryData"));
         * String sql = MLC.selectFrom(Account.class, true, excludes)
         *                 .innerJoin("orders").on("account.id = orders.accountId")
         *                 .sql();
         * // Output: SELECT account columns except temporaryData, orders.* FROM account INNER JOIN orders ON account.id = orders.accountId
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, QueryUtil.getTableAlias(entityClass), includeSubEntityProperties, excludedPropNames);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder with full control over alias, sub-entity inclusion, and property exclusion.
         * This is the most comprehensive selectFrom method providing complete customization.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> excludes = new HashSet<>(Arrays.asList("debugInfo"));
         * String sql = MLC.selectFrom(Account.class, "acc", true, excludes)
         *                 .innerJoin("orders o").on("acc.id = o.accountId")
         *                 .innerJoin("items i").on("o.id = i.orderId")
         *                 .where(Filters.gt("o.total", 100))
         *                 .sql();
         * // Output: Complex SELECT with multiple joins
         * }</pre>
         *
         * @param entityClass the entity class to generate SELECT FROM for
         * @param alias the table alias to use
         * @param includeSubEntityProperties whether to include properties of sub-entities
         * @param excludedPropNames set of property names to exclude from the SELECT
         * @return a new SQLBuilder instance configured for SELECT operation with FROM clause
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties,
                final Set<String> excludedPropNames) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            if (hasSubEntityToInclude(entityClass, includeSubEntityProperties)) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, alias, excludedPropNames, NamingPolicy.CAMEL_CASE);
                //noinspection ConstantValue
                return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, selectTableNames);
            }

            return select(entityClass, includeSubEntityProperties, excludedPropNames).from(entityClass, alias);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity classes with table and result aliases.
         * This method is useful for complex queries involving multiple tables with custom result mapping.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.select(Account.class, "a", "account", Order.class, "o", "order")
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .sql();
         * // Output: SELECT a.id AS "account.id", a.name AS "account.name", o.id AS "order.id", o.total AS "order.total" FROM account a INNER JOIN orders o ON a.id = o.accountId
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation
         * @throws IllegalArgumentException if any entity class is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return select(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity classes with table aliases, result aliases, and property exclusion.
         * This provides full control over multi-entity queries with custom column selection.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> accountExcludes = new HashSet<>(Arrays.asList("password"));
         * Set<String> orderExcludes = new HashSet<>(Arrays.asList("internalNotes"));
         * String sql = MLC.select(Account.class, "a", "account", accountExcludes,
         *                        Order.class, "o", "order", orderExcludes)
         *                 .from("account a")
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .sql();
         * // Output: SELECT with excluded columns from both tables
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation
         * @throws IllegalArgumentException if any entity class is null
         */
        public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA,
                final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return select(multiSelects);
        }

        /**
         * Creates a SELECT SQL builder for multiple entity selections.
         * This is the most flexible method for complex multi-table queries with different configurations for each table.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = Arrays.asList(
         *     new Selection(Account.class, "a", "account", null, true, null),
         *     new Selection(Order.class, "o", "order", Arrays.asList("id", "total"), false, null),
         *     new Selection(Product.class, "p", "product", null, false, new HashSet<>(Arrays.asList("description")))
         * );
         * String sql = MLC.select(selections).from("account a")
         *                 .innerJoin("orders o").on("a.id = o.accountId")
         *                 .innerJoin("order_items oi").on("o.id = oi.orderId")
         *                 .innerJoin("products p").on("oi.productId = p.id")
         *                 .sql();
         * // Output: Complex SELECT with multiple tables and custom column selection
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder select(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final SQLBuilder instance = createInstance();

            instance._op = OperationType.QUERY;
            instance.setEntityClass(multiSelects.get(0).entityClass());
            instance._multiSelects = multiSelects;

            return instance;
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for multiple entity classes.
         * Automatically generates the FROM clause with proper table names.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.selectFrom(Account.class, "a", "account", Order.class, "o", "order")
         *                 .innerJoin("a.id = o.accountId")
         *                 .where(Filters.gt("o.createdDate", someDate))
         *                 .sql();
         * // Output: SELECT with automatic FROM clause generation
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation with FROM clause
         * @throws IllegalArgumentException if any entity class is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB,
                final String tableAliasB, final String classAliasB) {
            return selectFrom(entityClassA, tableAliasA, classAliasA, null, entityClassB, tableAliasB, classAliasB, null);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for multiple entity classes with property exclusion.
         * Automatically generates the FROM clause and excludes specified properties.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Set<String> accountExcludes = new HashSet<>(Arrays.asList("sensitiveData"));
         * String sql = MLC.selectFrom(Account.class, "a", "account", accountExcludes,
         *                            Order.class, "o", "order", null)
         *                 .innerJoin("a.id = o.accountId")
         *                 .sql();
         * // Output: SELECT with automatic FROM clause and excluded columns
         * }</pre>
         *
         * @param entityClassA the first entity class
         * @param tableAliasA the table alias for the first entity
         * @param classAliasA the result set alias prefix for the first entity
         * @param excludedPropNamesA properties to exclude from the first entity
         * @param entityClassB the second entity class
         * @param tableAliasB the table alias for the second entity
         * @param classAliasB the result set alias prefix for the second entity
         * @param excludedPropNamesB properties to exclude from the second entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation with FROM clause
         * @throws IllegalArgumentException if any entity class is null
         */
        public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA,
                final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB,
                final Set<String> excludedPropNamesB) {
            N.checkArgNotNull(entityClassA, SELECTION_PART_MSG);

            final List<Selection> multiSelects = N.asList(new Selection(entityClassA, tableAliasA, classAliasA, null, false, excludedPropNamesA),
                    new Selection(entityClassB, tableAliasB, classAliasB, null, false, excludedPropNamesB));

            return selectFrom(multiSelects);
        }

        /**
         * Creates a complete SELECT...FROM SQL builder for multiple entity selections.
         * Automatically generates the FROM clause based on the Selection configurations.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<Selection> selections = createComplexSelections();
         * String sql = MLC.selectFrom(selections)
         *                 .where(complexConditions)
         *                 .groupBy("account.type")
         *                 .having(Filters.gt("COUNT(*)", 5))
         *                 .sql();
         * // Output: Complex SELECT with automatic FROM clause generation
         * }</pre>
         *
         * @param multiSelects list of Selection objects defining what to select from each entity
         * @return a new SQLBuilder instance configured for multi-entity SELECT operation with FROM clause
         * @throws IllegalArgumentException if multiSelects is null or empty
         */
        public static SQLBuilder selectFrom(final List<Selection> multiSelects) {
            checkMultiSelects(multiSelects);

            final NamingPolicy namingPolicy = NamingPolicy.CAMEL_CASE;
            final String fromClause = getFromClause(multiSelects, namingPolicy);

            return select(multiSelects).from(fromClause);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified table.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.count("account")
         *                 .where(Filters.eq("isActive", true))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE isActive = #{isActive}
         * }</pre>
         *
         * @param tableName the name of the table to count rows from
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if tableName is null or empty
         */
        public static SQLBuilder count(final String tableName) {
            N.checkArgNotEmpty(tableName, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(tableName);
        }

        /**
         * Creates a COUNT(*) SQL builder for the specified entity class.
         * The table name is automatically derived from the entity class name or @Table annotation.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String sql = MLC.count(Account.class)
         *                 .where(Filters.between("createdDate", startDate, endDate))
         *                 .sql();
         * // Output: SELECT count(*) FROM account WHERE createdDate BETWEEN #{minCreatedDate} AND #{maxCreatedDate}
         * }</pre>
         *
         * @param entityClass the entity class to count rows for
         * @return a new SQLBuilder instance configured for COUNT operation
         * @throws IllegalArgumentException if entityClass is null
         */
        public static SQLBuilder count(final Class<?> entityClass) {
            N.checkArgNotNull(entityClass, SELECTION_PART_MSG);

            return select(COUNT_ALL_LIST).from(entityClass);
        }

        /**
         * Parses a condition into SQL format for the specified entity class.
         * This method is useful for generating SQL fragments from condition objects.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Condition cond = Filters.and(
         *     Filters.eq("status", "ACTIVE"),
         *     Filters.gt("balance", 1000)
         * );
         * String sql = MLC.parse(cond, Account.class).sql();
         * // Output: status = #{status} AND balance > #{balance}
         * }</pre>
         *
         * @param cond the condition to parse into SQL
         * @param entityClass the entity class for column mapping metadata
         * @return a new SQLBuilder instance containing the parsed condition
         * @throws IllegalArgumentException if cond is null
         */
        public static SQLBuilder parse(final Condition cond, final Class<?> entityClass) {
            N.checkArgNotNull(cond, "cond");

            final SQLBuilder instance = createInstance();

            instance.setEntityClass(entityClass);
            instance._op = OperationType.QUERY;
            instance._isForConditionOnly = true;
            instance.append(cond);

            return instance;
        }
    }
}
