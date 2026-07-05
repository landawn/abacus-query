# test_audit — reusable scripts for the three-part test-suite cleanup

These scripts parse the JUnit test suite under `src/test/java`, report duplicate
test methods, report test methods without meaningful assertions, map test
classes to the source classes they test, and merge multiple test classes in one
file (or scattered across files) into a single canonical `FooTest.java`.

All scripts are plain Python 3 (no dependencies). Run them from the repo root.

## Scripts

### `parse_tests.py`
Parses every `@Test` method in `src/test/java` and emits a JSON dump:
`{files, classes, methods}` where each method has `file`, `class_name`,
`class_tag`, `name`, `sig_line`, `sig_body`, `norm_body` (comments stripped,
string/char literals collapsed, whitespace normalized — for dup detection), and
`has_assertion`.

```bash
python scripts/test_audit/parse_tests.py --out scripts/test_audit/parse_output.json
```

### `find_duplicates.py`
Exact-match (default) or fuzzy (`--fuzzy`) duplicate detection across the whole
suite. Groups methods whose normalized bodies are identical (or >= similarity
threshold). Use exact match for true duplicates; fuzzy mostly finds parallel
structure across different classes (NOT duplicates).

```bash
python scripts/test_audit/find_duplicates.py --out scripts/test_audit/duplicates.json --show-body
```

### `find_duplicates_intra.py`
Duplicate detection **within the same test class** — the right tool after
consolidation, when every source class has one test class. Reports methods in
the SAME class with identical (`--exact-only`) or highly similar bodies but
different names. `--exact-only` is the reliable signal; fuzzy catches
parallel-structure tests for different methods (usually distinct, not dups).

```bash
python scripts/test_audit/find_duplicates_intra.py --exact-only --min-body-len 60 --out scripts/test_audit/intra_exact.json
```

### `find_no_assertions.py`
Reports `@Test` methods whose body contains no meaningful assertion call
(assertEquals/assertThrows/.../any custom `assert*`/`expect*`/`verify*` helper).
`--tagged-only` (default) restricts to classes that actually run under `mvn test`
(`@Tag("base-test")`/`@Tag("2025")`); `--include-untagged` checks all.

```bash
python scripts/test_audit/find_no_assertions.py --include-untagged --out scripts/test_audit/no_assertions_all.json
```

### `map_consolidation.py`
Maps every test class to the source class it tests (by stripping
`Test/2025Test/2026Test/BatchTest/...` suffixes) and reports:
- `cross_file_scattering` — methods for one source class spread across >1 file
- `multi_class_one_file` — one FooTest.java holding >1 class testing the same source
- `orphan_test_classes` — test classes with no matching source (mostly nested helpers)
- `untested_source_classes` — source classes with no test class

```bash
python scripts/test_audit/map_consolidation.py --out scripts/test_audit/consolidation_map.json
```

### `java_struct.py`
Structural parser for a single Java test file: splits into `package`,
`imports`, and top-level `ClassInfo` objects (each with `leading_annotations`,
`signature`, `file_body`, `methods`). Used by the merge scripts. CLI:
```bash
python scripts/test_audit/java_struct.py path/to/FooTest.java
```

### `split_members.py`
Splits a class body into its direct member blocks (`method` / `class` /
`field` / `init-block`), each with verbatim `text` and `name`. Used by the merge
scripts.

### `merge_file.py`
Merges all top-level test classes (extends `TestBase`) in ONE file into a single
canonical class named after the file stem, tagged `@Tag("2025")` and made
`public`. Method-name collisions keep the canonical (tagged, known-passing)
version. Non-test top-level classes are preserved. Collision report on stderr.
```bash
python scripts/test_audit/merge_file.py path/to/FooTest.java --apply
```

### `merge_all.py`
Batch driver: applies `merge_file.py` to every test file that currently has
>1 top-level test class. Re-runnable (single-class files are skipped).
```bash
python scripts/test_audit/merge_all.py --apply
```

### `merge_across_files.py`
Cross-file consolidation: merges test classes from one or more `--source` files
into a `--target` file, unions imports, dedups method names (target wins), and
deletes the source files. Used for the sanctioned SqlParser split
(SqlParserCoverageTest.java -> SqlParserTest.java).
```bash
python scripts/test_audit/merge_across_files.py \
    --target src/test/java/.../SqlParserTest.java \
    --source src/test/java/.../SqlParserCoverageTest.java --apply
```

## Reports written

- `parse_output.json` — full parse dump
- `duplicates.json` — cross-suite exact duplicates
- `intra_exact.json` — intra-class exact duplicates
- `no_assertions_all.json` — methods without assertions
- `consolidation_map.json` — consolidation mapping report
- `merge_collisions.txt` — per-file collision log from `merge_all.py`

## Convention

The merge produces `@Tag("2025") public class FooTest extends TestBase` so every
merged class runs under `mvn test` (the suite `@IncludeTags({"base-test","2025"})`).
Previously-untagged legacy/batch methods therefore start running; verify with
`mvn clean test` after merging.