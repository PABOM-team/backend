package com.pabom.backend.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pabom.backend.notification.application.port.PushSenderPort;
import com.pabom.backend.notification.application.port.PushSenderPort.PushMessage;
import com.pabom.backend.notification.application.port.PushSenderPort.Receipt;
import com.pabom.backend.notification.application.port.PushSenderPort.SendTicket;
import com.pabom.backend.notification.domain.entity.PushTicket;
import com.pabom.backend.notification.domain.repository.PushTicketRepository;
import com.pabom.backend.notification.domain.repository.PushTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class PushDeliveryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-21T00:00:00Z");

    private PushSenderPort sender;
    private PushTokenRepository tokens;
    private PushTicketRepository tickets;
    private PushDeliveryService service;

    @BeforeEach
    void setUp() {
        sender = mock(PushSenderPort.class);
        tokens = mock(PushTokenRepository.class);
        tickets = mock(PushTicketRepository.class);
        given(sender.isConfigured()).willReturn(true);
        service = new PushDeliveryService(sender, tokens, tickets,
                new TransactionTemplate(mock(PlatformTransactionManager.class)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @SuppressWarnings("unchecked")
    void storesOkTicketsAndDeletesTokensReportedAsNotRegistered() {
        given(sender.send(any())).willReturn(List.of(
                SendTicket.ok("ticket-a"),
                SendTicket.error(PushSenderPort.DEVICE_NOT_REGISTERED, "not a valid token"),
                SendTicket.error("MessageRateExceeded", "slow down")));

        var result = service.deliver(List.of(message("t-a"), message("t-b"), message("t-c")));

        assertThat(result.targetCount()).isEqualTo(3);
        assertThat(result.acceptedCount()).isEqualTo(1);
        ArgumentCaptor<List<PushTicket>> saved = ArgumentCaptor.forClass(List.class);
        verify(tickets).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(ticket -> {
            assertThat(ticket.getTicketId()).isEqualTo("ticket-a");
            assertThat(ticket.getToken()).isEqualTo("t-a");
        });
        verify(tokens).deleteByToken("t-b");
        verify(tokens, never()).deleteByToken("t-c");
    }

    @Test
    @SuppressWarnings("unchecked")
    void checksReceiptsAfterDelayAndCleansUpUnregisteredDevices() {
        PushTicket ok = ticket(1L, "r-ok", "t-ok", NOW.minusSeconds(20 * 60));
        PushTicket gone = ticket(2L, "r-gone", "t-gone", NOW.minusSeconds(20 * 60));
        PushTicket pending = ticket(3L, "r-pending", "t-pending", NOW.minusSeconds(20 * 60));
        PushTicket expired = ticket(4L, "r-expired", "t-expired", NOW.minusSeconds(25 * 3600));
        given(tickets.findCreatedAtOrBefore(NOW.minus(PushDeliveryService.RECEIPT_DELAY),
                PushDeliveryService.RECEIPT_BATCH_SIZE)).willReturn(List.of(ok, gone, pending, expired));
        given(sender.getReceipts(List.of("r-ok", "r-gone", "r-pending", "r-expired"))).willReturn(Map.of(
                "r-ok", new Receipt(true, null, null),
                "r-gone", new Receipt(false, PushSenderPort.DEVICE_NOT_REGISTERED, "gone")));

        service.checkReceipts();

        verify(tokens).deleteByToken("t-gone");
        verify(tokens, never()).deleteByToken("t-ok");
        ArgumentCaptor<java.util.Collection<Long>> deleted = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(tickets).deleteAllByIdIn(deleted.capture());
        assertThat(deleted.getValue()).containsExactlyInAnyOrder(1L, 2L, 4L);
    }

    @Test
    void skipsReceiptCheckWhenNotConfigured() {
        given(sender.isConfigured()).willReturn(false);

        service.checkReceipts();

        verify(tickets, never()).findCreatedAtOrBefore(any(), anyInt());
    }

    private PushMessage message(String token) {
        return new PushMessage(token, "title", "body", Map.of("url", "/home"), "care");
    }

    private PushTicket ticket(Long id, String ticketId, String token, Instant createdAt) {
        PushTicket ticket = new PushTicket(ticketId, token, createdAt);
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }
}
