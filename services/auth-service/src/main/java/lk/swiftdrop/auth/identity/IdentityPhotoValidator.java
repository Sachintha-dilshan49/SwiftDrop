package lk.swiftdrop.auth.identity;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import lk.swiftdrop.common.error.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class IdentityPhotoValidator {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;
    private static final int MAX_DIMENSION = 6000;

    public ValidatedPhoto validate(MultipartFile photo) throws IOException {
        if (photo == null || photo.isEmpty() || photo.getSize() > MAX_BYTES) {
            throw new ValidationException("Each identity photo must be between 1 byte and 5 MB.");
        }
        byte[] bytes;
        try (InputStream input = photo.getInputStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new ValidationException("Each identity photo must be between 1 byte and 5 MB.");
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidPhoto();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String contentType = switch (format) {
                    case "jpeg", "jpg" -> "image/jpeg";
                    case "png" -> "image/png";
                    default -> throw invalidPhoto();
                };
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw new ValidationException("Identity photo dimensions are too large.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw invalidPhoto();
                return new ValidatedPhoto(bytes, contentType);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IndexOutOfBoundsException exception) {
            throw invalidPhoto();
        }
    }

    private static ValidationException invalidPhoto() {
        return new ValidationException("Identity photo must be a readable JPEG or PNG image.");
    }

    public record ValidatedPhoto(byte[] bytes, String contentType) {}
}
