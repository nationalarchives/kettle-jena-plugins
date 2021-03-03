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
import org.apache.jena.rdf.model.ModelFactory;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

import javax.annotation.Nullable;
import java.util.*;

import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;

public class JenaGroupMergeStep extends BaseStep implements StepInterface {
    private static Class<?> PKG = JenaGroupMergeStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    public JenaGroupMergeStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr,
                              final TransMeta transMeta, final Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {

        final JenaGroupMergeStepMeta meta = (JenaGroupMergeStepMeta) smi;
        final JenaGroupMergeStepData data = (JenaGroupMergeStepData) sdi;

        Object[] row = getRow(); // try and get a row
        if (row == null) {

            // output the last group
            if (data.getPreviousGroupFields() != null) {
                outputGroup(data);
            }

            // no more rows...
            setOutputDone();

            return false;  // signal that we are DONE

        } else {

            // process a row...

            final RowMetaInterface inputRowMeta = getInputRowMeta();
            final RowMetaInterface outputRowMeta = inputRowMeta.clone();
            smi.getFields(outputRowMeta, getStepname(), null, null, this, repository, metaStore);

            if (meta.isMutateFirstModel() || meta.getTargetFieldName() != null && !meta.getTargetFieldName().isEmpty()) {

                // get all group fields
                final LinkedHashMap<String, Object> currentGroupFields = getGroupFields(meta, row, inputRowMeta);

                // is this the first row in a group or a continuation of an existing group?
                final LinkedHashMap<String, Object> previousGroupFields = data.getPreviousGroupFields();
                if (isContinuation(previousGroupFields, currentGroupFields)) {

                    // this is a continuation row within a group

                    // merge the models from this row with the models from the previous row
                    final List<FieldModel> currentRowModels = getModels(meta, row, inputRowMeta);
                    mergeModels(meta, data, currentRowModels);

                } else {

                    // this is the first row within a group

                    // was there a previous group?
                    if (previousGroupFields != null) {
                        /*
                            yes, there was a previous group,
                            so we must output it and clear the previous data
                         */
                        outputGroup(data);
                    }

                    // persist the values of the 1st row of this group, so on the next call to processRow we can compare it
                    //TODO(AR) do we need to make a copy?
//                    final LinkedHashMap<String, Object> copyOfCurrentGroupFields = new LinkedHashMap<>(currentGroupFields.size());
//                    for (final Map.Entry<String, Object> currentGroupField : currentGroupFields.entrySet()) {
//                        final Object valueCopy = currentGroupField.getValue().clone();
//                        copyOfCurrentGroupFields.put(currentGroupField.getKey(), valueCopy);
//                    }
                    data.setPreviousGroupFields(currentGroupFields);

                    final List<FieldModel> currentRowModels = getModels(meta, row, inputRowMeta);
                    final LinkedHashMap<String, Model> currentGroupModels = new LinkedHashMap<>(currentRowModels.size());

                    // should we mutate the first model in the group of rows, or create a new model and copy it in?
                    for (final FieldModel currentRowModel : currentRowModels) {
                        final Model model;
                        if(currentRowModel.model == null) {
                            // may be created later in mergeModels
                            model = null;

                        } else if (meta.isMutateFirstModel()) {
                            // mutate the first model
                            model = currentRowModel.model;

                        } else {
                            // create a new model
                            model = ModelFactory.createDefaultModel();
                            model.add(currentRowModel.model);

                            /*
                               Model can no longer be used if
                               the user chose to remove it from the available
                               output fields... so close it!
                             */
                            if (meta.isRemoveSelectedFields()) {
                                currentRowModel.model.close();
                            }
                        }

                        currentGroupModels.put(currentRowModel.fieldName, model);
                    }

                    data.setPreviousGroupModels(currentGroupModels);

                    final RowMetaInterface previousGroupOutputRowMeta = outputRowMeta.clone();
                    // TODO(AR) do we need to adjust the previousGroupOutputRowMeta to name the fields etc?
                    // TODO(AR) don't forget about the target field
                    // TODO(AR) don't forget about the remove selected field
                    data.setPreviousGroupOutputRowMeta(previousGroupOutputRowMeta);
                }

                if (checkFeedback(getLinesRead())) {
                    if (log.isBasic())
                        logBasic(BaseMessages.getString(PKG, "JenaGroupMergeStep.Log.LineNumber") + getLinesRead());
                }

                return true;

            } else {
                // error mutate is not set and the target field is empty
                throw new KettleException("Mutate First Model is not selected, and the Target Field Name is empty. One or the other must be selected");
            }
        }
    }

    private void mergeModels(final JenaGroupMergeStepMeta meta, final JenaGroupMergeStepData data, final List<FieldModel> currentRowModels) {
        final LinkedHashMap<String, Model> previousGroupModels = data.getPreviousGroupModels();

        for (final Map.Entry<String, Model> previousGroupModel : previousGroupModels.entrySet()) {
            final Model currentRowFieldModel = getFieldModel(currentRowModels, previousGroupModel.getKey());
            if (currentRowFieldModel != null) {

                final Model model;
                if (previousGroupModel.getValue() == null) {
                    // no previous model, store this model as the previous model
                    if (meta.isMutateFirstModel()) {
                        // mutate this model
                        model = currentRowFieldModel;
                    } else {
                        // create a new model
                        model = ModelFactory.createDefaultModel();
                        model.add(currentRowFieldModel);

                        /*
                           Model can no longer be used if
                           the user chose to remove it from the available
                           output fields... so close it!
                         */
                        if (meta.isRemoveSelectedFields()) {
                            currentRowFieldModel.close();
                        }
                    }

                    // persist the model, so on the next call to processRow we can compare it
                    previousGroupModels.put(previousGroupModel.getKey(), model);

                } else {
                    // merge this model with previous model
                    previousGroupModel.getValue().add(currentRowFieldModel);

                    /*
                       Model can no longer be used if
                       the user chose to remove it from the available
                       output fields... so close it!
                     */
                    if (meta.isRemoveSelectedFields()) {
                        currentRowFieldModel.close();
                    }
                }
            }
        }
    }

    private static @Nullable Model getFieldModel(final List<FieldModel> fieldModels, final String fieldName) {
        for (final FieldModel fieldModel : fieldModels) {
            if (fieldModel.fieldName.equals(fieldName)) {
                return fieldModel.model;
            }
        }
        return null;
    }

    private void outputGroup(final JenaGroupMergeStepData data) throws KettleStepException {
        final LinkedHashMap<String, Object> previousGroupFields = data.getPreviousGroupFields();
        final LinkedHashMap<String, Model> previousGroupModels = data.getPreviousGroupModels();
        final int rowSize = previousGroupFields.size() + previousGroupModels.size();
        final Object[] previousGroupRow = RowDataUtil.allocateRowData(rowSize);

        // TODO(AR) don't forget about the target field
        // TODO(AR) don't forget about the remove selected field

        int i = 0;
        for (final Map.Entry<String, Object> previousGroupField : previousGroupFields.entrySet()) {
            previousGroupRow[i++] = previousGroupField.getValue();
        }
        for (final Map.Entry<String, Model> previousGroupModel : previousGroupModels.entrySet()) {
            previousGroupRow[i++] = previousGroupModel.getValue();
        }

        putRow(data.getPreviousGroupOutputRowMeta(), previousGroupRow);

        data.clear();
    }

    private boolean isContinuation(@Nullable final LinkedHashMap<String, Object> previousGroupFields, final LinkedHashMap<String, Object> currentGroupFields) {
        if (previousGroupFields == null || previousGroupFields.size() != currentGroupFields.size()) {
            return false;
        }

        for (final Map.Entry<String, Object> currentGroupField : currentGroupFields.entrySet()) {
            final String currentFieldName = currentGroupField.getKey();
            final Object currentFieldValue = currentGroupField.getValue();

            if (!previousGroupFields.containsKey(currentFieldName)) {
                return false;
            }

            final Object previousFieldValue = previousGroupFields.get(currentFieldName);
            if (currentFieldValue == null ^ previousFieldValue == null) {
                return false;
            }

            if (currentFieldValue != null) {
                if (!currentFieldValue.equals(previousFieldValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static class FieldModel {
        @Nullable final String fieldName;
        final Model model;

        private FieldModel(final String fieldName, final Model model) {
            this.fieldName = fieldName;
            this.model = model;
        }

        public FieldModel(final Model model) {
            this(null, model);
        }
    }

    private LinkedHashMap<String, Object> getGroupFields(final JenaGroupMergeStepMeta meta, final Object[] row, final RowMetaInterface inputRowMeta) throws KettleException {
        // NOTE: order is important, so we use a LinkedHashMap
        final LinkedHashMap<String, Object> groupFields = new LinkedHashMap<>(meta.getGroupFields().size());

        for (int i = 0; i < meta.getGroupFields().size(); i++) {
            final String groupField = meta.getGroupFields().get(i);
            if (isNullOrEmpty(groupField)) {
                throw new KettleException("Group field: " + i + " is missing its field name");
            }

            final String groupFieldName = environmentSubstitute(groupField);
            final int idxGroupField = inputRowMeta.indexOfValue(groupFieldName);
            if (idxGroupField == -1) {
                throw new KettleException("Group field: " + groupFieldName + ", column is absent in row!");
            } else {
                final Object groupFieldValue = row[idxGroupField];
                groupFields.put(groupFieldName, groupFieldValue);
            }
        }

        return groupFields;
    }

    private List<FieldModel> getModels(final JenaGroupMergeStepMeta meta, final Object[] row, final RowMetaInterface inputRowMeta)
            throws KettleException {
        if (meta.getJenaModelMergeFields().isEmpty()) {
            throw new KettleException("No fields configured");
        }

        final List<FieldModel> models = new ArrayList<>(meta.getJenaModelMergeFields().size());

        for (int i = 0; i < meta.getJenaModelMergeFields().size(); i++) {
            final JenaGroupMergeStepMeta.JenaModelField jenaModelField = meta.getJenaModelMergeFields().get(i);
            if (jenaModelField.fieldName == null || jenaModelField.fieldName.isEmpty()) {
                throw new KettleException("Jena Model field: " + i + " is missing its field name");
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
                        logBasic("Could not group and merge model for row field: {0}, column is absent!", jenaModelField.fieldName);
                        break;

                    case ERROR:
                        // throw an exception
                        throw new KettleException("Could not group and merge model for row field: " + jenaModelField.fieldName + ", column is absent!");
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
                            logBasic("Could not group and merge model for row field: {0}, value is null!", jenaModelField.fieldName);
                            break;

                        case ERROR:
                            // throw an exception
                            throw new KettleException("Could not group and merge model for row field: " + jenaModelField.fieldName + ", value is null!");
                    }
                } else {
                    if (jenaModelFieldValue instanceof Model) {
                        models.add(new FieldModel(jenaModelFieldName, (Model) jenaModelFieldValue));
                    } else {
                        throw new KettleException("Expected field '" + jenaModelFieldName + "' to contain a Jena Model, but found "
                                + jenaModelFieldValue.getClass());
                    }
                }
            }
        }

        return models;
    }
}
