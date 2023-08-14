package net.jp.hellparadise.testbridge.helpers.interfaces;

import java.util.List;

@FunctionalInterface
public interface ListSupplier<T> {
    List<T> getAsList();
}
