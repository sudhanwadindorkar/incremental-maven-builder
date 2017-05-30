package com.sdindorkar.maven.incremental;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.LifecycleModuleBuilder;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.builder.Builder;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named("incremental")
public class IncrementalMavenBuilder implements Builder {

	private static final String PRINT_DETAILED_REACTOR_FLAG = "incremental.reactor.detailed";

	private static final String SIMULATE_BUILD_FLAG = "incremental.simulate";

	private static final String SECTION_SEPARATOR = " -----------------------------------------------------------------------";

	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private final LifecycleModuleBuilder lifecycleModuleBuilder;

	@Inject
	public IncrementalMavenBuilder(LifecycleModuleBuilder lifecycleModuleBuilder) {
		LOGGER.info(SECTION_SEPARATOR);
		LOGGER.info("Incremental Maven Builder");
		LOGGER.info(SECTION_SEPARATOR);
		this.lifecycleModuleBuilder = lifecycleModuleBuilder;
	}

	/**
	 * 
	 * 
	 * @see org.apache.maven.lifecycle.internal.builder.Builder#build(org.apache.
	 *      maven.execution.MavenSession,
	 *      org.apache.maven.lifecycle.internal.ReactorContext,
	 *      org.apache.maven.lifecycle.internal.ProjectBuildList,
	 *      java.util.List,
	 *      org.apache.maven.lifecycle.internal.ReactorBuildStatus)
	 */
	public void build(MavenSession session, ReactorContext reactorContext, ProjectBuildList projectBuilds,
			List<TaskSegment> taskSegments, ReactorBuildStatus reactorBuildStatus)
			throws ExecutionException, InterruptedException {
		boolean simulate = Boolean.valueOf(session.getRequest().getUserProperties().getProperty(SIMULATE_BUILD_FLAG));
		LOGGER.debug("{} = {}.", SIMULATE_BUILD_FLAG, simulate);
		boolean printDetailedReactor = Boolean
				.valueOf(session.getRequest().getUserProperties().getProperty(PRINT_DETAILED_REACTOR_FLAG));
		LOGGER.debug("{} = {}.", PRINT_DETAILED_REACTOR_FLAG, printDetailedReactor);
		LOGGER.debug("Processing projects in build to determine changed projects.");
		List<MavenProject> changedProjects = getChangedProjects(session);
		List<MavenProject> downStreamProjects = getDownStreamProjects(session, changedProjects);
		List<MavenProject> projectsToBuild = new ArrayList<>();
		List<MavenProject> projectsSkipped = new ArrayList<>();
		LOGGER.debug("Building final list of changed projects.");
		for (MavenProject project : session.getProjectDependencyGraph().getSortedProjects()) {
			if (changedProjects.contains(project) || downStreamProjects.contains(project)) {
				LOGGER.debug("Adding project {} to build queue.", project);
				projectsToBuild.add(project);
			} else {
				projectsSkipped.add(project);
			}
		}
		if (printDetailedReactor) {
			LOGGER.info("Changed projects:");
			printProjectList(changedProjects.stream());
			LOGGER.info("Downstream projects:");
			printProjectList(downStreamProjects.stream()
					.filter(downStreamProject -> !changedProjects.contains(downStreamProject)));
			LOGGER.info("Skipped projects:");
			printProjectList(projectsSkipped.stream());
		}
		LOGGER.info("Recalculated reactor:");
		printProjectList(projectsToBuild.stream());
		build(session, reactorContext, projectBuilds, projectsToBuild, taskSegments, simulate);
	}

	private void printProjectList(Stream<MavenProject> projects) {
		projects.forEach(project -> LOGGER.info(" {} ({})", project.getName(), project.getArtifactId()));
		LOGGER.info("\n");
	}

	private List<MavenProject> getDownStreamProjects(MavenSession session, List<MavenProject> changedProjects) {
		List<MavenProject> downStreamProjects = new ArrayList<>();
		for (MavenProject project : changedProjects) {
			List<MavenProject> downStream = session.getProjectDependencyGraph().getDownstreamProjects(project, true);
			if (!downStream.isEmpty()) {
				LOGGER.debug("Downstream projects for {} : {}", project,
						downStream.stream().map(p -> p.getId()).collect(Collectors.joining(",")));
				downStreamProjects.addAll(downStream);
			}
		}
		return downStreamProjects;
	}

	private List<MavenProject> getChangedProjects(MavenSession session) {
		List<MavenProject> projectsToBuild = new ArrayList<>();
		for (MavenProject project : session.getProjects()) {
			LOGGER.debug(SECTION_SEPARATOR);
			LOGGER.debug("Processing project: {}", project.getId());
			if (isBuildRequired(project, session)) {
				LOGGER.debug("Adding project {} to build queue.", project.getId());
				projectsToBuild.add(project);
			}
		}
		return projectsToBuild;
	}

	private void build(MavenSession mavenSession, ReactorContext reactorContext, ProjectBuildList projectBuilds,
			List<MavenProject> projects, List<TaskSegment> taskSegments, boolean simulate)
			throws ExecutionException, InterruptedException {
		mavenSession.setProjects(projects);

		for (TaskSegment taskSegment : taskSegments) {
			LOGGER.debug("segment");
			List<Object> tasks = taskSegment.getTasks();
			for (Object task : tasks) {
				LOGGER.debug(" task:" + task);
			}
			for (MavenProject mavenProject : mavenSession.getProjects()) {
				if (!simulate) {
					LOGGER.info("Building project: {}", mavenProject.getId());
					lifecycleModuleBuilder.buildProject(mavenSession, reactorContext, mavenProject, taskSegment);
					if (reactorContext.getReactorBuildStatus().isHalted()) {
						break;
					}
				}
			}
		}
	}

	private boolean isBuildRequired(MavenProject project, MavenSession session) {
		LOGGER.debug("Entering isBuildRequired for project {}", project.getId());
		List<String> sourceDirectories = new ArrayList<String>();
		sourceDirectories.add(project.getOriginalModel().getPomFile().toString());
		sourceDirectories.add(project.getBuild().getScriptSourceDirectory());
		sourceDirectories.add(project.getBuild().getSourceDirectory());
		project.getBuild().getResources().forEach(resource -> sourceDirectories.add(resource.getDirectory()));
		LOGGER.debug("Source directories: {}", sourceDirectories);
		Path artifactPath = Paths.get(session.getLocalRepository().getBasedir(),
				session.getLocalRepository().pathOf(project.getArtifact()));
		LOGGER.debug("Project artifact path is {}", artifactPath);
		if (!artifactPath.toFile().exists()) {
			LOGGER.debug("Build required as project artifact does not exist.");
			return true;
		}
		long lastBuildTime = artifactPath.toFile().lastModified();
		LOGGER.debug("Project artifact last modified time is {}", lastBuildTime);
		for (String sourceDir : sourceDirectories) {
			if (sourceDir != null) {
				LOGGER.debug("Iterating through {} to find changes", sourceDir);
				if (isNewer(new File(sourceDir), lastBuildTime)) {
					LOGGER.debug("Found modified item. Build is required for this project.");
					return true;
				}
			}
		}
		LOGGER.debug("Build not required as no changes found since last build.");
		return false;
	}

	private boolean isNewer(File directoryOrFile, long timeInMillis) {
		if (!directoryOrFile.exists()) {
			return false;
		}
		if (directoryOrFile.isFile()) {
			if (directoryOrFile.lastModified() > timeInMillis) {
				LOGGER.debug("Found modified item {} with modified time {}", directoryOrFile,
						directoryOrFile.lastModified());
				return true;
			}
			return false;
		}
		for (File file : FileUtils.listFilesAndDirs(directoryOrFile, FileFileFilter.FILE, TrueFileFilter.INSTANCE)) {
			if (file.lastModified() > timeInMillis) {
				LOGGER.debug("Found modified item {} with modified time {}", file, file.lastModified());
				return true;
			}
		}
		return false;
	}
}
