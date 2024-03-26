package org.zim.gamsapi.System.bootstrap;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MimeTypeUtils;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.System.configproperties.GAMSAPIProperties;
import org.zim.gamsapi.System.security.GAMSAPISecurityRoles;
import org.zim.gamsapi.System.utils.DigitalObjectBuilder;
import org.zim.gamsapi.User.User;
import org.zim.gamsapi.User.interfaces.IUserRepository;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
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

    DigitalObject teiObject = new DigitalObjectBuilder("testtei")
            .withObjectType("TEI")
            .build();
    digitalObjectRepository.save(teiObject);

    Datastream teiSource = Datastream.builder()
            .dsid("TEI_SOURCE")
            .data("test".getBytes())
            .digitalObject(teiObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .build();

    datastreamRepository.save(teiSource);
    //teiObject.setDatastreams(new ArrayList<>(List.of(teiSource)));

    DigitalObject lidoObject = new DigitalObjectBuilder("testlido")
            .withObjectType("LIDO")
            .addBaseMetadata()
              .withTitle("A LIDO object title")
              .withCreator("Sebastian David Schiller-Stoff")
              //.withContributor(new ArrayList<>(List.of("Sebastian David Schiller-Stoff", "Moria")))
              .withDescription("This is a very beautiful LIDO object ... containing many descriptions of stuff ...")
              .withPublisher("Universität Graz")
              //.withSubject(new ArrayList<>(List.of("History", "Art History")))
              //.withLanguage(new ArrayList<>(List.of("DE")))
              .withRights("Creative Commons BY-NC 4.0")
              .add()
            .build();
    digitalObjectRepository.save(lidoObject);

    Datastream lidoSource = Datastream.builder()
            .dsid("LIDO_SOURCE")
            .data("test".getBytes())
            .digitalObject(lidoObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .baseMetadata(
                    MetadataBaseEntity
                            .builder()
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

    Datastream image = Datastream.builder()
            .dsid("IMAGE_1")
            .data("test".getBytes())
            .digitalObject(lidoObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .baseMetadata(
                    MetadataBaseEntity
                            .builder()
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

    DigitalObject gmlObject = new DigitalObjectBuilder("testgml")
            .withObjectType("GML")
            .build();
    digitalObjectRepository.save(gmlObject);

    Datastream gmlImage = Datastream.builder()
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

    // added hardcoded admin user here
    Optional<User> adminOptional = userRepository.findByUsername(GAMSAPIProperties.ADMIN_USER_NAME.name);
    User admin;

    if(adminOptional.isEmpty()){

      String generatedPassword = RandomStringUtils.random(20, true, true);

      admin = User.builder()
              .username(GAMSAPIProperties.ADMIN_USER_NAME.name)
              .password(
                //passwordEncoder.encode(generatedPassword)
                passwordEncoder.encode("admin")
              )
              .roles(new HashSet<>(Set.of(GAMSAPISecurityRoles.ADMINISTRATOR.name)))
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

      System.out.println("*** Generated admin password : " + generatedPassword);
      log.info("*** Successfully initialized admin user {} for project {}. Assigned project user: {} ***",admin, project.getProjectAbbr(), project.getUsers());

    } else {
      log.info("*** Admin user and project already initialized. Skipping process... ***");
    }
  }



}
