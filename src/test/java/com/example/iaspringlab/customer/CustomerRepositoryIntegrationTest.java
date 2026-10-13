package com.example.iaspringlab.customer;

import com.example.iaspringlab.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(PostgresTestContainerConfig.class)
@ActiveProfiles("test")
class CustomerRepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void save_persistsCustomer() {
        Customer saved = customerRepository.save(new Customer("Ada", "Lovelace", "ada@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(customerRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void save_rejectsDuplicateEmail_atDatabaseLevel() {
        customerRepository.saveAndFlush(new Customer("Ada", "Lovelace", "ada@example.com"));

        assertThatThrownBy(() -> customerRepository.saveAndFlush(new Customer("Grace", "Hopper", "ada@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByEmailIgnoreCase_isCaseInsensitive() {
        customerRepository.saveAndFlush(new Customer("Ada", "Lovelace", "Ada@Example.com"));

        assertThat(customerRepository.existsByEmailIgnoreCase("ada@example.com")).isTrue();
    }
}
