package com.toolcnc.api.service;

import com.toolcnc.api.model.Category;
import com.toolcnc.api.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            List<Category> categories = Arrays.asList(
                Category.builder().name("Máy Phay CNC").description("Danh mục máy phay").build(),
                Category.builder().name("Máy Tiện CNC").description("Danh mục máy tiện").build(),
                Category.builder().name("Dao Cụ Cắt Gọt").description("Danh mục dao cụ").build(),
                Category.builder().name("Phụ kiện kẹp chặt").description("Danh mục phụ kiện kẹp").build(),
                Category.builder().name("Dầu mỡ công nghiệp").description("Danh mục dầu mỡ").build(),
                Category.builder().name("Dụng cụ đo kiểm").description("Danh mục dụng cụ đo").build()
            );
            categoryRepository.saveAll(categories);
            System.out.println("Categories seeded successfully.");
        }
    }
}
