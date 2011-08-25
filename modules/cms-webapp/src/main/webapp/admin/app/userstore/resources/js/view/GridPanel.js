Ext.define( 'CMS.view.GridPanel', {
    extend: 'Ext.grid.Panel',
    alias : 'widget.userstoreGrid',

    requires: [
        'Shared.PageSizePlugin'
    ],
    layout: 'fit',
    loadMask: true,
    columnLines: true,
    frame: false,
    store: 'UserstoreConfigStore',
    styleHtmlContent: true,

    initComponent: function()
    {
        this.columns = [
            {
                text: 'Display Name',
                dataIndex: 'name',
                sortable: true,
                renderer: this.nameRenderer,
                flex: 1
            },
            {
                text: 'Last Modified',
                xtype: 'datecolumn',
                dataIndex: 'lastModified',
                format: 'Y-m-d h:m',
                sortable: true
            }
        ];

        this.tbar = {
            xtype: 'toolbar',
            items: [
                {
                    text: 'New Userstore',
                    iconCls: 'icon-new',
                    action: 'newUserstore'
                }
            ]
        };

        this.bbar = {
            xtype: 'pagingtoolbar',
            store: this.store,
            displayInfo: true,
            displayMsg: 'Displaying userstores {0} - {1} of {2}',
            emptyMsg: 'No userstores to display',
            plugins: ['pageSize']
        };

        this.viewConfig = {
            trackOver : true,
            stripeRows: true
        };

        this.callParent( arguments );
    },

    nameRenderer: function( value, p, record )
    {
        return Ext.String.format(
                '<h2 style="margin: 0;">{0}</h2><em>{1}</em>',
                value,
                record.data.name
                );
    }
});
