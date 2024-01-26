package org.palladiosimulator.view.plantuml.generator;

import java.util.Comparator;
import java.util.function.Supplier;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.pcm.core.entity.NamedElement;

public interface UmlDiagramSupplier extends Supplier<String> {

	static Comparator<NamedElement> byName() {
		return Comparator.comparing(x -> UmlDiagramSupplier.escape(x.getEntityName()));
	}

	private static String createMarkerLink(final String markerType, final String path, final String... args) {
		final StringBuilder link = new StringBuilder("marker:/").append(markerType).append(path);
		if (args.length > 0) {
			link.append("?");
			for (int i = 0; i < args.length; i += 2) {
				if (i > 0) {
					link.append("&");
				}
				link.append(args[i]).append("=").append(args[i + 1]);
			}
		}
		return link.toString();
	}

	static String escape(final Object identifier) {
		return identifier == null ? ""
		        : String.valueOf(identifier).strip().replaceAll("\\s+", ".").replaceAll("\\W+", "_");
	}

	static String getEObjectHyperlink(final EObject eObject) {
		final URI uri = EcoreUtil.getURI(eObject);
		if (uri.isPlatformResource()) {
			String path = uri.path();
			final String prefix = "/resource";
			if (path.startsWith(prefix)) {
				path = path.substring(prefix.length());
			}
			return UmlDiagramSupplier.createMarkerLink(EValidator.MARKER, path, EValidator.URI_ATTRIBUTE,
			        URI.encodeQuery(uri.toString(), false));
		}
		return uri.toString();
	}

}
