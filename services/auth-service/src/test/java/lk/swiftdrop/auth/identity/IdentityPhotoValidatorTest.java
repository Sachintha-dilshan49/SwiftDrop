package lk.swiftdrop.auth.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import lk.swiftdrop.common.error.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class IdentityPhotoValidatorTest {
    private final IdentityPhotoValidator validator = new IdentityPhotoValidator();

    @Test
    void acceptsImageByActualContentRatherThanUserSuppliedNameOrMimeType() throws Exception {
        var image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        var photo = new MockMultipartFile("photo", "untrusted.jpg", "image/jpeg", output.toByteArray());
        var result = validator.validate(photo);
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.bytes()).isEqualTo(output.toByteArray());
    }

    @Test
    void rejectsNonImageContentAndEmptyFiles() {
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("photo", "photo.png",
                "image/png", "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("photo", new byte[0])))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsFilesOverSizeLimit() {
        var photo = new MockMultipartFile("photo", "photo.jpg", "image/jpeg",
                new byte[IdentityPhotoValidator.MAX_BYTES + 1]);
        assertThatThrownBy(() -> validator.validate(photo)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsImagesWithExcessiveDimensions() throws Exception {
        var image = new BufferedImage(6001, 1, BufferedImage.TYPE_INT_RGB);
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        var photo = new MockMultipartFile("photo", "photo.png", "image/png", output.toByteArray());
        assertThatThrownBy(() -> validator.validate(photo)).isInstanceOf(ValidationException.class);
    }
}
