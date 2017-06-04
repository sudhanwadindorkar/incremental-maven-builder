package com.sdindorkar.maven.incremental;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class.
 * 
 * @author sudhanwa
 *
 */
public class IncrementalBuilderUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(IncrementalBuilderUtil.class);

	/**
	 * Print the extension version information.
	 */
	public static void printVersionInfo() {
		LOGGER.info(Constants.SECTION_SEPARATOR);
		LOGGER.info("Incremental Maven Builder");
		LOGGER.info(Constants.SECTION_SEPARATOR);
	}

	/**
	 * Replace the security manager set by Liferay's Maven plugins (if any) with
	 * the original security manager.
	 * <p>
	 * Liferay's maven plugins replace the system security manager with a custom
	 * security manager that throws an exception for all permission checks. See
	 * https://issues.liferay.com/browse/LPS-7524. Due to this, in a
	 * multithreaded build, the build cannot finish as System.exit throws an
	 * exception. Hence, we need to replace Liferay's security manager with the
	 * original one.
	 * 
	 * @param replacement
	 *            The original security manager.
	 */
	public static void replaceLiferaySecurityManager(SecurityManager replacement) {
		try {
			if (System.getSecurityManager() != null) {
				String currentSecurityManager = System.getSecurityManager().getClass().getName();
				if (currentSecurityManager.startsWith(Constants.LIFERAY_MOJO_CLASS)) {
					System.setSecurityManager(replacement);
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Unexpected exception during clean up. This does not affect the build."
					+ " Please run with -X to see more details.");
			LOGGER.warn(e.getMessage());
			LOGGER.debug("Original exception:", e);
		}
	}

}
