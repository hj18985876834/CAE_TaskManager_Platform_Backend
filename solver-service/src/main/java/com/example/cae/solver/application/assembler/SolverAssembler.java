package com.example.cae.solver.application.assembler;

import com.example.cae.solver.domain.model.SolverDefinition;
import com.example.cae.solver.infrastructure.persistence.entity.SolverDefinitionPO;
import com.example.cae.solver.interfaces.request.CreateSolverRequest;
import com.example.cae.solver.interfaces.response.SolverDetailResponse;
import com.example.cae.solver.interfaces.response.SolverListItemResponse;

public class SolverAssembler {
	private SolverAssembler() {
	}

	public static SolverDefinition toSolver(CreateSolverRequest request) {
		SolverDefinition solver = new SolverDefinition();
		solver.setSolverCode(request.getSolverCode());
		solver.setSolverName(request.getSolverName());
		solver.setVersion(request.getVersion());
		solver.setExecMode(request.getExecMode());
		solver.setExecPath(request.getExecPath());
		solver.setRemark(request.getRemark());
		return solver;
	}

	public static SolverListItemResponse toListItemResponse(SolverDefinition solver) {
		SolverListItemResponse response = new SolverListItemResponse();
		response.setId(solver.getId());
		response.setSolverId(solver.getId());
		response.setSolverCode(solver.getSolverCode());
		response.setSolverName(solver.getSolverName());
		response.setVersion(solver.getVersion());
		response.setExecMode(solver.getExecMode());
		response.setEnabled(solver.getEnabled());
		response.setDescription(solver.getRemark());
		return response;
	}

	public static SolverDetailResponse toDetailResponse(SolverDefinition solver) {
		SolverDetailResponse response = new SolverDetailResponse();
		response.setSolverId(solver.getId());
		response.setSolverCode(solver.getSolverCode());
		response.setSolverName(solver.getSolverName());
		response.setVersion(solver.getVersion());
		response.setExecMode(solver.getExecMode());
		response.setExecPath(solver.getExecPath());
		response.setEnabled(solver.getEnabled());
		response.setRemark(solver.getRemark());
		return response;
	}

	public static SolverDefinition fromPO(SolverDefinitionPO po) {
		SolverDefinition solver = new SolverDefinition();
		solver.setId(po.getId());
		solver.setSolverCode(po.getSolverCode());
		solver.setSolverName(po.getSolverName());
		solver.setVersion(po.getVersion());
		solver.setExecMode(po.getExecMode());
		solver.setExecPath(po.getExecPath());
		solver.setEnabled(po.getEnabled());
		solver.setRemark(po.getRemark());
		solver.setCreatedAt(po.getCreatedAt());
		solver.setUpdatedAt(po.getUpdatedAt());
		return solver;
	}

	public static SolverDefinitionPO toPO(SolverDefinition solver) {
		SolverDefinitionPO po = new SolverDefinitionPO();
		po.setId(solver.getId());
		po.setSolverCode(solver.getSolverCode());
		po.setSolverName(solver.getSolverName());
		po.setVersion(solver.getVersion());
		po.setExecMode(solver.getExecMode());
		po.setExecPath(solver.getExecPath());
		po.setEnabled(solver.getEnabled());
		po.setRemark(solver.getRemark());
		return po;
	}
}
