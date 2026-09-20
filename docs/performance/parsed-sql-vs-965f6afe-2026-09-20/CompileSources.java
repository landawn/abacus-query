import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;

/** Reproduces the compiler settings used by the benchmark snapshots without invoking Maven tests. */
public final class CompileSources {
    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]);
        String classpath = Files.readString(Path.of(args[1]), StandardCharsets.UTF_8).trim();
        Path output = Path.of(args[2]);
        Files.createDirectories(output);
        List<Path> sources;
        if (Files.isDirectory(source)) {
            try (var paths = Files.walk(source)) {
                sources = paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
            }
        } else {
            sources = List.of(source);
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        var options = new java.util.ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8", "-classpath", classpath, "-d", output.toString()));
        if (args.length > 3 && args[3].equals("lombok")) {
            options.addAll(List.of("-proc:full", "-processor", "lombok.launch.AnnotationProcessorHider$AnnotationProcessor"));
        } else {
            options.add("-proc:none");
        }
        boolean success = compiler.getTask(null, manager, null, options, null, manager.getJavaFileObjectsFromPaths(sources)).call();
        manager.flush();
        System.exit(success ? 0 : 1);
    }
}
