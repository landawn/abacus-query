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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.NamingPolicy;

/**
 * The base interface for all query conditions.
 * Conditions are immutable objects that represent various types of query criteria,
 * such as equality checks, comparisons, composable operations, and SQL clauses.
 * 
 * <p>This interface defines the contract that all conditions must follow, providing
 * methods for composable operations (AND, OR, NOT), parameter management, and string
 * representation. Conditions are designed to be composable, allowing complex queries
 * to be built from simple building blocks.</p>
 * 
 * <p>Conditions are immutable after construction. The only exception is {@code clearParameters()},
 * which may null out parameter values to release memory. This design helps prevent unexpected side effects when conditions are reused or shared.</p>
 * 
 * <p>Common implementations include:</p>
 * <ul>
 *   <li><b>Comparison conditions:</b> {@code Equal}, {@code NotEqual}, {@code GreaterThan}, 
 *       {@code LessThan}, {@code GreaterThanOrEqual}, {@code LessThanOrEqual}</li>
 *   <li><b>Range conditions:</b> {@code Between}, {@code NotBetween}</li>
 *   <li><b>Pattern matching:</b> {@code Like}, {@code NotLike}</li>
 *   <li><b>Null checks:</b> {@code IsNull}, {@code IsNotNull}</li>
 *   <li><b>Collection operations:</b> {@code In}, {@code NotIn}</li>
 *   <li><b>Composable operations:</b> {@code And}, {@code Or}, {@code Not}</li>
 *   <li><b>SQL clauses:</b> {@code Where}, {@code Having}, {@code GroupBy}, {@code OrderBy}, {@code Join}</li>
 *   <li><b>Subquery operations:</b> {@code Exists}, {@code NotExists}, {@code All}, {@code Any}</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create simple conditions
 * Condition ageCondition = Filters.greaterThan("age", 18);
 * Condition statusCondition = Filters.equal("status", "active");
 * 
 * // Combine conditions using composable operations
 * Condition combined = ageCondition.and(statusCondition);
 * 
 * // Negate a condition
 * Condition notActive = statusCondition.not();
 * 
 * // Use in queries
 * SqlBuilder builder = PSC.select("*")
 *     .from("users")
 *     .where(combined);
 * 
 * // Get parameters for prepared statements
 * ImmutableList<Object> params = combined.getParameters();   // [18, "active"]
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
     * Condition eq = Filters.equal("status", "active");
     * Operator op = eq.operator();   // Operator.EQUAL
     *
     * Condition between = Filters.between("age", 18, 65);
     * Operator betweenOp = between.operator();   // Operator.BETWEEN
     *
     * Condition combined = eq.and(between);
     * Operator andOp = combined.operator();   // Operator.AND
     * }</pre>
     *
     * @return the operator for this condition
     */
    Operator operator();

    /**
     * Gets the list of parameter values associated with this condition.
     * Parameters are the actual values used in comparisons (e.g., the "John" in name = "John").
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition eq = Filters.equal("name", "John");
     * ImmutableList<Object> params = eq.getParameters();   // ["John"]
     *
     * Condition between = Filters.between("age", 18, 65);
     * ImmutableList<Object> rangeParams = between.getParameters();   // [18, 65]
     *
     * Condition combined = Filters.and(eq, between);
     * ImmutableList<Object> allParams = combined.getParameters();   // ["John", 18, 65]
     * }</pre>
     *
     * @return an immutable list of parameter values, never null
     */
    ImmutableList<Object> getParameters();

    /**
     * Clears all parameter values by setting them to null to free memory.
     *
     * <p>The parameter list size remains unchanged, but all elements become null.
     * Use this method to release large objects when the condition is no longer needed.
     * This is the only mutating operation allowed on otherwise immutable conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition eq = Filters.equal("name", "John");
     * ImmutableList<Object> params = eq.getParameters();   // ["John"]
     *
     * // Release parameter memory when the condition is no longer needed
     * eq.clearParameters();
     * ImmutableList<Object> cleared = eq.getParameters();   // [null]
     *
     * // For compound conditions, clears parameters recursively
     * Condition combined = Filters.and(Filters.greaterThan("age", 18), Filters.equal("status", "active"));
     * combined.clearParameters();   // Clears parameters in both child conditions
     * }</pre>
     *
     */
    @Beta
    void clearParameters();

    /**
     * Returns a string representation of this condition using the specified naming policy.
     * The naming policy determines how property names are formatted in the output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Condition eq = Filters.equal("firstName", "John");
     *
     * // No change to property names
     * String noChange = eq.toString(NamingPolicy.NO_CHANGE);       // "firstName = 'John'"
     *
     * // Convert to lower case with underscores (snake_case)
     * String lower = eq.toString(NamingPolicy.SNAKE_CASE);   // "first_name = 'John'"
     *
     * // Convert to upper case with underscores (SCREAMING_SNAKE_CASE)
     * String upper = eq.toString(NamingPolicy.SCREAMING_SNAKE_CASE);   // "FIRST_NAME = 'John'"
     * }</pre>
     *
     * @param namingPolicy the policy for formatting property names
     * @return a string representation of this condition
     */
    String toString(NamingPolicy namingPolicy);
}
