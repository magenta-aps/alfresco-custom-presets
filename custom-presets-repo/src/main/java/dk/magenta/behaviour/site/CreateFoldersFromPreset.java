package dk.magenta.behaviour.site;

import dk.magenta.NodeExt;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.site.SiteServiceImpl;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.service.transaction.TransactionService;

import java.util.ArrayList;
import java.util.List;

import static dk.magenta.scripts.PresetGlobal.*;
import static org.alfresco.model.ContentModel.*;

public class CreateFoldersFromPreset implements NodeServicePolicies.OnCreateNodePolicy {

    // Dependencies
    private NodeService nodeService;
    private SiteService siteService;
    private TransactionService transactionService;
    private TaggingService taggingService;
    private PolicyComponent policyComponent;
    private FileFolderService fileFolderService;

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    public void setSiteService(SiteService siteService)
    {
        this.siteService = siteService;
    }
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }
    public void setTaggingService(TaggingService taggingService)
    {
        this.taggingService = taggingService;
    }
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    public void setFileFolderService(FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

    public void init() {

        policyComponent.bindClassBehaviour(
                NodeServicePolicies.OnCreateNodePolicy.QNAME,
                SiteModel.TYPE_SITE,
                new JavaBehaviour(this, "onCreateNode", NotificationFrequency.TRANSACTION_COMMIT));
    }

    public void onCreateNode(ChildAssociationRef childAssocRef) {

        NodeRef siteRef = childAssocRef.getChildRef();

        // Do not execute behaviour if this has been created in another store than workspace
        if (!siteRef.getStoreRef().equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)) {
            // This is not the spaces store - probably the archive store
            return;
        }

        // Only add preset folders if the site has been created
        if (nodeService.exists(siteRef)) {
            // Document Library for sites are created on first time a user enters it
            // Creating Document Library as SiteContainer to the new site
            SiteInfo siteInfo = siteService.getSite(siteRef);
            String presetId = siteInfo.getSitePreset();
            String siteName = siteInfo.getShortName();
            NodeRef documentLibrary = SiteServiceImpl.getSiteContainer(
                    siteName,
                    SiteService.DOCUMENT_LIBRARY,
                    true,
                    siteService,
                    transactionService,
                    taggingService);

            // Checks which folders have already been created
            List<String> existingFolders = new ArrayList<>();
            List<ChildAssociationRef> folderChildren = nodeService.getChildAssocs(documentLibrary);
            for (ChildAssociationRef folderChild:folderChildren) {
                NodeRef child = folderChild.getChildRef();
                existingFolders.add(nodeService.getProperty(child, PROP_NAME).toString());
            }

            // Get folders from the preset and add them to the new site
            List<NodeRef> source = NodeExt.getNodesByPath(new String[]{FOLDER_DATA_DICTIONARY, FOLDER_EXTENSION_PRESETS, FOLDER_FOLDER_SETUPS, "cm:" + presetId, "*"});
            for (NodeRef folder: source) {
                try {
                    String folderName = nodeService.getProperty(folder, PROP_NAME).toString();
                    // Only add folder if it does not already exist
                    if(!existingFolders.contains(folderName))
                        fileFolderService.copy(folder, documentLibrary, folderName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
