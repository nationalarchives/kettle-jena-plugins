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
package com.evolvedbinary.pdi.step.jena.model;

import com.evolvedbinary.pdi.step.jena.Rdf11;
import com.evolvedbinary.pdi.step.jena.Util;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
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
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.*;


/**
 * Skeleton for PDI Step plugin.
 */
@Step(id = "JenaModelStep", image = "JenaModelStep.svg", name = "Create Jena Model",
        description = "Constructs an Apache Jena Model", categoryDescription = "Transform")
public class JenaModelStepMeta extends BaseStepMeta implements StepMetaInterface {

    private static Class<?> PKG = JenaModelStep.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    // <editor-fold desc="settings XML element names">
    private static final String ELEM_NAME_TARGET_FIELD_NAME = "targetFieldName";
    private static final String ELEM_NAME_REMOVE_SELECTED_FIELDS = "removeSelectedFields";
    private static final String ELEM_NAME_RESOURCE_TYPE = "resourceType";
    private static final String ELEM_NAME_RESOURCE_URI = "resourceUri";
    private static final String ELEM_NAME_NAMESPACES = "namespaces";
    private static final String ELEM_NAME_NAMESPACE = "namespace";
    private static final String ELEM_NAME_PREFIX = "prefix";
    private static final String ELEM_NAME_URI = "uri";
    private static final String ELEM_NAME_LOCAL_PART = "localPart";
    private static final String ELEM_NAME_DB_TO_JENA_MAPPINGS = "dbToJenaMappings";
    private static final String ELEM_NAME_DB_TO_JENA_MAPPING = "dbToJenaMapping";
    private static final String ELEM_NAME_FIELD_NAME = "fieldName";
    private static final String ELEM_NAME_PROPERTY_NAME = "rdfPropertyName";
    private static final String ELEM_NAME_RDF_TYPE = "rdfType";
    // </editor-fold>

    // <editor-fold desc="settings">
    private String targetFieldName;
    private boolean removeSelectedFields;
    private String resourceType; // TODO(AR) store as QName
    private String resourceUriField;

    /**
     * Namespace mapping from prefix->uri
     */
    private Map<String, String> namespaces;
    static class DbToJenaMapping implements Cloneable {
        String fieldName;
        QName rdfPropertyName;
        QName rdfType;

        @Override
        public Object clone() {
            return copy();
        }

        public DbToJenaMapping copy() {
            final DbToJenaMapping copy = new DbToJenaMapping();
            copy.fieldName = fieldName;
            copy.rdfPropertyName = Util.copy(rdfPropertyName);
            copy.rdfType = Util.copy(rdfType);
            return copy;
        }
    }
    private DbToJenaMapping[] dbToJenaMappings;
    // </editor-fold>


    public JenaModelStepMeta() {
        super(); // allocate BaseStepMeta
    }

    @Override
    public void setDefault() {
        targetFieldName = "";
        removeSelectedFields = false;
        resourceType = "rdfs:Class";
        resourceUriField = "";
        namespaces = new LinkedHashMap<>();
        namespaces.put(Rdf11.RDF_PREFIX, Rdf11.RDF_NAMESPACE_IRI);
        namespaces.put(Rdf11.RDF_SCHEMA_PREFIX, Rdf11.RDF_SCHEMA_NAMESPACE_IRI);
        namespaces.put(Rdf11.XSD_PREFIX, Rdf11.XSD_NAMESPACE_IRI);
        dbToJenaMappings = new DbToJenaMapping[0];
    }

    @Override
    public Object clone() {
        final JenaModelStepMeta retval = (JenaModelStepMeta) super.clone();
        if (namespaces != null && !namespaces.isEmpty()) {
            retval.namespaces = new LinkedHashMap<>(namespaces);
        } else {
            retval.namespaces = Collections.emptyMap();
        }
        if (dbToJenaMappings != null && dbToJenaMappings.length > 0) {
            retval.dbToJenaMappings = new DbToJenaMapping[dbToJenaMappings.length];
            for (int i = 0; i < dbToJenaMappings.length; i++) {
                retval.dbToJenaMappings[i] = dbToJenaMappings[i].copy();
            }
        } else {
            retval.dbToJenaMappings = new DbToJenaMapping[0];
        }
        return retval;
    }

    @Override
    public String getXML() throws KettleException {
        final StringBuilder builder = new StringBuilder();
        builder
            .append(XMLHandler.addTagValue(ELEM_NAME_TARGET_FIELD_NAME, targetFieldName))
            .append(XMLHandler.addTagValue(ELEM_NAME_REMOVE_SELECTED_FIELDS, Boolean.toString(removeSelectedFields)))
            .append(XMLHandler.addTagValue(ELEM_NAME_RESOURCE_TYPE, resourceType))
            .append(XMLHandler.openTag(ELEM_NAME_RESOURCE_URI))
                .append(XMLHandler.addTagValue(ELEM_NAME_FIELD_NAME, resourceUriField))
            .append(XMLHandler.closeTag(ELEM_NAME_RESOURCE_URI));

        builder.append(XMLHandler.openTag(ELEM_NAME_NAMESPACES));
        for (final Map.Entry<String, String> pn : namespaces.entrySet()) {
            builder
                    .append(XMLHandler.openTag(ELEM_NAME_NAMESPACE))
                    .append(XMLHandler.addTagValue(ELEM_NAME_PREFIX, pn.getKey()))
                    .append(XMLHandler.addTagValue(ELEM_NAME_URI, pn.getValue()))
                    .append(XMLHandler.closeTag(ELEM_NAME_NAMESPACE));
        }
        builder.append(XMLHandler.closeTag(ELEM_NAME_NAMESPACES));

        builder.append(XMLHandler.openTag(ELEM_NAME_DB_TO_JENA_MAPPINGS));
        for (final DbToJenaMapping mapping : dbToJenaMappings) {
            if (mapping.fieldName != null && !mapping.fieldName.isEmpty()) {
                builder
                    .append(XMLHandler.openTag(ELEM_NAME_DB_TO_JENA_MAPPING))

                    .append(XMLHandler.addTagValue(ELEM_NAME_FIELD_NAME, mapping.fieldName))

                    .append(XMLHandler.openTag(ELEM_NAME_PROPERTY_NAME))
                        .append(addQNameValue(mapping.rdfPropertyName))
                    .append(XMLHandler.closeTag(ELEM_NAME_PROPERTY_NAME))

                    .append(XMLHandler.openTag(ELEM_NAME_RDF_TYPE));
                    if (mapping.rdfType != null) {
                        builder.append(addQNameValue(mapping.rdfType));
                    }
                    builder.append(XMLHandler.closeTag(ELEM_NAME_RDF_TYPE))

                    .append(XMLHandler.closeTag(ELEM_NAME_DB_TO_JENA_MAPPING));
            }
        }
        builder.append(XMLHandler.closeTag(ELEM_NAME_DB_TO_JENA_MAPPINGS));

        return builder.toString();
    }

    private String addQNameValue(final QName qname) {
        final StringBuilder builder = new StringBuilder();
        if (qname.getPrefix() != null) {
            builder.append(XMLHandler.addTagValue(ELEM_NAME_PREFIX, qname.getPrefix()));
        }
        if (qname.getNamespaceURI() != null) {
            builder.append(XMLHandler.addTagValue(ELEM_NAME_URI, qname.getNamespaceURI()));
        }
        builder.append(XMLHandler.addTagValue(ELEM_NAME_LOCAL_PART, qname.getLocalPart()));

        return builder.toString();
    }

    @Override
    public void loadXML(final Node stepnode, final List<DatabaseMeta> databases, final IMetaStore metaStore) throws KettleXMLException {
        final String xTargetFieldName = XMLHandler.getTagValue(stepnode, ELEM_NAME_TARGET_FIELD_NAME);
        if (xTargetFieldName != null) {
            this.targetFieldName = xTargetFieldName;

            final String xRemoveSelectedFields = XMLHandler.getTagValue(stepnode, ELEM_NAME_REMOVE_SELECTED_FIELDS);
            this.removeSelectedFields = xRemoveSelectedFields != null && !xRemoveSelectedFields.isEmpty() ? Boolean.parseBoolean(xRemoveSelectedFields) : false;

            final String xResourceType = XMLHandler.getTagValue(stepnode, ELEM_NAME_RESOURCE_TYPE);
            this.resourceType = Util.emptyIfNull(xResourceType);

            final Node resourceUriNode = XMLHandler.getSubNode(stepnode, ELEM_NAME_RESOURCE_URI);
            if (resourceUriNode == null) {
                this.resourceUriField = "";
            } else {
                final String xResourceUriField = XMLHandler.getTagValue(resourceUriNode, ELEM_NAME_FIELD_NAME);
                this.resourceUriField = xResourceUriField;
            }

            final Node namespacesNode = XMLHandler.getSubNode(stepnode, ELEM_NAME_NAMESPACES);
            if (namespacesNode == null) {
                this.namespaces = Collections.emptyMap();
            } else {
                final List<Node> namespaceNodes = XMLHandler.getNodes(namespacesNode, ELEM_NAME_NAMESPACE);
                if (namespaceNodes == null || namespaceNodes.isEmpty()) {
                    this.namespaces = Collections.emptyMap();
                } else {
                    this.namespaces = new LinkedHashMap<>();

                    final int len = namespaceNodes.size();
                    for (int i = 0; i < len; i++) {
                        final Node namespaceNode = namespaceNodes.get(i);

                        final String prefix = XMLHandler.getTagValue(namespaceNode, ELEM_NAME_PREFIX);
                        final String uri = XMLHandler.getTagValue(namespaceNode, ELEM_NAME_URI);
                        if (prefix == null || uri == null) {
                            continue;
                        }

                        this.namespaces.put(prefix, uri);
                    }
                }
            }

            final Node mappingsNode = XMLHandler.getSubNode(stepnode, ELEM_NAME_DB_TO_JENA_MAPPINGS);
            if (mappingsNode == null) {
                this.dbToJenaMappings = new DbToJenaMapping[0];
            } else {
                final List<Node> mappingNodes = XMLHandler.getNodes(mappingsNode, ELEM_NAME_DB_TO_JENA_MAPPING);
                if (mappingNodes == null || mappingNodes.isEmpty()) {
                    this.dbToJenaMappings = new DbToJenaMapping[0];
                } else {
                    final int len = mappingNodes.size();
                    this.dbToJenaMappings = new DbToJenaMapping[len];
                    int mappingsCount = 0;
                    for (int i = 0; i < len; i++) {
                        final Node mappingNode = mappingNodes.get(i);

                        final String fieldName = XMLHandler.getTagValue(mappingNode, ELEM_NAME_FIELD_NAME);
                        if (fieldName == null || fieldName.isEmpty()) {
                            continue;
                        }

                        final DbToJenaMapping mapping = new DbToJenaMapping();
                        mapping.fieldName = fieldName;

                        final Node propertyNameNode = XMLHandler.getSubNode(mappingNode, ELEM_NAME_PROPERTY_NAME);
                        mapping.rdfPropertyName = getQNameValue(propertyNameNode);

                        final Node rdfTypeNode = XMLHandler.getSubNode(mappingNode, ELEM_NAME_RDF_TYPE);
                        mapping.rdfType = getQNameValue(rdfTypeNode);
                        this.dbToJenaMappings[mappingsCount++] = mapping;
                    }

                    if (mappingsCount < len) {
                        this.dbToJenaMappings = Arrays.copyOf(this.dbToJenaMappings, mappingsCount);
                    }
                }
            }
        }
    }

    private QName getQNameValue(final Node node) {
        final String prefix = XMLHandler.getTagValue(node, ELEM_NAME_PREFIX);
        final String uri = XMLHandler.getTagValue(node, ELEM_NAME_URI);
        final String localPart = XMLHandler.getTagValue(node, ELEM_NAME_LOCAL_PART);

        if (localPart == null) {
            return null;
        }

        if (prefix != null) {
            return new QName(uri, localPart, prefix);
        } else if (uri != null) {
            return new QName(uri, localPart);
        } else {
            return new QName(localPart);
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

        try {
            // add the target field to the output rows
            if (targetFieldName != null && !targetFieldName.isEmpty()) {
                final ValueMetaInterface targetFieldValueMeta = ValueMetaFactory.createValueMeta(targetFieldName, ValueMeta.TYPE_SERIALIZABLE);
                targetFieldValueMeta.setOrigin(origin);
                rowMeta.addValueMeta(targetFieldValueMeta);
            }

        } catch (final KettlePluginException e) {
            throw new KettleStepException(e);
        }

        if (removeSelectedFields && dbToJenaMappings != null) {
            for (final DbToJenaMapping mapping : dbToJenaMappings) {
                try {
                    rowMeta.removeValueMeta(mapping.fieldName);
                } catch (final KettleValueException e) {
                    //TODO(AR) log error or throw?
                    System.out.println(e.getMessage());
                    e.printStackTrace();
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
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING, BaseMessages.getString(PKG, "JenaModelStepMeta.CheckResult.NotReceivingFields"), stepMeta);
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "JenaModelStepMeta.CheckResult.StepRecevingData", prev.size() + ""), stepMeta);
            remarks.add(cr);
        }

        // See if we have input streams leading to this step!
        if (input.length > 0) {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG, "JenaModelStepMeta.CheckResult.StepRecevingData2"), stepMeta);
            remarks.add(cr);
        } else {
            cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "JenaModelStepMeta.CheckResult.NoInputReceivedFromOtherSteps"), stepMeta);
            remarks.add(cr);
        }
    }

    @Override
    public StepInterface getStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int cnr, final TransMeta tr, final Trans trans) {
        return new JenaModelStep(stepMeta, stepDataInterface, cnr, tr, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new JenaModelStepData();
    }

    @Override
    public RepositoryDirectory getRepositoryDirectory() {
        return super.getRepositoryDirectory();
    }

    @Override
    public String getDialogClassName() {
        return "com.evolvedbinary.pdi.step.jena.model.JenaModelStepDialog";
    }



    // <editor-fold desc="settings getters and setters">
    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(final String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public boolean isRemoveSelectedFields() {
        return removeSelectedFields;
    }

    public void setRemoveSelectedFields(boolean removeSelectedFields) {
        this.removeSelectedFields = removeSelectedFields;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(final String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUriField() {
        return resourceUriField;
    }

    public void setResourceUriField(final String resourceUriField) {
        this.resourceUriField = resourceUriField;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    /**
     * Set the namespaces.
     *
     * @param namespaces (prefix->uri)
     */
    public void setNamespaces(final Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    public DbToJenaMapping[] getDbToJenaMappings() {
        return dbToJenaMappings;
    }

    public void setDbToJenaMappings(final DbToJenaMapping[] dbToJenaMappings) {
        this.dbToJenaMappings = dbToJenaMappings;
    }
    // </editor-fold>
}
