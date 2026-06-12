package com.filemgmt.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.filemgmt.domain.policy.repository.FilePolicyRepository;
import com.filemgmt.domain.policy.valueobject.FileFormatPolicy;
import com.filemgmt.domain.policy.valueobject.FileSizeLimit;
import com.filemgmt.infrastructure.persistence.entity.FilePolicyDO;
import com.filemgmt.infrastructure.persistence.mapper.FilePolicyMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FilePolicyRepositoryImpl implements FilePolicyRepository {

    private final FilePolicyMapper filePolicyMapper;

    public FilePolicyRepositoryImpl(FilePolicyMapper filePolicyMapper) {
        this.filePolicyMapper = filePolicyMapper;
    }

    @Override
    public FileFormatPolicy findActiveFormatPolicy() {
        LambdaQueryWrapper<FilePolicyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FilePolicyDO::getPolicyType, "FORMAT")
               .eq(FilePolicyDO::getIsActive, true)
               .last("LIMIT 1");
        FilePolicyDO policyDO = filePolicyMapper.selectOne(wrapper);
        if (policyDO == null) {
            return new FileFormatPolicy(null, null);
        }
        return new FileFormatPolicy(policyDO.getAllowedExtensions(), policyDO.getBlockedExtensions());
    }

    @Override
    public FileSizeLimit findActiveSizeLimit() {
        LambdaQueryWrapper<FilePolicyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FilePolicyDO::getPolicyType, "SIZE")
               .eq(FilePolicyDO::getIsActive, true)
               .last("LIMIT 1");
        FilePolicyDO policyDO = filePolicyMapper.selectOne(wrapper);
        if (policyDO == null) {
            return new FileSizeLimit(null);
        }
        return new FileSizeLimit(policyDO.getMaxFileSize());
    }
}
