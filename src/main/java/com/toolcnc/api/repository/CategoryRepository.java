package com.toolcnc.api.repository;

import com.toolcnc.api.dto.CategoryResponse;
import com.toolcnc.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT new com.toolcnc.api.dto.CategoryResponse(c.id, c.name, cp.id, COUNT(p)) " +
           "FROM Category c " +
           "LEFT JOIN c.parent cp " +
           "LEFT JOIN Product p ON p.category = c AND p.isActive = true " +
           "GROUP BY c.id, c.name, cp.id")
    List<CategoryResponse> findAllWithProductCount();

    /** Tìm category theo tên, không phân biệt hoa thường */
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<Category> findByNameIgnoreCase(@Param("name") String name);

    /** Lấy danh sách ID của tất cả category con trực tiếp của một category cha */
    @Query("SELECT c.id FROM Category c WHERE c.parent.id = :parentId")
    List<Long> findChildIdsByParentId(@Param("parentId") Long parentId);
}
