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

package com.landawn.abacus.condition;

/**
 * Represents a MINUS clause in SQL queries (also known as EXCEPT in some databases).
 * This class is used to create conditions that return rows from the first query
 * that are not present in the second query (subquery).
 * 
 * <p>The MINUS operator removes duplicate rows and returns only distinct rows
 * that appear in the first query but not in the second. Both queries must have
 * the same number of columns with compatible data types.
 * 
 * <p>Note: Different databases use different keywords:
 * <ul>
 *   <li>Oracle, DB2: MINUS</li>
 *   <li>PostgreSQL, SQL Server: EXCEPT</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Create a subquery for all customers
 * SubQuery allCustomers = new SubQuery("SELECT customer_id FROM customers");
 * 
 * // Create a subquery for customers with orders
 * SubQuery customersWithOrders = new SubQuery("SELECT DISTINCT customer_id FROM orders");
 * 
 * // Find customers who have never placed an order
 * Minus minus = new Minus(customersWithOrders);
 * // When combined with the first query, generates:
 * // SELECT customer_id FROM customers
 * // MINUS
 * // SELECT DISTINCT customer_id FROM orders
 * }</pre>
 * 
 * @see Except
 * @see Union
 * @see Intersect
 */
public class Minus extends Clause {

    /**
     * Default constructor for serialization frameworks like Kryo.
     * This constructor should not be used directly in application code.
     */
    Minus() {
    }

    /**
     * Creates a new MINUS clause with the specified subquery.
     * The MINUS operation will return rows from the main query that are not
     * present in this subquery.
     *
     * @param condition the subquery whose results will be subtracted from the main query. Must not be null.
     * 
     * <p>Example:
     * <pre>{@code
     * // Find products that are in inventory but have never been sold
     * SubQuery soldProducts = new SubQuery("SELECT product_id FROM sales");
     * Minus minus = new Minus(soldProducts);
     * // When used with a query selecting from inventory:
     * // SELECT product_id FROM inventory
     * // MINUS
     * // SELECT product_id FROM sales
     * }</pre>
     */
    public Minus(final SubQuery condition) {
        super(Operator.MINUS, condition);
    }
}