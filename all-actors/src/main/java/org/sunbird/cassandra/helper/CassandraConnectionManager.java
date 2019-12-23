package org.sunbird.cassandra.helper;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.sunbird.BaseException;

import java.util.List;

/**
 * Interface for cassandra connection manager , implementation would be Standalone and Embedde
 * cassandra connection manager .
 *
 * @author Amit Kumar
 * @author Arvind
 */
public interface CassandraConnectionManager {

  /**
   * Method to create the cassandra connection .
   *
   * @param ip
   * @param port
   * @param userName
   * @param password
   * @param keyspace
   * @return
   */
  boolean createConnection(
          String ip, String port, String userName, String password, String keyspace) throws BaseException;

  /**
   * Method to get the cassandra session oject on basis of keyspace name provided .
   *
   * @param keyspaceName
   * @return Session
   */
  Session getSession(String keyspaceName) throws BaseException;

  /**
   * Method to get the cassandra cluster oject on basis of keyspace name provided .
   *
   * @param keyspaceName
   * @return Cluster
   */
  Cluster getCluster(String keyspaceName) throws BaseException;

  /**
   * Method to get the cassandra cluster oject on basis of keyspace name provided .
   *
   * @param keyspaceName
   * @return List<String>
   */
  List<String> getTableList(String keyspaceName);
}
