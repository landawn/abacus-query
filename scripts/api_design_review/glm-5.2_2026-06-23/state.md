# API Design Review — State

- **MODEL**: glm-5.2
- **DATE**: 2026-06-23
- **EXECUTION**: PARALLEL — one sub-agent per batch; orchestrator verifies every finding against source before it enters a report.
- **Scope**: all PUBLIC API of every public class in `com.landawn.abacus.query` (./src/main/java), incl. `condition` subpackage. Deprecated methods noted but not excluded.

## In-scope class list (80 files)

### Top-level package `com.landawn.abacus.query` (12)
- AbstractQueryBuilder (6229) — abstract base; `This extends AbstractQueryBuilder<This>`
- SqlBuilder (1846) — `extends AbstractQueryBuilder<SqlBuilder>`
- DynamicQuery (2005) — final, standalone query holder
- Filters (3908) — static factory/utility for condition + builder construction
- ParsedSql (596) — final
- QueryUtil (932) — final utility
- Selection (420) — final
- SortDirection (134) — enum
- SqlDialect (183)
- SqlMapper (843) — final
- SqlOperation (247) — enum
- SqlParser (1385) — final

### `condition` subpackage (68)

Backbone / abstract:
- Condition (138) — interface
- AbstractCondition (851) — `implements Condition`
- ComposableCondition (172) — `extends AbstractCondition`
- Cell (265) — abstract `extends AbstractCondition`
- ComposableCell (265) — `extends ComposableCondition`
- Clause (123) — abstract `extends Cell`
- Operator (485) — enum
- NamedProperty (1203) — sealed `permits NP`

Binary / junction family:
- Junction (493) — `extends ComposableCondition`; And (205), Or (217)
- Binary (460) — `extends ComposableCondition`; Equal(104), NotEqual(99), GreaterThan(106), GreaterThanOrEqual(106), LessThan(114), LessThanOrEqual(114), Like(113), NotLike(108), Is(99), IsNot(96)
- Is subfamily: IsNull(117), IsNotNull(104), IsNaN(123), IsNotNaN(122), IsInfinite(124), IsNotInfinite(117)

Between / In / InSubQuery:
- AbstractBetween (318); Between(93), NotBetween(104)
- AbstractIn (298); In(101), NotIn(99)
- AbstractInSubQuery (343); InSubQuery(123), NotInSubQuery(124)

Quantifier / exists / not:
- All(132), Any(134), Some(122) — `extends ComposableCell`
- Exists(121), NotExists(117) — `extends ComposableCell`
- Not(124) — `extends ComposableCell`

Join family:
- Join (541) — `extends AbstractCondition`
- InnerJoin(208), LeftJoin(193), RightJoin(187), FullJoin(180), CrossJoin(206), NaturalJoin(189) — all `extends Join`
- On(298), Using(312) — `extends Cell`

Subquery / criteria / expression:
- SubQuery (688) — `extends AbstractCondition`
- Criteria (2359) — `extends AbstractCondition`
- Expression (1870) — `extends ComposableCondition`

Clause family (extends Clause):
- Where(104), Having(144), GroupBy(251), OrderBy(241), Limit(452)
- Union(181), UnionAll(167), Intersect(182), Except(188), Minus(197)

## Relationship map (inheritance edges)
```
Condition (interface)
 └─ AbstractCondition
     ├─ ComposableCondition
     │    ├─ ComposableCell
     │    │    ├─ All, Any, Some, Exists, NotExists, Not
     │    ├─ Junction ─ And, Or
     │    ├─ Binary ─ Equal, NotEqual, GreaterThan, GreaterThanOrEqual,
     │    │             LessThan, LessThanOrEqual, Like, NotLike, Is, IsNot
     │    │             Is ─ IsNull, IsNaN, IsInfinite
     │    │             IsNot ─ IsNotNull, IsNotNaN, IsNotInfinite
     │    ├─ AbstractBetween ─ Between, NotBetween
     │    ├─ AbstractIn ─ In, NotIn
     │    ├─ AbstractInSubQuery ─ InSubQuery, NotInSubQuery
     │    └─ Expression
     ├─ Cell
     │    ├─ Clause ─ Where, Having, GroupBy, OrderBy, Limit,
     │    │           Union, UnionAll, Intersect, Except, Minus
     │    ├─ On
     │    └─ Using
     ├─ Join ─ InnerJoin, LeftJoin, RightJoin, FullJoin, CrossJoin, NaturalJoin
     ├─ SubQuery
     └─ Criteria

AbstractQueryBuilder<This>
 └─ SqlBuilder
```

## Facade / delegate relationships (to be refined during review)
- `Filters` — primary user-facing facade/factory: builds Condition subtypes, Clause subtypes, Join subtypes, and likely delegates to QueryUtil/SqlBuilder.
- `SqlBuilder` / `AbstractQueryBuilder` — fluent builder; DynamicQuery is a separate holder (not a builder subclass).
- `QueryUtil` — low-level helpers used by builders/parsers.
- `SqlParser` ↔ `ParsedSql` — parser produces ParsedSql.

## Per-class / per-batch status

| Batch | Classes | Status |
|---|---|---|
| B1 | AbstractQueryBuilder | todo |
| B2 | Filters | todo |
| B3 | Criteria | todo |
| B4 | DynamicQuery | todo |
| B5 | Expression | todo |
| B6 | SqlBuilder | todo |
| B7 | SqlParser | todo |
| B8 | NamedProperty | todo |
| B9 | QueryUtil + SqlMapper | todo |
| B10 | backbone: Condition, AbstractCondition, ComposableCondition, Cell, ComposableCell | todo |
| B11 | Join family: Join, InnerJoin, LeftJoin, RightJoin, FullJoin, CrossJoin, NaturalJoin, On, Using, SubQuery | todo |
| B12 | Binary+Junction family: Junction, And, Or, Binary, Equal, NotEqual, GreaterThan, GreaterThanOrEqual, LessThan, LessThanOrEqual, Like, NotLike, Is, IsNot, IsNull, IsNotNull, IsNaN, IsNotNaN, IsInfinite, IsNotInfinite | todo |
| B13 | In/Between/Exists/Quantifier: AbstractBetween, Between, NotBetween, AbstractIn, In, NotIn, AbstractInSubQuery, InSubQuery, NotInSubQuery, Exists, NotExists, All, Any, Some, Not | todo |
| B14 | Clause family: Clause, Where, Having, GroupBy, OrderBy, Limit, Union, UnionAll, Intersect, Except, Minus | todo |
| B15 | Support: Selection, ParsedSql, Operator, SqlOperation, SortDirection, SqlDialect | todo |

## Segment-reading note
Files > ~1500 lines (AbstractQueryBuilder 6229, Filters 3908, Criteria 2359, DynamicQuery 2005, Expression 1870, SqlBuilder 1846, SqlParser 1385, NamedProperty 1203) must be read in line-range segments. Agents instructed accordingly.