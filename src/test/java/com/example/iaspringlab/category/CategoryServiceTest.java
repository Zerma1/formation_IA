package com.example.iaspringlab.category;

import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.product.Product;
import com.example.iaspringlab.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category books;

    @BeforeEach
    void setUp() {
        books = new Category("Livres");
        ReflectionTestUtils.setField(books, "id", 1L);
    }

    @Test
    void create_savesCategory() {
        when(categoryRepository.save(any(Category.class))).thenReturn(books);

        CategoryResponse response = categoryService.create(new CategoryRequest("Livres"));

        assertThat(response).isEqualTo(new CategoryResponse(1L, "Livres"));
    }

    @Test
    void update_renamesCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(books));

        CategoryResponse response = categoryService.update(1L, new CategoryRequest("Romans"));

        assertThat(response.name()).isEqualTo("Romans");
    }

    @Test
    void delete_detachesProductsAndKeepsThem() {
        Product product = new Product("REF-1", "Widget", new BigDecimal("10.00"), true);
        product.replaceCategories(List.of(books));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(books));
        when(productRepository.findByCategoriesId(1L)).thenReturn(List.of(product));

        categoryService.delete(1L);

        assertThat(product.getCategories()).isEmpty();
        verify(categoryRepository).delete(books);
    }

    @Test
    void getById_throwsResourceNotFoundException_whenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
