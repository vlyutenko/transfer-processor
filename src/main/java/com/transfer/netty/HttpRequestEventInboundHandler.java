package com.transfer.netty;


import com.transfer.core.AccountEvent;
import com.transfer.core.AccountOperationsEventProcessor;
import com.transfer.core.EventType;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.transfer.netty.NettyHttpUtil.*;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;

@ChannelHandler.Sharable
public class HttpRequestEventInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestEventInboundHandler.class);

    static final String ACCOUNT_CREATE_REQUEST = "/account/create";
    static final String ACCOUNT_INFO_REQUEST = "/account/info";
    static final String TRANSFER_REQUEST = "/account/transfer";

    static final String ACCOUNT_REQUEST_PARAMETER = "account";
    static final String ACCOUNT_FROM_REQUEST_PARAMETER = "fromAccount";
    static final String ACCOUNT_TO_REQUEST_PARAMETER = "toAccount";
    static final String AMOUNT_REQUEST_PARAMETER = "amount";

    private final AccountOperationsEventProcessor accountOperationsEventProcessor;

    public HttpRequestEventInboundHandler(AccountOperationsEventProcessor accountOperationsEventProcessor) {
        super(true);
        this.accountOperationsEventProcessor = accountOperationsEventProcessor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest message) throws Exception {
        HttpMethod httpMethod = message.method();
        var decodedUri = URLDecoder.decode(message.uri(), StandardCharsets.UTF_8);
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(decodedUri, true);
        String uri = queryStringDecoder.path();

        if (httpMethod == POST) {
            String payload = message.content().toString(CharsetUtil.UTF_8);
            handlePost(
                    context,
                    payload,
                    uri);
        } else if (httpMethod == GET) {
            Map<String, List<String>> parameters = queryStringDecoder.parameters();
            handleGet(
                    context,
                    parameters,
                    uri);
        }
    }

    private void handlePost(ChannelHandlerContext ctx,
                            String payload,
                            String uri) throws Exception {
        AccountEvent event;
        switch (uri) {
            case TRANSFER_REQUEST:
                event = accountOperationsEventProcessor.nextEvent();
                setupTransferEvent(event, payload, ctx);
                accountOperationsEventProcessor.publishEvent(event);
                break;
            case ACCOUNT_CREATE_REQUEST:
                event = accountOperationsEventProcessor.nextEvent();
                setupCreateEvent(event, payload, ctx);
                accountOperationsEventProcessor.publishEvent(event);
                break;
            default:
                LOGGER.warn("Not valid operation: {}", uri);
                send404NotFound(ctx);
                break;
        }
    }

    private void handleGet(ChannelHandlerContext ctx,
                           Map<String, List<String>> parameters,
                           String uri) throws Exception {

        AccountEvent event;
        switch (uri) {
            case ACCOUNT_INFO_REQUEST:
                event = accountOperationsEventProcessor.nextEvent();
                setupInfoEvent(event, extractGetRequestParameter(parameters, ACCOUNT_REQUEST_PARAMETER), ctx);
                accountOperationsEventProcessor.publishEvent(event);
                break;
            default:
                LOGGER.warn("Not valid operation: {}", uri);
                send404NotFound(ctx);
                break;
        }
    }

    private void setupTransferEvent(AccountEvent event, String payload, ChannelHandlerContext ctx) throws Exception {
        Map parameters = extractPostRequestBody(payload);
        event.eventType = EventType.TRANSFER;
        event.ctx = ctx;
        event.accountFrom = UUID.fromString((String) parameters.get(ACCOUNT_FROM_REQUEST_PARAMETER));
        event.accountTo = UUID.fromString((String) parameters.get(ACCOUNT_TO_REQUEST_PARAMETER));
        event.amount = (Long) parameters.get(AMOUNT_REQUEST_PARAMETER);
        event.resultConsumer = NettyHttpUtil::send200Ok;
        event.errorConsumer = NettyHttpUtil::send500InternalServerError;
    }

    private void setupCreateEvent(AccountEvent event, String payload, ChannelHandlerContext ctx) throws Exception {
        Map parameters = extractPostRequestBody(payload);
        event.eventType = EventType.CREATE;
        event.ctx = ctx;
        event.amount = (Long) parameters.get(AMOUNT_REQUEST_PARAMETER);
        event.resultConsumer = NettyHttpUtil::send200Ok;
        event.errorConsumer = NettyHttpUtil::send500InternalServerError;
    }

    private void setupInfoEvent(AccountEvent event, String account, ChannelHandlerContext ctx) throws Exception {
        event.eventType = EventType.INFO;
        event.ctx = ctx;
        event.accountFrom = UUID.fromString(account);
        event.resultConsumer = NettyHttpUtil::send200Ok;
        event.errorConsumer = NettyHttpUtil::send500InternalServerError;
    }
}
