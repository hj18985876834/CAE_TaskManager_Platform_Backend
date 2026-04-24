package com.example.cae.solver.infrastructure.support;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.solver.interfaces.request.CreateFileRuleRequest;
import com.example.cae.solver.interfaces.request.UpdateFileRuleRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ProfileRuleValidator {
	private static final Set<String> ALLOWED_FILE_TYPES = Set.of("ZIP", "FILE", "DIR", "DIRECTORY");
	private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^[A-Za-z]:[\\\\/].*");

	public void validateCreateRule(CreateFileRuleRequest request) {
		if (request == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "request is empty");
		}
		if (request.getFileKey() == null || request.getFileKey().trim().isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "fileKey is empty");
		}
		validatePathPattern(request.getPathPattern());
		validateFileNamePattern(request.getFileNamePattern());
		validateFileType(request.getFileType());
	}

	public void validateUpdateRule(UpdateFileRuleRequest request) {
		if (request == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "request is empty");
		}
		validatePathPattern(request.getPathPattern());
		validateFileNamePattern(request.getFileNamePattern());
		validateFileType(request.getFileType());
	}

	private void validatePathPattern(String pathPattern) {
		String raw = trimToNull(pathPattern);
		if (raw == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "pathPattern is empty");
		}
		if (isAbsolutePath(raw) || hasParentTraversal(raw)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "pathPattern must be a relative path under taskDir and cannot contain ..");
		}
		String normalized = normalizeRelativePath(raw);
		if (normalized.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "pathPattern is empty");
		}
	}

	private void validateFileNamePattern(String fileNamePattern) {
		String raw = trimToNull(fileNamePattern);
		if (raw == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "fileNamePattern is empty");
		}
		if (raw.contains("/") || raw.contains("\\") || isAbsolutePath(raw) || hasParentTraversal(raw)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "fileNamePattern must be a file-name glob, not a path");
		}
	}

	private void validateFileType(String fileType) {
		if (fileType == null || fileType.trim().isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "fileType is empty");
		}
		String normalized = fileType.trim().toUpperCase(Locale.ROOT);
		if (!ALLOWED_FILE_TYPES.contains(normalized)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "fileType only supports ZIP, FILE, DIR or DIRECTORY");
		}
	}

	private boolean isAbsolutePath(String value) {
		return value.startsWith("/")
				|| value.startsWith("\\")
				|| WINDOWS_ABSOLUTE_PATH.matcher(value).matches();
	}

	private boolean hasParentTraversal(String value) {
		String normalized = value.replace('\\', '/').trim();
		for (String segment : normalized.split("/")) {
			if ("..".equals(segment)) {
				return true;
			}
		}
		return false;
	}

	private String normalizeRelativePath(String value) {
		String normalized = value.replace('\\', '/').trim();
		while (normalized.startsWith("./")) {
			normalized = normalized.substring(2);
		}
		return normalized;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String text = value.trim();
		return text.isEmpty() ? null : text;
	}
}
