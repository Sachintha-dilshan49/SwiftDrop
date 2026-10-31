package lk.swiftdrop.parcel.parcel.repository;

import jakarta.persistence.LockModeType;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Parcel> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Parcel> findByCustomerIdAndStatus(UUID customerId, ParcelStatus status, Pageable pageable);

    Page<Parcel> findByStatus(ParcelStatus status, Pageable pageable);

    boolean existsByTrackingNo(String trackingNo);
}
