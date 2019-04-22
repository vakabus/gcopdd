package cz.cuni.mff.d3s.blood.utils;

import java.util.function.Consumer;

@FunctionalInterface
public interface CheckedConsumer<T> extends Consumer<T> {

    void checkedAccept(T t) throws Exception;

    @Override
    default void accept(T t) {
        try {
            checkedAccept(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
