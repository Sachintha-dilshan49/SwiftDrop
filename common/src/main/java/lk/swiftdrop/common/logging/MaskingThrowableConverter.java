package lk.swiftdrop.common.logging;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;

/** Logback {@code %maskedEx}: stack traces with personal data in exception messages masked. */
public class MaskingThrowableConverter extends ExtendedThrowableProxyConverter {

    @Override
    protected String throwableProxyToString(IThrowableProxy throwableProxy) {
        return PiiMasker.mask(super.throwableProxyToString(throwableProxy));
    }
}
