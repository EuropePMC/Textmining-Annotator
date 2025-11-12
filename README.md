# Textmining Annotator

## Local Installation and Deployment

### Prerequisites

- [Textmining Utility][1]
- [Textmining Annotator Resources][2]
- Run `mvn clean` in the project root to install the custom built Monq jar

### Logging

For logging to work, you need to set the following JVM options:

`HOSTNAME` and `LOGPATH`

as their values are used to determine the path of the log files "LOGPATH/logs/textmining_api_annotator-HOSTNAME.log".

### Starting the Project

- Comment the dictionaries you don't need in `init()` method in `LoadDictionary` class
  and the corresponding filters in `getBaseDictFilters(String ftId)` method in `ExecuteDictionaryService` class 
  if you are deploying locally because loading all dictionaries locally will take a lot of time 
  and will probably crash the app with a `java.lang.OutOfMemoryError: Java heap space` exception.
- Since this is a Spring Boot project, you can start it by running the main method of the `Application` class.


[1]: https://gitlab.ebi.ac.uk/literature-services/public-projects/textmining-utility
[2]: https://gitlab.ebi.ac.uk/literature-services/public-projects/textmining-annotator-resources
