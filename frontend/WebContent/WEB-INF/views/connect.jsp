<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page session="false" %>
<html>
<head>
<title>Connect to Serengeti</title>
<script language="javascript" src="resources/js/jquery-1.7.2.js"></script>
<link rel="stylesheet" type="text/css" href="resources/css/serengeti.css" />
<link rel="stylesheet" type="text/css" href="resources/css/default.css" />
</head>
<body>
<h2>
Serengeti Server FrontEnd
<hr noshade size=1>
</h2>
   	<div class="controlDiv">
		<form name="connect" action="action/connect" method="post">
  			Serengeti Server: <input name="serengetiServer" type="text" size="20" />
 			<input type="submit" value="Connect" />
		</form>		    	
   	</div>
   	<div class="emptyClusterDiv">&nbsp;</div>
   	<div class="emptyClusterDiv">&nbsp;</div>
   	<div class="emptyClusterDiv">&nbsp;</div>
   	<div class="emptyClusterDiv">&nbsp;</div>
   	<div class="emptyClusterDiv">&nbsp;</div>
   	<div class="emptyClusterDiv">&nbsp;</div>
</body>
</html>
