package net.jp.hellparadise.testbridge.pipes.upgrades;

import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.pipes.upgrades.*;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.item.SimpleStackInventory;
import net.jp.hellparadise.testbridge.pipes.PipeCraftingManager;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class ModuleUpgradeManager implements ISimpleInventoryEventHandler, ISlotUpgradeManager {

    private final UpgradeManager parent;
    private final SimpleStackInventory inv = new SimpleStackInventory(2, "UpgradeInventory", 16);
    private final IPipeUpgrade[] upgrades = new IPipeUpgrade[2];
    private final PipeCraftingManager pipe;
    private EnumFacing sneakyOrientation = null;
    private boolean isAdvancedCrafter = false;
    private boolean isFuzzyUpgrade = false;
    private int liquidCrafter = 0;
    private boolean hasByproductExtractor = false;
    private boolean hasPatternUpgrade = false;
    private int craftingCleanup = 0;
    private int actionSpeedUpgrade = 0;
    private int itemExtractionUpgrade = 0;
    private int itemStackExtractionUpgrade = 0;
    private boolean[] guiUpgrades = new boolean[2];

    public ModuleUpgradeManager(PipeCraftingManager pipe, UpgradeManager parent) {
        this.pipe = pipe;
        this.parent = parent;
        this.inv.addListener(this);
    }

    public boolean hasPatternUpgrade() {
        return this.hasPatternUpgrade || this.parent.hasPatternUpgrade();
    }

    public boolean isAdvancedSatelliteCrafter() {
        return this.isAdvancedCrafter || this.parent.isAdvancedSatelliteCrafter();
    }

    public boolean hasByproductExtractor() {
        return this.hasByproductExtractor || this.parent.hasByproductExtractor();
    }

    public int getFluidCrafter() {
        return Math.min(this.liquidCrafter + this.parent.getFluidCrafter(), 3);
    }

    public boolean isFuzzyUpgrade() {
        return this.isFuzzyUpgrade || this.parent.isFuzzyUpgrade();
    }

    public int getCrafterCleanup() {
        return Math.min(this.craftingCleanup + this.parent.getCrafterCleanup(), 4);
    }

    public boolean hasSneakyUpgrade() {
        return this.sneakyOrientation != null || this.parent.hasSneakyUpgrade();
    }

    public EnumFacing getSneakyOrientation() {
        return this.sneakyOrientation != null ? this.sneakyOrientation : this.parent.getSneakyOrientation();
    }

    public boolean hasOwnSneakyUpgrade() {
        return this.sneakyOrientation != null;
    }

    public IPipeUpgrade getUpgrade(int slot) {
        return this.upgrades[slot];
    }

    public DoubleCoordinates getPipePosition() {
        return this.pipe.getLPPosition();
    }

    public void InventoryChanged(IInventory inventory) {
        boolean needUpdate = false;

        int i;
        for (i = 0; i < this.inv.getSizeInventory(); ++i) {
            ItemStack item = this.inv.getStackInSlot(i);
            if (item.isEmpty()) {
                if (this.upgrades[i] != null) {
                    needUpdate |= this.removeUpgrade(i, this.upgrades);
                }
            } else {
                needUpdate |= this.updateModule(i, this.upgrades, this.inv);
            }
        }

        this.sneakyOrientation = null;
        this.isAdvancedCrafter = false;
        this.isFuzzyUpgrade = false;
        this.liquidCrafter = 0;
        this.hasByproductExtractor = false;
        this.hasPatternUpgrade = false;
        this.craftingCleanup = 0;
        this.actionSpeedUpgrade = 0;
        this.itemExtractionUpgrade = 0;
        this.itemStackExtractionUpgrade = 0;
        this.guiUpgrades = new boolean[2];

        for (i = 0; i < this.upgrades.length; ++i) {
            IPipeUpgrade upgrade = this.upgrades[i];
            if (upgrade instanceof SneakyUpgradeConfig sneakyConfig && this.sneakyOrientation == null) {
                ItemStack stack = this.inv.getStackInSlot(i);
                this.sneakyOrientation = sneakyConfig.getSide(stack);
            } else if (upgrade instanceof AdvancedSatelliteUpgrade) {
                this.isAdvancedCrafter = true;
            } else if (upgrade instanceof FuzzyUpgrade) {
                this.isFuzzyUpgrade = true;
            } else if (upgrade instanceof FluidCraftingUpgrade) {
                this.liquidCrafter += this.inv.getStackInSlot(i)
                    .getCount();
            } else if (upgrade instanceof CraftingByproductUpgrade) {
                this.hasByproductExtractor = true;
            } else if (upgrade instanceof PatternUpgrade) {
                this.hasPatternUpgrade = true;
            } else if (upgrade instanceof CraftingCleanupUpgrade) {
                this.craftingCleanup += this.inv.getStackInSlot(i)
                    .getCount();
            } else if (upgrade instanceof ActionSpeedUpgrade) {
                this.actionSpeedUpgrade += this.inv.getStackInSlot(i)
                    .getCount();
            } else if (upgrade instanceof ItemExtractionUpgrade) {
                this.itemExtractionUpgrade += this.inv.getStackInSlot(i)
                    .getCount();
            } else if (upgrade instanceof ItemStackExtractionUpgrade) {
                this.itemStackExtractionUpgrade += this.inv.getStackInSlot(i)
                    .getCount();
            }

            if (upgrade instanceof IConfigPipeUpgrade) {
                this.guiUpgrades[i] = true;
            }
        }

        this.liquidCrafter = Math.min(this.liquidCrafter, 3);
        this.craftingCleanup = Math.min(this.craftingCleanup, 4);
        this.itemExtractionUpgrade = Math.min(this.itemExtractionUpgrade, 8);
        this.itemStackExtractionUpgrade = Math.min(this.itemStackExtractionUpgrade, 8);
        if (needUpdate) {
            MainProxy.runOnServer(null, () -> () -> {
                this.pipe.connectionUpdate();
                if (this.pipe.container != null) {
                    this.pipe.container.sendUpdateToClient();
                }
            });
        }

    }

    public void readFromNBT(NBTTagCompound nbttagcompound, String prefix) {
        this.inv.readFromNBT(nbttagcompound, "ModuleUpgradeInventory_" + prefix);
        this.InventoryChanged(this.inv);
    }

    public void writeToNBT(NBTTagCompound nbttagcompound, String prefix) {
        this.inv.writeToNBT(nbttagcompound, "ModuleUpgradeInventory_" + prefix);
        this.InventoryChanged(this.inv);
    }

    private boolean updateModule(int slot, IPipeUpgrade[] upgrades, IInventory inv) {
        ItemStack stackInSlot = inv.getStackInSlot(slot);
        if (!stackInSlot.isEmpty() && stackInSlot.getItem() instanceof ItemUpgrade upgrade) {
            upgrades[slot] = upgrade.getUpgradeForItem(stackInSlot, upgrades[slot]);
        } else {
            upgrades[slot] = null;
        }

        if (upgrades[slot] == null) {
            inv.setInventorySlotContents(slot, ItemStack.EMPTY);
            return false;
        } else {
            return upgrades[slot].needsUpdate();
        }
    }

    private boolean removeUpgrade(int slot, IPipeUpgrade[] upgrades) {
        boolean needUpdate = upgrades[slot].needsUpdate();
        upgrades[slot] = null;
        return needUpdate;
    }

    public int getActionSpeedUpgrade() {
        return this.actionSpeedUpgrade;
    }

    public int getItemExtractionUpgrade() {
        return this.itemExtractionUpgrade;
    }

    public int getItemStackExtractionUpgrade() {
        return this.itemStackExtractionUpgrade;
    }

    public void dropUpgrades() {
        this.inv.dropContents(this.pipe.getWorld(), this.pipe.getPos());
    }

    public SimpleStackInventory getInv() {
        return this.inv;
    }
}
