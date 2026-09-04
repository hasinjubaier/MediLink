package com.medilink.repository;

import com.medilink.model.pharmacy.PharmacyStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyStockRepository extends JpaRepository<PharmacyStock, String> {

    List<PharmacyStock> findByMedicineId(String medicineId);

    List<PharmacyStock> findByPharmacyId(String pharmacyId);

    Optional<PharmacyStock> findByPharmacyIdAndMedicineId(String pharmacyId, String medicineId);

    @Query("SELECT s FROM PharmacyStock s WHERE LOWER(s.medicineId) = LOWER(:med) " +
           "OR LOWER(s.medicineBrandName) LIKE LOWER(CONCAT('%', :med, '%'))")
    List<PharmacyStock> searchStocksByMedicine(@Param("med") String med);
}
