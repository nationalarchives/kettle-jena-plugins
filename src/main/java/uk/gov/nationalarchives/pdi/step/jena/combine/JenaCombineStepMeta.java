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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.nationalarchives.pdi.step.jena.Util.*;


/**
 * Jena Combine Step meta.
 *
 * Deals with describing the step, and saving and loading the step configuration data from XML.
 */
@Step(id = "JenaCombineStep", image = "JenaCombineStep.svg", name = "Combine Jena Models",
        description = "Combines 2 or more Apache Jena Models within the same row", categoryDescription = "Transform")
public class JenaCombineStepMeta extends BaseStepMeta implements StepMetaInterface {

    private static Class<?> PKG = JenaCombineStep.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    // <editor-fold desc="settings XML element names">
    private static final String ELEM_NAME_MUTATE_FIRST_MODEL = "mutateFirstModel";
    private static final String ELEM_NAME_TARGET_FIELD_NAME = "targetFieldName";
    private static final String ELEM_NAME_REMOVE_SELECTED_FIELDS = "removeSelectedFields";
    private static final String ELEM_NAME_JENA_MODEL_FIELDS = "jenaModelFields";
    private static final String ELEM_NAME_JENA_MODEL_FIELD = "jenaModelField";
    private static final String ELEM_NAME_FIELD_NAME = "fieldName";
    private static final String ELEM_NAME_ACTION_IF_NO_SUCH_FIELD = "actionIfNoSuchField";
    private static final String ELEM_NAME_ACTION_IF_NULL = "actionIfNull";
    // </editor-fold>

    // <editor-fold desc="settings">
    private boolean mutateFirstModel;
    @Nullable private String targetFieldName;
    private boolean removeSelectedFields;
    private List<ConstrainedField> jenaModelFields;
    // </editor-fold>


    public JenaCombineStepMeta() {
        super(); // allocate BaseStepMeta
    }

    @Override
    public void setDefault() {
        mutateFirstModel = true;
        targetFieldName = "";
        removeSelectedFields = false;
        jenaModelFields = new ArrayList<>();
    }

    @Override
    public Object clone() {
        final JenaCombineStepMeta retval = (JenaCombineStepMeta) super.clone();
        retval.mutateFirstModel = mutateFirstModel;
        retval.targetFieldName = targetFieldName;
        retval.removeSelectedFields = removeSelectedFields;
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
            .append(XMLHandler.addTagValue(ELEM_NAME_TARGET_FIELD_NAME, emptyIfNull(targetFieldName)))
            .append(XMLHandler.addTagValue(ELEM_NAME_REMOVE_SELECTED_FIELDS, removeSelectedFields));

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
                        this.jenaModelFields.add(new ConstrainedField(xFieldName, actionIfNoSuchField, actionIfNull));
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

        if (!mutateFirstModel && isNullOrEmpty(targetFieldName)) {
            throw new KettleStepException("Mutate First Model is not selected, and the Target Field Name is empty. One or the other must be selected");
        }

        /**
         * 1. Remove any fields that we have mapped to RDF properties when `removeSelectedFields` is checked
         */
        if (removeSelectedFields && isNotEmpty(jenaModelFields)) {
            int startRemovalIdx = 0;
            if (mutateFirstModel) {
                // if we are mutating the first model, we must not remove it from the output row
                startRemovalIdx++;
            }

            for (int i = startRemovalIdx; i < jenaModelFields.size(); i++) {
                final ConstrainedField jenaModelField = jenaModelFields.get(i);
                if (isNotEmpty(jenaModelField.fieldName)) {
                    try {
                        rowMeta.removeValueMeta(jenaModelField.fieldName);
                    } catch (final KettleValueException e) {
                        throw new KettleStepException("Unable to remove field: " + jenaModelField.fieldName + ": " + e.getMessage(), e);
                    }
                }
            }
        }

        /**
         * 2. if we have a target field, add it to the output rows
         * NOTE: it is important this is added last, as such
         * behaviour is relied on in {@link JenaCombineStep#prepareForReMap(JenaCombineStepMeta, JenaCombineStepData)}.
         */
        if (!mutateFirstModel) {
            final String expandedTargetFieldName = space.environmentSubstitute(targetFieldName);
            final ValueMetaInterface targetFieldValueMeta;
            try {
                targetFieldValueMeta = ValueMetaFactory.createValueMeta(expandedTargetFieldName, ValueMeta.TYPE_SERIALIZABLE);
            } catch (final KettlePluginException e) {
                throw new KettleStepException("Unable to create Value Meta for target field: " + expandedTargetFieldName + (targetFieldName.equals(expandedTargetFieldName) ? "" : "(" + targetFieldName + ")") + ", : " + e.getMessage(), e);
            }
            targetFieldValueMeta.setOrigin(origin);
            rowMeta.addValueMeta(targetFieldValueMeta);
        }
    }

    @Override
    public void check(final List<CheckResultInterface> remarks, final TransMeta transMeta,
                      final StepMeta stepMeta, final RowMetaInterface prev, final String input[], final String output[],
                      final RowMetaInterface info, final VariableSpace space, final Repository repository,
                      final IMetaStore metaStore) {
        CheckResult cr;
        if (prev == null || prev.size() == 0) {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING, BaseMessages.getString(PKG, "JenaCombineStepMeta.CheckResult.NotReceivingFields"), stepMeta);
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "JenaCombineStepMeta.CheckResult.StepReceivingData", prev.size() + ""), stepMeta);
            remarks.add(cr);
        }

        // See if we have input streams leading to this step!
        if (input.length > 0) {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "JenaCombineStepMeta.CheckResult.StepReceivingData2"), stepMeta);
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "JenaCombineStepMeta.CheckResult.NoInputReceivedFromOtherSteps"), stepMeta);
            remarks.add(cr);
        }
    }

    @Override
    public StepInterface getStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr, final TransMeta transMeta, final Trans trans) {
        return new JenaCombineStep(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new JenaCombineStepData();
    }

    @Override
    public String getDialogClassName() {
        return "uk.gov.nationalarchives.pdi.step.jena.combine.JenaCombineStepDialog";
    }



    // <editor-fold desc="settings getters and setters">
    public boolean isMutateFirstModel() {
        return mutateFirstModel;
    }

    public void setMutateFirstModel(final boolean mutateFirstModel) {
        this.mutateFirstModel = mutateFirstModel;
    }

    public @Nullable String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(@Nullable final String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public boolean isRemoveSelectedFields() {
        return removeSelectedFields;
    }

    public void setRemoveSelectedFields(final boolean removeSelectedFields) {
        this.removeSelectedFields = removeSelectedFields;
    }

    public List<ConstrainedField> getJenaModelFields() {
        return jenaModelFields;
    }

    public void setJenaModelFields(final List<ConstrainedField> jenaModelFields) {
        this.jenaModelFields = jenaModelFields;
    }
    // </editor-fold>
}
