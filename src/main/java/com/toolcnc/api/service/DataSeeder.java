package com.toolcnc.api.service;

import com.toolcnc.api.model.Category;
import com.toolcnc.api.model.Product;
import com.toolcnc.api.repository.CategoryRepository;
import com.toolcnc.api.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            System.out.println("Seeding Categories...");
            Category c1 = Category.builder().name("Máy Phay CNC").description("Các loại máy phay đứng, ngang").build();
            Category c2 = Category.builder().name("Máy Tiện CNC").description("Các loại máy tiện").build();
            Category c3 = Category.builder().name("Dao Cụ Cắt Gọt").description("Các loại dao phay, dao tiện, mũi khoan").build();
            
            categoryRepository.saveAll(Arrays.asList(c1, c2, c3));

            System.out.println("Seeding Products...");
            List<Product> products = Arrays.asList(
                    Product.builder().category(c1).name("Máy Phay CNC Haas VF-2")
                            .sku("HAAS-VF2-01").price(new BigDecimal("1500000000")).brand("Haas")
                            .stock(5).imageUrl("https://placehold.co/400x300?text=Haas+VF-2").build(),
                            
                    Product.builder().category(c1).name("Máy Phay CNC Mazak VCN-530C")
                            .sku("MAZAK-VCN-01").price(new BigDecimal("2100000000")).brand("Mazak")
                            .stock(2).imageUrl("https://placehold.co/400x300?text=Mazak+VCN-530C").build(),
                            
                    Product.builder().category(c2).name("Máy Tiện CNC Haas ST-20")
                            .sku("HAAS-ST20-01").price(new BigDecimal("1200000000")).brand("Haas")
                            .stock(3).imageUrl("https://placehold.co/400x300?text=Haas+ST-20").build(),
                            
                    Product.builder().category(c3).name("Dao Phay Ngón Hợp Kim Sandvik 12mm")
                            .sku("SANDVIK-M12").price(new BigDecimal("850000")).brand("Sandvik")
                            .stock(150).imageUrl("https://placehold.co/400x300?text=Dao+Phay+Sandvik").build(),
                            
                    Product.builder().category(c3).name("Mảnh Tiện CCMT 09T304 Mitsubishi")
                            .sku("MIT-CCMT09").price(new BigDecimal("120000")).brand("Mitsubishi")
                            .stock(1000).imageUrl("https://placehold.co/400x300?text=Mảnh+Tiện+Mitsubishi").build()
            );
            
            productRepository.saveAll(products);
            System.out.println("Seeding completed!");
        }
    }
}
