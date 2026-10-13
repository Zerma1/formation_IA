package com.example.iaspringlab;

import com.example.iaspringlab.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@Import(PostgresTestContainerConfig.class)
@ActiveProfiles("test")
class IaSpringLabApplicationTests {

    @Test
    void contextLoads() {
    }

}
