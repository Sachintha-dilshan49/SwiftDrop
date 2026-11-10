package lk.swiftdrop.dispatch.proof;

import java.util.Optional;

/** A proof-of-delivery photo as uploaded by the courier. */
public record ProofPhoto(byte[] content) {

    public enum ImageType {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp");

        private final String contentType;
        private final String extension;

        ImageType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        public String contentType() {
            return contentType;
        }

        public String extension() {
            return extension;
        }
    }

    /** The image type read from the file's first bytes; the name and type the client sends are not trusted. */
    public Optional<ImageType> detectType() {
        if (startsWith(0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(ImageType.JPEG);
        }
        if (startsWith(0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(ImageType.PNG);
        }
        if (startsWith(0, 'R', 'I', 'F', 'F') && startsWith(8, 'W', 'E', 'B', 'P')) {
            return Optional.of(ImageType.WEBP);
        }
        return Optional.empty();
    }

    private boolean startsWith(int offset, int... expected) {
        if (content.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
