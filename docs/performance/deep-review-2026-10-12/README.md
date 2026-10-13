# Java source review — 2026-10-12

Four reviewers covered all 82 Java files under `src/main/java`: 80 class/interface/enum files, their nested types, and two package descriptors. The review inspected executable code line by line and checked its contracts, callers, state transitions, defensive copies, allocation behavior, and existing tests. A follow-up four-reviewer pass re-read every local production diff across seven files from the beginning. Builder, parser, and condition changes were assigned to reviewers who had not implemented them; the root reviewer checked utilities and integration.

Baseline revision: `48b1b8d93cd885a375fe765d2f4b1787d55864cb`. The checkout already changed `abacus-common` from 8.1.0 to 8.2.0 in `pom.xml`; that change was preserved and used throughout validation. Runtime: Oracle JDK 25, Maven compiler release 21, Windows.

## Fixes

| Area | Problem and resulting behavior | Regression coverage |
| --- | --- | --- |
| UPDATE assignments | An `=` inside a quoted column name or array subscript suppressed the generated placeholder. Assignment detection now requires a top-level `=` outside quoted text and follows the builder's dialect quote rules. | All four SQL policies; doubled quote delimiters; mixed assignment lists; array subscripts; existing complete assignments; default-dialect fallback readings; MySQL/MariaDB string escapes and backtick identifiers; unterminated quotes before/after assignments. |
| Product versions | Control-only text could become empty after `trim()` and throw from `charAt(0)`. Invalid version comparisons now return false as documented. | Invalid values in both comparison operands. |
| Temporary tables after functions | An aliased table-valued function consumed the backward scanner's name/alias budget, causing a following `#temp` table and its remaining SQL to be treated as a comment. Function arguments now belong to the same unit as the function name; this absorption excludes single-quoted literals. | Qualified/quoted functions, block comments before arguments, implicit/explicit/no aliases, named bindings, actual hash comments, following write statements, and malformed single-quoted function-name fragments. |
| Closed dynamic clauses | A retained clause handle kept its recycled SQL buffer alive. Closing now clears the reference before recycling. | All six clause types, empty clauses, idempotent close, and rejected use after build. |
| Closed SQL builders | Consumed builders retained staged maps, batch rows, and projection descriptors. Finalization now releases those inputs while preserving result parameters and placeholder metadata needed by composition. | Successful/failed finalization, recoverable incomplete JOINs, batch INSERTs, each projection form, named UNIONs, and reusable subquery snapshots. |
| Value snapshots | Every scalar/date/primitive-array snapshot allocated cycle-tracking state. Identity tracking is now allocated only for reference arrays. | Primitive array types, nested mutable values, repeated references, direct/indirect cycles, scalar identity. |
| Binary membership accessors | IN/NOT IN accessors copied owned unmodifiable scalar lists on every read. They now reuse those lists when no element is an array, date, calendar, or nested condition. Reuse also checks the constructor's operator: an overridden accessor alone cannot establish that a list is owned. | Caller-input isolation, unmodifiable exposure, mutable values, fresh parameters from custom conditions, and subclass operator overrides through both value accessors. |
| Quoted identifiers | Splitting a qualified name repeatedly copied its remaining suffix, producing quadratic allocation. The scanner now uses an index into the original string. | Escaped delimiters, Unicode escape clauses, empty segments, unterminated quotes, and long qualified names. |
| Nested property exclusions | Each property/exclusion pair allocated a dotted prefix. Matching now checks the path boundary directly. | Exact exclusions, parent exclusions, partial prefixes, null/empty exclusions, and trailing-dot boundaries. |
| Temporary-table lists | Every `#temp` lookup could rescan all preceding comma-separated tables. Per-scan memoization now shares the governing context of previously visited commas. | Long lists, aliases, quotes, comments, independent clauses, reverse-order lookups, and parser differential checks. |
| Sparse SQL tokenization | Token-list capacity grew with SQL text length even for one large literal or a discarded comment. Initial capacity is capped at 8,192 slots; the previous estimate is retained through 32 KiB of input. | Large identifiers/literals/comments, empty results, and dense streams that grow beyond initial capacity. |

Comments explain the lexical and lifecycle changes. The version-validation guard also explains why `trim()` can empty a value that passes `Strings.isBlank()`: trimming removes additional control characters.

Compatibility note: once final rendering begins, `build()` clears the protected `_props`, `_propsList`, `_propOrColumnNames`, `_propOrColumnNameAliases`, and `_multiSelects` fields, including when rendering fails. Subclasses that previously inspected these fields after `build()` must capture any required staged inputs before final rendering. Rejection of an incomplete qualified JOIN happens before finalization and leaves the builder open. This resource-release change is observable at the protected API boundary; it does not change the finished SQL/parameter result contract.

## Validation

The baseline suite passed 5,811 tests with zero failures/errors. The new cases are in:

- `SqlBuilderTest` and `SqlDialectPaginationTest`
- `SqlParserTest`
- `DynamicQueryTest` and `QueryUtilTest`
- `condition/AbstractConditionTest` and `condition/BinaryTest`

Correctness and retention failures were reproduced before their fixes. Performance tests assert output and isolation, avoiding wall-clock thresholds. The existing POM hardcodes `testFailureIgnore=true`, so Maven's exit status alone does not establish success; Surefire failures/errors were checked explicitly.

Parser memoization matched an otherwise identical uncached implementation on 20,000 generated tokenization cases and 68,201 reverse-order hash-identifier lookups. Comparing the final parser against HEAD produced five tokenization differences and seven identifier-lookup differences in the same corpus. All were malformed generated inputs containing the newly recognized `name(arguments) alias` shape before a comma/hash and a preceding table-context keyword; none were suppressed from the comparison. Dedicated tests additionally verify actual function/table syntax and preserve INSERT VALUES/SELECT hash-comment handling.

Final integrated run after applying review feedback and consolidating the tests: `mvn -B -ntp test` passed **5,846 tests**, including **35 new regression tests**, with **zero failures, errors, or skips**. All 87 fresh Surefire XML reports were checked, including individual discovery of all 35 added/relocated test methods. The four temporary review test classes were removed after their coverage was moved into the existing per-class files; obsolete compiled classes were archived outside test discovery. `git diff --check` passed. The current full log and machine-readable totals are in `target/review-consolidation-2026-10-12/full-tests.log` and `full-tests-summary.json`. Earlier runs remain under `target/deep-review-final.*`, `target/local-changes-recheck-2026-10-12/`, and `target/review-feedback-2026-10-12/`.

`mvn -B -ntp javadoc:javadoc '-DstaleDataPath=target/review-consolidation-2026-10-12/javadoc-stale-data.txt'` also passed with the configured `doclint=all`. A fresh cache record was necessary because the ordinary invocation reused older HTML. The generated `Binary` and `AbstractQueryBuilder` pages were checked for the revised text. The only warning is the pre-existing undocumented implicit default constructor in `SqlDialect`; no new Javadoc errors or warnings were introduced. The log is `target/review-consolidation-2026-10-12/javadocs.log`.

The separate checked-in API index was regenerated with `java tools/code/ApiDocGenerator.java`: 82 source files, zero diagnostics/errors. Both `docs/ai/API.md` and `docs/ai/api-index.json` now reflect the current SET assignment, builder lifecycle, and membership-accessor contracts. Their complete diffs were reviewed against source, and the JSON was parsed successfully. This generation is independent of Maven Javadoc; its log is `target/review-consolidation-2026-10-12/api-generation.log`.

## Follow-up findings and coverage

The fresh review found one introduced regression in the first-pass membership optimization. A `Binary` subclass constructed with `EQUAL` could override `operator()` to return `IN` or `NOT_IN`. The scalar constructor retains its caller's collection, so treating the virtual accessor as proof of ownership returned that mutable collection directly. The added guard checks the stored constructor operator before reusing a value. The regression test fails against the first-pass implementation and passes with the guard; it covers both operators, both accessors, unmodifiable independent results, and isolation from later source-list changes. Before/after evidence is in `target/condition-second-review/`.

The pass also expanded SET tests for nested subscripts, quoted equality signs, and Unicode escape clauses. Identifier tests now cover every naming policy, underscore/empty segments, and Unicode escape clauses at nonzero offsets. Parser tests cover quoted aliases, comments around function names and aliases, and nested functions/subqueries. The dense-input test was enlarged to 40,001 characters/tokens so it actually crosses the new 8,192-slot initial-capacity cap. Comments and Javadocs now describe the null cycle-map sentinel, snapshot ownership, buffer release, quote-scan starting boundaries, and comma-context cache state.

Independent diagnostic checks matched the original utility implementation on **120,000 identifier conversions** across all six naming policies (20,000 generated inputs, fixed seed 98474621). A separate parser check passed **40 function/alias combinations** and **39,327 shared-versus-fresh memo lookups**, including forward, reverse, and shuffled visitation. Evidence is in `target/local-changes-recheck-2026-10-12/utility-differential.log` and `target/fresh-parser-review/probe-results.txt`. No further correctness defects were found in the reviewed changes.

Accepted review feedback added three SET test methods whose fixtures distinguish standard and backslash-escaped readings, including fallback after an unterminated first reading. They also cover MySQL/MariaDB single/double-quoted strings, literal backslashes and doubled backticks in identifiers, and unterminated quotes before or after an established assignment. Synthetic malformed fragments are explicitly tested only for lexical behavior; adding a placeholder does not make them valid SQL. Public SET Javadocs now explain these rules. Membership-accessor documentation explicitly excludes nested conditions from list reuse, and its test checks both accessors' unmodifiable outer copies, shared condition objects, and fresh parameter extraction. The new SET tests and documentation received an independent source review.

The final follow-up merged all 34 existing regressions into seven established public `@Tag("2025")` test classes, preserving their scenarios and removing the cross-test fixture dependency. One additional parser test covers 36 malformed single-quoted function-name/gap/alias combinations. It failed before narrowing the function-name absorption branch and passes afterward; the general quoted-segment path remains unchanged. Valid function controls now also include no-alias forms. Before/after evidence is in `target/parser-followup-review/`.

After that parser change, the memo differential again matched an otherwise identical build with comma-context reuse disabled on 20,000 tokenizations and 68,201 reverse-order lookups. The separate 40 function/alias combinations and 39,327 memo visitation-order checks also passed. The five tokenization/seven lookup differences against HEAD are exactly the previously reviewed malformed examples; they are not all single-quoted function-name cases. Current logs are `target/review-consolidation-2026-10-12/parser-memo-differential.log`, `parser-memo-orders.log`, and `parser-head-differential.log`. An independent reviewer also checked the final parser change, relocated utility tests, generated API diffs, and builder compatibility note without finding further issues.

## Allocation and scaling samples

These are local diagnostic measurements using `ThreadMXBean`, not JMH results or general application throughput claims. JIT profiles affect allocation elimination, particularly for scalar snapshots. The three small harnesses are retained beside this report.

| Workload | Before | After |
| --- | ---: | ---: |
| Identifier conversion, 1,024 quoted/unquoted segment pairs | 27,688,144 B/call | 328,936 B/call |
| Tokenize one 8 MiB literal | 8,388,651 B/call | 32,811 B/call |
| Tokenize ordinary short query | 808 B/call | 808 B/call |
| Tokenize approximately 10 KiB dense input | 131,984 B/call | 131,984 B/call |
| Tokenize approximately 100 KiB dense input | 1,318,856 B/call | 1,614,232 B/call |
| Scan list of 8,000 temporary tables, one diagnostic sample | 1,201 ms | 8.9 ms |
| Scalar snapshot after mixed scalar/array warmup | 312 B/call | 0 B/call |
| Expose 32-value scalar membership list after the same warmup | 10,208 B/call | 0 B/call |

The token capacity limit trades about 22% more cumulative allocation in the 100 KiB dense synthetic case for about 99.6% less allocation in the 8 MiB sparse case. This is growth-array churn, not a token limit. Scalar-only warmup allowed the baseline JIT to eliminate some snapshot allocations; under that profile the membership-list copy alone measured 224 B/call before and 0 afterward.

After adding the ownership guard, the condition harness still measured 0 B/call for scalar snapshots and 32-value scalar membership exposure under the same mixed warmup. That recheck is recorded in `target/local-changes-recheck-2026-10-12/condition-allocations.log`.

To reproduce current measurements from the repository root in PowerShell after running tests:

```powershell
$report = [xml](Get-Content target/surefire-reports/TEST-com.landawn.abacus.query.SqlParserTest.xml -Raw)
$reviewClassPath = ($report.testsuite.properties.property | Where-Object name -eq 'java.class.path').value
$probeSources = Get-ChildItem docs/performance/deep-review-2026-10-12 -Filter '*.java' | ForEach-Object FullName
javac -cp $reviewClassPath -d target/deep-review-benchmarks $probeSources
java -Xmx512m -Xbatch -cp "target/deep-review-benchmarks;$reviewClassPath" com.landawn.abacus.query.QueryUtilityAllocationCheck
java -Xmx512m -Xbatch -cp "target/deep-review-benchmarks;$reviewClassPath" ParserReviewProbe
java -Xmx512m -Xbatch -cp "target/deep-review-benchmarks;$reviewClassPath" com.landawn.abacus.query.condition.ConditionAllocationBenchmark
```

Before measurements used baseline classes, including isolated compilation of the original parser/condition sources against the same dependency version. No production source was reset for those measurements.

## Coverage inventory

All entries below are `.java` files. Files without a confirmed defect or justified optimization were reviewed and retained.

| Reviewer area | Files |
| --- | --- |
| Builders — 5 | AbstractQueryBuilder, SqlBuilder, SqlDialect, Selection, SubQuerySnapshot |
| Parsing/mapping — 4 | SqlParser, ParsedSql, SqlMapper, SqlOperation |
| Query utilities — 7 | Dsl, DynamicQuery, Filters, QueryUtil, SortDirection, cs, package-info |

The condition reviewer covered these 66 files:

```text
AbstractBetween  AbstractCondition  AbstractIn  AbstractInSubQuery
All  And  Any  Between  Binary  Cell  Clause  ComposableCell
ComposableCondition  Condition  Criteria  CrossJoin  Equal  Except
Exists  FullJoin  GreaterThan  GreaterThanOrEqual  GroupBy  Having
In  InnerJoin  InSubQuery  Intersect  Is  IsInfinite  IsNaN  IsNot
IsNotInfinite  IsNotNaN  IsNotNull  IsNull  Join  Junction  LeftJoin
LessThan  LessThanOrEqual  Like  Limit  Minus  NamedProperty
NaturalJoin  Not  NotBetween  NotEqual  NotExists  NotIn
NotInSubQuery  NotLike  On  Operator  Or  OrderBy  RightJoin  Some
SqlExpression  SubQuery  Union  UnionAll  Using  Where  package-info
```

The review also covered nested builders, dialect options, SQL-parameter records, lexical state records, tokenizer configuration, rollback checkpoints, subquery metadata, and condition-specific helpers. Existing class-metadata caches and immutable result snapshot behavior were retained: changing their lifetime/identity rules requires broader dependency-level evidence.
