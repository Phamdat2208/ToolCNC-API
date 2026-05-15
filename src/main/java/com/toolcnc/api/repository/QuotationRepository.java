package com.toolcnc.api.repository;

import com.toolcnc.api.model.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByCustomerEmailOrderByCreatedAtDesc(String email);
}
