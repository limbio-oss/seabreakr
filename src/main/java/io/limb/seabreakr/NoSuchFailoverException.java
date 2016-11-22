package io.limb.seabreakr;

public class NoSuchFailoverException extends SeaBreakrException {
    public NoSuchFailoverException() {
    }

    public NoSuchFailoverException(String message) {
        super(message);
    }

    public NoSuchFailoverException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchFailoverException(Throwable cause) {
        super(cause);
    }

    public NoSuchFailoverException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
