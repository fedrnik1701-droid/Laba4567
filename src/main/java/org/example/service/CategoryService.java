package org.example.service;

import org.example.dto.CategoryRequest;
import org.example.entity.Category;
import org.example.exception.BusinessException;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Transactional
    public Category create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Category with name already exists: " + request.getName());
        }
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);
        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Category with name already exists: " + request.getName());
        }
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        if (!category.getBooks().isEmpty()) {
            throw new BusinessException("Cannot delete category that has books assigned");
        }
        categoryRepository.delete(category);
    }
}