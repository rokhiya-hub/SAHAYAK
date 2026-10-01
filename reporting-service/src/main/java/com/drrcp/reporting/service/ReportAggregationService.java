package com.drrcp.reporting.service;

import com.drrcp.reporting.dto.AllocationSnapshot;
import com.drrcp.reporting.dto.PageResponse;
import com.drrcp.reporting.dto.ResourceSnapshot;
import com.drrcp.reporting.dto.ShelterSnapshot;
import com.drrcp.reporting.dto.VictimSnapshot;
import com.drrcp.reporting.dto.VolunteerSnapshot;
import com.drrcp.reporting.exception.ReportingAggregationException;
import com.drrcp.reporting.generated.ObjectFactory;
import com.drrcp.reporting.generated.ReliefStatusReportRequestType;
import com.drrcp.reporting.generated.ReliefStatusReportResponseType;
import com.drrcp.reporting.generated.ResourceAllocationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.net.URI;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ReportAggregationService {

    private final ObjectMapper objectMapper;
    private final ObjectFactory objectFactory = new ObjectFactory();

    @Qualifier("victimClient")
    private final WebClient victimClient;
    @Qualifier("shelterClient")
    private final WebClient shelterClient;
    @Qualifier("resourceClient")
    private final WebClient resourceClient;
    @Qualifier("volunteerClient")
    private final WebClient volunteerClient;

    @Value("${reporting.page-size}")
    private int pageSize;

    public ReliefStatusReportResponseType generate(ReliefStatusReportRequestType request, String bearerToken) {
        LocalDate fromDate = toLocalDate(request.getFromDate());
        LocalDate toDate = toLocalDate(request.getToDate());
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must not be after toDate");
        }

        String district = request.getDistrictName().trim();
        Mono<List<VictimSnapshot>> victims = fetchAll(victimClient, "/api/victims", VictimSnapshot.class,
                bearerToken, null, null);
        Mono<List<ShelterSnapshot>> shelters = fetchAll(shelterClient, "/api/shelters", ShelterSnapshot.class,
                bearerToken, "search", district);
        Mono<List<ResourceSnapshot>> resources = fetchAll(resourceClient, "/api/resources", ResourceSnapshot.class,
                bearerToken, null, null);
        Mono<List<VolunteerSnapshot>> volunteers = fetchAll(volunteerClient, "/api/volunteers",
                VolunteerSnapshot.class, bearerToken, "availabilityStatus", "DISPATCHED");

        return Mono.zip(victims, shelters, resources, volunteers)
                .flatMap(data -> fetchAllocations(data.getT3(), bearerToken)
                        .map(allocations -> buildResponse(request, fromDate, toDate,
                                data.getT1(), data.getT2(), allocations, data.getT4())))
                .onErrorMap(WebClientRequestException.class, ex ->
                        new ReportingAggregationException("A reporting dependency is unavailable or timed out", ex))
                .onErrorMap(WebClientResponseException.class, ex ->
                        new ReportingAggregationException("A reporting dependency returned HTTP "
                                + ex.getStatusCode().value(), ex))
                .block();
    }

    private <T> Mono<List<T>> fetchAll(WebClient client, String path, Class<T> itemType,
                                      String bearerToken, String filterName, String filterValue) {
        return fetchPage(client, path, itemType, bearerToken, filterName, filterValue, 0)
                .flatMap(first -> {
                    if (first.totalPages() <= 1) {
                        return Mono.just(first.content());
                    }
                    return Flux.range(1, first.totalPages() - 1)
                            .flatMap(page -> fetchPage(client, path, itemType, bearerToken,
                                    filterName, filterValue, page).map(PageData::content))
                            .collectList()
                            .map(rest -> {
                                List<T> all = new ArrayList<>(first.content());
                                rest.forEach(all::addAll);
                                return all;
                            });
                });
    }

    private <T> Mono<PageData<T>> fetchPage(WebClient client, String path, Class<T> itemType,
                                            String bearerToken, String filterName, String filterValue, int page) {
        return client.get()
                .uri(uriBuilder -> pageUri(uriBuilder, path, filterName, filterValue, page))
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.createException())
                .bodyToMono(JsonNode.class)
                .map(json -> parsePage(json, itemType))
                .onErrorMap(WebClientRequestException.class, ex -> ex)
                .onErrorMap(WebClientResponseException.class, ex -> ex);
    }

    private URI pageUri(UriBuilder uriBuilder, String path, String filterName, String filterValue, int page) {
        uriBuilder.path(path).queryParam("page", page).queryParam("size", pageSize);
        if (filterName != null) {
            uriBuilder.queryParam(filterName, filterValue);
        }
        if (path.endsWith("/shelters")) {
            uriBuilder.queryParam("sort", "name,asc");
        } else if (path.endsWith("/victims")) {
            uriBuilder.queryParam("sort", "registeredAt,asc");
        } else if (path.endsWith("/resources")) {
            uriBuilder.queryParam("sort", "name,asc");
        } else {
            uriBuilder.queryParam("sort", "name,asc");
        }
        return uriBuilder.build();
    }

    private <T> PageData<T> parsePage(JsonNode json, Class<T> itemType) {
        List<T> content = new ArrayList<>();
        JsonNode items = json.path("content");
        if (items.isArray()) {
            for (JsonNode item : items) {
                content.add(objectMapper.convertValue(item, itemType));
            }
        }
        return new PageData<>(content, json.path("totalPages").asInt(1));
    }

    private Mono<List<AllocationSnapshot>> fetchAllocations(List<ResourceSnapshot> resources, String bearerToken) {
        return Flux.fromIterable(resources)
                .flatMap(resource -> resourceClient.get()
                        .uri("/api/resources/{id}/allocations", resource.getId())
                        .headers(headers -> headers.setBearerAuth(bearerToken))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response -> response.createException())
                        .bodyToFlux(AllocationSnapshot.class)
                        .filter(allocation -> allocation.getStatus() == null
                                || !"REJECTED".equalsIgnoreCase(allocation.getStatus()))
                        .map(allocation -> {
                            allocation.setResourceName(resource.getName());
                            return allocation;
                        }))
                .collectList();
    }

    private ReliefStatusReportResponseType buildResponse(ReliefStatusReportRequestType request,
                                                         LocalDate fromDate, LocalDate toDate,
                                                         List<VictimSnapshot> victims,
                                                         List<ShelterSnapshot> shelters,
                                                         List<AllocationSnapshot> allocations,
                                                         List<VolunteerSnapshot> volunteers) {
        List<ShelterSnapshot> activeShelters = shelters.stream()
                .filter(shelter -> shelter.getStatus() != null
                        && !"CLOSED".equalsIgnoreCase(shelter.getStatus()))
                .toList();
        Set<Long> shelterIds = activeShelters.stream().map(ShelterSnapshot::getId).collect(java.util.stream.Collectors.toSet());

        long victimCount = victims.stream()
                .filter(victim -> victim.getRegisteredAt() != null
                        && within(victim.getRegisteredAt(), fromDate, toDate)
                        && victim.getCurrentShelterId() != null
                        && shelterIds.contains(victim.getCurrentShelterId()))
                .count();
        long occupancy = activeShelters.stream()
                .map(ShelterSnapshot::getCurrentOccupancy)
                .filter(value -> value != null)
                .mapToLong(Integer::longValue)
                .sum();

        ReliefStatusReportResponseType response = objectFactory.createReliefStatusReportResponseType();
        response.setTotalVictimsRegistered(victimCount);
        response.setTotalSheltersActive(activeShelters.size());
        response.setTotalOccupancy(occupancy);
        response.setVolunteersDispatched(volunteers.size());
        response.setGeneratedAt(nowXmlDateTime());

        allocations.stream()
                .filter(allocation -> allocation.getShelterId() != null
                        && shelterIds.contains(allocation.getShelterId()))
                .filter(allocation -> allocation.getAllocatedAt() != null
                        && within(allocation.getAllocatedAt(), fromDate, toDate))
                .forEach(allocation -> {
                    ResourceAllocationType line = objectFactory.createResourceAllocationType();
                    line.setResourceName(allocation.getResourceName());
                    line.setQuantityAllocated(allocation.getQuantityAllocated() == null
                            ? 0 : allocation.getQuantityAllocated());
                    line.setShelterId(allocation.getShelterId());
                    response.getResourceAllocations().add(line);
                });
        return response;
    }

    private boolean within(LocalDateTime timestamp, LocalDate fromDate, LocalDate toDate) {
        LocalDate date = timestamp.toLocalDate();
        return !date.isBefore(fromDate) && !date.isAfter(toDate);
    }

    private LocalDate toLocalDate(XMLGregorianCalendar date) {
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    private XMLGregorianCalendar nowXmlDateTime() {
        try {
            GregorianCalendar now = GregorianCalendar.from(java.time.ZonedDateTime.now(ZoneId.systemDefault()));
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(now);
        } catch (DatatypeConfigurationException ex) {
            throw new IllegalStateException("Unable to create report timestamp", ex);
        }
    }

    private record PageData<T>(List<T> content, int totalPages) {
    }
}