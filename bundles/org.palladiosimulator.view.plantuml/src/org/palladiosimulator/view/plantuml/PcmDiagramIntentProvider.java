package org.palladiosimulator.view.plantuml;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.system.System;

import net.sourceforge.plantuml.ecore.AbstractEcoreDiagramIntentProvider;
import net.sourceforge.plantuml.util.AbstractDiagramIntent;
import net.sourceforge.plantuml.util.DiagramIntent;

public class PcmDiagramIntentProvider extends AbstractEcoreDiagramIntentProvider {

    public PcmDiagramIntentProvider() {
        super();
    }

    protected PcmDiagramIntentProvider(final Class<?> editorType) {
        super(editorType);
    }

    @Override
    protected Boolean supportsPath(final IPath path) {
        return "ecore".equals(path.getFileExtension()) || "xmi".equals(path.getFileExtension());
    }

    @Override
    protected boolean supportsEObject(final EObject object) {
        return true;
    }

    private static boolean isPcmRepositoryObject(final Object object) {
        return object instanceof Repository;
    }

    private static boolean isPcmSystemObject(final Object object) {
        return object instanceof System;
    }

    private static boolean isPcmAllocationObject(final Object object) {
        return object instanceof Allocation;
    }

    @Override
    protected Collection<? extends DiagramIntent> getDiagramInfos(final EObject eObject) {
        final Collection<AbstractDiagramIntent<?>> diagrams = new ArrayList<>();
        if (isPcmRepositoryObject(eObject)) {
            diagrams.add(new PcmComponentDiagramIntent((Repository) eObject));
        } else if (isPcmSystemObject(eObject)) {
            diagrams.add(new PcmSystemDiagramIntent((System) eObject));
        } else if (isPcmAllocationObject(eObject)) {
            diagrams.add(new PcmAllocationDiagramIntent((Allocation) eObject));
        }
        return diagrams;
    }
}
