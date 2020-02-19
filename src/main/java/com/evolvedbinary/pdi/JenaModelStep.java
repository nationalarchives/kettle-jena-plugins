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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

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
    public boolean processRow(final StepMetaInterface smi, final StepDataInterface sdi) throws KettleException {
        final Object[] r = getRow(); // get row, set busy!
        if (r == null) {
            // no more input to be expected...
            setOutputDone();
            return false;
        }

        //TODO(AR)temp
        final String resourceUri = "http://catalogue/thing1";

        final Model model = ModelFactory.createDefaultModel();
        final Resource resource = model.createResource(resourceUri);
        resource.addProperty(RDF.type, "premis:IntellectualEntity");
        resource.addLiteral(DC.identifier, "AN1234");


        final RowMetaInterface inputRowMeta = getInputRowMeta();
        final RowMetaInterface outputRowMeta = inputRowMeta.clone();
        smi.getFields(outputRowMeta, getStepname(), null, null, this, repository, metaStore);

        putRow(outputRowMeta, r); // copy row to possible alternate rowset(s).

        if (checkFeedback(getLinesRead())) {
            if (log.isBasic())
                logBasic(BaseMessages.getString(PKG, "JenaModelStep.Log.LineNumber") + getLinesRead());
        }

        return true;
    }
}