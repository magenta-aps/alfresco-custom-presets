package dk.magenta;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import java.util.ArrayList;
import java.util.List;

import static dk.magenta.scripts.PresetGlobal.*;

public class NodeExt {

    private static SearchService searchService;

    public void setSearchService(SearchService searchService) {
        NodeExt.searchService = searchService;
    }

    private static String companyHomeQuery = convertList(new String[]{QUERY_PATH, FOLDER_COMPANY_HOME}) + "/";

    public static NodeRef getNodeByPath(String[] pathList) {
        String path = convertList(pathList);
        return getNodeByPath(path);
    }

    public static NodeRef getNodeByPath(String path) {
        return getNodeByQuery(companyHomeQuery + path + "\"");
    }

    public static NodeRef getNodeByQuery(String query){
        List<NodeRef> nodes = getNodesByQuery(query);
        if(nodes.isEmpty()) return null;
        return getNodesByQuery(query).get(0);
    }

    public static List<NodeRef> getNodesByPath(String[] pathList) {
        String path = convertList(pathList);
        return getNodesByPath(path);
    }

    public static List<NodeRef> getNodesByPath(String path) {
        return getNodesByQuery(companyHomeQuery + path + "\"");
    }

    public static List<NodeRef> getNodesByQuery(String[] pathList) {
        String path = convertList(pathList);
        return getNodesByQuery(path);
    }

    public static List<NodeRef> getNodesByQuery(String query){
        StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
        ResultSet rs = searchService.query(storeRef, SearchService.LANGUAGE_FTS_ALFRESCO, query);
        try
        {
            if (rs.length() == 0) return new ArrayList<NodeRef>();
            return rs.getNodeRefs();
        }
        finally
        {
            rs.close();
        }
    }

    private static String convertList(String[] pathList){
        String resultPath = "";
        for (String path : pathList) {
            if(!resultPath.equals(""))
                resultPath += "/";
            resultPath += path;
        }
        return resultPath;
    }

    public static List<NodeRef> getPresetXMLFiles() {
        //Get children of extension preset folder
        return NodeExt.getNodesByQuery(new String[]{companyHomeQuery, FOLDER_DATA_DICTIONARY, FOLDER_EXTENSION_PRESETS, "*\" AND TYPE:\"cm:content\""});
    }
}
