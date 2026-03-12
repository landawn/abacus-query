#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const testRoot = path.join(repoRoot, "src", "test", "java", "com", "landawn", "abacus");

const OWNER_OVERRIDES = new Map([
    ["DynamicQueryBuilder2025Test", "DynamicQuery"],
    ["SimpleDynamicQueryBuilderTest", "DynamicQuery"],
    ["AbstractQueryBuilder2025Test", "AbstractQueryBuilder"],
    ["SimpleAbstractQueryBuilderTest", "AbstractQueryBuilder"],
    ["SqlBuilder10Test", "SqlBuilder"],
    ["SqlBuilder11Test", "SqlBuilder"],
    ["SqlBuilder12Test", "SqlBuilder"],
    ["SqlBuilder13Test", "SqlBuilder"],
    ["SqlBuilder14Test", "SqlBuilder"],
    ["SqlBuilder2025Test", "SqlBuilder"],
    ["SKTest", "SK"],
    ["SK2025Test", "SK"],
    ["SimpleNotExistsTest", "NotExists"]
]);

const PRIMARY_PREFERENCES = new Map([
    ["DynamicQuery", "DynamicQueryBuilder2025Test"],
    ["AbstractQueryBuilder", "AbstractQueryBuilder2025Test"],
    ["SqlBuilder", "SqlBuilder2025Test"],
    ["NotExists", "NotExists2025Test"]
]);

const PREFIX_TO_OWNER = new Map([
    ["ParsedSql", "ParsedSql"],
    ["QueryUtil", "QueryUtil"],
    ["SortDirection", "SortDirection"],
    ["SqlMapper", "SqlMapper"],
    ["SqlParser", "SqlParser"],
    ["SqlOperation", "SqlOperation"],
    ["SK", "SK"],
    ["Selection", "Selection"],
    ["Filters", "Filters"],
    ["DynamicQueryBuilder", "DynamicQuery"],
    ["SqlBuilder", "SqlBuilder"]
]);

const EXTRACTION_RULES = [
    {
        type: "whole-class-method-prefix",
        source: "src/test/java/com/landawn/abacus/query/JavadocExamplesQueryTest.java",
        generatedSuffix: "JavadocExamples"
    },
    {
        type: "named-methods",
        source: "src/test/java/com/landawn/abacus/query/FiltersTest.java",
        generatedSuffix: "FromFiltersTest",
        methods: [{ name: "testCriteria", owner: "Criteria" }]
    },
    {
        type: "named-methods",
        source: "src/test/java/com/landawn/abacus/query/Filters2025Test.java",
        generatedSuffix: "FromFilters2025Test",
        methods: [{ name: "testPatternForAlphanumericColumnName", owner: "QueryUtil" }]
    }
];

function toPosix(filePath) {
    return filePath.replace(/\\/g, "/");
}

function read(filePath) {
    return fs.readFileSync(filePath, "utf8");
}

function write(filePath, content) {
    fs.writeFileSync(filePath, content, "utf8");
}

function exists(filePath) {
    return fs.existsSync(filePath);
}

function walk(dir) {
    const results = [];
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            results.push(...walk(fullPath));
        } else if (entry.isFile() && entry.name.endsWith("Test.java")) {
            const relative = toPosix(path.relative(repoRoot, fullPath));
            if (relative.startsWith("src/test/java/com/landawn/abacus/") && !relative.startsWith("src/test/java/com/landawn/abacus/entity/")) {
                results.push(fullPath);
            }
        }
    }
    return results;
}

function getHeader(source) {
    const packageIndex = source.search(/^\s*package\s+/m);
    return packageIndex > 0 ? source.slice(0, packageIndex).trimEnd() : "";
}

function getPackageLine(source) {
    const match = source.match(/^\s*package\s+[^;]+;/m);
    if (!match) {
        throw new Error("Missing package declaration");
    }
    return match[0].trim();
}

function getImports(source) {
    return [...source.matchAll(/^\s*import\s+[^;]+;/gm)].map(match => match[0].trim());
}

function getFirstClassName(source) {
    const sanitized = stripComments(source);
    const match = sanitized.match(/\bclass\s+([A-Za-z_$][\w$]*)\b/);
    if (!match) {
        throw new Error("Missing class declaration");
    }
    return match[1];
}

function getClassBlock(source) {
    const packageMatch = source.match(/^\s*package\s+[^;]+;\s*/m);
    const lastImportMatch = [...source.matchAll(/^\s*import\s+[^;]+;\s*$/gm)].pop();
    let startIndex = 0;

    if (lastImportMatch) {
        startIndex = lastImportMatch.index + lastImportMatch[0].length;
    } else if (packageMatch) {
        startIndex = packageMatch.index + packageMatch[0].length;
    }

    return source.slice(startIndex).trim();
}

function ownerOf(className) {
    if (OWNER_OVERRIDES.has(className)) {
        return OWNER_OVERRIDES.get(className);
    }

    return className.replace(/2025Test$/, "").replace(/Test$/, "");
}

function canonicalTestClass(owner) {
    return `${owner}Test`;
}

function canonicalTestFile(filePath, owner) {
    return path.join(path.dirname(filePath), `${canonicalTestClass(owner)}.java`);
}

function stripComments(source) {
    return source
        .replace(/\/\*[\s\S]*?\*\//g, match => match.replace(/[^\n]/g, " "))
        .replace(/\/\/[^\n\r]*/g, match => " ".repeat(match.length));
}

function findMatchingBrace(source, openBraceIndex) {
    let depth = 0;
    let inString = false;
    let inChar = false;
    let inLineComment = false;
    let inBlockComment = false;
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

    throw new Error("Unbalanced braces");
}

function extractTestMethods(source) {
    const methods = [];
    const annotationPattern = /(^|\n)\s*@Test\b/gm;
    let match;

    while ((match = annotationPattern.exec(source)) !== null) {
        const annotationStart = match.index + match[1].length;
        let searchIndex = annotationPattern.lastIndex;

        while (true) {
            const nextBreak = source.indexOf("\n", searchIndex);
            if (nextBreak < 0) {
                break;
            }

            const line = source.slice(searchIndex, nextBreak).trim();
            if (line.startsWith("@")) {
                searchIndex = nextBreak + 1;
                continue;
            }
            break;
        }

        const methodSource = source.slice(searchIndex);
        const headerMatch = /\b(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?(?:<[^>]+>\s+)?[\w$<>\[\], ?]+\s+([A-Za-z_$][\w$]*)\s*\(/m.exec(
            methodSource
        );

        if (!headerMatch) {
            continue;
        }

        const headerStart = searchIndex + headerMatch.index;
        const openBraceIndex = source.indexOf("{", headerStart + headerMatch[0].length);
        const closeBraceIndex = findMatchingBrace(source, openBraceIndex);

        let endIndex = closeBraceIndex + 1;
        while (endIndex < source.length && /\s/.test(source[endIndex])) {
            endIndex += 1;
        }

        methods.push({
            name: headerMatch[1],
            start: annotationStart,
            end: endIndex,
            text: source.slice(annotationStart, endIndex).trimEnd()
        });

        annotationPattern.lastIndex = closeBraceIndex + 1;
    }

    return methods;
}

function removeNamedMethods(source, methodNames) {
    const methods = extractTestMethods(source);
    const removalSpans = methods.filter(method => methodNames.includes(method.name)).sort((a, b) => b.start - a.start);
    let updated = source;

    for (const method of removalSpans) {
        updated = updated.slice(0, method.start) + updated.slice(method.end);
    }

    return updated.replace(/\n{3,}/g, "\n\n");
}

function mapJavadocOwner(methodName) {
    const raw = methodName.replace(/^test/, "");
    const prefix = [...PREFIX_TO_OWNER.keys()].find(candidate => raw.startsWith(candidate));
    if (!prefix) {
        throw new Error(`Unable to map Javadoc method owner for ${methodName}`);
    }
    return PREFIX_TO_OWNER.get(prefix);
}

function buildGeneratedClass(owner, suffix, methods, comment) {
    const lines = [];
    if (comment) {
        lines.push("/**");
        lines.push(` * ${comment}`);
        lines.push(" */");
    }
    lines.push(`class ${owner}${suffix} extends TestBase {`);
    lines.push("");
    methods.forEach((method, index) => {
        lines.push(indentBlock(method.text));
        if (index < methods.length - 1) {
            lines.push("");
        }
    });
    lines.push("}");
    return lines.join("\n");
}

function indentBlock(block) {
    return block
        .split("\n")
        .map(line => (line.length === 0 ? "" : `    ${line}`))
        .join("\n");
}

function choosePrimary(group) {
    const owner = group.owner;
    const preferredName = PRIMARY_PREFERENCES.get(owner);
    if (preferredName) {
        const preferred = group.files.find(entry => entry.className === preferredName);
        if (preferred) {
            return preferred;
        }
    }

    const canonicalName = canonicalTestClass(owner);
    const canonical = group.files.find(entry => entry.className === canonicalName);
    if (canonical) {
        return canonical;
    }

    const regular = group.files.find(entry => entry.className.endsWith("Test") && !entry.className.endsWith("2025Test"));
    if (regular) {
        return regular;
    }

    return group.files[0];
}

function normalizePrimaryClassBlock(classBlock, oldName, newName) {
    if (oldName === newName) {
        return classBlock;
    }

    return classBlock.replace(new RegExp(`\\bpublic\\s+class\\s+${oldName}\\b`), `public class ${newName}`);
}

function demotePublicClass(classBlock) {
    return classBlock.replace(/\bpublic\s+class\b/, "class");
}

function ensureImports(imports, additionalImports) {
    const merged = [...new Set([...imports, ...additionalImports, "import com.landawn.abacus.TestBase;"])];
    const normalizedStaticImports = new Map();
    const regularImports = [];

    for (const entry of merged) {
        const staticMatch = entry.match(/^import\s+static\s+([^;]+)\.([^.]+);$/);
        if (!staticMatch) {
            regularImports.push(entry);
            continue;
        }

        const [, owner, member] = staticMatch;
        const existingOwner = normalizedStaticImports.get(member);
        if (!existingOwner || owner === "org.junit.jupiter.api.Assertions") {
            normalizedStaticImports.set(member, owner);
        }
    }

    const staticImports = [...normalizedStaticImports.entries()]
        .sort((a, b) => a[0].localeCompare(b[0]))
        .map(([member, owner]) => `import static ${owner}.${member};`);

    return [...new Set([...regularImports, ...staticImports])].sort();
}

function writeCanonicalGroup(group, generatedExtraClasses) {
    const primary = choosePrimary(group);
    const canonicalName = canonicalTestClass(group.owner);
    const canonicalFile = canonicalTestFile(primary.filePath, group.owner);
    const header = getHeader(primary.source);
    const packageLine = group.packageLine;
    const importSet = ensureImports(group.imports, generatedExtraClasses.flatMap(extra => extra.imports));
    const classBlocks = [];

    classBlocks.push(normalizePrimaryClassBlock(primary.classBlock, primary.className, canonicalName));

    for (const entry of group.files) {
        if (entry === primary) {
            continue;
        }

        classBlocks.push(demotePublicClass(entry.classBlock));
    }

    classBlocks.push(...generatedExtraClasses.map(extra => extra.classBlock));

    const sections = [];
    if (header) {
        sections.push(header);
    }
    sections.push(packageLine);
    if (importSet.length > 0) {
        sections.push(importSet.join("\n"));
    }
    sections.push(classBlocks.join("\n\n"));

    write(canonicalFile, `${sections.join("\n\n").trim()}\n`);

    for (const entry of group.files) {
        if (toPosix(entry.filePath) !== toPosix(canonicalFile)) {
            fs.unlinkSync(entry.filePath);
        }
    }

    return canonicalFile;
}

function loadFileInfo(filePath) {
    const source = read(filePath);
    return {
        filePath,
        relativePath: toPosix(path.relative(repoRoot, filePath)),
        source,
        packageLine: getPackageLine(source),
        imports: getImports(source),
        className: getFirstClassName(source),
        classBlock: getClassBlock(source)
    };
}

function main() {
    const allFiles = walk(path.join(testRoot, "query"))
        .concat(walk(path.join(testRoot, "query", "condition")))
        .filter((value, index, array) => array.indexOf(value) === index);

    const extractedByOwner = new Map();
    const mutatedSources = new Map();
    const deletedSources = new Set();

    for (const rule of EXTRACTION_RULES) {
        const filePath = path.join(repoRoot, rule.source);
        if (!exists(filePath)) {
            continue;
        }

        const original = read(filePath);
        const methods = extractTestMethods(original);
        const imports = getImports(original);
        const packageLine = getPackageLine(original);
        const className = getFirstClassName(original);

        if (rule.type === "whole-class-method-prefix") {
            const grouped = new Map();

            for (const method of methods) {
                const owner = mapJavadocOwner(method.name);
                if (!grouped.has(owner)) {
                    grouped.set(owner, []);
                }
                grouped.get(owner).push(method);
            }

            for (const [owner, ownerMethods] of grouped) {
                if (!extractedByOwner.has(owner)) {
                    extractedByOwner.set(owner, []);
                }

                const comment =
                    owner === "DynamicQuery"
                        ? "Javadoc usage examples for the DynamicQuery builder live here because DynamicQuery exposes the builder entry point."
                        : null;

                extractedByOwner.get(owner).push({
                    imports,
                    packageLine,
                    classBlock: buildGeneratedClass(owner, rule.generatedSuffix, ownerMethods, comment)
                });
            }

            deletedSources.add(filePath);
            continue;
        }

        if (rule.type === "named-methods") {
            const names = rule.methods.map(item => item.name);
            const methodMap = new Map(methods.map(method => [method.name, method]));

            for (const item of rule.methods) {
                const method = methodMap.get(item.name);
                if (!method) {
                    throw new Error(`Missing method ${item.name} in ${rule.source}`);
                }

                if (!extractedByOwner.has(item.owner)) {
                    extractedByOwner.set(item.owner, []);
                }

                extractedByOwner.get(item.owner).push({
                    imports,
                    packageLine,
                    classBlock: buildGeneratedClass(item.owner, rule.generatedSuffix, [method], null)
                });
            }

            mutatedSources.set(filePath, removeNamedMethods(original, names));
        }
    }

    const groupedByOwner = new Map();

    for (const filePath of allFiles) {
        if (deletedSources.has(filePath)) {
            continue;
        }

        const source = mutatedSources.get(filePath) ?? read(filePath);
        const info = {
            filePath,
            relativePath: toPosix(path.relative(repoRoot, filePath)),
            source,
            packageLine: getPackageLine(source),
            imports: getImports(source),
            className: getFirstClassName(source),
            classBlock: getClassBlock(source)
        };

        const owner = ownerOf(info.className);
        if (!groupedByOwner.has(owner)) {
            groupedByOwner.set(owner, {
                owner,
                packageLine: info.packageLine,
                files: [],
                imports: []
            });
        }

        const group = groupedByOwner.get(owner);
        group.files.push(info);
        group.imports.push(...info.imports);
    }

    const canonicalOutputs = new Map();

    for (const group of [...groupedByOwner.values()].sort((a, b) => a.owner.localeCompare(b.owner))) {
        const extras = extractedByOwner.get(group.owner) ?? [];
        const canonicalFile = writeCanonicalGroup(group, extras);
        canonicalOutputs.set(group.owner, canonicalFile);
    }

    for (const [owner, extras] of extractedByOwner.entries()) {
        if (canonicalOutputs.has(owner)) {
            continue;
        }

        const packageLine = extras[0].packageLine;
        const packageName = packageLine.replace(/^package\s+/, "").replace(/;$/, "");
        const packageDir = path.join(repoRoot, "src", "test", "java", ...packageName.split("."));
        const canonicalFile = path.join(packageDir, `${canonicalTestClass(owner)}.java`);
        const importSet = ensureImports([], extras.flatMap(extra => extra.imports));
        const content = [
            packageLine,
            importSet.join("\n"),
            `public class ${canonicalTestClass(owner)} extends TestBase {`,
            "}",
            extras.map(extra => extra.classBlock).join("\n\n")
        ]
            .filter(Boolean)
            .join("\n\n");

        write(canonicalFile, `${content.trim()}\n`);
        canonicalOutputs.set(owner, canonicalFile);
    }

    for (const filePath of deletedSources) {
        if (exists(filePath)) {
            fs.unlinkSync(filePath);
        }
    }

    for (const [filePath, updatedSource] of mutatedSources.entries()) {
        if (!deletedSources.has(filePath) && exists(filePath) && !canonicalOutputs.has(ownerOf(getFirstClassName(updatedSource)))) {
            write(filePath, `${updatedSource.trim()}\n`);
        }
    }
}

main();
