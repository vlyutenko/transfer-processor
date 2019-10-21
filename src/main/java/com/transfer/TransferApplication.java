package com.transfer;

import com.transfer.core.AccountOperationsEventProcessor;
import com.transfer.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferApplication implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferApplication.class);

    private final AccountOperationsEventProcessor accountOperationsEventProcessor;
    private final NettyServer nettyServer;

    TransferApplication() {
        this.accountOperationsEventProcessor = new AccountOperationsEventProcessor();
        this.nettyServer = new NettyServer(accountOperationsEventProcessor);
    }

    public void start() throws Exception {
        LOGGER.info("About to start exchange application");
        nettyServer.start();
    }

    public void close() {
        LOGGER.info("About to stop exchange application");
        nettyServer.stop();
        accountOperationsEventProcessor.close();
    }
}
