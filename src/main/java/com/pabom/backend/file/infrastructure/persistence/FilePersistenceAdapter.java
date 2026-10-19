package com.pabom.backend.file.infrastructure.persistence;

import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.repository.FileRepository;
import com.pabom.backend.file.domain.type.FileStatus;
import java.util.Optional;
import java.util.List;
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

    @Override
    public List<File> findAllByOwnerId(Long ownerId) {
        return repository.findAllByOwnerId(ownerId);
    }

    @Override
    public void deleteAllByOwnerId(Long ownerId) {
        repository.deleteAllByOwnerId(ownerId);
    }
}
