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
    private static final String ELEM_NAME_REMOVE_SELECTED_FIELDS = "removeSelectedFields";
    private static final String ELEM_NAME_JENA_MODEL_FIELDS = "jenaModelFields";
    private static final String ELEM_NAME_JENA_MODEL_FIELD = "jenaModelField";
    private static final String ELEM_NAME_GROUP_FIELDS = "groupFields";
    private static final String ELEM_NAME_GROUP_FIELD = "groupField";
    private static final String ELEM_NAME_FIELD_NAME = "fieldName";
    private static final String ELEM_NAME_ACTION_IF_NO_SUCH_FIELD = "actionIfNoSuchField";
    private static final String ELEM_NAME_ACTION_IF_NULL = "actionIfNull";
    // </editor-fold>

    // <editor-fold desc="settings">
    private boolean mutateFirstModel;
    private String targetFieldName;
    private boolean removeSelectedFields;
    private List<String> groupFields;
    private List<ConstrainedField> jenaModelFields;
    // </editor-fold>

    public JenaGroupMergeStepMeta() {
        super(); // allocate BaseStepMeta
    }

    @Override
    public void setDefault() {
        mutateFirstModel = true;
        targetFieldName = "";
        removeSelectedFields = false;
        groupFields = new ArrayList<>();
        jenaModelFields = new ArrayList<>();
    }

    @Override
    public Object clone() {
        final JenaGroupMergeStepMeta retval = (JenaGroupMergeStepMeta) super.clone();
        retval.mutateFirstModel = mutateFirstModel;
        retval.targetFieldName = targetFieldName;
        retval.removeSelectedFields = removeSelectedFields;
        retval.groupFields = new ArrayList<>(groupFields);
        retval.jenaModelFields = new ArrayList<>();
        for (final ConstrainedField jenaModelField : jenaModelFields) {
            retval.jenaModelFields.add(jenaModelField.copy());
        }
        return retval;
    }

    @Override
    public String getXML() throws KettleException {
        final StringBuilder builder = new StringBuilder();
        builder
            .append(XMLHandler.addTagValue(ELEM_NAME_MUTATE_FIRST_MODEL, mutateFirstModel))
            .append(XMLHandler.addTagValue(ELEM_NAME_TARGET_FIELD_NAME, targetFieldName))
            .append(XMLHandler.addTagValue(ELEM_NAME_REMOVE_SELECTED_FIELDS, removeSelectedFields));

        builder.append(XMLHandler.openTag(ELEM_NAME_GROUP_FIELDS));
        for (final String groupField : groupFields) {
            builder
                    .append(XMLHandler.openTag(ELEM_NAME_GROUP_FIELD))
                    .append(XMLHandler.addTagValue(ELEM_NAME_FIELD_NAME, groupField))
                    .append(XMLHandler.closeTag(ELEM_NAME_GROUP_FIELD));
        }
        builder.append(XMLHandler.closeTag(ELEM_NAME_GROUP_FIELDS));

        builder.append(XMLHandler.openTag(ELEM_NAME_JENA_MODEL_FIELDS));
        for (final ConstrainedField jenaModelField : jenaModelFields) {
            builder
                    .append(XMLHandler.openTag(ELEM_NAME_JENA_MODEL_FIELD))
                    .append(XMLHandler.addTagValue(ELEM_NAME_FIELD_NAME, jenaModelField.fieldName))
                    .append(XMLHandler.addTagValue(ELEM_NAME_ACTION_IF_NO_SUCH_FIELD, jenaModelField.actionIfNoSuchField.name()))
                    .append(XMLHandler.addTagValue(ELEM_NAME_ACTION_IF_NULL, jenaModelField.actionIfNull.name()))
                    .append(XMLHandler.closeTag(ELEM_NAME_JENA_MODEL_FIELD));
        }
        builder.append(XMLHandler.closeTag(ELEM_NAME_JENA_MODEL_FIELDS));

        return builder.toString();
    }

    @Override
    public void loadXML(final Node stepnode, final List<DatabaseMeta> databases, final IMetaStore metaStore) throws KettleXMLException {
        final String xMutateFirstModel = XMLHandler.getTagValue(stepnode, ELEM_NAME_MUTATE_FIRST_MODEL);
        if (xMutateFirstModel != null) {
            this.mutateFirstModel = xMutateFirstModel.isEmpty() ? true : xMutateFirstModel.equals("Y");

            final String xTargetFieldName = XMLHandler.getTagValue(stepnode, ELEM_NAME_TARGET_FIELD_NAME);
            this.targetFieldName = xTargetFieldName != null ? xTargetFieldName : "";

            final String xRemoveSelectedField = XMLHandler.getTagValue(stepnode, ELEM_NAME_MUTATE_FIRST_MODEL);
            this.removeSelectedFields = isNotEmpty(xRemoveSelectedField) ? xRemoveSelectedField.equals("Y") : false;

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

                        this.groupFields.add(xFieldName);
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
                        final ActionIfNoSuchField actionIfNoSuchFIeld = isNotEmpty(xActionIfNoSuchField) ? ActionIfNoSuchField.valueOf(xActionIfNoSuchField) : ActionIfNoSuchField.ERROR;
                        final String xActionIfNull = XMLHandler.getTagValue(jenaModelFieldNode, ELEM_NAME_ACTION_IF_NULL);
                        final ActionIfNull actionIfNull = isNotEmpty(xActionIfNull) ? ActionIfNull.valueOf(xActionIfNull) : ActionIfNull.ERROR;
                        this.jenaModelFields.add(new ConstrainedField(xFieldName, actionIfNoSuchFIeld, actionIfNull));
                    }
                }
            }
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
            // add the target field to the output row
            if (isNotEmpty(targetFieldName)) {
                final ValueMetaInterface targetFieldValueMeta = ValueMetaFactory.createValueMeta(space.environmentSubstitute(targetFieldName), ValueMeta.TYPE_SERIALIZABLE);
                targetFieldValueMeta.setOrigin(origin);
                rowMeta.addValueMeta(targetFieldValueMeta);
            }

        } catch (final KettlePluginException e) {
            throw new KettleStepException(e);
        }

        if (removeSelectedFields) {

            // if we are mutating the first model, we must not remove it from the output row
            boolean skipFirst = mutateFirstModel;

            for (final ConstrainedField jenaModelField : jenaModelFields) {
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
    public boolean isMutateFirstModel() {
        return mutateFirstModel;
    }

    public void setMutateFirstModel(final boolean mutateFirstModel) {
        this.mutateFirstModel = mutateFirstModel;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(final String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public boolean isRemoveSelectedFields() {
        return removeSelectedFields;
    }

    public void setRemoveSelectedFields(final boolean removeSelectedFields) {
        this.removeSelectedFields = removeSelectedFields;
    }

    public List<String> getGroupFields() {
        return groupFields;
    }

    public void setGroupFields(final List<String> groupFields) {
        this.groupFields = groupFields;
    }

    public List<ConstrainedField> getJenaModelMergeFields() {
        return jenaModelFields;
    }

    public void setJenaModelFields(final List<ConstrainedField> jenaModelFields) {
        this.jenaModelFields = jenaModelFields;
    }
    // </editor-fold>
}
