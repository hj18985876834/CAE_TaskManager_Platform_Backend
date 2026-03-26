package com.example.cae.task.infrastructure.persistence.mapper;

import com.example.cae.task.infrastructure.persistence.entity.TaskResultFilePO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskResultFileMapper {
	@Insert("INSERT INTO task_result_file(task_id, file_type, file_name, storage_path, file_size) VALUES(#{taskId}, #{fileType}, #{fileName}, #{storagePath}, #{fileSize})")
	int insert(TaskResultFilePO po);

	@Select("SELECT id, task_id AS taskId, file_type AS fileType, file_name AS fileName, storage_path AS storagePath, file_size AS fileSize, created_at AS createdAt FROM task_result_file WHERE task_id = #{taskId} ORDER BY id ASC")
	List<TaskResultFilePO> selectByTaskId(Long taskId);

	@Select("SELECT id, task_id AS taskId, file_type AS fileType, file_name AS fileName, storage_path AS storagePath, file_size AS fileSize, created_at AS createdAt FROM task_result_file WHERE id = #{fileId} LIMIT 1")
	TaskResultFilePO selectById(Long fileId);
}

