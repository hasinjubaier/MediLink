package com.medilink.repository;

import com.medilink.model.medicine.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, String> {

    List<Medicine> findByBrandNameContainingIgnoreCase(String brandName);

    List<Medicine> findByGenericNameContainingIgnoreCase(String genericName);

    List<Medicine> findByGenericNameIgnoreCase(String genericName);

    @Query("SELECT m FROM Medicine m WHERE LOWER(m.brandName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(m.company) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(m.category) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Medicine> searchAll(@Param("query") String query);
}
