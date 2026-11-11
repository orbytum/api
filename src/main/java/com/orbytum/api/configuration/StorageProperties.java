package com.orbytum.api.configuration;

import com.orbytum.api.models.enums.StorageProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "orbytum.storage")
public class StorageProperties {

    /**
     * auto: tenta S3 e cai para o banco em caso de falha.
     * s3: usa S3, respeitando fallback-enabled.
     * database: usa somente o PostgreSQL.
     */
    private StorageProvider provider = StorageProvider.AUTO;

    private boolean fallbackEnabled = true;

    private long maxFileSizeBytes = 20L * 1024 * 1024;

    private String publicBaseUrl = "http://localhost:8080";
}
