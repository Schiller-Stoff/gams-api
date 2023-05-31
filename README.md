
# GAMS API

REST-API built with spring boot and additional libraries

# Quick start / Development setup

## Installation

### Docker quick startup


```sh

# cd to clone root 
docker-compose up

# run spring boot application via maven or from SpringFedoraApplication.java
# e.g. from wsl:
./mvnw spring-boot:run -P dev

# or easier: start spring boot app from your IDE

```

# Development

Run application in 'dev' profile (best via IDE settings)

```sh
# runs spring
./mvnw spring-boot:run -P dev

# the production profile is only for production purposes (has enabled security etc.)
# will crash if an external spring application tries to access it.
./mvnw spring-boot:run -P prod

```


## Profiles

dev - development profile with disabled security etc.
prod - production profile with enabled security etc.


# Production

## Compilation / Packaging

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

### Docker Hub (Manual workflow)

```sh

# 1. Build image
# build image - naming is controlled by pom.xml
./mvnw spring-boot:build-image

# 2. Tag the image with current version
docker tag <IMAGE-ID> zimgraz/gams-api:<VERSION> 

# 3. push newest version  
docker push zimgraz/gams-api-<VERSION>


```




