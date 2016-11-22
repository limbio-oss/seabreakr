package io.limb.seabreakr;

public class SeaBreakrException extends RuntimeException {
    public SeaBreakrException() {
    }

    public SeaBreakrException(String message) {
        super(message);
    }

    public SeaBreakrException(String message, Throwable cause) {
        super(message, cause);
    }

    public SeaBreakrException(Throwable cause) {
        super(cause);
    }

    public SeaBreakrException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
