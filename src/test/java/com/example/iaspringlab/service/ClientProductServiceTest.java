package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.iaspringlab.dto.CreateClientRequest;
import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ClientProductServiceTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductService productService;

    @Test
    void clientEmailMustBeUnique() {
        String email = "unique+" + System.nanoTime() + "@example.com";
        clientService.create(new CreateClientRequest("John", "Doe", email));

        assertThatThrownBy(() -> clientService.create(new CreateClientRequest("Jane", "Doe", email)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("email");
    }

    @Test
    void productReferenceMustBeUnique() {
        String reference = "REF-" + System.nanoTime();
        productService.create(new CreateProductRequest(reference, "Produit", BigDecimal.TEN));

        assertThatThrownBy(() -> productService.create(new CreateProductRequest(reference, "Autre", BigDecimal.ONE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("référence");
    }

    @Test
    void productCanBeDeactivatedAndReactivated() {
        var created = productService.create(
                new CreateProductRequest("REF-" + System.nanoTime(), "Produit", BigDecimal.TEN));

        assertThat(productService.updateStatus(created.id(), false).active()).isFalse();
        assertThat(productService.updateStatus(created.id(), true).active()).isTrue();
    }
}
