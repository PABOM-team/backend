package com.pabom.backend.file.domain.repository;

import com.pabom.backend.file.domain.entity.File;
import java.util.Optional;

public interface FileRepository {

    File save(File file);

    Optional<File> findAccessibleByIdAndOwnerId(Long id, Long ownerId);
}
