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

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.gov.nationalarchives.pdi.step.jena.Util.*;

/**
 * Step for Creating a Jena Model
 */
public class JenaModelStep extends BaseStep implements StepInterface {

    private static Class<?> PKG = JenaModelStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    public JenaModelStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr,
            final TransMeta transMeta, final Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    /**
     * Initialize and do work where other steps need to wait for...
     *
     * @param stepMetaInterface The metadata to work with
     * @param stepDataInterface The data to initialize
     */
    @Override
    public boolean init(final StepMetaInterface stepMetaInterface, final StepDataInterface stepDataInterface) {
        return super.init(stepMetaInterface, stepDataInterface);
    }

    @Override
    public void dispose(final StepMetaInterface smi, final StepDataInterface sdi) {
        super.dispose(smi, sdi);
    }

    @Override
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {

        final JenaModelStepMeta meta = (JenaModelStepMeta)smi;

        Object[] row = getRow(); // try and get a row
        if (row == null) {
            // no more rows...
            setOutputDone();
            return false;  // signal that we are DONE

        } else {

            // process a row...

            final RowMetaInterface inputRowMeta = getInputRowMeta();
            final RowMetaInterface outputRowMeta = inputRowMeta.clone();
            smi.getFields(outputRowMeta, getStepname(), null, null, this, repository, metaStore);

            //TODO(AR) seems we have to duplicate behaviour of JenaModelStepMeta getFields here but on `r` ???
            if (meta.getTargetFieldName() != null && !meta.getTargetFieldName().isEmpty()) {
                // create Jena model
                final Model model = createModel(meta, inputRowMeta, row);

                // first, add the new column (for the Jena Model)
                row = RowDataUtil.resizeArray(row, inputRowMeta.size() + 1);
                row[inputRowMeta.size()] = model;

                // second, remove any unneeded columns
                if (meta.isRemoveSelectedFields() && meta.getDbToJenaMappings() != null) {
                    final JenaModelStepMeta.DbToJenaMapping[] mappings = meta.getDbToJenaMappings();
                    final int indexes[] = new int[mappings.length];
                    int i = 0;
                    for (final JenaModelStepMeta.DbToJenaMapping mapping : mappings) {
                        final int index = inputRowMeta.indexOfValue(mapping.fieldName);
                        indexes[i++] = index;
                    }
                    Arrays.sort(indexes);
                    row = RowDataUtil.removeItems(row, indexes);
                }
            }

            putRow(outputRowMeta, row); // copy row to possible alternate rowset(s).

            if (checkFeedback(getLinesRead())) {
                if (log.isBasic())
                    logBasic(BaseMessages.getString(PKG, "JenaModelStep.Log.LineNumber") + getLinesRead());
            }

            return true;  // signal that we want the next row...
        }
    }

    private Model createModel(final JenaModelStepMeta meta, final RowMetaInterface inputRowMeta, final Object[] row) throws KettleException {
        final String resourceUriFieldName = environmentSubstitute(meta.getResourceUriField());
        final int idxResourceUriField = inputRowMeta.indexOfValue(resourceUriFieldName);
        final Object resourceUriFieldValue =  row[idxResourceUriField];

        final String strResourceUriFieldValue;
        if (resourceUriFieldValue instanceof String) {
            strResourceUriFieldValue = (String) resourceUriFieldValue;
        } else {
            logBasic("Expecting java.lang.String when processing resourceUriFieldValue, but found {}. Will default to Object#toString()...");
            strResourceUriFieldValue = resourceUriFieldValue.toString();
        }

        final Model model = ModelFactory.createDefaultModel();
        try {
            // start a transaction on the model
            if (model.supportsTransactions()) {
                model.begin();
            }

            // add namespaces
            final Map<String, String> namespaces = meta.getNamespaces();
            if (namespaces != null) {
                model.setNsPrefixes(namespaces);
            }

            // create the resource
            final Resource resource = model.createResource(strResourceUriFieldValue);

            // add the resource properties
            addResourceProperties(
                    fieldName -> valueLookup(inputRowMeta, row, fieldName),
                    strResourceUriFieldValue,
                    model,
                    resource,
                    meta.getNamespaces(),
                    meta.getDbToJenaMappings(),
                    meta.getBlankNodeMappings());

            // commit the transaction
            if (model.supportsTransactions()) {
                model.commit();
            }
        } catch (final KettleException e) {
            // close the jena model
            model.close();

            // rethrow the exception
            throw e;
        }

        return model;
    }

    private Object valueLookup(final RowMetaInterface inputRowMeta, final Object[] row, final String fieldName) {
        final int idxField;
        if (fieldName != null) {
            idxField = inputRowMeta.indexOfValue(fieldName);
        } else {
            idxField = -1;
        }
        final Object fieldValue;
        if (idxField > -1) {
            fieldValue = row[idxField];
        } else {
            fieldValue = null;
        }
        return fieldValue;
    }

    private void addResourceProperties(final Function<String, Object> valueLookup, final String rootResourceUri,
            final Model model, final Resource resource, final Map<String, String> namespaces,
            final JenaModelStepMeta.DbToJenaMapping[] dbToJenaMappings,
            final JenaModelStepMeta.BlankNodeMapping[] blankNodeMappings) throws KettleException {

        if (dbToJenaMappings != null) {
            for (final JenaModelStepMeta.DbToJenaMapping mapping : dbToJenaMappings) {
                if (mapping.skip) {
                    continue;
                }

                final QName qname = mapping.rdfPropertyName;
                Property property;
                if (qname.getNamespaceURI() == null || qname.getNamespaceURI().isEmpty()) {
                    property = model.getProperty(qname.getLocalPart());
                    if (property == null) {
                        property = model.createProperty(qname.getLocalPart());
                    }
                } else {
                    property = model.getProperty(qname.getNamespaceURI(), qname.getLocalPart());
                    if (property == null) {
                        property = model.createProperty(qname.getNamespaceURI(), qname.getLocalPart());
                    }
                }

                final String fieldName = nullIfEmpty(mapping.fieldName);
                final boolean isBNodeFieldName = fieldName != null && fieldName.equals(BLANK_NODE_FIELD_NAME);
                final Object fieldValue = valueLookup.apply(fieldName);

                if ((!isBNodeFieldName) && fieldValue == null) {
                    if (mapping.actionIfNull == JenaModelStepMeta.ActionIfNull.IGNORE) {
                        // no-op - just ignore it!

                    } else if (mapping.actionIfNull == JenaModelStepMeta.ActionIfNull.WARN) {
                        // log a warning
                        logBasic("Could not write property: {0} for resource: {1}, row field is null!", property.toString(), rootResourceUri);

                    } else if (mapping.actionIfNull == JenaModelStepMeta.ActionIfNull.ERROR) {
                        // throw an exception

                        closeAndThrow(model, "Could not write property: " + property.toString() + " for resource: " + rootResourceUri + ", row field is null!");
                    }
                } else {
                    if (mapping.rdfType == null) {
                        final String rdfLiteralValue = (String) convertSqlValueToRdf(fieldValue, null);
                        Literal literal;

                        if (mapping.language == null) {
                            // non-typed literal
                            literal = model.createLiteral(rdfLiteralValue);
                        } else {
                            // language-tagged string
                            literal = model.createLiteral(rdfLiteralValue, mapping.language);
                        }

                        resource.addLiteral(property, literal);

                    } else if (isBNodeFieldName) {
                        // blank node
                        if (!BLANK_NODE_INTERNAL_URI.equals(mapping.rdfType.getNamespaceURI())) {
                            // field name indicates a blank node, but the rdf type does not... error!
                            closeAndThrow(model, "Could not write property: " + property.toString() + " for resource: " + rootResourceUri + ", mapping to Blank Node definition is invalid!");

                        } else {
                            // get the proposed blank node id
                            final int toBlankNodeId = Integer.parseInt(mapping.rdfType.getLocalPart());
                            if (blankNodeMappings == null || toBlankNodeId >= blankNodeMappings.length) {
                                // corresponding blank node mapping does not exist!
                                closeAndThrow(model, "Could not write property: " + property.toString() + " for resource: " + rootResourceUri + ", corresponding Blank Node mapping does not exist!");
                            }

                            // get the blank node mapping
                            final JenaModelStepMeta.BlankNodeMapping blankNodeMapping = blankNodeMappings[toBlankNodeId];
                            if (blankNodeMapping.id != toBlankNodeId) {
                                // corresponding blank node id does not match expected blank node id
                                closeAndThrow(model, "Could not write property: " + property.toString() + " for resource: " + rootResourceUri + ", corresponding Blank Node mapping has id: " + blankNodeMapping.id + " but expected id: " + toBlankNodeId + "!");
                            }

                            // create the blank node
                            final Resource blankNode = model.createResource();

                            // call this function recursively but passing in the blank node as the resource
                            addResourceProperties(valueLookup, rootResourceUri, model, blankNode, namespaces, blankNodeMapping.dbToJenaMappings, blankNodeMappings);

                            // add the property to the resource
                            resource.addProperty(property, blankNode);
                        }

                    } else if ("Resource".equals(mapping.rdfType.getLocalPart())) {
                        // resource
                        final String strFieldValue = (String) convertSqlValueToRdf(fieldValue, null);
                        final String otherResourceUri = asUri(namespaces, strFieldValue);
                        final Resource otherResource = model.createResource(otherResourceUri);
                        resource.addProperty(property, otherResource);

                    } else {
                        // typed literal
                        final String typeURI = mapping.rdfType.getNamespaceURI() + mapping.rdfType.getLocalPart();
                        final RDFDatatype rdfDatatype = TypeMapper.getInstance().getSafeTypeByName(typeURI);
                        final Object rdfLiteralValue = convertSqlValueToRdf(fieldValue, rdfDatatype);
                        final Literal literal = model.createTypedLiteral(rdfLiteralValue, rdfDatatype);
                        resource.addLiteral(property, literal);
                    }
                }
            }
        }
    }

    private void closeAndThrow(final Model model, final String message) throws KettleException {
        // abort the transaction on the model
        if (model.supportsTransactions()) {
            model.abort();
        }
        model.close();
        throw new KettleException(message);
    }

    private Object convertSqlValueToRdf(final Object sqlValue, @Nullable final RDFDatatype rdfDatatype) {
        if (rdfDatatype == null || rdfDatatype.equals(XSDDatatype.XSDstring)) {
            // to xsd:string
            if (sqlValue instanceof String) {
                return sqlValue;

            } else if (sqlValue instanceof byte[]) {
                return new String((byte[]) sqlValue, UTF_8);

            } else if (sqlValue instanceof java.sql.Date || sqlValue instanceof java.sql.Timestamp) {
                return sqlValue.toString();

            } else if (sqlValue instanceof Number) {
                return ((Number)sqlValue).toString();

            }

        } else if (rdfDatatype.equals(XSDDatatype.XSDdateTime)) {
            // to xsd:dateTime
            if (sqlValue instanceof String) {
                return sqlValue;

            } else if (sqlValue instanceof java.sql.Date || sqlValue instanceof java.sql.Timestamp || sqlValue instanceof java.util.Date) {
                return (java.util.Date) sqlValue;
            }

        } else if (rdfDatatype.equals(RDF.dtXMLLiteral)) {
            // to rdf:XMLLiteral
            if (sqlValue instanceof String) {
                return sqlValue;

            } else if (sqlValue instanceof byte[]) {
                return new String((byte[]) sqlValue, UTF_8);

            }
        }

        // fallback
        logBasic("convertSqlValueToRdfLiteralValue: required {0} but was given: {1}, unsure how to convert... Will default to {1}}!", rdfDatatype.getURI(), sqlValue.getClass());
        return sqlValue;
    }

    private static String asUri(final Map<String, String> namespaces, final String fieldValue) {
        final int idxNsSep = fieldValue.indexOf(':');
        if (idxNsSep > -1) {
            final String prefix = fieldValue.substring(0, idxNsSep);
            final String localPart = fieldValue.substring(idxNsSep + 1);
            if (namespaces != null && namespaces.containsKey(prefix)) {
                final String nsUri = namespaces.get(prefix);
                return nsUri + localPart;
            } else {
                return fieldValue;
            }
        } else {
            final int idxNsOpenBracket = fieldValue.indexOf('{');
            final int idxNsCloseBracket = fieldValue.indexOf('}');
            if (idxNsOpenBracket > -1 && idxNsCloseBracket > -1 && idxNsCloseBracket < fieldValue.length() - 1) {
                final String nsUri = fieldValue.substring(idxNsOpenBracket + 1, idxNsCloseBracket);
                final String localPart = fieldValue.substring(idxNsCloseBracket + 1);
                return nsUri + localPart;
            } else {
                return fieldValue;
            }
        }
    }
}