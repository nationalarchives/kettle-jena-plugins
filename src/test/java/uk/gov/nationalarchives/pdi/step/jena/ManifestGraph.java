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

import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.vocabulary.RDF;

import java.util.Collections;
import java.util.Iterator;

final class ManifestGraph extends ModelCom {
    ManifestGraph() {
        this(ModelFactory.createDefaultModel());
    }

    private ManifestGraph(Model model) {
        this(model.getGraph(), BuiltinPersonalities.model);
    }

    private ManifestGraph(Graph base, Personality<RDFNode> p) {
        super(base, p.copy());

        final Personality personality = getPersonality();
        personality.add(TypedNode.class, TypedNode.factory);
        personality.add(Manifest.class, Manifest.factory);
        personality.add(CompareGraphsEntry.class, CompareGraphsEntry.factory);
        personality.add(TransformationAction.class, TransformationAction.factory);
        personality.add(LoadGraphAction.class, LoadGraphAction.factory);
        personality.add(Result.class, Result.factory);
    }

    Iterator<Manifest> getManifests() {
        final ResIterator manifests = this.listResourcesWithProperty(RDF.type, Vocabulary.Manifest);

        if (manifests == null) {
            return Collections.emptyIterator();
        }

        return manifests.mapWith(node -> node.as(Manifest.class));
    }
}
