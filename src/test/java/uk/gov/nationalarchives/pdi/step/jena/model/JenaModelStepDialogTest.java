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

import org.junit.jupiter.api.Test;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;

public class JenaModelStepDialogTest {

    @Test
    public void addBNodeToRdfPropertyTypesOne() {
        final String[] defaultDataTypes = { "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" };

        // main table

        final TableView mainMappingsTableView = mock(TableView.class);
        final ColumnInfo ciMainMappingsTableView = new ColumnInfo("mainMappingsRdfPropertyType", ColumnInfo.COLUMN_TYPE_CCOMBO,  defaultDataTypes);
        when(mainMappingsTableView.getColumns()).thenReturn(new ColumnInfo[] { null, null, ciMainMappingsTableView });

        // 1st bNode table
        final TableView bNodeMappingsTableView1 = mock(TableView.class);
        final ColumnInfo ciBNodeMappingsTableView1 = new ColumnInfo("bNode1MappingsRdfPropertyType", ColumnInfo.COLUMN_TYPE_CCOMBO,  JenaModelStepDialog.getPropertyTypes(new TableView[] { mainMappingsTableView }));
        when(bNodeMappingsTableView1.getColumns()).thenReturn(new ColumnInfo[] { null, null, ciBNodeMappingsTableView1 });


        /* call - function under test: addBNodeToRdfPropertyTypes */
        final TableView[] mappingsTables1 = {
                mainMappingsTableView,
                bNodeMappingsTableView1
        };
        JenaModelStepDialog.addBNodeToRdfPropertyTypes(mappingsTables1, 0);

        assertArrayEquals(new String[] { "bNode: 0", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciMainMappingsTableView.getComboValues());
        assertArrayEquals(defaultDataTypes, ciBNodeMappingsTableView1.getComboValues());
    }

    @Test
    public void addBNodeToRdfPropertyTypesTwo() {
        final String[] defaultDataTypes = { "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" };

        // main table
        final TableView mainMappingsTableView = mock(TableView.class);
        final ColumnInfo ciMainMappingsTableView = new ColumnInfo("mainMappingsRdfPropertyType", ColumnInfo.COLUMN_TYPE_CCOMBO,  defaultDataTypes);
        when(mainMappingsTableView.getColumns()).thenReturn(new ColumnInfo[] { null, null, ciMainMappingsTableView });

        // 1st bNode table
        final TableView bNodeMappingsTableView1 = mock(TableView.class);
        final ColumnInfo ciBNodeMappingsTableView1 = new ColumnInfo("bNode1MappingsRdfPropertyType", ColumnInfo.COLUMN_TYPE_CCOMBO,  JenaModelStepDialog.getPropertyTypes(new TableView[] { mainMappingsTableView }));
        when(bNodeMappingsTableView1.getColumns()).thenReturn(new ColumnInfo[] { null, null, ciBNodeMappingsTableView1 });

        /* 1st call - function under test: addBNodeToRdfPropertyTypes */
        final TableView[] mappingsTables1 = {
                mainMappingsTableView,
                bNodeMappingsTableView1
        };
        JenaModelStepDialog.addBNodeToRdfPropertyTypes(mappingsTables1, 0);

        assertArrayEquals(new String[] { "bNode: 0", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciMainMappingsTableView.getComboValues());
        assertArrayEquals(defaultDataTypes, ciBNodeMappingsTableView1.getComboValues());


        /* 2nd call - function under test: addBNodeToRdfPropertyTypes */

        // 2nd bNode table
        final TableView bNodeMappingsTableView2 = mock(TableView.class);
        final ColumnInfo ciBNodeMappingsTableView2 = new ColumnInfo("bNode2MappingsRdfPropertyType", ColumnInfo.COLUMN_TYPE_CCOMBO, JenaModelStepDialog.getPropertyTypes(new TableView[] { mainMappingsTableView, bNodeMappingsTableView1 }));
        when(bNodeMappingsTableView2.getColumns()).thenReturn(new ColumnInfo[] { null, null, ciBNodeMappingsTableView2 });

        final TableView[] mappingsTables2 = {
                mainMappingsTableView,
                bNodeMappingsTableView1,
                bNodeMappingsTableView2
        };
        JenaModelStepDialog.addBNodeToRdfPropertyTypes(mappingsTables2, 1);

        assertArrayEquals(new String[] { "bNode: 0", "bNode: 1", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciMainMappingsTableView.getComboValues());
        assertArrayEquals(new String[] { "bNode: 1", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciBNodeMappingsTableView1.getComboValues());
        assertArrayEquals(new String[] { "bNode: 0", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciBNodeMappingsTableView2.getComboValues());
    }

    @Test
    public void addBNodeToRdfPropertyTypesThree() {
        final String[] defaultDataTypes = { "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" };

        // main table
        final TableView mainMappingsTableView = mock(TableView.class);
        final ColumnInfo ciMainMappingsTableView = new ColumnInfo("mainMappingsRdfPropertyType", ColumnInfo.COLUMN_TYPE_CCOMBO,  defaultDataTypes);
        when(mainMappingsTableView.getColumns()).thenReturn(new ColumnInfo[] { null, null, ciMainMappingsTableView });

        // 1st bNode table
        final TableView bNodeMappingsTableView1 = mock(TableView.class);
        final ColumnInfo ciBNodeMappingsTableView1 = new ColumnInfo("bNode1MappingsRdfPropertyType", ColumnInfo.COLUMN_TYPE_CCOMBO,  JenaModelStepDialog.getPropertyTypes(new TableView[] { mainMappingsTableView }));
        when(bNodeMappingsTableView1.getColumns()).thenReturn(new ColumnInfo[] { null, null, ciBNodeMappingsTableView1 });

        /* 1st call - function under test: addBNodeToRdfPropertyTypes */
        final TableView[] mappingsTables1 = {
                mainMappingsTableView,
                bNodeMappingsTableView1
        };
        JenaModelStepDialog.addBNodeToRdfPropertyTypes(mappingsTables1, 0);

        assertArrayEquals(new String[] { "bNode: 0", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciMainMappingsTableView.getComboValues());
        assertArrayEquals(defaultDataTypes, ciBNodeMappingsTableView1.getComboValues());


        /* 2nd call - function under test: addBNodeToRdfPropertyTypes */

        // 2nd bNode table
        final TableView bNodeMappingsTableView2 = mock(TableView.class);
        final ColumnInfo ciBNodeMappingsTableView2 = new ColumnInfo("bNode2MappingsRdfPropertyType", ColumnInfo.COLUMN_TYPE_CCOMBO, JenaModelStepDialog.getPropertyTypes(new TableView[] { mainMappingsTableView, bNodeMappingsTableView1 }));
        when(bNodeMappingsTableView2.getColumns()).thenReturn(new ColumnInfo[] { null, null, ciBNodeMappingsTableView2 });

        final TableView[] mappingsTables2 = {
                mainMappingsTableView,
                bNodeMappingsTableView1,
                bNodeMappingsTableView2
        };
        JenaModelStepDialog.addBNodeToRdfPropertyTypes(mappingsTables2, 1);

        assertArrayEquals(new String[] { "bNode: 0", "bNode: 1", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciMainMappingsTableView.getComboValues());
        assertArrayEquals(new String[] { "bNode: 1", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciBNodeMappingsTableView1.getComboValues());
        assertArrayEquals(new String[] { "bNode: 0", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciBNodeMappingsTableView2.getComboValues());

        /* 3rd call - function under test: addBNodeToRdfPropertyTypes */

        // 3rd bNode table
        final TableView bNodeMappingsTableView3 = mock(TableView.class);
        final ColumnInfo ciBNodeMappingsTableView3 = new ColumnInfo("bNode3MappingsRdfPropertyType", ColumnInfo.COLUMN_TYPE_CCOMBO, JenaModelStepDialog.getPropertyTypes(new TableView[] { mainMappingsTableView, bNodeMappingsTableView1, bNodeMappingsTableView2 }));
        when(bNodeMappingsTableView3.getColumns()).thenReturn(new ColumnInfo[] { null, null, ciBNodeMappingsTableView3 });

        final TableView[] mappingsTables3 = {
                mainMappingsTableView,
                bNodeMappingsTableView1,
                bNodeMappingsTableView2,
                bNodeMappingsTableView3
        };
        JenaModelStepDialog.addBNodeToRdfPropertyTypes(mappingsTables3, 2);

        assertArrayEquals(new String[] { "bNode: 0", "bNode: 1", "bNode: 2", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciMainMappingsTableView.getComboValues());
        assertArrayEquals(new String[] { "bNode: 1", "bNode: 2", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciBNodeMappingsTableView1.getComboValues());
        assertArrayEquals(new String[] { "bNode: 0", "bNode: 2", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciBNodeMappingsTableView2.getComboValues());
        assertArrayEquals(new String[] { "bNode: 0", "bNode: 1", "Resource", "xsd:string", "xsd:integer", "xsd:dateTime" }, ciBNodeMappingsTableView3.getComboValues());
    }
}
