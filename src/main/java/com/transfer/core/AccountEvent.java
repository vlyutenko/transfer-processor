package com.transfer.core;

import io.netty.channel.ChannelHandlerContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public class AccountEvent {
    long sequence;
    public ChannelHandlerContext ctx;
    public UUID accountFrom;
    public UUID accountTo;
    public long amount;
    public EventType eventType;
    public BiConsumer<ChannelHandlerContext, String> resultConsumer;
    public BiConsumer<ChannelHandlerContext, Throwable> errorConsumer;
}
