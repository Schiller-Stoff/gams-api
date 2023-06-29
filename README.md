# GAMS API

REST-API built with spring boot and additional libraries

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

# the production profile is only for production purposes (has enabled security etc.)
# will crash if an external spring application tries to access it.
# The production profile should be the default profile(?)
./mvnw spring-boot:run [-Dspring-boot.run.profiles=prod]

```


## Profiles

dev - development profile with disabled security etc.
prod - production profile with enabled security etc.


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

2. Run make command:

```sh
# 01. increment version in .release
# e.g. to 0.0.5

# 02. run make command on project root
make

# Optional: (Same as above but will push result image as to docker-hub) 
make push

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




