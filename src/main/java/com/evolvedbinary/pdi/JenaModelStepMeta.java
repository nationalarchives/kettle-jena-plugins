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

import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;


/**
 * Skeleton for PDI Step plugin.
 */
@Step(id = "JenaModelStep", image = "JenaModelStep.svg", name = "Jena Model Step",
        description = "Constructs an Apache Jena Model", categoryDescription = "Transform")
public class JenaModelStepMeta extends BaseStepMeta implements StepMetaInterface {

    private static Class<?> PKG = JenaModelStep.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    public JenaModelStepMeta() {
        super(); // allocate BaseStepMeta
    }

    @Override
    public void loadXML(final Node stepnode, final List<DatabaseMeta> databases, final IMetaStore metaStore) throws KettleXMLException {
        readData(stepnode);
    }

    @Override
    public Object clone() {
        Object retval = super.clone();
        return retval;
    }

    private void readData(final Node stepnode) {
        // Parse the XML (starting with the given stepnode) to extract the step metadata (into member variables, for example)
    }

    @Override
    public void setDefault() {
    }

    @Override
    public void readRep(final Repository rep, final IMetaStore metaStore, final ObjectId id_step, final List<DatabaseMeta> databases) throws KettleException {
    }

    @Override
    public void saveRep(final Repository rep, final IMetaStore metaStore, final ObjectId id_transformation, final ObjectId id_step)
            throws KettleException {
    }

    @Override
    public void getFields(final RowMetaInterface rowMeta, final String origin, final RowMetaInterface[] info, final StepMeta nextStep,
                          final VariableSpace space, final Repository repository, final IMetaStore metaStore) throws KettleStepException {
        // Default: nothing changes to rowMeta
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
    public String getDialogClassName() {
        return "com.evolvedbinary.pdi.JenaModelStepDialog";
    }
}
