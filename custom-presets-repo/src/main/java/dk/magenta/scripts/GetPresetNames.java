package dk.magenta.scripts;

import dk.magenta.NodeExt;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.extensions.webscripts.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.alfresco.model.ContentModel.*;

public class GetPresetNames extends DeclarativeWebScript {
    private NodeService nodeService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    // Gets a list of names of all custom presets
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        //Get children of extension preset folder
        List<NodeRef> presetExtensionNodes = NodeExt.getPresetXMLFiles();

        // Set up return model
        Map<String, Object> model = new HashMap<>();

        // For each preset in extension preset folder add name of file to a string separated with line break '\n'
        String presetNames = "";
        for(NodeRef presetNode : presetExtensionNodes) {
            if(!presetNames.equals(""))
                presetNames += "\n";
            presetNames += nodeService.getProperty(presetNode, PROP_NAME).toString();
        }

        // Respond with the string containing the file names
        model.put("presetNames", presetNames);
        return model;
    }
}