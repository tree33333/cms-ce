Ext.define( 'App.controller.SystemCacheController', {
    extend: 'Ext.app.Controller',

    stores: ['SystemCacheStore'],
    models: ['SystemCacheModel'],
    views: ['GridPanel', 'DetailPanel'],

    init: function()
    {
        this.control( {
          'systemCacheGrid': {
              itemclick: this.updateDetail
          },
          '*[action=clearCache]': {
              click: this.clearCache
          },
          '*[action=clearStats]': {
              click: this.clearStats
          },
          '*[action=refreshCache]': {
              toggle: this.toggleRefreshDetail
          }
        } );

        this.toggleRefreshDetail( null, true, {} );
    },

    clearCache: function( btn, evt, opts ) {
        var me = this;
        Ext.Msg.confirm("Warning", "All cache will be erased. Are you sure ?", function( btn ) {
            if ( "yes" == btn ) {
                //TODO clear cache
                me.getGrid().getStore().load();
                me.getDetail().clearDetail();
            }
        })
    },

    clearStats: function( btn, evt, opts ) {
        var me = this;
        Ext.Msg.confirm("Warning", "All statistics will be erased. Are you sure ?", function( btn ) {
            if ( "yes" == btn ) {
                //TODO clear stats
                me.getGrid().getStore().load();
                me.getDetail().clearDetail();
            }
        })
    },

    toggleRefreshDetail: function( btn, pressed, opts ) {
        var me = this;
        if( !this.refreshTask ) {
            this.refreshTask = {
                run: function(){
                    me.getDetail().updateDetail();
                },
                interval: 1000
            };
        }
        if ( pressed ) {
            Ext.TaskManager.start( this.refreshTask );
        } else {
            Ext.TaskManager.stop( this.refreshTask );
        }
    },

    updateDetail: function( view, record, item, index, evt, opts ) {
        this.getDetail().updateDetail( record );
    },

    getGrid: function() {
        return Ext.ComponentQuery.query('systemCacheGrid')[0];
    },

    getDetail: function() {
        return Ext.ComponentQuery.query('systemCacheDetail')[0];
    },

    getShow: function() {
        return Ext.ComponentQuery.query('systemCacheShow')[0];
    }

} );