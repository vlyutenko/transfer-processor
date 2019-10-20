package com.transfer.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetSocketAddress;

@Singleton
public class NettyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);
    private static final int BYTES_IN_MEGABYTE = 1048576;
    private static final int MAX_FRAME_LENGTH = 4 * BYTES_IN_MEGABYTE;

    private final ServerBootstrap bootstrap;
    private final InetSocketAddress tcpPort;
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup(3);

    private volatile Channel serverChannel;

    @Inject
    public NettyServer(InetSocketAddress tcpPort,
                       ApplicationInboundHandler applicationInboundHandler,
                       FlowExceptionInboundHandler flowExceptionInboundHandler) {
        this.tcpPort = tcpPort;
        this.bootstrap = new ServerBootstrap();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        socketChannel.pipeline().addLast(
                                new IdleStateHandler(0, 0, 120),
                                new HttpServerCodec(),
                                new HttpObjectAggregator(MAX_FRAME_LENGTH),
                                applicationInboundHandler,
                                flowExceptionInboundHandler
                        );
                    }
                });
    }

    public void start() throws Exception {
        LOGGER.info("Starting http server at " + tcpPort);
        serverChannel = bootstrap.bind(tcpPort).sync().channel();
    }

    public void stop() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        serverChannel.close();
    }

}
