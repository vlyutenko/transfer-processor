package com.transfer;

import dagger.Module;
import dagger.Provides;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.net.InetSocketAddress;

@Module
public class TransferModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferModule.class);

    private static final String SERVER_PORT_PROPERTY_NAME = "server.port";
    private static final int DEFAULT_SERVER_PORT = 80;

    @Provides
    @Singleton
    CompositeConfiguration providesConfiguration() {
        CompositeConfiguration config = new CompositeConfiguration();
        try {
            config.addConfiguration(new PropertiesConfiguration("configuration.properties"));
        } catch (Exception ex) {
            LOGGER.warn("Problems during loading configuration", ex);
        }

        return config;
    }

    @Provides
    InetSocketAddress providesInetSocketAddress(CompositeConfiguration properties) {
        return new InetSocketAddress(properties.getInt(SERVER_PORT_PROPERTY_NAME, DEFAULT_SERVER_PORT));
    }
}
