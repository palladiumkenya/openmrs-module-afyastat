openmrs-module-afyastat
afyastat API
==========================

Description
-----------
This module handles data exchange between CHT powered Afyastat and KenyaEMR.
These are REST services used to receive data from afyastat server.
Afyastat is built on top of CHT Web application which is a single page application built with Angular and NgRx frameworks.
This is a very basic module which can be used as a starting point in creating a new module.

Building from Source
--------------------
You will need to have Java 1.6+ and Maven 2.x+ installed.  Use the command 'mvn package' to 
compile and package the module.  The .omod file will be in the omod/target folder.

Alternatively you can add the snippet provided in the [Creating Modules](https://wiki.openmrs.org/x/cAEr) page to your 
omod/pom.xml and use the mvn command:

    mvn package -P deploy-web -D deploy.path="../../openmrs-1.8.x/webapp/src/main/webapp"

It will allow you to deploy any changes to your web 
resources such as jsp or js files without re-installing the module. The deploy path says 
where OpenMRS is deployed.

Installation
------------
1. Build the module to produce the .omod file.
2. Use the OpenMRS Administration > Manage Modules screen to upload and install the .omod file.

If uploads are not allowed from the web (changable via a runtime property), you can drop the omod
into the ~/.OpenMRS/modules folder.  (Where ~/.OpenMRS is assumed to be the Application 
Data Directory that the running openmrs is currently using.)  After putting the file in there 
simply restart OpenMRS/tomcat and the module will be loaded and started.

Accreditation
-------------
* Data exchange processing logic adapted from the OpenMRS muzima core module, and used under MPL 2.0 (https://github.com/muzima/openmrs-module-muzimacore)
```
Contact Tracing API
------------
This is used to sync contact tracing data from afyastat to kenyaEMR. Peer calendar api uses the same syntax as encounter api above


Peer Calendar API
------------
This is used to sync peer calendar data from afyastat to kenyaEMR. Peer calendar api uses the same syntax as encounter api above

Demographic Update API
------------
This is used to update registration data from afyastat to kenyaEMR. Peer calendar api uses the same syntax as Demographic api above

Queue data, Archive data, Error data
------------
Data from medic is being queued on openmrs.medic_queue_data table (**Queue data**) where it is then processed by Timed Scheduler.
This process can be Successful, or it can Fail (**Error data**) failed data is stored on openmrs.medic_error_data despite the outcome the data is archived as successful or as error (**Archive data**) on  openmrs.medic_archive_data  table so that queued data does not grow.
