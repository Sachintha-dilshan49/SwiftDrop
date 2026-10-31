package lk.swiftdrop.parcel.parcel.controller;

import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Builds {@link Pageable}s for parcel lists from {@code page}, {@code size} and {@code sort} (docs/conventions.md §1). */
final class ParcelPageRequests {

    static final int MAX_SIZE = 100;
    static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "updatedAt", "status");

    private ParcelPageRequests() {
    }

    /**
     * @param page zero-based page number (validated by the controller)
     * @param size page size, capped at {@value #MAX_SIZE}
     * @param sort {@code field} or {@code field,asc|desc}; only {@link #SORTABLE_FIELDS} are allowed
     */
    static Pageable of(int page, int size, String sort) {
        return PageRequest.of(page, Math.min(size, MAX_SIZE), parseSort(sort));
    }

    private static Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (parts.length > 2 || !SORTABLE_FIELDS.contains(field)) {
            throw invalidSort();
        }
        if (parts.length == 1) {
            return Sort.by(Sort.Direction.ASC, field);
        }
        return switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.by(Sort.Direction.ASC, field);
            case "desc" -> Sort.by(Sort.Direction.DESC, field);
            default -> throw invalidSort();
        };
    }

    private static ValidationException invalidSort() {
        return new ValidationException("Unsupported sort.", List.of(new FieldErrorDetail("sort",
                "must be one of createdAt, updatedAt, status, optionally followed by ,asc or ,desc")));
    }
}
