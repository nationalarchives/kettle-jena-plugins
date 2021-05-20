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
package uk.gov.nationalarchives.pdi.step;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.RowHandler;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * Simple RowHandler that takes input rows
 * and captures output and error rows.
 */
public class TestDataRowHandler implements RowHandler {
    private final Deque<Object[]> inputRows;
    private final Deque<RowMetaAndRowData> outputRows = new ArrayDeque<>();
    private final Deque<RowError> errorRows = new ArrayDeque<>();

    public TestDataRowHandler(final Collection<Object[]> inputRows) {
        this.inputRows = new ArrayDeque<>(inputRows);
    }

    @Override
    public Object[] getRow() {
        return inputRows.poll();
    }

    @Override
    public void putRow(final RowMetaInterface rowMeta, final Object[] row) {
        outputRows.push(new RowMetaAndRowData(rowMeta, row));
    }

    @Override
    public void putError(final RowMetaInterface rowMeta, final Object[] row, final long nrErrors, final String errorDescriptions,
                         final String fieldNames, final String errorCodes) {
        errorRows.push(new RowError(rowMeta, row, nrErrors, errorDescriptions, fieldNames, errorCodes));
    }

    /**
     * Gets the output rows.
     *
     * @return the output rows.
     */
    public Deque<RowMetaAndRowData> getOutputRows() {
        return new ArrayDeque<>(outputRows);
    }

    /**
     * Gets the error rows.
     *
     * @return the error rows.
     */
    public Deque<RowError> getErrorRows() {
        return new ArrayDeque<>(errorRows);
    }

    public static class RowMetaAndRowData {
        public final RowMetaInterface rowMeta;
        public final Object[] row;

        public RowMetaAndRowData(final RowMetaInterface rowMeta, final Object[] row) {
            this.rowMeta = rowMeta;
            this.row = row;
        }
    }

    public static class RowError {
        public final RowMetaInterface rowMeta;
        public final Object[] row;
        public final long nrErrors;
        public final String errorDescriptions;
        public final String fieldNames;
        public final String errorCodes;

        public RowError(final RowMetaInterface rowMeta, final Object[] row, final long nrErrors, final String errorDescriptions, final String fieldNames, final String errorCodes) {
            this.rowMeta = rowMeta;
            this.row = row;
            this.nrErrors = nrErrors;
            this.errorDescriptions = errorDescriptions;
            this.fieldNames = fieldNames;
            this.errorCodes = errorCodes;
        }
    }
}
