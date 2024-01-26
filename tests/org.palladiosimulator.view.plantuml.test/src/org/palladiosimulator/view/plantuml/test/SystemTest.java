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
class SystemTest extends AbstractPlantUmlTest {
    private static String diagramText;
    private static System system;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        system = loadSystem(getNormalizedUri("ScreencastMediaStore/MediaStore-Cacheless.system"));
        diagramText = new PcmSystemDiagramGenerator(system).get();
    }

    /**
     * Test number of assembly connectors.
     */
    @Test
    void testAssemblyConnectors() {
        int assemblyConnectors = 0;
        final int occurrencesOfConnectorLink = AbstractPlantUmlTest.countOccurrences(diagramText, "-(0-");
        for (final Connector connector : system.getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector) {
                assemblyConnectors++;
            }
        }
        assertEquals(assemblyConnectors, occurrencesOfConnectorLink);

    }

    /**
     * Test if component start is there
     */
    @Test
    void testComponentStart() {
        assertTrue(diagramText.contains("component"));
    }

    /**
     * Test if all contexts are there.
     */
    @Test
    void testContexts() {
        final EList<AssemblyContext> contexts = system.getAssemblyContexts__ComposedStructure();
        for (final AssemblyContext context : contexts) {
            assertTrue(diagramText.contains("[" + context.getEntityName() + "]"));
        }
    }

    /**
     * Test if a provided interface exists and is connected to the rest of the
     * diagram
     */
    @Test
    void testProvidedRoleConnection() {
        assertTrue(diagramText.contains("()"));
        final List<String> splittedText = Arrays.asList(diagramText.split("\n"));
        for (final String line : splittedText) {
            if (line.contains("()")) {
                final List<String> splittedLine = Arrays.asList(line.split(" "));
                assertEquals(splittedLine.size(), 2);
                final String providedInterface = splittedLine.get(1);
                assertTrue(diagramText.contains(providedInterface + " - "));
            }
        }

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
