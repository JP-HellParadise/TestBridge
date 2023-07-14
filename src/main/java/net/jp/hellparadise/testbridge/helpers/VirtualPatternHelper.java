package net.jp.hellparadise.testbridge.helpers;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerNull;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import java.util.*;
import net.jp.hellparadise.testbridge.core.TB_ItemHandlers;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

public class VirtualPatternHelper implements ICraftingPatternDetails, Comparable<VirtualPatternHelper> {

    public static final int PROCESSING_INPUT_HEIGHT = 4;
    public static final int PROCESSING_INPUT_WIDTH = 4;
    public static final int PROCESSING_INPUT_LIMIT = PROCESSING_INPUT_HEIGHT * PROCESSING_INPUT_WIDTH;

    private final ItemStack patternItem;
    private final InventoryCrafting crafting;
    private final InventoryCrafting testFrame;
    private final IAEItemStack[] condensedInputs;
    private final IAEItemStack[] condensedOutputs;
    private final IAEItemStack[] inputs;
    private final IAEItemStack[] outputs;
    private final Set<TestLookup> failCache = new HashSet<>();
    private final Set<TestLookup> passCache = new HashSet<>();
    private final IAEItemStack pattern;
    private int priority = 0;

    public VirtualPatternHelper(final ItemStack is) {
        final NBTTagCompound encodedValue = is.getTagCompound();

        if (encodedValue == null) {
            throw new IllegalArgumentException("No pattern here!");
        }

        final NBTTagList inTag = encodedValue.getTagList("in", 10);
        final NBTTagList outTag = encodedValue.getTagList("out", 10);

        crafting = new InventoryCrafting(new ContainerNull(), 4, 4);
        testFrame = new InventoryCrafting(new ContainerNull(), 4, 4);

        this.patternItem = is;
        this.pattern = AEItemStack.fromItemStack(is);

        final List<IAEItemStack> in = new ArrayList<>();
        final List<IAEItemStack> out = new ArrayList<>();

        for (int x = 0; x < inTag.tagCount(); x++) {
            NBTTagCompound ingredient = inTag.getCompoundTagAt(x);
            final ItemStack gs = new ItemStack(ingredient);

            if (ingredient.hasKey("stackSize")) {
                gs.setCount(ingredient.getInteger("stackSize"));
            }

            if (!ingredient.isEmpty() && gs.isEmpty()) {
                throw new IllegalArgumentException("No pattern here!");
            }

            if (ingredient.getCompoundTag("tag")
                .hasKey("__actContainer")) {
                gs.getTagCompound()
                    .setBoolean("__actContainer", true);
            }

            this.crafting.setInventorySlotContents(x, gs);

            if (!gs.isEmpty() && !gs.hasTagCompound()) {
                this.markItemAs(x, gs, TestStatus.ACCEPT);
            }

            in.add(
                AEApi.instance()
                    .storage()
                    .getStorageChannel(IItemStorageChannel.class)
                    .createStack(gs));
            this.testFrame.setInventorySlotContents(x, gs);
        }

        for (int x = 0; x < outTag.tagCount(); x++) {
            NBTTagCompound resultItemTag = outTag.getCompoundTagAt(x);
            final ItemStack gs = new ItemStack(resultItemTag);

            if (resultItemTag.hasKey("stackSize")) {
                gs.setCount(resultItemTag.getInteger("stackSize"));
            }

            if (!resultItemTag.isEmpty() && gs.isEmpty()) {
                throw new IllegalArgumentException("No pattern here!");
            }

            if (!gs.isEmpty()) {
                out.add(
                    AEApi.instance()
                        .storage()
                        .getStorageChannel(IItemStorageChannel.class)
                        .createStack(gs));
            }
        }

        final int outputLength = out.size();

        this.inputs = in.toArray(new IAEItemStack[PROCESSING_INPUT_LIMIT]);
        this.outputs = out.toArray(new IAEItemStack[outputLength]);

        final Map<IAEItemStack, IAEItemStack> tmpOutputs = new HashMap<>();

        for (final IAEItemStack io : this.outputs) {
            if (io == null) {
                continue;
            }

            final IAEItemStack g = tmpOutputs.get(io);

            if (g == null) {
                tmpOutputs.put(io, io.copy());
            } else {
                g.add(io);
            }
        }

        final Map<IAEItemStack, IAEItemStack> tmpInputs = new HashMap<>();

        for (final IAEItemStack io : this.inputs) {
            if (io == null) {
                continue;
            }

            final IAEItemStack g = tmpInputs.get(io);

            if (g == null) {
                tmpInputs.put(io, io.copy());
            } else {
                g.add(io);
            }
        }

        if (tmpOutputs.isEmpty() || tmpInputs.isEmpty()) {
            throw new IllegalStateException("No pattern here!");
        }

        this.condensedInputs = new IAEItemStack[tmpInputs.size()];
        int offset = 0;

        for (final IAEItemStack io : tmpInputs.values()) {
            this.condensedInputs[offset] = io;
            offset++;
        }

        offset = 0;
        this.condensedOutputs = new IAEItemStack[tmpOutputs.size()];

        for (final IAEItemStack io : tmpOutputs.values()) {
            this.condensedOutputs[offset] = io;
            offset++;
        }
    }

    public VirtualPatternHelper(final ItemStack input, final ItemStack output) {
        // Create new pattern and generate TagCompound
        ItemStack pattern = new ItemStack(TB_ItemHandlers.virtualPattern);
        pattern.setTagCompound(new NBTTagCompound());
        NBTTagCompound patternData = pattern.getTagCompound();

        crafting = new InventoryCrafting(new ContainerNull(), 1, 1);
        testFrame = new InventoryCrafting(new ContainerNull(), 1, 1);

        // Create NBTTagList each input and output
        NBTTagList newList = new NBTTagList();
        patternData.setTag("in", newList);
        newList.appendTag(input.writeToNBT(new NBTTagCompound()));

        newList = new NBTTagList();
        patternData.setTag("out", newList);
        newList.appendTag(output.writeToNBT(new NBTTagCompound()));

        // Definition
        this.patternItem = pattern;
        this.pattern = AEItemStack.fromItemStack(pattern);

        this.crafting.setInventorySlotContents(0, input);
        this.testFrame.setInventorySlotContents(0, input);

        this.condensedInputs = this.inputs = new IAEItemStack[] { AEItemStack.fromItemStack(input) };
        this.condensedOutputs = this.outputs = new IAEItemStack[] { AEItemStack.fromItemStack(output) };
    }

    private void markItemAs(final int slotIndex, final ItemStack i, final TestStatus b) {
        if (b == TestStatus.TEST || i.hasTagCompound()) {
            return;
        }

        (b == TestStatus.ACCEPT ? this.passCache : this.failCache).add(new TestLookup(slotIndex, i));
    }

    @Override
    public ItemStack getPattern() {
        return this.patternItem;
    }

    @Override
    public synchronized boolean isValidItemForSlot(final int slotIndex, final ItemStack i, final World w) {
        return true;
    }

    @Override
    public boolean isCraftable() {
        return false;
    }

    @Override
    public IAEItemStack[] getInputs() {
        return this.inputs;
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return this.condensedInputs;
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return condensedOutputs;
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return outputs;
    }

    @Override
    public boolean canSubstitute() {
        return true;
    }

    @Override
    public List<IAEItemStack> getSubstituteInputs(int slot) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getOutput(final InventoryCrafting craftingInv, final World w) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(final int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(final VirtualPatternHelper o) {
        return Integer.compare(o.priority, this.priority);
    }

    @Override
    public int hashCode() {
        return this.pattern.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final VirtualPatternHelper other = (VirtualPatternHelper) obj;

        if (this.pattern != null && other.pattern != null) {
            return this.pattern.equals(other.pattern);
        }
        return false;
    }

    private enum TestStatus {
        ACCEPT,
        DECLINE,
        TEST
    }

    private static final class TestLookup {

        private final int slot;
        private final int ref;
        private final int hash;

        public TestLookup(final int slot, final ItemStack i) {
            this(slot, i.getItem(), i.getItemDamage());
        }

        public TestLookup(final int slot, final Item item, final int dmg) {
            this.slot = slot;
            this.ref = (dmg << Platform.DEF_OFFSET) | (Item.getIdFromItem(item) & 0xffff);
            final int offset = 3 * slot;
            this.hash = (this.ref << offset) | (this.ref >> (offset + 32));
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        public boolean equals(final Object obj) {
            final boolean equality;

            if (obj instanceof TestLookup) {
                final TestLookup b = (TestLookup) obj;

                equality = b.slot == this.slot && b.ref == this.ref;
            } else {
                equality = false;
            }

            return equality;
        }
    }
}
