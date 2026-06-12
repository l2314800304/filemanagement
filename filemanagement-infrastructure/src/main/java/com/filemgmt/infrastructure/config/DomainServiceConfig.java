package com.filemgmt.infrastructure.config;

import com.filemgmt.domain.auth.repository.UserRepository;
import com.filemgmt.domain.auth.service.UserDomainService;
import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.domain.file.repository.FileChunkRepository;
import com.filemgmt.domain.file.repository.FileMetadataRepository;
import com.filemgmt.domain.file.service.FileDownloadDomainService;
import com.filemgmt.domain.file.service.FileUploadDomainService;
import com.filemgmt.domain.policy.repository.FilePolicyRepository;
import com.filemgmt.domain.policy.service.FilePolicyDomainService;
import com.filemgmt.infrastructure.crypto.BouncyCastleCryptoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public CryptoDomainService cryptoDomainService() {
        return new BouncyCastleCryptoService();
    }

    @Bean
    public UserDomainService userDomainService(UserRepository userRepository, CryptoDomainService cryptoService) {
        return new UserDomainService(userRepository, cryptoService);
    }

    @Bean
    public FileUploadDomainService fileUploadDomainService(FileMetadataRepository fileMetadataRepository,
                                                            FileChunkRepository fileChunkRepository) {
        return new FileUploadDomainService(fileMetadataRepository, fileChunkRepository);
    }

    @Bean
    public FileDownloadDomainService fileDownloadDomainService() {
        return new FileDownloadDomainService();
    }

    @Bean
    public FilePolicyDomainService filePolicyDomainService(FilePolicyRepository policyRepository) {
        return new FilePolicyDomainService(policyRepository);
    }
}
