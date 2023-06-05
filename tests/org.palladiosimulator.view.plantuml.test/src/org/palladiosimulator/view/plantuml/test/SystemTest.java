package org.palladiosimulator.view.plantuml.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.view.plantuml.generator.PcmSystemDiagramGenerator;

/**
 * @author Sonya Voneva
 *
 */
class SystemTest {

    private static PcmSystemDiagramGenerator systemDiagramIntent;
    private static org.palladiosimulator.pcm.system.System system;
    private static String diagramText;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        system = (System) TestUtil.loadModel("\\resources\\ScreencastMediaStore\\MediaStore-Cacheless.system");
        systemDiagramIntent = new PcmSystemDiagramGenerator(system);
        diagramText = systemDiagramIntent.getDiagramText();
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
     * Test if a provided interface exists and is connected to the rest of the diagram
     */
    @Test
    void testProvidedRoleConnection() {
        assertTrue(diagramText.contains("()"));
        List<String> splittedText = Arrays.asList(diagramText.split("\n"));
        for (String line : splittedText) {
            if (line.contains("()")) {
                List<String> splittedLine = Arrays.asList(line.split(" "));
                assertEquals(splittedLine.size(), 2);
                String providedInterface = splittedLine.get(1);
                assertTrue(diagramText.contains(providedInterface + " - "));
            }
        }

    }

    /**
     * Test if component start is there
     */
    @Test
    void testComponentStart() {
        assertTrue(diagramText.contains("component"));
    }

    /**
     * Test number of assembly connectors.
     */
    @Test
    void testAssemblyConnectors() {
        int assemblyConnectors = 0;
        int occurrencesOfConnectorLink = TestUtil.countOccurrences(diagramText, "-(0-");
        for (Connector connector : system.getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector) {
                assemblyConnectors++;
            }
        }
        assertEquals(assemblyConnectors, occurrencesOfConnectorLink);

    }

    /**
     * Test if all contexts are there.
     */
    @Test
    void testContexts() {
        EList<AssemblyContext> contexts = system.getAssemblyContexts__ComposedStructure();
        for (AssemblyContext context : contexts) {
            assertTrue(diagramText.contains("[" + context.getEntityName() + "]"));
        }
    }

}
