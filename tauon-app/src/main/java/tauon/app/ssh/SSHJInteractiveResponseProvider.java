/**
 *
 */
package tauon.app.ssh;

import net.schmizz.sshj.userauth.method.ChallengeResponseProvider;
import net.schmizz.sshj.userauth.password.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * @author subhro
 *
 */
public class SSHJInteractiveResponseProvider implements ChallengeResponseProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(SSHJInteractiveResponseProvider.class);
    
    private boolean retry = true;
    
    private GuiHandle<?> guiHandle;
    
    public SSHJInteractiveResponseProvider(GuiHandle<?> guiHandle) {
        this.guiHandle = guiHandle;
    }
    
    @Override
    public List<String> getSubmethods() {
        return Collections.emptyList();
    }

    @Override
    public void init(Resource resource, String name, String instruction) {
        if (
                (name != null && !name.isEmpty())
                || (instruction != null && !instruction.isEmpty())
        ) {
            guiHandle.showMessage(name, instruction);
        }
    }

    @Override
    public char[] getResponse(String prompt, boolean echo) {
        String str = guiHandle.promptInput(prompt, echo);
        if (str != null) {
            return str.toCharArray();
        }
        retry = false;
        return null;
    }

    @Override
    public boolean shouldRetry() {
        return retry;
    }

}
