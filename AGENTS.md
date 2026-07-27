# Huffman Visualizer — Agent Instructions

## Project overview

Huffman Visualizer is an academic desktop application that demonstrates
text compression and decompression using the Huffman algorithm.

The project uses:

- Java
- JavaFX
- FXML
- CSS
- Maven
- JUnit
- IntelliJ IDEA

The application must be understandable, robust, testable and suitable for
a university presentation.

## Main features

The application must:

- accept text typed by the user;
- load UTF-8 `.txt` files;
- analyze symbol frequencies and probabilities;
- construct a deterministic Huffman tree;
- generate a binary code for every symbol;
- encode the original text as a visible string of `0` and `1`;
- decode the binary string using the generated tree;
- verify that the reconstructed text exactly matches the original;
- calculate compression statistics;
- display the Huffman tree graphically;
- handle invalid inputs gracefully.

The project demonstrates Huffman compression. It does not initially create
a real binary compressed file.

## User interface

The JavaFX interface must contain three non-closable tabs.

### Home

- editable original-text area;
- button to open a UTF-8 `.txt` file;
- compress button;
- clear button;
- compressed binary-text area;
- decompress button;
- reconstructed-text area;
- original UTF-8 size;
- Huffman message size;
- theoretical saving percentage;
- verification status.

### Analysis

Display a table containing:

- visible symbol representation;
- Unicode code point;
- frequency;
- probability;
- Huffman code;
- code length.

Also display:

- total number of symbols;
- number of distinct symbols;
- weighted average code length.

### Huffman Tree

Display the Huffman tree graphically with:

- internal nodes;
- leaf nodes;
- frequencies;
- visible symbol labels;
- connections between nodes;
- `0` labels on left edges;
- `1` labels on right edges.

The tree may be placed inside a ScrollPane when it is larger than the window.

## Architecture rules

Keep the project separated into layers and responsibilities.

Suggested packages:

```
com.mati.huffman
com.mati.huffman.controller
com.mati.huffman.model
com.mati.huffman.service
com.mati.huffman.ui
com.mati.huffman.exception
```

Rules:

- algorithm classes must not depend on JavaFX;
- controllers must not contain the Huffman algorithm;
- FXML controls presentation structure;
- CSS controls visual styling;
- services contain application logic;
- models represent application data;
- avoid large classes with unrelated responsibilities;
- prefer immutable model objects where practical;
- do not introduce design patterns unless they provide a clear benefit.

## Unicode requirements

Do not treat Java `char` as a complete Unicode symbol.

Process text using Unicode code points:
```Java
text.codePoints();
```
Use `Integer` or `int` to represent a symbol internally.

Example frequency structure:
```
Map<Integer, Long>
```
Convert a code point back to a string using:
```
new String(Character.toChars(codePoint))
```
The application must correctly preserve:

- accented characters;
- ñ;
- non-Latin scripts;
- emojis represented by supplementary code points;
- spaces;
- tabs;
- line breaks.

Full grapheme-cluster segmentation is outside the initial project scope.
The Huffman symbols are Unicode code points.

## Huffman requirements

The Huffman implementation must:

use a priority queue;
assign `0` to a left branch;
assign `1` to a right branch;
support text containing only one distinct symbol;
assign code `0` when the tree contains a single leaf;
reject empty input before compression;
detect invalid binary input;
detect a binary sequence ending in the middle of a tree path;
produce deterministic results when frequencies are equal.

The node comparator must define a total and stable ordering. The same input
must produce the same tree and codes across repeated executions.

Possible tie-break information includes:

1. frequency;
2. minimum Unicode code point contained by the node;
3. node type where needed;
4. deterministic creation order.

The tie-breaking strategy must be documented and tested.

## Metrics

Calculate:

- total Unicode code-point count;
- distinct-symbol count;
- original UTF-8 byte size;
- original UTF-8 bit size;
- Huffman message bit size;
- weighted average code length;
- theoretical saving percentage.

Use:
```
probability = frequency / total symbols

average code length =
sum(probability × code length)

Huffman message size =
sum(frequency × code length)

saving percentage =
(1 - Huffman bits / original UTF-8 bits) × 100
```
Clearly state in the UI and documentation that the Huffman size represents
only the encoded message. It does not include the tree, code table or file
format metadata.

## Important edge cases
    
Tests must cover at least:

* empty input;
* one symbol;
* one repeated symbol;
* BANANA;
* equal frequencies;
* spaces;
* tabs;
* line breaks;
* accented Spanish text;
* Chinese or another non-Latin script;
* emojis;
* mixed Unicode text;
* invalid binary characters;
* incomplete binary paths;
* loading an empty file;
* replacing a previous compression result;
* clearing the application state.

## Development workflow

For every requested task:   

1. inspect the existing project before modifying it;
2. explain the planned changes;
3. implement only the requested phase;
4. do not rewrite unrelated files;
5. add or update tests;
6. run the relevant Maven tests;
7. report modified files;
8. report commands executed;
9. report failures honestly;
10. do not claim that something works unless it was tested.

Do not implement the entire application in one task.

Build commands

Prefer Maven Wrapper commands when `mvnw` exists.

Windows:
```PowerShell
.\mvnw.cmd test
.\mvnw.cmd clean verify
.\mvnw.cmd javafx:run
```
Otherwise use:

```PowerShell
mvn test
mvn clean verify
mvn javafx:run
```
Do not silently ignore compilation warnings or failing tests.

## Dependency policy

Before adding a dependency:

* explain why it is necessary;
* verify that the standard Java or JavaFX APIs are insufficient;
* avoid libraries used only for trivial functionality;
* keep the final application easy to package.

Review the initial JavaFX dependencies. Remove unused libraries only after
confirming that the current code does not require them.

Git policy

Do not:

* force-push;
* rewrite Git history;
* delete branches;
* commit generated build directories;
* commit IDE-specific user files;
* commit secrets or local absolute paths.

The `target/` directory must remain ignored.

Use small, descriptive commits when explicitly asked to commit.

