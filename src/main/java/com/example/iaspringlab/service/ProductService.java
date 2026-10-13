package com.example.iaspringlab.service;

import com.example.iaspringlab.domain.Product;
import com.example.iaspringlab.dto.CreateProductRequest;
import com.example.iaspringlab.dto.DtoMapper;
import com.example.iaspringlab.dto.ProductResponse;
import com.example.iaspringlab.dto.UpdateProductStatusRequest;
import com.example.iaspringlab.exception.DuplicateResourceException;
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
            throw new DuplicateResourceException("Un produit existe déjà avec la référence : " + request.reference());
        }
        Product product = new Product(request.reference(), request.name(), request.price());
        return DtoMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long id, UpdateProductStatusRequest request) {
        Product product = load(id);
        if (Boolean.TRUE.equals(request.active())) {
            product.activate();
        } else {
            product.deactivate();
        }
        return DtoMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return DtoMapper.toProductResponse(load(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(DtoMapper::toProductResponse)
                .toList();
    }

    private Product load(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + id));
    }
}
