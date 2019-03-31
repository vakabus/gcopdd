# Nodes of Graal's internal graph

## Implementation details

* All nodes inherit from one single abstract parent `org.graalvm.compiler.graph.Node`
* `Node` class implements `org.graalvm.compiler.graph.NodeInterface`, which allows you to call `Node asNode()` function on any `Node` object
  * reason why it's there is not clear to me right now, maybe just to avoid ugly casting

## Interesting properties that might come handy

### Node annotation with arbitrary data

There is `getNodeInfo` and `setNodeInfo` methods, which can save any property for any Node with a key-value interface. The key here is a `Class` object. It's not stored in an effitient manner. It might not be a good idea to store a lot of objects there. Access time is $O(n)$ with $n$ being the number of stored key-value pairs.
