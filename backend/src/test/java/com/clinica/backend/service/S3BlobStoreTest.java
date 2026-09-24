package com.clinica.backend.service;

import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class S3BlobStoreTest {

    private S3Client client;
    private S3BlobStore store;

    @BeforeEach
    void setUp() {
        client = mock(S3Client.class);
        store = new S3BlobStore(client, "clinica-archivos");
    }

    @Test
    void putSendsBucketKeyTypeAndLength() {
        byte[] content = {1, 2, 3};

        store.put("clinical/lesions/a.png", new ByteArrayInputStream(content), content.length, "image/png");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture(), any(RequestBody.class));
        assertEquals("clinica-archivos", request.getValue().bucket());
        assertEquals("clinical/lesions/a.png", request.getValue().key());
        assertEquals("image/png", request.getValue().contentType());
        assertEquals(3L, request.getValue().contentLength());
    }

    @Test
    void getReturnsContentWithFilename() throws Exception {
        byte[] content = {9, 8, 7};
        when(client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content));

        Resource resource = store.get("website/hero/b.webp");

        assertEquals("b.webp", resource.getFilename());
        assertArrayEquals(content, resource.getContentAsByteArray());
    }

    @Test
    void missingObjectIsNotFound() {
        when(client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        assertThrows(ResourceNotFoundException.class, () -> store.get("clinical/missing.png"));
    }

    @Test
    void http404FromCompatibleProviderIsNotFound() {
        when(client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("not found").build());

        assertThrows(ResourceNotFoundException.class, () -> store.get("clinical/missing.png"));
    }

    @Test
    void providerFailuresBecomeStorageExceptions() {
        when(client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(SdkClientException.create("timeout"));
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("boom").build());

        assertThrows(StorageException.class, () -> store.get("clinical/a.png"));
        assertThrows(StorageException.class,
                () -> store.put("clinical/a.png", new ByteArrayInputStream(new byte[]{1}), 1, "image/png"));
    }

    @Test
    void deleteAndCopyUseTheConfiguredBucket() {
        store.delete("website/draft/hero/a.png");
        store.copy("website/draft/hero/a.png", "website/hero/a.png");

        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(delete.capture());
        assertEquals("clinica-archivos", delete.getValue().bucket());
        assertEquals("website/draft/hero/a.png", delete.getValue().key());

        ArgumentCaptor<CopyObjectRequest> copy = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(client).copyObject(copy.capture());
        assertEquals("website/draft/hero/a.png", copy.getValue().sourceKey());
        assertEquals("website/hero/a.png", copy.getValue().destinationKey());
        assertEquals("clinica-archivos", copy.getValue().destinationBucket());
    }

    @Test
    void rejectsInvalidKeysBeforeCallingTheProvider() {
        assertThrows(IllegalArgumentException.class, () -> store.get("../other-bucket-object"));
        assertThrows(IllegalArgumentException.class, () -> store.delete("/absolute"));
        verifyNoInteractions(client);
    }

    @Test
    void requiresBucketAndCredentials() {
        assertThrows(IllegalStateException.class,
                () -> new S3BlobStore("", "", "auto", "key", "secret", true));
        assertThrows(IllegalStateException.class,
                () -> new S3BlobStore("bucket", "", "auto", "", "secret", true));
        assertThrows(IllegalStateException.class,
                () -> new S3BlobStore("bucket", "", "auto", "key", " ", true));
        verify(client, never()).close();
    }
}
