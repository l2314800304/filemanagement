package com.filemgmt.infrastructure.persistence.converter;

import com.filemgmt.domain.file.entity.FileChunk;
import com.filemgmt.infrastructure.persistence.entity.FileChunkDO;

public class FileChunkConverter {

    public static FileChunk toDomain(FileChunkDO chunkDO) {
        if (chunkDO == null) return null;
        FileChunk chunk = new FileChunk();
        chunk.setId(chunkDO.getId());
        chunk.setFileMetadataId(chunkDO.getFileMetadataId());
        chunk.setChunkIndex(chunkDO.getChunkIndex());
        chunk.setChunkSize(chunkDO.getChunkSize());
        chunk.setChunkHash(chunkDO.getChunkHash());
        chunk.setStoragePath(chunkDO.getStoragePath());
        chunk.setUploaded(chunkDO.getUploaded());
        chunk.setCreatedAt(chunkDO.getCreatedAt());
        return chunk;
    }

    public static FileChunkDO toDO(FileChunk chunk) {
        if (chunk == null) return null;
        FileChunkDO chunkDO = new FileChunkDO();
        chunkDO.setId(chunk.getId());
        chunkDO.setFileMetadataId(chunk.getFileMetadataId());
        chunkDO.setChunkIndex(chunk.getChunkIndex());
        chunkDO.setChunkSize(chunk.getChunkSize());
        chunkDO.setChunkHash(chunk.getChunkHash());
        chunkDO.setStoragePath(chunk.getStoragePath());
        chunkDO.setUploaded(chunk.getUploaded());
        chunkDO.setCreatedAt(chunk.getCreatedAt());
        return chunkDO;
    }
}
