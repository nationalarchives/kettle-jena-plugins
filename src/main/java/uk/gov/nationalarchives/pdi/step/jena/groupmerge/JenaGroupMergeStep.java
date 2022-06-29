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

import com.evolvedbinary.j8fu.function.QuadFunction;
import com.evolvedbinary.j8fu.function.QuintFunction;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import uk.gov.nationalarchives.pdi.step.jena.ConstrainedField;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;

//TODO(AR) make sure we are using environmentSubstitute on all fieldNames and targetFieldNames where appropriate

public class JenaGroupMergeStep extends BaseStep implements StepInterface {
    private static final Class<?> PKG = JenaGroupMergeStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    public JenaGroupMergeStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr,
            final TransMeta transMeta, final Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {

        final JenaGroupMergeStepMeta meta = (JenaGroupMergeStepMeta) smi;
        final JenaGroupMergeStepData data = (JenaGroupMergeStepData) sdi;

        Object[] inputRowData = getRow(); // try and get a row
        if (inputRowData == null) {

            // output the last group
            if (data.getGroupMergedRow() != null) {
                outputGroup(data);
            }

            // no more rows...
            setOutputDone();
            return false;  // signal that we are DONE
        }

        // process a row...
        final RowMetaInterface inputRowMeta = getInputRowMeta();

        // get the group fields from the input row
        final LinkedHashMap<String, Object> inputRowGroupFields = getGroupFields(meta, inputRowData, inputRowMeta, this::logBasic);

        // check for the merge fields
        checkForMergeFields(meta, inputRowData, inputRowMeta, this::logBasic);

        // is this the first row this step has seen?
        if (first) {
            first = false;

            // create output row meta data
            createOutputRowMeta(inputRowMeta, meta, data);

            // if we are removing fields, we need to map fields from input row to output row
            // NOTE: this must come after createOutputRowMeta
            prepareForReMap(inputRowMeta, data);

            // process the first row
            processFirstRowForGroup(meta, data, inputRowData);

            // continue onto the next row
            return true;
        }

        final RowMetaInterface outputRowMeta = data.getOutputRowMeta();

        // get the group fields from the groupMergedRow
        final Object[] groupMergedRow = data.getGroupMergedRow();
        final LinkedHashMap<String, Object> groupMergedRowGroupFields = getGroupFields(meta, groupMergedRow, outputRowMeta, this::logBasic);

        // does the input row continue an existing group, or should it start a new group?
        if (isContinuation(groupMergedRowGroupFields, inputRowGroupFields)) {

            // the input row is a continuation of the groupMergedRow

            // merge the models from the input row with the models from the groupMergedRow
            mergeRowIntoGroup(meta, data, inputRowData);

        } else {

            // the input row is the first row within a new group

            // was there a previous group?
            if (groupMergedRow != null) {
                /*
                    yes, there was a previous group,
                    so we must output it and clear the previous data
                 */
                outputGroup(data);
            }

            processFirstRowForGroup(meta, data, inputRowData);
        }

        // report progress
        if (checkFeedback(getLinesRead())) {
            if (log.isBasic()) {
                logBasic(BaseMessages.getString(PKG, "JenaGroupMergeStep.Log.LineNumber") + getLinesRead());
            }
        }

        // continue onto the next row
        return true;
    }

    /**
     * Create the Meta for the Output Row.
     *
     * @param inputRowMeta the input row meta.
     * @param meta this steps meta.
     * @param data this steps data.
     */
    private void createOutputRowMeta(final RowMetaInterface inputRowMeta, final JenaGroupMergeStepMeta meta,
            final JenaGroupMergeStepData data) throws KettleStepException {

        final RowMetaInterface outputRowMeta = inputRowMeta.clone();
        meta.getFields(outputRowMeta, getStepname(), null, null, this, repository, metaStore);
        data.setOutputRowMeta(outputRowMeta);
    }

    /**
     * Stores the indexes of any fields from the input row
     * that need to be copied into the output row in the data object.
     *
     * The remapping itself is performed in {@link #processRow(JenaGroupMergeStepMeta, JenaGroupMergeStepData, Object[], Object[], BiFunction, QuadFunction, QuintFunction)}.
     *
     * @param inputRowMeta the input row meta
     * @param data this steps data.
     */
    private void prepareForReMap(final RowMetaInterface inputRowMeta, final JenaGroupMergeStepData data) {
        final LinkedHashMap<String, Integer> remainingInputFieldIndexes = new LinkedHashMap<>();
        final String[] outputRowFieldNames = data.getOutputRowMeta().getFieldNames();
        for (final String outputRowFieldName : outputRowFieldNames) {
            final int remainingInputFieldIndex = inputRowMeta.indexOfValue(outputRowFieldName);
            if (remainingInputFieldIndex > -1) {
                remainingInputFieldIndexes.put(outputRowFieldName, remainingInputFieldIndex);
            }
        }
        data.setRemainingInputFieldIndexes(remainingInputFieldIndexes);
    }

    /**
     * Process an input row as the first row for a group.
     *
     * @param meta this steps meta.
     * @param data this steps data.
     * @param inputRowData the input row.
     */
    static void processFirstRowForGroup(final JenaGroupMergeStepMeta meta, final JenaGroupMergeStepData data, final Object[] inputRowData) {
        // allocate a new array to hold the new Group Merged Row
        final RowMetaInterface outputRowMeta = data.getOutputRowMeta();
        Object[] outputRowData = RowDataUtil.allocateRowData(outputRowMeta.size());

        // function - always create a new model for the target field as this is the first row
        final BiFunction<Object[], Integer, Model> fnGetOutputRowTargetFieldModel = (outputRowData1, outputRowTargetFieldIndex) -> ModelFactory.createDefaultModel();

        // function - get the value of a normal field from the input row for the output row
        final QuadFunction<OtherFieldAction, Object, Object[], Integer, Object> fnGetNormalFieldOutputValue = (otherFieldAction, inputRowFieldValue, outputRowData1, outputRowFieldIndex) -> {
            if (otherFieldAction == OtherFieldAction.SET_NULL) {
                return null;
            } else {
                return inputRowFieldValue;
            }
        };

        // function - get the value of a group or merge (non-target) field from the input row for the output row
        final QuintFunction<ModelMergeConstrainedField, OtherFieldAction, Object, Object[], Integer, Object> fnGetGroupOrMergeFieldOutputValue = (mergeField, otherFieldAction, inputRowFieldValue, outputRowData1, outputRowFieldIndex) -> inputRowFieldValue;

        // process the first row of a new group
        outputRowData = processRow(meta, data, inputRowData, outputRowData, fnGetOutputRowTargetFieldModel, fnGetNormalFieldOutputValue, fnGetGroupOrMergeFieldOutputValue);
        data.setGroupMergedRow(outputRowData);
    }

    /**
     * Process an input row as 1+n row in a group.
     *
     * @param meta this steps meta.
     * @param data this steps data.
     * @param inputRowData the input row.
     */
    static void mergeRowIntoGroup(final JenaGroupMergeStepMeta meta, final JenaGroupMergeStepData data, final Object[] inputRowData) {
        // get the array holding the current Group Merged Row
        final Object[] outputRowData = data.getGroupMergedRow();

        // function - get the model of the current targetField in the Group Merged Row
        final BiFunction<Object[], Integer, Model> fnGetOutputRowTargetFieldModel = (outputRowData1, outputRowTargetFieldIndex) -> (Model) outputRowData1[outputRowTargetFieldIndex];

        // function - get the value of a normal field from the input row for the output row
        final QuadFunction<OtherFieldAction, Object, Object[], Integer, Object> fnGetNormalFieldOutputValue = (otherFieldAction, inputRowFieldValue, outputRowData1, outputRowFieldIndex) -> {
            if (otherFieldAction == OtherFieldAction.USE_LAST) {
                return inputRowFieldValue;
            } else if (otherFieldAction == OtherFieldAction.SET_NULL) {
                return null;
            } else if (otherFieldAction == OtherFieldAction.NULL_IF_DIFFERENT &&
                    outputRowData1[outputRowFieldIndex] != null &&
                    !outputRowData1[outputRowFieldIndex].equals(inputRowFieldValue)) {
                return null;
            } else {
                // OtherFieldAction.USE_FIRST
                return outputRowData1[outputRowFieldIndex];
            }
        };

        // function - get the value of a group or merge (non-target) field from the input row for the output row
        final QuintFunction<ModelMergeConstrainedField, OtherFieldAction, Object, Object[], Integer, Object> fnGetGroupOrMergeFieldOutputValue = (mergeField, otherFieldAction, inputRowFieldValue, outputRowData1, outputRowFieldIndex) -> {
            if (mergeField != null && mergeField.mutateFirstModel == MutateFirstModel.YES) {
                final Model inputRowFieldModel = (Model) inputRowFieldValue;
                final Model outputRowFieldModel = (Model) outputRowData1[outputRowFieldIndex];

                // merged the input row model into the model in the output row
                outputRowFieldModel.add(inputRowFieldModel);

                // close the original input row model if the user set that option in the dialog
                if (meta.isCloseMergedModels()) {
                    inputRowFieldModel.close();
                }

                return outputRowFieldModel;

            } else {
                return inputRowFieldValue;
            }
        };

        processRow(meta, data, inputRowData, outputRowData, fnGetOutputRowTargetFieldModel, fnGetNormalFieldOutputValue, fnGetGroupOrMergeFieldOutputValue);
    }

    /**
     * Process an input row.
     *
     * @param meta this steps meta.
     * @param data this steps data.
     * @param inputRowData the input row.
     * @param outputRowData the output row. This may be either a new row if this is the first row in a group,
     *                      or the grouped merged row if this is row 1+n in a group)
     * @param fnGetOutputRowTargetFieldModel get the model for a targetField in the output row.
     * @param fnGetNormalFieldOutputValue get the value of a normal field (i.e. not group or merge field from the input row).
     * @param fnGetGroupOrMergeFieldOutputValue get the value of a group or merge (non-target) field.
     *
     * @return the updated outputRowData.
     */
    private static Object[] processRow(final JenaGroupMergeStepMeta meta, final JenaGroupMergeStepData data,
            final Object[] inputRowData, final Object[] outputRowData,
            final BiFunction<Object[], Integer, Model> fnGetOutputRowTargetFieldModel,
            final QuadFunction<OtherFieldAction, Object, Object[], Integer, Object> fnGetNormalFieldOutputValue,
            final QuintFunction<ModelMergeConstrainedField, OtherFieldAction, Object, Object[], Integer, Object> fnGetGroupOrMergeFieldOutputValue) {

        final RowMetaInterface outputRowMeta = data.getOutputRowMeta();

        // this accumulates any targetFields that we have already set, so we don't override them if they already exist after the merge field in the input row
        Set<String> skipTargetFields = null;

        // iterate over the required output row fields (NOTE: the OutputRowMeta contains all columns, i.e. input columns to preserve and new targetField columns to create)
        for (final ValueMetaInterface outputRowMetaValue : outputRowMeta.getValueMetaList()) {
            final String outputRowFieldName = outputRowMetaValue.getName();

            // is there a corresponding field from the output row in the input row?
            @Nullable final Integer inputRowFieldIndex = data.getRemainingInputFieldIndexes().get(outputRowFieldName);

            // if there is no corresponding field in the input row, or we have already created the targetField, we can skip setting the output field
            boolean skipSetOutputField = inputRowFieldIndex == null || (skipTargetFields != null && skipTargetFields.contains(outputRowFieldName));

            // is the output field a "merge field" which sets a targetField?
            @Nullable final ModelMergeConstrainedField mergeField = meta.getMergeField(outputRowFieldName);
            if (mergeField != null && mergeField.mutateFirstModel == MutateFirstModel.NO) {

                // does the targetField already exist in the input row
                @Nullable final Integer inputRowTargetFieldIndex = data.getRemainingInputFieldIndexes().get(mergeField.targetFieldName);
                if (inputRowTargetFieldIndex != null) {
                    final Object existingInputRowValue = inputRowData[inputRowTargetFieldIndex];
                    if (existingInputRowValue != null && existingInputRowValue instanceof Model && meta.isCloseMergedModels()) {
                        // targetField exists in the input row, and already contains a Jena Model... this is strange.. but let's close the existing Jena Model to avoid a memory leak
                        ((Model) existingInputRowValue).close();
                    }
                }

                // get the index in the output row for the targetField
                final int outputRowTargetFieldIndex = outputRowMeta.indexOfValue(mergeField.targetFieldName);

                // find the input model in the input row
                final Model inputRowFieldModel = (Model) inputRowData[inputRowFieldIndex];

                // merge the input row model into the targetField model in the output row
                final Model outputRowTargetFieldModel = fnGetOutputRowTargetFieldModel.apply(outputRowData, outputRowTargetFieldIndex);
                outputRowTargetFieldModel.add(inputRowFieldModel);

                // close the original input row model if the user set that option in the dialog
                if (meta.isCloseMergedModels()) {
                    inputRowFieldModel.close();
                }

                // place the model into the targetField of the output row
                outputRowData[outputRowTargetFieldIndex] = outputRowTargetFieldModel;

                // remember that we have set the targetField
                if (skipTargetFields == null) {
                    skipTargetFields = new HashSet<>();
                }
                skipTargetFields.add(mergeField.targetFieldName);
            }


            // if we should not skip setting the output field
            if (!skipSetOutputField) {

                // find the corresponding field in the input row
                final Object inputRowFieldValue = inputRowData[inputRowFieldIndex];

                // is the output field a "group field"
                @Nullable final ConstrainedField groupField = meta.getGroupField(outputRowFieldName);

                // copy the input row field to the output row
                final int outputRowFieldIndex = outputRowMeta.indexOfValue(outputRowFieldName);
                if (mergeField == null && groupField == null) {
                    outputRowData[outputRowFieldIndex] = fnGetNormalFieldOutputValue.apply(meta.getOtherFieldAction(), inputRowFieldValue, outputRowData, outputRowFieldIndex);
                } else {
                    outputRowData[outputRowFieldIndex] = fnGetGroupOrMergeFieldOutputValue.apply(mergeField, meta.getOtherFieldAction(), inputRowFieldValue, outputRowData, outputRowFieldIndex);
                }
            }
        }

        return outputRowData;
    }

    /**
     * Send a merged group to the step output.
     *
     * @param data this steps data.
     */
    private void outputGroup(final JenaGroupMergeStepData data) throws KettleStepException {
        putRow(data.getOutputRowMeta(), data.getGroupMergedRow());
        data.clear();
    }

    /**
     * Determine if the input row is a continuation of a current group merge.
     *
     * @param groupMergedRowGroupFields the fields in the current group merge.
     * @param inputRowGroupFields the fields in the input row.
     *
     * @return true if this is a continuation, false otherwise.
     */
    private boolean isContinuation(@Nullable final LinkedHashMap<String, Object> groupMergedRowGroupFields, final LinkedHashMap<String, Object> inputRowGroupFields) {
        if (groupMergedRowGroupFields == null || groupMergedRowGroupFields.size() != inputRowGroupFields.size()) {
            return false;
        }

        for (final Map.Entry<String, Object> currentGroupField : inputRowGroupFields.entrySet()) {
            final String currentFieldName = currentGroupField.getKey();
            final Object currentFieldValue = currentGroupField.getValue();

            if (!groupMergedRowGroupFields.containsKey(currentFieldName)) {
                return false;
            }

            final Object previousFieldValue = groupMergedRowGroupFields.get(currentFieldName);
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

    /**
     * Get the user-specified fields that are used for grouping from the row.
     *
     * @param meta this steps meta.
     * @param rowData the row data.
     * @param rowMeta the row meta.
     * @param logFunction a function that can be called to log a warning message.
     *
     * @return the group fields, the Map key is the field name and the Map value is the field value.
     *
     * @throws KettleException if a required user-specified group field is missing.
     */
    static LinkedHashMap<String, Object> getGroupFields(final JenaGroupMergeStepMeta meta, final Object[] rowData,
            final RowMetaInterface rowMeta, final BiConsumer<String, String[]> logFunction) throws KettleException {

        // NOTE: order is important, so we use a LinkedHashMap
        final LinkedHashMap<String, Object> groupFields = new LinkedHashMap<>(meta.getGroupFields().size());

        for (int i = 0; i < meta.getGroupFields().size(); i++) {
            final ConstrainedField groupField = meta.getGroupFields().get(i);
            if (isNullOrEmpty(groupField.fieldName)) {
                throw new KettleException("Group field: " + i + " is missing its field name");
            }

            final int idxGroupField = rowMeta.indexOfValue(groupField.fieldName);
            if (idxGroupField == -1) {
                handleNoSuchField("Group", groupField, logFunction);
            } else {
                final Object groupFieldValue = rowData[idxGroupField];
                groupFields.put(groupField.fieldName, groupFieldValue);
                if (groupFieldValue == null) {
                    handleNullField("Group", groupField, logFunction);
                }
            }
        }

        return groupFields;
    }

    /**
     * Check that the row contains all of the user-specified merge fields.
     *
     * @param meta this steps meta.
     * @param rowData the row data.
     * @param rowMeta the row meta.
     * @param logFunction a function that can be called to log a warning message.
     *
     * @throws KettleException if a required user-specified merge field is missing.
     */
    static void checkForMergeFields(final JenaGroupMergeStepMeta meta, final Object[] rowData,
            final RowMetaInterface rowMeta, final BiConsumer<String, String[]> logFunction) throws KettleException {

        for (int i = 0; i < meta.getMergeFields().size(); i++) {
            final ConstrainedField mergeField = meta.getMergeFields().get(i);
            if (isNullOrEmpty(mergeField.fieldName)) {
                throw new KettleException("Merge field: " + i + " is missing its field name");
            }

            final int idxMergeField = rowMeta.indexOfValue(mergeField.fieldName);
            if (idxMergeField == -1) {
                handleNoSuchField("Merge", mergeField, logFunction);
            } else {
                final Object mergeFieldValue = rowData[idxMergeField];
                if (mergeFieldValue == null) {
                    handleNullField("Merge", mergeField, logFunction);
                }
            }
        }
    }

    private static void handleNoSuchField(final String fieldType, final ConstrainedField field,
            final BiConsumer<String, String[]> logFunction) throws KettleException {

        switch (field.actionIfNoSuchField) {
            case IGNORE:
                // no-op - just ignore it!
                break;

            case WARN:
                // log a warning
                logFunction.accept("{0} field: {1}, column is absent in row!", new String[] { fieldType, field.fieldName });
                break;

            case ERROR:
                // throw an exception
                throw new KettleException(fieldType + " field: " + field.fieldName + ", column is absent in row!");
        }
    }

    private static void handleNullField(final String fieldType, final ConstrainedField field,
            final BiConsumer<String, String[]> logFunction) throws KettleException {

        switch (field.actionIfNull) {
            case IGNORE:
                // no-op - just ignore it!
                break;

            case WARN:
                // log a warning
                logFunction.accept("{0} field: {1}, column has a null value in row!", new String[] { fieldType, field.fieldName });
                break;

            case ERROR:
                // throw an exception
                throw new KettleException(fieldType + " field: " + field.fieldName + ", column has a null value in row!");
        }
    }
}
