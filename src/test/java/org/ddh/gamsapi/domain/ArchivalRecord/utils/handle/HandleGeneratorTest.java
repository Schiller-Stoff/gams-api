package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.infrastructure.System.configproperties.HandleServerProperties;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.List;
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
  class IsManagedHandle {

    private final List<String> MANAGED_HANDLES = List.of(
        "hdl:11471/zrqluwyzaa","11471/zrqluwyzaa",
        "11471/zrqluwyzaa", "11471/zrqluwyzaa",
        "11471/144.10.3055",  "11471/144.10.3055",
        "11471/505.20.69", "11471/505.20.69",
        "11471/1020.20", "11471/1020.20",
        "11471/1020.20.1", "11471/1020.20.1",
        "11471/561.10.53", "11471/561.10.53",
        "11471/908.20", "11471/908.20",
        "11471/559.20.7696", "11471/559.20.7696",
        "11471/518.10.1.11733", "11471/518.10.1.11733"
    );

    private final List<String> MALFORMED_HANDLES = List.of(
        "hdl:11470/zrqluwyzaa","1147/zrqluwyzaa",
        "471/zrqluwyzaa", "1111/zrqluwyzaa",
        "1/144.10.3055",  "1/144.10.3055",
        "1/11471.20.69", "11470/11471"
    );

    @Test
    void returnsTrueAtExpectedHandles(){
      for (var handleString : MANAGED_HANDLES){
        var parsed = Handle.parse(handleString);
        Assertions.assertThat(handleGenerator.isManagedHandle(parsed))
            .isTrue();
      }
    }

    @Test
    void returnsTrueAtExpectedHandlesOptional(){
      for (var handleString : MANAGED_HANDLES){
        Assertions.assertThat(handleGenerator.isManagedHandle(handleString))
            .isPresent();
      }
    }

    @Test
    void returnsFalseAtDifferentHandles(){
      for (var handleString : MALFORMED_HANDLES){
        var parsed = Handle.parse(handleString);
        Assertions.assertThat(handleGenerator.isManagedHandle(parsed))
            .isFalse();
      }
    }

    @Test
    void returnsFalseAtDifferentHandlesOptional(){
      for (var handleString : MALFORMED_HANDLES){
        Assertions.assertThat(handleGenerator.isManagedHandle(handleString))
            .isEmpty();
      }
    }

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
