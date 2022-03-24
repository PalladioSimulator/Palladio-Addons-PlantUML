package org.palladiosimulator.view.plantuml;

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

import net.sourceforge.plantuml.ecore.EcoreDiagramHelper;
import net.sourceforge.plantuml.text.AbstractDiagramIntent;

public class PcmAllocationDiagramIntent extends AbstractDiagramIntent<Allocation> {

	public PcmAllocationDiagramIntent(Allocation source) {
		super(source);
	}

	private static String COMPONENT_START = "[", COMPONENT_END = "]";
	private static String LINK_START = "[[", LINK_END = "]]";
	private static String CURLY_OPENING_BRACKET = "{", CURLY_CLOSING_BRACKET = "}";
	private static String COLON = " : ";
	private static String PROVIDES_REQUIRES_LINK = " -(0- ";
	private static String NEWLINE = "\n";
	private static String SPACE = " ";
	private static String CONTAINER_KEYWORD = "node";

	private final List<AllocationContext> contexts = new ArrayList<>();
	private final Map<ResourceContainer, List<AllocationContext>> containerToContexts = new HashMap<>();
	private final List<AssemblyConnector> connectors = new ArrayList<>();
	private String linkToSystem;

	@Override
	public String getDiagramText() {
		return getDiagramText(getSource());
	}

	private String getDiagramText(final Allocation allocation) {
		
		for (AllocationContext context : allocation.getAllocationContexts_Allocation()) {
			contexts.add(context);
			ResourceContainer container = context.getResourceContainer_AllocationContext();
			if (containerToContexts.containsKey(container)) {
				containerToContexts.get(container).add(context);
			} else {
				List<AllocationContext> contextsToMap = new ArrayList<>();
				contextsToMap.add(context);
				containerToContexts.put(container, contextsToMap);
			}
		}
		for (Connector connector : allocation.getSystem_Allocation().getConnectors__ComposedStructure()) {
			if (connector instanceof AssemblyConnector) {
				connectors.add((AssemblyConnector) connector);
			}
		}
		EcoreDiagramHelper diagramHelper = new EcoreDiagramHelper();
		linkToSystem = diagramHelper.getEObjectHyperlink(allocation.getSystem_Allocation());

		final String result = contexts.size() > 0 ? getAllocationDiagramText() : null;
		return result;
	}

	private String getAllocationDiagramText() {
		final StringBuilder buffer = new StringBuilder();

		buffer.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
		buffer.append(NEWLINE);

		for (ResourceContainer container : containerToContexts.keySet()) {
			appendContainerStart(container, buffer);
			for (AllocationContext context : containerToContexts.get(container)) {
				appendAllocationContext(context, buffer);

			}
			appendContainerEnd(container, buffer);
		}

		for (AssemblyConnector connector : connectors) {
			appendAssemblyConnector(connector, buffer);
		}
		return buffer.toString();
	}

	// example: node System1 {
	protected void appendContainerStart(final ResourceContainer container, StringBuilder buffer) {
		buffer.append(CONTAINER_KEYWORD);
		buffer.append(SPACE);
		buffer.append(container.getEntityName());
		buffer.append(SPACE);
		buffer.append(CURLY_OPENING_BRACKET);
		buffer.append(NEWLINE);
	}

	// example: [DataAccess]
	protected void appendAllocationContext(final AllocationContext context, StringBuilder buffer) {
		buffer.append(COMPONENT_START);
		buffer.append(context.getEntityName());
		buffer.append(COMPONENT_END);
		buffer.append(SPACE);
		buffer.append(LINK_START);
		buffer.append(linkToSystem);
		buffer.append(LINK_END);
		buffer.append(NEWLINE);
	}

	private void appendContainerEnd(final ResourceContainer container, StringBuilder buffer) {
		buffer.append(CURLY_CLOSING_BRACKET);
		buffer.append(NEWLINE);
	}

	// example: [Access Control] -(0- [Web Server] : REST
	protected void appendAssemblyConnector(final AssemblyConnector connector, final StringBuilder buffer) {
		buffer.append(COMPONENT_START);

		// requiring context
		AssemblyContext reqAssemblyContext = connector.getRequiringAssemblyContext_AssemblyConnector();
		AllocationContext reqAllocContext = assemblyToAllocationContext(reqAssemblyContext);
		buffer.append(reqAllocContext.getEntityName());
		buffer.append(COMPONENT_END);

		buffer.append(PROVIDES_REQUIRES_LINK);

		buffer.append(COMPONENT_START);

		// providing context
		AssemblyContext provAssemblyContext = connector.getProvidingAssemblyContext_AssemblyConnector();
		AllocationContext provAllocContext = assemblyToAllocationContext(provAssemblyContext);
		buffer.append(provAllocContext.getEntityName());
		buffer.append(COMPONENT_END);

		buffer.append(COLON);
		buffer.append(connector.getProvidedRole_AssemblyConnector().getEntityName());
		buffer.append(NEWLINE);
	}

	// helper method
	protected AllocationContext assemblyToAllocationContext(AssemblyContext assemblyContext) {
		return contexts.stream().filter(x -> x.getAssemblyContext_AllocationContext().equals(assemblyContext))
				.collect(Collectors.toList()).get(0);

	}
}
