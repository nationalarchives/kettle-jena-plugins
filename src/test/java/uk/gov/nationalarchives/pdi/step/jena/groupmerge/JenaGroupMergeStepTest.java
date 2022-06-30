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

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNoSuchField;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNull;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.pentaho.di.core.row.ValueMetaInterface.*;
import static org.pentaho.di.core.util.Assert.*;
import static uk.gov.nationalarchives.pdi.step.jena.groupmerge.TestDSL.*;

public class JenaGroupMergeStepTest {

    @BeforeAll
    public static void setup() throws KettleException {
        KettleEnvironment.init(false);
    }

    @ParameterizedTest(name = "{index} processFirstRow_oneModel(MutateFirstModel.{0}, targetFieldName={1}, targetFieldExists={2}, closeMergedModels={3}, OtherFieldAction.{4})")
    @CsvSource({
            // MutateFirstModel,TargetFieldName,TargetFieldExists,CloseMergedModels,OtherFieldAction
            "YES,,false,true,DROP",
            "YES,,false,true,USE_FIRST",
            "YES,,false,true,USE_LAST",
            "YES,,false,true,NULL_IF_DIFFERENT",
            "YES,,false,true,SET_NULL",

            "YES,,false,false,DROP",
            "YES,,false,false,USE_FIRST",
            "YES,,false,false,USE_LAST",
            "YES,,false,false,NULL_IF_DIFFERENT",
            "YES,,false,false,SET_NULL",

            "NO,new_model,false,true,DROP",
            "NO,new_model,false,true,USE_FIRST",
            "NO,new_model,false,true,USE_LAST",
            "NO,new_model,false,true,NULL_IF_DIFFERENT",
            "NO,new_model,false,true,SET_NULL",

            "NO,new_model,false,false,DROP",
            "NO,new_model,false,false,USE_FIRST",
            "NO,new_model,false,false,USE_LAST",
            "NO,new_model,false,false,NULL_IF_DIFFERENT",
            "NO,new_model,false,false,SET_NULL",

            "NO,new_model,true,true,DROP",
            "NO,new_model,true,true,USE_FIRST",
            "NO,new_model,true,true,USE_LAST",
            "NO,new_model,true,true,NULL_IF_DIFFERENT",
            "NO,new_model,true,true,SET_NULL",

            "NO,new_model,true,false,DROP",
            "NO,new_model,true,false,USE_FIRST",
            "NO,new_model,true,false,USE_LAST",
            "NO,new_model,true,false,NULL_IF_DIFFERENT",
            "NO,new_model,true,false,SET_NULL"
    })
    public void processFirstRow_oneModel(final MutateFirstModel mutateFirstModel, @Nullable final String targetFieldName, final boolean targetFieldExists, final boolean closeMergedModels, final OtherFieldAction otherFieldAction) throws KettlePluginException {
        // setup an example input row
        final int inputRowId = 1;
        final String inputRowSubject = "cats";
        final Model inputRowModel = ModelWithSubject(inputRowId, inputRowSubject);
        Row inputRow = Row(
                Field("id", TYPE_INTEGER, inputRowId),
                Field("model", TYPE_SERIALIZABLE, inputRowModel),
                Field("subject", TYPE_STRING, inputRowSubject)
        );
        if (targetFieldExists) {
          inputRow = inputRow.addField(Field(targetFieldName, TYPE_SERIALIZABLE, null));
        }

        // setup the configuration for the step
        final JenaGroupMergeStepMeta meta = JenaGroupMergeStepMeta(
                GroupFields(GroupField("id", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR)),
                MergeFields(MergeField("model", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR, mutateFirstModel, targetFieldName)),
                closeMergedModels,
                otherFieldAction
        );

        // setup output row metadata, and store in the data
        Row inputRowCopy = inputRow.copy();

        final List<RemainingInputField> remainingInputFields = List(
                RemainingInputField("id", 0),
                RemainingInputField("model", 1)
        );
        if (otherFieldAction != OtherFieldAction.DROP) {
            remainingInputFields.add(RemainingInputField("subject", 2));
        } else {
            inputRowCopy = inputRowCopy.removeField("subject");
        }
        if (targetFieldExists) {
            remainingInputFields.add(RemainingInputField(targetFieldName, 3));
        }
        if (mutateFirstModel == MutateFirstModel.NO && !targetFieldExists) {
            inputRowCopy = inputRowCopy.addField(Field(targetFieldName, TYPE_SERIALIZABLE, null));
        }
        final RowMetaInterface outputRowMeta = inputRowCopy.getMeta();

        final JenaGroupMergeStepData data = JenaGroupMergeStepData(
                outputRowMeta,
                remainingInputFields.toArray(new RemainingInputField[0])
        );

        // execute
        JenaGroupMergeStep.processFirstRowForGroup(meta, data, inputRow.values());

        // make assertions about the groupMergedRow in the data
        final Object[] groupMergedRow = data.getGroupMergedRow();

        int expectedLength = 2 + RowDataUtil.OVER_ALLOCATE_SIZE;  // the 'id' and 'model' columns, plus the default over allocation size
        if (otherFieldAction != OtherFieldAction.DROP) {
            expectedLength++;  // also include the 'subject' column
        }
        if (mutateFirstModel == MutateFirstModel.NO) {
            expectedLength++;  // also include the 'targetField' column
        }
        assertEquals(expectedLength, groupMergedRow.length);

        int outputRowIdx = 0;

        assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("id"));
        assertEquals(inputRowId, groupMergedRow[outputRowMeta.indexOfValue("id")]);
        assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("model"));
        final Model groupMergedRowModel = (Model) groupMergedRow[outputRowMeta.indexOfValue("model")];
        if (mutateFirstModel == MutateFirstModel.YES) {
            assertFalse(groupMergedRowModel.isClosed());
        } else {
            assertEquals(closeMergedModels, groupMergedRowModel.isClosed());
        }
        assertEquals(inputRowModel, groupMergedRowModel);

        if (otherFieldAction != OtherFieldAction.DROP) {
            assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("subject"));

            if (otherFieldAction == OtherFieldAction.SET_NULL) {
                assertNull(groupMergedRow[outputRowMeta.indexOfValue("subject")]);
            } else {
                assertEquals(inputRowSubject, groupMergedRow[outputRowMeta.indexOfValue("subject")]);
            }
        }

        if (mutateFirstModel == MutateFirstModel.NO) {
            assertEquals(outputRowIdx++, outputRowMeta.indexOfValue(targetFieldName));
            final Model groupMergedRowNewModel = (Model) groupMergedRow[outputRowMeta.indexOfValue(targetFieldName)];
            assertFalse(groupMergedRowNewModel.isClosed());
            assertNotNull(groupMergedRowNewModel);
        }
    }

    @ParameterizedTest(name = "{index} processFirstRow_twoModels(MutateFirstModel1.{0}, targetFieldName1={1}, targetFieldExists1={2}, MutateFirstModel2.{3}, targetFieldName2={4}, targetFieldExists2={5}, closeMergedModels={6}, OtherFieldAction.{7})")
    @CsvSource({
            // MutateFirstModel1,TargetFieldName1,TargetFieldExists2,MutateFirstModel2,TargetFieldName2,TargetFieldExists1,CloseMergedModels,OtherFieldAction
            "YES,,false,YES,,false,true,DROP",
            "YES,,false,YES,,false,true,USE_FIRST",
            "YES,,false,YES,,false,true,USE_LAST",
            "YES,,false,YES,,false,true,NULL_IF_DIFFERENT",
            "YES,,false,YES,,false,true,SET_NULL",

            "YES,,false,YES,,false,false,DROP",
            "YES,,false,YES,,false,false,USE_FIRST",
            "YES,,false,YES,,false,false,USE_LAST",
            "YES,,false,YES,,false,false,NULL_IF_DIFFERENT",
            "YES,,false,YES,,false,false,SET_NULL",

            "NO,new_model1,false,NO,new_model2,false,true,DROP",
            "NO,new_model1,false,NO,new_model2,false,true,USE_FIRST",
            "NO,new_model1,false,NO,new_model2,false,true,USE_LAST",
            "NO,new_model1,false,NO,new_model2,false,true,NULL_IF_DIFFERENT",
            "NO,new_model1,false,NO,new_model2,false,true,SET_NULL",

            "NO,new_model1,false,NO,new_model2,false,false,DROP",
            "NO,new_model1,false,NO,new_model2,false,false,USE_FIRST",
            "NO,new_model1,false,NO,new_model2,false,false,USE_LAST",
            "NO,new_model1,false,NO,new_model2,false,false,NULL_IF_DIFFERENT",
            "NO,new_model1,false,NO,new_model2,false,false,SET_NULL",

            "NO,new_model1,true,NO,new_model2,true,true,DROP",
            "NO,new_model1,true,NO,new_model2,true,true,USE_FIRST",
            "NO,new_model1,true,NO,new_model2,true,true,USE_LAST",
            "NO,new_model1,true,NO,new_model2,true,true,NULL_IF_DIFFERENT",
            "NO,new_model1,true,NO,new_model2,true,true,SET_NULL",

            "NO,new_model1,true,NO,new_model2,false,true,DROP",
            "NO,new_model1,true,NO,new_model2,false,true,USE_FIRST",
            "NO,new_model1,true,NO,new_model2,false,true,USE_LAST",
            "NO,new_model1,true,NO,new_model2,false,true,NULL_IF_DIFFERENT",
            "NO,new_model1,true,NO,new_model2,false,true,SET_NULL",

            "NO,new_model1,false,NO,new_model2,true,true,DROP",
            "NO,new_model1,false,NO,new_model2,true,true,USE_FIRST",
            "NO,new_model1,false,NO,new_model2,true,true,USE_LAST",
            "NO,new_model1,false,NO,new_model2,true,true,NULL_IF_DIFFERENT",
            "NO,new_model1,false,NO,new_model2,true,true,SET_NULL",

            "NO,new_model1,true,NO,new_model2,true,false,DROP",
            "NO,new_model1,true,NO,new_model2,true,false,USE_FIRST",
            "NO,new_model1,true,NO,new_model2,true,false,USE_LAST",
            "NO,new_model1,true,NO,new_model2,true,false,NULL_IF_DIFFERENT",
            "NO,new_model1,true,NO,new_model2,true,false,SET_NULL",

            "NO,new_model1,true,NO,new_model2,false,false,DROP",
            "NO,new_model1,true,NO,new_model2,false,false,USE_FIRST",
            "NO,new_model1,true,NO,new_model2,false,false,USE_LAST",
            "NO,new_model1,true,NO,new_model2,false,false,NULL_IF_DIFFERENT",
            "NO,new_model1,true,NO,new_model2,false,false,SET_NULL",

            "NO,new_model1,false,NO,new_model2,true,false,DROP",
            "NO,new_model1,false,NO,new_model2,true,false,USE_FIRST",
            "NO,new_model1,false,NO,new_model2,true,false,USE_LAST",
            "NO,new_model1,false,NO,new_model2,true,false,NULL_IF_DIFFERENT",
            "NO,new_model1,false,NO,new_model2,true,false,SET_NULL",

            "YES,,false,NO,new_model,false,true,DROP",
            "YES,,false,NO,new_model,false,true,USE_FIRST",
            "YES,,false,NO,new_model,false,true,USE_LAST",
            "YES,,false,NO,new_model,false,true,NULL_IF_DIFFERENT",
            "YES,,false,NO,new_model,false,true,SET_NULL",

            "NO,new_model,false,YES,,false,true,DROP",
            "NO,new_model,false,YES,,false,true,USE_FIRST",
            "NO,new_model,false,YES,,false,true,USE_LAST",
            "NO,new_model,false,YES,,false,true,NULL_IF_DIFFERENT",
            "NO,new_model,false,YES,,false,true,SET_NULL",

            "YES,,false,NO,new_model,true,true,DROP",
            "YES,,false,NO,new_model,true,true,USE_FIRST",
            "YES,,false,NO,new_model,true,true,USE_LAST",
            "YES,,false,NO,new_model,true,true,NULL_IF_DIFFERENT",
            "YES,,false,NO,new_model,true,true,SET_NULL",

            "NO,new_model,true,YES,,false,true,DROP",
            "NO,new_model,true,YES,,false,true,USE_FIRST",
            "NO,new_model,true,YES,,false,true,USE_LAST",
            "NO,new_model,true,YES,,false,true,NULL_IF_DIFFERENT",
            "NO,new_model,true,YES,,false,true,SET_NULL",

            "YES,,false,NO,new_model,false,false,DROP",
            "YES,,false,NO,new_model,false,false,USE_FIRST",
            "YES,,false,NO,new_model,false,false,USE_LAST",
            "YES,,false,NO,new_model,false,false,NULL_IF_DIFFERENT",
            "YES,,false,NO,new_model,false,false,SET_NULL",

            "NO,new_model,false,YES,,false,false,DROP",
            "NO,new_model,false,YES,,false,false,USE_FIRST",
            "NO,new_model,false,YES,,false,false,USE_LAST",
            "NO,new_model,false,YES,,false,false,NULL_IF_DIFFERENT",
            "NO,new_model,false,YES,,false,false,SET_NULL",

            "YES,,false,NO,new_model,true,false,DROP",
            "YES,,false,NO,new_model,true,false,USE_FIRST",
            "YES,,false,NO,new_model,true,false,USE_LAST",
            "YES,,false,NO,new_model,true,false,NULL_IF_DIFFERENT",
            "YES,,false,NO,new_model,true,false,SET_NULL",

            "NO,new_model,true,YES,,false,false,DROP",
            "NO,new_model,true,YES,,false,false,USE_FIRST",
            "NO,new_model,true,YES,,false,false,USE_LAST",
            "NO,new_model,true,YES,,false,false,NULL_IF_DIFFERENT",
            "NO,new_model,true,YES,,false,false,SET_NULL"
    })
    public void processFirstRow_twoModels(final MutateFirstModel mutateFirstModel1, @Nullable final String targetFieldName1, final boolean targetFieldExists1, final MutateFirstModel mutateFirstModel2, @Nullable final String targetFieldName2, final boolean targetFieldExists2, final boolean closeMergedModels, final OtherFieldAction otherFieldAction) throws KettlePluginException {
        // setup an example input row
        final int inputRowId = 1;
        final String inputRowSubject = "cats";
        final Model inputRowModel1 = ModelWithSubject(inputRowId, inputRowSubject + "1");
        final Model inputRowModel2 = ModelWithSubject(inputRowId, inputRowSubject + "2");
        Row inputRow = Row(
                Field("id", TYPE_INTEGER, inputRowId),
                Field("model1", TYPE_SERIALIZABLE, inputRowModel1),
                Field("model2", TYPE_SERIALIZABLE, inputRowModel2),
                Field("subject", TYPE_STRING, inputRowSubject)
        );
        if (targetFieldExists1) {
            inputRow = inputRow.addField(Field(targetFieldName1, TYPE_SERIALIZABLE, null));
        }
        if (targetFieldExists2) {
            inputRow = inputRow.addField(Field(targetFieldName2, TYPE_SERIALIZABLE, null));
        }

        // setup the configuration for the step
        final JenaGroupMergeStepMeta meta = JenaGroupMergeStepMeta(
                GroupFields(GroupField("id", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR)),
                MergeFields(
                        MergeField("model1", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR, mutateFirstModel1, targetFieldName1),
                        MergeField("model2", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR, mutateFirstModel2, targetFieldName2)
                ),
                closeMergedModels,
                otherFieldAction
        );

        // setup output row metadata, and store in the data
        Row inputRowCopy = inputRow.copy();

        int remainingInputFieldIdx = 0;

        final List<RemainingInputField> remainingInputFields = List(
                RemainingInputField("id", remainingInputFieldIdx++),
                RemainingInputField("model1", remainingInputFieldIdx++),
                RemainingInputField("model2", remainingInputFieldIdx++)
        );
        if (otherFieldAction != OtherFieldAction.DROP) {
            remainingInputFields.add(RemainingInputField("subject", remainingInputFieldIdx++));
        } else {
            inputRowCopy = inputRowCopy.removeField("subject");
        }
        if (targetFieldExists1) {
            remainingInputFields.add(RemainingInputField(targetFieldName1, remainingInputFieldIdx++));
        }
        if (targetFieldExists2) {
            remainingInputFields.add(RemainingInputField(targetFieldName2, remainingInputFieldIdx++));
        }
        if (mutateFirstModel1 == MutateFirstModel.NO && !targetFieldExists1) {
            inputRowCopy = inputRowCopy.addField(Field(targetFieldName1, TYPE_SERIALIZABLE, null));
        }
        if (mutateFirstModel2 == MutateFirstModel.NO && !targetFieldExists2) {
            inputRowCopy = inputRowCopy.addField(Field(targetFieldName2, TYPE_SERIALIZABLE, null));
        }
        final RowMetaInterface outputRowMeta = inputRowCopy.getMeta();

        final JenaGroupMergeStepData data = JenaGroupMergeStepData(
                outputRowMeta,
                remainingInputFields.toArray(new RemainingInputField[0])
        );

        // execute
        JenaGroupMergeStep.processFirstRowForGroup(meta, data, inputRow.values());

        // make assertions about the groupMergedRow in the data
        final Object[] groupMergedRow = data.getGroupMergedRow();

        int expectedLength = 3 + RowDataUtil.OVER_ALLOCATE_SIZE;  // the 'id', 'model1', and 'model2' columns, plus the default over allocation size
        if (otherFieldAction != OtherFieldAction.DROP) {
            expectedLength++;  // also include the 'subject' column
        }
        if (mutateFirstModel1 == MutateFirstModel.NO) {
            expectedLength++;  // also include the 'targetField1' column
        }
        if (mutateFirstModel2 == MutateFirstModel.NO) {
            expectedLength++;  // also include the 'targetField2' column
        }
        assertEquals(expectedLength, groupMergedRow.length);

        int outputRowIdx = 0;

        assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("id"));
        assertEquals(inputRowId, groupMergedRow[outputRowMeta.indexOfValue("id")]);
        assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("model1"));
        final Model groupMergedRowModel1 = (Model) groupMergedRow[outputRowMeta.indexOfValue("model1")];
        if (mutateFirstModel1 == MutateFirstModel.YES) {
            assertFalse(groupMergedRowModel1.isClosed());
        } else {
            assertEquals(closeMergedModels, groupMergedRowModel1.isClosed());
        }
        assertEquals(inputRowModel1, groupMergedRowModel1);
        assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("model2"));
        final Model groupMergedRowModel2 = (Model) groupMergedRow[outputRowMeta.indexOfValue("model2")];
        if (mutateFirstModel2 == MutateFirstModel.YES) {
            assertFalse(groupMergedRowModel2.isClosed());
        } else {
            assertEquals(closeMergedModels, groupMergedRowModel2.isClosed());
        }
        assertEquals(inputRowModel2, groupMergedRowModel2);

        if (otherFieldAction != OtherFieldAction.DROP) {
            assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("subject"));

            if (otherFieldAction == OtherFieldAction.SET_NULL) {
                assertNull(groupMergedRow[outputRowMeta.indexOfValue("subject")]);
            } else {
                assertEquals(inputRowSubject, groupMergedRow[outputRowMeta.indexOfValue("subject")]);
            }
        }

        if (targetFieldExists1 == false && targetFieldExists2 == true) {
            if (mutateFirstModel2 == MutateFirstModel.NO) {
                assertEquals(outputRowIdx++, outputRowMeta.indexOfValue(targetFieldName2));
                final Model groupMergedRowNewModel2 = (Model) groupMergedRow[outputRowMeta.indexOfValue(targetFieldName2)];
                assertFalse(groupMergedRowNewModel2.isClosed());
                assertNotNull(groupMergedRowNewModel2);
            }

            if (mutateFirstModel1 == MutateFirstModel.NO) {
                assertEquals(outputRowIdx++, outputRowMeta.indexOfValue(targetFieldName1));
                final Model groupMergedRowNewModel1 = (Model) groupMergedRow[outputRowMeta.indexOfValue(targetFieldName1)];
                assertFalse(groupMergedRowNewModel1.isClosed());
                assertNotNull(groupMergedRowNewModel1);
            }
        } else {
            if (mutateFirstModel1 == MutateFirstModel.NO) {
                assertEquals(outputRowIdx++, outputRowMeta.indexOfValue(targetFieldName1));
                final Model groupMergedRowNewModel1 = (Model) groupMergedRow[outputRowMeta.indexOfValue(targetFieldName1)];
                assertFalse(groupMergedRowNewModel1.isClosed());
                assertNotNull(groupMergedRowNewModel1);
            }

            if (mutateFirstModel2 == MutateFirstModel.NO) {
                assertEquals(outputRowIdx++, outputRowMeta.indexOfValue(targetFieldName2));
                final Model groupMergedRowNewModel2 = (Model) groupMergedRow[outputRowMeta.indexOfValue(targetFieldName2)];
                assertFalse(groupMergedRowNewModel2.isClosed());
                assertNotNull(groupMergedRowNewModel2);
            }
        }
    }

    @ParameterizedTest(name = "{index} getGroupFields(fieldExists={0}, ActionIfNoSuchField.{1})")
    @CsvSource({
            // FieldExists,getGroupFields_ActionIfNoSuchField
            "true,IGNORE",
            "true,WARN",
            "true,ERROR",
            "false,IGNORE",
            "false,WARN",
            "false,ERROR",
    })
    public void getGroupFields_ActionIfNoSuchField(final boolean fieldExists, final ActionIfNoSuchField actionIfNoSuchField) throws KettleException {
        // setup an example input row
        final int inputRowId = 1;
        final String inputRowSubject = "cats";
        final Model inputRowModel = ModelWithSubject(inputRowId, inputRowSubject);
        final Row inputRow;
        if (fieldExists) {
            inputRow = Row(
                    Field("id", TYPE_INTEGER, inputRowId),
                    Field("model", TYPE_SERIALIZABLE, inputRowModel),
                    Field("subject", TYPE_STRING, inputRowSubject)
            );
        } else {
            inputRow = Row(
                    Field("model", TYPE_SERIALIZABLE, inputRowModel),
                    Field("subject", TYPE_STRING, inputRowSubject)
            );
        }

        // setup the configuration for the step
        final JenaGroupMergeStepMeta meta = JenaGroupMergeStepMeta(
                GroupFields(GroupField("id", actionIfNoSuchField, ActionIfNull.ERROR)),
                MergeFields(MergeField("model", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR, MutateFirstModel.YES, null)),
                true,
                OtherFieldAction.DROP
        );

        // capturing log function
        final CapturingLogFunction logFunction = new CapturingLogFunction();

        // execute and assert
        if (fieldExists == false && actionIfNoSuchField == ActionIfNoSuchField.ERROR) {
            assertThrows(KettleException.class, () -> {
                JenaGroupMergeStep.getGroupFields(meta, inputRow.values(), inputRow.getMeta(), logFunction);
            });

        } else {
            final LinkedHashMap<String, Object> inputRowGroupFields = JenaGroupMergeStep.getGroupFields(meta, inputRow.values(), inputRow.getMeta(), logFunction);
            if (fieldExists == false) {
                if (actionIfNoSuchField == ActionIfNoSuchField.WARN) {
                    assertNotNull(logFunction.message);
                }
                assertTrue(inputRowGroupFields.isEmpty());

            } else {
                assertFalse(inputRowGroupFields.isEmpty());
                final Object groupField = inputRowGroupFields.get("id");
                assertNotNull(groupField);
                assertEquals(inputRow.fields[0].value, groupField);
            }
        }
    }

    @ParameterizedTest(name = "{index} getGroupFields(fieldIsNull={0}, ActionIfNull.{1})")
    @CsvSource({
            // FieldIsNull,ActionIfNull
            "true,IGNORE",
            "true,WARN",
            "true,ERROR",
            "false,IGNORE",
            "false,WARN",
            "false,ERROR",
    })
    public void getGroupFields_ActionIfNull(final boolean fieldIsNull, final ActionIfNull actionIfNull) throws KettleException {
        // setup an example input row
        final int inputRowId = 1;
        final String inputRowSubject = "cats";
        final Model inputRowModel = ModelWithSubject(inputRowId, inputRowSubject);
        final Row inputRow = Row(
                    Field("id", TYPE_INTEGER, fieldIsNull ? null : inputRowId),
                    Field("model", TYPE_SERIALIZABLE, inputRowModel),
                    Field("subject", TYPE_STRING, inputRowSubject)
            );

        // setup the configuration for the step
        final JenaGroupMergeStepMeta meta = JenaGroupMergeStepMeta(
                GroupFields(GroupField("id", ActionIfNoSuchField.ERROR, actionIfNull)),
                MergeFields(MergeField("model", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR, MutateFirstModel.YES, null)),
                true,
                OtherFieldAction.DROP
        );

        // capturing log function
        final CapturingLogFunction logFunction = new CapturingLogFunction();

        // execute and assert
        if (fieldIsNull && actionIfNull == ActionIfNull.ERROR) {
            assertThrows(KettleException.class, () -> {
                JenaGroupMergeStep.getGroupFields(meta, inputRow.values(), inputRow.getMeta(), logFunction);
            });

        } else {
            final LinkedHashMap<String, Object> inputRowGroupFields = JenaGroupMergeStep.getGroupFields(meta, inputRow.values(), inputRow.getMeta(), logFunction);
            if (fieldIsNull) {
                if (actionIfNull == ActionIfNull.WARN) {
                    assertNotNull(logFunction.message);
                }
            }
            assertFalse(inputRowGroupFields.isEmpty());
            final Object groupField = inputRowGroupFields.get("id");
            if (fieldIsNull) {
                assertNull(groupField);
            } else {
                assertNotNull(groupField);
                assertEquals(inputRow.fields[0].value, groupField);
            }
        }
    }

    @ParameterizedTest(name = "{index} checkForMergeFields(fieldExists={0}, ActionIfNoSuchField.{1})")
    @CsvSource({
            // FieldExists,ActionIfNoSuchField
            "true,IGNORE",
            "true,WARN",
            "true,ERROR",
            "false,IGNORE",
            "false,WARN",
            "false,ERROR",
    })
    public void checkForMergeFields_ActionIfNoSuchField(final boolean fieldExists, final ActionIfNoSuchField actionIfNoSuchField) throws KettleException {
        // setup an example input row
        final int inputRowId = 1;
        final String inputRowSubject = "cats";
        final Model inputRowModel = ModelWithSubject(inputRowId, inputRowSubject);
        final Row inputRow;
        if (fieldExists) {
            inputRow = Row(
                    Field("id", TYPE_INTEGER, inputRowId),
                    Field("model", TYPE_SERIALIZABLE, inputRowModel),
                    Field("subject", TYPE_STRING, inputRowSubject)
            );
        } else {
            inputRow = Row(
                    Field("id", TYPE_INTEGER, inputRowId),
                    Field("subject", TYPE_STRING, inputRowSubject)
            );
        }

        // setup the configuration for the step
        final JenaGroupMergeStepMeta meta = JenaGroupMergeStepMeta(
                GroupFields(GroupField("id", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR)),
                MergeFields(MergeField("model", actionIfNoSuchField, ActionIfNull.ERROR, MutateFirstModel.YES, null)),
                true,
                OtherFieldAction.DROP
        );

        // capturing log function
        final CapturingLogFunction logFunction = new CapturingLogFunction();

        // execute and assert
        if (fieldExists == false && actionIfNoSuchField == ActionIfNoSuchField.ERROR) {
            assertThrows(KettleException.class, () -> {
                JenaGroupMergeStep.checkForMergeFields(meta, inputRow.values(), inputRow.getMeta(), logFunction);
            });

        } else {
            JenaGroupMergeStep.checkForMergeFields(meta, inputRow.values(), inputRow.getMeta(), logFunction);

            if (fieldExists == false && actionIfNoSuchField == ActionIfNoSuchField.WARN) {
                assertNotNull(logFunction.message);
            }
        }
    }

    @ParameterizedTest(name = "{index} checkForMergeFields(fieldIsNull={0}, ActionIfNull.{1})")
    @CsvSource({
            // FieldIsNull,ActionIfNull
            "true,IGNORE",
            "true,WARN",
            "true,ERROR",
            "false,IGNORE",
            "false,WARN",
            "false,ERROR",
    })
    public void checkForMergeFields_ActionIfNull(final boolean fieldIsNull, final ActionIfNull actionIfNull) throws KettleException {
        // setup an example input row
        final int inputRowId = 1;
        final String inputRowSubject = "cats";
        final Model inputRowModel = ModelWithSubject(inputRowId, inputRowSubject);
        final Row inputRow = Row(
                Field("id", TYPE_INTEGER, inputRowId),
                Field("model", TYPE_SERIALIZABLE, fieldIsNull ? null : inputRowModel),
                Field("subject", TYPE_STRING, inputRowSubject)
        );

        // setup the configuration for the step
        final JenaGroupMergeStepMeta meta = JenaGroupMergeStepMeta(
                GroupFields(GroupField("id", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR)),
                MergeFields(MergeField("model", ActionIfNoSuchField.ERROR, actionIfNull, MutateFirstModel.YES, null)),
                true,
                OtherFieldAction.DROP
        );

        // capturing log function
        final CapturingLogFunction logFunction = new CapturingLogFunction();

        // execute and assert
        if (fieldIsNull && actionIfNull == ActionIfNull.ERROR) {
            assertThrows(KettleException.class, () -> {
                JenaGroupMergeStep.checkForMergeFields(meta, inputRow.values(), inputRow.getMeta(), logFunction);
            });

        } else {
            JenaGroupMergeStep.checkForMergeFields(meta, inputRow.values(), inputRow.getMeta(), logFunction);

            if (fieldIsNull && actionIfNull == ActionIfNull.WARN) {
                assertNotNull(logFunction.message);
            }
        }
    }

    @ParameterizedTest(name = "{index} mergeRowIntoGroup_oneModel(MutateFirstModel.{0}, targetFieldName={1}, targetFieldExists={2}, closeMergedModels={3}, OtherFieldAction.{4})")
    @CsvSource({
            // MutateFirstModel,TargetFieldName,TargetFieldExists,CloseMergedModels,OtherFieldAction
            "YES,,false,true,DROP",
            "YES,,false,true,USE_FIRST",
            "YES,,false,true,USE_LAST",
            "YES,,false,true,NULL_IF_DIFFERENT",
            "YES,,false,true,SET_NULL",

            "YES,,false,false,DROP",
            "YES,,false,false,USE_FIRST",
            "YES,,false,false,USE_LAST",
            "YES,,false,false,NULL_IF_DIFFERENT",
            "YES,,false,false,SET_NULL",

            "NO,new_model,false,true,DROP",
            "NO,new_model,false,true,USE_FIRST",
            "NO,new_model,false,true,USE_LAST",
            "NO,new_model,false,true,NULL_IF_DIFFERENT",
            "NO,new_model,false,true,SET_NULL",

            "NO,new_model,false,false,DROP",
            "NO,new_model,false,false,USE_FIRST",
            "NO,new_model,false,false,USE_LAST",
            "NO,new_model,false,false,NULL_IF_DIFFERENT",
            "NO,new_model,false,false,SET_NULL",

            "NO,new_model,true,true,DROP",
            "NO,new_model,true,true,USE_FIRST",
            "NO,new_model,true,true,USE_LAST",
            "NO,new_model,true,true,NULL_IF_DIFFERENT",
            "NO,new_model,true,true,SET_NULL",

            "NO,new_model,true,false,DROP",
            "NO,new_model,true,false,USE_FIRST",
            "NO,new_model,true,false,USE_LAST",
            "NO,new_model,true,false,NULL_IF_DIFFERENT",
            "NO,new_model,true,false,SET_NULL"
    })
    public void mergeRowIntoGroup_oneModel(final MutateFirstModel mutateFirstModel, @Nullable final String targetFieldName, final boolean targetFieldExists, final boolean closeMergedModels, final OtherFieldAction otherFieldAction) throws KettlePluginException {

        // setup one row which was already merged in groupMergedRow
        final int groupMergedRow1Id = 1;
        final String groupMergedRow1Subject = "cats";
        final Model groupMergedRow1Model = ModelWithSubject(groupMergedRow1Id, groupMergedRow1Subject);

        Row groupMergedRow1 = Row(
                Field("id", TYPE_INTEGER, groupMergedRow1Id),
                Field("model", TYPE_SERIALIZABLE, groupMergedRow1Model)
        );
        if (otherFieldAction != OtherFieldAction.DROP) {
            groupMergedRow1 = groupMergedRow1.addField(Field("subject", TYPE_STRING, groupMergedRow1Subject));
        }
        @Nullable final Model groupMergedRow1TargetModel;
        if (mutateFirstModel == MutateFirstModel.NO) {
            groupMergedRow1TargetModel = ModelWithSubject(groupMergedRow1Id, groupMergedRow1Subject);
            groupMergedRow1 = groupMergedRow1.addField(Field(targetFieldName, TYPE_SERIALIZABLE, groupMergedRow1TargetModel));
        } else {
            groupMergedRow1TargetModel = null;
        }

        // setup an example input row
        final int inputRowId = 1;
        final String inputRowSubject = "dogs";
        final Model inputRowModel = ModelWithSubject(inputRowId, inputRowSubject);
        Row inputRow = Row(
                Field("id", TYPE_INTEGER, inputRowId),
                Field("model", TYPE_SERIALIZABLE, inputRowModel),
                Field("subject", TYPE_STRING, inputRowSubject)
        );
        if (targetFieldExists) {
            inputRow = inputRow.addField(Field(targetFieldName, TYPE_SERIALIZABLE, null));
        }

        final Model expectedMergedModel = MergedModels(groupMergedRow1Model, inputRowModel);
        if (mutateFirstModel == MutateFirstModel.NO && closeMergedModels) {
            groupMergedRow1Model.close();
        }

        // setup the configuration for the step
        final JenaGroupMergeStepMeta meta = JenaGroupMergeStepMeta(
                GroupFields(GroupField("id", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR)),
                MergeFields(MergeField("model", ActionIfNoSuchField.ERROR, ActionIfNull.ERROR, mutateFirstModel, targetFieldName)),
                closeMergedModels,
                otherFieldAction
        );

        // setup output row metadata, and store in the data
        Row inputRowCopy = inputRow.copy();

        final List<RemainingInputField> remainingInputFields = List(
                RemainingInputField("id", 0),
                RemainingInputField("model", 1)
        );
        if (otherFieldAction != OtherFieldAction.DROP) {
            remainingInputFields.add(RemainingInputField("subject", 2));
        } else {
            inputRowCopy = inputRowCopy.removeField("subject");
        }
        if (targetFieldExists) {
            remainingInputFields.add(RemainingInputField(targetFieldName, 3));
        }
        if (mutateFirstModel == MutateFirstModel.NO && !targetFieldExists) {
            inputRowCopy = inputRowCopy.addField(Field(targetFieldName, TYPE_SERIALIZABLE, null));
        }
        final RowMetaInterface outputRowMeta = inputRowCopy.getMeta();

        final JenaGroupMergeStepData data = JenaGroupMergeStepData(
                groupMergedRow1.values(true),
                outputRowMeta,
                remainingInputFields.toArray(new RemainingInputField[0])
        );

        // execute
        JenaGroupMergeStep.mergeRowIntoGroup(meta, data, inputRow.values());

        // make assertions about the groupMergedRow in the data
        final Object[] groupMergedRow = data.getGroupMergedRow();

        int expectedLength = 2 + RowDataUtil.OVER_ALLOCATE_SIZE;  // the 'id' and 'model' columns, plus the default over allocation size
        if (otherFieldAction != OtherFieldAction.DROP) {
            expectedLength++;  // also include the 'subject' column
        }
        if (mutateFirstModel == MutateFirstModel.NO) {
            expectedLength++;  // also include the 'targetField' column
        }
        assertEquals(expectedLength, groupMergedRow.length);

        int outputRowIdx = 0;

        assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("id"));
        assertEquals(inputRowId, groupMergedRow[outputRowMeta.indexOfValue("id")]);
        assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("model"));
        final Model groupMergedRowModel = (Model) groupMergedRow[outputRowMeta.indexOfValue("model")];
        if (mutateFirstModel == MutateFirstModel.YES) {
            assertFalse(groupMergedRowModel.isClosed());
        } else {
            assertEquals(closeMergedModels, groupMergedRowModel.isClosed());
        }
        final Model actualModel;
        if (mutateFirstModel == MutateFirstModel.YES) {
            actualModel = groupMergedRowModel;
        } else {
            actualModel = (Model) groupMergedRow[outputRowMeta.indexOfValue(targetFieldName)];
        }
        assertTrue(actualModel.isIsomorphicWith(expectedMergedModel));
        if (otherFieldAction != OtherFieldAction.DROP) {
            assertEquals(outputRowIdx++, outputRowMeta.indexOfValue("subject"));

            if (otherFieldAction == OtherFieldAction.USE_FIRST) {
                assertEquals(groupMergedRow1Subject, groupMergedRow[outputRowMeta.indexOfValue("subject")]);
            } else if (otherFieldAction == OtherFieldAction.USE_LAST) {
                assertEquals(inputRowSubject, groupMergedRow[outputRowMeta.indexOfValue("subject")]);
            } else if (otherFieldAction == OtherFieldAction.SET_NULL) {
                assertNull(groupMergedRow[outputRowMeta.indexOfValue("subject")]);
            } else {
                // OtherFieldAction.SET_NULL_IF_DIFFERENT
                assertNull(groupMergedRow[outputRowMeta.indexOfValue("subject")]);
            }
        }

        if (mutateFirstModel == MutateFirstModel.NO) {
            assertEquals(outputRowIdx++, outputRowMeta.indexOfValue(targetFieldName));
            final Model groupMergedRowNewModel = (Model) groupMergedRow[outputRowMeta.indexOfValue(targetFieldName)];
            assertFalse(groupMergedRowNewModel.isClosed());
            assertNotNull(groupMergedRowNewModel);
        }
    }

    private static class CapturingLogFunction implements BiConsumer<String, String[]> {
        String[] params = null;
        String message = null;

        @Override
        public void accept(final String message, final String[] params) {
            this.message = message;
            this.params = params;
        }
    }
}
