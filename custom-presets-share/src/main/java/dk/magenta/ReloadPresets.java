package dk.magenta;

import org.springframework.extensions.webscripts.processor.BaseProcessorExtension;

/**
 * This class is a proxy for CustomPresetsManager.
 * It extends BaseProcessorExtension in order to expose CustomPresetsManager as root object.
 */
public class ReloadPresets extends BaseProcessorExtension {

    private CustomPresetsManager customPresetsManager;

    public void reload()
    {
        customPresetsManager.reloadPresets();
    }

    public void setCustomPresetsManager(CustomPresetsManager customPresetsManager) {
        this.customPresetsManager = customPresetsManager;
    }
}
