package com.medilink.repository;

import com.medilink.model.pharmacy.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, String> {

    @Query("SELECT p FROM Pharmacy p WHERE p.hasEmergencyDelivery IS NULL OR p.hasEmergencyDelivery = true")
    List<Pharmacy> findEmergencyPharmacies();

    List<Pharmacy> findByHasEmergencyDeliveryTrue();
    List<Pharmacy> findByIs24HoursTrue();
}
