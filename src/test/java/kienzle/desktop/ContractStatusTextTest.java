package kienzle.desktop;

import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractStatus;
import kienzle.desktop.contract.SignatureInfo;
import kienzle.desktop.contract.SignatureSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ContractStatusTextTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void describe_returnsHumanReadableStatusText() {
        LeaseContract contract = LeaseContract.builder()
                .name("Vertrag")
                .lessor("Verpächter")
                .tenant("Pächter")
                .build();

        ContractEntry entry = ContractEntry.draft(contract, Instant.now())
                .withStatus(ContractStatus.SIGNED, Instant.now())
                .withSignature(new SignatureInfo(UUID.randomUUID(), SignatureSource.MOBILE, Instant.now(), "Signer", "Phone"), Instant.now());

        String text = ContractStatusText.describe(entry);
        assertFalse(text.isBlank());
    }
}
