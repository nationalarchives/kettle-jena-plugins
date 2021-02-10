package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NiceIterator;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Objects;

abstract class ResourceCon extends ResourceImpl implements Resource {
    protected ResourceCon(Node node, EnhGraph enhGraph) {
        super(node, enhGraph);
    }

    protected final @Nullable
    <T extends RDFNode> T getTypedObjectOfFirstOptional(Property property, Class<T> targetType) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(targetType);

        final Statement statement = this.getProperty(property);

        if (statement == null) {
            return null;
        }

        return statement
                .getObject()
                .as(targetType);
    }

    protected final <T extends RDFNode> Iterator<T> listTypedItemsOfFirstOptional(Property property, Class<T> targetType) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(targetType);

        return iterateTypedItemsOfFirstOptional(property, targetType);
    }

    protected final <T extends RDFNode> ExtendedIterator<T> iterateTypedItemsOfFirstOptional(Property property, Class<T> targetType) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(targetType);

        final Statement statement = this.getProperty(property);
        if (statement == null) {
            return NiceIterator.emptyIterator();
        }

        final RDFNode object = statement.getObject();
        if (!object.canAs(RDFList.class)) {
            return NiceIterator.emptyIterator();
        }

        return object
                .as(RDFList.class)
                .mapWith(listItem -> listItem.as(targetType));
    }
}
