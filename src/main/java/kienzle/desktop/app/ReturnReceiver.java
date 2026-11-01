package kienzle.desktop.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import kienzle.desktop.contract.ContractEntry;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Receives {@code .leasesigned} files from a phone in the local network ({@code POST /return}). The endpoint does
 * no authentication of its own: every file goes through the normal import, which only accepts returns signed by a
 * paired device. It runs only while the user keeps the receiving window open.
 */
public final class ReturnReceiver implements AutoCloseable {
    public static final int DEFAULT_PORT = 48765;
    private static final long MAX_BODY_BYTES = 51L * 1024 * 1024 + 64 * 1024;

    /** Result of one upload: the imported entry, or the reason why it was refused. */
    public interface Importer {
        ContractEntry importFile(Path file) throws IOException;
    }

    private final HttpServer server;
    private final Importer importer;
    private final BiConsumer<ContractEntry, String> listener;

    /** {@code listener} gets the imported entry, or {@code null} plus a message when a file was refused. */
    public ReturnReceiver(int port, Importer importer, BiConsumer<ContractEntry, String> listener)
            throws IOException {
        this.importer = importer;
        this.listener = listener;
        HttpServer created;
        try {
            created = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (java.net.BindException e) {
            created = HttpServer.create(new InetSocketAddress(0), 0);
        }
        this.server = created;
        server.createContext("/return", this::handle);
        server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "return-receiver");
            thread.setDaemon(true);
            return thread;
        }));
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    /** Candidate URLs (one per local IPv4 address) that a phone in the same network can use. */
    public List<String> urls() {
        List<String> urls = new ArrayList<>();
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        urls.add("http://" + address.getHostAddress() + ":" + port() + "/return");
                    }
                }
            }
        } catch (java.net.SocketException ignored) {
            // No network information: the user sees an empty list.
        }
        return urls;
    }

    @Override
    public void close() {
        server.stop(0);
        if (server.getExecutor() instanceof java.util.concurrent.ExecutorService executor) {
            executor.shutdownNow();
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"POST".equals(exchange.getRequestMethod())) {
                reply(exchange, 405, "POST only");
                return;
            }
            String length = exchange.getRequestHeaders().getFirst("Content-Length");
            if (length != null && parseLength(length) > MAX_BODY_BYTES) {
                reply(exchange, 413, "Too large");
                return;
            }
            Path temporary = Files.createTempFile("lease-return", ".leasesigned");
            try {
                try (InputStream body = exchange.getRequestBody()) {
                    byte[] data = body.readNBytes((int) MAX_BODY_BYTES + 1);
                    if (data.length > MAX_BODY_BYTES) {
                        reply(exchange, 413, "Too large");
                        return;
                    }
                    Files.write(temporary, data);
                }
                ContractEntry entry = importer.importFile(temporary);
                listener.accept(entry, null);
                reply(exchange, 200, "OK");
            } catch (IOException | RuntimeException e) {
                listener.accept(null, String.valueOf(e.getMessage()));
                reply(exchange, 422, "Refused");
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static long parseLength(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    private static void reply(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }
}
