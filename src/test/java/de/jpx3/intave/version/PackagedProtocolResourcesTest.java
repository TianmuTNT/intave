package de.jpx3.intave.version;

import org.junit.jupiter.api.Test;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PackagedProtocolResourcesTest {
  @Test
  void loadsProductionClassesAndResourcesFromTheDistributionJar() throws Exception {
    String artifact = System.getProperty("intave.test.protocolJar");
    assumeTrue(artifact != null, "Run testProtocolResources to verify the distribution JAR");
    Path jar = Path.of(artifact).toRealPath();
    for (Class<?> type : new Class<?>[]{ServerProtocolVersion.class, ProtocolVersionConverter.class}) {
      assertEquals(jar, Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath());
    }
    for (String name : new String[]{"/protocolversions", "/locate"}) {
      URL resource = ProtocolVersionConverter.class.getResource(name);
      assertNotNull(resource, name + " must be bundled, without relying on the source-tree fallback");
      assertEquals("jar", resource.getProtocol(), name);
      JarURLConnection connection = (JarURLConnection) resource.openConnection();
      assertEquals(jar, Path.of(connection.getJarFileURL().toURI()).toRealPath(), name);
    }
  }
}
