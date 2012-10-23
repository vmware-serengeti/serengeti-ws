# COMMAND LINE INTERFACE (CLI) INTRODUCTION
## 1. Terminology
<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>Node: A
VM in the cluster</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>NodeGroup:
A group of VMs with same functionality and spec</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>Cluster:
A group of NodeGroups working together</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>Distro:
A Hadoop distribution version</span></p>


## 2. Usage
CLI supports shell mode, command line mode, and execution of script file. After compile, you can find the jar file under cli/target directory.
- Shell mode: java -jar serengeti-cli-*.jar. It supports tab key based command hint and completion. It supports history by up/down arrows.
- Command line mode: java -jar serengeti-cli-*.jar "command1;command2..."
- Execution of script file: in shell mode or command line mode, execute "script --file scriptFileName". The shell history file named cli.history will help to generate the script file. 

## 3. Command Syntax
<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>CLI
commands are built on spring
shell(https://github.com/SpringSource/spring-shell), so its syntax follows
spring shell command syntax below:</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>Command
::= serengeti &lt;command-category&gt; &lt;command-name&gt; &lt;required-command-key-values&gt;*
[&lt;optional-command-key-values&gt;]*</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>required-command-key-values
::= &lt;command-key-value-fullsize&gt;</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>optional-command-key-values
::= &lt;command-key-value-fullsize&gt;</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>required-command-key-values-fullsize
::= &lt;command-key-value-fullsize&gt;</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>optional-command-key-value-fullsize
::= &lt;command-key-value-fullsize&gt;</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>command-key-value-fullsize
::= '--'&lt;command-key-full&gt; [&lt;value&gt;]</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>-
Command-category is the type of object that the command will operate on.</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>-
Command-name is the operation to do.</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>-
Required-command-key-values are parameters that required for the operation.</span></p>

<p class=MsoNormal><span style='font-family:"Times New Roman","serif"'>-
Optional-command-key-values are parameters that are optional for the operation.</span></p>

## 4. Command Help
- Shell level Help: user can get shell level help by using "help" in command line mode or help command in shell mode. A list of objects will be displayed with operations and short descriptions.
- Command category level help: to get help for a particular class of object, user can enter help followed by an object. CLI will display all operations defined for the object.
- Command level help: to get help for a particular operation, user can enter help followed by an object and an operation. This will display all parameters.

## 5. Command List
<table class=MsoTableGrid border=1 cellspacing=0 cellpadding=0 width=800
 style='width:6.5in;border-collapse:collapse;border:none'>
 <tr>
  <td width=67 valign=top style='width:.7in;border:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>command-category</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border:solid windowtext 1.0pt;
  border-left:none;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>command-name</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border:solid windowtext 1.0pt;
  border-left:none;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Parameters</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border:solid windowtext 1.0pt;
  border-left:none;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>descriptions</span></p>
  </td>
 </tr>
 <tr>
  <td width=67 valign=top style='width:.7in;border:solid windowtext 1.0pt;
  border-top:none;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>connect</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--host
  &lt;Serengeti server host&gt;:8080</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--username
  &lt;serengeti user name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--password
  &lt;serengeti password&gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Connect
  a Serengeti server.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remark</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>The
  Serengeti host with optional port number, e.g. hostname:port .</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Connect command requires
  username and password interactively. If connect failed, the other Serengeti command is not allowed to execute.</span></p>
  </td>
 </tr>
 <tr>
  <td width=67 rowspan=3 valign=top style='width:.7in;border:solid windowtext 1.0pt;
  border-top:none;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>resource
  pool</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>add</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;resource pool name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--vcrp
  &lt; VC rp name &gt; </span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--vccluster
  &lt; vsphere cluster name &gt; </span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--username</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Add
  a new resource pool.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remark</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>User
  must add a resource pool before create a cluster.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>list</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;resource pool name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--detail
  &lt;flag to show node information&gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>
  Show resource pool information.</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>delete</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;resource pool name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Delete
  an unused resource pool.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remark</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>User
  cannot delete a resource pool which is in use.</span></p>
  </td>
 </tr>
 <tr>
  <td width=67 rowspan=3 valign=top style='width:.7in;border:solid windowtext 1.0pt;
  border-top:none;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>datastore</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>add</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;data store name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--spec
  &lt;</span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>datastore
  name(s) in the vsphere&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--type
  &lt;specify the type for storage: SHARED or LOCAL&gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Add
  new datastore(s) .</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remark</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>When
  the spec parameter include some datastore names, users need to use &quot;,&quot; to separate them. Users may also specify
  multiple data stores by a wildcard.</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>list</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;data store name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--detail
  &lt; flag to show datastore detail information &gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Show
  datastore information.</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>delete</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;data store name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Delete
  an unused datastore .</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
 </tr>
 <tr>
  <td width=67 rowspan=3 valign=top style='width:.7in;border:solid windowtext 1.0pt;
  border-top:none;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>network</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>add</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;network name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--portGroup&lt;vsphere
  port group name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><i><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Combination
  1</span></i></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--</span><span
  style='font-size:10.0pt;font-family:"Times New Roman","serif"'> </span><span
  style='font-size:9.0pt;font-family:"Times New Roman","serif"'>dhcp &lt; </span><span
  style='font-size:9.0pt;font-family:"Times New Roman","serif"'>mark as<span
  style='color:#313131'> </span>dhcp type</span><span style='font-size:9.0pt;
  font-family:"Times New Roman","serif"'>&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><i><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Combination
  2</span></i></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--</span><span
  style='font-size:10.0pt;font-family:"Times New Roman","serif"'> </span><span
  style='font-size:9.0pt;font-family:"Times New Roman","serif"'>ip &lt;ip range
  information&gt; </span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--</span><span
  style='font-size:10.0pt;font-family:"Times New Roman","serif"'> </span><span
  style='font-size:9.0pt;font-family:"Times New Roman","serif"'>dns&lt;</span><span
  style='font-size:10.0pt;font-family:"Times New Roman","serif"'> </span><span
  style='font-size:9.0pt;font-family:"Times New Roman","serif"'>first dns
  information &gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--</span><span
  style='font-size:10.0pt;font-family:"Times New Roman","serif"'> </span><span
  style='font-size:9.0pt;font-family:"Times New Roman","serif"'>secondDNS &lt;second
  dns information&gt; </span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--</span><span
  style='font-size:10.0pt;font-family:"Times New Roman","serif"'> </span><span
  style='font-size:9.0pt;font-family:"Times New Roman","serif"'>gateway
  &lt;gateway information&gt; </span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--</span><span
  style='font-size:10.0pt;font-family:"Times New Roman","serif"'> </span><span
  style='font-size:9.0pt;font-family:"Times New Roman","serif"'>mask&lt;mask
  information&gt; </span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Add
  a network to Serengeti.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remark</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Users
  must enter either ip range or dhcp, but not both.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Example</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>network
  add --name ipNetwork --ip 192.168.1.1-100,192.168.1.256-300 --portGroup pg1
  --dns 202.112.0.1 --gateway 192.168.1.255 --mask 255.255.255.1</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>network
  add --name dhcpNetwork --dhcp</span></p>
  </td>
 </tr>
 <tr style='height:31.05pt'>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:31.05pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>list</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:31.05pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;network name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--detail
  &lt;flag to show network detail information &gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:31.05pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Show network information.</span></a></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>delete</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;network name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Delete
  an unused network.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remark</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>User
  cannot delete a network which is in use.</span></p>
  </td>
 </tr>
 <tr>
  <td width=67 rowspan=11 valign=top style='width:.7in;border:solid windowtext 1.0pt;
  border-top:none;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>cluster</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>create</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--distro
  &lt; hadoop distro name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--specFile
  &lt;spec file pathname&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--rpNames
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>resource
  pools for the cluster &gt; </span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--dsNames
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>datastores
  for the cluster&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--networkName
  &lt;network name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--resume
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>flag
  to resume cluster creation &gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--topology
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>
Specify which topology type will be used for rack awareness: HVE, RACK_AS_RACK, or HOST_AS_RACK. &gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--skipConfigValidation
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>flag to skip cluster configuration validation 
&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--yes
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>flag
  to answer 'yes' to all Y/N questions &gt;</span></p>

  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Create
  a Hadoop cluster</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remark</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>The
  specFile is defined in json format.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Example</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>cluster
  create --name testCluster</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>config</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--specFile
  &lt;spec file pathname&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--skipConfigValidation
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>flag to skip cluster configuration validation 
&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--yes
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>flag
  to answer 'yes' to all Y/N questions &gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Config
  an existing cluster.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remark</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>The
  specFile is defined in json format.</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>delete</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Delete
  an unused cluster.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>resize</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--nodeGroup
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>node
  group name&gt; </span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--instanceNum
  &lt;instance number&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Change
  the number of nodes in a node group.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Parameter
  Definition</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>instance
  number ::= &lt;final number&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remark</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>The
  instanceNum should be larger than the existing instance numbers in the node
  group.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Example</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>cluster
  resize --name testCluster --nodeGroup slave -- instanceNum 10</span></p>
  </td>
 </tr>
 <tr style='height:47.2pt'>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:47.2pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>start</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:47.2pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--nodeGroupName
  &lt;node group name&gt;</a></span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--nodeName
  &lt;node name&gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:47.2pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Start
  VMs in a cluster.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>stop</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--nodeGroupName
  &lt;node group name&gt;</a></span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--nodeName
  &lt;node name&gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Stop
  VMs in a cluster.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>limit</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;: Name of the Hadoop cluster in Serengeti</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--activeComputeNodeNum
  &lt;active node number&gt;: Number of active compute nodes for the specified Hadoop cluster or node group within that cluster.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--nodeGroupName
  &lt;node group name&gt;: Name of a node group in the specified Hadoop cluster in Serengeti (only supports node groups with task tracker role)</a></span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Enable or disable provisioned compute nodes 
in the specified Hadoop cluster or node group in Serengeti to reach the limit specified by activeComputeNodeNum.  
Compute nodes are re-commissioned and powered-on or decommissioned and powered-off to reach the specified number of active compute nodes.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>unlimit</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;: Name of the Hadoop cluster in Serengeti</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--nodeGroupName
  &lt;node group name&gt;: Name of a node group in the specified Hadoop cluster in Serengeti (only supports node groups with task tracker role)</a></span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Remove cluster limit restriction for provisioned compute nodes in the specified Hadoop cluster or node group in Serengeti. All compute nodes in specified cluster or node group are all re-commissioned and powered-on.</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
 </tr>
<tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>list</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;</a></span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--detail
  &lt;flag to show node information&gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Show
  cluster information.     Note: with this option specified, Serengeti will query from vCenter server to get latest node status. That operation may take some time, for example, longer than 7 seconds for each cluster. Please be patient.</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>export</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--spec
  &lt;falg to export cluster specification&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--output
  &lt;output file name&gt;</a></span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Export
  cluster specification.</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>target</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;cluster name&gt;</a></span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--info
  &lt;flag to show target information&gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Set
  or query target cluster to run hadoop commands.</span></p>
  </td>
 </tr>
 <tr style='height:31.0pt'>
  <td width=67 valign=top style='width:.7in;border:solid windowtext 1.0pt;
  border-top:none;padding:0in 5.4pt 0in 5.4pt;height:31.0pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>distro</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:31.0pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>list</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:31.0pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--name
  &lt;distro name&gt;</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--detail
  &lt;</span><span style='font-size:10.0pt;font-family:"Times New Roman","serif"'>
  </span><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>flag
  to show distro detail information&gt;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:31.0pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Show
  distro information.</span></p>
  </td>
 </tr>
 <tr style='height:31.0pt'>
  <td width=67 rowspan=2 valign=top style='width:.7in;border:solid windowtext 1.0pt;
  border-top:none;padding:0in 5.4pt 0in 5.4pt'>

  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>topology</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:31.0pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>list</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:31.0pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None</span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>None
  </span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt;height:31.0pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Show
  the rack->hosts mapping topology.</span></p>
  </td>
 </tr>
 <tr>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>upload</span></p>
  </td>
  <td width=216 valign=top style='width:2.25in;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Mandatory</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--fileName
  &lt;topology file name&gt;</a></span></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Options</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>--yes
  &lt;answer 'yes' to all Y/N questions.&gt;</a></span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Upload a rack-->hosts mapping topology file.
  </span></p>
  </td>
 </tr>
 <tr>
  <td width=67 valign=top style='width:.7in;border:solid windowtext 1.0pt;
  border-top:none;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>disconnect</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
  <td width=66 valign=top style='width:49.5pt;border-top:none;border-left:none;
  border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>&nbsp;</span></p>
  </td>
  <td width=275 valign=top style='width:206.1pt;border-top:none;border-left:
  none;border-bottom:solid windowtext 1.0pt;border-right:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><b><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Function</span></b></p>
  <p class=MsoNormal style='margin-bottom:0in;margin-bottom:.0001pt;line-height:
  normal'><span style='font-size:9.0pt;font-family:"Times New Roman","serif"'>Disconnect
  the Serengeti server.</span></p>
  </td>
 </tr>
</table>
## 6. Hadoop Commands
From CLI 0.6.0, we integrated impala(https://github.com/SpringSource/impala) hadoop hdfs, map/reduce, pig, and hive commands into CLI. You need to use "cluster target" command to set hdfs or jobtracker url before launching hdfs or map/reduce commands. More details can be found from CLI help.

