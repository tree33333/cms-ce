Ext.define( 'CMS.controller.SystemController', {
    extend: 'Ext.app.Controller',

    stores: [],
    models: [],
    views: [],

    refs: [
        {ref: 'tabPanel', selector: 'cmsTabPanel'},
        {ref: 'systemNavigation', selector: 'systemNavigation'}
    ],

    init: function()
    {
        this.control({
            'cmsTabPanel': {
                afterrender: this.selectDefaultApplication
            },
            'systemNavigation': {
                itemclick: this.selectApplication
            }
         });
    },

    selectDefaultApplication: function( cmp, options ) {
        var nav = cmp.down('systemNavigation');
        if ( nav ) {
            var first = nav.getRootNode().firstChild;
            nav.getSelectionModel().select( first );
            this.selectApplication(null, first);
        }
    },

    selectApplication: function( view, record, item, index, evt, opts ) {
        var iframe = Ext.getDom('system-iframe');
        if (iframe && record)
            iframe.src = record.data.appUrl;
    }

} );