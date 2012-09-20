<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page session="false" %>
<div class="resourceGroupDiv ui-widget-content ui-corner-all">
<div class="titleDiv ui-widget-header">Resource Pool List</div>
<c:forEach var="rp" items="${resourcePools}">
<div class="resourceDiv">${rp.rpName} (${rp.vcCluster}/${rp.rpVsphereName})
<br clear="all">
<c:forEach var="node" items="${rp.nodes}">
	<div class="vmDiv" title="${node.name}">&nbsp;</div>
</c:forEach>	
</div>
</c:forEach>
</div>
<div class="resourceGroupDiv ui-widget-content ui-corner-all">
<div class="titleDiv ui-widget-header">Datastore List</div>
<c:forEach var="ds" items="${datastores}">
<div class="resourceDiv">${ds.name} (${ds.type}) 
<br clear="all">
</div>
</c:forEach>
</div>
<div class="resourceGroupDiv ui-widget-content ui-corner-all">
<div class="titleDiv ui-widget-header">Network List</div>
<c:forEach var="nw" items="${networks}">
<div class="resourceDiv">${nw.name} (${nw.portGroup})
<br clear="all">
</div>
</c:forEach>
</div>
