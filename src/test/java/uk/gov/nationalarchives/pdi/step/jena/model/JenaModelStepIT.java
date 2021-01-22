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

import javax.xml.namespace.QName;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.steps.mock.StepMockHelper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class JenaModelStepIT {
    @BeforeAll
    public static void setup() throws KettleException {
        KettleClientEnvironment.init();
    }

    @Test
    public void can_create_xsd_int_property() throws KettleException {
        final JenaModelStepMeta meta = getMeta();
        final StepMockHelper helper = mockHelper();
        final JenaModelStep step = mockStep(helper);

        final boolean rowProcessedSuccessfully = step.processRow(meta, helper.processRowsStepDataInterface);

        assertTrue(rowProcessedSuccessfully);
    }

    private static JenaModelStepMeta getMeta() {
        return new JenaModelStepMeta() {{
            setDefault();
            setTargetFieldName("targetField");
            setResourceUriField("uriField");
            setDbToJenaMappings(new DbToJenaMapping[]{
                    new DbToJenaMapping() {{
                        fieldName = "field1";
                        rdfPropertyName = new QName("rdf:predicate");
                        rdfType = new QName("xsd:int");
                    }},
            });
        }};
    }

    private static StepMockHelper mockHelper() {
        final StepMockHelper helper = new StepMockHelper<>("Create Jena Model", JenaModelStepMeta.class, JenaModelStepData.class);

        when(helper.logChannelInterfaceFactory.create(any(), any(LoggingObjectInterface.class))).thenReturn(helper.logChannelInterface);
        when(helper.trans.isRunning()).thenReturn(true);

        return helper;
    }

    private static JenaModelStep mockStep(StepMockHelper helper) throws KettleException {
        final JenaModelStep step = Mockito.spy(new JenaModelStep(helper.stepMeta, helper.stepDataInterface, 0, helper.transMeta, helper.trans));

        final Object[] inputRowValues = {
                0,
                "http://example.com/resource"
        };

        final RowMeta inputRowSchema = new RowMeta() {{
            addValueMeta(new ValueMetaInteger("field1"));
            addValueMeta(new ValueMetaString("uriField"));
        }};

        doReturn(inputRowValues).when(step).getRow();
        doReturn(inputRowSchema).when(step).getInputRowMeta();

        return step;
    }
}
