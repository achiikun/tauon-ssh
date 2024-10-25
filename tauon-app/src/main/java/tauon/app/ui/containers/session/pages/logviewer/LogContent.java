/**
 *
 */
package tauon.app.ui.containers.session.pages.logviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.XZInputStream;
import tauon.app.App;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.exceptions.RemoteOperationException;
import tauon.app.exceptions.SessionClosedException;
import tauon.app.services.SettingsService;
import tauon.app.ssh.TauonRemoteSessionInstance;
import tauon.app.ui.components.closabletabs.ClosableTabContent;
import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.misc.SkinnedTextArea;
import tauon.app.ui.components.misc.TextGutter;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.util.misc.LayoutUtilities;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * @author subhro
 */
public class LogContent extends JPanel implements ClosableTabContent {
    private static final Logger LOG = LoggerFactory.getLogger(LogContent.class);
    
    private final static int LINE_PER_PAGE = 50;
    private final SessionContentPanel holder;
    private final String remoteFile;
    private final JButton btnNextPage;
    private final JButton btnPrevPage;
    private final JButton btnFirstPage;
    private final JButton btnLastPage;
    private final JTextArea textArea;
    private final JLabel lblCurrentPage;
    private final JLabel lblTotalPage;
    private final PagedLogSearchPanel logSearchPanel;
    private final Highlighter.HighlightPainter painter;
    private final TextGutter gutter;
    private final StartPage startPage;
    private final Consumer<String> onCloseListener;
    private File indexFile;
    private RandomAccessFile raf;
    private long totalLines;
    private long currentPage;
    private long pageCount;
    
    /**
     *
     */
    public LogContent(SessionContentPanel holder, String remoteLogFile, StartPage startPage, Consumer<String> onCloseListener) {
        super(new BorderLayout(), true);
        this.holder = holder;
        this.onCloseListener = onCloseListener;
        this.startPage = startPage;
        this.remoteFile = remoteLogFile;
        lblCurrentPage = new JLabel();
        lblCurrentPage.setHorizontalAlignment(JLabel.CENTER);
        lblTotalPage = new JLabel();
        lblTotalPage.setHorizontalAlignment(JLabel.CENTER);
        
        UIDefaults skin = App.skin.createToolbarSkin();
        
        btnFirstPage = new JButton();
        btnFirstPage.setToolTipText("First page");
        btnFirstPage.putClientProperty("Nimbus.Overrides", skin);
        btnFirstPage.setFont(App.skin.getIconFont());
        btnFirstPage.setText(FontAwesomeContants.FA_FAST_BACKWARD);
        btnFirstPage.addActionListener(e -> firstPage());
        
        btnNextPage = new JButton();
        btnNextPage.setToolTipText("Next page");
        btnNextPage.putClientProperty("Nimbus.Overrides", skin);
        btnNextPage.setFont(App.skin.getIconFont());
        btnNextPage.setText(FontAwesomeContants.FA_STEP_FORWARD);
        btnNextPage.addActionListener(e -> nextPage());
        
        btnPrevPage = new JButton("");
        btnPrevPage.setToolTipText("Previous page");
        btnPrevPage.putClientProperty("Nimbus.Overrides", skin);
        btnPrevPage.setFont(App.skin.getIconFont());
        btnPrevPage.setText(FontAwesomeContants.FA_STEP_BACKWARD);
        btnPrevPage.addActionListener(e -> previousPage());
        
        btnLastPage = new JButton();
        btnLastPage.setToolTipText("Last page");
        btnLastPage.putClientProperty("Nimbus.Overrides", skin);
        btnLastPage.setFont(App.skin.getIconFont());
        btnLastPage.setText(FontAwesomeContants.FA_FAST_FORWARD);
        btnLastPage.addActionListener(e -> lastPage());
        
        textArea = new SkinnedTextArea();
        textArea.setEditable(false);
        textArea.setBackground(App.skin.getSelectedTabColor());
        textArea.setWrapStyleWord(true);
        textArea.setFont(textArea.getFont().deriveFont((float) SettingsService.getSettings().getLogViewerFont()));
        this.textArea.setLineWrap(SettingsService.getSettings().isLogViewerUseWordWrap());
        
        gutter = new TextGutter(textArea);
        JScrollPane scrollPane = new SkinnedScrollPane(textArea);
        scrollPane.setRowHeaderView(gutter);
        this.add(scrollPane);
        
        JCheckBox chkLineWrap = new JCheckBox("Word wrap");
        
        chkLineWrap.addActionListener(e -> {
            this.textArea.setLineWrap(chkLineWrap.isSelected());
            SettingsService.getInstance().setAndSave(settings ->
                    settings.setLogViewerUseWordWrap(chkLineWrap.isSelected())
            );
        });
        
        chkLineWrap.setSelected(SettingsService.getSettings().isLogViewerUseWordWrap());
        
        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(SettingsService.getSettings().getLogViewerFont(), 5, 255, 1);
        JSpinner spFontSize = new JSpinner(spinnerNumberModel);
        spFontSize.setMaximumSize(spFontSize.getPreferredSize());
        spFontSize.addChangeListener(e -> {
            int fontSize = (int) spinnerNumberModel.getValue();
            textArea.setFont(textArea.getFont().deriveFont((float) fontSize));
            gutter.setFont(textArea.getFont());
            gutter.revalidate();
            gutter.repaint();
            SettingsService.getInstance().setAndSave(settings ->
                    settings.setLogViewerFont(fontSize)
            );
        });
        
        JButton btnReload = new JButton();
        btnReload.setToolTipText("Reload");
        btnReload.putClientProperty("Nimbus.Overrides", skin);
        btnReload.setFont(App.skin.getIconFont());
        btnReload.setText(FontAwesomeContants.FA_UNDO);
        btnReload.addActionListener(e -> {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException ex) {
                LOG.error("Exception while rendering results.", ex);
            }
            try {
                Files.delete(this.indexFile.toPath());
            } catch (Exception ex) {
                LOG.error("Exception while rendering results.", ex);
            }
            this.currentPage = 0;
            initPages();
        });
        
        JButton btnBookMark = new JButton();
        btnBookMark.setToolTipText("Add to bookmark/pin");
        btnBookMark.putClientProperty("Nimbus.Overrides", skin);
        btnBookMark.setFont(App.skin.getIconFont());
        btnBookMark.setText(FontAwesomeContants.FA_BOOKMARK);
        btnBookMark.addActionListener(e -> startPage.pinLog(remoteLogFile));
        
        Box toolbar = Box.createHorizontalBox();
        toolbar.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, App.skin.getDefaultBorderColor()), new EmptyBorder(5, 10, 5, 10)));
        toolbar.add(btnFirstPage);
        toolbar.add(btnPrevPage);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(lblCurrentPage);
        toolbar.add(lblTotalPage);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnNextPage);
        toolbar.add(btnLastPage);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(chkLineWrap);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(spFontSize);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnReload);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnBookMark);
        
        this.add(toolbar, BorderLayout.NORTH);
        
        logSearchPanel = new PagedLogSearchPanel(new SearchListener() {
            @Override
            public void search(String text) {
                AtomicBoolean stopFlag = new AtomicBoolean(false);
                holder.submitSSHOperationStoppable(instance -> {
                    try (RandomAccessFile searchIndex = LogContent.this.search(instance, text, stopFlag)) {
                        long len = searchIndex.length();
                        SwingUtilities.invokeAndWait(() -> logSearchPanel.setResults(searchIndex, len));
                    } catch (InvocationTargetException e) {
                        LOG.error("Exception while rendering results.", e);
                    }
                }, stopFlag);

//                holder.disableUi(stopFlag);
//                holder.executor.execute(() -> {
//                    try {
//                        RandomAccessFile searchIndex = LogContent.this.search(text, stopFlag);
//                        long len = searchIndex.length();
//                        SwingUtilities.invokeLater(() -> logSearchPanel.setResults(searchIndex, len));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        holder.enableUi();
//                    }
//                });
            }
            
            @Override
            public void select(long index) {
                System.out.println("Search item found on line: " + index);
                int page = (int) index / LINE_PER_PAGE;
                int line = (int) (index % LINE_PER_PAGE);
                System.out.println("Found on page: " + page + " line: " + line);
                if (currentPage == page) {
                    if (line < textArea.getLineCount() && line != -1) {
                        highlightLine(line);
                    }
                } else {
                    currentPage = page;
                    loadPage(line);
                }
            }
        });
        this.add(logSearchPanel, BorderLayout.SOUTH);
        
        painter = new DefaultHighlighter.DefaultHighlightPainter(App.skin.getAddressBarSelectionBackground());
        
        initPages();
    }
    
    private static void toByteArray(long value, byte[] result) {
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xffL);
            value >>= 8;
        }
    }
    
    private void initPages() {
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        holder.submitSSHOperationStoppable(instance -> {
            try {
                if ((indexFile(instance, true, stopFlag)) || (indexFile(instance, false, stopFlag))) {
                    this.totalLines = this.raf.length() / 16;
                    LOG.debug("Total lines: {}", this.totalLines);
                    if (this.totalLines > 0) {
                        this.pageCount = (long) Math.ceil((double) totalLines / LINE_PER_PAGE);
                        System.out.println("Number of pages: " + this.pageCount);
                        if (this.currentPage > this.pageCount) {
                            this.currentPage = this.pageCount;
                        }
                        String pageText = getPageText(instance, this.currentPage, stopFlag);
                        SwingUtilities.invokeAndWait(() -> {
                            
                            this.lblTotalPage.setText(String.format("/ %d ", this.pageCount));
                            this.lblCurrentPage.setText((this.currentPage + 1) + "");
                            
                            LayoutUtilities.equalizeSize(this.lblTotalPage, this.lblCurrentPage);
                            
                            this.textArea.setText(pageText);
                            if (!pageText.isEmpty()) {
                                this.textArea.setCaretPosition(0);
                            }
                            
                            gutter.setLineStart(1);
                            
                            this.revalidate();
                            this.repaint();
                        });
                        
                    }
                }
            } catch (InvocationTargetException e) {
                LOG.error("Exception while rendering results.", e);
            }
        }, stopFlag);

//        holder.disableUi(stopFlag);
//        holder.executor.execute(() -> {
//            try {
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                holder.enableUi();
//            }
//        });
    }
    
    private String getPageText(TauonRemoteSessionInstance instance, long page, AtomicBoolean stopFlag) throws IOException, RemoteOperationException, OperationCancelledException, SessionClosedException {
        long lineStart = page * LINE_PER_PAGE;
        long lineEnd = lineStart + LINE_PER_PAGE - 1;
        
        StringBuilder command = new StringBuilder();
        
        raf.seek(lineStart * 16);
        byte[] longBytes = new byte[8];
        if (raf.read(longBytes) != 8) {
            throw new IOException("EOF found");
        }
        
        long startOffset = ByteBuffer.wrap(longBytes).getLong();
        
        raf.seek(lineEnd * 16);
        if (raf.read(longBytes) != 8) {
            raf.seek(raf.length() - 16);
            raf.read(longBytes);
        }
        
        long endOffset = ByteBuffer.wrap(longBytes).getLong();
        raf.seek(lineEnd * 16 + 8);
        if (raf.read(longBytes) != 8) {
            raf.seek(raf.length() - 8);
            raf.read(longBytes);
        }
        long lineLength = ByteBuffer.wrap(longBytes).getLong();
        
        endOffset = endOffset + lineLength;
        
        long byteRange = endOffset - startOffset;
        
        if (startOffset < 8192) {
            command.append("dd if=\"").append(this.remoteFile).append("\" ibs=1 skip=").append(startOffset).append(" count=").append(byteRange).append(" 2>/dev/null | sed -ne '1,").append(LINE_PER_PAGE).append("p;").append(LINE_PER_PAGE + 1).append("q'");
        } else {
            long blockToSkip = startOffset / 8192;
            long bytesToSkip = startOffset % 8192;
            int blocks = (int) Math.ceil((double) byteRange / 8192);
            
            
            if (blocks * 8192L - bytesToSkip < byteRange) {
                blocks++;
            }
            command.append("dd if=\"" + this.remoteFile + "\" ibs=8192 skip=" + blockToSkip + " count=" + blocks + " 2>/dev/null | dd bs=1 skip=" + bytesToSkip + " 2>/dev/null | sed -ne '1," + LINE_PER_PAGE + "p;" + (LINE_PER_PAGE + 1) + "q'");
        }
        
        System.out.println("Command: " + command);
        StringBuilder output = new StringBuilder();
        
        int ret;
        if ((ret = instance.exec(command.toString(), stopFlag, output)) == 0) {
            return output.toString();
        } else {
            throw new RemoteOperationException.ErrorReturnCode(command.toString(), ret);
        }
    }
    
    private boolean indexFile(TauonRemoteSessionInstance instance, boolean xz, AtomicBoolean stopFlag) throws IOException, RemoteOperationException, OperationCancelledException, SessionClosedException {
        File tempFile = Files.createTempFile("muon" + UUID.randomUUID(), "index").toFile();
        System.out.println("Temp file: " + tempFile);
        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            String command = "LANG=C awk '{len=length($0); print len; }' \"" + remoteFile + "\" | " + (xz ? "xz" : "gzip") + " |cat";
            if (instance.execBin(command, stopFlag, outputStream, null) == 0) {
                
                try (
                        InputStream inputStream = new FileInputStream(tempFile);
                        InputStream gzIn = xz ? new XZInputStream(inputStream) : new GZIPInputStream(inputStream)
                ) {
                    this.indexFile = createIndexFile(gzIn);
                    this.raf = new RandomAccessFile(this.indexFile, "r");
                    return true;
                }
            }
        }
        return false;
    }
    
    private static File createIndexFile(InputStream inputStream) throws IOException {
        byte[] longBytes = new byte[8];
        long offset = 0;
        File tempFile = Files.createTempFile("muon" + UUID.randomUUID(), "index").toFile();
        try (
                OutputStream outputStream = new FileOutputStream(tempFile);
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                BufferedOutputStream bout = new BufferedOutputStream(outputStream)
        ) {
            while (true) {
                
                String line = br.readLine();
                if (line == null) break;
                
                line = line.trim();
                if (line.isEmpty()) continue;
                
                toByteArray(offset, longBytes);
                bout.write(longBytes);
                long len = Long.parseLong(line);
                toByteArray(len, longBytes);
                bout.write(longBytes);
                offset += (len + 1);
            }
        }
        return tempFile;
    }
    
    private void nextPage() {
        if (currentPage < pageCount - 1) {
            currentPage++;
            loadPage();
        }
    }
    
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            loadPage();
        }
    }
    
    private void firstPage() {
        currentPage = 0;
        loadPage();
    }
    
    private void lastPage() {
        if (this.pageCount > 0) {
            currentPage = this.pageCount - 1;
            loadPage();
        }
    }
    
    public void loadPage() {
        loadPage(-1);
    }
    
    public void loadPage(int line) {
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        holder.submitSSHOperationStoppable(instance -> {
            try{
                String pageText = getPageText(instance, this.currentPage, stopFlag);
                SwingUtilities.invokeAndWait(() -> {
                    this.textArea.setText(pageText);
                    if (pageText.length() > 0) {
                        this.textArea.setCaretPosition(0);
                    }
                    this.lblCurrentPage.setText((this.currentPage + 1) + "");
                    LayoutUtilities.equalizeSize(this.lblTotalPage, this.lblCurrentPage);
                    if (line < textArea.getLineCount() && line != -1) {
                        highlightLine(line);
                    }
                    long lineStart = this.currentPage * LINE_PER_PAGE;
                    gutter.setLineStart(lineStart + 1);
                });
            } catch (InvocationTargetException e) {
                LOG.error("Exception while rendering results.", e);
            }
        }, stopFlag);
//        holder.disableUi(stopFlag);
//        holder.executor.execute(() -> {
//            try {
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                holder.enableUi();
//            }
//        });
    }
    
    @Override
    public boolean close() {
        try {
            if (raf != null) {
                raf.close();
            }
        } catch (IOException ex) {
            LOG.error("Exception while rendering results.", ex);
        }
        try {
            Files.delete(this.indexFile.toPath());
        } catch (Exception ex) {
            LOG.error("Exception while rendering results.", ex);
        }
        onCloseListener.accept(remoteFile);
        return true;
    }
    
    private RandomAccessFile search(TauonRemoteSessionInstance instance, String text, AtomicBoolean stopFlag) throws IOException, RemoteOperationException, OperationCancelledException, SessionClosedException {
        byte[] longBytes = new byte[8];
        File tempFile = Files.createTempFile("muon" + UUID.randomUUID(), "index").toFile();
        StringBuilder command = new StringBuilder();
        command.append("awk '{if(index(tolower($0),\"").append(text.toLowerCase(Locale.ENGLISH)).append("\")){ print NR}}' \"").append(this.remoteFile).append("\"");
        System.out.println("Command: " + command);
        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            
            File searchIndexes = Files.createTempFile("muon" + UUID.randomUUID(), "index").toFile();
            int ret;
            if ((ret = instance.execBin(command.toString(), stopFlag, outputStream, null)) == 0) {
                try (
                        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tempFile)));
                        OutputStream out = new FileOutputStream(searchIndexes);
                        BufferedOutputStream bout = new BufferedOutputStream(out)
                ) {
                    while (true) {
                        
                        String line = br.readLine();
                        if (line == null) break;
                        
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        
                        long lineNo = Long.parseLong(line);
                        toByteArray(lineNo, longBytes);
                        bout.write(longBytes);
                        
                    }
                    return new RandomAccessFile(searchIndexes, "r");
                }
            } else {
                throw new RemoteOperationException.ErrorReturnCode(command.toString(), ret);
            }
        }
    }
    
    private void highlightLine(int lineNumber) {
        try {
            int startIndex = textArea.getLineStartOffset(lineNumber);
            int endIndex = textArea.getLineEndOffset(lineNumber);
            System.out.println("selection: " + startIndex + " " + endIndex);
            textArea.setCaretPosition(startIndex);
            textArea.getHighlighter().removeAllHighlights();
            textArea.getHighlighter().addHighlight(startIndex, endIndex, painter);
            System.out.println(textArea.modelToView2D(startIndex));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * @return the remoteFile
     */
    public String getRemoteFile() {
        return remoteFile;
    }
}
