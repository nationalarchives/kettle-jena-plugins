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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.plugins.PluginFolder;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EndToEndIT {
    @BeforeAll
    public static void setup() throws KettleException {
        // TODO(SL): This should not be hard-coded.
        // This the only way I could find to inject a plugin into Kettle
        final String pluginFolderPath = "target\\kettle-jena-plugins-2.1.0-SNAPSHOT-kettle-plugin";
        StepPluginType.getInstance().getPluginFolders().add(new PluginFolder(pluginFolderPath, false, true));

        KettleEnvironment.init();
    }

    @Test
    public void can_create_rdf_type_statement() throws KettleException, IOException {
        try (final InputStream ktr = getClass().getResourceAsStream("can_create_rdf_type_statement.ktr")) {
            final Trans trans = new Trans(new TransMeta(ktr, null, false, null, null));

            trans.execute(null);
            trans.waitUntilFinished();

            // Actual value (Jena model created by step) is in the third field of the first row of the transformation result
            final String actual = trans.getResult().getRows().get(0).getData()[2].toString();

            // Using this lame string representation of the in-memory graph instead of some proper equality assertion because the class loaders differ between Kettle and test environments, so their Jena Model is not our Jena Model
            final String expected = "<ModelCom   {http://example.com/s @type C} |  [http://example.com/s, type, C]>";

            assertEquals(expected, actual);
        }
    }
}
