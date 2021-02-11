/**
 * The MIT License
 * Copyright © 2020 The National Archives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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