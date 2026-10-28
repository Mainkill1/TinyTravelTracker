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
import java.util.Collections;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Inserts the exact production discard method into the storage collaborator. */
public final class ExtractCacheDiscard {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path sourcePath = Paths.get(args[0]);
        String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
        String discard = null;
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, null,
                    Collections.singletonList("-proc:none"), null,
                    files.getJavaFileObjects(sourcePath.toFile()));
            CompilationUnitTree unit = task.parse().iterator().next();
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            for (Tree declaration : unit.getTypeDecls()) {
                if (!(declaration instanceof ClassTree)
                        || !((ClassTree) declaration).getSimpleName()
                                .contentEquals("GpsTrailerCacheCreator")) continue;
                for (Tree member : ((ClassTree) declaration).getMembers()) {
                    if (!(member instanceof MethodTree)
                            || !((MethodTree) member).getName()
                                    .contentEquals("discardViewNodes")) continue;
                    if (discard != null) throw new AssertionError("Ambiguous production discard method");
                    discard = source.substring((int) positions.getStartPosition(unit, member),
                            (int) positions.getEndPosition(unit, member));
                }
            }
        }
        if (discard == null) throw new AssertionError("Missing production discard method");
        String fixture = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);
        String marker = "/* PRODUCTION_DISCARD_METHOD */";
        if (fixture.indexOf(marker) < 0 || fixture.indexOf(marker) != fixture.lastIndexOf(marker))
            throw new AssertionError("Expected one fixture insertion point");
        Files.write(Paths.get(args[2]), fixture.replace(marker, discard).getBytes(StandardCharsets.UTF_8));
    }
}
