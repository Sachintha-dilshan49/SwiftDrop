package lk.swiftdrop.dispatch.proof;

import lk.swiftdrop.common.error.SwiftDropException;
import org.springframework.http.HttpStatus;

/** 502: the photo store (MinIO) could not be reached. */
public class ProofStorageException extends SwiftDropException {

    public ProofStorageException() {
        super(HttpStatus.BAD_GATEWAY, "proof-storage-unavailable", "Proof storage unavailable",
                "The proof photo could not be saved right now. Try again in a moment.");
    }
}
