package com.example.iaspringlab.product;

import com.example.iaspringlab.category.Category;
import com.example.iaspringlab.category.CategoryRepository;
import com.example.iaspringlab.category.CategoryResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

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

    @Test
    void create_attachesRequestedCategories() {
        Category books = category(10L, "Livres");
        Category music = category(11L, "Musique");
        when(categoryRepository.findAllById(Set.of(10L, 11L))).thenReturn(List.of(books, music));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProductRequest request = new ProductRequest("REF-1", "Widget", new BigDecimal("10.00"), true, List.of(11L, 10L));

        ProductResponse response = productService.create(request);

        assertThat(response.categories()).extracting(CategoryResponse::name).containsExactly("Livres", "Musique");
    }

    @Test
    void create_throwsResourceNotFoundException_whenCategoryMissing() {
        when(categoryRepository.findAllById(Set.of(10L, 99L))).thenReturn(List.of(category(10L, "Livres")));
        ProductRequest request = new ProductRequest("REF-1", "Widget", new BigDecimal("10.00"), true, List.of(10L, 99L));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_keepsCategories_whenCategoryIdsNotProvided() {
        existingProduct.replaceCategories(List.of(category(10L, "Livres")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        ProductResponse response = productService.update(1L,
                new ProductRequest("REF-1", "Widget", new BigDecimal("10.00"), true));

        assertThat(response.categories()).extracting(CategoryResponse::id).containsExactly(10L);
    }

    @Test
    void update_removesAllCategories_whenCategoryIdsEmpty() {
        existingProduct.replaceCategories(List.of(category(10L, "Livres")));
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findAllById(Set.of())).thenReturn(List.of());

        ProductResponse response = productService.update(1L,
                new ProductRequest("REF-1", "Widget", new BigDecimal("10.00"), true, List.of()));

        assertThat(response.categories()).isEmpty();
    }

    private static Category category(Long id, String name) {
        Category category = new Category(name);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
