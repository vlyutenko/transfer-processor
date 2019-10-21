package com.transfer.core;

import com.transfer.netty.NettyHttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Duration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AccountOperationsEventProcessorTest {

    private AccountOperationsEventProcessor accountOperationsEventProcessor;

    @BeforeClass
    public void init() {
        this.accountOperationsEventProcessor = new AccountOperationsEventProcessor();
    }

    @Test
    public void shouldNotCreateAccountWithNegativeAmount() {
        //Given
        AccountEvent createEvent = accountOperationsEventProcessor.nextEvent();
        createEvent.eventType = EventType.CREATE;
        createEvent.amount = -1;
        //Then
        createEvent.errorConsumer = (ctx, th) -> assertThat(th instanceof IllegalArgumentException).isTrue();
        //When
        accountOperationsEventProcessor.publishEvent(createEvent.sequence);
    }

    @Test
    public void shouldCreateAccountWithPositiveAmount() {
        //Given
        AccountEvent createEvent = accountOperationsEventProcessor.nextEvent();
        createEvent.eventType = EventType.CREATE;
        createEvent.amount = 1000;
        //Then
        createEvent.resultConsumer = (ctx, s) -> assertThat(StringUtils.isEmpty(s)).isFalse();
        //When
        accountOperationsEventProcessor.publishEvent(createEvent.sequence);
    }

    @Test
    public void shouldNotGetInfoIfNotPresent() {
        //Given
        AccountEvent infoEvent = accountOperationsEventProcessor.nextEvent();
        infoEvent.eventType = EventType.INFO;
        infoEvent.accountFrom = UUID.fromString("1473b088-f333-11e9-a713-2a2ae2dbcce4");
        //Then
        infoEvent.errorConsumer = (ctx, th) -> assertThat(th instanceof IllegalArgumentException).isTrue();
        //When
        accountOperationsEventProcessor.publishEvent(infoEvent.sequence);
    }

    @Test
    public void shouldGetInfoIfPresent() throws Exception {
        //Given
        AtomicReference<String> payload = new AtomicReference<>();

        AccountEvent createEvent = accountOperationsEventProcessor.nextEvent();
        createEvent.eventType = EventType.CREATE;
        createEvent.amount = 1000;
        createEvent.resultConsumer = (ctx, s) -> payload.set(s);
        accountOperationsEventProcessor.publishEvent(createEvent.sequence);

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertThat(payload.get()).isNotNull();
                });

        UUID uuid = UUID.fromString((String) NettyHttpUtil.extractPostRequestBody(payload.get()).get("account"));

        //When
        AccountEvent infoEvent = accountOperationsEventProcessor.nextEvent();
        infoEvent.eventType = EventType.INFO;
        infoEvent.accountFrom = uuid;
        //Then
        infoEvent.resultConsumer = (ctx, s) -> assertThat(StringUtils.isEmpty(s)).isFalse();
        accountOperationsEventProcessor.publishEvent(infoEvent.sequence);
    }

    @Test
    public void shouldCorrectTransfer() throws Exception {
        //Given
        long accountFromAmount = 1000;
        long accountToAmount = 1000;
        long transferAmount = 100;

        //Add accountFrom
        AtomicReference<String> payloadFrom = new AtomicReference<>();

        AccountEvent createEventFrom = accountOperationsEventProcessor.nextEvent();
        createEventFrom.eventType = EventType.CREATE;
        createEventFrom.amount = accountFromAmount;
        createEventFrom.resultConsumer = (ctx, s) -> payloadFrom.set(s);
        accountOperationsEventProcessor.publishEvent(createEventFrom.sequence);

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertThat(payloadFrom.get()).isNotNull();
                });

        UUID uuidFrom = UUID.fromString((String) NettyHttpUtil.extractPostRequestBody(payloadFrom.get()).get("account"));

        // Add accountTo
        AtomicReference<String> payloadTo = new AtomicReference<>();
        AccountEvent createEventTo = accountOperationsEventProcessor.nextEvent();
        createEventTo.eventType = EventType.CREATE;
        createEventTo.amount = accountToAmount;
        createEventTo.resultConsumer = (ctx, s) -> payloadTo.set(s);
        accountOperationsEventProcessor.publishEvent(createEventTo.sequence);

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertThat(payloadTo.get()).isNotNull();
                });

        UUID uuidTo = UUID.fromString((String) NettyHttpUtil.extractPostRequestBody(payloadTo.get()).get("account"));

        //When
        //Perform transfer
        AtomicReference<String> resultTransfer = new AtomicReference<>();
        AccountEvent transferEvent = accountOperationsEventProcessor.nextEvent();
        transferEvent.eventType = EventType.TRANSFER;
        transferEvent.accountFrom = uuidFrom;
        transferEvent.accountTo = uuidTo;
        transferEvent.amount = transferAmount;
        transferEvent.resultConsumer = (ctx, s) -> resultTransfer.set(s);

        accountOperationsEventProcessor.publishEvent(transferEvent.sequence);

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertThat(resultTransfer.get()).isNotNull();
                });

        //Then
        AccountEvent infoEventFrom = accountOperationsEventProcessor.nextEvent();
        infoEventFrom.eventType = EventType.INFO;
        infoEventFrom.accountFrom = uuidFrom;
        infoEventFrom.resultConsumer = (ctx, s) -> {
            //Then
            long amount = -1;
            try {
                amount = ((Long) NettyHttpUtil.extractPostRequestBody(s).get("amount"));
            } catch (Exception ignored) {
            }
            assertThat(amount).isEqualTo(accountFromAmount - transferAmount);
        };
        accountOperationsEventProcessor.publishEvent(infoEventFrom.sequence);

        AccountEvent infoEventTo = accountOperationsEventProcessor.nextEvent();
        infoEventTo.eventType = EventType.INFO;
        infoEventTo.accountFrom = uuidTo;
        infoEventTo.resultConsumer = (ctx, s) -> {
            //Then
            long amount = -1;
            try {
                amount = ((Long) NettyHttpUtil.extractPostRequestBody(s).get("amount"));
            } catch (Exception ignored) {
            }
            assertThat(amount).isEqualTo(accountFromAmount + transferAmount);
        };
        accountOperationsEventProcessor.publishEvent(infoEventTo.sequence);
    }
}
