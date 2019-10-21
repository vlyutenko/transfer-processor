package com.transfer.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ChannelHandler.Sharable
public class FlowExceptionInboundHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowExceptionInboundHandler.class);

    public FlowExceptionInboundHandler() {
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Sending fail response", cause);
        NettyHttpUtil.send500InternalServerError(ctx, cause);
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            LOGGER.debug("Idle state event {}", ctx.channel().remoteAddress());
            ctx.close();
        }
    }
}
