package org.palladiosimulator.view.plantuml.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.core.composition.ProvidedDelegationConnector;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.system.System;

public class PcmSystemDiagramGenerator implements UmlDiagramSupplier {

	private static String COLON = " : ";
	private static String COMPONENT_KEYWORD = "component";
	private static String COMPONENT_START = "[", COMPONENT_END = "]";
	private static String CURLY_OPENING_BRACKET = "{", CURLY_CLOSING_BRACKET = "}";
	private static String INTERFACE_START = "(", INTERFACE_END = ")";
	private static String LINK_START = "[[", LINK_END = "]]";
	private static String NEWLINE = "\n";
	private static String PROVIDES_REQUIRES_LINK = " -(0- ";
	private static String SIMPLE_LINK = " - ";
	private static String SPACE = " ";

	// example: [Access Control] -(0- [Web Server] : REST
	private static void appendAssemblyConnector(final AssemblyConnector connector, final StringBuilder buffer) {
		buffer.append(PcmSystemDiagramGenerator.COMPONENT_START);
		// requiring context
		buffer.append(
		        UmlDiagramSupplier.escape(connector.getRequiringAssemblyContext_AssemblyConnector().getEntityName()));
		buffer.append(PcmSystemDiagramGenerator.COMPONENT_END);

		buffer.append(PcmSystemDiagramGenerator.PROVIDES_REQUIRES_LINK);

		buffer.append(PcmSystemDiagramGenerator.COMPONENT_START);
		// providing context
		buffer.append(
		        UmlDiagramSupplier.escape(connector.getProvidingAssemblyContext_AssemblyConnector().getEntityName()));
		buffer.append(PcmSystemDiagramGenerator.COMPONENT_END);

		buffer.append(PcmSystemDiagramGenerator.COLON);
		buffer.append(UmlDiagramSupplier.escape(connector.getProvidedRole_AssemblyConnector().getEntityName()));
		buffer.append(PcmSystemDiagramGenerator.NEWLINE);
	}

	// example: [FileStorage] [[link]]
	private static void appendAssemblyContext(final AssemblyContext context, final String linkToRepository,
	        final StringBuilder buffer) {
		buffer.append(PcmSystemDiagramGenerator.COMPONENT_START);
		buffer.append(UmlDiagramSupplier.escape(context.getEntityName()));
		buffer.append(PcmSystemDiagramGenerator.COMPONENT_END);
		buffer.append(PcmSystemDiagramGenerator.SPACE);
		buffer.append(PcmSystemDiagramGenerator.LINK_START);
		buffer.append(linkToRepository);
		buffer.append(PcmSystemDiagramGenerator.LINK_END);
		buffer.append(PcmSystemDiagramGenerator.NEWLINE);
	}

	private static void appendComponentEnd(final StringBuilder buffer) {
		buffer.append(PcmSystemDiagramGenerator.CURLY_CLOSING_BRACKET);
		buffer.append(PcmSystemDiagramGenerator.NEWLINE);
	}

	// example: component System {
	private static void appendComponentStart(final String name, final StringBuilder buffer) {
		buffer.append(PcmSystemDiagramGenerator.COMPONENT_KEYWORD);
		buffer.append(PcmSystemDiagramGenerator.SPACE);
		buffer.append(name);
		buffer.append(PcmSystemDiagramGenerator.SPACE);
		buffer.append(PcmSystemDiagramGenerator.CURLY_OPENING_BRACKET);
		buffer.append(PcmSystemDiagramGenerator.NEWLINE);

	}

	// example:
	// DataAccess - IMedia
	// IMedia - [Access Control]
	private static void appendProvidedDelConnector(final ProvidedDelegationConnector connector,
	        final StringBuilder buffer) {
		buffer.append(UmlDiagramSupplier
		        .escape(connector.getOuterProvidedRole_ProvidedDelegationConnector().getEntityName()));
		buffer.append(PcmSystemDiagramGenerator.SIMPLE_LINK);
		buffer.append(UmlDiagramSupplier
		        .escape(connector.getInnerProvidedRole_ProvidedDelegationConnector().getEntityName()));
		buffer.append(PcmSystemDiagramGenerator.NEWLINE);
		buffer.append(UmlDiagramSupplier
		        .escape(connector.getInnerProvidedRole_ProvidedDelegationConnector().getEntityName()));
		buffer.append(PcmSystemDiagramGenerator.SIMPLE_LINK);
		buffer.append(PcmSystemDiagramGenerator.COMPONENT_START);
		buffer.append(UmlDiagramSupplier.escape(connector.getInnerProvidedRole_ProvidedDelegationConnector()
		        .getProvidingEntity_ProvidedRole().getEntityName()));
		buffer.append(PcmSystemDiagramGenerator.COMPONENT_END);
		buffer.append(PcmSystemDiagramGenerator.NEWLINE);
	}

	// example: () DataAccess
	private static void appendProvidedRole(final String role, final StringBuilder buffer) {
		buffer.append(PcmSystemDiagramGenerator.INTERFACE_START);
		buffer.append(PcmSystemDiagramGenerator.INTERFACE_END);
		buffer.append(PcmSystemDiagramGenerator.SPACE);
		buffer.append(role);
		buffer.append(PcmSystemDiagramGenerator.NEWLINE);
	}

	private List<Connector> connectors = new ArrayList<>();

	private List<AssemblyContext> contexts = new ArrayList<>();

	private final String diagramText;

	private List<String> providedRoles = new ArrayList<>();

	private final String systemName;

	public PcmSystemDiagramGenerator(final System system) {
		diagramText = generateDiagramText(Objects.requireNonNull(system));
		systemName = system.getEntityName() != null ? system.getEntityName() : "System123";
	}

	private String generateDiagramText(final System system) {
		contexts = system.getAssemblyContexts__ComposedStructure().stream()
		        .filter(c -> (c != null) && (c.getEntityName() != null) && !c.getEntityName().isBlank()).distinct()
		        .sorted(UmlDiagramSupplier.byName()).toList();

		connectors = system.getConnectors__ComposedStructure().stream()
		        .filter(c -> (c != null) && (c.getEntityName() != null) && !c.getEntityName().isBlank()).distinct()
		        .sorted(UmlDiagramSupplier.byName()).toList();

		providedRoles = system.getProvidedRoles_InterfaceProvidingEntity().stream().filter(r -> r != null)
		        .map(ProvidedRole::getEntityName).filter(n -> (n != null) && !n.isBlank())
		        .map(UmlDiagramSupplier::escape).distinct().sorted().toList();

		return contexts.size() > 0 ? getSystemDiagramText() : null;
	}

	@Override
	public String get() {
		return diagramText;
	}

	private String getSystemDiagramText() {
		final StringBuilder buffer = new StringBuilder();

		buffer.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
		buffer.append(PcmSystemDiagramGenerator.NEWLINE);

		for (final String role : providedRoles) {
			PcmSystemDiagramGenerator.appendProvidedRole(role, buffer);
		}

		PcmSystemDiagramGenerator.appendComponentStart(systemName, buffer);

		for (final Connector connector : connectors) {
			if (connector instanceof AssemblyConnector) {
				PcmSystemDiagramGenerator.appendAssemblyConnector((AssemblyConnector) connector, buffer);
			} else if (connector instanceof ProvidedDelegationConnector) {
				PcmSystemDiagramGenerator.appendProvidedDelConnector((ProvidedDelegationConnector) connector, buffer);
			}
		}

		// for the contexts without connectors
		for (final AssemblyContext context : contexts) {
			final String linkToRepository = UmlDiagramSupplier.getEObjectHyperlink(
			        context.getEncapsulatedComponent__AssemblyContext().getRepository__RepositoryComponent());
			PcmSystemDiagramGenerator.appendAssemblyContext(context, linkToRepository, buffer);
		}

		PcmSystemDiagramGenerator.appendComponentEnd(buffer);
		return buffer.toString();
	}

}
