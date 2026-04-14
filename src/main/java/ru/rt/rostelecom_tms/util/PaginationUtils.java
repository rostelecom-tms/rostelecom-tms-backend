package ru.rt.rostelecom_tms.util;

import ru.rt.rostelecom_tms.dto.common.PageResponseDto;

import java.util.Collections;
import java.util.List;

public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static <T> PageResponseDto<T> paginate(List<T> source, Integer page, Integer size) {
        int normalizedPage = page == null || page < 0 ? 0 : page;
        int normalizedSize = size == null || size <= 0 ? 20 : Math.min(size, 200);

        int totalElementsInt = source == null ? 0 : source.size();
        long totalElements = totalElementsInt;
        int totalPages = totalElementsInt == 0 ? 0 : (int) Math.ceil((double) totalElementsInt / normalizedSize);

        int fromIndex = normalizedPage * normalizedSize;
        if (fromIndex >= totalElementsInt || source == null || source.isEmpty()) {
            return new PageResponseDto<>(
                    Collections.emptyList(),
                    normalizedPage,
                    normalizedSize,
                    totalElements,
                    totalPages,
                    normalizedPage == 0,
                    totalPages == 0 || normalizedPage >= totalPages - 1
            );
        }

        int toIndex = Math.min(fromIndex + normalizedSize, totalElementsInt);
        List<T> content = source.subList(fromIndex, toIndex);

        return new PageResponseDto<>(
                content,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages,
                normalizedPage == 0,
                normalizedPage >= totalPages - 1
        );
    }
}
