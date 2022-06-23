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

import uk.gov.nationalarchives.pdi.step.jena.groupmerge.OtherFieldAction;

public enum MutateFirstModel {
    YES("Yes"),
    NO("No");

    private final String label;

    MutateFirstModel(final String label) {
        this.label = label;
    }

    /**
     * Get the String label.
     *
     * @return the label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the String labels of the enumerated values.
     *
     * @return an array of string names.
     */
    public static String[] labels() {
        final MutateFirstModel[] values = values();
        final String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].label;
        }
        return labels;
    }

    /**
     * Given the label get the MutateFirstModel.
     *
     * @param label the label of an MutateFirstModel.
     *
     * @return the MutateFirstModel that matches the label.
     *
     * @throws IllegalArgumentException if the label does not match an MutateFirstModel.
     */
    public static MutateFirstModel fromLabel(final String label) {
        for (final MutateFirstModel mutateFirstModel : values()) {
            if (mutateFirstModel.label.equals(label)) {
                return mutateFirstModel;
            }
        }
        throw new IllegalArgumentException("Unrecognised label: " + label);
    }
}
