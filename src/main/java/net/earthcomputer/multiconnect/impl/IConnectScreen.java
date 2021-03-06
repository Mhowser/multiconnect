package net.earthcomputer.multiconnect.impl;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.ClientConnection;

public interface IConnectScreen {

    boolean isConnectingCancelled();

    Screen getParent();

    void multiconnect_setVersionRequestConnection(ClientConnection connection);

}
