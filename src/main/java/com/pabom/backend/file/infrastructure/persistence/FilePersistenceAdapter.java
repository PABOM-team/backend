package com.pabom.backend.file.infrastructure.persistence;

import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.repository.FileRepository;
import com.pabom.backend.file.domain.type.FileStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FilePersistenceAdapter implements FileRepository {

    private final FileJpaRepository repository;

    @Override
    public File save(File file) {
        return repository.saveAndFlush(file);
    }

    @Override
    public Optional<File> findAccessibleByIdAndOwnerId(Long id, Long ownerId) {
        return repository.findByIdAndOwnerIdAndStatusNot(id, ownerId, FileStatus.DELETED);
    }
}
