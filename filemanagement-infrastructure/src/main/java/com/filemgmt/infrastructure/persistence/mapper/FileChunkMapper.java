package com.filemgmt.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.filemgmt.infrastructure.persistence.entity.FileChunkDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileChunkMapper extends BaseMapper<FileChunkDO> {
}
