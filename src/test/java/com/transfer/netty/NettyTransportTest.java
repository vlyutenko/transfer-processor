package com.transfer.netty;

import com.transfer.core.AccountOperationsEventProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.awaitility.Duration;
import org.testng.annotations.Test;


import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class NettyTransportTest {

    @Test
    public void shouldCreateAccount() {
        //Given
        EmbeddedChannel channel = new EmbeddedChannel(new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor()), new FlowExceptionInboundHandler());

        //When
        String payload = "{\"" + HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER + "\": 2000}";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.ACCOUNT_CREATE_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channel.writeInbound(httpRequest);

        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channel.readOutbound();
                    assertThat(res.status().code()).isEqualTo(200);
                });
    }

    @Test
    public void shouldNotCreateAccountWithNegativeAmount() {
        //Given
        EmbeddedChannel channel = new EmbeddedChannel(new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor()), new FlowExceptionInboundHandler());

        //When
        String payload = "{\"" + HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER + "\": -2000}";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.ACCOUNT_CREATE_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channel.writeInbound(httpRequest);

        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channel.readOutbound();
                    assertThat(res.status().code()).isEqualTo(500);
                });
    }

    @Test
    public void shouldNotCreateAccountWithWrongAmount() {
        //Given
        EmbeddedChannel channel = new EmbeddedChannel(new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor()), new FlowExceptionInboundHandler());

        //When
        String payload = "{\"" + HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER + "\": XXX}";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.ACCOUNT_CREATE_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channel.writeInbound(httpRequest);

        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channel.readOutbound();
                    assertThat(res.status().code()).isEqualTo(500);
                });
    }

    @Test
    public void shouldNotCreateAccountWithMissedAmount() {
        //Given
        EmbeddedChannel channel = new EmbeddedChannel(new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor()), new FlowExceptionInboundHandler());

        //When
        String payload = "{\"XXX\": XXX}";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.ACCOUNT_CREATE_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channel.writeInbound(httpRequest);

        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channel.readOutbound();
                    assertThat(res.status().code()).isEqualTo(500);
                });
    }

    @Test
    public void shouldCreateAccountAndGetInfo() {
        HttpRequestEventInboundHandler httpRequestEventInboundHandler = new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor());
        FlowExceptionInboundHandler flowExceptionInboundHandler = new FlowExceptionInboundHandler();
        //Given
        EmbeddedChannel channelCreate = new EmbeddedChannel(httpRequestEventInboundHandler, flowExceptionInboundHandler);

        //When
        String payload = "{\"" + HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER + "\": 2000}";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.ACCOUNT_CREATE_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channelCreate.writeInbound(httpRequest);

        AtomicReference<String> uuid = new AtomicReference<>();
        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channelCreate.readOutbound();
                    assertThat(res.status().code()).isEqualTo(200);
                    uuid.set((String) NettyHttpUtil.extractPostRequestBody(res.content().toString(CharsetUtil.UTF_8)).get(HttpRequestEventInboundHandler.ACCOUNT_REQUEST_PARAMETER));
                });

        EmbeddedChannel channelInfo = new EmbeddedChannel(httpRequestEventInboundHandler);
        FullHttpRequest getHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, HttpRequestEventInboundHandler.ACCOUNT_INFO_REQUEST + "?account=" + uuid.get());
        channelInfo.writeInbound(getHttpRequest);

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channelInfo.readOutbound();
                    assertThat(res.status().code()).isEqualTo(200);
                    long amount = (long) NettyHttpUtil.extractPostRequestBody(res.content().toString(CharsetUtil.UTF_8)).get(HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER);
                    assertThat(amount).isEqualTo(2000);
                });
    }

    @Test
    public void shouldNotReturnInfoWithWrongAccount() {
        //Given
        EmbeddedChannel channel = new EmbeddedChannel(new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor()), new FlowExceptionInboundHandler());

        //When
        String uuid = "xxx";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, HttpRequestEventInboundHandler.ACCOUNT_INFO_REQUEST + "?account=" + uuid);
        channel.writeInbound(httpRequest);

        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channel.readOutbound();
                    assertThat(res.status().code()).isEqualTo(500);
                });
    }

    @Test
    public void shouldNotReturnInfoWithNotPresentAccount() {
        //Given
        EmbeddedChannel channel = new EmbeddedChannel(new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor()), new FlowExceptionInboundHandler());

        //When
        String uuid = "1473b088-f333-11e9-a713-2a2ae2dbcce4";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, HttpRequestEventInboundHandler.ACCOUNT_INFO_REQUEST + "?account=" + uuid);
        channel.writeInbound(httpRequest);

        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channel.readOutbound();
                    assertThat(res.status().code()).isEqualTo(500);
                });
    }

    @Test
    public void shouldNotReturnInfoWithMissedAccount() {
        //Given
        EmbeddedChannel channel = new EmbeddedChannel(new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor()), new FlowExceptionInboundHandler());

        //When
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, HttpRequestEventInboundHandler.ACCOUNT_INFO_REQUEST);
        channel.writeInbound(httpRequest);

        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channel.readOutbound();
                    assertThat(res.status().code()).isEqualTo(500);
                });
    }

    @Test
    public void shouldTransferCorrect() {
        //Given
        HttpRequestEventInboundHandler httpRequestEventInboundHandler = new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor());
        FlowExceptionInboundHandler flowExceptionInboundHandler = new FlowExceptionInboundHandler();

        EmbeddedChannel channelCreateFrom = new EmbeddedChannel(httpRequestEventInboundHandler, flowExceptionInboundHandler);

        //When
        String payload = "{\"" + HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER + "\": 2000}";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.ACCOUNT_CREATE_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channelCreateFrom.writeInbound(httpRequest);

        AtomicReference<String> uuidFrom = new AtomicReference<>();
        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channelCreateFrom.readOutbound();
                    assertThat(res.status().code()).isEqualTo(200);
                    uuidFrom.set((String) NettyHttpUtil.extractPostRequestBody(res.content().toString(CharsetUtil.UTF_8)).get(HttpRequestEventInboundHandler.ACCOUNT_REQUEST_PARAMETER));
                });

        EmbeddedChannel channelCreateTo = new EmbeddedChannel(httpRequestEventInboundHandler, flowExceptionInboundHandler);
        httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.ACCOUNT_CREATE_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channelCreateTo.writeInbound(httpRequest);
        AtomicReference<String> uuidTo = new AtomicReference<>();
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channelCreateTo.readOutbound();
                    assertThat(res.status().code()).isEqualTo(200);
                    uuidTo.set((String) NettyHttpUtil.extractPostRequestBody(res.content().toString(CharsetUtil.UTF_8)).get(HttpRequestEventInboundHandler.ACCOUNT_REQUEST_PARAMETER));
                });

        payload = "{\"" + HttpRequestEventInboundHandler.ACCOUNT_FROM_REQUEST_PARAMETER + "\":\"" + uuidFrom.get() + "\"," +
                "\"" + HttpRequestEventInboundHandler.ACCOUNT_TO_REQUEST_PARAMETER + "\":\"" + uuidTo.get() + "\"," +
                "\"" + HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER + "\": 100}";
        EmbeddedChannel channelTransfer = new EmbeddedChannel(httpRequestEventInboundHandler);
        httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.TRANSFER_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channelTransfer.writeInbound(httpRequest);

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channelTransfer.readOutbound();
                    assertThat(res.status().code()).isEqualTo(200);
                });
    }

    @Test
    public void shouldNotTransferWithNotPresentAccounts() {
        //Given
        String uuidFrom = "1473b088-f333-11e9-a713-2a2ae2dbcce4";
        String uuidTo = "1473b088-f333-11e9-a713-2a2ae2dbcce4";

        String payload = "{\"" + HttpRequestEventInboundHandler.ACCOUNT_FROM_REQUEST_PARAMETER + "\":\"" + uuidFrom + "\"," +
                "\"" + HttpRequestEventInboundHandler.ACCOUNT_TO_REQUEST_PARAMETER + "\":\"" + uuidTo + "\"," +
                "\"" + HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER + "\": 100}";
        EmbeddedChannel channel = new EmbeddedChannel(new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor()), new FlowExceptionInboundHandler());
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.TRANSFER_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channel.writeInbound(httpRequest);

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channel.readOutbound();
                    assertThat(res.status().code()).isEqualTo(500);
                });
    }

    @Test
    public void shouldNotTransferIfNotEnoughAmount() {
        //Given
        HttpRequestEventInboundHandler httpRequestEventInboundHandler = new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor());
        FlowExceptionInboundHandler flowExceptionInboundHandler = new FlowExceptionInboundHandler();

        EmbeddedChannel channelCreateFrom = new EmbeddedChannel(httpRequestEventInboundHandler, flowExceptionInboundHandler);

        //When
        String payload = "{\"" + HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER + "\": 2000}";
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.ACCOUNT_CREATE_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channelCreateFrom.writeInbound(httpRequest);

        AtomicReference<String> uuidFrom = new AtomicReference<>();
        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channelCreateFrom.readOutbound();
                    assertThat(res.status().code()).isEqualTo(200);
                    uuidFrom.set((String) NettyHttpUtil.extractPostRequestBody(res.content().toString(CharsetUtil.UTF_8)).get(HttpRequestEventInboundHandler.ACCOUNT_REQUEST_PARAMETER));
                });

        EmbeddedChannel channelCreateTo = new EmbeddedChannel(httpRequestEventInboundHandler, flowExceptionInboundHandler);
        httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.ACCOUNT_CREATE_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channelCreateTo.writeInbound(httpRequest);
        AtomicReference<String> uuidTo = new AtomicReference<>();
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channelCreateTo.readOutbound();
                    assertThat(res.status().code()).isEqualTo(200);
                    uuidTo.set((String) NettyHttpUtil.extractPostRequestBody(res.content().toString(CharsetUtil.UTF_8)).get(HttpRequestEventInboundHandler.ACCOUNT_REQUEST_PARAMETER));
                });

        payload = "{\"" + HttpRequestEventInboundHandler.ACCOUNT_FROM_REQUEST_PARAMETER + "\":\"" + uuidFrom.get() + "\"," +
                "\"" + HttpRequestEventInboundHandler.ACCOUNT_TO_REQUEST_PARAMETER + "\":\"" + uuidTo.get() + "\"," +
                "\"" + HttpRequestEventInboundHandler.AMOUNT_REQUEST_PARAMETER + "\": 100000}";
        EmbeddedChannel channelTransfer = new EmbeddedChannel(httpRequestEventInboundHandler);
        httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, HttpRequestEventInboundHandler.TRANSFER_REQUEST, Unpooled.wrappedBuffer(payload.getBytes()));
        channelTransfer.writeInbound(httpRequest);

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channelTransfer.readOutbound();
                    assertThat(res.status().code()).isEqualTo(500);
                });
    }

    @Test
    public void shouldNotProccesIncorrectUrls() {
        //Given
        EmbeddedChannel channel = new EmbeddedChannel(new HttpRequestEventInboundHandler(new AccountOperationsEventProcessor()), new FlowExceptionInboundHandler());

        //When
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "XXX");
        channel.writeInbound(httpRequest);

        //Then
        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    DefaultFullHttpResponse res = channel.readOutbound();
                    assertThat(res.status().code()).isEqualTo(404);
                });
    }
}
