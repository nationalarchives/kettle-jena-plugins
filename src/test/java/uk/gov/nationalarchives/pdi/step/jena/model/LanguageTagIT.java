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

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
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
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.nationalarchives.pdi.step.jena.model.TestUtil.executeTransformation;
import static uk.gov.nationalarchives.pdi.step.jena.model.TestUtil.loadTurtleGraph;

public class LanguageTagIT {
    @SuppressWarnings("unused") // Marker field required to inject plugin
    @RegisterExtension
    static final StepPluginResource JENA_MODEL_STEP_PLUGIN = new StepPluginResource(JenaModelStepMeta.class);

    @SuppressWarnings("unused") // Marker field required to inject plugin
    @RegisterExtension
    static final StepPluginResource JENA_MODEL_SERIALIZE_PLUGIN = new StepPluginResource(JenaSerializerStepMeta.class);

    @BeforeAll
    public static void setup() throws KettleException {
        KettleEnvironment.init();
    }

    /**
     * @see <a href="https://github.com/nationalarchives/kettle-jena-plugins/pull/16#discussion_r568929699">nationalarchives/kettle-jena-plugins/pull/16#discussion_r568929699</a>
     */
    @Test
    public void can_create_lang_string(@TempDir final Path tempDir) throws KettleException, IOException {
        final Model expected = ModelFactory.createDefaultModel();
        final Resource s = expected.createResource("http://example.com/s");
        final Property p = expected.createProperty("http://example.com/p");

        // Untyped literal object
        expected.add(s, p, expected.createLiteral("o"));

        // Language-tagged string literal objects
        expected.add(s, p, expected.createLiteral("o", "en"));
        expected.add(s, p, expected.createLiteral("o", "fr"));

        // Typed literal object
        expected.add(s, p, expected.createTypedLiteral("o", TypeMapper.getInstance().getSafeTypeByName("http://example.com/D")));


        final Path outputFilePath = tempDir.resolve("output.ttl");
        try (final InputStream kettleTransformation = getClass().getResourceAsStream("can_create_lang_string.ktr")) {
            executeTransformation(kettleTransformation, outputFilePath);
        }
        final Model actual = loadTurtleGraph(outputFilePath);

        assertTrue(actual.isIsomorphicWith(expected));
    }
}
