define(["dojo/_base/declare",
        "alfresco/core/Core",
        "dojo/_base/lang",
        "alfresco/core/CoreXhr",
        "service/constants/Default"],
    function(declare, Core, lang, CoreXhr, AlfConstants) {

        return declare([Core, CoreXhr], {

            constructor: function custom_preset_share_PresetService__constructor(args) {
                lang.mixin(this, args);
                this.alfSubscribe("GENERATE_PRESET", lang.hitch(this, this.generatePreset));
            },

            generatePreset: function custom_preset_share_PresetService__generatePreset(payload) {
                // Display 'generating' message
                Alfresco.util.PopupManager.displayMessage(
                    {
                        text: this.message('generate-preset.generating'),
                        spanClass: "wait",
                        displayTime: 0
                    });

                // Call Post Preset webscript
                this.serviceXhr({
                    url : AlfConstants.PROXY_URI + "custom-presets-repo/preset/" + payload.site + "/" + payload.presetName,
                    method: "POST",
                    site: payload.site,
                    successCallback: this.onGenerate,
                    callbackScope: this
                });
            },

            onGenerate: function custom_preset_share_PresetService__onGenerate(response, originalRequestConfig) {
                // On success display success message
                Alfresco.util.PopupManager.displayMessage(
                    {
                        text: this.message('generate-preset.generate-ok')
                    });
            }
        });
    });