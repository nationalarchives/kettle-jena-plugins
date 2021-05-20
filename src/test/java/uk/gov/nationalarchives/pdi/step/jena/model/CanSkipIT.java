/*
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
package uk.gov.nationalarchives.pdi.step.jena.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import uk.gov.nationalarchives.pdi.step.StepPluginResource;
import uk.gov.nationalarchives.pdi.step.jena.serializer.JenaSerializerStepMeta;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.nationalarchives.pdi.step.jena.model.TestUtil.executeTransformation;
import static uk.gov.nationalarchives.pdi.step.jena.model.TestUtil.loadTurtleGraph;

public class CanSkipIT {
    @SuppressWarnings("unused") // Marker field required to inject plugin
    @RegisterExtension
    static final StepPluginResource JENA_MODEL_STEP_PLUGIN = new StepPluginResource(JenaModelStepMeta.class);

    @SuppressWarnings("unused") // Marker field required to inject plugin
    @RegisterExtension
    static final StepPluginResource JENA_SERIALIZE_STEP_PLUGIN = new StepPluginResource(JenaSerializerStepMeta.class);

    @BeforeAll
    public static void setup() throws KettleException {
        KettleEnvironment.init();
    }

    @Test
    public void skipProperties(@TempDir final Path tempDir) throws KettleException, IOException {
        final Model expected = ModelFactory.createDefaultModel();
        expected.add(
                expected.createResource("http://example.com/s"),
                expected.createProperty("http://example.com/pSkipNo"),
                expected.createLiteral("http://example.com/s"));

        final Path outputFilePath = Files.createTempFile(tempDir, "output", ".ttl");
        try (final InputStream kettleTransformation = getClass().getResourceAsStream("skipProperties.ktr")) {
            executeTransformation(kettleTransformation, outputFilePath);
        }
        final Model actual = loadTurtleGraph(outputFilePath);

        assertTrue(actual.isIsomorphicWith(expected));
    }
}
