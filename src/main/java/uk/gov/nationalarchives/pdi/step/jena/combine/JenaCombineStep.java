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
package uk.gov.nationalarchives.pdi.step.jena.combine;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import uk.gov.nationalarchives.pdi.step.jena.FieldModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static uk.gov.nationalarchives.pdi.step.jena.JenaUtil.closeAndThrow;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNotEmpty;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;

public class JenaCombineStep extends BaseStep implements StepInterface {
    private static Class<?> PKG = JenaCombineStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    public JenaCombineStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr,
                           final TransMeta transMeta, final Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {

        final JenaCombineStepMeta meta = (JenaCombineStepMeta) smi;

        Object[] row = getRow(); // try and get a row
        if (row == null) {
            // no more rows...
            setOutputDone();
            return false;  // signal that we are DONE

        } else {

            // process a row...

            final RowMetaInterface inputRowMeta = getInputRowMeta();
            final RowMetaInterface outputRowMeta = inputRowMeta.clone();
            smi.getFields(outputRowMeta, getStepname(), null, null, this, repository, metaStore);

            if (meta.isMutateFirstModel() || isNotEmpty(meta.getTargetFieldName())) {
                // get all Jena models from fields
                final List<FieldModel> fieldModels = getModels(meta, row, inputRowMeta);

                // get the Head Jena Model
                final int tailIdx;
                final FieldModel headModel;
                if (meta.isMutateFirstModel()) {
                    // get first model
                    headModel = fieldModels.get(0);
                    tailIdx = 1;

                    if (headModel.model.isClosed()) {
                        throw new KettleException("Head Model (mutateFirstModel=true) is already closed in row: " + getLinesRead() + " for field: " + headModel.fieldName);
                    }
                } else {
                    // create new model
                    headModel = new FieldModel(ModelFactory.createDefaultModel());
                    tailIdx = 0;
                }

                try {
                    // start a transaction on the model
                    if (headModel.model.supportsTransactions()) {
                        headModel.model.begin();
                    }

                    // first, add each Jena model from fields to the baseModel
                    int[] removeIndexes = new int[0];
                    for (int i = tailIdx; i < fieldModels.size(); i++) {
                        final FieldModel fieldModel = fieldModels.get(i);

                        if (fieldModel.model.isClosed()) {
                            throw new KettleException("Tail Model[" + i + "] (mutateFirstModel=" + meta.isMutateFirstModel() + ") is already closed in row: " + getLinesRead() + " for field: " + fieldModel.fieldName);
                        }

                        if (fieldModel.model.supportsTransactions()) {
                            fieldModel.model.begin();
                        }

                        headModel.model.add(fieldModel.model);

                        if (fieldModel.model.supportsTransactions()) {
                            fieldModel.model.commit();
                        }

                        // second, remove the column if it is no longer needed
                        if (meta.isRemoveSelectedFields()) {

                            // we no longer need this model so we can close it
                            fieldModel.model.close();

                            final String jenaModelFieldName = environmentSubstitute(meta.getJenaModelFields().get(i).fieldName);
                            final int removeIndex = inputRowMeta.indexOfValue(jenaModelFieldName);
                            removeIndexes = Arrays.copyOf(removeIndexes, removeIndexes.length + 1);
                            removeIndexes[removeIndexes.length - 1] = removeIndex;
                        }
                    }

                    Arrays.sort(removeIndexes);
                    row = RowDataUtil.removeItems(row, removeIndexes);

                    if (meta.getTargetFieldName() != null) {
                        row = RowDataUtil.resizeArray(row, inputRowMeta.size() - removeIndexes.length + 1);
                        row[inputRowMeta.size()] = headModel.model;
                        // TODO AR how does it know the target field name in the output row?
                    }

                    putRow(outputRowMeta, row); // copy row to possible alternate rowset(s).

                    if (checkFeedback(getLinesRead())) {
                        if (log.isBasic())
                            logBasic(BaseMessages.getString(PKG, "JenaCombineStep.Log.LineNumber") + getLinesRead());
                    }

                    // commit the transaction
                    if (headModel.model.supportsTransactions()) {
                        headModel.model.commit();
                    }

                    return true;  // signal that we want the next row...

                } catch (final KettleException e) {
                   closeAndThrow(headModel.model, e);
                   throw e; // needed for the compiler to pass
                }

            } else {
                // error mutate is not set and the target field is empty
                throw new KettleException("Mutate First Model is not selected, and the Target Field Name is empty. One or the other must be selected");
            }
        }
    }

    private List<FieldModel> getModels(final JenaCombineStepMeta meta, final Object[] row, final RowMetaInterface inputRowMeta)
            throws KettleException {
        if (meta.getJenaModelFields().isEmpty()) {
            throw new KettleException("No fields configured");
        }

        final List<FieldModel> models = new ArrayList<>(meta.getJenaModelFields().size());

        for (int i = 0; i < meta.getJenaModelFields().size(); i++) {
            final JenaCombineStepMeta.JenaModelField jenaModelField = meta.getJenaModelFields().get(i);
            if (isNullOrEmpty(jenaModelField.fieldName)) {
                throw new KettleException("Jena Model row: " + getLinesRead() + ", field: " + i + " is missing its field name");
            }

            final String jenaModelFieldName = environmentSubstitute(jenaModelField.fieldName);
            final int idxJenaModelField = inputRowMeta.indexOfValue(jenaModelFieldName);
            if (idxJenaModelField == -1) {
                switch (jenaModelField.actionIfNull) {
                    case IGNORE:
                        // no-op - just ignore it!
                        break;

                    case WARN:
                        // log a warning
                        logBasic("Could not combine model in row: {0}, field: {1}, column is absent!", getLinesRead(), jenaModelField.fieldName);
                        break;

                    case ERROR:
                        // throw an exception
                        throw new KettleException("Could not combine model in row: " + getLinesRead() + ", field: " + jenaModelField.fieldName + ", column is absent!");
                }
            } else {
                final Object jenaModelFieldValue = row[idxJenaModelField];
                if (jenaModelFieldValue == null) {
                    switch (jenaModelField.actionIfNull) {
                        case IGNORE:
                            // no-op - just ignore it!
                            break;

                        case WARN:
                            // log a warning
                            logBasic("Could not combine model in row: {0}, field: {1}, value is null!", getLinesRead(), jenaModelField.fieldName);
                            break;

                        case ERROR:
                            // throw an exception
                            throw new KettleException("Could not combine model in row: " + getLinesRead() + ", field: " + jenaModelField.fieldName + ", value is null!");
                    }
                } else {
                    if (jenaModelFieldValue instanceof Model) {
                        models.add(new FieldModel(jenaModelFieldName, (Model) jenaModelFieldValue));
                    } else {
                        throw new KettleException("Expected row: " + getLinesRead() + ", field: " + jenaModelFieldName + " to contain a Jena Model, but found "
                                + jenaModelFieldValue.getClass());
                    }
                }
            }
        }

        return models;
    }
}
