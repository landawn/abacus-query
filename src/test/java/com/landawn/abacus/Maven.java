package com.landawn.abacus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.stream.Stream.StreamEx;

public class Maven {

    public static void main(final String[] args) throws Exception {
        N.println(new File(".").getAbsolutePath());

        final String sourceVersion = "0.0.1-SNAPSHOT";
        final String targetVersion = StreamEx.ofLines(new File("./pom.xml"))
                .filter(line -> line.indexOf("<version>") > 0 && line.indexOf("</version>") > 0)
                .first()
                .map(line -> Strings.substringsBetween(line, "<version>", "</version>").get(0))
                .get();

        final String commonMavenPath = "./maven/";
        final String sourcePath = commonMavenPath + sourceVersion;
        final String targetPath = commonMavenPath + targetVersion;
        final File sourceDir = new File(sourcePath);
        final File targetDir = new File(targetPath);

        IOUtil.deleteAllIfExists(targetDir);

        targetDir.mkdir();

        IOUtil.copyToDirectory(sourceDir, targetDir);

        StreamEx.listFiles(new File("./target/"))
                .filter(f -> f.getName().startsWith("abacus-query") && f.getName().endsWith(".jar"))
                .peek(f -> N.println(f.getName()))
                .forEach(f -> IOUtil.copyToDirectory(f, targetDir));

        StreamEx.listFiles(targetDir) //
                .forEach(file -> IOUtil.renameTo(file, file.getName().replace(sourceVersion, targetVersion)));

        StreamEx.listFiles(targetDir)
                .filter(file -> file.getName().endsWith(".pom") || file.getName().endsWith(".xml") || file.getName().endsWith(".txt"))
                .forEach(file -> {
                    final List<String> lines = IOUtil.readAllLines(file);
                    final List<String> newLines = new ArrayList<>(lines.size());
                    for (final String line : lines) {
                        newLines.add(line.replaceAll(sourceVersion, targetVersion));
                    }
                    IOUtil.writeLines(newLines, file);
                });

        System.exit(0);
    }

}
