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

  public MetadataBaseEntityBuilder creator(String creator) {
    metadataBaseEntity.setCreator(creator);
    return this;
  }

  public MetadataBaseEntityBuilder description(String description) {
    metadataBaseEntity.setDescription(description);
    return this;
  }

  public MetadataBaseEntityBuilder md5Checksum(String md5Checksum) {
    if(md5Checksum.length() != 32){
      String msg = String.format("md5Checksum must be 32 characters long! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalArgumentException(msg);
    }
    metadataBaseEntity.setMd5Checksum(md5Checksum);
    return this;
  }

  public MetadataBaseEntityBuilder sha512Checksum(String sha512Checksum) {
    if(sha512Checksum.length() != 128){
      String msg = String.format("sha512Checksum must be 128 characters long! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalArgumentException(msg);
    }
    metadataBaseEntity.setSha512Checksum(sha512Checksum);
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

    if((metadataBaseEntity.getCreator() == null) || metadataBaseEntity.getCreator().isEmpty()){
      String msg = String.format("MetadataBaseEntity's creator must not be null or empty! Happened at class %s", this.getClass().getName());
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    return metadataBaseEntity;
  }
}