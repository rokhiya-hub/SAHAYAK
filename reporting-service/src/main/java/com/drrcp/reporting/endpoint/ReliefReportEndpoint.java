package com.drrcp.reporting.endpoint;

import com.drrcp.reporting.generated.ObjectFactory;
import com.drrcp.reporting.generated.ReliefStatusReportRequestType;
import com.drrcp.reporting.generated.ReliefStatusReportResponseType;
import com.drrcp.reporting.service.ReportAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import jakarta.xml.bind.JAXBElement;

@Endpoint
@RequiredArgsConstructor
public class ReliefReportEndpoint {

    private static final String NAMESPACE = "http://drrcp.com/reporting";

    private final ReportAggregationService reportAggregationService;
    private final ObjectFactory objectFactory = new ObjectFactory();

    @PayloadRoot(namespace = NAMESPACE, localPart = "ReliefStatusReportRequest")
    @ResponsePayload
    public JAXBElement<ReliefStatusReportResponseType> report(
            @RequestPayload JAXBElement<ReliefStatusReportRequestType> request) {
        return objectFactory.createReliefStatusReportResponse(
                reportAggregationService.generate(request.getValue(), bearerToken()));
    }

    private String bearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        throw new IllegalStateException("A valid bearer token is required for relief reporting");
    }
}