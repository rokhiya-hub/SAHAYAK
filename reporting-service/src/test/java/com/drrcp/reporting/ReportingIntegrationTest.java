package com.drrcp.reporting;

import com.drrcp.reporting.domain.AuditLog;
import com.drrcp.reporting.repository.AuditLogRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
@Import(AuditProbeService.class)
class ReportingIntegrationTest {

    private static final MockWebServer VICTIM_SERVER = startServer();
    private static final MockWebServer SHELTER_SERVER = startServer();
    private static final MockWebServer RESOURCE_SERVER = startServer();
    private static final MockWebServer VOLUNTEER_SERVER = startServer();

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditProbeService auditProbeService;

    @DynamicPropertySource
    static void upstreamUrls(DynamicPropertyRegistry registry) {
        registry.add("reporting.services.victims-url", () -> VICTIM_SERVER.url("/").toString());
        registry.add("reporting.services.shelters-url", () -> SHELTER_SERVER.url("/").toString());
        registry.add("reporting.services.resources-url", () -> RESOURCE_SERVER.url("/").toString());
        registry.add("reporting.services.volunteers-url", () -> VOLUNTEER_SERVER.url("/").toString());
        registry.add("reporting.response-timeout", () -> "4s");
    }

    @BeforeEach
    void clearAudit() {
        auditLogRepository.deleteAll();
    }

    @AfterAll
    static void stopUpstreams() throws IOException {
        VICTIM_SERVER.shutdown();
        SHELTER_SERVER.shutdown();
        RESOURCE_SERVER.shutdown();
        VOLUNTEER_SERVER.shutdown();
    }

    @Test
    void wsdlRequiresJwtAndIsPublishedAtContractUrl() {
        ResponseEntity<String> anonymous = restTemplate.getForEntity(url("/ws/relief-report.wsdl"), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, anonymous.getStatusCode());

        ResponseEntity<String> authenticated = restTemplate.exchange(url("/ws/relief-report.wsdl"),
                HttpMethod.GET, authorized("USER"), String.class);
        assertEquals(HttpStatus.OK, authenticated.getStatusCode());
        assertTrue(authenticated.getBody().contains("ReliefStatusReportRequest"));
    }

    @Test
    void soapReportAggregatesAllSourcesInParallel() throws Exception {
        enqueuePage(VICTIM_SERVER, """
                {"totalPages":1,"content":[
                  {"currentShelterId":7,"registeredAt":"2026-06-12T10:00:00"},
                  {"currentShelterId":7,"registeredAt":"2026-06-20T10:00:00"},
                  {"currentShelterId":7,"registeredAt":"2026-07-02T10:00:00"}]}
                """);
        enqueuePage(SHELTER_SERVER, """
                {"totalPages":1,"content":[
                  {"id":7,"name":"North district shelter","address":"North district","status":"OPEN","currentOccupancy":8},
                  {"id":8,"name":"North district closed shelter","address":"North district","status":"CLOSED","currentOccupancy":99}]}
                """);
        enqueuePage(RESOURCE_SERVER, """
                {"totalPages":1,"content":[{"id":20,"name":"Rice"},{"id":21,"name":"Blankets"}]}
                """);
        enqueuePage(VOLUNTEER_SERVER, """
                {"totalPages":1,"content":[{"id":1,"availabilityStatus":"DISPATCHED"},
                                            {"id":2,"availabilityStatus":"DISPATCHED"}]}
                """);
        RESOURCE_SERVER.enqueue(json("""
                [{"shelterId":7,"quantityAllocated":40,"allocatedAt":"2026-06-14T10:00:00","status":"APPROVED"},
                 {"shelterId":7,"quantityAllocated":10,"allocatedAt":"2026-06-16T10:00:00","status":"REJECTED"}]
                """));
        RESOURCE_SERVER.enqueue(json("""
                [{"shelterId":8,"quantityAllocated":5,"allocatedAt":"2026-06-14T10:00:00","status":"FULFILLED"}]
                """));

        String soapRequest = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:rep="http://drrcp.com/reporting">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <rep:ReliefStatusReportRequest>
                      <rep:districtName>North district</rep:districtName>
                      <rep:fromDate>2026-06-10</rep:fromDate>
                      <rep:toDate>2026-06-30</rep:toDate>
                    </rep:ReliefStatusReportRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.setBearerAuth(token("USER"));
        long startedAt = System.nanoTime();
        ResponseEntity<String> response = restTemplate.postForEntity(url("/ws"),
                new HttpEntity<>(soapRequest, headers), String.class);
        long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertEquals(List.of("2"), xmlValues(response.getBody(), "totalVictimsRegistered"));
        assertEquals(List.of("1"), xmlValues(response.getBody(), "totalSheltersActive"));
        assertEquals(List.of("8"), xmlValues(response.getBody(), "totalOccupancy"));
        assertEquals(List.of("2"), xmlValues(response.getBody(), "volunteersDispatched"));
        assertEquals(List.of("Rice"), xmlValues(response.getBody(), "resourceName"));
        assertFalse(xmlValues(response.getBody(), "quantityAllocated").contains("10"));
        assertTrue(elapsedMs < 3000, "four delayed source calls should overlap; elapsed=" + elapsedMs + "ms");
    }

    private List<String> xmlValues(String xml, String localName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        NodeList elements = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)))
                .getElementsByTagNameNS("http://drrcp.com/reporting", localName);
        List<String> values = new ArrayList<>();
        for (int index = 0; index < elements.getLength(); index++) {
            values.add(elements.item(index).getTextContent());
        }
        return values;
    }

    @Test
    void auditAspectPersistsOutcomeAndRedactsSensitiveValues() {
        auditProbeService.register(Map.of("phone", "9876543210", "action", "register"));

        AuditLog log = auditLogRepository.findAll().getFirst();
        assertTrue(log.getAction().contains("register"));
        assertEquals("anonymous", log.getPerformedBy());
        assertTrue(log.isSuccess());
        assertTrue(log.getDetails().contains("durationMs="));
        assertFalse(log.getDetails().contains("9876543210"));
    }

    @Test
    void auditEndpointRequiresAdminRole() {
        ResponseEntity<String> userResponse = restTemplate.exchange(url("/api/audit-log"), HttpMethod.GET,
                authorized("USER"), String.class);
        assertEquals(HttpStatus.FORBIDDEN, userResponse.getStatusCode());

        ResponseEntity<String> adminResponse = restTemplate.exchange(url("/api/audit-log"), HttpMethod.GET,
                authorized("ADMIN"), String.class);
        assertEquals(HttpStatus.OK, adminResponse.getStatusCode());
    }

    private HttpEntity<Void> authorized(String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token(role));
        return new HttpEntity<>(headers);
    }

    private String token(String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("integration-test")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("role", role)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private void enqueuePage(MockWebServer server, String body) {
        server.enqueue(json(body).setHeadersDelay(700, TimeUnit.MILLISECONDS));
    }

    private MockResponse json(String body) {
        return new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody(body);
    }

    private static MockWebServer startServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
            return server;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}