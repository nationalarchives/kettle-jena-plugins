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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

class Vocabulary {
    private final static String manifestNS = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#";
    private final static String someNS = "http://example.com/";

    static final Resource Manifest = manifestResource("Manifest");
    static final Property action = manifestProperty("action");
    static final Property result = manifestProperty("result");
    static final Property entries = manifestProperty("entries");
    static final Property name = manifestProperty("name");

    static final Resource TransformationAction = someProperty("TransformationAction");
    static final Resource LoadGraphAction = someProperty("LoadGraphAction");
    static final Resource CompareGraphsEntry = someProperty("CompareGraphsEntry");
    static final Property path = someProperty("path");

    private static Resource manifestResource(String name) {
        return ResourceFactory.createResource(manifestNS + name);
    }

    private static Property manifestProperty(String name) {
        return ResourceFactory.createProperty(manifestNS, name);
    }

    private static Resource someResource(String name) {
        return ResourceFactory.createResource(someNS + name);
    }

    private static Property someProperty(String name) {
        return ResourceFactory.createProperty(someNS, name);
    }
}
