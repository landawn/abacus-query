"""Small, lossless Java lexer used by the test inventory and duplicate checks.

This is deliberately a source audit, not a replacement for JUnit discovery.
Comments, literals and code retain their original offsets and line breaks.
"""
from __future__ import annotations

import re


def scan(src):
    """Yield (kind, text, starting_line), preserving every source character."""
    pos = 0
    line = 1
    code_start = 0
    code_line = 1
    while pos < len(src):
        if src.startswith("//", pos):
            kind = "LINE_COMMENT"
            end = src.find("\n", pos)
            end = len(src) if end < 0 else end
        elif src.startswith("/*", pos):
            kind = "BLOCK_COMMENT"
            end = src.find("*/", pos + 2)
            end = len(src) if end < 0 else end + 2
        elif src[pos] in "\"'":
            quote = src[pos]
            kind = "STRING" if quote == '"' else "CHAR"
            delimiter = '"""' if src.startswith('"""', pos) else quote
            end = pos + len(delimiter)
            while end < len(src):
                if src[end] == "\\":
                    end += 2
                elif src.startswith(delimiter, end):
                    end += len(delimiter)
                    break
                else:
                    end += 1
        else:
            if src[pos] == "\n":
                line += 1
            pos += 1
            continue
        if code_start < pos:
            yield "CODE", src[code_start:pos], code_line
        text = src[pos:end]
        yield kind, text, line
        line += text.count("\n")
        pos = end
        code_start = pos
        code_line = line
    if code_start < len(src):
        yield "CODE", src[code_start:], code_line


def code_mask(src):
    return "".join(text if kind == "CODE" else re.sub(r"[^\r\n]", " ", text)
                   for kind, text, _ in scan(src))


def matching(mask, start, opening="{", closing="}"):
    depth = 0
    for pos in range(start, len(mask)):
        if mask[pos] == opening:
            depth += 1
        elif mask[pos] == closing:
            depth -= 1
            if depth == 0:
                return pos
    raise ValueError(f"Unbalanced {opening!r} at offset {start}")


def normalize_body(sig_body):
    """Compare executable bodies, preserving literal contents and token boundaries."""
    start = code_mask(sig_body).find("{")
    if start < 0:
        raise ValueError("Method has no body")
    return normalize_tokens(sig_body[start:])


def normalize_tokens(src):
    tokens = []
    for kind, text, _ in scan(src):
        if kind == "CODE":
            # Token boundaries distinguish e.g. 'a + +b' from 'a++ + b'.
            tokens.extend(re.findall(r"[\w$]+|>>>?=?|<<=?|::|->|\+\+|--|&&|\|\||==|!=|<=|>=|[+*/%&|^!-]=|[^\s]", text))
        elif kind in ("STRING", "CHAR"):
            tokens.append(text)
    return " ".join(tokens)
