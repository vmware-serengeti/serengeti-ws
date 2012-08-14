<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page session="false" %>
<c:set var="count" value="0"/>
<c:forEach var="cluster" items="${clusterList}">
<c:set var="count" value="${count + 1}"/>
<div class="clusterDiv ui-widget-content ui-corner-all">
<div class="nameDiv">Name: ${cluster.name}</div>
<div class="statusDiv">Instances: ${cluster.instanceNum}</div>
<div class="statusDiv">Status: ${cluster.status}</div>
<div class="actionDiv"><a href="action/cluster/delete/${cluster.name}">Delete</a></div>
<div class="actionDiv"><a href="#notimplemented">Resize</a></div>
<br clear="all">
<c:forEach var="ng" items="${cluster.nodeGroups}">
	<div class="nodeGroupDiv"><label>${ng.name}:</label> 
	<c:forEach var="node" items="${ng.instances}">
		<div class="vmDiv ${node.status}" title="${node.name} - ${node.status}">&nbsp;</div>
	</c:forEach>
</div>
</c:forEach>
</div>
</c:forEach>
<c:if test="${count == 0}">
<div class="emptyClusterDiv ui-corner-all">
<h3>No Clusters</h3>
</div>
</c:if>
