package testbridge.logistics;

import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifierStack;

import net.minecraft.util.EnumFacing;

import testbridge.pipes.PipeCraftingManager;

public class CMTransportLayer extends TransportLayer {

  private final PipeCraftingManager _cmPipe;

  public CMTransportLayer(PipeCraftingManager craftingmanagerPipe) {
    _cmPipe = craftingmanagerPipe;
  }

  @Override
  public EnumFacing itemArrived(IRoutedItem item, EnumFacing blocked) {
    if (item.getItemIdentifierStack() != null) {
      _cmPipe.recievedItem(item.getItemIdentifierStack().getStackSize());
    }
    return _cmPipe.getPointedOrientation();
  }

  @Override
  public boolean stillWantItem(IRoutedItem item) {
    LogisticsModule module = _cmPipe.getLogisticsModule();
    if (module == null) {
      _cmPipe.notifyOfItemArival(item.getInfo());
      return false;
    }
    if (!_cmPipe.isEnabled()) {
      _cmPipe.notifyOfItemArival(item.getInfo());
      return false;
    }
    final ItemIdentifierStack itemidStack = item.getItemIdentifierStack();
    SinkReply reply = module.sinksItem(itemidStack.makeNormalStack(), itemidStack.getItem(), -1, 0, true, false, false);
    if (reply == null || reply.maxNumberOfItems < 0) {
      _cmPipe.notifyOfItemArival(item.getInfo());
      return false;
    }

    if (reply.maxNumberOfItems > 0 && itemidStack.getStackSize() > reply.maxNumberOfItems) {
      EnumFacing o = _cmPipe.getPointedOrientation();
      if (o == null) {
        o = EnumFacing.UP;
      }

      item.split(reply.maxNumberOfItems, o);
    }
    return true;
  }

}
