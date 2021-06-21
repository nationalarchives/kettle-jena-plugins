/*
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
package uk.gov.nationalarchives.pdi.step.jena.shacl;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.List;

import static uk.gov.nationalarchives.pdi.step.jena.Util.isNotEmpty;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;

@Step(
        id = "JenaShaclStep",
        name = "JenaShaclStep.Name",
        description = "JenaShaclStep.TooltipDesc",
        image = "JenaShaclStep.svg",
        categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Validation",
        i18nPackageName = "uk.gov.nationalarchives.pdi.step.jena.shacl"
)
public class JenaShaclStepMeta extends BaseStepMeta implements StepMetaInterface {

    /**
     * Constructor should call super() to make sure the base class has a chance to initialize properly.
     */
    public JenaShaclStepMeta() {
        super();
    }

    private static final String ELEM_NAME_JENA_MODEL_FIELD = "jenaModelField";
    private static final String ELEM_SHAPE_FILE_PATH = "shapeFilePath";

    private String jenaModelField;
    private String shapesFilePath;

    /**
     * Returns the row field that contains an Apache Jena Model to be validated
     */
    public String getJenaModelField() {
        return jenaModelField;
    }

    /**
     * Returns the SHACL shape file path to be used in the validation
     */
    public String getShapesFilePath() {
        return shapesFilePath;
    }

    /**
     * Sets the row field that contains an Apache Jena Model to be validated
     */
    public void setJenaModelField(final String jenaModelField) {
        this.jenaModelField = jenaModelField;
    }

    /**
     * Sets the SHACL shape file path to be used in the validation
     */
    public void setShapesFilePath(final String shapesFilePath) {
        this.shapesFilePath = shapesFilePath;
    }

    /**
     * Called by Spoon to get a new instance of the SWT dialog for the step.
     * A standard implementation passing the arguments to the constructor of the step dialog is recommended.
     *
     * @param shell    an SWT Shell
     * @param meta     description of the step
     * @param transMeta  description of the the transformation
     * @param name    the name of the step
     * @return       new instance of a dialog for this step
     */
    public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name ) {
        return new JenaShaclDialog( shell, meta, transMeta, name );
    }

    @Override
    public StepInterface getStep(final StepMeta stepMeta, final StepDataInterface stepDataInterface, final int copyNr, final TransMeta transMeta, final Trans trans) {
        return new JenaShaclStep(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new JenaShaclStepData();
    }

    @Override
    public boolean supportsErrorHandling() {
        return true;
    }

    @Override
    public void loadXML(final Node stepnode, final List<DatabaseMeta> databases, final IMetaStore metaStore) throws KettleXMLException {
        final String xJenaModelField = XMLHandler.getTagValue(stepnode, ELEM_NAME_JENA_MODEL_FIELD);
        if (xJenaModelField != null) {
            this.jenaModelField = xJenaModelField;
            final String xShapeFilePath = XMLHandler.getTagValue(stepnode, ELEM_SHAPE_FILE_PATH);
            this.shapesFilePath = isNotEmpty(xShapeFilePath) ? xShapeFilePath : "shape.ttl";
        }
    }

    @Override
    public void setDefault() {}

    @Override
    public String getXML() throws KettleException {
        final StringBuilder builder = new StringBuilder();
        builder.append(XMLHandler.addTagValue(ELEM_NAME_JENA_MODEL_FIELD, jenaModelField))
                .append(XMLHandler.addTagValue(ELEM_SHAPE_FILE_PATH, shapesFilePath));
        return builder.toString();
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
        if (isNullOrEmpty(rep)) {
            setDefault();
        }

        final Node stepnode = XMLHandler.loadXMLString(rep);
        loadXML(stepnode, null, (IMetaStore)null);
    }

}
