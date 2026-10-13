package com.example.iaspringlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.iaspringlab.domain.Product;
import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.dto.UpdateProductStatusRequest;
import com.example.iaspringlab.exception.DuplicateResourceException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createRejectsDuplicateReference() {
        when(productRepository.existsByReference("REF-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(new CreateProductRequest("REF-1", "Produit", new BigDecimal("10.00"))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void createSavesActiveProduct() {
        when(productRepository.existsByReference("REF-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.create(new CreateProductRequest("REF-1", "Produit", new BigDecimal("10.00")));

        assertThat(response.reference()).isEqualTo("REF-1");
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateStatusDeactivatesProduct() {
        Product product = new Product("REF-1", "Produit", new BigDecimal("10.00"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var response = productService.updateStatus(1L, new UpdateProductStatusRequest(false));

        assertThat(response.active()).isFalse();
    }

    @Test
    void updateStatusActivatesProduct() {
        Product product = new Product("REF-1", "Produit", new BigDecimal("10.00"));
        product.deactivate();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var response = productService.updateStatus(1L, new UpdateProductStatusRequest(true));

        assertThat(response.active()).isTrue();
    }

    @Test
    void getThrowsWhenProductMissing() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.get(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
