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

        Object[] r = getRow(); // try and get a row
        if (r == null) {

            // serialize the jena model
            try {
                serializeModel(meta, data);
            } catch (final IOException e) {
                throw new KettleException(e.getMessage(), e);
            }

            // no more rows...
            setOutputDone();
            return false;  // signal that we are DONE

        } else {

            // process a row...

            final RowMetaInterface inputRowMeta = getInputRowMeta();
            final RowMetaInterface outputRowMeta = inputRowMeta.clone();
            smi.getFields(outputRowMeta, getStepname(), null, null, this, repository, metaStore);

            //TODO(AR) seems we have to duplicate behaviour of JenaModelStepMeta getFields here but on `r` ???
            if (meta.getJenaModelField() != null && !meta.getJenaModelField().isEmpty()) {
                // get Jena model from row
                final Model model = getModel(meta, r, inputRowMeta);

                // merge this rows model with model for all rows
                data.getModel().add(model);
            }

            if (checkFeedback(getLinesRead())) {
                if (log.isBasic())
                    logBasic(BaseMessages.getString(PKG, "JenaSerializerStep.Log.LineNumber") + getLinesRead());
            }

            return true;  // signal that we want the next row...
        }
    }

    private Model getModel(final JenaSerializerStepMeta meta, final Object[] r, final RowMetaInterface inputRowMeta)
            throws KettleException {
        final String jenaModelField = environmentSubstitute(meta.getJenaModelField());
        final int idxJenaModelField = inputRowMeta.indexOfValue(jenaModelField);
        final Object jenaModelFieldValue =  r[idxJenaModelField];

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
            if (filename == null || filename.isEmpty()) {
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
        if (serializationFormat == null || serializationFormat.isEmpty()) {
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

        model.close();
    }
}
