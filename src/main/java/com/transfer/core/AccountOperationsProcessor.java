package com.transfer.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public class AccountOperationsProcessor implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountOperationsProcessor.class);

    private final Disruptor<AccountEvent> disruptor;
    private final TObjectLongHashMap<UUID> storage;

    @Inject
    public AccountOperationsProcessor() {
        this.disruptor = new Disruptor<>(AccountEvent::new, 256, new ThreadFactoryBuilder().setNameFormat("disruptor.executor-%d").build(), ProducerType.MULTI, new BusySpinWaitStrategy());
        this.storage = new TObjectLongHashMap<>(10, 0.5f, -1);
    }

    public void start() {
        LOGGER.info("Starting disruptor");
        this.disruptor.handleEventsWith(this::handleEvent);
        this.disruptor.start();
    }

    @Override
    public void close() {
        disruptor.shutdown();
    }

    private enum EventType {
        CREATE, INFO, TRANSFER
    }

    private static final class AccountEvent {
        private UUID accountFrom;
        private UUID accountTo;
        private long amount;
        private EventType eventType;
        private Consumer<String> resultConsumer;
        private Consumer<Throwable> errorConsumer;
    }

    public void processCreate(long amount, Consumer<String> resultConsumer, Consumer<Throwable> errorConsumer) {
        disruptor.publishEvent((event, sequence) -> {
            event.eventType = EventType.CREATE;
            event.amount = amount;
            event.resultConsumer = resultConsumer;
            event.errorConsumer = errorConsumer;
        });
    }

    public void processInfo(UUID account, Consumer<String> resultConsumer, Consumer<Throwable> errorConsumer) {
        disruptor.publishEvent((event, sequence) -> {
            event.eventType = EventType.INFO;
            event.accountFrom = account;
            event.resultConsumer = resultConsumer;
            event.errorConsumer = errorConsumer;
        });
    }

    public void processTransfer(UUID accountFrom, UUID accountTo, long amount, Consumer<String> resultConsumer, Consumer<Throwable> errorConsumer) {
        disruptor.publishEvent((event, sequence) -> {
            event.eventType = EventType.TRANSFER;
            event.accountFrom = accountFrom;
            event.accountTo = accountTo;
            event.amount = amount;
            event.resultConsumer = resultConsumer;
            event.errorConsumer = errorConsumer;
        });
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
                    event.errorConsumer.accept(new IllegalArgumentException("Not supported operation"));
                    return;
                }
            }
            event.resultConsumer.accept(response);
        } catch (Exception ex) {
            LOGGER.error("Problems during event processing", ex);
            event.errorConsumer.accept(ex);
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
}
