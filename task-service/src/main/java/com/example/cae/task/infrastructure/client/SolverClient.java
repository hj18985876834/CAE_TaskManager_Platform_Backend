package com.example.cae.task.infrastructure.client;

import com.example.cae.common.dto.FileRuleDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SolverClient {
	public Object getProfileDetail(Long profileId) {
		return null;
	}

	public List<FileRuleDTO> getFileRules(Long profileId) {
		return Collections.emptyList();
	}

	public Object getUploadSpec(Long profileId) {
		return null;
	}
}

