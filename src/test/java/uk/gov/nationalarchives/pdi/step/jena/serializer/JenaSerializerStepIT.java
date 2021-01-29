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

package uk.gov.nationalarchives.pdi.step.jena.serializer;

import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.trans.step.RowHandler;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.steps.mock.StepMockHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class JenaSerializerStepIT {
    @BeforeAll
    public static void setup() throws KettleException {
        KettleClientEnvironment.init();
    }

    @Test
    public void resolves_jena_model_field_variable() throws KettleException {
        final JenaSerializerStepMeta meta = getMeta();
        final StepDataInterface data = getData();
        final StepMockHelper<JenaSerializerStepMeta, JenaSerializerStepData> helper = mockHelper();
        final JenaSerializerStep step = mockStep(helper);

        step.setVariable("jenaModelFieldVar", "model");
        meta.setJenaModelField("${jenaModelFieldVar}");

        final boolean rowProcessed = step.processRow(meta, data);

        // Would throw if var was not resolved
        assertTrue(rowProcessed);
    }

    @Test
    public void resolves_filename_variable(@TempDir final Path tempDir) throws KettleException {
        final Path expectedFile = tempDir.resolve("output.ttl");

        final JenaSerializerStepMeta meta = getMeta();
        final StepDataInterface data = getData();
        final StepMockHelper<JenaSerializerStepMeta, JenaSerializerStepData> helper = mockHelper();
        final JenaSerializerStep step = mockStep(helper);

        meta.setJenaModelField("model");

        step.setVariable("filenameVar", expectedFile.toString());

        final JenaSerializerStepMeta.FileDetail fileDetail = new JenaSerializerStepMeta.FileDetail();
        fileDetail.filename = "${filenameVar}";

        meta.setFileDetail(fileDetail);

        // First pass create model, second serialise
        step.processRow(meta, data);
        step.processRow(meta, data);

        assertTrue(Files.exists(expectedFile));
    }

    @Test
    public void resolves_serialization_format_variable(@TempDir final Path tempDir) throws KettleException {
        final Path expectedFile = tempDir.resolve("output.xml");

        final JenaSerializerStepMeta meta = getMeta();
        final StepDataInterface data = getData();
        final StepMockHelper<JenaSerializerStepMeta, JenaSerializerStepData> helper = mockHelper();
        final JenaSerializerStep step = mockStep(helper);

        meta.setJenaModelField("model");

        final JenaSerializerStepMeta.FileDetail fileDetail = new JenaSerializerStepMeta.FileDetail();
        fileDetail.filename = expectedFile.toString();

        meta.setFileDetail(fileDetail);

        step.setVariable("serialisationFormatVar", "RDF/XML");
        meta.setSerializationFormat("${serialisationFormatVar}");

        // First pass create model, second serialise
        step.processRow(meta, data);
        final boolean rowProcessed = step.processRow(meta, data);

        // Would throw if var was not resolved
        assertFalse(rowProcessed);
    }

    private StepDataInterface getData() {
        final JenaSerializerStepData data = new JenaSerializerStepData();
        data.init();

        return data;
    }

    private static JenaSerializerStepMeta getMeta() {
        final JenaSerializerStepMeta meta = new JenaSerializerStepMeta();
        meta.setDefault();

        return meta;
    }

    private static StepMockHelper<JenaSerializerStepMeta, JenaSerializerStepData> mockHelper() {
        final StepMockHelper<JenaSerializerStepMeta, JenaSerializerStepData> helper = new StepMockHelper<>("Serialize Jena Model", JenaSerializerStepMeta.class, JenaSerializerStepData.class);

        when(helper.logChannelInterfaceFactory.create(any(), any(LoggingObjectInterface.class))).thenReturn(helper.logChannelInterface);
        when(helper.trans.isRunning()).thenReturn(true);

        return helper;
    }

    private JenaSerializerStep mockStep(StepMockHelper<JenaSerializerStepMeta, JenaSerializerStepData> helper) {
        final JenaSerializerStep step = Mockito.spy(new JenaSerializerStep(helper.stepMeta, helper.stepDataInterface, 0, helper.transMeta, helper.trans));

        final Collection<Object[]> rows = new ArrayList<>();
        rows.add(
                new Object[]{
                        ModelFactory.createDefaultModel()
                });

        step.setRowHandler(new StaticDataRowHandler(rows));

        final RowMeta inputRowSchema = new RowMeta();
        inputRowSchema.addValueMeta(new ValueMetaInteger("model"));

        doReturn(inputRowSchema).when(step).getInputRowMeta();

        return step;
    }

    private static class StaticDataRowHandler implements RowHandler {
        private final Queue<Object[]> rows;

        public StaticDataRowHandler(Collection<Object[]> rows) {
            this.rows = new LinkedList<>(rows);
        }

        @Override
        public Object[] getRow() {
            if (rows.isEmpty()) {
                return null;
            } else {
                return rows.remove();
            }
        }

        @Override
        public void putRow(RowMetaInterface rowMetaInterface, Object[] objects) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putError(RowMetaInterface rowMetaInterface, Object[] objects, long l, String s, String s1, String s2) {
            throw new UnsupportedOperationException();
        }
    }
}
