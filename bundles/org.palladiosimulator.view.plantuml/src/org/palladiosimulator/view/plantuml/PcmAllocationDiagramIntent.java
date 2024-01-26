package org.palladiosimulator.view.plantuml;

import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.view.plantuml.generator.PcmAllocationDiagramGenerator;

import net.sourceforge.plantuml.text.AbstractDiagramIntent;

public class PcmAllocationDiagramIntent extends AbstractDiagramIntent<Allocation> {

	private final PcmAllocationDiagramGenerator generator;

	public PcmAllocationDiagramIntent(final Allocation source) {
		super(source);
		generator = new PcmAllocationDiagramGenerator(getSource());
	}

	@Override
	public String getDiagramText() {
		return generator.get();
	}
}
