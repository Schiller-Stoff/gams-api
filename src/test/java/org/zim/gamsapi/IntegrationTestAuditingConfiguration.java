package org.zim.gamsapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.zim.gamsapi.System.security.IUserPrincipalAuditorMapping;
import org.zim.gamsapi.enums.TestUser;

import java.util.Optional;

/**
 * This class is used to provide a test configuration for the UserPrincipalAuditorMapping
 * to be used in the integration tests.
 */
@TestConfiguration
public class IntegrationTestAuditingConfiguration {

    @Bean
    @Primary
    IUserPrincipalAuditorMapping userPrincipalAuditorMappingITSetup() {
        return new UserPrincipalAuditorMapping();
    }

    /**
     * This class is used to provide a test value for the current auditor in the integration tests.
     */
    public static class UserPrincipalAuditorMapping implements IUserPrincipalAuditorMapping {
        @Override
        public Optional<String> getCurrentAuditor() {
            return Optional.of(TestUser.USERNAME.getValue());
        }
    }

}
