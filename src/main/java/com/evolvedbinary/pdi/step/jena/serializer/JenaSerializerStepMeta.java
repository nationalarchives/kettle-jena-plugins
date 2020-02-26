/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.evolvedbinary.pdi.step.jena.serializer;

import com.evolvedbinary.pdi.step.jena.Rdf11;
import com.evolvedbinary.pdi.step.jena.Util;
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
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.*;


/**
 * Skeleton for PDI Step plugin.
 */
@Step(id = "JenaSerializerStep", image = "JenaSerializerStep.svg", name = "Serialize Jena Model",
        description = "Serializes an Apache Jena Model", categoryDescription = "Output")
public class JenaSerializerStepMeta extends BaseStepMeta implements StepMetaInterface {

    private static Class<?> PKG = JenaSerializerStep.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    // <editor-fold desc="settings XML element names">
    private static final String ELEM_NAME_JENA_MODEL_FIELD = "jenaModelField";
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
        retval.serializationFormat = serializationFormat;
        retval.fileDetail = fileDetail == null ? null : fileDetail.copy();
        return retval;
    }

    @Override
    public String getXML() throws KettleException {
        final StringBuilder builder = new StringBuilder();
        builder
            .append(XMLHandler.addTagValue(ELEM_NAME_JENA_MODEL_FIELD, jenaModelField))
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

            final String xSerializationFormat = XMLHandler.getTagValue(stepnode, ELEM_NAME_SERIALIZATION_FORMAT);
            this.serializationFormat = xSerializationFormat != null && !xSerializationFormat.isEmpty() ? xSerializationFormat : Rdf11.DEFAULT_SERIALIZATION_FORMAT;

            final Node fileNode = XMLHandler.getSubNode(stepnode, ELEM_NAME_FILE);
            if (fileNode == null) {
                this.fileDetail = newDefaultFileDetail();
            } else {
                this.fileDetail = new FileDetail();

                final String xFilename = XMLHandler.getTagValue(fileNode, ELEM_NAME_FILENAME);
                this.fileDetail.filename = xFilename != null && !xFilename.isEmpty() ? xFilename : DEFAULT_FILENAME;

                final String xCreateParentFolder = XMLHandler.getTagValue(fileNode, ELEM_NAME_CREATE_PARENT_FOLDER);
                this.fileDetail.createParentFolder = xCreateParentFolder != null && !xCreateParentFolder.isEmpty() ? Boolean.parseBoolean(xCreateParentFolder) : true;

                final String xIncludeStepNr = XMLHandler.getTagValue(fileNode, ELEM_NAME_INCLUDE_STEP_NR);
                this.fileDetail.includeStepNr = xIncludeStepNr != null && !xIncludeStepNr.isEmpty() ? Boolean.parseBoolean(xIncludeStepNr) : false;

                final String xIncludePartitionNr = XMLHandler.getTagValue(fileNode, ELEM_NAME_INCLUDE_PARTITION_NR);
                this.fileDetail.includePartitionNr = xIncludePartitionNr != null && !xIncludePartitionNr.isEmpty() ? Boolean.parseBoolean(xIncludePartitionNr) : false;

                final String xIncludeDate = XMLHandler.getTagValue(fileNode, ELEM_NAME_INCLUDE_DATE);
                this.fileDetail.includeDate = xIncludeDate != null && !xIncludeDate.isEmpty() ? Boolean.parseBoolean(xIncludeDate) : false;

                final String xIncludeTime = XMLHandler.getTagValue(fileNode, ELEM_NAME_INCLUDE_TIME);
                this.fileDetail.includeTime = xIncludeTime != null && !xIncludeTime.isEmpty() ? Boolean.parseBoolean(xIncludeTime) : false;
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
        if (rep == null || rep.isEmpty()) {
            setDefault();
        }

        final Node stepnode = XMLHandler.loadXMLString(rep);
        loadXML(stepnode, (List<DatabaseMeta>)null, (IMetaStore)null);
    }

    @Override
    public void getFields(final RowMetaInterface rowMeta, final String origin, final RowMetaInterface[] info, final StepMeta nextStep,
                          final VariableSpace space, final Repository repository, final IMetaStore metaStore) throws KettleStepException {

        //TODO(AR) we also need the database fields here?

//        try {
//            // add the target field to the output rows
//            if (targetFieldName != null && !targetFieldName.isEmpty()) {
//                final ValueMetaInterface targetFieldValueMeta = ValueMetaFactory.createValueMeta(targetFieldName, ValueMeta.TYPE_SERIALIZABLE);
//                targetFieldValueMeta.setOrigin(origin);
//                rowMeta.addValueMeta(targetFieldValueMeta);
//            }
//
//        } catch (final KettlePluginException e) {
//            throw new KettleStepException(e);
//        }
//
//        if (removeSelectedFields && dbToJenaMappings != null) {
//            for (final DbToJenaMapping mapping : dbToJenaMappings) {
//                try {
//                    rowMeta.removeValueMeta(mapping.fieldName);
//                } catch (final KettleValueException e) {
//                    //TODO(AR) log error or throw?
//                    System.out.println(e.getMessage());
//                    e.printStackTrace();
//                }
//            }
//        }
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
    public StepInterface getStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int cnr, final TransMeta tr, final Trans trans) {
        return new JenaSerializerStep(stepMeta, stepDataInterface, cnr, tr, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new JenaSerializerStepData();
    }

    @Override
    public RepositoryDirectory getRepositoryDirectory() {
        return super.getRepositoryDirectory();
    }

    @Override
    public String getDialogClassName() {
        return "com.evolvedbinary.pdi.step.jena.serializer.JenaSerializerStepDialog";
    }



    // <editor-fold desc="settings getters and setters">
    public String getJenaModelField() {
        return jenaModelField;
    }

    public void setJenaModelField(final String jenaModelField) {
        this.jenaModelField = jenaModelField;
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
