package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.nio.file.Path;

public final class CompareGraphsEntry extends Entry implements Resource {
    final static Implementation factory = new CanAlwaysWrapImplementation() {
        @Override
        public EnhNode wrap(Node node, EnhGraph enhGraph) {
            return new CompareGraphsEntry(node, enhGraph);
        }
    };

    CompareGraphsEntry(Node node, EnhGraph enhGraph) {
        super(node, enhGraph);
    }

    @Override
    boolean execute(final Path tempDir) throws Exception {
        final Action action = getAction();

        if (action == null) {
            return false;
        }

        final Action result = getResult();

        if (result == null) {
            return false;
        }

        final Model actionModel = (Model) action.execute(tempDir);
        final Model resultModel = (Model) result.execute(tempDir);

        return actionModel.isIsomorphicWith(resultModel);
    }
}
