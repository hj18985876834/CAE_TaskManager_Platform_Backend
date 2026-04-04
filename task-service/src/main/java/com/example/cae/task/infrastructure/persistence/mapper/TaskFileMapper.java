package com.example.cae.task.infrastructure.persistence.mapper;

import com.example.cae.task.infrastructure.persistence.entity.TaskFilePO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TaskFileMapper {
	@Options(useGeneratedKeys = true, keyProperty = "id")
	@Insert("INSERT INTO task_file(task_id, file_role, file_key, origin_name, storage_path, unpack_dir, relative_path, archive_flag, file_size, file_suffix, checksum) VALUES(#{taskId}, #{fileRole}, #{fileKey}, #{originName}, #{storagePath}, #{unpackDir}, #{relativePath}, #{archiveFlag}, #{fileSize}, #{fileSuffix}, #{checksum})")
	int insert(TaskFilePO po);

	@Select("SELECT id, task_id AS taskId, file_role AS fileRole, file_key AS fileKey, origin_name AS originName, storage_path AS storagePath, unpack_dir AS unpackDir, relative_path AS relativePath, archive_flag AS archiveFlag, file_size AS fileSize, file_suffix AS fileSuffix, checksum, created_at AS createdAt FROM task_file WHERE task_id = #{taskId} ORDER BY id ASC")
	List<TaskFilePO> selectByTaskId(Long taskId);

	@Update("UPDATE task_file SET file_role = #{fileRole}, file_key = #{fileKey}, origin_name = #{originName}, storage_path = #{storagePath}, unpack_dir = #{unpackDir}, relative_path = #{relativePath}, archive_flag = #{archiveFlag}, file_size = #{fileSize}, file_suffix = #{fileSuffix}, checksum = #{checksum} WHERE id = #{id}")
	int updateById(TaskFilePO po);
}
