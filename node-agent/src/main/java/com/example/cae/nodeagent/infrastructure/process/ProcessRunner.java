package com.example.cae.nodeagent.infrastructure.process;

import com.example.cae.nodeagent.application.manager.TaskRuntimeRegistry;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class ProcessRunner {
	private final ProcessLogReader processLogReader;
	private final ProcessExitHandler processExitHandler;
	private final TaskRuntimeRegistry taskRuntimeRegistry;

	public ProcessRunner(ProcessLogReader processLogReader, ProcessExitHandler processExitHandler, TaskRuntimeRegistry taskRuntimeRegistry) {
		this.processLogReader = processLogReader;
		this.processExitHandler = processExitHandler;
		this.taskRuntimeRegistry = taskRuntimeRegistry;
	}

	public int run(Long taskId, List<String> command, File workDir, Integer timeoutSeconds, Consumer<String> stdoutConsumer, Consumer<String> stderrConsumer) {
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.directory(workDir);
			Process process = builder.start();
			taskRuntimeRegistry.attachProcess(taskId, process);

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
			if (taskRuntimeRegistry.isCancelRequested(taskId)) {
				throw new ProcessCanceledException("task canceled");
			}
			processExitHandler.checkExitCode(exitCode);
			return exitCode;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			if (taskRuntimeRegistry.isCancelRequested(taskId)) {
				throw new ProcessCanceledException("task canceled");
			}
			throw new RuntimeException("process interrupted", ex);
		} catch (ProcessCanceledException | ProcessTimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new RuntimeException("run process failed", ex);
		} finally {
			taskRuntimeRegistry.clearProcess(taskId);
		}
	}
}
