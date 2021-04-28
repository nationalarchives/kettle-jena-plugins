# Jena Plugins for Pentaho KETTLE

[![CI](https://github.com/nationalarchives/kettle-jena-plugins/workflows/CI/badge.svg)](https://github.com/nationalarchives/kettle-jena-plugins/actions?query=workflow%3ACI)
[![Java 8](https://img.shields.io/badge/java-8+-blue.svg)](https://adoptopenjdk.net/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

This project contains plugins for [Pentaho Data Integration](https://github.com/pentaho/pentaho-kettle) (or KETTLE as it is commonly known),
that add functionality via [Apache Jena](https://jena.apache.org/) for producing RDF.

The plugins provided are:
1. Create Jena Model
   
    <img alt="Create Jena Model Icon" src="https://raw.githubusercontent.com/nationalarchives/kettle-jena-plugins/main/src/main/resources/JenaModelStep.svg" width="32"/>
    This transform plugin can be used to create a Jena Model for each row sent to it. Each Row becomes a Resource, and the plugin enables the mapping of fields to RDF Literals or Resources.
    The plugin includes support for constructing Blank Nodes within Resources.

2. Combine Jena Models
    
    <img alt="Combine Jena Models Icon" src="https://raw.githubusercontent.com/nationalarchives/kettle-jena-plugins/main/src/main/resources/JenaCombineStep.svg" width="32"/>
    This transform plugin allows you to merge multiple Jena Models that are within the same row into a single model. This can be considered as a horizontal transformation within a row.

2. Group Merge Jena Models

    <img alt="Group Merge Jena Models Icon" src="https://raw.githubusercontent.com/nationalarchives/kettle-jena-plugins/main/src/main/resources/JenaGroupMergeStep.svg" width="32"/>
    This transform plugin performs a Group By operation across consecutive rows, allowing you to merge multiple Jena Models that are within consecutive rows into a single model in a single row.
    This can be considered as a vertical transformation across rows.

4. Serialize Jena Model
    
    <img alt="Serialize Jena Model Icon" src="https://raw.githubusercontent.com/nationalarchives/kettle-jena-plugins/main/src/main/resources/JenaSerializerStep.svg" width="32"/>
    This output plugin takes the output of the Create Jena Model plugin, and serializes it to an RDF file on disk. Supports Turtle, N3, N-Triples, and RDF/XML output formats.

This project was developed by [Evolved Binary](https://evolvedbinary.com) as part of Project OMEGA for the [National Archives](https://nationalarchives.gov.uk).

## Getting the Plugins

You can either download the plugins from our GitHub releases page: https://github.com/nationalarchives/kettle-jena-plugins/releases/, or you can build them from source.

## Building from Source Code
The plugins can be built from Source code by installing the pre-requisites and following the steps described below.

### Pre-requisites for building the project:
* [Apache Maven](https://maven.apache.org/), version 3+
* [Java JDK](https://adoptopenjdk.net/) 1.8
* [Git](https://git-scm.com)

### Build steps:
1. Clone the Git repository
    ```
    $ git clone https://github.com/nationalarchives/kettle-jena-plugins.git
    ```

2. Compile a package
    ```
    $ cd kettle-jena-plugins
    $ mvn clean package
    ```
    
3. The plugins directory is then available at `target/kettle-jena-plugins-1.0.0-SNAPSHOT-kettle-plugin/kettle-jena-plugins`


## Installing the plugins
* Tested with Pentaho Data Integration - Community Edition - version: 8.3.0.0-371

You need to copy the plugins directory `kettle-jena-plugins` (from building above) into the `plugins` sub-directory of your KETTLE installation.

This can be done by either running:
```
  $ mvn -Pdeploy-pdi-local -Dpentaho-kettle.plugins.dir=/opt/data-integration/plugins antrun:run@deploy-to-pdi
```

or, you can do so manually, e.g. e.g.:
```
  $ cp -r target/kettle-jena-plugins-1.0.0-SNAPSHOT-kettle-plugin/kettle-jena-plugins /opt/data-integration/plugins/
```

## Using the plugins
We wrote a short blog about working with the plugins: https://blog.adamretter.org.uk/rdf-plugins-for-pentaho-kettle/

We also created a small screencast demonstrating how to use the plugins in Pentaho Kettle. It's hosted on YouTube, click the image below to visit the video:

[![Watch the video](https://img.youtube.com/vi/2uqG_z2Qy9g/maxresdefault.jpg)](https://www.youtube.com/embed/2uqG_z2Qy9g)
