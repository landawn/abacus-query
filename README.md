# abacus-query

[![Maven Central](https://img.shields.io/maven-central/v/com.landawn.abacus/abacus-query.svg)](https://central.sonatype.com/artifact/com.landawn.abacus/abacus-query)
[![Javadocs](https://img.shields.io/badge/javadoc-online-brightgreen.svg)](https://www.javadoc.io/doc/com.landawn.abacus/abacus-query)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)

**A Java SQL toolkit for building, composing, and inspecting SQL.** `abacus-query`
produces SQL strings and ordered parameter values. Your application supplies the
database connection, executes statements, manages transactions, and maps results.
It works with JDBC and can supply SQL to execution libraries such as
[abacus-jdbc](https://github.com/landawn/abacus-jdbc).

This guide describes the **4.9.6 source tree**, which targets **Java 21**. Dependency
versions below match [pom.xml](./pom.xml); use matching artifacts available in your
repository, or install this checkout locally with Maven.

## Contents

- [Installation](#installation)
- [Quick start](#quick-start)
- [Core concepts and builder lifecycle](#core-concepts-and-builder-lifecycle)
- [Parameter styles and naming policies](#parameter-styles-and-naming-policies)
- [SELECT queries](#select-queries)
- [Conditions and optional filters](#conditions-and-optional-filters)
- [INSERT, UPDATE, and DELETE](#insert-update-and-delete)
- [Entity mapping and selections](#entity-mapping-and-selections)
- [Joins and reusable criteria](#joins-and-reusable-criteria)
- [Subqueries and set operations](#subqueries-and-set-operations)
- [Dialects and pagination](#dialects-and-pagination)
- [Executing SQL and binding parameters](#executing-sql-and-binding-parameters)
- [Parsing named SQL with ParsedSql](#parsing-named-sql-with-parsedsql)
- [Tokenizing and inspecting SQL](#tokenizing-and-inspecting-sql)
- [External SQL with SqlMapper](#external-sql-with-sqlmapper)
- [Text assembly with DynamicQuery](#text-assembly-with-dynamicquery)
- [API reference map](#api-reference-map)
- [Troubleshooting and usage rules](#troubleshooting-and-usage-rules)
- [Building and testing](#building-and-testing)
- [Documentation and related projects](#documentation-and-related-projects)
- [License](#license)

## Installation

Use **JDK 21 or later** for this source version. The project POM declares
`abacus-common`, Lombok, and the Jakarta XML Binding API with `provided` scope;
Maven therefore does not supply them transitively to consumers.

Declare `abacus-common` explicitly for compilation and runtime:

```xml
<dependencies>
    <dependency>
        <groupId>com.landawn.abacus</groupId>
        <artifactId>abacus-query</artifactId>
        <version>4.9.6</version>
    </dependency>
    <dependency>
        <groupId>com.landawn.abacus</groupId>
        <artifactId>abacus-common</artifactId>
        <version>8.3.0</version>
    </dependency>
</dependencies>
```

The equivalent Gradle dependencies are:

```groovy
implementation 'com.landawn.abacus:abacus-query:4.9.6'
implementation 'com.landawn.abacus:abacus-common:8.3.0'
```

If you use `SqlMapper` XML loading, also make the Jakarta XML Binding API available;
the XML utilities in `abacus-common` reference it:

```xml
<dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
    <version>4.0.5</version>
</dependency>
```

```groovy
implementation 'jakarta.xml.bind:jakarta.xml.bind-api:4.0.5'
```

Lombok is used to build this library. Applications using the compiled library do
not need Lombok unless their own source uses it. Add a JDBC driver separately if
your application will execute SQL; this library does not bundle one.

## Quick start

This complete program generates a parameterized query without connecting to a database:

```java
import static com.landawn.abacus.query.Dsl.PSC;

import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.Filters;

public class QueryExample {
    public static void main(String[] args) {
        SP query = PSC.select("id", "firstName", "lastName")
                .from("account")
                .where(Filters.eq("status", "ACTIVE"))
                .orderBy("id")
                .limit(20)
                .build();

        System.out.println(query.query());
        System.out.println(query.parameters());
    }
}
```

Output:

```text
SELECT id, first_name AS "firstName", last_name AS "lastName" FROM account WHERE status = ? ORDER BY id LIMIT 20
[ACTIVE]
```

`PSC` means positional `?` parameters and `snake_case` naming. Converted
projection names receive aliases that preserve the original property names.
Keep the returned `SP` to access both parts; calling `build()` consumes the builder.

The remaining snippets are independent examples inside a method unless they
declare a class. They use these common imports, with additional imports shown
where needed:

```java
import static com.landawn.abacus.query.Dsl.PSC;
import static com.landawn.abacus.query.Dsl.NSC;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.Filters;
import com.landawn.abacus.query.SortDirection;
import com.landawn.abacus.query.condition.Condition;
```

## Core concepts and builder lifecycle

| Type | Purpose | Lifetime |
| --- | --- | --- |
| `Dsl` | Starts statements using a fixed dialect, such as `PSC` or `NSC`. | Reuse across calls and threads when custom dialect callbacks are thread-safe. |
| `SqlDialect` | Configures naming, parameter rendering, identifier quoting, product information, and tokenization. | Immutable configuration; custom callbacks have their own concurrency obligations. |
| `SqlBuilder` | Accumulates one SQL statement. | Mutable, single-use, and not thread-safe. |
| `Condition` | Represents a predicate, expression, subquery, or clause. | Standard implementations have stable structure; retained values and custom implementations are not necessarily deeply immutable. |
| `SP` | Holds `query()` and `parameters()`. | SQL string plus an immutable list snapshot; ordinary parameter objects are not deep-copied. |

The usual flow is **choose a DSL → start a statement → add clauses → build → execute**.

```java
var builder = PSC.select("id").from("account")
        .where(Filters.eq("email", "alice@example.com"));
SP result = builder.build();

String sql = result.query();
List<Object> values = result.parameters();
// Use result again if needed. Start a new builder for another statement.
```

`build()`, `apply(...)`, `accept(...)`, `debugPrint()`, and `toSubQuery()` are
terminal operations. Passing a builder to a composing operation such as
`union(otherBuilder)` or `from(otherBuilder, alias)` also consumes that child.
Once final rendering starts, the builder closes even if rendering fails. An
incomplete qualified join rejected by `build()` is a special case: its missing
`on(...)` or `using(...)` connector may still be supplied.

Write clauses in SQL order: `FROM`, joins, `WHERE`, `GROUP BY`, `HAVING`, `ORDER BY`,
pagination, and locking where supported. The fluent builder rejects many
duplicates, incomplete statements, and invalid clause positions. It does not
validate your schema or implement every database's grammar.

String-based column names are not compile-time checked against a schema.
Likewise, `Condition` is an umbrella type: a `Where` clause and a scalar expression
both implement it, but are not interchangeable in every API. Use predicates in
`where(...)`, and whole clauses or `Criteria` with `append(...)`.

## Parameter styles and naming policies

Each preset fixes both choices. The naming examples below refer to generated
property/column identifiers, not a promise to rewrite every word in raw SQL.

| Naming policy | Positional `?` | Named `:name` | Deprecated MyBatis `#{name}` | `firstName` becomes |
| --- | --- | --- | --- | --- |
| `NO_CHANGE` | `PSB` | `NSB` | `MSB` | `firstName` |
| `SNAKE_CASE` | `PSC` | `NSC` | `MSC` | `first_name` |
| `SCREAMING_SNAKE_CASE` | `PAC` | `NAC` | `MAC` | `FIRST_NAME` |
| `CAMEL_CASE` | `PLC` | `NLC` | `MLC` | `firstName` |

`SCSB`, `ACSB`, and `LCSB` inline SQL literals and are deprecated. Prefer a
positional or named preset for application values. The `M...` presets are also
deprecated; migrating to an `N...` preset changes placeholder syntax, so update
the consuming execution layer as well.

```java
SP positional = PSC.select("id").from("account")
        .where(Filters.eq("firstName", "Alice")).build();
// SELECT id FROM account WHERE first_name = ?
// parameters: [Alice]

SP named = NSC.select("id").from("account")
        .where(Filters.eq("firstName", "Alice")).build();
// SELECT id FROM account WHERE first_name = :firstName
// parameters: [Alice]
```

Named placeholder names are distinct from rendered column names. Repeated
properties, ranges, and membership predicates may produce generated or suffixed
names. Inspect the finished SQL or parse it with `ParsedSql`; do not reconstruct
names by guessing from the property list. Even named builders return an ordered
list of values, not a name-to-value map.

Entity metadata can provide explicit table/column mappings. Unannotated
properties follow the selected naming policy. Raw table expressions, aliases,
and already quoted identifiers have different handling from generated property
names; use entity metadata or explicit SQL when exact spelling matters.

## SELECT queries

### Projection, aliases, and distinct values

Select one expression, multiple property names, a collection, an ordered alias
map, or an entity class. An alias map maps **column/expression → result alias**:

```java
Map<String, String> columns = new LinkedHashMap<>();
columns.put("id", "accountId");
columns.put("firstName", "displayName");

SP query = PSC.select(columns).from("account")
        .where(Filters.eq("status", "ACTIVE"))
        .build();
```

Use ordered collections and `LinkedHashMap` when column or parameter order must
be reproducible. Do not depend on the iteration order of `HashMap` or `Map.of`.

```java
SP departments = PSC.select("department").distinct()
        .from("account").orderBy("department").build();

SP count = PSC.selectCountFrom("account")
        .where(Filters.eq("status", "ACTIVE")).build();
```

`distinctOn(...)`, `distinctRow()`, and `selectModifier(...)` support additional
SQL forms. Their availability in the builder does not imply that every database
accepts the resulting syntax.

### Grouping, aggregates, and ordering

```java
SP totals = PSC.select("department", "COUNT(*) AS member_count")
        .from("account")
        .where(Filters.eq("status", "ACTIVE"))
        .groupBy("department")
        .having(Filters.gt("COUNT(*)", 5))
        .orderBy("COUNT(*)", SortDirection.DESC)
        .limit(10)
        .build();
// parameters: [ACTIVE, 5]
```

`orderByAsc(...)` and `orderByDesc(...)` are shortcuts. For mixed directions,
pass a `LinkedHashMap<String, SortDirection>` to `orderBy(...)`. Use a stable,
unique tie-breaker such as `id` when paging results.

`forUpdate()` appends a locking clause. Whether it is legal, and what it locks,
depends on your database and transaction context.

## Conditions and optional filters

### Factory reference

Use [`Filters`](src/main/java/com/landawn/abacus/query/Filters.java) to build
conditions; the short and long comparison names are equivalent.

| Purpose | Factories and examples |
| --- | --- |
| Equality | `eq("id", 7)`, `equal(...)`, `ne(...)`, `notEqual(...)` |
| Ordering | `gt(...)`, `ge(...)`, `lt(...)`, `le(...)`; corresponding long names |
| Inclusive range | `between("age", 18, 65)`, `notBetween(...)` |
| Other range bounds | `geAndLt(...)`, `gtAndLt(...)`, `geAndLe(...)`, `gtAndLe(...)` |
| Membership | `in("id", List.of(1, 2, 3))`, `notIn(...)`; primitive-array and subquery overloads |
| Patterns | `like("name", "A%")`, `notLike(...)`, `contains(...)`, `startsWith(...)`, `endsWith(...)` and negated variants |
| Null checks | `isNull(...)`, `isNotNull(...)`, `isNullOrEmpty(...)`, `isNotNullAndNotEmpty(...)` |
| Boolean/identity tests | `isTrue(...)`, `isFalse(...)`, `is(...)`, `isNot(...)` |
| Composition | `and(...)`, `or(...)`, `not(...)`, `alwaysTrue()`, `alwaysFalse()` |
| Multiple properties | `allEqual(map)` joins equalities with AND; `anyEqual(map)` joins them with OR |
| Raw expression | `expr("CURRENT_TIMESTAMP")` for trusted SQL syntax |
| Subquery predicates | `exists(...)`, `notExists(...)`, `in(..., subquery)`, `notIn(..., subquery)` |
| Quantified operands | `any(subquery)`, `all(subquery)`, `some(subquery)`, used on the right of a comparison |

### Boolean composition

```java
Condition eligible = Filters.and(
        Filters.eq("status", "ACTIVE"),
        Filters.or(Filters.ge("age", 21), Filters.eq("vip", true)),
        Filters.isNotNull("email"));

SP query = PSC.select("id").from("account").where(eligible).build();
// parameters: [ACTIVE, 21, true]
```

The builder supplies parentheses for logical grouping. Standard conditions can
be reused across statements, but do not mutate retained parameter objects or
custom conditions while they are in use.

For a frequently used property, `NamedProperty` provides the same style of factories:

```java
import com.landawn.abacus.query.condition.NamedProperty;

NamedProperty age = NamedProperty.of("age");
SP query = PSC.select("id").from("account")
        .where(age.between(18, 65)).build();
```

Use cached `NamedProperty.of(...)` for a fixed set of application-known property
names. It is a convenience wrapper around a string, not a generated schema type.

### Optional search fields

Collect only applicable predicates, then add a single `WHERE`:

```java
String status = "ACTIVE";       // Request field; may be null.
Integer minimumAge = null;     // Omitted request field.
List<Condition> filters = new ArrayList<>();
if (status != null && !status.isBlank()) {
    filters.add(Filters.eq("status", status));
}
if (minimumAge != null) {
    filters.add(Filters.ge("age", minimumAge));
}

var builder = PSC.select("id", "email").from("account");
if (!filters.isEmpty()) {
    builder.where(Filters.and(filters));
}
SP query = builder.orderBy("id").limit(50).build();
```

An empty AND renders the Boolean identity `1 = 1`; an empty OR renders `1 = 0`.
Null children are rejected. Decide explicitly whether an empty search should
return everything, nothing, or omit the clause. This is particularly important
when constructing UPDATE and DELETE predicates.

### NULL, collections, and expressions

- In `SqlBuilder`, `eq("deletedAt", null)` becomes `IS NULL` and `ne(..., null)`
  becomes `IS NOT NULL`; neither contributes a bound null. Explicit
  `isNull(...)`/`isNotNull(...)` makes this intent clear.
- `in(...)` and `notIn(...)` reject empty collections and null members. To match
  null as well as listed values, compose `or(in(...), isNull(...))`. For an empty
  input list, omit the filter or use `alwaysFalse()` according to the application.
- `contains`, `startsWith`, and `endsWith` add `%` wildcards. They do not escape
  `%` or `_` already present in the supplied text.
- A string value is a bound value. `eq("u.id", "o.user_id")` compares to text,
  not to a column. Use a join connector or `Filters.expr("o.user_id")` for a
  trusted column expression.

```java
SP query = PSC.select("id").from("account")
        .where(Filters.lt("expiresAt", Filters.expr("CURRENT_TIMESTAMP")))
        .build();
// CURRENT_TIMESTAMP is SQL syntax; it is not added to parameters().
```

`Condition.toSql(...)` and `toString()` are useful for diagnostics. They are not
a replacement for rendering through the chosen DSL and using the finished
`SP.parameters()`: rendering can normalize nulls, inline expressions, and
rewrite subquery parameters.

## INSERT, UPDATE, and DELETE

### INSERT with values

An ordered map makes both the column list and bindings explicit:

```java
Map<String, Object> row = new LinkedHashMap<>();
row.put("firstName", "Alice");
row.put("lastName", "Chen");
row.put("email", null);

SP insert = PSC.insert(row).into("account").build();
// INSERT INTO account (first_name, last_name, email) VALUES (?, ?, ?)
// parameters: [Alice, Chen, null]
```

Map input retains explicit null values. Bean input behaves differently:
`insert(bean)` omits null properties and default-valued ID properties (for a
composite ID, the default-ID rule applies when all ID components are default).
Non-ID primitive defaults such as `0` remain values. Insert metadata also
excludes properties such as `@Transient`, `@NonColumn`, `@ReadOnly`, and
`@ReadOnlyId`, and honors `@Table` field selection. An empty resulting insert
is rejected.

Use `insert(bean, excludedPropNames)` to omit additional properties and
`into(Entity.class)` to obtain the target table from metadata.

### Multiple rows

```java
Map<String, Object> first = new LinkedHashMap<>();
first.put("firstName", "Alice");
first.put("lastName", "Chen");
Map<String, Object> second = new LinkedHashMap<>();
second.put("firstName", "Bob");
second.put("lastName", "Singh");

SP insert = PSC.batchInsert(List.of(first, second)).into("account").build();
// INSERT INTO account (first_name, last_name) VALUES (?, ?), (?, ?)
// parameters: [Alice, Chen, Bob, Singh]
```

`batchInsert(...)` is marked `@Beta`. It generates one multi-row statement;
it does not call JDBC `addBatch()` or execute anything. Map rows must have the
same key set. Bean rows must have the same runtime class, and maps and beans
cannot be mixed. Null rows are skipped, but an empty/all-null batch is invalid.
Split large inputs yourself to respect your database's statement and parameter
limits. Check that your database supports the emitted multi-row syntax.

### UPDATE with values

```java
SP update = PSC.update("account")
        .set("status", "INACTIVE")
        .set("email", (Object) null)
        .where(Filters.eq("id", 42L))
        .build();
// UPDATE account SET status = ?, email = ? WHERE id = ?
// parameters: [INACTIVE, null, 42]
```

`set(map)` supplies an explicit group of assignments. `set(bean)` reads bean
properties and honors update exclusions such as `@NonUpdatable`, `@ReadOnly`,
`@ReadOnlyId`, and `@Transient`. Use a map for partial updates so that only
intended fields are supplied; do not assume bean updates apply INSERT's null
omission rule. Exclusion overloads let you omit additional properties. The cast
in `set("email", (Object) null)` selects the value overload rather than the
overload accepting an entity and a set of excluded properties.

A trusted SQL expression can be used as an assignment value:

```java
SP update = PSC.update("account")
        .set("loginCount", Filters.expr("login_count + 1"))
        .where(Filters.eq("id", 42L)).build();
// parameters: [42]
```

### DELETE

```java
SP delete = PSC.deleteFrom("account")
        .where(Filters.eq("id", 42L)).build();
// DELETE FROM account WHERE id = ?
// parameters: [42]
```

UPDATE and DELETE without `WHERE` can affect every row. The library does not
require a predicate; enforce any application rule before executing the result.

### SQL templates without values

Column-only methods intentionally generate unbound placeholders:

```java
SP insertTemplate = PSC.insert("firstName", "lastName").into("account").build();
// INSERT INTO account (first_name, last_name) VALUES (?, ?)
// parameters: [] -- supply both values yourself when executing.

SP updateTemplate = PSC.update("account").set(List.of("status"))
        .where("id = ?").build();
// UPDATE account SET status = ? WHERE id = ?
// parameters: [] -- supply status, then id.
```

The same principle applies to value-free filters such as `Filters.eq("id")`
and class-only INSERT/UPDATE templates. Raw `where("id = ?")` does not collect
a value. Mixing unbound template slots with value-bearing filters leaves a
partial parameter list; use value-bearing overloads throughout when passing
`SP.parameters()` directly to JDBC.

## Entity mapping and selections

Entity support uses the annotations and bean metadata from `abacus-common`.
For example, this ordinary JavaBean supplies an explicit table and column mapping:

```java
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;

@Table(name = "account")
public class Account {
    @Id
    private long id;
    @Column(name = "email_address")
    private String email;
    private String firstName;

    public Account() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
}
```

The examples in this section use that `Account` class:

```java
SP select = PSC.selectFrom(Account.class)
        .where(Filters.eq("email", "alice@example.com"))
        .build();

Account account = new Account();
account.setEmail("alice@example.com");
account.setFirstName("Alice");
SP insert = PSC.insert(account).into(Account.class).build();
// Default id = 0 is omitted; email maps to email_address.

SP update = PSC.update(Account.class)
        .set("email", "new@example.com")
        .where(Filters.eq("id", 42L)).build();
```

`selectFrom(Entity.class)` derives both the projection and table. Generated
projection aliases preserve Java property names for a downstream row mapper.
`select(Entity.class).from(...)` separates projection metadata from the chosen
source. Explicit table overloads accepting an entity class retain property
mapping while allowing a different target table.

### Selection descriptors

`Selection` describes one entity projection, its table alias, result prefix,
and included/excluded properties:

```java
import com.landawn.abacus.query.Selection;

Selection selection = Selection.builder(Account.class)
        .tableAlias("a")
        .classAlias("account")
        .includedPropNames(List.of("id", "email"))
        .build();

SP query = PSC.selectFrom(selection)
        .where(Filters.eq("a.id", 42L)).build();
```

`tableAlias` qualifies SQL columns. `classAlias` prefixes result aliases so a
consumer can distinguish properties from multiple objects. `excludedPropNames`
and `includeSubEntityProperties` control the projection further. A selection
must resolve to at least one property.

Pass a `List<Selection>` for multiple entities. `selectFrom(selections)` derives
the sources, but does not infer relationship predicates; provide the intended
joins/conditions yourself. Including sub-entity properties describes columns,
not ORM relationship loading, cascading, or automatic object hydration.

## Joins and reusable criteria

### Direct joins

```java
SP query = PSC.select("a.id", "o.total")
        .from("account a")
        .leftJoin("orders o")
        .on(Filters.eq("a.id", Filters.expr("o.account_id")))
        .where(Filters.eq("a.status", "ACTIVE"))
        .orderBy("a.id")
        .build();
// parameters: [ACTIVE]
```

Qualified joins (`join`, `innerJoin`, `leftJoin`, `rightJoin`, `fullJoin`) need
`on(...)` or `using(...)`. `crossJoin` and `naturalJoin` do not take those
connectors. `on("a.id = o.account_id")` is the trusted string alternative;
`using("account_id")` uses common column names. Entity-class overloads supply
table metadata and aliases. Database support for individual join types varies.

### Criteria

Use a `Criteria` to package a reusable set of clauses:

```java
import com.landawn.abacus.query.condition.Criteria;

Criteria criteria = Criteria.builder()
        .join("orders o", Filters.on("a.id", "o.account_id"))
        .where(Filters.eq("a.status", "ACTIVE"))
        .groupBy("a.id")
        .having(Filters.gt("SUM(o.total)", 100))
        .orderBy("a.id")
        .limit(10)
        .build();

SP query = PSC.select("a.id", "SUM(o.total) AS total")
        .from("account a").append(criteria).build();
```

Criteria structure is immutable once built. Append it where its clauses belong
in SQL order; it cannot duplicate an existing `WHERE` or put a join after an
already-rendered `ORDER BY`. Do not pass a whole `Criteria` to `where(...)`, or
nest a `Where` inside another `Where`.

`appendIf(...)` and `appendIfOrElse(...)` support conditional clauses, but
arguments are evaluated by Java before the method runs. Build optional
conditions only after checking their required inputs.

## Subqueries and set operations

### Builder-backed subqueries

`toSubQuery()` finishes a SELECT builder and returns a reusable subquery snapshot:

```java
import com.landawn.abacus.query.condition.SubQuery;

SubQuery paidAccounts = PSC.select("accountId").from("orders")
        .where(Filters.gt("total", 100)).toSubQuery();

SP query = PSC.select("id", "email").from("account")
        .where(Filters.and(
                Filters.eq("status", "ACTIVE"),
                Filters.in("id", paidAccounts)))
        .build();
// parameters: [ACTIVE, 100]
```

`toSubQuery()` is marked `@Beta`. Keep the same SQL policy in parent and child
when the child has generated placeholders. The snapshot carries values and
placeholder metadata so composition preserves order and resolves generated
named-parameter collisions. Do not concatenate a child's `build().query()`
and discard its bindings.

For a derived table, pass an unfinished child builder and an alias to `from`:

```java
var paid = PSC.select("account_id", "total").from("orders")
        .where(Filters.gt("total", 100));
SP query = PSC.select("p.account_id", "p.total").from(paid, "p")
        .orderBy("p.account_id").build();
// paid has been consumed; the outer query includes its [100] binding.
```

The outer query addresses the child's output column names. This example uses
`account_id` explicitly to keep that output name; selecting the Java-style
`accountId` would generate the quoted result alias `"accountId"` instead.

### Raw SQL subqueries with bindings

```java
import com.landawn.abacus.query.condition.SubQuery;

SubQuery paid = Filters.subQuery(
        "SELECT account_id FROM orders WHERE total > ?", List.of(100));
SP query = PSC.select("id").from("account")
        .where(Filters.in("id", paid)).build();
```

Use the raw overload taking a parameter collection when the SQL contains values
to bind. Its bindings correspond to positional `?` placeholders. Structured
`Filters.subQuery(entityOrTable, projection, condition)` overloads are also
available. Scalar comparisons and single-column `IN` need a single-column
projection; tuple membership needs matching projection width.

`exists`, `notExists`, and quantified comparisons such as
`Filters.gt("total", Filters.all(subquery))` cover other subquery forms.

### UNION and other set operations

```java
SP query = PSC.select("email").from("account")
        .where(Filters.eq("status", "ACTIVE"))
        .unionAll(PSC.select("email").from("archived_account")
                .where(Filters.eq("status", "ACTIVE")))
        .orderBy("email")
        .build();
// parameters: [ACTIVE, ACTIVE]
```

The builder also exposes `union`, `intersect`, `except`, and `minus`, plus
`...Select(...)` variants that start another projection in the same chain.
Builder operands are consumed during composition. Use compatible projections
and SQL policies, and choose an operator supported by your database. Raw string
operands carry no parameter values. Set operands and captured subqueries are
checked as syntactic SELECT candidates; these checks are not a database security
boundary or a guarantee that invoked SQL functions have no side effects.

## Dialects and pagination

A `SqlDialect` configures rendering choices; it is not a complete database SQL
translator. Build a reusable custom DSL when you need product-aware pagination
or custom quoting:

```java
import com.landawn.abacus.query.Dsl;
import com.landawn.abacus.query.SqlDialect;
import com.landawn.abacus.query.SqlDialect.ProductInfo;
import com.landawn.abacus.query.SqlDialect.SqlPolicy;
import com.landawn.abacus.util.NamingPolicy;

Dsl sqlServer = Dsl.forDialect(SqlDialect.builder()
        .namingPolicy(NamingPolicy.SNAKE_CASE)
        .sqlPolicy(SqlPolicy.PARAMETERIZED_SQL)
        .productInfo(ProductInfo.of("Microsoft SQL Server"))
        .build());

SP page = sqlServer.select("id").from("account")
        .orderBy("id").limit(20, 40).build();
// SELECT id FROM account ORDER BY id OFFSET 40 ROWS FETCH NEXT 20 ROWS ONLY
```

**`limit(count, offset)` takes the page size first**, then the number of rows
to skip. Counts and offsets must be nonnegative. Use a positive page size for
portable FETCH syntax. Pagination numbers are emitted as SQL numbers, not
added to the value parameter list.

| Product information | `limit(10)` | `limit(10, 20)` |
| --- | --- | --- |
| Unset/unknown, MySQL, MariaDB, PostgreSQL, SQLite, H2 | `LIMIT 10` | `LIMIT 10 OFFSET 20` |
| Oracle, DB2 | `FETCH FIRST 10 ROWS ONLY` | `OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY` |
| SQL Server | `OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY` | `OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY` |

SQL Server requires an `ORDER BY` before OFFSET/FETCH, including a zero-offset
page. `offset(...)`, `offsetRows(...)`, `fetchNextRows(...)`, and
`fetchFirstRows(...)` provide more explicit forms; do not combine mutually
exclusive pagination forms or add a second limit. Product metadata does not
provide legacy-version emulation: use syntax supported by your database version.

| Dialect setting | Meaning and effective default |
| --- | --- |
| `namingPolicy(...)` | Default `SNAKE_CASE`. `KEBAB_CASE` is rejected. |
| `sqlPolicy(...)` | Default **`RAW_SQL`** when unset; explicitly choose `PARAMETERIZED_SQL` or `NAMED_SQL` for binding. |
| `identifierQuote(...)` | `DOUBLE_QUOTE` or `BACKTICK`, for generated aliases/identifiers needing quotes. Unset defaults to double quotes, or backticks for MySQL/MariaDB. |
| `productInfo(...)` | `ProductInfo.of(name)` or `of(name, version)`; recognizes product families from the name. |
| `namedParameterHandler(...)` | Custom NAMED_SQL token renderer; default appends `:` followed by the generated name. |
| `tokenizerConfig(...)` | Immutable tokenizer configuration used for fragment inspection. |

Setting `identifierQuote` does not quote every identifier or convert arbitrary
SQL between dialects. You can derive a configuration with
`PSC.sqlDialect().toBuilder()`, preserving its explicit parameter policy.
Custom named-parameter handlers must emit nonempty, distinct tokens for distinct
names and be deterministic and thread-safe. A custom syntax may require its own
executor/parser; `ParsedSql` recognizes its documented parameter formats.

## Executing SQL and binding parameters

### JDBC

With an application-supplied `java.sql.Connection connection`, a value-bearing
positional query maps directly to a prepared statement:

```java
SP query = PSC.select("id", "email").from("account")
        .where(Filters.eq("status", "ACTIVE")).build();

try (var statement = connection.prepareStatement(query.query())) {
    for (int i = 0; i < query.parameters().size(); i++) {
        statement.setObject(i + 1, query.parameters().get(i));
    }
    try (var rows = statement.executeQuery()) {
        while (rows.next()) {
            long id = rows.getLong("id");
            String email = rows.getString("email");
            // Map or process the row in your application.
        }
    }
}
```

Use `executeUpdate()` for INSERT/UPDATE/DELETE. Select JDBC setters appropriate
for your driver and data types; typed nulls, arrays, JSON, temporal values, and
vendor types may need explicit handling beyond `setObject`. Transactions,
generated keys, fetch size, timeouts, and connection cleanup belong to the
execution layer.

A JDBC-style library generally needs the same two inputs: `query.query()` and
`query.parameters().toArray()`. For Spring `JdbcTemplate`, for example, the
result can be supplied to `query(sql, rowMapper, args...)` or
`update(sql, args...)`. The application supplies that library and its row mapper.

### Named builder output

Plain JDBC does not bind `:name` syntax. Convert default named output before
execution, keeping the builder's ordered values:

```java
import com.landawn.abacus.query.ParsedSql;

SP named = NSC.select("id").from("account")
        .where(Filters.eq("email", "alice@example.com")).build();
ParsedSql parsed = ParsedSql.parse(named.query());
String jdbcSql = parsed.parameterizedSql();
List<Object> values = named.parameters();
// jdbcSql: SELECT id FROM account WHERE email = ?
// values: [alice@example.com]
```

For a named-parameter executor, build its name-to-value structure using the
actual parsed names and the corresponding builder values. This assumes every
slot was supplied through a value-bearing API and the default supported named
syntax was used. Keep the positional path for templates with separately bound
values or for custom placeholder formats.

`apply(...)` and `accept(...)` are terminal conveniences for passing the built
result to your own callback; they do not execute SQL by themselves:

```java
String sql = PSC.select("id").from("account").apply(SP::query);
```

## Parsing named SQL with ParsedSql

`ParsedSql` converts hand-written SQL's named markers to JDBC placeholders and
records names in occurrence order. It does not look up parameter values.

```java
import com.landawn.abacus.query.ParsedSql;

ParsedSql parsed = ParsedSql.parse(
        "SELECT id FROM account WHERE email = :email OR backup_email = :email");
Map<String, Object> arguments = Map.of("email", "alice@example.com");
List<Object> values = new ArrayList<>();
for (String name : parsed.namedParameters()) {
    if (!arguments.containsKey(name)) {
        throw new IllegalArgumentException("Missing SQL parameter: " + name);
    }
    values.add(arguments.get(name));
}
// parameterizedSql(): SELECT id FROM account WHERE email = ? OR backup_email = ?
// namedParameters(): [email, email]
// parameterCount(): 2
// values: [alice@example.com, alice@example.com]
```

| Method | Returns |
| --- | --- |
| `parse(sql)` | Parsed metadata, with a bounded cache for eligible repeated SQL strings. |
| `originalSql()` | The original SQL with leading/trailing whitespace trimmed. |
| `parameterizedSql()` | Normalized SQL, with supported named markers converted for recognized data operations. |
| `namedParameters()` | Names in occurrence order, including duplicates; empty for positional-only SQL. |
| `parameterCount()` | Number of detected binding occurrences, including positional ones. |
| `isDataOperation()` | Whether the statement is classified as a supported data operation. |

Supported formats are `?`, `:name` (including dotted paths), and `#{name}`.
MyBatis marker attributes such as `#{id, jdbcType=BIGINT}` are discarded during
conversion; this does not implement MyBatis type handling. A statement cannot
mix detected placeholder styles. `:ids` is one parameter: parsing does not expand
a collection into an `IN` list. Use `Filters.in(...)` to generate multiple slots.

Parameter conversion is limited to recognized data-operation statements,
including SELECT, INSERT, UPDATE, DELETE, WITH, MERGE, CALL, VALUES, TABLE,
EXPLAIN, and REPLACE. Other SQL is normalized without parameter conversion.
Parsing trims surrounding whitespace/trailing semicolons and normally removes
comments and collapses inter-token whitespace in converted statements; quoted
content and some significant line boundaries are preserved. It is not a
lossless SQL formatter.

Quoted text and recognized comments do not supply parameters. PostgreSQL casts,
question-mark operators, and pgJDBC `??` escapes receive special handling, so
counting literal `?` characters is not a substitute for `parameterCount()`.
There are dialect ambiguities, especially bracket quoting versus array
subscripts; consult the [ParsedSql source documentation](src/main/java/com/landawn/abacus/query/ParsedSql.java)
when using such syntax. Parsing does not validate or authorize database execution.

## Tokenizing and inspecting SQL

`SqlParser` provides lexical tools without requiring a query builder:

```java
import com.landawn.abacus.query.SqlParser;

List<String> tokens = SqlParser.tokenize(
        "SELECT id, email FROM account WHERE id >= 10");
int whereIndex = SqlParser.indexOfToken(
        "SELECT id FROM account WHERE id = 10", "WHERE");
boolean select = SqlParser.isSelectQuery("SELECT id FROM account");
```

`tokenize(...)` keeps quoted regions together, recognizes operators, and uses
single-space tokens for ordinary whitespace runs. Comments are normally
removed according to the tokenizer's documented rules. `indexOfToken(...)`
returns a character position in the source string (or `-1`), not an index into
the returned token list. `nextToken(...)` and `nextTokenEndIndex(...)` support
individual lookups.

For repeated forward scans, use a cursor rather than restarting a search:

```java
import com.landawn.abacus.query.SqlParser;

var cursor = SqlParser.cursor("SELECT id FROM account");
while (cursor.next()) {
    String token = cursor.token();
    int start = cursor.startIndex();   // Inclusive source position.
    int end = cursor.endIndex();       // Exclusive source position.
}
```

Custom separators belong to an immutable configuration and tokenizer instance:

```java
import com.landawn.abacus.query.SqlParser;

var config = SqlParser.TokenizerConfig.builder().withSeparator("::").build();
var tokenizer = SqlParser.tokenizer(config);
List<String> tokens = tokenizer.tokenize("SELECT value::text FROM events");
```

Classification helpers include `isSelectQuery`, `isInsertQuery`,
`isUpdateQuery`, `isDeleteQuery`, `isInsertOrReplaceQuery`, and
`isReadOrInsertQuery`. `isSyntacticallyReadQuery` performs stronger lexical checks
for a SELECT candidate; the deprecated `isReadOnlyQuery` name is an alias.
These methods do not resolve functions, stored procedures, permissions, or
database side effects. Use actual database privileges for enforcement.

## External SQL with SqlMapper

`SqlMapper` stores SQL by ID and loads/saves a small XML format. It is not a
MyBatis mapper engine and does not perform execution or result mapping.

Save this as `sql/account.xml` relative to the application's working directory:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<sqlMapper>
    <sql id="findAccountById" fetchSize="50" timeout="10">
        SELECT id, email FROM account WHERE id = :id
    </sql>
    <sql id="findSmallOrders"><![CDATA[
        SELECT id, total FROM orders WHERE total < :maximum
    ]]></sql>
</sqlMapper>
```

Use CDATA or XML escaping for characters such as `<` and `&` in SQL text.

```java
import com.landawn.abacus.query.ParsedSql;
import com.landawn.abacus.query.SqlMapper;

SqlMapper mapper = SqlMapper.loadFrom("sql/account.xml");
ParsedSql query = mapper.get("findAccountById");
String sql = query.parameterizedSql();
List<String> names = query.namedParameters();
String fetchSize = mapper.attributes("findAccountById").get("fetchSize");
```

`loadFrom` accepts a path list, multiple path arguments, `File` objects, or an
`InputStream`. For a classpath resource, obtain a stream through your class
loader and pass it to `loadFrom(InputStream)`; check for a missing resource.
Use distinct IDs across merged files. An ID must be nonempty, contain no
whitespace, and have at most 128 characters. Duplicate IDs are rejected.

| Operation | API |
| --- | --- |
| Lookup | `get(id)` returns `ParsedSql`, or null when missing; `containsId(id)` checks existence. |
| Enumerate | `ids()`, `size()`, `isEmpty()` |
| Inspect metadata | `attributes(id)` |
| Add SQL | `add(id, sql)`; overloads accept `ParsedSql` and attribute maps. |
| Remove/copy | `remove(id)`, `copy()` |
| Save XML | `saveTo(file)`, `saveTo(path)`, `saveTo(outputStream)` |

Attributes such as `fetchSize`, `batchSize`, `timeout`, and `resultSetType` are
stored as strings for downstream consumers. The mapper does not apply them to
a JDBC statement or validate their numeric meaning. Configure a mapper before
sharing it, or synchronize access: it is mutable and not thread-safe.

## Text assembly with DynamicQuery

Use `DynamicQuery` when you already have SQL fragments and want clause-oriented
string assembly. It does not translate property names or collect bindings:

```java
import com.landawn.abacus.query.DynamicQuery;

DynamicQuery.Builder builder = DynamicQuery.builder();
builder.select().append("id", "account_id").append("email");
builder.from().append("account", "a");
builder.where().append("a.status = ?").and("a.age >= ?");
builder.orderBy().append("a.id ASC");
builder.limit(10);

String sql = builder.build();
List<Object> values = List.of("ACTIVE", 18); // Maintained by the application.
// SELECT id AS account_id, email FROM account a
// WHERE a.status = ? AND a.age >= ? ORDER BY a.id ASC LIMIT 10
```

Clause builders offer append and conditional-append methods; the WHERE clause
also offers `and`, `or`, and `appendPlaceholders`. Typed clauses are emitted in a
fixed order. Raw fragments remain your responsibility, including Boolean
grouping, identifiers, binding order, and database syntax.

`DynamicQuery.Builder` and its clause handles are mutable and single-use:
`build()` closes both the builder and retained clause handles, including on
failure. It can produce a fragment with only a WHERE clause and does not require
or validate a complete SELECT. Use `Dsl` when you need condition rendering,
entity metadata, parameter collection, or dialect-aware SQL generation.

## API reference map

These are the main entry points; follow the source links or Javadoc for overloads
and exact validation rules.

| API | Main entry points |
| --- | --- |
| [`Dsl`](src/main/java/com/landawn/abacus/query/Dsl.java) | Presets; `forDialect`, `select`, `selectFrom`, `selectCountFrom`, `insert`, `insertInto`, `batchInsert`, `update`, `deleteFrom`, `renderCondition` |
| [`SqlBuilder`](src/main/java/com/landawn/abacus/query/SqlBuilder.java) / [`AbstractQueryBuilder`](src/main/java/com/landawn/abacus/query/AbstractQueryBuilder.java) | `from`, `into`, joins/connectors, `where`, `groupBy`, `having`, `orderBy`, pagination, set operations, `set`, `append`, `build`, `toSubQuery`, `apply`, `accept` |
| [`Filters`](src/main/java/com/landawn/abacus/query/Filters.java) | Predicates, Boolean composition, clauses, joins, subqueries, set operations, raw expressions |
| [`Selection`](src/main/java/com/landawn/abacus/query/Selection.java) | `builder(entityClass)`, aliases, included/excluded properties, sub-entity projection |
| [`SqlDialect`](src/main/java/com/landawn/abacus/query/SqlDialect.java) | `builder`, `toBuilder`, `SqlPolicy`, `IdentifierQuote`, `ProductInfo` |
| [`ParsedSql`](src/main/java/com/landawn/abacus/query/ParsedSql.java) | Named-parameter conversion, ordered names, binding count, operation classification |
| [`SqlParser`](src/main/java/com/landawn/abacus/query/SqlParser.java) | Token lists, cursors, token lookup, configurable separators, statement classifiers |
| [`SqlMapper`](src/main/java/com/landawn/abacus/query/SqlMapper.java) | XML SQL registry, attributes, load/save operations |
| [`DynamicQuery`](src/main/java/com/landawn/abacus/query/DynamicQuery.java) | Raw fragment assembly by clause |
| [`QueryUtil`](src/main/java/com/landawn/abacus/query/QueryUtil.java) | Entity table/property/column metadata helpers |
| [`SortDirection`](src/main/java/com/landawn/abacus/query/SortDirection.java), [`SqlOperation`](src/main/java/com/landawn/abacus/query/SqlOperation.java) | Sort and operation enums |

The [`condition` package](src/main/java/com/landawn/abacus/query/condition) groups
the SQL node types:

| Category | Types |
| --- | --- |
| Base contracts | `Condition`, `AbstractCondition`, `ComposableCondition`, `Binary`, `Cell`, `ComposableCell`, `Clause` |
| Comparisons | `Equal`, `NotEqual`, `GreaterThan`, `GreaterThanOrEqual`, `LessThan`, `LessThanOrEqual`, `Between`, `NotBetween` |
| Membership/patterns | `In`, `NotIn`, `InSubQuery`, `NotInSubQuery`, `Like`, `NotLike` |
| Null/identity/special numeric tests | `Is`, `IsNot`, `IsNull`, `IsNotNull`, `IsNaN`, `IsNotNaN`, `IsInfinite`, `IsNotInfinite` |
| Boolean composition | `And`, `Or`, `Not`, `Junction` |
| Clauses | `Where`, `Having`, `GroupBy`, `OrderBy`, `Limit`, `Criteria` |
| Joins | `Join`, `InnerJoin`, `LeftJoin`, `RightJoin`, `FullJoin`, `CrossJoin`, `NaturalJoin`, `On`, `Using` |
| Subqueries/quantifiers | `SubQuery`, `Exists`, `NotExists`, `Any`, `All`, `Some` |
| Set operations | `Union`, `UnionAll`, `Intersect`, `Except`, `Minus` |
| Expressions/properties | `SqlExpression`, `NamedProperty`, `Operator` |

For just a condition fragment and its values, use the same DSL renderer:

```java
SP fragment = PSC.renderCondition(Filters.eq("firstName", "Alice")).build();
// first_name = ?
// parameters: [Alice]
```

## Troubleshooting and usage rules

| Symptom | Explanation / action |
| --- | --- |
| Missing `com.landawn.abacus...` classes | Explicitly add the matching `abacus-common` dependency; it is `provided` in this project's POM. |
| Missing `jakarta.xml.bind...` classes while loading XML | Add the Jakarta XML Binding API dependency shown above. |
| Unsupported class-file version | Run with Java 21 or later for this source version. |
| `SqlBuilder is closed` | A terminal operation or parent composition already consumed the builder. Reuse `SP`/a subquery snapshot or start a fresh builder. |
| More SQL placeholders than collected values | A column-only template, value-free filter, or raw SQL fragment created unbound slots. Bind all slots yourself or use value-bearing APIs throughout. |
| `IN` rejects a list | Empty lists and null members are invalid. Handle an empty search explicitly; combine `IN` with `IS NULL` when needed. |
| A column name is sent as text | Strings on the value side are values. Use `Filters.expr` for a trusted SQL expression or `Filters.on(left, right)` for a join condition. |
| Unexpected wildcard matches | LIKE helpers do not escape supplied `%`/`_`. Apply an application-defined escaping policy and database-appropriate `ESCAPE` syntax when literal matching is required. |
| Unexpected INSERT column omissions | Bean INSERT skips null values/default IDs. Use an explicit map to retain null assignments. |
| A join fails at build time | Complete a qualified join with `on` or `using`. |
| Duplicate/out-of-order clause exception | Combine predicates before calling `where`; append criteria before later clauses. |
| SQL Server pagination fails | Supply an `ORDER BY` and a positive fetch count. The signature is `limit(count, offset)`. |
| Named SQL will not execute in JDBC | Convert supported named syntax with `ParsedSql` and bind the ordered values, or use a named-parameter executor. |
| Parsed SQL has fewer comments or different spacing | Conversion rebuilds normalized SQL. `originalSql()` retains the trimmed original text; retain the input separately for exact whitespace. |

Treat raw SQL fragments, expressions, table names, aliases, and sort columns as
application-controlled syntax. Placeholders bind **values**, not identifiers or
keywords. Map user-facing sort/filter options to an allowlist of known columns;
do not concatenate request text into `expr`, `where(String)`, `append(String)`,
or `DynamicQuery`. Rendering and lexical checks cannot replace database
permissions or application authorization.

Validate SQL and binding order against your target database, especially for
vendor functions, special numeric predicates, locking, set operations, and
custom dialect settings. The toolkit generates SQL; it does not promise that
every exposed SQL form is portable across products.

## Building and testing

From the repository root with JDK 21+ and Maven available:

```sh
mvn compile
mvn test
mvn test jacoco:report
mvn -DskipTests -Dgpg.skip=true install
```

The last command installs this source version into the local Maven repository
without requiring a signing key. Maven may need repository access to obtain the
POM's plugin and dependency versions. `mvn test` runs the configured
`AbacusQueryTestSuite`; the suite excludes `slow-test`. JaCoCo reporting is
available through `jacoco:report`.

See [test instructions](src/test/java/README.md) for suite organization and
[scripts](scripts/README.md) for maintenance tools. The executable
[supported-SQL catalog](src/test/java/com/landawn/abacus/query/SqlBuilderSupportedSqlTest.java)
and [pagination tests](src/test/java/com/landawn/abacus/query/SqlDialectPaginationTest.java)
provide additional usage examples. A source unit test of emitted SQL does not
replace testing that statement with your actual database and driver.

## Documentation and related projects

- [API Javadoc](https://www.javadoc.io/doc/com.landawn.abacus/abacus-query)
- [Project wiki](https://github.com/landawn/abacus-query/wiki)
- [Changelog](./CHANGES.md)
- [abacus-common](https://github.com/landawn/abacus-common): shared utilities, metadata, annotations, and collection types.
- [abacus-jdbc](https://github.com/landawn/abacus-jdbc): JDBC and DAO execution facilities.

## License

Licensed under the [Apache License, Version 2.0](./LICENSE).
