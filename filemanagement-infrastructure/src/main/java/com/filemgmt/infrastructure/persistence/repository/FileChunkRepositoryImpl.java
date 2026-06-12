package com.filemgmt.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.filemgmt.domain.file.entity.FileChunk;
import com.filemgmt.domain.file.repository.FileChunkRepository;
import com.filemgmt.infrastructure.persistence.converter.FileChunkConverter;
import com.filemgmt.infrastructure.persistence.entity.FileChunkDO;
import com.filemgmt.infrastructure.persistence.mapper.FileChunkMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class FileChunkRepositoryImpl implements FileChunkRepository {

    private final FileChunkMapper fileChunkMapper;

    public FileChunkRepositoryImpl(FileChunkMapper fileChunkMapper) {
        this.fileChunkMapper = fileChunkMapper;
    }

    @Override
    public FileChunk save(FileChunk fileChunk) {
        FileChunkDO chunkDO = FileChunkConverter.toDO(fileChunk);
        if (chunkDO.getId() == null) {
            fileChunkMapper.insert(chunkDO);
            fileChunk.setId(chunkDO.getId());
        } else {
            fileChunkMapper.updateById(chunkDO);
        }
        return fileChunk;
    }

    @Override
    public FileChunk findByFileMetadataIdAndChunkIndex(Long fileMetadataId, Integer chunkIndex) {
        LambdaQueryWrapper<FileChunkDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileChunkDO::getFileMetadataId, fileMetadataId)
               .eq(FileChunkDO::getChunkIndex, chunkIndex);
        FileChunkDO chunkDO = fileChunkMapper.selectOne(wrapper);
        return FileChunkConverter.toDomain(chunkDO);
    }

    @Override
    public List<FileChunk> findByFileMetadataId(Long fileMetadataId) {
        LambdaQueryWrapper<FileChunkDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileChunkDO::getFileMetadataId, fileMetadataId)
               .orderByAsc(FileChunkDO::getChunkIndex);
        List<FileChunkDO> chunkDOs = fileChunkMapper.selectList(wrapper);
        return chunkDOs.stream()
                .map(FileChunkConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countUploadedChunks(Long fileMetadataId) {
        LambdaQueryWrapper<FileChunkDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileChunkDO::getFileMetadataId, fileMetadataId)
               .eq(FileChunkDO::getUploaded, true);
        return fileChunkMapper.selectCount(wrapper);
    }

    @Override
    public void deleteByFileMetadataId(Long fileMetadataId) {
        LambdaQueryWrapper<FileChunkDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileChunkDO::getFileMetadataId, fileMetadataId);
        fileChunkMapper.delete(wrapper);
    }
}
