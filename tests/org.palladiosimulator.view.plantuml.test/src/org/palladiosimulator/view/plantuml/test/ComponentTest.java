package org.palladiosimulator.view.plantuml.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.view.plantuml.generator.PcmComponentDiagramGenerator;

/**
 * @author Sonya Voneva
 *
 */
class ComponentTest extends AbstractPlantUmlTest {

    private static String diagramText;
    private static Repository repository;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        repository = loadRepository(getNormalizedUri("ScreencastMediaStore/MediaStore.repository"));
        diagramText = new PcmComponentDiagramGenerator(repository).get();
    }

    /**
     * Test if all components are in the diagram text
     */
    @Test
    void testComponents() {
        final EList<RepositoryComponent> components = repository.getComponents__Repository();
        for (final RepositoryComponent component : components) {
            assertTrue(diagramText.contains("[" + component.getEntityName() + "]"));
        }
    }

    /**
     * Test if all providing roles are in the diagram text
     */
    @Test
    void testProvidingRoles() {
        final EList<RepositoryComponent> components = repository.getComponents__Repository();
        final EList<ProvidedRole> provRoles = new BasicEList<>();
        for (final RepositoryComponent component : components) {
            provRoles.addAll(component.getProvidedRoles_InterfaceProvidingEntity());
        }
        final int occurrencesInText = AbstractPlantUmlTest.countOccurrences(diagramText, "-");
        assertEquals(occurrencesInText, provRoles.size());

    }

    /**
     * Test if the Repository was loaded an the diagram text was generated
     */
    @Test
    void testRepository() {
        assertNotNull(repository);
        assertNotNull(diagramText);
    }

    /**
     * Test if all requiring roles are in the diagram text
     */
    @Test
    void testRequiringRoles() {
        final EList<RepositoryComponent> components = repository.getComponents__Repository();
        final EList<RequiredRole> reqRoles = new BasicEList<>();
        for (final RepositoryComponent component : components) {
            reqRoles.addAll(component.getRequiredRoles_InterfaceRequiringEntity());
        }
        final int occurrencesInText = AbstractPlantUmlTest.countOccurrences(diagramText, "requires");
        assertEquals(occurrencesInText, reqRoles.size());

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
