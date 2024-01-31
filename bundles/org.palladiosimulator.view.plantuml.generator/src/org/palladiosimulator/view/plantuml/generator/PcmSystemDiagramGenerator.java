package org.palladiosimulator.view.plantuml.generator;

import static org.palladiosimulator.view.plantuml.generator.UmlDiagramSupplier.byName;
import static org.palladiosimulator.view.plantuml.generator.UmlDiagramSupplier.escape;
import static org.palladiosimulator.view.plantuml.generator.UmlDiagramSupplier.getEObjectHyperlink;

import java.util.List;
import java.util.Objects;

import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.core.composition.ProvidedDelegationConnector;
import org.palladiosimulator.pcm.core.entity.InterfaceProvidingEntity;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.CompositeComponent;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.system.System;

public class PcmSystemDiagramGenerator implements UmlDiagramSupplier {

	private static final String COLON = " : ";
	private static final String COMPONENT_START = "[", COMPONENT_END = "]";
	private static final String CURLY_OPENING_BRACKET = "{", CURLY_CLOSING_BRACKET = "}";
	private static final String DEFAULT_NAME = "System";
	private static final String INTERFACE_START = "(", INTERFACE_END = ")";
	private static final String LINK_START = "[[", LINK_END = "]]";
	private static final String NEWLINE = "\n";
	private static final String PORT = "port ";
	private static final String PROVIDES_REQUIRES_LINK = " -(0- ";
	private static final String RECTANGLE_KEYWORD = "rectangle";
	private static final String SIMPLE_LINK = " - ";
	private static final String SPACE = " ";

	private final StringBuilder builder;
	private final List<BasicComponent> components;
	private final List<Connector> connectors;
	private final String diagramText;
	private final List<String> providedRoles;
	private final String systemName;

	public PcmSystemDiagramGenerator(final System system) {
		builder = new StringBuilder();

		systemName = getSystemName(Objects.requireNonNull(system));

		components = system.getAssemblyContexts__ComposedStructure().stream().filter(c -> (c != null))
		        .map(AssemblyContext::getEncapsulatedComponent__AssemblyContext)
		        .filter(c -> (c != null) && (c.getEntityName() != null) && !c.getEntityName().isBlank()).distinct()
		        .filter(BasicComponent.class::isInstance).map(BasicComponent.class::cast).sorted(byName()).toList();

		connectors = system.getConnectors__ComposedStructure().stream()
		        .filter(c -> (c != null) && (c.getEntityName() != null) && !c.getEntityName().isBlank()).distinct()
		        .sorted(byName()).toList();

		providedRoles = system.getProvidedRoles_InterfaceProvidingEntity().stream().filter(r -> r != null)
		        .map(ProvidedRole::getEntityName).filter(n -> (n != null) && !n.isBlank())
		        .map(UmlDiagramSupplier::escape).distinct().sorted().toList();

		diagramText = components.isEmpty() ? "" : getSystemDiagramText();

	}

	// example: [Access Control] -(0- [Web Server] : REST
	private void appendAssemblyConnector(final AssemblyConnector connector) {

		final RepositoryComponent requiring = connector.getRequiringAssemblyContext_AssemblyConnector()
		        .getEncapsulatedComponent__AssemblyContext();

		final RepositoryComponent providing = connector.getProvidingAssemblyContext_AssemblyConnector()
		        .getEncapsulatedComponent__AssemblyContext();

		if (components.contains(requiring) && components.contains(providing)) {
			builder.append(COMPONENT_START);

			// requiring context
			builder.append(escape(requiring.getEntityName()));
			builder.append(COMPONENT_END);
			builder.append(PROVIDES_REQUIRES_LINK);
			builder.append(COMPONENT_START);

			// providing context
			builder.append(escape(providing.getEntityName()));
			builder.append(COMPONENT_END);
			builder.append(COLON);
			builder.append(escape(connector.getProvidedRole_AssemblyConnector().getEntityName()));
			builder.append(NEWLINE);
		}

	}

	// example: [FileStorage] [[link]]
	private void appendComponent(final BasicComponent component) {
		builder.append(COMPONENT_START);
		builder.append(escape(component.getEntityName()));
		builder.append(COMPONENT_END);
		builder.append(SPACE);
		builder.append(LINK_START);
		builder.append(getEObjectHyperlink(component.getRepository__RepositoryComponent()));
		builder.append(LINK_END);
		builder.append(NEWLINE);
	}

	// example:
	// DataAccess - IMedia
	// IMedia - [Access Control]
	private void appendProvidedDelConnector(final ProvidedDelegationConnector connector) {
		final String innerProvidedRole = escape(
		        connector.getInnerProvidedRole_ProvidedDelegationConnector().getEntityName());
		InterfaceProvidingEntity providingEntity = connector.getInnerProvidedRole_ProvidedDelegationConnector()
		        .getProvidingEntity_ProvidedRole();
		if (providingEntity instanceof final CompositeComponent composite) {
			providingEntity = composite.getConnectors__ComposedStructure().stream()
			        .filter(ProvidedDelegationConnector.class::isInstance).map(ProvidedDelegationConnector.class::cast)
			        .filter(c -> (c != null) && (c.getEntityName() != null) && !c.getEntityName().isBlank()).distinct()
			        .sorted(byName())
			        .filter(c -> (escape(c.getOuterProvidedRole_ProvidedDelegationConnector().getEntityName())
			                .equals(innerProvidedRole)))
			        .findFirst().map(ProvidedDelegationConnector::getAssemblyContext_ProvidedDelegationConnector)
			        .map(AssemblyContext::getEncapsulatedComponent__AssemblyContext)
			        .orElse((CompositeComponent) providingEntity);
		}

		if (components.contains(providingEntity)) {

			builder.append(PORT);
			builder.append(innerProvidedRole);
			builder.append(NEWLINE);

			builder.append(escape(connector.getOuterProvidedRole_ProvidedDelegationConnector().getEntityName()));
			builder.append(SIMPLE_LINK);
			builder.append(innerProvidedRole);
			builder.append(NEWLINE);
			builder.append(innerProvidedRole);
			builder.append(SIMPLE_LINK);
			builder.append(COMPONENT_START);
			builder.append(escape(providingEntity.getEntityName()));
			builder.append(COMPONENT_END);
			builder.append(NEWLINE);
		}

	}

	// example: () DataAccess
	private void appendProvidedRole(final String role) {
		builder.append(INTERFACE_START);
		builder.append(INTERFACE_END);
		builder.append(SPACE);
		builder.append(role);
		builder.append(NEWLINE);
	}

	// example: }
	private void appendSystemEnd() {
		builder.append(CURLY_CLOSING_BRACKET);
		builder.append(NEWLINE);
	}

	// example: rectangle System {
	private void appendSystemStart(final String name) {
		builder.append(RECTANGLE_KEYWORD);
		builder.append(SPACE);
		builder.append(name);
		builder.append(SPACE);
		builder.append(CURLY_OPENING_BRACKET);
		builder.append(NEWLINE);

	}

	@Override
	public String get() {
		return diagramText;
	}

	private String getSystemDiagramText() {
		builder.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
		builder.append(NEWLINE);
		builder.append("skinparam componentStyle uml2"); // UML2 Style
		builder.append(NEWLINE);

		providedRoles.forEach(this::appendProvidedRole);

		appendSystemStart(systemName);

		components.forEach(this::appendComponent);

		for (final Connector connector : connectors) {
			if (connector instanceof AssemblyConnector) {
				appendAssemblyConnector((AssemblyConnector) connector);
			} else if (connector instanceof ProvidedDelegationConnector) {
				appendProvidedDelConnector((ProvidedDelegationConnector) connector);
			}
		}

		appendSystemEnd();
		return builder.toString();
	}

	private String getSystemName(final System system) {
		if ((system == null) || (system.getEntityName() == null) || system.getEntityName().isBlank()) {
			return DEFAULT_NAME;
		}
		final String name = escape(system.getEntityName());
		if (name.isBlank() || "null".equalsIgnoreCase(name) || "aName".equalsIgnoreCase(name)) {
			return DEFAULT_NAME;
		}
		return name;
	}

}
