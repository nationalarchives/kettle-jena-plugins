package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;

abstract class CanAlwaysWrapImplementation extends Implementation {
    @Override
    public boolean canWrap(Node node, EnhGraph enhGraph) {
        return true;
    }
}
