DELETE FROM datastream
WHERE digital_object_id IN (
    SELECT dig_obj.id
    FROM digital_object dig_obj
             JOIN project cur_proj ON cur_proj.project_abbr = dig_obj.project_project_abbr
    WHERE cur_proj.project_abbr = 'demo'
)