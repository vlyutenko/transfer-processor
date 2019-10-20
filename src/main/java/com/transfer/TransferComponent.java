package com.transfer;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
        TransferModule.class,
})
public interface TransferComponent {

    TransferApplication exchangeApplication();
}
