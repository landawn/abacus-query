#!/usr/bin/env python3
"""
Find duplicate @Test methods across the test suite.

A duplicate is a group of >=2 test methods whose normalized bodies are identical
(or, with --fuzzy, whose normalized bodies are very similar). For each group we
report the file/class/line of every member and the verbatim body of the first
member, so a human can pick the canonical version to keep.

Normalization (see parse_tests.normalize_body):
  - comments removed
  - string/char literals collapsed to placeholders
  - whitespace collapsed

Tunables:
  --min-size N     only report groups with >= N members (default 2)
  --cross-file     only report groups spanning more than one file
  --fuzzy          use a token-similarity threshold instead of exact match
  --sim-threshold  0..1, default 0.92
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from difflib import SequenceMatcher


def similarity(a: str, b: str) -> float:
    if not a and not b:
        return 1.0
    return SequenceMatcher(None, a, b).ratio()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", default="scripts/test_audit/parse_output.json")
    ap.add_argument("--out", default=None, help="write report here (else stdout)")
    ap.add_argument("--min-size", type=int, default=2)
    ap.add_argument("--cross-file", action="store_true")
    ap.add_argument("--fuzzy", action="store_true")
    ap.add_argument("--sim-threshold", type=float, default=0.92)
    ap.add_argument("--max-groups", type=int, default=200, help="cap reported groups")
    ap.add_argument("--show-body", action="store_true", help="include the verbatim body of the first member")
    args = ap.parse_args()

    data = json.load(open(args.input, encoding="utf-8"))
    methods = data["methods"]

    if args.fuzzy:
        # O(n^2) -- only feasible if filtered; require same method name first to prune
        by_name = defaultdict(list)
        for m in methods:
            by_name[m["name"]].append(m)
        groups = []
        seen = set()
        for name, lst in by_name.items():
            if len(lst) < 2:
                continue
            for i, a in enumerate(lst):
                if id(a) in seen:
                    continue
                cluster = [a]
                for b in lst[i+1:]:
                    if id(b) in seen:
                        continue
                    if similarity(a["norm_body"], b["norm_body"]) >= args.sim_threshold:
                        cluster.append(b)
                        seen.add(id(b))
                if len(cluster) >= args.min_size:
                    groups.append(cluster)
                    seen.add(id(a))
        # sort by size desc
        groups.sort(key=lambda g: -len(g))
    else:
        by_key = defaultdict(list)
        for m in methods:
            by_key[m["norm_body"]].append(m)
        groups = [g for g in by_key.values() if len(g) >= args.min_size]
        groups.sort(key=lambda g: -len(g))

    report = []
    for g in groups[:args.max_groups]:
        files = {m["file"] for m in g}
        if args.cross_file and len(files) < 2:
            continue
        entry = {
            "size": len(g),
            "files": sorted(files),
            "method_names": sorted({m["name"] for m in g}),
            "members": [
                {"file": m["file"], "class": m["class_name"], "name": m["name"],
                 "line": m["sig_line"], "class_tag": m["class_tag"]}
                for m in g
            ],
        }
        if args.show_body:
            entry["sample_body"] = g[0]["sig_body"]
        report.append(entry)

    out_text = json.dumps(report, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(out_text)
        print(f"wrote {args.out}: {len(report)} duplicate groups "
              f"(covering {sum(g['size'] for g in report)} methods)", file=sys.stderr)
    else:
        print(out_text)


if __name__ == "__main__":
    main()