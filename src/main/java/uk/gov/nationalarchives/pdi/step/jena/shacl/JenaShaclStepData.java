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
