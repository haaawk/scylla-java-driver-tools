// Copyright (C) 2018 ScyllaDB
// Use of this source code is governed by a ALv2-style
// license that can be found in the LICENSE file.

package com.scylladb.driver.tools;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.LatencyTracker;
import com.datastax.driver.core.Statement;

class PagingOptimizingLatencyTracker implements LatencyTracker {

	@Override
	public void update(Host host, Statement statement, Exception exception, long newLatencyNanos) {
		if (statement instanceof PagingOptimizingStatement) {
			final PagingOptimizingStatement optimizingStatement = (PagingOptimizingStatement) statement;
			if (exception == null) {
				optimizingStatement.setLastHost(host);
			} else {
				final Host lastHost = optimizingStatement.getLastHost();
				if (lastHost != null && lastHost.equals(host)) {
					optimizingStatement.setLastHost(null);
				}
			}
		}
	}

	@Override
	public void onRegister(Cluster cluster) {
	}

	@Override
	public void onUnregister(Cluster cluster) {
	}

}
