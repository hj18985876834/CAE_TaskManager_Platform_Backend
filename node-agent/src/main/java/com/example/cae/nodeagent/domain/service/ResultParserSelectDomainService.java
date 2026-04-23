package com.example.cae.nodeagent.domain.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.nodeagent.domain.parser.ResultParser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResultParserSelectDomainService {
	private final List<ResultParser> resultParsers;

	public ResultParserSelectDomainService(List<ResultParser> resultParsers) {
		this.resultParsers = resultParsers == null ? List.of() : resultParsers;
	}

	public ResultParser select(String parserName) {
		for (ResultParser resultParser : resultParsers) {
			if (resultParser != null && resultParser.supports(parserName)) {
				return resultParser;
			}
		}
		throw new BizException(
				ErrorCodeConstants.BAD_REQUEST,
				"no result parser found for parserName: " + parserName
		);
	}
}
