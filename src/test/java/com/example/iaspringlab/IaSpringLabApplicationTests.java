package com.example.iaspringlab;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

/**
 * Verifies that the Spring context (Flyway migration included) starts against PostgreSQL.
 * Skipped when Docker is not available.
 */
@EnabledIfDockerAvailable
class IaSpringLabApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // The context starts only if Flyway migrated the schema and Hibernate validated it.
    }
}
