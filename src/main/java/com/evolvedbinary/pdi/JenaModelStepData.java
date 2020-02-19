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

//import org.apache.jena.rdf.model.Model;
//import org.apache.jena.rdf.model.ModelFactory;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;


public class JenaModelStepData extends BaseStepData implements StepDataInterface {
    private String targetFieldName;
    private String resourceType;
    private DbToJenaMapping[] dbToJenaMappings;

    private static class DbToJenaMapping {
        String fieldName;
        String rdfPropertyName;
        String rdfType;
    }

//    private final Model model;
    // Add any execution-specific data here

    /**
     *
     */
    public JenaModelStepData() {
        super();
//        this.model = ModelFactory.createDefaultModel();
    }

    public void setTargetFieldName(final String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public void setResourceType(final String resourceType) {
        this.resourceType = resourceType;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public DbToJenaMapping[] getDbToJenaMappings() {
        return dbToJenaMappings;
    }

    public void setDbToJenaMappings(final DbToJenaMapping[] dbToJenaMappings) {
        this.dbToJenaMappings = dbToJenaMappings;
    }

    //    public Model getModel() {
//        return model;
//    }
}