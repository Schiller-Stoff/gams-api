package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.UnitTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class HandleTest extends UnitTest {


  final Map<String, String> HANDLE_EXPECTED_TO_STRING_MAP =  Map.of(
      "hdl:99999/zrqluwyzaa","99999/zrqluwyzaa",
      "99999/zrqluwyzaa", "99999/zrqluwyzaa",
      "11471/144.10.3055",  "11471/144.10.3055",
      "11471/505.20.69", "11471/505.20.69",
      "11471/1020.20", "11471/1020.20",
      "11471/1020.20.1", "11471/1020.20.1",
      "11471/561.10.53", "11471/561.10.53",
      "11471/908.20", "11471/908.20",
      "11471/559.20.7696", "11471/559.20.7696",
      "11471/518.10.1.11733", "11471/518.10.1.11733"
  );

  @Nested
  class IsValid {

    @Test
    void validatesExpectedHandles(){
      for (var handle : HANDLE_EXPECTED_TO_STRING_MAP.entrySet()){
        var currentHandleString = handle.getKey();
        Assertions.assertThat(Handle.isValid(currentHandleString))
            .isTrue();
      }
    }

    @Test
    void invalidatesExpectedHandles(){

      List<String> invalidHandles = List.of(
          "hd1:99999/zrqluwyzaa",
          "bla",
          "1",
          "hdl:99999zrqluwyzaa",
          "hdl:99999"
      );

      for (var handle : invalidHandles){
        Assertions.assertThat(Handle.isValid(handle))
            .isFalse();
      }

    }


  }

  @Nested
  class ToHdlUri {

    @Test
    void generatesExpectedHdlUri(){

      final var TEST_HANDLE = "99999/zrqluwyzaa";
      var parsedHandle = Handle.parse(TEST_HANDLE);
      Assertions.assertThat(parsedHandle.toHdlUri())
          .isEqualTo("hdl:" + TEST_HANDLE)
      ;
    }

  }

  @Nested
  class Parse {

    @Test
    void parsesExpectedHandles(){
      for (var handle : HANDLE_EXPECTED_TO_STRING_MAP.entrySet()) {
        var parsedHandle = Handle.parse(handle.getKey());
        Assertions.assertThat(parsedHandle.toString())
            .isNotNull()
            .isEqualTo(handle.getValue());
      }
    }

    @Test
    void throwsIllegalArgumentExceptionIfNoSlashIsContained(){
      var TEST_MALFORMED_HANDLE = "hdl:99999zrqluwyzaa";
      Assertions.assertThatThrownBy(() -> Handle.parse(TEST_MALFORMED_HANDLE))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsIfGivenHandleIsNull(){
      Assertions.assertThatThrownBy(() -> Handle.parse(null))
          .isInstanceOf(NullPointerException.class);
    }

  }


}
