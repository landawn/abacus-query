#!/usr/bin/env python3
"""
Structural parser for Java test files under src/test/java.

Splits a file into:
  - package declaration (verbatim line)
  - imports (list of verbatim lines)
  - top-level classes: each with
        * leading_annotations  (text between the last top-level separator and the
          class keyword, e.g. "@Tag(\"2025\")\n")
        * signature            (the "class Foo extends Bar {" line, minus the '{')
        * name                 (simple class name)
        * super_class          (simple super name, or "")
        * header_span          (start..end line of annotations+signature)
        * body_text            (verbatim text between the opening '{' and matching '}')
        * body_start_line, body_end_line

Only TOP-LEVEL classes are returned (brace depth 0). Nested classes stay embedded
in their parent's body_text.

Also extracts, for each top-level class, the direct member methods that are
annotated @Test (or @ParameterizedTest/@RepeatedTest/@TestFactory) plus any
non-test helper methods, each with verbatim source and line span. Nested-class
methods are NOT extracted (they belong to the nested class).

Used by merge_file.py to merge multiple test classes in one file into one.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import List, Optional, Tuple


@dataclass
class MethodInfo:
    name: str
    text: str            # verbatim source: annotations + signature + body
    start_line: int
    end_line: int
    is_test: bool
    annotations: str = ""  # verbatim annotation text preceding the signature


@dataclass
class ClassInfo:
    name: str
    super_class: str
    leading_annotations: str   # text above the class keyword (e.g. @Tag("2025")\n)
    signature: str             # "class Foo extends Bar"
    file_body: str             # verbatim body between { and matching }
    body_start_line: int
    body_end_line: int
    header_start_line: int     # first line of leading_annotations (or signature)
    sig_line: int              # line of the signature
    methods: List[MethodInfo] = field(default_factory=list)
    fields_text: List[str] = field(default_factory=list)  # non-method members (verbatim)


def _scan_strings_comments(src: str, i: int, end_ch: str) -> int:
    """Skip a string/char literal starting at i (src[i] == opening quote). Return
    index just past the closing quote."""
    n = len(src)
    i += 1
    while i < n and src[i] != end_ch:
        if src[i] == "\\":
            i += 2
            continue
        i += 1
    return i + 1


def _find_matching_brace(src: str, open_idx: int) -> int:
    """open_idx points at '{'. Return index of the matching '}'."""
    n = len(src)
    depth = 0
    i = open_idx
    while i < n:
        c = src[i]
        if c == "{":
            depth += 1
            i += 1
        elif c == "}":
            depth -= 1
            i += 1
            if depth == 0:
                return i - 1
        elif c == '"':
            i = _scan_strings_comments(src, i, '"')
        elif c == "'":
            i = _scan_strings_comments(src, i, "'")
        elif c == "/" and i + 1 < n and src[i+1] == "/":
            while i < n and src[i] != "\n":
                i += 1
        elif c == "/" and i + 1 < n and src[i+1] == "*":
            i += 2
            while i + 1 < n and not (src[i] == "*" and src[i+1] == "/"):
                i += 1
            i += 2
        else:
            i += 1
    return -1


CLASS_KW_RE = re.compile(r"\b(class|interface|enum|record)\b")


def parse_file(src: str) -> Tuple[str, List[str], List[ClassInfo]]:
    """Return (package_line, imports, top_level_classes)."""
    lines = src.split("\n")
    n = len(src)

    package_line = ""
    imports: List[str] = []
    # Find package and imports (top-of-file, before any class)
    # We scan line by line until we hit the first top-level class keyword.
    pos = 0
    header_end_pos = 0
    for li, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("package "):
            package_line = line
        elif stripped.startswith("import "):
            imports.append(line)
        # detect first top-level class/record/interface/enum keyword at brace depth 0
        # cheap: only check lines whose first non-ws char is not / * or //
        if stripped and not stripped.startswith("//") and not stripped.startswith("/*"):
            # find a class keyword on this line not inside a string
            for m in CLASS_KW_RE.finditer(line):
                # ensure not in a comment/string on the same line (cheap)
                before = line[:m.start()]
                if '//' in before or '"' in before:
                    break
                header_end_pos = sum(len(lines[k]) + 1 for k in range(li))
                break
            else:
                continue
            break
        if stripped.startswith("@") or stripped.startswith("public") or stripped.startswith("class") \
                or stripped.startswith("abstract") or stripped.startswith("final") \
                or stripped.startswith("import") or stripped.startswith("package") \
                or stripped == "" or stripped.startswith("//") or stripped.startswith("/*") \
                or stripped.startswith("*"):
            continue
        # something else -> likely class decl region; stop
        break

    # Compute the position just after the last top-of-file import line, so that
    # the first class's leading_annotations starts after the imports.
    header_end = 0
    if imports:
        last_import = imports[-1]
        # find the last occurrence of this import line in src
        idx = src.rfind(last_import)
        if idx >= 0:
            header_end = idx + len(last_import) + 1  # +1 for the newline
    elif package_line:
        idx = src.find(package_line)
        if idx >= 0:
            header_end = idx + len(package_line) + 1

    # Walk the whole source for top-level class declarations using brace depth 0.
    classes: List[ClassInfo] = []
    i = 0
    depth = 0
    line_no = 1
    prev_end = header_end  # position just after the previous top-level class's closing brace (or imports)
    # We track positions of class keywords at depth 0.
    while i < n:
        c = src[i]
        if c == "\n":
            line_no += 1
            i += 1
            continue
        if c == '"':
            new_i = _scan_strings_comments(src, i, '"')
            line_no += src.count("\n", i, new_i)
            i = new_i
            continue
        if c == "'":
            new_i = _scan_strings_comments(src, i, "'")
            i = new_i
            continue
        if c == "/" and i + 1 < n and src[i+1] == "/":
            while i < n and src[i] != "\n":
                i += 1
            continue
        if c == "/" and i + 1 < n and src[i+1] == "*":
            start_line = line_no
            j = i + 2
            while j + 1 < n and not (src[j] == "*" and src[j+1] == "/"):
                if src[j] == "\n":
                    line_no += 1
                j += 1
            i = j + 2
            continue
        if c == "{":
            depth += 1
            i += 1
            continue
        if c == "}":
            depth -= 1
            i += 1
            continue
        if depth == 0:
            m = CLASS_KW_RE.match(src, i)
            if m and (i == 0 or not src[i-1].isalnum() and src[i-1] not in "_$"):
                # found a top-level class keyword
                kw = m.group(1)
                # find the class name
                rest = src[i:]
                name_m = re.match(r"\b(?:class|interface|enum|record)\s+([A-Za-z_$][A-Za-z0-9_$]*)", rest)
                if name_m:
                    name = name_m.group(1)
                    sig_end = i + name_m.end()
                    # find opening brace
                    ob = sig_end
                    while ob < n and src[ob] != "{":
                        if src[ob] == '"':
                            ob = _scan_strings_comments(src, ob, '"')
                            continue
                        if src[ob] == "'":
                            ob = _scan_strings_comments(src, ob, "'")
                            continue
                        if src[ob] == "/" and ob+1 < n and src[ob+1] == "/":
                            while ob < n and src[ob] != "\n":
                                ob += 1
                            continue
                        if src[ob] == "/" and ob+1 < n and src[ob+1] == "*":
                            ob += 2
                            while ob+1 < n and not (src[ob] == "*" and src[ob+1] == "/"):
                                ob += 1
                            ob += 2
                            continue
                        ob += 1
                    if ob < n and src[ob] == "{":
                        close = _find_matching_brace(src, ob)
                        body_text = src[ob+1:close]
                        sig_text = src[i:ob].strip()
                        # super class
                        sup = ""
                        sup_m = re.search(r"\bextends\s+([A-Za-z_$][A-Za-z0-9_$<>,\s.]*)", sig_text)
                        if sup_m:
                            sup = sup_m.group(1).strip().split("<")[0]
                        # leading annotations / comments: everything between the
                        # previous top-level class's closing brace (or file start)
                        # and this class keyword.
                        lead_start = prev_end
                        leading = src[lead_start:i]
                        sig_line = src.count("\n", 0, i) + 1
                        body_start_line = src.count("\n", 0, ob+1) + 1
                        body_end_line = src.count("\n", 0, close) + 1
                        header_start_line = src.count("\n", 0, lead_start) + 1
                        ci = ClassInfo(name=name, super_class=sup,
                                       leading_annotations=leading,
                                       signature=sig_text,
                                       file_body=body_text,
                                       body_start_line=body_start_line,
                                       body_end_line=body_end_line,
                                       header_start_line=header_start_line,
                                       sig_line=sig_line)
                        classes.append(ci)
                        # advance past the close brace
                        line_no = src.count("\n", 0, close+1) + 1
                        i = close + 1
                        prev_end = i
                        depth = 0
                        continue
        i += 1

    # Now extract methods for each top-level class from its file_body.
    for ci in classes:
        ci.methods = _extract_methods(ci.file_body, ci.body_start_line)
    return package_line, imports, classes


def _find_annotation_start(src: str, class_kw_pos: int) -> int:
    """Scan backwards from the class keyword to find the start of the annotation
    block / header. Stops at the previous '}' or at file start or at a non-ws,
    non-@Annotation, non-comment token."""
    i = class_kw_pos - 1
    # skip trailing ws
    while i >= 0 and src[i] in " \t\r\n":
        i -= 1
    # now walk back over @Anno(...) blocks and comments
    while i >= 0:
        if src[i] == ")":
            # find matching '(' going back (skipping strings)
            depth = 1
            i -= 1
            while i >= 0 and depth > 0:
                c = src[i]
                if c == ")":
                    depth += 1
                elif c == "(":
                    depth -= 1
                elif c == '"':
                    # walk back to opening quote
                    j = i - 1
                    while j >= 0 and src[j] != '"':
                        if src[j] == "\\":
                            # escaped -- but backslash-escape of quote counts; cheaply skip
                            pass
                        j -= 1
                    i = j - 1
                    continue
                i -= 1
            # now i points just before '(' ; before it should be an identifier then @
            while i >= 0 and (src[i].isalnum() or src[i] in "_$."):
                i -= 1
            if i >= 0 and src[i] == "@":
                i -= 1
                while i >= 0 and src[i] in " \t\r\n":
                    i -= 1
                continue
            else:
                i += 1
                break
        elif src[i] == "@":
            # annotation with no parens
            i -= 1
            while i >= 0 and src[i] in " \t\r\n":
                i -= 1
            continue
        elif src[i] in " \t\r\n":
            i -= 1
            continue
        elif src[i] == "/" and i >= 1 and src[i-1] == "*":
            # end of a block comment -- walk back to its start /*
            j = i - 1
            while j >= 1 and not (src[j] == "/" and src[j-1] == "*"):
                j -= 1
            i = j - 2
            continue
        elif src[i] == "\n":
            i -= 1
            continue
        else:
            # some other token (e.g. a '}'); stop
            break
    return i + 1


TEST_ANNO_RE = re.compile(r"@(Test|ParameterizedTest|RepeatedTest|TestFactory)\b")
METHOD_SIG_RE = re.compile(
    r"(?P<mods>(?:public|protected|private|static|final|synchronized|native|abstract|default|<[^>]*>|\s)*)"
    r"(?P<ret>[A-Za-z_$][\w.$]*(?:\s*<[^<>]*>(?:\s*,\s*[A-Za-z_$][\w.$]*\s*<[^<>]*>)*)?)\s+"
    r"(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*\("
)


def _extract_methods(body: str, body_start_line: int) -> List[MethodInfo]:
    """Extract direct member methods (and @Test-annotated ones) from a class body.
    Nested-class contents are skipped via brace-depth tracking."""
    n = len(body)
    methods: List[MethodInfo] = []
    i = 0
    depth = 0
    line_no = body_start_line
    # We scan for method-like declarations at depth 0 within the body.
    # Strategy: find '@Test' annotations and method signatures at brace depth 0.
    # Track brace depth; only consider decls at depth 0.
    while i < n:
        c = body[i]
        if c == "\n":
            line_no += 1
            i += 1
            continue
        if c == '"':
            j = _scan_strings_comments(body, i, '"')
            line_no += body.count("\n", i, j)
            i = j
            continue
        if c == "'":
            j = _scan_strings_comments(body, i, "'")
            i = j
            continue
        if c == "/" and i+1 < n and body[i+1] == "/":
            while i < n and body[i] != "\n":
                i += 1
            continue
        if c == "/" and i+1 < n and body[i+1] == "*":
            j = i + 2
            while j+1 < n and not (body[j] == "*" and body[j+1] == "/"):
                if body[j] == "\n":
                    line_no += 1
                j += 1
            line_no += body.count("\n", i, j+2)
            i = j + 2
            continue
        if c == "{":
            depth += 1
            i += 1
            continue
        if c == "}":
            depth -= 1
            i += 1
            continue
        if depth == 0 and c == "@":
            # potential annotation; capture the annotation block + following method
            m = TEST_ANNO_RE.match(body, i)
            if m:
                # collect this test method
                mi = _capture_method(body, i, line_no, body_start_line)
                if mi:
                    methods.append(mi)
                    # advance past the method
                    new_i = mi.start_line - body_start_line  # not exact; use end pos
                    # we need the end position; recompute by finding the method end
                    # _capture_method returns text; compute end offset
                    end_off = _find_method_end_offset(body, i)
                    line_no += body.count("\n", i, end_off)
                    i = end_off
                    continue
            # skip the annotation token (advance past @Ident(...)
            i = _skip_annotation(body, i)
            continue
        if depth == 0 and (c.isalpha() or c == "_" or c == "$" or c == "<"):
            # could be a non-test method or field; try to capture a method
            mi = _capture_method_if_signature(body, i, line_no, body_start_line)
            if mi:
                methods.append(mi)
                end_off = _find_method_end_offset(body, i)
                line_no += body.count("\n", i, end_off)
                i = end_off
                continue
        i += 1
    return methods


def _skip_annotation(body: str, i: int) -> int:
    """Skip an @Annotation(...) or @Annotation token. Return index past it."""
    n = len(body)
    i += 1  # skip @
    while i < n and (body[i].isalnum() or body[i] in "_$."):
        i += 1
    # optional (...)
    while i < n and body[i] in " \t":
        i += 1
    if i < n and body[i] == "(":
        depth = 1
        i += 1
        while i < n and depth > 0:
            c = body[i]
            if c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
            elif c == '"':
                i = _scan_strings_comments(body, i, '"')
                continue
            elif c == "'":
                i = _scan_strings_comments(body, i, "'")
                continue
            i += 1
    return i


def _capture_method(body: str, anno_start: int, line_no: int, body_start_line: int) -> Optional[MethodInfo]:
    """Capture a method starting at an @Test annotation. Returns the method with
    verbatim text from the annotation through the closing brace."""
    # gather all preceding annotations? We assume only @Test precedes here.
    # find the method signature after the annotation(s)
    j = anno_start
    # skip annotations
    while j < len(body) and body[j] in " \t\r\n@":
        if body[j] == "@":
            j = _skip_annotation(body, j)
        else:
            j += 1
    # now j points at method modifiers / return type
    sig_match = METHOD_SIG_RE.match(body, j)
    if not sig_match:
        return None
    name = sig_match.group("name")
    # find '('
    paren = body.find("(", sig_match.start(), len(body))
    if paren == -1:
        return None
    # match parens
    depth = 1
    k = paren + 1
    while k < len(body) and depth > 0:
        c = body[k]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
        elif c == '"':
            k = _scan_strings_comments(body, k, '"')
            continue
        elif c == "'":
            k = _scan_strings_comments(body, k, "'")
            continue
        k += 1
    # k points just past ')'
    # find '{' (skip throws, etc.)
    while k < len(body) and body[k] != "{" and body[k] != ";":
        k += 1
    if k >= len(body) or body[k] == ";":
        return None
    close = _find_matching_brace(body, k)
    text = body[anno_start:close+1]
    start_line = body_start_line + body.count("\n", 0, anno_start)
    end_line = body_start_line + body.count("\n", 0, close)
    # extract annotation text
    anno_text = body[anno_start:sig_match.start()].rstrip()
    # but sig_match.start() might include leading ws; recompute annotation end
    anno_end = j
    anno_text = body[anno_start:anno_end]
    return MethodInfo(name=name, text=text, start_line=start_line, end_line=end_line,
                      is_test=True, annotations=anno_text)


def _capture_method_if_signature(body: str, i: int, line_no: int, body_start_line: int) -> Optional[MethodInfo]:
    """If position i begins a method signature (not a field, not a class), capture it."""
    m = METHOD_SIG_RE.match(body, i)
    if not m:
        return None
    name = m.group("name")
    # reject keywords that aren't methods
    if name in ("class", "interface", "enum", "record", "new", "return", "if", "for", "while", "switch"):
        return None
    paren = body.find("(", m.start(), len(body))
    if paren == -1:
        return None
    depth = 1
    k = paren + 1
    while k < len(body) and depth > 0:
        c = body[k]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
        elif c == '"':
            k = _scan_strings_comments(body, k, '"')
            continue
        elif c == "'":
            k = _scan_strings_comments(body, k, "'")
            continue
        k += 1
    while k < len(body) and body[k] != "{" and body[k] != ";":
        k += 1
    if k >= len(body) or body[k] == ";":
        return None
    close = _find_matching_brace(body, k)
    text = body[i:close+1]
    start_line = body_start_line + body.count("\n", 0, i)
    end_line = body_start_line + body.count("\n", 0, close)
    return MethodInfo(name=name, text=text, start_line=start_line, end_line=end_line,
                      is_test=False, annotations="")


def _find_method_end_offset(body: str, start: int) -> int:
    """Given a method starting at offset start (at an annotation or signature),
    return the offset just past the method's closing brace."""
    # find '{' then match
    k = start
    n = len(body)
    while k < n and body[k] != "{" and body[k] != ";":
        if body[k] == '"':
            k = _scan_strings_comments(body, k, '"')
            continue
        if body[k] == "'":
            k = _scan_strings_comments(body, k, "'")
            continue
        if body[k] == "/" and k+1 < n and body[k+1] == "/":
            while k < n and body[k] != "\n":
                k += 1
            continue
        if body[k] == "/" and k+1 < n and body[k+1] == "*":
            k += 2
            while k+1 < n and not (body[k] == "*" and body[k+1] == "/"):
                k += 1
            k += 2
            continue
        k += 1
    if k >= n or body[k] == ";":
        return k + 1
    close = _find_matching_brace(body, k)
    return close + 1


if __name__ == "__main__":
    import sys
    path = sys.argv[1]
    with open(path, encoding="utf-8") as f:
        src = f.read()
    pkg, imps, classes = parse_file(src)
    print("package:", pkg.strip())
    print("imports:", len(imps))
    for c in classes:
        print(f"  class {c.name} extends {c.super_class}  L{c.header_start_line}-{c.body_end_line}"
              f"  methods={len(c.methods)}")