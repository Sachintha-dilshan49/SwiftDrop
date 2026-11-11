package lk.swiftdrop.dispatch.delivery;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.proof.DeliveryProof;
import lk.swiftdrop.dispatch.proof.DeliveryProofRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Status update with a proof photo against real PostgreSQL and MinIO, and a fake parcel-service. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CourierStatusUpdateIT {

    private static final String BUCKET = "delivery-proofs";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static final String MINIO_USER = "proof-test";
    private static final String MINIO_PASSWORD = "proof-test-secret";

    // the official minio/minio images are no longer public; Bitnami's frozen build is the same server
    @Container
    static GenericContainer<?> minio = new GenericContainer<>("bitnamilegacy/minio:2025.7.23")
            .withEnv("MINIO_ROOT_USER", MINIO_USER)
            .withEnv("MINIO_ROOT_PASSWORD", MINIO_PASSWORD)
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    @RegisterExtension
    static WireMockExtension parcelService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.storage.endpoint", CourierStatusUpdateIT::minioUrl);
        registry.add("swiftdrop.storage.access-key", () -> MINIO_USER);
        registry.add("swiftdrop.storage.secret-key", () -> MINIO_PASSWORD);
        registry.add("swiftdrop.storage.bucket", () -> BUCKET);
        registry.add("swiftdrop.services.parcel-url", parcelService::baseUrl);
    }

    // MinIO answers its health check a moment before it accepts S3 calls, so the first call may be dropped
    @BeforeAll
    static void createBucket() throws Exception {
        for (int attempt = 1; ; attempt++) {
            try {
                storageClient().makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
                return;
            } catch (IOException e) {
                if (attempt == 10) {
                    throw e;
                }
                Thread.sleep(500);
            }
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private DeliveryProofRepository deliveryProofRepository;

    @Test
    void deliveredWithPhotoStoresProofInMinioAndCompletesTheAssignment() throws Exception {
        Courier courier = courier();
        Assignment assignment = assign(courier);
        parcelService.stubFor(patch(urlMatching("/internal/parcels/.*/status")).willReturn(aResponse().withStatus(200)));

        mockMvc.perform(multipart("/api/v1/courier/deliveries/{id}/status", assignment.getId())
                        .file(new MockMultipartFile("photo", "door.jpg", "image/jpeg", JPEG))
                        .param("status", "DELIVERED").param("recipientName", "Nimali")
                        .with(signedInAs(courier)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.parcelStatus").value("DELIVERED"));

        List<DeliveryProof> proofs = deliveryProofRepository.findByAssignmentId(assignment.getId());
        assertThat(proofs).hasSize(1);
        String key = proofs.get(0).getObjectKey();
        assertThat(key).startsWith("assignments/" + assignment.getId() + "/").endsWith(".jpg");
        assertThat(storageClient().statObject(StatObjectArgs.builder().bucket(BUCKET).object(key).build())
                .contentType()).isEqualTo("image/jpeg");
        parcelService.verify(patchRequestedFor(urlMatching("/internal/parcels/" + assignment.getParcelId() + "/status"))
                .withRequestBody(matchingJsonPath("$.proofReference", equalTo(key))));
        assertThat(assignmentRepository.findById(assignment.getId()).orElseThrow().getStatus())
                .isEqualTo(AssignmentStatus.COMPLETED);
    }

    @Test
    void rejectedStatusChangeLeavesNoPhotoBehind() throws Exception {
        Courier courier = courier();
        Assignment assignment = assign(courier);
        parcelService.stubFor(patch(urlMatching("/internal/parcels/" + assignment.getParcelId() + "/status"))
                .willReturn(aResponse().withStatus(409)));

        mockMvc.perform(multipart("/api/v1/courier/deliveries/{id}/status", assignment.getId())
                        .file(new MockMultipartFile("photo", "door.jpg", "image/jpeg", JPEG))
                        .param("status", "DELIVERED").with(signedInAs(courier)))
                .andExpect(status().isConflict());

        assertThat(deliveryProofRepository.findByAssignmentId(assignment.getId())).isEmpty();
        assertThat(objectsUnder("assignments/" + assignment.getId() + "/")).isZero();
    }

    private static long objectsUnder(String prefix) {
        return StreamSupport.stream(storageClient().listObjects(
                ListObjectsArgs.builder().bucket(BUCKET).prefix(prefix).recursive(true).build()).spliterator(), false)
                .count();
    }

    private static String minioUrl() {
        return "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
    }

    private static MinioClient storageClient() {
        return MinioClient.builder().endpoint(minioUrl()).credentials(MINIO_USER, MINIO_PASSWORD).build();
    }

    private Courier courier() {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        return courierRepository.save(courier);
    }

    private Assignment assign(Courier courier) {
        Assignment assignment = new Assignment();
        assignment.setParcelId(UUID.randomUUID());
        assignment.setCourierId(courier.getId());
        assignment.setAssignedByUserId(UUID.randomUUID());
        return assignmentRepository.save(assignment);
    }

    private static RequestPostProcessor signedInAs(Courier courier) {
        return jwt().jwt(token -> token.subject(courier.getUserId().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_COURIER"));
    }
}
