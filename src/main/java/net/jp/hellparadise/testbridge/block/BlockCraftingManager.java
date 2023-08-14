package net.jp.hellparadise.testbridge.block;

import appeng.api.util.AEPartLocation;
import appeng.block.AEBaseTileBlock;
import appeng.util.Platform;
import javax.annotation.Nullable;
import net.jp.hellparadise.testbridge.block.tile.TileEntityCraftingManager;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCraftingManager extends AEBaseTileBlock {

    public BlockCraftingManager() {
        super(Material.IRON);
    }

    @Override
    public boolean onActivated(final World w, final BlockPos pos, final EntityPlayer p, final EnumHand hand,
        final @Nullable ItemStack heldItem, final EnumFacing side, final float hitX, final float hitY,
        final float hitZ) {
        if (p.isSneaking()) {
            return false;
        }

        final TileEntityCraftingManager tg = this.getTileEntity(w, pos);
        if (tg != null) {
            if (Platform.isServer()) {
                Platform.openGUI(p, tg, AEPartLocation.fromFacing(side), tg.getGuiBridge());
            }
            return true;
        }
        return false;
    }
}
