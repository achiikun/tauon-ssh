/**
 *
 */
package tauon.app.ui.containers.session.pages.info;

import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.ui.components.page.subpage.SubpagingPage;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.containers.session.pages.info.processview.ProcessViewer;
import tauon.app.ui.containers.session.pages.info.portview.PortViewer;
import tauon.app.ui.containers.session.pages.info.services.ServicePanel;
import tauon.app.ui.containers.session.pages.info.sysinfo.SysInfoPanel;
import tauon.app.ui.containers.session.pages.info.sysload.SysLoadPage;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class InfoPage extends SubpagingPage {
    
    public InfoPage(SessionContentPanel holder) {
        super(holder);
    }
    
    @Override
    public String getIcon() {
        return FontAwesomeContants.FA_INFO_CIRCLE;
        // return FontAwesomeContants.FA_SLIDERS;
    }

    @Override
    public String getText() {
        return getBundle().getString("app.info.title");
    }

    @Override
    public void onCreateSubpages(SessionContentPanel holder) {
        addSubpage(
                "SYS_INFO",
                getBundle().getString("app.system_info.title"),
                FontAwesomeContants.FA_LINUX,
                new SysInfoPanel(holder)
        );
        addSubpage(
                "SYS_LOAD",
                getBundle().getString("app.system_load.title"),
                FontAwesomeContants.FA_AREA_CHART,
                new SysLoadPage(holder)
        );
        addSubpage(
                "SYSTEMD_SERVICES",
                getBundle().getString("app.info_services_systemd.title"),
                FontAwesomeContants.FA_SERVER,
                new ServicePanel(holder)
        );
        addSubpage(
                "SYS_PROCESSES",
                getBundle().getString("app.info_processes.title"),
                FontAwesomeContants.FA_COGS,
                new ProcessViewer(holder)
        );
        addSubpage(
                "PROC_PORT",
                getBundle().getString("app.info_ports.title"),
                FontAwesomeContants.FA_DATABASE,
                new PortViewer(holder)
        );
    }
    
}
