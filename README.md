# ISLE2CLARIN

Convert ISLE Metadata (IMDI) records into CLARIN Component Metadata (CMDI) records.

## Compile
```sh
MAVEN_OPTS=-Xss10M mvn install
```

## Run
```sh
java -jar target/isle2clarin.jar <DIR with IMDI files>
```

For more command line options see:

```sh
java -jar target/isle2clarin.jar -?
```

## Docker
```sh
docker build -t isle2clarin https://raw.githubusercontent.com/TheLanguageArchive/ISLE2CLARIN/master/Dockerfile
docker run isle2clarin -?
```

## Dependencies
- SchemAnon: https://github.com/TheLanguageArchive/SchemAnon

- Translator: https://trac.mpi.nl/browser/latsvn/MetadataTranslator

- CMDIValidator: https://trac.clarin.eu/browser/CMDIValidator

- IMDI_3.0.xsd (included): https://trac.mpi.nl/browser/latsvn/corpustools/vocabs/IMDI/Schema/IMDI_3.0.xsd