Ext.define( 'CMS.view.user.EditUserPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.editUserPanel',

    requires: [
        'CMS.view.user.EditUserPropertiesPanel',
        'CMS.view.user.EditUserPreferencesPanel',
        'CMS.view.user.EditUserMembershipPanel'
    ],

    autoScroll: true,

    defaults: {
        bodyPadding: 10
    },
    items: [],
    currentUser: '',
//    items: [
//        {
//            xtype: 'tabpanel',
//            region: 'center',
//            items: [
//                {
//                    xtype: 'editUserFormPanel',
//                    autoScroll: true
//                },
//                {
//                    xtype: 'editUserPropertiesPanel'
//                },
//                {
//                    xtype: 'editUserMembershipPanel'
//                },
//                {
//                    xtype: 'editUserPreferencesPanel'
//                }
//            ]
//        }
//
//    ],



    modal: true,

    layout: {
        type: 'border'
    },

    initComponent: function()
    {
        var me = this;
        var editUserFormPanel = {
            xtype: 'editUserFormPanel',
            userFields: [],
            autoScroll: true
        };
        var tabPanel = {
            xtype: 'tabpanel',
            region: 'center',
            items: [
                editUserFormPanel,
                {
                    xtype: 'editUserPropertiesPanel'
                },
                {
                    xtype: 'editUserMembershipPanel'
                },
                {
                    xtype: 'editUserPreferencesPanel'
                }
            ]
        };
        if ( this.userFields != null )
        {
            editUserFormPanel.userFields = this.userFields;
        }
        me.items = [ tabPanel ];
        this.callParent( arguments );

    }

} );

