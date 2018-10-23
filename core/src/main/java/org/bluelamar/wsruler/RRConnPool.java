/**
 * 
 */
package org.bluelamar;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements a Round Robin selection of service connection from the pool.
 *
 */
public class RRConnPool implements ConnPool {

	final Map<String, List<Connection>> svcConns = new HashMap<>();
	final Map<String, AtomicInteger> svcConnsNext = new HashMap<>();
	final Map<String, ConnCreds> svcConnCreds = new HashMap<>();
	
	public RRConnPool() {
		
	}
	
	/**
	 * Obtains a Connection object per the request service type.
	 * @svcName is name of type of service
	 * @return Connection object if the service type is supported
	 */
	@Override
	public Connection getConnection(String svcName) throws ConnException {
		
		// @todo keep limited number of connection rather than always
		// cloning and init'ing
		List<Connection> conns = svcConns.get(svcName);
		if (conns == null) {
			throw new IllegalArgumentException("RR-pool: no such service: " + svcName);
		}
		
		AtomicInteger svcConnNext = svcConnsNext.get(svcName);
		int next = svcConnNext.getAndIncrement();
		next %= conns.size();
		Connection conn = conns.get(next).clone();
		conn.doAuthInit(svcConnCreds.get(svcName));
		return conn;
	}

	/*
	 * Caller returns a Connection object obtained via @getConnection
	 */
	@Override
	public void returnConnection(Connection conn) {
		try {
			conn.close();
		} catch (IOException exc) {
			System.out.println("RR-pool: close connection exc: " + exc);
		}
	}

	/**
	 * Sets a Connection object for which clones will be created in calls
	 * to @getConnection.
	 */
	@Override
	public void setConnectionCloner(Connection connCloner, ConnCreds creds) {
		
		List<Connection> conns = svcConns.get(connCloner.getSvcName());
		if (conns == null) {
			conns = new ArrayList<>();
			svcConns.put(connCloner.getSvcName(), conns);
		}
		conns.add(connCloner);
	}

	/*
	 * Set a cloned connection object upper limit.
	 * @param numActiveConns is maximum cloned connections.
	 */
	public void setConnLimit(int numActiveConns) {
		// @todo unimplemented
	}
}
