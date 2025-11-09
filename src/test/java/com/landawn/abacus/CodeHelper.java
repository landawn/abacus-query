package com.landawn.abacus;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.stream.Stream;

class CodeHelper {

    @Test
    public void test_addParameterCheck() throws IOException {
        File file = new File("src/main/java/com/landawn/abacus/util/SQLBuilder.java");
        List<String> lines = IOUtil.readAllLines(file);

        Stream.of(lines).filter(line -> line.startsWith("        public static SQLBuilder")).forEach(Fn.println());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.startsWith("        public static SQLBuilder")) {
                //        if (lines.get(i + 1).contains("INSERTION_PART")) {
                //            lines.remove(i + 1);
                //        }
                //
                //        if (lines.get(i + 2).contains("INSERTION_PART")) {
                //            lines.remove(i + 2);
                //        }

                if (lines.get(i + 1).startsWith("            return ") || lines.get(i + 2).startsWith("            return ")
                        || lines.get(i + 3).startsWith("            return ")) {
                    continue;
                }

                String parameterName = "";
                if (Strings.containsAnyIgnoreCase(line, "select", "count", "exists")) {
                    parameterName = "SELECTION_PART_MSG";
                } else if (Strings.containsIgnoreCase(line, "insert")) {
                    parameterName = "INSERTION_PART_MSG";
                } else if (Strings.containsIgnoreCase(line, "update")) {
                    parameterName = "UPDATE_PART_MSG";
                } else if (Strings.containsIgnoreCase(line, "delete")) {
                    parameterName = "DELETION_PART_MSG";
                }

                String lineToAdd = null;
                if (Strings.containsAny(line, "String expr")) {
                    lineToAdd = "            N.checkArgNotEmpty(expr, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                } else if (Strings.containsAny(line, "String... propOrColumnNames", "Collection<String> propOrColumnNames")) {
                    lineToAdd = "            N.checkArgNotEmpty(propOrColumnNames, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                } else if (Strings.containsAny(line, "Object entity")) {
                    lineToAdd = "            N.checkArgNotNull(entity, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                } else if (Strings.containsAny(line, "String tableName")) {
                    lineToAdd = "            N.checkArgNotEmpty(tableName, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                } else if (Strings.containsAny(line, "Map<String, Object> props")) {
                    lineToAdd = "            N.checkArgNotEmpty(props, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                } else if (Strings.containsAny(line, "Collection<?> propsList")) {
                    lineToAdd = "            N.checkArgNotEmpty(propsList, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                } else if (Strings.containsAny(line, "Class<?> entityClassA")) {
                    lineToAdd = "            N.checkArgNotNull(entityClassA, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                } else if (Strings.containsAny(line, "Class<?> entityClass")) {
                    lineToAdd = "            N.checkArgNotNull(entityClass, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                } else if (Strings.containsAny(line, "String selectPart")) {
                    lineToAdd = "            N.checkArgNotEmpty(selectPart, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                } else if (Strings.containsAny(line, "Map<String, String> propOrColumnNameAliases")) {
                    lineToAdd = "            N.checkArgNotEmpty(propOrColumnNameAliases, " + parameterName + ");" + IOUtil.LINE_SEPARATOR_UNIX;
                }

                if (Strings.isNotEmpty(parameterName) && Strings.isNotEmpty(lineToAdd)) {
                    if (line.endsWith(" {")) {
                        lines.add(i + 1, lineToAdd);
                    } else if (lines.get(i + 1).endsWith(" {")) {
                        lines.add(i + 2, lineToAdd);
                    } else if (lines.get(i + 2).endsWith(" {")) {
                        lines.add(i + 3, lineToAdd);
                    }
                }
            }
        }

        IOUtil.writeLines(lines, file);
    }

}
