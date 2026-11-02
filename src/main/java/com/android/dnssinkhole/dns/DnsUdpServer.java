package com.android.dnssinkhole.dns;

import com.android.dnssinkhole.blocklist.BlocklistService;
import com.android.dnssinkhole.blocklist.DomainNames;
import com.android.dnssinkhole.config.SinkholeProperties;
import com.android.dnssinkhole.querylog.QueryLogWriter;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DnsUdpServer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DnsUdpServer.class);
    private static final int MAX_PACKET_SIZE = 4096;
    private static final int MIN_UDP_PAYLOAD = 512;

    private final BlocklistService blocklistService;
    private final DnsForwarder dnsForwarder;
    private final QueryLogWriter queryLogWriter;
    private final int listenPort;
    private final ThreadPoolExecutor workerPool;

    private volatile boolean running = true;
    private DatagramSocket socket;

    public DnsUdpServer(BlocklistService blocklistService,
                        DnsForwarder dnsForwarder,
                        QueryLogWriter queryLogWriter,
                        SinkholeProperties properties) {
        this.blocklistService = blocklistService;
        this.dnsForwarder = dnsForwarder;
        this.queryLogWriter = queryLogWriter;
        this.listenPort = properties.listenPort();
        this.workerPool = new ThreadPoolExecutor(
                properties.workerThreads(),
                properties.workerThreads(),
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.queueCapacity()),
                workerThreadFactory(),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread listenerThread = new Thread(this::listenLoop, "dns-udp-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenLoop() {
        try (DatagramSocket boundSocket = new DatagramSocket(listenPort)) {
            socket = boundSocket;
            log.info("DNS sinkhole listening on UDP port {}", listenPort);

            byte[] buffer = new byte[MAX_PACKET_SIZE];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                DatagramPacket copy = copyPacket(packet);
                workerPool.execute(() -> handleQuery(copy));
            }
        } catch (Exception e) {
            if (running) {
                log.error("DNS listener crashed", e);
            }
        }
    }

    void handleQuery(DatagramPacket packet) {
        String clientIp = packet.getAddress().getHostAddress();

        Message query;
        try {
            query = new Message(packet.getData());
        } catch (IOException e) {
            log.debug("Dropping malformed DNS packet from {}: {}", clientIp, e.getMessage());
            return;
        }

        Record question = query.getQuestion();
        if (question == null) {
            return;
        }

        String domain = question.getName().toString();
        boolean blocked = blocklistService.isBlocked(domain);

        Message response;
        if (blocked) {
            response = buildErrorResponse(query, Rcode.NXDOMAIN);
            log.debug("BLOCKED {} (requested by {})", domain, clientIp);
        } else {
            response = forwardOrFail(query, domain);
            log.debug("ALLOWED {} (requested by {})", domain, clientIp);
        }

        try {
            sendResponse(response, maxResponseSize(query), packet.getAddress(), packet.getPort());
        } catch (IOException e) {
            log.warn("Failed to send DNS response to {}: {}", clientIp, e.getMessage());
        }

        queryLogWriter.record(DomainNames.normalize(domain), clientIp, blocked);
    }

    private Message forwardOrFail(Message query, String domain) {
        try {
            return dnsForwarder.forward(query);
        } catch (IOException e) {
            log.warn("Upstream lookup failed for {}: {}", domain, e.getMessage());
            return buildErrorResponse(query, Rcode.SERVFAIL);
        }
    }

    private Message buildErrorResponse(Message query, int rcode) {
        Message response = new Message(query.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setFlag(Flags.RA);
        if (query.getHeader().getFlag(Flags.RD)) {
            response.getHeader().setFlag(Flags.RD);
        }
        response.getHeader().setRcode(rcode);
        response.addRecord(query.getQuestion(), Section.QUESTION);
        return response;
    }

    private static int maxResponseSize(Message query) {
        OPTRecord opt = query.getOPT();
        if (opt == null) {
            return MIN_UDP_PAYLOAD;
        }
        return Math.min(MAX_PACKET_SIZE, Math.max(MIN_UDP_PAYLOAD, opt.getPayloadSize()));
    }

    private void sendResponse(Message response, int maxSize, InetAddress address, int port) throws IOException {
        byte[] responseBytes = response.toWire(maxSize);
        socket.send(new DatagramPacket(responseBytes, responseBytes.length, address, port));
    }

    private static DatagramPacket copyPacket(DatagramPacket original) {
        byte[] data = Arrays.copyOfRange(original.getData(), original.getOffset(),
                original.getOffset() + original.getLength());
        return new DatagramPacket(data, data.length, original.getAddress(), original.getPort());
    }

    private static ThreadFactory workerThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "dns-worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        running = false;
        if (socket != null) {
            socket.close();
        }
        workerPool.shutdown();
        if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
            workerPool.shutdownNow();
        }
    }
}
