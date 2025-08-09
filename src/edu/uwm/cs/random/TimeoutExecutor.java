package edu.uwm.cs.random;

/**
 * An executor that executes a runnable if there is a timeout,
 * but the timeout can be repeatedly deferred (unlike {@link java.util.Timer}.
 * This executor can cause the JVM to stay alive unless it is canceled.
 */
public class TimeoutExecutor {
	private final Runnable action;
	private Thread myThread;
	
	private long timeoutDeadline; // when timeout should happen
	private boolean executed = false;
	
	public TimeoutExecutor(Runnable timeoutAction, long milliseconds) {
		action = timeoutAction;
		timeoutDeadline = System.currentTimeMillis() + milliseconds;
		myThread = new Thread(() -> run());
		myThread.start();
	}

	/** Change the timeout deadline to the given amount.
	 * @param newMillis new amount of time to wait, must not be negative
	 * @return if its too late: the timeout action has already executed
	 */
	public boolean defer(long newMillis) {
		if (newMillis < 0) throw new IllegalArgumentException("cannot defer a negative time");
		synchronized (myThread) {
			if (executed) return false;
			if (!myThread.isAlive()) return true;
			timeoutDeadline = System.currentTimeMillis() + newMillis;
		}
		return true;
	}
	
	/**
	 * Stop this executor.  The timeout won't happen any more.
	 * The timeout can be canceled multiple times.  Later cancellations have no effect.
	 * @return false if its too late, the timeout action has already happened
	 */
	public boolean cancel() {
		synchronized (myThread) {
			if (executed) return false;
			timeoutDeadline = -1;
			if (!myThread.isAlive()) return true;
			myThread.interrupt();
		}
		return true;
	}

	/**
	 * Return true if this timeout has executed.
	 * @return true if the timeout action has happened.
	 */
	public boolean executed() {
		synchronized (myThread) {
			return executed;
		}
	}
	
	/**
	 * Return true if this timeout is still live.
	 * @return true if still live
	 */
	public boolean isAlive() {
		return myThread.isAlive();
	}
	
	private void run() {
		try {
			for (;;) {
				long deadline;
				synchronized (myThread) {
					deadline = timeoutDeadline;
				}
				if (deadline == -1) return;
				long sleepTime = deadline - System.currentTimeMillis();
				if (sleepTime < 0) break;
				Thread.sleep(sleepTime);
			}
			executed = true;
			action.run();
		} catch (InterruptedException e) {
			return;
		}
	}
}
