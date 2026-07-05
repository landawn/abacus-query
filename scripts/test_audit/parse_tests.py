#!/usr/bin/env python3
"""
Parse all JUnit test files under src/test/java and extract every @Test method.

For each test method we capture:
  - file path (relative to repo root)
  - enclosing class simple name (nearest preceding class decl)
  - class tag (the @Tag(...) on the enclosing class, if any)
  - method name
  - full method source (signature + body), verbatim
  - normalized body (comments + whitespace stripped, for dup detection)
  - line range (1-indexed start/end of the method)

Also classifies whether the method body contains a "meaningful assertion" call.

Usage:
    python parse_tests.py [--root <repo-root>] [--out <json-file>]
                          [--only-package com.landawn.abacus.query.condition]

Default repo root is the parent of this script's parent (..).
Output is written to stdout as JSON (or to --out file) with shape:
    {
      "files":   [ <relpath>, ... ],
      "classes": [ { "file", "name", "tag", "super", "start_line", "end_line" }, ... ],
      "methods": [ { "file", "class", "class_tag", "name", "start_line", "end_line",
                    "body", "norm_body", "has_assertion" }, ... ]
    }
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from dataclasses import dataclass, field, asdict
from typing import List, Tuple


# --------------------------------------------------------------------------- #
# Tokenizing scanner: walks a Java source string and yields (kind, text, line)
# where kind in {CODE, LINE_COMMENT, BLOCK_COMMENT, STRING, CHAR, WS, NEWLINE}.
# We need comment/char/string awareness so brace matching ignores braces that
# appear inside them, and so normalized bodies can strip comments cleanly.
# --------------------------------------------------------------------------- #

class Scanner:
    def __init__(self, src: str):
        self.src = src
        self.n = len(src)
        self.i = 0
        self.line = 1

    def peek(self, k: int = 0) -> str:
        j = self.i + k
        return self.src[j] if j < self.n else ""

    def advance(self) -> str:
        c = self.src[self.i]
        self.i += 1
        if c == "\n":
            self.line += 1
        return c

    def at_end(self) -> bool:
        return self.i >= self.n


def scan(src: str) -> List[Tuple[str, str, int]]:
    """Return a list of (kind, text, start_line) tokens covering all of src."""
    s = Scanner(src)
    out: List[Tuple[str, str, int]] = []
    buf = []
    buf_line = 0

    def flush(kind: str):
        nonlocal buf, buf_line
        if buf:
            out.append((kind, "".join(buf), buf_line))
            buf = []

    while not s.at_end():
        c = s.peek()
        # whitespace
        if c in " \t\f":
            if not buf:
                buf_line = s.line
            buf.append(c)
            s.advance()
            continue
        if c == "\n":
            if not buf:
                buf_line = s.line
            buf.append(c)
            s.advance()
            continue
        # comments
        if c == "/" and s.peek(1) == "/":
            flush("CODE")
            line = s.line
            text = []
            while not s.at_end() and s.peek() != "\n":
                text.append(s.advance())
            out.append(("LINE_COMMENT", "".join(text), line))
            continue
        if c == "/" and s.peek(1) == "*":
            flush("CODE")
            line = s.line
            text = []
            text.append(s.advance())  # /
            text.append(s.advance())  # *
            while not s.at_end() and not (s.peek() == "*" and s.peek(1) == "/"):
                text.append(s.advance())
            if not s.at_end():
                text.append(s.advance())  # *
                text.append(s.advance())  # /
            out.append(("BLOCK_COMMENT", "".join(text), line))
            continue
        # string literal (watch for triple-quote text blocks """...""")
        if c == '"':
            flush("CODE")
            line = s.line
            text = []
            # text block?
            if s.peek(1) == '"' and s.peek(2) == '"':
                text.append(s.advance())
                text.append(s.advance())
                text.append(s.advance())
                while not s.at_end() and not (s.peek() == '"' and s.peek(1) == '"' and s.peek(2) == '"'):
                    text.append(s.advance())
                if not s.at_end():
                    text.append(s.advance())
                    text.append(s.advance())
                    text.append(s.advance())
            else:
                text.append(s.advance())  # opening "
                while not s.at_end() and s.peek() != '"':
                    if s.peek() == "\\":
                        text.append(s.advance())
                        if not s.at_end():
                            text.append(s.advance())
                    else:
                        text.append(s.advance())
                if not s.at_end():
                    text.append(s.advance())  # closing "
            out.append(("STRING", "".join(text), line))
            continue
        # char literal
        if c == "'":
            flush("CODE")
            line = s.line
            text = []
            text.append(s.advance())  # opening '
            while not s.at_end() and s.peek() != "'":
                if s.peek() == "\\":
                    text.append(s.advance())
                    if not s.at_end():
                        text.append(s.advance())
                else:
                    text.append(s.advance())
            if not s.at_end():
                text.append(s.advance())  # closing '
            out.append(("CHAR", "".join(text), line))
            continue
        # ordinary code char
        if not buf:
            buf_line = s.line
        buf.append(s.advance())
    flush("CODE")
    return out


# --------------------------------------------------------------------------- #
# Method / class extraction
# --------------------------------------------------------------------------- #

CLASS_DECL_RE = re.compile(
    r"(?:public|protected|private|abstract|final|static|\s)*"
    r"(?:class|interface|enum|record)\s+([A-Za-z_$][A-Za-z0-9_$<>_,\s.\[\]?]*)"
)

METHOD_DECL_RE = re.compile(
    r"(?:public|protected|private|static|final|synchronized|native|abstract|default|\s)*"
    r"(?:[A-Za-z_$][\w.$]*(?:\s*<[^>]*>)?)\s+"   # return type
    r"([A-Za-z_$][A-Za-z0-9_$]*)"                # method name
    r"\s*\([^)]*\)"                               # parameter list
)


@dataclass
class ClassInfo:
    file: str
    name: str
    super_class: str = ""
    tag: str = ""
    start_line: int = 0
    end_line: int = 0


@dataclass
class MethodInfo:
    file: str
    class_name: str
    class_tag: str
    name: str
    start_line: int  # first line of the @Test annotation (or method sig if none)
    sig_line: int    # line of the method signature
    end_line: int
    body: str        # full source from @Test (or sig) through closing brace
    sig_body: str    # source from method signature through closing brace
    has_test_anno: bool
    has_assertion: bool = False
    norm_body: str = field(default="")


ASSERTION_RE = re.compile(
    r"\b(?:"
    # JUnit 6 Assertions.* + Hamcrest-ish
    r"assertEquals|assertNotEquals|assertTrue|assertFalse|assertNull|assertNotNull|"
    r"assertSame|assertNotSame|assertArrayEquals|assertIterableEquals|"
    r"assertLinesMatch|assertThrows|assertThrowsExactly|assertDoesNotThrow|"
    r"assertTimeout|assertTimeoutPreemptively|assertAll|fail|expectThrows|"
    # TestBase helpers
    r"assertHaveSameElements|"
    # Any custom helper whose name starts with assert/expect/verify/validate
    r"assert[A-Z][A-Za-z0-9_]*|expect[A-Z][A-Za-z0-9_]*|"
    r"verify[A-Z][A-Za-z0-9_]*|validate[A-Z][A-Za-z0-9_]*"
    r")\s*\("
)


def find_classes(file_rel: str, tokens: List[Tuple[str, str, int]]) -> List[ClassInfo]:
    """Walk tokens (concatenated CODE) to find class/interface/enum/record decls
    and their brace spans. Returns classes with start/end lines."""
    # Reconstruct code-only source with line tracking
    code_chars: List[Tuple[str, int]] = []  # (char, line)
    for kind, text, line in tokens:
        if kind in ("LINE_COMMENT", "BLOCK_COMMENT"):
            # treat as a single newline-or-space so spans stay aligned
            for ch in text:
                if ch == "\n":
                    code_chars.append(("\n", line))
                else:
                    code_chars.append((" ", line))
            continue
        if kind in ("STRING", "CHAR"):
            for ch in text:
                if ch == "\n":
                    code_chars.append(("\n", line))
                else:
                    code_chars.append((" ", line))
            continue
        if kind == "CODE":
            start = 0
            for idx, ch in enumerate(text):
                if ch == "\n":
                    line += 1
                code_chars.append((ch, line))
            continue
        # WS / NEWLINE
        start = 0
        for ch in text:
            if ch == "\n":
                line += 1
            code_chars.append((ch, line))
        continue

    code = "".join(c for c, _ in code_chars)
    line_at = [ln for _, ln in code_chars]  # line number for each code char

    classes: List[ClassInfo] = []
    for m in CLASS_DECL_RE.finditer(code):
        # only consider 'class|interface|enum|record' keyword decls (skip stray words)
        kw_match = re.search(r"\b(class|interface|enum|record)\b", m.group(0))
        if not kw_match:
            continue
        name = m.group(1).strip().split("<")[0].strip()
        # find the extends/super
        # find tag for this class: look upward for nearest @Tag(...) on a class
        # find opening brace after this match
        brace_pos = code.find("{", m.end())
        if brace_pos == -1:
            continue
        # match braces (code-only, so strings/chars/comments already neutralized)
        depth = 1
        j = brace_pos + 1
        while j < len(code) and depth > 0:
            cj = code[j]
            if cj == "{":
                depth += 1
            elif cj == "}":
                depth -= 1
            j += 1
        end_pos = j - 1
        start_line = line_at[m.start()] if m.start() < len(line_at) else 0
        end_line = line_at[end_pos] if end_pos < len(line_at) else 0
        # tag for the class: nearest preceding @Tag(...) token
        tag = find_class_tag(tokens, m.start(), code, line_at)
        classes.append(ClassInfo(file=file_rel, name=name, tag=tag,
                                 start_line=start_line, end_line=end_line))
    return classes


def find_class_tag(tokens, code_pos, code, line_at) -> str:
    """Look backwards in the original tokens for an @Tag(...) preceding the class decl."""
    # find the line just before the class decl in original source; scan upward for @Tag
    # Simpler: search the original src for @Tag(...) annotations; for each, check it's
    # immediately above a class decl. We do this via tokens.
    # Walk tokens backwards from the class start position.
    # We reconstruct a token index -> code position mapping is hard; instead, use line.
    class_line = line_at[code_pos] if code_pos < len(line_at) else 0
    # find tokens that are CODE containing '@Tag'
    # We need the original file text; we'll pass it in via closure below.
    return ""


# Simpler tag finder using the raw source and class start line.
def find_tags_in_src(src: str) -> List[Tuple[int, str]]:
    """Return list of (line_no, tag_value) for every @Tag("...") in src."""
    out = []
    for m in re.finditer(r'@Tag\s*\(\s*"([^"]*)"\s*\)', src):
        line = src.count("\n", 0, m.start()) + 1
        out.append((line, m.group(1)))
    return out


def find_test_methods(src: str, file_rel: str, classes: List[ClassInfo]) -> List[MethodInfo]:
    """Find every method annotated with @Test (or @ParameterizedTest) and extract
    its full body via brace matching on the raw source."""
    methods: List[MethodInfo] = []
    n = len(src)
    # Find all @Test / @ParameterizedTest annotations
    anno_re = re.compile(r'@(Test|ParameterizedTest|RepeatedTest|TestFactory)\b')
    for m in anno_re.finditer(src):
        anno_end = m.end()
        # skip trailing whitespace/newlines, find the method decl
        k = anno_end
        # Skip any continuation annotations? JUnit allows @Test @DisplayName on same method.
        # We already anchored on @Test; the next non-ws tokens should be modifiers + return type + name(params) {
        # Find the method signature: scan forward for an identifier followed by ( ... )
        # then the body { ... }
        # We search for the first '(' that begins the param list. Heuristic: the method name
        # is the identifier immediately preceding the first '(' that is not inside a generic.
        # Use a small state machine.
        # Find first '{' after the anno that is at depth 0 outside of ()/[]/<>
        depth_paren = 0
        depth_brack = 0
        depth_angle = 0
        sig_start = None
        # locate signature start (first non-ws char after annotation)
        j = anno_end
        while j < n and src[j] in " \t\r\n":
            j += 1
        sig_start = j
        sig_line = src.count("\n", 0, sig_start) + 1
        # walk forward to find the matching '(' for the param list, then the body '{'
        # while tracking we don't cross a ';' (would mean it's not a method, e.g. a field).
        body_start = None
        idx = sig_start
        # First, find the method name + '('
        # We scan until we hit '(' at angle-depth 0.
        last_ident_end = None
        while idx < n:
            c = src[idx]
            if c == "(" and depth_angle == 0:
                # method param list start
                # method name ends just before this '(' at the last identifier
                break
            if c == ";":
                # not a method (e.g. annotation on a field) - skip
                break
            if c == "<":
                depth_angle += 1
            elif c == ">":
                if depth_angle > 0:
                    depth_angle -= 1
            if c.isidentifier() or c == "_" or c == "$":
                # accumulate identifier
                start = idx
                while idx < n and (src[idx].isalnum() or src[idx] in "_$"):
                    idx += 1
                last_ident_end = idx
                continue
            idx += 1
        if idx >= n or src[idx] != "(":
            continue
        method_name = src[sig_start:idx].strip().split("(")[0]
        # method_name may include return type; grab last identifier
        name_match = re.search(r'([A-Za-z_$][A-Za-z0-9_$]*)\s*$', src[sig_start:idx])
        if not name_match:
            continue
        method_name = name_match.group(1)
        # match the param list parens
        depth_paren = 1
        idx += 1
        while idx < n and depth_paren > 0:
            c = src[idx]
            if c == "(":
                depth_paren += 1
            elif c == ")":
                depth_paren -= 1
            elif c == '"':
                # skip string
                idx += 1
                while idx < n and src[idx] != '"':
                    if src[idx] == "\\":
                        idx += 1
                    idx += 1
            elif c == "'":
                idx += 1
                while idx < n and src[idx] != "'":
                    if src[idx] == "\\":
                        idx += 1
                    idx += 1
            elif c == "/" and idx + 1 < n and src[idx+1] == "/":
                while idx < n and src[idx] != "\n":
                    idx += 1
                continue
            elif c == "/" and idx + 1 < n and src[idx+1] == "*":
                idx += 2
                while idx + 1 < n and not (src[idx] == "*" and src[idx+1] == "/"):
                    idx += 1
                idx += 1
            idx += 1
        # now idx points just past ')'
        # find '{' (skip throws clause, etc.)
        while idx < n and src[idx] != "{" and src[idx] != ";":
            idx += 1
        if idx >= n or src[idx] == ";":
            continue  # abstract method or interface default? skip
        body_start = idx
        idx += 1
        depth_brace = 1
        body_chars = [src[body_start]]
        while idx < n and depth_brace > 0:
            c = src[idx]
            body_chars.append(c)
            if c == "{":
                depth_brace += 1
            elif c == "}":
                depth_brace -= 1
            elif c == '"':
                idx += 1
                while idx < n and src[idx] != '"':
                    if src[idx] == "\\":
                        idx += 1
                        body_chars.append(src[idx])
                    body_chars.append(src[idx])
                    idx += 1
                if idx < n:
                    body_chars.append(src[idx])
            elif c == "'":
                idx += 1
                while idx < n and src[idx] != "'":
                    if src[idx] == "\\":
                        idx += 1
                        body_chars.append(src[idx])
                    body_chars.append(src[idx])
                    idx += 1
                if idx < n:
                    body_chars.append(src[idx])
            elif c == "/" and idx + 1 < n and src[idx+1] == "/":
                while idx < n and src[idx] != "\n":
                    body_chars.append(src[idx])
                    idx += 1
                continue
            elif c == "/" and idx + 1 < n and src[idx+1] == "*":
                body_chars.append(src[idx]); body_chars.append(src[idx+1])
                idx += 2
                while idx + 1 < n and not (src[idx] == "*" and src[idx+1] == "/"):
                    body_chars.append(src[idx])
                    idx += 1
                if idx + 1 < n:
                    body_chars.append(src[idx]); body_chars.append(src[idx+1])
                    idx += 2
                continue
            idx += 1
        end_line = src.count("\n", 0, idx) + 1
        body = src[m.start():idx]
        sig_body = src[sig_start:idx]
        # enclosing class
        enclosing = ""
        enclosing_tag = ""
        for cl in classes:
            if cl.start_line <= sig_line <= cl.end_line:
                enclosing = cl.name
                enclosing_tag = cl.tag
        has_assertion = bool(ASSERTION_RE.search(sig_body))
        methods.append(MethodInfo(
            file=file_rel, class_name=enclosing, class_tag=enclosing_tag,
            name=method_name, start_line=src.count("\n", 0, m.start()) + 1,
            sig_line=sig_line, end_line=end_line, body=body, sig_body=sig_body,
            has_test_anno=True, has_assertion=has_assertion,
        ))
    return methods


def normalize_body(sig_body: str) -> str:
    """Strip comments, strings' contents (keep quotes as placeholders), collapse whitespace."""
    s = sig_body
    # remove block comments
    s = re.sub(r"/\*.*?\*/", " ", s, flags=re.S)
    # remove line comments
    s = re.sub(r"//[^\n]*", " ", s)
    # collapse strings to a placeholder (preserving quotedness so different literals
    # with the same shape still compare equal)
    s = re.sub(r'"(?:\\.|[^"\\])*"', '"_"', s)
    s = re.sub(r"'(?:\\.|[^'\\])*'", "'_'", s)
    # collapse all whitespace
    s = re.sub(r"\s+", " ", s).strip()
    return s


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=None, help="repo root (default: ../../.. from this file)")
    ap.add_argument("--out", default=None, help="write JSON to this file instead of stdout")
    ap.add_argument("--only-package", default=None,
                    help="restrict to a single sub-package, e.g. com.landawn.abacus.query.condition")
    args = ap.parse_args()

    here = os.path.dirname(os.path.abspath(__file__))
    repo_root = args.root or os.path.abspath(os.path.join(here, "..", ".."))
    test_root = os.path.join(repo_root, "src", "test", "java")

    if not os.path.isdir(test_root):
        print(f"error: {test_root} not found", file=sys.stderr)
        sys.exit(2)

    sub = args.only_package.replace(".", os.sep) if args.only_package else None
    file_list: List[str] = []
    for dirpath, dirnames, filenames in os.walk(test_root):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            full = os.path.join(dirpath, fn)
            rel = os.path.relpath(full, repo_root).replace(os.sep, "/")
            if sub and sub not in rel.replace(os.sep, "/"):
                continue
            file_list.append(rel)

    file_list.sort()

    all_classes: List[dict] = []
    all_methods: List[dict] = []

    for rel in file_list:
        full = os.path.join(repo_root, rel.replace("/", os.sep))
        with open(full, "r", encoding="utf-8", errors="replace") as f:
            src = f.read()
        tokens = scan(src)
        # find classes
        classes: List[ClassInfo] = []
        # We need tags: find @Tag lines in src
        tag_lines = find_tags_in_src(src)
        # find class decls with start/end lines
        for m in re.finditer(
            r"(?P<kw>\b(?:class|interface|enum|record)\b)\s+(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)",
            src,
        ):
            # skip matches inside strings/comments? do a cheap check: count quotes before
            # this position on the same line -- but most class decls are real. We rely on
            # the fact that the keyword 'class' is unlikely to appear in test strings often.
            # For robustness, only accept if preceded by a modifier or whitespace at start of line.
            start = m.start()
            # ensure this isn't inside a // or /* */ - cheap check via tokenizing
            # find nearest preceding non-ws char in tokens
            line_no = src.count("\n", 0, start) + 1
            # find opening brace
            brace_pos = src.find("{", m.end())
            if brace_pos == -1:
                continue
            depth = 1
            j = brace_pos + 1
            in_str = None
            while j < len(src) and depth > 0:
                c = src[j]
                if in_str:
                    if c == "\\":
                        j += 2
                        continue
                    if c == in_str:
                        in_str = None
                    j += 1
                    continue
                if c == '"':
                    in_str = '"'
                elif c == "'":
                    in_str = "'"
                elif c == "{":
                    depth += 1
                elif c == "}":
                    depth -= 1
                elif c == "/" and j + 1 < len(src) and src[j+1] == "/":
                    while j < len(src) and src[j] != "\n":
                        j += 1
                    continue
                elif c == "/" and j + 1 < len(src) and src[j+1] == "*":
                    j += 2
                    while j + 1 < len(src) and not (src[j] == "*" and src[j+1] == "/"):
                        j += 1
                    j += 2
                    continue
                j += 1
            end_pos = j - 1
            end_line = src.count("\n", 0, end_pos) + 1
            # find tag: nearest @Tag at a line <= class line, that is within a few lines above
            tag = ""
            for tl, tv in tag_lines:
                if tl <= line_no and line_no - tl < 5:
                    tag = tv
            super_match = re.search(r"\bextends\s+([A-Za-z_$][A-Za-z0-9_$<>,\s.]*)", src[m.end():m.end()+200])
            super_class = super_match.group(1).strip().split("<")[0] if super_match else ""
            classes.append(ClassInfo(file=rel, name=m.group("name"), tag=tag,
                                     super_class=super_class,
                                     start_line=line_no, end_line=end_line))

        # find test methods
        methods = find_test_methods(src, rel, classes)
        for cl in classes:
            all_classes.append(asdict(cl))
        for me in methods:
            d = asdict(me)
            d["norm_body"] = normalize_body(me.sig_body)
            all_methods.append(d)

    result = {"files": file_list, "classes": all_classes, "methods": all_methods}
    text = json.dumps(result, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(text)
        print(f"wrote {args.out}: {len(file_list)} files, "
              f"{len(all_classes)} classes, {len(all_methods)} test methods", file=sys.stderr)
    else:
        print(text)


if __name__ == "__main__":
    main()