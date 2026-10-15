# Query unit tests

Run the suite with `mvn test`. Run `mvn clean test jacoco:report` for a fresh
coverage report. Maven failures fail the build, and its JVM arguments retain
the JaCoCo agent. The compiler target is Java 21.

After moving or renaming test classes, run `mvn clean test` once to remove stale
compiled test classes before using incremental `mvn test` runs.

`AbacusQueryTestSuite` selects `com.landawn.abacus.query` and its subpackages,
including nested tests, and excludes `slow-test`. Discovery does not require a
year tag or inheritance from `TestBase`; the latter supplies shared assertions
and the optional `unit` tag.

## Ownership and organization

- Keep tests in the package of the production unit they exercise.
- Use `<Unit>Test` as the main owner. `DslTest` covers the DSL factory variants;
  `SqlBuilderTest` covers built queries; `AbstractQueryBuilderTest` covers the
  shared builder operations and lifecycle.
- Group large classes with `@Nested` behavior containers. Keep overloads and
  related scenarios together, and sort test methods by name within a group.
  Source order is for navigation; tests must not rely on execution order.
- Use package-private `test<Method><Scenario>` methods with JUnit annotations.
  Preserve descriptive regression names and comments explaining the contract.
  Name the behavior rather than the pass or batch that introduced the test.
- Put setup fields and lifecycle methods first, tests next, and private helpers
  and entity fixtures last. Preserve entity field order: it can affect SQL.
- Use explicit static JUnit assertion imports. Keep expected values before
  actual values. Prefer exact SQL **and ordered bindings**, using
  `QueryTestSupport.assertSql`, when both belong to the contract.
- Keep materially different entities, policies, overloads, and validation
  precedence cases distinct. Parameterize cases only when setup and assertions
  are equivalent; give each invocation a descriptive name.
- Use fixed dates and `@TempDir` where applicable. Restore any global state in
  `finally` or lifecycle cleanup.

Some suites intentionally remain separate: `SqlBuilderSupportedSqlTest` is the
executable supported-SQL catalog, and `SqlDialectPaginationTest` covers pagination.
`ParsedSqlTest` groups parser behaviors, including internal scanner preconditions
in `Validation`. `SKTest` tests the `abacus-common` keyword constants used by query code.

Shared query assertions, selection construction, and the shared `Order` entity
live in `QueryTestSupport`. The `entity` package contains reusable domain
fixtures. Manual Java maintenance tools live under `scripts/java`, outside test
compilation and discovery.

`AbstractConditionTest.PropertiesAndSorting` owns the `createSortSpec` helper
contracts; `GroupByTest` and `OrderByTest` own their clause behavior. The other
large condition and query suites use nested groups for construction, rendering,
parameters, validation, and their unit-specific operations. Keep static method
sources with their parameterized tests when introducing a nested group.

## Auditing changes

See [the audit tools](../../../scripts/test_audit/README.md). Generate reports
from current sources; historical JSON files are not a discovery baseline.
Compare JUnit invocations, not just `@Test` counts: parameterized tests have
multiple invocations. Review duplicate candidates against their fixtures,
annotations, and expected behavior before removing anything.

The [initial cleanup record](../../../docs/testing/test-cleanup-2026-10-12.md)
and [second-pass record](../../../docs/testing/test-cleanup-2026-10-13.md) record
the earlier consolidation. The [2026-10-15 cleanup record](../../../docs/testing/test-cleanup-2026-10-15.md)
records the current grouping, semantic duplicate review, stronger assertions,
and before/after verification. Run `python -B scripts/test_audit/check_test_layout.py`
from the repository root to check naming, visibility, and ordering conventions.
