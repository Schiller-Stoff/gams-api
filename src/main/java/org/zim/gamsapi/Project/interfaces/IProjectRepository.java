package org.zim.gamsapi.Project.interfaces;

import org.springframework.data.repository.CrudRepository;
import org.zim.gamsapi.Project.Project;

public interface IProjectRepository extends CrudRepository<Project, String> {
}
