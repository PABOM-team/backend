package com.pabom.backend.notification.application.service;

import com.pabom.backend.notification.application.port.PushSenderPort;
import com.pabom.backend.notification.application.port.PushSenderPort.PushMessage;
import com.pabom.backend.notification.application.port.PushSenderPort.Receipt;
import com.pabom.backend.notification.application.port.PushSenderPort.SendTicket;
import com.pabom.backend.notification.application.result.PushDeliveryResult;
import com.pabom.backend.notification.domain.entity.PushTicket;
import com.pabom.backend.notification.domain.repository.PushTicketRepository;
import com.pabom.backend.notification.domain.repository.PushTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 푸시를 보내고 ticket·receipt 결과로 토큰을 정리한다. 외부 호출 중에는 DB 트랜잭션을 잡지 않는다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushDeliveryService {

    /** Expo 권장: 발송 후 약 15분 뒤 receipt 확인. */
    static final Duration RECEIPT_DELAY = Duration.ofMinutes(15);
    /** Expo는 receipt를 24시간 동안 보관한다. 그보다 오래된 ticket은 버린다. */
    static final Duration RECEIPT_RETENTION = Duration.ofHours(24);
    static final int RECEIPT_BATCH_SIZE = 1000;

    private final PushSenderPort sender;
    private final PushTokenRepository pushTokenRepository;
    private final PushTicketRepository pushTicketRepository;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public PushDeliveryResult deliver(List<PushMessage> messages) {
        if (messages.isEmpty()) {
            return PushDeliveryResult.empty();
        }
        List<SendTicket> tickets = sender.send(messages);
        Instant now = clock.instant();
        List<PushTicket> accepted = new ArrayList<>();
        Set<String> unregisteredTokens = new LinkedHashSet<>();
        for (int index = 0; index < messages.size(); index++) {
            String token = messages.get(index).to();
            SendTicket ticket = index < tickets.size() ? tickets.get(index) : null;
            if (ticket == null) {
                log.warn("Expo push ticket missing. index={}", index);
            } else if (ticket.ok()) {
                if (ticket.ticketId() != null) {
                    accepted.add(new PushTicket(ticket.ticketId(), token, now));
                }
            } else if (ticket.isDeviceNotRegistered()) {
                unregisteredTokens.add(token);
            } else {
                log.warn("Expo push ticket error. error={} message={}", ticket.error(), ticket.message());
            }
        }
        transactions.executeWithoutResult(status -> {
            pushTicketRepository.saveAll(accepted);
            unregisteredTokens.forEach(pushTokenRepository::deleteByToken);
        });
        if (!unregisteredTokens.isEmpty()) {
            log.info("Deleted unregistered push tokens from tickets. count={}", unregisteredTokens.size());
        }
        int acceptedCount = Math.toIntExact(tickets.stream().filter(SendTicket::ok).count());
        return new PushDeliveryResult(messages.size(), acceptedCount);
    }

    /** 15분 이상 지난 ticket의 receipt를 확인한다. DeviceNotRegistered 토큰은 지운다. */
    public void checkReceipts() {
        if (!sender.isConfigured()) {
            return;
        }
        Instant now = clock.instant();
        List<PushTicket> tickets = pushTicketRepository.findCreatedAtOrBefore(
                now.minus(RECEIPT_DELAY), RECEIPT_BATCH_SIZE);
        if (tickets.isEmpty()) {
            return;
        }
        Map<String, Receipt> receipts = sender.getReceipts(
                tickets.stream().map(PushTicket::getTicketId).toList());
        Instant expiredBefore = now.minus(RECEIPT_RETENTION);
        List<Long> processed = new ArrayList<>();
        Set<String> unregisteredTokens = new LinkedHashSet<>();
        for (PushTicket ticket : tickets) {
            Receipt receipt = receipts.get(ticket.getTicketId());
            if (receipt == null) {
                if (!ticket.getCreatedAt().isAfter(expiredBefore)) {
                    processed.add(ticket.getId());
                }
                continue;
            }
            processed.add(ticket.getId());
            if (receipt.isDeviceNotRegistered()) {
                unregisteredTokens.add(ticket.getToken());
            } else if (!receipt.ok()) {
                log.warn("Expo push receipt error. ticketId={} error={} message={}",
                        ticket.getTicketId(), receipt.error(), receipt.message());
            }
        }
        transactions.executeWithoutResult(status -> {
            unregisteredTokens.forEach(pushTokenRepository::deleteByToken);
            pushTicketRepository.deleteAllByIdIn(processed);
        });
        if (!unregisteredTokens.isEmpty()) {
            log.info("Deleted unregistered push tokens from receipts. count={}", unregisteredTokens.size());
        }
    }
}
