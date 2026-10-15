# Unit-test cleanup — 2026-10-12

Follow-up: the [second pass](test-cleanup-2026-10-13.md) completes parser
organization and records additional consolidation and verification.

## Result

The suite is organized around production units and behavior groups. Seventeen
redundant tests were removed, eleven factory invocations were consolidated into
five parameterized methods, and assertions now check additional SQL, binding,
function-name, and snapshot contracts.

There are 89 Java files under `src/test/java`, including 5,832 test methods.
The final suite executes 5,847 invocations with zero failures, errors, or skips.
Every inventoried test method was matched to an executed JUnit method.

## Consolidation

| Previous location | Current owner |
| --- | --- |
| Fifteen factory groups in `SqlBuilderTest` | `DslTest`, retaining policy-specific fixture scopes |
| `BuilderNullArgumentTest`, `BuilderValidationOrderTest` | `AbstractQueryBuilderTest.Validation` |
| `ValidationOrderTest` | `DynamicQueryTest.Validation`, `FiltersTest.Validation` |
| `NullArgumentContractTest` | `DslTest.Validation`, `QueryUtilTest.Validation`, `DynamicQueryTest.Validation` |
| `ParserMapperNullArgumentTest` | `SqlParserTest`, `ParsedSqlValidationTest`, `SqlDialectTest`, `SqlOperationTest`, `SortDirectionTest` |
| `ConditionNullArgumentTest` | `AbstractConditionTest.Validation`, `SqlExpressionTest.Validation`, `ConditionTest.SharedContracts` |
| `ConditionValidationOrderTest` | `ComposableConditionTest`, `AbstractBetweenTest`, `CriteriaTest`, `OrderByTest`, `InTest` validation groups |

Large builder and parser classes now use named nested behavior groups. Other
unit tests are sorted by method name, with setup first and helpers/fixtures last.
Assertion imports and JUnit method visibility are consistent. Reordering was
checked against literal-preserving method-body fingerprints before compilation.

`QueryTestSupport` centralizes exact SQL/ordered-binding assertions, selection
construction, and the shared `Order` entity. Fixed dates replace wall-clock
inputs in the DSL cases and the range-condition fixture. SQL debug output was
replaced with assertions. Manual `CodeHelper` and `Maven` utilities were moved
to `scripts/java`; `CodeHelper` no longer carries a JUnit annotation.

`SqlBuilderSupportedSqlTest` remains the supported-SQL catalog, and
`SqlDialectPaginationTest` remains a focused pagination suite.

Concurrent edits to `ParsedSql.java`, `ParsedSqlTest.java`, and the API docs were
preserved. `ParsedSqlTest` was excluded from the reordering pass; its additional
scanner preconditions were placed in `ParsedSqlValidationTest`.

## Removed cases

Deletion required matching setup and assertions, or a stronger retained test
covering the same contract. Different literals, policies, overloads, fixtures,
and validation precedence were preserved.

| Unit | Removed method | Retained coverage / reason |
| --- | --- | --- |
| `SKTest` | `testSQLKeywords_Transaction` | Identical body retained in the descriptively named keyword/function test. |
| `SKTest` | `testSQLFunctions_Aggregate` | Identical body retained in the descriptively named keyword/function test. |
| `SKTest` | `testSQLFunctions_Mathematical` | Identical body retained in the descriptively named keyword/function test. |
| `SKTest` | `testSQLFunctions_String` | Identical body retained in the descriptively named keyword/function test. |
| `SKTest` | `testSQLFunctions_DateTime` | Identical body retained in the descriptively named keyword/function test. |
| `AbstractQueryBuilderTest` | `testConstants` | testPublicConstants checks the exact values of all seven constants. |
| `AbstractQueryBuilderTest` | `testConstantsAreNotNull` | testPublicConstants checks the exact values of all seven constants. |
| `condition/CriteriaTest` | `testCriteria` | Identical body retained in testConstructor. |
| `condition/LessThanOrEqualTest` | `testGetPropValue_Null` | Identical body retained in testWithNullValue. |
| `condition/LessThanTest` | `testGetPropValue_Null` | Identical body retained in testWithNullValue. |
| `condition/LikeTest` | `testGetPropValue_Null` | Identical body retained in testNullPattern. |
| `condition/NamedPropertyTest` | `testConstructorWithNull` | Identical body retained in testConstructorWithNullThrowsIAE. |
| `condition/NamedPropertyTest` | `testOfMethodWithEmptyString` | Identical body retained in testOfMethodWithEmptyStringThrowsException. |
| `condition/NamedPropertyTest` | `testOfMethodWithNull` | Identical body retained in testOfMethodWithNullThrowsException. |
| `condition/UsingTest` | `testCreateUsingConditionWithNullCollection` | Identical body retained in testCreateUsingCondition_ThrowsOnNullCollection. |
| `condition/UsingTest` | `testCreateUsingConditionWithNullVarArgs` | Identical body retained in testCreateUsingCondition_ThrowsOnNullArray. |
| `SqlParserTest` | `testIsFunctionName (token-presence-only version)` | Consolidated into the API assertion and boundary cases of testIsFunctionName_twoArgOverload, now named testIsFunctionName. |

## Parameterized cases

The original invocations remain independently reported by JUnit:

| Factory contract | Variants |
| --- | --- |
| Independent builder instances | PSB, PAC, PLC |
| INSERT RETURNING | NSB, NLC |
| Empty selection arguments | NAC, NLC |
| Table-name preservation | NAC, NLC |
| Single aggregate expression | MSB, MSC |

The consolidated tests check exact SQL and bindings where applicable. Snapshot
coverage adds 14 invocations for equality, deep array bindings, binding counts,
nulls/order, composition metadata, and isolation from caller list mutation.
Three mixed-unit contract methods were split to keep assertions with their owners.

Invocation accounting: 5,847 baseline − 17 redundant cases + 14 new snapshot cases
+ 3 additional methods from contract splits = 5,847 final invocations.
Parameterization retains all eleven original factory invocations.

## Verification

The initial local run passed 5,846 tests. A concurrent parser regression added
one case while cleanup was in progress. The comparable baseline therefore ran
5,847 tests: original tracked tests plus the current, untouched `ParsedSqlTest`.

Both comparison runs used fresh, isolated Maven build directories against the
same current production sources. This excluded stale generated classes from the
original `target` directory and avoided disturbing concurrent build output.

- Final Maven run: 5,847 tests; zero failures, errors, or skips.
- Audit-tool regression tests: 10 passed, including literal/comment handling,
  parameter-source separation, inherited tags, and refusal to discard collisions.
- Exact duplicate candidates within a test class: zero, including short methods.
- Additional cross-group duplicate check within each file: zero.
- Methods without assertion calls: zero candidates.
- Test methods missing from JUnit discovery: zero.
- No class lost covered instructions, lines, branches, or methods.

| Coverage | Comparable baseline | Final |
| --- | ---: | ---: |
| Covered lines | 12,382 / 12,953 | 12,384 / 12,953 |
| Covered branches | 11,102 / 12,719 | 11,109 / 12,719 |
| `SubQuerySnapshot` lines | 36 / 38 | 38 / 38 |
| `SubQuerySnapshot` branches | 17 / 24 | 24 / 24 |

The snapshot class is the only class with a coverage change; all changes are gains.
The name-based ownership audit reports only `cs` without a dedicated test class;
its compile-time validation labels are exercised through validation tests.

## Reproduction

After class moves, use a clean build:

```sh
mvn clean test jacoco:report
python -B -m unittest discover -s scripts/test_audit -p 'test_*.py'
python -B scripts/test_audit/parse_tests.py --out .audit/test-audit/parse_output.json
python -B scripts/test_audit/find_duplicates_intra.py --exact-only --out .audit/test-audit/intra_exact.json
python -B scripts/test_audit/find_no_assertions.py --out .audit/test-audit/no_assertions.json
python -B scripts/test_audit/map_consolidation.py --out .audit/test-audit/consolidation.json
```

Maven now fails on test failures. Suite discovery selects the query package,
includes nested tests without requiring year tags, and retains the `slow-test`
exclusion. JaCoCo's agent arguments are retained in the test JVM configuration.

Local logs and comparison data are retained under `.audit/test-cleanup`;
regenerated audit reports are under `.audit/test-audit`. These generated paths
are ignored by Git. See the [test conventions](../../src/test/java/README.md)
and [audit tooling guide](../../scripts/test_audit/README.md).
