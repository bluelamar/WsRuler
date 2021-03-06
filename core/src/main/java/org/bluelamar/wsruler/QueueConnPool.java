/**
 * 
 */
package org.bluelamar.wsruler;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a queued selection of service connections from the pool.
 *
 */
public class QueueConnPool implements ConnPool {

	private static final Logger LOG = LoggerFactory.getLogger(QueueConnPool.class);
	
	final Map<String, List<RestConnection>> svcConns = new HashMap<>();
	final Map<String, AtomicInteger> svcConnsNext = new HashMap<>();
	final Map<String, ConnLoginFactory> svcConnCreds = new HashMap<>();
	
	public QueueConnPool() {
		
	}
	
	/**
	 * Obtains a Connection object per the request service type.
	 * @svcName is name of type of service
	 * @return Connection object if the service type is supported
	 */
	@Override
	public RestConnection getConnection(String svcName) throws ConnException {
		
		LOG.debug("QueueConnPool:getConnection: svcname=" + svcName);
		// @todo keep limited number of connection rather than always
		// cloning and init'ing
		List<RestConnection> conns = svcConns.get(svcName);
		if (conns == null) {
			throw new IllegalArgumentException("Queue-pool: no such service: " + svcName);
		}
		
		AtomicInteger svcConnNext = svcConnsNext.get(svcName);
		int current = svcConnNext.get();
		current %= conns.size();
		RestConnection conn = conns.get(current).clone();

		conn.doAuthInit(svcConnCreds.get(svcName));
		conn.setConnStatus(RestConnection.ConnStatus.Connected);
		return conn;
	}

	/*
	 * Caller returns a Connection object obtained via @getConnection
	 */
	@Override
	public void returnConnection(RestConnection conn) {
		try {
			if (conn.getConnStatus() == RestConnection.ConnStatus.BadConnection) {
				AtomicInteger svcConnNext = svcConnsNext.get(conn.getSvcName());
				int next = svcConnNext.getAndIncrement();
				LOG.debug("Return connection: bad connection: increment to next in queue=" + next);
			}
			conn.close();
		} catch (IOException exc) {
			LOG.error("Return connection: close connection exc: " + exc);
		}
	}

	/**
	 * Sets a Connection object for which clones will be created in calls
	 * to @getConnection.
	 */
	@Override
	public void setConnectionCloner(RestConnection connCloner, ConnLoginFactory creds) {
		
		LOG.debug("QueueConnPool:setConnectionCloner: svcname=" + connCloner.getSvcName());
		
		List<RestConnection> conns = svcConns.get(connCloner.getSvcName());
		if (conns == null) {
			conns = new ArrayList<>();
			svcConns.put(connCloner.getSvcName(), conns);
		}
		conns.add(connCloner);
		
		svcConnsNext.put(connCloner.getSvcName(), new AtomicInteger());
		
		svcConnCreds.put(connCloner.getSvcName(), creds);
	}

	/*
	 * Shutdown all connections from the pool.
	 */
	public void shutdown() {
		// @todo cleanup conns
	}
	
	/*
	 * Set a cloned connection object upper limit.
	 * @param numActiveConns is maximum cloned connections.
	 */
	public void setConnLimit(int numActiveConns) {
		// @todo unimplemented
	}
}
