package com.orbytum.api.configuration.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {
    private String region = "sa-east-1";
    private String bucketName;
    private String accessKeyId;
    private String secretAccessKey;
}
