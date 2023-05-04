package net.jp.hellparadise.testbridge.proxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public interface Proxy {

    @Nonnull
    IThreadListener getThreadListener(MessageContext context);

    @Nullable
    EntityPlayer getPlayer(MessageContext context);

    @Nullable
    World getWorld(MessageContext context);

    class WrongSideException extends RuntimeException {

        private static final long serialVersionUID = 6009692443694310561L;

        WrongSideException(String message) {
            super(message);
        }
    }
}
