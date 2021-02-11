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

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NiceIterator;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Objects;

abstract class ResourceCon extends ResourceImpl implements Resource {
    protected ResourceCon(Node node, EnhGraph enhGraph) {
        super(node, enhGraph);
    }

    protected final @Nullable
    <T extends RDFNode> T getTypedObjectOfFirstOptional(Property property, Class<T> targetType) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(targetType);

        final Statement statement = this.getProperty(property);

        if (statement == null) {
            return null;
        }

        return statement
                .getObject()
                .as(targetType);
    }

    protected final <T extends RDFNode> Iterator<T> listTypedItemsOfFirstOptional(Property property, Class<T> targetType) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(targetType);

        return iterateTypedItemsOfFirstOptional(property, targetType);
    }

    protected final <T extends RDFNode> ExtendedIterator<T> iterateTypedItemsOfFirstOptional(Property property, Class<T> targetType) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(targetType);

        final Statement statement = this.getProperty(property);
        if (statement == null) {
            return NiceIterator.emptyIterator();
        }

        final RDFNode object = statement.getObject();
        if (!object.canAs(RDFList.class)) {
            return NiceIterator.emptyIterator();
        }

        return object
                .as(RDFList.class)
                .mapWith(listItem -> listItem.as(targetType));
    }
}
