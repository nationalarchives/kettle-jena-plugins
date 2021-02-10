package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;

import javax.annotation.Nullable;
import java.util.Iterator;

final class Manifest extends ResourceCon implements Resource {
    final static Implementation factory = new CanAlwaysWrapImplementation() {
        @Override
        public EnhNode wrap(Node node, EnhGraph enhGraph) {
            return new Manifest(node, enhGraph);
        }
    };

    Manifest(Node node, EnhGraph enhGraph) {
        super(node, enhGraph);
    }

    Iterator<Entry> getEntries() {
        return iterateTypedItemsOfFirstOptional(Vocabulary.entries, TypedNode.class).mapWith(Manifest::getEntry);
    }

    private static @Nullable
    Entry getEntry(TypedNode node) {
        final Resource type = node.getType();

        if (Vocabulary.CompareGraphsEntry.equals(type)) {
            return node.as(CompareGraphsEntry.class);
        } else {
            throw new IllegalStateException(String.format("No entry implementation found for node %s", node));
        }
    }
}

