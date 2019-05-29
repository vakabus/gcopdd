# GCOPDD

![Travis CI Status](https://travis-ci.org/vakabus/gcopdd.svg?branch=master)

= Graal Compiler Optimization Phases Data Dumper

## Description of repository structure

* `graal`, `mx` and `PLuG` subdirectories are dependencies included as submodules. Documentation of basic Graal internals can be found [here](docs/graal_internals.md).
* `blood` is our code specifying instrumentation. Code from here is compiled and injected into Graal compiler. It's idea is documented [here](docs/blood.md).
* `tests` contains simple test applications that we can test the compiler on
* `docs` is for documentation
* `tools` contains scripts that can be used in conjunction with the instrumented compiler for convenience

## Dependencies

To use this project, you have to have these tools installed:

* Java 11
* GNU Make
* Python 2.7
* Python 3.4 or newer
* Gradle 5.0.0 or newer
* Ant

## Configuration

You can turn individual components on and off by modifying file `blood/config`. Lines starting with hash character (`#`) are comments and will be ignored.

Example config named `blood/config.example` will be generated during compilation. It contains all modules and they are by default enabled. When overriding user configuration is not found, this file will be used instead. So if you don't care about it, you can just ignore all the configuration files and it will just work.

## Running Java with instrumented Graal compiler

```sh
make
./vm {JAVA_ARGS}
```

This command can be run in cleanly cloned repository. Everything necessary will be built and in the end executed. When run again without changes to `blood` code, results from last builds will be reused.

## Browsing dumps

It is possible to view dumps in their graphical representation using a web browser.
Execute `tools/dump-browser` to get more information.

## Example

For example, you can run this to print 17th fibbonaci number:

```sh
./vm tests/Fibbonaci.java
```
