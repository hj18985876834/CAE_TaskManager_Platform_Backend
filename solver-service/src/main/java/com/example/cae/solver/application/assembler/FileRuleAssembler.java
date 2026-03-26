package com.example.cae.solver.application.assembler;

import com.example.cae.solver.domain.model.SolverProfileFileRule;
import com.example.cae.solver.infrastructure.persistence.entity.SolverProfileFileRulePO;
import com.example.cae.solver.interfaces.request.CreateFileRuleRequest;
import com.example.cae.solver.interfaces.response.FileRuleResponse;

public class FileRuleAssembler {
	private FileRuleAssembler() {
	}

	public static SolverProfileFileRule toRule(Long profileId, CreateFileRuleRequest request) {
		SolverProfileFileRule rule = new SolverProfileFileRule();
		rule.setProfileId(profileId);
		rule.setFileKey(request.getFileKey());
		rule.setFileNamePattern(request.getFileNamePattern());
		rule.setFileType(request.getFileType());
		rule.setRequiredFlag(request.getRequiredFlag());
		rule.setSortOrder(request.getSortOrder());
		rule.setRemark(request.getRemark());
		return rule;
	}

	public static FileRuleResponse toResponse(SolverProfileFileRule rule) {
		FileRuleResponse response = new FileRuleResponse();
		response.setRuleId(rule.getId());
		response.setProfileId(rule.getProfileId());
		response.setFileKey(rule.getFileKey());
		response.setFileNamePattern(rule.getFileNamePattern());
		response.setFileType(rule.getFileType());
		response.setRequiredFlag(rule.getRequiredFlag());
		response.setSortOrder(rule.getSortOrder());
		response.setRemark(rule.getRemark());
		return response;
	}

	public static SolverProfileFileRule fromPO(SolverProfileFileRulePO po) {
		SolverProfileFileRule rule = new SolverProfileFileRule();
		rule.setId(po.getId());
		rule.setProfileId(po.getProfileId());
		rule.setFileKey(po.getFileKey());
		rule.setFileNamePattern(po.getFileNamePattern());
		rule.setFileType(po.getFileType());
		rule.setRequiredFlag(po.getRequiredFlag());
		rule.setSortOrder(po.getSortOrder());
		rule.setRemark(po.getRemark());
		return rule;
	}

	public static SolverProfileFileRulePO toPO(SolverProfileFileRule rule) {
		SolverProfileFileRulePO po = new SolverProfileFileRulePO();
		po.setId(rule.getId());
		po.setProfileId(rule.getProfileId());
		po.setFileKey(rule.getFileKey());
		po.setFileNamePattern(rule.getFileNamePattern());
		po.setFileType(rule.getFileType());
		po.setRequiredFlag(rule.getRequiredFlag());
		po.setSortOrder(rule.getSortOrder());
		po.setRemark(rule.getRemark());
		return po;
	}
}

