package com.transfer;

import com.transfer.core.AccountOperationsProcessor;
import com.transfer.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransferApplication implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferApplication.class);

    @Inject
    NettyServer nettyServer;
    @Inject
    AccountOperationsProcessor accountOperationsProcessor;

    @Inject
    public TransferApplication() {
    }

    public void start() throws Exception {
        LOGGER.info("About to start exchange application");
        nettyServer.start();
        accountOperationsProcessor.start();
    }

    public void close() {
        LOGGER.info("About to stop exchange application");
        nettyServer.stop();
        accountOperationsProcessor.close();
    }
}
