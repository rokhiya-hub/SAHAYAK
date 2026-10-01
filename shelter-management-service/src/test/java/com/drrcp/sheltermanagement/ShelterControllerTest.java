package com.drrcp.sheltermanagement;

import com.drrcp.sheltermanagement.controller.ShelterController;
import com.drrcp.sheltermanagement.domain.ShelterStatus;
import com.drrcp.sheltermanagement.dto.ShelterPageResponse;
import com.drrcp.sheltermanagement.dto.ShelterResponse;
import com.drrcp.sheltermanagement.exception.ResourceNotFoundException;
import com.drrcp.sheltermanagement.exception.ShelterFullException;
import com.drrcp.sheltermanagement.service.ShelterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShelterController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShelterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShelterService shelterService;

    private ShelterResponse response() {
        return ShelterResponse.builder()
                .id(1L)
                .name("Central Community Shelter")
                .address("MG Road, Bengaluru")
                .latitude(new BigDecimal("12.9716"))
                .longitude(new BigDecimal("77.5946"))
                .totalCapacity(200)
                .currentOccupancy(87)
                .availableCapacity(113)
                .status(ShelterStatus.OPEN)
                .resourcesAvailable(List.of("Food", "Water"))
                .build();
    }

    private ShelterPageResponse page() {
        return ShelterPageResponse.builder()
                .content(List.of(response()))
                .page(1)
                .size(1)
                .totalElements(13)
                .totalPages(13)
                .first(false)
                .last(true)
                .build();
    }

    @Test
    void getAllShelters_returnsPagedResponse() throws Exception {
        when(shelterService.getAllShelters(isNull(), isNull(), any(Pageable.class))).thenReturn(page());

        mockMvc.perform(get("/api/shelters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Central Community Shelter"))
                .andExpect(jsonPath("$.totalElements").value(13))
                .andExpect(jsonPath("$.totalPages").value(13))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getAllShelters_forwardsSearchAndStatusFilters() throws Exception {
        when(shelterService.getAllShelters("mg road", ShelterStatus.OPEN, Pageable.ofSize(5)))
                .thenReturn(page());

        mockMvc.perform(get("/api/shelters")
                        .param("search", "mg road")
                        .param("status", "OPEN")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(shelterService).getAllShelters(eq("mg road"), eq(ShelterStatus.OPEN), any(Pageable.class));
    }

    @Test
    void getAllShelters_forwardsPagingAndSorting() throws Exception {
        when(shelterService.getAllShelters(isNull(), isNull(), any(Pageable.class))).thenReturn(page());

        mockMvc.perform(get("/api/shelters")
                        .param("page", "2")
                        .param("size", "25")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk());

        verify(shelterService).getAllShelters(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getShelterById_returnsShelter() throws Exception {
        when(shelterService.getShelterById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/shelters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getShelterById_whenMissing_returns404() throws Exception {
        when(shelterService.getShelterById(99L))
                .thenThrow(new ResourceNotFoundException("Shelter with id 99 not found"));

        mockMvc.perform(get("/api/shelters/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Shelter with id 99 not found"))
                .andExpect(jsonPath("$.path").value("/api/shelters/99"));
    }

    @Test
    void createShelter_returns201() throws Exception {
        when(shelterService.createShelter(any())).thenReturn(response());

        mockMvc.perform(post("/api/shelters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Central Community Shelter","address":"MG Road, Bengaluru",
                                 "latitude":12.9716,"longitude":77.5946,"totalCapacity":200,
                                 "resourcesAvailable":["Food","Water"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createShelter_withInvalidBody_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/shelters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","address":"","latitude":91.0,"longitude":181.0,"totalCapacity":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Shelter name is required"))
                .andExpect(jsonPath("$.fieldErrors.address").value("Address is required"))
                .andExpect(jsonPath("$.fieldErrors.latitude").exists())
                .andExpect(jsonPath("$.fieldErrors.longitude").exists())
                .andExpect(jsonPath("$.fieldErrors.totalCapacity").value("Total capacity must be at least 1"));
    }

    @Test
    void updateShelter_returns200() throws Exception {
        when(shelterService.updateShelter(eq(1L), any())).thenReturn(response());

        mockMvc.perform(put("/api/shelters/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Central Community Shelter","address":"MG Road, Bengaluru",
                                 "latitude":12.9716,"longitude":77.5946,"totalCapacity":200,
                                 "currentOccupancy":87,"resourcesAvailable":["Food"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteShelter_returns204() throws Exception {
        mockMvc.perform(delete("/api/shelters/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void checkIn_returns200() throws Exception {
        when(shelterService.checkIn(1L)).thenReturn(response());

        mockMvc.perform(post("/api/shelters/1/checkin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCapacity").value(113));
    }

    @Test
    void checkOut_returns200() throws Exception {
        when(shelterService.checkOut(1L)).thenReturn(response());

        mockMvc.perform(post("/api/shelters/1/checkout"))
                .andExpect(status().isOk());
    }

    @Test
    void checkIn_whenFull_returns409() throws Exception {
        when(shelterService.checkIn(2L))
                .thenThrow(new ShelterFullException("Shelter 2 is at full capacity"));

        mockMvc.perform(post("/api/shelters/2/checkin"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Shelter 2 is at full capacity"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void checkIn_whenClosed_returns409() throws Exception {
        when(shelterService.checkIn(3L))
                .thenThrow(new IllegalStateException("Shelter is closed and cannot accept new residents"));

        mockMvc.perform(post("/api/shelters/3/checkin"))
                .andExpect(status().isConflict());
    }

    @Test
    void getAvailableShelters_forwardsQueryParams() throws Exception {
        when(shelterService.getAvailableShelters(50, 12.9716, 77.5946, 25.0))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/shelters/available")
                        .param("minCapacity", "50")
                        .param("lat", "12.9716")
                        .param("lng", "77.5946")
                        .param("radiusKm", "25.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(shelterService).getAvailableShelters(50, 12.9716, 77.5946, 25.0);
    }

    @Test
    void getAvailableShelters_defaultsWhenParamsMissing() throws Exception {
        when(shelterService.getAvailableShelters(0, null, null, 100.0))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/shelters/available"))
                .andExpect(status().isOk());

        verify(shelterService).getAvailableShelters(0, null, null, 100.0);
    }
}
