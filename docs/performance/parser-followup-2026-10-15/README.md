# SqlParser and ParsedSql follow-up fixes

This follow-up addresses the four findings reported after the [first implementation](../parser-implementation-2026-10-15/README.md). Three specialist agents implemented or reviewed separate parts, with independent cross-review and final validation by the coordinating agent. Production edits remain confined to `ParsedSql` and `SqlParser`.

“Original” means commit `33b655b87de09b33c4cad4a711956f0b93ff266a`; “previous” means the preserved first implementation, before these fixes; “current” means the snapshot identified in [source-hashes.json](source-hashes.json). Original and previous compiled classes were preserved separately. Earlier reports and measurement files retain their historical results.

## Changes

| Finding | Fix | Compatibility and allocation constraints |
| --- | --- | --- |
| Whole-token `:name` validation ran before quote ambiguity handling | Require the token to contain no backslash before taking the whole-name shortcut. | Ambiguous tokens reach the original scanner before any throwing name validation. Malformed names in unambiguous tokens still throw, and later mixed-style errors retain their precedence. |
| Different bracket starts repeatedly scanned overlapping suffixes | Cache covered intervals, physical stop positions, and the last relevant word positions. Extend a cached interval by scanning only its uncovered prefix. | Extended answers may use words through EOF, but coverage ends at the physical opener. Later bounded regions remain independent. Flat and nesting comment readings and both insertion policies remain active. |
| Dense incremental memo walks repeatedly copied large tables | When a table's planned geometric capacity reaches at least half the SQL length, promote directly to final length. | Short-prefix lookups keep small tables. Tokenization/classification still use lazy full-size tables on first use. A complete search or matcher walk can still allocate earlier small arrays; single allocation is not promised for those APIs. |
| Cleanup and retained cursor state | Remove redundant operation-prefix guards, release `TokenCursor.memo` when `next()` first returns `false`, and use normal collection imports. | The cursor retains its immutable source/configuration and existing exhausted-state behavior. Memo release reduces retention after exhaustion, not bytes allocated during the walk. |

The bracket summary uses four scalar word positions and one reusable object for ordinary sequential bracketed names. Only simultaneously live, disjoint intervals require a `TreeMap`, keyed by immutable physical stop. Prefix extension updates a summary without removing/reinserting tree entries. Expired intervals are retired as the raw bracket cursor advances. One scalar remembers the latest qualifying start with a negative answer, avoiding repeated checks when many brackets reach exactly the same word. Repeated nonkeyword starts retain their separate protection against rescanning long identifiers.

The region character scan is shared across starts; indexed interleaved regions add `O(b log r)` map operations for `b` bracket starts and at most `r` live regions. This is not a blanket linear-time guarantee for every part of SQL classification or first-word/comment skipping. The reported distinct-start overlap family needs one summary and no interval map.

The growth threshold concerns the **next planned capacity**, not merely the requested position. For example, a 1,024-character source grows through capacities 64, 128, 256, then 1,024; a 1,025-character source may also use 512 before promotion. Compared with unrestricted doubling, this removes one large intermediate allocation. Cumulative element slots per growing table remain below twice final length rather than three times final length, excluding array headers. The tradeoff is earlier final-size allocation on a lookup that stops after covering slightly more than one quarter of a suitably sized source.

## Correctness

- All 82 production sources and 88 test sources compile with `--release 21` and the locally installed dependencies.
- The complete configured `AbacusQueryTestSuite` passes **6,261 invocations** with zero failures, errors, skipped or aborted tests. Nine regression methods were added to the previous 6,252 passing invocations.
- **130,094 deterministic records match the original baseline byte-for-byte**: 42,000 tuning records, 24,000 classifier records, 35,530 edge records, and 28,564 new follow-up records. The edge corpus also checks cursor text and bounds against stateless walking.
- The expanded corpus exposes **240 mismatches in the previous implementation and zero in the current version**. It preserves raw UTF-16 code units, including unpaired surrogates, and compares exact exception classes/messages as well as parsed SQL, names, counts, offsets, classifier results, and contextual searches.
- Regression tests cover the exact overlap family at 32,768 starts, interleaved/disjoint regions, physical stop versus EOF extension, keyword and dollar boundaries, both insertion policies, positive mutation vetoes, ambiguous and unambiguous malformed names, cached answers across memo growth, odd/even promotion thresholds, and cursor exhaustion.
- Javadoc `-Xdoclint:all` produces zero diagnostics; the generated API reference has been refreshed. The javac-AST test-layout check validates 6,013 methods in 88 files with no issues.
- The no-hash remasking counterexample still passes: masking a named parameter can expose a dollar-quote boundary, so the second consistency mask remains necessary.

See [validation.json](validation.json), [test-summary.txt](test-summary.txt), [test-layout.txt](test-layout.txt), and [remask-current.txt](remask-current.txt). Raw build, suite, and differential outputs are under `.audit/parser-followup-2026-10-15/`. The user's staged index is unchanged.

The configured suite excludes the existing `slow-test` tag. Verification uses direct `javac` and JUnit ConsoleLauncher because the earlier offline Maven attempt could not resolve its publishing plugin; this is not a successful Maven lifecycle claim. Existing unrelated compiler warnings remain. The AST layout check substitutes for the repository Python checker because the installed launcher has no Python runtime.

## Measurements

Measurement results are recorded in [summary.csv](summary.csv) and [summary.json](summary.json); all fork/round samples are retained alongside them. Values below are medians of three JVM medians. Times are diagnostic, while the allocation counts are more stable across forks.

For the exact reported family, `"SELECT '" + "[/*".repeat(n) + "*/ UPDATE x ".repeat(n) + "]'"`:

| Starts | SQL characters | Previous time | Current time | Speedup |
| ---: | ---: | ---: | ---: | ---: |
| 1,000 | 15,010 | 31.708 ms | 0.476 ms | 66.5x |
| 2,000 | 30,010 | 118.469 ms | 0.696 ms | 170.3x |
| 4,000 | 60,010 | 457.103 ms | 1.391 ms | 328.7x |
| 8,000 | 120,010 | 1,912.332 ms | 2.746 ms | 696.5x |

Doubling from 2,000 to 4,000 to 8,000 starts now approximately doubles time instead of quadrupling it. At 8,000 starts, allocation also falls from 3,322,680 to 2,359,264 bytes per call, a 29.0% reduction. This remains a nontrivial allocation cost from the rest of classification; it is not a constant-memory classifier claim.

The original shared-first-word control at 8,192 brackets improves from 1.105 to 0.971 ms, with a 32-byte fixed allocation increase (789,504 to 789,536). Classifying 1,024 ordinary `[update date]` names uses 68,128 to 43,608 bytes, **36.0% less**, because the reusable summary replaces per-region arrays; its time changes from 0.379 to 0.358 ms.

Allocation per operation for contextual lookups:

| Case | Original bytes | Previous bytes | Current bytes | Change vs previous |
| --- | ---: | ---: | ---: | ---: |
| `indexOfToken`, missing token among 1,024 hash names | 84,696 | 125,568 | 105,056 | -16.3% |
| Matcher, all 1,024 hash names | 84,720 | 125,592 | 105,080 | -16.3% |
| `indexOfToken`, missing token among 1,024 hyphenated bracket/alias pairs | 29,640 | 62,496 | 46,096 | -26.2% |
| `indexOfToken`, one early hash and a 1 MiB tail | 5,243,096 | 448 | 448 | unchanged |
| Matcher, one early hash and a 1 MiB tail | 5,243,120 | 472 | 472 | unchanged |
| `indexOfToken`, finds `#t512` among 1,024 hash names | 60,248 | 65,616 | 80,608 | +22.8% |

The memory regression is **reduced, not completely eliminated**: complete dense hash walks still allocate about 24% more than the original full-size policy, and the bracket/alias case about 56% more. Earlier promotion also makes the middle-exit case allocate more than the previous implementation. Keeping incremental allocation avoids giving up the much larger early-search savings; applying full-size allocation unconditionally to the matcher would lose those savings. The README's single-allocation claim applies specifically to tokenization/classification, not every search that happens to reach EOF.

Full tokenization of the 1,024-hash input remains at 167,712 bytes per operation. The named-parameter guard leaves allocation unchanged in all measured distributions: 432 bytes for one name, 5,016 for 32 repeated names, 137,496 for 1,024 repeated names, and 127,152 for 1,024 unique names. Their final measured times range from 9.4% faster to 2.4% slower than the previous snapshot; the guard restores correctness without introducing new objects.

Lower allocation does not establish uniformly lower CPU time. The dense 1,024-name matcher measures 162.531 to 181.059 microseconds (11.4% slower), with broadly overlapping fork ranges of 156.917–283.614 and 149.481–277.322 microseconds. The 128-name matcher median is 21.2% slower and similarly variable; the 20,000-character sparse search is 12.3% slower with unchanged allocation. These results remain in the raw samples and summaries. The overlap-family scaling change is much larger than this variance; small control timing differences should not be treated as production throughput guarantees.

## Method and reproduction

[FollowupProbe.java](FollowupProbe.java) measures 31 cases for previous/current implementations, plus 18 original-baseline allocation references. Each case has at least 300 ms warmup, adaptive batches targeting 5 ms (maximum 32,768 operations), and seven measured rounds. Each version has three fresh JVM forks; previous/current order alternates. All JVMs run sequentially after builds and correctness checks finish, using `-Xms256m -Xmx1024m -Xbatch` on Oracle JDK 25. The same compiled harness runs against each version. SQL construction occurs before timing; parsing uses the null-cache overload. Dependency logging is disabled before measurement. Allocation is thread-allocated bytes from `ThreadMXBean`, and results are retained through volatile sinks.

The report uses the median of seven rounds within each fork, then the median of the three fork medians. The summary retains every fork median and checks that all versions return the same result. These are diagnostic microbenchmarks, not JMH or whole-application measurements. Allocation is not retained heap, and timing does not establish production latency, cold-start behavior or concurrent throughput.

The [initial region-cache run](initial-region-cache/) found that removing the old same-start shortcut slowed the shared-first-word control. That evidence prompted the separate scalar negative-answer cache. The parent directory's measurements come from a fresh run after the adjustment; exploratory samples are excluded from its summaries.

Keep separately compiled original, previous, and current classes; never overwrite the historical outputs. Dependency versions and baseline compilation procedures are documented in the original review. Compile `FollowupProbe.java` and `FollowupDifferential.java` against the original API into `.audit/parser-followup-2026-10-15/probes`. Then, from the repository root:

```powershell
./docs/performance/parser-followup-2026-10-15/run-probes.ps1
node docs/performance/parser-followup-2026-10-15/summarize.mjs
node docs/performance/parser-followup-2026-10-15/collect-evidence.mjs
```

The runner accepts alternative class/probe/dependency paths. The collector requires the completed test and differential outputs, checks source hashes, and rejects later changes rather than silently attributing measurements to different code. It also verifies the staged-index snapshot stayed unchanged. The [expanded corpus](FollowupDifferential.java) can be rerun with its output TSV path as its sole argument against each separately compiled version.
