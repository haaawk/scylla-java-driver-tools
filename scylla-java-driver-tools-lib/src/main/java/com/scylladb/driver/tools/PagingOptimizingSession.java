// Copyright (C) 2018 ScyllaDB
// Use of this source code is governed by a ALv2-style
// license that can be found in the LICENSE file.

package com.scylladb.driver.tools;

import java.util.Map;

import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.google.common.util.concurrent.ListenableFuture;

class PagingOptimizingSession implements Session {
	
	private final Cluster cluster;
	private final Session session;
	
	PagingOptimizingSession(Cluster cluster, Session session) {
		this.cluster = cluster;
		this.session = session;
	}

	@Override
	public String getLoggedKeyspace() {
		return session.getLoggedKeyspace();
	}

	@Override
	public Session init() {
		return session.init();
	}

	@Override
	public ListenableFuture<Session> initAsync() {
		return session.initAsync();
	}

	@Override
	public ResultSet execute(String query) {
		return execute(new SimpleStatement(query));
	}

	@Override
	public ResultSet execute(String query, Object... values) {
		return execute(new SimpleStatement(query, values));
	}

	@Override
	public ResultSet execute(String query, Map<String, Object> values) {
		return execute(new SimpleStatement(query, values));
	}

	@Override
	public ResultSet execute(Statement statement) {
		return session.execute(new PagingOptimizingStatement(statement));
	}

	@Override
	public ResultSetFuture executeAsync(String query) {
		return executeAsync(new SimpleStatement(query));
	}

	@Override
	public ResultSetFuture executeAsync(String query, Object... values) {
		return executeAsync(new SimpleStatement(query, values));
	}

	@Override
	public ResultSetFuture executeAsync(String query, Map<String, Object> values) {
		return executeAsync(new SimpleStatement(query, values));
	}

	@Override
	public ResultSetFuture executeAsync(Statement statement) {
		return session.executeAsync(new PagingOptimizingStatement(statement));
	}

	@Override
	public PreparedStatement prepare(String query) {
		return session.prepare(query);
	}

	@Override
	public PreparedStatement prepare(RegularStatement statement) {
		return session.prepare(statement);
	}

	@Override
	public ListenableFuture<PreparedStatement> prepareAsync(String query) {
		return session.prepareAsync(query);
	}

	@Override
	public ListenableFuture<PreparedStatement> prepareAsync(RegularStatement statement) {
		return session.prepareAsync(statement);
	}

	@Override
	public CloseFuture closeAsync() {
		return session.closeAsync();
	}

	@Override
	public void close() {
		session.close();		
	}

	@Override
	public boolean isClosed() {
		return session.isClosed();
	}

	@Override
	public Cluster getCluster() {
		return cluster;
	}

	@Override
	public State getState() {
		return session.getState();
	}

}
