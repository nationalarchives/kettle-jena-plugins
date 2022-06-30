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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNoSuchField;
import uk.gov.nationalarchives.pdi.step.jena.ActionIfNull;
import uk.gov.nationalarchives.pdi.step.jena.ConstrainedField;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class TestDSL {

    static Model ModelWithSubject(final int id, final String subject) {
        final Model model = ModelFactory.createDefaultModel();
        final Resource resource = model.createResource("http://model/" + id);
        final Property dctSubjectProperty = model.createProperty("http://purl.org/dc/terms/", "subject");
        model.add(resource, dctSubjectProperty, subject);
        return model;
    }

    static Model MergedModels(final Model... models) {
        final Model newModel = ModelFactory.createDefaultModel();
        for (final Model model : models) {
            newModel.add(model);
        }
        return newModel;
    }

    static class Row {
        final Field[] fields;

        public Row(final Field... fields) {
            this.fields = fields;
        }

        public Row copy() {
            final Field[] newFields = Arrays.copyOf(fields, fields.length);
            return new Row(newFields);
        }

        public Row addField(final Field field) {
            final Field[] newFields = Arrays.copyOf(fields, fields.length + 1);
            newFields[newFields.length - 1] = field;
            return new Row(newFields);
        }

        public Row addFields(final Field... additionalFields) {
            final Field[] newFields = Arrays.copyOf(fields, fields.length + additionalFields.length);
            System.arraycopy(additionalFields, 0, newFields, fields.length, additionalFields.length);
            return new Row(newFields);
        }

        public Row removeField(final String fieldName) {
            final Field[] newFields = new Field[fields.length - 1];
            int i = 0;
            for (final Field field : fields) {
                if (!field.name.equals(fieldName)) {
                    newFields[i++] = field;
                }
            }
            return new Row(newFields);
        }

        public RowMetaInterface getMeta() throws KettlePluginException {
            final RowMetaInterface meta = new RowMeta();
            for (final Field field : fields) {
                final ValueMetaInterface valueMeta = ValueMetaFactory.createValueMeta(field.name, field.type);
                meta.addValueMeta(valueMeta);
            }
            return meta;
        }

        public Object[] values() {
            return values(false);
        }

        public Object[] values(final boolean overAllocate) {
            final Object[] values;
            if (overAllocate) {
                values = RowDataUtil.allocateRowData(fields.length);
            } else {
                values = new Object[fields.length];
            }
            int i = 0;
            for (final Field field : fields) {
                values[i++] = field.value;
            }
            return values;
        }
    }

    static class Field {
        final String name;
        final int type;
        final Object value;

        private Field(final String name, final int type, final Object value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }

    static Row Row(final Field... fields) {
        return new Row(fields);
    }

    static Field Field(final String name, final int type, final Object value) {
        return new Field(name, type, value);
    }

    static ConstrainedField GroupField(final String fieldName, final ActionIfNoSuchField actionIfNoSuchField,
            final ActionIfNull actionIfNull) {
        return new ConstrainedField(fieldName, actionIfNoSuchField, actionIfNull);
    }

    static ModelMergeConstrainedField MergeField(final String fieldName, final ActionIfNoSuchField actionIfNoSuchField,
            final ActionIfNull actionIfNull, final MutateFirstModel mutateFirstModel,
            @Nullable final String targetFieldName) {
        return new ModelMergeConstrainedField(fieldName, actionIfNoSuchField, actionIfNull, mutateFirstModel, targetFieldName);
    }

    static ConstrainedField[] GroupFields(final ConstrainedField... groupFields) {
        return groupFields;
    }

    static ModelMergeConstrainedField[] MergeFields(final ModelMergeConstrainedField... mergeFields) {
        return mergeFields;
    }

    static JenaGroupMergeStepMeta JenaGroupMergeStepMeta(final ConstrainedField[] groupFields,  final ModelMergeConstrainedField[] mergeFields, final boolean closeMergedModels, final OtherFieldAction otherFieldAction) {
        final JenaGroupMergeStepMeta jenaGroupMergeStepMeta = new JenaGroupMergeStepMeta();
        jenaGroupMergeStepMeta.setGroupFields(Arrays.asList(groupFields));
        jenaGroupMergeStepMeta.setMergeFields(Arrays.asList(mergeFields));
        jenaGroupMergeStepMeta.setCloseMergedModels(closeMergedModels);
        jenaGroupMergeStepMeta.setOtherFieldAction(otherFieldAction);
        return jenaGroupMergeStepMeta;
    }

    static JenaGroupMergeStepData JenaGroupMergeStepData(final RowMetaInterface outputRowMeta, final RemainingInputField... remainingInputFields) {
        final JenaGroupMergeStepData jenaGroupMergeStepData = new JenaGroupMergeStepData();
        jenaGroupMergeStepData.setOutputRowMeta(outputRowMeta);

        final LinkedHashMap<String, Integer> remainingInputFieldIndexes = new LinkedHashMap<>();
        for (final RemainingInputField remainingInputField : remainingInputFields) {
            remainingInputFieldIndexes.put(remainingInputField.name, remainingInputField.index);
        }
        jenaGroupMergeStepData.setRemainingInputFieldIndexes(remainingInputFieldIndexes);

        return jenaGroupMergeStepData;
    }

    static JenaGroupMergeStepData JenaGroupMergeStepData(final Object[] groupMergedRow, final RowMetaInterface outputRowMeta, final RemainingInputField... remainingInputFields) {
        final JenaGroupMergeStepData jenaGroupMergeStepData = new JenaGroupMergeStepData();
        jenaGroupMergeStepData.setOutputRowMeta(outputRowMeta);

        final LinkedHashMap<String, Integer> remainingInputFieldIndexes = new LinkedHashMap<>();
        for (final RemainingInputField remainingInputField : remainingInputFields) {
            remainingInputFieldIndexes.put(remainingInputField.name, remainingInputField.index);
        }
        jenaGroupMergeStepData.setRemainingInputFieldIndexes(remainingInputFieldIndexes);

        jenaGroupMergeStepData.setGroupMergedRow(groupMergedRow);

        return jenaGroupMergeStepData;
    }

    static class RemainingInputField {
        final String name;
        final int index;

        public RemainingInputField(final String name, final int index) {
            this.name = name;
            this.index = index;
        }
    }

    static RemainingInputField RemainingInputField(final String name, final int index) {
        return new RemainingInputField(name, index);
    }

    static <T> List<T> List(final T... items) {
        return new ArrayList<>(Arrays.asList(items));  // create a mutable list
    }
}
