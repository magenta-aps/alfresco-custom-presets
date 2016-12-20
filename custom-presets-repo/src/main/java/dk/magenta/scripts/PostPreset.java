package dk.magenta.scripts;

import dk.magenta.NodeExt;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;

import static org.alfresco.model.ContentModel.*;
import static dk.magenta.scripts.PresetGlobal.*;
import static org.alfresco.service.namespace.NamespaceService.*;

public class PostPreset extends DeclarativeWebScript {

    private ContentService contentService;
    private NodeService nodeService;
    private FileFolderService fileFolderService;
    private NodeRef extensionPresetsFolder;
    private NodeRef folderSetupsFolder;

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    public void setFileFolderService(FileFolderService fileFolderService) { this.fileFolderService = fileFolderService; }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        getExtensionPresetFolder();
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

        // Get the parameter "site"
        Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
        String siteName = templateArgs.get("site");
        String presetName = templateArgs.get("presetName");
        String presetId = presetName.replace(' ', '-');

        // Find current site
        NodeRef siteNode = NodeExt.getNodeByPath("st:sites/cm:" + siteName);

        //Find surf-config folder in current site
        NodeRef surfConfigNode = NodeExt.getNodeByQuery("TYPE:\"cm:folder\" AND PARENT:\"" + siteNode + "\" AND ASPECT:\"sys:hidden\"");

        if (surfConfigNode != null) try {

            List<String> pagePaths = new ArrayList<>();
            // Get Components and Pages folders from surf-config folder
            List<ChildAssociationRef> surfConfigChildren = nodeService.getChildAssocs(surfConfigNode);
            NodeRef componentsNode = null;
            NodeRef pagesNode = null;
            for (ChildAssociationRef surfConfigChild : surfConfigChildren) {
                NodeRef childRef = surfConfigChild.getChildRef();
                if (nodeService.getProperty(childRef, PROP_NAME).equals("components"))
                    componentsNode = childRef;
                else if (nodeService.getProperty(childRef, PROP_NAME).equals("pages"))
                    pagesNode = childRef;
            }

            // Create DocumentBuilder
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

            // Create xml structure for presets.xml
            Document presetDoc = docBuilder.newDocument();
            Element presetsElement = presetDoc.createElement("presets");
            Element presetElement = presetDoc.createElement("preset");
            presetId = presetId.toLowerCase();
            presetElement.setAttribute("id", presetId);
            Element componentsElement = presetDoc.createElement("components");
            presetDoc.appendChild(presetsElement);
            presetsElement.appendChild(presetElement);
            presetElement.appendChild(componentsElement);

            if (componentsNode != null) {
                // Get components xml files
                List<ChildAssociationRef> componentsChildren = nodeService.getChildAssocs(componentsNode);

                for (ChildAssociationRef componentChild : componentsChildren) {
                    // Get input stream from component xml file
                    NodeRef componentNode = componentChild.getChildRef();
                    ContentReader contentReader = contentService.getReader(componentNode, ContentModel.PROP_CONTENT);
                    InputStream componentsInputStream = contentReader.getContentInputStream();

                    // Read components xml file
                    Document componentDoc = docBuilder.parse(componentsInputStream);

                    // Get the component node and remove guid node
                    Node component = componentDoc.getDocumentElement();
                    Boolean hasScope = false;
                    for (int i = 0; i < component.getChildNodes().getLength(); i++) {

                        String nodeName = component.getChildNodes().item(i).getNodeName();

                        switch (nodeName) {
                            case "guid":
                                component.removeChild(component.getChildNodes().item(i).getNextSibling());
                                component.removeChild(component.getChildNodes().item(i));
                                // Reset i to the current element
                                i--;
                                break;
                            case "scope":
                                hasScope = true;
                                break;
                            case "source-id":
                                String sourceIdText = component.getChildNodes().item(i).getTextContent();
                                if (!pagePaths.contains(sourceIdText))
                                    pagePaths.add(sourceIdText);
                                String changedSourceIdText = sourceIdText.replace(siteName, "${siteid}");
                                component.getChildNodes().item(i).setTextContent(changedSourceIdText);
                                break;
                        }
                    }

                    // Imports the node to presetDoc is needed to append it
                    Node importedNode = presetDoc.importNode(component, true);

                    // Adds scope if not present
                    if (!hasScope) {
                        Node firstNode = importedNode.getFirstChild();
                        Element scopeElement = presetDoc.createElement("scope");
                        scopeElement.setTextContent("page");
                        importedNode.insertBefore(scopeElement, firstNode);
                    }

                    // Append node from components file to presets.xml
                    componentsElement.appendChild(importedNode);

                    // Close input stream
                    componentsInputStream.close();
                }
            }
            if (pagesNode != null) {

                Element pagesElement = presetDoc.createElement("pages");
                presetElement.appendChild(pagesElement);

                // Get pages xml files IDEA: iterates through source-ids to find relevant files to copy
                HashMap<NodeRef, String> pageNodes = new HashMap<NodeRef, String>();
                for (String path : pagePaths) {
                    NodeRef pageNode = getPages(pagesNode, path);
                    pageNodes.put(pageNode, path);
                }
                Set<NodeRef> nodeRefs = pageNodes.keySet();
                for(NodeRef pageNode : nodeRefs){
                    ContentReader contentReader = contentService.getReader(pageNode, ContentModel.PROP_CONTENT);
                    InputStream pagesInputStream = contentReader.getContentInputStream();

                    // Read pages xml file
                    Document pageDoc = docBuilder.parse(pagesInputStream);

                    // Get the page node
                    Element page = pageDoc.getDocumentElement();

                    // Add id attribute
                    String changedIdText = pageNodes.get(pageNode).replace(siteName, "${siteid}");
                    page.setAttribute("id", changedIdText);

                    // Imports the node to presetDoc: is needed to append it
                    Node importedNode = presetDoc.importNode(page, true);

                    // Append node from components file to presets.xml
                    pagesElement.appendChild(importedNode);

                    // Close input stream
                    pagesInputStream.close();
                }
            }

            //Create new preset file
            OutputStream presetInputStream = createNewPresetFile(extensionPresetsFolder, presetName);

            // Write to new preset file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult result = new StreamResult(presetInputStream);
            DOMSource source = new DOMSource(presetDoc);
            transformer.transform(source, result);
            presetInputStream.close();

        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            e.printStackTrace();
        }

        createDocumentLibraryTemplate(presetName, presetId, siteName);

        // Respond with success
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("Status", "Success");
        return model;
    }


    private void createDocumentLibraryTemplate(String presetName, String presetId, String siteName) {
        Map<QName,Serializable> properties = new HashMap<>();
        properties.put(PROP_NAME, presetName);
        NodeRef parent = nodeService.createNode(folderSetupsFolder, ASSOC_CONTAINS, QName.createQName(CONTENT_MODEL_1_0_URI, presetId), TYPE_FOLDER, properties).getChildRef();
        List<NodeRef> source = NodeExt.getNodesByPath(new String[]{FOLDER_SITES, "cm:" + siteName, FOLDER_DOCUMENT_LIBRARY, "*"});
        for (NodeRef folder: source) {
            try {
                fileFolderService.copy(folder, parent, nodeService.getProperty(folder, PROP_NAME).toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private OutputStream createNewPresetFile(NodeRef parent, String presetName)
    {
        String fileName = presetName + ".xml";
        Map<QName,Serializable> properties = new HashMap<QName,Serializable>();
        properties.put(PROP_NAME, fileName);
        NodeRef presetNode = nodeService.createNode(parent, ASSOC_CONTAINS, QName.createQName(CONTENT_MODEL_1_0_URI, fileName), TYPE_CONTENT, properties).getChildRef();
        ContentWriter contentWriter = contentService.getWriter(presetNode, ContentModel.PROP_CONTENT, true);
        contentWriter.setMimetype(MimetypeMap.MIMETYPE_XML);
        return contentWriter.getContentOutputStream();
    }

    private NodeRef getPages(NodeRef parentNode, String path) {
        // Iterate through the path until the xml file is reached
        List<ChildAssociationRef> children = nodeService.getChildAssocs(parentNode);
        for (ChildAssociationRef child : children) {
            NodeRef childNode = child.getChildRef();
            String folderName = path.split("/")[0];
            if (nodeService.getProperty(childNode, PROP_NAME).equals(folderName)) {
                if (folderName.endsWith(".xml"))
                    return childNode;
                String newPath = path.substring(folderName.length() + 1);
                if (!newPath.contains("/"))
                    newPath += ".xml";
                return getPages(childNode, newPath);
            }
        }
        return null;
    }

    private void getExtensionPresetFolder()
    {
        // Get NodeRef for Extension Presets folder
        extensionPresetsFolder = NodeExt.getNodeByPath(new String[]{FOLDER_DATA_DICTIONARY, FOLDER_EXTENSION_PRESETS});
        if(extensionPresetsFolder == null) {

            // Get NodeRef for Data Dictionary folder
            NodeRef dataDictionaryNode = NodeExt.getNodeByPath(FOLDER_DATA_DICTIONARY);

            // Create Extension Presets Folder
            Map<QName,Serializable> properties = new HashMap<>();
            properties.put(PROP_NAME, FOLDER_EXTENSION_PRESETS_NAME);
            extensionPresetsFolder = nodeService.createNode(dataDictionaryNode, ASSOC_CONTAINS, FOLDER_EXTENSION_PRESETS_QNAME, TYPE_FOLDER, properties).getChildRef();
        }

        // Get NodeRef for Folder Setup folder
        folderSetupsFolder = NodeExt.getNodeByPath(new String[]{FOLDER_DATA_DICTIONARY, FOLDER_EXTENSION_PRESETS, FOLDER_FOLDER_SETUPS});
        if(folderSetupsFolder == null) {
            // Create folder for folder setups for presets
            Map<QName,Serializable> properties = new HashMap<>();
            properties.put(PROP_NAME, FOLDER_FOLDER_SETUPS_NAME);
            folderSetupsFolder = nodeService.createNode(extensionPresetsFolder, ASSOC_CONTAINS, FOLDER_FOLDER_SETUPS_QNAME, TYPE_FOLDER, properties).getChildRef();
        }
    }
}