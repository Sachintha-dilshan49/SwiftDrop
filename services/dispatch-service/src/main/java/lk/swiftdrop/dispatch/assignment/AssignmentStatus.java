package lk.swiftdrop.dispatch.assignment;

import java.util.Set;

public enum AssignmentStatus {
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    FAILED;

    /** Statuses that count as the courier still having the parcel. */
    public static final Set<AssignmentStatus> ACTIVE = Set.of(ASSIGNED, IN_PROGRESS);
}
