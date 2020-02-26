# Jena Plugins for Pentaho KETTLE

[![Build Status](https://travis-ci.com/nationalarchives/kettle-jena-plugins.svg?branch=master)](https://travis-ci.com/nationalarchives/kettle-jena-plugins)
[![Java 8](https://img.shields.io/badge/java-8+-blue.svg)](https://adoptopenjdk.net/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

This project contains plugins for [Pentaho Data Integration](https://github.com/pentaho/pentaho-kettle) (or KETTLE as it is commonly known),
that add functionality via [Apache Jena](https://jena.apache.org/) for producing RDF.

The plugins provided are:
1. Create Jena Model
    This plugin can be used to create a Jena Model for each row sent to it. The plugin enables the mapping of database columns to RDF Literals or Resources.

2. Serialize Jena Model
    Takes the output of the Jena Model plugin, and serializes it to an RDF file on disk. Supports Turtle, N3, N-Triples, and RDF/XML output formats.

This project was developed by [Evolved Binary](https://evolvedbinary.com) as part of Project OMEGA for the [National Archives](https://nationalarchives.gov.uk).

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

You can simply copy the plugins directory `kettle-jena-plugins` (from building above) into the `plugins` sub-directory of your KETTLE installation, e.g.:
```
  $ cp -r target/kettle-jena-plugins-1.0.0-SNAPSHOT-kettle-plugin/kettle-jena-plugins /opt/data-integration/plugins/
```