package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;

class BasicAuthHandleClientIT extends HandleIntegrationTest {

  @Nested
  class Register {

    @Test
    void registerExpectedHandleDoesNotThrow(){
      String pid = handleClient.generate();
      org.junit.jupiter.api.Assertions.assertDoesNotThrow(
          () -> handleClient.register(pid, URI.create("https://google.at"))
      );
    }

    @Test
    void registersExpectedHandle(){
      String pid = handleClient.generate();
      handleClient.register(pid, URI.create("https://google.at"));

      Assertions.assertThat(
          handleClient.exists(pid)
      ).isTrue();
    }

    @Test
    void throwsHandleAlreadyExistsExceptionIfHandleAlreadyExists(){
      final String PID = handleClient.generate();
      handleClient.register(PID, URI.create("https://google.at"));

      final URI DIFFERENT_TARGET = URI.create("https://different-target.at");
      Assertions.assertThatThrownBy(
          () -> handleClient.register(PID, DIFFERENT_TARGET)
      ).isInstanceOf(HandleAlreadyExistsException.class);

    }

    @Test
    void doesNotThrowIfHandleAlreadyExistsButTargetsAreTheSame(){
      String pid = handleClient.generate();
      handleClient.register(pid, URI.create("https://google.at"));

      org.junit.jupiter.api.Assertions.assertDoesNotThrow(
          () -> handleClient.register(pid, URI.create("https://google.at"))
      );
    }

  }

  @Nested
  class Retarget {

    @Test
    void retargetExpectedHandleDoesNotThrow(){
      String pid = handleClient.generate();
      handleClient.register(pid, URI.create("https://google.at"));
      org.junit.jupiter.api.Assertions.assertDoesNotThrow(
          () -> handleClient.retarget(pid, URI.create("https://google.at"))
      );

    }

    @Test
    void resolvesExpectedRetarget(){
      String pid = handleClient.generate();
      handleClient.register(pid, URI.create("https://google.at"));

      final String TEST_TARGET = "https://gams.uni-graz.at";
      handleClient.retarget(pid, URI.create(TEST_TARGET));

      final String ACTUAL_TARGET = handleClient.resolveTarget(pid).orElseThrow().toString();

      Assertions.assertThat(ACTUAL_TARGET).isEqualTo(TEST_TARGET);

    }

  }

  @Nested
  class ResolveTarget {

    @Test
    void resolvesExpectedTargetDoesNotThrow(){
      final String TEST_PID = handleClient.generate();
      final String TEST_TARGET = "https://google.at";
      handleClient.register(TEST_PID, URI.create(TEST_TARGET));

      org.junit.jupiter.api.Assertions.assertDoesNotThrow(
          () -> handleClient.resolveTarget(TEST_PID).orElseThrow()
      );

    }

    @Test
    void resolvesExpectedTarget(){
      final String TEST_PID = handleClient.generate();
      final String TEST_TARGET = "https://google.at";

      handleClient.register(TEST_PID, URI.create(TEST_TARGET));

      final String actualTarget = handleClient.resolveTarget(TEST_PID).orElseThrow().toString();

      Assertions.assertThat(actualTarget).isEqualTo(TEST_TARGET);


    }

  }

  @Nested
  class Exists {

    @Test
    void returnsTrueWhenHandleExists(){
      final String TEST_PID = handleClient.generate();
      handleClient.register(TEST_PID, URI.create("https://google.at"));
      Assertions.assertThat(handleClient.exists(TEST_PID))
          .isTrue();
    }

    @Test
    void returnsFalseWhenHandleDoesNotExist(){
      final String TEST_PID = handleClient.generate();
      Assertions.assertThat(handleClient.exists(TEST_PID))
          .isFalse();
    }

  }

  @Nested
  class Delete {

    @Test
    void deletesExpectedHandle(){
      final String TEST_PID = handleClient.generate();
      handleClient.register(TEST_PID, URI.create("https://google.at"));
      Assertions.assertThat(handleClient.exists(TEST_PID)).isTrue();
      handleClient.delete(TEST_PID);
      Assertions.assertThat(handleClient.exists(TEST_PID)).isFalse();

    }

  }

}
