package org.zim.gamsapi.DigitalObject;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.UnitTest;
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
            .baseMetadata(
                new MetadataBaseEntityBuilder()
                    .title("foo")
                    .rights("foo")
                    .publisher("foo")
                    .creator("foo")
                    .description("foo-bar")
                    .build()
            )
            .build();

        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet, is(empty()));
    }

    @Test
    public void shouldRaiseConstraintViolationIfPidIsNull() {
        DigitalObject digitalObject = new DigitalObject();
        digitalObject.setId(null);
        digitalObject.setProject(Project.builder().projectAbbr("Foo").build());

        digitalObject.setBaseMetadata(
            new MetadataBaseEntityBuilder()
                .title("foo")
                .rights("foo")
                .publisher("foo")
                .creator("foo")
                .description("foo-bar")
                .build()
        );

        Set<ConstraintViolation<DigitalObject>> violationSet = validator.validate(digitalObject);
        assertThat(violationSet.size(), is(1));
    }


    @Test
    public void shouldRaiseConstraintViolationIfMetadataIsEmpty() {
        MetadataBaseEntity metadataBaseEntity = new MetadataBaseEntity();

        Set<ConstraintViolation<MetadataBaseEntity>> violationSet = validator.validate(metadataBaseEntity);
        assertThat(violationSet.size(), is(5));
    }
}
