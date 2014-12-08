ISLE2CLARIN
===========

Convert ISLE Metadata (IMDI) records into CLARIN Component Metadata (CMDI) records.

Dependencies
------------
SchemAnon: https://github.com/TheLanguageArchive/SchemAnon

Translator: https://trac.mpi.nl/browser/latsvn/MetadataTranslator

CMDIValidator: https://trac.clarin.eu/browser/CMDIValidator

IMDI_3.0.xsd (included): https://trac.mpi.nl/browser/latsvn/corpustools/vocabs/IMDI/Schema/IMDI_3.0.xsd

Run
---
```sh
java -jar isle2clarin.jar <DIR with IMDI files> <file with skip list>?
```
