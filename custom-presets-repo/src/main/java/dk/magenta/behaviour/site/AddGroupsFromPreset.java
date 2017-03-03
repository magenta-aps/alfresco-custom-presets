package dk.magenta.behaviour.site;

import dk.magenta.NodeExt;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static dk.magenta.scripts.PresetGlobal.*;

public class AddGroupsFromPreset implements NodeServicePolicies.OnCreateNodePolicy {

    // Dependencies
    private NodeService nodeService;
    private SiteService siteService;
    private PolicyComponent policyComponent;
    private ContentService contentService;
    private AuthorityService authorityService;

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    public void setSiteService(SiteService siteService)
    {
        this.siteService = siteService;
    }
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }
    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
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

            String query = QUERY_PATH + "/" + FOLDER_DATA_DICTIONARY + "/" + FOLDER_EXTENSION_PRESETS + "/" + "*\" AND title:\"" + presetId + "\"";
            List<NodeRef> presetList = NodeExt.getNodesByQuery(query);
            NodeRef presetNode = presetList.get(0);
            ContentReader contentReader = contentService.getReader(presetNode, ContentModel.PROP_CONTENT);
            InputStream componentsInputStream = contentReader.getContentInputStream();

            // Read components xml file
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document componentDoc = docBuilder.parse(componentsInputStream);
                NodeList authoritesList = componentDoc.getElementsByTagName("authorities");
                Element authoritesElement = (Element)authoritesList.item(0);
                NodeList authorities = authoritesElement.getElementsByTagName("authority");

                for (int i = 0; i < authorities.getLength(); i++) {
                    Node authorityTypeNode = authorities.item(i).getAttributes().getNamedItem("type");
                    String authorityType = authorityTypeNode.getNodeValue();
                    String authorityName = "GROUP_site_" + siteName + "_" + authorityType;
                    Element authorityElement = (Element)authorities.item(i);
                    NodeList members = authorityElement.getElementsByTagName("memberGroup");
                    for (int j = 0; j < members.getLength(); j++) {
                        String groupName = members.item(j).getTextContent();
                        authorityService.addAuthority(authorityName, groupName);
                    }
                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
