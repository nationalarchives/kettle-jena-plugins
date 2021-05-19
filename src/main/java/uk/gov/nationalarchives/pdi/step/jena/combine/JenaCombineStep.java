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
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import uk.gov.nationalarchives.pdi.step.jena.FieldModel;
import uk.gov.nationalarchives.pdi.step.jena.ConstrainedField;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.nationalarchives.pdi.step.jena.JenaUtil.closeAndThrow;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;

public class JenaCombineStep extends BaseStep implements StepInterface {
    private static Class<?> PKG = JenaCombineStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    public JenaCombineStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr,
                           final TransMeta transMeta, final Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {
        Object[] row = getRow(); // try and get a row
        if (row == null) {
            // no more rows...
            setOutputDone();
            return false;  // signal that we are DONE
        }

        // process a row...
        final RowMetaInterface inputRowMeta = getInputRowMeta();
        final JenaCombineStepMeta meta = (JenaCombineStepMeta) smi;
        final JenaCombineStepData data = (JenaCombineStepData) sdi;

        if (first) {
            first = false;

            // create output row meta data
            createOutputRowMeta(inputRowMeta, meta, data);

            // if we are removing fields, we need to map fields from input row to output row
            // NOTE: this must come after createOutputRowMeta
            prepareForReMap(inputRowMeta, meta, data);
        }

        if (!meta.isMutateFirstModel() && isNullOrEmpty(meta.getTargetFieldName())) {
            // error mutate is not set and the target field is empty
            throw new KettleException("Mutate First Model is not selected, and the Target Field Name is empty. One or the other must be selected");
        }

        // get all Jena models from fields and combine
        final List<FieldModel> fieldModels = getModels(meta, row, inputRowMeta);
        final FieldModel combinedFieldModel = combineModels(meta, fieldModels);

        // remap any fields that we are keeping from the input row to the output row
        row = prepareOutputRow(meta, data, row);

        // if we are not mutating the first model, we need to set the combined Jena model as the target field
        if (!meta.isMutateFirstModel()) {
            // Set combined Jena model in target field of the output row
            row[data.getTargetFieldIndex()] = combinedFieldModel.model;
        }

        // output the row
        putRow(data.getOutputRowMeta(), row);

        if (checkFeedback(getLinesRead())) {
            if (log.isBasic())
                logBasic(BaseMessages.getString(PKG, "JenaCombineStep.Log.LineNumber") + getLinesRead());
        }

        return true;  // signal that we want the next row...
    }

    private void createOutputRowMeta(final RowMetaInterface inputRowMeta, final JenaCombineStepMeta meta, final JenaCombineStepData data) throws KettleStepException {
        final RowMetaInterface outputRowMeta = inputRowMeta.clone();
        meta.getFields(outputRowMeta, getStepname(), null, null, this, repository, metaStore);
        data.setOutputRowMeta(outputRowMeta);

        // if there is a target field
        if (meta.isMutateFirstModel()) {
            data.setTargetFieldIndex(null);
        } else {
            // must be done on the output row meta!
            final String expandedTargetFieldName = environmentSubstitute(meta.getTargetFieldName());
            final int targetFieldIndex = outputRowMeta.indexOfValue(expandedTargetFieldName);
            if (targetFieldIndex < 0) {
                throw new KettleStepException(BaseMessages.getString(
                        PKG, "JenaCombineStep.Error.TargetFieldNotFoundOutputStream", meta.getTargetFieldName() + (meta.getTargetFieldName().equals(expandedTargetFieldName) ? "" : "(" + meta.getTargetFieldName() + ")")));
            }
            data.setTargetFieldIndex(targetFieldIndex);
        }
    }

    /**
     * Stores the indexes of any fields from the input row
     * that need to be copied into the output row in the data object.
     *
     * The remapping itself is performed in {@link #prepareOutputRow(JenaCombineStepMeta, JenaCombineStepData, Object[])}.
     *
     * @param inputRowMeta the input row meta
     * @param meta the metadata
     * @param data the data
     *
     * @throws KettleException if an error occurs whilst preparing
     */
    private void prepareForReMap(final RowMetaInterface inputRowMeta, final JenaCombineStepMeta meta, final JenaCombineStepData data) throws KettleStepException {
        // prepare for re-map when removeSelectedFields is checked and mutateFirstModel is not checked
        if (willRemoveFields(meta)) {
            final int hasTargetField = meta.isMutateFirstModel() ? 0 : 1;
            final int[] remainingInputFieldIndexes = new int[data.getOutputRowMeta().size() - hasTargetField]; // NOTE: the `- hasTargetField` - we don't need the new target field (if it is present)

            // fields present in the outputRowMeta
            final String[] outputRowFieldName = data.getOutputRowMeta().getFieldNames();
            // NOTE the `- hasTargetField` - we don't search the new target field (if it is present)
            for (int i = 0; i < outputRowFieldName.length - hasTargetField; i++) {
                final int remainingInputFieldIndex = inputRowMeta.indexOfValue(outputRowFieldName[i]);
                if (remainingInputFieldIndex < 0) {
                    throw new KettleStepException(BaseMessages.getString(PKG,
                            "JenaCombineStep.Error.RemainingFieldNotFoundInputStream", outputRowFieldName[i]));
                }
                remainingInputFieldIndexes[i] = remainingInputFieldIndex;
            }

            data.setRemainingInputFieldIndexes(remainingInputFieldIndexes);
        }
    }

    /**
     * Reserve room for the target field and re-map the fields
     * from input row to output row that were stored in
     * {@link #prepareForReMap(RowMetaInterface, JenaCombineStepMeta, JenaCombineStepData)}.
     *
     * @param meta the metadata
     * @param data the data
     * @param row the input row
     *
     * @return the output row
     */
    private Object[] prepareOutputRow(final JenaCombineStepMeta meta, final JenaCombineStepData data, final Object[] row) {
        final Object[] outputRowData;

        if (willRemoveFields(meta)) {
            // re-map fields from input to output when removeSelectedFields is checked and mutateFirstModel is not checked

            // reserve room for the target field
            outputRowData = new Object[data.getOutputRowMeta().size() + RowDataUtil.OVER_ALLOCATE_SIZE];

            // re-map the fields from input to output
            final int[] remainingInputFieldIndexes = data.getRemainingInputFieldIndexes();
            for (int i = 0; i < remainingInputFieldIndexes.length; i++) { // NOTE: this does not include the new target field (see prepareForReMap)
                final int remainingInputFieldIndex = remainingInputFieldIndexes[i];
                outputRowData[i] = row[remainingInputFieldIndex];
            }

        } else {
            // reserve room for the target field
            outputRowData = RowDataUtil.resizeArray(row, data.getOutputRowMeta().size());
        }
        return outputRowData;
    }


    /**
     * Returns true if fields will be removed
     * from the input row.
     *
     * @para meta the metadata
     *
     * @return true if fields will be removed from the input
     *     row (i.e. not copied to the output row), false otherwise.
     */
    private boolean willRemoveFields(final JenaCombineStepMeta meta) {
        if (!meta.isRemoveSelectedFields()) {
            return false;
        }

        int fieldsToRemove = meta.getJenaModelFields() == null ? 0 : meta.getJenaModelFields().size();
        if (fieldsToRemove > 0 && meta.isMutateFirstModel()) {
            // NOTE: if we are mutating the first model, we don't need to remove it from the output row
            fieldsToRemove--;
        }
        return fieldsToRemove > 0;
    }

    private List<FieldModel> getModels(final JenaCombineStepMeta meta, final Object[] row, final RowMetaInterface inputRowMeta)
            throws KettleException {
        if (meta.getJenaModelFields().isEmpty()) {
            throw new KettleException("No fields configured");
        }

        final List<FieldModel> models = new ArrayList<>(meta.getJenaModelFields().size());

        for (int i = 0; i < meta.getJenaModelFields().size(); i++) {
            final ConstrainedField jenaModelField = meta.getJenaModelFields().get(i);
            if (isNullOrEmpty(jenaModelField.fieldName)) {
                throw new KettleException("Jena Model row: " + getLinesRead() + ", field: " + i + " is missing its field name");
            }

            final String jenaModelFieldName = environmentSubstitute(jenaModelField.fieldName);
            final int idxJenaModelField = inputRowMeta.indexOfValue(jenaModelFieldName);
            if (idxJenaModelField == -1) {
                switch (jenaModelField.actionIfNoSuchField) {
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

    private FieldModel combineModels(final JenaCombineStepMeta meta, final List<FieldModel> fieldModels) throws KettleException {
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

            // first, add each Jena model filed from tail to the headModel
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

                // second, if the field will be removed from the output row
                if (meta.isRemoveSelectedFields()) {
                    // we no longer need this model so we can close it
                    fieldModel.model.close();
                }
            }

            // commit the transaction
            if (headModel.model.supportsTransactions()) {
                headModel.model.commit();
            }

            return headModel;

        } catch (final KettleException e) {
            closeAndThrow(headModel.model, e);
            throw e; // needed for the compiler to pass
        }
    }
}
