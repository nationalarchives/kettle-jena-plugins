package uk.gov.nationalarchives.pdi.step.jena.shacl;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.ShaclValidator;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class JenaShaclStepData extends BaseStepData implements StepDataInterface {

    private RowMetaInterface outputRowMeta;
    private ShaclValidator shaclValidator;
    private Graph shapesGraph;

    public void setOutputRowMeta(RowMetaInterface outputRowMeta) {
        this.outputRowMeta = outputRowMeta;
    }

    public RowMetaInterface getOutputRowMeta() {
        return outputRowMeta;
    }

    public void setValidator(ShaclValidator shaclValidator) {
        this.shaclValidator = shaclValidator;
    }

    public ShaclValidator getValidator() {
        return shaclValidator;
    }

    public void setShapesGraph(Graph shapesGraph) {
        this.shapesGraph = shapesGraph;
    }

    public Graph getShapesGraph() {
        return shapesGraph;
    }

    public void dispose() {
        this.shapesGraph.close();
        this.shapesGraph = null;
    }

}
