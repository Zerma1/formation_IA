package com.example.iaspringlab.service;

import com.example.iaspringlab.domain.Product;
import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.dto.DtoMapper;
import com.example.iaspringlab.dto.ProductResponse;
import com.example.iaspringlab.exception.BusinessRuleException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import com.example.iaspringlab.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsByReference(request.reference())) {
            throw new BusinessRuleException("Un produit avec la référence '" + request.reference() + "' existe déjà");
        }
        Product product = new Product(request.reference(), request.name(), request.price());
        return DtoMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long id, boolean active) {
        Product product = loadProduct(id);
        product.setActive(active);
        return DtoMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(DtoMapper::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return DtoMapper.toProductResponse(loadProduct(id));
    }

    private Product loadProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + id));
    }
}
