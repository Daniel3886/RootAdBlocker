package com.android.dnssinkhole.dns;

import com.android.dnssinkhole.blocklist.BlocklistService;
import com.android.dnssinkhole.model.QueryLog;
import com.android.dnssinkhole.repository.QueryLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnsUdpServerTest {

    @Mock
    private BlocklistService blocklistService;

    @Mock
    private DnsForwarder dnsForwarder;

    @Mock
    private QueryLogRepository queryLogRepository;

    @Test
    void blockedDomainReturnsNxdomainAndIsLogged() throws Exception {

        String domain = "google.com";

        Name name = Name.fromString(domain + ".");
        Record question = Record.newRecord(name, Type.A, DClass.IN);

        Message query = new Message();
        query.addRecord(question, Section.QUESTION);

        byte[] data = query.toWire();

        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getByName("127.0.0.1"),
                12345
        );

        when(blocklistService.isBlocked("google.com."))
                .thenReturn(true);

        DnsUdpServer dnsUdpServer =
                new DnsUdpServer(
                        blocklistService,
                        dnsForwarder,
                        queryLogRepository
                );

        DatagramSocket socket = new DatagramSocket();

        Field socketField = DnsUdpServer.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(dnsUdpServer, socket);

        Method method = DnsUdpServer.class.getDeclaredMethod(
                "handleQuery",
                DatagramPacket.class
        );

        method.setAccessible(true);
        method.invoke(dnsUdpServer, packet);

        verify(blocklistService).isBlocked("google.com.");
        verify(dnsForwarder, never()).forward(any());
        verify(queryLogRepository).save(any());

        socket.close();
    }

    @Test
    void allowedDomainIsForwardedAndLogged() throws Exception {

        String domain = "example.com";

        Name name = Name.fromString(domain + ".");
        Record question = Record.newRecord(name, Type.A, DClass.IN);

        Message query = new Message();
        query.addRecord(question, Section.QUESTION);

        byte[] data = query.toWire();

        DatagramPacket packet =
                new DatagramPacket(
                        data,
                        data.length,
                        InetAddress.getByName("127.0.0.1"),
                        12345
                );

        when(blocklistService.isBlocked("example.com."))
                .thenReturn(false);

        when(dnsForwarder.forward(any(Message.class)))
                .thenReturn(query);

        DnsUdpServer dnsUdpServer =
                new DnsUdpServer(
                        blocklistService,
                        dnsForwarder,
                        queryLogRepository
                );

        DatagramSocket socket = new DatagramSocket();

        Field socketField = DnsUdpServer.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(dnsUdpServer, socket);

        Method method = DnsUdpServer.class.getDeclaredMethod(
                "handleQuery",
                DatagramPacket.class
        );

        method.setAccessible(true);
        method.invoke(dnsUdpServer, packet);

        verify(blocklistService).isBlocked("example.com.");
        verify(dnsForwarder).forward(any(Message.class));
        verify(queryLogRepository).save(any(QueryLog.class));

        socket.close();
    }
}
