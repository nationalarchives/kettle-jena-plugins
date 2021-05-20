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
package uk.gov.nationalarchives.pdi.step.jena.serializer;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;


public class JenaSerializerStepData extends BaseStepData implements StepDataInterface {
    private RowMetaInterface outputRowMeta;

    /**
     * Indexes of fields in the input row
     * that need to be mapped into the output
     * row.
     *
     * Basically the input fields, less any fields
     * that are removed by this step.
     */
    private int[] remainingInputFieldIndexes;

    // the model we are building for serialization
    private Model model;

    public JenaSerializerStepData() {
        super();
    }

    public void init() {
        this.model = ModelFactory.createDefaultModel();
    }

    public Model getModel() {
        return model;
    }

    public void dispose() {
        this.model.close();
        this.model = null;
    }

    public RowMetaInterface getOutputRowMeta() {
        return outputRowMeta;
    }

    public void setOutputRowMeta(final RowMetaInterface outputRowMeta) {
        this.outputRowMeta = outputRowMeta;
    }

    public int[] getRemainingInputFieldIndexes() {
        return remainingInputFieldIndexes;
    }

    public void setRemainingInputFieldIndexes(final int[] remainingInputFieldIndexes) {
        this.remainingInputFieldIndexes = remainingInputFieldIndexes;
    }
}