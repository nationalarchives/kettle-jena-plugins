package uk.gov.nationalarchives.pdi.step.jena.groupmerge;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaSerializable;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.trans.RowStepCollector;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransTestFactory;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNoSuchField;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNull;
import uk.gov.nationalarchives.pdi.step.jena.ConstrainedField;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JenaGroupMergeStepIT {

    static final String STEP_NAME = "Integration test for Jena Group Merge step";

    @BeforeAll
    public static void setUpBeforeClass() throws KettleException {
        KettleEnvironment.init(false);
    }

    @Test
    public void hasNoErrorsMergingTwoRows() throws KettleException {
        final TransMeta tm = TransTestFactory.generateTestTransformationError(new Variables(), getTestMeta(), STEP_NAME);
        final Map<String, RowStepCollector> result = TransTestFactory.executeTestTransformationError(tm, TransTestFactory.INJECTOR_STEPNAME,
                STEP_NAME, TransTestFactory.DUMMY_STEPNAME, TransTestFactory.ERROR_STEPNAME, generateInputData("FO_371_190180_1-policy.ttl"));
        assertEquals(0, result.get(STEP_NAME).getRowsError().size());
    }

    @Test
    public void hasNoErrorsMergingTwoRowsWithNullValueColumn() throws KettleException {
        final TransMeta tm = TransTestFactory.generateTestTransformationError(new Variables(), getTestMeta(), STEP_NAME);
        final Map<String, RowStepCollector> result = TransTestFactory.executeTestTransformationError(tm, TransTestFactory.INJECTOR_STEPNAME,
                STEP_NAME, TransTestFactory.DUMMY_STEPNAME, TransTestFactory.ERROR_STEPNAME, generateInputData2("FO_371_190180_1-policy.ttl"));
        assertEquals(true, result.get(STEP_NAME).getRowsWritten().get(0).getData()[0] == null);
        assertEquals(true, result.get(STEP_NAME).getRowsWritten().get(0).getData()[1] == "true");
        assertEquals(true, result.get(STEP_NAME).getRowsWritten().get(0).getData()[2] instanceof Model);
    }

    private JenaGroupMergeStepMeta getTestMeta() {
        final JenaGroupMergeStepMeta meta = new JenaGroupMergeStepMeta();
        final List<ConstrainedField> groupFields = new ArrayList<>(1);
        groupFields.add(new ConstrainedField("id", ActionIfNoSuchField.ERROR, ActionIfNull.IGNORE));
        groupFields.add(new ConstrainedField("test", ActionIfNoSuchField.ERROR, ActionIfNull.IGNORE));
        meta.setGroupFields(groupFields);
        meta.setMutateFirstModel(true);
        final List<ConstrainedField> jenaModelFields = new ArrayList<>(1);
        jenaModelFields.add(new ConstrainedField("jena_model",ActionIfNoSuchField.ERROR,ActionIfNull.ERROR));
        meta.setJenaModelFields(jenaModelFields);
        return meta;
    }

    private List<RowMetaAndData> generateInputData(final String filename) {
        final List<RowMetaAndData> retval = new ArrayList<>();
        final RowMetaInterface rowMeta = new RowMeta();
        final String modelFilePath = getFilePath(filename);
        final Model model = RDFDataMgr.loadModel(modelFilePath);
        rowMeta.addValueMeta(new ValueMetaInteger("id"));
        rowMeta.addValueMeta(new ValueMetaString("test"));
        rowMeta.addValueMeta(new ValueMetaSerializable("jena_model"));
        retval.add(new RowMetaAndData(rowMeta, 1, "true", model));
        retval.add(new RowMetaAndData(rowMeta, 1, "true", model));
        return retval;
    }

    private List<RowMetaAndData> generateInputData2(final String filename) {
        final List<RowMetaAndData> retval = new ArrayList<>();
        final RowMetaInterface rowMeta = new RowMeta();
        final String modelFilePath = getFilePath(filename);
        final Model model = RDFDataMgr.loadModel(modelFilePath);
        rowMeta.addValueMeta(new ValueMetaInteger("id"));
        rowMeta.addValueMeta(new ValueMetaString("test"));
        rowMeta.addValueMeta(new ValueMetaSerializable("jena_model"));
        retval.add(new RowMetaAndData(rowMeta, null, "true", model));
        retval.add(new RowMetaAndData(rowMeta, null, "true", model));
        return retval;
    }

    private String getFilePath(final String filename) {
        final URL url = this.getClass().getResource("/" + filename);
        assert url != null;
        return url.toString();
    }
}
