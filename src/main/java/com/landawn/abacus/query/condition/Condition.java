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

import java.util.List;

import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.NamingPolicy;

/**
 * The base interface for all query conditions.
 * Conditions are immutable objects that represent various types of query criteria,
 * such as equality checks, comparisons, logical operations, and SQL clauses.
 * 
 * <p>This interface defines the contract that all conditions must follow, providing
 * methods for logical operations (AND, OR, NOT), parameter management, and string
 * representation. Conditions are designed to be composable, allowing complex queries
 * to be built from simple building blocks.</p>
 * 
 * <p>Conditions should be immutable except when using {@code clearParameters()} to release resources.
 * This design ensures thread-safety and prevents unexpected side effects when conditions
 * are reused or shared.</p>
 * 
 * <p>Common implementations include:</p>
 * <ul>
 *   <li><b>Comparison conditions:</b> {@code Equal}, {@code NotEqual}, {@code GreaterThan}, 
 *       {@code LessThan}, {@code GreaterEqual}, {@code LessEqual}</li>
 *   <li><b>Range conditions:</b> {@code Between}, {@code NotBetween}</li>
 *   <li><b>Pattern matching:</b> {@code Like}, {@code NotLike}</li>
 *   <li><b>Null checks:</b> {@code IsNull}, {@code IsNotNull}</li>
 *   <li><b>Collection operations:</b> {@code In}, {@code NotIn}</li>
 *   <li><b>Logical operations:</b> {@code And}, {@code Or}, {@code Not}</li>
 *   <li><b>SQL clauses:</b> {@code Where}, {@code Having}, {@code GroupBy}, {@code OrderBy}, {@code Join}</li>
 *   <li><b>Subquery operations:</b> {@code Exists}, {@code NotExists}, {@code All}, {@code Any}</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create simple conditions
 * Condition ageCondition = Filters.gt("age", 18);
 * Condition statusCondition = Filters.eq("status", "active");
 * 
 * // Combine conditions using logical operations
 * Condition combined = ageCondition.and(statusCondition);
 * 
 * // Negate a condition
 * Condition notActive = statusCondition.not();
 * 
 * // Use in queries
 * query.where(combined);
 * 
 * // Get parameters for prepared statements
 * List<Object> params = combined.getParameters(); // [18, "active"]
 * }</pre>
 * 
 * @see Filters
 * @see AbstractCondition
 */
public interface Condition {
    /**
     * Gets the operator associated with this condition.
     * The operator determines the type of comparison or operation performed.
     * 
     * <p>Each condition has exactly one operator that defines its behavior.
     * For example, an Equal condition has the EQUAL operator, while an
     * And condition has the AND operator.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition eq = Filters.eq("name", "John");
     * Operator op1 = eq.getOperator(); // Returns Operator.EQUAL
     * 
     * Condition gt = Filters.gt("age", 18);
     * Operator op2 = gt.getOperator(); // Returns Operator.GREATER_THAN
     * 
     * Condition and = eq.and(gt);
     * Operator op3 = and.getOperator(); // Returns Operator.AND
     * }</pre>
     * 
     * @return the operator for this condition
     */
    Operator getOperator();

    /**
     * Creates a new AND condition combining this condition with another.
     * Both conditions must be true for the result to be true.
     * 
     * <p>The AND operation follows standard logical conjunction rules:</p>
     * <ul>
     *   <li>true AND true = true</li>
     *   <li>true AND false = false</li>
     *   <li>false AND true = false</li>
     *   <li>false AND false = false</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition age = Filters.gt("age", 18);
     * Condition status = Filters.eq("status", "active");
     * And combined = age.and(status);
     * // Equivalent to: age > 18 AND status = 'active'
     * 
     * // Can be chained
     * Condition verified = Filters.eq("verified", true);
     * And all = age.and(status).and(verified);
     * // Equivalent to: age > 18 AND status = 'active' AND verified = true
     * }</pre>
     * 
     * @param condition the condition to AND with this condition
     * @return a new And condition containing both conditions
     */
    And and(Condition condition);

    /**
     * Creates a new OR condition combining this condition with another.
     * Either condition can be true for the result to be true.
     * 
     * <p>The OR operation follows standard logical disjunction rules:</p>
     * <ul>
     *   <li>true OR true = true</li>
     *   <li>true OR false = true</li>
     *   <li>false OR true = true</li>
     *   <li>false OR false = false</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition premium = Filters.eq("memberType", "premium");
     * Condition vip = Filters.eq("memberType", "vip");
     * Or combined = premium.or(vip);
     * // Equivalent to: memberType = 'premium' OR memberType = 'vip'
     * 
     * // Can be chained
     * Condition gold = Filters.eq("memberType", "gold");
     * Or any = premium.or(vip).or(gold);
     * // Equivalent to: memberType = 'premium' OR memberType = 'vip' OR memberType = 'gold'
     * }</pre>
     * 
     * @param condition the condition to OR with this condition
     * @return a new Or condition containing both conditions
     */
    Or or(Condition condition);

    /**
     * Creates a new NOT condition that negates this condition.
     * The result is true when this condition is false, and vice versa.
     * 
     * <p>The NOT operation follows standard logical negation rules:</p>
     * <ul>
     *   <li>NOT true = false</li>
     *   <li>NOT false = true</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition isNull = Filters.isNull("email");
     * Not isNotNull = isNull.not();
     * // Equivalent to: NOT (email IS NULL)
     * 
     * // Complex negation
     * Condition complex = Filters.and(
     *     Filters.eq("status", "active"),
     *     Filters.gt("age", 18)
     * );
     * Not negated = complex.not();
     * // Equivalent to: NOT (status = 'active' AND age > 18)
     * }</pre>
     * 
     * @return a new Not condition wrapping this condition
     */
    Not not();

    /**
     * Creates a deep copy of this condition.
     * The copy is independent of the original and can be modified without affecting it.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition original = Filters.eq("status", "active");
     * Condition copy = original.copy();
     * // copy is a new instance with the same values
     * }</pre>
     * 
     * @param <T> the type of condition to return
     * @return a deep copy of this condition
     */
    <T extends Condition> T copy();

    /**
     * Gets the list of parameter values associated with this condition.
     * Parameters are the actual values used in comparisons (e.g., the "John" in name = "John").
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition condition = Filters.between("age", 18, 65);
     * List<Object> params = condition.getParameters();
     * // Returns [18, 65]
     * }</pre>
     * 
     * @return a list of parameter values, never null
     */
    List<Object> getParameters();

    /**
     * Clears all parameter values by setting them to null to free memory.
     *
     * <p>The parameter list size remains unchanged, but all elements become null.
     * Use this method to release large objects when the condition is no longer needed.
     * This is the only mutating operation allowed on otherwise immutable conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition condition = Filters.between("age", 18, 65);
     * List<Object> parameters = condition.getParameters(); // Returns [18, 65]
     * condition.clearParameters(); // All parameters become null
     * List<Object> updatedParameters = condition.getParameters(); // Returns [null, null]
     * }</pre>
     */
    void clearParameters();

    /**
     * Returns a string representation of this condition using the specified naming policy.
     * The naming policy determines how property names are formatted in the output.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition condition = Filters.eq("firstName", "John");
     * String sql = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // Returns: "first_name = 'John'"
     * }</pre>
     * 
     * @param namingPolicy the policy for formatting property names
     * @return a string representation of this condition
     */
    String toString(NamingPolicy namingPolicy);
}