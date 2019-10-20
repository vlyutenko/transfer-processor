package com.transfer.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NettyHttpUtil {

    public static void send400BadRequest(ChannelHandlerContext context, String message) {
        ByteBuf errorMessage = Unpooled.wrappedBuffer(message.getBytes());

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, errorMessage);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, errorMessage.readableBytes());
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void send404NotFound(ChannelHandlerContext context) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void send408RequestTimeout(ChannelHandlerContext context) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_TIMEOUT);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void send200Ok(ChannelHandlerContext context, ByteBuf result) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, result);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, result.readableBytes());

        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void send500InternalServerError(ChannelHandlerContext context, Throwable e) {
        ByteBuf errorMessage = Unpooled.wrappedBuffer(e.toString().getBytes());

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorMessage);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, errorMessage.readableBytes());
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private final static JSONParser parser = new JSONParser();
    //TODO should be replaced to not produce garbage
    public static Map extractPostRequestBody(String payload) throws Exception {
        if (StringUtils.isEmpty(payload)) {
            throw new IllegalArgumentException("Payload is empty");
        }
        return (JSONObject) parser.parse(payload);
    }

    static Optional<String> extractGetRequestParameter(Map<String, List<String>> parameters, String parameterName) {
        List<String> account = parameters.get(parameterName);
        if (account == null) {
            return Optional.empty();
        } else {
            return Optional.of(account.get(0));
        }
    }
}
