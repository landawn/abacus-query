# Builder performance tuning — 2026-09-20

Comparison: exact commit 965f6afef86d928fbff63589fb3d59edc322574e versus the tuned working tree, starting from commit cbc5e44a848c2932dc345a136c81582860f661ba.

Across **five fresh JVMs per primary version**, the tuned implementation is faster in **188/188 measured cases** and allocates less in **188/188**. Median execution time is **7.9–82.6% lower**; allocated bytes per operation are **20.1–97.1% lower**. All 182 equivalent-output cases improve; six historical raw-string escaping differences are flagged below.

The completed dataset contains **25,380 samples from 15 JVMs**: five exact-baseline forks, five tuned forks and five starting-snapshot forks. [summary.csv](final/summary.csv), [per-fork.csv](final/per-fork.csv) and [summary.json](final/summary.json) retain every case, fork range and allocation result. These are results for the measured corpus, not proof for every possible query or JVM.

| Case | 965f6afe ns/op | Tuned ns/op | Less time | Allocated B/op, old → tuned |
|---|---:|---:|---:|---:|
| positional_select_star | 288.2 | 223.1 | 22.6% | 1,424 → 880 |
| named_select_entity | 591.5 | 544.8 | 7.9% | 2,000 → 1,304 |
| positional_where_16 | 4,463.3 | 3,083.9 | 30.9% | 5,888 → 3,648 |
| named_where_128 | 48,434.2 | 32,865.6 | 32.1% | 71,280 → 47,920 |
| positional_in_1024 | 14,876.8 | 6,167.9 | 58.5% | 28,504 → 20,800 |
| raw_in_1024 | 77,429.0 | 13,464.1 | 82.6% | 220,248 → 6,328 |
| raw_batch_64 | 28,635.4 | 11,983.1 | 58.2% | 97,344 → 30,184 |
| named_update_chain_16 | 17,135.5 | 9,642.6 | 43.7% | 47,000 → 24,072 |
| mybatis_union_builder | 10,981.3 | 4,190.3 | 61.8% | 16,648 → 6,816 |
| positional_retry_rollback | 3,191.7 | 2,542.8 | 20.3% | 5,872 → 3,864 |

The same-dependency starting-snapshot comparison has **187/188 lower latency medians and 188/188 lower allocation medians**. The exception is named_union_builder: **4,296.0 → 4,338.3 ns/op (+1.0%)**, with **8,912 → 6,840 B/op (23.2% less allocation)**. Its fork ranges overlap: starting 4,114.0–4,650.8 ns/op, tuned 3,970.3–4,633.7 ns/op. This is not evidence that this pass improved every path relative to the already-tuned starting tree. It remains substantially faster than the requested 965f6afe baseline.

## Implementation and correctness

The production change is limited to AbstractQueryBuilder.java, SqlBuilder.java, and a small owned-snapshot handoff in Dsl.java. The factory already validates, filters and copies map/bean inputs; it now passes its private map to the builder without a second copy. The protected helper still copies caller-owned maps. ParsedSql, SqlParser and dependencies were not changed by this tuning pass.

- Rollback snapshots use flat arrays instead of duplicate lists, hash tables and hash nodes. Empty snapshots share an empty array. SQL text, overwritten map values, null entries, alias mappings, nested mutations and the full rollback state remain preserved; this is not a truncation-only rollback.
- Name handling avoids a regex Matcher and temporary strings for common identifiers. The lower-camel identity check is conservative; acronyms, digit/capital boundaries, Unicode and separators retain the shared conversion path. No new identifier cache was added.
- Clause validation shares one ordering array and constructs diagnostic strings only on failure. Numeric raw literals append final integral wrapper values directly; arbitrary Number implementations still undergo validation.
- INSERT rendering uses one policy-dispatch loop, mapped-column rendering uses one helper, and bean-batch filtering uses short-circuit loops instead of nested streams. Existing policy-specific subclass hooks remain active.
- Large positional IN lists copy runs of ordinary bindings together and render expressions/subqueries in their original positions. Placeholder-only and literal-only lists preserve the generated-placeholder flag. Subclasses and other policies keep their existing setter calls. BETWEEN avoids names that the built-in raw and positional setters do not consume.

All 81 production and 86 test sources compiled with JDK 25 targeting Java 17. The final configured JUnit suite passed **5,300 tests**, with no failures, errors, aborted tests or skips. Twelve new test methods were added to the existing AbstractQueryBuilderTest and SqlBuilderTest. They cover rollback of overwritten map entries and SQL prefixes, alias maps and nested mutations, Unicode/underscore/name conversion boundaries, lazy validation messages, integer extremes and invalid numeric literals, large IN boundaries and mixed operands, binding order, subclass setter hooks, and defensive snapshot ownership. Existing batch-ID, clause-order and mapped-alias tests also remain green.

The **108,800-probe** differential corpus matches the starting snapshot byte for byte: 28,800 end-to-end query cases, 20,000 randomized parameter-name cases and 60,000 naming-policy cases. Query probes check SQL, parameters, rejection messages, generated-placeholder flags and retry behavior across all SQL and naming policies. The builder resource counter returns to zero. Both digests are b611e5465c4f02ebc6adca9c3bdddc841de2921f5345ec95078bd5d2d5cc5cf1.

Full-source Javadoc doclint passes with one existing SqlDialect constructor warning. The two missing mutateAtomically tags were supplied, and docs/ai was regenerated. Changed Java sources retain CRLF, UTF-8 without BOM. The direct JUnit runner propagates failures because Maven is configured to ignore them; temporary-directory cleanup is disabled for the Windows sandbox while directory assertions still run.

The last new bean ownership test initially omitted its ordinary otherId property from the expected result. The fixture now sets and mutates that property explicitly and checks both its captured value and SQL. No production change was needed for that test correction.

## Protocol and limitations

Windows 10.0.26100, AMD Ryzen 7 PRO 7840U, 16 logical processors, JDK 25+37; every final JVM uses -Xms128m -Xmx256m -XX:+UseG1GC. Exact baseline 965f6afef86d928fbff63589fb3d59edc322574e uses abacus-common 7.9.3; the starting commit cbc5e44a848c2932dc345a136c81582860f661ba and tuned code use 8.0.0. Five starting-snapshot forks use the same 8.0.0 dependency as the candidate, isolating this builder pass from prior parser work and the dependency upgrade. The exact old bytecode cannot serve as an 8.0.0 control: its builder references the removed com.landawn.abacus.util.ObjectPool class. That failed startup is preserved under dependency-control-unavailable/; it produced no samples, and no compatibility shim was added.

One benchmark JVM runs at a time. Baseline/current launch order alternates across five pairs. Each case has at least 250 ms of warmup, followed by nine measured rounds whose order is shuffled with a fixed per-fork seed. After warmup, calibration takes the median of five batches and doubles the batch until that median reaches 2 ms (or the calibration cap). The resulting measurement batch targets 20 ms, capped at 262,144 operations. Across the final dataset, per-case median batch durations range from 1.5 to 33.3 ms; the per-fork CSV preserves them. A single paused sample cannot collapse the operation count. Each operation creates a builder and consumes its built SQL/parameter result; immutable fixtures are prepared outside timing. The result escapes through a volatile sink. Reference validation and CSV formatting are outside the measured window. This tuning session runs no compilation or test suite alongside final benchmarks. The shared host has unrelated Java workloads; the retained fork ranges expose timing variation.

Timing summaries take each fork's median of nine samples, then the median across forks. Allocation uses ThreadMXBean bytes allocated by the benchmark thread per operation. This is not peak heap, retained heap, startup cost or concurrent throughput. Existing library metadata caches are warmed. New static arrays have a small one-time footprint; no new global cache was introduced. This corpus cannot prove superiority for every input, extension subclass, JVM or machine.

Of the 188 benchmark cases, **182 have identical results at the old commit**. Six raw INSERT/batch/UPDATE cases preserve earlier correctness fixes for escaping an apostrophe: the old output uses a backslash escape while current output doubles the apostrophe. These differences are recorded in historical-output-differences.json, and all candidate outputs must match the starting snapshot before measurement. Empty junctions and bound raw SubQuery constructors are not part of the common-version timing matrix because their supported behavior/API differs at the old commit; current behavior remains tested.

The exact-old comparison includes improvements made before this pass, including the preceding parser tuning. The five-fork starting-snapshot comparison distinguishes this builder pass from those prior changes. Starting forks run after the primary pairs, so their launch order is not interleaved with the candidate; shared-host timing drift remains a limitation.

An exploratory run with a 1 GiB maximum heap failed during warmup due to exhausted system commit space; the JVM log reported approximately 26 MB available despite free physical memory. That incomplete run and its paired baseline are retained under aborted-native-memory/ and excluded from final summaries. All final versions use the same smaller heap. A first repeated pass was also stopped when calibration produced only 10 operations for one case; its completed and interrupted measurements are retained under calibration-pass-aborted/ and excluded from final summaries. The corrected calibration is used for every final version. Earlier 132-case experiments and intermediate 188-case pilots are retained under exploratory/; they are not mixed into final measurements. The expanded harness also fixes a Map.of iteration-order ambiguity in the retry fixture by inserting its two entries explicitly.

## Reproduction

Build each complete source snapshot with --release 17 and its corresponding dependency version. Compile BuilderBenchmark.java against the starting snapshot, then use the same benchmark class with each runtime classpath. Supply classpaths for your builds:

    javac --release 17 -cp $beforeCp -d bench-classes BuilderBenchmark.java
    java -cp "bench-classes;$beforeCp" BuilderBenchmark before 1 reference.txt probe
    java -Xms128m -Xmx256m -XX:+UseG1GC -cp "bench-classes;$baselineCp" BuilderBenchmark baseline 1 reference.txt steady
    java -Xms128m -Xmx256m -XX:+UseG1GC -cp "bench-classes;$currentCp" BuilderBenchmark current 1 reference.txt steady

Repeat forks 1 through 5, alternating launch order. Save stdout to final/<version>-<fork>.csv and stderr separately. Run five before forks for the starting snapshot with the current dependency. The exact old artifact cannot be loaded with 8.0.0 because ObjectPool was removed. Run node summarize.cjs to regenerate all summaries. BuilderDifferential.java uses package com.landawn.abacus.query and runs separately against before/current. manifest.json records the exact source, test, harness, dependency and jar hashes; implementation.patch records this pass.

## Every measured case

| Case | 965f6afe ns/op | Tuned ns/op | Less time | Old B/op | Tuned B/op | Less allocation | Same old output |
|---|---:|---:|---:|---:|---:|---:|:---:|
| custom_handler_union | 10,760.5 | 4,822.0 | 55.2% | 16,536 | 7,296 | 55.9% | yes |
| custom_handler_where | 6,444.7 | 4,275.4 | 33.7% | 10,616 | 6,968 | 34.4% | yes |
| mybatis_batch_4 | 2,949.8 | 1,924.9 | 34.7% | 8,784 | 4,536 | 48.4% | yes |
| mybatis_batch_64 | 34,955.1 | 21,247.5 | 39.2% | 118,896 | 52,248 | 56.1% | yes |
| mybatis_batch_beans | 7,896.2 | 5,756.5 | 27.1% | 22,080 | 14,120 | 36.1% | yes |
| mybatis_between | 1,398.0 | 1,085.4 | 22.4% | 3,544 | 2,344 | 33.9% | yes |
| mybatis_condition_only | 5,407.2 | 3,543.0 | 34.5% | 7,960 | 4,784 | 39.9% | yes |
| mybatis_delete | 468.7 | 348.7 | 25.6% | 1,808 | 1,232 | 31.9% | yes |
| mybatis_derived | 11,095.3 | 4,224.0 | 61.9% | 17,144 | 7,208 | 58.0% | yes |
| mybatis_group_order | 3,799.5 | 1,812.6 | 52.3% | 7,264 | 3,608 | 50.3% | yes |
| mybatis_in_1024 | 105,055.4 | 81,474.2 | 22.4% | 256,896 | 182,240 | 29.1% | yes |
| mybatis_in_16 | 2,130.7 | 1,533.8 | 28.0% | 6,128 | 4,048 | 33.9% | yes |
| mybatis_in_17 | 2,187.7 | 1,659.5 | 24.1% | 6,344 | 4,192 | 33.9% | yes |
| mybatis_in_256 | 25,801.7 | 18,320.0 | 29.0% | 66,616 | 47,256 | 29.1% | yes |
| mybatis_in_expression | 24,656.7 | 19,146.9 | 22.3% | 66,656 | 48,432 | 27.3% | yes |
| mybatis_insert_entity | 882.4 | 736.3 | 16.6% | 2,456 | 1,568 | 36.2% | yes |
| mybatis_insert_placeholders | 977.9 | 696.9 | 28.7% | 2,584 | 1,616 | 37.5% | yes |
| mybatis_insert_values | 1,138.2 | 791.2 | 30.5% | 2,744 | 1,840 | 32.9% | yes |
| mybatis_join_on | 1,825.1 | 1,227.6 | 32.7% | 4,128 | 2,648 | 35.9% | yes |
| mybatis_join_using | 971.8 | 781.3 | 19.6% | 3,352 | 1,976 | 41.1% | yes |
| mybatis_late_modifier | 826.8 | 606.7 | 26.6% | 2,656 | 1,656 | 37.7% | yes |
| mybatis_mixed_literals | 1,576.3 | 1,106.3 | 29.8% | 3,432 | 2,080 | 39.4% | yes |
| mybatis_naming_CAMEL_CASE | 5,013.1 | 3,142.2 | 37.3% | 8,024 | 3,568 | 55.5% | yes |
| mybatis_naming_KEBAB_CASE | 5,910.7 | 4,224.6 | 28.5% | 8,984 | 5,336 | 40.6% | yes |
| mybatis_naming_NO_CHANGE | 4,867.5 | 2,989.3 | 38.6% | 8,024 | 3,568 | 55.5% | yes |
| mybatis_naming_SCREAMING_SNAKE_CASE | 6,013.2 | 4,205.9 | 30.1% | 9,000 | 5,352 | 40.5% | yes |
| mybatis_naming_UPPER_CAMEL_CASE | 6,268.3 | 4,432.1 | 29.3% | 8,984 | 5,064 | 43.6% | yes |
| mybatis_nested | 1,424.8 | 1,051.3 | 26.2% | 3,416 | 2,024 | 40.7% | yes |
| mybatis_pagination | 1,197.2 | 908.0 | 24.2% | 3,808 | 2,152 | 43.5% | yes |
| mybatis_raw_expression | 1,576.8 | 966.9 | 38.7% | 2,624 | 1,744 | 33.5% | yes |
| mybatis_repeated_names | 27,068.1 | 18,154.6 | 32.9% | 52,872 | 30,536 | 42.2% | yes |
| mybatis_retry_rollback | 4,720.5 | 3,374.9 | 28.5% | 7,568 | 4,784 | 36.8% | yes |
| mybatis_row_in | 1,408.3 | 1,027.2 | 27.1% | 3,608 | 2,272 | 37.0% | yes |
| mybatis_select_columns | 834.5 | 626.8 | 24.9% | 1,952 | 1,216 | 37.7% | yes |
| mybatis_select_entity | 605.5 | 551.4 | 8.9% | 2,000 | 1,304 | 34.8% | yes |
| mybatis_select_expressions | 2,129.2 | 1,430.4 | 32.8% | 2,728 | 1,936 | 29.0% | yes |
| mybatis_select_star | 302.4 | 219.1 | 27.5% | 1,424 | 880 | 38.2% | yes |
| mybatis_subquery_raw | 627.5 | 496.4 | 20.9% | 2,272 | 1,344 | 40.8% | yes |
| mybatis_subquery_structured | 1,824.1 | 1,394.3 | 23.6% | 5,232 | 3,312 | 36.7% | yes |
| mybatis_union_builder | 10,981.3 | 4,190.3 | 61.8% | 16,648 | 6,816 | 59.1% | yes |
| mybatis_union_raw | 3,276.1 | 1,081.4 | 67.0% | 5,200 | 1,472 | 71.7% | yes |
| mybatis_update_chain_16 | 14,041.1 | 8,480.2 | 39.6% | 39,936 | 21,304 | 46.7% | yes |
| mybatis_update_map | 1,726.0 | 1,141.8 | 33.8% | 4,200 | 2,512 | 40.2% | yes |
| mybatis_where_128 | 45,273.0 | 30,167.4 | 33.4% | 58,208 | 34,848 | 40.1% | yes |
| mybatis_where_16 | 6,140.9 | 4,100.8 | 33.2% | 8,984 | 5,336 | 40.6% | yes |
| mybatis_where_eq | 1,051.1 | 771.4 | 26.6% | 2,960 | 1,816 | 38.6% | yes |
| named_batch_4 | 2,965.8 | 2,080.0 | 29.9% | 9,736 | 5,616 | 42.3% | yes |
| named_batch_64 | 38,395.8 | 23,398.1 | 39.1% | 134,912 | 70,312 | 47.9% | yes |
| named_batch_beans | 8,413.7 | 6,567.2 | 21.9% | 26,064 | 18,616 | 28.6% | yes |
| named_between | 1,370.6 | 1,114.8 | 18.7% | 3,800 | 2,600 | 31.6% | yes |
| named_condition_only | 5,638.1 | 3,733.2 | 33.8% | 9,560 | 6,384 | 33.2% | yes |
| named_delete | 466.6 | 375.9 | 19.4% | 1,960 | 1,384 | 29.4% | yes |
| named_derived | 10,635.2 | 4,355.7 | 59.0% | 17,800 | 7,256 | 59.2% | yes |
| named_dialect_DB2 | 1,364.5 | 1,075.4 | 21.2% | 4,320 | 2,584 | 40.2% | yes |
| named_dialect_Microsoft_SQL_Server | 1,554.0 | 1,209.3 | 22.2% | 4,320 | 2,584 | 40.2% | yes |
| named_dialect_MySQL | 1,309.5 | 1,031.3 | 21.2% | 4,144 | 2,408 | 41.9% | yes |
| named_dialect_Oracle | 1,373.4 | 1,021.8 | 25.6% | 4,272 | 2,584 | 39.5% | yes |
| named_dialect_PostgreSQL | 1,407.8 | 1,072.6 | 23.8% | 4,144 | 2,408 | 41.9% | yes |
| named_group_order | 3,964.9 | 1,932.2 | 51.3% | 7,688 | 3,936 | 48.8% | yes |
| named_in_1024 | 130,085.7 | 93,163.3 | 28.4% | 353,216 | 278,560 | 21.1% | yes |
| named_in_16 | 2,196.2 | 1,744.9 | 20.5% | 7,600 | 5,520 | 27.4% | yes |
| named_in_17 | 2,312.4 | 1,803.0 | 22.0% | 7,888 | 5,736 | 27.3% | yes |
| named_in_256 | 27,110.3 | 21,698.2 | 20.0% | 90,712 | 71,352 | 21.3% | yes |
| named_in_expression | 27,734.9 | 23,887.1 | 13.9% | 90,680 | 72,456 | 20.1% | yes |
| named_insert_entity | 939.9 | 764.6 | 18.7% | 2,760 | 1,904 | 31.0% | yes |
| named_insert_placeholders | 979.3 | 724.7 | 26.0% | 2,888 | 1,952 | 32.4% | yes |
| named_insert_values | 1,140.7 | 851.3 | 25.4% | 3,048 | 2,176 | 28.6% | yes |
| named_join_on | 1,731.3 | 1,267.6 | 26.8% | 4,288 | 2,808 | 34.5% | yes |
| named_join_using | 996.2 | 828.0 | 16.9% | 3,512 | 2,136 | 39.2% | yes |
| named_late_modifier | 792.4 | 623.6 | 21.3% | 2,816 | 1,816 | 35.5% | yes |
| named_mixed_literals | 1,576.5 | 1,140.2 | 27.7% | 3,768 | 2,416 | 35.9% | yes |
| named_naming_CAMEL_CASE | 5,031.8 | 3,474.4 | 31.0% | 9,624 | 5,168 | 46.3% | yes |
| named_naming_KEBAB_CASE | 6,144.6 | 4,339.0 | 29.4% | 10,584 | 6,936 | 34.5% | yes |
| named_naming_NO_CHANGE | 4,928.0 | 3,016.2 | 38.8% | 9,624 | 5,168 | 46.3% | yes |
| named_naming_SCREAMING_SNAKE_CASE | 6,171.4 | 4,379.1 | 29.0% | 10,600 | 6,952 | 34.4% | yes |
| named_naming_UPPER_CAMEL_CASE | 6,314.8 | 4,616.6 | 26.9% | 10,584 | 6,664 | 37.0% | yes |
| named_nested | 1,434.2 | 1,086.4 | 24.2% | 3,648 | 2,256 | 38.2% | yes |
| named_pagination | 1,256.3 | 986.1 | 21.5% | 4,072 | 2,384 | 41.5% | yes |
| named_raw_expression | 1,529.1 | 942.9 | 38.3% | 2,624 | 1,744 | 33.5% | yes |
| named_repeated_names | 27,233.6 | 19,908.2 | 26.9% | 64,920 | 42,584 | 34.4% | yes |
| named_retry_rollback | 4,698.0 | 3,489.7 | 25.7% | 8,264 | 5,304 | 35.8% | yes |
| named_row_in | 1,382.5 | 1,088.2 | 21.3% | 4,016 | 2,680 | 33.3% | yes |
| named_select_columns | 829.1 | 618.9 | 25.4% | 1,952 | 1,216 | 37.7% | yes |
| named_select_entity | 591.5 | 544.8 | 7.9% | 2,000 | 1,304 | 34.8% | yes |
| named_select_expressions | 2,155.3 | 1,438.1 | 33.3% | 2,728 | 1,936 | 29.0% | yes |
| named_select_star | 301.4 | 217.8 | 27.7% | 1,424 | 880 | 38.2% | yes |
| named_subquery_raw | 637.7 | 506.0 | 20.6% | 2,272 | 1,344 | 40.8% | yes |
| named_subquery_structured | 1,813.8 | 1,501.6 | 17.2% | 5,488 | 3,568 | 35.0% | yes |
| named_union_builder | 10,683.6 | 4,338.3 | 59.4% | 17,272 | 6,840 | 60.4% | yes |
| named_union_raw | 3,248.3 | 1,072.3 | 67.0% | 5,200 | 1,472 | 71.7% | yes |
| named_update_chain_16 | 17,135.5 | 9,642.6 | 43.7% | 47,000 | 24,072 | 48.8% | yes |
| named_update_map | 1,778.8 | 1,218.4 | 31.5% | 4,752 | 2,976 | 37.4% | yes |
| named_where_128 | 48,434.2 | 32,865.6 | 32.1% | 71,280 | 47,920 | 32.8% | yes |
| named_where_16 | 6,429.9 | 4,447.7 | 30.8% | 10,584 | 6,936 | 34.5% | yes |
| named_where_eq | 1,036.7 | 771.1 | 25.6% | 3,120 | 1,976 | 36.7% | yes |
| positional_batch_4 | 1,937.9 | 1,254.0 | 35.3% | 5,968 | 2,840 | 52.4% | yes |
| positional_batch_64 | 18,058.1 | 9,723.7 | 46.2% | 72,176 | 23,448 | 67.5% | yes |
| positional_batch_beans | 4,286.3 | 2,940.3 | 31.4% | 10,464 | 6,984 | 33.3% | yes |
| positional_between | 1,146.7 | 759.3 | 33.8% | 3,056 | 1,736 | 43.2% | yes |
| positional_condition_only | 3,798.9 | 2,692.4 | 29.1% | 4,872 | 3,104 | 36.3% | yes |
| positional_delete | 340.1 | 286.9 | 15.7% | 1,504 | 1,000 | 33.5% | yes |
| positional_derived | 9,326.5 | 3,537.6 | 62.1% | 15,296 | 5,480 | 64.2% | yes |
| positional_dialect_DB2 | 1,123.4 | 845.5 | 24.7% | 3,544 | 2,032 | 42.7% | yes |
| positional_dialect_Microsoft_SQL_Server | 1,316.2 | 1,027.8 | 21.9% | 3,544 | 2,032 | 42.7% | yes |
| positional_dialect_MySQL | 1,061.0 | 803.4 | 24.3% | 3,360 | 1,848 | 45.0% | yes |
| positional_dialect_Oracle | 1,109.6 | 850.1 | 23.4% | 3,496 | 2,032 | 41.9% | yes |
| positional_dialect_PostgreSQL | 1,106.7 | 856.8 | 22.6% | 3,360 | 1,848 | 45.0% | yes |
| positional_group_order | 3,156.7 | 1,597.0 | 49.4% | 6,336 | 3,112 | 50.9% | yes |
| positional_in_1024 | 14,876.8 | 6,167.9 | 58.5% | 28,504 | 20,800 | 27.0% | yes |
| positional_in_16 | 873.8 | 708.1 | 19.0% | 2,656 | 1,728 | 34.9% | yes |
| positional_in_17 | 862.5 | 610.6 | 29.2% | 2,672 | 1,680 | 37.1% | yes |
| positional_in_256 | 4,430.3 | 2,133.1 | 51.9% | 9,576 | 6,208 | 35.2% | yes |
| positional_in_expression | 4,469.8 | 3,172.9 | 29.0% | 9,760 | 7,536 | 22.8% | yes |
| positional_insert_entity | 611.6 | 559.2 | 8.6% | 1,816 | 1,192 | 34.4% | yes |
| positional_insert_placeholders | 720.0 | 479.9 | 33.4% | 1,944 | 1,208 | 37.9% | yes |
| positional_insert_values | 838.4 | 632.4 | 24.6% | 2,104 | 1,464 | 30.4% | yes |
| positional_join_on | 1,617.4 | 1,140.2 | 29.5% | 3,832 | 2,424 | 36.7% | yes |
| positional_join_using | 875.6 | 718.8 | 17.9% | 3,056 | 1,752 | 42.7% | yes |
| positional_late_modifier | 670.6 | 515.1 | 23.2% | 2,360 | 1,432 | 39.3% | yes |
| positional_mixed_literals | 1,228.3 | 885.5 | 27.9% | 2,800 | 1,696 | 39.4% | yes |
| positional_naming_CAMEL_CASE | 3,342.2 | 2,297.4 | 31.3% | 4,928 | 1,880 | 61.9% | yes |
| positional_naming_KEBAB_CASE | 4,363.9 | 3,271.2 | 25.0% | 5,888 | 3,648 | 38.0% | yes |
| positional_naming_NO_CHANGE | 3,243.8 | 1,984.2 | 38.8% | 4,928 | 1,880 | 61.9% | yes |
| positional_naming_SCREAMING_SNAKE_CASE | 4,547.0 | 3,205.3 | 29.5% | 5,904 | 3,664 | 37.9% | yes |
| positional_naming_UPPER_CAMEL_CASE | 4,541.3 | 3,415.3 | 24.8% | 5,888 | 3,376 | 42.7% | yes |
| positional_nested | 1,215.0 | 946.1 | 22.1% | 2,952 | 1,704 | 42.3% | yes |
| positional_pagination | 988.2 | 759.3 | 23.2% | 3,288 | 1,824 | 44.5% | yes |
| positional_raw_expression | 1,571.0 | 953.5 | 39.3% | 2,624 | 1,744 | 33.5% | yes |
| positional_repeated_names | 16,276.4 | 11,671.1 | 28.3% | 30,360 | 17,240 | 43.2% | yes |
| positional_retry_rollback | 3,191.7 | 2,542.8 | 20.3% | 5,872 | 3,864 | 34.2% | yes |
| positional_row_in | 982.1 | 720.5 | 26.6% | 2,624 | 1,608 | 38.7% | yes |
| positional_select_columns | 849.3 | 609.3 | 28.3% | 1,952 | 1,216 | 37.7% | yes |
| positional_select_entity | 605.2 | 543.7 | 10.2% | 2,000 | 1,304 | 34.8% | yes |
| positional_select_expressions | 2,095.4 | 1,427.5 | 31.9% | 2,728 | 1,936 | 29.0% | yes |
| positional_select_star | 288.2 | 223.1 | 22.6% | 1,424 | 880 | 38.2% | yes |
| positional_subquery_raw | 631.6 | 496.2 | 21.4% | 2,272 | 1,344 | 40.8% | yes |
| positional_subquery_structured | 1,450.4 | 1,119.4 | 22.8% | 4,704 | 2,856 | 39.3% | yes |
| positional_union_builder | 5,343.4 | 2,322.9 | 56.5% | 9,768 | 3,968 | 59.4% | yes |
| positional_union_raw | 3,282.3 | 1,078.5 | 67.1% | 5,200 | 1,472 | 71.7% | yes |
| positional_update_chain_16 | 6,484.6 | 4,965.2 | 23.4% | 22,360 | 14,560 | 34.9% | yes |
| positional_update_map | 1,089.1 | 784.6 | 28.0% | 3,024 | 1,872 | 38.1% | yes |
| positional_where_128 | 33,105.5 | 22,768.8 | 31.2% | 32,808 | 20,712 | 36.9% | yes |
| positional_where_16 | 4,463.3 | 3,083.9 | 30.9% | 5,888 | 3,648 | 38.0% | yes |
| positional_where_eq | 923.9 | 685.1 | 25.8% | 2,664 | 1,592 | 40.2% | yes |
| raw_batch_4 | 2,650.2 | 1,343.0 | 49.3% | 7,584 | 3,304 | 56.4% | no, escaping |
| raw_batch_64 | 28,635.4 | 11,983.1 | 58.2% | 97,344 | 30,184 | 69.0% | no, escaping |
| raw_batch_beans | 6,492.1 | 3,617.4 | 44.3% | 16,896 | 8,808 | 47.9% | no, escaping |
| raw_between | 1,251.4 | 737.2 | 41.1% | 3,384 | 1,648 | 51.3% | yes |
| raw_condition_only | 4,833.1 | 2,560.9 | 47.0% | 7,816 | 2,720 | 65.2% | yes |
| raw_delete | 432.5 | 273.7 | 36.7% | 1,624 | 912 | 43.8% | yes |
| raw_derived | 9,578.5 | 3,385.9 | 64.7% | 15,848 | 5,216 | 67.1% | yes |
| raw_group_order | 3,306.6 | 1,505.8 | 54.5% | 6,432 | 2,952 | 54.1% | yes |
| raw_in_1024 | 77,429.0 | 13,464.1 | 82.6% | 220,248 | 6,328 | 97.1% | yes |
| raw_in_16 | 1,714.3 | 632.3 | 63.1% | 5,600 | 1,344 | 76.0% | yes |
| raw_in_17 | 1,795.5 | 632.3 | 64.8% | 5,816 | 1,352 | 76.8% | yes |
| raw_in_256 | 18,590.7 | 3,637.8 | 80.4% | 56,640 | 2,464 | 95.6% | yes |
| raw_in_expression | 18,691.4 | 4,856.5 | 74.0% | 56,616 | 3,712 | 93.4% | yes |
| raw_insert_entity | 848.7 | 622.1 | 26.7% | 2,176 | 1,264 | 41.9% | no, escaping |
| raw_insert_placeholders | 678.0 | 480.8 | 29.1% | 1,944 | 1,208 | 37.9% | yes |
| raw_insert_values | 1,055.4 | 722.1 | 31.6% | 2,464 | 1,536 | 37.7% | no, escaping |
| raw_join_on | 1,742.1 | 1,115.9 | 35.9% | 3,952 | 2,336 | 40.9% | yes |
| raw_join_using | 931.9 | 683.6 | 26.7% | 3,176 | 1,664 | 47.6% | yes |
| raw_late_modifier | 733.1 | 493.5 | 32.7% | 2,480 | 1,344 | 45.8% | yes |
| raw_mixed_literals | 1,543.4 | 1,062.1 | 31.2% | 3,192 | 1,864 | 41.6% | yes |
| raw_naming_CAMEL_CASE | 4,126.3 | 2,342.2 | 43.2% | 7,880 | 1,504 | 80.9% | yes |
| raw_naming_KEBAB_CASE | 5,381.1 | 3,168.8 | 41.1% | 8,840 | 3,272 | 63.0% | yes |
| raw_naming_NO_CHANGE | 4,239.4 | 1,952.6 | 53.9% | 7,880 | 1,504 | 80.9% | yes |
| raw_naming_SCREAMING_SNAKE_CASE | 5,339.8 | 3,161.8 | 40.8% | 8,856 | 3,288 | 62.9% | yes |
| raw_naming_UPPER_CAMEL_CASE | 5,400.6 | 3,258.3 | 39.7% | 8,840 | 3,000 | 66.1% | yes |
| raw_nested | 1,359.4 | 883.5 | 35.0% | 3,280 | 1,616 | 50.7% | yes |
| raw_pagination | 1,030.4 | 723.3 | 29.8% | 3,400 | 1,712 | 49.6% | yes |
| raw_raw_expression | 1,572.5 | 962.2 | 38.8% | 2,624 | 1,744 | 33.5% | yes |
| raw_repeated_names | 23,071.9 | 11,911.2 | 48.4% | 53,896 | 14,152 | 73.7% | yes |
| raw_retry_rollback | 3,919.7 | 2,792.1 | 28.8% | 6,376 | 3,976 | 37.6% | yes |
| raw_row_in | 1,236.9 | 764.8 | 38.2% | 3,136 | 1,608 | 48.7% | yes |
| raw_select_columns | 828.5 | 617.2 | 25.5% | 1,952 | 1,216 | 37.7% | yes |
| raw_select_entity | 584.0 | 535.3 | 8.3% | 2,000 | 1,304 | 34.8% | yes |
| raw_select_expressions | 2,105.2 | 1,459.4 | 30.7% | 2,728 | 1,936 | 29.0% | yes |
| raw_select_star | 307.8 | 219.0 | 28.8% | 1,424 | 880 | 38.2% | yes |
| raw_subquery_raw | 638.4 | 500.6 | 21.6% | 2,272 | 1,344 | 40.8% | yes |
| raw_subquery_structured | 1,415.0 | 1,119.9 | 20.9% | 4,616 | 2,720 | 41.1% | yes |
| raw_union_builder | 5,552.0 | 2,165.9 | 61.0% | 10,160 | 3,752 | 63.1% | yes |
| raw_union_raw | 3,469.0 | 1,100.0 | 68.3% | 5,200 | 1,472 | 71.7% | yes |
| raw_update_chain_16 | 8,112.7 | 4,898.7 | 39.6% | 25,448 | 13,856 | 45.6% | yes |
| raw_update_map | 1,381.5 | 825.6 | 40.2% | 3,592 | 1,960 | 45.4% | no, escaping |
| raw_where_128 | 39,734.2 | 22,231.5 | 44.0% | 56,496 | 17,776 | 68.5% | yes |
| raw_where_16 | 5,299.9 | 2,998.0 | 43.4% | 8,840 | 3,272 | 63.0% | yes |
| raw_where_eq | 1,000.9 | 639.2 | 36.1% | 2,784 | 1,504 | 46.0% | yes |
