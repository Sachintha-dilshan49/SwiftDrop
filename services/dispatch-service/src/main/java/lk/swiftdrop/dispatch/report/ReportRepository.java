package lk.swiftdrop.dispatch.report;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.dispatch.courier.Courier;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends Repository<Courier, UUID> {

    /**
     * Assignments per Sri Lanka day and courier: assigned on the day they were assigned, delivered or failed on the
     * day they were closed (an assignment's last change is closing it).
     */
    @Query(nativeQuery = true, value = """
            select day, courier_id as courierId, sum(assigned) as assigned, sum(delivered) as delivered,
                   sum(failed) as failed
            from (
                select (assigned_at at time zone 'Asia/Colombo')::date as day, courier_id,
                       1 as assigned, 0 as delivered, 0 as failed
                from assignments
                where assigned_at >= :from and assigned_at < :to
                union all
                select (updated_at at time zone 'Asia/Colombo')::date, courier_id, 0,
                       case when status = 'COMPLETED' then 1 else 0 end,
                       case when status = 'FAILED' then 1 else 0 end
                from assignments
                where status in ('COMPLETED', 'FAILED') and updated_at >= :from and updated_at < :to
            ) counts
            group by day, courier_id
            """)
    List<DayCourierCount> countByDayAndCourier(@Param("from") Instant from, @Param("to") Instant to);

    /** Couriers' auth user ids, for their names. */
    @Query("select c.id as courierId, c.userId as userId from Courier c where c.id in :courierIds")
    List<CourierUser> findUsers(@Param("courierIds") List<UUID> courierIds);

    interface DayCourierCount {
        java.time.LocalDate getDay();

        UUID getCourierId();

        long getAssigned();

        long getDelivered();

        long getFailed();
    }

    interface CourierUser {
        UUID getCourierId();

        UUID getUserId();
    }
}
