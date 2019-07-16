# GCOPDD

![Travis CI Status](https://travis-ci.org/vakabus/gcopdd.svg?branch=master)

= Graal Compiler Optimization Phases Data Dumper

## Description of repository structure

* `graal`, `mx` and `PLuG` subdirectories are dependent git repositories. They are cloned during build. Documentation of basic Graal internals can be found [here](docs/graal_internals.md).
* `blood` is our code specifying instrumentation. Code from here is compiled and injected into Graal compiler. It's idea is documented [here](docs/blood.md).
* `tests` contains simple test applications that we can test the compiler on and that are quick to execute
* `docs` is for documentation
* `tools` contains scripts that can be used in conjunction with the instrumented compiler for convenience
* `dumps` (not in repo, but generated) contains collected data (see [this](docs/blood.md) for format)

## Dependencies

To use this project, you have to have these tools installed:

* Java 11
* GNU Make
* Python 2.7
* Python 3.4 or newer
* Gradle 5.0.0 or newer
* Ant
* git

## Usage

The idea behind the repository structure is that you provide a snapshot of Graal within `graal/` and it is further processed by our instrumentation, so that a Graal compiler with instrumented data collection tools is the output.

Basically, you could use it as follows:
```sh
git clone https://github.com/oracle/graal.git
# checkout whatever version of graal you like
make build

# collect data into a dumps/ directory by running it on some program
# command vm behaves just as a java command
./vm JAVA_ARGS

# generate and browse the report
tools/dump-browser

# to reset the system for a try with other version
make clean
```

### Configuration of data collection

You can turn individual components on and off by modifying file `blood/config`. Lines starting with hash character (`#`) are comments and will be ignored.

Example config named `blood/config.example` will be generated during compilation. It contains all modules and they are by default enabled. When overriding user configuration is not found, this file will be used instead. So if you don't care about it, you can just ignore all the configuration files and it will just work.

### Browsing dumps

It is possible to view dumps in their graphical representation using a web browser. [More information](docs/dump-browser.md)

### Concrete example of usage

```sh
# clone graal
git clone https://github.com/oracle/graal.git

# choose revision we want to analyse
git -C graal checkout vm-19.0.2

# checkout whatever version of graal you like
make build

# collect data into a dumps/ directory by running it on some program
# command vm behaves just as a java command
./vm tests/Fibonacci.java
```

## Versions of Graal it works on

The whole project should work should work without issues at least since version `vm-1.0.0-rc7` (git tag). It builds fine in RC6, but the JVM won't start. That's probably fixable.

Everything before that is not buildable and it does not work. Java11 was not released at that time but it's used in `blood`.
