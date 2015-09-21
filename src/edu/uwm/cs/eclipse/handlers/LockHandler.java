package edu.uwm.cs.eclipse.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import edu.uwm.cs.junit.Util;


public class LockHandler extends MyHandler {

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		Shell shell = window.getShell();
		IWorkbenchPage page = window.getActivePage();
		IEditorPart ed = page.getActiveEditor();
		if (!(ed instanceof ITextEditor)) {
		  return warningMessage(shell,"Current window does not eappar to be text.");
		}
		ITextEditor editor = (ITextEditor)ed;
		ISelection sel = editor.getSelectionProvider().getSelection();
    if (sel.isEmpty()) {
      return errorMessage(shell,"Need expression to lock");
    }
		if (!(sel instanceof ITextSelection)) {
		  return errorMessage(shell,"Current selection does not appear to be text.");
		}
		ITextSelection selection = (ITextSelection)sel;
		Object obj;
		try {
		  obj = Util.parseObject(selection.getText());
		} catch (RuntimeException ex) {
		  return errorMessage(shell,"Could not parse literal: " + ex.getLocalizedMessage());
		}
		String type = "T";
		if (obj instanceof Integer) {
		  type = "Ti";
		} else if (obj instanceof Boolean) {
		  type = "Tb";
		} else if (obj instanceof String) {
		  type = "Ts";
		} else if (obj instanceof Character) {
		  type = "Tc";
		}
		int hash = Util.hash(obj);
    String locked = type + "(" + hash + ")";
		
		IDocumentProvider provider = editor.getDocumentProvider();
    IDocument document = provider.getDocument(editor.getEditorInput());
    try {
      document.replace(selection.getOffset(), selection.getLength(), locked);
      return infoMessage(shell,"Substituted with " + locked);
    } catch (BadLocationException e) {
      return errorMessage(shell,"Internal error: couldn't replace selection with " + locked);
    }
		// return null;
	}
  
}
