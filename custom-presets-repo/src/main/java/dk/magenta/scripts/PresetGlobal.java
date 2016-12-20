package dk.magenta.scripts;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

public interface PresetGlobal {
    String QUERY_PATH = "PATH:\"/";
    String FOLDER_COMPANY_HOME = "app:company_home";
    String FOLDER_DATA_DICTIONARY = "app:dictionary";
    String FOLDER_SITES = "st:sites";
    String FOLDER_DOCUMENT_LIBRARY = "cm:documentLibrary";

    String FOLDER_EXTENSION_PRESETS_NAME = "Extension Presets";
    String FOLDER_EXTENSION_PRESETS_SMALL = FOLDER_EXTENSION_PRESETS_NAME.toLowerCase().replace(" ", "");
    String FOLDER_EXTENSION_PRESETS = "cm:" + FOLDER_EXTENSION_PRESETS_SMALL;
    QName FOLDER_EXTENSION_PRESETS_QNAME = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, FOLDER_EXTENSION_PRESETS_SMALL);

    String FOLDER_FOLDER_SETUPS_NAME = "Folder Setups";
    String FOLDER_FOLDER_SETUPS_SMALL = FOLDER_FOLDER_SETUPS_NAME.toLowerCase().replace(" ", "");
    String FOLDER_FOLDER_SETUPS = "cm:" + FOLDER_FOLDER_SETUPS_SMALL;
    QName FOLDER_FOLDER_SETUPS_QNAME = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, FOLDER_FOLDER_SETUPS_SMALL);
}
