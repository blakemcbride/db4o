# Building db4o for Java

The Java version of db4o 8.1 is built with [BLD](https://github.com/blakemcbride/bld),
a small Java-based build system that is included in this repository
(`bld`, `bld.cmd` and `builder/`). The build definition is
`builder/Tasks.java`.

## Requirements

- JDK 17 or newer (tested with OpenJDK 21)

Nothing else is needed. BLD compiles itself the first time it runs, and the
few third-party jars the build uses are already in the repository.

## Build

From the repository root:

```sh
./bld jar          # Linux, macOS
bld.cmd jar        # Windows
```

This compiles all modules and writes the jars to `target/`:

- `target/db4o-8.1-all.jar`: everything in one jar
- `target/db4o-8.1-<module>.jar`: one jar per module

The first build takes a few seconds. Later builds recompile only the source
files that changed. After changing a class that other modules use, run
`./bld clean` first so that the dependent code is recompiled too.

`javac` prints notes about unchecked and deprecated APIs; these are expected.

## Tasks

| Task | What it does |
|---|---|
| `./bld build` | Compile all modules into `target/classes/<module>` |
| `./bld jar` | Build the jars in `target/` |
| `./bld javadoc` | Build the core API javadoc in `target/javadoc` |
| `./bld test` | Build and run the db4o test suite (see [Tests](#tests)) |
| `./bld manual` | Build the PDF manual, `manual/db4o-manual.pdf` (needs LaTeX: `latexmk`, `pdflatex`) |
| `./bld clean` | Remove `target/` |
| `./bld realclean` | Also remove the compiled build system |
| `./bld list-tasks` | List the tasks |

Put `-v` before the task (`./bld -v jar`) to see the commands being run.

## Modules

The library is split into these source projects. They are compiled in this
order, each against the ones before it.

| Module | Source | Contents |
|---|---|---|
| `core` | `db4oj/core/src` | The database engine (`com.db4o`) |
| `optional` | `db4oj.optional/core/src`, `db4oj.optional/tools/src` | Optional features: monitoring, BigMath, defragment, statistics, etc. |
| `cs` | `db4o.cs/src` | Client/server |
| `cs-optional` | `db4o.cs.optional/src` | Optional client/server features (SSL, monitoring) |
| `bloat` | `bloat/src` | BLOAT bytecode library (LGPL), used by the enhancer |
| `instrumentation` | `db4o.instrumentation/src` | Bytecode instrumentation framework |
| `nqopt` | `db4onqopt/core/src` | Native Query optimizer |
| `taj` | `db4otaj/src` | Transparent Activation/Persistence enhancer |
| `tools` | `db4otools/src` | Enhancer Ant tasks and command-line tools |

Most applications only need `core` and, for client/server, `cs`. The `all`
jar contains every module, like the `db4o-8.1-all-java5.jar` of the original
distribution.

To add or change a module, edit the `MODULES` list in `builder/Tasks.java`.

## Quick check

```java
import com.db4o.*;

public class Hello {
    static class Pilot {
        String name;
        Pilot(String name) { this.name = name; }
    }

    public static void main(String[] args) {
        ObjectContainer db = Db4oEmbedded.openFile("hello.db4o");
        try {
            db.store(new Pilot("Michael"));
            for (Pilot p : db.query(Pilot.class))
                System.out.println(p.name);
        } finally {
            db.close();
        }
    }
}
```

```sh
javac -cp target/db4o-8.1-all.jar Hello.java
java -cp target/db4o-8.1-all.jar:. Hello
```

## Tests

```sh
./bld test
```

This compiles the test framework (`db4ounit`, `db4ounit.extensions`) and the
test suite (`db4oj.tests`) into `target/test-classes`, then runs
`com.db4o.db4ounit.jre5.AllTestsDb4oUnitJdk5`, the main suite of about 9,000
tests. It takes 15 to 20 minutes and prints a lot of output; the summary
(`GREEN` or `RED (n out of m tests failed)`) comes just before the list of
failures. The suite runs in `target/test-run`, where it leaves its log
(`db4ounit.log`) and database files.

On JDK 21, the last full run had 18 of the 9,085 tests failing. The 7
client/server reflection failures have since been fixed. The others are:
- defragmenting databases whose classes are unavailable (4)
- `retainAll` on db4o's activatable `Stack` (3)
- file locking across VMs, where the test helper does not recognize current JVM names (3)
- a schema-update test that needs the removed `db4obuild/machine.properties` (1)

## Running on Java 9 and later

db4o uses reflection to store JDK classes such as `Hashtable`, `HashSet`,
`BigInteger` and `BigDecimal`. Since Java 9 the module system blocks this
unless the packages are opened, so applications that store such objects need
these JVM options:

```sh
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.math=ALL-UNNAMED
--add-opens java.base/java.io=ALL-UNNAMED
```

`./bld test` passes them. Without them about 1,800 of the tests fail.
