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

