package com.sdindorkar.maven.incremental;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.graph.DefaultProjectDependencyGraph;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangedModulesCalculator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChangedModulesCalculator.class);

	private List<MavenProject> changedProjects;
	private List<MavenProject> downStreamProjects;
	private List<MavenProject> projectsToBuild;
	private List<MavenProject> projectsSkipped;
	private List<ProjectSegment> changedProjectSegments;

	public ChangedModulesCalculator(MavenSession session, ProjectBuildList originalProjectBuildList) {
		LOGGER.debug("Processing projects in build to determine changed projects.");
		changedProjects = calculateChangedProjects(session);
		downStreamProjects = getDownStreamProjects(session, changedProjects);
		projectsToBuild = new ArrayList<>();
		projectsSkipped = new ArrayList<>();
		LOGGER.debug("Building final list of changed projects.");
		changedProjectSegments = new ArrayList<>();
		ProjectSegment projectSegment;
		for (MavenProject project : session.getProjectDependencyGraph().getSortedProjects()) {
			if (changedProjects.contains(project) || downStreamProjects.contains(project)) {
				LOGGER.debug("Adding project {} to build queue.", project);
				projectsToBuild.add(project);
				projectSegment = originalProjectBuildList.findByMavenProject(project);
				if (projectSegment != null) {
					changedProjectSegments.add(projectSegment);
				}
			} else {
				projectsSkipped.add(project);
			}
		}
	}

	public void updateMavenSession(MavenSession session) throws Exception {
		session.setProjects(projectsToBuild);
		session.setProjectDependencyGraph(new DefaultProjectDependencyGraph(session.getAllProjects(), projectsToBuild));
	}

	public ProjectBuildList getChangedProjectBuildList() {
		return new ProjectBuildList(changedProjectSegments);
	}

	public void printReactor(BuilderOptions builderOptions) {
		if (builderOptions.isPrintDetailedReactor()) {
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

	private List<MavenProject> calculateChangedProjects(MavenSession session) {
		List<MavenProject> projectsToBuild = new ArrayList<>();
		for (MavenProject project : session.getProjects()) {
			LOGGER.debug(Constants.SECTION_SEPARATOR);
			LOGGER.debug("Processing project: {}", project.getId());
			if (isBuildRequired(project, session)) {
				LOGGER.debug("Adding project {} to build queue.", project.getId());
				projectsToBuild.add(project);
			}
		}
		return projectsToBuild;
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
