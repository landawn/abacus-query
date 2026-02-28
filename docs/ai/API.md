# abacus-query API Index (v4.6.1)
- Build: unknown
- Java: 17
- Generated: 2026-02-28

## Packages
- com.landawn.abacus.query
- com.landawn.abacus.query.condition

## com.landawn.abacus.query
### Class AbstractQueryBuilder (com.landawn.abacus.query.AbstractQueryBuilder)
A fluent SQL builder for constructing SQL statements programmatically.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### setHandlerForNamedParameter(...) -> void
- **Signature:** `public static void setHandlerForNamedParameter(final BiConsumer<StringBuilder, String> handlerForNamedParameter)`
- **Summary:** Sets a custom handler for formatting named parameters in SQL strings.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Use MyBatis-style named parameters: #{paramName} AbstractQueryBuilder.setHandlerForNamedParameter( (sb, propName) -> sb.append("#{").append(propName).append("}")); // Reset to default when done AbstractQueryBuilder.resetHandlerForNamedParameter(); } </pre>
- **Parameters:**
  - `handlerForNamedParameter` (`BiConsumer<StringBuilder, String>`) — the handler to format named parameters; must not be null
##### resetHandlerForNamedParameter(...) -> void
- **Signature:** `public static void resetHandlerForNamedParameter()`
- **Summary:** Resets the named parameter handler to the default format.
- **Parameters:**
  - (none)

#### Public Instance Methods
##### into(...) -> This
- **Signature:** `public This into(final String tableName)`
- **Summary:** Specifies the target table for an INSERT or SELECT INTO operation.
- **Contract:**
  - <p> Must be called after setting the columns/values to insert or columns to select.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to insert into
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This into(final Class<?> entityClass)`
- **Summary:** Specifies the target table for an INSERT operation using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the target table
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This into(final String tableName, final Class<?> entityClass)`
- **Summary:** Specifies the target table for an INSERT operation with explicit table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to insert into
  - `entityClass` (`Class<?>`) — the entity class for property mapping (can be null)
- **Returns:** this SQLBuilder instance for method chaining
##### distinct(...) -> This
- **Signature:** `public This distinct()`
- **Summary:** Adds DISTINCT clause to the SELECT statement.
- **Parameters:**
  - (none)
- **Returns:** this SQLBuilder instance for method chaining
##### preselect(...) -> This
- **Signature:** `public This preselect(final String preselect)`
- **Summary:** Adds a pre-select modifier to the SELECT statement.
- **Contract:**
  - <p> For better performance, this method should be called before {@code from} .
- **Parameters:**
  - `preselect` (`String`) — modifiers like ALL, DISTINCT, DISTINCTROW, TOP, etc.
- **Returns:** this SQLBuilder instance for method chaining
##### from(...) -> This
- **Signature:** `public This from(final String... tableNames)`
- **Summary:** Sets the FROM clause with multiple table names.
- **Parameters:**
  - `tableNames` (`String[]`) — the table names to use in the FROM clause
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This from(final Collection<String> tableNames)`
- **Summary:** Sets the FROM clause with a collection of table names.
- **Parameters:**
  - `tableNames` (`Collection<String>`) — the collection of table names to use in the FROM clause
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This from(String expr)`
- **Summary:** Sets the FROM clause with a single expression.
- **Parameters:**
  - `expr` (`String`) — the FROM clause expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This from(final String expr, final Class<?> entityClass)`
- **Summary:** Sets the FROM clause with an expression and associates it with an entity class.
- **Parameters:**
  - `expr` (`String`) — the FROM clause expression
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This from(final Class<?> entityClass)`
- **Summary:** Sets the FROM clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This from(final Class<?> entityClass, final String alias)`
- **Summary:** Sets the FROM clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table
  - `alias` (`String`) — the table alias
- **Returns:** this SQLBuilder instance for method chaining
##### join(...) -> This
- **Signature:** `public This join(final String expr)`
- **Summary:** Adds a JOIN clause to the SQL statement.
- **Parameters:**
  - `expr` (`String`) — the join expression (e.g., "orders o ON u.id = o.user_id")
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This join(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass)`
- **Summary:** Adds a JOIN clause with a table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the table name to join
  - `entityClass` (`@SuppressWarnings(value = "unused") Class<?>`) — the entity class (currently unused but reserved for future use)
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This join(final Class<?> entityClass)`
- **Summary:** Adds a JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This join(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SQLBuilder instance for method chaining
##### innerJoin(...) -> This
- **Signature:** `public This innerJoin(final String expr)`
- **Summary:** Adds an INNER JOIN clause to the SQL statement.
- **Parameters:**
  - `expr` (`String`) — the join expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This innerJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass)`
- **Summary:** Adds an INNER JOIN clause with a table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the table name to join
  - `entityClass` (`@SuppressWarnings(value = "unused") Class<?>`) — the entity class (currently unused but reserved for future use)
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This innerJoin(final Class<?> entityClass)`
- **Summary:** Adds an INNER JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This innerJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds an INNER JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SQLBuilder instance for method chaining
##### leftJoin(...) -> This
- **Signature:** `public This leftJoin(final String expr)`
- **Summary:** Adds a LEFT JOIN clause to the SQL statement.
- **Parameters:**
  - `expr` (`String`) — the join expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This leftJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass)`
- **Summary:** Adds a LEFT JOIN clause with a table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the table name to join
  - `entityClass` (`@SuppressWarnings(value = "unused") Class<?>`) — the entity class (currently unused but reserved for future use)
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This leftJoin(final Class<?> entityClass)`
- **Summary:** Adds a LEFT JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This leftJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a LEFT JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SQLBuilder instance for method chaining
##### rightJoin(...) -> This
- **Signature:** `public This rightJoin(final String expr)`
- **Summary:** Adds a RIGHT JOIN clause to the SQL statement.
- **Parameters:**
  - `expr` (`String`) — the join expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This rightJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass)`
- **Summary:** Adds a RIGHT JOIN clause with a table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the table name to join
  - `entityClass` (`@SuppressWarnings(value = "unused") Class<?>`) — the entity class (currently unused but reserved for future use)
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This rightJoin(final Class<?> entityClass)`
- **Summary:** Adds a RIGHT JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This rightJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a RIGHT JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SQLBuilder instance for method chaining
##### fullJoin(...) -> This
- **Signature:** `public This fullJoin(final String expr)`
- **Summary:** Adds a FULL JOIN clause to the SQL statement.
- **Parameters:**
  - `expr` (`String`) — the join expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This fullJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass)`
- **Summary:** Adds a FULL JOIN clause with a table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the table name to join
  - `entityClass` (`@SuppressWarnings(value = "unused") Class<?>`) — the entity class (currently unused but reserved for future use)
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This fullJoin(final Class<?> entityClass)`
- **Summary:** Adds a FULL JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This fullJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a FULL JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SQLBuilder instance for method chaining
##### crossJoin(...) -> This
- **Signature:** `public This crossJoin(final String expr)`
- **Summary:** Adds a CROSS JOIN clause to the SQL statement.
- **Parameters:**
  - `expr` (`String`) — the join expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This crossJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass)`
- **Summary:** Adds a CROSS JOIN clause with a table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the table name to join
  - `entityClass` (`@SuppressWarnings(value = "unused") Class<?>`) — the entity class (currently unused but reserved for future use)
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This crossJoin(final Class<?> entityClass)`
- **Summary:** Adds a CROSS JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This crossJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a CROSS JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SQLBuilder instance for method chaining
##### naturalJoin(...) -> This
- **Signature:** `public This naturalJoin(final String expr)`
- **Summary:** Adds a NATURAL JOIN clause to the SQL statement.
- **Parameters:**
  - `expr` (`String`) — the join expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This naturalJoin(final String tableName, @SuppressWarnings("unused") final Class<?> entityClass)`
- **Summary:** Adds a NATURAL JOIN clause with a table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the table name to join
  - `entityClass` (`@SuppressWarnings(value = "unused") Class<?>`) — the entity class (currently unused but reserved for future use)
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This naturalJoin(final Class<?> entityClass)`
- **Summary:** Adds a NATURAL JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This naturalJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a NATURAL JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SQLBuilder instance for method chaining
##### on(...) -> This
- **Signature:** `public This on(final String expr)`
- **Summary:** Adds an ON clause for join conditions.
- **Parameters:**
  - `expr` (`String`) — the join condition expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This on(final Condition cond)`
- **Summary:** Adds an ON clause with a condition object for join conditions.
- **Parameters:**
  - `cond` (`Condition`) — the join condition
- **Returns:** this SQLBuilder instance for method chaining
##### using(...) -> This
- **Signature:** `public This using(final String expr)`
- **Summary:** Adds a USING clause for join conditions.
- **Parameters:**
  - `expr` (`String`) — the column name(s) for the USING clause
- **Returns:** this SQLBuilder instance for method chaining
##### where(...) -> This
- **Signature:** `public This where(final String expr)`
- **Summary:** Adds a WHERE clause with a string expression.
- **Parameters:**
  - `expr` (`String`) — the WHERE condition expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This where(final Condition cond)`
- **Summary:** Adds a WHERE clause with a condition object.
- **Parameters:**
  - `cond` (`Condition`) — the WHERE condition
- **Returns:** this SQLBuilder instance for method chaining
- **See also:** Filters
##### groupBy(...) -> This
- **Signature:** `public This groupBy(final String expr)`
- **Summary:** Adds a GROUP BY clause with a single column.
- **Parameters:**
  - `expr` (`String`) — the column to group by
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This groupBy(final String... propOrColumnNames)`
- **Summary:** Adds a GROUP BY clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to group by
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This groupBy(final String columnName, final SortDirection direction)`
- **Summary:** Adds a GROUP BY clause with a single column and sort direction.
- **Parameters:**
  - `columnName` (`String`) — the column to group by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This groupBy(final Collection<String> propOrColumnNames)`
- **Summary:** Adds a GROUP BY clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This groupBy(final Collection<String> propOrColumnNames, final SortDirection direction)`
- **Summary:** Adds a GROUP BY clause with a collection of columns and sort direction.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by
  - `direction` (`SortDirection`) — the direction appended after the grouped column list (affecting the trailing expression in the generated SQL)
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This groupBy(final Map<String, SortDirection> orders)`
- **Summary:** Adds a GROUP BY clause with columns and individual sort directions.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of columns to their sort directions
- **Returns:** this SQLBuilder instance for method chaining
##### having(...) -> This
- **Signature:** `public This having(final String expr)`
- **Summary:** Adds a HAVING clause with a string expression.
- **Parameters:**
  - `expr` (`String`) — the HAVING condition expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This having(final Condition cond)`
- **Summary:** Adds a HAVING clause with a condition object.
- **Parameters:**
  - `cond` (`Condition`) — the HAVING condition
- **Returns:** this SQLBuilder instance for method chaining
- **See also:** Filters
##### orderBy(...) -> This
- **Signature:** `public This orderBy(final String expr)`
- **Summary:** Adds an ORDER BY clause with a single column.
- **Parameters:**
  - `expr` (`String`) — the column to order by
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This orderBy(final String... propOrColumnNames)`
- **Summary:** Adds an ORDER BY clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to order by
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This orderBy(final String columnName, final SortDirection direction)`
- **Summary:** Adds an ORDER BY clause with a single column and sort direction.
- **Parameters:**
  - `columnName` (`String`) — the column to order by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This orderBy(final Collection<String> propOrColumnNames)`
- **Summary:** Adds an ORDER BY clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This orderBy(final Collection<String> propOrColumnNames, final SortDirection direction)`
- **Summary:** Adds an ORDER BY clause with a collection of columns and sort direction.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by
  - `direction` (`SortDirection`) — the direction appended after the ordered column list (affecting the trailing expression in the generated SQL)
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Adds an ORDER BY clause with columns and individual sort directions.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of columns to their sort directions
- **Returns:** this SQLBuilder instance for method chaining
##### orderByAsc(...) -> This
- **Signature:** `@Beta public This orderByAsc(final String expr)`
- **Summary:** Adds an ORDER BY ASC clause with a single column.
- **Parameters:**
  - `expr` (`String`) — the column to order by ascending
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This orderByAsc(final String... propOrColumnNames)`
- **Summary:** Adds an ORDER BY ASC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to order by ascending
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This orderByAsc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds an ORDER BY ASC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by ascending
- **Returns:** this SQLBuilder instance for method chaining
##### orderByDesc(...) -> This
- **Signature:** `@Beta public This orderByDesc(final String expr)`
- **Summary:** Adds an ORDER BY DESC clause with a single column.
- **Parameters:**
  - `expr` (`String`) — the column to order by descending
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This orderByDesc(final String... propOrColumnNames)`
- **Summary:** Adds an ORDER BY DESC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to order by descending
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This orderByDesc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds an ORDER BY DESC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by descending
- **Returns:** this SQLBuilder instance for method chaining
##### limit(...) -> This
- **Signature:** `public This limit(final int count)`
- **Summary:** Adds a LIMIT clause to restrict the number of rows returned.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This limit(final int offset, final int count)`
- **Summary:** Adds a LIMIT clause with an offset for pagination.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip (appears as OFFSET in SQL)
  - `count` (`int`) — the maximum number of rows to return (appears as LIMIT in SQL)
- **Returns:** this SQLBuilder instance for method chaining
##### offset(...) -> This
- **Signature:** `public This offset(final int offset)`
- **Summary:** Adds an OFFSET clause to skip a number of rows.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this SQLBuilder instance for method chaining
##### offsetRows(...) -> This
- **Signature:** `public This offsetRows(final int offset)`
- **Summary:** Adds an OFFSET ROWS clause (SQL Server syntax).
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this SQLBuilder instance for method chaining
##### fetchNextNRowsOnly(...) -> This
- **Signature:** `public This fetchNextNRowsOnly(final int n)`
- **Summary:** Adds a FETCH NEXT N ROWS ONLY clause (SQL Server syntax).
- **Parameters:**
  - `n` (`int`) — the number of rows to fetch
- **Returns:** this SQLBuilder instance for method chaining
##### fetchFirstNRowsOnly(...) -> This
- **Signature:** `public This fetchFirstNRowsOnly(final int n)`
- **Summary:** Adds a FETCH FIRST N ROWS ONLY clause (SQL standard syntax).
- **Parameters:**
  - `n` (`int`) — the number of rows to fetch
- **Returns:** this SQLBuilder instance for method chaining
##### append(...) -> This
- **Signature:** `@Beta public This append(final Condition cond)`
- **Summary:** Appends a {@code Criteria} or {@code Where} condition to the SQL statement.
- **Contract:**
  - Automatically adds WHERE clause if not already present.
- **Parameters:**
  - `cond` (`Condition`) — the condition to append
- **Returns:** this SQLBuilder instance for method chaining
- **See also:** Filters
- **Signature:** `public This append(final String expr)`
- **Summary:** Appends a string expression to the SQL statement.
- **Parameters:**
  - `expr` (`String`) — the expression to append
- **Returns:** this SQLBuilder instance for method chaining
##### appendIf(...) -> This
- **Signature:** `@Beta public This appendIf(final boolean b, final Condition cond)`
- **Summary:** Conditionally appends a condition to the SQL statement.
- **Parameters:**
  - `b` (`boolean`) — if true, the condition will be appended
  - `cond` (`Condition`) — the condition to append
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This appendIf(final boolean b, final String expr)`
- **Summary:** Conditionally appends a string expression to the SQL statement.
- **Parameters:**
  - `b` (`boolean`) — if true, the expression will be appended
  - `expr` (`String`) — the expression to append
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This appendIf(final boolean b, final java.util.function.Consumer<? super This> append)`
- **Summary:** Conditionally executes an append operation using a consumer function.
- **Parameters:**
  - `b` (`boolean`) — if true, the consumer will be executed
  - `append` (`java.util.function.Consumer<? super This>`) — the consumer function to execute
- **Returns:** this SQLBuilder instance for method chaining
##### appendIfOrElse(...) -> This
- **Signature:** `@Beta public This appendIfOrElse(final boolean b, final Condition condToAppendForTrue, final Condition condToAppendForFalse)`
- **Summary:** Conditionally appends one of two conditions based on a boolean value.
- **Parameters:**
  - `b` (`boolean`) — if true, append condToAppendForTrue; otherwise append condToAppendForFalse
  - `condToAppendForTrue` (`Condition`) — the condition to append if b is true
  - `condToAppendForFalse` (`Condition`) — the condition to append if b is false
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `@Beta public This appendIfOrElse(final boolean b, final String exprToAppendForTrue, final String exprToAppendForFalse)`
- **Summary:** Conditionally appends one of two string expressions based on a boolean value.
- **Parameters:**
  - `b` (`boolean`) — if true, append exprToAppendForTrue; otherwise append exprToAppendForFalse
  - `exprToAppendForTrue` (`String`) — the expression to append if b is true
  - `exprToAppendForFalse` (`String`) — the expression to append if b is false
- **Returns:** this SQLBuilder instance for method chaining
##### union(...) -> This
- **Signature:** `public This union(final This sqlBuilder)`
- **Summary:** Adds a UNION clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to union
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This union(final String query)`
- **Summary:** Adds a UNION clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the SQL query to union
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This union(final String... propOrColumnNames)`
- **Summary:** Starts a new SELECT query for UNION operation.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns for the union query
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This union(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for UNION operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the union query
- **Returns:** this SQLBuilder instance for method chaining
##### unionAll(...) -> This
- **Signature:** `public This unionAll(final This sqlBuilder)`
- **Summary:** Adds a UNION ALL clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to union all
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This unionAll(final String query)`
- **Summary:** Adds a UNION ALL clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the SQL query to union all
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This unionAll(final String... propOrColumnNames)`
- **Summary:** Starts a new SELECT query for UNION ALL operation.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns for the union all query
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This unionAll(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for UNION ALL operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the union all query
- **Returns:** this SQLBuilder instance for method chaining
##### intersect(...) -> This
- **Signature:** `public This intersect(final This sqlBuilder)`
- **Summary:** Adds an INTERSECT clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to intersect
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This intersect(final String query)`
- **Summary:** Adds an INTERSECT clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the SQL query to intersect
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This intersect(final String... propOrColumnNames)`
- **Summary:** Starts a new SELECT query for INTERSECT operation.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns for the intersect query
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This intersect(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for INTERSECT operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the intersect query
- **Returns:** this SQLBuilder instance for method chaining
##### except(...) -> This
- **Signature:** `public This except(final This sqlBuilder)`
- **Summary:** Adds an EXCEPT clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to except
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This except(final String query)`
- **Summary:** Adds an EXCEPT clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the SQL query to except
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This except(final String... propOrColumnNames)`
- **Summary:** Starts a new SELECT query for EXCEPT operation.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns for the except query
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This except(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for EXCEPT operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the except query
- **Returns:** this SQLBuilder instance for method chaining
##### minus(...) -> This
- **Signature:** `public This minus(final This sqlBuilder)`
- **Summary:** Adds a MINUS clause with another SQL query (Oracle syntax).
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to minus
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This minus(final String query)`
- **Summary:** Adds a MINUS clause with a SQL query string (Oracle syntax).
- **Parameters:**
  - `query` (`String`) — the SQL query to minus
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This minus(final String... propOrColumnNames)`
- **Summary:** Starts a new SELECT query for MINUS operation (Oracle syntax).
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns for the minus query
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This minus(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for MINUS operation with a collection of columns (Oracle syntax).
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the minus query
- **Returns:** this SQLBuilder instance for method chaining
##### forUpdate(...) -> This
- **Signature:** `public This forUpdate()`
- **Summary:** Adds a FOR UPDATE clause to lock selected rows.
- **Parameters:**
  - (none)
- **Returns:** this SQLBuilder instance for method chaining
##### set(...) -> This
- **Signature:** `public This set(final String expr)`
- **Summary:** Sets columns for UPDATE operation with a single expression.
- **Parameters:**
  - `expr` (`String`) — the SET expression
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This set(final String... propOrColumnNames)`
- **Summary:** Sets columns for UPDATE operation.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to update
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This set(final Collection<String> propOrColumnNames)`
- **Summary:** Sets columns for UPDATE operation with a collection of property or column names.
- **Contract:**
  - If a column name already contains an {@code =} sign, it is treated as a raw SET expression and no placeholder is appended.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to update
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This set(final Map<String, Object> props)`
- **Summary:** Sets columns and values for UPDATE operation using a map.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of column names to values
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This set(final Object entity)`
- **Summary:** Sets properties to update from an entity object.
- **Contract:**
  - Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing properties to set
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This set(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Sets properties to update from an entity object, excluding specified properties.
- **Contract:**
  - Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing properties to set
  - `excludedPropNames` (`Set<String>`) — properties to exclude from the update
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This set(final Class<?> entityClass)`
- **Summary:** Sets all updatable properties from an entity class for UPDATE operation.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to get properties from
- **Returns:** this SQLBuilder instance for method chaining
- **Signature:** `public This set(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Sets updatable properties from an entity class for UPDATE operation, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to get properties from
  - `excludedPropNames` (`Set<String>`) — additional properties to exclude from the update
- **Returns:** this SQLBuilder instance for method chaining
##### query(...) -> String
- **Signature:** `public String query()`
- **Summary:** Generates the final SQL query string and releases resources.
- **Contract:**
  - This method should be called only once.
- **Parameters:**
  - (none)
- **Returns:** the generated SQL query string
##### parameters(...) -> List<Object>
- **Signature:** `public List<Object> parameters()`
- **Summary:** Returns the list of parameter values for the generated SQL.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values
##### build(...) -> SP
- **Signature:** `public SP build()`
- **Summary:** Generates both the SQL string and its parameters as a pair.
- **Parameters:**
  - (none)
- **Returns:** an SP (SQL-Parameters) pair containing the SQL string and parameter list
##### apply(...) -> T
- **Signature:** `@Beta public <T, E extends Exception> T apply(final Throwables.Function<? super SP, T, E> func) throws E`
- **Summary:** Applies a function to the SQL-Parameters pair and returns the result.
- **Parameters:**
  - `func` (`Throwables.Function<? super SP, T, E>`) — the function to apply to the SP pair
- **Returns:** the result of applying the function
- **Throws:**
  - `E` — if the function throws an exception
- **Signature:** `@Beta public <T, E extends Exception> T apply(final Throwables.BiFunction<? super String, ? super List<Object>, T, E> func) throws E`
- **Summary:** Applies a bi-function to the SQL string and parameters separately and returns the result.
- **Parameters:**
  - `func` (`Throwables.BiFunction<? super String, ? super List<Object>, T, E>`) — the bi-function to apply to the SQL and parameters
- **Returns:** the result of applying the function
- **Throws:**
  - `E` — if the function throws an exception
##### accept(...) -> void
- **Signature:** `@Beta public <E extends Exception> void accept(final Throwables.Consumer<? super SP, E> consumer) throws E`
- **Summary:** Accepts a consumer for the SQL-Parameters pair.
- **Contract:**
  - This is useful for executing the SQL with a data access framework when no return value is needed.
- **Parameters:**
  - `consumer` (`Throwables.Consumer<? super SP, E>`) — the consumer to accept the SP pair
- **Throws:**
  - `E` — if the consumer throws an exception
- **Signature:** `@Beta public <E extends Exception> void accept(final Throwables.BiConsumer<? super String, ? super List<Object>, E> consumer) throws E`
- **Summary:** Accepts a bi-consumer for the SQL string and parameters separately.
- **Contract:**
  - This is useful for executing the SQL with a data access framework when no return value is needed.
- **Parameters:**
  - `consumer` (`Throwables.BiConsumer<? super String, ? super List<Object>, E>`) — the bi-consumer to accept the SQL and parameters
- **Throws:**
  - `E` — if the consumer throws an exception
##### println(...) -> void
- **Signature:** `public void println()`
- **Summary:** Prints the generated SQL to standard output.
- **Parameters:**
  - (none)
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns the generated SQL string representation of this builder.
- **Parameters:**
  - (none)
- **Returns:** the generated SQL string

### Class SP (com.landawn.abacus.query.AbstractQueryBuilder.SP)
Represents a SQL string and its associated parameters.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this SP object.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this object
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Indicates whether some other object is "equal to" this one.
- **Contract:**
  - Two SP objects are equal if they have the same SQL string and parameters.
- **Parameters:**
  - `obj` (`Object`) — the reference object with which to compare
- **Returns:** {@code true} if this object is the same as the obj argument; false otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this SP object.
- **Parameters:**
  - (none)
- **Returns:** a string representation of the object

### Class DynamicSQLBuilder (com.landawn.abacus.query.DynamicSQLBuilder)
A fluent builder for creating dynamic SQL queries programmatically.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### create(...) -> DynamicSQLBuilder
- **Signature:** `public static DynamicSQLBuilder create()`
- **Summary:** Creates a new instance of DynamicSQLBuilder.
- **Parameters:**
  - (none)
- **Returns:** a new DynamicSQLBuilder instance for method chaining

#### Public Instance Methods
##### select(...) -> Select
- **Signature:** `public Select select()`
- **Summary:** Returns the SELECT clause builder for defining columns to retrieve.
- **Parameters:**
  - (none)
- **Returns:** the Select clause builder for method chaining
##### from(...) -> From
- **Signature:** `public From from()`
- **Summary:** Returns the FROM clause builder for defining tables and joins.
- **Parameters:**
  - (none)
- **Returns:** the From clause builder for method chaining
##### where(...) -> Where
- **Signature:** `public Where where()`
- **Summary:** Returns the WHERE clause builder for defining query conditions.
- **Parameters:**
  - (none)
- **Returns:** the Where clause builder for method chaining
##### groupBy(...) -> GroupBy
- **Signature:** `public GroupBy groupBy()`
- **Summary:** Returns the GROUP BY clause builder for defining grouping columns.
- **Parameters:**
  - (none)
- **Returns:** the GroupBy clause builder for method chaining
##### having(...) -> Having
- **Signature:** `public Having having()`
- **Summary:** Returns the HAVING clause builder for defining conditions on grouped results.
- **Parameters:**
  - (none)
- **Returns:** the Having clause builder for method chaining
##### orderBy(...) -> OrderBy
- **Signature:** `public OrderBy orderBy()`
- **Summary:** Returns the ORDER BY clause builder for defining result ordering.
- **Parameters:**
  - (none)
- **Returns:** the OrderBy clause builder for method chaining
##### limit(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder limit(final String limitCond)`
- **Summary:** Appends a custom LIMIT clause to the SQL query.
- **Parameters:**
  - `limitCond` (`String`) — the complete limit condition including the LIMIT keyword (must not be null)
- **Returns:** this builder instance for method chaining
- **Signature:** `public DynamicSQLBuilder limit(final int count)`
- **Summary:** Adds a LIMIT clause to restrict the number of rows returned.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must not be negative)
- **Returns:** this builder instance for method chaining
- **Signature:** `public DynamicSQLBuilder limit(final int offset, final int count)`
- **Summary:** Adds a LIMIT clause with offset for pagination.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip (must not be negative)
  - `count` (`int`) — the maximum number of rows to return (must not be negative)
- **Returns:** this builder instance for method chaining
- **See also:** #offsetRows(int), #fetchNextNRowsOnly(int), #fetchFirstNRowsOnly(int)
##### limitByRowNum(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder limitByRowNum(final int count)`
- **Summary:** Adds an Oracle-style ROWNUM condition to limit results.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must not be negative)
- **Returns:** this builder instance for method chaining
##### offsetRows(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder offsetRows(final int offset)`
- **Summary:** Adds an OFFSET clause for SQL:2008 standard pagination.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip (must not be negative)
- **Returns:** this builder instance for method chaining
##### fetchNextNRowsOnly(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder fetchNextNRowsOnly(final int n)`
- **Summary:** Adds a FETCH NEXT clause for SQL:2008 standard result limiting.
- **Parameters:**
  - `n` (`int`) — the number of rows to fetch (must not be negative)
- **Returns:** this builder instance for method chaining
##### fetchFirstNRowsOnly(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder fetchFirstNRowsOnly(final int n)`
- **Summary:** Adds a FETCH FIRST clause for SQL:2008 standard result limiting.
- **Parameters:**
  - `n` (`int`) — the number of rows to fetch (must not be negative)
- **Returns:** this builder instance for method chaining
##### union(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder union(final String query)`
- **Summary:** Adds a UNION operator to combine results with another query.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to union with (must not be null)
- **Returns:** this builder instance for method chaining
##### unionAll(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder unionAll(final String query)`
- **Summary:** Adds a UNION ALL operator to combine results with another query.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to union with (must not be null)
- **Returns:** this builder instance for method chaining
##### intersect(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder intersect(final String query)`
- **Summary:** Adds an INTERSECT operator to find common rows between queries.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to intersect with (must not be null)
- **Returns:** this builder instance for method chaining
##### except(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder except(final String query)`
- **Summary:** Adds an EXCEPT operator to find rows in the first query but not in the second.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to exclude results from (must not be null)
- **Returns:** this builder instance for method chaining
##### minus(...) -> DynamicSQLBuilder
- **Signature:** `public DynamicSQLBuilder minus(final String query)`
- **Summary:** Adds a MINUS operator to find rows in the first query but not in the second.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to exclude results from (must not be null)
- **Returns:** this builder instance for method chaining
##### build(...) -> String
- **Signature:** `public String build()`
- **Summary:** Builds the final SQL string from all the components and releases resources.
- **Contract:**
  - This method MUST be called to get the SQL and clean up internal resources.
  - After calling build(), this builder instance should not be reused.
- **Parameters:**
  - (none)
- **Returns:** the complete SQL query string

### Class Select (com.landawn.abacus.query.DynamicSQLBuilder.Select)
Builder class for constructing the SELECT clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> Select
- **Signature:** `public Select append(final String column)`
- **Summary:** Appends a single column to the SELECT clause.
- **Parameters:**
  - `column` (`String`) — the column name to select (must not be null)
- **Returns:** this Select instance for method chaining
- **Signature:** `public Select append(final String column, final String alias)`
- **Summary:** Appends a column with an alias to the SELECT clause.
- **Parameters:**
  - `column` (`String`) — the column name to select (must not be null)
  - `alias` (`String`) — the alias for the column (must not be null)
- **Returns:** this Select instance for method chaining
- **Signature:** `public Select append(final Collection<String> columns)`
- **Summary:** Appends multiple columns to the SELECT clause.
- **Parameters:**
  - `columns` (`Collection<String>`) — collection of column names to select (must not be null)
- **Returns:** this Select instance for method chaining
- **Signature:** `public Select append(final Map<String, String> columnsAndAliasMap)`
- **Summary:** Appends multiple columns with their aliases to the SELECT clause.
- **Parameters:**
  - `columnsAndAliasMap` (`Map<String, String>`) — map where keys are column names and values are aliases (must not be null)
- **Returns:** this Select instance for method chaining
##### appendIf(...) -> Select
- **Signature:** `public Select appendIf(final boolean b, final String str)`
- **Summary:** Conditionally appends a string to the SELECT clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `str` (`String`) — the string to append if condition is true
- **Returns:** this Select instance for method chaining
##### appendIfOrElse(...) -> Select
- **Signature:** `public Select appendIfOrElse(final boolean b, final String strToAppendForTrue, final String strToAppendForFalse)`
- **Summary:** Appends one of two strings to the SELECT clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `strToAppendForTrue` (`String`) — the string to append if condition is true
  - `strToAppendForFalse` (`String`) — the string to append if condition is false
- **Returns:** this Select instance for method chaining

### Class From (com.landawn.abacus.query.DynamicSQLBuilder.From)
Builder class for constructing the FROM clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> From
- **Signature:** `public From append(final String table)`
- **Summary:** Appends a table to the FROM clause.
- **Parameters:**
  - `table` (`String`) — the table name to add (must not be null)
- **Returns:** this From instance for method chaining
- **Signature:** `public From append(final String table, final String alias)`
- **Summary:** Appends a table with an alias to the FROM clause.
- **Parameters:**
  - `table` (`String`) — the table name to add (must not be null)
  - `alias` (`String`) — the alias for the table (must not be null)
- **Returns:** this From instance for method chaining
##### join(...) -> From
- **Signature:** `public From join(final String table, final String on)`
- **Summary:** Adds a JOIN clause (implicit INNER JOIN) with the specified table and join condition.
- **Parameters:**
  - `table` (`String`) — the table to join (can include alias; must not be null)
  - `on` (`String`) — the join condition (must not be null)
- **Returns:** this From instance for method chaining
##### innerJoin(...) -> From
- **Signature:** `public From innerJoin(final String table, final String on)`
- **Summary:** Adds an INNER JOIN clause with the specified table and join condition.
- **Parameters:**
  - `table` (`String`) — the table to join (can include alias; must not be null)
  - `on` (`String`) — the join condition (must not be null)
- **Returns:** this From instance for method chaining
##### leftJoin(...) -> From
- **Signature:** `public From leftJoin(final String table, final String on)`
- **Summary:** Adds a LEFT JOIN clause with the specified table and join condition.
- **Parameters:**
  - `table` (`String`) — the table to join (can include alias; must not be null)
  - `on` (`String`) — the join condition (must not be null)
- **Returns:** this From instance for method chaining
##### rightJoin(...) -> From
- **Signature:** `public From rightJoin(final String table, final String on)`
- **Summary:** Adds a RIGHT JOIN clause with the specified table and join condition.
- **Parameters:**
  - `table` (`String`) — the table to join (can include alias; must not be null)
  - `on` (`String`) — the join condition (must not be null)
- **Returns:** this From instance for method chaining
##### fullJoin(...) -> From
- **Signature:** `public From fullJoin(final String table, final String on)`
- **Summary:** Adds a FULL JOIN clause with the specified table and join condition.
- **Contract:**
  - Returns all rows when there is a match in either table.
- **Parameters:**
  - `table` (`String`) — the table to join (can include alias; must not be null)
  - `on` (`String`) — the join condition (must not be null)
- **Returns:** this From instance for method chaining
##### appendIf(...) -> From
- **Signature:** `public From appendIf(final boolean b, final String str)`
- **Summary:** Conditionally appends a string to the FROM clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `str` (`String`) — the string to append if condition is true
- **Returns:** this From instance for method chaining
##### appendIfOrElse(...) -> From
- **Signature:** `public From appendIfOrElse(final boolean b, final String strToAppendForTrue, final String strToAppendForFalse)`
- **Summary:** Appends one of two strings to the FROM clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `strToAppendForTrue` (`String`) — the string to append if condition is true
  - `strToAppendForFalse` (`String`) — the string to append if condition is false
- **Returns:** this From instance for method chaining

### Class Where (com.landawn.abacus.query.DynamicSQLBuilder.Where)
Builder class for constructing the WHERE clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> Where
- **Signature:** `public Where append(final String cond)`
- **Summary:** Appends a condition to the WHERE clause.
- **Parameters:**
  - `cond` (`String`) — the condition to append (must not be null)
- **Returns:** this Where instance for method chaining
##### repeatQM(...) -> Where
- **Signature:** `public Where repeatQM(final int n)`
- **Summary:** Appends question mark placeholders for parameterized queries.
- **Parameters:**
  - `n` (`int`) — the number of question marks to append
- **Returns:** this Where instance for method chaining
- **Signature:** `public Where repeatQM(final int n, final String prefix, final String postfix)`
- **Summary:** Appends question mark placeholders surrounded by prefix and postfix.
- **Parameters:**
  - `n` (`int`) — the number of question marks to append
  - `prefix` (`String`) — the string to add before the question marks
  - `postfix` (`String`) — the string to add after the question marks
- **Returns:** this Where instance for method chaining
##### and(...) -> Where
- **Signature:** `public Where and(final String cond)`
- **Summary:** Adds an AND condition to the WHERE clause.
- **Parameters:**
  - `cond` (`String`) — the condition to add with AND (must not be null)
- **Returns:** this Where instance for method chaining
##### or(...) -> Where
- **Signature:** `public Where or(final String cond)`
- **Summary:** Adds an OR condition to the WHERE clause.
- **Parameters:**
  - `cond` (`String`) — the condition to add with OR (must not be null)
- **Returns:** this Where instance for method chaining
##### appendIf(...) -> Where
- **Signature:** `public Where appendIf(final boolean b, final String str)`
- **Summary:** Conditionally appends a string to the WHERE clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `str` (`String`) — the string to append if condition is true
- **Returns:** this Where instance for method chaining
##### appendIfOrElse(...) -> Where
- **Signature:** `public Where appendIfOrElse(final boolean b, final String strToAppendForTrue, final String strToAppendForFalse)`
- **Summary:** Appends one of two strings to the WHERE clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `strToAppendForTrue` (`String`) — the string to append if condition is true
  - `strToAppendForFalse` (`String`) — the string to append if condition is false
- **Returns:** this Where instance for method chaining

### Class GroupBy (com.landawn.abacus.query.DynamicSQLBuilder.GroupBy)
Builder class for constructing the GROUP BY clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> GroupBy
- **Signature:** `public GroupBy append(final String column)`
- **Summary:** Appends a column to the GROUP BY clause.
- **Parameters:**
  - `column` (`String`) — the column name to group by (must not be null)
- **Returns:** this GroupBy instance for method chaining
- **Signature:** `public GroupBy append(final Collection<String> columns)`
- **Summary:** Appends multiple columns to the GROUP BY clause.
- **Parameters:**
  - `columns` (`Collection<String>`) — collection of column names to group by (must not be null)
- **Returns:** this GroupBy instance for method chaining
##### appendIf(...) -> GroupBy
- **Signature:** `public GroupBy appendIf(final boolean b, final String str)`
- **Summary:** Conditionally appends a string to the GROUP BY clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `str` (`String`) — the string to append if condition is true
- **Returns:** this GroupBy instance for method chaining
##### appendIfOrElse(...) -> GroupBy
- **Signature:** `public GroupBy appendIfOrElse(final boolean b, final String strToAppendForTrue, final String strToAppendForFalse)`
- **Summary:** Appends one of two strings to the GROUP BY clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `strToAppendForTrue` (`String`) — the string to append if condition is true
  - `strToAppendForFalse` (`String`) — the string to append if condition is false
- **Returns:** this GroupBy instance for method chaining

### Class Having (com.landawn.abacus.query.DynamicSQLBuilder.Having)
Builder class for constructing the HAVING clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> Having
- **Signature:** `public Having append(final String cond)`
- **Summary:** Appends a condition to the HAVING clause.
- **Parameters:**
  - `cond` (`String`) — the condition to append (must not be null)
- **Returns:** this Having instance for method chaining
##### and(...) -> Having
- **Signature:** `public Having and(final String cond)`
- **Summary:** Adds an AND condition to the HAVING clause.
- **Parameters:**
  - `cond` (`String`) — the condition to add with AND (must not be null)
- **Returns:** this Having instance for method chaining
##### or(...) -> Having
- **Signature:** `public Having or(final String cond)`
- **Summary:** Adds an OR condition to the HAVING clause.
- **Parameters:**
  - `cond` (`String`) — the condition to add with OR (must not be null)
- **Returns:** this Having instance for method chaining
##### appendIf(...) -> Having
- **Signature:** `public Having appendIf(final boolean b, final String str)`
- **Summary:** Conditionally appends a string to the HAVING clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `str` (`String`) — the string to append if condition is true
- **Returns:** this Having instance for method chaining
##### appendIfOrElse(...) -> Having
- **Signature:** `public Having appendIfOrElse(final boolean b, final String strToAppendForTrue, final String strToAppendForFalse)`
- **Summary:** Appends one of two strings to the HAVING clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `strToAppendForTrue` (`String`) — the string to append if condition is true
  - `strToAppendForFalse` (`String`) — the string to append if condition is false
- **Returns:** this Having instance for method chaining

### Class OrderBy (com.landawn.abacus.query.DynamicSQLBuilder.OrderBy)
Builder class for constructing the ORDER BY clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> OrderBy
- **Signature:** `public OrderBy append(final String column)`
- **Summary:** Appends a column (with optional sort direction) to the ORDER BY clause.
- **Parameters:**
  - `column` (`String`) — the column name with optional ASC/DESC (must not be null)
- **Returns:** this OrderBy instance for method chaining
- **Signature:** `public OrderBy append(final Collection<String> columns)`
- **Summary:** Appends multiple columns to the ORDER BY clause.
- **Parameters:**
  - `columns` (`Collection<String>`) — collection of column names with optional sort directions (must not be null)
- **Returns:** this OrderBy instance for method chaining
##### appendIf(...) -> OrderBy
- **Signature:** `public OrderBy appendIf(final boolean b, final String str)`
- **Summary:** Conditionally appends a string to the ORDER BY clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `str` (`String`) — the string to append if condition is true
- **Returns:** this OrderBy instance for method chaining
##### appendIfOrElse(...) -> OrderBy
- **Signature:** `public OrderBy appendIfOrElse(final boolean b, final String strToAppendForTrue, final String strToAppendForFalse)`
- **Summary:** Appends one of two strings to the ORDER BY clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `strToAppendForTrue` (`String`) — the string to append if condition is true
  - `strToAppendForFalse` (`String`) — the string to append if condition is false
- **Returns:** this OrderBy instance for method chaining

### Class DSB (com.landawn.abacus.query.DynamicSQLBuilder.DSB)
A convenience subclass of DynamicSQLBuilder with a shorter name.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### create(...) -> DSB
- **Signature:** `public static DSB create()`
- **Summary:** Creates a new instance of DSB.
- **Parameters:**
  - (none)
- **Returns:** a new DSB instance for method chaining

#### Public Instance Methods
- (none)

### Class Filters (com.landawn.abacus.query.Filters)
A comprehensive, enterprise-grade factory class providing a complete suite of SQL condition builders for constructing type-safe, parameterized database queries with advanced logical operations, comparison operators, and complex join conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### alwaysTrue(...) -> Expression
- **Signature:** `public static Expression alwaysTrue()`
- **Summary:** Returns a condition that always evaluates to true.
- **Parameters:**
  - (none)
- **Returns:** an Expression that always evaluates to true (1 &lt; 2)
##### alwaysFalse(...) -> Expression
- **Signature:** `@Deprecated public static Expression alwaysFalse()`
- **Summary:** Returns a condition that always evaluates to false.
- **Parameters:**
  - (none)
- **Returns:** an Expression that always evaluates to false (1 &gt; 2)
##### not(...) -> Not
- **Signature:** `public static Not not(final Condition condition)`
- **Summary:** Creates a negation condition that represents the logical NOT of the provided condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition to negate
- **Returns:** a Not condition that wraps and negates the provided condition
- **See also:** Not, Condition
##### namedProperty(...) -> NamedProperty
- **Signature:** `public static NamedProperty namedProperty(final String propName)`
- **Summary:** Creates a NamedProperty instance representing a property/column name.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column
- **Returns:** a NamedProperty instance
##### expr(...) -> Expression
- **Signature:** `public static Expression expr(final String literal)`
- **Summary:** Creates an Expression from a string literal.
- **Parameters:**
  - `literal` (`String`) — the SQL expression as a string
- **Returns:** an Expression instance
##### binary(...) -> Binary
- **Signature:** `public static Binary binary(final String propName, final Operator operator, final Object propValue)`
- **Summary:** Creates a binary condition with the specified property name, operator, and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `operator` (`Operator`) — the binary operator to use
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a Binary condition
##### equal(...) -> Equal
- **Signature:** `public static Equal equal(final String propName, final Object propValue)`
- **Summary:** Creates an equality condition (=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare for equality
- **Returns:** an Equal condition
- **Signature:** `public static Equal equal(final String propName)`
- **Summary:** Creates a parameterized equality condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an Equal condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### eq(...) -> Equal
- **Signature:** `public static Equal eq(final String propName, final Object propValue)`
- **Summary:** Creates an equality condition (=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare for equality
- **Returns:** an Equal condition
- **Signature:** `public static Equal eq(final String propName)`
- **Summary:** Creates a parameterized equality condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an Equal condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### eqOr(...) -> Or
- **Signature:** `@Deprecated public static Or eqOr(final String propName, final Collection<?> propValues)`
- **Summary:** Creates an OR condition where the property equals any of the values in the collection.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValues` (`Collection<?>`) — the collection of values to compare against (must not be empty)
- **Returns:** an Or condition
- **Signature:** `public static Or eqOr(final Map<String, ?> props)`
- **Summary:** Creates an OR condition from a map where each entry represents a property-value equality check.
- **Parameters:**
  - `props` (`Map<String, ?>`) — map of property names to values (must not be empty)
- **Returns:** an Or condition
- **Signature:** `@SuppressWarnings("deprecation") public static Or eqOr(final Object entity)`
- **Summary:** Creates an OR condition from an entity object using all its properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object whose properties will be used
- **Returns:** an Or condition
- **Signature:** `public static Or eqOr(final Object entity, final Collection<String> selectPropNames)`
- **Summary:** Creates an OR condition from an entity object using only the specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object
  - `selectPropNames` (`Collection<String>`) — the property names to include (must not be empty)
- **Returns:** an Or condition
- **Signature:** `public static Or eqOr(final String propName1, final Object propValue1, final String propName2, final Object propValue2)`
- **Summary:** Creates an OR condition with two property-value pairs.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
- **Returns:** an Or condition
- **Signature:** `public static Or eqOr(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3, final Object propValue3)`
- **Summary:** Creates an OR condition with three property-value pairs.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
  - `propName3` (`String`) — third property name
  - `propValue3` (`Object`) — third property value
- **Returns:** an Or condition
##### eqAnd(...) -> And
- **Signature:** `public static And eqAnd(final Map<String, ?> props)`
- **Summary:** Creates an AND condition from a map where each entry represents a property-value equality check.
- **Parameters:**
  - `props` (`Map<String, ?>`) — map of property names to values (must not be empty)
- **Returns:** an And condition
- **Signature:** `@SuppressWarnings("deprecation") public static And eqAnd(final Object entity)`
- **Summary:** Creates an AND condition from an entity object using all its properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object whose properties will be used
- **Returns:** an And condition
- **Signature:** `public static And eqAnd(final Object entity, final Collection<String> selectPropNames)`
- **Summary:** Creates an AND condition from an entity object using only the specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object
  - `selectPropNames` (`Collection<String>`) — the property names to include (must not be empty)
- **Returns:** an And condition
- **Signature:** `public static And eqAnd(final String propName1, final Object propValue1, final String propName2, final Object propValue2)`
- **Summary:** Creates an AND condition with two property-value pairs.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
- **Returns:** an And condition
- **Signature:** `public static And eqAnd(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3, final Object propValue3)`
- **Summary:** Creates an AND condition with three property-value pairs.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
  - `propName3` (`String`) — third property name
  - `propValue3` (`Object`) — third property value
- **Returns:** an And condition
##### eqAndOr(...) -> Or
- **Signature:** `@Beta public static Or eqAndOr(final List<? extends Map<String, ?>> propsList)`
- **Summary:** Creates an OR condition where each element in the list represents an AND condition of property-value pairs.
- **Parameters:**
  - `propsList` (`List<? extends Map<String, ?>>`) — list of property maps (must not be empty)
- **Returns:** an Or condition
- **Signature:** `@SuppressWarnings("deprecation") @Beta public static Or eqAndOr(final Collection<?> entities)`
- **Summary:** Creates an OR condition from a collection of entities, where each entity forms an AND condition.
- **Parameters:**
  - `entities` (`Collection<?>`) — collection of entity objects (must not be empty)
- **Returns:** an Or condition
- **Signature:** `@Beta public static Or eqAndOr(final Collection<?> entities, final Collection<String> selectPropNames)`
- **Summary:** Creates an OR condition from a collection of entities using only specified properties.
- **Parameters:**
  - `entities` (`Collection<?>`) — collection of entity objects (must not be empty)
  - `selectPropNames` (`Collection<String>`) — the property names to include (must not be empty)
- **Returns:** an Or condition
##### gtAndLt(...) -> And
- **Signature:** `public static And gtAndLt(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN-like condition using greater than and less than comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (exclusive)
  - `maxValue` (`Object`) — the maximum value (exclusive)
- **Returns:** an And condition
- **Signature:** `public static And gtAndLt(final String propName)`
- **Summary:** Creates a parameterized BETWEEN-like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition with parameter placeholders
##### geAndLt(...) -> And
- **Signature:** `public static And geAndLt(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN-like condition using greater than or equal and less than comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (exclusive)
- **Returns:** an And condition
- **Signature:** `public static And geAndLt(final String propName)`
- **Summary:** Creates a parameterized BETWEEN-like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition with parameter placeholders
##### geAndLe(...) -> And
- **Signature:** `public static And geAndLe(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN-like condition using greater than or equal and less than or equal comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** an And condition
- **Signature:** `public static And geAndLe(final String propName)`
- **Summary:** Creates a parameterized BETWEEN-like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition with parameter placeholders
##### gtAndLe(...) -> And
- **Signature:** `public static And gtAndLe(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN-like condition using greater than and less than or equal comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (exclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** an And condition
- **Signature:** `public static And gtAndLe(final String propName)`
- **Summary:** Creates a parameterized BETWEEN-like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an And condition with parameter placeholders
##### id2Cond(...) -> And
- **Signature:** `public static And id2Cond(final EntityId entityId)`
- **Summary:** Converts an EntityId to an AND condition where each key-value pair becomes an equality check.
- **Parameters:**
  - `entityId` (`EntityId`) — the EntityId containing key-value pairs (must not be null)
- **Returns:** an And condition
- **Signature:** `public static Or id2Cond(final Collection<? extends EntityId> entityIds)`
- **Summary:** Converts a collection of EntityIds to an OR condition where each EntityId becomes an AND condition.
- **Parameters:**
  - `entityIds` (`Collection<? extends EntityId>`) — collection of EntityIds (must not be empty)
- **Returns:** an Or condition
##### notEqual(...) -> NotEqual
- **Signature:** `public static NotEqual notEqual(final String propName, final Object propValue)`
- **Summary:** Creates a not-equal condition (!=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare for inequality
- **Returns:** a NotEqual condition
- **Signature:** `public static NotEqual notEqual(final String propName)`
- **Summary:** Creates a parameterized not-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a NotEqual condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### ne(...) -> NotEqual
- **Signature:** `public static NotEqual ne(final String propName, final Object propValue)`
- **Summary:** Creates a not-equal condition (!=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare for inequality
- **Returns:** a NotEqual condition
- **Signature:** `public static NotEqual ne(final String propName)`
- **Summary:** Creates a parameterized not-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a NotEqual condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### greaterThan(...) -> GreaterThan
- **Signature:** `public static GreaterThan greaterThan(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than condition (>) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a GreaterThan condition
- **Signature:** `public static GreaterThan greaterThan(final String propName)`
- **Summary:** Creates a parameterized greater-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a GreaterThan condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### gt(...) -> GreaterThan
- **Signature:** `public static GreaterThan gt(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than condition (>) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a GreaterThan condition
- **Signature:** `public static GreaterThan gt(final String propName)`
- **Summary:** Creates a parameterized greater-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a GreaterThan condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### greaterEqual(...) -> GreaterEqual
- **Signature:** `public static GreaterEqual greaterEqual(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than-or-equal condition (>=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a GreaterEqual condition
- **Signature:** `public static GreaterEqual greaterEqual(final String propName)`
- **Summary:** Creates a parameterized greater-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a GreaterEqual condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### ge(...) -> GreaterEqual
- **Signature:** `public static GreaterEqual ge(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than-or-equal condition (>=) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a GreaterEqual condition
- **Signature:** `public static GreaterEqual ge(final String propName)`
- **Summary:** Creates a parameterized greater-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a GreaterEqual condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### lessThan(...) -> LessThan
- **Signature:** `public static LessThan lessThan(final String propName, final Object propValue)`
- **Summary:** Creates a less-than condition ( &lt; ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a LessThan condition
- **Signature:** `public static LessThan lessThan(final String propName)`
- **Summary:** Creates a parameterized less-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a LessThan condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### lt(...) -> LessThan
- **Signature:** `public static LessThan lt(final String propName, final Object propValue)`
- **Summary:** Creates a less-than condition ( &lt; ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a LessThan condition
- **Signature:** `public static LessThan lt(final String propName)`
- **Summary:** Creates a parameterized less-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a LessThan condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### lessEqual(...) -> LessEqual
- **Signature:** `public static LessEqual lessEqual(final String propName, final Object propValue)`
- **Summary:** Creates a less-than-or-equal condition ( &lt; =) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a LessEqual condition
- **Signature:** `public static LessEqual lessEqual(final String propName)`
- **Summary:** Creates a parameterized less-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a LessEqual condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### le(...) -> LessEqual
- **Signature:** `public static LessEqual le(final String propName, final Object propValue)`
- **Summary:** Creates a less-than-or-equal condition ( &lt; =) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a LessEqual condition
- **Signature:** `public static LessEqual le(final String propName)`
- **Summary:** Creates a parameterized less-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a LessEqual condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SQLBuilder
##### between(...) -> Between
- **Signature:** `public static Between between(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN condition for the specified property and range values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** a Between condition
- **Signature:** `public static Between between(final String propName)`
- **Summary:** Creates a parameterized BETWEEN condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a Between condition with parameter placeholders
##### bt(...) -> Between
- **Signature:** `@Deprecated public static Between bt(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN condition for the specified property and range values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** a Between condition
- **Signature:** `@Deprecated public static Between bt(final String propName)`
- **Summary:** Creates a parameterized BETWEEN condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a Between condition with parameter placeholders
##### notBetween(...) -> NotBetween
- **Signature:** `public static NotBetween notBetween(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a NOT BETWEEN condition for the specified property and range values.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code NotBetween condition = Filters.notBetween("temperature", -10, 40); // SQL fragment: temperature NOT BETWEEN -10 AND 40 // True when temperature < -10 OR temperature > 40 } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value of the excluded range (inclusive)
  - `maxValue` (`Object`) — the maximum value of the excluded range (inclusive)
- **Returns:** a NotBetween condition
- **Signature:** `public static NotBetween notBetween(final String propName)`
- **Summary:** Creates a parameterized NOT BETWEEN condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a NotBetween condition with parameter placeholders
##### like(...) -> Like
- **Signature:** `public static Like like(final String propName, final Object propValue)`
- **Summary:** Creates a LIKE condition for pattern matching.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the pattern to match (can include SQL wildcards)
- **Returns:** a Like condition
- **Signature:** `public static Like like(final String propName)`
- **Summary:** Creates a parameterized LIKE condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a Like condition with a parameter placeholder
##### notLike(...) -> NotLike
- **Signature:** `public static NotLike notLike(final String propName, final Object propValue)`
- **Summary:** Creates a NOT LIKE condition for pattern matching exclusion.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the pattern to exclude (can include SQL wildcards)
- **Returns:** a NotLike condition
- **Signature:** `public static NotLike notLike(final String propName)`
- **Summary:** Creates a parameterized NOT LIKE condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a NotLike condition with a parameter placeholder
##### contains(...) -> Like
- **Signature:** `public static Like contains(final String propName, final Object propValue)`
- **Summary:** Creates a LIKE condition that checks if the property contains the specified value.
- **Contract:**
  - Creates a LIKE condition that checks if the property contains the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to search for
- **Returns:** a Like condition
##### notContains(...) -> NotLike
- **Signature:** `public static NotLike notContains(final String propName, final Object propValue)`
- **Summary:** Creates a NOT LIKE condition that checks if the property does not contain the specified value.
- **Contract:**
  - Creates a NOT LIKE condition that checks if the property does not contain the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to exclude
- **Returns:** a NotLike condition
##### startsWith(...) -> Like
- **Signature:** `public static Like startsWith(final String propName, final Object propValue)`
- **Summary:** Creates a LIKE condition that checks if the property starts with the specified value.
- **Contract:**
  - Creates a LIKE condition that checks if the property starts with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the prefix to search for
- **Returns:** a Like condition
##### notStartsWith(...) -> NotLike
- **Signature:** `public static NotLike notStartsWith(final String propName, final Object propValue)`
- **Summary:** Creates a NOT LIKE condition that checks if the property does not start with the specified value.
- **Contract:**
  - Creates a NOT LIKE condition that checks if the property does not start with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the prefix to exclude
- **Returns:** a NotLike condition
##### endsWith(...) -> Like
- **Signature:** `public static Like endsWith(final String propName, final Object propValue)`
- **Summary:** Creates a LIKE condition that checks if the property ends with the specified value.
- **Contract:**
  - Creates a LIKE condition that checks if the property ends with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the suffix to search for
- **Returns:** a Like condition
##### notEndsWith(...) -> NotLike
- **Signature:** `public static NotLike notEndsWith(final String propName, final Object propValue)`
- **Summary:** Creates a NOT LIKE condition that checks if the property does not end with the specified value.
- **Contract:**
  - Creates a NOT LIKE condition that checks if the property does not end with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the suffix to exclude
- **Returns:** a NotLike condition
##### isNull(...) -> IsNull
- **Signature:** `public static IsNull isNull(final String propName)`
- **Summary:** Creates an IS NULL condition to check if a property value is null.
- **Contract:**
  - Creates an IS NULL condition to check if a property value is null.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNull condition
##### isEmpty(...) -> Or
- **Signature:** `@Beta public static Or isEmpty(final String propName)`
- **Summary:** Creates a condition to check if a property is null or empty string.
- **Contract:**
  - Creates a condition to check if a property is null or empty string.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an Or condition combining null and empty checks
##### isNullOrZero(...) -> Or
- **Signature:** `@Beta public static Or isNullOrZero(final String propName)`
- **Summary:** Creates a condition to check if a property is null or zero.
- **Contract:**
  - Creates a condition to check if a property is null or zero.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an Or condition combining null and zero checks
##### isNotNull(...) -> IsNotNull
- **Signature:** `public static IsNotNull isNotNull(final String propName)`
- **Summary:** Creates an IS NOT NULL condition to check if a property value is not null.
- **Contract:**
  - Creates an IS NOT NULL condition to check if a property value is not null.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNotNull condition
##### isNaN(...) -> IsNaN
- **Signature:** `public static IsNaN isNaN(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is NaN (Not a Number).
- **Contract:**
  - Creates a condition to check if a numeric property value is NaN (Not a Number).
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNaN condition
##### isNotNaN(...) -> IsNotNaN
- **Signature:** `public static IsNotNaN isNotNaN(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is not NaN.
- **Contract:**
  - Creates a condition to check if a numeric property value is not NaN.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNotNaN condition
##### isInfinite(...) -> IsInfinite
- **Signature:** `public static IsInfinite isInfinite(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is infinite.
- **Contract:**
  - Creates a condition to check if a numeric property value is infinite.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsInfinite condition
##### isNotInfinite(...) -> IsNotInfinite
- **Signature:** `public static IsNotInfinite isNotInfinite(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is not infinite.
- **Contract:**
  - Creates a condition to check if a numeric property value is not infinite.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an IsNotInfinite condition
##### is(...) -> Is
- **Signature:** `public static Is is(final String propName, final Object propValue)`
- **Summary:** Creates an IS condition for database-specific identity comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare
- **Returns:** an Is condition
##### isNot(...) -> IsNot
- **Signature:** `public static IsNot isNot(final String propName, final Object propValue)`
- **Summary:** Creates an IS NOT condition for database-specific identity comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare
- **Returns:** an IsNot condition
##### xor(...) -> XOR
- **Signature:** `public static XOR xor(final String propName, final Object propValue)`
- **Summary:** Creates an XOR (exclusive OR) condition for the specified property and value.
- **Contract:**
  - The condition is true when exactly one of the operands is true.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to XOR with
- **Returns:** an XOR condition
##### or(...) -> Or
- **Signature:** `public static Or or(final Condition... conditions)`
- **Summary:** Creates an OR junction combining multiple conditions.
- **Contract:**
  - At least one condition must be true for the OR to be true.
- **Parameters:**
  - `conditions` (`Condition[]`) — the array of conditions to combine with OR
- **Returns:** an Or junction
- **Signature:** `public static Or or(final Collection<? extends Condition> conditions)`
- **Summary:** Creates an OR junction combining multiple conditions from a collection.
- **Contract:**
  - At least one condition must be true for the OR to be true.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with OR
- **Returns:** an Or junction
##### and(...) -> And
- **Signature:** `public static And and(final Condition... conditions)`
- **Summary:** Creates an AND junction combining multiple conditions.
- **Contract:**
  - All conditions must be true for the AND to be true.
- **Parameters:**
  - `conditions` (`Condition[]`) — the array of conditions to combine with AND
- **Returns:** an And junction
- **Signature:** `public static And and(final Collection<? extends Condition> conditions)`
- **Summary:** Creates an AND junction combining multiple conditions from a collection.
- **Contract:**
  - All conditions must be true for the AND to be true.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with AND
- **Returns:** an And junction
##### junction(...) -> Junction
- **Signature:** `@Beta public static Junction junction(final Operator operator, final Condition... conditions)`
- **Summary:** Creates a junction with a custom operator combining multiple conditions.
- **Parameters:**
  - `operator` (`Operator`) — the junction operator to use
  - `conditions` (`Condition[]`) — the array of conditions to combine
- **Returns:** a Junction with the specified operator
- **Signature:** `@Beta public static Junction junction(final Operator operator, final Collection<? extends Condition> conditions)`
- **Summary:** Creates a junction with a custom operator combining conditions from a collection.
- **Parameters:**
  - `operator` (`Operator`) — the junction operator to use
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine
- **Returns:** a Junction with the specified operator
##### where(...) -> Where
- **Signature:** `public static Where where(final Condition condition)`
- **Summary:** Creates a WHERE clause with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition for the WHERE clause
- **Returns:** a Where clause
- **Signature:** `public static Where where(final String condition)`
- **Summary:** Creates a WHERE clause from a string expression.
- **Parameters:**
  - `condition` (`String`) — the SQL expression as a string
- **Returns:** a Where clause
##### groupBy(...) -> GroupBy
- **Signature:** `public static GroupBy groupBy(final String... propNames)`
- **Summary:** Creates a GROUP BY clause with the specified property names.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to group by
- **Returns:** a GroupBy clause
- **Signature:** `public static GroupBy groupBy(final Collection<String> propNames)`
- **Summary:** Creates a GROUP BY clause with properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by
- **Returns:** a GroupBy clause
- **Signature:** `public static GroupBy groupBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates a GROUP BY clause with properties and specified sort direction.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** a GroupBy clause
- **Signature:** `public static GroupBy groupBy(final String propName, final SortDirection direction)`
- **Summary:** Creates a GROUP BY clause with a single property and sort direction.
- **Parameters:**
  - `propName` (`String`) — the property/column name to group by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** a GroupBy clause
- **Signature:** `public static GroupBy groupBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB)`
- **Summary:** Creates a GROUP BY clause with two properties and their respective sort directions.
- **Parameters:**
  - `propNameA` (`String`) — first property name
  - `directionA` (`SortDirection`) — first property sort direction
  - `propNameB` (`String`) — second property name
  - `directionB` (`SortDirection`) — second property sort direction
- **Returns:** a GroupBy clause
- **Signature:** `public static GroupBy groupBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB, final String propNameC, final SortDirection directionC)`
- **Summary:** Creates a GROUP BY clause with three properties and their respective sort directions.
- **Parameters:**
  - `propNameA` (`String`) — first property name
  - `directionA` (`SortDirection`) — first property sort direction
  - `propNameB` (`String`) — second property name
  - `directionB` (`SortDirection`) — second property sort direction
  - `propNameC` (`String`) — third property name
  - `directionC` (`SortDirection`) — third property sort direction
- **Returns:** a GroupBy clause
- **Signature:** `public static GroupBy groupBy(final Map<String, SortDirection> orders)`
- **Summary:** Creates a GROUP BY clause from a map of property names to sort directions.
- **Contract:**
  - The map should be a LinkedHashMap to preserve order.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of property names to sort directions (should be LinkedHashMap)
- **Returns:** a GroupBy clause
- **Signature:** `public static GroupBy groupBy(final Condition condition)`
- **Summary:** Creates a GROUP BY clause with a custom condition.
- **Parameters:**
  - `condition` (`Condition`) — the grouping condition
- **Returns:** a GroupBy clause
##### having(...) -> Having
- **Signature:** `public static Having having(final Condition condition)`
- **Summary:** Creates a HAVING clause with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition for the HAVING clause
- **Returns:** a Having clause
- **Signature:** `public static Having having(final String condition)`
- **Summary:** Creates a HAVING clause from a string expression.
- **Parameters:**
  - `condition` (`String`) — the SQL expression as a string
- **Returns:** a Having clause
##### orderBy(...) -> OrderBy
- **Signature:** `public static OrderBy orderBy(final String... propNames)`
- **Summary:** Creates an ORDER BY clause with the specified property names.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by
- **Returns:** an OrderBy clause
- **Signature:** `public static OrderBy orderBy(final Collection<String> propNames)`
- **Summary:** Creates an ORDER BY clause with properties from a collection in ascending order.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by
- **Returns:** an OrderBy clause
- **Signature:** `public static OrderBy orderBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates an ORDER BY clause with properties and specified sort direction.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** an OrderBy clause
- **Signature:** `public static OrderBy orderBy(final String propName, final SortDirection direction)`
- **Summary:** Creates an ORDER BY clause with a single property and sort direction.
- **Parameters:**
  - `propName` (`String`) — the property/column name to order by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** an OrderBy clause
- **Signature:** `public static OrderBy orderBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB)`
- **Summary:** Creates an ORDER BY clause with two properties and their respective sort directions.
- **Parameters:**
  - `propNameA` (`String`) — first property name
  - `directionA` (`SortDirection`) — first property sort direction
  - `propNameB` (`String`) — second property name
  - `directionB` (`SortDirection`) — second property sort direction
- **Returns:** an OrderBy clause
- **Signature:** `public static OrderBy orderBy(final String propNameA, final SortDirection directionA, final String propNameB, final SortDirection directionB, final String propNameC, final SortDirection directionC)`
- **Summary:** Creates an ORDER BY clause with three properties and their respective sort directions.
- **Parameters:**
  - `propNameA` (`String`) — first property name
  - `directionA` (`SortDirection`) — first property sort direction
  - `propNameB` (`String`) — second property name
  - `directionB` (`SortDirection`) — second property sort direction
  - `propNameC` (`String`) — third property name
  - `directionC` (`SortDirection`) — third property sort direction
- **Returns:** an OrderBy clause
- **Signature:** `public static OrderBy orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Creates an ORDER BY clause from a map of property names to sort directions.
- **Contract:**
  - The map should be a LinkedHashMap to preserve order.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of property names to sort directions (should be LinkedHashMap)
- **Returns:** an OrderBy clause
- **Signature:** `public static OrderBy orderBy(final Condition condition)`
- **Summary:** Creates an ORDER BY clause with a custom condition.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code OrderBy orderBy = Filters.orderBy( Filters.expr("CASE WHEN status = 'urgent' THEN 1 ELSE 2 END, created_date DESC") ); } </pre>
- **Parameters:**
  - `condition` (`Condition`) — the ordering condition
- **Returns:** an OrderBy clause
##### orderByAsc(...) -> OrderBy
- **Signature:** `public static OrderBy orderByAsc(final String... propNames)`
- **Summary:** Creates an ORDER BY clause with ascending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by ascending
- **Returns:** an OrderBy clause
- **Signature:** `public static OrderBy orderByAsc(final Collection<String> propNames)`
- **Summary:** Creates an ORDER BY clause with ascending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by ascending
- **Returns:** an OrderBy clause
##### orderByDesc(...) -> OrderBy
- **Signature:** `public static OrderBy orderByDesc(final String... propNames)`
- **Summary:** Creates an ORDER BY clause with descending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by descending
- **Returns:** an OrderBy clause
- **Signature:** `public static OrderBy orderByDesc(final Collection<String> propNames)`
- **Summary:** Creates an ORDER BY clause with descending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by descending
- **Returns:** an OrderBy clause
##### on(...) -> On
- **Signature:** `public static On on(final Condition condition)`
- **Summary:** Creates an ON clause for JOIN operations with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the join condition
- **Returns:** an On clause
- **Signature:** `public static On on(final String condition)`
- **Summary:** Creates an ON clause from a string expression for JOIN operations.
- **Parameters:**
  - `condition` (`String`) — the join condition as a string
- **Returns:** an On clause
- **Signature:** `public static On on(final String propName, final String anotherPropName)`
- **Summary:** Creates an ON clause for simple equality join between two columns.
- **Parameters:**
  - `propName` (`String`) — the first column name
  - `anotherPropName` (`String`) — the second column name to join with
- **Returns:** an On clause
- **Signature:** `public static On on(final Map<String, String> propNamePair)`
- **Summary:** Creates an ON clause from a map of column pairs for JOIN operations.
- **Parameters:**
  - `propNamePair` (`Map<String, String>`) — map of column name pairs for joining
- **Returns:** an On clause
##### using(...) -> Using
- **Signature:** `@Deprecated public static Using using(final String... columnNames)`
- **Summary:** Creates a USING clause for JOIN operations with the specified columns.
- **Contract:**
  - USING is an alternative to ON when joining tables on columns with the same name.
- **Parameters:**
  - `columnNames` (`String[]`) — the column names used for joining
- **Returns:** a Using clause
- **Signature:** `@Deprecated public static Using using(final Collection<String> columnNames)`
- **Summary:** Creates a USING clause from a collection of column names for JOIN operations.
- **Parameters:**
  - `columnNames` (`Collection<String>`) — collection of column names used for joining
- **Returns:** a Using clause
##### join(...) -> Join
- **Signature:** `public static Join join(final String joinEntity)`
- **Summary:** Creates a JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to join
- **Returns:** a Join clause
- **Signature:** `public static Join join(final String joinEntity, final Condition condition)`
- **Summary:** Creates a JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to join
  - `condition` (`Condition`) — the join condition
- **Returns:** a Join clause
- **Signature:** `public static Join join(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to join
  - `condition` (`Condition`) — the join condition
- **Returns:** a Join clause
##### leftJoin(...) -> LeftJoin
- **Signature:** `public static LeftJoin leftJoin(final String joinEntity)`
- **Summary:** Creates a LEFT JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to left join
- **Returns:** a LeftJoin clause
- **Signature:** `public static LeftJoin leftJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a LEFT JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to left join
  - `condition` (`Condition`) — the join condition
- **Returns:** a LeftJoin clause
- **Signature:** `public static LeftJoin leftJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a LEFT JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to left join
  - `condition` (`Condition`) — the join condition
- **Returns:** a LeftJoin clause
##### rightJoin(...) -> RightJoin
- **Signature:** `public static RightJoin rightJoin(final String joinEntity)`
- **Summary:** Creates a RIGHT JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to right join
- **Returns:** a RightJoin clause
- **Signature:** `public static RightJoin rightJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a RIGHT JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to right join
  - `condition` (`Condition`) — the join condition
- **Returns:** a RightJoin clause
- **Signature:** `public static RightJoin rightJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a RIGHT JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to right join
  - `condition` (`Condition`) — the join condition
- **Returns:** a RightJoin clause
##### crossJoin(...) -> CrossJoin
- **Signature:** `public static CrossJoin crossJoin(final String joinEntity)`
- **Summary:** Creates a CROSS JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to cross join
- **Returns:** a CrossJoin clause
- **Signature:** `public static CrossJoin crossJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a CROSS JOIN clause with the specified entity and optional condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to cross join
  - `condition` (`Condition`) — the optional join condition
- **Returns:** a CrossJoin clause
- **Signature:** `public static CrossJoin crossJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a CROSS JOIN clause with multiple entities and optional condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to cross join
  - `condition` (`Condition`) — the optional join condition
- **Returns:** a CrossJoin clause
##### fullJoin(...) -> FullJoin
- **Signature:** `public static FullJoin fullJoin(final String joinEntity)`
- **Summary:** Creates a FULL JOIN clause for the specified entity/table.
- **Contract:**
  - Returns all records when there is a match in either table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to full join
- **Returns:** a FullJoin clause
- **Signature:** `public static FullJoin fullJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a FULL JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to full join
  - `condition` (`Condition`) — the join condition
- **Returns:** a FullJoin clause
- **Signature:** `public static FullJoin fullJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a FULL JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to full join
  - `condition` (`Condition`) — the join condition
- **Returns:** a FullJoin clause
##### innerJoin(...) -> InnerJoin
- **Signature:** `public static InnerJoin innerJoin(final String joinEntity)`
- **Summary:** Creates an INNER JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to inner join
- **Returns:** an InnerJoin clause
- **Signature:** `public static InnerJoin innerJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates an INNER JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to inner join
  - `condition` (`Condition`) — the join condition
- **Returns:** an InnerJoin clause
- **Signature:** `public static InnerJoin innerJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates an INNER JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to inner join
  - `condition` (`Condition`) — the join condition
- **Returns:** an InnerJoin clause
##### naturalJoin(...) -> NaturalJoin
- **Signature:** `public static NaturalJoin naturalJoin(final String joinEntity)`
- **Summary:** Creates a NATURAL JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to natural join
- **Returns:** a NaturalJoin clause
- **Signature:** `public static NaturalJoin naturalJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a NATURAL JOIN clause with the specified entity and additional condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to natural join
  - `condition` (`Condition`) — the additional join condition
- **Returns:** a NaturalJoin clause
- **Signature:** `public static NaturalJoin naturalJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a NATURAL JOIN clause with multiple entities and additional condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to natural join
  - `condition` (`Condition`) — the additional join condition
- **Returns:** a NaturalJoin clause
##### in(...) -> In
- **Signature:** `public static In in(final String propName, final int[] values)`
- **Summary:** Creates an IN condition with an array of integer values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`int[]`) — array of integer values
- **Returns:** an In condition
- **Signature:** `public static In in(final String propName, final long[] values)`
- **Summary:** Creates an IN condition with an array of long values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`long[]`) — array of long values
- **Returns:** an In condition
- **Signature:** `public static In in(final String propName, final double[] values)`
- **Summary:** Creates an IN condition with an array of double values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`double[]`) — array of double values
- **Returns:** an In condition
- **Signature:** `public static In in(final String propName, final Object[] values)`
- **Summary:** Creates an IN condition with an array of object values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Object[]`) — array of values
- **Returns:** an In condition
- **Signature:** `public static In in(final String propName, final Collection<?> values)`
- **Summary:** Creates an IN condition with a collection of values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Collection<?>`) — collection of values
- **Returns:** an In condition
- **Signature:** `public static InSubQuery in(final String propName, final SubQuery subQuery)`
- **Summary:** Creates an IN condition with a subquery.
- **Contract:**
  - The property value must be in the result set of the subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** an InSubQuery condition
- **Signature:** `public static InSubQuery in(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates an IN condition with multiple properties and a subquery.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** an InSubQuery condition
##### notIn(...) -> NotIn
- **Signature:** `public static NotIn notIn(final String propName, final int[] values)`
- **Summary:** Creates a NOT IN condition with an array of integer values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`int[]`) — array of integer values to exclude
- **Returns:** a NotIn condition
- **Signature:** `public static NotIn notIn(final String propName, final long[] values)`
- **Summary:** Creates a NOT IN condition with an array of long values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`long[]`) — array of long values to exclude
- **Returns:** a NotIn condition
- **Signature:** `public static NotIn notIn(final String propName, final double[] values)`
- **Summary:** Creates a NOT IN condition with an array of double values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`double[]`) — array of double values to exclude
- **Returns:** a NotIn condition
- **Signature:** `public static NotIn notIn(final String propName, final Object[] values)`
- **Summary:** Creates a NOT IN condition with an array of object values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Object[]`) — array of values to exclude
- **Returns:** a NotIn condition
- **Signature:** `public static NotIn notIn(final String propName, final Collection<?> values)`
- **Summary:** Creates a NOT IN condition with a collection of values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Collection<?>`) — collection of values to exclude
- **Returns:** a NotIn condition
- **Signature:** `public static NotInSubQuery notIn(final String propName, final SubQuery subQuery)`
- **Summary:** Creates a NOT IN condition with a subquery.
- **Contract:**
  - The property value must not be in the result set of the subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** a NotInSubQuery condition
- **Signature:** `public static NotInSubQuery notIn(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates a NOT IN condition with multiple properties and a subquery.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** a NotInSubQuery condition
##### all(...) -> All
- **Signature:** `public static All all(final SubQuery condition)`
- **Summary:** Creates an ALL condition for comparison with all values from a subquery.
- **Contract:**
  - The condition is true if the comparison is true for all values returned by the subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery condition
- **Returns:** an All condition
##### any(...) -> Any
- **Signature:** `public static Any any(final SubQuery condition)`
- **Summary:** Creates an ANY condition for comparison with any value from a subquery.
- **Contract:**
  - The condition is true if the comparison is true for at least one value returned by the subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery condition
- **Returns:** an Any condition
##### some(...) -> Some
- **Signature:** `public static Some some(final SubQuery condition)`
- **Summary:** Creates a SOME condition for comparison with some values from a subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery condition
- **Returns:** a Some condition
##### exists(...) -> Exists
- **Signature:** `public static Exists exists(final SubQuery condition)`
- **Summary:** Creates an EXISTS condition to check if a subquery returns any rows.
- **Contract:**
  - Creates an EXISTS condition to check if a subquery returns any rows.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery to check
- **Returns:** an Exists condition
##### notExists(...) -> NotExists
- **Signature:** `public static NotExists notExists(final SubQuery condition)`
- **Summary:** Creates a NOT EXISTS condition to check if a subquery returns no rows.
- **Contract:**
  - Creates a NOT EXISTS condition to check if a subquery returns no rows.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery to check
- **Returns:** a NotExists condition
##### union(...) -> Union
- **Signature:** `public static Union union(final SubQuery condition)`
- **Summary:** Creates a UNION clause to combine results from a subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery to union with
- **Returns:** a Union clause
##### unionAll(...) -> UnionAll
- **Signature:** `public static UnionAll unionAll(final SubQuery condition)`
- **Summary:** Creates a UNION ALL clause to combine results from a subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery to union with
- **Returns:** a UnionAll clause
##### except(...) -> Except
- **Signature:** `public static Except except(final SubQuery condition)`
- **Summary:** Creates an EXCEPT clause to subtract results from a subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery to subtract
- **Returns:** an Except clause
##### intersect(...) -> Intersect
- **Signature:** `public static Intersect intersect(final SubQuery condition)`
- **Summary:** Creates an INTERSECT clause to find common results with a subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery to intersect with
- **Returns:** an Intersect clause
##### minus(...) -> Minus
- **Signature:** `public static Minus minus(final SubQuery condition)`
- **Summary:** Creates a MINUS clause to subtract results from a subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery to subtract
- **Returns:** a Minus clause
##### cell(...) -> Cell
- **Signature:** `@Beta public static Cell cell(final Operator operator, final Condition condition)`
- **Summary:** Creates a Cell condition with a custom operator and condition.
- **Parameters:**
  - `operator` (`Operator`) — the operator to apply
  - `condition` (`Condition`) — the condition to wrap
- **Returns:** a Cell condition
##### subQuery(...) -> SubQuery
- **Signature:** `public static SubQuery subQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a SubQuery from an entity class with selected properties and condition.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table
  - `propNames` (`Collection<String>`) — collection of property names to select
  - `condition` (`Condition`) — the WHERE condition for the subquery
- **Returns:** a SubQuery
- **Signature:** `public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a SubQuery from an entity name with selected properties and condition.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name
  - `propNames` (`Collection<String>`) — collection of property names to select
  - `condition` (`Condition`) — the WHERE condition for the subquery
- **Returns:** a SubQuery
- **Signature:** `public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final String condition)`
- **Summary:** Creates a SubQuery from an entity name with selected properties and string condition.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name
  - `propNames` (`Collection<String>`) — collection of property names to select
  - `condition` (`String`) — the WHERE condition as a string
- **Returns:** a SubQuery
- **Signature:** `@Deprecated public static SubQuery subQuery(final String entityName, final String sql)`
- **Summary:** Creates a SubQuery from an entity name and raw SQL.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name
  - `sql` (`String`) — the complete SQL for the subquery
- **Returns:** a SubQuery
- **See also:** #subQuery(String)
- **Signature:** `public static SubQuery subQuery(final String sql)`
- **Summary:** Creates a SubQuery from raw SQL.
- **Parameters:**
  - `sql` (`String`) — the complete SQL for the subquery
- **Returns:** a SubQuery
##### limit(...) -> Limit
- **Signature:** `public static Limit limit(final int count)`
- **Summary:** Creates a LIMIT clause to restrict the number of rows returned.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return
- **Returns:** a Limit clause
- **Signature:** `public static Limit limit(final int offset, final int count)`
- **Summary:** Creates a LIMIT clause with an offset and count.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip
  - `count` (`int`) — the maximum number of rows to return
- **Returns:** a Limit clause
- **Signature:** `public static Limit limit(final String expr)`
- **Summary:** Creates a LIMIT clause from a string expression.
- **Parameters:**
  - `expr` (`String`) — the limit expression as a string
- **Returns:** a Limit clause
##### criteria(...) -> Criteria
- **Signature:** `public static Criteria criteria()`
- **Summary:** Creates an empty Criteria object for building complex query conditions.
- **Parameters:**
  - (none)
- **Returns:** a new empty Criteria instance

#### Public Instance Methods
- (none)

### Class CF (com.landawn.abacus.query.Filters.CF)
A utility class providing static factory methods identical to Filters.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class CB (com.landawn.abacus.query.Filters.CB)
A utility class for building Criteria objects with a fluent interface.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### where(...) -> Criteria
- **Signature:** `public static Criteria where(final Condition condition)`
- **Summary:** Creates a new Criteria with a WHERE clause containing the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition for the WHERE clause
- **Returns:** a new Criteria with the WHERE condition
- **Signature:** `public static Criteria where(final String condition)`
- **Summary:** Creates a new Criteria with a WHERE clause from a string expression.
- **Parameters:**
  - `condition` (`String`) — the SQL expression as a string
- **Returns:** a new Criteria with the WHERE condition
##### groupBy(...) -> Criteria
- **Signature:** `public static Criteria groupBy(final Condition condition)`
- **Summary:** Creates a new Criteria with a GROUP BY clause containing the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the grouping condition
- **Returns:** a new Criteria with the GROUP BY condition
- **Signature:** `public static Criteria groupBy(final String... propNames)`
- **Summary:** Creates a new Criteria with a GROUP BY clause for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to group by
- **Returns:** a new Criteria with the GROUP BY clause
- **Signature:** `public static Criteria groupBy(final String propName, final SortDirection direction)`
- **Summary:** Creates a new Criteria with a GROUP BY clause for a single property with sort direction.
- **Parameters:**
  - `propName` (`String`) — the property/column name to group by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** a new Criteria with the GROUP BY clause
- **Signature:** `public static Criteria groupBy(final Collection<String> propNames)`
- **Summary:** Creates a new Criteria with a GROUP BY clause for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by
- **Returns:** a new Criteria with the GROUP BY clause
- **Signature:** `public static Criteria groupBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates a new Criteria with a GROUP BY clause for properties with sort direction.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** a new Criteria with the GROUP BY clause
- **Signature:** `public static Criteria groupBy(final Map<String, SortDirection> orders)`
- **Summary:** Creates a new Criteria with a GROUP BY clause from a map of properties to sort directions.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of property names to sort directions
- **Returns:** a new Criteria with the GROUP BY clause
##### having(...) -> Criteria
- **Signature:** `public static Criteria having(final Condition condition)`
- **Summary:** Creates a new Criteria with a HAVING clause containing the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition for the HAVING clause
- **Returns:** a new Criteria with the HAVING condition
- **Signature:** `public static Criteria having(final String condition)`
- **Summary:** Creates a new Criteria with a HAVING clause from a string expression.
- **Parameters:**
  - `condition` (`String`) — the SQL expression as a string
- **Returns:** a new Criteria with the HAVING condition
##### orderByAsc(...) -> Criteria
- **Signature:** `public static Criteria orderByAsc(final String... propNames)`
- **Summary:** Creates a new Criteria with an ORDER BY clause in ascending order.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by ascending
- **Returns:** a new Criteria with the ORDER BY clause
- **Signature:** `public static Criteria orderByAsc(final Collection<String> propNames)`
- **Summary:** Creates a new Criteria with an ORDER BY clause in ascending order from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by ascending
- **Returns:** a new Criteria with the ORDER BY clause
##### orderByDesc(...) -> Criteria
- **Signature:** `public static Criteria orderByDesc(final String... propNames)`
- **Summary:** Creates a new Criteria with an ORDER BY clause in descending order.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by descending
- **Returns:** a new Criteria with the ORDER BY clause
- **Signature:** `public static Criteria orderByDesc(final Collection<String> propNames)`
- **Summary:** Creates a new Criteria with an ORDER BY clause in descending order from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by descending
- **Returns:** a new Criteria with the ORDER BY clause
##### orderBy(...) -> Criteria
- **Signature:** `public static Criteria orderBy(final Condition condition)`
- **Summary:** Creates a new Criteria with an ORDER BY clause containing the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the ordering condition
- **Returns:** a new Criteria with the ORDER BY condition
- **Signature:** `public static Criteria orderBy(final String... propNames)`
- **Summary:** Creates a new Criteria with an ORDER BY clause for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by
- **Returns:** a new Criteria with the ORDER BY clause
- **Signature:** `public static Criteria orderBy(final String propName, final SortDirection direction)`
- **Summary:** Creates a new Criteria with an ORDER BY clause for a single property with direction.
- **Parameters:**
  - `propName` (`String`) — the property/column name to order by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** a new Criteria with the ORDER BY clause
- **Signature:** `public static Criteria orderBy(final Collection<String> propNames)`
- **Summary:** Creates a new Criteria with an ORDER BY clause for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by
- **Returns:** a new Criteria with the ORDER BY clause
- **Signature:** `public static Criteria orderBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates a new Criteria with an ORDER BY clause for properties with direction.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC)
- **Returns:** a new Criteria with the ORDER BY clause
- **Signature:** `public static Criteria orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Creates a new Criteria with an ORDER BY clause from a map of properties to directions.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of property names to sort directions
- **Returns:** a new Criteria with the ORDER BY clause
##### limit(...) -> Criteria
- **Signature:** `public static Criteria limit(final Limit condition)`
- **Summary:** Creates a new Criteria with a LIMIT clause from a Limit condition.
- **Parameters:**
  - `condition` (`Limit`) — the Limit condition
- **Returns:** a new Criteria with the LIMIT clause
- **Signature:** `public static Criteria limit(final int count)`
- **Summary:** Creates a new Criteria with a LIMIT clause for the specified count.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return
- **Returns:** a new Criteria with the LIMIT clause
- **Signature:** `public static Criteria limit(final int offset, final int count)`
- **Summary:** Creates a new Criteria with a LIMIT clause with offset and count.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip
  - `count` (`int`) — the maximum number of rows to return
- **Returns:** a new Criteria with the LIMIT clause
- **Signature:** `public static Criteria limit(final String expr)`
- **Summary:** Creates a new Criteria with a LIMIT clause from a string expression.
- **Parameters:**
  - `expr` (`String`) — the limit expression as a string
- **Returns:** a new Criteria with the LIMIT clause

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
- **Parameters:**
  - `sql` (`String`) — the SQL string to parse (must not be null or empty)
- **Returns:** a ParsedSql instance containing the parsed information

#### Public Instance Methods
##### sql(...) -> String
- **Signature:** `public String sql()`
- **Summary:** Gets the SQL string (trimmed of leading and trailing whitespace).
- **Parameters:**
  - (none)
- **Returns:** the trimmed SQL string
##### getParameterizedSql(...) -> String
- **Signature:** `public String getParameterizedSql()`
- **Summary:** Gets the parameterized SQL with all named parameters replaced by JDBC placeholders (?).
- **Parameters:**
  - (none)
- **Returns:** the parameterized SQL string with ? placeholders
- **Signature:** `public String getParameterizedSql(final boolean isForCouchbase)`
- **Summary:** Gets the parameterized SQL formatted for the specified database system.
- **Contract:**
  - When isForCouchbase is true, JDBC placeholders (?) are converted to Couchbase positional parameters ($1, $2, etc.).
  - When false, returns standard JDBC SQL with ?
- **Parameters:**
  - `isForCouchbase` (`boolean`) — {@code true} to get Couchbase-formatted SQL with $n parameters, {@code false} for standard JDBC format with ? placeholders
- **Returns:** the parameterized SQL string in the requested format
##### getNamedParameters(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> getNamedParameters()`
- **Summary:** Gets the list of named parameters extracted from the SQL in order of appearance.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter names
- **Signature:** `public ImmutableList<String> getNamedParameters(final boolean isForCouchbase)`
- **Summary:** Gets the list of named parameters formatted for the specified database system.
- **Contract:**
  - When isForCouchbase is true, returns parameter names suitable for Couchbase N1QL positional binding.
- **Parameters:**
  - `isForCouchbase` (`boolean`) — {@code true} to get Couchbase-formatted parameter names, {@code false} for standard format
- **Returns:** an immutable list of parameter names
##### getParameterCount(...) -> int
- **Signature:** `public int getParameterCount()`
- **Summary:** Gets the total number of parameters (named or positional) in the SQL.
- **Parameters:**
  - (none)
- **Returns:** the number of parameters in the SQL
- **Signature:** `public int getParameterCount(final boolean isForCouchbase)`
- **Summary:** Gets the parameter count formatted for the specified database system.
- **Parameters:**
  - `isForCouchbase` (`boolean`) — {@code true} to get Couchbase parameter count, {@code false} for standard count
- **Returns:** the number of parameters
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code value for this ParsedSql.
- **Parameters:**
  - (none)
- **Returns:** the hash code value
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Indicates whether some other object is "equal to" this one.
- **Contract:**
  - Two ParsedSql objects are equal if they have the same original SQL string.
- **Parameters:**
  - `obj` (`Object`) — the reference object with which to compare
- **Returns:** {@code true} if this object equals the obj argument; false otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this ParsedSql.
- **Parameters:**
  - (none)
- **Returns:** a string representation of the object

### Class QueryUtil (com.landawn.abacus.query.QueryUtil)
Utility class for handling database query operations, entity-column mappings, and SQL generation helpers.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### prop2ColumnNameMap(...) -> ImmutableMap<String, Tuple2<String, Boolean>>
- **Signature:** `@Deprecated @Beta public static ImmutableMap<String, Tuple2<String, Boolean>> prop2ColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy)`
- **Summary:** Returns a mapping of property names to their corresponding column names and a flag indicating if it's a simple property.
- **Contract:**
  - Returns a mapping of property names to their corresponding column names and a flag indicating if it's a simple property.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `namingPolicy` (`NamingPolicy`) — the naming policy to use for column name conversion
- **Returns:** an immutable map where keys are property names and values are tuples of (column name, isSimpleProperty)
##### getColumn2PropNameMap(...) -> ImmutableMap<String, String>
- **Signature:** `public static ImmutableMap<String, String> getColumn2PropNameMap(final Class<?> entityClass)`
- **Summary:** Gets a mapping of column names to property names for the specified entity class.
- **Contract:**
  - <p> This method is useful when you need to map database result set columns back to entity properties, especially when dealing with case-insensitive database systems or when column names don't match the exact case in your code.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Given an entity class with @Column annotations ImmutableMap<String, String> columnToProp = QueryUtil.getColumn2PropNameMap(User.class); // If User has @Column("user_name") on property "userName": String propName = columnToProp.get("user_name"); // "userName" String propName2 = columnToProp.get("USER_NAME"); // "userName" (uppercase variant) String propName3 = columnToProp.get("user_name"); // "userName" (lowercase variant) } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
- **Returns:** an immutable map of column names (including case variations) to property names
##### getProp2ColumnNameMap(...) -> ImmutableMap<String, String>
- **Signature:** `public static ImmutableMap<String, String> getProp2ColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy)`
- **Summary:** Gets a mapping of property names to column names for the specified entity class using the given naming policy.
- **Contract:**
  - <p> The naming policy determines how property names are converted to column names when no explicit {@code @Column} annotation is present.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Get property-to-column mapping with SNAKE_CASE naming policy ImmutableMap<String, String> propToColumn = QueryUtil.getProp2ColumnNameMap(User.class, NamingPolicy.SNAKE_CASE); // If User has property "firstName" without @Column annotation: String columnName = propToColumn.get("firstName"); // "first_name" // With SCREAMING_SNAKE_CASE naming policy ImmutableMap<String, String> propToColumnUpper = QueryUtil.getProp2ColumnNameMap(User.class, NamingPolicy.SCREAMING_SNAKE_CASE); String upperColumn = propToColumnUpper.get("firstName"); // "FIRST_NAME" } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze
  - `namingPolicy` (`NamingPolicy`) — the naming policy to use for column name conversion
- **Returns:** an immutable map of property names to column names, or empty map if entityClass is null or Map
##### getInsertPropNames(...) -> Collection<String>
- **Signature:** `@Internal public static Collection<String> getInsertPropNames(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Gets the property names to be used for INSERT operations on the given entity instance.
- **Contract:**
  - <p> The method intelligently handles ID fields: </p> <ul> <li> If all ID fields have default values (0 for numbers, null for objects), they are excluded from the result </li> <li> If any ID field has a non-default value, all insertable properties including IDs are returned </li> <li> This allows both auto-generated IDs and manually-assigned IDs to work correctly </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code User user = new User(); user.setName("John"); user.setEmail("john@example.com"); // user.id is 0 (default) so ID field will be excluded Collection<String> insertProps = QueryUtil.getInsertPropNames(user, null); // Returns: \["name", "email", ...\] (excludes "id" since it has default value) // With excluded properties Set<String> excluded = N.asSet("email"); Collection<String> filteredProps = QueryUtil.getInsertPropNames(user, excluded); // Returns: \["name", ...\] (excludes both "id" and "email") } </pre>
- **Parameters:**
  - `entity` (`Object`) — the entity instance to analyze (must not be null)
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; null or empty means no exclusions)
- **Returns:** collection of property names suitable for INSERT operations
- **Signature:** `@Internal public static Collection<String> getInsertPropNames(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Gets the property names to be used for INSERT operations on the given entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; null or empty means no exclusions)
- **Returns:** collection of property names suitable for INSERT operations
##### getSelectPropNames(...) -> Collection<String>
- **Signature:** `@Internal public static Collection<String> getSelectPropNames(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Gets the property names to be used for SELECT operations on the given entity class.
- **Contract:**
  - <p> When includeSubEntityProperties is true, the method returns nested properties using dot notation (e.g., "address.street").
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `includeSubEntityProperties` (`boolean`) — {@code true} to include nested entity properties, {@code false} for top-level only
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; null or empty means no exclusions)
- **Returns:** collection of property names suitable for SELECT operations
##### getUpdatePropNames(...) -> Collection<String>
- **Signature:** `@Internal public static Collection<String> getUpdatePropNames(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Gets the property names to be used for UPDATE operations on the given entity class.
- **Contract:**
  - <p> Properties are considered non-updatable if they are: </p> <ul> <li> Annotated with @Id </li> <li> Marked as insertable=false, updatable=false in @Column </li> <li> Listed in the excludedPropNames parameter </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code // Get all updatable property names (excludes ID fields automatically) Collection<String> updateProps = QueryUtil.getUpdatePropNames(User.class, null); // Returns: \["name", "email", "status", ...\] (excludes "id") // Exclude additional properties Set<String> excluded = N.asSet("createdDate"); Collection<String> filteredProps = QueryUtil.getUpdatePropNames(User.class, excluded); // Returns: \["name", "email", "status", ...\] (excludes "id" and "createdDate") } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; null or empty means no exclusions)
- **Returns:** collection of property names suitable for UPDATE operations
##### getIdFieldNames(...) -> List<String>
- **Signature:** `@Deprecated @Internal @Immutable public static List<String> getIdFieldNames(final Class<?> targetClass)`
- **Summary:** Gets the ID field names for the specified entity class.
- **Parameters:**
  - `targetClass` (`Class<?>`) — the entity class to analyze (must not be null)
- **Returns:** an immutable list of ID field names, or empty list if no ID fields are defined
- **Signature:** `@Deprecated @Internal @Immutable public static List<String> getIdFieldNames(final Class<?> targetClass, final boolean fakeIdForEmpty)`
- **Summary:** Gets the ID field names for the specified entity class with option to return fake ID if none found.
- **Contract:**
  - Gets the ID field names for the specified entity class with option to return fake ID if none found.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Without fake ID - returns empty list when no @Id is found List<String> idFields = QueryUtil.getIdFieldNames(LogEntry.class, false); // Returns: \[\] // With fake ID - returns a synthetic ID when no @Id is found List<String> fakeIdFields = QueryUtil.getIdFieldNames(LogEntry.class, true); // Returns: \["not_defined_fake_id_in_abacus_<uuid>"\] boolean isFake = QueryUtil.isFakeId(fakeIdFields); // true // Entity with @Id returns actual IDs regardless of fakeIdForEmpty List<String> realIds = QueryUtil.getIdFieldNames(User.class, true); // Returns: \["id"\] } </pre>
- **Parameters:**
  - `targetClass` (`Class<?>`) — the entity class to analyze
  - `fakeIdForEmpty` (`boolean`) — if true, returns a fake ID when no ID fields are found
- **Returns:** an immutable list of ID field names or fake ID if requested and none found
##### isNonColumn(...) -> boolean
- **Signature:** `public static boolean isNonColumn(final Set<String> columnFields, final Set<String> nonColumnFields, final PropInfo propInfo)`
- **Summary:** Determines whether a property should be excluded from database column mapping.
- **Contract:**
  - Determines whether a property should be excluded from database column mapping.
  - A property is not a column if it's transient, annotated with @NonColumn, or excluded by @Table configuration.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code BeanInfo beanInfo = ParserUtil.getBeanInfo(User.class); PropInfo propInfo = beanInfo.getPropInfo("tempField"); // Check if a property is excluded from column mapping Set<String> columnFields = N.asSet("id", "name", "email"); Set<String> nonColumnFields = N.emptySet(); boolean excluded = QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo); // Returns true if "tempField" is not in columnFields // Check with nonColumnFields Set<String> nonColumns = N.asSet("tempField", "transientData"); boolean excluded2 = QueryUtil.isNonColumn(N.emptySet(), nonColumns, propInfo); // Returns true if "tempField" is in nonColumnFields } </pre>
- **Parameters:**
  - `columnFields` (`Set<String>`) — set of field names explicitly included as columns (from @Table annotation, can be null or empty)
  - `nonColumnFields` (`Set<String>`) — set of field names explicitly excluded as columns (from @Table annotation, can be null or empty)
  - `propInfo` (`PropInfo`) — the property information to check (must not be null)
- **Returns:** {@code true} if the property should not be mapped to a database column
##### isFakeId(...) -> boolean
- **Signature:** `@Deprecated @Internal public static boolean isFakeId(final List<String> idPropNames)`
- **Summary:** Checks if the given ID property names represent a fake/synthetic ID.
- **Contract:**
  - Checks if the given ID property names represent a fake/synthetic ID.
  - Fake IDs are used internally when entities have no defined ID fields.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check if ID fields are real or synthetic List<String> idFields = QueryUtil.getIdFieldNames(User.class, true); boolean fake = QueryUtil.isFakeId(idFields); // Returns false for entities with real @Id annotations List<String> fakeFields = QueryUtil.getIdFieldNames(LogEntry.class, true); boolean isFake = QueryUtil.isFakeId(fakeFields); // Returns true for entities without @Id when fakeIdForEmpty was true } </pre>
- **Parameters:**
  - `idPropNames` (`List<String>`) — the list of ID property names to check
- **Returns:** {@code true} if this is a fake ID
##### repeatQM(...) -> String
- **Signature:** `public static String repeatQM(final int n)`
- **Summary:** Generates a string of question marks (?) repeated n times with comma-space delimiter.
- **Parameters:**
  - `n` (`int`) — the number of question marks to generate (must not be negative)
- **Returns:** a string containing n question marks separated by ", ", or empty string if n is 0
##### getTableAlias(...) -> String
- **Signature:** `public static String getTableAlias(final Class<?> entityClass)`
- **Summary:** Gets the table alias from the @Table annotation on the entity class.
- **Contract:**
  - <p> If no @Table annotation exists or if the alias is not specified in the annotation, this method returns null.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Given: @Table(name = "users", alias = "u") on User class String alias = QueryUtil.getTableAlias(User.class); // Returns: "u" // Given: @Table(name = "orders") on Order class (no alias) String alias2 = QueryUtil.getTableAlias(Order.class); // Returns: "" (empty string when alias is not specified) // Given: no @Table annotation on LogEntry class String alias3 = QueryUtil.getTableAlias(LogEntry.class); // Returns: null } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to check (must not be null)
- **Returns:** the table alias if defined in @Table annotation, null otherwise
##### getTableNameAndAlias(...) -> String
- **Signature:** `public static String getTableNameAndAlias(final Class<?> entityClass)`
- **Summary:** Gets the table name and optional alias for the entity class using the default naming policy.
- **Contract:**
  - If @Table annotation is present, uses its values; otherwise derives the table name from the class name using SNAKE_CASE naming policy.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
- **Returns:** the table name, optionally followed by space and alias
- **Signature:** `public static String getTableNameAndAlias(final Class<?> entityClass, final NamingPolicy namingPolicy)`
- **Summary:** Gets the table name and optional alias for the entity class using the specified naming policy.
- **Contract:**
  - If @Table annotation is present, uses its values; otherwise derives the table name from the class name using the provided naming policy.
  - <p> The naming policy is only used when no @Table annotation is present.
  - If @Table is defined, its name and alias values are used directly without any transformation.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be null)
  - `namingPolicy` (`NamingPolicy`) — the naming policy to use for table name conversion when @Table is not present
- **Returns:** the table name, optionally followed by space and alias

#### Public Instance Methods
- (none)

### Class SK (com.landawn.abacus.query.SK)
A utility class that provides a comprehensive dictionary of commonly used characters and strings, including special characters, operators, SQL keywords, and mathematical functions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class SQLBuilder (com.landawn.abacus.query.SQLBuilder)
A comprehensive, enterprise-grade fluent SQL builder providing type-safe, programmatic construction of complex SQL statements with advanced features including parameterized queries, multiple naming conventions, entity mapping, and sophisticated query optimization.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### sql(...) -> String
- **Signature:** `public String sql()`
- **Summary:** Generates the final SQL string from this builder.
- **Contract:**
  - After calling this method, the builder instance should not be reused as it may be in a closed state.
- **Parameters:**
  - (none)
- **Returns:** the generated SQL string
- **See also:** #sql(), #build()
##### query(...) -> String
- **Signature:** `@Override public String query()`
- **Summary:** Generates the final SQL query string and releases resources.
- **Contract:**
  - This method should be called only once.
- **Parameters:**
  - (none)
- **Returns:** the generated SQL query string
- **See also:** #sql()

### Class SCSB (com.landawn.abacus.query.SQLBuilder.SCSB)
Un-parameterized SQL builder with snake case (lower case with underscore) field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT SQL builder for a single column.
- **Parameters:**
  - `expr` (`String`) — the column name or expression
- **Returns:** a new SQLBuilder instance for INSERT operation
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the column names to insert
- **Returns:** a new SQLBuilder instance for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for a collection of columns.
- **Contract:**
  - <p> This method is useful when column names are determined dynamically.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of column names to insert
- **Returns:** a new SQLBuilder instance for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT SQL builder with column-value mappings.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of column names to values
- **Returns:** a new SQLBuilder instance for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT SQL builder from an entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SQLBuilder instance for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder from an entity object, excluding specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — properties to exclude from the insert
- **Returns:** a new SQLBuilder instance for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder from an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** a new SQLBuilder instance for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder from an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — properties to exclude from the insert
- **Returns:** a new SQLBuilder instance for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** a new SQLBuilder instance for INSERT operation
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO SQL builder for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — properties to exclude from the insert
- **Returns:** a new SQLBuilder instance for INSERT operation
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT SQL builder for multiple entities or property maps.
- **Contract:**
  - All entities or maps in the collection must have the same structure (same properties).
- **Parameters:**
  - `propsList` (`Collection<?>`) — list of entities or property maps to insert
- **Returns:** a new SQLBuilder instance for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE SQL builder for a table.
- **Contract:**
  - The SET clause should be added using the set() method.
- **Parameters:**
  - `tableName` (`String`) — the table name to update
- **Returns:** a new SQLBuilder instance for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for a table with entity class context.
- **Contract:**
  - <p> This method provides entity class information for property-to-column name mapping when building the UPDATE statement.
- **Parameters:**
  - `tableName` (`String`) — the table name to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** a new SQLBuilder instance for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE SQL builder for an entity class, excluding specified properties.
- **Contract:**
  - Useful for partial updates or when certain fields should not be modified.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — additional properties to exclude from updates
- **Returns:** a new SQLBuilder instance for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM SQL builder for a table.
- **Contract:**
  - A WHERE clause should typically be added to avoid deleting all rows.
- **Parameters:**
  - `tableName` (`String`) — the table name to delete from
- **Returns:** a new SQLBuilder instance for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for a table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the table name to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** a new SQLBuilder instance for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT SQL builder with a custom select expression.
- **Parameters:**
  - `selectPart` (`String`) — the select expression (e.g., "COUNT(*)", "DISTINCT name")
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the column names to select
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for a collection of columns.
- **Contract:**
  - <p> This method is useful when column names are determined dynamically at runtime.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of column names to select
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT SQL builder with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of column names to their aliases
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT SQL builder for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT SQL builder for an entity class with optional sub-entity properties.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties from related entities (marked with appropriate annotations) will also be included in the SELECT statement.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related entities
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for an entity class with full control over property inclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related entities
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT SQL builder for joining two entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — table alias for the first entity
  - `classAliasA` (`String`) — property prefix for the first entity in results
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — table alias for the second entity
  - `classAliasB` (`String`) — property prefix for the second entity in results
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT SQL builder for joining two entity classes with property exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — table alias for the first entity
  - `classAliasA` (`String`) — property prefix for the first entity in results
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — table alias for the second entity
  - `classAliasB` (`String`) — property prefix for the second entity in results
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SQLBuilder instance for SELECT operation
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a SELECT FROM SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM SQL builder for an entity class with table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder with sub-entity inclusion option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related entities
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder with alias and sub-entity inclusion option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related entities
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with alias, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with sub-entity inclusion and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related entities
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with full control over all options.
- **Contract:**
  - When sub-entities are included, appropriate joins will be generated automatically.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related entities
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT FROM SQL builder for joining two entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — table alias for the first entity
  - `classAliasA` (`String`) — property prefix for the first entity in results
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — table alias for the second entity
  - `classAliasB` (`String`) — property prefix for the second entity in results
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM SQL builder for joining two entity classes with property exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — table alias for the first entity
  - `classAliasA` (`String`) — property prefix for the first entity in results
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — table alias for the second entity
  - `classAliasB` (`String`) — property prefix for the second entity in results
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SQLBuilder instance for SELECT operation
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) SQL builder for a table.
- **Parameters:**
  - `tableName` (`String`) — the table name to count rows from
- **Returns:** a new SQLBuilder instance for COUNT operation
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** a new SQLBuilder instance for COUNT operation
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL with entity class context.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse
  - `entityClass` (`Class<?>`) — the entity class for property mapping (can be null)
- **Returns:** a new SQLBuilder instance containing the condition SQL

#### Public Instance Methods
- (none)

### Class ACSB (com.landawn.abacus.query.SQLBuilder.ACSB)
Un-parameterized SQL builder with all capital case (upper case with underscore) field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT SQL builder for a single column.
- **Parameters:**
  - `expr` (`String`) — the column name or expression to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert, in order
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for a collection of columns.
- **Contract:**
  - <p> This method is useful when column names are dynamically determined.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of column names to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT SQL builder with column-value mappings.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of column names to their corresponding values
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT SQL builder from an entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing data to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder from an entity object, excluding specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing data to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder from an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to use as template
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder from an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to use as template
  - `excludedPropNames` (`Set<String>`) — properties to exclude from the insert template
- **Returns:** a new SQLBuilder instance configured for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert into
- **Returns:** a new SQLBuilder instance configured for INSERT INTO operation
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO SQL builder for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert into
  - `excludedPropNames` (`Set<String>`) — properties to exclude from the insert
- **Returns:** a new SQLBuilder instance configured for INSERT INTO operation
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT SQL builder for multiple entities or property maps.
- **Contract:**
  - All entities or maps in the collection must have the same structure (same properties).
- **Parameters:**
  - `propsList` (`Collection<?>`) — list of entities or property maps to insert
- **Returns:** a new SQLBuilder instance configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE SQL builder for the specified table.
- **Contract:**
  - The columns to update should be specified using the set() method.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for a table with entity class context.
- **Contract:**
  - <p> This method provides entity class information for property-to-column name mapping when building the UPDATE statement.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE SQL builder for an entity class, excluding specified properties.
- **Contract:**
  - Useful for partial updates or when certain fields should not be modified.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — additional properties to exclude from updates
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM SQL builder for the specified table.
- **Contract:**
  - A WHERE clause should typically be added to avoid deleting all rows.
- **Parameters:**
  - `tableName` (`String`) — the table name to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for a table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the table name to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT SQL builder with a custom select expression.
- **Parameters:**
  - `selectPart` (`String`) — the SELECT expression or clause
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for the specified columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for a collection of columns.
- **Contract:**
  - <p> This method is useful when column names are determined dynamically at runtime.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT SQL builder with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of column names to their aliases
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT SQL builder for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class whose properties to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT SQL builder for an entity class with sub-entity option.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties of nested entities are included.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class whose properties to select
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class whose properties to select
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for an entity class with full control over property selection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class whose properties to select
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT SQL builder for joining two entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — class alias prefix for first entity columns
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — class alias prefix for second entity columns
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT SQL builder for joining two entity classes with exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — class alias prefix for first entity columns
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — class alias prefix for second entity columns
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from second entity
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT FROM SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM SQL builder with table alias for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder with sub-entity inclusion option.
- **Contract:**
  - <p> When includeSubEntityProperties is true, joins are added for sub-entities.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include and join sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder with alias and sub-entity options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — whether to include and join sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with alias and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with sub-entity and exclusion options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include and join sub-entities
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with full control over all options.
- **Contract:**
  - When sub-entities are included, appropriate joins are generated automatically.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — whether to include and join sub-entities
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a complete SELECT FROM SQL builder for joining two entities.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — class alias prefix for first entity columns
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — class alias prefix for second entity columns
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM SQL builder for joining two entities with exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — class alias prefix for first entity columns
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — class alias prefix for second entity columns
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from second entity
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) SQL builder for a table.
- **Parameters:**
  - `tableName` (`String`) — the table to count rows from
- **Returns:** a new SQLBuilder instance configured for COUNT operation
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count
- **Returns:** a new SQLBuilder instance configured for COUNT operation
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a Condition object into SQL with entity class mapping.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse into SQL
  - `entityClass` (`Class<?>`) — the entity class for property name mapping (can be null)
- **Returns:** a new SQLBuilder instance containing the parsed condition

#### Public Instance Methods
- (none)

### Class LCSB (com.landawn.abacus.query.SQLBuilder.LCSB)
SQL builder implementation with lower camel case naming policy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT SQL builder for a single column expression.
- **Parameters:**
  - `expr` (`String`) — the column name or expression to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **See also:** #insert(String...)
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified columns.
- **Contract:**
  - The actual values should be provided later using the VALUES clause.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified columns collection.
- **Contract:**
  - Useful when column names are dynamically determined.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT SQL builder with property name-value pairs.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of property names to their values
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT SQL builder from an entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **See also:** #insert(Object, Set)
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder from an entity object with excluded properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to create INSERT for
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **See also:** #insert(Class, Set)
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to create INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert into
- **Returns:** a new SQLBuilder instance configured for INSERT INTO operation
- **See also:** #insertInto(Class, Set)
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO SQL builder for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert into
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance configured for INSERT INTO operation
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT SQL builder for multiple entities or property maps.
- **Contract:**
  - All items must have the same structure.
- **Parameters:**
  - `propsList` (`Collection<?>`) — collection of entities or property maps to batch insert
- **Returns:** a new SQLBuilder instance configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE SQL builder for the specified table.
- **Contract:**
  - The columns to update should be specified using the {@code set()} method.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for the specified table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class corresponding to the table
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **See also:** #update(Class, Set)
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE SQL builder for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM SQL builder for the specified table.
- **Contract:**
  - A WHERE clause should typically be added to avoid deleting all rows.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for the specified table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class corresponding to the table
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT SQL builder with a custom select expression.
- **Parameters:**
  - `selectPart` (`String`) — the SELECT expression or clause
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for the specified columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for the specified columns collection.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT SQL builder with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of column names to their aliases
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT SQL builder for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **See also:** #select(Class, boolean)
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT SQL builder for an entity class with sub-entity option.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties of sub-entities (nested objects) will also be included in the SELECT statement.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // If User has an Address sub-entity String sql = LCSB.select(User.class, true) .from("users") .sql(); // Output: SELECT firstName, lastName, address.street, address.city FROM users } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for an entity class with sub-entity option and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT SQL builder for two entity classes with aliases.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias for the first entity (used in result mapping)
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias for the second entity (used in result mapping)
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT SQL builder for two entity classes with aliases and excluded properties.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias for the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias for the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT operation
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining the entities to select
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT operation
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a SELECT FROM SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **See also:** #selectFrom(Class, boolean)
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM SQL builder for an entity class with table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder for an entity class with sub-entity option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder with table alias and sub-entity option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the select
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with table alias and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the select
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with sub-entity option and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the select
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with full control over all options.
- **Contract:**
  - When sub-entities are included, appropriate joins will be generated.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the select
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT FROM SQL builder for two entity classes with aliases.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias for the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias for the second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT FROM operation
- **See also:** #select(Class, String, String, Class, String, String)
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM SQL builder for two entity classes with aliases and excluded properties.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias for the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias for the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining the entities to select
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT FROM operation
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the table to count rows from
- **Returns:** a new SQLBuilder instance configured for COUNT operation
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count
- **Returns:** a new SQLBuilder instance configured for COUNT operation
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL with entity class context.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse into SQL
  - `entityClass` (`Class<?>`) — the entity class for property name resolution
- **Returns:** a new SQLBuilder instance containing only the condition SQL
- **See also:** Filters

#### Public Instance Methods
- (none)

### Class PSB (com.landawn.abacus.query.SQLBuilder.PSB)
Parameterized SQL builder with no naming policy transformation.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT statement builder for a single column expression.
- **Parameters:**
  - `expr` (`String`) — the column name or expression to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT statement builder for the specified columns.
- **Contract:**
  - The actual values must be provided later using the {@code values()} method.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to include in the INSERT statement
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT statement builder for the specified collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to include in the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT statement builder using a map of property names to values.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map where keys are column names and values are the values to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT statement builder from an entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object whose properties will be inserted
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement builder from an entity object with excluded properties.
- **Contract:**
  - This is useful when certain properties should not be inserted even if they have values.
- **Parameters:**
  - `entity` (`Object`) — the entity object whose properties will be inserted
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT statement for
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement builder for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT statement for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO statement builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO statement for
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO statement builder for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO statement for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT statement builder for multiple records.
- **Parameters:**
  - `propsList` (`Collection<?>`) — collection of entities or property maps to insert
- **Returns:** a new SQLBuilder instance configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE statement builder for the specified table.
- **Contract:**
  - Columns to update must be specified using the {@code set()} method.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement builder for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate UPDATE statement for
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE statement builder for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate UPDATE statement for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the UPDATE
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM statement builder for the specified table.
- **Contract:**
  - WHERE conditions should be added to avoid deleting all records.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement builder for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate DELETE FROM statement for
- **Returns:** a new SQLBuilder instance configured for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT statement builder for a single column or expression.
- **Parameters:**
  - `selectPart` (`String`) — the column name or expression to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT statement builder for multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT statement builder for a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT statement builder with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map where keys are column names and values are aliases
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT statement builder for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class whose properties to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement builder for an entity class with sub-entity option.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties of sub-entities (nested objects) are also included in the selection with appropriate aliasing.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // If User has an Address sub-entity SQLBuilder builder = PSB.select(User.class, true) .from("users u") .join("addresses a").on("u.address_id = a.id"); } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class whose properties to select
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement builder for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class whose properties to select
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement builder with full control over entity property selection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class whose properties to select
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT statement builder for joining two entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class to select from
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the prefix for properties of the first entity in results
  - `entityClassB` (`Class<?>`) — the second entity class to select from
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the prefix for properties of the second entity in results
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT statement builder for joining two entities with excluded properties.
- **Contract:**
  - <p> Provides fine-grained control over which properties to include from each entity when performing joins.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class to select from
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the prefix for properties of the first entity in results
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class to select from
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the prefix for properties of the second entity in results
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT statement builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for each table
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT FROM statement builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM statement for
- **Returns:** a new SQLBuilder instance with both SELECT and FROM clauses set
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM statement builder with a table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM statement for
  - `alias` (`String`) — the table alias to use in the FROM clause
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses set
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement builder with sub-entity properties option.
- **Contract:**
  - <p> When includeSubEntityProperties is true, appropriate joins are automatically generated for sub-entities.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM statement for
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses set
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement builder with alias and sub-entity properties option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM statement for
  - `alias` (`String`) — the table alias to use in the FROM clause
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses set
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement builder with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM statement for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses set
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement builder with alias and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM statement for
  - `alias` (`String`) — the table alias to use in the FROM clause
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses set
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement builder with sub-entities and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM statement for
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses set
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement builder with full control over all options.
- **Contract:**
  - When sub-entities are included, appropriate JOIN clauses may be automatically generated.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM statement for
  - `alias` (`String`) — the table alias to use in the FROM clause
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses set
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a complete SELECT FROM statement for joining two entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class to select from
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the prefix for properties of the first entity in results
  - `entityClassB` (`Class<?>`) — the second entity class to select from
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the prefix for properties of the second entity in results
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM statement for two entities with excluded properties.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class to select from
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the prefix for properties of the first entity in results
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class to select from
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the prefix for properties of the second entity in results
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM statement builder for multiple entity selections.
- **Contract:**
  - <p> This method automatically generates both SELECT and FROM clauses based on the provided Selection configurations, including proper table aliasing and joins for sub-entities when specified.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for each table
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) query builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to count rows from
- **Returns:** a new SQLBuilder instance configured for COUNT query
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) query builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count rows for
- **Returns:** a new SQLBuilder instance configured for COUNT query
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL with entity class context.
- **Contract:**
  - <p> This method is useful for generating SQL fragments from Condition objects, particularly for debugging or when building complex dynamic queries.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse into SQL
  - `entityClass` (`Class<?>`) — the entity class for property name context
- **Returns:** a new SQLBuilder instance containing the parsed condition

#### Public Instance Methods
- (none)

### Class PSC (com.landawn.abacus.query.SQLBuilder.PSC)
Parameterized SQL builder with snake_case (lower case with underscore) field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT statement for a single column expression.
- **Contract:**
  - The actual value will be provided as a parameter when executing the query.
- **Parameters:**
  - `expr` (`String`) — the column name or expression to insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT statement for multiple columns.
- **Contract:**
  - Values will be provided as parameters when executing the query.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT statement for a collection of columns.
- **Contract:**
  - <p> This method provides flexibility when column names are dynamically generated or come from a collection.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT statement from a map of property names and values.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of property names to their values
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT statement from an entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement from an entity object with excluded properties.
- **Contract:**
  - Properties in the exclusion set will not be included even if they have values and are normally insertable.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance for method chaining
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO for
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance for method chaining
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Generates a MySQL-style batch INSERT statement.
- **Parameters:**
  - `propsList` (`Collection<?>`) — list of entities or property maps to insert
- **Returns:** a new SQLBuilder instance for method chaining
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE statement for a table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE statement for an entity class with excluded properties.
- **Contract:**
  - This is useful for partial updates or when certain fields should never be updated.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the update
- **Returns:** a new SQLBuilder instance for method chaining
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM statement for a table.
- **Contract:**
  - Property names in WHERE conditions will be converted to snake_case format if an entity class is associated.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SQLBuilder instance for method chaining
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT statement with a single expression.
- **Contract:**
  - <p> This method is useful for complex select expressions, aggregate functions, or when selecting computed values.
- **Parameters:**
  - `selectPart` (`String`) — the select expression
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT statement with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT statement with a collection of columns.
- **Contract:**
  - <p> This method provides flexibility when column names are dynamically generated.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT statement with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of property/column names to their aliases
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT statement for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement for an entity class with optional sub-entity properties.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties of nested entity objects are also included in the selection with appropriate prefixes.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with sub-entities and exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT statement for multiple entity classes (for joins).
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity results
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity results
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
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
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT statement for multiple entities using Selection descriptors.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SQLBuilder instance for method chaining
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM statement for an entity class with table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with optional sub-entity properties.
- **Contract:**
  - When sub-entities are included, appropriate joins may be generated automatically.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with alias and sub-entity option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with alias and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with sub-entities and exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT FROM statement with all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT FROM statement for multiple entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
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
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM statement for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) query for a table.
- **Parameters:**
  - `tableName` (`String`) — the table to count rows from
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) query for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count
- **Returns:** a new SQLBuilder instance for method chaining
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL with entity class mapping.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance containing just the condition SQL

#### Public Instance Methods
- (none)

### Class PAC (com.landawn.abacus.query.SQLBuilder.PAC)
Parameterized SQL builder with SCREAMING_SNAKE_CASE naming policy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT statement for a single expression or column.
- **Parameters:**
  - `expr` (`String`) — the expression or column name to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT statement for specified columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to include in the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT statement for specified columns from a collection.
- **Contract:**
  - <p> This method accepts a collection of column names, providing flexibility when the column list is dynamically generated.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to include
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT statement from a map of property names to values.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of property names to their values
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT statement from an entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement from an entity object with excluded properties.
- **Contract:**
  - Properties in the exclusion set will not be included even if they are normally insertable.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement template for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement template for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** a new SQLBuilder instance configured for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement for an entity class with automatic table name resolution.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert into
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement for an entity class with excluded properties and automatic table name.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert into
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** a new SQLBuilder instance configured for INSERT operation
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT statement for multiple entities (MySQL style).
- **Parameters:**
  - `propsList` (`Collection<?>`) — collection of entities or property maps to batch insert
- **Returns:** a new SQLBuilder instance configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE statement for a specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for a table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for an entity class with automatic table name.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from updates
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE statement for a specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE statement for a table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE statement for an entity class with automatic table name.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT statement with a single expression or column.
- **Parameters:**
  - `selectPart` (`String`) — the SELECT expression (e.g., "COUNT(*)", "MAX(age)", "firstName")
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT statement with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT statement with columns from a collection.
- **Contract:**
  - <p> This method accepts a collection of column names, useful when the column list is dynamically generated.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT statement with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of column names to their aliases
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT statement for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement for an entity class with sub-entity control.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties of nested entity types are also included in the selection with appropriate prefixes.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // If User has an Address sub-entity String sql = PAC.select(User.class, true).from("users").sql(); // Output includes address properties: ADDRESS_STREET AS "address.street", etc.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement with full control over entity property selection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT statement for two entity classes with aliases.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity results
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity results
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT statement for two entity classes with aliases and exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT statement for multiple entities using Selection descriptors.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection descriptors for each entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance with both SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM statement with a table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with sub-entity property control.
- **Contract:**
  - <p> When includeSubEntityProperties is true and the entity has sub-entities, appropriate joins may be generated automatically.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code String sql = PAC.selectFrom(User.class, true) .where(Filters.eq("active", true)) .sql(); // Output includes joins for sub-entities if present } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include sub-entity properties
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with table alias and sub-entity control.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — whether to include sub-entity properties
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with alias and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
  - `excludedPropNames` (`Set<String>`) — properties to exclude
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with sub-entity control and exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include sub-entity properties
  - `excludedPropNames` (`Set<String>`) — properties to exclude
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with full control over all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — whether to include sub-entity properties
  - `excludedPropNames` (`Set<String>`) — properties to exclude
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a complete SELECT FROM statement for two entities.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM statement for two entities with exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from second entity
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM statement for multiple entities.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection descriptors
- **Returns:** a new SQLBuilder instance with SELECT and FROM configured
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) query for a table.
- **Parameters:**
  - `tableName` (`String`) — the table to count rows from
- **Returns:** a new SQLBuilder instance configured for COUNT query
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) query for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count
- **Returns:** a new SQLBuilder instance configured for COUNT query
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL with entity property mapping.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse
  - `entityClass` (`Class<?>`) — entity class for property name mapping
- **Returns:** a new SQLBuilder instance containing the parsed condition

#### Public Instance Methods
- (none)

### Class PLC (com.landawn.abacus.query.SQLBuilder.PLC)
Parameterized SQL builder with camelCase field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT statement for a single column expression.
- **Parameters:**
  - `expr` (`String`) — the column name or expression to insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT statement for multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT statement for a collection of columns.
- **Contract:**
  - <p> This method is useful when the column list is dynamically generated or comes from another source.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT statement from a map of property names and values.
- **Contract:**
  - <p> This method is particularly useful when you have a dynamic set of fields to insert.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of property names to their values
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT statement from an entity object.
- **Contract:**
  - This is the most convenient way to insert data when working with entity objects.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement from an entity object with excluded properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement for an entity class.
- **Contract:**
  - This is useful when you want to generate the INSERT structure without having an actual entity instance.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement for an entity class with excluded properties.
- **Contract:**
  - <p> This method provides control over which properties to include when generating the INSERT statement from a class definition.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance for method chaining
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO for
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SQLBuilder instance for method chaining
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Generates a MySQL-style batch INSERT statement.
- **Parameters:**
  - `propsList` (`Collection<?>`) — list of entities or property maps to insert
- **Returns:** a new SQLBuilder instance for method chaining
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE statement for a table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for a table with entity class mapping.
- **Contract:**
  - <p> The entity class provides property-to-column name mapping information, which is useful when using the set() method with entity objects.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for an entity class.
- **Contract:**
  - All updatable properties are included by default when using set() with an entity object.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the update
- **Returns:** a new SQLBuilder instance for method chaining
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM statement for a table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SQLBuilder instance for method chaining
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT statement with a single expression.
- **Contract:**
  - <p> This method is useful for complex select expressions, aggregate functions, or when selecting computed values.
- **Parameters:**
  - `selectPart` (`String`) — the select expression
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT statement with multiple columns.
- **Contract:**
  - This is the most common way to create SELECT statements when you know the specific columns needed.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT statement with a collection of columns.
- **Contract:**
  - <p> This method is useful when the column list is dynamically generated or comes from another source.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Dynamic column selection List<String> columns = getUserSelectedColumns(); String sql = PLC.select(columns) .from("account") .sql(); // Programmatically built column list List<String> cols = new ArrayList<>(); cols.add("id"); cols.add("firstName"); if (includeEmail) { cols.add("emailAddress"); } String sql2 = PLC.select(cols).from("account").sql(); // From entity metadata List<String> entityColumns = getEntityColumns(Account.class); String sql3 = PLC.select(entityColumns).from("account").sql(); } </pre>
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT statement with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of property/column names to their aliases
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT statement for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement for an entity class with optional sub-entity properties.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties of nested entity objects are also included in the selection, which is useful for fetching related data.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT statement for multiple entity classes (for joins).
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT statement for multiple entity classes with exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity
  - `excludedPropNamesA` (`Set<String>`) — excluded properties for first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity
  - `excludedPropNamesB` (`Set<String>`) — excluded properties for second entity
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT statement for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SQLBuilder instance for method chaining
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM statement for an entity class with table alias.
- **Contract:**
  - <p> Table aliases are essential for joins and disambiguating column names when multiple tables are involved in the query.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with optional sub-entity properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with alias and sub-entity option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with alias and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with sub-entities and exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT FROM statement with all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT FROM statement for multiple entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — property prefix for first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — property prefix for second entity
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
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
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM statement for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SQLBuilder instance for method chaining
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) query for a table.
- **Parameters:**
  - `tableName` (`String`) — the table to count rows from
- **Returns:** a new SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) query for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count
- **Returns:** a new SQLBuilder instance for method chaining
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL with entity class mapping.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance containing just the condition SQL

#### Public Instance Methods
- (none)

### Class NSB (com.landawn.abacus.query.SQLBuilder.NSB)
Named SQL builder with {@code NamingPolicy.NO_CHANGE} field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT SQL builder with a single column expression.
- **Parameters:**
  - `expr` (`String`) — the column name or expression to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder with specified column names.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to include in the INSERT statement
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder with a collection of column names.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to include
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT SQL builder from a map of column names to values.
- **Contract:**
  - This method is useful when you have dynamic column-value pairs.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of column names to their values
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT SQL builder from an entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing data to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder from an entity object with excluded properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing data to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder for a specific entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for a specific entity class with excluded properties.
- **Contract:**
  - <p> This method provides control over which properties to include in the INSERT statement when generating SQL from a class definition.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** a new SQLBuilder instance configured for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder with automatic table name detection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class for INSERT operation
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder with automatic table name detection and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class for INSERT operation
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT SQL builder for multiple records (MySQL style).
- **Parameters:**
  - `propsList` (`Collection<?>`) — collection of entities or property maps to insert
- **Returns:** a new SQLBuilder instance configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE SQL builder for the specified table.
- **Contract:**
  - You must call {@code set()} methods to specify which columns to update.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for an entity class with automatic table name detection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE SQL builder for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the UPDATE
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE SQL builder for a table with entity class mapping.
- **Contract:**
  - <p> This method enables proper property-to-column name mapping when building WHERE conditions for the DELETE statement.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE SQL builder for an entity class with automatic table name detection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT SQL builder with a single column or expression.
- **Parameters:**
  - `selectPart` (`String`) — the column name or SQL expression to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — array of property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT SQL builder with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of column names/expressions to their aliases
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT SQL builder for all properties of an entity class.
- **Contract:**
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // If User class has properties: id, name, email, address String sql = NSB.select(User.class).from("users").sql(); // SELECT id, name, email, address FROM users } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT SQL builder for an entity class with optional sub-entity properties.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties of nested entity types are also included in the selection, which is useful for fetching related data in a single query.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // If User has an Address sub-entity String sql = NSB.select(User.class, true) .from("users u") .leftJoin("addresses a").on("u.address_id = a.id") .sql(); // SELECT u.id, u.name, u.email, a.street, a.city, a.zip FROM users u // LEFT JOIN addresses a ON u.address_id = a.id } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from nested entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for an entity class with excluded properties.
- **Contract:**
  - <p> This method allows selecting most properties from an entity while excluding specific ones, which is useful when you want to omit large fields like BLOBs or sensitive data.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder with full control over entity property selection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from nested entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT SQL builder for two entity classes with aliases.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — column prefix for first entity in results
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — column prefix for second entity in results
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT SQL builder for two entity classes with aliases and exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — column prefix for first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — column prefix for second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT SQL builder for multiple entity selections.
- **Contract:**
  - <p> This is the most flexible method for multi-table selections, accepting a list of Selection objects that define how each entity should be selected.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT...FROM SQL builder for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a complete SELECT...FROM SQL builder with a table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT...FROM SQL builder with sub-entity property inclusion.
- **Contract:**
  - <p> When includeSubEntityProperties is true, the method automatically handles joining related tables for nested entities.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include nested entity properties
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT...FROM SQL builder with alias and sub-entity control.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — whether to include nested entity properties
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT...FROM SQL builder with property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT...FROM SQL builder with alias and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
  - `excludedPropNames` (`Set<String>`) — properties to exclude
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT...FROM SQL builder with sub-entities and exclusions.
- **Contract:**
  - <p> This method automatically handles complex FROM clauses when sub-entities are included.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include nested entity properties
  - `excludedPropNames` (`Set<String>`) — properties to exclude
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a fully-configured SELECT...FROM SQL builder.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — whether to include nested entity properties
  - `excludedPropNames` (`Set<String>`) — properties to exclude from selection
- **Returns:** a new SQLBuilder instance with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a complete SELECT...FROM SQL builder for two entities.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — column prefix for first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — column prefix for second entity
- **Returns:** a new SQLBuilder with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a complete SELECT...FROM SQL builder for two entities with exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — first entity class
  - `tableAliasA` (`String`) — table alias for first entity
  - `classAliasA` (`String`) — column prefix for first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from first entity
  - `entityClassB` (`Class<?>`) — second entity class
  - `tableAliasB` (`String`) — table alias for second entity
  - `classAliasB` (`String`) — column prefix for second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from second entity
- **Returns:** a new SQLBuilder with SELECT and FROM clauses configured
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a complete SELECT...FROM SQL builder for multiple entities.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations
- **Returns:** a new SQLBuilder with SELECT and FROM clauses configured
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) query for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the table to count records from
- **Returns:** a new SQLBuilder configured for COUNT query
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) query for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count
- **Returns:** a new SQLBuilder configured for COUNT query
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL with entity class mapping.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse
  - `entityClass` (`Class<?>`) — the entity class for property name mapping
- **Returns:** a new SQLBuilder containing only the condition SQL

#### Public Instance Methods
- (none)

### Class NSC (com.landawn.abacus.query.SQLBuilder.NSC)
Named SQL builder with snake_case (lower case with underscore) field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT SQL builder for a single column expression with named parameters.
- **Parameters:**
  - `expr` (`String`) — the column name or expression to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation with named parameters
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for multiple columns with named parameters.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation with named parameters
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for a collection of columns with named parameters.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of property or column names to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation with named parameters
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT SQL builder from a map of column-value pairs with named parameters.
- **Parameters:**
  - `props` (`Map<String, Object>`) — the map of property names to values
- **Returns:** a new SQLBuilder instance configured for INSERT operation with named parameters
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT SQL builder from an entity object with named parameters.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation with named parameters
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder from an entity object with excluded properties and named parameters.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation with named parameters
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder for an entity class with named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to base the INSERT on
- **Returns:** a new SQLBuilder instance configured for INSERT operation with named parameters
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for an entity class with excluded properties and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to base the INSERT on
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation with named parameters
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO SQL builder for an entity class with named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert into
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO SQL builder for an entity class with excluded properties and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert into
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT SQL builder with named parameters in MySQL style.
- **Parameters:**
  - `propsList` (`Collection<?>`) — list of entities or property maps to insert
- **Returns:** a new SQLBuilder instance configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE SQL builder for a table with named parameters.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation with named parameters
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for a table with entity class context and named parameters.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for UPDATE operation with named parameters
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for an entity class with named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation with named parameters
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE SQL builder for an entity class with excluded properties and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the UPDATE
- **Returns:** a new SQLBuilder instance configured for UPDATE operation with named parameters
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM SQL builder for a table with named parameters.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation with named parameters
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for a table with entity class context and named parameters.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for DELETE operation with named parameters
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for an entity class with named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation with named parameters
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT SQL builder with a single column or expression using named parameters.
- **Parameters:**
  - `selectPart` (`String`) — the column name or SQL expression to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation with named parameters
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder with multiple columns using named parameters.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation with named parameters
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder with a collection of columns using named parameters.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation with named parameters
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT SQL builder with column aliases using named parameters.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of column names to their aliases
- **Returns:** a new SQLBuilder instance configured for SELECT operation with named parameters
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT SQL builder for all properties of an entity class with named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
- **Returns:** a new SQLBuilder instance configured for SELECT operation with named parameters
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT SQL builder for an entity class with sub-entity option and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — true to include properties of embedded entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation with named parameters
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for an entity class with excluded properties and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation with named parameters
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for an entity class with all options and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — true to include properties of embedded entities
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation with named parameters
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT SQL builder for multiple entity classes with named parameters.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias for property prefixing of the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias for property prefixing of the second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT SQL builder for multiple entity classes with exclusions and named parameters.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias for property prefixing of the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias for property prefixing of the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT operation
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT SQL builder for multiple entity selections with named parameters.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining the entities to select
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT operation
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a SELECT FROM SQL builder for an entity class with named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM SQL builder for an entity class with table alias and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder with sub-entity option and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — true to include properties of embedded entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder with alias and sub-entity option using named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — true to include properties of embedded entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with excluded properties and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with alias and excluded properties using named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with sub-entity and exclusion options using named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — true to include properties of embedded entities
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with all options and named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — true to include properties of embedded entities
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from selection
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT FROM SQL builder for multiple entity classes with named parameters.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias for property prefixing of the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias for property prefixing of the second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM SQL builder for multiple entities with exclusions and named parameters.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias for property prefixing of the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias for property prefixing of the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM SQL builder for multiple selections with named parameters.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining the entities to select
- **Returns:** a new SQLBuilder instance configured for multi-table SELECT with FROM clause
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) SQL builder for a table with named parameters.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to count rows from
- **Returns:** a new SQLBuilder instance configured for COUNT operation
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) SQL builder for an entity class with named parameters.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count rows from
- **Returns:** a new SQLBuilder instance configured for COUNT operation
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL with entity class context and named parameters.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse into SQL
  - `entityClass` (`Class<?>`) — the entity class for property-to-column mapping
- **Returns:** a new SQLBuilder instance containing only the parsed condition

#### Public Instance Methods
- (none)

### Class NAC (com.landawn.abacus.query.SQLBuilder.NAC)
Named SQL builder with all capital case (upper case with underscore) field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT SQL builder for a single column expression.
- **Parameters:**
  - `expr` (`String`) — the column expression to insert
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified property or column names.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified collection of property or column names.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of property or column names to insert
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT SQL builder for the specified property-value map.
- **Parameters:**
  - `props` (`Map<String, Object>`) — the map of property names to values
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT SQL builder for the specified entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the specified entity object with excluded properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the INSERT
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the INSERT
- **Returns:** an SQLBuilder configured for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for INSERT operation with table name set
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO SQL builder for the specified entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the INSERT
- **Returns:** an SQLBuilder configured for INSERT operation with table name set
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT SQL builder for MySQL-style batch inserts.
- **Parameters:**
  - `propsList` (`Collection<?>`) — collection of entities or property maps to insert
- **Returns:** an SQLBuilder configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** an SQLBuilder configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for the specified table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** an SQLBuilder configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE SQL builder for the specified entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the UPDATE
- **Returns:** an SQLBuilder configured for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** an SQLBuilder configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for the specified table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** an SQLBuilder configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT SQL builder with a single select expression.
- **Parameters:**
  - `selectPart` (`String`) — the select expression
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for the specified property or column names.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for the specified collection of property or column names.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of property or column names to select
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT SQL builder with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of property/column names to their aliases
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT SQL builder for all properties of the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT SQL builder for properties of the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for properties of the specified entity class with exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for properties of the specified entity class with options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT SQL builder for two entity classes with table and class aliases.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for columns from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for columns from the second entity
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT SQL builder for two entity classes with aliases and exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for columns from the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for columns from the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for each entity
- **Returns:** an SQLBuilder configured for SELECT operation
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a SELECT FROM SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM SQL builder for the specified entity class with table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder for the specified entity class with sub-entity option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder with table alias and sub-entity option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with table alias and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with sub-entity option and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT FROM SQL builder for two entity classes with table and class aliases.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for columns from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for columns from the second entity
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM SQL builder for two entity classes with aliases and exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for columns from the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for columns from the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for each entity
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to count rows from
- **Returns:** an SQLBuilder configured for COUNT query
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for COUNT query
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL using the entity class for property mapping.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse
  - `entityClass` (`Class<?>`) — the entity class for property-to-column mapping
- **Returns:** an SQLBuilder containing the parsed condition

#### Public Instance Methods
- (none)

### Class NLC (com.landawn.abacus.query.SQLBuilder.NLC)
Named SQL builder with lower camel case field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT SQL builder for a single column expression.
- **Contract:**
  - This method is useful when inserting data into a single column or when using SQL expressions.
- **Parameters:**
  - `expr` (`String`) — the column expression to insert
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified property or column names.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified collection of property or column names.
- **Contract:**
  - This method is useful when the column names are dynamically determined at runtime.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of property or column names to insert
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT SQL builder for the specified property-value map.
- **Parameters:**
  - `props` (`Map<String, Object>`) — the map of property names to values
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT SQL builder for the specified entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the specified entity object with excluded properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the INSERT
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class.
- **Contract:**
  - This method is useful when you want to prepare an INSERT template based on the entity structure.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the INSERT
- **Returns:** an SQLBuilder configured for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for INSERT operation with table name set
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO SQL builder for the specified entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the INSERT
- **Returns:** an SQLBuilder configured for INSERT operation with table name set
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT SQL builder for MySQL-style batch inserts.
- **Parameters:**
  - `propsList` (`Collection<?>`) — collection of entities or property maps to insert
- **Returns:** an SQLBuilder configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** an SQLBuilder configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for the specified table with entity class mapping.
- **Contract:**
  - This is useful when you want to update a table using entity property names.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** an SQLBuilder configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE SQL builder for the specified entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the UPDATE
- **Returns:** an SQLBuilder configured for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** an SQLBuilder configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for the specified table with entity class mapping.
- **Contract:**
  - This is useful when you want to use entity property names in the WHERE clause.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** an SQLBuilder configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT SQL builder with a single select expression.
- **Contract:**
  - This method is useful for simple queries or when using SQL functions.
- **Parameters:**
  - `selectPart` (`String`) — the select expression
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for the specified property or column names.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for the specified collection of property or column names.
- **Contract:**
  - This method is useful when the columns to select are determined dynamically at runtime.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of property or column names to select
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT SQL builder with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of property/column names to their aliases
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT SQL builder for all properties of the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT SQL builder for properties of the specified entity class.
- **Contract:**
  - This method allows control over whether properties from sub-entities should be included.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Without sub-entity properties String sql1 = NLC.select(Order.class, false).from("orders").sql(); // Output: SELECT id, orderNumber, amount, status FROM orders // With sub-entity properties (if Order has an Account sub-entity) String sql2 = NLC.select(Order.class, true).from("orders").sql(); // Output: SELECT id, orderNumber, amount, status, account.id, account.firstName, account.lastName FROM orders } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for properties of the specified entity class with exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for properties of the specified entity class with full control.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT SQL builder for two entity classes with table and class aliases.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for columns from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for columns from the second entity
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT SQL builder for two entity classes with aliases and exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for columns from the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for columns from the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** an SQLBuilder configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT SQL builder for multiple entity selections.
- **Contract:**
  - Each Selection object specifies how columns from a particular entity should be selected and aliased.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for each entity
- **Returns:** an SQLBuilder configured for SELECT operation
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a SELECT FROM SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM SQL builder for the specified entity class with table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder for the specified entity class with sub-entity option.
- **Contract:**
  - When sub-entity properties are included, the appropriate JOIN clauses are automatically generated.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM SQL builder with table alias and sub-entity option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with excluded properties.
- **Contract:**
  - This is a convenience method for common use cases where certain properties should be excluded.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with table alias and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with sub-entity option and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM SQL builder with full control over all options.
- **Contract:**
  - When includeSubEntityProperties is true, appropriate JOIN clauses are automatically generated.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `alias` (`String`) — the table alias
  - `includeSubEntityProperties` (`boolean`) — if true, properties of sub-entities will be included
  - `excludedPropNames` (`Set<String>`) — the set of property names to exclude from the SELECT
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT FROM SQL builder for two entity classes with table and class aliases.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for columns from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for columns from the second entity
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM SQL builder for two entity classes with aliases and exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for columns from the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for columns from the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for each entity
- **Returns:** an SQLBuilder configured for SELECT operation with FROM clause
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to count rows from
- **Returns:** an SQLBuilder configured for COUNT query
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** an SQLBuilder configured for COUNT query
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL using the entity class for property mapping.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse
  - `entityClass` (`Class<?>`) — the entity class for property-to-column mapping
- **Returns:** an SQLBuilder containing the parsed condition

#### Public Instance Methods
- (none)

### Class MSB (com.landawn.abacus.query.SQLBuilder.MSB)
Named SQL builder with {@code NamingPolicy.NO_CHANGE} field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT statement for a single column.
- **Parameters:**
  - `expr` (`String`) — the column name or expression to insert
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT statement for the specified columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to include in the INSERT
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT statement for the specified columns provided as a collection.
- **Contract:**
  - <p> This method is useful when the column names are dynamically determined.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to include
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT statement using a map of column names to values.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of column names to their values
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT statement based on an entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing data to insert
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement based on an entity object, excluding specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing data to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement for all insertable properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement for an entity class, excluding specified properties.
- **Contract:**
  - <p> This method provides control over which properties are included when generating an INSERT template from an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** the SQLBuilder instance for method chaining
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement for an entity class with automatic table name detection.
- **Contract:**
  - <p> The table name is determined from the {@code @Table} annotation on the entity class, or derived from the class name if no annotation is present.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement for an entity class with automatic table name detection, excluding specified properties.
- **Contract:**
  - This is useful when certain properties should not be inserted (e.g., auto-generated IDs, calculated fields).
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** this builder instance for method chaining
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT statement for multiple records in MySQL style.
- **Contract:**
  - All items must have the same structure (same properties/keys).
- **Parameters:**
  - `propsList` (`Collection<?>`) — collection of entities or property maps to insert
- **Returns:** the SQLBuilder instance for method chaining
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE statement for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for a table with entity class context.
- **Contract:**
  - <p> This method is useful when you want to specify a custom table name but still use entity class metadata for column mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for column mapping
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for an entity class with automatic table name detection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE statement for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the update
- **Returns:** the SQLBuilder instance for method chaining
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE statement for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE statement for a table with entity class context.
- **Contract:**
  - <p> This method is useful when you want to use a custom table name but still benefit from entity class metadata for column mapping in WHERE conditions.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for column mapping
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE statement for an entity class with automatic table name detection.
- **Contract:**
  - <p> The table name is determined from the {@code @Table} annotation on the entity class, or derived from the class name if no annotation is present.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table
- **Returns:** the SQLBuilder instance for method chaining
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT statement with a single expression or column.
- **Parameters:**
  - `selectPart` (`String`) — the SELECT expression (column, function, etc.)
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT statement with multiple columns or expressions.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — array of property or column names to select
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT statement with columns specified as a collection.
- **Contract:**
  - <p> This method is useful when the columns to select are determined dynamically.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT statement with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of column names/expressions to their aliases
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT statement for all columns of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement for an entity class with sub-entity control.
- **Contract:**
  - <p> When {@code includeSubEntityProperties} is true, properties that are themselves entities will have their properties included in the selection with prefixed names.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // If User has an Address property String sql = MSB.select(User.class, true).from("users").sql(); // May include: id, firstName, address.street, address.city, etc.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with full control over selection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT statement for joining two entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — table alias for the first entity
  - `classAliasA` (`String`) — column prefix for the first entity's columns in the result
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — table alias for the second entity
  - `classAliasB` (`String`) — column prefix for the second entity's columns in the result
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT statement for joining two entity classes with property exclusion.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — table alias for the first entity
  - `classAliasA` (`String`) — column prefix for the first entity's columns
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — table alias for the second entity
  - `classAliasB` (`String`) — column prefix for the second entity's columns
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT statement for multiple entity classes with detailed configuration.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for each entity
- **Returns:** the SQLBuilder instance for method chaining
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a SELECT FROM statement for an entity class with a table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with sub-entity inclusion control.
- **Contract:**
  - <p> When sub-entities are included, the query may generate JOINs to fetch related entity data in a single query.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with table alias and sub-entity control.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with alias and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with sub-entity control and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with full control over all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a complete SELECT FROM statement for joining two entities.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — table alias for the first entity
  - `classAliasA` (`String`) — column prefix for the first entity's columns
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — table alias for the second entity
  - `classAliasB` (`String`) — column prefix for the second entity's columns
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT FROM statement for two entities with property exclusion.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — table alias for the first entity
  - `classAliasA` (`String`) — column prefix for the first entity's columns
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — table alias for the second entity
  - `classAliasB` (`String`) — column prefix for the second entity's columns
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** this builder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT FROM statement for multiple entities with detailed configuration.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for each entity
- **Returns:** this builder instance for method chaining
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) query for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the table to count rows from
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) query for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table
- **Returns:** the SQLBuilder instance for method chaining
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL with entity class context.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse
  - `entityClass` (`Class<?>`) — the entity class for column name mapping
- **Returns:** the SQLBuilder instance containing the parsed condition

#### Public Instance Methods
- (none)

### Class MSC (com.landawn.abacus.query.SQLBuilder.MSC)
MyBatis-style SQL builder with snake_case field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT statement for a single column.
- **Parameters:**
  - `expr` (`String`) — the property name or expression to insert
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT statement for the specified properties.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property names to include in the INSERT
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT statement for properties provided as a collection.
- **Contract:**
  - <p> This method is useful when property names are determined at runtime.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property names to include
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT statement using a map of properties to values.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of property names to their values
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT statement based on an entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing data to insert
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement based on an entity object, excluding specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object containing data to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement template for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement template for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** the SQLBuilder instance for method chaining
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement with automatic table name detection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement with automatic table name detection, excluding properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** the SQLBuilder instance for method chaining
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT statement for multiple records.
- **Parameters:**
  - `propsList` (`Collection<?>`) — collection of entities or property maps to insert
- **Returns:** the SQLBuilder instance for method chaining
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE statement for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for a table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE statement for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the update
- **Returns:** the SQLBuilder instance for method chaining
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE statement for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE statement for a table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** the SQLBuilder instance for method chaining
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT statement with a single expression or column.
- **Parameters:**
  - `selectPart` (`String`) — the SQL expression or column name to select
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT statement for the specified columns or properties.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT statement for columns provided as a collection.
- **Contract:**
  - <p> Useful when column names are determined at runtime.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT statement with custom column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of property/column names to their aliases
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT statement for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement for an entity class with sub-entity control.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties of embedded entities will also be included in the selection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from embedded entities
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with full control options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from embedded entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT statement for joining two entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the result set alias prefix for the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the result set alias prefix for the second entity
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT statement for joining two entity classes with property exclusion.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the result set alias prefix for the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the result set alias prefix for the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT statement for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining entities and their configurations
- **Returns:** the SQLBuilder instance for method chaining
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT statement with automatic table name detection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a complete SELECT statement with table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement with sub-entity inclusion control.
- **Contract:**
  - <p> When includeSubEntityProperties is true, performs joins for embedded entities.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from embedded entities
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement with table alias and sub-entity control.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from embedded entities
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement with table alias and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement with sub-entity control and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from embedded entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement with full control over all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from embedded entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a complete SELECT statement for joining two entities with automatic FROM clause.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the result set alias prefix for the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the result set alias prefix for the second entity
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a complete SELECT statement for joining entities with property exclusion.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the result set alias prefix for the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the result set alias prefix for the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a complete SELECT statement for multiple entities with automatic FROM clause.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining entities and their configurations
- **Returns:** the SQLBuilder instance for method chaining
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a SELECT COUNT(*) statement for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to count
- **Returns:** the SQLBuilder instance for method chaining
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a SELECT COUNT(*) statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count
- **Returns:** the SQLBuilder instance for method chaining
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Generates SQL for a condition only, without a complete statement.
- **Parameters:**
  - `cond` (`Condition`) — the condition to generate SQL for
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** the SQLBuilder instance containing only the condition SQL

#### Public Instance Methods
- (none)

### Class MAC (com.landawn.abacus.query.SQLBuilder.MAC)
MyBatis-style SQL builder with all capital case (upper case with underscore) field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT SQL builder for a single column expression.
- **Parameters:**
  - `expr` (`String`) — the column expression or property name to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified collection of columns.
- **Contract:**
  - <p> Useful when column names are determined at runtime.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT SQL builder using a map of property names to values.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of property names to their values
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT SQL builder for the given entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the given entity object, excluding specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class with automatic table name detection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class with automatic table name detection, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Creates a batch INSERT SQL builder for multiple entities or property maps.
- **Parameters:**
  - `propsList` (`Collection<?>`) — collection of entities or property maps to batch insert
- **Returns:** a new SQLBuilder instance configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for the specified table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE SQL builder for the specified entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the UPDATE
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE SQL builder for the specified table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT SQL builder with a single column or expression.
- **Parameters:**
  - `selectPart` (`String`) — the column name or SQL expression to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT SQL builder with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of property/column names to their aliases
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT SQL builder for all properties of the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT SQL builder for the specified entity class with optional sub-entity properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for the specified entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for the specified entity class with full control over included properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT SQL builder for joining two entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for the first entity's columns
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for the second entity's columns
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT SQL builder for joining two entity classes with property exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for the first entity's columns
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for the second entity's columns
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT SQL builder for multiple entity selections with custom configurations.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for multiple entities
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT FROM SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a complete SELECT FROM SQL builder with a table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a complete SELECT FROM SQL builder with optional sub-entity properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a complete SELECT FROM SQL builder with table alias and sub-entity control.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT FROM SQL builder with property exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT FROM SQL builder with table alias and property exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT FROM SQL builder with sub-entity control and property exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT FROM SQL builder with full control over all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from related sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT FROM operation
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a complete SELECT FROM SQL builder for joining two entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for the first entity's columns
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for the second entity's columns
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT FROM
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a complete SELECT FROM SQL builder for joining two entity classes with property exclusions.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the class alias prefix for the first entity's columns
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the class alias prefix for the second entity's columns
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT FROM
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a complete SELECT FROM SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection configurations for multiple entities
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT FROM
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the table to count rows from
- **Returns:** a new SQLBuilder instance configured for COUNT operation
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count rows from
- **Returns:** a new SQLBuilder instance configured for COUNT operation
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL format using the specified entity class for property mapping.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse
  - `entityClass` (`Class<?>`) — the entity class for property to column mapping
- **Returns:** a new SQLBuilder instance containing the parsed condition

#### Public Instance Methods
- (none)

### Class MLC (com.landawn.abacus.query.SQLBuilder.MLC)
MyBatis-style SQL builder with lower camel case field/column naming strategy.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### insert(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insert(final String expr)`
- **Summary:** Creates an INSERT SQL builder for a single column expression.
- **Parameters:**
  - `expr` (`String`) — the column expression or property name to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT SQL builder for the specified collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT SQL builder using a map of property names to values.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of property names to their values
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT SQL builder for the given entity object.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the given entity object, excluding specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SQLBuilder instance configured for INSERT operation
- **Signature:** `public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation
##### insertInto(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class with automatic table name detection.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
- **Signature:** `public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT SQL builder for the specified entity class with automatic table name detection, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the INSERT
- **Returns:** a new SQLBuilder instance configured for INSERT operation with table name set
##### batchInsert(...) -> SQLBuilder
- **Signature:** `@Beta public static SQLBuilder batchInsert(final Collection<?> propsList)`
- **Summary:** Generates MySQL-style batch insert SQL for multiple entities or property maps.
- **Parameters:**
  - `propsList` (`Collection<?>`) — list of entities or properties maps to batch insert
- **Returns:** a new SQLBuilder instance configured for batch INSERT operation
##### update(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for the specified table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for column mapping metadata
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate UPDATE for
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
- **Signature:** `public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE SQL builder for the specified entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate UPDATE for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the UPDATE
- **Returns:** a new SQLBuilder instance configured for UPDATE operation
##### deleteFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE SQL builder for the specified table with entity class context.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for column mapping metadata
- **Returns:** a new SQLBuilder instance configured for DELETE operation
- **Signature:** `public static SQLBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate DELETE for
- **Returns:** a new SQLBuilder instance configured for DELETE operation
##### select(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder select(final String selectPart)`
- **Summary:** Creates a SELECT SQL builder with a custom select expression.
- **Parameters:**
  - `selectPart` (`String`) — the custom select expression
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for the specified columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT SQL builder for the specified collection of columns.
- **Contract:**
  - This is useful when column names are dynamically determined.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT SQL builder with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of column names to their aliases
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT SQL builder for all properties of the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT for
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT SQL builder for the specified entity class with optional sub-entity inclusion.
- **Contract:**
  - When includeSubEntityProperties is true, properties of embedded entities are also selected.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT for
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for the specified entity class, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT SQL builder for the specified entity class with sub-entity inclusion control and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT for
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a SELECT SQL builder for multiple entity classes with table and result aliases.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the result set alias prefix for the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the result set alias prefix for the second entity
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT operation
- **Signature:** `public static SQLBuilder select(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a SELECT SQL builder for multiple entity classes with table aliases, result aliases, and property exclusion.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the result set alias prefix for the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the result set alias prefix for the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT operation
- **Signature:** `public static SQLBuilder select(final List<Selection> multiSelects)`
- **Summary:** Creates a SELECT SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT operation
##### selectFrom(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT...FROM SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM for
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias)`
- **Summary:** Creates a complete SELECT...FROM SQL builder for the specified entity class with table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM for
  - `alias` (`String`) — the table alias to use
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a complete SELECT...FROM SQL builder with sub-entity inclusion control.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM for
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a complete SELECT...FROM SQL builder with table alias and sub-entity inclusion control.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM for
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT...FROM SQL builder with property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT...FROM SQL builder with table alias and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM for
  - `alias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT...FROM SQL builder with sub-entity inclusion control and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM for
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClass, final String alias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT...FROM SQL builder with full control over alias, sub-entity inclusion, and property exclusion.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate SELECT FROM for
  - `alias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of sub-entities
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the SELECT
- **Returns:** a new SQLBuilder instance configured for SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB)`
- **Summary:** Creates a complete SELECT...FROM SQL builder for multiple entity classes.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the result set alias prefix for the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the result set alias prefix for the second entity
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final Class<?> entityClassA, final String tableAliasA, final String classAliasA, final Set<String> excludedPropNamesA, final Class<?> entityClassB, final String tableAliasB, final String classAliasB, final Set<String> excludedPropNamesB)`
- **Summary:** Creates a complete SELECT...FROM SQL builder for multiple entity classes with property exclusion.
- **Parameters:**
  - `entityClassA` (`Class<?>`) — the first entity class
  - `tableAliasA` (`String`) — the table alias for the first entity
  - `classAliasA` (`String`) — the result set alias prefix for the first entity
  - `excludedPropNamesA` (`Set<String>`) — properties to exclude from the first entity
  - `entityClassB` (`Class<?>`) — the second entity class
  - `tableAliasB` (`String`) — the table alias for the second entity
  - `classAliasB` (`String`) — the result set alias prefix for the second entity
  - `excludedPropNamesB` (`Set<String>`) — properties to exclude from the second entity
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT operation with FROM clause
- **Signature:** `public static SQLBuilder selectFrom(final List<Selection> multiSelects)`
- **Summary:** Creates a complete SELECT...FROM SQL builder for multiple entity selections.
- **Parameters:**
  - `multiSelects` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SQLBuilder instance configured for multi-entity SELECT operation with FROM clause
##### count(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) SQL builder for the specified table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to count rows from
- **Returns:** a new SQLBuilder instance configured for COUNT operation
- **Signature:** `public static SQLBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) SQL builder for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count rows for
- **Returns:** a new SQLBuilder instance configured for COUNT operation
##### parse(...) -> SQLBuilder
- **Signature:** `public static SQLBuilder parse(final Condition cond, final Class<?> entityClass)`
- **Summary:** Parses a condition into SQL format for the specified entity class.
- **Parameters:**
  - `cond` (`Condition`) — the condition to parse into SQL
  - `entityClass` (`Class<?>`) — the entity class for column mapping metadata
- **Returns:** a new SQLBuilder instance containing the parsed condition

#### Public Instance Methods
- (none)

### Class SQLMapper (com.landawn.abacus.query.SQLMapper)
A utility class for managing SQL scripts stored in XML files and mapping them to short identifiers.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### fromFile(...) -> SQLMapper
- **Signature:** `public static SQLMapper fromFile(final String filePath)`
- **Summary:** Creates a SQLMapper instance by loading SQL definitions from one or more XML files.
- **Parameters:**
  - `filePath` (`String`) — one or more file paths separated by ',' or ';'
- **Returns:** a new SQLMapper instance loaded with SQL definitions from the specified files

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public SQLMapper()`
- **Summary:** Creates an empty SQLMapper instance.
- **Parameters:**
  - (none)
##### keySet(...) -> Set<String>
- **Signature:** `public Set<String> keySet()`
- **Summary:** Returns a set of all SQL identifiers registered in this mapper.
- **Parameters:**
  - (none)
- **Returns:** a set view of all SQL identifiers in this mapper, maintaining insertion order
##### get(...) -> ParsedSql
- **Signature:** `public ParsedSql get(final String id)`
- **Summary:** Retrieves the parsed SQL associated with the specified identifier.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code SQLMapper mapper = SQLMapper.fromFile("sql/queries.xml"); ParsedSql sql = mapper.get("findAccountById"); if (sql != null) { String parameterizedSql = sql.getParameterizedSql(); // Use with PreparedStatement PreparedStatement stmt = connection.prepareStatement(parameterizedSql); } // Returns null for unknown ids ParsedSql unknown = mapper.get("nonExistentId"); // unknown is null } </pre>
- **Parameters:**
  - `id` (`String`) — the SQL identifier to look up
- **Returns:** the ParsedSql object, or {@code null} if the id is empty, exceeds {@link #MAX_ID_LENGTH} , or not found
##### getAttrs(...) -> ImmutableMap<String, String>
- **Signature:** `public ImmutableMap<String, String> getAttrs(final String id)`
- **Summary:** Retrieves the attributes associated with the specified SQL identifier.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Given XML: <sql id="batchInsert" batchSize="100" timeout="30">...</sql> SQLMapper mapper = SQLMapper.fromFile("sql/queries.xml"); ImmutableMap<String, String> attrs = mapper.getAttrs("batchInsert"); if (attrs != null) { String batchSize = attrs.get("batchSize"); // "100" String timeout = attrs.get("timeout"); // "30" } // Returns null for unknown ids ImmutableMap<String, String> unknown = mapper.getAttrs("nonExistentId"); // unknown is null } </pre>
- **Parameters:**
  - `id` (`String`) — the SQL identifier to look up
- **Returns:** an immutable map of attribute names to values, or {@code null} if the id is empty, exceeds {@link #MAX_ID_LENGTH} , or not found
##### add(...) -> ParsedSql
- **Signature:** `public ParsedSql add(final String id, final ParsedSql sql)`
- **Summary:** Adds a parsed SQL with the specified identifier.
- **Contract:**
  - This method validates the ID and throws an exception if an SQL with the same ID already exists.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
  - `sql` (`ParsedSql`) — the parsed SQL to associate with the identifier
- **Returns:** always {@code null} since duplicate IDs are not allowed (the method throws if the ID already exists)
- **Signature:** `public void add(final String id, final String sql, final Map<String, String> attrs)`
- **Summary:** Adds a SQL string with the specified identifier and attributes.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
  - `sql` (`String`) — the SQL string to parse and store
  - `attrs` (`Map<String, String>`) — additional attributes for the SQL (e.g., batchSize, fetchSize, resultSetType, timeout); may be null or empty
##### remove(...) -> void
- **Signature:** `public void remove(final String id)`
- **Summary:** Removes the SQL and its attributes associated with the specified identifier.
- **Contract:**
  - If the id is empty, exceeds {@link #MAX_ID_LENGTH} , or not found, this method does nothing.
- **Parameters:**
  - `id` (`String`) — the SQL identifier to remove
##### copy(...) -> SQLMapper
- **Signature:** `public SQLMapper copy()`
- **Summary:** Creates a shallow copy of this SQLMapper instance.
- **Parameters:**
  - (none)
- **Returns:** a new SQLMapper instance with the same SQL definitions and attributes
##### saveTo(...) -> void
- **Signature:** `@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE") public void saveTo(final File file)`
- **Summary:** Saves all SQL definitions in this mapper to an XML file.
- **Contract:**
  - If the file already exists, it will be overwritten.
- **Parameters:**
  - `file` (`File`) — the file to write to (will be created if it doesn't exist; parent directories must exist)
##### isEmpty(...) -> boolean
- **Signature:** `public boolean isEmpty()`
- **Summary:** Checks if this mapper contains no SQL definitions.
- **Contract:**
  - Checks if this mapper contains no SQL definitions.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if the mapper contains no SQL definitions, {@code false} otherwise
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code value for this SQLMapper.
- **Parameters:**
  - (none)
- **Returns:** the hash code value
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this SQLMapper to another object for equality.
- **Contract:**
  - Two SQLMappers are considered equal if they contain the same SQL definitions.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this SQLMapper.
- **Parameters:**
  - (none)
- **Returns:** a string representation of the SQL map

### Enum SQLOperation (com.landawn.abacus.query.SQLOperation)
Enumeration representing SQL operation types.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### of(...) -> SQLOperation
- **Signature:** `public static SQLOperation of(final String name)`
- **Summary:** Retrieves the SQLOperation enum value corresponding to the given operation name.
- **Parameters:**
  - `name` (`String`) — the SQL operation name to look up (case-sensitive)
- **Returns:** the corresponding SQLOperation enum value, or {@code null} if no matching operation is found

#### Public Instance Methods
##### sqlText(...) -> String
- **Signature:** `public String sqlText()`
- **Summary:** Returns the SQL text representation of this operation.
- **Parameters:**
  - (none)
- **Returns:** the SQL keyword string representation of this operation, never {@code null}
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns the string representation of this SQL operation.
- **Parameters:**
  - (none)
- **Returns:** the operation name as a string

### Class SQLParser (com.landawn.abacus.query.SQLParser)
A utility class for parsing SQL statements into individual words and tokens.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### parse(...) -> List<String>
- **Signature:** `public static List<String> parse(final String sql)`
- **Summary:** Parses a SQL statement into a list of individual words and tokens.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to parse
- **Returns:** a list of tokens representing the parsed SQL statement
##### indexWord(...) -> int
- **Signature:** `public static int indexWord(final String sql, final String word, final int fromIndex, final boolean caseSensitive)`
- **Summary:** Finds the index of a specific word within a SQL statement starting from a given position.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search within
  - `word` (`String`) — the word or composite keyword to find
  - `fromIndex` (`int`) — the starting position for the search (0-based)
  - `caseSensitive` (`boolean`) — whether the search should be case-sensitive
- **Returns:** the index of the word if found, or -1 if not found
##### nextWord(...) -> String
- **Signature:** `public static String nextWord(final String sql, final int fromIndex)`
- **Summary:** Extracts the next word or token from a SQL statement starting at the specified index.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to extract the word from
  - `fromIndex` (`int`) — the starting position for extraction (0-based)
- **Returns:** the next word or token found, or an empty string if no more tokens exist
##### registerSeparator(...) -> void
- **Signature:** `public static void registerSeparator(final char separator)`
- **Summary:** Registers a single character as a SQL separator.
- **Parameters:**
  - `separator` (`char`) — the character to register as a separator
- **Signature:** `public static void registerSeparator(final String separator)`
- **Summary:** Registers a string as a SQL separator.
- **Contract:**
  - This can be used to register multi-character operators or separators that should be recognized as single tokens during parsing.
  - <p> If the separator is a single character, it will also be registered as a character separator for efficiency.
- **Parameters:**
  - `separator` (`String`) — the string to register as a separator (must not be null)
##### isSeparator(...) -> boolean
- **Signature:** `public static boolean isSeparator(final String str, final int len, final int index, final char ch)`
- **Summary:** Checks if a character at a specific position in a SQL string is a separator.
- **Contract:**
  - Checks if a character at a specific position in a SQL string is a separator.
- **Parameters:**
  - `str` (`String`) — the SQL string being parsed
  - `len` (`int`) — the length of the SQL string
  - `index` (`int`) — the current position in the string
  - `ch` (`char`) — the character to check
- **Returns:** {@code true} if the character is a separator in this context, {@code false} otherwise
##### isFunctionName(...) -> boolean
- **Signature:** `public static boolean isFunctionName(final List<String> words, final int len, final int index)`
- **Summary:** Determines if a word at a specific position in a parsed word list represents a function name.
- **Contract:**
  - Determines if a word at a specific position in a parsed word list represents a function name.
  - A word is considered a function name if it is followed by an opening parenthesis, either immediately or after whitespace.
- **Parameters:**
  - `words` (`List<String>`) — the list of parsed SQL words/tokens
  - `len` (`int`) — the total length of the words list
  - `index` (`int`) — the index of the word to check
- **Returns:** {@code true} if the word at the specified index is a function name, {@code false} otherwise

#### Public Instance Methods
- (none)

### Class Selection (com.landawn.abacus.query.Selection)
Represents a selection specification for SQL queries, particularly useful for complex multi-table selections.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### multiSelectionBuilder(...) -> MultiSelectionBuilder
- **Signature:** `public static MultiSelectionBuilder multiSelectionBuilder()`
- **Summary:** Creates a new MultiSelectionBuilder for building complex multi-table selections.
- **Parameters:**
  - (none)
- **Returns:** a new MultiSelectionBuilder instance for constructing multi-table selections

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Selection()`
- **Summary:** Creates a new empty Selection instance.
- **Parameters:**
  - (none)

### Class MultiSelectionBuilder (com.landawn.abacus.query.Selection.MultiSelectionBuilder)
Builder class for creating multiple Selection objects in a fluent manner.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### add(...) -> MultiSelectionBuilder
- **Signature:** `public MultiSelectionBuilder add(final Class<?> entityClass)`
- **Summary:** Adds a simple selection for the specified entity class with default settings.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** this builder instance for method chaining
- **Signature:** `public MultiSelectionBuilder add(final Class<?> entityClass, final Collection<String> selectPropNames)`
- **Summary:** Adds a selection for the specified entity class with specific properties to select.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `selectPropNames` (`Collection<String>`) — the property names to include in the selection
- **Returns:** this builder instance for method chaining
- **Signature:** `public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias)`
- **Summary:** Adds a selection for the specified entity class with table and class aliases.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the alias to use for the table in SQL
  - `classAlias` (`String`) — the alias to use for result mapping
- **Returns:** this builder instance for method chaining
- **Signature:** `public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias, final Collection<String> selectPropNames)`
- **Summary:** Adds a selection for the specified entity class with full configuration options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the alias to use for the table in SQL (can be null)
  - `classAlias` (`String`) — the alias to use for result mapping (can be null)
  - `selectPropNames` (`Collection<String>`) — the property names to include in the selection (null means all)
- **Returns:** this builder instance for method chaining
- **Signature:** `public MultiSelectionBuilder add(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Adds a selection with sub-entity property control and exclusion options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from sub-entities
  - `excludedPropNames` (`Set<String>`) — property names to exclude from the selection
- **Returns:** this builder instance for method chaining
- **Signature:** `public MultiSelectionBuilder add(final Class<?> entityClass, final String tableAlias, final String classAlias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Adds a selection with full configuration including sub-entity and exclusion options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the alias to use for the table in SQL (can be null)
  - `classAlias` (`String`) — the alias to use for result mapping (can be null)
  - `includeSubEntityProperties` (`boolean`) — whether to include properties from sub-entities
  - `excludedPropNames` (`Set<String>`) — property names to exclude from the selection
- **Returns:** this builder instance for method chaining
##### build(...) -> List<Selection>
- **Signature:** `public List<Selection> build()`
- **Summary:** Builds and returns the list of Selection objects configured in this builder.
- **Parameters:**
  - (none)
- **Returns:** a list of Selection objects
##### apply(...) -> SQLBuilder
- **Signature:** `@Beta public SQLBuilder apply(final Function<? super List<Selection>, SQLBuilder> func)`
- **Summary:** Applies the built selections to the provided SQLBuilder function and returns the resulting SQLBuilder.
- **Parameters:**
  - `func` (`Function<? super List<Selection>, SQLBuilder>`) — the function to apply the selections to (e.g., PSC::select, NSC::selectFrom)
- **Returns:** the SQLBuilder instance returned by the function
- **See also:** PSC#select(List), PSC#selectFrom(List), NSC#select(List), NSC#selectFrom(List)

### Enum SortDirection (com.landawn.abacus.query.SortDirection)
Enumeration representing the sort direction for database queries and collections.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### isAscending(...) -> boolean
- **Signature:** `public boolean isAscending()`
- **Summary:** Checks if this sort direction is ascending.
- **Contract:**
  - Checks if this sort direction is ascending.
  - This is a convenience method equivalent to checking if the direction equals ASC.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code SortDirection direction = SortDirection.ASC; boolean ascending = direction.isAscending(); // true SortDirection descDirection = SortDirection.DESC; boolean descAscending = descDirection.isAscending(); // false // Conditional logic based on sort direction if (direction.isAscending()) { // Apply ascending sort logic } else { // Apply descending sort logic } } </pre>
- **Parameters:**
  - (none)
- **Returns:** {@code true} if this sort direction is ASC, {@code false} if it is DESC

## com.landawn.abacus.query.condition
### Class AbstractCondition (com.landawn.abacus.query.condition.AbstractCondition)
Abstract base class for all condition implementations.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### getOperator(...) -> Operator
- **Signature:** `@Override public Operator getOperator()`
- **Summary:** Gets the operator for this condition.
- **Parameters:**
  - (none)
- **Returns:** the operator for this condition
##### and(...) -> And
- **Signature:** `@Override public And and(final Condition condition)`
- **Summary:** Creates a new AND condition combining this condition with another.
- **Contract:**
  - Both conditions must be true for the AND condition to be true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to AND with this condition (must not be null)
- **Returns:** a new And condition containing both conditions
##### or(...) -> Or
- **Signature:** `@Override public Or or(final Condition condition)`
- **Summary:** Creates a new OR condition combining this condition with another.
- **Contract:**
  - At least one condition must be true for the OR condition to be true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to OR with this condition
- **Returns:** a new Or condition containing both conditions
##### not(...) -> Not
- **Signature:** `@Override public Not not()`
- **Summary:** Creates a new NOT condition that negates this condition.
- **Contract:**
  - The NOT condition is true when this condition is false, and vice versa.
- **Parameters:**
  - (none)
- **Returns:** a new Not condition wrapping this condition
##### copy(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") @Override public <T extends Condition> T copy()`
- **Summary:** Creates a shallow copy of this condition using object cloning.
- **Contract:**
  - Subclasses should override this method to provide deep copying of their specific fields to ensure complete independence between copies.
- **Parameters:**
  - (none)
- **Returns:** a shallow copy of this condition
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this condition using the default naming policy.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this condition

### Class All (com.landawn.abacus.query.condition.All)
Represents the SQL ALL operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public All(final SubQuery condition)`
- **Summary:** Creates a new ALL condition with the specified subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery that returns values to compare against. Must not be null.
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`)
- **Returns:** unspecified

### Class And (com.landawn.abacus.query.condition.And)
Represents a logical AND condition that combines multiple conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public And(final Condition... conditions)`
- **Summary:** Creates a new AND condition with the specified conditions.
- **Contract:**
  - All provided conditions must be true for this AND condition to evaluate to true.
- **Parameters:**
  - `conditions` (`Condition[]`) — the conditions to combine with AND logic
- **Signature:** `public And(final Collection<? extends Condition> conditions)`
- **Summary:** Creates a new AND condition with the specified collection of conditions.
- **Contract:**
  - All conditions in the collection must be true for this AND condition to evaluate to true.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Build conditions dynamically List<Condition> conditions = new ArrayList<>(); conditions.add(Filters.eq("status", "active")); conditions.add(Filters.isNotNull("email")); if (includeAgeFilter) { conditions.add(Filters.gt("age", 21)); } And and = new And(conditions); // Results in dynamic AND condition based on the list } </pre>
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with AND logic
##### and(...) -> And
- **Signature:** `@Override public And and(final Condition condition)`
- **Summary:** Creates a new AND condition by adding another condition to this AND.
- **Parameters:**
  - `condition` (`Condition`) — the condition to add to this AND. Must not be null.
- **Returns:** a new AND condition containing all existing conditions plus the new one

### Class Any (com.landawn.abacus.query.condition.Any)
Represents the SQL ANY operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Any(final SubQuery condition)`
- **Summary:** Creates a new ANY condition with the specified subquery.
- **Contract:**
  - The ANY operator is used in conjunction with comparison operators to test if the comparison is true for any value returned by the subquery.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery that returns values to compare against. Must not be null.
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`)
- **Returns:** unspecified

### Class Between (com.landawn.abacus.query.condition.Between)
Represents a BETWEEN condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Between(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a new BETWEEN condition.
- **Contract:**
  - The condition checks if the property value falls within the specified range, inclusive.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check if age is between 18 and 65 (inclusive) Between ageRange = new Between("age", 18, 65); // Check if salary is within a range Between salaryRange = new Between("salary", 50000, 100000); // Check if date is in current year Between currentYear = new Between("createdDate", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)); // Use with subqueries for dynamic ranges SubQuery avgMinus10 = Filters.subQuery("SELECT AVG(score) - 10 FROM scores"); SubQuery avgPlus10 = Filters.subQuery("SELECT AVG(score) + 10 FROM scores"); Between nearAverage = new Between("score", avgMinus10, avgPlus10); } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `minValue` (`Object`) — the minimum value (inclusive) (can be null, literal value, or subquery)
  - `maxValue` (`Object`) — the maximum value (inclusive) (can be null, literal value, or subquery)
##### getPropName(...) -> String
- **Signature:** `public String getPropName()`
- **Summary:** Gets the property name being checked.
- **Parameters:**
  - (none)
- **Returns:** the property name
##### getMinValue(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T> T getMinValue()`
- **Summary:** Gets the minimum value of the range.
- **Parameters:**
  - (none)
- **Returns:** the minimum value (inclusive)
##### setMinValue(...) -> void
- **Signature:** `@Deprecated public void setMinValue(final Object minValue)`
- **Summary:** Sets the minimum value of the range.
- **Contract:**
  - This method should generally not be used as conditions should be immutable.
- **Parameters:**
  - `minValue` (`Object`) — the new minimum value
##### getMaxValue(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T> T getMaxValue()`
- **Summary:** Gets the maximum value of the range.
- **Parameters:**
  - (none)
- **Returns:** the maximum value (inclusive)
##### setMaxValue(...) -> void
- **Signature:** `@Deprecated public void setMaxValue(final Object maxValue)`
- **Summary:** Sets the maximum value of the range.
- **Contract:**
  - This method should generally not be used as conditions should be immutable.
- **Parameters:**
  - `maxValue` (`Object`) — the new maximum value
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the parameters for this BETWEEN condition.
- **Contract:**
  - If either value is a Condition (subquery), its parameters are included instead.
- **Parameters:**
  - (none)
- **Returns:** a list containing \[minValue, maxValue\] or their parameters if they are Conditions
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears all parameter values by setting them to null to free memory.
- **Contract:**
  - If min/max values are themselves conditions (like subqueries), their parameters are cleared.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") @Override public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this BETWEEN condition.
- **Contract:**
  - If minValue or maxValue are Conditions, they are also copied to ensure complete independence.
- **Parameters:**
  - (none)
- **Returns:** a new Between instance with copied values
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this Between condition to its string representation using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name
- **Returns:** a string representation like "propertyName BETWEEN minValue AND maxValue"
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this BETWEEN condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name, operator, and range values
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this BETWEEN condition is equal to another object.
- **Contract:**
  - Checks if this BETWEEN condition is equal to another object.
  - Two BETWEEN conditions are equal if they have the same property name, operator, minValue, and maxValue.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

### Class Binary (com.landawn.abacus.query.condition.Binary)
Abstract base class for binary conditions that compare a property with a value.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Binary(final String propName, final Operator operator, final Object propValue)`
- **Summary:** Creates a new Binary condition.
- **Parameters:**
  - `propName` (`String`) — the property name to compare (must not be null or empty)
  - `operator` (`Operator`) — the comparison operator
  - `propValue` (`Object`) — the value to compare against (can be a literal or Condition)
##### getPropName(...) -> String
- **Signature:** `public String getPropName()`
- **Summary:** Gets the property name being compared.
- **Parameters:**
  - (none)
- **Returns:** the property name
##### getPropValue(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T> T getPropValue()`
- **Summary:** Gets the value being compared against.
- **Parameters:**
  - (none)
- **Returns:** the property value, cast to the requested type
##### setPropValue(...) -> void
- **Signature:** `@Deprecated public void setPropValue(final Object propValue)`
- **Summary:** Sets the value being compared against.
- **Contract:**
  - This method should generally not be used as conditions should be immutable.
- **Parameters:**
  - `propValue` (`Object`) — the new property value
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the parameters for this condition.
- **Contract:**
  - If the value is a Condition (subquery), returns its parameters.
- **Parameters:**
  - (none)
- **Returns:** a list of parameter values
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears the parameter value by setting it to null to free memory.
- **Contract:**
  - If the value is a nested Condition, delegates to that condition's clearParameters() method.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") @Override public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this Binary condition.
- **Contract:**
  - If the value is a Condition, it is also copied to ensure complete independence.
- **Parameters:**
  - (none)
- **Returns:** a new Binary instance with copied values
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this Binary condition to its string representation using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name
- **Returns:** a string representation of this condition
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this Binary condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name, operator, and value
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this Binary condition is equal to another object.
- **Contract:**
  - Checks if this Binary condition is equal to another object.
  - Two Binary conditions are equal if they have the same property name, operator, and value.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

### Class Cell (com.landawn.abacus.query.condition.Cell)
Represents a condition cell that wraps another condition with an operator.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Cell(final Operator operator, final Condition condition)`
- **Summary:** Creates a new Cell with the specified operator and condition.
- **Parameters:**
  - `operator` (`Operator`) — the operator to apply to the condition
  - `condition` (`Condition`) — the condition to wrap (must not be null)
##### getCondition(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T extends Condition> T getCondition()`
- **Summary:** Gets the wrapped condition.
- **Contract:**
  - The returned condition can be cast to its specific type if needed.
- **Parameters:**
  - (none)
- **Returns:** the wrapped condition, cast to the specified type
##### setCondition(...) -> void
- **Signature:** `@Deprecated public void setCondition(final Condition condition)`
- **Summary:** Sets the wrapped condition.
- **Contract:**
  - This method should generally not be used as conditions should be immutable.
- **Parameters:**
  - `condition` (`Condition`) — the new condition to wrap
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the parameters from the wrapped condition.
- **Parameters:**
  - (none)
- **Returns:** a list of parameters from the wrapped condition, or an empty list if no condition is set
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears all parameter values by setting them to null to free memory.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") @Override public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this Cell.
- **Contract:**
  - The wrapped condition is also copied if present, ensuring complete independence between the original and the copy.
- **Parameters:**
  - (none)
- **Returns:** a new Cell instance with copied values
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this Cell condition to its string representation using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names
- **Returns:** a string representation of this Cell
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this Cell.
- **Parameters:**
  - (none)
- **Returns:** hash code based on operator and wrapped condition
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this Cell is equal to another object.
- **Contract:**
  - Checks if this Cell is equal to another object.
  - Two Cells are equal if they have the same operator and wrapped condition.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

### Class Clause (com.landawn.abacus.query.condition.Clause)
Abstract base class for SQL clause conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### and(...) -> And
- **Signature:** `@Override public And and(final Condition condition) throws UnsupportedOperationException`
- **Summary:** This operation is not supported for Clause objects.
- **Parameters:**
  - `condition` (`Condition`) — the condition to AND with (ignored)
- **Returns:** never returns normally
- **Throws:**
  - `java.lang.UnsupportedOperationException` — always thrown
##### or(...) -> Or
- **Signature:** `@Override public Or or(final Condition condition) throws UnsupportedOperationException`
- **Summary:** This operation is not supported for Clause objects.
- **Contract:**
  - <p> Clauses are structural components of SQL that must maintain their independence.
- **Parameters:**
  - `condition` (`Condition`) — the condition to OR with (ignored)
- **Returns:** never returns normally
- **Throws:**
  - `java.lang.UnsupportedOperationException` — always thrown
##### not(...) -> Not
- **Signature:** `@Override public Not not() throws UnsupportedOperationException`
- **Summary:** This operation is not supported for Clause objects.
- **Parameters:**
  - (none)
- **Returns:** never returns normally
- **Throws:**
  - `java.lang.UnsupportedOperationException` — always thrown

### Interface Condition (com.landawn.abacus.query.condition.Condition)
The base interface for all query conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### getOperator(...) -> Operator
- **Signature:** `Operator getOperator()`
- **Summary:** Gets the operator associated with this condition.
- **Parameters:**
  - (none)
- **Returns:** the operator for this condition
##### and(...) -> And
- **Signature:** `And and(Condition condition)`
- **Summary:** Creates a new AND condition combining this condition with another.
- **Contract:**
  - Both conditions must be true for the result to be true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to AND with this condition (must not be null)
- **Returns:** a new And condition containing both conditions
##### or(...) -> Or
- **Signature:** `Or or(Condition condition)`
- **Summary:** Creates a new OR condition combining this condition with another.
- **Parameters:**
  - `condition` (`Condition`) — the condition to OR with this condition (must not be null)
- **Returns:** a new Or condition containing both conditions
##### not(...) -> Not
- **Signature:** `Not not()`
- **Summary:** Creates a new NOT condition that negates this condition.
- **Contract:**
  - The result is true when this condition is false, and vice versa.
- **Parameters:**
  - (none)
- **Returns:** a new Not condition wrapping this condition
##### copy(...) -> T
- **Signature:** `<T extends Condition> T copy()`
- **Summary:** Creates a copy of this condition.
- **Contract:**
  - Implementations should ensure copied instances are safe to use independently for query construction.
- **Parameters:**
  - (none)
- **Returns:** a copy of this condition
##### getParameters(...) -> List<Object>
- **Signature:** `List<Object> getParameters()`
- **Summary:** Gets the list of parameter values associated with this condition.
- **Parameters:**
  - (none)
- **Returns:** a list of parameter values, never null
##### clearParameters(...) -> void
- **Signature:** `void clearParameters()`
- **Summary:** Clears all parameter values by setting them to null to free memory.
- **Contract:**
  - Use this method to release large objects when the condition is no longer needed.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code Condition eq = Filters.eq("name", "John"); List<Object> params = eq.getParameters(); // \["John"\] // Release parameter memory when the condition is no longer needed eq.clearParameters(); List<Object> cleared = eq.getParameters(); // \[null\] // For compound conditions, clears parameters recursively Condition combined = Filters.and(Filters.gt("age", 18), Filters.eq("status", "active")); combined.clearParameters(); // Clears parameters in both child conditions } </pre>
- **Parameters:**
  - (none)
##### toString(...) -> String
- **Signature:** `String toString(NamingPolicy namingPolicy)`
- **Summary:** Returns a string representation of this condition using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the policy for formatting property names
- **Returns:** a string representation of this condition

### Class Criteria (com.landawn.abacus.query.condition.Criteria)
Represents a complete query criteria that can contain multiple SQL clauses.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Criteria()`
- **Summary:** Creates a new empty Criteria instance.
- **Parameters:**
  - (none)
##### preselect(...) -> String
- **Signature:** `public String preselect()`
- **Summary:** Gets the preselect modifier (e.g., DISTINCT, DISTINCTROW).
- **Parameters:**
  - (none)
- **Returns:** the preselect modifier, or null if not set
- **Signature:** `public Criteria preselect(final String preselect)`
- **Summary:** Sets a custom preselect modifier.
- **Parameters:**
  - `preselect` (`String`) — the custom preselect modifier
- **Returns:** this Criteria instance for method chaining
##### getJoins(...) -> List<Join>
- **Signature:** `public List<Join> getJoins()`
- **Summary:** Gets all JOIN clauses in this criteria.
- **Parameters:**
  - (none)
- **Returns:** a list of Join conditions, empty list if none exist
##### getWhere(...) -> Cell
- **Signature:** `public Cell getWhere()`
- **Summary:** Gets the WHERE clause from this criteria.
- **Contract:**
  - Returns null if no WHERE clause has been set.
- **Parameters:**
  - (none)
- **Returns:** the Where condition, or null if not set
##### getGroupBy(...) -> Cell
- **Signature:** `public Cell getGroupBy()`
- **Summary:** Gets the GROUP BY clause from this criteria.
- **Contract:**
  - Returns null if no GROUP BY clause has been set.
- **Parameters:**
  - (none)
- **Returns:** the GroupBy condition, or null if not set
##### getHaving(...) -> Cell
- **Signature:** `public Cell getHaving()`
- **Summary:** Gets the HAVING clause from this criteria.
- **Contract:**
  - Returns null if no HAVING clause has been set.
- **Parameters:**
  - (none)
- **Returns:** the Having condition, or null if not set
##### getAggregation(...) -> List<Cell>
- **Signature:** `public List<Cell> getAggregation()`
- **Summary:** Gets all aggregation operations (UNION, UNION ALL, INTERSECT, EXCEPT, MINUS).
- **Parameters:**
  - (none)
- **Returns:** a list of aggregation conditions, empty if none exist
##### getOrderBy(...) -> Cell
- **Signature:** `public Cell getOrderBy()`
- **Summary:** Gets the ORDER BY clause from this criteria.
- **Contract:**
  - Returns null if no ORDER BY clause has been set.
- **Parameters:**
  - (none)
- **Returns:** the OrderBy condition, or null if not set
##### getLimit(...) -> Limit
- **Signature:** `public Limit getLimit()`
- **Summary:** Gets the LIMIT clause from this criteria.
- **Contract:**
  - Returns null if no LIMIT clause has been set.
- **Parameters:**
  - (none)
- **Returns:** the Limit condition, or null if not set
##### getConditions(...) -> List<Condition>
- **Signature:** `public List<Condition> getConditions()`
- **Summary:** Gets all conditions in this criteria.
- **Parameters:**
  - (none)
- **Returns:** a list of all conditions
##### get(...) -> List<Condition>
- **Signature:** `public List<Condition> get(final Operator operator)`
- **Summary:** Gets all conditions with the specified operator.
- **Parameters:**
  - `operator` (`Operator`) — the operator to filter by (must not be null)
- **Returns:** a list of conditions with the specified operator, empty list if none found
##### clear(...) -> void
- **Signature:** `public void clear()`
- **Summary:** Clears all conditions from this criteria.
- **Parameters:**
  - (none)
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets all parameters from all conditions in the proper order.
- **Parameters:**
  - (none)
- **Returns:** a list of all parameters from all conditions
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears all parameter values by setting them to null to free memory.
- **Contract:**
  - Use this method to release large objects when the condition is no longer needed.
- **Parameters:**
  - (none)
##### distinct(...) -> Criteria
- **Signature:** `public Criteria distinct()`
- **Summary:** Sets the DISTINCT modifier for the query.
- **Parameters:**
  - (none)
- **Returns:** this Criteria instance for method chaining
##### distinctBy(...) -> Criteria
- **Signature:** `public Criteria distinctBy(final String columnNames)`
- **Summary:** Sets the DISTINCT modifier with specific columns.
- **Parameters:**
  - `columnNames` (`String`) — the columns to apply DISTINCT to
- **Returns:** this Criteria instance for method chaining
##### distinctRow(...) -> Criteria
- **Signature:** `public Criteria distinctRow()`
- **Summary:** Sets the DISTINCTROW modifier for the query.
- **Parameters:**
  - (none)
- **Returns:** this Criteria instance for method chaining
##### distinctRowBy(...) -> Criteria
- **Signature:** `public Criteria distinctRowBy(final String columnNames)`
- **Summary:** Sets the DISTINCTROW modifier with specific columns.
- **Parameters:**
  - `columnNames` (`String`) — the columns to apply DISTINCTROW to
- **Returns:** this Criteria instance for method chaining
##### join(...) -> Criteria
- **Signature:** `public final Criteria join(final Join... joins)`
- **Summary:** Adds JOIN clauses to this criteria.
- **Parameters:**
  - `joins` (`Join[]`) — the JOIN clauses to add
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria join(final Collection<Join> joins)`
- **Summary:** Adds JOIN clauses to this criteria.
- **Parameters:**
  - `joins` (`Collection<Join>`) — the collection of JOIN clauses to add
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria join(final String joinEntity)`
- **Summary:** Adds a simple INNER JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria join(final String joinEntity, final Condition condition)`
- **Summary:** Adds an INNER JOIN with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `condition` (`Condition`) — the join condition
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria join(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Adds an INNER JOIN with multiple entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `condition` (`Condition`) — the join condition
- **Returns:** this Criteria instance for method chaining
##### where(...) -> Criteria
- **Signature:** `public Criteria where(final Condition condition)`
- **Summary:** Sets or replaces the WHERE clause.
- **Contract:**
  - If a WHERE clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the WHERE condition
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria where(final String condition)`
- **Summary:** Sets or replaces the WHERE clause using a string expression.
- **Contract:**
  - If a WHERE clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`String`) — the WHERE condition as a string
- **Returns:** this Criteria instance for method chaining
##### groupBy(...) -> Criteria
- **Signature:** `public Criteria groupBy(final Condition condition)`
- **Summary:** Sets or replaces the GROUP BY clause.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the GROUP BY condition
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public final Criteria groupBy(final String... propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with property names.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to group by
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria groupBy(final String propName, final SortDirection direction)`
- **Summary:** Sets or replaces the GROUP BY clause with a property and sort direction.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the property name to group by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria groupBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2)`
- **Summary:** Sets or replaces the GROUP BY clause with two properties and their sort directions.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the first property name to group by
  - `direction` (`SortDirection`) — the sort direction for the first property
  - `propName2` (`String`) — the second property name to group by
  - `direction2` (`SortDirection`) — the sort direction for the second property
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria groupBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2, final String propName3, final SortDirection direction3)`
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
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria groupBy(final Collection<String> propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with multiple properties.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria groupBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Sets or replaces the GROUP BY clause with multiple properties and sort direction.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by
  - `direction` (`SortDirection`) — the sort direction for all properties
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria groupBy(final Map<String, SortDirection> orders)`
- **Summary:** Sets or replaces the GROUP BY clause with custom sort directions per property.
- **Contract:**
  - The map should be a LinkedHashMap to preserve order.
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — a map of property names to sort directions
- **Returns:** this Criteria instance for method chaining
##### having(...) -> Criteria
- **Signature:** `public Criteria having(final Condition condition)`
- **Summary:** Sets or replaces the HAVING clause.
- **Contract:**
  - If a HAVING clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the HAVING condition
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria having(final String condition)`
- **Summary:** Sets or replaces the HAVING clause using a string expression.
- **Contract:**
  - If a HAVING clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`String`) — the HAVING condition as a string
- **Returns:** this Criteria instance for method chaining
##### orderByAsc(...) -> Criteria
- **Signature:** `public Criteria orderByAsc(final String... propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with ascending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to order by ascending
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria orderByAsc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with ascending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by ascending
- **Returns:** this Criteria instance for method chaining
##### orderByDesc(...) -> Criteria
- **Signature:** `public Criteria orderByDesc(final String... propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with descending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to order by descending
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria orderByDesc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with descending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by descending
- **Returns:** this Criteria instance for method chaining
##### orderBy(...) -> Criteria
- **Signature:** `public Criteria orderBy(final Condition condition)`
- **Summary:** Sets or replaces the ORDER BY clause.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Complex ordering expression Criteria criteria = new Criteria() .orderBy(Filters.expr("CASE WHEN priority = 'HIGH' THEN 1 ELSE 2 END, created_date DESC")); } </pre>
- **Parameters:**
  - `condition` (`Condition`) — the ORDER BY condition
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public final Criteria orderBy(final String... propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with property names.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to order by
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria orderBy(final String propName, final SortDirection direction)`
- **Summary:** Sets or replaces the ORDER BY clause with a property and sort direction.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the property name to order by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria orderBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2)`
- **Summary:** Sets or replaces the ORDER BY clause with two properties and their sort directions.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the first property name to order by
  - `direction` (`SortDirection`) — the sort direction for the first property
  - `propName2` (`String`) — the second property name to order by
  - `direction2` (`SortDirection`) — the sort direction for the second property
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria orderBy(final String propName, final SortDirection direction, final String propName2, final SortDirection direction2, final String propName3, final SortDirection direction3)`
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
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria orderBy(final Collection<String> propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with multiple properties.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria orderBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Sets or replaces the ORDER BY clause with multiple properties and sort direction.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by
  - `direction` (`SortDirection`) — the sort direction for all properties
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Sets or replaces the ORDER BY clause with custom sort directions per property.
- **Contract:**
  - The map should be a LinkedHashMap to preserve order.
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — a map of property names to sort directions
- **Returns:** this Criteria instance for method chaining
##### limit(...) -> Criteria
- **Signature:** `public Criteria limit(final Limit condition)`
- **Summary:** Sets or replaces the LIMIT clause.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Limit`) — the LIMIT condition
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria limit(final int count)`
- **Summary:** Sets or replaces the LIMIT clause with a count.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `count` (`int`) — the maximum number of results to return
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria limit(final int offset, final int count)`
- **Summary:** Sets or replaces the LIMIT clause with offset and count.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip
  - `count` (`int`) — the maximum number of results to return
- **Returns:** this Criteria instance for method chaining
- **Signature:** `public Criteria limit(final String expr)`
- **Summary:** Sets or replaces the LIMIT clause using a string expression.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `expr` (`String`) — the LIMIT expression as a string
- **Returns:** this Criteria instance for method chaining
##### union(...) -> Criteria
- **Signature:** `public Criteria union(final SubQuery subQuery)`
- **Summary:** Adds a UNION operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with
- **Returns:** this Criteria instance for method chaining
##### unionAll(...) -> Criteria
- **Signature:** `public Criteria unionAll(final SubQuery subQuery)`
- **Summary:** Adds a UNION ALL operation with a subquery.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code SubQuery pendingOrders = Filters.subQuery("SELECT * FROM pending_orders"); Criteria criteria = new Criteria() .where(Filters.eq("status", "completed")) .unionAll(pendingOrders); // Returns all orders, including duplicates if any exist } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with
- **Returns:** this Criteria instance for method chaining
##### intersect(...) -> Criteria
- **Signature:** `public Criteria intersect(final SubQuery subQuery)`
- **Summary:** Adds an INTERSECT operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to intersect with
- **Returns:** this Criteria instance for method chaining
##### except(...) -> Criteria
- **Signature:** `public Criteria except(final SubQuery subQuery)`
- **Summary:** Adds an EXCEPT operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to except
- **Returns:** this Criteria instance for method chaining
##### minus(...) -> Criteria
- **Signature:** `public Criteria minus(final SubQuery subQuery)`
- **Summary:** Adds a MINUS operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to minus
- **Returns:** this Criteria instance for method chaining
##### copy(...) -> T
- **Signature:** `@Override @SuppressWarnings("unchecked") public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this Criteria.
- **Parameters:**
  - (none)
- **Returns:** a new Criteria instance with copied values
##### toString(...) -> String
- **Signature:** `@SuppressWarnings("StringConcatenationInLoop") @Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Returns a string representation of this Criteria using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names
- **Returns:** a string representation of this Criteria
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this Criteria.
- **Parameters:**
  - (none)
- **Returns:** the hash code value
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this Criteria is equal to another object.
- **Contract:**
  - Checks if this Criteria is equal to another object.
  - Two Criteria are equal if they have the same preselect modifier and conditions.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

### Class CriteriaUtil (com.landawn.abacus.query.condition.CriteriaUtil)
Utility class for working with Criteria and clause operators.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### getClauseOperators(...) -> Set<Operator>
- **Signature:** `public static Set<Operator> getClauseOperators()`
- **Summary:** Gets the set of all valid clause operators.
- **Parameters:**
  - (none)
- **Returns:** an immutable set of clause operators in proper SQL order
##### isClause(...) -> boolean
- **Signature:** `public static boolean isClause(final Operator operator)`
- **Summary:** Checks if the given operator is a valid clause operator.
- **Contract:**
  - Checks if the given operator is a valid clause operator.
- **Parameters:**
  - `operator` (`Operator`) — the operator to check
- **Returns:** {@code true} if the operator is a clause operator, {@code false} otherwise
- **Signature:** `public static boolean isClause(final String operator)`
- **Summary:** Checks if the given operator string represents a valid clause operator.
- **Contract:**
  - Checks if the given operator string represents a valid clause operator.
  - This method converts the string to an Operator and checks if it's a clause.
- **Parameters:**
  - `operator` (`String`) — the operator string to check
- **Returns:** {@code true} if the operator string represents a clause operator, {@code false} otherwise
- **Signature:** `public static boolean isClause(final Condition condition)`
- **Summary:** Checks if the given condition is a clause condition.
- **Contract:**
  - Checks if the given condition is a clause condition.
  - A condition is a clause if its operator is a clause operator.
- **Parameters:**
  - `condition` (`Condition`) — the condition to check
- **Returns:** {@code true} if the condition has a clause operator, {@code false} if null or not a clause
##### add(...) -> void
- **Signature:** `public static void add(final Criteria criteria, final Condition... conditions)`
- **Summary:** Adds conditions to the specified criteria.
- **Parameters:**
  - `criteria` (`Criteria`) — the criteria to add conditions to
  - `conditions` (`Condition[]`) — the conditions to add
- **Signature:** `public static void add(final Criteria criteria, final Collection<Condition> conditions)`
- **Summary:** Adds a collection of conditions to the specified criteria.
- **Parameters:**
  - `criteria` (`Criteria`) — the criteria to add conditions to
  - `conditions` (`Collection<Condition>`) — the collection of conditions to add
##### remove(...) -> void
- **Signature:** `public static void remove(final Criteria criteria, final Operator operator)`
- **Summary:** Removes all conditions with the specified operator from the criteria.
- **Parameters:**
  - `criteria` (`Criteria`) — the criteria to remove conditions from
  - `operator` (`Operator`) — the operator of conditions to remove
- **Signature:** `public static void remove(final Criteria criteria, final Condition... conditions)`
- **Summary:** Removes specific conditions from the criteria.
- **Parameters:**
  - `criteria` (`Criteria`) — the criteria to remove conditions from
  - `conditions` (`Condition[]`) — the conditions to remove
- **Signature:** `public static void remove(final Criteria criteria, final Collection<Condition> conditions)`
- **Summary:** Removes a collection of conditions from the criteria.
- **Parameters:**
  - `criteria` (`Criteria`) — the criteria to remove conditions from
  - `conditions` (`Collection<Condition>`) — the collection of conditions to remove

#### Public Instance Methods
- (none)

### Class CrossJoin (com.landawn.abacus.query.condition.CrossJoin)
Represents a CROSS JOIN operation in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public CrossJoin(final String joinEntity)`
- **Summary:** Creates a CROSS JOIN clause for the specified table or entity.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Simple cross join - all combinations CrossJoin join = new CrossJoin("colors"); // Generates: CROSS JOIN colors // If products has 10 rows and colors has 5 rows, result has 50 rows // Cross join with table alias CrossJoin aliasJoin = new CrossJoin("available_sizes s"); // Generates: CROSS JOIN available_sizes s } </pre>
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public CrossJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a CROSS JOIN clause with a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .
- **Signature:** `public CrossJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a CROSS JOIN clause with multiple tables/entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .

### Class Equal (com.landawn.abacus.query.condition.Equal)
Represents an equality (=) condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Equal(final String propName, final Object propValue)`
- **Summary:** Creates a new Equal condition.
- **Contract:**
  - The condition evaluates to true when the property value exactly matches the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `propValue` (`Object`) — the value to compare against (can be null, literal value, or subquery)

### Class Except (com.landawn.abacus.query.condition.Except)
Represents an EXCEPT set operation in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Except(final SubQuery subQuery)`
- **Summary:** Creates a new EXCEPT clause with the specified subquery.
- **Contract:**
  - Both queries must have the same number of columns with compatible data types.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Find employees who are not managers SubQuery managers = Filters.subQuery("SELECT employee_id FROM employees WHERE is_manager = true"); Except notManagers = new Except(managers); // When combined with all employees query: // SELECT employee_id FROM employees WHERE department = 'Sales' // EXCEPT // SELECT employee_id FROM employees WHERE is_manager = true // Returns Sales employees who are not managers // Find customers who haven't placed orders SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders"); Except customersWithoutOrders = new Except(customersWithOrders); // SELECT customer_id FROM customers // EXCEPT // SELECT DISTINCT customer_id FROM orders // Returns customers with no orders // Find skills not required for a specific job SubQuery requiredSkills = Filters.subQuery("SELECT skill_id FROM job_requirements WHERE job_id = 123"); Except otherSkills = new Except(requiredSkills); // SELECT skill_id FROM skills // EXCEPT // SELECT skill_id FROM job_requirements WHERE job_id = 123 // Returns all skills except those required for job 123 // Find products in inventory but never sold SubQuery soldProducts = Filters.subQuery("SELECT product_id FROM sales"); Except unsoldProducts = new Except(soldProducts); // SELECT product_id FROM inventory // EXCEPT // SELECT product_id FROM sales } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to perform the EXCEPT operation with. Must not be null. The subquery must have the same number of columns with compatible types as the main query.
- **See also:** Minus, Union, UnionAll, Intersect

### Class Exists (com.landawn.abacus.query.condition.Exists)
Represents the SQL EXISTS operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Exists(final SubQuery condition)`
- **Summary:** Creates a new EXISTS condition with the specified subquery.
- **Contract:**
  - The condition evaluates to true if the subquery returns at least one row.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check if employee has any subordinates SubQuery subordinatesQuery = Filters.subQuery( "SELECT 1 FROM employees e2 WHERE e2.manager_id = e1.id" ); Exists hasSubordinates = new Exists(subordinatesQuery); // Generates: EXISTS (SELECT 1 FROM employees e2 WHERE e2.manager_id = e1.id) // Check if product is in any active order SubQuery activeOrderQuery = Filters.subQuery( "SELECT 1 FROM order_items oi " + "JOIN orders o ON oi.order_id = o.id " + "WHERE oi.product_id = products.id " + "AND o.status = 'active'" ); Exists inActiveOrder = new Exists(activeOrderQuery); // Generates: EXISTS (SELECT 1 FROM order_items oi JOIN orders o ...) // Find users with specific permissions SubQuery permissionQuery = Filters.subQuery( "SELECT 1 FROM user_permissions up " + "WHERE up.user_id = users.id " + "AND up.permission = 'admin'" ); Exists isAdmin = new Exists(permissionQuery); // Generates: EXISTS (SELECT 1 FROM user_permissions up WHERE ...) // Find departments with employees SubQuery hasEmployees = Filters.subQuery("SELECT 1 FROM employees WHERE dept_id = departments.id"); Exists deptHasEmployees = new Exists(hasEmployees); // Generates: EXISTS (SELECT 1 FROM employees WHERE dept_id = departments.id) } </pre>
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery to check for existence of rows (must not be null)
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`)
- **Returns:** unspecified

### Class Expression (com.landawn.abacus.query.condition.Expression)
Represents a raw SQL expression that can be used in queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### of(...) -> Expression
- **Signature:** `public static Expression of(final String literal)`
- **Summary:** Creates or retrieves a cached Expression instance for the given literal.
- **Parameters:**
  - `literal` (`String`) — the SQL expression string
- **Returns:** a cached or new Expression instance
##### equal(...) -> String
- **Signature:** `public static String equal(final String literal, final Object value)`
- **Summary:** Creates an equality expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the equality
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the equality expression
##### eq(...) -> String
- **Signature:** `public static String eq(final String literal, final Object value)`
- **Summary:** Creates an equality expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the equality
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the equality expression
##### notEqual(...) -> String
- **Signature:** `public static String notEqual(final String literal, final Object value)`
- **Summary:** Creates a not-equal expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the inequality
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the not-equal expression
##### ne(...) -> String
- **Signature:** `public static String ne(final String literal, final Object value)`
- **Summary:** Creates a not-equal expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the inequality
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the not-equal expression
##### greaterThan(...) -> String
- **Signature:** `public static String greaterThan(final String literal, final Object value)`
- **Summary:** Creates a greater-than expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the greater-than expression
##### gt(...) -> String
- **Signature:** `public static String gt(final String literal, final Object value)`
- **Summary:** Creates a greater-than expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the greater-than expression
##### greaterEqual(...) -> String
- **Signature:** `public static String greaterEqual(final String literal, final Object value)`
- **Summary:** Creates a greater-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the greater-than-or-equal expression
##### ge(...) -> String
- **Signature:** `public static String ge(final String literal, final Object value)`
- **Summary:** Creates a greater-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the greater-than-or-equal expression
##### lessThan(...) -> String
- **Signature:** `public static String lessThan(final String literal, final Object value)`
- **Summary:** Creates a less-than expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the less-than expression
##### lt(...) -> String
- **Signature:** `public static String lt(final String literal, final Object value)`
- **Summary:** Creates a less-than expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the less-than expression
##### lessEqual(...) -> String
- **Signature:** `public static String lessEqual(final String literal, final Object value)`
- **Summary:** Creates a less-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the less-than-or-equal expression
##### le(...) -> String
- **Signature:** `public static String le(final String literal, final Object value)`
- **Summary:** Creates a less-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `literal` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value
- **Returns:** a string representation of the less-than-or-equal expression
##### between(...) -> String
- **Signature:** `public static String between(final String literal, final Object min, final Object max)`
- **Summary:** Creates a BETWEEN expression for a literal with min and max values.
- **Parameters:**
  - `literal` (`String`) — the literal to test
  - `min` (`Object`) — the minimum value (inclusive)
  - `max` (`Object`) — the maximum value (inclusive)
- **Returns:** a string representation of the BETWEEN expression
##### bt(...) -> String
- **Signature:** `@Deprecated public static String bt(final String literal, final Object min, final Object max)`
- **Summary:** Creates a BETWEEN expression for a literal with min and max values.
- **Parameters:**
  - `literal` (`String`) — the literal to test
  - `min` (`Object`) — the minimum value (inclusive)
  - `max` (`Object`) — the maximum value (inclusive)
- **Returns:** a string representation of the BETWEEN expression
##### like(...) -> String
- **Signature:** `public static String like(final String literal, final String value)`
- **Summary:** Creates a LIKE expression for pattern matching.
- **Parameters:**
  - `literal` (`String`) — the literal to match
  - `value` (`String`) — the pattern to match against (can include % and _ wildcards)
- **Returns:** a string representation of the LIKE expression
##### isNull(...) -> String
- **Signature:** `public static String isNull(final String literal)`
- **Summary:** Creates an IS NULL expression for the specified literal.
- **Parameters:**
  - `literal` (`String`) — the literal to check for null
- **Returns:** a string representation of the IS NULL expression
##### isNotNull(...) -> String
- **Signature:** `public static String isNotNull(final String literal)`
- **Summary:** Creates an IS NOT NULL expression for the specified literal.
- **Parameters:**
  - `literal` (`String`) — the literal to check for not null
- **Returns:** a string representation of the IS NOT NULL expression
##### isEmpty(...) -> String
- **Signature:** `public static String isEmpty(final String literal)`
- **Summary:** Creates an IS EMPTY expression for the specified literal.
- **Contract:**
  - This checks if a value is empty (blank).
- **Parameters:**
  - `literal` (`String`) — the literal to check for emptiness
- **Returns:** a string representation of the IS EMPTY expression
##### isNotEmpty(...) -> String
- **Signature:** `public static String isNotEmpty(final String literal)`
- **Summary:** Creates an IS NOT EMPTY expression for the specified literal.
- **Contract:**
  - This checks if a value is not empty (not blank).
- **Parameters:**
  - `literal` (`String`) — the literal to check for non-emptiness
- **Returns:** a string representation of the IS NOT EMPTY expression
##### and(...) -> String
- **Signature:** `public static String and(final String... literals)`
- **Summary:** Creates an AND expression combining multiple literals.
- **Contract:**
  - All conditions must be true for the AND expression to be true.
- **Parameters:**
  - `literals` (`String[]`) — the literals to combine with AND
- **Returns:** a string representation of the AND expression
##### or(...) -> String
- **Signature:** `public static String or(final String... literals)`
- **Summary:** Creates an OR expression combining multiple literals.
- **Contract:**
  - At least one condition must be true for the OR expression to be true.
- **Parameters:**
  - `literals` (`String[]`) — the literals to combine with OR
- **Returns:** a string representation of the OR expression
##### plus(...) -> String
- **Signature:** `public static String plus(final Object... objects)`
- **Summary:** Creates an addition expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values to add
- **Returns:** a string representation of the addition expression
##### minus(...) -> String
- **Signature:** `public static String minus(final Object... objects)`
- **Summary:** Creates a subtraction expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values to subtract
- **Returns:** a string representation of the subtraction expression
##### multi(...) -> String
- **Signature:** `public static String multi(final Object... objects)`
- **Summary:** Creates a multiplication expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values to multiply
- **Returns:** a string representation of the multiplication expression
##### division(...) -> String
- **Signature:** `public static String division(final Object... objects)`
- **Summary:** Creates a division expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values to divide
- **Returns:** a string representation of the division expression
##### modulus(...) -> String
- **Signature:** `public static String modulus(final Object... objects)`
- **Summary:** Creates a modulus expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for modulus operation
- **Returns:** a string representation of the modulus expression
##### lShift(...) -> String
- **Signature:** `public static String lShift(final Object... objects)`
- **Summary:** Creates a left shift expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for left shift operation
- **Returns:** a string representation of the left shift expression
##### rShift(...) -> String
- **Signature:** `public static String rShift(final Object... objects)`
- **Summary:** Creates a right shift expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for right shift operation
- **Returns:** a string representation of the right shift expression
##### bitwiseAnd(...) -> String
- **Signature:** `public static String bitwiseAnd(final Object... objects)`
- **Summary:** Creates a bitwise AND expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for bitwise AND operation
- **Returns:** a string representation of the bitwise AND expression
##### bitwiseOr(...) -> String
- **Signature:** `public static String bitwiseOr(final Object... objects)`
- **Summary:** Creates a bitwise OR expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for bitwise OR operation
- **Returns:** a string representation of the bitwise OR expression
##### bitwiseXOr(...) -> String
- **Signature:** `public static String bitwiseXOr(final Object... objects)`
- **Summary:** Creates a bitwise XOR expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for bitwise XOR operation
- **Returns:** a string representation of the bitwise XOR expression
##### formalize(...) -> String
- **Signature:** `public static String formalize(final Object value)`
- **Summary:** Converts a value to its SQL representation.
- **Parameters:**
  - `value` (`Object`) — the value to formalize
- **Returns:** the SQL representation of the value
##### count(...) -> String
- **Signature:** `public static String count(final String expression)`
- **Summary:** Creates a COUNT function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to count
- **Returns:** a COUNT function string
##### average(...) -> String
- **Signature:** `public static String average(final String expression)`
- **Summary:** Creates an AVERAGE function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to average
- **Returns:** an AVG function string
##### sum(...) -> String
- **Signature:** `public static String sum(final String expression)`
- **Summary:** Creates a SUM function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to sum
- **Returns:** a SUM function string
##### min(...) -> String
- **Signature:** `public static String min(final String expression)`
- **Summary:** Creates a MIN function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to find minimum
- **Returns:** a MIN function string
##### max(...) -> String
- **Signature:** `public static String max(final String expression)`
- **Summary:** Creates a MAX function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to find maximum
- **Returns:** a MAX function string
##### abs(...) -> String
- **Signature:** `public static String abs(final String expression)`
- **Summary:** Creates an ABS (absolute value) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to get absolute value of
- **Returns:** an ABS function string
##### acos(...) -> String
- **Signature:** `public static String acos(final String expression)`
- **Summary:** Creates an ACOS (arc cosine) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to calculate arc cosine of
- **Returns:** an ACOS function string
##### asin(...) -> String
- **Signature:** `public static String asin(final String expression)`
- **Summary:** Creates an ASIN (arc sine) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to calculate arc sine of
- **Returns:** an ASIN function string
##### atan(...) -> String
- **Signature:** `public static String atan(final String expression)`
- **Summary:** Creates an ATAN (arc tangent) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to calculate arc tangent of
- **Returns:** an ATAN function string
##### ceil(...) -> String
- **Signature:** `public static String ceil(final String expression)`
- **Summary:** Creates a CEIL (ceiling) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to round up
- **Returns:** a CEIL function string
##### cos(...) -> String
- **Signature:** `public static String cos(final String expression)`
- **Summary:** Creates a COS (cosine) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to calculate cosine of
- **Returns:** a COS function string
##### exp(...) -> String
- **Signature:** `public static String exp(final String expression)`
- **Summary:** Creates an EXP (exponential) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to calculate exponential of
- **Returns:** an EXP function string
##### floor(...) -> String
- **Signature:** `public static String floor(final String expression)`
- **Summary:** Creates a FLOOR function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to round down
- **Returns:** a FLOOR function string
##### log(...) -> String
- **Signature:** `public static String log(final String base, final String value)`
- **Summary:** Creates a LOG function expression with specified base.
- **Parameters:**
  - `base` (`String`) — the logarithm base
  - `value` (`String`) — the value to calculate logarithm of
- **Returns:** a LOG function string
##### ln(...) -> String
- **Signature:** `public static String ln(final String expression)`
- **Summary:** Creates an LN (natural logarithm) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to calculate natural logarithm of
- **Returns:** an LN function string
##### mod(...) -> String
- **Signature:** `public static String mod(final String dividend, final String divisor)`
- **Summary:** Creates a MOD (modulo) function expression.
- **Parameters:**
  - `dividend` (`String`) — the dividend
  - `divisor` (`String`) — the divisor
- **Returns:** a MOD function string
##### power(...) -> String
- **Signature:** `public static String power(final String base, final String exponent)`
- **Summary:** Creates a POWER function expression.
- **Parameters:**
  - `base` (`String`) — the base
  - `exponent` (`String`) — the exponent
- **Returns:** a POWER function string
##### sign(...) -> String
- **Signature:** `public static String sign(final String expression)`
- **Summary:** Creates a SIGN function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to get sign of
- **Returns:** a SIGN function string
##### sin(...) -> String
- **Signature:** `public static String sin(final String expression)`
- **Summary:** Creates a SIN (sine) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to calculate sine of
- **Returns:** a SIN function string
##### sqrt(...) -> String
- **Signature:** `public static String sqrt(final String expression)`
- **Summary:** Creates a SQRT (square root) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to calculate square root of
- **Returns:** a SQRT function string
##### tan(...) -> String
- **Signature:** `public static String tan(final String expression)`
- **Summary:** Creates a TAN (tangent) function expression.
- **Parameters:**
  - `expression` (`String`) — the expression to calculate tangent of
- **Returns:** a TAN function string
##### concat(...) -> String
- **Signature:** `public static String concat(final String str1, final String str2)`
- **Summary:** Creates a CONCAT function expression to concatenate two strings.
- **Parameters:**
  - `str1` (`String`) — the first string
  - `str2` (`String`) — the second string
- **Returns:** a CONCAT function string
##### replace(...) -> String
- **Signature:** `public static String replace(final String str, final String oldString, final String replacement)`
- **Summary:** Creates a REPLACE function expression.
- **Parameters:**
  - `str` (`String`) — the string to search in
  - `oldString` (`String`) — the string to search for
  - `replacement` (`String`) — the replacement string
- **Returns:** a REPLACE function string
##### stringLength(...) -> String
- **Signature:** `public static String stringLength(final String str)`
- **Summary:** Creates a LENGTH function expression.
- **Parameters:**
  - `str` (`String`) — the string to get length of
- **Returns:** a LENGTH function string
##### subString(...) -> String
- **Signature:** `public static String subString(final String str, final int fromIndex)`
- **Summary:** Creates a SUBSTR function expression starting from a position.
- **Parameters:**
  - `str` (`String`) — the string to extract from
  - `fromIndex` (`int`) — the starting position (1-based)
- **Returns:** a SUBSTR function string
- **Signature:** `public static String subString(final String str, final int fromIndex, final int length)`
- **Summary:** Creates a SUBSTR function expression with start position and length.
- **Parameters:**
  - `str` (`String`) — the string to extract from
  - `fromIndex` (`int`) — the starting position (1-based)
  - `length` (`int`) — the number of characters to extract
- **Returns:** a SUBSTR function string
##### trim(...) -> String
- **Signature:** `public static String trim(final String str)`
- **Summary:** Creates a TRIM function expression.
- **Parameters:**
  - `str` (`String`) — the string to trim
- **Returns:** a TRIM function string
##### lTrim(...) -> String
- **Signature:** `public static String lTrim(final String str)`
- **Summary:** Creates an LTRIM (left trim) function expression.
- **Parameters:**
  - `str` (`String`) — the string to left trim
- **Returns:** an LTRIM function string
##### rTrim(...) -> String
- **Signature:** `public static String rTrim(final String str)`
- **Summary:** Creates an RTRIM (right trim) function expression.
- **Parameters:**
  - `str` (`String`) — the string to right trim
- **Returns:** an RTRIM function string
##### lPad(...) -> String
- **Signature:** `public static String lPad(final String str, final int length, final String padStr)`
- **Summary:** Creates an LPAD (left pad) function expression.
- **Parameters:**
  - `str` (`String`) — the string to pad
  - `length` (`int`) — the total length after padding
  - `padStr` (`String`) — the string to pad with
- **Returns:** an LPAD function string
##### rPad(...) -> String
- **Signature:** `public static String rPad(final String str, final int length, final String padStr)`
- **Summary:** Creates an RPAD (right pad) function expression.
- **Parameters:**
  - `str` (`String`) — the string to pad
  - `length` (`int`) — the total length after padding
  - `padStr` (`String`) — the string to pad with
- **Returns:** an RPAD function string
##### lower(...) -> String
- **Signature:** `public static String lower(final String str)`
- **Summary:** Creates a LOWER function expression.
- **Parameters:**
  - `str` (`String`) — the string to convert to lowercase
- **Returns:** a LOWER function string
##### upper(...) -> String
- **Signature:** `public static String upper(final String str)`
- **Summary:** Creates an UPPER function expression.
- **Parameters:**
  - `str` (`String`) — the string to convert to uppercase
- **Returns:** an UPPER function string

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Expression(final String literal)`
- **Summary:** Constructs a new Expression with the specified SQL literal.
- **Contract:**
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code Expression expr1 = new Expression("CURRENT_TIMESTAMP"); Expression expr2 = new Expression("price * quantity"); Expression expr3 = new Expression("CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END"); Expression expr4 = new Expression("COALESCE(middle_name, '')"); } </pre>
- **Parameters:**
  - `literal` (`String`) — the SQL expression as a string. Can contain any valid SQL.
##### getLiteral(...) -> String
- **Signature:** `public String getLiteral()`
- **Summary:** Gets the SQL literal string of this expression.
- **Parameters:**
  - (none)
- **Returns:** the SQL expression string
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Returns an empty list as expressions have no parameters.
- **Parameters:**
  - (none)
- **Returns:** an empty list
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** No-op method as Expression has no parameters to clear.
- **Parameters:**
  - (none)
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Returns the literal string of this expression.
- **Contract:**
  - The naming policy may be applied to property names within the expression if they can be identified as simple column names.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply
- **Returns:** the literal string of this expression with applied naming policy
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Computes the hash code based on the literal string.
- **Parameters:**
  - (none)
- **Returns:** the hash code of the literal
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this expression equals another object.
- **Contract:**
  - Checks if this expression equals another object.
  - Two expressions are equal if they have the same literal string.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal

### Class Expr (com.landawn.abacus.query.condition.Expression.Expr)
A simplified alias class for {@link Expression} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class FullJoin (com.landawn.abacus.query.condition.FullJoin)
Represents a FULL OUTER JOIN operation in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public FullJoin(final String joinEntity)`
- **Summary:** Creates a FULL JOIN clause for the specified table or entity.
- **Contract:**
  - This creates a join without an ON condition, which may need to be specified separately or will use implicit join conditions based on foreign key relationships (if supported by the database).
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public FullJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a FULL JOIN clause with a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .
- **Signature:** `public FullJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a FULL JOIN clause with multiple tables/entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .

### Class GreaterEqual (com.landawn.abacus.query.condition.GreaterEqual)
Represents a greater-than-or-equal-to ( &gt; =) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public GreaterEqual(final String propName, final Object propValue)`
- **Summary:** Creates a new GreaterEqual condition.
- **Contract:**
  - The condition evaluates to true when the property value is greater than or equal to the specified value.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check if salary is at least 50000 GreaterEqual salaryCondition = new GreaterEqual("salary", 50000); // Check if score meets minimum requirement GreaterEqual scoreCondition = new GreaterEqual("score", 60); // Check if date is on or after a specific date GreaterEqual dateCondition = new GreaterEqual("expiryDate", LocalDate.now()); // Use with subquery - find products priced at or above average SubQuery avgPrice = Filters.subQuery("SELECT AVG(price) FROM products"); GreaterEqual atOrAboveAverage = new GreaterEqual("price", avgPrice); } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `propValue` (`Object`) — the value to compare against (can be null, literal value, or subquery)

### Class GreaterThan (com.landawn.abacus.query.condition.GreaterThan)
Represents a greater-than ( &gt; ) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public GreaterThan(final String propName, final Object propValue)`
- **Summary:** Creates a new GreaterThan condition.
- **Contract:**
  - The condition evaluates to true when the property value is strictly greater than the specified value.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check if salary is above 50000 GreaterThan salaryCondition = new GreaterThan("salary", 50000); // Check if temperature exceeds threshold GreaterThan tempCondition = new GreaterThan("temperature", 100); // Check if date is after a specific date GreaterThan dateCondition = new GreaterThan("expiryDate", LocalDate.of(2024, 12, 31)); // Use with subquery - find products priced above average SubQuery avgPrice = Filters.subQuery("SELECT AVG(price) FROM products"); GreaterThan aboveAverage = new GreaterThan("price", avgPrice); } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `propValue` (`Object`) — the value to compare against (can be null, literal value, or subquery)

### Class GroupBy (com.landawn.abacus.query.condition.GroupBy)
Represents a GROUP BY clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public GroupBy(final Condition condition)`
- **Summary:** Creates a new GROUP BY clause with the specified condition.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Group by year extracted from date GroupBy byYear = new GroupBy(Filters.expr("YEAR(order_date)")); // SQL: GROUP BY YEAR(order_date) // Group by calculated expression GroupBy byRange = new GroupBy(Filters.expr("CASE WHEN age < 30 THEN 'Young' ELSE 'Senior' END")); // SQL: GROUP BY CASE WHEN age < 30 THEN 'Young' ELSE 'Senior' END } </pre>
- **Parameters:**
  - `condition` (`Condition`) — the grouping condition or expression. Must not be null.
- **Signature:** `public GroupBy(final String... propNames)`
- **Summary:** Creates a new GROUP BY clause with the specified property names.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to group by, in order. Must not be null or empty.
- **Signature:** `public GroupBy(final String propName, final SortDirection direction)`
- **Summary:** Creates a new GROUP BY clause with a single property and sort direction.
- **Parameters:**
  - `propName` (`String`) — the property name to group by. Must not be null or empty.
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC). Must not be null.
- **Signature:** `public GroupBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates a new GROUP BY clause with multiple properties and a single sort direction.
- **Contract:**
  - This is useful when you want consistent ordering across all grouping columns.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by. Must not be null or empty.
  - `direction` (`SortDirection`) — the sort direction to apply to all properties. Must not be null.
- **Signature:** `public GroupBy(final Map<String, SortDirection> orders)`
- **Summary:** Creates a new GROUP BY clause with custom sort directions for each property.
- **Contract:**
  - The map should maintain insertion order (use LinkedHashMap) to preserve the grouping order, as the order of columns in GROUP BY can affect performance and results.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — a map of property names to their sort directions. Should be a LinkedHashMap to maintain order. Must not be null or empty.

### Class Having (com.landawn.abacus.query.condition.Having)
Represents a HAVING clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Having(final Condition condition)`
- **Summary:** Creates a new HAVING clause with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition to apply in the HAVING clause. Must not be null.

### Class In (com.landawn.abacus.query.condition.In)
Represents an IN condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public In(final String propName, final Collection<?> values)`
- **Summary:** Creates a new IN condition with the specified property name and collection of values.
- **Contract:**
  - The condition checks if the property value matches any value in the collection.
- **Parameters:**
  - `propName` (`String`) — the property/column name. Must not be null or empty.
  - `values` (`Collection<?>`) — the collection of values to check against. Must not be null or empty. The collection is copied internally to prevent external modifications.
##### getPropName(...) -> String
- **Signature:** `public String getPropName()`
- **Summary:** Gets the property name being checked in this IN condition.
- **Parameters:**
  - (none)
- **Returns:** the property name, or {@code null} for an uninitialized instance created by serialization frameworks
##### getValues(...) -> List<?>
- **Signature:** `public List<?> getValues()`
- **Summary:** Gets the values used by this IN condition.
- **Parameters:**
  - (none)
- **Returns:** the internal values list, or {@code null} for an uninitialized instance
##### setValues(...) -> void
- **Signature:** `@Deprecated public void setValues(final List<?> values)`
- **Summary:** Sets new values for this IN condition.
- **Contract:**
  - However, modifying conditions after creation is strongly discouraged as conditions should be treated as immutable to ensure thread safety and predictable behavior.
- **Parameters:**
  - `values` (`List<?>`) — the new list of values. Must not be null or empty.
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the parameter values for this condition.
- **Contract:**
  - These values will be bound to the prepared statement placeholders when the query is executed.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of values as parameters, or an empty list if no values are set
##### clearParameters(...) -> void
- **Signature:** `@SuppressWarnings("rawtypes") @Override public void clearParameters()`
- **Summary:** Clears all parameter values by setting them to null to free memory.
- **Contract:**
  - Use this method to release large objects when the condition is no longer needed.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") @Override public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this IN condition.
- **Parameters:**
  - (none)
- **Returns:** a new IN instance with a copy of all values
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this IN condition to its string representation using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name
- **Returns:** the string representation, e.g., "status IN ('active', 'pending')"
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this IN condition.
- **Parameters:**
  - (none)
- **Returns:** the hash code based on property name, operator, and values
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this IN condition is equal to another object.
- **Contract:**
  - Checks if this IN condition is equal to another object.
  - Two IN conditions are equal if they have the same property name, operator, and values list.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

### Class InSubQuery (com.landawn.abacus.query.condition.InSubQuery)
Represents an IN condition with a subquery in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public InSubQuery(final String propName, final SubQuery subQuery)`
- **Summary:** Creates an IN subquery condition for a single property.
- **Contract:**
  - Use this constructor when checking if a single column value exists in the subquery result.
  - The subquery should return a single column of compatible type.
- **Parameters:**
  - `propName` (`String`) — the property/column name. Must not be null or empty.
  - `subQuery` (`SubQuery`) — the subquery that returns the values to check against. Must not be null.
- **Signature:** `public InSubQuery(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates an IN subquery condition for multiple properties.
- **Contract:**
  - Use this constructor for composite key checks or when multiple columns need to match the subquery results.
  - The subquery must return the same number of columns in the same order.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the names of the properties to check. Must not be null or empty. The order must match the column order in the subquery.
  - `subQuery` (`SubQuery`) — the subquery that returns the value combinations to check against. Must not be null. Must return the same number of columns as propNames.size().
##### getPropName(...) -> String
- **Signature:** `public String getPropName()`
- **Summary:** Gets the property name for single-column IN conditions.
- **Contract:**
  - Returns null if this is a multi-column condition.
- **Parameters:**
  - (none)
- **Returns:** the property name, or null if this is a multi-column condition
##### getPropNames(...) -> Collection<String>
- **Signature:** `public Collection<String> getPropNames()`
- **Summary:** Gets the property names for multi-column IN conditions.
- **Contract:**
  - Returns null if this is a single-column condition.
- **Parameters:**
  - (none)
- **Returns:** collection of property names, or null if this is a single-column condition
##### getSubQuery(...) -> SubQuery
- **Signature:** `public SubQuery getSubQuery()`
- **Summary:** Gets the subquery used in this IN condition.
- **Parameters:**
  - (none)
- **Returns:** the subquery
##### setSubQuery(...) -> void
- **Signature:** `@Deprecated public void setSubQuery(final SubQuery subQuery)`
- **Summary:** Sets a new subquery for this IN condition.
- **Contract:**
  - However, modifying conditions after creation is strongly discouraged as conditions should be treated as immutable to ensure thread safety and predictable behavior.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the new subquery to set. Must not be null.
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the list of parameters from the subquery.
- **Contract:**
  - These are the parameter values that will be bound to the prepared statement placeholders when the query is executed.
- **Parameters:**
  - (none)
- **Returns:** list of parameter values from the subquery
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears parameters in the underlying subquery.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") @Override public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this InSubQuery condition.
- **Parameters:**
  - (none)
- **Returns:** a new InSubQuery instance with a deep copy of the subquery
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this InSubQuery condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name(s), operator, and subquery
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this InSubQuery condition is equal to another object.
- **Contract:**
  - Checks if this InSubQuery condition is equal to another object.
  - Two InSubQuery conditions are equal if they have the same property name(s), operator, and subquery.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this InSubQuery condition to its string representation according to the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names
- **Returns:** the string representation of the IN subquery condition

### Class InnerJoin (com.landawn.abacus.query.condition.InnerJoin)
Represents an INNER JOIN clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public InnerJoin(final String joinEntity)`
- **Summary:** Creates an INNER JOIN clause for the specified table or entity.
- **Contract:**
  - This creates a join without an ON condition, which may need to be specified separately or will use implicit join conditions based on foreign key relationships (if supported by the database).
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public InnerJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates an INNER JOIN clause with a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .
- **Signature:** `public InnerJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates an INNER JOIN clause with multiple tables/entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .

### Class Intersect (com.landawn.abacus.query.condition.Intersect)
Represents an INTERSECT clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Intersect(final SubQuery subQuery)`
- **Summary:** Creates a new INTERSECT clause with the specified subquery.
- **Contract:**
  - Both queries must have the same number of columns with compatible data types.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Find users who are both premium AND active in the last 30 days SubQuery activeUsers = Filters.subQuery("SELECT user_id FROM activity WHERE last_login > CURRENT_DATE - 30"); Intersect premiumActive = new Intersect(activeUsers); // When combined with premium users query: // SELECT user_id FROM users WHERE plan = 'premium' // INTERSECT // SELECT user_id FROM activity WHERE last_login > CURRENT_DATE - 30 // Returns only user_id values present in both result sets // Find employees who work in both projects SubQuery projectB = Filters.subQuery("SELECT employee_id FROM assignments WHERE project = 'B'"); Intersect bothProjects = new Intersect(projectB); // Use with project A query to find employees assigned to both projects // Find common skills between two job positions SubQuery position2Skills = Filters.subQuery("SELECT skill_id FROM position_skills WHERE position_id = 2"); Intersect commonSkills = new Intersect(position2Skills); // Identifies skills required by both positions // Find products in stock AND on promotion SubQuery onPromotion = Filters.subQuery("SELECT product_id FROM promotions WHERE active = true"); Intersect availablePromotions = new Intersect(onPromotion); // SELECT product_id FROM inventory WHERE quantity > 0 // INTERSECT // SELECT product_id FROM promotions WHERE active = true } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to perform the INTERSECT operation with. Must not be null. The subquery should return the same number of columns with compatible types as the main query.
- **See also:** Union, UnionAll, Except, Minus

### Class Is (com.landawn.abacus.query.condition.Is)
Represents an IS condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Is(final String propName, final Object propValue)`
- **Summary:** Creates a new IS condition with the specified property name and value.
- **Contract:**
  - This condition checks if the property is equal to the specified value using the SQL IS operator.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be null or empty)
  - `propValue` (`Object`) — the value to compare against (can be null, literal value, or subquery)

### Class IsInfinite (com.landawn.abacus.query.condition.IsInfinite)
Represents a condition that checks if a numeric property value is infinite.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public IsInfinite(final String propName)`
- **Summary:** Creates a new IsInfinite condition for the specified property.
- **Contract:**
  - This condition generates an "IS INFINITE" SQL clause to check if the property's numeric value is infinite (either positive infinity or negative infinity).
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be null or empty)

### Class IsNaN (com.landawn.abacus.query.condition.IsNaN)
Represents a condition that checks if a numeric property value is NaN (Not a Number).

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public IsNaN(final String propName)`
- **Summary:** Creates a new IsNaN condition for the specified property.
- **Contract:**
  - This condition generates an "IS NAN" SQL clause to check if the property's numeric value is NaN (Not a Number), which represents an invalid or undefined mathematical result.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check if temperature reading is NaN IsNaN tempCheck = new IsNaN("temperature"); // Generates SQL: temperature IS NAN // Find all records with invalid calculations IsNaN calcError = new IsNaN("computed_value"); // Generates SQL: computed_value IS NAN // Identify division by zero errors (0/0 results in NaN) IsNaN divError = new IsNaN("average_score"); // Generates SQL: average_score IS NAN // Check statistical calculations IsNaN statsCheck = new IsNaN("standard_deviation"); // Generates SQL: standard_deviation IS NAN // Validate sensor readings IsNaN sensorError = new IsNaN("pressure_reading"); // Generates SQL: pressure_reading IS NAN // Use in query builders to find problematic data List<Measurement> invalidMeasurements = queryExecutor .prepareQuery(Measurement.class) .where(new IsNaN("sensor_value")) .list(); } </pre>
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be null or empty)

### Class IsNot (com.landawn.abacus.query.condition.IsNot)
Represents an IS NOT condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public IsNot(final String propName, final Object propValue)`
- **Summary:** Creates a new IS NOT condition with the specified property name and value.
- **Contract:**
  - This condition checks if the property is not equal to the specified value using the SQL IS NOT operator.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check for NOT NULL (though IsNotNull is preferred) IsNot notNull = new IsNot("phone_number", null); // Generates: phone_number IS NOT NULL // Check if not NaN Expression nanExpr = Filters.expr("NAN"); IsNot notNaN = new IsNot("temperature", nanExpr); // Generates: temperature IS NOT NAN // Check if not a custom value Expression pendingExpr = Filters.expr("PENDING"); IsNot notPending = new IsNot("order_status", pendingExpr); // Generates: order_status IS NOT PENDING } </pre>
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be null or empty)
  - `propValue` (`Object`) — the value to compare against (can be null, literal value, or subquery)

### Class IsNotInfinite (com.landawn.abacus.query.condition.IsNotInfinite)
Represents a condition that checks if a numeric property value is NOT infinite.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public IsNotInfinite(final String propName)`
- **Summary:** Creates a new IsNotInfinite condition for the specified property.
- **Contract:**
  - This condition generates an "IS NOT INFINITE" SQL clause to check if the property's numeric value is NOT infinite (neither positive infinity nor negative infinity).
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be null or empty)

### Class IsNotNaN (com.landawn.abacus.query.condition.IsNotNaN)
Represents a condition that checks if a numeric property value is NOT NaN (Not a Number).

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public IsNotNaN(final String propName)`
- **Summary:** Creates a new IsNotNaN condition for the specified property.
- **Contract:**
  - This condition generates an "IS NOT NAN" SQL clause to check if the property's numeric value is NOT NaN (i.e., is a valid number).
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be null or empty)

### Class IsNotNull (com.landawn.abacus.query.condition.IsNotNull)
Represents a condition that checks if a property value is NOT NULL.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public IsNotNull(final String propName)`
- **Summary:** Creates a new IsNotNull condition for the specified property.
- **Contract:**
  - This condition generates an "IS NOT NULL" SQL clause to check if the property value is not NULL, effectively filtering for records that have values in the specified field.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be null or empty)

### Class IsNull (com.landawn.abacus.query.condition.IsNull)
Represents a condition that checks if a property value is NULL.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public IsNull(final String propName)`
- **Summary:** Creates a new IsNull condition for the specified property.
- **Contract:**
  - This condition generates an "IS NULL" SQL clause to check if the property value is NULL, which represents the absence of a value in the database.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be null or empty)

### Class Join (com.landawn.abacus.query.condition.Join)
Base class for SQL JOIN operations.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Join(final String joinEntity)`
- **Summary:** Creates a simple JOIN clause for the specified table or entity.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
- **Signature:** `public Join(final String joinEntity, final Condition condition)`
- **Summary:** Creates a JOIN clause with a condition.
- **Contract:**
  - This specifies how the tables are related and which rows should be combined.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .
- **Signature:** `public Join(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a JOIN clause with multiple tables or entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .
##### getJoinEntities(...) -> List<String>
- **Signature:** `public List<String> getJoinEntities()`
- **Summary:** Gets the list of tables or entities involved in this join.
- **Parameters:**
  - (none)
- **Returns:** a copy of the list of join entities
##### getCondition(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T extends Condition> T getCondition()`
- **Summary:** Gets the join condition.
- **Contract:**
  - May return null if no condition was specified (natural join or cross join).
- **Parameters:**
  - (none)
- **Returns:** the join condition, or null if no condition is specified
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets all parameters from the join condition.
- **Contract:**
  - Returns an empty list if there's no condition or the condition has no parameters.
- **Parameters:**
  - (none)
- **Returns:** the list of parameters from the condition, or an empty list if no condition
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears all parameter values by setting them to null to free memory.
- **Contract:**
  - <p> This method delegates to the join condition, if present.
  - If this join has no condition, this method is a no-op.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@Override @SuppressWarnings("unchecked") public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this JOIN clause.
- **Parameters:**
  - (none)
- **Returns:** a new Join instance with copies of all entities and condition
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this JOIN clause to its string representation according to the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply
- **Returns:** the string representation, e.g., "JOIN orders o ON customers.id = o.customer_id"
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Computes the hash code for this JOIN clause.
- **Parameters:**
  - (none)
- **Returns:** hash code based on operator, join entities, and condition
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this JOIN clause is equal to another object.
- **Contract:**
  - Checks if this JOIN clause is equal to another object.
  - Two Join instances are equal if they have the same operator, join entities, and condition.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the object is a Join with the same operator, entities, and condition

### Class Junction (com.landawn.abacus.query.condition.Junction)
Base class for logical junction conditions that combine multiple conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Junction(final Operator operator, final Condition... conditions)`
- **Summary:** Creates a new Junction with the specified operator and conditions.
- **Parameters:**
  - `operator` (`Operator`) — the logical operator to use (AND, OR, etc.). Must not be null.
  - `conditions` (`Condition[]`) — the conditions to combine. Can be empty but not null.
- **Signature:** `public Junction(final Operator operator, final Collection<? extends Condition> conditions)`
- **Summary:** Creates a new Junction with the specified operator and collection of conditions.
- **Contract:**
  - This constructor is useful when conditions are already collected in a list or set.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Create conditions dynamically List<Condition> conditions = new ArrayList<>(); conditions.add(new Equal("status", "active")); conditions.add(new GreaterThan("score", 80)); if (includeDateCheck) { conditions.add(new LessEqual("date", today)); } Junction junction = new Junction(Operator.AND, conditions); } </pre>
- **Parameters:**
  - `operator` (`Operator`) — the logical operator to use (AND, OR, etc.). Must not be null.
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine. Can be empty but not null.
##### getConditions(...) -> List<Condition>
- **Signature:** `public List<Condition> getConditions()`
- **Summary:** Gets the list of conditions contained in this junction.
- **Parameters:**
  - (none)
- **Returns:** the list of conditions. Modifications to this list will affect the junction.
##### set(...) -> void
- **Signature:** `public final void set(final Condition... conditions)`
- **Summary:** Replaces all conditions in this junction with the specified conditions.
- **Parameters:**
  - `conditions` (`Condition[]`) — the new conditions to set. Existing conditions will be cleared.
- **Signature:** `public void set(final Collection<? extends Condition> conditions)`
- **Summary:** Replaces all conditions in this junction with the specified collection of conditions.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the new collection of conditions to set. Existing conditions will be cleared.
##### add(...) -> void
- **Signature:** `public final void add(final Condition... conditions)`
- **Summary:** Adds the specified conditions to this junction.
- **Parameters:**
  - `conditions` (`Condition[]`) — the conditions to add
- **Signature:** `public void add(final Collection<? extends Condition> conditions)`
- **Summary:** Adds the specified collection of conditions to this junction.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to add
##### remove(...) -> void
- **Signature:** `@Deprecated public final void remove(final Condition... conditions)`
- **Summary:** Removes the specified conditions from this junction.
- **Parameters:**
  - `conditions` (`Condition[]`) — the conditions to remove
- **Signature:** `@Deprecated public void remove(final Collection<? extends Condition> conditions)`
- **Summary:** Removes the specified collection of conditions from this junction.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to remove
##### clear(...) -> void
- **Signature:** `public void clear()`
- **Summary:** Removes all conditions from this junction.
- **Parameters:**
  - (none)
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets all parameters from all conditions in this junction.
- **Parameters:**
  - (none)
- **Returns:** a list containing all parameters from all conditions
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears parameters in all child conditions by recursively calling clearParameters() on each.
- **Contract:**
  - <p> Use this method to release large objects held by any condition in the junction tree when the junction is no longer needed.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@Override @SuppressWarnings("unchecked") public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this junction including all its conditions.
- **Parameters:**
  - (none)
- **Returns:** a new Junction instance with copies of all conditions
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this junction to its string representation according to the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names
- **Returns:** the string representation with proper parentheses and spacing
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Computes the hash code for this junction based on its operator and conditions.
- **Parameters:**
  - (none)
- **Returns:** hash code based on operator and condition list
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this junction is equal to another object.
- **Contract:**
  - Checks if this junction is equal to another object.
  - Two junctions are considered equal if they have the same operator and contain the same conditions in the same order.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the object is a Junction with the same operator and conditions

### Class LeftJoin (com.landawn.abacus.query.condition.LeftJoin)
Represents a LEFT JOIN clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public LeftJoin(final String joinEntity)`
- **Summary:** Creates a LEFT JOIN clause for the specified table or entity.
- **Contract:**
  - This creates a join without an ON condition, which may need to be specified separately or will use implicit join conditions based on foreign key relationships (if supported by the database).
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public LeftJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a LEFT JOIN clause with a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .
- **Signature:** `public LeftJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a LEFT JOIN clause with multiple tables/entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .

### Class LessEqual (com.landawn.abacus.query.condition.LessEqual)
Represents a less-than-or-equal-to ( &lt; =) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public LessEqual(final String propName, final Object propValue)`
- **Summary:** Creates a new LessEqual condition.
- **Contract:**
  - This condition checks if the property value is less than or equal to the specified value, providing an inclusive upper bound check.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `propValue` (`Object`) — the value to compare against (can be null, literal value, or subquery)

### Class LessThan (com.landawn.abacus.query.condition.LessThan)
Represents a less-than ( &lt; ) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public LessThan(final String propName, final Object propValue)`
- **Summary:** Creates a new LessThan condition.
- **Contract:**
  - This condition checks if the property value is less than the specified value, providing an exclusive upper bound check.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `propValue` (`Object`) — the value to compare against (can be null, literal value, or subquery)

### Class Like (com.landawn.abacus.query.condition.Like)
Represents a LIKE condition in SQL queries for pattern matching.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Like(final String propName, final Object propValue)`
- **Summary:** Creates a new LIKE condition with the specified property name and pattern.
- **Contract:**
  - The pattern should include SQL wildcards (% or _) for pattern matching.
  - If special characters need to be matched literally, they should be escaped according to your database's escape syntax.
  - // Email domain pattern Like emailDomain = new Like("email", "%@%.com"); // Matches any .com email address // Phone number pattern (specific format) Like phonePattern = new Like("phone", "(___) ___-____"); // Matches: "(555) 123-4567" format // Escape special characters if needed (syntax varies by database) Like escaped = new Like("path", "%\\\\_%"); // To match literal underscore // Check your database documentation for escape syntax } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `propValue` (`Object`) — the pattern to match, including wildcards (can be null, literal value, or subquery). Use % for any characters, _ for single character.

### Class Limit (com.landawn.abacus.query.condition.Limit)
Represents a LIMIT clause in SQL queries to restrict the number of rows returned.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Limit(final int count)`
- **Summary:** Creates a LIMIT clause with the specified row count.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return. Should be non-negative (typically positive).
- **Signature:** `public Limit(final int offset, final int count)`
- **Summary:** Creates a LIMIT clause with both count and offset.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip before returning results. Must be non-negative.
  - `count` (`int`) — the maximum number of rows to return after the offset. Must be non-negative.
- **Signature:** `public Limit(final String expr)`
- **Summary:** Creates a LIMIT clause from a string expression.
- **Parameters:**
  - `expr` (`String`) — the custom LIMIT expression as a string. Should not be null or empty.
##### getExpr(...) -> String
- **Signature:** `public String getExpr()`
- **Summary:** Returns the custom expression string if one was provided.
- **Contract:**
  - Returns the custom expression string if one was provided.
  - This method returns the raw expression string passed to the string constructor, or {@code null} if the Limit was created with count/offset parameters.
- **Parameters:**
  - (none)
- **Returns:** the custom expression string, or {@code null} if constructed with count/offset parameters
##### getCount(...) -> int
- **Signature:** `public int getCount()`
- **Summary:** Gets the maximum number of rows to return.
- **Parameters:**
  - (none)
- **Returns:** the row count limit, or {@link Integer#MAX_VALUE} if constructed with a custom expression
##### getOffset(...) -> int
- **Signature:** `public int getOffset()`
- **Summary:** Gets the number of rows to skip before returning results.
- **Parameters:**
  - (none)
- **Returns:** the offset value, or 0 if constructed with only count or with a custom expression
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the parameters for this LIMIT clause.
- **Parameters:**
  - (none)
- **Returns:** an empty list as LIMIT has no parameters
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** This method does nothing for LIMIT clauses.
- **Parameters:**
  - (none)
##### and(...) -> And
- **Signature:** `@Override public And and(final Condition condition) throws UnsupportedOperationException`
- **Summary:** Attempts to combine this LIMIT with another condition using AND.
- **Parameters:**
  - `condition` (`Condition`) — the condition to combine with (ignored)
- **Returns:** never returns normally
- **Throws:**
  - `java.lang.UnsupportedOperationException` — always thrown as LIMIT cannot be combined with AND
##### or(...) -> Or
- **Signature:** `@Override public Or or(final Condition condition) throws UnsupportedOperationException`
- **Summary:** Attempts to combine this LIMIT with another condition using OR.
- **Parameters:**
  - `condition` (`Condition`) — the condition to combine with (ignored)
- **Returns:** never returns normally
- **Throws:**
  - `java.lang.UnsupportedOperationException` — always thrown as LIMIT cannot be combined with OR
##### not(...) -> Not
- **Signature:** `@Override public Not not() throws UnsupportedOperationException`
- **Summary:** Attempts to negate this LIMIT clause.
- **Parameters:**
  - (none)
- **Returns:** never returns normally
- **Throws:**
  - `java.lang.UnsupportedOperationException` — always thrown as LIMIT cannot be negated
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this LIMIT clause to its string representation according to the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply (though LIMIT typically doesn't need name conversion)
- **Returns:** the string representation of this LIMIT clause
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Computes the hash code for this LIMIT clause.
- **Contract:**
  - The hash code is calculated based on either the custom expression (if present) or the combination of count and offset values.
- **Parameters:**
  - (none)
- **Returns:** the hash code based on expr if present, otherwise based on count and offset
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this LIMIT clause is equal to another object.
- **Contract:**
  - Checks if this LIMIT clause is equal to another object.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the object is a Limit with the same expr or count/offset values

### Class Minus (com.landawn.abacus.query.condition.Minus)
Represents a MINUS clause in SQL queries (also known as EXCEPT in some databases).

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Minus(final SubQuery subQuery)`
- **Summary:** Creates a new MINUS clause with the specified subquery.
- **Contract:**
  - Both queries must have compatible column structures (same number of columns with compatible types).
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Find products that are in inventory but have never been sold SubQuery soldProducts = Filters.subQuery("SELECT product_id FROM sales"); Minus unsoldProducts = new Minus(soldProducts); // When used with a query selecting from inventory: // SELECT product_id FROM inventory // MINUS // SELECT product_id FROM sales // Returns product_id values in inventory but not in sales // Find active employees not assigned to any project SubQuery assignedEmployees = Filters.subQuery("SELECT employee_id FROM project_assignments"); Minus unassigned = new Minus(assignedEmployees); // SELECT employee_id FROM employees WHERE status = 'ACTIVE' // MINUS // SELECT employee_id FROM project_assignments // Returns active employees with no project assignments // Find customers who have never placed an order SubQuery customersWithOrders = Filters.subQuery("SELECT DISTINCT customer_id FROM orders"); Minus customersWithoutOrders = new Minus(customersWithOrders); // SELECT customer_id FROM customers // MINUS // SELECT DISTINCT customer_id FROM orders // Returns customers who have never ordered // Find skills not required for a position SubQuery requiredSkills = Filters.subQuery("SELECT skill_id FROM position_requirements WHERE position_id = 5"); Minus optionalSkills = new Minus(requiredSkills); // SELECT skill_id FROM all_skills // MINUS // SELECT skill_id FROM position_requirements WHERE position_id = 5 } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to perform the MINUS operation with. Must not be null. The subquery must have the same number of columns with compatible types as the main query.
- **See also:** Except, Union, UnionAll, Intersect

### Class NamedProperty (com.landawn.abacus.query.condition.NamedProperty)
A utility class that provides a fluent API for creating SQL conditions based on a property name.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### of(...) -> NamedProperty
- **Signature:** `public static NamedProperty of(final String propName)`
- **Summary:** Gets or creates a NamedProperty instance for the specified property name.
- **Parameters:**
  - `propName` (`String`) — the property name. Must not be null or empty.
- **Returns:** a cached or new NamedProperty instance

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public NamedProperty(final String propName)`
- **Summary:** Creates a NamedProperty with the specified property name.
- **Parameters:**
  - `propName` (`String`) — the property name. Must not be null.
##### propName(...) -> String
- **Signature:** `public String propName()`
- **Summary:** Returns the property name associated with this NamedProperty.
- **Parameters:**
  - (none)
- **Returns:** the property name
##### eq(...) -> Equal
- **Signature:** `public Equal eq(final Object value)`
- **Summary:** Creates an EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be of any type compatible with the property.
- **Returns:** an Equal condition for this property
- **See also:** Equal, Filters#eq(String, Object)
##### eqOr(...) -> Or
- **Signature:** `public Or eqOr(final Object... values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property.
- **Parameters:**
  - `values` (`Object[]`) — array of values to check equality against. Each value will be tested with OR logic.
- **Returns:** an Or condition containing multiple Equal conditions
- **See also:** Or, Equal
- **Signature:** `public Or eqOr(final Collection<?> values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property using a collection.
- **Contract:**
  - Useful when the values are already in a collection or list.
- **Parameters:**
  - `values` (`Collection<?>`) — collection of values to check equality against. Each value will be tested with OR logic.
- **Returns:** an Or condition containing multiple Equal conditions
- **See also:** Or, Equal
##### ne(...) -> NotEqual
- **Signature:** `public NotEqual ne(final Object value)`
- **Summary:** Creates a NOT EQUAL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is not equal to the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be of any type compatible with the property.
- **Returns:** a NotEqual condition for this property
- **See also:** NotEqual, Filters#ne(String, Object)
##### gt(...) -> GreaterThan
- **Signature:** `public GreaterThan gt(final Object value)`
- **Summary:** Creates a GREATER THAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is strictly greater than the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a GreaterThan condition for this property
- **See also:** GreaterThan, Filters#gt(String, Object)
##### ge(...) -> GreaterEqual
- **Signature:** `public GreaterEqual ge(final Object value)`
- **Summary:** Creates a GREATER THAN OR EQUAL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is greater than or equal to the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against (inclusive). Can be numeric, date, string, or any comparable type.
- **Returns:** a GreaterEqual condition for this property
- **See also:** GreaterEqual, Filters#ge(String, Object)
##### lt(...) -> LessThan
- **Signature:** `public LessThan lt(final Object value)`
- **Summary:** Creates a LESS THAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is strictly less than the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a LessThan condition for this property
- **See also:** LessThan, Filters#lt(String, Object)
##### le(...) -> LessEqual
- **Signature:** `public LessEqual le(final Object value)`
- **Summary:** Creates a LESS THAN OR EQUAL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is less than or equal to the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against (inclusive). Can be numeric, date, string, or any comparable type.
- **Returns:** a LessEqual condition for this property
- **See also:** LessEqual, Filters#le(String, Object)
##### isNull(...) -> IsNull
- **Signature:** `public IsNull isNull()`
- **Summary:** Creates an IS NULL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is NULL in the database.
- **Parameters:**
  - (none)
- **Returns:** an IsNull condition for this property
- **See also:** IsNull, Filters#isNull(String)
##### isNotNull(...) -> IsNotNull
- **Signature:** `public IsNotNull isNotNull()`
- **Summary:** Creates an IS NOT NULL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is not NULL in the database.
- **Parameters:**
  - (none)
- **Returns:** an IsNotNull condition for this property
- **See also:** IsNotNull, Filters#isNotNull(String)
##### between(...) -> Between
- **Signature:** `public Between between(final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value falls within the specified range (inclusive).
- **Parameters:**
  - `minValue` (`Object`) — the minimum value (inclusive). Can be numeric, date, string, or any comparable type.
  - `maxValue` (`Object`) — the maximum value (inclusive). Can be numeric, date, string, or any comparable type.
- **Returns:** a Between condition for this property
- **See also:** Between, Filters#between(String, Object, Object)
##### bt(...) -> Between
- **Signature:** `@Deprecated public Between bt(final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN condition for this property.
- **Parameters:**
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** a Between condition
##### like(...) -> Like
- **Signature:** `public Like like(final Object value)`
- **Summary:** Creates a LIKE condition for this property.
- **Parameters:**
  - `value` (`Object`) — the pattern to match (can include % for any characters and _ for single character)
- **Returns:** a Like condition for this property
- **See also:** Like, Filters#like(String, Object)
##### notLike(...) -> NotLike
- **Signature:** `public NotLike notLike(final Object value)`
- **Summary:** Creates a NOT LIKE condition for this property.
- **Parameters:**
  - `value` (`Object`) — the pattern to exclude (can include % for any characters and _ for single character)
- **Returns:** a NotLike condition for this property
- **See also:** NotLike, Filters#notLike(String, Object)
##### startsWith(...) -> Like
- **Signature:** `public Like startsWith(final Object value)`
- **Summary:** Creates a LIKE condition that matches values starting with the specified prefix.
- **Parameters:**
  - `value` (`Object`) — the prefix to match. The % wildcard will be automatically appended.
- **Returns:** a Like condition with % appended to the value
- **See also:** Like, Filters#startsWith(String, Object)
##### endsWith(...) -> Like
- **Signature:** `public Like endsWith(final Object value)`
- **Summary:** Creates a LIKE condition that matches values ending with the specified suffix.
- **Parameters:**
  - `value` (`Object`) — the suffix to match. The % wildcard will be automatically prepended.
- **Returns:** a Like condition with % prepended to the value
- **See also:** Like, Filters#endsWith(String, Object)
##### contains(...) -> Like
- **Signature:** `public Like contains(final Object value)`
- **Summary:** Creates a LIKE condition that matches values containing the specified substring.
- **Parameters:**
  - `value` (`Object`) — the substring to match. The % wildcard will be automatically added to both sides.
- **Returns:** a Like condition with % on both sides of the value
- **See also:** Like, Filters#contains(String, Object)
##### in(...) -> In
- **Signature:** `public In in(final Object... values)`
- **Summary:** Creates an IN condition for this property with an array of values.
- **Contract:**
  - This generates a condition that checks if the property value matches any of the specified values.
- **Parameters:**
  - `values` (`Object[]`) — array of values to check membership against
- **Returns:** an In condition for this property
- **See also:** In, Filters#in(String, Object\[\])
- **Signature:** `public In in(final Collection<?> values)`
- **Summary:** Creates an IN condition for this property with a collection of values.
- **Contract:**
  - Useful when the values are already in a collection or list.
- **Parameters:**
  - `values` (`Collection<?>`) — collection of values to check membership against
- **Returns:** an In condition for this property
- **See also:** In, Filters#in(String, Collection)
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this NamedProperty based on the property name.
- **Parameters:**
  - (none)
- **Returns:** hash code of the property name
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this NamedProperty is equal to another object.
- **Contract:**
  - Checks if this NamedProperty is equal to another object.
  - Two NamedProperty instances are equal if they have the same property name.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal (same property name), {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns the string representation of this NamedProperty.
- **Parameters:**
  - (none)
- **Returns:** the property name

### Class NaturalJoin (com.landawn.abacus.query.condition.NaturalJoin)
Represents a NATURAL JOIN clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public NaturalJoin(final String joinEntity)`
- **Summary:** Creates a NATURAL JOIN clause for the specified table or entity.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // If 'orders' and 'customers' both have 'customer_id' column NaturalJoin join = new NaturalJoin("customers"); // Generates: NATURAL JOIN customers // Automatically joins on orders.customer_id = customers.customer_id } </pre>
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public NaturalJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a NATURAL JOIN clause with an additional condition.
- **Contract:**
  - This is useful when you want to combine the automatic column matching of natural join with specific filtering criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `condition` (`Condition`) — an additional filter condition applied after the natural join; any {@link Condition} is allowed and can be {@code null} .
- **Signature:** `public NaturalJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a NATURAL JOIN clause with multiple tables/entities and a condition.
- **Contract:**
  - <p> When joining multiple tables, the natural join is performed sequentially.
  - Care must be taken to ensure the intended columns are matched, especially with multiple tables.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `condition` (`Condition`) — an additional filter condition applied after the natural join; any {@link Condition} is allowed and can be {@code null} .

### Class Not (com.landawn.abacus.query.condition.Not)
Represents a NOT logical operator in SQL conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Not(final Condition condition)`
- **Summary:** Creates a new NOT condition that negates the specified condition.
- **Contract:**
  - The resulting condition will be true when the input condition is false, and false when the input condition is true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to be negated. Can be any type of condition including simple comparisons, complex logical conditions, or subquery conditions. Must not be null.

### Class NotBetween (com.landawn.abacus.query.condition.NotBetween)
Represents a NOT BETWEEN condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public NotBetween(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a NOT BETWEEN condition for the specified property and range.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `minValue` (`Object`) — the minimum value of the range to exclude (can be null, literal value, or subquery)
  - `maxValue` (`Object`) — the maximum value of the range to exclude (can be null, literal value, or subquery)
##### getPropName(...) -> String
- **Signature:** `public String getPropName()`
- **Summary:** Gets the property name for this NOT BETWEEN condition.
- **Parameters:**
  - (none)
- **Returns:** the property name
##### getMinValue(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T> T getMinValue()`
- **Summary:** Gets the minimum value of the range to exclude.
- **Parameters:**
  - (none)
- **Returns:** the minimum value of the excluded range
##### setMinValue(...) -> void
- **Signature:** `@Deprecated public void setMinValue(final Object minValue)`
- **Summary:** Sets a new minimum value for the range.
- **Contract:**
  - Note: Modifying conditions after creation is not recommended as they should be immutable.
  - This method exists for backward compatibility but should be avoided in new code.
- **Parameters:**
  - `minValue` (`Object`) — the new minimum value to set
##### getMaxValue(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T> T getMaxValue()`
- **Summary:** Gets the maximum value of the range to exclude.
- **Parameters:**
  - (none)
- **Returns:** the maximum value of the excluded range
##### setMaxValue(...) -> void
- **Signature:** `@Deprecated public void setMaxValue(final Object maxValue)`
- **Summary:** Sets a new maximum value for the range.
- **Contract:**
  - Note: Modifying conditions after creation is not recommended as they should be immutable.
  - This method exists for backward compatibility but should be avoided in new code.
- **Parameters:**
  - `maxValue` (`Object`) — the new maximum value to set
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the list of parameters for this condition.
- **Contract:**
  - If min/max values are themselves conditions (like subqueries), their parameters are included.
- **Parameters:**
  - (none)
- **Returns:** list containing the min and max values or their parameters
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears all parameter values by setting them to null to free memory.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") @Override public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this NOT BETWEEN condition.
- **Contract:**
  - If min/max values are conditions themselves (like expressions or subqueries), they are also copied to ensure complete independence.
- **Parameters:**
  - (none)
- **Returns:** a new instance with copied values
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this NOT BETWEEN condition to its string representation.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name
- **Returns:** string representation of the NOT BETWEEN condition
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this NOT BETWEEN condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name, operator, and range values
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this NOT BETWEEN condition is equal to another object.
- **Contract:**
  - Checks if this NOT BETWEEN condition is equal to another object.
  - Two NOT BETWEEN conditions are equal if they have the same property name, operator, and range boundaries (both min and max values).
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

### Class NotEqual (com.landawn.abacus.query.condition.NotEqual)
Represents a NOT EQUAL (!= or &lt; &gt; ) condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public NotEqual(final String propName, final Object propValue)`
- **Summary:** Creates a new NotEqual condition.
- **Contract:**
  - This condition will evaluate to true when the property value is not equal to the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `propValue` (`Object`) — the value to compare against (can be null, literal value, or subquery)

### Class NotExists (com.landawn.abacus.query.condition.NotExists)
Represents the SQL NOT EXISTS operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public NotExists(final SubQuery condition)`
- **Summary:** Creates a new NOT EXISTS condition with the specified subquery.
- **Contract:**
  - The condition evaluates to true when the subquery returns no rows.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery to check for non-existence of rows (must not be null)
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`)
- **Returns:** unspecified

### Class NotIn (com.landawn.abacus.query.condition.NotIn)
Represents a NOT IN condition in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public NotIn(final String propName, final Collection<?> values)`
- **Summary:** Creates a NOT IN condition for the specified property and collection of values.
- **Parameters:**
  - `propName` (`String`) — the property/column name. Must not be null or empty.
  - `values` (`Collection<?>`) — the collection of values that the property should NOT match. The collection is copied internally to ensure immutability.
##### getPropName(...) -> String
- **Signature:** `public String getPropName()`
- **Summary:** Gets the property name for this NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** the property name, or {@code null} for an uninitialized instance created by serialization frameworks
##### getValues(...) -> List<?>
- **Signature:** `public List<?> getValues()`
- **Summary:** Gets the collection of values that the property should NOT match.
- **Contract:**
  - Gets the collection of values that the property should NOT match.
  - These are the values that will be excluded when the query is executed.
- **Parameters:**
  - (none)
- **Returns:** list of values to exclude (may be null if cleared)
##### setValues(...) -> void
- **Signature:** `@Deprecated public void setValues(final List<?> values)`
- **Summary:** Sets new values for this NOT IN condition.
- **Contract:**
  - However, modifying conditions after creation is strongly discouraged as conditions should be treated as immutable to ensure thread safety and predictable behavior.
- **Parameters:**
  - `values` (`List<?>`) — the new collection of values to exclude. Must not be null or empty.
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the parameter values for this condition.
- **Contract:**
  - Returns the values that should be excluded when the query is executed.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values, or empty list if values is null
##### clearParameters(...) -> void
- **Signature:** `@SuppressWarnings("rawtypes") @Override public void clearParameters()`
- **Summary:** Clears all parameter values by setting them to null to free memory.
- **Contract:**
  - Use this method to release large objects when the condition is no longer needed.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") @Override public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** a new instance with copied values
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this NOT IN condition to its string representation using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name
- **Returns:** string representation of the NOT IN condition
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name, operator, and values
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this NOT IN condition is equal to another object.
- **Contract:**
  - Checks if this NOT IN condition is equal to another object.
  - Two NOT IN conditions are equal if they have the same property name, operator, and values list.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

### Class NotInSubQuery (com.landawn.abacus.query.condition.NotInSubQuery)
Represents a NOT IN subquery condition used in SQL WHERE clauses.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public NotInSubQuery(final String propName, final SubQuery subQuery)`
- **Summary:** Creates a NOT IN subquery condition for a single property.
- **Contract:**
  - This checks if the property value is not present in the subquery results.
  - <p> Use this constructor when comparing a single column against a subquery that returns a single column of values.
- **Parameters:**
  - `propName` (`String`) — the property/column name. Must not be null or empty.
  - `subQuery` (`SubQuery`) — the subquery that returns the values to check against. Must not be null.
- **Signature:** `public NotInSubQuery(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates a NOT IN subquery condition for multiple properties.
- **Contract:**
  - The number and order of properties must match the columns returned by the subquery.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property names to check against the subquery results. Must not be null or empty.
  - `subQuery` (`SubQuery`) — the subquery that returns the values to check against. Must not be null. Must return the same number of columns as propNames.size().
##### getPropName(...) -> String
- **Signature:** `public String getPropName()`
- **Summary:** Gets the property name for single-property NOT IN conditions.
- **Contract:**
  - Returns null if this is a multi-property condition.
- **Parameters:**
  - (none)
- **Returns:** the property name, or null if this is a multi-property condition
##### getPropNames(...) -> Collection<String>
- **Signature:** `public Collection<String> getPropNames()`
- **Summary:** Gets the property names for multi-property NOT IN conditions.
- **Contract:**
  - Returns null if this is a single-property condition.
- **Parameters:**
  - (none)
- **Returns:** collection of property names, or null if this is a single-property condition
##### getSubQuery(...) -> SubQuery
- **Signature:** `public SubQuery getSubQuery()`
- **Summary:** Gets the subquery used in this NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** the subquery
##### setSubQuery(...) -> void
- **Signature:** `@Deprecated public void setSubQuery(final SubQuery subQuery)`
- **Summary:** Sets a new subquery for this NOT IN condition.
- **Contract:**
  - However, modifying conditions after creation is strongly discouraged as conditions should be treated as immutable to ensure thread safety and predictable behavior.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the new subquery to set. Must not be null.
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the list of parameters from the subquery.
- **Contract:**
  - These are the parameter values that will be bound to the prepared statement placeholders when the query is executed.
- **Parameters:**
  - (none)
- **Returns:** list of parameter values from the subquery
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears parameters in the underlying subquery.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") @Override public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this NOT IN subquery condition.
- **Parameters:**
  - (none)
- **Returns:** a new NotInSubQuery instance with a deep copy of the subquery
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this NOT IN subquery condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name(s), operator, and subquery
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this NOT IN subquery condition is equal to another object.
- **Contract:**
  - Checks if this NOT IN subquery condition is equal to another object.
  - Two NotInSubQuery conditions are equal if they have the same property name(s), operator, and subquery.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this NOT IN subquery condition to its string representation using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names
- **Returns:** string representation of the NOT IN subquery condition

### Class NotLike (com.landawn.abacus.query.condition.NotLike)
Represents a NOT LIKE condition in SQL queries for pattern exclusion.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public NotLike(final String propName, final Object propValue)`
- **Summary:** Creates a new NOT LIKE condition for the specified property and pattern.
- **Contract:**
  - The condition evaluates to true when the property value does not match the given pattern.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be null or empty)
  - `propValue` (`Object`) — the pattern to match against (can be null, literal value, or subquery). Can include % and _ wildcards.

### Class On (com.landawn.abacus.query.condition.On)
Represents an ON clause used in SQL JOIN operations.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public On(final Condition condition)`
- **Summary:** Creates an ON clause with a custom condition.
- **Parameters:**
  - `condition` (`Condition`) — the join condition, can be any type of condition including Expression, Equal, And, Or, Between, or more complex conditions
- **Signature:** `public On(final String propName, final String anotherPropName)`
- **Summary:** Creates an ON clause for simple column equality between tables.
- **Parameters:**
  - `propName` (`String`) — the column name from the first table (can include table name/alias)
  - `anotherPropName` (`String`) — the column name from the second table (can include table name/alias)
- **Signature:** `public On(final Map<String, String> propNamePair)`
- **Summary:** Creates an ON clause with multiple column equality conditions.
- **Contract:**
  - This is useful for composite key joins or when multiple columns must match between tables.
- **Parameters:**
  - `propNamePair` (`Map<String, String>`) — map of column pairs where key is from first table, value is from second table. Order is preserved if LinkedHashMap is used.

### Enum Operator (com.landawn.abacus.query.condition.Operator)
Enumeration of SQL operators supported by the condition framework.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### getOperator(...) -> Operator
- **Signature:** `public static Operator getOperator(final String name)`
- **Summary:** Gets an Operator by its string representation.
- **Parameters:**
  - `name` (`String`) — the string representation of the operator. Can be null.
- **Returns:** the corresponding Operator enum value, or {@code null} if name is null or not found

#### Public Instance Methods
##### getName(...) -> String
- **Signature:** `public String getName()`
- **Summary:** Gets the SQL string representation of this operator.
- **Parameters:**
  - (none)
- **Returns:** the SQL string representation of this operator (e.g., "=", "AND", "LIKE")
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns the SQL string representation of this operator.
- **Parameters:**
  - (none)
- **Returns:** the SQL string representation of this operator (e.g., "=", "AND", "LIKE")

### Class Or (com.landawn.abacus.query.condition.Or)
Represents a logical OR condition that combines multiple conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Or(final Condition... conditions)`
- **Summary:** Creates a new OR condition with the specified conditions.
- **Parameters:**
  - `conditions` (`Condition[]`) — the conditions to combine with OR logic
- **Signature:** `public Or(final Collection<? extends Condition> conditions)`
- **Summary:** Creates a new OR condition with a collection of conditions.
- **Contract:**
  - This constructor is useful when conditions are built dynamically.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with OR logic
##### or(...) -> Or
- **Signature:** `@Override public Or or(final Condition condition)`
- **Summary:** Creates a new Or condition by adding another condition to this OR.
- **Contract:**
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Build condition step by step Or or = new Or(Filters.eq("type", "A")) .or(Filters.eq("type", "B")) .or(Filters.eq("type", "C")); // Results in: ((type = 'A') OR (type = 'B') OR (type = 'C')) // Add conditions conditionally Or baseOr = new Or(Filters.eq("status", "active")); if (includeInactive) { baseOr = baseOr.or(Filters.eq("status", "inactive")); } if (includePending) { baseOr = baseOr.or(Filters.eq("status", "pending")); } // Results vary based on flags } </pre>
- **Parameters:**
  - `condition` (`Condition`) — the condition to add to this OR. Must not be null.
- **Returns:** a new Or condition containing all existing conditions plus the new one

### Class OrderBy (com.landawn.abacus.query.condition.OrderBy)
Represents an ORDER BY clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public OrderBy(final Condition condition)`
- **Summary:** Creates an ORDER BY clause with a custom condition.
- **Contract:**
  - <p> Use this constructor when you need to order by calculated values, case expressions, or other complex SQL expressions.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Order by CASE expression Condition expr = Filters.expr("CASE WHEN status='urgent' THEN 1 ELSE 2 END"); OrderBy orderBy = new OrderBy(expr); // SQL: ORDER BY CASE WHEN status='urgent' THEN 1 ELSE 2 END // Order by calculated field Condition calcExpr = Filters.expr("(price * quantity) DESC"); OrderBy totalOrder = new OrderBy(calcExpr); // SQL: ORDER BY (price * quantity) DESC } </pre>
- **Parameters:**
  - `condition` (`Condition`) — the ordering condition. Must not be null.
- **Signature:** `public OrderBy(final String... propNames)`
- **Summary:** Creates an ORDER BY clause with multiple property names.
- **Parameters:**
  - `propNames` (`String[]`) — variable number of property names to sort by. Must not be null or empty.
- **Signature:** `public OrderBy(final String propName, final SortDirection direction)`
- **Summary:** Creates an ORDER BY clause with a single property and sort direction.
- **Parameters:**
  - `propName` (`String`) — the property name to sort by. Must not be null or empty.
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC). Must not be null.
- **Signature:** `public OrderBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates an ORDER BY clause with multiple properties and a single sort direction.
- **Contract:**
  - <p> This is useful when you want to sort by multiple columns in the same direction, such as sorting multiple date fields in descending order.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property names to sort by. Must not be null or empty.
  - `direction` (`SortDirection`) — the sort direction to apply to all properties. Must not be null.
- **Signature:** `public OrderBy(final Map<String, SortDirection> orders)`
- **Summary:** Creates an ORDER BY clause with properties having different sort directions.
- **Contract:**
  - <p> The map should maintain insertion order (use LinkedHashMap) to ensure predictable sort priority.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — should be a {@code LinkedHashMap} to preserve insertion order. Maps property names to their respective sort directions. Must not be null or empty.

### Class RightJoin (com.landawn.abacus.query.condition.RightJoin)
Represents a RIGHT JOIN clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public RightJoin(final String joinEntity)`
- **Summary:** Creates a RIGHT JOIN clause for the specified table or entity.
- **Contract:**
  - This creates a join without an ON condition, which may need to be specified separately or will use implicit join conditions based on foreign key relationships (if supported by the database).
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public RightJoin(final String joinEntity, final Condition condition)`
- **Summary:** Creates a RIGHT JOIN clause with a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .
- **Signature:** `public RightJoin(final Collection<String> joinEntities, final Condition condition)`
- **Summary:** Creates a RIGHT JOIN clause with multiple tables/entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `condition` (`Condition`) — the join condition, typically an {@link On} condition for column equality; any {@link Condition} is allowed and can be {@code null} .

### Class Some (com.landawn.abacus.query.condition.Some)
Represents the SQL SOME operator for use with subqueries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Some(final SubQuery condition)`
- **Summary:** Creates a new SOME condition with the specified subquery.
- **Contract:**
  - The SOME operator must be used with a comparison operator in the containing condition.
- **Parameters:**
  - `condition` (`SubQuery`) — the subquery that returns values to compare against. Must not be null.
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`)
- **Returns:** unspecified

### Class SubQuery (com.landawn.abacus.query.condition.SubQuery)
Represents a subquery that can be used within SQL conditions.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public SubQuery(final String sql)`
- **Summary:** Creates a subquery with raw SQL.
- **Parameters:**
  - `sql` (`String`) — the SQL SELECT statement
- **Signature:** `public SubQuery(final String entityName, final String sql)`
- **Summary:** Creates a subquery with an entity name and raw SQL.
- **Contract:**
  - The entity name is for reference only when using raw SQL and doesn't affect the query.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name (can be empty)
  - `sql` (`String`) — the SQL SELECT statement
- **Signature:** `public SubQuery(final String entityName, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a structured subquery with entity name, selected properties, and condition.
- **Contract:**
  - If the condition is not already a clause (like WHERE), it will be automatically wrapped in a WHERE clause.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name
  - `propNames` (`Collection<String>`) — collection of property names to select
  - `condition` (`Condition`) — the WHERE condition (if it's not already a clause, it will be wrapped in WHERE)
- **Signature:** `public SubQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a structured subquery with entity class, selected properties, and condition.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `propNames` (`Collection<String>`) — collection of property names to select
  - `condition` (`Condition`) — the WHERE condition (if it's not already a clause, it will be wrapped in WHERE)
##### getSql(...) -> String
- **Signature:** `public String getSql()`
- **Summary:** Returns the raw SQL script if this is a raw SQL subquery.
- **Contract:**
  - Returns the raw SQL script if this is a raw SQL subquery.
- **Parameters:**
  - (none)
- **Returns:** the SQL script, or {@code null} if this is a structured subquery
##### getEntityName(...) -> String
- **Signature:** `public String getEntityName()`
- **Summary:** Gets the entity/table name for this subquery.
- **Parameters:**
  - (none)
- **Returns:** the entity/table name, or an empty string if not set
##### getEntityClass(...) -> Class<?>
- **Signature:** `public Class<?> getEntityClass()`
- **Summary:** Gets the entity class if this subquery was created with a class reference.
- **Contract:**
  - Gets the entity class if this subquery was created with a class reference.
- **Parameters:**
  - (none)
- **Returns:** the entity class, or {@code null} if created with entity name string or raw SQL
##### getSelectPropNames(...) -> Collection<String>
- **Signature:** `public Collection<String> getSelectPropNames()`
- **Summary:** Gets the collection of property names to select in this subquery.
- **Parameters:**
  - (none)
- **Returns:** unmodifiable collection of property names to select, or {@code null} for raw SQL subqueries
##### getCondition(...) -> Condition
- **Signature:** `public Condition getCondition()`
- **Summary:** Gets the WHERE condition for this subquery.
- **Contract:**
  - This condition is applied when generating the SQL for structured subqueries.
- **Parameters:**
  - (none)
- **Returns:** the WHERE condition, or {@code null} if no condition or raw SQL subquery
##### getParameters(...) -> List<Object>
- **Signature:** `@Override public List<Object> getParameters()`
- **Summary:** Gets the list of parameter values from the condition.
- **Contract:**
  - These are the parameter values that will be bound to the prepared statement placeholders when the query is executed.
- **Parameters:**
  - (none)
- **Returns:** list of parameter values, or an empty list if no condition or raw SQL subquery
##### clearParameters(...) -> void
- **Signature:** `@Override public void clearParameters()`
- **Summary:** Clears parameters in the underlying condition.
- **Contract:**
  - <p> Use this method to release large objects when the subquery is no longer needed.
  - If this is a raw SQL subquery with no condition, this method is a no-op.
- **Parameters:**
  - (none)
##### copy(...) -> T
- **Signature:** `@Override @SuppressWarnings("unchecked") public <T extends Condition> T copy()`
- **Summary:** Creates a deep copy of this subquery.
- **Parameters:**
  - (none)
- **Returns:** a new SubQuery instance with deeply copied values
##### toString(...) -> String
- **Signature:** `@Override public String toString(final NamingPolicy namingPolicy)`
- **Summary:** Converts this subquery to its string representation.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to column and table names. Can be null.
- **Returns:** string representation of the subquery
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this subquery.
- **Parameters:**
  - (none)
- **Returns:** hash code based on sql, entity name/class, properties, and condition
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this subquery is equal to another object.
- **Contract:**
  - Checks if this subquery is equal to another object.
  - Two subqueries are equal if they have the same SQL (for raw queries) or the same entity name/class, properties, and condition (for structured queries).
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

### Class Union (com.landawn.abacus.query.condition.Union)
Represents a UNION clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Union(final SubQuery subQuery)`
- **Summary:** Creates a new UNION clause with the specified subquery.
- **Contract:**
  - Both queries must have the same number of columns with compatible data types.
  - <p> The UNION operator is useful when you need to merge data from different sources or conditions while ensuring result uniqueness.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Combine customers from different regions SubQuery eastCustomers = Filters.subQuery("SELECT customer_id, name FROM customers WHERE region = 'East'"); Union union = new Union(eastCustomers); // When added to a query that has West region: // SELECT customer_id, name FROM customers WHERE region = 'West' // UNION // SELECT customer_id, name FROM customers WHERE region = 'East' // Duplicates are automatically removed // Merge active and inactive users SubQuery inactiveUsers = Filters.subQuery("SELECT user_id, email FROM inactive_users"); Union allUsers = new Union(inactiveUsers); // When combined with active users query, returns complete list without duplicates // Combine current and historical orders SubQuery historicalOrders = Filters.subQuery("SELECT order_id, total FROM archived_orders"); Union allOrders = new Union(historicalOrders); // When combined with current orders query, removes any duplicate order_id entries } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to perform the UNION operation with. Must not be null. The subquery must have the same number of columns with compatible types as the main query.
- **See also:** UnionAll, Intersect, Except, Minus

### Class UnionAll (com.landawn.abacus.query.condition.UnionAll)
Represents a UNION ALL clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public UnionAll(final SubQuery subQuery)`
- **Summary:** Creates a new UNION ALL clause with the specified subquery.
- **Contract:**
  - <p> The subquery must return the same number of columns as the main query, and the data types must be compatible.
  - </p> <p> Use UNION ALL when you know there are no duplicates in the combined result, or when you want to preserve all rows regardless of duplication.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Combine orders from multiple regions SubQuery eastOrders = Filters.subQuery("SELECT order_id, amount FROM orders WHERE region = 'EAST'"); UnionAll allOrders = new UnionAll(eastOrders); // When combined with West region query: // SELECT order_id, amount FROM orders WHERE region = 'WEST' // UNION ALL // SELECT order_id, amount FROM orders WHERE region = 'EAST' // Keeps all orders, including any duplicates // Merge current and archived transactions SubQuery archivedTxns = Filters.subQuery("SELECT txn_id, date, amount FROM archived_transactions"); UnionAll allTxns = new UnionAll(archivedTxns); // Combines with current transactions, preserving all records // Combine data from partitioned tables SubQuery q1Data = Filters.subQuery("SELECT * FROM sales_q1"); SubQuery q2Data = Filters.subQuery("SELECT * FROM sales_q2"); UnionAll allSales = new UnionAll(q2Data); // Efficiently combines quarterly data without duplicate check } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to perform the UNION ALL operation with. Must not be null. The subquery must have the same number of columns with compatible types as the main query.
- **See also:** Union, Intersect, Except, Minus

### Class Using (com.landawn.abacus.query.condition.Using)
Represents a USING clause in SQL JOIN operations.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Using(final String... columnNames)`
- **Summary:** Creates a USING clause with the specified column names.
- **Contract:**
  - The columns must exist with identical names in both tables being joined.
- **Parameters:**
  - `columnNames` (`String[]`) — variable number of column names to join on. All columns must exist in both tables with identical names. Must not be null or empty.
- **Signature:** `public Using(final Collection<String> columnNames)`
- **Summary:** Creates a USING clause with a collection of column names.
- **Contract:**
  - This constructor is useful when column names are determined dynamically or retrieved from metadata/configuration.
- **Parameters:**
  - `columnNames` (`Collection<String>`) — collection of column names to join on. Must not be null or empty. Order matters for some databases. Use LinkedHashSet or List to preserve order.

### Class Where (com.landawn.abacus.query.condition.Where)
Represents a WHERE clause in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Where(final Condition condition)`
- **Summary:** Creates a WHERE clause with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition to apply in the WHERE clause. Must not be null.

### Class XOR (com.landawn.abacus.query.condition.XOR)
Represents an XOR (exclusive OR) logical operator in SQL queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public XOR(final String propName, final Object propValue)`
- **Summary:** Creates a new XOR condition that evaluates to true when exactly one of the operands is true.
- **Contract:**
  - Creates a new XOR condition that evaluates to true when exactly one of the operands is true.
  - <p> The XOR condition is true when the property value and the provided value differ in their boolean evaluation (one true, one false).
  - This is useful for enforcing business rules where only one of two options should be active.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Note: XOR is a binary operator in SQL, typically used with boolean expressions // The exact SQL syntax varies by database // MySQL-style XOR - ensure exactly one condition is true XOR exclusiveAuth = new XOR("usePasswordAuth", true); // SQL: usePasswordAuth XOR true // This evaluates to true when usePasswordAuth != true // XOR with numeric values (treated as boolean: 0=false, non-zero=true) XOR xorCheck = new XOR("flagA", 1); // SQL: flagA XOR 1 // Database compatibility note: // MySQL: Native XOR operator support // Other databases: May need to be expanded to (A AND NOT B) OR (NOT A AND B) // Consider using AND/OR/NOT combinations for better portability: // Or portableXor = new Or( // new And(condition1, new Not(condition2)), // new And(new Not(condition1), condition2) // ); } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name. Must not be null or empty.
  - `propValue` (`Object`) — the value to compare against. Can be null, a literal value, Expression, or SubQuery.

