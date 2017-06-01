package com.sdindorkar.maven.incremental;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The options that can be specified to the incremental builder.
 * <p>
 * The following options are currently supported:
 * <ol>
 * <li>{@link #PRINT_DETAILED_REACTOR_FLAG}</li>
 * <li>{@link #SKIP_BUILD_FLAG}</li>
 * </ol>
 * 
 * @author sudhanwa
 *
 */
public class BuilderOptions {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuilderOptions.class);

	/**
	 * Option to print detailed reactor information.
	 */
	public static final String PRINT_DETAILED_REACTOR_FLAG = "incremental.reactor.detailed";

	/**
	 * Options to skip the build and just print the reactor information.
	 */
	public static final String SKIP_BUILD_FLAG = "incremental.skip.build";

	private boolean skipBuild;
	private boolean printDetailedReactor;

	public BuilderOptions(Properties userProperties) {
		this.skipBuild = Boolean.valueOf(userProperties.getProperty(SKIP_BUILD_FLAG));
		this.printDetailedReactor = Boolean.valueOf(userProperties.getProperty(PRINT_DETAILED_REACTOR_FLAG));
		LOGGER.debug("{} = {}.", SKIP_BUILD_FLAG, this.skipBuild);
		LOGGER.debug("{} = {}.", PRINT_DETAILED_REACTOR_FLAG, this.printDetailedReactor);
	}

	public boolean isSkipBuild() {
		return skipBuild;
	}

	public boolean isPrintDetailedReactor() {
		return printDetailedReactor;
	}

}
