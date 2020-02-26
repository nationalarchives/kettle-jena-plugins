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
package uk.gov.nationalarchives.pdi.step.jena.model;

import uk.gov.nationalarchives.pdi.step.jena.Rdf11;
import uk.gov.nationalarchives.pdi.step.jena.Util;
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
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private Label wResourceUriLabel;
    private ComboVar wResourceUriCombo;
    private Button wGetUriFieldButton;
    private Label wRemoveSelectedLabel;
    private Button wRemoveSelectedCheckbox;
    private TableView wNamespacesTableView;
    private TableView wMappingsTableView;
    private Button wTableButton;
    private Button wCancel;
    private Button wOK;
    private ModifyListener lsMod;
    private Listener lsGetField;
    private Listener lsGetFields;
    private Listener lsCancel;
    private Listener lsOK;
    private SelectionAdapter lsDef;
    private boolean changed;

    public JenaModelStepDialog(final Shell parent, final Object in, final TransMeta tr, final String sname) {
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
        FormData fdlTransformation0 = new FormDataBuilder().left()
                .top()
                .result();
        wTargetLabel.setLayoutData(fdlTransformation0);

        wTargetTextField = new TextVar(transMeta, group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wTargetTextField);
        FormData fdTransformation0 = new FormDataBuilder().left()
                .top(wTargetLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wTargetTextField.setLayoutData(fdTransformation0);

        // remove selected label/checkbox
        wRemoveSelectedLabel = new Label(group, SWT.LEFT);
        props.setLook(wRemoveSelectedLabel);
        wRemoveSelectedLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.CheckboxRemoveSelected"));
        FormData fdlTransformation2 = new FormDataBuilder().left()
                .top(wTargetTextField, ELEMENT_SPACING)
                .result();
        wRemoveSelectedLabel.setLayoutData(fdlTransformation2);

        wRemoveSelectedCheckbox = new Button(group, SWT.CHECK);
        props.setLook(wRemoveSelectedCheckbox);
        wRemoveSelectedCheckbox.setBackground(display.getSystemColor(SWT.COLOR_TRANSPARENT));
        FormData fdTransformation2 = new FormDataBuilder().left(wRemoveSelectedLabel, LABEL_SPACING)
                .top(wTargetTextField, ELEMENT_SPACING)
                .result();
        wRemoveSelectedCheckbox.setLayoutData(fdTransformation2);

        // namespaces table
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
                .top(wRemoveSelectedCheckbox, ELEMENT_SPACING)
                .result();
        wNamespacesTableView.setLayoutData(fdTable);

        //resource rdf:type label/field
        wResourceTypeLabel = new Label(group, SWT.LEFT);
        props.setLook(wResourceTypeLabel);
        wResourceTypeLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.TextFieldResourceType"));
        FormData fdlTransformation3 = new FormDataBuilder().left()
                //.top(wTargetTextField, ELEMENT_SPACING)
                .top(wNamespacesTableView, ELEMENT_SPACING)
                .result();
        wResourceTypeLabel.setLayoutData(fdlTransformation3);

        wResourceTypeTextField = new TextVar(transMeta, group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wResourceTypeTextField);
        FormData fdTransformation3 = new FormDataBuilder().left()
                .top(wResourceTypeLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wResourceTypeTextField.setLayoutData(fdTransformation3);

        //resource URI label/field
        wResourceUriLabel = new Label(group, SWT.LEFT);
        props.setLook(wResourceUriLabel);
        wResourceUriLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.TextFieldResourceUri"));
        FormData fdlTransformation4 = new FormDataBuilder().left()
                .top(wResourceTypeTextField, ELEMENT_SPACING)
                .result();
        wResourceUriLabel.setLayoutData(fdlTransformation4);

        wResourceUriCombo = new ComboVar(transMeta, group,  SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wResourceUriCombo);
        FormData fdTransformation4 = new FormDataBuilder().left()
                .top(wResourceUriLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wResourceUriCombo.setLayoutData(fdTransformation4);

        wGetUriFieldButton = new Button(group, SWT.PUSH);
        wGetUriFieldButton.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.GetFieldsButton"));
        FormData fdGetField = new FormDataBuilder().left(wResourceUriCombo, LABEL_SPACING)
                .top(wResourceUriLabel, LABEL_SPACING)
                .result();
        wGetUriFieldButton.setLayoutData(fdGetField);

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
        wTableButton.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.GetFieldsButton"));
        FormData fdTableButton = new FormDataBuilder().right()
                .bottom()
                .result();
        wTableButton.setLayoutData(fdTableButton);

        final String[] propertyTypes = new String[Rdf11.DATA_TYPES.length + 1];
        propertyTypes[0] = "Resource";
        System.arraycopy(Rdf11.DATA_TYPES, 0, propertyTypes, 1, Rdf11.DATA_TYPES.length);

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
                        propertyTypes  // combo-box options
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
        lsGetField = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                getFieldsFromPrevious(wResourceUriCombo, transMeta, stepMeta);
                wResourceUriCombo.select(0);
            }
        };

        lsGetFields = new Listener() {
        @Override
          public void handleEvent(final Event e) {
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

        wGetUriFieldButton.addListener(SWT.Selection, lsGetField);
        wTableButton.addListener(SWT.Selection, lsGetFields);
        wOK.addListener(SWT.Selection, lsOK);
        wCancel.addListener(SWT.Selection, lsCancel);

        lsDef = new SelectionAdapter() {
            public void widgetDefaultSelected(final SelectionEvent e) {
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

        final String resourceUriField = meta.getResourceUriField();
        if (resourceUriField != null) {
            wResourceUriCombo.setText(resourceUriField);
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
                        Util.asPrefixString(dbToJenaMapping.rdfPropertyName),
                        Util.asPrefixString(dbToJenaMapping.rdfType)
                });
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

        // START save data
        meta.setTargetFieldName(wTargetTextField.getText());
        meta.setRemoveSelectedFields(wRemoveSelectedCheckbox.getSelection());
        meta.setResourceType(wResourceTypeTextField.getText());
        meta.setResourceUriField(wResourceUriCombo.getText());

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
            mapping.rdfPropertyName = Util.parseQName(namespaces, propertyName);
            final String rdfType = Util.nullIfEmpty(wMappingsTableView.getItem(i, 3));
            mapping.rdfType = Util.parseQName(namespaces, rdfType);
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