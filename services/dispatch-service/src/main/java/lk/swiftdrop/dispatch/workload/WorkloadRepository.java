package lk.swiftdrop.dispatch.workload;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkloadRepository extends Repository<Courier, UUID> {

    /** Every courier with the number of their assignments in {@code statuses}, least busy first. */
    @Query("""
            select new lk.swiftdrop.dispatch.workload.CourierLoad(
                c.id, c.userId, c.status, count(a.id))
            from Courier c
            left join Assignment a on a.courierId = c.id and a.status in :statuses
            group by c.id, c.userId, c.status
            order by count(a.id), c.id
            """)
    List<CourierLoad> findWorkload(@Param("statuses") Collection<AssignmentStatus> statuses);
}
