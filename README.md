# sample-dcapture-db 

Application sample for dcapture-db and dcapture-io projects

Java -version 10

### Dependency

1. dcapture-db, dcapture-io - 1.0

2. Required dependency from both projects

### Development vs Production

Localization instance load method for production usage. 
log4j2.properties replaced using log4j2-production.properties

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