package com.example.cae.task.infrastructure.persistence.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.task.application.assembler.TaskAssembler;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.infrastructure.persistence.mapper.TaskMapper;
import com.example.cae.task.interfaces.request.TaskListQueryRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskRepositoryImpl implements TaskRepository {
	private final TaskMapper taskMapper;
	private final TaskAssembler taskAssembler;

	public TaskRepositoryImpl(TaskMapper taskMapper, TaskAssembler taskAssembler) {
		this.taskMapper = taskMapper;
		this.taskAssembler = taskAssembler;
	}

	@Override
	public Optional<Task> findById(Long taskId) {
		return Optional.ofNullable(taskMapper.selectById(taskId)).map(taskAssembler::fromPO);
	}

	@Override
	public void save(Task task) {
		var po = taskAssembler.toPO(task);
		taskMapper.insert(po);
		task.setId(po.getId());
	}

	@Override
	public void update(Task task) {
		taskMapper.updateById(taskAssembler.toPO(task));
	}

	@Override
	public PageResult<Task> pageMyTasks(TaskListQueryRequest request, Long userId) {
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;
		long total = taskMapper.countMyPage(request, userId);
		List<Task> records = taskMapper.selectMyPage(request, userId, offset, pageSize).stream().map(taskAssembler::fromPO).toList();
		return PageResult.of(total, pageNum, pageSize, records);
	}

	@Override
	public PageResult<Task> pageAdminTasks(TaskListQueryRequest request) {
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;
		long total = taskMapper.countAdminPage(request);
		List<Task> records = taskMapper.selectAdminPage(request, offset, pageSize).stream().map(taskAssembler::fromPO).toList();
		return PageResult.of(total, pageNum, pageSize, records);
	}

	@Override
	public List<Task> listByStatus(String status) {
		return taskMapper.selectByStatus(status).stream().map(taskAssembler::fromPO).toList();
	}

	@Override
	public List<Task> listByNodeIdAndStatuses(Long nodeId, List<String> statuses) {
		return taskMapper.selectByNodeIdAndStatuses(nodeId, statuses).stream().map(taskAssembler::fromPO).toList();
	}

	@Override
	public long countAll() {
		return taskMapper.countAll();
	}

	@Override
	public long countByStatus(String status) {
		return taskMapper.countByStatus(status);
	}

	@Override
	public long countFinished() {
		return taskMapper.countFinished();
	}

	@Override
	public List<Task> listStaleUnsubmittedTasks(LocalDateTime updatedBefore, int limit) {
		return taskMapper.selectStaleUnsubmittedTasks(updatedBefore, limit).stream().map(taskAssembler::fromPO).toList();
	}
}
