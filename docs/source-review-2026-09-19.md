# Source review — 2026-09-19

Four agents reviewed all 81 Java source files under `src/main/java`, including nested types, implementation paths, Javadocs, and both package descriptions. Review areas were SQL builders/dialects, conditions, query APIs/mapping, and parsing/mapping utilities. Findings were cross-checked against the existing tests and dependency behavior before changing code.

## Fixed defects

| Defect | Result | Regression coverage in existing classes |
| --- | --- | --- |
| Raw subquery bindings lost the enclosing builder's parameter-policy requirement | Later composition rejects incompatible placeholder policies and preserves binding order | `SqlBuilderTest`: sibling set operations, derived tables, reusable snapshots |
| Explicit mapped projection aliases disappeared with `NO_CHANGE` naming | Explicit aliases are emitted for direct and qualified properties under every naming policy | `SqlBuilderTest`: positional, named, and MyBatis policies; map and expression aliases |
| `USING` columns inherited the current table alias | Entity column mappings are retained without automatic qualification; alias state is restored afterward | `SqlBuilderTest`: string, parenthesized, varargs, collection, condition, Criteria, and failure-recovery paths |
| Independent set-operation operands leaked their own compound/order/pagination scope into the parent | Structured operands requiring isolation are rejected before composition; callers can explicitly wrap a child in a derived-table SELECT | `SqlBuilderTest`: five set operations, child reuse, snapshots, structured subqueries, explicit isolation, binding collisions, rollback |
| Dots inside quoted mapped identifiers were treated as qualifiers | Alias detection distinguishes quoted dots from actual qualification, including nested entity mappings | `QueryUtilTest`: double quotes, backticks, brackets, escaped delimiters, and truly qualified controls |
| Standalone array subscripts bypassed mixed-parameter validation | Recognized subscripts inspect all marker styles consistently while preserving quoted/commented markers | `ParsedSqlTest`: positional/named/MyBatis mixtures and literal/identifier controls |
| Token scanners disagreed on whitespace-prefixed custom separators | Tokenization, next-token scanning, token bounds, and token search honor the same longest-match rule | `SqlParserTest`: space, tab, newline, carriage return, and form-feed prefixes |
| Scalar value operands accepted known multi-column structured subqueries | Scalar comparisons, bounds, and membership values require a one-column known projection | `AbstractConditionTest`: rejection paths plus single-column, raw/wildcard, and row-valued IN compatibility |
| SQL clause recognition accepted Java enum aliases as SQL keywords | Identifiers such as `order_by` and `union_all` remain valid expressions; real SQL clause prefixes remain recognized | `AbstractConditionTest`: 11 identifier names, WHERE, ORDER BY, predicate composition, and keyword controls |

## Javadoc improvements

Updated scalar-subquery exception contracts across constructors and Filters factories; clarified fluent return types, placeholder ownership, raw-fragment validation, entity naming behavior, snapshot ownership, quoted identifier qualification, USING rendering, set-operation isolation, and terminal builder methods. Corrected a misplaced parser helper comment and inaccurate portability wording for LIMIT.

The review also clarified existing intentional boundaries: direct condition `toSql` rendering is diagnostic and does not resolve trailing condition identifiers against entity column annotations; raw set-operation strings retain caller-selected grouping; arithmetic expression helpers concatenate operands and require explicit grouping for compound expressions. These behaviors were documented rather than silently redefined.

A suspected Selection defensive-copy defect was ruled out by inspecting the installed dependency implementation. No unnecessary runtime change was retained.

## Validation

- Original configured Maven suite: 5,186 tests, no failures or errors.
- Isolated original-source comparison: 19 new test methods, 14 expected failures and five passing compatibility/recovery controls. Every defect category above has a failing regression against the original sources.
- Final configured Maven suite (`mvn -o test`): 5,205 tests, no failures, errors, or skipped tests.
- Fresh Javadoc generation with configured `doclint=all` (`mvn -o javadoc:jar -Dmaven.javadoc.skip=false -DstaleDataPath=target/review-javadoc-final-stale.txt`): successful. One warning remains on unchanged Lombok-generated `SqlDialect` constructor documentation; no constructor/API change was introduced to suppress it.
- `git diff --check`: clean.

Tests verify generated SQL, parameters, validation, and state recovery; no live database execution was performed. The existing Maven configuration ignores test failures when choosing its exit status, so verification also inspects actual failure/error totals.

## File coverage

All files listed below were reviewed; unchanged files had no additional confirmed defect requiring a change.

- [x] `com/landawn/abacus/query/AbstractQueryBuilder.java`
- [x] `com/landawn/abacus/query/condition/AbstractBetween.java`
- [x] `com/landawn/abacus/query/condition/AbstractCondition.java`
- [x] `com/landawn/abacus/query/condition/AbstractIn.java`
- [x] `com/landawn/abacus/query/condition/AbstractInSubQuery.java`
- [x] `com/landawn/abacus/query/condition/All.java`
- [x] `com/landawn/abacus/query/condition/And.java`
- [x] `com/landawn/abacus/query/condition/Any.java`
- [x] `com/landawn/abacus/query/condition/Between.java`
- [x] `com/landawn/abacus/query/condition/Binary.java`
- [x] `com/landawn/abacus/query/condition/Cell.java`
- [x] `com/landawn/abacus/query/condition/Clause.java`
- [x] `com/landawn/abacus/query/condition/ComposableCell.java`
- [x] `com/landawn/abacus/query/condition/ComposableCondition.java`
- [x] `com/landawn/abacus/query/condition/Condition.java`
- [x] `com/landawn/abacus/query/condition/Criteria.java`
- [x] `com/landawn/abacus/query/condition/CrossJoin.java`
- [x] `com/landawn/abacus/query/condition/Equal.java`
- [x] `com/landawn/abacus/query/condition/Except.java`
- [x] `com/landawn/abacus/query/condition/Exists.java`
- [x] `com/landawn/abacus/query/condition/FullJoin.java`
- [x] `com/landawn/abacus/query/condition/GreaterThan.java`
- [x] `com/landawn/abacus/query/condition/GreaterThanOrEqual.java`
- [x] `com/landawn/abacus/query/condition/GroupBy.java`
- [x] `com/landawn/abacus/query/condition/Having.java`
- [x] `com/landawn/abacus/query/condition/In.java`
- [x] `com/landawn/abacus/query/condition/InnerJoin.java`
- [x] `com/landawn/abacus/query/condition/InSubQuery.java`
- [x] `com/landawn/abacus/query/condition/Intersect.java`
- [x] `com/landawn/abacus/query/condition/Is.java`
- [x] `com/landawn/abacus/query/condition/IsInfinite.java`
- [x] `com/landawn/abacus/query/condition/IsNaN.java`
- [x] `com/landawn/abacus/query/condition/IsNot.java`
- [x] `com/landawn/abacus/query/condition/IsNotInfinite.java`
- [x] `com/landawn/abacus/query/condition/IsNotNaN.java`
- [x] `com/landawn/abacus/query/condition/IsNotNull.java`
- [x] `com/landawn/abacus/query/condition/IsNull.java`
- [x] `com/landawn/abacus/query/condition/Join.java`
- [x] `com/landawn/abacus/query/condition/Junction.java`
- [x] `com/landawn/abacus/query/condition/LeftJoin.java`
- [x] `com/landawn/abacus/query/condition/LessThan.java`
- [x] `com/landawn/abacus/query/condition/LessThanOrEqual.java`
- [x] `com/landawn/abacus/query/condition/Like.java`
- [x] `com/landawn/abacus/query/condition/Limit.java`
- [x] `com/landawn/abacus/query/condition/Minus.java`
- [x] `com/landawn/abacus/query/condition/NamedProperty.java`
- [x] `com/landawn/abacus/query/condition/NaturalJoin.java`
- [x] `com/landawn/abacus/query/condition/Not.java`
- [x] `com/landawn/abacus/query/condition/NotBetween.java`
- [x] `com/landawn/abacus/query/condition/NotEqual.java`
- [x] `com/landawn/abacus/query/condition/NotExists.java`
- [x] `com/landawn/abacus/query/condition/NotIn.java`
- [x] `com/landawn/abacus/query/condition/NotInSubQuery.java`
- [x] `com/landawn/abacus/query/condition/NotLike.java`
- [x] `com/landawn/abacus/query/condition/On.java`
- [x] `com/landawn/abacus/query/condition/Operator.java`
- [x] `com/landawn/abacus/query/condition/Or.java`
- [x] `com/landawn/abacus/query/condition/OrderBy.java`
- [x] `com/landawn/abacus/query/condition/package-info.java`
- [x] `com/landawn/abacus/query/condition/RightJoin.java`
- [x] `com/landawn/abacus/query/condition/Some.java`
- [x] `com/landawn/abacus/query/condition/SqlExpression.java`
- [x] `com/landawn/abacus/query/condition/SubQuery.java`
- [x] `com/landawn/abacus/query/condition/Union.java`
- [x] `com/landawn/abacus/query/condition/UnionAll.java`
- [x] `com/landawn/abacus/query/condition/Using.java`
- [x] `com/landawn/abacus/query/condition/Where.java`
- [x] `com/landawn/abacus/query/Dsl.java`
- [x] `com/landawn/abacus/query/DynamicQuery.java`
- [x] `com/landawn/abacus/query/Filters.java`
- [x] `com/landawn/abacus/query/package-info.java`
- [x] `com/landawn/abacus/query/ParsedSql.java`
- [x] `com/landawn/abacus/query/QueryUtil.java`
- [x] `com/landawn/abacus/query/Selection.java`
- [x] `com/landawn/abacus/query/SortDirection.java`
- [x] `com/landawn/abacus/query/SqlBuilder.java`
- [x] `com/landawn/abacus/query/SqlDialect.java`
- [x] `com/landawn/abacus/query/SqlMapper.java`
- [x] `com/landawn/abacus/query/SqlOperation.java`
- [x] `com/landawn/abacus/query/SqlParser.java`
- [x] `com/landawn/abacus/query/SubQuerySnapshot.java`
