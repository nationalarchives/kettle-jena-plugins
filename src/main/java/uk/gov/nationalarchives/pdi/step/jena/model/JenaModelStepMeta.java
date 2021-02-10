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
package uk.gov.nationalarchives.pdi.step.jena.model;

import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import uk.gov.nationalarchives.pdi.step.jena.Rdf11;
import uk.gov.nationalarchives.pdi.step.jena.Util;
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

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.*;

import static uk.gov.nationalarchives.pdi.step.jena.Util.BLANK_NODE_INTERNAL_URI;
import static uk.gov.nationalarchives.pdi.step.jena.Util.BLANK_NODE_NAME;


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
    private static final String ELEM_NAME_SKIP = "skip";
    private static final String ELEM_NAME_ACTION_IF_NULL = "actionIfNull";
    private static final String ELEM_NAME_BLANK_NODE_MAPPINGS = "blankNodeMappings";
    private static final String ELEM_NAME_BLANK_NODE_MAPPING = "blankNodeMapping";
    private static final String ELEM_NAME_ID = "id";
    // </editor-fold>

    // <editor-fold desc="settings">
    private String targetFieldName;
    private boolean removeSelectedFields;
    private String resourceUriField;

    /**
     * Namespace mapping from prefix->uri
     */
    private Map<String, String> namespaces;

    enum ActionIfNull {
        IGNORE,
        WARN,
        ERROR
    }

    static class DbToJenaMapping implements Cloneable {
        String fieldName;
        QName rdfPropertyName;
        QName rdfType;
        boolean skip;
        ActionIfNull actionIfNull;

        @Override
        public Object clone() {
            return copy();
        }

        public DbToJenaMapping copy() {
            final DbToJenaMapping copy = new DbToJenaMapping();
            copy.fieldName = fieldName;
            copy.rdfPropertyName = Util.copy(rdfPropertyName);
            copy.rdfType = Util.copy(rdfType);
            copy.skip = skip;
            copy.actionIfNull = actionIfNull;
            return copy;
        }
    }

    static class BlankNodeMapping implements Comparable<BlankNodeMapping>, Cloneable {
        int id;
        DbToJenaMapping[] dbToJenaMappings;

        @Override
        public int compareTo(final BlankNodeMapping other) {
            return this.id - other.id;
        }

        @Override
        public Object clone() {
            return copy();
        }

        public BlankNodeMapping copy() {
            final BlankNodeMapping copy = new BlankNodeMapping();
            copy.id = id;
            copy.dbToJenaMappings = JenaModelStepMeta.copy(dbToJenaMappings);
            return copy;
        }
    }

    private DbToJenaMapping[] dbToJenaMappings;
    private BlankNodeMapping[] blankNodeMappings;
    // </editor-fold>


    public JenaModelStepMeta() {
        super(); // allocate BaseStepMeta
    }

    @Override
    public void setDefault() {
        targetFieldName = "";
        removeSelectedFields = false;
        resourceUriField = "";
        namespaces = new LinkedHashMap<>();
        namespaces.put(Rdf11.RDF_PREFIX, RDF.uri);
        namespaces.put(Rdf11.RDF_SCHEMA_PREFIX, RDFS.uri);
        namespaces.put(Rdf11.XSD_PREFIX, XSD.NS);
        dbToJenaMappings = new DbToJenaMapping[0];
        blankNodeMappings = new BlankNodeMapping[0];
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
            retval.dbToJenaMappings = JenaModelStepMeta.copy(dbToJenaMappings);
        } else {
            retval.dbToJenaMappings = new DbToJenaMapping[0];
        }
        if (blankNodeMappings != null && blankNodeMappings.length > 0) {
            retval.blankNodeMappings = JenaModelStepMeta.copy(blankNodeMappings);
        } else {
            retval.blankNodeMappings = new BlankNodeMapping[0];
        }
        return retval;
    }

    @Override
    public String getXML() throws KettleException {
        final StringBuilder builder = new StringBuilder();
        builder
            .append(XMLHandler.addTagValue(ELEM_NAME_TARGET_FIELD_NAME, targetFieldName))
            .append(XMLHandler.addTagValue(ELEM_NAME_REMOVE_SELECTED_FIELDS, Boolean.toString(removeSelectedFields)))
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

        getDbToJenaMappingsXML(dbToJenaMappings, builder);

        builder.append(XMLHandler.openTag(ELEM_NAME_BLANK_NODE_MAPPINGS));
        for (final BlankNodeMapping blankNodeMapping : blankNodeMappings) {
            builder.append(XMLHandler.openTag(ELEM_NAME_BLANK_NODE_MAPPING));
                builder.append(XMLHandler.openTag(ELEM_NAME_ID));
                    final QName idQName = new QName(BLANK_NODE_INTERNAL_URI, String.valueOf(blankNodeMapping.id), BLANK_NODE_NAME);
                    builder.append(addQNameValue(idQName));
                builder.append(XMLHandler.closeTag(ELEM_NAME_ID));
                getDbToJenaMappingsXML(blankNodeMapping.dbToJenaMappings, builder);
            builder.append(XMLHandler.closeTag(ELEM_NAME_BLANK_NODE_MAPPING));
        }
        builder.append(XMLHandler.closeTag(ELEM_NAME_BLANK_NODE_MAPPINGS));

        return builder.toString();
    }

    private void getDbToJenaMappingsXML(final DbToJenaMapping[] dbToJenaMappings, final StringBuilder builder) {
        builder.append(XMLHandler.openTag(ELEM_NAME_DB_TO_JENA_MAPPINGS));
        for (final DbToJenaMapping dbToJenaMapping : dbToJenaMappings) {
            if (dbToJenaMapping.fieldName != null && !dbToJenaMapping.fieldName.isEmpty()) {
                builder
                        .append(XMLHandler.openTag(ELEM_NAME_DB_TO_JENA_MAPPING))

                            .append(XMLHandler.addTagValue(ELEM_NAME_FIELD_NAME, dbToJenaMapping.fieldName))

                            .append(XMLHandler.openTag(ELEM_NAME_PROPERTY_NAME))
                                .append(addQNameValue(dbToJenaMapping.rdfPropertyName))
                            .append(XMLHandler.closeTag(ELEM_NAME_PROPERTY_NAME))

                            .append(XMLHandler.openTag(ELEM_NAME_RDF_TYPE));
                                if (dbToJenaMapping.rdfType != null) {
                                    builder.append(addQNameValue(dbToJenaMapping.rdfType));
                                }
                            builder.append(XMLHandler.closeTag(ELEM_NAME_RDF_TYPE))

                            .append(XMLHandler.addTagValue(ELEM_NAME_SKIP, dbToJenaMapping.skip))

                            .append(XMLHandler.addTagValue(ELEM_NAME_ACTION_IF_NULL, dbToJenaMapping.actionIfNull.name()))

                        .append(XMLHandler.closeTag(ELEM_NAME_DB_TO_JENA_MAPPING));
            }
        }
        builder.append(XMLHandler.closeTag(ELEM_NAME_DB_TO_JENA_MAPPINGS));
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

                        if (BLANK_NODE_INTERNAL_URI.equals(uri)) {
                            // don't load the internal namespace to our namespaces map (as it is used for display)
                            continue;
                        }

                        this.namespaces.put(prefix, uri);
                    }
                }
            }

            this.dbToJenaMappings = loadDbToJenaMappingsXML(stepnode);

            final Node blankNodeMappingsNode = XMLHandler.getSubNode(stepnode, ELEM_NAME_BLANK_NODE_MAPPINGS);
            if (blankNodeMappingsNode == null) {
                this.blankNodeMappings = new BlankNodeMapping[0];
            } else {
                final List<Node> blankNodeMappingNodes = XMLHandler.getNodes(blankNodeMappingsNode, ELEM_NAME_BLANK_NODE_MAPPING);
                if (blankNodeMappingNodes == null || blankNodeMappingNodes.isEmpty()) {
                    this.blankNodeMappings = new BlankNodeMapping[0];
                } else {
                    final int len = blankNodeMappingNodes.size();
                    this.blankNodeMappings = new BlankNodeMapping[len];
                    int mappingsCount = 0;
                    for (int i = 0; i < len; i++) {
                        final Node blankNodeMappingNode = blankNodeMappingNodes.get(i);

                        final Node idNode = XMLHandler.getSubNode(blankNodeMappingNode, ELEM_NAME_ID);
                        if (idNode == null) {
                            continue;
                        }

                        final QName idQName = getQNameValue(idNode);
                        if (!BLANK_NODE_INTERNAL_URI.equals(idQName.getNamespaceURI())) {
                            continue;
                        }

                        final BlankNodeMapping blankNodeMapping = new BlankNodeMapping();
                        blankNodeMapping.id = Integer.valueOf(idQName.getLocalPart());
                        blankNodeMapping.dbToJenaMappings = loadDbToJenaMappingsXML(blankNodeMappingNode);

                        this.blankNodeMappings[mappingsCount++] = blankNodeMapping;
                    }

                    if (mappingsCount < len) {
                        this.blankNodeMappings = Arrays.copyOf(this.blankNodeMappings, mappingsCount);
                    }
                }
            }
            Arrays.sort(this.blankNodeMappings);  // just-in-case the incoming XML is not ordered correctly
        }
    }

    private DbToJenaMapping[] loadDbToJenaMappingsXML(final Node parentNode) {
        DbToJenaMapping[] dbToJenaMappings = null;

        final Node dbToJenaMappingsNode = XMLHandler.getSubNode(parentNode, ELEM_NAME_DB_TO_JENA_MAPPINGS);
        if (dbToJenaMappingsNode == null) {
            dbToJenaMappings = new DbToJenaMapping[0];
        } else {
            final List<Node> dbToJenaMappingNodes = XMLHandler.getNodes(dbToJenaMappingsNode, ELEM_NAME_DB_TO_JENA_MAPPING);
            if (dbToJenaMappingNodes == null || dbToJenaMappingNodes.isEmpty()) {
                dbToJenaMappings = new DbToJenaMapping[0];
            } else {
                final int len = dbToJenaMappingNodes.size();
                dbToJenaMappings = new DbToJenaMapping[len];
                int mappingsCount = 0;
                for (int i = 0; i < len; i++) {
                    final Node dbToJenaMappingNode = dbToJenaMappingNodes.get(i);

                    final String fieldName = XMLHandler.getTagValue(dbToJenaMappingNode, ELEM_NAME_FIELD_NAME);
                    if (fieldName == null || fieldName.isEmpty()) {
                        continue;
                    }

                    final DbToJenaMapping dbToJenaMapping = new DbToJenaMapping();
                    dbToJenaMapping.fieldName = fieldName;

                    final Node propertyNameNode = XMLHandler.getSubNode(dbToJenaMappingNode, ELEM_NAME_PROPERTY_NAME);
                    dbToJenaMapping.rdfPropertyName = getQNameValue(propertyNameNode);

                    final Node rdfTypeNode = XMLHandler.getSubNode(dbToJenaMappingNode, ELEM_NAME_RDF_TYPE);
                    dbToJenaMapping.rdfType = getQNameValue(rdfTypeNode);

                    final String skipNode = XMLHandler.getTagValue(dbToJenaMappingNode, ELEM_NAME_SKIP);
                    dbToJenaMapping.skip = skipNode != null && skipNode.equals("Y");

                    final String actionIfNullName = XMLHandler.getTagValue(dbToJenaMappingNode, ELEM_NAME_ACTION_IF_NULL);
                    if (actionIfNullName != null) {
                        dbToJenaMapping.actionIfNull = ActionIfNull.valueOf(actionIfNullName);
                    } else {
                        // default for backwards-compatibility with previous versions of our plugin
                        dbToJenaMapping.actionIfNull = ActionIfNull.WARN;
                    }

                    dbToJenaMappings[mappingsCount++] = dbToJenaMapping;
                }

                if (mappingsCount < len) {
                    dbToJenaMappings = Arrays.copyOf(dbToJenaMappings, mappingsCount);
                }
            }
        }

        return dbToJenaMappings;
    }

    private @Nullable QName getQNameValue(final Node node) {
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
                final ValueMetaInterface targetFieldValueMeta = ValueMetaFactory.createValueMeta(space.environmentSubstitute(targetFieldName), ValueMeta.TYPE_SERIALIZABLE);
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
        return "uk.gov.nationalarchives.pdi.step.jena.model.JenaModelStepDialog";
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

    public BlankNodeMapping[] getBlankNodeMappings() {
        return blankNodeMappings;
    }

    public void setBlankNodeMappings(final BlankNodeMapping[] blankNodeMappings) {
        this.blankNodeMappings = blankNodeMappings;
    }

    // </editor-fold>

    private static @Nullable DbToJenaMapping[] copy(@Nullable final DbToJenaMapping[] dbToJenaMappings) {
        if (dbToJenaMappings == null) {
            return null;
        }

        final DbToJenaMapping[] copiedDbToJenaMappings = new DbToJenaMapping[dbToJenaMappings.length];
        for (int i = 0; i < dbToJenaMappings.length; i++) {
            copiedDbToJenaMappings[i] = dbToJenaMappings[i].copy();
        }
        return copiedDbToJenaMappings;
    }

    private static @Nullable BlankNodeMapping[] copy(@Nullable final BlankNodeMapping[] blankNodeMappings) {
        if (blankNodeMappings == null) {
            return null;
        }

        final BlankNodeMapping[] copiedBlankNodeMappings = new BlankNodeMapping[blankNodeMappings.length];
        for (int i = 0; i < blankNodeMappings.length; i++) {
            copiedBlankNodeMappings[i] = blankNodeMappings[i].copy();
        }
        return copiedBlankNodeMappings;
    }
}
