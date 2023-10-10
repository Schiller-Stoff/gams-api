package org.zim.gamsapi.System.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
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
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class DigitalObjectInitializer implements CommandLineRunner {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;
  private final IProjectRepository projectRepository;

  @Override
  public void run(String... args) {
    log.info("*** Start bootstrapping gams-api ...");
    logAvailableSystemResources();
    initializeProjects();
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

    DigitalObject teiObject = DigitalObject.builder()
            .id("testtei")
            .objectType("TEI")
            .build();
    digitalObjectRepository.save(teiObject);

    Datastream teiSource = Datastream.builder()
            .dsid("TEI_SOURCE")
            .data("test".getBytes())
            .digitalObject(teiObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .build();

    datastreamRepository.save(teiSource);
    //teiObject.setDatastreams(List.of(teiSource));

    DigitalObject lidoObject = DigitalObject.builder()
            .id("testlido")
            .objectType("LIDO")
            .baseMetadata(
                    MetadataBaseEntity
                            .builder()
                            .title("A LIDO object title")
                            .creator(List.of("Sebastian David Schiller-Stoff"))
                            .contributor(List.of("Sebastian David Schiller-Stoff", "Moria"))
                            .description("This is a very beautiful LIDO object ... containing many descriptions of stuff ...")
                            .publisher(List.of("ZIM Graz", "Universität Graz"))
                            .subject(List.of("History", "Art History"))
                            .language(List.of("DE"))
                            .rights("Creative Commons BY-NC 4.0")
                            .build()
            )
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
                            .creator(List.of("Eva Musterfrau", "Ada Lovelace"))
                            .subject(List.of("Chemistry", "Physics", "Architecture"))
                            .description("This source datastream contains some information about...")
                            .language(List.of("de"))
                            .type(List.of("Building"))
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
                            .creator(List.of("Eva Musterfrau", "Ada Lovelace"))
                            .subject(List.of("Chemistry", "Physics", "Architecture"))
                            .description("This source datastream contains some information about...")
                            .language(List.of("de"))
                            .type(List.of("Building"))
                            .rights("Creative Commons BY-NC 4.0")
                            .build()
            )
            .build();

    datastreamRepository.save(image);

    DigitalObject gmlObject = DigitalObject.builder()
            .id("testgml")
            .objectType("GML")
            .build();
    digitalObjectRepository.save(gmlObject);

    Datastream gmlImage = Datastream.builder()
            .dsid("IMAGE_1")
            .data("test".getBytes())
            .digitalObject(gmlObject)
            .mimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
            .build();
    datastreamRepository.save(gmlImage);

    //lidoObject.setDatastreams(List.of(lidoSource, image));
    //digitalObjectRepository.save(teiObject);
    //digitalObjectRepository.save(lidoObject);

  }

  private void initializeProjects(){

    Project project = new Project();
    project.setProjectAbbr("admin");
    project.setDescription("Demo admin project");

    Optional<Project> projectOptional = projectRepository.findById(project.getProjectAbbr());
    if(projectOptional.isEmpty()){
      projectRepository.save(project);
    }


  }

}
