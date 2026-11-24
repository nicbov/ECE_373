# ECE 373 document editor

Java 17 command-line document editor with sections, editing, formatting,
undo/redo, checkpoints, search/replace, statistics, rendering, scripts, and macros.

## How to run

Need Java 17 and Maven.

```bash
cd document_editor
mvn clean test
mvn -q exec:java -Dexec.mainClass=text_editor.Main
```

Type `help` after it starts. Example:

```text
create demo
add-section Main
add-paragraph 1, hello
insert 1#1, 5, -team
statistics
render
```

Run a script file (one command per line):

```bash
mvn -q exec:java -Dexec.mainClass=text_editor.Main \
  -Dexec.args="/absolute/path/to/commands.txt"
```

## Macros

```text
macro-start greeting
add-paragraph 1, hello
insert 1#1, 5, -team
macro-stop
greeting
```

One macro replay = one undo. A failed step rolls the whole replay back.

## Patterns

- Singleton: `DocumentEngine`
- Adapter: `SectionTreeReadView`
- Strategy: render views + line breakers
- Facade: `EditorApplication`

Diagrams are in `docs/`.
