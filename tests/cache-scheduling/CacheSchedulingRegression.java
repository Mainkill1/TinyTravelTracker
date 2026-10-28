import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Runs the exact production scheduling/lifecycle methods with storage and UI doubles. */
public final class CacheSchedulingRegression {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path sourcePath = Paths.get(args[0]);
        String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
        StringBuilder methods = new StringBuilder();
        int methodCount = 0;
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, null,
                    Arrays.asList("-proc:none"), null,
                    files.getJavaFileObjects(sourcePath.toFile()));
            CompilationUnitTree unit = task.parse().iterator().next();
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            for (Tree declaration : unit.getTypeDecls()) {
                if (!(declaration instanceof ClassTree)) continue;
                for (Tree member : ((ClassTree) declaration).getMembers()) {
                    if (!(member instanceof MethodTree)) continue;
                    String name = ((MethodTree) member).getName().toString();
                    if (!name.equals("run") && !name.equals("shutdown")
                            && !name.equals("notifyMediaDirty")) continue;
                    methods.append(source, (int) positions.getStartPosition(unit, member),
                            (int) positions.getEndPosition(unit, member)).append('\n');
                    methodCount++;
                }
            }
        }
        if (methodCount != 3) throw new AssertionError("Expected three production methods");
        String fixture = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);
        Path generated = Paths.get(args[2], "CacheWorker.java");
        Files.write(generated, fixture.replace("/* PRODUCTION_METHODS */", methods.toString())
                .getBytes(StandardCharsets.UTF_8));
        if (compiler.run(null, null, null, "-d", args[2], generated.toString()) != 0)
            throw new AssertionError("Could not compile production cache methods");
        Class.forName("CacheWorker").getMethod("main", String[].class)
                .invoke(null, (Object) new String[0]);
    }
}
