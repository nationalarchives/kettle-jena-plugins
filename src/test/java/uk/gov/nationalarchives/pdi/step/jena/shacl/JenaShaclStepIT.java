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
package uk.gov.nationalarchives.pdi.step.jena.shacl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaSerializable;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.trans.RowStepCollector;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransTestFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JenaShaclStepIT {

    static final String STEP_NAME = "Integration test for Jena SHACL step";

    @BeforeAll
    public static void setUpBeforeClass() throws KettleException {
        KettleEnvironment.init(false);
    }

    @Test
    public void hasNoErrorsWithValidModel() throws KettleException {
        final TransMeta tm = TransTestFactory.generateTestTransformationError(new Variables(), getTestMeta(), STEP_NAME);
        final Map<String, RowStepCollector> result = TransTestFactory.executeTestTransformationError(tm, TransTestFactory.INJECTOR_STEPNAME,
                STEP_NAME, TransTestFactory.DUMMY_STEPNAME, TransTestFactory.ERROR_STEPNAME, generateInputData("FO_371_190180_1-policy.ttl"));
        assertEquals(0, result.get(STEP_NAME).getRowsError().size());
    }

    @Test
    public void hasErrorswithInvalidModel() throws KettleException {
        final TransMeta tm = TransTestFactory.generateTestTransformationError(new Variables(), getTestMeta(), STEP_NAME);
        final Map<String, RowStepCollector> result = TransTestFactory.executeTestTransformationError(tm, TransTestFactory.INJECTOR_STEPNAME,
                STEP_NAME, TransTestFactory.DUMMY_STEPNAME, TransTestFactory.ERROR_STEPNAME, generateInputData("FO_371_190180_1-policy-invalid.ttl"));
        assertEquals(1, result.get(STEP_NAME).getRowsError().size());
    }

    @Test
    public void shapeFilePathNotValid() {
        final TransMeta tm = TransTestFactory.generateTestTransformationError(new Variables(), getInvalidTestMeta(), STEP_NAME);
        assertThrows(KettleException.class, () -> {
            TransTestFactory.executeTestTransformationError(tm, TransTestFactory.INJECTOR_STEPNAME,
                    STEP_NAME, TransTestFactory.DUMMY_STEPNAME, TransTestFactory.ERROR_STEPNAME, generateInputData("FO_371_190180_1-policy.ttl"));
        });
    }

    private List<RowMetaAndData> generateInputData(final String filename) {
        final List<RowMetaAndData> retval = new ArrayList<>();
        final RowMetaInterface rowMeta = new RowMeta();
        final String modelFilePath = getFilePath(filename);
        final Model model = RDFDataMgr.loadModel(modelFilePath);
        rowMeta.addValueMeta(new ValueMetaSerializable("jena_model"));
        retval.add(new RowMetaAndData(rowMeta, model));
        return retval;
    }

    private String getFilePath(final String filename) {
        final URL url = this.getClass().getResource("/" + filename);
        assert url != null;
        return url.toString();
    }

    private JenaShaclStepMeta getTestMeta() {
        final JenaShaclStepMeta meta = new JenaShaclStepMeta();
        meta.setShapesFilePath("ODRL-shape.ttl");
        meta.setJenaModelField("jena_model");
        return meta;
    }

    private JenaShaclStepMeta getInvalidTestMeta() {
        final JenaShaclStepMeta meta = new JenaShaclStepMeta();
        meta.setShapesFilePath("missing-shape.ttl");
        meta.setJenaModelField("jena_model");
        return meta;
    }
}
