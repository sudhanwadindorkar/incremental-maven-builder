# Incremental Maven Builder
Build only what has changed.

## Overview

This is a Maven extension to run incremental builds on a multi-module Maven project. This extension automatically determines the modules that have changed since the last build and builds only the changed modules.

## Prerequisites
 * Maven 3.5.0+
 * Java 8

## Configure the extension
This extension is not yet published to Maven Central since it is in a POC stage. Hence it will have to be installed manually. Clone this Git repo (or download it) and run "mvn clean install" in this project's directory. This will install the extension to your Maven repository and will be available for other Maven projects to use.
In order to use this extension in a Maven project, Create a .mvn folder in your top-level Maven project directory. Create a extensions.xml file in the .mvn folder and add the following to the extensions.xml fiile:

``` xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
  <extension>
    <groupId>com.github.sudhanwadindorkar</groupId>
    <artifactId>incremental-maven-builder</artifactId>
    <version>xxx</version>
  </extension>
</extensions>
```
Replace the "xxx" in the version tag with the version from this project's pom.xml.

## Usage
	mvn -b incremental -Dincremental.simulate=true|false -Dincremental.reactor.detailed=true|false 
Invoke the incremental builder using the `mvn -b incremental` switch. It will print the list of changed projects that will be built and also the projects that were skipped. It will then build the changed projects. The "incremental.simulate" flag, if set to true, does not run the actual build. The value of the flag is false by default. The "incremental.reactor.detailed" flag, if set to true, prints a detailed reactor include the list of changed projects, dependent projects & skipped projects. The value of the flag is false by default.

The following are some examples:

`mvn -b incremental clean install` -  Runs clean install on the changed projects.

`mvn -b incremental -Dincremental.simulate clean install` -  Just determines the changed projects, prints the list of the changed projects & skipped projects.

`mvn -b incremental -Dincremental.reactor.detailed clean install` -  Runs clean install on the changed projects while printing a detailed reactor.

`mvn -b incremental -X clean install` -  Runs clean install on the changed projects while printing debug level information.


## How does it work?
>tldr; This extension requires the install phase to be run for functioning.

The incremental builder determines the list of changed projects by comparing the last modified timestamps of a module's source artifacts with the last modified timestamp of the module artifact (.war, .jar, etc) installed in the local Maven repository (the .m2/repository folder usually). The source artifacts for a module include:

 * The module's pom.xml.
 * The script source directory (if any). e.g. src/main/scripts.
 * The source directory e.g. src/main/java
 * The resource directories.

If at least one source artifact is found such that it's last modified timestamp is after the last modified timestamp of the module artifact installed in the local Maven repository, the the module is added to the list of projects to be built.


## Next Steps

 * Unit Tests.
 * Publish to Maven Central.
 * Get feedback & suggestions from users.
 * Determine the changed projects based on the artifacts in the target folder i.e. support for the package phase (if someone asks for it).
 * What would you like?

## Credits
This extension has been inspired by Karl Heinz Marbaise's Maven extension for incremental Maven builds - https://github.com/khmarbaise/incremental-module-builder. My thanks to Karl Heinz Marbaise for making it available.