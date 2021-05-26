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
package uk.gov.nationalarchives.pdi.step.jena.model;

import org.eclipse.swt.custom.CCombo;
import org.pentaho.di.ui.core.widget.*;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNull;
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
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static uk.gov.nationalarchives.pdi.step.jena.Rdf11.RESOURCE_DATA_TYPE;
import static uk.gov.nationalarchives.pdi.step.jena.Util.*;

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
    private Label wResourceUriLabel;
    private ComboVar wResourceUriCombo;
    private Button wGetUriFieldButton;
    private Label wRemoveSelectedLabel;
    private Button wRemoveSelectedCheckbox;
    private TableView wNamespacesTableView;
    private CTabFolder wTabFolder;
    private Button wTableGetFieldsButton;
    private Button wAddBNodeButton;
    private Button wCancel;
    private Button wOK;
    private ModifyListener lsMappingsTableModify;
    private Listener lsResourceUriGetFields;
    private Listener lsTableGetFields;
    private Listener lsAddBNode;
    private Listener lsCancel;
    private Listener lsOK;
    private SelectionAdapter lsDef;
    private boolean changed;

    /**
     * mappingsTables[0] is always the first database mappings table (i.e. NOT a Blank Node),
     * the remaining entries are for Blank Node mappings.
     */
    TableView[] mappingsTables;

    public JenaModelStepDialog(final Shell parent, final Object in, final TransMeta transMeta, final String stepname) {
        super(parent, (BaseStepMeta) in, transMeta, stepname);
        meta = (JenaModelStepMeta) in;
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

        lsMappingsTableModify = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                meta.setChanged();
            }
        };
        changed = meta.hasChanged();

        //15 pixel margins
        final FormLayout formLayout = new FormLayout();
        formLayout.marginLeft = MARGIN_SIZE;
        formLayout.marginHeight = MARGIN_SIZE;
        shell.setLayout(formLayout);
        shell.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.Shell.Title"));

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
        wStepNameLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.Stepname.Label"));
        props.setLook(wStepNameLabel);
        final FormData fdStepNameLabel = new FormDataBuilder().left()
                .top()
                .result();
        wStepNameLabel.setLayoutData(fdStepNameLabel);

        wStepNameField = new Text(contentComposite, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepNameField.setText(stepname);
        props.setLook(wStepNameField);
        wStepNameField.addModifyListener(lsMappingsTableModify);
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
        group.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.GroupText"));
        final FormLayout groupLayout = new FormLayout();
        groupLayout.marginWidth = MARGIN_SIZE;
        groupLayout.marginHeight = MARGIN_SIZE;
        group.setLayout(groupLayout);
        final FormData groupLayoutData = new FormDataBuilder().fullWidth()
                .top(topSpacer, MARGIN_SIZE)
                .result();
        group.setLayoutData(groupLayoutData);
        props.setLook(group);

        //target field name label/field
        wTargetLabel = new Label(group, SWT.LEFT);
        props.setLook(wTargetLabel);
        wTargetLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.TextFieldTarget"));
        final FormData fdlTransformation0 = new FormDataBuilder().left()
                .top()
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
        wRemoveSelectedLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.CheckboxRemoveSelected"));
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

        // namespaces table
        final ColumnInfo[] namespacesColumns = new ColumnInfo[] {
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
                5, lsMappingsTableModify, props );
        props.setLook(wNamespacesTableView);
        final FormData fdNamespacesTableView = new FormDataBuilder().fullWidth()
                .top(wRemoveSelectedCheckbox, ELEMENT_SPACING)
                .result();
        wNamespacesTableView.setLayoutData(fdNamespacesTableView);

        //resource URI label/field
        wResourceUriLabel = new Label(group, SWT.LEFT);
        props.setLook(wResourceUriLabel);
        wResourceUriLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.TextFieldResourceUri"));
        final FormData fdResourceUriLabel = new FormDataBuilder().left()
                .top(wNamespacesTableView, ELEMENT_SPACING)
                .result();
        wResourceUriLabel.setLayoutData(fdResourceUriLabel);

        wResourceUriCombo = new ComboVar(transMeta, group,  SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wResourceUriCombo);
        final FormData fdResourceUriCombo = new FormDataBuilder().left()
                .top(wResourceUriLabel, LABEL_SPACING)
                .width(LARGE_FIELD)
                .result();
        wResourceUriCombo.setLayoutData(fdResourceUriCombo);

        wGetUriFieldButton = new Button(group, SWT.PUSH);
        wGetUriFieldButton.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.GetFieldsButton"));
        final FormData fdGetField = new FormDataBuilder().left(wResourceUriCombo, LABEL_SPACING)
                .top(wResourceUriLabel, LABEL_SPACING)
                .result();
        wGetUriFieldButton.setLayoutData(fdGetField);

        //Tabs
        wTabFolder = new CTabFolder(contentComposite, SWT.BORDER);
        props.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

        // 1st Tab - Mappings
        final CTabItem wTabMappings = new CTabItem(wTabFolder, SWT.NONE);
        wTabMappings.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.TabMappings"));
        final Composite wTabMappingsContents = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wTabMappingsContents);
        final FormLayout tabMappingsLayout = new FormLayout();
        tabMappingsLayout.marginWidth = MARGIN_SIZE;
        tabMappingsLayout.marginHeight = MARGIN_SIZE;
        wTabMappingsContents.setLayout(tabMappingsLayout);
        final FormData fdTabMappings = new FormDataBuilder().fullSize()
                .result();
        wTabMappingsContents.setLayoutData(fdTabMappings);
        wTabMappings.setControl(wTabMappingsContents);

        // select 1st Tab
        wTabFolder.setSelection(0);

        //Table and buttons for the first tab
        wTableGetFieldsButton = new Button(wTabMappingsContents, SWT.PUSH);
        wTableGetFieldsButton.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.GetFieldsButton"));
        final FormData fdTableGetFieldsButton = new FormDataBuilder().right()
                .bottom()
                .result();
        wTableGetFieldsButton.setLayoutData(fdTableGetFieldsButton);

        // create mappings table
        final FormData fdTableMappings = new FormDataBuilder().fullWidth()
                .top()
                .bottom(wTableGetFieldsButton, -ELEMENT_SPACING)
                .result();
        final TableView wMappingsTableView = createMappingsTable(wTabMappingsContents, fdTableMappings, lsMappingsTableModify);
        addMappingsTableToMappingsTables(wMappingsTableView);

        final FormData fdTabFolder = new FormDataBuilder().fullWidth()
                .top(group, MARGIN_SIZE)
                .bottom()
                .result();
        wTabFolder.setLayoutData(fdTabFolder);

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

        // Add bNode button
        wAddBNodeButton = new Button(shell, SWT.PUSH);
        wAddBNodeButton.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.AddBNodeButton"));
        final FormData fdAddBNodeButton = new FormDataBuilder().right(100, -MARGIN_SIZE)
                .bottom(bottomSpacer, -MARGIN_SIZE)
                .result();
        wAddBNodeButton.setLayoutData(fdAddBNodeButton);

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

        //Listeners
        lsResourceUriGetFields = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                getFieldsFromPrevious(wResourceUriCombo, transMeta, stepMeta);
                wResourceUriCombo.select(0);
            }
        };

        lsTableGetFields = new Listener() {
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
        lsAddBNode = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                final int bNodeTabId = createBNodeTab();
                addBNodeToRdfPropertyTypes(mappingsTables, bNodeTabId);
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

        wGetUriFieldButton.addListener(SWT.Selection, lsResourceUriGetFields);
        wTableGetFieldsButton.addListener(SWT.Selection, lsTableGetFields);
        wAddBNodeButton.addListener(SWT.Selection, lsAddBNode);
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

        // populate the table
        getDbToJenaMappingTableData(meta.getDbToJenaMappings(), mappingsTables[0]);  // 0 - is always the main (non Blank Nodes) table

        final JenaModelStepMeta.BlankNodeMapping[] blankNodeMappings = meta.getBlankNodeMappings();
        if (blankNodeMappings != null) {
            for (final JenaModelStepMeta.BlankNodeMapping blankNodeMapping : blankNodeMappings) {

                final int mappingTablesIdx = blankNodeMapping.id + 1;
                if (mappingTablesIdx >= mappingsTables.length) {
                    // create a tab
                    final int bNodeTabId = createBNodeTab();
                    if (bNodeTabId != blankNodeMapping.id) {
                        throw new IllegalStateException();
                    }
                }

                // populate the table in the tab
                final TableView bNodeMappingTable = mappingsTables[mappingTablesIdx];
                getDbToJenaMappingTableData(blankNodeMapping.dbToJenaMappings, bNodeMappingTable);
            }
        }
    }

    private static void getDbToJenaMappingTableData(final JenaModelStepMeta.DbToJenaMapping[] dbToJenaMappings,
            final TableView tableView) {
        if (dbToJenaMappings != null) {
            tableView.getTable().removeAll();
            for (final JenaModelStepMeta.DbToJenaMapping dbToJenaMapping : dbToJenaMappings) {
                tableView.add(new String[] {
                        dbToJenaMapping.fieldName,
                        dbToJenaMapping.rdfPropertyNameSource.toString(),
                        Util.asPrefixString(dbToJenaMapping.rdfType),
                        dbToJenaMapping.skip ?
                                BaseMessages.getString(PKG, "JenaModelStepDialog.SkipYes") :
                                BaseMessages.getString(PKG, "JenaModelStepDialog.SkipNo"),
                        dbToJenaMapping.language,
                        dbToJenaMapping.actionIfNull.name()
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

    static String[] getPropertyTypes(final TableView[] mappingsTables) {
        final String[] propertyTypes;
        if (mappingsTables == null) {
            // get the default property types for RDF 1.1 and add "Resource"
            propertyTypes = new String[Rdf11.DATA_TYPES.length + 1];
            propertyTypes[0] = RESOURCE_DATA_TYPE;
            System.arraycopy(Rdf11.DATA_TYPES, 0, propertyTypes, 1, Rdf11.DATA_TYPES.length);
        } else {
            // copy the property types from the main mappings tab (i.e. the first tab)
            final String[] mainPropertyTypes = mappingsTables[0].getColumns()[2].getComboValues();
            propertyTypes = Arrays.copyOf(mainPropertyTypes, mainPropertyTypes.length);
        }
        return propertyTypes;
    }

    private TableView createMappingsTable(final Composite parent, final FormData formData, final ModifyListener lsMod) {
        final String[] propertyTypes = getPropertyTypes(mappingsTables);

        final String[] skipNames = {
                BaseMessages.getString(PKG, "JenaModelStepDialog.SkipNo"),
                BaseMessages.getString(PKG, "JenaModelStepDialog.SkipYes"),
        };

        final ColumnInfo ciFieldName = new ColumnInfo(
                BaseMessages.getString(PKG, "JenaModelStepDialog.Fieldname"),
                ColumnInfo.COLUMN_TYPE_TEXT,
                false
        );
        final ColumnInfo ciRdfPropertyName = new ColumnInfo(
                BaseMessages.getString(PKG, "JenaModelStepDialog.RdfPropertyName"),
                ColumnInfo.COLUMN_TYPE_TEXT,
                false
        );
        final ColumnInfo ciRdfPropertyType = new ColumnInfo(
                BaseMessages.getString(PKG, "JenaModelStepDialog.RdfPropertyType"),
                ColumnInfo.COLUMN_TYPE_CCOMBO,
                propertyTypes  // combo-box options
        );
        final ColumnInfo ciSkip = new ColumnInfo(
                BaseMessages.getString(PKG, "JenaModelStepDialog.Skip"),
                ColumnInfo.COLUMN_TYPE_CCOMBO,
                skipNames  // combo-box options
        );
        final ColumnInfo ciLanguage = new ColumnInfo(
                BaseMessages.getString(PKG, "JenaModelStepDialog.Language"),
                ColumnInfo.COLUMN_TYPE_TEXT,
                false
        );
        final ColumnInfo ciIfNull = new ColumnInfo(
                BaseMessages.getString(PKG, "JenaModelStepDialog.IfNull"),
                ColumnInfo.COLUMN_TYPE_CCOMBO,
                ActionIfNull.names()  // combo-box options
        );

        ciRdfPropertyType.setSelectionAdapter(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent selectionEvent) {
                final CCombo combo = (CCombo)selectionEvent.getSource();
                final String selectedRdfPropertyType = combo.getItem(combo.getSelectionIndex());

                final Matcher mtcBlankNodeId = BLANK_NODE_ID_PATTERN.matcher(selectedRdfPropertyType);
                if (mtcBlankNodeId.matches()) {
                    final Table table = (Table) combo.getParent();
                    final TableItem tableItem = table.getSelection()[0];
                    tableItem.setText(1, BLANK_NODE_FIELD_NAME);
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent selectionEvent) {
                // no-op
            }
        });

        final ColumnInfo[] mappingsColumns = {
                ciFieldName,
                ciRdfPropertyName,
                ciRdfPropertyType,
                ciSkip,
                ciLanguage,
                ciIfNull
        };

        final TableView wMappingsTableView = new TableView(
                transMeta, parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
                mappingsColumns,10, lsMod, props);

        wMappingsTableView.setLayoutData(formData);

        return wMappingsTableView;
    }

    private void addMappingsTableToMappingsTables(final TableView mappingsTable) {
        if (this.mappingsTables == null) {
            this.mappingsTables = new TableView[1];
        } else {
            this.mappingsTables = Arrays.copyOf(mappingsTables, mappingsTables.length + 1);
        }
        this.mappingsTables[mappingsTables.length - 1] = mappingsTable;
    }

    private void removeMappingsTableFromMappingsTables(final int removeIdx) {
        if (this.mappingsTables != null) {
            final TableView[] newMappingsTables = new TableView[mappingsTables.length - 1];
            System.arraycopy(mappingsTables, 0, newMappingsTables, 0, removeIdx);
            final int srcIdx = removeIdx + 1;
            System.arraycopy(mappingsTables, srcIdx, newMappingsTables, removeIdx, newMappingsTables.length - removeIdx);

            this.mappingsTables = newMappingsTables;
        }
    }

    private int createBNodeTab() {
        final int bNodeTabIndex = wTabFolder.getItems().length;
        final int bNodeTabId = bNodeTabIndex - 1;

        // create tab
        final CTabItem wTabBNode = new CTabItem(wTabFolder, SWT.NONE);
        wTabBNode.setText(BLANK_NODE_NAME + ":" + bNodeTabId);
        final Composite wTabBNodeContents = new Composite(wTabFolder, SWT.NONE);
        props.setLook(wTabBNodeContents);
        final FormLayout tabBNodeLayout = new FormLayout();
        tabBNodeLayout.marginWidth = MARGIN_SIZE;
        tabBNodeLayout.marginHeight = MARGIN_SIZE;
        wTabBNodeContents.setLayout(tabBNodeLayout);
        final FormData fdTabBNode = new FormDataBuilder().fullSize()
                .result();
        wTabBNodeContents.setLayoutData(fdTabBNode);
        wTabBNode.setControl(wTabBNodeContents);

        // add ID label/label
        final Label wIdLabel = new Label(wTabBNodeContents, SWT.LEFT);
        props.setLook(wIdLabel);
        wIdLabel.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.bNodeId"));
        final FormData fdIdLabel = new FormDataBuilder().left()
                .top()
                .result();
        wIdLabel.setLayoutData(fdIdLabel);

        final Label wIdLabelValue = new Label(wTabBNodeContents, SWT.CHECK);
        wIdLabelValue.setText(String.valueOf(bNodeTabId));
        props.setLook(wIdLabelValue);
        final FormData fdIdLabelValue = new FormDataBuilder().left(wIdLabel, LABEL_SPACING)
                .top()
                .result();
        wIdLabelValue.setLayoutData(fdIdLabelValue);

        // add buttons
        final Button wGetFieldsButton = new Button(wTabBNodeContents, SWT.PUSH);
        wGetFieldsButton.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.GetFieldsButton"));
        final FormData fdTableGetFieldsButton = new FormDataBuilder().right()
                .bottom()
                .result();
        wGetFieldsButton.setLayoutData(fdTableGetFieldsButton);

        final Button wRemoveBNodeButton = new Button(wTabBNodeContents, SWT.PUSH);
        wRemoveBNodeButton.setText(BaseMessages.getString(PKG, "JenaModelStepDialog.RemoveBNodeButton"));
        final FormData fdRemoveBNodeButton = new FormDataBuilder().right(wGetFieldsButton, -LABEL_SPACING)
                .bottom()
                .result();
        wRemoveBNodeButton.setLayoutData(fdRemoveBNodeButton);

        // add table
        final FormData fdTableMappings = new FormDataBuilder().fullWidth()
                .top(wIdLabel, ELEMENT_SPACING)
                //.bottom(wGetFieldsButton, -ELEMENT_SPACING)
                .result();
        final TableView wBNodeMappingsTableView = createMappingsTable(wTabBNodeContents, fdTableMappings,
                lsMappingsTableModify);
        addMappingsTableToMappingsTables(wBNodeMappingsTableView);

        // add listeners
        wRemoveBNodeButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                final int selectedTabIndex = wTabFolder.getSelectionIndex();
                final int bNodeTabId = removeBNodeTab(selectedTabIndex);

               // renumber any bNodes after this one!
               renumberBNodeTabs(bNodeTabId);
               renumberRdfPropertyTypes(bNodeTabId);
               renumberSelectedRdfPropertyTypeBNodes(bNodeTabId);
            }
        });

        wGetFieldsButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                getFieldsFromPrevious(transMeta, stepMeta, wBNodeMappingsTableView,
                        1,
                        new int[]{1},
                        new int[]{},
                        -1,
                        -1,
                        null);
            }
        });

        // select this tab
        wTabFolder.setSelection(bNodeTabIndex);

        return bNodeTabId;
    }

    static void addBNodeToRdfPropertyTypes(final TableView[] mappingsTables, final int bNodeTabId) {
        if (mappingsTables != null) {

            final Matcher mtcBlankNodeId = BLANK_NODE_ID_PATTERN.matcher("");

            for (int i = 0; i < mappingsTables.length; i++) {
                if (bNodeTabId != i - 1) {  // don't add a bNodeId to RDF Property Types in the bNode tab that created it!
                    final TableView mappingsTable = mappingsTables[i];

                    final ColumnInfo ciRdfPropertyTypes = mappingsTable.getColumns()[2];
                    final String[] rdfPropertyTypes = ciRdfPropertyTypes.getComboValues();

                    int pivotIdx = -1;
                    for (int j = 0; j < rdfPropertyTypes.length; j++) {

                        pivotIdx = j;

                        final String rdfPropertyType = rdfPropertyTypes[j];
                        if (rdfPropertyType.equals(RESOURCE_DATA_TYPE)) {
                            break;
                        }

                        mtcBlankNodeId.reset(rdfPropertyType);
                        if (mtcBlankNodeId.matches()) {
                            final int bNodeId = Integer.parseInt(mtcBlankNodeId.group(1));
                            if (bNodeId > bNodeTabId) {
                                break;
                            } else if (bNodeId == bNodeTabId) {
                                pivotIdx = -1;  // entry already exists
                                break;
                            }
                        }
                    }

                    if (pivotIdx > -1) {
                        final String[] newRdfPropertyTypes = new String[rdfPropertyTypes.length + 1];
                        System.arraycopy(rdfPropertyTypes, 0, newRdfPropertyTypes, 0, pivotIdx);
                        newRdfPropertyTypes[pivotIdx] = BLANK_NODE_NAME + ":" + bNodeTabId;
                        final int destIdx = pivotIdx + 1;
                        System.arraycopy(rdfPropertyTypes, pivotIdx, newRdfPropertyTypes, destIdx, newRdfPropertyTypes.length - destIdx);

                        ciRdfPropertyTypes.setComboValues(newRdfPropertyTypes);
                    }
                }
            }
        }
    }

    private int removeBNodeTab(final int tabIndex) {
        // remove the tab
        final CTabItem wTab = wTabFolder.getItem(tabIndex);
        wTab.dispose();

        // remove the mappings table for the bNodeTab
        removeMappingsTableFromMappingsTables(tabIndex);

        // calculate the bNode ID
        final int bNodeTabId = tabIndex - 1;
        return bNodeTabId;
    }

    private void renumberBNodeTabs(final int removedBNodeTabId) {
        // NOTE: we only need to renumber those tabs starting from removedBNodeTabId

        final Matcher mtcBlankNodeId = BLANK_NODE_ID_PATTERN.matcher("");

        for (int i = removedBNodeTabId + 1; i < wTabFolder.getItemCount(); i++) {
            final CTabItem wTab = wTabFolder.getItem(i);

            if (!wTab.isDisposed()) {

                // match against the tab's title
                mtcBlankNodeId.reset(wTab.getText());
                if (mtcBlankNodeId.matches()) {
                    final int prevBNodeId = Integer.parseInt(mtcBlankNodeId.group(1));
                    final int newBNodeId = prevBNodeId - 1;  // decrement

                    // decrement the bNodeId in the tab's title
                    wTab.setText(BLANK_NODE_NAME + ":" + newBNodeId);

                    // update the bNodeId label's value within the tab
                    final Composite tabContents = (Composite) wTab.getControl();
                    for (final Control child : tabContents.getChildren()) {
                        if (child instanceof Label) {
                            final Label label = (Label) child;
                            if (label.getText().matches("[0-9]+")) {
                                label.setText(String.valueOf(newBNodeId));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void renumberRdfPropertyTypes(final int removedBNodeTabId) {
        // NOTE: we only need to renumber those RDFPropertyTypes which are blobIds and starting from removedBNodeTabId

        if (mappingsTables != null) {

            final Matcher mtcBlankNodeId = BLANK_NODE_ID_PATTERN.matcher("");

            for (int i = 0; i < mappingsTables.length; i++) {
                final TableView mappingsTable = mappingsTables[i];

                final ColumnInfo ciRdfPropertyTypes = mappingsTable.getColumns()[2];
                final String[] rdfPropertyTypes = ciRdfPropertyTypes.getComboValues();

                int removeIdx = -1;
                for (int j = 0; j < rdfPropertyTypes.length; j++) {
                    final String rdfPropertyType = rdfPropertyTypes[j];
                    if (rdfPropertyType.equals(RESOURCE_DATA_TYPE)) {
                        break;
                    }

                    mtcBlankNodeId.reset(rdfPropertyType);
                    if (mtcBlankNodeId.matches()) {
                        final int bNodeId = Integer.parseInt(mtcBlankNodeId.group(1));
                        if (bNodeId == removedBNodeTabId) {
                            removeIdx = j;
                        }

                        if (bNodeId > removedBNodeTabId) {
                            // decrement the entry
                            final int newBNodeId = bNodeId - 1;
                            rdfPropertyTypes[j] = BLANK_NODE_NAME + ":" + newBNodeId;
                        }
                    }
                }

                if (removeIdx > -1) {
                    // remove the removedBNode Entry from the list

                    final String[] newRdfPropertyTypes = new String[rdfPropertyTypes.length - 1];
                    System.arraycopy(rdfPropertyTypes, 0, newRdfPropertyTypes, 0, removeIdx);
                    final int srcIdx = removeIdx + 1;
                    System.arraycopy(rdfPropertyTypes, srcIdx, newRdfPropertyTypes, removeIdx, newRdfPropertyTypes.length - removeIdx);

                    ciRdfPropertyTypes.setComboValues(newRdfPropertyTypes);
                }
            }
        }
    }

    private void renumberSelectedRdfPropertyTypeBNodes(final int removedBNodeTabId) {
        if (mappingsTables != null) {

            final Matcher mtcBlankNodeId = BLANK_NODE_ID_PATTERN.matcher("");

            for (int i = 0; i < mappingsTables.length; i++) {
                final TableView mappingsTable = mappingsTables[i];

                final int rows = mappingsTable.getItemCount();

                for (int r = 0; r < rows; r++) {
                    final String rdfPropertyType = mappingsTable.getItem(r, 3);

                    mtcBlankNodeId.reset(rdfPropertyType);

                    if (mtcBlankNodeId.matches()) {
                        final int bNodeId = Integer.parseInt(mtcBlankNodeId.group(1));

                        if (bNodeId == removedBNodeTabId) {
                            mappingsTable.setText("", 3, r);

                        } else if (bNodeId > removedBNodeTabId) {
                            // decrement the entry
                            final int newBNodeId = bNodeId - 1;
                            mappingsTable.setText(BLANK_NODE_NAME + ":" + newBNodeId, 3, r);
                        }
                    }
                }
            }
        }
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
        meta.setTargetFieldName(wTargetTextField.getText());
        meta.setRemoveSelectedFields(wRemoveSelectedCheckbox.getSelection());
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

        // both user-specified namespaces and internal namespaces
        final Map<String, String> allNamespaces = new LinkedHashMap<>(namespaces);
        allNamespaces.put(BLANK_NODE_NAME, BLANK_NODE_INTERNAL_URI);

        final JenaModelStepMeta.DbToJenaMapping[] dbToJenaMappings = dbToJenaMappingsDataFromTable(mappingsTables[0], allNamespaces);
        meta.setDbToJenaMappings(dbToJenaMappings);

        final JenaModelStepMeta.BlankNodeMapping[] blankNodeMappings = new JenaModelStepMeta.BlankNodeMapping[mappingsTables.length - 1];
        for (int i = 0; i < blankNodeMappings.length; i++) {
            final JenaModelStepMeta.BlankNodeMapping blankNodeMapping = new JenaModelStepMeta.BlankNodeMapping();
            blankNodeMapping.id = i;
            blankNodeMapping.dbToJenaMappings = dbToJenaMappingsDataFromTable(mappingsTables[i + 1], allNamespaces);
            blankNodeMappings[i] = blankNodeMapping;
        }
        meta.setBlankNodeMappings(blankNodeMappings);
    }

    private JenaModelStepMeta.DbToJenaMapping[] dbToJenaMappingsDataFromTable(final TableView tableView,
            final Map<String, String> namespaces) {
        final int mappingsLen = tableView.getItemCount();
        JenaModelStepMeta.DbToJenaMapping dbToJenaMappings[] = new JenaModelStepMeta.DbToJenaMapping[mappingsLen];
        int mappingsCount = 0;
        for (int i = 0; i < mappingsLen; i++) {
            final String fieldName = tableView.getItem(i, 1);
            if (isNullOrEmpty(fieldName)) {
                continue;
            }

            final JenaModelStepMeta.DbToJenaMapping dbToJenaMapping = new JenaModelStepMeta.DbToJenaMapping();
            dbToJenaMapping.fieldName = fieldName;
            final String propertyName = tableView.getItem(i, 2);
            dbToJenaMapping.rdfPropertyNameSource = JenaModelStepMeta.RdfPropertyNameSource.fromString(namespaces, propertyName);
            final String rdfType = Util.nullIfEmpty(tableView.getItem(i, 3));
            dbToJenaMapping.rdfType = Util.parseQName(namespaces, rdfType);
            dbToJenaMapping.skip = tableView.getItem(i, 4).equals(BaseMessages.getString(PKG, "JenaModelStepDialog.SkipYes"));
            dbToJenaMapping.language = tableView.getItem(i, 5);
            final String actionIfNullName = tableView.getItem(i, 6);
            if (isNullOrEmpty(actionIfNullName)) {
                // default
                dbToJenaMapping.actionIfNull = ActionIfNull.WARN;
            } else {
                dbToJenaMapping.actionIfNull = ActionIfNull.valueOf(actionIfNullName);
            }
            dbToJenaMappings[mappingsCount++] = dbToJenaMapping;
        }

        if (mappingsCount < mappingsLen) {
            dbToJenaMappings = Arrays.copyOf(dbToJenaMappings, mappingsCount);
        }

        return dbToJenaMappings;
    }
}