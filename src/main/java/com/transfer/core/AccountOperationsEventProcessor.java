package com.transfer.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AccountOperationsEventProcessor implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountOperationsEventProcessor.class);

    private final Disruptor<AccountEvent> disruptor;
    private final TObjectLongHashMap<UUID> storage;
    private final RingBuffer<AccountEvent> ringBuffer;

    public AccountOperationsEventProcessor() {
        this.disruptor = new Disruptor<>(AccountEvent::new, 256, new ThreadFactoryBuilder().setNameFormat("disruptor.executor-%d").build(), ProducerType.MULTI, new BusySpinWaitStrategy());
        this.storage = new TObjectLongHashMap<>(10, 0.5f, -1);
        this.disruptor.handleEventsWith(this::handleEvent);
        this.ringBuffer = this.disruptor.start();
    }

    public AccountEvent nextEvent() {
        long sequence = ringBuffer.next();
        AccountEvent event = ringBuffer.get(sequence);
        event.sequence = sequence;
        return event;
    }

    public void publishEvent(long eventSequence) {
        this.ringBuffer.publish(eventSequence);
    }

    private void handleEvent(AccountEvent event, long sequence, boolean endOfBatch) {
        String response;
        try {
            switch (event.eventType) {
                case CREATE: {
                    response = createAccount(event);
                    break;
                }
                case INFO: {
                    response = accountInfo(event);
                    break;
                }
                case TRANSFER: {
                    response = transfer(event);
                    break;
                }
                default: {
                    event.errorConsumer.accept(event.ctx, new IllegalArgumentException("Not supported operation"));
                    return;
                }
            }
            event.resultConsumer.accept(event.ctx, response);
        } catch (Exception ex) {
            LOGGER.error("Problems during event processing", ex);
            event.errorConsumer.accept(event.ctx, ex);
        }
    }

    private String transfer(AccountEvent event) {
        UUID fromAccount = event.accountFrom;
        long fromAmount = storage.get(fromAccount);
        if (fromAmount == -1) {
            throw new IllegalArgumentException("from account not present in storage");
        }

        UUID toAccount = event.accountTo;
        long toAmount = storage.get(toAccount);

        if (toAmount == -1) {
            throw new IllegalArgumentException("to account not present in storage");
        }

        long amount = event.amount;

        if (amount < 0) {
            throw new IllegalArgumentException("Should not be less then 0");
        }

        if (fromAmount < amount) {
            throw new IllegalArgumentException("Not enough money for transfer");
        }

        storage.put(fromAccount, fromAmount - amount);
        storage.put(toAccount, toAmount + amount);

        LOGGER.info("Transfer from account {} to account {}, amount {}", fromAccount, toAccount, amount);

        return "{\"status\":\"success\" }";
    }

    private String accountInfo(AccountEvent event) {
        UUID uuid = event.accountFrom;
        long amount = storage.get(uuid);
        if (amount == -1) {
            throw new IllegalArgumentException("account not present");
        }

        LOGGER.info("{} available amount for account {}", amount, uuid);
        return String.format("{\"amount\":%d }", amount);
    }

    private String createAccount(AccountEvent event) {
        long amount = event.amount;
        if (amount < 0) {
            throw new IllegalArgumentException("Should not be less then 0");
        }
        UUID uuid = UUID.randomUUID();
        storage.put(uuid, amount);

        LOGGER.info("{} account created with amount {}", uuid, amount);
        return String.format("{\"account\":\"%s\" }", uuid.toString());
    }

    @Override
    public void close() {
        disruptor.shutdown();
    }
}
