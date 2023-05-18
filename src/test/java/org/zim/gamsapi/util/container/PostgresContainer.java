package org.zim.gamsapi.util.container;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresContainer {

    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine");

    /**
     * Define the PostgreSQL version for the testcontainer instance
     *
     * @return Postgres testcontainer instance
     */
    public static PostgreSQLContainer<?> getInstance() {
        return postgres;
    }

}
