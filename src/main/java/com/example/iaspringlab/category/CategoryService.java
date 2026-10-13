package com.example.iaspringlab.category;

import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public CategoryResponse create(CategoryRequest request) {
        return CategoryResponse.from(categoryRepository.save(new Category(request.name())));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return CategoryResponse.from(findCategoryOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);
        category.setName(request.name());
        return CategoryResponse.from(category);
    }

    /**
     * Deletes the category. Products attached to it are kept and simply detached from it.
     */
    public void delete(Long id) {
        Category category = findCategoryOrThrow(id);
        productRepository.findByCategoriesId(id).forEach(product -> product.removeCategory(category));
        categoryRepository.delete(category);
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
    }
}
