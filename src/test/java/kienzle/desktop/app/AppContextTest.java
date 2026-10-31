package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static kienzle.desktop.TestContracts.sampleContract;

class AppContextTest {
    @TempDir
    Path tempDir;

    @Test
    void create_buildsWorkflowAndPackageService() throws Exception {
        AppContext context = AppContext.create(tempDir);

        assertNotNull(context.workflow());
        assertNotNull(context.packages());
        assertEquals(0, context.workflow().list().size());

        LeaseContract contract = sampleContract();
        var saved = context.workflow().save(null, contract);

        assertEquals(1, context.workflow().list().size());
        assertEquals(saved.id(), context.workflow().list().get(0).id());

        context.workflow().archive(saved);
        assertEquals(0, context.workflow().list().size());

        var restored = context.workflow().restore(saved);
        assertEquals(saved.id(), restored.id());
    }
}
