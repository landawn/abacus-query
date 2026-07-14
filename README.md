# abacus-query

[![Maven Central](https://img.shields.io/maven-central/v/com.landawn.abacus/abacus-query.svg)](https://central.sonatype.com/artifact/com.landawn.abacus/abacus-query)
[![Javadocs](https://img.shields.io/badge/javadoc-4.8.8-brightgreen.svg)](https://www.javadoc.io/doc/com.landawn.abacus/abacus-query/4.8.8/index.html)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)

A lightweight, dependency-light **Java 17+ SQL toolkit** — build, compose, and parse SQL without an ORM.

`abacus-query` produces plain SQL strings plus an ordered parameter list. Hand the result to
JDBC, Spring `JdbcTemplate`, MyBatis, or [abacus-jdbc](https://github.com/landawn/abacus-jdbc) —
you stay in control of execution, transactions, and mapping.

```java
import static com.landawn.abacus.query.Dsl.PSC;
import com.landawn.abacus.query.Filters;

var sp = PSC.select("firstName", "lastName")
            .from("users")
            .where(Filters.eq("department", "Engineering"))
            .orderBy("lastName")
            .build();

sp.query();       // SELECT first_name, last_name FROM users WHERE department = ? ORDER BY last_name
sp.parameters();  // ["Engineering"]
```

---

## Why abacus-query?

- **No ORM, no magic** — it emits SQL text and parameters; you decide how it runs.
- **Type-safe fluent builders** for `SELECT` / `INSERT` / `UPDATE` / `DELETE`, driven by either column
  names or annotated bean classes.
- **Pluggable parameter styles** (`?`, `:name`, `#{name}`, or inlined literals) and **naming policies**
  (no-change, `snake_case`, `UPPER_CASE`, `camelCase`) — pick one preset and the builder handles the rest.
- **A composable condition model** — `WHERE`, `JOIN`, `GROUP BY`, `HAVING`, `ORDER BY`, subqueries, and
  set operations expressed as immutable `Condition` objects you can build, combine, and reuse.
- **SQL parsing utilities** — a quoting/comment-aware tokenizer, a named-parameter parser that rewrites
  `:name` / `#{name}` into JDBC `?`, and an XML-backed store for externalizing SQL.
- **Small footprint** — only [abacus-common](https://github.com/landawn/abacus-common) is required at
  runtime; Lombok and the Jakarta XML API are `provided`-scope, compile-time only.

---

## Installation

Requires **JDK 17 or above**.

**Maven**

```xml
<dependency>
    <groupId>com.landawn.abacus</groupId>
    <artifactId>abacus-query</artifactId>
    <version>4.8.8</version>
</dependency>
```

**Gradle**

```groovy
implementation 'com.landawn.abacus:abacus-query:4.8.8'
```

---

## Quick start

Each entry point in [`Dsl`](src/main/java/com/landawn/abacus/query/Dsl.java) is a preset bound to one
parameter style + naming policy. Static-import the presets you use, then chain builder methods and call
`build()` to get an `SP` (SQL + Parameters) record.

```java
import static com.landawn.abacus.query.Dsl.PSC;   // ? placeholders, snake_case columns
import static com.landawn.abacus.query.Dsl.NSC;   // :name placeholders, snake_case columns
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;
```

### SELECT

```java
SP sp = PSC.select("firstName", "lastName", "email")
           .from("account")
           .where(Filters.and(
               Filters.eq("status", "ACTIVE"),
               Filters.gt("createdAt", cutoff)))
           .orderBy("lastName")
           .build();

// sp.query()      -> SELECT first_name, last_name, email FROM account
//                    WHERE status = ? AND created_at > ? ORDER BY last_name
// sp.parameters() -> ["ACTIVE", <cutoff>]
```

### INSERT / UPDATE / DELETE

```java
// INSERT from a bean (column names derived from properties)
PSC.insert(account).into("account").build();

// UPDATE
PSC.update("account")
   .set("status", "lastModified")
   .where(Filters.eq("id", id))
   .build();

// DELETE
PSC.deleteFrom("account")
   .where(Filters.eq("id", id))
   .build();
```

### Named parameters

Switch presets to change the placeholder style — the fluent chain stays identical.

```java
SP sp = NSC.select("*")
           .from("orders")
           .where(Filters.between("orderDate", start, end))
           .build();

// sp.query() -> SELECT * FROM orders WHERE order_date BETWEEN :minOrderDate AND :maxOrderDate
```

### Entity-based queries

Bean classes annotated for abacus (`@Table`, `@Column`, `@Id`, …) can drive both the column list and
the `FROM` table, so you rarely repeat names:

```java
// SELECT ... FROM account  — columns and table both come from the Account class
PSC.selectFrom(Account.class)
   .where(Filters.eq("id", id))
   .build();
```

### Feed the result to your runner

The `SP` record exposes `query()` and `parameters()`, which map cleanly onto any JDBC-style API:

```java
SP sp = PSC.select("*").from("account").where(Filters.eq("status", "ACTIVE")).build();
jdbcTemplate.query(sp.query(), sp.parameters().toArray(), accountRowMapper);
```

---

## Parameter styles & naming policies

Every `Dsl` preset encodes a **parameter style** and an **identifier naming policy**. Choose one preset;
the builder applies both consistently across generated SQL.

| Preset | Placeholders | Naming | Example output |
|--------|--------------|--------|----------------|
| `PSB`  | `?`          | no change   | `SELECT firstName FROM account WHERE id = ?` |
| `PSC`  | `?`          | snake\_case | `SELECT first_name FROM account WHERE id = ?` |
| `PAC`  | `?`          | UPPER\_CASE | `SELECT FIRST_NAME FROM ACCOUNT WHERE ID = ?` |
| `PLC`  | `?`          | camelCase   | `SELECT firstName FROM account WHERE id = ?` |
| `NSB`  | `:name`      | no change   | `SELECT firstName FROM account WHERE id = :id` |
| `NSC`  | `:name`      | snake\_case | `SELECT first_name FROM account WHERE id = :id` |
| `NAC`  | `:name`      | UPPER\_CASE | `SELECT FIRST_NAME FROM ACCOUNT WHERE ID = :id` |
| `NLC`  | `:name`      | camelCase   | `SELECT firstName FROM account WHERE id = :id` |

- **`P…`** → positional `?` (the standard, portable choice).
- **`N…`** → named `:name` parameters.
- **`M…`** (`MSB`/`MSC`/`MAC`/`MLC`) → MyBatis/iBATIS `#{name}` — **deprecated**, prefer the `N…` family.
- **`…SB`** raw family (`SCSB`/`ACSB`/`LCSB`) inlines literal values instead of parameterizing — **deprecated**
  due to SQL-injection risk; use `P…` or `N…` instead.

Need a style the presets don't cover? Build a custom dialect (see [Custom dialects](#custom-dialects)).

---

## The condition model

[`Filters`](src/main/java/com/landawn/abacus/query/Filters.java) is a static factory for immutable
`Condition` objects. Conditions compose freely and can be reused across queries.

```java
import com.landawn.abacus.query.Filters;

Filters.eq("status", "ACTIVE");
Filters.in("id", List.of(1, 2, 3));
Filters.between("age", 18, 65);
Filters.like("name", "A%");

// Boolean composition
Filters.and(
    Filters.eq("country", "US"),
    Filters.or(Filters.gt("age", 21), Filters.eq("vip", true)));
```

For a full query shape — joins, grouping, ordering, limits, set operations — assemble a `Criteria` and
hand it to any builder:

```java
import com.landawn.abacus.query.condition.Criteria;
import com.landawn.abacus.query.condition.On;
import com.landawn.abacus.query.SortDirection;

Criteria criteria = Criteria.builder()
    .join("orders", new On("users.id", "orders.user_id"))
    .where(Filters.and(
        Filters.eq("users.status", "active"),
        Filters.gt("orders.amount", 100)))
    .groupBy("users.department")
    .having(Filters.gt("COUNT(*)", 5))
    .orderBy("COUNT(*)", SortDirection.DESC)
    .limit(10)
    .build();

SP sp = PSC.select("users.department").from("users").append(criteria).build();
```

The `condition` package covers the full SQL surface:

- **Comparisons** — `Equal`, `NotEqual`, `GreaterThan`, `LessThan`, `GreaterThanOrEqual`,
  `LessThanOrEqual`, `Like`, `NotLike`, `Between`, `NotBetween`, `In`, `NotIn`, `InSubQuery`,
  `NotInSubQuery`, `Is`, `IsNot`, `IsNull`, `IsNotNull`, `IsNaN`, `IsInfinite`, …
- **Boolean logic & grouping** — `And`, `Or`, `Not`, `Junction`, `Cell`, `Binary`.
- **Clauses** — `Where`, `GroupBy`, `Having`, `OrderBy`, `Limit`, `Criteria`.
- **Joins** — `Join`, `InnerJoin`, `LeftJoin`, `RightJoin`, `FullJoin`, `CrossJoin`, `NaturalJoin`,
  `On`, `Using`.
- **Quantifiers & existence** — `Any`, `All`, `Some`, `Exists`, `NotExists`, `SubQuery`.
- **Set operations** — `Union`, `UnionAll`, `Intersect`, `Except`, `Minus`.

---

## Parsing utilities

### `ParsedSql` — named-parameter parsing

Rewrites `:name` / `#{name}` placeholders into JDBC `?` and returns the parameter names in order.
Results are cached for repeated parses of the same SQL.

```java
import com.landawn.abacus.query.ParsedSql;

ParsedSql parsed = ParsedSql.parse(
    "SELECT * FROM users WHERE id = :userId AND status = :status");

parsed.parameterizedSql();   // SELECT * FROM users WHERE id = ? AND status = ?
parsed.namedParameters();    // ["userId", "status"]
```

### `SqlParser` — tokenizer

Splits SQL into tokens while respecting quoted strings/identifiers, comments, and multi-character
operators (`>=`, `<>`, `->>`, …). Extensible with custom separators.

```java
import com.landawn.abacus.query.SqlParser;

List<String> tokens = SqlParser.parse("SELECT a, b FROM t WHERE a >= 1");
// keywords, identifiers, ",", and the ">=" operator each come back as their own token
// (whitespace runs collapse into single " " tokens)
```

### `SqlMapper` — externalize SQL in XML

Keep SQL statements out of Java source, referenced by short IDs.

```xml
<!-- sqlMapper.xml -->
<sqlMapper>
    <sql id="findAccountById">SELECT * FROM account WHERE id = ?</sql>
    <sql id="updateAccountNameById">UPDATE account SET name = ? WHERE id = ?</sql>
</sqlMapper>
```

```java
import com.landawn.abacus.query.SqlMapper;

SqlMapper mapper = SqlMapper.loadFrom("sqlMapper.xml");
String sql = mapper.get("findAccountById").parameterizedSql();
```

---

## Custom dialects

When the built-in presets don't fit — e.g. you need double-quoted identifiers or product-specific
pagination — build a `SqlDialect` and wrap it with `Dsl.forDialect(...)`:

```java
import com.landawn.abacus.query.Dsl;
import com.landawn.abacus.query.SqlDialect;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.query.SqlDialect.IdentifierQuote;
import com.landawn.abacus.util.NamingPolicy;

Dsl myDsl = Dsl.forDialect(SqlDialect.builder()
        .namingPolicy(NamingPolicy.SNAKE_CASE)
        .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
        .identifierQuote(IdentifierQuote.DOUBLE_QUOTE)
        .build());
```

Setting `productInfo` on the dialect lets builders emit product-specific syntax (for example
`FETCH … OFFSET` vs `LIMIT … OFFSET` pagination, or MySQL backtick quoting).

---

## Also see: `DynamicQuery`

For string-append use cases where the typed builder is more structure than you need,
[`DynamicQuery`](src/main/java/com/landawn/abacus/query/DynamicQuery.java) offers a lighter,
clause-by-clause builder:

```java
import com.landawn.abacus.query.DynamicQuery;
import com.landawn.abacus.query.DynamicQuery.DynamicSqlBuilder;

DynamicSqlBuilder builder = DynamicQuery.builder();
builder.select().append("id", "user_id").append("name");
builder.from().append("users", "u");
builder.where().append("u.active = ?").and("u.age > ?");
builder.orderBy().append("u.name ASC");
builder.limit(10);

String sql = builder.build();
// SELECT id AS user_id, name FROM users u WHERE u.active = ? AND u.age > ? ORDER BY u.name ASC LIMIT 10
```

---

## Package overview

**`com.landawn.abacus.query`**

| Class | Role |
|-------|------|
| `Dsl` | Entry point; predefined presets (`PSC`, `NSC`, …) and `forDialect(...)`. |
| `SqlBuilder` | The fluent `SELECT`/`INSERT`/`UPDATE`/`DELETE` builder. |
| `AbstractQueryBuilder` | Shared SQL-generation machinery and the `SP` result record. |
| `Filters` | Static factory for `Condition` objects. |
| `Selection` | Describes a `SELECT` target (entity class, alias, included/excluded props). |
| `SqlDialect` | Immutable naming-policy + parameter-style + quoting configuration. |
| `DynamicQuery` | Lightweight string-append builder. |
| `ParsedSql` | Named-parameter parser (`:name` / `#{name}` → `?`). |
| `SqlParser` | Quoting/comment-aware SQL tokenizer. |
| `SqlMapper` | Loads/saves named SQL statements from XML. |
| `QueryUtil`, `SortDirection`, `SqlOperation` | Shared helpers and small enums. |

**`com.landawn.abacus.query.condition`** — the immutable condition model (comparisons, boolean logic,
clauses, joins, quantifiers, and set operations listed [above](#the-condition-model)).

---

## Documentation

- **User guide / Wiki** — https://github.com/landawn/abacus-query/wiki
- **API docs (Javadoc)** — https://www.javadoc.io/doc/com.landawn.abacus/abacus-query/4.8.8/index.html
- **Changelog** — [CHANGES.md](./CHANGES.md)

## Related projects

- [abacus-common](https://github.com/landawn/abacus-common) — core utilities (required dependency).
- [abacus-jdbc](https://github.com/landawn/abacus-jdbc) — a JDBC/DAO layer that executes the SQL built here.

## License

Licensed under the [Apache License, Version 2.0](./LICENSE).
