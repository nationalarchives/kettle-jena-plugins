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
package uk.gov.nationalarchives.pdi.step.jena.groupmerge;

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
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ComboValuesSelectionListener;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNoSuchField;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNull;
import uk.gov.nationalarchives.pdi.step.jena.ConstrainedField;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.nationalarchives.pdi.step.jena.Util.isNotEmpty;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;

public class JenaGroupMergeStepDialog extends BaseStepDialog implements StepDialogInterface {

    private static Class<?> PKG = JenaGroupMergeStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    private static final int MARGIN_SIZE = 15;
    private static final int LABEL_SPACING = 5;
    private static final int ELEMENT_SPACING = 10;

    private static final int LARGE_FIELD = 350;
    private static final int MEDIUM_FIELD = 250;
    private static final int SMALL_FIELD = 75;

    private JenaGroupMergeStepMeta meta;

    private ScrolledComposite scrolledComposite;
    private Composite contentComposite;
    private Label wStepNameLabel;
    private Text wStepNameField;
    private Label wGroupFieldsLabel;
    private TableView wGroupFieldsTableView;
    private Button wGetGroupFieldsButton;
    private Label wCloseMergedModelsLabel;
    private Button wCloseMergedModelsCheckbox;
    private Label wMergeFieldsLabel;
    private TableView wMergeFieldsTableView;
    private Button wGetMergeFieldsButton;
    private Label wOtherFieldsLabel;
    private Combo wOtherFieldsCombo;
    private Button wCancel;
    private Button wOK;
    private ModifyListener lsGroupFieldsTableModify;
    private ModifyListener lsMergeFieldsTableModify;
    private Listener lsGetGroupFields;
    private Listener lsGetMergeFields;
    private Listener lsCancel;
    private Listener lsOK;
    private SelectionAdapter lsDef;
    private boolean changed;


    public JenaGroupMergeStepDialog(final Shell parent, final Object in, final TransMeta transMeta, final String stepname) {
        super(parent, (BaseStepMeta) in, transMeta, stepname);
        meta = (JenaGroupMergeStepMeta) in;
    }

    @Override
    public String open() {
        //Set up window
        final Shell parent = getParent();
        final Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
        shell.setMinimumSize(450, 335);
        props.setLook(shell);
        setShellImage(shell, meta);

        lsGroupFieldsTableModify = new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                meta.setChanged();
            }
        };
        lsMergeFieldsTableModify = new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                meta.setChanged();
            }
        };
        changed = meta.hasChanged();

        //15 pixel margins
        final FormLayout formLayout = new FormLayout();
        formLayout.marginLeft = MARGIN_SIZE;
        formLayout.marginHeight = MARGIN_SIZE;
        shell.setLayout(formLayout);
        shell.setText(BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.Shell.Title"));

        //Build a scrolling composite and a composite for holding all content
        scrolledComposite = new ScrolledComposite(shell, SWT.V_SCROLL);
        contentComposite = new Composite(scrolledComposite, SWT.NONE);
        final FormLayout contentLayout = new FormLayout();
        contentLayout.marginRight = MARGIN_SIZE;
        contentComposite.setLayout(contentLayout);
        final FormData compositeLayoutData = new FormDataBuilder().fullSize()
                .result();
        contentComposite.setLayoutData(compositeLayoutData);
        props.setLook(contentComposite);

        //Step name label and text field
        wStepNameLabel = new Label(contentComposite, SWT.RIGHT);
        wStepNameLabel.setText(BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.Stepname.Label"));
        props.setLook(wStepNameLabel);
        final FormData fdStepNameLabel = new FormDataBuilder().left()
                .top()
                .result();
        wStepNameLabel.setLayoutData(fdStepNameLabel);

        wStepNameField = new Text(contentComposite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepNameField.setText(stepname);
        props.setLook(wStepNameField);
        wStepNameField.addModifyListener(lsGroupFieldsTableModify);
        wStepNameField.addModifyListener(lsMergeFieldsTableModify);
        final FormData fdStepName = new FormDataBuilder().left()
                .top(wStepNameLabel, LABEL_SPACING)
                .width(MEDIUM_FIELD)
                .result();
        wStepNameField.setLayoutData(fdStepName);

        //Job icon, centered vertically between the top of the label and the bottom of the field.
        final Label wicon = new Label(contentComposite, SWT.CENTER);
        wicon.setImage(getImage());
        final FormData fdIcon = new FormDataBuilder().right()
                .top(0, 4)
                .bottom(new FormAttachment(wStepNameField, 0, SWT.BOTTOM))
                .result();
        wicon.setLayoutData(fdIcon);
        props.setLook(wicon);

        //Spacer between entry info and content
        final Label topSpacer = new Label(contentComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
        final FormData fdSpacer = new FormDataBuilder().fullWidth()
                .top(wStepNameField, MARGIN_SIZE)
                .result();
        topSpacer.setLayoutData(fdSpacer);

        //Groups for first type of content
        final Group group = new Group(contentComposite, SWT.SHADOW_ETCHED_IN);
        group.setText(BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.GroupText"));
        final FormLayout groupLayout = new FormLayout();
        groupLayout.marginWidth = MARGIN_SIZE;
        groupLayout.marginHeight = MARGIN_SIZE;
        group.setLayout(groupLayout);
        final FormData groupLayoutData = new FormDataBuilder().fullWidth()
                .top(topSpacer, MARGIN_SIZE)
                .result();
        group.setLayoutData(groupLayoutData);
        props.setLook(group);

        // group fields label/table
        wGroupFieldsLabel = new Label(group, SWT.LEFT);
        props.setLook(wGroupFieldsLabel);
        wGroupFieldsLabel.setText(BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.GroupFields"));
        final FormData fdGroupFieldsLabel = new FormDataBuilder().left()
                .top()
                .result();
        wGroupFieldsLabel.setLayoutData(fdGroupFieldsLabel);

        final ColumnInfo[] groupFieldsColumns = new ColumnInfo[] {
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.Fieldname"),
                        ColumnInfo.COLUMN_TYPE_TEXT,
                        false
                ),
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.IfNoSuchField"),
                        ColumnInfo.COLUMN_TYPE_CCOMBO,
                        ActionIfNoSuchField.names()
                ),
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.IfNull"),
                        ColumnInfo.COLUMN_TYPE_CCOMBO,
                        ActionIfNull.names()
                )
        };

        wGroupFieldsTableView = new TableView(
                transMeta, group, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, groupFieldsColumns,
                5, lsGroupFieldsTableModify, props );
        props.setLook(wGroupFieldsTableView);
        final FormData fdGroupFieldsTableView = new FormDataBuilder().fullWidth()
                .top(wGroupFieldsLabel, ELEMENT_SPACING)
                .result();
        wGroupFieldsTableView.setLayoutData(fdGroupFieldsTableView);

        wGetGroupFieldsButton = new Button(group, SWT.PUSH);
        wGetGroupFieldsButton.setText(BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.GetFieldsButton"));
        final FormData fdGetGroupFieldsButton = new FormDataBuilder().right()
                .top(wGroupFieldsTableView, ELEMENT_SPACING)
                .result();
        wGetGroupFieldsButton.setLayoutData(fdGetGroupFieldsButton);

        // merge fields label/table
        wMergeFieldsLabel = new Label(group, SWT.LEFT);
        props.setLook(wMergeFieldsLabel);
        wMergeFieldsLabel.setText(BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.MergeFields"));
        final FormData fdMergeFieldsLabel = new FormDataBuilder().left()
                .top(wGetGroupFieldsButton, ELEMENT_SPACING)
                .result();
        wMergeFieldsLabel.setLayoutData(fdMergeFieldsLabel);

        final ColumnInfo ciMutateFirstModel = new ColumnInfo(
                BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.CheckboxMutateFirstModel"),
                ColumnInfo.COLUMN_TYPE_CCOMBO,
                MutateFirstModel.labels()
        );

        final ColumnInfo ciTargetField = new ColumnInfo(
                BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.TextFieldTarget"),
                ColumnInfo.COLUMN_TYPE_TEXT,
                false
        );

        ciMutateFirstModel.setComboValuesSelectionListener(new ComboValuesSelectionListener() {
            @Override
            public String[] getComboValues(final TableItem tableItem, final int rowNr, final int colNr) {
                final String mutateFirstModelLabel = tableItem.getText(colNr);
                final MutateFirstModel mutateFirstModel = MutateFirstModel.fromLabel(mutateFirstModelLabel);

                if (MutateFirstModel.YES == mutateFirstModel) {
                    tableItem.setText(colNr + 1, "");
                    //targetFieldTableItem.setEnabled(false);
                    ciTargetField.setReadOnly(true);
                } else {
                    //targetFieldTableItem.setEnabled(true);
                    ciTargetField.setReadOnly(false);
                }

                return MutateFirstModel.labels();
            }
        });

        final ColumnInfo[] mergeFieldsColumns = new ColumnInfo[] {
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.Fieldname"),
                        ColumnInfo.COLUMN_TYPE_TEXT,
                        false
                ),
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.IfNoSuchField"),
                        ColumnInfo.COLUMN_TYPE_CCOMBO,
                        ActionIfNoSuchField.names()
                ),
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.IfNull"),
                        ColumnInfo.COLUMN_TYPE_CCOMBO,
                        ActionIfNull.names()
                ),
                ciMutateFirstModel,
                ciTargetField
        };

        wMergeFieldsTableView = new TableView(
                transMeta, group, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, mergeFieldsColumns,
                5, lsMergeFieldsTableModify, props );
        props.setLook(wMergeFieldsTableView);
        final FormData fdMergeFieldsTableView = new FormDataBuilder().fullWidth()
                .top(wMergeFieldsLabel, ELEMENT_SPACING)
                .result();
        wMergeFieldsTableView.setLayoutData(fdMergeFieldsTableView);

        //Table and buttons for the first tab
        wGetMergeFieldsButton = new Button(group, SWT.PUSH);
        wGetMergeFieldsButton.setText(BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.GetFieldsButton"));
        final FormData fdGetMergeFieldsButton = new FormDataBuilder().right()
                .top(wMergeFieldsTableView, ELEMENT_SPACING)
                .result();
        wGetMergeFieldsButton.setLayoutData(fdGetMergeFieldsButton);

        // remove selected label/checkbox
        wCloseMergedModelsLabel = new Label(group, SWT.LEFT);
        props.setLook(wCloseMergedModelsLabel);
        wCloseMergedModelsLabel.setText(BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.CheckboxCloseMergedModels"));
        final FormData fdCloseMergedModelsLabel = new FormDataBuilder().left()
                .top(wGetMergeFieldsButton, ELEMENT_SPACING)
                .result();
        wCloseMergedModelsLabel.setLayoutData(fdCloseMergedModelsLabel);

        wCloseMergedModelsCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wCloseMergedModelsCheckbox);
        wCloseMergedModelsCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        final FormData fdMergeClosedModelsCheckbox = new FormDataBuilder().left(wCloseMergedModelsLabel, LABEL_SPACING)
                .top(wGetMergeFieldsButton, ELEMENT_SPACING)
                .result();
        wCloseMergedModelsCheckbox.setLayoutData(fdMergeClosedModelsCheckbox);

        // Other fields
        wOtherFieldsLabel = new Label(group, SWT.LEFT);
        props.setLook(wOtherFieldsLabel);
        wOtherFieldsLabel.setText(BaseMessages.getString(PKG, "JenaGroupMergeStepDialog.TextFieldOtherFields"));
        final FormData fdOtherFieldsLabel = new FormDataBuilder().left()
                .top(wCloseMergedModelsLabel, ELEMENT_SPACING)
                .result();
        wOtherFieldsLabel.setLayoutData(fdOtherFieldsLabel);

        wOtherFieldsCombo = new Combo(group, SWT.SINGLE | SWT.LEFT | SWT.BORDER | SWT.READ_ONLY);
        props.setLook(wOtherFieldsCombo);
        final FormData fdOtherFieldsCombo = new FormDataBuilder().left()
                .top(wOtherFieldsLabel, LABEL_SPACING)
                .width(MEDIUM_FIELD)
                .result();
        wOtherFieldsCombo.setLayoutData(fdOtherFieldsCombo);

        //Cancel and OK buttons for the bottom of the window.
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
        final FormData fdCancel = new FormDataBuilder().right(100, -MARGIN_SIZE)
                .bottom()
                .result();
        wCancel.setLayoutData(fdCancel);

        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
        final FormData fdOk = new FormDataBuilder().right(wCancel, -LABEL_SPACING)
                .bottom()
                .result();
        wOK.setLayoutData(fdOk);

        //Space between bottom buttons and the table, final layout for table
        final Label bottomSpacer = new Label(shell, SWT.HORIZONTAL | SWT.SEPARATOR);
        final FormData fdhSpacer = new FormDataBuilder().left()
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
        final FormData fdScrolledComposite = new FormDataBuilder().fullWidth()
                .top()
                .bottom(bottomSpacer, -MARGIN_SIZE * 4)
                .result();
        scrolledComposite.setLayoutData(fdScrolledComposite);
        props.setLook(scrolledComposite);

        // Listeners
        lsGetGroupFields = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                //getFieldsFromPrevious(transMeta, meta, wTable, 0, new int[]{1}, new int[]{2}, 3, 4, null);

                getFieldsFromPrevious(transMeta, stepMeta, wGroupFieldsTableView,
                        1,
                        new int[]{1},
                        new int[]{},
                        -1,
                        -1,
                        null);
            }
        };
        lsGetMergeFields = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                //getFieldsFromPrevious(transMeta, meta, wTable, 0, new int[]{1}, new int[]{2}, 3, 4, null);

                getFieldsFromPrevious(transMeta, stepMeta, wMergeFieldsTableView,
                        1,
                        new int[]{1},
                        new int[]{},
                        -1,
                        -1,
                        null);
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
        lsDef = new SelectionAdapter() {
            public void widgetDefaultSelected(final SelectionEvent e) {
                ok();
            }
        };

        wGetGroupFieldsButton.addListener(SWT.Selection, lsGetGroupFields);
        wGetMergeFieldsButton.addListener(SWT.Selection, lsGetMergeFields);
        wOK.addListener(SWT.Selection, lsOK);
        wCancel.addListener(SWT.Selection, lsCancel);
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

    private void getData(final JenaGroupMergeStepMeta meta) {
        wCloseMergedModelsCheckbox.setSelection(meta.isCloseMergedModels());

        if (meta.getGroupFields() != null) {
            wGroupFieldsTableView.getTable().removeAll();
            for (final ConstrainedField groupField : meta.getGroupFields()) {
                wGroupFieldsTableView.add(new String[] {
                        groupField.fieldName,
                        groupField.actionIfNoSuchField.name(),
                        groupField.actionIfNull.name()
                });
            }
        }

        if (meta.getJenaModelMergeFields() != null) {
            wMergeFieldsTableView.getTable().removeAll();
            for (final ModelMergeConstrainedField jenaModelField : meta.getJenaModelMergeFields()) {
                wMergeFieldsTableView.add(new String[] {
                        jenaModelField.fieldName,
                        jenaModelField.actionIfNoSuchField.name(),
                        jenaModelField.actionIfNull.name(),
                        jenaModelField.mutateFirstModel.getLabel(),
                        isNotEmpty(jenaModelField.targetFieldName) ? jenaModelField.targetFieldName : ""
                });

                //TODO(AR) enable disable the targetField depending on the value of mutateFirstModel
//                wTargetTextField.setEnabled(false);
            }
        }

        wOtherFieldsCombo.removeAll();
        for (final String serializationFormat : OtherFieldAction.labels()) {
            wOtherFieldsCombo.add(serializationFormat);
        }
        // set selected
        OtherFieldAction otherFieldAction = meta.getOtherFieldAction();
        if (otherFieldAction == null) {
            otherFieldAction = OtherFieldAction.DROP;
        }
        wOtherFieldsCombo.setText(otherFieldAction.getLabel());
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
        // SAVE DATA
        saveData();

        // NOTIFY CHANGE
        meta.setChanged(true);

        stepname = wStepNameField.getText();
        dispose();
    }

    private void saveData() {
        meta.setCloseMergedModels(wCloseMergedModelsCheckbox.getSelection());

        final int groupFieldsLen = wGroupFieldsTableView.getItemCount();
        final List<ConstrainedField> groupFields = new ArrayList<>(groupFieldsLen);
        for (int i = 0; i < groupFieldsLen; i++) {
            final String fieldName = wGroupFieldsTableView.getItem(i, 1);
            if (!isNullOrEmpty(fieldName)) {
                final String strActionIfNoSuchField = wGroupFieldsTableView.getItem(i, 2);
                final ActionIfNoSuchField actionIfNoSuchField = isNotEmpty(strActionIfNoSuchField) ? ActionIfNoSuchField.valueOf(strActionIfNoSuchField) : ActionIfNoSuchField.ERROR;
                final String strActionIfNull = wGroupFieldsTableView.getItem(i, 3);
                final ActionIfNull actionIfNull = isNotEmpty(strActionIfNull) ? ActionIfNull.valueOf(strActionIfNull) : ActionIfNull.ERROR;

                groupFields.add(new ConstrainedField(fieldName, actionIfNoSuchField, actionIfNull));
            }
        }
        meta.setGroupFields(groupFields);

        final int mergeFieldsLen = wMergeFieldsTableView.getItemCount();
        final List<ModelMergeConstrainedField> jenaModelFields = new ArrayList<>(mergeFieldsLen);
        for (int i = 0; i < mergeFieldsLen; i++) {
            final String fieldName = wMergeFieldsTableView.getItem(i, 1);
            if (!isNullOrEmpty(fieldName)) {
                final String strActionIfNoSuchField = wMergeFieldsTableView.getItem(i, 2);
                final ActionIfNoSuchField actionIfNoSuchField = isNotEmpty(strActionIfNoSuchField) ? ActionIfNoSuchField.valueOf(strActionIfNoSuchField) : ActionIfNoSuchField.ERROR;
                final String strActionIfNull = wMergeFieldsTableView.getItem(i, 3);
                final ActionIfNull actionIfNull = isNotEmpty(strActionIfNull) ? ActionIfNull.valueOf(strActionIfNull) : ActionIfNull.ERROR;
                final String strMutateFirstModel = wMergeFieldsTableView.getItem(i, 4);
                final MutateFirstModel mutateFirstModel = isNotEmpty(strMutateFirstModel) ? MutateFirstModel.fromLabel(strMutateFirstModel) : MutateFirstModel.YES;
                final String targetField;
                if (mutateFirstModel == MutateFirstModel.YES) {
                     targetField = null;
                } else {
                    final String strTargetField = wMergeFieldsTableView.getItem(i, 5);
                    targetField = isNotEmpty(strTargetField) ? strTargetField : null;
                }
                jenaModelFields.add(new ModelMergeConstrainedField(fieldName, actionIfNoSuchField, actionIfNull, mutateFirstModel, targetField));
            }
        }
        meta.setJenaModelFields(jenaModelFields);

        final OtherFieldAction otherFieldAction = OtherFieldAction.fromLabel(wOtherFieldsCombo.getText());
        meta.setOtherFieldAction(otherFieldAction);
    }
}