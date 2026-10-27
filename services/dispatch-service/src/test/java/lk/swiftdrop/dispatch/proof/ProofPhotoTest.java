package lk.swiftdrop.dispatch.proof;

import static org.assertj.core.api.Assertions.assertThat;

import lk.swiftdrop.dispatch.proof.ProofPhoto.ImageType;
import org.junit.jupiter.api.Test;

class ProofPhotoTest {

    @Test
    void recognisesJpegPngAndWebpByTheirFirstBytes() {
        assertThat(photo(0xFF, 0xD8, 0xFF, 0xE0).detectType()).contains(ImageType.JPEG);
        assertThat(photo(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0).detectType()).contains(ImageType.PNG);
        assertThat(photo('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P').detectType()).contains(ImageType.WEBP);
    }

    @Test
    void rejectsFilesThatOnlyPretendToBeImages() {
        assertThat(new ProofPhoto("<?php echo 1; ?>".getBytes()).detectType()).isEmpty();
        assertThat(photo('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E').detectType()).isEmpty();
    }

    @Test
    void rejectsFilesTooShortToBeAnImage() {
        assertThat(photo(0xFF, 0xD8).detectType()).isEmpty();
        assertThat(photo().detectType()).isEmpty();
    }

    @Test
    void imageTypesCarryTheirStoredContentTypeAndExtension() {
        assertThat(ImageType.PNG.contentType()).isEqualTo("image/png");
        assertThat(ImageType.WEBP.extension()).isEqualTo("webp");
    }

    private static ProofPhoto photo(int... bytes) {
        byte[] content = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            content[i] = (byte) bytes[i];
        }
        return new ProofPhoto(content);
    }
}
