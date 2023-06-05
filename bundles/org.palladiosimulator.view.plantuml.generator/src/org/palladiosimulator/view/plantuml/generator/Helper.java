package org.palladiosimulator.view.plantuml.generator;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * Provides select functions from net.sourceforge.plantuml.ecore.
 */
public final class Helper {
    private Helper() {
        throw new IllegalStateException();
    }

    public static String getEObjectHyperlink(final EObject eObject) {
        final URI uri = EcoreUtil.getURI(eObject);
        if (uri.isPlatformResource()) {
            String path = uri.path();
            final String prefix = "/resource";
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
            }
            return createMarkerLink(EValidator.MARKER, path, EValidator.URI_ATTRIBUTE,
                    URI.encodeQuery(uri.toString(), false));
        }
        return uri.toString();
    }

    private static String createMarkerLink(String markerType, String path, String... args) {
        String link = "marker:/" + markerType + path;
        if (args.length > 0) {
            link = link + "?";
            for (int i = 0; i < args.length; i += 2) {
                if (i > 0) {
                    link = link + "&";
                }
                link = link + args[i] + "=" + args[i + 1];
            }
        }
        return link;
    }
}
