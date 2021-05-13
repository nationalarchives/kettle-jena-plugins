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

import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import uk.gov.nationalarchives.pdi.step.jena.Rdf11;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdf.model.RDFWriterF;
import org.apache.jena.rdf.model.impl.RDFWriterFImpl;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.gov.nationalarchives.pdi.step.jena.JenaUtil.closeAndThrow;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNotEmpty;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;

public class JenaSerializerStep extends BaseStep implements StepInterface {
    private static Class<?> PKG = JenaSerializerStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    public JenaSerializerStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr,
            final TransMeta transMeta, final Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    /**
     * Initialize and do work where other steps need to wait for...
     *
     * @param smi The metadata to work with
     * @param sdi The data to initialize
     */
    @Override
    public boolean init(final StepMetaInterface smi, final StepDataInterface sdi) {
        final boolean result = super.init(smi, sdi);

        final JenaSerializerStepData data = (JenaSerializerStepData) sdi;
        data.init();

        return result;
    }

    @Override
    public void dispose(final StepMetaInterface smi, final StepDataInterface sdi) {
        super.dispose(smi, sdi);

        final JenaSerializerStepData data = (JenaSerializerStepData) sdi;
        data.dispose();
    }

    @Override
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {
        final JenaSerializerStepMeta meta = (JenaSerializerStepMeta) smi;
        final JenaSerializerStepData data = (JenaSerializerStepData) sdi;

        Object[] row = getRow(); // try and get a row
        if (row == null) {
            // serialize the jena model
            try {
                serializeModel(meta, data);
            } catch (final IOException e) {
                throw new KettleException(e.getMessage(), e);
            }

            // no more rows...
            setOutputDone();
            return false;  // signal that we are DONE
        }

        // process a row...
        final RowMetaInterface inputRowMeta = getInputRowMeta();

        if (first) {
            first = false;

            // create output row meta data
            createOutputRowMeta(inputRowMeta, meta, data);

            // if we are removing fields, we need to map fields from input row to output row
            // NOTE: this must come after createOutputRowMeta
            prepareForReMap(inputRowMeta, meta, data);
        }

        if (isNotEmpty(meta.getJenaModelField())) {
            // get Jena model from this row
            final Model model = getModel(meta, row, inputRowMeta);
            try {
                // merge this row's Jena model with our Jena model for serialization
                data.getModel().add(model);
            } finally {
                //TODO(AR) consider adding a 'removeSelectedFields' option to the dialog, if not set, don't close and remove, instead call putRow to pass it on.

                /*
                    if closeModelAndRemoveField is selected, we are now
                    finished with the Jena model from this row, so we
                    close it to release any resources
                 */
                if (meta.isCloseModelAndRemoveField()) {
                    model.close();
                }
            }
        }

        // remap any fields that we are keeping from the input row to the output row
        row = prepareOutputRow(meta, data, row);

        // output the row
        putRow(data.getOutputRowMeta(), row);

        if (checkFeedback(getLinesRead())) {
            if (log.isBasic())
                logBasic(BaseMessages.getString(PKG, "JenaSerializerStep.Log.LineNumber") + getLinesRead());
        }

        return true;  // signal that we want the next row...
    }

    private void createOutputRowMeta(final RowMetaInterface inputRowMeta, final JenaSerializerStepMeta meta, final JenaSerializerStepData data) throws KettleStepException {
        final RowMetaInterface outputRowMeta = inputRowMeta.clone();
        meta.getFields(outputRowMeta, getStepname(), null, null, this, repository, metaStore);
        data.setOutputRowMeta(outputRowMeta);
    }

    /**
     * Stores the indexes of any fields from the input row
     * that need to be copied into the output row in the data object.
     *
     * The remapping itself is performed in {@link #prepareOutputRow(JenaSerializerStepMeta, JenaSerializerStepData, Object[])}.
     *
     * @param inputRowMeta the input row meta
     * @param meta the metadata
     * @param data the data
     *
     * @throws KettleException if an error occurs whilst preparing
     */
    private void prepareForReMap(final RowMetaInterface inputRowMeta, final JenaSerializerStepMeta meta, final JenaSerializerStepData data) throws KettleStepException {
        // prepare for re-map when closeModelAndRemoveField
        if (meta.isCloseModelAndRemoveField()) {
            final int[] remainingInputFieldIndexes = new int[data.getOutputRowMeta().size()];

            // fields present in the outputRowMeta
            final String[] outputRowFieldName = data.getOutputRowMeta().getFieldNames();
            for (int i = 0; i < outputRowFieldName.length; i++) {
                final int remainingInputFieldIndex = inputRowMeta.indexOfValue(outputRowFieldName[i]);
                if (remainingInputFieldIndex < 0) {
                    throw new KettleStepException(BaseMessages.getString(PKG,
                            "JenaSerializerStep.Error.RemainingFieldNotFoundInputStream", outputRowFieldName[i]));
                }
                remainingInputFieldIndexes[i] = remainingInputFieldIndex;
            }

            data.setRemainingInputFieldIndexes(remainingInputFieldIndexes);
        }
    }

    /**
     * re-map the fields from input row to output row that were stored in
     * {@link #prepareForReMap(RowMetaInterface, JenaSerializerStepMeta, JenaSerializerStepData)}.
     *
     * @param meta the metadata
     * @param data the data
     * @param row the input row
     *
     * @return the output row
     */
    private Object[] prepareOutputRow(final JenaSerializerStepMeta meta, final JenaSerializerStepData data, final Object[] row) {
        final Object[] outputRowData;

        if (meta.isCloseModelAndRemoveField()) {
            // re-map fields from input to output when closeModelAndRemoveField is checked

            outputRowData = new Object[data.getOutputRowMeta().size() + RowDataUtil.OVER_ALLOCATE_SIZE];

            // re-map the fields from input to output
            final int[] remainingInputFieldIndexes = data.getRemainingInputFieldIndexes();
            for (int i = 0; i < remainingInputFieldIndexes.length; i++) {
                final int remainingInputFieldIndex = remainingInputFieldIndexes[i];
                outputRowData[i] = row[remainingInputFieldIndex];
            }

        } else {
            outputRowData = RowDataUtil.resizeArray(row, data.getOutputRowMeta().size());
        }
        return outputRowData;
    }

    private Model getModel(final JenaSerializerStepMeta meta, final Object[] row, final RowMetaInterface inputRowMeta)
            throws KettleException {
        final String jenaModelField = environmentSubstitute(meta.getJenaModelField());
        final int idxJenaModelField = inputRowMeta.indexOfValue(jenaModelField);
        final Object jenaModelFieldValue =  row[idxJenaModelField];

        if (jenaModelFieldValue instanceof Model) {
            return (Model) jenaModelFieldValue;
        } else {
            throw new KettleException("Expected field " + jenaModelField + " to contain a Jena Model, but found "
                    + jenaModelFieldValue.getClass());
        }
    }

    private void serializeModel(final JenaSerializerStepMeta meta, final JenaSerializerStepData data) throws IOException {
        final Model model = data.getModel();

        String filename = JenaSerializerStepMeta.DEFAULT_FILENAME;

        final JenaSerializerStepMeta.FileDetail fileDetail = meta.getFileDetail();
        if (fileDetail != null) {
            filename = environmentSubstitute(fileDetail.filename);
            if (isNullOrEmpty(filename)) {
                filename = JenaSerializerStepMeta.DEFAULT_FILENAME;
            }

            String additional = "";

            if (fileDetail.includeStepNr) {
                final int stepNr = getUniqueStepNrAcrossSlaves();
                additional += ('.' + stepNr);
            }

            if (fileDetail.includePartitionNr) {
                final String partitionNr = getPartitionID();
                additional += ('.' + partitionNr);
            }

            Date now = null;
            if (fileDetail.includeDate) {
                if (now == null) {
                    now = Calendar.getInstance().getTime();
                }
                final SimpleDateFormat df = new SimpleDateFormat("yyyMMdd");
                additional += ('.' + df.format(now));
            }

            if (fileDetail.includeTime) {
                if (now == null) {
                    now = Calendar.getInstance().getTime();
                    additional += '.';
                }

                final SimpleDateFormat df = new SimpleDateFormat( "HHmmss");
                additional += df.format(now);
            }

            // combine additional
            if (!additional.isEmpty()) {
                final int extSep = filename.lastIndexOf('.');
                final String name;
                final String ext;
                if (extSep > -1 ) {
                    name = filename.substring(0, extSep);
                    ext = filename.substring(extSep + 1);
                } else {
                    name = filename;
                    ext = "";
                }

                filename = name + additional + '.' + ext;
            }
        }

        String serializationFormat = environmentSubstitute(meta.getSerializationFormat());
        if (isNullOrEmpty(serializationFormat)) {
            serializationFormat = Rdf11.DEFAULT_SERIALIZATION_FORMAT;
        }

        final Path path = Paths.get(filename);
        if (!Files.exists(path)) {
            if (fileDetail != null && fileDetail.createParentFolder) {
                Files.createDirectories(path.getParent());
            }
        }

        final RDFWriterF factory = new RDFWriterFImpl();
        final RDFWriter rdfWriter = factory.getWriter(serializationFormat);

        try {
            // start a transaction on the model
            if (model.supportsTransactions()) {
                model.begin();
            }

            try (final Writer writer = new OutputStreamWriter(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), UTF_8)) {
                //model.write(writer, serializationFormat);
                rdfWriter.write(model, writer, "");
            }

            // finish the transaction on the model
            if (model.supportsTransactions()) {
                model.commit();
            }
        } catch (final IOException e) {
            closeAndThrow(model, e);
        } finally {
            model.close();
        }
    }
}
