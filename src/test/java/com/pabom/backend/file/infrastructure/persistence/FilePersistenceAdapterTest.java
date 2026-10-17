package com.pabom.backend.file.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.type.FilePurpose;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FilePersistenceAdapterTest {

    @Autowired
    private FileJpaRepository jpaRepository;

    @Test
    void exposesOnlyOwnedNonDeletedFile() {
        FileJpaRepository repository = jpaRepository;
        File file = repository.saveAndFlush(File.temporary(
                7L,
                FilePurpose.PET_PHOTO,
                "image/jpeg",
                100,
                10,
                20,
                "files/7/key.jpg",
                Instant.parse("2026-10-24T01:00:00Z")
        ));
        FilePersistenceAdapter adapter = new FilePersistenceAdapter(repository);

        assertThat(adapter.findAccessibleByIdAndOwnerId(file.getId(), 7L)).contains(file);
        assertThat(adapter.findAccessibleByIdAndOwnerId(file.getId(), 8L)).isEmpty();
        assertThat(adapter.findAccessibleByIdAndOwnerId(999999L, 7L)).isEmpty();

        file.delete();
        repository.saveAndFlush(file);

        assertThat(adapter.findAccessibleByIdAndOwnerId(file.getId(), 7L)).isEmpty();
    }
}
