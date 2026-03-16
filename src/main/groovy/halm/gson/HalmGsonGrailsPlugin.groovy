package halm.gson

import grails.plugins.*

class HalmGsonGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "7.0 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "HAL Maker 4 GSON view" // Headline display name of the plugin
    def author = "Constantine Bialik"
    def authorEmail = "grails@helpchoice.com"
    def description = '''\
This plugin functionally replaces the standard extension of the GSON view for HAL support.
It allows to make GSON view file looks more like final HAL document and at the same time 
minimize amount of code developer must write. Also it makes sure unlike built-in GSON extension
that resulting document will be valid HAL document.

Finally by coping the code of the GSON view file (without model definition part) into Controller
developer can debug the view line-by-line to find and squash all bugs in it. 
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "https://github.com/C06A/HALM/wiki"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Exa Corp.", url: "http://exa.com" ]

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Tucker Pelletier", email: "tpelletier@exa.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "GitHub", url: "https://github.com/C06A/HALM/issues" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/C06A/HALM" ]

    Closure doWithSpring() { {->
            // TODO Implement runtime spring config (optional)
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
