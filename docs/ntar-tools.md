# ntar tools

You can use two tools for inspecting ntar files.


## `ntar.py` CLI

The Python module `ntar.py` can also be invoked as a command and offers basic command-line interface for inspecting ntar files.
Both `aliases.sh` and `aliases.fish` contain (among others) the alias `ntar` -> `.../gcopdd/tools/ntar.py`.

All of these commands are safe to use and produce meaningful output when used with dumps from `blood`.
The disclaimers only apply to ntar files from external sources.

For all subcommands, if `<FILE>` is omitted or `-`, read from stdin is attempted.

### `ntar list [<FILE>]`
Lists entries to stdout (one per line) in two columns: the first column contains the entry name and the second column contains its uncompressed size (in bytes, not utf8 characters).
Depending on characters used in entry names, the output may be ambiguous, and is not intended to be parsed.
A malicious escape character in entry name can disrupt the console.

### `ntar dump [<FILE>]`
Dumps entries in human-readable format to stdout. For each entry, `=== entry_name ===` is printed, followed by newline, entry content, newline.
Depending on characters used in entry names and contents, the output may be ambiguous, and is not intended to be parsed.
Non-character entry contents or a malicious escape character in entry name can disrupt the console.

### `ntar hexdump [<FILE>]`
Dumps entries in a human-readable format to stdout. For each entry, `=== entry_name ===` is printed, followed by newline and hexa-editor-like output.
The first column is offset in hexadecimal, next are 16 hexadecimal byte values,
then the same values in ASCII (non-ascii and control chars replaced with dots (`.`)), enclosed between `>` and `<`.
Depending on characters used in entry names, the output may be ambiguous, and is not intended to be parsed.
A malicious escape character in entry name can disrupt the console.

### `ntar xf <FILE> <DIR>`
A parody of `tar xf`. Extracts entries to files in newly-created directory `<DIR>`.
Replaces slashes (`/`) in names with underscores (`_`) and escapes leading dot (`.`) by prepending an underscore (`_`).
Note that `xf` in ntar is a subcommand, not two individual options `-x`, `-f`.
Malicious control or quoting characters in entry name can make the output files hard (but not impossible (unless you use Windows)) to list, view, manipulate or delete.

### `ntar x <DIR>`
Similar to above, but reads from stdin. Equivalent to `ntar xf - <DIR>`.


## `ntar-fm` GUI

`ntar-fm` stands for "ntar file manager" and its graphical interface remotely resembles that of file/archive managers.

### Installation

Try running `tools/ntar-fm` in the repository root directory. If you get `ModuleNotFoundError: No module named 'PyQt5'`, you will have to install PyQt5.
(For example using one of the following commands. Do not use them unless you know what they do.)

```
pip3 install --user PyQt5
sudo pip3 install PyQt5
sudo pacman -S python-pyqt5
sudo apt get python3-pyqt5
```

### Starting

In most cases, it is enough to run `tools/ntar-fm` without arguments (or run it from GUI). The program will find the `dumps` directory using its own location.

If you want to run it in a specific directory, use `tools/ntar-fm <DIR>`. `<DIR>` is relative to your CWD (as usual).

### Interface

The interface is split into two parts. The left panel contains the directory tree of the dumps directory.
It includes all files, not just ntar files, but selecting a non-ntar file displays an error message.

Selecting a ntar file displays it in the right panel. The right panel contains a tab for each entry in the ntar file.
The content of the entry is assumed to be UTF-8. Other data cannot be viewed (displaying an error message instead).

ntar FM currently only allows reading, not writing. Upon external changes, the tree view updates automatically, but the tabbed text view does not.
Reload the file by selecting another file (or directory) and the file in question again.

You can drag the splitter between the two panels to resize them.
