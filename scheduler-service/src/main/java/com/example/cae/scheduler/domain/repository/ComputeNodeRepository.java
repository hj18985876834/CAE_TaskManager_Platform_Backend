package com.example.cae.scheduler.domain.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;

import java.util.List;
import java.util.Optional;

public interface ComputeNodeRepository {
	Optional<ComputeNode> findById(Long nodeId);

	Optional<ComputeNode> findByNodeCode(String nodeCode);

	Optional<ComputeNode> findByNodeToken(String nodeToken);

	void save(ComputeNode node);

	void update(ComputeNode node);

	PageResult<ComputeNode> page(NodePageQueryRequest request, long offset, int pageSize);

	List<ComputeNode> listByStatus(String status);
}
