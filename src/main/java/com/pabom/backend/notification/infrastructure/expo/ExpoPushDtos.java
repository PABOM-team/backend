package com.pabom.backend.notification.infrastructure.expo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/** Expo Push API 요청·응답 형식. https://docs.expo.dev/push-notifications/sending-notifications/ */
final class ExpoPushDtos {

    private ExpoPushDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Message(
            String to,
            String title,
            String body,
            Map<String, Object> data,
            String sound,
            String channelId,
            String priority
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendResponse(List<Ticket> data, List<ApiError> errors) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Ticket(String status, String id, String message, Details details) {
    }

    record ReceiptsRequest(List<String> ids) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReceiptsResponse(Map<String, Receipt> data, List<ApiError> errors) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Receipt(String status, String message, Details details) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Details(String error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiError(String code, String message) {
    }
}
