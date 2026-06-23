# Multi-agent line-by-line review — 2026-06-22

Resumable ledger. Status: pending / in-review / fixed / clean.
Workstreams: (1) bug fixes HIGH-risk, (2) Javadoc/comment comment-only, (3) logging/exception-message LOW-risk.

## Baseline
- `mvn -o clean compile` : GREEN (exit 0)
- `mvn -o test` baseline : GREEN — 4864 testcase elements, 0 failures / 0 errors / 0 skipped (surefire @Suite header tests=2431 is a JUnit-Platform suite quirk; use 0/0/0 as health metric).
- Uncommitted working-tree changes (PRESERVE): `productInfo()` -> `SqlDialect.ProductInfo` javadoc link fix in AbstractQueryBuilder, Filters, condition/Limit. Consistent with javadoc-doclint-productinfo-lombok memory.

## Focus weighting (from prior ledgers)
- Condition package deeply bug-reviewed 5+ times -> bug yield near zero; real bugs always in SqlDialect-era code (SqlBuilder/AbstractQueryBuilder).
- HIGHEST bug risk = files touched by f765d2f "API consistency & ergonomics pass" (+3532 lines): SqlMapper(+268), SqlParser(+273), NamedProperty(+120), Using(+64), SortDirection(+43), Criteria, Binary, Junction, Cell, Join, AbstractIn, Operator, condition set-op/join classes.
- Javadoc commits 387804c/1bcbc3f touched many condition leaf classes -> Javadoc accuracy/consistency pass warranted there.

## Deliberate-design / known false-positives (DO NOT re-report)
See agent prompts. Summary: empty-expr OK for on()/subQuery(); SqlParser/QueryUtil null-handling deliberate; containsSqlCommentToken backslash gated to single-quote (defensive guard, don't touch); blank propName accepted by predicate ctors; IN() only via Kryo; from(String) comma scan harmless; Criteria param-vs-toString divergence unreachable; StringBuilder cap overflow non-bug; On via Cell -> ON (a=b) valid; toString caching REMOVED on purpose (don't reintroduce/flag); productInfo()->ProductInfo link intentional; SqlDialect default-ctor doclint warning benign.

## Partition (19 agents, no overlap/gaps; 77 files)
| Agent | Files | Status |
|-------|-------|--------|
| A1 | SqlBuilder.java, SqlDialect.java | CLEAN (all 3 workstreams) |
| A2 | AbstractQueryBuilder.java L1-3150 | reviewed: 1 low javadoc gap (limit(int) @throws OFFSET-slot) |
| A3 | AbstractQueryBuilder.java L3150-6230 | reviewed: 1 confirmed javadoc fix (union(String...) garbled sentence 3813-3816) |
| B1 | Filters.java | CLEAN (minor: subquery-family @throws-null doc gap, optional) |
| B2 | SqlParser.java, ParsedSql.java | CLEAN (cosmetic: ParsedSql:156 exception msg uses untrimmed `sql`) |
| B3 | SqlMapper.java, SqlOperation.java, SortDirection.java | CLEAN (all 3 workstreams) |
| C1 | DynamicQuery.java, Selection.java | CLEAN (javadoc: DynamicQuery:304 limit(int,int) OFFSET-omitted-when-0 gap) |
| C2 | QueryUtil.java | CLEAN (javadoc: QueryUtil:104-107 slot enumeration omits slot 3) |
| C3 | condition/Criteria.java | CLEAN (B-1 suspected CCE on getWhere() narrowed cast — unreachable w/ lib types, by-design tightening from f765d2f) |
| D1 | condition/Expression.java | CLEAN (all 3 workstreams) |
| D2 | condition/NamedProperty.java | CLEAN (all 3 workstreams; new fluent methods correctly wired) |
| D3 | condition/AbstractCondition.java, Condition.java, Operator.java | CLEAN (Operator map no collisions; of(null)->null doc updated correctly) |
| E1 | Binary, Equal, NotEqual, GreaterThan, GreaterThanOrEqual, LessThan, LessThanOrEqual, Like, NotLike | CLEAN (all operators correctly wired+documented) |
| E2 | Is, IsNot, IsNull, IsNotNull, IsNaN, IsNotNaN, IsInfinite, IsNotInfinite | CLEAN (@see symmetry complete, NOT-variants negate correctly) |
| E3 | AbstractIn, In, NotIn, AbstractInSubQuery, InSubQuery, NotInSubQuery, AbstractBetween, Between, NotBetween | CLEAN (NOT-variants correct, param-collection complete) |
| F1 | Join, InnerJoin, LeftJoin, RightJoin, FullJoin, CrossJoin, NaturalJoin, On, Using | CLEAN (every {@code} example constructs+matches; @throws ON/USING wording correct) |
| F2 | Junction, And, Or, Cell, ComposableCell, Clause, ComposableCondition, Not | CLEAN (no mutable leak; parenthesization correct) |
| F3 | Union, UnionAll, Except, Intersect, Minus, SubQuery, Exists, NotExists, All, Any, Some | CLEAN (all set-op/quantifier operators correctly wired+documented) |
| F4 | GroupBy, OrderBy, Having, Where, Limit | CLEAN (ASC/DESC + Limit rendering correct; minor Limit blank-msg wording) |

## Consolidated findings (all 19 agents)
WORKSTREAM 1 (BUGS): **ZERO confirmed bugs.** Consistent with the codebase's 5+ prior clean bug passes. The heavily-changed f765d2f code (SqlMapper/SqlParser/NamedProperty/Using/condition classes) all traced correct: operators correctly wired, NOT-variants negate, param-collection complete, no copy-paste errors.

WORKSTREAM 2 (Javadoc) — fixed:
- AQB:3814-3816 union(String...) garbled self-contradictory sentence ("...collection is *not* a workaround...") -> rewritten affirmatively. [main verified vs code]
- DynamicQuery:304 limit(int,int) doc claimed unconditional `LIMIT count OFFSET offset` but code omits OFFSET when offset==0 -> clarified. [verified: line 327 `if (offset > 0)`]
- QueryUtil:104-105 + 118 noExclusionPropNamesPool slot enumeration omitted slot 3 (insert-without-id) -> added. [verified: line 471 `baseSlot = 3` when all id props default]

WORKSTREAM 3 (exception messages) — fixed:
- Limit.normalizeExpression:427/433 both said "null or empty" but line 433 fires only on blank input -> unified to "null, empty, or blank" (matches public @throws line 180). [no test asserts on the string]

## Deferred / report-only (not fixed — rationale)
- Filters subquery-family factories (some/any/all/exists/notExists/union/unionAll/intersect/except/minus) omit `@throws IAE if subQuery is null` though the ctor throws. Consistent across the whole family (omission, not divergence); fixing spans 10+ overloads. Deferred to keep diff focused per scope guardrails.
- AQB:3116 limit(int) @throws: re-examined, ACCURATE as-is (body already documents OFFSET-slot consumption; limit() itself never throws on a pre-set OFFSET). No change.
- ParsedSql:156 malformed-iBatis exception interpolates untrimmed `sql` param. KEPT — showing the raw user input verbatim in a parse error is more helpful than the trimmed form; not a defect.

## New false-positives appended to deliberate-design list
- Criteria.getWhere()/getGroupBy()/getHaving()/getOrderBy() narrowed casts (Clause->concrete type, from f765d2f) can CCE only for a hand-rolled Clause subtype carrying a WHERE/etc operator that isn't the canonical class. Unreachable with library types; intentional API tightening. Do NOT "fix".
- QueryUtil local `Collection<String>[] val = loadPropNamesByClass(...)` (actual return Set<String>[]) is a safe widening — non-bug.

## Edits applied (all comment/message-only; no behavior change)
1. AbstractQueryBuilder.java — union(String...) javadoc sentence rewrite
2. DynamicQuery.java — limit(int,int) javadoc OFFSET-omission note
3. QueryUtil.java — slot enumeration (field comment + @param) add slot 3
4. condition/Limit.java — normalizeExpression message "null, empty, or blank" (x2)
(plus pre-existing uncommitted productInfo()->ProductInfo javadoc links in AQB/Filters/Limit, preserved)

## Build verification
- mvn -o clean compile: GREEN
- mvn -o javadoc:jar (doclint=all): GREEN (exit 0; pre-existing benign SqlDialect default-ctor warning only)
- mvn -o test (after edits): GREEN — 4864 testcases, 0 failures / 0 errors / 0 skipped (== baseline). Trailing stack traces in after_test.log are EXPECTED logged exceptions from negative-path tests (SqlMapper2026Test.testLoad_MalformedXmlWrapsParsingException @ SqlMapperTest.java:1017, testLoad_MissingFileWrapsIOException @ :1006); both tests PASS. Same noise present in baseline.

## Regression tests
NONE added — all 4 edits are comment/exception-message-only with zero behavior change (Limit still throws IAE for the same inputs; no test asserts the old string). No behavioral code fix was made, so there is nothing to RED/GREEN.

## STATUS: COMPLETE. All 19 partitions reviewed (no gaps/overlaps); 4 comment/message fixes applied; compile + javadoc(doclint) + full suite all green.
