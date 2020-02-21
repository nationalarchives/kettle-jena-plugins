package com.evolvedbinary.pdi;

import javax.xml.XMLConstants;
import java.util.Collections;

public interface Rdf11 {

    final String RDF_PREFIX = "rdf";
    final String RDF_NAMESPACE_IRI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    final String RDF_SCHEMA_PREFIX = "rdfs";
    final String RDF_SCHEMA_NAMESPACE_IRI = "http://www.w3.org/2000/01/rdf-schema#";

    final String XSD_PREFIX = "xsd";
    final String XSD_NAMESPACE_IRI = "http://www.w3.org/2001/XMLSchema#";

    final String[] DATA_TYPES = new String[] {
                "xsd:string",
                "xsd:boolean",
                "xsd:decimal",
                "xsd:integer",
                "xsd:double",
                "xsd:float",
                "xsd:date",
                "xsd:time",
                "xsd:dateTime",
                "xsd:dateTimeStamp",
                "xsd:gYear",
                "xsd:gMonth",
                "xsd:gDay",
                "xsd:gYearMonth",
                "xsd:gMonthDay",
                "xsd:duration",
                "xsd:yearMonthDuration",
                "xsd:dayTimeDuration",
                "xsd:byte",
                "xsd:short",
                "xsd:int",
                "xsd:long",
                "xsd:unsignedByte",
                "xsd:unsignedShort",
                "xsd:unsignedInt",
                "xsd:unsignedLong",
                "xsd:unsignedInteger",
                "xsd:nonNegativeInteger",
                "xsd:negativeInteger",
                "xsd:nonPositiveInteger",
                "xsd:hexBinary",
                "xsd:base64Binary",
                "xsd:anyURI",
                "xsd:language",
                "xsd:normalizeString",
                "xsd:token",
                "xsd:NMTOKEN",
                "xsd:Name",
                "xsd:NCName",
                "rdf:HTML",
                "rdf:XMLLiteral"
    };
}
