package com.sdindorkar.maven.incremental;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncrementalBuilderUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(IncrementalBuilderUtil.class);

	public static void printVersionInfo() {
		LOGGER.info(Constants.SECTION_SEPARATOR);
		LOGGER.info("Incremental Maven Builder");
		LOGGER.info(Constants.SECTION_SEPARATOR);
	}

	public static void replaceLiferaySecurityManager(SecurityManager replacement) {
		String currentSecurityManager = System.getSecurityManager().getClass().getName();
		if (currentSecurityManager.startsWith(Constants.LIFERAY_MOJO_CLASS)) {
			System.setSecurityManager(replacement);
		}
	}

}
