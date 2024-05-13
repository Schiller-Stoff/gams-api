package org.zim.gamsapi.System.bootstrap;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MimeTypeUtils;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.System.configproperties.GAMSAPIProperties;
import org.zim.gamsapi.System.security.GAMSAPISecurityRoles;
import org.zim.gamsapi.User.User;
import org.zim.gamsapi.User.interfaces.IUserRepository;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
@Profile("!test")
public class DigitalObjectInitializer implements CommandLineRunner {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IProjectRepository projectRepository;
  private final IUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {
    log.info("*** Start bootstrapping gams-api ...");
    logAvailableSystemResources();
    initializeAdmin();
    // saveTestData();
  }

  /**
   * Info logs available system resources.
   */
  private void logAvailableSystemResources(){

    int mb = 1024 * 1024;
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    long xmx = memoryBean.getHeapMemoryUsage().getMax() / mb;
    long xms = memoryBean.getHeapMemoryUsage().getInit() / mb;
    log.info("*** GAMS-API Initialization: Initial Memory (xms) : {}mb", xms);
    log.info("*** GAMS-API Initialization: Max Memory (xmx) : {}mb", xmx);
  }

  /**
   * Saves some test data to the database, like some digital objects and datastreams.
   */
  private void saveTestData(){

    DigitalObject teiObject = new DigitalObjectBuilder()
        .id("testtei")
        .objectType("TEI")
        .build();
    digitalObjectRepository.save(teiObject);

    Datastream teiSource = new DatastreamBuilder()
            .dsid("TEI_SOURCE")
            .data("test".getBytes())
            .digitalObject(teiObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .build();

    datastreamRepository.save(teiSource);

    DigitalObject lidoObject = new DigitalObjectBuilder()
        .objectType("testlido")
        .baseMetadata(
            new MetadataBaseEntityBuilder()
                .title("Digital object representing a Chair of the king")
                .creator("Ada Lovelace")
                .description("This source datastream contains some information about...")
                .publisher("Universität Graz")
                .rights("Creative Commons BY-NC 4.0")
                .build())
        .build();

    digitalObjectRepository.save(lidoObject);

    Datastream lidoSource = new DatastreamBuilder()
            .dsid("LIDO_SOURCE")
            .data("test".getBytes())
            .digitalObject(lidoObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .baseMetadata(
                  new MetadataBaseEntityBuilder()
                            .title("Digital object representing a Chair of the king")
                            .creator("Ada Lovelace")
                            //.subject(new ArrayList<>(List.of("Chemistry", "Physics", "Architecture")))
                            .description("This source datastream contains some information about...")
                            //.language(new ArrayList<>(List.of("de")))
                            //.type(new ArrayList<>(List.of("Building")))
                            .rights("Creative Commons BY-NC 4.0")
                            .build()
            )
            .build();
    datastreamRepository.save(lidoSource);

    Datastream image = new DatastreamBuilder()
            .dsid("IMAGE_1")
            .data("test".getBytes())
            .digitalObject(lidoObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .baseMetadata(
                    new MetadataBaseEntityBuilder()
                            .title("An Image of something")
                            .creator("Ada Lovelace")
                            //.subject(new ArrayList<>(List.of("Chemistry", "Physics", "Architecture")))
                            .description("This source datastream contains some information about...")
                            //.language(new ArrayList<>(List.of("de")))
                            //.type(new ArrayList<>(List.of("Building")))
                            .rights("Creative Commons BY-NC 4.0")
                            .build()
            )
            .build();

    datastreamRepository.save(image);

    DigitalObject gmlObject = new DigitalObjectBuilder()
        .id("testgml")
        .objectType("GML")
        .build();

    digitalObjectRepository.save(gmlObject);

    Datastream gmlImage = new DatastreamBuilder()
            .dsid("IMAGE_1")
            .data("test".getBytes())
            .digitalObject(gmlObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .build();
    datastreamRepository.save(gmlImage);

    //lidoObject.setDatastreams(new ArrayList<>(List.of(lidoSource, image)));
    //digitalObjectRepository.save(teiObject);
    //digitalObjectRepository.save(lidoObject);

  }


  public void initializeAdmin(){

    // TODO this whole method is outdated? initializing uers is now done in the Keycloak configuration
    // TODO remove complete method?
    // BUT: careful needs base setup linking users with projects?

    // added hardcoded admin user here
    Optional<User> adminOptional = userRepository.findByUsername(GAMSAPIProperties.ADMIN_USER_NAME.name);
    User admin;

    if(adminOptional.isEmpty()){

      admin = User.builder()
              .userid("ADMIN_ID")
              .username(GAMSAPIProperties.ADMIN_USER_NAME.name)
              .build();

      userRepository.save(admin);

      Project project = new Project();
      project.setProjectAbbr(GAMSAPIProperties.DEMO_PROJECT_ABBR.name);
      project.setDescription("Demo admin project");
      project.setUsers(new HashSet<>(Set.of(admin)));

      Optional<Project> projectOptional = projectRepository.findById(project.getProjectAbbr());
      if(projectOptional.isEmpty()){
        project = projectRepository.save(project);
      }

      admin.setProjects(new HashSet<>(Set.of(project)));
      admin = userRepository.save(admin);

      log.info("*** Successfully initialized admin user {} for project {}. Assigned project user: {} ***",admin, project.getProjectAbbr(), project.getUsers());

    } else {
      log.info("*** Admin user and project already initialized. Skipping process... ***");
    }
  }



}
