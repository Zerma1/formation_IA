package com.example.iaspringlab.product;

import com.example.iaspringlab.category.Category;
import com.example.iaspringlab.category.CategoryRepository;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.reference(), request.name(), request.price(), request.active());
        if (request.categoryIds() != null) {
            product.replaceCategories(findCategoriesOrThrow(request.categoryIds()));
        }
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(findProductOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * Products that can be added to an order, optionally restricted to one category.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getActive(Long categoryId) {
        List<Product> products = categoryId != null
                ? productRepository.findDistinctByActiveTrueAndCategoriesIdOrderByNameAsc(categoryId)
                : productRepository.findByActiveTrueOrderByNameAsc();
        return products.stream().map(ProductResponse::from).toList();
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProductOrThrow(id);
        product.setReference(request.reference());
        product.setName(request.name());
        product.setPrice(request.price());
        product.setActive(request.active());
        if (request.categoryIds() != null) {
            product.replaceCategories(findCategoriesOrThrow(request.categoryIds()));
        }
        return ProductResponse.from(product);
    }

    public void delete(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
    }

    Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    private List<Category> findCategoriesOrThrow(List<Long> categoryIds) {
        Set<Long> distinctIds = new HashSet<>(categoryIds);
        List<Category> categories = categoryRepository.findAllById(distinctIds);
        if (categories.size() != distinctIds.size()) {
            Set<Long> missing = new HashSet<>(distinctIds);
            categories.forEach(category -> missing.remove(category.getId()));
            throw new ResourceNotFoundException("Category not found with id(s) " + missing);
        }
        return categories;
    }
}
