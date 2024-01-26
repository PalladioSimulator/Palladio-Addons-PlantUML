package org.palladiosimulator.view.plantuml.test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.palladiosimulator.pcm.PcmPackage;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourcetype.ResourceRepository;
import org.palladiosimulator.pcm.resourcetype.ResourcetypePackage;
import org.palladiosimulator.pcm.system.System;

import de.uka.ipd.sdq.stoex.StoexPackage;

/**
 * The abstract class for all PlantUML tests.
 */
abstract class AbstractPlantUmlTest {

    /**
     * Count occurrences.
     *
     * @param text the text
     * @param part the searched string
     * @return the number
     */
    static int countOccurrences(final String text, final String part) {
        int counter = 0;
        final List<String> splittedText = Arrays.asList(text.split("\n"));
        for (final String line : splittedText) {
            if (line.contains(part)) {
                counter++;
            }
        }
        return counter;
    }

    protected static String getNormalizedUri(final String modelId) {
        return Paths.get("resources/" + Objects.requireNonNull(modelId)).normalize().toAbsolutePath().toUri()
                .toString();
    }

    private static EObject load(final String uri, final String extension) {
        if ((uri == null) || uri.isBlank() || (extension == null) || extension.isBlank()) {
            throw new IllegalArgumentException(
                    "The elements of the URI where the model will be loaded with the specified extension must not be blank.");
        }

        // Register the XMI resource
        ResourcetypePackage.eINSTANCE.eClass();
        final Resource.Factory.Registry registry = Resource.Factory.Registry.INSTANCE;
        for (final String fe : new String[] { extension.strip(), "xmi" }) {
            registry.getExtensionToFactoryMap().put(fe, new XMIResourceFactoryImpl());
        }

        // Get the resource
        final Resource resource = new ResourceSetImpl().getResource(URI.createURI(uri.strip()), true);

        // Get the first model element
        return resource.getContents().get(0);
    }

    /**
     * Loads an Allocation form the specified path.
     *
     * @param uri
     * @return the org.palladiosimulator.generator.fluent.allocation
     * @see org.palladiosimulator.pcm.allocation.Allocation
     */
    static Allocation loadAllocation(final String uri) {
        return (Allocation) load(uri, "allocation");
    }

    /**
     * Loads a repository from the specified path.
     *
     * @param uri
     * @return the repository
     * @see org.palladiosimulator.pcm.repository.Repository
     */
    static Repository loadRepository(final String uri) {
        return (Repository) load(uri, "repository");
    }

    /**
     * Loads a ResourceEnvironment from the specified path.
     *
     * @param uri
     * @return the resource environment
     * @see org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment
     */
    static ResourceEnvironment loadResourceEnvironment(final String uri) {
        return (ResourceEnvironment) load(uri, "resourceenvironment");
    }

    /**
     * Loads a ResourceRepository from the specified path.
     *
     * @param uri
     * @return the resource repository
     * @see org.palladiosimulator.pcm.resourcetype.ResourceRepository
     */
    static ResourceRepository loadResourceTypeRepository(final String uri) {
        return (ResourceRepository) load(uri, "resourcetype");
    }

    /**
     * Loads a System from the specified path.
     *
     * @param uri
     * @return the org.palladiosimulator.generator.fluent.system
     * @see org.palladiosimulator.pcm.system.System
     */
    static System loadSystem(final String uri) {
        return (System) load(uri, "system");
    }

    protected final Repository failures;

    protected final Repository primitives;

    protected final ResourceRepository resourceTypes;

    AbstractPlantUmlTest() {
        // Processing of all registered extensions
        EcorePlugin.ExtensionProcessor.process(null);
        PcmPackage.eINSTANCE.eClass();
        StoexPackage.eINSTANCE.eClass();
        primitives = loadRepository("pathmap://PCM_MODELS/PrimitiveTypes.repository");
        resourceTypes = loadResourceTypeRepository("pathmap://PCM_MODELS/Palladio.resourcetype");
        failures = loadRepository("pathmap://PCM_MODELS/FailureTypes.repository");
    }
}
