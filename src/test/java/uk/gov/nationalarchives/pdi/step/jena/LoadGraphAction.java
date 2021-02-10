package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.pentaho.di.core.exception.KettleException;

import java.nio.file.Path;

final class LoadGraphAction extends Action {
    final static Implementation factory = new CanAlwaysWrapImplementation() {
        @Override
        public EnhNode wrap(Node node, EnhGraph enhGraph) {
            return new LoadGraphAction(node, enhGraph);
        }
    };

    LoadGraphAction(Node node, EnhGraph enhGraph) {
        super(node, enhGraph);
    }

    @Override
    Object execute(Path tempDir) throws KettleException {
        final Model g = ModelFactory.createDefaultModel();
        g.read(getPath(), "TURTLE");

        return g;
    }
}
