package net.jp.hellparadise.testbridge.proxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SuppressWarnings("unused")
@SideOnly(Side.CLIENT)
public class ClientProxy implements Proxy {

    @Nonnull
    @Override
    public IThreadListener getThreadListener(MessageContext context) {
        if (context.side.isClient()) {
            return Minecraft.getMinecraft();
        } else {
            return context.getServerHandler().player.server;
        }
    }

    @Override
    @Nullable public EntityPlayer getPlayer(MessageContext context) {
        if (context.side.isClient()) {
            return Minecraft.getMinecraft().player;
        } else {
            return context.getServerHandler().player;
        }
    }

    @Override
    @Nullable public World getWorld(MessageContext context) {
        if (context.side.isClient()) {
            return Minecraft.getMinecraft().world;
        } else {
            return context.getServerHandler().player.getEntityWorld();
        }
    }
}
