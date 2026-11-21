package com.pabom.backend.notification.infrastructure.expo;

import com.pabom.backend.notification.application.port.PushSenderPort;
import com.pabom.backend.notification.infrastructure.expo.ExpoPushDtos.ApiError;
import com.pabom.backend.notification.infrastructure.expo.ExpoPushDtos.Message;
import com.pabom.backend.notification.infrastructure.expo.ExpoPushDtos.ReceiptsRequest;
import com.pabom.backend.notification.infrastructure.expo.ExpoPushDtos.ReceiptsResponse;
import com.pabom.backend.notification.infrastructure.expo.ExpoPushDtos.SendResponse;
import com.pabom.backend.notification.infrastructure.expo.ExpoPushDtos.Ticket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/** Expo Push Service 클라이언트. 429·5xx·네트워크 오류는 지수 백오프로 재시도한다. */
@Slf4j
public class ExpoPushClient implements PushSenderPort {

    static final String SEND_PATH = "/--/api/v2/push/send";
    static final String RECEIPTS_PATH = "/--/api/v2/push/getReceipts";
    static final int MAX_MESSAGES_PER_REQUEST = 100;
    static final int MAX_RECEIPT_IDS_PER_REQUEST = 1000;
    static final String REQUEST_FAILED = "RequestFailed";

    private final RestTemplate restTemplate;
    private final ExpoPushProperties properties;
    private final Sleeper sleeper;

    public ExpoPushClient(RestTemplate restTemplate, ExpoPushProperties properties, Sleeper sleeper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.sleeper = sleeper;
    }

    @Override
    public boolean isConfigured() {
        return properties.hasAccessToken();
    }

    @Override
    public List<SendTicket> send(List<PushMessage> messages) {
        List<SendTicket> tickets = new ArrayList<>(messages.size());
        for (List<PushMessage> chunk : chunks(messages, MAX_MESSAGES_PER_REQUEST)) {
            tickets.addAll(sendChunk(chunk));
        }
        return tickets;
    }

    @Override
    public Map<String, Receipt> getReceipts(List<String> ticketIds) {
        Map<String, Receipt> receipts = new HashMap<>();
        for (List<String> chunk : chunks(ticketIds, MAX_RECEIPT_IDS_PER_REQUEST)) {
            Optional<ReceiptsResponse> response = executeWithRetry("getReceipts", () ->
                    restTemplate.postForObject(url(RECEIPTS_PATH),
                            new HttpEntity<>(new ReceiptsRequest(chunk), headers()),
                            ReceiptsResponse.class));
            response.ifPresent(body -> {
                logApiErrors("getReceipts", body.errors());
                if (body.data() != null) {
                    body.data().forEach((id, receipt) -> receipts.put(id, new Receipt(
                            "ok".equals(receipt.status()),
                            receipt.details() == null ? null : receipt.details().error(),
                            receipt.message())));
                }
            });
        }
        return receipts;
    }

    private List<SendTicket> sendChunk(List<PushMessage> chunk) {
        List<Message> body = chunk.stream()
                .map(message -> new Message(message.to(), message.title(), message.body(),
                        message.data(), "default", message.channelId(), "high"))
                .toList();
        Optional<SendResponse> response = executeWithRetry("send", () ->
                restTemplate.postForObject(url(SEND_PATH), new HttpEntity<>(body, headers()),
                        SendResponse.class));
        if (response.isEmpty()) {
            return failAll(chunk.size(), "Expo push request failed after retries.");
        }
        logApiErrors("send", response.get().errors());
        List<Ticket> data = response.get().data();
        if (data == null || data.size() != chunk.size()) {
            log.warn("Unexpected Expo push ticket count. expected={} actual={}",
                    chunk.size(), data == null ? 0 : data.size());
        }
        List<SendTicket> tickets = new ArrayList<>(chunk.size());
        for (int index = 0; index < chunk.size(); index++) {
            Ticket ticket = data != null && index < data.size() ? data.get(index) : null;
            if (ticket == null) {
                tickets.add(SendTicket.error(REQUEST_FAILED, "Expo push ticket missing."));
            } else if ("ok".equals(ticket.status())) {
                tickets.add(SendTicket.ok(ticket.id()));
            } else {
                tickets.add(SendTicket.error(
                        ticket.details() == null ? null : ticket.details().error(), ticket.message()));
            }
        }
        return tickets;
    }

    private <T> Optional<T> executeWithRetry(String operation, Supplier<T> request) {
        Duration backoff = properties.initialBackoff();
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                return Optional.ofNullable(request.get());
            } catch (HttpStatusCodeException exception) {
                int status = exception.getStatusCode().value();
                boolean retryable = status == 429 || exception.getStatusCode().is5xxServerError();
                log.warn("Expo push {} failed. status={} attempt={}", operation, status, attempt);
                if (!retryable) {
                    return Optional.empty();
                }
            } catch (ResourceAccessException exception) {
                log.warn("Expo push {} I/O failure. attempt={}", operation, attempt, exception);
            }
            if (attempt < properties.maxAttempts()) {
                try {
                    sleeper.sleep(backoff);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
                backoff = backoff.multipliedBy(2);
            }
        }
        log.error("Expo push {} gave up after {} attempts.", operation, properties.maxAttempts());
        return Optional.empty();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(properties.accessToken());
        return headers;
    }

    private String url(String path) {
        String base = properties.baseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
    }

    private void logApiErrors(String operation, List<ApiError> errors) {
        if (errors != null) {
            errors.forEach(error -> log.warn("Expo push {} error. code={} message={}",
                    operation, error.code(), error.message()));
        }
    }

    private List<SendTicket> failAll(int size, String message) {
        List<SendTicket> tickets = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            tickets.add(SendTicket.error(REQUEST_FAILED, message));
        }
        return tickets;
    }

    private static <T> List<List<T>> chunks(List<T> items, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int start = 0; start < items.size(); start += size) {
            chunks.add(items.subList(start, Math.min(items.size(), start + size)));
        }
        return chunks;
    }

    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }
}
