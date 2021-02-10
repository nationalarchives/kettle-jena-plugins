package uk.gov.nationalarchives.pdi.step.jena;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

class Vocabulary {
    private final static String manifestNS = "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#";
    private final static String someNS = "http://example.com/";

    static final Resource Manifest = manifestResource("Manifest");
    static final Property action = manifestProperty("action");
    static final Property result = manifestProperty("result");
    static final Property entries = manifestProperty("entries");
    static final Property name = manifestProperty("name");

    static final Resource TransformationAction = someProperty("TransformationAction");
    static final Resource LoadGraphAction = someProperty("LoadGraphAction");
    static final Resource CompareGraphsEntry = someProperty("CompareGraphsEntry");
    static final Property path = someProperty("path");

    private static Resource manifestResource(String name) {
        return ResourceFactory.createResource(manifestNS + name);
    }

    private static Property manifestProperty(String name) {
        return ResourceFactory.createProperty(manifestNS, name);
    }

    private static Resource someResource(String name) {
        return ResourceFactory.createResource(someNS + name);
    }

    private static Property someProperty(String name) {
        return ResourceFactory.createProperty(someNS, name);
    }
}
