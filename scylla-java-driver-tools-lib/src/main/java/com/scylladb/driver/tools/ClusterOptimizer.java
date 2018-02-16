// Copyright (C) 2018 ScyllaDB
// Use of this source code is governed by a ALv2-style
// license that can be found in the LICENSE file.

package com.scylladb.driver.tools;

import java.util.function.Function;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.google.common.annotations.VisibleForTesting;

/**
 * Allow building {@link Cluster} from {@link Cluster.Builder} with certain optimizations.
 */
public class ClusterOptimizer {
	
	private ClusterOptimizer() {
		throw new UnsupportedOperationException(ClusterOptimizer.class.getName() + " is not supposed to be instantiated.");
	}
	
	/**
	 * Create a {@link Cluster} using a given {@link Cluster.Builder} but modify
	 * it in such a way that all a paged query is served by a single node if possible.
	 * 
	 * This is good for locality and makes cache usage much more efficient.
	 */
	public static Cluster buildWithPagingOptimized(final Cluster.Builder builder) {
		return buildWithPagingOptimizedWithLoadBalancingPolicyDecorator(builder, null);
	}
	
	/**
	 * Same as {@link ClusterOptimizer#buildWithPagingOptimized} but takes
	 * a load balancing policy decorator that wraps around load balancing policy
	 * and can be used for example to inspect what hosts were returned from
	 * load balancing policy. Structure looks like this:
	 * 
	 *   Cluster --> Decorator --> LoadBalancingPolicy
	 * 
	 * This method is used only for testing.
	 */
	@VisibleForTesting
	static Cluster buildWithPagingOptimizedWithLoadBalancingPolicyDecorator(final Cluster.Builder builder,
			final Function<LoadBalancingPolicy, LoadBalancingPolicy> loadBalancingPolicyDecorator) {
		return new PagingOptimizingCluster(builder, loadBalancingPolicyDecorator);
	}
	
}
