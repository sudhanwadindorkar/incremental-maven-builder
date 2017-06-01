package com.sdindorkar.maven.incremental;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single threaded Incremental Builder implementation.
 * 
 * @author sudhanwa
 * @see {@link BuilderOptions} for options that can be specified.
 */
@Singleton
@Named("incremental")
public class SingleThreadedIncrementalBuilder extends SingleThreadedBuilder {

	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	public SingleThreadedIncrementalBuilder() {
		IncrementalBuilderUtil.printVersionInfo();
	}

	/**
	 * Calculates the list of changed projects and passes them to Maven's single
	 * threaded builder for building.
	 * 
	 * @see org.apache.maven.lifecycle.internal.builder.Builder#build(org.apache.
	 *      maven.execution.MavenSession,
	 *      org.apache.maven.lifecycle.internal.ReactorContext,
	 *      org.apache.maven.lifecycle.internal.ProjectBuildList,
	 *      java.util.List,
	 *      org.apache.maven.lifecycle.internal.ReactorBuildStatus)
	 */
	public void build(MavenSession session, ReactorContext reactorContext, ProjectBuildList projectBuilds,
			List<TaskSegment> taskSegments, ReactorBuildStatus reactorBuildStatus) {
		BuilderOptions builderOptions = new BuilderOptions(session.getUserProperties());
		ChangedModulesCalculator changedModulesCalculator = new ChangedModulesCalculator(session, projectBuilds);
		changedModulesCalculator.printReactor(builderOptions);
		try {
			if (!builderOptions.isSkipBuild()) {
				ProjectBuildList changedBuildList = changedModulesCalculator.getChangedProjectBuildList();
				if (changedBuildList.size() != 0) {
					SecurityManager originalSecurityManager = System.getSecurityManager();
					changedModulesCalculator.updateMavenSession(session);
					super.build(session, reactorContext, changedBuildList, taskSegments, reactorBuildStatus);
					IncrementalBuilderUtil.replaceLiferaySecurityManager(originalSecurityManager);
				} else {
					LOGGER.info("Nothing to build");
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error running the incremental build.", e);
		}

	}

}
