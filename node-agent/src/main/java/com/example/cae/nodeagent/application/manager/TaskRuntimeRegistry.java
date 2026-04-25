package com.example.cae.nodeagent.application.manager;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TaskRuntimeRegistry {
	private final ConcurrentMap<Long, TaskRuntime> activeTasks = new ConcurrentHashMap<>();

	public boolean register(Long taskId) {
		if (taskId == null) {
			return false;
		}
		return activeTasks.putIfAbsent(taskId, new TaskRuntime()) == null;
	}

	public void finish(Long taskId) {
		if (taskId != null) {
			activeTasks.remove(taskId);
		}
	}

	public boolean isRunning(Long taskId) {
		return taskId != null && activeTasks.containsKey(taskId);
	}

	public boolean isRunningReported(Long taskId) {
		TaskRuntime runtime = taskId == null ? null : activeTasks.get(taskId);
		return runtime != null && runtime.isRunningReported();
	}

	public int activeCount() {
		return activeTasks.size();
	}

	public int runningCount() {
		return (int) activeTasks.values().stream()
				.filter(TaskRuntime::isRunningReported)
				.count();
	}

	public void markRunningReported(Long taskId) {
		TaskRuntime runtime = activeTasks.get(taskId);
		if (runtime != null) {
			runtime.setRunningReported(true);
		}
	}

	public void attachWorker(Long taskId, Thread workerThread) {
		TaskRuntime runtime = activeTasks.get(taskId);
		if (runtime == null) {
			return;
		}
		runtime.setWorkerThread(workerThread);
		if (runtime.isCancelRequested() && workerThread != null) {
			workerThread.interrupt();
		}
	}

	public void attachProcess(Long taskId, Process process) {
		TaskRuntime runtime = activeTasks.get(taskId);
		if (runtime == null) {
			return;
		}
		runtime.setProcess(process);
		if (runtime.isCancelRequested() && process != null) {
			process.destroyForcibly();
		}
	}

	public void clearProcess(Long taskId) {
		TaskRuntime runtime = activeTasks.get(taskId);
		if (runtime != null) {
			runtime.setProcess(null);
		}
	}

	public boolean cancel(Long taskId, String reason) {
		TaskRuntime runtime = activeTasks.get(taskId);
		if (runtime == null) {
			return false;
		}
		runtime.setCancelRequested(true);
		runtime.setCancelReason(reason);
		Process process = runtime.getProcess();
		if (process != null) {
			process.destroyForcibly();
		}
		Thread workerThread = runtime.getWorkerThread();
		if (workerThread != null) {
			workerThread.interrupt();
		}
		return true;
	}

	public boolean isCancelRequested(Long taskId) {
		TaskRuntime runtime = activeTasks.get(taskId);
		return runtime != null && runtime.isCancelRequested();
	}

	public String getCancelReason(Long taskId) {
		TaskRuntime runtime = activeTasks.get(taskId);
		return runtime == null ? null : runtime.getCancelReason();
	}

	private static class TaskRuntime {
		private volatile boolean cancelRequested;
		private volatile boolean runningReported;
		private volatile String cancelReason;
		private volatile Thread workerThread;
		private volatile Process process;

		public boolean isCancelRequested() {
			return cancelRequested;
		}

		public void setCancelRequested(boolean cancelRequested) {
			this.cancelRequested = cancelRequested;
		}

		public boolean isRunningReported() {
			return runningReported;
		}

		public void setRunningReported(boolean runningReported) {
			this.runningReported = runningReported;
		}

		public String getCancelReason() {
			return cancelReason;
		}

		public void setCancelReason(String cancelReason) {
			this.cancelReason = cancelReason;
		}

		public Thread getWorkerThread() {
			return workerThread;
		}

		public void setWorkerThread(Thread workerThread) {
			this.workerThread = workerThread;
		}

		public Process getProcess() {
			return process;
		}

		public void setProcess(Process process) {
			this.process = process;
		}
	}
}
