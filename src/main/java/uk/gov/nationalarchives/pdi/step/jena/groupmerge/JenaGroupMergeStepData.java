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
package uk.gov.nationalarchives.pdi.step.jena.groupmerge;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Objects;


public class JenaGroupMergeStepData extends BaseStepData implements StepDataInterface {

    private RowMetaInterface outputRowMeta;

    /**
     * Indexes of fields in the input row
     * that need to be mapped into the output
     * row.
     *
     * Basically the input fields, less any fields
     * that are removed by this step.
     */
    private LinkedHashMap<String, Integer> remainingInputFieldIndexes;

    @Nullable private Object[] groupMergedRow;

    public JenaGroupMergeStepData() {
        super();
    }

    public @Nullable Object[] getGroupMergedRow() {
        return groupMergedRow;
    }

    public void setGroupMergedRow(final Object[] groupMergedRow) {
        Objects.requireNonNull(groupMergedRow);
        this.groupMergedRow = groupMergedRow;
    }

    public RowMetaInterface getOutputRowMeta() {
        return outputRowMeta;
    }

    public void setOutputRowMeta(final RowMetaInterface outputRowMeta) {
        this.outputRowMeta = outputRowMeta;
    }

    public LinkedHashMap<String, Integer> getRemainingInputFieldIndexes() {
        return remainingInputFieldIndexes;
    }

    public void setRemainingInputFieldIndexes(final LinkedHashMap<String, Integer> remainingInputFieldIndexes) {
        this.remainingInputFieldIndexes = remainingInputFieldIndexes;
    }

    public void clear() {
        groupMergedRow = null;
    }
}