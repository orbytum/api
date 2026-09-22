package com.orbytum.api.configuration.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    private static final Logger logger = LoggerFactory.getLogger(S3Config.class);

    private final S3Properties properties;

    public S3Config(S3Properties properties) {
        this.properties = properties;
    }

    private AwsCredentialsProvider resolveCredentialsProvider() {
        if (properties.getAccessKeyId() != null && !properties.getAccessKeyId().isBlank()
                && properties.getSecretAccessKey() != null && !properties.getSecretAccessKey().isBlank()) {
            logger.info("Configurando credenciais do Amazon S3 via StaticCredentialsProvider.");
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())
            );
        }
        logger.info("Credenciais explícitas não fornecidas. Utilizando DefaultCredentialsProvider para o Amazon S3.");
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client() {
        Region region = Region.of(properties.getRegion());
        AwsCredentialsProvider credentialsProvider = resolveCredentialsProvider();

        return S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .crossRegionAccessEnabled(true)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        Region region = Region.of(properties.getRegion());
        AwsCredentialsProvider credentialsProvider = resolveCredentialsProvider();

        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
