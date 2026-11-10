package lk.swiftdrop.dispatch.workload;

import java.util.ArrayList;
import java.util.List;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.AuthServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WorkloadService {

    private static final Logger log = LoggerFactory.getLogger(WorkloadService.class);

    private final WorkloadRepository workloadRepository;
    private final AuthServiceClient authServiceClient;

    public WorkloadService(WorkloadRepository workloadRepository, AuthServiceClient authServiceClient) {
        this.workloadRepository = workloadRepository;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Least busy first, with each courier's first name. Names are a nicety: if auth-service is down the list is
     * still returned, without names, and auth-service is not asked again for the remaining couriers.
     */
    public List<CourierWorkloadResponse> getWorkload() {
        List<CourierWorkloadResponse> workload = new ArrayList<>();
        boolean namesAvailable = true;
        for (CourierLoad load : workloadRepository.findWorkload(AssignmentStatus.ACTIVE)) {
            String firstName = null;
            if (namesAvailable) {
                try {
                    firstName = authServiceClient.firstName(load.userId()).orElse(null);
                } catch (ServiceUnavailableException e) {
                    log.warn("Courier names unavailable, showing workload without them");
                    namesAvailable = false;
                }
            }
            workload.add(CourierWorkloadResponse.from(load, firstName));
        }
        return workload;
    }
}
