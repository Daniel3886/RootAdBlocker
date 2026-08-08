package com.android.dnssinkhole.dns;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;

/**
 * Forwards a query we're NOT blocking to a real upstream DNS server
 * (e.g. 1.1.1.1) and hands back its real answer.
 */
@Component
public class DnsForwarder {

    private final SimpleResolver resolver;

    public DnsForwarder(@Value("${sinkhole.upstream-dns:1.1.1.1}") String upstreamHost) throws IOException {
        this.resolver = new SimpleResolver(upstreamHost);
        this.resolver.setTimeout(java.time.Duration.ofSeconds(3));
    }

    public Message forward(Message query) throws IOException {
        return resolver.send(query);
    }
}
