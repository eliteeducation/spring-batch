/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.support.CompositeItemStream;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.listener.RetryListenerSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class ChunkElementParserTests {

	private ConfigurableApplicationContext chunkElementParentAttributeParserTestsContext = new ClassPathXmlApplicationContext(
			"org/springframework/batch/core/configuration/xml/ChunkElementParentAttributeParserTests-context.xml");

	@Test
	public void testInheritSkippable() throws Exception {
		Collection<Class<?>> skippable = getExceptionClasses("s1", "skippable",
				chunkElementParentAttributeParserTestsContext);
		assertEquals(3, skippable.size());
		boolean e = false;
		boolean f = false;
		for (Class<?> cls : skippable) {
			if (cls.equals(NullPointerException.class)) {
				e = true;
			}
			else if (cls.equals(ArithmeticException.class)) {
				f = true;
			}
		}
		assertTrue(e);
		assertTrue(f);
	}

	@Test
	public void testInheritFatal() throws Exception {
		Collection<Class<?>> fatal = getExceptionClasses("s1", "fatal", chunkElementParentAttributeParserTestsContext);
		boolean a = false;
		boolean b = false;
		for (Class<?> cls : fatal) {
			if (cls.equals(CannotAcquireLockException.class)) {
				a = true;
			}
			else if (cls.equals(DeadlockLoserDataAccessException.class)) {
				b = true;
			}
		}
		assertTrue(a);
		assertTrue(b);
	}

	@Test
	public void testInheritStreams() throws Exception {
		Collection<ItemStream> streams = getStreams("s1", chunkElementParentAttributeParserTestsContext);
		assertEquals(2, streams.size());
		boolean c = false;
		for (ItemStream o : streams) {
			if (o instanceof CompositeItemStream) {
				c = true;
			}
		}
		assertTrue(c);
	}

	@Test
	public void testInheritRetryListeners() throws Exception {
		Collection<RetryListener> retryListeners = getRetryListeners("s1",
				chunkElementParentAttributeParserTestsContext);
		assertEquals(2, retryListeners.size());
		boolean g = false;
		boolean h = false;
		for (RetryListener o : retryListeners) {
			if (o instanceof RetryListenerSupport) {
				g = true;
			}
			else if (o instanceof DummyRetryListener) {
				h = true;
			}
		}
		assertTrue(g);
		assertTrue(h);
	}

	@Test
	public void testInheritSkippableWithNoMerge() throws Exception {
		Collection<Class<?>> skippable = getExceptionClasses("s2", "skippable",
				chunkElementParentAttributeParserTestsContext);
		assertEquals(2, skippable.size());
		boolean e = false;
		for (Class<?> cls : skippable) {
			if (cls.equals(NullPointerException.class)) {
				e = true;
			}
		}
		assertTrue(e);
	}

	@Test
	public void testInheritFatalWithNoMerge() throws Exception {
		Collection<Class<?>> fatal = getExceptionClasses("s2", "fatal", chunkElementParentAttributeParserTestsContext);
		boolean a = false;
		boolean b = false;
		for (Class<?> cls : fatal) {
			if (cls.equals(CannotAcquireLockException.class)) {
				a = true;
			}
			else if (cls.equals(DeadlockLoserDataAccessException.class)) {
				b = true;
			}
		}
		assertTrue(a);
		assertTrue(!b);
	}

	@Test
	public void testInheritStreamsWithNoMerge() throws Exception {
		Collection<ItemStream> streams = getStreams("s2", chunkElementParentAttributeParserTestsContext);
		assertEquals(1, streams.size());
		boolean c = false;
		for (ItemStream o : streams) {
			if (o instanceof CompositeItemStream) {
				c = true;
			}
		}
		assertTrue(c);
	}

	@Test
	public void testInheritRetryListenersWithNoMerge() throws Exception {
		Collection<RetryListener> retryListeners = getRetryListeners("s2",
				chunkElementParentAttributeParserTestsContext);
		assertEquals(1, retryListeners.size());
		boolean h = false;
		for (RetryListener o : retryListeners) {
			if (o instanceof DummyRetryListener) {
				h = true;
			}
		}
		assertTrue(h);
	}

	@SuppressWarnings("unchecked")
	private Set<Class<?>> getExceptionClasses(String stepName, String type, ApplicationContext ctx) throws Exception {
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Object step = ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);

		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
		Object skipPolicy = ReflectionTestUtils.getField(chunkProvider, "skipPolicy");
		Object classifier = ReflectionTestUtils.getField(skipPolicy, type + "ExceptionClassifier");
		Map<Class<?>, ?> classified = (Map<Class<?>, ?>) ReflectionTestUtils.getField(classifier, "classified");

		return classified.keySet();
	}

	@SuppressWarnings("unchecked")
	private Collection<ItemStream> getStreams(String stepName, ApplicationContext ctx) throws Exception {
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Object step = ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);

		Object compositeStream = ReflectionTestUtils.getField(step, "stream");
		return (Collection<ItemStream>) ReflectionTestUtils.getField(compositeStream, "streams");
	}

	@SuppressWarnings("unchecked")
	private Collection<RetryListener> getRetryListeners(String stepName, ApplicationContext ctx) throws Exception {
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue(beans.containsKey(stepName));
		Object step = ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);

		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		Object retryTemplate = ReflectionTestUtils.getField(chunkProcessor, "batchRetryTemplate");
		Object regular = ReflectionTestUtils.getField(retryTemplate, "regular");
		RetryListener[] listeners = (RetryListener[]) ReflectionTestUtils.getField(regular, "listeners");
		return Arrays.asList(listeners);
	}
}
