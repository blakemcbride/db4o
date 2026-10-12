/*
 * BLD build definition for the Java version of db4o.
 *
 * The library is made of several source projects that must be compiled in
 * order, each one against the classes of the ones before it.  MODULES lists
 * them in that order.  See BUILD.md.
 */

package builder;

import java.io.File;

import static builder.BuildUtils.*;


/**
 * This class contains the tasks that are executed by the build system.
 * <br><br>
 * The build system finds the names of the tasks through reflection.
 * It also does camelCase conversion.  So a task named abcDef may be evoked
 * as abc-def.
 * <br><br>
 * Each task must be declared as a public static method with no parameters.
 */
public class Tasks {

    final static String LIBS = "libs";
    final static String VERSION = "8.1";
    final static String BUILDDIR = "target";
    final static String CLASSES = BUILDDIR + "/classes";
    final static String TEST_CLASSES = BUILDDIR + "/test-classes";
    final static String TEST_RUN_DIR = BUILDDIR + "/test-run";
    final static String TEST_SUITE = "com.db4o.db4ounit.jre5.AllTestsDb4oUnitJdk5";

    /**
     * JDK packages db4o reflects into when storing JDK classes (collections, BigInteger, etc.).
     * Java 9+ blocks that unless they are opened.
     */
    final static String[] OPENS = { "java.base/java.lang", "java.base/java.util", "java.base/java.math", "java.base/java.io" };

    /**
     * A module: its name (used for the class directory and jar name) and its source roots.
     */
    private record Module(String name, String... sourceRoots) {}

    /**
     * The modules in dependency order.  Each is compiled against all the ones before it.
     */
    private final static Module[] MODULES = {
        new Module("core",            "db4oj/core/src"),
        new Module("optional",        "db4oj.optional/core/src", "db4oj.optional/tools/src"),
        new Module("cs",              "db4o.cs/src"),
        new Module("cs-optional",     "db4o.cs.optional/src"),
        new Module("bloat",           "bloat/src"),
        new Module("instrumentation", "db4o.instrumentation/src"),
        new Module("nqopt",           "db4onqopt/core/src"),
        new Module("taj",             "db4otaj/src"),
        new Module("tools",           "db4otools/src"),
    };

    /**
     * The test framework and the test suite, compiled against all of MODULES.
     */
    private final static Module[] TEST_MODULES = {
        new Module("db4ounit",            "db4ounit/src"),
        new Module("db4ounit-extensions", "db4ounit.extensions/src"),
        new Module("db4oj-tests",         "db4oj.tests/src", "db4oj.tests/generated/src"),
    };

    /**
     * Jars in the repository needed to compile.  Ant is only used by the enhancer's Ant tasks.
     */
    private static LocalDependencies baseLibs() {
        final LocalDependencies dep = new LocalDependencies();
        dep.add("db4oj/lib", "decaf-annotations.jar");
        dep.add("db4o.instrumentation/lib", "ant.jar");
        dep.add("db4o.instrumentation/lib", "ant-launcher.jar");
        return dep;
    }

    /**
     * Main entry point for the build system.  It tells the build system what arguments were passed in
     * and what class contains all the tasks.
     *
     * @param args the arguments to the program
     * @throws Exception if exception is thrown
     */
    public static void main(String[] args) throws Exception {
        BuildUtils.build(args, Tasks.class, LIBS);
    }

    /**
     * Display a list of valid tasks.  It is called by the build system
     * when the user selects the 'list-tasks' task.
     */
    public static void listTasks() {
        println("");
        println("build                    compile all modules into " + CLASSES);
        println("jar                      build the module jars and db4o-" + VERSION + "-all.jar in " + BUILDDIR);
        println("javadoc                  build the API javadoc in " + BUILDDIR + "/javadoc");
        println("test                     build and run the db4o test suite (takes several minutes)");
        println("manual                   build the PDF manual (manual/db4o-manual.pdf; needs LaTeX)");
        println("");
        println("clean                    remove all built files");
        println("realclean                + the compiled build system");
        println("");
    }

    /**
     * Compile all modules.  Only out-of-date sources are recompiled.
     */
    public static void build() {
        // resources: e.g. db4otools' instrumentation-def.properties
        compile(MODULES, CLASSES, baseLibs(), ".*\\.properties", null);
    }

    /**
     * Compile modules in order into classesDir/&lt;module&gt;, each against classpath plus the
     * modules before it, and copy the resources matching includeRegex/excludeRegex.
     */
    private static void compile(Module[] modules, String classesDir, LocalDependencies classpath,
                                String includeRegex, String excludeRegex) {
        for (Module m : modules) {
            final String dest = classesDir + "/" + m.name();
            classpath.add(classesDir, m.name());   // a module's later source roots see its earlier ones
            for (String root : m.sourceRoots()) {
                buildJava(root, dest, classpath, null, null);
                copyTreeRegex(root, dest, includeRegex, excludeRegex);
            }
        }
    }

    /**
     * Build and run the db4o test suite (TEST_SUITE).  It runs in TEST_RUN_DIR because it
     * leaves log and database files in its working directory.  The task fails if any test fails.
     */
    public static void test() {
        build();
        final LocalDependencies classpath = baseLibs();
        classpath.add("db4ounit.extensions/lib/easymock", "easymock.jar");
        for (Module m : MODULES)
            classpath.add(CLASSES, m.name());
        compile(TEST_MODULES, TEST_CLASSES, classpath, null, ".*\\.java");

        // Absolute paths, since the suite runs in TEST_RUN_DIR; passed in an argument file
        // so that paths with spaces work.
        final StringBuilder cp = new StringBuilder();
        for (String lib : new String[] { "db4oj/lib/decaf-annotations.jar", "db4o.instrumentation/lib/ant.jar",
                                         "db4o.instrumentation/lib/ant-launcher.jar",
                                         "db4ounit.extensions/lib/easymock/easymock.jar" })
            cp.append(new File(lib).getAbsolutePath()).append(File.pathSeparator);
        for (Module m : MODULES)
            cp.append(new File(CLASSES, m.name()).getAbsolutePath()).append(File.pathSeparator);
        for (Module m : TEST_MODULES)
            cp.append(new File(TEST_CLASSES, m.name()).getAbsolutePath()).append(File.pathSeparator);
        rmTree(TEST_RUN_DIR);
        mkdir(TEST_RUN_DIR);
        final StringBuilder opens = new StringBuilder();
        for (String pkg : OPENS)
            opens.append("--add-opens ").append(pkg).append("=ALL-UNNAMED\n");
        writeToFile(TEST_RUN_DIR + "/java-args",
                "-Xmx1g\n"
                + opens
                + quote("-Ddir.workspace=" + new File(".").getAbsoluteFile().getParent()) + "\n"
                + "-cp " + quote(cp.toString()) + "\n"
                + TEST_SUITE + "\n");
        runWait(true, TEST_RUN_DIR, "java @java-args");
    }

    /**
     * Quote a value for a java argument file.
     */
    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Build one jar per module, plus db4o-VERSION-all.jar containing every module.
     */
    public static void jar() {
        build();
        final String all = BUILDDIR + "/all";
        for (Module m : MODULES) {
            final String classes = CLASSES + "/" + m.name();
            createJar(classes, BUILDDIR + "/db4o-" + VERSION + "-" + m.name() + ".jar");
            copyTree(classes, all);
        }
        createJar(all, BUILDDIR + "/db4o-" + VERSION + "-all.jar");
    }

    /**
     * Build the javadoc for the core API.
     */
    public static void javadoc() {
        buildJavadoc("db4oj/core/src", "db4oj/lib", BUILDDIR + "/javadoc", null);
    }

    /**
     * Build the PDF manual.  Requires LaTeX (latexmk, pdflatex).
     */
    public static void manual() {
        runWait(true, "manual", "make");
    }

    /**
     * Remove all files that were built.
     */
    public static void clean() {
        rmTree(BUILDDIR);
    }

    /**
     * Remove all files that were built and the compiled build system.
     */
    public static void realclean() {
        clean();
        rmRegex("builder", ".*\\.class");
    }

}
