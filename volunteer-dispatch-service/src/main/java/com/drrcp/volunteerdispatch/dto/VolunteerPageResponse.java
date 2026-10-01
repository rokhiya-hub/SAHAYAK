package com.drrcp.volunteerdispatch.dto;

import com.drrcp.volunteerdispatch.domain.Volunteer;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class VolunteerPageResponse {
    private List<VolunteerResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static VolunteerPageResponse from(Page<Volunteer> page) {
        return VolunteerPageResponse.builder()
                .content(page.getContent().stream().map(VolunteerResponse::from).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}