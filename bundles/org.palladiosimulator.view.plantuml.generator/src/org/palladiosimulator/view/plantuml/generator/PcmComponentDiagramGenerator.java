package org.palladiosimulator.view.plantuml.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.ProvidedDelegationConnector;
import org.palladiosimulator.pcm.core.composition.RequiredDelegationConnector;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.CompositeComponent;
import org.palladiosimulator.pcm.repository.Interface;
import org.palladiosimulator.pcm.repository.OperationInterface;
import org.palladiosimulator.pcm.repository.OperationProvidedRole;
import org.palladiosimulator.pcm.repository.OperationRequiredRole;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Role;

public class PcmComponentDiagramGenerator implements UmlDiagramSupplier {

	private static final String COMPONENT_START = "[", COMPONENT_END = "]";
	private static final String COMPOSITE_BLOCK_END = "}";
	private static final String COMPOSITE_BLOCK_START = PcmComponentDiagramGenerator.NAME_END + " {";
	private static final String COMPOSITE_COMPONENT_START = "component " + PcmComponentDiagramGenerator.NAME_START;
	private static final String COMPOSITE_TITLE_SPACER = "\\n\\n\\n\\n\\n\\n";
	private static final String INPORT_DECLARATION = "portin \" \" as ";
	private static final String INPORT_DELIMITER = ".requires.";
	private static final String NAME_START = "\"", NAME_END = "\"";
	private static final String NEWLINE = "\n";
	private static final String OUTPORT_DECLARATION = "portout \" \" as ";
	private static final String OUTPORT_DELIMITER = ".provides.";
	private static final String REQUIRES_LABEL = " : requires";
	private static final String SIMPLE_LINK = "--", REQUIRES_LINK = "..>", INTERNAL_REQUIRES_LINK = "..";

	private static void appendComponent(final BasicComponent component, final StringBuilder buffer) {
		buffer.append(PcmComponentDiagramGenerator.COMPONENT_START);
		buffer.append(UmlDiagramSupplier.escape(component.getEntityName()));
		buffer.append(PcmComponentDiagramGenerator.COMPONENT_END);
	}

	private static void appendConnector(final AssemblyConnector connector, final StringBuilder buffer) {
		final AssemblyContext requiringContext = connector.getRequiringAssemblyContext_AssemblyConnector();
		final AssemblyContext providingContext = connector.getProvidingAssemblyContext_AssemblyConnector();

		// TODO: Currently assumes non-nested components.
		PcmComponentDiagramGenerator
		        .appendComponent((BasicComponent) requiringContext.getEncapsulatedComponent__AssemblyContext(), buffer);
		buffer.append(PcmComponentDiagramGenerator.REQUIRES_LINK);
		PcmComponentDiagramGenerator
		        .appendComponent((BasicComponent) providingContext.getEncapsulatedComponent__AssemblyContext(), buffer);
		buffer.append(PcmComponentDiagramGenerator.REQUIRES_LABEL);
		buffer.append(PcmComponentDiagramGenerator.NEWLINE);
	}

	private final List<BasicComponent> basicComponents = new ArrayList<>();
	private final Set<String> componentNames = new HashSet<>();
	private final List<CompositeComponent> compositeComponents = new ArrayList<>();
	private final String diagramText;
	private final List<OperationInterface> ifaces = new ArrayList<>();
	private final Set<RepositoryComponent> innerComponents = new HashSet<>();
	private final Map<CompositeComponent, Map<Role, String>> inPorts = new HashMap<>();

	private final Map<CompositeComponent, Map<Role, String>> outPorts = new HashMap<>();

	private final Map<RepositoryComponent, EList<ProvidedRole>> providedRoles = new HashMap<>();

	private final Map<RepositoryComponent, EList<RequiredRole>> requiredRoles = new HashMap<>();

	public PcmComponentDiagramGenerator(final Repository repository) {
		diagramText = generateDiagramText(Objects.requireNonNull(repository));
	}

	private void addInnerComponents(final CompositeComponent component) {
		for (final AssemblyContext assemblyContext : component.getAssemblyContexts__ComposedStructure()) {
			final RepositoryComponent innerComponent = assemblyContext.getEncapsulatedComponent__AssemblyContext();

			innerComponents.add(innerComponent);

			final Set<ProvidedRole> connectedProvisions = component.getConnectors__ComposedStructure().stream()
			        .filter(AssemblyConnector.class::isInstance).map(AssemblyConnector.class::cast)
			        .map(AssemblyConnector::getProvidedRole_AssemblyConnector)
			        .sorted((a, b) -> UmlDiagramSupplier
			                .escape(a.getProvidedInterface__OperationProvidedRole().getEntityName())
			                .compareTo(UmlDiagramSupplier
			                        .escape(b.getProvidedInterface__OperationProvidedRole().getEntityName())))
			        .collect(Collectors.toSet());

			final Set<RequiredRole> connectedRequirements = component.getConnectors__ComposedStructure().stream()
			        .filter(AssemblyConnector.class::isInstance).map(AssemblyConnector.class::cast)
			        .map(AssemblyConnector::getRequiredRole_AssemblyConnector)
			        .sorted((a, b) -> UmlDiagramSupplier
			                .escape(a.getRequiredInterface__OperationRequiredRole().getEntityName())
			                .compareTo(UmlDiagramSupplier
			                        .escape(b.getRequiredInterface__OperationRequiredRole().getEntityName())))
			        .collect(Collectors.toSet());

			final EList<ProvidedRole> innerProvisions = innerComponent.getProvidedRoles_InterfaceProvidingEntity();
			innerProvisions.removeAll(connectedProvisions);
			ECollections.sort(innerProvisions, UmlDiagramSupplier.byName());

			final EList<RequiredRole> innerRequirements = innerComponent.getRequiredRoles_InterfaceRequiringEntity();
			innerRequirements.removeAll(connectedRequirements);
			ECollections.sort(innerRequirements, UmlDiagramSupplier.byName());

			providedRoles.put(innerComponent, innerProvisions);
			requiredRoles.put(innerComponent, innerRequirements);
		}
	}

	private void appendComponent(final CompositeComponent component, final StringBuilder buffer) {
		buffer.append(PcmComponentDiagramGenerator.COMPOSITE_COMPONENT_START);
		buffer.append(component.getEntityName());
		buffer.append(PcmComponentDiagramGenerator.COMPOSITE_TITLE_SPACER);
		buffer.append(PcmComponentDiagramGenerator.COMPOSITE_BLOCK_START);
		buffer.append(PcmComponentDiagramGenerator.NEWLINE);
		component.getAssemblyContexts__ComposedStructure().stream()
		        .map(AssemblyContext::getEncapsulatedComponent__AssemblyContext)
		        .filter(BasicComponent.class::isInstance).map(BasicComponent.class::cast).forEach(x -> {
			        PcmComponentDiagramGenerator.appendComponent(x, buffer);
			        buffer.append(PcmComponentDiagramGenerator.NEWLINE);
		        });
		inPorts.get(component).values().forEach(x -> {
			buffer.append(PcmComponentDiagramGenerator.INPORT_DECLARATION);
			buffer.append(x);
			buffer.append(PcmComponentDiagramGenerator.NEWLINE);
		});
		outPorts.get(component).values().forEach(x -> {
			buffer.append(PcmComponentDiagramGenerator.OUTPORT_DECLARATION);
			buffer.append(x);
			buffer.append(PcmComponentDiagramGenerator.NEWLINE);
		});
		component.getConnectors__ComposedStructure().stream().filter(AssemblyConnector.class::isInstance)
		        .map(AssemblyConnector.class::cast).sorted(UmlDiagramSupplier.byName())
		        .forEach(x -> PcmComponentDiagramGenerator.appendConnector(x, buffer));
		component.getConnectors__ComposedStructure().stream().filter(ProvidedDelegationConnector.class::isInstance)
		        .map(ProvidedDelegationConnector.class::cast).sorted(UmlDiagramSupplier.byName())
		        .forEach(x -> appendDelegation(component, x, buffer));
		component.getConnectors__ComposedStructure().stream().filter(RequiredDelegationConnector.class::isInstance)
		        .map(RequiredDelegationConnector.class::cast).sorted(UmlDiagramSupplier.byName())
		        .forEach(x -> appendDelegation(component, x, buffer));
		buffer.append(PcmComponentDiagramGenerator.COMPOSITE_BLOCK_END);
	}

	private void appendDelegation(final CompositeComponent parent, final ProvidedDelegationConnector delegation,
	        final StringBuilder buffer) {
		final AssemblyContext providingContext = delegation.getAssemblyContext_ProvidedDelegationConnector();
		final String portName = inPorts.get(parent).get(delegation.getOuterProvidedRole_ProvidedDelegationConnector());

		// TODO: Currently assumes non-nested components.
		buffer.append(PcmComponentDiagramGenerator.NAME_START);
		buffer.append(portName);
		buffer.append(PcmComponentDiagramGenerator.NAME_END);
		buffer.append(PcmComponentDiagramGenerator.SIMPLE_LINK);
		PcmComponentDiagramGenerator
		        .appendComponent((BasicComponent) providingContext.getEncapsulatedComponent__AssemblyContext(), buffer);
		buffer.append(PcmComponentDiagramGenerator.NEWLINE);
	}

	protected void appendDelegation(final CompositeComponent parent, final RequiredDelegationConnector delegation,
	        final StringBuilder buffer) {
		final AssemblyContext requiringContext = delegation.getAssemblyContext_RequiredDelegationConnector();
		final String portName = outPorts.get(parent).get(delegation.getOuterRequiredRole_RequiredDelegationConnector());

		// TODO: Currently assumes non-nested components.
		PcmComponentDiagramGenerator
		        .appendComponent((BasicComponent) requiringContext.getEncapsulatedComponent__AssemblyContext(), buffer);
		buffer.append(PcmComponentDiagramGenerator.INTERNAL_REQUIRES_LINK);
		buffer.append(PcmComponentDiagramGenerator.NAME_START);
		buffer.append(portName);
		buffer.append(PcmComponentDiagramGenerator.NAME_END);
		buffer.append(PcmComponentDiagramGenerator.NEWLINE);
	}

	private String appendIface(final Role role, final StringBuilder buffer) {
		final String ifaceName = getIFaceByRef(role).getEntityName();
		final String ifaceIdentifier = "interface." + UmlDiagramSupplier.escape(ifaceName);
		buffer.append("interface ");
		buffer.append(PcmComponentDiagramGenerator.NAME_START);
		buffer.append(ifaceName);
		buffer.append(PcmComponentDiagramGenerator.NAME_END);
		buffer.append(" as ");
		buffer.append(ifaceIdentifier);
		buffer.append(PcmComponentDiagramGenerator.NEWLINE);
		return ifaceIdentifier;
	}

	private void appendProvIfaces(final BasicComponent component, final StringBuilder buffer) {
		for (final ProvidedRole provRole : providedRoles.get(component)) {
			final String ifaceName = UmlDiagramSupplier.escape(getIFaceByRef(provRole).getEntityName());
			// Do not draw implicit interfaces.
			if (componentNames.contains(ifaceName)) {
				continue;
			}

			final String ifaceIdentifier = appendIface(provRole, buffer);

			buffer.append(ifaceIdentifier);
			buffer.append(PcmComponentDiagramGenerator.SIMPLE_LINK);
			PcmComponentDiagramGenerator.appendComponent(component, buffer);
			buffer.append(PcmComponentDiagramGenerator.NEWLINE);
		}
	}

	private void appendProvIfaces(final CompositeComponent component, final StringBuilder buffer) {
		for (final ProvidedRole provRole : providedRoles.get(component)) {
			final String portName = inPorts.get(component).get(provRole);
			final String ifaceIdentifier = appendIface(provRole, buffer);

			buffer.append(ifaceIdentifier);
			buffer.append(PcmComponentDiagramGenerator.SIMPLE_LINK);
			buffer.append(PcmComponentDiagramGenerator.NAME_START);
			buffer.append(portName);
			buffer.append(PcmComponentDiagramGenerator.NAME_END);
			buffer.append(PcmComponentDiagramGenerator.NEWLINE);
		}
	}

	// example: [First Component] ..> HTTP : requires
	private void appendReqIfaces(final BasicComponent component, final StringBuilder buffer) {
		for (final RequiredRole reqRole : requiredRoles.get(component)) {
			// Refer directly to the component for implicit interfaces.
			final String ifaceName = UmlDiagramSupplier.escape(getIFaceByRef(reqRole).getEntityName());
			if (componentNames.contains(ifaceName)) {
				PcmComponentDiagramGenerator.appendComponent(component, buffer);
				buffer.append(PcmComponentDiagramGenerator.REQUIRES_LINK);
				buffer.append(PcmComponentDiagramGenerator.COMPONENT_START);
				buffer.append(ifaceName);
				buffer.append(PcmComponentDiagramGenerator.COMPONENT_END);
			} else {
				final String ifaceIdentifier = appendIface(reqRole, buffer);
				PcmComponentDiagramGenerator.appendComponent(component, buffer);
				buffer.append(PcmComponentDiagramGenerator.REQUIRES_LINK);
				buffer.append(ifaceIdentifier);
			}
			buffer.append(PcmComponentDiagramGenerator.REQUIRES_LABEL);
			buffer.append(PcmComponentDiagramGenerator.NEWLINE);
		}
	}

	// example: [First Component] ..> HTTP : requires
	private void appendReqIfaces(final CompositeComponent component, final StringBuilder buffer) {
		for (final RequiredRole reqRole : requiredRoles.get(component)) {
			final String portName = outPorts.get(component).get(reqRole);
			final String ifaceIdentifier = appendIface(reqRole, buffer);

			buffer.append(PcmComponentDiagramGenerator.NAME_START);
			buffer.append(portName);
			buffer.append(PcmComponentDiagramGenerator.NAME_END);
			buffer.append(PcmComponentDiagramGenerator.REQUIRES_LINK);
			buffer.append(ifaceIdentifier);
			buffer.append(PcmComponentDiagramGenerator.REQUIRES_LABEL);
			buffer.append(PcmComponentDiagramGenerator.NEWLINE);
		}
	}

	private void createPorts(final CompositeComponent component) {
		final HashMap<Role, String> inPortNames = new HashMap<>();
		for (final ProvidedRole role : providedRoles.getOrDefault(component, ECollections.asEList())) {
			if (role instanceof OperationProvidedRole) {
				final String interfaceName = ((OperationProvidedRole) role)
				        .getProvidedInterface__OperationProvidedRole().getEntityName();
				final String componentName = component.getEntityName();
				final String name = componentName + PcmComponentDiagramGenerator.INPORT_DELIMITER + interfaceName;
				inPortNames.put(role, UmlDiagramSupplier.escape(name));
			}
		}
		inPorts.put(component, inPortNames);

		final HashMap<Role, String> outPortNames = new HashMap<>();
		for (final RequiredRole role : requiredRoles.getOrDefault(component, ECollections.asEList())) {
			if (role instanceof OperationRequiredRole) {
				final String interfaceName = ((OperationRequiredRole) role)
				        .getRequiredInterface__OperationRequiredRole().getEntityName();
				final String componentName = component.getEntityName();
				final String name = componentName + PcmComponentDiagramGenerator.OUTPORT_DELIMITER + interfaceName;
				outPortNames.put(role, UmlDiagramSupplier.escape(name));
			}
		}
		outPorts.put(component, outPortNames);
	}

	private String generateDiagramText(final Repository repository) {

		for (final RepositoryComponent component : repository.getComponents__Repository()) {
			if (component instanceof final CompositeComponent comp) {
				compositeComponents.add(comp);
				addInnerComponents(comp);
				providedRoles.put(component, comp.getProvidedRoles_InterfaceProvidingEntity());
				ECollections.sort(providedRoles.get(component), UmlDiagramSupplier.byName());
				requiredRoles.put(component, comp.getRequiredRoles_InterfaceRequiringEntity());
				ECollections.sort(requiredRoles.get(component), UmlDiagramSupplier.byName());
				createPorts(comp);
			}
		}

		for (final RepositoryComponent component : repository.getComponents__Repository()) {
			if (innerComponents.contains(component)) {
				continue;
			}
			if (component instanceof BasicComponent) {
				basicComponents.add((BasicComponent) component);
				providedRoles.put(component, component.getProvidedRoles_InterfaceProvidingEntity());
				ECollections.sort(providedRoles.get(component), UmlDiagramSupplier.byName());
				requiredRoles.put(component, component.getRequiredRoles_InterfaceRequiringEntity());
				ECollections.sort(requiredRoles.get(component), UmlDiagramSupplier.byName());
			}
		}

		compositeComponents.sort(UmlDiagramSupplier.byName());
		basicComponents.sort(UmlDiagramSupplier.byName());

		basicComponents.forEach(x -> componentNames.add(UmlDiagramSupplier.escape(x.getEntityName())));
		compositeComponents.forEach(x -> componentNames.add(UmlDiagramSupplier.escape(x.getEntityName())));
		innerComponents.forEach(x -> componentNames.add(UmlDiagramSupplier.escape(x.getEntityName())));

		for (final Interface iface : repository.getInterfaces__Repository()) {
			ifaces.add((OperationInterface) iface);
		}

		ifaces.sort(UmlDiagramSupplier.byName());

		if (compositeComponents.isEmpty() && basicComponents.isEmpty()) {
			return null;
		}

		return getComponentDiagramText();
	}

	@Override
	public String get() {
		return diagramText;
	}

	private String getComponentDiagramText() {
		final StringBuilder buffer = new StringBuilder();

		buffer.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
		buffer.append(PcmComponentDiagramGenerator.NEWLINE);

		/*
		 * Declare composite components before basic components. PlantUML nested
		 * components are only valid if they are the first reference to those
		 * components.
		 */
		for (final CompositeComponent component : compositeComponents) {
			appendComponent(component, buffer);
			buffer.append(PcmComponentDiagramGenerator.NEWLINE);
			appendProvIfaces(component, buffer);
			appendReqIfaces(component, buffer);
		}

		for (final BasicComponent component : basicComponents) {
			final boolean isNotProviding = providedRoles.get(component).isEmpty();
			final boolean isNotRequiring = requiredRoles.get(component).isEmpty();

			if (isNotProviding && isNotRequiring) {
				PcmComponentDiagramGenerator.appendComponent(component, buffer);
				buffer.append(PcmComponentDiagramGenerator.NEWLINE);
			} else {
				appendProvIfaces(component, buffer);
				appendReqIfaces(component, buffer);
			}
		}

		return buffer.toString();
	}

	private Interface getIFaceByRef(final Role role) {
		final EList<EObject> crossRefs = role.eCrossReferences();
		for (final Interface iface : ifaces) {
			if (crossRefs.contains(iface)) {
				return iface;
			}
		}
		return null;
	}

}