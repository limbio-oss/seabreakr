package io.limb.seabreakr;

public class CallTimeoutException extends SeaBreakrException {
    public CallTimeoutException() {
    }

    public CallTimeoutException(String message) {
        super(message);
    }

    public CallTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public CallTimeoutException(Throwable cause) {
        super(cause);
    }

    public CallTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
