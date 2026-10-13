package dev.ryanhcode.sable.network.udp;

import io.netty.channel.Channel;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.minecraft.server.network.EventLoopGroupHolder;

/**
 * Picks the datagram channel class matching the transport that vanilla chose for its TCP connections.
 * In 1.21.11 the event loop groups are owned by {@link EventLoopGroupHolder}, so the datagram channel has to
 * be compatible with the holder's group.
 */
public final class SableUDPTransport {

    private SableUDPTransport() {
    }

    public static Class<? extends Channel> datagramChannelClass(final EventLoopGroupHolder holder) {
        final Class<? extends Channel> tcpClass = holder.channelCls();
        if (tcpClass == EpollSocketChannel.class) {
            return EpollDatagramChannel.class;
        }
        if (tcpClass == KQueueSocketChannel.class) {
            return KQueueDatagramChannel.class;
        }
        return NioDatagramChannel.class;
    }
}
