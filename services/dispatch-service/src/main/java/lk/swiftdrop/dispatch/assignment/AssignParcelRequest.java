package lk.swiftdrop.dispatch.assignment;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignParcelRequest(
        @NotNull UUID parcelId,
        @NotNull UUID courierId) {
}
