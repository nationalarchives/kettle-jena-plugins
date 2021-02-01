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

package uk.gov.nationalarchives.pdi.step.jena.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import uk.gov.nationalarchives.pdi.step.StepPluginResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LanguageTagIT {
    @SuppressWarnings("unused") // Marker field required to inject plugin
    @RegisterExtension
    static final StepPluginResource JENA_MODEL_STEP_PLUGIN = new StepPluginResource(JenaModelStepMeta.class);

    @BeforeAll
    public static void setup() throws KettleException {
        KettleEnvironment.init();
    }

    @Test
    public void can_create_lang_string(@TempDir final Path tempDir) throws KettleException, IOException {
        final Path outputFilePath = tempDir.resolve("output.ttl");

        executeTransformation(outputFilePath);
        final Model actual = loadOutputGraph(outputFilePath);
        final Model expected = createExpectedModel();

        assertTrue(actual.isIsomorphicWith(expected));
    }

    private static void executeTransformation(final Path outputFilePath) throws IOException, KettleException {
        try (final InputStream ktr = EndToEndIT.class.getResourceAsStream("can_create_lang_string.ktr")) {
            final Trans trans = new Trans(new TransMeta(ktr, null, false, null, null));

            trans.setVariable("Filename", outputFilePath.toString());
            trans.execute(null);
            trans.waitUntilFinished();
        }
    }

    private static Model loadOutputGraph(final Path outputFilePath) {
        final Model output = ModelFactory.createDefaultModel();
        output.read(outputFilePath.toUri().toString(), "TURTLE");

        return output;
    }

    private static Model createExpectedModel() {
        final Model expected = ModelFactory.createDefaultModel();
        expected.add(
                expected.createResource("http://example.com/s"),
                expected.createProperty("http://example.com/p"),
                expected.createLiteral("o","en"));

        return expected;
    }
}
