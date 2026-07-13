# Refreshed code/documentation review - 2026-07-12

This is the final refreshed result for the current combined `HEAD..working tree`. It supersedes the 2026-07-10 report result while retaining that report as the historical audit trail.

## Verdict

All implementation fixes in the current 34-path patch compile and pass the complete test suite. One confirmed patch-scoped issue remains: the hand-maintained AI API artifacts are semantically stale. Several previously reported implementation edge cases remain because this patch did not address them.

## Findings

| ID | Location | Severity / confidence | Result |
|---|---|---|---|
| R001 | `docs/ai/API.md:511,534,557,580,603,3088`; `docs/ai/api-index.json:2088,2182,2276,2370,2464,12469` | medium / confirmed | The artifacts still advertise the five removed set-operation `String...` overloads and removed `QueryUtil.prop2ColumnNameMap(...)`; they omit `propToColumnInfoMap(...)`. Regenerate both artifacts from the current public API. |
| R002 | `AbstractQueryBuilder.toInsertPropsList` (`AbstractQueryBuilder.java:6243`) | high / confirmed | Heterogeneous bean batches still apply the first bean class's ID metadata to every row when deciding which default ID columns to remove. |
| R003 | `SqlParser.hashIdentifierContextKeywords` (`SqlParser.java:117`) | medium / confirmed | Optional-preposition `INSERT`, `DELETE`, and `MERGE` forms using SQL Server `#temp` identifiers remain uncovered. |
| R004 | `AbstractQueryBuilder` FROM/alias scanners | medium / confirmed | Alias discovery remains incomplete for quoted parentheses/spaces and nested text containing ` AS `. |
| R005 | `AbstractQueryBuilder.appendIf` (`AbstractQueryBuilder.java:3899,3928`) | low / confirmed | `appendIf(false, ...)` still bypasses the closed-builder lifecycle check. |
| R006 | Cross-cutting condition/SQL edge cases | mixed / retained | Raw outer-join detection, backslash-escaped literal portability, bare `SubQuery` predicates, the `Limit` maximum-integer sentinel, quoted `Using` identifiers containing commas/parentheses, and protected between/in operator invariants remain open from the earlier audit. |

## Closed by the current fixes

- Derived-table comma followed by a SQL Server `#temp` identifier.
- PostgreSQL JSON/JSONB `?` before function and `CAST` right-hand sides.
- Non-atomic `NamedProperty` first-access cache publication.
- The `QueryUtil` column-to-property exclusion concern, now regression-tested and closed as a false positive.

The refresh also verified collection/map snapshots, set-operation state validation and overload cleanup, `Criteria` defensive copying/select modifiers, atomic query-metadata cache publication, XML attribute validation, and malformed database-version rejection.

## Verification

| Check | Result |
|---|---|
| Test compilation | passed |
| Focused changed-area tests | passed |
| Clean full suite | 2,894 tests; 0 failures; 0 errors; 0 skipped |
| Javadocs | passed |
| API index JSON parse | passed |
| Diff whitespace check | passed; only line-ending/global-ignore warnings |

No production or test code was changed during this refresh.
