package com.example.iaspringlab.product;

import com.example.iaspringlab.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product existingProduct;

    @BeforeEach
    void setUp() {
        existingProduct = new Product("REF-1", "Widget", new BigDecimal("10.00"), true);
        ReflectionTestUtils.setField(existingProduct, "id", 1L);
    }

    @Test
    void create_savesActiveProduct() {
        ProductRequest request = new ProductRequest("REF-1", "Widget", new BigDecimal("10.00"), true);
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        ProductResponse response = productService.create(request);

        assertThat(response.reference()).isEqualTo("REF-1");
        assertThat(response.active()).isTrue();
    }

    @Test
    void getById_throwsResourceNotFoundException_whenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_canDeactivateProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        ProductRequest request = new ProductRequest("REF-1", "Widget", new BigDecimal("10.00"), false);

        ProductResponse response = productService.update(1L, request);

        assertThat(response.active()).isFalse();
    }

    @Test
    void update_changesPrice() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        ProductRequest request = new ProductRequest("REF-1", "Widget", new BigDecimal("15.50"), true);

        ProductResponse response = productService.update(1L, request);

        assertThat(response.price()).isEqualByComparingTo("15.50");
    }

    @Test
    void delete_removesProduct_whenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        productService.delete(1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        org.mockito.Mockito.verify(productRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
    }

    @Test
    void getAll_returnsAllProducts() {
        Product second = new Product("REF-2", "Gadget", new BigDecimal("5.00"), false);
        ReflectionTestUtils.setField(second, "id", 2L);
        when(productRepository.findAll()).thenReturn(java.util.List.of(existingProduct, second));

        java.util.List<ProductResponse> responses = productService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(ProductResponse::reference)
                .containsExactly("REF-1", "REF-2");
    }
}
