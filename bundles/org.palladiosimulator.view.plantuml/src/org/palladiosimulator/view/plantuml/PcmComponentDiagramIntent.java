package org.palladiosimulator.view.plantuml;

import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.view.plantuml.generator.PcmComponentDiagramGenerator;

import net.sourceforge.plantuml.text.AbstractDiagramIntent;

public class PcmComponentDiagramIntent extends AbstractDiagramIntent<Repository> {

    private final PcmComponentDiagramGenerator generator;

    public PcmComponentDiagramIntent(Repository source) {
        super(source);
        generator = new PcmComponentDiagramGenerator(getSource());
    }

    @Override
    public String getDiagramText() {
        return generator.getDiagramText();
    }
}