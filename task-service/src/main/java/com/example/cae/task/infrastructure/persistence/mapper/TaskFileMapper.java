package com.example.cae.task.infrastructure.persistence.mapper;

import com.example.cae.task.infrastructure.persistence.entity.TaskFilePO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskFileMapper {
	@Options(useGeneratedKeys = true, keyProperty = "id")
	@Insert("INSERT INTO task_file(task_id, file_role, file_key, origin_name, storage_path, file_size, file_suffix, checksum) VALUES(#{taskId}, #{fileRole}, #{fileKey}, #{originName}, #{storagePath}, #{fileSize}, #{fileSuffix}, #{checksum})")
	int insert(TaskFilePO po);

	@Select("SELECT id, task_id AS taskId, file_role AS fileRole, file_key AS fileKey, origin_name AS originName, storage_path AS storagePath, file_size AS fileSize, file_suffix AS fileSuffix, checksum, created_at AS createdAt FROM task_file WHERE task_id = #{taskId} ORDER BY id ASC")
	List<TaskFilePO> selectByTaskId(Long taskId);
}
