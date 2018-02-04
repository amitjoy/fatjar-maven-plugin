## Why?

This maven plugin is responsible to create a Fat JAR bundle containing all the dependencies 
	specified in a POM file. Sometimes users need to unpack specific dependency (eq. zip, tar etc) 
	before packing the dependency as this dependency could contain other jar files. The primary 
	motive is to collect all the dependencies including the plain jar dependencies and the other 
	jar dependencies that reside in other dependency (eq. zip, tar etc). Apart from this, it could 
	also update the versions of the specified dependencies before wrapping in a big fat JAR bundle. 
	This internally uses bnd to wrap all the dependencies in a single fat JAR.

-----------------------------------------------------------------

### Contribution [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/fatjar-maven-plugin/issues)

Want to contribute? Great! Check out [Contribution Guide](https://github.com/amitjoy/fatjar-maven-plugin/blob/master/CONTRIBUTING.md)

----------------------------------------------------------------

#### Project Import

**Import as Maven Project**

Import all the projects as Existing Maven Projects (`File -> Import -> Maven -> Existing Maven Projects`)

----------------------------------------------------------------

#### Building from Source

Run `mvn clean install -Dgpg.skip` in the project root directory

----------------------------------------------------------------

### License

This project is licensed under EPL-1.0 [![License](http://img.shields.io/badge/license-EPL-blue.svg)](http://www.eclipse.org/legal/epl-v10.html)

-----------------------------------------------------------------

### Usage

```xml
<plugin>
  <groupId>com.amitinside</groupId>
  <artifactId>fatjar-maven-plugin</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <configuration>
    <bundleSymbolicName>${bundle.symbolic.name}</bundleSymbolicName>
    <bundleVersion>${bundle.version}</bundleVersion>
    <extensionsToUnarchive>
        <param>zip</param>
        <param>tar</param>
    </extensionsToUnarchive>
    <targetDirectory>${file.store.location}</targetDirectory>
    <targetFilename>com.mybundle.mybsn.fat.jar</targetFilename>
    <updateDependencyVersions>true</updateDependencyVersions>
  </configuration>
</plugin>
```
-----------------------------------------------------------------
