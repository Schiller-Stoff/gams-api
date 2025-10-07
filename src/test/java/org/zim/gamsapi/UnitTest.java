package org.zim.gamsapi;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class to extend unittests from
 */
@ActiveProfiles("test")
// allows the usage of Mockito for all unittests
@ExtendWith(MockitoExtension.class)
public abstract class UnitTest {
  
}
