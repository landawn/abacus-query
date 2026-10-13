#!/usr/bin/env python3
"""
Find @Test methods whose body contains no meaningful assertion call.

A method is flagged when it has no call to any of the JUnit 6 assertion methods
(assertEquals, assertThrows, assertDoesNotThrow, fail, assertAll, ...) nor to
TestBase.assertHaveSameElements / N.assertXxx helpers.

The report lists file, class, method name, line range, and the method body so a
human can add a meaningful assertion (or replace a try/catch with assertThrows).

All tests are audited by default. --tagged-only restricts the report to unit
tags (including inherited tags); it is not a substitute for JUnit discovery.
"""
from __future__ import annotations

import argparse
import json
import sys


RUN_TAGS = {"unit", "base-test", "2025"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", default=".audit/test-audit/parse_output.json")
    ap.add_argument("--out", default=None)
    ap.add_argument("--tagged-only", action="store_true", default=False,
                    help="restrict to unit/base-test/2025 tags (including inherited tags)")
    ap.add_argument("--include-untagged", dest="tagged_only", action="store_false")
    args = ap.parse_args()

    data = json.load(open(args.input, encoding="utf-8"))
    methods = data["methods"]

    flagged = []
    for m in methods:
        if m.get("has_assertion"):
            continue
        if args.tagged_only and not RUN_TAGS.intersection(m.get("class_tags", [m.get("class_tag")])):
            # untagged classes (or tagged with something other than base-test/2025)
            # don't run under `mvn test`, so don't flag them.
            continue
        flagged.append(m)

    flagged.sort(key=lambda m: (m["file"], m["sig_line"]))
    out = json.dumps(flagged, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(out)
        print(f"wrote {args.out}: {len(flagged)} methods without assertions "
              f"(tagged-only={args.tagged_only})", file=sys.stderr)
    else:
        print(out)


if __name__ == "__main__":
    main()
