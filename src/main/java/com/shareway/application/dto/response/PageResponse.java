package com.shareway.application.dto.response;
import lombok.*; import org.springframework.data.domain.Page; import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages, currentPage, size;
    private boolean first, last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
            .content(page.getContent()).totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages()).currentPage(page.getNumber())
            .size(page.getSize()).first(page.isFirst()).last(page.isLast()).build();
    }
}
