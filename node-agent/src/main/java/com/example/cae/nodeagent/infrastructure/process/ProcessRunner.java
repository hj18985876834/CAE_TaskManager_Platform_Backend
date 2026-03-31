package com.example.cae.nodeagent.infrastructure.process;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class ProcessRunner {
	private final ProcessLogReader processLogReader;
	private final ProcessExitHandler processExitHandler;

	public ProcessRunner(ProcessLogReader processLogReader, ProcessExitHandler processExitHandler) {
		this.processLogReader = processLogReader;
		this.processExitHandler = processExitHandler;
	}

	public int run(List<String> command, File workDir, Integer timeoutSeconds, Consumer<String> stdoutConsumer, Consumer<String> stderrConsumer) {
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.directory(workDir);
			Process process = builder.start();

			Thread outThread = new Thread(() -> processLogReader.read(process.getInputStream(), stdoutConsumer));
			Thread errThread = new Thread(() -> processLogReader.read(process.getErrorStream(), stderrConsumer));
			outThread.start();
			errThread.start();

			boolean finished;
			if (timeoutSeconds != null && timeoutSeconds > 0) {
				finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
			} else {
				process.waitFor();
				finished = true;
			}
			if (!finished) {
				process.destroyForcibly();
				throw new ProcessTimeoutException("solver process timeout after " + timeoutSeconds + " seconds");
			}
			int exitCode = process.exitValue();
			outThread.join();
			errThread.join();
			processExitHandler.checkExitCode(exitCode);
			return exitCode;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("process interrupted", ex);
		} catch (Exception ex) {
			throw new RuntimeException("run process failed", ex);
		}
	}
}
