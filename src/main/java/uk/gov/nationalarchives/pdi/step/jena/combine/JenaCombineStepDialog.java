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
package uk.gov.nationalarchives.pdi.step.jena.combine;

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
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.nationalarchives.pdi.step.jena.Util.isNotEmpty;
import static uk.gov.nationalarchives.pdi.step.jena.Util.isNullOrEmpty;

public class JenaCombineStepDialog extends BaseStepDialog implements StepDialogInterface {

    private static Class<?> PKG = JenaCombineStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    private static final int MARGIN_SIZE = 15;
    private static final int LABEL_SPACING = 5;
    private static final int ELEMENT_SPACING = 10;

    private static final int LARGE_FIELD = 350;
    private static final int MEDIUM_FIELD = 250;
    private static final int SMALL_FIELD = 75;

    private JenaCombineStepMeta meta;

    private ScrolledComposite scrolledComposite;
    private Composite contentComposite;
    private Label wStepNameLabel;
    private Text wStepNameField;
    private Label wMutateFirstModelLabel;
    private Button wMutateFirstModelCheckbox;
    private Label wTargetLabel;
    private TextVar wTargetTextField;
    private Label wFieldsLabel;
    private Label wRemoveSelectedLabel;
    private Button wRemoveSelectedCheckbox;
    private TableView wFieldsTableView;
    private Button wGetFieldsButton;
    private Button wCancel;
    private Button wOK;
    private Listener lsModifyFirstModel;
    private ModifyListener lsFieldsTableModify;
    private Listener lsGetFields;
    private Listener lsCancel;
    private Listener lsOK;
    private SelectionAdapter lsDef;
    private boolean changed;


    public JenaCombineStepDialog(final Shell parent, final Object in, final TransMeta tr, final String sname) {
        super(parent, (BaseStepMeta) in, tr, sname);
        meta = (JenaCombineStepMeta) in;
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

        lsFieldsTableModify = new ModifyListener() {
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
        shell.setText(BaseMessages.getString(PKG, "JenaCombineStepDialog.Shell.Title"));

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
        wStepNameLabel.setText(BaseMessages.getString(PKG, "JenaCombineStepDialog.Stepname.Label"));
        props.setLook(wStepNameLabel);
        final FormData fdStepNameLabel = new FormDataBuilder().left()
                .top()
                .result();
        wStepNameLabel.setLayoutData(fdStepNameLabel);

        wStepNameField = new Text(contentComposite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepNameField.setText(stepname);
        props.setLook(wStepNameField);
        wStepNameField.addModifyListener(lsFieldsTableModify);
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
        group.setText(BaseMessages.getString(PKG, "JenaCombineStepDialog.GroupText"));
        final FormLayout groupLayout = new FormLayout();
        groupLayout.marginWidth = MARGIN_SIZE;
        groupLayout.marginHeight = MARGIN_SIZE;
        group.setLayout(groupLayout);
        final FormData groupLayoutData = new FormDataBuilder().fullWidth()
                .top(topSpacer, MARGIN_SIZE)
                .result();
        group.setLayoutData(groupLayoutData);
        props.setLook(group);

        // mutate first model label/checkbox
        wMutateFirstModelLabel = new Label(group, SWT.LEFT);
        props.setLook(wMutateFirstModelLabel);
        wMutateFirstModelLabel.setText(BaseMessages.getString(PKG, "JenaCombineStepDialog.CheckboxMutateFirstModel"));
        final FormData fdMutateFirstModelLabel = new FormDataBuilder().left()
                .top()
                .result();
        wMutateFirstModelLabel.setLayoutData(fdMutateFirstModelLabel);

        wMutateFirstModelCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wMutateFirstModelCheckbox);
        wMutateFirstModelCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        final FormData fdMutateFirstModelCheckbox = new FormDataBuilder().left(wMutateFirstModelLabel, LABEL_SPACING)
                .top()
                .result();
        wMutateFirstModelCheckbox.setLayoutData(fdMutateFirstModelCheckbox);

        //target field name label/field
        wTargetLabel = new Label(group, SWT.LEFT);
        props.setLook(wTargetLabel);
        wTargetLabel.setText(BaseMessages.getString(PKG, "JenaCombineStepDialog.TextFieldTarget"));
        final FormData fdlTransformation0 = new FormDataBuilder().left()
                .top(wMutateFirstModelCheckbox, ELEMENT_SPACING)
                .result();
        wTargetLabel.setLayoutData(fdlTransformation0);

        wTargetTextField = new TextVar(transMeta, group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wTargetTextField);
        final FormData fdTransformation0 = new FormDataBuilder().left()
                .top(wTargetLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wTargetTextField.setLayoutData(fdTransformation0);

        // remove selected label/checkbox
        wRemoveSelectedLabel = new Label(group, SWT.LEFT);
        props.setLook(wRemoveSelectedLabel);
        wRemoveSelectedLabel.setText(BaseMessages.getString(PKG, "JenaCombineStepDialog.CheckboxRemoveSelected"));
        final FormData fdRemoveSelectedLabel = new FormDataBuilder().left()
                .top(wTargetTextField, ELEMENT_SPACING)
                .result();
        wRemoveSelectedLabel.setLayoutData(fdRemoveSelectedLabel);

        wRemoveSelectedCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wRemoveSelectedCheckbox);
        wRemoveSelectedCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        final FormData fdRemoveSelectedCheckbox = new FormDataBuilder().left(wRemoveSelectedLabel, LABEL_SPACING)
                .top(wTargetTextField, ELEMENT_SPACING)
                .result();
        wRemoveSelectedCheckbox.setLayoutData(fdRemoveSelectedCheckbox);

        wFieldsLabel = new Label(group, SWT.LEFT);
        props.setLook(wFieldsLabel);
        wFieldsLabel.setText(BaseMessages.getString(PKG, "JenaCombineStepDialog.Fields"));
        final FormData fdFieldsLabel = new FormDataBuilder().left()
                .top(wRemoveSelectedCheckbox, ELEMENT_SPACING)
                .result();
        wFieldsLabel.setLayoutData(fdFieldsLabel);

        final JenaCombineStepMeta.ActionIfNull[] actionsIfNull = JenaCombineStepMeta.ActionIfNull.values();
        final int actionsIfNullLen = actionsIfNull.length;
        final String[] actionIfNullNames = new String[actionsIfNullLen];
        for (int i = 0; i < actionsIfNullLen; i++) {
            actionIfNullNames[i] = actionsIfNull[i].name();
        }

        // fields table
        final ColumnInfo[] fieldsColumns = new ColumnInfo[] {
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaCombineStepDialog.Fieldname"),
                        ColumnInfo.COLUMN_TYPE_TEXT,
                        false
                ),
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaCombineStepDialog.IfNull"),
                        ColumnInfo.COLUMN_TYPE_CCOMBO,
                        actionIfNullNames
                )
        };

        wFieldsTableView = new TableView(
                transMeta, group, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, fieldsColumns,
                5, lsFieldsTableModify, props );
        props.setLook(wFieldsTableView);
        final FormData fdFieldsTableView = new FormDataBuilder().fullWidth()
                .top(wFieldsLabel, ELEMENT_SPACING)
                .result();
        wFieldsTableView.setLayoutData(fdFieldsTableView);

        //Table and buttons for the first tab
        wGetFieldsButton = new Button(group, SWT.PUSH);
        wGetFieldsButton.setText(BaseMessages.getString(PKG, "JenaCombineStepDialog.GetFieldsButton"));
        final FormData fdGetFieldsButton = new FormDataBuilder().right()
                .top(wFieldsTableView, ELEMENT_SPACING)
                .result();
        wGetFieldsButton.setLayoutData(fdGetFieldsButton);

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
        lsModifyFirstModel = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                // clear and enable/disable wTargetTextField
                wTargetTextField.setText("");
                wTargetTextField.setEnabled(!wMutateFirstModelCheckbox.getSelection());
            }
        };
        lsGetFields = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                //getFieldsFromPrevious(transMeta, meta, wTable, 0, new int[]{1}, new int[]{2}, 3, 4, null);

                getFieldsFromPrevious(transMeta, stepMeta, wFieldsTableView,
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

        wMutateFirstModelCheckbox.addListener(SWT.Selection, lsModifyFirstModel);
        wGetFieldsButton.addListener(SWT.Selection, lsGetFields);
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

    private void getData(final JenaCombineStepMeta meta) {
        wMutateFirstModelCheckbox.setSelection(meta.isMutateFirstModel());

        if (meta.isMutateFirstModel()) {
            wTargetTextField.setText("");
            wTargetTextField.setEnabled(false);
        } else {
            final String targetFieldName = meta.getTargetFieldName();
            if (targetFieldName != null) {
                wTargetTextField.setText(targetFieldName);
            }
            wTargetTextField.setEnabled(true);
        }

        wRemoveSelectedCheckbox.setSelection(meta.isRemoveSelectedFields());

        if (meta.getJenaModelFields() != null) {
            wFieldsTableView.getTable().removeAll();
            for (final JenaCombineStepMeta.JenaModelField jenaModelField : meta.getJenaModelFields()) {
                wFieldsTableView.add(new String[] { jenaModelField.fieldName, jenaModelField.actionIfNull.name() });
            }
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
        // SAVE DATA
        saveData();

        // NOTIFY CHANGE
        meta.setChanged(true);

        stepname = wStepNameField.getText();
        dispose();
    }

    private void saveData() {
        meta.setMutateFirstModel(wMutateFirstModelCheckbox.getSelection());
        meta.setTargetFieldName(wTargetTextField.getText());
        meta.setRemoveSelectedFields(wRemoveSelectedCheckbox.getSelection());

        final List<JenaCombineStepMeta.JenaModelField> jenaModelFields = new ArrayList<>();
        final int fieldsLen = wFieldsTableView.getItemCount();
        for (int i = 0; i < fieldsLen; i++) {
            final String fieldName = wFieldsTableView.getItem(i, 1);
            if (!isNullOrEmpty(fieldName)) {
                final String strActionIfNull = wFieldsTableView.getItem(i, 2);
                final JenaCombineStepMeta.ActionIfNull actionIfNull = isNotEmpty(strActionIfNull) ? JenaCombineStepMeta.ActionIfNull.valueOf(strActionIfNull) : JenaCombineStepMeta.ActionIfNull.ERROR;
                jenaModelFields.add(new JenaCombineStepMeta.JenaModelField(fieldName, actionIfNull));
            }
        }
        meta.setJenaModelFields(jenaModelFields);
    }
}