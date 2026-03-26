package com.example.cae.scheduler.domain.strategy;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.scheduler.domain.model.ComputeNode;

import java.util.List;

public interface ScheduleStrategy {
	ComputeNode selectNode(TaskDTO task, List<ComputeNode> nodes);
}

