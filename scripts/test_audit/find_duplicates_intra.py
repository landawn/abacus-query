#!/usr/bin/env python3
"""
Find duplicate @Test methods WITHIN the same test class (same file + same class).

After consolidation, every source class has one test class, so true duplicates
are methods in the SAME class that have:

  (a) identical normalized bodies but different names, or
  (b) highly similar normalized bodies (>= --sim-threshold) but different names

Cross-class parallel structure (e.g. testConstructor_NullPropertyName in
EqualTest vs IsNullTest) is NOT reported here -- those test different source
classes and are distinct tests.

Usage:
    python find_duplicates_intra.py [--input parse_output.json] [--out report.json]
        [--sim-threshold 0.93] [--exact-only]
"""
from __future__ import annotations

import argparse
import json
from collections import defaultdict
from difflib import SequenceMatcher


def sim(a: str, b: str) -> float:
    if not a and not b:
        return 1.0
    return SequenceMatcher(None, a, b).ratio()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", default="scripts/test_audit/parse_output.json")
    ap.add_argument("--out", default=None)
    ap.add_argument("--sim-threshold", type=float, default=0.93)
    ap.add_argument("--exact-only", action="store_true")
    ap.add_argument("--min-body-len", type=int, default=80,
                    help="ignore very short methods (likely trivially similar)")
    args = ap.parse_args()

    data = json.load(open(args.input, encoding="utf-8"))
    methods = data["methods"]

    # group by (file, class_name)
    by_class = defaultdict(list)
    for m in methods:
        by_class[(m["file"], m["class_name"])].append(m)

    groups = []
    for (file, cls), ms in by_class.items():
        if len(ms) < 2:
            continue
        # exact-body duplicates
        if args.exact_only:
            by_body = defaultdict(list)
            for m in ms:
                by_body[m["norm_body"]].append(m)
            for body, lst in by_body.items():
                if len(lst) >= 2 and len(body) >= args.min_body_len:
                    groups.append({"file": file, "class": cls,
                                   "members": [{"name": m["name"], "line": m["sig_line"]}
                                               for m in lst],
                                   "similarity": 1.0})
            continue
        # fuzzy: O(n^2) within class
        seen = set()
        for i, a in enumerate(ms):
            if id(a) in seen:
                continue
            if len(a["norm_body"]) < args.min_body_len:
                continue
            cluster = [a]
            for b in ms[i+1:]:
                if id(b) in seen:
                    continue
                if len(b["norm_body"]) < args.min_body_len:
                    continue
                if a["name"] == b["name"]:
                    continue
                s = sim(a["norm_body"], b["norm_body"])
                if s >= args.sim_threshold:
                    cluster.append(b)
                    seen.add(id(b))
            if len(cluster) >= 2:
                seen.add(id(a))
                groups.append({"file": file, "class": cls,
                               "members": [{"name": m["name"], "line": m["sig_line"]}
                                           for m in cluster],
                               "similarity": round(s, 3),
                               "sample_body": cluster[0]["sig_body"][:400]})

    groups.sort(key=lambda g: -g["similarity"])
    out = json.dumps(groups, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(out)
        print(f"wrote {args.out}: {len(groups)} intra-class duplicate groups",
              file=sys.stderr)
    else:
        print(out)


if __name__ == "__main__":
    main()