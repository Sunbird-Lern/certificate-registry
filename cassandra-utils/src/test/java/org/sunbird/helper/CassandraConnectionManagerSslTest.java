package org.sunbird.helper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.datastax.driver.core.Cluster;
import java.io.File;
import java.net.URL;
import javax.net.ssl.SSLContext;
import org.junit.Test;

public class CassandraConnectionManagerSslTest {

  private static String resource(String name) throws Exception {
    URL url = CassandraConnectionManagerSslTest.class.getClassLoader().getResource(name);
    return new File(url.toURI()).getAbsolutePath();
  }

  private static Cluster.Builder builder() {
    return Cluster.builder().addContactPoint("127.0.0.1");
  }

  @Test
  public void applySsl_disabledLeavesBuilderUntouched() {
    Cluster.Builder b = builder();
    CassandraConnectionManagerImpl.applySsl(b, false, "");

    assertNull(b.getConfiguration().getProtocolOptions().getSSLOptions());
  }

  @Test
  public void applySsl_setsSslOptions() throws Exception {
    Cluster.Builder b = builder();
    CassandraConnectionManagerImpl.applySsl(b, true, resource("test-root.crt"));

    assertNotNull(b.getConfiguration().getProtocolOptions().getSSLOptions());
  }

  @Test(expected = IllegalStateException.class)
  public void applySsl_enabledWithoutCertFails() {
    CassandraConnectionManagerImpl.applySsl(builder(), true, "");
  }

  @Test
  public void buildSslContext_loadsSingleCertificate() throws Exception {
    SSLContext ctx = CassandraConnectionManagerImpl.buildSslContext(resource("test-root.crt"));
    assertNotNull(ctx);
  }

  @Test
  public void buildSslContext_loadsCertificateBundle() throws Exception {
    SSLContext ctx = CassandraConnectionManagerImpl.buildSslContext(resource("test-bundle.crt"));
    assertNotNull(ctx);
  }

  @Test(expected = IllegalStateException.class)
  public void buildSslContext_missingFileFails() {
    CassandraConnectionManagerImpl.buildSslContext("/does/not/exist.crt");
  }

  @Test(expected = IllegalStateException.class)
  public void buildSslContext_blankPathFails() {
    CassandraConnectionManagerImpl.buildSslContext(null);
  }

  @Test(expected = IllegalStateException.class)
  public void buildSslContext_fileWithoutCertificateFails() throws Exception {
    // a properties file is readable but holds no PEM certificate
    CassandraConnectionManagerImpl.buildSslContext(resource("dbconfig.properties"));
  }
}
