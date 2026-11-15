package lk.swiftdrop.ai.model;

/** Rules for model names, shared by the configuration and request validation. */
public final class ModelNames {

    /** Lower-case letters, digits and dashes, 1 to 32 characters. */
    public static final String PATTERN = "^[a-z0-9-]{1,32}$";

    private ModelNames() {
    }
}
