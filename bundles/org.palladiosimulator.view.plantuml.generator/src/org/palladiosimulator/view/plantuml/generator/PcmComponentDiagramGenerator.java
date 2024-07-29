package org.palladiosimulator.view.plantuml.generator;

import static org.palladiosimulator.view.plantuml.generator.UmlDiagramSupplier.byName;
import static org.palladiosimulator.view.plantuml.generator.UmlDiagramSupplier.escape;

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
        buffer.append(COMPONENT_START);
        buffer.append(escape(component.getEntityName()));
        buffer.append(COMPONENT_END);
    }

    private static void appendConnector(final AssemblyConnector connector, final StringBuilder buffer) {
        final AssemblyContext requiringContext = connector.getRequiringAssemblyContext_AssemblyConnector();
        final AssemblyContext providingContext = connector.getProvidingAssemblyContext_AssemblyConnector();

        // TODO: Currently assumes non-nested components.
        PcmComponentDiagramGenerator
            .appendComponent((BasicComponent) requiringContext.getEncapsulatedComponent__AssemblyContext(), buffer);
        buffer.append(REQUIRES_LINK);
        PcmComponentDiagramGenerator
            .appendComponent((BasicComponent) providingContext.getEncapsulatedComponent__AssemblyContext(), buffer);
        buffer.append(REQUIRES_LABEL);
        buffer.append(NEWLINE);
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

            final Set<ProvidedRole> connectedProvisions = component.getConnectors__ComposedStructure()
                .stream()
                .filter(AssemblyConnector.class::isInstance)
                .map(AssemblyConnector.class::cast)
                .map(AssemblyConnector::getProvidedRole_AssemblyConnector)
                .sorted((a, b) -> escape(a.getProvidedInterface__OperationProvidedRole()
                    .getEntityName()).compareTo(escape(
                            b.getProvidedInterface__OperationProvidedRole()
                                .getEntityName())))
                .collect(Collectors.toSet());

            final Set<RequiredRole> connectedRequirements = component.getConnectors__ComposedStructure()
                .stream()
                .filter(AssemblyConnector.class::isInstance)
                .map(AssemblyConnector.class::cast)
                .map(AssemblyConnector::getRequiredRole_AssemblyConnector)
                .sorted((a, b) -> escape(a.getRequiredInterface__OperationRequiredRole()
                    .getEntityName()).compareTo(escape(
                            b.getRequiredInterface__OperationRequiredRole()
                                .getEntityName())))
                .collect(Collectors.toSet());

            final EList<ProvidedRole> innerProvisions = innerComponent.getProvidedRoles_InterfaceProvidingEntity();
            innerProvisions.removeAll(connectedProvisions);
            ECollections.sort(innerProvisions, byName());

            final EList<RequiredRole> innerRequirements = innerComponent.getRequiredRoles_InterfaceRequiringEntity();
            innerRequirements.removeAll(connectedRequirements);
            ECollections.sort(innerRequirements, byName());

            providedRoles.put(innerComponent, innerProvisions);
            requiredRoles.put(innerComponent, innerRequirements);
        }
    }

    private void appendComponent(final CompositeComponent component, final StringBuilder buffer) {
        buffer.append(COMPOSITE_COMPONENT_START);
        buffer.append(component.getEntityName());
        buffer.append(COMPOSITE_TITLE_SPACER);
        buffer.append(COMPOSITE_BLOCK_START);
        buffer.append(NEWLINE);
        component.getAssemblyContexts__ComposedStructure()
            .stream()
            .map(AssemblyContext::getEncapsulatedComponent__AssemblyContext)
            .filter(BasicComponent.class::isInstance)
            .map(BasicComponent.class::cast)
            .forEach(x -> {
                appendComponent(x, buffer);
                buffer.append(NEWLINE);
            });
        inPorts.get(component)
            .values()
            .forEach(x -> {
                buffer.append(INPORT_DECLARATION);
                buffer.append(x);
                buffer.append(NEWLINE);
            });
        outPorts.get(component)
            .values()
            .forEach(x -> {
                buffer.append(OUTPORT_DECLARATION);
                buffer.append(x);
                buffer.append(NEWLINE);
            });
        component.getConnectors__ComposedStructure()
            .stream()
            .filter(AssemblyConnector.class::isInstance)
            .map(AssemblyConnector.class::cast)
            .sorted(byName())
            .forEach(x -> appendConnector(x, buffer));
        component.getConnectors__ComposedStructure()
            .stream()
            .filter(ProvidedDelegationConnector.class::isInstance)
            .map(ProvidedDelegationConnector.class::cast)
            .sorted(byName())
            .forEach(x -> appendDelegation(component, x, buffer));
        component.getConnectors__ComposedStructure()
            .stream()
            .filter(RequiredDelegationConnector.class::isInstance)
            .map(RequiredDelegationConnector.class::cast)
            .sorted(byName())
            .forEach(x -> appendDelegation(component, x, buffer));
        buffer.append(COMPOSITE_BLOCK_END);
    }

    private void appendDelegation(final CompositeComponent parent, final ProvidedDelegationConnector delegation,
            final StringBuilder buffer) {
        final AssemblyContext providingContext = delegation.getAssemblyContext_ProvidedDelegationConnector();
        final String portName = inPorts.get(parent)
            .get(delegation.getOuterProvidedRole_ProvidedDelegationConnector());

        // TODO: Currently assumes non-nested components.
        buffer.append(NAME_START);
        buffer.append(portName);
        buffer.append(NAME_END);
        buffer.append(SIMPLE_LINK);
        PcmComponentDiagramGenerator
            .appendComponent((BasicComponent) providingContext.getEncapsulatedComponent__AssemblyContext(), buffer);
        buffer.append(NEWLINE);
    }

    protected void appendDelegation(final CompositeComponent parent, final RequiredDelegationConnector delegation,
            final StringBuilder buffer) {
        final AssemblyContext requiringContext = delegation.getAssemblyContext_RequiredDelegationConnector();
        final String portName = outPorts.get(parent)
            .get(delegation.getOuterRequiredRole_RequiredDelegationConnector());

        // TODO: Currently assumes non-nested components.
        PcmComponentDiagramGenerator
            .appendComponent((BasicComponent) requiringContext.getEncapsulatedComponent__AssemblyContext(), buffer);
        buffer.append(INTERNAL_REQUIRES_LINK);
        buffer.append(NAME_START);
        buffer.append(portName);
        buffer.append(NAME_END);
        buffer.append(NEWLINE);
    }

    private String appendIface(final Role role, final StringBuilder buffer) {
        final String ifaceName = getIFaceByRef(role).getEntityName();
        final String ifaceIdentifier = "interface." + escape(ifaceName);
        buffer.append("interface ");
        buffer.append(NAME_START);
        buffer.append(ifaceName);
        buffer.append(NAME_END);
        buffer.append(" as ");
        buffer.append(ifaceIdentifier);
        buffer.append(NEWLINE);
        return ifaceIdentifier;
    }

    private void appendProvIfaces(final BasicComponent component, final StringBuilder buffer) {
        for (final ProvidedRole provRole : providedRoles.get(component)) {
            final String ifaceName = escape(getIFaceByRef(provRole).getEntityName());
            // Do not draw implicit interfaces.
            if (componentNames.contains(ifaceName)) {
                continue;
            }

            final String ifaceIdentifier = appendIface(provRole, buffer);

            buffer.append(ifaceIdentifier);
            buffer.append(SIMPLE_LINK);
            appendComponent(component, buffer);
            buffer.append(NEWLINE);
        }
    }

    private void appendProvIfaces(final CompositeComponent component, final StringBuilder buffer) {
        for (final ProvidedRole provRole : providedRoles.get(component)) {
            final String portName = inPorts.get(component)
                .get(provRole);
            final String ifaceIdentifier = appendIface(provRole, buffer);

            buffer.append(ifaceIdentifier);
            buffer.append(SIMPLE_LINK);
            buffer.append(NAME_START);
            buffer.append(portName);
            buffer.append(NAME_END);
            buffer.append(NEWLINE);
        }
    }

    // example: [First Component] ..> HTTP : requires
    private void appendReqIfaces(final BasicComponent component, final StringBuilder buffer) {
        for (final RequiredRole reqRole : requiredRoles.get(component)) {
            // Refer directly to the component for implicit interfaces.
            final String ifaceName = escape(getIFaceByRef(reqRole).getEntityName());
            if (componentNames.contains(ifaceName)) {
                appendComponent(component, buffer);
                buffer.append(REQUIRES_LINK);
                buffer.append(COMPONENT_START);
                buffer.append(ifaceName);
                buffer.append(COMPONENT_END);
            } else {
                final String ifaceIdentifier = appendIface(reqRole, buffer);
                appendComponent(component, buffer);
                buffer.append(REQUIRES_LINK);
                buffer.append(ifaceIdentifier);
            }
            buffer.append(REQUIRES_LABEL);
            buffer.append(NEWLINE);
        }
    }

    // example: [First Component] ..> HTTP : requires
    private void appendReqIfaces(final CompositeComponent component, final StringBuilder buffer) {
        for (final RequiredRole reqRole : requiredRoles.get(component)) {
            final String portName = outPorts.get(component)
                .get(reqRole);
            final String ifaceName = escape(getIFaceByRef(reqRole).getEntityName());

            if (componentNames.contains(ifaceName)) {
                buffer.append(NAME_START);
                buffer.append(portName);
                buffer.append(NAME_END);
                buffer.append(REQUIRES_LINK);
                buffer.append(COMPONENT_START);
                buffer.append(ifaceName);
                buffer.append(COMPONENT_END);
            } else {
                final String ifaceIdentifier = appendIface(reqRole, buffer);
                buffer.append(NAME_START);
                buffer.append(portName);
                buffer.append(NAME_END);
                buffer.append(REQUIRES_LINK);
                buffer.append(ifaceIdentifier);
            }
            buffer.append(REQUIRES_LABEL);
            buffer.append(NEWLINE);
        }
    }

    private void createPorts(final CompositeComponent component) {
        final HashMap<Role, String> inPortNames = new HashMap<>();
        for (final ProvidedRole role : providedRoles.getOrDefault(component, ECollections.asEList())) {
            if (role instanceof OperationProvidedRole) {
                final String interfaceName = ((OperationProvidedRole) role)
                    .getProvidedInterface__OperationProvidedRole()
                    .getEntityName();
                final String componentName = component.getEntityName();
                final String name = componentName + INPORT_DELIMITER + interfaceName;
                inPortNames.put(role, escape(name));
            }
        }
        inPorts.put(component, inPortNames);

        final HashMap<Role, String> outPortNames = new HashMap<>();
        for (final RequiredRole role : requiredRoles.getOrDefault(component, ECollections.asEList())) {
            if (role instanceof OperationRequiredRole) {
                final String interfaceName = ((OperationRequiredRole) role)
                    .getRequiredInterface__OperationRequiredRole()
                    .getEntityName();
                final String componentName = component.getEntityName();
                final String name = componentName + OUTPORT_DELIMITER + interfaceName;
                outPortNames.put(role, escape(name));
            }
        }
        outPorts.put(component, outPortNames);
    }

    private String generateDiagramText(final Repository repository) {
        // Find inner components
        for (final RepositoryComponent component : repository.getComponents__Repository()) {
            if (component instanceof final CompositeComponent comp) {
                for (final AssemblyContext assemblyContext : comp.getAssemblyContexts__ComposedStructure()) {
                    final RepositoryComponent innerComponent = assemblyContext
                        .getEncapsulatedComponent__AssemblyContext();
                    innerComponents.add(innerComponent);
                }
            }
        }

        // Detect free components
        for (final RepositoryComponent component : repository.getComponents__Repository()) {
            if (innerComponents.contains(component)) {
                continue;
            }
            if (component instanceof BasicComponent) {
                basicComponents.add((BasicComponent) component);
                providedRoles.put(component, component.getProvidedRoles_InterfaceProvidingEntity());
                ECollections.sort(providedRoles.get(component), byName());
                requiredRoles.put(component, component.getRequiredRoles_InterfaceRequiringEntity());
                ECollections.sort(requiredRoles.get(component), byName());
            }
        }

        // Detect composite components and their inner components
        for (final RepositoryComponent component : repository.getComponents__Repository()) {
            if (component instanceof final CompositeComponent comp) {
                compositeComponents.add(comp);
                addInnerComponents(comp);
                providedRoles.put(component, comp.getProvidedRoles_InterfaceProvidingEntity());
                ECollections.sort(providedRoles.get(component), byName());
                requiredRoles.put(component, comp.getRequiredRoles_InterfaceRequiringEntity());
                ECollections.sort(requiredRoles.get(component), byName());
                createPorts(comp);
            }
        }

        compositeComponents.sort(byName());
        basicComponents.sort(byName());

        basicComponents.forEach(x -> componentNames.add(escape(x.getEntityName())));
        compositeComponents.forEach(x -> componentNames.add(escape(x.getEntityName())));
        innerComponents.forEach(x -> componentNames.add(escape(x.getEntityName())));

        for (final Interface iface : repository.getInterfaces__Repository()) {
            ifaces.add((OperationInterface) iface);
        }

        ifaces.sort(byName());

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
        buffer.append(NEWLINE);
        buffer.append("skinparam componentStyle uml2"); // UML2 Style
        buffer.append(NEWLINE);

        /*
         * Declare composite components before basic components. PlantUML nested components are only
         * valid if they are the first reference to those components.
         */
        for (final CompositeComponent component : compositeComponents) {
            appendComponent(component, buffer);
            buffer.append(NEWLINE);
            appendProvIfaces(component, buffer);
            appendReqIfaces(component, buffer);
        }

        for (final BasicComponent component : basicComponents) {
            final boolean isNotProviding = providedRoles.get(component)
                .isEmpty();
            final boolean isNotRequiring = requiredRoles.get(component)
                .isEmpty();

            if (isNotProviding && isNotRequiring) {
                appendComponent(component, buffer);
                buffer.append(NEWLINE);
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