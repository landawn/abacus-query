# abacus-query API Index (v4.8.8)
- Build: unknown
- Java: 17
- Generated: 2026-07-13

## Packages
- com.landawn.abacus.query
- com.landawn.abacus.query.condition

## com.landawn.abacus.query
### Class AbstractQueryBuilder (com.landawn.abacus.query.AbstractQueryBuilder)
Base class for fluent SQL builders.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### setHandlerForNamedParameter(...) -> void
- **Signature:** `public static void setHandlerForNamedParameter(final BiConsumer<StringBuilder, String> handlerForNamedParameter)`
- **Summary:** Sets a custom handler for formatting named parameters in SQL strings.
- **Contract:**
  - Each builder snapshots the handler in its constructor, so calling this method only affects builders created afterwards on the calling thread; builders that already exist keep the handler that was in effect when they were created.
  - The handler should be deterministic, side-effect free, and make its output depend only on the supplied parameter name: compound-query assembly may invoke it again with a suffixed name when a sibling query's parameter name must be made unique.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Use MyBatis-style named parameters: #{paramName} AbstractQueryBuilder.setHandlerForNamedParameter( (sb, propName) -> sb.append("#{").append(propName).append("}")); // Reset to default when done AbstractQueryBuilder.resetHandlerForNamedParameter(); } </pre>
- **Parameters:**
  - `handlerForNamedParameter` (`BiConsumer<StringBuilder, String>`) — the handler to format named parameters; must not be null
##### resetHandlerForNamedParameter(...) -> void
- **Signature:** `public static void resetHandlerForNamedParameter()`
- **Summary:** Resets the named parameter handler to the default format.
- **Parameters:**
  - (none)

#### Public Instance Methods
##### sqlDialect(...) -> SqlDialect
- **Signature:** `public SqlDialect sqlDialect()`
- **Summary:** Returns the {@link SqlDialect} this builder renders SQL with.
- **Parameters:**
  - (none)
- **Returns:** the dialect (naming policy, parameter style, identifier quote and optional product info) bound to this builder
##### into(...) -> This
- **Signature:** `public This into(final String tableName)`
- **Summary:** Specifies the target table for an {@code INSERT} or {@code INSERT ... SELECT} operation.
- **Contract:**
  - <p> Must be called after setting the columns/values via {@code insert(...)} or the columns to copy via {@code select(...)} .
  - When chained after {@code select(...)} , the eventual {@code from(...)} call appends the source query, producing {@code INSERT INTO target (cols) SELECT cols FROM source} .
- **Parameters:**
  - `tableName` (`String`) — the name of the target table (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This into(final Class<?> entityClass)`
- **Summary:** Specifies the target table for an {@code INSERT} or {@code INSERT ... SELECT} operation using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the target table
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This into(final String tableName, final Class<?> entityClass)`
- **Summary:** Specifies the target table for an {@code INSERT} or {@code INSERT ... SELECT} operation with an explicit table name and entity class.
- **Parameters:**
  - `tableName` (`String`) — the name of the target table (must not be {@code null} , empty, or blank)
  - `entityClass` (`Class<?>`) — the entity class for property mapping (may be {@code null} )
- **Returns:** this SqlBuilder instance for method chaining
##### distinct(...) -> This
- **Signature:** `public This distinct()`
- **Summary:** Adds DISTINCT clause to the SELECT statement.
- **Parameters:**
  - (none)
- **Returns:** this SqlBuilder instance for method chaining
##### selectModifier(...) -> This
- **Signature:** `public This selectModifier(final String selectModifier)`
- **Summary:** Adds a pre-select modifier to the SELECT statement.
- **Contract:**
  - <p> For better performance, this method should be called before {@code from} .
- **Parameters:**
  - `selectModifier` (`String`) — modifiers like {@code ALL} , {@code DISTINCT} , {@code DISTINCTROW} , {@code TOP} , etc.; may be {@code null} or empty (no-op)
- **Returns:** this SqlBuilder instance for method chaining
##### from(...) -> This
- **Signature:** `public This from(final String... tableNames)`
- **Summary:** Sets the FROM clause with multiple table names.
- **Parameters:**
  - `tableNames` (`String[]`) — the table names to use in the FROM clause (must not be {@code null} or empty, and no element may be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This from(final Collection<String> tableNames)`
- **Summary:** Sets the FROM clause with a collection of table names.
- **Parameters:**
  - `tableNames` (`Collection<String>`) — the collection of table names to use in the FROM clause (must not be {@code null} or empty, and no element may be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This from(final String expr)`
- **Summary:** Sets the FROM clause with a single expression.
- **Parameters:**
  - `expr` (`String`) — the FROM clause expression
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This from(final String expr, final Class<?> entityClass)`
- **Summary:** Sets the FROM clause with an expression and associates it with an entity class.
- **Parameters:**
  - `expr` (`String`) — the FROM clause expression
  - `entityClass` (`Class<?>`) — the entity class for property mapping (may be {@code null} , in which case no entity-class association is performed)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This from(final Class<?> entityClass)`
- **Summary:** Sets the FROM clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This from(final Class<?> entityClass, final String alias)`
- **Summary:** Sets the FROM clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
##### join(...) -> This
- **Signature:** `public This join(final String joinExpr)`
- **Summary:** Adds a JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the {@code ON} clause if present, e.g. {@code "orders o ON u.id = o.user_id"} (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This join(final Class<?> entityClass)`
- **Summary:** Adds a JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This join(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
##### innerJoin(...) -> This
- **Signature:** `public This innerJoin(final String joinExpr)`
- **Summary:** Adds an INNER JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the {@code ON} clause if present (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This innerJoin(final Class<?> entityClass)`
- **Summary:** Adds an INNER JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This innerJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds an INNER JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
##### leftJoin(...) -> This
- **Signature:** `public This leftJoin(final String joinExpr)`
- **Summary:** Adds a LEFT JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the {@code ON} clause if present (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This leftJoin(final Class<?> entityClass)`
- **Summary:** Adds a LEFT JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This leftJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a LEFT JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
##### rightJoin(...) -> This
- **Signature:** `public This rightJoin(final String joinExpr)`
- **Summary:** Adds a RIGHT JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the {@code ON} clause if present (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This rightJoin(final Class<?> entityClass)`
- **Summary:** Adds a RIGHT JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This rightJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a RIGHT JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
##### fullJoin(...) -> This
- **Signature:** `public This fullJoin(final String joinExpr)`
- **Summary:** Adds a FULL JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the full join expression, including the {@code ON} clause if present (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This fullJoin(final Class<?> entityClass)`
- **Summary:** Adds a FULL JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This fullJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a FULL JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
##### crossJoin(...) -> This
- **Signature:** `public This crossJoin(final String joinExpr)`
- **Summary:** Adds a CROSS JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the join expression (a table reference, optionally with alias; a {@code CROSS JOIN} takes no {@code ON} clause) (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This crossJoin(final Class<?> entityClass)`
- **Summary:** Adds a CROSS JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This crossJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a CROSS JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
##### naturalJoin(...) -> This
- **Signature:** `public This naturalJoin(final String joinExpr)`
- **Summary:** Adds a NATURAL JOIN clause to the SQL statement.
- **Parameters:**
  - `joinExpr` (`String`) — the join expression (a table reference, optionally with alias; a {@code NATURAL JOIN} takes no {@code ON} clause) (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This naturalJoin(final Class<?> entityClass)`
- **Summary:** Adds a NATURAL JOIN clause using an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This naturalJoin(final Class<?> entityClass, final String alias)`
- **Summary:** Adds a NATURAL JOIN clause using an entity class with an alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to join
  - `alias` (`String`) — the table alias
- **Returns:** this SqlBuilder instance for method chaining
##### on(...) -> This
- **Signature:** `public This on(final String expr)`
- **Summary:** Adds an ON clause for join conditions.
- **Parameters:**
  - `expr` (`String`) — the join condition expression (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This on(final String... exprs)`
- **Summary:** Adds an ON clause for a composite join condition, joining the given expressions with {@code AND} .
- **Parameters:**
  - `exprs` (`String[]`) — the join condition expressions (must not be {@code null} or empty, and no element may be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This on(final Condition condition)`
- **Summary:** Adds an ON clause with a condition object for join conditions.
- **Parameters:**
  - `condition` (`Condition`) — the join condition (must not be {@code null} )
- **Returns:** this SqlBuilder instance for method chaining
##### using(...) -> This
- **Signature:** `public This using(final String expr)`
- **Summary:** Adds a USING clause for join conditions.
- **Parameters:**
  - `expr` (`String`) — the column name(s) for the USING clause
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This using(final String... propOrColumnNames)`
- **Summary:** Adds a USING clause with multiple columns for join conditions.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names for the USING clause (must not be {@code null} or empty, and no element may be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This using(final Collection<String> propOrColumnNames)`
- **Summary:** Adds a USING clause with a collection of columns for join conditions.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of property or column names for the USING clause (must not be {@code null} or empty, and no element may be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
##### where(...) -> This
- **Signature:** `public This where(final String expr)`
- **Summary:** Adds a WHERE clause with a string expression.
- **Parameters:**
  - `expr` (`String`) — the WHERE condition expression (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This where(final Condition condition)`
- **Summary:** Adds a WHERE clause with a condition object.
- **Parameters:**
  - `condition` (`Condition`) — the WHERE condition (must not be {@code null} )
- **Returns:** this SqlBuilder instance for method chaining
- **See also:** Filters
##### groupByAsc(...) -> This
- **Signature:** `@Beta public This groupByAsc(final String propOrColumnName)`
- **Summary:** Adds a GROUP BY ASC clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This groupByAsc(final String... propOrColumnNames)`
- **Summary:** Adds a GROUP BY ASC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to group by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This groupByAsc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds a GROUP BY ASC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by ascending
- **Returns:** this SqlBuilder instance for method chaining
##### groupByDesc(...) -> This
- **Signature:** `@Beta public This groupByDesc(final String propOrColumnName)`
- **Summary:** Adds a GROUP BY DESC clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This groupByDesc(final String... propOrColumnNames)`
- **Summary:** Adds a GROUP BY DESC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to group by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This groupByDesc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds a GROUP BY DESC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by descending
- **Returns:** this SqlBuilder instance for method chaining
##### groupBy(...) -> This
- **Signature:** `public This groupBy(final String propOrColumnName)`
- **Summary:** Adds a GROUP BY clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This groupBy(final String... propOrColumnNames)`
- **Summary:** Adds a GROUP BY clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to group by (must not be {@code null} or empty, and no element may be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This groupBy(final String expr, final SortDirection direction)`
- **Summary:** Adds a GROUP BY clause with a single column and sort direction.
- **Parameters:**
  - `expr` (`String`) — the column or expression to group by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This groupBy(final Collection<String> propOrColumnNames)`
- **Summary:** Adds a GROUP BY clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This groupBy(final Collection<String> propOrColumnNames, final SortDirection direction)`
- **Summary:** Adds a GROUP BY clause with a collection of columns and sort direction.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to group by
  - `direction` (`SortDirection`) — the direction appended after each column in the GROUP BY clause
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This groupBy(final Map<String, SortDirection> groupings)`
- **Summary:** Adds a GROUP BY clause with columns and individual sort directions.
- **Parameters:**
  - `groupings` (`Map<String, SortDirection>`) — map of columns to their sort directions
- **Returns:** this SqlBuilder instance for method chaining
##### having(...) -> This
- **Signature:** `public This having(final String expr)`
- **Summary:** Adds a HAVING clause with a string expression.
- **Parameters:**
  - `expr` (`String`) — the HAVING condition expression (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This having(final Condition condition)`
- **Summary:** Adds a HAVING clause with a condition object.
- **Parameters:**
  - `condition` (`Condition`) — the HAVING condition (must not be {@code null} )
- **Returns:** this SqlBuilder instance for method chaining
- **See also:** Filters
##### orderByAsc(...) -> This
- **Signature:** `@Beta public This orderByAsc(final String propOrColumnName)`
- **Summary:** Adds an ORDER BY ASC clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This orderByAsc(final String... propOrColumnNames)`
- **Summary:** Adds an ORDER BY ASC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to order by ascending
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This orderByAsc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds an ORDER BY ASC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by ascending
- **Returns:** this SqlBuilder instance for method chaining
##### orderByDesc(...) -> This
- **Signature:** `@Beta public This orderByDesc(final String propOrColumnName)`
- **Summary:** Adds an ORDER BY DESC clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This orderByDesc(final String... propOrColumnNames)`
- **Summary:** Adds an ORDER BY DESC clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to order by descending
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This orderByDesc(final Collection<String> propOrColumnNames)`
- **Summary:** Adds an ORDER BY DESC clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by descending
- **Returns:** this SqlBuilder instance for method chaining
##### orderBy(...) -> This
- **Signature:** `public This orderBy(final String propOrColumnName)`
- **Summary:** Adds an ORDER BY clause with a single column.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by (must not be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This orderBy(final String... propOrColumnNames)`
- **Summary:** Adds an ORDER BY clause with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to order by (must not be {@code null} or empty, and no element may be {@code null} , empty, or blank)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This orderBy(final String expr, final SortDirection direction)`
- **Summary:** Adds an ORDER BY clause with a single column and sort direction.
- **Parameters:**
  - `expr` (`String`) — the column or expression to order by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This orderBy(final Collection<String> propOrColumnNames)`
- **Summary:** Adds an ORDER BY clause with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This orderBy(final Collection<String> propOrColumnNames, final SortDirection direction)`
- **Summary:** Adds an ORDER BY clause with a collection of columns and sort direction.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to order by
  - `direction` (`SortDirection`) — the direction appended after each column in the ORDER BY clause
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Adds an ORDER BY clause with columns and individual sort directions.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of columns to their sort directions
- **Returns:** this SqlBuilder instance for method chaining
##### limit(...) -> This
- **Signature:** `public This limit(final int count)`
- **Summary:** Adds a row-count restriction to the query, rendered in the dialect's pagination syntax.
- **Contract:**
  - FETCH} together with an {@code ORDER BY} clause); the {@code OFFSET 0 ROWS} prefix is omitted when {@link #offset(int)} has already been called </li> <li> any other product, or no product info: {@code LIMIT count} </li> </ul> <p> On the {@code FETCH} -style dialects (Oracle, DB2, SQL Server) this method also consumes the {@code OFFSET} and {@code FETCH} slots, because {@code OFFSET} must precede {@code FETCH} : call {@link #offset(int)} <i> before </i> this method, or prefer {@link #limit(int, int)} , which emits the combined clause in the correct order.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This limit(final int count, final int offset)`
- **Summary:** Adds a count-plus-offset pagination clause, rendered in the dialect's pagination syntax.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this SqlBuilder instance for method chaining
##### offset(...) -> This
- **Signature:** `public This offset(final int offset)`
- **Summary:** Adds an OFFSET clause to skip a number of rows, rendered in the dialect's pagination syntax.
- **Contract:**
  - <p> On Oracle, DB2 and SQL Server dialects (per {@link SqlDialect.ProductInfo} ) the clause is rendered as {@code OFFSET offset ROWS} ; on those dialects call this method <i> before </i> {@link #limit(int)} , because {@code OFFSET} must precede {@code FETCH} .
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this SqlBuilder instance for method chaining
##### offsetRows(...) -> This
- **Signature:** `public This offsetRows(final int offset)`
- **Summary:** Adds an OFFSET ROWS clause (SQL:2008 standard syntax).
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this SqlBuilder instance for method chaining
- **See also:** #offset(int), #limit(int, int)
##### fetchNextRows(...) -> This
- **Signature:** `public This fetchNextRows(final int count)`
- **Summary:** Adds a FETCH NEXT N ROWS ONLY clause (SQL:2008 standard syntax).
- **Parameters:**
  - `count` (`int`) — the number of rows to fetch
- **Returns:** this SqlBuilder instance for method chaining
- **See also:** #limit(int), #limit(int, int)
##### fetchFirstRows(...) -> This
- **Signature:** `public This fetchFirstRows(final int count)`
- **Summary:** Adds a FETCH FIRST N ROWS ONLY clause (SQL standard syntax).
- **Parameters:**
  - `count` (`int`) — the number of rows to fetch
- **Returns:** this SqlBuilder instance for method chaining
- **See also:** #limit(int), #limit(int, int)
##### append(...) -> This
- **Signature:** `@Beta public This append(final Condition condition)`
- **Summary:** Appends a condition to the SQL statement.
- **Parameters:**
  - `condition` (`Condition`) — the condition to append
- **Returns:** this SqlBuilder instance for method chaining
- **See also:** Filters
- **Signature:** `public This append(final String expr)`
- **Summary:** Appends a string expression to the SQL statement.
- **Contract:**
  - <p> A single separating space is inserted before {@code expr} when, and only when, it is needed: that is, when the statement built so far does not already end with a space and {@code expr} does not already begin with one.
  - As a result both {@code .append("FOR UPDATE")} and {@code .append(" FOR UPDATE")} produce the same, correctly spaced output (a doubled space is possible only when the statement built so far already ends with a space and {@code expr} also begins with one).
- **Parameters:**
  - `expr` (`String`) — the expression to append
- **Returns:** this SqlBuilder instance for method chaining
##### appendIf(...) -> This
- **Signature:** `@Beta public This appendIf(final boolean b, final Condition condition)`
- **Summary:** Conditionally appends a condition to the SQL statement.
- **Parameters:**
  - `b` (`boolean`) — if true, the condition will be appended
  - `condition` (`Condition`) — the condition to append
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This appendIf(final boolean b, final String expr)`
- **Summary:** Conditionally appends a string expression to the SQL statement.
- **Parameters:**
  - `b` (`boolean`) — if true, the expression will be appended
  - `expr` (`String`) — the expression to append
- **Returns:** this SqlBuilder instance for method chaining
##### appendIfOrElse(...) -> This
- **Signature:** `@Beta public This appendIfOrElse(final boolean b, final Condition conditionToAppendForTrue, final Condition conditionToAppendForFalse)`
- **Summary:** Conditionally appends one of two conditions based on a boolean value.
- **Parameters:**
  - `b` (`boolean`) — if true, append condToAppendForTrue; otherwise append condToAppendForFalse
  - `conditionToAppendForTrue` (`Condition`) — the condition to append if {@code b} is true
  - `conditionToAppendForFalse` (`Condition`) — the condition to append if {@code b} is false
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Beta public This appendIfOrElse(final boolean b, final String exprToAppendForTrue, final String exprToAppendForFalse)`
- **Summary:** Conditionally appends one of two string expressions based on a boolean value.
- **Parameters:**
  - `b` (`boolean`) — if true, append exprToAppendForTrue; otherwise append exprToAppendForFalse
  - `exprToAppendForTrue` (`String`) — the expression to append if {@code b} is true
  - `exprToAppendForFalse` (`String`) — the expression to append if {@code b} is false
- **Returns:** this SqlBuilder instance for method chaining
##### union(...) -> This
- **Signature:** `public This union(final This sqlBuilder)`
- **Summary:** Adds a UNION clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to union (must not be {@code null} and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This union(final String query)`
- **Summary:** Adds a UNION clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the complete read-only {@code SELECT} sub-query to union
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This union(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for UNION operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the union query
- **Returns:** this SqlBuilder instance for method chaining
##### unionAll(...) -> This
- **Signature:** `public This unionAll(final This sqlBuilder)`
- **Summary:** Adds a UNION ALL clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to union all (must not be {@code null} and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This unionAll(final String query)`
- **Summary:** Adds a UNION ALL clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the complete read-only {@code SELECT} sub-query to union all
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This unionAll(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for UNION ALL operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the union all query
- **Returns:** this SqlBuilder instance for method chaining
##### intersect(...) -> This
- **Signature:** `public This intersect(final This sqlBuilder)`
- **Summary:** Adds an INTERSECT clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to intersect (must not be {@code null} and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This intersect(final String query)`
- **Summary:** Adds an INTERSECT clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the complete read-only {@code SELECT} sub-query to intersect
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This intersect(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for INTERSECT operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the intersect query
- **Returns:** this SqlBuilder instance for method chaining
##### except(...) -> This
- **Signature:** `public This except(final This sqlBuilder)`
- **Summary:** Adds an EXCEPT clause with another SQL query.
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to except (must not be {@code null} and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This except(final String query)`
- **Summary:** Adds an EXCEPT clause with a SQL query string.
- **Parameters:**
  - `query` (`String`) — the complete read-only {@code SELECT} sub-query to except
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This except(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for EXCEPT operation with a collection of columns.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the except query
- **Returns:** this SqlBuilder instance for method chaining
##### minus(...) -> This
- **Signature:** `public This minus(final This sqlBuilder)`
- **Summary:** Adds a MINUS clause with another SQL query (Oracle syntax).
- **Parameters:**
  - `sqlBuilder` (`This`) — the SQL builder containing the query to minus (must not be {@code null} and must not be this same instance)
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This minus(final String query)`
- **Summary:** Adds a MINUS clause with a SQL query string (Oracle syntax).
- **Parameters:**
  - `query` (`String`) — the complete read-only {@code SELECT} sub-query to subtract with MINUS
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This minus(final Collection<String> propOrColumnNames)`
- **Summary:** Starts a new SELECT query for MINUS operation with a collection of columns (Oracle syntax).
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns for the minus query
- **Returns:** this SqlBuilder instance for method chaining
##### forUpdate(...) -> This
- **Signature:** `public This forUpdate()`
- **Summary:** Adds a FOR UPDATE clause to lock selected rows.
- **Parameters:**
  - (none)
- **Returns:** this SqlBuilder instance for method chaining
##### set(...) -> This
- **Signature:** `public This set(final String expr)`
- **Summary:** Sets a single column or raw assignment expression for an UPDATE operation.
- **Contract:**
  - <p> If {@code expr} contains an {@code =} sign, it is treated as a complete assignment and no placeholder is generated (identifiers are still normalized according to the naming policy).
- **Parameters:**
  - `expr` (`String`) — a column name (placeholder will be appended) or a complete {@code col = value} assignment
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This set(final String... propOrColumnNames)`
- **Summary:** Sets columns for UPDATE operation.
- **Contract:**
  - If a column name already contains an {@code =} sign, it is treated as a raw SET expression and no placeholder is appended.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the columns to update
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This set(final Collection<String> propOrColumnNames)`
- **Summary:** Sets columns for UPDATE operation with a collection of property or column names.
- **Contract:**
  - If a column name already contains an {@code =} sign, it is treated as a raw SET expression and no placeholder is appended.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — the collection of columns to update
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This set(final Map<String, Object> props)`
- **Summary:** Sets columns and values for UPDATE operation using a map.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of column names to values
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Deprecated public This set(final Object entity)`
- **Summary:** Sets properties to update from an entity object, a {@code Map} , or a single column-name {@code String} .
- **Parameters:**
  - `entity` (`Object`) — the entity object, {@code Map<String, Object>} , or column-name {@code String} containing properties to set
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `@Deprecated public This set(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Sets properties to update from an entity object, a {@code Map} , or a single column-name {@code String} , excluding the specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object, {@code Map<String, Object>} , or column-name {@code String} containing properties to set
  - `excludedPropNames` (`Set<String>`) — property names to exclude from the update (may be {@code null} )
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
- **Summary:** Sets properties to update from an entity object, a {@code Map} , or a single column-name {@code String} .
- **Parameters:**
  - `entity` (`Object`) — the entity object, {@code Map<String, Object>} , or column-name {@code String} containing properties to set
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This setEntity(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Sets properties to update from an entity object, a {@code Map} , or a single column-name {@code String} , excluding the specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object, {@code Map<String, Object>} , or column-name {@code String} containing properties to set
  - `excludedPropNames` (`Set<String>`) — property names to exclude from the update (may be {@code null} )
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This setEntity(final Class<?> entityClass)`
- **Summary:** Sets all updatable properties from an entity class for UPDATE operation.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to get properties from
- **Returns:** this SqlBuilder instance for method chaining
- **Signature:** `public This setEntity(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Sets updatable properties from an entity class for UPDATE operation, excluding specified properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to get properties from
  - `excludedPropNames` (`Set<String>`) — additional properties to exclude from the update
- **Returns:** this SqlBuilder instance for method chaining
##### build(...) -> SP
- **Signature:** `public SP build()`
- **Summary:** Generates the final SQL string and its parameters as an {@link SP} pair, then releases resources.
- **Parameters:**
  - (none)
- **Returns:** an SP (SQL-Parameters) pair containing the SQL string and parameter list
##### apply(...) -> T
- **Signature:** `@Beta public <T, E extends Exception> T apply(final Throwables.Function<? super SP, T, E> function) throws E`
- **Summary:** Applies a function to the SQL-Parameters pair and returns the result.
- **Parameters:**
  - `function` (`Throwables.Function<? super SP, T, E>`) — the function to apply to the SP pair
- **Returns:** the result of applying the function
- **Throws:**
  - `E` — if the function throws an exception
- **Signature:** `@Beta public <T, E extends Exception> T apply(final Throwables.BiFunction<? super String, ? super List<Object>, T, E> function) throws E`
- **Summary:** Applies a bi-function to the SQL string and parameters separately and returns the result.
- **Parameters:**
  - `function` (`Throwables.BiFunction<? super String, ? super List<Object>, T, E>`) — the bi-function to apply to the SQL and parameters
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
##### debugPrint(...) -> void
- **Signature:** `@Beta public void debugPrint()`
- **Summary:** Builds the SQL and prints the resulting query string to standard output.
- **Parameters:**
  - (none)

### Record SP (com.landawn.abacus.query.AbstractQueryBuilder.SP)
Represents a SQL string and its associated parameters.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `record SP(String query, ImmutableList<Object> parameters) { } }`
- **Parameters:**
  - `query` (`String`)
  - `parameters` (`ImmutableList<Object>`)

### Class Dsl (com.landawn.abacus.query.Dsl)
Entry point for building SQL statements with a fixed {@link SqlDialect} (naming policy + parameter style).

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### forDialect(...) -> Dsl
- **Signature:** `public static Dsl forDialect(final SqlDialect sqlDialect)`
- **Summary:** Creates a {@code Dsl} bound to the given {@link SqlDialect} , fixing the naming policy and parameter style of every {@link SqlBuilder} it produces.
- **Parameters:**
  - `sqlDialect` (`SqlDialect`) — the dialect (naming policy, parameter style, identifier quote and optional product info) the DSL is bound to
- **Returns:** a {@code Dsl} that produces {@link SqlBuilder} instances using the given dialect; a shared cached instance is returned for the predefined dialect combinations, otherwise a new instance

#### Public Instance Methods
##### sqlDialect(...) -> SqlDialect
- **Signature:** `public SqlDialect sqlDialect()`
- **Summary:** Returns the {@link SqlDialect} this DSL is bound to; every {@link SqlBuilder} it produces renders SQL with this dialect.
- **Parameters:**
  - (none)
- **Returns:** the dialect (naming policy, parameter style, identifier quote and optional product info) bound to this DSL
##### insert(...) -> SqlBuilder
- **Signature:** `public SqlBuilder insert(final String propOrColumnName)`
- **Summary:** Creates an INSERT statement for a single column.
- **Contract:**
  - The actual value will be provided as a parameter when executing the query.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Signature:** `public SqlBuilder insert(final String... propOrColumnNames)`
- **Summary:** Creates an INSERT statement for multiple columns.
- **Contract:**
  - Values will be provided as parameters when executing the query.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Signature:** `public SqlBuilder insert(final Collection<String> propOrColumnNames)`
- **Summary:** Creates an INSERT statement for a collection of columns.
- **Contract:**
  - <p> This method provides flexibility when column names are dynamically generated or come from a collection.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Signature:** `public SqlBuilder insert(final Map<String, Object> props)`
- **Summary:** Creates an INSERT statement from a map of property names and values.
- **Parameters:**
  - `props` (`Map<String, Object>`) — map of property names to their values
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Signature:** `public SqlBuilder insert(final Object entity)`
- **Summary:** Creates an INSERT statement from an entity object.
- **Contract:**
  - Properties whose value is {@code null} are also skipped, as are ID properties still holding their default value (for a composite ID, only when every ID property holds its default value).
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Signature:** `public SqlBuilder insert(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement from an entity object with excluded properties.
- **Contract:**
  - Properties in the exclusion set will not be included even if they have values and are normally insertable.
  - Properties whose value is {@code null} are also skipped, as are ID properties still holding their default value (for a composite ID, only when every ID property holds its default value).
- **Parameters:**
  - `entity` (`Object`) — the entity object to insert
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Signature:** `public SqlBuilder insert(final Class<?> entityClass)`
- **Summary:** Creates an INSERT statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Signature:** `public SqlBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
##### insertInto(...) -> SqlBuilder
- **Signature:** `public SqlBuilder insertInto(final Class<?> entityClass)`
- **Summary:** Creates an INSERT INTO statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO for
- **Returns:** a new SqlBuilder instance configured for INSERT operation
- **Signature:** `public SqlBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an INSERT INTO statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to generate INSERT INTO for
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the insert
- **Returns:** a new SqlBuilder instance configured for INSERT operation
##### batchInsert(...) -> SqlBuilder
- **Signature:** `@Beta public SqlBuilder batchInsert(final Collection<?> entitiesOrPropMaps)`
- **Summary:** Generates a MySQL-style batch INSERT statement.
- **Parameters:**
  - `entitiesOrPropMaps` (`Collection<?>`) — list of entities or property maps to insert
- **Returns:** a new SqlBuilder instance configured for batch INSERT operation
##### update(...) -> SqlBuilder
- **Signature:** `public SqlBuilder update(final String tableName)`
- **Summary:** Creates an UPDATE statement for a table.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
- **Returns:** a new SqlBuilder instance configured for UPDATE operation
- **Signature:** `public SqlBuilder update(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to update
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SqlBuilder instance configured for UPDATE operation
- **Signature:** `public SqlBuilder update(final Class<?> entityClass)`
- **Summary:** Creates an UPDATE statement for an entity class.
- **Contract:**
  - A WHERE clause should be added before calling {@code build()} .
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
- **Returns:** a new SqlBuilder instance configured for UPDATE operation
- **Signature:** `public SqlBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates an UPDATE statement for an entity class with excluded properties.
- **Contract:**
  - This is useful for partial updates or when certain fields should never be updated.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to update
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the update
- **Returns:** a new SqlBuilder instance configured for UPDATE operation
##### deleteFrom(...) -> SqlBuilder
- **Signature:** `public SqlBuilder deleteFrom(final String tableName)`
- **Summary:** Creates a DELETE FROM statement for a table.
- **Contract:**
  - Property names in WHERE conditions will be rendered according to this DSL's naming policy if an entity class is associated.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
- **Returns:** a new SqlBuilder instance configured for DELETE operation
- **Signature:** `public SqlBuilder deleteFrom(final String tableName, final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement for a table with entity class mapping.
- **Parameters:**
  - `tableName` (`String`) — the name of the table to delete from
  - `entityClass` (`Class<?>`) — the entity class for property mapping
- **Returns:** a new SqlBuilder instance configured for DELETE operation
- **Signature:** `public SqlBuilder deleteFrom(final Class<?> entityClass)`
- **Summary:** Creates a DELETE FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to delete from
- **Returns:** a new SqlBuilder instance configured for DELETE operation
##### select(...) -> SqlBuilder
- **Signature:** `public SqlBuilder select(final String expr)`
- **Summary:** Creates a SELECT statement with a single expression.
- **Contract:**
  - <p> This method is useful for complex select expressions, aggregate functions, or when selecting computed values.
- **Parameters:**
  - `expr` (`String`) — the select expression
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder select(final String... propOrColumnNames)`
- **Summary:** Creates a SELECT statement with multiple columns.
- **Parameters:**
  - `propOrColumnNames` (`String[]`) — the property or column names to select
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder select(final Collection<String> propOrColumnNames)`
- **Summary:** Creates a SELECT statement with a collection of columns.
- **Contract:**
  - <p> This method provides flexibility when column names are dynamically generated.
- **Parameters:**
  - `propOrColumnNames` (`Collection<String>`) — collection of property or column names to select
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder select(final Map<String, String> propOrColumnNameAliases)`
- **Summary:** Creates a SELECT statement with column aliases.
- **Parameters:**
  - `propOrColumnNameAliases` (`Map<String, String>`) — map of property/column names to their aliases
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder select(final Class<?> entityClass)`
- **Summary:** Creates a SELECT statement for all properties of an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT statement for an entity class with optional sub-entity properties.
- **Contract:**
  - <p> When includeSubEntityProperties is true, properties of nested entity objects are also included in the selection with appropriate prefixes.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT statement for an entity class with sub-entities and exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select properties from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
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
- **Signature:** `public SqlBuilder select(final Selection selection)`
- **Summary:** Creates a SELECT statement from a single {@link Selection} descriptor.
- **Contract:**
  - Prefer it over the positional {@code select(Class, ...)} overloads when you need full control (table alias, class alias, sub-entity inclusion, property exclusion) over a single entity, because each attribute is set through a named {@link Selection} setter rather than by argument position.
- **Parameters:**
  - `selection` (`Selection`) — the selection descriptor defining the entity, aliases, and property filtering; must not be {@code null}
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **See also:** #select(List), Selection
- **Signature:** `public SqlBuilder select(final List<Selection> selections)`
- **Summary:** Creates a SELECT statement for multiple entities using Selection descriptors.
- **Contract:**
  - The descriptors and their property-name collections are snapshotted when this method returns, so later caller mutations cannot change the deferred SQL generated by the returned builder.
- **Parameters:**
  - `selections` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SqlBuilder instance configured for SELECT operation
##### selectFrom(...) -> SqlBuilder
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass)`
- **Summary:** Creates a complete SELECT FROM statement for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final String tableAlias)`
- **Summary:** Creates a SELECT FROM statement for an entity class with table alias.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the table alias to use
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with optional sub-entity properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final String tableAlias, final boolean includeSubEntityProperties)`
- **Summary:** Creates a SELECT FROM statement with alias and sub-entity option.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final String tableAlias, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with alias and excluded properties.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the table alias to use
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a SELECT FROM statement with sub-entities and exclusions.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder selectFrom(final Class<?> entityClass, final String tableAlias, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Creates a complete SELECT FROM statement with all options.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select from
  - `tableAlias` (`String`) — the table alias to use
  - `includeSubEntityProperties` (`boolean`) — whether to include properties of nested entity objects
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from selection
- **Returns:** a new SqlBuilder instance configured for SELECT operation
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
- **Signature:** `public SqlBuilder selectFrom(final Selection selection)`
- **Summary:** Creates a SELECT ...
- **Contract:**
  - Prefer it over the positional {@code selectFrom(Class, ...)} overloads when configuring a single entity, since each attribute is set through a named {@link Selection} setter rather than by argument position.
- **Parameters:**
  - `selection` (`Selection`) — the selection descriptor defining the entity, aliases, and property filtering; must not be {@code null}
- **Returns:** a new SqlBuilder instance with SELECT and FROM configured
- **See also:** #selectFrom(List), Selection
- **Signature:** `public SqlBuilder selectFrom(final List<Selection> selections)`
- **Summary:** Creates a SELECT FROM statement for multiple entity selections.
- **Parameters:**
  - `selections` (`List<Selection>`) — list of Selection objects defining what to select from each entity
- **Returns:** a new SqlBuilder instance with SELECT and FROM configured
##### count(...) -> SqlBuilder
- **Signature:** `public SqlBuilder count(final String tableName)`
- **Summary:** Creates a COUNT(*) query for a table.
- **Parameters:**
  - `tableName` (`String`) — the table to count rows from
- **Returns:** a new SqlBuilder instance configured for SELECT operation
- **Signature:** `public SqlBuilder count(final Class<?> entityClass)`
- **Summary:** Creates a COUNT(*) query for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to count
- **Returns:** a new SqlBuilder instance configured for SELECT operation
##### renderCondition(...) -> SqlBuilder
- **Signature:** `public SqlBuilder renderCondition(final Condition condition, final Class<?> entityClass)`
- **Summary:** Renders a condition as a standalone SQL fragment, using the given entity class for property-to-column mapping.
- **Parameters:**
  - `condition` (`Condition`) — the condition to render (must not be {@code null} )
  - `entityClass` (`Class<?>`) — the entity class used for property-to-column mapping (may be {@code null} )
- **Returns:** a new SqlBuilder instance containing the rendered condition SQL
- **Signature:** `public SqlBuilder renderCondition(final Condition condition)`
- **Summary:** Renders a condition as a standalone SQL fragment without an entity class.
- **Parameters:**
  - `condition` (`Condition`) — the condition to render (must not be {@code null} )
- **Returns:** a new SqlBuilder instance containing the rendered condition SQL
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
- **Summary:** Creates a new {@link Builder} instance for constructing a dynamic SQL query.
- **Parameters:**
  - (none)
- **Returns:** a new {@link Builder} instance

#### Public Instance Methods
- (none)

### Class Builder (com.landawn.abacus.query.DynamicQuery.Builder)
Builder for constructing dynamic SQL queries clause by clause.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### select(...) -> SelectClause
- **Signature:** `public SelectClause select()`
- **Summary:** Returns the {@link SelectClause} builder for defining columns to retrieve.
- **Parameters:**
  - (none)
- **Returns:** the {@link SelectClause} builder for method chaining
##### from(...) -> FromClause
- **Signature:** `public FromClause from()`
- **Summary:** Returns the {@link FromClause} builder for defining tables and joins.
- **Parameters:**
  - (none)
- **Returns:** the {@link FromClause} builder for method chaining
##### where(...) -> WhereClause
- **Signature:** `public WhereClause where()`
- **Summary:** Returns the {@link WhereClause} builder for defining query conditions.
- **Parameters:**
  - (none)
- **Returns:** the {@link WhereClause} builder for method chaining
##### groupBy(...) -> GroupByClause
- **Signature:** `public GroupByClause groupBy()`
- **Summary:** Returns the {@link GroupByClause} builder for defining grouping columns.
- **Parameters:**
  - (none)
- **Returns:** the {@link GroupByClause} builder for method chaining
##### having(...) -> HavingClause
- **Signature:** `public HavingClause having()`
- **Summary:** Returns the {@link HavingClause} builder for defining conditions on grouped results.
- **Parameters:**
  - (none)
- **Returns:** the {@link HavingClause} builder for method chaining
##### orderBy(...) -> OrderByClause
- **Signature:** `public OrderByClause orderBy()`
- **Summary:** Returns the {@link OrderByClause} builder for defining result ordering.
- **Parameters:**
  - (none)
- **Returns:** the {@link OrderByClause} builder for method chaining
##### limit(...) -> Builder
- **Signature:** `public Builder limit(final int count)`
- **Summary:** Adds a {@code LIMIT} clause to restrict the number of rows returned.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must not be negative)
- **Returns:** this builder instance for method chaining
- **Signature:** `public Builder limit(final int count, final int offset)`
- **Summary:** Adds a {@code LIMIT} clause with count and offset for pagination.
- **Contract:**
  - Generates: {@code LIMIT count OFFSET offset} ; the {@code OFFSET} portion is omitted when {@code offset} is {@code 0} , emitting just {@code LIMIT count} .
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must not be negative)
  - `offset` (`int`) — the number of rows to skip (must not be negative)
- **Returns:** this builder instance for method chaining
- **See also:** #offsetRows(int), #fetchNextRows(int), #fetchFirstRows(int)
##### offset(...) -> Builder
- **Signature:** `public Builder offset(final int offset)`
- **Summary:** Adds a plain {@code OFFSET} clause to skip the given number of leading rows.
- **Contract:**
  - When {@code offset} is {@code 0} , nothing is appended.
  - <p> Use {@link #offsetRows(int)} instead when you need the SQL:2008 {@code OFFSET n ROWS} form (typically paired with {@link #fetchNextRows(int)} or {@link #fetchFirstRows(int)} ).
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip (must not be negative)
- **Returns:** this builder instance for method chaining
- **See also:** #offsetRows(int)
##### offsetRows(...) -> Builder
- **Signature:** `public Builder offsetRows(final int offset)`
- **Summary:** Adds an {@code OFFSET} clause for SQL:2008 standard pagination.
- **Contract:**
  - When {@code offset} is {@code 0} , nothing is appended.
- **Parameters:**
  - `offset` (`int`) — the number of rows to skip (must not be negative)
- **Returns:** this builder instance for method chaining
##### fetchNextRows(...) -> Builder
- **Signature:** `public Builder fetchNextRows(final int count)`
- **Summary:** Adds a {@code FETCH NEXT} clause for SQL:2008 standard result limiting.
- **Parameters:**
  - `count` (`int`) — the number of rows to fetch (must not be negative)
- **Returns:** this builder instance for method chaining
##### fetchFirstRows(...) -> Builder
- **Signature:** `public Builder fetchFirstRows(final int count)`
- **Summary:** Adds a {@code FETCH FIRST} clause for SQL:2008 standard result limiting.
- **Parameters:**
  - `count` (`int`) — the number of rows to fetch (must not be negative)
- **Returns:** this builder instance for method chaining
- **See also:** #offsetRows(int)
##### union(...) -> Builder
- **Signature:** `public Builder union(final String query)`
- **Summary:** Adds a {@code UNION} operator to combine results with another query.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to union with (must not be {@code null} , empty, or blank)
- **Returns:** this builder instance for method chaining
##### unionAll(...) -> Builder
- **Signature:** `public Builder unionAll(final String query)`
- **Summary:** Adds a {@code UNION ALL} operator to combine results with another query.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to union with (must not be {@code null} , empty, or blank)
- **Returns:** this builder instance for method chaining
##### intersect(...) -> Builder
- **Signature:** `public Builder intersect(final String query)`
- **Summary:** Adds an {@code INTERSECT} operator to find common rows between queries.
- **Parameters:**
  - `query` (`String`) — the complete SQL query to intersect with (must not be {@code null} , empty, or blank)
- **Returns:** this builder instance for method chaining
##### except(...) -> Builder
- **Signature:** `public Builder except(final String query)`
- **Summary:** Adds an {@code EXCEPT} operator to find rows in the first query but not in the second.
- **Parameters:**
  - `query` (`String`) — the complete SQL query whose result rows are subtracted from the current result set (must not be {@code null} , empty, or blank)
- **Returns:** this builder instance for method chaining
##### minus(...) -> Builder
- **Signature:** `public Builder minus(final String query)`
- **Summary:** Adds a {@code MINUS} operator to find rows in the first query but not in the second.
- **Parameters:**
  - `query` (`String`) — the complete SQL query whose result rows are subtracted from the current result set (must not be {@code null} , empty, or blank)
- **Returns:** this builder instance for method chaining
##### append(...) -> Builder
- **Signature:** `public Builder append(final String textToAppend)`
- **Summary:** Appends a raw, database-specific SQL clause or fragment verbatim to the end of the query.
- **Contract:**
  - </p> <p> A single separating space is inserted before {@code textToAppend} when, and only when, it is needed: that is, when the text built so far does not already end with a space and {@code textToAppend} does not already begin with one.
  - As a result both {@code .append("LIMIT 10")} and {@code .append(" LIMIT 10")} produce the same, correctly spaced output (a doubled space is possible only when the previously appended raw text already ends with a space and {@code textToAppend} also begins with one).
- **Parameters:**
  - `textToAppend` (`String`) — the complete raw SQL clause to append verbatim (e.g., {@code "LIMIT 10 OFFSET 20"} ) (must not be {@code null} , empty, or blank)
- **Returns:** this builder instance for method chaining
##### appendIf(...) -> Builder
- **Signature:** `public Builder appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a raw SQL clause or fragment verbatim to the end of the query.
- **Contract:**
  - When {@code b} is {@code true} this behaves exactly like {@link #append(String)} (a single separating space is inserted only when needed, then the text is emitted unchanged with no validation, escaping, or interpretation); when {@code b} is {@code false} the builder is left unchanged and {@code textToAppend} is not inspected.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the raw SQL clause to append verbatim if {@code b} is {@code true} (must not be {@code null} , empty, or blank when {@code b} is {@code true} )
- **Returns:** this builder instance for method chaining
- **See also:** #append(String)
##### appendIfOrElse(...) -> Builder
- **Signature:** `public Builder appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two raw SQL clauses verbatim to the end of the query based on a boolean condition.
- **Contract:**
  - Always appends something, choosing between the two options; the chosen text is emitted exactly as {@link #append(String)} would emit it (a single separating space is inserted only when needed, with no validation, escaping, or interpretation).
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the raw SQL clause to append if condition is true (must not be {@code null} , empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the raw SQL clause to append if condition is false (must not be {@code null} , empty, or blank)
- **Returns:** this builder instance for method chaining
- **See also:** #append(String)
##### build(...) -> String
- **Signature:** `public String build()`
- **Summary:** Builds the final SQL string from all the components and releases resources.
- **Contract:**
  - This method MUST be called to get the SQL and clean up internal resources.
  - After calling {@code build()} , this builder is closed and must not be reused: any subsequent call to a builder method (including {@code build()} itself) throws {@link IllegalStateException} .
- **Parameters:**
  - (none)
- **Returns:** the complete SQL query string

### Class SelectClause (com.landawn.abacus.query.DynamicQuery.SelectClause)
Builder class for constructing the {@code SELECT} clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> SelectClause
- **Signature:** `public SelectClause append(final String column)`
- **Summary:** Appends a single column to the {@code SELECT} clause.
- **Parameters:**
  - `column` (`String`) — the column name to select (must not be {@code null} , empty, or blank)
- **Returns:** this {@link SelectClause} instance for method chaining
- **Signature:** `public SelectClause append(final String column, final String alias)`
- **Summary:** Appends a column with an alias to the {@code SELECT} clause.
- **Parameters:**
  - `column` (`String`) — the column name to select (must not be {@code null} , empty, or blank)
  - `alias` (`String`) — the alias for the column (must not be {@code null} , empty, or blank)
- **Returns:** this {@link SelectClause} instance for method chaining
- **Signature:** `public SelectClause append(final Collection<String> columns)`
- **Summary:** Appends multiple columns to the {@code SELECT} clause.
- **Contract:**
  - If the collection is empty, this method does nothing.
  - A non-empty collection is snapshotted before validation and rendering, so both phases observe the same elements even if the supplied collection is live or mutable.
- **Parameters:**
  - `columns` (`Collection<String>`) — collection of column names to select (may be {@code null} or empty; individual elements must not be {@code null} , empty, or blank)
- **Returns:** this {@link SelectClause} instance for method chaining
- **Signature:** `public SelectClause append(final Map<String, String> columnAliases)`
- **Summary:** Appends multiple columns with their aliases to the {@code SELECT} clause.
- **Contract:**
  - If the map is empty, this method does nothing.
  - Columns are emitted in the map's iteration order, so use a {@link java.util.LinkedHashMap} if a stable column order matters.
- **Parameters:**
  - `columnAliases` (`Map<String, String>`) — map where keys are column names and values are aliases (may be {@code null} or empty; individual keys and values must not be {@code null} , empty, or blank)
- **Returns:** this {@link SelectClause} instance for method chaining
##### appendIf(...) -> SelectClause
- **Signature:** `public SelectClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the {@code SELECT} clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank when {@code b} is {@code true} )
- **Returns:** this {@link SelectClause} instance for method chaining
##### appendIfOrElse(...) -> SelectClause
- **Signature:** `public SelectClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the {@code SELECT} clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be {@code null} , empty, or blank)
- **Returns:** this {@link SelectClause} instance for method chaining

### Class FromClause (com.landawn.abacus.query.DynamicQuery.FromClause)
Builder class for constructing the {@code FROM} clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> FromClause
- **Signature:** `public FromClause append(final String table)`
- **Summary:** Appends a table to the {@code FROM} clause.
- **Parameters:**
  - `table` (`String`) — the table name to add (must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
- **Signature:** `public FromClause append(final String table, final String alias)`
- **Summary:** Appends a table with an alias to the {@code FROM} clause.
- **Parameters:**
  - `table` (`String`) — the table name to add (must not be {@code null} , empty, or blank)
  - `alias` (`String`) — the alias for the table (must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
- **Signature:** `public FromClause append(final Collection<String> tables)`
- **Summary:** Appends multiple tables to the {@code FROM} clause.
- **Contract:**
  - If the collection is empty, this method does nothing.
- **Parameters:**
  - `tables` (`Collection<String>`) — collection of table names to add (may be {@code null} or empty; individual elements must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
##### join(...) -> FromClause
- **Signature:** `public FromClause join(final String joinExpr, final String expr)`
- **Summary:** Adds a {@code JOIN} clause (implicit {@code INNER JOIN} ) with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
  - `expr` (`String`) — the join condition (must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
- **Signature:** `public FromClause join(final String joinExpr)`
- **Summary:** Adds a {@code JOIN} clause (implicit {@code INNER JOIN} ) with the specified table and no {@code ON} condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
##### innerJoin(...) -> FromClause
- **Signature:** `public FromClause innerJoin(final String joinExpr, final String expr)`
- **Summary:** Adds an {@code INNER JOIN} clause with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
  - `expr` (`String`) — the join condition (must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
- **Signature:** `public FromClause innerJoin(final String joinExpr)`
- **Summary:** Adds an {@code INNER JOIN} clause with the specified table and no {@code ON} condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
##### leftJoin(...) -> FromClause
- **Signature:** `public FromClause leftJoin(final String joinExpr, final String expr)`
- **Summary:** Adds a {@code LEFT JOIN} clause with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
  - `expr` (`String`) — the join condition (must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
- **Signature:** `public FromClause leftJoin(final String joinExpr)`
- **Summary:** Adds a {@code LEFT JOIN} clause with the specified table and no {@code ON} condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
##### rightJoin(...) -> FromClause
- **Signature:** `public FromClause rightJoin(final String joinExpr, final String expr)`
- **Summary:** Adds a {@code RIGHT JOIN} clause with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
  - `expr` (`String`) — the join condition (must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
- **Signature:** `public FromClause rightJoin(final String joinExpr)`
- **Summary:** Adds a {@code RIGHT JOIN} clause with the specified table and no {@code ON} condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
##### fullJoin(...) -> FromClause
- **Signature:** `public FromClause fullJoin(final String joinExpr, final String expr)`
- **Summary:** Adds a {@code FULL JOIN} clause with the specified table and join condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
  - `expr` (`String`) — the join condition (must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
- **Signature:** `public FromClause fullJoin(final String joinExpr)`
- **Summary:** Adds a {@code FULL JOIN} clause with the specified table and no {@code ON} condition.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
##### crossJoin(...) -> FromClause
- **Signature:** `public FromClause crossJoin(final String joinExpr)`
- **Summary:** Adds a {@code CROSS JOIN} clause with the specified table.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
##### naturalJoin(...) -> FromClause
- **Signature:** `public FromClause naturalJoin(final String joinExpr)`
- **Summary:** Adds a {@code NATURAL JOIN} clause with the specified table.
- **Parameters:**
  - `joinExpr` (`String`) — the table or entity to join (can include alias; must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining
##### appendIf(...) -> FromClause
- **Signature:** `public FromClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the {@code FROM} clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank when {@code b} is {@code true} )
- **Returns:** this {@link FromClause} instance for method chaining
##### appendIfOrElse(...) -> FromClause
- **Signature:** `public FromClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the {@code FROM} clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be {@code null} , empty, or blank)
- **Returns:** this {@link FromClause} instance for method chaining

### Class WhereClause (com.landawn.abacus.query.DynamicQuery.WhereClause)
Builder class for constructing the {@code WHERE} clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> WhereClause
- **Signature:** `public WhereClause append(final String expr)`
- **Summary:** Appends a condition to the {@code WHERE} clause.
- **Contract:**
  - <p> Unlike {@link #and(String)} / {@link #or(String)} , this method does <em> not </em> insert a logical connective \\u2014 the caller must include any required {@code AND} / {@code OR} in the argument.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to append (must not be {@code null} , empty, or blank)
- **Returns:** this {@link WhereClause} instance for method chaining
##### appendPlaceholders(...) -> WhereClause
- **Signature:** `public WhereClause appendPlaceholders(final int placeholderCount)`
- **Summary:** Appends question mark placeholders for parameterized queries.
- **Contract:**
  - Use the {@link #appendPlaceholders(int, String, String)} overload when you also need a prefix/postfix (e.g.
  - // Prefer appendPlaceholders(int, String, String) when you want the parentheses tightly attached.
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to append (must not be negative)
- **Returns:** this {@link WhereClause} instance for method chaining
- **Signature:** `public WhereClause appendPlaceholders(final int placeholderCount, final String prefix, final String postfix)`
- **Summary:** Appends question mark placeholders surrounded by prefix and postfix.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code where.append("status IN ").appendPlaceholders(3, "(", ")"); // Generates: status IN (?, ?, ?) } </pre> <p> If {@code placeholderCount} is {@code 0} , neither {@code prefix} nor {@code postfix} is appended.
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to append (must not be negative)
  - `prefix` (`String`) — the string to add before the question marks (must not be {@code null} )
  - `postfix` (`String`) — the string to add after the question marks (must not be {@code null} )
- **Returns:** this {@link WhereClause} instance for method chaining
##### and(...) -> WhereClause
- **Signature:** `public WhereClause and(final String expr)`
- **Summary:** Adds an {@code AND} condition to the {@code WHERE} clause.
- **Contract:**
  - If called before any {@link #append(String)} , this acts as the first condition and emits {@code WHERE cond} with no leading {@code AND} .
- **Parameters:**
  - `expr` (`String`) — the SQL expression to add with {@code AND} (must not be {@code null} , empty, or blank)
- **Returns:** this {@link WhereClause} instance for method chaining
##### or(...) -> WhereClause
- **Signature:** `public WhereClause or(final String expr)`
- **Summary:** Adds an {@code OR} condition to the {@code WHERE} clause.
- **Contract:**
  - If called before any {@link #append(String)} , this acts as the first condition and emits {@code WHERE cond} with no leading {@code OR} .
- **Parameters:**
  - `expr` (`String`) — the SQL expression to add with {@code OR} (must not be {@code null} , empty, or blank)
- **Returns:** this {@link WhereClause} instance for method chaining
##### appendIf(...) -> WhereClause
- **Signature:** `public WhereClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the {@code WHERE} clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank when {@code b} is {@code true} )
- **Returns:** this {@link WhereClause} instance for method chaining
##### appendIfOrElse(...) -> WhereClause
- **Signature:** `public WhereClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the {@code WHERE} clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be {@code null} , empty, or blank)
- **Returns:** this {@link WhereClause} instance for method chaining

### Class GroupByClause (com.landawn.abacus.query.DynamicQuery.GroupByClause)
Builder class for constructing the {@code GROUP BY} clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> GroupByClause
- **Signature:** `public GroupByClause append(final String propOrColumnName)`
- **Summary:** Appends a column to the {@code GROUP BY} clause.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by (must not be {@code null} , empty, or blank)
- **Returns:** this {@link GroupByClause} instance for method chaining
- **Signature:** `public GroupByClause append(final Collection<String> columns)`
- **Summary:** Appends multiple columns to the {@code GROUP BY} clause.
- **Contract:**
  - If the collection is empty, this method does nothing.
- **Parameters:**
  - `columns` (`Collection<String>`) — collection of column names to group by (may be {@code null} or empty; individual elements must not be {@code null} , empty, or blank)
- **Returns:** this {@link GroupByClause} instance for method chaining
##### appendIf(...) -> GroupByClause
- **Signature:** `public GroupByClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the {@code GROUP BY} clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank when {@code b} is {@code true} )
- **Returns:** this {@link GroupByClause} instance for method chaining
##### appendIfOrElse(...) -> GroupByClause
- **Signature:** `public GroupByClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the {@code GROUP BY} clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be {@code null} , empty, or blank)
- **Returns:** this {@link GroupByClause} instance for method chaining

### Class HavingClause (com.landawn.abacus.query.DynamicQuery.HavingClause)
Builder class for constructing the {@code HAVING} clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> HavingClause
- **Signature:** `public HavingClause append(final String expr)`
- **Summary:** Appends a condition to the {@code HAVING} clause.
- **Contract:**
  - <p> Unlike {@link #and(String)} / {@link #or(String)} , this method does <em> not </em> insert a logical connective \\u2014 the caller must include any required {@code AND} / {@code OR} in the argument.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to append (must not be {@code null} , empty, or blank)
- **Returns:** this {@link HavingClause} instance for method chaining
##### appendPlaceholders(...) -> HavingClause
- **Signature:** `public HavingClause appendPlaceholders(final int placeholderCount)`
- **Summary:** Appends question mark placeholders for parameterized queries.
- **Contract:**
  - Use the {@link #appendPlaceholders(int, String, String)} overload when you also need a prefix/postfix (e.g.
  - // Prefer appendPlaceholders(int, String, String) when you want the parentheses tightly attached.
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to append (must not be negative)
- **Returns:** this {@link HavingClause} instance for method chaining
- **Signature:** `public HavingClause appendPlaceholders(final int placeholderCount, final String prefix, final String postfix)`
- **Summary:** Appends question mark placeholders surrounded by prefix and postfix.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code having.append("MAX(score) IN ").appendPlaceholders(3, "(", ")"); // Generates: MAX(score) IN (?, ?, ?) } </pre> <p> If {@code placeholderCount} is {@code 0} , neither {@code prefix} nor {@code postfix} is appended.
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to append (must not be negative)
  - `prefix` (`String`) — the string to add before the question marks (must not be {@code null} )
  - `postfix` (`String`) — the string to add after the question marks (must not be {@code null} )
- **Returns:** this {@link HavingClause} instance for method chaining
##### and(...) -> HavingClause
- **Signature:** `public HavingClause and(final String expr)`
- **Summary:** Adds an {@code AND} condition to the {@code HAVING} clause.
- **Contract:**
  - If called before any {@link #append(String)} , this acts as the first condition and emits {@code HAVING cond} with no leading {@code AND} .
- **Parameters:**
  - `expr` (`String`) — the SQL expression to add with {@code AND} (must not be {@code null} , empty, or blank)
- **Returns:** this {@link HavingClause} instance for method chaining
##### or(...) -> HavingClause
- **Signature:** `public HavingClause or(final String expr)`
- **Summary:** Adds an {@code OR} condition to the {@code HAVING} clause.
- **Contract:**
  - If called before any {@link #append(String)} , this acts as the first condition and emits {@code HAVING cond} with no leading {@code OR} .
- **Parameters:**
  - `expr` (`String`) — the SQL expression to add with {@code OR} (must not be {@code null} , empty, or blank)
- **Returns:** this {@link HavingClause} instance for method chaining
##### appendIf(...) -> HavingClause
- **Signature:** `public HavingClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the {@code HAVING} clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank when {@code b} is {@code true} )
- **Returns:** this {@link HavingClause} instance for method chaining
##### appendIfOrElse(...) -> HavingClause
- **Signature:** `public HavingClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the {@code HAVING} clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be {@code null} , empty, or blank)
- **Returns:** this {@link HavingClause} instance for method chaining

### Class OrderByClause (com.landawn.abacus.query.DynamicQuery.OrderByClause)
Builder class for constructing the {@code ORDER BY} clause of a SQL query.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### append(...) -> OrderByClause
- **Signature:** `public OrderByClause append(final String propOrColumnName)`
- **Summary:** Appends a column (with optional sort direction) to the {@code ORDER BY} clause.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name with optional {@code ASC} / {@code DESC} (must not be {@code null} , empty, or blank)
- **Returns:** this {@link OrderByClause} instance for method chaining
- **Signature:** `public OrderByClause append(final Collection<String> columns)`
- **Summary:** Appends multiple columns to the {@code ORDER BY} clause.
- **Contract:**
  - If the collection is empty, this method does nothing.
- **Parameters:**
  - `columns` (`Collection<String>`) — collection of column names with optional sort directions (may be {@code null} or empty; individual elements must not be {@code null} , empty, or blank)
- **Returns:** this {@link OrderByClause} instance for method chaining
##### appendIf(...) -> OrderByClause
- **Signature:** `public OrderByClause appendIf(final boolean b, final String textToAppend)`
- **Summary:** Conditionally appends a string to the {@code ORDER BY} clause based on a boolean condition.
- **Contract:**
  - The string is only appended if the condition is true.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppend` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank when {@code b} is {@code true} )
- **Returns:** this {@link OrderByClause} instance for method chaining
##### appendIfOrElse(...) -> OrderByClause
- **Signature:** `public OrderByClause appendIfOrElse(final boolean b, final String textToAppendWhenTrue, final String textToAppendWhenFalse)`
- **Summary:** Appends one of two strings to the {@code ORDER BY} clause based on a boolean condition.
- **Parameters:**
  - `b` (`boolean`) — the condition to check
  - `textToAppendWhenTrue` (`String`) — the string to append if condition is true (must not be {@code null} , empty, or blank)
  - `textToAppendWhenFalse` (`String`) — the string to append if condition is false (must not be {@code null} , empty, or blank)
- **Returns:** this {@link OrderByClause} instance for method chaining

### Class Filters (com.landawn.abacus.query.Filters)
Factory class for creating SQL {@link Condition} objects used in query construction.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### alwaysTrue(...) -> Expression
- **Signature:** `@Deprecated public static Expression alwaysTrue()`
- **Summary:** Returns a condition that always evaluates to true.
- **Parameters:**
  - (none)
- **Returns:** an {@link Expression} that always evaluates to true (1 &lt; 2)
##### alwaysFalse(...) -> Expression
- **Signature:** `@Deprecated public static Expression alwaysFalse()`
- **Summary:** Returns a condition that always evaluates to false.
- **Parameters:**
  - (none)
- **Returns:** an {@link Expression} that always evaluates to false (1 &gt; 2)
##### namedProperty(...) -> NamedProperty
- **Signature:** `@Beta public static NamedProperty namedProperty(final String propName)`
- **Summary:** Creates (or returns a cached) {@link NamedProperty} instance representing a property/column name.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column (must not be {@code null} , empty, or blank)
- **Returns:** a {@link NamedProperty} instance
##### expr(...) -> Expression
- **Signature:** `public static Expression expr(final String literal)`
- **Summary:** Creates an {@link Expression} from a string literal.
- **Parameters:**
  - `literal` (`String`) — the SQL expression as a string (must not be {@code null} )
- **Returns:** an {@link Expression} instance
##### not(...) -> Not
- **Signature:** `public static Not not(final Condition condition)`
- **Summary:** Creates a negation condition that represents the logical {@code NOT} of the provided condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition to negate (must not be {@code null} and must be a composable condition)
- **Returns:** a {@link Not} condition that wraps and negates the provided condition
- **See also:** Not, Condition
##### binary(...) -> Binary
- **Signature:** `public static Binary binary(final String propName, final Operator operator, final Object propValue)`
- **Summary:** Creates a {@link Binary} condition with the specified property name, operator, and value.
- **Contract:**
  - This is a general factory for creating conditions with a binary comparison/membership {@link Operator} , useful when one of the convenience factories (e.g.
  - <p> The {@code operator} must be a valid binary comparison or membership operator: one of {@link Operator#EQUAL} , {@link Operator#NOT_EQUAL} , {@link Operator#NOT_EQUAL_ANSI} , {@link Operator#GREATER_THAN} , {@link Operator#GREATER_THAN_OR_EQUAL} , {@link Operator#LESS_THAN} , {@link Operator#LESS_THAN_OR_EQUAL} , {@link Operator#LIKE} , {@link Operator#NOT_LIKE} , {@link Operator#IS} , {@link Operator#IS_NOT} , {@link Operator#IN} , or {@link Operator#NOT_IN} .
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `operator` (`Operator`) — the binary comparison operator to use (must not be {@code null} ; membership and structural operators are rejected)
  - `propValue` (`Object`) — the value to compare against; may be a literal, {@code null} , or another {@link Condition} such as a {@link SubQuery} . For an {@code IN} / {@code NOT_IN} operator, a {@link Collection} or array value is copied defensively and must be non-empty.
- **Returns:** a {@link Binary} condition
- **Signature:** `public static Binary binary(final String propName, final Operator operator)`
- **Summary:** Creates a parameterized {@link Binary} condition for use with prepared statements.
- **Contract:**
  - The condition uses a question mark ( {@code ?} ) placeholder in place of the value, which is provided later when the statement is executed.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `operator` (`Operator`) — the binary comparison operator to use (must not be {@code null} ; membership and structural operators are rejected)
- **Returns:** a {@link Binary} condition with a {@code ?} placeholder value
- **See also:** #binary(String, Operator, Object)
##### equal(...) -> Equal
- **Signature:** `public static Equal equal(final String propName, final Object propValue)`
- **Summary:** Creates an equality condition ( {@code =} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare for equality; may be a literal, {@code null} (renders as {@code IS NULL} ), or another {@link Condition} such as a {@link SubQuery}
- **Returns:** an {@link Equal} condition
- **Signature:** `public static Equal equal(final String propName)`
- **Summary:** Creates a parameterized equality condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link Equal} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### eq(...) -> Equal
- **Signature:** `public static Equal eq(final String propName, final Object propValue)`
- **Summary:** Creates an equality condition ( {@code =} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare for equality
- **Returns:** an {@link Equal} condition
- **Signature:** `public static Equal eq(final String propName)`
- **Summary:** Creates a parameterized equality condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link Equal} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### anyEqual(...) -> Or
- **Signature:** `public static Or anyEqual(final Map<String, ?> props)`
- **Summary:** Creates an {@code OR} condition from a map where each entry represents a property-value equality check across <b> different </b> columns/properties.
- **Parameters:**
  - `props` (`Map<String, ?>`) — map of property names to values (must not be empty). Entries are consumed once during this call; subsequent mutations do not affect the returned condition
- **Returns:** an {@link Or} condition
- **See also:** NamedProperty#equalsAny(Object...)
- **Signature:** `public static Or anyEqual(final Object entity)`
- **Summary:** Creates an {@code OR} condition from an entity object using all its properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object whose properties will be used
- **Returns:** an {@link Or} condition
- **Signature:** `public static Or anyEqual(final Object entity, final Collection<String> includedPropNames)`
- **Summary:** Creates an {@code OR} condition from an entity object using only the specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object
  - `includedPropNames` (`Collection<String>`) — the property names to include (must not be empty). Names are consumed once during this call; subsequent mutations do not affect the returned condition
- **Returns:** an {@link Or} condition
- **Signature:** `public static Or anyEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2)`
- **Summary:** Creates an {@code OR} condition with two property-value pairs across <b> different </b> columns/properties.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
- **Returns:** an {@link Or} condition
- **Signature:** `public static Or anyEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3, final Object propValue3)`
- **Summary:** Creates an {@code OR} condition with three property-value pairs across <b> different </b> columns/properties.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
  - `propName3` (`String`) — third property name
  - `propValue3` (`Object`) — third property value
- **Returns:** an {@link Or} condition
##### allEqual(...) -> And
- **Signature:** `public static And allEqual(final Map<String, ?> props)`
- **Summary:** Creates an {@code AND} condition from a map where each entry represents a property-value equality check across <b> different </b> columns/properties.
- **Parameters:**
  - `props` (`Map<String, ?>`) — map of property names to values (must not be empty). Entries are consumed once during this call; subsequent mutations do not affect the returned condition
- **Returns:** an {@link And} condition
- **Signature:** `public static And allEqual(final Object entity)`
- **Summary:** Creates an {@code AND} condition from an entity object using all its properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object whose properties will be used
- **Returns:** an {@link And} condition
- **Signature:** `public static And allEqual(final Object entity, final Collection<String> includedPropNames)`
- **Summary:** Creates an {@code AND} condition from an entity object using only the specified properties.
- **Parameters:**
  - `entity` (`Object`) — the entity object
  - `includedPropNames` (`Collection<String>`) — the property names to include (must not be empty). Names are consumed once during this call; subsequent mutations do not affect the returned condition
- **Returns:** an {@link And} condition
- **Signature:** `public static And allEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2)`
- **Summary:** Creates an {@code AND} condition with two property-value pairs across <b> different </b> columns/properties.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
- **Returns:** an {@link And} condition
- **Signature:** `public static And allEqual(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3, final Object propValue3)`
- **Summary:** Creates an {@code AND} condition with three property-value pairs across <b> different </b> columns/properties.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `propValue1` (`Object`) — first property value
  - `propName2` (`String`) — second property name
  - `propValue2` (`Object`) — second property value
  - `propName3` (`String`) — third property name
  - `propValue3` (`Object`) — third property value
- **Returns:** an {@link And} condition
##### anyOfAllEqual(...) -> Or
- **Signature:** `@Beta public static Or anyOfAllEqual(final Collection<?> entitiesOrPropMaps)`
- **Summary:** Creates an {@code OR} of per-row {@code AND} -of-equals conditions &mdash; i.e.
- **Contract:**
  - a "match any row" filter, where a row matches when <em> all </em> of its columns equal the given values.
  - <p> If the first non-null element is a {@link Map} , all non-null elements must be maps with non-null {@link String} keys.
  - Otherwise, every non-null element must be an entity rather than a map, and all properties selected from the first entity's class are used.
- **Parameters:**
  - `entitiesOrPropMaps` (`Collection<?>`) — collection of property maps or entity objects (must not be empty)
- **Returns:** an {@link Or} condition
- **See also:** #anyOfAllEqual(Collection, Collection), #anyEqual(Map), #allEqual(Map)
- **Signature:** `@Beta public static Or anyOfAllEqual(final Collection<?> entities, final Collection<String> includedPropNames)`
- **Summary:** Creates an {@code OR} of per-row {@code AND} -of-equals conditions &mdash; a "match any row" filter &mdash; using only the specified properties of each entity.
- **Parameters:**
  - `entities` (`Collection<?>`) — collection of entity objects (must not be empty)
  - `includedPropNames` (`Collection<String>`) — the property names to include (must not be empty). Both input collections are snapshotted during the call
- **Returns:** an {@link Or} condition
- **See also:** #anyOfAllEqual(Collection), #allEqual(Object, Collection)
##### gtAndLt(...) -> And
- **Signature:** `public static And gtAndLt(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a {@code BETWEEN} -like condition using greater-than ( {@code gt} ) and less-than ( {@code lt} ) comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (exclusive)
  - `maxValue` (`Object`) — the maximum value (exclusive)
- **Returns:** an {@link And} condition
- **Signature:** `public static And gtAndLt(final String propName)`
- **Summary:** Creates a parameterized {@code BETWEEN} -like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link And} condition with parameter placeholders
##### geAndLt(...) -> And
- **Signature:** `public static And geAndLt(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a {@code BETWEEN} -like condition using greater-than-or-equal ( {@code ge} ) and less-than ( {@code lt} ) comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (exclusive)
- **Returns:** an {@link And} condition
- **Signature:** `public static And geAndLt(final String propName)`
- **Summary:** Creates a parameterized {@code BETWEEN} -like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link And} condition with parameter placeholders
##### geAndLe(...) -> And
- **Signature:** `public static And geAndLe(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a {@code BETWEEN} -like condition using greater-than-or-equal ( {@code ge} ) and less-than-or-equal ( {@code le} ) comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** an {@link And} condition
- **Signature:** `public static And geAndLe(final String propName)`
- **Summary:** Creates a parameterized {@code BETWEEN} -like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link And} condition with parameter placeholders
##### gtAndLe(...) -> And
- **Signature:** `public static And gtAndLe(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a {@code BETWEEN} -like condition using greater-than ( {@code gt} ) and less-than-or-equal ( {@code le} ) comparisons.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (exclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** an {@link And} condition
- **Signature:** `public static And gtAndLe(final String propName)`
- **Summary:** Creates a parameterized {@code BETWEEN} -like condition for prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link And} condition with parameter placeholders
##### idToCond(...) -> And
- **Signature:** `public static And idToCond(final EntityId entityId)`
- **Summary:** Converts an {@link EntityId} to an {@link And} condition where each key-value pair becomes an equality check.
- **Parameters:**
  - `entityId` (`EntityId`) — the {@link EntityId} containing key-value pairs (must not be null). Entries are consumed once as key/value pairs during this call
- **Returns:** an {@link And} condition
- **Signature:** `public static Or idToCond(final Collection<? extends EntityId> entityIds)`
- **Summary:** Converts a collection of {@link EntityId} s to an {@link Or} condition where each {@link EntityId} becomes an {@link And} condition.
- **Parameters:**
  - `entityIds` (`Collection<? extends EntityId>`) — collection of {@link EntityId} s (must not be {@code null} , empty, or contain null)
- **Returns:** an {@link Or} condition
##### id2Cond(...) -> And
- **Signature:** `@Deprecated public static And id2Cond(final EntityId entityId)`
- **Summary:** Converts an {@link EntityId} to an {@link And} condition where each key-value pair becomes an equality check.
- **Parameters:**
  - `entityId` (`EntityId`) — the {@link EntityId} containing key-value pairs (must not be null)
- **Returns:** an {@link And} condition
- **Signature:** `@Deprecated public static Or id2Cond(final Collection<? extends EntityId> entityIds)`
- **Summary:** Converts a collection of {@link EntityId} s to an {@link Or} condition where each {@link EntityId} becomes an {@link And} condition.
- **Parameters:**
  - `entityIds` (`Collection<? extends EntityId>`) — collection of {@link EntityId} s (must not be {@code null} or empty)
- **Returns:** an {@link Or} condition
##### notEqual(...) -> NotEqual
- **Signature:** `public static NotEqual notEqual(final String propName, final Object propValue)`
- **Summary:** Creates a not-equal condition ( {@code !=} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare for inequality; may be a literal, {@code null} (renders as {@code IS NOT NULL} ), or another {@link Condition} such as a {@link SubQuery}
- **Returns:** a {@link NotEqual} condition
- **Signature:** `public static NotEqual notEqual(final String propName)`
- **Summary:** Creates a parameterized not-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link NotEqual} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### ne(...) -> NotEqual
- **Signature:** `public static NotEqual ne(final String propName, final Object propValue)`
- **Summary:** Creates a not-equal condition ( {@code !=} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare for inequality
- **Returns:** a {@link NotEqual} condition
- **Signature:** `public static NotEqual ne(final String propName)`
- **Summary:** Creates a parameterized not-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link NotEqual} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### greaterThan(...) -> GreaterThan
- **Signature:** `public static GreaterThan greaterThan(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than condition ( {@code >} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a {@link GreaterThan} condition
- **Signature:** `public static GreaterThan greaterThan(final String propName)`
- **Summary:** Creates a parameterized greater-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link GreaterThan} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### gt(...) -> GreaterThan
- **Signature:** `public static GreaterThan gt(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than condition ( {@code >} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a {@link GreaterThan} condition
- **Signature:** `public static GreaterThan gt(final String propName)`
- **Summary:** Creates a parameterized greater-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link GreaterThan} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### greaterThanOrEqual(...) -> GreaterThanOrEqual
- **Signature:** `public static GreaterThanOrEqual greaterThanOrEqual(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than-or-equal condition ( {@code >=} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a {@link GreaterThanOrEqual} condition
- **Signature:** `public static GreaterThanOrEqual greaterThanOrEqual(final String propName)`
- **Summary:** Creates a parameterized greater-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link GreaterThanOrEqual} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### ge(...) -> GreaterThanOrEqual
- **Signature:** `public static GreaterThanOrEqual ge(final String propName, final Object propValue)`
- **Summary:** Creates a greater-than-or-equal condition ( {@code >=} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a {@link GreaterThanOrEqual} condition
- **Signature:** `public static GreaterThanOrEqual ge(final String propName)`
- **Summary:** Creates a parameterized greater-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link GreaterThanOrEqual} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### lessThan(...) -> LessThan
- **Signature:** `public static LessThan lessThan(final String propName, final Object propValue)`
- **Summary:** Creates a less-than condition ( {@code <} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a {@link LessThan} condition
- **Signature:** `public static LessThan lessThan(final String propName)`
- **Summary:** Creates a parameterized less-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link LessThan} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### lt(...) -> LessThan
- **Signature:** `public static LessThan lt(final String propName, final Object propValue)`
- **Summary:** Creates a less-than condition ( {@code <} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a {@link LessThan} condition
- **Signature:** `public static LessThan lt(final String propName)`
- **Summary:** Creates a parameterized less-than condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link LessThan} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### lessThanOrEqual(...) -> LessThanOrEqual
- **Signature:** `public static LessThanOrEqual lessThanOrEqual(final String propName, final Object propValue)`
- **Summary:** Creates a less-than-or-equal condition ( {@code <=} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a {@link LessThanOrEqual} condition
- **Signature:** `public static LessThanOrEqual lessThanOrEqual(final String propName)`
- **Summary:** Creates a parameterized less-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link LessThanOrEqual} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### le(...) -> LessThanOrEqual
- **Signature:** `public static LessThanOrEqual le(final String propName, final Object propValue)`
- **Summary:** Creates a less-than-or-equal condition ( {@code <=} ) for the specified property and value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the value to compare against
- **Returns:** a {@link LessThanOrEqual} condition
- **Signature:** `public static LessThanOrEqual le(final String propName)`
- **Summary:** Creates a parameterized less-than-or-equal condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link LessThanOrEqual} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### between(...) -> Between
- **Signature:** `public static Between between(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a {@link Between} condition for the specified property and range values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** a {@link Between} condition
- **Signature:** `public static Between between(final String propName)`
- **Summary:** Creates a parameterized {@link Between} condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link Between} condition with parameter placeholders
- **See also:** com.landawn.abacus.query.SqlBuilder
##### notBetween(...) -> NotBetween
- **Signature:** `public static NotBetween notBetween(final String propName, final Object minValue, final Object maxValue)`
- **Summary:** Creates a {@link NotBetween} condition for the specified property and range values.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code NotBetween condition = Filters.notBetween("temperature", -10, 40); // SQL fragment: temperature NOT BETWEEN -10 AND 40 // True when temperature < -10 OR temperature > 40 } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `minValue` (`Object`) — the minimum value of the excluded range (inclusive)
  - `maxValue` (`Object`) — the maximum value of the excluded range (inclusive)
- **Returns:** a {@link NotBetween} condition
- **Signature:** `public static NotBetween notBetween(final String propName)`
- **Summary:** Creates a parameterized {@link NotBetween} condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link NotBetween} condition with parameter placeholders
- **See also:** com.landawn.abacus.query.SqlBuilder
##### like(...) -> Like
- **Signature:** `public static Like like(final String propName, final String pattern)`
- **Summary:** Creates a {@link Like} condition for pattern matching.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `pattern` (`String`) — the pattern to match (can include SQL wildcards). Passing {@code null} renders as {@code propName LIKE null} , which is not a meaningful SQL comparison; do not pass {@code null} (the {@link #contains(String, String)} / {@link #startsWith(String, String)} siblings reject a {@code null} value)
- **Returns:** a {@link Like} condition
- **Signature:** `public static Like like(final String propName, final Object propValue)`
- **Summary:** Creates a {@link Like} condition with a non-string operand such as an expression or subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the operand to compare with {@code LIKE} ; may be a literal or another condition
- **Returns:** a {@link Like} condition
- **Signature:** `public static Like like(final String propName)`
- **Summary:** Creates a parameterized {@link Like} condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link Like} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### notLike(...) -> NotLike
- **Signature:** `public static NotLike notLike(final String propName, final String pattern)`
- **Summary:** Creates a {@link NotLike} condition for pattern matching exclusion.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `pattern` (`String`) — the pattern to exclude (can include SQL wildcards). Passing {@code null} renders as {@code propName NOT LIKE null} , which is not a meaningful SQL comparison; do not pass {@code null} (the {@link #notContains(String, String)} sibling rejects a {@code null} value)
- **Returns:** a {@link NotLike} condition
- **Signature:** `public static NotLike notLike(final String propName, final Object propValue)`
- **Summary:** Creates a {@link NotLike} condition with a non-string operand such as an expression or subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`Object`) — the operand to compare with {@code NOT LIKE} ; may be a literal or another condition
- **Returns:** a {@link NotLike} condition
- **Signature:** `public static NotLike notLike(final String propName)`
- **Summary:** Creates a parameterized {@link NotLike} condition for use with prepared statements.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** a {@link NotLike} condition with a parameter placeholder
- **See also:** com.landawn.abacus.query.SqlBuilder
##### contains(...) -> Like
- **Signature:** `public static Like contains(final String propName, final String propValue)`
- **Summary:** Creates a {@link Like} condition that checks if the property contains the specified value.
- **Contract:**
  - Creates a {@link Like} condition that checks if the property contains the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the value to search for (must not be {@code null} )
- **Returns:** a {@link Like} condition
##### notContains(...) -> NotLike
- **Signature:** `public static NotLike notContains(final String propName, final String propValue)`
- **Summary:** Creates a {@link NotLike} condition that checks if the property does not contain the specified value.
- **Contract:**
  - Creates a {@link NotLike} condition that checks if the property does not contain the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the value to exclude (must not be {@code null} )
- **Returns:** a {@link NotLike} condition
##### startsWith(...) -> Like
- **Signature:** `public static Like startsWith(final String propName, final String propValue)`
- **Summary:** Creates a {@link Like} condition that checks if the property starts with the specified value.
- **Contract:**
  - Creates a {@link Like} condition that checks if the property starts with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the prefix to search for (must not be {@code null} )
- **Returns:** a {@link Like} condition
##### notStartsWith(...) -> NotLike
- **Signature:** `public static NotLike notStartsWith(final String propName, final String propValue)`
- **Summary:** Creates a {@link NotLike} condition that checks if the property does not start with the specified value.
- **Contract:**
  - Creates a {@link NotLike} condition that checks if the property does not start with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the prefix to exclude (must not be {@code null} )
- **Returns:** a {@link NotLike} condition
##### endsWith(...) -> Like
- **Signature:** `public static Like endsWith(final String propName, final String propValue)`
- **Summary:** Creates a {@link Like} condition that checks if the property ends with the specified value.
- **Contract:**
  - Creates a {@link Like} condition that checks if the property ends with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the suffix to search for (must not be {@code null} )
- **Returns:** a {@link Like} condition
##### notEndsWith(...) -> NotLike
- **Signature:** `public static NotLike notEndsWith(final String propName, final String propValue)`
- **Summary:** Creates a {@link NotLike} condition that checks if the property does not end with the specified value.
- **Contract:**
  - Creates a {@link NotLike} condition that checks if the property does not end with the specified value.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `propValue` (`String`) — the suffix to exclude (must not be {@code null} )
- **Returns:** a {@link NotLike} condition
##### isNull(...) -> IsNull
- **Signature:** `public static IsNull isNull(final String propName)`
- **Summary:** Creates an {@link IsNull} condition to check if a property value is null.
- **Contract:**
  - Creates an {@link IsNull} condition to check if a property value is null.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link IsNull} condition
##### isNullOrEmpty(...) -> Or
- **Signature:** `@Beta public static Or isNullOrEmpty(final String propName)`
- **Summary:** Creates a condition to check if a property is null or empty string.
- **Contract:**
  - Creates a condition to check if a property is null or empty string.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link Or} condition combining null and empty checks
##### isNullOrZero(...) -> Or
- **Signature:** `@Beta public static Or isNullOrZero(final String propName)`
- **Summary:** Creates a condition to check if a property is null or zero.
- **Contract:**
  - Creates a condition to check if a property is null or zero.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link Or} condition combining null and zero checks
##### isNotNull(...) -> IsNotNull
- **Signature:** `public static IsNotNull isNotNull(final String propName)`
- **Summary:** Creates an {@link IsNotNull} condition to check if a property value is not null.
- **Contract:**
  - Creates an {@link IsNotNull} condition to check if a property value is not null.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link IsNotNull} condition
##### isNotNullAndNotEmpty(...) -> And
- **Signature:** `@Beta public static And isNotNullAndNotEmpty(final String propName)`
- **Summary:** Creates a compound condition to check that a property is neither null nor an empty string.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link And} condition combining not-null and not-empty checks
##### isNotNullAndNotZero(...) -> And
- **Signature:** `@Beta public static And isNotNullAndNotZero(final String propName)`
- **Summary:** Creates a compound condition to check that a property is neither null nor zero.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link And} condition combining not-null and non-zero checks
##### isNaN(...) -> IsNaN
- **Signature:** `public static IsNaN isNaN(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is {@code NaN} (Not a Number).
- **Contract:**
  - Creates a condition to check if a numeric property value is {@code NaN} (Not a Number).
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link IsNaN} condition
##### isNotNaN(...) -> IsNotNaN
- **Signature:** `public static IsNotNaN isNotNaN(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is not {@code NaN} .
- **Contract:**
  - Creates a condition to check if a numeric property value is not {@code NaN} .
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link IsNotNaN} condition
##### isInfinite(...) -> IsInfinite
- **Signature:** `public static IsInfinite isInfinite(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is infinite.
- **Contract:**
  - Creates a condition to check if a numeric property value is infinite.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link IsInfinite} condition
##### isNotInfinite(...) -> IsNotInfinite
- **Signature:** `public static IsNotInfinite isNotInfinite(final String propName)`
- **Summary:** Creates a condition to check if a numeric property value is not infinite.
- **Contract:**
  - Creates a condition to check if a numeric property value is not infinite.
- **Parameters:**
  - `propName` (`String`) — the property/column name
- **Returns:** an {@link IsNotInfinite} condition
##### is(...) -> Is
- **Signature:** `public static Is is(final String propName, final Object propValue)`
- **Summary:** Creates an {@link Is} condition (SQL {@code IS} predicate) for the specified property and value.
- **Contract:**
  - <p> If {@code propValue} is Java {@code null} , the rendered SQL collapses to {@code propName IS NULL} .
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the right-hand value (typically an {@link Expression} ); may be {@code null} (renders as {@code IS NULL} )
- **Returns:** an {@link Is} condition
##### isNot(...) -> IsNot
- **Signature:** `public static IsNot isNot(final String propName, final Object propValue)`
- **Summary:** Creates an {@link IsNot} condition (SQL {@code IS NOT} predicate) for the specified property and value.
- **Contract:**
  - <p> If {@code propValue} is Java {@code null} , the rendered SQL collapses to {@code propName IS NOT NULL} .
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the right-hand value (typically an {@link Expression} ); may be {@code null} (renders as {@code IS NOT NULL} )
- **Returns:** an {@link IsNot} condition
##### or(...) -> Or
- **Signature:** `public static Or or(final Condition... conditions)`
- **Summary:** Creates an {@link Or} junction combining multiple conditions.
- **Contract:**
  - At least one condition must be true for the {@code OR} to be true.
- **Parameters:**
  - `conditions` (`Condition[]`) — the array of conditions to combine with {@code OR} ; {@code null} or empty is permitted and yields an empty junction (which renders as an empty string)
- **Returns:** an {@link Or} junction
- **Signature:** `public static Or or(final Collection<? extends Condition> conditions)`
- **Summary:** Creates an {@link Or} junction combining multiple conditions from a collection.
- **Contract:**
  - At least one condition must be true for the {@code OR} to be true.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with {@code OR} ; {@code null} or empty is permitted and yields an empty junction
- **Returns:** an {@link Or} junction
##### and(...) -> And
- **Signature:** `public static And and(final Condition... conditions)`
- **Summary:** Creates an {@link And} junction combining multiple conditions.
- **Contract:**
  - All conditions must be true for the {@code AND} to be true.
- **Parameters:**
  - `conditions` (`Condition[]`) — the array of conditions to combine with {@code AND} ; {@code null} or empty is permitted and yields an empty junction (which renders as an empty string)
- **Returns:** an {@link And} junction
- **Signature:** `public static And and(final Collection<? extends Condition> conditions)`
- **Summary:** Creates an {@link And} junction combining multiple conditions from a collection.
- **Contract:**
  - All conditions must be true for the {@code AND} to be true.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with {@code AND} ; {@code null} or empty is permitted and yields an empty junction
- **Returns:** an {@link And} junction
##### junction(...) -> Junction
- **Signature:** `@Beta public static Junction junction(final Operator operator, final Condition... conditions)`
- **Summary:** Creates a {@link Junction} combining multiple conditions with the given operator, which must be {@link Operator#AND} or {@link Operator#OR} .
- **Contract:**
  - Creates a {@link Junction} combining multiple conditions with the given operator, which must be {@link Operator#AND} or {@link Operator#OR} .
  - This is useful when the operator is chosen at runtime; for a fixed operator prefer {@link #and(Condition...)} or {@link #or(Condition...)} .
- **Parameters:**
  - `operator` (`Operator`) — the junction operator; must be {@link Operator#AND} or {@link Operator#OR}
  - `conditions` (`Condition[]`) — the array of conditions to combine; {@code null} or empty is permitted and yields an empty junction
- **Returns:** a {@link Junction} with the specified operator
- **Signature:** `@Beta public static Junction junction(final Operator operator, final Collection<? extends Condition> conditions)`
- **Summary:** Creates a {@link Junction} combining conditions from a collection with the given operator, which must be {@link Operator#AND} or {@link Operator#OR} .
- **Contract:**
  - Creates a {@link Junction} combining conditions from a collection with the given operator, which must be {@link Operator#AND} or {@link Operator#OR} .
  - This is useful when the operator is chosen at runtime; for a fixed operator prefer {@link #and(Collection)} or {@link #or(Collection)} .
- **Parameters:**
  - `operator` (`Operator`) — the junction operator; must be {@link Operator#AND} or {@link Operator#OR}
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine; {@code null} or empty is permitted and yields an empty junction
- **Returns:** a {@link Junction} with the specified operator
##### where(...) -> Where
- **Signature:** `public static Where where(final Condition condition)`
- **Summary:** Creates a {@link Where} clause with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition for the {@code WHERE} clause (must not be {@code null} )
- **Returns:** a {@link Where} clause
- **Signature:** `public static Where where(final String expr)`
- **Summary:** Creates a {@link Where} clause from a raw SQL expression string.
- **Parameters:**
  - `expr` (`String`) — the SQL expression as a string (must not be {@code null} , empty, or blank)
- **Returns:** a {@link Where} clause
##### groupByAsc(...) -> GroupBy
- **Signature:** `public static GroupBy groupByAsc(final String propOrColumnName)`
- **Summary:** Creates a {@link GroupBy} clause with ascending order for a single property.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property/column name to group by ascending
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupByAsc(final String... propNames)`
- **Summary:** Creates a {@link GroupBy} clause with ascending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to group by ascending
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupByAsc(final Collection<String> propNames)`
- **Summary:** Creates a {@link GroupBy} clause with ascending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by ascending
- **Returns:** a {@link GroupBy} clause
##### groupByDesc(...) -> GroupBy
- **Signature:** `public static GroupBy groupByDesc(final String propOrColumnName)`
- **Summary:** Creates a {@link GroupBy} clause with descending order for a single property.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property/column name to group by descending
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupByDesc(final String... propNames)`
- **Summary:** Creates a {@link GroupBy} clause with descending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to group by descending
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupByDesc(final Collection<String> propNames)`
- **Summary:** Creates a {@link GroupBy} clause with descending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by descending
- **Returns:** a {@link GroupBy} clause
##### groupBy(...) -> GroupBy
- **Signature:** `public static GroupBy groupBy(final String... propNames)`
- **Summary:** Creates a {@link GroupBy} clause with the specified property names.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to group by
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupBy(final Collection<String> propNames)`
- **Summary:** Creates a {@link GroupBy} clause with properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates a {@link GroupBy} clause with properties and specified sort direction.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to group by
  - `direction` (`SortDirection`) — the sort direction ( {@code ASC} or {@code DESC} )
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupBy(final String propName, final SortDirection direction)`
- **Summary:** Creates a {@link GroupBy} clause with a single property and sort direction.
- **Parameters:**
  - `propName` (`String`) — the property/column name to group by
  - `direction` (`SortDirection`) — the sort direction ( {@code ASC} or {@code DESC} )
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2)`
- **Summary:** Creates a {@link GroupBy} clause with two properties and their respective sort directions.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `direction1` (`SortDirection`) — first property sort direction
  - `propName2` (`String`) — second property name
  - `direction2` (`SortDirection`) — second property sort direction
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2, final String propName3, final SortDirection direction3)`
- **Summary:** Creates a {@link GroupBy} clause with three properties and their respective sort directions.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `direction1` (`SortDirection`) — first property sort direction
  - `propName2` (`String`) — second property name
  - `direction2` (`SortDirection`) — second property sort direction
  - `propName3` (`String`) — third property name
  - `direction3` (`SortDirection`) — third property sort direction
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupBy(final Map<String, SortDirection> groupings)`
- **Summary:** Creates a {@link GroupBy} clause from a map of property names to sort directions.
- **Contract:**
  - The map should be a {@link java.util.LinkedHashMap} to preserve insertion order.
- **Parameters:**
  - `groupings` (`Map<String, SortDirection>`) — map of property names to sort directions (should be a {@link java.util.LinkedHashMap} to preserve order)
- **Returns:** a {@link GroupBy} clause
- **Signature:** `public static GroupBy groupBy(final Condition condition)`
- **Summary:** Creates a {@link GroupBy} clause with a custom condition.
- **Parameters:**
  - `condition` (`Condition`) — the grouping condition (must not be {@code null} )
- **Returns:** a {@link GroupBy} clause
##### having(...) -> Having
- **Signature:** `public static Having having(final Condition condition)`
- **Summary:** Creates a {@link Having} clause with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the condition for the {@code HAVING} clause (must not be {@code null} )
- **Returns:** a {@link Having} clause
- **Signature:** `public static Having having(final String expr)`
- **Summary:** Creates a {@link Having} clause from a raw SQL expression string.
- **Parameters:**
  - `expr` (`String`) — the SQL expression as a string (must not be {@code null} , empty, or blank)
- **Returns:** a {@link Having} clause
##### orderByAsc(...) -> OrderBy
- **Signature:** `public static OrderBy orderByAsc(final String propOrColumnName)`
- **Summary:** Creates an {@link OrderBy} clause with ascending order for a single property.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property/column name to order by ascending
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderByAsc(final String... propNames)`
- **Summary:** Creates an {@link OrderBy} clause with ascending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by ascending
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderByAsc(final Collection<String> propNames)`
- **Summary:** Creates an {@link OrderBy} clause with ascending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by ascending
- **Returns:** an {@link OrderBy} clause
##### orderByDesc(...) -> OrderBy
- **Signature:** `public static OrderBy orderByDesc(final String propOrColumnName)`
- **Summary:** Creates an {@link OrderBy} clause with descending order for a single property.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property/column name to order by descending
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderByDesc(final String... propNames)`
- **Summary:** Creates an {@link OrderBy} clause with descending order for the specified properties.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by descending
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderByDesc(final Collection<String> propNames)`
- **Summary:** Creates an {@link OrderBy} clause with descending order for properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by descending
- **Returns:** an {@link OrderBy} clause
##### orderBy(...) -> OrderBy
- **Signature:** `public static OrderBy orderBy(final String... propNames)`
- **Summary:** Creates an {@link OrderBy} clause with the specified property names.
- **Parameters:**
  - `propNames` (`String[]`) — the property/column names to order by
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderBy(final Collection<String> propNames)`
- **Summary:** Creates an {@link OrderBy} clause with properties from a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates an {@link OrderBy} clause with properties and specified sort direction.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names to order by
  - `direction` (`SortDirection`) — the sort direction ( {@code ASC} or {@code DESC} )
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderBy(final String propName, final SortDirection direction)`
- **Summary:** Creates an {@link OrderBy} clause with a single property and sort direction.
- **Parameters:**
  - `propName` (`String`) — the property/column name to order by
  - `direction` (`SortDirection`) — the sort direction ( {@code ASC} or {@code DESC} )
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2)`
- **Summary:** Creates an {@link OrderBy} clause with two properties and their respective sort directions.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `direction1` (`SortDirection`) — first property sort direction
  - `propName2` (`String`) — second property name
  - `direction2` (`SortDirection`) — second property sort direction
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderBy(final String propName1, final SortDirection direction1, final String propName2, final SortDirection direction2, final String propName3, final SortDirection direction3)`
- **Summary:** Creates an {@link OrderBy} clause with three properties and their respective sort directions.
- **Parameters:**
  - `propName1` (`String`) — first property name
  - `direction1` (`SortDirection`) — first property sort direction
  - `propName2` (`String`) — second property name
  - `direction2` (`SortDirection`) — second property sort direction
  - `propName3` (`String`) — third property name
  - `direction3` (`SortDirection`) — third property sort direction
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Creates an {@link OrderBy} clause from a map of property names to sort directions.
- **Contract:**
  - The map should be a {@link java.util.LinkedHashMap} to preserve insertion order.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — map of property names to sort directions (should be a {@link java.util.LinkedHashMap} to preserve order)
- **Returns:** an {@link OrderBy} clause
- **Signature:** `public static OrderBy orderBy(final Condition condition)`
- **Summary:** Creates an {@link OrderBy} clause with a custom condition.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code OrderBy orderBy = Filters.orderBy( Filters.expr("CASE WHEN status = 'urgent' THEN 1 ELSE 2 END, created_date DESC") ); // Results in SQL like: ORDER BY CASE WHEN status = 'urgent' THEN 1 ELSE 2 END, created_date DESC } </pre>
- **Parameters:**
  - `condition` (`Condition`) — the ordering condition (must not be {@code null} )
- **Returns:** an {@link OrderBy} clause
##### on(...) -> On
- **Signature:** `public static On on(final Condition condition)`
- **Summary:** Creates an {@link On} clause for JOIN operations with the specified condition.
- **Parameters:**
  - `condition` (`Condition`) — the join condition (must not be {@code null} )
- **Returns:** an {@link On} clause
- **Signature:** `public static On on(final String expr)`
- **Summary:** Creates an {@link On} clause from a raw SQL expression string for JOIN operations.
- **Parameters:**
  - `expr` (`String`) — the join condition as a string (must not be {@code null} , empty, or blank)
- **Returns:** an {@link On} clause
- **Signature:** `public static On on(final String leftPropName, final String rightPropName)`
- **Summary:** Creates an {@link On} clause for simple equality join between two columns.
- **Parameters:**
  - `leftPropName` (`String`) — the first column name
  - `rightPropName` (`String`) — the second column name to join with
- **Returns:** an {@link On} clause
- **Signature:** `public static On on(final Map<String, String> propNamePairs)`
- **Summary:** Creates an {@link On} clause from a map of column pairs for JOIN operations.
- **Parameters:**
  - `propNamePairs` (`Map<String, String>`) — map of column name pairs for joining (should be a {@link java.util.LinkedHashMap} to preserve order; must not be {@code null} or empty)
- **Returns:** an {@link On} clause
##### using(...) -> Using
- **Signature:** `@Deprecated public static Using using(final String... columnNames)`
- **Summary:** Creates a USING clause for JOIN operations with the specified columns.
- **Contract:**
  - USING is an alternative to ON when joining tables on columns with the same name.
- **Parameters:**
  - `columnNames` (`String[]`) — the column names used for joining
- **Returns:** a {@link Using} clause
- **Signature:** `@Deprecated public static Using using(final Collection<String> columnNames)`
- **Summary:** Creates a USING clause from a collection of column names for JOIN operations.
- **Parameters:**
  - `columnNames` (`Collection<String>`) — collection of column names used for joining
- **Returns:** a {@link Using} clause
##### join(...) -> Join
- **Signature:** `public static Join join(final String joinEntity)`
- **Summary:** Creates a {@link Join} clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to join
- **Returns:** a {@link Join} clause
- **Signature:** `public static Join join(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a {@link Join} clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** a {@link Join} clause
- **Signature:** `public static Join join(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a {@link Join} clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** a {@link Join} clause
##### leftJoin(...) -> LeftJoin
- **Signature:** `public static LeftJoin leftJoin(final String joinEntity)`
- **Summary:** Creates a LEFT JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to left join
- **Returns:** a {@link LeftJoin} clause
- **Signature:** `public static LeftJoin leftJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a LEFT JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to left join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** a {@link LeftJoin} clause
- **Signature:** `public static LeftJoin leftJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a LEFT JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to left join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** a {@link LeftJoin} clause
##### rightJoin(...) -> RightJoin
- **Signature:** `public static RightJoin rightJoin(final String joinEntity)`
- **Summary:** Creates a RIGHT JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to right join
- **Returns:** a {@link RightJoin} clause
- **Signature:** `public static RightJoin rightJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a RIGHT JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to right join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** a {@link RightJoin} clause
- **Signature:** `public static RightJoin rightJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a RIGHT JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to right join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** a {@link RightJoin} clause
##### crossJoin(...) -> CrossJoin
- **Signature:** `public static CrossJoin crossJoin(final String joinEntity)`
- **Summary:** Creates a CROSS JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to cross join
- **Returns:** a {@link CrossJoin} clause
- **Signature:** `public static CrossJoin crossJoin(final Collection<String> joinEntities)`
- **Summary:** Creates a conditionless CROSS JOIN for multiple entities.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the entity/table names to cross join
- **Returns:** a {@link CrossJoin} clause
##### fullJoin(...) -> FullJoin
- **Signature:** `public static FullJoin fullJoin(final String joinEntity)`
- **Summary:** Creates a FULL JOIN clause for the specified entity/table.
- **Contract:**
  - Returns all records when there is a match in either table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to full join
- **Returns:** a {@link FullJoin} clause
- **Signature:** `public static FullJoin fullJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a FULL JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to full join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** a {@link FullJoin} clause
- **Signature:** `public static FullJoin fullJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a FULL JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to full join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** a {@link FullJoin} clause
##### innerJoin(...) -> InnerJoin
- **Signature:** `public static InnerJoin innerJoin(final String joinEntity)`
- **Summary:** Creates an INNER JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to inner join
- **Returns:** an {@link InnerJoin} clause
- **Signature:** `public static InnerJoin innerJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates an INNER JOIN clause with the specified entity and join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to inner join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** an {@link InnerJoin} clause
- **Signature:** `public static InnerJoin innerJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates an INNER JOIN clause with multiple entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — collection of entity/table names to inner join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** an {@link InnerJoin} clause
##### naturalJoin(...) -> NaturalJoin
- **Signature:** `public static NaturalJoin naturalJoin(final String joinEntity)`
- **Summary:** Creates a NATURAL JOIN clause for the specified entity/table.
- **Parameters:**
  - `joinEntity` (`String`) — the entity/table name to natural join
- **Returns:** a {@link NaturalJoin} clause
- **Signature:** `public static NaturalJoin naturalJoin(final Collection<String> joinEntities)`
- **Summary:** Creates a conditionless NATURAL JOIN for multiple entities.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the entity/table names to natural join
- **Returns:** a {@link NaturalJoin} clause
##### in(...) -> In
- **Signature:** `public static In in(final String propName, final boolean[] values)`
- **Summary:** Creates an IN condition with an array of boolean values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`boolean[]`) — array of boolean values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final String propName, final char[] values)`
- **Summary:** Creates an IN condition with an array of char values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`char[]`) — array of char values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final String propName, final byte[] values)`
- **Summary:** Creates an IN condition with an array of byte values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`byte[]`) — array of byte values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final String propName, final short[] values)`
- **Summary:** Creates an IN condition with an array of short values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`short[]`) — array of short values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final String propName, final int[] values)`
- **Summary:** Creates an IN condition with an array of integer values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`int[]`) — array of integer values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final String propName, final long[] values)`
- **Summary:** Creates an IN condition with an array of long values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`long[]`) — array of long values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final String propName, final float[] values)`
- **Summary:** Creates an IN condition with an array of float values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`float[]`) — array of float values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final String propName, final double[] values)`
- **Summary:** Creates an IN condition with an array of double values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`double[]`) — array of double values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final String propName, final Object... values)`
- **Summary:** Creates an IN condition with an array of object values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Object[]`) — array of values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final String propName, final Collection<?> values)`
- **Summary:** Creates an IN condition with a collection of values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Collection<?>`) — collection of values
- **Returns:** an {@link In} condition
- **Signature:** `public static In in(final Collection<String> propNames, final Collection<?> valueRows)`
- **Summary:** Creates a row value constructor IN condition.
- **Contract:**
  - The tuple of property values must match one of the supplied value rows.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the property/column names (must not be {@code null} or empty and must not contain {@code null} /blank names)
  - `valueRows` (`Collection<?>`) — collection of value rows; each row must resolve to exactly {@code propNames.size()} values. A row may be a {@link Collection} , {@link Iterable} , object array, {@link Map} or bean
- **Returns:** an {@link In} condition
- **Signature:** `public static InSubQuery in(final String propName, final SubQuery subQuery)`
- **Summary:** Creates an IN condition with a subquery.
- **Contract:**
  - The property value must be in the result set of the subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** an {@link InSubQuery} condition
- **Signature:** `public static InSubQuery in(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates an IN condition with multiple properties and a subquery.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** an {@link InSubQuery} condition
##### notIn(...) -> NotIn
- **Signature:** `public static NotIn notIn(final String propName, final boolean[] values)`
- **Summary:** Creates a NOT IN condition with an array of boolean values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`boolean[]`) — array of boolean values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final String propName, final char[] values)`
- **Summary:** Creates a NOT IN condition with an array of char values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`char[]`) — array of char values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final String propName, final byte[] values)`
- **Summary:** Creates a NOT IN condition with an array of byte values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`byte[]`) — array of byte values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final String propName, final short[] values)`
- **Summary:** Creates a NOT IN condition with an array of short values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`short[]`) — array of short values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final String propName, final int[] values)`
- **Summary:** Creates a NOT IN condition with an array of integer values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`int[]`) — array of integer values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final String propName, final long[] values)`
- **Summary:** Creates a NOT IN condition with an array of long values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`long[]`) — array of long values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final String propName, final float[] values)`
- **Summary:** Creates a NOT IN condition with an array of float values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`float[]`) — array of float values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final String propName, final double[] values)`
- **Summary:** Creates a NOT IN condition with an array of double values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`double[]`) — array of double values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final String propName, final Object... values)`
- **Summary:** Creates a NOT IN condition with an array of object values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Object[]`) — array of values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final String propName, final Collection<?> values)`
- **Summary:** Creates a NOT IN condition with a collection of values.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `values` (`Collection<?>`) — collection of values to exclude
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotIn notIn(final Collection<String> propNames, final Collection<?> valueRows)`
- **Summary:** Creates a row value constructor NOT IN condition.
- **Contract:**
  - The tuple of property values must not match any of the supplied value rows.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the property/column names (must not be {@code null} or empty and must not contain {@code null} /blank names)
  - `valueRows` (`Collection<?>`) — collection of value rows to exclude; each row must resolve to exactly {@code propNames.size()} values. A row may be a {@link Collection} , {@link Iterable} , object array, {@link Map} or bean
- **Returns:** a {@link NotIn} condition
- **Signature:** `public static NotInSubQuery notIn(final String propName, final SubQuery subQuery)`
- **Summary:** Creates a NOT IN condition with a subquery.
- **Contract:**
  - The property value must not be in the result set of the subquery.
- **Parameters:**
  - `propName` (`String`) — the property/column name
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** a {@link NotInSubQuery} condition
- **Signature:** `public static NotInSubQuery notIn(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates a NOT IN condition with multiple properties and a subquery.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property/column names
  - `subQuery` (`SubQuery`) — the subquery to check against
- **Returns:** a {@link NotInSubQuery} condition
##### all(...) -> All
- **Signature:** `public static All all(final SubQuery subQuery)`
- **Summary:** Creates an ALL condition for comparison with all values from a subquery.
- **Contract:**
  - The condition is true if the comparison is true for all values returned by the subquery.
  - // When used as the RHS of a comparison such as gt, the full fragment renders: // salary > ALL (SELECT salary FROM employees WHERE dept = 'IT') } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery
- **Returns:** an {@link All} condition
##### any(...) -> Any
- **Signature:** `public static Any any(final SubQuery subQuery)`
- **Summary:** Creates an ANY condition for comparison with any value from a subquery.
- **Contract:**
  - The condition is true if the comparison is true for at least one value returned by the subquery.
  - // When used as the RHS of a comparison such as lt, the full fragment renders: // price < ANY (SELECT price FROM products WHERE category = 'electronics') } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery
- **Returns:** an {@link Any} condition
##### some(...) -> Some
- **Signature:** `public static Some some(final SubQuery subQuery)`
- **Summary:** Creates a SOME condition for comparison with some values from a subquery.
- **Contract:**
  - // When used as the RHS of a comparison such as le, the full fragment renders: // passing_score <= SOME (SELECT score FROM exams WHERE student_id = 123) } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery
- **Returns:** a {@link Some} condition
##### exists(...) -> Exists
- **Signature:** `public static Exists exists(final SubQuery subQuery)`
- **Summary:** Creates an EXISTS condition to check if a subquery returns any rows.
- **Contract:**
  - Creates an EXISTS condition to check if a subquery returns any rows.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check
- **Returns:** an {@link Exists} condition
##### notExists(...) -> NotExists
- **Signature:** `public static NotExists notExists(final SubQuery subQuery)`
- **Summary:** Creates a NOT EXISTS condition to check if a subquery returns no rows.
- **Contract:**
  - Creates a NOT EXISTS condition to check if a subquery returns no rows.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check
- **Returns:** a {@link NotExists} condition
##### union(...) -> Union
- **Signature:** `public static Union union(final SubQuery subQuery)`
- **Summary:** Creates a UNION clause to combine results from a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with
- **Returns:** a {@link Union} clause
##### unionAll(...) -> UnionAll
- **Signature:** `public static UnionAll unionAll(final SubQuery subQuery)`
- **Summary:** Creates a UNION ALL clause to combine results from a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with
- **Returns:** a {@link UnionAll} clause
##### except(...) -> Except
- **Signature:** `public static Except except(final SubQuery subQuery)`
- **Summary:** Creates an EXCEPT clause to subtract results from a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to subtract
- **Returns:** an {@link Except} clause
##### intersect(...) -> Intersect
- **Signature:** `public static Intersect intersect(final SubQuery subQuery)`
- **Summary:** Creates an INTERSECT clause to find common results with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to intersect with
- **Returns:** an {@link Intersect} clause
##### minus(...) -> Minus
- **Signature:** `public static Minus minus(final SubQuery subQuery)`
- **Summary:** Creates a MINUS clause to subtract results from a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to subtract
- **Returns:** a {@link Minus} clause
##### subQuery(...) -> SubQuery
- **Signature:** `public static SubQuery subQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a SubQuery from an entity class with selected properties and condition.
- **Contract:**
  - If {@code condition} is not already a {@link com.landawn.abacus.query.condition.Criteria Criteria} or a clause (such as {@link Where} ), it is automatically wrapped in a {@code WHERE} clause at construction time; {@code condition()} on the returned subquery returns the wrapping {@link Where} .
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table (must not be {@code null} )
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be {@code null} or empty, and must not contain {@code null} , empty, or blank elements)
  - `condition` (`Condition`) — the WHERE condition for the subquery; may be {@code null} for no WHERE clause
- **Returns:** a {@link SubQuery}
- **Signature:** `public static SubQuery subQuery(final Class<?> entityClass, final String propName, final Condition condition)`
- **Summary:** Creates a structured single-property subquery for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `propName` (`String`) — the property to select
  - `condition` (`Condition`) — the optional query condition
- **Returns:** a structured subquery
- **Signature:** `public static SubQuery subQuery(final Class<?> entityClass, final Collection<String> propNames, final String expr)`
- **Summary:** Creates a SubQuery from an entity class with selected properties and a raw SQL condition string.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class representing the table (must not be {@code null} )
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be {@code null} or empty, and must not contain {@code null} , empty, or blank elements)
  - `expr` (`String`) — the WHERE condition as a raw SQL string (must not be {@code null} ; may be empty for no filter condition)
- **Returns:** a {@link SubQuery}
- **See also:** #subQuery(String, Collection, String)
- **Signature:** `public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a SubQuery from an entity name with selected properties and condition.
- **Contract:**
  - If {@code condition} is not already a {@link com.landawn.abacus.query.condition.Criteria Criteria} or a clause (such as {@link Where} ), it is automatically wrapped in a {@code WHERE} clause at construction time; {@code condition()} on the returned subquery returns the wrapping {@link Where} .
- **Parameters:**
  - `entityName` (`String`) — the entity/table name (must not be {@code null} , empty, or blank)
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be {@code null} or empty, and must not contain {@code null} , empty, or blank elements)
  - `condition` (`Condition`) — the WHERE condition for the subquery; may be {@code null} for no WHERE clause
- **Returns:** a {@link SubQuery}
- **Signature:** `public static SubQuery subQuery(final String entityName, final String propName, final Condition condition)`
- **Summary:** Creates a structured single-property subquery for an entity name.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name
  - `propName` (`String`) — the property to select
  - `condition` (`Condition`) — the optional query condition
- **Returns:** a structured subquery
- **Signature:** `public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final String expr)`
- **Summary:** Creates a SubQuery from an entity name with selected properties and a raw SQL condition string.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name (must not be {@code null} , empty, or blank)
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be {@code null} or empty, and must not contain {@code null} , empty, or blank elements)
  - `expr` (`String`) — the WHERE condition as a raw SQL string (must not be {@code null} ; may be empty for no filter condition)
- **Returns:** a {@link SubQuery}
- **Signature:** `@Deprecated public static SubQuery subQuery(final String entityName, final String sql)`
- **Summary:** Creates a SubQuery from an entity name and raw SQL.
- **Contract:**
  - (entityName is ignored when full SQL is supplied) } </pre>
- **Parameters:**
  - `entityName` (`String`) — the entity/table name
  - `sql` (`String`) — the complete SQL for the subquery (must not be {@code null} , empty, or blank)
- **Returns:** a {@link SubQuery}
- **See also:** #subQuery(String)
- **Signature:** `public static SubQuery subQuery(final String sql)`
- **Summary:** Creates a SubQuery from raw SQL.
- **Parameters:**
  - `sql` (`String`) — the complete SQL for the subquery (must not be {@code null} , empty, or blank)
- **Returns:** a {@link SubQuery}
##### limit(...) -> Limit
- **Signature:** `public static Limit limit(final int count)`
- **Summary:** Creates a {@link Limit} clause to restrict the number of rows returned.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must be non-negative)
- **Returns:** a {@link Limit} clause
- **Signature:** `public static Limit limit(final int count, final int offset)`
- **Summary:** Creates a {@link Limit} clause with a count and offset.
- **Contract:**
  - When {@code offset == 0} , the rendered SQL omits the {@code OFFSET} clause.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return (must be non-negative)
  - `offset` (`int`) — the number of rows to skip (must be non-negative)
- **Returns:** a {@link Limit} clause
- **Signature:** `public static Limit limit(final String expr)`
- **Summary:** Creates a LIMIT clause from a string expression, formatting and validating it against a fixed grammar.
- **Contract:**
  - The expression is trimmed, its internal whitespace collapsed, and its SQL keywords upper-cased (parameter names left intact); a {@code "LIMIT "} prefix is prepended when it begins with a digit, {@code '?'} , {@code ':'} , or <code> "#{" </code> .
  - It must be one of {@code LIMIT count} , {@code LIMIT count OFFSET offset} , MySQL's {@code LIMIT offset, count} , or the SQL:2008 {@code \[OFFSET offset ROWS\] FETCH NEXT/FIRST count ROWS ONLY} forms \\u2014 where each number is an integer or a {@code ?} / {@code :name} / <code> #{name} </code> placeholder \\u2014 otherwise an {@link IllegalArgumentException} is thrown.
  - <p> When the condition is rendered by a SQL builder, a parsed expression is emitted in the target dialect's pagination syntax (so MySQL's comma form and the {@code FETCH} forms are re-rendered per dialect).
  - An opaque (placeholder) expression is re-rendered in the dialect's {@code FETCH} syntax only when the dialect paginates with {@code OFFSET} / {@code FETCH} (Oracle, DB2 or SQL Server, per {@link SqlDialect.ProductInfo} ) and it is a generic {@code LIMIT count \[OFFSET offset\]} form; otherwise it is emitted verbatim.
- **Parameters:**
  - `expr` (`String`) — the limit expression as a string (must not be {@code null} , empty, or blank, and must match one of the accepted forms)
- **Returns:** a {@link Limit} clause

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
- **Summary:** Parses the given SQL string and returns a {@code ParsedSql} instance.
- **Contract:**
  - </p> <p> Parameter conversion is only applied when the SQL is a recognized data operation statement (see the class-level documentation).
- **Parameters:**
  - `sql` (`String`) — the SQL string to parse (must not be {@code null} , empty, or blank)
- **Returns:** a {@code ParsedSql} instance containing the parsed information

#### Public Instance Methods
##### originalSql(...) -> String
- **Signature:** `public String originalSql()`
- **Summary:** Returns the original SQL string (trimmed of leading and trailing whitespace), before any parameter conversion or processing.
- **Parameters:**
  - (none)
- **Returns:** the trimmed original SQL string
##### parameterizedSql(...) -> String
- **Signature:** `public String parameterizedSql()`
- **Summary:** Gets the parameterized SQL with named parameters replaced by JDBC placeholders ( {@code ?} ) for recognized data-operation statements.
- **Parameters:**
  - (none)
- **Returns:** the parameterized SQL string with {@code ?} placeholders
##### namedParameters(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> namedParameters()`
- **Summary:** Gets the list of named parameters extracted from the SQL in order of appearance.
- **Contract:**
  - Returns an empty list if the SQL has no named parameters, or if the SQL is not a recognized data operation statement (see the class-level documentation), in which case no parameter extraction is performed.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter names
##### parameterCount(...) -> int
- **Signature:** `public int parameterCount()`
- **Summary:** Gets the total number of parameters (named or positional) in the SQL.
- **Parameters:**
  - (none)
- **Returns:** the number of parameters in the SQL
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code value for this {@code ParsedSql} .
- **Parameters:**
  - (none)
- **Returns:** the hash code value
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Indicates whether some other object is "equal to" this one.
- **Contract:**
  - Two {@code ParsedSql} objects are equal if their trimmed original SQL strings (as returned by {@link #originalSql()} ) are equal.
- **Parameters:**
  - `obj` (`Object`) — the reference object with which to compare
- **Returns:** {@code true} if this object equals the obj argument; {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this {@code ParsedSql} .
- **Parameters:**
  - (none)
- **Returns:** a string representation of this object

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
  - The {@link ColumnInfo#isUnqualified()} flag describes the lookup key, not the mapped column value: it is {@code true} when the key contains no {@code '.'} character.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be {@code null} )
  - `namingPolicy` (`NamingPolicy`) — the naming policy to use for column name conversion. If {@code null} , defaults to {@code NamingPolicy.SNAKE_CASE} .
- **Returns:** an immutable map containing property-name keys and, when a mapped column name is not already a property-name key, an additional column-name key. Each value contains the mapped column name and whether its corresponding lookup key has no dot.
- **See also:** #propertyToColumnMap(Class, NamingPolicy)
##### columnToPropertyMap(...) -> ImmutableMap<String, String>
- **Signature:** `@Internal public static ImmutableMap<String, String> columnToPropertyMap(final Class<?> entityClass)`
- **Summary:** Returns a mapping of column names to property names for the specified entity class.
- **Contract:**
  - <p> This method is useful when you need to map database result set columns back to entity properties, especially when dealing with case-insensitive database systems or when column names don't match the exact case in your code.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Given an entity class with @Column annotations ImmutableMap<String, String> columnToProp = QueryUtil.columnToPropertyMap(User.class); // If User has @Column("User_Name") on property "userName": String propName = columnToProp.get("User_Name"); // "userName" (looked up by original column name) String propName2 = columnToProp.get("USER_NAME"); // "userName" (looked up by uppercase variant) String propName3 = columnToProp.get("user_name"); // "userName" (looked up by lowercase variant) } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be {@code null} )
- **Returns:** an immutable map of column names (including upper- and lower-case variations) to property names
##### propertyToColumnMap(...) -> ImmutableMap<String, String>
- **Signature:** `@Internal public static ImmutableMap<String, String> propertyToColumnMap(final Class<?> entityClass, final NamingPolicy namingPolicy)`
- **Summary:** Returns a mapping of property names to column names for the specified entity class using the given naming policy.
- **Contract:**
  - <p> The naming policy determines how property names are converted to column names when no explicit {@code @Column} annotation is present.
  - {@code "addr.street"} when the {@code Address} entity declares an alias {@code "addr"} ).
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Build property-to-column mapping with SNAKE_CASE naming policy ImmutableMap<String, String> propToColumn = QueryUtil.propertyToColumnMap(User.class, NamingPolicy.SNAKE_CASE); // If User has property "firstName" without @Column annotation: String columnName = propToColumn.get("firstName"); // "first_name" // With SCREAMING_SNAKE_CASE naming policy ImmutableMap<String, String> propToColumnUpper = QueryUtil.propertyToColumnMap(User.class, NamingPolicy.SCREAMING_SNAKE_CASE); String upperColumn = propToColumnUpper.get("firstName"); // "FIRST_NAME" } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (may be {@code null} )
  - `namingPolicy` (`NamingPolicy`) — the naming policy to use for column name conversion. If {@code null} , defaults to {@code NamingPolicy.SNAKE_CASE} .
- **Returns:** an immutable map of property names to column names, or an empty immutable map if {@code entityClass} is {@code null} or is a {@link Map} type
##### insertPropertyNames(...) -> ImmutableList<String>
- **Signature:** `@Internal public static ImmutableList<String> insertPropertyNames(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for INSERT operations on the given entity instance.
- **Contract:**
  - <p> The method intelligently handles ID fields: </p> <ul> <li> If all ID fields have default values (e.g.
  - </li> <li> If any ID field has a non-default value, all insertable properties including IDs are returned.
- **Parameters:**
  - `entity` (`Object`) — the entity instance to analyze (must not be {@code null} )
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for INSERT operations
- **Signature:** `@Internal public static ImmutableList<String> insertPropertyNames(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for INSERT operations on the given entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be {@code null} )
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for INSERT operations
##### selectPropertyNames(...) -> ImmutableList<String>
- **Signature:** `@Internal public static ImmutableList<String> selectPropertyNames(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for SELECT operations on the given entity class.
- **Contract:**
  - <p> When {@code includeSubEntityProperties} is {@code true} , the method returns nested properties using dot notation (e.g., {@code "address.street"} ).
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be {@code null} )
  - `includeSubEntityProperties` (`boolean`) — {@code true} to include nested entity properties, {@code false} for top-level only
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude (nullable). When sub-entity properties are included, excluding a root such as {@code "address"} also excludes descendants such as {@code "address.street"} .
- **Returns:** an immutable list of property names suitable for SELECT operations
- **Signature:** `@Internal public static ImmutableList<String> selectPropertyNames(final Object entity, final boolean includeSubEntityProperties, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for SELECT operations on the given entity instance.
- **Parameters:**
  - `entity` (`Object`) — the entity instance to analyze (must not be {@code null} )
  - `includeSubEntityProperties` (`boolean`) — {@code true} to include nested entity properties, {@code false} for top-level only
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for SELECT operations
- **See also:** #selectPropertyNames(Class, boolean, Set)
##### updatePropertyNames(...) -> ImmutableList<String>
- **Signature:** `@Internal public static ImmutableList<String> updatePropertyNames(final Class<?> entityClass, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for UPDATE operations on the given entity class.
- **Contract:**
  - <p> Properties are considered non-updatable if they are: </p> <ul> <li> Annotated with {@code @ReadOnly} , {@code @ReadOnlyId} , or otherwise marked as a read-only id property </li> <li> Annotated with {@code @NonUpdatable} </li> <li> Excluded from column mapping (e.g.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be {@code null} )
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for UPDATE operations
- **Signature:** `@Internal public static ImmutableList<String> updatePropertyNames(final Object entity, final Set<String> excludedPropNames)`
- **Summary:** Returns the property names to be used for UPDATE operations on the given entity instance.
- **Parameters:**
  - `entity` (`Object`) — the entity instance to analyze (must not be {@code null} )
  - `excludedPropNames` (`Set<String>`) — set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions)
- **Returns:** an immutable list of property names suitable for UPDATE operations
- **See also:** #updatePropertyNames(Class, Set)
##### idPropertyNames(...) -> ImmutableList<String>
- **Signature:** `@Internal @Immutable public static ImmutableList<String> idPropertyNames(final Class<?> entityClass)`
- **Summary:** Returns the ID property names for the specified entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be {@code null} )
- **Returns:** an immutable list of ID property names, or an empty list if no ID properties are defined
##### isNonColumn(...) -> boolean
- **Signature:** `@Internal public static boolean isNonColumn(final Set<String> columnFields, final Set<String> nonColumnFields, final PropInfo propInfo)`
- **Summary:** Determines whether a property should be excluded from database column mapping.
- **Contract:**
  - Determines whether a property should be excluded from database column mapping.
  - A property is not a column if it's {@code transient} , annotated with {@code @NonColumn} , or excluded by {@code @Table} configuration.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code BeanInfo beanInfo = ParserUtil.getBeanInfo(User.class); PropInfo propInfo = beanInfo.getPropInfo("tempField"); // Check if a property is excluded from column mapping Set<String> columnFields = N.asSet("id", "name", "email"); Set<String> nonColumnFields = N.emptySet(); boolean excluded = QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo); // Returns true if "tempField" is not in columnFields // Check with nonColumnFields Set<String> nonColumns = N.asSet("tempField", "transientData"); boolean excluded2 = QueryUtil.isNonColumn(N.emptySet(), nonColumns, propInfo); // Returns true if "tempField" is in nonColumnFields } </pre>
- **Parameters:**
  - `columnFields` (`Set<String>`) — set of field names explicitly included as columns (typically derived from {@link Table#columnFields()} ; may be {@code null} or empty for no whitelist)
  - `nonColumnFields` (`Set<String>`) — set of field names explicitly excluded as columns (typically derived from {@link Table#nonColumnFields()} ; may be {@code null} or empty for no blacklist)
  - `propInfo` (`PropInfo`) — the property information to check (must not be {@code null} )
- **Returns:** {@code true} if the property should not be mapped to a database column
##### placeholders(...) -> String
- **Signature:** `public static String placeholders(final int placeholderCount)`
- **Summary:** Generates a string of question marks ( {@code ?} ) repeated {@code placeholderCount} times with comma-space delimiter ( {@code ", "} ).
- **Parameters:**
  - `placeholderCount` (`int`) — the number of question marks to generate (must not be negative)
- **Returns:** a string containing {@code placeholderCount} question marks separated by {@code ", "} , or empty string if {@code placeholderCount} is 0
##### tableAlias(...) -> String
- **Signature:** `@Internal public static String tableAlias(final Class<?> entityClass)`
- **Summary:** Returns the table alias from the {@code @Table} annotation on the entity class.
- **Contract:**
  - <p> If no {@code @Table} annotation exists, this method returns {@code null} .
  - If {@code @Table} is present but the alias attribute is not specified, this method returns an empty string.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Given: @Table(name = "users", alias = "u") on User class String alias = QueryUtil.tableAlias(User.class); // Returns: "u" // Given: @Table(name = "orders") on Order class (no alias) String alias2 = QueryUtil.tableAlias(Order.class); // Returns: "" (empty string when alias is not specified) // Given: no @Table annotation on LogEntry class String alias3 = QueryUtil.tableAlias(LogEntry.class); // Returns: null } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to check (must not be {@code null} )
- **Returns:** the table alias if defined in {@code @Table} annotation, empty string if {@code @Table} is present but alias is not set, or {@code null} if no {@code @Table} annotation exists
##### tableNameAndAlias(...) -> String
- **Signature:** `@Internal public static String tableNameAndAlias(final Class<?> entityClass)`
- **Summary:** Returns the table name and optional alias for the entity class using the default naming policy.
- **Contract:**
  - If {@code @Table} annotation is present, uses its values; otherwise derives the table name from the class name using {@code NamingPolicy.SNAKE_CASE} .
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be {@code null} )
- **Returns:** the table name, optionally followed by space and alias
- **Signature:** `@Internal public static String tableNameAndAlias(final Class<?> entityClass, final NamingPolicy namingPolicy)`
- **Summary:** Returns the table name and optional alias for the entity class using the specified naming policy.
- **Contract:**
  - The table name is resolved the same way the query builders resolve it: from the {@code @Table} annotation ( {@link Table#name() name} or its deprecated {@code value()} alias) or a JPA {@code javax.persistence} / {@code jakarta.persistence} {@code @Table} annotation; only when no annotated name exists is the table name derived from the class name using the provided naming policy.
  - The alias comes from {@link Table#alias()} and is appended after a space when present.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to analyze (must not be {@code null} )
  - `namingPolicy` (`NamingPolicy`) — the naming policy used when no annotated table name exists. If {@code null} , defaults to {@code NamingPolicy.SNAKE_CASE} .
- **Returns:** the table name, optionally followed by space and alias

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
##### <init>(...) -> void
- **Signature:** `record ColumnInfo(String columnName, boolean isUnqualified) { } /** * Regular expression pattern for validating simple column names. * To match, a column name must be non-empty and consist solely of letters, digits, underscores, or hyphens. * * <p><b>Usage Examples:</b></p> * <pre>{@code * boolean isValid = SIMPLE_COLUMN_NAME_PATTERN.matcher("column_name").matches(); * }</pre> */ public static final Pattern SIMPLE_COLUMN_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$"); private QueryUtil() { // utility class — no instances } @SuppressWarnings("deprecation") static final int POOL_SIZE = InternalUtil.POOL_SIZE; private static final int DEFAULT_MAX_NESTED_PROP_DEPTH = 2; private static final int MAX_NESTED_PROP_DEPTH = Math.max(0, Integer.getInteger("abacus.query.maxNestedPropDepth", DEFAULT_MAX_NESTED_PROP_DEPTH)); private static final String ENTITY_CLASS = "entityClass"; private static final Map<Class<?>, ImmutableMap<String, String>> column2PropNameMapPool = new ConcurrentHashMap<>(); private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, String>>> entityTablePropColumnNameMap = new ObjectPool<>(POOL_SIZE); private static final Map<Class<?>, Map<NamingPolicy, ImmutableMap<String, ColumnInfo>>> entityPropColumnInfoMap = new ObjectPool<>(POOL_SIZE); /** * Caches, per entity class, the {@link PropInfo} objects resolved for that class's ID * property names (in {@link #idPropertyNames(Class)} order). This avoids repeating the * reflective {@code BeanInfo.getPropInfo(name)} lookups on every insert-prop-name call. * A cached entry may contain {@code null} elements, mirroring the original per-call * behavior when an id name does not resolve to a {@link PropInfo}. */ private static final Map<Class<?>, PropInfo[]> idPropInfosPool = new ConcurrentHashMap<>(); /** * Caches, per entity class, the immutable {@code ImmutableList} returned for the * no-exclusion ({@code excludedPropNames} null/empty) paths of the {@code get*PropNames} * methods. Index by the column slot of {@code SqlBuilder.loadPropNamesByClass(Class)} * ({@code 0}: select incl. sub-entities, {@code 1}: select top-level only, {@code 2}: insert with id, * {@code 3}: insert without id, {@code 4}: update). Returning a single stable instance per (class, slot) preserves the * reference-identity fast paths in the builders (which compare the stored prop-name list with * {@code ==} against a fresh no-exclusion call) while still handing back an immutable list. */ private static final Map<Class<?>, AtomicReferenceArray<ImmutableList<String>>> noExclusionPropNamesPool = new ConcurrentHashMap<>(); /** * Returns the cached immutable, no-exclusion property-name list for the given entity class and * {@code SqlBuilder.loadPropNamesByClass(Class)} slot, building (and memoizing) it on first use. * The cached list is a defensive immutable copy of the corresponding cached prop-name set, so a * single stable instance is returned for every no-exclusion call. * * @param entityClass the entity class whose prop-name list is requested * @param slot the {@code loadPropNamesByClass} array index (0, 1, 2, 3, or 4) * @return the memoized immutable list for that (class, slot) */ private static ImmutableList<String> getNoExclusionPropNames(final Class<?> entityClass, final int slot) { final AtomicReferenceArray<ImmutableList<String>> cache = noExclusionPropNamesPool.computeIfAbsent(entityClass, cls -> new AtomicReferenceArray<>(5)); ImmutableList<String> result = cache.get(slot); if (result == null) { final ImmutableList<String> candidate = ImmutableList.copyOf(SqlBuilder.loadPropNamesByClass(entityClass)[slot]); // Publish the first completed immutable snapshot atomically. A plain array element read // outside the writer's synchronized block has no happens-before edge and can expose a // stale/null reference; AtomicReferenceArray provides both safe publication and the // stable reference identity required by builder fast paths. if (cache.compareAndSet(slot, null, candidate)) { result = candidate; } else { result = cache.get(slot); } } return result; } /** * Returns column information keyed by both property names and mapped column names. * The {@link ColumnInfo#isUnqualified()} flag describes the lookup key, not the mapped * column value: it is {@code true} when the key contains no {@code '.'} character. * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Get property-to-column details * ImmutableMap<String, QueryUtil.ColumnInfo> columnInfoMap = * QueryUtil.propToColumnInfoMap(User.class, NamingPolicy.SNAKE_CASE); * * QueryUtil.ColumnInfo result = columnInfoMap.get("firstName"); * String columnName = result.columnName(); // "first_name" * boolean unqualified = result.isUnqualified(); // true * * // Nested property example * QueryUtil.ColumnInfo nested = columnInfoMap.get("address.street"); * String nestedColumn = nested.columnName(); // e.g. "addr.street" * boolean nestedUnqualified = nested.isUnqualified(); // false * }</pre> * * <p><b>Note:</b> despite the similar name, this is a different method from * {@link #propertyToColumnMap(Class, NamingPolicy)}, which returns a plain * property-name-to-column-name map ({@code ImmutableMap<String, String>}) and is the one * intended for general use.</p> * * @param entityClass the entity class to analyze (must not be {@code null}) * @param namingPolicy the naming policy to use for column name conversion. If {@code null}, defaults to {@code NamingPolicy.SNAKE_CASE}. * @return an immutable map containing property-name keys and, when a mapped column name is not * already a property-name key, an additional column-name key. Each value contains the * mapped column name and whether its corresponding lookup key has no dot. * @throws IllegalArgumentException if {@code entityClass} is {@code null} * @see #propertyToColumnMap(Class, NamingPolicy) */ @Beta @Internal public static ImmutableMap<String, ColumnInfo> propToColumnInfoMap(final Class<?> entityClass, final NamingPolicy namingPolicy) { N.checkArgNotNull(entityClass, ENTITY_CLASS); final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy; Map<NamingPolicy, ImmutableMap<String, ColumnInfo>> namingPropColumnInfoMap = entityPropColumnInfoMap.get(entityClass); ImmutableMap<String, ColumnInfo> result = null; if (namingPropColumnInfoMap == null || (result = namingPropColumnInfoMap.get(effectiveNamingPolicy)) == null) { final ImmutableMap<String, String> propertyToColumnMap = propertyToColumnMap(entityClass, effectiveNamingPolicy); final Map<String, ColumnInfo> newPropColumnInfoMap = N.newHashMap(propertyToColumnMap.size() * 2); for (final Map.Entry<String, String> entry : propertyToColumnMap.entrySet()) { newPropColumnInfoMap.put(entry.getKey(), new ColumnInfo(entry.getValue(), entry.getKey().indexOf('.') < 0)); if (!propertyToColumnMap.containsKey(entry.getValue())) { newPropColumnInfoMap.put(entry.getValue(), new ColumnInfo(entry.getValue(), entry.getValue().indexOf('.') < 0)); } } result = ImmutableMap.wrap(newPropColumnInfoMap); if (namingPropColumnInfoMap == null) { final Map<NamingPolicy, ImmutableMap<String, ColumnInfo>> newMap = Collections.synchronizedMap(new EnumMap<>(NamingPolicy.class)); final Map<NamingPolicy, ImmutableMap<String, ColumnInfo>> existingMap = entityPropColumnInfoMap.putIfAbsent(entityClass, newMap); namingPropColumnInfoMap = existingMap == null ? newMap : existingMap; } namingPropColumnInfoMap.put(effectiveNamingPolicy, result); } return result; } /** * Returns a mapping of column names to property names for the specified entity class. * The map includes variations of column names in lowercase and uppercase for case-insensitive lookups. * An explicitly declared column spelling takes precedence over a generated case-folded variation, * so distinct quoted columns such as {@code code} and {@code CODE} retain their exact mappings. * Only properties whose effective metadata exposes a column name (i.e. * {@link PropInfo#columnName} is present) are included. Metadata resolution also honors * {@link NonColumn} and {@link Table#columnFields()}/{@link Table#nonColumnFields()}, so an * otherwise annotated property excluded by the table mapping is not added. * * <p>This method is useful when you need to map database result set columns back to entity properties, * especially when dealing with case-insensitive database systems or when column names don't match * the exact case in your code.</p> * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Given an entity class with @Column annotations * ImmutableMap<String, String> columnToProp = QueryUtil.columnToPropertyMap(User.class); * * // If User has @Column("User_Name") on property "userName": * String propName = columnToProp.get("User_Name"); // "userName" (looked up by original column name) * String propName2 = columnToProp.get("USER_NAME"); // "userName" (looked up by uppercase variant) * String propName3 = columnToProp.get("user_name"); // "userName" (looked up by lowercase variant) * }</pre> * * @param entityClass the entity class to analyze (must not be {@code null}) * @return an immutable map of column names (including upper- and lower-case variations) to property names * @throws IllegalArgumentException if {@code entityClass} is {@code null} */ @Internal public static ImmutableMap<String, String> columnToPropertyMap(final Class<?> entityClass) { N.checkArgNotNull(entityClass, ENTITY_CLASS); // Backing map is a ConcurrentHashMap, so computeIfAbsent runs the (expensive) // metadata build at most once per class even under concurrent first-touch. return column2PropNameMapPool.computeIfAbsent(entityClass, cls -> { final BeanInfo entityInfo = ParserUtil.getBeanInfo(cls); final Map<String, String> map = N.newHashMap(entityInfo.propInfoList.size() * 3); // Register every exact spelling first. If two quoted identifiers differ only by case, // a folded alias from one must never overwrite the other's exact mapping. for (final PropInfo propInfo : entityInfo.propInfoList) { if (propInfo.columnName.isPresent()) { final String columnName = propInfo.columnName.get(); map.put(columnName, propInfo.name); } } // Add case-insensitive convenience keys only where no exact spelling (or earlier folded // spelling) is already registered. Ambiguous folded lookups are inherently lossy, but // exact lookups remain deterministic and correct. for (final PropInfo propInfo : entityInfo.propInfoList) { if (propInfo.columnName.isPresent()) { final String columnName = propInfo.columnName.get(); map.putIfAbsent(columnName.toLowerCase(Locale.ROOT), propInfo.name); map.putIfAbsent(columnName.toUpperCase(Locale.ROOT), propInfo.name); } } return ImmutableMap.copyOf(map); }); } /** * Returns a mapping of property names to column names for the specified entity class using the given naming policy. * This method handles nested properties and respects {@code @Table} annotations for column field configurations. * * <p>The naming policy determines how property names are converted to column names when no explicit * {@code @Column} annotation is present. For nested bean properties, the method recursively builds mappings * with dot notation up to {@code abacus.query.maxNestedPropDepth} bean hops (default: 2): a nested property * like {@code "address.street"} resolves to a value of the form {@code "<sub-table-alias-or-name>.<column>"} * (e.g. {@code "addr.street"} when the {@code Address} entity declares an alias {@code "addr"}). * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Build property-to-column mapping with SNAKE_CASE naming policy * ImmutableMap<String, String> propToColumn = QueryUtil.propertyToColumnMap(User.class, NamingPolicy.SNAKE_CASE); * * // If User has property "firstName" without @Column annotation: * String columnName = propToColumn.get("firstName"); // "first_name" * * // With SCREAMING_SNAKE_CASE naming policy * ImmutableMap<String, String> propToColumnUpper = * QueryUtil.propertyToColumnMap(User.class, NamingPolicy.SCREAMING_SNAKE_CASE); * String upperColumn = propToColumnUpper.get("firstName"); // "FIRST_NAME" * }</pre> * * @param entityClass the entity class to analyze (may be {@code null}) * @param namingPolicy the naming policy to use for column name conversion. If {@code null}, defaults to {@code NamingPolicy.SNAKE_CASE}. * @return an immutable map of property names to column names, or an empty immutable map if {@code entityClass} is {@code null} or is a {@link Map} type */ @Internal public static ImmutableMap<String, String> propertyToColumnMap(final Class<?> entityClass, final NamingPolicy namingPolicy) { final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy; if (entityClass == null || Map.class.isAssignableFrom(entityClass)) { return ImmutableMap.empty(); } final Map<NamingPolicy, ImmutableMap<String, String>> namingColumnNameMap = entityTablePropColumnNameMap.get(entityClass); ImmutableMap<String, String> result = null; if (namingColumnNameMap == null || (result = namingColumnNameMap.get(effectiveNamingPolicy)) == null) { result = registerEntityPropColumnNameMap(entityClass, effectiveNamingPolicy, null); } return result; } static ImmutableMap<String, String> registerEntityPropColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy, final Set<Class<?>> registeringClasses) { return registerEntityPropColumnNameMap(entityClass, namingPolicy, registeringClasses, MAX_NESTED_PROP_DEPTH); } private static ImmutableMap<String, String> registerEntityPropColumnNameMap(final Class<?> entityClass, final NamingPolicy namingPolicy, final Set<Class<?>> registeringClasses, final int remainingNestedPropDepth) { N.checkArgNotNull(entityClass, ENTITY_CLASS); if (registeringClasses != null) { if (registeringClasses.contains(entityClass)) { // Stop expanding nested bean paths once a cycle is detected so // self-referential entities remain queryable instead of failing fast. return ImmutableMap.empty(); } registeringClasses.add(entityClass); } final Table tableAnno = entityClass.getAnnotation(Table.class); final Set<String> columnFields; final Set<String> nonColumnFields; if (tableAnno == null) { columnFields = N.emptySet(); nonColumnFields = N.emptySet(); } else { // @Table accessors clone their array on each call and N.toSet allocates a HashSet even // for an empty array; capture each array once and reuse the shared empty set when the // field list is absent (the common case for tables that only declare name/alias). final String[] columnFieldArray = tableAnno.columnFields(); columnFields = columnFieldArray.length == 0 ? N.emptySet() : N.toSet(columnFieldArray); final String[] nonColumnFieldArray = tableAnno.nonColumnFields(); nonColumnFields = nonColumnFieldArray.length == 0 ? N.emptySet() : N.toSet(nonColumnFieldArray); } final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass); Map<String, String> propColumnNameMap = N.newHashMap(entityInfo.propInfoList.size() * 2); for (final PropInfo propInfo : entityInfo.propInfoList) { if (isNonColumn(columnFields, nonColumnFields, propInfo)) { continue; } if (propInfo.columnName.isPresent()) { propColumnNameMap.put(propInfo.name, propInfo.columnName.get()); } else { final Type<?> propType = propInfo.type.isCollection() ? propInfo.type.elementType() : propInfo.type; if (propType.isBean()) { if (remainingNestedPropDepth <= 0 || (registeringClasses != null && registeringClasses.contains(propType.javaType()))) { continue; } final Set<Class<?>> newRegisteringClasses = registeringClasses == null ? N.newLinkedHashSet() : new LinkedHashSet<>(registeringClasses); newRegisteringClasses.add(entityClass); final Map<String, String> subPropColumnNameMap = registerEntityPropColumnNameMap(propType.javaType(), namingPolicy, newRegisteringClasses, remainingNestedPropDepth - 1); if (N.notEmpty(subPropColumnNameMap)) { final String subTableAliasOrName = SqlBuilder.tableAliasOrName(propType.javaType(), namingPolicy); for (final Map.Entry<String, String> entry : subPropColumnNameMap.entrySet()) { propColumnNameMap.put(propInfo.name + SK.PERIOD + entry.getKey(), subTableAliasOrName + SK.PERIOD + entry.getValue()); } } } else { propColumnNameMap.put(propInfo.name, SqlBuilder.normalizeColumnName(propInfo.name, namingPolicy)); } } } if (N.isEmpty(propColumnNameMap)) { propColumnNameMap = N.emptyMap(); } final ImmutableMap<String, String> result = ImmutableMap.wrap(propColumnNameMap); if (registeringClasses != null) { return result; } Map<NamingPolicy, ImmutableMap<String, String>> namingPropColumnMap = entityTablePropColumnNameMap.get(entityClass); if (namingPropColumnMap == null) { final Map<NamingPolicy, ImmutableMap<String, String>> newMap = Collections.synchronizedMap(new EnumMap<>(NamingPolicy.class)); final Map<NamingPolicy, ImmutableMap<String, String>> existingMap = entityTablePropColumnNameMap.putIfAbsent(entityClass, newMap); namingPropColumnMap = existingMap == null ? newMap : existingMap; } namingPropColumnMap.put(namingPolicy, result); return result; } /** * Returns the property names to be used for INSERT operations on the given entity instance. * This method considers ID fields and excludes properties marked as non-insertable. * * <p>The method intelligently handles ID fields:</p> * <ul> * <li>If all ID fields have default values (e.g. {@code 0} for primitive numeric types, * {@code null} for reference types) as determined by {@link SqlBuilder#isDefaultIdPropValue(Object)}, * they are excluded from the result.</li> * <li>If any ID field has a non-default value, all insertable properties including IDs are returned.</li> * <li>This allows both auto-generated IDs and manually-assigned IDs to work correctly.</li> * </ul> * * <p><b>Usage Examples:</b></p> * <pre>{@code * User user = new User(); * user.setName("John"); * user.setEmail("john@example.com"); * // user.id is at its default value (e.g. null for Long, 0 for long primitive) * // so the "id" property will be excluded. * * Collection<String> insertProps = QueryUtil.insertPropertyNames(user, null); * // Returns: ["name", "email", ...] (excludes "id" since it has default value) * * // With excluded properties * Set<String> excluded = N.asSet("email"); * Collection<String> filteredProps = QueryUtil.insertPropertyNames(user, excluded); * // Returns: ["name", ...] (excludes both "id" and "email") * }</pre> * * @param entity the entity instance to analyze (must not be {@code null}) * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions) * @return an immutable list of property names suitable for INSERT operations * @throws IllegalArgumentException if {@code entity} is {@code null} */ @Internal public static ImmutableList<String> insertPropertyNames(final Object entity, final Set<String> excludedPropNames) { N.checkArgNotNull(entity, "entity"); final Class<?> entityClass = entity.getClass(); // Resolve the ID PropInfo array once per class (cached); on subsequent calls this // is just a value read instead of repeated reflective BeanInfo.getPropInfo lookups. // The array mirrors idPropertyNames(entityClass) order and may contain null // elements where an id name does not resolve, exactly as the original per-call code. final PropInfo[] idPropInfos = idPropInfosPool.computeIfAbsent(entityClass, cls -> { final Collection<String> idPropNames = idPropertyNames(cls); final BeanInfo entityInfo = ParserUtil.getBeanInfo(cls); final PropInfo[] resolved = new PropInfo[idPropNames.size()]; int i = 0; for (final String idPropName : idPropNames) { resolved[i++] = entityInfo.getPropInfo(idPropName); } return resolved; }); // Determine the base property names based on whether ID fields have default values. // val[2] includes ID fields, val[3] excludes them. int baseSlot = 2; if (idPropInfos.length > 0) { boolean allDefault = true; for (final PropInfo propInfo : idPropInfos) { if (propInfo != null) { final Object propValue = propInfo.getPropValue(entity); if (!SqlBuilder.isDefaultIdPropValue(propValue)) { allDefault = false; break; } } } if (allDefault) { baseSlot = 3; } } if (!N.isEmpty(excludedPropNames)) { final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass); // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the // exclusion path returns an immutable list, matching the no-exclusion path below. return ImmutableList.wrap(N.excludeAll(val[baseSlot], excludedPropNames)); } // Return the memoized immutable list for this (class, slot) so both paths return an // ImmutableList<String> while preserving reference identity for the builders' == fast paths. return getNoExclusionPropNames(entityClass, baseSlot); } /** * Returns the property names to be used for INSERT operations on the given entity class. * This method returns all insertable properties (including ID fields) excluding those specified in {@code excludedPropNames}. * * <p>Unlike the instance-based version, this method does not check for default ID values since no * entity instance is provided. It returns all insertable properties including IDs.</p> * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Get all insertable property names for a class * Collection<String> insertProps = QueryUtil.insertPropertyNames(User.class, null); * // Returns: ["id", "name", "email", "createdDate", ...] * * // Exclude specific properties * Set<String> excluded = N.asSet("createdDate", "updatedDate"); * Collection<String> filteredProps = QueryUtil.insertPropertyNames(User.class, excluded); * // Returns: ["id", "name", "email", ...] * }</pre> * * @param entityClass the entity class to analyze (must not be {@code null}) * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions) * @return an immutable list of property names suitable for INSERT operations * @throws IllegalArgumentException if {@code entityClass} is {@code null} */ @Internal public static ImmutableList<String> insertPropertyNames(final Class<?> entityClass, final Set<String> excludedPropNames) { N.checkArgNotNull(entityClass, ENTITY_CLASS); if (N.isEmpty(excludedPropNames)) { // Return the memoized immutable list (slot 2: insert with id) so both paths return an // ImmutableList<String> while preserving reference identity for the builders' == fast paths. return getNoExclusionPropNames(entityClass, 2); } final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass); // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the // exclusion path also returns an immutable list. return ImmutableList.wrap(N.excludeAll(val[2], excludedPropNames)); } /** * Returns the property names to be used for SELECT operations on the given entity class. * This method can optionally include sub-entity properties for nested object retrieval. * * <p>When {@code includeSubEntityProperties} is {@code true}, the method returns nested properties using * dot notation (e.g., {@code "address.street"}). This is useful for building SELECT statements * that need to retrieve data for nested entity relationships.</p> * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Get top-level properties only * Collection<String> selectProps = QueryUtil.selectPropertyNames(User.class, false, null); * // Returns: ["id", "name", "email", ...] * * // Include sub-entity properties (e.g., nested Address) * Collection<String> allProps = QueryUtil.selectPropertyNames(User.class, true, null); * // Returns: ["id", "name", "email", "address.street", "address.city", ...] * * // Exclude specific properties * Set<String> excluded = N.asSet("email"); * Collection<String> filteredProps = QueryUtil.selectPropertyNames(User.class, false, excluded); * // Returns: ["id", "name", ...] * }</pre> * * @param entityClass the entity class to analyze (must not be {@code null}) * @param includeSubEntityProperties {@code true} to include nested entity properties, {@code false} for top-level only * @param excludedPropNames set of property names to exclude (nullable). When sub-entity properties * are included, excluding a root such as {@code "address"} also excludes descendants such as * {@code "address.street"}. * @return an immutable list of property names suitable for SELECT operations * @throws IllegalArgumentException if {@code entityClass} is {@code null} */ @Internal public static ImmutableList<String> selectPropertyNames(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) { N.checkArgNotNull(entityClass, ENTITY_CLASS); final int slot = includeSubEntityProperties ? 0 : 1; if (N.isEmpty(excludedPropNames)) { // Return the memoized immutable list for this (class, slot) so both paths return an // ImmutableList<String> while preserving reference identity for the builders' == fast paths. return getNoExclusionPropNames(entityClass, slot); } final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass); if (includeSubEntityProperties) { final List<String> result = new ArrayList<>(val[slot].size()); outer: for (final String propName : val[slot]) { for (final String excludedPropName : excludedPropNames) { if (excludedPropName != null && (propName.equals(excludedPropName) || propName.startsWith(excludedPropName + SK.PERIOD))) { continue outer; } } result.add(propName); } return ImmutableList.wrap(result); } // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the // exclusion path also returns an immutable list. return ImmutableList.wrap(N.excludeAll(val[slot], excludedPropNames)); } /** * Returns the property names to be used for SELECT operations on the given entity instance. * This is an instance-based convenience overload that derives the entity class via * {@code entity.getClass()} and delegates to {@link #selectPropertyNames(Class, boolean, Set)}. * * <p><b>Usage Examples:</b></p> * <pre>{@code * User user = new User(); * Collection<String> selectProps = QueryUtil.selectPropertyNames(user, false, null); * // Returns: ["id", "name", "email", ...] * }</pre> * * @param entity the entity instance to analyze (must not be {@code null}) * @param includeSubEntityProperties {@code true} to include nested entity properties, {@code false} for top-level only * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions) * @return an immutable list of property names suitable for SELECT operations * @throws IllegalArgumentException if {@code entity} is {@code null} * @see #selectPropertyNames(Class, boolean, Set) */ @Internal public static ImmutableList<String> selectPropertyNames(final Object entity, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) { N.checkArgNotNull(entity, "entity"); return selectPropertyNames(entity.getClass(), includeSubEntityProperties, excludedPropNames); } /** * Returns the property names to be used for UPDATE operations on the given entity class. * This method excludes properties marked as non-updatable. Note that a plain read-write * {@code @Id} property is <i>not</i> excluded; only {@code @ReadOnly}/{@code @ReadOnlyId} * (read-only) id properties are. * * <p>Properties are considered non-updatable if they are:</p> * <ul> * <li>Annotated with {@code @ReadOnly}, {@code @ReadOnlyId}, or otherwise marked as a read-only id property</li> * <li>Annotated with {@code @NonUpdatable}</li> * <li>Excluded from column mapping (e.g. {@code transient}, annotated with {@code @NonColumn}, * or filtered out by the {@code @Table} {@code columnFields}/{@code nonColumnFields} configuration)</li> * <li>Listed in the {@code excludedPropNames} parameter</li> * </ul> * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Account declares a plain read-write @Id "id", so it remains updatable. * Collection<String> updateProps = QueryUtil.updatePropertyNames(Account.class, null); * // returns ["id", "gui", "emailAddress", "firstName", ...] (a plain @Id is NOT excluded) * * // Exclude additional properties explicitly. * Collection<String> filteredProps = QueryUtil.updatePropertyNames(Account.class, N.asSet("createTime")); * // returns the same list without "createTime" * }</pre> * * @param entityClass the entity class to analyze (must not be {@code null}) * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions) * @return an immutable list of property names suitable for UPDATE operations * @throws IllegalArgumentException if {@code entityClass} is {@code null} */ @Internal public static ImmutableList<String> updatePropertyNames(final Class<?> entityClass, final Set<String> excludedPropNames) { N.checkArgNotNull(entityClass, ENTITY_CLASS); if (N.isEmpty(excludedPropNames)) { // Return the memoized immutable list (slot 4: update) so both paths return an // ImmutableList<String> while preserving reference identity for the builders' == fast paths. return getNoExclusionPropNames(entityClass, 4); } final Collection<String>[] val = SqlBuilder.loadPropNamesByClass(entityClass); // N.excludeAll returns a freshly-built mutable ArrayList; wrap it (no extra copy) so the // exclusion path also returns an immutable list. return ImmutableList.wrap(N.excludeAll(val[4], excludedPropNames)); } /** * Returns the property names to be used for UPDATE operations on the given entity instance. * This is an instance-based convenience overload that derives the entity class via * {@code entity.getClass()} and delegates to {@link #updatePropertyNames(Class, Set)}. * * <p><b>Usage Examples:</b></p> * <pre>{@code * Account account = new Account(); * Collection<String> updateProps = QueryUtil.updatePropertyNames(account, null); * // Returns the same names as updatePropertyNames(Account.class, null) * }</pre> * * @param entity the entity instance to analyze (must not be {@code null}) * @param excludedPropNames set of property names to exclude from the result (nullable; {@code null} or empty means no exclusions) * @return an immutable list of property names suitable for UPDATE operations * @throws IllegalArgumentException if {@code entity} is {@code null} * @see #updatePropertyNames(Class, Set) */ @Internal public static ImmutableList<String> updatePropertyNames(final Object entity, final Set<String> excludedPropNames) { N.checkArgNotNull(entity, "entity"); return updatePropertyNames(entity.getClass(), excludedPropNames); } /** * Returns the ID property names for the specified entity class. * ID fields are identified by {@code @Id} annotations on the entity properties. * * <p>This method returns all properties annotated with {@code @Id}. For entities without * explicit ID fields, an empty list is returned.</p> * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Get ID property names for an entity with @Id annotation * List<String> idFields = QueryUtil.idPropertyNames(User.class); * // Returns: ["id"] for a class with @Id on the "id" property * * // Composite key example * List<String> compositeIds = QueryUtil.idPropertyNames(OrderItem.class); * // Returns: ["orderId", "itemId"] for a composite key entity * * // Entity without @Id returns empty list * List<String> noIds = QueryUtil.idPropertyNames(LogEntry.class); * // Returns: [] * }</pre> * * @param entityClass the entity class to analyze (must not be {@code null}) * @return an immutable list of ID property names, or an empty list if no ID properties are defined * @throws IllegalArgumentException if {@code entityClass} is {@code null} */ @Internal @Immutable public static ImmutableList<String> idPropertyNames(final Class<?> entityClass) { N.checkArgNotNull(entityClass, ENTITY_CLASS); return ParserUtil.getBeanInfo(entityClass).idPropNameList; } /** * Determines whether a property should be excluded from database column mapping. * A property is not a column if it's {@code transient}, annotated with {@code @NonColumn}, or excluded by {@code @Table} configuration. * * <p><b>Usage Examples:</b></p> * <pre>{@code * BeanInfo beanInfo = ParserUtil.getBeanInfo(User.class); * PropInfo propInfo = beanInfo.getPropInfo("tempField"); * * // Check if a property is excluded from column mapping * Set<String> columnFields = N.asSet("id", "name", "email"); * Set<String> nonColumnFields = N.emptySet(); * boolean excluded = QueryUtil.isNonColumn(columnFields, nonColumnFields, propInfo); * // Returns true if "tempField" is not in columnFields * * // Check with nonColumnFields * Set<String> nonColumns = N.asSet("tempField", "transientData"); * boolean excluded2 = QueryUtil.isNonColumn(N.emptySet(), nonColumns, propInfo); * // Returns true if "tempField" is in nonColumnFields * }</pre> * * @param columnFields set of field names explicitly included as columns (typically derived from * {@link Table#columnFields()}; may be {@code null} or empty for no whitelist) * @param nonColumnFields set of field names explicitly excluded as columns (typically derived from * {@link Table#nonColumnFields()}; may be {@code null} or empty for no blacklist) * @param propInfo the property information to check (must not be {@code null}) * @return {@code true} if the property should not be mapped to a database column * @throws IllegalArgumentException if {@code propInfo} is {@code null} */ @Internal public static boolean isNonColumn(final Set<String> columnFields, final Set<String> nonColumnFields, final PropInfo propInfo) { N.checkArgNotNull(propInfo, "propInfo"); return propInfo.isTransient || propInfo.isAnnotationPresent(NonColumn.class) || (N.notEmpty(columnFields) && !columnFields.contains(propInfo.name)) || (N.notEmpty(nonColumnFields) && nonColumnFields.contains(propInfo.name)); } /** * Dense lookup of pre-built placeholder strings for the common counts {@code 0..30}. * Indexed directly by count so {@link #placeholders(int)} avoids both {@link Integer} * boxing and a hash lookup on the hot path; {@code QM_CACHE[i]} equals * {@code Strings.repeat("?", i, ", ")}. */ private static final String[] QM_CACHE = new String[31]; // Pre-built placeholder strings for the larger "round" counts that the previous map-based // cache also retained, kept as constants so batch INSERT / large IN (...) clauses reuse a // single instance instead of rebuilding the string on every call. private static final String QM_100; private static final String QM_200; private static final String QM_300; private static final String QM_500; private static final String QM_1000; static { for (int i = 0; i < QM_CACHE.length; i++) { QM_CACHE[i] = Strings.repeat("?", i, ", "); } QM_100 = Strings.repeat("?", 100, ", "); QM_200 = Strings.repeat("?", 200, ", "); QM_300 = Strings.repeat("?", 300, ", "); QM_500 = Strings.repeat("?", 500, ", "); QM_1000 = Strings.repeat("?", 1000, ", "); } /** * Generates a string of question marks ({@code ?}) repeated {@code placeholderCount} times with comma-space delimiter ({@code ", "}). * This is commonly used for building parameterized SQL queries with multiple placeholders. * Common values are pre-cached for performance optimization. * * <p>Cached values include: 0-30, 100, 200, 300, 500, 1000</p> * * <p><b>Usage Examples:</b></p> * <pre>{@code * String placeholders = QueryUtil.placeholders(3); * // Returns: "?, ?, ?" * String sql = "INSERT INTO users (name, email, age) VALUES (" + placeholders + ")"; * // Result: "INSERT INTO users (name, email, age) VALUES (?, ?, ?)" * }</pre> * * @param placeholderCount the number of question marks to generate (must not be negative) * @return a string containing {@code placeholderCount} question marks separated by {@code ", "}, or empty string if {@code placeholderCount} is 0 * @throws IllegalArgumentException if {@code placeholderCount} is negative */ public static String placeholders(final int placeholderCount) { N.checkArgNotNegative(placeholderCount, "placeholderCount"); if (placeholderCount < QM_CACHE.length) { return QM_CACHE[placeholderCount]; } switch (placeholderCount) { case 100: return QM_100; case 200: return QM_200; case 300: return QM_300; case 500: return QM_500; case 1000: return QM_1000; default: return Strings.repeat("?", placeholderCount, ", "); } } /** * Returns the table alias from the {@code @Table} annotation on the entity class. * The alias can be used in SQL queries to reference the table with a shorter name. * * <p>If no {@code @Table} annotation exists, this method returns {@code null}. If {@code @Table} is present * but the alias attribute is not specified, this method returns an empty string.</p> * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Given: @Table(name = "users", alias = "u") on User class * String alias = QueryUtil.tableAlias(User.class); * // Returns: "u" * * // Given: @Table(name = "orders") on Order class (no alias) * String alias2 = QueryUtil.tableAlias(Order.class); * // Returns: "" (empty string when alias is not specified) * * // Given: no @Table annotation on LogEntry class * String alias3 = QueryUtil.tableAlias(LogEntry.class); * // Returns: null * }</pre> * * @param entityClass the entity class to check (must not be {@code null}) * @return the table alias if defined in {@code @Table} annotation, empty string if {@code @Table} is present but alias is not set, or {@code null} if no {@code @Table} annotation exists * @throws IllegalArgumentException if {@code entityClass} is {@code null} */ @Internal public static String tableAlias(final Class<?> entityClass) { N.checkArgNotNull(entityClass, ENTITY_CLASS); final Table anno = entityClass.getAnnotation(Table.class); return anno == null ? null : anno.alias(); } /** * Returns the table name and optional alias for the entity class using the default naming policy. * If {@code @Table} annotation is present, uses its values; otherwise derives the table name from the class name * using {@code NamingPolicy.SNAKE_CASE}. * * <p>The returned string is formatted as "tableName" or "tableName alias" depending on whether * an alias is defined.</p> * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Given: @Table(name = "users", alias = "u") on User class * String tableAndAlias = QueryUtil.tableNameAndAlias(User.class); * // Returns: "users u" * * // Given: @Table(name = "orders") on Order class (no alias) * String tableOnly = QueryUtil.tableNameAndAlias(Order.class); * // Returns: "orders" * * // Given: no @Table annotation on MyEntity class * String derived = QueryUtil.tableNameAndAlias(MyEntity.class); * // Returns: "my_entity" (derived from class name using SNAKE_CASE) * }</pre> * * @param entityClass the entity class to analyze (must not be {@code null}) * @return the table name, optionally followed by space and alias * @throws IllegalArgumentException if {@code entityClass} is {@code null} */ @Internal public static String tableNameAndAlias(final Class<?> entityClass) { return tableNameAndAlias(entityClass, NamingPolicy.SNAKE_CASE); } /** * Returns the table name and optional alias for the entity class using the specified naming policy. * The table name is resolved the same way the query builders resolve it: from the {@code @Table} * annotation ({@link Table#name() name} or its deprecated {@code value()} alias) or a JPA * {@code javax.persistence}/{@code jakarta.persistence} {@code @Table} annotation; only when no * annotated name exists is the table name derived from the class name using the provided naming * policy. The alias comes from {@link Table#alias()} and is appended after a space when present. * * <p>Annotated names are used directly without any transformation; the naming policy is only applied * to a class-name-derived table name.</p> * * <p><b>Usage Examples:</b></p> * <pre>{@code * // Given: @Table(name = "users", alias = "u") - annotation takes priority * String tableAndAlias = QueryUtil.tableNameAndAlias(User.class, NamingPolicy.SCREAMING_SNAKE_CASE); * // Returns: "users u" (annotation values used as-is, naming policy ignored) * * // Given: no @Table annotation on MyEntity class * String snakeCase = QueryUtil.tableNameAndAlias(MyEntity.class, NamingPolicy.SNAKE_CASE); * // Returns: "my_entity" * * String upperCase = QueryUtil.tableNameAndAlias(MyEntity.class, NamingPolicy.SCREAMING_SNAKE_CASE); * // Returns: "MY_ENTITY" * }</pre> * * @param entityClass the entity class to analyze (must not be {@code null}) * @param namingPolicy the naming policy used when no annotated table name exists. If {@code null}, * defaults to {@code NamingPolicy.SNAKE_CASE}. * @return the table name, optionally followed by space and alias * @throws IllegalArgumentException if {@code entityClass} is {@code null} */ @Internal public static String tableNameAndAlias(final Class<?> entityClass, final NamingPolicy namingPolicy) { N.checkArgNotNull(entityClass, ENTITY_CLASS); final NamingPolicy effectiveNamingPolicy = namingPolicy == null ? NamingPolicy.SNAKE_CASE : namingPolicy; // Resolve the annotated table name the same way the query builders do: BeanInfo.tableName honors // @Table.name(), its deprecated @Table.value() alias, and the javax/jakarta persistence @Table // annotations, so this helper cannot diverge from the FROM clause rendered for the same class. final BeanInfo entityInfo = ParserUtil.getBeanInfo(entityClass); final Table anno = entityClass.getAnnotation(Table.class); final String alias = anno == null ? null : anno.alias(); final String tableName = entityInfo.tableName.isPresent() ? entityInfo.tableName.get() : effectiveNamingPolicy.convert(ClassUtil.getSimpleClassName(entityClass)); return Strings.isEmpty(alias) ? tableName : Strings.concat(tableName, SK.SPACE, alias); } }`
- **Parameters:**
  - `columnName` (`String`)
  - `isUnqualified` (`boolean`)

### Class Selection (com.landawn.abacus.query.Selection)
Represents a selection specification for SQL queries, particularly useful for complex multi-table selections.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### builder(...) -> SelectionBuilder
- **Signature:** `public static SelectionBuilder builder(final Class<?> entityClass)`
- **Summary:** Creates a builder with the required entity class already set.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class to select; must not be {@code null}
- **Returns:** a new selection builder
- **Signature:** `public static SelectionBuilder builder()`
- **Summary:** Creates an unconstrained selection builder for compatibility.
- **Parameters:**
  - (none)
- **Returns:** a new selection builder

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Selection()`
- **Summary:** Creates a new empty Selection instance.
- **Parameters:**
  - (none)
##### includedPropNames(...) -> Selection
- **Signature:** `public Selection includedPropNames(final Collection<String> includedPropNames)`
- **Summary:** Sets the property names to include in this selection.
- **Parameters:**
  - `includedPropNames` (`Collection<String>`) — the property names to include; {@code null} means all properties
- **Returns:** this {@code Selection} instance for method chaining
- **Signature:** `public Collection<String> includedPropNames()`
- **Summary:** Returns the property names to include in this selection.
- **Contract:**
  - Returns {@code null} when no specific properties have been set.
- **Parameters:**
  - (none)
- **Returns:** an immutable view of the selected property names, or {@code null} if none was set
##### includesSubEntityProperties(...) -> boolean
- **Signature:** `public boolean includesSubEntityProperties()`
- **Summary:** Returns whether properties from sub-entities are included.
- **Parameters:**
  - (none)
- **Returns:** {@code true} when sub-entity properties are included; otherwise {@code false}
##### excludedPropNames(...) -> Selection
- **Signature:** `public Selection excludedPropNames(final Set<String> excludedPropNames)`
- **Summary:** Sets the property names to exclude from this selection.
- **Parameters:**
  - `excludedPropNames` (`Set<String>`) — the property names to exclude; can be {@code null}
- **Returns:** this {@code Selection} instance for method chaining
- **Signature:** `public Set<String> excludedPropNames()`
- **Summary:** Returns the property names to exclude from this selection.
- **Contract:**
  - Returns {@code null} when no exclusions have been set.
- **Parameters:**
  - (none)
- **Returns:** an immutable view of the excluded property names, or {@code null} if none was set

### Class SelectionBuilder (com.landawn.abacus.query.Selection.SelectionBuilder)
Builder for {@link Selection} instances.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### entityClass(...) -> SelectionBuilder
- **Signature:** `public SelectionBuilder entityClass(final Class<?> entityClass)`
- **Summary:** Sets the entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
- **Returns:** this builder
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
- **Summary:** Sets included property names.
- **Parameters:**
  - `includedPropNames` (`Collection<String>`) — the included names
- **Returns:** this builder
##### includeSubEntityProperties(...) -> SelectionBuilder
- **Signature:** `public SelectionBuilder includeSubEntityProperties(final boolean includeSubEntityProperties)`
- **Summary:** Sets whether sub-entity properties are included.
- **Parameters:**
  - `includeSubEntityProperties` (`boolean`) — the inclusion flag
- **Returns:** this builder
##### excludedPropNames(...) -> SelectionBuilder
- **Signature:** `public SelectionBuilder excludedPropNames(final Set<String> excludedPropNames)`
- **Summary:** Sets excluded property names.
- **Parameters:**
  - `excludedPropNames` (`Set<String>`) — the excluded names
- **Returns:** this builder
##### build(...) -> Selection
- **Signature:** `public Selection build()`
- **Summary:** Builds a selection, defensively copying its property collections.
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
- **Summary:** Returns the {@code SortDirection} corresponding to the given name.
- **Parameters:**
  - `name` (`String`) — the sort direction name to look up (case-insensitive); may be {@code null}
- **Returns:** the matching {@code SortDirection} , or {@code null} if {@code name} is {@code null} or does not name a recognized direction

#### Public Instance Methods
##### isAscending(...) -> boolean
- **Signature:** `public boolean isAscending()`
- **Summary:** Checks if this sort direction is ascending.
- **Contract:**
  - Checks if this sort direction is ascending.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if this is ASC, {@code false} if DESC
##### isDescending(...) -> boolean
- **Signature:** `public boolean isDescending()`
- **Summary:** Checks if this sort direction is descending.
- **Contract:**
  - Checks if this sort direction is descending.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if this is DESC, {@code false} if ASC
##### opposite(...) -> SortDirection
- **Signature:** `public SortDirection opposite()`
- **Summary:** Returns the opposite sort direction.
- **Parameters:**
  - (none)
- **Returns:** {@link #DESC} if this is {@link #ASC} , otherwise {@link #ASC}

### Class SqlBuilder (com.landawn.abacus.query.SqlBuilder)
A fluent SQL builder that extends {@link AbstractQueryBuilder} with concrete SQL generation, including condition rendering, operator handling, and NULL semantics.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class SqlDialect (com.landawn.abacus.query.SqlDialect)
Immutable configuration object used by {@link Dsl} to render generated SQL.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `class SqlDialect { /** * Optional descriptor of the target database product. When set, query builders branch on * {@link ProductInfo#name()} to emit product-specific SQL: Oracle, DB2 and SQL Server dialects * render pagination with {@code OFFSET ... ROWS} / {@code FETCH ... ROWS ONLY} instead of * {@code LIMIT}/{@code OFFSET}, and a {@code null} {@code identifierQuote} defaults to * {@link IdentifierQuote#BACKTICK} for MySQL/MariaDB. When {@code null}, builders use the default * SQL syntax. */ private ProductInfo productInfo; /** * Naming policy used to translate Java property names into generated SQL identifiers. For example, * {@link NamingPolicy#SNAKE_CASE} renders {@code firstName} as {@code first_name}. When {@code null}, * builders use {@link NamingPolicy#SNAKE_CASE}. */ private NamingPolicy namingPolicy; /** * Parameter rendering policy for values supplied to builder operations. When {@code null}, builders * use {@link SqlPolicy#RAW_SQL}. */ private SqlPolicy sqlPolicy; /** * Quote style used for generated aliases and identifiers that need quoting. When {@code null}, * builders use {@link IdentifierQuote#DOUBLE_QUOTE}, except for MySQL/MariaDB product metadata, * which defaults to {@link IdentifierQuote#BACKTICK}. */ private IdentifierQuote identifierQuote; /** * Identifier quoting style used by SQL builders. * * <p>The enum currently distinguishes the two quote characters supported by this builder: * ANSI double quotes and MySQL-style backticks.</p> */ public static enum IdentifierQuote { /** * ANSI/standard SQL double quote ({@code "}). This is the effective default when * {@code identifierQuote} is {@code null}, except when {@code productInfo} names MySQL or * MariaDB, in which case {@link #BACKTICK} is the default. */ DOUBLE_QUOTE, /** * MySQL/MariaDB-style backtick ({@code `}). */ BACKTICK; } /** * Defines how values supplied to query builders are represented in generated SQL. * * <p>This setting controls value placeholders only. It does not change table/column naming, * identifier quoting, or database-specific SQL syntax.</p> */ public static enum SqlPolicy { /** * Inline values directly into the SQL string as literals. * * <p><b>&#9888;&#65039;</b> Use only for trusted values; parameterized or named policies are preferred for user input.</p> */ RAW_SQL, /** * Render each value as a positional {@code ?} placeholder and collect parameter values in order. */ PARAMETERIZED_SQL, /** * Render values as named placeholders, such as {@code :id} or {@code :firstName}. */ NAMED_SQL, /** * Render values as iBATIS/MyBatis-style named placeholders, such as {@code #{id}}. */ IBATIS_SQL } /** * Immutable descriptor of a database product, holding the product name and version separately. * * <p>When attached to a dialect via {@code SqlDialect.productInfo}, the {@link #name()} drives * product-specific SQL generation in query builders (for example, Oracle-style * {@code FETCH FIRST ... ROWS ONLY} pagination). The name is matched case-insensitively as a * substring, so raw JDBC values from {@code DatabaseMetaData.getDatabaseProductName()} such as * {@code "Microsoft SQL Server"} or {@code "Oracle Database 19c"} are recognized. The * {@link #version()} can be compared numerically via {@link #isVersionAtLeast(String)} and * {@link #isVersionAtMost(String)}.</p> * * @param name the nonblank database product name, such as {@code "MySQL"} or {@code "PostgreSQL"} * @param version the database product version, such as {@code "9.7"} or {@code "18"}; a {@code null} * version is normalized to an empty string, so {@link #version()} never returns {@code null}. * Comparable via {@link #isVersionAtLeast(String)} / {@link #isVersionAtMost(String)} */ public record ProductInfo(String name, String version) { /** * Canonical constructor that requires a nonblank product name and normalizes a {@code null} {@code version} to an empty string, so * {@link #version()} never returns {@code null} and an absent version has a single canonical * representation. This keeps {@code equals}/{@code hashCode} consistent whether the version was * omitted (via {@link #of(String)}) or passed as {@code null}. * * @throws IllegalArgumentException if {@code name} is {@code null}, empty, or blank */ public ProductInfo { if (Strings.isBlank(name)) { throw new IllegalArgumentException("Database product name must not be null, empty, or blank"); } version = version == null ? "" : version; } /** * Creates a {@code ProductInfo} with the given product name and no version (an empty {@link #version()}). * * @param name the database product name, such as {@code "Oracle"} or {@code "MySQL"} * @return a new {@code ProductInfo} with the given name and an empty ({@code ""}) version */ public static ProductInfo of(final String name) { return new ProductInfo(name, ""); } /** * Creates a {@code ProductInfo} with the given product name and version. * * @param name the database product name, such as {@code "Oracle"} or {@code "MySQL"} * @param version the database product version, such as {@code "19c"} or {@code "9.7"} * @return a new {@code ProductInfo} with the given name and version */ public static ProductInfo of(final String name, final String version) { return new ProductInfo(name, version); } /** * Returns whether this descriptor names MySQL. The match is a case-insensitive substring test * against {@link #name()}. This is distinct from {@link #isMariaDB()}, although both share the * same {@code LIMIT}/{@code OFFSET} pagination and backtick-quoting behavior in query builders. * * @return {@code true} if {@link #name()} contains {@code "mysql"} (case-insensitively) */ public boolean isMySQL() { return Strings.containsIgnoreCase(name, "mysql"); } /** * Returns whether this descriptor names MariaDB. The match is a case-insensitive substring test * against {@link #name()}. * * @return {@code true} if {@link #name()} contains {@code "mariadb"} (case-insensitively) */ public boolean isMariaDB() { return Strings.containsIgnoreCase(name, "mariadb"); } /** * Returns whether this descriptor names PostgreSQL. The match is a case-insensitive substring * test against {@link #name()} (the substring {@code "postgres"} also matches {@code "PostgreSQL"}). * * @return {@code true} if {@link #name()} contains {@code "postgres"} (case-insensitively) */ public boolean isPostgreSQL() { return Strings.containsIgnoreCase(name, "postgres"); } /** * Returns whether this descriptor names Microsoft SQL Server. The match is a case-insensitive * substring test against {@link #name()}, so raw JDBC values such as {@code "Microsoft SQL Server"} * are recognized. * * @return {@code true} if {@link #name()} contains {@code "sql server"} or {@code "sqlserver"} (case-insensitively) */ public boolean isSQLServer() { return Strings.containsIgnoreCase(name, "sql server") || Strings.containsIgnoreCase(name, "sqlserver"); } /** * Returns whether this descriptor names Oracle Database. The match is a case-insensitive * substring test against {@link #name()}, so raw JDBC values from * {@code DatabaseMetaData.getDatabaseProductName()} such as {@code "Oracle Database 19c"} * are recognized. * * @return {@code true} if {@link #name()} contains {@code "oracle"} (case-insensitively) */ public boolean isOracle() { return Strings.containsIgnoreCase(name, "oracle"); } /** * Returns whether this descriptor names IBM DB2. The match is a case-insensitive substring * test against {@link #name()}. * * @return {@code true} if {@link #name()} contains {@code "db2"} (case-insensitively) */ public boolean isDB2() { return Strings.containsIgnoreCase(name, "db2"); } /** * Returns whether this descriptor names H2 Database. The match is a case-insensitive substring * test against {@link #name()}. * * @return {@code true} if {@link #name()} contains {@code "h2"} (case-insensitively) */ public boolean isH2() { return Strings.containsIgnoreCase(name, "h2"); } /** * Returns whether this descriptor names SQLite. The match is a case-insensitive substring test * against {@link #name()}. * * @return {@code true} if {@link #name()} contains {@code "sqlite"} (case-insensitively) */ public boolean isSQLite() { return Strings.containsIgnoreCase(name, "sqlite"); } /** * Returns whether this product's {@link #version()} is greater than or equal to the given version. * * <p>Versions are compared by their leading dot-separated run of integer components: for example * {@code "8.0.32"} parses to {@code [8, 0, 32]} and {@code "19c"} to {@code [19]} (parsing stops at * the first character that is neither a digit nor a dot). Missing trailing components are treated as * {@code 0}, so {@code "8.0"} equals {@code "8.0.0"} and is less than {@code "8.1"}.</p> * * <p>Returns {@code false} (not comparable) when either this {@link #version()} or {@code minVersion} * is {@code null}, blank, or does not begin with an integer component. This makes the method safe for * feature-gating: an unknown or unparseable version simply fails the check.</p> * * @param minVersion the minimum version to compare against, such as {@code "8.0"} or {@code "19"} * @return {@code true} if this product's version parses and is greater than or equal to {@code minVersion}; * {@code false} otherwise, including when either version is not comparable * @see #isVersionAtMost(String) */ public boolean isVersionAtLeast(final String minVersion) { final long[] mine = parseVersionComponents(version()); final long[] other = parseVersionComponents(minVersion); return mine != null && other != null && compareVersionComponents(mine, other) >= 0; } /** * Returns whether this product's {@link #version()} is less than or equal to the given version. * * <p>Versions are compared by their leading dot-separated run of integer components; see * {@link #isVersionAtLeast(String)} for the parsing and padding rules.</p> * * <p>Returns {@code false} (not comparable) when either this {@link #version()} or {@code maxVersion} * is {@code null}, blank, or does not begin with an integer component.</p> * * @param maxVersion the maximum version to compare against, such as {@code "8.0"} or {@code "19"} * @return {@code true} if this product's version parses and is less than or equal to {@code maxVersion}; * {@code false} otherwise, including when either version is not comparable * @see #isVersionAtLeast(String) */ public boolean isVersionAtMost(final String maxVersion) { final long[] mine = parseVersionComponents(version()); final long[] other = parseVersionComponents(maxVersion); return mine != null && other != null && compareVersionComponents(mine, other) <= 0; } /** * Parses the leading dot-separated run of integer components of a version string (for example * {@code "8.0.32"} to {@code [8, 0, 32]} and {@code "19c"} to {@code [19]}), or {@code null} when the * string is {@code null}, blank, does not begin with a digit, or has an empty/unparseable component. */ private static long[] parseVersionComponents(final String version) { if (Strings.isBlank(version)) { return null; } final String trimmed = version.trim(); if (!Character.isDigit(trimmed.charAt(0))) { return null; } int end = 0; while (end < trimmed.length() && (Character.isDigit(trimmed.charAt(end)) || trimmed.charAt(end) == '.')) { end++; } // Retain trailing empty components so malformed versions such as "8." and "8.." // are rejected instead of silently comparing as version 8. final String[] parts = trimmed.substring(0, end).split("\\.", -1); final long[] components = new long[parts.length]; try { for (int i = 0; i < parts.length; i++) { components[i] = Long.parseLong(parts[i]); } } catch (final NumberFormatException e) { return null; } return components; } /** * Compares two version-component arrays element by element, treating missing trailing components as * {@code 0}. Returns a negative, zero, or positive value when {@code a} is respectively less than, * equal to, or greater than {@code b}. */ private static int compareVersionComponents(final long[] a, final long[] b) { final int len = Math.max(a.length, b.length); for (int i = 0; i < len; i++) { final long av = i < a.length ? a[i] : 0L; final long bv = i < b.length ? b[i] : 0L; if (av != bv) { return av < bv ? -1 : 1; } } return 0; } } }`
- **Parameters:**
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
- (none)

#### Public Static Methods
##### of(...) -> ProductInfo
- **Signature:** `public static ProductInfo of(final String name)`
- **Summary:** Creates a {@code ProductInfo} with the given product name and no version (an empty {@link #version()} ).
- **Parameters:**
  - `name` (`String`) — the database product name, such as {@code "Oracle"} or {@code "MySQL"}
- **Returns:** a new {@code ProductInfo} with the given name and an empty ( {@code ""} ) version
- **Signature:** `public static ProductInfo of(final String name, final String version)`
- **Summary:** Creates a {@code ProductInfo} with the given product name and version.
- **Parameters:**
  - `name` (`String`) — the database product name, such as {@code "Oracle"} or {@code "MySQL"}
  - `version` (`String`) — the database product version, such as {@code "19c"} or {@code "9.7"}
- **Returns:** a new {@code ProductInfo} with the given name and version

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public ProductInfo`
- **Summary:** Canonical constructor that requires a nonblank product name and normalizes a {@code null} {@code version} to an empty string, so {@link #version()} never returns {@code null} and an absent version has a single canonical representation.
- **Parameters:**
  - `name` (`String`)
  - `version` (`String`)
##### isMySQL(...) -> boolean
- **Signature:** `public boolean isMySQL()`
- **Summary:** Returns whether this descriptor names MySQL.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if {@link #name()} contains {@code "mysql"} (case-insensitively)
##### isMariaDB(...) -> boolean
- **Signature:** `public boolean isMariaDB()`
- **Summary:** Returns whether this descriptor names MariaDB.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if {@link #name()} contains {@code "mariadb"} (case-insensitively)
##### isPostgreSQL(...) -> boolean
- **Signature:** `public boolean isPostgreSQL()`
- **Summary:** Returns whether this descriptor names PostgreSQL.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if {@link #name()} contains {@code "postgres"} (case-insensitively)
##### isSQLServer(...) -> boolean
- **Signature:** `public boolean isSQLServer()`
- **Summary:** Returns whether this descriptor names Microsoft SQL Server.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if {@link #name()} contains {@code "sql server"} or {@code "sqlserver"} (case-insensitively)
##### isOracle(...) -> boolean
- **Signature:** `public boolean isOracle()`
- **Summary:** Returns whether this descriptor names Oracle Database.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if {@link #name()} contains {@code "oracle"} (case-insensitively)
##### isDB2(...) -> boolean
- **Signature:** `public boolean isDB2()`
- **Summary:** Returns whether this descriptor names IBM DB2.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if {@link #name()} contains {@code "db2"} (case-insensitively)
##### isH2(...) -> boolean
- **Signature:** `public boolean isH2()`
- **Summary:** Returns whether this descriptor names H2 Database.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if {@link #name()} contains {@code "h2"} (case-insensitively)
##### isSQLite(...) -> boolean
- **Signature:** `public boolean isSQLite()`
- **Summary:** Returns whether this descriptor names SQLite.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if {@link #name()} contains {@code "sqlite"} (case-insensitively)
##### isVersionAtLeast(...) -> boolean
- **Signature:** `public boolean isVersionAtLeast(final String minVersion)`
- **Summary:** Returns whether this product's {@link #version()} is greater than or equal to the given version.
- **Contract:**
  - </p> <p> Returns {@code false} (not comparable) when either this {@link #version()} or {@code minVersion} is {@code null} , blank, or does not begin with an integer component.
- **Parameters:**
  - `minVersion` (`String`) — the minimum version to compare against, such as {@code "8.0"} or {@code "19"}
- **Returns:** {@code true} if this product's version parses and is greater than or equal to {@code minVersion} ; {@code false} otherwise, including when either version is not comparable
- **See also:** #isVersionAtMost(String)
##### isVersionAtMost(...) -> boolean
- **Signature:** `public boolean isVersionAtMost(final String maxVersion)`
- **Summary:** Returns whether this product's {@link #version()} is less than or equal to the given version.
- **Contract:**
  - </p> <p> Returns {@code false} (not comparable) when either this {@link #version()} or {@code maxVersion} is {@code null} , blank, or does not begin with an integer component.
- **Parameters:**
  - `maxVersion` (`String`) — the maximum version to compare against, such as {@code "8.0"} or {@code "19"}
- **Returns:** {@code true} if this product's version parses and is less than or equal to {@code maxVersion} ; {@code false} otherwise, including when either version is not comparable
- **See also:** #isVersionAtLeast(String)

### Class SqlMapper (com.landawn.abacus.query.SqlMapper)
A utility class for managing SQL scripts stored in XML files and mapping them to short identifiers.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### loadFrom(...) -> SqlMapper
- **Signature:** `public static SqlMapper loadFrom(final String filePaths)`
- **Summary:** Creates a SqlMapper instance by loading SQL definitions from one or more XML files.
- **Parameters:**
  - `filePaths` (`String`) — one or more file paths separated by ',' or ';' (must not be {@code null} or empty). Each path is resolved against the literal location first; when no file exists there, the common configuration directories are searched as a fallback
- **Returns:** a new SqlMapper instance loaded with SQL definitions from the specified files
- **Signature:** `public static SqlMapper loadFrom(final String firstFilePath, final String... additionalFilePaths)`
- **Summary:** Creates a SqlMapper by loading separately supplied file paths.
- **Parameters:**
  - `firstFilePath` (`String`) — the first XML mapper path; must not be {@code null} or empty
  - `additionalFilePaths` (`String[]`) — additional XML mapper paths; no element may be {@code null} or empty
- **Returns:** a new mapper containing definitions from every supplied path
- **Signature:** `public static SqlMapper loadFrom(final File... files)`
- **Summary:** Creates a SqlMapper instance by loading SQL definitions from one or more XML files.
- **Contract:**
  - Each file must contain a {@code <sqlMapper>} root element; definitions from all files are merged into a single mapper.
- **Parameters:**
  - `files` (`File[]`) — one or more XML files to load (must not be {@code null} or empty, and no element may be {@code null} )
- **Returns:** a new SqlMapper instance loaded with SQL definitions from the specified files
- **Signature:** `public static SqlMapper loadFrom(final InputStream inputStream)`
- **Summary:** Creates a SqlMapper instance by loading SQL definitions from the supplied input stream.
- **Contract:**
  - The stream content must contain a {@code <sqlMapper>} root element.
- **Parameters:**
  - `inputStream` (`InputStream`) — the input stream to read the XML SQL definitions from (must not be {@code null} )
- **Returns:** a new SqlMapper instance loaded with SQL definitions from the stream

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public SqlMapper()`
- **Summary:** Creates an empty SqlMapper instance.
- **Parameters:**
  - (none)
##### ids(...) -> ImmutableSet<String>
- **Signature:** `public ImmutableSet<String> ids()`
- **Summary:** Returns an immutable snapshot of all SQL identifiers registered in this mapper.
- **Parameters:**
  - (none)
- **Returns:** an immutable snapshot of all SQL identifiers in this mapper, maintaining insertion order
##### get(...) -> ParsedSql
- **Signature:** `public ParsedSql get(final String id)`
- **Summary:** Retrieves the parsed SQL associated with the specified identifier.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml"); ParsedSql sql = mapper.get("findAccountById"); if (sql != null) { String parameterizedSql = sql.parameterizedSql(); // Use with PreparedStatement PreparedStatement stmt = connection.prepareStatement(parameterizedSql); } // Returns null for unknown ids ParsedSql unknown = mapper.get("nonExistentId"); // unknown is null } </pre>
- **Parameters:**
  - `id` (`String`) — the SQL identifier to look up
- **Returns:** the {@link ParsedSql} object, or {@code null} if the id is {@code null} , empty, exceeds {@link #MAX_ID_LENGTH} characters, or is not found
##### containsId(...) -> boolean
- **Signature:** `public boolean containsId(final String id)`
- **Summary:** Returns {@code true} if this mapper contains an SQL registered under the specified identifier.
- **Contract:**
  - Returns {@code true} if this mapper contains an SQL registered under the specified identifier.
- **Parameters:**
  - `id` (`String`) — the SQL identifier to test
- **Returns:** {@code true} if a matching SQL is registered; {@code false} if the id is {@code null} , empty, exceeds {@link #MAX_ID_LENGTH} characters, or is not found
- **See also:** #get(String)
##### attributes(...) -> ImmutableMap<String, String>
- **Signature:** `public ImmutableMap<String, String> attributes(final String id)`
- **Summary:** Retrieves the attributes associated with the specified SQL identifier.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Given XML: <sql id="batchInsert" batchSize="100" timeout="30">...</sql> SqlMapper mapper = SqlMapper.loadFrom("sql/queries.xml"); ImmutableMap<String, String> attrs = mapper.attributes("batchInsert"); if (attrs != null) { String batchSize = attrs.get("batchSize"); // "100" String timeout = attrs.get("timeout"); // "30" } // Returns null for unknown ids ImmutableMap<String, String> unknown = mapper.attributes("nonExistentId"); // unknown is null } </pre>
- **Parameters:**
  - `id` (`String`) — the SQL identifier to look up
- **Returns:** an immutable map of attribute names to values, or {@code null} if the id is {@code null} , empty, exceeds {@link #MAX_ID_LENGTH} characters, or is not found
##### add(...) -> void
- **Signature:** `public void add(final String id, final ParsedSql sql)`
- **Summary:** Adds a parsed SQL with the specified identifier.
- **Contract:**
  - This method validates the ID and throws an exception if an SQL with the same ID already exists.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
  - `sql` (`ParsedSql`) — the parsed SQL to associate with the identifier (must not be {@code null} )
- **Signature:** `public void add(final String id, final ParsedSql sql, final Map<String, String> attributes)`
- **Summary:** Adds a parsed SQL with the specified identifier and attributes.
- **Contract:**
  - This method validates the ID and throws an exception if an SQL with the same ID already exists.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
  - `sql` (`ParsedSql`) — the parsed SQL to associate with the identifier (must not be {@code null} )
  - `attributes` (`Map<String, String>`) — additional XML attributes for the SQL (e.g., batchSize, fetchSize, resultSetType, timeout); may be null or empty, but keys must be valid non-namespace XML attribute names and values must be non-null
- **Signature:** `public void add(final String id, final String sql)`
- **Summary:** Adds a SQL string with the specified identifier and no attributes.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
  - `sql` (`String`) — the SQL string to parse and store (must not be {@code null} or blank)
- **Signature:** `public void add(final String id, final String sql, final Map<String, String> attributes)`
- **Summary:** Adds a SQL string with the specified identifier and attributes.
- **Parameters:**
  - `id` (`String`) — the SQL identifier (must be non-empty, not contain whitespace, and not exceed {@link #MAX_ID_LENGTH} characters)
  - `sql` (`String`) — the SQL string to parse and store (must not be {@code null} or blank)
  - `attributes` (`Map<String, String>`) — additional XML attributes for the SQL (e.g., batchSize, fetchSize, resultSetType, timeout); may be null or empty, but keys must be valid non-namespace XML attribute names and values must be non-null
##### remove(...) -> void
- **Signature:** `public void remove(final String id)`
- **Summary:** Removes the SQL and its attributes associated with the specified identifier.
- **Contract:**
  - If the id is {@code null} , empty, exceeds {@link #MAX_ID_LENGTH} characters, or is not found, this method does nothing.
- **Parameters:**
  - `id` (`String`) — the SQL identifier to remove
##### copy(...) -> SqlMapper
- **Signature:** `public SqlMapper copy()`
- **Summary:** Creates a shallow copy of this SqlMapper instance.
- **Parameters:**
  - (none)
- **Returns:** a new SqlMapper instance with the same SQL definitions and attributes
##### saveTo(...) -> void
- **Signature:** `@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE") public void saveTo(final File file)`
- **Summary:** Saves all SQL definitions in this mapper to an XML file.
- **Contract:**
  - If the file already exists, it will be overwritten.
  - <p> The canonical SQL identifier (the registered map key) is always written as the {@code id} attribute and is protected from being overridden: any stray {@code id} entry in a SQL's attributes map is ignored when emitting attributes.
- **Parameters:**
  - `file` (`File`) — the file to write to (will be created if it doesn't exist; parent directories will be created if needed)
- **Signature:** `public void saveTo(final String filePath)`
- **Summary:** Writes this mapper to the specified file path.
- **Parameters:**
  - `filePath` (`String`) — the target file path; must not be {@code null} or empty
- **Signature:** `public void saveTo(final OutputStream outputStream)`
- **Summary:** Writes all SQL definitions in this mapper to the supplied output stream as XML.
- **Contract:**
  - <p> The canonical SQL identifier (the registered map key) is always written as the {@code id} attribute and is protected from being overridden: any stray {@code id} entry in a SQL's attributes map is ignored when emitting attributes.
- **Parameters:**
  - `outputStream` (`OutputStream`) — the output stream to write to (not closed by this method)
##### size(...) -> int
- **Signature:** `public int size()`
- **Summary:** Returns the number of SQL definitions registered in this mapper.
- **Parameters:**
  - (none)
- **Returns:** the number of registered SQL definitions
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
- **Summary:** Returns the hash code value for this {@code SqlMapper} .
- **Parameters:**
  - (none)
- **Returns:** the hash code value
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this {@code SqlMapper} to another object for equality.
- **Contract:**
  - Two {@code SqlMapper} instances are considered equal if they contain the same id-to-SQL mappings and id-to-attributes mappings (order-independent, per {@link Map#equals(Object)} ).
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if {@code obj} is a {@code SqlMapper} whose internal SQL and attribute maps are equal to this mapper's; {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this {@code SqlMapper} .
- **Parameters:**
  - (none)
- **Returns:** a string representation of this SQL mapper

### Enum SqlOperation (com.landawn.abacus.query.SqlOperation)
Enumeration representing SQL operation types.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### of(...) -> SqlOperation
- **Signature:** `public static SqlOperation of(final String name)`
- **Summary:** Retrieves the {@code SqlOperation} enum value corresponding to the given operation name.
- **Parameters:**
  - `name` (`String`) — the exact SQL operation token or enum constant name to look up (case-insensitive, without surrounding whitespace); may be {@code null}
- **Returns:** the corresponding {@code SqlOperation} enum value, or {@code null} if {@code name} is {@code null} or no matching operation is found
##### fromOrUnknown(...) -> SqlOperation
- **Signature:** `public static SqlOperation fromOrUnknown(final String token)`
- **Summary:** Resolves an operation token, returning {@link #UNKNOWN} for {@code null} or unsupported input.
- **Parameters:**
  - `token` (`String`) — the SQL operation token or enum constant name; may be {@code null}
- **Returns:** the resolved operation, or {@link #UNKNOWN} when no operation matches

#### Public Instance Methods
##### sqlToken(...) -> String
- **Signature:** `public String sqlToken()`
- **Summary:** Returns the SQL text representation of this operation.
- **Parameters:**
  - (none)
- **Returns:** the SQL keyword string representation of this operation, never {@code null}
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns the string representation of this SQL operation.
- **Parameters:**
  - (none)
- **Returns:** the SQL keyword string representation of this operation, never {@code null}

### Class SqlParser (com.landawn.abacus.query.SqlParser)
A utility class for parsing SQL statements into lexical SQL tokens.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### parse(...) -> List<String>
- **Signature:** `public static List<String> parse(final String sql)`
- **Summary:** Parses a SQL statement into a list of lexical SQL tokens.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to parse (must not be {@code null} )
- **Returns:** a list of tokens representing the parsed SQL statement
##### indexOfToken(...) -> int
- **Signature:** `public static int indexOfToken(final String sql, final String token)`
- **Summary:** Finds the index of a specific token within a SQL statement, searching from the beginning using case-insensitive matching.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search within (must not be {@code null} )
  - `token` (`String`) — the token or composite keyword to find (must not be {@code null} )
- **Returns:** the index of the token if found, or {@code -1} if not found
- **See also:** #indexOfToken(String, String, int, boolean)
- **Signature:** `public static int indexOfToken(final String sql, final String token, final int fromIndex)`
- **Summary:** Finds the index of a specific token within a SQL statement, searching from the given position using case-insensitive matching.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search within (must not be {@code null} )
  - `token` (`String`) — the token or composite keyword to find (must not be {@code null} )
  - `fromIndex` (`int`) — the earliest character position at which a match may be reported (0-based); scanning still begins at the start of {@code sql} for correct tokenization, but any match starting before {@code fromIndex} is skipped; negative values are treated as {@code 0}
- **Returns:** the index of the token if found, or {@code -1} if not found
- **See also:** #indexOfToken(String, String, int, boolean)
- **Signature:** `public static int indexOfToken(final String sql, final String token, final int fromIndex, final boolean caseSensitive)`
- **Summary:** Finds the index of a specific token within a SQL statement starting from a given position.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to search within (must not be {@code null} )
  - `token` (`String`) — the token or composite keyword to find (must not be {@code null} )
  - `fromIndex` (`int`) — the earliest character position at which a match may be reported (0-based); scanning still begins at the start of {@code sql} for correct tokenization, but any match starting before {@code fromIndex} is skipped; negative values are treated as {@code 0}
  - `caseSensitive` (`boolean`) — whether the search should be case-sensitive
- **Returns:** the index of the token if found, or {@code -1} if not found
##### nextToken(...) -> String
- **Signature:** `public static String nextToken(final String sql, final int fromIndex)`
- **Summary:** Extracts the next token from a SQL statement starting at the specified index.
- **Contract:**
  - </li> <li> Comment skipping when encountered before any token character (line {@code -- ...} , MySQL hash {@code # ...} , and block {@code /* ...
- **Parameters:**
  - `sql` (`String`) — the SQL statement to extract the token from (must not be {@code null} )
  - `fromIndex` (`int`) — the starting position for extraction (0-based); negative values are treated as {@code 0}
- **Returns:** the next token found, or an empty string if no more tokens exist
##### nextTokenEndIndex(...) -> int
- **Signature:** `public static int nextTokenEndIndex(final String sql, final int fromIndex)`
- **Summary:** Returns the position just past the next token in a SQL statement, scanning from the specified index.
- **Contract:**
  - </p> <p> If no further token exists (only trailing whitespace and/or comments remain), the length of {@code sql} is returned, consistent with {@link #nextToken(String, int)} returning an empty string in that case.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to scan (must not be {@code null} )
  - `fromIndex` (`int`) — the starting position for scanning (0-based); negative values are treated as {@code 0}
- **Returns:** the index immediately after the next token, or the length of {@code sql} if no further token exists
- **See also:** #nextToken(String, int)
##### registerSeparator(...) -> void
- **Signature:** `public static synchronized void registerSeparator(final char separator)`
- **Summary:** Registers a single character as a SQL separator.
- **Parameters:**
  - `separator` (`char`) — the character to register as a separator
- **See also:** #registerSeparator(String), #unregisterSeparator(char), #resetSeparators()
- **Signature:** `public static synchronized void registerSeparator(final String separator)`
- **Summary:** Registers a string as a SQL separator.
- **Contract:**
  - This can be used to register multi-character operators or separators that should be recognized as single tokens during parsing.
  - <p> If the separator is a single character, it will also be registered as a character separator for efficiency.
- **Parameters:**
  - `separator` (`String`) — the string to register as a separator (must not be {@code null} or empty)
- **See also:** #registerSeparator(char), #unregisterSeparator(String), #resetSeparators()
##### unregisterSeparator(...) -> void
- **Signature:** `public static synchronized void unregisterSeparator(final char separator)`
- **Summary:** Unregisters a previously registered separator character, removing it from the recognized separator set.
- **Parameters:**
  - `separator` (`char`) — the character to unregister as a separator
- **See also:** #unregisterSeparator(String)
- **Signature:** `public static synchronized void unregisterSeparator(final String separator)`
- **Summary:** Unregisters a previously registered separator, removing it from the recognized separator set.
- **Contract:**
  - <p> If {@code separator} is a single character, both its {@code String} and character forms are removed (mirroring the dual registration performed by {@link #registerSeparator(String)} for single-character separators).
- **Parameters:**
  - `separator` (`String`) — the separator to unregister (must not be {@code null} or empty)
##### resetSeparators(...) -> void
- **Signature:** `public static synchronized void resetSeparators()`
- **Summary:** Restores the separator set to the built-in defaults, discarding every separator added via {@link #registerSeparator(char)} / {@link #registerSeparator(String)} and re-adding any default separator that was removed via {@link #unregisterSeparator(String)} .
- **Contract:**
  - <p> This rebuilds the internal separator set and all derived lookup tables ( {@link #maxSeparatorLength} , the ASCII lookup and the multi-character tables) to exactly the state established when the class was first loaded.
- **Parameters:**
  - (none)
##### isFunctionName(...) -> boolean
- **Signature:** `public static boolean isFunctionName(final List<String> tokens, final int index)`
- **Summary:** Determines if a token at a specific position in a parsed token list represents a function name.
- **Contract:**
  - Determines if a token at a specific position in a parsed token list represents a function name.
  - A token is considered a function name if it is followed by the opening parenthesis token, either immediately or after whitespace.
- **Parameters:**
  - `tokens` (`List<String>`) — the parsed SQL tokens (typically the result of {@link #parse(String)} )
  - `index` (`int`) — the index of the token to check; invalid indices return {@code false}
- **Returns:** {@code true} if the token at {@code index} is followed (after zero or more space tokens) by the {@code "("} token; {@code false} otherwise
- **Signature:** `@Deprecated public static boolean isFunctionName(final List<String> tokens, final int len, final int index)`
- **Summary:** Determines if a token at a specific position in a parsed token list represents a function name, examining only the tokens below the given exclusive upper bound.
- **Contract:**
  - Determines if a token at a specific position in a parsed token list represents a function name, examining only the tokens below the given exclusive upper bound.
- **Parameters:**
  - `tokens` (`List<String>`) — the parsed SQL tokens (typically the result of {@link #parse(String)} )
  - `len` (`int`) — the exclusive upper bound to search within {@code tokens} (usually {@code tokens.size()} ; indices {@code >= len} are not examined; values above {@code tokens.size()} are capped at {@code tokens.size()} )
  - `index` (`int`) — the index of the token to check; invalid indices return {@code false}
- **Returns:** {@code true} if the token at {@code index} is followed (after zero or more space tokens) by the {@code "("} token; {@code false} otherwise
##### isSelectQuery(...) -> boolean
- **Signature:** `public static boolean isSelectQuery(final String sql)`
- **Summary:** Checks if the given SQL statement is a SELECT query.
- **Contract:**
  - Checks if the given SQL statement is a SELECT query.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or {@code null}
- **Returns:** {@code true} if the SQL is a SELECT query, {@code false} otherwise
- **See also:** #isInsertQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String), #isNoUpdateQuery(String)
##### isReadOnlyQuery(...) -> boolean
- **Signature:** `public static boolean isReadOnlyQuery(final String sql)`
- **Summary:** Checks whether the given SQL statement is read-only, i.e.
- **Contract:**
  - <p> A statement is considered read-only only if its leading keyword is {@code SELECT} (see {@link #isSelectQuery(String)} ) <i> and </i> it contains no top-level mutation or DDL keyword ( {@code INSERT} , {@code UPDATE} , {@code DELETE} , {@code MERGE} , {@code REPLACE} , {@code TRUNCATE} , {@code CREATE} , {@code ALTER} or {@code DROP} ) and no standalone {@code SELECT ...
  - For multi-statement SQL, a later statement that starts with one of the mutation or DDL keywords listed above also makes the SQL non-read-only, including when that later statement starts with a {@code WITH} clause or leading parentheses.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or {@code null}
- **Returns:** {@code true} if the SQL is a read-only SELECT query, {@code false} otherwise
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isNoUpdateQuery(String)
##### isInsertQuery(...) -> boolean
- **Signature:** `public static boolean isInsertQuery(final String sql)`
- **Summary:** Checks if the given SQL statement is an INSERT query.
- **Contract:**
  - Checks if the given SQL statement is an INSERT query.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or {@code null}
- **Returns:** {@code true} if the SQL is an INSERT query, {@code false} otherwise
- **See also:** #isSelectQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String), #isNoUpdateQuery(String)
##### isUpdateQuery(...) -> boolean
- **Signature:** `public static boolean isUpdateQuery(final String sql)`
- **Summary:** Checks if the given SQL statement is an UPDATE query.
- **Contract:**
  - Checks if the given SQL statement is an UPDATE query.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or {@code null}
- **Returns:** {@code true} if the SQL is an UPDATE query, {@code false} otherwise
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String), #isNoUpdateQuery(String)
##### isDeleteQuery(...) -> boolean
- **Signature:** `public static boolean isDeleteQuery(final String sql)`
- **Summary:** Checks if the given SQL statement is a DELETE query.
- **Contract:**
  - Checks if the given SQL statement is a DELETE query.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or {@code null}
- **Returns:** {@code true} if the SQL is a DELETE query, {@code false} otherwise
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isUpdateQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String), #isNoUpdateQuery(String)
##### isInsertOrReplaceQuery(...) -> boolean
- **Signature:** `public static boolean isInsertOrReplaceQuery(final String sql)`
- **Summary:** Checks whether the given SQL statement begins with an {@code INSERT OR REPLACE} clause (the SQLite upsert form that overwrites an existing row when a uniqueness constraint is violated).
- **Contract:**
  - Checks whether the given SQL statement begins with an {@code INSERT OR REPLACE} clause (the SQLite upsert form that overwrites an existing row when a uniqueness constraint is violated).
  - *} {@code /} ), any leading parentheses and any leading {@code WITH} clause; the three keywords {@code INSERT} , {@code OR} and {@code REPLACE} must appear (case-insensitively) in that order at the start of the actual statement.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or {@code null}
- **Returns:** {@code true} if the SQL begins with {@code INSERT OR REPLACE} , {@code false} otherwise
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isReadOnlyQuery(String), #isNoUpdateQuery(String)
##### isNoUpdateQuery(...) -> boolean
- **Signature:** `public static boolean isNoUpdateQuery(final String sql)`
- **Summary:** Checks whether the given SQL statement neither updates nor deletes existing rows.
- **Contract:**
  - <p> A statement qualifies as "no-update" only if its leading keyword is {@code SELECT} or {@code INSERT} <i> and </i> it contains none of the following (matching outside of quoted string literals and SQL comments): </p> <ul> <li> a top-level {@code UPDATE} , {@code DELETE} or {@code MERGE} keyword (matched only at statement-start positions, so e.g.
- **Parameters:**
  - `sql` (`String`) — the SQL statement to check; may be empty or {@code null}
- **Returns:** {@code true} if the SQL neither updates nor deletes existing rows, {@code false} otherwise (including for a {@code null} or empty statement)
- **See also:** #isSelectQuery(String), #isInsertQuery(String), #isUpdateQuery(String), #isDeleteQuery(String), #isInsertOrReplaceQuery(String), #isReadOnlyQuery(String)

#### Public Instance Methods
- (none)

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
- **Returns:** the property name
##### minValue(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T> T minValue()`
- **Summary:** Returns the lower bound of the range.
- **Parameters:**
  - (none)
- **Returns:** the configured minimum value, which may be a literal, a {@link SubQuery} , any other {@link Condition} , or {@code null}
##### maxValue(...) -> T
- **Signature:** `@SuppressWarnings("unchecked") public <T> T maxValue()`
- **Summary:** Returns the upper bound of the range.
- **Parameters:**
  - (none)
- **Returns:** the configured maximum value, which may be a literal, a {@link SubQuery} , any other {@link Condition} , or {@code null}
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameters for this condition.
- **Contract:**
  - If either bound is a {@link Condition} (typically a {@link SubQuery} ), its parameters are spliced in place of the bound itself.
- **Parameters:**
  - (none)
- **Returns:** an immutable list containing {@code \[minValue, maxValue\]} , or their respective parameters spliced in where a bound is itself a {@link Condition}
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this condition to its SQL representation.
- **Contract:**
  - If the operator is {@code null} (only possible for an uninitialized instance), the literal {@code "null"} is rendered in place of the operator.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name; if {@code null} , {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
- **Returns:** a SQL representation of this condition
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name, operator, and range values
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this condition is equal to another object.
- **Contract:**
  - Checks if this condition is equal to another object.
  - Two conditions are equal if they have the same property name, operator, minValue, and maxValue.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

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
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a SQL representation of this condition using the default naming policy.
- **Parameters:**
  - (none)
- **Returns:** a SQL representation of this condition

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
- **Returns:** the (first) property name, or {@code null} for an uninitialized instance
##### propNames(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> propNames()`
- **Summary:** Returns the property names checked in this IN or NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** non-null immutable collection of property names
##### values(...) -> ImmutableList<?>
- **Signature:** `public ImmutableList<?> values()`
- **Summary:** Returns the values used by this IN or NOT IN condition.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of the values (or value tuples), or an empty immutable list for an uninitialized instance
##### usesRowValueConstructor(...) -> boolean
- **Signature:** `public boolean usesRowValueConstructor()`
- **Summary:** Checks whether this condition was created in row value constructor form, i.e.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if this condition renders in row value constructor form, {@code false} for the scalar form
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameter values for this condition, flattened in declaration order.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values, or an empty immutable list for an uninitialized instance (e.g. created via the no-arg constructor for deserialization)
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this condition to its SQL representation.
- **Contract:**
  - If the operator is {@code null} (only possible for an uninitialized instance), the literal {@code "null"} is rendered in place of the operator.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name(s); if {@code null} , {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
- **Returns:** the SQL representation, e.g., {@code "status IN ('active', 'pending')"} or, for a multi-column condition, {@code "(first_name, last_name) IN (('John', 'Doe'), ('Jane', 'Roe'))"}
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this condition.
- **Parameters:**
  - (none)
- **Returns:** the hash code based on property name(s), operator, row-value mode, and values
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this condition is equal to another object.
- **Contract:**
  - Checks if this condition is equal to another object.
  - Two conditions are equal if they have the same property name(s), operator, row-value mode, and values list.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

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
- **Returns:** the (first) property name, or {@code null} for an uninitialized instance
##### propNames(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> propNames()`
- **Summary:** Returns the property names for this IN or NOT IN subquery condition.
- **Parameters:**
  - (none)
- **Returns:** non-null immutable collection of property names
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used in this IN or NOT IN subquery condition.
- **Parameters:**
  - (none)
- **Returns:** the subquery, or {@code null} for an uninitialized instance
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the list of parameters from the subquery.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values from the subquery; an empty immutable list if the subquery is {@code null} (only possible for an uninitialized instance)
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Generates the hash code for this condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on property name(s), operator, and subquery
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this condition is equal to another object.
- **Contract:**
  - Checks if this condition is equal to another object.
  - Two conditions are equal if they have the same property names, operator, and subquery.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this condition to its SQL representation.
- **Contract:**
  - </p> <p> If {@code propNames} is empty (only possible for an uninitialized instance), only {@code OPERATOR (subQuery)} is rendered, and the operator falls back to the literal {@code "null"} when {@code operator} is also {@code null} .
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names; if {@code null} , {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
- **Returns:** the SQL representation of the condition

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
- **Signature:** `public All(final SubQuery subQuery)`
- **Summary:** Creates a new ALL condition with the specified subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery that returns values to compare against (must not be {@code null} )
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this ALL condition.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

### Class And (com.landawn.abacus.query.condition.And)
Represents a composable AND condition that combines multiple conditions.

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
  - `conditions` (`Condition[]`) — the conditions to combine with AND logic; may be {@code null} or empty
- **Signature:** `public And(final Collection<? extends Condition> conditions)`
- **Summary:** Creates a new AND condition with the specified collection of conditions.
- **Contract:**
  - All conditions in the collection must be true for this AND condition to evaluate to true.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Build conditions dynamically List<Condition> conditions = new ArrayList<>(); conditions.add(Filters.equal("status", "active")); conditions.add(Filters.isNotNull("email")); if (includeAgeFilter) { conditions.add(Filters.greaterThan("age", 21)); } And and = new And(conditions); // Results in dynamic AND condition based on the list } </pre>
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with AND logic; may be {@code null} or empty
##### and(...) -> And
- **Signature:** `@Override public And and(final Condition condition)`
- **Summary:** Creates a new AND condition by adding another condition to this AND.
- **Parameters:**
  - `condition` (`Condition`) — the condition to add to this AND. Must not be {@code null} and must be composable (i.e. not a {@link Criteria} , a {@link Clause} , an {@code ON} / {@code USING} connector, an {@code ANY} / {@code ALL} / {@code SOME} quantified-subquery operand, or an empty predicate).
- **Returns:** a new {@link And} condition containing all existing conditions plus the new one

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
- **Signature:** `public Any(final SubQuery subQuery)`
- **Summary:** Creates a new ANY condition with the specified subquery.
- **Contract:**
  - The ANY operator is used in conjunction with comparison operators to test if the comparison is true for any value returned by the subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery that returns values to compare against (must not be {@code null} )
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this ANY condition.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

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
  - <p> <b> &#9888; &#65039; </b> If the property value or either bound is SQL {@code NULL} , SQL three-valued logic makes the predicate unknown rather than true.
  - </p> <p> <b> Usage Example: </b> </p> <pre> {@code // Check if age is between 18 and 65 (inclusive) Between ageRange = new Between("age", 18, 65); // Use with subqueries for a dynamic range SubQuery avgMinus10 = Filters.subQuery("SELECT AVG(score) - 10 FROM scores"); SubQuery avgPlus10 = Filters.subQuery("SELECT AVG(score) + 10 FROM scores"); Between nearAverage = new Between("score", avgMinus10, avgPlus10); // SQL: score BETWEEN (SELECT AVG(score) - 10 FROM scores) AND (SELECT AVG(score) + 10 FROM scores) } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `minValue` (`Object`) — the minimum value (inclusive); may be a literal value, a {@link SubQuery} , or any other {@link Condition} (may be {@code null} )
  - `maxValue` (`Object`) — the maximum value (inclusive); may be a literal value, a {@link SubQuery} , or any other {@link Condition} (may be {@code null} )

### Class Binary (com.landawn.abacus.query.condition.Binary)
Base class for binary conditions that compare a property with a value.

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
  - `propName` (`String`) — the property name to compare (must not be {@code null} , empty, or blank)
  - `operator` (`Operator`) — the comparison operator (must not be {@code null} ); must be an operator valid for a binary {@code propName OP value} condition, i.e. one of {@link Operator#EQUAL} , {@link Operator#NOT_EQUAL} , {@link Operator#NOT_EQUAL_ANSI} , {@link Operator#GREATER_THAN} , {@link Operator#GREATER_THAN_OR_EQUAL} , {@link Operator#LESS_THAN} , {@link Operator#LESS_THAN_OR_EQUAL} , {@link Operator#LIKE} , {@link Operator#NOT_LIKE} , {@link Operator#IS} , {@link Operator#IS_NOT} , {@link Operator#IN} , or {@link Operator#NOT_IN}
  - `propValue` (`Object`) — the value to compare against; may be a literal value, {@code null} (for equality operators, renders as {@code IS NULL} / {@code IS NOT NULL} ), or a {@link Condition} such as a {@link SubQuery} . For an {@code IN} / {@code NOT_IN} operator, a {@link Collection} or array value is copied defensively and must be non-empty.
##### propName(...) -> String
- **Signature:** `public String propName()`
- **Summary:** Returns the property name being compared.
- **Parameters:**
  - (none)
- **Returns:** the property name
##### propValue(...) -> Object
- **Signature:** `public Object propValue()`
- **Summary:** Returns the property value without an unchecked generic cast.
- **Parameters:**
  - (none)
- **Returns:** the property value, which may be {@code null}
- **Signature:** `public <T> T propValue(final Class<T> valueType)`
- **Summary:** Returns the property value cast by the supplied runtime type.
- **Parameters:**
  - `valueType` (`Class<T>`) — the requested value type; must not be {@code null}
- **Returns:** the property value cast to {@code valueType} , or {@code null} when the stored value is {@code null}
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameters for this condition.
- **Contract:**
  - <ul> <li> If the value is {@code null} and the operator is {@code =} , {@code !=} , {@code <>} , {@code IS} , or {@code IS NOT} , an empty list is returned because the SQL is rendered as {@code IS NULL} / {@code IS NOT NULL} with no bind parameter.
  - </li> <li> If the value is {@code null} with any other operator (e.g.
  - </li> <li> If the operator is {@code null} (only possible for an uninitialized instance), an empty list is returned.
  - </li> <li> If the operator is {@code IN} or {@code NOT IN} and the value is a {@link Collection} , each element is added as a parameter; any element that is itself a {@link Condition} has its own parameters spliced in.
  - </li> <li> If the value is a {@link Condition} (e.g., a subquery), the subquery's own parameters are returned.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values; never {@code null}
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this Binary condition to its SQL representation using the specified naming policy.
- **Contract:**
  - When the value is {@code null} and the operator is {@code =} or {@code IS} , the output is {@code propertyName IS NULL} ; when the operator is {@code !=} , {@code <>} , or {@code IS NOT} , the output is {@code propertyName IS NOT NULL} .
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to the property name; if {@code null} , {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
- **Returns:** a SQL representation of this condition
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
  - Two conditions are equal only if they are of the exact same runtime class and have the same property name, operator, and value.
  - The runtime class is part of the equality contract, so an instance of one concrete subclass is never equal to an instance of a different subclass (or to a raw {@code Binary} ), even when their property name, operator, and value all match.
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
##### condition(...) -> Condition
- **Signature:** `public Condition condition()`
- **Summary:** Returns the wrapped condition.
- **Contract:**
  - Callers that need a more specific subtype must cast explicitly.
- **Parameters:**
  - (none)
- **Returns:** the wrapped condition; never {@code null} for instances created via the protected constructor, but may be {@code null} for uninitialized instances produced by the package-private default constructor (e.g., during Kryo deserialization)
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameters from the wrapped condition.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameters from the wrapped condition, or an empty immutable list if no condition is set
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this {@code Cell} condition to its SQL representation using the specified naming policy.
- **Contract:**
  - The output format is {@code OPERATOR condition_string} (separated by a single space), or just {@code OPERATOR} if the wrapped condition is {@code null} .
  - If the operator is {@code null} (only possible for an uninitialized instance), the literal {@code "null"} is rendered in place of the operator.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names within the wrapped condition; if {@code null} , {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
- **Returns:** a SQL representation of this Cell
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
  - Two Cells are equal if they are of the same runtime class and have the same operator and wrapped condition.
  - Different concrete subclasses of {@code Cell} are never equal, even when their operator and wrapped condition are equal.
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
- (none)

### Class ComposableCell (com.landawn.abacus.query.condition.ComposableCell)
A composable variant of {@link Cell} that supports logical composition via AND/OR/NOT operations.

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
- **Returns:** the wrapped condition; never {@code null} for instances created via the protected constructor, but may be {@code null} for uninitialized instances produced by the package-private default constructor (e.g., during Kryo deserialization)
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameters from the wrapped condition.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameters from the wrapped condition, or an empty immutable list if no condition is set
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this {@code ComposableCell} to its SQL representation using the specified naming policy.
- **Contract:**
  - If the operator is {@code null} (only possible for an uninitialized instance), the literal {@code "null"} is rendered in place of the operator.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names within the wrapped condition; if {@code null} , {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
- **Returns:** a SQL representation of this ComposableCell
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this ComposableCell, based on the operator and wrapped condition.
- **Parameters:**
  - (none)
- **Returns:** hash code based on operator and wrapped condition
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this ComposableCell is equal to another object.
- **Contract:**
  - Checks if this ComposableCell is equal to another object.
  - Two ComposableCells are equal if they are of the same runtime class and have the same operator and wrapped condition.
  - Different concrete subclasses of {@code ComposableCell} are never equal, even when their operator and wrapped condition are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise

### Class ComposableCondition (com.landawn.abacus.query.condition.ComposableCondition)
A {@link Condition} that supports logical composition via {@code and()} , {@code or()} , {@code not()} , and {@code xor()} .

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
- **Returns:** a new {@link Not} condition wrapping this condition
##### and(...) -> And
- **Signature:** `public And and(final Condition condition)`
- **Summary:** Creates a new AND condition combining this condition with another.
- **Contract:**
  - Both conditions must be true for the result to be true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to AND with this condition (must not be {@code null} )
- **Returns:** a new {@link And} condition containing both conditions
##### or(...) -> Or
- **Signature:** `public Or or(final Condition condition)`
- **Summary:** Creates a new OR condition combining this condition with another.
- **Contract:**
  - At least one condition must be true for the result to be true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to OR with this condition (must not be {@code null} )
- **Returns:** a new {@link Or} condition containing both conditions
##### xor(...) -> Or
- **Signature:** `public Or xor(final Condition condition)`
- **Summary:** Creates a new XOR (exclusive OR) condition combining this condition with another.
- **Contract:**
  - Exactly one of the two conditions must be true for the result to be true.
- **Parameters:**
  - `condition` (`Condition`) — the condition to XOR with this condition (must not be {@code null} )
- **Returns:** a composable condition representing the exclusive-or {@code (this AND NOT condition) OR (NOT this AND condition)}

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
- **Returns:** the operator for this condition; standard constructed implementations return a non- {@code null} value (a {@code null} is possible only in an uninitialized serialization-framework instance or a non-conforming custom implementation)
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `ImmutableList<Object> parameters()`
- **Summary:** Returns the list of parameter values associated with this condition.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values; never {@code null} (an empty list is returned when there are no parameters)
##### toSql(...) -> String
- **Signature:** `String toSql(NamingPolicy namingPolicy)`
- **Summary:** Returns a SQL representation of this condition using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the policy for formatting property names; a {@code null} naming policy is treated as {@link NamingPolicy#NO_CHANGE} by the standard implementations
- **Returns:** a SQL representation of this condition

### Class Criteria (com.landawn.abacus.query.condition.Criteria)
A container representing a complete SQL query structure composed of multiple clauses ( {@link Join} , {@link Where} , {@link GroupBy} , {@link Having} , {@link OrderBy} , {@link Limit} , and set operations like {@link Union} / {@link UnionAll} / {@link Intersect} / {@link Except} / {@link Minus} ).

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

#### Public Instance Methods
##### selectModifier(...) -> String
- **Signature:** `public String selectModifier()`
- **Summary:** Returns the SELECT modifier (e.g., {@code DISTINCT} , {@code DISTINCTROW} , {@code DISTINCT ON (col1, col2)} , or any custom modifier set via {@link Builder#selectModifier(String)} ), or {@code null} if none was set.
- **Contract:**
  - Returns the SELECT modifier (e.g., {@code DISTINCT} , {@code DISTINCTROW} , {@code DISTINCT ON (col1, col2)} , or any custom modifier set via {@link Builder#selectModifier(String)} ), or {@code null} if none was set.
- **Parameters:**
  - (none)
- **Returns:** the SELECT modifier, or {@code null} if not set
- **See also:** Builder#distinct(), Builder#distinctOn(String), Builder#distinctRow(), Builder#distinctRowBy(String), Builder#selectModifier(String)
##### joins(...) -> ImmutableList<Join>
- **Signature:** `public ImmutableList<Join> joins()`
- **Summary:** Returns all JOIN clauses (JOIN, INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL JOIN, CROSS JOIN, NATURAL JOIN) in the order they were added.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of {@link Join} conditions; empty if none exist
##### where(...) -> Where
- **Signature:** `public Where where()`
- **Summary:** Returns the WHERE clause, or {@code null} if none was set.
- **Contract:**
  - Returns the WHERE clause, or {@code null} if none was set.
- **Parameters:**
  - (none)
- **Returns:** the {@link Where} clause, or {@code null}
##### groupBy(...) -> GroupBy
- **Signature:** `public GroupBy groupBy()`
- **Summary:** Returns the GROUP BY clause, or {@code null} if none was set.
- **Contract:**
  - Returns the GROUP BY clause, or {@code null} if none was set.
- **Parameters:**
  - (none)
- **Returns:** the {@link GroupBy} clause, or {@code null}
##### having(...) -> Having
- **Signature:** `public Having having()`
- **Summary:** Returns the HAVING clause, or {@code null} if none was set.
- **Contract:**
  - Returns the HAVING clause, or {@code null} if none was set.
- **Parameters:**
  - (none)
- **Returns:** the {@link Having} clause, or {@code null}
##### setOperations(...) -> ImmutableList<Clause>
- **Signature:** `public ImmutableList<Clause> setOperations()`
- **Summary:** Returns all set operations (UNION, UNION ALL, INTERSECT, EXCEPT, MINUS) in the order they were added.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of set operation clauses; empty if none exist
##### orderBy(...) -> OrderBy
- **Signature:** `public OrderBy orderBy()`
- **Summary:** Returns the ORDER BY clause, or {@code null} if none was set.
- **Contract:**
  - Returns the ORDER BY clause, or {@code null} if none was set.
- **Parameters:**
  - (none)
- **Returns:** the {@link OrderBy} clause, or {@code null}
##### limit(...) -> Limit
- **Signature:** `public Limit limit()`
- **Summary:** Returns the LIMIT clause, or {@code null} if none was set.
- **Contract:**
  - Returns the LIMIT clause, or {@code null} if none was set.
- **Parameters:**
  - (none)
- **Returns:** the {@link Limit} clause, or {@code null}
##### conditions(...) -> ImmutableList<Condition>
- **Signature:** `public ImmutableList<Condition> conditions()`
- **Summary:** Returns all conditions (clauses) in this criteria in the order they were added.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of all conditions
##### findConditions(...) -> ImmutableList<Condition>
- **Signature:** `public ImmutableList<Condition> findConditions(final Operator operator)`
- **Summary:** Returns all conditions whose {@link Condition#operator()} equals the given operator, in the order they were added.
- **Parameters:**
  - `operator` (`Operator`) — the operator to match (may be {@code null} , in which case this returns an empty list since {@link AbstractCondition} disallows null operators)
- **Returns:** an immutable list of matching conditions; empty if none found
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Collects parameters from all conditions in SQL clause order: JOIN, WHERE, GROUP BY, HAVING, set operations, ORDER BY, LIMIT.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of all parameters collected from the constituent clauses; empty if this criteria has no conditions or if none of the conditions carry parameters
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Returns a SQL representation of this Criteria using the specified naming policy.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names within each clause
- **Returns:** a SQL representation of this Criteria
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns the hash code of this Criteria, based on its select modifier and conditions list.
- **Parameters:**
  - (none)
- **Returns:** hash code based on the select modifier and the ordered conditions list
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks whether this Criteria is equal to another object.
- **Contract:**
  - Two {@code Criteria} instances are equal if they have the same select modifier and the same ordered list of conditions.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise
##### toBuilder(...) -> Builder
- **Signature:** `public Builder toBuilder()`
- **Summary:** Creates a new {@link Builder} pre-populated with this criteria's select modifier and conditions.
- **Parameters:**
  - (none)
- **Returns:** a new mutable Builder initialized from this criteria

### Class Builder (com.landawn.abacus.query.condition.Criteria.Builder)
A mutable builder for constructing {@link Criteria} instances with a fluent API.

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
  - When the criteria is appended to a {@code SqlBuilder} , it is applied to that builder's current {@code SELECT} segment.
- **Parameters:**
  - (none)
- **Returns:** this Builder instance for method chaining
- **See also:** #distinctOn(String)
##### distinctOn(...) -> Builder
- **Signature:** `public Builder distinctOn(final String columnNames)`
- **Summary:** Sets the PostgreSQL-style {@code DISTINCT ON} modifier with specific expressions.
- **Contract:**
  - The database dialect used to execute the generated SQL must support this syntax.
  - If {@code columnNames} is {@code null} , empty, or blank, a plain {@code DISTINCT} modifier is used.
- **Parameters:**
  - `columnNames` (`String`) — the expressions for {@code DISTINCT ON} ; if {@code null} , empty, or blank, plain {@code DISTINCT} is used
- **Returns:** this Builder instance for method chaining
##### distinctRow(...) -> Builder
- **Signature:** `public Builder distinctRow()`
- **Summary:** Sets the DISTINCTROW modifier for the query.
- **Contract:**
  - Like {@link #distinct()} , the modifier is applied to the current {@code SELECT} when this criteria is appended to a {@code SqlBuilder} .
- **Parameters:**
  - (none)
- **Returns:** this Builder instance for method chaining
##### distinctRowBy(...) -> Builder
- **Signature:** `public Builder distinctRowBy(final String columnNames)`
- **Summary:** Sets the DISTINCTROW modifier with specific columns.
- **Contract:**
  - If {@code columnNames} is {@code null} , empty, or blank, a plain {@code DISTINCTROW} modifier (without parentheses) is used.
- **Parameters:**
  - `columnNames` (`String`) — the columns to apply DISTINCTROW to; if {@code null} , empty, or blank, plain {@code DISTINCTROW} is used
- **Returns:** this Builder instance for method chaining
##### selectModifier(...) -> Builder
- **Signature:** `public Builder selectModifier(final String selectModifier)`
- **Summary:** Sets a custom SELECT modifier.
- **Parameters:**
  - `selectModifier` (`String`) — the custom SELECT modifier; {@code null} , empty, or blank means no modifier
- **Returns:** this Builder instance for method chaining
##### join(...) -> Builder
- **Signature:** `public Builder join(final Join... joins)`
- **Summary:** Adds JOIN clauses to this criteria.
- **Parameters:**
  - `joins` (`Join[]`) — the JOIN clauses to add
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder join(final Collection<Join> joins)`
- **Summary:** Adds JOIN clauses to this criteria.
- **Parameters:**
  - `joins` (`Collection<Join>`) — the collection of JOIN clauses to add
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder join(final String joinEntity)`
- **Summary:** Adds a plain JOIN (no explicit type keyword) to this criteria, without an explicit condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder join(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds a plain JOIN (no explicit type keyword) with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder join(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds a plain JOIN (no explicit type keyword) with multiple entities and a condition to this criteria.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
##### innerJoin(...) -> Builder
- **Signature:** `public Builder innerJoin(final String joinEntity)`
- **Summary:** Adds an INNER JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder innerJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds an INNER JOIN with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder innerJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds an INNER JOIN with multiple entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
##### leftJoin(...) -> Builder
- **Signature:** `public Builder leftJoin(final String joinEntity)`
- **Summary:** Adds a LEFT JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder leftJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds a LEFT JOIN with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder leftJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds a LEFT JOIN with multiple entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
##### rightJoin(...) -> Builder
- **Signature:** `public Builder rightJoin(final String joinEntity)`
- **Summary:** Adds a RIGHT JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder rightJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds a RIGHT JOIN with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder rightJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds a RIGHT JOIN with multiple entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
##### fullJoin(...) -> Builder
- **Signature:** `public Builder fullJoin(final String joinEntity)`
- **Summary:** Adds a FULL JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder fullJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Adds a FULL JOIN with a condition to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder fullJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Adds a FULL JOIN with multiple entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables/entities to join
  - `joinCondition` (`Condition`) — the join condition
- **Returns:** this Builder instance for method chaining
##### crossJoin(...) -> Builder
- **Signature:** `public Builder crossJoin(final String joinEntity)`
- **Summary:** Adds a CROSS JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder crossJoin(final Collection<String> joinEntities)`
- **Summary:** Adds a conditionless CROSS JOIN for multiple entities.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the entity/table names to cross join
- **Returns:** this builder
##### naturalJoin(...) -> Builder
- **Signature:** `public Builder naturalJoin(final String joinEntity)`
- **Summary:** Adds a NATURAL JOIN to this criteria.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder naturalJoin(final Collection<String> joinEntities)`
- **Summary:** Adds a conditionless NATURAL JOIN for multiple entities.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the entity/table names to natural join
- **Returns:** this builder
##### where(...) -> Builder
- **Signature:** `public Builder where(final Condition condition)`
- **Summary:** Sets or replaces the WHERE clause.
- **Contract:**
  - If a WHERE clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the WHERE condition (must not be {@code null} ); if its operator is already {@link Operator#WHERE} it is added directly, otherwise it is wrapped in a {@link Where}
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder where(final String expr)`
- **Summary:** Sets or replaces the WHERE clause using a string expression.
- **Contract:**
  - If a WHERE clause already exists, it will be replaced.
- **Parameters:**
  - `expr` (`String`) — the WHERE condition as a string (must not be {@code null} , empty, or blank)
- **Returns:** this Builder instance for method chaining
##### groupByAsc(...) -> Builder
- **Signature:** `public Builder groupByAsc(final String propOrColumnName)`
- **Summary:** Sets or replaces the GROUP BY clause with a single column in ascending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by ascending
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder groupByAsc(final String... propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with ascending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to group by ascending
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder groupByAsc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with ascending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by ascending (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet} to preserve the column order)
- **Returns:** this Builder instance for method chaining
##### groupByDesc(...) -> Builder
- **Signature:** `public Builder groupByDesc(final String propOrColumnName)`
- **Summary:** Sets or replaces the GROUP BY clause with a single column in descending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by descending
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder groupByDesc(final String... propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with descending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to group by descending
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder groupByDesc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with descending order.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by descending (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet} to preserve the column order)
- **Returns:** this Builder instance for method chaining
##### groupBy(...) -> Builder
- **Signature:** `public Builder groupBy(final Condition condition)`
- **Summary:** Sets or replaces the GROUP BY clause.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the GROUP BY condition (must not be {@code null} ); if its operator is already {@link Operator#GROUP_BY} it is added directly, otherwise it is wrapped in a {@link GroupBy}
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder groupBy(final String... propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with property names.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to group by
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder groupBy(final String propName, final SortDirection direction)`
- **Summary:** Sets or replaces the GROUP BY clause with a property and sort direction.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the property name to group by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this Builder instance for method chaining
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
- **Signature:** `public Builder groupBy(final Collection<String> propNames)`
- **Summary:** Sets or replaces the GROUP BY clause with multiple properties.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet} to preserve the column order; must not be {@code null} or empty)
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder groupBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Sets or replaces the GROUP BY clause with multiple properties and sort direction.
- **Contract:**
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet} to preserve the column order)
  - `direction` (`SortDirection`) — the sort direction for all properties
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder groupBy(final Map<String, SortDirection> groupings)`
- **Summary:** Sets or replaces the GROUP BY clause with custom sort directions per property.
- **Contract:**
  - The map should be a LinkedHashMap to preserve order.
  - If a GROUP BY clause already exists, it will be replaced.
- **Parameters:**
  - `groupings` (`Map<String, SortDirection>`) — a map of property names to sort directions
- **Returns:** this Builder instance for method chaining
##### having(...) -> Builder
- **Signature:** `public Builder having(final Condition condition)`
- **Summary:** Sets or replaces the HAVING clause.
- **Contract:**
  - If a HAVING clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the HAVING condition (must not be {@code null} ); if its operator is already {@link Operator#HAVING} it is added directly, otherwise it is wrapped in a {@link Having}
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder having(final String expr)`
- **Summary:** Sets or replaces the HAVING clause using a string expression.
- **Contract:**
  - If a HAVING clause already exists, it will be replaced.
- **Parameters:**
  - `expr` (`String`) — the HAVING condition as a string (must not be {@code null} , empty, or blank)
- **Returns:** this Builder instance for method chaining
##### orderByAsc(...) -> Builder
- **Signature:** `public Builder orderByAsc(final String propOrColumnName)`
- **Summary:** Sets or replaces the ORDER BY clause with a single column in ascending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by ascending
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder orderByAsc(final String... propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with ascending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to order by ascending
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder orderByAsc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with ascending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by ascending (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet} to preserve the column order)
- **Returns:** this Builder instance for method chaining
##### orderByDesc(...) -> Builder
- **Signature:** `public Builder orderByDesc(final String propOrColumnName)`
- **Summary:** Sets or replaces the ORDER BY clause with a single column in descending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to order by descending
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder orderByDesc(final String... propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with descending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to order by descending
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder orderByDesc(final Collection<String> propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with descending order.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by descending (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet} to preserve the column order)
- **Returns:** this Builder instance for method chaining
##### orderBy(...) -> Builder
- **Signature:** `public Builder orderBy(final Condition condition)`
- **Summary:** Sets or replaces the ORDER BY clause.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Condition`) — the ORDER BY condition (must not be {@code null} ); if its operator is already {@link Operator#ORDER_BY} it is added directly, otherwise it is wrapped in an {@link OrderBy}
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder orderBy(final String... propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with property names.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to order by
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder orderBy(final String propName, final SortDirection direction)`
- **Summary:** Sets or replaces the ORDER BY clause with a property and sort direction.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propName` (`String`) — the property name to order by
  - `direction` (`SortDirection`) — the sort direction
- **Returns:** this Builder instance for method chaining
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
- **Signature:** `public Builder orderBy(final Collection<String> propNames)`
- **Summary:** Sets or replaces the ORDER BY clause with multiple properties.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet} to preserve the column order; must not be {@code null} or empty)
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder orderBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Sets or replaces the ORDER BY clause with multiple properties and sort direction.
- **Contract:**
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to order by (use an ordered collection such as {@link List} or {@link java.util.LinkedHashSet} to preserve the column order)
  - `direction` (`SortDirection`) — the sort direction for all properties
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder orderBy(final Map<String, SortDirection> orders)`
- **Summary:** Sets or replaces the ORDER BY clause with custom sort directions per property.
- **Contract:**
  - The map should be a LinkedHashMap to preserve order.
  - If an ORDER BY clause already exists, it will be replaced.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — a map of property names to sort directions
- **Returns:** this Builder instance for method chaining
##### limit(...) -> Builder
- **Signature:** `public Builder limit(final Limit condition)`
- **Summary:** Sets or replaces the LIMIT clause.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `condition` (`Limit`) — the LIMIT condition (must not be {@code null} ); its operator must be {@link Operator#LIMIT} , which is guaranteed for any {@link Limit} instance
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder limit(final int count)`
- **Summary:** Sets or replaces the LIMIT clause with a count.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `count` (`int`) — the maximum number of results to return
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder limit(final int count, final int offset)`
- **Summary:** Sets or replaces the LIMIT clause with count and offset.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
- **Parameters:**
  - `count` (`int`) — the maximum number of results to return
  - `offset` (`int`) — the number of rows to skip
- **Returns:** this Builder instance for method chaining
- **Signature:** `public Builder limit(final String expr)`
- **Summary:** Sets or replaces the LIMIT clause using a string expression.
- **Contract:**
  - If a LIMIT clause already exists, it will be replaced.
  - When rendered by a SQL builder whose dialect paginates with {@code OFFSET} / {@code FETCH} (Oracle, DB2 or SQL Server), a generic {@code LIMIT count \[OFFSET offset\]} expression is re-rendered in that dialect's syntax.
- **Parameters:**
  - `expr` (`String`) — the LIMIT expression as a string
- **Returns:** this Builder instance for method chaining
##### union(...) -> Builder
- **Signature:** `public Builder union(final SubQuery subQuery)`
- **Summary:** Adds a UNION operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with (must not be {@code null} )
- **Returns:** this Builder instance for method chaining
##### unionAll(...) -> Builder
- **Signature:** `public Builder unionAll(final SubQuery subQuery)`
- **Summary:** Adds a UNION ALL operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to union with (must not be {@code null} )
- **Returns:** this Builder instance for method chaining
##### intersect(...) -> Builder
- **Signature:** `public Builder intersect(final SubQuery subQuery)`
- **Summary:** Adds an INTERSECT operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to intersect with (must not be {@code null} )
- **Returns:** this Builder instance for method chaining
##### except(...) -> Builder
- **Signature:** `public Builder except(final SubQuery subQuery)`
- **Summary:** Adds an EXCEPT operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the right-hand subquery for the EXCEPT operation (must not be {@code null} )
- **Returns:** this Builder instance for method chaining
##### minus(...) -> Builder
- **Signature:** `public Builder minus(final SubQuery subQuery)`
- **Summary:** Adds a MINUS operation with a subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the right-hand subquery for the MINUS operation (must not be {@code null} )
- **Returns:** this Builder instance for method chaining
##### add(...) -> Builder
- **Signature:** `public Builder add(final Condition condition)`
- **Summary:** Adds a condition to this builder, routing it to the appropriate clause based on its operator.
- **Parameters:**
  - `condition` (`Condition`) — the condition to add (must not be {@code null} )
- **Returns:** this Builder instance for method chaining
##### build(...) -> Criteria
- **Signature:** `public Criteria build()`
- **Summary:** Builds and returns the Criteria instance from the configured conditions.
- **Parameters:**
  - (none)
- **Returns:** a new Criteria instance

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
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Simple cross join - all combinations CrossJoin join = new CrossJoin("colors"); // SQL: CROSS JOIN colors // If products has 10 rows and colors has 5 rows, result has 50 rows // Cross join with table alias CrossJoin aliasJoin = new CrossJoin("available_sizes s"); // SQL: CROSS JOIN available_sizes s } </pre>
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public CrossJoin(final Collection<String> joinEntities)`
- **Summary:** Creates a CROSS JOIN clause with multiple tables/entities and no join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.

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
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against; may be {@code null} (renders as {@code IS NULL} ), a literal value, or a {@link SubQuery}

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
  - `subQuery` (`SubQuery`) — the subquery to perform the EXCEPT operation with (must not be {@code null} ). The subquery must have the same number of columns with compatible types as the main query.
- **See also:** Minus, Union, UnionAll, Intersect
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this EXCEPT clause.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

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
- **Signature:** `public Exists(final SubQuery subQuery)`
- **Summary:** Creates a new EXISTS condition with the specified subquery.
- **Contract:**
  - The condition evaluates to true if the subquery returns at least one row.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check if employee has any subordinates (correlated subquery) SubQuery subordinatesQuery = Filters.subQuery( "SELECT 1 FROM employees e2 WHERE e2.manager_id = e1.id" ); Exists hasSubordinates = new Exists(subordinatesQuery); hasSubordinates.toString(); // returns "EXISTS (SELECT 1 FROM employees e2 WHERE e2.manager_id = e1.id)" // A null subquery is rejected Exists bad = new Exists((SubQuery) null); // throws IllegalArgumentException } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check for existence of rows (must not be {@code null} )
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this EXISTS condition.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

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
  - `literal` (`String`) — the SQL expression string (must not be {@code null} )
- **Returns:** a cached or newly created Expression instance for the given literal
##### equal(...) -> String
- **Signature:** `public static String equal(final String expr, final Object value)`
- **Summary:** Creates an equality expression between a literal and a value.
- **Contract:**
  - If {@code value} is {@code null} , the result is rendered as {@code "literal IS NULL"} instead of {@code "literal = null"} .
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the equality
  - `value` (`Object`) — the right-hand side value; may be {@code null} (renders as {@code IS NULL} )
- **Returns:** a SQL representation of the equality expression
##### eq(...) -> String
- **Signature:** `@Beta public static String eq(final String expr, final Object value)`
- **Summary:** Creates an equality expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the equality
  - `value` (`Object`) — the right-hand side value; may be {@code null} (renders as {@code IS NULL} )
- **Returns:** a SQL representation of the equality expression
##### notEqual(...) -> String
- **Signature:** `public static String notEqual(final String expr, final Object value)`
- **Summary:** Creates a not-equal expression between a literal and a value.
- **Contract:**
  - If {@code value} is {@code null} , the result is rendered as {@code "literal IS NOT NULL"} instead of {@code "literal != null"} .
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the inequality
  - `value` (`Object`) — the right-hand side value; may be {@code null} (renders as {@code IS NOT NULL} )
- **Returns:** a SQL representation of the not-equal expression
##### ne(...) -> String
- **Signature:** `@Beta public static String ne(final String expr, final Object value)`
- **Summary:** Creates a not-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the inequality
  - `value` (`Object`) — the right-hand side value; may be {@code null} (renders as {@code IS NOT NULL} )
- **Returns:** a SQL representation of the not-equal expression
##### greaterThan(...) -> String
- **Signature:** `public static String greaterThan(final String expr, final Object value)`
- **Summary:** Creates a greater-than expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the greater-than expression
##### gt(...) -> String
- **Signature:** `@Beta public static String gt(final String expr, final Object value)`
- **Summary:** Creates a greater-than expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the greater-than expression
##### greaterThanOrEqual(...) -> String
- **Signature:** `public static String greaterThanOrEqual(final String expr, final Object value)`
- **Summary:** Creates a greater-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the greater-than-or-equal expression
##### ge(...) -> String
- **Signature:** `@Beta public static String ge(final String expr, final Object value)`
- **Summary:** Creates a greater-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the greater-than-or-equal expression
##### lessThan(...) -> String
- **Signature:** `public static String lessThan(final String expr, final Object value)`
- **Summary:** Creates a less-than expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the less-than expression
##### lt(...) -> String
- **Signature:** `@Beta public static String lt(final String expr, final Object value)`
- **Summary:** Creates a less-than expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the less-than expression
##### lessThanOrEqual(...) -> String
- **Signature:** `public static String lessThanOrEqual(final String expr, final Object value)`
- **Summary:** Creates a less-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the less-than-or-equal expression
##### le(...) -> String
- **Signature:** `@Beta public static String le(final String expr, final Object value)`
- **Summary:** Creates a less-than-or-equal expression between a literal and a value.
- **Parameters:**
  - `expr` (`String`) — the left-hand side of the comparison
  - `value` (`Object`) — the right-hand side value; should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the less-than-or-equal expression
##### between(...) -> String
- **Signature:** `public static String between(final String expr, final Object minValue, final Object maxValue)`
- **Summary:** Creates a BETWEEN expression for a literal with min and max values.
- **Parameters:**
  - `expr` (`String`) — the expression to test
  - `minValue` (`Object`) — the minimum value (inclusive)
  - `maxValue` (`Object`) — the maximum value (inclusive)
- **Returns:** a SQL representation of the BETWEEN expression
##### notBetween(...) -> String
- **Signature:** `public static String notBetween(final String expr, final Object minValue, final Object maxValue)`
- **Summary:** Creates a NOT BETWEEN expression for a literal with min and max values.
- **Contract:**
  - A value satisfies {@code NOT BETWEEN min AND max} when it is strictly less than {@code min} or strictly greater than {@code max} , so both ends of the range are excluded.
- **Parameters:**
  - `expr` (`String`) — the expression to test
  - `minValue` (`Object`) — the lower bound of the excluded range (inclusive)
  - `maxValue` (`Object`) — the upper bound of the excluded range (inclusive)
- **Returns:** a SQL representation of the NOT BETWEEN expression
##### like(...) -> String
- **Signature:** `public static String like(final String expr, final String value)`
- **Summary:** Creates a LIKE expression for pattern matching.
- **Parameters:**
  - `expr` (`String`) — the expression to match
  - `value` (`String`) — the pattern to match against (can include % and _ wildcards); should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the LIKE expression
##### notLike(...) -> String
- **Signature:** `public static String notLike(final String expr, final String value)`
- **Summary:** Creates a NOT LIKE expression for pattern matching.
- **Parameters:**
  - `expr` (`String`) — the expression to match
  - `value` (`String`) — the pattern to exclude (can include % and _ wildcards); should not be {@code null} \\u2014 a {@code null} renders as the literal {@code null}
- **Returns:** a SQL representation of the NOT LIKE expression
##### isNull(...) -> String
- **Signature:** `public static String isNull(final String expr)`
- **Summary:** Creates an IS NULL expression for the specified literal.
- **Parameters:**
  - `expr` (`String`) — the expression to check for null
- **Returns:** a SQL representation of the IS NULL expression
##### isNotNull(...) -> String
- **Signature:** `public static String isNotNull(final String expr)`
- **Summary:** Creates an IS NOT NULL expression for the specified literal.
- **Parameters:**
  - `expr` (`String`) — the expression to check for not null
- **Returns:** a SQL representation of the IS NOT NULL expression
##### isNullOrEmpty(...) -> String
- **Signature:** `public static String isNullOrEmpty(final String expr)`
- **Summary:** Creates a framework-specific {@code IS BLANK} expression for the specified literal, which the query engine interprets as a combined null-or-empty check.
- **Parameters:**
  - `expr` (`String`) — the column reference or expression to check
- **Returns:** a framework-specific {@code IS BLANK} expression string
##### isNotNullAndNotEmpty(...) -> String
- **Signature:** `public static String isNotNullAndNotEmpty(final String expr)`
- **Summary:** Creates a framework-specific {@code IS NOT BLANK} expression for the specified literal, which the query engine interprets as a combined not-null-and-not-empty check.
- **Parameters:**
  - `expr` (`String`) — the column reference or expression to check
- **Returns:** a framework-specific {@code IS NOT BLANK} expression string
##### and(...) -> String
- **Signature:** `public static String and(final String... exprs)`
- **Summary:** Creates an AND expression combining multiple literals.
- **Contract:**
  - All conditions must be true for the AND expression to be true.
- **Parameters:**
  - `exprs` (`String[]`) — the expressions to combine with AND
- **Returns:** a SQL representation of the AND expression
##### or(...) -> String
- **Signature:** `public static String or(final String... exprs)`
- **Summary:** Creates an OR expression combining multiple literals.
- **Contract:**
  - At least one condition must be true for the OR expression to be true.
- **Parameters:**
  - `exprs` (`String[]`) — the expressions to combine with OR
- **Returns:** a SQL representation of the OR expression
##### plus(...) -> String
- **Signature:** `public static String plus(final Object... operands)`
- **Summary:** Creates an addition expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values to add
- **Returns:** a SQL representation of the addition expression
##### subtract(...) -> String
- **Signature:** `public static String subtract(final Object... operands)`
- **Summary:** Creates a subtraction expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values to subtract
- **Returns:** a SQL representation of the subtraction expression
##### minus(...) -> String
- **Signature:** `@Deprecated public static String minus(final Object... objects)`
- **Summary:** Creates a subtraction expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values to subtract
- **Returns:** a SQL representation of the subtraction expression
##### multiply(...) -> String
- **Signature:** `public static String multiply(final Object... operands)`
- **Summary:** Creates a multiplication expression for the given objects.
- **Parameters:**
  - `operands` (`Object[]`) — the values to multiply
- **Returns:** a SQL representation of the multiplication expression
##### divide(...) -> String
- **Signature:** `public static String divide(final Object... objects)`
- **Summary:** Creates a division expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values to divide
- **Returns:** a SQL representation of the division expression
##### modulus(...) -> String
- **Signature:** `public static String modulus(final Object... objects)`
- **Summary:** Creates a modulus expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for modulus operation
- **Returns:** a SQL representation of the modulus expression
##### leftShift(...) -> String
- **Signature:** `public static String leftShift(final Object... objects)`
- **Summary:** Creates a left shift expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for left shift operation
- **Returns:** a SQL representation of the left shift expression
##### rightShift(...) -> String
- **Signature:** `public static String rightShift(final Object... objects)`
- **Summary:** Creates a right shift expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for right shift operation
- **Returns:** a SQL representation of the right shift expression
##### bitwiseAnd(...) -> String
- **Signature:** `public static String bitwiseAnd(final Object... objects)`
- **Summary:** Creates a bitwise AND expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for bitwise AND operation
- **Returns:** a SQL representation of the bitwise AND expression
##### bitwiseOr(...) -> String
- **Signature:** `public static String bitwiseOr(final Object... objects)`
- **Summary:** Creates a bitwise OR expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for bitwise OR operation
- **Returns:** a SQL representation of the bitwise OR expression
##### bitwiseXor(...) -> String
- **Signature:** `public static String bitwiseXor(final Object... objects)`
- **Summary:** Creates a bitwise XOR expression for the given objects.
- **Parameters:**
  - `objects` (`Object[]`) — the values for bitwise XOR operation
- **Returns:** a SQL representation of the bitwise XOR expression
##### renderValue(...) -> String
- **Signature:** `public static String renderValue(final Object value)`
- **Summary:** Converts a value to its SQL representation.
- **Contract:**
  - This method performs SQL escaping and formatting: <ul> <li> {@code null} values become the string {@code "null"} </li> <li> Strings are wrapped in single quotes and escaped via {@link AbstractCondition#escapeStringLiteral(String)} : embedded unescaped single and double quotes are backslash-escaped ( {@code '} becomes {@code \\'} , {@code "} becomes {@code \\"} ); a backslash shields the character that follows it, so any existing {@code \\x} pair \\u2014 including an already-escaped quote such as {@code \\'} \\u2014 is copied verbatim rather than escaped again, plus a defensive guard that appends one extra backslash when the body would otherwise end in an unescaped trailing backslash </li> <li> {@link Number} and {@link Boolean} values are converted via {@code toString()} (no quoting); {@code NaN} /infinite {@link Float} / {@link Double} values are rejected </li> <li> {@link Expression} objects return their literal SQL text (or {@code "null"} if the literal is {@code null} ) </li> <li> {@link SubQuery} instances render their {@code toString()} wrapped in parentheses; other {@link Condition} s use their {@code toString()} verbatim </li> <li> Other objects are converted via {@link N#stringOf(Object)} , then quoted and escaped </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code Expression.renderValue("text"); // returns "'text'" Expression.renderValue("O'Brien"); // returns "'O\\'Brien'" (single quote backslash-escaped) Expression.renderValue("say \\"hi\\""); // returns "'say \\"hi\\"'" (double quote backslash-escaped) Expression.renderValue(123); // returns "123" Expression.renderValue(45.67); // returns "45.67" Expression.renderValue(null); // returns "null" Expression.renderValue(true); // returns "true" Expression.renderValue(false); // returns "false" Expression.renderValue(new Expression("COUNT(*)")); // returns "COUNT(*)" (the expression's literal) Expression.renderValue(Double.NaN); // throws IllegalArgumentException } </pre>
- **Parameters:**
  - `value` (`Object`) — the value to render
- **Returns:** the SQL representation of the value
##### count(...) -> String
- **Signature:** `public static String count(final String expr)`
- **Summary:** Creates a COUNT function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to count
- **Returns:** a COUNT function string
##### avg(...) -> String
- **Signature:** `public static String avg(final String expr)`
- **Summary:** Creates an AVG (average) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to average
- **Returns:** an AVG function string
##### sum(...) -> String
- **Signature:** `public static String sum(final String expr)`
- **Summary:** Creates a SUM function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to sum
- **Returns:** a SUM function string
##### min(...) -> String
- **Signature:** `public static String min(final String expr)`
- **Summary:** Creates a MIN function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to find minimum
- **Returns:** a MIN function string
##### max(...) -> String
- **Signature:** `public static String max(final String expr)`
- **Summary:** Creates a MAX function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to find maximum
- **Returns:** a MAX function string
##### abs(...) -> String
- **Signature:** `public static String abs(final String expr)`
- **Summary:** Creates an ABS (absolute value) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to get absolute value of
- **Returns:** an ABS function string
##### acos(...) -> String
- **Signature:** `public static String acos(final String expr)`
- **Summary:** Creates an ACOS (arc cosine) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate arc cosine of
- **Returns:** an ACOS function string
##### asin(...) -> String
- **Signature:** `public static String asin(final String expr)`
- **Summary:** Creates an ASIN (arc sine) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate arc sine of
- **Returns:** an ASIN function string
##### atan(...) -> String
- **Signature:** `public static String atan(final String expr)`
- **Summary:** Creates an ATAN (arc tangent) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate arc tangent of
- **Returns:** an ATAN function string
##### ceil(...) -> String
- **Signature:** `public static String ceil(final String expr)`
- **Summary:** Creates a CEIL (ceiling) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to round up
- **Returns:** a CEIL function string
##### cos(...) -> String
- **Signature:** `public static String cos(final String expr)`
- **Summary:** Creates a COS (cosine) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate cosine of
- **Returns:** a COS function string
##### exp(...) -> String
- **Signature:** `public static String exp(final String expr)`
- **Summary:** Creates an EXP (exponential) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate exponential of
- **Returns:** an EXP function string
##### floor(...) -> String
- **Signature:** `public static String floor(final String expr)`
- **Summary:** Creates a FLOOR function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to round down
- **Returns:** a FLOOR function string
##### log(...) -> String
- **Signature:** `public static String log(final String base, final String value)`
- **Summary:** Creates a LOG function expression with specified base.
- **Parameters:**
  - `base` (`String`) — the logarithm base
  - `value` (`String`) — the value to calculate logarithm of
- **Returns:** a LOG function string
##### ln(...) -> String
- **Signature:** `public static String ln(final String expr)`
- **Summary:** Creates an LN (natural logarithm) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate natural logarithm of
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
- **Signature:** `public static String sign(final String expr)`
- **Summary:** Creates a SIGN function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to get sign of
- **Returns:** a SIGN function string
##### sin(...) -> String
- **Signature:** `public static String sin(final String expr)`
- **Summary:** Creates a SIN (sine) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate sine of
- **Returns:** a SIN function string
##### sqrt(...) -> String
- **Signature:** `public static String sqrt(final String expr)`
- **Summary:** Creates a SQRT (square root) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate square root of
- **Returns:** a SQRT function string
##### tan(...) -> String
- **Signature:** `public static String tan(final String expr)`
- **Summary:** Creates a TAN (tangent) function expression.
- **Parameters:**
  - `expr` (`String`) — the expression to calculate tangent of
- **Returns:** a TAN function string
##### concat(...) -> String
- **Signature:** `public static String concat(final String expr1, final String expr2)`
- **Summary:** Creates a CONCAT function expression that concatenates two operands.
- **Parameters:**
  - `expr1` (`String`) — the first SQL expression (column reference or pre-quoted literal)
  - `expr2` (`String`) — the second SQL expression (column reference or pre-quoted literal)
- **Returns:** a CONCAT function string of the form {@code CONCAT(str1, str2)}
##### replace(...) -> String
- **Signature:** `public static String replace(final String expr, final String oldString, final String replacement)`
- **Summary:** Creates a REPLACE function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to search in
  - `oldString` (`String`) — the string to search for
  - `replacement` (`String`) — the replacement string
- **Returns:** a REPLACE function string
##### length(...) -> String
- **Signature:** `public static String length(final String expr)`
- **Summary:** Creates a LENGTH function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression whose length is returned
- **Returns:** a LENGTH function string
##### substr(...) -> String
- **Signature:** `public static String substr(final String expr, final int fromIndex)`
- **Summary:** Creates a SUBSTR function expression starting from a position.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to extract from
  - `fromIndex` (`int`) — the starting position (1-based)
- **Returns:** a SUBSTR function string
- **Signature:** `public static String substr(final String expr, final int fromIndex, final int length)`
- **Summary:** Creates a SUBSTR function expression with start position and length.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to extract from
  - `fromIndex` (`int`) — the starting position (1-based)
  - `length` (`int`) — the number of characters to extract
- **Returns:** a SUBSTR function string
##### trim(...) -> String
- **Signature:** `public static String trim(final String expr)`
- **Summary:** Creates a TRIM function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to trim
- **Returns:** a TRIM function string
##### ltrim(...) -> String
- **Signature:** `public static String ltrim(final String expr)`
- **Summary:** Creates an LTRIM (left trim) function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to left trim
- **Returns:** an LTRIM function string
##### rtrim(...) -> String
- **Signature:** `public static String rtrim(final String expr)`
- **Summary:** Creates an RTRIM (right trim) function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to right trim
- **Returns:** an RTRIM function string
##### lpad(...) -> String
- **Signature:** `public static String lpad(final String expr, final int length, final String padExpr)`
- **Summary:** Creates an LPAD (left pad) function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to pad
  - `length` (`int`) — the total length after padding
  - `padExpr` (`String`) — the SQL expression to pad with
- **Returns:** an LPAD function string
##### rpad(...) -> String
- **Signature:** `public static String rpad(final String expr, final int length, final String padExpr)`
- **Summary:** Creates an RPAD (right pad) function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to pad
  - `length` (`int`) — the total length after padding
  - `padExpr` (`String`) — the SQL expression to pad with
- **Returns:** an RPAD function string
##### lower(...) -> String
- **Signature:** `public static String lower(final String expr)`
- **Summary:** Creates a LOWER function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to convert to lowercase
- **Returns:** a LOWER function string
##### upper(...) -> String
- **Signature:** `public static String upper(final String expr)`
- **Summary:** Creates an UPPER function expression.
- **Parameters:**
  - `expr` (`String`) — the SQL expression to convert to uppercase
- **Returns:** an UPPER function string

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Expression(final String literal)`
- **Summary:** Constructs a new {@code Expression} with the specified SQL literal.
- **Contract:**
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code Expression expr1 = new Expression("CURRENT_TIMESTAMP"); Expression expr2 = new Expression("price * quantity"); Expression expr3 = new Expression("CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END"); Expression expr4 = new Expression("COALESCE(middle_name, '')"); // expr4.toString() returns: "COALESCE(middle_name, '')" } </pre>
- **Parameters:**
  - `literal` (`String`) — the SQL expression as a string (must not be {@code null} )
##### literal(...) -> String
- **Signature:** `public String literal()`
- **Summary:** Returns the SQL literal string of this expression.
- **Parameters:**
  - (none)
- **Returns:** the SQL expression string; never {@code null} for instances created via the public constructor or {@link #of(String)} , but may be {@code null} for uninitialized instances produced by the package-private default constructor (e.g., during Kryo deserialization)
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns an empty list as expressions have no parameters.
- **Parameters:**
  - (none)
- **Returns:** an empty immutable list
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Returns the string form of this expression, with the naming policy applied to any identifiers (column or property names) that can be detected within the literal.
- **Contract:**
  - Recognized SQL keyword tokens are also left unchanged when written in their canonical upper-case form (for example {@code CURRENT_DATE} ); a lower-case token is treated as an identifier and converted.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to detected identifiers; if {@code null} , {@link NamingPolicy#NO_CHANGE} is used
- **Returns:** the expression string with identifiers converted according to the naming policy
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
  - Two expressions are equal if they are both {@code Expression} instances with the same literal string.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal

### Class FullJoin (com.landawn.abacus.query.condition.FullJoin)
Represents a FULL JOIN (a.k.a.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public FullJoin(final String joinEntity)`
- **Summary:** Creates a FULL JOIN clause for the specified table or entity without a join condition.
- **Contract:**
  - Most databases require an {@code ON} or {@code USING} clause for a FULL JOIN; use {@link #FullJoin(String, Condition)} when a condition is required.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public FullJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a FULL JOIN clause with a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `joinCondition` (`Condition`) — the condition appended after the join target. Use {@link On} (or the deprecated {@link Using} ) when the SQL should include those keywords. A non-empty predicate is allowed; {@code joinCondition} itself may be {@code null} .
- **Signature:** `public FullJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a FULL JOIN clause with multiple tables/entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `joinCondition` (`Condition`) — the condition appended after the joined table list. Use {@link On} (or the deprecated {@link Using} ) when the SQL should include those keywords. A non-empty predicate is allowed; {@code joinCondition} itself may be {@code null} .

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
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check if salary is above 50000 GreaterThan salaryCondition = new GreaterThan("salary", 50000); // Check if temperature exceeds threshold GreaterThan tempCondition = new GreaterThan("temperature", 100); // Check if date is after a specific date GreaterThan dateCondition = new GreaterThan("expiryDate", LocalDate.of(2024, 12, 31)); // Use with subquery - find products priced above average SubQuery avgPrice = Filters.subQuery("SELECT AVG(price) FROM products"); GreaterThan aboveAverage = new GreaterThan("price", avgPrice); // SQL: price > (SELECT AVG(price) FROM products) } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against (a literal value or a {@link SubQuery} ); passing {@code null} renders as {@code prop > null} , which is not a meaningful SQL comparison; do not pass {@code null} to this operator

### Class GreaterThanOrEqual (com.landawn.abacus.query.condition.GreaterThanOrEqual)
Represents a greater-than-or-equal-to ( &gt; =) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public GreaterThanOrEqual(final String propName, final Object propValue)`
- **Summary:** Creates a new GreaterThanOrEqual condition.
- **Contract:**
  - The condition evaluates to true when the property value is greater than or equal to the specified value.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Check if salary is at least 50000 GreaterThanOrEqual salaryCondition = new GreaterThanOrEqual("salary", 50000); // Check if score meets minimum requirement GreaterThanOrEqual scoreCondition = new GreaterThanOrEqual("score", 60); // Check if date is on or after a specific date GreaterThanOrEqual dateCondition = new GreaterThanOrEqual("expiryDate", LocalDate.now()); // Use with subquery - find products priced at or above average SubQuery avgPrice = Filters.subQuery("SELECT AVG(price) FROM products"); GreaterThanOrEqual atOrAboveAverage = new GreaterThanOrEqual("price", avgPrice); // SQL: price >= (SELECT AVG(price) FROM products) } </pre>
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against (a literal value or a {@link SubQuery} ); passing {@code null} renders as {@code prop >= null} , which is not a meaningful SQL comparison; do not pass {@code null} to this operator

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
  - `condition` (`Condition`) — the grouping condition or expression. Must not be {@code null} .
- **See also:** Filters#expr(String)
- **Signature:** `public GroupBy(final String... propNames)`
- **Summary:** Creates a new GROUP BY clause with the specified property names.
- **Parameters:**
  - `propNames` (`String[]`) — the property names to group by, in order. Must not be {@code null} or empty and must not contain {@code null} , empty, or blank elements.
- **Signature:** `public GroupBy(final Collection<String> propNames)`
- **Summary:** Creates a new GROUP BY clause with the property names supplied as a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by, in iteration order. Must not be {@code null} or empty and must not contain {@code null} , empty, or blank elements.
- **Signature:** `public GroupBy(final String propOrColumnName, final SortDirection direction)`
- **Summary:** Creates a new GROUP BY clause with a single property and sort direction.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to group by. Must not be {@code null} , empty, or blank.
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC). Must not be {@code null} .
- **Signature:** `public GroupBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates a new GROUP BY clause with multiple properties and a single sort direction.
- **Contract:**
  - This is useful when you want consistent ordering across all grouping columns.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to group by. Must not be {@code null} or empty and must not contain {@code null} , empty, or blank elements.
  - `direction` (`SortDirection`) — the sort direction to apply to all properties. Must not be {@code null} .
- **Signature:** `public GroupBy(final Map<String, SortDirection> groupings)`
- **Summary:** Creates a new GROUP BY clause with custom sort directions for each property.
- **Contract:**
  - The map should maintain insertion order (use LinkedHashMap) to preserve the generated grouping order.
- **Parameters:**
  - `groupings` (`Map<String, SortDirection>`) — a map of property names to their sort directions. Should be a {@code LinkedHashMap} to maintain order. Must not be {@code null} or empty; keys must not be {@code null} , empty, or blank and values must not be {@code null} .

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
  - `condition` (`Condition`) — the condition to apply in the HAVING clause. Must not be {@code null} .

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
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `values` (`Collection<?>`) — the collection of values to check against (must not be {@code null} or empty); the collection is copied internally to prevent external modifications
- **Signature:** `public In(final Collection<String> propNames, final Collection<?> valueRows)`
- **Summary:** Creates a new row value constructor IN condition.
- **Contract:**
  - Each element of {@code valueRows} must resolve to exactly {@code propNames.size()} values.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the property/column names (must not be {@code null} or empty and must not contain {@code null} , empty, or blank names)
  - `valueRows` (`Collection<?>`) — the collection of value rows (must not be {@code null} or empty); each row must be non- {@code null} and resolve to exactly {@code propNames.size()} values. A row may be a {@link Collection} , {@link Iterable} , object array, {@link Map} or bean

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
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `subQuery` (`SubQuery`) — the subquery that returns the values to check against (must not be {@code null} ); if it is a structured subquery, it must select exactly one column
- **Signature:** `public InSubQuery(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates an IN subquery condition for multiple properties.
- **Contract:**
  - Use this constructor for composite key checks or when multiple columns need to match the subquery results.
  - The subquery must return the same number of columns in the same order.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the property names to check (must not be {@code null} or empty and must not contain {@code null} , empty, or blank elements). Their order must match the column order in the subquery.
  - `subQuery` (`SubQuery`) — the subquery that returns the value combinations to check against (must not be {@code null} ). If it is a structured subquery, it must select exactly {@code propNames.size()} columns.

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
- **Summary:** Creates an INNER JOIN clause for the specified table or entity without a join condition.
- **Contract:**
  - Most databases require an {@code ON} or {@code USING} clause for an INNER JOIN; use {@link #InnerJoin(String, Condition)} when a condition is required.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public InnerJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates an INNER JOIN clause with a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `joinCondition` (`Condition`) — the condition appended after the join target. Use {@link On} (or the deprecated {@link Using} ) when the SQL should include those keywords. A non-empty predicate is allowed; {@code joinCondition} itself may be {@code null} .
- **Signature:** `public InnerJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates an INNER JOIN clause with multiple tables/entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `joinCondition` (`Condition`) — the condition appended after the joined table list. Use {@link On} (or the deprecated {@link Using} ) when the SQL should include those keywords. A non-empty predicate is allowed; {@code joinCondition} itself may be {@code null} .

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
  - `subQuery` (`SubQuery`) — the subquery to perform the INTERSECT operation with (must not be {@code null} ). The subquery must have the same number of columns with compatible types as the main query.
- **See also:** Union, UnionAll, Except, Minus
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this INTERSECT clause.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

### Class Is (com.landawn.abacus.query.condition.Is)
Represents an SQL {@code IS} predicate (e.g.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Is(final String propName, final Object propValue)`
- **Summary:** Creates a new {@code IS} condition with the specified property name and right-hand value.
- **Contract:**
  - <p> If {@code propValue} is the Java {@code null} reference, the generated SQL collapses to {@code propName IS NULL} .
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the right-hand value of the IS predicate; may be {@code null} (renders as {@code IS NULL} ) or an {@link Expression} for a SQL keyword

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
  - This condition generates an {@code IS INFINITE} SQL clause to check if the property's numeric value is infinite (either positive infinity or negative infinity).
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be {@code null} , empty, or blank)

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
  - This condition generates an {@code IS NAN} SQL clause to check if the property's numeric value is NaN (Not a Number), which represents an invalid or undefined mathematical result.
  - As noted above, the predicate itself is also vendor-specific and must be supported by the selected database.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be {@code null} , empty, or blank)

### Class IsNot (com.landawn.abacus.query.condition.IsNot)
Represents an SQL {@code IS NOT} predicate (e.g.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public IsNot(final String propName, final Object propValue)`
- **Summary:** Creates a new {@code IS NOT} condition with the specified property name and right-hand value.
- **Contract:**
  - <p> If {@code propValue} is the Java {@code null} reference, the generated SQL collapses to {@code propName IS NOT NULL} .
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Check for NOT NULL (though IsNotNull is preferred) IsNot notNull = new IsNot("phone_number", null); // SQL: phone_number IS NOT NULL // Check if not a custom value Expression unknownExpr = Filters.expr("UNKNOWN"); IsNot notUnknown = new IsNot("verification_status", unknownExpr); // SQL: verification_status IS NOT UNKNOWN } </pre>
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the right-hand value of the IS NOT predicate; may be {@code null} (renders as {@code IS NOT NULL} ) or an {@link Expression} for a SQL keyword

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
  - This condition generates an {@code IS NOT INFINITE} SQL clause to check if the property's numeric value is NOT infinite (neither positive infinity nor negative infinity).
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be {@code null} , empty, or blank)

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
  - This condition generates an {@code IS NOT NAN} SQL clause to check if the property's numeric value is NOT NaN.
  - It does not exclude infinities; combine it with {@link IsNotInfinite} when finite numeric values are required.
  - As noted above, the predicate itself is also vendor-specific and must be supported by the selected database.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be {@code null} , empty, or blank)

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
  - This condition generates an {@code IS NOT NULL} SQL clause to check if the property value is not NULL, effectively filtering for records that have values in the specified field.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be {@code null} , empty, or blank)

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
  - This condition generates an {@code IS NULL} SQL clause to check if the property value is NULL, which represents the absence of a value in the database.
- **Parameters:**
  - `propName` (`String`) — the name of the property/column to check (must not be {@code null} , empty, or blank)

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
- **Signature:** `public Join(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a JOIN clause with a condition.
- **Contract:**
  - This specifies how the tables are related and which rows should be combined.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `joinCondition` (`Condition`) — the join condition. A plain predicate is rendered with an {@code ON} prefix; an explicit {@link On} or deprecated {@link Using} supplies its own keyword. May be {@code null} .
- **Signature:** `public Join(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a JOIN clause with multiple tables or entities and a condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with
  - `joinCondition` (`Condition`) — the join condition. A plain predicate is rendered with an {@code ON} prefix; an explicit {@link On} or deprecated {@link Using} supplies its own keyword. May be {@code null} .
##### joinEntities(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> joinEntities()`
- **Summary:** Returns the list of tables or entities involved in this join.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of join entities
##### condition(...) -> Condition
- **Signature:** `public Condition condition()`
- **Summary:** Returns the join condition.
- **Contract:**
  - Returns the condition that specifies how the tables are related, or {@code null} if no condition was supplied at construction time.
  - Callers that need a more specific subtype must cast explicitly.
- **Parameters:**
  - (none)
- **Returns:** the join condition, or {@code null} if no condition was specified
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns all parameters from the join condition.
- **Contract:**
  - Returns an empty list if there's no condition or the condition has no parameters.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameters from the condition, or an empty immutable list if no condition
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this JOIN clause to its SQL representation, propagating the specified naming policy to the join condition.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy passed through to the join condition's {@code toString}
- **Returns:** the SQL representation, e.g., "JOIN orders o ON customers.id = o.customer_id"
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
- **Returns:** {@code true} if the object is of the same class with the same operator, entities, and condition

### Class Junction (com.landawn.abacus.query.condition.Junction)
Base class for composable junction conditions that combine multiple conditions.

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
  - `operator` (`Operator`) — the composable operator to use; must be {@link Operator#AND} or {@link Operator#OR}
  - `conditions` (`Condition[]`) — the conditions to combine; may be {@code null} or empty (treated as no conditions)
- **Signature:** `public Junction(final Operator operator, final Collection<? extends Condition> conditions)`
- **Summary:** Creates a new Junction with the specified operator and collection of conditions.
- **Contract:**
  - This constructor is useful when conditions are already collected in a list or set.
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // Create conditions dynamically List<Condition> conditions = new ArrayList<>(); conditions.add(new Equal("status", "active")); conditions.add(new GreaterThan("score", 80)); if (includeDateCheck) { conditions.add(new LessThanOrEqual("date", today)); } Junction junction = new Junction(Operator.AND, conditions); // junction.toString() returns "((status = 'active') AND (score > 80))" // Edge: a null collection is treated as no conditions -> renders as "" Junction empty = new Junction(Operator.OR, (Collection<Condition>) null); // empty.toString() returns "" } </pre>
- **Parameters:**
  - `operator` (`Operator`) — the composable operator to use; must be {@link Operator#AND} or {@link Operator#OR}
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine; may be {@code null} or empty (treated as no conditions)
##### conditions(...) -> ImmutableList<Condition>
- **Signature:** `public ImmutableList<Condition> conditions()`
- **Summary:** Returns the list of conditions contained in this junction.
- **Parameters:**
  - (none)
- **Returns:** an immutable view of the list of conditions in this junction
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns all parameters from all conditions in this junction.
- **Parameters:**
  - (none)
- **Returns:** an immutable list containing all parameters from all conditions
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this junction to its SQL representation according to the specified naming policy.
- **Contract:**
  - Any {@code null} entries in the conditions list are skipped, and an empty string is returned if the junction has no conditions or every condition is {@code null} .
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply to property names within each condition; if {@code null} , {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
- **Returns:** the SQL representation with proper parentheses and spacing, or an empty string if no non- {@code null} conditions are present
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
- **Returns:** {@code true} if the object is of the same class with the same operator and conditions

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
- **Summary:** Creates a LEFT JOIN clause for the specified table or entity without a join condition.
- **Contract:**
  - Most databases require an {@code ON} or {@code USING} clause for a LEFT JOIN; use {@link #LeftJoin(String, Condition)} when a condition is required.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public LeftJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a LEFT JOIN clause with a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `joinCondition` (`Condition`) — the condition appended after the join target. Use {@link On} (or the deprecated {@link Using} ) when the SQL should include those keywords. A non-empty predicate is allowed; {@code joinCondition} itself may be {@code null} .
- **Signature:** `public LeftJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a LEFT JOIN clause with multiple tables/entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `joinCondition` (`Condition`) — the condition appended after the joined table list. Use {@link On} (or the deprecated {@link Using} ) when the SQL should include those keywords. A non-empty predicate is allowed; {@code joinCondition} itself may be {@code null} .

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
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against (a literal value or a {@link SubQuery} ); passing {@code null} renders as {@code prop < null} , which is not a meaningful SQL comparison; do not pass {@code null} to this operator

### Class LessThanOrEqual (com.landawn.abacus.query.condition.LessThanOrEqual)
Represents a less-than-or-equal-to ( &lt; =) comparison condition in SQL-like queries.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public LessThanOrEqual(final String propName, final Object propValue)`
- **Summary:** Creates a new LessThanOrEqual condition.
- **Contract:**
  - This condition checks if the property value is less than or equal to the specified value, providing an inclusive upper bound check.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against (a literal value or a {@link SubQuery} ); passing {@code null} renders as {@code prop <= null} , which is not a meaningful SQL comparison; do not pass {@code null} to this operator

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
  - The pattern should include SQL wildcards ( {@code %} or {@code _} ) for pattern matching.
  - If special characters need to be matched literally, they should be escaped according to your database's escape syntax.
  - // Escape special characters if needed (syntax varies by database) Like escaped = new Like("path", "%\\\\_%"); // To match literal underscore // Note: the query must include an ESCAPE clause (e.g., ESCAPE '\\') for the // escape character to be honored by the database engine.
- **Parameters:**
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the pattern to match (typically a {@link String} containing {@code %} and/or {@code _} wildcards; may also be a {@link SubQuery} ). Use {@code %} to match any sequence of characters and {@code _} to match a single character. Passing {@code null} renders as {@code prop LIKE null} , which is not a meaningful SQL comparison; do not pass {@code null} to this operator.

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
  - `count` (`int`) — the maximum number of rows to return. Must be non-negative.
- **Signature:** `public Limit(final int count, final int offset)`
- **Summary:** Creates a LIMIT clause with both count and offset.
- **Contract:**
  - When {@code offset} is {@code 0} , the rendered SQL omits the {@code OFFSET} clause and produces {@code LIMIT count} only.
- **Parameters:**
  - `count` (`int`) — the maximum number of rows to return. Must be non-negative.
  - `offset` (`int`) — the number of rows to skip before returning results. Must be non-negative.
- **Signature:** `public Limit(final String expr)`
- **Summary:** Creates a LIMIT clause from a string expression, formatting and validating it against a fixed grammar.
- **Contract:**
  - If the expression starts with a digit, {@code '?'} , {@code ':'} , or <code> "#{" </code> , a {@code "LIMIT "} prefix is added automatically; otherwise it is used as-is.
  - </b> When every number slot is an integer literal, {@link #count()} and {@link #offset()} return their concrete values.
  - When a slot is a placeholder (or an integer literal that overflows {@code int} ), the value stays unresolved and opaque: {@link #count()} returns {@link Integer#MAX_VALUE} and {@link #offset()} returns {@code 0} .
  - </p> <p> <b> &#9888; &#65039; </b> When this condition is rendered by a SQL builder, a parsed expression is emitted in the target dialect's pagination syntax from its {@code count} / {@code offset} (so, e.g., MySQL's comma form and the {@code FETCH} forms are re-rendered per dialect).
  - An opaque (placeholder) expression is re-rendered in the dialect's {@code FETCH} syntax only when the dialect paginates with {@code OFFSET} / {@code FETCH} (Oracle, DB2 or SQL Server, per {@link com.landawn.abacus.query.SqlDialect.ProductInfo} ) and it is a generic {@code LIMIT count \[OFFSET offset\]} form; otherwise it is emitted verbatim.
- **Parameters:**
  - `expr` (`String`) — the LIMIT expression as a string. Must not be {@code null} , empty, or blank, and must match one of the accepted forms.
##### literal(...) -> String
- **Signature:** `public String literal()`
- **Summary:** Returns the LIMIT literal string if one was provided.
- **Contract:**
  - Returns the LIMIT literal string if one was provided.
  - This method returns the formatted literal from the string constructor \\u2014 trimmed, with internal whitespace collapsed and SQL keywords upper-cased, and possibly with a {@code "LIMIT "} prefix added (when the input starts with a digit, {@code '?'} , {@code ':'} , or <code> "#{" </code> ) \\u2014 or {@code null} if the Limit was created with count/offset parameters.
  - The literal is retained even when the expression was parsed into concrete {@code count} / {@code offset} values.
- **Parameters:**
  - (none)
- **Returns:** the LIMIT literal string, or {@code null} if constructed with count/offset parameters
##### hasLiteral(...) -> boolean
- **Signature:** `public boolean hasLiteral()`
- **Summary:** Checks whether this Limit was created from a string expression (see {@link #Limit(String)} ).
- **Contract:**
  - When this returns {@code true} , {@link #literal()} is non-null and is what gets rendered; {@link #count()} / {@link #offset()} may hold sentinel values ( {@link Integer#MAX_VALUE} /0) if the expression stayed opaque.
  - When it returns {@code false} , the Limit was created with numeric count/offset parameters and {@link #literal()} returns {@code null} .
- **Parameters:**
  - (none)
- **Returns:** {@code true} if this Limit was constructed from a string expression, {@code false} otherwise
- **See also:** #literal()
##### count(...) -> int
- **Signature:** `public int count()`
- **Summary:** Returns the maximum number of rows to return.
- **Parameters:**
  - (none)
- **Returns:** the row count limit, or {@link Integer#MAX_VALUE} for an opaque (unparsed) string expression
##### offset(...) -> int
- **Signature:** `public int offset()`
- **Summary:** Returns the number of rows to skip before returning results.
- **Parameters:**
  - (none)
- **Returns:** the offset value, or 0 if constructed with only count or with an opaque (unparsed) string expression
##### isResolved(...) -> boolean
- **Signature:** `public boolean isResolved()`
- **Summary:** Returns whether the count and offset are resolved numeric values rather than sentinels for an opaque expression.
- **Parameters:**
  - (none)
- **Returns:** {@code true} for numeric limits and parsed numeric expressions; {@code false} for opaque expressions
##### resolvedCount(...) -> OptionalInt
- **Signature:** `public OptionalInt resolvedCount()`
- **Summary:** Returns the resolved row count when available.
- **Contract:**
  - Returns the resolved row count when available.
- **Parameters:**
  - (none)
- **Returns:** the resolved count, or an empty optional for an opaque expression
##### resolvedOffset(...) -> OptionalInt
- **Signature:** `public OptionalInt resolvedOffset()`
- **Summary:** Returns the resolved row offset when available.
- **Contract:**
  - Returns the resolved row offset when available.
- **Parameters:**
  - (none)
- **Returns:** the resolved offset, or an empty optional for an opaque expression
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the parameters for this LIMIT clause.
- **Contract:**
  - <p> If the expression form ( {@link #Limit(String)} ) contains placeholders ( {@code ?} or named parameters), this class does not track them; the caller is responsible for binding those values separately when preparing the statement.
- **Parameters:**
  - (none)
- **Returns:** an empty immutable list as LIMIT has no parameters
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this LIMIT clause to its SQL representation according to the specified naming policy.
- **Contract:**
  - The output format depends on how the Limit was constructed: <ul> <li> String expression (non-empty {@code literal} ): returns the formatted literal as-is (whitespace collapsed and keywords upper-cased, possibly with {@code "LIMIT "} prepended, per {@link #Limit(String)} ), even when it was parsed into concrete count/offset </li> <li> Uninitialized instance (no expression and {@code null} operator, e.g.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy parameter is currently ignored \\u2014 LIMIT operates on numeric values or a raw expression, not property names
- **Returns:** the SQL representation of this LIMIT clause; {@code "null"} for an uninitialized instance
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Computes the hash code for this LIMIT clause.
- **Contract:**
  - The hash code is calculated based on either the custom expression (if present) or the combination of count and offset values.
- **Parameters:**
  - (none)
- **Returns:** the hash code based on literal if present, otherwise based on count and offset
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Checks if this LIMIT clause is equal to another object.
- **Contract:**
  - Checks if this LIMIT clause is equal to another object.
  - Two Limit instances are considered equal if either: <ul> <li> both have a non-empty custom expression and the expressions are equal, or </li> <li> neither has a custom expression and both have the same count and offset values.
  - </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code new Limit(10, 20).equals(new Limit(10, 20)); // returns true new Limit("10 OFFSET 20").equals(new Limit("10 OFFSET 20")); // returns true // Edge: different count -> not equal new Limit(10).equals(new Limit(20)); // returns false // Edge: the numeric form and the expression form are never equal, // even when they render to the same SQL new Limit(10, 20).equals(new Limit("10 OFFSET 20")); // returns false new Limit(10).equals(null); // returns false } </pre>
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the object is of the same class with the same {@code literal} or matching count/offset values

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
  - `subQuery` (`SubQuery`) — the subquery to perform the MINUS operation with (must not be {@code null} ). The subquery must have the same number of columns with compatible types as the main query.
- **See also:** Except, Union, UnionAll, Intersect
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this MINUS clause.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

### Class NamedProperty (com.landawn.abacus.query.condition.NamedProperty)
A utility class that provides a fluent API for creating SQL conditions based on a property name.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### of(...) -> NamedProperty
- **Signature:** `public static NamedProperty of(final String propName)`
- **Summary:** Returns or atomically creates a cached NamedProperty instance for the specified property name.
- **Parameters:**
  - `propName` (`String`) — the property name. Must not be null, empty, or blank. A {@code null} argument causes an {@code IllegalArgumentException} (null is treated like a blank string by the internal {@code Strings.isBlank} check).
- **Returns:** a cached or new NamedProperty instance

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public NamedProperty(final String propName)`
- **Summary:** Creates a NamedProperty with the specified property name.
- **Contract:**
  - <p> <b> Usage Examples: </b> </p> <pre> {@code NamedProperty age = new NamedProperty("age"); // Use NamedProperty.of("age") when the name comes from a finite application schema } </pre>
- **Parameters:**
  - `propName` (`String`) — the property name; must not be {@code null} , empty, or blank
##### propName(...) -> String
- **Signature:** `public String propName()`
- **Summary:** Returns the property name associated with this NamedProperty.
- **Parameters:**
  - (none)
- **Returns:** the property name
##### equal(...) -> Equal
- **Signature:** `public Equal equal(final Object value)`
- **Summary:** Creates an EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be of any type compatible with the property.
- **Returns:** an Equal condition for this property
- **See also:** Equal, Filters#equal(String, Object)
##### eq(...) -> Equal
- **Signature:** `@Beta public Equal eq(final Object value)`
- **Summary:** Creates an EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** an Equal condition for this property
- **See also:** #equal(Object)
##### equalsAny(...) -> Or
- **Signature:** `public Or equalsAny(final Object... values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property.
- **Parameters:**
  - `values` (`Object[]`) — array of values to check equality against. Each value will be tested with OR logic. Must not be {@code null} or empty.
- **Returns:** an Or condition containing multiple Equal conditions
- **See also:** Or, Equal, com.landawn.abacus.query.Filters#anyEqual(java.util.Map)
- **Signature:** `public Or equalsAny(final int[] values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property using primitive int values.
- **Parameters:**
  - `values` (`int[]`) — primitive int values to check. Must not be {@code null} or empty.
- **Returns:** an Or condition containing multiple Equal conditions
- **See also:** Or, Equal
- **Signature:** `public Or equalsAny(final long[] values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property using primitive long values.
- **Parameters:**
  - `values` (`long[]`) — primitive long values to check. Must not be {@code null} or empty.
- **Returns:** an Or condition containing multiple Equal conditions
- **See also:** Or, Equal
- **Signature:** `public Or equalsAny(final double[] values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property using primitive double values.
- **Parameters:**
  - `values` (`double[]`) — primitive double values to check. Must not be {@code null} or empty.
- **Returns:** an Or condition containing multiple Equal conditions
- **See also:** Or, Equal
- **Signature:** `public Or equalsAny(final Collection<?> values)`
- **Summary:** Creates an OR condition with multiple EQUAL checks for this property using a collection.
- **Contract:**
  - Useful when the values are already in a collection or list.
- **Parameters:**
  - `values` (`Collection<?>`) — collection of values to check equality against. Each value will be tested with OR logic. Must not be {@code null} or empty.
- **Returns:** an Or condition containing multiple Equal conditions
- **See also:** Or, Equal
##### notEqual(...) -> NotEqual
- **Signature:** `public NotEqual notEqual(final Object value)`
- **Summary:** Creates a NOT EQUAL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is not equal to the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be of any type compatible with the property.
- **Returns:** a NotEqual condition for this property
- **See also:** NotEqual, Filters#notEqual(String, Object)
##### ne(...) -> NotEqual
- **Signature:** `@Beta public NotEqual ne(final Object value)`
- **Summary:** Creates a NOT EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a NotEqual condition for this property
- **See also:** #notEqual(Object)
##### greaterThan(...) -> GreaterThan
- **Signature:** `public GreaterThan greaterThan(final Object value)`
- **Summary:** Creates a GREATER THAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is strictly greater than the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a GreaterThan condition for this property
- **See also:** GreaterThan, Filters#greaterThan(String, Object)
##### gt(...) -> GreaterThan
- **Signature:** `@Beta public GreaterThan gt(final Object value)`
- **Summary:** Creates a GREATER THAN condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a GreaterThan condition for this property
- **See also:** #greaterThan(Object)
##### greaterThanOrEqual(...) -> GreaterThanOrEqual
- **Signature:** `public GreaterThanOrEqual greaterThanOrEqual(final Object value)`
- **Summary:** Creates a GREATER THAN OR EQUAL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is greater than or equal to the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a GreaterThanOrEqual condition for this property
- **See also:** GreaterThanOrEqual, Filters#greaterThanOrEqual(String, Object)
##### ge(...) -> GreaterThanOrEqual
- **Signature:** `@Beta public GreaterThanOrEqual ge(final Object value)`
- **Summary:** Creates a GREATER THAN OR EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a GreaterThanOrEqual condition for this property
- **See also:** #greaterThanOrEqual(Object)
##### lessThan(...) -> LessThan
- **Signature:** `public LessThan lessThan(final Object value)`
- **Summary:** Creates a LESS THAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is strictly less than the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a LessThan condition for this property
- **See also:** LessThan, Filters#lessThan(String, Object)
##### lt(...) -> LessThan
- **Signature:** `@Beta public LessThan lt(final Object value)`
- **Summary:** Creates a LESS THAN condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a LessThan condition for this property
- **See also:** #lessThan(Object)
##### lessThanOrEqual(...) -> LessThanOrEqual
- **Signature:** `public LessThanOrEqual lessThanOrEqual(final Object value)`
- **Summary:** Creates a LESS THAN OR EQUAL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is less than or equal to the specified value.
- **Parameters:**
  - `value` (`Object`) — the value to compare against. Can be numeric, date, string, or any comparable type.
- **Returns:** a LessThanOrEqual condition for this property
- **See also:** LessThanOrEqual, Filters#lessThanOrEqual(String, Object)
##### le(...) -> LessThanOrEqual
- **Signature:** `@Beta public LessThanOrEqual le(final Object value)`
- **Summary:** Creates a LESS THAN OR EQUAL condition for this property.
- **Parameters:**
  - `value` (`Object`) — the value to compare against
- **Returns:** a LessThanOrEqual condition for this property
- **See also:** #lessThanOrEqual(Object)
##### isNull(...) -> IsNull
- **Signature:** `public IsNull isNull()`
- **Summary:** Creates an IS NULL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is NULL in the database.
- **Parameters:**
  - (none)
- **Returns:** an IsNull condition for this property
- **See also:** IsNull, Filters#isNull(String)
##### isNullOrEmpty(...) -> Or
- **Signature:** `public Or isNullOrEmpty()`
- **Summary:** Creates a condition matching {@code null} or empty values for this property.
- **Parameters:**
  - (none)
- **Returns:** an OR condition combining null and empty checks
##### isNotNull(...) -> IsNotNull
- **Signature:** `public IsNotNull isNotNull()`
- **Summary:** Creates an IS NOT NULL condition for this property.
- **Contract:**
  - This generates a condition that checks if the property value is not NULL in the database.
- **Parameters:**
  - (none)
- **Returns:** an IsNotNull condition for this property
- **See also:** IsNotNull, Filters#isNotNull(String)
##### isNotNullAndNotEmpty(...) -> And
- **Signature:** `public And isNotNullAndNotEmpty()`
- **Summary:** Creates a condition requiring this property to be both non-null and non-empty.
- **Parameters:**
  - (none)
- **Returns:** an AND condition combining non-null and non-empty checks
##### isNaN(...) -> IsNaN
- **Signature:** `public IsNaN isNaN()`
- **Summary:** Creates an IS NAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the floating-point property value is {@code NaN} (Not-a-Number).
- **Parameters:**
  - (none)
- **Returns:** an IsNaN condition for this property
- **See also:** IsNaN, Filters#isNaN(String)
##### isNotNaN(...) -> IsNotNaN
- **Signature:** `public IsNotNaN isNotNaN()`
- **Summary:** Creates an IS NOT NAN condition for this property.
- **Contract:**
  - This generates a condition that checks if the floating-point property value is not {@code NaN} (Not-a-Number).
- **Parameters:**
  - (none)
- **Returns:** an IsNotNaN condition for this property
- **See also:** IsNotNaN, Filters#isNotNaN(String)
##### isInfinite(...) -> IsInfinite
- **Signature:** `public IsInfinite isInfinite()`
- **Summary:** Creates an IS INFINITE condition for this property.
- **Contract:**
  - This generates a condition that checks if the floating-point property value is infinite.
- **Parameters:**
  - (none)
- **Returns:** an IsInfinite condition for this property
- **See also:** IsInfinite, Filters#isInfinite(String)
##### isNotInfinite(...) -> IsNotInfinite
- **Signature:** `public IsNotInfinite isNotInfinite()`
- **Summary:** Creates an IS NOT INFINITE condition for this property.
- **Contract:**
  - This generates a condition that checks if the floating-point property value is not infinite.
- **Parameters:**
  - (none)
- **Returns:** an IsNotInfinite condition for this property
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
- **See also:** NotBetween, Filters#notBetween(String, Object, Object)
##### like(...) -> Like
- **Signature:** `public Like like(final String value)`
- **Summary:** Creates a LIKE condition for this property.
- **Parameters:**
  - `value` (`String`) — the pattern to match (can include % for any characters and _ for single character). A {@code null} value is permitted (the resulting condition renders the value as {@code null} ).
- **Returns:** a Like condition for this property
- **See also:** Like, Filters#like(String, String)
##### notLike(...) -> NotLike
- **Signature:** `public NotLike notLike(final String value)`
- **Summary:** Creates a NOT LIKE condition for this property.
- **Parameters:**
  - `value` (`String`) — the pattern to exclude (can include % for any characters and _ for single character). A {@code null} value is permitted (the resulting condition renders the value as {@code null} ).
- **Returns:** a NotLike condition for this property
- **See also:** NotLike, Filters#notLike(String, String)
##### startsWith(...) -> Like
- **Signature:** `public Like startsWith(final String value)`
- **Summary:** Creates a LIKE condition that matches values starting with the specified prefix.
- **Parameters:**
  - `value` (`String`) — the prefix to match. The % wildcard will be automatically appended.
- **Returns:** a Like condition with % appended to the value
- **See also:** Like, Filters#startsWith(String, String)
##### notStartsWith(...) -> NotLike
- **Signature:** `public NotLike notStartsWith(final String value)`
- **Summary:** Creates a NOT LIKE condition that excludes values starting with the specified prefix.
- **Parameters:**
  - `value` (`String`) — the prefix to exclude. The % wildcard will be automatically appended.
- **Returns:** a NotLike condition with % appended to the value
- **See also:** NotLike, Filters#notStartsWith(String, String)
##### endsWith(...) -> Like
- **Signature:** `public Like endsWith(final String value)`
- **Summary:** Creates a LIKE condition that matches values ending with the specified suffix.
- **Parameters:**
  - `value` (`String`) — the suffix to match. The % wildcard will be automatically prepended.
- **Returns:** a Like condition with % prepended to the value
- **See also:** Like, Filters#endsWith(String, String)
##### notEndsWith(...) -> NotLike
- **Signature:** `public NotLike notEndsWith(final String value)`
- **Summary:** Creates a NOT LIKE condition that excludes values ending with the specified suffix.
- **Parameters:**
  - `value` (`String`) — the suffix to exclude. The % wildcard will be automatically prepended.
- **Returns:** a NotLike condition with % prepended to the value
- **See also:** NotLike, Filters#notEndsWith(String, String)
##### contains(...) -> Like
- **Signature:** `public Like contains(final String value)`
- **Summary:** Creates a LIKE condition that matches values containing the specified substring.
- **Parameters:**
  - `value` (`String`) — the substring to match. The % wildcard will be automatically added to both sides.
- **Returns:** a Like condition with % on both sides of the value
- **See also:** Like, Filters#contains(String, String)
##### notContains(...) -> NotLike
- **Signature:** `public NotLike notContains(final String value)`
- **Summary:** Creates a NOT LIKE condition that excludes values containing the specified substring.
- **Parameters:**
  - `value` (`String`) — the substring to exclude. The % wildcard will be automatically added to both sides.
- **Returns:** a NotLike condition with % on both sides of the value
- **See also:** NotLike, Filters#notContains(String, String)
##### in(...) -> In
- **Signature:** `public In in(final Object... values)`
- **Summary:** Creates an IN condition for this property with an array of values.
- **Contract:**
  - This generates a condition that checks if the property value matches any of the specified values.
- **Parameters:**
  - `values` (`Object[]`) — array of values to check membership against (must not be {@code null} or empty)
- **Returns:** an In condition for this property
- **See also:** In, Filters#in(String, Object\[\])
- **Signature:** `public In in(final int[] values)`
- **Summary:** Creates an IN condition for this property with primitive int values.
- **Parameters:**
  - `values` (`int[]`) — primitive int values to check membership against. Must not be null or empty.
- **Returns:** an In condition for this property
- **See also:** In, Filters#in(String, int\[\])
- **Signature:** `public In in(final long[] values)`
- **Summary:** Creates an IN condition for this property with primitive long values.
- **Parameters:**
  - `values` (`long[]`) — primitive long values to check membership against. Must not be null or empty.
- **Returns:** an In condition for this property
- **See also:** In, Filters#in(String, long\[\])
- **Signature:** `public In in(final double[] values)`
- **Summary:** Creates an IN condition for this property with primitive double values.
- **Parameters:**
  - `values` (`double[]`) — primitive double values to check membership against. Must not be null or empty.
- **Returns:** an In condition for this property
- **See also:** In, Filters#in(String, double\[\])
- **Signature:** `public In in(final Collection<?> values)`
- **Summary:** Creates an IN condition for this property with a collection of values.
- **Contract:**
  - Useful when the values are already in a collection or list.
- **Parameters:**
  - `values` (`Collection<?>`) — collection of values to check membership against (must not be {@code null} or empty)
- **Returns:** an In condition for this property
- **See also:** In, Filters#in(String, Collection)
- **Signature:** `public InSubQuery in(final SubQuery subQuery)`
- **Summary:** Creates an IN condition for this property with a subquery.
- **Contract:**
  - This generates a condition that checks if the property value is contained in the result set returned by the specified subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check membership against (must not be {@code null} )
- **Returns:** an InSubQuery condition for this property
- **See also:** InSubQuery, Filters#in(String, SubQuery)
##### notIn(...) -> NotIn
- **Signature:** `public NotIn notIn(final Object... values)`
- **Summary:** Creates a NOT IN condition for this property with an array of values.
- **Contract:**
  - This generates a condition that checks if the property value does not match any of the specified values.
- **Parameters:**
  - `values` (`Object[]`) — array of values to check non-membership against (must not be {@code null} or empty)
- **Returns:** a NotIn condition for this property
- **See also:** NotIn, Filters#notIn(String, Object\[\])
- **Signature:** `public NotIn notIn(final int[] values)`
- **Summary:** Creates a NOT IN condition for this property with primitive int values.
- **Parameters:**
  - `values` (`int[]`) — primitive int values to check non-membership against. Must not be null or empty.
- **Returns:** a NotIn condition for this property
- **See also:** NotIn, Filters#notIn(String, int\[\])
- **Signature:** `public NotIn notIn(final long[] values)`
- **Summary:** Creates a NOT IN condition for this property with primitive long values.
- **Parameters:**
  - `values` (`long[]`) — primitive long values to check non-membership against. Must not be null or empty.
- **Returns:** a NotIn condition for this property
- **See also:** NotIn, Filters#notIn(String, long\[\])
- **Signature:** `public NotIn notIn(final double[] values)`
- **Summary:** Creates a NOT IN condition for this property with primitive double values.
- **Parameters:**
  - `values` (`double[]`) — primitive double values to check non-membership against. Must not be null or empty.
- **Returns:** a NotIn condition for this property
- **See also:** NotIn, Filters#notIn(String, double\[\])
- **Signature:** `public NotIn notIn(final Collection<?> values)`
- **Summary:** Creates a NOT IN condition for this property with a collection of values.
- **Contract:**
  - Useful when the values are already in a collection or list.
- **Parameters:**
  - `values` (`Collection<?>`) — collection of values to check non-membership against (must not be {@code null} or empty)
- **Returns:** a NotIn condition for this property
- **See also:** NotIn, Filters#notIn(String, Collection)
- **Signature:** `public NotInSubQuery notIn(final SubQuery subQuery)`
- **Summary:** Creates a NOT IN condition for this property with a subquery.
- **Contract:**
  - This generates a condition that checks if the property value is not contained in the result set returned by the specified subquery.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check non-membership against (must not be {@code null} )
- **Returns:** a NotInSubQuery condition for this property
- **See also:** NotInSubQuery, Filters#notIn(String, SubQuery)
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
- **Summary:** Returns the SQL representation of this NamedProperty.
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
  - <p> <b> Usage Examples: </b> </p> <pre> {@code // If 'orders' and 'customers' both have 'customer_id' column NaturalJoin join = new NaturalJoin("customers"); // SQL: NATURAL JOIN customers // Automatically joins on orders.customer_id = customers.customer_id } </pre>
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public NaturalJoin(final Collection<String> joinEntities)`
- **Summary:** Creates a NATURAL JOIN clause with multiple tables/entities.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.

### Class Not (com.landawn.abacus.query.condition.Not)
Represents a logical NOT condition in SQL queries.

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
  - `condition` (`Condition`) — the condition to be negated; must not be {@code null} . May be any composable condition, including simple comparisons, logical junctions ( {@link And} , {@link Or} ), or subquery conditions. It should not be a clause condition (such as {@link Where} or {@link Having} ), since those are not meant to be composed.

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
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `minValue` (`Object`) — the lower bound of the range to exclude; may be a literal value, a {@link SubQuery} , or any other {@link Condition} (may be {@code null} )
  - `maxValue` (`Object`) — the upper bound of the range to exclude; may be a literal value, a {@link SubQuery} , or any other {@link Condition} (may be {@code null} )

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
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the value to compare against; may be {@code null} (renders as {@code IS NOT NULL} ), a literal value, or a {@link SubQuery}

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
- **Signature:** `public NotExists(final SubQuery subQuery)`
- **Summary:** Creates a new NOT EXISTS condition with the specified subquery.
- **Contract:**
  - The condition evaluates to true when the subquery returns no rows.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to check for non-existence of rows (must not be {@code null} )
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this NOT EXISTS condition.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

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
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `values` (`Collection<?>`) — the collection of values that the property should NOT match (must not be {@code null} or empty); the collection is copied internally to ensure immutability
- **Signature:** `public NotIn(final Collection<String> propNames, final Collection<?> valueRows)`
- **Summary:** Creates a new row value constructor NOT IN condition.
- **Contract:**
  - Each element of {@code valueRows} must resolve to exactly {@code propNames.size()} values.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the property/column names (must not be {@code null} or empty and must not contain {@code null} , empty, or blank names)
  - `valueRows` (`Collection<?>`) — the collection of value rows (must not be {@code null} or empty); each row must be non- {@code null} and resolve to exactly {@code propNames.size()} values. A row may be a {@link Collection} , {@link Iterable} , object array, {@link Map} or bean

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
- **Parameters:**
  - `propName` (`String`) — the property/column name to check against the subquery results (must not be {@code null} , empty, or blank)
  - `subQuery` (`SubQuery`) — the subquery that returns the values to check against (must not be {@code null} ); if it is a structured subquery, it must select exactly one column
- **Signature:** `public NotInSubQuery(final Collection<String> propNames, final SubQuery subQuery)`
- **Summary:** Creates a NOT IN subquery condition for multiple properties.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property names to check against the subquery results (must not be {@code null} or empty, and no element may be {@code null} , empty, or blank). Their order must match the column order in the subquery.
  - `subQuery` (`SubQuery`) — the subquery that returns the values to check against (must not be {@code null} ). If it is structured, it must select exactly {@code propNames.size()} columns.

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
  - `propName` (`String`) — the property/column name (must not be {@code null} , empty, or blank)
  - `propValue` (`Object`) — the pattern to match against (typically a {@link String} containing {@code %} and/or {@code _} wildcards; may also be a {@link SubQuery} ). Use {@code %} to match any sequence of characters and {@code _} to match a single character. Passing {@code null} renders as {@code prop NOT LIKE null} , which is not a meaningful SQL comparison; do not pass {@code null} to this operator.

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
  - `condition` (`Condition`) — the join condition. Any non-clause, non- {@code null} predicate is allowed, including {@link Expression} , {@link Equal} , {@link And} , {@link Or} , or {@link Between} . Must not be {@code null} .
- **Signature:** `public On(final String leftPropName, final String rightPropName)`
- **Summary:** Creates an ON clause for simple column equality between tables.
- **Parameters:**
  - `leftPropName` (`String`) — the column name from the first table (can include table name/alias)
  - `rightPropName` (`String`) — the column name from the second table (can include table name/alias). Treated as a column expression rather than a string literal.
- **Signature:** `public On(final Map<String, String> propNamePairs)`
- **Summary:** Creates an ON clause with multiple column equality conditions.
- **Contract:**
  - This is useful for composite key joins or when multiple columns must match between tables.
- **Parameters:**
  - `propNamePairs` (`Map<String, String>`) — map of column pairs where the key is from the first table and the value is from the second table. Order is preserved if a {@code LinkedHashMap} is used.

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
  - `name` (`String`) — the SQL representation of the operator. May be {@code null} .
- **Returns:** the corresponding Operator enum value, or {@code null} if {@code name} is {@code null} or not a known token/name

#### Public Instance Methods
##### sqlToken(...) -> String
- **Signature:** `public String sqlToken()`
- **Summary:** Returns the SQL SQL representation of this operator.
- **Parameters:**
  - (none)
- **Returns:** the SQL SQL representation of this operator (e.g., "=", "AND", "LIKE")
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns the SQL SQL representation of this operator.
- **Parameters:**
  - (none)
- **Returns:** the SQL SQL representation of this operator (e.g., "=", "AND", "LIKE")

### Class Or (com.landawn.abacus.query.condition.Or)
Represents a composable OR condition that combines multiple conditions.

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
- **Contract:**
  - <p> The OR expression is true if any single child condition is true.
- **Parameters:**
  - `conditions` (`Condition[]`) — the conditions to combine with OR logic; may be {@code null} or empty
- **Signature:** `public Or(final Collection<? extends Condition> conditions)`
- **Summary:** Creates a new OR condition with a collection of conditions.
- **Contract:**
  - This constructor is useful when conditions are built dynamically.
- **Parameters:**
  - `conditions` (`Collection<? extends Condition>`) — the collection of conditions to combine with OR logic; may be {@code null} or empty
##### or(...) -> Or
- **Signature:** `@Override public Or or(final Condition condition)`
- **Summary:** Creates a new Or condition by adding another condition to this OR.
- **Contract:**
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Build condition step by step Or or = new Or(Filters.equal("type", "A")) .or(Filters.equal("type", "B")) .or(Filters.equal("type", "C")); // SQL: ((type = 'A') OR (type = 'B') OR (type = 'C')) // Add conditions conditionally Or baseOr = new Or(Filters.equal("status", "active")); if (includeInactive) { baseOr = baseOr.or(Filters.equal("status", "inactive")); } if (includePending) { baseOr = baseOr.or(Filters.equal("status", "pending")); } // Results vary based on flags } </pre>
- **Parameters:**
  - `condition` (`Condition`) — the condition to add to this OR. Must not be {@code null} and must be composable (i.e. not a {@link Criteria} , a {@link Clause} , an {@code ON} / {@code USING} connector, an {@code ANY} / {@code ALL} / {@code SOME} quantified-subquery operand, or an empty predicate).
- **Returns:** a new {@link Or} condition containing all existing conditions plus the new one

### Class OrderBy (com.landawn.abacus.query.condition.OrderBy)
Represents an ORDER BY clause in SQL queries, used to specify the sort order of query results.

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
  - `condition` (`Condition`) — the ordering condition. Must not be {@code null} .
- **See also:** Filters#expr(String)
- **Signature:** `public OrderBy(final String... propNames)`
- **Summary:** Creates an ORDER BY clause with multiple property names.
- **Parameters:**
  - `propNames` (`String[]`) — variable number of property names to sort by. Must not be {@code null} or empty and must not contain {@code null} , empty, or blank elements.
- **Signature:** `public OrderBy(final Collection<String> propNames)`
- **Summary:** Creates an ORDER BY clause with multiple property names supplied as a collection.
- **Parameters:**
  - `propNames` (`Collection<String>`) — the collection of property names to sort by, in iteration order. Must not be {@code null} or empty and must not contain {@code null} , empty, or blank elements.
- **Signature:** `public OrderBy(final String propOrColumnName, final SortDirection direction)`
- **Summary:** Creates an ORDER BY clause with a single property and sort direction.
- **Parameters:**
  - `propOrColumnName` (`String`) — the property or column name to sort by. Must not be {@code null} , empty, or blank.
  - `direction` (`SortDirection`) — the sort direction (ASC or DESC). Must not be {@code null} .
- **Signature:** `public OrderBy(final Collection<String> propNames, final SortDirection direction)`
- **Summary:** Creates an ORDER BY clause with multiple properties and a single sort direction.
- **Contract:**
  - <p> This is useful when you want to sort by multiple columns in the same direction, such as sorting multiple date fields in descending order.
- **Parameters:**
  - `propNames` (`Collection<String>`) — collection of property names to sort by. Must not be {@code null} or empty and must not contain {@code null} , empty, or blank elements.
  - `direction` (`SortDirection`) — the sort direction to apply to all properties. Must not be {@code null} .
- **Signature:** `public OrderBy(final Map<String, SortDirection> orders)`
- **Summary:** Creates an ORDER BY clause with properties having different sort directions.
- **Contract:**
  - <p> The map should maintain insertion order (use LinkedHashMap) to ensure predictable sort priority.
- **Parameters:**
  - `orders` (`Map<String, SortDirection>`) — a map of property names to their respective sort directions; should be a {@code LinkedHashMap} (or another order-preserving map) so that the sort priority matches insertion order. Must not be {@code null} or empty; keys must not be {@code null} , empty, or blank and values must not be {@code null} .

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
- **Summary:** Creates a RIGHT JOIN clause for the specified table or entity without a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias (e.g., "orders o").
- **Signature:** `public RightJoin(final String joinEntity, final Condition joinCondition)`
- **Summary:** Creates a RIGHT JOIN clause with a join condition.
- **Parameters:**
  - `joinEntity` (`String`) — the table or entity to join with. Can include alias.
  - `joinCondition` (`Condition`) — the condition appended after the join target. Use {@link On} (or the deprecated {@link Using} ) when the SQL should include those keywords. Any non-clause {@link Condition} is allowed and can be {@code null} .
- **Signature:** `public RightJoin(final Collection<String> joinEntities, final Condition joinCondition)`
- **Summary:** Creates a RIGHT JOIN clause with multiple tables/entities and a join condition.
- **Parameters:**
  - `joinEntities` (`Collection<String>`) — the collection of tables or entities to join with.
  - `joinCondition` (`Condition`) — the condition appended after the joined table list. Use {@link On} (or the deprecated {@link Using} ) when the SQL should include those keywords. Any non-clause {@link Condition} is allowed and can be {@code null} .

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
- **Signature:** `public Some(final SubQuery subQuery)`
- **Summary:** Creates a new SOME condition with the specified subquery.
- **Contract:**
  - The SOME operator must be used with a comparison operator in the containing condition.
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery that returns values to compare against (must not be {@code null} )
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this SOME condition.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

### Class SubQuery (com.landawn.abacus.query.condition.SubQuery)
Represents raw query-expression text or a structured SELECT used within SQL conditions.

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
  - `sql` (`String`) — complete raw query-expression text (must not be {@code null} , empty, or blank)
- **Signature:** `@Deprecated public SubQuery(final String entityName, final String sql)`
- **Summary:** Creates a subquery with an entity name and raw SQL.
- **Contract:**
  - The entity name is for reference only when using raw SQL and doesn't affect the query.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name; may be {@code null} or empty, in which case it is stored as the empty string
  - `sql` (`String`) — complete raw query-expression text (must not be {@code null} , empty, or blank)
- **Signature:** `public SubQuery(final String entityName, final String propName, final Condition condition)`
- **Summary:** Creates a structured single-property subquery for an entity name.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name
  - `propName` (`String`) — the property to select
  - `condition` (`Condition`) — the optional query condition
- **Signature:** `public SubQuery(final String entityName, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a structured subquery with entity name, selected properties, and condition.
- **Contract:**
  - If the condition is not already a {@link Criteria} or a clause (like WHERE), it will be automatically wrapped in a WHERE clause.
- **Parameters:**
  - `entityName` (`String`) — the entity/table name (must not be {@code null} , empty, or blank)
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be {@code null} or empty and must not contain {@code null} , empty, or blank names)
  - `condition` (`Condition`) — the WHERE condition (if it's not already a {@link Criteria} or a clause, it will be wrapped in WHERE). May be {@code null} to select without a WHERE clause.
- **Signature:** `public SubQuery(final Class<?> entityClass, final String propName, final Condition condition)`
- **Summary:** Creates a structured single-property subquery for an entity class.
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class
  - `propName` (`String`) — the property to select
  - `condition` (`Condition`) — the optional query condition
- **Signature:** `public SubQuery(final Class<?> entityClass, final Collection<String> propNames, final Condition condition)`
- **Summary:** Creates a structured subquery with entity class, selected properties, and condition.
- **Contract:**
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Type-safe subquery construction SubQuery subQuery = Filters.subQuery(Product.class, Arrays.asList("id", "categoryId"), Filters.like("name", "%electronics%") ); // SQL: SELECT id, categoryId FROM <Product-table-name> WHERE name LIKE '%electronics%' // (the FROM target is resolved from the entity class via QueryUtil \\u2014 for a class without // table-mapping annotations it falls back to the class's simple name "Product"; column-name // mapping for the selected properties is also applied when available) // With complex conditions SubQuery activeProducts = Filters.subQuery(Product.class, Arrays.asList("id", "name", "price"), Filters.and( Filters.equal("active", true), Filters.between("price", 10, 100) ) ); } </pre>
- **Parameters:**
  - `entityClass` (`Class<?>`) — the entity class (must not be {@code null} )
  - `propNames` (`Collection<String>`) — collection of property names to select (must not be {@code null} or empty and must not contain {@code null} , empty, or blank names)
  - `condition` (`Condition`) — the WHERE condition (if it's not already a {@link Criteria} or a clause, it will be wrapped in WHERE). May be {@code null} to select without a WHERE clause.
##### rawSql(...) -> String
- **Signature:** `public String rawSql()`
- **Summary:** Returns the raw SQL script if this is a raw SQL subquery.
- **Contract:**
  - Returns the raw SQL script if this is a raw SQL subquery.
- **Parameters:**
  - (none)
- **Returns:** the SQL script, or {@code null} if this is a structured subquery
##### entityName(...) -> String
- **Signature:** `public String entityName()`
- **Summary:** Returns the entity/table name for this subquery.
- **Parameters:**
  - (none)
- **Returns:** the entity/table name, or an empty string if not set
##### entityClass(...) -> Class<?>
- **Signature:** `public Class<?> entityClass()`
- **Summary:** Returns the entity class if this subquery was created with a class reference.
- **Contract:**
  - Returns the entity class if this subquery was created with a class reference.
- **Parameters:**
  - (none)
- **Returns:** the entity class, or {@code null} if created with an entity name string or raw SQL
##### selectPropNames(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> selectPropNames()`
- **Summary:** Returns the collection of property names to select in this subquery.
- **Parameters:**
  - (none)
- **Returns:** immutable list of property names to select, or {@code null} for raw SQL subqueries
##### condition(...) -> Condition
- **Signature:** `public Condition condition()`
- **Summary:** Returns the WHERE condition for this subquery.
- **Contract:**
  - This condition is applied when generating the SQL for structured subqueries.
- **Parameters:**
  - (none)
- **Returns:** the WHERE condition, or {@code null} if no condition or raw SQL subquery
##### parameters(...) -> ImmutableList<Object>
- **Signature:** `@Override public ImmutableList<Object> parameters()`
- **Summary:** Returns the list of parameter values from the condition.
- **Contract:**
  - These are the parameter values that will be bound to the prepared statement placeholders when the query is executed.
  - <p> <b> &#9888; &#65039; </b> For raw SQL subqueries this returns an empty list even when the raw text contains placeholders; raw bindings are managed by the caller.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of parameter values, or an empty immutable list if no condition or raw SQL subquery
##### toSql(...) -> String
- **Signature:** `@Override public String toSql(final NamingPolicy namingPolicy)`
- **Summary:** Converts this subquery to its SQL representation.
- **Parameters:**
  - `namingPolicy` (`NamingPolicy`) — the naming policy to apply; if {@code null} , {@link com.landawn.abacus.util.NamingPolicy#NO_CHANGE} is used
- **Returns:** SQL representation of the subquery
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
  - Two subqueries are equal only when all of their identity fields are equal: the entity name, the entity class, the selected properties, the raw SQL string, and the condition.
  - The entity name participates even for raw-SQL subqueries, so two raw subqueries are equal only when both their SQL and their entity name match.
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
  - `subQuery` (`SubQuery`) — the subquery to perform the UNION operation with (must not be {@code null} ). The subquery must have the same number of columns with compatible types as the main query.
- **See also:** UnionAll, Intersect, Except, Minus
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this UNION clause.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

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
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code // Combine orders from multiple regions SubQuery eastOrders = Filters.subQuery("SELECT order_id, amount FROM orders WHERE region = 'EAST'"); UnionAll allOrders = new UnionAll(eastOrders); // When combined with West region query: // SELECT order_id, amount FROM orders WHERE region = 'WEST' // UNION ALL // SELECT order_id, amount FROM orders WHERE region = 'EAST' // Keeps all orders, including any duplicates // Merge current and archived transactions SubQuery archivedTxns = Filters.subQuery("SELECT txn_id, date, amount FROM archived_transactions"); UnionAll allTxns = new UnionAll(archivedTxns); // Combines with current transactions, preserving all records // Combine data from partitioned tables SubQuery q2Data = Filters.subQuery("SELECT * FROM sales_q2"); UnionAll allSales = new UnionAll(q2Data); // When combined with a "SELECT * FROM sales_q1" query, efficiently combines quarterly data without duplicate check } </pre>
- **Parameters:**
  - `subQuery` (`SubQuery`) — the subquery to perform the UNION ALL operation with (must not be {@code null} ). The subquery must have the same number of columns with compatible types as the main query.
- **See also:** Union, Intersect, Except, Minus
##### subQuery(...) -> SubQuery
- **Signature:** `public SubQuery subQuery()`
- **Summary:** Returns the subquery used by this UNION ALL clause.
- **Parameters:**
  - (none)
- **Returns:** the {@link SubQuery} supplied at construction time

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
- **Signature:** `@Deprecated public Using(final String... columnNames)`
- **Summary:** Creates a USING clause with the specified column names.
- **Contract:**
  - The columns must exist with identical names in both tables being joined.
- **Parameters:**
  - `columnNames` (`String[]`) — variable number of column names to join on. All columns must exist in both tables with identical names. Must not be {@code null} or empty, and individual names must not be {@code null} , empty, or blank. Names must be unqualified (cannot contain a {@code .} ) and must each be a single column name (cannot contain {@code ,} , {@code (} , or {@code )} ).
- **Signature:** `@Deprecated public Using(final Collection<String> columnNames)`
- **Summary:** Creates a USING clause with a collection of column names.
- **Contract:**
  - This constructor is useful when column names are determined dynamically or retrieved from metadata/configuration.
- **Parameters:**
  - `columnNames` (`Collection<String>`) — collection of column names to join on. Must not be {@code null} or empty, and individual names must not be {@code null} , empty, or blank. Names must be unqualified (cannot contain a {@code .} ) and must each be a single column name (cannot contain {@code ,} , {@code (} , or {@code )} ). The collection is read once and snapshotted. Order matters for some databases; use a {@code LinkedHashSet} or {@code List} to preserve insertion order.
##### columnNames(...) -> ImmutableList<String>
- **Signature:** `public ImmutableList<String> columnNames()`
- **Summary:** Returns the validated column names this USING clause joins on, in the order they were supplied.
- **Parameters:**
  - (none)
- **Returns:** an immutable list of the unqualified column names in supplied order, or an empty immutable list for an uninitialized instance produced by the package-private default constructor

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
  - `condition` (`Condition`) — the condition to apply in the WHERE clause. Must not be {@code null} .
