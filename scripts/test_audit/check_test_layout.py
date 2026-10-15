#!/usr/bin/env python3
"""Check test naming, visibility, and source order without imposing execution order."""
from __future__ import annotations

import argparse
from collections import defaultdict
import json
from pathlib import Path
import re

from java_tokens import code_mask
from parse_tests import inventory


def layout_issues(methods):
    issues = []
    owners = defaultdict(list)
    for method in methods:
        owners[(method['file'], method['class_name'])].append(method)
        signature = code_mask(method['sig_body']).split('{', 1)[0]
        reason = None
        if re.search(r'\b(public|protected|private)\b', signature):
            reason = 'Test methods must be package-private'
        elif not method['name'].startswith('test'):
            reason = 'Test names must start with test'
        elif re.search(r'^test\d+(?:st|nd|rd|th)Pass_|_(?:Pass|Batch)\d+$', method['name']):
            reason = 'Name the behavior instead of the cleanup pass or batch'
        if reason:
            issues.append({'file': method['file'], 'class': method['class_name'],
                           'method': method['name'], 'line': method['sig_line'], 'reason': reason})

    for (file, owner), members in owners.items():
        ordered = sorted(members, key=lambda m: m['start_line'])
        previous = None
        for method in ordered:
            if previous and previous['name'].casefold() > method['name'].casefold():
                issues.append({'file': file, 'class': owner, 'method': method['name'],
                               'line': method['sig_line'],
                               'reason': 'Source order: place before ' + previous['name']})
            previous = method
    return issues


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--input', type=Path, help='Use a previously generated source inventory')
    parser.add_argument('--out', type=Path)
    args = parser.parse_args()
    data = json.loads(args.input.read_text(encoding='utf8')) if args.input else inventory(args.root)
    issues = layout_issues(data['methods'])
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(issues, indent=2) + '\n', encoding='utf8')
    for issue in issues:
        print(f"{issue['file']}:{issue['line']}: {issue['class']}.{issue['method']}: {issue['reason']}")
    print(f"Checked {len(data['methods'])} test methods: {len(issues)} layout issues")
    return bool(issues)


if __name__ == '__main__':
    raise SystemExit(main())
