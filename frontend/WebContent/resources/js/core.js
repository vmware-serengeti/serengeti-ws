init = function() {
	$("#controlTabs").tabs();
//	$("#controlTabs").hide();
	$("#content").load('clusters');

	setInterval(function(response) {
		$("#content").load('clusters');
	}, 5000);

	$('#clusterCreate').submit(function(e) {
		e.preventDefault();
		$.post('action/cluster/create', $(this).serialize(), function(data) {
			$("#content").load('clusters');
		});
	});	

	
};

$(document).ready(init);
