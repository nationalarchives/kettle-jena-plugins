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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Props;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.evolvedbinary.pdi.Util.*;

public class JenaModelStepDialog extends BaseStepDialog implements StepDialogInterface {

    private static Class<?> PKG = JenaModelStepMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    private static final int MARGIN_SIZE = 15;
    private static final int LABEL_SPACING = 5;
    private static final int ELEMENT_SPACING = 10;

    private static final int LARGE_FIELD = 350;
    private static final int MEDIUM_FIELD = 250;
    private static final int SMALL_FIELD = 75;

    private JenaModelStepMeta meta;

    private ScrolledComposite scrolledComposite;
    private Composite contentComposite;
    private Label wStepNameLabel;
    private Text wStepNameField;
    private Label wTargetLabel;
    private TextVar wTargetTextField;
    private Label wResourceTypeLabel;
    private TextVar wResourceTypeTextField;
    private Label wRemoveLabel;
    private Button wRemoveSelectedCheckbox;
    private Button wRadioButton1;
    private Button wRadioButton2;
    private Button wCheckbox1;
    private Button wCheckbox2;
    //private Table wTable;
    private TableView wNamespacesTableView;
    private TableView wMappingsTableView;
    private Button wTableButton;
    private Button wCancel;
    private Button wAction;
    private Button wOK;
    private ModifyListener lsMod;
    private Listener lsGetFields;
    private Listener lsCancel;
    private Listener lsOK;
    private SelectionAdapter lsDef;
    private boolean changed;

    public JenaModelStepDialog(Shell parent, Object in, TransMeta tr, String sname) {
        super(parent, (BaseStepMeta) in, tr, sname);
        meta = (JenaModelStepMeta) in;
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
        shell.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.Shell.Title"));

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
        wStepNameLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.Stepname.Label"));
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
        group.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.GroupText"));
        FormLayout groupLayout = new FormLayout();
        groupLayout.marginWidth = MARGIN_SIZE;
        groupLayout.marginHeight = MARGIN_SIZE;
        group.setLayout(groupLayout);
        FormData groupLayoutData = new FormDataBuilder().fullWidth()
                .top(topSpacer, MARGIN_SIZE)
                .result();
        group.setLayoutData(groupLayoutData);
        props.setLook(group);

        //target field name label/field
        wTargetLabel = new Label(group, SWT.LEFT);
        props.setLook(wTargetLabel);
        wTargetLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.TextFieldTarget"));
        FormData fdlTransformation = new FormDataBuilder().left()
                .top()
                .result();
        wTargetLabel.setLayoutData(fdlTransformation);

        wTargetTextField = new TextVar(transMeta, group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wTargetTextField);
        FormData fdTransformation = new FormDataBuilder().left()
                .top(wTargetLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wTargetTextField.setLayoutData(fdTransformation);

        wRemoveSelectedCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wRemoveSelectedCheckbox);
        wRemoveSelectedCheckbox.setOrientation(SWT.RIGHT_TO_LEFT);
        wRemoveSelectedCheckbox.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.CheckboxRemoveSelected"));
        wRemoveSelectedCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        FormData fdTransformation2 = new FormDataBuilder().left()
                .top(wTargetTextField, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wRemoveSelectedCheckbox.setLayoutData(fdTransformation2);

        //resource rdf:type label/field
        wResourceTypeLabel = new Label(group, SWT.LEFT);
        props.setLook(wResourceTypeLabel);
        wResourceTypeLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.TextFieldResourceType"));
        FormData fdlTransformation3 = new FormDataBuilder().left()
                //.top(wTargetTextField, ELEMENT_SPACING)
                .top(wRemoveSelectedCheckbox, ELEMENT_SPACING)
                .result();
        wResourceTypeLabel.setLayoutData(fdlTransformation3);

        wResourceTypeTextField = new TextVar(transMeta, group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wResourceTypeTextField);
        FormData fdTransformation3 = new FormDataBuilder().left()
                .top(wResourceTypeLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wResourceTypeTextField.setLayoutData(fdTransformation3);

        ColumnInfo[] namespacesColumns = new ColumnInfo[] {
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaModelStepDialog.Namespace.Prefix"),
                        ColumnInfo.COLUMN_TYPE_TEXT,
                        false
                ),
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaModelStepDialog.Namespace.URI"),
                        ColumnInfo.COLUMN_TYPE_TEXT,
                        false
                )
        };

        wNamespacesTableView = new TableView(
                transMeta, group, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, namespacesColumns,
                5, lsMod, props );
        props.setLook(wNamespacesTableView);
        FormData fdTable = new FormDataBuilder().fullWidth()
                .top(wResourceTypeTextField, ELEMENT_SPACING)
                .result();
        wNamespacesTableView.setLayoutData(fdTable);

        //Tabs
        CTabFolder wTabFolder = new CTabFolder(contentComposite, SWT.BORDER);
        props.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

//        CTabItem wTab1 = new CTabItem(wTabFolder, SWT.NONE);
//        wTab1.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.Tab1"));
//        Composite wTab1Contents = new Composite(wTabFolder, SWT.SHADOW_NONE);
//        props.setLook(wTab1Contents);
//        FormLayout tab1Layout = new FormLayout();
//        tab1Layout.marginWidth = MARGIN_SIZE;
//        tab1Layout.marginHeight = MARGIN_SIZE;
//        wTab1Contents.setLayout(tab1Layout);
//        FormData fdTab1 = new FormDataBuilder().fullSize()
//                .result();
//        wTab1Contents.setLayoutData(fdTab1);
//        wTab1.setControl(wTab1Contents);

        CTabItem wTab2 = new CTabItem(wTabFolder, SWT.NONE);
        wTab2.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.TabMappings"));
        Composite wTab2Contents = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wTab2Contents);
        FormLayout tab2Layout = new FormLayout();
        tab2Layout.marginWidth = MARGIN_SIZE;
        tab2Layout.marginHeight = MARGIN_SIZE;
        wTab2Contents.setLayout(tab2Layout);
        FormData fdTab2 = new FormDataBuilder().fullSize()
                .result();
        wTab2Contents.setLayoutData(fdTab2);
        wTab2.setControl(wTab2Contents);

        wTabFolder.setSelection(0);

//        //Radio buttons and checkboxes for the first tab
//        wRadioButton1 = new Button(wTab1Contents, SWT.RADIO);
//        wRadioButton1.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.RadioButton1"));
//        wRadioButton1.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
//        FormData fdRadioButton1 = new FormDataBuilder().left()
//                .top()
//                .result();
//        wRadioButton1.setLayoutData(fdRadioButton1);
//
//        wRadioButton2 = new Button(wTab1Contents, SWT.RADIO);
//        wRadioButton2.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.RadioButton2"));
//        wRadioButton2.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
//        FormData fdRadioButton2 = new FormDataBuilder().left()
//                .top(wRadioButton1, ELEMENT_SPACING)
//                .result();
//        wRadioButton2.setLayoutData(fdRadioButton2);
//
//        wCheckbox1 = new Button(wTab1Contents, SWT.CHECK);
//        wCheckbox1.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.Checkbox1"));
//        wCheckbox1.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
//        FormData fdCheck1 = new FormDataBuilder().left(wRadioButton1, MARGIN_SIZE)
//                .top()
//                .result();
//        wCheckbox1.setLayoutData(fdCheck1);
//
//        wCheckbox2 = new Button(wTab1Contents, SWT.CHECK);
//        wCheckbox2.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.Checkbox2"));
//        wCheckbox2.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
//        FormData fdCheck2 = new FormDataBuilder().left(wRadioButton1, MARGIN_SIZE)
//                .top(wCheckbox1, ELEMENT_SPACING)
//                .result();
//        wCheckbox2.setLayoutData(fdCheck2);

        //Table and button for the second tab
        wTableButton = new Button(wTab2Contents, SWT.PUSH);
        wTableButton.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.Button"));
        FormData fdTableButton = new FormDataBuilder().right()
                .bottom()
                .result();
        wTableButton.setLayoutData(fdTableButton);


        ColumnInfo[] mappingsColumns = new ColumnInfo[] {
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaModelStepDialog.Fieldname"),
                        ColumnInfo.COLUMN_TYPE_TEXT,
                        false
                ),
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaModelStepDialog.RdfPropertyName"),
                        ColumnInfo.COLUMN_TYPE_TEXT,
                        false
                ),
                new ColumnInfo(
                        BaseMessages.getString(PKG, "JenaModelStepDialog.RdfPropertyType"),
                        ColumnInfo.COLUMN_TYPE_CCOMBO,
                        Rdf11.DATA_TYPES  // combo-box options
                ),
        };

        wMappingsTableView = new TableView(
                        transMeta, wTab2Contents, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, mappingsColumns,
                        10, lsMod, props );
        FormData fdTableMappings = new FormDataBuilder().fullWidth()
                .top()
                .bottom(wTableButton, -ELEMENT_SPACING)
                .result();
        wMappingsTableView.setLayoutData(fdTableMappings);
        //wTableView.setItemCount(5);


        //wTable = new Table(wTab2Contents, SWT.MULTI | SWT.BORDER | SWT.NO_SCROLL);
//        wTable.setHeaderVisible(true);
//        wTable.setLinesVisible(true);
//        FormData fdTable = new FormDataBuilder().fullWidth()
//                .top()
//                .bottom(wTableButton, -ELEMENT_SPACING)
//                .result();
//        wTable.setLayoutData(fdTable);
//        wTable.setItemCount(5);

//        int numColumns = 3;
//        for (int i = 0; i < numColumns; i++) {
//            TableColumn col = new TableColumn(wTable, SWT.NONE);
//            col.setResizable(false);
//            col.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.TableHeader") + " " + (i + 1));
//        }
//        wTableView.addControlListener(new ControlAdapter() {
//            @Override
//            public void controlResized(ControlEvent controlEvent) {
//                int tableWidth = wTableView.getSize().x;
//                int numColumns = wTableView.table.getColumnCount();
//                for (ColumnInfo col : wTableView.getColumns()) {
//                    col.width(tableWidth / numColumns);
//                }
//            }
//        });

        //Cancel, action and OK buttons for the bottom of the window.
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
        FormData fdCancel = new FormDataBuilder().right(100, -MARGIN_SIZE)
                .bottom()
                .result();
        wCancel.setLayoutData(fdCancel);

        wAction = new Button(shell, SWT.PUSH);
        wAction.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.ActionButton"));
        int actionButtonWidth = wAction.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        FormData fdAction = new FormDataBuilder().right(50, actionButtonWidth / 2)
                .bottom()
                .result();
        wAction.setLayoutData(fdAction);

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

        FormData fdTabFolder = new FormDataBuilder().fullWidth()
                .top(group, MARGIN_SIZE)
                .bottom()
                .result();
        wTabFolder.setLayoutData(fdTabFolder);

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
        lsGetFields = new Listener() {
          public void handleEvent(Event e) {
              //getFieldsFromPrevious(transMeta, meta, wTable, 0, new int[]{1}, new int[]{2}, 3, 4, null);

              getFieldsFromPrevious(transMeta, stepMeta, wMappingsTableView,
              1,
              new int[]{1},
              new int[]{},
              -1,
              -1,
              null);
          }
        };
        lsCancel = new Listener() {
            public void handleEvent(Event e) {
                cancel();
            }
        };
        lsOK = new Listener() {
            public void handleEvent(Event e) {
                ok();
            }
        };

        wTableButton.addListener(SWT.Selection, lsGetFields);
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

    private void getData(final JenaModelStepMeta meta) {
        final String targetFieldName = meta.getTargetFieldName();
        if (targetFieldName != null) {
            wTargetTextField.setText(targetFieldName);
        }

        wRemoveSelectedCheckbox.setSelection(meta.isRemoveSelectedFields());

        final String resourceType = meta.getResourceType();
        if (resourceType != null) {
            wResourceTypeTextField.setText(resourceType);
        }

        if (meta.getNamespaces() != null) {
            wNamespacesTableView.getTable().removeAll();
            for (final Map.Entry<String, String> namespace : meta.getNamespaces().entrySet()) {
                wNamespacesTableView.add(new String[]{namespace.getKey(), namespace.getValue()});
            }
        }

        final JenaModelStepMeta.DbToJenaMapping[] dbToJenaMappings = meta.getDbToJenaMappings();
        if (dbToJenaMappings != null) {
            wMappingsTableView.getTable().removeAll();
            for (final JenaModelStepMeta.DbToJenaMapping dbToJenaMapping : dbToJenaMappings) {
                wMappingsTableView.add(new String[] {
                        dbToJenaMapping.fieldName,
                        asPrefixString(dbToJenaMapping.rdfPropertyName),
                        asPrefixString(dbToJenaMapping.rdfType)
                });
            }
        }
    }

    private Image getImage() {
        PluginInterface plugin =
                PluginRegistry.getInstance().getPlugin(StepPluginType.class, stepMeta.getStepMetaInterface());
        String id = plugin.getIds()[0];
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
        meta.setTargetFieldName(wTargetTextField.getText());
        meta.setRemoveSelectedFields(wRemoveSelectedCheckbox.getSelection());
        meta.setResourceType(wResourceTypeTextField.getText());

        final Map<String, String> namespaces = new LinkedHashMap<>();
        final int namespacesLen = wNamespacesTableView.getItemCount();
        for (int i = 0; i < namespacesLen; i++) {
            final String prefix = wNamespacesTableView.getItem(i, 1);
            final String uri = wNamespacesTableView.getItem(i, 2);
            if (prefix != null && uri != null) {
                namespaces.put(prefix, uri);
            }
        }
        meta.setNamespaces(namespaces);

        final int mappingsLen = wMappingsTableView.getItemCount();
        JenaModelStepMeta.DbToJenaMapping mappings[] = new JenaModelStepMeta.DbToJenaMapping[mappingsLen];
        int mappingsCount = 0;
        for (int i = 0; i < mappingsLen; i++) {
            final String fieldName = wMappingsTableView.getItem(i, 1);
            if (fieldName == null || fieldName.isEmpty()) {
                continue;
            }

            final JenaModelStepMeta.DbToJenaMapping mapping = new JenaModelStepMeta.DbToJenaMapping();
            mapping.fieldName = fieldName;
            final String propertyName = wMappingsTableView.getItem(i, 2);
            mapping.rdfPropertyName = parseQName(namespaces, propertyName);
            final String rdfType = nullIfEmpty(wMappingsTableView.getItem(i, 3));
            mapping.rdfType = parseQName(namespaces, rdfType);
            mappings[mappingsCount++] = mapping;
        }

        if (mappingsCount < mappingsLen) {
            mappings = Arrays.copyOf(mappings, mappingsCount);
        }

        meta.setDbToJenaMappings(mappings);
        // END save data

        // NOTIFY CHANGE
        meta.setChanged(true);


        stepname = wStepNameField.getText();
        dispose();
    }
}