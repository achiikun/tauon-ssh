package tauon.app.ui.containers.session.pages.terminal.snippets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.services.SnippetManager;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.components.misc.FontAwesomeContants;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static tauon.app.services.LanguageService.getBundle;

public class SnippetPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(SnippetPanel.class);
    
    private final DefaultListModel<SnippetItem> listModel = new DefaultListModel<>();
    private final List<SnippetItem> snippetList = new ArrayList<>();
    private final JList<SnippetItem> listView = new JList<>(listModel);
    private final JTextField searchTextField;
    private final JButton btnCopy;
    private final JButton btnInsert;
    private final JButton btnAdd;
    private final JButton btnEdit;
    private final JButton btnDel;

    public SnippetPanel(Consumer<String> callback, Consumer<String> callback2) {
        super(new BorderLayout());
        setBorder(new LineBorder(App.skin.getDefaultBorderColor(), 1));
        Box topBox = Box.createHorizontalBox();
        topBox.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, App.skin.getDefaultBorderColor()),
                new EmptyBorder(10, 10, 10, 10)));
        JLabel lblSearch = new JLabel();
        lblSearch.setFont(App.skin.getIconFont());
        lblSearch.setText(FontAwesomeContants.FA_SEARCH);
        topBox.add(lblSearch);
        topBox.add(Box.createHorizontalStrut(10));

        searchTextField = new SkinnedTextField(30);// new
        searchTextField.getDocument()
                .addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        filter();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        filter();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        filter();
                    }
                });
        topBox.add(searchTextField);

        listView.setCellRenderer(new SnippetListRenderer());
        listView.setBackground(App.skin.getTableBackgroundColor());

        btnAdd = new JButton(getBundle().getString("general.action.add"));
        btnEdit = new JButton(getBundle().getString("edit"));
        btnDel = new JButton(getBundle().getString("app.files.action.delete"));
        btnInsert = new JButton(getBundle().getString("insert"));
        btnCopy = new JButton(getBundle().getString("app.files.action.copy"));

        btnAdd.addActionListener(e -> {
            JTextField txtName = new SkinnedTextField(30);
            JTextField txtCommand = new SkinnedTextField(30);
            
            // TODO i18n
            if (JOptionPane.showOptionDialog(null,
                    new Object[]{"Snippet name", txtName, "Command",
                            txtCommand},
                    "New snippet", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, null,
                    null) == JOptionPane.OK_OPTION) {
                if (txtCommand.getText().length() < 1
                        || txtName.getText().length() < 1) {
                    JOptionPane.showMessageDialog(null,
                            "Please enter name and command");
                    return;
                }
                SnippetManager.getInstance().getSnippetItems().add(new SnippetItem(
                        txtName.getText(), txtCommand.getText()));
                SnippetManager.getInstance().saveSnippets();
            }
            callback2.accept(null);
        });

        btnEdit.addActionListener(e -> {
            int index = listView.getSelectedIndex();
            if (index < 0) {
                JOptionPane.showMessageDialog(null,
                        "Please select an item to edit");
                return;
            }

            SnippetItem snippetItem = listModel.get(index);

            JTextField txtName = new SkinnedTextField(30);
            JTextField txtCommand = new SkinnedTextField(30);

            txtName.setText(snippetItem.getName());
            txtCommand.setText(snippetItem.getCommand());

            if (JOptionPane.showOptionDialog(null,
                    new Object[]{"Snippet name", txtName, "Command",
                            txtCommand},
                    "New snippet", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, null,
                    null) == JOptionPane.OK_OPTION) {
                if (txtCommand.getText().length() < 1
                        || txtName.getText().length() < 1) {
                    JOptionPane.showMessageDialog(null,
                            "Please enter name and command");
                    return;
                }
                snippetItem.setCommand(txtCommand.getText());
                snippetItem.setName(txtName.getText());
                SnippetManager.getInstance().saveSnippets();
            }
            callback2.accept(null);
        });

        btnDel.addActionListener(e -> {
            int index = listView.getSelectedIndex();
            if (index < 0) {
                JOptionPane.showMessageDialog(null, "Please select an item");
                return;
            }

            SnippetItem snippetItem = listModel.get(index);
            SnippetManager.getInstance().getSnippetItems().remove(snippetItem);
            SnippetManager.getInstance().saveSnippets();
            loadSnippets();
            callback2.accept(null);
        });

        btnCopy.addActionListener(e -> {
            int index = listView.getSelectedIndex();
            if (index < 0) {
                JOptionPane.showMessageDialog(null, "Please select an item");
                return;
            }

            SnippetItem snippetItem = listModel.get(index);

            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(snippetItem.getCommand()), null);
            callback2.accept(null);
        });

        btnInsert.addActionListener(e -> {
            int index = listView.getSelectedIndex();
            if (index < 0) {
                JOptionPane.showMessageDialog(null, "Please select an item");
                return;
            }

            SnippetItem snippetItem = listModel.get(index);
            callback.accept(snippetItem.getCommand());
            callback2.accept(null);
        });

        Box bottomBox = Box.createHorizontalBox();
        bottomBox.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, App.skin.getDefaultBorderColor()),
                new EmptyBorder(10, 10, 10, 10)));
        bottomBox.add(btnInsert);
        bottomBox.add(Box.createHorizontalStrut(5));
        bottomBox.add(btnCopy);
        bottomBox.add(Box.createHorizontalGlue());
        bottomBox.add(Box.createHorizontalStrut(5));
        bottomBox.add(btnAdd);
        bottomBox.add(Box.createHorizontalStrut(5));
        bottomBox.add(btnEdit);
        bottomBox.add(Box.createHorizontalStrut(5));
        bottomBox.add(btnDel);

        setPreferredSize(new Dimension(400, 500));
        add(topBox, BorderLayout.NORTH);
        JScrollPane jScrollPane = new JScrollPane(listView);
        add(jScrollPane);
        add(bottomBox, BorderLayout.SOUTH);

    }

    public void loadSnippets() {
        this.snippetList.clear();
        this.snippetList.addAll(SnippetManager.getInstance().getSnippetItems());
        System.out.println("Snippet size: " + snippetList.size());
        filter();
    }

    private void filter() {
        this.listModel.clear();
        String text = searchTextField.getText().trim();
        if (text.length() < 1) {
            this.listModel.addAll(this.snippetList);
            return;
        }
        for (SnippetItem item : snippetList) {
            if (item.getCommand().contains(text)
                    || item.getName().contains(text)) {
                this.listModel.addElement(item);
            }
        }
    }
}
