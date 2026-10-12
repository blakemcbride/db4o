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

The unit tests live in `db4oj.tests` and use the bundled `db4ounit` framework
(`db4ounit`, `db4ounit.extensions`). They are not part of the build yet,
because they do not compile with a modern `javac`: 11
`Assert.areEqual(double, Object)` calls in
`db4oj.tests/src/com/db4o/db4ounit/optional/monitoring` are ambiguous under
Java 8+ overload resolution and need an explicit cast.
