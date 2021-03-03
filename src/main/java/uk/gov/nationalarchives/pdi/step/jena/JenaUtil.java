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
package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.rdf.model.Model;
import org.pentaho.di.core.exception.KettleException;

public class JenaUtil {

    /**
     * Close the model and throw a Kettle Exception.
     *
     * @param model The Jena Model
     * @param message The message for the exception
     *
     * @throws KettleException an exception with the message
     */
    public static void closeAndThrow(final Model model, final String message) throws KettleException {
        closeAndThrow(model, new KettleException(message));
    }

    /**
     * Close the model and throw a Kettle Exception.
     *
     * @param <E> The type of the exception
     * @param model The Jena Model
     * @param e The exception
     *
     * @throws E the exception e
     */
    public static <E extends Exception> void closeAndThrow(final Model model, final E e) throws E {
        // abort the transaction on the model
        if (model.supportsTransactions()) {
            model.abort();
        }
        model.close();
        throw e;
    }
}
