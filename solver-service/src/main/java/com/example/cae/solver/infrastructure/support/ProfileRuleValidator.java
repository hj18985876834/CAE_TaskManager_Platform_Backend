package com.example.cae.solver.infrastructure.support;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.solver.interfaces.request.CreateFileRuleRequest;
import com.example.cae.solver.interfaces.request.UpdateFileRuleRequest;
import org.springframework.stereotype.Component;

@Component
public class ProfileRuleValidator {
	public void validateCreateRule(CreateFileRuleRequest request) {
		if (request == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "request is empty");
		}
		if (request.getFileKey() == null || request.getFileKey().trim().isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "fileKey is empty");
		}
		if (request.getFileType() == null || request.getFileType().trim().isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "fileType is empty");
		}
	}

	public void validateUpdateRule(UpdateFileRuleRequest request) {
		if (request == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "request is empty");
		}
		if (request.getFileType() == null || request.getFileType().trim().isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "fileType is empty");
		}
	}
}
