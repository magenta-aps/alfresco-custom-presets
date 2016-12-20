package dk.magenta.scripts;

import dk.magenta.NodeExt;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.dom4j.DocumentFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.extensions.surf.exception.PlatformRuntimeException;
import org.springframework.extensions.webscripts.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetPresets extends DeclarativeWebScript {
    private ContentService contentService;

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    // Search for our custom preset XML files
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        // Create new presets document with root element
        DocumentFactory documentFactory = DocumentFactory.getInstance();
        SAXReader xmlReader = new SAXReader();
        xmlReader.setDocumentFactory(documentFactory);
        Document resultDoc = documentFactory.createDocument();
        Element presetsRootElement = documentFactory.createElement("presets");

        //Get children of extension preset folder
        List<NodeRef> presetExtensionNodes = NodeExt.getPresetXMLFiles();

        // For each store in search path find all files
        for (NodeRef presetNode : presetExtensionNodes) {
            try {
                // Parse each file and then add the documents to root element of xml document
                ContentReader contentReader = contentService.getReader(presetNode, ContentModel.PROP_CONTENT);
                InputStream componentsInputStream = contentReader.getContentInputStream();
                Document doc = xmlReader.read(componentsInputStream);
                List<Element> presets = doc.getRootElement().elements();
                for (Element preset : presets)
                    presetsRootElement.add(preset.detach());
            } catch (DocumentException e) {
                throw new PlatformRuntimeException("Error processing presets XML file: " + presetNode, e);
            }
        }
        // Add root element to result preset file
        resultDoc.add(presetsRootElement);

        // Respond with result preset file
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("resultDoc", resultDoc.asXML());
        return model;
    }
}