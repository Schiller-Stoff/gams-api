package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces.ArchivalRecordCompactView;

import java.time.Instant;

/**
 * DTO for transferring archival records to clients
 */
@Data
public class ArchivalRecordDto {

  private String pid;

  private String objectId;

  private Instant publicationTimeStamp;

  private Instant created;

  private String externalId;

  @NotNull
  private ArchivalState archivalState;

  /**
   * Creates an ArchivalRecordDto from an archivalRecordCompact spring projection
   * @param archivalRecordCompactView spring projection to be converted
   * @return converted ArchivalRecordDto
   */
  public static ArchivalRecordDto from(ArchivalRecordCompactView archivalRecordCompactView){
    ArchivalRecordDto archivalRecordDto = new ArchivalRecordDto();
    archivalRecordDto.setPid(archivalRecordCompactView.getPid());
    archivalRecordDto.setExternalId(archivalRecordCompactView.getExternalId());
    archivalRecordDto.setPublicationTimeStamp(archivalRecordCompactView.getPublicationTimeStamp());
    archivalRecordDto.setArchivalState(archivalRecordCompactView.getArchivalState());
    archivalRecordDto.setObjectId(archivalRecordCompactView.getDigitalObjectId());
    archivalRecordDto.setCreated(archivalRecordCompactView.getCreated());
    return archivalRecordDto;
  }

}
