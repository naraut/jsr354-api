/*
 * Copyright (c) 2012, 2013, Credit Suisse (Anatole Tresch), Werner Keil. Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package javax.money.spi;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loader for the Java Money configuration.
 * 
 * @author Anatole Tresch
 */
public final class JavaMoneyConfig {

	private static final Logger LOG = Logger
			.getLogger(JavaMoneyConfig.class.getName());

	private static final JavaMoneyConfig INSTANCE = new JavaMoneyConfig();
	
	private Map<String, String> config = new HashMap<>();
	private Map<String, Integer> priorities = new HashMap<>();

	private JavaMoneyConfig() {
		try {
			Enumeration<URL> urls = getClass().getClassLoader().getResources(
					"javamoney.properties");
			while (urls.hasMoreElements()) {
				URL url = (URL) urls.nextElement();
				try {
					Properties props = new Properties();
					props.load(url.openStream());
					updateConfig(props);
				} catch (Exception e) {
					LOG.log(Level.SEVERE,
							"Error loading javamoney.properties, ignoring "
									+ url, e);
				}
			}
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Error loading javamoney.properties.", e);
		}
	}

	private void updateConfig(Properties props) {
		for (Map.Entry<Object, Object> en : props.entrySet()) {
			String key = en.getKey().toString();
			String value = en.getValue().toString();
			int prio = 0;
			if (value.contains("{prio=")) {
				int index = value.indexOf('}');
				if (index > 0) {
					String prioString = value.substring("{prio=".length(),
							index - 1);
					value = value.substring(index + 1);
					prio = Integer.parseInt(prioString);
					this.priorities.put(key, prio);
				}
			}
			Integer existingPrio = priorities.get(key);
			if (existingPrio == null) {
				this.config.put(key, value);
			} else if (existingPrio < prio) {
				this.config.put(key, value);
			} else if (existingPrio == prio) {
				throw new IllegalStateException(
						"AmbigousConfiguration detected for '" + key + "'.");
			}
			// else ignore entry with lower prio!
		}
	}

	public static Map<String, String> getConfig() {
		return Collections.unmodifiableMap(INSTANCE.config);
	}

}