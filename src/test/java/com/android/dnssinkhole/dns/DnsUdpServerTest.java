package com.android.dnssinkhole.dns;

import com.android.dnssinkhole.blocklist.BlocklistService;
import com.android.dnssinkhole.config.TestProperties;
import com.android.dnssinkhole.querylog.QueryLogWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DnsUdpServerTest {

    @Mock
    private BlocklistService blocklistService;

    @Mock
    private DnsForwarder dnsForwarder;

    @Mock
    private QueryLogWriter queryLogWriter;

    private DnsUdpServer dnsUdpServer;
    private DatagramSocket serverSocket;
    private DatagramSocket client;

    @BeforeEach
    void setUp() throws Exception {
        dnsUdpServer = new DnsUdpServer(blocklistService, dnsForwarder, queryLogWriter, TestProperties.defaults());

        serverSocket = new DatagramSocket();
        Field socketField = DnsUdpServer.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(dnsUdpServer, serverSocket);

        client = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        client.setSoTimeout(2000);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        client.close();
        dnsUdpServer.shutdown();
        serverSocket.close();
    }

    @Test
    void blockedDomainReturnsNxdomainAndIsLogged() throws Exception {
        Message query = queryFor("google.com");
        when(blocklistService.isBlocked("google.com.")).thenReturn(true);

        dnsUdpServer.handleQuery(packetFromClient(query));

        Message response = receiveResponse();
        assertEquals(Rcode.NXDOMAIN, response.getRcode());
        assertEquals(query.getHeader().getID(), response.getHeader().getID());
        verify(dnsForwarder, never()).forward(any());
        verify(queryLogWriter).record("google.com", "127.0.0.1", true);
    }

    @Test
    void allowedDomainIsForwardedAndLogged() throws Exception {
        Message query = queryFor("example.com");
        when(blocklistService.isBlocked("example.com.")).thenReturn(false);
        when(dnsForwarder.forward(any(Message.class))).thenReturn(query);

        dnsUdpServer.handleQuery(packetFromClient(query));

        receiveResponse();
        verify(dnsForwarder).forward(any(Message.class));
        verify(queryLogWriter).record("example.com", "127.0.0.1", false);
    }

    @Test
    void upstreamFailureReturnsServfail() throws Exception {
        Message query = queryFor("example.com");
        when(blocklistService.isBlocked("example.com.")).thenReturn(false);
        when(dnsForwarder.forward(any(Message.class))).thenThrow(new IOException("timed out"));

        dnsUdpServer.handleQuery(packetFromClient(query));

        assertEquals(Rcode.SERVFAIL, receiveResponse().getRcode());
    }

    private static Message queryFor(String domain) throws IOException {
        Record question = Record.newRecord(Name.fromString(domain + "."), Type.A, DClass.IN);
        return Message.newQuery(question);
    }

    private DatagramPacket packetFromClient(Message query) {
        byte[] data = query.toWire();
        return new DatagramPacket(data, data.length, client.getLocalAddress(), client.getLocalPort());
    }

    private Message receiveResponse() throws IOException {
        byte[] buffer = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        client.receive(packet);
        Message response = new Message(packet.getData());
        assertEquals(1, response.getSectionArray(Section.QUESTION).length);
        return response;
    }
}
