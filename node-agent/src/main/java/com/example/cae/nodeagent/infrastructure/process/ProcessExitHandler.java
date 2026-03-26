package com.example.cae.nodeagent.infrastructure.process;

import com.example.cae.common.exception.BizException;
import org.springframework.stereotype.Component;

@Component
public class ProcessExitHandler {
	public void checkExitCode(int exitCode) {
		if (exitCode != 0) {
			throw new BizException("solver process failed, exitCode=" + exitCode);
		}
	}
}

