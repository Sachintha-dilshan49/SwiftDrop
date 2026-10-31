package lk.swiftdrop.parcel.parcel.repository;

import lk.swiftdrop.parcel.parcel.entity.ParcelStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParcelStatusHistoryRepository extends JpaRepository<ParcelStatusHistory, UUID> {

    List<ParcelStatusHistory> findByParcelIdOrderByCreatedAtAsc(UUID parcelId);
}
