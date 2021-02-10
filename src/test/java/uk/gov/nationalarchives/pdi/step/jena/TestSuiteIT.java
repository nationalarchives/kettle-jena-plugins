package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.ext.com.google.common.collect.Streams;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import uk.gov.nationalarchives.pdi.step.StepPluginResource;
import uk.gov.nationalarchives.pdi.step.jena.model.JenaModelStepMeta;
import uk.gov.nationalarchives.pdi.step.jena.serializer.JenaSerializerStepMeta;

import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSuiteIT {
    @SuppressWarnings("unused") // Marker field required to inject plugin
    @RegisterExtension
    static final StepPluginResource JENA_MODEL_STEP_PLUGIN = new StepPluginResource(JenaModelStepMeta.class);

    @SuppressWarnings("unused") // Marker field required to inject plugin
    @RegisterExtension
    static final StepPluginResource JENA_SERIALIZE_STEP_PLUGIN = new StepPluginResource(JenaSerializerStepMeta.class);

    @TestFactory
    public Stream<DynamicTest> executeTest(@TempDir final Path tempDir) throws Exception {
        final ManifestGraph g = new ManifestGraph();
        final String manifestUrl = TestSuiteIT.class.getResource("manifest.ttl").toString();
        g.read(manifestUrl, "TURTLE");

        return Streams
                .stream(g.getManifests())
                .flatMap(manifest -> Streams.stream(manifest.getEntries()))
                .map(entry -> DynamicTest.dynamicTest(entry.getName(), () -> assertTrue(entry.execute(tempDir))));
    }
}