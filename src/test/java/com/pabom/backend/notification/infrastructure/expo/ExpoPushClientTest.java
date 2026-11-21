package com.pabom.backend.notification.infrastructure.expo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.pabom.backend.notification.application.port.PushSenderPort.PushMessage;
import com.pabom.backend.notification.application.port.PushSenderPort.Receipt;
import com.pabom.backend.notification.application.port.PushSenderPort.SendTicket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class ExpoPushClientTest {

    private static final String SEND_URL = "https://exp.host/--/api/v2/push/send";
    private static final String RECEIPTS_URL = "https://exp.host/--/api/v2/push/getReceipts";

    private MockRestServiceServer server;
    private List<Duration> sleeps;
    private ExpoPushClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        sleeps = new ArrayList<>();
        client = new ExpoPushClient(restTemplate,
                new ExpoPushProperties("expo-secret", null, 3, Duration.ofMillis(100), null, null),
                sleeps::add);
    }

    @Test
    void isConfiguredOnlyWithAccessToken() {
        assertThat(client.isConfigured()).isTrue();
        assertThat(new ExpoPushClient(new RestTemplate(),
                new ExpoPushProperties(" ", null, null, null, null, null), duration -> { })
                .isConfigured()).isFalse();
    }

    @Test
    void sendsMessagesWithBearerTokenAndCareChannelAndMapsTickets() {
        server.expect(once(), requestTo(SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer expo-secret"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[a]"))
                .andExpect(jsonPath("$[0].title").value("오늘의 케어 일정"))
                .andExpect(jsonPath("$[0].body").value("레오가 오늘 급여를 기다려요"))
                .andExpect(jsonPath("$[0].data.url").value("/home"))
                .andExpect(jsonPath("$[0].sound").value("default"))
                .andExpect(jsonPath("$[0].channelId").value("care"))
                .andRespond(withSuccess("""
                        {"data":[
                          {"status":"ok","id":"ticket-1"},
                          {"status":"error","message":"\\"ExponentPushToken[b]\\" is not a registered push notification recipient","details":{"error":"DeviceNotRegistered"}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<SendTicket> tickets = client.send(List.of(message("ExponentPushToken[a]"), message("ExponentPushToken[b]")));

        assertThat(tickets).hasSize(2);
        assertThat(tickets.get(0)).isEqualTo(SendTicket.ok("ticket-1"));
        assertThat(tickets.get(1).ok()).isFalse();
        assertThat(tickets.get(1).isDeviceNotRegistered()).isTrue();
        server.verify();
    }

    @Test
    void splitsIntoRequestsOfAtMostOneHundredMessages() {
        server.expect(once(), requestTo(SEND_URL))
                .andExpect(jsonPath("$.length()").value(100))
                .andRespond(withSuccess(okTickets(100), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(SEND_URL))
                .andExpect(jsonPath("$.length()").value(50))
                .andRespond(withSuccess(okTickets(50), MediaType.APPLICATION_JSON));

        List<SendTicket> tickets = client.send(IntStream.range(0, 150)
                .mapToObj(index -> message("ExponentPushToken[" + index + "]")).toList());

        assertThat(tickets).hasSize(150).allMatch(SendTicket::ok);
        server.verify();
    }

    @Test
    void retriesRateLimitAndServerErrorsWithExponentialBackoff() {
        server.expect(once(), requestTo(SEND_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(once(), requestTo(SEND_URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(once(), requestTo(SEND_URL))
                .andRespond(withSuccess(okTickets(1), MediaType.APPLICATION_JSON));

        List<SendTicket> tickets = client.send(List.of(message("ExponentPushToken[a]")));

        assertThat(tickets).singleElement().matches(SendTicket::ok);
        assertThat(sleeps).containsExactly(Duration.ofMillis(100), Duration.ofMillis(200));
        server.verify();
    }

    @Test
    void givesUpAfterMaxAttemptsAndReportsFailedTickets() {
        server.expect(times(3), requestTo(SEND_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        List<SendTicket> tickets = client.send(List.of(message("ExponentPushToken[a]"), message("ExponentPushToken[b]")));

        assertThat(tickets).hasSize(2).allSatisfy(ticket -> {
            assertThat(ticket.ok()).isFalse();
            assertThat(ticket.isDeviceNotRegistered()).isFalse();
        });
        assertThat(sleeps).hasSize(2);
        server.verify();
    }

    @Test
    void doesNotRetryOtherClientErrors() {
        server.expect(once(), requestTo(SEND_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        List<SendTicket> tickets = client.send(List.of(message("ExponentPushToken[a]")));

        assertThat(tickets).singleElement().matches(ticket -> !ticket.ok());
        assertThat(sleeps).isEmpty();
        server.verify();
    }

    @Test
    void readsReceiptsById() {
        server.expect(once(), requestTo(RECEIPTS_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer expo-secret"))
                .andExpect(jsonPath("$.ids[0]").value("ticket-1"))
                .andExpect(jsonPath("$.ids[1]").value("ticket-2"))
                .andRespond(withSuccess("""
                        {"data":{
                          "ticket-1":{"status":"ok"},
                          "ticket-2":{"status":"error","message":"gone","details":{"error":"DeviceNotRegistered"}}
                        }}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Receipt> receipts = client.getReceipts(List.of("ticket-1", "ticket-2", "ticket-3"));

        assertThat(receipts).containsOnlyKeys("ticket-1", "ticket-2");
        assertThat(receipts.get("ticket-1").ok()).isTrue();
        assertThat(receipts.get("ticket-2").isDeviceNotRegistered()).isTrue();
        server.verify();
    }

    private PushMessage message(String token) {
        return new PushMessage(token, "오늘의 케어 일정", "레오가 오늘 급여를 기다려요",
                Map.of("url", "/home"), "care");
    }

    private String okTickets(int count) {
        return "{\"data\":[" + String.join(",", IntStream.range(0, count)
                .mapToObj(index -> "{\"status\":\"ok\",\"id\":\"t-" + index + "\"}").toList()) + "]}";
    }
}
