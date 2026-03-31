package com.example.cae.scheduler.infrastructure.persistence.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.assembler.NodeAssembler;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.infrastructure.persistence.entity.ComputeNodePO;
import com.example.cae.scheduler.infrastructure.persistence.mapper.ComputeNodeMapper;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ComputeNodeRepositoryImpl implements ComputeNodeRepository {
	private final ComputeNodeMapper computeNodeMapper;

	public ComputeNodeRepositoryImpl(ComputeNodeMapper computeNodeMapper) {
		this.computeNodeMapper = computeNodeMapper;
	}

	@Override
	public Optional<ComputeNode> findById(Long nodeId) {
		return Optional.ofNullable(computeNodeMapper.selectById(nodeId)).map(NodeAssembler::fromPO);
	}

	@Override
	public Optional<ComputeNode> findByNodeCode(String nodeCode) {
		return Optional.ofNullable(computeNodeMapper.selectByNodeCode(nodeCode)).map(NodeAssembler::fromPO);
	}

	@Override
	public Optional<ComputeNode> findByNodeToken(String nodeToken) {
		return Optional.ofNullable(computeNodeMapper.selectByNodeToken(nodeToken)).map(NodeAssembler::fromPO);
	}

	@Override
	public void save(ComputeNode node) {
		ComputeNodePO po = NodeAssembler.toPO(node);
		computeNodeMapper.insert(po);
		node.setId(po.getId());
	}

	@Override
	public void update(ComputeNode node) {
		computeNodeMapper.updateById(NodeAssembler.toPO(node));
	}

	@Override
	public PageResult<ComputeNode> page(NodePageQueryRequest request, long offset, int pageSize) {
		long total = computeNodeMapper.countPage(request);
		List<ComputeNode> records = computeNodeMapper.selectPage(request, offset, pageSize).stream().map(NodeAssembler::fromPO).toList();
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		return PageResult.of(total, pageNum, pageSize, records);
	}

	@Override
	public List<ComputeNode> listByStatus(String status) {
		return computeNodeMapper.selectByStatus(status).stream().map(NodeAssembler::fromPO).toList();
	}
}
