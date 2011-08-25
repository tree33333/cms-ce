Ext.define('CMS.store.UserstoreConnectorStore', {
    extend: 'Ext.data.Store',
    model: 'CMS.model.UserstoreConnectorModel',

    autoLoad: true,

    proxy: {
        type: 'ajax',
        url: '/admin/data/userstore/connectors',
        simpleSortMode: true,
        reader: {
            type: 'json',
            root: 'userStoreConnectors'
        }
    }
});
