package lk.swiftdrop.dispatch.workload;

import java.util.List;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkloadService {

    private final WorkloadRepository workloadRepository;

    public WorkloadService(WorkloadRepository workloadRepository) {
        this.workloadRepository = workloadRepository;
    }

    @Transactional(readOnly = true)
    public List<CourierWorkloadResponse> getWorkload() {
        return workloadRepository.findWorkload(AssignmentStatus.ACTIVE);
    }
}
