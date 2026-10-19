package com.pabom.backend.file.infrastructure.persistence;

import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.type.FileStatus;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileJpaRepository extends JpaRepository<File, Long> {

    Optional<File> findByIdAndOwnerIdAndStatusNot(Long id, Long ownerId, FileStatus status);

    List<File> findAllByOwnerId(Long ownerId);

    void deleteAllByOwnerId(Long ownerId);
}
