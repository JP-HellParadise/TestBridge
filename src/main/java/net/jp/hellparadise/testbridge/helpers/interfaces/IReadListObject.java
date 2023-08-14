package net.jp.hellparadise.testbridge.helpers.interfaces;

import net.minecraft.network.PacketBuffer;

@FunctionalInterface
public interface IReadListObject<T> {
    T readObject(PacketBuffer buffer);
}
