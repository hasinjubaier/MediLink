package com.medilink.repository;

import com.medilink.model.market.MedicinePriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicinePriceHistoryRepository extends JpaRepository<MedicinePriceHistory, String> {

    List<MedicinePriceHistory> findByMedicineIdOrderByTimestampDesc(String medicineId);

    List<MedicinePriceHistory> findTop20ByOrderByTimestampDesc();

    List<MedicinePriceHistory> findAllByOrderByTimestampDesc();
}
