
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
./mvnw spring-boot:run

# or easier: start spring boot app from your IDE

```

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

### 




