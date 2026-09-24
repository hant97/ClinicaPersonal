package com.clinica.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** Comprueba que {@code storage.provider} elige una sola implementación y que S3 arranca. */
class BlobStoreSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(LocalBlobStore.class, S3BlobStore.class);

    @Test
    void usesLocalStoreByDefault() {
        runner.withPropertyValues("storage.local.root=target/test-uploads")
                .run(context -> assertThat(context).hasSingleBean(BlobStore.class).hasSingleBean(LocalBlobStore.class));
    }

    @Test
    void usesS3StoreWhenConfigured() {
        runner.withPropertyValues(
                        "storage.provider=s3",
                        "storage.s3.bucket=clinica-archivos",
                        "storage.s3.endpoint=https://example-account.r2.cloudflarestorage.com",
                        "storage.s3.region=auto",
                        "storage.s3.access-key=test-access-key",
                        "storage.s3.secret-key=test-secret-key")
                .run(context -> assertThat(context).hasSingleBean(BlobStore.class).hasSingleBean(S3BlobStore.class));
    }

    @Test
    void failsFastWhenS3CredentialsAreMissing() {
        runner.withPropertyValues("storage.provider=s3", "storage.s3.bucket=clinica-archivos")
                .run(context -> assertThat(context).hasFailed());
    }
}
