package com.pabom.backend.notification.infrastructure.persistence;

import com.pabom.backend.notification.domain.entity.PushTicket;
import com.pabom.backend.notification.domain.repository.PushTicketRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PushTicketPersistenceAdapter implements PushTicketRepository {

    private final PushTicketJpaRepository repository;

    @Override
    public void saveAll(List<PushTicket> tickets) {
        repository.saveAll(tickets);
    }

    @Override
    public List<PushTicket> findCreatedAtOrBefore(Instant cutoff, int limit) {
        return repository.findCreatedAtOrBefore(cutoff, PageRequest.of(0, limit));
    }

    @Override
    public void deleteAllByIdIn(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        repository.deleteAllByIdIn(ids);
    }
}
