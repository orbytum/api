package com.orbytum.api.service;

import com.orbytum.api.configuration.s3.S3Properties;
import com.orbytum.api.models.exceptions.S3StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Service
public class S3StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner, S3Properties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        validateKey(key);
        if (inputStream == null) {
            throw new IllegalArgumentException("O stream do arquivo não pode ser nulo.");
        }

        try {
            String resolvedContentType = (contentType != null && !contentType.isBlank())
                    ? contentType
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .contentType(resolvedContentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
            logger.info("Arquivo '{}' enviado com sucesso para o bucket '{}'.", key, getBucketName());
            return getFileUrl(key);
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Erro do AWS S3 ao enviar arquivo '{}': {}", key, e.getMessage(), e);
            throw new S3StorageException("Erro ao enviar arquivo para o S3: " + e.getMessage(), e);
        }
    }

    public String uploadFile(String key, byte[] data, String contentType) {
        if (data == null) {
            throw new IllegalArgumentException("Os bytes do arquivo não podem ser nulos.");
        }
        return uploadFile(key, new ByteArrayInputStream(data), data.length, contentType);
    }

    public String uploadFile(String key, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo MultipartFile não pode ser nulo ou vazio.");
        }
        try {
            return uploadFile(key, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            logger.error("Falha ao ler os dados do arquivo MultipartFile para chave '{}': {}", key, e.getMessage(), e);
            throw new S3StorageException("Falha ao ler o arquivo enviado: " + e.getMessage(), e);
        }
    }

    public byte[] downloadFile(String key) {
        validateKey(key);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return responseBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            logger.warn("Arquivo '{}' não encontrado no bucket '{}'.", key, getBucketName());
            throw new S3StorageException("Arquivo não encontrado no S3: " + key, e);
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Erro do AWS S3 ao baixar arquivo '{}': {}", key, e.getMessage(), e);
            throw new S3StorageException("Erro ao baixar arquivo do S3: " + e.getMessage(), e);
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFileStream(String key) {
        validateKey(key);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            return s3Client.getObject(getObjectRequest);
        } catch (NoSuchKeyException e) {
            logger.warn("Arquivo '{}' não encontrado no bucket '{}'.", key, getBucketName());
            throw new S3StorageException("Arquivo não encontrado no S3: " + key, e);
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Erro do AWS S3 ao obter stream do arquivo '{}': {}", key, e.getMessage(), e);
            throw new S3StorageException("Erro ao obter stream do arquivo do S3: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String key) {
        validateKey(key);
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            logger.info("Arquivo '{}' excluído com sucesso do bucket '{}'.", key, getBucketName());
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Erro do AWS S3 ao excluir arquivo '{}': {}", key, e.getMessage(), e);
            throw new S3StorageException("Erro ao excluir arquivo do S3: " + e.getMessage(), e);
        }
    }

    public boolean doesFileExist(String key) {
        validateKey(key);
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Erro do AWS S3 ao verificar existência do arquivo '{}': {}", key, e.getMessage(), e);
            throw new S3StorageException("Erro ao verificar existência do arquivo no S3: " + e.getMessage(), e);
        }
    }

    public String generatePresignedGetUrl(String key, Duration expiration) {
        validateKey(key);
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("O tempo de expiração da URL pré-assinada deve ser maior que zero.");
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
            return presignedGetObjectRequest.url().toString();
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Erro ao gerar URL pré-assinada de leitura para o arquivo '{}': {}", key, e.getMessage(), e);
            throw new S3StorageException("Erro ao gerar URL pré-assinada de leitura: " + e.getMessage(), e);
        }
    }

    public String generatePresignedPutUrl(String key, String contentType, Duration expiration) {
        validateKey(key);
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("O tempo de expiração da URL pré-assinada deve ser maior que zero.");
        }

        try {
            PutObjectRequest.Builder putRequestBuilder = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key);

            if (contentType != null && !contentType.isBlank()) {
                putRequestBuilder.contentType(contentType);
            }

            PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .putObjectRequest(putRequestBuilder.build())
                    .build();

            PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(putObjectPresignRequest);
            return presignedPutObjectRequest.url().toString();
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Erro ao gerar URL pré-assinada de upload para o arquivo '{}': {}", key, e.getMessage(), e);
            throw new S3StorageException("Erro ao gerar URL pré-assinada de upload: " + e.getMessage(), e);
        }
    }

    public String getFileUrl(String key) {
        validateKey(key);
        try {
            GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            return s3Client.utilities().getUrl(getUrlRequest).toExternalForm();
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Erro ao obter URL do arquivo '{}': {}", key, e.getMessage(), e);
            throw new S3StorageException("Erro ao obter URL do arquivo no S3: " + e.getMessage(), e);
        }
    }

    private String getBucketName() {
        String bucket = properties.getBucketName();
        if (bucket == null || bucket.isBlank()) {
            throw new S3StorageException("O nome do bucket S3 não foi configurado (aws.s3.bucket-name).");
        }
        return bucket;
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("A chave (key) do arquivo no S3 não pode ser nula ou vazia.");
        }
    }
}
