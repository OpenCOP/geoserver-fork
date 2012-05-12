/*

Copyright (c) 2008-2010, The Open Source Geospatial Foundation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Open Source Geospatial Foundation nor the names
      of its contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

Ext.namespace("GeoExt");GeoExt.Action=Ext.extend(Ext.Action,{control:null,map:null,uScope:null,uHandler:null,uToggleHandler:null,uCheckHandler:null,constructor:function(config){this.uScope=config.scope;this.uHandler=config.handler;this.uToggleHandler=config.toggleHandler;this.uCheckHandler=config.checkHandler;config.scope=this;config.handler=this.pHandler;config.toggleHandler=this.pToggleHandler;config.checkHandler=this.pCheckHandler;var ctrl=this.control=config.control;delete config.control;if(ctrl){if(config.map){config.map.addControl(ctrl);delete config.map;}
if((config.pressed||config.checked)&&ctrl.map){ctrl.activate();}
ctrl.events.on({activate:this.onCtrlActivate,deactivate:this.onCtrlDeactivate,scope:this});}
arguments.callee.superclass.constructor.call(this,config);},pHandler:function(cmp){var ctrl=this.control;if(ctrl&&ctrl.type==OpenLayers.Control.TYPE_BUTTON){ctrl.trigger();}
if(this.uHandler){this.uHandler.apply(this.uScope,arguments);}},pToggleHandler:function(cmp,state){this.changeControlState(state);if(this.uToggleHandler){this.uToggleHandler.apply(this.uScope,arguments);}},pCheckHandler:function(cmp,state){this.changeControlState(state);if(this.uCheckHandler){this.uCheckHandler.apply(this.uScope,arguments);}},changeControlState:function(state){if(state){if(!this._activating){this._activating=true;this.control.activate();this._activating=false;}}else{if(!this._deactivating){this._deactivating=true;this.control.deactivate();this._deactivating=false;}}},onCtrlActivate:function(){var ctrl=this.control;if(ctrl.type==OpenLayers.Control.TYPE_BUTTON){this.enable();}else{this.safeCallEach("toggle",[true]);this.safeCallEach("setChecked",[true]);}},onCtrlDeactivate:function(){var ctrl=this.control;if(ctrl.type==OpenLayers.Control.TYPE_BUTTON){this.disable();}else{this.safeCallEach("toggle",[false]);this.safeCallEach("setChecked",[false]);}},safeCallEach:function(fnName,args){var cs=this.items;for(var i=0,len=cs.length;i<len;i++){if(cs[i][fnName]){cs[i].rendered?cs[i][fnName].apply(cs[i],args):cs[i].on({"render":cs[i][fnName].createDelegate(cs[i],args),single:true});}}}});
Ext.namespace("GeoExt");GeoExt.MapPanel=Ext.extend(Ext.Panel,{map:null,layers:null,center:null,zoom:null,prettyStateKeys:false,extent:null,stateEvents:["aftermapmove","afterlayervisibilitychange","afterlayeropacitychange"],initComponent:function(){if(!(this.map instanceof OpenLayers.Map)){this.map=new OpenLayers.Map(Ext.applyIf(this.map||{},{allOverlays:true}));}
var layers=this.layers;if(!layers||layers instanceof Array){this.layers=new GeoExt.data.LayerStore({layers:layers,map:this.map.layers.length>0?this.map:null});}
if(typeof this.center=="string"){this.center=OpenLayers.LonLat.fromString(this.center);}else if(this.center instanceof Array){this.center=new OpenLayers.LonLat(this.center[0],this.center[1]);}
if(typeof this.extent=="string"){this.extent=OpenLayers.Bounds.fromString(this.extent);}else if(this.extent instanceof Array){this.extent=OpenLayers.Bounds.fromArray(this.extent);}
GeoExt.MapPanel.superclass.initComponent.call(this);this.addEvents("aftermapmove","afterlayervisibilitychange","afterlayeropacitychange");this.map.events.on({"moveend":this.onMoveend,"changelayer":this.onLayerchange,scope:this});},onMoveend:function(){this.fireEvent("aftermapmove");},onLayerchange:function(e){if(e.property){if(e.property==="visibility"){this.fireEvent("afterlayervisibilitychange");}else if(e.property==="opacity"){this.fireEvent("afterlayeropacitychange");}}},applyState:function(state){this.center=new OpenLayers.LonLat(state.x,state.y);this.zoom=state.zoom;var i,l,layer,layerId,visibility,opacity;var layers=this.map.layers;for(i=0,l=layers.length;i<l;i++){layer=layers[i];layerId=this.prettyStateKeys?layer.name:layer.id;visibility=state["visibility_"+layerId];if(visibility!==undefined){visibility=(/^true$/i).test(visibility);if(layer.isBaseLayer){if(visibility){this.map.setBaseLayer(layer);}}else{layer.setVisibility(visibility);}}
opacity=state["opacity_"+layerId];if(opacity!==undefined){layer.setOpacity(opacity);}}},getState:function(){var state;if(!this.map){return;}
var center=this.map.getCenter();state={x:center.lon,y:center.lat,zoom:this.map.getZoom()};var i,l,layer,layerId,layers=this.map.layers;for(i=0,l=layers.length;i<l;i++){layer=layers[i];layerId=this.prettyStateKeys?layer.name:layer.id;state["visibility_"+layerId]=layer.getVisibility();state["opacity_"+layerId]=layer.opacity==null?1:layer.opacity;}
return state;},updateMapSize:function(){if(this.map){this.map.updateSize();}},renderMap:function(){var map=this.map;map.render(this.body.dom);this.layers.bind(map);if(map.layers.length>0){if(this.center||this.zoom!=null){map.setCenter(this.center,this.zoom);}else if(this.extent){map.zoomToExtent(this.extent);}else{map.zoomToMaxExtent();}}},afterRender:function(){GeoExt.MapPanel.superclass.afterRender.apply(this,arguments);if(!this.ownerCt){this.renderMap();}else{this.ownerCt.on("move",this.updateMapSize,this);this.ownerCt.on({"afterlayout":{fn:this.renderMap,scope:this,single:true}});}},onResize:function(){GeoExt.MapPanel.superclass.onResize.apply(this,arguments);this.updateMapSize();},onBeforeAdd:function(item){if(typeof item.addToMapPanel==="function"){item.addToMapPanel(this);}
GeoExt.MapPanel.superclass.onBeforeAdd.apply(this,arguments);},remove:function(item,autoDestroy){if(typeof item.removeFromMapPanel==="function"){item.removeFromMapPanel(this);}
GeoExt.MapPanel.superclass.remove.apply(this,arguments);},beforeDestroy:function(){if(this.ownerCt){this.ownerCt.un("move",this.updateMapSize,this);}
if(this.map&&this.map.events){this.map.events.un({"moveend":this.onMoveend,"changelayer":this.onLayerchange,scope:this});}
if(!this.initialConfig.map||!(this.initialConfig.map instanceof OpenLayers.Map)){if(this.map&&this.map.destroy){this.map.destroy();}}
delete this.map;GeoExt.MapPanel.superclass.beforeDestroy.apply(this,arguments);}});GeoExt.MapPanel.guess=function(){return Ext.ComponentMgr.all.find(function(o){return o instanceof GeoExt.MapPanel;});};Ext.reg('gx_mappanel',GeoExt.MapPanel);
Ext.namespace('GeoExt');GeoExt.FeatureRenderer=Ext.extend(Ext.BoxComponent,{feature:undefined,symbolizers:[OpenLayers.Feature.Vector.style["default"]],symbolType:"Polygon",resolution:1,minWidth:20,minHeight:20,renderers:["SVG","VML","Canvas"],rendererOptions:null,pointFeature:undefined,lineFeature:undefined,polygonFeature:undefined,renderer:null,initComponent:function(){GeoExt.FeatureRenderer.superclass.initComponent.apply(this,arguments);Ext.applyIf(this,{pointFeature:new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Point(0,0)),lineFeature:new OpenLayers.Feature.Vector(new OpenLayers.Geometry.LineString([new OpenLayers.Geometry.Point(-8,-3),new OpenLayers.Geometry.Point(-3,3),new OpenLayers.Geometry.Point(3,-3),new OpenLayers.Geometry.Point(8,3)])),polygonFeature:new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([new OpenLayers.Geometry.LinearRing([new OpenLayers.Geometry.Point(-8,-4),new OpenLayers.Geometry.Point(-6,-6),new OpenLayers.Geometry.Point(6,-6),new OpenLayers.Geometry.Point(8,-4),new OpenLayers.Geometry.Point(8,4),new OpenLayers.Geometry.Point(6,6),new OpenLayers.Geometry.Point(-6,6),new OpenLayers.Geometry.Point(-8,4)])]))});if(!this.feature){this.setFeature(null,{draw:false});}
this.addEvents("click");},initCustomEvents:function(){this.clearCustomEvents();this.el.on("click",this.onClick,this);},clearCustomEvents:function(){if(this.el&&this.el.removeAllListeners){this.el.removeAllListeners();}},onClick:function(){this.fireEvent("click",this);},onRender:function(ct,position){if(!this.el){this.el=document.createElement("div");this.el.id=this.getId();}
if(!this.renderer||!this.renderer.supported()){this.assignRenderer();}
this.renderer.map={getResolution:(function(){return this.resolution;}).createDelegate(this)};GeoExt.FeatureRenderer.superclass.onRender.apply(this,arguments);this.drawFeature();},afterRender:function(){GeoExt.FeatureRenderer.superclass.afterRender.apply(this,arguments);this.initCustomEvents();},onResize:function(w,h){this.setRendererDimensions();GeoExt.FeatureRenderer.superclass.onResize.apply(this,arguments);},setRendererDimensions:function(){var gb=this.feature.geometry.getBounds();var gw=gb.getWidth();var gh=gb.getHeight();var resolution=this.initialConfig.resolution;if(!resolution){resolution=Math.max(gw/this.width||0,gh/this.height||0)||1;}
this.resolution=resolution;var width=Math.max(this.width||this.minWidth,gw/resolution);var height=Math.max(this.height||this.minHeight,gh/resolution);var center=gb.getCenterPixel();var bhalfw=width*resolution/2;var bhalfh=height*resolution/2;var bounds=new OpenLayers.Bounds(center.x-bhalfw,center.y-bhalfh,center.x+bhalfw,center.y+bhalfh);this.renderer.setSize(new OpenLayers.Size(Math.round(width),Math.round(height)));this.renderer.setExtent(bounds,true);},assignRenderer:function(){for(var i=0,len=this.renderers.length;i<len;++i){var Renderer=OpenLayers.Renderer[this.renderers[i]];if(Renderer&&Renderer.prototype.supported()){this.renderer=new Renderer(this.el,this.rendererOptions);break;}}},setSymbolizers:function(symbolizers,options){this.symbolizers=symbolizers;if(!options||options.draw){this.drawFeature();}},setSymbolType:function(type,options){this.symbolType=type;this.setFeature(null,options);},setFeature:function(feature,options){this.feature=feature||this[this.symbolType.toLowerCase()+"Feature"];if(!options||options.draw){this.drawFeature();}},drawFeature:function(){this.renderer.clear();this.setRendererDimensions();var Symbolizer=OpenLayers.Symbolizer;var Text=Symbolizer&&Symbolizer.Text;var symbolizer,feature,geomType;for(var i=0,len=this.symbolizers.length;i<len;++i){symbolizer=this.symbolizers[i];feature=this.feature;if(!Text||!(symbolizer instanceof Text)){if(Symbolizer&&(symbolizer instanceof Symbolizer)){symbolizer=symbolizer.clone();if(!this.initialConfig.feature){geomType=symbolizer.CLASS_NAME.split(".").pop().toLowerCase();feature=this[geomType+"Feature"];}}else{symbolizer=Ext.apply({},symbolizer);}
this.renderer.drawFeature(feature.clone(),symbolizer);}}},update:function(options){options=options||{};if(options.feature){this.setFeature(options.feature,{draw:false});}else if(options.symbolType){this.setSymbolType(options.symbolType,{draw:false});}
if(options.symbolizers){this.setSymbolizers(options.symbolizers,{draw:false});}
this.drawFeature();},beforeDestroy:function(){this.clearCustomEvents();if(this.renderer){this.renderer.destroy();}}});Ext.reg('gx_renderer',GeoExt.FeatureRenderer);
Ext.namespace('GeoExt.grid');GeoExt.grid.FeatureSelectionModelMixin=function(){return{autoActivateControl:true,layerFromStore:true,selectControl:null,bound:false,superclass:null,constructor:function(config){config=config||{};if(config.selectControl instanceof OpenLayers.Control.SelectFeature){if(!config.singleSelect){var ctrl=config.selectControl;config.singleSelect=!(ctrl.multiple||!!ctrl.multipleKey);}}else if(config.layer instanceof OpenLayers.Layer.Vector){this.selectControl=this.createSelectControl(config.layer,config.selectControl);delete config.layer;delete config.selectControl;}
this.superclass=arguments.callee.superclass;this.superclass.constructor.call(this,config);},initEvents:function(){this.superclass.initEvents.call(this);if(this.layerFromStore){var layer=this.grid.getStore()&&this.grid.getStore().layer;if(layer&&!(this.selectControl instanceof OpenLayers.Control.SelectFeature)){this.selectControl=this.createSelectControl(layer,this.selectControl);}}
if(this.selectControl){this.bind(this.selectControl);}},createSelectControl:function(layer,config){config=config||{};var singleSelect=config.singleSelect!==undefined?config.singleSelect:this.singleSelect;config=OpenLayers.Util.extend({toggle:true,multipleKey:singleSelect?null:(Ext.isMac?"metaKey":"ctrlKey")},config);var selectControl=new OpenLayers.Control.SelectFeature(layer,config);layer.map.addControl(selectControl);return selectControl;},bind:function(obj,options){if(!this.bound){options=options||{};this.selectControl=obj;if(obj instanceof OpenLayers.Layer.Vector){this.selectControl=this.createSelectControl(obj,options.controlConfig);}
if(this.autoActivateControl){this.selectControl.activate();}
var layers=this.getLayers();for(var i=0,len=layers.length;i<len;i++){layers[i].events.on({featureselected:this.featureSelected,featureunselected:this.featureUnselected,scope:this});}
this.on("rowselect",this.rowSelected,this);this.on("rowdeselect",this.rowDeselected,this);this.bound=true;}
return this.selectControl;},unbind:function(){var selectControl=this.selectControl;if(this.bound){var layers=this.getLayers();for(var i=0,len=layers.length;i<len;i++){layers[i].events.un({featureselected:this.featureSelected,featureunselected:this.featureUnselected,scope:this});}
this.un("rowselect",this.rowSelected,this);this.un("rowdeselect",this.rowDeselected,this);if(this.autoActivateControl){selectControl.deactivate();}
this.selectControl=null;this.bound=false;}
return selectControl;},featureSelected:function(evt){if(!this._selecting){var store=this.grid.store;var row=store.findBy(function(record,id){return record.getFeature()==evt.feature;});if(row!=-1&&!this.isSelected(row)){this._selecting=true;this.selectRow(row,!this.singleSelect);this._selecting=false;this.grid.getView().focusRow(row);}}},featureUnselected:function(evt){if(!this._selecting){var store=this.grid.store;var row=store.findBy(function(record,id){return record.getFeature()==evt.feature;});if(row!=-1&&this.isSelected(row)){this._selecting=true;this.deselectRow(row);this._selecting=false;this.grid.getView().focusRow(row);}}},rowSelected:function(model,row,record){var feature=record.getFeature();if(!this._selecting&&feature){var layers=this.getLayers();for(var i=0,len=layers.length;i<len;i++){if(layers[i].selectedFeatures.indexOf(feature)==-1){this._selecting=true;this.selectControl.select(feature);this._selecting=false;break;}}}},rowDeselected:function(model,row,record){var feature=record.getFeature();if(!this._selecting&&feature){var layers=this.getLayers();for(var i=0,len=layers.length;i<len;i++){if(layers[i].selectedFeatures.indexOf(feature)!=-1){this._selecting=true;this.selectControl.unselect(feature);this._selecting=false;break;}}}},getLayers:function(){return this.selectControl.layers||[this.selectControl.layer];}};};GeoExt.grid.FeatureSelectionModel=Ext.extend(Ext.grid.RowSelectionModel,new GeoExt.grid.FeatureSelectionModelMixin);
Ext.namespace('GeoExt','GeoExt.data');GeoExt.data.ProtocolProxy=function(config){Ext.apply(this,config);GeoExt.data.ProtocolProxy.superclass.constructor.apply(this,arguments);};Ext.extend(GeoExt.data.ProtocolProxy,Ext.data.DataProxy,{protocol:null,abortPrevious:true,setParamsAsOptions:false,response:null,load:function(params,reader,callback,scope,arg){if(this.fireEvent("beforeload",this,params)!==false){var o={params:params||{},request:{callback:callback,scope:scope,arg:arg},reader:reader};var cb=OpenLayers.Function.bind(this.loadResponse,this,o);if(this.abortPrevious){this.abortRequest();}
var options={params:params,callback:cb,scope:this};Ext.applyIf(options,arg);if(this.setParamsAsOptions===true){Ext.applyIf(options,options.params);delete options.params;}
this.response=this.protocol.read(options);}else{callback.call(scope||this,null,arg,false);}},abortRequest:function(){if(this.response){this.protocol.abort(this.response);this.response=null;}},loadResponse:function(o,response){if(response.success()){var result=o.reader.read(response);this.fireEvent("load",this,o,o.request.arg);o.request.callback.call(o.request.scope,result,o.request.arg,true);}else{this.fireEvent("loadexception",this,o,response);o.request.callback.call(o.request.scope,null,o.request.arg,false);}}});
Ext.namespace("GeoExt.data");GeoExt.data.FeatureStoreMixin=function(){return{layer:null,reader:null,featureFilter:null,constructor:function(config){config=config||{};config.reader=config.reader||new GeoExt.data.FeatureReader({},config.fields);var layer=config.layer;delete config.layer;if(config.features){config.data=config.features;}
delete config.features;var options={initDir:config.initDir};delete config.initDir;arguments.callee.superclass.constructor.call(this,config);if(layer){this.bind(layer,options);}},bind:function(layer,options){if(this.layer){return;}
this.layer=layer;options=options||{};var initDir=options.initDir;if(options.initDir==undefined){initDir=GeoExt.data.FeatureStore.LAYER_TO_STORE|GeoExt.data.FeatureStore.STORE_TO_LAYER;}
var features=layer.features.slice(0);if(initDir&GeoExt.data.FeatureStore.STORE_TO_LAYER){var records=this.getRange();for(var i=records.length-1;i>=0;i--){this.layer.addFeatures([records[i].getFeature()]);}}
if(initDir&GeoExt.data.FeatureStore.LAYER_TO_STORE){this.loadData(features,true);}
layer.events.on({"featuresadded":this.onFeaturesAdded,"featuresremoved":this.onFeaturesRemoved,"featuremodified":this.onFeatureModified,scope:this});this.on({"load":this.onLoad,"clear":this.onClear,"add":this.onAdd,"remove":this.onRemove,"update":this.onUpdate,scope:this});},unbind:function(){if(this.layer){this.layer.events.un({"featuresadded":this.onFeaturesAdded,"featuresremoved":this.onFeaturesRemoved,"featuremodified":this.onFeatureModified,scope:this});this.un("load",this.onLoad,this);this.un("clear",this.onClear,this);this.un("add",this.onAdd,this);this.un("remove",this.onRemove,this);this.un("update",this.onUpdate,this);this.layer=null;}},getRecordFromFeature:function(feature){var record=null;if(feature.state!==OpenLayers.State.INSERT){record=this.getById(feature.id);}else{var index=this.findBy(function(r){return r.getFeature()===feature;});if(index>-1){record=this.getAt(index);}}
return record;},onFeaturesAdded:function(evt){if(!this._adding){var features=evt.features,toAdd=features;if(this.featureFilter){toAdd=[];var i,len,feature;for(var i=0,len=features.length;i<len;i++){feature=features[i];if(this.featureFilter.evaluate(feature)!==false){toAdd.push(feature);}}}
this._adding=true;this.loadData(toAdd,true);delete this._adding;}},onFeaturesRemoved:function(evt){if(!this._removing){var features=evt.features,feature,record,i;for(i=features.length-1;i>=0;i--){feature=features[i];record=this.getRecordFromFeature(feature);if(record!==undefined){this._removing=true;this.remove(record);delete this._removing;}}}},onFeatureModified:function(evt){if(!this._updating){var feature=evt.feature;var record=this.getRecordFromFeature(feature);if(record!==undefined){record.beginEdit();var attributes=feature.attributes;if(attributes){var fields=this.recordType.prototype.fields;for(var i=0,len=fields.length;i<len;i++){var field=fields.items[i];var key=field.mapping||field.name;if(key in attributes){record.set(field.name,field.convert(attributes[key]));}}}
record.set("state",feature.state);record.set("fid",feature.fid);record.setFeature(feature);this._updating=true;record.endEdit();delete this._updating;}}},addFeaturesToLayer:function(records){var i,len,features;features=new Array((len=records.length));for(i=0;i<len;i++){features[i]=records[i].getFeature();}
if(features.length>0){this._adding=true;this.layer.addFeatures(features);delete this._adding;}},onLoad:function(store,records,options){if(!options||options.add!==true){this._removing=true;this.layer.removeFeatures(this.layer.features);delete this._removing;this.addFeaturesToLayer(records);}},onClear:function(store){this._removing=true;this.layer.removeFeatures(this.layer.features);delete this._removing;},onAdd:function(store,records,index){if(!this._adding){this.addFeaturesToLayer(records);}},onRemove:function(store,record,index){if(!this._removing){var feature=record.getFeature();if(this.layer.getFeatureById(feature.id)!=null){this._removing=true;this.layer.removeFeatures([record.getFeature()]);delete this._removing;}}},onUpdate:function(store,record,operation){if(!this._updating){var defaultFields=new GeoExt.data.FeatureRecord().fields;var feature=record.getFeature();if(record.fields){var cont=this.layer.events.triggerEvent("beforefeaturemodified",{feature:feature});if(cont!==false){var attributes=feature.attributes;record.fields.each(function(field){var key=field.mapping||field.name;if(!defaultFields.containsKey(key)){attributes[key]=record.get(field.name);}});this._updating=true;this.layer.events.triggerEvent("featuremodified",{feature:feature});delete this._updating;if(this.layer.getFeatureById(feature.id)!=null){this.layer.drawFeature(feature);}}}}}};};GeoExt.data.FeatureStore=Ext.extend(Ext.data.Store,new GeoExt.data.FeatureStoreMixin);GeoExt.data.FeatureStore.LAYER_TO_STORE=1;GeoExt.data.FeatureStore.STORE_TO_LAYER=2;
Ext.namespace("GeoExt","GeoExt.data");GeoExt.data.LayerReader=function(meta,recordType){meta=meta||{};if(!(recordType instanceof Function)){recordType=GeoExt.data.LayerRecord.create(recordType||meta.fields||{});}
GeoExt.data.LayerReader.superclass.constructor.call(this,meta,recordType);};Ext.extend(GeoExt.data.LayerReader,Ext.data.DataReader,{totalRecords:null,readRecords:function(layers){var records=[];if(layers){var recordType=this.recordType,fields=recordType.prototype.fields;var i,lenI,j,lenJ,layer,values,field,v;for(i=0,lenI=layers.length;i<lenI;i++){layer=layers[i];values={};for(j=0,lenJ=fields.length;j<lenJ;j++){field=fields.items[j];v=layer[field.mapping||field.name]||field.defaultValue;v=field.convert(v);values[field.name]=v;}
values.layer=layer;records[records.length]=new recordType(values,layer.id);}}
return{records:records,totalRecords:this.totalRecords!=null?this.totalRecords:records.length};}});
Ext.namespace("GeoExt.data");GeoExt.data.FeatureRecord=Ext.data.Record.create([{name:"feature"},{name:"state"},{name:"fid"}]);GeoExt.data.FeatureRecord.prototype.getFeature=function(){return this.get("feature");};GeoExt.data.FeatureRecord.prototype.setFeature=function(feature){if(feature!==this.data.feature){this.dirty=true;if(!this.modified){this.modified={};}
if(this.modified.feature===undefined){this.modified.feature=this.data.feature;}
this.data.feature=feature;if(!this.editing){this.afterEdit();}}};GeoExt.data.FeatureRecord.create=function(o){var f=Ext.extend(GeoExt.data.FeatureRecord,{});var p=f.prototype;p.fields=new Ext.util.MixedCollection(false,function(field){return field.name;});GeoExt.data.FeatureRecord.prototype.fields.each(function(f){p.fields.add(f);});if(o){for(var i=0,len=o.length;i<len;i++){p.fields.add(new Ext.data.Field(o[i]));}}
f.getField=function(name){return p.fields.get(name);};return f;};
Ext.namespace("GeoExt.data");GeoExt.data.LayerRecord=Ext.data.Record.create([{name:"layer"},{name:"title",type:"string",mapping:"name"}]);GeoExt.data.LayerRecord.prototype.getLayer=function(){return this.get("layer");};GeoExt.data.LayerRecord.prototype.setLayer=function(layer){if(layer!==this.data.layer){this.dirty=true;if(!this.modified){this.modified={};}
if(this.modified.layer===undefined){this.modified.layer=this.data.layer;}
this.data.layer=layer;if(!this.editing){this.afterEdit();}}};GeoExt.data.LayerRecord.prototype.clone=function(id){var layer=this.getLayer()&&this.getLayer().clone();return new this.constructor(Ext.applyIf({layer:layer},this.data),id||layer.id);};GeoExt.data.LayerRecord.create=function(o){var f=Ext.extend(GeoExt.data.LayerRecord,{});var p=f.prototype;p.fields=new Ext.util.MixedCollection(false,function(field){return field.name;});GeoExt.data.LayerRecord.prototype.fields.each(function(f){p.fields.add(f);});if(o){for(var i=0,len=o.length;i<len;i++){p.fields.add(new Ext.data.Field(o[i]));}}
f.getField=function(name){return p.fields.get(name);};return f;};
Ext.namespace('GeoExt','GeoExt.data');GeoExt.data.FeatureReader=function(meta,recordType){meta=meta||{};if(!(recordType instanceof Function)){recordType=GeoExt.data.FeatureRecord.create(recordType||meta.fields||{});}
GeoExt.data.FeatureReader.superclass.constructor.call(this,meta,recordType);};Ext.extend(GeoExt.data.FeatureReader,Ext.data.DataReader,{totalRecords:null,read:function(response){return this.readRecords(response.features);},readRecords:function(features){var records=[];if(features){var recordType=this.recordType,fields=recordType.prototype.fields;var i,lenI,j,lenJ,feature,values,field,v;for(i=0,lenI=features.length;i<lenI;i++){feature=features[i];values={};if(feature.attributes){for(j=0,lenJ=fields.length;j<lenJ;j++){field=fields.items[j];if(/[\[\.]/.test(field.mapping)){try{v=new Function("obj","return obj."+field.mapping)(feature.attributes);}catch(e){v=field.defaultValue;}}
else{v=feature.attributes[field.mapping||field.name]||field.defaultValue;}
if(field.convert){v=field.convert(v);}
values[field.name]=v;}}
values.feature=feature;values.state=feature.state;values.fid=feature.fid;var id=(feature.state===OpenLayers.State.INSERT)?undefined:feature.id;records[records.length]=new recordType(values,id);}}
return{records:records,totalRecords:this.totalRecords!=null?this.totalRecords:records.length};}});
Ext.namespace("GeoExt.data");GeoExt.data.LayerStoreMixin=function(){return{map:null,reader:null,constructor:function(config){config=config||{};config.reader=config.reader||new GeoExt.data.LayerReader({},config.fields);delete config.fields;var map=config.map instanceof GeoExt.MapPanel?config.map.map:config.map;delete config.map;if(config.layers){config.data=config.layers;}
delete config.layers;var options={initDir:config.initDir};delete config.initDir;arguments.callee.superclass.constructor.call(this,config);if(map){this.bind(map,options);}},bind:function(map,options){if(this.map){return;}
this.map=map;options=options||{};var initDir=options.initDir;if(options.initDir==undefined){initDir=GeoExt.data.LayerStore.MAP_TO_STORE|GeoExt.data.LayerStore.STORE_TO_MAP;}
var layers=map.layers.slice(0);if(initDir&GeoExt.data.LayerStore.STORE_TO_MAP){this.each(function(record){this.map.addLayer(record.getLayer());},this);}
if(initDir&GeoExt.data.LayerStore.MAP_TO_STORE){this.loadData(layers,true);}
map.events.on({"changelayer":this.onChangeLayer,"addlayer":this.onAddLayer,"removelayer":this.onRemoveLayer,scope:this});this.on({"load":this.onLoad,"clear":this.onClear,"add":this.onAdd,"remove":this.onRemove,"update":this.onUpdate,scope:this});this.data.on({"replace":this.onReplace,scope:this});},unbind:function(){if(this.map){this.map.events.un({"changelayer":this.onChangeLayer,"addlayer":this.onAddLayer,"removelayer":this.onRemoveLayer,scope:this});this.un("load",this.onLoad,this);this.un("clear",this.onClear,this);this.un("add",this.onAdd,this);this.un("remove",this.onRemove,this);this.data.un("replace",this.onReplace,this);this.map=null;}},onChangeLayer:function(evt){var layer=evt.layer;var recordIndex=this.findBy(function(rec,id){return rec.getLayer()===layer;});if(recordIndex>-1){var record=this.getAt(recordIndex);if(evt.property==="order"){if(!this._adding&&!this._removing){var layerIndex=this.map.getLayerIndex(layer);if(layerIndex!==recordIndex){this._removing=true;this.remove(record);delete this._removing;this._adding=true;this.insert(layerIndex,[record]);delete this._adding;}}}else if(evt.property==="name"){record.set("title",layer.name);}else{this.fireEvent("update",this,record,Ext.data.Record.EDIT);}}},onAddLayer:function(evt){if(!this._adding){var layer=evt.layer;this._adding=true;this.loadData([layer],true);delete this._adding;}},onRemoveLayer:function(evt){if(this.map.unloadDestroy){if(!this._removing){var layer=evt.layer;this._removing=true;this.remove(this.getById(layer.id));delete this._removing;}}else{this.unbind();}},onLoad:function(store,records,options){if(!Ext.isArray(records)){records=[records];}
if(options&&!options.add){this._removing=true;for(var i=this.map.layers.length-1;i>=0;i--){this.map.removeLayer(this.map.layers[i]);}
delete this._removing;var len=records.length;if(len>0){var layers=new Array(len);for(var j=0;j<len;j++){layers[j]=records[j].getLayer();}
this._adding=true;this.map.addLayers(layers);delete this._adding;}}},onClear:function(store){this._removing=true;for(var i=this.map.layers.length-1;i>=0;i--){this.map.removeLayer(this.map.layers[i]);}
delete this._removing;},onAdd:function(store,records,index){if(!this._adding){this._adding=true;var layer;for(var i=records.length-1;i>=0;--i){layer=records[i].getLayer();this.map.addLayer(layer);if(index!==this.map.layers.length-1){this.map.setLayerIndex(layer,index);}}
delete this._adding;}},onRemove:function(store,record,index){if(!this._removing){var layer=record.getLayer();if(this.map.getLayer(layer.id)!=null){this._removing=true;this.removeMapLayer(record);delete this._removing;}}},onUpdate:function(store,record,operation){if(operation===Ext.data.Record.EDIT){if(record.modified&&record.modified.title){var layer=record.getLayer();var title=record.get("title");if(title!==layer.name){layer.setName(title);}}}},removeMapLayer:function(record){this.map.removeLayer(record.getLayer());},onReplace:function(key,oldRecord,newRecord){this.removeMapLayer(oldRecord);},getByLayer:function(layer){var index=this.findBy(function(r){return r.getLayer()===layer;});if(index>-1){return this.getAt(index);}},destroy:function(){this.unbind();GeoExt.data.LayerStore.superclass.destroy.call(this);}};};GeoExt.data.LayerStore=Ext.extend(Ext.data.Store,new GeoExt.data.LayerStoreMixin);GeoExt.data.LayerStore.MAP_TO_STORE=1;GeoExt.data.LayerStore.STORE_TO_MAP=2;