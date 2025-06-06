package tauon.app.ui.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tauon.app.settings.NamedItem;
import tauon.app.settings.SessionFolder;
import tauon.app.settings.SiteInfo;
import tauon.app.ui.dialogs.sessions.SavedSessionTree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

import static tauon.app.services.LanguageService.getBundle;

public class TreeManager {
    private static final Logger LOG = LoggerFactory.getLogger(TreeManager.class);
    
    public static final String EMPTY_ROOT_NODE_USER_OBJET = "Empty_Root";

    private TreeManager() {
        // TODO make it public and accept a JTree
    }
    
    public static synchronized DefaultMutableTreeNode createNodeTree(SessionFolder folder) {
        NamedItem item = new NamedItem();
        item.setName(folder.getName());
        item.setId(folder.getId());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(item);
        for (SiteInfo info : folder.getItems()) {
            DefaultMutableTreeNode c = new DefaultMutableTreeNode(info.copy());
            c.setAllowsChildren(false);
            node.add(c);
        }

        for (SessionFolder folderItem : folder.getFolders()) {
            node.add(createNodeTree(folderItem));
        }
        return node;
    }
    
    public static DefaultMutableTreeNode loadTree(SavedSessionTree stree, DefaultTreeModel treeModel, JTree tree) {
        DefaultMutableTreeNode rootNode = createNodeTree(stree.getFolder());
        DefaultMutableTreeNode emptyRoot = new DefaultMutableTreeNode(EMPTY_ROOT_NODE_USER_OBJET);
        String lastSelected = stree.getLastSelection();

        if (rootNode.getUserObject().toString().equals(EMPTY_ROOT_NODE_USER_OBJET)) {
            emptyRoot = rootNode;
        } else {
            rootNode.setAllowsChildren(true);
            emptyRoot.add(rootNode);
        }
        emptyRoot.setAllowsChildren(true);
        treeModel.setRoot(emptyRoot);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        try {
            if (lastSelected != null) {
                selectNode(lastSelected, rootNode, tree);
            } else {
                DefaultMutableTreeNode n = findFirstInfoNode(rootNode);
                if (n == null) {
                    // Handle there is no one in your list
                    n = createNewSiteAndAppendToParentNode(rootNode, rootNode, treeModel);
                    tree.scrollPathToVisible(new TreePath(n.getPath()));
                    TreePath path = new TreePath(n.getPath());
                    tree.setSelectionPath(path);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        treeModel.nodeChanged(rootNode);
        return emptyRoot;
    }

    public static boolean selectNode(String id, DefaultMutableTreeNode parent, JTree tree) {
        if (id != null && id.equals((((NamedItem) parent.getUserObject()).getId()))) {
            TreePath path = new TreePath(parent.getPath());
            tree.expandPath(path);
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
            return true;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (selectNode(id, child, tree)) {
                return true;
            }
        }
        return false;
    }

    private static DefaultMutableTreeNode findFirstInfoNode(DefaultMutableTreeNode node) {
        if (!node.getAllowsChildren()) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = findFirstInfoNode((DefaultMutableTreeNode) node.getChildAt(i));
            if (child != null) {
                return child;
            }
        }
        return null;
    }
    
    public static String createNewUuid(DefaultMutableTreeNode node) {
        // Step 1: Collect all existing UUIDs in the tree
        Set<String> existingUuids= new HashSet<>();
        collectUuids(node, existingUuids);
        return createNewUuid(existingUuids);
    }
    public static String createNewUuid(Set<String> existingUuids) {
        
        // Step 2: Generate a new UUID and ensure it's unique
        String newUuid;
        do {
            newUuid = UUID.randomUUID().toString();
        }
        while (existingUuids.contains(newUuid));

        return newUuid;
    }

    private static void collectUuids(DefaultMutableTreeNode node, Set<String> uuids) {
        // Add the current node's UUID
        if (node.getUserObject() instanceof NamedItem) {
            uuids.add(((NamedItem) node.getUserObject()).getId());
        }

        // Traverse all children and add their UUIDs
        Enumeration<TreeNode> children = node.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (child.getUserObject() instanceof SiteInfo) {
                uuids.add(((SiteInfo) child.getUserObject()).getId());
            } else if (child.getUserObject() instanceof SessionFolder) {
                uuids.add(((SessionFolder) child.getUserObject()).getId());
            }

            // Recursively collect UUIDs from child nodes
            collectUuids(child, uuids);
        }

    }
    
    private static void collectUuids(SessionFolder node, Set<String> uuids) {
        // Add the current node's UUID
        uuids.add(node.getId());
        
        // Traverse all children and add their UUIDs
        for(SessionFolder child: node.getFolders()){
            collectUuids(child, uuids);
        }
        
        for (SiteInfo child: node.getItems()){
            uuids.add( child.getId());
        }
    }

    public static DefaultMutableTreeNode createNewSiteAndAppendToParentNode(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode rootNode, DefaultTreeModel treeModel) {
        SiteInfo sessionInfo = new SiteInfo();
        sessionInfo.setName(getBundle().getString("app.sites.action.new_site"));
        sessionInfo.setId(createNewUuid(rootNode));
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(sessionInfo);
        childNode.setUserObject(sessionInfo);
        childNode.setAllowsChildren(false);
        treeModel.insertNodeInto(childNode, parentNode, parentNode.getChildCount());
        return childNode;
    }
    
    public static void assignNewUuidsToAll(SessionFolder folder, DefaultMutableTreeNode currentRootNode, Map<String, String> outOldToNew) {
        Set<String> currentUuids = new HashSet<>();
        collectUuids(currentRootNode, currentUuids);
        assignNewUuidsToFolderAndChilds(folder, currentUuids, outOldToNew);
    }
    
    private static void assignNewUuidsToFolderAndChilds(SessionFolder folder, Set<String> currentUuids, Map<String, String> outOldToNew) {
        // Add the current node's UUID
        String newUuid = createNewUuid(currentUuids);
        String oldUuid = folder.getId();
        folder.setId(newUuid);
        outOldToNew.put(oldUuid, newUuid);
        
        // Traverse all children and add their UUIDs
        for(SessionFolder child: folder.getFolders()){
            assignNewUuidsToFolderAndChilds(child, currentUuids, outOldToNew);
        }
        
        for (SiteInfo child: folder.getItems()){
            newUuid = createNewUuid(currentUuids);
            oldUuid = child.getId();
            child.setId(newUuid);
            outOldToNew.put(oldUuid, newUuid);
        }
        
    }
}
