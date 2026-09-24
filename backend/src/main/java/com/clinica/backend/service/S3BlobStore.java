package com.clinica.backend.service;

import com.clinica.backend.exception.ResourceNotFoundException;
import com.clinica.backend.exception.StorageException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.net.URI;

/**
 * Almacén en un bucket compatible con S3. Para Cloudflare R2: {@code endpoint}
 * {@code https://<account-id>.r2.cloudflarestorage.com} y {@code region} {@code auto}.
 * <p>
 * El bucket debe ser privado: los archivos se sirven siempre a través de la API, que aplica
 * autenticación y alcance por especialidad.
 */
@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
public class S3BlobStore implements BlobStore {

    private final S3Client client;
    private final String bucket;

    @Autowired
    public S3BlobStore(@Value("${storage.s3.bucket:}") String bucket,
                       @Value("${storage.s3.endpoint:}") String endpoint,
                       @Value("${storage.s3.region:auto}") String region,
                       @Value("${storage.s3.access-key:}") String accessKey,
                       @Value("${storage.s3.secret-key:}") String secretKey,
                       @Value("${storage.s3.path-style:true}") boolean pathStyle) {
        this(buildClient(requireSetting(bucket, "STORAGE_S3_BUCKET"), endpoint, region,
                requireSetting(accessKey, "STORAGE_S3_ACCESS_KEY"),
                requireSetting(secretKey, "STORAGE_S3_SECRET_KEY"), pathStyle), bucket.trim());
    }

    S3BlobStore(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(BlobStore.requireValidKey(key))
                .contentType(contentType)
                .contentLength(size)
                .build();
        try {
            client.putObject(request, RequestBody.fromInputStream(content, size));
        } catch (SdkException ex) {
            throw new StorageException("No se pudo almacenar el archivo", ex);
        }
    }

    @Override
    public Resource get(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(BlobStore.requireValidKey(key))
                .build();
        try {
            byte[] content = client.getObjectAsBytes(request).asByteArray();
            return new NamedByteArrayResource(content, BlobStore.filenameOf(key));
        } catch (NoSuchKeyException ex) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new ResourceNotFoundException("Archivo no encontrado");
            }
            throw new StorageException("No se pudo leer el archivo", ex);
        } catch (SdkException ex) {
            throw new StorageException("No se pudo leer el archivo", ex);
        }
    }

    @Override
    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(BlobStore.requireValidKey(key))
                .build();
        try {
            client.deleteObject(request);
        } catch (SdkException ex) {
            throw new StorageException("No se pudo eliminar el archivo", ex);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey) {
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(BlobStore.requireValidKey(sourceKey))
                .destinationBucket(bucket)
                .destinationKey(BlobStore.requireValidKey(targetKey))
                .build();
        try {
            client.copyObject(request);
        } catch (NoSuchKeyException ex) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        } catch (SdkException ex) {
            throw new StorageException("No se pudo copiar el archivo", ex);
        }
    }

    @PreDestroy
    void close() {
        client.close();
    }

    // bucket se recibe solo para validarlo antes de construir el cliente.
    private static S3Client buildClient(String bucket, String endpoint, String region,
                                        String accessKey, String secretKey, boolean pathStyle) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region == null || region.isBlank() ? "auto" : region.trim()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey.trim(), secretKey.trim())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build())
                // Servicios compatibles (R2, MinIO) no siempre admiten las sumas de verificación
                // que el SDK añade por defecto desde la versión 2.30.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint.trim()));
        }
        return builder.build();
    }

    private static String requireSetting(String value, String variable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variable + " es obligatorio cuando STORAGE_PROVIDER=s3.");
        }
        return value.trim();
    }

    /** Conserva el nombre del archivo para que los controladores deduzcan el tipo de contenido. */
    static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] content, String filename) {
            super(content);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other) && other instanceof NamedByteArrayResource named
                    && filename.equals(named.filename);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + filename.hashCode();
        }
    }
}
