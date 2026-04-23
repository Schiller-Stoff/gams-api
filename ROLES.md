
# Authorization model of the GAMS-api

## Global roles

### Super admin role

- super_admin
- allowed to do everything
- may create / delete / change projects

### Projects admin role

- projects_admin
- may not perform certain administration tasks
- designed to be used by metadata managers responsible "for all projects"
- - may create / delete / change projects

## Project specific roles

### Project admin (of a singular project)

- e.g.: foo_project_admin

### Project editor

- e.g.: foo_project_editor

### Project viewer

- e.g.: foo_project_viewer