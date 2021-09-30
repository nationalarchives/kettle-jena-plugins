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

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

/**
 *  A PDI plugin step which can be used to validate an Apache Jena Model contained in a row field against a SHACL shape
 *  file loaded from a give path on the file system
 */
public class JenaShaclStep extends BaseStep implements StepInterface {

    public JenaShaclStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr,
                         final TransMeta transMeta, final Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {
        final JenaShaclStepMeta meta = (JenaShaclStepMeta) smi;
        final JenaShaclStepData data = (JenaShaclStepData) sdi;

        final Object[] row = getRow();
        if (row == null) {
            setOutputDone();
            return false;
        }

        if(first) {
            try{
                data.setShapesGraph(RDFDataMgr.loadGraph(environmentSubstitute(meta.getShapesFilePath())));
            } catch (Exception ex) {
                throw new KettleException("Unable to load SHACL shape file due to " + ex.getMessage(), ex);
            }
            data.setValidator(ShaclValidator.get());
            data.setOutputRowMeta(getInputRowMeta().clone());
            final RowMetaInterface inputRowMeta = getInputRowMeta();
            data.setJenaModelFieldIdx(inputRowMeta.indexOfValue(meta.getJenaModelField()));
            meta.getFields(data.getOutputRowMeta(), getStepname(), null, null, null, null, null);
            first = false;
        }
        final Object jenaModelFieldValue = row[data.getJenaModelFieldIdx()];
        if (jenaModelFieldValue instanceof Model) {
            final ValidationResult result = validate((Model) jenaModelFieldValue,data);
            if (!result.hasErrors()) {
                putRow(data.getOutputRowMeta(), row);
            } else {
                putError(data.getOutputRowMeta(), row, result.getErrorCount(), result.getAllErrors(), "data", "ERROR_01");
            }
            return true;
        } else {
            throw new KettleException("Expected field " + jenaModelFieldValue + " to contain a Jena Model, but found "
                    + jenaModelFieldValue.getClass());
        }
    }

    @Override
    public void dispose(final StepMetaInterface smi, final StepDataInterface sdi) {
        super.dispose(smi, sdi);
        final JenaShaclStepData data = (JenaShaclStepData) sdi;
        data.dispose();
    }

    private ValidationResult validate(final Model dataModel, final JenaShaclStepData data) {
        ValidationResult result = new ValidationResult();
        final Graph dataGraph = dataModel.getGraph();
        final Graph shapesGraph = data.getShapesGraph();
        final ShaclValidator validator = data.getValidator();
        if (!validator.conforms(shapesGraph, dataGraph)) {
            ValidationReport report = validator.validate(shapesGraph, dataGraph);
            for (final ReportEntry reportEntry : report.getEntries()) {
                result.appendError(reportEntry.message());
                result.setHasErrors(true);
                result.incrementErrorCount();
            }
        }
        return result;
    }

}
