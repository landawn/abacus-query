#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const mainRoot = path.join(repoRoot, "src", "main", "java");
const testRoot = path.join(repoRoot, "src", "test", "java");

function stripComments(source) {
    return source
        .replace(/\/\*[\s\S]*?\*\//g, " ")
        .replace(/\/\/.*$/gm, " ");
}

function normalizeWhitespace(value) {
    return value.replace(/\s+/g, " ").trim();
}

function splitParameters(paramsText) {
    const text = paramsText.trim();

    if (!text) {
        return [];
    }

    const parts = [];
    let current = "";
    let angleDepth = 0;
    let parenDepth = 0;
    let bracketDepth = 0;

    for (const ch of text) {
        if (ch === "<") {
            angleDepth += 1;
        } else if (ch === ">") {
            angleDepth = Math.max(0, angleDepth - 1);
        } else if (ch === "(") {
            parenDepth += 1;
        } else if (ch === ")") {
            parenDepth = Math.max(0, parenDepth - 1);
        } else if (ch === "[") {
            bracketDepth += 1;
        } else if (ch === "]") {
            bracketDepth = Math.max(0, bracketDepth - 1);
        } else if (ch === "," && angleDepth === 0 && parenDepth === 0 && bracketDepth === 0) {
            parts.push(current.trim());
            current = "";
            continue;
        }

        current += ch;
    }

    if (current.trim()) {
        parts.push(current.trim());
    }

    return parts;
}

function normalizeParameterType(parameter) {
    const cleaned = normalizeWhitespace(parameter)
        .replace(/@\w+(?:\([^)]*\))?\s*/g, "")
        .replace(/\bfinal\s+/g, "")
        .trim();

    const match = cleaned.match(/(.+?)\s+([A-Za-z_$][\w$]*)$/);
    return normalizeWhitespace(match ? match[1] : cleaned).replace(/\s*\.\.\./g, "...");
}

function relativePath(filePath) {
    return path.relative(repoRoot, filePath).replace(/\\/g, "/");
}

function extractPublicMethods(sourceText, sourcePath) {
    const cleaned = stripComments(sourceText);
    const fileClassName = path.basename(sourcePath, ".java");
    const methodPattern =
        /(^|\n)\s*(?:@[\w$.]+(?:\([^)]*\))?\s*)*(public)\s+(?!class\b|interface\b|enum\b|record\b)(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:default\s+)?(?:<[^>{;]+>\s+)?([\w$<>\[\].?,\s]+?)\s+([A-Za-z_$][\w$]*)\s*\(([^)]*)\)\s*(?:throws\s+[^{;]+)?([;{])/gm;

    const methods = [];
    let match;

    while ((match = methodPattern.exec(cleaned)) !== null) {
        const methodName = match[4];
        if (methodName === fileClassName) {
            continue;
        }

        methods.push({
            methodName,
            signature: `${methodName}(${splitParameters(match[5]).map(normalizeParameterType).join(", ")})`
        });
    }

    return methods;
}

function extractTestMethods(testText) {
    const methods = [];
    const annotationPattern = /(^|\n)\s*@Test\b/gm;
    let match;

    while ((match = annotationPattern.exec(testText)) !== null) {
        let searchIndex = annotationPattern.lastIndex;

        while (true) {
            const nextLineBreak = testText.indexOf("\n", searchIndex);
            if (nextLineBreak < 0) {
                break;
            }

            const line = testText.slice(searchIndex, nextLineBreak).trim();
            if (line.startsWith("@")) {
                searchIndex = nextLineBreak + 1;
                continue;
            }

            break;
        }

        const sourceAfter = testText.slice(searchIndex);
        const headerMatch =
            /\b(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:<[^>]+>\s+)?[\w$<>\[\], ?]+\s+([A-Za-z_$][\w$]*)\s*\(/m.exec(
                sourceAfter
            );

        if (!headerMatch) {
            continue;
        }

        methods.push(headerMatch[1]);
    }

    return methods;
}

const requested = process.argv[2];

if (!requested) {
    console.error("Usage: node tools/report-test-method-order.mjs <relative-source-path>");
    process.exit(1);
}

const sourcePath = path.resolve(repoRoot, requested);
const relative = path.relative(mainRoot, sourcePath);
const testPath = path.join(testRoot, relative.replace(/\.java$/, "Test.java"));

if (!fs.existsSync(sourcePath)) {
    console.error(`Source file not found: ${relativePath(sourcePath)}`);
    process.exit(1);
}

if (!fs.existsSync(testPath)) {
    console.error(`Test file not found: ${relativePath(testPath)}`);
    process.exit(1);
}

const sourceMethods = extractPublicMethods(fs.readFileSync(sourcePath, "utf8"), sourcePath);
const testMethods = extractTestMethods(fs.readFileSync(testPath, "utf8"));

console.log(JSON.stringify({
    sourceFile: relativePath(sourcePath),
    testFile: relativePath(testPath),
    sourceMethodOrder: sourceMethods,
    testMethods
}, null, 2));
