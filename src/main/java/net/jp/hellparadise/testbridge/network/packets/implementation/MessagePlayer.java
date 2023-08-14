package net.jp.hellparadise.testbridge.network.packets.implementation;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import net.jp.hellparadise.testbridge.core.TestBridge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessagePlayer implements IMessage {

    private ArrayList<String> message;

    public static final MessagePlayer PACKET = new MessagePlayer();

    @Override
    public void fromBytes(ByteBuf buf) {
        message = new ArrayList<>();
        final int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            message.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(message.size());
        for (String s : message) {
            ByteBufUtils.writeUTF8String(buf, s);
        }
    }

    public final ArrayList<String> getMessage() {
        return message;
    }

    public final MessagePlayer setMessage(ArrayList<String> message) {
        this.message = message;
        return this;
    }

    public static class Handler implements IMessageHandler<MessagePlayer, IMessage> {

        @Override
        public IMessage onMessage(MessagePlayer packet, MessageContext ctx) {
            TestBridge.getProxy()
                .getThreadListener(ctx)
                .addScheduledTask(() -> {
                    EntityPlayer player = TestBridge.getProxy()
                        .getPlayer(ctx);
                    if (player != null && packet != null) {
                        for (String key : packet.message) {
                            player.sendMessage(new TextComponentTranslation(key));
                        }
                    }
                });
            return null;
        }
    }
}
