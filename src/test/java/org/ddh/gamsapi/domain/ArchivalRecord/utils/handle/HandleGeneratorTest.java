package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.infrastructure.System.configproperties.HandleServerProperties;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

class HandleGeneratorTest extends UnitTest {

  private static HandleGenerator handleGenerator;

  private static HandleServerProperties handleServerProperties;

  @BeforeAll
  static void setup(){
    handleServerProperties = new HandleServerProperties();
    handleServerProperties.setBaseUrl("http://localhost:8080");
    handleServerProperties.setPrefix("11471");

    handleGenerator = new HandleGenerator(
        handleServerProperties
    );
  }

  @Nested
  class Format {

    @Test
    @DisplayName("uses the configured prefix")
    void usesConfiguredPrefix() {
      Assertions.assertThat(handleGenerator.generate().prefix())
          .isEqualTo(handleServerProperties.getPrefix());
    }

  }

  @Nested
  @DisplayName("produces distinct handles")
  class Randomness {

    final int TEST_SAMPLE_SIZE = 1_000_000;

    @Test
    @DisplayName("produces distinct handles")
    void producesDistinctHandles() {

      Set<Handle> handles = new HashSet<>();
      for (int i = 0; i < TEST_SAMPLE_SIZE; i++) {
        handles.add(handleGenerator.generate());
      }
      Assertions.assertThat(handles).hasSize(TEST_SAMPLE_SIZE);
    }

    @Test
    @DisplayName("produces distinct handles when called concurrently")
    void producesDistinctHandlesConcurrently() {
      Set<Handle> handles = ConcurrentHashMap.newKeySet();
      IntStream.range(0, TEST_SAMPLE_SIZE).parallel().forEach(i -> handles.add(handleGenerator.generate()));
      Assertions.assertThat(handles).hasSize(TEST_SAMPLE_SIZE);
    }

  }




}
