package lk.swiftdrop.dispatch.proof;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Stores proof photos in the MinIO delivery-proofs bucket. */
@Component
public class ProofStorage {

    private static final Logger log = LoggerFactory.getLogger(ProofStorage.class);

    private final MinioClient minioClient;
    private final String bucket;

    public ProofStorage(MinioClient minioClient, @Value("${swiftdrop.storage.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /** Uploads the photo under a random, unguessable key and returns that key. */
    public String store(UUID assignmentId, ProofPhoto photo, ProofPhoto.ImageType type) {
        String key = "assignments/" + assignmentId + "/" + UUID.randomUUID() + "." + type.extension();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(new ByteArrayInputStream(photo.content()), photo.content().length, -1)
                    .contentType(type.contentType())
                    .build());
        } catch (Exception e) {
            log.error("Storing proof photo for delivery {} failed: {}", assignmentId, e.getClass().getSimpleName());
            throw new ProofStorageException();
        }
        return key;
    }

    /** Best effort: removes a photo whose status change was rejected, so no orphans are left behind. */
    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            log.warn("Could not remove orphaned proof photo {}: {}", key, e.getClass().getSimpleName());
        }
    }
}
