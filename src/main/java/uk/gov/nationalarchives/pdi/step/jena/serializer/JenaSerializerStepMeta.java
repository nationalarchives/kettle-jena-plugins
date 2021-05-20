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
package uk.gov.nationalarchives.pdi.step.jena.serializer;

import uk.gov.nationalarchives.pdi.step.jena.Rdf11;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.*;
import org.pentaho.di.core.row.RowMetaInterface;
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

import java.util.*;

import static uk.gov.nationalarchives.pdi.step.jena.Util.isNotEmpty;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;


/**
 * Jena Serializer Step meta.
 *
 * Deals with describing the step, and saving and loading the step configuration data from XML.
 */
@Step(id = "JenaSerializerStep", image = "JenaSerializerStep.svg", name = "Serialize Jena Model",
        description = "Serializes an Apache Jena Model", categoryDescription = "Output")
public class JenaSerializerStepMeta extends BaseStepMeta implements StepMetaInterface {

    private static Class<?> PKG = JenaSerializerStep.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    // <editor-fold desc="settings XML element names">
    private static final String ELEM_NAME_JENA_MODEL_FIELD = "jenaModelField";
    private static final String ATTR_NAME_CLOSE_AND_REMOVE = "closeAndRemove";
    private static final String ELEM_NAME_SERIALIZATION_FORMAT = "serializationFormat";
    private static final String ELEM_NAME_FILE = "file";
    private static final String ELEM_NAME_FILENAME = "filename";
    private static final String ELEM_NAME_CREATE_PARENT_FOLDER = "createParentFolder";
    private static final String ELEM_NAME_INCLUDE_STEP_NR = "includeStepNr";
    private static final String ELEM_NAME_INCLUDE_PARTITION_NR = "includePartitionNr";
    private static final String ELEM_NAME_INCLUDE_DATE = "includeDate";
    private static final String ELEM_NAME_INCLUDE_TIME = "includeTime";
    // </editor-fold>

    public static final String DEFAULT_FILENAME = "output.ttl";

    // <editor-fold desc="settings">
    private String jenaModelField;
    private boolean closeModelAndRemoveField;
    private String serializationFormat;
    static class FileDetail implements Cloneable {
        String filename;
        boolean createParentFolder;
        boolean includeStepNr;
        boolean includePartitionNr;
        boolean includeDate;
        boolean includeTime;

        @Override
        protected Object clone() {
            return copy();
        }

        public FileDetail copy() {
            final FileDetail copy = new FileDetail();
            copy.filename = filename;
            copy.createParentFolder = createParentFolder;
            copy.includeStepNr = includeStepNr;
            copy.includePartitionNr = includePartitionNr;
            copy.includeDate = includeDate;
            copy.includeTime = includeTime;
            return copy;
        }
    }
    private FileDetail fileDetail;
    // </editor-fold>


    public JenaSerializerStepMeta() {
        super(); // allocate BaseStepMeta
    }

    @Override
    public void setDefault() {
        jenaModelField = "";
        closeModelAndRemoveField = true;
        serializationFormat = Rdf11.DEFAULT_SERIALIZATION_FORMAT;
        fileDetail = newDefaultFileDetail();
    }

    private static FileDetail newDefaultFileDetail() {
        final FileDetail fileDetail = new FileDetail();
        fileDetail.filename = DEFAULT_FILENAME;
        fileDetail.createParentFolder = true;
        fileDetail.includeStepNr = false;
        fileDetail.includePartitionNr = false;
        fileDetail.includeDate = false;
        fileDetail.includeTime = false;
        return fileDetail;
    }

    @Override
    public Object clone() {
        final JenaSerializerStepMeta retval = (JenaSerializerStepMeta) super.clone();
        retval.jenaModelField = jenaModelField;
        retval.closeModelAndRemoveField = closeModelAndRemoveField;
        retval.serializationFormat = serializationFormat;
        retval.fileDetail = fileDetail == null ? null : fileDetail.copy();
        return retval;
    }

    @Override
    public String getXML() throws KettleException {
        final StringBuilder builder = new StringBuilder();
        builder
            .append(XMLHandler.addTagValue(ELEM_NAME_JENA_MODEL_FIELD, jenaModelField, true, ATTR_NAME_CLOSE_AND_REMOVE, Boolean.toString(closeModelAndRemoveField)))
            .append(XMLHandler.addTagValue(ELEM_NAME_SERIALIZATION_FORMAT, serializationFormat));

        if (fileDetail != null) {
            builder.append(XMLHandler.openTag(ELEM_NAME_FILE))
                    .append(XMLHandler.addTagValue(ELEM_NAME_FILENAME, fileDetail.filename))
                    .append(XMLHandler.addTagValue(ELEM_NAME_CREATE_PARENT_FOLDER, Boolean.toString(fileDetail.createParentFolder)))
                    .append(XMLHandler.addTagValue(ELEM_NAME_INCLUDE_STEP_NR, Boolean.toString(fileDetail.includeStepNr)))
                    .append(XMLHandler.addTagValue(ELEM_NAME_INCLUDE_PARTITION_NR, Boolean.toString(fileDetail.includePartitionNr)))
                    .append(XMLHandler.addTagValue(ELEM_NAME_INCLUDE_DATE, Boolean.toString(fileDetail.includeDate)))
                    .append(XMLHandler.addTagValue(ELEM_NAME_INCLUDE_TIME, Boolean.toString(fileDetail.includeTime)))
            .append(XMLHandler.closeTag(ELEM_NAME_FILE));
        }

        return builder.toString();
    }

    @Override
    public void loadXML(final Node stepnode, final List<DatabaseMeta> databases, final IMetaStore metaStore) throws KettleXMLException {
        final String xJenaModelField = XMLHandler.getTagValue(stepnode, ELEM_NAME_JENA_MODEL_FIELD);
        if (xJenaModelField != null) {
            this.jenaModelField = xJenaModelField;

            final Node node = XMLHandler.getSubNode(stepnode, ELEM_NAME_JENA_MODEL_FIELD);
            final String xCloseModelAndRemoveField = XMLHandler.getTagAttribute(node, ATTR_NAME_CLOSE_AND_REMOVE);
            this.closeModelAndRemoveField = isNullOrEmpty(xCloseModelAndRemoveField) ? true : Boolean.valueOf(xCloseModelAndRemoveField);

            final String xSerializationFormat = XMLHandler.getTagValue(stepnode, ELEM_NAME_SERIALIZATION_FORMAT);
            this.serializationFormat = isNotEmpty(xSerializationFormat) ? xSerializationFormat : Rdf11.DEFAULT_SERIALIZATION_FORMAT;

            final Node fileNode = XMLHandler.getSubNode(stepnode, ELEM_NAME_FILE);
            if (fileNode == null) {
                this.fileDetail = newDefaultFileDetail();
            } else {
                this.fileDetail = new FileDetail();

                final String xFilename = XMLHandler.getTagValue(fileNode, ELEM_NAME_FILENAME);
                this.fileDetail.filename = isNotEmpty(xFilename) ? xFilename : DEFAULT_FILENAME;

                final String xCreateParentFolder = XMLHandler.getTagValue(fileNode, ELEM_NAME_CREATE_PARENT_FOLDER);
                this.fileDetail.createParentFolder = isNotEmpty(xCreateParentFolder) ? Boolean.parseBoolean(xCreateParentFolder) : true;

                final String xIncludeStepNr = XMLHandler.getTagValue(fileNode, ELEM_NAME_INCLUDE_STEP_NR);
                this.fileDetail.includeStepNr = isNotEmpty(xIncludeStepNr) ? Boolean.parseBoolean(xIncludeStepNr) : false;

                final String xIncludePartitionNr = XMLHandler.getTagValue(fileNode, ELEM_NAME_INCLUDE_PARTITION_NR);
                this.fileDetail.includePartitionNr = isNotEmpty(xIncludePartitionNr) ? Boolean.parseBoolean(xIncludePartitionNr) : false;

                final String xIncludeDate = XMLHandler.getTagValue(fileNode, ELEM_NAME_INCLUDE_DATE);
                this.fileDetail.includeDate = isNotEmpty(xIncludeDate) ? Boolean.parseBoolean(xIncludeDate) : false;

                final String xIncludeTime = XMLHandler.getTagValue(fileNode, ELEM_NAME_INCLUDE_TIME);
                this.fileDetail.includeTime = isNotEmpty(xIncludeTime) ? Boolean.parseBoolean(xIncludeTime) : false;
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

        /**
         * 1. if we should close and remove the model field, then remove it from the rowMeta
         */
        if (closeModelAndRemoveField) {
            if (isNotEmpty(jenaModelField)) {
                final String expandedJenaModelField = space.environmentSubstitute(jenaModelField);
                try {
                    rowMeta.removeValueMeta(expandedJenaModelField);
                } catch (final KettleValueException e) {
                    throw new KettleStepException("Unable to remove field: " + expandedJenaModelField + (jenaModelField.equals(expandedJenaModelField) ? "" : "(" + jenaModelField + ")") + ": " + e.getMessage(), e);
                }
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
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING, BaseMessages.getString(PKG, "JenaSerializerStepMeta.CheckResult.NotReceivingFields"), stepMeta);
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "JenaSerializerStepMeta.CheckResult.StepRecevingData", prev.size() + ""), stepMeta);
            remarks.add(cr);
        }

        // See if we have input streams leading to this step!
        if (input.length > 0) {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "JenaSerializerStepMeta.CheckResult.StepRecevingData2"), stepMeta);
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "JenaSerializerStepMeta.CheckResult.NoInputReceivedFromOtherSteps"), stepMeta);
            remarks.add(cr);
        }
    }

    @Override
    public StepInterface getStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr, final TransMeta transMeta, final Trans trans) {
        return new JenaSerializerStep(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new JenaSerializerStepData();
    }

    @Override
    public String getDialogClassName() {
        return "uk.gov.nationalarchives.pdi.step.jena.serializer.JenaSerializerStepDialog";
    }



    // <editor-fold desc="settings getters and setters">
    public String getJenaModelField() {
        return jenaModelField;
    }

    public void setJenaModelField(final String jenaModelField) {
        this.jenaModelField = jenaModelField;
    }

    public boolean isCloseModelAndRemoveField() {
        return closeModelAndRemoveField;
    }

    public void setCloseModelAndRemoveField(final boolean closeModelAndRemoveField) {
        this.closeModelAndRemoveField = closeModelAndRemoveField;
    }

    public String getSerializationFormat() {
        return serializationFormat;
    }

    public void setSerializationFormat(final String serializationFormat) {
        this.serializationFormat = serializationFormat;
    }

    public FileDetail getFileDetail() {
        return fileDetail;
    }

    public void setFileDetail(final FileDetail fileDetail) {
        this.fileDetail = fileDetail;
    }
    // </editor-fold>
}
