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
            // Root Categories
            Category daoPhay = Category.builder().name("Dao Phay (End Mill)").build();
            Category chipTien = Category.builder().name("Chip Tiện (Insert)").build();
            Category muiKhoan = Category.builder().name("Mũi Khoan (Drill)").build();
            
            categoryRepository.saveAll(Arrays.asList(daoPhay, chipTien, muiKhoan));

            // Subcategories for Dao Phay
            Category daoPhayNgon = Category.builder().name("Dao Phay Ngón (Solid End Mill)").parent(daoPhay).build();
            Category daoPhayCau = Category.builder().name("Dao Phay Cầu (Ball Nose)").parent(daoPhay).build();
            
            // Subcategories for Chip Tiện
            Category chipTienNgoai = Category.builder().name("Chip Tiện Ngoài").parent(chipTien).build();
            Category chipTienTrong = Category.builder().name("Chip Tiện Trong").parent(chipTien).build();

            categoryRepository.saveAll(Arrays.asList(daoPhayNgon, daoPhayCau, chipTienNgoai, chipTienTrong));
            
            System.out.println("Hierarchical categories seeded successfully.");
        }
    }
}
