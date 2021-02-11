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
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import javax.annotation.Nullable;
import java.nio.file.Path;

abstract class Entry extends ResourceCon implements Resource {
    Entry(Node node, EnhGraph enhGraph) {
        super(node, enhGraph);
    }

    @Nullable
    String getName() {
        final Literal x = getTypedObjectOfFirstOptional(Vocabulary.name, Literal.class);
        if (x == null) {
            return null;
        }

        return x.getString();
    }

    @Nullable
    Action getAction() {
        return getAction(Vocabulary.action);
    }

    @Nullable
    Action getResult() {
        return getAction(Vocabulary.result);
    }

    private @Nullable
    Action getAction(Property property) {
        final TypedNode action = getTypedObjectOfFirstOptional(property, TypedNode.class);
        if (action == null) {
            return null;
        }

        final Resource type = action.getType();

        if (Vocabulary.TransformationAction.equals(type)) {
            return action.as(TransformationAction.class);
        } else if (Vocabulary.LoadGraphAction.equals(type)) {
            return action.as(LoadGraphAction.class);
        } else {
            throw new IllegalStateException(String.format("No action implementation found for node %s", action));
        }
    }

    abstract boolean execute(final Path tempDir) throws Exception;
}

