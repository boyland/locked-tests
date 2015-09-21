package edu.uwm.cs.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public abstract class MyHandler extends AbstractHandler {

  private static final String SUBSYSTEM_NAME = "Locked Tests";

  public MyHandler() {
    super();
  }

  protected Void infoMessage(Shell shell, String message) {
    MessageDialog.openInformation(shell, SUBSYSTEM_NAME, message);
    return null;
  }

  protected Void errorMessage(Shell shell, String message) {
    MessageDialog.openError(shell, SUBSYSTEM_NAME, message);
    return null;
  }

  protected Void warningMessage(Shell shell, String message) {
    MessageDialog.openError(shell, SUBSYSTEM_NAME, message);
    return null;
  }

}