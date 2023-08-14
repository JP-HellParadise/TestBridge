package net.jp.hellparadise.testbridge.helpers;

import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.jp.hellparadise.testbridge.helpers.interfaces.IReadListObject;
import net.jp.hellparadise.testbridge.helpers.interfaces.IWriteListObject;
import net.jp.hellparadise.testbridge.helpers.interfaces.ListSupplier;
import net.minecraft.network.PacketBuffer;

// Handle simple data object (Such as int, String)
public class ListSyncHandler<T> extends ValueSyncHandler<List<T>> {

    private List<T> cache;
    private final ListSupplier<T> getter;
    private final IWriteListObject<T> writeHandler;
    private final IReadListObject<T> readHandler;

    public ListSyncHandler(ListSupplier<T> getter, IWriteListObject<T> writeHandler, IReadListObject<T> readHandler) {
        this.getter = getter;
        this.writeHandler = writeHandler;
        this.readHandler = readHandler;
    }

    @Override
    public List<T> getValue() {
        return cache;
    }

    @Override
    public void setValue(List<T> value, boolean setSource, boolean sync) {
        this.cache = value;
    }

    @Override
    public boolean updateCacheFromSource(boolean isFirstSync) {
        if (this.getter != null && (isFirstSync || this.getter.getAsList().stream().anyMatch(it -> !cache.contains(it)))) {
            setValue(this.getter.getAsList(), false, false);
            return true;
        }
        return false;
    }

    @Override
    public void write(PacketBuffer buffer) {
        if (cache == null) {
            buffer.writeInt(-1);
        } else {
            buffer.writeInt(cache.size());
            for (T value : cache) {
                writeHandler.writeObject(buffer, value);
            }
        }
    }

    @Override
    public void read(PacketBuffer buffer) {
        int size = buffer.readInt();
        if (size == -1) return;

        List<T> readList = new ObjectArrayList<>(size);

        for (int i = 0 ; i < size ; i++) {
            readList.add(readHandler.readObject(buffer));
        }

        setValue(readList, false, false);
    }
}
