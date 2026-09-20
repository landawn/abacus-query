# ParsedSql geometric-cast and JSON-query fixes

Measured locally on September 20, 2026, after fixing quoted/formatted geometric casts and `JSON_ARRAY` query-body classification. Performance tuning remains deferred.

Compared with the code immediately before these two fixes, ordinary cases and the 32,000-element stress cases changed by approximately -3% to +3% in median time. These small differences do not establish a speed improvement or regression. Ordinary-query and largest-stress allocations were unchanged. The corrected JSON query case allocated 240 fewer bytes because its nonexistent binding is no longer collected.

The earlier performance gap remains: uncached queries with two and sixteen positional bindings are respectively 39.0% and 22.4% slower than the recorded reconstructed baseline in this fresh comparison. Cached calls remain about 13 ns with zero measured allocation.

## Versions and method

- `recorded`: the reconstructed behavioral baseline described in the [earlier record](../parsed-sql-2026-09-20/README.md), recompiled and rerun here. It is neither Git HEAD nor a recovered original artifact. Source SHA-256: `8253277056001c2c8593ea2c93876512b85b098a2cf4c0bc802d62b04c9234e5`.
- `before`: the actual working-tree `ParsedSql.java` captured immediately before these two fixes. Source SHA-256: `a490b6d72e136698e0ec1f6f6a0847b6a6a432ecad85a7e61d22bd72806e6df8`.
- `current`: the fixed parser. Source SHA-256: `d67fc566b8a8db29aef4f84aadd684ecd3a55520160d2551b560b3a37b1dbede`.

All versions used the same other production classes and abacus-common 8.0.0 dependencies. Parser sources were compiled with Java 25, `--release 17 -proc:none`. The Java 25 benchmark uses the earlier harness, with two additional short correctness cases: `json_query_format` and `unary_quoted_cast`.

Three fresh JVMs per version ran sequentially with `-Xms512m -Xmx1024m -XX:+UseG1GC`, in this order: recorded1, before1, current1, current2, before2, recorded2, before3, recorded3, current3. Each warmed every short case with 10,000 parses and every stress case/size with 30 parses. Nine measured rounds followed, with deterministically shuffled case order. Uncached SQL generation and cache clearing were outside timing; cache-hit batches reused one SQL string. Allocation was measured on the benchmark thread with `ThreadMXBean`.

Reported times are medians of the three per-fork medians. These are local comparative microbenchmarks, not JMH or application-throughput results. Fork ranges appear in [summary.csv](summary.csv); the experiment does not provide statistical confidence intervals. The table compares fresh measurements under identical settings, rather than ratios against timings from the earlier session.

## Ordinary queries

Times are microseconds per operation except the cache-hit row.

| Case | Recorded baseline | Before these fixes | Fixed | Change from before |
|---|---:|---:|---:|---:|
| No parameters | 1.394 | 1.362 | 1.337 | -1.8% |
| Two named parameters | 2.227 | 2.172 | 2.124 | -2.2% |
| Two positional parameters | 2.044 | 2.916 | 2.840 | -2.6% |
| Sixteen positional parameters | 10.876 | 13.408 | 13.314 | -0.7% |
| Cache hit, two positional parameters | 12.800 ns | 13.095 ns | 12.859 ns | -1.8% |

For two positional parameters, before-fork medians ranged from 2.861 to 3.142 us; fixed-fork medians ranged from 2.761 to 2.867 us. For sixteen parameters, the ranges were 13.259-14.661 us and 12.997-13.404 us. One fixed cache-hit fork measured 21.170 ns, so the approximately 13 ns median does not describe every fork.

## Stress inputs: 32,000 elements or nesting levels

Times are milliseconds per uncached parse. Before and fixed versions returned identical, correct counts for every row. The recorded baseline has incorrect counts for the rows marked with an asterisk; its times are not equivalent-correctness comparisons.

| Shape | Recorded baseline | Before these fixes | Fixed | Change from before |
|---|---:|---:|---:|---:|
| Bare compact concatenation* | 7.334 | 3.545 | 3.622 | +2.2% |
| Array concatenation | 3.722 | 4.186 | 4.311 | +3.0% |
| Array addition | 5.924 | 7.304 | 7.297 | -0.1% |
| Repeated unary CAST* | 40.820 | 50.551 | 51.453 | +1.8% |
| Deep unary nesting* | 7.098 | 9.806 | 9.683 | -1.3% |

Sizes 2,000, 4,000, 8,000, 16,000 and 32,000 are preserved in the CSVs. The new type reader examines only a schema, qualification dot and type name; the existing lazy parenthesis index is retained. JSON query/value context uses the existing primitive depth stack without allocating scope objects.

## Correctness-sensitive cases

| SQL shape | Before count | Fixed count | Before / fixed time | Before / fixed allocation |
|---|---:|---:|---:|---:|
| `JSON_ARRAY(SELECT payload ? format JSON FROM t)` | 1 | 0 | 2.471 / 2.408 us | 1592 / 1352 B |
| `?- ?:: "line"` | 2 | 1 | 2.214 / 2.176 us | 1352 / 1352 B |

These timings compare different results. The recorded reconstructed baseline also miscounts these two specific inputs; this does not contradict a different result from Git HEAD. It also throws on the existing named `format(...)` benchmark, so the summary's numerical time ratio for that row must not be interpreted as a successful-parse speedup.

Every fixed-version measured row passed its expected-count and zero-error checks. All nine JVM runs exited successfully.

## Validation and artifacts

Three new methods in the existing `ParsedSqlTest` cover cast formatting, quoting/case/schema boundaries, positional/named/MyBatis/literal operands, SQL rewriting and offsets, comments, bracket groups, CTE/query scopes, nested constructors, and real mixed-style errors. All three fail against the immediate before version. A related conversion fix preserves bindings glued to quoted cast types without interpreting markers inside the type name.

The configured suite's 5,242 tests were exercised through JUnit with the same tag filters. Initially 5,137 passed and 105 `SqlMapperTest` cases hit sandbox access errors during temporary-directory cleanup. Those 105 passed when rerun with workspace temporary directories and `junit.jupiter.tempdir.cleanup.mode.default=NEVER`; no test assertions were disabled. Maven compilation also hit dependency-archive close access errors, so the changed source/tests were compiled through the Java compiler API and launched directly. Updated `ParsedSql` Javadoc passed doclint. `git diff --check` and byte-level CRLF checks passed.

This directory preserves the [harness](ParserBenchmark.java), [summary](summary.csv), and nine raw per-fork CSV files: `recorded-1.csv` through `recorded-3.csv`, `before-1.csv` through `before-3.csv`, and `current-1.csv` through `current-3.csv`. Raw files contain each fork's nine-round median, binding counts and error totals.

Local source snapshots, compiled comparison jars, launch/summary scripts and validation logs are under ignored `.audit/p2-fixes/`; dependencies and compiler helpers are under `.audit/p3-validation/`. They are not durable checkout artifacts. To reproduce, supply the exact source versions identified above, compile each against identical remaining classes/dependencies, and run the preserved harness with the recorded flags and version/fork arguments. Do not substitute Git HEAD for the reconstructed baseline.
