# sample-dcapture-db 

Application sample for dcapture-db and dcapture-io projects

Java -version 10

### Dependency

1. dcapture-db, dcapture-io - 1.0

2. Required dependency from both projects

### Development vs Production

1. Localization instance load method for production usage.
2. log4j2.properties replaced using log4j2-production.properties 
3. mvn package  (Terminal maven command - sources package into target folder)
4. mvn dependency:copy-dependencies (Terminal maven command helps to copy dependency to target folder)
5. After dependency copied folder renamed to lib (All dependencies)
6. Run following command to execute application  
6.1  java -cp sample-dcapture-db.jar sample.dcapture.db.dev.Main
 
##### July-2018

IM01

- >Java 10 migration are updated.
- >dcapture-db and dcapture-io changes are updated
- >webapp folder moved to class path location

IM02

- >dcapture-io changes are updated
- >User service fix issues
- >single page (Modulation) environment are created.
- >bootstrap.bundle.js dependency is used, instead of bootstrap.js 
- >bootstrap unused dependencies are removed
- >LookupField visual effect improved
- >User Register two page concept are updated
- >simple-grid renamed as data-table, main.js renamed to core.js

##### Sep-2018

IM03

- >dcapture-db and dcapture-io changes are updated
- >Log4j dependency removed, default java logging used
