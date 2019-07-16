# dump-browser

**dump-browser** is a tool to visualize, aggregate and compare dumps produced by [blood](blood.md).

It renders the visualizations in HTML which it serves over HTTP to a client (web browser).

## Terminology

**dump**:
The result of a single run of an application under instrumented Graal JIT compiler.
Stored as a subdirectory in the `dumps/` directory.
Example: `2019-02-29T12:34:56.MergeSort`

**dump entry**:
One file in the dump and the information it holds.
Dump entry is identified by its type (*dump entry type*) and the *event* it originates in.
The number of dump entries should be (number of *events*) times (number of *types*).
Example: `76543210deadbeef.depmat`

**dump entry type** (sometimes just **type**):
The type of a *dump entry*. Entries of the same type are in the same format,
are viewed in the same way, and can be aggregated into one.
Examples: `depmat`, `nodelist`

**event** (sometimes **compilation event**):
One unit of compilation. Usually corresponds to one compiled method.
Currently identified by a 16-digit hexadecimal number.

**call tree**:
Similar to [call graph], except it captures the actual run of the program instead of its code.
If a procedure runs multiple times, it gets multiple vertices in the tree.
If a procedure calls another procedure multiple times (or in a loop),
it creates multiple edges to corresponding vertices.
In context of GCOPDD, call tree refers to the call tree of [phases] (their `BasePhase.apply` method).

[call graph]: https://en.wikipedia.org/wiki/Call_graph
[phases]: graal_internals.md#phases

## User guide

### Prerequisites

Required:

- Python 3.4 or newer
- Graphical web browser with HTML5 and CSS3 support (tested with newest Firefox and Chromium)

Recommended:

- JavaScript enabled (for navigation, no modern (later than 1997) features required)
- UNIX-like operating system (for starting server in background)

### First run

The current working directory has no effect on the operation of *dump-browser*
(it will always change to the correct directory).
Assuming you are in the repository root directory, execute `tools/dump-browser` in your shell.

It will create configuration file named `tools/dump-browser.conf` and exit.
You can edit the file, or leave it in the default configuration.

### Changing port number and address

By default, *dump-browser* uses port 8000, which is a common alternative HTTP port.
In case you are already using port 8000 for another purpose, change the `http_port` value in the configuration.
All integers between 1 and 65535 inclusively can be used,
but most systems do not allow normal users to use values less than 1024.

If you want to run the web browser on a different machine, you will have to change `http_address`.
This is not covered by this guide.
Keep in mind that *dump-browser* is vulnerable to DOS attacks and changing the value can let remote attackers in.

### Starting (normal usage)

The easiest way to use *dump-browser* is the `auto` subcommand,
which will start the server and open the client in your default web browser.
The exact command is `tools/dump-browser auto`.

You can also configure *dump-browser* to use `auto` as the default command
by replacing `default_command = 'usage'` with `default_command = 'auto'` in the configuration file.
This will make it possible to start *dump-browser* from your file manager (if it has this functionality).

Note that neither of above will automatically stop the server,
as the browser does not indicate it is not going to send any more requests.
If you want to stop it to release the memory and the network port, use `tools/dump-browser stop`.

You can get more information on invocation by issuing `tools/dump-browser usage`.

***Caution***: on some occasions, dump-browser (and especially the web browser used with it)
may take hundreds of megabytes or gigabytes of memory. Make sure you do not have unsaved data
in your browser, in case you (or your OOM killer) need to terminate it.

### Starting for debugging

Executing as `tools/dump-browser fg` will start *dump-browser*
and use the stdout and stderr for its debugging output (which would normally be discarded).
Server started in this way cannot be stopped by the `stop` subcommand,
but rather by sending it interrupt (Ctrl+C in the console).

### Common graphical interface

Upon starting the server and opening the corresponding page in your web browser,
*dump-browser* will open the latest dump and ask for instructions.

In the top of the window is the navigation bar, containgng from left to right:

- **The refresh button (circle arrow).**
  Use this after generating more dumps by `blood` to be able to browse the new dumps.

- **Dumps drop-down menu.**
  Use this to see different dumps or to select multiple dumps (for aggregation) using a [pattern](#patterns).

- **Events drop-down menu.**
  Use this to see different *compilation events* or to select multiple *compilation events* using a [pattern](#patterns).

- **Entry type list.**
  Here you can pick the *dump entry type* you are interested in.

The rest of the window depends mostly on what *entry type* was selected and whether a [pattern](#patterns) was used.
Viewers for *entry types* are described in the following sections.
Before continuing, please have look at [blood collection tools](blood.md#collection-tools).

### `depmat` and `nodemat`

These two are displayed as tables where each cell contains two numbers.
The first number is the quotient of X (positive occurences) and Y (total occurences) from the dump,
expressed as percentage. The second number is Z (iterations) from the dump.

The quotient also determines hue of the background color (red for 100%, green for 0%),
while the number of iterations determines the saturation.

In `nodemat`, the header of the table contains for each column the name of the node class.
The same header is repeated three times.
First before the first matrix, second between the two matrices, and third after the second matrix.
The rows (corresponding to phases) are not headed, but you can hover over the individual cells
to see the tooltip, or click them to be navigated to `phasestack` with the phase highlighted.
If you have enough desktop space (multiple monitors?), you can turn on the JS legend,
which combines advantages of both: you can see the entire call tree and highlight the phases
by moving mouse in the matrix. If available, this is the recommended way.

In `depmat` there are no headers whatsoever, but it is interactive in the same way.

Both also include a [call tree mode](#call-tree-modes) switch.

The aggregated versions of `depmat` and `nodemat` look and behave the same as the non-aggregated ones.

### `nodelist`

*Entries* of this *type* are displayed in the same way they are saved on the disk, except formatting.

In the aggregated version you can also see in how many *events* the individual node classes were seen,
and the share of such *events* within all matched *events*.

### `phasestack`

While the file contains a list of "snapshots" of the stack, it is displayed as hierarchical list.
For example, following file

```
a
a b
a b c
a b
a
a d
a
```

would be displayed as

```
- a
  - b
    - c
  - d
```

A [call tree mode](#call-tree-modes) switch is also present.

The aggregated version differs from the normal one in the same way it is with `nodelist`s.

### `request` (as well as unknown *entry types*)

Displayed exactly as read from the file.

Aggregated version is just concatenation of `request` entries in all matched *events*,
sorted by their *event* identifier.

### `timing`

When not aggregated, it is displayed exactly as read from the file.

The aggregated version is a table which, in addition to the timing of the individual events,
shows the corresponding `request`s and (in the "Recompilation" column) the number of times
the method from the request was seen (*, **). It takes form `A/B`, where A is how many times
it was seen before this *event* (inclusive) and B is how many times it was seen in total.

(*)  This uses the `request` entries, so it may be inaccurate (we do not have control over their format).

(**) Note that the counter only regards *events* that are in this table.
     (Not the *events* that are in other [categories](#categories), or not matched at all.)

### *Call tree* modes

As described in the [definition](#terminology), *call trees* may have multiple vertices for one phase class.
This can be changed by using a different call tree mode.
Using a different call tree mode influences how detailed is the view of `phasestack` dump entries.
Because the matrices use elements in `phasestack` as keys (`nodemat` for rows and `depmat` for both rows and columns),
call tree mode also affects these. There are four call tree modes:

- **`full`**:
  Turns off any transformation.
  Note that this can cause very big matrices to be rendered. Never use this option when aggregating.

- **`roll`**:
  Detects loops in the phases (i.e. when a phase calls another phase repeatedly in a row)
  and removes them. This is the default option.

- **`stack`**:
  Identifies every item by the list of phases on stack.
  In addition to removing loops in the same way as `roll`,
  it also removes all remaining cases where a phase calls another phase multiple times
  and merges items such as "inside A before A calls B" and "inside A after A calls B".

- **`top`**:
  Identifies every item by the top-most phase on stack.
  This does everything `stack` does, but also merges items such as "inside A called by B"
  and "inside A called by C".

#### Example

This is a real example although the names are shortened and most phases were left out for brevity.

```
(full)                 (roll)                 (stack)                (top)
- HighTier             - HighTier             - HighTier             - HighTier
  - Canonicalizer        - Canonicalizer        - Canonicalizer      - Canonicalizer
  - Inlining             - Inlining             - Inlining           - Inlining
    - PhaseSuite           - PhaseSuite           - PhaseSuite       - PhaseSuite
      - GraphBuilder         - GraphBuilder         - GraphBuilder   - GraphBuilder
    - Canonicalizer        - Canonicalizer        - Canonicalizer
    - PhaseSuite
      - GraphBuilder
    - Canonicalizer
  - IncrementalCan..     - IncrementalCan..     - IncrementalCan..   - IncrementalCan..
  - LoopFullUnroll       - LoopFullUnroll       - LoopFullUnroll     - LoopFullUnroll
  - IncrementalCan..     - IncrementalCan..
```

### Patterns

Instead of specifying a particular *dump* or *event*, you can use a pattern.
In that case, aggregated results are shown instead of displaying a single *dump entry*.

*Dumps* are matched by their identifiers (such as `2019-09-31T12:34:56.MergeSort`)
and *events* are matched by contents of their `.request` entry (and NOT by their identifier/hash).

#### Characters in patterns

Non-ASCII and control characters (codepoints outside Basic Latin,
to be Unicodely-correct) are NOT supported in patterns.

Characters `0`-`9`, `A`-`Z`, `a`-`z`, `-.:_` have no special meaning,
because they can appear in *dump* identifiers.

The following characters have special meaning in URL and/or get percent-escaped,
so it is not practical to use them.

```
"#%/<>?\^`{}
SPACE
DELETE
```

We have assigned special meaning to `!&()*+,@|~`, which is described below.

Characters `$';=[]` are not (yet) used and are currently considered reserved.

#### At sign (`@`)

At sign is always used in the request URL to introduce a pattern.
When entering a pattern in the dialog box, DO NOT enter `@` - it will be added automatically.

#### Substring

The pattern to match a substring is the substring itself. It is case-insensitive.

Example: pattern `ox` matches `ox`, `fox`, `Oxford`, `proxy`, but not `x`.

#### Ranges

Given two strings A and B, range pattern matches all strings which start
with A or with B or with any string between them (in lexicographical order).

Example: `2019-01~2019-08` matches `2019-01-01T02:03:05.Fibonacci`, `2019-08-13T21:34:55.Fibonacci`,
`2019-02-29T12:34:56.MergeSort`, but not `2019-09-31T12:34:56.MergeSort`.

#### Logical operators

There are three logical operators: negation, conjunction and disjunction.

Negation is denoted by prefix `!`. Example: `!bc` matches `ad` and `abd`, but not `abcd`.

Conjunction is denoted by infix `&`. Example: `m&s` matches `mergesort` and `sortmerge`, but not `merge`.

Disjunction is denoted by infix `|`. Example: `m|s` matches `mergesort` and `merge`, but not `fibonacci`.

`+` can be used instead of `|` with the same priority and meaning.
This is intended for (1) mathematicians who think `+` should be used in regexes for disjunction
and (2) Google Chrome users, whose browser escapes `|` as `%7C`.

Of the logical operators, negation has the highest priority, and disjunction the lowest.
Example: `a|!b&c` means `a or ((not b) and c)` and `!3~7` means `not (between 3 and 7)`.

#### Parentheses

Parentheses can be used in the usual way to change the priority of operators.
Parentheses may NOT appear inside *ranges*.

#### Categories

A pattern can contain multiple expressions separated by `,`.
The effect is similar to opening multiple windows and using different patterns in them.

`,` has the lowest priority of all operators and may NOT appear inside parentheses.

Example: `scala,fibonacci|mergesort` displays two pages in one.
Assuming you have run all the bundled tests,
the first page will contain the aggregated results of Scalabench test
and the second page will contain the aggregated results of our two trivial tests.

#### Wildcard

`*` can be used instead of a pattern to match everything.

When used as one of categories, it matches everything that is not matched by the previous patterns.

`*` may NOT be used as an operand of any other function (`a|*`, `!*`).

Example: `*,a,b,*,c` applied on `a`, `b`, `c`, `d` yields (`a`, `b`, `c`, `d`), (`a`), (`b`), (`c`, `d`), (`c`).

## Internals

*dump-browser* is written entirely in Python 3
and is split into two main parts - *core* and *viewers*.
I tried to make *core* as general as possible and keep
all GCOPDD-, Graal- and blood-specific parts in the *viewers*.
*Core* is independent of any particular *viewers*.

### API between *core* and *viewers*

Every viewer is a module located in package `viewers`.
The name of a viewer is the same as the *type* of entries it can view.
The module must export following two functions:

```
view(
	file: io.TextIOWrapper,
	open_sibling: Callable[[str], io.TextIOWrapper],
	params: Params
) -> Iterable[str]

aggregate(
	files: Iterable[io.TextIOWrapper],
	open_sibling: Callable[[str], Iterable[io.TextIOWrapper]],
	params: Params
) -> Iterable[str]
```

..., where `Callable` and `Iterable` are defined as in
[`typing` standard module](https://docs.python.org/3/library/typing.html)
and `Params` is the class defined in `tools/dump-browser`.

The arguments `file` and `files` are open files that
should be viewed/aggregated.

The argument `open_sibling` of `view` takes a *dump entry type*
and returns an open file that comes from the same *event*
as `file`, but has the requested *dump entry type*.

The argument `open_sibling` of `aggregate` takes a *dump entry type*
and returns an iterable of open files that come from the same *events*
as `files`, but have the requested *dump entry type*.

The iterables of files must be sorted by their *event* identifier.
This applies both to iterables passed directly in arguments (`files`)
and to iterables returned by `open_sibling`.

The viewer is expected to close all the files after it no longer needs them.
This applies both to files passed directly in arguments (`file`, `files`)
and to files returned by `open_sibling`.

Both functions return an iterable of strings which should after
concatenation contain snippets of HTML that can be used in `<body>`.

If the HTML contains elements with `id` or `name` attributes,
the values of these attributes must be prefixed with
the value of `params.cat` to avoid conflicts.

#### Default viewer

At least one viewer is expected to exist, named `default`,
which is used when a specific viewer is not found.

In addition to `view` and `aggregate`,
it must also have `bytes` constant `STYLESHEET`.
This constant must contain ASCII-encoded CSS rules,
which will be applied to all outputs of all viewers.

### Overview of *core*

Dump-browser core consists of one executable file - `tools/dump-browser`.

The "`main` function" (actually just a piece of global code) is near end of the executable
and it loads configuration and then invokes function requested by user,
which most often means to (optionally fork, then) initialize the HTTP server and
enter its main loop, which is realized by `HTTPServer.serve_forever()` method,
a part of Python standard library.

After each request, this method calls back to our request handler, namely
`DumpBrowserHTTPRequestHandler.do_GET()`.
The handler parses and validates the URL and (except some special cases) delegates
the rest to function `viewer_dispatch(dump, event, type, params)`.

This function locates the requested file (or files for a pattern) in the filesystem,
opens (decompresses) it, selects the appropriate viewer according to the *entry type*,
invokes its `view` or `aggregate` function and wraps its result in a common GUI
using `html_all(...)` function.

All functions whose name starts with `html_` are iterators of
strings implemented using `yield` statements. This convention was chosen as
a more readable alternative to passing a handle to the socket as an argument.

### Overview of *viewers* used in this project

There are six viewers: `default`, `depmat`, `nodelist`, `nodemat`, `phasestack` and `timing`.

With one exception (`timing` uses `default`'s `view` function),
these viewers do not depend (`import`) on each other,
but they do share code - in the form of the `common` module.

This module contains (among others):

- utilities that I missed (or overlooked) in the standard library
- functions for convenient manipulation with text files
- data types that occur in multiple *types of entries*, such as `ClassDesc`
- functions for convenient manipulation with matrices
- functions for aggregation of `phasestack` data, which are also used in `depmat` and `nodemat`

All the modules mentioned here reside in the `tools/viewers/` directory.
