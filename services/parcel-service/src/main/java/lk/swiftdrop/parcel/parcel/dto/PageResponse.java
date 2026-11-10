package lk.swiftdrop.parcel.parcel.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Page envelope from docs/conventions.md §1: {@code content}, {@code page}, {@code size}, {@code totalElements}. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
