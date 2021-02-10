package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import javax.annotation.Nullable;

public final class TypedNode extends ResourceCon implements Resource {
    final static Implementation factory = new CanAlwaysWrapImplementation() {
        @Override
        public EnhNode wrap(Node node, EnhGraph enhGraph) {
            return new TypedNode(node, enhGraph);
        }
    };

    protected TypedNode(Node node, EnhGraph enhGraph) {
        super(node, enhGraph);
    }

    @Nullable
    Resource getType() {
        return getTypedObjectOfFirstOptional(RDF.type, Resource.class);
    }
}
