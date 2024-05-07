package org.zim.gamsapi;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetadataBaseEntityBuilder {

  private final MetadataBaseEntity metadataBaseEntity = new MetadataBaseEntity();

  public MetadataBaseEntityBuilder title(String title) {
    metadataBaseEntity.setTitle(title);
    return this;
  }

  public MetadataBaseEntityBuilder rights(String rights) {
    metadataBaseEntity.setRights(rights);
    return this;
  }

  public MetadataBaseEntityBuilder publisher(String publisher) {
    metadataBaseEntity.setPublisher(publisher);
    return this;
  }

  public MetadataBaseEntityBuilder creator(String creator) {
    metadataBaseEntity.setCreator(creator);
    return this;
  }

  public MetadataBaseEntityBuilder description(String description) {
    metadataBaseEntity.setDescription(description);
    return this;
  }

  public MetadataBaseEntity build() {
    if((metadataBaseEntity.getTitle() == null) || metadataBaseEntity.getTitle().isEmpty()){
      String msg = String.format("MetadataBaseEntity's title must not be null or empty! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((metadataBaseEntity.getRights() == null) || metadataBaseEntity.getRights().isEmpty()){
      String msg = String.format("MetadataBaseEntity's rights must not be null or empty! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((metadataBaseEntity.getPublisher() == null) || metadataBaseEntity.getPublisher().isEmpty()){
      String msg = String.format("MetadataBaseEntity's publisher must not be null or empty! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((metadataBaseEntity.getCreator() == null) || metadataBaseEntity.getCreator().isEmpty()){
      String msg = String.format("MetadataBaseEntity's creator must not be null or empty! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    if((metadataBaseEntity.getDescription() == null) || metadataBaseEntity.getDescription().isEmpty()){
      String msg = String.format("MetadataBaseEntity's description must not be null or empty! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    return metadataBaseEntity;
  }
}