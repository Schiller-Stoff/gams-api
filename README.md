# GAMS API

REST-API built with spring boot and dependent libraries

# Quick start / Development setup

## Installation

### Docker quick startup


```sh

# cd to clone root 
docker-compose up

# run spring boot application via maven or from SpringFedoraApplication.java
# e.g. from wsl with 'dev' profile (needs application-dev.yml):
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# or easier: start spring boot app from your IDE

```

# Development

Run application in 'dev' profile (best via IDE settings)

```sh
# runs spring
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# default profile is the production profile

```

# Testing

Integration and unittests are meant to be run separately via own maven goals.

## Unit tests

**/*Test.java classes

```sh
# run all tests
./mvnw test
```

## Integration tests

**/*IT.java classes

```sh
# run all tests
./mvnw integration-test

```

## Profiles

default - production profile
dev - development profile


# Production

## Compilation / Packaging

### Via make

see deployment section


### Spring native workflow / AOT ("Ahead of Time") Processing

```sh
# build gams-api via native profile  
./mvnw -Pnative spring-boot:build-image

# run with turned on error stack trace
./mvnw -Pnative -e spring-boot:build-image

```

### Standard compilation to .jar

```shell

./mvnw spring-boot:build-image


```


## Deployment

### Installation requirements
- Docker installed and running
- Docker Hub account with push rights to zimgraz/gams-api repository
- Make installed
- Java required version installed
- Maven installed

### Automatic via make

1. Increment version in pom.xml

2. Run make command (take a look at Makefile on root) - either native or default profile.


```shell

# 01. increment version in pom.xml
nano pom.xml

# 02. COMMIT everything
git commit -m "commit_message"

# 03. create and publish the release (docker hub + git tag)
make release


### Local Testing

# 02. build new image (for local testing)
make

# (03. push new version by hand to docker hub) 
make push


## Native workflow 

# 01b. increment version in pom.xml
nano pom.xml

# 02b. native workflow
make build-native

# 03b. push new version
make push-native

 

```


### Manual workflow (via Docker Hub )

```sh

# 1. Build image
# build image - naming is controlled by pom.xml
./mvnw spring-boot:build-image

# 2. Tag the image with current version
docker tag <IMAGE-ID> zimgraz/gams-api:<VERSION> 

# 3. push newest version  
docker push zimgraz/gams-api-<VERSION>


```




