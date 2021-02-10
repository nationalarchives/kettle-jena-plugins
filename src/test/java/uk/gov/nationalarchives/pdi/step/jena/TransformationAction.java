package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;

import java.nio.file.Path;

final class TransformationAction extends Action {
    final static Implementation factory = new CanAlwaysWrapImplementation() {
        @Override
        public EnhNode wrap(Node node, EnhGraph enhGraph) {
            return new TransformationAction(node, enhGraph);
        }
    };

    TransformationAction(Node node, EnhGraph enhGraph) {
        super(node, enhGraph);
    }

    @Override
    Object execute(Path tempDir) throws KettleException {
        KettleEnvironment.init();

        final String transformationFile = getPath();

        final Trans trans = new Trans(new TransMeta(transformationFile));

        final Path outputFilePath = tempDir.resolve("output.ttl");

        trans.setVariable("filename", outputFilePath.toString());
        trans.execute(null);
        trans.waitUntilFinished();

        KettleEnvironment.shutdown();

        final Model output = ModelFactory.createDefaultModel();
        output.read(outputFilePath.toUri().toString(), "TURTLE");

        return output;
    }
}
