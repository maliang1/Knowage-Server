var app = angular.module('svgViewerApp', ['ngMaterial','sbiModule']);

app.config(['$mdThemingProvider', function($mdThemingProvider) {
	    $mdThemingProvider.theme('knowage')
	    $mdThemingProvider.setDefaultTheme('knowage');
	}]);

app.controller('SvgViewerController', ['$scope','sbiModule_restServices','$mdSidenav','sbiModule_logger','$window','sbiModule_config','$rootScope',SvgViewerControllerFunction] );
		
function SvgViewerControllerFunction($scope, sbiModule_restServices, $mdSidenav,sbiModule_logger,$window,sbiModule_config,$rootScope)	{
  $scope.isSidenavOpen = false;
  $scope.showBackButton = false;
  
  $scope.currentLevel = 1;
  $scope.currentId = null;
  
  //stack that contains the drill path elements
  $scope.drillPathStack = [];
    
  $scope.openSideNav = function() {
    $mdSidenav('svgSideNav').toggle();
  };
  
  //Go back to the previous level
  //TODO: to test and change
  $scope.goToPreviousLevel = function(){
	  $scope.currentLevel = $scope.currentLevel - 1;
	  
	  var pathElement = $scope.drillPathStack.pop();
	  $scope.currentId = pathElement.id;

	  
	  if (pathElement.level == 1){
		  document.getElementById('svgContainer').src = sbiModule_config.contextName+"/api/1.0/svgviewer/drillMap?level="+pathElement.level;
	  } else {
		  document.getElementById('svgContainer').src = sbiModule_config.contextName+"/api/1.0/svgviewer/drillMap?member="+pathElement.id+"&level="+pathElement.level;
	  }
	  
	  if($scope.currentLevel == 1){
		  $scope.showBackButton = false;
	  }
  }
    
  
  $window.document.addEventListener("SVGLoaded", function(e) {
	  //retrieve Measures
	  $scope.getMeasures();
	  //retrieve Layers
	  $scope.getLayers();
	  $scope.getLegendColors();
	});
  
  //Listener called when an element on the svg is clicked
  $window.document.addEventListener("SVGElementClicked", function(e) {
	  
	  //update drill path with stack
	  var pathElement = new Object();
	  pathElement.level = $scope.currentLevel;
	  pathElement.id = $scope.currentId;
	  $scope.drillPathStack.push(pathElement);
	  
	  //alert("Clicked element with id "+e.detail);  
	  $scope.currentLevel = $scope.currentLevel +1;
	  $scope.currentId = e.detail;
	  document.getElementById('svgContainer').src = sbiModule_config.contextName+"/api/1.0/svgviewer/drillMap?member="+e.detail+"&level="+$scope.currentLevel;
	  
	  if  ($scope.currentLevel > 1){
		  $scope.showBackButton = true;
	  }
	});
  
  /**
   * Loads the measures list with a REST service
   * */
  $scope.getMeasures = function(){
	  sbiModule_restServices.get("1.0/svgviewer", 'getMeasures','level='+$scope.currentLevel).success(
			  function(data, status, headers, config) {
				  if (data.hasOwnProperty("errors")) {
					  sbiModule_logger.log("measures not retrivied");
				  } else {
					  $scope.measures = data;
					  for (var propt in $scope.measures){
						  if ($scope.measures[propt].selected){
							  //set default selected measure
							  $scope.measureValue = $scope.measures[propt].columnId;
						  }
					  }
					  sbiModule_logger.trace("measures correctly retrivied",data);		

				  }
			  }).error(function(data, status, headers, config) {
				  sbiModule_logger.log("measures not retrivied");
			  });
  };
	
	
	

  /**
   * Loads the measures list with a REST service
   * */
  $scope.getLayers = function(){
	  sbiModule_restServices.get("1.0/svgviewer", 'getLayers','level='+$scope.currentLevel).success(
			  function(data, status, headers, config) {
				  if (data.hasOwnProperty("errors")) {
					  sbiModule_logger.log("layers not retrivied");
				  } else {
					  
					  for (var key in data) {
						  //force all layers to be selected by default
						  data[key].selected = true;
					  }
					  $scope.layers = data;
					  sbiModule_logger.trace("layers correctly retrivied",data);		

				  }
			  }).error(function(data, status, headers, config) {
				  sbiModule_logger.log("layers not retrivied");
			  });
  };


  //retrieve the SVG element from the specified container
  $scope.getSVG = function(containerId){
	  var svgdoc = null;
	  var svgwin = null;
	  var embed = document.getElementById(containerId);
	  try {
		  svgdoc = embed.getSVGDocument();
	  }
	  catch(exception) {
		  sbiModule_logger.log('The GetSVGDocument interface is not supported');
	  }

	  if (svgdoc && svgdoc.defaultView){  // try the W3C standard way first
		  svgwin = svgdoc.defaultView;
	  }
	  else if (embed.window)
		  svgwin = embed.window;
	  else try {
		  svgwin = embed.getWindow();
	  }
	  catch(exception) {
		  sbiModule_logger.log('The DocumentView interface is not supported\r\n' +
		  'Non-W3C methods of obtaining "window" also failed');
	  }

	  return svgwin;
  }
  
  /**
  * Change the selected measure and apply the color visualization
  * */  
  $scope.changeSelectedMeasure =  function(columnId,description)  {
	  var svgwin = $scope.getSVG('svgContainer');
	  //call setKPI function inside the SVG
      svgwin.setKpi('radioButtons',columnId,description);
	  $scope.getLegendColors();

  }
  /**
   * Show or hide a specific layer 
   * */  
  $scope.changeSelectedLayer =  function(layerName, showToggle)  {
	  var svgwin = $scope.getSVG('svgContainer');
	  //call toggleMapLayer function inside the SVG 
      svgwin.toggleMapLayer(layerName,showToggle);
  }
  
  $scope.getLegendColors = function(){
	  var svgwin = $scope.getSVG('svgContainer');
	  if (svgwin.myMapApp.colArray){
		  $scope.legend = new Object();
		  $scope.legend.colors = svgwin.myMapApp.colArray;
		  $scope.legend.tresholds = svgwin.myMapApp.threshArray;
	  }
  }
};