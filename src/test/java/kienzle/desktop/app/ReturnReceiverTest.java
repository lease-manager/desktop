package kienzle.desktop.app;

import kienzle.desktop.contract.ContractEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnReceiverTest {
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> send(ReturnReceiver receiver, String method, byte[] body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + receiver.port() + "/return"));
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void acceptedFileIsHandedToTheImporterAndReported() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        AtomicReference<ContractEntry> reported = new AtomicReference<>();
        ContractEntry entry = ContractEntry.draft(kienzle.desktop.TestContracts.sampleContract(),
                java.time.Instant.now());
        try (ReturnReceiver receiver = new ReturnReceiver(0, file -> {
            received.set(Files.readString(file));
            return entry;
        }, (imported, error) -> reported.set(imported))) {
            receiver.start();
            HttpResponse<String> response = send(receiver, "POST", "payload".getBytes());

            assertEquals(200, response.statusCode());
            assertEquals("payload", received.get());
            assertEquals(entry, reported.get());
        }
    }

    @Test
    void refusedFileGets422AndTheReasonIsReported() throws Exception {
        AtomicReference<String> error = new AtomicReference<>();
        AtomicReference<ContractEntry> reported = new AtomicReference<>();
        reported.set(ContractEntry.draft(kienzle.desktop.TestContracts.sampleContract(), java.time.Instant.now()));
        try (ReturnReceiver receiver = new ReturnReceiver(0, file -> {
            throw new IOException("not from a paired device");
        }, (imported, message) -> {
            reported.set(imported);
            error.set(message);
        })) {
            receiver.start();
            HttpResponse<String> response = send(receiver, "POST", "x".getBytes());

            assertEquals(422, response.statusCode());
            assertNull(reported.get());
            assertTrue(error.get().contains("paired"));
        }
    }

    @Test
    void otherMethodsAreRejectedAndNothingIsImported() throws Exception {
        AtomicReference<String> called = new AtomicReference<>();
        try (ReturnReceiver receiver = new ReturnReceiver(0, file -> {
            called.set("yes");
            return null;
        }, (imported, error) -> { })) {
            receiver.start();

            assertEquals(405, send(receiver, "GET", null).statusCode());
            assertNull(called.get());
            assertNotNull(receiver.urls());
        }
    }
}
