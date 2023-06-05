package org.palladiosimulator.view.plantuml.test;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.palladiosimulator.pcm.PcmPackage;

/**
 * The Class TestUtil.
 */

public class TestUtil {

    /**
     * Load model from path
     * 
     * @param filePath
     *            the file path
     * @return the e object - casted at use
     */
    public static EObject loadModel(final String filePath) {
        PcmPackage.eINSTANCE.eClass();

        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        URI filePathUri = org.eclipse.emf.common.util.URI.createFileURI(filePath);

        Resource resource = resourceSet.getResource(filePathUri, true);

        return resource.getContents()
            .get(0);

    }

    /**
     * Count occurrences.
     *
     * @param text
     *            the text
     * @param part
     *            the searched string
     * @return the number
     */
    public static int countOccurrences(String text, String part) {
        int counter = 0;
        List<String> splittedText = Arrays.asList(text.split("\n"));
        for (String line : splittedText) {
            if (line.contains(part)) {
                counter++;
            }
        }
        return counter;
    }
}
