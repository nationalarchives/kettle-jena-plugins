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
package uk.gov.nationalarchives.pdi.step.jena.groupmerge;

import org.apache.jena.rdf.model.Model;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Objects;


public class JenaGroupMergeStepData extends BaseStepData implements StepDataInterface {

    @Nullable private LinkedHashMap<String, Object> previousGroupFields;
    @Nullable private LinkedHashMap<String, Model> previousGroupModels;
    @Nullable private RowMetaInterface previousGroupOutputRowMeta;

    public JenaGroupMergeStepData() {
        super();
    }

    public @Nullable LinkedHashMap<String, Object> getPreviousGroupFields() {
        return previousGroupFields;
    }

    public void setPreviousGroupFields(final LinkedHashMap<String, Object> previousGroupFields) {
        Objects.requireNonNull(previousGroupFields);
        this.previousGroupFields = previousGroupFields;
    }

    public @Nullable LinkedHashMap<String, Model> getPreviousGroupModels() {
        return previousGroupModels;
    }

    public void setPreviousGroupModels(final LinkedHashMap<String, Model> previousGroupModels) {
        Objects.requireNonNull(previousGroupModels);
        this.previousGroupModels = previousGroupModels;
    }

    public @Nullable RowMetaInterface getPreviousGroupOutputRowMeta() {
        return previousGroupOutputRowMeta;
    }

    public void setPreviousGroupOutputRowMeta(final RowMetaInterface previousGroupOutputRowMeta) {
        Objects.requireNonNull(previousGroupOutputRowMeta);
        this.previousGroupOutputRowMeta = previousGroupOutputRowMeta;
    }

    public void clear() {
        previousGroupFields.clear();
        previousGroupFields = null;
        previousGroupModels.clear();
        previousGroupModels = null;
        previousGroupOutputRowMeta = null;
    }
}