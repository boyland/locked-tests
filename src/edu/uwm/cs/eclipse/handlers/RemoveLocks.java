package edu.uwm.cs.eclipse.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
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

public class RemoveLocks extends MyHandler {


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
			return warningMessage(shell,"Current window does not appear to be text.");
		}
		ITextEditor editor = (ITextEditor)ed;
		IFile file = (IFile)ed.getEditorInput().getAdapter(IFile.class);
		if (file == null) {
			return warningMessage(shell,"Current selection doesn't appear to be in a source file.");
		}
		IFile testFile = null;
		String fileName = file.getName().replace(".java","");
		IContainer c = file.getParent();
		while (c != null) {
			IFolder folder = (IFolder)c.getAdapter(IFolder.class);
			if (folder == null) break;
			testFile = folder.getProject().getFile(fileName+".tst");
			if (testFile.exists()) break;
			fileName = folder.getName() + "." + fileName;
			c = folder.getParent();
		}
		if (testFile == null || !testFile.exists()) {
			return errorMessage(shell, "Cannot find lock data file.");
		}
		ISelection sel = editor.getSelectionProvider().getSelection();
		if (sel.isEmpty()) {
			return errorMessage(shell,"Need expression to lock");
		}
		if (!(sel instanceof ITextSelection)) {
			return errorMessage(shell,"Current selection does not appear to be text.");
		}
		ITextSelection selection = (ITextSelection)sel;
		String text = selection.getText();
		if (!text.startsWith("T") || !text.endsWith(")")) {
			return errorMessage(shell,"Current selection doesn't appear to be a locked value: " + text);
		}
		int p = text.indexOf('(');
		if (p < 0) {
			return errorMessage(shell,"Current selection doesn't appear to be a locked value: " + text);
		}
		String code = text.substring(p+1, text.length()-1) + "=";
		String kl = null;
		try {
			InputStream is = testFile.getContents();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			while ((kl = br.readLine()) != null) {
				if (kl.startsWith(code)) break;
			}
			is.close();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (kl == null) {
			return errorMessage(shell,"Could not find key for " + text);
		}
		String replacement = kl.substring(code.length());
		IDocumentProvider provider = editor.getDocumentProvider();
		IDocument document = provider.getDocument(editor.getEditorInput());
		try {
			document.replace(selection.getOffset(), selection.getLength(), replacement);
			// return infoMessage(shell,"Substituted with " + locked);
		} catch (BadLocationException e) {
			return errorMessage(shell,"Internal error: couldn't replace selection with " + replacement);
		}
		return null;
	}
}
