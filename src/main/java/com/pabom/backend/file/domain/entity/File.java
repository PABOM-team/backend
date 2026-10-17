package com.pabom.backend.file.domain.entity;

import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.domain.type.FileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "files")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FilePurpose purpose;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "storage_key", nullable = false, length = 500, unique = true)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileStatus status;

    @Column(name = "temp_expires_at", nullable = false)
    private Instant tempExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected File() {
    }

    private File(
            Long ownerId,
            FilePurpose purpose,
            String contentType,
            long sizeBytes,
            int width,
            int height,
            String storageKey,
            Instant createdAt
    ) {
        this.ownerId = ownerId;
        this.purpose = purpose;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.storageKey = storageKey;
        this.status = FileStatus.TEMP;
        this.tempExpiresAt = createdAt.plusSeconds(24 * 60 * 60);
        this.createdAt = createdAt;
    }

    public static File temporary(
            Long ownerId,
            FilePurpose purpose,
            String contentType,
            long sizeBytes,
            int width,
            int height,
            String storageKey,
            Instant createdAt
    ) {
        return new File(ownerId, purpose, contentType, sizeBytes, width, height, storageKey, createdAt);
    }

    public void delete() {
        this.status = FileStatus.DELETED;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public FilePurpose getPurpose() {
        return purpose;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public FileStatus getStatus() {
        return status;
    }

    public Instant getTempExpiresAt() {
        return tempExpiresAt;
    }
}
