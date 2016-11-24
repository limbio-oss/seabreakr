package io.limb.seabreakr;

import reactor.core.Exceptions;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletionException;

final class BreakerExceptions {

    static Throwable unwrapException(Throwable throwable) {
        Throwable t = throwable;
        while (true) {
            t = Exceptions.unwrap(t);
            if (t instanceof InvocationTargetException || t instanceof CompletionException) {
                t = t.getCause();
            } else {
                break;
            }
        }
        return t;
    }

    static RuntimeException rethrow(Throwable throwable) {
        return BreakerExceptions.rethrow0(throwable);
    }

    private static <T extends RuntimeException> RuntimeException rethrow0(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
