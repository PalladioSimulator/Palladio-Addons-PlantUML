package org.palladiosimulator.view.plantuml.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.palladiosimulator.view.plantuml.PcmComponentDiagramIntent;

/**
 * @author Sonya Voneva
 *
 */
class ComponentTest {

    private static PcmComponentDiagramIntent componentDiagramIntent;

    private static Repository repository;
    private static String diagramText;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        repository = (Repository) TestUtil.loadModel("\\resources\\ScreencastMediaStore\\MediaStore.repository");
        componentDiagramIntent = new PcmComponentDiagramIntent(repository);
        diagramText = componentDiagramIntent.getDiagramText();
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
     * Test if all providing roles are in the diagram text
     */
    @Test
    void testProvidingRoles() {
        EList<RepositoryComponent> components = repository.getComponents__Repository();
        EList<ProvidedRole> provRoles = new BasicEList<>();
        for (RepositoryComponent component : components) {
            provRoles.addAll(component.getProvidedRoles_InterfaceProvidingEntity());
        }
        int occurrencesInText = TestUtil.countOccurrences(diagramText, "-");
        assertEquals(occurrencesInText, provRoles.size());

    }

    /**
     * Test if all requiring roles are in the diagram text
     */
    @Test
    void testRequiringRoles() {
        EList<RepositoryComponent> components = repository.getComponents__Repository();
        EList<RequiredRole> reqRoles = new BasicEList<>();
        for (RepositoryComponent component : components) {
            reqRoles.addAll(component.getRequiredRoles_InterfaceRequiringEntity());
        }
        int occurrencesInText = TestUtil.countOccurrences(diagramText, "requires");
        assertEquals(occurrencesInText, reqRoles.size());

    }

    /**
     * Test if all components are in the diagram text
     */
    @Test
    void testComponents() {
        EList<RepositoryComponent> components = repository.getComponents__Repository();
        for (RepositoryComponent component : components) {
            assertTrue(diagramText.contains("[" + component.getEntityName() + "]"));
        }
    }

}
