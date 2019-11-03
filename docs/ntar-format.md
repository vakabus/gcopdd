# ntar

> Perfection is finally attained not when there is no longer anything to add, but when there is no longer anything to take away.
>
> -- <cite>Antoine de Saint-Exupéry</cite>

ntar is a binary format for storing key-value data, created specifically for GCOPDD. The name stands for "not tar".


## Structure

The file consists of zero or more *entries*, concatenated without any padding or delimiters.

Each *entry* has a *key* (name) and a *value* (contents). The key is an ASCII string, and the value is a sequence of bytes.

Each entry is encoded as the concatenation of the following fields:

1. M: 4 bytes unsigned integer, MSB first
2. *key*: M bytes of ASCII string, one character per byte, NOT null-terminated
3. N: 4 bytes unsigned integer, MSB first
4. *value*: N bytes of data in the [zlib format]. (N is the size AFTER compression.)

Note 1: M and N are encoded with the most significant byte (MSB) first (also called big-endian or network byte order), which is usually the opposite of your processor's native byte order.
Note 2: M and N do NOT have to be aligned. No padding is supported by the ntar format.

[zlib format]: https://tools.ietf.org/html/rfc1950


## Repeated keys

Valid ntar file may contain multiple *entries* with the same *key*.

For each *key* K present in the file, the reader may ignore all entries whose *key* is K, except the last one.

Note that keys are considered equal IF AND ONLY IF they contain exactly the same characters. The comparison is sensitive to letter case, whitespace, and control characters.


## Example

We want to encode the following table:

| *key*  | *value* (bytes in hexadecimal)                  |
| ------ | ----------------------------------------------- |
| foo    | 09 F9 11 02 9D 74 E3 5B D8 41 56 C5 63 56 88 C0 |
| foo2   | 42 42 42 42 42 42 42 42 42 42 42 42 42 42 42 42 |
| naught |                                                 |

The sequence `09 F9 11 02 9D 74 E3 5B D8 41 56 C5 63 56 88 C0` is zlib-compressed to `78 9C E3 FC 29 C8 34 B7 E4 71 F4 0D C7 B0 A3 C9 61 1D 07 00 3B 6A 07 9A` (24 bytes).

The sequence `09 F9 11 02 9D 74 E3 5B D8 41 56 C5 63 56 88 C0` is zlib-compressed to `78 9C 73 72 42 05 00 23 20 04 21` (11 bytes).

The empty sequence os zlib-compressed to `78 9C 03 00 00 00 00 01` (8 bytes).

The file will contain the following fields:

- `00 00 00 03` (length of "foo" = 3)
- `66 6F 6F` ("foo")
- `00 00 00 18` (length of following field = 24)
- `78 9C E3 FC 29 C8 34 B7 E4 71 F4 0D C7 B0 A3 C9 61 1D 07 00 3B 6A 07 9A`
- `00 00 00 04` (length of "foo2" = 4)
- `66 6F 6F 32` ("foo2")
- `00 00 00 0B` (length of following field = 11)
- `78 9C 73 72 42 05 00 23 20 04 21`
- `00 00 00 06` (length of "naught" = 6)
- `6E 61 75 67 68 74` ("naught")
- `00 00 00 08` (length of following field = 8)
- `78 9C 03 00 00 00 00 01`

So the file will contain the following sequence of bytes:

`00 00 00 03 66 6F 6F 00 00 00 18 78 9C E3 FC 29 C8 34 B7 E4 71 F4 0D C7 B0 A3 C9 61 1D 07 00 3B 6A 07 9A 00 00 00 04 66 6F 6F 32 00 00 00 0B 78 9C 73 72 42 05 00 23 20 04 21 00 00 00 06 6E 61 75 67 68 74 00 00 00 08 78 9C 03 00 00 00 00 01`

## Advantages

- simplicity: given zlib library, the file can be {en,de}coded using tens of lines of code
- it is easily parsable: parsers do not have to iterate over characters, looking for delimiters, processing escapes, etc.
- low memory overhead: 8B per entry (zlib can add more overhead - analyzing this is out of the scope of this spec)
- new entries can be added by appending to the file without reading it


## Disadvantages

- it is not a standard: libraries and tools except those in GCOPDD do not exist
- it does not have a magic value: format detection (other than validating the file) is impossible, reading another file as ntar is not guaranteed (but is probable) to fail
- it is flat: encoding a tree structure requires either using full path in keys or putting ntar files into values (which means compressing already compressed data)
- it is not human-readable (which is the fate of binary formats) - but due to compression, it could not be done in a human readable way (base64 is not human readable)


## Recommendations for other potential users

For keys, prefer letters (`a`-`z`), digits (`0`-`9`) and common neutral punctuation like hyphen (`-`), underscore (`_`) or period (`.`).

If you need to store text as the content (*value*), use UTF-8 to avoid problems with compatibility and debugging.
