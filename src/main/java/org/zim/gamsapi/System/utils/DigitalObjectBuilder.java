package org.zim.gamsapi.System.utils;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;
import java.util.Set;

@Slf4j
/**
 * Builder for DigitalObject.
 */
public class DigitalObjectBuilder {

    DigitalObject digitalObject = new DigitalObject();

    public DigitalObjectBuilder(String id) {
        digitalObject.setId(id);
    }

    public DigitalObject build() {
        if(digitalObject.getProject() == null){
            String msg = String.format("Project missing: Digital object must have a valid project assigned to it. Don't forget to call the appropriate builder method. %s", digitalObject);
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // TODO call validation here?

        return digitalObject;
    }


    public DatastreamBuilder addDatastream(String dsid) {
        return new DatastreamBuilder(dsid);
    }

    public ProjectBuilder addProject(String projectAbbr) {
        return new ProjectBuilder(projectAbbr);
    }


    public MetadataBaseEntityBuilder addBaseMetadata() {
        return new MetadataBaseEntityBuilder();
    }

    public DigitalObjectBuilder withChildObjects(Set<DigitalObject> childObjects) {
        digitalObject.setChildObjects(childObjects);
        return this;
    }

    public DigitalObjectBuilder withObjectType(String objectType) {
        digitalObject.setObjectType(objectType);
        return this;
    }

    public DigitalObjectBuilder withTypes(Set<String> types) {
        digitalObject.setTypes(types);
        return this;
    }

    public class DatastreamBuilder {

            Datastream datastream = new Datastream();

            public DatastreamBuilder(String id) {
                datastream.setDsid(id);

            }

            public DatastreamBuilder withData(byte[] data) {
                datastream.setData(data);
                return this;
            }

            public DatastreamBuilder withMimeType(String mimeType) {
                datastream.setMimeType(mimeType);
                return this;
            }

            public DatastreamBuilder withFileName(String fileName) {
                datastream.setFileName(fileName);
                return this;
            }

            public DatastreamBuilder withSize(Long size) {
                datastream.setSize(size);
                return this;
            }

            public DatastreamBuilder withType(String type) {
                datastream.setType(type);
                return this;
            }

            public DatastreamBuilder withMetadataBaseEnity(MetadataBaseEntity metadataBaseEnity) {
                datastream.setBaseMetadata(metadataBaseEnity);
                return this;
            }

            public DigitalObjectBuilder add(){
                // establishes bidirectional relationship
                digitalObject.addDatastream(datastream);

                // TODO call validation here?

                return DigitalObjectBuilder.this;
            }
            public Datastream build() {
                return datastream;
            }

    }

    public class ProjectBuilder {

        Project project = new Project();

        public ProjectBuilder(String projectAbbr) {
            project.setProjectAbbr(projectAbbr);
        }

        public ProjectBuilder withDescription(String description) {
            project.setDescription(description);
            return this;
        }

        public DigitalObjectBuilder add() {
            // establishes bidirectional relationship
            project.addDigitalObject(digitalObject);

            //TODO call validation methods here?

            return DigitalObjectBuilder.this;
        }
    }


    public class MetadataBaseEntityBuilder {

        MetadataBaseEntity metadataBaseEntity = new MetadataBaseEntity();

        public MetadataBaseEntityBuilder withTitle(String title) {
            metadataBaseEntity.setTitle(title);
            return this;
        }

        public MetadataBaseEntityBuilder withRights(String rights) {
            metadataBaseEntity.setRights(rights);
            return this;
        }

        public MetadataBaseEntityBuilder withPublisher(String publisher) {
            metadataBaseEntity.setPublisher(publisher);
            return this;
        }

        public MetadataBaseEntityBuilder withCreator(String creator) {
            metadataBaseEntity.setCreator(creator);
            return this;
        }

        public MetadataBaseEntityBuilder withDescription(String description) {
            metadataBaseEntity.setDescription(description);
            return this;
        }

        public DigitalObjectBuilder add() {
            digitalObject.setBaseMetadata(metadataBaseEntity);
            // TODO could call some validation here?
            return DigitalObjectBuilder.this;
        }

    }



}
