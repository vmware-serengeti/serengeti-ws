<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page session="false" %>
<html>
<head>
<title>Home</title>
<link href="resources/css/jquery-ui-1.8.22.custom.css" rel="stylesheet" type="text/css"/>
<script language="javascript" src="resources/js/jquery-1.7.2.min.js"></script>
<script language="javascript" src="resources/js/jquery-ui-1.8.22.custom.min.js"></script>
<script language="javascript" src="resources/js/core.js"></script>
<link rel="stylesheet" type="text/css" href="resources/css/serengeti.css" />
<link rel="stylesheet" type="text/css" href="resources/css/default.css" />
</head>
<body>
<h2>
Serengeti Server FrontEnd
<hr noshade size=1>
</h2>
<div class="controlDiv">
[ Connected to ${serengetiServer} | <a href="action/disconnect">Disconnect</a> ] 
&nbsp; [ <a href="#manage" onClick="$('#controlTabs').slideToggle()">Manage</a> ]
<p>
<div id="controlTabs">
<ul>
<li><a href="#cluster">Clusters</a>
<li><a href="resources">Resources</a>
</ul>
<div id="cluster">
<form id="clusterCreate" action="">
Cluster Name: <input type="text" name="name"><input type="submit" value="Create">
</form>
</div>
<div id="resources"></div>
</div>
</div>
<div id="content" class="ui-widget"></div>
</body>
</html>
