# Graal Compiler

## Compilers in HotSpot VM

### Overview of HotSpot VM compilation process

The JVM can run bytecode in multiple modes. They differ by their ability to collect profiling information (if branches are usually taken or not) and by performace. As one might expect, when collecting data, the performance is penalized.

Every function begins by having its bytecode **interpreted**. This is the slowest possible way to run it, but it can handle all edge case behaviours and collects lots of useful information about how the function runs. After a while, when the function is considered hot (runs enough times), it is **compiled with C1 compiler**. C1 compiler is non-optimizing compiler written in C++. It generates code that looks close to the bytecode and which keeps collecting profiling information. The compilation is rather fast. Then again, when this method is called enough times, **tier 2 compiler** compiles this function again. This time, the tier 2 compiler is an optimizing compiler. No profiling information is collected with the compiled code. It's the most performant code JVM can generate.

Tier 2 compiler used is by default is called the C2 compiler. It's also written in C++ and it has been part of HotSpot VM for quite a while. Recently, since Java 11, the tier 2 compiler has been abstracted away by designing JVMCI - JVM Compiler Interface. The main reason behind doing that has been the development of Graal compiler which is plug-in replacement for C2 compiler.

### What should the tier 2 compiler be able to do?

The obvious answer is to compile code. But that's not enough. HotSpot VM is designed to allow **code doptimization**, so that any running method can jump back into the interpreter when it want's to. This can (and is) used to improve code locality and also to handle edge cases like throwing exceptions when something goes wrong. The compiler can assume something and compile the method with that assumption. Then, when the assumption turns out to be false, it can deoptimize and just interpret the bytecode.

The compiler should also support **On Stack Replacement** (OSR) technique to patch running functions with faster code. That means, when a method is considered hot, but it's still running in loop or something similar, the compiler can compile just the inner part of the loop to make it run faster.

In the standard library of Java, there are performance critical functions that can be called. For example `System.nanoTime()`. These function are implemented with a bytecode snippet provided by the HotSpot VM. This snippet can be than optimized and inlined inside the function that uses it. In Graal compiler, this part can be found in class named `Stub`.

## Intermediate language (IL)

Graal compiler uses an IL in SSA form. It's internally represented as a graph with nodes being operations and edges as dependencies between them. There are 2 types of dependencies - data dependency and control flow dependency. There are similarly two types of nodes - floating nodes and fixed nodes. Floating nodes can move around, they don't have a specific place in the code. Fixed nodes must be placed where they belong. To give an example of a floating node, runtime guards are usually made as floating nodes during compilation, so that they can be moved to the beginning of the function.

The IL can be displayed using a tool called "Ideal Graph Visualizer". This tool is released as part of enterprise version of Graal and its source code is proprietary.

Actually, there is also a secondary intermediate language LIR, which is used in the lowest tiers of compilation, when the compiler emits actual instruction for the target architecture. This intermediate language is the same as in C1 compiler. It can be displayed using a tool called "Client Compiler Visualiser".

## Compilation process

The core idea is, that the compiler consists of multiple optimization phases. These phases are then grouped together into phase suites, which are actually also normal phases. So you end up with this hierarchy of phases, which call each other in a structure that looks like a tree. Each one of them makes changes to the IL and in the end, one graph representing optimized code falls out. This is then passed into the lower phase of the compiler which emits final instructions. Not a lot of optimizations is performed at that moment, only the lowest level optimizations regarding memory access with proper instructions and similar.

### Phases

All phases are represented as subclasses of abstract class `org.graalvm.compiler.phases.BasePhase<C>` where `C` is type of data context being passed around between phase runs. The actual work with the graph happens in protected abstract method `run(StructuredGraph graph, C context)`. This is where the logic is implemented. However, it's not meant to be called directly. For that, there's a final method in `BasePhase` called `apply(StructuredGraph graph, C context)`. Due to this, any implementation of optimization phase is provided with facilities such as graph dumping and resource usage tracking.

All phases are meant to be stateless, because they can be reused. They are however not singletons. New instances of them are created whenever needed.

### PhaseSuite

Phase grouping happens with class `org.graalvm.compiler.phases.PhaseSuite<C>` extending `BasePhase<C>`. It keeps a list of other phases and when it's method `run` is called, it invokes their apply methods one by one.

Phase suites are used as a compiler configuration primitive. Graal enterprise version differs mainly by the implemented phases and by their composition into phases. There are 3 optimization tiers in the compiler - phase suites `HighTier`, `MidTier` and `LowTier` located in package `org.graalvm.compiler.core.phases`. These are then provided to the core compiler logic by an instance of `CompilarConfiguration` interface, in the community edition it's `org.graalvm.compiler.core.phases.CommunityCompilerConfiguration`.
