// Copyright (C) 2018 ScyllaDB
// Use of this source code is governed by a ALv2-style
// license that can be found in the LICENSE file.

package com.scylladb.driver.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

import static org.junit.Assert.*;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.LoadBalancingPolicy;

class InspectingLoadBalancingPolicyDecorator implements Function<LoadBalancingPolicy, LoadBalancingPolicy> {
	
	private final Set<Host> hosts = Collections.synchronizedSet(new HashSet<>());

	@Override
	public LoadBalancingPolicy apply(LoadBalancingPolicy p) {
		return new InspectingLoadBalancingPolicy(p);
	}

	void clearHosts() {
		hosts.clear();		
	}

	void assertOnlyOneHostContacted() {
		assertEquals(1, hosts.size());	
	}

	Host getContactedHost() {
		assertOnlyOneHostContacted();
		return hosts.iterator().next();
	}
	
	private class InspectingLoadBalancingPolicy implements LoadBalancingPolicy {
		
		private final LoadBalancingPolicy wrapped;
		
		InspectingLoadBalancingPolicy(LoadBalancingPolicy policy) {
			wrapped = policy;
		}
		

		@Override
		public void init(Cluster cluster, Collection<Host> hosts) {
			wrapped.init(cluster, hosts);
		}

		@Override
		public HostDistance distance(Host host) {
			return wrapped.distance(host);
		}

		@Override
		public Iterator<Host> newQueryPlan(String loggedKeyspace, Statement statement) {
			final Iterator<Host> wrappedIterator = wrapped.newQueryPlan(loggedKeyspace, statement);
			return new Iterator<Host>() {

				@Override
				public boolean hasNext() {
					return wrappedIterator.hasNext();
				}

				@Override
				public Host next() {
					Host host = wrappedIterator.next();
					hosts.add(host);
					return host;
				}
			};
		}

		@Override
		public void onAdd(Host host) {
			wrapped.onAdd(host);
		}

		@Override
		public void onUp(Host host) {
			wrapped.onUp(host);			
		}

		@Override
		public void onDown(Host host) {
			wrapped.onDown(host);
		}

		@Override
		public void onRemove(Host host) {
			wrapped.onRemove(host);
		}

		@Override
		public void close() {
			wrapped.close();
		}
		
	}
	
}
