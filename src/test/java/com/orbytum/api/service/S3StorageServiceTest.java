package com.orbytum.api.service;

import com.orbytum.api.configuration.s3.S3Properties;
import com.orbytum.api.models.exceptions.S3StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Utilities s3Utilities;

    private S3Properties properties;
    private S3StorageService storageService;

    private final String bucketName = "test-orbytum-bucket";
    private final String key = "documentos/teste.pdf";

    @BeforeEach
    void setUp() {
        properties = new S3Properties();
        properties.setBucketName(bucketName);
        properties.setRegion("sa-east-1");

        storageService = new S3StorageService(s3Client, s3Presigner, properties);
    }

    @Test
    void testUploadFileBytesComSucesso() throws MalformedURLException {
        byte[] content = "conteudo de teste".getBytes(StandardCharsets.UTF_8);
        URL expectedUrl = URI.create("https://" + bucketName + ".s3.sa-east-1.amazonaws.com/" + key).toURL();

        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(GetUrlRequest.class))).thenReturn(expectedUrl);

        String resultUrl = storageService.uploadFile(key, content, "application/pdf");

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals(expectedUrl.toExternalForm(), resultUrl);
    }

    @Test
    void testUploadMultipartFileComSucesso() throws MalformedURLException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "teste.txt",
                "text/plain",
                "texto simples".getBytes(StandardCharsets.UTF_8)
        );
        URL expectedUrl = URI.create("https://" + bucketName + ".s3.sa-east-1.amazonaws.com/" + key).toURL();

        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(GetUrlRequest.class))).thenReturn(expectedUrl);

        String resultUrl = storageService.uploadFile(key, file);

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals(expectedUrl.toExternalForm(), resultUrl);
    }

    @Test
    void testUploadFileLancaExcecaoQuandoAwsFalha() {
        byte[] content = "dados".getBytes(StandardCharsets.UTF_8);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(AwsServiceException.builder().message("Access Denied").build());

        assertThrows(S3StorageException.class, () -> storageService.uploadFile(key, content, "text/plain"));
    }

    @Test
    void testDownloadFileComSucesso() {
        byte[] expectedContent = "arquivo baixado".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse response = GetObjectResponse.builder().build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response, expectedContent);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] result = storageService.downloadFile(key);

        assertNotNull(result);
        assertArrayEquals(expectedContent, result);
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void testDownloadFileArquivoNaoEncontrado() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("The specified key does not exist.").build());

        assertThrows(S3StorageException.class, () -> storageService.downloadFile(key));
    }

    @Test
    void testDeleteFileComSucesso() {
        doReturn(DeleteObjectResponse.builder().build())
                .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        assertDoesNotThrow(() -> storageService.deleteFile(key));
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDoesFileExistRetornaTrueQuandoExiste() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertTrue(storageService.doesFileExist(key));
    }

    @Test
    void testDoesFileExistRetornaFalseQuandoNaoExiste() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertFalse(storageService.doesFileExist(key));
    }

    @Test
    void testGeneratePresignedGetUrlComSucesso() throws MalformedURLException {
        URL expectedUrl = URI.create("https://" + bucketName + ".s3.sa-east-1.amazonaws.com/" + key + "?signature=abc").toURL();
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = storageService.generatePresignedGetUrl(key, Duration.ofMinutes(15));

        assertEquals(expectedUrl.toString(), url);
        verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void testGeneratePresignedPutUrlComSucesso() throws MalformedURLException {
        URL expectedUrl = URI.create("https://" + bucketName + ".s3.sa-east-1.amazonaws.com/" + key + "?upload=xyz").toURL();
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = storageService.generatePresignedPutUrl(key, "application/pdf", Duration.ofMinutes(10));

        assertEquals(expectedUrl.toString(), url);
        verify(s3Presigner, times(1)).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void testValidacoesDeParametros() {
        assertThrows(IllegalArgumentException.class, () -> storageService.uploadFile("", new byte[0], "text/plain"));
        assertThrows(IllegalArgumentException.class, () -> storageService.downloadFile(" "));
        assertThrows(IllegalArgumentException.class, () -> storageService.deleteFile(null));
        assertThrows(IllegalArgumentException.class, () -> storageService.generatePresignedGetUrl(key, Duration.ZERO));
    }
}
