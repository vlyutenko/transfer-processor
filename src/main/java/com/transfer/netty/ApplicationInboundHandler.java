package com.transfer.netty;


import com.transfer.core.AccountOperationsProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static com.transfer.netty.NettyHttpUtil.*;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;

@Singleton
@ChannelHandler.Sharable
public class ApplicationInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInboundHandler.class);

    static final String ACCOUNT_CREATE_REQUEST = "/account/create";
    static final String ACCOUNT_INFO_REQUEST = "/account/info";
    static final String TRANSFER_REQUEST = "/account/transfer";

    static final String ACCOUNT_REQUEST_PARAMETER = "account";
    static final String ACCOUNT_FROM_REQUEST_PARAMETER = "fromAccount";
    static final String ACCOUNT_TO_REQUEST_PARAMETER = "toAccount";
    static final String AMOUNT_REQUEST_PARAMETER = "amount";

    private final AccountOperationsProcessor accountOperationsProcessor;

    @Inject
    public ApplicationInboundHandler(AccountOperationsProcessor accountOperationsProcessor) {
        super(true);
        this.accountOperationsProcessor = accountOperationsProcessor;
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

    private void handlePost(ChannelHandlerContext channelHandlerContext,
                            String payload,
                            String uri) throws Exception {
        Map parameters;
        switch (uri) {
            case TRANSFER_REQUEST:
                parameters = extractPostRequestBody(payload);
                accountOperationsProcessor.processTransfer(
                        UUID.fromString((String) parameters.get(ACCOUNT_FROM_REQUEST_PARAMETER)),
                        UUID.fromString((String) parameters.get(ACCOUNT_TO_REQUEST_PARAMETER)),
                        (Long) parameters.get(AMOUNT_REQUEST_PARAMETER),
                        successHandler(channelHandlerContext),
                        errorHandler(channelHandlerContext));
                break;
            case ACCOUNT_CREATE_REQUEST:
                parameters = extractPostRequestBody(payload);
                accountOperationsProcessor.processCreate(
                        (Long) parameters.get(AMOUNT_REQUEST_PARAMETER),
                        successHandler(channelHandlerContext),
                        errorHandler(channelHandlerContext));
                break;
            default:
                LOGGER.warn("Not valid operation: {}", uri);
                send404NotFound(channelHandlerContext);
                break;
        }
    }

    private void handleGet(ChannelHandlerContext channelHandlerContext,
                           Map<String, List<String>> parameters,
                           String uri) {
        if (ACCOUNT_INFO_REQUEST.equals(uri)) {
            extractGetRequestParameter(parameters, ACCOUNT_REQUEST_PARAMETER)
                    .ifPresentOrElse(account -> accountOperationsProcessor.processInfo(
                            UUID.fromString(account),
                            successHandler(channelHandlerContext),
                            errorHandler(channelHandlerContext)),
                            () -> send400BadRequest(channelHandlerContext, "Invalid request account parameter"));
        } else {
            LOGGER.warn("Not valid operation: {}", uri);
            send404NotFound(channelHandlerContext);
        }
    }

    /**
     * TODO:
     * lambda captures outside values and instance is create every time
     * could be omitted see branch https://github.com/vlyutenko/transfer-processor/tree/singleton_lambda
     * but downside is that processor will be tightly coupled with transport
     */
    private Consumer<String> successHandler(ChannelHandlerContext ctx) {
        return s -> send200Ok(ctx, Unpooled.wrappedBuffer(s.getBytes()));
    }

    private Consumer<Throwable> errorHandler(ChannelHandlerContext ctx) {
        return th -> send500InternalServerError(ctx, th);
    }
}
