# Makefile for building gams-api images
#
# Commands:
#  - 'make' runs tests and builds gams-api Docker image
#  - 'make build-skip-tests' builds without running tests
#  - 'make test' runs unit tests only
#  - 'make integration-test' runs integration tests only
#  - 'make verify' runs all tests
#  - 'make push' pushes the default build
#  - 'make build-native' builds the native image
#  - 'make push-native' pushes the native build
#

# (read out version from pom.xml)
VERSION = $(shell ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
IMAGENAME ?= gams-api
PREFIX ?= zimgraz
IMAGE ?= $(IMAGENAME):$(VERSION)
FULL_IMAGE_TAG = "$(PREFIX)/$(IMAGE)"

# allows to pass through cli arguments to ./mvnw call
MVN_OPTIONS ?=

all: pre-tag build

pre-tag:
	test $(VERSION)
	@echo "*** Release version: $(VERSION) ***"
	@echo "*** Building with java version ***"
	java --version
	@echo "*** Building with mvnw version ***"
	./mvnw --version
	@echo "*** Cleaning maven cache..."
	./mvnw clean

# Run all tests (unit + integration) and build image
build: pre-tag
	@echo "*** Running all tests ***"
	./mvnw verify $(MVN_OPTIONS)
	@echo "*** Building image $(FULL_IMAGE_TAG) ***"
	./mvnw spring-boot:build-image -DskipTests $(MVN_OPTIONS)
	@echo "*** Docker tagging $(IMAGENAME) as $(FULL_IMAGE_TAG)"
	docker tag $(IMAGENAME) $(FULL_IMAGE_TAG)

# Build without running tests (for quick iterations)
build-skip-tests: pre-tag
	@echo "*** Building image $(FULL_IMAGE_TAG) (SKIPPING TESTS) ***"
	./mvnw spring-boot:build-image -DskipTests $(MVN_OPTIONS)
	@echo "*** Docker tagging $(IMAGENAME) as $(FULL_IMAGE_TAG)"
	docker tag $(IMAGENAME) $(FULL_IMAGE_TAG)

release: pre-tag build
	@echo "*** Releasing now version: $(VERSION) ***"
	@echo "*** Pushing image $(FULL_IMAGE_TAG) to docker ***"
	docker push $(FULL_IMAGE_TAG)
	@echo "*** Git tagging $(IMAGENAME) as $(VERSION)"
	git tag -a "$(VERSION)" -m "Release version $(VERSION)"
	@echo "*** Pushing git tag $(VERSION) to origin ***"
	git push origin "$(VERSION)"

# Run unit tests only
test:
	@echo "*** Running unit tests ***"
	./mvnw test $(MVN_OPTIONS)

# Run integration tests only
integration-test:
	@echo "*** Running integration tests ***"
	./mvnw integration-test $(MVN_OPTIONS)

# Run all tests
verify:
	@echo "*** Running all tests (unit + integration) ***"
	./mvnw verify $(MVN_OPTIONS)

build-native: pre-tag
	@echo "*** Running integration tests ***"
	./mvnw verify -DskipTests $(MVN_OPTIONS)
	@echo "*** Building native image $(FULL_IMAGE_TAG) ***"
	./mvnw -Pnative spring-boot:build-image -DskipTests $(MVN_OPTIONS)
	@echo "*** Tagging $(IMAGENAME) as $(FULL_IMAGE_TAG)"
	docker tag $(IMAGENAME) $(FULL_IMAGE_TAG).native

push:
	@echo "*** Pushing image $(FULL_IMAGE_TAG) to docker ***"
	docker push $(FULL_IMAGE_TAG)

push-native:
	@echo "*** Pushing image $(FULL_IMAGE_TAG) ***"
	docker push $(FULL_IMAGE_TAG).native

.PHONY: clean test integration-test verify
clean: pre-tag
	@echo "*** Removing image $(FULL_IMAGE_TAG) ***"
	docker image rm $(FULL_IMAGE_TAG)