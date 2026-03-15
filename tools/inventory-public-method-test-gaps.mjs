#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const mainRoot = path.join(repoRoot, "src", "main", "java");
const testRoot = path.join(repoRoot, "src", "test", "java");

function walk(dir) {
    const results = [];

    if (!fs.existsSync(dir)) {
        return results;
    }

    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const fullPath = path.join(dir, entry.name);

        if (entry.isDirectory()) {
            results.push(...walk(fullPath));
        } else if (entry.isFile() && entry.name.endsWith(".java")) {
            results.push(fullPath);
        }
    }

    return results;
}

function stripComments(source) {
    let result = "";
    let i = 0;
    let inLineComment = false;
    let inBlockComment = false;
    let inString = false;
    let inChar = false;
    let escaped = false;

    while (i < source.length) {
        const ch = source[i];
        const next = source[i + 1];

        if (inLineComment) {
            if (ch === "\n") {
                inLineComment = false;
                result += ch;
            }
            i += 1;
            continue;
        }

        if (inBlockComment) {
            if (ch === "*" && next === "/") {
                inBlockComment = false;
                i += 2;
            } else {
                i += 1;
            }
            continue;
        }

        if (inString) {
            result += ch;
            if (!escaped && ch === "\"") {
                inString = false;
            }
            escaped = !escaped && ch === "\\";
            i += 1;
            continue;
        }

        if (inChar) {
            result += ch;
            if (!escaped && ch === "'") {
                inChar = false;
            }
            escaped = !escaped && ch === "\\";
            i += 1;
            continue;
        }

        if (ch === "/" && next === "/") {
            inLineComment = true;
            i += 2;
            continue;
        }

        if (ch === "/" && next === "*") {
            inBlockComment = true;
            i += 2;
            continue;
        }

        if (ch === "\"") {
            inString = true;
            escaped = false;
            result += ch;
            i += 1;
            continue;
        }

        if (ch === "'") {
            inChar = true;
            escaped = false;
            result += ch;
            i += 1;
            continue;
        }

        result += ch;
        i += 1;
    }

    return result;
}

function findMatchingBrace(source, openBraceIndex) {
    let depth = 0;
    let inLineComment = false;
    let inBlockComment = false;
    let inString = false;
    let inChar = false;
    let escaped = false;

    for (let i = openBraceIndex; i < source.length; i += 1) {
        const ch = source[i];
        const next = source[i + 1];

        if (inLineComment) {
            if (ch === "\n") {
                inLineComment = false;
            }
            continue;
        }

        if (inBlockComment) {
            if (ch === "*" && next === "/") {
                inBlockComment = false;
                i += 1;
            }
            continue;
        }

        if (inString) {
            if (!escaped && ch === "\"") {
                inString = false;
            }
            escaped = !escaped && ch === "\\";
            continue;
        }

        if (inChar) {
            if (!escaped && ch === "'") {
                inChar = false;
            }
            escaped = !escaped && ch === "\\";
            continue;
        }

        if (ch === "/" && next === "/") {
            inLineComment = true;
            i += 1;
            continue;
        }

        if (ch === "/" && next === "*") {
            inBlockComment = true;
            i += 1;
            continue;
        }

        if (ch === "\"") {
            inString = true;
            escaped = false;
            continue;
        }

        if (ch === "'") {
            inChar = true;
            escaped = false;
            continue;
        }

        if (ch === "{") {
            depth += 1;
        } else if (ch === "}") {
            depth -= 1;

            if (depth === 0) {
                return i;
            }
        }
    }

    return -1;
}

function lineNumberAt(source, index) {
    let line = 1;

    for (let i = 0; i < index; i += 1) {
        if (source[i] === "\n") {
            line += 1;
        }
    }

    return line;
}

function relativePath(filePath) {
    return path.relative(repoRoot, filePath).replace(/\\/g, "/");
}

function expectedTestPath(sourcePath) {
    const relative = path.relative(mainRoot, sourcePath);
    return path.join(testRoot, relative.replace(/\.java$/, "Test.java"));
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
        .replace(/\bvolatile\s+/g, "")
        .replace(/\btransient\s+/g, "")
        .trim();

    const match = cleaned.match(/(.+?)\s+([A-Za-z_$][\w$]*)$/);

    return normalizeWhitespace(match ? match[1] : cleaned).replace(/\s*\.\.\./g, "...");
}

function extractPackageName(source) {
    const match = source.match(/^\s*package\s+([^;]+);/m);
    return match ? match[1].trim() : "";
}

function extractPrimaryClassName(source, filePath) {
    const cleaned = stripComments(source);
    const match = cleaned.match(/\b(?:public\s+)?(?:abstract\s+)?(?:final\s+)?(?:class|enum|record|interface)\s+([A-Za-z_$][\w$]*)\b/);

    return match ? match[1] : path.basename(filePath, ".java");
}

function isSimpleGetter(methodName, parameters, returnType, body) {
    if (parameters.length !== 0 || returnType === "void" || !/^get[A-Z]/.test(methodName)) {
        return false;
    }

    const compactBody = body.replace(/\s+/g, " ").trim();
    return /^return\s+(?:this\.)?[A-Za-z_$][\w$]*\s*;\s*$/.test(compactBody);
}

function isSimpleBooleanGetter(methodName, parameters, returnType, body) {
    if (parameters.length !== 0 || !/^is[A-Z]/.test(methodName)) {
        return false;
    }

    const compactBody = body.replace(/\s+/g, " ").trim();
    return /^return\s+(?:this\.)?[A-Za-z_$][\w$]*\s*;\s*$/.test(compactBody) && /^(boolean|Boolean)$/.test(returnType);
}

function isSimpleSetter(methodName, parameters, returnType, body, className) {
    if (parameters.length !== 1 || !/^set[A-Z]/.test(methodName)) {
        return false;
    }

    const compactBody = body.replace(/\s+/g, " ").trim();

    if (/^(?:this\.)?[A-Za-z_$][\w$]*\s*=\s*[A-Za-z_$][\w$]*\s*;\s*$/.test(compactBody) && returnType === "void") {
        return true;
    }

    return (
        /^(?:this\.)?[A-Za-z_$][\w$]*\s*=\s*[A-Za-z_$][\w$]*\s*;\s*return\s+this\s*;\s*$/.test(compactBody)
        && returnType.endsWith(className)
    );
}

function isSimpleObjectMethod(methodName, parameters, body) {
    if (!["toString", "hashCode", "equals"].includes(methodName)) {
        return false;
    }

    if (methodName === "equals" && parameters.length !== 1) {
        return false;
    }

    const compactBody = body.replace(/\s+/g, " ").trim();

    if (compactBody.includes("super.")) {
        return true;
    }

    return compactBody.length < 80;
}

function extractPublicMethods(source, filePath) {
    const methods = [];
    const cleaned = stripComments(source);
    const className = extractPrimaryClassName(source, filePath);
    const methodPattern =
        /(^|\n)\s*(?:@[\w$.]+(?:\([^)]*\))?\s*)*(public)\s+(?!class\b|interface\b|enum\b|record\b)(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:default\s+)?(?:<[^>{;]+>\s+)?([\w$<>\[\].?,\s]+?)\s+([A-Za-z_$][\w$]*)\s*\(([^)]*)\)\s*(?:throws\s+[^{;]+)?([;{])/gm;

    let match;

    while ((match = methodPattern.exec(cleaned)) !== null) {
        const returnType = normalizeWhitespace(match[3]);
        const methodName = match[4];
        const parameters = splitParameters(match[5]).map(normalizeParameterType);
        const signature = `${methodName}(${parameters.join(", ")})`;
        const terminator = match[6];
        const line = lineNumberAt(source, match.index + match[1].length);
        let body = "";

        if (terminator === "{") {
            const openBraceIndex = cleaned.indexOf("{", match.index + match[0].length - 1);
            const closeBraceIndex = findMatchingBrace(cleaned, openBraceIndex);
            body = closeBraceIndex > openBraceIndex ? source.slice(openBraceIndex + 1, closeBraceIndex) : "";
        }

        if (methodName === className) {
            continue;
        }

        if (isSimpleGetter(methodName, parameters, returnType, body) || isSimpleBooleanGetter(methodName, parameters, returnType, body)) {
            continue;
        }

        if (isSimpleSetter(methodName, parameters, returnType, body, className)) {
            continue;
        }

        if (isSimpleObjectMethod(methodName, parameters, body)) {
            continue;
        }

        methods.push({
            className,
            methodName,
            returnType,
            parameters,
            signature,
            arity: parameters.length,
            isAbstract: terminator === ";",
            line
        });
    }

    return methods;
}

function extractTestMethods(testFilePath) {
    if (!fs.existsSync(testFilePath)) {
        return { source: "", cleanedSource: "", methods: [] };
    }

    const source = fs.readFileSync(testFilePath, "utf8");
    const methods = [];
    const cleanedSource = stripComments(source);
    const annotationPattern = /(^|\n)\s*@Test\b/gm;
    let match;

    while ((match = annotationPattern.exec(source)) !== null) {
        let searchIndex = annotationPattern.lastIndex;

        while (true) {
            const nextLineBreak = source.indexOf("\n", searchIndex);

            if (nextLineBreak < 0) {
                break;
            }

            const line = source.slice(searchIndex, nextLineBreak).trim();

            if (line.startsWith("@")) {
                searchIndex = nextLineBreak + 1;
                continue;
            }

            break;
        }

        const sourceAfter = source.slice(searchIndex);
        const headerMatch =
            /\b(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:<[^>]+>\s+)?[\w$<>\[\], ?]+\s+([A-Za-z_$][\w$]*)\s*\(/m.exec(
                sourceAfter
            );

        if (!headerMatch) {
            continue;
        }

        const methodHeaderIndex = searchIndex + headerMatch.index;
        const braceIndex = source.indexOf("{", methodHeaderIndex);
        const closeBraceIndex = braceIndex >= 0 ? findMatchingBrace(source, braceIndex) : -1;
        const body = braceIndex >= 0 && closeBraceIndex > braceIndex ? source.slice(braceIndex + 1, closeBraceIndex) : "";

        methods.push({
            methodName: headerMatch[1],
            line: lineNumberAt(source, methodHeaderIndex),
            body,
            cleanedBody: stripComments(body)
        });
    }

    return { source, cleanedSource, methods };
}

function extractCoverageHints(testMethods) {
    const coverage = new Map();

    for (const testMethod of testMethods) {
        const rawName = testMethod.methodName.replace(/^test/, "");
        const primary = rawName.split("_")[0];

        if (!primary) {
            continue;
        }

        const decapitalized = primary.charAt(0).toLowerCase() + primary.slice(1);

        if (!coverage.has(decapitalized)) {
            coverage.set(decapitalized, []);
        }

        coverage.get(decapitalized).push(testMethod.methodName);
    }

    return coverage;
}

function containsMethodInvocation(cleanedTestSource, methodName) {
    const invocationPattern = new RegExp(`\\b${methodName}\\s*\\(`);
    return invocationPattern.test(cleanedTestSource);
}

function isCovered(method, coverageHints, testMethods) {
    const direct = coverageHints.get(method.methodName) ?? [];

    if (direct.length > 0) {
        return true;
    }

    return testMethods.some(testMethod => containsMethodInvocation(testMethod.cleanedBody, method.methodName));
}

const sourceFiles = walk(mainRoot)
    .filter(file => !file.endsWith("package-info.java"))
    .filter(file => relativePath(file).startsWith("src/main/java/com/landawn/abacus/"));

const report = [];

for (const sourceFile of sourceFiles) {
    const source = fs.readFileSync(sourceFile, "utf8");
    const sourceClassName = extractPrimaryClassName(source, sourceFile);
    const sourcePackageName = extractPackageName(source);
    const testFile = expectedTestPath(sourceFile);
    const publicMethods = extractPublicMethods(source, sourceFile);
    const testInfo = extractTestMethods(testFile);
    const coverageHints = extractCoverageHints(testInfo.methods);
    const gaps = publicMethods.filter(method => !isCovered(method, coverageHints, testInfo.methods));

    if (gaps.length === 0 && fs.existsSync(testFile)) {
        continue;
    }

    report.push({
        sourceClassName,
        sourcePackageName,
        sourceFile: relativePath(sourceFile),
        testFile: relativePath(testFile),
        testExists: fs.existsSync(testFile),
        publicMethodCount: publicMethods.length,
        testMethodCount: testInfo.methods.length,
        unmatchedMethods: gaps.map(method => ({
            line: method.line,
            signature: method.signature,
            returnType: method.returnType,
            isAbstract: method.isAbstract
        }))
    });
}

console.log(JSON.stringify(report, null, 2));
