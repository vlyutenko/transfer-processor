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

public class AccountOperationsProcessorTest {

    private AccountOperationsProcessor accountOperationsProcessor;

    @BeforeClass
    public void init() {
        this.accountOperationsProcessor = new AccountOperationsProcessor();
        accountOperationsProcessor.start();
    }

    @Test
    public void shouldNotCreateAccountWithNegativeAmount() {
        //Given
        //When
        accountOperationsProcessor.processCreate(-1, s -> {
        }, th -> {
            //Then
            assertThat(th instanceof IllegalArgumentException).isTrue();
        });
    }

    @Test
    public void shouldCreateAccountWithPositiveAmount() {
        //Given
        //When
        accountOperationsProcessor.processCreate(1000, s -> {
            //Then
            assertThat(StringUtils.isEmpty(s)).isFalse();
        }, th -> {
        });
    }

    @Test
    public void shouldNotGetInfoIfNotPresent() {
        //Given
        //When
        accountOperationsProcessor.processInfo(UUID.fromString("1473b088-f333-11e9-a713-2a2ae2dbcce4"), s -> {
        }, th -> {
            //Then
            assertThat(th instanceof IllegalArgumentException).isTrue();
        });
    }

    @Test
    public void shouldGetInfoIfPresent() throws Exception {
        //Given
        AtomicReference<String> payload = new AtomicReference<>();
        accountOperationsProcessor.processCreate(1000, payload::set, th -> {
        });

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertThat(payload.get()).isNotNull();
                });

        UUID uuid = UUID.fromString((String) NettyHttpUtil.extractPostRequestBody(payload.get()).get("account"));

        //When
        accountOperationsProcessor.processInfo(uuid, s -> {
            //Then
            assertThat(StringUtils.isEmpty(s)).isFalse();
        }, th -> {
        });
    }

    @Test
    public void shouldCorrectTransfer() throws Exception {
        //Given
        long accountFromAmount = 1000;
        long accountToAmount = 1000;
        long transferAmount = 100;

        //Add accountFrom
        AtomicReference<String> payloadFrom = new AtomicReference<>();
        accountOperationsProcessor.processCreate(accountFromAmount, payloadFrom::set, th -> {
        });

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
        accountOperationsProcessor.processCreate(accountToAmount, payloadTo::set, th -> {
        });

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
        AtomicReference<String> result = new AtomicReference<>();

        accountOperationsProcessor.processTransfer(uuidFrom, uuidTo, transferAmount, result::set, th -> {
        });

        await()
                .atMost(Duration.FIVE_SECONDS)
                .pollInterval(Duration.ONE_MILLISECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertThat(result.get()).isNotNull();
                });

        //Then
        accountOperationsProcessor.processInfo(uuidFrom, s -> {
            //Then
            long amount = -1;
            try {
                amount = ((Long) NettyHttpUtil.extractPostRequestBody(s).get("amount"));
            } catch (Exception ignored) {
            }
            assertThat(amount).isEqualTo(accountFromAmount - transferAmount);
        }, th -> {
        });

        accountOperationsProcessor.processInfo(uuidTo, s -> {
            //Then
            long amount = -1;
            try {
                amount = ((Long) NettyHttpUtil.extractPostRequestBody(s).get("amount"));
            } catch (Exception ignored) {
            }
            assertThat(amount).isEqualTo(accountToAmount + transferAmount);
        }, th -> {
        });
    }
}
