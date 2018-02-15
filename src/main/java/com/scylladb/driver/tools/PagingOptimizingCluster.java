// Copyright (C) 2018 ScyllaDB
// Use of this source code is governed by a ALv2-style
// license that can be found in the LICENSE file.

package com.scylladb.driver.tools;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.google.common.util.concurrent.ListenableFuture;

class PagingOptimizingCluster extends Cluster {
	
	PagingOptimizingCluster(final Cluster.Builder builder,
			final Function<LoadBalancingPolicy, LoadBalancingPolicy> loadBalancingPolicyDecorator) {
		super(addPagingOptimizingLoadBalancingPolicies(builder, loadBalancingPolicyDecorator));
	}
	
	private static Cluster.Builder addPagingOptimizingLoadBalancingPolicies(final Cluster.Builder builder,
			final Function<LoadBalancingPolicy, LoadBalancingPolicy> loadBalancingPolicyDecorator) {
		final LoadBalancingPolicy loadBalancingPolicy =
				builder.getConfiguration().getPolicies().getLoadBalancingPolicy();
		final PagingOptimizingLoadBalancingPolicy pageOptimizingLoadBalancingPolicy =
				new PagingOptimizingLoadBalancingPolicy(loadBalancingPolicy);

		builder.withLoadBalancingPolicy(loadBalancingPolicyDecorator != null
				? loadBalancingPolicyDecorator.apply(pageOptimizingLoadBalancingPolicy)
				: pageOptimizingLoadBalancingPolicy);
		return builder;
	}
	
	@Override
	public Session connect() {
		return new PagingOptimizingSession(this, super.connect());
	}
	
	@Override
	public Session connect(final String keyspace) {
		return new PagingOptimizingSession(this, super.connect(keyspace));
	}
	
	private class ConnectFuture implements ListenableFuture<Session> {
		
		private final ListenableFuture<Session> parentFuture;
		
		ConnectFuture(final ListenableFuture<Session> parentFuture) {
			this.parentFuture = parentFuture;
		}
		
		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return parentFuture.cancel(mayInterruptIfRunning);
		}

		@Override
		public Session get() throws InterruptedException, ExecutionException {
			final Session parentSession = parentFuture.get();
			return new PagingOptimizingSession(PagingOptimizingCluster.this, parentSession);
		}

		@Override
		public Session get(final long timeout, final TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			final Session parentSession = parentFuture.get(timeout, unit);
			return new PagingOptimizingSession(PagingOptimizingCluster.this, parentSession);
		}

		@Override
		public boolean isCancelled() {
			return parentFuture.isCancelled();
		}

		@Override
		public boolean isDone() {
			return parentFuture.isDone();
		}

		@Override
		public void addListener(final Runnable callback, final Executor executor) {
			parentFuture.addListener(callback, executor);	
		}
	}
	
	@Override
	public ListenableFuture<Session> connectAsync() {
		return new ConnectFuture(super.connectAsync());
	}
	
	@Override
	public ListenableFuture<Session> connectAsync(final String keyspace) {
		return new ConnectFuture(super.connectAsync(keyspace));
	}
	
}
