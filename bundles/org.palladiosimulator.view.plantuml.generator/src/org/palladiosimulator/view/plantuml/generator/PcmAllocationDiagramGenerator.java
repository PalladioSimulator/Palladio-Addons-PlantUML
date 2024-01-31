package org.palladiosimulator.view.plantuml.generator;

import static org.palladiosimulator.view.plantuml.generator.UmlDiagramSupplier.byName;
import static org.palladiosimulator.view.plantuml.generator.UmlDiagramSupplier.escape;
import static org.palladiosimulator.view.plantuml.generator.UmlDiagramSupplier.getEObjectHyperlink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.impl.AssemblyConnectorImpl;
import org.palladiosimulator.pcm.core.entity.NamedElement;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.CompositeComponent;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.system.System;

public class PcmAllocationDiagramGenerator implements UmlDiagramSupplier {

	private static final String COMPONENT_KEYWORD = "component";
	private static final String COMPONENT_START = "[", COMPONENT_END = "]";
	private static final String CONTAINER_KEYWORD = "node";
	private static final String CURLY_OPENING_BRACKET = "{", CURLY_CLOSING_BRACKET = "}";
	private static final String LINK_START = "[[", LINK_END = "]]";
	private static final String NEWLINE = "\n";
	private static final String PROVIDES_REQUIRES_LINK = " - ";
	private static final String SPACE = " ";

	private final Map<RepositoryComponent, RepositoryComponent> assembly;
	private final List<BasicComponent> basicComponents;
	private final StringBuilder buffer;
	private final List<CompositeComponent> compositeComponents;
	private final String diagramText;
	private final String linkToSystem;

	public PcmAllocationDiagramGenerator(final Allocation allocation) {
		buffer = new StringBuilder();

		final List<AllocationContext> contexts = allocation.getAllocationContexts_Allocation().stream()
		        .filter(context -> context != null)
		        .filter(context -> context.getResourceContainer_AllocationContext() != null)
		        .filter(context -> context.getAssemblyContext_AllocationContext() != null).toList();
		contexts.get(0).getAllocation_AllocationContext().getSystem_Allocation()
		        .getAssemblyContexts__ComposedStructure();

		assembly = new HashMap<>();
		contexts.stream().map(AllocationContext::getAllocation_AllocationContext).filter(a -> a != null)
		        .map(Allocation::getSystem_Allocation).filter(s -> s != null).distinct()
		        .map(System::getConnectors__ComposedStructure).filter(c -> c != null).flatMap(List::stream)
		        .filter(c -> c != null).distinct().filter(AssemblyConnectorImpl.class::isInstance)
		        .map(AssemblyConnectorImpl.class::cast).distinct().sorted(byName()).forEachOrdered(c -> {
			        final AssemblyContext providingAssembly = c.getProvidingAssemblyContext_AssemblyConnector();
			        final AssemblyContext requiringAssembly = c.getRequiringAssemblyContext_AssemblyConnector();
			        if ((providingAssembly != null) && (requiringAssembly != null)) {
				        final RepositoryComponent providingComponent = providingAssembly
				                .getEncapsulatedComponent__AssemblyContext();
				        final RepositoryComponent requiringComponent = requiringAssembly
				                .getEncapsulatedComponent__AssemblyContext();
				        if ((providingComponent != null) && (requiringComponent != null)) {
					        assembly.put(providingComponent, requiringComponent);
				        }
			        }
		        });

		assembly.keySet().stream().forEach(k -> java.lang.System.out
		        .println(escape(k.getEntityName()) + " : " + escape(assembly.get(k).getEntityName())));

		final List<RepositoryComponent> components = contexts.stream()
		        .map(AllocationContext::getAssemblyContext_AllocationContext)
		        .map(AssemblyContext::getEncapsulatedComponent__AssemblyContext).toList();

		basicComponents = components.stream().filter(BasicComponent.class::isInstance).map(BasicComponent.class::cast)
		        .distinct().sorted(byName()).collect(Collectors.toCollection(ArrayList::new));

		compositeComponents = components.stream().filter(CompositeComponent.class::isInstance)
		        .map(CompositeComponent.class::cast).distinct().sorted(byName()).toList();

		linkToSystem = getEObjectHyperlink(allocation.getSystem_Allocation());

		diagramText = contexts.isEmpty() ? "" : getAllocationDiagramText();
	}

	// example: [Access Control] - [Web Server]
	private void appendAssemblyConnector(final Entry<String, String> assembly) {
		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_START);
		buffer.append(assembly.getKey());
		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_END);

		buffer.append(PcmAllocationDiagramGenerator.PROVIDES_REQUIRES_LINK);

		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_START);
		buffer.append(assembly.getValue());
		buffer.append(PcmAllocationDiagramGenerator.COMPONENT_END);
		buffer.append(PcmAllocationDiagramGenerator.NEWLINE);
	}

	// example: [DataAccess]
	private void appendComponent(final BasicComponent component) {
		buffer.append(COMPONENT_START);
		buffer.append(escape(component.getEntityName()));
		buffer.append(COMPONENT_END);
		buffer.append(SPACE);
		buffer.append(LINK_START);
		buffer.append(linkToSystem);
		buffer.append(LINK_END);
		buffer.append(NEWLINE);
	}

	// example: component System1 {
	private void appendComponentStart(final CompositeComponent component) {
		buffer.append(COMPONENT_KEYWORD);
		buffer.append(SPACE);
		buffer.append(escape(component.getEntityName()));
		buffer.append(SPACE);
		buffer.append(CURLY_OPENING_BRACKET);
		buffer.append(NEWLINE);
	}

	// example: node System1 {
	private void appendContainerStart(final String containerName) {
		buffer.append(CONTAINER_KEYWORD);
		buffer.append(SPACE);
		buffer.append(escape(containerName));
		buffer.append(SPACE);
		buffer.append(CURLY_OPENING_BRACKET);
		buffer.append(NEWLINE);
	}

	private void appendEnd() {
		buffer.append(CURLY_CLOSING_BRACKET);
		buffer.append(NEWLINE);
	}

	private boolean contains(final CompositeComponent composite, final BasicComponent basic) {
		return composite.getAssemblyContexts__ComposedStructure().stream()
		        .map(AssemblyContext::getEncapsulatedComponent__AssemblyContext)
		        .anyMatch(c -> Objects.equals(c, basic));
	}

	@Override
	public String get() {
		return diagramText;
	}

	private String getAllocationDiagramText() {
		buffer.append("skinparam fixCircleLabelOverlapping true"); // avoid overlapping of labels
		buffer.append(NEWLINE);
		buffer.append("skinparam componentStyle uml2"); // UML2 Style
		buffer.append(NEWLINE);

		final Map<RepositoryComponent, String> map = new HashMap<>();

		for (final CompositeComponent composite : compositeComponents) {
			final String container = getContainerName(composite);
			appendContainerStart(container);
			appendComponentStart(composite);
			final List<BasicComponent> done = new ArrayList<>();
			for (final BasicComponent basic : basicComponents) {
				if (contains(composite, basic)) {
					appendComponent(basic);
					map.put(basic, container);
					done.add(basic);
				}
			}
			basicComponents.removeAll(done);
			appendEnd();
			appendEnd();
		}

		for (final BasicComponent component : basicComponents) {
			final String container = getContainerName(component);
			appendContainerStart(container);
			appendComponent(component);
			map.put(component, container);
			appendEnd();
		}

		final Set<Entry<String, String>> set = new HashSet<>();

		for (final RepositoryComponent component : assembly.keySet()) {
			final String from = map.get(component);
			final String to = map.get(assembly.get(component));
			if ((from == null) || (to == null) || from.equals(to)) {
				continue;
			}
			set.add(Map.entry(from, to));
		}

		set.stream().distinct()
		        .sorted((o1, o2) -> (o1.getKey() + o1.getValue()).compareTo((o2.getKey() + o2.getValue())))
		        .forEachOrdered(this::appendAssemblyConnector);

		return buffer.toString();
	}

	private String getContainerName(final NamedElement entity) {
		return CONTAINER_KEYWORD + escape(entity.getEntityName());
	}

}
