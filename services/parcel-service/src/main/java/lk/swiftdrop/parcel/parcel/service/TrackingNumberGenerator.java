package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.parcel.parcel.repository.ParcelRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TrackingNumberGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ParcelRepository parcelRepository;

    public TrackingNumberGenerator(ParcelRepository parcelRepository) {
        this.parcelRepository = parcelRepository;
    }

    public String generateUniqueTrackingNumber() {
        String trackingNo;
        do {
            trackingNo = generate();
        } while (parcelRepository.existsByTrackingNo(trackingNo));
        return trackingNo;
    }

    private String generate() {
        StringBuilder sb = new StringBuilder("SD-");
        for (int i = 0; i < 4; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        sb.append("-");
        for (int i = 0; i < 4; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
