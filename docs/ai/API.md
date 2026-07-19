# abacus-query API Index (v4.8.10)
- Build: 8e5f5ca081344fd605eb701a3424a4baed2ae494
- Java: 17
- Generated: 2026-07-18

## Packages
- com.landawn.abacus.query
- com.landawn.abacus.query.condition

## com.landawn.abacus.query
### Class AbstractQueryBuilder (com.landawn.abacus.query.AbstractQueryBuilder)
Base class for fluent SQL builders.

**Thread-safety:** not-thread-safe
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### sqlDialect(...) -> SqlDialect
- **Signature:** `public SqlDialect sqlDialect()`
- **Summary:** Returns the SqlDialect this builder renders SQL with.
- **Parameters:**
  - (none)
- **Returns:** the complete rendering and tokenizer configuration bound to this builder
##### into(...) -> This
- **Signature:** `public This into(final String tableName)`
- **Summary:** Specifies the target table for an INSERT or INSERT ... SELECT operation.
- **Contract:**
  - Must be called after setting the columns/values via insert(...) or the columns to copy via select(...).
  - When chained after select(...), the eventual from(...) call appends the source query, producing INSERT INTO target (cols) SELECT cols FROM source.
- **Parameters:**
  - `tableName` (`String`) — the name of the target table (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String insertSql = PSC.insert("firstName", "lastName").into("account").build().query();
    // Output: INSERT INTO account (first_name, last_name) VALUES (?, ?)
    
    String insertSelectSql = PSC.select("firstName").into("account_backup").from("account").build().query();
    // Output: INSERT INTO account_backup (first_name) SELECT first_name AS "firstName" FROM account
    ```
- **Signature:** `public This into(final Class<?> entityClass)`
- **Summary:** Specifies the target table for an INSERT or INSERT ... SELECT operation using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the target table (must not be null)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.insert(account).into(Account.class).build().query();
    // Table name derived from Account class based on naming policy
    ```
- **Signature:** `public This into(final String tableName, final Class<?> entityClass)`
- **Summary:** Specifies the target table for an INSERT or INSERT ... SELECT operation with an explicit table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the name of the target table (must not be null, empty, or blank)
  - `entityClass` (`Class<?>`) — the entity class for property mapping (may be null)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.insert(account).into("account_archive", Account.class).build().query();
    // Inserts into specified table with Account class mapping
    ```
##### distinct(...) -> This
- **Signature:** `public This distinct()`
- **Summary:** Adds DISTINCT clause to the SELECT statement.
- **Parameters:**
  - (none)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("name").distinct().from("account").build().query();
    // Output: SELECT DISTINCT name FROM account
    ```
##### selectModifier(...) -> This
- **Signature:** `public This selectModifier(final String selectModifier)`
- **Summary:** Adds a pre-select modifier to the SELECT statement.
- **Contract:**
  - For better performance, this method should be called before from.
- **Parameters:**
  - `selectModifier` (`String`) — modifiers like ALL, DISTINCT, DISTINCTROW, TOP, etc.; may be null or empty (no-op)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").selectModifier("TOP 10").from("account").build().query();
    // Output: SELECT TOP 10 * FROM account
    ```
##### from(...) -> This
- **Signature:** `public This from(final String... tableNames)`
- **Summary:** Sets the FROM clause with multiple table names.
- **Parameters:**
  - `tableNames` (`String[]`) — the table names to use in the FROM clause (must not be null or empty, and no element may be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from("users", "orders", "items").build().query();
    // Output: SELECT * FROM users, orders, items
    ```
- **Signature:** `public This from(final Collection<String> tableNames)`
- **Summary:** Sets the FROM clause with a collection of table names.
- **Parameters:**
  - `tableNames` (`Collection<String>`) — the collection of table names to use in the FROM clause (must not be null or empty, and no element may be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> tables = Arrays.asList("users", "orders");
    String sql = PSC.select("*").from(tables).build().query();
    // Output: SELECT * FROM users, orders
    ```
- **Signature:** `public This from(final String expr)`
- **Summary:** Sets the FROM clause with a single expression.
- **Parameters:**
  - `expr` (`String`) — the FROM clause expression
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from("users u").build().query();
    // Output: SELECT * FROM users u
    
    String sql2 = PSC.select("*").from("(SELECT * FROM users) t").build().query();
    // Output: SELECT * FROM (SELECT * FROM users) t
    ```
- **Signature:** `public This from(final String expr, final Class<?> entityClass)`
- **Summary:** Sets the FROM clause with an expression and associates it with an entity class.
- **Parameters:**
  - `expr` (`String`) — the FROM clause expression
  - `entityClass` (`Class<?>`) — the entity class for property mapping (may be null, in which case no entity-class association is performed)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from("users u", User.class).build().query();
    // Associates the User class for property mapping
    ```
- **Signature:** `public This from(final Class<?> entityClass)`
- **Summary:** Sets the FROM clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class).build().query();
    // Table name derived from User class based on naming policy
    ```
- **Signature:** `public This from(final Class<?> entityClass, final String alias)`
- **Summary:** Sets the FROM clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table (must not be null)
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class, "u").build().query();
    // Output: SELECT * FROM users u (table name based on naming policy)
    ```
##### join(...) -> This
- **Signature:** `public This join(final String joinExpr)`
- **Summary:** Adds a JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the ON clause if present, e.g. "orders o ON u.id = o.user_id" (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users u")
                    .join("orders o ON u.id = o.user_id")
                    .build().query();
    // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
    ```
- **Signature:** `public This join(final Class<?> entityClass)`
- **Summary:** Adds a JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from(User.class)
                    .join(Order.class)
                    .on("users.id = orders.user_id")
                    .build().query();
    ```
- **Signature:** `public This join(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join (must not be null)
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from(User.class, "u")
                    .join(Order.class, "o")
                    .on("u.id = o.user_id")
                    .build().query();
    ```
##### innerJoin(...) -> This
- **Signature:** `public This innerJoin(final String joinExpr)`
- **Summary:** Adds an INNER JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the ON clause if present (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users u")
                    .innerJoin("orders o ON u.id = o.user_id")
                    .build().query();
    // Output: SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id
    ```
- **Signature:** `public This innerJoin(final Class<?> entityClass)`
- **Summary:** Adds an INNER JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class).innerJoin(Order.class).on("users.id = orders.user_id").build().query();
    // Output: SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id
    ```
- **Signature:** `public This innerJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds an INNER JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class, "u").innerJoin(Order.class, "o").on("u.id = o.user_id").build().query();
    // Output: SELECT * FROM users u INNER JOIN orders o ON u.id = o.user_id
    ```
##### leftJoin(...) -> This
- **Signature:** `public This leftJoin(final String joinExpr)`
- **Summary:** Adds a LEFT JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the ON clause if present (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users u")
                    .leftJoin("orders o ON u.id = o.user_id")
                    .build().query();
    // Output: SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id
    ```
- **Signature:** `public This leftJoin(final Class<?> entityClass)`
- **Summary:** Adds a LEFT JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class).leftJoin(Order.class).on("users.id = orders.user_id").build().query();
    // Output: SELECT * FROM users LEFT JOIN orders ON users.id = orders.user_id
    ```
- **Signature:** `public This leftJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a LEFT JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class, "u").leftJoin(Order.class, "o").on("u.id = o.user_id").build().query();
    // Output: SELECT * FROM users u LEFT JOIN orders o ON u.id = o.user_id
    ```
##### rightJoin(...) -> This
- **Signature:** `public This rightJoin(final String joinExpr)`
- **Summary:** Adds a RIGHT JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the ON clause if present (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users u")
                    .rightJoin("orders o ON u.id = o.user_id")
                    .build().query();
    // Output: SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id
    ```
- **Signature:** `public This rightJoin(final Class<?> entityClass)`
- **Summary:** Adds a RIGHT JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class).rightJoin(Order.class).on("users.id = orders.user_id").build().query();
    // Output: SELECT * FROM users RIGHT JOIN orders ON users.id = orders.user_id
    ```
- **Signature:** `public This rightJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a RIGHT JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class, "u").rightJoin(Order.class, "o").on("u.id = o.user_id").build().query();
    // Output: SELECT * FROM users u RIGHT JOIN orders o ON u.id = o.user_id
    ```
##### fullJoin(...) -> This
- **Signature:** `public This fullJoin(final String joinExpr)`
- **Summary:** Adds a FULL JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the ON clause if present (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users u")
                    .fullJoin("orders o ON u.id = o.user_id")
                    .build().query();
    // Output: SELECT * FROM users u FULL JOIN orders o ON u.id = o.user_id
    ```
- **Signature:** `public This fullJoin(final Class<?> entityClass)`
- **Summary:** Adds a FULL JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class).fullJoin(Order.class).on("users.id = orders.user_id").build().query();
    // Output: SELECT * FROM users FULL JOIN orders ON users.id = orders.user_id
    ```
- **Signature:** `public This fullJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a FULL JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class, "u").fullJoin(Order.class, "o").on("u.id = o.user_id").build().query();
    // Output: SELECT * FROM users u FULL JOIN orders o ON u.id = o.user_id
    ```
##### crossJoin(...) -> This
- **Signature:** `public This crossJoin(final String joinExpr)`
- **Summary:** Adds a CROSS JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the join expression (a table reference, optionally with alias; a CROSS JOIN takes no ON clause) (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .crossJoin("orders")
                    .build().query();
    // Output: SELECT * FROM users CROSS JOIN orders
    ```
- **Signature:** `public This crossJoin(final Class<?> entityClass)`
- **Summary:** Adds a CROSS JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class).crossJoin(Order.class).build().query();
    // Output: SELECT * FROM users CROSS JOIN orders
    ```
- **Signature:** `public This crossJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a CROSS JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class, "u").crossJoin(Order.class, "o").build().query();
    // Output: SELECT * FROM users u CROSS JOIN orders o
    ```
##### naturalJoin(...) -> This
- **Signature:** `public This naturalJoin(final String joinExpr)`
- **Summary:** Adds a NATURAL JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the join expression (a table reference, optionally with alias; a NATURAL JOIN takes no ON clause) (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .naturalJoin("orders")
                    .build().query();
    // Output: SELECT * FROM users NATURAL JOIN orders
    ```
- **Signature:** `public This naturalJoin(final Class<?> entityClass)`
- **Summary:** Adds a NATURAL JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class).naturalJoin(Order.class).build().query();
    // Output: SELECT * FROM users NATURAL JOIN orders
    ```
- **Signature:** `public This naturalJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a NATURAL JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*").from(User.class, "u").naturalJoin(Order.class, "o").build().query();
    // Output: SELECT * FROM users u NATURAL JOIN orders o
    ```
##### on(...) -> This
- **Signature:** `public This on(final String expr)`
- **Summary:** Adds an ON clause for join conditions.
- **Parameters:**
  - `expr` (`String`) — the join condition expression (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users u")
                    .join("orders o")
                    .on("u.id = o.user_id")
                    .build().query();
    // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
    ```
- **Signature:** `public This on(final String... exprs)`
- **Summary:** Adds an ON clause for a composite join condition, joining the given expressions with AND.
- **Parameters:**
  - `exprs` (`String[]`) — the join condition expressions (must not be null or empty, and no element may be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users u")
                    .join("orders o")
                    .on("u.id = o.user_id", "u.tenant_id = o.tenant_id")
                    .build().query();
    // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id AND u.tenant_id = o.tenant_id
    ```
- **Signature:** `public This on(final Condition condition)`
- **Summary:** Adds an ON clause with a condition object for join conditions.
- **Parameters:**
  - `condition` (`Condition`) — the join condition (must not be null)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users u")
                    .join("orders o")
                    .on(Filters.expr("u.id = o.user_id"))
                    .build().query();
    // Output: SELECT * FROM users u JOIN orders o ON u.id = o.user_id
    ```
##### using(...) -> This
- **Signature:** `public This using(final String expr)`
- **Summary:** Adds a USING clause for join conditions.
- **Parameters:**
  - `expr` (`String`) — the column name(s) for the USING clause
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .join("orders")
                    .using("user_id")
                    .build().query();
    // Output: SELECT * FROM users JOIN orders USING (user_id)
    ```
- **Signature:** `public This using(final String... propOrColumnNames)`
- **Summary:** Adds a USING clause with multiple columns for join conditions.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names for the USING clause (must not be null or empty, and no element may be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("orders")
                    .join("order_items")
                    .using("order_id", "tenant_id")
                    .build().query();
    // Output: SELECT * FROM orders JOIN order_items USING (order_id, tenant_id)
    ```
- **Signature:** `public This using(final Collection<String> propOrColumnNames)`
- **Summary:** Adds a USING clause with a collection of columns for join conditions.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of property or column names for the USING clause (must not be null or empty, and no element may be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("order_id", "tenant_id");
    String sql = PSC.select("*")
                    .from("orders")
                    .join("order_items")
                    .using(columns)
                    .build().query();
    // Output: SELECT * FROM orders JOIN order_items USING (order_id, tenant_id)
    ```
##### where(...) -> This
- **Signature:** `public This where(final String expr)`
- **Summary:** Adds a WHERE clause with a string expression.
- **Parameters:**
  - `expr` (`String`) — the WHERE condition expression (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .where("age > 18")
                    .build().query();
    // Output: SELECT * FROM users WHERE age > 18
    ```
- **Signature:** `public This where(final Condition condition)`
- **Summary:** Adds a WHERE clause with a condition object.
- **Parameters:**
  - `condition` (`Condition`) — the WHERE condition (must not be null)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.greaterThan("age", 18))
                    .build().query();
    // Output: SELECT * FROM users WHERE age > ?
    ```
- **See also:** Filters
##### groupByAsc(...) -> This
- **Signature:** `@Beta public This groupByAsc(final String propOrColumnName)`
- **Summary:** Adds a GROUP BY ASC clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("category", "COUNT(*)")
                    .from("products")
                    .groupByAsc("category")
                    .build().query();
    // Output: SELECT category, COUNT(*) FROM products GROUP BY category ASC
    ```
- **Signature:** `@Beta public This groupByAsc(final String... propOrColumnNames)`
- **Summary:** Adds a GROUP BY ASC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to group by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupByAsc("category", "brand")
                    .build().query();
    // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC
    ```
- **Signature:** `@Beta public This groupByAsc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds a GROUP BY ASC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("category", "brand");
    String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupByAsc(columns)
                    .build().query();
    // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand ASC
    ```
##### groupByDesc(...) -> This
- **Signature:** `@Beta public This groupByDesc(final String propOrColumnName)`
- **Summary:** Adds a GROUP BY DESC clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("category", "COUNT(*)")
                    .from("products")
                    .groupByDesc("category")
                    .build().query();
    // Output: SELECT category, COUNT(*) FROM products GROUP BY category DESC
    ```
- **Signature:** `@Beta public This groupByDesc(final String... propOrColumnNames)`
- **Summary:** Adds a GROUP BY DESC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to group by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupByDesc("category", "brand")
                    .build().query();
    // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC
    ```
- **Signature:** `@Beta public This groupByDesc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds a GROUP BY DESC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("category", "brand");
    String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupByDesc(columns)
                    .build().query();
    // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC
    ```
##### groupBy(...) -> This
- **Signature:** `public This groupBy(final String propOrColumnName)`
- **Summary:** Adds a GROUP BY clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("category", "COUNT(*)")
                    .from("products")
                    .groupBy("category")
                    .build().query();
    // Output: SELECT category, COUNT(*) FROM products GROUP BY category
    ```
- **Signature:** `public This groupBy(final String... propOrColumnNames)`
- **Summary:** Adds a GROUP BY clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to group by (must not be null or empty, and no element may be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupBy("category", "brand")
                    .build().query();
    // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand
    ```
- **Signature:** `public This groupBy(final String expr, final SortDirection direction)`
- **Summary:** Adds a GROUP BY clause with a single column and sort direction.
- **Parameters:**
  - `expr` (`String`) — the column or expression to group by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("category", "COUNT(*)")
                    .from("products")
                    .groupBy("category", SortDirection.DESC)
                    .build().query();
    // Output: SELECT category, COUNT(*) FROM products GROUP BY category DESC
    ```
- **Signature:** `public This groupBy(final Collection<String> propOrColumnNames)`
- **Summary:** Adds a GROUP BY clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("category", "brand");
    String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupBy(columns)
                    .build().query();
    // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category, brand
    ```
- **Signature:** `public This groupBy(final Collection<String> propOrColumnNames, final SortDirection direction)`
- **Summary:** Adds a GROUP BY clause with a collection of columns and sort direction.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by
  - `direction` (`SortDirection`) — the direction appended after each column in the GROUP BY clause
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("category", "brand");
    String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupBy(columns, SortDirection.DESC)
                    .build().query();
    // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category DESC, brand DESC
    ```
- **Signature:** `public This groupBy(final Map<String, SortDirection> groupings)`
- **Summary:** Adds a GROUP BY clause with columns and individual sort directions.
- **Parameters:**
  - `groupings` (`Map<String, SortDirection>`) — map of columns to their sort directions
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    Map<String, SortDirection> orders = new LinkedHashMap<>();
    orders.put("category", SortDirection.ASC);
    orders.put("brand", SortDirection.DESC);
    String sql = PSC.select("category", "brand", "COUNT(*)")
                    .from("products")
                    .groupBy(orders)
                    .build().query();
    // Output: SELECT category, brand, COUNT(*) FROM products GROUP BY category ASC, brand DESC
    ```
##### having(...) -> This
- **Signature:** `public This having(final String expr)`
- **Summary:** Adds a HAVING clause with a string expression.
- **Parameters:**
  - `expr` (`String`) — the HAVING condition expression (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("category", "COUNT(*) as count")
                    .from("products")
                    .groupBy("category")
                    .having("COUNT(*) > 10")
                    .build().query();
    // Output: SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > 10
    ```
- **Signature:** `public This having(final Condition condition)`
- **Summary:** Adds a HAVING clause with a condition object.
- **Parameters:**
  - `condition` (`Condition`) — the HAVING condition (must not be null)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("category", "COUNT(*) as count")
                    .from("products")
                    .groupBy("category")
                    .having(Filters.greaterThan("COUNT(*)", 10))
                    .build().query();
    // Output: SELECT category, COUNT(*) AS count FROM products GROUP BY category HAVING COUNT(*) > ?
    ```
- **See also:** Filters
##### orderByAsc(...) -> This
- **Signature:** `@Beta public This orderByAsc(final String propOrColumnName)`
- **Summary:** Adds an ORDER BY ASC clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderByAsc("name")
                    .build().query();
    // Output: SELECT * FROM users ORDER BY name ASC
    ```
- **Signature:** `@Beta public This orderByAsc(final String... propOrColumnNames)`
- **Summary:** Adds an ORDER BY ASC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to order by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderByAsc("lastName", "firstName")
                    .build().query();
    // Output: SELECT * FROM users ORDER BY last_name ASC, first_name ASC
    ```
- **Signature:** `@Beta public This orderByAsc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds an ORDER BY ASC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("lastName", "firstName");
    String sql = PSC.select("*")
                    .from("users")
                    .orderByAsc(columns)
                    .build().query();
    // Output: SELECT * FROM users ORDER BY last_name ASC, first_name ASC
    ```
##### orderByDesc(...) -> This
- **Signature:** `@Beta public This orderByDesc(final String propOrColumnName)`
- **Summary:** Adds an ORDER BY DESC clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderByDesc("createdDate")
                    .build().query();
    // Output: SELECT * FROM users ORDER BY created_date DESC
    ```
- **Signature:** `@Beta public This orderByDesc(final String... propOrColumnNames)`
- **Summary:** Adds an ORDER BY DESC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to order by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderByDesc("createdDate", "id")
                    .build().query();
    // Output: SELECT * FROM users ORDER BY created_date DESC, id DESC
    ```
- **Signature:** `@Beta public This orderByDesc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds an ORDER BY DESC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("createdDate", "id");
    String sql = PSC.select("*")
                    .from("users")
                    .orderByDesc(columns)
                    .build().query();
    // Output: SELECT * FROM users ORDER BY created_date DESC, id DESC
    ```
##### orderBy(...) -> This
- **Signature:** `public This orderBy(final String propOrColumnName)`
- **Summary:** Adds an ORDER BY clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by (must not be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderBy("name")
                    .build().query();
    // Output: SELECT * FROM users ORDER BY name
    ```
- **Signature:** `public This orderBy(final String... propOrColumnNames)`
- **Summary:** Adds an ORDER BY clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to order by (must not be null or empty, and no element may be null, empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderBy("lastName", "firstName")
                    .build().query();
    // Output: SELECT * FROM users ORDER BY last_name, first_name
    ```
- **Signature:** `public This orderBy(final String expr, final SortDirection direction)`
- **Summary:** Adds an ORDER BY clause with a single column and sort direction.
- **Parameters:**
  - `expr` (`String`) — the column or expression to order by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderBy("name", SortDirection.DESC)
                    .build().query();
    // Output: SELECT * FROM users ORDER BY name DESC
    ```
- **Signature:** `public This orderBy(final Collection<String> propOrColumnNames)`
- **Summary:** Adds an ORDER BY clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("lastName", "firstName");
    String sql = PSC.select("*")
                    .from("users")
                    .orderBy(columns)
                    .build().query();
    // Output: SELECT * FROM users ORDER BY last_name, first_name
    ```
- **Signature:** `public This orderBy(final Collection<String> propOrColumnNames, final SortDirection direction)`
- **Summary:** Adds an ORDER BY clause with a collection of columns and sort direction.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by
  - `direction` (`SortDirection`) — the direction appended after each column in the ORDER BY clause
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("lastName", "firstName");
    String sql = PSC.select("*")
                    .from("users")
                    .orderBy(columns, SortDirection.DESC)
                    .build().query();
    // Output: SELECT * FROM users ORDER BY last_name DESC, first_name DESC
    ```
- **Signature:** `public This orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Adds an ORDER BY clause with columns and individual sort directions.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of columns to their sort directions
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    Map<String, SortDirection> orders = new LinkedHashMap<>();
    orders.put("lastName", SortDirection.ASC);
    orders.put("firstName", SortDirection.DESC);
    String sql = PSC.select("*")
                    .from("users")
                    .orderBy(orders)
                    .build().query();
    // Output: SELECT * FROM users ORDER BY last_name ASC, first_name DESC
    ```
##### limit(...) -> This
- **Signature:** `public This limit(final int count)`
- **Summary:** Adds a row-count restriction to the query, rendered in the dialect's pagination syntax.
- **Contract:**
  - FETCH together with an ORDER BY clause); the OFFSET 0 ROWS prefix is omitted when #offset(int) has already been called any other product, or no product info: LIMIT count On the FETCH-style dialects (Oracle, DB2, SQL Server) this method also consumes the OFFSET and FETCH slots, because OFFSET must precede FETCH: call #offset(int) before this method, or prefer #limit(int, int), which emits the combined clause in the correct order.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .limit(10)
                    .build().query();
    // Output: SELECT * FROM users LIMIT 10
    
    Dsl oracleDsl = Dsl.forDialect(SqlDialect.builder()
            .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
            .productInfo(SqlDialect.ProductInfo.of("Oracle"))
            .build());
    String oracleSql = oracleDsl.select("*").from("users").limit(10).build().query();
    // Output: SELECT * FROM users FETCH FIRST 10 ROWS ONLY
    ```
- **Signature:** `public This limit(final int count, final int offset)`
- **Summary:** Adds a count-plus-offset pagination clause, rendered in the dialect's pagination syntax.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .limit(10, 20)  // limit 10, offset 20
                    .build().query();
    // Output: SELECT * FROM users LIMIT 10 OFFSET 20
    
    Dsl oracleDsl = Dsl.forDialect(SqlDialect.builder()
            .sqlPolicy(SqlDialect.SqlPolicy.PARAMETERIZED_SQL)
            .productInfo(SqlDialect.ProductInfo.of("Oracle"))
            .build());
    String oracleSql = oracleDsl.select("*").from("users").limit(10, 20).build().query();
    // Output: SELECT * FROM users OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
    ```
##### offset(...) -> This
- **Signature:** `public This offset(final int offset)`
- **Summary:** Adds an OFFSET clause to skip a number of rows, rendered in the dialect's pagination syntax.
- **Contract:**
  - On Oracle, DB2 and SQL Server dialects (per SqlDialect.ProductInfo) the clause is rendered as OFFSET offset ROWS; on those dialects call this method before #limit(int), because OFFSET must precede FETCH.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .limit(10)
                    .offset(20)
                    .build().query();
    // Output: SELECT * FROM users LIMIT 10 OFFSET 20
    ```
##### offsetRows(...) -> This
- **Signature:** `public This offsetRows(final int offset)`
- **Summary:** Adds an OFFSET ROWS clause (SQL:2008 standard syntax).
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderBy("id")
                    .offsetRows(20)
                    .fetchNextRows(10)
                    .build().query();
    // Output: SELECT * FROM users ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
    ```
- **See also:** #offset(int), #limit(int, int)
##### fetchNextRows(...) -> This
- **Signature:** `public This fetchNextRows(final int count)`
- **Summary:** Adds a FETCH NEXT N ROWS ONLY clause (SQL:2008 standard syntax).
- **Contract:**
  - Call #offsetRows(int) first when an offset is needed; #offset(int) is also compatible when this builder uses a FETCH-style dialect.
- **Parameters:**
  - `count` (`int`) — the number of rows to fetch
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderBy("id")
                    .offsetRows(0)
                    .fetchNextRows(10)
                    .build().query();
    // Output: SELECT * FROM users ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
    ```
- **See also:** #limit(int), #limit(int, int)
##### fetchFirstRows(...) -> This
- **Signature:** `public This fetchFirstRows(final int count)`
- **Summary:** Adds a FETCH FIRST N ROWS ONLY clause (SQL standard syntax).
- **Contract:**
  - Call #offsetRows(int) first when an offset is needed; #offset(int) is also compatible when this builder uses a FETCH-style dialect.
- **Parameters:**
  - `count` (`int`) — the number of rows to fetch
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .orderBy("id")
                    .fetchFirstRows(10)
                    .build().query();
    // Output: SELECT * FROM users ORDER BY id FETCH FIRST 10 ROWS ONLY
    ```
- **See also:** #limit(int), #limit(int, int)
##### append(...) -> This
- **Signature:** `@Beta public This append(final Condition condition)`
- **Summary:** Appends a condition to the SQL statement.
- **Contract:**
  - method: it must follow a SELECT segment completed by from(...), must precede ORDER BY, pagination, and FOR UPDATE, its operand must be a complete read-only SELECT sub-query, and afterwards only compound-result clauses (ORDER BY, pagination, FOR UPDATE) may follow.
- **Parameters:**
  - `condition` (`Condition`) — the condition to append
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .append(Filters.and(Filters.greaterThan("age", 18), Filters.lessThan("age", 65)))
                    .build().query();
    // Output: SELECT * FROM users WHERE (age > ?) AND (age < ?)
    ```
- **See also:** Filters
- **Signature:** `public This append(final String expr)`
- **Summary:** Appends a string expression to the SQL statement.
- **Contract:**
  - A single separating space is inserted before expr when, and only when, it is needed: that is, when the statement built so far does not already end with a space and expr does not already begin with one.
  - As a result both .append("FOR UPDATE") and .append(" FOR UPDATE") produce the same, correctly spaced output (a doubled space is possible only when the statement built so far already ends with a space and expr also begins with one).
- **Parameters:**
  - `expr` (`String`) — the expression to append
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .append("FOR UPDATE")
                    .build().query();
    // Output: SELECT * FROM users FOR UPDATE
    ```
##### appendIf(...) -> This
- **Signature:** `@Beta public This appendIf(final boolean b, final Condition condition)`
- **Summary:** Conditionally appends a condition to the SQL statement.
- **Parameters:**
  - `b` (`boolean`) — if true, the condition will be appended
  - `condition` (`Condition`) — the condition to append
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    boolean includeAgeFilter = true;
    String sql = PSC.select("*")
                    .from("users")
                    .appendIf(includeAgeFilter, Filters.greaterThan("age", 18))
                    .build().query();
    // Output: SELECT * FROM users WHERE age > ?
    ```
- **Signature:** `@Beta public This appendIf(final boolean b, final String expr)`
- **Summary:** Conditionally appends a string expression to the SQL statement.
- **Parameters:**
  - `b` (`boolean`) — if true, the expression will be appended
  - `expr` (`String`) — the expression to append
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    boolean includeForUpdate = true;
    String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.equal("id", 1))
                    .appendIf(includeForUpdate, " FOR UPDATE")
                    .build().query();
    // Output: SELECT * FROM users WHERE id = ? FOR UPDATE
    ```
##### appendIfOrElse(...) -> This
- **Signature:** `@Beta public This appendIfOrElse(final boolean b, final Condition conditionToAppendForTrue, final Condition conditionToAppendForFalse)`
- **Summary:** Conditionally appends one of two conditions based on a boolean value.
- **Parameters:**
  - `b` (`boolean`) — if true, append conditionToAppendForTrue; otherwise append conditionToAppendForFalse
  - `conditionToAppendForTrue` (`Condition`) — the condition to append if b is true
  - `conditionToAppendForFalse` (`Condition`) — the condition to append if b is false
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    boolean isActive = true;
    String sql = PSC.select("*")
                    .from("users")
                    .appendIfOrElse(isActive,
                        Filters.equal("status", "active"),
                        Filters.equal("status", "inactive"))
                    .build().query();
    // Output: SELECT * FROM users WHERE status = ?
    ```
- **Signature:** `@Beta public This appendIfOrElse(final boolean b, final String exprToAppendForTrue, final String exprToAppendForFalse)`
- **Summary:** Conditionally appends one of two string expressions based on a boolean value.
- **Parameters:**
  - `b` (`boolean`) — if true, append exprToAppendForTrue; otherwise append exprToAppendForFalse
  - `exprToAppendForTrue` (`String`) — the expression to append if b is true
  - `exprToAppendForFalse` (`String`) — the expression to append if b is false
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    boolean sortAscending = true;
    String sql = PSC.select("*")
                    .from("users")
                    .appendIfOrElse(sortAscending,
                        " ORDER BY name ASC",
                        " ORDER BY name DESC")
                    .build().query();
    // Output: SELECT * FROM users ORDER BY name ASC
    ```
##### union(...) -> This
- **Signature:** `public This union(final This sqlBuilder)`
- **Summary:** Adds a UNION clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to union (must not be null and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    SqlBuilder query1 = PSC.select("id", "name").from("users");
    SqlBuilder query2 = PSC.select("id", "name").from("customers");
    String sql = query1.union(query2).build().query();
    // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
    ```
- **Signature:** `public This union(final String query)`
- **Summary:** Adds a UNION clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the complete read-only SELECT sub-query to union
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("id", "name")
                    .from("users")
                    .union("SELECT id, name FROM customers")
                    .build().query();
    // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
    ```
- **Signature:** `public This union(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for UNION operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the union query
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("id", "name");
    String sql = PSC.select("id", "name")
                    .from("users")
                    .union(columns)
                    .from("customers")
                    .build().query();
    // Output: SELECT id, name FROM users UNION SELECT id, name FROM customers
    ```
##### unionAll(...) -> This
- **Signature:** `public This unionAll(final This sqlBuilder)`
- **Summary:** Adds a UNION ALL clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to union all (must not be null and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    SqlBuilder query1 = PSC.select("id", "name").from("users");
    SqlBuilder query2 = PSC.select("id", "name").from("customers");
    String sql = query1.unionAll(query2).build().query();
    // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
    ```
- **Signature:** `public This unionAll(final String query)`
- **Summary:** Adds a UNION ALL clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the complete read-only SELECT sub-query to union all
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("id", "name")
                    .from("users")
                    .unionAll("SELECT id, name FROM customers")
                    .build().query();
    // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
    ```
- **Signature:** `public This unionAll(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for UNION ALL operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the union all query
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("id", "name");
    String sql = PSC.select("id", "name")
                    .from("users")
                    .unionAll(columns)
                    .from("customers")
                    .build().query();
    // Output: SELECT id, name FROM users UNION ALL SELECT id, name FROM customers
    ```
##### intersect(...) -> This
- **Signature:** `public This intersect(final This sqlBuilder)`
- **Summary:** Adds an INTERSECT clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to intersect (must not be null and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    SqlBuilder query1 = PSC.select("id", "name").from("users");
    SqlBuilder query2 = PSC.select("id", "name").from("customers");
    String sql = query1.intersect(query2).build().query();
    // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM customers
    ```
- **Signature:** `public This intersect(final String query)`
- **Summary:** Adds an INTERSECT clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the complete read-only SELECT sub-query to intersect
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("id", "name")
                    .from("users")
                    .intersect("SELECT id, name FROM premium_users")
                    .build().query();
    // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM premium_users
    ```
- **Signature:** `public This intersect(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for INTERSECT operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the intersect query
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("id", "name");
    String sql = PSC.select("id", "name")
                    .from("users")
                    .intersect(columns)
                    .from("premium_users")
                    .build().query();
    // Output: SELECT id, name FROM users INTERSECT SELECT id, name FROM premium_users
    ```
##### except(...) -> This
- **Signature:** `public This except(final This sqlBuilder)`
- **Summary:** Adds an EXCEPT clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to except (must not be null and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    SqlBuilder query1 = PSC.select("id", "name").from("users");
    SqlBuilder query2 = PSC.select("id", "name").from("customers");
    String sql = query1.except(query2).build().query();
    // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM customers
    ```
- **Signature:** `public This except(final String query)`
- **Summary:** Adds an EXCEPT clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the complete read-only SELECT sub-query to except
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("id", "name")
                    .from("users")
                    .except("SELECT id, name FROM inactive_users")
                    .build().query();
    // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM inactive_users
    ```
- **Signature:** `public This except(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for EXCEPT operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the except query
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("id", "name");
    String sql = PSC.select("id", "name")
                    .from("users")
                    .except(columns)
                    .from("inactive_users")
                    .build().query();
    // Output: SELECT id, name FROM users EXCEPT SELECT id, name FROM inactive_users
    ```
##### minus(...) -> This
- **Signature:** `public This minus(final This sqlBuilder)`
- **Summary:** Adds a MINUS clause with another SQL query (Oracle syntax).
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to minus (must not be null and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    SqlBuilder query1 = PSC.select("id", "name").from("users");
    SqlBuilder query2 = PSC.select("id", "name").from("inactive_users");
    String sql = query1.minus(query2).build().query();
    // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
    ```
- **Signature:** `public This minus(final String query)`
- **Summary:** Adds a MINUS clause with a SQL query string (Oracle syntax).
- **Parameters:**
  - `query` (`String`) — the complete read-only SELECT sub-query to subtract with MINUS
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("id", "name")
                    .from("users")
                    .minus("SELECT id, name FROM inactive_users")
                    .build().query();
    // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
    ```
- **Signature:** `public This minus(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for MINUS operation with a collection of columns (Oracle syntax).
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the minus query
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("id", "name");
    String sql = PSC.select("id", "name")
                    .from("users")
                    .minus(columns)
                    .from("inactive_users")
                    .build().query();
    // Output: SELECT id, name FROM users MINUS SELECT id, name FROM inactive_users
    ```
##### forUpdate(...) -> This
- **Signature:** `public This forUpdate()`
- **Summary:** Adds a FOR UPDATE clause to lock selected rows.
- **Parameters:**
  - (none)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.equal("id", 1))
                    .forUpdate()
                    .build().query();
    // Output: SELECT * FROM users WHERE id = ? FOR UPDATE
    ```
##### set(...) -> This
- **Signature:** `public This set(final String expr)`
- **Summary:** Sets a single column or raw assignment expression for an UPDATE operation.
- **Contract:**
  - If expr contains an = sign, it is treated as a complete assignment and no placeholder is generated (identifiers are still normalized according to the naming policy).
- **Parameters:**
  - `expr` (`String`) — a column name (placeholder will be appended) or a complete col = value assignment
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    // Raw assignment expression (already contains '='):
    String sql = PSC.update("users")
                    .set("name = 'John'")
                    .where(Filters.equal("id", 1))
                    .build().query();
    // Output: UPDATE users SET name = 'John' WHERE id = ?
    
    // Column name (placeholder is generated):
    String sql2 = PSC.update("users")
                     .set("status")
                     .where(Filters.equal("id", 1))
                     .build().query();
    // Output: UPDATE users SET status = ? WHERE id = ?
    ```
- **Signature:** `public This set(final String... propOrColumnNames)`
- **Summary:** Sets columns for UPDATE operation.
- **Contract:**
  - If a column name already contains an = sign, it is treated as a raw SET expression and no placeholder is appended.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to update
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.update("users")
                    .set("firstName", "lastName", "email")
                    .where(Filters.equal("id", 1))
                    .build().query();
    // Output: UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?
    ```
- **Signature:** `public This set(final Collection<String> propOrColumnNames)`
- **Summary:** Sets columns for UPDATE operation with a collection of property or column names.
- **Contract:**
  - If a column name already contains an = sign, it is treated as a raw SET expression and no placeholder is appended.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to update
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("firstName", "lastName", "email");
    String sql = PSC.update("users")
                    .set(columns)
                    .where("id = ?")
                    .build().query();
    // Output: UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?
    ```
- **Signature:** `public This set(final Map<String, Object> props)`
- **Summary:** Sets columns and values for UPDATE operation using a map.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of column names to values
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("firstName", "John");
    values.put("lastName", "Doe");
    String sql = PSC.update("users")
                    .set(values)
                    .where(Filters.equal("id", 1))
                    .build().query();
    // Output: UPDATE users SET first_name = ?, last_name = ? WHERE id = ?
    ```
- **Signature:** `@Deprecated public This set(final Object entity)`
- **Summary:** Sets properties to update from an entity object, a Map, or a single column-name String.
- **Parameters:**
  - `entity` (`Object`) — the entity object, Map<String, Object>, or column-name String containing properties to set
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Deprecated public This set(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Sets properties to update from an entity object, a Map, or a single column-name String, excluding the specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object, Map<String, Object>, or column-name String containing properties to set
  - `excludedPropNames` (`Set<String>`) — property names to exclude from the update (may be null)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Deprecated public This set(final Class<?> entityClass)`
- **Summary:** Sets all updatable properties from an entity class for UPDATE operation.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to get properties from
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Deprecated public This set(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Sets updatable properties from an entity class for UPDATE operation, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to get properties from
  - `excludedPropNames` (`Set<String>`) — additional properties to exclude from the update
- **Returns:** this SqlBuilder instance for method chaining
##### setEntity(...) -> This
- **Signature:** `public This setEntity(final Object entity)`
- **Summary:** Sets properties to update from an entity object, a Map, or a single column-name String.
- **Parameters:**
  - `entity` (`Object`) — the entity object, Map<String, Object>, or column-name String containing properties to set
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.update("account")
                    .setEntity(accountEntity)
                    .where(Filters.equal("id", 1))
                    .build().query();
    ```
- **Signature:** `public This setEntity(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Sets properties to update from an entity object, a Map, or a single column-name String, excluding the specified properties.
- **Contract:**
  - Entity metadata selection, bean-property extraction, and SET rendering are atomic: if a getter or parameter renderer fails, this builder is restored to the state it had before this call.
- **Parameters:**
  - `entity` (`Object`) — the entity object, Map<String, Object>, or column-name String containing properties to set
  - `excludedPropNames` (`Set<String>`) — property names to exclude from the update (may be null)
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("createdDate", "version");
    String sql = PSC.update("account")
                    .setEntity(accountEntity, excluded)
                    .where(Filters.equal("id", 1))
                    .build().query();
    ```
- **Signature:** `public This setEntity(final Class<?> entityClass)`
- **Summary:** Sets all updatable properties from an entity class for UPDATE operation.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to get properties from
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    String sql = PSC.update("account")
                    .setEntity(Account.class)
                    .where(Filters.equal("id", 1))
                    .build().query();
    // Output: UPDATE account SET id = ?, gui = ?, email_address = ?, first_name = ?, middle_name = ?, last_name = ?, birth_date = ?, status = ?, last_update_time = ?, create_time = ?, contact = ? WHERE id = ?
    ```
- **Signature:** `public This setEntity(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Sets updatable properties from an entity class for UPDATE operation, excluding specified properties.
- **Contract:**
  - Entity metadata selection and SET rendering are atomic: if parameter rendering fails, this builder is restored to the state it had before this call.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to get properties from
  - `excludedPropNames` (`Set<String>`) — additional properties to exclude from the update
- **Returns:** this SqlBuilder instance for method chaining
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("lastUpdateTime");
    String sql = PSC.update("account")
                    .setEntity(Account.class, excluded)
                    .where(Filters.equal("id", 1))
                    .build().query();
    // Output: UPDATE account SET id = ?, gui = ?, email_address = ?, first_name = ?, middle_name = ?, last_name = ?, birth_date = ?, status = ?, create_time = ?, contact = ? WHERE id = ?
    ```
##### build(...) -> SP
- **Signature:** `public SP build()`
- **Summary:** Generates the final SQL string and its parameters as an SP pair, then releases resources.
- **Parameters:**
  - (none)
- **Returns:** an SP (SQL-Parameters) pair containing the SQL string and parameter list
- **Examples:**
  - ```java
    // Get SQL and parameters together
    SP sqlPair = PSC.select("*")
                    .from("account")
                    .where(Filters.equal("status", "ACTIVE"))
                    .build();
    // sqlPair.query() returns: "SELECT * FROM account WHERE status = ?"
    // sqlPair.parameters() returns: ["ACTIVE"]
    
    // Get just the SQL string
    String sql = PSC.select("*")
                    .from("users")
                    .where(Filters.equal("status", "ACTIVE"))
                    .build()
                    .query();
    // Output: SELECT * FROM users WHERE status = ?
    ```
##### apply(...) -> T
- **Signature:** `@Beta public <T, E extends Exception> T apply(final Throwables.Function<? super SP, T, E> function) throws E`
- **Summary:** Applies a function to the SQL-Parameters pair and returns the result.
- **Parameters:**
  - `function` (`Throwables.Function<? super SP, T, E>`) — the function to apply to the SP pair
- **Returns:** the result of applying the function
- **Throws:**
  - `E` — if the function throws an exception
- **Examples:**
  - ```java
    List<Account> accounts = PSC.select("*")
        .from("account")
        .where(Filters.equal("status", "ACTIVE"))
        .apply(sp -> jdbcTemplate.query(sp.query(), sp.parameters(), accountRowMapper));
    ```
- **Signature:** `@Beta public <T, E extends Exception> T apply(final Throwables.BiFunction<? super String, ? super List<Object>, T, E> function) throws E`
- **Summary:** Applies a bi-function to the SQL string and parameters separately and returns the result.
- **Parameters:**
  - `function` (`Throwables.BiFunction<? super String, ? super List<Object>, T, E>`) — the bi-function to apply to the SQL and parameters
- **Returns:** the result of applying the function
- **Throws:**
  - `E` — if the function throws an exception
- **Examples:**
  - ```java
    int count = PSC.update("account")
        .set("status", "lastLogin")
        .where(Filters.lessThan("lastLogin", oneYearAgo))
        .apply((sql, params) -> jdbcTemplate.update(sql, params.toArray()));
    ```
##### accept(...) -> void
- **Signature:** `@Beta public <E extends Exception> void accept(final Throwables.Consumer<? super SP, E> consumer) throws E`
- **Summary:** Accepts a consumer for the SQL-Parameters pair.
- **Contract:**
  - This is useful for executing the SQL with a data access framework when no return value is needed.
- **Parameters:**
  - `consumer` (`Throwables.Consumer<? super SP, E>`) — the consumer to accept the SP pair
- **Throws:**
  - `E` — if the consumer throws an exception
- **Examples:**
  - ```java
    PSC.insert("name", "email", "status")
       .into("account")
       .accept(sp -> jdbcTemplate.update(sp.query(), sp.parameters().toArray()));
    ```
- **Signature:** `@Beta public <E extends Exception> void accept(final Throwables.BiConsumer<? super String, ? super List<Object>, E> consumer) throws E`
- **Summary:** Accepts a bi-consumer for the SQL string and parameters separately.
- **Contract:**
  - This is useful for executing the SQL with a data access framework when no return value is needed.
- **Parameters:**
  - `consumer` (`Throwables.BiConsumer<? super String, ? super List<Object>, E>`) — the bi-consumer to accept the SQL and parameters
- **Throws:**
  - `E` — if the consumer throws an exception
- **Examples:**
  - ```java
    PSC.deleteFrom("account")
       .where(Filters.equal("status", "DELETED"))
       .accept((sql, params) -> {
           logger.info("Executing SQL with {} parameters", params.size());
           jdbcTemplate.update(sql, params.toArray());
       });
    ```
##### debugPrint(...) -> void
- **Signature:** `@Beta public void debugPrint()`
- **Summary:** Builds the SQL and prints the resulting query string to standard output.
- **Parameters:**
  - (none)
- **Examples:**
  - ```java
    PSC.select("*")
       .from("account")
       .where(Filters.between("age", 18, 65))
       .debugPrint();
    // Prints: SELECT * FROM account WHERE age BETWEEN ? AND ?
    ```

### Record SP (com.landawn.abacus.query.AbstractQueryBuilder.SP)
Represents a SQL string and its associated parameters.

**Thread-safety:** thread-safe
**Nullability:** unspecified

#### Public Constructors
- `public SP` — Creates an immutable SQL/parameter pair.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Dsl (com.landawn.abacus.query.Dsl)
Entry point for building SQL statements with a fixed SqlDialect, including its naming, parameter, identifier-quoting, database-product, named-parameter-rendering, and tokenizer settings.

**Thread-safety:** thread-safe
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### forDialect(...) -> Dsl
- **Signature:** `public static Dsl forDialect(final SqlDialect sqlDialect)`
- **Summary:** Creates a Dsl bound to the given SqlDialect, fixing the naming policy and parameter style of every SqlBuilder it produces.
- **Contract:**
  - The returned Dsl is immutable and thread-safe when any custom named-parameter handler is safe for concurrent invocation, so it is typically stored in a static final field and reused.
- **Parameters:**
  - `sqlDialect` (`SqlDialect`) — the complete immutable rendering and tokenizer configuration the DSL is bound to
- **Returns:** a Dsl that produces SqlBuilder instances using the given dialect; a shared cached instance is returned for the predefined dialect combinations, otherwise a new instance
- **Examples:**
  - ```java
    static final Dsl MY_DSL = Dsl.forDialect(SqlDialect.builder()
            .namingPolicy(NamingPolicy.SNAKE_CASE)
            .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
            .build());
    
    String sql = MY_DSL.insert("firstName", "lastName").into("account").build().query();
    // Output: INSERT INTO account (first_name, last_name) VALUES (?, ?)
    ```

#### Public Instance Methods
##### sqlDialect(...) -> SqlDialect
- **Signature:** `public SqlDialect sqlDialect()`
- **Summary:** Returns the SqlDialect this DSL is bound to; every SqlBuilder it produces renders SQL with this dialect.
- **Parameters:**
  - (none)
- **Returns:** the complete rendering and tokenizer configuration bound to this DSL
##### insert(...) -> SqlBuilder
- **Signature:** `public SqlBuilder insert(final String propOrColumnName)`
- **Summary:** Creates an INSERT statement for a single column.
- **Contract:**
  - The actual value will be provided as a parameter when executing the query.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    String sql = PSC.insert("firstName").into("account").build().query();
    // Output: INSERT INTO account (first_name) VALUES (?)
    ```
- **Signature:** `public SqlBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT statement for multiple columns.
- **Contract:**
  - Values will be provided as parameters when executing the query.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    String sql = PSC.insert("firstName", "lastName", "email")
                    .into("account")
                    .build().query();
    // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
    ```
- **Signature:** `public SqlBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT statement for a collection of columns.
- **Contract:**
  - This method provides flexibility when column names are dynamically generated or come from a collection.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("firstName", "lastName", "email");
    String sql = PSC.insert(columns).into("account").build().query();
    // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
    ```
- **Signature:** `public SqlBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT statement from a map of property names and values.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of property names to their values
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("firstName", "John");
    props.put("lastName", "Doe");
    SP sqlPair = PSC.insert(props).into("account").build();
    // sqlPair.query(): INSERT INTO account (first_name, last_name) VALUES (?, ?)
    // sqlPair.parameters(): ["John", "Doe"]
    ```
- **Signature:** `public SqlBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT statement from an entity object.
- **Contract:**
  - Properties whose value is null are also skipped, as are ID properties still holding their default value (for a composite ID, only when every ID property holds its default value).
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    Account account = new Account();
    account.setFirstName("John");
    account.setLastName("Doe");
    account.setEmail("john.doe@example.com");
    
    SP sqlPair = PSC.insert(account).into("account").build();
    // sqlPair.query(): INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
    // sqlPair.parameters(): ["John", "Doe", "john.doe@example.com"]
    ```
- **Signature:** `public SqlBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement from an entity object with excluded properties.
- **Contract:**
  - Properties in the exclusion set will not be included even if they have values and are normally insertable.
  - Properties whose value is null are also skipped, as are ID properties still holding their default value (for a composite ID, only when every ID property holds its default value).
  - When entity is a String column name, excludedPropNames is ignored and the named column is always inserted.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    Account account = new Account();
    account.setFirstName("John");
    account.setLastName("Doe");
    account.setEmail("john.doe@example.com");
    account.setCreatedDate(new Date());
    
    Set<String> excluded = N.asSet("createdDate");
    SP sqlPair = PSC.insert(account, excluded).into("account").build();
    // sqlPair.query(): INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
    // sqlPair.parameters(): ["John", "Doe", "john.doe@example.com"]
    ```
- **Signature:** `public SqlBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    String sql = PSC.insert(Account.class).into("account").build().query();
    // Output: INSERT INTO account (first_name, last_name, email, created_date) VALUES (?, ?, ?, ?)
    ```
- **Signature:** `public SqlBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("createdDate", "modifiedDate");
    String sql = PSC.insert(Account.class, excluded).into("account").build().query();
    // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
    ```
##### insertInto(...) -> SqlBuilder
- **Signature:** `public SqlBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO for
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    String sql = PSC.insertInto(Account.class).build().query();
    // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
    ```
- **Signature:** `public SqlBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("id", "createdDate");
    String sql = PSC.insertInto(Account.class, excluded).build().query();
    // Output: INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
    ```
##### batchInsert(...) -> SqlBuilder
- **Signature:** `@Beta public SqlBuilder batchInsert(final Collection<?> entitiesOrPropMaps)`
- **Summary:** Generates a MySQL-style batch INSERT statement.
- **Parameters:**
  - `entitiesOrPropMaps` (`Collection<?>`) — list of entities or property maps to insert
- **Returns:** a new SqlBuilder instance configured for batch INSERT operation
- **Examples:**
  - ```java
    List<Account> accounts = Arrays.asList(
        new Account("John", "Doe"),
        new Account("Jane", "Smith"),
        new Account("Bob", "Johnson")
    );
    
    SP sqlPair = PSC.batchInsert(accounts).into("account").build();
    // sqlPair.query(): INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?), (?, ?)
    // sqlPair.parameters(): ["John", "Doe", "Jane", "Smith", "Bob", "Johnson"]
    ```
##### update(...) -> SqlBuilder
- **Signature:** `public SqlBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE statement for a table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SqlBuilder instance configured for UPDATE operation
- **Examples:**
  - ```java
    String sql = PSC.update("account")
                    .set("firstName", "lastName")
                    .where(Filters.equal("id", 1))
                    .build().query();
    // Output: UPDATE account SET first_name = ?, last_name = ? WHERE id = ?
    ```
- **Signature:** `public SqlBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SqlBuilder instance configured for UPDATE operation
- **Examples:**
  - ```java
    String sql = PSC.update("account", Account.class)
                    .set("firstName", "lastModified")
                    .where(Filters.equal("id", 1))
                    .build().query();
    // Output: UPDATE account SET first_name = ?, last_modified = ? WHERE id = ?
    ```
- **Signature:** `public SqlBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for an entity class.
- **Contract:**
  - A WHERE clause should be added before calling build().
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SqlBuilder instance configured for UPDATE operation
- **Examples:**
  - ```java
    String sql = PSC.update(Account.class)
                    .where(Filters.equal("id", 1))
                    .build().query();
    // Output: UPDATE account SET first_name = ?, last_name = ?, email = ?, ... WHERE id = ?
    ```
- **Signature:** `public SqlBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE statement for an entity class with excluded properties.
- **Contract:**
  - This is useful for partial updates or when certain fields should never be updated.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the update
- **Returns:** a new SqlBuilder instance configured for UPDATE operation
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("createdDate", "createdBy");
    String sql = PSC.update(Account.class, excluded)
                    .set(account)
                    .where(Filters.equal("id", account.getId()))
                    .build().query();
    ```
##### deleteFrom(...) -> SqlBuilder
- **Signature:** `public SqlBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM statement for a table.
- **Contract:**
  - Property names in WHERE conditions will be rendered according to this DSL's naming policy if an entity class is associated.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SqlBuilder instance configured for DELETE operation
- **Examples:**
  - ```java
    String sql = PSC.deleteFrom("account")
                    .where(Filters.equal("status", "inactive"))
                    .build().query();
    // Output: DELETE FROM account WHERE status = ?
    ```
- **Signature:** `public SqlBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SqlBuilder instance configured for DELETE operation
- **Examples:**
  - ```java
    String sql = PSC.deleteFrom("account", Account.class)
                    .where(Filters.lessThan("lastLoginDate", thirtyDaysAgo))
                    .build().query();
    // Output: DELETE FROM account WHERE last_login_date < ?
    ```
- **Signature:** `public SqlBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SqlBuilder instance configured for DELETE operation
- **Examples:**
  - ```java
    String sql = PSC.deleteFrom(Account.class)
                    .where(Filters.equal("id", 1))
                    .build().query();
    // Output: DELETE FROM account WHERE id = ?
    ```
##### select(...) -> SqlBuilder
- **Signature:** `public SqlBuilder select(final String expr)`
- **Summary:** Creates a SELECT statement with a single expression.
- **Contract:**
  - This method is useful for complex select expressions, aggregate functions, or when selecting computed values.
- **Parameters:**
  - `expr` (`String`) — the select expression
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.select("COUNT(*)")
                    .from("account")
                    .where(Filters.equal("status", "active"))
                    .build().query();
    // Output: SELECT COUNT(*) FROM account WHERE status = ?
    // (COUNT is recognized as a function name and left unchanged)
    
    String sql2 = PSC.select("firstName || ' ' || lastName AS fullName")
                     .from("account")
                     .build().query();
    // Output: SELECT first_name || ' ' || last_name AS fullName FROM account
    // (identifiers converted to snake_case, the AS alias preserved)
    ```
- **Signature:** `public SqlBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT statement with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.select("id", "firstName", "lastName", "email")
                    .from("account")
                    .where(Filters.equal("status", "active"))
                    .build().query();
    // Output: SELECT id, first_name AS "firstName", last_name AS "lastName", email FROM account WHERE status = ?
    ```
- **Signature:** `public SqlBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT statement with a collection of columns.
- **Contract:**
  - This method provides flexibility when column names are dynamically generated.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("id", "firstName", "lastName");
    String sql = PSC.select(columns)
                    .from("account")
                    .build().query();
    // Output: SELECT id, first_name AS "firstName", last_name AS "lastName" FROM account
    ```
- **Signature:** `public SqlBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT statement with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of property/column names to their aliases
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    Map<String, String> columnAliases = new LinkedHashMap<>();
    columnAliases.put("firstName", "fname");
    columnAliases.put("lastName", "lname");
    columnAliases.put("emailAddress", "email");
    
    String sql = PSC.select(columnAliases)
                    .from("account")
                    .build().query();
    // Output: SELECT first_name AS "fname", last_name AS "lname", email_address AS "email" FROM account
    ```
- **Signature:** `public SqlBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT statement for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.select(Account.class)
                    .from("account")
                    .build().query();
    // Output: SELECT id AS "id", first_name AS "firstName", last_name AS "lastName", email AS "email", created_date AS "createdDate" FROM account
    ```
- **Signature:** `public SqlBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement for an entity class with optional sub-entity properties.
- **Contract:**
  - When includeSubEntityProperties is true, properties of nested entity objects are also included in the selection with appropriate prefixes.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    // Without sub-entities
    String sql1 = PSC.select(Order.class, false)
                     .from("orders")
                     .build().query();
    
    // With sub-entities (includes nested object properties)
    String sql2 = PSC.select(Order.class, true)
                     .from("orders")
                     .build().query();
    ```
- **Signature:** `public SqlBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("password", "secretKey");
    String sql = PSC.select(Account.class, excluded)
                    .from("account")
                    .build().query();
    // Selects all Account properties except password and secretKey
    ```
- **Signature:** `public SqlBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with sub-entities and exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("internalNotes", "auditLog");
    String sql = PSC.select(Order.class, true, excluded)
                    .from("orders")
                    .build().query();
    // Selects all Order properties including sub-entities, except excluded ones
    ```
- **Signature:** `@Deprecated public SqlBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT statement for multiple entity classes (for joins).
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity results
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity results
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.select(Account.class, "a", "account",
                           Order.class, "o", "order")
                    .from("account a")
                    .innerJoin("orders o").on("a.id = o.account_id")
                    .build().query();
    // Output: SELECT a.id AS "account.id", a.first_name AS "account.firstName", ...,
    //                o.id AS "order.id", o.order_date AS "order.orderDate", ...
    ```
- **Signature:** `@Deprecated public SqlBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT statement for multiple entity classes with exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity results
  - `excludedPropNamesA` (`Set<String>`) — excluded properties for first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity results
  - `excludedPropNamesB` (`Set<String>`) — excluded properties for second entity
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    Set<String> userExclude = N.asSet("password", "salt");
    Set<String> orderExclude = N.asSet("internalNotes");
    
    String sql = PSC.select(Account.class, "a", "account", userExclude,
                           Order.class, "o", "order", orderExclude)
                    .from("account a")
                    .innerJoin("orders o").on("a.id = o.account_id")
                    .build().query();
    ```
- **Signature:** `public SqlBuilder select(final Selection selection)`
- **Summary:** Creates a SELECT statement from a single Selection descriptor.
- **Contract:**
  - Prefer it over the positional select(Class, ...) overloads when you need full control (table alias, class alias, sub-entity inclusion, property exclusion) over a single entity, because each attribute is set through a named Selection.SelectionBuilder method rather than by argument position.
- **Parameters:**
  - `selection` (`Selection`) — the selection descriptor defining the entity, aliases, and property filtering; must not be null
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    SqlBuilder sql = PSC.select(Selection.builder(Account.class)
            .tableAlias("a")
            .classAlias("account")
            .excludedPropNames(N.asSet("password"))
            .build());
    ```
- **See also:** #select(List), Selection
- **Signature:** `public SqlBuilder select(final List<Selection> selections)`
- **Summary:** Creates a SELECT statement for multiple entities using Selection descriptors.
- **Parameters:**
  - `selections` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    List<Selection> selections = Arrays.asList(
        Selection.builder(Account.class).tableAlias("a").classAlias("account").build(),
        Selection.builder(Order.class).tableAlias("o").classAlias("order")
            .includedPropNames(Arrays.asList("id", "total")).build(),
        Selection.builder(Product.class).tableAlias("p").classAlias("product")
            .excludedPropNames(N.asSet("description")).build()
    );
    
    String sql = PSC.select(selections)
                    .from("account a")
                    .innerJoin("orders o").on("a.id = o.account_id")
                    .innerJoin("order_items oi").on("o.id = oi.order_id")
                    .innerJoin("products p").on("oi.product_id = p.id")
                    .build().query();
    ```
##### selectFrom(...) -> SqlBuilder
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.selectFrom(Account.class)
                    .where(Filters.equal("status", "active"))
                    .build().query();
    // Output: SELECT id AS "id", first_name AS "firstName", last_name AS "lastName", email AS "email" FROM account WHERE status = ?
    ```
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final String tableAlias)`
- **Summary:** Creates a SELECT FROM statement for an entity class with table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the table alias to use
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.selectFrom(Account.class, "a")
                    .where(Filters.equal("a.status", "active"))
                    .build().query();
    // Output: SELECT a.id AS "id", a.first_name AS "firstName", a.last_name AS "lastName", a.email AS "email" FROM account a WHERE a.status = ?
    ```
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with optional sub-entity properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.selectFrom(Order.class, true)
                    .where(Filters.greaterThan("total", 100))
                    .build().query();
    // Includes properties from nested entities like customer, items, etc.
    ```
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final String tableAlias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with alias and sub-entity option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.selectFrom(Order.class, "o", true)
                    .where(Filters.equal("o.status", "pending"))
                    .build().query();
    // Selects from orders with alias 'o' including sub-entity properties
    ```
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("password", "secretKey");
    String sql = PSC.selectFrom(Account.class, excluded)
                    .where(Filters.equal("active", true))
                    .build().query();
    // Selects all properties except password and secretKey
    ```
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final String tableAlias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with alias and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("password");
    String sql = PSC.selectFrom(Account.class, "a", excluded)
                    .innerJoin("orders o").on("a.id = o.account_id")
                    .build().query();
    // Selects from account with alias 'a', excluding password
    ```
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with sub-entities and exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("internalData");
    String sql = PSC.selectFrom(Order.class, true, excluded)
                    .where(Filters.between("orderDate", startDate, endDate))
                    .build().query();
    // Includes sub-entities but excludes internalData
    ```
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final String tableAlias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT FROM statement with all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    Set<String> excluded = N.asSet("password", "internalNotes");
    String sql = PSC.selectFrom(Account.class, "a", true, excluded)
                    .innerJoin("orders o").on("a.id = o.account_id")
                    .where(Filters.greaterThan("o.total", 1000))
                    .build().query();
    // Complex query with full control over selection
    ```
- **Signature:** `@Deprecated public SqlBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT FROM statement for multiple entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity
- **Returns:** a new SqlBuilder instance with SELECT and FROM configured
- **Examples:**
  - ```java
    String sql = PSC.selectFrom(Account.class, "a", "account",
                               Order.class, "o", "order")
                    .where(Filters.expr("a.id = o.account_id"))
                    .build().query();
    // Automatically generates appropriate FROM clause
    ```
- **Signature:** `@Deprecated public SqlBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM statement for multiple entity classes with exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity
  - `excludedPropNamesA` (`Set<String>`) — excluded properties for first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity
  - `excludedPropNamesB` (`Set<String>`) — excluded properties for second entity
- **Returns:** a new SqlBuilder instance with SELECT and FROM configured
- **Examples:**
  - ```java
    Set<String> userExclude = N.asSet("password");
    String sql = PSC.selectFrom(Account.class, "a", "account", userExclude,
                               Order.class, "o", "order", null)
                    .where(Filters.expr("a.id = o.account_id"))
                    .build().query();
    ```
- **Signature:** `public SqlBuilder selectFrom(final Selection selection)`
- **Summary:** Creates a SELECT ...
- **Contract:**
  - Prefer it over the positional selectFrom(Class, ...) overloads when configuring a single entity, since each attribute is set through a named Selection.SelectionBuilder method rather than by argument position.
- **Parameters:**
  - `selection` (`Selection`) — the selection descriptor defining the entity, aliases, and property filtering; must not be null
- **Returns:** a new SqlBuilder instance with SELECT and FROM configured
- **Examples:**
  - ```java
    SqlBuilder sql = PSC.selectFrom(Selection.builder(Account.class)
            .tableAlias("a")
            .classAlias("account")
            .includeSubEntityProperties(true)
            .build());
    ```
- **See also:** #selectFrom(List), Selection
- **Signature:** `public SqlBuilder selectFrom(final List<Selection> selections)`
- **Summary:** Creates a SELECT FROM statement for multiple entity selections.
- **Parameters:**
  - `selections` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SqlBuilder instance with SELECT and FROM configured
- **Examples:**
  - ```java
    List<Selection> selections = Arrays.asList(
        Selection.builder(Account.class).tableAlias("a").classAlias("account").build(),
        Selection.builder(Order.class).tableAlias("o").classAlias("order")
            .includeSubEntityProperties(true).build(),
        Selection.builder(Product.class).tableAlias("p").classAlias("product")
            .excludedPropNames(N.asSet("details")).build()
    );
    
    String sql = PSC.selectFrom(selections)
                    .where(Filters.equal("a.status", "active"))
                    .build().query();
    ```
##### count(...) -> SqlBuilder
- **Signature:** `public SqlBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) query for a table.
- **Parameters:**
  - `tableName` (`String`) — the table to count rows from
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.count("account")
                    .where(Filters.equal("status", "active"))
                    .build().query();
    // Output: SELECT count(*) FROM account WHERE status = ?
    ```
- **Signature:** `public SqlBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) query for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Examples:**
  - ```java
    String sql = PSC.count(Account.class)
                    .where(Filters.isNotNull("email"))
                    .build().query();
    // Output: SELECT count(*) FROM account WHERE email IS NOT NULL
    ```
##### renderCondition(...) -> SqlBuilder
- **Signature:** `public SqlBuilder renderCondition(final Condition condition, final Class<?> entityClass)`
- **Summary:** Renders a condition as a standalone SQL fragment, using the given entity class for property-to-column mapping.
- **Parameters:**
  - `condition` (`Condition`) — the condition to render (must not be null)
  - `entityClass` (`Class<?>`) — the entity class used for property-to-column mapping (may be null)
- **Returns:** a new SqlBuilder instance containing the rendered condition SQL
- **Examples:**
  - ```java
    Condition cond = Filters.and(
        Filters.equal("firstName", "John"),
        Filters.like("email", "%@example.com")
    );
    
    String sql = PSC.renderCondition(cond, Account.class).build().query();
    // Output: (first_name = ?) AND (email LIKE ?)
    ```
- **Signature:** `public SqlBuilder renderCondition(final Condition condition)`
- **Summary:** Renders a condition as a standalone SQL fragment without an entity class.
- **Parameters:**
  - `condition` (`Condition`) — the condition to render (must not be null)
- **Returns:** a new SqlBuilder instance containing the rendered condition SQL
- **Examples:**
  - ```java
    String sql = PSC.renderCondition(Filters.equal("firstName", "John")).build().query();
    // Output: first_name = ?
    ```
- **See also:** #renderCondition(Condition, Class)

### Class DynamicQuery (com.landawn.abacus.query.DynamicQuery)
Entry point for fluently creating dynamic SQL queries programmatically.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### builder(...) -> Builder
- **Signature:** `public static Builder builder()`
- **Summary:** Creates a new Builder instance for constructing a dynamic SQL query.
- **Parameters:**
  - (none)
- **Returns:** a new Builder instance
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.select().append("*");
    builder.from().append("users");
    builder.where().append("active = true");
    String sql = builder.build();
    // Returns: "SELECT * FROM users WHERE active = true"
    
    // Each call returns a new, independent builder
    Builder another = DynamicQuery.builder();   // not the same instance as 'builder'
    ```

#### Public Instance Methods
- (none)

### Class Builder (com.landawn.abacus.query.DynamicQuery.Builder)
Builder for constructing dynamic SQL queries clause by clause.

**Thread-safety:** not-thread-safe
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### select(...) -> SelectClause
- **Signature:** `public SelectClause select()`
- **Summary:** Returns the SelectClause builder for defining columns to retrieve.
- **Parameters:**
  - (none)
- **Returns:** the SelectClause builder for method chaining
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.select().append("id").append("name", "user_name");
    // Generates: SELECT id, name AS user_name
    ```
##### from(...) -> FromClause
- **Signature:** `public FromClause from()`
- **Summary:** Returns the FromClause builder for defining tables and joins.
- **Parameters:**
  - (none)
- **Returns:** the FromClause builder for method chaining
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.from().append("users", "u").leftJoin("orders o", "u.id = o.user_id");
    // Generates: FROM users u LEFT JOIN orders o ON u.id = o.user_id
    ```
##### where(...) -> WhereClause
- **Signature:** `public WhereClause where()`
- **Summary:** Returns the WhereClause builder for defining query conditions.
- **Parameters:**
  - (none)
- **Returns:** the WhereClause builder for method chaining
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.where().append("status = ?").and("created_date > ?");
    // Generates: WHERE status = ? AND created_date > ?
    ```
##### groupBy(...) -> GroupByClause
- **Signature:** `public GroupByClause groupBy()`
- **Summary:** Returns the GroupByClause builder for defining grouping columns.
- **Parameters:**
  - (none)
- **Returns:** the GroupByClause builder for method chaining
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.groupBy().append("department").append("year");
    // Generates: GROUP BY department, year
    ```
##### having(...) -> HavingClause
- **Signature:** `public HavingClause having()`
- **Summary:** Returns the HavingClause builder for defining conditions on grouped results.
- **Parameters:**
  - (none)
- **Returns:** the HavingClause builder for method chaining
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.having().append("COUNT(*) > ?").and("SUM(amount) < ?");
    // Generates: HAVING COUNT(*) > ? AND SUM(amount) < ?
    ```
##### orderBy(...) -> OrderByClause
- **Signature:** `public OrderByClause orderBy()`
- **Summary:** Returns the OrderByClause builder for defining result ordering.
- **Parameters:**
  - (none)
- **Returns:** the OrderByClause builder for method chaining
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.orderBy().append("created_date DESC").append("name ASC");
    // Generates: ORDER BY created_date DESC, name ASC
    ```
##### limit(...) -> Builder
- **Signature:** `public Builder limit(final int count)`
- **Summary:** Adds a LIMIT clause to restrict the number of rows returned.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must not be negative)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.select().append("*");
    builder.from().append("users");
    builder.limit(10);
    // Generates: LIMIT 10
    ```
- **Signature:** `public Builder limit(final int count, final int offset)`
- **Summary:** Adds a LIMIT clause with count and offset for pagination.
- **Contract:**
  - Generates: LIMIT count OFFSET offset; the OFFSET portion is omitted when offset is 0, emitting just LIMIT count.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must not be negative)
  - `offset` (`int`) — the number of rows to skip (must not be negative)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.select().append("*");
    builder.from().append("users");
    builder.limit(10, 20);  // count=10, offset=20
    // Generates: SELECT * FROM users LIMIT 10 OFFSET 20 (skip 20 rows, return next 10)
    ```
- **See also:** #offsetRows(int), #fetchNextRows(int), #fetchFirstRows(int)
##### offset(...) -> Builder
- **Signature:** `public Builder offset(final int offset)`
- **Summary:** Adds a plain OFFSET clause to skip the given number of leading rows.
- **Contract:**
  - When offset is 0, nothing is appended.
  - Use #offsetRows(int) instead when you need the SQL:2008 OFFSET n ROWS form (typically paired with #fetchNextRows(int) or #fetchFirstRows(int)).
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip (must not be negative)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.limit(10).offset(20);
    // Generates: LIMIT 10 OFFSET 20
    ```
- **See also:** #offsetRows(int)
##### offsetRows(...) -> Builder
- **Signature:** `public Builder offsetRows(final int offset)`
- **Summary:** Adds an OFFSET clause for SQL:2008 standard pagination.
- **Contract:**
  - When offset is 0, nothing is appended.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip (must not be negative)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.offsetRows(20).fetchNextRows(10);
    // Generates: OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
    ```
##### fetchNextRows(...) -> Builder
- **Signature:** `public Builder fetchNextRows(final int count)`
- **Summary:** Adds a FETCH NEXT clause for SQL:2008 standard result limiting.
- **Parameters:**
  - `count` (`int`) — the number of rows to fetch (must not be negative)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.offsetRows(100).fetchNextRows(25);
    // Generates: OFFSET 100 ROWS FETCH NEXT 25 ROWS ONLY
    ```
##### fetchFirstRows(...) -> Builder
- **Signature:** `public Builder fetchFirstRows(final int count)`
- **Summary:** Adds a FETCH FIRST clause for SQL:2008 standard result limiting.
- **Parameters:**
  - `count` (`int`) — the number of rows to fetch (must not be negative)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.fetchFirstRows(10);
    // Generates: FETCH FIRST 10 ROWS ONLY
    ```
- **See also:** #offsetRows(int)
##### union(...) -> Builder
- **Signature:** `public Builder union(final String query)`
- **Summary:** Adds a UNION operator to combine results with another query.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to union with (must not be null, empty, or blank)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.union("SELECT id, name FROM archived_users");
    // Appends: UNION SELECT id, name FROM archived_users
    ```
##### unionAll(...) -> Builder
- **Signature:** `public Builder unionAll(final String query)`
- **Summary:** Adds a UNION ALL operator to combine results with another query.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to union with (must not be null, empty, or blank)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.unionAll("SELECT id, name FROM temp_users");
    // Appends: UNION ALL SELECT id, name FROM temp_users
    ```
##### intersect(...) -> Builder
- **Signature:** `public Builder intersect(final String query)`
- **Summary:** Adds an INTERSECT operator to find common rows between queries.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to intersect with (must not be null, empty, or blank)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.intersect("SELECT user_id FROM premium_users");
    // Appends: INTERSECT SELECT user_id FROM premium_users
    ```
##### except(...) -> Builder
- **Signature:** `public Builder except(final String query)`
- **Summary:** Adds an EXCEPT operator to find rows in the first query but not in the second.
- **Parameters:**
  - `query` (`String`) — the complete SQL query whose result rows are subtracted from the current result set (must not be null, empty, or blank)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.except("SELECT user_id FROM blocked_users");
    // Appends: EXCEPT SELECT user_id FROM blocked_users
    ```
##### minus(...) -> Builder
- **Signature:** `public Builder minus(final String query)`
- **Summary:** Adds a MINUS operator to find rows in the first query but not in the second.
- **Parameters:**
  - `query` (`String`) — the complete SQL query whose result rows are subtracted from the current result set (must not be null, empty, or blank)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.minus("SELECT user_id FROM inactive_users");
    // Appends: MINUS SELECT user_id FROM inactive_users
    ```
##### append(...) -> Builder
- **Signature:** `public Builder append(final String textToAppend)`
- **Summary:** Appends a raw, database-specific SQL clause or fragment verbatim to the end of the query.
- **Contract:**
  - The supplied text is emitted unchanged (preceded by a separating space when needed; see below) and is not validated, escaped, or interpreted in any way \\u2014 whatever you pass becomes the literal tail of the generated SQL.
  - A single separating space is inserted before textToAppend when, and only when, it is needed: that is, when the text built so far does not already end with a space and textToAppend does not already begin with one.
  - Consequently, either orderBy().append(...) or a trailing append("ORDER BY ...") can order a combined set-operation result; prefer the typed form when it is sufficient.
- **Parameters:**
  - `textToAppend` (`String`) — the complete raw SQL clause to append verbatim (e.g., "LIMIT 10 OFFSET 20") (must not be null, empty, or blank)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.append("LIMIT 10 OFFSET 20");
    // Or any trailing clause that has no typed overload (e.g. a SQL:2008 row-limiting clause)
    builder.append("FETCH FIRST 10 ROWS ONLY");
    ```
##### appendIf(...) -> Builder
- **Signature:** `public Builder appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a raw SQL clause or fragment verbatim to the end of the query.
- **Contract:**
  - When b is true this behaves exactly like #append(String) (a single separating space is inserted only when needed, then the text is emitted unchanged with no validation, escaping, or interpretation); when b is false the builder is left unchanged and textToAppend is not inspected.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the raw SQL clause to append verbatim if b is true (must not be null, empty, or blank when b is true)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.appendIf(paged, "LIMIT 10 OFFSET 20")
           .appendIf(locked, "FOR UPDATE");
    ```
- **See also:** #append(String)
##### appendIfOrElse(...) -> Builder
- **Signature:** `public Builder appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two raw SQL clauses verbatim to the end of the query based on a boolean condition.
- **Contract:**
  - Always appends something, choosing between the two options; the chosen text is emitted exactly as #append(String) would emit it (a single separating space is inserted only when needed, with no validation, escaping, or interpretation).
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the raw SQL clause to append if condition is true (must not be null, empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the raw SQL clause to append if condition is false (must not be null, empty, or blank)
- **Returns:** this builder instance for method chaining
- **Examples:**
  - ```java
    builder.appendIfOrElse(smallPage, "LIMIT 10", "LIMIT 100");
    ```
- **See also:** #append(String)
##### build(...) -> String
- **Signature:** `public String build()`
- **Summary:** Builds the final SQL string from all the components and releases resources.
- **Contract:**
  - This method MUST be called to get the SQL and clean up internal resources.
  - After calling build(), this builder is closed and must not be reused: any subsequent call to a builder method (including build() itself) throws IllegalStateException.
- **Parameters:**
  - (none)
- **Returns:** the complete SQL query string
- **Examples:**
  - ```java
    Builder builder = DynamicQuery.builder();
    builder.select().append("*");
    builder.from().append("users");
    builder.where().append("active = true");
    String sql = builder.build();
    // Returns: "SELECT * FROM users WHERE active = true"
    ```

### Class SelectClause (com.landawn.abacus.query.DynamicQuery.SelectClause)
Builder class for constructing the SELECT clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> SelectClause
- **Signature:** `public SelectClause append(final String column)`
- **Summary:** Appends a single column to the SELECT clause.
- **Parameters:**
  - `column` (`String`) — the column name to select (must not be null, empty, or blank)
- **Returns:** this SelectClause instance for method chaining
- **Examples:**
  - ```java
    select.append("user_id").append("username");
    // Generates: SELECT user_id, username
    ```
- **Signature:** `public SelectClause append(final String column, final String alias)`
- **Summary:** Appends a column with an alias to the SELECT clause.
- **Parameters:**
  - `column` (`String`) — the column name to select (must not be null, empty, or blank)
  - `alias` (`String`) — the alias for the column (must not be null, empty, or blank)
- **Returns:** this SelectClause instance for method chaining
- **Examples:**
  - ```java
    select.append("first_name", "fname").append("last_name", "lname");
    // Generates: SELECT first_name AS fname, last_name AS lname
    ```
- **Signature:** `public SelectClause append(final Collection<String> columns)`
- **Summary:** Appends multiple columns to the SELECT clause.
- **Contract:**
  - If the collection is empty, this method does nothing.
  - A non-empty collection is snapshotted before validation and rendering, so both phases observe the same elements even if the supplied collection is live or mutable.
- **Parameters:**
  - `columns` (`Collection<String>`) — collection of column names to select (may be null or empty; individual elements must not be null, empty, or blank)
- **Returns:** this SelectClause instance for method chaining
- **Examples:**
  - ```java
    select.append(Arrays.asList("id", "name", "email"));
    // Generates: SELECT id, name, email
    ```
- **Signature:** `public SelectClause append(final Map<String, String> columnAliases)`
- **Summary:** Appends multiple columns with their aliases to the SELECT clause.
- **Contract:**
  - If the map is empty, this method does nothing.
  - Columns are emitted in the map's iteration order, so use a java.util.LinkedHashMap if a stable column order matters.
- **Parameters:**
  - `columnAliases` (`Map<String, String>`) — map where keys are column names and values are aliases (may be null or empty; individual keys and values must not be null, empty, or blank)
- **Returns:** this SelectClause instance for method chaining
- **Examples:**
  - ```java
    Map<String, String> cols = new LinkedHashMap<>();
    cols.put("first_name", "fname");
    cols.put("last_name", "lname");
    select.append(cols);
    // Generates: SELECT first_name AS fname, last_name AS lname
    ```
##### appendIf(...) -> SelectClause
- **Signature:** `public SelectClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the SELECT clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be null, empty, or blank when b is true)
- **Returns:** this SelectClause instance for method chaining
- **Examples:**
  - ```java
    select.appendIf(includeSalary, "salary")
          .appendIf(includeBonus, "bonus");
    ```
##### appendIfOrElse(...) -> SelectClause
- **Signature:** `public SelectClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the SELECT clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be null, empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be null, empty, or blank)
- **Returns:** this SelectClause instance for method chaining
- **Examples:**
  - ```java
    select.appendIfOrElse(showFullName,
                         "first_name || ' ' || last_name AS full_name",
                         "first_name");
    ```

### Class FromClause (com.landawn.abacus.query.DynamicQuery.FromClause)
Builder class for constructing the FROM clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> FromClause
- **Signature:** `public FromClause append(final String table)`
- **Summary:** Appends a table to the FROM clause.
- **Parameters:**
  - `table` (`String`) — the table name to add (must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users").append("departments");
    // Generates: FROM users, departments
    ```
- **Signature:** `public FromClause append(final String table, final String alias)`
- **Summary:** Appends a table with an alias to the FROM clause.
- **Parameters:**
  - `table` (`String`) — the table name to add (must not be null, empty, or blank)
  - `alias` (`String`) — the alias for the table (must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users", "u").append("orders", "o");
    // Generates: FROM users u, orders o
    ```
- **Signature:** `public FromClause append(final Collection<String> tables)`
- **Summary:** Appends multiple tables to the FROM clause.
- **Contract:**
  - If the collection is empty, this method does nothing.
- **Parameters:**
  - `tables` (`Collection<String>`) — collection of table names to add (may be null or empty; individual elements must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append(Arrays.asList("users", "departments"));
    // Generates: FROM users, departments
    ```
##### join(...) -> FromClause
- **Signature:** `public FromClause join(final String joinExpr, final String expr)`
- **Summary:** Adds a JOIN clause (implicit INNER JOIN) with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
  - `expr` (`String`) — the join condition (must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users u").join("orders o", "u.id = o.user_id");
    // Generates: FROM users u JOIN orders o ON u.id = o.user_id
    ```
- **Signature:** `public FromClause join(final String joinExpr)`
- **Summary:** Adds a JOIN clause (implicit INNER JOIN) with the specified table and no ON condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users u").join("orders o USING (user_id)");
    // Generates: FROM users u JOIN orders o USING (user_id)
    ```
##### innerJoin(...) -> FromClause
- **Signature:** `public FromClause innerJoin(final String joinExpr, final String expr)`
- **Summary:** Adds an INNER JOIN clause with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
  - `expr` (`String`) — the join condition (must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users u").innerJoin("orders o", "u.id = o.user_id");
    // Generates: FROM users u INNER JOIN orders o ON u.id = o.user_id
    ```
- **Signature:** `public FromClause innerJoin(final String joinExpr)`
- **Summary:** Adds an INNER JOIN clause with the specified table and no ON condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users u").innerJoin("orders o USING (user_id)");
    // Generates: FROM users u INNER JOIN orders o USING (user_id)
    ```
##### leftJoin(...) -> FromClause
- **Signature:** `public FromClause leftJoin(final String joinExpr, final String expr)`
- **Summary:** Adds a LEFT JOIN clause with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
  - `expr` (`String`) — the join condition (must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users u").leftJoin("orders o", "u.id = o.user_id");
    // Generates: FROM users u LEFT JOIN orders o ON u.id = o.user_id
    ```
- **Signature:** `public FromClause leftJoin(final String joinExpr)`
- **Summary:** Adds a LEFT JOIN clause with the specified table and no ON condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users u").leftJoin("orders o USING (user_id)");
    // Generates: FROM users u LEFT JOIN orders o USING (user_id)
    ```
##### rightJoin(...) -> FromClause
- **Signature:** `public FromClause rightJoin(final String joinExpr, final String expr)`
- **Summary:** Adds a RIGHT JOIN clause with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
  - `expr` (`String`) — the join condition (must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("orders o").rightJoin("users u", "o.user_id = u.id");
    // Generates: FROM orders o RIGHT JOIN users u ON o.user_id = u.id
    ```
- **Signature:** `public FromClause rightJoin(final String joinExpr)`
- **Summary:** Adds a RIGHT JOIN clause with the specified table and no ON condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("orders o").rightJoin("users u USING (user_id)");
    // Generates: FROM orders o RIGHT JOIN users u USING (user_id)
    ```
##### fullJoin(...) -> FromClause
- **Signature:** `public FromClause fullJoin(final String joinExpr, final String expr)`
- **Summary:** Adds a FULL JOIN clause with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
  - `expr` (`String`) — the join condition (must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("employees e").fullJoin("departments d", "e.dept_id = d.id");
    // Generates: FROM employees e FULL JOIN departments d ON e.dept_id = d.id
    ```
- **Signature:** `public FromClause fullJoin(final String joinExpr)`
- **Summary:** Adds a FULL JOIN clause with the specified table and no ON condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("employees e").fullJoin("departments d USING (dept_id)");
    // Generates: FROM employees e FULL JOIN departments d USING (dept_id)
    ```
##### crossJoin(...) -> FromClause
- **Signature:** `public FromClause crossJoin(final String joinExpr)`
- **Summary:** Adds a CROSS JOIN clause with the specified table.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users u").crossJoin("colors c");
    // Generates: FROM users u CROSS JOIN colors c
    ```
##### naturalJoin(...) -> FromClause
- **Signature:** `public FromClause naturalJoin(final String joinExpr)`
- **Summary:** Adds a NATURAL JOIN clause with the specified table.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.append("users u").naturalJoin("user_profiles");
    // Generates: FROM users u NATURAL JOIN user_profiles
    ```
##### appendIf(...) -> FromClause
- **Signature:** `public FromClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the FROM clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be null, empty, or blank when b is true)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.appendIf(includeArchive, "archived_users");
    ```
##### appendIfOrElse(...) -> FromClause
- **Signature:** `public FromClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the FROM clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be null, empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be null, empty, or blank)
- **Returns:** this FromClause instance for method chaining
- **Examples:**
  - ```java
    from.appendIfOrElse(useArchive, "archived_users", "active_users");
    ```

### Class WhereClause (com.landawn.abacus.query.DynamicQuery.WhereClause)
Builder class for constructing the WHERE clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> WhereClause
- **Signature:** `public WhereClause append(final String expr)`
- **Summary:** Appends a condition to the WHERE clause.
- **Contract:**
  - Unlike #and(String)/#or(String), this method does not insert a logical connective \\u2014 the caller must include any required AND/OR in the argument.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to append (must not be null, empty, or blank)
- **Returns:** this WhereClause instance for method chaining
- **Examples:**
  - ```java
    where.append("active = true").append("AND deleted = false");
    // Generates: WHERE active = true AND deleted = false
    ```
##### appendPlaceholders(...) -> WhereClause
- **Signature:** `public WhereClause appendPlaceholders(final int placeholderCount)`
- **Summary:** Appends question mark placeholders for parameterized queries.
- **Contract:**
  - Use the #appendPlaceholders(int, String, String) overload when you also need a prefix/postfix (e.g.
  - Usage Examples: If placeholderCount is 0, nothing is appended.
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to append (must not be negative)
- **Returns:** this WhereClause instance for method chaining
- **Examples:**
  - ```java
    where.append("id IN (").appendPlaceholders(3).append(")");
    // sb contents so far: "WHERE id IN (?, ?, ? )"
    // Note: appendPlaceholders(int) appends the markers with no leading space, while append(String)
    // inserts a single space before each fragment, so a manual close paren is preceded by a space.
    // Prefer appendPlaceholders(int, String, String) when you want the parentheses tightly attached.
    ```
- **Signature:** `public WhereClause appendPlaceholders(final int placeholderCount, final String prefix, final String postfix)`
- **Summary:** Appends question mark placeholders surrounded by prefix and postfix.
- **Contract:**
  - Usage Examples: If placeholderCount is 0, neither prefix nor postfix is appended.
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to append (must not be negative)
  - `prefix` (`String`) — the string to add before the question marks (must not be null)
  - `postfix` (`String`) — the string to add after the question marks (must not be null)
- **Returns:** this WhereClause instance for method chaining
- **Examples:**
  - ```java
    where.append("status IN ").appendPlaceholders(3, "(", ")");
    // Generates: status IN (?, ?, ?)
    ```
##### and(...) -> WhereClause
- **Signature:** `public WhereClause and(final String expr)`
- **Summary:** Adds an AND condition to the WHERE clause.
- **Contract:**
  - If called before any #append(String), this acts as the first condition and emits WHERE cond with no leading AND.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to add with AND (must not be null, empty, or blank)
- **Returns:** this WhereClause instance for method chaining
- **Examples:**
  - ```java
    where.append("active = true").and("age >= 18").and("country = ?");
    // Generates: WHERE active = true AND age >= 18 AND country = ?
    ```
##### or(...) -> WhereClause
- **Signature:** `public WhereClause or(final String expr)`
- **Summary:** Adds an OR condition to the WHERE clause.
- **Contract:**
  - If called before any #append(String), this acts as the first condition and emits WHERE cond with no leading OR.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to add with OR (must not be null, empty, or blank)
- **Returns:** this WhereClause instance for method chaining
- **Examples:**
  - ```java
    where.append("role = 'admin'").or("role = 'moderator'");
    // Generates: WHERE role = 'admin' OR role = 'moderator'
    ```
##### appendIf(...) -> WhereClause
- **Signature:** `public WhereClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the WHERE clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be null, empty, or blank when b is true)
- **Returns:** this WhereClause instance for method chaining
- **Examples:**
  - ```java
    where.append("active = true")
         .appendIf(filterByDate, "AND created_date > ?");
    ```
##### appendIfOrElse(...) -> WhereClause
- **Signature:** `public WhereClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the WHERE clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be null, empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be null, empty, or blank)
- **Returns:** this WhereClause instance for method chaining
- **Examples:**
  - ```java
    where.appendIfOrElse(includeDeleted,
                         "status IN ('active', 'deleted')",
                         "status = 'active'");
    ```

### Class GroupByClause (com.landawn.abacus.query.DynamicQuery.GroupByClause)
Builder class for constructing the GROUP BY clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> GroupByClause
- **Signature:** `public GroupByClause append(final String propOrColumnName)`
- **Summary:** Appends a column to the GROUP BY clause.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by (must not be null, empty, or blank)
- **Returns:** this GroupByClause instance for method chaining
- **Examples:**
  - ```java
    groupBy.append("category").append("subcategory");
    // Generates: GROUP BY category, subcategory
    ```
- **Signature:** `public GroupByClause append(final Collection<String> columns)`
- **Summary:** Appends multiple columns to the GROUP BY clause.
- **Contract:**
  - If the collection is empty, this method does nothing.
- **Parameters:**
  - `columns` (`Collection<String>`) — collection of column names to group by (may be null or empty; individual elements must not be null, empty, or blank)
- **Returns:** this GroupByClause instance for method chaining
- **Examples:**
  - ```java
    groupBy.append(Arrays.asList("year", "quarter", "region"));
    // Generates: GROUP BY year, quarter, region
    ```
##### appendIf(...) -> GroupByClause
- **Signature:** `public GroupByClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the GROUP BY clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be null, empty, or blank when b is true)
- **Returns:** this GroupByClause instance for method chaining
- **Examples:**
  - ```java
    groupBy.append("product_id")
           .appendIf(groupByRegion, "region_id");
    ```
##### appendIfOrElse(...) -> GroupByClause
- **Signature:** `public GroupByClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the GROUP BY clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be null, empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be null, empty, or blank)
- **Returns:** this GroupByClause instance for method chaining
- **Examples:**
  - ```java
    groupBy.appendIfOrElse(detailedReport,
                           "year, month, day",
                           "year");
    ```

### Class HavingClause (com.landawn.abacus.query.DynamicQuery.HavingClause)
Builder class for constructing the HAVING clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> HavingClause
- **Signature:** `public HavingClause append(final String expr)`
- **Summary:** Appends a condition to the HAVING clause.
- **Contract:**
  - Unlike #and(String)/#or(String), this method does not insert a logical connective \\u2014 the caller must include any required AND/OR in the argument.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to append (must not be null, empty, or blank)
- **Returns:** this HavingClause instance for method chaining
- **Examples:**
  - ```java
    having.append("SUM(amount) > 1000");
    // Generates: HAVING SUM(amount) > 1000
    ```
##### appendPlaceholders(...) -> HavingClause
- **Signature:** `public HavingClause appendPlaceholders(final int placeholderCount)`
- **Summary:** Appends question mark placeholders for parameterized queries.
- **Contract:**
  - Use the #appendPlaceholders(int, String, String) overload when you also need a prefix/postfix (e.g.
  - Usage Examples: If placeholderCount is 0, nothing is appended.
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to append (must not be negative)
- **Returns:** this HavingClause instance for method chaining
- **Examples:**
  - ```java
    having.append("MAX(score) IN (").appendPlaceholders(3).append(")");
    // sb contents so far: "HAVING MAX(score) IN (?, ?, ? )"
    // Note: appendPlaceholders(int) appends the markers with no leading space, while append(String)
    // inserts a single space before each fragment, so a manual close paren is preceded by a space.
    // Prefer appendPlaceholders(int, String, String) when you want the parentheses tightly attached.
    ```
- **Signature:** `public HavingClause appendPlaceholders(final int placeholderCount, final String prefix, final String postfix)`
- **Summary:** Appends question mark placeholders surrounded by prefix and postfix.
- **Contract:**
  - Usage Examples: If placeholderCount is 0, neither prefix nor postfix is appended.
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to append (must not be negative)
  - `prefix` (`String`) — the string to add before the question marks (must not be null)
  - `postfix` (`String`) — the string to add after the question marks (must not be null)
- **Returns:** this HavingClause instance for method chaining
- **Examples:**
  - ```java
    having.append("MAX(score) IN ").appendPlaceholders(3, "(", ")");
    // Generates: MAX(score) IN (?, ?, ?)
    ```
##### and(...) -> HavingClause
- **Signature:** `public HavingClause and(final String expr)`
- **Summary:** Adds an AND condition to the HAVING clause.
- **Contract:**
  - If called before any #append(String), this acts as the first condition and emits HAVING cond with no leading AND.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to add with AND (must not be null, empty, or blank)
- **Returns:** this HavingClause instance for method chaining
- **Examples:**
  - ```java
    having.append("COUNT(*) > 5").and("MAX(price) < 1000");
    // Generates: HAVING COUNT(*) > 5 AND MAX(price) < 1000
    ```
##### or(...) -> HavingClause
- **Signature:** `public HavingClause or(final String expr)`
- **Summary:** Adds an OR condition to the HAVING clause.
- **Contract:**
  - If called before any #append(String), this acts as the first condition and emits HAVING cond with no leading OR.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to add with OR (must not be null, empty, or blank)
- **Returns:** this HavingClause instance for method chaining
- **Examples:**
  - ```java
    having.append("MIN(score) > 80").or("AVG(score) > 90");
    // Generates: HAVING MIN(score) > 80 OR AVG(score) > 90
    ```
##### appendIf(...) -> HavingClause
- **Signature:** `public HavingClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the HAVING clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be null, empty, or blank when b is true)
- **Returns:** this HavingClause instance for method chaining
- **Examples:**
  - ```java
    having.append("COUNT(*) > 0")
          .appendIf(checkRevenue, "AND SUM(revenue) > ?");
    ```
##### appendIfOrElse(...) -> HavingClause
- **Signature:** `public HavingClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the HAVING clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be null, empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be null, empty, or blank)
- **Returns:** this HavingClause instance for method chaining
- **Examples:**
  - ```java
    having.appendIfOrElse(strictFilter,
                          "COUNT(*) > 100",
                          "COUNT(*) > 10");
    ```

### Class OrderByClause (com.landawn.abacus.query.DynamicQuery.OrderByClause)
Builder class for constructing the ORDER BY clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> OrderByClause
- **Signature:** `public OrderByClause append(final String propOrColumnName)`
- **Summary:** Appends a column (with optional sort direction) to the ORDER BY clause.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name with optional ASC/DESC (must not be null, empty, or blank)
- **Returns:** this OrderByClause instance for method chaining
- **Examples:**
  - ```java
    orderBy.append("created_date DESC").append("name ASC");
    // Generates: ORDER BY created_date DESC, name ASC
    ```
- **Signature:** `public OrderByClause append(final Collection<String> columns)`
- **Summary:** Appends multiple columns to the ORDER BY clause.
- **Contract:**
  - If the collection is empty, this method does nothing.
- **Parameters:**
  - `columns` (`Collection<String>`) — collection of column names with optional sort directions (may be null or empty; individual elements must not be null, empty, or blank)
- **Returns:** this OrderByClause instance for method chaining
- **Examples:**
  - ```java
    orderBy.append(Arrays.asList("year DESC", "month DESC", "day DESC"));
    // Generates: ORDER BY year DESC, month DESC, day DESC
    ```
##### appendIf(...) -> OrderByClause
- **Signature:** `public OrderByClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the ORDER BY clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be null, empty, or blank when b is true)
- **Returns:** this OrderByClause instance for method chaining
- **Examples:**
  - ```java
    orderBy.append("priority DESC")
           .appendIf(sortByDate, "created_date DESC");
    ```
##### appendIfOrElse(...) -> OrderByClause
- **Signature:** `public OrderByClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the ORDER BY clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be null, empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be null, empty, or blank)
- **Returns:** this OrderByClause instance for method chaining
- **Examples:**
  - ```java
    orderBy.appendIfOrElse(newestFirst,
                           "created_date DESC",
                           "created_date ASC");
    ```

### Class Filters (com.landawn.abacus.query.Filters)
Factory class for creating SQL Condition objects used in query construction.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### alwaysTrue(...) -> SqlExpression
- **Signature:** `@Deprecated public static SqlExpression alwaysTrue()`
- **Summary:** Returns a condition that always evaluates to true.
- **Parameters:**
  - (none)
- **Returns:** an SqlExpression that always evaluates to true (1 &lt; 2)
- **Examples:**
  - ```java
    Condition condition = includeFilter ? Filters.equal("status", "active")
                                       : Filters.alwaysTrue();
    // Filters.alwaysTrue() renders the literal: 1 < 2
    ```
##### alwaysFalse(...) -> SqlExpression
- **Signature:** `@Deprecated public static SqlExpression alwaysFalse()`
- **Summary:** Returns a condition that always evaluates to false.
- **Parameters:**
  - (none)
- **Returns:** an SqlExpression that always evaluates to false (1 &gt; 2)
- **Examples:**
  - ```java
    Condition condition = excludeAll ? Filters.alwaysFalse()
                                     : Filters.equal("status", "active");
    // Filters.alwaysFalse() renders the literal: 1 > 2
    ```
##### namedProperty(...) -> NamedProperty
- **Signature:** `@Beta public static NamedProperty namedProperty(final String propName)`
- **Summary:** Creates (or returns a cached) NamedProperty instance representing a property/column name.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column (must not be null, empty, or blank)
- **Returns:** a NamedProperty instance
- **Examples:**
  - ```java
    NamedProperty prop = Filters.namedProperty("user_name");
    // Renders as: user_name
    ```
##### expr(...) -> SqlExpression
- **Signature:** `public static SqlExpression expr(final String literal)`
- **Summary:** Creates an SqlExpression from a string literal.
- **Parameters:**
  - `literal` (`String`) — the SQL expression as a string (must not be null)
- **Returns:** an SqlExpression instance
- **Examples:**
  - ```java
    SqlExpression expr = Filters.expr("UPPER(name) = 'JOHN'");
    // SQL fragment: UPPER(name) = 'JOHN'
    ```
##### not(...) -> Not
- **Signature:** `public static Not not(final Condition condition)`
- **Summary:** Creates a negation condition that represents the logical NOT of the provided condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition to negate (must not be null and must be a composable condition)
- **Returns:** a Not condition that wraps and negates the provided condition
- **Examples:**
  - ```java
    // Create a NOT LIKE condition
    Like likeCondition = Filters.like("name", "%test%");
    Not notLike = Filters.not(likeCondition);
    
    // Create a NOT IN condition
    Not notIn = Filters.not(Filters.in("status", Arrays.asList("inactive", "deleted")));
    
    // Create a NOT BETWEEN condition
    Not notBetween = Filters.not(Filters.between("age", 18, 65));
    
    // Create a complex negated condition
    Not complexNot = Filters.not(Filters.and(
        Filters.equal("status", "active"),
        Filters.greaterThan("age", 18),
        Filters.like("email", "%@company.com")
    ));
    ```
- **See also:** Not, Condition
##### binary(...) -> Binary
- **Signature:** `public static Binary binary(final String propName, final Operator operator, final Object propValue)`
- **Summary:** Creates a Binary condition with the specified property name, operator, and value.
- **Contract:**
  - This is a general factory for creating conditions with a binary comparison/membership Operator, useful when one of the convenience factories (e.g.
  - The operator must be a valid binary comparison or membership operator: one of Operator#EQUAL, Operator#NOT_EQUAL, Operator#NOT_EQUAL_ANSI, Operator#GREATER_THAN, Operator#GREATER_THAN_OR_EQUAL, Operator#LESS_THAN, Operator#LESS_THAN_OR_EQUAL, Operator#LIKE, Operator#NOT_LIKE, Operator#IS, Operator#IS_NOT, Operator#IN, or Operator#NOT_IN.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `operator` (`Operator`) — the binary comparison or membership operator to use (must not be null; structural operators are rejected)
  - `propValue` (`Object`) — the value to compare against; may be a literal, null, or another Condition such as a SubQuery. For an IN/NOT_IN operator, a Collection or array value is copied defensively and must be non-empty.
- **Returns:** a Binary condition
- **Examples:**
  - ```java
    Binary condition = Filters.binary("price", Operator.GREATER_THAN, 100);
    // SQL fragment: price > 100
    ```
- **Signature:** `public static Binary binary(final String propName, final Operator operator)`
- **Summary:** Creates a parameterized Binary condition for use with prepared statements.
- **Contract:**
  - The condition uses a question mark (?) placeholder in place of the value, which is provided later when the statement is executed.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `operator` (`Operator`) — the binary comparison operator to use (must not be null; membership and structural operators are rejected)
- **Returns:** a Binary condition with a ? placeholder value
- **Examples:**
  - ```java
    Binary condition = Filters.binary("price", Operator.GREATER_THAN);
    // SQL fragment: price > ?
    ```
- **See also:** #binary(String, Operator, Object)
##### equal(...) -> Equal
- **Signature:** `public static Equal equal(final String propName, final Object propValue)`
- **Summary:** Creates an equality condition (=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the value to compare for equality; may be a literal, null (renders as IS NULL), or another Condition such as a SubQuery
- **Returns:** an Equal condition
- **Examples:**
  - ```java
    Equal condition = Filters.equal("username", "john_doe");
    // SQL fragment: username = 'john_doe'
    ```
- **Signature:** `public static Equal equal(final String propName)`
- **Summary:** Creates a parameterized equality condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an Equal condition with a parameter placeholder
- **Examples:**
  - ```java
    Equal condition = Filters.equal("user_id");
    // SQL fragment: user_id = ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### eq(...) -> Equal
- **Signature:** `public static Equal eq(final String propName, final Object propValue)`
- **Summary:** Creates an equality condition (=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare for equality
- **Returns:** an Equal condition
- **Examples:**
  - ```java
    Equal condition = Filters.eq("status", "active");
    // SQL fragment: status = 'active'
    ```
- **Signature:** `public static Equal eq(final String propName)`
- **Summary:** Creates a parameterized equality condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an Equal condition with a parameter placeholder
- **Examples:**
  - ```java
    Equal condition = Filters.eq("email");
    // SQL fragment: email = ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### anyEqual(...) -> Or
- **Signature:** `public static Or anyEqual(final Map<String, ?> props)`
- **Summary:** Creates an OR condition from a map where each entry represents a property-value equality check across <b>different</b> columns/properties.
- **Parameters:**
  - `props` (`Map<String, ?>`) — map of property names to values (must not be empty). Entries are consumed once during this call; subsequent mutations do not affect the returned condition
- **Returns:** an Or condition
- **Examples:**
  - ```java
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("name", "John");
    props.put("email", "john@example.com");
    Or condition = Filters.anyEqual(props);
    // SQL fragment: ((name = 'John') OR (email = 'john@example.com'))
    ```
- **See also:** NamedProperty#equalsAny(Object...)
- **Signature:** `public static Or anyEqual(final Object entity)`
- **Summary:** Creates an OR condition from an entity object using all its properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object whose properties will be used
- **Returns:** an Or condition
- **Examples:**
  - ```java
    User user = new User("John", "john@example.com");
    Or condition = Filters.anyEqual(user);
    // SQL fragment: ((name = 'John') OR (email = 'john@example.com'))
    ```
- **Signature:** `public static Or anyEqual(final Object entity, final Collection<String> includedPropNames)`
- **Summary:** Creates an OR condition from an entity object using only the specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object
  - `includedPropNames` (`Collection<String>`) — the property names to include (must not be empty). Names are consumed once during this call; subsequent mutations do not affect the returned condition
- **Returns:** an Or condition
- **Examples:**
  - ```java
    User user = new User("John", "john@example.com", 25);
    Or condition = Filters.anyEqual(user, Arrays.asList("name", "email"));
    // Only uses name and email, ignores age
    ```
- **Signature:** `public static Or anyEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2)`
- **Summary:** Creates an OR condition with two property-value pairs across <b>different</b> columns/properties.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
- **Returns:** an Or condition
- **Examples:**
  - ```java
    Or condition = Filters.anyEqual("name", "John", "email", "john@example.com");
    // SQL fragment: ((name = 'John') OR (email = 'john@example.com'))
    ```
- **Signature:** `public static Or anyEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3, final Object propValue3)`
- **Summary:** Creates an OR condition with three property-value pairs across <b>different</b> columns/properties.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
  - `propName3` (`String`) — third property name
  - `propValue3` (`Object`) — third property value
- **Returns:** an Or condition
- **Examples:**
  - ```java
    Or condition = Filters.anyEqual("status", "active", "type", "premium", "verified", true);
    // SQL fragment: ((status = 'active') OR (type = 'premium') OR (verified = true))
    ```
##### allEqual(...) -> And
- **Signature:** `public static And allEqual(final Map<String, ?> props)`
- **Summary:** Creates an AND condition from a map where each entry represents a property-value equality check across <b>different</b> columns/properties.
- **Parameters:**
  - `props` (`Map<String, ?>`) — map of property names to values (must not be empty). Entries are consumed once during this call; subsequent mutations do not affect the returned condition
- **Returns:** an And condition
- **Examples:**
  - ```java
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("status", "active");
    props.put("type", "premium");
    And condition = Filters.allEqual(props);
    // SQL fragment: ((status = 'active') AND (type = 'premium'))
    ```
- **Signature:** `public static And allEqual(final Object entity)`
- **Summary:** Creates an AND condition from an entity object using all its properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object whose properties will be used
- **Returns:** an And condition
- **Examples:**
  - ```java
    User user = new User("John", "john@example.com", 25);
    And condition = Filters.allEqual(user);
    // SQL fragment: ((name = 'John') AND (email = 'john@example.com') AND (age = 25))
    ```
- **Signature:** `public static And allEqual(final Object entity, final Collection<String> includedPropNames)`
- **Summary:** Creates an AND condition from an entity object using only the specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object
  - `includedPropNames` (`Collection<String>`) — the property names to include (must not be empty). Names are consumed once during this call; subsequent mutations do not affect the returned condition
- **Returns:** an And condition
- **Examples:**
  - ```java
    User user = new User("John", "john@example.com", 25);
    And condition = Filters.allEqual(user, Arrays.asList("email", "age"));
    // Only uses email and age, ignores name
    ```
- **Signature:** `public static And allEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2)`
- **Summary:** Creates an AND condition with two property-value pairs across <b>different</b> columns/properties.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
- **Returns:** an And condition
- **Examples:**
  - ```java
    And condition = Filters.allEqual("status", "active", "type", "premium");
    // SQL fragment: ((status = 'active') AND (type = 'premium'))
    ```
- **Signature:** `public static And allEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3, final Object propValue3)`
- **Summary:** Creates an AND condition with three property-value pairs across <b>different</b> columns/properties.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
  - `propName3` (`String`) — third property name
  - `propValue3` (`Object`) — third property value
- **Returns:** an And condition
- **Examples:**
  - ```java
    And condition = Filters.allEqual("status", "active", "type", "premium", "verified", true);
    // SQL fragment: ((status = 'active') AND (type = 'premium') AND (verified = true))
    ```
##### anyOfAllEqual(...) -> Or
- **Signature:** `@Beta public static Or anyOfAllEqual(final Collection<?> entitiesOrPropMaps)`
- **Summary:** Creates an OR of per-row AND-of-equals conditions &mdash; i.e.
- **Contract:**
  - a "match any row" filter, where a row matches when all of its columns equal the given values.
  - If the first non-null element is a Map, all non-null elements must be maps with non-null String keys.
  - Otherwise, every non-null element must be an entity rather than a map, and all properties selected from the first entity's class are used.
- **Parameters:**
  - `entitiesOrPropMaps` (`Collection<?>`) — collection of property maps or entity objects (must not be empty)
- **Returns:** an Or condition
- **Examples:**
  - ```java
    Set<Map<String, Object>> propsSet = new LinkedHashSet<>();
    propsSet.add(Map.of("status", "active", "type", "premium"));
    propsSet.add(Map.of("status", "trial", "verified", true));
    Or mapCondition = Filters.anyOfAllEqual(propsSet);
    // Results in: (status = 'active' AND type = 'premium') OR (status = 'trial' AND verified = true)
    
    List<User> users = Arrays.asList(
        new User("John", "john@example.com"),
        new User("Jane", "jane@example.com")
    );
    Or entityCondition = Filters.anyOfAllEqual(users);
    // Results in: (name = 'John' AND email = 'john@example.com') OR (name = 'Jane' AND email = 'jane@example.com')
    ```
- **See also:** #anyOfAllEqual(Collection, Collection), #anyEqual(Map), #allEqual(Map)
- **Signature:** `@Beta public static Or anyOfAllEqual(final Collection<?> entities, final Collection<String> includedPropNames)`
- **Summary:** Creates an OR of per-row AND-of-equals conditions &mdash; a "match any row" filter &mdash; using only the specified properties of each entity.
- **Parameters:**
  - `entities` (`Collection<?>`) — collection of entity objects (must not be empty)
  - `includedPropNames` (`Collection<String>`) — the property names to include (must not be empty). Both input collections are snapshotted during the call
- **Returns:** an Or condition
- **Examples:**
  - ```java
    List<User> users = Arrays.asList(new User("John", "active"), new User("Jane", "trial"));
    Or condition = Filters.anyOfAllEqual(users, Arrays.asList("name", "status"));
    // Only uses name and status properties from each user
    // Results in: (name = 'John' AND status = 'active') OR (name = 'Jane' AND status = 'trial')
    ```
- **See also:** #anyOfAllEqual(Collection), #allEqual(Object, Collection)
##### gtAndLt(...) -> And
- **Signature:** `public static And gtAndLt(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN-like condition using greater-than (gt) and less-than (lt) comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (exclusive)
  - `maxValue` (`Object`) — the maximum value (exclusive)
- **Returns:** an And condition
- **Examples:**
  - ```java
    And condition = Filters.gtAndLt("age", 18, 65);
    // SQL fragment: ((age > 18) AND (age < 65))
    ```
- **Signature:** `public static And gtAndLt(final String propName)`
- **Summary:** Creates a parameterized BETWEEN-like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition with parameter placeholders
- **Examples:**
  - ```java
    And condition = Filters.gtAndLt("price");
    // SQL fragment: ((price > ?) AND (price < ?))
    ```
##### geAndLt(...) -> And
- **Signature:** `public static And geAndLt(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN-like condition using greater-than-or-equal (ge) and less-than (lt) comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (exclusive)
- **Returns:** an And condition
- **Examples:**
  - ```java
    And condition = Filters.geAndLt("price", 100, 500);
    // SQL fragment: ((price >= 100) AND (price < 500))
    ```
- **Signature:** `public static And geAndLt(final String propName)`
- **Summary:** Creates a parameterized BETWEEN-like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition with parameter placeholders
- **Examples:**
  - ```java
    And condition = Filters.geAndLt("score");
    // SQL fragment: ((score >= ?) AND (score < ?))
    ```
##### geAndLe(...) -> And
- **Signature:** `public static And geAndLe(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN-like condition using greater-than-or-equal (ge) and less-than-or-equal (le) comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** an And condition
- **Examples:**
  - ```java
    And condition = Filters.geAndLe("date", "2023-01-01", "2023-12-31");
    // SQL fragment: ((date >= '2023-01-01') AND (date <= '2023-12-31'))
    ```
- **Signature:** `public static And geAndLe(final String propName)`
- **Summary:** Creates a parameterized BETWEEN-like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition with parameter placeholders
- **Examples:**
  - ```java
    And condition = Filters.geAndLe("amount");
    // SQL fragment: ((amount >= ?) AND (amount <= ?))
    ```
##### gtAndLe(...) -> And
- **Signature:** `public static And gtAndLe(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN-like condition using greater-than (gt) and less-than-or-equal (le) comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (exclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** an And condition
- **Examples:**
  - ```java
    And condition = Filters.gtAndLe("score", 0, 100);
    // SQL fragment: ((score > 0) AND (score <= 100))
    ```
- **Signature:** `public static And gtAndLe(final String propName)`
- **Summary:** Creates a parameterized BETWEEN-like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition with parameter placeholders
- **Examples:**
  - ```java
    And condition = Filters.gtAndLe("temperature");
    // SQL fragment: ((temperature > ?) AND (temperature <= ?))
    ```
##### idToCond(...) -> And
- **Signature:** `public static And idToCond(final EntityId entityId)`
- **Summary:** Converts an EntityId to an And condition where each key-value pair becomes an equality check.
- **Parameters:**
  - `entityId` (`EntityId`) — the EntityId containing key-value pairs (must not be null). Entries are consumed once as key/value pairs during this call
- **Returns:** an And condition
- **Examples:**
  - ```java
    EntityId id = EntityId.of("companyId", 1, "userId", 100);
    And condition = Filters.idToCond(id);
    // SQL fragment: ((companyId = 1) AND (userId = 100))
    // (EntityId orders its keys alphabetically, regardless of insertion order)
    ```
- **Signature:** `public static Or idToCond(final Collection<? extends EntityId> entityIds)`
- **Summary:** Converts a collection of EntityIds to an Or condition where each EntityId becomes an And condition.
- **Parameters:**
  - `entityIds` (`Collection<? extends EntityId>`) — collection of EntityIds (must not be null, empty, or contain null)
- **Returns:** an Or condition
- **Examples:**
  - ```java
    List<EntityId> ids = Arrays.asList(
        EntityId.of("companyId", 1, "userId", 100),
        EntityId.of("companyId", 2, "userId", 200)
    );
    Or condition = Filters.idToCond(ids);
    // Results in: ((((companyId = 1) AND (userId = 100))) OR (((companyId = 2) AND (userId = 200))))
    ```
##### id2Cond(...) -> And
- **Signature:** `@Deprecated public static And id2Cond(final EntityId entityId)`
- **Summary:** Converts an EntityId to an And condition where each key-value pair becomes an equality check.
- **Parameters:**
  - `entityId` (`EntityId`) — the EntityId containing key-value pairs (must not be null)
- **Returns:** an And condition
- **Signature:** `@Deprecated public static Or id2Cond(final Collection<? extends EntityId> entityIds)`
- **Summary:** Converts a collection of EntityIds to an Or condition where each EntityId becomes an And condition.
- **Parameters:**
  - `entityIds` (`Collection<? extends EntityId>`) — collection of EntityIds (must not be null or empty)
- **Returns:** an Or condition
##### notEqual(...) -> NotEqual
- **Signature:** `public static NotEqual notEqual(final String propName, final Object propValue)`
- **Summary:** Creates a not-equal condition (!=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the value to compare for inequality; may be a literal, null (renders as IS NOT NULL), or another Condition such as a SubQuery
- **Returns:** a NotEqual condition
- **Examples:**
  - ```java
    NotEqual condition = Filters.notEqual("status", "deleted");
    // SQL fragment: status != 'deleted'
    ```
- **Signature:** `public static NotEqual notEqual(final String propName)`
- **Summary:** Creates a parameterized not-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a NotEqual condition with a parameter placeholder
- **Examples:**
  - ```java
    NotEqual condition = Filters.notEqual("user_type");
    // SQL fragment: user_type != ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### ne(...) -> NotEqual
- **Signature:** `public static NotEqual ne(final String propName, final Object propValue)`
- **Summary:** Creates a not-equal condition (!=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare for inequality
- **Returns:** a NotEqual condition
- **Examples:**
  - ```java
    NotEqual condition = Filters.ne("status", "inactive");
    // SQL fragment: status != 'inactive'
    ```
- **Signature:** `public static NotEqual ne(final String propName)`
- **Summary:** Creates a parameterized not-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a NotEqual condition with a parameter placeholder
- **Examples:**
  - ```java
    NotEqual condition = Filters.ne("category");
    // SQL fragment: category != ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### greaterThan(...) -> GreaterThan
- **Signature:** `public static GreaterThan greaterThan(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than condition (>) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a GreaterThan condition
- **Examples:**
  - ```java
    GreaterThan condition = Filters.greaterThan("age", 18);
    // SQL fragment: age > 18
    ```
- **Signature:** `public static GreaterThan greaterThan(final String propName)`
- **Summary:** Creates a parameterized greater-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a GreaterThan condition with a parameter placeholder
- **Examples:**
  - ```java
    GreaterThan condition = Filters.greaterThan("salary");
    // SQL fragment: salary > ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### gt(...) -> GreaterThan
- **Signature:** `public static GreaterThan gt(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than condition (>) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a GreaterThan condition
- **Examples:**
  - ```java
    GreaterThan condition = Filters.gt("price", 100);
    // SQL fragment: price > 100
    ```
- **Signature:** `public static GreaterThan gt(final String propName)`
- **Summary:** Creates a parameterized greater-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a GreaterThan condition with a parameter placeholder
- **Examples:**
  - ```java
    GreaterThan condition = Filters.gt("quantity");
    // SQL fragment: quantity > ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### greaterThanOrEqual(...) -> GreaterThanOrEqual
- **Signature:** `public static GreaterThanOrEqual greaterThanOrEqual(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than-or-equal condition (>=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a GreaterThanOrEqual condition
- **Examples:**
  - ```java
    GreaterThanOrEqual condition = Filters.greaterThanOrEqual("score", 60);
    // SQL fragment: score >= 60
    ```
- **Signature:** `public static GreaterThanOrEqual greaterThanOrEqual(final String propName)`
- **Summary:** Creates a parameterized greater-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a GreaterThanOrEqual condition with a parameter placeholder
- **Examples:**
  - ```java
    GreaterThanOrEqual condition = Filters.greaterThanOrEqual("min_age");
    // SQL fragment: min_age >= ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### ge(...) -> GreaterThanOrEqual
- **Signature:** `public static GreaterThanOrEqual ge(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than-or-equal condition (>=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a GreaterThanOrEqual condition
- **Examples:**
  - ```java
    GreaterThanOrEqual condition = Filters.ge("level", 5);
    // SQL fragment: level >= 5
    ```
- **Signature:** `public static GreaterThanOrEqual ge(final String propName)`
- **Summary:** Creates a parameterized greater-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a GreaterThanOrEqual condition with a parameter placeholder
- **Examples:**
  - ```java
    GreaterThanOrEqual condition = Filters.ge("rating");
    // SQL fragment: rating >= ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### lessThan(...) -> LessThan
- **Signature:** `public static LessThan lessThan(final String propName, final Object propValue)`
- **Summary:** Creates a less-than condition (<) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a LessThan condition
- **Examples:**
  - ```java
    LessThan condition = Filters.lessThan("age", 65);
    // SQL fragment: age < 65
    ```
- **Signature:** `public static LessThan lessThan(final String propName)`
- **Summary:** Creates a parameterized less-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a LessThan condition with a parameter placeholder
- **Examples:**
  - ```java
    LessThan condition = Filters.lessThan("max_price");
    // SQL fragment: max_price < ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### lt(...) -> LessThan
- **Signature:** `public static LessThan lt(final String propName, final Object propValue)`
- **Summary:** Creates a less-than condition (<) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a LessThan condition
- **Examples:**
  - ```java
    LessThan condition = Filters.lt("stock", 10);
    // SQL fragment: stock < 10
    ```
- **Signature:** `public static LessThan lt(final String propName)`
- **Summary:** Creates a parameterized less-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a LessThan condition with a parameter placeholder
- **Examples:**
  - ```java
    LessThan condition = Filters.lt("expiry_date");
    // SQL fragment: expiry_date < ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### lessThanOrEqual(...) -> LessThanOrEqual
- **Signature:** `public static LessThanOrEqual lessThanOrEqual(final String propName, final Object propValue)`
- **Summary:** Creates a less-than-or-equal condition (<=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a LessThanOrEqual condition
- **Examples:**
  - ```java
    LessThanOrEqual condition = Filters.lessThanOrEqual("discount", 50);
    // SQL fragment: discount <= 50
    ```
- **Signature:** `public static LessThanOrEqual lessThanOrEqual(final String propName)`
- **Summary:** Creates a parameterized less-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a LessThanOrEqual condition with a parameter placeholder
- **Examples:**
  - ```java
    LessThanOrEqual condition = Filters.lessThanOrEqual("max_attempts");
    // SQL fragment: max_attempts <= ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### le(...) -> LessThanOrEqual
- **Signature:** `public static LessThanOrEqual le(final String propName, final Object propValue)`
- **Summary:** Creates a less-than-or-equal condition (<=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a LessThanOrEqual condition
- **Examples:**
  - ```java
    LessThanOrEqual condition = Filters.le("priority", 3);
    // SQL fragment: priority <= 3
    ```
- **Signature:** `public static LessThanOrEqual le(final String propName)`
- **Summary:** Creates a parameterized less-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a LessThanOrEqual condition with a parameter placeholder
- **Examples:**
  - ```java
    LessThanOrEqual condition = Filters.le("weight");
    // SQL fragment: weight <= ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### between(...) -> Between
- **Signature:** `public static Between between(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a Between condition for the specified property and range values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** a Between condition
- **Examples:**
  - ```java
    Between condition = Filters.between("age", 18, 65);
    // SQL fragment: age BETWEEN 18 AND 65
    ```
- **Signature:** `public static Between between(final String propName)`
- **Summary:** Creates a parameterized Between condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a Between condition with parameter placeholders
- **Examples:**
  - ```java
    Between condition = Filters.between("price");
    // SQL fragment: price BETWEEN ? AND ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### notBetween(...) -> NotBetween
- **Signature:** `public static NotBetween notBetween(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a NotBetween condition for the specified property and range values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value of the excluded range (inclusive)
  - `maxValue` (`Object`) — the maximum value of the excluded range (inclusive)
- **Returns:** a NotBetween condition
- **Examples:**
  - ```java
    NotBetween condition = Filters.notBetween("temperature", -10, 40);
    // SQL fragment: temperature NOT BETWEEN -10 AND 40
    // True when temperature < -10 OR temperature > 40
    ```
- **Signature:** `public static NotBetween notBetween(final String propName)`
- **Summary:** Creates a parameterized NotBetween condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a NotBetween condition with parameter placeholders
- **Examples:**
  - ```java
    NotBetween condition = Filters.notBetween("score");
    // SQL fragment: score NOT BETWEEN ? AND ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### like(...) -> Like
- **Signature:** `public static Like like(final String propName, final String pattern)`
- **Summary:** Creates a Like condition for pattern matching.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `pattern` (`String`) — the pattern to match (can include SQL wildcards). Passing null renders as propName LIKE null, which is not a meaningful SQL comparison; do not pass null (the #contains(String, String) / #startsWith(String, String) siblings reject a null value)
- **Returns:** a Like condition
- **Examples:**
  - ```java
    Like condition = Filters.like("email", "%@gmail.com");
    // SQL fragment: email LIKE '%@gmail.com'
    ```
- **Signature:** `public static Like like(final String propName, final Object propValue)`
- **Summary:** Creates a Like condition with a non-string operand such as an expression or subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the operand to compare with LIKE; may be a literal or another condition
- **Returns:** a Like condition
- **Examples:**
  - ```java
    Like condition = Filters.like("email", Filters.expr("CONCAT(domain, '%')"));
    // SQL fragment: email LIKE CONCAT(domain, '%')
    ```
- **Signature:** `public static Like like(final String propName)`
- **Summary:** Creates a parameterized Like condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a Like condition with a parameter placeholder
- **Examples:**
  - ```java
    Like condition = Filters.like("name");
    // SQL fragment: name LIKE ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### notLike(...) -> NotLike
- **Signature:** `public static NotLike notLike(final String propName, final String pattern)`
- **Summary:** Creates a NotLike condition for pattern matching exclusion.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `pattern` (`String`) — the pattern to exclude (can include SQL wildcards). Passing null renders as propName NOT LIKE null, which is not a meaningful SQL comparison; do not pass null (the #notContains(String, String) sibling rejects a null value)
- **Returns:** a NotLike condition
- **Examples:**
  - ```java
    NotLike condition = Filters.notLike("filename", "%.tmp");
    // SQL fragment: filename NOT LIKE '%.tmp'
    ```
- **Signature:** `public static NotLike notLike(final String propName, final Object propValue)`
- **Summary:** Creates a NotLike condition with a non-string operand such as an expression or subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the operand to compare with NOT LIKE; may be a literal or another condition
- **Returns:** a NotLike condition
- **Examples:**
  - ```java
    NotLike condition = Filters.notLike("email", Filters.expr("CONCAT('%', blocked_domain)"));
    // SQL fragment: email NOT LIKE CONCAT('%', blocked_domain)
    ```
- **Signature:** `public static NotLike notLike(final String propName)`
- **Summary:** Creates a parameterized NotLike condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a NotLike condition with a parameter placeholder
- **Examples:**
  - ```java
    NotLike condition = Filters.notLike("description");
    // SQL fragment: description NOT LIKE ?
    ```
- **See also:** com.landawn.abacus.query.SqlBuilder
##### contains(...) -> Like
- **Signature:** `public static Like contains(final String propName, final String propValue)`
- **Summary:** Creates a Like condition that checks if the property contains the specified value.
- **Contract:**
  - Creates a Like condition that checks if the property contains the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the value to search for (must not be null)
- **Returns:** a Like condition
- **Examples:**
  - ```java
    Like condition = Filters.contains("description", "java");
    // SQL fragment: description LIKE '%java%'
    ```
##### notContains(...) -> NotLike
- **Signature:** `public static NotLike notContains(final String propName, final String propValue)`
- **Summary:** Creates a NotLike condition that checks if the property does not contain the specified value.
- **Contract:**
  - Creates a NotLike condition that checks if the property does not contain the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the value to exclude (must not be null)
- **Returns:** a NotLike condition
- **Examples:**
  - ```java
    NotLike condition = Filters.notContains("tags", "deprecated");
    // SQL fragment: tags NOT LIKE '%deprecated%'
    ```
##### startsWith(...) -> Like
- **Signature:** `public static Like startsWith(final String propName, final String propValue)`
- **Summary:** Creates a Like condition that checks if the property starts with the specified value.
- **Contract:**
  - Creates a Like condition that checks if the property starts with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the prefix to search for (must not be null)
- **Returns:** a Like condition
- **Examples:**
  - ```java
    Like condition = Filters.startsWith("name", "John");
    // SQL fragment: name LIKE 'John%'
    ```
##### notStartsWith(...) -> NotLike
- **Signature:** `public static NotLike notStartsWith(final String propName, final String propValue)`
- **Summary:** Creates a NotLike condition that checks if the property does not start with the specified value.
- **Contract:**
  - Creates a NotLike condition that checks if the property does not start with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the prefix to exclude (must not be null)
- **Returns:** a NotLike condition
- **Examples:**
  - ```java
    NotLike condition = Filters.notStartsWith("code", "TEST");
    // SQL fragment: code NOT LIKE 'TEST%'
    ```
##### endsWith(...) -> Like
- **Signature:** `public static Like endsWith(final String propName, final String propValue)`
- **Summary:** Creates a Like condition that checks if the property ends with the specified value.
- **Contract:**
  - Creates a Like condition that checks if the property ends with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the suffix to search for (must not be null)
- **Returns:** a Like condition
- **Examples:**
  - ```java
    Like condition = Filters.endsWith("email", "@company.com");
    // SQL fragment: email LIKE '%@company.com'
    ```
##### notEndsWith(...) -> NotLike
- **Signature:** `public static NotLike notEndsWith(final String propName, final String propValue)`
- **Summary:** Creates a NotLike condition that checks if the property does not end with the specified value.
- **Contract:**
  - Creates a NotLike condition that checks if the property does not end with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the suffix to exclude (must not be null)
- **Returns:** a NotLike condition
- **Examples:**
  - ```java
    NotLike condition = Filters.notEndsWith("filename", ".tmp");
    // SQL fragment: filename NOT LIKE '%.tmp'
    ```
##### isNull(...) -> IsNull
- **Signature:** `public static IsNull isNull(final String propName)`
- **Summary:** Creates an IsNull condition to check if a property value is null.
- **Contract:**
  - Creates an IsNull condition to check if a property value is null.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNull condition
- **Examples:**
  - ```java
    IsNull condition = Filters.isNull("deleted_at");
    // SQL fragment: deleted_at IS NULL
    ```
##### isNullOrEmpty(...) -> Or
- **Signature:** `@Beta public static Or isNullOrEmpty(final String propName)`
- **Summary:** Creates a condition to check if a property is null or empty string.
- **Contract:**
  - Creates a condition to check if a property is null or empty string.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an Or condition combining null and empty checks
- **Examples:**
  - ```java
    Or condition = Filters.isNullOrEmpty("description");
    // SQL fragment: ((description IS NULL) OR (description = ''))
    ```
##### isNullOrZero(...) -> Or
- **Signature:** `@Beta public static Or isNullOrZero(final String propName)`
- **Summary:** Creates a condition to check if a property is null or zero.
- **Contract:**
  - Creates a condition to check if a property is null or zero.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an Or condition combining null and zero checks
- **Examples:**
  - ```java
    Or condition = Filters.isNullOrZero("quantity");
    // SQL fragment: ((quantity IS NULL) OR (quantity = 0))
    ```
##### isNotNull(...) -> IsNotNull
- **Signature:** `public static IsNotNull isNotNull(final String propName)`
- **Summary:** Creates an IsNotNull condition to check if a property value is not null.
- **Contract:**
  - Creates an IsNotNull condition to check if a property value is not null.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNotNull condition
- **Examples:**
  - ```java
    IsNotNull condition = Filters.isNotNull("created_at");
    // SQL fragment: created_at IS NOT NULL
    ```
##### isNotNullAndNotEmpty(...) -> And
- **Signature:** `@Beta public static And isNotNullAndNotEmpty(final String propName)`
- **Summary:** Creates a compound condition to check that a property is neither null nor an empty string.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition combining not-null and not-empty checks
- **Examples:**
  - ```java
    And condition = Filters.isNotNullAndNotEmpty("email");
    // SQL fragment: ((email IS NOT NULL) AND (email != ''))
    ```
##### isNotNullAndNotZero(...) -> And
- **Signature:** `@Beta public static And isNotNullAndNotZero(final String propName)`
- **Summary:** Creates a compound condition to check that a property is neither null nor zero.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition combining not-null and non-zero checks
- **Examples:**
  - ```java
    And condition = Filters.isNotNullAndNotZero("quantity");
    // SQL fragment: ((quantity IS NOT NULL) AND (quantity != 0))
    ```
##### isNaN(...) -> IsNaN
- **Signature:** `public static IsNaN isNaN(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is NaN (Not a Number).
- **Contract:**
  - Creates a condition to check if a numeric property value is NaN (Not a Number).
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNaN condition
- **Examples:**
  - ```java
    IsNaN condition = Filters.isNaN("calculation_result");
    // SQL fragment: calculation_result IS NAN
    ```
##### isNotNaN(...) -> IsNotNaN
- **Signature:** `public static IsNotNaN isNotNaN(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is not NaN.
- **Contract:**
  - Creates a condition to check if a numeric property value is not NaN.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNotNaN condition
- **Examples:**
  - ```java
    IsNotNaN condition = Filters.isNotNaN("temperature");
    // SQL fragment: temperature IS NOT NAN
    ```
##### isInfinite(...) -> IsInfinite
- **Signature:** `public static IsInfinite isInfinite(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is infinite.
- **Contract:**
  - Creates a condition to check if a numeric property value is infinite.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsInfinite condition
- **Examples:**
  - ```java
    IsInfinite condition = Filters.isInfinite("ratio");
    // SQL fragment: ratio IS INFINITE
    ```
##### isNotInfinite(...) -> IsNotInfinite
- **Signature:** `public static IsNotInfinite isNotInfinite(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is not infinite.
- **Contract:**
  - Creates a condition to check if a numeric property value is not infinite.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNotInfinite condition
- **Examples:**
  - ```java
    IsNotInfinite condition = Filters.isNotInfinite("percentage");
    // SQL fragment: percentage IS NOT INFINITE
    ```
##### is(...) -> Is
- **Signature:** `public static Is is(final String propName, final Object propValue)`
- **Summary:** Creates an Is condition (SQL IS predicate) for the specified property and value.
- **Contract:**
  - If propValue is Java null, the rendered SQL collapses to propName IS NULL.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the right-hand value (typically an SqlExpression); may be null (renders as IS NULL)
- **Returns:** an Is condition
- **Examples:**
  - ```java
    Is condition = Filters.is("status", Filters.expr("UNKNOWN"));
    // SQL fragment: status IS UNKNOWN
    ```
##### isNot(...) -> IsNot
- **Signature:** `public static IsNot isNot(final String propName, final Object propValue)`
- **Summary:** Creates an IsNot condition (SQL IS NOT predicate) for the specified property and value.
- **Contract:**
  - If propValue is Java null, the rendered SQL collapses to propName IS NOT NULL.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null, empty, or blank)
  - `propValue` (`Object`) — the right-hand value (typically an SqlExpression); may be null (renders as IS NOT NULL)
- **Returns:** an IsNot condition
- **Examples:**
  - ```java
    IsNot condition = Filters.isNot("status", Filters.expr("UNKNOWN"));
    // SQL fragment: status IS NOT UNKNOWN
    ```
##### or(...) -> Or
- **Signature:** `public static Or or(final Condition... conditions)`
- **Summary:** Creates an Or junction combining multiple conditions.
- **Contract:**
  - At least one condition must be true for the OR to be true.
- **Parameters:**
  - `conditions` (`Condition[]`) — the array of conditions to combine with OR; null or empty is permitted and yields an empty junction (which renders as an empty string)
- **Returns:** an Or junction
- **Examples:**
  - ```java
    Or condition = Filters.or(
        Filters.equal("status", "active"),
        Filters.greaterThan("priority", 5),
        Filters.isNull("deleted_at")
    );
    // Results in: ((status = 'active') OR (priority > 5) OR (deleted_at IS NULL))
    ```
- **Signature:** `public static Or or(final Collection<? extends Condition> conditions)`
- **Summary:** Creates an Or junction combining multiple conditions from a collection.
- **Contract:**
  - At least one condition must be true for the OR to be true.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with OR; null or empty is permitted and yields an empty junction (which renders as an empty string)
- **Returns:** an Or junction
- **Examples:**
  - ```java
    List<Condition> conditions = Arrays.asList(
        Filters.equal("type", "admin"),
        Filters.equal("type", "moderator")
    );
    Or condition = Filters.or(conditions);
    // Results in: ((type = 'admin') OR (type = 'moderator'))
    ```
##### and(...) -> And
- **Signature:** `public static And and(final Condition... conditions)`
- **Summary:** Creates an And junction combining multiple conditions.
- **Contract:**
  - All conditions must be true for the AND to be true.
- **Parameters:**
  - `conditions` (`Condition[]`) — the array of conditions to combine with AND; null or empty is permitted and yields an empty junction (which renders as an empty string)
- **Returns:** an And junction
- **Examples:**
  - ```java
    And condition = Filters.and(
        Filters.equal("status", "active"),
        Filters.greaterThanOrEqual("age", 18),
        Filters.isNotNull("email")
    );
    // Results in: ((status = 'active') AND (age >= 18) AND (email IS NOT NULL))
    ```
- **Signature:** `public static And and(final Collection<? extends Condition> conditions)`
- **Summary:** Creates an And junction combining multiple conditions from a collection.
- **Contract:**
  - All conditions must be true for the AND to be true.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with AND; null or empty is permitted and yields an empty junction (which renders as an empty string)
- **Returns:** an And junction
- **Examples:**
  - ```java
    List<Condition> conditions = Arrays.asList(
        Filters.between("price", 10, 100),
        Filters.equal("in_stock", true)
    );
    And condition = Filters.and(conditions);
    // Results in: ((price BETWEEN 10 AND 100) AND (in_stock = true))
    ```
##### junction(...) -> Junction
- **Signature:** `@Beta public static Junction junction(final Operator operator, final Condition... conditions)`
- **Summary:** Creates a Junction combining multiple conditions with the given operator, which must be Operator#AND or Operator#OR.
- **Contract:**
  - Creates a Junction combining multiple conditions with the given operator, which must be Operator#AND or Operator#OR.
  - This is useful when the operator is chosen at runtime; for a fixed operator prefer #and(Condition...) or #or(Condition...).
- **Parameters:**
  - `operator` (`Operator`) — the junction operator; must be Operator#AND or Operator#OR
  - `conditions` (`Condition[]`) — the array of conditions to combine; null or empty is permitted and yields an empty junction
- **Returns:** a Junction with the specified operator
- **Examples:**
  - ```java
    Junction condition = Filters.junction(Operator.OR,
        Filters.equal("flag1", true),
        Filters.equal("flag2", true)
    );
    // Results in: ((flag1 = true) OR (flag2 = true))
    ```
- **Signature:** `@Beta public static Junction junction(final Operator operator, final Collection<? extends Condition> conditions)`
- **Summary:** Creates a Junction combining conditions from a collection with the given operator, which must be Operator#AND or Operator#OR.
- **Contract:**
  - Creates a Junction combining conditions from a collection with the given operator, which must be Operator#AND or Operator#OR.
  - This is useful when the operator is chosen at runtime; for a fixed operator prefer #and(Collection) or #or(Collection).
- **Parameters:**
  - `operator` (`Operator`) — the junction operator; must be Operator#AND or Operator#OR
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine; null or empty is permitted and yields an empty junction
- **Returns:** a Junction with the specified operator
- **Examples:**
  - ```java
    List<Condition> conditionsList = Arrays.asList(Filters.equal("flag1", true), Filters.equal("flag2", true));
    Junction condition = Filters.junction(Operator.OR, conditionsList);
    // Results in: ((flag1 = true) OR (flag2 = true))
    ```
##### where(...) -> Where
- **Signature:** `public static Where where(final Condition condition)`
- **Summary:** Creates a Where clause with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition for the WHERE clause (must not be null)
- **Returns:** a Where clause
- **Examples:**
  - ```java
    Where where = Filters.where(Filters.equal("active", true));
    // Results in SQL like: WHERE active = true
    ```
- **Signature:** `public static Where where(final String expr)`
- **Summary:** Creates a Where clause from a raw SQL expression string.
- **Parameters:**
  - `expr` (`String`) — the SQL expression as a string (must not be null, empty, or blank)
- **Returns:** a Where clause
- **Examples:**
  - ```java
    Where where = Filters.where("YEAR(created_date) = 2023");
    // Results in SQL like: WHERE YEAR(created_date) = 2023
    ```
##### groupByAsc(...) -> GroupBy
- **Signature:** `public static GroupBy groupByAsc(final String propOrColumnName)`
- **Summary:** Creates a GroupBy clause with ascending order for a single property.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property/column name to group by ascending
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupByAsc("department");
    // Results in SQL like: GROUP BY department ASC
    ```
- **Signature:** `public static GroupBy groupByAsc(final String... propNames)`
- **Summary:** Creates a GroupBy clause with ascending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to group by ascending
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupByAsc("department", "role");
    // Results in SQL like: GROUP BY department ASC, role ASC
    ```
- **Signature:** `public static GroupBy groupByAsc(final Collection<String> propNames)`
- **Summary:** Creates a GroupBy clause with ascending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by ascending
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("department", "role");
    GroupBy groupBy = Filters.groupByAsc(columns);
    // Results in SQL like: GROUP BY department ASC, role ASC
    ```
##### groupByDesc(...) -> GroupBy
- **Signature:** `public static GroupBy groupByDesc(final String propOrColumnName)`
- **Summary:** Creates a GroupBy clause with descending order for a single property.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property/column name to group by descending
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupByDesc("sales");
    // Results in SQL like: GROUP BY sales DESC
    ```
- **Signature:** `public static GroupBy groupByDesc(final String... propNames)`
- **Summary:** Creates a GroupBy clause with descending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to group by descending
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupByDesc("sales", "region");
    // Results in SQL like: GROUP BY sales DESC, region DESC
    ```
- **Signature:** `public static GroupBy groupByDesc(final Collection<String> propNames)`
- **Summary:** Creates a GroupBy clause with descending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by descending
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("sales", "region");
    GroupBy groupBy = Filters.groupByDesc(columns);
    // Results in SQL like: GROUP BY sales DESC, region DESC
    ```
##### groupBy(...) -> GroupBy
- **Signature:** `public static GroupBy groupBy(final String... propNames)`
- **Summary:** Creates a GroupBy clause with the specified property names.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to group by
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupBy("department", "role");
    // Results in SQL like: GROUP BY department, role
    ```
- **Signature:** `public static GroupBy groupBy(final Collection<String> propNames)`
- **Summary:** Creates a GroupBy clause with properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("country", "city");
    GroupBy groupBy = Filters.groupBy(columns);
    // Results in SQL like: GROUP BY country, city
    ```
- **Signature:** `public static GroupBy groupBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates a GroupBy clause with properties and specified sort direction.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupBy(Arrays.asList("sales", "region"), SortDirection.DESC);
    // Results in SQL like: GROUP BY sales DESC, region DESC
    ```
- **Signature:** `public static GroupBy groupBy(final String propName, final SortDirection direction)`
- **Summary:** Creates a GroupBy clause with a single property and sort direction.
- **Parameters:**
  - `propName` (`String`) — the property/column name to group by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupBy("category", SortDirection.DESC);
    // Results in SQL like: GROUP BY category DESC
    ```
- **Signature:** `public static GroupBy groupBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2)`
- **Summary:** Creates a GroupBy clause with two properties and their respective sort directions.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `direction1` (`SortDirection`) — first property sort direction
  - `propName2` (`String`) — second property name
  - `direction2` (`SortDirection`) — second property sort direction
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
    // Results in SQL like: GROUP BY year DESC, month ASC
    ```
- **Signature:** `public static GroupBy groupBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2, final String propName3, final SortDirection direction3)`
- **Summary:** Creates a GroupBy clause with three properties and their respective sort directions.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `direction1` (`SortDirection`) — first property sort direction
  - `propName2` (`String`) — second property name
  - `direction2` (`SortDirection`) — second property sort direction
  - `propName3` (`String`) — third property name
  - `direction3` (`SortDirection`) — third property sort direction
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupBy("country", SortDirection.ASC, "state", SortDirection.ASC, "city", SortDirection.DESC);
    // Results in SQL like: GROUP BY country ASC, state ASC, city DESC
    ```
- **Signature:** `public static GroupBy groupBy(final Map<String, SortDirection> groupings)`
- **Summary:** Creates a GroupBy clause from a map of property names to sort directions.
- **Contract:**
  - The map should be a java.util.LinkedHashMap to preserve insertion order.
- **Parameters:**
  - `groupings` (`Map<String, SortDirection>`) — map of property names to sort directions (should be a java.util.LinkedHashMap to preserve order)
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    Map<String, SortDirection> orders = new LinkedHashMap<>();
    orders.put("department", SortDirection.ASC);
    orders.put("salary", SortDirection.DESC);
    GroupBy groupBy = Filters.groupBy(orders);
    // Results in SQL like: GROUP BY department ASC, salary DESC
    ```
- **Signature:** `public static GroupBy groupBy(final Condition condition)`
- **Summary:** Creates a GroupBy clause with a custom condition.
- **Parameters:**
  - `condition` (`Condition`) — the grouping condition (must not be null)
- **Returns:** a GroupBy clause
- **Examples:**
  - ```java
    GroupBy groupBy = Filters.groupBy(
        Filters.expr("YEAR(order_date), MONTH(order_date)")
    );
    ```
##### having(...) -> Having
- **Signature:** `public static Having having(final Condition condition)`
- **Summary:** Creates a Having clause with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition for the HAVING clause (must not be null)
- **Returns:** a Having clause
- **Examples:**
  - ```java
    Having having = Filters.having(Filters.greaterThan("COUNT(*)", 5));
    // Results in SQL like: HAVING COUNT(*) > 5
    ```
- **Signature:** `public static Having having(final String expr)`
- **Summary:** Creates a Having clause from a raw SQL expression string.
- **Parameters:**
  - `expr` (`String`) — the SQL expression as a string (must not be null, empty, or blank)
- **Returns:** a Having clause
- **Examples:**
  - ```java
    Having having = Filters.having("SUM(amount) > 1000");
    // Results in SQL like: HAVING SUM(amount) > 1000
    ```
##### orderByAsc(...) -> OrderBy
- **Signature:** `public static OrderBy orderByAsc(final String propOrColumnName)`
- **Summary:** Creates an OrderBy clause with ascending order for a single property.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property/column name to order by ascending
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderByAsc("created_date");
    // Results in SQL like: ORDER BY created_date ASC
    ```
- **Signature:** `public static OrderBy orderByAsc(final String... propNames)`
- **Summary:** Creates an OrderBy clause with ascending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by ascending
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderByAsc("created_date", "id");
    // Results in SQL like: ORDER BY created_date ASC, id ASC
    ```
- **Signature:** `public static OrderBy orderByAsc(final Collection<String> propNames)`
- **Summary:** Creates an OrderBy clause with ascending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by ascending
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("priority", "created_date");
    OrderBy orderBy = Filters.orderByAsc(columns);
    // Results in SQL like: ORDER BY priority ASC, created_date ASC
    ```
##### orderByDesc(...) -> OrderBy
- **Signature:** `public static OrderBy orderByDesc(final String propOrColumnName)`
- **Summary:** Creates an OrderBy clause with descending order for a single property.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property/column name to order by descending
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderByDesc("score");
    // Results in SQL like: ORDER BY score DESC
    ```
- **Signature:** `public static OrderBy orderByDesc(final String... propNames)`
- **Summary:** Creates an OrderBy clause with descending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by descending
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderByDesc("score", "timestamp");
    // Results in SQL like: ORDER BY score DESC, timestamp DESC
    ```
- **Signature:** `public static OrderBy orderByDesc(final Collection<String> propNames)`
- **Summary:** Creates an OrderBy clause with descending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by descending
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("amount", "date");
    OrderBy orderBy = Filters.orderByDesc(columns);
    // Results in SQL like: ORDER BY amount DESC, date DESC
    ```
##### orderBy(...) -> OrderBy
- **Signature:** `public static OrderBy orderBy(final String... propNames)`
- **Summary:** Creates an OrderBy clause with the specified property names.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderBy("last_name", "first_name");
    // Results in SQL like: ORDER BY last_name, first_name
    ```
- **Signature:** `public static OrderBy orderBy(final Collection<String> propNames)`
- **Summary:** Creates an OrderBy clause with properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    List<String> columns = Arrays.asList("name", "age");
    OrderBy orderBy = Filters.orderBy(columns);
    // Results in SQL like: ORDER BY name, age
    ```
- **Signature:** `public static OrderBy orderBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates an OrderBy clause with properties and specified sort direction.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderBy(Arrays.asList("price", "rating"), SortDirection.DESC);
    // Results in SQL like: ORDER BY price DESC, rating DESC
    ```
- **Signature:** `public static OrderBy orderBy(final String propName, final SortDirection direction)`
- **Summary:** Creates an OrderBy clause with a single property and sort direction.
- **Parameters:**
  - `propName` (`String`) — the property/column name to order by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderBy("modified_date", SortDirection.DESC);
    // Results in SQL like: ORDER BY modified_date DESC
    ```
- **Signature:** `public static OrderBy orderBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2)`
- **Summary:** Creates an OrderBy clause with two properties and their respective sort directions.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `direction1` (`SortDirection`) — first property sort direction
  - `propName2` (`String`) — second property name
  - `direction2` (`SortDirection`) — second property sort direction
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderBy("status", SortDirection.ASC, "priority", SortDirection.DESC);
    // Results in SQL like: ORDER BY status ASC, priority DESC
    ```
- **Signature:** `public static OrderBy orderBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2, final String propName3, final SortDirection direction3)`
- **Summary:** Creates an OrderBy clause with three properties and their respective sort directions.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `direction1` (`SortDirection`) — first property sort direction
  - `propName2` (`String`) — second property name
  - `direction2` (`SortDirection`) — second property sort direction
  - `propName3` (`String`) — third property name
  - `direction3` (`SortDirection`) — third property sort direction
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderBy("year", SortDirection.DESC, "month", SortDirection.DESC, "day", SortDirection.ASC);
    // Results in SQL like: ORDER BY year DESC, month DESC, day ASC
    ```
- **Signature:** `public static OrderBy orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Creates an OrderBy clause from a map of property names to sort directions.
- **Contract:**
  - The map should be a java.util.LinkedHashMap to preserve insertion order.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of property names to sort directions (should be a java.util.LinkedHashMap to preserve order)
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    Map<String, SortDirection> orders = new LinkedHashMap<>();
    orders.put("category", SortDirection.ASC);
    orders.put("price", SortDirection.DESC);
    orders.put("name", SortDirection.ASC);
    OrderBy orderBy = Filters.orderBy(orders);
    // Results in SQL like: ORDER BY category ASC, price DESC, name ASC
    ```
- **Signature:** `public static OrderBy orderBy(final Condition condition)`
- **Summary:** Creates an OrderBy clause with a custom condition.
- **Parameters:**
  - `condition` (`Condition`) — the ordering condition (must not be null)
- **Returns:** an OrderBy clause
- **Examples:**
  - ```java
    OrderBy orderBy = Filters.orderBy(
        Filters.expr("CASE WHEN status = 'urgent' THEN 1 ELSE 2 END, created_date DESC")
    );
    // Results in SQL like: ORDER BY CASE WHEN status = 'urgent' THEN 1 ELSE 2 END, created_date DESC
    ```
##### on(...) -> On
- **Signature:** `public static On on(final Condition condition)`
- **Summary:** Creates an On clause for JOIN operations with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the join condition (must not be null)
- **Returns:** an On clause
- **Examples:**
  - ```java
    On on = Filters.on(Filters.expr("users.id = orders.user_id"));
    // Results in SQL like: ON users.id = orders.user_id
    ```
- **Signature:** `public static On on(final String expr)`
- **Summary:** Creates an On clause from a raw SQL expression string for JOIN operations.
- **Parameters:**
  - `expr` (`String`) — the join condition as a string (must not be null, empty, or blank)
- **Returns:** an On clause
- **Examples:**
  - ```java
    On on = Filters.on("users.department_id = departments.id AND users.active = true");
    // Results in SQL like: ON users.department_id = departments.id AND users.active = true
    ```
- **Signature:** `public static On on(final String leftPropName, final String rightPropName)`
- **Summary:** Creates an On clause for simple equality join between two columns.
- **Parameters:**
  - `leftPropName` (`String`) — the first column name
  - `rightPropName` (`String`) — the second column name to join with
- **Returns:** an On clause
- **Examples:**
  - ```java
    On on = Filters.on("user_id", "id");
    // Results in SQL like: ON user_id = id
    ```
- **Signature:** `public static On on(final Map<String, String> propNamePairs)`
- **Summary:** Creates an On clause from a map of column pairs for JOIN operations.
- **Parameters:**
  - `propNamePairs` (`Map<String, String>`) — map of column name pairs for joining (should be a java.util.LinkedHashMap to preserve order; must not be null or empty)
- **Returns:** an On clause
- **Examples:**
  - ```java
    Map<String, String> joinPairs = new LinkedHashMap<>();
    joinPairs.put("orders.user_id", "users.id");
    joinPairs.put("orders.product_id", "products.id");
    On on = Filters.on(joinPairs);
    // Generates: ON ((orders.user_id = users.id) AND (orders.product_id = products.id))
    ```
##### using(...) -> Using
- **Signature:** `@Deprecated public static Using using(final String... columnNames)`
- **Summary:** Creates a USING clause for JOIN operations with the specified columns.
- **Contract:**
  - USING is an alternative to ON when joining tables on columns with the same name.
- **Parameters:**
  - `columnNames` (`String[]`) — the column names used for joining
- **Returns:** a Using clause
- **Examples:**
  - ```java
    Using usingClause = Filters.using("user_id", "department_id");
    // Results in SQL like: USING (user_id, department_id)
    ```
- **Signature:** `@Deprecated public static Using using(final Collection<String> columnNames)`
- **Summary:** Creates a USING clause from a collection of column names for JOIN operations.
- **Parameters:**
  - `columnNames` (`Collection<String>`) — collection of column names used for joining
- **Returns:** a Using clause
- **Examples:**
  - ```java
    List<String> cols = Arrays.asList("user_id", "department_id");
    Using usingClause = Filters.using(cols);
    // Results in SQL like: USING (user_id, department_id)
    ```
##### join(...) -> Join
- **Signature:** `public static Join join(final String joinEntity)`
- **Summary:** Creates a Join clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to join
- **Returns:** a Join clause
- **Examples:**
  - ```java
    Join join = Filters.join("orders");
    // Results in SQL like: JOIN orders
    ```
- **Signature:** `public static Join join(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a Join clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** a Join clause
- **Examples:**
  - ```java
    Join join = Filters.join("orders",
        Filters.on("users.id", "orders.user_id"));
    // Results in SQL like: JOIN orders ON users.id = orders.user_id
    ```
- **Signature:** `public static Join join(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a Join clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** a Join clause
- **Examples:**
  - ```java
    Join join = Filters.join(Arrays.asList("orders", "products"),
        Filters.on("orders.product_id", "products.id"));
    // Results in SQL like: JOIN (orders, products) ON orders.product_id = products.id
    ```
##### leftJoin(...) -> LeftJoin
- **Signature:** `public static LeftJoin leftJoin(final String joinEntity)`
- **Summary:** Creates a LEFT JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to left join
- **Returns:** a LeftJoin clause
- **Examples:**
  - ```java
    LeftJoin join = Filters.leftJoin("orders");
    // Results in SQL like: LEFT JOIN orders
    ```
- **Signature:** `public static LeftJoin leftJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a LEFT JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to left join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** a LeftJoin clause
- **Examples:**
  - ```java
    LeftJoin join = Filters.leftJoin("orders",
        Filters.on("users.id", "orders.user_id"));
    // Results in SQL like: LEFT JOIN orders ON users.id = orders.user_id
    ```
- **Signature:** `public static LeftJoin leftJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a LEFT JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to left join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** a LeftJoin clause
- **Examples:**
  - ```java
    LeftJoin join = Filters.leftJoin(Arrays.asList("orders", "order_items"),
        Filters.on("orders.id", "order_items.order_id"));
    // Results in SQL like: LEFT JOIN (orders, order_items) ON orders.id = order_items.order_id
    ```
##### rightJoin(...) -> RightJoin
- **Signature:** `public static RightJoin rightJoin(final String joinEntity)`
- **Summary:** Creates a RIGHT JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to right join
- **Returns:** a RightJoin clause
- **Examples:**
  - ```java
    RightJoin join = Filters.rightJoin("users");
    // Results in SQL like: RIGHT JOIN users
    ```
- **Signature:** `public static RightJoin rightJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a RIGHT JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to right join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** a RightJoin clause
- **Examples:**
  - ```java
    RightJoin join = Filters.rightJoin("users",
        Filters.on("orders.user_id", "users.id"));
    // Results in SQL like: RIGHT JOIN users ON orders.user_id = users.id
    ```
- **Signature:** `public static RightJoin rightJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a RIGHT JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to right join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** a RightJoin clause
- **Examples:**
  - ```java
    RightJoin join = Filters.rightJoin(Arrays.asList("departments", "locations"),
        Filters.on("departments.location_id", "locations.id"));
    // Results in SQL like: RIGHT JOIN (departments, locations) ON departments.location_id = locations.id
    ```
##### crossJoin(...) -> CrossJoin
- **Signature:** `public static CrossJoin crossJoin(final String joinEntity)`
- **Summary:** Creates a CROSS JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to cross join
- **Returns:** a CrossJoin clause
- **Examples:**
  - ```java
    CrossJoin join = Filters.crossJoin("colors");
    // Results in SQL like: CROSS JOIN colors
    ```
- **Signature:** `public static CrossJoin crossJoin(final Collection<String> joinEntities)`
- **Summary:** Creates a conditionless CROSS JOIN for multiple entities.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the entity/table names to cross join
- **Returns:** a CrossJoin clause
- **Examples:**
  - ```java
    CrossJoin join = Filters.crossJoin(Arrays.asList("colors", "sizes"));
    // Results in SQL like: CROSS JOIN (colors, sizes)
    ```
##### fullJoin(...) -> FullJoin
- **Signature:** `public static FullJoin fullJoin(final String joinEntity)`
- **Summary:** Creates a FULL JOIN clause for the specified entity/table.
- **Contract:**
  - Returns all records when there is a match in either table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to full join
- **Returns:** a FullJoin clause
- **Examples:**
  - ```java
    FullJoin join = Filters.fullJoin("departments");
    // Results in SQL like: FULL JOIN departments
    ```
- **Signature:** `public static FullJoin fullJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a FULL JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to full join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** a FullJoin clause
- **Examples:**
  - ```java
    FullJoin join = Filters.fullJoin("employees",
        Filters.on("departments.id", "employees.dept_id"));
    // Results in SQL like: FULL JOIN employees ON departments.id = employees.dept_id
    ```
- **Signature:** `public static FullJoin fullJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a FULL JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to full join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** a FullJoin clause
- **Examples:**
  - ```java
    FullJoin join = Filters.fullJoin(Arrays.asList("employees", "contractors"),
        Filters.on("employees.project_id", "contractors.project_id"));
    // Results in SQL like: FULL JOIN (employees, contractors) ON employees.project_id = contractors.project_id
    ```
##### innerJoin(...) -> InnerJoin
- **Signature:** `public static InnerJoin innerJoin(final String joinEntity)`
- **Summary:** Creates an INNER JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to inner join
- **Returns:** an InnerJoin clause
- **Examples:**
  - ```java
    InnerJoin join = Filters.innerJoin("orders");
    // Results in SQL like: INNER JOIN orders
    ```
- **Signature:** `public static InnerJoin innerJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates an INNER JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to inner join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** an InnerJoin clause
- **Examples:**
  - ```java
    InnerJoin join = Filters.innerJoin("products",
        Filters.on("order_items.product_id", "products.id"));
    // Results in SQL like: INNER JOIN products ON order_items.product_id = products.id
    ```
- **Signature:** `public static InnerJoin innerJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates an INNER JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to inner join
  - `joinCondition` (`Condition`) — the join condition; may be null for a condition-less join
- **Returns:** an InnerJoin clause
- **Examples:**
  - ```java
    InnerJoin join = Filters.innerJoin(Arrays.asList("orders", "order_details"),
        Filters.on("orders.id", "order_details.order_id"));
    // Results in SQL like: INNER JOIN (orders, order_details) ON orders.id = order_details.order_id
    ```
##### naturalJoin(...) -> NaturalJoin
- **Signature:** `public static NaturalJoin naturalJoin(final String joinEntity)`
- **Summary:** Creates a NATURAL JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to natural join
- **Returns:** a NaturalJoin clause
- **Examples:**
  - ```java
    NaturalJoin join = Filters.naturalJoin("departments");
    // Results in SQL like: NATURAL JOIN departments
    ```
- **Signature:** `public static NaturalJoin naturalJoin(final Collection<String> joinEntities)`
- **Summary:** Creates a conditionless NATURAL JOIN for multiple entities.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the entity/table names to natural join
- **Returns:** a NaturalJoin clause
- **Examples:**
  - ```java
    NaturalJoin join = Filters.naturalJoin(Arrays.asList("employees", "departments"));
    // Results in SQL like: NATURAL JOIN (employees, departments)
    ```
##### in(...) -> In
- **Signature:** `public static In in(final String propName, final boolean[] values)`
- **Summary:** Creates an IN condition with an array of boolean values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`boolean[]`) — array of boolean values
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in("active", new boolean[] {true, false});
    // SQL fragment: active IN (true, false)
    ```
- **Signature:** `public static In in(final String propName, final char[] values)`
- **Summary:** Creates an IN condition with an array of char values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`char[]`) — array of char values
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in("grade", new char[] {'A', 'B', 'C'});
    // SQL fragment: grade IN ('A', 'B', 'C')
    ```
- **Signature:** `public static In in(final String propName, final byte[] values)`
- **Summary:** Creates an IN condition with an array of byte values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`byte[]`) — array of byte values
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in("flag", new byte[] {0, 1, 2});
    // SQL fragment: flag IN (0, 1, 2)
    ```
- **Signature:** `public static In in(final String propName, final short[] values)`
- **Summary:** Creates an IN condition with an array of short values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`short[]`) — array of short values
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in("level", new short[] {1, 2, 3});
    // SQL fragment: level IN (1, 2, 3)
    ```
- **Signature:** `public static In in(final String propName, final int[] values)`
- **Summary:** Creates an IN condition with an array of integer values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`int[]`) — array of integer values
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in("user_id", new int[] {1, 2, 3, 4});
    // SQL fragment: user_id IN (1, 2, 3, 4)
    ```
- **Signature:** `public static In in(final String propName, final long[] values)`
- **Summary:** Creates an IN condition with an array of long values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`long[]`) — array of long values
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in("order_id", new long[] {1001L, 1002L, 1003L});
    // SQL fragment: order_id IN (1001, 1002, 1003)
    ```
- **Signature:** `public static In in(final String propName, final float[] values)`
- **Summary:** Creates an IN condition with an array of float values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`float[]`) — array of float values
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in("ratio", new float[] {0.25f, 0.5f, 0.75f});
    // SQL fragment: ratio IN (0.25, 0.5, 0.75)
    ```
- **Signature:** `public static In in(final String propName, final double[] values)`
- **Summary:** Creates an IN condition with an array of double values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`double[]`) — array of double values
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in("price", new double[] {9.99, 19.99, 29.99});
    // SQL fragment: price IN (9.99, 19.99, 29.99)
    ```
- **Signature:** `public static In in(final String propName, final Object... values)`
- **Summary:** Creates an IN condition with an array of object values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Object[]`) — array of values
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in("status", new String[] {"active", "pending", "approved"});
    // SQL fragment: status IN ('active', 'pending', 'approved')
    ```
- **Signature:** `public static In in(final String propName, final Collection<?> values)`
- **Summary:** Creates an IN condition with a collection of values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Collection<?>`) — collection of values
- **Returns:** an In condition
- **Examples:**
  - ```java
    List<String> categories = Arrays.asList("electronics", "books", "toys");
    In condition = Filters.in("category", categories);
    // SQL fragment: category IN ('electronics', 'books', 'toys')
    ```
- **Signature:** `public static In in(final Collection<String> propNames, final Collection<?> valueRows)`
- **Summary:** Creates a row value constructor IN condition.
- **Contract:**
  - The tuple of property values must match one of the supplied value rows.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the property/column names (must not be null or empty and must not contain null/blank names)
  - `valueRows` (`Collection<?>`) — collection of value rows; each row must resolve to exactly propNames.size() values. A row may be a Collection, Iterable, object array, Map or bean
- **Returns:** an In condition
- **Examples:**
  - ```java
    In condition = Filters.in(Arrays.asList("first_name", "last_name"),
            Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));
    // SQL fragment: (first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))
    ```
- **Signature:** `public static InSubQuery in(final String propName, final SubQuery subQuery)`
- **Summary:** Creates an IN condition with a subquery.
- **Contract:**
  - The property value must be in the result set of the subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** an InSubQuery condition
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT id FROM active_users");
    InSubQuery condition = Filters.in("user_id", subQuery);
    // SQL fragment: user_id IN (SELECT id FROM active_users)
    ```
- **Signature:** `public static InSubQuery in(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates an IN condition with multiple properties and a subquery.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** an InSubQuery condition
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT user_id, order_id FROM recent_orders");
    InSubQuery condition = Filters.in(Arrays.asList("user_id", "order_id"), subQuery);
    // SQL fragment: (user_id, order_id) IN (SELECT user_id, order_id FROM recent_orders)
    ```
##### notIn(...) -> NotIn
- **Signature:** `public static NotIn notIn(final String propName, final boolean[] values)`
- **Summary:** Creates a NOT IN condition with an array of boolean values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`boolean[]`) — array of boolean values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn("active", new boolean[] {false});
    // SQL fragment: active NOT IN (false)
    ```
- **Signature:** `public static NotIn notIn(final String propName, final char[] values)`
- **Summary:** Creates a NOT IN condition with an array of char values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`char[]`) — array of char values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn("grade", new char[] {'D', 'F'});
    // SQL fragment: grade NOT IN ('D', 'F')
    ```
- **Signature:** `public static NotIn notIn(final String propName, final byte[] values)`
- **Summary:** Creates a NOT IN condition with an array of byte values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`byte[]`) — array of byte values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn("flag", new byte[] {0, 1});
    // SQL fragment: flag NOT IN (0, 1)
    ```
- **Signature:** `public static NotIn notIn(final String propName, final short[] values)`
- **Summary:** Creates a NOT IN condition with an array of short values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`short[]`) — array of short values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn("level", new short[] {0, 9});
    // SQL fragment: level NOT IN (0, 9)
    ```
- **Signature:** `public static NotIn notIn(final String propName, final int[] values)`
- **Summary:** Creates a NOT IN condition with an array of integer values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`int[]`) — array of integer values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn("status_code", new int[] {404, 500, 503});
    // SQL fragment: status_code NOT IN (404, 500, 503)
    ```
- **Signature:** `public static NotIn notIn(final String propName, final long[] values)`
- **Summary:** Creates a NOT IN condition with an array of long values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`long[]`) — array of long values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn("excluded_ids", new long[] {110L, 120L, 130L});
    // SQL fragment: excluded_ids NOT IN (110, 120, 130)
    ```
- **Signature:** `public static NotIn notIn(final String propName, final float[] values)`
- **Summary:** Creates a NOT IN condition with an array of float values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`float[]`) — array of float values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn("ratio", new float[] {0.0f, 1.0f});
    // SQL fragment: ratio NOT IN (0.0, 1.0)
    ```
- **Signature:** `public static NotIn notIn(final String propName, final double[] values)`
- **Summary:** Creates a NOT IN condition with an array of double values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`double[]`) — array of double values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn("discount", new double[] {0.0, 100.0});
    // SQL fragment: discount NOT IN (0.0, 100.0)
    ```
- **Signature:** `public static NotIn notIn(final String propName, final Object... values)`
- **Summary:** Creates a NOT IN condition with an array of object values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Object[]`) — array of values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn("role", new String[] {"guest", "banned"});
    // SQL fragment: role NOT IN ('guest', 'banned')
    ```
- **Signature:** `public static NotIn notIn(final String propName, final Collection<?> values)`
- **Summary:** Creates a NOT IN condition with a collection of values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Collection<?>`) — collection of values to exclude
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    Set<String> excludedCountries = new HashSet<>(Arrays.asList("XX", "YY"));
    NotIn condition = Filters.notIn("country_code", excludedCountries);
    // SQL fragment: country_code NOT IN ('XX', 'YY')
    ```
- **Signature:** `public static NotIn notIn(final Collection<String> propNames, final Collection<?> valueRows)`
- **Summary:** Creates a row value constructor NOT IN condition.
- **Contract:**
  - The tuple of property values must not match any of the supplied value rows.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the property/column names (must not be null or empty and must not contain null/blank names)
  - `valueRows` (`Collection<?>`) — collection of value rows to exclude; each row must resolve to exactly propNames.size() values. A row may be a Collection, Iterable, object array, Map or bean
- **Returns:** a NotIn condition
- **Examples:**
  - ```java
    NotIn condition = Filters.notIn(Arrays.asList("first_name", "last_name"),
            Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));
    // SQL fragment: (first_name, last_name) NOT IN (('John', 'Doe'), ('Jane', 'Roe'))
    ```
- **Signature:** `public static NotInSubQuery notIn(final String propName, final SubQuery subQuery)`
- **Summary:** Creates a NOT IN condition with a subquery.
- **Contract:**
  - The property value must not be in the result set of the subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** a NotInSubQuery condition
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT id FROM blacklisted_users");
    NotInSubQuery condition = Filters.notIn("user_id", subQuery);
    // SQL fragment: user_id NOT IN (SELECT id FROM blacklisted_users)
    ```
- **Signature:** `public static NotInSubQuery notIn(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates a NOT IN condition with multiple properties and a subquery.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** a NotInSubQuery condition
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT user_id, product_id FROM returns");
    NotInSubQuery condition = Filters.notIn(Arrays.asList("user_id", "product_id"), subQuery);
    // SQL fragment: (user_id, product_id) NOT IN (SELECT user_id, product_id FROM returns)
    ```
##### all(...) -> All
- **Signature:** `public static All all(final SubQuery subQuery)`
- **Summary:** Creates an ALL condition for comparison with all values from a subquery.
- **Contract:**
  - The condition is true if the comparison is true for all values returned by the subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery
- **Returns:** an All condition
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE dept = 'IT'");
    All condition = Filters.all(subQuery);
    // This factory only wraps the subquery as: ALL (SELECT salary FROM employees WHERE dept = 'IT').
    // When used as the RHS of a comparison such as gt, the full fragment renders:
    // salary > ALL (SELECT salary FROM employees WHERE dept = 'IT')
    ```
##### any(...) -> Any
- **Signature:** `public static Any any(final SubQuery subQuery)`
- **Summary:** Creates an ANY condition for comparison with any value from a subquery.
- **Contract:**
  - The condition is true if the comparison is true for at least one value returned by the subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery
- **Returns:** an Any condition
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'electronics'");
    Any condition = Filters.any(subQuery);
    // This factory only wraps the subquery as: ANY (SELECT price FROM products WHERE category = 'electronics').
    // When used as the RHS of a comparison such as lt, the full fragment renders:
    // price < ANY (SELECT price FROM products WHERE category = 'electronics')
    ```
##### some(...) -> Some
- **Signature:** `public static Some some(final SubQuery subQuery)`
- **Summary:** Creates a SOME condition for comparison with some values from a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery
- **Returns:** a Some condition
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT score FROM exams WHERE student_id = 123");
    Some condition = Filters.some(subQuery);
    // This factory only wraps the subquery as: SOME (SELECT score FROM exams WHERE student_id = 123).
    // When used as the RHS of a comparison such as le, the full fragment renders:
    // passing_score <= SOME (SELECT score FROM exams WHERE student_id = 123)
    ```
##### exists(...) -> Exists
- **Signature:** `public static Exists exists(final SubQuery subQuery)`
- **Summary:** Creates an EXISTS condition to check if a subquery returns any rows.
- **Contract:**
  - Creates an EXISTS condition to check if a subquery returns any rows.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check
- **Returns:** an Exists condition
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.user_id = users.id");
    Exists condition = Filters.exists(subQuery);
    // SQL fragment: EXISTS (SELECT 1 FROM orders WHERE orders.user_id = users.id)
    ```
##### notExists(...) -> NotExists
- **Signature:** `public static NotExists notExists(final SubQuery subQuery)`
- **Summary:** Creates a NOT EXISTS condition to check if a subquery returns no rows.
- **Contract:**
  - Creates a NOT EXISTS condition to check if a subquery returns no rows.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check
- **Returns:** a NotExists condition
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT 1 FROM archived_users WHERE archived_users.id = users.id");
    NotExists condition = Filters.notExists(subQuery);
    // SQL fragment: NOT EXISTS (SELECT 1 FROM archived_users WHERE archived_users.id = users.id)
    ```
##### union(...) -> Union
- **Signature:** `public static Union union(final SubQuery subQuery)`
- **Summary:** Creates a UNION clause to combine results from a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with
- **Returns:** a Union clause
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT id FROM archived_users");
    Union union = Filters.union(subQuery);
    // Results in SQL like: UNION SELECT id FROM archived_users
    ```
##### unionAll(...) -> UnionAll
- **Signature:** `public static UnionAll unionAll(final SubQuery subQuery)`
- **Summary:** Creates a UNION ALL clause to combine results from a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with
- **Returns:** a UnionAll clause
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT name FROM inactive_products");
    UnionAll unionAll = Filters.unionAll(subQuery);
    // Results in SQL like: UNION ALL SELECT name FROM inactive_products
    ```
##### except(...) -> Except
- **Signature:** `public static Except except(final SubQuery subQuery)`
- **Summary:** Creates an EXCEPT clause to subtract results from a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to subtract
- **Returns:** an Except clause
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT id FROM blacklisted_customers");
    Except except = Filters.except(subQuery);
    // Results in SQL like: EXCEPT SELECT id FROM blacklisted_customers
    ```
##### intersect(...) -> Intersect
- **Signature:** `public static Intersect intersect(final SubQuery subQuery)`
- **Summary:** Creates an INTERSECT clause to find common results with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to intersect with
- **Returns:** an Intersect clause
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT product_id FROM discounted_items");
    Intersect intersect = Filters.intersect(subQuery);
    // Results in SQL like: INTERSECT SELECT product_id FROM discounted_items
    ```
##### minus(...) -> Minus
- **Signature:** `public static Minus minus(final SubQuery subQuery)`
- **Summary:** Creates a MINUS clause to subtract results from a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to subtract
- **Returns:** a Minus clause
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT id FROM deleted_records");
    Minus minus = Filters.minus(subQuery);
    // Results in SQL like: MINUS SELECT id FROM deleted_records
    ```
##### subQuery(...) -> SubQuery
- **Signature:** `public static SubQuery subQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a SubQuery from an entity class with selected properties and condition.
- **Contract:**
  - If condition is not already a Criteria or a clause (such as Where), it is automatically wrapped in a WHERE clause at construction time; condition() on the returned subquery returns the wrapping Where.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table (must not be null)
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be null or empty, and must not contain null, empty, or blank elements)
  - `condition` (`Condition`) — the WHERE condition for the subquery; may be null for no WHERE clause (a blank SqlExpression condition is likewise treated as no filter condition)
- **Returns:** a SubQuery
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery(User.class,
        Arrays.asList("id", "name"),
        Filters.equal("active", true));
    // Generates: SELECT id, name FROM User WHERE active = true
    ```
- **Signature:** `public static SubQuery subQuery(final Class<?> entityClass, final String propName, final Condition condition)`
- **Summary:** Creates a structured single-property subquery for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class (must not be null)
  - `propName` (`String`) — the property to select (must not be null, empty, or blank)
  - `condition` (`Condition`) — the optional query condition; may be null
- **Returns:** a structured subquery
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery(User.class, "id", Filters.equal("active", true));
    // Generates: SELECT id FROM User WHERE active = true
    ```
- **Signature:** `public static SubQuery subQuery(final Class<?> entityClass, final Collection<String> propNames, final String expr)`
- **Summary:** Creates a SubQuery from an entity class with selected properties and a raw SQL condition string.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table (must not be null)
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be null or empty, and must not contain null, empty, or blank elements)
  - `expr` (`String`) — the WHERE condition as a raw SQL string (must not be null; may be empty for no filter condition)
- **Returns:** a SubQuery
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery(Product.class,
        Arrays.asList("id", "price"),
        "category = 'electronics' AND in_stock = true");
    // Generates: SELECT id, price FROM Product WHERE category = 'electronics' AND in_stock = true
    ```
- **See also:** #subQuery(String, Collection, String)
- **Signature:** `public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a SubQuery from an entity name with selected properties and condition.
- **Contract:**
  - If condition is not already a Criteria or a clause (such as Where), it is automatically wrapped in a WHERE clause at construction time; condition() on the returned subquery returns the wrapping Where.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name (must not be null, empty, or blank)
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be null or empty, and must not contain null, empty, or blank elements)
  - `condition` (`Condition`) — the WHERE condition for the subquery; may be null for no WHERE clause (a blank SqlExpression condition is likewise treated as no filter condition)
- **Returns:** a SubQuery
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("users",
        Arrays.asList("id", "email"),
        Filters.like("email", "%@company.com"));
    // Generates: SELECT id, email FROM users WHERE email LIKE '%@company.com'
    ```
- **Signature:** `public static SubQuery subQuery(final String entityName, final String propName, final Condition condition)`
- **Summary:** Creates a structured single-property subquery for an entity name.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name (must not be null, empty, or blank)
  - `propName` (`String`) — the property to select (must not be null, empty, or blank)
  - `condition` (`Condition`) — the optional query condition; may be null
- **Returns:** a structured subquery
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("users", "id", Filters.equal("active", true));
    // Generates: SELECT id FROM users WHERE active = true
    ```
- **Signature:** `public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final String expr)`
- **Summary:** Creates a SubQuery from an entity name with selected properties and a raw SQL condition string.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name (must not be null, empty, or blank)
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be null or empty, and must not contain null, empty, or blank elements)
  - `expr` (`String`) — the WHERE condition as a raw SQL string (must not be null; may be empty for no filter condition)
- **Returns:** a SubQuery
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("products",
        Arrays.asList("id", "price"),
        "category = 'electronics' AND in_stock = true");
    // Generates: SELECT id, price FROM products WHERE category = 'electronics' AND in_stock = true
    ```
- **Signature:** `@Deprecated public static SubQuery subQuery(final String entityName, final String sql)`
- **Summary:** Creates a SubQuery from an entity name and raw SQL.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name
  - `sql` (`String`) — the complete SQL for the subquery (must not be null, empty, or blank)
- **Returns:** a SubQuery
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("orders",
        "SELECT COUNT(*) FROM orders WHERE user_id = ?");
    // Generates: SELECT COUNT(*) FROM orders WHERE user_id = ?   (entityName is ignored when full SQL is supplied)
    ```
- **See also:** #subQuery(String)
- **Signature:** `public static SubQuery subQuery(final String sql)`
- **Summary:** Creates a SubQuery from raw SQL.
- **Parameters:**
  - `sql` (`String`) — the complete SQL for the subquery (must not be null, empty, or blank)
- **Returns:** a SubQuery
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery(
        "SELECT user_id FROM orders WHERE total > 1000 GROUP BY user_id"
    );
    // Generates: SELECT user_id FROM orders WHERE total > 1000 GROUP BY user_id
    ```
##### limit(...) -> Limit
- **Signature:** `public static Limit limit(final int count)`
- **Summary:** Creates a Limit clause to restrict the number of rows returned.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must be non-negative)
- **Returns:** a Limit clause
- **Examples:**
  - ```java
    Limit limit = Filters.limit(10);
    // Results in SQL like: LIMIT 10
    ```
- **Signature:** `public static Limit limit(final int count, final int offset)`
- **Summary:** Creates a Limit clause with a count and offset.
- **Contract:**
  - When offset == 0, the rendered SQL omits the OFFSET clause.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must be non-negative)
  - `offset` (`int`) — the number of rows to skip (must be non-negative)
- **Returns:** a Limit clause
- **Examples:**
  - ```java
    Limit limit = Filters.limit(20, 10);
    // Results in SQL like: LIMIT 20 OFFSET 10 (skip 10, take 20)
    ```
- **Signature:** `public static Limit limit(final String expr)`
- **Summary:** Creates a LIMIT clause from a string expression, formatting and validating it against a fixed grammar.
- **Contract:**
  - The expression is trimmed, its internal whitespace collapsed, and its SQL keywords upper-cased (parameter names left intact); a "LIMIT " prefix is prepended when it begins with a digit, '?', ':', or "#{" .
  - It must be one of LIMIT count, LIMIT count OFFSET offset, MySQL's LIMIT offset, count, or the SQL:2008 \[OFFSET offset ROWS\] FETCH NEXT/FIRST count ROWS ONLY forms \\u2014 where each number is an integer or a ?
  - When the condition is rendered by a SQL builder, a parsed expression is emitted in the target dialect's pagination syntax (so MySQL's comma form and the FETCH forms are re-rendered per dialect).
  - An opaque (placeholder) expression is re-rendered in the dialect's FETCH syntax only when the dialect paginates with OFFSET/FETCH (Oracle, DB2 or SQL Server, per SqlDialect.ProductInfo) and it is a generic LIMIT count \[OFFSET offset\] form; otherwise it is emitted verbatim.
- **Parameters:**
  - `expr` (`String`) — the limit expression as a string (must not be null, empty, or blank, and must match one of the accepted forms)
- **Returns:** a Limit clause
- **Examples:**
  - ```java
    Limit limit = Filters.limit("10 OFFSET 20");
    // Renders as: LIMIT 10 OFFSET 20
    ```

#### Public Instance Methods
- (none)

### Class ParsedSql (com.landawn.abacus.query.ParsedSql)
Represents a parsed SQL statement with support for named parameters and parameterized queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### parse(...) -> ParsedSql
- **Signature:** `public static ParsedSql parse(final String sql)`
- **Summary:** Parses the given SQL string and returns a ParsedSql instance.
- **Contract:**
  - Parameter conversion is only applied when the SQL is a recognized data operation statement (see the class-level documentation).
- **Parameters:**
  - `sql` (`String`) — the SQL string to parse (must not be null, empty, or blank)
- **Returns:** a ParsedSql instance containing the parsed information
- **Examples:**
  - ```java
    // Using named parameters
    ParsedSql ps1 = ParsedSql.parse("SELECT * FROM users WHERE id = :userId");
    System.out.println(ps1.parameterizedSql());   // "SELECT * FROM users WHERE id = ?"
    
    // Using iBatis/MyBatis style
    ParsedSql ps2 = ParsedSql.parse("INSERT INTO users (name, email) VALUES (#{name}, #{email})");
    System.out.println(ps2.namedParameters());   // ["name", "email"]
    
    // Using standard JDBC placeholders
    ParsedSql ps3 = ParsedSql.parse("UPDATE users SET status = ? WHERE id = ?");
    System.out.println(ps3.parameterCount());   // 2
    ```

#### Public Instance Methods
##### originalSql(...) -> String
- **Signature:** `public String originalSql()`
- **Summary:** Returns the original SQL string (trimmed of leading and trailing whitespace), before any parameter conversion or processing.
- **Parameters:**
  - (none)
- **Returns:** the trimmed original SQL string
- **Examples:**
  - ```java
    ParsedSql parsed = ParsedSql.parse("  SELECT * FROM users WHERE id = :userId  ");
    String sql = parsed.originalSql();   // Returns: "SELECT * FROM users WHERE id = :userId"
    ```
##### parameterizedSql(...) -> String
- **Signature:** `public String parameterizedSql()`
- **Summary:** Gets the parameterized SQL with named parameters replaced by JDBC placeholders (?) for recognized data-operation statements.
- **Parameters:**
  - (none)
- **Returns:** the parameterized SQL string with ? placeholders
- **Examples:**
  - ```java
    ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :userId AND status = :status");
    String sql = parsed.parameterizedSql();
    // Returns: "SELECT * FROM users WHERE id = ? AND status = ?"
    
    // Use with PreparedStatement
    PreparedStatement stmt = connection.prepareStatement(parsed.parameterizedSql());
    stmt.setLong(1, userId);
    stmt.setString(2, status);
    ```
##### namedParameters(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> namedParameters()`
- **Summary:** Gets the list of named parameters extracted from the SQL in order of appearance.
- **Contract:**
  - Returns an empty list if the SQL has no named parameters, or if the SQL is not a recognized data operation statement (see the class-level documentation), in which case no parameter extraction is performed.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter names
- **Examples:**
  - ```java
    ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE name = :name AND age > :minAge");
    ImmutableList<String> params = parsed.namedParameters();
    // Returns: ["name", "minAge"]
    
    // SQL with no named parameters returns empty list
    ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users WHERE id = ?");
    ImmutableList<String> params2 = parsed2.namedParameters();
    // Returns: []
    ```
##### parameterCount(...) -> int
- **Signature:** `public int parameterCount()`
- **Summary:** Gets the total number of parameters (named or positional) in the SQL.
- **Parameters:**
  - (none)
- **Returns:** the number of parameters in the SQL
- **Examples:**
  - ```java
    ParsedSql parsed = ParsedSql.parse("INSERT INTO users (name, email, age) VALUES (:name, :email, :age)");
    int count = parsed.parameterCount();
    // Returns: 3
    
    ParsedSql parsed2 = ParsedSql.parse("SELECT * FROM users");
    int count2 = parsed2.parameterCount();
    // Returns: 0
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code value for this ParsedSql.
- **Parameters:**
  - (none)
- **Returns:** the hash code value
- **Examples:**
  - ```java
    ParsedSql a = ParsedSql.parse("SELECT * FROM users WHERE id = :id");
    ParsedSql b = ParsedSql.parse("  SELECT * FROM users WHERE id = :id  ");
    // Both trim to the same original SQL, so their hash codes match
    boolean sameHash = a.hashCode() == b.hashCode();   // true
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Indicates whether some other object is "equal to" this one.
- **Contract:**
  - Two ParsedSql objects are equal if their trimmed original SQL strings (as returned by #originalSql()) are equal.
- **Parameters:**
  - `obj` (`Object`) — the reference object with which to compare
- **Returns:** true if this object equals the obj argument; false otherwise
- **Examples:**
  - ```java
    ParsedSql a = ParsedSql.parse("SELECT * FROM users WHERE id = :id");
    ParsedSql b = ParsedSql.parse("  SELECT * FROM users WHERE id = :id  ");
    ParsedSql c = ParsedSql.parse("SELECT * FROM users WHERE id = :otherId");
    
    boolean eq = a.equals(b);                    // true (same trimmed original SQL)
    boolean ne = a.equals(c);                    // false (different parameter name)
    boolean notString = a.equals("SELECT ...");  // false (not a ParsedSql)
    ```
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this ParsedSql.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this object
- **Examples:**
  - ```java
    ParsedSql parsed = ParsedSql.parse("SELECT * FROM users WHERE id = :id");
    String s = parsed.toString();
    // "{sql=SELECT * FROM users WHERE id = :id, parameterizedSql=SELECT * FROM users WHERE id = ?}"
    ```

### Class QueryUtil (com.landawn.abacus.query.QueryUtil)
Utility class for handling database query operations, entity-column mappings, and SQL generation helpers.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### propToColumnInfoMap(...) -> ImmutableMap<String, ColumnInfo>
- **Signature:** `@Beta @Internal public static ImmutableMap<String, ColumnInfo> propToColumnInfoMap(final Class<?> entityClass, final NamingPolicy namingPolicy)`
- **Summary:** Returns column information keyed by both property names and mapped column names.
- **Contract:**
  - The ColumnInfo#isUnqualified() flag describes the mapped column value: it is true when the column name contains no '.' character.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `namingPolicy` (`NamingPolicy`) — the naming policy to use for column name conversion. If null, defaults to NamingPolicy.SNAKE_CASE.
- **Returns:** an immutable map containing property-name keys and, when a mapped column name is not already a property-name key, an additional column-name key. Each value contains the mapped column name and whether that column name has no dot.
- **Examples:**
  - ```java
    // Get property-to-column details
    ImmutableMap<String, QueryUtil.ColumnInfo> columnInfoMap =
        QueryUtil.propToColumnInfoMap(User.class, NamingPolicy.SNAKE_CASE);
    
    QueryUtil.ColumnInfo result = columnInfoMap.get("firstName");
    String columnName = result.columnName(); // "first_name"
    boolean unqualified = result.isUnqualified(); // true
    
    // Nested property example
    QueryUtil.ColumnInfo nested = columnInfoMap.get("address.street");
    String nestedColumn = nested.columnName(); // e.g. "addr.street"
    boolean nestedUnqualified = nested.isUnqualified(); // false
    ```
- **See also:** #propToColumnNameMap(Class, NamingPolicy)
##### columnToPropNameMap(...) -> ImmutableMap<String, String>
- **Signature:** `@Internal public static ImmutableMap<String, String> columnToPropNameMap(final Class<?> entityClass)`
- **Summary:** Returns a mapping of column names to property names for the specified entity class.
- **Contract:**
  - This method is useful when you need to map database result set columns back to entity properties, especially when dealing with case-insensitive database systems or when column names don't match the exact case in your code.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
- **Returns:** an immutable map of column names (including upper- and lower-case variations) to property names
- **Examples:**
  - ```java
    // Given an entity class with @Column annotations
    ImmutableMap<String, String> columnToProp = QueryUtil.columnToPropNameMap(User.class);
    
    // If User has @Column("User_Name") on property "userName":
    String propName  = columnToProp.get("User_Name");   // "userName" (looked up by original column name)
    String propName2 = columnToProp.get("USER_NAME");   // "userName" (looked up by uppercase variant)
    String propName3 = columnToProp.get("user_name");   // "userName" (looked up by lowercase variant)
    ```
##### propToColumnNameMap(...) -> ImmutableMap<String, String>
- **Signature:** `@Internal public static ImmutableMap<String, String> propToColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy)`
- **Summary:** Returns a mapping of property names to column names for the specified entity class using the given naming policy.
- **Contract:**
  - The naming policy determines how property names are converted to column names when no explicit @Column annotation is present.
  - "addr.street" when the Address entity declares an alias "addr").
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (may be null)
  - `namingPolicy` (`NamingPolicy`) — the naming policy to use for column name conversion. If null, defaults to NamingPolicy.SNAKE_CASE.
- **Returns:** an immutable map of property names to column names, or an empty immutable map if entityClass is null or is a Map type
- **Examples:**
  - ```java
    // Build property-to-column mapping with SNAKE_CASE naming policy
    ImmutableMap<String, String> propToColumn = QueryUtil.propToColumnNameMap(User.class, NamingPolicy.SNAKE_CASE);
    
    // If User has property "firstName" without @Column annotation:
    String columnName = propToColumn.get("firstName");   // "first_name"
    
    // With SCREAMING_SNAKE_CASE naming policy
    ImmutableMap<String, String> propToColumnUpper =
        QueryUtil.propToColumnNameMap(User.class, NamingPolicy.SCREAMING_SNAKE_CASE);
    String upperColumn = propToColumnUpper.get("firstName");   // "FIRST_NAME"
    ```
##### insertPropNames(...) -> ImmutableList<String>
- **Signature:** `@Internal public static ImmutableList<String> insertPropNames(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for INSERT operations on the given entity instance.
- **Contract:**
  - The method intelligently handles ID fields: If all ID fields have default values (e.g.
  - If any ID field has a non-default value, all insertable properties including IDs are returned.
- **Parameters:**
  - `entity` (`Object`) — the entity instance to analyze (must not be null)
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; null or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for INSERT operations
- **Examples:**
  - ```java
    User user = new User();
    user.setName("John");
    user.setEmail("john@example.com");
    // user.id is at its default value (e.g. null for Long, 0 for long primitive)
    // so the "id" property will be excluded.
    
    Collection<String> insertProps = QueryUtil.insertPropNames(user, null);
    // Returns: ["name", "email", ...] (excludes "id" since it has default value)
    
    // With excluded properties
    Set<String> excluded = N.asSet("email");
    Collection<String> filteredProps = QueryUtil.insertPropNames(user, excluded);
    // Returns: ["name", ...] (excludes both "id" and "email")
    ```
- **Signature:** `@Internal public static ImmutableList<String> insertPropNames(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for INSERT operations on the given entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; null or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for INSERT operations
- **Examples:**
  - ```java
    // Get all insertable property names for a class
    Collection<String> insertProps = QueryUtil.insertPropNames(User.class, null);
    // Returns: ["id", "name", "email", "createdDate", ...]
    
    // Exclude specific properties
    Set<String> excluded = N.asSet("createdDate", "updatedDate");
    Collection<String> filteredProps = QueryUtil.insertPropNames(User.class, excluded);
    // Returns: ["id", "name", "email", ...]
    ```
##### selectPropNames(...) -> ImmutableList<String>
- **Signature:** `@Internal public static ImmutableList<String> selectPropNames(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for SELECT operations on the given entity class.
- **Contract:**
  - When includeSubEntityProperties is true, the method returns nested properties using dot notation (e.g., "address.street").
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `includeSubEntityProperties` (`boolean`) — true to include nested entity properties, false for top-level only
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude (nullable). When sub-entity properties are included, excluding a root such as "address" also excludes descendants such as "address.street".
- **Returns:** an immutable list of property names suitable for SELECT operations
- **Examples:**
  - ```java
    // Get top-level properties only
    Collection<String> selectProps = QueryUtil.selectPropNames(User.class, false, null);
    // Returns: ["id", "name", "email", ...]
    
    // Include sub-entity properties (e.g., nested Address)
    Collection<String> allProps = QueryUtil.selectPropNames(User.class, true, null);
    // Returns: ["id", "name", "email", "address.street", "address.city", ...]
    
    // Exclude specific properties
    Set<String> excluded = N.asSet("email");
    Collection<String> filteredProps = QueryUtil.selectPropNames(User.class, false, excluded);
    // Returns: ["id", "name", ...]
    ```
- **Signature:** `@Internal public static ImmutableList<String> selectPropNames(final Object entity, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for SELECT operations on the given entity instance.
- **Parameters:**
  - `entity` (`Object`) — the entity instance to analyze (must not be null)
  - `includeSubEntityProperties` (`boolean`) — true to include nested entity properties, false for top-level only
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; null or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for SELECT operations
- **Examples:**
  - ```java
    User user = new User();
    Collection<String> selectProps = QueryUtil.selectPropNames(user, false, null);
    // Returns: ["id", "name", "email", ...]
    ```
- **See also:** #selectPropNames(Class, boolean, Set)
##### updatePropNames(...) -> ImmutableList<String>
- **Signature:** `@Internal public static ImmutableList<String> updatePropNames(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for UPDATE operations on the given entity class.
- **Contract:**
  - Properties are considered non-updatable if they are: Annotated with @ReadOnly, @ReadOnlyId, or otherwise marked as a read-only id property Annotated with @NonUpdatable Excluded from column mapping (e.g.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; null or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for UPDATE operations
- **Examples:**
  - ```java
    // Account declares a plain read-write @Id "id", so it remains updatable.
    Collection<String> updateProps = QueryUtil.updatePropNames(Account.class, null);
    // returns ["id", "gui", "emailAddress", "firstName", ...] (a plain @Id is NOT excluded)
    
    // Exclude additional properties explicitly.
    Collection<String> filteredProps = QueryUtil.updatePropNames(Account.class, N.asSet("createTime"));
    // returns the same list without "createTime"
    ```
- **Signature:** `@Internal public static ImmutableList<String> updatePropNames(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for UPDATE operations on the given entity instance.
- **Parameters:**
  - `entity` (`Object`) — the entity instance to analyze (must not be null)
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; null or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for UPDATE operations
- **Examples:**
  - ```java
    Account account = new Account();
    Collection<String> updateProps = QueryUtil.updatePropNames(account, null);
    // Returns the same names as updatePropNames(Account.class, null)
    ```
- **See also:** #updatePropNames(Class, Set)
##### idPropNames(...) -> ImmutableList<String>
- **Signature:** `@Internal @Immutable public static ImmutableList<String> idPropNames(final Class<?> entityClass)`
- **Summary:** Returns the ID property names for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
- **Returns:** an immutable list of ID property names, or an empty list if no ID properties are defined
- **Examples:**
  - ```java
    // Get ID property names for an entity with @Id annotation
    List<String> idFields = QueryUtil.idPropNames(User.class);
    // Returns: ["id"] for a class with @Id on the "id" property
    
    // Composite key example
    List<String> compositeIds = QueryUtil.idPropNames(OrderItem.class);
    // Returns: ["orderId", "itemId"] for a composite key entity
    
    // Entity without @Id returns empty list
    List<String> noIds = QueryUtil.idPropNames(LogEntry.class);
    // Returns: []
    ```
##### isNonColumn(...) -> boolean
- **Signature:** `@Internal public static boolean isNonColumn(final Set<String> columnFields, final Set<String> nonColumnFields, final PropInfo propInfo)`
- **Summary:** Determines whether a property should be excluded from database column mapping.
- **Contract:**
  - Determines whether a property should be excluded from database column mapping.
  - A property is not a column if it's transient, annotated with @NonColumn, or excluded by @Table configuration.
- **Parameters:**
  - `columnFields` (`Set<String>`) — set of field names explicitly included as columns (typically derived from Table#columnFields(); may be null or empty for no whitelist)
  - `nonColumnFields` (`Set<String>`) — set of field names explicitly excluded as columns (typically derived from Table#nonColumnFields(); may be null or empty for no blacklist)
  - `propInfo` (`PropInfo`) — the property information to check (must not be null)
- **Returns:** true if the property should not be mapped to a database column
- **Examples:**
  - ```java
    BeanInfo beanInfo = ParserUtil.getBeanInfo(User.class);
    PropInfo propInfo = beanInfo.getPropInfo("tempField");
    
    // Check if a property is excluded from column mapping
    Set<String> columnFields = N.asSet("id", "name", "email");
    Set<String> nonColumnFields = N.emptySet();
    boolean excluded = QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo);
    // Returns true if "tempField" is not in columnFields
    
    // Check with nonColumnFields
    Set<String> nonColumns = N.asSet("tempField", "transientData");
    boolean excluded2 = QueryUtil.isNonColumn(N.emptySet(), nonColumns, propInfo);
    // Returns true if "tempField" is in nonColumnFields
    ```
##### placeholders(...) -> String
- **Signature:** `public static String placeholders(final int placeholderCount)`
- **Summary:** Generates a string of question marks (?) repeated placeholderCount times with comma-space delimiter (", ").
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to generate (must not be negative)
- **Returns:** a string containing placeholderCount question marks separated by ", ", or empty string if placeholderCount is 0
- **Examples:**
  - ```java
    String placeholders = QueryUtil.placeholders(3);
    // Returns: "?, ?, ?"
    String sql = "INSERT INTO users (name, email, age) VALUES (" + placeholders + ")";
    // Result: "INSERT INTO users (name, email, age) VALUES (?, ?, ?)"
    ```
##### tableAlias(...) -> String
- **Signature:** `@Internal public static String tableAlias(final Class<?> entityClass)`
- **Summary:** Returns the table alias from the @Table annotation on the entity class.
- **Contract:**
  - If no @Table annotation exists, this method returns null.
  - If @Table is present but the alias attribute is not specified, this method returns an empty string.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to check (must not be null)
- **Returns:** the table alias if defined in @Table annotation, empty string if @Table is present but alias is not set, or null if no @Table annotation exists
- **Examples:**
  - ```java
    // Given: @Table(name = "users", alias = "u") on User class
    String alias = QueryUtil.tableAlias(User.class);
    // Returns: "u"
    
    // Given: @Table(name = "orders") on Order class (no alias)
    String alias2 = QueryUtil.tableAlias(Order.class);
    // Returns: "" (empty string when alias is not specified)
    
    // Given: no @Table annotation on LogEntry class
    String alias3 = QueryUtil.tableAlias(LogEntry.class);
    // Returns: null
    ```
##### tableNameAndAlias(...) -> String
- **Signature:** `@Internal public static String tableNameAndAlias(final Class<?> entityClass)`
- **Summary:** Returns the table name and optional alias for the entity class using the default naming policy.
- **Contract:**
  - If @Table annotation is present, uses its values; otherwise derives the table name from the class name using NamingPolicy.SNAKE_CASE.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
- **Returns:** the table name, optionally followed by space and alias
- **Examples:**
  - ```java
    // Given: @Table(name = "users", alias = "u") on User class
    String tableAndAlias = QueryUtil.tableNameAndAlias(User.class);
    // Returns: "users u"
    
    // Given: @Table(name = "orders") on Order class (no alias)
    String tableOnly = QueryUtil.tableNameAndAlias(Order.class);
    // Returns: "orders"
    
    // Given: no @Table annotation on MyEntity class
    String derived = QueryUtil.tableNameAndAlias(MyEntity.class);
    // Returns: "my_entity" (derived from class name using SNAKE_CASE)
    ```
- **Signature:** `@Internal public static String tableNameAndAlias(final Class<?> entityClass, final NamingPolicy namingPolicy)`
- **Summary:** Returns the table name and optional alias for the entity class using the specified naming policy.
- **Contract:**
  - The table name is resolved the same way the query builders resolve it: from the @Table annotation (name or its deprecated value() alias) or a JPA javax.persistence/jakarta.persistence @Table annotation; only when no annotated name exists is the table name derived from the class name using the provided naming policy.
  - The alias comes from Table#alias() and is appended after a space when present.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `namingPolicy` (`NamingPolicy`) — the naming policy used when no annotated table name exists. If null, defaults to NamingPolicy.SNAKE_CASE.
- **Returns:** the table name, optionally followed by space and alias
- **Examples:**
  - ```java
    // Given: @Table(name = "users", alias = "u") - annotation takes priority
    String tableAndAlias = QueryUtil.tableNameAndAlias(User.class, NamingPolicy.SCREAMING_SNAKE_CASE);
    // Returns: "users u" (annotation values used as-is, naming policy ignored)
    
    // Given: no @Table annotation on MyEntity class
    String snakeCase = QueryUtil.tableNameAndAlias(MyEntity.class, NamingPolicy.SNAKE_CASE);
    // Returns: "my_entity"
    
    String upperCase = QueryUtil.tableNameAndAlias(MyEntity.class, NamingPolicy.SCREAMING_SNAKE_CASE);
    // Returns: "MY_ENTITY"
    ```

#### Public Instance Methods
- (none)

### Record ColumnInfo (com.landawn.abacus.query.QueryUtil.ColumnInfo)
Describes the database column associated with a property or column lookup key.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Selection (com.landawn.abacus.query.Selection)
Immutable selection specification for SQL queries, particularly useful for complex multi-table selections.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### builder(...) -> SelectionBuilder
- **Signature:** `public static SelectionBuilder builder(final Class<?> entityClass)`
- **Summary:** Creates a builder with the required entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select; must not be null
- **Returns:** a new selection builder

#### Public Instance Methods
##### entityClass(...) -> Class<?>
- **Signature:** `public Class<?> entityClass()`
- **Summary:** Returns the entity class to select.
- **Parameters:**
  - (none)
- **Returns:** the entity class, never null
##### tableAlias(...) -> String
- **Signature:** `public String tableAlias()`
- **Summary:** Returns the table alias.
- **Parameters:**
  - (none)
- **Returns:** the table alias, or null if none was specified
##### classAlias(...) -> String
- **Signature:** `public String classAlias()`
- **Summary:** Returns the result class alias.
- **Parameters:**
  - (none)
- **Returns:** the class alias, or null if none was specified
##### includedPropNames(...) -> Collection<String>
- **Signature:** `public Collection<String> includedPropNames()`
- **Summary:** Returns the property names to include in this selection.
- **Parameters:**
  - (none)
- **Returns:** the immutable included property names; null or an empty collection means the projection is derived from the entity class
##### includesSubEntityProperties(...) -> boolean
- **Signature:** `public boolean includesSubEntityProperties()`
- **Summary:** Returns whether properties from sub-entities are included.
- **Parameters:**
  - (none)
- **Returns:** true if sub-entity properties are included
##### excludedPropNames(...) -> Set<String>
- **Signature:** `public Set<String> excludedPropNames()`
- **Summary:** Returns the property names to exclude from this selection.
- **Parameters:**
  - (none)
- **Returns:** the immutable excluded property names, or null if no properties are excluded
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Parameters:**
  - (none)
- **Returns:** unspecified
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Parameters:**
  - `obj` (`Object`)
- **Returns:** unspecified
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Parameters:**
  - (none)
- **Returns:** unspecified

### Class SelectionBuilder (com.landawn.abacus.query.Selection.SelectionBuilder)
Builder for immutable Selection instances.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### tableAlias(...) -> SelectionBuilder
- **Signature:** `public SelectionBuilder tableAlias(final String tableAlias)`
- **Summary:** Sets the table alias.
- **Parameters:**
  - `tableAlias` (`String`) — the table alias
- **Returns:** this builder
##### classAlias(...) -> SelectionBuilder
- **Signature:** `public SelectionBuilder classAlias(final String classAlias)`
- **Summary:** Sets the result class alias.
- **Parameters:**
  - `classAlias` (`String`) — the class alias
- **Returns:** this builder
##### includedPropNames(...) -> SelectionBuilder
- **Signature:** `public SelectionBuilder includedPropNames(final Collection<String> includedPropNames)`
- **Summary:** Sets the property names to include.
- **Parameters:**
  - `includedPropNames` (`Collection<String>`) — the included property names
- **Returns:** this builder
##### includeSubEntityProperties(...) -> SelectionBuilder
- **Signature:** `public SelectionBuilder includeSubEntityProperties(final boolean includeSubEntityProperties)`
- **Summary:** Sets whether properties from sub-entities are included.
- **Parameters:**
  - `includeSubEntityProperties` (`boolean`) — the inclusion flag
- **Returns:** this builder
##### excludedPropNames(...) -> SelectionBuilder
- **Signature:** `public SelectionBuilder excludedPropNames(final Set<String> excludedPropNames)`
- **Summary:** Sets the property names to exclude.
- **Parameters:**
  - `excludedPropNames` (`Set<String>`) — the excluded property names
- **Returns:** this builder
##### build(...) -> Selection
- **Signature:** `public Selection build()`
- **Summary:** Builds an immutable selection, defensively copying its property collections.
- **Parameters:**
  - (none)
- **Returns:** the new selection

### Enum SortDirection (com.landawn.abacus.query.SortDirection)
Enumeration representing the sort direction for database queries and collections.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### of(...) -> SortDirection
- **Signature:** `public static SortDirection of(final String name)`
- **Summary:** Returns the SortDirection corresponding to the given name.
- **Parameters:**
  - `name` (`String`) — the sort direction name to look up (case-insensitive); may be null
- **Returns:** the matching SortDirection, or null if name is null or does not name a recognized direction
- **Examples:**
  - ```java
    SortDirection asc = SortDirection.of("asc");     // ASC
    SortDirection desc = SortDirection.of("DESC");   // DESC
    SortDirection none = SortDirection.of("up");     // null (not recognized)
    SortDirection nil = SortDirection.of(null);      // null
    ```

#### Public Instance Methods
##### isAscending(...) -> boolean
- **Signature:** `public boolean isAscending()`
- **Summary:** Checks if this sort direction is ascending.
- **Contract:**
  - Checks if this sort direction is ascending.
- **Parameters:**
  - (none)
- **Returns:** true if this is ASC, false if DESC
- **Examples:**
  - ```java
    SortDirection dir = SortDirection.ASC;
    boolean ascending = dir.isAscending();   // true
    ```
##### isDescending(...) -> boolean
- **Signature:** `public boolean isDescending()`
- **Summary:** Checks if this sort direction is descending.
- **Contract:**
  - Checks if this sort direction is descending.
- **Parameters:**
  - (none)
- **Returns:** true if this is DESC, false if ASC
- **Examples:**
  - ```java
    SortDirection dir = SortDirection.DESC;
    boolean descending = dir.isDescending();   // true
    ```
##### opposite(...) -> SortDirection
- **Signature:** `public SortDirection opposite()`
- **Summary:** Returns the opposite sort direction.
- **Parameters:**
  - (none)
- **Returns:** #DESC if this is #ASC, otherwise #ASC
- **Examples:**
  - ```java
    SortDirection reversed = SortDirection.ASC.opposite();    // DESC
    SortDirection original = reversed.opposite();             // ASC
    ```

### Class SqlBuilder (com.landawn.abacus.query.SqlBuilder)
A fluent SQL builder that extends AbstractQueryBuilder with concrete SQL generation, including condition rendering, operator handling, and NULL semantics.

**Thread-safety:** not-thread-safe
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class SqlDialect (com.landawn.abacus.query.SqlDialect)
Immutable configuration object used by Dsl to render generated SQL.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Enum IdentifierQuote (com.landawn.abacus.query.SqlDialect.IdentifierQuote)
Identifier quoting style used by SQL builders.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Enum SqlPolicy (com.landawn.abacus.query.SqlDialect.SqlPolicy)
Defines how values supplied to query builders are represented in generated SQL.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Record ProductInfo (com.landawn.abacus.query.SqlDialect.ProductInfo)
Immutable descriptor of a database product, holding the product name and version separately.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public ProductInfo` — Canonical constructor that requires a nonblank product name and normalizes a null version to an empty string, so #version() never returns null and an absent version has a single canonical representation.

#### Public Static Methods
##### of(...) -> ProductInfo
- **Signature:** `public static ProductInfo of(final String name)`
- **Summary:** Creates a ProductInfo with the given product name and no version (an empty #version()).
- **Parameters:**
  - `name` (`String`) — the database product name, such as "Oracle" or "MySQL"
- **Returns:** a new ProductInfo with the given name and an empty ("") version
- **Signature:** `public static ProductInfo of(final String name, final String version)`
- **Summary:** Creates a ProductInfo with the given product name and version.
- **Parameters:**
  - `name` (`String`) — the database product name, such as "Oracle" or "MySQL"
  - `version` (`String`) — the database product version, such as "19c" or "9.7"
- **Returns:** a new ProductInfo with the given name and version

#### Public Instance Methods
##### isMySQL(...) -> boolean
- **Signature:** `public boolean isMySQL()`
- **Summary:** Returns whether this descriptor names MySQL.
- **Parameters:**
  - (none)
- **Returns:** true if #name() contains "mysql" (case-insensitively)
##### isMariaDB(...) -> boolean
- **Signature:** `public boolean isMariaDB()`
- **Summary:** Returns whether this descriptor names MariaDB.
- **Parameters:**
  - (none)
- **Returns:** true if #name() contains "mariadb" (case-insensitively)
##### isPostgreSQL(...) -> boolean
- **Signature:** `public boolean isPostgreSQL()`
- **Summary:** Returns whether this descriptor names PostgreSQL.
- **Parameters:**
  - (none)
- **Returns:** true if #name() contains "postgres" (case-insensitively)
##### isSQLServer(...) -> boolean
- **Signature:** `public boolean isSQLServer()`
- **Summary:** Returns whether this descriptor names Microsoft SQL Server.
- **Parameters:**
  - (none)
- **Returns:** true if #name() contains "sql server" or "sqlserver" (case-insensitively)
##### isOracle(...) -> boolean
- **Signature:** `public boolean isOracle()`
- **Summary:** Returns whether this descriptor names Oracle Database.
- **Parameters:**
  - (none)
- **Returns:** true if #name() contains "oracle" (case-insensitively)
##### isDB2(...) -> boolean
- **Signature:** `public boolean isDB2()`
- **Summary:** Returns whether this descriptor names IBM DB2.
- **Parameters:**
  - (none)
- **Returns:** true if #name() contains "db2" (case-insensitively)
##### isH2(...) -> boolean
- **Signature:** `public boolean isH2()`
- **Summary:** Returns whether this descriptor names H2 Database.
- **Parameters:**
  - (none)
- **Returns:** true if #name() contains "h2" (case-insensitively)
##### isSQLite(...) -> boolean
- **Signature:** `public boolean isSQLite()`
- **Summary:** Returns whether this descriptor names SQLite.
- **Parameters:**
  - (none)
- **Returns:** true if #name() contains "sqlite" (case-insensitively)
##### isVersionAtLeast(...) -> boolean
- **Signature:** `public boolean isVersionAtLeast(final String minVersion)`
- **Summary:** Returns whether this product's #version() is greater than or equal to the given version.
- **Contract:**
  - Returns false (not comparable) when either this #version() or minVersion is null, blank, or does not begin with an integer component.
- **Parameters:**
  - `minVersion` (`String`) — the minimum version to compare against, such as "8.0" or "19"
- **Returns:** true if this product's version parses and is greater than or equal to minVersion; false otherwise, including when either version is not comparable
- **See also:** #isVersionAtMost(String)
##### isVersionAtMost(...) -> boolean
- **Signature:** `public boolean isVersionAtMost(final String maxVersion)`
- **Summary:** Returns whether this product's #version() is less than or equal to the given version.
- **Contract:**
  - Returns false (not comparable) when either this #version() or maxVersion is null, blank, or does not begin with an integer component.
- **Parameters:**
  - `maxVersion` (`String`) — the maximum version to compare against, such as "8.0" or "19"
- **Returns:** true if this product's version parses and is less than or equal to maxVersion; false otherwise, including when either version is not comparable
- **See also:** #isVersionAtLeast(String)

### Class SqlMapper (com.landawn.abacus.query.SqlMapper)
A utility class for managing SQL scripts stored in XML files and mapping them to short identifiers.

**Thread-safety:** not-thread-safe
**Nullability:** unspecified

#### Public Constructors
- `public SqlMapper()` — Creates an empty SqlMapper instance.

#### Public Static Methods
##### loadFrom(...) -> SqlMapper
- **Signature:** `public static SqlMapper loadFrom(final String filePaths)`
- **Summary:** Creates a SqlMapper instance by loading SQL definitions from one or more XML files.
- **Parameters:**
  - `filePaths` (`String`) — one or more file paths separated by ',' or ';' (must not be null or empty). Each path is resolved against the literal location first; when no file exists there, the common configuration directories are searched as a fallback
- **Returns:** a new SqlMapper instance loaded with SQL definitions from the specified files
- **Examples:**
  - ```java
    // Single file
    SqlMapper mapper = SqlMapper.loadFrom("config/sql-mapper.xml");
    
    // Multiple files
    SqlMapper mapper = SqlMapper.loadFrom("sql/users.xml,sql/orders.xml,sql/products.xml");
    // or
    SqlMapper mapper = SqlMapper.loadFrom("sql/users.xml;sql/orders.xml;sql/products.xml");
    ```
- **Signature:** `public static SqlMapper loadFrom(final String firstFilePath, final String... additionalFilePaths)`
- **Summary:** Creates a SqlMapper by loading separately supplied file paths.
- **Parameters:**
  - `firstFilePath` (`String`) — the first XML mapper path; must not be null or empty
  - `additionalFilePaths` (`String[]`) — additional XML mapper paths; no element may be null or empty
- **Returns:** a new mapper containing definitions from every supplied path
- **Signature:** `public static SqlMapper loadFrom(final File... files)`
- **Summary:** Creates a SqlMapper instance by loading SQL definitions from one or more XML files.
- **Contract:**
  - Each file must contain a <sqlMapper> root element; definitions from all files are merged into a single mapper.
- **Parameters:**
  - `files` (`File[]`) — one or more XML files to load (must not be null or empty, and no element may be null)
- **Returns:** a new SqlMapper instance loaded with SQL definitions from the specified files
- **Examples:**
  - ```java
    SqlMapper mapper = SqlMapper.loadFrom(new File("sql/users.xml"), new File("sql/orders.xml"));
    ```
- **Signature:** `public static SqlMapper loadFrom(final InputStream inputStream)`
- **Summary:** Creates a SqlMapper instance by loading SQL definitions from the supplied input stream.
- **Contract:**
  - The stream content must contain a <sqlMapper> root element.
- **Parameters:**
  - `inputStream` (`InputStream`) — the input stream to read the XML SQL definitions from (must not be null)
- **Returns:** a new SqlMapper instance loaded with SQL definitions from the stream
- **Examples:**
  - ```java
    try (InputStream is = new FileInputStream("sql/queries.xml")) {
        SqlMapper mapper = SqlMapper.loadFrom(is);
    }
    ```

#### Public Instance Methods
##### ids(...) -> ImmutableSet<String>
- **Signature:** `public ImmutableSet<String> ids()`
- **Summary:** Returns an immutable snapshot of all SQL identifiers registered in this mapper.
- **Parameters:**
  - (none)
- **Returns:** an immutable snapshot of all SQL identifiers in this mapper, maintaining insertion order
- **Examples:**
  - ```java
    SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
    ImmutableSet<String> ids = mapper.ids();
    ids.forEach(id -> System.out.println("Available SQL: " + id));
    ```
##### get(...) -> ParsedSql
- **Signature:** `public ParsedSql get(final String id)`
- **Summary:** Retrieves the parsed SQL associated with the specified identifier.
- **Parameters:**
  - `id` (`String`) — the SQL identifier to look up
- **Returns:** the ParsedSql object, or null if the id is null, empty, exceeds #MAX_ID_LENGTH characters, or is not found
- **Examples:**
  - ```java
    SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
    
    ParsedSql sql = mapper.get("findAccountById");
    if (sql != null) {
        String parameterizedSql = sql.parameterizedSql();
        // Use with PreparedStatement
        PreparedStatement stmt = connection.prepareStatement(parameterizedSql);
    }
    
    // Returns null for unknown ids
    ParsedSql unknown = mapper.get("nonExistentId");
    // unknown is null
    ```
##### containsId(...) -> boolean
- **Signature:** `public boolean containsId(final String id)`
- **Summary:** Returns true if this mapper contains an SQL registered under the specified identifier.
- **Contract:**
  - Returns true if this mapper contains an SQL registered under the specified identifier.
- **Parameters:**
  - `id` (`String`) — the SQL identifier to test
- **Returns:** true if a matching SQL is registered; false if the id is null, empty, exceeds #MAX_ID_LENGTH characters, or is not found
- **Examples:**
  - ```java
    SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
    boolean present = mapper.containsId("findAccountById");
    boolean absent = mapper.containsId("nonExistentId");   // false
    ```
- **See also:** #get(String)
##### attributes(...) -> ImmutableMap<String, String>
- **Signature:** `public ImmutableMap<String, String> attributes(final String id)`
- **Summary:** Retrieves the attributes associated with the specified SQL identifier.
- **Parameters:**
  - `id` (`String`) — the SQL identifier to look up
- **Returns:** an immutable map of attribute names to values, or null if the id is null, empty, exceeds #MAX_ID_LENGTH characters, or is not found
- **Examples:**
  - ```java
    // Given XML: <sql id="batchInsert" batchSize="100" timeout="30">...</sql>
    SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
    
    ImmutableMap<String, String> attrs = mapper.attributes("batchInsert");
    if (attrs != null) {
        String batchSize = attrs.get("batchSize");   // "100"
        String timeout = attrs.get("timeout");       // "30"
    }
    
    // Returns null for unknown ids
    ImmutableMap<String, String> unknown = mapper.attributes("nonExistentId");
    // unknown is null
    ```
##### add(...) -> void
- **Signature:** `public void add(final String id, final ParsedSql sql)`
- **Summary:** Adds a parsed SQL with the specified identifier.
- **Contract:**
  - This method validates the ID and throws an exception if an SQL with the same ID already exists.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed #MAX_ID_LENGTH characters)
  - `sql` (`ParsedSql`) — the parsed SQL to associate with the identifier (must not be null)
- **Examples:**
  - ```java
    SqlMapper mapper = new SqlMapper();
    ParsedSql parsedSql = ParsedSql.parse("select * from users where id = ?");
    mapper.add("findUserById", parsedSql);
    
    // Later, retrieve the SQL
    ParsedSql retrieved = mapper.get("findUserById");   // returns the same parsedSql instance just added
    ```
- **Signature:** `public void add(final String id, final ParsedSql sql, final Map<String, String> attributes)`
- **Summary:** Adds a parsed SQL with the specified identifier and attributes.
- **Contract:**
  - This method validates the ID and throws an exception if an SQL with the same ID already exists.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed #MAX_ID_LENGTH characters)
  - `sql` (`ParsedSql`) — the parsed SQL to associate with the identifier (must not be null)
  - `attributes` (`Map<String, String>`) — additional XML attributes for the SQL (e.g., batchSize, fetchSize, resultSetType, timeout); may be null or empty, but keys must be valid non-namespace XML attribute names and values must be non-null
- **Examples:**
  - ```java
    SqlMapper mapper = new SqlMapper();
    ParsedSql parsedSql = ParsedSql.parse("insert into users (id, name) values (?, ?)");
    Map<String, String> attrs = new HashMap<>();
    attrs.put("batchSize", "100");
    mapper.add("insertUser", parsedSql, attrs);
    ```
- **Signature:** `public void add(final String id, final String sql)`
- **Summary:** Adds a SQL string with the specified identifier and no attributes.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed #MAX_ID_LENGTH characters)
  - `sql` (`String`) — the SQL string to parse and store (must not be null or blank)
- **Examples:**
  - ```java
    SqlMapper mapper = new SqlMapper();
    mapper.add("findAll", "select * from users");
    ```
- **Signature:** `public void add(final String id, final String sql, final Map<String, String> attributes)`
- **Summary:** Adds a SQL string with the specified identifier and attributes.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed #MAX_ID_LENGTH characters)
  - `sql` (`String`) — the SQL string to parse and store (must not be null or blank)
  - `attributes` (`Map<String, String>`) — additional XML attributes for the SQL (e.g., batchSize, fetchSize, resultSetType, timeout); may be null or empty, but keys must be valid non-namespace XML attribute names and values must be non-null
- **Examples:**
  - ```java
    SqlMapper mapper = new SqlMapper();
    Map<String, String> attrs = new HashMap<>();
    attrs.put("batchSize", "100");
    attrs.put("timeout", "30");
    mapper.add("insertUser", "insert into users (id, name) values (?, ?)", attrs);
    ```
##### remove(...) -> void
- **Signature:** `public void remove(final String id)`
- **Summary:** Removes the SQL and its attributes associated with the specified identifier.
- **Contract:**
  - If the id is null, empty, exceeds #MAX_ID_LENGTH characters, or is not found, this method does nothing.
- **Parameters:**
  - `id` (`String`) — the SQL identifier to remove
- **Examples:**
  - ```java
    SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml");
    mapper.remove("deprecatedQuery");
    // Verify removal
    boolean removed = mapper.get("deprecatedQuery") == null;
    ```
##### copy(...) -> SqlMapper
- **Signature:** `public SqlMapper copy()`
- **Summary:** Creates a shallow copy of this SqlMapper instance.
- **Parameters:**
  - (none)
- **Returns:** a new SqlMapper instance with the same SQL definitions and attributes
- **Examples:**
  - ```java
    SqlMapper original = SqlMapper.loadFrom("sql/queries.xml");
    SqlMapper copy = original.copy();
    
    // Modifications to the copy do not affect the original
    copy.add("newQuery", ParsedSql.parse("SELECT 1"));
    boolean originalHasIt = original.get("newQuery") != null;  // false
    boolean copyHasIt = copy.get("newQuery") != null;          // true
    ```
##### saveTo(...) -> void
- **Signature:** `@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE") public void saveTo(final File file)`
- **Summary:** Saves all SQL definitions in this mapper to an XML file.
- **Contract:**
  - If the file already exists, it will be overwritten.
  - The canonical SQL identifier (the registered map key) is always written as the id attribute and is protected from being overridden: any stray id entry in a SQL's attributes map is ignored when emitting attributes.
- **Parameters:**
  - `file` (`File`) — the file to write to (will be created if it doesn't exist; parent directories will be created if needed)
- **Examples:**
  - ```java
    <sqlMapper>
        <sql id="findUser" fetchSize="100">select * from users where id = ?</sql>
        <sql id="updateUser">update users set name = ? where id = ?</sql>
    </sqlMapper>
    ```
  - ```java
    SqlMapper mapper = new SqlMapper();
    mapper.add("findUser", "select * from users where id = ?", Collections.emptyMap());
    mapper.saveTo(new File("sql/queries.xml"));
    ```
- **Signature:** `public void saveTo(final String filePath)`
- **Summary:** Writes this mapper to the specified file path.
- **Parameters:**
  - `filePath` (`String`) — the target file path; must not be null or empty
- **Signature:** `public void saveTo(final OutputStream outputStream)`
- **Summary:** Writes all SQL definitions in this mapper to the supplied output stream as XML.
- **Contract:**
  - The canonical SQL identifier (the registered map key) is always written as the id attribute and is protected from being overridden: any stray id entry in a SQL's attributes map is ignored when emitting attributes.
- **Parameters:**
  - `outputStream` (`OutputStream`) — the output stream to write to (not closed by this method)
- **Examples:**
  - ```java
    SqlMapper mapper = new SqlMapper();
    mapper.add("findUser", "select * from users where id = ?");
    try (OutputStream os = new FileOutputStream("sql/queries.xml")) {
        mapper.saveTo(os);
    }
    ```
##### size(...) -> int
- **Signature:** `public int size()`
- **Summary:** Returns the number of SQL definitions registered in this mapper.
- **Parameters:**
  - (none)
- **Returns:** the number of registered SQL definitions
- **Examples:**
  - ```java
    SqlMapper mapper = new SqlMapper();
    int empty = mapper.size();   // 0
    mapper.add("findAll", ParsedSql.parse("SELECT * FROM users"));
    int one = mapper.size();     // 1
    ```
##### isEmpty(...) -> boolean
- **Signature:** `public boolean isEmpty()`
- **Summary:** Checks if this mapper contains no SQL definitions.
- **Contract:**
  - Checks if this mapper contains no SQL definitions.
- **Parameters:**
  - (none)
- **Returns:** true if the mapper contains no SQL definitions, false otherwise
- **Examples:**
  - ```java
    SqlMapper emptyMapper = new SqlMapper();
    boolean empty = emptyMapper.isEmpty();  // true
    
    SqlMapper loadedMapper = SqlMapper.loadFrom("sql/queries.xml");
    boolean hasEntries = !loadedMapper.isEmpty();  // true (assuming file has SQL definitions)
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code value for this SqlMapper.
- **Parameters:**
  - (none)
- **Returns:** the hash code value
- **Examples:**
  - ```java
    SqlMapper a = new SqlMapper();
    a.add("q", ParsedSql.parse("SELECT 1"));
    SqlMapper b = new SqlMapper();
    b.add("q", ParsedSql.parse("SELECT 1"));
    // Equal mappers produce equal hash codes
    boolean sameHash = a.hashCode() == b.hashCode();   // true
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this SqlMapper to another object for equality.
- **Contract:**
  - Two SqlMapper instances are considered equal if they contain the same id-to-SQL mappings and id-to-attributes mappings (order-independent, per Map#equals(Object)).
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if obj is a SqlMapper whose internal SQL and attribute maps are equal to this mapper's; false otherwise
- **Examples:**
  - ```java
    SqlMapper a = new SqlMapper();
    a.add("q", ParsedSql.parse("SELECT 1"));
    SqlMapper b = new SqlMapper();
    b.add("q", ParsedSql.parse("SELECT 1"));
    SqlMapper c = new SqlMapper();
    c.add("q", ParsedSql.parse("SELECT 2"));
    
    boolean eq = a.equals(b);                // true (same id-to-SQL mappings)
    boolean ne = a.equals(c);                // false (different SQL for "q")
    boolean notMapper = a.equals("text");    // false (not a SqlMapper)
    ```
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this SqlMapper.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this SQL mapper
- **Examples:**
  - ```java
    SqlMapper mapper = new SqlMapper();
    String empty = mapper.toString();   // "{}"
    
    mapper.add("findUser", ParsedSql.parse("select * from users where id = ?"));
    String s = mapper.toString();       // contains "findUser" and the parsed SQL
    ```

### Enum SqlOperation (com.landawn.abacus.query.SqlOperation)
Enumeration representing SQL operation types.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### of(...) -> SqlOperation
- **Signature:** `public static SqlOperation of(final String name)`
- **Summary:** Retrieves the SqlOperation enum value corresponding to the given operation name.
- **Parameters:**
  - `name` (`String`) — the exact SQL operation token or enum constant name to look up (case-insensitive, without surrounding whitespace); may be null
- **Returns:** the corresponding SqlOperation enum value, or null if name is null or no matching operation is found
- **Examples:**
  - ```java
    SqlOperation selectOp = SqlOperation.of("SELECT");      // returns SELECT
    SqlOperation insertOp = SqlOperation.of("INSERT");      // returns INSERT
    SqlOperation mergeOp = SqlOperation.of("MERGE");        // returns MERGE
    
    // Case-insensitive; multi-word and enum constant names are accepted
    SqlOperation lower = SqlOperation.of("select");                  // returns SELECT
    SqlOperation tx = SqlOperation.of("begin transaction");          // returns BEGIN_TRANSACTION
    SqlOperation txByName = SqlOperation.of("BEGIN_TRANSACTION");    // returns BEGIN_TRANSACTION
    
    // Edge cases
    SqlOperation unsupported = SqlOperation.of("TRUNCATE");   // returns null (not supported)
    SqlOperation blank = SqlOperation.of("");                 // returns null
    SqlOperation nil = SqlOperation.of(null);                 // returns null
    ```
##### fromOrUnknown(...) -> SqlOperation
- **Signature:** `public static SqlOperation fromOrUnknown(final String token)`
- **Summary:** Resolves an operation token, returning #UNKNOWN for null or unsupported input.
- **Parameters:**
  - `token` (`String`) — the SQL operation token or enum constant name; may be null
- **Returns:** the resolved operation, or #UNKNOWN when no operation matches
- **Examples:**
  - ```java
    SqlOperation.fromOrUnknown("select");   // SELECT
    SqlOperation.fromOrUnknown("TRUNCATE"); // UNKNOWN
    SqlOperation.fromOrUnknown(null);        // UNKNOWN
    ```

#### Public Instance Methods
##### sqlToken(...) -> String
- **Signature:** `public String sqlToken()`
- **Summary:** Returns the SQL text representation of this operation.
- **Parameters:**
  - (none)
- **Returns:** the SQL keyword string representation of this operation, never null
- **Examples:**
  - ```java
    SqlOperation op = SqlOperation.SELECT;
    String sqlKeyword = op.sqlToken();   // Returns "SELECT"
    
    SqlOperation txOp = SqlOperation.BEGIN_TRANSACTION;
    String txText = txOp.sqlToken();   // Returns "BEGIN TRANSACTION"
    ```
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns the string representation of this SQL operation.
- **Parameters:**
  - (none)
- **Returns:** the SQL keyword string representation of this operation, never null
- **Examples:**
  - ```java
    String select = SqlOperation.SELECT.toString();             // "SELECT"
    String insert = SqlOperation.INSERT.toString();             // "INSERT"
    String tx = SqlOperation.BEGIN_TRANSACTION.toString();      // "BEGIN TRANSACTION"
    String unknown = SqlOperation.UNKNOWN.toString();           // "UNKNOWN"
    
    // toString() always equals sqlToken()
    boolean same = SqlOperation.DELETE.toString().equals(SqlOperation.DELETE.sqlToken()); // true
    ```

### Class SqlParser (com.landawn.abacus.query.SqlParser)
A utility class for parsing SQL statements into lexical SQL tokens.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### tokenizerConfigBuilder(...) -> TokenizerConfig.Builder
- **Signature:** `@Deprecated public static TokenizerConfig.Builder tokenizerConfigBuilder()`
- **Summary:** Starts an immutable tokenizer configuration from the built-in SQL separators.
- **Parameters:**
  - (none)
- **Returns:** a builder initialized with the default separators
##### defaultTokenizerConfig(...) -> TokenizerConfig
- **Signature:** `public static TokenizerConfig defaultTokenizerConfig()`
- **Summary:** Returns the immutable configuration used by the static parsing methods.
- **Parameters:**
  - (none)
- **Returns:** the default tokenizer configuration
##### tokenizer(...) -> Tokenizer
- **Signature:** `public static Tokenizer tokenizer()`
- **Summary:** Returns the default tokenizer using the built-in separator configuration.
- **Parameters:**
  - (none)
- **Returns:** a tokenizer using #defaultTokenizerConfig()
- **Signature:** `public static Tokenizer tokenizer(final TokenizerConfig tokenizerConfig)`
- **Summary:** Creates an instance-scoped tokenizer.
- **Parameters:**
  - `tokenizerConfig` (`TokenizerConfig`) — the tokenizer configuration; must not be null
- **Returns:** a tokenizer bound to the supplied configuration
##### parse(...) -> List<String>
- **Signature:** `public static List<String> parse(final String sql)`
- **Summary:** Parses a SQL statement into a list of lexical SQL tokens.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to parse (must not be null)
- **Returns:** a list of tokens representing the parsed SQL statement
- **Examples:**
  - ```java
    List<String> tokens = SqlParser.parse("SELECT name, age FROM users WHERE age >= 18");
    // Result: ["SELECT", " ", "name", ",", " ", "age", " ", "FROM", " ", "users", " ", "WHERE", " ", "age", " ", ">=", " ", "18"]
    ```
##### indexOfToken(...) -> int
- **Signature:** `public static int indexOfToken(final String sql, final String token)`
- **Summary:** Finds the index of a specific token within a SQL statement, searching from the beginning using case-insensitive matching.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search within (must not be null)
  - `token` (`String`) — the token or composite keyword to find (must not be null)
- **Returns:** the index of the token if found, or -1 if not found
- **Examples:**
  - ```java
    String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
    int index = SqlParser.indexOfToken(sql, "ORDER BY");   // 40
    ```
- **See also:** #indexOfToken(String, String, int, boolean)
- **Signature:** `public static int indexOfToken(final String sql, final String token, final int fromIndex)`
- **Summary:** Finds the index of a specific token within a SQL statement, searching from the given position using case-insensitive matching.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search within (must not be null)
  - `token` (`String`) — the token or composite keyword to find (must not be null)
  - `fromIndex` (`int`) — the earliest character position at which a match may be reported (0-based); scanning still begins at the start of sql for correct tokenization, but any match starting before fromIndex is skipped; negative values are treated as 0
- **Returns:** the index of the token if found, or -1 if not found
- **Examples:**
  - ```java
    String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
    int whereIndex = SqlParser.indexOfToken(sql, "WHERE", 0);   // 20
    ```
- **See also:** #indexOfToken(String, String, int, boolean)
- **Signature:** `public static int indexOfToken(final String sql, final String token, final int fromIndex, final boolean caseSensitive)`
- **Summary:** Finds the index of a specific token within a SQL statement starting from a given position.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search within (must not be null)
  - `token` (`String`) — the token or composite keyword to find (must not be null)
  - `fromIndex` (`int`) — the earliest character position at which a match may be reported (0-based); scanning still begins at the start of sql for correct tokenization, but any match starting before fromIndex is skipped; negative values are treated as 0
  - `caseSensitive` (`boolean`) — whether the search should be case-sensitive
- **Returns:** the index of the token if found, or -1 if not found
- **Examples:**
  - ```java
    String sql = "SELECT * FROM users WHERE name = 'John' ORDER BY age";
    int index = SqlParser.indexOfToken(sql, "ORDER BY", 0, false);
    // Returns: 40 (the position where "ORDER BY" starts)
    
    int whereIndex = SqlParser.indexOfToken(sql, "WHERE", 0, false);
    // Returns: 20 (the position where "WHERE" starts)
    ```
##### nextToken(...) -> String
- **Signature:** `public static String nextToken(final String sql, final int fromIndex)`
- **Summary:** Extracts the next token from a SQL statement starting at the specified index.
- **Contract:**
  - Comment skipping when encountered before any token character (line -- ..., MySQL hash # ..., and block /* ...
- **Parameters:**
  - `sql` (`String`) — the SQL statement to extract the token from (must not be null)
  - `fromIndex` (`int`) — the starting position for extraction (0-based); negative values are treated as 0
- **Returns:** the next token found, or an empty string if no more tokens exist
- **Examples:**
  - ```java
    String sql = "SELECT   name,   age FROM users";
    String token1 = SqlParser.nextToken(sql, 6);    // Returns: "name" (skips spaces after SELECT)
    String token2 = SqlParser.nextToken(sql, 13);   // Returns: ","
    String token3 = SqlParser.nextToken(sql, 14);   // Returns: "age" (skips spaces after comma)
    ```
##### nextTokenEndIndex(...) -> int
- **Signature:** `public static int nextTokenEndIndex(final String sql, final int fromIndex)`
- **Summary:** Returns the position just past the next token in a SQL statement, scanning from the specified index.
- **Contract:**
  - If no further token exists (only trailing whitespace and/or comments remain), the length of sql is returned, consistent with #nextToken(String, int) returning an empty string in that case.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to scan (must not be null)
  - `fromIndex` (`int`) — the starting position for scanning (0-based); negative values are treated as 0
- **Returns:** the index immediately after the next token, or the length of sql if no further token exists
- **Examples:**
  - ```java
    String sql = "SELECT   name,   age FROM users";
    int end1 = SqlParser.nextTokenEndIndex(sql, 6);    // 13 (just past "name")
    int end2 = SqlParser.nextTokenEndIndex(sql, 13);   // 14 (just past ",")
    int end3 = SqlParser.nextTokenEndIndex(sql, 14);   // 20 (just past "age")
    ```
- **See also:** #nextToken(String, int)
##### isFunctionName(...) -> boolean
- **Signature:** `public static boolean isFunctionName(final List<String> tokens, final int index)`
- **Summary:** Determines if a token at a specific position in a parsed token list represents a function name.
- **Contract:**
  - Determines if a token at a specific position in a parsed token list represents a function name.
  - A token is considered a function name if it is followed by the opening parenthesis token, either immediately or after whitespace.
- **Parameters:**
  - `tokens` (`List<String>`) — the parsed SQL tokens (typically the result of #parse(String))
  - `index` (`int`) — the index of the token to check; invalid indices return false
- **Returns:** true if the token at index is followed (after zero or more space tokens) by the "(" token; false otherwise
- **Examples:**
  - ```java
    List<String> tokens = SqlParser.parse("SELECT COUNT(*) FROM users");
    boolean isFunc = SqlParser.isFunctionName(tokens, 2);   // true for "COUNT"
    boolean notFunc = SqlParser.isFunctionName(tokens, 0);  // false for "SELECT"
    ```
- **Signature:** `@Deprecated public static boolean isFunctionName(final List<String> tokens, final int len, final int index)`
- **Summary:** Determines whether a token is a function name while examining only tokens below the supplied exclusive upper bound.
- **Parameters:**
  - `tokens` (`List<String>`) — the parsed SQL tokens (typically the result of #parse(String))
  - `len` (`int`) — the exclusive upper bound for the scan; values above tokens.size() are capped
  - `index` (`int`) — the index of the candidate function-name token
- **Returns:** true if an opening-parenthesis token occurs after index, with only space tokens between them, and before the effective upper bound; false otherwise
##### isSelectQuery(...) -> boolean
- **Signature:** `public static boolean isSelectQuery(final String sql)`
- **Summary:** Checks if the given SQL statement is a SELECT query.
- **Contract:**
  - Checks if the given SQL statement is a SELECT query.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or null
- **Returns:** true if the SQL is a SELECT query, false otherwise
- **Examples:**
  - ```java
    // Valid SELECT queries
    boolean result1 = SqlParser.isSelectQuery("SELECT * FROM users");
    // result1 = true
    
    boolean result2 = SqlParser.isSelectQuery("select id, name from products");
    // result2 = true
    
    boolean result3 = SqlParser.isSelectQuery("  SELECT count(*) FROM orders");
    // result3 = true
    
    // Non-SELECT queries
    boolean result4 = SqlParser.isSelectQuery("UPDATE users SET name = 'John'");
    // result4 = false
    
    boolean result5 = SqlParser.isSelectQuery("INSERT INTO users VALUES (1, 'John')");
    // result5 = false
    ```
- **See also:** #isInsertQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String), #isReadOrInsertQuery(String)
##### isReadOnlyQuery(...) -> boolean
- **Signature:** `public static boolean isReadOnlyQuery(final String sql)`
- **Summary:** Checks whether the given SQL statement is read-only, i.e.
- **Contract:**
  - A statement is considered read-only only if its leading keyword is SELECT (see #isSelectQuery(String)) and it contains no top-level mutation, DDL, or procedure-invocation keyword (INSERT, UPDATE, DELETE, MERGE, REPLACE, TRUNCATE, CREATE, ALTER or DROP), no procedure invocation (CALL, JDBC {call ...} / {?
  - For multi-statement SQL, a later statement is permitted only when it also resolves to a SELECT; a later statement with any other leading verb (including an unrecognized or vendor-specific command) makes the SQL non-read-only.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or null
- **Returns:** true if the SQL is a read-only SELECT query, false otherwise
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isReadOrInsertQuery(String)
##### isInsertQuery(...) -> boolean
- **Signature:** `public static boolean isInsertQuery(final String sql)`
- **Summary:** Checks if the given SQL statement is an INSERT query.
- **Contract:**
  - Checks if the given SQL statement is an INSERT query.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or null
- **Returns:** true if the SQL is an INSERT query, false otherwise
- **Examples:**
  - ```java
    // Valid INSERT queries
    boolean result1 = SqlParser.isInsertQuery("INSERT INTO users VALUES (1, 'John')");
    // result1 = true
    
    boolean result2 = SqlParser.isInsertQuery("insert into products (name, price) values ('Widget', 9.99)");
    // result2 = true
    
    boolean result3 = SqlParser.isInsertQuery("  INSERT INTO orders (order_id) VALUES (100)");
    // result3 = true
    
    // Non-INSERT queries
    boolean result4 = SqlParser.isInsertQuery("UPDATE users SET name = 'John'");
    // result4 = false
    
    boolean result5 = SqlParser.isInsertQuery("SELECT * FROM users");
    // result5 = false
    ```
- **See also:** #isSelectQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String), #isReadOrInsertQuery(String)
##### isUpdateQuery(...) -> boolean
- **Signature:** `public static boolean isUpdateQuery(final String sql)`
- **Summary:** Checks if the given SQL statement is an UPDATE query.
- **Contract:**
  - Checks if the given SQL statement is an UPDATE query.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or null
- **Returns:** true if the SQL is an UPDATE query, false otherwise
- **Examples:**
  - ```java
    // Valid UPDATE queries
    boolean result1 = SqlParser.isUpdateQuery("UPDATE users SET name = 'John'");
    // result1 = true
    
    boolean result2 = SqlParser.isUpdateQuery("update products set price = 9.99 where id = 1");
    // result2 = true
    
    // Non-UPDATE queries
    boolean result3 = SqlParser.isUpdateQuery("SELECT * FROM users");
    // result3 = false
    
    boolean result4 = SqlParser.isUpdateQuery("DELETE FROM users WHERE id = 1");
    // result4 = false
    ```
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String), #isReadOrInsertQuery(String)
##### isDeleteQuery(...) -> boolean
- **Signature:** `public static boolean isDeleteQuery(final String sql)`
- **Summary:** Checks if the given SQL statement is a DELETE query.
- **Contract:**
  - Checks if the given SQL statement is a DELETE query.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or null
- **Returns:** true if the SQL is a DELETE query, false otherwise
- **Examples:**
  - ```java
    // Valid DELETE queries
    boolean result1 = SqlParser.isDeleteQuery("DELETE FROM users WHERE id = 1");
    // result1 = true
    
    boolean result2 = SqlParser.isDeleteQuery("delete from products where price < 1");
    // result2 = true
    
    // Non-DELETE queries
    boolean result3 = SqlParser.isDeleteQuery("SELECT * FROM users");
    // result3 = false
    
    boolean result4 = SqlParser.isDeleteQuery("UPDATE users SET name = 'John'");
    // result4 = false
    ```
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isUpdateQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String), #isReadOrInsertQuery(String)
##### isInsertOrReplaceQuery(...) -> boolean
- **Signature:** `public static boolean isInsertOrReplaceQuery(final String sql)`
- **Summary:** Checks whether the given SQL statement begins with an INSERT OR REPLACE clause (the SQLite upsert form that overwrites an existing row when a uniqueness constraint is violated).
- **Contract:**
  - Checks whether the given SQL statement begins with an INSERT OR REPLACE clause (the SQLite upsert form that overwrites an existing row when a uniqueness constraint is violated).
  - */), any leading parentheses and any leading WITH clause; the three keywords INSERT, OR and REPLACE must appear (case-insensitively) in that order at the start of the actual statement.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or null
- **Returns:** true if the SQL begins with INSERT OR REPLACE, false otherwise
- **Examples:**
  - ```java
    boolean result1 = SqlParser.isInsertOrReplaceQuery("INSERT OR REPLACE INTO t (id) VALUES (1)");
    // result1 = true
    
    boolean result2 = SqlParser.isInsertOrReplaceQuery("INSERT INTO t (id) VALUES (1)");
    // result2 = false
    ```
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isReadOnlyQuery(String), #isReadOrInsertQuery(String)
##### isReadOrInsertQuery(...) -> boolean
- **Signature:** `public static boolean isReadOrInsertQuery(final String sql)`
- **Summary:** Checks whether the given SQL statement is a read or a plain/safe insert.
- **Contract:**
  - A statement qualifies as read-or-insert only if its leading keyword is SELECT or INSERT and it contains none of the following (matching outside of quoted string literals and SQL comments): a top-level UPDATE, DELETE, MERGE, REPLACE, TRUNCATE, CREATE, ALTER or DROP keyword (matched only at statement-start positions, so e.g.
  - More generally, every top-level statement must resolve to either SELECT or INSERT; an unrecognized or vendor-specific command is rejected rather than assumed to be safe.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or null
- **Returns:** true for an accepted read or plain/safe insert; false otherwise, including for a null or empty statement
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String), #isNonUpdateQuery(String)
##### isNonUpdateQuery(...) -> boolean
- **Signature:** `@Deprecated public static boolean isNonUpdateQuery(final String sql)`
- **Summary:** Compatibility alias for the historical NoUpdateDao terminology.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or null
- **Returns:** the result of #isReadOrInsertQuery(String)
##### isNoUpdateQuery(...) -> boolean
- **Signature:** `@Deprecated public static boolean isNoUpdateQuery(final String sql)`
- **Summary:** Compatibility alias for the original public API name.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or null
- **Returns:** the result of #isReadOrInsertQuery(String)

#### Public Instance Methods
- (none)

### Class TokenizerConfig (com.landawn.abacus.query.SqlParser.TokenizerConfig)
Immutable separator configuration for Tokenizer.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### builder(...) -> Builder
- **Signature:** `public static Builder builder()`
- **Summary:** Starts a configuration builder initialized with the built-in SQL separators.
- **Parameters:**
  - (none)
- **Returns:** a new builder initialized with the default separators

#### Public Instance Methods
##### toBuilder(...) -> Builder
- **Signature:** `public Builder toBuilder()`
- **Summary:** Creates a builder initialized with this configuration's separators.
- **Parameters:**
  - (none)
- **Returns:** a new builder initialized from this configuration
##### separators(...) -> Set<String>
- **Signature:** `public Set<String> separators()`
- **Summary:** Returns the configured separators as an immutable set.
- **Parameters:**
  - (none)
- **Returns:** the configured separators
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Parameters:**
  - (none)
- **Returns:** unspecified
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Parameters:**
  - `obj` (`Object`)
- **Returns:** unspecified
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Parameters:**
  - (none)
- **Returns:** unspecified

### Class Builder (com.landawn.abacus.query.SqlParser.TokenizerConfig.Builder)
Builder for immutable TokenizerConfig instances.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### withSeparator(...) -> Builder
- **Signature:** `public Builder withSeparator(final char separator)`
- **Summary:** Adds a single-character separator to this configuration.
- **Parameters:**
  - `separator` (`char`) — the separator to add
- **Returns:** this builder
- **Signature:** `public Builder withSeparator(final String separator)`
- **Summary:** Adds a single- or multi-character separator to this configuration.
- **Parameters:**
  - `separator` (`String`) — the separator to add; must not be null or empty
- **Returns:** this builder
##### withoutSeparator(...) -> Builder
- **Signature:** `public Builder withoutSeparator(final char separator)`
- **Summary:** Removes a single-character separator from this configuration.
- **Parameters:**
  - `separator` (`char`) — the separator to remove
- **Returns:** this builder
- **Signature:** `public Builder withoutSeparator(final String separator)`
- **Summary:** Removes a single- or multi-character separator from this configuration.
- **Parameters:**
  - `separator` (`String`) — the separator to remove; must not be null or empty
- **Returns:** this builder
##### build(...) -> TokenizerConfig
- **Signature:** `public TokenizerConfig build()`
- **Summary:** Builds the immutable configuration.
- **Parameters:**
  - (none)
- **Returns:** a new tokenizer configuration

### Class Tokenizer (com.landawn.abacus.query.SqlParser.Tokenizer)
Instance-scoped, thread-safe SQL tokenizer.

**Thread-safety:** thread-safe
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### tokenizerConfig(...) -> TokenizerConfig
- **Signature:** `public TokenizerConfig tokenizerConfig()`
- **Summary:** Returns the immutable configuration used by this tokenizer.
- **Parameters:**
  - (none)
- **Returns:** this tokenizer's configuration
##### parse(...) -> List<String>
- **Signature:** `public List<String> parse(final String sql)`
- **Summary:** Parses a SQL statement with this tokenizer's separator configuration.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to parse; must not be null
- **Returns:** the lexical SQL tokens
- **See also:** SqlParser#parse(String)
##### indexOfToken(...) -> int
- **Signature:** `public int indexOfToken(final String sql, final String token)`
- **Summary:** Finds a token from the beginning using case-insensitive matching.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search; must not be null
  - `token` (`String`) — the token to find; must not be null
- **Returns:** the token's character index, or -1
- **See also:** SqlParser#indexOfToken(String, String)
- **Signature:** `public int indexOfToken(final String sql, final String token, final int fromIndex)`
- **Summary:** Finds a token from the supplied character index using case-insensitive matching.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search; must not be null
  - `token` (`String`) — the token to find; must not be null
  - `fromIndex` (`int`) — the earliest character index to return
- **Returns:** the token's character index, or -1
- **See also:** SqlParser#indexOfToken(String, String, int)
- **Signature:** `public int indexOfToken(final String sql, final String token, final int fromIndex, final boolean caseSensitive)`
- **Summary:** Finds a token using this tokenizer's configured separators.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search; must not be null
  - `token` (`String`) — the token to find; must not be null
  - `fromIndex` (`int`) — the earliest character index to return
  - `caseSensitive` (`boolean`) — whether matching is case-sensitive
- **Returns:** the token's character index, or -1
- **See also:** SqlParser#indexOfToken(String, String, int, boolean)
##### nextToken(...) -> String
- **Signature:** `public String nextToken(final String sql, final int fromIndex)`
- **Summary:** Returns the next token at or after a character index.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to scan; must not be null
  - `fromIndex` (`int`) — the starting character index
- **Returns:** the next token, or an empty string if none remains
- **See also:** SqlParser#nextToken(String, int)
##### nextTokenEndIndex(...) -> int
- **Signature:** `public int nextTokenEndIndex(final String sql, final int fromIndex)`
- **Summary:** Returns the index immediately after the next token.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to scan; must not be null
  - `fromIndex` (`int`) — the starting character index
- **Returns:** the next token's exclusive end index, or sql.length() if none remains
- **See also:** SqlParser#nextTokenEndIndex(String, int)
##### isReadOnlyQuery(...) -> boolean
- **Signature:** `public boolean isReadOnlyQuery(final String sql)`
- **Summary:** Checks whether the statement is a read-only SELECT using this tokenizer's configured separator rules.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to classify; may be empty or null
- **Returns:** true if the SQL is a read-only SELECT query, false otherwise
- **See also:** SqlParser#isReadOnlyQuery(String)

## com.landawn.abacus.query.condition
### Class AbstractBetween (com.landawn.abacus.query.condition.AbstractBetween)
Abstract base class for BETWEEN and NOT BETWEEN conditions in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### propName(...) -> String
- **Signature:** `public String propName()`
- **Summary:** Returns the property name being checked in this BETWEEN or NOT BETWEEN condition.
- **Parameters:**
  - (none)
- **Returns:** the property name, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    Between between = new Between("age", 18, 65);
    String prop = between.propName();   // "age"
    ```
##### minValue(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T> T minValue()`
- **Summary:** Returns the lower bound of the range.
- **Parameters:**
  - (none)
- **Returns:** the configured minimum value, which may be a literal, a SubQuery, any other Condition, or null
- **Examples:**
  - ```java
    Between between = new Between("age", 18, 65);
    Integer min = between.minValue();   // 18
    ```
##### maxValue(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T> T maxValue()`
- **Summary:** Returns the upper bound of the range.
- **Parameters:**
  - (none)
- **Returns:** the configured maximum value, which may be a literal, a SubQuery, any other Condition, or null
- **Examples:**
  - ```java
    Between between = new Between("age", 18, 65);
    Integer max = between.maxValue();   // 65
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameters for this condition.
- **Contract:**
  - If either bound is a Condition (typically a SubQuery), its parameters are spliced in place of the bound itself.
- **Parameters:**
  - (none)
- **Returns:** an immutable list containing \[minValue, maxValue\], or their respective parameters spliced in where a bound is itself a Condition
- **Examples:**
  - ```java
    // Literal bounds -> [min, max]
    Between between = new Between("age", 18, 65);
    List<Object> p1 = between.parameters();   // [18, 65]
    
    // Null bounds are kept as-is
    Between nullBounds = new Between("age", (Object) null, (Object) null);
    List<Object> p2 = nullBounds.parameters();   // [null, null]
    
    // A Condition bound has its parameters spliced in
    SubQuery sub = Filters.subQuery("config", Arrays.asList("minAge"), Filters.eq("active", true));
    Between subBound = new Between("age", sub, 65);
    List<Object> p3 = subBound.parameters();   // [true, 65]
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this condition to its SQL representation.
- **Contract:**
  - If the operator is null (only possible for an uninitialized instance), the literal "null" is rendered in place of the operator.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name; if null, com.landawn.abacus.util.NamingPolicy#NO_CHANGE is used
- **Returns:** a SQL representation of this condition
- **Examples:**
  - ```java
    // Numeric bounds are unquoted
    Between between = new Between("age", 18, 65);
    String s1 = between.toSql(NamingPolicy.NO_CHANGE);   // "age BETWEEN 18 AND 65"
    
    // NotBetween uses the NOT BETWEEN operator
    NotBetween nb = new NotBetween("age", 18, 65);
    String s2 = nb.toSql(NamingPolicy.NO_CHANGE);   // "age NOT BETWEEN 18 AND 65"
    
    // String bounds are single-quoted; a null naming policy uses NO_CHANGE
    Between str = new Between("name", "A", "M");
    String s3 = str.toSql(null);   // "name BETWEEN 'A' AND 'M'"
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name, operator, and range values
- **Examples:**
  - ```java
    // Same property/operator/bounds -> equal hash codes
    Between a = new Between("age", 18, 65);
    Between b = new Between("age", 18, 65);
    boolean same = a.hashCode() == b.hashCode();   // true
    
    // Different bound -> different hash codes
    Between c = new Between("age", 18, 99);
    boolean diff = a.hashCode() == c.hashCode();   // false
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this condition is equal to another object.
- **Contract:**
  - Checks if this condition is equal to another object.
  - Two conditions are equal if they have the same property name, operator, minValue, and maxValue.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal, false otherwise
- **Examples:**
  - ```java
    Between a = new Between("age", 18, 65);
    Between b = new Between("age", 18, 65);
    boolean eq = a.equals(b);   // true
    
    // Different upper bound -> not equal
    boolean neMax = a.equals(new Between("age", 18, 99));   // false
    
    // Different operator (BETWEEN vs NOT BETWEEN) -> not equal
    boolean neOp = a.equals(new NotBetween("age", 18, 65));   // false
    
    // Non-AbstractBetween object -> not equal
    boolean neType = a.equals("age");   // false
    ```

### Class AbstractCondition (com.landawn.abacus.query.condition.AbstractCondition)
Abstract base class for all condition implementations.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### operator(...) -> Operator
- **Signature:** `@Override public Operator operator()`
- **Summary:** Returns the operator for this condition.
- **Parameters:**
  - (none)
- **Returns:** the operator for this condition
- **Examples:**
  - ```java
    Equal eq = new Equal("status", "active");
    Operator op1 = eq.operator();   // Operator.EQUAL
    
    GreaterThan gt = new GreaterThan("age", 18);
    Operator op2 = gt.operator();   // Operator.GREATER_THAN
    ```
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a SQL representation of this condition using the default naming policy.
- **Parameters:**
  - (none)
- **Returns:** a SQL representation of this condition
- **Examples:**
  - ```java
    Equal eq = new Equal("name", "John");
    String s1 = eq.toString();   // "name = 'John'"
    
    // Binary rendering (inherited by Equal): a null value with = renders as IS NULL
    Equal nullEq = new Equal("deletedAt", (Object) null);
    String s2 = nullEq.toString();   // "deletedAt IS NULL"
    ```

### Class AbstractIn (com.landawn.abacus.query.condition.AbstractIn)
Abstract base class for IN and NOT IN conditions in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### propName(...) -> String
- **Signature:** `public String propName()`
- **Summary:** Returns the property name being checked in this IN or NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** the (first) property name, or null for an uninitialized instance
- **Examples:**
  - ```java
    In inCond = new In("status", Arrays.asList("active", "pending"));
    String prop = inCond.propName();   // "status"
    ```
##### propNames(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> propNames()`
- **Summary:** Returns the property names checked in this IN or NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** non-null immutable collection of property names
- **Examples:**
  - ```java
    In single = new In("status", Arrays.asList("active", "pending"));
    Collection<String> p1 = single.propNames();   // ["status"]
    
    In multi = new In(Arrays.asList("first_name", "last_name"),
                      Arrays.asList(Arrays.asList("John", "Doe"), Arrays.asList("Jane", "Roe")));
    Collection<String> p2 = multi.propNames();   // ["first_name", "last_name"]
    ```
##### values(...) -> ImmutableList<?>
- **Signature:** `public ImmutableList<?> values()`
- **Summary:** Returns the values used by this IN or NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of the values (or value tuples), or an empty immutable list for an uninitialized instance
- **Examples:**
  - ```java
    In inCond = new In("status", Arrays.asList("active", "pending"));
    List<?> values = inCond.values();   // ["active", "pending"]
    ```
##### usesRowValueConstructor(...) -> boolean
- **Signature:** `public boolean usesRowValueConstructor()`
- **Summary:** Checks whether this condition was created in row value constructor form, i.e.
- **Parameters:**
  - (none)
- **Returns:** true if this condition renders in row value constructor form, false for the scalar form
- **Examples:**
  - ```java
    In scalar = new In("status", Arrays.asList("active", "pending"));
    boolean b1 = scalar.usesRowValueConstructor();   // false
    
    In rowValue = new In(Arrays.asList("firstName", "lastName"),
                         Arrays.asList(Arrays.asList("John", "Doe")));
    boolean b2 = rowValue.usesRowValueConstructor(); // true
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameter values for this condition, flattened in declaration order.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values, or an empty immutable list for an uninitialized instance (e.g. created via the no-arg constructor for deserialization)
- **Examples:**
  - ```java
    // String values listed in order
    In in = new In("status", Arrays.asList("active", "pending"));
    List<Object> p1 = in.parameters();   // ["active", "pending"]
    
    // Numeric values
    In nums = new In("id", Arrays.asList(1, 2, 3));
    List<Object> p2 = nums.parameters();   // [1, 2, 3]
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this condition to its SQL representation.
- **Contract:**
  - If the operator is null (only possible for an uninitialized instance), the literal "null" is rendered in place of the operator.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name(s); if null, com.landawn.abacus.util.NamingPolicy#NO_CHANGE is used
- **Returns:** the SQL representation, e.g., "status IN ('active', 'pending')" or, for a multi-column condition, "(first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))"
- **Examples:**
  - ```java
    // String values are single-quoted
    In in = new In("status", Arrays.asList("active", "pending"));
    String s1 = in.toSql(NamingPolicy.NO_CHANGE);   // "status IN ('active', 'pending')"
    
    // NotIn uses the NOT IN operator
    NotIn notIn = new NotIn("status", Arrays.asList("active", "pending"));
    String s2 = notIn.toSql(NamingPolicy.NO_CHANGE);   // "status NOT IN ('active', 'pending')"
    
    // Numeric values are unquoted; a null naming policy uses NO_CHANGE
    In nums = new In("id", Arrays.asList(1, 2, 3));
    String s3 = nums.toSql(null);   // "id IN (1, 2, 3)"
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this condition.
- **Parameters:**
  - (none)
- **Returns:** the hash code based on property name(s), operator, row-value mode, and values
- **Examples:**
  - ```java
    // Same property/operator/values -> equal hash codes
    In a = new In("status", Arrays.asList("active", "pending"));
    In b = new In("status", Arrays.asList("active", "pending"));
    boolean same = a.hashCode() == b.hashCode();   // true
    
    // Different values -> different hash codes
    In c = new In("status", Arrays.asList("active"));
    boolean diff = a.hashCode() == c.hashCode();   // false
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this condition is equal to another object.
- **Contract:**
  - Checks if this condition is equal to another object.
  - Two conditions are equal if they have the same property name(s), operator, row-value mode, and values list.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal, false otherwise
- **Examples:**
  - ```java
    In a = new In("status", Arrays.asList("active", "pending"));
    In b = new In("status", Arrays.asList("active", "pending"));
    boolean eq = a.equals(b);   // true
    
    // Different values -> not equal
    boolean neValues = a.equals(new In("status", Arrays.asList("active")));   // false
    
    // Different operator (IN vs NOT IN) -> not equal
    boolean neOp = a.equals(new NotIn("status", Arrays.asList("active", "pending")));   // false
    
    // Non-AbstractIn object -> not equal
    boolean neType = a.equals("status");   // false
    ```

### Class AbstractInSubQuery (com.landawn.abacus.query.condition.AbstractInSubQuery)
Abstract base class for IN and NOT IN subquery conditions in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### propName(...) -> String
- **Signature:** `public String propName()`
- **Summary:** Returns the property name being checked in this IN or NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** the (first) property name, or null for an uninitialized instance
- **Examples:**
  - ```java
    SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
    InSubQuery inSub = new InSubQuery("dept_id", subQuery);
    String prop = inSub.propName();   // "dept_id"
    ```
##### propNames(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> propNames()`
- **Summary:** Returns the property names for this IN or NOT IN subquery condition.
- **Parameters:**
  - (none)
- **Returns:** non-null immutable collection of property names
- **Examples:**
  - ```java
    SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
    InSubQuery inSub = new InSubQuery("dept_id", subQuery);
    Collection<String> props = inSub.propNames();   // ["dept_id"]
    ```
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used in this IN or NOT IN subquery condition.
- **Parameters:**
  - (none)
- **Returns:** the subquery, or null for an uninitialized instance
- **Examples:**
  - ```java
    SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
    InSubQuery inSub = new InSubQuery("dept_id", subQuery);
    SubQuery sq = inSub.subQuery();   // the subquery instance
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the list of parameters from the subquery.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values from the subquery; an empty immutable list if the subquery is null (only possible for an uninitialized instance)
- **Examples:**
  - ```java
    // Raw SQL subquery has no bind parameters -> empty list
    SubQuery raw = new SubQuery("SELECT id FROM departments WHERE active = true");
    InSubQuery inSub = new InSubQuery("deptId", raw);
    List<Object> p1 = inSub.parameters();   // [] (empty)
    
    // Structured subquery with a parameterized condition -> subquery's params
    SubQuery structured = Filters.subQuery("departments", Arrays.asList("id"), Filters.eq("active", true));
    InSubQuery inSub2 = new InSubQuery("deptId", structured);
    List<Object> p2 = inSub2.parameters();   // [true]
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name(s), operator, and subquery
- **Examples:**
  - ```java
    SubQuery subQuery = new SubQuery("SELECT id FROM departments");
    InSubQuery a = new InSubQuery("deptId", subQuery);
    InSubQuery b = new InSubQuery("deptId", subQuery);
    boolean same = a.hashCode() == b.hashCode();   // true
    
    // Different property -> different hash codes
    InSubQuery c = new InSubQuery("teamId", subQuery);
    boolean diff = a.hashCode() == c.hashCode();   // false
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this condition is equal to another object.
- **Contract:**
  - Checks if this condition is equal to another object.
  - Two conditions are equal if they have the same property names, operator, and subquery.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal, false otherwise
- **Examples:**
  - ```java
    SubQuery subQuery = new SubQuery("SELECT id FROM departments");
    InSubQuery a = new InSubQuery("deptId", subQuery);
    InSubQuery b = new InSubQuery("deptId", subQuery);
    boolean eq = a.equals(b);   // true
    
    // Different property -> not equal
    boolean neProp = a.equals(new InSubQuery("teamId", subQuery));   // false
    
    // Different operator (IN vs NOT IN) -> not equal
    boolean neOp = a.equals(new NotInSubQuery("deptId", subQuery));   // false
    
    // Non-AbstractInSubQuery object -> not equal
    boolean neType = a.equals("deptId");   // false
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this condition to its SQL representation.
- **Contract:**
  - If propNames is empty (only possible for an uninitialized instance), only OPERATOR (subQuery) is rendered, and the operator falls back to the literal "null" when operator is also null.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names; if null, com.landawn.abacus.util.NamingPolicy#NO_CHANGE is used
- **Returns:** the SQL representation of the condition
- **Examples:**
  - ```java
    // Single property -> propName IN (subQuery)
    SubQuery subQuery = new SubQuery("SELECT id FROM departments WHERE active = true");
    InSubQuery inSub = new InSubQuery("deptId", subQuery);
    String s1 = inSub.toSql(NamingPolicy.NO_CHANGE);
    // "deptId IN (SELECT id FROM departments WHERE active = true)"
    
    // NotInSubQuery uses the NOT IN operator
    NotInSubQuery notInSub = new NotInSubQuery("deptId", subQuery);
    String s2 = notInSub.toSql(NamingPolicy.NO_CHANGE);
    // "deptId NOT IN (SELECT id FROM departments WHERE active = true)"
    
    // Multiple properties -> (prop1, prop2) IN (subQuery)
    SubQuery multi = new SubQuery("SELECT firstName, lastName FROM employees");
    InSubQuery inMulti = new InSubQuery(Arrays.asList("firstName", "lastName"), multi);
    String s3 = inMulti.toSql(null);   // null naming policy uses NO_CHANGE
    // "(firstName, lastName) IN (SELECT firstName, lastName FROM employees)"
    ```

### Class All (com.landawn.abacus.query.condition.All)
Represents the SQL ALL operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public All(final SubQuery subQuery)` — Creates a new ALL condition with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this ALL condition.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE in_stock = true");
    All all = new All(subQuery);
    SubQuery retrieved = all.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = all.subQuery() == all.condition();
    // returns true
    ```

### Class And (com.landawn.abacus.query.condition.And)
Represents a composable AND condition that combines multiple conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public And(final Condition... conditions)` — Creates a new AND condition with the specified conditions.
- `public And(final Collection<? extends Condition> conditions)` — Creates a new AND condition with the specified collection of conditions.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### and(...) -> And
- **Signature:** `@Override public And and(final Condition condition)`
- **Summary:** Creates a new AND condition by adding another condition to this AND.
- **Parameters:**
  - `condition` (`Condition`) — the condition to add to this AND. Must not be null and must be composable (i.e. must not be or contain a Criteria, a Clause, an ON/USING connector, an ANY/ALL/SOME quantified-subquery operand, a standalone SubQuery, or an empty predicate).
- **Returns:** a new And condition containing all existing conditions plus the new one
- **Examples:**
  - ```java
    // Start with a basic AND
    And and = new And(Filters.equal("status", "active"));
    
    // Add more conditions through chaining
    And extended = and
        .and(Filters.greaterThan("score", 80))
        .and(Filters.lessThan("attempts", 3))
        .and(Filters.equal("verified", true));
    // SQL: ((status = 'active') AND (score > 80) AND (attempts < 3) AND (verified = true))
    
    // Original 'and' is unchanged
    // extended is a new instance with all conditions
    ```

### Class Any (com.landawn.abacus.query.condition.Any)
Represents the SQL ANY operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Any(final SubQuery subQuery)` — Creates a new ANY condition with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this ANY condition.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT price FROM products WHERE category = 'Electronics'");
    Any any = new Any(subQuery);
    SubQuery retrieved = any.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = any.subQuery() == any.condition();
    // returns true
    ```

### Class Between (com.landawn.abacus.query.condition.Between)
Represents a BETWEEN condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Between(final String propName, final Object minValue, final Object maxValue)` — Creates a new BETWEEN condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Binary (com.landawn.abacus.query.condition.Binary)
Base class for binary conditions that compare a property with a value.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Binary(final String propName, final Operator operator, final Object propValue)` — Creates a new Binary condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### propName(...) -> String
- **Signature:** `public String propName()`
- **Summary:** Returns the property name being compared.
- **Parameters:**
  - (none)
- **Returns:** the property name, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    Binary eq = new Equal("age", 25);
    String name = eq.propName();   // "age"
    
    Binary like = new Like("email", "%@example.com");
    String likeName = like.propName();   // "email"
    ```
##### propValue(...) -> Object
- **Signature:** `public Object propValue()`
- **Summary:** Returns the property value without an unchecked generic cast.
- **Parameters:**
  - (none)
- **Returns:** the property value, which may be null
- **Signature:** `public <T> T propValue(final Class<T> valueType)`
- **Summary:** Returns the property value cast by the supplied runtime type.
- **Parameters:**
  - `valueType` (`Class<T>`) — the requested value type; must not be null
- **Returns:** the property value cast to valueType, or null when the stored value is null
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameters for this condition.
- **Contract:**
  - If the value is null and the operator is =, !=, <>, IS, or IS NOT, an empty list is returned because the SQL is rendered as IS NULL / IS NOT NULL with no bind parameter.
  - If the value is null with any other operator (e.g.
  - If the operator is null (only possible for an uninitialized instance), an empty list is returned.
  - If the operator is IN or NOT IN and the value is a Collection, each element is added as a parameter; any element that is itself a Condition has its own parameters spliced in.
  - If the value is a Condition (e.g., a subquery), the subquery's own parameters are returned.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values; never null
- **Examples:**
  - ```java
    // Literal value -> single-element list
    Binary eq = new Equal("age", 25);
    List<Object> p1 = eq.parameters();   // [25]
    
    // Null value with = or != -> empty list (rendered as IS NULL / IS NOT NULL)
    Binary nullEq = new Equal("name", (Object) null);
    List<Object> p2 = nullEq.parameters();   // [] (empty)
    
    // Subquery value -> the subquery's own parameters
    SubQuery sub = Filters.subQuery("users", Arrays.asList("id"), Filters.eq("active", true));
    Binary eqSub = new Equal("userId", sub);
    List<Object> p3 = eqSub.parameters();   // [true] (the subquery's params)
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this Binary condition to its SQL representation using the specified naming policy.
- **Contract:**
  - When the value is null and the operator is = or IS, the output is propertyName IS NULL; when the operator is !=, <>, or IS NOT, the output is propertyName IS NOT NULL.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name; if null, com.landawn.abacus.util.NamingPolicy#NO_CHANGE is used
- **Returns:** a SQL representation of this condition
- **Examples:**
  - ```java
    // String values are single-quoted; numbers are unquoted
    Binary eq = new Equal("name", "John");
    String s1 = eq.toSql(NamingPolicy.NO_CHANGE);   // "name = 'John'"
    
    Binary gt = new GreaterThan("age", 18);
    String s2 = gt.toSql(NamingPolicy.NO_CHANGE);   // "age > 18"
    
    // Null value with = renders as IS NULL; with != renders as IS NOT NULL
    Binary nullEq = new Equal("deletedAt", (Object) null);
    String s3 = nullEq.toSql(NamingPolicy.NO_CHANGE);   // "deletedAt IS NULL"
    
    Binary nullNe = new NotEqual("deletedAt", (Object) null);
    String s4 = nullNe.toSql(NamingPolicy.NO_CHANGE);   // "deletedAt IS NOT NULL"
    
    // Subquery values are parenthesized; a null naming policy uses NO_CHANGE
    Binary sub = new Equal("userId", Filters.subQuery("SELECT id FROM users"));
    String s5 = sub.toSql(null);   // "userId = (SELECT id FROM users)"
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this Binary condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name, operator, and value
- **Examples:**
  - ```java
    // Equal property/operator/value -> equal hash codes
    Binary a = new Equal("age", 25);
    Binary b = new Equal("age", 25);
    boolean same = a.hashCode() == b.hashCode();   // true
    
    // Different value -> different hash codes
    Binary c = new Equal("age", 30);
    boolean diff = a.hashCode() == c.hashCode();   // false
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this Binary condition is equal to another object.
- **Contract:**
  - Checks if this Binary condition is equal to another object.
  - Two conditions are equal only if they are of the exact same runtime class and have the same property name, operator, and value.
  - The runtime class is part of the equality contract, so an instance of one concrete subclass is never equal to an instance of a different subclass (or to a raw Binary), even when their property name, operator, and value all match.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal, false otherwise
- **Examples:**
  - ```java
    Binary a = new Equal("age", 25);
    Binary b = new Equal("age", 25);
    boolean eq = a.equals(b);   // true (same prop, operator, value)
    
    // Different value or property -> not equal
    boolean neValue = a.equals(new Equal("age", 30));   // false
    boolean neProp = a.equals(new Equal("name", 25));   // false
    
    // Non-Binary object -> not equal
    boolean neType = a.equals("age");   // false
    ```

### Class Cell (com.landawn.abacus.query.condition.Cell)
Represents a condition cell that wraps another condition with an operator.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### condition(...) -> Condition
- **Signature:** `public Condition condition()`
- **Summary:** Returns the wrapped condition.
- **Contract:**
  - Callers that need a more specific subtype must cast explicitly.
- **Parameters:**
  - (none)
- **Returns:** the wrapped condition; never null for instances created via the protected constructor, but may be null for uninitialized instances produced by the package-private default constructor (e.g., during Kryo deserialization)
- **Examples:**
  - ```java
    On onCond = new On("a.id", "b.id");
    Equal inner = (Equal) onCond.condition();
    // inner.toString() returns "a.id = b.id"
    
    Where where = new Where(Filters.eq("active", true));
    Condition c = where.condition();
    // c.toString() returns "active = true"
    
    // Edge: the wrapped condition is returned as-is; cast to the concrete type to access subtype API
    Equal eq = (Equal) where.condition();   // ok
    Like bad = (Like) where.condition();    // throws ClassCastException
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameters from the wrapped condition.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameters from the wrapped condition, or an empty immutable list if no condition is set
- **Examples:**
  - ```java
    Where where = new Where(Filters.between("age", 18, 65));
    where.parameters();          // returns [18, 65]
    
    Where single = new Where(Filters.eq("active", true));
    single.parameters();         // returns [true]
    
    // Edge: wrapping a parameter-free condition yields an empty list
    On onCond = new On("a.id", "b.id");
    onCond.parameters();         // returns [] (empty, immutable)
    
    // Edge: the returned list reflects the wrapped condition only and is immutable
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this Cell condition to its SQL representation using the specified naming policy.
- **Contract:**
  - The output format is OPERATOR condition_string (separated by a single space), or just OPERATOR if the wrapped condition is null.
  - If the operator is null (only possible for an uninitialized instance), the literal "null" is rendered in place of the operator.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names within the wrapped condition; if null, com.landawn.abacus.util.NamingPolicy#NO_CHANGE is used
- **Returns:** a SQL representation of this Cell
- **Examples:**
  - ```java
    Where where = new Where(Filters.eq("active", true));
    where.toSql(NamingPolicy.NO_CHANGE);   // returns "WHERE active = true"
    
    On onCond = new On("a.id", "b.id");
    onCond.toSql(NamingPolicy.NO_CHANGE);  // returns "ON a.id = b.id" (inner NOT parenthesized)
    
    // Edge: naming policy rewrites property names in the wrapped condition
    Where w = new Where(Filters.eq("firstName", "John"));
    w.toSql(NamingPolicy.SNAKE_CASE);      // returns "WHERE first_name = 'John'"
    
    // Edge: an uninitialized Cell (null operator) renders the literal "null" for the operator
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this Cell.
- **Parameters:**
  - (none)
- **Returns:** hash code based on operator and wrapped condition
- **Examples:**
  - ```java
    Where a = new Where(Filters.eq("active", true));
    Where b = new Where(Filters.eq("active", true));
    a.hashCode() == b.hashCode();   // true (same operator and condition)
    
    // Edge: a different wrapped condition produces a different hash code
    Where c = new Where(Filters.eq("active", false));
    a.hashCode() == c.hashCode();   // (typically) false
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this Cell is equal to another object.
- **Contract:**
  - Checks if this Cell is equal to another object.
  - Two Cells are equal if they are of the same runtime class and have the same operator and wrapped condition.
  - Different concrete subclasses of Cell are never equal, even when their operator and wrapped condition are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal, false otherwise
- **Examples:**
  - ```java
    Where a = new Where(Filters.eq("active", true));
    Where b = new Where(Filters.eq("active", true));
    a.equals(b);    // returns true
    
    // Edge: different wrapped condition -> not equal
    Where c = new Where(Filters.eq("active", false));
    a.equals(c);    // returns false
    
    // Edge: different concrete subclass -> never equal, even with equal operator/condition
    Where w = new Where(Filters.eq("x", 1));
    Having h = new Having(Filters.eq("x", 1));
    w.equals(h);    // returns false
    
    a.equals(null); // returns false
    ```

### Class Clause (com.landawn.abacus.query.condition.Clause)
Abstract base class for SQL clause conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class ComposableCell (com.landawn.abacus.query.condition.ComposableCell)
A composable variant of Cell that supports logical composition via AND/OR/NOT operations.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### condition(...) -> Condition
- **Signature:** `public Condition condition()`
- **Summary:** Returns the wrapped condition.
- **Contract:**
  - Callers that need a more specific subtype must cast explicitly.
- **Parameters:**
  - (none)
- **Returns:** the wrapped condition; never null for instances created via the protected constructor, but may be null for uninitialized instances produced by the package-private default constructor (e.g., during Kryo deserialization)
- **Examples:**
  - ```java
    Condition eq = Filters.equal("status", "active");
    Not notCond = new Not(eq);
    Equal inner = (Equal) notCond.condition();
    // inner.toString() returns "status = 'active'"
    
    SubQuery sub = Filters.subQuery("SELECT id FROM orders");
    Exists exists = new Exists(sub);
    Condition c = exists.condition();
    // c == sub (the same wrapped SubQuery is returned)
    
    // Edge: the wrapped condition is returned as-is; an incompatible cast fails
    Like bad = (Like) notCond.condition();   // throws ClassCastException
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameters from the wrapped condition.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameters from the wrapped condition, or an empty immutable list if no condition is set
- **Examples:**
  - ```java
    Not not = new Not(Filters.between("age", 18, 65));
    not.parameters();          // returns [18, 65]
    
    Not single = new Not(Filters.eq("active", true));
    single.parameters();       // returns [true]
    
    // Edge: a wrapped SubQuery with no bound parameters yields an empty list
    Exists exists = new Exists(Filters.subQuery("SELECT 1"));
    exists.parameters();       // returns [] (empty, immutable)
    
    // Edge: the returned list reflects the wrapped condition only and is immutable
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this ComposableCell to its SQL representation using the specified naming policy.
- **Contract:**
  - If the operator is null (only possible for an uninitialized instance), the literal "null" is rendered in place of the operator.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names within the wrapped condition; if null, com.landawn.abacus.util.NamingPolicy#NO_CHANGE is used
- **Returns:** a SQL representation of this ComposableCell
- **Examples:**
  - ```java
    Not not = new Not(Filters.eq("status", "active"));
    not.toSql(NamingPolicy.NO_CHANGE);   // returns "NOT (status = 'active')"
    
    Exists exists = new Exists(Filters.subQuery("SELECT 1"));
    exists.toSql(NamingPolicy.NO_CHANGE); // returns "EXISTS (SELECT 1)"
    
    // Edge: naming policy rewrites property names in the wrapped condition
    Not w = new Not(Filters.eq("firstName", "John"));
    w.toSql(NamingPolicy.SNAKE_CASE);    // returns "NOT (first_name = 'John')"
    
    // Edge: a null naming policy falls back to NO_CHANGE; an uninitialized
    // instance (null operator) renders the literal "null" for the operator
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this ComposableCell, based on the operator and wrapped condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on operator and wrapped condition
- **Examples:**
  - ```java
    Not a = new Not(Filters.eq("status", "active"));
    Not b = new Not(Filters.eq("status", "active"));
    a.hashCode() == b.hashCode();   // true (same operator and condition)
    
    // Edge: a different wrapped condition produces a different hash code
    Not c = new Not(Filters.eq("status", "inactive"));
    a.hashCode() == c.hashCode();   // (typically) false
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this ComposableCell is equal to another object.
- **Contract:**
  - Checks if this ComposableCell is equal to another object.
  - Two ComposableCells are equal if they are of the same runtime class and have the same operator and wrapped condition.
  - Different concrete subclasses of ComposableCell are never equal, even when their operator and wrapped condition are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal, false otherwise
- **Examples:**
  - ```java
    Not a = new Not(Filters.eq("status", "active"));
    Not b = new Not(Filters.eq("status", "active"));
    a.equals(b);    // returns true
    
    // Edge: different wrapped condition -> not equal
    Not c = new Not(Filters.eq("status", "inactive"));
    a.equals(c);    // returns false
    
    // Edge: different concrete subclass -> never equal, even with equal operator/condition
    SubQuery sub = Filters.subQuery("SELECT 1");
    Exists exists = new Exists(sub);
    NotExists notExists = new NotExists(sub);
    exists.equals(notExists);   // returns false
    
    a.equals(null); // returns false
    ```

### Class ComposableCondition (com.landawn.abacus.query.condition.ComposableCondition)
A Condition that supports logical composition via and(), or(), not(), and xor().

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### not(...) -> Not
- **Signature:** `public Not not()`
- **Summary:** Creates a new NOT condition that negates this condition.
- **Contract:**
  - The result is true when this condition is false, and vice versa.
- **Parameters:**
  - (none)
- **Returns:** a new Not condition wrapping this condition
- **Examples:**
  - ```java
    Condition active = Filters.equal("status", "active");
    Not notActive = ((ComposableCondition) active).not();
    // SQL: NOT (status = 'active')
    ```
##### and(...) -> And
- **Signature:** `public And and(final Condition condition)`
- **Summary:** Creates a new AND condition combining this condition with another.
- **Contract:**
  - Both conditions must be true for the result to be true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to AND with this condition (must not be null)
- **Returns:** a new And condition containing both conditions
- **Examples:**
  - ```java
    Condition age = Filters.greaterThan("age", 18);
    Condition status = Filters.equal("status", "active");
    And combined = ((ComposableCondition) age).and(status);
    // SQL: ((age > 18) AND (status = 'active'))
    ```
##### or(...) -> Or
- **Signature:** `public Or or(final Condition condition)`
- **Summary:** Creates a new OR condition combining this condition with another.
- **Contract:**
  - At least one condition must be true for the result to be true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to OR with this condition (must not be null)
- **Returns:** a new Or condition containing both conditions
- **Examples:**
  - ```java
    Condition admin = Filters.equal("role", "admin");
    Condition manager = Filters.equal("role", "manager");
    Or either = ((ComposableCondition) admin).or(manager);
    // SQL: ((role = 'admin') OR (role = 'manager'))
    ```
##### xor(...) -> Or
- **Signature:** `public Or xor(final Condition condition)`
- **Summary:** Creates a new XOR (exclusive OR) condition combining this condition with another.
- **Contract:**
  - Exactly one of the two conditions must be true for the result to be true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to XOR with this condition (must not be null)
- **Returns:** a composable condition representing the exclusive-or (this AND NOT condition) OR (NOT this AND condition)
- **Examples:**
  - ```java
    Condition a = Filters.equal("type", "A");
    Condition b = Filters.equal("type", "B");
    Or exclusive = ((ComposableCondition) a).xor(b);
    // Logically: (a AND NOT b) OR (NOT a AND b)
    // SQL: ((((type = 'A') AND (NOT (type = 'B')))) OR (((NOT (type = 'A')) AND (type = 'B'))))
    // (each junction wraps every child in parentheses and the whole expression in an outer pair,
    //  so the nested And inside the outer Or picks up an extra layer of parens)
    ```

### Interface Condition (com.landawn.abacus.query.condition.Condition)
The base interface for all query conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### operator(...) -> Operator
- **Signature:** `Operator operator()`
- **Summary:** Returns the operator associated with this condition.
- **Parameters:**
  - (none)
- **Returns:** the operator for this condition; standard constructed implementations return a non-null value (a null is possible only in an uninitialized serialization-framework instance or a non-conforming custom implementation)
- **Examples:**
  - ```java
    Condition eq = Filters.equal("status", "active");
    Operator op = eq.operator();   // Operator.EQUAL
    
    Condition between = Filters.between("age", 18, 65);
    Operator betweenOp = between.operator();   // Operator.BETWEEN
    
    Condition combined = Filters.and(eq, between);
    Operator andOp = combined.operator();   // Operator.AND
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `ImmutableList<Object> parameters()`
- **Summary:** Returns the list of parameter values associated with this condition.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values; never null (an empty list is returned when there are no parameters)
- **Examples:**
  - ```java
    Condition eq = Filters.equal("name", "John");
    ImmutableList<Object> params = eq.parameters();   // ["John"]
    
    Condition between = Filters.between("age", 18, 65);
    ImmutableList<Object> rangeParams = between.parameters();   // [18, 65]
    
    Condition combined = Filters.and(eq, between);
    ImmutableList<Object> allParams = combined.parameters();   // ["John", 18, 65]
    ```
##### toSql(...) -> String
- **Signature:** `String toSql(NamingPolicy namingPolicy)`
- **Summary:** Returns a SQL representation of this condition using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the policy for formatting property names; a null naming policy is treated as NamingPolicy#NO_CHANGE by the standard implementations
- **Returns:** a SQL representation of this condition
- **Examples:**
  - ```java
    Condition eq = Filters.equal("firstName", "John");
    
    // No change to property names
    String noChange = eq.toSql(NamingPolicy.NO_CHANGE);       // "firstName = 'John'"
    
    // Convert to lower case with underscores (snake_case)
    String lower = eq.toSql(NamingPolicy.SNAKE_CASE);   // "first_name = 'John'"
    
    // Convert to upper case with underscores (SCREAMING_SNAKE_CASE)
    String upper = eq.toSql(NamingPolicy.SCREAMING_SNAKE_CASE);   // "FIRST_NAME = 'John'"
    ```

### Class Criteria (com.landawn.abacus.query.condition.Criteria)
A container representing a complete SQL query structure composed of multiple clauses (Join, Where, GroupBy, Having, OrderBy, Limit, and set operations like Union/UnionAll/Intersect/Except/Minus).

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### builder(...) -> Builder
- **Signature:** `public static Builder builder()`
- **Summary:** Creates a new Criteria builder.
- **Parameters:**
  - (none)
- **Returns:** a new Builder instance
- **Examples:**
  - ```java
    Criteria criteria = Criteria.builder()
        .where(Filters.equal("status", "active"))
        .orderBy("name")
        .limit(50)
        .build();
    ```

#### Public Instance Methods
##### selectModifier(...) -> String
- **Signature:** `public String selectModifier()`
- **Summary:** Returns the SELECT modifier (e.g., DISTINCT, DISTINCTROW, DISTINCT ON (col1, col2), or any custom modifier set via Builder#selectModifier(String)), or null if none was set.
- **Contract:**
  - Returns the SELECT modifier (e.g., DISTINCT, DISTINCTROW, DISTINCT ON (col1, col2), or any custom modifier set via Builder#selectModifier(String)), or null if none was set.
- **Parameters:**
  - (none)
- **Returns:** the SELECT modifier, or null if not set
- **Examples:**
  - ```java
    Criteria.builder().build().selectModifier();                    // returns null
    Criteria.builder().distinct().build().selectModifier();         // returns "DISTINCT"
    Criteria.builder().distinctOn("a, b").build().selectModifier(); // returns "DISTINCT ON (a, b)"
    ```
- **See also:** Builder#distinct(), Builder#distinctOn(String), Builder#distinctRow(), Builder#distinctRowBy(String), Builder#selectModifier(String)
##### joins(...) -> ImmutableList<Join>
- **Signature:** `public ImmutableList<Join> joins()`
- **Summary:** Returns all JOIN clauses (JOIN, INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL JOIN, CROSS JOIN, NATURAL JOIN) in the order they were added.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of Join conditions; empty if none exist
- **Examples:**
  - ```java
    Criteria.builder().build().joins();   // returns [] (empty list)
    
    Criteria c = Criteria.builder().join("orders").innerJoin("items").build();
    c.joins().size();                     // returns 2
    c.joins().add(null);                  // throws UnsupportedOperationException (unmodifiable view)
    ```
##### where(...) -> Where
- **Signature:** `public Where where()`
- **Summary:** Returns the WHERE clause, or null if none was set.
- **Contract:**
  - Returns the WHERE clause, or null if none was set.
- **Parameters:**
  - (none)
- **Returns:** the Where clause, or null
- **Examples:**
  - ```java
    Criteria.builder().build().where();   // returns null
    
    Criteria c = Criteria.builder().where(Filters.eq("a", 1)).build();
    c.where().operator();                 // returns Operator.WHERE
    ```
##### groupBy(...) -> GroupBy
- **Signature:** `public GroupBy groupBy()`
- **Summary:** Returns the GROUP BY clause, or null if none was set.
- **Contract:**
  - Returns the GROUP BY clause, or null if none was set.
- **Parameters:**
  - (none)
- **Returns:** the GroupBy clause, or null
- **Examples:**
  - ```java
    Criteria.builder().build().groupBy();   // returns null
    
    Criteria c = Criteria.builder().groupBy("dept").build();
    c.groupBy().operator();                 // returns Operator.GROUP_BY
    ```
##### having(...) -> Having
- **Signature:** `public Having having()`
- **Summary:** Returns the HAVING clause, or null if none was set.
- **Contract:**
  - Returns the HAVING clause, or null if none was set.
- **Parameters:**
  - (none)
- **Returns:** the Having clause, or null
- **Examples:**
  - ```java
    Criteria.builder().build().having();   // returns null
    
    Criteria c = Criteria.builder().having(Filters.greaterThan("COUNT(*)", 5)).build();
    c.having().operator();                 // returns Operator.HAVING
    ```
##### setOperations(...) -> ImmutableList<Clause>
- **Signature:** `public ImmutableList<Clause> setOperations()`
- **Summary:** Returns all set operations (UNION, UNION ALL, INTERSECT, EXCEPT, MINUS) in the order they were added.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of set operation clauses; empty if none exist
- **Examples:**
  - ```java
    Criteria.builder().build().setOperations();   // returns [] (empty list)
    
    Criteria c = Criteria.builder().union(Filters.subQuery("SELECT id FROM t")).build();
    c.setOperations().size();                     // returns 1
    ```
##### orderBy(...) -> OrderBy
- **Signature:** `public OrderBy orderBy()`
- **Summary:** Returns the ORDER BY clause, or null if none was set.
- **Contract:**
  - Returns the ORDER BY clause, or null if none was set.
- **Parameters:**
  - (none)
- **Returns:** the OrderBy clause, or null
- **Examples:**
  - ```java
    Criteria.builder().build().orderBy();   // returns null
    
    Criteria c = Criteria.builder().orderBy("name").build();
    c.orderBy().operator();                 // returns Operator.ORDER_BY
    ```
##### limit(...) -> Limit
- **Signature:** `public Limit limit()`
- **Summary:** Returns the LIMIT clause, or null if none was set.
- **Contract:**
  - Returns the LIMIT clause, or null if none was set.
- **Parameters:**
  - (none)
- **Returns:** the Limit clause, or null
- **Examples:**
  - ```java
    Criteria.builder().build().limit();   // returns null
    
    Criteria c = Criteria.builder().limit(10).build();
    c.limit().toSql(NamingPolicy.NO_CHANGE);   // returns "LIMIT 10"
    ```
##### conditions(...) -> ImmutableList<Condition>
- **Signature:** `public ImmutableList<Condition> conditions()`
- **Summary:** Returns all conditions (clauses) in this criteria in the order they were added.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of all conditions
- **Examples:**
  - ```java
    Criteria.builder().build().conditions();   // returns [] (empty list)
    
    Criteria c = Criteria.builder().where(Filters.eq("a", 1)).orderBy("b").build();
    c.conditions().size();                      // returns 2
    c.conditions().clear();                     // throws UnsupportedOperationException (unmodifiable view)
    ```
##### findConditions(...) -> ImmutableList<Condition>
- **Signature:** `public ImmutableList<Condition> findConditions(final Operator operator)`
- **Summary:** Returns all conditions whose Condition#operator() equals the given operator, in the order they were added.
- **Parameters:**
  - `operator` (`Operator`) — the operator to match (may be null, in which case this returns an empty list since AbstractCondition disallows null operators)
- **Returns:** an immutable list of matching conditions; empty if none found
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().join("o1").join("o2").where(Filters.eq("a", 1)).build();
    c.findConditions(Operator.JOIN).size();    // returns 2 (JOINs accumulate)
    c.findConditions(Operator.WHERE).size();   // returns 1
    c.findConditions(Operator.HAVING);         // returns [] (no HAVING present)
    c.findConditions(null);                    // returns [] (null never matches an operator)
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Collects parameters from all conditions in SQL clause order: JOIN, WHERE, GROUP BY, HAVING, set operations, ORDER BY, LIMIT.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of all parameters collected from the constituent clauses; empty if this criteria has no conditions or if none of the conditions carry parameters
- **Examples:**
  - ```java
    Criteria.builder().build().parameters();   // returns [] (empty list)
    
    Criteria c = Criteria.builder().where(Filters.eq("status", "active")).limit(10).build();
    c.parameters();   // returns ["active"] (the literal LIMIT count carries no parameter)
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Returns a SQL representation of this Criteria using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names within each clause; null is treated as NamingPolicy#NO_CHANGE by the standard clause implementations
- **Returns:** a SQL representation of this Criteria
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().where(Filters.eq("firstName", "John")).build();
    c.toSql(NamingPolicy.NO_CHANGE);    // returns " WHERE firstName = 'John'"
    c.toSql(NamingPolicy.SNAKE_CASE);   // returns " WHERE first_name = 'John'"
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this Criteria, based on its select modifier and conditions list.
- **Parameters:**
  - (none)
- **Returns:** hash code based on the select modifier and the ordered conditions list
- **Examples:**
  - ```java
    Criteria c1 = Criteria.builder().where(Filters.eq("a", 1)).build();
    Criteria c2 = Criteria.builder().where(Filters.eq("a", 1)).build();
    c1.hashCode() == c2.hashCode();   // returns true (equal criteria share a hash code)
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks whether this Criteria is equal to another object.
- **Contract:**
  - Two Criteria instances are equal if they have the same select modifier and the same ordered list of conditions.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal, false otherwise
- **Examples:**
  - ```java
    Criteria c1 = Criteria.builder().where(Filters.eq("a", 1)).build();
    Criteria c2 = Criteria.builder().where(Filters.eq("a", 1)).build();
    c1.equals(c2);               // returns true
    c1.equals(c1);               // returns true (reflexive)
    c1.equals(null);             // returns false
    c1.equals("not a Criteria"); // returns false
    ```
##### toBuilder(...) -> Builder
- **Signature:** `public Builder toBuilder()`
- **Summary:** Creates a new Builder pre-populated with this criteria's select modifier and conditions.
- **Parameters:**
  - (none)
- **Returns:** a new mutable Builder initialized from this criteria
- **Examples:**
  - ```java
    Criteria original = Criteria.builder().distinct().where(Filters.eq("a", 1)).build();
    Criteria copy = original.toBuilder().build();
    copy.equals(original);   // returns true (a faithful copy)
    ```

### Class Builder (com.landawn.abacus.query.condition.Criteria.Builder)
A mutable builder for constructing Criteria instances with a fluent API.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### distinct(...) -> Builder
- **Signature:** `public Builder distinct()`
- **Summary:** Sets the DISTINCT modifier for the query.
- **Contract:**
  - When the criteria is appended to a SqlBuilder, it is applied to that builder's current SELECT segment.
- **Parameters:**
  - (none)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .distinct()
        .where(Filters.equal("status", "active"))
        .build();
    c.selectModifier();                 // returns "DISTINCT"
    c.toSql(NamingPolicy.NO_CHANGE);    // returns " DISTINCT WHERE status = 'active'"
    ```
- **See also:** #distinctOn(String)
##### distinctOn(...) -> Builder
- **Signature:** `public Builder distinctOn(final String columnNames)`
- **Summary:** Sets the PostgreSQL-style DISTINCT ON modifier with specific expressions.
- **Contract:**
  - The database dialect used to execute the generated SQL must support this syntax.
  - If columnNames is null, empty, or blank, a plain DISTINCT modifier is used.
- **Parameters:**
  - `columnNames` (`String`) — the expressions for DISTINCT ON; if null, empty, or blank, plain DISTINCT is used
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.builder().distinctOn("department, location").build().selectModifier();
    // returns "DISTINCT ON (department, location)"
    
    Criteria.builder().distinctOn("city").build().selectModifier();   // returns "DISTINCT ON (city)"
    
    // null, empty, or blank falls back to a plain DISTINCT (no parentheses).
    Criteria.builder().distinctOn(null).build().selectModifier();     // returns "DISTINCT"
    Criteria.builder().distinctOn("").build().selectModifier();       // returns "DISTINCT"
    ```
##### distinctRow(...) -> Builder
- **Signature:** `public Builder distinctRow()`
- **Summary:** Sets the DISTINCTROW modifier for the query.
- **Contract:**
  - Like #distinct(), the modifier is applied to the current SELECT when this criteria is appended to a SqlBuilder.
- **Parameters:**
  - (none)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .distinctRow()
        .where(Filters.equal("active", true))
        .build();
    c.selectModifier();   // returns "DISTINCTROW"
    ```
##### distinctRowBy(...) -> Builder
- **Signature:** `public Builder distinctRowBy(final String columnNames)`
- **Summary:** Sets the DISTINCTROW modifier with specific columns.
- **Contract:**
  - If columnNames is null, empty, or blank, a plain DISTINCTROW modifier (without parentheses) is used.
- **Parameters:**
  - `columnNames` (`String`) — the columns to apply DISTINCTROW to; if null, empty, or blank, plain DISTINCTROW is used
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.builder().distinctRowBy("category, subcategory").build().selectModifier();
    // returns "DISTINCTROW(category, subcategory)"
    
    // null, empty, or blank falls back to a plain DISTINCTROW (no parentheses).
    Criteria.builder().distinctRowBy(null).build().selectModifier();   // returns "DISTINCTROW"
    ```
##### selectModifier(...) -> Builder
- **Signature:** `public Builder selectModifier(final String selectModifier)`
- **Summary:** Sets a custom SELECT modifier.
- **Parameters:**
  - `selectModifier` (`String`) — the custom SELECT modifier; null, empty, or blank means no modifier
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.builder().selectModifier("SQL_CALC_FOUND_ROWS").build().selectModifier();
    // returns "SQL_CALC_FOUND_ROWS"
    
    // Passing null (or an empty/blank string) clears any previously set modifier.
    Criteria.builder().selectModifier(null).build().selectModifier();   // returns null
    ```
##### join(...) -> Builder
- **Signature:** `public Builder join(final Join... joins)`
- **Summary:** Adds JOIN clauses to this criteria.
- **Parameters:**
  - `joins` (`Join[]`) — the JOIN clauses to add
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .join(
            new LeftJoin("orders", new On("users.id", "orders.user_id")),
            new InnerJoin("products", new On("orders.product_id", "products.id"))
        )
        .build();
    c.joins().size();   // returns 2
    
    // Passing no joins is a no-op.
    Criteria empty = Criteria.builder().join(new Join[0]).build();
    empty.joins();      // returns [] (empty list)
    ```
- **Signature:** `public Builder join(final Collection<Join> joins)`
- **Summary:** Adds JOIN clauses to this criteria.
- **Parameters:**
  - `joins` (`Collection<Join>`) — the collection of JOIN clauses to add
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    List<Join> joins = Arrays.asList(
        new LeftJoin("orders", new On("users.id", "orders.user_id")),
        new RightJoin("payments", new On("orders.id", "payments.order_id"))
    );
    Criteria c = Criteria.builder().join(joins).build();
    c.joins().size();   // returns 2
    
    // An empty collection is a no-op.
    Criteria empty = Criteria.builder().join(new ArrayList<Join>()).build();
    empty.joins();      // returns [] (empty list)
    ```
- **Signature:** `public Builder join(final String joinEntity)`
- **Summary:** Adds a plain JOIN (no explicit type keyword) to this criteria, without an explicit condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria criteria = Criteria.builder()
        .join("orders")
        .where(Filters.expr("users.id = orders.user_id"))
        .build();
    // SQL: JOIN orders WHERE users.id = orders.user_id
    ```
- **Signature:** `public Builder join(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds a plain JOIN (no explicit type keyword) with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria criteria = Criteria.builder()
        .join("orders", new On("users.id", "orders.user_id"))
        .where(Filters.equal("users.status", "active"))
        .build();
    // SQL: JOIN orders ON users.id = orders.user_id WHERE users.status = 'active'
    ```
- **Signature:** `public Builder join(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds a plain JOIN (no explicit type keyword) with multiple entities and a condition to this criteria.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Collection<String> tables = Arrays.asList("orders", "order_items");
    Criteria c = Criteria.builder()
        .join(tables, new On("id", "order_id"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " JOIN (orders, order_items) ON id = order_id"
    
    // A single-element collection is rendered without parentheses.
    Criteria c2 = Criteria.builder().join(Arrays.asList("orders"), new On("a", "b")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " JOIN orders ON a = b"
    ```
##### innerJoin(...) -> Builder
- **Signature:** `public Builder innerJoin(final String joinEntity)`
- **Summary:** Adds an INNER JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .innerJoin("orders")
        .where(Filters.expr("users.id = orders.user_id"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders WHERE users.id = orders.user_id"
    
    Criteria bare = Criteria.builder().innerJoin("orders").build();
    bare.toSql(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders"
    ```
- **Signature:** `public Builder innerJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds an INNER JOIN with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .innerJoin("orders", Filters.on("users.id", "orders.user_id"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders ON users.id = orders.user_id"
    
    // The ON value is treated as a property reference, so it is rendered unquoted.
    Criteria c2 = Criteria.builder().innerJoin("orders", Filters.on("a", "b")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders ON a = b"
    ```
- **Signature:** `public Builder innerJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds an INNER JOIN with multiple entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .innerJoin(Arrays.asList("orders", "order_items"), Filters.on("id", "order_id"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN (orders, order_items) ON id = order_id"
    
    // A single-element collection is rendered without parentheses.
    Criteria c2 = Criteria.builder().innerJoin(Arrays.asList("orders"), Filters.on("a", "b")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " INNER JOIN orders ON a = b"
    ```
##### leftJoin(...) -> Builder
- **Signature:** `public Builder leftJoin(final String joinEntity)`
- **Summary:** Adds a LEFT JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().leftJoin("orders").build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders"
    
    Criteria c2 = Criteria.builder().leftJoin("orders").where(Filters.eq("a", 1)).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders WHERE a = 1"
    ```
- **Signature:** `public Builder leftJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds a LEFT JOIN with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .leftJoin("orders", Filters.on("users.id", "orders.user_id"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders ON users.id = orders.user_id"
    
    Criteria c2 = Criteria.builder().leftJoin("orders", Filters.on("a", "b")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders ON a = b"
    ```
- **Signature:** `public Builder leftJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds a LEFT JOIN with multiple entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .leftJoin(Arrays.asList("orders", "items"), Filters.on("a", "b"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN (orders, items) ON a = b"
    
    // A single-element collection is rendered without parentheses.
    Criteria c2 = Criteria.builder().leftJoin(Arrays.asList("orders"), Filters.on("a", "b")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " LEFT JOIN orders ON a = b"
    ```
##### rightJoin(...) -> Builder
- **Signature:** `public Builder rightJoin(final String joinEntity)`
- **Summary:** Adds a RIGHT JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().rightJoin("orders").build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders"
    
    Criteria c2 = Criteria.builder().rightJoin("orders").where(Filters.eq("a", 1)).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders WHERE a = 1"
    ```
- **Signature:** `public Builder rightJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds a RIGHT JOIN with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .rightJoin("orders", Filters.on("users.id", "orders.user_id"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders ON users.id = orders.user_id"
    
    Criteria c2 = Criteria.builder().rightJoin("orders", Filters.on("a", "b")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders ON a = b"
    ```
- **Signature:** `public Builder rightJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds a RIGHT JOIN with multiple entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .rightJoin(Arrays.asList("orders", "items"), Filters.on("a", "b"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN (orders, items) ON a = b"
    
    // A single-element collection is rendered without parentheses.
    Criteria c2 = Criteria.builder().rightJoin(Arrays.asList("orders"), Filters.on("a", "b")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " RIGHT JOIN orders ON a = b"
    ```
##### fullJoin(...) -> Builder
- **Signature:** `public Builder fullJoin(final String joinEntity)`
- **Summary:** Adds a FULL JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().fullJoin("orders").build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders"
    
    Criteria c2 = Criteria.builder().fullJoin("orders").where(Filters.eq("a", 1)).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders WHERE a = 1"
    ```
- **Signature:** `public Builder fullJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds a FULL JOIN with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .fullJoin("orders", Filters.on("users.id", "orders.user_id"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders ON users.id = orders.user_id"
    
    Criteria c2 = Criteria.builder().fullJoin("orders", Filters.on("a", "b")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders ON a = b"
    ```
- **Signature:** `public Builder fullJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds a FULL JOIN with multiple entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .fullJoin(Arrays.asList("orders", "items"), Filters.on("a", "b"))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN (orders, items) ON a = b"
    
    // A single-element collection is rendered without parentheses.
    Criteria c2 = Criteria.builder().fullJoin(Arrays.asList("orders"), Filters.on("a", "b")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " FULL JOIN orders ON a = b"
    ```
##### crossJoin(...) -> Builder
- **Signature:** `public Builder crossJoin(final String joinEntity)`
- **Summary:** Adds a CROSS JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().crossJoin("colors").build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN colors"
    
    Criteria c2 = Criteria.builder().crossJoin("colors").where(Filters.eq("a", 1)).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN colors WHERE a = 1"
    ```
- **Signature:** `public Builder crossJoin(final Collection<String> joinEntities)`
- **Summary:** Adds a conditionless CROSS JOIN for multiple entities.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the entity/table names to cross join
- **Returns:** this builder
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().crossJoin(Arrays.asList("colors", "sizes")).build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN (colors, sizes)"
    
    // A single-element collection is rendered without parentheses.
    Criteria c2 = Criteria.builder().crossJoin(Arrays.asList("colors")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " CROSS JOIN colors"
    ```
##### naturalJoin(...) -> Builder
- **Signature:** `public Builder naturalJoin(final String joinEntity)`
- **Summary:** Adds a NATURAL JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().naturalJoin("employees").build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN employees"
    
    Criteria c2 = Criteria.builder().naturalJoin("employees").where(Filters.eq("a", 1)).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN employees WHERE a = 1"
    ```
- **Signature:** `public Builder naturalJoin(final Collection<String> joinEntities)`
- **Summary:** Adds a conditionless NATURAL JOIN for multiple entities.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the entity/table names to natural join
- **Returns:** this builder
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().naturalJoin(Arrays.asList("employees", "departments")).build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN (employees, departments)"
    
    // A single-element collection is rendered without parentheses.
    Criteria c2 = Criteria.builder().naturalJoin(Arrays.asList("employees")).build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " NATURAL JOIN employees"
    ```
##### where(...) -> Builder
- **Signature:** `public Builder where(final Condition condition)`
- **Summary:** Sets or replaces the WHERE clause.
- **Contract:**
  - If a WHERE clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the WHERE condition (must not be null); if its operator is already Operator#WHERE it is added directly, otherwise it is wrapped in a Where
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .where(Filters.and(Filters.equal("status", "active"), Filters.greaterThan("age", 18)))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " WHERE ((status = 'active') AND (age > 18))"
    
    // Edge cases:
    Criteria.builder().where((Condition) null);   // throws IllegalArgumentException
    Criteria.builder().where(new GroupBy("x"));   // throws IllegalArgumentException (wrong clause)
    ```
- **Signature:** `public Builder where(final String expr)`
- **Summary:** Sets or replaces the WHERE clause using a string expression.
- **Contract:**
  - If a WHERE clause already exists, it will be replaced.
- **Parameters:**
  - `expr` (`String`) — the WHERE condition as a string (must not be null, empty, or blank)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().where("age > 18 AND status = 'active'").build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " WHERE age > 18 AND status = 'active'"
    
    // Edge cases:
    Criteria.builder().where((String) null);   // throws IllegalArgumentException
    Criteria.builder().where("");              // throws IllegalArgumentException
    ```
##### groupByAsc(...) -> Builder
- **Signature:** `public Builder groupByAsc(final String propOrColumnName)`
- **Summary:** Sets or replaces the GROUP BY clause with a single column in ascending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by ascending
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .groupByAsc("category");
    // SQL: GROUP BY category ASC
    ```
- **Signature:** `public Builder groupByAsc(final String... propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with ascending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to group by ascending
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .groupByAsc("category", "brand");
    // SQL: GROUP BY category ASC, brand ASC
    ```
- **Signature:** `public Builder groupByAsc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with ascending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by ascending (use an ordered collection such as List or java.util.LinkedHashSet to preserve the column order)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    List<String> groupCols = Arrays.asList("category", "brand");
    Criteria.Builder builder = Criteria.builder()
        .groupByAsc(groupCols);
    // SQL: GROUP BY category ASC, brand ASC
    ```
##### groupByDesc(...) -> Builder
- **Signature:** `public Builder groupByDesc(final String propOrColumnName)`
- **Summary:** Sets or replaces the GROUP BY clause with a single column in descending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by descending
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .groupByDesc("sales");
    // SQL: GROUP BY sales DESC
    ```
- **Signature:** `public Builder groupByDesc(final String... propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with descending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to group by descending
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .groupByDesc("sales", "region");
    // SQL: GROUP BY sales DESC, region DESC
    ```
- **Signature:** `public Builder groupByDesc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with descending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by descending (use an ordered collection such as List or java.util.LinkedHashSet to preserve the column order)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    List<String> groupCols = Arrays.asList("sales", "region");
    Criteria.Builder builder = Criteria.builder()
        .groupByDesc(groupCols);
    // SQL: GROUP BY sales DESC, region DESC
    ```
##### groupBy(...) -> Builder
- **Signature:** `public Builder groupBy(final Condition condition)`
- **Summary:** Sets or replaces the GROUP BY clause.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the GROUP BY condition (must not be null); if its operator is already Operator#GROUP_BY it is added directly, otherwise it is wrapped in a GroupBy
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().groupBy(Filters.expr("YEAR(order_date)")).build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " GROUP BY YEAR(order_date)"
    
    Criteria.builder().groupBy((Condition) null);   // throws IllegalArgumentException
    ```
- **Signature:** `public Builder groupBy(final String... propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with property names.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to group by
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .groupBy("department", "location", "role");
    // SQL: GROUP BY department, location, role
    ```
- **Signature:** `public Builder groupBy(final String propName, final SortDirection direction)`
- **Summary:** Sets or replaces the GROUP BY clause with a property and sort direction.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the property name to group by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .groupBy("total_sales", SortDirection.DESC);
    // SQL: GROUP BY total_sales DESC
    ```
- **Signature:** `public Builder groupBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2)`
- **Summary:** Sets or replaces the GROUP BY clause with two properties and their sort directions.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the first property name to group by
  - `direction` (`SortDirection`) — the sort direction for the first property
  - `propName2` (`String`) — the second property name to group by
  - `direction2` (`SortDirection`) — the sort direction for the second property
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .groupBy("year", SortDirection.DESC, "month", SortDirection.ASC);
    // SQL: GROUP BY year DESC, month ASC
    ```
- **Signature:** `public Builder groupBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2, final String propName3, final SortDirection direction3)`
- **Summary:** Sets or replaces the GROUP BY clause with three properties and their sort directions.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the first property name to group by
  - `direction` (`SortDirection`) — the sort direction for the first property
  - `propName2` (`String`) — the second property name to group by
  - `direction2` (`SortDirection`) — the sort direction for the second property
  - `propName3` (`String`) — the third property name to group by
  - `direction3` (`SortDirection`) — the sort direction for the third property
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .groupBy("country", SortDirection.ASC, "state", SortDirection.ASC, "city", SortDirection.DESC);
    // SQL: GROUP BY country ASC, state ASC, city DESC
    ```
- **Signature:** `public Builder groupBy(final Collection<String> propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with multiple properties.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by (use an ordered collection such as List or java.util.LinkedHashSet to preserve the column order; must not be null or empty)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    List<String> groupCols = Arrays.asList("region", "product_type");
    Criteria c = Criteria.builder().groupBy(groupCols).build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " GROUP BY region, product_type"
    
    Criteria.builder().groupBy((Collection<String>) null);   // throws IllegalArgumentException
    ```
- **Signature:** `public Builder groupBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Sets or replaces the GROUP BY clause with multiple properties and sort direction.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by (use an ordered collection such as List or java.util.LinkedHashSet to preserve the column order)
  - `direction` (`SortDirection`) — the sort direction for all properties
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    List<String> groupCols = Arrays.asList("category", "brand");
    Criteria.Builder builder = Criteria.builder()
        .groupBy(groupCols, SortDirection.DESC);
    // SQL: GROUP BY category DESC, brand DESC
    ```
- **Signature:** `public Builder groupBy(final Map<String, SortDirection> groupings)`
- **Summary:** Sets or replaces the GROUP BY clause with custom sort directions per property.
- **Contract:**
  - The map should be a LinkedHashMap to preserve order.
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `groupings` (`Map<String, SortDirection>`) — a map of property names to sort directions
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Map<String, SortDirection> grouping = new LinkedHashMap<>();
    grouping.put("department", SortDirection.ASC);
    grouping.put("salary_range", SortDirection.DESC);
    grouping.put("years_experience", SortDirection.DESC);
    Criteria.Builder builder = Criteria.builder()
        .groupBy(grouping);
    // SQL: GROUP BY department ASC, salary_range DESC, years_experience DESC
    ```
##### having(...) -> Builder
- **Signature:** `public Builder having(final Condition condition)`
- **Summary:** Sets or replaces the HAVING clause.
- **Contract:**
  - If a HAVING clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the HAVING condition (must not be null); if its operator is already Operator#HAVING it is added directly, otherwise it is wrapped in a Having
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .groupBy("department")
        .having(Filters.and(Filters.greaterThan("COUNT(*)", 10), Filters.lessThan("AVG(salary)", 100000)))
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);
    // returns " GROUP BY department HAVING ((COUNT(*) > 10) AND (AVG(salary) < 100000))"
    
    Criteria.builder().having((Condition) null);   // throws IllegalArgumentException
    ```
- **Signature:** `public Builder having(final String expr)`
- **Summary:** Sets or replaces the HAVING clause using a string expression.
- **Contract:**
  - If a HAVING clause already exists, it will be replaced.
- **Parameters:**
  - `expr` (`String`) — the HAVING condition as a string (must not be null, empty, or blank)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .groupBy("product_category")
        .having("SUM(revenue) > 10000 AND COUNT(*) > 5")
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);
    // returns " GROUP BY product_category HAVING SUM(revenue) > 10000 AND COUNT(*) > 5"
    
    Criteria.builder().having("");   // throws IllegalArgumentException
    ```
##### orderByAsc(...) -> Builder
- **Signature:** `public Builder orderByAsc(final String propOrColumnName)`
- **Summary:** Sets or replaces the ORDER BY clause with a single column in ascending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by ascending
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .orderByAsc("lastName");
    // SQL: ORDER BY lastName ASC
    ```
- **Signature:** `public Builder orderByAsc(final String... propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with ascending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to order by ascending
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .orderByAsc("lastName", "firstName", "middleName");
    // SQL: ORDER BY lastName ASC, firstName ASC, middleName ASC
    ```
- **Signature:** `public Builder orderByAsc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with ascending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by ascending (use an ordered collection such as List or java.util.LinkedHashSet to preserve the column order)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    List<String> sortCols = Arrays.asList("country", "state", "city");
    Criteria.Builder builder = Criteria.builder()
        .orderByAsc(sortCols);
    // SQL: ORDER BY country ASC, state ASC, city ASC
    ```
##### orderByDesc(...) -> Builder
- **Signature:** `public Builder orderByDesc(final String propOrColumnName)`
- **Summary:** Sets or replaces the ORDER BY clause with a single column in descending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by descending
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .orderByDesc("score");
    // SQL: ORDER BY score DESC
    ```
- **Signature:** `public Builder orderByDesc(final String... propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with descending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to order by descending
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .orderByDesc("score", "createdDate");
    // SQL: ORDER BY score DESC, createdDate DESC
    ```
- **Signature:** `public Builder orderByDesc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with descending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by descending (use an ordered collection such as List or java.util.LinkedHashSet to preserve the column order)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    List<String> sortCols = Arrays.asList("revenue", "profit");
    Criteria.Builder builder = Criteria.builder()
        .orderByDesc(sortCols);
    // SQL: ORDER BY revenue DESC, profit DESC
    ```
##### orderBy(...) -> Builder
- **Signature:** `public Builder orderBy(final Condition condition)`
- **Summary:** Sets or replaces the ORDER BY clause.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the ORDER BY condition (must not be null); if its operator is already Operator#ORDER_BY it is added directly, otherwise it is wrapped in an OrderBy
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().orderBy(Filters.expr("created_date DESC")).build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " ORDER BY created_date DESC"
    
    Criteria.builder().orderBy((Condition) null);   // throws IllegalArgumentException
    ```
- **Signature:** `public Builder orderBy(final String... propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with property names.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to order by
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .orderBy("department", "lastName", "firstName");
    // SQL: ORDER BY department, lastName, firstName
    ```
- **Signature:** `public Builder orderBy(final String propName, final SortDirection direction)`
- **Summary:** Sets or replaces the ORDER BY clause with a property and sort direction.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the property name to order by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .orderBy("createdDate", SortDirection.DESC);
    // SQL: ORDER BY createdDate DESC
    ```
- **Signature:** `public Builder orderBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2)`
- **Summary:** Sets or replaces the ORDER BY clause with two properties and their sort directions.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the first property name to order by
  - `direction` (`SortDirection`) — the sort direction for the first property
  - `propName2` (`String`) — the second property name to order by
  - `direction2` (`SortDirection`) — the sort direction for the second property
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .orderBy("priority", SortDirection.DESC, "createdDate", SortDirection.ASC);
    // SQL: ORDER BY priority DESC, createdDate ASC
    ```
- **Signature:** `public Builder orderBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2, final String propName3, final SortDirection direction3)`
- **Summary:** Sets or replaces the ORDER BY clause with three properties and their sort directions.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the first property name to order by
  - `direction` (`SortDirection`) — the sort direction for the first property
  - `propName2` (`String`) — the second property name to order by
  - `direction2` (`SortDirection`) — the sort direction for the second property
  - `propName3` (`String`) — the third property name to order by
  - `direction3` (`SortDirection`) — the sort direction for the third property
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria.Builder builder = Criteria.builder()
        .orderBy("category", SortDirection.ASC, "price", SortDirection.DESC, "name", SortDirection.ASC);
    // SQL: ORDER BY category ASC, price DESC, name ASC
    ```
- **Signature:** `public Builder orderBy(final Collection<String> propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with multiple properties.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by (use an ordered collection such as List or java.util.LinkedHashSet to preserve the column order; must not be null or empty)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    List<String> sortCols = Arrays.asList("country", "state", "city");
    Criteria c = Criteria.builder().orderBy(sortCols).build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " ORDER BY country, state, city"
    
    Criteria.builder().orderBy((Collection<String>) null);   // throws IllegalArgumentException
    ```
- **Signature:** `public Builder orderBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Sets or replaces the ORDER BY clause with multiple properties and sort direction.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by (use an ordered collection such as List or java.util.LinkedHashSet to preserve the column order)
  - `direction` (`SortDirection`) — the sort direction for all properties
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    List<String> sortCols = Arrays.asList("score", "rating");
    Criteria.Builder builder = Criteria.builder()
        .orderBy(sortCols, SortDirection.DESC);
    // SQL: ORDER BY score DESC, rating DESC
    ```
- **Signature:** `public Builder orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Sets or replaces the ORDER BY clause with custom sort directions per property.
- **Contract:**
  - The map should be a LinkedHashMap to preserve order.
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — a map of property names to sort directions
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Map<String, SortDirection> ordering = new LinkedHashMap<>();
    ordering.put("priority", SortDirection.DESC);
    ordering.put("createdDate", SortDirection.DESC);
    ordering.put("name", SortDirection.ASC);
    Criteria.Builder builder = Criteria.builder()
        .orderBy(ordering);
    // SQL: ORDER BY priority DESC, createdDate DESC, name ASC
    ```
##### limit(...) -> Builder
- **Signature:** `public Builder limit(final Limit condition)`
- **Summary:** Sets or replaces the LIMIT clause.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Limit`) — the LIMIT condition (must not be null); its operator must be Operator#LIMIT, which is guaranteed for any Limit instance
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().limit(Filters.limit(100)).build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " LIMIT 100"
    
    Criteria.builder().limit((Limit) null);   // throws IllegalArgumentException
    ```
- **Signature:** `public Builder limit(final int count)`
- **Summary:** Sets or replaces the LIMIT clause with a count.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `count` (`int`) — the maximum number of results to return
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria criteria = Criteria.builder()
        .where(Filters.equal("status", "active"))
        .limit(10)
        .build();
    // SQL: WHERE status = 'active' LIMIT 10
    ```
- **Signature:** `public Builder limit(final int count, final int offset)`
- **Summary:** Sets or replaces the LIMIT clause with count and offset.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `count` (`int`) — the maximum number of results to return
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    // Page 3 with 20 items per page (take 20, skip 40)
    Criteria criteria = Criteria.builder()
        .orderBy("id")
        .limit(20, 40)
        .build();
    // SQL: ORDER BY id LIMIT 20 OFFSET 40
    ```
- **Signature:** `public Builder limit(final String expr)`
- **Summary:** Sets or replaces the LIMIT clause using a string expression.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
  - When rendered by a SQL builder whose dialect paginates with OFFSET/FETCH (Oracle, DB2 or SQL Server), a generic LIMIT count \[OFFSET offset\] expression is re-rendered in that dialect's syntax.
- **Parameters:**
  - `expr` (`String`) — the LIMIT expression as a string
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder().limit("10 OFFSET 20").build();
    c.toSql(NamingPolicy.NO_CHANGE);   // returns " LIMIT 10 OFFSET 20"
    
    // Placeholder form for parameterized queries.
    Criteria c2 = Criteria.builder().limit("? OFFSET ?").build();
    c2.toSql(NamingPolicy.NO_CHANGE);   // returns " LIMIT ? OFFSET ?"
    ```
##### union(...) -> Builder
- **Signature:** `public Builder union(final SubQuery subQuery)`
- **Summary:** Adds a UNION operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with (must not be null)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    SubQuery archivedUsers = Filters.subQuery("SELECT * FROM archived_users WHERE active = true");
    Criteria c = Criteria.builder()
        .where(Filters.equal("status", "active"))
        .union(archivedUsers)
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);
    // returns " WHERE status = 'active' UNION SELECT * FROM archived_users WHERE active = true"
    
    // Multiple set operations accumulate in order.
    c.setOperations().size();   // returns 1
    ```
##### unionAll(...) -> Builder
- **Signature:** `public Builder unionAll(final SubQuery subQuery)`
- **Summary:** Adds a UNION ALL operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with (must not be null)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    SubQuery pendingOrders = Filters.subQuery("SELECT * FROM pending_orders");
    Criteria c = Criteria.builder()
        .where(Filters.equal("status", "completed"))
        .unionAll(pendingOrders)
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);
    // returns " WHERE status = 'completed' UNION ALL SELECT * FROM pending_orders"
    ```
##### intersect(...) -> Builder
- **Signature:** `public Builder intersect(final SubQuery subQuery)`
- **Summary:** Adds an INTERSECT operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to intersect with (must not be null)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    SubQuery premiumUsers = Filters.subQuery("SELECT user_id FROM premium_members");
    Criteria c = Criteria.builder()
        .where(Filters.equal("active", true))
        .intersect(premiumUsers)
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);
    // returns " WHERE active = true INTERSECT SELECT user_id FROM premium_members"
    ```
##### except(...) -> Builder
- **Signature:** `public Builder except(final SubQuery subQuery)`
- **Summary:** Adds an EXCEPT operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the right-hand subquery for the EXCEPT operation (must not be null)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    SubQuery excludedUsers = Filters.subQuery("SELECT user_id FROM blacklist");
    Criteria c = Criteria.builder()
        .where(Filters.equal("status", "active"))
        .except(excludedUsers)
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);
    // returns " WHERE status = 'active' EXCEPT SELECT user_id FROM blacklist"
    ```
##### minus(...) -> Builder
- **Signature:** `public Builder minus(final SubQuery subQuery)`
- **Summary:** Adds a MINUS operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the right-hand subquery for the MINUS operation (must not be null)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    SubQuery inactiveUsers = Filters.subQuery("SELECT user_id FROM inactive_users");
    Criteria c = Criteria.builder()
        .where(Filters.equal("registered", true))
        .minus(inactiveUsers)
        .build();
    c.toSql(NamingPolicy.NO_CHANGE);
    // returns " WHERE registered = true MINUS SELECT user_id FROM inactive_users"
    ```
##### add(...) -> Builder
- **Signature:** `public Builder add(final Condition condition)`
- **Summary:** Adds a condition to this builder, routing it to the appropriate clause based on its operator.
- **Parameters:**
  - `condition` (`Condition`) — the condition to add (must not be null)
- **Returns:** this Builder instance for method chaining
- **Examples:**
  - ```java
    Criteria c = Criteria.builder()
        .add(Filters.equal("status", "active"))   // -> WHERE status = 'active'
        .add(new OrderBy("createdDate"))          // -> ORDER BY createdDate
        .add(new Limit(10))                       // -> LIMIT 10
        .build();
    ```
##### build(...) -> Criteria
- **Signature:** `public Criteria build()`
- **Summary:** Builds and returns the Criteria instance from the configured conditions.
- **Parameters:**
  - (none)
- **Returns:** a new Criteria instance
- **Examples:**
  - ```java
    Criteria criteria = Criteria.builder()
        .where(Filters.equal("active", true))
        .orderBy("createdDate", SortDirection.DESC)
        .limit(100)
        .build();
    ```

### Class CrossJoin (com.landawn.abacus.query.condition.CrossJoin)
Represents a CROSS JOIN operation in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public CrossJoin(final String joinEntity)` — Creates a CROSS JOIN clause for the specified table or entity.
- `public CrossJoin(final Collection<String> joinEntities)` — Creates a CROSS JOIN clause with multiple tables/entities and no join condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Equal (com.landawn.abacus.query.condition.Equal)
Represents an equality (=) condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Equal(final String propName, final Object propValue)` — Creates a new Equal condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Except (com.landawn.abacus.query.condition.Except)
Represents an EXCEPT set operation in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Except(final SubQuery subQuery)` — Creates a new EXCEPT clause with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this EXCEPT clause.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT employee_id FROM employees WHERE is_manager = true");
    Except except = new Except(subQuery);
    SubQuery retrieved = except.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = except.subQuery() == except.condition();
    // returns true
    ```

### Class Exists (com.landawn.abacus.query.condition.Exists)
Represents the SQL EXISTS operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Exists(final SubQuery subQuery)` — Creates a new EXISTS condition with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this EXISTS condition.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
    Exists exists = new Exists(subQuery);
    SubQuery retrieved = exists.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = exists.subQuery() == exists.condition();
    // returns true
    ```

### Class FullJoin (com.landawn.abacus.query.condition.FullJoin)
Represents a FULL JOIN (a.k.a.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public FullJoin(final String joinEntity)` — Creates a FULL JOIN clause for the specified table or entity without a join condition.
- `public FullJoin(final String joinEntity, final Condition joinCondition)` — Creates a FULL JOIN clause with a join condition.
- `public FullJoin(final Collection<String> joinEntities, final Condition joinCondition)` — Creates a FULL JOIN clause with multiple tables/entities and a join condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class GreaterThan (com.landawn.abacus.query.condition.GreaterThan)
Represents a greater-than (&gt;) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public GreaterThan(final String propName, final Object propValue)` — Creates a new GreaterThan condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class GreaterThanOrEqual (com.landawn.abacus.query.condition.GreaterThanOrEqual)
Represents a greater-than-or-equal-to (&gt;=) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public GreaterThanOrEqual(final String propName, final Object propValue)` — Creates a new GreaterThanOrEqual condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class GroupBy (com.landawn.abacus.query.condition.GroupBy)
Represents a GROUP BY clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public GroupBy(final Condition condition)` — Creates a new GROUP BY clause with the specified condition.
- `public GroupBy(final String... propNames)` — Creates a new GROUP BY clause with the specified property names.
- `public GroupBy(final Collection<String> propNames)` — Creates a new GROUP BY clause with the property names supplied as a collection.
- `public GroupBy(final String propOrColumnName, final SortDirection direction)` — Creates a new GROUP BY clause with a single property and sort direction.
- `public GroupBy(final Collection<String> propNames, final SortDirection direction)` — Creates a new GROUP BY clause with multiple properties and a single sort direction.
- `public GroupBy(final Map<String, SortDirection> groupings)` — Creates a new GROUP BY clause with custom sort directions for each property.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Having (com.landawn.abacus.query.condition.Having)
Represents a HAVING clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Having(final Condition condition)` — Creates a new HAVING clause with the specified condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class In (com.landawn.abacus.query.condition.In)
Represents an IN condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public In(final String propName, final Collection<?> values)` — Creates a new IN condition with the specified property name and collection of values.
- `public In(final Collection<String> propNames, final Collection<?> valueRows)` — Creates a new row value constructor IN condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class InSubQuery (com.landawn.abacus.query.condition.InSubQuery)
Represents an IN condition with a subquery in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public InSubQuery(final String propName, final SubQuery subQuery)` — Creates an IN subquery condition for a single property.
- `public InSubQuery(final Collection<String> propNames, final SubQuery subQuery)` — Creates an IN subquery condition for multiple properties.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class InnerJoin (com.landawn.abacus.query.condition.InnerJoin)
Represents an INNER JOIN clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public InnerJoin(final String joinEntity)` — Creates an INNER JOIN clause for the specified table or entity without a join condition.
- `public InnerJoin(final String joinEntity, final Condition joinCondition)` — Creates an INNER JOIN clause with a join condition.
- `public InnerJoin(final Collection<String> joinEntities, final Condition joinCondition)` — Creates an INNER JOIN clause with multiple tables/entities and a join condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Intersect (com.landawn.abacus.query.condition.Intersect)
Represents an INTERSECT clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Intersect(final SubQuery subQuery)` — Creates a new INTERSECT clause with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this INTERSECT clause.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT user_id FROM activity WHERE last_login > CURRENT_DATE - 30");
    Intersect intersect = new Intersect(subQuery);
    SubQuery retrieved = intersect.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = intersect.subQuery() == intersect.condition();
    // returns true
    ```

### Class Is (com.landawn.abacus.query.condition.Is)
Represents an SQL IS predicate (e.g.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Is(final String propName, final Object propValue)` — Creates a new IS condition with the specified property name and right-hand value.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class IsInfinite (com.landawn.abacus.query.condition.IsInfinite)
Represents a condition that checks if a numeric property value is infinite.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public IsInfinite(final String propName)` — Creates a new IsInfinite condition for the specified property.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class IsNaN (com.landawn.abacus.query.condition.IsNaN)
Represents a condition that checks if a numeric property value is NaN (Not a Number).

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public IsNaN(final String propName)` — Creates a new IsNaN condition for the specified property.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class IsNot (com.landawn.abacus.query.condition.IsNot)
Represents an SQL IS NOT predicate (e.g.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public IsNot(final String propName, final Object propValue)` — Creates a new IS NOT condition with the specified property name and right-hand value.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class IsNotInfinite (com.landawn.abacus.query.condition.IsNotInfinite)
Represents a condition that checks if a numeric property value is NOT infinite.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public IsNotInfinite(final String propName)` — Creates a new IsNotInfinite condition for the specified property.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class IsNotNaN (com.landawn.abacus.query.condition.IsNotNaN)
Represents a condition that checks if a numeric property value is NOT NaN (Not a Number).

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public IsNotNaN(final String propName)` — Creates a new IsNotNaN condition for the specified property.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class IsNotNull (com.landawn.abacus.query.condition.IsNotNull)
Represents a condition that checks if a property value is NOT NULL.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public IsNotNull(final String propName)` — Creates a new IsNotNull condition for the specified property.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class IsNull (com.landawn.abacus.query.condition.IsNull)
Represents a condition that checks if a property value is NULL.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public IsNull(final String propName)` — Creates a new IsNull condition for the specified property.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Join (com.landawn.abacus.query.condition.Join)
Base class for SQL JOIN operations.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Join(final String joinEntity)` — Creates a simple JOIN clause for the specified table or entity.
- `public Join(final String joinEntity, final Condition joinCondition)` — Creates a JOIN clause with a condition.
- `public Join(final Collection<String> joinEntities, final Condition joinCondition)` — Creates a JOIN clause with multiple tables or entities and a condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### joinEntities(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> joinEntities()`
- **Summary:** Returns the list of tables or entities involved in this join.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of join entities
- **Examples:**
  - ```java
    // Single table join
    Join join = new Join("orders o", new On("customers.id", "o.customer_id"));
    List<String> entities = join.joinEntities();
    // entities = ["orders o"]
    
    // Multi-table join
    Join multiJoin = new Join(Arrays.asList("orders o", "order_items oi"),
        new On("o.id", "oi.order_id"));
    List<String> multiEntities = multiJoin.joinEntities();
    // multiEntities = ["orders o", "order_items oi"]
    
    // Edge: the returned list is immutable
    entities.add("more");   // throws UnsupportedOperationException
    ```
##### condition(...) -> Condition
- **Signature:** `public Condition condition()`
- **Summary:** Returns the join condition.
- **Contract:**
  - Returns the condition that specifies how the tables are related, or null if no condition was supplied at construction time.
  - Callers that need a more specific subtype must cast explicitly.
- **Parameters:**
  - (none)
- **Returns:** the join condition, or null if no condition was specified
- **Examples:**
  - ```java
    // Join with ON condition
    On onCondition = new On("customers.id", "o.customer_id");
    Join join = new Join("orders o", onCondition);
    On condition = (On) join.condition();
    // condition == onCondition (the same On instance is returned)
    
    // Join without condition
    Join simpleJoin = new Join("products");
    Condition noCondition = simpleJoin.condition();
    // noCondition == null
    
    // Edge: the condition is returned as-is; an incompatible cast fails
    Using bad = (Using) join.condition();   // throws ClassCastException
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns all parameters from the join condition.
- **Contract:**
  - Returns an empty list if there's no condition or the condition has no parameters.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameters from the condition, or an empty immutable list if no condition
- **Examples:**
  - ```java
    // Condition with a bound value
    Join valueJoin = new Join("products p", Filters.equal("p.active", true));
    valueJoin.parameters();    // returns [true]
    
    // Edge: an ON condition compares columns and has no bound parameters
    Join onJoin = new Join("orders o", new On("customers.id", "o.customer_id"));
    onJoin.parameters();       // returns [] (empty, immutable)
    
    // Edge: no condition at all -> empty list
    new Join("products").parameters();   // returns []
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this JOIN clause to its SQL representation, propagating the specified naming policy to the join condition.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy passed through to the join condition's Condition#toSql(NamingPolicy) method; if null, the nested condition treats it as NamingPolicy#NO_CHANGE
- **Returns:** the SQL representation, e.g., "JOIN orders o ON customers.id = o.customer_id"
- **Examples:**
  - ```java
    Join join = new Join("orders o", new On("customers.id", "o.customer_id"));
    join.toSql(NamingPolicy.NO_CHANGE);
    // returns "JOIN orders o ON customers.id = o.customer_id"
    
    // Edge: naming policy rewrites property names within the condition
    Join snake = new Join("orders o", Filters.equal("firstName", "John"));
    snake.toSql(NamingPolicy.SNAKE_CASE);
    // returns "JOIN orders o ON first_name = 'John'"
    
    // Edge: no condition -> just the operator and entity
    new Join("products").toSql(NamingPolicy.NO_CHANGE);   // returns "JOIN products"
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Computes the hash code for this JOIN clause.
- **Parameters:**
  - (none)
- **Returns:** hash code based on operator, join entities, and condition
- **Examples:**
  - ```java
    Join a = new Join("orders o", new On("a.id", "b.id"));
    Join b = new Join("orders o", new On("a.id", "b.id"));
    a.hashCode() == b.hashCode();   // true (same operator, entities, and condition)
    
    // Edge: a different join entity produces a different hash code
    Join c = new Join("customers c", new On("a.id", "b.id"));
    a.hashCode() == c.hashCode();   // (typically) false
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this JOIN clause is equal to another object.
- **Contract:**
  - Checks if this JOIN clause is equal to another object.
  - Two Join instances are equal if they have the same operator, join entities, and condition.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the object is of the same class with the same operator, entities, and condition
- **Examples:**
  - ```java
    Join a = new Join("orders o", new On("a.id", "b.id"));
    Join b = new Join("orders o", new On("a.id", "b.id"));
    a.equals(b);    // returns true
    
    // Edge: different join entity -> not equal
    Join c = new Join("customers c", new On("a.id", "b.id"));
    a.equals(c);    // returns false
    
    // Edge: a different join type (operator) -> not equal
    Join left = new LeftJoin("orders o", new On("a.id", "b.id"));
    a.equals(left); // returns false
    
    a.equals(null); // returns false
    ```

### Class Junction (com.landawn.abacus.query.condition.Junction)
Base class for composable junction conditions that combine multiple conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Junction(final Operator operator, final Condition... conditions)` — Creates a new Junction with the specified operator and conditions.
- `public Junction(final Operator operator, final Collection<? extends Condition> conditions)` — Creates a new Junction with the specified operator and collection of conditions.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### conditions(...) -> ImmutableList<Condition>
- **Signature:** `public ImmutableList<Condition> conditions()`
- **Summary:** Returns the list of conditions contained in this junction.
- **Parameters:**
  - (none)
- **Returns:** an immutable view of the list of conditions in this junction
- **Examples:**
  - ```java
    Junction and = new Junction(Operator.AND,
        new Equal("status", "active"),
        new GreaterThan("age", 18));
    List<Condition> conditions = and.conditions();
    // conditions = [Equal("status", "active"), GreaterThan("age", 18)]
    conditions.size();             // returns 2
    conditions.get(0).toString();  // returns "status = 'active'"
    
    // Edge: the returned view is immutable
    conditions.add(new Equal("x", 1));   // throws UnsupportedOperationException
    
    // Edge: an empty junction returns an empty list
    new Junction(Operator.AND).conditions().isEmpty();   // returns true
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns all parameters from all conditions in this junction.
- **Parameters:**
  - (none)
- **Returns:** an immutable list containing all parameters from all conditions
- **Examples:**
  - ```java
    Junction and = new Junction(Operator.AND,
        new Equal("status", "active"),
        new Between("age", 18, 65));
    and.parameters();          // returns ["active", 18, 65]
    
    // Edge: an empty junction has no parameters
    Junction empty = new Junction(Operator.AND);
    empty.parameters();        // returns [] (empty, immutable)
    
    // Edge: conditions without bound values contribute nothing
    Junction noParams = new Junction(Operator.OR, new IsNotNull("email"));
    noParams.parameters();     // returns []
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this junction to its SQL representation according to the specified naming policy.
- **Contract:**
  - Any null entries in the conditions list are skipped, and an empty string is returned if the junction has no conditions or every condition is null.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names within each condition; if null, com.landawn.abacus.util.NamingPolicy#NO_CHANGE is used
- **Returns:** the SQL representation with proper parentheses and spacing, or an empty string if no non-null conditions are present
- **Examples:**
  - ```java
    Junction and = new Junction(Operator.AND,
        new Equal("status", "active"),
        new GreaterThan("age", 18));
    and.toSql(NamingPolicy.NO_CHANGE);
    // returns "((status = 'active') AND (age > 18))"
    
    // Edge: a single condition is still doubly parenthesized
    Junction single = new Junction(Operator.OR, new Equal("x", 1));
    single.toSql(NamingPolicy.NO_CHANGE);   // returns "((x = 1))"
    
    // Edge: naming policy rewrites property names
    Junction snake = new Junction(Operator.AND, new Equal("firstName", "John"));
    snake.toSql(NamingPolicy.SNAKE_CASE);   // returns "((first_name = 'John'))"
    
    // Edge: an empty junction renders as an empty string
    new Junction(Operator.AND).toSql(NamingPolicy.NO_CHANGE);   // returns ""
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Computes the hash code for this junction based on its operator and conditions.
- **Parameters:**
  - (none)
- **Returns:** hash code based on operator and condition list
- **Examples:**
  - ```java
    Junction a = new Junction(Operator.AND, new Equal("status", "active"));
    Junction b = new Junction(Operator.AND, new Equal("status", "active"));
    a.hashCode() == b.hashCode();   // true (same operator and conditions)
    
    // Edge: a different operator produces a different hash code
    Junction c = new Junction(Operator.OR, new Equal("status", "active"));
    a.hashCode() == c.hashCode();   // (typically) false
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this junction is equal to another object.
- **Contract:**
  - Checks if this junction is equal to another object.
  - Two junctions are considered equal if they have the same operator and contain the same conditions in the same order.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the object is of the same class with the same operator and conditions
- **Examples:**
  - ```java
    Junction a = new Junction(Operator.AND, new Equal("status", "active"));
    Junction b = new Junction(Operator.AND, new Equal("status", "active"));
    a.equals(b);    // returns true
    
    // Edge: a different operator -> not equal
    Junction c = new Junction(Operator.OR, new Equal("status", "active"));
    a.equals(c);    // returns false
    
    // Edge: same conditions in a different order -> not equal
    Junction d = new Junction(Operator.AND, new Equal("a", 1), new Equal("b", 2));
    Junction e = new Junction(Operator.AND, new Equal("b", 2), new Equal("a", 1));
    d.equals(e);    // returns false
    
    a.equals(null); // returns false
    ```

### Class LeftJoin (com.landawn.abacus.query.condition.LeftJoin)
Represents a LEFT JOIN clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public LeftJoin(final String joinEntity)` — Creates a LEFT JOIN clause for the specified table or entity without a join condition.
- `public LeftJoin(final String joinEntity, final Condition joinCondition)` — Creates a LEFT JOIN clause with a join condition.
- `public LeftJoin(final Collection<String> joinEntities, final Condition joinCondition)` — Creates a LEFT JOIN clause with multiple tables/entities and a join condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class LessThan (com.landawn.abacus.query.condition.LessThan)
Represents a less-than (&lt;) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public LessThan(final String propName, final Object propValue)` — Creates a new LessThan condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class LessThanOrEqual (com.landawn.abacus.query.condition.LessThanOrEqual)
Represents a less-than-or-equal-to (&lt;=) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public LessThanOrEqual(final String propName, final Object propValue)` — Creates a new LessThanOrEqual condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Like (com.landawn.abacus.query.condition.Like)
Represents a LIKE condition in SQL queries for pattern matching.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Like(final String propName, final Object propValue)` — Creates a new LIKE condition with the specified property name and pattern.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Limit (com.landawn.abacus.query.condition.Limit)
Models a SQL row-limiting clause.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Limit(final int count)` — Creates LIMIT count with no offset.
- `public Limit(final int count, final int offset)` — Creates LIMIT count OFFSET offset.
- `public Limit(final String expr)` — Creates a row-limiting clause from a validated SQL expression.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### expression(...) -> String
- **Signature:** `public String expression()`
- **Summary:** Returns the normalized expression supplied to #Limit(String).
- **Contract:**
  - The expression is retained even when its count and offset were successfully resolved.
- **Parameters:**
  - (none)
- **Returns:** the normalized expression, or null if this instance was created numerically
- **See also:** #hasExpression()
##### hasExpression(...) -> boolean
- **Signature:** `public boolean hasExpression()`
- **Summary:** Tests whether this instance was created from an expression.
- **Parameters:**
  - (none)
- **Returns:** true if #expression() is non-null
- **See also:** #isResolved()
##### count(...) -> int
- **Signature:** `public int count()`
- **Summary:** Returns the maximum number of rows to return.
- **Contract:**
  - API note: Prefer #resolvedCount() when Integer#MAX_VALUE could be a legitimate count.
- **Parameters:**
  - (none)
- **Returns:** the resolved count, Integer#MAX_VALUE when an expression is unresolved, or zero for an uninitialized serialization instance
##### offset(...) -> int
- **Signature:** `public int offset()`
- **Summary:** Returns the number of rows to skip.
- **Parameters:**
  - (none)
- **Returns:** the resolved offset, or zero when no offset is specified, the expression is unresolved, or this is an uninitialized serialization instance
##### isResolved(...) -> boolean
- **Signature:** `public boolean isResolved()`
- **Summary:** Tests whether both the count and offset are available as int values.
- **Parameters:**
  - (none)
- **Returns:** true if #resolvedCount() and #resolvedOffset() are both present
##### resolvedCount(...) -> OptionalInt
- **Signature:** `public OptionalInt resolvedCount()`
- **Summary:** Returns the row count when it can be represented as an int.
- **Contract:**
  - Returns the row count when it can be represented as an int.
- **Parameters:**
  - (none)
- **Returns:** the resolved count, or an empty optional for an unresolved expression
##### resolvedOffset(...) -> OptionalInt
- **Signature:** `public OptionalInt resolvedOffset()`
- **Summary:** Returns the row offset when it can be represented as an int.
- **Contract:**
  - Returns the row offset when it can be represented as an int.
- **Parameters:**
  - (none)
- **Returns:** the resolved offset, or an empty optional for an unresolved expression
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns an empty parameter list.
- **Parameters:**
  - (none)
- **Returns:** an empty immutable list
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Returns the normalized, dialect-independent SQL representation.
- **Contract:**
  - A numeric instance returns LIMIT count, followed by OFFSET offset when the offset is greater than zero.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — naming policy; ignored and may be null
- **Returns:** normalized SQL for this limit, or "null" for an uninitialized instance
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code based on the operator and either the normalized expression or the numeric count and offset.
- **Parameters:**
  - (none)
- **Returns:** this limit's hash code
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares limits by representation and value.
- **Contract:**
  - The two representations are intentionally not equal even when they produce identical SQL text.
- **Parameters:**
  - `obj` (`Object`) — object to compare
- **Returns:** true if obj is a Limit with the same operator and representation

### Class Minus (com.landawn.abacus.query.condition.Minus)
Represents a MINUS clause in SQL queries (also known as EXCEPT in some databases).

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Minus(final SubQuery subQuery)` — Creates a new MINUS clause with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this MINUS clause.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT product_id FROM sales");
    Minus minus = new Minus(subQuery);
    SubQuery retrieved = minus.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = minus.subQuery() == minus.condition();
    // returns true
    ```

### Class NamedProperty (com.landawn.abacus.query.condition.NamedProperty)
A utility class that provides a fluent API for creating SQL conditions based on a property name.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public NamedProperty(final String propName)` — Creates a NamedProperty with the specified property name.

#### Public Static Methods
##### of(...) -> NamedProperty
- **Signature:** `public static NamedProperty of(final String propName)`
- **Summary:** Returns or atomically creates a cached NamedProperty instance for the specified property name.
- **Parameters:**
  - `propName` (`String`) — the property name. Must not be null, empty, or blank. A null argument causes an IllegalArgumentException (null is treated like a blank string by the internal Strings.isBlank check).
- **Returns:** a cached or new NamedProperty instance
- **Examples:**
  - ```java
    NamedProperty username = NamedProperty.of("username");
    NamedProperty status = NamedProperty.of("status");
    
    // Invalid usage - throws IllegalArgumentException
    try {
        NamedProperty invalid = NamedProperty.of("");
    } catch (IllegalArgumentException e) {
        // Handle empty property name
    }
    ```

#### Public Instance Methods
##### propName(...) -> String
- **Signature:** `public String propName()`
- **Summary:** Returns the property name associated with this NamedProperty.
- **Parameters:**
  - (none)
- **Returns:** the property name
- **Examples:**
  - ```java
    NamedProperty age = NamedProperty.of("age");
    String name = age.propName();   // Returns "age"
    ```
##### equal(...) -> Equal
- **Signature:** `public Equal equal(final Object value)`
- **Summary:** Creates an EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be of any type compatible with the property.
- **Returns:** an Equal condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("status").equal("active");   // status = 'active'
    NamedProperty.of("count").equal(5);           // count = 5
    ```
- **See also:** Equal, Filters#equal(String, Object)
##### eq(...) -> Equal
- **Signature:** `@Beta public Equal eq(final Object value)`
- **Summary:** Creates an EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** an Equal condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("status").eq("active");   // status = 'active'
    NamedProperty.of("count").eq(5);           // count = 5
    ```
- **See also:** #equal(Object)
##### equalsAny(...) -> Or
- **Signature:** `public Or equalsAny(final Object... values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property.
- **Parameters:**
  - `values` (`Object[]`) — array of values to check equality against. Each value will be tested with OR logic. Must not be null or empty.
- **Returns:** an Or condition containing multiple Equal conditions
- **Examples:**
  - ```java
    NamedProperty.of("color").equalsAny("red", "green", "blue");
    // SQL: ((color = 'red') OR (color = 'green') OR (color = 'blue'))
    
    NamedProperty.of("priority").equalsAny(1, 2, 3);
    // SQL: ((priority = 1) OR (priority = 2) OR (priority = 3))
    
    NamedProperty.of("x").equalsAny();   // throws IllegalArgumentException (empty values)
    ```
- **See also:** Or, Equal, com.landawn.abacus.query.Filters#anyEqual(java.util.Map)
- **Signature:** `public Or equalsAny(final int[] values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property using primitive int values.
- **Parameters:**
  - `values` (`int[]`) — primitive int values to check. Must not be null or empty.
- **Returns:** an Or condition containing multiple Equal conditions
- **Examples:**
  - ```java
    NamedProperty.of("priority").equalsAny(new int[]{1, 2, 3});
    // SQL: ((priority = 1) OR (priority = 2) OR (priority = 3))
    ```
- **See also:** Or, Equal
- **Signature:** `public Or equalsAny(final long[] values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property using primitive long values.
- **Parameters:**
  - `values` (`long[]`) — primitive long values to check. Must not be null or empty.
- **Returns:** an Or condition containing multiple Equal conditions
- **Examples:**
  - ```java
    NamedProperty.of("user_id").equalsAny(new long[]{1001L, 1002L, 1003L});
    // SQL: ((user_id = 1001) OR (user_id = 1002) OR (user_id = 1003))
    ```
- **See also:** Or, Equal
- **Signature:** `public Or equalsAny(final double[] values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property using primitive double values.
- **Parameters:**
  - `values` (`double[]`) — primitive double values to check. Must not be null or empty.
- **Returns:** an Or condition containing multiple Equal conditions
- **Examples:**
  - ```java
    NamedProperty.of("rate").equalsAny(new double[]{1.5, 2.0, 2.5});
    // SQL: ((rate = 1.5) OR (rate = 2.0) OR (rate = 2.5))
    ```
- **See also:** Or, Equal
- **Signature:** `public Or equalsAny(final Collection<?> values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property using a collection.
- **Contract:**
  - Useful when the values are already in a collection or list.
- **Parameters:**
  - `values` (`Collection<?>`) — collection of values to check equality against. Each value will be tested with OR logic. Must not be null or empty and must yield at least one value while snapshotted.
- **Returns:** an Or condition containing multiple Equal conditions
- **Examples:**
  - ```java
    List<String> cities = Arrays.asList("New York", "Los Angeles", "Chicago");
    NamedProperty.of("city").equalsAny(cities);
    // SQL: ((city = 'New York') OR (city = 'Los Angeles') OR (city = 'Chicago'))
    
    List<Integer> validIds = List.of(10, 20, 30);
    NamedProperty.of("department_id").equalsAny(validIds);
    // SQL: ((department_id = 10) OR (department_id = 20) OR (department_id = 30))
    ```
- **See also:** Or, Equal
##### notEqual(...) -> NotEqual
- **Signature:** `public NotEqual notEqual(final Object value)`
- **Summary:** Creates a NOT EQUAL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is not equal to the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be of any type compatible with the property.
- **Returns:** a NotEqual condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("status").notEqual("deleted");   // status != 'deleted'
    NamedProperty.of("count").notEqual(0);            // count != 0
    ```
- **See also:** NotEqual, Filters#notEqual(String, Object)
##### ne(...) -> NotEqual
- **Signature:** `@Beta public NotEqual ne(final Object value)`
- **Summary:** Creates a NOT EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a NotEqual condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("status").ne("deleted");   // status != 'deleted'
    NamedProperty.of("count").ne(0);            // count != 0
    ```
- **See also:** #notEqual(Object)
##### greaterThan(...) -> GreaterThan
- **Signature:** `public GreaterThan greaterThan(final Object value)`
- **Summary:** Creates a GREATER THAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is strictly greater than the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a GreaterThan condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("age").greaterThan(18);        // age > 18
    NamedProperty.of("price").greaterThan(99.99);   // price > 99.99
    ```
- **See also:** GreaterThan, Filters#greaterThan(String, Object)
##### gt(...) -> GreaterThan
- **Signature:** `@Beta public GreaterThan gt(final Object value)`
- **Summary:** Creates a GREATER THAN condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a GreaterThan condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("age").gt(18);        // age > 18
    NamedProperty.of("price").gt(99.99);   // price > 99.99
    ```
- **See also:** #greaterThan(Object)
##### greaterThanOrEqual(...) -> GreaterThanOrEqual
- **Signature:** `public GreaterThanOrEqual greaterThanOrEqual(final Object value)`
- **Summary:** Creates a GREATER THAN OR EQUAL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is greater than or equal to the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a GreaterThanOrEqual condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("score").greaterThanOrEqual(60);   // score >= 60
    NamedProperty.of("age").greaterThanOrEqual(21);     // age >= 21
    ```
- **See also:** GreaterThanOrEqual, Filters#greaterThanOrEqual(String, Object)
##### ge(...) -> GreaterThanOrEqual
- **Signature:** `@Beta public GreaterThanOrEqual ge(final Object value)`
- **Summary:** Creates a GREATER THAN OR EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a GreaterThanOrEqual condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("score").ge(60);   // score >= 60
    NamedProperty.of("age").ge(21);     // age >= 21
    ```
- **See also:** #greaterThanOrEqual(Object)
##### lessThan(...) -> LessThan
- **Signature:** `public LessThan lessThan(final Object value)`
- **Summary:** Creates a LESS THAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is strictly less than the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a LessThan condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("price").lessThan(100);   // price < 100
    NamedProperty.of("age").lessThan(18);      // age < 18
    ```
- **See also:** LessThan, Filters#lessThan(String, Object)
##### lt(...) -> LessThan
- **Signature:** `@Beta public LessThan lt(final Object value)`
- **Summary:** Creates a LESS THAN condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a LessThan condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("price").lt(100);   // price < 100
    NamedProperty.of("age").lt(18);      // age < 18
    ```
- **See also:** #lessThan(Object)
##### lessThanOrEqual(...) -> LessThanOrEqual
- **Signature:** `public LessThanOrEqual lessThanOrEqual(final Object value)`
- **Summary:** Creates a LESS THAN OR EQUAL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is less than or equal to the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a LessThanOrEqual condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("quantity").lessThanOrEqual(10);   // quantity <= 10
    NamedProperty.of("age").lessThanOrEqual(65);        // age <= 65
    ```
- **See also:** LessThanOrEqual, Filters#lessThanOrEqual(String, Object)
##### le(...) -> LessThanOrEqual
- **Signature:** `@Beta public LessThanOrEqual le(final Object value)`
- **Summary:** Creates a LESS THAN OR EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a LessThanOrEqual condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("quantity").le(10);   // quantity <= 10
    NamedProperty.of("age").le(65);        // age <= 65
    ```
- **See also:** #lessThanOrEqual(Object)
##### isNull(...) -> IsNull
- **Signature:** `public IsNull isNull()`
- **Summary:** Creates an IS NULL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is NULL in the database.
- **Parameters:**
  - (none)
- **Returns:** an IsNull condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("email").isNull();          // email IS NULL
    NamedProperty.of("middle_name").isNull();    // middle_name IS NULL
    NamedProperty.of("deleted_at").isNull();     // deleted_at IS NULL
    ```
- **See also:** IsNull, Filters#isNull(String)
##### isNullOrEmpty(...) -> Or
- **Signature:** `public Or isNullOrEmpty()`
- **Summary:** Creates a condition matching null or empty values for this property.
- **Parameters:**
  - (none)
- **Returns:** an OR condition combining null and empty checks
- **Examples:**
  - ```java
    NamedProperty.of("email").isNullOrEmpty();         // ((email IS NULL) OR (email = ''))
    NamedProperty.of("description").isNullOrEmpty();   // ((description IS NULL) OR (description = ''))
    ```
- **See also:** Filters#isNullOrEmpty(String)
##### isNotNull(...) -> IsNotNull
- **Signature:** `public IsNotNull isNotNull()`
- **Summary:** Creates an IS NOT NULL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is not NULL in the database.
- **Parameters:**
  - (none)
- **Returns:** an IsNotNull condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("email").isNotNull();      // email IS NOT NULL
    NamedProperty.of("phone").isNotNull();      // phone IS NOT NULL
    NamedProperty.of("address").isNotNull();    // address IS NOT NULL
    ```
- **See also:** IsNotNull, Filters#isNotNull(String)
##### isNotNullAndNotEmpty(...) -> And
- **Signature:** `public And isNotNullAndNotEmpty()`
- **Summary:** Creates a condition requiring this property to be both non-null and non-empty.
- **Parameters:**
  - (none)
- **Returns:** an AND condition combining non-null and non-empty checks
- **Examples:**
  - ```java
    NamedProperty.of("email").isNotNullAndNotEmpty();   // ((email IS NOT NULL) AND (email != ''))
    NamedProperty.of("phone").isNotNullAndNotEmpty();   // ((phone IS NOT NULL) AND (phone != ''))
    ```
- **See also:** Filters#isNotNullAndNotEmpty(String)
##### isNaN(...) -> IsNaN
- **Signature:** `public IsNaN isNaN()`
- **Summary:** Creates an IS NAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the floating-point property value is NaN (Not-a-Number).
- **Parameters:**
  - (none)
- **Returns:** an IsNaN condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("calculation_result").isNaN();   // calculation_result IS NAN
    NamedProperty.of("ratio").isNaN();                 // ratio IS NAN
    ```
- **See also:** IsNaN, Filters#isNaN(String)
##### isNotNaN(...) -> IsNotNaN
- **Signature:** `public IsNotNaN isNotNaN()`
- **Summary:** Creates an IS NOT NAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the floating-point property value is not NaN (Not-a-Number).
- **Parameters:**
  - (none)
- **Returns:** an IsNotNaN condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("temperature").isNotNaN();   // temperature IS NOT NAN
    NamedProperty.of("ratio").isNotNaN();         // ratio IS NOT NAN
    ```
- **See also:** IsNotNaN, Filters#isNotNaN(String)
##### isInfinite(...) -> IsInfinite
- **Signature:** `public IsInfinite isInfinite()`
- **Summary:** Creates an IS INFINITE condition for this property.
- **Contract:**
  - This generates a condition that checks if the floating-point property value is infinite.
- **Parameters:**
  - (none)
- **Returns:** an IsInfinite condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("ratio").isInfinite();     // ratio IS INFINITE
    NamedProperty.of("balance").isInfinite();   // balance IS INFINITE
    ```
- **See also:** IsInfinite, Filters#isInfinite(String)
##### isNotInfinite(...) -> IsNotInfinite
- **Signature:** `public IsNotInfinite isNotInfinite()`
- **Summary:** Creates an IS NOT INFINITE condition for this property.
- **Contract:**
  - This generates a condition that checks if the floating-point property value is not infinite.
- **Parameters:**
  - (none)
- **Returns:** an IsNotInfinite condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("percentage").isNotInfinite();   // percentage IS NOT INFINITE
    NamedProperty.of("balance").isNotInfinite();      // balance IS NOT INFINITE
    ```
- **See also:** IsNotInfinite, Filters#isNotInfinite(String)
##### between(...) -> Between
- **Signature:** `public Between between(final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value falls within the specified range (inclusive).
- **Parameters:**
  - `minValue` (`Object`) — the minimum value (inclusive). Can be numeric, date, string, or any comparable type.
  - `maxValue` (`Object`) — the maximum value (inclusive). Can be numeric, date, string, or any comparable type.
- **Returns:** a Between condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("age").between(18, 65);          // age BETWEEN 18 AND 65
    NamedProperty.of("price").between(10.0, 100.0);   // price BETWEEN 10.0 AND 100.0
    ```
- **See also:** Between, Filters#between(String, Object, Object)
##### notBetween(...) -> NotBetween
- **Signature:** `public NotBetween notBetween(final Object minValue, final Object maxValue)`
- **Summary:** Creates a NOT BETWEEN condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is outside the specified range.
- **Parameters:**
  - `minValue` (`Object`) — the lower bound of the excluded range (a value equal to this boundary is itself excluded, per SQL NOT BETWEEN). Can be numeric, date, string, or any comparable type.
  - `maxValue` (`Object`) — the upper bound of the excluded range (a value equal to this boundary is itself excluded, per SQL NOT BETWEEN). Can be numeric, date, string, or any comparable type.
- **Returns:** a NotBetween condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("age").notBetween(18, 65);          // age NOT BETWEEN 18 AND 65
    NamedProperty.of("price").notBetween(10.0, 100.0);   // price NOT BETWEEN 10.0 AND 100.0
    ```
- **See also:** NotBetween, Filters#notBetween(String, Object, Object)
##### like(...) -> Like
- **Signature:** `public Like like(final String value)`
- **Summary:** Creates a LIKE condition for this property.
- **Parameters:**
  - `value` (`String`) — the pattern to match (can include % for any characters and _ for single character). A null value is permitted (the resulting condition renders the value as null).
- **Returns:** a Like condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("name").like("John%");            // name LIKE 'John%'
    NamedProperty.of("email").like("%@example.com");   // email LIKE '%@example.com'
    ```
- **See also:** Like, Filters#like(String, String)
##### notLike(...) -> NotLike
- **Signature:** `public NotLike notLike(final String value)`
- **Summary:** Creates a NOT LIKE condition for this property.
- **Parameters:**
  - `value` (`String`) — the pattern to exclude (can include % for any characters and _ for single character). A null value is permitted (the resulting condition renders the value as null).
- **Returns:** a NotLike condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("email").notLike("%@temp.com");   // email NOT LIKE '%@temp.com'
    NamedProperty.of("name").notLike("test%");         // name NOT LIKE 'test%'
    ```
- **See also:** NotLike, Filters#notLike(String, String)
##### startsWith(...) -> Like
- **Signature:** `public Like startsWith(final String value)`
- **Summary:** Creates a LIKE condition that matches values starting with the specified prefix.
- **Parameters:**
  - `value` (`String`) — the prefix to match. The % wildcard will be automatically appended.
- **Returns:** a Like condition with % appended to the value
- **Examples:**
  - ```java
    NamedProperty.of("name").startsWith("John");   // name LIKE 'John%'
    NamedProperty.of("code").startsWith("PRD");    // code LIKE 'PRD%'
    ```
- **See also:** Like, Filters#startsWith(String, String)
##### notStartsWith(...) -> NotLike
- **Signature:** `public NotLike notStartsWith(final String value)`
- **Summary:** Creates a NOT LIKE condition that excludes values starting with the specified prefix.
- **Parameters:**
  - `value` (`String`) — the prefix to exclude. The % wildcard will be automatically appended.
- **Returns:** a NotLike condition with % appended to the value
- **Examples:**
  - ```java
    NamedProperty.of("name").notStartsWith("test");   // name NOT LIKE 'test%'
    NamedProperty.of("code").notStartsWith("TMP");    // code NOT LIKE 'TMP%'
    ```
- **See also:** NotLike, Filters#notStartsWith(String, String)
##### endsWith(...) -> Like
- **Signature:** `public Like endsWith(final String value)`
- **Summary:** Creates a LIKE condition that matches values ending with the specified suffix.
- **Parameters:**
  - `value` (`String`) — the suffix to match. The % wildcard will be automatically prepended.
- **Returns:** a Like condition with % prepended to the value
- **Examples:**
  - ```java
    NamedProperty.of("email").endsWith("@example.com");   // email LIKE '%@example.com'
    NamedProperty.of("filename").endsWith(".pdf");        // filename LIKE '%.pdf'
    ```
- **See also:** Like, Filters#endsWith(String, String)
##### notEndsWith(...) -> NotLike
- **Signature:** `public NotLike notEndsWith(final String value)`
- **Summary:** Creates a NOT LIKE condition that excludes values ending with the specified suffix.
- **Parameters:**
  - `value` (`String`) — the suffix to exclude. The % wildcard will be automatically prepended.
- **Returns:** a NotLike condition with % prepended to the value
- **Examples:**
  - ```java
    NamedProperty.of("email").notEndsWith("@temp.com");   // email NOT LIKE '%@temp.com'
    NamedProperty.of("filename").notEndsWith(".tmp");     // filename NOT LIKE '%.tmp'
    ```
- **See also:** NotLike, Filters#notEndsWith(String, String)
##### contains(...) -> Like
- **Signature:** `public Like contains(final String value)`
- **Summary:** Creates a LIKE condition that matches values containing the specified substring.
- **Parameters:**
  - `value` (`String`) — the substring to match. The % wildcard will be automatically added to both sides.
- **Returns:** a Like condition with % on both sides of the value
- **Examples:**
  - ```java
    NamedProperty.of("description").contains("important");   // description LIKE '%important%'
    NamedProperty.of("title").contains("query");             // title LIKE '%query%'
    ```
- **See also:** Like, Filters#contains(String, String)
##### notContains(...) -> NotLike
- **Signature:** `public NotLike notContains(final String value)`
- **Summary:** Creates a NOT LIKE condition that excludes values containing the specified substring.
- **Parameters:**
  - `value` (`String`) — the substring to exclude. The % wildcard will be automatically added to both sides.
- **Returns:** a NotLike condition with % on both sides of the value
- **Examples:**
  - ```java
    NamedProperty.of("description").notContains("deprecated");   // description NOT LIKE '%deprecated%'
    NamedProperty.of("title").notContains("draft");              // title NOT LIKE '%draft%'
    ```
- **See also:** NotLike, Filters#notContains(String, String)
##### in(...) -> In
- **Signature:** `public In in(final Object... values)`
- **Summary:** Creates an IN condition for this property with an array of values.
- **Contract:**
  - This generates a condition that checks if the property value matches any of the specified values.
- **Parameters:**
  - `values` (`Object[]`) — array of values to check membership against (must not be null or empty)
- **Returns:** an In condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("status").in("active", "pending", "approved");
    // SQL: status IN ('active', 'pending', 'approved')
    
    NamedProperty.of("priority").in(1, 2, 3);
    // SQL: priority IN (1, 2, 3)
    ```
- **See also:** In, Filters#in(String, Object\[\])
- **Signature:** `public In in(final int[] values)`
- **Summary:** Creates an IN condition for this property with primitive int values.
- **Parameters:**
  - `values` (`int[]`) — primitive int values to check membership against. Must not be null or empty.
- **Returns:** an In condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("priority").in(new int[]{1, 2, 3});
    // SQL: priority IN (1, 2, 3)
    ```
- **See also:** In, Filters#in(String, int\[\])
- **Signature:** `public In in(final long[] values)`
- **Summary:** Creates an IN condition for this property with primitive long values.
- **Parameters:**
  - `values` (`long[]`) — primitive long values to check membership against. Must not be null or empty.
- **Returns:** an In condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("user_id").in(new long[]{1001L, 1002L, 1003L});
    // SQL: user_id IN (1001, 1002, 1003)
    ```
- **See also:** In, Filters#in(String, long\[\])
- **Signature:** `public In in(final double[] values)`
- **Summary:** Creates an IN condition for this property with primitive double values.
- **Parameters:**
  - `values` (`double[]`) — primitive double values to check membership against. Must not be null or empty.
- **Returns:** an In condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("rate").in(new double[]{1.5, 2.0, 2.5});
    // SQL: rate IN (1.5, 2.0, 2.5)
    ```
- **See also:** In, Filters#in(String, double\[\])
- **Signature:** `public In in(final Collection<?> values)`
- **Summary:** Creates an IN condition for this property with a collection of values.
- **Contract:**
  - Useful when the values are already in a collection or list.
- **Parameters:**
  - `values` (`Collection<?>`) — collection of values to check membership against (must not be null or empty)
- **Returns:** an In condition for this property
- **Examples:**
  - ```java
    List<Integer> validIds = Arrays.asList(1, 2, 3, 4, 5);
    NamedProperty.of("id").in(validIds);
    // SQL: id IN (1, 2, 3, 4, 5)
    
    List<String> departments = Arrays.asList("Sales", "Marketing", "IT");
    NamedProperty.of("department").in(departments);
    // SQL: department IN ('Sales', 'Marketing', 'IT')
    ```
- **See also:** In, Filters#in(String, Collection)
- **Signature:** `public InSubQuery in(final SubQuery subQuery)`
- **Summary:** Creates an IN condition for this property with a subquery.
- **Contract:**
  - This generates a condition that checks if the property value is contained in the result set returned by the specified subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check membership against (must not be null)
- **Returns:** an InSubQuery condition for this property
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT id FROM active_users");
    NamedProperty.of("user_id").in(subQuery);
    // SQL: user_id IN (SELECT id FROM active_users)
    ```
- **See also:** InSubQuery, Filters#in(String, SubQuery)
##### notIn(...) -> NotIn
- **Signature:** `public NotIn notIn(final Object... values)`
- **Summary:** Creates a NOT IN condition for this property with an array of values.
- **Contract:**
  - This generates a condition that checks if the property value does not match any of the specified values.
- **Parameters:**
  - `values` (`Object[]`) — array of values to check non-membership against (must not be null or empty)
- **Returns:** a NotIn condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("status").notIn("deleted", "archived");
    // SQL: status NOT IN ('deleted', 'archived')
    
    NamedProperty.of("priority").notIn(4, 5);
    // SQL: priority NOT IN (4, 5)
    ```
- **See also:** NotIn, Filters#notIn(String, Object\[\])
- **Signature:** `public NotIn notIn(final int[] values)`
- **Summary:** Creates a NOT IN condition for this property with primitive int values.
- **Parameters:**
  - `values` (`int[]`) — primitive int values to check non-membership against. Must not be null or empty.
- **Returns:** a NotIn condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("priority").notIn(new int[]{4, 5});
    // SQL: priority NOT IN (4, 5)
    ```
- **See also:** NotIn, Filters#notIn(String, int\[\])
- **Signature:** `public NotIn notIn(final long[] values)`
- **Summary:** Creates a NOT IN condition for this property with primitive long values.
- **Parameters:**
  - `values` (`long[]`) — primitive long values to check non-membership against. Must not be null or empty.
- **Returns:** a NotIn condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("user_id").notIn(new long[]{999L, 1000L});
    // SQL: user_id NOT IN (999, 1000)
    ```
- **See also:** NotIn, Filters#notIn(String, long\[\])
- **Signature:** `public NotIn notIn(final double[] values)`
- **Summary:** Creates a NOT IN condition for this property with primitive double values.
- **Parameters:**
  - `values` (`double[]`) — primitive double values to check non-membership against. Must not be null or empty.
- **Returns:** a NotIn condition for this property
- **Examples:**
  - ```java
    NamedProperty.of("rate").notIn(new double[]{0.0, -1.0});
    // SQL: rate NOT IN (0.0, -1.0)
    ```
- **See also:** NotIn, Filters#notIn(String, double\[\])
- **Signature:** `public NotIn notIn(final Collection<?> values)`
- **Summary:** Creates a NOT IN condition for this property with a collection of values.
- **Contract:**
  - Useful when the values are already in a collection or list.
- **Parameters:**
  - `values` (`Collection<?>`) — collection of values to check non-membership against (must not be null or empty)
- **Returns:** a NotIn condition for this property
- **Examples:**
  - ```java
    List<Integer> excludedIds = Arrays.asList(100, 200, 300);
    NamedProperty.of("id").notIn(excludedIds);
    // SQL: id NOT IN (100, 200, 300)
    
    List<String> blockedDepartments = Arrays.asList("Temp", "Archived");
    NamedProperty.of("department").notIn(blockedDepartments);
    // SQL: department NOT IN ('Temp', 'Archived')
    ```
- **See also:** NotIn, Filters#notIn(String, Collection)
- **Signature:** `public NotInSubQuery notIn(final SubQuery subQuery)`
- **Summary:** Creates a NOT IN condition for this property with a subquery.
- **Contract:**
  - This generates a condition that checks if the property value is not contained in the result set returned by the specified subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check non-membership against (must not be null)
- **Returns:** a NotInSubQuery condition for this property
- **Examples:**
  - ```java
    SubQuery subQuery = Filters.subQuery("SELECT id FROM blacklisted_users");
    NamedProperty.of("user_id").notIn(subQuery);
    // SQL: user_id NOT IN (SELECT id FROM blacklisted_users)
    ```
- **See also:** NotInSubQuery, Filters#notIn(String, SubQuery)
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this NamedProperty based on the property name.
- **Parameters:**
  - (none)
- **Returns:** hash code of the property name
- **Examples:**
  - ```java
    NamedProperty.of("age").hashCode();                                         // returns "age".hashCode()
    NamedProperty.of("age").hashCode() == NamedProperty.of("age").hashCode();   // true (same name)
    NamedProperty.of("age").hashCode() == NamedProperty.of("Age").hashCode();   // false — hash codes of "age" and "Age" differ (case-sensitive)
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this NamedProperty is equal to another object.
- **Contract:**
  - Checks if this NamedProperty is equal to another object.
  - Two NamedProperty instances are equal if they have the same property name.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal (same property name), false otherwise
- **Examples:**
  - ```java
    new NamedProperty("age").equals(new NamedProperty("age"));   // returns true (same name)
    NamedProperty.of("age").equals(NamedProperty.of("age"));     // true — same property name (structural equality)
    new NamedProperty("age").equals(new NamedProperty("Age"));   // returns false (case-sensitive)
    new NamedProperty("age").equals("age");                      // returns false (not a NamedProperty)
    ```
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns the SQL representation of this NamedProperty.
- **Parameters:**
  - (none)
- **Returns:** the property name
- **Examples:**
  - ```java
    NamedProperty.of("age").toString();          // returns "age"
    NamedProperty.of("user.email").toString();   // returns "user.email" (returned verbatim, no quoting)
    ```

### Class NaturalJoin (com.landawn.abacus.query.condition.NaturalJoin)
Represents a NATURAL JOIN clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public NaturalJoin(final String joinEntity)` — Creates a NATURAL JOIN clause for the specified table or entity.
- `public NaturalJoin(final Collection<String> joinEntities)` — Creates a NATURAL JOIN clause with multiple tables/entities.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Not (com.landawn.abacus.query.condition.Not)
Represents a logical NOT condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Not(final Condition condition)` — Creates a new NOT condition that negates the specified condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class NotBetween (com.landawn.abacus.query.condition.NotBetween)
Represents a NOT BETWEEN condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public NotBetween(final String propName, final Object minValue, final Object maxValue)` — Creates a NOT BETWEEN condition for the specified property and range.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class NotEqual (com.landawn.abacus.query.condition.NotEqual)
Represents a NOT EQUAL (!= or &lt;&gt;) condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public NotEqual(final String propName, final Object propValue)` — Creates a new NotEqual condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class NotExists (com.landawn.abacus.query.condition.NotExists)
Represents the SQL NOT EXISTS operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public NotExists(final SubQuery subQuery)` — Creates a new NOT EXISTS condition with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this NOT EXISTS condition.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT 1 FROM orders WHERE orders.customer_id = customers.id");
    NotExists notExists = new NotExists(subQuery);
    SubQuery retrieved = notExists.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = notExists.subQuery() == notExists.condition();
    // returns true
    ```

### Class NotIn (com.landawn.abacus.query.condition.NotIn)
Represents a NOT IN condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public NotIn(final String propName, final Collection<?> values)` — Creates a NOT IN condition for the specified property and collection of values.
- `public NotIn(final Collection<String> propNames, final Collection<?> valueRows)` — Creates a new row value constructor NOT IN condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class NotInSubQuery (com.landawn.abacus.query.condition.NotInSubQuery)
Represents a NOT IN subquery condition used in SQL WHERE clauses.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public NotInSubQuery(final String propName, final SubQuery subQuery)` — Creates a NOT IN subquery condition for a single property.
- `public NotInSubQuery(final Collection<String> propNames, final SubQuery subQuery)` — Creates a NOT IN subquery condition for multiple properties.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class NotLike (com.landawn.abacus.query.condition.NotLike)
Represents a NOT LIKE condition in SQL queries for pattern exclusion.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public NotLike(final String propName, final Object propValue)` — Creates a new NOT LIKE condition for the specified property and pattern.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class On (com.landawn.abacus.query.condition.On)
Represents an ON clause used in SQL JOIN operations.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public On(final Condition condition)` — Creates an ON clause with a custom condition.
- `public On(final String leftPropName, final String rightPropName)` — Creates an ON clause for simple column equality between tables.
- `public On(final Map<String, String> propNamePairs)` — Creates an ON clause with multiple column equality conditions.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Enum Operator (com.landawn.abacus.query.condition.Operator)
Enumeration of SQL operators supported by the condition framework.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### of(...) -> Operator
- **Signature:** `public static Operator of(final String name)`
- **Summary:** Returns an Operator by its SQL representation.
- **Parameters:**
  - `name` (`String`) — the SQL token or enum constant name. May be null; surrounding whitespace is not trimmed.
- **Returns:** the corresponding Operator enum value, or null if name is null or not a known token/name
- **Examples:**
  - ```java
    // Look up symbolic operators
    Operator eq = Operator.of("=");       // Operator.EQUAL
    Operator gt = Operator.of(">");       // Operator.GREATER_THAN
    Operator gte = Operator.of(">=");     // Operator.GREATER_THAN_OR_EQUAL
    
    // Look up word operators (case-insensitive)
    Operator and = Operator.of("AND");    // Operator.AND
    Operator or = Operator.of("or");      // Operator.OR
    Operator like = Operator.of("LIKE");  // Operator.LIKE
    
    // Enum constant names are also accepted (case-insensitive)
    Operator eq2 = Operator.of("EQUAL");          // Operator.EQUAL
    Operator gt2 = Operator.of("greater_than");    // Operator.GREATER_THAN
    
    // Edge cases
    Operator unknown = Operator.of("UNKNOWN");    // null (not a known token or name)
    Operator empty = Operator.of("");             // Operator.EMPTY (its SQL token is "")
    Operator nil = Operator.of(null);             // null
    ```

#### Public Instance Methods
##### sqlToken(...) -> String
- **Signature:** `public String sqlToken()`
- **Summary:** Returns the SQL representation of this operator.
- **Parameters:**
  - (none)
- **Returns:** the SQL representation of this operator (e.g., "=", "AND", "LIKE")
- **Examples:**
  - ```java
    String eqToken = Operator.EQUAL.sqlToken();           // "="
    String andToken = Operator.AND.sqlToken();            // "AND"
    String betweenToken = Operator.BETWEEN.sqlToken();    // "BETWEEN"
    String likeToken = Operator.LIKE.sqlToken();          // "LIKE"
    String joinToken = Operator.LEFT_JOIN.sqlToken();     // "LEFT JOIN"
    String emptyToken = Operator.EMPTY.sqlToken();        // "" (empty placeholder operator)
    ```
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns the SQL representation of this operator.
- **Parameters:**
  - (none)
- **Returns:** the SQL representation of this operator (e.g., "=", "AND", "LIKE")
- **Examples:**
  - ```java
    String eq = Operator.EQUAL.toString();                  // "="
    String and = Operator.AND.toString();                   // "AND"
    String gte = Operator.GREATER_THAN_OR_EQUAL.toString(); // ">="
    String join = Operator.LEFT_JOIN.toString();            // "LEFT JOIN"
    String empty = Operator.EMPTY.toString();               // "" (empty placeholder operator)
    
    // toString() always equals sqlToken()
    boolean same = Operator.IN.toString().equals(Operator.IN.sqlToken()); // true
    ```

### Class Or (com.landawn.abacus.query.condition.Or)
Represents a composable OR condition that combines multiple conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Or(final Condition... conditions)` — Creates a new OR condition with the specified conditions.
- `public Or(final Collection<? extends Condition> conditions)` — Creates a new OR condition with a collection of conditions.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### or(...) -> Or
- **Signature:** `@Override public Or or(final Condition condition)`
- **Summary:** Creates a new Or condition by adding another condition to this OR.
- **Parameters:**
  - `condition` (`Condition`) — the condition to add to this OR. Must not be null and must be composable (i.e. must not be or contain a Criteria, a Clause, an ON/USING connector, an ANY/ALL/SOME quantified-subquery operand, a standalone SubQuery, or an empty predicate).
- **Returns:** a new Or condition containing all existing conditions plus the new one
- **Examples:**
  - ```java
    // Build condition step by step
    Or or = new Or(Filters.equal("type", "A"))
        .or(Filters.equal("type", "B"))
        .or(Filters.equal("type", "C"));
    // SQL: ((type = 'A') OR (type = 'B') OR (type = 'C'))
    
    // Add conditions conditionally
    Or baseOr = new Or(Filters.equal("status", "active"));
    if (includeInactive) {
        baseOr = baseOr.or(Filters.equal("status", "inactive"));
    }
    if (includePending) {
        baseOr = baseOr.or(Filters.equal("status", "pending"));
    }
    // Results vary based on flags
    ```

### Class OrderBy (com.landawn.abacus.query.condition.OrderBy)
Represents an ORDER BY clause in SQL queries, used to specify the sort order of query results.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public OrderBy(final Condition condition)` — Creates an ORDER BY clause with a custom condition.
- `public OrderBy(final String... propNames)` — Creates an ORDER BY clause with multiple property names.
- `public OrderBy(final Collection<String> propNames)` — Creates an ORDER BY clause with multiple property names supplied as a collection.
- `public OrderBy(final String propOrColumnName, final SortDirection direction)` — Creates an ORDER BY clause with a single property and sort direction.
- `public OrderBy(final Collection<String> propNames, final SortDirection direction)` — Creates an ORDER BY clause with multiple properties and a single sort direction.
- `public OrderBy(final Map<String, SortDirection> orders)` — Creates an ORDER BY clause with properties having different sort directions.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class RightJoin (com.landawn.abacus.query.condition.RightJoin)
Represents a RIGHT JOIN clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public RightJoin(final String joinEntity)` — Creates a RIGHT JOIN clause for the specified table or entity without a join condition.
- `public RightJoin(final String joinEntity, final Condition joinCondition)` — Creates a RIGHT JOIN clause with a join condition.
- `public RightJoin(final Collection<String> joinEntities, final Condition joinCondition)` — Creates a RIGHT JOIN clause with multiple tables/entities and a join condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class Some (com.landawn.abacus.query.condition.Some)
Represents the SQL SOME operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Some(final SubQuery subQuery)` — Creates a new SOME condition with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this SOME condition.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT salary FROM employees WHERE role = 'manager'");
    Some some = new Some(subQuery);
    SubQuery retrieved = some.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = some.subQuery() == some.condition();
    // returns true
    ```

### Class SqlExpression (com.landawn.abacus.query.condition.SqlExpression)
Represents a raw SQL expression that can be used in queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public SqlExpression(final String literal)` — Constructs a new SqlExpression with the specified raw SQL expression text.

#### Public Static Methods
##### of(...) -> SqlExpression
- **Signature:** `public static SqlExpression of(final String literal)`
- **Summary:** Creates or retrieves a cached SqlExpression instance for the given expression text.
- **Parameters:**
  - `literal` (`String`) — the raw SQL expression text (must not be null)
- **Returns:** a cached or newly created SqlExpression instance for the given text
- **Examples:**
  - ```java
    SqlExpression expr1 = SqlExpression.of("CURRENT_DATE");
    SqlExpression expr2 = SqlExpression.of("CURRENT_DATE");
    // expr1 == expr2 (same instance due to caching)
    
    SqlExpression calc = SqlExpression.of("price * 1.1");
    // Reuse the same expression in multiple places
    ```
##### equal(...) -> String
- **Signature:** `public static String equal(final String expr, final Object value)`
- **Summary:** Creates an equality expression between a literal and a value.
- **Contract:**
  - If value is null, the result is rendered as "literal IS NULL" instead of "literal = null".
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the equality
  - `value` (`Object`) — the right-hand side value; may be null (renders as IS NULL)
- **Returns:** a SQL representation of the equality expression
- **Examples:**
  - ```java
    String expr = SqlExpression.equal("age", 25);
    // Returns: "age = 25"
    
    String expr2 = SqlExpression.equal("status", "active");
    // Returns: "status = 'active'"
    
    String expr3 = SqlExpression.equal("middle_name", null);
    // Returns: "middle_name IS NULL"
    ```
##### eq(...) -> String
- **Signature:** `@Beta public static String eq(final String expr, final Object value)`
- **Summary:** Creates an equality expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the equality
  - `value` (`Object`) — the right-hand side value; may be null (renders as IS NULL)
- **Returns:** a SQL representation of the equality expression
- **Examples:**
  - ```java
    String expr = SqlExpression.eq("user_id", 123);
    // Returns: "user_id = 123"
    ```
##### notEqual(...) -> String
- **Signature:** `public static String notEqual(final String expr, final Object value)`
- **Summary:** Creates a not-equal expression between a literal and a value.
- **Contract:**
  - If value is null, the result is rendered as "literal IS NOT NULL" instead of "literal != null".
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the inequality
  - `value` (`Object`) — the right-hand side value; may be null (renders as IS NOT NULL)
- **Returns:** a SQL representation of the not-equal expression
- **Examples:**
  - ```java
    String expr = SqlExpression.notEqual("status", "INACTIVE");
    // Returns: "status != 'INACTIVE'"
    
    String expr2 = SqlExpression.notEqual("count", 0);
    // Returns: "count != 0"
    
    String expr3 = SqlExpression.notEqual("email", null);
    // Returns: "email IS NOT NULL"
    ```
##### ne(...) -> String
- **Signature:** `@Beta public static String ne(final String expr, final Object value)`
- **Summary:** Creates a not-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the inequality
  - `value` (`Object`) — the right-hand side value; may be null (renders as IS NOT NULL)
- **Returns:** a SQL representation of the not-equal expression
- **Examples:**
  - ```java
    String expr = SqlExpression.ne("type", "guest");
    // Returns: "type != 'guest'"
    ```
##### greaterThan(...) -> String
- **Signature:** `public static String greaterThan(final String expr, final Object value)`
- **Summary:** Creates a greater-than expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the greater-than expression
- **Examples:**
  - ```java
    String expr = SqlExpression.greaterThan("salary", 50000);
    // Returns: "salary > 50000"
    
    String expr2 = SqlExpression.greaterThan("created_date", "2024-01-01");
    // Returns: "created_date > '2024-01-01'"
    ```
##### gt(...) -> String
- **Signature:** `@Beta public static String gt(final String expr, final Object value)`
- **Summary:** Creates a greater-than expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the greater-than expression
- **Examples:**
  - ```java
    String expr = SqlExpression.gt("age", 18);
    // Returns: "age > 18"
    ```
##### greaterThanOrEqual(...) -> String
- **Signature:** `public static String greaterThanOrEqual(final String expr, final Object value)`
- **Summary:** Creates a greater-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the greater-than-or-equal expression
- **Examples:**
  - ```java
    String expr = SqlExpression.greaterThanOrEqual("score", 60);
    // Returns: "score >= 60"
    ```
##### ge(...) -> String
- **Signature:** `@Beta public static String ge(final String expr, final Object value)`
- **Summary:** Creates a greater-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the greater-than-or-equal expression
- **Examples:**
  - ```java
    String expr = SqlExpression.ge("quantity", 1);
    // Returns: "quantity >= 1"
    ```
##### lessThan(...) -> String
- **Signature:** `public static String lessThan(final String expr, final Object value)`
- **Summary:** Creates a less-than expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the less-than expression
- **Examples:**
  - ```java
    String expr = SqlExpression.lessThan("price", 100);
    // Returns: "price < 100"
    ```
##### lt(...) -> String
- **Signature:** `@Beta public static String lt(final String expr, final Object value)`
- **Summary:** Creates a less-than expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the less-than expression
- **Examples:**
  - ```java
    String expr = SqlExpression.lt("stock", 10);
    // Returns: "stock < 10"
    ```
##### lessThanOrEqual(...) -> String
- **Signature:** `public static String lessThanOrEqual(final String expr, final Object value)`
- **Summary:** Creates a less-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the less-than-or-equal expression
- **Examples:**
  - ```java
    String expr = SqlExpression.lessThanOrEqual("discount", 50);
    // Returns: "discount <= 50"
    ```
##### le(...) -> String
- **Signature:** `@Beta public static String le(final String expr, final Object value)`
- **Summary:** Creates a less-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the less-than-or-equal expression
- **Examples:**
  - ```java
    String expr = SqlExpression.le("temperature", 32);
    // Returns: "temperature <= 32"
    ```
##### between(...) -> String
- **Signature:** `public static String between(final String expr, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN expression for a literal with min and max values.
- **Parameters:**
  - `expr` (`String`) — the expression to test
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** a SQL representation of the BETWEEN expression
- **Examples:**
  - ```java
    String expr = SqlExpression.between("age", 18, 65);
    // Returns: "age BETWEEN 18 AND 65"
    
    String expr2 = SqlExpression.between("price", 10.0, 50.0);
    // Returns: "price BETWEEN 10.0 AND 50.0"
    ```
##### notBetween(...) -> String
- **Signature:** `public static String notBetween(final String expr, final Object minValue, final Object maxValue)`
- **Summary:** Creates a NOT BETWEEN expression for a literal with min and max values.
- **Contract:**
  - A value satisfies NOT BETWEEN min AND max when it is strictly less than min or strictly greater than max, so both ends of the range are excluded.
- **Parameters:**
  - `expr` (`String`) — the expression to test
  - `minValue` (`Object`) — the lower bound of the excluded range (inclusive)
  - `maxValue` (`Object`) — the upper bound of the excluded range (inclusive)
- **Returns:** a SQL representation of the NOT BETWEEN expression
- **Examples:**
  - ```java
    String expr = SqlExpression.notBetween("age", 18, 65);
    // Returns: "age NOT BETWEEN 18 AND 65"
    
    String expr2 = SqlExpression.notBetween("price", 10.0, 50.0);
    // Returns: "price NOT BETWEEN 10.0 AND 50.0"
    ```
##### like(...) -> String
- **Signature:** `public static String like(final String expr, final String value)`
- **Summary:** Creates a LIKE expression for pattern matching.
- **Parameters:**
  - `expr` (`String`) — the expression to match
  - `value` (`String`) — the pattern to match against (can include % and _ wildcards); should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the LIKE expression
- **Examples:**
  - ```java
    String expr = SqlExpression.like("name", "John%");
    // Returns: "name LIKE 'John%'"
    
    String expr2 = SqlExpression.like("email", "%@gmail.com");
    // Returns: "email LIKE '%@gmail.com'"
    
    String expr3 = SqlExpression.like("code", "A__");
    // Returns: "code LIKE 'A__'" (matches 'A' followed by exactly 2 characters)
    ```
##### notLike(...) -> String
- **Signature:** `public static String notLike(final String expr, final String value)`
- **Summary:** Creates a NOT LIKE expression for pattern matching.
- **Parameters:**
  - `expr` (`String`) — the expression to match
  - `value` (`String`) — the pattern to exclude (can include % and _ wildcards); should not be null \\u2014 a null renders as the literal null
- **Returns:** a SQL representation of the NOT LIKE expression
- **Examples:**
  - ```java
    String expr = SqlExpression.notLike("name", "John%");
    // Returns: "name NOT LIKE 'John%'"
    
    String expr2 = SqlExpression.notLike("email", "%@gmail.com");
    // Returns: "email NOT LIKE '%@gmail.com'"
    ```
##### isNull(...) -> String
- **Signature:** `public static String isNull(final String expr)`
- **Summary:** Creates an IS NULL expression for the specified literal.
- **Parameters:**
  - `expr` (`String`) — the expression to check for null
- **Returns:** a SQL representation of the IS NULL expression
- **Examples:**
  - ```java
    String expr = SqlExpression.isNull("email");          // Returns: "email IS NULL"
    String expr2 = SqlExpression.isNull("middle_name");   // Returns: "middle_name IS NULL"
    ```
##### isNotNull(...) -> String
- **Signature:** `public static String isNotNull(final String expr)`
- **Summary:** Creates an IS NOT NULL expression for the specified literal.
- **Parameters:**
  - `expr` (`String`) — the expression to check for not null
- **Returns:** a SQL representation of the IS NOT NULL expression
- **Examples:**
  - ```java
    String expr = SqlExpression.isNotNull("email");    // Returns: "email IS NOT NULL"
    String expr2 = SqlExpression.isNotNull("phone");   // Returns: "phone IS NOT NULL"
    ```
##### isNullOrEmpty(...) -> String
- **Signature:** `public static String isNullOrEmpty(final String expr)`
- **Summary:** Creates a framework-specific IS BLANK expression for the specified literal, which the query engine interprets as a combined null-or-empty check.
- **Parameters:**
  - `expr` (`String`) — the column reference or expression to check
- **Returns:** a framework-specific IS BLANK expression string
- **Examples:**
  - ```java
    String expr = SqlExpression.isNullOrEmpty("description");   // Returns: "description IS BLANK"
    String expr2 = SqlExpression.isNullOrEmpty("address");      // Returns: "address IS BLANK"
    ```
##### isNotNullAndNotEmpty(...) -> String
- **Signature:** `public static String isNotNullAndNotEmpty(final String expr)`
- **Summary:** Creates a framework-specific IS NOT BLANK expression for the specified literal, which the query engine interprets as a combined not-null-and-not-empty check.
- **Parameters:**
  - `expr` (`String`) — the column reference or expression to check
- **Returns:** a framework-specific IS NOT BLANK expression string
- **Examples:**
  - ```java
    String expr = SqlExpression.isNotNullAndNotEmpty("name");       // Returns: "name IS NOT BLANK"
    String expr2 = SqlExpression.isNotNullAndNotEmpty("comment");   // Returns: "comment IS NOT BLANK"
    ```
##### and(...) -> String
- **Signature:** `public static String and(final String... exprs)`
- **Summary:** Creates an AND expression combining multiple literals.
- **Contract:**
  - All conditions must be true for the AND expression to be true.
- **Parameters:**
  - `exprs` (`String[]`) — the expressions to combine with AND; a null or empty array yields an empty string
- **Returns:** a SQL representation of the AND expression, or an empty string if no expressions are supplied
- **Examples:**
  - ```java
    String expr = SqlExpression.and("active = true", "age > 18", "status = 'APPROVED'");
    // Returns: "active = true AND age > 18 AND status = 'APPROVED'"
    
    String expr2 = SqlExpression.and("verified = 1", "email IS NOT NULL");
    // Returns: "verified = 1 AND email IS NOT NULL"
    ```
##### or(...) -> String
- **Signature:** `public static String or(final String... exprs)`
- **Summary:** Creates an OR expression combining multiple literals.
- **Contract:**
  - At least one condition must be true for the OR expression to be true.
- **Parameters:**
  - `exprs` (`String[]`) — the expressions to combine with OR; a null or empty array yields an empty string
- **Returns:** a SQL representation of the OR expression, or an empty string if no expressions are supplied
- **Examples:**
  - ```java
    String expr = SqlExpression.or("status = 'active'", "status = 'pending'", "priority = 1");
    // Returns: "status = 'active' OR status = 'pending' OR priority = 1"
    ```
##### plus(...) -> String
- **Signature:** `public static String plus(final Object... operands)`
- **Summary:** Creates an addition expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values to add; a null or empty array yields an empty string
- **Returns:** a SQL representation of the addition expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.plus(SqlExpression.of("price"), SqlExpression.of("tax"), SqlExpression.of("shipping"));
    // Returns: "price + tax + shipping"
    
    String expr2 = SqlExpression.plus(SqlExpression.of("base_salary"), 5000, SqlExpression.of("bonus"));
    // Returns: "base_salary + 5000 + bonus"
    ```
##### subtract(...) -> String
- **Signature:** `public static String subtract(final Object... operands)`
- **Summary:** Creates a subtraction expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values to subtract; a null or empty array yields an empty string
- **Returns:** a SQL representation of the subtraction expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.subtract(SqlExpression.of("total"), SqlExpression.of("discount"), SqlExpression.of("tax_credit"));
    // Returns: "total - discount - tax_credit"
    
    String expr2 = SqlExpression.subtract(SqlExpression.of("price"), 10);
    // Returns: "price - 10"
    ```
##### minus(...) -> String
- **Signature:** `@Deprecated public static String minus(final Object... operands)`
- **Summary:** Creates a subtraction expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values to subtract; a null or empty array yields an empty string
- **Returns:** a SQL representation of the subtraction expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.minus(SqlExpression.of("total"), SqlExpression.of("discount"));
    // Returns: "total - discount"
    
    String expr2 = SqlExpression.minus(SqlExpression.of("price"), 10);
    // Returns: "price - 10"
    
    String none = SqlExpression.minus();
    // Returns: "" (no operands)
    
    SqlExpression.minus(SqlExpression.of("x"), Double.NaN);
    // throws IllegalArgumentException (NaN has no portable SQL literal)
    ```
##### multiply(...) -> String
- **Signature:** `public static String multiply(final Object... operands)`
- **Summary:** Creates a multiplication expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values to multiply; a null or empty array yields an empty string
- **Returns:** a SQL representation of the multiplication expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.multiply(SqlExpression.of("price"), SqlExpression.of("quantity"), SqlExpression.of("tax_rate"));
    // Returns: "price * quantity * tax_rate"
    
    String expr2 = SqlExpression.multiply(SqlExpression.of("hours"), 60);
    // Returns: "hours * 60"
    ```
##### divide(...) -> String
- **Signature:** `public static String divide(final Object... operands)`
- **Summary:** Creates a division expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values to divide; a null or empty array yields an empty string
- **Returns:** a SQL representation of the division expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.divide(SqlExpression.of("total"), SqlExpression.of("count"));
    // Returns: "total / count"
    
    String expr2 = SqlExpression.divide(SqlExpression.of("distance"), SqlExpression.of("time"), 60);
    // Returns: "distance / time / 60"
    ```
##### modulus(...) -> String
- **Signature:** `public static String modulus(final Object... operands)`
- **Summary:** Creates a modulus expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values for modulus operation; a null or empty array yields an empty string
- **Returns:** a SQL representation of the modulus expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.modulus(SqlExpression.of("value"), 10);
    // Returns: "value % 10"
    
    String expr2 = SqlExpression.modulus(SqlExpression.of("id"), SqlExpression.of("batch_size"));
    // Returns: "id % batch_size"
    ```
##### leftShift(...) -> String
- **Signature:** `public static String leftShift(final Object... operands)`
- **Summary:** Creates a left shift expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values for left shift operation; a null or empty array yields an empty string
- **Returns:** a SQL representation of the left shift expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.leftShift(SqlExpression.of("flags"), 2);
    // Returns: "flags << 2"
    ```
##### rightShift(...) -> String
- **Signature:** `public static String rightShift(final Object... operands)`
- **Summary:** Creates a right shift expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values for right shift operation; a null or empty array yields an empty string
- **Returns:** a SQL representation of the right shift expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.rightShift(SqlExpression.of("value"), 4);
    // Returns: "value >> 4"
    ```
##### bitwiseAnd(...) -> String
- **Signature:** `public static String bitwiseAnd(final Object... operands)`
- **Summary:** Creates a bitwise AND expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values for bitwise AND operation; a null or empty array yields an empty string
- **Returns:** a SQL representation of the bitwise AND expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.bitwiseAnd(SqlExpression.of("permissions"), SqlExpression.of("mask"));
    // Returns: "permissions & mask"
    
    String expr2 = SqlExpression.bitwiseAnd(SqlExpression.of("flags"), 0xFF);
    // Returns: "flags & 255"
    ```
##### bitwiseOr(...) -> String
- **Signature:** `public static String bitwiseOr(final Object... operands)`
- **Summary:** Creates a bitwise OR expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values for bitwise OR operation; a null or empty array yields an empty string
- **Returns:** a SQL representation of the bitwise OR expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.bitwiseOr(SqlExpression.of("flags1"), SqlExpression.of("flags2"));
    // Returns: "flags1 | flags2"
    ```
##### bitwiseXor(...) -> String
- **Signature:** `public static String bitwiseXor(final Object... operands)`
- **Summary:** Creates a bitwise XOR expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values for bitwise XOR operation; a null or empty array yields an empty string
- **Returns:** a SQL representation of the bitwise XOR expression, or an empty string if no operands are supplied
- **Examples:**
  - ```java
    // Use SqlExpression.of() for column references to avoid single-quote wrapping
    String expr = SqlExpression.bitwiseXor(SqlExpression.of("value1"), SqlExpression.of("value2"));
    // Returns: "value1 ^ value2"
    ```
##### renderValue(...) -> String
- **Signature:** `public static String renderValue(final Object value)`
- **Summary:** Converts a value to its SQL representation.
- **Contract:**
  - This method performs SQL escaping and formatting: null values become the string "null" Strings are wrapped in single quotes and escaped via AbstractCondition#escapeStringLiteral(String): embedded unescaped single and double quotes are backslash-escaped (' becomes \\', " becomes \\"); a backslash shields the character that follows it, so any existing \\x pair \\u2014 including an already-escaped quote such as \\' \\u2014 is copied verbatim rather than escaped again, plus a defensive guard that appends one extra backslash when the body would otherwise end in an unescaped trailing backslash Number values must render as decimal, integer, or scientific-notation literals; NaN/infinite Float/Double values and non-numeric custom text are rejected.
  - SqlExpression objects return their literal SQL text (or "null" if the literal is null) SubQuery instances render their toString() wrapped in parentheses; other Conditions use their toString() verbatim Other objects are converted via N#stringOf(Object), then quoted and escaped Usage Examples:
- **Parameters:**
  - `value` (`Object`) — the value to render
- **Returns:** the SQL representation of the value
- **Examples:**
  - ```java
    SqlExpression.renderValue("text");                      // returns "'text'"
    SqlExpression.renderValue("O'Brien");                   // returns "'O\'Brien'" (single quote backslash-escaped)
    SqlExpression.renderValue("say \"hi\"");                // returns "'say \"hi\"'" (double quote backslash-escaped)
    SqlExpression.renderValue(123);                         // returns "123"
    SqlExpression.renderValue(45.67);                       // returns "45.67"
    SqlExpression.renderValue(null);                        // returns "null"
    SqlExpression.renderValue(true);                        // returns "true"
    SqlExpression.renderValue(false);                       // returns "false"
    SqlExpression.renderValue(new SqlExpression("COUNT(*)"));  // returns "COUNT(*)" (the expression's literal)
    SqlExpression.renderValue(Double.NaN);                  // throws IllegalArgumentException
    ```
##### count(...) -> String
- **Signature:** `public static String count(final String expr)`
- **Summary:** Creates a COUNT function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to count
- **Returns:** a COUNT function string
- **Examples:**
  - ```java
    String expr = SqlExpression.count("*");                      // Returns: "COUNT(*)"
    String expr2 = SqlExpression.count("id");                    // Returns: "COUNT(id)"
    String expr3 = SqlExpression.count("DISTINCT department");   // Returns: "COUNT(DISTINCT department)"
    ```
##### avg(...) -> String
- **Signature:** `public static String avg(final String expr)`
- **Summary:** Creates an AVG (average) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to average
- **Returns:** an AVG function string
- **Examples:**
  - ```java
    String expr = SqlExpression.avg("salary");   // Returns: "AVG(salary)"
    String expr2 = SqlExpression.avg("age");     // Returns: "AVG(age)"
    ```
##### sum(...) -> String
- **Signature:** `public static String sum(final String expr)`
- **Summary:** Creates a SUM function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to sum
- **Returns:** a SUM function string
- **Examples:**
  - ```java
    String expr = SqlExpression.sum("amount");              // Returns: "SUM(amount)"
    String expr2 = SqlExpression.sum("quantity * price");   // Returns: "SUM(quantity * price)"
    ```
##### min(...) -> String
- **Signature:** `public static String min(final String expr)`
- **Summary:** Creates a MIN function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to find minimum
- **Returns:** a MIN function string
- **Examples:**
  - ```java
    String expr = SqlExpression.min("price");           // Returns: "MIN(price)"
    String expr2 = SqlExpression.min("created_date");   // Returns: "MIN(created_date)"
    ```
##### max(...) -> String
- **Signature:** `public static String max(final String expr)`
- **Summary:** Creates a MAX function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to find maximum
- **Returns:** a MAX function string
- **Examples:**
  - ```java
    String expr = SqlExpression.max("score");         // Returns: "MAX(score)"
    String expr2 = SqlExpression.max("last_login");   // Returns: "MAX(last_login)"
    ```
##### abs(...) -> String
- **Signature:** `public static String abs(final String expr)`
- **Summary:** Creates an ABS (absolute value) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to get absolute value of
- **Returns:** an ABS function string
- **Examples:**
  - ```java
    String expr = SqlExpression.abs("balance");        // Returns: "ABS(balance)"
    String expr2 = SqlExpression.abs("temperature");   // Returns: "ABS(temperature)"
    ```
##### acos(...) -> String
- **Signature:** `public static String acos(final String expr)`
- **Summary:** Creates an ACOS (arc cosine) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate arc cosine of
- **Returns:** an ACOS function string
- **Examples:**
  - ```java
    String expr = SqlExpression.acos("0.5");             // Returns: "ACOS(0.5)"
    String expr2 = SqlExpression.acos("cosine_value");   // Returns: "ACOS(cosine_value)"
    ```
##### asin(...) -> String
- **Signature:** `public static String asin(final String expr)`
- **Summary:** Creates an ASIN (arc sine) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate arc sine of
- **Returns:** an ASIN function string
- **Examples:**
  - ```java
    String expr = SqlExpression.asin("0.5");           // Returns: "ASIN(0.5)"
    String expr2 = SqlExpression.asin("sine_value");   // Returns: "ASIN(sine_value)"
    ```
##### atan(...) -> String
- **Signature:** `public static String atan(final String expr)`
- **Summary:** Creates an ATAN (arc tangent) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate arc tangent of
- **Returns:** an ATAN function string
- **Examples:**
  - ```java
    String expr = SqlExpression.atan("1");                // Returns: "ATAN(1)"
    String expr2 = SqlExpression.atan("tangent_value");   // Returns: "ATAN(tangent_value)"
    ```
##### ceil(...) -> String
- **Signature:** `public static String ceil(final String expr)`
- **Summary:** Creates a CEIL (ceiling) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to round up
- **Returns:** a CEIL function string
- **Examples:**
  - ```java
    String expr = SqlExpression.ceil("4.2");      // Returns: "CEIL(4.2)"
    String expr2 = SqlExpression.ceil("price");   // Returns: "CEIL(price)"
    ```
##### cos(...) -> String
- **Signature:** `public static String cos(final String expr)`
- **Summary:** Creates a COS (cosine) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate cosine of
- **Returns:** a COS function string
- **Examples:**
  - ```java
    String expr = SqlExpression.cos("3.14159");   // Returns: "COS(3.14159)"
    String expr2 = SqlExpression.cos("angle");    // Returns: "COS(angle)"
    ```
##### exp(...) -> String
- **Signature:** `public static String exp(final String expr)`
- **Summary:** Creates an EXP (exponential) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate exponential of
- **Returns:** an EXP function string
- **Examples:**
  - ```java
    String expr = SqlExpression.exp("2");              // Returns: "EXP(2)"
    String expr2 = SqlExpression.exp("growth_rate");   // Returns: "EXP(growth_rate)"
    ```
##### floor(...) -> String
- **Signature:** `public static String floor(final String expr)`
- **Summary:** Creates a FLOOR function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to round down
- **Returns:** a FLOOR function string
- **Examples:**
  - ```java
    String expr = SqlExpression.floor("4.8");        // Returns: "FLOOR(4.8)"
    String expr2 = SqlExpression.floor("average");   // Returns: "FLOOR(average)"
    ```
##### log(...) -> String
- **Signature:** `public static String log(final String base, final String value)`
- **Summary:** Creates a LOG function expression with specified base.
- **Parameters:**
  - `base` (`String`) — the logarithm base
  - `value` (`String`) — the value to calculate logarithm of
- **Returns:** a LOG function string
- **Examples:**
  - ```java
    String expr = SqlExpression.log("10", "100");     // Returns: "LOG(10, 100)"
    String expr2 = SqlExpression.log("2", "value");   // Returns: "LOG(2, value)"
    ```
##### ln(...) -> String
- **Signature:** `public static String ln(final String expr)`
- **Summary:** Creates an LN (natural logarithm) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate natural logarithm of
- **Returns:** an LN function string
- **Examples:**
  - ```java
    String expr = SqlExpression.ln("10");       // Returns: "LN(10)"
    String expr2 = SqlExpression.ln("value");   // Returns: "LN(value)"
    ```
##### mod(...) -> String
- **Signature:** `public static String mod(final String dividend, final String divisor)`
- **Summary:** Creates a MOD (modulo) function expression.
- **Parameters:**
  - `dividend` (`String`) — the dividend
  - `divisor` (`String`) — the divisor
- **Returns:** a MOD function string
- **Examples:**
  - ```java
    String expr = SqlExpression.mod("10", "3");             // Returns: "MOD(10, 3)"
    String expr2 = SqlExpression.mod("id", "batch_size");   // Returns: "MOD(id, batch_size)"
    ```
##### power(...) -> String
- **Signature:** `public static String power(final String base, final String exponent)`
- **Summary:** Creates a POWER function expression.
- **Parameters:**
  - `base` (`String`) — the base
  - `exponent` (`String`) — the exponent
- **Returns:** a POWER function string
- **Examples:**
  - ```java
    String expr = SqlExpression.power("2", "10");             // Returns: "POWER(2, 10)"
    String expr2 = SqlExpression.power("base", "exponent");   // Returns: "POWER(base, exponent)"
    ```
##### sign(...) -> String
- **Signature:** `public static String sign(final String expr)`
- **Summary:** Creates a SIGN function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to get sign of
- **Returns:** a SIGN function string
- **Examples:**
  - ```java
    String expr = SqlExpression.sign("-5");         // Returns: "SIGN(-5)"
    String expr2 = SqlExpression.sign("balance");   // Returns: "SIGN(balance)"
    ```
##### sin(...) -> String
- **Signature:** `public static String sin(final String expr)`
- **Summary:** Creates a SIN (sine) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate sine of
- **Returns:** a SIN function string
- **Examples:**
  - ```java
    String expr = SqlExpression.sin("1.5708");   // Returns: "SIN(1.5708)"
    String expr2 = SqlExpression.sin("angle");   // Returns: "SIN(angle)"
    ```
##### sqrt(...) -> String
- **Signature:** `public static String sqrt(final String expr)`
- **Summary:** Creates a SQRT (square root) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate square root of
- **Returns:** a SQRT function string
- **Examples:**
  - ```java
    String expr = SqlExpression.sqrt("16");      // Returns: "SQRT(16)"
    String expr2 = SqlExpression.sqrt("area");   // Returns: "SQRT(area)"
    ```
##### tan(...) -> String
- **Signature:** `public static String tan(final String expr)`
- **Summary:** Creates a TAN (tangent) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate tangent of
- **Returns:** a TAN function string
- **Examples:**
  - ```java
    String expr = SqlExpression.tan("0.7854");   // Returns: "TAN(0.7854)"
    String expr2 = SqlExpression.tan("angle");   // Returns: "TAN(angle)"
    ```
##### concat(...) -> String
- **Signature:** `public static String concat(final String expr1, final String expr2)`
- **Summary:** Creates a CONCAT function expression that concatenates two operands.
- **Parameters:**
  - `expr1` (`String`) — the first SQL expression (column reference or pre-quoted literal)
  - `expr2` (`String`) — the second SQL expression (column reference or pre-quoted literal)
- **Returns:** a CONCAT function string of the form CONCAT(str1, str2)
- **Examples:**
  - ```java
    String expr = SqlExpression.concat("firstName", "' '");
    // Returns: "CONCAT(firstName, ' ')"
    
    String expr2 = SqlExpression.concat("city", "', '");
    // Returns: "CONCAT(city, ', ')"
    ```
##### replace(...) -> String
- **Signature:** `public static String replace(final String expr, final String oldString, final String replacement)`
- **Summary:** Creates a REPLACE function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to search in
  - `oldString` (`String`) — the string to search for
  - `replacement` (`String`) — the replacement string
- **Returns:** a REPLACE function string
- **Examples:**
  - ```java
    String expr = SqlExpression.replace("email", "'@'", "'_at_'");
    // Returns: "REPLACE(email, '@', '_at_')"
    
    String expr2 = SqlExpression.replace("phone", "'-'", "''");
    // Returns: "REPLACE(phone, '-', '')"
    ```
##### length(...) -> String
- **Signature:** `public static String length(final String expr)`
- **Summary:** Creates a LENGTH function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression whose length is returned
- **Returns:** a LENGTH function string
- **Examples:**
  - ```java
    String expr = SqlExpression.length("name");           // Returns: "LENGTH(name)"
    String expr2 = SqlExpression.length("description");   // Returns: "LENGTH(description)"
    ```
##### substr(...) -> String
- **Signature:** `public static String substr(final String expr, final int fromIndex)`
- **Summary:** Creates a SUBSTR function expression starting from a position.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to extract from
  - `fromIndex` (`int`) — the starting position (1-based)
- **Returns:** a SUBSTR function string
- **Examples:**
  - ```java
    String expr = SqlExpression.substr("phone", 1);   // Returns: "SUBSTR(phone, 1)"
    String expr2 = SqlExpression.substr("code", 3);   // Returns: "SUBSTR(code, 3)"
    ```
- **Signature:** `public static String substr(final String expr, final int fromIndex, final int length)`
- **Summary:** Creates a SUBSTR function expression with start position and length.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to extract from
  - `fromIndex` (`int`) — the starting position (1-based)
  - `length` (`int`) — the number of characters to extract
- **Returns:** a SUBSTR function string
- **Examples:**
  - ```java
    String expr = SqlExpression.substr("phone", 1, 3);   // Returns: "SUBSTR(phone, 1, 3)"
    String expr2 = SqlExpression.substr("zip", 1, 5);    // Returns: "SUBSTR(zip, 1, 5)"
    ```
##### trim(...) -> String
- **Signature:** `public static String trim(final String expr)`
- **Summary:** Creates a TRIM function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to trim
- **Returns:** a TRIM function string
- **Examples:**
  - ```java
    String expr = SqlExpression.trim("input");        // Returns: "TRIM(input)"
    String expr2 = SqlExpression.trim("user_name");   // Returns: "TRIM(user_name)"
    ```
##### ltrim(...) -> String
- **Signature:** `public static String ltrim(final String expr)`
- **Summary:** Creates an LTRIM (left trim) function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to left trim
- **Returns:** an LTRIM function string
- **Examples:**
  - ```java
    String expr = SqlExpression.ltrim("comment");    // Returns: "LTRIM(comment)"
    String expr2 = SqlExpression.ltrim("address");   // Returns: "LTRIM(address)"
    ```
##### rtrim(...) -> String
- **Signature:** `public static String rtrim(final String expr)`
- **Summary:** Creates an RTRIM (right trim) function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to right trim
- **Returns:** an RTRIM function string
- **Examples:**
  - ```java
    String expr = SqlExpression.rtrim("code");           // Returns: "RTRIM(code)"
    String expr2 = SqlExpression.rtrim("description");   // Returns: "RTRIM(description)"
    ```
##### lpad(...) -> String
- **Signature:** `public static String lpad(final String expr, final int length, final String padExpr)`
- **Summary:** Creates an LPAD (left pad) function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to pad
  - `length` (`int`) — the total length after padding
  - `padExpr` (`String`) — the SQL expression to pad with
- **Returns:** an LPAD function string
- **Examples:**
  - ```java
    String expr = SqlExpression.lpad("id", 10, "'0'");     // Returns: "LPAD(id, 10, '0')"
    String expr2 = SqlExpression.lpad("code", 5, "' '");   // Returns: "LPAD(code, 5, ' ')"
    ```
##### rpad(...) -> String
- **Signature:** `public static String rpad(final String expr, final int length, final String padExpr)`
- **Summary:** Creates an RPAD (right pad) function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to pad
  - `length` (`int`) — the total length after padding
  - `padExpr` (`String`) — the SQL expression to pad with
- **Returns:** an RPAD function string
- **Examples:**
  - ```java
    String expr = SqlExpression.rpad("name", 20, "' '");    // Returns: "RPAD(name, 20, ' ')"
    String expr2 = SqlExpression.rpad("code", 10, "'X'");   // Returns: "RPAD(code, 10, 'X')"
    ```
##### lower(...) -> String
- **Signature:** `public static String lower(final String expr)`
- **Summary:** Creates a LOWER function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to convert to lowercase
- **Returns:** a LOWER function string
- **Examples:**
  - ```java
    String expr = SqlExpression.lower("email");      // Returns: "LOWER(email)"
    String expr2 = SqlExpression.lower("COUNTRY");   // Returns: "LOWER(COUNTRY)"
    ```
##### upper(...) -> String
- **Signature:** `public static String upper(final String expr)`
- **Summary:** Creates an UPPER function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to convert to uppercase
- **Returns:** an UPPER function string
- **Examples:**
  - ```java
    String expr = SqlExpression.upper("name");            // Returns: "UPPER(name)"
    String expr2 = SqlExpression.upper("country_code");   // Returns: "UPPER(country_code)"
    ```

#### Public Instance Methods
##### literal(...) -> String
- **Signature:** `public String literal()`
- **Summary:** Returns the raw SQL text represented by this expression.
- **Parameters:**
  - (none)
- **Returns:** the SQL expression string; never null for instances created via the public constructor or #of(String), but may be null for uninitialized instances produced by the package-private default constructor (e.g., during Kryo deserialization)
- **Examples:**
  - ```java
    SqlExpression expr = new SqlExpression("price * quantity");
    String literal = expr.literal();   // Returns "price * quantity"
    
    SqlExpression expr2 = SqlExpression.of("CURRENT_TIMESTAMP");
    String literal2 = expr2.literal();   // Returns "CURRENT_TIMESTAMP"
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns an empty list as expressions have no parameters.
- **Parameters:**
  - (none)
- **Returns:** an empty immutable list
- **Examples:**
  - ```java
    ImmutableList<Object> params = SqlExpression.of("price * quantity").parameters();   // returns []
    boolean empty = params.isEmpty();                                                   // returns true
    SqlExpression.of("id = 5").parameters();                                            // returns [] (value is part of the literal, not a parameter)
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Returns the string form of this expression, with the naming policy applied to any identifiers (column or property names) that can be detected within the literal.
- **Contract:**
  - Recognized SQL keyword tokens are also left unchanged when written in their canonical upper-case form (for example CURRENT_DATE); a lower-case token is treated as an identifier and converted.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to detected identifiers; if null, NamingPolicy#NO_CHANGE is used
- **Returns:** the expression string with identifiers converted according to the naming policy
- **Examples:**
  - ```java
    SqlExpression.of("firstName").toSql(NamingPolicy.SNAKE_CASE);          // returns "first_name"
    SqlExpression.of("_firstName").toSql(NamingPolicy.SNAKE_CASE);         // returns "_first_name"
    SqlExpression.of("firstName = 'John'").toSql(NamingPolicy.SNAKE_CASE); // returns "first_name = 'John'" (identifier converted, quoted literal kept)
    SqlExpression.of("price-tax").toSql(NamingPolicy.CAMEL_CASE);          // returns "price-tax" (SQL subtraction preserved; each operand converted independently)
    SqlExpression.of("price  *  2").toSql(NamingPolicy.NO_CHANGE);         // returns "price * 2" (parser path collapses whitespace runs)
    SqlExpression.of("firstName").toSql(NamingPolicy.NO_CHANGE);           // returns "firstName"
    SqlExpression.of("firstName").toSql(null);                             // returns "firstName" (null defaults to NO_CHANGE)
    SqlExpression.of("").toSql(NamingPolicy.NO_CHANGE);                    // returns "" (empty literal)
    // an uninitialized instance (null literal, only possible via deserialization) returns "null"
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Computes the hash code based on the literal string.
- **Parameters:**
  - (none)
- **Returns:** the hash code of the literal
- **Examples:**
  - ```java
    new SqlExpression("price * quantity").hashCode();                            // returns "price * quantity".hashCode()
    SqlExpression.of("a + b").hashCode() == SqlExpression.of("a + b").hashCode();   // true (same literal)
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this expression equals another object.
- **Contract:**
  - Checks if this expression equals another object.
  - Two expressions are equal if they are both SqlExpression instances with the same literal string.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal
- **Examples:**
  - ```java
    new SqlExpression("a + b").equals(new SqlExpression("a + b"));   // returns true (same literal)
    SqlExpression.of("a + b").equals(SqlExpression.of("a + b"));     // returns true (cached, same instance)
    new SqlExpression("a + b").equals(new SqlExpression("a - b"));   // returns false (different literal)
    new SqlExpression("a + b").equals("a + b");                   // returns false (not an SqlExpression)
    ```

### Class SubQuery (com.landawn.abacus.query.condition.SubQuery)
Represents raw query-expression text or a structured SELECT used within SQL conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public SubQuery(final String sql)` — Creates a subquery with raw SQL.
- `@Deprecated public SubQuery(final String entityName, final String sql)` — Creates a subquery with an entity name and raw SQL.
- `public SubQuery(final String entityName, final String propName, final Condition condition)` — Creates a structured single-property subquery for an entity name.
- `public SubQuery(final String entityName, final Collection<String> propNames, final Condition condition)` — Creates a structured subquery with entity name, selected properties, and condition.
- `public SubQuery(final Class<?> entityClass, final String propName, final Condition condition)` — Creates a structured single-property subquery for an entity class.
- `public SubQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition)` — Creates a structured subquery with entity class, selected properties, and condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### rawSql(...) -> String
- **Signature:** `public String rawSql()`
- **Summary:** Returns the raw SQL script if this is a raw SQL subquery.
- **Contract:**
  - Returns the raw SQL script if this is a raw SQL subquery.
- **Parameters:**
  - (none)
- **Returns:** the SQL script, or null if this is a structured subquery
- **Examples:**
  - ```java
    // Raw SQL subquery
    SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE status = 'active'");
    String sql = rawQuery.rawSql();
    // Returns: "SELECT id FROM users WHERE status = 'active'"
    
    // Structured subquery returns null for rawSql()
    SubQuery structured = new SubQuery("users", Arrays.asList("id"), Filters.equal("status", "active"));
    String structuredSql = structured.rawSql();
    // Returns: null
    ```
##### entityName(...) -> String
- **Signature:** `public String entityName()`
- **Summary:** Returns the entity/table name for this subquery.
- **Parameters:**
  - (none)
- **Returns:** the entity/table name; an empty string for an initialized raw subquery without an associated entity, or null only for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Structured subquery with entity name
    SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), Filters.equal("active", true));
    String entityName = subQuery.entityName();
    // Returns: "users"
    
    // Raw SQL subquery with entity name
    SubQuery rawQuery = new SubQuery("orders", "SELECT order_id FROM orders WHERE total > 1000");
    String name = rawQuery.entityName();
    // Returns: "orders"
    
    // Raw SQL subquery without entity name
    SubQuery simpleRaw = new SubQuery("SELECT id FROM users");
    String emptyName = simpleRaw.entityName();
    // Returns: "" (empty string)
    ```
##### entityClass(...) -> Class<?>
- **Signature:** `public Class<?> entityClass()`
- **Summary:** Returns the entity class if this subquery was created with a class reference.
- **Contract:**
  - Returns the entity class if this subquery was created with a class reference.
- **Parameters:**
  - (none)
- **Returns:** the entity class, or null if created with an entity name string or raw SQL
- **Examples:**
  - ```java
    // Subquery created with entity class
    SubQuery subQuery = new SubQuery(Product.class, Arrays.asList("id", "name"), Filters.equal("active", true));
    Class<?> entityClass = subQuery.entityClass();
    // Returns: Product.class
    
    // Subquery created with entity name string returns null
    SubQuery namedQuery = new SubQuery("products", Arrays.asList("id"), Filters.equal("active", true));
    Class<?> clazz = namedQuery.entityClass();
    // Returns: null
    ```
##### selectPropNames(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> selectPropNames()`
- **Summary:** Returns the collection of property names to select in this subquery.
- **Parameters:**
  - (none)
- **Returns:** immutable list of property names to select, or null for raw SQL subqueries
- **Examples:**
  - ```java
    // Structured subquery with selected properties
    SubQuery subQuery = new SubQuery("users", Arrays.asList("id", "email", "name"), Filters.equal("active", true));
    Collection<String> propNames = subQuery.selectPropNames();
    // Returns: ["id", "email", "name"]
    
    // Raw SQL subquery returns null
    SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE active = true");
    Collection<String> rawProps = rawQuery.selectPropNames();
    // Returns: null
    ```
##### condition(...) -> Condition
- **Signature:** `public Condition condition()`
- **Summary:** Returns the trailing condition or clause for this structured subquery.
- **Parameters:**
  - (none)
- **Returns:** the normalized condition/clause, or null if none or for a raw SQL subquery
- **Examples:**
  - ```java
    // Structured subquery with condition
    Condition activeCondition = Filters.equal("active", true);
    SubQuery subQuery = new SubQuery("users", Arrays.asList("id"), activeCondition);
    Condition condition = subQuery.condition();
    // Returns the wrapped WHERE condition: WHERE active = true
    
    // Raw SQL subquery returns null for condition()
    SubQuery rawQuery = new SubQuery("SELECT id FROM users WHERE active = true");
    Condition rawCondition = rawQuery.condition();
    // Returns: null
    ```
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the list of parameter values from the condition.
- **Contract:**
  - These are the parameter values that will be bound to the prepared statement placeholders when the query is executed.
  - &#9888;&#65039; For raw SQL subqueries this returns an empty list even when the raw text contains placeholders; raw bindings are managed by the caller.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values, or an empty immutable list if no condition or raw SQL subquery
- **Examples:**
  - ```java
    // Structured subquery: parameters are collected from the condition (in order)
    Condition cond = Filters.and(Filters.equal("status", "active"), Filters.between("age", 18, 65));
    SubQuery structured = Filters.subQuery("users", Arrays.asList("id"), cond);
    List<Object> params = structured.parameters();
    // returns ["active", 18, 65]
    
    // Raw SQL subquery: no bound parameters
    SubQuery raw = Filters.subQuery("SELECT * FROM users");
    List<Object> rawParams = raw.parameters();
    // returns [] (empty immutable list)
    
    // Structured subquery without a condition: also empty
    SubQuery noCond = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);
    List<Object> noCondParams = noCond.parameters();
    // returns [] (empty immutable list)
    ```
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this subquery to its SQL representation.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply; if null, com.landawn.abacus.util.NamingPolicy#NO_CHANGE is used
- **Returns:** SQL representation of the subquery
- **Examples:**
  - ```java
    // Raw SQL subquery: SQL returned as-is, naming policy is ignored
    SubQuery raw = Filters.subQuery("SELECT user_id FROM users");
    String rawStr = raw.toSql(NamingPolicy.SNAKE_CASE);
    // returns "SELECT user_id FROM users"
    
    // Structured subquery with NO_CHANGE: names rendered verbatim
    SubQuery sq = Filters.subQuery("users", Arrays.asList("id", "name"), Filters.equal("status", "active"));
    String s1 = sq.toSql(NamingPolicy.NO_CHANGE);
    // returns "SELECT id, name FROM users WHERE status = 'active'"
    
    // Structured subquery with SNAKE_CASE: props, entity, and condition are all converted
    SubQuery sq2 = Filters.subQuery("userAccount", Arrays.asList("firstName", "lastName"),
                                    Filters.equal("isActive", true));
    String s2 = sq2.toSql(NamingPolicy.SNAKE_CASE);
    // returns "SELECT first_name, last_name FROM user_account WHERE is_active = true"
    
    // null naming policy behaves like NO_CHANGE
    SubQuery sq3 = Filters.subQuery("users", Arrays.asList("id"), (Condition) null);
    String s3 = sq3.toSql((NamingPolicy) null);
    // returns "SELECT id FROM users"
    ```
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this subquery.
- **Parameters:**
  - (none)
- **Returns:** hash code based on sql, entity name/class, properties, and condition
- **Examples:**
  - ```java
    // Equal subqueries produce equal hash codes
    SubQuery a = Filters.subQuery("SELECT id FROM users");
    SubQuery b = Filters.subQuery("SELECT id FROM users");
    boolean sameHash = a.hashCode() == b.hashCode();
    // returns true
    
    // Different SQL typically produces different hash codes
    SubQuery c = Filters.subQuery("SELECT id FROM customers");
    boolean differentHash = a.hashCode() != c.hashCode();
    // returns true
    ```
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this subquery is equal to another object.
- **Contract:**
  - Checks if this subquery is equal to another object.
  - Two subqueries are equal only when all of their identity fields are equal: the entity name, the entity class, the selected properties, the raw SQL string, and the condition.
  - The entity name participates even for raw-SQL subqueries, so two raw subqueries are equal only when both their SQL and their entity name match.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** true if the objects are equal, false otherwise
- **Examples:**
  - ```java
    // Same raw SQL: equal
    SubQuery a = Filters.subQuery("SELECT id FROM users");
    SubQuery b = Filters.subQuery("SELECT id FROM users");
    boolean eq = a.equals(b);
    // returns true
    
    // Different raw SQL: not equal
    SubQuery c = Filters.subQuery("SELECT id FROM customers");
    boolean ne = a.equals(c);
    // returns false
    
    // A class-based subquery is NOT equal to a name-based one, even with the same simple name,
    // because the entity class is part of identity.
    SubQuery classBased = Filters.subQuery(Product.class, Arrays.asList("id"), Filters.equal("active", true));
    SubQuery nameBased = Filters.subQuery("Product", Arrays.asList("id"), Filters.equal("active", true));
    boolean differ = classBased.equals(nameBased);
    // returns false
    
    // Comparison with null or a different type
    boolean vsNull = a.equals(null);   // returns false
    boolean vsString = a.equals("x");  // returns false
    ```

### Class Union (com.landawn.abacus.query.condition.Union)
Represents a UNION clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Union(final SubQuery subQuery)` — Creates a new UNION clause with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this UNION clause.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT id, name FROM customers WHERE region = 'East'");
    Union union = new Union(subQuery);
    SubQuery retrieved = union.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = union.subQuery() == union.condition();
    // returns true
    ```

### Class UnionAll (com.landawn.abacus.query.condition.UnionAll)
Represents a UNION ALL clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public UnionAll(final SubQuery subQuery)` — Creates a new UNION ALL clause with the specified subquery.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this UNION ALL clause.
- **Parameters:**
  - (none)
- **Returns:** the SubQuery supplied at construction time, or null for an uninitialized serialization-framework instance
- **Examples:**
  - ```java
    // Retrieve the wrapped subquery
    SubQuery subQuery = Filters.subQuery("SELECT order_id, amount FROM orders WHERE region = 'EAST'");
    UnionAll unionAll = new UnionAll(subQuery);
    SubQuery retrieved = unionAll.subQuery();
    // returns the subquery passed to the constructor
    
    // The wrapped subquery is also what condition() returns
    boolean sameAsCondition = unionAll.subQuery() == unionAll.condition();
    // returns true
    ```

### Class Using (com.landawn.abacus.query.condition.Using)
Represents a USING clause in SQL JOIN operations.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `@Deprecated public Using(final String... columnNames)` — Creates a USING clause with the specified column names.
- `@Deprecated public Using(final Collection<String> columnNames)` — Creates a USING clause with a collection of column names.

#### Public Static Methods
- (none)

#### Public Instance Methods
##### columnNames(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> columnNames()`
- **Summary:** Returns the validated column names this USING clause joins on, in the order they were supplied.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of the unqualified column names in supplied order, or an empty immutable list for an uninitialized instance produced by the package-private default constructor
- **Examples:**
  - ```java
    Using using = new Using("company_id", "branch_id");
    List<String> cols = using.columnNames();
    // cols = ["company_id", "branch_id"]
    
    // Edge: the returned list is immutable
    cols.add("extra");   // throws UnsupportedOperationException
    
    // Edge: an uninitialized instance (created only via the package-private default
    // constructor, e.g. during Kryo deserialization) returns an empty list
    ```

### Class Where (com.landawn.abacus.query.condition.Where)
Represents a WHERE clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- `public Where(final Condition condition)` — Creates a WHERE clause with the specified condition.

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

