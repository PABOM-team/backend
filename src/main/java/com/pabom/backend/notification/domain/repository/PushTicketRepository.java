package com.pabom.backend.notification.domain.repository;

import com.pabom.backend.notification.domain.entity.PushTicket;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PushTicketRepository {

    void saveAll(List<PushTicket> tickets);

    List<PushTicket> findCreatedAtOrBefore(Instant cutoff, int limit);

    void deleteAllByIdIn(Collection<Long> ids);
}
