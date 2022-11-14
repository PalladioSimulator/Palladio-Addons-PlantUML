package org.palladiosimulator.view.plantuml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.Interface;
import org.palladiosimulator.pcm.repository.OperationInterface;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Role;

import net.sourceforge.plantuml.text.AbstractDiagramIntent;

public class PcmComponentDiagramIntent extends AbstractDiagramIntent<Repository> {

    private static String COMPONENT_START = "[", COMPONENT_END = "]";
    private static String SIMPLE_LINK = "-", REQUIRES_LINK = "..>";
    private static String REQUIRES_LABEL = " : requires";
    private static String NEWLINE = "\n";

    public PcmComponentDiagramIntent(Repository source) {
        super(source);
    }

    @Override
    public String getDiagramText() {
        return getDiagramText(getSource());
    }

    private final List<RepositoryComponent> components = new ArrayList<>();
    private final List<OperationInterface> ifaces = new ArrayList<>();
    private final Map<RepositoryComponent, EList<ProvidedRole>> providedRoles = new HashMap<>();
    private final Map<RepositoryComponent, EList<RequiredRole>> requiredRoles = new HashMap<>();

    protected String getDiagramText(final Repository repository) {
        for (RepositoryComponent component : repository.getComponents__Repository()) {
            components.add(component);
            providedRoles.put(component, component.getProvidedRoles_InterfaceProvidingEntity());
            requiredRoles.put(component, component.getRequiredRoles_InterfaceRequiringEntity());
        }
        for (Interface iface : repository.getInterfaces__Repository()) {
            ifaces.add((OperationInterface) iface);
        }
        final String result = components.size() > 0 ? getComponentDiagramText() : null;
        return result;
    }

    protected String getComponentDiagramText() {
        final StringBuilder buffer = new StringBuilder();

        buffer.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
        buffer.append(NEWLINE);

        for (final RepositoryComponent component : components) {
            if (providedRoles.get(component)
                .size() > 0
                    || requiredRoles.get(component)
                        .size() > 0) {
                appendProvIfaces((BasicComponent) component, buffer);
                appendReqIfaces((BasicComponent) component, buffer);
            } else {
                appendComponent((BasicComponent) component, buffer);
                buffer.append(NEWLINE);
            }
        }
        return buffer.toString();
    }

    protected void appendComponent(final BasicComponent component, final StringBuilder buffer) {
        buffer.append(COMPONENT_START);
        buffer.append(component.getEntityName());
        buffer.append(COMPONENT_END);

    }

    protected void appendProvIfaces(final BasicComponent component, final StringBuilder buffer) {
        for (ProvidedRole provRole : providedRoles.get(component)) {
            buffer.append(getIFaceByRef(provRole).getEntityName());
            buffer.append(SIMPLE_LINK);
            appendComponent(component, buffer);
            buffer.append(NEWLINE);
        }
    }

    // example: [First Component] ..> HTTP : requires
    protected void appendReqIfaces(final BasicComponent component, final StringBuilder buffer) {
        for (RequiredRole reqRole : requiredRoles.get(component)) {
            appendComponent(component, buffer);
            buffer.append(REQUIRES_LINK);
            buffer.append(getIFaceByRef(reqRole).getEntityName());
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