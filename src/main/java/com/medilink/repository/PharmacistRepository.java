package com.medilink.repository;

import com.medilink.model.user.Pharmacist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PharmacistRepository extends JpaRepository<Pharmacist, String> {
    Optional<Pharmacist> findByEmailIgnoreCase(String email);
}
