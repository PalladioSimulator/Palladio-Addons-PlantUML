package org.palladiosimulator.view.plantuml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import net.sourceforge.plantuml.text.AbstractDiagramIntent;

public class PcmComponentDiagramIntent extends AbstractDiagramIntent<Repository> {

    private static String NAME_START = "\"", NAME_END = "\"";
    private static String COMPONENT_START = "[", COMPONENT_END = "]";
    private static String SIMPLE_LINK = "--", REQUIRES_LINK = "..>";
    private static String REQUIRES_LABEL = " : requires";
    private static String NEWLINE = "\n";
    private static String COMPOSITE_COMPONENT_START = "component " + NAME_START;
    private static String COMPOSITE_BLOCK_START = NAME_END + " {";
    private static String COMPOSITE_BLOCK_END = "}";
    private static String INPORT_START = "portin " + NAME_START;
    private static String OUTPORT_START = "portout " + NAME_START;
    private static String INTERFACE_START = "interface ";
    private static String PORT_DELIMITER = "\\n";
    private static String PORT_END = NAME_END;
    private static String COMPOSITE_TITLE_SPACER = "\\n\\n\\n\\n\\n\\n";

    public PcmComponentDiagramIntent(Repository source) {
        super(source);
    }

    @Override
    public String getDiagramText() {
        return getDiagramText(getSource());
    }

    private final List<BasicComponent> basicComponents = new ArrayList<>();
    private final List<CompositeComponent> compositeComponents = new ArrayList<>();
    private final Set<RepositoryComponent> innerComponents = new HashSet<>();
    private final List<OperationInterface> ifaces = new ArrayList<>();
    private final Map<CompositeComponent, Map<Role, String>> inPorts = new HashMap<>();
    private final Map<CompositeComponent, Map<Role, String>> outPorts = new HashMap<>();
    private final Map<RepositoryComponent, List<ProvidedRole>> providedRoles = new HashMap<>();
    private final Map<RepositoryComponent, List<RequiredRole>> requiredRoles = new HashMap<>();
    private final Set<String> componentNames = new HashSet<>();

    protected String getDiagramText(final Repository repository) {

        for (RepositoryComponent component : repository.getComponents__Repository()) {
            if (component instanceof CompositeComponent) {
                CompositeComponent comp = (CompositeComponent) component;
                compositeComponents.add(comp);
                addInnerComponents(comp);
                providedRoles.put(component, comp.getProvidedRoles_InterfaceProvidingEntity());
                requiredRoles.put(component, comp.getRequiredRoles_InterfaceRequiringEntity());
                createPorts(comp);
            }
        }

        for (RepositoryComponent component : repository.getComponents__Repository()) {
            if (innerComponents.contains(component)) {
                continue;
            } else if (component instanceof BasicComponent) {
                basicComponents.add((BasicComponent) component);
                providedRoles.put(component, component.getProvidedRoles_InterfaceProvidingEntity());
                requiredRoles.put(component, component.getRequiredRoles_InterfaceRequiringEntity());
            }
        }

        basicComponents.forEach(x -> componentNames.add(x.getEntityName()));
        compositeComponents.forEach(x -> componentNames.add(x.getEntityName()));
        innerComponents.forEach(x -> componentNames.add(x.getEntityName()));

        for (Interface iface : repository.getInterfaces__Repository()) {
            ifaces.add((OperationInterface) iface);
        }

        if (compositeComponents.isEmpty() && basicComponents.isEmpty()) {
            return null;
        }

        return getComponentDiagramText();
    }

    private void addInnerComponents(CompositeComponent component) {
        for (AssemblyContext assemblyContext : component.getAssemblyContexts__ComposedStructure()) {
            RepositoryComponent innerComponent = assemblyContext.getEncapsulatedComponent__AssemblyContext();

            innerComponents.add(innerComponent);

            Set<ProvidedRole> connectedProvisions = component.getConnectors__ComposedStructure()
                .stream()
                .filter(AssemblyConnector.class::isInstance)
                .map(AssemblyConnector.class::cast)
                .map(x -> x.getProvidedRole_AssemblyConnector())
                .collect(Collectors.toSet());

            Set<RequiredRole> connectedRequirements = component.getConnectors__ComposedStructure()
                .stream()
                .filter(AssemblyConnector.class::isInstance)
                .map(AssemblyConnector.class::cast)
                .map(x -> x.getRequiredRole_AssemblyConnector())
                .collect(Collectors.toSet());

            List<ProvidedRole> innerProvisions = new ArrayList<>(
                    innerComponent.getProvidedRoles_InterfaceProvidingEntity());
            innerProvisions.removeAll(connectedProvisions);

            List<RequiredRole> innerRequirements = new ArrayList<>(
                    innerComponent.getRequiredRoles_InterfaceRequiringEntity());
            innerRequirements.removeAll(connectedRequirements);

            providedRoles.put(innerComponent, innerProvisions);
            requiredRoles.put(innerComponent, innerRequirements);
        }
    }

    private void createPorts(final CompositeComponent component) {
        HashMap<Role, String> inPortNames = new HashMap<>();
        for (ProvidedRole role : providedRoles.getOrDefault(component, List.of())) {
            if (role instanceof OperationProvidedRole) {
                String interfaceName = ((OperationProvidedRole) role).getProvidedInterface__OperationProvidedRole()
                    .getEntityName();
                String componentName = component.getEntityName();
                String name = interfaceName + PORT_DELIMITER + componentName;
                inPortNames.put(role, name);
            }
        }
        inPorts.put(component, inPortNames);

        HashMap<Role, String> outPortNames = new HashMap<>();
        for (RequiredRole role : requiredRoles.getOrDefault(component, List.of())) {
            if (role instanceof OperationRequiredRole) {
                String interfaceName = ((OperationRequiredRole) role).getRequiredInterface__OperationRequiredRole()
                    .getEntityName();
                String componentName = component.getEntityName();
                String name = interfaceName + PORT_DELIMITER + componentName;
                outPortNames.put(role, name);
            }
        }
        outPorts.put(component, outPortNames);
    }

    protected String getComponentDiagramText() {
        final StringBuilder buffer = new StringBuilder();

        buffer.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
        buffer.append(NEWLINE);

        // Declare composite components before basic components.
        // PlantUML nested components are only valid if they are the first reference to those
        // components.
        for (final CompositeComponent component : compositeComponents) {
            appendComponent(component, buffer);
            buffer.append(NEWLINE);
            appendProvIfaces(component, buffer);
            appendReqIfaces(component, buffer);
        }

        for (final BasicComponent component : basicComponents) {
            boolean isNotProviding = providedRoles.get(component)
                .isEmpty();
            boolean isNotRequiring = requiredRoles.get(component)
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

    protected void appendComponent(final BasicComponent component, final StringBuilder buffer) {
        buffer.append(COMPONENT_START);
        buffer.append(component.getEntityName());
        buffer.append(COMPONENT_END);

    }

    protected void appendComponent(final CompositeComponent component, final StringBuilder buffer) {
        buffer.append(COMPOSITE_COMPONENT_START);
        buffer.append(component.getEntityName());
        buffer.append(COMPOSITE_TITLE_SPACER);
        buffer.append(COMPOSITE_BLOCK_START);
        buffer.append(NEWLINE);
        component.getAssemblyContexts__ComposedStructure()
            .stream()
            .map(x -> x.getEncapsulatedComponent__AssemblyContext())
            .filter(BasicComponent.class::isInstance)
            .map(BasicComponent.class::cast)
            .forEach(x -> {
                appendComponent(x, buffer);
                buffer.append(NEWLINE);
            });
        inPorts.get(component)
            .values()
            .forEach(x -> {
                buffer.append(INPORT_START);
                buffer.append(x);
                buffer.append(PORT_END);
                buffer.append(NEWLINE);
            });
        outPorts.get(component)
            .values()
            .forEach(x -> {
                buffer.append(OUTPORT_START);
                buffer.append(x);
                buffer.append(PORT_END);
                buffer.append(NEWLINE);
            });
        component.getConnectors__ComposedStructure()
            .stream()
            .filter(AssemblyConnector.class::isInstance)
            .map(AssemblyConnector.class::cast)
            .forEach(x -> appendConnector(x, buffer));
        component.getConnectors__ComposedStructure()
            .stream()
            .filter(ProvidedDelegationConnector.class::isInstance)
            .map(ProvidedDelegationConnector.class::cast)
            .forEach(x -> appendDelegation(component, x, buffer));
        component.getConnectors__ComposedStructure()
            .stream()
            .filter(RequiredDelegationConnector.class::isInstance)
            .map(RequiredDelegationConnector.class::cast)
            .forEach(x -> appendDelegation(component, x, buffer));
        buffer.append(COMPOSITE_BLOCK_END);
    }

    protected void appendConnector(final AssemblyConnector connector, final StringBuilder buffer) {
        AssemblyContext requiringContext = connector.getRequiringAssemblyContext_AssemblyConnector();
        AssemblyContext providingContext = connector.getProvidingAssemblyContext_AssemblyConnector();

        // TODO: Currently assumes non-nested components.
        appendComponent((BasicComponent) requiringContext.getEncapsulatedComponent__AssemblyContext(), buffer);
        buffer.append(REQUIRES_LINK);
        appendComponent((BasicComponent) providingContext.getEncapsulatedComponent__AssemblyContext(), buffer);
        buffer.append(REQUIRES_LABEL);
        buffer.append(NEWLINE);
    }

    protected void appendDelegation(final CompositeComponent parent, final ProvidedDelegationConnector delegation,
            final StringBuilder buffer) {
        AssemblyContext providingContext = delegation.getAssemblyContext_ProvidedDelegationConnector();
        String portName = inPorts.get(parent)
            .get(delegation.getOuterProvidedRole_ProvidedDelegationConnector());

        // TODO: Currently assumes non-nested components.
        buffer.append(NAME_START);
        buffer.append(portName);
        buffer.append(NAME_END);
        buffer.append(REQUIRES_LINK);
        appendComponent((BasicComponent) providingContext.getEncapsulatedComponent__AssemblyContext(), buffer);
        buffer.append(REQUIRES_LABEL);
        buffer.append(NEWLINE);
    }

    protected void appendDelegation(final CompositeComponent parent, final RequiredDelegationConnector delegation,
            final StringBuilder buffer) {
        AssemblyContext requiringContext = delegation.getAssemblyContext_RequiredDelegationConnector();
        String portName = outPorts.get(parent)
            .get(delegation.getOuterRequiredRole_RequiredDelegationConnector());

        // TODO: Currently assumes non-nested components.
        appendComponent((BasicComponent) requiringContext.getEncapsulatedComponent__AssemblyContext(), buffer);
        buffer.append(REQUIRES_LINK);
        buffer.append(NAME_START);
        buffer.append(portName);
        buffer.append(NAME_END);
        buffer.append(REQUIRES_LABEL);
        buffer.append(NEWLINE);
    }

    protected void appendProvIfaces(final BasicComponent component, final StringBuilder buffer) {
        for (ProvidedRole provRole : providedRoles.get(component)) {
            String ifaceName = getIFaceByRef(provRole).getEntityName();
            // Do not draw implicit interfaces.
            if (componentNames.contains(ifaceName)) {
                continue;
            }
            buffer.append(NAME_START);
            buffer.append(INTERFACE_START);
            buffer.append(ifaceName);
            buffer.append(NAME_END);
            buffer.append(SIMPLE_LINK);
            appendComponent(component, buffer);
            buffer.append(NEWLINE);
        }
    }

    // example: [First Component] ..> HTTP : requires
    protected void appendReqIfaces(final BasicComponent component, final StringBuilder buffer) {
        for (RequiredRole reqRole : requiredRoles.get(component)) {
            String ifaceName = getIFaceByRef(reqRole).getEntityName();
            appendComponent(component, buffer);
            buffer.append(REQUIRES_LINK);
            // Refer directly to the component for implicit interfaces.
            if (componentNames.contains(ifaceName)) {
                buffer.append(COMPONENT_START);
                buffer.append(ifaceName);
                buffer.append(COMPONENT_END);
            } else {
                buffer.append(NAME_START);
                buffer.append(INTERFACE_START);
                buffer.append(ifaceName);
                buffer.append(NAME_END);
            }
            buffer.append(REQUIRES_LABEL);
            buffer.append(NEWLINE);
        }
    }

    protected void appendProvIfaces(final CompositeComponent component, final StringBuilder buffer) {
        for (ProvidedRole provRole : providedRoles.get(component)) {
            String portName = inPorts.get(component)
                .get(provRole);
            buffer.append(NAME_START);
            buffer.append(INTERFACE_START);
            buffer.append(getIFaceByRef(provRole).getEntityName());
            buffer.append(NAME_END);
            buffer.append(SIMPLE_LINK);
            buffer.append(NAME_START);
            buffer.append(portName);
            buffer.append(NAME_END);
            buffer.append(NEWLINE);
        }
    }

    // example: [First Component] ..> HTTP : requires
    protected void appendReqIfaces(final CompositeComponent component, final StringBuilder buffer) {
        for (RequiredRole reqRole : requiredRoles.get(component)) {
            String portName = outPorts.get(component)
                .get(reqRole);
            buffer.append(NAME_START);
            buffer.append(portName);
            buffer.append(NAME_END);
            buffer.append(REQUIRES_LINK);
            buffer.append(NAME_START);
            buffer.append(INTERFACE_START);
            buffer.append(getIFaceByRef(reqRole).getEntityName());
            buffer.append(NAME_END);
            buffer.append(REQUIRES_LABEL);
            buffer.append(NEWLINE);
        }
    }

    // helper method
    protected Interface getIFaceByRef(Role role) {
        EList<EObject> crossRefs = role.eCrossReferences();
        for (Interface iface : ifaces) {
            if (crossRefs.contains(iface)) {
                return iface;
            }
        }
        return null;
    }

}