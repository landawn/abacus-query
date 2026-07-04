# abacus-query
 
[![Maven Central](https://img.shields.io/maven-central/v/com.landawn.abacus/abacus-query.svg)](https://central.sonatype.com/artifact/com.landawn.abacus/abacus-query/4.8.7)
[![Javadocs](https://img.shields.io/badge/javadoc-4.8.7-brightgreen.svg)](https://www.javadoc.io/doc/com.landawn.abacus/abacus-query/4.8.7/index.html)

## Overview

`abacus-query` is a lightweight, dependency-light Java 17+ library for building, parsing, and managing SQL — without an ORM. It produces plain SQL strings (plus parameter lists) that can be handed to JDBC, Spring `JdbcTemplate`, MyBatis, or [abacus-jdbc](https://github.com/landawn/abacus-jdbc).

It centers on three things:

- **Fluent SQL builders** — type-safe `SELECT` / `INSERT` / `UPDATE` / `DELETE` construction from either column names or annotated bean classes, with pluggable parameter styles and identifier naming policies.
- **A composable condition model** — `WHERE`, `JOIN`, `GROUP BY`, `HAVING`, `ORDER BY`, set operations, and subqueries expressed as immutable `Condition` objects you can build, combine, and reuse.
- **SQL parsing utilities** — a tokenizer (`SqlParser`), a named-parameter parser (`ParsedSql`) that rewrites `:name` / `#{name}` into JDBC `?`, and an XML-backed `SqlMapper` for externalizing statements.

### Key packages and classes

`com.landawn.abacus.query`
- **`SqlBuilder`** — the main fluent builder. 15 concrete inner classes encode parameter style × naming policy: prefix `S`/`P`/`N`/`M` selects raw-inlined / `?` / `:name` / `#{name}`; suffix `SB`/`SC`/`AC`/`LC` selects no-change / snake\_case / UPPER\_CASE / camelCase (e.g. `PSC`, `NLC`, `MAC`). The `*CSB` raw-SQL family is deprecated due to injection risk.
- **`AbstractQueryBuilder`** — shared SQL-generation machinery backing `SqlBuilder` (entity introspection, naming-policy handling, prop→column mapping).
- **`DynamicQuery`** — a lighter, string-append-style builder for cases where the typed `SqlBuilder` API is overkill.
- **`Filters`** — static factory for `Condition` instances (`equal`, `in`, `between`, `like`, joins, etc.).
- **`Selection`** — describes a `SELECT` target (entity class, table alias, sub-entity properties, included/excluded props).
- **`ParsedSql`** — parses a SQL string with `:name`/`#{name}` parameters, returns parameterized SQL + ordered parameter names; results are pooled/cached.
- **`SqlParser`** — SQL tokenizer that respects quoting, comments, and multi-char operators; extensible via custom separators.
- **`SqlMapper`** — loads/saves named SQL statements from XML, useful for keeping SQL out of Java source.
- **`QueryUtil`** — common helpers shared across the builders.
- **`SortDirection`**, **`SqlOperation`** — small enums for ordering and statement type.

`com.landawn.abacus.query.condition` — the condition model:
- Comparisons: `Equal`, `NotEqual`, `GreaterThan`, `GreaterThanOrEqual`, `LessThan`, `LessThanOrEqual`, `Like`, `NotLike`, `Between`, `NotBetween`, `In`, `NotIn`, `InSubQuery`, `NotInSubQuery`, `Is`, `IsNot`, `IsNull`, `IsNotNull`, `IsNaN`, `IsNotNaN`, `IsInfinite`, `IsNotInfinite`.
- Boolean logic & grouping: `And`, `Or`, `Not`, `Junction`, `Cell`, `ComposableCell`, `Binary`.
- Clauses: `Where`, `GroupBy`, `Having`, `OrderBy`, `Limit`, `Criteria`, `Clause`.
- Joins: `Join`, `InnerJoin`, `LeftJoin`, `RightJoin`, `FullJoin`, `CrossJoin`, `NaturalJoin`, `On`, `Using`.
- Quantifiers & existence: `Any`, `All`, `Some`, `Exists`, `NotExists`, `SubQuery`.
- Set operations: `Union`, `UnionAll`, `Intersect`, `Except`, `Minus`.
- Foundations: `Condition`, `AbstractCondition`, `Expression`, `Operator`, `NamedProperty`, plus `AbstractBetween`, `AbstractIn`, `AbstractInSubQuery`, `ComposableCondition`.

### Typical usage

```java
// SELECT with conditions, ? placeholders, snake_case columns
SP sp = PSC.select("firstName", "lastName")
           .from("users")
           .where(CF.eq("department", "Engineering"))
           .orderBy("lastName")
           .build();
// sp.sql    -> SELECT first_name, last_name FROM users WHERE department = ? ORDER BY last_name
// sp.parameters -> ["Engineering"]
```

## Download/Installation & [Changes](https://github.com/landawn/abacus-query/blob/master/CHANGES.md):

* [Maven](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.landawn%22)

* Gradle:
```gradle
// JDK 17 or above:
compile 'com.landawn.abacus:abacus-query:4.8.7'
```


## User Guide:
Please refer to [Wiki](https://github.com/landawn/abacus-query/wiki)


## Also See: [abacus-common](https://github.com/landawn/abacus-common), [abacus-jdbc](https://github.com/landawn/abacus-jdbc).


## Recommended Java programming libraries/frameworks:
[Apache Flink](https://flink.apache.org/), 
[Apache Spark](https://spark.apache.org/), 
[Oryx 2](http://oryx.io/), 
[lombok](https://github.com/rzwitserloot/lombok), [Guava](https://github.com/google/guava), [Kyro](https://github.com/EsotericSoftware/kryo), [snappy-java](https://github.com/xerial/snappy-java), [lz4-java](https://github.com/lz4/lz4-java), [Caffeine](https://github.com/ben-manes/caffeine), [Ehcache](http://www.ehcache.org/), [Chronicle-Map](https://github.com/OpenHFT/Chronicle-Map), [echarts](https://github.com/apache/incubator-echarts), 
[Chartjs](https://github.com/chartjs/Chart.js), [Highcharts](https://www.highcharts.com/blog/products/highcharts/), [Apache POI](https://github.com/apache/poi)/[easyexcel](https://github.com/alibaba/easyexcel), [mapstruct](https://github.com/mapstruct/mapstruct), [Sharding-JDBC](https://github.com/apache/incubator-shardingsphere), [hppc](https://github.com/carrotsearch/hppc), [fastutil](https://github.com/vigna/fastutil) ...[awesome-java](https://github.com/akullpp/awesome-java)

## Recommended Java programming tools:
[Spotbugs](https://github.com/spotbugs/spotbugs), [JaCoCo](https://www.eclemma.org/jacoco/)...