# Multi-agent line-by-line review — 2026-06-25

Resumable ledger. Status: pending / in-review / fixed / clean.
Workstreams: (1) bug fixes HIGH-risk, (2) Javadoc/comment comment-only, (3) logging/exception-message LOW-risk.

## Context: what changed since last clean review (2026-06-22)
Uncommitted in-progress refactor: the nested class `SqlBuilder.Dsl` (+ all 15 predefined DSL
constants `PSB/PSC/.../MLC`) was **extracted into a new top-level class `Dsl.java`** (1517 lines).
- `SqlBuilder.java` shrank 1509 lines (nested Dsl + constants + validateColumnAliases removed).
- `AbstractQueryBuilder.java`: `sqlDialect()` getter moved up; class-doc `{@link SqlBuilder.Dsl}`→`{@link Dsl}`.
- `Selection.java`, `SqlDialect.java`: `@see`/prose link updates.
- Tests massively rewritten (SqlBuilderTest 2847 lines, AbstractQueryBuilderTest 366) to use `Dsl.*`.

All other 70+ files unchanged since the 6+ prior clean bug passes (memory: "target only changed code").

## Baseline (BEFORE fix)
- `mvn -o clean test`: **RED** — `Tests run: 4044, Failures: 46, Errors: 537`.
  Every error: `NoClassDefFound: Could not initialize class com.landawn.abacus.query.Dsl`.

## Deliberate-design / known false-positives (from prior ledgers — DO NOT re-report)
empty-expr OK for on()/subQuery(); SqlParser/QueryUtil null-handling deliberate; containsSqlCommentToken
backslash gated to single-quote; blank propName accepted by predicate ctors; from(String) comma scan harmless;
Criteria narrowed-cast CCE unreachable (intentional f765d2f tightening); toString caching REMOVED on purpose;
productInfo()->ProductInfo link intentional; SqlDialect default-ctor doclint warning benign;
QueryUtil Collection<String>[] widening safe.

## WORKSTREAM 1 — BUGS

### [CRITICAL / CONFIRMED] Dsl static-initialization NPE — FIXED
- **File:** `Dsl.java`. `dslCache` (`ConcurrentHashMap`) was declared at L141, *after* the 15
  `public static final Dsl` constants (L53-139) whose initializers call `forDialect(...)`, which
  dereferences `dslCache`. Java runs static field initializers in textual order → `dslCache` is still
  `null` when `PSB = forDialect(...)` runs → NPE → `ExceptionInInitializerError`, then
  `NoClassDefFoundError: Could not initialize class ...Dsl` on every later access.
- **Repro:** entire suite RED (537 errors, all "Could not initialize class Dsl"). Confirmed by main agent.
- **Fix:** moved the `dslCache` field declaration to the top of the class (before the constants). During
  constant init the map is empty → `forDialect` returns `new Dsl(...)`; the trailing static block then
  registers the 15 canonical instances into the cache (unchanged). Minimal, local; preserves the
  intended caching semantics.
- **Verified:** `mvn -o clean test` after fix = **GREEN, Tests run: 4864, 0/0/0, BUILD SUCCESS**
  (== the 4864 baseline from 2026-06-22). RED→GREEN.

### Behavioral-equivalence proof of the Dsl extraction
Normalized method-body diff of old nested `SqlBuilder.Dsl` (git HEAD) vs new `Dsl.java`: the ONLY
differences are the 4 intended ones — (a) `forDialect` cache lookup, (b) new `sqlDialect()` getter,
(c) private `namingPolicy()` method → `namingPolicy` field set in ctor, (d) `validateColumnAliases`
moved in. All 44 statement methods otherwise byte-identical. Extraction is behavior-preserving.

## WORKSTREAM 2 — Javadoc (stale refs to symbols removed by the refactor) — FIXED
The constants/class moved SqlBuilder→Dsl; these explicit `SqlBuilder.Dsl` / `SqlBuilder.<CONST>`
references no longer compile / resolve. Fixed (all comment-only):
- `Selection.java` L185, L399, L405, L408: `SqlBuilder.PSC`/`SqlBuilder.NSC` → `Dsl.PSC`/`Dsl.NSC` (3 `{@code}` examples + @param prose).
- `AbstractQueryBuilder.java` L3114, L3210: `SqlBuilder.Dsl oracleDsl = SqlBuilder.Dsl.forDialect(...)` → `Dsl oracleDsl = Dsl.forDialect(...)`.
- `SqlDialect.java` L40, L44: prose `{@link SqlBuilder} DSL constants` → `{@link Dsl} constants`; example `SqlBuilder.Dsl myDsl = SqlBuilder.Dsl.forDialect` → `Dsl myDsl = Dsl.forDialect`.
- `SqlBuilder.java` L71: table `<caption>Predefined SqlBuilder.Dsl constants</caption>` → `Predefined Dsl constants`.
- `SortDirection.java` L39: `SqlBuilder.PSC.selectFrom` → `Dsl.PSC.selectFrom`.
- `condition/Condition.java` L57: `SqlBuilder.PSC.select` → `Dsl.PSC.select`.
Note: bare unqualified `PSC.select(...)`/`NSC.select(...)` examples were left as-is — they were already
unqualified before the refactor (illustrative, never said `SqlBuilder.PSC`), so still valid.

## Partition / agent coverage (read-only agents cross-check; main agent edits)
| Batch | Agent | Scope | Status |
|-------|-------|-------|--------|
| 1 | A | Dsl.java + SqlBuilder.java — Javadoc accuracy + any 2nd correctness bug | dispatched |
| 1 | B | AbstractQueryBuilder/Selection/SqlDialect/SortDirection changed regions | dispatched |
| 2 | C | condition/* (all) — confirmatory; rename doc-drift + NEW high-conf bugs only | pending |
| 2 | D | Filters/DynamicQuery/QueryUtil/SqlParser/ParsedSql/SqlMapper/SqlOperation — same | pending |

## Agent results (all 4 dispatched, READ-ONLY)
- Batch1-A (Dsl + SqlBuilder): no 2nd correctness bug. dslCache leak-free (only reads; seed-only puts).
  SqlDialect valid map key (@Value over productInfo record + 3 enums). All @throws match N.checkArg*;
  message constants all semantically correct (no copy-paste). appendCondition family correct. Flagged 2
  stale prose refs in Dsl.java (L43, L177 said constants "on {@link SqlBuilder}") -> FIXED.
- Batch1-B (AQB/Selection/SqlDialect/SortDirection): sqlDialect() appears exactly once (clean move, not
  duplicated). All {@link Dsl#PSC..} resolve. Pagination examples internally consistent. Zero remaining
  SqlBuilder.Dsl / SqlBuilder.<CONST> refs.
- Batch2-C (condition/*, 65 files): condition package ZERO stale references.
- Batch2-D (Filters/DynamicQuery/QueryUtil/SqlParser/ParsedSql/SqlMapper/SqlOperation): ZERO stale references.

## WORKSTREAM 3 — logging/exception messages
No logging added/changed. Exception messages in changed code verified accurate (validateColumnAliases,
Junction "must contain at least one element", "Unsupported condition type", forDialect/checkArg messages).
Nothing to fix.

## Regression test (RED->GREEN proven)
Added `SqlBuilder2026DialectBugFixTest#testPredefinedDslConstantsInitializeAndForDialectCaches`
(SqlBuilderTest.java) — touches all 15 Dsl constants + asserts forDialect cache returns the canonical
PSC for an equal dialect and a fresh instance for a custom dialect.
- Against REINTRODUCED bug: ERROR `ExceptionInInitializerError` (1 error). [RED, verified]
- Against the fix: PASS (Tests run: 1, 0/0). [GREEN, verified]

## Edits applied
1.  Dsl.java — move dslCache decl above the constants (CRITICAL static-init fix)
2.  Selection.java L185 — SqlBuilder.PSC -> Dsl.PSC ({@code} example)
3.  Selection.java L399/405/408 — SqlBuilder.PSC/NSC -> Dsl.PSC/NSC (2 examples + @param)
4.  AbstractQueryBuilder.java L3114 — SqlBuilder.Dsl ... forDialect -> Dsl ... forDialect
5.  AbstractQueryBuilder.java L3210 — SqlBuilder.Dsl ... forDialect -> Dsl ... forDialect
6.  SqlDialect.java L40/L44 — prose {@link SqlBuilder}->{@link Dsl}; example SqlBuilder.Dsl -> Dsl
7.  SqlBuilder.java L71 — caption "Predefined SqlBuilder.Dsl constants" -> "Predefined Dsl constants"
8.  SortDirection.java L39 — SqlBuilder.PSC.selectFrom -> Dsl.PSC.selectFrom
9.  condition/Condition.java L57 — SqlBuilder.PSC.select -> Dsl.PSC.select
10. Dsl.java L43 — "constant on {@link SqlBuilder}" -> "constant on this class"
11. Dsl.java L177 — "constants on {@link SqlBuilder}" -> "constants on {@link Dsl}"
12. SqlBuilderTest.java — new regression test (see above)

## Final build verification
- `mvn -o clean compile`: GREEN (exit 0)
- `mvn -o clean test`: GREEN — **Tests run: 4865, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS**
  (== 4864 prior baseline + 1 new regression test). Was RED (4044/46/537) before the fix.
- `mvn -o javadoc:jar` (doclint): GREEN — only the known-benign `SqlDialect.java:66` default-constructor
  warning (Lombok @Builder; pre-existing, see memory javadoc-doclint-productinfo-lombok). No broken links.

## STATUS: COMPLETE
1 CRITICAL bug found+fixed (Dsl static-init NPE — whole library was unloadable), proven RED->GREEN with a
new regression test. Dsl extraction proven behavior-preserving (normalized diff). 11 comment-only Javadoc
stale-ref fixes from the SqlBuilder->Dsl move. Zero other bugs (4 agents + own trace). All gates green.
