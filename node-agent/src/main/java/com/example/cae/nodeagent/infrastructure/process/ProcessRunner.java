package com.example.cae.nodeagent.infrastructure.process;

import com.example.cae.nodeagent.application.manager.TaskRuntimeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class ProcessRunner {
	private static final Logger log = LoggerFactory.getLogger(ProcessRunner.class);
	private static final long READER_JOIN_TIMEOUT_MILLIS = 5000L;
	private static final long PROCESS_DESTROY_WAIT_SECONDS = 5L;

	private final ProcessLogReader processLogReader;
	private final ProcessExitHandler processExitHandler;
	private final TaskRuntimeRegistry taskRuntimeRegistry;

	public ProcessRunner(ProcessLogReader processLogReader, ProcessExitHandler processExitHandler, TaskRuntimeRegistry taskRuntimeRegistry) {
		this.processLogReader = processLogReader;
		this.processExitHandler = processExitHandler;
		this.taskRuntimeRegistry = taskRuntimeRegistry;
	}

	public int run(Long taskId, List<String> command, File workDir, Integer timeoutSeconds, Consumer<String> stdoutConsumer, Consumer<String> stderrConsumer) {
		Process process = null;
		Thread outThread = null;
		Thread errThread = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.directory(workDir);
			process = builder.start();
			taskRuntimeRegistry.attachProcess(taskId, process);

			outThread = newReaderThread(taskId, "stdout", process, stdoutConsumer);
			errThread = newReaderThread(taskId, "stderr", process, stderrConsumer);
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
				destroyProcess(taskId, process);
				joinReadersBounded(taskId, outThread, errThread);
				throw new ProcessTimeoutException("solver process timeout after " + timeoutSeconds + " seconds");
			}
			int exitCode = process.exitValue();
			joinReadersBounded(taskId, outThread, errThread);
			if (taskRuntimeRegistry.isCancelRequested(taskId)) {
				throw new ProcessCanceledException("task execution interrupted by unsupported runtime cancel");
			}
			processExitHandler.checkExitCode(exitCode);
			return exitCode;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			if (process != null && process.isAlive()) {
				destroyProcess(taskId, process);
			}
			joinReadersAfterInterrupt(taskId, outThread, errThread);
			if (taskRuntimeRegistry.isCancelRequested(taskId)) {
				throw new ProcessCanceledException("task execution interrupted by unsupported runtime cancel");
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

	private Thread newReaderThread(Long taskId, String streamName, Process process, Consumer<String> consumer) {
		Thread thread = new Thread(
				() -> processLogReader.read("stdout".equals(streamName) ? process.getInputStream() : process.getErrorStream(), consumer),
				"node-agent-task-" + taskId + "-" + streamName
		);
		thread.setDaemon(true);
		return thread;
	}

	private void destroyProcess(Long taskId, Process process) {
		if (process == null || !process.isAlive()) {
			return;
		}
		process.destroyForcibly();
		try {
			if (!process.waitFor(PROCESS_DESTROY_WAIT_SECONDS, TimeUnit.SECONDS)) {
				log.warn("solver process did not exit after destroyForcibly, taskId={}", taskId);
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.warn("interrupted while waiting destroyed solver process, taskId={}", taskId);
		}
	}

	private void joinReadersBounded(Long taskId, Thread outThread, Thread errThread) throws InterruptedException {
		joinReaderBounded(taskId, outThread);
		joinReaderBounded(taskId, errThread);
	}

	private void joinReadersAfterInterrupt(Long taskId, Thread outThread, Thread errThread) {
		try {
			joinReadersBounded(taskId, outThread, errThread);
		} catch (InterruptedException joinInterrupted) {
			Thread.currentThread().interrupt();
			log.warn("interrupted while waiting process log readers, taskId={}", taskId);
		}
	}

	private void joinReaderBounded(Long taskId, Thread thread) throws InterruptedException {
		if (thread == null) {
			return;
		}
		thread.join(READER_JOIN_TIMEOUT_MILLIS);
		if (thread.isAlive()) {
			log.warn("process log reader still alive after bounded join, taskId={}, thread={}", taskId, thread.getName());
		}
	}
}
