package org.ltc.hitalk.wam.machine;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.wam.machine.WAMCodeView;
import com.thesett.aima.logic.fol.wam.machine.WAMInternalRegisters;
import com.thesett.aima.logic.fol.wam.machine.WAMMemoryLayout;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingMachineDPIMonitor;

import java.nio.IntBuffer;

public
interface HiTalkWAMResolvingMachineDPI extends WAMCodeView {
    /**
     * Attaches a monitor to the abstract machine.
     *
     * @param monitor The machine monitor.
     */
    void attachMonitor ( WAMResolvingMachineDPIMonitor monitor );

    /**
     * Provides read access to the the machines data area.
     *
     * @return The requested portion of the machines data area.
     */
    IntBuffer getDataBuffer ();

    /**
     * Provides the internal register file and flags for the machine.
     *
     * @return The internal register file and flags for the machine.
     */
    WAMInternalRegisters getInternalRegisters ();

    /**
     * Provides the internal register set describing the memory layout of the machine.
     *
     * @return The internal register set describing the memory layout of the machine.
     */
    WAMMemoryLayout getMemoryLayout ();

    /**
     * Provides an interner for translating interned names against the underlying machine.
     *
     * @return An interner for translating interned names against the underlying machine.
     */
    VariableAndFunctorInterner getVariableAndFunctorInterner ();
}
