# Coverage follow-up: 2026-10-14

The attempt to reach 100% added 58 tests and extended existing assertions. A
fresh `mvn -B clean test jacoco:report` run passed all 6,181 tests, with zero
failures, errors, or skipped tests.

| Counter | Before | After | Covered / total |
| --- | ---: | ---: | ---: |
| Lines | 98.031927% | 99.008674% | 13,583 / 13,719 |
| Instructions | 98.169060% | 99.074686% | 70,453 / 71,111 |

The additions cover 134 previously missed lines and 644 instructions. They
exercise metadata-failure resource cleanup, final-flush I/O failures, logging,
composite default IDs, aliases, custom placeholder failures, SQL scanner
boundaries, Unicode, comments, CTE suffixes, and parameter offsets.
`SqlMapper.java` and `DynamicQuery.java` now have 100% line and instruction
coverage, including their nested classes.

The overall 100% target was not reached. The report still contains 136 missed
lines and 658 missed instructions. These include unexercised scanner and
defensive paths, hash normalization branches, and the following obstacles:

- Deprecated conditionless qualified-join constructors always throw, as their
  contracts specify. Existing tests assert those exceptions, but JaCoCo still
  reports the delegating constructors and factory wrappers as missed because
  the throwing calls do not reach their subsequent probes.
- Some guards repeat validation already performed by their callers. For
  example, `SqlBuilder.appendInClause` contains a multiple-column branch, while
  its caller already routes multiple-column conditions to
  `appendMultiColumnInClause` directly.
- Enum-switch defaults for unsupported SQL policies and static-initialization
  reflection-failure handlers cannot be reached with the supported runtime
  state.

Production code and coverage configuration are unchanged. No coverage
exclusions or disabled tests were added. Further progress requires reviewing
the remaining paths individually; the uncovered report is not a list of 136
distinct missing behavior tests.

Regenerate the local [HTML report](../../target/site/jacoco/index.html) with the
command above. The machine-readable counters are in
`target/site/jacoco/jacoco.xml`.
