package com.filemgmt.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.filemgmt.domain.file.entity.FileMetadata;
import com.filemgmt.domain.file.repository.FileMetadataRepository;
import com.filemgmt.infrastructure.persistence.converter.FileMetadataConverter;
import com.filemgmt.infrastructure.persistence.entity.FileMetadataDO;
import com.filemgmt.infrastructure.persistence.mapper.FileMetadataMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class FileMetadataRepositoryImpl implements FileMetadataRepository {

    private final FileMetadataMapper fileMetadataMapper;

    public FileMetadataRepositoryImpl(FileMetadataMapper fileMetadataMapper) {
        this.fileMetadataMapper = fileMetadataMapper;
    }

    @Override
    public FileMetadata save(FileMetadata fileMetadata) {
        FileMetadataDO metadataDO = FileMetadataConverter.toDO(fileMetadata);
        if (metadataDO.getId() == null) {
            fileMetadataMapper.insert(metadataDO);
            fileMetadata.setId(metadataDO.getId());
        } else {
            fileMetadataMapper.updateById(metadataDO);
        }
        return fileMetadata;
    }

    @Override
    public FileMetadata findById(Long id) {
        FileMetadataDO metadataDO = fileMetadataMapper.selectById(id);
        return FileMetadataConverter.toDomain(metadataDO);
    }

    @Override
    public FileMetadata findByFileHash(String fileHash) {
        LambdaQueryWrapper<FileMetadataDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadataDO::getFileHash, fileHash)
               .eq(FileMetadataDO::getUploadStatus, "COMPLETED")
               .last("LIMIT 1");
        FileMetadataDO metadataDO = fileMetadataMapper.selectOne(wrapper);
        return FileMetadataConverter.toDomain(metadataDO);
    }

    @Override
    public List<FileMetadata> findByUserId(Long userId, int page, int size) {
        LambdaQueryWrapper<FileMetadataDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadataDO::getUserId, userId)
               .eq(FileMetadataDO::getUploadStatus, "COMPLETED")
               .orderByDesc(FileMetadataDO::getCreatedAt)
               .last("LIMIT " + size + " OFFSET " + ((page - 1) * size));
        List<FileMetadataDO> records = fileMetadataMapper.selectList(wrapper);
        return records.stream()
                .map(FileMetadataConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId) {
        LambdaQueryWrapper<FileMetadataDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadataDO::getUserId, userId)
               .eq(FileMetadataDO::getUploadStatus, "COMPLETED");
        return fileMetadataMapper.selectCount(wrapper);
    }

    @Override
    public void deleteById(Long id) {
        fileMetadataMapper.deleteById(id);
    }
}
