require.config({
	urlArgs: "v=3.4-SNAPSHOT",
	baseUrl: '/static/js/',
	paths: {
		'jquery' : 'cdr-admin',
		'jquery-ui' : 'cdr-admin',
		'text' : 'lib/text',
		'underscore' : 'lib/underscore',
		'tpl' : 'lib/tpl',
		'qtip' : 'lib/jquery.qtip.min',
		'moment' : 'cdr-admin',
		
		'PID' : 'cdr-admin',
		'RemoteStateChangeMonitor' : 'cdr-admin',
		'ConfirmationDialog' : 'cdr-admin'
	},
	shim: {
		'qtip' : ['jquery'],
		'underscore': {
			exports: '_'
		}
	}
});

define('collector', ['jquery', 'moment', 'ConfirmationDialog', 'tpl!../templates/admin/collector/binList', 'tpl!../templates/admin/collector/binDetails', 'tpl!../templates/admin/collector/confirm'], 
		function($, moment, ConfirmationDialog, binListTemplate, binDetailsTemplate, confirmTemplate) {
	
	// Trigger collection of the new applicable files
	function collectFiles() {
		var allVals = [];
		$('.file_list :checked').each(function() {
			allVals.push($(this).val());
		});
		
		$.post("collector/bin/" + this.binKey, {"files" : allVals}, function(data){
			$("#collector").html(confirmTemplate(data));
		});
	}
	
	function confirmCollection(binKey, numFiles) {
		var numFilesText = numFiles + " new item" + (numFiles != 1? "s" :"");
		
		new ConfirmationDialog({
				promptText : "Are you sure you want collect " + numFilesText + "?",
				confirmFunction : collectFiles,
				confirmTarget : {binKey : binKey},
				autoOpen : true,
				dialogOptions : {
					width : 'auto',
					modal : true,
					position : { my: "center", at: "center", of: window }
				}
		});
	}
	
	function formatCounts(applicable, nonapplicable) {
		if (applicable) {
			if (nonapplicable) {
				return "(" + applicable + " new item" + (applicable != 1? "s" : "")
						+ ", " + nonapplicable + " unexpected item" + (nonapplicable != 1? "s" : "") + ")";
			} else {
				return "(" + applicable + " new item" + (applicable != 1? "s" : "") + ")";
			}
		} else {
			if (nonapplicable) {
				return "(" + nonapplicable + " unexpected item" + (nonapplicable != 1? "s" : "") + ")";
			} else {
				return "(no new items)";
			}
		}
	}
	
	function bytesToSize(bytes) {
		if (bytes == 0) return '0 B';
		var k = 1024;
		var sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
		var i = Math.floor(Math.log(bytes) / Math.log(k));
		var val = (bytes / Math.pow(k, i)).toFixed(1);
		if (val - Math.floor(val) == 0)
			return Math.floor(val) + ' ' + sizes[i];
		return val + ' ' + sizes[i];
	}
	
	function formatDate(timestamp) {
		var mTime = moment(parseInt(timestamp));
		if (mTime.year() == moment().year()) {
			return mTime.format("MMMM D, H:m:s");
		}
		return mTime.format("MMMM D, YYYY");
	}
	
	// Toggling of checkboxes by clicking the whole row
	$("#collector").on("click", ".file_list.checkable td", function(event) {
		if ($(event.target).is("input"))
			return true;
		var checkbox = $(this).closest("tr").find("input");
		checkbox.prop('checked', !checkbox.prop('checked'));
	});
	
	// Sorting by column
	$("#collector").on("click", ".file_list .sortable", function(event) {
		var $this = $(this);
		var columnIndex = this.cellIndex;
		var table = $this.closest(".file_list");
		var tableBody = table.children("tbody").detach()[0];
		var isNumeric = $this.hasClass("numeric");
		
		var sortList = [];
		for(var i = 0, len = tableBody.rows.length; i < len; i++){
			var row = tableBody.rows[i];
			var value = row.cells[columnIndex].getAttribute("data-sort");
			sortList.push([isNumeric? parseInt(value) : value, row]);
		}
		
		if ($this.hasClass("reverse")) {
			sortList.sort(function(x, y) {
				return x[0] < y[0];
			});
			$this.removeClass("reverse");
		} else {
			sortList.sort(function(x, y) {
				return x[0] > y[0];
			});
			$this.addClass("reverse");
		}
		
		for (var i = 0; i < sortList.length; i++) {
			tableBody.appendChild(sortList[i][1]);
		}
		
		table[0].appendChild(tableBody);
	});
	
	function binDetailsView(binKey, url) {
		$.getJSON("collector/bin/" + binKey, function(response) {
			if (window.history && url) {
				window.history.pushState("Collector bin details", null, url);
			}
			
			var mostRecent = 0;
			for (var index in response.applicableFiles) {
				var time = parseInt(response.applicableFiles[index].time);
				if (time > mostRecent)
					mostRecent = time;
			}
			
			$("#collector").html(binDetailsTemplate({data : response, formatCounts : formatCounts, mostRecent : mostRecent,
					formatDate : formatDate, bytesToSize : bytesToSize}));
					
			$("#bin_details .collect_action").click(function(event) {
				confirmCollection(binKey, $('.file_list tbody :checked').length);
			});
					
			$("#check_all").change(function(event) {
				var $this = $(this);

				if ($this.is(":checked")) {
					$this.closest(".file_list").find("td input").prop('checked', true);
				} else {
					$this.closest(".file_list").find("td input").prop('checked', false);
				}
			});
		});
	}
	
	function binListView() {
		$.getJSON("collector/list", function(response) {
			$("#collector").html(binListTemplate({collectorList : response, formatCounts : formatCounts}));
			
			$("#bin_list").on("click", ".collect_action", function(event) {
				confirmCollection(this.parentNode.getAttribute("data-binkey"), $(this).attr("data-numfiles"));
			});
			
			$("#collector").on("click", ".collect_details_link", function(event) {
				if (!window.history) {
					return true;
				}
				var binKey = $(this).closest(".browseitem").attr("data-binkey");
				binDetailsView(binKey, this.getAttribute("href"));
		
				return false;
			});
		});
	}
	
	var detailsRegex = /collector\/details\/(\w+)$/;
	window.onpopstate = function(e) {
		binCollectorController();
	};
	
	function binCollectorController() {
		var location = document.location;
		
		var detailsBin = document.location.toString().match(detailsRegex);
		if (detailsBin) {
			binDetailsView(detailsBin[1])
		} else {
			binListView();
		}
	}
	
	binCollectorController();
});
