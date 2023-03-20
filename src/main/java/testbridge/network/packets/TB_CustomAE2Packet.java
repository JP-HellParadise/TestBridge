package testbridge.network.packets;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;

import net.minecraftforge.fml.common.Optional;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.me.GridAccessException;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.PacketHandler;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import testbridge.container.ContainerPackage;
import testbridge.container.ContainerSatelliteSelect;
import testbridge.helpers.interfaces.ICraftingManagerHost;
import testbridge.items.FakeItem;
import testbridge.network.packets.gui.ProvideSatResultListPacket;
import testbridge.network.packets.pipehandler.TB_SyncNamePacket;
import testbridge.part.PartCraftingManager;
import testbridge.part.PartSatelliteBus;

@StaticResolve
public class TB_CustomAE2Packet<T> extends CoordinatesPacket {
  @Getter
  @Setter
  private String key;

  @Getter
  @Setter
  private String value;

  @Getter
  @Setter
  private ItemStack is;

  @Getter
  @Setter
  private boolean setting;

  @Getter
  @Setter
  private List<T> list;

  public TB_CustomAE2Packet(int id) {
    super(id);
  }

  @Override
  @Optional.Method(modid = "appliedenergistics2")
  public void processPacket(EntityPlayer player) {
    final Container c = player.openContainer;
    if (this.key.startsWith("Package.Opening") && ((!player.getHeldItem(EnumHand.MAIN_HAND).isEmpty() && player.getHeldItem(EnumHand.MAIN_HAND)
        .getItem() instanceof FakeItem) || (!player.getHeldItem(EnumHand.OFF_HAND).isEmpty() && player.getHeldItem(EnumHand.OFF_HAND).getItem() instanceof FakeItem))
      && c instanceof ContainerPackage) {
      final EnumHand hand;
      if (!player.getHeldItem(EnumHand.MAIN_HAND).isEmpty() && player.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof FakeItem) {
        hand = EnumHand.MAIN_HAND;
      } else if (!player.getHeldItem(EnumHand.OFF_HAND).isEmpty() && player.getHeldItem(EnumHand.OFF_HAND).getItem() instanceof FakeItem) {
        hand = EnumHand.OFF_HAND;
      } else {
        return;
      }

      final ItemStack pkg = player.getHeldItem(hand);

      if (pkg.getItem() instanceof FakeItem) {
        NBTTagCompound newNBT = new NBTTagCompound();
        newNBT.setBoolean("__actContainer", false);
        if (!value.isEmpty())
          newNBT.setString("__pkgDest", value);
        if (is != null && !is.isEmpty())
          newNBT.setTag("__itemHold", is.writeToNBT(new NBTTagCompound()));
        pkg.setTagCompound(newNBT);
      }
      if (setting) {
        player.setHeldItem(hand, pkg);
        player.closeScreen();
      }
    }
    if (this.key.startsWith("CMSatellite.") && c instanceof ContainerSatelliteSelect) {
      final ContainerSatelliteSelect qk = (ContainerSatelliteSelect) c;
      if (this.key.equals("CMSatellite.Opening")) {
        openSatelliteSelect(player);
      } else if (this.key.equals("CMSatellite.Setting")) {
        qk.setSatName(this.value != null ? value : "");
      }
    }
  }

  @Optional.Method(modid = "appliedenergistics2")
  private void openSatelliteSelect(EntityPlayer player) {
    List<String> list = new ArrayList<>();
    final Container c = player.openContainer;
    if (c instanceof ContainerSatelliteSelect) {
      ICraftingManagerHost cmHost = ((ContainerSatelliteSelect) c).getCMHost();
      if (cmHost != null) {
        // GET current satellite bus select
        CoordinatesPacket packet = PacketHandler.getPacket(TB_SyncNamePacket.class)
            .setSide(cmHost instanceof PartCraftingManager ? ((PartCraftingManager) cmHost).getSide().ordinal() : 0)
            .setString(cmHost.getSatelliteName()).setTilePos(cmHost.getTileEntity());
        MainProxy.sendPacketToPlayer(packet, player);
        // GET list of satellite bus
        try {
          for (final IGridNode node : cmHost.getCMDuality().gridProxy.getGrid().getMachines(PartSatelliteBus.class)) {
            IGridHost h = node.getMachine();
            if (h instanceof PartSatelliteBus) {
              PartSatelliteBus part = (PartSatelliteBus) h;
              if (!part.getSatellitePipeName().equals("")) {
                list.add(part.getSatellitePipeName());
              }
            }
          }
        } catch (final GridAccessException ignore) {
          // :P
        }
      }
    }
    MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ProvideSatResultListPacket.class).setStringList(list), player);
  }

  @Override
  public ModernPacket template() {
    return new TB_CustomAE2Packet<>(getId());
  }

  @Override
  public void writeData(LPDataOutput output) {
    super.writeData(output);
    output.writeUTF(this.key);
    output.writeUTF(this.value);
    output.writeItemStack(this.is != null ? is : new ItemStack(Items.AIR));
    output.writeBoolean(this.setting);
  }

  @Override
  public void readData(LPDataInput input) {
    super.readData(input);
    this.key = input.readUTF();
    this.value = input.readUTF();
    this.is = input.readItemStack();
    this.setting = input.readBoolean();
  }
}
