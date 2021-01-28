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
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.plugins.PluginFolder;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import uk.gov.nationalarchives.pdi.step.StepPluginResource;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndToEndIT {

    @RegisterExtension
    static final StepPluginResource JENA_MODEL_STEP_PLUGIN = new StepPluginResource(JenaModelStepMeta.class);

    @BeforeAll
    public static void setup() throws KettleException {
        KettleEnvironment.init();
    }

    @Test
    public void can_create_rdf_type_statement() throws KettleException, IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Result transformationResult = executeTransformation();
        final Model actual = extractGraph(transformationResult);
        final Model expected = createExpectedModel();

        assertTrue(actual.isIsomorphicWith(expected));
    }

    private static Result executeTransformation() throws IOException, KettleException {
        try (final InputStream ktr = EndToEndIT.class.getResourceAsStream("can_create_rdf_type_statement.ktr")) {
            final Trans trans = new Trans(new TransMeta(ktr, null, false, null, null));

            trans.execute(null);
            trans.waitUntilFinished();

            return trans.getResult();
        }
    }

    private static Model extractGraph(Result result) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
        // Actual value (Jena model created by step) is in the third field of the first row of the transformation result.
        final Object model = result.getRows().get(0).getData()[2];

        // Use reflection to serialise original model, then deserialise back to an instance.
        // This is required because the class loaders differ between Kettle and test environments, so their Jena Model is not our Jena Model.
        return unmarshall(model);
    }

    private static Model unmarshall(Object original) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
        final Method method = original.getClass().getMethod("write", OutputStream.class);

        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            method.invoke(original, output);

            try (final InputStream inputStream = new ByteArrayInputStream(output.toByteArray())) {
                final Model model = ModelFactory.createDefaultModel();
                model.read(inputStream, null);

                return model;
            }
        }
    }

    private static Model createExpectedModel() {
        final Model expected = ModelFactory.createDefaultModel();
        expected.add(
                expected.createResource("http://example.com/s"),
                expected.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                expected.createResource("http://example.com/C"));

        return expected;
    }
}
