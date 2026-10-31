package lk.swiftdrop.parcel.parcel.repository;

import jakarta.persistence.LockModeType;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, UUID> {

    /** {@code SELECT ... FOR UPDATE}: the row stays locked until the surrounding transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Parcel p WHERE p.id = :id")
    Optional<Parcel> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByTrackingNo(String trackingNo);
}
