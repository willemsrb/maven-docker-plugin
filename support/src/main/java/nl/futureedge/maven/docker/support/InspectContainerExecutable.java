package nl.futureedge.maven.docker.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import nl.futureedge.maven.docker.executor.DockerExecutionException;
import nl.futureedge.maven.docker.executor.DockerExecutor;

public final class InspectContainerExecutable extends DockerExecutable {

    private final Properties projectProperties;
    private final String containerId;
    private final String containerNameProperty;
    private final Properties portProperties;

    public InspectContainerExecutable(final InspectContainerSettings settings) {
        super(settings);

        this.projectProperties = settings.getProjectProperties();
        this.containerId = settings.getContainerId();
        this.containerNameProperty = settings.getContainerNameProperty();
        this.portProperties = settings.getPortProperties();
    }

    public void execute() throws DockerExecutionException {
        debug("Inspect container configuration: ");
        debug("- projectProperties: " + projectProperties);
        debug("- containerId: " + containerId);
        debug("- containerNameProperty: " + containerNameProperty);
        debug("- portProperties: " + portProperties);

        final DockerExecutor executor = createDockerExecutor();
        final String containerInfoJson = doIgnoringFailure(() -> inspectContainer(executor, containerId));

        final JsonParser parser = new JsonParser();
        final JsonArray containerInfos = parser.parse(containerInfoJson).getAsJsonArray();
        final JsonObject containerInfo = containerInfos.get(0).getAsJsonObject();

        handleContainerName(containerInfo);
        handlePorts(containerInfo);
    }

    private void handleContainerName(final JsonObject containerInfo) {
        if (containerNameProperty == null || "".equals(containerNameProperty.trim())) {
            return;
        }

        final JsonPrimitive name = containerInfo.getAsJsonPrimitive("Name");
        if (name == null) {
            return;
        }

        String containerName = name.getAsString();
        if (containerName.startsWith("/")) {
            containerName = containerName.substring(1);
        }
        debug("Name: " + containerName);
        projectProperties.setProperty(containerNameProperty, containerName);
    }

    private void handlePorts(JsonObject containerInfo) {
        if (portProperties == null || portProperties.isEmpty()) {
            return;
        }

        final JsonObject networkSettings = containerInfo.getAsJsonObject("NetworkSettings");
        if (networkSettings == null) {
            return;
        }
        final JsonObject ports = networkSettings.getAsJsonObject("Ports");
        if (ports == null) {
            return;
        }

        for (String portPropertyKey : portProperties.stringPropertyNames()) {
            final JsonArray mappings = ports.getAsJsonArray(portPropertyKey);
            if (mappings == null || mappings.size() == 0) {
                warn("Port " + portPropertyKey + ": not mapped");
            } else {
                if (mappings.size() > 1) {
                    warn("Port " + portPropertyKey + " is mapped multiple times; an undetermined mapping wil be returned");
                }
                final JsonObject mapping = mappings.get(0).getAsJsonObject();
                final JsonPrimitive port = mapping.getAsJsonPrimitive("HostPort");
                debug("Port " + portPropertyKey + ": " + port.getAsString());
                projectProperties.setProperty(portProperties.getProperty(portPropertyKey), port.getAsString());
            }
        }
    }

    private String inspectContainer(final DockerExecutor executor, final String containerId) throws DockerExecutionException {
        final List<String> arguments = new ArrayList<>();
        arguments.add("inspect");
        arguments.add("--type");
        arguments.add("container");
        arguments.add(containerId);

        final List<String> result = executor.execute(arguments, false, true);
        return result.stream().collect(Collectors.joining());

    }
}