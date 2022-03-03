package org.palladiosimulator.view.plantuml;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.system.System;

import net.sourceforge.plantuml.ecore.AbstractEcoreDiagramIntentProvider;
import net.sourceforge.plantuml.ecore.EcoreDiagramHelper;
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

	protected final EcoreDiagramHelper diagramHelper = new EcoreDiagramHelper();

	protected EPackage getEPackage(final EObject selection) {
		return diagramHelper.getAncestor(selection, EPackage.class);
	}

	public static boolean isEcoreClassDiagramObject(final Object object) {
		return object instanceof EModelElement;
	}

	private static boolean isPCMRepositoryObject(final Object object) {
		return object instanceof Repository;
	}

	private static boolean isPCMSystemObject(final Object object) {
		return object instanceof System;
	}

	private static boolean isPCMAllocationObject(final Object object) {
		return object instanceof Allocation;
	}

	@Override
	protected Collection<? extends DiagramIntent> getDiagramInfos(final EObject eObject) {
		final Collection<AbstractDiagramIntent<?>> diagrams = new ArrayList<>();
//		final boolean isEcoreClassDiagram = isEcoreClassDiagramObject(eObject);
//		final EPackage pack = getEPackage(isEcoreClassDiagram ? eObject : eObject.eClass());
//		if (!isEcoreClassDiagram) {
//			diagrams.add(new EcoreObjectDiagramIntent(eObject));
//		}
//		if (pack != null) {
//			final EcoreClassDiagramIntent classDiagramIntent = new EcoreClassDiagramIntent(
//					Collections.singletonList(pack));
//			if (!isEcoreClassDiagram) {
//				classDiagramIntent.setPriority(-1);
//			}
//			diagrams.add(classDiagramIntent);
//		}
		if (isPCMRepositoryObject(eObject)) {
			diagrams.add(new PcmComponentDiagramIntent((Repository) eObject));
		} else if (isPCMSystemObject(eObject)) {
			diagrams.add(new PcmSystemDiagramIntent((System) eObject));
		} else if (isPCMAllocationObject(eObject)) {
			diagrams.add(new PcmAllocationDiagramIntent((Allocation) eObject));
		}
		return diagrams;
	}
}
