# Unit-test cleanup: 2026-10-15

## Plan and scope

Starting revision: `13eec809`; the working tree was clean. Scope: tests,
test conventions, and audit tooling. Production behavior and Maven dependency
versions remain unchanged.

1. Establish a fresh baseline with `mvn -o -B clean test jacoco:report`.
   Preserve Surefire XML, JaCoCo XML, and a source inventory under the ignored
   `.audit/test-cleanup-2026-10-15` directory.
2. Audit ownership, source ordering, exact and semantic duplicate candidates,
   parameterized discovery, and assertion quality. Review scenarios and fixtures
   before deleting any test.
3. Group the remaining large suites by behavior: Filters, DynamicQuery,
   QueryUtil, SqlMapper, Criteria, SqlExpression, SubQuery, NamedProperty,
   Binary, and AbstractCondition. Preserve existing focused groups and put
   helpers and entity fixtures after tests. Sort methods within behavior groups;
   keep execution independent of that source order.
4. Consolidate demonstrably redundant cases, retaining their stronger assertions.
   Standardize descriptive method and parameterized invocation names. Strengthen
   exact SQL, ordered bindings, and metadata checks where tests currently assert
   only non-null results or fragments.
5. Run focused tests after logical batches, then the full suite from fresh
   output. Compare discovered methods, executed cases, and per-class coverage.
   Investigate any lost coverage and account for every removed invocation.
   Update the test conventions and record the results here.

## Baseline

- 88 Java files in the test source tree; 6,034 executable test methods.
- 6,176 JUnit invocations; zero failures, errors, or skips.
- No exact duplicate groups and no assertion-free test methods.
- 29 class/group scopes contain methods out of name order.
- The ten audit-tool regression tests pass.

The local sandbox could not access the installed Python runtime or resolve the
existing Maven cache. The baseline ran successfully with approved access to
those installed tools, offline dependencies, and temporary 512 MB JVM heap caps.
No build configuration change was required.

## Implementation and validation

All five steps are complete. Production sources, dependencies, test discovery
configuration, and coverage exclusions are unchanged.

### Organization and standardization

- Added 75 behavior groups across the ten suites named in the plan, preserving
  existing focused groups. Constructors, rendering, parameters, copying, cache
  behavior, clauses, and validation now have identifiable owners.
- Moved 14 sorting-helper tests from `OrderByTest` to
  `AbstractConditionTest.PropertiesAndSorting`, where `createSortSpec` is
  implemented. The moved tests now name `createSortSpec` explicitly.
- Sorted all test scopes by case-insensitive method name. Renamed 91 methods
  with obsolete pass/batch labels and replaced an ambiguous null-array name
  with `testEqualsAnyRejectsNullArray`.
- Made the ten remaining public JUnit methods package-private and gave three
  projection parameterized tests descriptive invocation names. Their static
  method sources moved with them into `SubQueryTest.ProjectionRendering`.
- Added `scripts/test_audit/check_test_layout.py` and five regression tests.
  The checker includes parameterized methods, respects nested scopes, and
  rejects visibility, naming, and source-order violations without imposing
  execution order.

Literal-preserving fingerprints checked every mechanical move and rename.
Fixture declarations and initialization order were retained. A final comparison
accounted for every changed body: 87 retained tests have stronger assertions,
and six methods replace the consolidated operator and placeholder cases.
There are no unexplained body changes.

### Consolidation and duplicate review

`OperatorTest`'s 34 methods are now three methods with 61 invocations: an
exhaustive enum case checks each of the 49 independently specified SQL tokens,
`toString`, repeated lookups, and case-insensitive token/name aliases; eleven
cases cover null, unknown, and untrimmed tokens; a uniqueness test checks the
token set. Enum existence and ordinal smoke checks were removed with the
overlapping lookup assertions.

The eleven placeholder tests are now three methods with 51 invocations. They
check exact output for every count from 0 through 31, the previous size 50,
special cached sizes and their neighbors, two negative counts, and insertion
into a VALUES clause. Repeated lookup checks also compare with independently
specified expected output.

The following fifteen other tests had equivalent setup and inputs. The retained
cases have the same or stronger assertions:

| Removed test | Retained test |
| --- | --- |
| `SubQueryTest.testConstructorWithRawSQL` | `testConstructorWithRawSql` |
| `SubQueryTest.testConstructorWithEntityAndSql` | `testConstructorWithEntityNameAndSQL` |
| `SubQueryTest.testParametersWithNoCondition` | `testParametersEmptyForRawSQL` |
| `SqlMapperTest.testConstructor` | `testEmptyConstructor` |
| `SqlMapperTest.testSqlMapper_isEmpty` | `testIsEmpty` |
| `ExceptTest.testGetOperator` | `testConstructor` |
| `IntersectTest.testGetOperator` | `testConstructor` |
| `MinusTest.testGetOperator` | `testConstructor` |
| `UnionAllTest.testGetOperator` | `testConstructor` |
| `UnionTest.testGetOperator` | `testConstructor` |
| `IsNotNullTest.testUseCaseScenario_ValidatedFields` | `testGetPropName` |
| `NamedPropertyTest.testAnyEqualWithArray` | `testAnyEqualWithVarargs`, now checking exact SQL and ordered parameters |
| `QueryUtilTest.testPropertyToColumnMap_MapClass` | `testPropertyToColumnMap_MapEntityClassReturnsEmpty` |
| `QueryUtilTest.testPropertyToColumnMap_NullClass` | `testPropertyToColumnMap_NullEntityClassReturnsEmpty` |
| `OrderByTest.testStaticCreateConditionWithEmptyMap` | `AbstractConditionTest.PropertiesAndSorting.testCreateSortSpecWithEmptyMap` |

The removed `testAnyEqualWithArray` passed individual varargs, exactly as the
retained varargs test did. Actual array, collection, primitive-array, and other
overload cases remain. Similar-looking criteria accessor tests were retained:
they call different accessors. DSL policy variants and distinct SQL inputs also
remain separate.

### Assertion improvements

- 55 expression tests check complete numeric, aggregate, string, arithmetic,
  bitwise, and Boolean SQL, including all operands and their quoting.
- Eleven GROUP BY and nine ORDER BY tests check exact column order, directions,
  expressions, naming-policy conversion, and empty parameter lists. The former
  alternatives accepting either camelCase or snake_case were removed.
- Six moved sorting-helper tests check complete specifications.
- Two structured-subquery tests check exact compound-predicate SQL and ordered
  bindings; two NOT BETWEEN expression tests check SQL and empty bindings.
- The retained membership case checks exact SQL and ordered values, and the
  dynamic zero-placeholder case checks the complete query.

### Verification results

The reorganized suites first passed 1,694 focused invocations. Final verification
used `mvn -o -B clean test jacoco:report`; all audit-tool tests also pass.

| Check | Baseline | Final clean build |
| --- | ---: | ---: |
| Java files under `src/test/java` | 88 | 88 |
| Test methods | 6,034 | 5,980 |
| Executed JUnit invocations | 6,176 | 6,228 |
| Failures / errors / skips | 0 / 0 / 0 | 0 / 0 / 0 |
| Audit-tool regression tests | 10 | 15 |
| Unordered test scopes | 29 | 0 |

The 54 fewer methods come from the operator consolidation (31), placeholder
consolidation (8), and fifteen reviewed removals. More parameterized cases
increase executed invocations by 52. All 5,980 final source methods appear in
the JUnit results; there are no unexpected or stale test methods.

| Coverage counter | Baseline | Final clean build | After focused concurrency check |
| --- | ---: | ---: | ---: |
| Covered lines | 13,583 / 13,719 | 13,583 / 13,719 | 13,583 / 13,719 |
| Covered instructions | 70,452 / 71,110 | 70,451 / 71,110 | 70,452 / 71,110 |
| Covered branches | 12,633 / 13,785 | 12,632 / 13,785 | 12,633 / 13,785 |
| Covered methods | 1,987 / 2,003 | 1,987 / 2,003 | 1,987 / 2,003 |

The clean run differed only at `NamedProperty.of`'s losing-concurrent-insertion
branch. Its existing synchronized first-access test is unchanged apart from its
container; thread scheduling determines whether that branch executes. One
additional `mvn -o -B test jacoco:report -Dtest=NamedPropertyTest` run passed all
106 cases and exercised the branch. Accumulated coverage then matched the
baseline for every production class and method, with no lost covered lines.
The clean-run and accumulated coverage reports are preserved separately; the
comparison does not assume deterministic race-branch coverage on every run.

Final audits report zero exact duplicate groups, zero assertion-free methods,
zero layout violations, and zero unexplained ownership splits. Compile-time
`cs` labels remain the only source unit without a name-matched test class.
`git diff --check` passes.

Logs, source fingerprints, review records, Surefire XML, coverage snapshots,
and the machine-readable comparison are under
`.audit/test-cleanup-2026-10-15`. Routine reproduction commands are documented
in the [audit guide](../../scripts/test_audit/README.md) and
[test conventions](../../src/test/java/README.md).
