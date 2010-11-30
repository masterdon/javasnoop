/*/*
 * Copyright, Aspect Security, Inc.
 *
 * This file is part of JavaSnoop.
 *
 * JavaSnoop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JavaSnoop is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaSnoop.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aspect.snoop.ui;

import com.aspect.snoop.agent.AgentCommunicationException;
import com.aspect.snoop.agent.SnoopToAgentClient;
import com.aspect.snoop.messages.agent.ExecuteScriptResponse;
import com.aspect.snoop.util.UIUtil;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jdesktop.application.Action;

/**
 *
 * @author adabirsiaghi
 */
public class ScriptingView extends javax.swing.JDialog {

    private static final Logger logger = Logger.getLogger(ScriptingView.class);

    private static final String nl = System.getProperty("line.separator");

    private static final String prompt = "% ";

    private StyledDocument console;

    SnoopToAgentClient client;

    public ScriptingView(java.awt.Frame parent, boolean modal, SnoopToAgentClient client) {
        super(parent, modal);

        this.client = client;
        console = new DefaultStyledDocument();

        initComponents();

        ((RSyntaxTextArea)txtScript).setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        ((RSyntaxTextArea)txtScript).setFont(new Font("Courier",Font.PLAIN,12));

        showPrompt();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        txtScript = new RSyntaxTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtConsole = new JTextPane(console);
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        chkRememberState = new javax.swing.JCheckBox();
        lstLanguage = new javax.swing.JComboBox();
        btnExecute = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.aspect.snoop.JavaSnoop.class).getContext().getResourceMap(ScriptingView.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        txtScript.setColumns(20);
        txtScript.setRows(5);
        txtScript.setName("txtScript"); // NOI18N
        jScrollPane1.setViewportView(txtScript);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        txtConsole.setEditable(false);
        txtConsole.setName("txtConsole"); // NOI18N
        jScrollPane2.setViewportView(txtConsole);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        chkRememberState.setText(resourceMap.getString("chkRememberState.text")); // NOI18N
        chkRememberState.setName("chkRememberState"); // NOI18N

        lstLanguage.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "BeanShell", "Jython" }));
        lstLanguage.setName("lstLanguage"); // NOI18N
        lstLanguage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lstLanguageActionPerformed(evt);
            }
        });

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.aspect.snoop.JavaSnoop.class).getContext().getActionMap(ScriptingView.class, this);
        btnExecute.setAction(actionMap.get("executeScript")); // NOI18N
        btnExecute.setText(resourceMap.getString("btnExecute.text")); // NOI18N
        btnExecute.setName("btnExecute"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 323, Short.MAX_VALUE)
                        .addComponent(lstLanguage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chkRememberState))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 361, Short.MAX_VALUE)
                        .addComponent(btnExecute)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(chkRememberState)
                    .addComponent(lstLanguage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnExecute)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void lstLanguageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lstLanguageActionPerformed
        String lang = (String)lstLanguage.getSelectedItem();
        if ( "Jython".equals(lang) ) {
            ((RSyntaxTextArea)txtScript).setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        } else if ( "BeanShell".equals(lang) ) {
            ((RSyntaxTextArea)txtScript).setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        }
    }//GEN-LAST:event_lstLanguageActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ScriptingView dialog = new ScriptingView(new javax.swing.JFrame(), true, null);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    public void showPrompt() {
        
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        attributes.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.FALSE);
        attributes.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.FALSE);
        attributes.addAttribute(StyleConstants.CharacterConstants.Foreground, Color.black);
        
        try {
            console.insertString(console.getLength(), prompt, attributes);
            txtConsole.setCaretPosition( console.getLength() );
        } catch (BadLocationException ex) {
            logger.error(ex);
        }
        
    }

    public void showErr(String message) {

        SimpleAttributeSet attributes = new SimpleAttributeSet();
        attributes.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.FALSE);
        attributes.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.FALSE);
        attributes.addAttribute(StyleConstants.CharacterConstants.Foreground, Color.red);

        try {
            console.insertString(console.getLength(), message + nl, attributes);
            txtConsole.setCaretPosition( console.getLength() );
        } catch (BadLocationException ex) {
            logger.error(ex);
        }
    }

    public void showOut(String s) {

        SimpleAttributeSet attributes = new SimpleAttributeSet();
        attributes.addAttribute(StyleConstants.CharacterConstants.Bold, Boolean.FALSE);
        attributes.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.FALSE);
        attributes.addAttribute(StyleConstants.CharacterConstants.Foreground, Color.blue);

        try {
            console.insertString(console.getLength(), s + nl, attributes);
            txtConsole.setCaretPosition( console.getLength() );
        } catch (BadLocationException ex) {
            logger.error(ex);
        }
    }

    @Action
    public void executeScript() {

        try {

            String lang = (String) lstLanguage.getSelectedItem();
            String code = txtScript.getText();

            ExecuteScriptResponse response = client.executeScript(lang, code);
            
            String output = response.getOutput();
            String err = response.getErr();

            if ( output != null && output.length() > 0 )
                showOut(response.getOutput());

            if ( err != null && err.length() > 0 )
                showErr(response.getErr());

            showOut("");

            showPrompt();

        } catch (AgentCommunicationException ex) {
            UIUtil.showErrorMessage(this, "Problem with script execution: " + ex.getMessage());
           logger.error(ex);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnExecute;
    private javax.swing.JCheckBox chkRememberState;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JComboBox lstLanguage;
    private javax.swing.JTextPane txtConsole;
    private javax.swing.JTextArea txtScript;
    // End of variables declaration//GEN-END:variables

}
