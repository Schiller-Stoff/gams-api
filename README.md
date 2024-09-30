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

### Automatic via make

1. Increment version in .release

2. Run make command (take a look at Makefile on root) - either native or default profile.


```shell

# 01. increment version in .release
nano .release

# 02. build new image
make

# 03. push new version 
make push


## Alternative workflow (native) 

# 01b. increment version in .release
nano .release

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




