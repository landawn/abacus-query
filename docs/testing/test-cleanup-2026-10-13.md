# Unit-test cleanup — second pass, 2026-10-13

This pass completes the parser organization deferred during the
[initial cleanup](test-cleanup-2026-10-12.md), removes eight further redundant
tests, and strengthens SQL, cache, and concurrency assertions.

## Organization and alignment

`ParsedSqlTest` now contains 253 tests in 14 nested behavior groups: caching,
geometric operators, JDBC calls, metadata/equality, MyBatis parameters, named
parameters, parameter-style validation, positional parameters, question-mark
operators, SQL/JSON expressions, SQL normalization, statement recognition,
subscripts, and validation. Tests are sorted by name within each group.

The two scanner-precondition tests from `ParsedSqlValidationTest` now live in
`ParsedSqlTest.Validation`; the redundant file and ownership-audit exception
were removed. Literal-preserving fingerprints verified every retained and moved
parser test body during reorganization. Subsequent assertion changes were
reviewed separately.

The remaining year tag was removed, assertions use explicit static imports,
and all JUnit methods now have package-private visibility. Three parser method
names gained the conventional `test` prefix. The whitespace-cache test was
renamed to describe its actual contract: whitespace variants use separate cache
entries while producing the same normalized SQL.

## Removed cases

Different SQL inputs, overloads, regression cases, and binding styles remain
distinct. These removals have equivalent or stronger retained assertions:

| Removed method | Retained method |
| --- | --- |
| `ParsedSqlTest.testParse_simpleSelect` | `testParse_SimpleSelect` |
| `ParsedSqlTest.testParse_withIbatisParameterOptions` | `testParse_WithIBatisParameterOptions`, also checking parameter count |
| `ParsedSqlTest.testGetParameterizedSql_withSingleNamedParameter` | `testGetParameterizedSql_WithSingleNamedParameter` |
| `ParsedSqlTest.testParse_parenthesizedSelectPrefix` | `testParse_ParenthesizedSelectPrefix` |
| `ParsedSqlTest.testParse_nullSql_throwsException` | `testParse_NullThrows` |
| `ParsedSqlTest.testParse_emptySql_throwsException` | `testParse_EmptyThrows` |
| `ParsedSqlTest.testParse_cacheReuse` | `testParse_Caching` |
| `SqlBuilderTest.testAllPublicConstants` | `AbstractQueryBuilderTest.RenderingContracts.testPublicConstants` |

## Assertion and determinism improvements

- Six builder tests now check exact SQL and ordered parameters for deletion,
  entity insertion with excluded properties, and a compound update predicate.
  The insert expectation includes the documented omission of null bean values.
- Ten remaining wall-clock `Date` constructions were replaced with fixed values.
- Parser cache tests now check expected SQL, binding metadata, and reuse of each
  whitespace variant's cache entry.
- The `NamedProperty` first-access concurrency test now checks 64 synchronized
  rounds across eight callers, each using a fresh property name. Every round
  checks identity across callers, the property name, and the retained cache entry.
  Waits are bounded and executor cleanup remains in `finally`. This improves
  contention coverage; thread scheduling still determines the exact interleaving.

A concurrent production edit temporarily accepted control-character-only SQL.
The user confirmed that the rejection contract should remain. Validation after
trimming was restored, and the existing rejection test remains intact. There is
no net production-source change relative to the starting commit, `b55692d0`.

## Verification

Both the baseline and final suite used isolated Maven build directories.
An additional comparison used an immutable copy of the starting production
sources while the concurrent parser edit was being resolved. Final validation
uses the current workspace sources.

| Check | Baseline | Final |
| --- | ---: | ---: |
| Java files under `src/test/java` | 89 | 88 |
| Test methods | 5,832 | 5,824 |
| Executed JUnit invocations | 5,847 | 5,839 |
| Failures / errors / skips | 0 / 0 / 0 | 0 / 0 / 0 |
| Covered lines | 12,383 / 12,952 | 12,383 / 12,952 |
| Covered branches | 11,106 / 12,715 | 11,106 / 12,715 |

All eight fewer invocations correspond to the reviewed removals. Every remaining
source test method was found in the JUnit results, and no class lost covered
instructions, lines, branches, or methods. The audit-tool regression suite passes
all ten tests. Source audits report no exact duplicate groups across the suite,
no methods without assertions, no unsorted test groups, and no unexplained
ownership splits. Compile-time `cs` labels remain the only source unit without
a dedicated name-matched test class.

Parallel verification JVMs initially exceeded the Windows paging-file limit.
Successful final runs used sequential execution and temporary 512 MB heap caps;
the project's Maven configuration was not changed.

Logs, the comparison summary, and temporary Maven configurations are under
`.audit/test-cleanup-round2`; regenerated source reports are under
`.audit/test-audit`. Both generated locations are ignored by Git.
For routine reproduction after removing the old validation class, run
`mvn clean test jacoco:report` and the commands in the
[audit guide](../../scripts/test_audit/README.md).
