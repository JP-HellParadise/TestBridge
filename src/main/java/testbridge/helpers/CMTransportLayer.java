package testbridge.helpers;

import net.minecraft.util.EnumFacing;

import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifierStack;

import testbridge.pipes.PipeCraftingManager;

public class CMTransportLayer extends TransportLayer {

  private final PipeCraftingManager pipe;

  public CMTransportLayer(PipeCraftingManager pipe) {
    this.pipe = pipe;
  }

  @Override
  public EnumFacing itemArrived(IRoutedItem item, EnumFacing blocked) {
    if (item.getItemIdentifierStack() != null) {
      pipe.recievedItem(item.getItemIdentifierStack().getStackSize());
    }
    return pipe.getPointedOrientation();
  }

  @Override
  public boolean stillWantItem(IRoutedItem item) {
    LogisticsModule module = pipe.getLogisticsModule();
    if (module == null) {
      pipe.notifyOfItemArival(item.getInfo());
      return false;
    }
    if (!pipe.isEnabled()) {
      pipe.notifyOfItemArival(item.getInfo());
      return false;
    }
    final ItemIdentifierStack itemIDStack = item.getItemIdentifierStack();
    SinkReply reply = module.sinksItem(itemIDStack.makeNormalStack(), itemIDStack.getItem(), -1, 0, true, false, false);
    if (reply == null || reply.maxNumberOfItems < 0) {
      pipe.notifyOfItemArival(item.getInfo());
      return false;
    }

    if (reply.maxNumberOfItems > 0 && itemIDStack.getStackSize() > reply.maxNumberOfItems) {
      EnumFacing o = pipe.getPointedOrientation();
      if (o == null) {
        o = EnumFacing.UP;
      }

      item.split(reply.maxNumberOfItems, o);
    }
    return true;
  }

}
