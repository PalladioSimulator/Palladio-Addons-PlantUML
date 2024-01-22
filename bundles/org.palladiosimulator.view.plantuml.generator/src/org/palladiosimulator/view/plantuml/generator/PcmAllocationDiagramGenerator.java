package org.palladiosimulator.view.plantuml.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;

public class PcmAllocationDiagramGenerator {

    private static String COMPONENT_START = "[", COMPONENT_END = "]";
    private static String LINK_START = "[[", LINK_END = "]]";
    private static String CURLY_OPENING_BRACKET = "{", CURLY_CLOSING_BRACKET = "}";
    private static String COLON = " : ";
    private static String PROVIDES_REQUIRES_LINK = " -(0- ";
    private static String NEWLINE = "\n";
    private static String SPACE = " ";
    private static String CONTAINER_KEYWORD = "node";

    private static void appendContainerEnd(final StringBuilder buffer) {
        buffer.append(CURLY_CLOSING_BRACKET);
        buffer.append(NEWLINE);
    }

    // example: node System1 {
    private static void appendContainerStart(final ResourceContainer container, final StringBuilder buffer) {
        buffer.append(CONTAINER_KEYWORD);
        buffer.append(SPACE);
        buffer.append(container.getEntityName());
        buffer.append(SPACE);
        buffer.append(CURLY_OPENING_BRACKET);
        buffer.append(NEWLINE);
    }

    private final List<AllocationContext> contexts = new ArrayList<>();
    private final Map<ResourceContainer, List<AllocationContext>> containerToContexts = new HashMap<>();

    private final List<AssemblyConnector> connectors = new ArrayList<>();

    private String linkToSystem;

    private final String diagramText;

    public PcmAllocationDiagramGenerator(final Allocation allocation) {
        diagramText = getDiagramText(allocation);
    }

    // example: [DataAccess]
    private void appendAllocationContext(final AllocationContext context, final StringBuilder buffer) {
        buffer.append(COMPONENT_START);
        buffer.append(context.getEntityName());
        buffer.append(COMPONENT_END);
        buffer.append(SPACE);
        buffer.append(LINK_START);
        buffer.append(linkToSystem);
        buffer.append(LINK_END);
        buffer.append(NEWLINE);
    }

    // example: [Access Control] -(0- [Web Server] : REST
    private void appendAssemblyConnector(final AssemblyConnector connector, final StringBuilder buffer) {
        buffer.append(COMPONENT_START);

        // requiring context
        final AssemblyContext reqAssemblyContext = connector.getRequiringAssemblyContext_AssemblyConnector();
        final AllocationContext reqAllocContext = assemblyToAllocationContext(reqAssemblyContext);
        buffer.append(reqAllocContext.getEntityName());
        buffer.append(COMPONENT_END);

        buffer.append(PROVIDES_REQUIRES_LINK);

        buffer.append(COMPONENT_START);

        // providing context
        final AssemblyContext provAssemblyContext = connector.getProvidingAssemblyContext_AssemblyConnector();
        final AllocationContext provAllocContext = assemblyToAllocationContext(provAssemblyContext);
        buffer.append(provAllocContext.getEntityName());
        buffer.append(COMPONENT_END);

        buffer.append(COLON);
        buffer.append(connector.getProvidedRole_AssemblyConnector().getEntityName());
        buffer.append(NEWLINE);
    }

    // helper method
    private AllocationContext assemblyToAllocationContext(final AssemblyContext assemblyContext) {
        return contexts.stream().filter(x -> x.getAssemblyContext_AllocationContext().equals(assemblyContext))
                .collect(Collectors.toList()).get(0);

    }

    private String getAllocationDiagramText() {
        final StringBuilder buffer = new StringBuilder();

        buffer.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
        buffer.append(NEWLINE);

        for (final ResourceContainer container : containerToContexts.keySet()) {
            appendContainerStart(container, buffer);
            for (final AllocationContext context : containerToContexts.get(container)) {
                appendAllocationContext(context, buffer);

            }
            appendContainerEnd(buffer);
        }

        for (final AssemblyConnector connector : connectors) {
            appendAssemblyConnector(connector, buffer);
        }
        return buffer.toString();
    }

    public String getDiagramText() {
        return diagramText;
    }

    private String getDiagramText(final Allocation allocation) {

        for (final AllocationContext context : allocation.getAllocationContexts_Allocation()) {
            contexts.add(context);
            final ResourceContainer container = context.getResourceContainer_AllocationContext();
            if (containerToContexts.containsKey(container)) {
                containerToContexts.get(container).add(context);
            } else {
                final List<AllocationContext> contextsToMap = new ArrayList<>();
                contextsToMap.add(context);
                containerToContexts.put(container, contextsToMap);
            }
        }
        for (final Connector connector : allocation.getSystem_Allocation().getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector) {
                connectors.add((AssemblyConnector) connector);
            }
        }
        linkToSystem = Helper.getEObjectHyperlink(allocation.getSystem_Allocation());

        return contexts.size() > 0 ? getAllocationDiagramText() : null;
    }
}
