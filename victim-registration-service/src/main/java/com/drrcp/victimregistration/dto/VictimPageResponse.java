package com.drrcp.victimregistration.dto;

import com.drrcp.victimregistration.domain.Victim;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class VictimPageResponse {
    private List<VictimResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static VictimPageResponse from(Page<Victim> page) {
        return VictimPageResponse.builder()
                .content(page.getContent().stream().map(VictimResponse::from).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}