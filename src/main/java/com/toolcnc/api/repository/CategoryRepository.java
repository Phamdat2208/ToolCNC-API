package com.toolcnc.api.repository;

import com.toolcnc.api.dto.CategoryResponse;
import com.toolcnc.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT new com.toolcnc.api.dto.CategoryResponse(c.id, c.name, COUNT(p)) " +
           "FROM Category c LEFT JOIN Product p ON p.category = c " +
           "GROUP BY c.id, c.name")
    List<CategoryResponse> findAllWithProductCount();
}
