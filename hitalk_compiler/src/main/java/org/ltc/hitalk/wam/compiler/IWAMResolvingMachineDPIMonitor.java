package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.wam.machine.IWAMResolvingMachineDPI;

/**
 *
 */
public
interface IWAMResolvingMachineDPIMonitor {
    /**
     * Accepts notification that the machine has been reset.
     *
     * @param dpi The machines DPI
     */
    void onReset ( IWAMResolvingMachineDPI dpi );

    /**
     * Accepts notification of changes to byte code loaded into the machine.
     *
     * @param dpi    The machines DPI.
     * @param start  The start offset of the changed byte code within the machines code buffer.
     * @param length The length of the changed byte code within the machines code buffer.
     */
    void onCodeUpdate ( IWAMResolvingMachineDPI dpi, int start, int length );

    /**
     * Accepts notification that the machine is starting a code execution.
     *
     * @param dpi The machines DPI
     */
    void onExecute ( IWAMResolvingMachineDPI dpi );

    /**
     * Accepts notification that the machine has been stepped by one instruction.
     *
     * @param dpi The machines DPI
     */
    void onStep ( IWAMResolvingMachineDPI dpi );
}
