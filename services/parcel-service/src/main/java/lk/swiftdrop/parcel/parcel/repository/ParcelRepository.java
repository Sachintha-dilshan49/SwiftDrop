package lk.swiftdrop.parcel.parcel.repository;

import lk.swiftdrop.parcel.parcel.entity.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, UUID> {

    boolean existsByTrackingNo(String trackingNo);
}
