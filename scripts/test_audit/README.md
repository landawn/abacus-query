# Test audit tools

These dependency-free Python tools inventory the current Java tests, identify
duplicate candidates, and map tests to their production units.

Run from the repository root:

```sh
python -B -m unittest discover -s scripts/test_audit -p 'test_*.py'
python -B scripts/test_audit/parse_tests.py --out .audit/test-audit/parse_output.json
python -B scripts/test_audit/find_duplicates_intra.py --exact-only --min-body-len 0 --out .audit/test-audit/intra_exact.json
python -B scripts/test_audit/find_duplicates.py --out .audit/test-audit/cross_class.json
python -B scripts/test_audit/find_no_assertions.py --out .audit/test-audit/no_assertions.json
python -B scripts/test_audit/map_consolidation.py --out .audit/test-audit/consolidation.json
python -B scripts/test_audit/check_test_layout.py --out .audit/test-audit/layout.json
```

The report commands default to the freshly generated inventory under
`.audit/test-audit`. Pass `--input` to use another inventory. The older JSON
files alongside these scripts are historical snapshots, not current results.

## Inventory and duplicate detection

`java_tokens.py` masks comments, strings, character literals, and text blocks
for source discovery while retaining their original positions. `parse_tests.py`
records nested class paths, inherited tags, annotated methods, and assertion
calls. Assertion words appearing only in comments or strings do not count.

Duplicate fingerprints ignore signatures, comments, and code formatting while
preserving SQL strings, character values, and code token boundaries. The
duplicate tools also compare parameter signatures and annotations, including
parameter sources. A different method name alone does not hide a duplicate.

- `find_duplicates_intra.py --exact-only` compares methods in the same nested
  class. Use `--min-body-len 0` to include short validation cases.
- `find_duplicates.py` reports matching bodies across classes. Similar-looking
  tests can use different fields, fixtures, policies, or lifecycle setup.
- `--fuzzy` / `--sim-threshold` are review aids, not deletion instructions.
- No duplicate report authorizes automatic deletion. Retain the stronger case
  only after checking setup, exercised overloads, annotations, and expectations.

`find_no_assertions.py` checks all tests by default. Its optional
`--tagged-only` filter recognizes inherited `unit` and legacy tags, but tag
inspection is not a substitute for actual JUnit discovery.

`map_consolidation.py` assigns nested behavior groups to their enclosing unit,
prefers an exact production-class name, excludes fixture-only classes, and
documents the supported-SQL and pagination splits.
Dependency contracts such as `SKTest` are reported separately. A missing
name-based mapping does not establish missing runtime coverage.

`check_test_layout.py` checks case-insensitive method-name ordering separately
within each top-level or nested test class, package-private visibility, the
`test` prefix, and obsolete pass/batch labels. It includes parameterized tests,
ignores helpers, and returns a nonzero exit code for violations. It reads current
sources by default; pass `--input` to check a saved inventory. This is a source
navigation convention, not a JUnit execution-order requirement.

## Legacy merge utilities

`java_struct.py` and `split_members.py` support the older top-level
`TestBase` class merge tools:

```sh
python -B scripts/test_audit/merge_file.py path/to/FooTest.java
python -B scripts/test_audit/merge_all.py
python -B scripts/test_audit/merge_across_files.py --target path/to/FooTest.java --source path/to/LegacyTest.java
```

These commands default to a dry run. `--apply` writes the changes; the
cross-file tool then deletes the merged source files. Any colliding member name
aborts before writing that merge. Resolve collisions explicitly rather than
choosing a winner by name or test length. The batch collision log is written
under `.audit/test-audit` only with `--apply`.

The merge tools do not establish that different fixtures, superclass behavior,
or lifecycle methods are equivalent. Review those dependencies and verify
discovery and coverage after a merge.

## Verification

```sh
mvn clean test jacoco:report
```

The suite selects `com.landawn.abacus.query` and its subpackages and excludes
`slow-test`. Year tags are no longer required. Compare executed invocations
and coverage, not just source method counts: parameterized tests run multiple
cases. See [the test conventions](../../src/test/java/README.md).
