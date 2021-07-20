# ISLE2CLARIN

Convert ISLE Metadata (IMDI) records into CLARIN Component Metadata (CMDI) records.

## Clone and compile
```sh
git clone https://github.com/TheLanguageArchive/ISLE2CLARIN && cd ISLE2CLARIN && MAVEN_OPTS=-Xss10M mvn install
```

## or else if already cloned, go to the directory ISLE2CLARIN and Compile
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
### example IMDI conversion
```sh
docker run -v $HOME/imdis:/tmp/imdis isle2clarin -- /tmp/imdis
```

## Dependencies
- SchemAnon: https://github.com/TheLanguageArchive/SchemAnon

- Translator: https://trac.mpi.nl/browser/latsvn/MetadataTranslator

- CMDIValidator: https://trac.clarin.eu/browser/CMDIValidator

- IMDI_3.0.xsd (included): https://trac.mpi.nl/browser/latsvn/corpustools/vocabs/IMDI/Schema/IMDI_3.0.xsd
