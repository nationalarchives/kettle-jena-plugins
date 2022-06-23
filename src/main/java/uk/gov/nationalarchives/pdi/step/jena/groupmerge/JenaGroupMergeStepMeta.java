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

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.*;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNoSuchField;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNull;
import uk.gov.nationalarchives.pdi.step.jena.ConstrainedField;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.nationalarchives.pdi.step.jena.Util.isNotEmpty;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;


/**
 * Jena Group Merge Step meta.
 *
 * Deals with describing the step, and saving and loading the step configuration data from XML.
 */
@Step(id = "JenaGroupMergeStep", image = "JenaGroupMergeStep.svg", name = "Group and Merge Jena Models",
        description = "Groups Apache Jena Models from different rows and merges them", categoryDescription = "Transform")
public class JenaGroupMergeStepMeta extends BaseStepMeta implements StepMetaInterface {

    private static Class<?> PKG = JenaGroupMergeStep.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    // <editor-fold desc="settings XML element names">
    private static final String ELEM_NAME_MUTATE_FIRST_MODEL = "mutateFirstModel";
    private static final String ELEM_NAME_TARGET_FIELD_NAME = "targetFieldName";

    /**
     * Replaced by {@link #ELEM_NAME_CLOSE_MERGED_MODELS}.
     */
    @Deprecated
    private static final String ELEM_NAME_REMOVE_SELECTED_FIELDS = "removeSelectedFields";

    private static final String ELEM_NAME_CLOSE_MERGED_MODELS = "closeMergedModels";
    private static final String ELEM_NAME_JENA_MODEL_FIELDS = "jenaModelFields";
    private static final String ELEM_NAME_JENA_MODEL_FIELD = "jenaModelField";
    private static final String ELEM_NAME_GROUP_FIELDS = "groupFields";
    private static final String ELEM_NAME_GROUP_FIELD = "groupField";
    private static final String ELEM_NAME_FIELD_NAME = "fieldName";
    private static final String ELEM_NAME_ACTION_IF_NO_SUCH_FIELD = "actionIfNoSuchField";
    private static final String ELEM_NAME_ACTION_IF_NULL = "actionIfNull";

    private static final String ELEM_NAME_OTHER_FIELD_ACTION = "otherFieldAction";
    // </editor-fold>

    // <editor-fold desc="settings">
    private boolean closeMergedModels;
    private List<ConstrainedField> groupFields;
    private List<ModelMergeConstrainedField> jenaModelFields;
    private OtherFieldAction otherFieldAction;
    // </editor-fold>

    public JenaGroupMergeStepMeta() {
        super(); // allocate BaseStepMeta
    }

    @Override
    public void setDefault() {
        closeMergedModels = false;
        groupFields = new ArrayList<>();
        jenaModelFields = new ArrayList<>();
        otherFieldAction = OtherFieldAction.DROP;
    }

    @Override
    public Object clone() {
        final JenaGroupMergeStepMeta retval = (JenaGroupMergeStepMeta) super.clone();
        retval.closeMergedModels = closeMergedModels;
        retval.groupFields = new ArrayList<>(groupFields);
        retval.jenaModelFields = new ArrayList<>();
        for (final ModelMergeConstrainedField jenaModelField : jenaModelFields) {
            retval.jenaModelFields.add(jenaModelField.copy());
        }
        retval.otherFieldAction = otherFieldAction;
        return retval;
    }

    @Override
    public String getXML() throws KettleException {
        final StringBuilder builder = new StringBuilder();
        builder.append(XMLHandler.addTagValue(ELEM_NAME_CLOSE_MERGED_MODELS, closeMergedModels));

        builder.append(XMLHandler.openTag(ELEM_NAME_GROUP_FIELDS));
        for (final ConstrainedField groupField : groupFields) {
            builder
                    .append(XMLHandler.openTag(ELEM_NAME_GROUP_FIELD))
                    .append(XMLHandler.addTagValue(ELEM_NAME_FIELD_NAME, groupField.fieldName))
                    .append(XMLHandler.addTagValue(ELEM_NAME_ACTION_IF_NO_SUCH_FIELD, groupField.actionIfNoSuchField.name()))
                    .append(XMLHandler.addTagValue(ELEM_NAME_ACTION_IF_NULL, groupField.actionIfNull.name()))
                    .append(XMLHandler.closeTag(ELEM_NAME_GROUP_FIELD));
        }
        builder.append(XMLHandler.closeTag(ELEM_NAME_GROUP_FIELDS));

        builder.append(XMLHandler.openTag(ELEM_NAME_JENA_MODEL_FIELDS));
        for (final ModelMergeConstrainedField jenaModelField : jenaModelFields) {
            builder
                    .append(XMLHandler.openTag(ELEM_NAME_JENA_MODEL_FIELD))
                    .append(XMLHandler.addTagValue(ELEM_NAME_FIELD_NAME, jenaModelField.fieldName))
                    .append(XMLHandler.addTagValue(ELEM_NAME_ACTION_IF_NO_SUCH_FIELD, jenaModelField.actionIfNoSuchField.name()))
                    .append(XMLHandler.addTagValue(ELEM_NAME_ACTION_IF_NULL, jenaModelField.actionIfNull.name()))
                    .append(XMLHandler.addTagValue(ELEM_NAME_MUTATE_FIRST_MODEL, jenaModelField.mutateFirstModel.name()))
                    .append(XMLHandler.addTagValue(ELEM_NAME_TARGET_FIELD_NAME, jenaModelField.targetFieldName))
                    .append(XMLHandler.closeTag(ELEM_NAME_JENA_MODEL_FIELD));
        }
        builder.append(XMLHandler.closeTag(ELEM_NAME_JENA_MODEL_FIELDS));
        builder.append(XMLHandler.addTagValue(ELEM_NAME_OTHER_FIELD_ACTION, otherFieldAction.name()));

        return builder.toString();
    }

    @Override
    public void loadXML(final Node stepnode, final List<DatabaseMeta> databases, final IMetaStore metaStore) throws KettleXMLException {
        final String xCloseMergedModels = XMLHandler.getTagValue(stepnode, ELEM_NAME_CLOSE_MERGED_MODELS);
        if (isNotEmpty(xCloseMergedModels)) {
            this.closeMergedModels = isNotEmpty(xCloseMergedModels) ? xCloseMergedModels.equals("Y") : false;
        } else {
            // load legacy value (if present)
            final String xRemoveSelectedField = XMLHandler.getTagValue(stepnode, ELEM_NAME_REMOVE_SELECTED_FIELDS);
            this.closeMergedModels = isNotEmpty(xRemoveSelectedField) ? xRemoveSelectedField.equals("Y") : false;
        }

        final Node groupFieldsNode = XMLHandler.getSubNode(stepnode, ELEM_NAME_GROUP_FIELDS);
        if (groupFieldsNode == null) {
            this.groupFields = new ArrayList<>();
        } else {
            final List<Node> groupFieldsNodes = XMLHandler.getNodes(groupFieldsNode, ELEM_NAME_GROUP_FIELD);
            if (isNullOrEmpty(groupFieldsNodes)) {
                this.groupFields = new ArrayList<>();
            } else {
                this.groupFields = new ArrayList<>();

                final int len = groupFieldsNodes.size();
                for (int i = 0; i < len; i++) {
                    final Node groupFieldNode = groupFieldsNodes.get(i);

                    final String xFieldName = XMLHandler.getTagValue(groupFieldNode, ELEM_NAME_FIELD_NAME);
                    if (isNullOrEmpty(xFieldName)) {
                        continue;
                    }

                    final String xActionIfNoSuchField = XMLHandler.getTagValue(groupFieldNode, ELEM_NAME_ACTION_IF_NO_SUCH_FIELD);
                    final ActionIfNoSuchField actionIfNoSuchField = isNotEmpty(xActionIfNoSuchField) ? ActionIfNoSuchField.valueOf(xActionIfNoSuchField) : ActionIfNoSuchField.ERROR;
                    final String xActionIfNull = XMLHandler.getTagValue(groupFieldNode, ELEM_NAME_ACTION_IF_NULL);
                    final ActionIfNull actionIfNull = isNotEmpty(xActionIfNull) ? ActionIfNull.valueOf(xActionIfNull) : ActionIfNull.ERROR;
                    this.groupFields.add(new ConstrainedField(xFieldName, actionIfNoSuchField, actionIfNull));
                }
            }
        }

        final Node jenaModelFieldsNode = XMLHandler.getSubNode(stepnode, ELEM_NAME_JENA_MODEL_FIELDS);
        if (jenaModelFieldsNode == null) {
            this.jenaModelFields = new ArrayList<>();
        } else {
            final List<Node> jenaModelFieldNodes = XMLHandler.getNodes(jenaModelFieldsNode, ELEM_NAME_JENA_MODEL_FIELD);
            if (isNullOrEmpty(jenaModelFieldNodes)) {
                this.jenaModelFields = new ArrayList<>();
            } else {
                this.jenaModelFields = new ArrayList<>();

                final int len = jenaModelFieldNodes.size();
                for (int i = 0; i < len; i++) {
                    final Node jenaModelFieldNode = jenaModelFieldNodes.get(i);

                    final String xFieldName = XMLHandler.getTagValue(jenaModelFieldNode, ELEM_NAME_FIELD_NAME);
                    if (isNullOrEmpty(xFieldName)) {
                        continue;
                    }

                    final String xActionIfNoSuchField = XMLHandler.getTagValue(jenaModelFieldNode, ELEM_NAME_ACTION_IF_NO_SUCH_FIELD);
                    final ActionIfNoSuchField actionIfNoSuchField = isNotEmpty(xActionIfNoSuchField) ? ActionIfNoSuchField.valueOf(xActionIfNoSuchField) : ActionIfNoSuchField.ERROR;
                    final String xActionIfNull = XMLHandler.getTagValue(jenaModelFieldNode, ELEM_NAME_ACTION_IF_NULL);
                    final ActionIfNull actionIfNull = isNotEmpty(xActionIfNull) ? ActionIfNull.valueOf(xActionIfNull) : ActionIfNull.ERROR;
                    final String xMutateFirstModel = XMLHandler.getTagValue(jenaModelFieldNode, ELEM_NAME_MUTATE_FIRST_MODEL);
                    final MutateFirstModel mutateFirstModel = isNotEmpty(xMutateFirstModel) ? MutateFirstModel.valueOf(xMutateFirstModel) : MutateFirstModel.YES;
                    final String xTargetFieldName = XMLHandler.getTagValue(jenaModelFieldNode, ELEM_NAME_TARGET_FIELD_NAME);
                    final String targetFieldName = isNotEmpty(xTargetFieldName) ? xTargetFieldName : null;

                    this.jenaModelFields.add(new ModelMergeConstrainedField(xFieldName, actionIfNoSuchField, actionIfNull, mutateFirstModel, targetFieldName));
                }
            }
        }

        final String xOtherFieldAction = XMLHandler.getTagValue(stepnode, ELEM_NAME_OTHER_FIELD_ACTION);
        if (isNotEmpty(xOtherFieldAction)) {
            this.otherFieldAction = OtherFieldAction.valueOf(xOtherFieldAction);
        }
    }

    @Override
    public void saveRep(final Repository repo, final IMetaStore metaStore, final ObjectId id_transformation, final ObjectId id_step)
            throws KettleException {

        final String rep = getXML();
        repo.saveStepAttribute(id_transformation, id_step, "step-xml", rep);
    }

    @Override
    public void readRep(final Repository repo, final IMetaStore metaStore, final ObjectId id_step, final List<DatabaseMeta> databases) throws KettleException {
        final String rep = repo.getStepAttributeString(id_step, "step-xml");
        if (isNullOrEmpty(rep)) {
            setDefault();
        }

        final Node stepnode = XMLHandler.loadXMLString(rep);
        loadXML(stepnode, (List<DatabaseMeta>)null, (IMetaStore)null);
    }

    @Override
    public void getFields(final RowMetaInterface rowMeta, final String origin, final RowMetaInterface[] info, final StepMeta nextStep,
                          final VariableSpace space, final Repository repository, final IMetaStore metaStore) throws KettleStepException {

        //TODO(AR) we also need the database fields here?

        try {
            // add any target fields to the output row
            for (final ModelMergeConstrainedField jenaModelField : jenaModelFields) {
                if (jenaModelField.mutateFirstModel == MutateFirstModel.NO && isNotEmpty(jenaModelField.targetFieldName)) {
                    final ValueMetaInterface targetFieldValueMeta = ValueMetaFactory.createValueMeta(space.environmentSubstitute(jenaModelField.targetFieldName), ValueMeta.TYPE_SERIALIZABLE);
                    targetFieldValueMeta.setOrigin(origin);
                    rowMeta.addValueMeta(targetFieldValueMeta);
                }
            }

        } catch (final KettlePluginException e) {
            throw new KettleStepException(e);
        }

        if (closeMergedModels) {

            // if we are mutating the first model, we must not remove it from the output row
            Boolean skipFirst = null;

            for (final ModelMergeConstrainedField jenaModelField : jenaModelFields) {
                if (skipFirst == null) {
                    skipFirst = jenaModelField.mutateFirstModel == MutateFirstModel.YES;
                }

                if (!skipFirst) {
                    try {
                        rowMeta.removeValueMeta(jenaModelField.fieldName);
                    } catch (final KettleValueException e) {
                        //TODO(AR) log error or throw?
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
                skipFirst = false;
            }
        }
    }

    @Override
    public void check(final List<CheckResultInterface> remarks, final TransMeta transMeta,
                      final StepMeta stepMeta, final RowMetaInterface prev, final String input[], final String output[],
                      final RowMetaInterface info, final VariableSpace space, final Repository repository,
                      final IMetaStore metaStore) {
        CheckResult cr;
        if (prev == null || prev.size() == 0) {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING, BaseMessages.getString(PKG, "JenaGroupMergeStepMeta.CheckResult.NotReceivingFields"), stepMeta);
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "JenaGroupMergeStepMeta.CheckResult.StepReceivingData", prev.size() + ""), stepMeta);
            remarks.add(cr);
        }

        // See if we have input streams leading to this step!
        if (input.length > 0) {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "JenaGroupMergeStepMeta.CheckResult.StepReceivingData2"), stepMeta);
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "JenaGroupMergeStepMeta.CheckResult.NoInputReceivedFromOtherSteps"), stepMeta);
            remarks.add(cr);
        }
    }

    @Override
    public StepInterface getStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr, final TransMeta transMeta, final Trans trans) {
        return new JenaGroupMergeStep(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new JenaGroupMergeStepData();
    }

    @Override
    public String getDialogClassName() {
        return "uk.gov.nationalarchives.pdi.step.jena.groupmerge.JenaGroupMergeStepDialog";
    }



    // <editor-fold desc="settings getters and setters">
    public boolean isCloseMergedModels() {
        return closeMergedModels;
    }

    public void setCloseMergedModels(final boolean closeMergedModels) {
        this.closeMergedModels = closeMergedModels;
    }

    public List<ConstrainedField> getGroupFields() {
        return groupFields;
    }

    public void setGroupFields(final List<ConstrainedField> groupFields) {
        this.groupFields = groupFields;
    }

    public List<ModelMergeConstrainedField> getJenaModelMergeFields() {
        return jenaModelFields;
    }

    public void setJenaModelFields(final List<ModelMergeConstrainedField> jenaModelFields) {
        this.jenaModelFields = jenaModelFields;
    }

    public OtherFieldAction getOtherFieldAction() {
        return otherFieldAction;
    }

    public void setOtherFieldAction(final OtherFieldAction otherFieldAction) {
        this.otherFieldAction = otherFieldAction;
    }

    // </editor-fold>
}
