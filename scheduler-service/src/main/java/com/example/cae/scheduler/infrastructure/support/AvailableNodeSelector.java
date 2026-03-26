package com.example.cae.scheduler.infrastructure.support;

import com.example.cae.scheduler.domain.model.ComputeNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AvailableNodeSelector {
	public List<ComputeNode> select(List<ComputeNode> nodes) {
		return nodes.stream().filter(ComputeNode::canDispatch).toList();
	}
}

