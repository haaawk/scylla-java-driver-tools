// Copyright (C) 2018 ScyllaDB
// Use of this source code is governed by a ALv2-style
// license that can be found in the LICENSE file.

package com.scylladb.driver.tools;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.StatementWrapper;

class PagingOptimizingStatement extends StatementWrapper {
	
	private volatile Host lastHost;

	PagingOptimizingStatement(Statement wrapped) {
		super(wrapped);
	}
	
	Host getLastHost() {
		return lastHost;
	}
	
	void setLastHost(Host host) {
		lastHost = host;
	}

}
