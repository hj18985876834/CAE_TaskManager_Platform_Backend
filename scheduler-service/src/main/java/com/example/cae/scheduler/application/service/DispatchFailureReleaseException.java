package com.example.cae.scheduler.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;

public class DispatchFailureReleaseException extends BizException {
	public DispatchFailureReleaseException(String message, Throwable cause) {
		super(ErrorCodeConstants.BAD_GATEWAY, message, cause);
	}
}
