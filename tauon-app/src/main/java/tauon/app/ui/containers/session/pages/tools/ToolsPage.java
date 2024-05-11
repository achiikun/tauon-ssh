/**
 *
 */
package tauon.app.ui.containers.session.pages.tools;

import tauon.app.ui.components.misc.FontAwesomeContants;
import tauon.app.ui.components.page.subpage.SubpagingPage;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.ui.containers.session.pages.tools.diskspace.DiskspaceAnalyzer;
import tauon.app.ui.containers.session.pages.tools.keys.KeyPage;
import tauon.app.ui.containers.session.pages.tools.nettools.NetworkToolsPage;
import tauon.app.ui.containers.session.pages.tools.search.SearchPanel;

import static tauon.app.services.LanguageService.getBundle;

/**
 * @author subhro
 *
 */
public class ToolsPage extends SubpagingPage {
    
    public ToolsPage(SessionContentPanel holder) {
        super(holder);
    }
    
    @Override
    public String getIcon() {
        return FontAwesomeContants.FA_BRIEFCASE;
        // return FontAwesomeContants.FA_SLIDERS;
    }

    @Override
    public String getText() {
        return getBundle().getString("toolbox");
    }

    @Override
    public void onCreateSubpages(SessionContentPanel holder) {
        addSubpage(
                "SSH_KEYS",
                getBundle().getString("ssh_keys"),
                FontAwesomeContants.FA_KEY,
                new KeyPage(holder)
        );
        addSubpage(
                "NET_TOOLS",
                getBundle().getString("network_tools"),
                FontAwesomeContants.FA_WRENCH,
                new NetworkToolsPage(holder)
        );
        addSubpage(
                "FILE_SEARCH",
                getBundle().getString("file_search"),
                FontAwesomeContants.FA_SEARCH,
                new SearchPanel(holder)
        );
        addSubpage(
                "DISKSPACE",
                getBundle().getString("diskspace"),
                FontAwesomeContants.FA_PIE_CHART,
                new DiskspaceAnalyzer(holder)
        );
    }
    
}
