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

import com.landawn.abacus.util.NamingPolicy;

/**
 * The base interface for all query conditions.
 * Conditions are immutable objects that represent various types of query criteria,
 * such as equality checks, comparisons, logical operations, and SQL clauses.
 * 
 * <p>Conditions should be immutable except when using {@code clearParameter()} to release resources.</p>
 * 
 * <p>Common implementations include:</p>
 * <ul>
 *   <li>Binary conditions: {@code Equal}, {@code GreaterThan}, {@code LessThan}, etc.</li>
 *   <li>Logical operations: {@code And}, {@code Or}, {@code Not}</li>
 *   <li>SQL clauses: {@code Where}, {@code Having}, {@code GroupBy}, {@code OrderBy}</li>
 *   <li>Special conditions: {@code In}, {@code Between}, {@code Like}, {@code IsNull}</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Create simple conditions
 * Condition ageCondition = CF.gt("age", 18);
 * Condition statusCondition = CF.eq("status", "active");
 * 
 * // Combine conditions
 * Condition combined = ageCondition.and(statusCondition);
 * 
 * // Use in queries
 * query.where(combined);
 * }</pre>
 * 
 * @see ConditionFactory
 * @see ConditionFactory.CF
 */
public interface Condition {
    /**
     * Gets the operator associated with this condition.
     * The operator determines the type of comparison or operation performed.
     * 
     * @return the operator for this condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition condition = CF.eq("name", "John");
     * Operator op = condition.getOperator(); // Returns Operator.EQUAL
     * }</pre>
     */
    Operator getOperator();

    /**
     * Creates a new AND condition combining this condition with another.
     * Both conditions must be true for the result to be true.
     * 
     * @param condition the condition to AND with this condition
     * @return a new And condition containing both conditions
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition age = CF.gt("age", 18);
     * Condition status = CF.eq("status", "active");
     * Condition combined = age.and(status);
     * // Equivalent to: age > 18 AND status = 'active'
     * }</pre>
     */
    And and(Condition condition);

    /**
     * Creates a new OR condition combining this condition with another.
     * Either condition can be true for the result to be true.
     * 
     * @param condition the condition to OR with this condition
     * @return a new Or condition containing both conditions
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition premium = CF.eq("memberType", "premium");
     * Condition vip = CF.eq("memberType", "vip");
     * Condition combined = premium.or(vip);
     * // Equivalent to: memberType = 'premium' OR memberType = 'vip'
     * }</pre>
     */
    Or or(Condition condition);

    /**
     * Creates a new NOT condition that negates this condition.
     * The result is true when this condition is false, and vice versa.
     * 
     * @return a new Not condition wrapping this condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition isNull = CF.isNull("email");
     * Condition isNotNull = isNull.not();
     * // Equivalent to: NOT (email IS NULL)
     * }</pre>
     */
    Not not();

    /**
     * Creates a deep copy of this condition.
     * The copy is independent of the original and can be modified without affecting it.
     * 
     * @param <T> the type of condition to return
     * @return a deep copy of this condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition original = CF.eq("status", "active");
     * Condition copy = original.copy();
     * // copy is a new instance with the same values
     * }</pre>
     */
    <T extends Condition> T copy();

    /**
     * Gets the list of parameter values associated with this condition.
     * Parameters are the actual values used in comparisons (e.g., the "John" in name = "John").
     * 
     * @return a list of parameter values, never null
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition condition = CF.between("age", 18, 65);
     * List<Object> params = condition.getParameters();
     * // Returns [18, 65]
     * }</pre>
     */
    List<Object> getParameters();

    /**
     * Clears all parameters associated with this condition.
     * This method is used to release resources, particularly for large parameter values.
     * After calling this method, the condition should not be used for queries.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition condition = CF.in("id", largeIdList);
     * // Use the condition in query...
     * condition.clearParameters(); // Release the large list
     * }</pre>
     */
    void clearParameters();

    /**
     * Returns a string representation of this condition using the specified naming policy.
     * The naming policy determines how property names are formatted in the output.
     * 
     * @param namingPolicy the policy for formatting property names
     * @return a string representation of this condition
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Condition condition = CF.eq("firstName", "John");
     * String sql = condition.toString(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // Returns: "first_name = 'John'"
     * }</pre>
     */
    String toString(NamingPolicy namingPolicy);
}