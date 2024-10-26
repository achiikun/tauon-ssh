/**
 *
 */
package tauon.app.ui.containers.session.pages.info.sysload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.components.page.subpage.Subpage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class SysLoadPage extends Subpage {
    private static final Logger LOG = LoggerFactory.getLogger(SysLoadPage.class);
    
    private final AtomicInteger sleepInterval = new AtomicInteger(3);
    private SystemLoadPanel systemLoadPanel;
    private JSpinner spInterval;
    private Timer timer;
    private LinuxMetrics metrics;
    private String os;

    /**
     *
     */
    public SysLoadPage(SessionContentPanel holder) {
        super(holder);
    }

    /**
     *
     */
    private void fetchSystemLoad() {
        holder.submitSSHOperation(instance -> {
            try {
                if (holder.isSessionClosed()) {
                    SwingUtilities.invokeAndWait(() -> timer.stop());
                    return;
                }
                System.out.println("Getting system metrics");
                this.metrics.updateMetrics(instance);
                if ("Linux".equals(this.metrics.getOs())) {
                    SwingUtilities.invokeAndWait(() -> {
                        // update ui stat
                        systemLoadPanel.setCpuUsage(this.metrics.getCpuUsage());
                        systemLoadPanel
                                .setMemoryUsage(this.metrics.getMemoryUsage());
                        systemLoadPanel
                                .setSwapUsage(this.metrics.getSwapUsage());
                        systemLoadPanel
                                .setTotalMemory(this.metrics.getTotalMemory());
                        systemLoadPanel
                                .setUsedMemory(this.metrics.getUsedMemory());
                        systemLoadPanel
                                .setTotalSwap(this.metrics.getTotalSwap());
                        systemLoadPanel.setUsedSwap(this.metrics.getUsedSwap());
                        systemLoadPanel.refreshUi();
                    });
                } else {
                    this.os = this.metrics.getOs();
                    this.metrics = null;
                    SwingUtilities.invokeLater(() -> {
                        this.timer.stop();
                        JLabel lblError = new JLabel("Unsupported OS " + this.os
                                + ", currently only Linux is supported");
                        lblError.setHorizontalAlignment(JLabel.CENTER);
                        lblError.setVerticalAlignment(JLabel.CENTER);
                        this.remove(systemLoadPanel);
                        this.add(lblError);
                        this.revalidate();
                        this.repaint(0);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void componentVisible() {
        startMonitoring();
    }

    private void componentHidden() {
        stopMonitoring();
    }

    private void startMonitoring() {
        if (metrics != null) {
            timer.start();
        }
    }

    private void stopMonitoring() {
        timer.stop();
    }

    @Override
    protected void createUI() {
        spInterval = new JSpinner(new SpinnerNumberModel(100, 1, 100, 1));
        spInterval.setValue(sleepInterval.get());
        spInterval.setMaximumSize(spInterval.getPreferredSize());
        spInterval.addChangeListener(e -> {
            int interval = (Integer) spInterval.getValue();
            System.out.println("New interval: " + interval);
            this.sleepInterval.set(interval);
            timer.stop();
            timer.setDelay(this.sleepInterval.get() * 1000);
            timer.start();
        });

        systemLoadPanel = new SystemLoadPanel();

        Box topPanel = Box.createHorizontalBox();
        topPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel titleLabel = new JLabel("System Monitor");
        titleLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 18));

        topPanel.add(titleLabel);
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(new JLabel(getBundle().getString("app.info_system_load.label.refresh_interval")));
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(spInterval);
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(new JLabel("Sec"));
        this.add(topPanel, BorderLayout.NORTH);
        this.add(systemLoadPanel);

        timer = new Timer(this.sleepInterval.get() * 1000, e -> fetchSystemLoad());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        metrics = new LinuxMetrics();
    }

    @Override
    protected void onComponentVisible() {
        componentVisible();
    }

    @Override
    protected void onComponentHide() {
        componentHidden();
    }

}
