/**
 * The MIT License
 * Copyright © 2020 The National Archives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package uk.gov.nationalarchives.pdi.step.jena;

public interface Rdf11 {

    String RDF_PREFIX = "rdf";

    String RDF_SCHEMA_PREFIX = "rdfs";

    String XSD_PREFIX = "xsd";

    String RESOURCE_DATA_TYPE = "Resource";

    String[] DATA_TYPES = new String[] {
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

    String[] SERIALIZATION_FORMATS = new String[] {
            "RDF/XML",
            "RDF/XML-ABBREV",
            "N-TRIPLE",
            "TURTLE",
            "N3"
    };
    String DEFAULT_SERIALIZATION_FORMAT = "TURTLE";
}
