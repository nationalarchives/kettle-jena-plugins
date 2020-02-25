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
package com.evolvedbinary.pdi;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
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

/**
 * Describe your step plugin.
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

        Object[] r = getRow(); // get row, set busy!
        if (r == null) {
            // no more input to be expected...
            setOutputDone();
            return false;
        }

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

        return true;
    }

    private Model createModel(final JenaModelStepMeta meta, final Object[] r, final RowMetaInterface inputRowMeta) {
        final String resourceUriFieldName = meta.getResourceUriField();
        final int idxResourceUriField = inputRowMeta.indexOfValue(resourceUriFieldName);
        final Object resourceUriFieldValue =  r[idxResourceUriField];
        //TODO(AR) need to do better data conversion
        final String strResourceUriFieldValue = resourceUriFieldValue.toString();

        final Model model = ModelFactory.createDefaultModel();
        final Resource resource = model.createResource(strResourceUriFieldValue);
        resource.addProperty(RDF.type, meta.getResourceType());

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
                final Object fieldValue =  r[idxField];
                //TODO(AR) need to do better data conversion
                final String strFieldValue = fieldValue.toString();

                //TODO(AR) add literal typing for non xsd:string e.g. model.createTypedLiteral

                resource.addLiteral(property, strFieldValue);
            }
        }

        resource.addLiteral(DC.identifier, "AN1234");

        return model;
    }
}