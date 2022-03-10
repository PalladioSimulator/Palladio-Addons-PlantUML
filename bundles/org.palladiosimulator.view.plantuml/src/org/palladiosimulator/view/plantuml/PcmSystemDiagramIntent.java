package org.palladiosimulator.view.plantuml;

import java.util.ArrayList;
import java.util.List;

import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.core.composition.ProvidedDelegationConnector;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.system.System;

import net.sourceforge.plantuml.ecore.EcoreDiagramHelper;
import net.sourceforge.plantuml.text.AbstractDiagramIntent;

public class PcmSystemDiagramIntent extends AbstractDiagramIntent<System> {

	public PcmSystemDiagramIntent(System source) {
		super(source);
	}

	private static String COMPONENT_START = "[", COMPONENT_END = "]";
	private static String LINK_START = "[[", LINK_END = "]]";
	private static String INTERFACE_START = "(", INTERFACE_END = ")";
	private static String CURLY_OPENING_BRACKET = "{", CURLY_CLOSING_BRACKET = "}";
	private static String COLON = " : ";
	private static String SIMPLE_LINK = " - ";
	private static String PROVIDES_REQUIRES_LINK = " -(0- ";
	private static String NEWLINE = "\n";
	private static String SPACE = " ";
	private static String SYSTEM_NAME = "System";
	private static String COMPONENT_KEYWORD = "component";

	@Override
	public String getDiagramText() {
		return getDiagramText(getSource());
	}

	private final List<AssemblyContext> contexts = new ArrayList<>();
	private final List<Connector> connectors = new ArrayList<>();
	private final List<ProvidedRole> providedRoles = new ArrayList<>(); 

	protected String getDiagramText(final System system) {
		for (AssemblyContext context : system.getAssemblyContexts__ComposedStructure()) {
			contexts.add(context);
		}

		for (Connector connector : system.getConnectors__ComposedStructure()) {
			connectors.add(connector);
		}

		for (ProvidedRole role : system.getProvidedRoles_InterfaceProvidingEntity()) {
			providedRoles.add(role);
		}

		final String result = contexts.size() > 0 ? getSystemDiagramText() : null;
		return result;
	}

	protected String getSystemDiagramText() {
		final StringBuilder buffer = new StringBuilder();
		final EcoreDiagramHelper helper = new EcoreDiagramHelper();

		buffer.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
		buffer.append(NEWLINE);

		for (final ProvidedRole role : providedRoles) {
			appendProvidedRole(role, buffer);
		}

		appendComponentStart(SYSTEM_NAME, buffer);

		for (final Connector connector : connectors) {
			if (connector instanceof AssemblyConnector) {
				appendAssemblyConnector((AssemblyConnector) connector, buffer);
			} else if (connector instanceof ProvidedDelegationConnector) {
				appendProvidedDelConnector((ProvidedDelegationConnector) connector, buffer);
			}
		}

		// for the contexts without connectors
		for (final AssemblyContext context : contexts) {
			String linkToRepository = helper.getEObjectHyperlink(
					context.getEncapsulatedComponent__AssemblyContext().getRepository__RepositoryComponent());
			appendAssemblyContext(context, linkToRepository, buffer);
		}

		appendComponentEnd(buffer);
		return buffer.toString();
	}

	// example: () DataAccess
	protected void appendProvidedRole(final ProvidedRole role, final StringBuilder buffer) {
		buffer.append(INTERFACE_START);
		buffer.append(INTERFACE_END);
		buffer.append(SPACE);
		buffer.append(role.getEntityName());
		buffer.append(NEWLINE);
	}

	// example: component System {
	protected void appendComponentStart(final String name, final StringBuilder buffer) {
		buffer.append(COMPONENT_KEYWORD);
		buffer.append(SPACE);
		buffer.append(name);
		buffer.append(SPACE);
		buffer.append(CURLY_OPENING_BRACKET);
		buffer.append(NEWLINE);

	}
	// example: [FileStorage] [[link]]
	protected void appendAssemblyContext(final AssemblyContext context, final String linkToRepository,
			final StringBuilder buffer) {
		buffer.append(COMPONENT_START);
		buffer.append(context.getEntityName());
		buffer.append(COMPONENT_END);
		buffer.append(SPACE);
		buffer.append(LINK_START);
		buffer.append(linkToRepository);
		buffer.append(LINK_END);
		buffer.append(NEWLINE);
	}

	// example: [Access Control] -(0- [Web Server] : REST
	protected void appendAssemblyConnector(final AssemblyConnector connector, final StringBuilder buffer) {
		buffer.append(COMPONENT_START);
		// requiring context
		buffer.append(connector.getRequiringAssemblyContext_AssemblyConnector().getEntityName());
		buffer.append(COMPONENT_END);

		buffer.append(PROVIDES_REQUIRES_LINK);

		buffer.append(COMPONENT_START);
		// providing context
		buffer.append(connector.getProvidingAssemblyContext_AssemblyConnector().getEntityName());
		buffer.append(COMPONENT_END);

		buffer.append(COLON);
		buffer.append(connector.getProvidedRole_AssemblyConnector().getEntityName());
		buffer.append(NEWLINE);
	}

	// example: 
	// DataAccess - IMedia
	// IMedia - [Access Control]
	protected void appendProvidedDelConnector(final ProvidedDelegationConnector connector, final StringBuilder buffer) {
		buffer.append(connector.getOuterProvidedRole_ProvidedDelegationConnector().getEntityName());
		buffer.append(SIMPLE_LINK);
		buffer.append(connector.getInnerProvidedRole_ProvidedDelegationConnector().getEntityName());
		buffer.append(NEWLINE);
		buffer.append(connector.getInnerProvidedRole_ProvidedDelegationConnector().getEntityName());
		buffer.append(SIMPLE_LINK);
		buffer.append(COMPONENT_START);
		buffer.append(connector.getInnerProvidedRole_ProvidedDelegationConnector().getProvidingEntity_ProvidedRole()
				.getEntityName());
		buffer.append(COMPONENT_END);
		buffer.append(NEWLINE);
	}

	protected void appendComponentEnd(StringBuilder buffer) {
		buffer.append(CURLY_CLOSING_BRACKET);
		buffer.append(NEWLINE);
	}
}
