package org.palladiosimulator.view.plantuml.test;

import static org.junit.jupiter.api.Assertions.*;

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
import org.palladiosimulator.view.plantuml.PcmAllocationDiagramIntent;

/**
 * @author Sonya Voneva
 *
 */
class AllocationTest {

    private static PcmAllocationDiagramIntent allocationDiagramIntent;
    private static Allocation allocation;
    private static String diagramText;

    /**
     * @throws java.lang.Exception
     */
    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        allocation = (Allocation) TestUtil
            .loadModel("\\resources\\ScreencastMediaStore\\MediaStore-Cacheless.allocation");
        allocationDiagramIntent = new PcmAllocationDiagramIntent(allocation);
        diagramText = allocationDiagramIntent.getDiagramText();
    }

    /**
     * Test if starting and ending tags are there
     */
    @Test
    void testTags() {
        List<String> splittedText = Arrays.asList(diagramText.split("\n"));
        assertTrue(splittedText.get(0)
            .equals("@startuml")
                && splittedText.get(splittedText.size() - 1)
                    .equals("@enduml"));
    }

    /**
     * Test if number of containers is correct
     */
    @Test
    void testContainers() {
        EList<AllocationContext> contexts = allocation.getAllocationContexts_Allocation();
        Set<ResourceContainer> containers = new HashSet<>();
        int occurrencesOfContainerKeyword = TestUtil.countOccurrences(diagramText, "node");

        for (AllocationContext context : contexts) {
            ResourceContainer container = context.getResourceContainer_AllocationContext();
            assertNotNull(container);
            containers.add(container);
        }
        assertEquals(occurrencesOfContainerKeyword, containers.size());
    }

    /**
     * Test if number of connectors is correct
     */
    @Test
    void testAssemblyConnectors() {
        int assemblyConnectors = 0;
        int occurrencesOfConnectorLink = TestUtil.countOccurrences(diagramText, "-(0-");
        for (Connector connector : allocation.getSystem_Allocation()
            .getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector) {
                assemblyConnectors++;
            }
        }
        assertEquals(assemblyConnectors, occurrencesOfConnectorLink);

    }

}
