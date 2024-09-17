package org.zim.gamsapi.DigitalObject;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class ConstraintViolationTest extends UnitTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }


    @Test
    public void shouldRaiseNoConstraintViolation() {
        DigitalObject digitalObject = new DigitalObjectBuilder()
            .project(Project.builder().projectAbbr("foo").build())
            .id("foo")
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build();

        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet, is(empty()));
    }

    @Test
    public void shouldRaiseConstraintViolationIfPidIsNull() {
        DigitalObject digitalObject = new DigitalObject();
        digitalObject.setId(null);
        digitalObject.setProject(Project.builder().projectAbbr("Foo").build());

        digitalObject.setBaseMetadata(TestMetadataBaseEntity.generate());

        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet.size(), is(1));
    }


    @Test
    public void shouldRaiseConstraintViolationIfMetadataIsEmpty() {
        MetadataBaseEntity metadataBaseEntity = new MetadataBaseEntity();
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(4));
    }

    @Test
    public void shouldNotRaiseConstraintViolationIfMetadataDescriptionIsAnEmptyString() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set a description that is too short
        metadataBaseEntity.setDescription("");
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(0));
    }

    @Test
    public void shouldNotRaiseConstraintViolationIfMetadataDescriptionIsNull() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set a description that is too short
        metadataBaseEntity.setDescription(null);
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(0));
    }

    @Test
    public void shouldRaiseConstraintViolationIfMetadataPublisherIsEmpty() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set publisher to empty string
        metadataBaseEntity.setPublisher("");
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(1));
    }

    @Test
    public void shouldRaiseConstraintViolationIfMetadataCreatorIsNull() {
        MetadataBaseEntity metadataBaseEntity = TestMetadataBaseEntity.generate();
        // set creator to null
        metadataBaseEntity.setCreator(null);
        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(1));
    }

}
