package org.zim.gamsapi.System.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MimeTypeUtils;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.System.configproperties.GAMSAPIProperties;
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

  @Override
  public void run(String... args) {
    log.info("*** Start bootstrapping gams-api ...");
    logAvailableSystemResources();
    // initializeDemoProject();
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
            .digitalObject(gmlObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .build();
    datastreamRepository.save(gmlImage);

    //lidoObject.setDatastreams(new ArrayList<>(List.of(lidoSource, image)));
    //digitalObjectRepository.save(teiObject);
    //digitalObjectRepository.save(lidoObject);

  }


  /**
   * Saves the demo project to the database.
   */
  public void initializeDemoProject(){

    Project project = new Project();
    project.setProjectAbbr(GAMSAPIProperties.DEMO_PROJECT_ABBR.name);
    project.setDescription("Demo admin project");

    Optional<Project> projectOptional = projectRepository.findById(project.getProjectAbbr());
    if(projectOptional.isEmpty()){
      project = projectRepository.save(project);
    }

    log.info("*** Successfully initialized demo project {}***", project.getProjectAbbr());
  }



}
