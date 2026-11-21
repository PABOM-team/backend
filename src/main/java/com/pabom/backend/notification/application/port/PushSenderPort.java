package com.pabom.backend.notification.application.port;

import java.util.List;
import java.util.Map;

/** 푸시 발송 서비스(Expo Push) 포트. */
public interface PushSenderPort {

    String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

    /** 발송에 필요한 설정(EXPO_ACCESS_TOKEN)이 있는지. */
    boolean isConfigured();

    /** 메시지를 보내고, 요청 순서와 같은 순서로 ticket을 돌려준다. */
    List<SendTicket> send(List<PushMessage> messages);

    /** ticket id별 receipt. 아직 준비되지 않은 receipt는 결과에 없다. */
    Map<String, Receipt> getReceipts(List<String> ticketIds);

    record PushMessage(
            String to,
            String title,
            String body,
            Map<String, Object> data,
            String channelId
    ) {
    }

    record SendTicket(boolean ok, String ticketId, String error, String message) {

        public static SendTicket ok(String ticketId) {
            return new SendTicket(true, ticketId, null, null);
        }

        public static SendTicket error(String error, String message) {
            return new SendTicket(false, null, error, message);
        }

        public boolean isDeviceNotRegistered() {
            return DEVICE_NOT_REGISTERED.equals(error);
        }
    }

    record Receipt(boolean ok, String error, String message) {

        public boolean isDeviceNotRegistered() {
            return DEVICE_NOT_REGISTERED.equals(error);
        }
    }
}
