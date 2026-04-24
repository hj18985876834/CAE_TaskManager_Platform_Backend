package com.example.cae.nodeagent.infrastructure.process;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Component
public class ProcessLogReader {
	private final Charset processLogCharset;

	public ProcessLogReader(NodeAgentConfig nodeAgentConfig) {
		this.processLogCharset = resolveCharset(nodeAgentConfig.getProcessLogCharset());
	}

	public void read(InputStream inputStream, Consumer<String> lineConsumer) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, processLogCharset))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lineConsumer.accept(line + "\n");
			}
		} catch (IOException ex) {
			throw new RuntimeException("read process log failed", ex);
		}
	}

	private Charset resolveCharset(String configuredCharset) {
		if (configuredCharset == null || configuredCharset.isBlank()) {
			return StandardCharsets.UTF_8;
		}
		try {
			return Charset.forName(configuredCharset);
		} catch (Exception ex) {
			return StandardCharsets.UTF_8;
		}
	}
}
