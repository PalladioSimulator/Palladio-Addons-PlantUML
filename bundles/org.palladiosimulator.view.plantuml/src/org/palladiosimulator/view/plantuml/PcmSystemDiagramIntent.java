package org.palladiosimulator.view.plantuml;

import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.view.plantuml.generator.PcmSystemDiagramGenerator;

import net.sourceforge.plantuml.text.AbstractDiagramIntent;

public class PcmSystemDiagramIntent extends AbstractDiagramIntent<System> {

    private PcmSystemDiagramGenerator generator;

    public PcmSystemDiagramIntent(System source) {
        super(source);
        generator = new PcmSystemDiagramGenerator(getSource());
    }

    @Override
    public String getDiagramText() {
        return generator.getDiagramText();
    }
}
