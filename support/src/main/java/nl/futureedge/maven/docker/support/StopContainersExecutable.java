package nl.futureedge.maven.docker.support;

import java.util.List;
import nl.futureedge.maven.docker.exception.DockerException;
import nl.futureedge.maven.docker.executor.DockerCommands;
import nl.futureedge.maven.docker.executor.DockerExecutor;

public final class StopContainersExecutable extends DockerExecutable {

    private final String filter;

    public StopContainersExecutable(final StopContainersSettings settings) {
        super(settings);

        this.filter = settings.getFilter();
    }

    public void execute() throws DockerException {
        debug("Stop containers configuration: ");
        debug("- filter: " + filter);

        final DockerExecutor executor = createDockerExecutor();
        final List<String> containers = doIgnoringFailure(() -> DockerCommands.listContainers(executor, filter));

        if (containers != null) {
            for (final String container : containers) {
                if ("".equals(container.trim())) {
                    continue;
                }
                info("Stop container: " + container);
                doIgnoringFailure(() -> DockerCommands.stopContainer(executor, container));
            }
        }
    }
}
