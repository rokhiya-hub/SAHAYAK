package com.drrcp.sheltermanagement.dto;

import com.drrcp.sheltermanagement.domain.Shelter;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class ShelterPageResponse {
    private List<ShelterResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static ShelterPageResponse from(Page<Shelter> page) {
        return ShelterPageResponse.builder()
                .content(page.getContent().stream().map(ShelterResponse::from).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}