/**
 *
 */
package tauon.app.ui.containers.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.App;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.services.ConfigFilesService;
import tauon.app.services.SettingsService;
import tauon.app.settings.SessionInfo;
import tauon.app.ui.components.glasspanes.AppInputBlocker;
import tauon.app.ui.components.glasspanes.InputBlocker;
import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.dialogs.sessions.NewSessionDlg;
import tauon.app.ui.dialogs.settings.SettingsDialog;
import tauon.app.ui.dialogs.settings.SettingsPageName;
import tauon.app.util.misc.CertificateValidator;
import tauon.app.util.misc.FormatUtils;
import tauon.app.util.misc.PlatformUtils;
import tauon.app.util.misc.VersionEntry;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.swing.Timer;

import static tauon.app.services.LanguageService.getBundle;
import static tauon.app.util.misc.Constants.*;

/**
 * @author subhro
 */
public class AppWindow extends JFrame {
    private static final Logger LOG = LoggerFactory.getLogger(AppWindow.class);
    public static final int SESSION_LIST_PANEL_SIZE = 200;
    
    private final CardLayout sessionCard;
    private final JPanel cardPanel;
    private final BackgroundTransferPanel uploadPanel;
    private final BackgroundTransferPanel downloadPanel;
    private final Component bottomPanel;
    
    private final Component sessionPanelTop;
    private final Component collapedSessionPanelTop;
    
    private SessionListPanel sessionListPanel;
    private JLabel lblUploadCount, lblDownloadCount;
    private JPopupMenu popup;
//    private JLabel lblUpdate, lblUpdateText;
    
    private final InputBlocker inputBlocker;
    private final FileTransferManager fileTransferManager;
    
    public final GraphicalHostKeyVerifier hostKeyVerifier;

    private boolean desiredPanelVisible = true; // Variável para controlar o estado de visibilidade
    private boolean panelVisible = true; // Variável para controlar o estado de visibilidade
    
    /**
     *
     */
    public AppWindow() throws IOException {
        super(APPLICATION_NAME);
        
        try {
            this.setIconImage(ImageIO.read(Objects.requireNonNull(AppWindow.class.getResource("/tauonssh.png"))));
        } catch (Exception e) {
            LOG.warn("Icon not loaded", e);
        }
        
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        inputBlocker = new AppInputBlocker(this);
        
//        File knownHostFile = new File(CONFIG_DIR, "known_hosts");
        File knownHostFile = ConfigFilesService.getInstance().getFile("known_hosts");
        hostKeyVerifier = new GraphicalHostKeyVerifier(knownHostFile);
        
        Insets inset = Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());

        Dimension screenD = Toolkit.getDefaultToolkit().getScreenSize();

        int screenWidth = screenD.width - inset.left - inset.right;
        int screenHeight = screenD.height - inset.top - inset.bottom;

        if (screenWidth < 1024 || screenHeight < 650 || SettingsService.getSettings().isStartMaximized()) {
            setSize(screenWidth, screenHeight);
        } else {
            int width = (screenWidth * 80) / 100;
            int height = (screenHeight * 80) / 100;
            setSize(width, height);
        }

        this.setLocationRelativeTo(null);

        this.sessionCard = new CardLayout();
        this.cardPanel = new JPanel(this.sessionCard, true);
        this.cardPanel.setDoubleBuffered(true);
        
        collapedSessionPanelTop = createCollapsedSessionPanelTop();
        sessionPanelTop = createSessionPanelTop();
        
        sessionListPanel = new SessionListPanel(this);
        sessionListPanel.add(sessionPanelTop, BorderLayout.NORTH);
        sessionListPanel.setPreferredSize(new Dimension(SESSION_LIST_PANEL_SIZE, sessionListPanel.getHeight()));
        
        this.add(sessionListPanel, BorderLayout.WEST);
        this.add(this.cardPanel);


        this.bottomPanel = createBottomPanel();
        this.add(this.bottomPanel, BorderLayout.SOUTH);

        this.uploadPanel = new BackgroundTransferPanel(count ->
                SwingUtilities.invokeLater(() ->
                        lblUploadCount.setText(count + "")));

        this.downloadPanel = new BackgroundTransferPanel(count ->
                SwingUtilities.invokeLater(() ->
                        lblDownloadCount.setText(count + "")));
        
        this.fileTransferManager = new FileTransferManager(this, uploadPanel, downloadPanel);
        
    }

    public void createFirstSessionPanel() {
        try {
            
            SessionInfo info = new NewSessionDlg(this).newSession();
            if (info != null) {
                sessionListPanel.createSession(info);
            }
            
        } catch (OperationCancelledException ignored) {
        
        }
    }
    
    public void slideSessionListPanel() {
        
        if(desiredPanelVisible != panelVisible){
            // Wait until animation ends
            return;
        }
        
        desiredPanelVisible = !desiredPanelVisible;
        
        if(desiredPanelVisible){
            sessionListPanel.remove(collapedSessionPanelTop);
            sessionListPanel.add(sessionPanelTop, BorderLayout.NORTH);
            sessionListPanel.collapsed(false);
        }else{
            sessionListPanel.remove(sessionPanelTop);
            sessionListPanel.add(collapedSessionPanelTop, BorderLayout.NORTH);
            sessionListPanel.collapsed(true);
        }
        
        int minWidth = (int) sessionListPanel.getMinimumSize().getWidth();
        int targetWidth = desiredPanelVisible ? SESSION_LIST_PANEL_SIZE : minWidth; // Define 200 como a largura quando o painel está visível
        
        // Define a direção da animação
        int direction = panelVisible ? -10 : 10; // Ajusta a largura em 10 pixels a cada passo
        
        Timer timer = new Timer(10, null); // Timer para animar a cada 10 milissegundos
        timer.addActionListener(e -> {
            // Calcula o novo tamanho
            int newWidth = sessionListPanel.getWidth() + direction;
            
            // Verifica se a animação terminou
            if ((direction > 0 && newWidth >= targetWidth) || (direction < 0 && newWidth <= targetWidth)) {
                newWidth = targetWidth;
                timer.stop(); // Para o timer quando a animação termina
            }
            
            // Ajusta o tamanho do painel
            sessionListPanel.setPreferredSize(new Dimension(newWidth, sessionListPanel.getHeight()));
            sessionListPanel.revalidate();
            
            // Quando a animação termina, atualiza o estado de visibilidade
            if (newWidth == targetWidth) {
                panelVisible = !panelVisible;
            }
        });
        timer.start(); // Inicia o timer
    }

    private Component createSessionPanelTop() {
        JLabel lblSession = new JLabel(getBundle().getString("app.ui.label.sessions"));
        lblSession.setFont(App.skin.getDefaultFont().deriveFont(14.0f));
        
        Font font = App.skin.getIconFont().deriveFont(14.0f);
        Dimension dimension = new Dimension(30,30);
        
        JButton btnList = new JButton();
        btnList.setFont(font);
        btnList.setText(FontAwesomeContants.FA_BARS);
        btnList.setMaximumSize(dimension);
        btnList.addActionListener(e -> this.slideSessionListPanel());
        
        JButton btnNew = new JButton();
        btnNew.setFont(font);
        btnNew.setText(FontAwesomeContants.FA_PLUS);
        btnNew.setMaximumSize(dimension);
        btnNew.addActionListener(e -> this.createFirstSessionPanel());

        Box topBox = Box.createHorizontalBox();
        topBox.setBorder(new EmptyBorder(5, 5, 5, 5));
        topBox.add(btnList);
        topBox.add(Box.createRigidArea(new Dimension(5, 0)));
        topBox.add(btnNew);
        topBox.add(Box.createRigidArea(new Dimension(5, 0)));
        topBox.add(lblSession);

        topBox.add(Box.createHorizontalGlue());
    
        return topBox;

    }
    
    private Component createCollapsedSessionPanelTop() {
        
        JLabel lblSession = new JLabel(getBundle().getString("app.ui.label.sessions"));
        lblSession.setFont(App.skin.getDefaultFont().deriveFont(14.0f));
        
        Font font = App.skin.getIconFont().deriveFont(14.0f);
        Dimension dimension = new Dimension(30,30);
        
        JButton btnList = new JButton();
        btnList.setFont(font);
        btnList.setText(FontAwesomeContants.FA_BARS);
        btnList.setMaximumSize(dimension);
        btnList.addActionListener(e -> this.slideSessionListPanel());
        
        JButton btnNew = new JButton();
        btnNew.setFont(font);
        btnNew.setText(FontAwesomeContants.FA_PLUS);
        btnNew.setMaximumSize(dimension);
        btnNew.addActionListener(e -> this.createFirstSessionPanel());
        
        Box listBox = Box.createHorizontalBox();
        listBox.setBorder(new EmptyBorder(5, 5, 5, 5));
        listBox.add(btnList);
        listBox.add(Box.createGlue());
        
        Box newBox = Box.createHorizontalBox();
        newBox.setBorder(new EmptyBorder(0, 5, 5, 5));
        newBox.add(btnNew);
        newBox.add(Box.createGlue());
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(listBox, BorderLayout.NORTH);
        panel.add(newBox, BorderLayout.SOUTH);
        
        return panel;
        
    }

    /**
     * @param sessionContentPanel
     */
    public void showSession(SessionContentPanel sessionContentPanel) {
        cardPanel.add(sessionContentPanel, sessionContentPanel.hashCode() + "");
        sessionCard.show(cardPanel, sessionContentPanel.hashCode() + "");
        revalidate();
        repaint();
    }
    
    /**
     * @param sessionContentPanel
     */
    public void removeSession(SessionContentPanel sessionContentPanel) {
        cardPanel.remove(sessionContentPanel);
        uploadPanel.removePendingTransfers(sessionContentPanel);
        downloadPanel.removePendingTransfers(sessionContentPanel);
        revalidate();
        repaint();
    }

    private Component createBottomPanel() {
        popup = new JPopupMenu();
        popup.setBorder(new LineBorder(App.skin.getDefaultBorderColor(), 1));
        popup.setPreferredSize(new Dimension(400, 500));

        Box b1 = Box.createHorizontalBox();
        b1.setOpaque(true);
        b1.setBackground(App.skin.getTableBackgroundColor());
        b1.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, App.skin.getDefaultBorderColor()),
                new EmptyBorder(5, 5, 5, 5)));
        b1.add(Box.createRigidArea(new Dimension(10, 10)));
        
        JLabel lblBrand = new JLabel(APPLICATION_NAME + " " + VERSION.getTagName());
        lblBrand.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(CertificateValidator.registerCertificateHook(
                        (chain, authType) -> JOptionPane.showConfirmDialog(
                                AppWindow.this,
                                getBundle().getString("app.ui.trust_certificate.message"),
                                getBundle().getString("app.ui.trust_certificate.title"),
                                JOptionPane.YES_NO_OPTION
                        ) == JOptionPane.YES_OPTION)
                ) {
                    VersionEntry lastVersion = VersionEntry.getLastVersionFromGithub();
                    if(lastVersion != null) {
                        if (lastVersion.compareTo(VERSION) > 0) {
//                            lblUpdate.setText(FontAwesomeContants.FA_DOWNLOAD);
//                            lblUpdateText.setText("Update available");
                            if(JOptionPane.showConfirmDialog(
                                    AppWindow.this,
                                    FormatUtils.$$(
                                            getBundle().getString("app.ui.update_check.new_version.message"),
                                            Map.of(
                                                    "OLD_VERSION", VERSION.getTagName(),
                                                    "NEW_VERSION", lastVersion.getTagName()
                                            )
                                    ),
                                    getBundle().getString("app.ui.update_check.new_version.title"),
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE
                            )== JOptionPane.YES_OPTION)
                                PlatformUtils.openWeb(REPOSITORY_TAG_URL + lastVersion.getTagName());
                        }else{
                            JOptionPane.showMessageDialog(
                                    AppWindow.this,
                                    getBundle().getString("app.ui.update_check.no_version.message"),
                                    getBundle().getString("app.ui.update_check.no_version.title"),
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    }else{
                        if(JOptionPane.showConfirmDialog(
                                AppWindow.this,
                                getBundle().getString("app.ui.update_check.failed.message"),
                                getBundle().getString("app.ui.update_check.failed.title"),
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        )== JOptionPane.YES_OPTION)
                            PlatformUtils.openWeb(REPOSITORY_URL);
                    }
                }
                
                
            }
        });
        lblBrand.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblBrand.setVerticalAlignment(JLabel.CENTER);
        lblBrand.setToolTipText(getBundle().getString("app.ui.update_check.tooltip"));
        b1.add(lblBrand);
        b1.add(Box.createRigidArea(new Dimension(10, 10)));
        
        JLabel lblUrl = new JLabel(REPOSITORY_URL);
        lblUrl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                PlatformUtils.openWeb(REPOSITORY_URL);
            }
        });
        lblUrl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblUrl.setToolTipText(getBundle().getString("app.ui.repository_url.tooltip"));
        b1.add(lblUrl);

        b1.add(Box.createHorizontalGlue());

        JLabel lblUpload = new JLabel();
        lblUpload.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblUpload.setFont(App.skin.getIconFont().deriveFont(16.0f));
        lblUpload.setText(FontAwesomeContants.FA_CLOUD_UPLOAD);
        b1.add(lblUpload);
        b1.add(Box.createRigidArea(new Dimension(5, 10)));
        lblUploadCount = new JLabel("0");
        b1.add(lblUploadCount);

        lblUpload.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup(uploadPanel, lblUpload);
            }
        });

        lblUploadCount.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup(uploadPanel, lblUpload);
            }
        });

        b1.add(Box.createRigidArea(new Dimension(10, 10)));

        JLabel lblDownload = new JLabel();
        lblDownload.setBorder(null);
        lblDownload.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblDownload.setFont(App.skin.getIconFont().deriveFont(16.0f));
        lblDownload.setText(FontAwesomeContants.FA_CLOUD_DOWNLOAD);
        b1.add(lblDownload);
        b1.add(Box.createRigidArea(new Dimension(5, 10)));
        lblDownloadCount = new JLabel("0");
        b1.add(lblDownloadCount);

        lblDownload.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup(downloadPanel, lblDownload);
            }
        });
        
        lblDownloadCount.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup(downloadPanel, lblDownload);
            }
        });

        b1.add(Box.createRigidArea(new Dimension(30, 10)));

        JLabel lblHelp = new JLabel();
        lblHelp.setFont(App.skin.getIconFont().deriveFont(16.0f));
        lblHelp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                PlatformUtils.openWeb(HELP_URL);
            }
        });
        lblHelp.setText(FontAwesomeContants.FA_QUESTION_CIRCLE);
        lblHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblHelp.setToolTipText(getBundle().getString("app.ui.help.tooltip"));
        b1.add(lblHelp);
        
        b1.add(Box.createRigidArea(new Dimension(30, 10)));
        
        JLabel btnSettings = new JLabel();
        btnSettings.setFont(App.skin.getIconFont().deriveFont(16.0f));
        btnSettings.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openSettings(null);
            }
        });
        btnSettings.setText(FontAwesomeContants.FA_COG);
        btnSettings.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b1.add(btnSettings);
        b1.add(Box.createRigidArea(new Dimension(10, 10)));

//        lblUpdate = new JLabel();
//        lblUpdate.setCursor(new Cursor(Cursor.HAND_CURSOR));
//        lblUpdate.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                checkUpdates();
//            }
//        });
//
//        lblUpdate.setFont(App.skin.getIconFont().deriveFont(16.0f));
//        lblUpdate.setText(FontAwesomeContants.FA_REFRESH);
//        b1.add(lblUpdate);
//
//        b1.add(Box.createRigidArea(new Dimension(5, 10)));
//
//        lblUpdateText = new JLabel(getBundle().getString("app.ui.label.check_updates"));
//        lblUpdateText.setCursor(new Cursor(Cursor.HAND_CURSOR));
//        lblUpdateText.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                checkUpdates();
//            }
//        });
//
//        b1.add(lblUpdateText);

        b1.add(Box.createRigidArea(new Dimension(10, 10)));

        return b1;
    }

    private void showPopup(Component panel, Component invoker) {
        popup.removeAll();
        popup.add(panel);

        popup.show(bottomPanel, bottomPanel.getWidth() - popup.getPreferredSize().width,
                -popup.getPreferredSize().height);
        popup.setInvoker(invoker);
    }

    public void openSettings(SettingsPageName page) {
        SettingsDialog settingsDialog = new SettingsDialog(this);
        settingsDialog.showDialog(this, page);
    }
    
    public InputBlocker getInputBlocker() {
        return inputBlocker;
    }
    
    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }
    
    public SessionContentPanel findSessionById(UUID uuid) {
        return sessionListPanel.findSessionById(uuid);
    }
}
