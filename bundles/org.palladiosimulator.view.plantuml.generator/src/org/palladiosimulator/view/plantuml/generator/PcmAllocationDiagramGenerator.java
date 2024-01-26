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
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;

public class PcmAllocationDiagramGenerator implements UmlDiagramSupplier {

	private static String COLON = " : ";
	private static String COMPONENT_START = "[", COMPONENT_END = "]";
	private static String CONTAINER_KEYWORD = "node";
	private static String CURLY_OPENING_BRACKET = "{", CURLY_CLOSING_BRACKET = "}";
	private static String LINK_START = "[[", LINK_END = "]]";
	private static String NEWLINE = "\n";
	private static String PROVIDES_REQUIRES_LINK = " -(0- ";
	private static String SPACE = " ";

	private static void appendContainerEnd(final StringBuilder buffer) {
		buffer.append(PcmAllocationDiagramGenerator.CURLY_CLOSING_BRACKET);
		buffer.append(PcmAllocationDiagramGenerator.NEWLINE);
	}

	// example: node System1 {
	private static void appendContainerStart(final ResourceContainer container, final StringBuilder buffer) {
		buffer.append(PcmAllocationDiagramGenerator.CONTAINER_KEYWORD);
		buffer.append(PcmAllocationDiagramGenerator.SPACE);
		buffer.append(UmlDiagramSupplier.escape(container.getEntityName()));
		buffer.append(PcmAllocationDiagramGenerator.SPACE);
		buffer.append(PcmAllocationDiagramGenerator.CURLY_OPENING_BRACKET);
		buffer.append(PcmAllocationDiagramGenerator.NEWLINE);
	}

	private List<AssemblyConnector> connectors = new ArrayList<>();
	private final Map<ResourceContainer, List<AllocationContext>> containerToContexts = new HashMap<>();
	private List<AllocationContext> contexts = new ArrayList<>();
	private final String diagramText;

	private String linkToSystem;

	public PcmAllocationDiagramGenerator(final Allocation allocation) {
		diagramText = getDiagramText(allocation);
	}

	// example: [DataAccess]
	private void appendAllocationContext(final AllocationContext context, final StringBuilder buffer) {
		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_START);
		buffer.append(UmlDiagramSupplier.escape(context.getEntityName()));
		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_END);
		buffer.append(PcmAllocationDiagramGenerator.SPACE);
		buffer.append(PcmAllocationDiagramGenerator.LINK_START);
		buffer.append(linkToSystem);
		buffer.append(PcmAllocationDiagramGenerator.LINK_END);
		buffer.append(PcmAllocationDiagramGenerator.NEWLINE);
	}

	// example: [Access Control] -(0- [Web Server] : REST
	private void appendAssemblyConnector(final AssemblyConnector connector, final StringBuilder buffer) {
		final AssemblyContext reqAssemblyContext = connector.getRequiringAssemblyContext_AssemblyConnector();
		final AllocationContext reqAllocContext = assemblyToAllocationContext(reqAssemblyContext);
		final String requiringName = UmlDiagramSupplier.escape(reqAllocContext.getEntityName());
		if ((reqAssemblyContext == null) || (reqAllocContext == null) || requiringName.isBlank()) {
			return;
		}

		final AssemblyContext provAssemblyContext = connector.getProvidingAssemblyContext_AssemblyConnector();
		final AllocationContext provAllocContext = assemblyToAllocationContext(provAssemblyContext);
		final String providingName = UmlDiagramSupplier.escape(provAllocContext.getEntityName());
		final String connectorName = UmlDiagramSupplier
		        .escape(connector.getProvidedRole_AssemblyConnector().getEntityName());
		if ((provAssemblyContext == null) || (provAllocContext == null) || providingName.isBlank()
		        || connectorName.isBlank()) {
			return;
		}

		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_START);

		// requiring context
		buffer.append(requiringName);
		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_END);
		buffer.append(PcmAllocationDiagramGenerator.PROVIDES_REQUIRES_LINK);
		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_START);

		// providing context
		buffer.append(providingName);
		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_END);
		buffer.append(PcmAllocationDiagramGenerator.COLON);
		buffer.append(connectorName);
		buffer.append(PcmAllocationDiagramGenerator.NEWLINE);
	}

	// helper method
	private AllocationContext assemblyToAllocationContext(final AssemblyContext assemblyContext) {
		return contexts.stream().filter(x -> x.getAssemblyContext_AllocationContext().equals(assemblyContext))
		        .collect(Collectors.toList()).get(0);

	}

	@Override
	public String get() {
		return diagramText;
	}

	private String getAllocationDiagramText() {
		final StringBuilder buffer = new StringBuilder();

		buffer.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
		buffer.append(PcmAllocationDiagramGenerator.NEWLINE);

		for (final ResourceContainer container : containerToContexts.keySet()) {
			PcmAllocationDiagramGenerator.appendContainerStart(container, buffer);
			for (final AllocationContext context : containerToContexts.get(container)) {
				appendAllocationContext(context, buffer);

			}
			PcmAllocationDiagramGenerator.appendContainerEnd(buffer);
		}

		for (final AssemblyConnector connector : connectors) {
			appendAssemblyConnector(connector, buffer);
		}
		return buffer.toString();
	}

	private String getDiagramText(final Allocation allocation) {

		contexts = allocation.getAllocationContexts_Allocation().stream().filter(context -> context != null)
		        .filter(context -> (context.getEntityName() != null) && !context.getEntityName().isBlank())
		        .filter(context -> context.getResourceContainer_AllocationContext() != null).distinct()
		        .sorted(UmlDiagramSupplier.byName()).toList();

		contexts.forEach(context -> {
			final ResourceContainer container = context.getResourceContainer_AllocationContext();
			if (containerToContexts.containsKey(container)) {
				containerToContexts.get(container).add(context);
			} else {
				final List<AllocationContext> contextsToMap = new ArrayList<>();
				contextsToMap.add(context);
				containerToContexts.put(container, contextsToMap);
			}
		});

		connectors = allocation.getSystem_Allocation().getConnectors__ComposedStructure().stream()
		        .filter(connector -> connector != null).filter(AssemblyConnector.class::isInstance)
		        .map(AssemblyConnector.class::cast)
		        .filter(connector -> (connector.getEntityName() != null) && !connector.getEntityName().isBlank())
		        .distinct().sorted(UmlDiagramSupplier.byName()).toList();

		linkToSystem = UmlDiagramSupplier.getEObjectHyperlink(allocation.getSystem_Allocation());

		return contexts.size() > 0 ? getAllocationDiagramText() : "";
	}
}
