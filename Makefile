# Makefile for building gams-api images
#
# Commands:
#  - 'make' without targets builds gams-api Docker image
#  - 'make push' pushes the default build
#  - 'make build-native' builds the native image
#  - 'make push-native' pushes the native build
#
# After build and push a new image don't forget to update 
# the docker-compose.yml in the project 'production'
#
VERSION = $(shell cat .release)
IMAGENAME ?= gams-api
PREFIX ?= zimgraz
IMAGE ?= $(IMAGENAME):$(VERSION)
FULL_IMAGE_TAG = "$(PREFIX)/$(IMAGE)"

# allows to pass through cli arguments to ./mvnw call during pre-tag. Like -DskipTests
MVN_OPTIONS ?=

all: pre-tag build

pre-tag:
	test $(VERSION)
	@echo "*** Release version: $(VERSION) ***"
	@echo "*** Building with java version ***"
	java --version

build: pre-tag
	@echo "*** Building image $(FULL_IMAGE_TAG) ***"
	./mvnw spring-boot:build-image $(MVN_OPTIONS)
	@echo "*** Tagging $(IMAGENAME) as $(FULL_IMAGE_TAG)"
	docker tag $(IMAGENAME) $(FULL_IMAGE_TAG)

build-native: pre-tag
	@echo "*** Building image $(FULL_IMAGE_TAG) ***"
	./mvnw -Pnative spring-boot:build-image -DskipTests $(MVN_OPTIONS)
	@echo "*** Tagging $(IMAGENAME) as $(FULL_IMAGE_TAG)"
	docker tag $(IMAGENAME) $(FULL_IMAGE_TAG).native

push:
	@echo "*** Pushing image $(FULL_IMAGE_TAG) ***"
	docker push $(FULL_IMAGE_TAG)

push-native:
	@echo "*** Pushing image $(FULL_IMAGE_TAG) ***"
	docker push $(FULL_IMAGE_TAG).native

.PHONY: clean
clean: pre-tag
	@echo "*** Removing image $(FULL_IMAGE_TAG) ***"
	docker image rm $(FULL_IMAGE_TAG)
	
