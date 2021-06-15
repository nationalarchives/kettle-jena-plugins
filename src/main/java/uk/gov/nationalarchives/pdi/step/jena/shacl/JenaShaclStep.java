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
            first = false;
            data.setValidator(ShaclValidator.get());
            data.setShapesGraph(RDFDataMgr.loadGraph(meta.getShapesFilePath()));
            data.setOutputRowMeta(getInputRowMeta().clone());
        }

        final RowMetaInterface inputRowMeta = getInputRowMeta();
        meta.getFields(data.getOutputRowMeta(), getStepname(), null, null, null, null, null);
        final int jenaModelFieldIdx = inputRowMeta.indexOfValue(meta.getJenaModelField());
        final Object jenaModelFieldValue = row[jenaModelFieldIdx];
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
