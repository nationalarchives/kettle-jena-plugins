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
