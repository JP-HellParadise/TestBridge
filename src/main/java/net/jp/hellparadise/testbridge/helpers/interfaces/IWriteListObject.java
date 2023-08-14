package net.jp.hellparadise.testbridge.helpers.interfaces;

import net.minecraft.network.PacketBuffer;

@FunctionalInterface
public interface IWriteListObject<T> {
    void writeObject(PacketBuffer buffer, T value);
}
