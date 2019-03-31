# Nodes of Graal's internal graph

## Implementation details

* All nodes inherit from one single abstract parent `org.graalvm.compiler.graph.Node`
* `Node` class implements `org.graalvm.compiler.graph.NodeInterface`, which allows you to call `Node asNode()` function on any `Node` object
  * reason why it's there is not clear to me right now, maybe just to avoid ugly casting

## How do optimization phases interact with Graph?

Based on observations of `DeadCodeEliminationPhase` and `LoopPartialUnrollPhase`, optimization phases walk the graph using some abstraction and perform modifying operations on the existing `Graph`. Therefore, `Graph` object is mutable. Nodes can be and are accessed by calling `getNodes()` method returning an iterator over all nodes. Documentation there says, that the nodes returned are only live nodes. As far as I understand it that's only leaking abstraction and can be ignored.

Looking at `ConditionalEliminationPhase`, you can see that `Graph` is just a simple container for nodes. For more high-level operations like this phase needs, `ControlFlowGraph` (next referred to as a CFG) can be calculated from the ordinary `Graph`. It allows the phases to operate on basic blocks instead of simple nodes. The CFG allows using visitor to walk the blocks.

## Interesting properties that might come handy

### Node annotation with arbitrary data

There is `getNodeInfo` and `setNodeInfo` methods, which can save any property for any Node with a key-value interface. The key here is a `Class` object. It's not stored in an effitient manner. It might not be a good idea to store a lot of objects there. Access time is $O(n)$ with $n$ being the number of stored key-value pairs.

### Listening for Graph changes

`Graph` object supports notifying listeners about changes in it's nodes.  There's an abstract class `Graph.NodeEventListener`, which we can implement hook ourselves to Graph changes. Sadly, the only information the listener gives is the node, which was changed. No other context is given.

The listener can be passed into `Graph` using an `Graph::trackNodeEvents` method. Intended use case is with try-with-resources statement (as in the snippet bellow), so that the listener removes itself from the `Graph`. For our use case, we can probably ignore that. To remove the listener, we must store the `NodeEventScope` object (see bellow) and call `close()` method on it.

```java
try (NodeEventScope nes = graph.trackNodeEvents(listener)) {
    // make changes to the graph
}
```


