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

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class DigitalObjectInitializer implements CommandLineRunner {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;

  @Override
  public void run(String... args) {
    log.info("*** Start bootstrapping gams-api ...");

    DigitalObject teiObject = DigitalObject.builder()
            .pid("testtei")
            .objectType("TEI")
            .projectAbbr("derla")
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
            .pid("testlido")
            .objectType("LIDO")
            .baseMetadata(
              MetadataBaseEntity
                .builder()
                .title(List.of("A LIDO object title"))
                .creator(List.of("Sebastian David Schiller-Stoff"))
                .contributor(List.of("Sebastian David Schiller-Stoff", "Moria"))
                .description("This is a very beautiful LIDO object ... containing many descriptions of stuff ...")
                .publisher(List.of("ZIM Graz", "Universität Graz"))
                .subject(List.of("History", "Art History"))
                .language(List.of("DE"))
                .rights(List.of("Creative Commons BY-NC 4.0", "https://creativecommons.org/licenses/by-nc/4.0"))
                .build()
            )
            .projectAbbr("derla")
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
                .title(List.of("Chair of the king"))
                .creator(List.of("Eva Musterfrau", "Ada Lovelace"))
                .subject(List.of("Chemistry", "Physics", "Architecture"))
                .description("This source datastream contains some information about...")
                .language(List.of("de"))
                .type(List.of("Building"))
                .rights(List.of("Creative Commons BY-NC 4.0", "https://creativecommons.org/licenses/by-nc/4.0"))
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
                .title(List.of("An Image of something"))
                .creator(List.of("Eva Musterfrau", "Ada Lovelace"))
                .subject(List.of("Chemistry", "Physics", "Architecture"))
                .description("This source datastream contains some information about...")
                .language(List.of("de"))
                .type(List.of("Building"))
                .rights(List.of("Creative Commons BY-NC 4.0", "https://creativecommons.org/licenses/by-nc/4.0"))
                .build()
            )
            .build();

    datastreamRepository.save(image);

    DigitalObject gmlObject = DigitalObject.builder()
            .pid("testgml")
            .objectType("GML")
            .projectAbbr("derla")
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

}
