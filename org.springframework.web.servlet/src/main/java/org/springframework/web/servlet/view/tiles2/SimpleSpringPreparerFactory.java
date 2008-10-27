/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.servlet.view.tiles2;

import java.util.Map;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.NoSuchPreparerException;
import org.apache.tiles.preparer.PreparerException;
import org.apache.tiles.preparer.ViewPreparer;

import org.springframework.core.CollectionFactory;
import org.springframework.web.context.WebApplicationContext;

/**
 * Tiles2 {@link org.apache.tiles.preparer.PreparerFactory} implementation
 * that expects preparer class names and builds preparer instances for those,
 * creating them through the Spring ApplicationContext in order to apply
 * Spring container callbacks and configured Spring BeanPostProcessors.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SpringBeanPreparerFactory
 */
public class SimpleSpringPreparerFactory extends AbstractSpringPreparerFactory {

	/** Cache of shared ViewPreparer instances: bean name --> bean instance */
	private final Map sharedPreparers = CollectionFactory.createConcurrentMapIfPossible(16);


	protected ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException {
		// Quick check on the concurrent map first, with minimal locking.
		ViewPreparer preparer = (ViewPreparer) this.sharedPreparers.get(name);
		if (preparer == null) {
			synchronized (this.sharedPreparers) {
				preparer = (ViewPreparer) this.sharedPreparers.get(name);
				if (preparer == null) {
					try {
						Class beanClass = context.getClassLoader().loadClass(name);
						if (!ViewPreparer.class.isAssignableFrom(beanClass)) {
							throw new PreparerException(
									"Invalid preparer class [" + name + "]: does not implement ViewPreparer interface");
						}
						preparer = (ViewPreparer) context.getAutowireCapableBeanFactory().createBean(beanClass);
						this.sharedPreparers.put(name, preparer);
					}
					catch (ClassNotFoundException ex) {
						throw new NoSuchPreparerException("Preparer class [" + name + "] not found", ex);
					}
				}
			}
		}
		return preparer;
	}

}
