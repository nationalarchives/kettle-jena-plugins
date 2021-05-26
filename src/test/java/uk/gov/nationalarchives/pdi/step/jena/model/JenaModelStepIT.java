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

import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.step.RowHandler;
import org.pentaho.di.trans.steps.mock.StepMockHelper;
import uk.gov.nationalarchives.pdi.step.jena.Rdf11;

import javax.xml.namespace.QName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.nationalarchives.pdi.step.jena.Util.Entry;
import static uk.gov.nationalarchives.pdi.step.jena.Util.Map;

public class JenaModelStepIT {
    @BeforeAll
    public static void setup() throws KettleException {
        KettleClientEnvironment.init();
    }

    @Test
    public void can_create_xsd_int_property() throws KettleException {
        final JenaModelStepMeta meta = getMeta();
        final StepMockHelper<JenaModelStepMeta, JenaModelStepData> helper = mockHelper();
        final JenaModelStep step = mockStep(helper);

        final boolean rowProcessedSuccessfully = step.processRow(meta, helper.processRowsStepDataInterface);

        assertTrue(rowProcessedSuccessfully);
    }

    @Test
    public void resolves_uri_field_variable() throws KettleException {
        final JenaModelStepMeta meta = getMeta();
        final StepMockHelper<JenaModelStepMeta, JenaModelStepData> helper = mockHelper();
        final JenaModelStep step = mockStep(helper);

        step.setVariable("uriFieldVar", "uriField");
        meta.setResourceUriField("${uriFieldVar}");

        final boolean rowProcessedSuccessfully = step.processRow(meta, helper.processRowsStepDataInterface);

        assertTrue(rowProcessedSuccessfully);
    }

    @Test
    public void resolves_target_field_name_variable() throws KettleException {
        final String expectedFieldName = "targetField";

        final JenaModelStepMeta meta = getMeta();
        final StepMockHelper<JenaModelStepMeta, JenaModelStepData> helper = mockHelper();
        final JenaModelStep step = mockStep(helper);
        final SingleRowMetaObserver rowMetaObserver = new SingleRowMetaObserver(step.getRowHandler());
        step.setRowHandler(rowMetaObserver);

        step.setVariable("targetFieldVar", expectedFieldName);
        meta.setTargetFieldName("${targetFieldVar}");

        step.processRow(meta, helper.processRowsStepDataInterface);

        final String actualFieldName = rowMetaObserver.meta.getFieldNames()[2];

        assertEquals(expectedFieldName, actualFieldName);
    }

    private static JenaModelStepMeta getMeta() {
        final JenaModelStepMeta meta = new JenaModelStepMeta();
        meta.setDefault();
        meta.setTargetFieldName("targetField");
        meta.setResourceUriField("uriField");

        final JenaModelStepMeta.DbToJenaMapping mapping = new JenaModelStepMeta.DbToJenaMapping();
        mapping.fieldName = "field1";
        mapping.rdfPropertyNameSource = JenaModelStepMeta.RdfPropertyNameSource.fromString(Map(Entry(Rdf11.RDF_PREFIX, RDF.uri)), "rdf:predicate");
        mapping.rdfType = new QName("xsd:int");

        meta.setDbToJenaMappings(new JenaModelStepMeta.DbToJenaMapping[]{
                mapping,
        });

        return meta;
    }

    private static StepMockHelper<JenaModelStepMeta, JenaModelStepData> mockHelper() {
        final StepMockHelper<JenaModelStepMeta, JenaModelStepData> helper = new StepMockHelper<>("Create Jena Model", JenaModelStepMeta.class, JenaModelStepData.class);

        when(helper.logChannelInterfaceFactory.create(any(), any(LoggingObjectInterface.class))).thenReturn(helper.logChannelInterface);
        when(helper.trans.isRunning()).thenReturn(true);

        doCallRealMethod().when(helper.processRowsStepDataInterface).setOutputRowMeta(any(RowMetaInterface.class));
        when(helper.processRowsStepDataInterface.getOutputRowMeta()).thenCallRealMethod();

        return helper;
    }

    private static JenaModelStep mockStep(StepMockHelper<JenaModelStepMeta, JenaModelStepData> helper) throws KettleException {
        final JenaModelStep step = Mockito.spy(new JenaModelStep(helper.stepMeta, helper.stepDataInterface, 0, helper.transMeta, helper.trans));

        final Object[] inputRowValues = {
                0,
                "http://example.com/resource"
        };

        final RowMeta inputRowSchema = new RowMeta();
        inputRowSchema.addValueMeta(new ValueMetaInteger("field1"));
        inputRowSchema.addValueMeta(new ValueMetaString("uriField"));

        doReturn(inputRowValues).when(step).getRow();
        doReturn(inputRowSchema).when(step).getInputRowMeta();

        return step;
    }

    private static class SingleRowMetaObserver implements RowHandler {
        private final RowHandler original;
        private RowMetaInterface meta;

        public SingleRowMetaObserver(RowHandler original) {
            this.original = original;
        }

        @Override
        public Object[] getRow() throws KettleException {
            return original.getRow();
        }

        @Override
        public void putRow(RowMetaInterface rowMetaInterface, Object[] objects) throws KettleStepException {
            // Test this is used in only ever does one row anyway
            meta = rowMetaInterface;

            original.putRow(rowMetaInterface, objects);
        }

        @Override
        public void putError(RowMetaInterface rowMetaInterface, Object[] objects, long l, String s, String s1, String s2) throws KettleStepException {
            original.putError(rowMetaInterface, objects, l, s, s1, s2);
        }
    }

}
