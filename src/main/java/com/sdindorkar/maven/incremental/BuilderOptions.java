package com.sdindorkar.maven.incremental;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuilderOptions {

	private static final Logger LOGGER = LoggerFactory.getLogger(BuilderOptions.class);

	private static final String PRINT_DETAILED_REACTOR_FLAG = "incremental.reactor.detailed";

	private static final String SIMULATE_BUILD_FLAG = "incremental.simulate";

	private boolean simulate;
	private boolean printDetailedReactor;

	public BuilderOptions(Properties userProperties) {
		this.simulate = Boolean.valueOf(userProperties.getProperty(SIMULATE_BUILD_FLAG));
		this.printDetailedReactor = Boolean.valueOf(userProperties.getProperty(PRINT_DETAILED_REACTOR_FLAG));
		LOGGER.debug("{} = {}.", SIMULATE_BUILD_FLAG, this.simulate);
		LOGGER.debug("{} = {}.", PRINT_DETAILED_REACTOR_FLAG, this.printDetailedReactor);
	}

	public boolean isSimulate() {
		return simulate;
	}

	public boolean isPrintDetailedReactor() {
		return printDetailedReactor;
	}

}
