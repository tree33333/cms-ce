Ext.define('CMS.view.ShowPanel', {
    extend: 'Ext.panel.Panel',
    alias : 'widget.userstoreShow',

    layout: 'border',
    border: false,
    padding: 0,

    initComponent: function() {
        this.items = [
            {
                region: 'center',
                id: 'userstoreGridID',
                xtype: 'userstoreGrid',
                flex: 2
            },
            {
                region: 'south',
                xtype: 'userstoreDetail',
                flex: 1
            }
        ];

        this.callParent(arguments);
    }

});
