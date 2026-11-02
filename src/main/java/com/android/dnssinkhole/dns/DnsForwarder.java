package com.android.dnssinkhole.dns;

import com.android.dnssinkhole.config.SinkholeProperties;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;

@Component
public class DnsForwarder {

    private final SimpleResolver resolver;

    public DnsForwarder(SinkholeProperties properties) throws IOException {
        this.resolver = new SimpleResolver(properties.upstreamDns());
        this.resolver.setTimeout(properties.upstreamTimeout());
    }

    public Message forward(Message query) throws IOException {
        return resolver.send(query);
    }
}
