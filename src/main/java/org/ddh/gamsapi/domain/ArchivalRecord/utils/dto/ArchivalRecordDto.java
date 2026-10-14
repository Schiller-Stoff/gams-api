package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ddh.gamsapi.domain.ArchivalRecord.ValidPid;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces.ArchivalRecordCompactView;

import java.time.Instant;

/**
 * DTO for creating an ArchivalRecord (represents user requests)
 */
@Data
public class ArchivalRecordDto {
  @ValidPid
  private String pid;

  private String objectId;

  private Instant publicationTimeStamp;

  private Instant created;

  private String externalId;

  @NotNull
  private ArchivalState archivalState;

  /**
   * TODO jdoc
   * TODO unittest this method
   * @param archivalRecordCompactView
   * @return
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
