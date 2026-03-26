package com.example.cae.task.infrastructure.persistence.repository;

import com.example.cae.task.domain.model.TaskResultSummary;
import com.example.cae.task.domain.repository.TaskResultSummaryRepository;
import com.example.cae.task.infrastructure.persistence.entity.TaskResultSummaryPO;
import com.example.cae.task.infrastructure.persistence.mapper.TaskResultSummaryMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TaskResultSummaryRepositoryImpl implements TaskResultSummaryRepository {
	private final TaskResultSummaryMapper taskResultSummaryMapper;

	public TaskResultSummaryRepositoryImpl(TaskResultSummaryMapper taskResultSummaryMapper) {
		this.taskResultSummaryMapper = taskResultSummaryMapper;
	}

	@Override
	public void saveOrUpdate(TaskResultSummary summary) {
		TaskResultSummaryPO exists = taskResultSummaryMapper.selectByTaskId(summary.getTaskId());
		TaskResultSummaryPO po = toPO(summary);
		if (exists == null) {
			taskResultSummaryMapper.insert(po);
		} else {
			po.setId(exists.getId());
			taskResultSummaryMapper.updateById(po);
		}
	}

	@Override
	public Optional<TaskResultSummary> findByTaskId(Long taskId) {
		return Optional.ofNullable(taskResultSummaryMapper.selectByTaskId(taskId)).map(this::toDomain);
	}

	private TaskResultSummary toDomain(TaskResultSummaryPO po) {
		TaskResultSummary summary = new TaskResultSummary();
		summary.setId(po.getId());
		summary.setTaskId(po.getTaskId());
		summary.setSuccessFlag(po.getSuccessFlag());
		summary.setDurationSeconds(po.getDurationSeconds());
		summary.setSummaryText(po.getSummaryText());
		summary.setMetricsJson(po.getMetricsJson());
		summary.setCreatedAt(po.getCreatedAt());
		summary.setUpdatedAt(po.getUpdatedAt());
		return summary;
	}

	private TaskResultSummaryPO toPO(TaskResultSummary summary) {
		TaskResultSummaryPO po = new TaskResultSummaryPO();
		po.setTaskId(summary.getTaskId());
		po.setSuccessFlag(summary.getSuccessFlag());
		po.setDurationSeconds(summary.getDurationSeconds());
		po.setSummaryText(summary.getSummaryText());
		po.setMetricsJson(summary.getMetricsJson());
		return po;
	}
}

