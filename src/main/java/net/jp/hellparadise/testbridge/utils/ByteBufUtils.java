package net.jp.hellparadise.testbridge.utils;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

public class ByteBufUtils {

    public static byte readByte(ByteBuf buffer) {
        return buffer.readByte();
    }

    public static byte[] readBytes(ByteBuf buffer, int length) {
        byte[] arr = new byte[length];
        buffer.readBytes(arr, 0, length);
        return arr;
    }

    public static byte[] readByteArray(ByteBuf buffer) {
        final int length = readInt(buffer);
        if (length == -1) {
            return null;
        }

        return readBytes(buffer, length);
    }

    public static short readShort(ByteBuf buffer) {
        return buffer.readShort();
    }

    public static int readInt(ByteBuf buffer) {
        return buffer.readInt();
    }

    public static long readLong(ByteBuf buffer) {
        return buffer.readLong();
    }

    public static float readFloat(ByteBuf buffer) {
        return buffer.readFloat();
    }

    public static double readDouble(ByteBuf buffer) {
        return buffer.readDouble();
    }

    public static boolean readBoolean(ByteBuf buffer) {
        return buffer.readBoolean();
    }

    @Nullable
    public static String readUTF(ByteBuf buffer) {
        byte[] arr = readByteArray(buffer);
        if (arr == null) {
            return null;
        } else {
            return new String(arr, StandardCharsets.UTF_8);
        }
    }
}
