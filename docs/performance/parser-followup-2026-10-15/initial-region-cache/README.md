# Intermediate region-cache measurements

This exploratory snapshot already fixes the reported compatibility, overlapping-region, memo-growth and cursor issues. Its last-start shortcut covers only nonkeyword starts. The same-first-word control regressed from 0.962 ms to 1.342 ms at 8,192 brackets, while the distinct-start overlap improved from 1.703 seconds to 2.628 ms at 8,000 brackets.

These measurements motivated a separate scalar cache for the latest qualifying start with a negative answer. The final implementation includes that cache, and the parent report uses a fresh measurement run. Files here are historical evidence and are excluded from the final summaries; their source hashes identify this intermediate snapshot.
