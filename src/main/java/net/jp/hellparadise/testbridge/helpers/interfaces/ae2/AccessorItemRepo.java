package net.jp.hellparadise.testbridge.helpers.interfaces.ae2;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.prioritylist.IPartitionList;

public interface AccessorItemRepo {
    IPartitionList<IAEItemStack> getMyPartitionList();
    void setMyPartitionList(IPartitionList<IAEItemStack> partitionList);
    void setResort(boolean resort);
}
