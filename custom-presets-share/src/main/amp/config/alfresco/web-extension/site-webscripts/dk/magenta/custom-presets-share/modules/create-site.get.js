// Make PresetsManager reload its presets
reloadPresets.reload();

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
        model.sitePresets.push({
            id: id,
            name: name
        });
    }
}