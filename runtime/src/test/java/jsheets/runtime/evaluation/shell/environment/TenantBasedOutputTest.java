package jsheets.runtime.evaluation.shell.environment;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TenantBasedOutputTest {
	@Test
	public void multiGroupsAreIsolated() {
		var fooGroupMessages = new ConcurrentLinkedQueue<>();
		var barGroupMessages = new ConcurrentLinkedQueue<>();
		var output = TenantBasedOutput.create();
		output.registerGroup("foo", fooGroupMessages::add);
		output.registerGroup("bar", barGroupMessages::add);
		runInGroup("foo", 2, repeat(10, () -> output.print("foo")));
		runInGroup("bar", 2, repeat(10, () -> output.print("bar")));
		Assertions.assertEquals(repeat(10, "foo"), List.copyOf(fooGroupMessages));
		Assertions.assertEquals(repeat(10, "bar"), List.copyOf(barGroupMessages));
	}

	@Test
	public void unknownGroupForwardsToFallback() {
		var received = new ArrayList<String>();
		var output = TenantBasedOutput.createWithFallback(received::add);
		output.print("hello");
		Assertions.assertEquals(List.of("hello"), received);
	}

	@Test
	@DisplayName("logs of threads created by threads in the group are captured")
	public void subThreadIsCaptured() {
		var messageCount = new AtomicInteger();
		var output = TenantBasedOutput.create();
		output.registerGroup("test", message -> messageCount.incrementAndGet());
		runInChild("test", () -> output.println("Hello"));
		Assertions.assertEquals(1, messageCount.get());
	}

	@Test
	@DisplayName("logs of threads created by threads in the group are captured")
	public void subGroupIsCaptured() {
		var messageCount = new AtomicInteger();
		var output = TenantBasedOutput.create();
		output.registerGroup("test", message -> messageCount.incrementAndGet());
		runInChildGroup("test", "child", () -> output.println("Hello"));
		Assertions.assertEquals(1, messageCount.get());
	}

	/**
	 * Runs the {@code task} in a thread that belongs to a child group with
	 * the given {@code childGroupName}, which is spawned by a thread in a group
	 * with the given {@code groupName}. It is used to ensure, that the semantics
	 * do not change for threads that were created by threads in the group but
	 * belong to a different sub-group themselves.
	 */
	private static void runInChildGroup(
		String groupName,
		String childGroupName,
		Runnable task
	) {
		var group = new ThreadGroup(groupName);
		var parent = new Thread(group, () -> {
			var childGroup = new ThreadGroup(childGroupName);
			var child = new Thread(childGroup, task);
			child.start();
			Uninterruptibles.joinUninterruptibly(child);
		});
		parent.start();
		Uninterruptibles.joinUninterruptibly(parent);
	}

	/**
	 * Runs the {@code task} in a thread spawned by a thread in a group with the
	 * given {@code GroupName}. It is used to ensure, that the semantics do not
	 * change for threads that were created by threads in the group.
	 */
	private static void runInChild(String groupName, Runnable task) {
		var group = new ThreadGroup(groupName);
		var parent = new Thread(group, () -> {
			var child = new Thread(task);
			child.start();
			Uninterruptibles.joinUninterruptibly(child);
		});
		parent.start();
		Uninterruptibles.joinUninterruptibly(parent);
	}

	@Test
	@DisplayName(
		"given a single group, all logs from threads in that group are captured"
	)
	public void singleGroupCapturesAll() {
		var output = TenantBasedOutput.create();
		var messageCount = new AtomicInteger();
		output.registerGroup("test", (message) -> messageCount.incrementAndGet());
		runInGroup("test", 2, repeat(10, () -> output.println("hello")));
		Assertions.assertEquals(10, messageCount.get());
	}

	/** Creates a list with {@code count} instances of {@code element}. */
	private static <T> Collection<T> repeat(int count, T element) {
		var output = new ArrayList<T>(count);
		for (int index = 0; index < count; index++) {
			output.add(element);
		}
		return output;
	}

	/**
	 * Distributes the execution of all {@code tasks} among {@code groupSize}
	 * worker threads that are created in a group with the given {@code name}.
	 */
	public void runInGroup(
		String name,
		int groupSize,
		Collection<Runnable> tasks
	) {
		var group = new ThreadGroup(name);
		int tasksPerGroup = tasks.size() / groupSize;
		var taskIteration = tasks.iterator();
		var latch = new CountDownLatch(groupSize);
		for (int threadIndex = 0; threadIndex < groupSize; threadIndex++) {
			new Thread(
				group,
				() -> {
					take(tasksPerGroup, taskIteration, Runnable::run);
					latch.countDown();
				}
			).start();
		}
		try {
			latch.await();
		} catch (InterruptedException interruption) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("interruption");
		}
	}

	/**
	 * Feeds the next {@code count} elements of the {@code iteration} into the
	 * {@code consumer}. It won't guarantee that the {@code consumer} receives
	 * the exact {@code count} as the {@code iteration} could be done before that.
	 */
	private static <Element> void take(
		int count,
		Iterator<Element> iteration,
		Consumer<Element> consumer
	) {
		for (int index = 0; index < count && iteration.hasNext(); index++) {
			consumer.accept(iteration.next());
		}
	}
}
