This project is a set of utilities that improve [datastax java-driver](https://github.com/datastax/java-driver) for [ScyllaDB](https://github.com/scylladb/scylla) and [Apache Cassandra®](https://github.com/apache/cassandra).

## ClusterOptimizer

[ClusterOptimizer](https://github.com/haaawk/scylla-java-driver-tools/blob/master/src/main/java/com/scylladb/driver/tools/ClusterOptimizer.java) is a util that allows creation of an optimized [Cluster](https://github.com/datastax/java-driver/blob/3.x/driver-core/src/main/java/com/datastax/driver/core/Cluster.java) from a `Cluster.Builder`.

The usage is very simple. Having a `Cluster.Builder`, we pass it to `ClusterOptimizer.buildWithPagingOptimized` instead of calling `.build()` on it.

```java
Cluster.Builder builder = ...
Cluster cluster = ClusterOptimizer.buildWithPagingOptimized(builder);
```

This will make sure that when a query results in multiple pages then all of those pages are obtained from the same node (if possible).
This makes locality of the query much better and allows better usage of caches.

## License

Copyright (C) 2018 ScyllaDB

This project is distributed under the Apache 2.0 license. See the [LICENSE](https://github.com/haaawk/scylla-java-driver-tools/blob/master/LICENSE) file for details.
It contains software from:

* [datastax java-driver project](https://github.com/datastax/java-driver), licensed under the Apache License Version 2.0 license

Apache®, Apache Cassandra®,  are either registered trademarks or trademarks of 
the Apache Software Foundation in the United States and/or other countries. 
No endorsement by The Apache Software Foundation is implied by the use of these marks.
