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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.FormDataBuilder;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import static uk.gov.nationalarchives.pdi.step.jena.Util.isNotEmpty;
import static uk.gov.nationalarchives.pdi.step.jena.serializer.JenaSerializerStepMeta.DEFAULT_FILENAME;

public class JenaShaclDialog extends BaseStepDialog implements StepDialogInterface {

    private static final Class<?> PKG = JenaShaclStepMeta.class; // for i18n purposes

    private static final int MARGIN_SIZE = 15;
    private static final int LABEL_SPACING = 5;
    private static final int ELEMENT_SPACING = 10;

    private static final int LARGE_FIELD = 350;
    private static final int MEDIUM_FIELD = 250;

    private final JenaShaclStepMeta meta;

    private Text wStepNameField;
    private ComboVar wModelFieldCombo;
    private TextVar wShapeFileTextField;
    private FileDialog wBrowseFileDialog;
    private boolean changed;

    public JenaShaclDialog(final Shell parent, final Object in, final TransMeta transMeta, final String sname) {
        super(parent, (BaseStepMeta) in, transMeta, sname);
        meta = (JenaShaclStepMeta) in;
    }

    @Override
    public String open() {
        //Set up window
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
        shell.setMinimumSize(450, 335);
        props.setLook(shell);
        setShellImage(shell, meta);

        ModifyListener lsMod = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                meta.setChanged();
            }
        };
        changed = meta.hasChanged();

        //15 pixel margins
        FormLayout formLayout = new FormLayout();
        formLayout.marginLeft = MARGIN_SIZE;
        formLayout.marginHeight = MARGIN_SIZE;
        shell.setLayout(formLayout);
        shell.setText(BaseMessages.getString(PKG, "JenaShaclStep.Shell.Title"));

        //Build a scrolling composite and a composite for holding all content
        ScrolledComposite scrolledComposite = new ScrolledComposite(shell, SWT.V_SCROLL);
        Composite contentComposite = new Composite(scrolledComposite, SWT.NONE);
        FormLayout contentLayout = new FormLayout();
        contentLayout.marginRight = MARGIN_SIZE;
        contentComposite.setLayout(contentLayout);
        FormData compositeLayoutData = new FormDataBuilder().fullSize()
                .result();
        contentComposite.setLayoutData(compositeLayoutData);
        props.setLook(contentComposite);

        //Step name label and text field
        Label wStepNameLabel = new Label(contentComposite, SWT.RIGHT);
        wStepNameLabel.setText(BaseMessages.getString(PKG, "JenaShaclStep.Stepname.Label"));
        props.setLook(wStepNameLabel);
        FormData fdStepNameLabel = new FormDataBuilder().left()
                .top()
                .result();
        wStepNameLabel.setLayoutData(fdStepNameLabel);

        wStepNameField = new Text(contentComposite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepNameField.setText(stepname);
        props.setLook(wStepNameField);
        wStepNameField.addModifyListener(lsMod);
        FormData fdStepName = new FormDataBuilder().left()
                .top(wStepNameLabel, LABEL_SPACING)
                .width(MEDIUM_FIELD)
                .result();
        wStepNameField.setLayoutData(fdStepName);

        //Job icon, centered vertically between the top of the label and the bottom of the field.
        Label wicon = new Label(contentComposite, SWT.CENTER);
        wicon.setImage(getImage());
        FormData fdIcon = new FormDataBuilder().right()
                .top(0, 4)
                .bottom(new FormAttachment(wStepNameField, 0, SWT.BOTTOM))
                .result();
        wicon.setLayoutData(fdIcon);
        props.setLook(wicon);

        //Spacer between entry info and content
        Label topSpacer = new Label(contentComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
        FormData fdSpacer = new FormDataBuilder().fullWidth()
                .top(wStepNameField, MARGIN_SIZE)
                .result();
        topSpacer.setLayoutData(fdSpacer);

        //Groups for first type of content
        Group group = new Group(contentComposite, SWT.SHADOW_ETCHED_IN);
        group.setText(BaseMessages.getString(PKG, "JenaShaclStep.GroupText"));
        FormLayout groupLayout = new FormLayout();
        groupLayout.marginWidth = MARGIN_SIZE;
        groupLayout.marginHeight = MARGIN_SIZE;
        group.setLayout(groupLayout);
        FormData groupLayoutData = new FormDataBuilder().fullWidth()
                .top(topSpacer, MARGIN_SIZE)
                .result();
        group.setLayoutData(groupLayoutData);
        props.setLook(group);

        //Model Field label/field/button
        Label wModelFieldLabel = new Label(group, SWT.LEFT);
        props.setLook(wModelFieldLabel);
        wModelFieldLabel.setText(BaseMessages.getString(PKG, "JenaShaclStep.TextFieldModelField"));
        FormData fdTransformation0 = new FormDataBuilder().left()
                .top()
                .result();
        wModelFieldLabel.setLayoutData(fdTransformation0);

        wModelFieldCombo = new ComboVar(transMeta, group,  SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wModelFieldCombo);
        FormData fdlTransformation0 = new FormDataBuilder().left()
                .top(wModelFieldLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wModelFieldCombo.setLayoutData(fdlTransformation0);

        Button wGetModelFieldButton = new Button(group, SWT.PUSH);
        wGetModelFieldButton.setText(BaseMessages.getString(PKG, "JenaShaclStep.GetFieldsButton"));
        FormData fdGetModelField = new FormDataBuilder().left(wModelFieldCombo, LABEL_SPACING)
                .top(wModelFieldLabel, LABEL_SPACING)
                .result();
        wGetModelFieldButton.setLayoutData(fdGetModelField);

        //filename label/field/button
        Label wFilenameLabel = new Label(group, SWT.LEFT);
        props.setLook(wFilenameLabel);
        wFilenameLabel.setText(BaseMessages.getString(PKG, "JenaShaclStep.TextFieldFilename"));
        FormData fdTransformation2 = new FormDataBuilder().left()
                .top(wModelFieldCombo, ELEMENT_SPACING)
                .result();
        wFilenameLabel.setLayoutData(fdTransformation2);

        wShapeFileTextField = new TextVar(transMeta, group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wShapeFileTextField);
        FormData fdlTransformation2 = new FormDataBuilder().left()
                .top(wFilenameLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wShapeFileTextField.setLayoutData(fdlTransformation2);

        Button wShapeFileBrowseButton = new Button(group, SWT.PUSH);
        wShapeFileBrowseButton.setText(BaseMessages.getString(PKG, "JenaShaclStep.ButtonBrowse"));
        FormData fdBrowse = new FormDataBuilder().left(wShapeFileTextField, LABEL_SPACING)
                .top(wFilenameLabel, LABEL_SPACING)
                .result();
        wShapeFileBrowseButton.setLayoutData(fdBrowse);

        wBrowseFileDialog = new FileDialog(shell, SWT.OPEN);
        wBrowseFileDialog.setFilterNames(new String[] {
                "Turtle Files (*.ttl)",
                "N-Triple Files (*.nt)",
                "N3 Files (*.n3)",
                "RDF/XML Files (*.xml)",
                "All Files (*.*)"
        });
        wBrowseFileDialog.setFilterExtensions(new String[] { "*.ttl", "*.nt", "*.n3", "*.xml", "*.*" });
        wBrowseFileDialog.setFilterPath("c:\\"); // Windows path
        wBrowseFileDialog.setFileName(DEFAULT_FILENAME);

        //Cancel, action and OK buttons for the bottom of the window.
        Button wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
        FormData fdCancel = new FormDataBuilder().right(100, -MARGIN_SIZE)
                .bottom()
                .result();
        wCancel.setLayoutData(fdCancel);

        Button wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
        FormData fdOk = new FormDataBuilder().right(wCancel, -LABEL_SPACING)
                .bottom()
                .result();
        wOK.setLayoutData(fdOk);

        //Space between bottom buttons and the table, final layout for table
        Label bottomSpacer = new Label(shell, SWT.HORIZONTAL | SWT.SEPARATOR);
        FormData fdhSpacer = new FormDataBuilder().left()
                .right(100, -MARGIN_SIZE)
                .bottom(wCancel, -MARGIN_SIZE)
                .result();
        bottomSpacer.setLayoutData(fdhSpacer);

        //Add everything to the scrolling composite
        scrolledComposite.setContent(contentComposite);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setMinSize(contentComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        scrolledComposite.setLayout(new FormLayout());
        FormData fdScrolledComposite = new FormDataBuilder().fullWidth()
                .top()
                .bottom(bottomSpacer, -MARGIN_SIZE)
                .result();
        scrolledComposite.setLayoutData(fdScrolledComposite);
        props.setLook(scrolledComposite);

        //Listeners
        Listener lsGetField = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                getFieldsFromPrevious(wModelFieldCombo, transMeta, stepMeta);
                wModelFieldCombo.select(0);
            }
        };
        Listener lsBrowseFilename = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                wShapeFileTextField.setText(wBrowseFileDialog.open());
            }
        };
        Listener lsCancel = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                cancel();
            }
        };
        Listener lsOK = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                ok();
            }
        };

        wGetModelFieldButton.addListener(SWT.Selection, lsGetField);
        wShapeFileBrowseButton.addListener(SWT.Selection, lsBrowseFilename);
        wOK.addListener(SWT.Selection, lsOK);
        wCancel.addListener(SWT.Selection, lsCancel);

        SelectionAdapter lsDef = new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                ok();
            }
        };
        wStepNameField.addSelectionListener(lsDef);

        shell.addShellListener(new ShellAdapter() {
            public void shellClosed(ShellEvent e) {
                cancel();
            }
        });

        //Show shell
        setSize();
        getData(meta);
        meta.setChanged(changed);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return stepname;
    }

    private Image getImage() {
        final PluginInterface plugin =
                PluginRegistry.getInstance().getPlugin(StepPluginType.class, stepMeta.getStepMetaInterface());
        final String id = plugin.getIds()[0];
        if (id != null) {
            return GUIResource.getInstance().getImagesSteps().get(id).getAsBitmapForSize(shell.getDisplay(),
                    ConstUI.ICON_SIZE, ConstUI.ICON_SIZE);
        }
        return null;
    }

    private void getData(final JenaShaclStepMeta meta) {
        final String jenaModelField = meta.getJenaModelField();
        if (isNotEmpty(jenaModelField)) {
            wModelFieldCombo.setText(jenaModelField);
        }

        final String shapesFilePath = meta.getShapesFilePath();
        if (shapesFilePath != null) {
            wShapeFileTextField.setText(shapesFilePath);
        }
    }

    /**
     * Called when the user cancels the dialog.
     */
    private void cancel() {
        // The "stepname" variable will be the return value for the open() method.
        // Setting to null to indicate that dialog was cancelled.
        stepname = null;
        // Restoring original "changed" flag on the met aobject
        meta.setChanged( changed );
        // close the SWT dialog window
        dispose();
    }

    /**
     * Called when the user confirms the dialog
     */
    private void ok() {
        // START save data
        meta.setJenaModelField(wModelFieldCombo.getText());
        meta.setShapesFilePath(wShapeFileTextField.getText());
        // END save data

        // NOTIFY CHANGE
        meta.setChanged(true);


        stepname = wStepNameField.getText();
        dispose();
    }

}
