package com.rbkmoney.fraudbusters.mg.connector.config;

import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.fistful.withdrawal.ManagementSrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class HgConfig {

    @Bean
    public InvoicingSrv.Iface invoicingClient(@Value("${service.invoicing.url}") Resource resource,
                                              @Value("${service.invoicing.networkTimeout}") int networkTimeout)
            throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(InvoicingSrv.Iface.class);
    }

    @Bean
    public ManagementSrv.Iface withdrawalClient(
            @Value("${service.withdrawal.url}") Resource resource,
            @Value("${service.withdrawal.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(ManagementSrv.Iface.class);
    }

    @Bean
    public com.rbkmoney.fistful.destination.ManagementSrv.Iface destinationClient(
            @Value("${service.destination.url}") Resource resource,
            @Value("${service.destination.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(com.rbkmoney.fistful.destination.ManagementSrv.Iface.class);
    }

    @Bean
    public com.rbkmoney.fistful.wallet.ManagementSrv.Iface walletClient(
            @Value("${service.wallet.url}") Resource resource,
            @Value("${service.wallet.networkTimeout}") int networkTimeout) throws IOException {
        return new THSpawnClientBuilder()
                .withNetworkTimeout(networkTimeout)
                .withAddress(resource.getURI()).build(com.rbkmoney.fistful.wallet.ManagementSrv.Iface.class);
    }

}
