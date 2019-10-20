package com.transfer;


import org.agrona.concurrent.SigIntBarrier;

public class Runner {

    public static void main(String[] args) throws Exception {

        TransferComponent transferComponent = DaggerTransferComponent.builder().build();

        try (TransferApplication transferApplication = transferComponent.exchangeApplication()) {
            transferApplication.start();
            new SigIntBarrier().await();
        }
    }
}
