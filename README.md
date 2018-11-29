# sample-dcapture-db 

Application sample for dcapture-db and dcapture-io projects

Java -version 10

### Dependency

1. dcapture-db, dcapture-io - 1.0

2. Required dependency from both projects

### Development vs Production

1. Localization instance load method for production usage.
2. mvn package  (Terminal maven command - sources package into target folder)
3. mvn dependency:copy-dependencies (Terminal maven command helps to copy dependency to target folder)
4. After dependency copied folder renamed to lib (All dependencies)
5. Run following command to execute application  
5.1  java -cp sample-dcapture-db.jar sample.dcapture.db.luncher.Main
 
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

IM04

- >dcapture-db and dcapture-io changes are updated
- >H2DatabaseFactory helps to start h2 database for development purpose
- >SqlTableBuilder removed, builder is added in db project

IM05

- >dcapture-db and dcapture-io changes are updated
- >SqlMapper removed instead of use SqlParser

##### Oct-2018

IM06

- >dcapture-db and dcapture-io changes are updated
- >csv-import-view and csv-table util pages are created
- >log4j2.xml logger configuration file added for HikariCP jdbc connection pool.
- >Currency view are updated latest changes with export and import

IM07

- >@Path annotation replaced by @HttpPath 
- >@HttpMethod annotation are used
- >system-db and key sequence are added
- >dcapture-db and dcapture-io changes are updated

##### Nov-2018

IM08

- >dcapture-db and dcapture-io changes are updated
- >Expense, Currency and Expense Category status removed
- >Project concept removed

IM09

- >Default values added into appdata directory
- >Lookup field ui improved
- >dcapture-db and dcapture-io changes are updated
