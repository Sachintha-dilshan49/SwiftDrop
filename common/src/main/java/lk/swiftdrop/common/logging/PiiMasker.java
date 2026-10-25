package lk.swiftdrop.common.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Last line of defence that masks personal data and tokens in log output. It does not make it
 * acceptable to log personal data: log IDs, not people (AGENTS.md §5.2).
 *
 * <ul>
 *   <li>Bearer tokens and JWTs become {@code [TOKEN]}</li>
 *   <li>Email addresses become {@code [EMAIL]}</li>
 *   <li>Sri Lankan and international phone numbers become {@code [PHONE]}</li>
 *   <li>Values of address-like keys ({@code address=}, {@code "deliveryAddress":}, {@code street=})
 *       become {@code [ADDRESS]}; free-text addresses cannot be detected reliably</li>
 * </ul>
 */
public final class PiiMasker {

    static final String TOKEN = "[TOKEN]";
    static final String EMAIL = "[EMAIL]";
    static final String PHONE = "[PHONE]";
    static final String ADDRESS = "[ADDRESS]";

    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*");
    private static final Pattern EMAIL_ADDRESS = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    // +94 / 0094 / 0 followed by 9 digits, optionally grouped: 0771234567, 077 123 4567, +94-11-234-5678
    private static final Pattern LK_PHONE = Pattern.compile(
            "(?<![\\w+-])(?:\\+94|0094|0)[\\s-]?\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{4}(?![\\w-])");
    private static final Pattern INTERNATIONAL_PHONE = Pattern.compile(
            "(?<![\\w+])\\+\\d{1,3}(?:[\\s-]?\\d){6,12}(?![\\w-])");

    // key=value (toString of records/maps) or "key": "value" (JSON); the key ends in "address" or is "street"
    private static final Pattern ADDRESS_FIELD = Pattern.compile(
            "(?i)(\"?\\b(?:[a-z]*address(?:line\\d?)?|street)\"?\\s*[:=]\\s*)"
                    + "(\"(?:[^\"\\\\]|\\\\.)*\"|.+?(?=,\\s*\"?\\w+\"?\\s*[:=]|[\\]})\\r\\n]|$))");

    private PiiMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String masked = BEARER.matcher(text).replaceAll("Bearer " + TOKEN);
        masked = JWT.matcher(masked).replaceAll(TOKEN);
        masked = maskAddresses(masked);
        masked = EMAIL_ADDRESS.matcher(masked).replaceAll(EMAIL);
        masked = LK_PHONE.matcher(masked).replaceAll(PHONE);
        return INTERNATIONAL_PHONE.matcher(masked).replaceAll(PHONE);
    }

    private static String maskAddresses(String text) {
        Matcher matcher = ADDRESS_FIELD.matcher(text);
        StringBuilder out = new StringBuilder(text.length());
        while (matcher.find()) {
            String value = matcher.group(2);
            String replacement = value.startsWith("\"") ? "\"" + ADDRESS + "\"" : ADDRESS;
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(1) + replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
