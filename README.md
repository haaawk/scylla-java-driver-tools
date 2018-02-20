This project is a set of utilities that improve [datastax java-driver](https://github.com/datastax/java-driver) for [ScyllaDB](https://github.com/scylladb/scylla) and [Apache Cassandra®](https://github.com/apache/cassandra).

## Important note

This tool was tested only with driver version 3.4. This means your application should be using driver version 3.4 to allow the tool to work correctly.
The tool may compile and work with other driver version but you have to test it yourself to make sure everything is ok.

## ClusterOptimizer

[ClusterOptimizer](https://github.com/haaawk/scylla-java-driver-tools/blob/master/src/main/java/com/scylladb/driver/tools/ClusterOptimizer.java) is a util that allows creation of an optimized [Cluster](https://github.com/datastax/java-driver/blob/3.x/driver-core/src/main/java/com/datastax/driver/core/Cluster.java) from a `Cluster.Builder`.

The usage is very simple. Having a `Cluster.Builder`, we pass it to `ClusterOptimizer.buildWithPagingOptimized` instead of calling `.build()` on it.

```java
Cluster.Builder builder = ...
Cluster cluster = ClusterOptimizer.buildWithPagingOptimized(builder);
```

This will make sure that when a query results in multiple pages then all of those pages are obtained from the same node (if possible).
This makes locality of the query much better and allows better usage of caches.

## Instumenting existing application

If you can't or don't want to modify your code, you can use a java agent that will instrument your existing code and apply the same optimization as if you had changed all invocations of `Cluster$Builder#build` to `ClusterOptimizer.buildWithPagingOptimized(builder)`.

To instrument your application you need to run it with the following command:

`JAVA_TOOL_OPTIONS="-javaagent:<path to scylla-java-driver-tools-agent-1.0.0-jar-with-dependencies.jar>" <command starting your application>`

## License

Copyright (C) 2018 ScyllaDB

This project is distributed under the Apache 2.0 license. See the [LICENSE](https://github.com/haaawk/scylla-java-driver-tools/blob/master/LICENSE) file for details.
It contains software from:

* [datastax java-driver project](https://github.com/datastax/java-driver), licensed under the Apache License Version 2.0 license

Apache®, Apache Cassandra®,  are either registered trademarks or trademarks of 
the Apache Software Foundation in the United States and/or other countries. 
No endorsement by The Apache Software Foundation is implied by the use of these marks.
