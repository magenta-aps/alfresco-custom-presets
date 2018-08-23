<import resource="classpath:/alfresco/site-webscripts/org/alfresco/share/imports/share-header.lib.js">

var siteDropDown = widgetUtils.findObject(model.jsonModel, "id", "HEADER_SITE_CONFIGURATION_DROPDOWN");

// Add new 'Generate Preset'-button to site drop down list
if (siteDropDown == null) {
} else {
    var siteData = getSiteData();
    if (siteData != null) {
        if (user.isAdmin) {
            model.jsonModel.services.push("js/custom-presets-share/PresetService");
            // If the user is an admin then let them generate a preset from the site
            siteDropDown.config.widgets.push({
                id: "HEADER_GENERATE_PRESET",
                name: "alfresco/menus/AlfMenuItem",
                config: {
                    id: "HEADER_GENERATE_PRESET",
                    label: "generate-preset.label",
                    iconClass: "alf-cog-icon",
                    publishTopic: "ALF_CREATE_FORM_DIALOG_REQUEST",
                    publishPayload: {
                        dialogTitle: "generate-preset.label",
                        dialogConfirmationButtonTitle: "generate-preset.generate",
                        dialogCancellationButtonTitle: "generate-preset.cancel",
                        formSubmissionTopic: "GENERATE_PRESET",
                        formSubmissionPayloadMixin: {
                            site: page.url.templateArgs.site
                        },
                        fixedWidth: true,
                        widgets: [{
                            name: "alfresco/forms/controls/TextBox",
                            config: {
                                fieldId: "preset-name",
                                label: "generate-preset.name",
                                name: "presetName"
                            }
                        }]
                    }
                }
            });
        }
    }
}

// Make PresetsManager reload its presets
reloadPresets.reload();



var siteService = widgetUtils.findObject(model.jsonModel, "id", "SITE_SERVICE");
if (siteService && siteService.config)
{
    reloadPresets.reload();

    siteService.config.additionalSitePresets = new Array();

// Get list of preset names
    var connector = remote.connect("alfresco");
    var result = connector.get("/custom-presets-repo/preset/names");
    if (result.status == 200)
    {
        var names = (String)(result).split('\n');

        // Add all custom presets to the list of presets in Share
        for (var i = 0; i < names.length; i++) {
            if (names[i] == "")
                break;
            var name = names[i].replace(".xml", "");
            var id = name.toLowerCase().split(' ').join('-');
            siteService.config.additionalSitePresets.push({
                label: id,
                value: name
            });
        }
    }

}