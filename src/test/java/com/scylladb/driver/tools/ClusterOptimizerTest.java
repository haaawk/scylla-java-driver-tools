// Copyright (C) 2018 ScyllaDB
// Use of this source code is governed by a ALv2-style
// license that can be found in the LICENSE file.

package com.scylladb.driver.tools;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.StatementWrapper;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class ClusterOptimizerTest {
	
	private static final String CONTACT_POINT = "127.0.0.1";
	
	private static final int FETCH_SIZE = 5;
	private static final int PARTITIONS_COUNT = 3;
	private static final int ROWS_COUNT = 3 * FETCH_SIZE;
	
	private Cluster testedCluster;
	private Session testedSession;
	private InspectingLoadBalancingPolicyDecorator loadBalancingPolicyDecorator;

	private static Cluster.Builder getClusterBuilder() {
		// It is important for tests that this is round robin because it will generate different host every time.
		return Cluster.builder().addContactPoint(CONTACT_POINT).withLoadBalancingPolicy(new RoundRobinPolicy());
	}
	
	@BeforeClass
	public static void setupTable() {
		try (final Cluster cluster = getClusterBuilder().build()) {			
			assertTrue("Test requires at least 3 nodes.", cluster.getMetadata().getAllHosts().size() > 2);
			assertTrue("ROWS_COUNT has to be bigger than FETCH_SIZE", ROWS_COUNT > FETCH_SIZE);
			
			final Session session = cluster.connect();

		    session.execute("CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION = { 'class' : 'SimpleStrategy' , 'replication_factor' : 1 };");
		    
		    session.execute("CREATE TABLE IF NOT EXISTS test.test ( pk int, ck int, value int,  PRIMARY KEY ((pk), ck));");
		    
		    session.execute("TRUNCATE TABLE test.test;");
		    
		    final BatchStatement batchStatement = new BatchStatement();
		    final PreparedStatement insertStatement = session.prepare(
		    		  "INSERT INTO test.test (pk, ck, value) VALUES (?, ?, ?);");
		    for (int pk = 0; pk < PARTITIONS_COUNT; ++pk) {
		    	for (int ck = 0; ck < ROWS_COUNT; ++ck) {
		    		batchStatement.add(insertStatement.bind(pk, ck, ck + 5));
		    	}
		    }
		    session.execute(batchStatement);
		}
	}
	
	@AfterClass
	public static void cleanUpTable() {
		try (final Cluster cluster = getClusterBuilder().build()) {
			cluster.connect().execute("DROP TABLE IF EXISTS test.test");
		}
	}
	
	@Before
	public void setupTest() {
		loadBalancingPolicyDecorator = new InspectingLoadBalancingPolicyDecorator();
		testedCluster = 
				ClusterOptimizer.buildWithPagingOptimizedWithLoadBalancingPolicyDecorator(getClusterBuilder(), loadBalancingPolicyDecorator);
		testedSession = testedCluster.connect();
	}
	
	@After
	public void cleanUpTest() {
		if (testedCluster != null) {
			testedCluster.close();
		}
	}
	
	private void executeAndReadAllRows(final Statement statement) {
		statement.setFetchSize(FETCH_SIZE);
	    
	    final ResultSet rs = testedSession.execute(statement);
	    for (@SuppressWarnings("unused") Row row : rs) {
	    	// Ignore the result
	    }
	}
	
	private void executeAsyncAndReadAllRows(final Statement statement) throws Exception {
		statement.setFetchSize(FETCH_SIZE);
	    
	    final ResultSetFuture rsFuture = testedSession.executeAsync(statement);
	    ResultSet rs = rsFuture.get();
	    do {
		    Iterator<Row> it = rs.iterator();
		    while (rs.getAvailableWithoutFetching() > 0) {
		    	it.next();
		    }
		    if (rs.isFullyFetched()) {
		    	break;
		    }
		    rs = rs.fetchMoreResults().get();
	    } while (true);
	}
	
	private void testStatement(final Statement statement) throws Exception {
		loadBalancingPolicyDecorator.clearHosts();
		executeAndReadAllRows(statement);
		loadBalancingPolicyDecorator.assertOnlyOneHostContacted();
		final Host contactedHost1 = loadBalancingPolicyDecorator.getContactedHost();
		
		loadBalancingPolicyDecorator.clearHosts();
		executeAndReadAllRows(statement);
		loadBalancingPolicyDecorator.assertOnlyOneHostContacted();
		final Host contactedHost2 = loadBalancingPolicyDecorator.getContactedHost();
		
		Assert.assertNotEquals(
				"Two consecutive executions of the same statement should go to different nodes due to round robin load balancing policy.",
				contactedHost1,
				contactedHost2);
		
		loadBalancingPolicyDecorator.clearHosts();
		executeAsyncAndReadAllRows(statement);
		loadBalancingPolicyDecorator.assertOnlyOneHostContacted();
		final Host contactedHost3 = loadBalancingPolicyDecorator.getContactedHost();
		
		Assert.assertNotEquals(
				"Two consecutive executions of the same statement should go to different nodes due to round robin load balancing policy.",
				contactedHost2,
				contactedHost3);
	}
	
	@Test
	public void testSimpleStatement() throws Exception {
		testStatement(new SimpleStatement("select * from test.test")); 
	}
	
	@Test
	public void testPreparedStatement() throws Exception {
		final PreparedStatement prepared = testedSession.prepare("select * from test.test where pk = ?;");
	    testStatement(prepared.bind(0));
	}
	
	@Test
	public void testBuiltStatement() throws Exception {
		testStatement(QueryBuilder.select().all().from("test", "test").where(QueryBuilder.eq("pk", 1)));
	}
	
	@Test
    public void testStatementWrapperWithSimpleStatement() throws Exception {
    	testStatement(new StatementWrapper(new SimpleStatement("select * from test.test")) {});
    }
	
	@Test
    public void testStatementWrapperWithPreparedStatement() throws Exception {
		final PreparedStatement prepared = testedSession.prepare("select * from test.test where pk = ?;");
    	testStatement(new StatementWrapper(prepared.bind(0)) {});
    }
	
    @Test
    public void testStatementWrapperWithBuildtStatement() throws Exception {
    	testStatement(new StatementWrapper(QueryBuilder.select().all().from("test", "test").where(QueryBuilder.eq("pk", 1))) {});
    }

}
