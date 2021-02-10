package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;

import java.nio.file.Path;

abstract class Action extends ResourceCon implements Resource {
    Action(Node node, EnhGraph enhGraph) {
        super(node, enhGraph);
    }

    String getPath() {
        return getTypedObjectOfFirstOptional(Vocabulary.path, Resource.class).getURI();
    }

    // TODO(SL): Execute from entry?
    // TODO(SL): Take context?
    abstract Object execute(final Path tempDir) throws Exception;
}

