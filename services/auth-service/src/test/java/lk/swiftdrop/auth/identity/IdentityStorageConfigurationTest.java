package lk.swiftdrop.auth.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdentityStorageConfigurationTest {
    private final IdentityStorageConfiguration configuration = new IdentityStorageConfiguration();

    @Test
    void acceptsHttpsEndpointAndSeparateBucket() {
        assertThat(configuration.identityMinioClient(
                "https://identity-minio:9000", "access", "secret", "identity-documents"))
                .isNotNull();
    }

    @Test
    void rejectsUnencryptedEndpoint() {
        assertThatThrownBy(() -> configuration.identityMinioClient(
                "http://identity-minio:9000", "access", "secret", "identity-documents"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsPublicDeliveryProofsBucket() {
        assertThatThrownBy(() -> configuration.identityMinioClient(
                "https://identity-minio:9000", "access", "secret", "delivery-proofs"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingCredentialsAndCredentialsInUrl() {
        assertThatThrownBy(() -> configuration.identityMinioClient(
                "https://identity-minio:9000", "", "secret", "identity-documents"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.identityMinioClient(
                "https://user:password@identity-minio:9000", "access", "secret", "identity-documents"))
                .isInstanceOf(IllegalStateException.class);
    }
}
