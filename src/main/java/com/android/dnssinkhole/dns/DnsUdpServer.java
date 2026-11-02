package com.android.dnssinkhole.dns;

import com.android.dnssinkhole.blocklist.BlocklistService;
import com.android.dnssinkhole.model.QueryLog;
import com.android.dnssinkhole.repository.QueryLogRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DnsUdpServer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DnsUdpServer.class);
    private static final int MAX_PACKET_SIZE = 512;

    @Value("${sinkhole.listen-port:5300}")
    private int listenPort;

    private final BlocklistService blocklistService;
    private final DnsForwarder dnsForwarder;
    private final QueryLogRepository queryLogRepository;

    private final ExecutorService workerPool = Executors.newFixedThreadPool(4);
    private volatile boolean running = true;
    private DatagramSocket socket;

    public DnsUdpServer(BlocklistService blocklistService,
                         DnsForwarder dnsForwarder,
                         QueryLogRepository queryLogRepository) {
        this.blocklistService = blocklistService;
        this.dnsForwarder = dnsForwarder;
        this.queryLogRepository = queryLogRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread listenerThread = new Thread(this::listenLoop, "dns-udp-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenLoop() {
        try {
            socket = new DatagramSocket(listenPort);
            log.info("DNS sinkhole listening on UDP port {}", listenPort);

            while (running) {
                byte[] buffer = new byte[MAX_PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                DatagramPacket finalPacket = clonePacket(packet);
                workerPool.submit(() -> handleQuery(finalPacket));
            }
        } catch (Exception e) {
            if (running) {
                log.error("DNS listener crashed", e);
            }
        }
    }

    private void handleQuery(DatagramPacket packet) {
        String clientIp = packet.getAddress().getHostAddress();
        try {
            Message query = new Message(packet.getData());
            Record question = query.getQuestion();
            if (question == null) return;

            String domain = question.getName().toString();
            boolean blocked = blocklistService.isBlocked(domain);

            Message response;
            if (blocked) {
                response = buildBlockedResponse(query);
                log.debug("BLOCKED {} (requested by {})", domain, clientIp);
            } else {
                response = dnsForwarder.forward(query);
                log.debug("ALLOWED {} (requested by {})", domain, clientIp);
            }

            sendResponse(response, packet.getAddress(), packet.getPort());

            queryLogRepository.save(new QueryLog(domain, clientIp, blocked));

        } catch (Exception e) {
            log.warn("Failed to handle DNS query from {}: {}", clientIp, e.getMessage());
        }
    }

    private Message buildBlockedResponse(Message query) {
        Message response = new Message(query.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(Rcode.NXDOMAIN);
        response.addRecord(query.getQuestion(), Section.QUESTION);
        return response;
    }

    private void sendResponse(Message response, InetAddress address, int port) throws Exception {
        byte[] responseBytes = response.toWire();
        DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, address, port);
        socket.send(responsePacket);
    }

    private DatagramPacket clonePacket(DatagramPacket original) {
        byte[] dataCopy = new byte[original.getLength()];
        System.arraycopy(original.getData(), 0, dataCopy, 0, original.getLength());
        return new DatagramPacket(dataCopy, dataCopy.length, original.getAddress(), original.getPort());
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (socket != null) socket.close();
        workerPool.shutdown();
    }
}
