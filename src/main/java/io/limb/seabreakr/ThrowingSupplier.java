package io.limb.seabreakr;

import java.util.function.Supplier;

public interface ThrowingSupplier<T> extends Supplier<T> {

    T supply() throws Exception;

    @Override
    default T get() {
        try {
            return supply();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
}
