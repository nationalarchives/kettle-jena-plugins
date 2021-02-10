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
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TestUtil {

    /**
     * Execute a Kettle transformation.
     *
     * @param kettleTransformation the kettle transformation
     * @param outputFilePath a "Filename" variable to pass into the kettle transformation
     *
     * @throws KettleException if an error occurs whilst executing the transformation
     */
    static void executeTransformation(final InputStream kettleTransformation, final Path outputFilePath)
            throws KettleException {
        final Map<String, String> variables = new HashMap<>();
        variables.put("Filename", outputFilePath.toString());
        executeTransformation(kettleTransformation, variables);
    }

    /**
     * Execute a Kettle transformation.
     *
     * @param kettleTransformation the kettle transformation
     * @param variables variables to pass into the transformation
     *
     * @throws KettleException if an error occurs whilst executing the transformation
     */
    private static void executeTransformation(final InputStream kettleTransformation, final Map<String, String> variables)
            throws KettleException {
        final Trans trans = new Trans(new TransMeta(kettleTransformation, null, false, null, null));
        variables.forEach(trans::setVariable);
        trans.execute(null);
        trans.waitUntilFinished();
    }

    /**
     * Load a Turtle Graph.
     *
     * @param path the path to the Turtle RDF file.
     * @return a Jena Model.
     */
    static Model loadTurtleGraph(final Path path) {
        return loadGraph(path, "TURTLE");
    }

    /**
     * Load a Graph.
     *
     * @param path the path to the RDF file.
     * @return a Jena Model.
     */
    private static Model loadGraph(final Path path, final String rdfSerializationFormat) {
        final Model model = ModelFactory.createDefaultModel();
        model.read(path.toUri().toString(), rdfSerializationFormat);
        return model;
    }
}
