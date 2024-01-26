package org.palladiosimulator.view.plantuml.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.view.plantuml.generator.PcmAllocationDiagramGenerator;

/**
 * @author Sonya Voneva
 *
 */
class AllocationTest extends AbstractPlantUmlTest {
    private static Allocation allocation;
    private static String diagramText;

    /**
     * @throws java.lang.Exception
     */
    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        allocation = loadAllocation(getNormalizedUri("ScreencastMediaStore/MediaStore-Cacheless.allocation"));
        diagramText = new PcmAllocationDiagramGenerator(allocation).get();
    }

    /**
     * Test if number of connectors is correct
     */
    @Test
    void testAssemblyConnectors() {
        int assemblyConnectors = 0;
        final int occurrencesOfConnectorLink = AbstractPlantUmlTest.countOccurrences(diagramText, "-(0-");
        for (final Connector connector : allocation.getSystem_Allocation().getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector) {
                assemblyConnectors++;
            }
        }
        assertEquals(assemblyConnectors, occurrencesOfConnectorLink);

    }

    /**
     * Test if number of containers is correct
     */
    @Test
    void testContainers() {
        final EList<AllocationContext> contexts = allocation.getAllocationContexts_Allocation();
        final Set<ResourceContainer> containers = new HashSet<>();
        final int occurrencesOfContainerKeyword = AbstractPlantUmlTest.countOccurrences(diagramText, "node");

        for (final AllocationContext context : contexts) {
            final ResourceContainer container = context.getResourceContainer_AllocationContext();
            assertNotNull(container);
            containers.add(container);
        }
        assertEquals(occurrencesOfContainerKeyword, containers.size());
    }

    /**
     * Test if starting and ending tags are there
     */
    @Test
    void testTags() {
        final List<String> splittedText = Arrays.asList(diagramText.split("\n"));
        assertTrue(
                "@startuml".equals(splittedText.get(0)) && "@enduml".equals(splittedText.get(splittedText.size() - 1)));
    }

}
