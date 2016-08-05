package edu.uwm.cs.junit;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;


public class TestCaseUnlockDialog extends JDialog {

	private static final String BUILT_IN = "<Built-In>";

	/**
	 * Keep Eclipse Happy
	 */
	private static final long serialVersionUID = 1L;

	private static final int CODE_SIZE = 14;
	
	private final int hash;
	private final JComponent typeChooser;
	private final JTextField valueField;
	
	private String type;
	private boolean complete = false;
	
	public TestCaseUnlockDialog(String[] code, String t, int h) {
		super((JWindow)null, "Unlock Test Case", ModalityType.DOCUMENT_MODAL);
		type = t;
		hash = h;
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.gridwidth = 2;
		this.add(createCodeArea(code),c);
		typeChooser = createTypeComponent(type);
		c.gridy = 1;
		c.weightx = 0.0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.ipadx = 5;
		this.add(typeChooser,c);
		c.ipadx = 0;
		valueField = new JTextField(20);
		valueField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCheck();
			}	
		});
		c.gridx = 1;
		c.weightx = 0.5;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add(valueField,c);
		c.gridy = 2;
		c.gridx = 0;
		c.weightx = 1.0;
		c.gridwidth = 2;
		this.add(createButtonPanel(),c);
		// Dimension d = this.getPreferredSize();
		// setSize(d.width,d.height+20); // fudge factors needed for some reason
		setLocationRelativeTo(null);
	}

	/**
	 * @param code
	 * @param w
	 */
	private JTextArea createCodeArea(String[] code) {
		int w=0;
		for (int i=0; i < code.length; ++i) {
			if (code[i].length() > w) {
				w = code[i].length();
			}
		}
		JTextArea codeArea = new JTextArea(code.length,w+20); // handle tabs, I think
		codeArea.setEditable(false);
		codeArea.setLineWrap(false);
		StringBuilder allCode = new StringBuilder();
		for (int i=0; i < code.length; ++i) {
			allCode.append(code[i]);
			allCode.append('\n');
		}
		codeArea.setText(allCode.toString());
		codeArea.setFont(new Font("Monospaced",Font.PLAIN,CODE_SIZE));
		return codeArea;
	}
	
	private JComponent createTypeComponent(String type) {
		if (type == null) {
			List<String> classes = Util.getFromStringClasses();
			classes.add(0,BUILT_IN);
			type = BUILT_IN;
			final JComboBox<String> result = new JComboBox<String>(classes.toArray(new String[classes.size()]));
			result.setEditable(true);
			result.setSelectedIndex(0);
			result.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					doSelectType(result);
				}
			});
			return result;
		} else {
			return new JLabel(type);
		}
	}
	
	private JPanel createButtonPanel() {
		JButton checkButton = new JButton("Check");
		JButton cancelButton = new JButton("Cancel");
		JPanel result = new JPanel();
		result.add(checkButton);
		result.add(cancelButton);
		checkButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				doCheck();
			}
			
		});
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				doCancel();
			}
			
		});
		return result;
	}
	
	private void doSelectType(JComboBox<String> typeChooser) {
		type = (String)typeChooser.getSelectedItem();
	}
	
	public Object getObject() throws NumberFormatException, ParseException {
		String s = valueField.getText();
		if (type == null || type.equals("BUILT_IN")) return Util.parseObject(s);
		else if (type.equals("Integer")) return new Integer(Integer.parseInt(s));
		else if (type.equals("String")) return Util.unescape(s);
		else if (type.equals("Character")) return Util.unescape(s).charAt(0);
		else if (type.equals("Boolean")) {
			if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
			else if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
			throw new ParseException("The only legal booleans are true and false.");
		} else return Util.fromString(type,s);
	}
	
	private void doCheck() {
		try {
			Object result = getObject();
			if (Util.checkHash(hash, result)) {
				JOptionPane.showMessageDialog(this, "Test case will be unlocked", "Success!", JOptionPane.INFORMATION_MESSAGE);
				complete = true;
				setVisible(false);
			} else {
				JOptionPane.showMessageDialog(this, "Not the correct value", "Failure", JOptionPane.ERROR_MESSAGE);
			}
		} catch (RuntimeException e) {
		  e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot read value", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void doCancel() {
		setVisible(false);
	}
	
	private static class Results {
		public Object result = Util.ERROR_OBJECT;
	}
	
	public static Object show(final String[] code, final String type, final int key) throws HeadlessException {
		final Results results = new Results();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					TestCaseUnlockDialog d = new TestCaseUnlockDialog(code,type,key);
					// Dimension dim = d.getPreferredSize();
					// d.setSize(dim.width+100,dim.height+20); // fudge factors needed for some reason
					d.pack();
					d.valueField.requestFocusInWindow();
					d.setVisible(true);
					if (d.complete) results.result = d.getObject();
					d.dispose();
				}
			});
		} catch (InvocationTargetException e) {
			// muffle
		} catch (InterruptedException e) {
			// muffle
		}
		return results.result;
	}
}
