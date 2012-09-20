<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="f" uri="http://www.springframework.org/tags/form"%>
<html>
<head>
<title>Connect to Serengeti</title>
<script language="javascript" src="resources/js/jquery-1.7.2.js"></script>
<script language="javascript" src="resources/js/jquery-ui-1.8.22.custom.min.js"></script>

<link rel="stylesheet" type="text/css"
	href="resources/css/serengeti.css" />
<link rel="stylesheet" type="text/css" href="resources/css/default.css" />
</head>
<body>
	<h2>
		Serengeti Server FrontEnd
		<hr noshade size=1>
	</h2>
	<div class="controlDiv">
		<f:form modelAttribute="ConnectForm" name="connect" action="action/connect" method="post">
			<label>Serengeti Server:</label> <f:input path="serengetiServer"/>
			<label>Username:</label><f:input path="username"></f:input>
			<label>Password:</label><f:input path="password"></f:input>
			<input type="submit" value="Connect" />
		</f:form>
	</div>
	<div class="emptyClusterDiv">&nbsp;</div>
	<div class="emptyClusterDiv">&nbsp;</div>
	<div class="emptyClusterDiv">&nbsp;</div>
	<div class="emptyClusterDiv">&nbsp;</div>
	<div class="emptyClusterDiv">&nbsp;</div>
	<div class="emptyClusterDiv">&nbsp;</div>
</body>
</html>
