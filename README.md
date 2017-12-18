Dicoogle NIFTI converter
========================

[![Build Status](https://travis-ci.org/Enet4/dicoogle-nifti.svg?branch=master)](https://travis-ci.org/Enet4/dicoogle-nifti)

This plugin for Dicoogle allows for the upload and automatic storage of
NIFTI-1 files, a format defined by the Neuroimaging Informatics Technology 
Initiative (NIfTI).

Building
---------

This is a maven project. Run `mvn install` to create a jar file of the plugin.

It relies on a custom version of `niftijio`, which is automatically fetched by Maven using JitPack.

Installing
----------

Pass the jar file with dependencies, named with the pattern "nifti-vvvvvv-jar-with-dependencies",
to the "Plugins" folder in Dicoogle's working directory.

Configuring
-----------

The plugin needs to be configured before use. The settings file can be found
in the Plugin/settings folder by the name "NIFTI.xml".

 - `storage-scheme` : The storage scheme of which to keep converted objects in.
                      Defaults to "file".
 - `root-uid` : Defines the base DICOM UID part to be included in all generated
                instance UIDs as the prefix. This is usually associated to an
                institution's unique identifier, although it is not required to
                be so. Defaults to a fixed, but valid, UID.

For instance, if your system has a simple file storage plugin with the "file"
scheme and your institution is identified by the UID "1.2.351.472728", you may
use the following configuration:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<configuration>
   <storage-scheme>file</storage-scheme>
   <root-uid>1.2.351.472728</root-uid>
</configuration>
```

Using the Web Service API
-------------------------

### **POST** `/nifti/convert`

Convert and store the NIFTI files contained in the HTTP request.
The server expects either an entity containing the full data of the NIFTI file or a
`multipart/form-data` entity containing one or more NIFTI-1 files, with or without GZip
compression. If any of the files is compressed, the content type must mention that it is
so ("application/gzip").

Additional parameters can be included via query strings:

  - _modality_ : specifies the Modality attribute as well as additional attributes for
  compliance to that modality (currently only "CT" and "MR" are supported).
  - _patientName_ : defines the PatientName attribute, defaults to "Patient^Anonymous"
  - _patientID_ : defines the PatientID attribute, defaults to a random UUID.
  - _institutionName_
  - _manufacturer_
  - _bodyPart_

The storage procedure will be delegated to the storage plugin capable of
handling storage of the given scheme.

## License

Copyright (C) 2017 UA.PT Bioinformatics - http://bioinformatics.ua.pt

dicoogle-nifti is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

dicoogle-nifti is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
