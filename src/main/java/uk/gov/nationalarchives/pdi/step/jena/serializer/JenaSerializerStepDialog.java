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
package uk.gov.nationalarchives.pdi.step.jena.serializer;

import uk.gov.nationalarchives.pdi.step.jena.Rdf11;
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
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;
import static uk.gov.nationalarchives.pdi.step.jena.serializer.JenaSerializerStepMeta.DEFAULT_FILENAME;

public class JenaSerializerStepDialog extends BaseStepDialog implements StepDialogInterface {

    private static Class<?> PKG = JenaSerializerStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    private static final int MARGIN_SIZE = 15;
    private static final int LABEL_SPACING = 5;
    private static final int ELEMENT_SPACING = 10;

    private static final int LARGE_FIELD = 350;
    private static final int MEDIUM_FIELD = 250;
    private static final int SMALL_FIELD = 75;

    private JenaSerializerStepMeta meta;

    private ScrolledComposite scrolledComposite;
    private Composite contentComposite;
    private Label wStepNameLabel;
    private Text wStepNameField;
    private Label wModelFieldLabel;
    private ComboVar wModelFieldCombo;
    private Button wGetModelFieldButton;
    private Label wCloseModelAndRemoveFieldLabel;
    private Button wCloseModelAndRemoveFieldCheckbox;
    private Label wSerializationFormatLabel;
    private ComboVar wSerializationFormatCombo;
    private Label wFilenameLabel;
    private TextVar wFilenameTextField;
    private Button wFilenameBrowseButton;
    private FileDialog wBrowseFileDialog;
    private Label wCreateParentLabel;
    private Button wCreateParentFolderCheckbox;
    private Label wIncludeStepNrLabel;
    private Button wIncludeStepNrCheckbox;
    private Label wIncludePartitionNrLabel;
    private Button wIncludePartitionNrCheckbox;
    private Label wIncludeDateLabel;
    private Button wIncludeDateCheckbox;
    private Label wIncludeTimeLabel;
    private Button wIncludeTimeCheckbox;
    private Button wCancel;
    private Button wOK;
    private ModifyListener lsMod;
    private Listener lsGetField;
    private Listener lsBrowseFilename;
    private Listener lsCancel;
    private Listener lsOK;
    private SelectionAdapter lsDef;
    private boolean changed;


    public JenaSerializerStepDialog(final Shell parent, final Object in, final TransMeta transMeta, final String stepname) {
        super(parent, (BaseStepMeta) in, transMeta, stepname);
        meta = (JenaSerializerStepMeta) in;
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

        lsMod = new ModifyListener() {
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
        shell.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.Shell.Title"));

        //Build a scrolling composite and a composite for holding all content
        scrolledComposite = new ScrolledComposite(shell, SWT.V_SCROLL);
        contentComposite = new Composite(scrolledComposite, SWT.NONE);
        FormLayout contentLayout = new FormLayout();
        contentLayout.marginRight = MARGIN_SIZE;
        contentComposite.setLayout(contentLayout);
        FormData compositeLayoutData = new FormDataBuilder().fullSize()
                .result();
        contentComposite.setLayoutData(compositeLayoutData);
        props.setLook(contentComposite);

        //Step name label and text field
        wStepNameLabel = new Label(contentComposite, SWT.RIGHT);
        wStepNameLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.Stepname.Label"));
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
        group.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.GroupText"));
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
        wModelFieldLabel = new Label(group, SWT.LEFT);
        props.setLook(wModelFieldLabel);
        wModelFieldLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.TextFieldModelField"));
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

        wGetModelFieldButton = new Button(group, SWT.PUSH);
        wGetModelFieldButton.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.GetFieldsButton"));
        FormData fdGetModelField = new FormDataBuilder().left(wModelFieldCombo, LABEL_SPACING)
                .top(wModelFieldLabel, LABEL_SPACING)
                .result();
        wGetModelFieldButton.setLayoutData(fdGetModelField);

        // Close Model and Remove Field label/field/button
        wCloseModelAndRemoveFieldLabel = new Label(group, SWT.LEFT);
        props.setLook(wCloseModelAndRemoveFieldLabel);
        wCloseModelAndRemoveFieldLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.CheckboxCloseModelAndRemoveField"));
        final FormData fdCloseModelAndRemoveFieldLabel = new FormDataBuilder().left()
                .top(wModelFieldCombo, ELEMENT_SPACING)
                .result();
        wCloseModelAndRemoveFieldLabel.setLayoutData(fdCloseModelAndRemoveFieldLabel);

        wCloseModelAndRemoveFieldCheckbox =  new Button(group, SWT.CHECK);
        props.setLook(wCloseModelAndRemoveFieldCheckbox);
        wCloseModelAndRemoveFieldCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        final FormData fdCloseModelAndRemoveFieldCheckbox = new FormDataBuilder().left(wCloseModelAndRemoveFieldLabel, LABEL_SPACING)
                .top(wModelFieldCombo, ELEMENT_SPACING)
                .width(LARGE_FIELD)
                .result();
        wCloseModelAndRemoveFieldCheckbox.setLayoutData(fdCloseModelAndRemoveFieldCheckbox);

        //serialization format label/field
        wSerializationFormatLabel = new Label(group, SWT.LEFT);
        props.setLook(wSerializationFormatLabel);
        wSerializationFormatLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.TextFieldSerializationFormat"));
        FormData fdTransformation1 = new FormDataBuilder().left()
                .top(wCloseModelAndRemoveFieldLabel, ELEMENT_SPACING)
                .result();
        wSerializationFormatLabel.setLayoutData(fdTransformation1);

        wSerializationFormatCombo = new ComboVar(transMeta, group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wSerializationFormatCombo);
        FormData fdlTransformation1 = new FormDataBuilder().left()
                .top(wSerializationFormatLabel, LABEL_SPACING)
                .width(MEDIUM_FIELD)
                .result();
        wSerializationFormatCombo.setLayoutData(fdlTransformation1);

        //filename label/field/button
        wFilenameLabel = new Label(group, SWT.LEFT);
        props.setLook(wFilenameLabel);
        wFilenameLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.TextFieldFilename"));
        FormData fdTransformation2 = new FormDataBuilder().left()
                .top(wSerializationFormatCombo, ELEMENT_SPACING)
                .result();
        wFilenameLabel.setLayoutData(fdTransformation2);

        wFilenameTextField = new TextVar(transMeta, group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wFilenameTextField);
        FormData fdlTransformation2 = new FormDataBuilder().left()
                .top(wFilenameLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wFilenameTextField.setLayoutData(fdlTransformation2);

        wFilenameBrowseButton = new Button(group, SWT.PUSH);
        wFilenameBrowseButton.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.ButtonBrowse"));
        FormData fdBrowse = new FormDataBuilder().left(wFilenameTextField, LABEL_SPACING)
                .top(wFilenameLabel, LABEL_SPACING)
                .result();
        wFilenameBrowseButton.setLayoutData(fdBrowse);

        wBrowseFileDialog = new FileDialog(shell, SWT.SAVE);
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

        // create parent label/checkbox
        wCreateParentLabel = new Label(group, SWT.LEFT);
        props.setLook(wCreateParentLabel);
        wCreateParentLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.CheckboxCreateParentFolder"));
        FormData fdlTransformation3 = new FormDataBuilder().left()
                .top(wFilenameBrowseButton, ELEMENT_SPACING)
                .result();
        wCreateParentLabel.setLayoutData(fdlTransformation3);

        wCreateParentFolderCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wCreateParentFolderCheckbox);
        wCreateParentFolderCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        FormData fdTransformation3 = new FormDataBuilder().left(wCreateParentLabel, LABEL_SPACING)
                .top(wFilenameBrowseButton, ELEMENT_SPACING)
                .result();
        wCreateParentFolderCheckbox.setLayoutData(fdTransformation3);

        // include step number label/checkbox
        wIncludeStepNrLabel = new Label(group, SWT.LEFT);
        props.setLook(wIncludeStepNrLabel);
        wIncludeStepNrLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.CheckboxIncludeStepNr"));
        FormData fdlTransformation4 = new FormDataBuilder().left()
                .top(wCreateParentFolderCheckbox, ELEMENT_SPACING)
                .result();
        wIncludeStepNrLabel.setLayoutData(fdlTransformation4);

        wIncludeStepNrCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wIncludeStepNrCheckbox);
        wIncludeStepNrCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        FormData fdTransformation4 = new FormDataBuilder().left(wIncludeStepNrLabel, LABEL_SPACING)
                .top(wCreateParentFolderCheckbox, ELEMENT_SPACING)
                .result();
        wIncludeStepNrCheckbox.setLayoutData(fdTransformation4);

        // include partition number label/checkbox
        wIncludePartitionNrLabel = new Label(group, SWT.LEFT);
        props.setLook(wIncludePartitionNrLabel);
        wIncludePartitionNrLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.CheckboxIncludePartitionNr"));
        FormData fdlTransformation5 = new FormDataBuilder().left()
                .top(wIncludeStepNrCheckbox, ELEMENT_SPACING)
                .result();
        wIncludePartitionNrLabel.setLayoutData(fdlTransformation5);

        wIncludePartitionNrCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wIncludePartitionNrCheckbox);
        wIncludePartitionNrCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        FormData fdTransformation5 = new FormDataBuilder().left(wIncludePartitionNrLabel, LABEL_SPACING)
                .top(wIncludeStepNrCheckbox, ELEMENT_SPACING)
                .result();
        wIncludePartitionNrCheckbox.setLayoutData(fdTransformation5);

        // include date label/checkbox
        wIncludeDateLabel = new Label(group, SWT.LEFT);
        props.setLook(wIncludeDateLabel);
        wIncludeDateLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.CheckboxIncludeDate"));
        FormData fdlTransformation6 = new FormDataBuilder().left()
                .top(wIncludePartitionNrCheckbox, ELEMENT_SPACING)
                .result();
        wIncludeDateLabel.setLayoutData(fdlTransformation6);

        wIncludeDateCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wIncludeDateCheckbox);
        wIncludeDateCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        FormData fdTransformation6 = new FormDataBuilder().left(wIncludeDateLabel, LABEL_SPACING)
                .top(wIncludePartitionNrCheckbox, ELEMENT_SPACING)
                .result();
        wIncludeDateCheckbox.setLayoutData(fdTransformation6);

        // include time label/checkbox
        wIncludeTimeLabel = new Label(group, SWT.LEFT);
        props.setLook(wIncludeTimeLabel);
        wIncludeTimeLabel.setText(BaseMessages.getString(PKG, "JenaSerializerStepDialog.CheckboxIncludeTime"));
        FormData fdlTransformation7 = new FormDataBuilder().left()
                .top(wIncludeDateCheckbox, ELEMENT_SPACING)
                .result();
        wIncludeTimeLabel.setLayoutData(fdlTransformation7);

        wIncludeTimeCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wIncludeTimeCheckbox);
        wIncludeTimeCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        FormData fdTransformation7 = new FormDataBuilder().left(wIncludeTimeLabel, LABEL_SPACING)
                .top(wIncludeDateCheckbox, ELEMENT_SPACING)
                .result();
        wIncludeTimeCheckbox.setLayoutData(fdTransformation7);


        //Cancel, action and OK buttons for the bottom of the window.
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
        FormData fdCancel = new FormDataBuilder().right(100, -MARGIN_SIZE)
                .bottom()
                .result();
        wCancel.setLayoutData(fdCancel);

        wOK = new Button(shell, SWT.PUSH);
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

//        FormData fdTabFolder = new FormDataBuilder().fullWidth()
//                .top(group, MARGIN_SIZE)
//                .bottom()
//                .result();
//        wTabFolder.setLayoutData(fdTabFolder);

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
        lsGetField = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                getFieldsFromPrevious(wModelFieldCombo, transMeta, stepMeta);
                wModelFieldCombo.select(0);
            }
        };
        lsBrowseFilename = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                wFilenameTextField.setText(wBrowseFileDialog.open());
            }
        };
        lsCancel = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                cancel();
            }
        };
        lsOK = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                ok();
            }
        };

        wGetModelFieldButton.addListener(SWT.Selection, lsGetField);
        wFilenameBrowseButton.addListener(SWT.Selection, lsBrowseFilename);
        wOK.addListener(SWT.Selection, lsOK);
        wCancel.addListener(SWT.Selection, lsCancel);

        lsDef = new SelectionAdapter() {
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

    private void getData(final JenaSerializerStepMeta meta) {
        final String jenaModelField = meta.getJenaModelField();
        if (isNotEmpty(jenaModelField)) {
            wModelFieldCombo.setText(jenaModelField);
        }
        wCloseModelAndRemoveFieldCheckbox.setSelection(meta.isCloseModelAndRemoveField());

        wSerializationFormatCombo.removeAll();
        for (final String serializationFormat : Rdf11.SERIALIZATION_FORMATS) {
            wSerializationFormatCombo.add(serializationFormat);
        }
        // set selected
        String serializationFormat = meta.getSerializationFormat();
        if (isNullOrEmpty(serializationFormat)) {
            serializationFormat = Rdf11.DEFAULT_SERIALIZATION_FORMAT;
        }
        wSerializationFormatCombo.setText(serializationFormat);

        final JenaSerializerStepMeta.FileDetail fileDetail = meta.getFileDetail();
        if (fileDetail != null) {
            if (fileDetail.filename != null) {
                wFilenameTextField.setText(fileDetail.filename);
            }
            wCreateParentFolderCheckbox.setSelection(fileDetail.createParentFolder);
            wIncludeStepNrCheckbox.setSelection(fileDetail.includeStepNr);
            wIncludePartitionNrCheckbox.setSelection(fileDetail.includePartitionNr);
            wIncludeDateCheckbox.setSelection(fileDetail.includeDate);
            wIncludeTimeCheckbox.setSelection(fileDetail.includeTime);
        }
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

    private void cancel() {
        dispose();
    }

    private void ok() {

        // START save data
        meta.setJenaModelField(wModelFieldCombo.getText());
        meta.setCloseModelAndRemoveField(wCloseModelAndRemoveFieldCheckbox.getSelection());
        meta.setSerializationFormat(wSerializationFormatCombo.getText());

        JenaSerializerStepMeta.FileDetail fileDetail = meta.getFileDetail();
        if (fileDetail == null) {
            fileDetail = new JenaSerializerStepMeta.FileDetail();
        }
        fileDetail.filename = wFilenameTextField.getText();
        fileDetail.createParentFolder = wCreateParentFolderCheckbox.getSelection();
        fileDetail.includeStepNr = wIncludeStepNrCheckbox.getSelection();
        fileDetail.includePartitionNr = wIncludePartitionNrCheckbox.getSelection();
        fileDetail.includeDate = wIncludeDateCheckbox.getSelection();
        fileDetail.includeTime = wIncludeTimeCheckbox.getSelection();
        meta.setFileDetail(fileDetail);
        // END save data

        // NOTIFY CHANGE
        meta.setChanged(true);


        stepname = wStepNameField.getText();
        dispose();
    }
}
