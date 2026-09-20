# ParsedSql: current versus commit 965f6afe

Measured on September 20, 2026. The current implementation is substantially slower for ordinary uncached positional SQL: approximately **73% slower for two bindings, 40% for sixteen, and 33% for 128**. The same-object positional cache-hit case remains approximately unchanged at **14 ns**, with no measured allocation. Large MyBatis arrays improve substantially: an 8,192-binding array falls from **156.742 ms to 1.905 ms** per parse, about **82 times faster**.

These results compare the exact requested commit with current production sources, not the reconstructed baseline in the earlier performance records. No production or test code was changed, and no performance tuning was attempted.

## Versions and environment

| Variant | Production source | abacus-common runtime | Fresh JVMs |
| --- | --- | --- | ---: |
| baseline | `965f6afef86d928fbff63589fb3d59edc322574e` | 7.9.3, as declared by that commit | 5 |
| current | `417ab8a8a4c038b041b63592c99ee215ac7da686` | 8.0.0, as declared by current pom.xml | 5 |
| baseline_common8 | The same compiled baseline jar | 8.0.0, dependency control | 3 |

Current `src/main/java` matched its commit when copied and was rechecked after benchmarking. Existing uncommitted changes were generated AI documentation. Both complete snapshots contain 81 production Java source files and include their own `SqlParser`; this measures the complete `ParsedSql.parse` call path, not an isolated replacement of only `ParsedSql.java`.

- Windows NT 10.0.26100 x64, AMD Ryzen 7 PRO 7840U, 16 logical processors, approximately 64 GB visible memory.
- Oracle JDK 25, runtime `25+37-LTS-3491`; both snapshots compiled with `--release 17`, UTF-8, and Lombok 1.18.46 annotation processing.
- Identical JVM options: `-Xms512m -Xmx1024m -XX:+UseG1GC`.
- Each JVM ran sequentially. Primary variants alternate, with the three dependency-control runs interleaved. Exact order, timestamps, source fingerprints and dependency hashes are recorded in [manifest.json](manifest.json) and [run-status.json](run-status.json).

## Ordinary uncached queries

Times are microseconds per public `ParsedSql.parse` call, including small result-access checks. Positive percentages mean slower. Allocation is bytes per operation on the calling thread. These rows passed the same expected count, named-parameter order, and rendered-SQL checks in both versions.

| Query | Baseline us | Current us | Time change | Allocated bytes, baseline / current |
| --- | ---: | ---: | ---: | ---: |
| No parameters | 1.290 | 1.421 | +10.2% | 1,248 / 1,400 |
| Two positional parameters | 1.704 | 2.954 | +73.4% | 1,456 / 1,872 |
| Sixteen positional parameters | 10.573 | 14.768 | +39.7% | 5,224 / 6,128 |
| 128 positional parameters | 79.839 | 105.815 | +32.5% | 35,904 / 42,632 |
| Two named parameters | 2.016 | 2.345 | +16.4% | 1,816 / 2,384 |
| Sixteen named parameters | 11.887 | 12.013 | +1.1% | 7,960 / 11,552 |
| Two MyBatis parameters with metadata | 2.606 | 3.177 | +21.9% | 2,552 / 4,008 |
| Sixteen MyBatis parameters with metadata | 16.157 | 18.654 | +15.5% | 14,656 / 25,368 |
| JOIN/GROUP BY/HAVING, four positional parameters | 5.378 | 8.975 | +66.9% | 3,824 / 4,312 |
| JOIN/GROUP BY/HAVING, four named parameters | 5.450 | 5.970 | +9.6% | 3,968 / 5,008 |
| JOIN/GROUP BY/HAVING, four MyBatis parameters | 6.953 | 7.799 | +12.2% | 5,800 / 8,616 |
| Quotes and comments, two positional parameters | 1.818 | 2.966 | +63.1% | 1,192 / 1,608 |
| JSON existence operator and one positional parameter | 1.666 | 2.464 | +47.9% | 1,128 / 1,536 |
| JDBC-escaped operator and one positional parameter | 1.927 | 2.627 | +36.3% | 1,176 / 1,584 |
| Unary geometric cast, one positional parameter | 1.644 | 2.261 | +37.5% | 792 / 1,400 |

The positional slowdown is larger than the observed fork variation: two-binding baseline fork medians span 1.655–2.113 us versus 2.872–3.388 us for current; sixteen-binding medians span 10.323–11.438 us versus 14.506–20.503 us. Smaller differences, particularly sixteen named parameters, should not be read as conclusive changes because the fork ranges overlap.

The runtime dependency control does not explain away the positional regression. Against baseline code running on the same abacus-common 8.0.0, current is **75.9%, 47.0%, and 37.9% slower** for two, sixteen, and 128 positional parameters respectively. This does not isolate individual methods or establish a CPU profile; changes in `ParsedSql`, `SqlParser`, and their interactions remain combined.

## Cache hits

These inputs are already cached. The same-object case reuses the exact String; the equal-object case cycles through 1,024 different equal Strings whose hashes were computed before timing. String creation and first-time hashing are excluded.

| Query/key | Baseline ns | Current ns | Baseline with common 8 ns | Allocation |
| --- | ---: | ---: | ---: | ---: |
| Positional, same String | 13.831 | 13.585 | 13.473 | 0 B |
| Positional, equal String | 22.373 | 17.266 | 17.873 | 0 B |
| Named, same String | 18.067 | 12.670 | 13.169 | 0 B |
| Named, equal String | 23.103 | 17.538 | 17.612 | 0 B |

Baseline cache measurements vary substantially across forks (for example 13.106–22.170 ns for named/same String). The same-dependency results are close. These measurements show no material cache-hit regression; they do not support a strong parser-specific cache speedup claim. They measure uncontended, single-thread hits, not concurrent cache throughput.

## Large inputs

The following rows produce the same checked results in both versions. Times are milliseconds per uncached parse.

| Shape at size 8,192 | Baseline ms | Current ms | Time change | Allocated bytes, baseline / current |
| --- | ---: | ---: | ---: | ---: |
| Named parameters inside one ARRAY token | 0.558 | 0.704 | +26.2% | 863,844 / 1,178,932 |
| MyBatis parameters with metadata inside one ARRAY token | 156.742 | 1.905 | -98.8% | 1,814,143,252 / 2,745,516 |
| Deeply parenthesized geometric operand | 1.781 | 2.551 | +43.3% | 1,061,092 / 1,209,356 |

The MyBatis improvement removes approximately 1.81 GB of allocation per large parse in this workload, reducing allocation by about 661 times. Sizes 256, 1,024, 4,096 and 8,192 are all retained in [summary.csv](summary.csv) to show scaling rather than relying on one endpoint.

Other workloads perform different work because the baseline misses bindings. They are retained as correctness-sensitive measurements, not equivalent-work performance ratios:

| Shape | Baseline count | Current count | Baseline time | Current time |
| --- | ---: | ---: | ---: | ---: |
| SQL/JSON constructor with `? NULL ON NULL` | 0 | 1 | 1.574 us | 2.488 us |
| ARRAY with 8,192 positional markers | 0 | 8,192 | 0.084 ms | 1.805 ms |
| Compact concatenation with 8,192 markers | 1 | 8,192 | 0.651 ms | 0.945 ms |

CSV rows flag these differences with `equivalent_results=false`. The raw numerical ratios remain in the CSV for transparency and must not be interpreted as the cost of processing equivalent bindings.

## Method and limits

- Each JVM checks 36 uncached cases before timing: exact parameter count, named-parameter order and converted SQL. All current checks passed. Baseline differences are logged rather than silently treated as equivalent results.
- Each ordinary case warms for 8,192 calls; each stress case warms for 16 calls. Nine measured rounds follow, with a deterministically shuffled case order matched by fork number.
- Each ordinary measured round contains 2,048 parses in batches of 256. Stress rounds contain `max(2, 8192 / size)` parses, in bounded batches. The cache is cleared before each batch, and SQL strings have unique fixed-width identifiers. Batches stay below the cache capacity, so these are misses without forced capacity eviction.
- SQL construction, expected-result construction, cache clearing, class loading, startup and compilation are outside measured time. Timed work is `parse`, reading the count/rendered length/name-list size, min/max bookkeeping and result consumption. Objects cannot be discarded as dead benchmark work because results contribute to a volatile sink.
- Allocated bytes come from `ThreadMXBean.getThreadAllocatedBytes` around the timed loop. They exclude SQL-generation allocation, setup and background-thread allocations; they are not retained-heap measurements. Garbage collection during timed work can still affect elapsed time.
- Each cache case warms for 1,000,000 calls and measures nine rounds of 1,000,000 calls. Setup verifies that every key resolves to the seeded cached instance.
- Reported values are medians of each JVM's nine-round median: five JVMs per primary version and three for the dependency control. [fork-summary.csv](fork-summary.csv) exposes each fork, and the thirteen raw CSVs retain all **4,680 measured samples**.
- All thirteen JVMs exited successfully. Every measured current row had the expected count and zero errors; baseline runs also had zero thrown parse errors. This is benchmark-input validation, not a rerun of the complete unit-test suite.
- This is a local comparative microbenchmark, not JMH or a database benchmark. There was no CPU affinity, frequency pinning, statistical confidence-interval calculation, cold-start measurement, mixed cache-hit-rate workload, or concurrent-load test. Small differences and cache-hit improvements deserve caution. Allocation differences and the large positional/MyBatis effects are clearer than small timing changes.

## Reproduction

The runnable [ParserBenchmark.java](ParserBenchmark.java), [CompileSources.java](CompileSources.java), [run-benchmarks.mjs](run-benchmarks.mjs), and [summarize.mjs](summarize.mjs) are retained with the results. The actual prepared snapshots, compiled jars, dependencies and compiler logs are under ignored `.audit/parsed-sql-vs-965f6afe/`; those files are not required to interpret the checked-in results but must be recreated on a fresh checkout.

1. Export `src/main/java` and `pom.xml` at each exact commit into separate directories using `git archive`; do not switch or overwrite the working tree. For a future uncommitted candidate, copy its source tree and record its fingerprint instead.
2. Resolve each snapshot's Maven dependency classpath (including Lombok) into `baseline-classpath.txt` and `current-classpath.txt`. Keep all non-abacus-common dependencies identical where applicable. This run used locally available jars and no downloads.
3. Compile each source directory with `java CompileSources.java <source-root> <classpath-file> <classes-dir> lombok`, then package it with `jar --create --file <baseline.jar-or-current.jar> -C <classes-dir> .`.
4. Make a harness classpath text file containing `current.jar` followed by the current dependencies using the platform classpath separator. Compile the harness with `java CompileSources.java ParserBenchmark.java <harness-classpath-file> <work-dir>/harness`.
5. Place both jars and dependency classpath files in the prepared work directory. Run `node run-benchmarks.mjs <work-dir> <java-executable> <new-output-dir>`. The control uses the unchanged baseline jar with the current dependency classpath. Use a new output directory to preserve this measurement.
6. Run `node summarize.mjs <new-output-dir>`. It rejects missing rounds, thrown parse errors, or unexpected current results before writing its summaries.

For tuning, the largest ordinary-query opportunity is uncached positional parsing, followed by allocation growth in named and MyBatis parsing. Preserve the correctness fixes and the large MyBatis-array improvement when comparing candidates.
