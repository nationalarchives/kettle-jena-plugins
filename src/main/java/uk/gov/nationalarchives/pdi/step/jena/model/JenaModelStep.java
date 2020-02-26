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
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Map;

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

        Object[] r = getRow(); // try and get a row
        if (r == null) {
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
                final Model model = createModel(meta, r, inputRowMeta);

                // first, add the new column (for the Jena Model)
                r = RowDataUtil.resizeArray(r, inputRowMeta.size() + 1);
                r[inputRowMeta.size()] = model;

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
                    r = RowDataUtil.removeItems(r, indexes);
                }
            }

            putRow(outputRowMeta, r); // copy row to possible alternate rowset(s).

            if (checkFeedback(getLinesRead())) {
                if (log.isBasic())
                    logBasic(BaseMessages.getString(PKG, "JenaModelStep.Log.LineNumber") + getLinesRead());
            }

            return true;  // signal that we want the next row...
        }
    }

    private Model createModel(final JenaModelStepMeta meta, final Object[] r, final RowMetaInterface inputRowMeta) {
        final String resourceUriFieldName = meta.getResourceUriField();
        final int idxResourceUriField = inputRowMeta.indexOfValue(resourceUriFieldName);
        final Object resourceUriFieldValue =  r[idxResourceUriField];
        //TODO(AR) need to do better data conversion
        final String strResourceUriFieldValue = resourceUriFieldValue.toString();

        final Model model = ModelFactory.createDefaultModel();

        // add namespaces
        final Map<String, String> namespaces = meta.getNamespaces();
        if (namespaces != null) {
            model.setNsPrefixes(namespaces);
        }

        // create the resource
        final Resource resource = model.createResource(strResourceUriFieldValue);
        resource.addProperty(RDF.type, meta.getResourceType());

        // add the resource properties
        if (meta.getDbToJenaMappings() != null) {
            for (final JenaModelStepMeta.DbToJenaMapping mapping : meta.getDbToJenaMappings()) {
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

                final String fieldName = mapping.fieldName;
                final int idxField = inputRowMeta.indexOfValue(fieldName);
                final Object fieldValue = r[idxField];

                if (fieldValue == null) {
                    logBasic("Could not write property: {0} for resource: {1}, row field is null!", property.toString(), strResourceUriFieldValue);
                } else {
                    //TODO(AR) need to do better data conversion
                    final String strFieldValue = fieldValue.toString();

                    if (mapping.rdfType == null) {
                        // non-typed literal
                        final Literal literal = model.createLiteral(strFieldValue);
                        resource.addLiteral(property, literal);

                    } else if ("Resource".equals(mapping.rdfType.getLocalPart())) {
                        // resource
                        final String otherResourceUri = asUri(meta.getNamespaces(), strFieldValue);
                        final Resource otherResource = model.createResource(otherResourceUri);
                        resource.addProperty(property, otherResource);

                    } else {
                        // typed literal
                        final String typeURI = mapping.rdfType.getNamespaceURI() + mapping.rdfType.getLocalPart();
                        final RDFDatatype rdfDatatype = TypeMapper.getInstance().getSafeTypeByName(typeURI);
                        final Literal literal = model.createTypedLiteral(strFieldValue, rdfDatatype);
                        resource.addLiteral(property, literal);
                    }
                }
            }
        }

        return model;
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