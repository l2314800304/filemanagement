package com.filemgmt.infrastructure.persistence.converter;

import com.filemgmt.domain.file.entity.FileMetadata;
import com.filemgmt.domain.file.valueobject.UploadStatus;
import com.filemgmt.infrastructure.persistence.entity.FileMetadataDO;

public class FileMetadataConverter {

    public static FileMetadata toDomain(FileMetadataDO metadataDO) {
        if (metadataDO == null) return null;
        FileMetadata metadata = new FileMetadata();
        metadata.setId(metadataDO.getId());
        metadata.setFileName(metadataDO.getFileName());
        metadata.setFileSize(metadataDO.getFileSize());
        metadata.setFileHash(metadataDO.getFileHash());
        metadata.setMimeType(metadataDO.getMimeType());
        metadata.setStoragePath(metadataDO.getStoragePath());
        metadata.setUploadStatus(UploadStatus.valueOf(metadataDO.getUploadStatus()));
        metadata.setUserId(metadataDO.getUserId());
        metadata.setTotalChunks(metadataDO.getTotalChunks());
        metadata.setCreatedAt(metadataDO.getCreatedAt());
        metadata.setUpdatedAt(metadataDO.getUpdatedAt());
        return metadata;
    }

    public static FileMetadataDO toDO(FileMetadata metadata) {
        if (metadata == null) return null;
        FileMetadataDO metadataDO = new FileMetadataDO();
        metadataDO.setId(metadata.getId());
        metadataDO.setFileName(metadata.getFileName());
        metadataDO.setFileSize(metadata.getFileSize());
        metadataDO.setFileHash(metadata.getFileHash());
        metadataDO.setMimeType(metadata.getMimeType());
        metadataDO.setStoragePath(metadata.getStoragePath());
        metadataDO.setUploadStatus(metadata.getUploadStatus().name());
        metadataDO.setUserId(metadata.getUserId());
        metadataDO.setTotalChunks(metadata.getTotalChunks());
        metadataDO.setCreatedAt(metadata.getCreatedAt());
        metadataDO.setUpdatedAt(metadata.getUpdatedAt());
        return metadataDO;
    }
}
