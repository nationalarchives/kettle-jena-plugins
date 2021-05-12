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
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.gov.nationalarchives.pdi.step.jena.JenaUtil.closeAndThrow;
import static uk.gov.nationalarchives.pdi.step.jena.Util.*;

/**
 * Step for Creating a Jena Model
 */
public class JenaModelStep extends BaseStep implements StepInterface {

    private static Class<?> PKG = JenaModelStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    /**
     * Regular expression for xsd:dateTime
     */
    private static final Pattern PATTERN_XSD_DATE_TIME = Pattern.compile("-?[0-9]{4}-(((0(1|3|5|7|8)|1(0|2))-(0[1-9]|(1|2)[0-9]|3[0-1]))|((0(4|6|9)|11)-(0[1-9]|(1|2)[0-9]|30))|(02-(0[1-9]|(1|2)[0-9])))T([0-1][0-9]|2[0-4]):(0[0-9]|[1-5][0-9]):(0[0-9]|[1-5][0-9])(\\.[0-9]{1,3})?((\\+|-)([0-1][0-9]|2[0-4]):(0[0-9]|[1-5][0-9])|Z)?");

    /**
     * Regular expression for xsd:date
     */
    private static final Pattern PATTERN_XSD_DATE = Pattern.compile("-?[0-9]{4}-(((0(1|3|5|7|8)|1(0|2))-(0[1-9]|(1|2)[0-9]|3[0-1]))|((0(4|6|9)|11)-(0[1-9]|(1|2)[0-9]|30))|(02-(0[1-9]|(1|2)[0-9])))((\\+|-)([0-1][0-9]|2[0-4]):(0[0-9]|[1-5][0-9])|Z)?");


    public JenaModelStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr,
            final TransMeta transMeta, final Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {
        Object[] row = getRow(); // try and get a row
        if (row == null) {
            // no more rows...
            setOutputDone();
            return false;  // signal that we are DONE
        }

        // process a row...
        final RowMetaInterface inputRowMeta = getInputRowMeta();
        final JenaModelStepMeta meta = (JenaModelStepMeta) smi;
        final JenaModelStepData data = (JenaModelStepData) sdi;

        if (first) {
            first = false;

            // create output row meta data
            createOutputRowMeta(inputRowMeta, meta, data);

            // if we are removing fields, we need to map fields from input row to output row
            // NOTE: this must come after createOutputRowMeta
            prepareForReMap(inputRowMeta, meta, data);
        }

        if (isNullOrEmpty(meta.getTargetFieldName())) {
            throw new KettleStepException(BaseMessages.getString(
                    PKG, "JenaModelStep.Error.TargetFieldUndefined"));
        }

        // Create Jena model
        final Model model = createModel(inputRowMeta, meta, row);

        // remap any fields that we are keeping from the input row to the output row
        row = prepareOutputRow(meta, data, row);

        // Set Jena model in target field of the output row
        row[data.getTargetFieldIndex()] = model;

        // output the row
        putRow(data.getOutputRowMeta(), row);

        if (checkFeedback(getLinesRead())) {
            if (log.isBasic())
                logBasic(BaseMessages.getString(PKG, "JenaModelStep.Log.LineNumber") + getLinesRead());
        }

        return true;  // signal that we want the next row...
    }

    private void createOutputRowMeta(final RowMetaInterface inputRowMeta, final JenaModelStepMeta meta, final JenaModelStepData data) throws KettleStepException {
        final RowMetaInterface outputRowMeta = inputRowMeta.clone();
        meta.getFields(outputRowMeta, getStepname(), null, null, this, repository, metaStore);
        data.setOutputRowMeta(outputRowMeta);

        // must be done on the output row meta!
        final int targetFieldIndex = outputRowMeta.indexOfValue(meta.getTargetFieldName());
        if (targetFieldIndex < 0 ) {
            throw new KettleStepException(BaseMessages.getString(
                    PKG, "JenaModelStep.Error.TargetFieldNotFoundOutputStream", meta.getTargetFieldName()));
        }
        data.setTargetFieldIndex(targetFieldIndex);
    }

    /**
     * Stores the indexes of any fields from the input row
     * that need to be copied into the output row in the data object.
     *
     * The remapping itself is performed in {@link #prepareOutputRow(JenaModelStepMeta, JenaModelStepData, Object[])}.
     *
     * @param inputRowMeta the input row meta
     * @param meta the metadata
     * @param data the data
     *
     * @throws KettleException if an error occurs whilst preparing
     */
    private void prepareForReMap(final RowMetaInterface inputRowMeta, final JenaModelStepMeta meta, final JenaModelStepData data) throws KettleStepException {
        // prepare for re-map when removeSelectedFields is checked
        if (meta.isRemoveSelectedFields() && isNotEmpty(meta.getDbToJenaMappings())) {
            final int[] remainingInputFieldIndexes = new int[data.getOutputRowMeta().size() - 1]; // NOTE: the `-1` - we don't need the new target field

            // fields present in the outputRowMeta
            final String[] outputRowFieldName = data.getOutputRowMeta().getFieldNames();
            // NOTE the `-1` - we don't search the new target field
            for (int i = 0; i < outputRowFieldName.length - 1; i++) {
                final int remainingInputFieldIndex = inputRowMeta.indexOfValue(outputRowFieldName[i]);
                if (remainingInputFieldIndex < 0) {
                    throw new KettleStepException( BaseMessages.getString( PKG,
                            "JenaModelStep.Error.RemainingFieldNotFoundInputStream", outputRowFieldName[i]));
                }
                remainingInputFieldIndexes[i] = remainingInputFieldIndex;
            }

            data.setRemainingInputFieldIndexes(remainingInputFieldIndexes);
        }
    }

    /**
     * Reserve room for the target field and re-map the fields
     * from input row to output row that were stored in
     * {@link #prepareForReMap(RowMetaInterface, JenaModelStepMeta, JenaModelStepData)}.
     *
     * @param meta the metadata
     * @param data the data
     * @param row the input row
     *
     * @return the output row
     */
    private Object[] prepareOutputRow(final JenaModelStepMeta meta, final JenaModelStepData data, final Object[] row) {
        final Object[] outputRowData;

        if (meta.isRemoveSelectedFields() && isNotEmpty(meta.getDbToJenaMappings())) {
            // re-map fields from input to output when removeSelectedFields is checked

            // reserve room for the target field
            outputRowData = new Object[data.getOutputRowMeta().size() + RowDataUtil.OVER_ALLOCATE_SIZE];

            // re-map the fields from input to output
            final int[] remainingInputFieldIndexes = data.getRemainingInputFieldIndexes();
            for (int i = 0; i < remainingInputFieldIndexes.length; i++) { // NOTE: this does not include the  new target field (see prepareForReMap)
                final int remainingInputFieldIndex = remainingInputFieldIndexes[i];
                outputRowData[i] = row[remainingInputFieldIndex];
            }

        } else {
            // reserve room for the target field
            outputRowData = RowDataUtil.resizeArray(row, data.getOutputRowMeta().size());
        }
        return outputRowData;
    }

    private Model createModel(final RowMetaInterface inputRowMeta, final JenaModelStepMeta meta, final Object[] row) throws KettleException {
        final String resourceUriFieldName = environmentSubstitute(meta.getResourceUriField());
        final int idxResourceUriField = inputRowMeta.indexOfValue(resourceUriFieldName);

        if (idxResourceUriField < 0) {
            throw new KettleException("Could not find Resource URI field '" + resourceUriFieldName + "', index is: " + idxResourceUriField);
        } else if (idxResourceUriField >= row.length) {
            throw new KettleException("Could not find Resource URI field '" + resourceUriFieldName + "', index is beyond the bounds of the row(length=" + row.length + "): " + idxResourceUriField);
        }
        final Object resourceUriFieldValue =  row[idxResourceUriField];

        final String strResourceUriFieldValue;
        if (resourceUriFieldValue == null) {
            throw new KettleException("Resource URI field '" + resourceUriFieldName + "' cannot be null");
        } else if (resourceUriFieldValue instanceof String) {
            strResourceUriFieldValue = (String) resourceUriFieldValue;
        } else {
            logBasic("Expecting java.lang.String when processing resourceUriFieldValue, but found {0}. Will default to Object#toString()...", resourceUriFieldValue.getClass().getName());
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
            closeAndThrow(model, e);
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
                if (isNullOrEmpty(qname.getNamespaceURI())) {
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
                    switch (mapping.actionIfNull) {
                        case IGNORE:
                            // no-op - just ignore it!
                            break;

                        case WARN:
                            // log a warning
                            logBasic("Could not write property: {0} for resource: {1}, row field: {2} is null!", property.toString(), rootResourceUri, fieldName);
                            break;

                        case ERROR:
                            // throw an exception
                            closeAndThrow(model, "Could not write property: " + property.toString() + " for resource: " + rootResourceUri + ", row field: " + fieldName + " is null!");
                            break;
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

        } else if (rdfDatatype.equals(XSDDatatype.XSDboolean)) {
            // to xsd:boolean
            if (sqlValue instanceof Boolean) {
                return sqlValue;
            }

        } else if (rdfDatatype.equals(XSDDatatype.XSDdate)) {
            // to xsd:date
            if (sqlValue instanceof String) {
                if (sqlValue != null) {
                    // check lexical form
                    final Matcher matcher = PATTERN_XSD_DATE.matcher(sqlValue.toString());
                    if (matcher.matches()) {
                        return sqlValue;
                    }
                } else {
                    return sqlValue;
                }

            } else if (sqlValue instanceof java.sql.Date || sqlValue instanceof java.sql.Timestamp || sqlValue instanceof java.util.Date) {
                // TODO(AR) need to check about the locale/timezone here... make configurable?
                final Calendar ukCalendar = Calendar.getInstance(Locale.UK);
                ukCalendar.setTime((java.util.Date) sqlValue);
                return new XSDDateTime(ukCalendar);
            }

        } else if (rdfDatatype.equals(XSDDatatype.XSDdateTime)) {
            // to xsd:dateTime
            if (sqlValue instanceof String) {
                if (sqlValue != null) {
                    // check lexical form
                    final Matcher matcher = PATTERN_XSD_DATE_TIME.matcher(sqlValue.toString());
                    if (matcher.matches()) {
                        return sqlValue;
                    }
                } else {
                    return sqlValue;
                }

            } else if (sqlValue instanceof java.sql.Date || sqlValue instanceof java.sql.Timestamp || sqlValue instanceof java.util.Date) {
                // TODO(AR) need to check about the locale/timezone here... make configurable?
                final Calendar ukCalendar = Calendar.getInstance(Locale.UK);
                ukCalendar.setTime((java.util.Date) sqlValue);
                return new XSDDateTime(ukCalendar);
            }

        } else if (rdfDatatype.equals(XSDDatatype.XSDgYear)) {
            // to xsd:gYear
            if (sqlValue instanceof Long) {
                if (sqlValue != null) {
                    // TODO(AR) should we check the lexical form?
                    return sqlValue;
                }
            }

        } else if (rdfDatatype.equals(RDF.dtXMLLiteral)) {
            // to rdf:XMLLiteral
            if (sqlValue instanceof String) {
                return sqlValue;

            } else if (sqlValue instanceof byte[]) {
                return new String((byte[]) sqlValue, UTF_8);
            }

        } else if (rdfDatatype.equals(RDF.dtRDFHTML)) {
            // to rdf:HTML
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